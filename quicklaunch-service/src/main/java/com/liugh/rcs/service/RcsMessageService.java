package com.liugh.rcs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.BillingCdrInfo;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.entity.RcsSendTask;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.mapper.RCSCdrInfoMapper;
import com.liugh.mapper.SmsCdrInfoMapper;
import com.liugh.rcs.channel.RcsChannelAdapter;
import com.liugh.rcs.channel.RcsChannelAdapterRegistry;
import com.liugh.rcs.channel.RcsChannelSubmitException;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.channel.infobip.RcsContentParser;
import com.liugh.rcs.model.RcsCard;
import com.liugh.rcs.model.RcsContent;
import com.liugh.rcs.model.RcsContentValidator;
import com.liugh.rcs.model.RcsMedia;
import com.liugh.service.entity.SubmitResponse;
import com.liugh.service.util.MessageIdGenerator;
import com.liugh.util.PhoneUtils;
import com.liugh.util.QuotaManagerUtil;

import java.util.Date;

import lombok.extern.slf4j.Slf4j;

/**
 * RCS 发送编排（通道无关）：鉴权 → 路由 → 校验 → 媒体解析 → 写 CDR → adapter.send → 计费。
 * 各通道差异下沉到 RcsChannelAdapter。
 */
@Service
@Slf4j
public class RcsMessageService {

    @Autowired
    private RcsMessagePersistenceService persistenceService;
    @Autowired
    private RcsFileService rcsFileService;
    @Autowired
    private RcsRouter rcsRouter;
    @Autowired
    private RcsChannelAdapterRegistry adapterRegistry;
    @Autowired
    private RcsCustomerAccessService customerAccessService;
    @Autowired
    private RcsCallbackService rcsCallbackService;

    @Value("${project.url}")
    private String projectUrl;
    private String sendMode = "DIRECT";
    private int asyncMaxRetries = 3;

    @Value("${rcs.send-mode:DIRECT}")
    public void setSendMode(String sendMode) {
        this.sendMode = sendMode == null ? "DIRECT" : sendMode.trim();
    }

    @Value("${rcs.send-task.max-retries:3}")
    public void setAsyncMaxRetries(int asyncMaxRetries) {
        this.asyncMaxRetries = Math.max(1, asyncMaxRetries);
    }

    public SubmitResponse send(JSONObject request) {
        SubmitResponse result = new SubmitResponse();
        result.setStatus(false);
        try {
            AdminInfo customer = customerAccessService.requireCustomer(request);
            String sender = firstNonBlank(request.getString("sender"), request.getString("Sender"));
            String recipient = firstNonBlank(request.getString("recipient"), request.getString("Recipient"));
            if (recipient == null) {
                log.error("rcs send missing recipient");
                return result;
            }
            if (!recipient.startsWith("+")) {
                recipient = "+" + recipient;
            }
            // Token → 归属客户(ssid)，客户不直传 ssid（与原平台一致）
            int ssid = customer.getSsid();
            String agentId = request.getString("agentId");
            String trafficType = firstNonBlank(request.getString("trafficType"), "TRANSACTION");
            Integer ttl = request.getInteger("ttl");
            String contentKeywordId = request.getString("contentKeywordId");

            RcsContent content = RcsContentParser.parse(request);
            String err = RcsContentValidator.validate(content);
            if (err != null) {
                log.warn("rcs content invalid: {}", err);
                return result;
            }
            resolveMedia(content);

            // 路由选通道：ossid(=customer.id) + 收件国家 → rssid → 供应商 admin_info（与 SMS 一致）
            // 供应商记录同时携带 channel_code（选适配器）与连接凭证（url/appId/appSecurity）。
            String country = safeRegion(recipient);
            AdminInfo supplier = rcsRouter.route(customer, country, sender, contentKeywordId);
            if (supplier == null) {
                log.error("rcs no supplier routed, ssid:{}, country:{}", ssid, country);
                return result;
            }
            String channelCode = supplier.getChannelCode();
            Integer rssid = toInt(supplier.getId());
            RcsChannelAdapter adapter = adapterRegistry.get(channelCode);
            if (adapter == null) {
                log.error("rcs no adapter for channel {}, rssid:{}", channelCode, rssid);
                return result;
            }
            // sender-first：未传 sender 时用 agent 在该通道的绑定
            if (sender == null && agentId != null) {
                RcsChannelAgent binding = rcsRouter.resolveAgentBinding(agentId, channelCode);
                if (binding != null) {
                    sender = binding.getSender();
                }
            }

            String msgId = MessageIdGenerator.generateCommonOutMsgID(ssid);
            long now = System.currentTimeMillis();

            RCSCdrInfo cdr = new RCSCdrInfo();
            cdr.setSsid(ssid);
            cdr.setRssid(rssid == null ? 0 : rssid);
            cdr.setMsgId(msgId);
            cdr.setMsisdn(recipient);
            cdr.setSender(sender);
            cdr.setContent(buildContentJson(request));
            cdr.setMsgType(content.getType().name());
            cdr.setTrafficType(trafficType);
            cdr.setChannelCode(channelCode);
            cdr.setCountryCode(country);
            cdr.setThumbnail(extractThumbnail(content));
            cdr.setSuggestions(extractSuggestionsJson(request));
            cdr.setStatus(-1);
            cdr.setCreateTime(now);
            cdr.setInsubmittime(now);
            cdr.setExpireTime(ttl == null ? 0 : now + ttl * 1000L);
            cdr.setRetryCount(0);
            cdr.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(now, "yyyy-MM-dd HH:mm:ss", "GMT+8"));
            BillingCdrInfo billing = buildBilling(cdr, -1);

            RcsSendCtx ctx = new RcsSendCtx();
            ctx.setSsid(ssid);
            ctx.setSender(sender);
            ctx.setRecipient(recipient);
            ctx.setMsgId(msgId);
            ctx.setNotifyUrl(buildNotifyUrl(channelCode));
            ctx.setTtl(ttl);
            ctx.setChannelCode(channelCode);
            ctx.setRssid(rssid);
            // Infobip: apiKey=admin.appId；RM: user=admin.appId, pass=admin.appSecurity
            ctx.setBaseUrl(supplier.getUrl());
            ctx.setApiKey(supplier.getAppId());
            ctx.setAuthUser(supplier.getAppId());
            ctx.setAuthPass(supplier.getAppSecurity());
            ctx.setSmsSender(supplier.getSmsSender());
            ctx.setFallbackText(request.getString("fallbackText"));
            ctx.setCountryCode(country);
            ctx.setChannelSender(firstNonBlank(supplier.getChannelSender(), sender));

            if (isAsyncMode()) {
                RcsSendTask task = buildTask(request, cdr);
                persistenceService.acceptQueued(cdr, billing, task);
                result.setOutMsgId(msgId);
                result.setStatus(true);
                return result;
            }

            persistenceService.acceptDirect(cdr, billing);
            String channelMsgId;
            try {
                channelMsgId = adapter.send(content, ctx);
            } catch (RcsChannelSubmitException e) {
                persistenceService.markFinalFailure(cdr, e.getErrorCode(), e.getErrorReason());
                return result;
            }
            if (channelMsgId != null) {
                if (adapter.isImmediateDelivery()) {
                    // 即时投递通道（如 BLACKHOLE）：跳过 submit→callback 流程，
                    // 直接推进到 status=3 并同步触发客户 DR 通知。
                    persistenceService.markDirectDelivered(cdr, channelMsgId);
                    rcsCallbackService.fireSyntheticDelivery(cdr);
                } else {
                    persistenceService.markSubmitted(cdr, channelMsgId);
                }
                result.setOutMsgId(msgId);
                result.setStatus(true);
            } else {
                persistenceService.markFinalFailure(cdr, "submit rejected");
            }
        } catch (RcsCustomerAccessException e) {
        	e.printStackTrace();
            log.warn("rcs send customer request rejected, error:{}", e.getErrorCode());
            
        } catch (Exception e) {
            log.error("rcs send failed", e);
        }
        return result;
    }

    // ================= helpers =================

    private BillingCdrInfo buildBilling(RCSCdrInfo cdr, int ostatus) {
        BillingCdrInfo billing = new BillingCdrInfo();
        billing.setSsid(cdr.getSsid());
        billing.setRssid(cdr.getRssid());
        billing.setMsgId(cdr.getMsgId());
        billing.setMsisdn(cdr.getMsisdn());
        billing.setSender(cdr.getSender());
        billing.setContent(cdr.getContent());
        billing.setOstatus(ostatus);
        billing.setRstatus(-1);
        billing.setMobileType("RCS:" + cdr.getMsgType());
        billing.setCreateTime(cdr.getCreateTime());
        billing.setTimemark(cdr.getTimemark());
        billing.setInsubmittime(cdr.getInsubmittime());
        return billing;
    }

    private RcsSendTask buildTask(JSONObject request, RCSCdrInfo cdr) {
        Date now = new Date();
        RcsSendTask task = new RcsSendTask();
        task.setMsgId(cdr.getMsgId());
        task.setSsid(cdr.getSsid());
        task.setRssid(cdr.getRssid());
        task.setChannelCode(cdr.getChannelCode());
        task.setTaskStatus("QUEUED");
        task.setRequestJson(request.toJSONString());
        task.setAttemptCount(0);
        task.setMaxRetries(asyncMaxRetries);
        task.setNextAttemptTime(now);
        task.setCreateTime(now);
        task.setUpdateTime(now);
        return task;
    }

    private boolean isAsyncMode() {
        return "ASYNC".equalsIgnoreCase(sendMode);
    }

    private void resolveMedia(RcsContent content) {
        if (content.getFile() != null) {
            resolveMediaObj(content.getFile());
        }
        if (content.getCard() != null && content.getCard().getMedia() != null) {
            resolveMediaObj(content.getCard().getMedia());
        }
        if (content.getCards() != null) {
            for (RcsCard card : content.getCards()) {
                if (card.getMedia() != null) {
                    resolveMediaObj(card.getMedia());
                }
            }
        }
    }

    private void resolveMediaObj(RcsMedia m) {
        m.setUrl(rcsFileService.resolve(m.getUrl()));
        if (m.getThumbnailUrl() != null) {
            m.setThumbnailUrl(rcsFileService.resolve(m.getThumbnailUrl()));
        }
    }

    private String buildContentJson(JSONObject request) {
        JSONObject c = request.getJSONObject("content");
        if (c != null) {
            return c.toJSONString();
        }
        return request.getString("Content");
    }

    private String extractThumbnail(RcsContent content) {
        if (content.getFile() != null) {
            return content.getFile().getThumbnailUrl();
        }
        if (content.getCard() != null && content.getCard().getMedia() != null) {
            return content.getCard().getMedia().getThumbnailUrl();
        }
        return null;
    }

    private String extractSuggestionsJson(JSONObject request) {
        JSONObject c = request.getJSONObject("content");
        if (c == null) {
            return null;
        }
        if (c.getJSONArray("suggestions") != null) {
            return c.getJSONArray("suggestions").toJSONString();
        }
        JSONObject card = c.getJSONObject("card");
        if (card != null && card.getJSONArray("suggestions") != null) {
            return card.getJSONArray("suggestions").toJSONString();
        }
        return null;
    }

    private String buildNotifyUrl(String channelCode) {
        String base = projectUrl;
        if (base != null && !base.endsWith("/")) {
            base = base + "/";
        }
        return base + "rcs/callback/" + channelCode.toLowerCase();
    }

    private String safeRegion(String e164) {
        try {
            return PhoneUtils.getRegionCodeByPhone(e164);
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return null;
        }
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }
}

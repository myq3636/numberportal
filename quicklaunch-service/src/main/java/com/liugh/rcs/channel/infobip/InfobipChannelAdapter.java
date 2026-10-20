package com.liugh.rcs.channel.infobip;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.RcsMoInfo;
import com.liugh.rcs.channel.RcsCallbackResult;
import com.liugh.rcs.channel.RcsChannelAdapter;
import com.liugh.rcs.channel.RcsChannelCapability;
import com.liugh.rcs.channel.RcsDrEvent;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.model.RcsContent;
import com.liugh.service.util.ThirdPartyLoginHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * Infobip 通道适配器。发送走 /rcs/2/messages；回调解析统一大写 groupName（修 B7）。
 * 由原 RcsInfobipService 的 send / handleDr / handleInbound 平移而来，行为等价。
 */
@Component
@Slf4j
public class InfobipChannelAdapter implements RcsChannelAdapter {

    public static final String CODE = "INFOBIP";

    private static final Set<RcsChannelCapability> CAPABILITIES =
            Collections.unmodifiableSet(EnumSet.of(
                    RcsChannelCapability.MESSAGE_SEND,
                    RcsChannelCapability.CAPABILITY_QUERY_SYNC,
                    RcsChannelCapability.CAPABILITY_QUERY_ASYNC,
                    RcsChannelCapability.OUTBOUND_EVENT,
                    RcsChannelCapability.SENDER_MANAGEMENT,
                    RcsChannelCapability.TEST_NUMBER_MANAGEMENT,
                    RcsChannelCapability.TEMPLATE_MANAGEMENT,
                    RcsChannelCapability.SENDER_LAUNCH_STATUS,
                    RcsChannelCapability.SENDER_LAUNCH_MANAGEMENT));

    @Override
    public String channelCode() {
        return CODE;
    }

    @Override
    public Set<RcsChannelCapability> capabilities() {
        return CAPABILITIES;
    }

    @Override
    public String send(RcsContent content, RcsSendCtx ctx) throws Exception {
        Map<String, Object> payload = InfobipPayloadMapper.toSendPayload(
                content, ctx.getSender(), ctx.getRecipient(), ctx.getMsgId(), ctx.getNotifyUrl(),
                ctx.getTtl(), ctx.getSmsSender(), ctx.getFallbackText());
        // 连接凭证来自供应商 admin_info：baseUrl=admin.url，apiKey=admin.appId
        String authHeader = "App " + ctx.getApiKey();
        return ThirdPartyLoginHelper.sendInfoBipRCSMsg(payload, ctx.getBaseUrl(), authHeader);
    }

    @Override
    public RcsCallbackResult parseCallback(JSONObject request) {
        RcsCallbackResult result = new RcsCallbackResult();
        JSONArray results = request.getJSONArray("results");
        if (results == null || results.isEmpty()) {
            log.warn("infobip callback contains no results");
            return result;
        }
        for (int i = 0; i < results.size(); i++) {
            JSONObject r = results.getJSONObject(i);
            if (r.containsKey("message")) {
                result.getInboundEvents().add(parseInbound(r));
            } else {
                result.getDrEvents().add(parseDr(r));
            }
        }
        return result;
    }

    /** 修 B7：统一用大写 groupName 判定。 */
    private RcsDrEvent parseDr(JSONObject r) {
        RcsDrEvent e = new RcsDrEvent();
        e.setOurMsgId(r.getString("callbackData"));
        e.setChannelMsgId(r.getString("messageId"));

        JSONObject status = r.getJSONObject("status");
        String groupName = status == null ? null : status.getString("groupName");
        boolean seen = r.containsKey("seenAt") || "SEEN".equalsIgnoreCase(groupName) || "READ".equalsIgnoreCase(groupName);

        JSONObject price = r.getJSONObject("price");
        if (price != null) {
            if (price.get("pricePerMessage") != null) {
                e.setCost(String.valueOf(price.get("pricePerMessage")));
            }
            e.setCurrency(price.getString("currency"));
        }

        if (seen) {
            e.setStatus(9);
        } else if ("DELIVERED".equalsIgnoreCase(groupName)) {
            e.setStatus(3);
        } else if ("EXPIRED".equalsIgnoreCase(groupName)) {
            e.setStatus(5);
        } else if ("REJECTED".equalsIgnoreCase(groupName) || "UNDELIVERABLE".equalsIgnoreCase(groupName)) {
            e.setStatus(4);
            JSONObject error = r.getJSONObject("error");
            if (error != null) {
                e.setErrorCode(error.getString("id"));
                e.setErrorReason(error.getString("name"));
            }
        } else {
            // PENDING / ACCEPTED：仅回填价格
            e.setStatus(null);
        }
        return e;
    }

    private RcsMoInfo parseInbound(JSONObject r) {
        JSONObject message = r.getJSONObject("message");
        String type = message == null ? null : message.getString("type");
        long now = System.currentTimeMillis();

        RcsMoInfo mo = new RcsMoInfo();
        mo.setChannelCode(CODE);
        // Temporarily keep the provider sender identity. Callback orchestration
        // resolves it to the platform senderId and tenant before persistence.
        mo.setAgentId(firstNonBlank(r.getString("to"),
                r.getString("destination"), r.getString("recipient")));
        mo.setMsisdn(firstNonBlank(r.getString("from"), r.getString("sender")));
        mo.setClientMsgId(r.getString("messageId"));
        mo.setReceiveTime(now);
        mo.setCreateTime(now);
        mo.setPushStatus(0);
        mo.setPushRetry(0);

        if (message != null && message.getString("postbackData") != null) {
            mo.setEventType("SUGGESTION_RESPONSE");
            mo.setContentType("SUGGESTION");
            mo.setPostbackData(message.getString("postbackData"));
            mo.setSuggestionText(message.getString("text"));
            mo.setSuggestionType(message.getString("suggestionType"));
        } else if ("TEXT".equalsIgnoreCase(type)) {
            mo.setEventType("USER_MESSAGE");
            mo.setContentType("TEXT");
            mo.setText(message.getString("text"));
        } else if ("LOCATION".equalsIgnoreCase(type)) {
            mo.setEventType("USER_MESSAGE");
            mo.setContentType("LOCATION");
            mo.setLatitude(message.getDouble("latitude"));
            mo.setLongitude(message.getDouble("longitude"));
        } else if (type != null) {
            mo.setEventType("USER_MESSAGE");
            mo.setContentType("FILE");
            mo.setFileUri(message.getString("url"));
            mo.setMimeType(message.getString("mimeType"));
        } else {
            mo.setEventType("USER_MESSAGE");
            mo.setContentType("TEXT");
        }
        return mo;
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

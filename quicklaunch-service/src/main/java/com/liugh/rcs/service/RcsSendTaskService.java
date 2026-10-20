package com.liugh.rcs.service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsSendTask;
import com.liugh.mapper.RCSCdrInfoMapper;
import com.liugh.mapper.RcsSendTaskMapper;
import com.liugh.rcs.channel.RcsChannelAdapter;
import com.liugh.rcs.channel.RcsChannelAdapterRegistry;
import com.liugh.rcs.channel.RcsChannelSubmitException;
import com.liugh.rcs.service.RcsCallbackService;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.channel.infobip.RcsContentParser;
import com.liugh.rcs.model.RcsCard;
import com.liugh.rcs.model.RcsContent;
import com.liugh.rcs.model.RcsContentValidator;
import com.liugh.rcs.model.RcsMedia;
import com.liugh.service.util.GlobalMap;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RcsSendTaskService {

    private final RcsSendTaskMapper taskMapper;
    private final RCSCdrInfoMapper cdrMapper;
    private final RcsMessagePersistenceService persistenceService;
    private final RcsChannelAdapterRegistry adapterRegistry;
    private final RcsFileService fileService;
    private final RcsCallbackService callbackService;

    private boolean enabled;
    private String projectUrl;

    @Autowired
    public RcsSendTaskService(RcsSendTaskMapper taskMapper,
            RCSCdrInfoMapper cdrMapper,
            RcsMessagePersistenceService persistenceService,
            RcsChannelAdapterRegistry adapterRegistry,
            RcsFileService fileService,
            RcsCallbackService callbackService) {
        this.taskMapper = taskMapper;
        this.cdrMapper = cdrMapper;
        this.persistenceService = persistenceService;
        this.adapterRegistry = adapterRegistry;
        this.fileService = fileService;
        this.callbackService = callbackService;
    }

    @Value("${rcs.send-task.enabled:true}")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Value("${project.url:}")
    public void setProjectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
    }

    @Scheduled(fixedDelayString = "${rcs.send-task.scan-ms:2000}")
    public void deliverPending() {
        if (!enabled) {
            return;
        }
        List<RcsSendTask> tasks = taskMapper.selectList(
                new EntityWrapper<RcsSendTask>()
                        .in("task_status", Arrays.asList("QUEUED", "RETRY"))
                        .orderBy("next_attempt_time", true)
                        .orderBy("create_time", true)
                        .last("LIMIT 100"));
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        Date now = new Date();
        for (RcsSendTask task : tasks) {
            if (task.getNextAttemptTime() != null
                    && task.getNextAttemptTime().after(now)) {
                continue;
            }
            if (persistenceService.claim(task)) {
                process(task);
            }
        }
    }

    public void process(RcsSendTask task) {
        RCSCdrInfo cdr = first(cdrMapper.selectList(
                new EntityWrapper<RCSCdrInfo>()
                        .eq("msg_id", task.getMsgId())));
        if (cdr == null) {
            persistenceService.markTaskRetry(task, "cdr not found");
            return;
        }

        RcsChannelAdapter adapter =
                adapterRegistry.get(task.getChannelCode());
        AdminInfo supplier = GlobalMap.rcsadmin.get(String.valueOf(
                task.getRssid()));
        if (adapter == null || supplier == null) {
            failOrRetry(task, cdr, "adapter or supplier not found");
            return;
        }

        try {
            JSONObject raw = JSONObject.parseObject(task.getRequestJson());
            RcsContent content = RcsContentParser.parse(raw);
            String error = RcsContentValidator.validate(content);
            if (error != null) {
                failOrRetry(task, cdr, error);
                return;
            }
            resolveMedia(content);
            String channelMsgId;
            try {
                channelMsgId = adapter.send(content,
                        buildCtx(task, cdr, supplier, raw));
            } catch (RcsChannelSubmitException e) {
                persistenceService.markTaskFailed(task, cdr,
                        e.getErrorCode(), e.getErrorReason());
                return;
            }
            if (channelMsgId != null) {
                if (adapter.isImmediateDelivery()) {
                    // 即时投递通道（如 BLACKHOLE）：直接推进到 status=3 并触发客户 DR 通知。
                    persistenceService.markDirectDelivered(cdr, channelMsgId);
                    callbackService.fireSyntheticDelivery(cdr);
                } else {
                    persistenceService.markSubmitted(cdr, channelMsgId);
                }
                persistenceService.markTaskSucceeded(task);
            } else {
                failOrRetry(task, cdr, "submit returned null");
            }
        } catch (Exception e) {
            log.error("rcs async send failed, msgId:{}", task.getMsgId(), e);
            failOrRetry(task, cdr, safeError(e));
        }
    }

    private void failOrRetry(RcsSendTask task, RCSCdrInfo cdr, String error) {
        if (attemptCount(task) + 1 >= maxRetries(task)) {
            persistenceService.markTaskFailed(task, cdr, error);
        } else {
            persistenceService.markTaskRetry(task, error);
        }
    }

    private RcsSendCtx buildCtx(RcsSendTask task, RCSCdrInfo cdr,
            AdminInfo supplier, JSONObject raw) {
        RcsSendCtx ctx = new RcsSendCtx();
        ctx.setSsid(task.getSsid() == null ? 0 : task.getSsid());
        ctx.setSender(cdr.getSender());
        ctx.setRecipient(cdr.getMsisdn());
        ctx.setMsgId(cdr.getMsgId());
        ctx.setNotifyUrl(buildNotifyUrl(task.getChannelCode()));
        ctx.setTtl(raw.getInteger("ttl"));
        ctx.setChannelCode(task.getChannelCode());
        ctx.setRssid(task.getRssid());
        ctx.setBaseUrl(supplier.getUrl());
        ctx.setApiKey(supplier.getAppId());
        ctx.setAuthUser(supplier.getAppId());
        ctx.setAuthPass(supplier.getAppSecurity());
        ctx.setSmsSender(supplier.getSmsSender());
        ctx.setFallbackText(raw.getString("fallbackText"));
        ctx.setCountryCode(cdr.getCountryCode());
        ctx.setChannelSender(firstNonBlank(supplier.getChannelSender(), cdr.getSender()));
        return ctx;
    }

    private void resolveMedia(RcsContent content) {
        if (content.getFile() != null) {
            resolveMediaObj(content.getFile());
        }
        if (content.getCard() != null
                && content.getCard().getMedia() != null) {
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

    private void resolveMediaObj(RcsMedia media) {
        media.setUrl(fileService.resolve(media.getUrl()));
        if (media.getThumbnailUrl() != null) {
            media.setThumbnailUrl(fileService.resolve(media.getThumbnailUrl()));
        }
    }

    private String buildNotifyUrl(String channelCode) {
        String base = projectUrl;
        if (base != null && !base.endsWith("/")) {
            base = base + "/";
        }
        return base + "rcs/callback/" + channelCode.toLowerCase();
    }

    private static int attemptCount(RcsSendTask task) {
        return task.getAttemptCount() == null ? 0 : task.getAttemptCount();
    }

    private static int maxRetries(RcsSendTask task) {
        return task.getMaxRetries() == null
                ? 3 : Math.max(1, task.getMaxRetries());
    }

    private static String safeError(Exception error) {
        String value = error.getClass().getSimpleName();
        if (error.getMessage() != null
                && !error.getMessage().trim().isEmpty()) {
            value += ": " + error.getMessage();
        }
        return value.length() > 512 ? value.substring(0, 512) : value;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }

    private static <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}

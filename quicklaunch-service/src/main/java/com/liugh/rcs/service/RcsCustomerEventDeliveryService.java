package com.liugh.rcs.service;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.RcsCustomerEvent;
import com.liugh.entity.RcsDeliveryConfig;
import com.liugh.entity.RcsEventDeliveryAttempt;
import com.liugh.mapper.RcsCustomerEventMapper;
import com.liugh.mapper.RcsEventDeliveryAttemptMapper;

/**
 * Delivers normalized customer events outside provider callback threads.
 */
@Service
public class RcsCustomerEventDeliveryService {

    private final RcsCustomerEventMapper eventMapper;
    private final RcsEventDeliveryAttemptMapper attemptMapper;
    private final RcsDeliveryConfigService configService;
    private final RcsCustomerEventHttpClient httpClient;

    private boolean enabled;

    @Autowired
    public RcsCustomerEventDeliveryService(RcsCustomerEventMapper eventMapper,
            RcsEventDeliveryAttemptMapper attemptMapper,
            RcsDeliveryConfigService configService,
            RcsCustomerEventHttpClient httpClient) {
        this.eventMapper = eventMapper;
        this.attemptMapper = attemptMapper;
        this.configService = configService;
        this.httpClient = httpClient;
    }

    @Value("${rcs.customer-events.enabled:false}")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${rcs.customer-events.scan-ms:5000}")
    public void deliverPending() {
        if (!enabled) {
            return;
        }
        List<RcsCustomerEvent> events = eventMapper.selectList(
                new EntityWrapper<RcsCustomerEvent>()
                        .eq("deliverable", true)
                        .in("delivery_status",
                                Arrays.asList("PENDING", "RETRY"))
                        .orderBy("next_attempt_time", true)
                        .orderBy("create_time", true)
                        .last("LIMIT 100"));
        if (events == null) {
            return;
        }
        Date now = new Date();
        for (RcsCustomerEvent event : events) {
            if (event.getNextAttemptTime() == null
                    || !event.getNextAttemptTime().after(now)) {
                deliver(event);
            }
        }
    }

    public void deliver(RcsCustomerEvent event) {
        if (event == null || !Boolean.TRUE.equals(event.getDeliverable())
                || "DELIVERED".equals(event.getDeliveryStatus())) {
            return;
        }
        Integer previousAttempt = event.getAttemptCount() == null
                ? 0 : event.getAttemptCount();
        if (!claim(event, previousAttempt)) {
            return;
        }

        RcsDeliveryConfig config = configService.findBySsid(event.getSsid());
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            event.setDeliveryStatus("RETRY");
            event.setNextAttemptTime(new Date(System.currentTimeMillis()
                    + 300000L));
            event.setUpdateTime(new Date());
            eventMapper.updateById(event);
            return;
        }

        int attemptNo = previousAttempt + 1;
        String requestId = "req_"
                + UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000L);

        Date started = new Date();
        Integer httpStatus = null;
        String result = "FAILED";
        String errorSummary = null;
        try {
            String secret = configService.decryptSecret(config);
            Map<String, String> headers = headers(event, requestId,
                    timestamp, secret);
            httpStatus = httpClient.post(config.getEventUrl(),
                    event.getPayloadJson(), headers);
            if (httpStatus >= 200 && httpStatus < 300) {
                result = "SUCCESS";
            } else {
                errorSummary = "HTTP_" + httpStatus;
            }
        } catch (Exception e) {
            errorSummary = safeError(e);
        }

        Date completed = new Date();
        RcsEventDeliveryAttempt attempt = new RcsEventDeliveryAttempt();
        attempt.setSsid(event.getSsid());
        attempt.setEventId(event.getEventId());
        attempt.setAttemptNo(attemptNo);
        attempt.setRequestId(requestId);
        attempt.setStartedAt(started);
        attempt.setCompletedAt(completed);
        attempt.setHttpStatus(httpStatus);
        attempt.setResult(result);
        attempt.setErrorSummary(errorSummary);
        attempt.setCreateTime(completed);
        attemptMapper.insert(attempt);

        event.setAttemptCount(attemptNo);
        if ("SUCCESS".equals(result)) {
            event.setDeliveryStatus("DELIVERED");
            event.setNextAttemptTime(null);
        } else if (attemptNo >= retries(config)) {
            event.setDeliveryStatus("FAILED");
            event.setNextAttemptTime(null);
        } else {
            event.setDeliveryStatus("RETRY");
            event.setNextAttemptTime(new Date(System.currentTimeMillis()
                    + backoffMillis(attemptNo)));
        }
        event.setUpdateTime(completed);
        eventMapper.updateById(event);
    }

    public RcsCustomerEvent manualRetry(Integer ssid, String eventId) {
        RcsCustomerEvent event = first(eventMapper.selectList(
                new EntityWrapper<RcsCustomerEvent>()
                        .eq("ssid", ssid)
                        .eq("event_id", eventId)));
        if (event == null) {
            throw new RcsCustomerAccessException("RESOURCE_NOT_FOUND");
        }
        if ("DELIVERED".equals(event.getDeliveryStatus())
                || !Boolean.TRUE.equals(event.getDeliverable())) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        event.setDeliveryStatus("PENDING");
        event.setNextAttemptTime(null);
        event.setUpdateTime(new Date());
        eventMapper.updateById(event);
        return event;
    }

    private boolean claim(RcsCustomerEvent event, int previousAttempt) {
        RcsCustomerEvent claim = new RcsCustomerEvent();
        claim.setDeliveryStatus("DELIVERING");
        claim.setUpdateTime(new Date());
        int updated = eventMapper.update(claim,
                new EntityWrapper<RcsCustomerEvent>()
                        .eq("event_id", event.getEventId())
                        .eq("delivery_status", event.getDeliveryStatus())
                        .eq("attempt_count", previousAttempt));
        if (updated > 0) {
            event.setDeliveryStatus("DELIVERING");
            return true;
        }
        return false;
    }

    private Map<String, String> headers(RcsCustomerEvent event,
            String requestId, String timestamp, String secret) {
        Map<String, String> headers =
                new LinkedHashMap<String, String>();
        headers.put("Content-Type", "application/json");
        headers.put("X-RCS-Event-Id", event.getEventId());
        headers.put("X-RCS-Request-Id", requestId);
        headers.put("X-RCS-Timestamp", timestamp);
        headers.put("X-RCS-Signature", RcsEventSignature.sign(secret,
                event.getEventId(), timestamp, event.getPayloadJson()));
        return headers;
    }

    private int retries(RcsDeliveryConfig config) {
        return config.getMaxRetries() == null
                ? 5 : Math.max(1, config.getMaxRetries());
    }

    private long backoffMillis(int attemptNo) {
        long seconds = 30L * (1L << Math.min(attemptNo - 1, 7));
        return Math.min(seconds, 3600L) * 1000L;
    }

    private String safeError(Exception error) {
        String value = error.getClass().getSimpleName();
        if (error.getMessage() != null
                && !error.getMessage().trim().isEmpty()) {
            value += ": " + error.getMessage();
        }
        return value.length() > 512 ? value.substring(0, 512) : value;
    }

    private static <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}

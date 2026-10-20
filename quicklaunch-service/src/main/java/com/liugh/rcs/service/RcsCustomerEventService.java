package com.liugh.rcs.service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsCapabilityBatch;
import com.liugh.entity.RcsCapabilityResult;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.entity.RcsCustomerEvent;
import com.liugh.entity.RcsMoInfo;
import com.liugh.mapper.RcsCapabilityBatchMapper;
import com.liugh.mapper.RcsCapabilityResultMapper;
import com.liugh.mapper.RcsChannelAgentMapper;
import com.liugh.mapper.RcsCustomerEventMapper;
import com.liugh.rcs.channel.RcsDrEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Normalizes provider callbacks into tenant-scoped customer events.
 */
@Service
@Slf4j
public class RcsCustomerEventService {

    private final RcsCustomerEventMapper eventMapper;
    private final RcsChannelAgentMapper bindingMapper;
    private final RcsCapabilityBatchMapper batchMapper;
    private final RcsCapabilityResultMapper resultMapper;

    @Autowired
    public RcsCustomerEventService(RcsCustomerEventMapper eventMapper,
            RcsChannelAgentMapper bindingMapper,
            RcsCapabilityBatchMapper batchMapper,
            RcsCapabilityResultMapper resultMapper) {
        this.eventMapper = eventMapper;
        this.bindingMapper = bindingMapper;
        this.batchMapper = batchMapper;
        this.resultMapper = resultMapper;
    }

    public RcsCustomerEvent recordDr(RCSCdrInfo cdr, RcsDrEvent dr) {
        String eventType = drType(dr == null ? null : dr.getStatus());
        boolean deliverable = !"UNKNOWN_DELIVERY_STATUS".equals(eventType);
        String providerMessageId = firstNonBlank(
                dr == null ? null : dr.getChannelMsgId(),
                cdr == null ? null : cdr.getChannelMsgId());
        String providerKey = "dr:" + providerMessageId + ":"
                + (dr == null ? null : dr.getStatus());

        JSONObject data = new JSONObject();
        data.put("recipient", cdr == null ? null : cdr.getMsisdn());
        data.put("status", eventType);
        if (dr != null && !isBlank(dr.getErrorCode())) {
            data.put("errorCode", dr.getErrorCode());
            data.put("errorSummary", dr.getErrorReason());
        }
        return store(cdr == null ? null : cdr.getSsid(), eventType,
                resolveSenderId(cdr), cdr == null ? null : cdr.getMsgId(),
                cdr == null ? null : cdr.getChannelCode(), providerKey,
                data, deliverable);
    }

    public RcsCustomerEvent recordInbound(RcsMoInfo inbound) {
        String eventType = inboundType(inbound == null
                ? null : inbound.getEventType());
        boolean deliverable = !"UNKNOWN_EVENT".equals(eventType)
                && inbound != null && inbound.getSsid() != null;
        String messageId = inbound == null || inbound.getId() == null
                ? null : "mo_" + inbound.getId();
        String providerKey = "mo:"
                + (inbound == null ? null : inbound.getClientMsgId())
                + ":" + (inbound == null ? null : inbound.getEventType());

        JSONObject data = new JSONObject();
        if (inbound != null) {
            data.put("from", inbound.getMsisdn());
            if (!isBlank(inbound.getContentType())) {
                data.put("contentType", inbound.getContentType());
            }
            if (!isBlank(inbound.getText())) {
                data.put("text", inbound.getText());
            }
            if (!isBlank(inbound.getFileUri())) {
                data.put("fileUri", inbound.getFileUri());
                data.put("mimeType", inbound.getMimeType());
                data.put("thumbnailUri", inbound.getThumbnailUri());
            }
            if (inbound.getLatitude() != null || inbound.getLongitude() != null) {
                data.put("latitude", inbound.getLatitude());
                data.put("longitude", inbound.getLongitude());
            }
            if (!isBlank(inbound.getPostbackData())) {
                data.put("postbackData", inbound.getPostbackData());
                data.put("suggestionText", inbound.getSuggestionText());
                data.put("suggestionType", inbound.getSuggestionType());
            }
        }
        return store(inbound == null ? null : inbound.getSsid(), eventType,
                inbound == null ? null : inbound.getAgentId(), messageId,
                inbound == null ? null : inbound.getChannelCode(), providerKey,
                data, deliverable);
    }

    public RcsCustomerEvent recordCapabilityResults(String channelCode,
            JSONObject callback) {
    	JSONObject value = callback.getJSONObject("capabilityCheckResult");
    	if(value == null) {
    		return store(null, "UNKNOWN_EVENT", null, null, channelCode,
                    "capability:null", new JSONObject(), false);
    	}
        String upstreamBulkId = value == null ? null
                : value.getString("bulkId");
        log.info("upstreamBulkId:{}", upstreamBulkId);
        RcsCapabilityBatch batch = first(batchMapper.selectList(
                new EntityWrapper<RcsCapabilityBatch>()
                        .eq("channel_code", channelCode)
                        .eq("upstream_bulk_id", upstreamBulkId)));
        if (batch == null) {
            return store(null, "UNKNOWN_EVENT", null, null, channelCode,
                    "capability:" + upstreamBulkId, new JSONObject(), false);
        }
        int success = 0;
        int failure = 0;
        JSONArray customerResults = new JSONArray();
        Date now = new Date();
        
            
            log.info("capabilityCheckResult:{}", value);
            String recipient = firstNonBlank(value.getString("phoneNumber"),
                    value.getString("to"));
            String status = firstNonBlank(value.getString("code"),
                    value.getString("status"));
            Object capabilities = value.get("features");
            if (capabilities == null) {
                capabilities = value.get("capabilities");
            }
            JSONObject error = value.getJSONObject("error");

            RcsCapabilityResult stored = new RcsCapabilityResult();
            stored.setSsid(batch.getSsid());
            stored.setBatchId(batch.getBatchId());
            stored.setSenderId(batch.getSenderId());
            stored.setRecipient(recipient);
            stored.setStatus(status == null ? "UNKNOWN" : status);
            stored.setCapabilitiesJson(capabilities == null
                    ? null : JSONObject.toJSONString(capabilities));
            if (error != null) {
                stored.setErrorCode(firstNonBlank(error.getString("id"),
                        error.getString("code")));
                stored.setErrorSummary(firstNonBlank(error.getString("name"),
                        error.getString("description")));
                failure++;
            } else {
                success++;
            }
            stored.setCreateTime(now);
            stored.setUpdateTime(now);
            try {
                resultMapper.insert(stored);
            } catch (RuntimeException ignoredDuplicate) {
                // Unique(batch_id, recipient) makes callback replay idempotent.
            }

            JSONObject customerResult = new JSONObject();
            customerResult.put("recipient", recipient);
            customerResult.put("status", stored.getStatus());
            if (capabilities != null) {
                customerResult.put("capabilities", capabilities);
            }
            if (stored.getErrorCode() != null) {
                customerResult.put("errorCode", stored.getErrorCode());
                customerResult.put("errorSummary", stored.getErrorSummary());
            }
            customerResults.add(customerResult);

        batch.setStatus("COMPLETED");
        batch.setSuccessCount(success);
        batch.setFailureCount(failure);
        batch.setCompletedAt(now);
        batch.setUpdateTime(now);
        batchMapper.updateById(batch);

        JSONObject data = new JSONObject();
        data.put("batchId", batch.getBatchId());
        data.put("status", batch.getStatus());
        data.put("results", customerResults);
        return store(batch.getSsid(), "CAPABILITY_RESULTS_READY",
                batch.getSenderId(), null, channelCode,
                "capability:" + upstreamBulkId, data, true);
    }

    public RcsCustomerEvent createTestEvent(Integer ssid) {
        JSONObject data = new JSONObject();
        data.put("message", "RCS customer event delivery test");
        String key = "test:" + UUID.randomUUID().toString();
        return store(ssid, "DELIVERY_TEST", null, null, "PLATFORM",
                key, data, true);
    }

    /**
     * Records an already tenant-resolved provider management event.
     * Internal routing fields are removed defensively before customer payload
     * construction.
     */
    public RcsCustomerEvent recordManagementEvent(Integer ssid,
            String eventType, String senderId, String messageId,
            String providerKey, JSONObject data) {
        if (ssid == null || ssid <= 0 || isBlank(eventType)
                || isBlank(providerKey)) {
            throw new IllegalArgumentException(
                    "Owned management event fields are required");
        }
        JSONObject customerData = data == null ? new JSONObject()
                : JSONObject.parseObject(data.toJSONString());
        sanitizeCustomerData(customerData);
        return store(ssid, eventType, senderId, messageId, "INFOBIP",
                providerKey, customerData, true);
    }

    private RcsCustomerEvent store(Integer ssid, String eventType,
            String senderId, String messageId, String channelCode,
            String providerKey, JSONObject data, boolean deliverable) {
        RcsCustomerEvent existing = first(eventMapper.selectList(
                new EntityWrapper<RcsCustomerEvent>()
                        .eq("channel_code", channelCode)
                        .eq("provider_event_key", providerKey)));
        if (existing != null) {
            return existing;
        }

        Date now = new Date();
        RcsCustomerEvent event = new RcsCustomerEvent();
        event.setEventId("evt_" + UUID.randomUUID().toString().replace("-", ""));
        // Unknown/unowned provider events are retained internally under tenant
        // 0 and are never deliverable, while the table keeps ssid NOT NULL.
        event.setSsid(ssid == null ? 0 : ssid);
        event.setEventType(eventType);
        event.setOccurredAt(now);
        event.setSenderId(senderId);
        event.setMessageId(messageId);
        event.setChannelCode(channelCode);
        event.setProviderEventKey(providerKey);
        event.setDeliverable(deliverable);
        event.setDeliveryStatus(deliverable ? "PENDING" : "NOT_DELIVERABLE");
        event.setAttemptCount(0);
        event.setCreateTime(now);
        event.setUpdateTime(now);

        JSONObject payload = new JSONObject();
        payload.put("eventId", event.getEventId());
        payload.put("type", eventType);
        payload.put("occurredAt", now.getTime());
        if (!isBlank(senderId)) {
            payload.put("senderId", senderId);
        }
        if (!isBlank(messageId)) {
            payload.put("messageId", messageId);
        }
        payload.put("data", data == null ? new JSONObject() : data);
        event.setPayloadJson(payload.toJSONString());
        try {
            eventMapper.insert(event);
            return event;
        } catch (RuntimeException duplicate) {
            RcsCustomerEvent concurrent = first(eventMapper.selectList(
                    new EntityWrapper<RcsCustomerEvent>()
                            .eq("channel_code", channelCode)
                            .eq("provider_event_key", providerKey)));
            if (concurrent != null) {
                return concurrent;
            }
            throw duplicate;
        }
    }

    private String resolveSenderId(RCSCdrInfo cdr) {
        if (cdr == null || isBlank(cdr.getSender())
                || isBlank(cdr.getChannelCode())) {
            return null;
        }
        RcsChannelAgent binding = first(bindingMapper.selectList(
                new EntityWrapper<RcsChannelAgent>()
                        .eq("channel_code", cdr.getChannelCode())
                        .eq("sender", cdr.getSender())
                        .eq("ssid", cdr.getSsid())));
        return binding == null ? null : binding.getAgentId();
    }

    private static void sanitizeCustomerData(Object value) {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            for (String forbidden : new String[]{"channel", "channelCode",
                    "provider", "rssid", "platform", "apiKey",
                    "authorization", "appId", "appSecurity"}) {
                removeIgnoreCase(object, forbidden);
            }
            for (Object child : object.values()) {
                sanitizeCustomerData(child);
            }
        } else if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.size(); i++) {
                sanitizeCustomerData(array.get(i));
            }
        }
    }

    private static void removeIgnoreCase(JSONObject object, String name) {
        String found = null;
        for (String key : object.keySet()) {
            if (name.equalsIgnoreCase(key)) {
                found = key;
                break;
            }
        }
        if (found != null) {
            object.remove(found);
        }
    }

    private static String drType(Integer status) {
        if (status == null) {
            return "UNKNOWN_DELIVERY_STATUS";
        }
        switch (status) {
        case 3:
            return "MESSAGE_DELIVERED";
        case 9:
            return "MESSAGE_SEEN";
        case 5:
            return "MESSAGE_EXPIRED";
        case 4:
            return "MESSAGE_FAILED";
        case 12:
            return "MESSAGE_FALLBACK";
        default:
            return "UNKNOWN_DELIVERY_STATUS";
        }
    }

    private static String inboundType(String eventType) {
        if ("USER_MESSAGE".equalsIgnoreCase(eventType)) {
            return "USER_MESSAGE";
        }
        if ("SUGGESTION_RESPONSE".equalsIgnoreCase(eventType)) {
            return "SUGGESTION_RESPONSE";
        }
        if ("IS_TYPING".equalsIgnoreCase(eventType)
                || "TYPING_INDICATOR".equalsIgnoreCase(eventType)) {
            return "TYPING_INDICATOR";
        }
        return "UNKNOWN_EVENT";
    }

    private static <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static String firstNonBlank(String... values) {
        if (values != null) {
            for (String value : values) {
                if (!isBlank(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

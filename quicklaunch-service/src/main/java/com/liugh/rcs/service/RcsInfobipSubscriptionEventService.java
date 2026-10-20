package com.liugh.rcs.service;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.entity.RcsLaunchRequest;
import com.liugh.entity.RcsTemplateBinding;
import com.liugh.mapper.RCSCdrInfoMapper;
import com.liugh.mapper.RcsChannelAgentMapper;
import com.liugh.mapper.RcsLaunchRequestMapper;
import com.liugh.mapper.RcsTemplateBindingMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Normalizes Infobip subscription webhooks after resolving a unique owner.
 */
@Service
@Slf4j
public class RcsInfobipSubscriptionEventService {

    private static final String INFOBIP = "INFOBIP";

    private final RcsChannelAgentMapper bindingMapper;
    private final RcsTemplateBindingMapper templateMapper;
    private final RcsLaunchRequestMapper launchMapper;
    private final RCSCdrInfoMapper cdrMapper;
    private final RcsCustomerEventService customerEvents;

    @Autowired
    public RcsInfobipSubscriptionEventService(
            RcsChannelAgentMapper bindingMapper,
            RcsTemplateBindingMapper templateMapper,
            RcsLaunchRequestMapper launchMapper,
            RCSCdrInfoMapper cdrMapper,
            RcsCustomerEventService customerEvents) {
        this.bindingMapper = bindingMapper;
        this.templateMapper = templateMapper;
        this.launchMapper = launchMapper;
        this.cdrMapper = cdrMapper;
        this.customerEvents = customerEvents;
    }

    public boolean handle(Integer rssid, JSONObject raw) {
        if (rssid == null || raw == null) {
            return true;
        }
        try {
            JSONArray results = raw.getJSONArray("results");
            if (results != null) {
                for (int i = 0; i < results.size(); i++) {
                    JSONObject value = results.getJSONObject(i);
                    if (value != null) {
                        handleOne(rssid, value);
                    }
                }
            } else if (!isBlank(raw.getString("name"))
                    && !isBlank(raw.getString("templateStatus"))) {
                handleTemplate(rssid, raw);
            } else {
                log.warn("Ignored unsupported Infobip subscription event, "
                        + "rssid:{}", rssid);
            }
        } catch (Exception e) {
            // A malformed or unowned provider event must not trigger an
            // unbounded Infobip retry storm.
            log.error("Infobip subscription event processing failed, rssid:{}",
                    rssid, e);
        }
        return true;
    }

    private void handleOne(Integer rssid, JSONObject value) {
        String providerType = providerType(value);
        if ("RCS_SENDER_STATUS_UPDATE".equals(providerType)) {
            handleSender(rssid, value, "SENDER_UPDATED");
        } else if ("RCS_SENDER_LAUNCH_STATUS_UPDATE".equals(providerType)) {
            handleSender(rssid, value, "SENDER_LAUNCH_UPDATED");
        } else if ("RCS_TEST_NUMBER_STATUS_UPDATE".equals(providerType)) {
            handleSender(rssid, value, "TEST_NUMBER_UPDATED");
        } else if ("RESOURCE_REQUEST_STATUS_UPDATE".equals(providerType)) {
            handleResource(rssid, value, "RESOURCE_REQUEST_UPDATED");
        } else if ("RESOURCE_REQUEST_REMINDER".equals(providerType)) {
            handleResource(rssid, value, "RESOURCE_REQUEST_REMINDER");
        } else if ("RESOURCE_REQUEST_FOC_DATE_CHANGE".equals(providerType)) {
            handleResource(rssid, value,
                    "RESOURCE_REQUEST_FOC_DATE_CHANGED");
        } else if ("TYPING_INDICATOR".equals(providerType)) {
            handleTyping(rssid, value);
        } else if ("CONVERSATION_STARTED".equals(providerType)) {
            handleConversation(rssid, value);
        } else {
            log.warn("Ignored unsupported Infobip subscription event type:{}, "
                    + "rssid:{}", providerType, rssid);
        }
    }

    private void handleSender(Integer rssid, JSONObject value,
            String customerType) {
        RcsChannelAgent binding = findBinding(rssid,
                value.getString("senderName"));
        if (binding == null) {
            log.warn("Ignored unowned Infobip sender event, rssid:{}",
                    rssid);
            return;
        }
        JSONObject data = new JSONObject();
        put(data, "senderName", value.get("senderName"));
        put(data, "status", value.get("senderStatus"));
        put(data, "testNumber", value.get("testNumber"));
        put(data, "testNumberStatus", value.get("testNumberStatus"));
        put(data, "coverage", value.get("coverage"));
        put(data, "comment", value.get("comment"));
        String key = key(customerType, value.getString("senderName"),
                firstNonBlank(value.getString("testNumber"),
                        value.getString("updatedAt"),
                        value.getString("completedAt")),
                firstNonBlank(value.getString("testNumberStatus"),
                        value.getString("senderStatus")));
        customerEvents.recordManagementEvent(binding.getSsid(), customerType,
                binding.getAgentId(), null, key, data);
    }

    private void handleTemplate(Integer rssid, JSONObject value) {
        String name = value.getString("name");
        RcsTemplateBinding binding = first(templateMapper.selectList(
                new EntityWrapper<RcsTemplateBinding>()
                        .eq("rssid", rssid)
                        .eq("template_name", name)));
        if (binding == null || !same(rssid, binding.getRssid())
                || binding.getSsid() == null) {
            log.warn("Ignored unowned Infobip template event, rssid:{}",
                    rssid);
            return;
        }
        binding.setStatus(value.getString("templateStatus"));
        binding.setUpdateTime(new Date());
        templateMapper.updateById(binding);

        JSONObject data = new JSONObject();
        data.put("templateName", name);
        put(data, "status", value.get("templateStatus"));
        put(data, "comment", value.get("comment"));
        String key = key("TEMPLATE_UPDATED", name,
                value.getString("updatedAt"),
                value.getString("templateStatus"));
        customerEvents.recordManagementEvent(binding.getSsid(),
                "TEMPLATE_UPDATED", binding.getSenderId(), null, key, data);
    }

    private void handleResource(Integer rssid, JSONObject value,
            String customerType) {
        JSONObject content = value.getJSONObject("content");
        String upstreamId = content == null ? null : content.getString("id");
        RcsLaunchRequest launch = first(launchMapper.selectList(
                new EntityWrapper<RcsLaunchRequest>()
                        .eq("rssid", rssid)
                        .eq("upstream_request_id", upstreamId)));
        if (launch == null || !same(rssid, launch.getRssid())
                || launch.getSsid() == null) {
            log.warn("Ignored unowned Infobip resource event, rssid:{}",
                    rssid);
            return;
        }
        String status = content == null ? null : content.getString("status");
        if (!isBlank(status)) {
            launch.setStatus(status);
            launch.setUpdateTime(new Date());
            launchMapper.updateById(launch);
        }
        JSONObject data = new JSONObject();
        data.put("requestId", launch.getRequestId());
        if (content != null) {
            put(data, "status", content.get("status"));
            put(data, "type", content.get("type"));
            put(data, "daysUntilRejection",
                    content.get("daysUntilRejection"));
            put(data, "requirementsFeedback",
                    content.get("requirementsFeedback"));
            put(data, "focDate", first(content, "focDate",
                    "newFocDate", "requestedFocDate"));
        }
        String key = key(customerType, upstreamId,
                firstNonBlank(value.getString("receivedAt"),
                        content == null ? null
                                : content.getString("updatedAt"),
                        content == null ? null
                                : valueOf(content.get("daysUntilRejection")),
                        content == null ? null
                                : valueOf(first(content, "focDate",
                                        "newFocDate",
                                        "requestedFocDate"))),
                status);
        customerEvents.recordManagementEvent(launch.getSsid(), customerType,
                launch.getSenderId(), null, key, data);
    }

    private void handleTyping(Integer rssid, JSONObject value) {
        RcsChannelAgent binding = findBinding(rssid, value.getString("to"));
        if (binding == null) {
            log.warn("Ignored unowned Infobip typing event, rssid:{}", rssid);
            return;
        }
        JSONObject data = new JSONObject();
        put(data, "from", value.get("sender"));
        put(data, "receivedAt", value.get("receivedAt"));
        String key = key("TYPING_INDICATOR",
                firstNonBlank(value.getString("entityId"),
                        value.getString("sender")),
                value.getString("receivedAt"), null);
        customerEvents.recordManagementEvent(binding.getSsid(),
                "TYPING_INDICATOR", binding.getAgentId(), null, key, data);
    }

    private void handleConversation(Integer rssid, JSONObject value) {
        String upstreamMessageId = value.getString("messageId");
        RCSCdrInfo cdr = first(cdrMapper.selectList(
                new EntityWrapper<RCSCdrInfo>()
                        .eq("channel_code", INFOBIP)
                        .eq("channel_msg_id", upstreamMessageId)));
        RcsChannelAgent binding = cdr == null ? null
                : findBinding(rssid, cdr.getSender());
        if (cdr == null || binding == null
                || !same(cdr.getSsid(), binding.getSsid())) {
            log.warn("Ignored unowned Infobip conversation event, rssid:{}",
                    rssid);
            return;
        }
        JSONObject data = new JSONObject();
        put(data, "trafficType", value.get("trafficType"));
        JSONObject conversation = value.getJSONObject("conversation");
        if (conversation != null) {
            JSONObject customerConversation = new JSONObject();
            put(customerConversation, "type", conversation.get("type"));
            put(customerConversation, "startTime",
                    conversation.get("startTime"));
            put(customerConversation, "endTime",
                    conversation.get("endTime"));
            data.put("conversation", customerConversation);
        }
        String key = key("CONVERSATION_STARTED", upstreamMessageId,
                conversation == null ? null
                        : conversation.getString("startTime"),
                null);
        customerEvents.recordManagementEvent(cdr.getSsid(),
                "CONVERSATION_STARTED", binding.getAgentId(), cdr.getMsgId(),
                key, data);
    }

    private RcsChannelAgent findBinding(Integer rssid, String senderName) {
        if (isBlank(senderName)) {
            return null;
        }
        RcsChannelAgent value = first(bindingMapper.selectList(
                new EntityWrapper<RcsChannelAgent>()
                        .eq("rssid", rssid)
                        .eq("channel_code", INFOBIP)
                        .eq("sender", senderName)
                        .eq("status", 1)));
        return value != null && same(rssid, value.getRssid())
                && INFOBIP.equalsIgnoreCase(value.getChannelCode())
                && value.getSsid() != null ? value : null;
    }

    private static String providerType(JSONObject value) {
        Object event = value.get("event");
        if (event instanceof JSONObject) {
            return ((JSONObject) event).getString("type");
        }
        return event == null ? null : String.valueOf(event);
    }

    private static String key(String type, String identity, String timestamp,
            String status) {
        return "subscription:" + safe(type) + ":" + safe(identity) + ":"
                + safe(timestamp) + ":" + safe(status);
    }

    private static Object first(JSONObject value, String... names) {
        if (value != null && names != null) {
            for (String name : names) {
                if (value.get(name) != null) {
                    return value.get(name);
                }
            }
        }
        return null;
    }

    private static void put(JSONObject target, String name, Object value) {
        if (value != null) {
            target.put(name, value);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String valueOf(Object value) {
        return value == null ? null : String.valueOf(value);
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

    private static boolean same(Object first, Object second) {
        return first == null ? second == null : first.equals(second);
    }

    private static <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

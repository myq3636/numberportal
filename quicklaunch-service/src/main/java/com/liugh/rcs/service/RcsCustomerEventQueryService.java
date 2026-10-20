package com.liugh.rcs.service;

import java.util.Collections;
import java.util.List;

import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsCustomerEvent;
import com.liugh.entity.RcsEventDeliveryAttempt;
import com.liugh.mapper.RcsCustomerEventMapper;
import com.liugh.mapper.RcsEventDeliveryAttemptMapper;

/**
 * Tenant-scoped customer event query and retry operations.
 */
@Service
public class RcsCustomerEventQueryService {

    private final RcsCustomerAccessService accessService;
    private final RcsCustomerEventMapper eventMapper;
    private final RcsEventDeliveryAttemptMapper attemptMapper;
    private final RcsCustomerEventService eventService;
    private final RcsCustomerEventDeliveryService deliveryService;

    @Autowired
    public RcsCustomerEventQueryService(RcsCustomerAccessService accessService,
            RcsCustomerEventMapper eventMapper,
            RcsEventDeliveryAttemptMapper attemptMapper,
            RcsCustomerEventService eventService,
            RcsCustomerEventDeliveryService deliveryService) {
        this.accessService = accessService;
        this.eventMapper = eventMapper;
        this.attemptMapper = attemptMapper;
        this.eventService = eventService;
        this.deliveryService = deliveryService;
    }

    public JSONObject list(JSONObject authRequest, Integer page,
            Integer pageSize) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(authRequest);
        int currentPage = page == null ? 1 : page;
        int size = pageSize == null ? 20 : pageSize;
        if (currentPage < 1 || size < 1 || size > 100) {
            throw new RcsCustomerAccessException("INVALID_ARGUMENT");
        }
        Wrapper<RcsCustomerEvent> tenantQuery =
                new EntityWrapper<RcsCustomerEvent>()
                        .eq("ssid", customer.getSsid());
        Integer total = eventMapper.selectCount(tenantQuery);
        List<RcsCustomerEvent> pageItems = eventMapper.selectPage(
                new RowBounds((currentPage - 1) * size, size),
                new EntityWrapper<RcsCustomerEvent>()
                        .eq("ssid", customer.getSsid())
                        .orderBy("create_time", false));
        if (pageItems == null) {
            pageItems = Collections.emptyList();
        }
        JSONArray items = new JSONArray();
        for (RcsCustomerEvent event : pageItems) {
            items.add(customerView(event));
        }
        JSONObject result = new JSONObject();
        result.put("page", currentPage);
        result.put("pageSize", size);
        result.put("total", total == null ? 0 : total);
        result.put("items", items);
        return result;
    }

    public JSONObject get(JSONObject authRequest, String eventId) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(authRequest);
        RcsCustomerEvent event = find(customer.getSsid(), eventId);
        if (event == null) {
            throw new RcsCustomerAccessException("RESOURCE_NOT_FOUND");
        }
        JSONObject result = customerView(event);
        List<RcsEventDeliveryAttempt> attempts = attemptMapper.selectList(
                new EntityWrapper<RcsEventDeliveryAttempt>()
                        .eq("ssid", customer.getSsid())
                        .eq("event_id", eventId)
                        .orderBy("attempt_no", true));
        JSONArray values = new JSONArray();
        if (attempts != null) {
            for (RcsEventDeliveryAttempt attempt : attempts) {
                JSONObject value = new JSONObject();
                value.put("attemptNo", attempt.getAttemptNo());
                value.put("requestId", attempt.getRequestId());
                value.put("startedAt", attempt.getStartedAt());
                value.put("completedAt", attempt.getCompletedAt());
                value.put("httpStatus", attempt.getHttpStatus());
                value.put("result", attempt.getResult());
                value.put("errorSummary", attempt.getErrorSummary());
                values.add(value);
            }
        }
        result.put("deliveryAttempts", values);
        return result;
    }

    public JSONObject retry(JSONObject request, String eventId) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(request);
        RcsCustomerEvent event = deliveryService.manualRetry(
                customer.getSsid(), eventId);
        return customerView(event);
    }

    public JSONObject createTest(JSONObject request) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(request);
        RcsCustomerEvent event = eventService.createTestEvent(
                customer.getSsid());
        deliveryService.deliver(event);
        return customerView(event);
    }

    private RcsCustomerEvent find(Integer ssid, String eventId) {
        if (eventId == null || eventId.trim().isEmpty()) {
            return null;
        }
        return first(eventMapper.selectList(
                new EntityWrapper<RcsCustomerEvent>()
                        .eq("ssid", ssid)
                        .eq("event_id", eventId)));
    }

    private JSONObject customerView(RcsCustomerEvent event) {
        JSONObject result;
        try {
            result = JSONObject.parseObject(event.getPayloadJson());
        } catch (Exception e) {
            result = new JSONObject();
            result.put("eventId", event.getEventId());
            result.put("type", event.getEventType());
        }
        result.put("deliveryStatus", event.getDeliveryStatus());
        result.put("attemptCount", event.getAttemptCount());
        result.put("nextAttemptAt", event.getNextAttemptTime());
        return result;
    }

    private static <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}

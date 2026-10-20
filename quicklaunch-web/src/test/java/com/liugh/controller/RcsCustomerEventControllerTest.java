package com.liugh.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSONObject;
import com.liugh.config.ResponseModel;
import com.liugh.rcs.service.RcsCustomerEventQueryService;

public class RcsCustomerEventControllerTest {

    private RcsCustomerEventQueryService service;
    private RcsCustomerEventController controller;

    @Before
    public void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest(), new MockHttpServletResponse()));
        service = mock(RcsCustomerEventQueryService.class);
        controller = new RcsCustomerEventController(service);
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void listsTenantEventsWithQueryTokenAndPagination() {
        JSONObject data = JSONObject.parseObject(
                "{\"page\":1,\"pageSize\":20,\"items\":[]}");
        when(service.list(any(JSONObject.class), eq(1), eq(20)))
                .thenReturn(data);

        ResponseModel<JSONObject> response =
                controller.list(null, "customer-token", 1, 20);

        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(service).list(captor.capture(), eq(1), eq(20));
        assertEquals("customer-token", captor.getValue().getString("token"));
        assertSame(data, response.getData());
    }

    @Test
    public void readsTenantEventDetail() {
        JSONObject data = JSONObject.parseObject("{\"eventId\":\"evt_1\"}");
        when(service.get(any(JSONObject.class), eq("evt_1"))).thenReturn(data);

        ResponseModel<JSONObject> response =
                controller.get("evt_1", "customer-token", null);

        assertSame(data, response.getData());
    }

    @Test
    public void retriesFailedTenantEventWithBodyToken() {
        JSONObject request = new JSONObject();
        request.put("Token", "customer-token");
        JSONObject data = JSONObject.parseObject(
                "{\"eventId\":\"evt_1\",\"deliveryStatus\":\"PENDING\"}");
        when(service.retry(request, "evt_1")).thenReturn(data);

        assertSame(data, controller.retry("evt_1", request).getData());
    }
}

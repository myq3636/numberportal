package com.liugh.controller;

import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.service.RcsMessageQueryService;

public class RcsMessageQueryControllerTest {

    private RcsMessageQueryService service;
    private RcsMessageQueryController controller;

    @Before
    public void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest(), new MockHttpServletResponse()));
        service = mock(RcsMessageQueryService.class);
        controller = new RcsMessageQueryController(service);
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void getsTenantMessageUsingQueryToken() {
        JSONObject data = JSONObject.parseObject("{\"messageId\":\"msg-1\"}");
        when(service.get(any(JSONObject.class), eq("msg-1"))).thenReturn(data);

        assertSame(data, controller.get("msg-1", null, "customer-token")
                .getData());
    }
}

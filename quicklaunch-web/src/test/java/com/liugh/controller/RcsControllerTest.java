package com.liugh.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSONObject;
import com.liugh.config.ResponseModel;
import com.liugh.rcs.service.RcsInfobipSubscriptionEventService;

public class RcsControllerTest {

    private RcsInfobipSubscriptionEventService eventService;
    private RcsController controller;

    @Before
    public void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                request, new MockHttpServletResponse()));
        eventService = mock(RcsInfobipSubscriptionEventService.class);
        controller = new RcsController();
        controller.configureInfobipSubscriptionCallback(eventService,
                "callback-secret");
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void acceptsValidInfobipSubscriptionToken() throws Exception {
        JSONObject body = JSONObject.parseObject(
                "{\"results\":[{\"event\":\"RCS_SENDER_STATUS_UPDATE\"}]}");
        MockHttpServletResponse servletResponse =
                new MockHttpServletResponse();
        when(eventService.handle(901, body)).thenReturn(true);

        ResponseModel<?> response = controller.callbackInfobipSubscription(
                901, "callback-secret", body, servletResponse);

        assertEquals(200, response.getCode());
        assertEquals(200, servletResponse.getStatus());
        verify(eventService).handle(901, body);
    }

    @Test
    public void rejectsInvalidTokenBeforeProcessingBody() throws Exception {
        JSONObject body = new JSONObject();
        MockHttpServletResponse servletResponse =
                new MockHttpServletResponse();

        controller.callbackInfobipSubscription(901, "wrong", body,
                servletResponse);

        assertEquals(401, servletResponse.getStatus());
        verify(eventService, never()).handle(eq(901), eq(body));
    }

    @Test
    public void rejectsCallbackWhenServerTokenIsNotConfigured()
            throws Exception {
        controller.configureInfobipSubscriptionCallback(eventService, "");
        JSONObject body = new JSONObject();
        MockHttpServletResponse servletResponse =
                new MockHttpServletResponse();

        controller.callbackInfobipSubscription(901, "anything", body,
                servletResponse);

        assertEquals(503, servletResponse.getStatus());
        verify(eventService, never()).handle(eq(901), eq(body));
    }
}

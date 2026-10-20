package com.liugh.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
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
import com.liugh.rcs.service.RcsAgentEventService;
import com.liugh.rcs.service.RcsCustomerAccessException;

public class RcsAgentEventControllerTest {

    private RcsAgentEventService service;
    private RcsAgentEventController controller;

    @Before
    public void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                request, new MockHttpServletResponse()));
        service = mock(RcsAgentEventService.class);
        controller = new RcsAgentEventController(service);
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void sendsEventWithExistingBodyTokenContract() {
        JSONObject request = new JSONObject();
        request.put("Token", "customer-token");
        request.put("senderId", "sender-1");
        request.put("recipient", "+8613800138000");
        request.put("type", "TYPING_INDICATOR");
        JSONObject data = JSONObject.parseObject("{\"status\":\"ACCEPTED\"}");
        when(service.send(request)).thenReturn(data);

        ResponseModel<JSONObject> response = controller.send(request);

        assertEquals(200, response.getCode());
        assertSame(data, response.getData());
        verify(service).send(request);
    }

    @Test
    public void returnsPublicUnsupportedErrorWithoutProviderDetails() {
        JSONObject request = new JSONObject();
        request.put("token", "customer-token");
        when(service.send(request))
                .thenThrow(new RcsCustomerAccessException("OPERATION_NOT_SUPPORTED"));

        ResponseModel<JSONObject> response = controller.send(request);

        assertEquals(422, response.getCode());
        assertEquals("OPERATION_NOT_SUPPORTED", response.getMessage());
    }
}

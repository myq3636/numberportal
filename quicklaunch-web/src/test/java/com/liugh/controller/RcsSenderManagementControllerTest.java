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
import com.liugh.rcs.service.RcsSenderManagementService;

public class RcsSenderManagementControllerTest {

    private RcsSenderManagementService service;
    private RcsSenderManagementController controller;

    @Before
    public void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                request, new MockHttpServletResponse()));
        service = mock(RcsSenderManagementService.class);
        controller = new RcsSenderManagementController(service);
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void createsSenderWithExistingBodyTokenContract() {
        JSONObject request = new JSONObject();
        request.put("Token", "customer-token");
        JSONObject data = new JSONObject();
        data.put("senderId", "snd_1");
        when(service.createSender(request)).thenReturn(data);

        ResponseModel<JSONObject> response = controller.createSender(request);

        assertEquals(200, response.getCode());
        assertSame(data, response.getData());
        verify(service).createSender(request);
    }

    @Test
    public void readsSenderWithQueryToken() {
        JSONObject data = new JSONObject();
        data.put("senderId", "snd_1");
        when(service.getSender(any(JSONObject.class), eq("snd_1")))
                .thenReturn(data);

        ResponseModel<JSONObject> response = controller.getSender(
                "snd_1", null, "customer-token");

        ArgumentCaptor<JSONObject> auth =
                ArgumentCaptor.forClass(JSONObject.class);
        verify(service).getSender(auth.capture(), eq("snd_1"));
        assertEquals("customer-token", auth.getValue().getString("token"));
        assertSame(data, response.getData());
    }

    @Test
    public void delegatesTemplateRegistrationWithoutChannelParameters() {
        JSONObject request = new JSONObject();
        request.put("Token", "customer-token");
        JSONObject data = new JSONObject();
        data.put("status", "PENDING");
        when(service.registerTemplate(request, "snd_1", "tpl_1"))
                .thenReturn(data);

        ResponseModel<JSONObject> response = controller.registerTemplate(
                "snd_1", "tpl_1", request);

        assertSame(data, response.getData());
        verify(service).registerTemplate(request, "snd_1", "tpl_1");
    }
}

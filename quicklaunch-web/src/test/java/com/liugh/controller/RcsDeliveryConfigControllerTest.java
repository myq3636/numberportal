package com.liugh.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.any;
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
import com.liugh.rcs.service.RcsDeliveryConfigService;

public class RcsDeliveryConfigControllerTest {

    private RcsDeliveryConfigService configService;
    private RcsCustomerEventQueryService eventQueryService;
    private RcsDeliveryConfigController controller;

    @Before
    public void setUp() {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                new MockHttpServletRequest(), new MockHttpServletResponse()));
        configService = mock(RcsDeliveryConfigService.class);
        eventQueryService = mock(RcsCustomerEventQueryService.class);
        controller = new RcsDeliveryConfigController(configService,
                eventQueryService);
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void savesSingleDeliveryAddressWithBodyToken() {
        JSONObject request = JSONObject.parseObject(
                "{\"Token\":\"customer-token\","
                        + "\"eventUrl\":\"https://customer.example.com/events\","
                        + "\"signatureSecret\":\"secret\"}");
        JSONObject data = JSONObject.parseObject("{\"configured\":true}");
        when(configService.save(request)).thenReturn(data);

        ResponseModel<JSONObject> response = controller.save(request);

        assertSame(data, response.getData());
        verify(configService).save(request);
    }

    @Test
    public void readsConfigurationWithExistingQueryToken() {
        JSONObject data = JSONObject.parseObject("{\"configured\":true}");
        when(configService.get(any(JSONObject.class))).thenReturn(data);

        ResponseModel<JSONObject> response =
                controller.get(null, "customer-token");

        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(configService).get(captor.capture());
        assertEquals("customer-token", captor.getValue().getString("token"));
        assertSame(data, response.getData());
    }

    @Test
    public void triggersSignedTestDelivery() {
        JSONObject request = new JSONObject();
        request.put("Token", "customer-token");
        JSONObject data = JSONObject.parseObject("{\"eventId\":\"evt_test\"}");
        when(eventQueryService.createTest(request)).thenReturn(data);

        assertSame(data, controller.test(request).getData());
    }
}

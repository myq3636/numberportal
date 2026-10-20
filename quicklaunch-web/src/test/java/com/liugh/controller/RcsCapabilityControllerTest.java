package com.liugh.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
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
import com.liugh.rcs.service.RcsCapabilityService;
import com.liugh.rcs.service.RcsCustomerAccessException;

public class RcsCapabilityControllerTest {

    private RcsCapabilityService service;
    private RcsCapabilityController controller;

    @Before
    public void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute("response", new MockHttpServletResponse());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(
                request, new MockHttpServletResponse()));
        service = mock(RcsCapabilityService.class);
        controller = new RcsCapabilityController(service);
    }

    @After
    public void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void delegatesBodyTokenCapabilityQuery() {
        JSONObject request = new JSONObject();
        request.put("Token", "customer-token");
        JSONObject data = new JSONObject();
        data.put("results", new Object[0]);
        when(service.query(request)).thenReturn(data);

        ResponseModel<JSONObject> response = controller.query(request);

        assertEquals(200, response.getCode());
        assertSame(data, response.getData());
        verify(service).query(request);
    }

    @Test
    public void mapsCapabilityErrorsToPublicStatusCode() {
        JSONObject request = new JSONObject();
        request.put("token", "customer-token");
        when(service.query(request))
                .thenThrow(new RcsCustomerAccessException("OPERATION_NOT_SUPPORTED"));

        ResponseModel<JSONObject> response = controller.query(request);

        assertEquals(422, response.getCode());
        assertEquals("OPERATION_NOT_SUPPORTED", response.getMessage());
    }

    @Test
    public void delegatesQueryParameterTokenForBatchRead() {
        JSONObject data = new JSONObject();
        data.put("batchId", "cap_1");
        when(service.getBatch(org.mockito.Matchers.any(JSONObject.class),
                org.mockito.Matchers.eq("cap_1"))).thenReturn(data);

        ResponseModel<JSONObject> response =
                controller.getBatch("cap_1", null, "customer-token");

        ArgumentCaptor<JSONObject> captor = ArgumentCaptor.forClass(JSONObject.class);
        verify(service).getBatch(captor.capture(), org.mockito.Matchers.eq("cap_1"));
        assertEquals("customer-token", captor.getValue().getString("token"));
        assertSame(data, response.getData());
    }
}

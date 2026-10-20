package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.entity.RcsLaunchRequest;
import com.liugh.mapper.RcsChannelAgentMapper;
import com.liugh.mapper.RcsLaunchRequestMapper;
import com.liugh.rcs.channel.infobip.InfobipRcsManagementClient;

public class RcsLaunchManagementServiceTest {

    private RcsCustomerAccessService access;
    private RcsRouter router;
    private InfobipRcsManagementClient client;
    private RcsChannelAgentMapper bindingMapper;
    private RcsLaunchRequestMapper launchMapper;
    private RcsLaunchManagementService service;
    private AdminInfo customer;
    private AdminInfo supplier;
    private RcsChannelAgent binding;

    @Before
    public void setUp() {
        access = mock(RcsCustomerAccessService.class);
        router = mock(RcsRouter.class);
        client = mock(InfobipRcsManagementClient.class);
        bindingMapper = mock(RcsChannelAgentMapper.class);
        launchMapper = mock(RcsLaunchRequestMapper.class);
        service = new RcsLaunchManagementService(access, router, client,
                bindingMapper, launchMapper);

        customer = new AdminInfo();
        customer.setSsid(100);
        supplier = new AdminInfo();
        supplier.setId(901L);
        supplier.setChannelCode("INFOBIP");
        supplier.setUrl("https://tenant.api.infobip.com");
        supplier.setAppId("key");
        binding = new RcsChannelAgent();
        binding.setAgentId("snd_1");
        binding.setSender("brand");
        binding.setSsid(100);
        binding.setRssid(901);
        binding.setStatus(1);

        when(access.requireCustomer(any(JSONObject.class))).thenReturn(customer);
        when(bindingMapper.selectList(any())).thenReturn(
                Collections.singletonList(binding));
        when(router.resolveSupplier(binding)).thenReturn(supplier);
        when(launchMapper.insert(any(RcsLaunchRequest.class))).thenReturn(1);
    }

    @Test
    public void submitsRcsLaunchAndPersistsTenantOwnedCorrelation() {
        JSONObject request = JSONObject.parseObject(
                "{\"Token\":\"customer-token\",\"countryCode\":\"GB\","
                        + "\"trafficOrigin\":\"LOCAL\"}");
        when(client.submitResourceRequest(eq(supplier.getUrl()),
                eq(supplier.getAppId()), any(JSONObject.class)))
                .thenReturn(JSONObject.parseObject(
                        "{\"id\":\"upstream-1\",\"status\":\"PENDING\","
                                + "\"channel\":\"RCS\"}"));

        JSONObject result = service.submit(request, "snd_1");

        ArgumentCaptor<JSONObject> upstream =
                ArgumentCaptor.forClass(JSONObject.class);
        verify(client).submitResourceRequest(eq(supplier.getUrl()),
                eq(supplier.getAppId()), upstream.capture());
        assertEquals("RCS_SENDER_LAUNCH",
                upstream.getValue().getString("type"));
        assertEquals("brand", upstream.getValue().getString("senderName"));
        assertFalse(upstream.getValue().containsKey("Token"));

        ArgumentCaptor<RcsLaunchRequest> stored =
                ArgumentCaptor.forClass(RcsLaunchRequest.class);
        verify(launchMapper).insert(stored.capture());
        assertEquals(Integer.valueOf(100), stored.getValue().getSsid());
        assertEquals(Integer.valueOf(901), stored.getValue().getRssid());
        assertEquals("upstream-1", stored.getValue().getUpstreamRequestId());
        assertEquals(stored.getValue().getRequestId(),
                result.getString("requestId"));
        assertFalse(result.containsKey("id"));
        assertFalse(result.containsKey("channel"));
    }

    @Test
    public void looksUpRequirementsOnSenderBoundSupplier() {
        JSONObject request = JSONObject.parseObject(
                "{\"Token\":\"customer-token\",\"countryCode\":\"GB\"}");
        when(client.lookupResourceRequirements(eq(supplier.getUrl()),
                eq(supplier.getAppId()), any(JSONObject.class)))
                .thenReturn(JSONObject.parseObject("{\"requirements\":[]}"));

        service.requirements(request, "snd_1");

        verify(router).resolveSupplier(binding);
        verify(client).lookupResourceRequirements(eq(supplier.getUrl()),
                eq(supplier.getAppId()), any(JSONObject.class));
    }
}

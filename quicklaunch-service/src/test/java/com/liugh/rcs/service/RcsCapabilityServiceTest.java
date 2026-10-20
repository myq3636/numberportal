package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsCapabilityBatch;
import com.liugh.entity.RcsCapabilityResult;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.mapper.RcsCapabilityBatchMapper;
import com.liugh.mapper.RcsCapabilityResultMapper;
import com.liugh.rcs.channel.RcsChannelCapability;
import com.liugh.rcs.channel.RcsChannelCapabilityRegistry;
import com.liugh.rcs.channel.infobip.InfobipCapabilityClient;

public class RcsCapabilityServiceTest {

    private RcsCustomerAccessService accessService;
    private RcsRouter router;
    private RcsChannelCapabilityRegistry capabilityRegistry;
    private InfobipCapabilityClient infobipClient;
    private RcsCapabilityBatchMapper batchMapper;
    private RcsCapabilityResultMapper resultMapper;
    private RcsCapabilityService service;
    private AdminInfo customer;
    private AdminInfo supplier;

    @Before
    public void setUp() {
        accessService = mock(RcsCustomerAccessService.class);
        router = mock(RcsRouter.class);
        capabilityRegistry = mock(RcsChannelCapabilityRegistry.class);
        infobipClient = mock(InfobipCapabilityClient.class);
        batchMapper = mock(RcsCapabilityBatchMapper.class);
        resultMapper = mock(RcsCapabilityResultMapper.class);
        service = new RcsCapabilityService(accessService, router, capabilityRegistry,
                infobipClient, batchMapper, resultMapper);
        service.setProjectUrl("https://platform.example.com/api/v1/");

        customer = new AdminInfo();
        customer.setId(100L);
        customer.setSsid(100);
        supplier = new AdminInfo();
        supplier.setId(901L);
        supplier.setChannelCode("INFOBIP");
        supplier.setUrl("https://api.example.com");
        supplier.setAppId("api-key");

        RcsChannelAgent binding = new RcsChannelAgent();
        binding.setAgentId("sender-1");
        binding.setChannelCode("INFOBIP");
        binding.setSender("infobip-sender");
        binding.setSsid(100);

        when(accessService.requireCustomer(any(JSONObject.class))).thenReturn(customer);
        when(router.route(eq(customer), anyString())).thenReturn(supplier);
        when(router.resolveAgentBinding("sender-1", "INFOBIP")).thenReturn(binding);
    }

    @Test
    public void queriesCapabilitiesThroughInternallyRoutedInfobipResource() {
        when(capabilityRegistry.supports("INFOBIP",
                RcsChannelCapability.CAPABILITY_QUERY_SYNC)).thenReturn(true);
        JSONObject upstream = JSONObject.parseObject(
                "{\"capabilityCheckResults\":[{\"phoneNumber\":\"+8613800138000\","
                        + "\"code\":\"ENABLED\",\"features\":[\"RICHCARD_STANDALONE\"]}]}");
        when(infobipClient.query(eq("https://api.example.com"), eq("api-key"),
                eq("infobip-sender"), anyList())).thenReturn(upstream);

        JSONObject result = service.query(request("sender-1",
                "+8613800138000"));

        assertEquals("ENABLED",
                result.getJSONArray("results").getJSONObject(0).getString("status"));
        assertFalse(result.containsKey("channel"));
        assertFalse(result.containsKey("channelCode"));
        assertFalse(result.containsKey("rssid"));
        verify(accessService).requireOwned(100, 100);
    }

    @Test
    public void createsPrivateAsyncBatchWithoutExposingProviderMetadata() {
        when(capabilityRegistry.supports("INFOBIP",
                RcsChannelCapability.CAPABILITY_QUERY_ASYNC)).thenReturn(true);
        when(infobipClient.notify(eq("https://api.example.com"), eq("api-key"),
                eq("infobip-sender"), anyList(), anyString()))
                .thenReturn(JSONObject.parseObject("{\"bulkId\":\"provider-bulk-1\"}"));

        JSONObject result = service.createBatch(request("sender-1",
                "+8613800138000", "+8613800138001"));

        ArgumentCaptor<RcsCapabilityBatch> captor =
                ArgumentCaptor.forClass(RcsCapabilityBatch.class);
        verify(batchMapper).insert(captor.capture());
        assertEquals("provider-bulk-1", captor.getValue().getUpstreamBulkId());
        assertEquals(Integer.valueOf(100), captor.getValue().getSsid());
        assertEquals("INFOBIP", captor.getValue().getChannelCode());
        assertEquals("PENDING", result.getString("status"));
        assertFalse(result.containsKey("bulkId"));
        assertFalse(result.containsKey("channelCode"));
    }

    @Test
    public void returnsOnlyBatchOwnedByAuthenticatedCustomer() {
        RcsCapabilityBatch batch = new RcsCapabilityBatch();
        batch.setBatchId("cap_batch_1");
        batch.setSsid(100);
        batch.setSenderId("sender-1");
        batch.setStatus("COMPLETED");
        batch.setTotalCount(1);
        batch.setSuccessCount(1);
        batch.setFailureCount(0);
        when(batchMapper.selectList(any())).thenReturn(Collections.singletonList(batch));

        RcsCapabilityResult capabilityResult = new RcsCapabilityResult();
        capabilityResult.setRecipient("+8613800138000");
        capabilityResult.setStatus("ENABLED");
        capabilityResult.setCapabilitiesJson("[\"RICHCARD_STANDALONE\"]");
        when(resultMapper.selectList(any()))
                .thenReturn(Collections.singletonList(capabilityResult));

        JSONObject auth = new JSONObject();
        auth.put("Token", "customer-token");
        JSONObject result = service.getBatch(auth, "cap_batch_1");

        assertEquals("COMPLETED", result.getString("status"));
        assertEquals(1, result.getJSONArray("results").size());
        assertFalse(result.containsKey("channelCode"));
        verify(accessService).requireOwned(100, 100);
    }

    @Test(expected = RcsCustomerAccessException.class)
    public void rejectsCapabilityBatchSpanningDifferentPlatformRoutes() {
        AdminInfo otherSupplier = new AdminInfo();
        otherSupplier.setId(902L);
        otherSupplier.setChannelCode("INFOBIP");
        when(router.route(eq(customer), eq("US"))).thenReturn(otherSupplier);

        service.createBatch(request("sender-1",
                "+8613800138000", "+14155552671"));
    }

    private JSONObject request(String senderId, String... phoneNumbers) {
        JSONObject request = new JSONObject();
        request.put("Token", "customer-token");
        request.put("senderId", senderId);
        request.put("phoneNumbers", new JSONArray(Arrays.asList(phoneNumbers)));
        return request;
    }
}

package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.entity.RcsMoInfo;
import com.liugh.mapper.RcsMoInfoMapper;
import com.liugh.rcs.channel.RcsChannelCapability;
import com.liugh.rcs.channel.RcsChannelCapabilityRegistry;
import com.liugh.rcs.channel.infobip.InfobipOutboundEventClient;

public class RcsAgentEventServiceTest {

    private RcsCustomerAccessService accessService;
    private RcsRouter router;
    private RcsChannelCapabilityRegistry capabilityRegistry;
    private InfobipOutboundEventClient infobipClient;
    private RcsMoInfoMapper moMapper;
    private RcsAgentEventService service;
    private AdminInfo customer;
    private AdminInfo supplier;

    @Before
    public void setUp() {
        accessService = mock(RcsCustomerAccessService.class);
        router = mock(RcsRouter.class);
        capabilityRegistry = mock(RcsChannelCapabilityRegistry.class);
        infobipClient = mock(InfobipOutboundEventClient.class);
        moMapper = mock(RcsMoInfoMapper.class);
        service = new RcsAgentEventService(accessService, router,
                capabilityRegistry, infobipClient, moMapper);

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
        when(capabilityRegistry.supports("INFOBIP",
                RcsChannelCapability.OUTBOUND_EVENT)).thenReturn(true);
    }

    @Test
    public void sendsTypingIndicatorUsingInternalRoute() {
        when(infobipClient.typing("https://api.example.com", "api-key",
                "infobip-sender", "+8613800138000"))
                .thenReturn(JSONObject.parseObject("{\"status\":\"PENDING\"}"));

        JSONObject result = service.send(request("TYPING_INDICATOR", null));

        assertEquals("ACCEPTED", result.getString("status"));
        assertFalse(result.containsKey("channelCode"));
        assertFalse(result.containsKey("rssid"));
        verify(accessService).requireOwned(100, 100);
    }

    @Test
    public void resolvesOwnedInboundMessageBeforeSendingSeen() {
        RcsMoInfo inbound = new RcsMoInfo();
        inbound.setId(42L);
        inbound.setSsid(100);
        inbound.setChannelCode("INFOBIP");
        inbound.setAgentId("sender-1");
        inbound.setMsisdn("+8613800138000");
        inbound.setClientMsgId("provider-inbound-42");
        when(moMapper.selectById(42L)).thenReturn(inbound);
        when(infobipClient.seen("https://api.example.com", "api-key",
                "infobip-sender", "+8613800138000", "provider-inbound-42"))
                .thenReturn(JSONObject.parseObject("{\"status\":\"PENDING\"}"));

        JSONObject result = service.send(request("SEEN", "mo_42"));

        assertEquals("ACCEPTED", result.getString("status"));
        verify(accessService, times(2)).requireOwned(100, 100);
        verify(infobipClient).seen("https://api.example.com", "api-key",
                "infobip-sender", "+8613800138000", "provider-inbound-42");
    }

    @Test
    public void rejectsRouteWithoutOutboundEventCapability() {
        when(capabilityRegistry.supports("INFOBIP",
                RcsChannelCapability.OUTBOUND_EVENT)).thenReturn(false);
        try {
            service.send(request("TYPING_INDICATOR", null));
            fail("Expected OPERATION_NOT_SUPPORTED");
        } catch (RcsCustomerAccessException e) {
            assertEquals("OPERATION_NOT_SUPPORTED", e.getErrorCode());
        }
    }

    private JSONObject request(String type, String messageId) {
        JSONObject request = new JSONObject();
        request.put("Token", "customer-token");
        request.put("senderId", "sender-1");
        request.put("recipient", "+8613800138000");
        request.put("type", type);
        if (messageId != null) {
            request.put("messageId", messageId);
        }
        return request;
    }
}

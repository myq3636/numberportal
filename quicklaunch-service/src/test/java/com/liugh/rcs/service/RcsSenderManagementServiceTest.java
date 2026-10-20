package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.entity.RcsTemplateBinding;
import com.liugh.mapper.RcsChannelAgentMapper;
import com.liugh.mapper.RcsTemplateBindingMapper;
import com.liugh.rcs.channel.infobip.InfobipRcsManagementClient;

public class RcsSenderManagementServiceTest {

    private RcsCustomerAccessService accessService;
    private RcsRouter router;
    private InfobipRcsManagementClient client;
    private RcsChannelAgentMapper bindingMapper;
    private RcsTemplateBindingMapper templateBindingMapper;
    private RcsSenderManagementService service;
    private AdminInfo customer;
    private AdminInfo supplier;

    @Before
    public void setUp() {
        accessService = mock(RcsCustomerAccessService.class);
        router = mock(RcsRouter.class);
        client = mock(InfobipRcsManagementClient.class);
        bindingMapper = mock(RcsChannelAgentMapper.class);
        templateBindingMapper = mock(RcsTemplateBindingMapper.class);
        service = new RcsSenderManagementService(accessService, router, client,
                bindingMapper, templateBindingMapper);

        customer = new AdminInfo();
        customer.setSsid(100);
        supplier = new AdminInfo();
        supplier.setId(901L);
        supplier.setChannelCode("INFOBIP");
        supplier.setUrl("https://tenant.api.infobip.com");
        supplier.setAppId("api-key");
        when(accessService.requireCustomer(any(JSONObject.class)))
                .thenReturn(customer);
        when(router.route(customer, "GB")).thenReturn(supplier);
        when(bindingMapper.insert(any(RcsChannelAgent.class))).thenReturn(1);
    }

    @Test
    public void createsSenderOnCountryRouteAndPersistsResourceBinding() {
        JSONObject request = JSONObject.parseObject(
                "{\"Token\":\"customer-token\",\"country\":\"GB\","
                        + "\"senderName\":\"brand\"}");
        when(client.createSender(eq(supplier.getUrl()), eq(supplier.getAppId()),
                any(JSONObject.class))).thenReturn(
                        JSONObject.parseObject("{\"senderName\":\"brand\","
                                + "\"status\":\"PENDING\",\"channel\":\"RCS\"}"));

        JSONObject result = service.createSender(request);

        ArgumentCaptor<JSONObject> upstream =
                ArgumentCaptor.forClass(JSONObject.class);
        verify(client).createSender(eq(supplier.getUrl()),
                eq(supplier.getAppId()), upstream.capture());
        assertFalse(upstream.getValue().containsKey("Token"));
        assertFalse(upstream.getValue().containsKey("country"));
        assertFalse(upstream.getValue().containsKey("channel"));

        ArgumentCaptor<RcsChannelAgent> binding =
                ArgumentCaptor.forClass(RcsChannelAgent.class);
        verify(bindingMapper).insert(binding.capture());
        assertEquals(Integer.valueOf(100), binding.getValue().getSsid());
        assertEquals(Integer.valueOf(901), binding.getValue().getRssid());
        assertEquals("INFOBIP", binding.getValue().getChannelCode());
        assertEquals("brand", binding.getValue().getSender());
        assertEquals(binding.getValue().getAgentId(), result.getString("senderId"));
        assertFalse(result.containsKey("channel"));
        assertFalse(result.containsKey("rssid"));
    }

    @Test
    public void listsOnlyBindingsOwnedByAuthenticatedCustomer() {
        RcsChannelAgent binding = binding("snd_one", 100, 901);
        when(bindingMapper.selectList(any())).thenReturn(
                Collections.singletonList(binding));
        when(router.resolveSupplier(binding)).thenReturn(supplier);
        when(client.getSender(supplier.getUrl(), supplier.getAppId(), "brand"))
                .thenReturn(JSONObject.parseObject(
                        "{\"senderName\":\"brand\",\"status\":\"TESTING\","
                                + "\"channelCode\":\"INFOBIP\"}"));

        JSONObject result = service.listSenders(auth());

        JSONArray senders = result.getJSONArray("senders");
        assertEquals(1, senders.size());
        assertEquals("snd_one", senders.getJSONObject(0).getString("senderId"));
        assertFalse(senders.getJSONObject(0).containsKey("channelCode"));
        assertFalse(senders.getJSONObject(0).containsKey("rssid"));
    }

    @Test(expected = RcsCustomerAccessException.class)
    public void rejectsSenderOwnedByAnotherCustomer() {
        RcsChannelAgent binding = binding("snd_other", 200, 901);
        when(bindingMapper.selectList(any())).thenReturn(
                Collections.singletonList(binding));
        doThrow(new RcsCustomerAccessException("FORBIDDEN_RESOURCE"))
                .when(accessService).requireOwned(100, 200);

        service.getSender(auth(), "snd_other");
    }

    @Test
    public void testNumberOperationUsesOriginalBoundSupplier() {
        RcsChannelAgent binding = binding("snd_one", 100, 901);
        when(bindingMapper.selectList(any())).thenReturn(
                Collections.singletonList(binding));
        when(router.resolveSupplier(binding)).thenReturn(supplier);
        when(client.listTestNumbers(supplier.getUrl(), supplier.getAppId(),
                "brand")).thenReturn(JSONObject.parseObject(
                        "{\"testNumbers\":[{\"testNumber\":\"+44123\"}],"
                                + "\"channel\":\"RCS\"}"));

        JSONObject result = service.listTestNumbers(auth(), "snd_one");

        assertEquals(1, result.getJSONArray("testNumbers").size());
        assertFalse(result.containsKey("channel"));
        verify(router).resolveSupplier(binding);
    }

    @Test
    public void persistsTemplateBindingAfterUpstreamCreation() {
        RcsChannelAgent binding = binding("snd_one", 100, 901);
        when(bindingMapper.selectList(any())).thenReturn(
                Collections.singletonList(binding));
        when(router.resolveSupplier(binding)).thenReturn(supplier);
        when(templateBindingMapper.selectList(any()))
                .thenReturn(Collections.<RcsTemplateBinding>emptyList());
        when(templateBindingMapper.insert(any(RcsTemplateBinding.class)))
                .thenReturn(1);
        when(client.createTemplate(eq(supplier.getUrl()),
                eq(supplier.getAppId()), eq("brand"),
                any(JSONObject.class))).thenReturn(JSONObject.parseObject(
                        "{\"id\":\"tpl-1\",\"name\":\"welcome\","
                                + "\"templateStatus\":\"PENDING\"}"));
        JSONObject request = auth();
        request.put("name", "welcome");

        service.createTemplate(request, "snd_one");

        ArgumentCaptor<RcsTemplateBinding> stored =
                ArgumentCaptor.forClass(RcsTemplateBinding.class);
        verify(templateBindingMapper).insert(stored.capture());
        assertEquals(Integer.valueOf(901), stored.getValue().getRssid());
        assertEquals(Integer.valueOf(100), stored.getValue().getSsid());
        assertEquals("snd_one", stored.getValue().getSenderId());
        assertEquals("welcome", stored.getValue().getTemplateName());
        assertEquals("tpl-1", stored.getValue().getUpstreamTemplateId());
    }

    @Test(expected = RcsCustomerAccessException.class)
    public void rejectsTemplateNameOwnedByAnotherTenantBeforeCallingUpstream() {
        RcsChannelAgent binding = binding("snd_one", 100, 901);
        when(bindingMapper.selectList(any())).thenReturn(
                Collections.singletonList(binding));
        when(router.resolveSupplier(binding)).thenReturn(supplier);
        RcsTemplateBinding conflict = new RcsTemplateBinding();
        conflict.setRssid(901);
        conflict.setSsid(200);
        conflict.setTemplateName("welcome");
        when(templateBindingMapper.selectList(any()))
                .thenReturn(Collections.singletonList(conflict));
        JSONObject request = auth();
        request.put("name", "welcome");

        try {
            service.createTemplate(request, "snd_one");
        } finally {
            verify(client, never()).createTemplate(any(String.class),
                    any(String.class), any(String.class),
                    any(JSONObject.class));
        }
    }

    @Test
    public void removesTemplateBindingAfterUpstreamDeletion() {
        RcsChannelAgent binding = binding("snd_one", 100, 901);
        when(bindingMapper.selectList(any())).thenReturn(
                Collections.singletonList(binding));
        when(router.resolveSupplier(binding)).thenReturn(supplier);
        RcsTemplateBinding stored = new RcsTemplateBinding();
        stored.setId(77L);
        stored.setRssid(901);
        stored.setSsid(100);
        stored.setSenderId("snd_one");
        stored.setTemplateName("welcome");
        stored.setUpstreamTemplateId("tpl-1");
        when(templateBindingMapper.selectList(any()))
                .thenReturn(Arrays.asList(stored));
        when(client.deleteTemplate(eq(supplier.getUrl()),
                eq(supplier.getAppId()), eq("brand"), eq("tpl-1")))
                .thenReturn(new JSONObject());

        service.deleteTemplate(auth(), "snd_one", "tpl-1");

        verify(templateBindingMapper).deleteById(77L);
    }

    private JSONObject auth() {
        JSONObject auth = new JSONObject();
        auth.put("Token", "customer-token");
        return auth;
    }

    private RcsChannelAgent binding(String senderId, int ssid, int rssid) {
        RcsChannelAgent binding = new RcsChannelAgent();
        binding.setAgentId(senderId);
        binding.setSsid(ssid);
        binding.setRssid(rssid);
        binding.setChannelCode("INFOBIP");
        binding.setSender("brand");
        binding.setStatus(1);
        return binding;
    }
}

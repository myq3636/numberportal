package com.liugh.rcs.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.entity.RcsLaunchRequest;
import com.liugh.entity.RcsTemplateBinding;
import com.liugh.mapper.RCSCdrInfoMapper;
import com.liugh.mapper.RcsChannelAgentMapper;
import com.liugh.mapper.RcsLaunchRequestMapper;
import com.liugh.mapper.RcsTemplateBindingMapper;

public class RcsInfobipSubscriptionEventServiceTest {

    private RcsChannelAgentMapper bindingMapper;
    private RcsTemplateBindingMapper templateMapper;
    private RcsLaunchRequestMapper launchMapper;
    private RCSCdrInfoMapper cdrMapper;
    private RcsCustomerEventService customerEvents;
    private RcsInfobipSubscriptionEventService service;

    @Before
    public void setUp() {
        bindingMapper = mock(RcsChannelAgentMapper.class);
        templateMapper = mock(RcsTemplateBindingMapper.class);
        launchMapper = mock(RcsLaunchRequestMapper.class);
        cdrMapper = mock(RCSCdrInfoMapper.class);
        customerEvents = mock(RcsCustomerEventService.class);
        service = new RcsInfobipSubscriptionEventService(bindingMapper,
                templateMapper, launchMapper, cdrMapper, customerEvents);
    }

    @Test
    public void normalizesSenderStatusForItsBoundTenant() {
        when(bindingMapper.selectList(any())).thenReturn(
                Collections.singletonList(binding()));
        JSONObject callback = JSONObject.parseObject(
                "{\"results\":[{\"event\":\"RCS_SENDER_STATUS_UPDATE\","
                        + "\"senderName\":\"brand\",\"senderStatus\":\"READY\","
                        + "\"updatedAt\":\"2026-07-28T08:00:00Z\","
                        + "\"platform\":{\"entityId\":\"internal\"}}]}");

        service.handle(901, callback);

        ArgumentCaptor<JSONObject> data =
                ArgumentCaptor.forClass(JSONObject.class);
        verify(customerEvents).recordManagementEvent(eq(100),
                eq("SENDER_UPDATED"), eq("snd_one"),
                eq((String) null), anyString(), data.capture());
        assertEquals("READY", data.getValue().getString("status"));
        assertFalse(data.getValue().containsKey("platform"));
        assertFalse(data.getValue().containsKey("rssid"));
        assertFalse(data.getValue().containsKey("channel"));
    }

    @Test
    public void resolvesSenderlessTemplateEventAndUpdatesBinding() {
        RcsTemplateBinding template = new RcsTemplateBinding();
        template.setId(7L);
        template.setRssid(901);
        template.setSsid(100);
        template.setSenderId("snd_one");
        template.setTemplateName("welcome");
        when(templateMapper.selectList(any())).thenReturn(
                Collections.singletonList(template));
        JSONObject callback = JSONObject.parseObject(
                "{\"name\":\"welcome\",\"templateStatus\":\"APPROVED\","
                        + "\"comment\":\"ok\","
                        + "\"updatedAt\":\"2026-07-28T08:00:00Z\"}");

        service.handle(901, callback);

        verify(templateMapper).updateById(template);
        verify(customerEvents).recordManagementEvent(eq(100),
                eq("TEMPLATE_UPDATED"), eq("snd_one"),
                eq((String) null), anyString(), any(JSONObject.class));
        assertEquals("APPROVED", template.getStatus());
    }

    @Test
    public void updatesResourceRequestAndUsesPlatformRequestId() {
        RcsLaunchRequest launch = new RcsLaunchRequest();
        launch.setId(8L);
        launch.setRssid(901);
        launch.setSsid(100);
        launch.setSenderId("snd_one");
        launch.setRequestId("launch_local");
        launch.setUpstreamRequestId("upstream-1");
        when(launchMapper.selectList(any())).thenReturn(
                Collections.singletonList(launch));
        JSONObject callback = JSONObject.parseObject(
                "{\"results\":[{\"event\":\"RESOURCE_REQUEST_STATUS_UPDATE\","
                        + "\"receivedAt\":\"2026-07-28T08:00:00Z\","
                        + "\"content\":{\"id\":\"upstream-1\","
                        + "\"status\":\"COMPLETED\","
                        + "\"type\":\"RCS_SENDER_LAUNCH\"}}]}");

        service.handle(901, callback);

        ArgumentCaptor<JSONObject> data =
                ArgumentCaptor.forClass(JSONObject.class);
        verify(customerEvents).recordManagementEvent(eq(100),
                eq("RESOURCE_REQUEST_UPDATED"), eq("snd_one"),
                eq((String) null), anyString(), data.capture());
        assertEquals("launch_local", data.getValue().getString("requestId"));
        assertFalse(data.getValue().containsKey("id"));
        assertEquals("COMPLETED", launch.getStatus());
        verify(launchMapper).updateById(launch);
    }

    @Test
    public void resolvesConversationByCdrAndBoundSupplier() {
        RCSCdrInfo cdr = new RCSCdrInfo();
        cdr.setSsid(100);
        cdr.setSender("brand");
        cdr.setMsgId("msg_local");
        cdr.setChannelMsgId("msg-upstream");
        when(cdrMapper.selectList(any())).thenReturn(
                Collections.singletonList(cdr));
        when(bindingMapper.selectList(any())).thenReturn(
                Collections.singletonList(binding()));
        JSONObject callback = JSONObject.parseObject(
                "{\"results\":[{\"messageId\":\"msg-upstream\","
                        + "\"event\":{\"type\":\"CONVERSATION_STARTED\"},"
                        + "\"conversation\":{\"type\":\"A2P\","
                        + "\"startTime\":\"2026-07-28T08:00:00Z\"}}]}");

        service.handle(901, callback);

        verify(customerEvents).recordManagementEvent(eq(100),
                eq("CONVERSATION_STARTED"), eq("snd_one"),
                eq("msg_local"), anyString(), any(JSONObject.class));
    }

    @Test
    public void resolvesTypingIndicatorByDestinationSender() {
        when(bindingMapper.selectList(any())).thenReturn(
                Collections.singletonList(binding()));
        JSONObject callback = JSONObject.parseObject(
                "{\"results\":[{\"sender\":\"+8613800138000\","
                        + "\"to\":\"brand\","
                        + "\"receivedAt\":\"2026-07-28T08:00:00Z\","
                        + "\"event\":{\"type\":\"TYPING_INDICATOR\"},"
                        + "\"entityId\":\"typing-1\"}]}");

        service.handle(901, callback);

        verify(customerEvents).recordManagementEvent(eq(100),
                eq("TYPING_INDICATOR"), eq("snd_one"),
                eq((String) null), anyString(), any(JSONObject.class));
    }

    @Test
    public void normalizesLaunchAndTestNumberStatusEvents() {
        when(bindingMapper.selectList(any())).thenReturn(
                Collections.singletonList(binding()));
        JSONObject callback = JSONObject.parseObject(
                "{\"results\":["
                        + "{\"event\":\"RCS_SENDER_LAUNCH_STATUS_UPDATE\","
                        + "\"senderName\":\"brand\","
                        + "\"senderStatus\":\"PENDING_LAUNCH\","
                        + "\"updatedAt\":\"2026-07-28T08:00:00Z\"},"
                        + "{\"event\":\"RCS_TEST_NUMBER_STATUS_UPDATE\","
                        + "\"senderName\":\"brand\","
                        + "\"testNumber\":\"+8613800138000\","
                        + "\"testNumberStatus\":\"APPROVED\","
                        + "\"updatedAt\":\"2026-07-28T08:01:00Z\"}]}");

        service.handle(901, callback);

        verify(customerEvents).recordManagementEvent(eq(100),
                eq("SENDER_LAUNCH_UPDATED"), eq("snd_one"),
                eq((String) null), anyString(), any(JSONObject.class));
        verify(customerEvents).recordManagementEvent(eq(100),
                eq("TEST_NUMBER_UPDATED"), eq("snd_one"),
                eq((String) null), anyString(), any(JSONObject.class));
    }

    @Test
    public void ignoresEventWhenResolvedBindingBelongsToAnotherSupplier() {
        RcsChannelAgent other = binding();
        other.setRssid(902);
        when(bindingMapper.selectList(any())).thenReturn(
                Collections.singletonList(other));
        JSONObject callback = JSONObject.parseObject(
                "{\"results\":[{\"event\":\"RCS_SENDER_STATUS_UPDATE\","
                        + "\"senderName\":\"brand\",\"senderStatus\":\"READY\"}]}");

        service.handle(901, callback);

        verifyZeroInteractions(customerEvents);
    }

    private RcsChannelAgent binding() {
        RcsChannelAgent value = new RcsChannelAgent();
        value.setAgentId("snd_one");
        value.setSsid(100);
        value.setRssid(901);
        value.setChannelCode("INFOBIP");
        value.setSender("brand");
        value.setStatus(1);
        return value;
    }
}

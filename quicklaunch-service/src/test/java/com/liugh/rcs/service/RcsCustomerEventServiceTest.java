package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsCapabilityBatch;
import com.liugh.entity.RcsCustomerEvent;
import com.liugh.entity.RcsMoInfo;
import com.liugh.mapper.RcsCapabilityBatchMapper;
import com.liugh.mapper.RcsCapabilityResultMapper;
import com.liugh.mapper.RcsChannelAgentMapper;
import com.liugh.mapper.RcsCustomerEventMapper;
import com.liugh.rcs.channel.RcsDrEvent;

public class RcsCustomerEventServiceTest {

    private RcsCustomerEventMapper eventMapper;
    private RcsChannelAgentMapper bindingMapper;
    private RcsCapabilityBatchMapper batchMapper;
    private RcsCapabilityResultMapper resultMapper;
    private RcsCustomerEventService service;

    @Before
    public void setUp() {
        eventMapper = mock(RcsCustomerEventMapper.class);
        bindingMapper = mock(RcsChannelAgentMapper.class);
        batchMapper = mock(RcsCapabilityBatchMapper.class);
        resultMapper = mock(RcsCapabilityResultMapper.class);
        service = new RcsCustomerEventService(eventMapper, bindingMapper,
                batchMapper, resultMapper);
        when(eventMapper.selectList(any())).thenReturn(Collections.emptyList());
    }

    @Test
    public void normalizesInfobipDeliverySeenExpiredAndFailure() {
        RCSCdrInfo cdr = cdr();
        service.recordDr(cdr, dr(3, null));
        service.recordDr(cdr, dr(9, null));
        service.recordDr(cdr, dr(5, null));
        service.recordDr(cdr, dr(4, "REJECTED"));

        ArgumentCaptor<RcsCustomerEvent> captor =
                ArgumentCaptor.forClass(RcsCustomerEvent.class);
        verify(eventMapper, times(4)).insert(captor.capture());
        List<RcsCustomerEvent> events = captor.getAllValues();
        assertEquals("MESSAGE_DELIVERED", events.get(0).getEventType());
        assertEquals("MESSAGE_SEEN", events.get(1).getEventType());
        assertEquals("MESSAGE_EXPIRED", events.get(2).getEventType());
        assertEquals("MESSAGE_FAILED", events.get(3).getEventType());
        assertFalse(events.get(3).getPayloadJson().contains("channelCode"));
        assertFalse(events.get(3).getPayloadJson().contains("rssid"));
    }

    @Test
    public void normalizesInboundTextMediaLocationSuggestionAndTyping() {
        service.recordInbound(inbound("USER_MESSAGE", "TEXT"));
        service.recordInbound(inbound("USER_MESSAGE", "FILE"));
        service.recordInbound(inbound("USER_MESSAGE", "LOCATION"));
        service.recordInbound(inbound("SUGGESTION_RESPONSE", "SUGGESTION"));
        service.recordInbound(inbound("IS_TYPING", null));

        ArgumentCaptor<RcsCustomerEvent> captor =
                ArgumentCaptor.forClass(RcsCustomerEvent.class);
        verify(eventMapper, times(5)).insert(captor.capture());
        List<RcsCustomerEvent> events = captor.getAllValues();
        assertEquals("USER_MESSAGE", events.get(0).getEventType());
        assertTrue(events.get(0).getPayloadJson().contains("\"text\":\"hello\""));
        assertTrue(events.get(1).getPayloadJson().contains("\"fileUri\""));
        assertTrue(events.get(2).getPayloadJson().contains("\"latitude\""));
        assertEquals("SUGGESTION_RESPONSE", events.get(3).getEventType());
        assertEquals("TYPING_INDICATOR", events.get(4).getEventType());
    }

    @Test
    public void duplicateProviderEventCreatesOnlyOneCustomerEvent() {
        RcsMoInfo inbound = inbound("USER_MESSAGE", "TEXT");
        RcsCustomerEvent first = service.recordInbound(inbound);
        when(eventMapper.selectList(any()))
                .thenReturn(Collections.singletonList(first));

        RcsCustomerEvent duplicate = service.recordInbound(inbound);

        assertEquals(first.getEventId(), duplicate.getEventId());
        verify(eventMapper, times(1)).insert(any(RcsCustomerEvent.class));
    }

    @Test
    public void storesAsyncCapabilityResultsAndCreatesCustomerEvent() {
        RcsCapabilityBatch batch = new RcsCapabilityBatch();
        batch.setBatchId("cap_1");
        batch.setSsid(100);
        batch.setSenderId("sender-1");
        batch.setChannelCode("INFOBIP");
        batch.setUpstreamBulkId("bulk-1");
        when(batchMapper.selectList(any()))
                .thenReturn(Collections.singletonList(batch));

        JSONObject callback = JSONObject.parseObject(
                "{\"bulkId\":\"bulk-1\",\"capabilityCheckResults\":["
                        + "{\"phoneNumber\":\"+8613800138000\","
                        + "\"code\":\"ENABLED\",\"features\":[\"CHAT\"]}]}");
        RcsCustomerEvent event =
                service.recordCapabilityResults("INFOBIP", callback);

        assertEquals("CAPABILITY_RESULTS_READY", event.getEventType());
        assertTrue(event.getPayloadJson().contains("\"batchId\":\"cap_1\""));
        assertFalse(event.getPayloadJson().contains("bulk-1"));
        verify(resultMapper).insert(any(com.liugh.entity.RcsCapabilityResult.class));
        verify(batchMapper).updateById(batch);
    }

    @Test
    public void stripsInternalRoutingFieldsFromManagementEvents() {
        JSONObject data = JSONObject.parseObject(
                "{\"status\":\"READY\",\"channel\":\"RCS\","
                        + "\"rssid\":901,\"provider\":\"INFOBIP\","
                        + "\"platform\":{\"entityId\":\"internal\"}}");

        RcsCustomerEvent event = service.recordManagementEvent(100,
                "SENDER_UPDATED", "sender-1", null,
                "subscription:sender:1", data);

        assertTrue(event.getPayloadJson().contains("\"status\":\"READY\""));
        assertFalse(event.getPayloadJson().contains("\"channel\""));
        assertFalse(event.getPayloadJson().contains("\"rssid\""));
        assertFalse(event.getPayloadJson().contains("\"provider\""));
        assertFalse(event.getPayloadJson().contains("\"platform\""));
    }

    private RCSCdrInfo cdr() {
        RCSCdrInfo cdr = new RCSCdrInfo();
        cdr.setSsid(100);
        cdr.setMsgId("msg-1");
        cdr.setChannelCode("INFOBIP");
        cdr.setChannelMsgId("provider-msg-1");
        cdr.setSender("infobip-sender");
        cdr.setMsisdn("+8613800138000");
        return cdr;
    }

    private RcsDrEvent dr(Integer status, String errorCode) {
        RcsDrEvent dr = new RcsDrEvent();
        dr.setStatus(status);
        dr.setChannelMsgId("provider-msg-1");
        dr.setErrorCode(errorCode);
        dr.setErrorReason(errorCode);
        return dr;
    }

    private RcsMoInfo inbound(String eventType, String contentType) {
        RcsMoInfo inbound = new RcsMoInfo();
        inbound.setId(42L);
        inbound.setSsid(100);
        inbound.setChannelCode("INFOBIP");
        inbound.setAgentId("sender-1");
        inbound.setMsisdn("+8613800138000");
        inbound.setClientMsgId("provider-inbound-42-" + eventType + "-" + contentType);
        inbound.setEventType(eventType);
        inbound.setContentType(contentType);
        inbound.setText("hello");
        inbound.setFileUri("https://media.example.com/file");
        inbound.setMimeType("image/jpeg");
        inbound.setLatitude(31.2);
        inbound.setLongitude(121.5);
        inbound.setPostbackData("confirm");
        inbound.setSuggestionText("Confirm");
        return inbound;
    }
}

package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.EnumSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.BillingCdrInfo;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsSendTask;
import com.liugh.rcs.channel.RcsCallbackResult;
import com.liugh.rcs.channel.RcsChannelAdapter;
import com.liugh.rcs.channel.RcsChannelAdapterRegistry;
import com.liugh.rcs.channel.RcsChannelCapability;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.model.RcsContent;
import com.liugh.service.entity.SubmitResponse;

public class RcsMessageAsyncSendServiceTest {

    private RcsMessagePersistenceService persistenceService;
    private RcsFileService fileService;
    private RcsRouter router;
    private RcsChannelAdapterRegistry adapterRegistry;
    private RcsCustomerAccessService accessService;
    private CapturingAdapter adapter;
    private RcsMessageService service;
    private AdminInfo customer;
    private AdminInfo supplier;

    @Before
    public void setUp() {
        persistenceService = mock(RcsMessagePersistenceService.class);
        fileService = mock(RcsFileService.class);
        router = mock(RcsRouter.class);
        adapterRegistry = mock(RcsChannelAdapterRegistry.class);
        accessService = mock(RcsCustomerAccessService.class);
        adapter = new CapturingAdapter();
        service = new RcsMessageService();

        ReflectionTestUtils.setField(service, "persistenceService",
                persistenceService);
        ReflectionTestUtils.setField(service, "rcsFileService", fileService);
        ReflectionTestUtils.setField(service, "rcsRouter", router);
        ReflectionTestUtils.setField(service, "adapterRegistry",
                adapterRegistry);
        ReflectionTestUtils.setField(service, "customerAccessService",
                accessService);
        ReflectionTestUtils.setField(service, "projectUrl",
                "https://platform.example.com/api/v1/");
        service.setSendMode("ASYNC");

        customer = new AdminInfo();
        customer.setId(100L);
        customer.setSsid(100);
        supplier = new AdminInfo();
        supplier.setId(901L);
        supplier.setChannelCode("INFOBIP");
        supplier.setUrl("https://infobip.example.com");
        supplier.setAppId("api-key");

        when(accessService.requireCustomer(any(JSONObject.class)))
                .thenReturn(customer);
        when(router.route(any(AdminInfo.class), any(), any(), any()))
                .thenReturn(supplier);
        when(adapterRegistry.get("INFOBIP")).thenReturn(adapter);
        when(fileService.resolve(any(String.class))).thenAnswer(
                invocation -> invocation.getArguments()[0]);
    }

    @Test
    public void asyncModePersistsCdrBillingAndTaskBeforeReturning() {
        SubmitResponse response = service.send(request());

        assertTrue(response.isStatus());
        assertEquals(0, adapter.sendCount);

        ArgumentCaptor<RCSCdrInfo> cdr =
                ArgumentCaptor.forClass(RCSCdrInfo.class);
        ArgumentCaptor<BillingCdrInfo> billing =
                ArgumentCaptor.forClass(BillingCdrInfo.class);
        ArgumentCaptor<RcsSendTask> task =
                ArgumentCaptor.forClass(RcsSendTask.class);
        verify(persistenceService).acceptQueued(cdr.capture(),
                billing.capture(), task.capture());

        assertEquals(response.getOutMsgId(), cdr.getValue().getMsgId());
        assertEquals(Integer.valueOf(-1), cdr.getValue().getStatus());
        assertEquals(response.getOutMsgId(), billing.getValue().getMsgId());
        assertEquals(Integer.valueOf(-1), billing.getValue().getOstatus());
        assertEquals(Integer.valueOf(-1), billing.getValue().getRstatus());
        assertEquals(response.getOutMsgId(), task.getValue().getMsgId());
        assertEquals("QUEUED", task.getValue().getTaskStatus());
        assertEquals(Integer.valueOf(0), task.getValue().getAttemptCount());
        assertTrue(task.getValue().getRequestJson().contains("\"content\""));
    }

    @Test
    public void directModeWritesBillingBeforeAdapterAndUpdatesSubmittedState() {
        service.setSendMode("DIRECT");
        adapter.channelMsgId = "provider-1";

        SubmitResponse response = service.send(request());

        assertTrue(response.isStatus());
        verify(persistenceService).acceptDirect(any(RCSCdrInfo.class),
                any(BillingCdrInfo.class));
        verify(persistenceService).markSubmitted(any(RCSCdrInfo.class),
                org.mockito.Matchers.eq("provider-1"));
        verify(persistenceService, never()).acceptQueued(any(RCSCdrInfo.class),
                any(BillingCdrInfo.class), any(RcsSendTask.class));
    }

    @Test
    public void directModeMarksFinalFailureWhenAdapterRejects() {
        service.setSendMode("DIRECT");
        adapter.channelMsgId = null;

        SubmitResponse response = service.send(request());

        assertFalse(response.isStatus());
        verify(persistenceService).acceptDirect(any(RCSCdrInfo.class),
                any(BillingCdrInfo.class));
        verify(persistenceService).markFinalFailure(any(RCSCdrInfo.class),
                org.mockito.Matchers.eq("submit rejected"));
    }

    private JSONObject request() {
        return JSONObject.parseObject("{"
                + "\"token\":\"customer-token\","
                + "\"sender\":\"agent-sender\","
                + "\"recipient\":\"+8613800138000\","
                + "\"content\":{\"type\":\"TEXT\",\"text\":\"hello\"}"
                + "}");
    }

    private static final class CapturingAdapter implements RcsChannelAdapter {
        private String channelMsgId = "provider-1";
        private int sendCount;

        @Override
        public String channelCode() {
            return "INFOBIP";
        }

        @Override
        public java.util.Set<RcsChannelCapability> capabilities() {
            return Collections.unmodifiableSet(
                    EnumSet.of(RcsChannelCapability.MESSAGE_SEND));
        }

        @Override
        public String send(RcsContent content, RcsSendCtx ctx) {
            sendCount++;
            return channelMsgId;
        }

        @Override
        public RcsCallbackResult parseCallback(JSONObject raw) {
            return new RcsCallbackResult();
        }
    }
}

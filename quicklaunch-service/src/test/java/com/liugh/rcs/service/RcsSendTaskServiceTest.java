package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsSendTask;
import com.liugh.mapper.RCSCdrInfoMapper;
import com.liugh.mapper.RcsSendTaskMapper;
import com.liugh.rcs.channel.RcsCallbackResult;
import com.liugh.rcs.channel.RcsChannelAdapter;
import com.liugh.rcs.channel.RcsChannelAdapterRegistry;
import com.liugh.rcs.channel.RcsChannelCapability;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.model.RcsContent;
import com.liugh.service.util.GlobalMap;

public class RcsSendTaskServiceTest {

    private RcsSendTaskMapper taskMapper;
    private RCSCdrInfoMapper cdrMapper;
    private RcsMessagePersistenceService persistenceService;
    private RcsChannelAdapterRegistry adapterRegistry;
    private RcsFileService fileService;
    private CapturingAdapter adapter;
    private RcsSendTaskService service;
    private RcsCallbackService callbackService;
    private RcsSendTask task;
    private RCSCdrInfo cdr;

    @Before
    public void setUp() {
        taskMapper = mock(RcsSendTaskMapper.class);
        cdrMapper = mock(RCSCdrInfoMapper.class);
        persistenceService = mock(RcsMessagePersistenceService.class);
        adapterRegistry = mock(RcsChannelAdapterRegistry.class);
        fileService = mock(RcsFileService.class);
        callbackService = mock(RcsCallbackService.class);
        adapter = new CapturingAdapter();
        service = new RcsSendTaskService(taskMapper, cdrMapper,
                persistenceService, adapterRegistry, fileService,callbackService);

        AdminInfo supplier = new AdminInfo();
        supplier.setId(901L);
        supplier.setChannelCode("INFOBIP");
        supplier.setUrl("https://infobip.example.com");
        supplier.setAppId("api-key");
        GlobalMap.rcsadmin.put("901", supplier);

        task = new RcsSendTask();
        task.setMsgId("msg-1");
        task.setSsid(100);
        task.setRssid(901);
        task.setChannelCode("INFOBIP");
        task.setTaskStatus("QUEUED");
        task.setAttemptCount(0);
        task.setMaxRetries(3);
        task.setNextAttemptTime(new Date(0));
        task.setRequestJson("{\"content\":{\"type\":\"TEXT\",\"text\":\"hi\"}}");

        cdr = new RCSCdrInfo();
        cdr.setMsgId("msg-1");
        cdr.setSsid(100);
        cdr.setRssid(901);
        cdr.setChannelCode("INFOBIP");
        cdr.setSender("agent-sender");
        cdr.setMsisdn("+8613800138000");
        cdr.setStatus(-1);

        when(cdrMapper.selectList(any()))
                .thenReturn(Collections.singletonList(cdr));
        when(adapterRegistry.get("INFOBIP")).thenReturn(adapter);
        when(taskMapper.update(any(RcsSendTask.class), any())).thenReturn(1);
        when(fileService.resolve(any(String.class))).thenAnswer(
                invocation -> invocation.getArguments()[0]);
    }

    @After
    public void tearDown() {
        service.shutdown();
        GlobalMap.rcsadmin.remove("901");
    }

    @Test
    public void successfulSendMarksTaskAndBillingSubmitted() {
        adapter.channelMsgId = "provider-1";

        service.process(task);

        assertEquals("msg-1", adapter.ctx.getMsgId());
        verify(persistenceService).markSubmitted(cdr, "provider-1");
        verify(persistenceService).markTaskSucceeded(task);
    }

    @Test
    public void retryableFailureKeepsBillingPendingUntilRetryLimit() {
        adapter.channelMsgId = null;

        service.process(task);

        verify(persistenceService).markTaskRetry(task, "submit returned null");
    }

    @Test
    public void finalFailureUpdatesCdrBillingAndTask() {
        adapter.channelMsgId = null;
        task.setAttemptCount(2);

        service.process(task);

        verify(persistenceService).markTaskFailed(task, cdr,
                "submit returned null");
    }

    @Test
    public void deliverPendingSendsClaimedTaskOnChannelWorker() {
        adapter.channelMsgId = "provider-1";
        service.setEnabled(true);
        when(adapterRegistry.channelCodes())
                .thenReturn(Collections.singleton("INFOBIP"));
        when(taskMapper.selectList(any()))
                .thenReturn(Collections.singletonList(task));
        when(persistenceService.claim(task)).thenReturn(true);

        service.deliverPending();
        service.shutdown();

        assertTrue(adapter.threadName.startsWith("rcs-send-infobip-"));
        verify(persistenceService).markSubmitted(cdr, "provider-1");
        verify(persistenceService).markTaskSucceeded(task);
    }

    @Test
    public void deliverPendingSkipsScanWhenChannelHasNoFreeCapacity()
            throws Exception {
        adapter.channelMsgId = "provider-1";
        adapter.started = new CountDownLatch(1);
        adapter.release = new CountDownLatch(1);
        service.setEnabled(true);
        service.setThreadsPerChannel(1);
        service.setQueuePerChannel(0);
        when(adapterRegistry.channelCodes())
                .thenReturn(Collections.singleton("INFOBIP"));
        when(taskMapper.selectList(any()))
                .thenReturn(Collections.singletonList(task));
        when(persistenceService.claim(task)).thenReturn(true);

        service.deliverPending();
        assertTrue(adapter.started.await(5, TimeUnit.SECONDS));
        service.deliverPending();
        adapter.release.countDown();
        service.shutdown();

        verify(taskMapper, times(1)).selectList(any());
        verify(persistenceService, times(1)).claim(task);
    }

    @Test
    public void failedClaimReleasesCapacityWithoutSending() {
        service.setEnabled(true);
        service.setThreadsPerChannel(1);
        service.setQueuePerChannel(0);
        when(adapterRegistry.channelCodes())
                .thenReturn(Collections.singleton("INFOBIP"));
        when(taskMapper.selectList(any()))
                .thenReturn(Collections.singletonList(task));
        when(persistenceService.claim(task)).thenReturn(false);

        service.deliverPending();
        service.deliverPending();
        service.shutdown();

        verify(taskMapper, times(2)).selectList(any());
        verify(persistenceService, never()).markTaskSucceeded(task);
    }

    private static final class CapturingAdapter implements RcsChannelAdapter {
        private String channelMsgId;
        private RcsSendCtx ctx;
        private volatile String threadName;
        private CountDownLatch started;
        private CountDownLatch release;

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
        public String send(RcsContent content, RcsSendCtx ctx)
                throws Exception {
            this.ctx = ctx;
            this.threadName = Thread.currentThread().getName();
            if (started != null) {
                started.countDown();
                release.await(5, TimeUnit.SECONDS);
            }
            return channelMsgId;
        }

        @Override
        public RcsCallbackResult parseCallback(JSONObject raw) {
            return new RcsCallbackResult();
        }
    }
}

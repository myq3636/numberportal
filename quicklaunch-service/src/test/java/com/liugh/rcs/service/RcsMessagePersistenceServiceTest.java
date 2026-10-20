package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import com.liugh.entity.BillingCdrInfo;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsSendTask;
import com.liugh.mapper.RCSCdrInfoMapper;
import com.liugh.mapper.RcsSendTaskMapper;
import com.liugh.service.IBillingCdrInfoService;

public class RcsMessagePersistenceServiceTest {

    private RCSCdrInfoMapper cdrMapper;
    private IBillingCdrInfoService billingService;
    private RcsSendTaskMapper taskMapper;
    private RcsMessagePersistenceService service;

    @Before
    public void setUp() {
        cdrMapper = mock(RCSCdrInfoMapper.class);
        billingService = mock(IBillingCdrInfoService.class);
        taskMapper = mock(RcsSendTaskMapper.class);
        service = new RcsMessagePersistenceService(cdrMapper, billingService,
                taskMapper);
    }

    @Test
    public void queuedAcceptanceInsertsCdrBillingAndTaskTogether() {
        RCSCdrInfo cdr = cdr();
        BillingCdrInfo billing = billing(-1);
        RcsSendTask task = new RcsSendTask();
        task.setMsgId("msg-1");

        service.acceptQueued(cdr, billing, task);

        verify(cdrMapper).insert(cdr);
        verify(billingService).insert(billing);
        verify(taskMapper).insert(task);
    }

    @Test
    public void submittedStateUpdatesSameBillingCdrOstatus() {
        RCSCdrInfo cdr = cdr();
        BillingCdrInfo billing = billing(-1);
        when(billingService.selectBillingCdr("msg-1")).thenReturn(billing);

        service.markSubmitted(cdr, "provider-1");

        assertEquals(Integer.valueOf(0), cdr.getStatus());
        assertEquals("provider-1", cdr.getChannelMsgId());
        assertEquals(Integer.valueOf(0), billing.getOstatus());
        verify(cdrMapper).updateById(cdr);
        verify(billingService).updateById(billing);
    }

    @Test
    public void finalFailureUpdatesSameBillingCdrOstatus() {
        RCSCdrInfo cdr = cdr();
        BillingCdrInfo billing = billing(-1);
        when(billingService.selectBillingCdr("msg-1")).thenReturn(billing);

        service.markFinalFailure(cdr, "submit rejected");

        assertEquals(Integer.valueOf(1), cdr.getStatus());
        assertEquals(Integer.valueOf(1), billing.getOstatus());
        verify(cdrMapper).updateById(cdr);
        verify(billingService).updateById(billing);
    }

    @Test
    public void finalFailureWithErrorCodePersistsCodeAndReason() {
        RCSCdrInfo cdr = cdr();
        BillingCdrInfo billing = billing(-1);
        when(billingService.selectBillingCdr("msg-1")).thenReturn(billing);

        service.markFinalFailure(cdr, "SPARK_401", "bad creds");

        assertEquals(Integer.valueOf(1), cdr.getStatus());
        assertEquals("SPARK_401", cdr.getErrorCode());
        assertEquals("bad creds", cdr.getErrorReason());
        assertEquals(Integer.valueOf(1), billing.getOstatus());
        verify(cdrMapper).updateById(cdr);
        verify(billingService).updateById(billing);
    }

    @Test
    public void taskFailedWithErrorCodePersistsCode() {
        RcsSendTask task = new RcsSendTask();
        task.setMsgId("msg-1");
        task.setAttemptCount(0);
        RCSCdrInfo cdr = cdr();
        BillingCdrInfo billing = billing(-1);
        when(billingService.selectBillingCdr("msg-1")).thenReturn(billing);

        service.markTaskFailed(task, cdr, "SPARK_500", "boom");

        assertEquals("SPARK_500", cdr.getErrorCode());
        assertEquals("boom", cdr.getErrorReason());
        assertEquals(Integer.valueOf(1), task.getAttemptCount());
        assertEquals("FAILED", task.getTaskStatus());
        verify(cdrMapper).updateById(cdr);
        verify(taskMapper).updateById(task);
    }

    @Test
    public void successfulTaskRecordsAttemptCount() {
        RcsSendTask task = new RcsSendTask();
        task.setMsgId("msg-1");
        task.setAttemptCount(0);

        service.markTaskSucceeded(task);

        assertEquals(Integer.valueOf(1), task.getAttemptCount());
        assertEquals("SUCCEEDED", task.getTaskStatus());
        verify(taskMapper).updateById(task);
    }

    private RCSCdrInfo cdr() {
        RCSCdrInfo cdr = new RCSCdrInfo();
        cdr.setMsgId("msg-1");
        cdr.setStatus(-1);
        return cdr;
    }

    private BillingCdrInfo billing(int ostatus) {
        BillingCdrInfo billing = new BillingCdrInfo();
        billing.setMsgId("msg-1");
        billing.setOstatus(ostatus);
        return billing;
    }
}

package com.liugh.rcs.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.BillingCdrInfo;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsSendTask;
import com.liugh.mapper.RCSCdrInfoMapper;
import com.liugh.mapper.RcsSendTaskMapper;
import com.liugh.service.IBillingCdrInfoService;
import com.liugh.util.QuotaManagerUtil;

@Service
public class RcsMessagePersistenceService {

    private final RCSCdrInfoMapper cdrMapper;
    private final IBillingCdrInfoService billingCdrInfoService;
    private final RcsSendTaskMapper taskMapper;

    @Autowired
    public RcsMessagePersistenceService(RCSCdrInfoMapper cdrMapper,
            IBillingCdrInfoService billingCdrInfoService,
            RcsSendTaskMapper taskMapper) {
        this.cdrMapper = cdrMapper;
        this.billingCdrInfoService = billingCdrInfoService;
        this.taskMapper = taskMapper;
    }

    @Transactional
    public void acceptQueued(RCSCdrInfo cdr, BillingCdrInfo billing,
            RcsSendTask task) {
        cdrMapper.insert(cdr);
        billingCdrInfoService.insert(billing);
        taskMapper.insert(task);
    }

    @Transactional
    public void acceptDirect(RCSCdrInfo cdr, BillingCdrInfo billing) {
        cdrMapper.insert(cdr);
        billingCdrInfoService.insert(billing);
    }

    @Transactional
    public void markSubmitted(RCSCdrInfo cdr, String channelMsgId) {
        cdr.setChannelMsgId(channelMsgId);
        cdr.setStatus(0);
        cdr.setErrorCode(null);
        cdr.setErrorReason(null);
        cdrMapper.updateById(cdr);
        updateBillingOstatus(cdr.getMsgId(), 0);
    }

    @Transactional
    public void markFinalFailure(RCSCdrInfo cdr, String error) {
        markFinalFailure(cdr, null, error);
    }

    @Transactional
    public void markFinalFailure(RCSCdrInfo cdr, String errorCode, String errorReason) {
        cdr.setStatus(1);
        cdr.setErrorCode(errorCode);
        cdr.setErrorReason(limit(errorReason));
        long now = System.currentTimeMillis();
        cdr.setOutsubmittime(now);
        cdrMapper.updateById(cdr);
        updateBillingOstatus(cdr.getMsgId(), 1);
    }

    @Transactional
    public void markTaskSucceeded(RcsSendTask task) {
        task.setAttemptCount(attemptCount(task) + 1);
        task.setTaskStatus("SUCCEEDED");
        task.setNextAttemptTime(null);
        task.setLastError(null);
        task.setUpdateTime(new Date());
        taskMapper.updateById(task);
    }

    @Transactional
    public void markTaskRetry(RcsSendTask task, String error) {
        int attempt = attemptCount(task) + 1;
        task.setAttemptCount(attempt);
        task.setTaskStatus("RETRY");
        task.setLastError(limit(error));
        task.setNextAttemptTime(new Date(System.currentTimeMillis()
                + backoffMillis(attempt)));
        task.setUpdateTime(new Date());
        taskMapper.updateById(task);
    }

    @Transactional
    public void markTaskFailed(RcsSendTask task, RCSCdrInfo cdr,
            String error) {
        markTaskFailed(task, cdr, null, error);
    }

    @Transactional
    public void markTaskFailed(RcsSendTask task, RCSCdrInfo cdr,
            String errorCode, String errorReason) {
        cdr.setStatus(1);
        cdr.setErrorCode(errorCode);
        cdr.setErrorReason(limit(errorReason));
        cdrMapper.updateById(cdr);
        updateBillingOstatus(cdr.getMsgId(), 1);

        task.setAttemptCount(attemptCount(task) + 1);
        task.setTaskStatus("FAILED");
        task.setNextAttemptTime(null);
        task.setLastError(limit(errorReason));
        task.setUpdateTime(new Date());
        taskMapper.updateById(task);
    }

    /**
     * Blackhole / 即时投递通道专用：一次事务内完成 submit + delivered 两步，
     * 跳过 submit→callback 流程，直接将 CDR 推进为终态 status=3。
     */
    @Transactional
    public void markDirectDelivered(RCSCdrInfo cdr, String channelMsgId) {
        long now = System.currentTimeMillis();
        cdr.setChannelMsgId(channelMsgId);
        cdr.setStatus(3);
        cdr.setDeliveredTime(now);
        cdr.setOutdrtime(now);
        cdr.setOutsubmittime(now);
        cdr.setIndrtime(now);
        cdr.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(now, "yyyy-MM-dd HH:mm:ss", "GMT+8"));
        cdrMapper.updateById(cdr);

        BillingCdrInfo billing = billingCdrInfoService.selectBillingCdr(cdr.getMsgId());
        if (billing != null) {
            billing.setOstatus(0);
            billing.setRstatus(3);
            billing.setOutdrtime(now);
            billing.setOutsubmittime(now);
            billingCdrInfoService.updateById(billing);
        }
    }

    public boolean claim(RcsSendTask task) {
        RcsSendTask update = new RcsSendTask();
        update.setTaskStatus("PROCESSING");
        update.setUpdateTime(new Date());
        return taskMapper.update(update, new EntityWrapper<RcsSendTask>()
                .eq("msg_id", task.getMsgId())
                .eq("task_status", task.getTaskStatus())
                .eq("attempt_count", attemptCount(task))) > 0;
    }

    private void updateBillingOstatus(String msgId, int ostatus) {
        BillingCdrInfo billing = billingCdrInfoService.selectBillingCdr(msgId);
        if (billing != null) {
            billing.setOstatus(ostatus);
            billing.setRstatus(ostatus);
            long now = System.currentTimeMillis();
            billing.setOutsubmittime(now);
            billingCdrInfoService.updateById(billing);
        }
    }

    private static int attemptCount(RcsSendTask task) {
        return task.getAttemptCount() == null ? 0 : task.getAttemptCount();
    }

    private static long backoffMillis(int attemptNo) {
        long seconds = 30L * (1L << Math.min(attemptNo - 1, 7));
        return Math.min(seconds, 3600L) * 1000L;
    }

    private static String limit(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 512 ? value.substring(0, 512) : value;
    }
}

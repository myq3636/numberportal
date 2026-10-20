package com.liugh.rcs.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.BillingCdrInfo;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsMoInfo;
import com.liugh.mapper.RCSCdrInfoMapper;
import com.liugh.mapper.RcsChannelAgentMapper;
import com.liugh.mapper.RcsMoInfoMapper;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.rcs.channel.RcsCallbackResult;
import com.liugh.rcs.channel.RcsChannelAdapter;
import com.liugh.rcs.channel.RcsChannelAdapterRegistry;
import com.liugh.rcs.channel.RcsDrEvent;
import com.liugh.service.IBillingCdrInfoService;
import com.liugh.service.util.GlobalMap;
import com.liugh.service.util.ThirdPartyLoginHelper;
import com.liugh.util.QuotaManagerUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * RCS 回调编排（通道无关）：adapter 归一化 → 落库 CDR/计费 → DR 回推给客户 → 上行落库。
 */
@Service
@Slf4j
public class RcsCallbackService {

    @Autowired
    private RCSCdrInfoMapper rcsCdrInfoMapper;
    @Autowired
    private RcsMoInfoMapper rcsMoInfoMapper;
    @Autowired
    private IBillingCdrInfoService billingCdrInfoService;
    @Autowired
    private RcsChannelAdapterRegistry adapterRegistry;
    @Autowired
    private RcsChannelAgentMapper rcsChannelAgentMapper;
    @Autowired
    private RcsCustomerEventService customerEventService;

    /**
     * Blackhole / 即时投递通道调用：构造合成 DR 事件，走与真实 DR 相同的两条
     * 客户通知路径（legacy GET 回调 + webhook 事件），无需等待上游 callback。
     */
    public void fireSyntheticDelivery(RCSCdrInfo cdr) {
        try {
            RcsDrEvent syntheticDr = new RcsDrEvent();
            syntheticDr.setOurMsgId(cdr.getMsgId());
            syntheticDr.setChannelMsgId(cdr.getChannelMsgId());
            syntheticDr.setStatus(3);

            pushDrToCustomer(cdr);
            customerEventService.recordDr(cdr, syntheticDr);
        } catch (Exception e) {
            log.error("rcs synthetic DR delivery failed, msgId:{}", cdr.getMsgId(), e);
        }
    }

    /** 处理某通道的一条回调（原始 JSON）。 */
    public boolean handle(String channelCode, JSONObject raw) {
        try {
            RcsChannelAdapter adapter = adapterRegistry.get(channelCode);
            if (adapter == null) {
                log.error("rcs callback no adapter for channel {}", channelCode);
                return false;
            }
            RcsCallbackResult r = adapter.parseCallback(raw);
            for (RcsDrEvent dr : r.getDrEvents()) {
                applyDr(dr);
            }
            for (RcsMoInfo mo : r.getInboundEvents()) {
                applyInbound(mo);
            }
        } catch (Exception e) {
            log.error("rcs callback throw error, channel:{}", channelCode, e);
        }
        return true;
    }

    /** Handles Infobip asynchronous capability results. */
    public boolean handleCapabilityResults(String channelCode, JSONObject raw) {
        try {
            customerEventService.recordCapabilityResults(channelCode, raw);
            return true;
        } catch (Exception e) {
            log.error("rcs capability callback failed, channel:{}", channelCode, e);
            return false;
        }
    }

    private void applyDr(RcsDrEvent dr) {
        RCSCdrInfo cdr = findCdr(dr.getOurMsgId(), dr.getChannelMsgId());
        if (cdr == null) {
            log.warn("rcs DR no matching cdr, ourMsgId:{}, channelMsgId:{}", dr.getOurMsgId(), dr.getChannelMsgId());
            return;
        }
        long now = System.currentTimeMillis();

        // 价格 / 降级 id 回填
        if (dr.getCost() != null) {
            cdr.setCost(dr.getCost());
        }
        if (dr.getCurrency() != null) {
            cdr.setCurrency(dr.getCurrency());
        }
        if (dr.getFallbackMsgId() != null) {
            cdr.setFallbackMsgId(dr.getFallbackMsgId());
        }

        Integer status = dr.getStatus();
        if (status == null) {
            // 中间态：仅回填价格，不推进状态、不回推客户
            rcsCdrInfoMapper.updateById(cdr);
            return;
        }

        cdr.setStatus(status);
        Integer rstatus = null;
        if (status == 3) {
            cdr.setDeliveredTime(now);
            rstatus = 3;
        } else if (status == 9) {
            cdr.setReadTime(now);
        } else if (status == 4) {
            cdr.setErrorCode(dr.getErrorCode());
            cdr.setErrorReason(dr.getErrorReason());
            rstatus = 4;
        }
        // 5 过期 / 12 降级：不动计费
        cdr.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(now, "yyyy-MM-dd HH:mm:ss", "GMT+8"));
        cdr.setOutdrtime(now);
        cdr.setIndrtime(now);
        rcsCdrInfoMapper.updateById(cdr);

        if (rstatus != null) {
            BillingCdrInfo billing = billingCdrInfoService.selectBillingCdr(cdr.getMsgId());
            if (billing != null && billing.getId() != null && billing.getId() > 0) {
                billing.setRstatus(rstatus);
                billing.setOutdrtime(now);
                billingCdrInfoService.updateById(billing);
            }
        }

        // 归一化后的终态 DR 回推给客户（与原 SMS 平台一致）
        pushDrToCustomer(cdr);
        customerEventService.recordDr(cdr, dr);
    }

    private void applyInbound(RcsMoInfo mo) {
        try {
            resolveInboundOwner(mo);
            rcsMoInfoMapper.insert(mo);
            customerEventService.recordInbound(mo);
        } catch (Exception e) {
            log.error("rcs inbound insert failed, msisdn:{}", mo.getMsisdn(), e);
        }
    }

    private void resolveInboundOwner(RcsMoInfo mo) {
        if (mo == null || mo.getSsid() != null || mo.getAgentId() == null) {
            return;
        }
        RcsChannelAgent binding = first(rcsChannelAgentMapper.selectList(
                new EntityWrapper<RcsChannelAgent>()
                        .eq("channel_code", mo.getChannelCode())
                        .eq("sender", mo.getAgentId())));
        if (binding != null) {
            mo.setSsid(binding.getSsid());
            mo.setAgentId(binding.getAgentId());
        }
    }

    /**
     * 把归一化后的 RCS DR 状态回推给客户。
     * 与原平台一致：GET {customer.url}?REFERENCE={msgId}&STATUS={status}。
     * status 复用 SMS 状态码：3 已送达 / 4 送达失败 / 5 过期 / 9 已读 / 12 已降级。
     */
    private void pushDrToCustomer(RCSCdrInfo cdr) {
        try {
            AdminInfo customer = GlobalMap.rcsadmin.get("" + cdr.getSsid());
            if (customer == null || customer.getUrl() == null || customer.getUrl().trim().isEmpty()) {
                log.warn("rcs DR push skipped, no customer url, ssid:{}, msgId:{}", cdr.getSsid(), cdr.getMsgId());
                return;
            }
            if(customer.getToken()!=null && customer.getToken().contains("jxrcs")) {
            	ThirdPartyLoginHelper.sendDRToSMSGW(customer.getUrl(), cdr.getMsgId(), cdr.getStatus());
            }else {
            	ThirdPartyLoginHelper.sendRCSDRToSMSGW(customer.getUrl(), cdr, customer.getToken());
            }
            
           
            BillingCdrInfo billing = billingCdrInfoService.selectBillingCdr(cdr.getMsgId());
            if (billing != null && billing.getId() != null && billing.getId() > 0) {
                billing.setOstatus(cdr.getStatus());
                billing.setIndrtime(cdr.getIndrtime());
                billingCdrInfoService.updateById(billing);
            }
        } catch (Exception e) {
            log.error("rcs DR push to customer failed, msgId:{}", cdr.getMsgId(), e);
        }
    }

    private RCSCdrInfo findCdr(String ourMsgId, String channelMsgId) {
        if (ourMsgId != null && !ourMsgId.trim().isEmpty()) {
            RCSCdrInfo cdr = first(rcsCdrInfoMapper.selectList(new EntityWrapper<RCSCdrInfo>().eq("msg_id", ourMsgId)));
            if (cdr != null) {
                return cdr;
            }
        }
        if (channelMsgId != null && !channelMsgId.trim().isEmpty()) {
            return first(rcsCdrInfoMapper.selectList(new EntityWrapper<RCSCdrInfo>().eq("channel_msg_id", channelMsgId)));
        }
        return null;
    }

    private static <T> T first(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }
}

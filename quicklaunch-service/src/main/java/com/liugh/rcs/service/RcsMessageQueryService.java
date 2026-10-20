package com.liugh.rcs.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.mapper.RCSCdrInfoMapper;
import com.liugh.mapper.RcsChannelAgentMapper;

/**
 * Tenant-scoped unified RCS message status query.
 */
@Service
public class RcsMessageQueryService {

    private final RcsCustomerAccessService accessService;
    private final RCSCdrInfoMapper cdrMapper;
    private final RcsChannelAgentMapper bindingMapper;

    @Autowired
    public RcsMessageQueryService(RcsCustomerAccessService accessService,
            RCSCdrInfoMapper cdrMapper,
            RcsChannelAgentMapper bindingMapper) {
        this.accessService = accessService;
        this.cdrMapper = cdrMapper;
        this.bindingMapper = bindingMapper;
    }

    public JSONObject get(JSONObject authRequest, String messageId) {
        accessService.requirePhase1Enabled();
        AdminInfo customer = accessService.requireCustomer(authRequest);
        RCSCdrInfo cdr = first(cdrMapper.selectList(
                new EntityWrapper<RCSCdrInfo>().eq("msg_id", messageId)));
        accessService.requireOwned(customer.getSsid(),
                cdr == null ? null : cdr.getSsid());

        JSONObject result = new JSONObject();
        result.put("messageId", cdr.getMsgId());
        result.put("senderId", resolveSenderId(cdr));
        result.put("recipient", cdr.getMsisdn());
        result.put("messageType", cdr.getMsgType());
        result.put("status", status(cdr.getStatus()));
        result.put("createdAt", cdr.getCreateTime());
        result.put("deliveredAt", cdr.getDeliveredTime());
        result.put("seenAt", cdr.getReadTime());
        if (cdr.getErrorCode() != null) {
            result.put("errorCode", cdr.getErrorCode());
            result.put("errorSummary", cdr.getErrorReason());
        }
        return result;
    }

    private String resolveSenderId(RCSCdrInfo cdr) {
        RcsChannelAgent binding = first(bindingMapper.selectList(
                new EntityWrapper<RcsChannelAgent>()
                        .eq("ssid", cdr.getSsid())
                        .eq("channel_code", cdr.getChannelCode())
                        .eq("sender", cdr.getSender())));
        return binding == null ? null : binding.getAgentId();
    }

    private String status(Integer status) {
        if (status == null) {
            return "UNKNOWN";
        }
        switch (status) {
        case -1:
            return "CREATED";
        case 0:
            return "SUBMITTED";
        case 1:
            return "REJECTED";
        case 3:
            return "DELIVERED";
        case 4:
            return "FAILED";
        case 5:
            return "EXPIRED";
        case 9:
            return "SEEN";
        case 12:
            return "FALLBACK";
        default:
            return "UNKNOWN";
        }
    }

    private static <T> T first(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}

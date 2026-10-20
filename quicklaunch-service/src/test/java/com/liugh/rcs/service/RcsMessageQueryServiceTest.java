package com.liugh.rcs.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Test;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.mapper.RCSCdrInfoMapper;
import com.liugh.mapper.RcsChannelAgentMapper;

public class RcsMessageQueryServiceTest {

    @Test
    public void returnsTenantMessageWithoutProviderMetadata() {
        RcsCustomerAccessService access = mock(RcsCustomerAccessService.class);
        RCSCdrInfoMapper cdrMapper = mock(RCSCdrInfoMapper.class);
        RcsChannelAgentMapper bindingMapper = mock(RcsChannelAgentMapper.class);
        RcsMessageQueryService service = new RcsMessageQueryService(
                access, cdrMapper, bindingMapper);

        AdminInfo customer = new AdminInfo();
        customer.setSsid(100);
        when(access.requireCustomer(any(JSONObject.class))).thenReturn(customer);
        RCSCdrInfo cdr = new RCSCdrInfo();
        cdr.setSsid(100);
        cdr.setRssid(901);
        cdr.setChannelCode("INFOBIP");
        cdr.setChannelMsgId("provider-message");
        cdr.setMsgId("msg-1");
        cdr.setSender("infobip-sender");
        cdr.setMsisdn("+8613800138000");
        cdr.setMsgType("TEXT");
        cdr.setStatus(3);
        when(cdrMapper.selectList(any()))
                .thenReturn(Collections.singletonList(cdr));
        RcsChannelAgent binding = new RcsChannelAgent();
        binding.setSsid(100);
        binding.setAgentId("sender-1");
        when(bindingMapper.selectList(any()))
                .thenReturn(Collections.singletonList(binding));

        JSONObject result = service.get(new JSONObject(), "msg-1");

        assertEquals("msg-1", result.getString("messageId"));
        assertEquals("sender-1", result.getString("senderId"));
        assertEquals("DELIVERED", result.getString("status"));
        assertFalse(result.containsKey("channelCode"));
        assertFalse(result.containsKey("rssid"));
        assertFalse(result.containsKey("channelMessageId"));
        verify(access).requireOwned(100, 100);
    }
}

package com.liugh.service.impl;


import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.liugh.entity.BillingCdrInfo;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.mapper.BillingCdrInfoMapper;
import com.liugh.service.IBillingCdrInfoService;

import org.springframework.stereotype.Service;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author liugh123
 * @since 2019-06-24
 */
@Service
@Slf4j
public class BillingCdrInfoServiceImpl extends
		ServiceImpl<BillingCdrInfoMapper, BillingCdrInfo> implements IBillingCdrInfoService {@Override
	public BillingCdrInfo selectBillingCdr(String msgid) {
		return this.selectOne(new EntityWrapper<BillingCdrInfo>().eq("msg_id", msgid));
	}		
}

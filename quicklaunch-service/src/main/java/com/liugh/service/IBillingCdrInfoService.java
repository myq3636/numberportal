package com.liugh.service;

import com.liugh.entity.BillingCdrInfo;
import com.baomidou.mybatisplus.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author liugh123
 * @since 2019-06-24
 */
public interface IBillingCdrInfoService extends IService<BillingCdrInfo> {
	
	public BillingCdrInfo selectBillingCdr(String msgid);
	
}

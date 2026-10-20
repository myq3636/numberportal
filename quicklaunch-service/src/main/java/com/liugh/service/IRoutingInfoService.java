package com.liugh.service;


import java.util.List;

import com.liugh.entity.RoutingInfo;
import com.baomidou.mybatisplus.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author liugh123
 * @since 2020-04-10
 */
public interface IRoutingInfoService extends IService<RoutingInfo> {
	public int getRssidByOssidAndCountryCode(int ossid, String countrySN);
	public List<Integer> getRssidsByOssidAndCountryCode(int ossid, String countrySN);
}

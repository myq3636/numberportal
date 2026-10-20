package com.liugh.service.impl;

import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import com.liugh.entity.RoutingInfo;
import com.liugh.mapper.RoutingInfoMapper;
import com.liugh.service.IRoutingInfoService;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author liugh123
 * @since 2020-04-10
 */
@Service
@Slf4j
public class RoutingInfoServiceImpl extends ServiceImpl<RoutingInfoMapper, RoutingInfo> implements IRoutingInfoService {

	
	
	@Autowired
	private RoutingInfoMapper routingInfoMapper;


	@Override
	public int getRssidByOssidAndCountryCode(int ossid, String countrySN) {
		RoutingInfo info = this.selectOne(new EntityWrapper<RoutingInfo>()
				.eq("ossid", ossid).eq("country_code", countrySN));
		if (info!=null && info.getRssid()>0) {
			return info.getRssid();
		}
		return 0;
	}


	@Override
	public List<Integer> getRssidsByOssidAndCountryCode(int ossid,
			String countrySN) {
		List<RoutingInfo> infos = this.selectList(new EntityWrapper<RoutingInfo>()
				.eq("ossid", ossid).eq("country_code", countrySN));
		List<Integer> rssids = new ArrayList<Integer>();
		if (infos!=null && !infos.isEmpty()) {
			for(RoutingInfo info: infos){
				rssids.add(info.getRssid());
			}
		}
		return rssids;
	}
	
	
	
	
}

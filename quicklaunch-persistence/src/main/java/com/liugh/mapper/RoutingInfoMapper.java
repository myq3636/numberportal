package com.liugh.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.liugh.entity.RoutingInfo;
import com.baomidou.mybatisplus.mapper.BaseMapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author liugh123
 * @since 2020-04-10
 */
public interface RoutingInfoMapper extends BaseMapper<RoutingInfo> {
	
	public List<Map<String, String>> groupByCountryCode(@Param("ossid")int ossid, @Param("type")int type);

}

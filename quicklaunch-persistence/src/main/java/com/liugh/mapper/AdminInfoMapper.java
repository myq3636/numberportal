package com.liugh.mapper;

import java.util.List;

import com.liugh.entity.AdminInfo;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import com.baomidou.mybatisplus.plugins.Page;

/**
 * <p>
 * 用户表 Mapper 接口
 * </p>
 *
 * @author liugh123
 * @since 2019-01-04
 */
public interface AdminInfoMapper extends BaseMapper<AdminInfo> {

	public List<AdminInfo> selectAdminList(Page<AdminInfo> page);
}

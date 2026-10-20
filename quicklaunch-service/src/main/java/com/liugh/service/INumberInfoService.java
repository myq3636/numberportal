package com.liugh.service;

import com.liugh.entity.AdminInfo;
import com.liugh.entity.NumberInfo;

import scala.collection.generic.BitOperations.Int;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.IService;

/**
 * <p>
 * 用户表 服务类
 * </p>
 *
 * @author liugh123
 * @since 2019-01-04
 */
public interface INumberInfoService extends IService<NumberInfo> {
	
	boolean saveNumbers(AdminInfo admin, MultipartFile file, int customerId, String msisdn, String dateTime) throws Exception;
	public Page<NumberInfo> selectPageBySsid(Page<NumberInfo> page, AdminInfo admin, String number);
}

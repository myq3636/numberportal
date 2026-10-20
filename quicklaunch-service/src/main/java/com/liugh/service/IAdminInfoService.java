package com.liugh.service;

import com.liugh.base.ChargeResponse;
import com.liugh.entity.AdminInfo;

import java.util.Map;


import com.alibaba.fastjson.JSONObject;
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
public interface IAdminInfoService extends IService<AdminInfo> {

	Map<String, Object> register(JSONObject requestJson) throws Exception;

	Map<String, Object> checkAccountAndPasswd(JSONObject requestJson) throws Exception;
	
	public Map<String, Object> logout(AdminInfo admin) throws Exception;
	
	void forgetPassword(JSONObject requestJson) throws Exception;
	
	public Map<String, Object> showAdminList(AdminInfo admin) throws Exception;
	
	public Map<String, Object> showAdminInfo(AdminInfo admin) throws Exception;
	
	public Map<String, Object> save(AdminInfo admin, JSONObject requestJson) throws Exception;
	
	public boolean credit(AdminInfo admin, JSONObject requestJson) throws Exception;

	void confirm(JSONObject requestJson) throws Exception;

	void delete(JSONObject requestJson) throws Exception;

	
	Page<AdminInfo> selectPageWithCondition(Page<AdminInfo> page, AdminInfo admin) throws Exception;

	Page<ChargeResponse> selectChargeRecord(Page<ChargeResponse> page, AdminInfo admin) throws Exception;
	
	void loadAdminToSystem() throws Exception;

}

package com.liugh.service.impl;

import com.liugh.base.BusinessException;
import com.liugh.base.ChargeResponse;
import com.liugh.base.Charges;
import com.liugh.base.PublicResultConstant;
import com.liugh.entity.AdminInfo;
import com.liugh.mapper.AdminInfoMapper;
import com.liugh.service.IAdminInfoService;
import com.liugh.service.IRoutingInfoService;
import com.liugh.service.entity.AdminResponse;
import com.liugh.service.util.GlobalMap;
import com.liugh.service.util.ThirdPartyLoginHelper;
import com.liugh.util.ComUtil;
import com.liugh.util.FileUtil;
import com.liugh.util.JWTUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.liugh.entity.RoutingInfo;
import com.liugh.rcs.service.RcsRoutingSnapshot;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author liugh123
 * @since 2019-01-04
 */
@Service
public class AdminInfoServiceImpl extends ServiceImpl<AdminInfoMapper, AdminInfo> implements IAdminInfoService {
	@Value("${project.image.path}")
	private String imagePath;
	@Value("${project.url}")
	private String projectURL;
			
	@Value("${billing.system.url}")
	private String billingURl;
	
	@Autowired
	private AdminInfoMapper adminMapper;
	
	@Autowired
	private IRoutingInfoService routing;
	
	
	@Override
	public Map<String, Object> register(JSONObject requestJson) throws Exception{
		String username = requestJson.getString("username");
		String password = requestJson.getString("password");
		String nickname = requestJson.getString("nickname");
		String imgBase64 = requestJson.getString("imgBase64");
		String descrption = requestJson.getString("descrption");
		String phoneNumber = requestJson.getString("phoneNumber");
		AdminInfo info = null;
		if (ComUtil.isEmpty(username)) {
			throw new BusinessException(PublicResultConstant.PARAM_ERROR);
		}else {
			info = this.selectOne(new EntityWrapper<AdminInfo>().where("username = {0}",username));
			if (null != info) {
	            throw new BusinessException(PublicResultConstant.EXISTED_USERNAME_PASSWORD);
	        }
			// 检查昵称
			if (null == info){
				info = this.selectOne(new EntityWrapper<AdminInfo>().where("nickname = {0}",nickname));
				if (null != info) {
		            throw new BusinessException(PublicResultConstant.EXISTED_NICKNAME_PASSWORD);
		        }
			}
		}
        
		// get image url
		String imgUrlFilename = username + ".jpg";
    	info = new AdminInfo();
    	info.setUsername(username);
    	info.setPassword(password);
    	info.setNickname(nickname);
    	info.setImgBase64(imgUrlFilename);
    	info.setDescrption(descrption);
    	info.setPhoneNumber(phoneNumber);
    	info.setStatus(0);
    	info.setRole(0);
    	//TODO call billing system for ssid
    	int ssid = ThirdPartyLoginHelper.rigster2BillingSystem(nickname, username, billingURl+"/customer/add");
    	if (ssid==0) {
			throw new BusinessException(PublicResultConstant.USER_RIGESTER_FAILED);
		}
    	info.setSsid(ssid);
		this.insert(info);
		
		// store admin header image
		File folder = new File(imagePath);
		if (!folder.exists()){
			folder.mkdirs();
		}
		String imageFilePath = imagePath + imgUrlFilename;
		FileUtil.generateImage(imgBase64, imageFilePath);
		
		Map<String, Object> result = new HashMap<>();
        return result;
	}
	
	@Override
	public Map<String, Object> checkAccountAndPasswd(JSONObject requestJson) throws Exception{
		String username = requestJson.getString("username");
		String pwd = requestJson.getString("password");
		AdminInfo info = null;
		Map<String, Object> result = new HashMap<>();
		if (ComUtil.isEmpty(username)) {
			throw new BusinessException(PublicResultConstant.PARAM_ERROR);
		}else {
			info = this.selectOne(new EntityWrapper<AdminInfo>().where("username = {0}",username));
			// 比较密码
			if (ComUtil.isNull(info) || !info.getPassword().equals(pwd)) {
	            throw new BusinessException(PublicResultConstant.INVALID_USERNAME_PASSWORD);
	        }
			if (0 == info.getRole() && 0 == info.getStatus()){
				// 返回空结果
				return result;
			}
		}
		
		// 返回admininfo response
		if (null != info){
			AdminResponse res = new AdminResponse();
			res.setId(info.getId());
			res.setUsername(info.getUsername());
			res.setNickname(info.getNickname());
			// 设置图片url
			String newImageURL = projectURL + "images/" + info.getImgBase64();
			res.setImgBase64(newImageURL);
			res.setDescrption(info.getCredit());
			res.setPhoneNumber(info.getPhoneNumber());
			res.setStatus(info.getStatus());
			result.put("adminInfo", res);
		}
        result.put("token", JWTUtil.sign(info.getId()+"", info.getUsername()));
        result.put("adminInfo", info);
        return result;
	}
	
	@Override
	public Map<String, Object> logout(AdminInfo admin) throws Exception{
		String username = admin.getUsername();
		AdminInfo info = null;
		if (ComUtil.isEmpty(username)) {
			throw new BusinessException(PublicResultConstant.UNAUTHORIZED);
		}else {
			info = this.selectOne(new EntityWrapper<AdminInfo>().where("username = {0}",username));
			if (null == info){
				// 返回空结果
				throw new BusinessException(PublicResultConstant.INVALID_USER);
			}
		}
		
		Map<String, Object> result = new HashMap<>();
        return result;		
	}
	
	@Override
	public void forgetPassword(JSONObject requestJson) throws Exception {
		
	}
	
	@Override
	public Map<String, Object> showAdminList(AdminInfo admin) throws Exception{
		if (null == admin){
			// 返回空结果
			throw new BusinessException(PublicResultConstant.INVALID_USER);
		}
		Map<String, Object> result = new HashMap<>();
		if (admin.getRole() == 0){
			// 普通管理员没有权限查看其他管理员信息
			List<AdminResponse> userList = new ArrayList<AdminResponse>();
			result.put("adminInfo", userList);
	        return result;
		}else{
			// 返回所有普通管理员的信息
			List<AdminInfo> userList = new ArrayList<AdminInfo>();
			userList = this.selectList(new EntityWrapper<AdminInfo>());
			List<AdminResponse> responseList = new ArrayList<AdminResponse>();
			if (null != userList && !userList.isEmpty()){
				for(AdminInfo user : userList){
					if (0 == user.getRole()){
						// 返回普通管理员列表
						AdminResponse res = new AdminResponse();
						res.setId(user.getId());
						res.setUsername(user.getUsername());
						res.setNickname(user.getNickname());
						res.setPassword(user.getPassword());
						// 设置图片url
						String newImageURL = projectURL + "images/" + user.getImgBase64();
						res.setImgBase64(newImageURL);
						res.setDescrption(user.getDescrption());
						res.setPhoneNumber(user.getPhoneNumber());
						res.setStatus(user.getStatus());
						if (1==user.getSource()) {
							res.setCredit(ThirdPartyLoginHelper.queryCredit(user.getSsid(), billingURl));
						}else {
							res.setCredit(user.getCredit());
						}
						responseList.add(res);
					}
				}
			}
			result.put("adminInfo", responseList);
			return result;
		}
	}
	
	@Override
	public Map<String, Object> showAdminInfo(AdminInfo admin) throws Exception{
		if (null == admin){
			// 返回空结果
			throw new BusinessException(PublicResultConstant.INVALID_USER);
		}
		Map<String, Object> result = new HashMap<>();
		AdminInfo user = this.selectOne(new EntityWrapper<AdminInfo>().where("username = {0}", admin.getUsername()));
		if (null != user){
			AdminResponse res = new AdminResponse();
			res.setId(user.getId());
			res.setUsername(user.getUsername());
			res.setNickname(user.getNickname());
			// 设置图片url
			String newImageURL = projectURL + "images/" + user.getImgBase64();
			res.setImgBase64(newImageURL);
			//for gateway account query credit from billing, 
			//for portal account query credit from db
			if (1==user.getSource()) {
				res.setDescrption(ThirdPartyLoginHelper.queryCredit(user.getSsid(), billingURl));
			}else {
				res.setDescrption(user.getCredit());
			}
			
			res.setPhoneNumber(user.getPhoneNumber());
			res.setStatus(user.getStatus());
			result.put("adminInfo", res);
		}
		return result;
	}
	
	@Override
	public Map<String, Object> save(AdminInfo admin, JSONObject requestJson) throws Exception{
		if (null == admin){
			// 返回空结果
			throw new BusinessException(PublicResultConstant.INVALID_USER);
		}
		AdminInfo user = this.selectOne(new EntityWrapper<AdminInfo>().where("username = {0}", admin.getUsername()));
		Map<String, Object> result = new HashMap<>();
		if (null != user){
			String imgFilename = admin.getUsername() + ".jpg";
			user.setNickname(requestJson.getString("nickname"));
			user.setImgBase64(imgFilename);
			user.setPhoneNumber(requestJson.getString("phoneNumber"));
			this.insertOrUpdate(user);
			
			// 更新头像图片
			File folder = new File(imagePath);
			if (!folder.exists()){
				folder.mkdirs();
			}
			String imageFilePath = imagePath + imgFilename;
			FileUtil.generateImage(requestJson.getString("imgBase64"), imageFilePath);
			
			AdminResponse res = new AdminResponse();
			res.setId(user.getId());
			res.setUsername(user.getUsername());
			res.setNickname(user.getNickname());
			// 设置图片url
			String newImageURL = projectURL + "images/" + user.getImgBase64();
			res.setImgBase64(newImageURL);
			res.setDescrption(user.getDescrption());
			res.setPhoneNumber(user.getPhoneNumber());
			res.setStatus(user.getStatus());
			result.put("adminInfo", res);
		}
        return result;	
	}
	
	@Override
	public void confirm(JSONObject requestJson) throws Exception{
		String userName = requestJson.getString("username");
		AdminInfo user = this.selectOne(new EntityWrapper<AdminInfo>().where("username = {0}", userName));
		if (null != user){
			user.setStatus(1);
			//TODO add ssid account for user.
			this.insertOrUpdate(user);
		}else{
			throw new BusinessException(PublicResultConstant.INVALID_USER);
		}
	}
	
	@Override
	public void delete(JSONObject requestJson) throws Exception{
		String userName = requestJson.getString("username");
		this.delete(new EntityWrapper<AdminInfo>().where("username = {0}", userName));
	}
	
	

	@Override
	public boolean credit(AdminInfo admin, JSONObject requestJson)
			throws Exception {
		String userName = requestJson.getString("username");
		String credit = requestJson.getString("credit");
		AdminInfo user = this.selectOne(new EntityWrapper<AdminInfo>().where("username = {0}", userName));
		if (admin.getStatus()!=1) {
			throw new BusinessException(PublicResultConstant.USER_NO_PERMITION);
		}
		if (null != user && user.getSsid() !=0){			
			//TODO add ssid account for user.
			if(ThirdPartyLoginHelper.addQuota(user.getSsid(), billingURl+"/customer/attribute/quota/add", credit)){
				BigDecimal bcredit = new BigDecimal(user.getCredit());
				BigDecimal bprepaid = new BigDecimal(credit);			
				BigDecimal balance = bcredit.add(bprepaid);
				user.setCredit(""+balance);
				this.updateById(user);
			}else {
				throw new BusinessException(PublicResultConstant.TOPUP_FAILED);
			}			
		}else{
			throw new BusinessException(PublicResultConstant.INVALID_USER);
		}
		return false;
	}
	
	@Override
	public Page<AdminInfo> selectPageWithCondition(Page<AdminInfo> page, AdminInfo admin) throws Exception {
		if (null == admin){
			// 返回空结果
			throw new BusinessException(PublicResultConstant.INVALID_USER);
		}
		if (admin.getRole() == 0){
			// 普通管理员没有权限查看其他管理员信息
			throw new BusinessException(PublicResultConstant.USER_NO_PERMITION);
		}
		// 注意！！ 分页 total 是经过插件自动 回写 到传入 page 对象
		List<AdminInfo> adminInfos = selectAdminInfoList(page, admin);
		return page.setRecords(adminInfos);
	}
	
	private List<AdminInfo> selectAdminInfoList(Page<AdminInfo> page, AdminInfo admin) {
		// 返回所有普通管理员的信息
		List<AdminInfo> userList = adminMapper.selectAdminList(page);
		if (null != userList && !userList.isEmpty()){
			for(AdminInfo adminInfo : userList){
				// 返回普通管理员列表
				// 设置图片url
				String newImageURL = projectURL+"/" + adminInfo.getImgBase64();	
				adminInfo.setImgBase64(newImageURL);
				if (1==adminInfo.getSource()) {
					adminInfo.setCredit(ThirdPartyLoginHelper.queryCredit(adminInfo.getSsid(), billingURl));
				}
			}
		}
		return userList;
	}

	@Override
	public Page<ChargeResponse> selectChargeRecord(Page<ChargeResponse> page,
			AdminInfo admin) throws Exception {
		Charges data = ThirdPartyLoginHelper.queryCharge(admin.getSsid(), billingURl+"quota/query/recharge_record", page.getCurrent(), page.getSize());
		if (data == null || data.getRecords() == null || data.getRecords().size() ==0) {
			return page;
		}
		page.setRecords(data.getRecords());
		page.setTotal(data.getTotal());
		return page;
	}

	@Override
	public void loadAdminToSystem() throws Exception {
		List<AdminInfo> list = this.selectList(new EntityWrapper<AdminInfo>());
		for (AdminInfo adminInfo : list) {
			GlobalMap.admin.put(""+adminInfo.getId(), adminInfo);
			GlobalMap.rcsadmin.put(""+adminInfo.getSsid(), adminInfo);
			if(adminInfo.getToken()!=null) {
				GlobalMap.admin.put(adminInfo.getToken(), adminInfo);
			}			
		}
		
		List<RoutingInfo> routings = routing.selectList(new EntityWrapper<RoutingInfo>());
		for(RoutingInfo info: routings) {
			GlobalMap.routingTable.put(info.getOssid(), info.getCountryCode(), info.getRssid());
		}

		// RCS 专用权重路由：只保留 RCS 供应商（channel_code 非空），SMS 仍走 routingTable。
		GlobalMap.rcsRoutingSnapshot = RcsRoutingSnapshot.build(routings, new java.util.function.Predicate<Integer>() {
			@Override
			public boolean test(Integer rssid) {
				AdminInfo a = GlobalMap.rcsadmin.get(String.valueOf(rssid));
				return a != null && a.getChannelCode() != null && !a.getChannelCode().trim().isEmpty()
						&& (a.getStatus() == null || a.getStatus() == 0);
			}
		});
		
	}
}

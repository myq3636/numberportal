package com.liugh.service.impl;

import com.liugh.base.BusinessException;
import com.liugh.base.ChargeResponse;
import com.liugh.base.Charges;
import com.liugh.base.PublicResultConstant;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.NumberInfo;
import com.liugh.mapper.AdminInfoMapper;
import com.liugh.mapper.NumberInfoMapper;
import com.liugh.service.IAdminInfoService;
import com.liugh.service.INumberInfoService;
import com.liugh.service.entity.AdminResponse;
import com.liugh.service.util.ThirdPartyLoginHelper;
import com.liugh.util.ComUtil;
import com.liugh.util.FileUtil;
import com.liugh.util.JWTUtil;
import com.liugh.util.QuotaManagerUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 用户表 服务实现类
 * </p>
 *
 * @author liugh123
 * @since 2019-01-04
 */
@Service
public class NumberInfoServiceImpl extends ServiceImpl<NumberInfoMapper, NumberInfo> implements INumberInfoService {

	@Value("${sms.file.path}")
	private String filePath;
	
	@Override
	public boolean saveNumbers(AdminInfo admin, MultipartFile file, int customerId, String msisdn,
			String dateTime) throws Exception {
         int ssid = admin.getSsid();				
		
		
		if (0==admin.getRole()) {
			throw new BusinessException("此用户权限不足");
		}
		
		
		if (StringUtils.isBlank(msisdn) && file.isEmpty()) {
			throw new BusinessException("号码和号码文件不能同时为空");
		}
		
		if (StringUtils.isNotBlank(msisdn) && !file.isEmpty()) {
			throw new BusinessException("号码和号码文件不能同时有值");
		}
		
		if (StringUtils.isNotBlank(msisdn) && msisdn.contains(",")) {
			throw new BusinessException("号码必须以;分割");
		}
		
		if (!file.isEmpty()) {
			File folder = new File(filePath);
			if (!folder.exists()){
				folder.mkdirs();
			}
			String packagePath = filePath +"/upload/"+file.getOriginalFilename()+".tmp";
			if(!ComUtil.isNull(file) && !ComUtil.isEmpty(file)) {
	            File outFile = new File(packagePath);
	            //拷贝文件到输出文件对象
	            FileUtils.copyInputStreamToFile(file.getInputStream(), outFile);
	            List<String> records = FileUtils.readLines(outFile, "UTF-8");
	            File uploadFile = new File(filePath +"/upload/"+file.getOriginalFilename()+"."+System.currentTimeMillis());
	            FileUtils.copyFile(outFile, uploadFile);
	            FileUtils.deleteQuietly(outFile);
	            
	            if (records != null && !records.isEmpty()) {
	            	List<NumberInfo> infos = new ArrayList<NumberInfo>();
					for (String number : records) {
						NumberInfo numberInfo = new NumberInfo();
						numberInfo.setCustomerId(customerId);
						numberInfo.setCreateTime(System.currentTimeMillis());
						numberInfo.setNumber(number.trim());
						numberInfo.setExpireTime(dateTime);
						numberInfo.setExpireTimemark(QuotaManagerUtil.getMsFromFormatTimeWithTimeZone(dateTime, "yyyy-MM-dd HH:mm:ss", "GMT+8"));
					    infos.add(numberInfo);
					}
					insertBatch(infos);
				}
	        }
			
		}else {
			if (msisdn !=null) {
				String[] msisdns = msisdn.split(";");
				List<NumberInfo> infos = new ArrayList<NumberInfo>();
				for (int i = 0; i < msisdns.length; i++) {					
					NumberInfo numberInfo = new NumberInfo();
					numberInfo.setCustomerId(customerId);
					numberInfo.setCreateTime(System.currentTimeMillis());
					numberInfo.setNumber(msisdns[i].trim());
					numberInfo.setExpireTime(dateTime);
					numberInfo.setExpireTimemark(QuotaManagerUtil.getMsFromFormatTimeWithTimeZone(dateTime, "yyyy-MM-dd HH:mm:ss", "GMT+8"));
				    infos.add(numberInfo);					
				}
				insertBatch(infos);
			}
		}
		return true;
	}

	@Override
	public Page<NumberInfo> selectPageBySsid(Page<NumberInfo> page, AdminInfo admininfo, String number) {
		if (StringUtils.isNotBlank(number)) {
			return this.selectPage(page, new EntityWrapper<NumberInfo>().eq("customer_id", admininfo.getId()).eq("number", number).eq("status", 0));
		}
		return this.selectPage(page, new EntityWrapper<NumberInfo>().eq("customer_id", admininfo.getId()).eq("status", 0));
	}
	
}

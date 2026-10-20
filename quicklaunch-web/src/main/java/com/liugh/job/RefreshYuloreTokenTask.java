package com.liugh.job;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.service.IAdminInfoService;
import com.liugh.service.ISmsCdrInfoService;
import com.liugh.service.impl.AsyncService;
import com.liugh.service.util.GlobalMap;
import com.liugh.service.util.ThirdPartyLoginHelper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RefreshYuloreTokenTask {
		       	
    @Scheduled(fixedDelay=1000*60*110)     
    public void refreshToken(){ 
    	if (!GlobalMap.admin.isEmpty()) {
			for (AdminInfo adminInfo : GlobalMap.admin.values()) {
				String appid=adminInfo.getAppId();
				String appsecuret=adminInfo.getAppSecurity();
				String url=adminInfo.getUrl();
				if (StringUtils.isNotBlank(appid)
						&&StringUtils.isNotBlank(appsecuret)
						&&StringUtils.isNotBlank(adminInfo.getNumberCheckUrl())
						&&StringUtils.isNotBlank(url)) {
					doRefreshToken(adminInfo);
				}
			}
		}                          
    }    	
    
    

	private void doRefreshToken(AdminInfo adminInfo) {
		try {
        	long time = System.currentTimeMillis()/1000;  
        	List<String> parameters = new ArrayList<String>();
        	parameters.add(String.valueOf(time));
        	parameters.add(adminInfo.getAppId());
        	parameters.add(adminInfo.getAppSecurity());
        	parameters.add("4999");
        	Collections.sort(parameters);
        	String sign = "";
        	for (String para : parameters) {
				sign = sign+para;
			}
        	sign = DigestUtils.sha1Hex(sign);
        	Map<String, Object> request = new HashMap<>();
        	request.put("appId", adminInfo.getAppId());
        	request.put("timeStamp", time);
        	request.put("nonce", 4999);
        	request.put("sign", sign);
        	String token = ThirdPartyLoginHelper.refreshYuloreToken(request, adminInfo.getUrl());  
        	if (StringUtils.isNotBlank(token)) {
				adminInfo.setToken(token);
			}
		} catch (Exception e) {
			log.error("do refresh yulore token error", e);
		}
	}	
    
    /**
     * @Description: long类型生成没有符号的日期格式
     *
     * @param lo 日期毫秒数
     * @return String yyyyMMddHHmmss
     */
    public static String getLongToDate(long lo){
        Date date = new Date(lo);
        SimpleDateFormat sd = new SimpleDateFormat("yyyyMM");
        return sd.format(date);
    }
    
    public static void main(String[] args) {
		String sh = "azbycxdw";
		System.out.println(DigestUtils.sha1Hex(sh));
	}

}

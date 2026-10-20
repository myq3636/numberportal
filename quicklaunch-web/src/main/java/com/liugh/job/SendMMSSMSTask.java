package com.liugh.job;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.service.IAdminInfoService;
import com.liugh.service.ISmsCdrInfoService;
import com.liugh.service.impl.AsyncService;
import com.liugh.service.impl.RedisService;
import com.liugh.service.util.GlobalMap;
import com.liugh.service.util.ThirdPartyLoginHelper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SendMMSSMSTask {

	
	@Autowired
	ISmsCdrInfoService smsCdrService;	
	
	
	@Value("${mms.msg.send.interval:5000}")
	private String sleepTimeInMs;
	
	@Value("${mms.msg.limit:300}")
	private String msgLimit;
	
	@Autowired
	private IAdminInfoService adminInfoService;
	
	@Autowired
	private AsyncService asyncService;
	
	@Autowired
	private RedisService redisService;
	
	
    @Scheduled(cron="0/2 * * * * ?")     
    public void sendWaSMSJob(){   	        
    	doSendMMSSMSJob();                  
    }    	
    
    

	private void doSendMMSSMSJob() {
		try {
			
			/*List<String> msgs = redisService.getList("simMsg");	
        	if (msgs!=null && !msgs.isEmpty()) {        		
        		for (String msg : msgs) {
        			SmsCdrInfo cdr = JSONObject.parseObject(msg, SmsCdrInfo.class);
        			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(cdr.getRssid()));          			        			
        			if(rCustomer == null){
        				continue;
        			} 
        			long sleepInterval = rCustomer.getInterval();
        			String ports = rCustomer.getPorts();
        			List<String> portList = null;
        			if(StringUtils.isNotBlank(ports)){
        				portList = Arrays.asList(ports.split(","));
        			}        			
        			if(sleepInterval == 0){
        				if(portList!=null){
        					sleepInterval = 1000/portList.size();
        				}else{
        					sleepInterval = 1000;
        				}        				
        			}
        			Thread.sleep(sleepInterval);
        			asyncService.sendSMS2SimBank(cdr,rCustomer);
        		
				}        		
			}*/
			
			while(true){
				String msg = redisService.rightPop("mmsMsg");
				if(StringUtils.isEmpty(msg)){
					continue;
				}
				SmsCdrInfo cdr = JSONObject.parseObject(msg, SmsCdrInfo.class);
    			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(cdr.getRssid()));          			        			
    			if(rCustomer == null){
    				continue;
    			} 
    			long sleepInterval = rCustomer.getInterval();
    			String ports = rCustomer.getPorts();
    			List<String> portList = null;
    			if(StringUtils.isNotBlank(ports)){
    				portList = Arrays.asList(ports.split(","));
    			}        			
    			if(sleepInterval == 0){
    				if(portList!=null){
    					sleepInterval = 1000/portList.size();
    				}else{
    					sleepInterval = 1;
    				}        				
    			}
    			Thread.sleep(sleepInterval);
    			asyncService.sendMmsToMK(cdr, rCustomer);				
			}
        	
        	
		} catch (Exception e) {
			log.error("do send mms job error", e);
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

}

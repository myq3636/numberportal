package com.liugh.job;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
public class SendWaSMSTask {

	
	@Autowired
	ISmsCdrInfoService smsCdrService;	
	
	
	@Value("${wa.msg.send.interval:5000}")
	private String sleepTimeInMs;
	
	@Value("${wa.msg.limit:300}")
	private String msgLimit;
	
	@Autowired
	private IAdminInfoService adminInfoService;
	
	@Autowired
	private AsyncService asyncService;
	
	
    //@Scheduled(cron="0/5 * * * * ?")     
    public void sendWaSMSJob(){   	
        
        doSendWaSMSJob();  
                
    }    	
    
    

	private void doSendWaSMSJob() {
		try {
        	
        	List<Long> clientList = new ArrayList();
			clientList.addAll(GlobalMap.waClients);
			Collections.sort(clientList);
			
        	
        	//success dr
        	List<SmsCdrInfo> cdrs = smsCdrService.selectList(new EntityWrapper<SmsCdrInfo>().eq("status", -1).last(" limit 10"));
        	if (cdrs!=null && !cdrs.isEmpty()) {
        		for (SmsCdrInfo smsCdrInfo : cdrs) {
        			AdminInfo adminInfo = null;   
        			if(clientList.isEmpty()){
        				continue;
        			}
        			if(GlobalMap.lastSendClientIndex>=clientList.size()){
        				GlobalMap.lastSendClientIndex =0;
        				adminInfo = adminInfoService.selectById(clientList.get(0));
        			}else{
        				adminInfo = adminInfoService.selectById(clientList.get(GlobalMap.lastSendClientIndex));
        				GlobalMap.lastSendClientIndex++;
        			}
        			/*for(Long client : clientList){
        				if(client>GlobalMap.lastSendClient){
        					if(GlobalMap.clientCount.get(client)==null || GlobalMap.clientCount.get(client)<Integer.parseInt(msgLimit)){
        						adminInfo = adminInfoService.selectById(client);
        						break;
        					}
        				}								
        			}
        			if(adminInfo == null){
        				if(GlobalMap.lastSendClient == 0 ||(!clientList.isEmpty() && clientList.get(clientList.size()-1) <= GlobalMap.lastSendClient) 
        						|| clientList.size() ==1){
        					if(GlobalMap.clientCount.get(clientList.get(0))==null || GlobalMap.clientCount.get(clientList.get(0))<Integer.parseInt(msgLimit)){
        						adminInfo = adminInfoService.selectById(clientList.get(0));						
        					}
        				}
        			}*/
        			
        			if(adminInfo == null){
        				continue;
        			}
        			log.info("get the adminInfo: {}, last client: {}", adminInfo, GlobalMap.lastSendClient);
        			try {					
        				smsCdrInfo.setSsid(adminInfo.getSsid());				
    					asyncService.sendWaMessage(smsCdrInfo, adminInfo);
    					Thread.sleep(Long.parseLong(sleepTimeInMs));
    				} catch (Exception e) {					
    					log.error("send msg to wa failed, {}", smsCdrInfo, e);
    				}
        			
        			GlobalMap.lastSendClient = adminInfo.getId();
        			Long count = GlobalMap.clientCount.get(""+adminInfo.getId());
        			if(count == null){
        				GlobalMap.clientCount.put(""+adminInfo.getId(), 1L);
        			}else{
        				GlobalMap.clientCount.put(""+adminInfo.getId(), count+1);
        			}
        		
				}        		
			}
        	
        	
		} catch (Exception e) {
			log.error("do send wa sms job error", e);
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

package com.liugh.job;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.service.ISmsCdrInfoService;
import com.liugh.service.util.GlobalMap;
import com.liugh.service.util.ThirdPartyLoginHelper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SendWaSMSDRTask {

	
	@Autowired
	ISmsCdrInfoService smsCdrService;	
	
	
	@Value("${wa.message.drTime:5000}")
	private String expireTimeInMs;
	
    //@Scheduled(cron="0/5 * * * * ?")     
    public void sendWaSMSDRJob(){   	
        
        doSendWaSMSDRJob();  
                
    }    	
    
    

	private void doSendWaSMSDRJob() {
		try {        	 
        	//success dr
        	List<SmsCdrInfo> cdrs = smsCdrService.selectList(new EntityWrapper<SmsCdrInfo>().eq("status", 0).last(" limit 100"));
        	if (cdrs!=null && !cdrs.isEmpty()) {
        		for (SmsCdrInfo smsCdrInfo : cdrs) {
        			Thread.sleep(10);
        			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
        			if (adminInfo!=null) {
        				if (ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), smsCdrInfo.getMsgId(), 3)) {
							smsCdrInfo.setStatus(3);
							smsCdrService.updateById(smsCdrInfo);
						} 				
					}else {
						smsCdrInfo.setStatus(6);
						smsCdrService.updateById(smsCdrInfo);
						log.error("not found admin info by cdr when send wa msg dr job, sms: [{}]", smsCdrInfo);
					}
        			
				}        		
			}
        	
        	//failed dr
        	List<SmsCdrInfo> failcdrs = smsCdrService.selectList(new EntityWrapper<SmsCdrInfo>().eq("status", 1).last(" limit 100"));
        	if (failcdrs!=null && !failcdrs.isEmpty()) {
        		for (SmsCdrInfo smsCdrInfo : failcdrs) {
        			Thread.sleep(1);
        			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
        			if (adminInfo!=null) {
        				if (ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), smsCdrInfo.getMsgId(), 4)) {
							smsCdrInfo.setStatus(4);
							smsCdrService.updateById(smsCdrInfo);
						} 				
					}else {
						smsCdrInfo.setStatus(6);
						smsCdrService.updateById(smsCdrInfo);
						log.error("not found admin info by cdr when send wa msg dr job, sms: [{}]", smsCdrInfo);
					}
        			
				}        		
			}
		} catch (Exception e) {
			log.error("do send wa sms dr job error", e);
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

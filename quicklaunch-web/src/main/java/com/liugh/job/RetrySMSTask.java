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
import com.liugh.service.IAdminInfoService;
import com.liugh.service.ISmsCdrInfoService;
import com.liugh.service.impl.AsyncService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RetrySMSTask {

	
	@Autowired
	ISmsCdrInfoService smsCdrService;	
	
	@Autowired
	private AsyncService asyncService;
	
	@Autowired
	private IAdminInfoService adminInfoService;
	
	@Value("${sms.max.retry.count:3}")
	private String count;
	
    //@Scheduled(cron="0 0/1 * * * ?")     
    public void retrySMSJob(){   	
        
        doRetrySMSJob();  
                
    }    	
    
    

	private void doRetrySMSJob() {
		try {
        	long time = System.currentTimeMillis()-5*60*1000;        	
        	List<SmsCdrInfo> cdrs = smsCdrService.selectList(new EntityWrapper<SmsCdrInfo>().eq("status", 1).le("create_time", time).le("retry_count", count).last(" limit 100"));
        	if (cdrs!=null && !cdrs.isEmpty()) {
        		for (SmsCdrInfo smsCdrInfo : cdrs) {
        			Thread.sleep(10);
        			AdminInfo adminInfo = adminInfoService.selectById(smsCdrInfo.getSsid());    						
    				asyncService.sendMessage(smsCdrInfo, adminInfo.getUrl(), adminInfo.getToken());
				}        		
			}
		} catch (Exception e) {
			log.error("do retry sms job error", e);
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

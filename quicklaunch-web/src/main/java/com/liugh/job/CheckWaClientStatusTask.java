package com.liugh.job;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.service.IAdminInfoService;
import com.liugh.service.util.GlobalMap;
import com.liugh.service.util.ThirdPartyLoginHelper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CheckWaClientStatusTask {

	
	@Autowired
	IAdminInfoService adminInfoService;	
	
	
	@Value("${yulore.message.expireTime:5000}")
	private String expireTimeInMs;
	
    @Scheduled(cron="0 0/2 * * * ?")     
    public void checkWaClientStatusJob(){   	
        
        doCheckWaClientStatusJob();  
                
    }    	
    
    

	private void doCheckWaClientStatusJob() {
		try {
			List<AdminInfo> list = adminInfoService.selectList(new EntityWrapper<AdminInfo>().eq("source", 3));
    		for (AdminInfo adminInfo : list) {
    			Thread.sleep(1000);  
    			if (adminInfo!=null && adminInfo.getSource() == 3) {
    				if (ThirdPartyLoginHelper.checkWaClientStatus(adminInfo.getUrl())) {
    					GlobalMap.waClients.add(adminInfo.getId());
					}else{
						GlobalMap.waClients.remove(adminInfo.getId());
						log.error("Wa account client connection is broken, please check. {}", adminInfo);
					}				
				}    			
			}        		
		} catch (Exception e) {
			log.error("doCheckWaClientStatusJob sms job error", e);
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

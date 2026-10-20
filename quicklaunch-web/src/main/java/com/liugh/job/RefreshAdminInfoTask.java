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
import com.liugh.service.util.GlobalMap;
import com.liugh.service.util.ThirdPartyLoginHelper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RefreshAdminInfoTask {

	
	 @Autowired
	 IAdminInfoService adminInfoService;	
		
	
    @Scheduled(cron="3 0/6 * * * ?")     
    public void refreshAdminInfoJob(){   	
        
    	try {
			adminInfoService.loadAdminToSystem();
		} catch (Exception e) {
			log.error("load admininfo to system throw error.", e);
		}
		
                
    }    	

}

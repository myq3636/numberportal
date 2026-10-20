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
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.mapper.RCSCdrInfoMapper;
import com.liugh.rcs.service.RcsMessagePersistenceService;
import com.liugh.rcs.service.RcsMessageService;
import com.liugh.service.ISmsCdrInfoService;
import com.liugh.service.util.GlobalMap;
import com.liugh.service.util.ThirdPartyLoginHelper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExpiredRCSDRTask {

	
	@Autowired
	RCSCdrInfoMapper cdrMapper;
	
	
	@Value("${yulore.message.expireTime:120000}")
	private String expireTimeInMs;
	
    @Scheduled(cron="0/5 * * * * ?")     
    public void expiredSMSDRJob(){   	
        
        doExpiredSMSDRJob();  
                
    }    	
    
    

	private void doExpiredSMSDRJob() {
		try {
        	long time = System.currentTimeMillis();         	
        	List<RCSCdrInfo> cdrs = cdrMapper.selectList(new EntityWrapper<RCSCdrInfo>().eq("status", 0).le("expire_time", time).last(" limit 100"));
        	if (cdrs!=null && !cdrs.isEmpty()) {
        		for (RCSCdrInfo smsCdrInfo : cdrs) {
        			Thread.sleep(1);
        			AdminInfo adminInfo = GlobalMap.rcsadmin.get(""+smsCdrInfo.getSsid());
        			if (adminInfo!=null) {
        				if (ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), smsCdrInfo.getMsgId(), 5)) {
							smsCdrInfo.setStatus(5);
							cdrMapper.updateById(smsCdrInfo);
						} 				
					}else {
						smsCdrInfo.setStatus(6);
						cdrMapper.updateById(smsCdrInfo);
						log.error("not found admin info by cdr when do expired sms job, sms: [{}]", smsCdrInfo);
					}
        			
				}        		
			}
		} catch (Exception e) {
			log.error("do expired sms job error", e);
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

package com.liugh.job;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.service.ISmsCdrInfoService;


import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class BackupSMSDRTask {

	@Autowired
	ISmsCdrInfoService smsCdrService;	
	
	
	@Value("${yulore.message.expireTime:120000}")
	private String expireTimeInMs;
	
    @Scheduled(cron="8 0/8 * * * ?")      
    public void backupSMSDRJob(){   	
    	log.info("backup cdr job start.");
        doBackupSMSDRJob();  
        log.info("backup cdr job end.");        
    }    	
    
    

	private void doBackupSMSDRJob() {				
		try {
			long time = System.currentTimeMillis()-Long.parseLong(expireTimeInMs)-3600*1000L;
        	String currentYYYYMM = getLongToDate(time);
        	String currTable = "sms_cdr_info_"+currentYYYYMM;
        	if (smsCdrService.existTable(currTable)==0) {
        		smsCdrService.createNewTable(currTable);
			}
        	boolean isCdr = true;
        	while (isCdr) {
        		        	
            	List<SmsCdrInfo> cdrs = smsCdrService.selectList(new EntityWrapper<SmsCdrInfo>().where("status !={0} and create_time<{1}", 0, time).last(" limit 100"));            	
				if (cdrs == null || cdrs.isEmpty()) {
					isCdr = false;
				}				
				backupSMSCdrs(currTable, cdrs);
			}        	       	
		} catch (Exception e) {
			log.error("backup sms_cdr_info error", e);
		}
		
	}	
	
	private void backupSMSCdrs(String historyTable, List<SmsCdrInfo> cdrs) {
		if (cdrs == null || cdrs.isEmpty()) {
			return;
		}
		for (SmsCdrInfo cdr : cdrs) {
			smsCdrService.insertCdr(historyTable, cdr);
			smsCdrService.deleteById(cdr);
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

package com.liugh.service.impl;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.freeswitch.esl.client.IEslEventListener;
import org.freeswitch.esl.client.inbound.Client;
import org.freeswitch.esl.client.transport.event.EslEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.service.ISmsCdrInfoService;
import com.liugh.service.util.GlobalMap;
import com.liugh.util.PhoneUtils;


public class PeeringTcpThread extends TTSClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeeringTcpThread.class);
    private String serverAddress;
    private int serverPort;
    private static Object object = new Object();   
    private Client  session;

    private String pwd;

    
	private RedisService redisService;
    
	private ISmsCdrInfoService smsCdrInfoService;
    
    private int ssid;
    
    public PeeringTcpThread(String serverIp, int port, String pwd, int ssid, RedisService redisService, ISmsCdrInfoService smsCdrInfoService) {
        init(serverIp, port, pwd, ssid, redisService, smsCdrInfoService);
    }
    
    private class TTSEventListener implements IEslEventListener {

        @Override
        public void eventReceived(EslEvent eslEvent) {
        	try {
        		String eventName = eslEvent.getEventName();
            	if (eventName.startsWith("CHANNEL")) {            		
                    if (eventName.startsWith("CHANNEL_HANGUP_COMPLETE")) {                                            	                        	
                    	String msgid = eslEvent.getEventHeaders().get("variable_variable_call_uuid");                    	
                    	String statustext = eslEvent.getEventHeaders().get("Hangup-Cause");
                    	LOGGER.info("receive the tts DR of {} status is {}", msgid, statustext);
                    	String status = "0";
                    	if("NORMAL_CLEARING".equalsIgnoreCase(statustext)) {
                    		status="3";
                    	}
                    	smsCdrInfoService.receiveDRFromTTS(msgid, status);
                    }
                }
			} catch (Exception e) {
				LOGGER.error("tts eventReceived error!", e);
			}
        	
        }

        @Override
        public void backgroundJobResultReceived(EslEvent event) {
            
        }
    }

    private void init(String serverAddress, int serverPort, String pwd, int ssid, RedisService redisService, ISmsCdrInfoService smsCdrInfoService) {
        try {
        	this.serverAddress = serverAddress;
        	this.serverPort = serverPort; 
        	this.pwd = pwd;
        	this.ssid = ssid;
        	this.redisService = redisService;
        	this.smsCdrInfoService = smsCdrInfoService;
        	session = new Client(2, 4);
        	session.connect(serverAddress, serverPort, pwd, 10);
        	session.setEventSubscriptions("plain", "all"); 
    		session.addEventListener(new TTSEventListener());
        } catch (Exception e) {
            LOGGER.error("PeeringTcpThread init error!", e);
        }
    }

    public void run() {
        LOGGER.info("TTSClient begins running.");
        this.keepRunning = true;
        while (this.keepRunning) {
            try { 
            	
            	//LOGGER.info("session tcp status is: {}", session.canSend());
                if (!session.canSend()) { 
                	try { //重连
                		session.close();
                		session.connect(serverAddress, serverPort, pwd, 10);
                		session.cancelEventSubscriptions();
                		session.setEventSubscriptions("plain", "all"); 
                		session.addEventListener(new TTSEventListener());
               		 } catch (Exception e) {
               			LOGGER.error("Exception occurred when session reconnect:", e); 
               		 } 
                		
                }else {
                	processTTSMessage();
                }                                          
                Thread.sleep(10);
            } catch (Exception exp) {
                LOGGER.error("Exception occurred when running:", exp);
            } catch (Error e) {            	
            	LOGGER.error("Error occurred when running:", e);
			}
        }
        this.stop();
    }

    /**
     * 
     * return: void
     */
    private void processTTSMessage()
            throws Exception {
        try {
            if (session.canSend()) { 
            	String msg = redisService.rightPop("ttsMsg_"+ssid);
				if(StringUtils.isEmpty(msg)){
					return;
				}
				
				SmsCdrInfo cdr = JSONObject.parseObject(msg, SmsCdrInfo.class);
				LOGGER.info("get message from buffer, message id is:{}"
	                    ,cdr.getMsgId());
    			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(cdr.getRssid()));          			        			
    			if(rCustomer == null){
    				return;
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
    			String msisdn = cdr.getMsisdn();
    			String countrySN = PhoneUtils.getRegionCodeByPhone(msisdn);
                session.sendAsyncApiCommand("originate", "{variable_call_uuid="
    			+cdr.getMsgId()
    			+",ignore_early_media=true}sofia/gateway/GW_TEST/"
                +cdr.getMsisdn().substring(2)
    			+" '&lua(TTS_ML_Aliyun.Lua "
                +countrySN
                +" \\'"+cdr.getContent()+")\\'' "
                +msisdn);
                //GlobalMap.ttsMsgCache.put("ttsMsg_"+cdr.getMsgId(), cdr);
            } else {
            	this.stop();
            }
            
        } catch (Exception e) {

            // here need retry::::
            LOGGER.error("PeeringTcpThread other error:", e);
        }
    }

    

	/*
	 * public void start() { this.start(); }
	 */

    public void stop() {
        this.keepRunning = false;
        session.close();
    }	
    
}

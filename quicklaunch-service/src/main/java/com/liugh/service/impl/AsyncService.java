package com.liugh.service.impl;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


















import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.google.common.util.concurrent.RateLimiter;
import com.liugh.base.BusinessException;
import com.liugh.base.Constant;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.BillingCdrInfo;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.mapper.BillingCdrInfoMapper;
import com.liugh.service.IAdminInfoService;
import com.liugh.service.ISmsCdrInfoService;
import com.liugh.service.entity.GmmsMessage;
import com.liugh.service.entity.SubmitResponse;
import com.liugh.service.util.GlobalMap;
import com.liugh.service.util.MessageIdGenerator;
import com.liugh.service.util.ThirdPartyLoginHelper;
import com.liugh.service.util.XmlGeneratorUtil;
import com.liugh.util.DateTimeUtil;
import com.liugh.util.HttpUtil;
import com.liugh.util.PhoneNumberGeo;
import com.liugh.util.PhoneNumberInfo;
import com.liugh.util.PhoneUtils;
import com.liugh.util.QuotaManagerUtil;
import com.liugh.util.SmsSendUtil;
import com.liugh.util.SsidLocalCache;

@Component
public class AsyncService {
	
	private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	
	private static final String SUCCESS = "OK";
	
	
	@Autowired
	private ISmsCdrInfoService smsCdrInfoService;
	@Value("${yulore.message.expireTime:5000}")
	private String expireTimeInMs;
	
	@Autowired
	private RedisService redisService;
	
	@Autowired
	private BillingCdrInfoMapper billingCdrInfoMapper;
	
	@Async("sendMsgExecutor")
	public Integer sendMessage(SmsCdrInfo info,String url, String token) throws Exception {
		String response = null;
		String content = info.getContent();
		String sender = info.getSender();
		String textContent = content;
		String recipent = info.getMsisdn();		
		try {						
			// send message
			response = doSendSMS(textContent, sender, recipent, info.getSsid(), url, token);
			// check response
			int success = response.endsWith(SUCCESS)? 0:1;
			String msgId = "";
			if (response.endsWith(SUCCESS)) {
				msgId = response.split(":")[0];
			}else{
				info.setRetryCount(info.getRetryCount()+1);
				logger.info("the message send to customer failed, response is: {}, msisdn is:{}, ssid is:{}",response, recipent, info.getSsid());
			}
					
			info.setMsgId(msgId);
			info.setStatus(success);
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);
			return success;
		} catch (Exception e) {
			info.setStatus(1);
			info.setRetryCount(info.getRetryCount()+1);
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return 1;
	}
	
	@Async("sendMsgExecutor")
	public SubmitResponse sendWaMessage(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();		
		result.setStatus(false);
		try {
			Map<String, Object> request = new HashMap<String, Object>();
			request.put("msisdn", recipent);
			request.put("content", content);
			request.put("timeoutMs", expireTimeInMs);
			// send message
			ThirdPartyLoginHelper.sendWaMsg(request, admin);			
			info.setStatus(0);
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		
		return result;
	}
	
	public boolean isWaRegister(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String recipent = info.getMsisdn();	
		try {
			Map<String, Object> request = new HashMap<String, Object>();
			request.put("msisdn", recipent);			
			// send message
			return ThirdPartyLoginHelper.isWaRegister(request, admin.getUrl());			
		} catch (Exception e) {
			logger.error("isWaRegister failed, the msg is"+info, e);
		}
		return false;
	}
	
	public boolean isInfoBipRCSRegister(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String recipent = info.getMsisdn();	
		/*
		 * { "sender": "441134960000", "phoneNumbers": [ "441134960001", "441134960002"
		 * ] }
		 */
		try {
			Map<String, Object> request = new HashMap<String, Object>();
			request.put("sender", info.getSender());
			List<String> recipients = new ArrayList();
			recipients.add(recipent);
			request.put("phoneNumbers", recipients);
			// send message
			return ThirdPartyLoginHelper.isInfoBipRCSRegister(request, admin.getUrl(),admin.getAppId());			
		} catch (Exception e) {
			logger.error("isInfoBipRCSRegister failed, the msg is"+info, e);
		}
		return false;
	}
	
	
	public SubmitResponse sendMessageToYulore(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);
		try {
			Map<String, Object> request = new HashMap<String, Object>();
			request.put("bizNum", sender);
			request.put("bizName", sender);
			if (!recipent.startsWith("+")) {
				recipent = "+"+recipent;
			}
			request.put("phone", recipent);
			request.put("content", content);
			request.put("timeoutMs", expireTimeInMs);
			// send message
			String response = ThirdPartyLoginHelper.sendMsgToYulore(request, admin);			
			if (StringUtils.isNotBlank(response)) {
				info.setMsgId(response);
				info.setStatus(0);
				result.setOutMsgId(response);
				result.setStatus(true);				
			}else{
				info.setStatus(1);				
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	public SubmitResponse sendRCSMessageToInfobip(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);
		try {
			/*{
				  "messages": [
				               {
				                 "sender": "441134960000",
				                 "destinations": [
				                   {
				                     "to": "441134960001"
				                   }
				                 ],
				                 "content": {
				                   "text": "Some text",
				                   "suggestions": [
				                     {
				                       "text": "Example text",
				                       "postbackData": "Example postback data",
				                       "type": "REPLY"
				                     }
				                   ],
				                   "type": "TEXT"
				                 }
				               }
				             ]
				           }

				           {
				             "messages": [
				               {
				                 "sender": "441134960000",
				                 "destinations": [
				                   {
				                     "to": "441134960001"
				                   }
				                 ],
				                 "content": {
				                   "file": {
				                     "url": "https://www.example.com/video.mp4"
				                   },
				                   "thumbnail": {
				                     "url": "https://www.example.com/thumbnail.jpg"
				                   },
				                   "suggestions": [
				                     {
				                       "text": "Example text",
				                       "postbackData": "Example postback data",
				                       "type": "REPLY"
				                     }
				                   ],
				                   "type": "FILE"
				                 }
				               }
				             ]
				           }
				           */
			Map<String, Object> request = new HashMap<String, Object>();
			List<Object> msgs = new ArrayList<>();
			request.put("messages", msgs);
			Map<String, Object> msg = new HashMap<String, Object>();
			msg.put("sender", sender);
			List<Object> destinations = new ArrayList<>();
			msg.put("destinations", destinations);
			Map<String, Object> tos = new HashMap<String, Object>();
			destinations.add(tos);
			tos.put("to", recipent);
			Map<String, Object> contentMap = new HashMap<String, Object>();
			msg.put("content", contentMap);
			String mobileType = info.getMobileType();
			if("FILE".equalsIgnoreCase(mobileType)) {
				contentMap.put("type", "FILE");
				Map<String, Object> url = new HashMap<String, Object>();
				contentMap.put("file", url);
				url.put("url", content);
			}else {
				contentMap.put("type", "TEXT");
				contentMap.put("text", content);				
			}
			
			
			// send message
			String response = ThirdPartyLoginHelper.sendInfoBipRCSMsg(request, admin.getUrl(),admin.getAppId());			
			if (StringUtils.isNotBlank(response)) {
				info.setMsgId(response);
				info.setStatus(0);
				result.setOutMsgId(response);
				result.setStatus(true);				
			}else{
				info.setStatus(1);				
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	public SubmitResponse sendRCSMessageToAvant(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);
		/*
		 * { "recipient": "54996076961", "class": "Basic", "items": [ { "type": "Text",
		 * "text": "Esse" }, { "type": "Card", "title": "meu chapeu", "text":
		 * "sera q vai bufa", "media": { "url":
		 * "https://images4.alphacoders.com/814/thumb-1920-81446.jpg", "disposition":
		 * "TopMedium", "caption": "Star" }, "buttons": [ { "type": "Link", "text":
		 * "Teste", "url": "https://www.terra.com.br" } ] } ] }
		 */
		try {			
			Map<String, Object> request = new HashMap<String, Object>();
			List<Object> msgs = new ArrayList<>();
			request.put("recipient", recipent);
			request.put("class", "Basic");
			request.put("items", msgs);
			request.put("key", msgId);
			Map<String, Object> msg = new HashMap<String, Object>();
			msgs.add(msg);
			msg.put("type", "text");
			msg.put("text", content);
						
			// send message
			boolean su = ThirdPartyLoginHelper.sendAvantRCSMsg(request, admin.getUrl(),admin.getAppId());			
			if (su) {
				info.setMsgId(msgId);
				info.setStatus(0);
				result.setOutMsgId(msgId);
				result.setStatus(true);				
			}else{
				info.setStatus(1);				
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	public SubmitResponse sendMessageToWABA(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);
		/*{ 
			 "from": "brand", 
			 "recipient": "whatsapp_id", 
			 "type": "template", 
			 "template": { 
			 "name": "template_name", 
			 "language_code": "EN", 
			 "template_params": [ 
			 { 
			"value": "David" 
			}, 
			 { 
			"value": "TN12399512" 
			 }, 
			 { 
			"value": "2020-06-28" 
			 }, 
			 ] 
			 } 
			} */

		try {
			Map<String, Object> request = new HashMap<String, Object>();
			request.put("channel", "whatsapp");
			request.put("from", java.net.URLDecoder.decode(sender,"ASCII"));
			request.put("recipient", recipent);
			request.put("type", "template");
			Map<String, Object> body = new HashMap<>();
			String contents[] = content.split(":");
			if(contents!=null &&contents.length >= 3){
				body.put("name", contents[0]);
				body.put("language_code", contents[1]);				
				List<Map<String, String>> parameters = new ArrayList<>();
				Map<String, String> para = new HashMap<>();
				String parameterV = contents[2];								
				for(int i =3; i<contents.length; i++){
					parameterV = parameterV+":"+contents[i];
				}
				para.put("value", parameterV);
				parameters.add(para);
				body.put("template_params", parameters);
			}
			
			request.put("template", body);
			// send message
			String response = ThirdPartyLoginHelper.sendMsgToWABA(request, admin);		
			BillingCdrInfo billing = null;
			if (StringUtils.isNotBlank(response)) {
				info.setMsgId(response);
				info.setStatus(0);
				billing = BillingCdrInfo.convertBillingCDR(info);
				billing.setRstatus(0);
				result.setOutMsgId(response);
				result.setStatus(true);				
			}else{				
				info.setStatus(1);	
				billing = BillingCdrInfo.convertBillingCDR(info);
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);						
			//save billing info
			billingCdrInfoMapper.insert(billing);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	public SubmitResponse sendA2PMessage(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);		
		try {
			Map<String, Object> request = new HashMap<String, Object>();
			request.put("phone", recipent);
			//request.put("message", URLEncoder.encode(content, "UTF-8"));
			request.put("message", content);
			// send message
			String response = ThirdPartyLoginHelper.sendA2PMsg(request, admin);		
			if (StringUtils.isNotBlank(response)) {
				info.setMsgId(response);
				info.setStatus(0);				
				result.setOutMsgId(response);
				result.setStatus(true);				
			}else{				
				info.setStatus(1);					
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);									
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	public SubmitResponse sendVoiceMsgToBraxis(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);		
		try {
			Map<String, Object> request = new HashMap<String, Object>();
			request.put("phone", recipent);
			//request.put("message", URLEncoder.encode(content, "UTF-8"));
			request.put("otp", content);
			// send message
			String response = ThirdPartyLoginHelper.sendVoiceToBraxis(request, admin);		
			if (StringUtils.isNotBlank(response)) {
				info.setMsgId(response);
				info.setStatus(0);				
				result.setOutMsgId(response);
				result.setStatus(true);				
			}else{				
				info.setStatus(1);					
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);									
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	public SubmitResponse sendViberToMK(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);
		/*{
			 "channel": "viber",
			 "from": "brand",
			 "recipient": "60123456789",
			 "label": "transaction",
			 "type": "text",
			 "text": {
			 "body": "hello"
			 }
			}

		 */

		try {
			Map<String, Object> request = new HashMap<String, Object>();
			request.put("channel", "viber");
			request.put("from", java.net.URLDecoder.decode(sender,"ASCII"));
			request.put("recipient", recipent);
			request.put("type", admin.getCustomerName());
			request.put("label", admin.getDescrption());
			Map<String, Object> body = new HashMap<>();			
			body.put("body", content);
			request.put(admin.getCustomerName(), body);
			// send message
			String response = ThirdPartyLoginHelper.sendViberMsgToMK(request, admin);		
			BillingCdrInfo billing = null;
			if (StringUtils.isNotBlank(response)) {
				info.setMsgId(response);
				info.setStatus(0);
				billing = BillingCdrInfo.convertBillingCDR(info);
				billing.setRstatus(0);
				result.setOutMsgId(response);
				result.setStatus(true);				
			}else{				
				info.setStatus(1);	
				billing = BillingCdrInfo.convertBillingCDR(info);
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);						
			//save billing info
			billingCdrInfoMapper.insert(billing);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	public SubmitResponse sendViberToTelSMS(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		if(!recipent.startsWith("+")) {
			recipent="+"+recipent;
		}
		SubmitResponse result = new SubmitResponse();		
		int randomNumber = ThreadLocalRandom.current().nextInt(0, 1000000);
        // 格式化为6位数，不足前面补0
        String sixDigitNumber = String.format("%06d", randomNumber);
		/*
	        orgCode: 20210106184413AGVA
			mobileArea: +0
			mobiles: +8612345678901
			content: Hello%2520World%2521
			rand: 1ESZj6
			MD5Key: EXCLRCGUPOAHHYJGUXKMNUAMYMQFASFG
			sign: 0A3A721EC712A6605A7869BCC2301BE5
			notifyUrl: http://http://127.0.0.1:8888/result
		 */
        //MD5(orgCode+content+rand+MD5Key)
        String md5pwd = ThirdPartyLoginHelper.md5Encrypt(admin.getAppId()+content+sixDigitNumber+admin.getToken()).toUpperCase();
		try {
			Map<String, String> request = new HashMap<String, String>();
			request.put("orgCode", admin.getAppId());
			request.put("mobileArea", "+0");
			request.put("mobiles", recipent);
			request.put("content", URLEncoder.encode(URLEncoder.encode(content,"UTF-8"),"UTF-8"));
			request.put("rand", sixDigitNumber);
			request.put("sign", md5pwd);
			
			// send message
			String response = ThirdPartyLoginHelper.sendViberToTelSMS(request, admin.getUrl());		
			BillingCdrInfo billing = null;
			if (StringUtils.isNotBlank(response)) {
				info.setMsgId(response);
				info.setStatus(0);
				billing = BillingCdrInfo.convertBillingCDR(info);
				billing.setRstatus(0);
				result.setOutMsgId(response);
				result.setStatus(true);				
			}else{				
				info.setStatus(1);	
				billing = BillingCdrInfo.convertBillingCDR(info);
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);						
			//save billing info
			billingCdrInfoMapper.insert(billing);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	
	public SubmitResponse sendZaloToMK(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);
			/*{
				"from": "brand",
				"recipient": "phone_number",
				"type": "template",
				"template": {
				"name": "template_name",
				"data": [
				{
				"param": "name",
				"value": "David"
				},
				{
				"param": "order_code",
				"value": "TN12399512"
				}
			]
			}
			}
		 */

		try {
			Map<String, Object> request = new HashMap<String, Object>();
			request.put("channel", "zalo");
			request.put("from", java.net.URLDecoder.decode(sender,"ASCII"));
			request.put("recipient", recipent);
			request.put("type", "template");
			Map<String, Object> body = new HashMap<>();			
			String contents[] = content.split(":");
			if(contents!=null &&contents.length >= 3){
				body.put("name", contents[0]);				
				List<Map<String, String>> parameters = new ArrayList<>();
				for(int i =1; i<contents.length-1; i++){
					Map<String, String> para = new HashMap<>();
					para.put("param", contents[i]);
					para.put("value", contents[i+1]);
					i++;
					parameters.add(para);
				}
				body.put("data", parameters);
			}			
			request.put("template", body);
			// send message
			String response = ThirdPartyLoginHelper.sendZaloMsgToMK(request, admin);		
			BillingCdrInfo billing = null;
			if (StringUtils.isNotBlank(response)) {
				info.setMsgId(response);
				info.setStatus(0);
				billing = BillingCdrInfo.convertBillingCDR(info);
				billing.setRstatus(0);
				result.setOutMsgId(response);
				result.setStatus(true);				
			}else{				
				info.setStatus(1);	
				billing = BillingCdrInfo.convertBillingCDR(info);
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);						
			//save billing info
			billingCdrInfoMapper.insert(billing);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	public SubmitResponse sendMessageToCMWABA(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);		
		/*{
		    "messages": {
		        "authentication": {
		            "producttoken": "your product token"
		        },
		        "msg": [{
		            "from": "00316098765432",
		            "to": [{
		                    "number": "0031612345678"
		                }
		            ],
		            "body": {
		                "type": "auto",
		                "content": "This is a WhatsApp message"
		            },
		            "allowedChannels": ["WhatsApp"],
		            "richContent": {
		                "conversation":
		                [{
		                    "template": {
		                        "whatsapp": {
		                            "namespace": "your namespace id here",
		                            "element_name": "TEMPLATE_NAME",
		                            "language": {
		                                "policy": "deterministic",
		                                "code": "en"
		                            },
		                            "components": [{
		                                "type": "body",
		                                "parameters": [
		                                    {
		                                        "type": "text",
		                                        "text": "your-text-string"
		                                    }*/
		try {
			Map<String, Object> request = new HashMap<String, Object>();
			Map<String, Object> messages = new HashMap<String, Object>();
			request.put("messages", messages);
			Map<String, Object> authentication = new HashMap<String, Object>();
			authentication.put("producttoken", admin.getAppSecurity());
			messages.put("authentication", authentication);
			List<Object> msgs = new ArrayList<>();
			messages.put("msg", msgs);
			messages.put("reference", msgId);
			Map<String, Object> msg = new HashMap<>();
			msgs.add(msg);
			msg.put("from", sender);
			
			List<Object> tos = new ArrayList<>();
			msg.put("to", tos);
			Map<String, Object> to = new HashMap<>();
			tos.add(to);
			if(!recipent.startsWith("00")) {
				recipent = "00"+recipent;
			}
			to.put("number", recipent);
			Map<String, Object> body= new HashMap<>();
			msg.put("body", body);
			body.put("type", "auto");
			body.put("content", "This is a WhatsApp message");
			List<Object> allowedChannels = new ArrayList<>();
			allowedChannels.add("WhatsApp");
			msg.put("allowedChannels", allowedChannels);
			Map<String, Object> richContent = new HashMap<>();
			msg.put("richContent", richContent);
			List<Object> conversations = new ArrayList<>();
			richContent.put("conversation", conversations);
			Map<String, Object> conversation = new HashMap<>();
			conversations.add(conversation);
			Map<String, Object> template = new HashMap<>();
			conversation.put("template", template);
			Map<String, Object> whatsapp = new HashMap<>();
			template.put("whatsapp", whatsapp);
			//TODO
			
			String contents[] = content.split(":");
			if(contents!=null &&contents.length >= 3){
				whatsapp.put("namespace", contents[0]);
				whatsapp.put("element_name", contents[1]);
				Map<String, Object> language = new HashMap<>();
				whatsapp.put("language", language);
				language.put("code", contents[2]);
				language.put("policy", "deterministic");//"policy": "deterministic",				
				List<Object> components = new ArrayList<>();
				whatsapp.put("components", components);
				Map<String, Object> component = new HashMap<>();
				components.add(component);
				component.put("type", "body");
				List<Map<String, String>> parameters = new ArrayList<>();
				component.put("parameters", parameters);				
				for(int i =3; i<contents.length; i++){
					Map<String, String> para = new HashMap<>();
					para.put("type", "text");
					para.put("text", contents[i]);
					parameters.add(para);
				}
				//button
				Map<String, Object> component1 = new HashMap<>();
				components.add(component1);
				component1.put("type", "button");
				component1.put("sub_type", "url");
				component1.put("index", "0");
				List<Map<String, String>> parameters1 = new ArrayList<>();
				component1.put("parameters", parameters1);				
				for(int i =3; i<contents.length; i++){
					Map<String, String> para = new HashMap<>();
					para.put("type", "text");
					para.put("text", contents[i]);
					parameters1.add(para);
				}
				
				
				
			}			
			// send message
			String response = ThirdPartyLoginHelper.sendMsgToCMWABA(request, admin);	
			BillingCdrInfo billing = null;
			if (StringUtils.isNotBlank(response)) {
				info.setMsgId(response);
				info.setStatus(0);
				billing = BillingCdrInfo.convertBillingCDR(info);
				result.setOutMsgId(response);
				result.setStatus(true);				
			}else{
				info.setStatus(1);	
				billing = BillingCdrInfo.convertBillingCDR(info);
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);
			//save billing info
			billingCdrInfoMapper.insert(billing);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	public SubmitResponse sendMessageToTelegram(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		result.setOutMsgId(info.getMsgId());
		result.setStatus(false);		
		/**/
		try {
			Map<String, Object> request = new HashMap<String, Object>();
			
			if(!recipent.startsWith("+")) {
				recipent = "+"+recipent;
			}
			request.put("phone_number", recipent);
			request.put("request_id", info.getMsgId());
			request.put("code", content);
			request.put("callback_url", admin.getUrl());
			request.put("ttl", admin.getExpireTime());
				
			// send message
			Map<String,String> response = ThirdPartyLoginHelper.sendTelegramMsg(admin, request);	
			BillingCdrInfo billing = null;
			if (response != null) {				
				info.setStatus(0);
				billing = BillingCdrInfo.convertBillingCDR(info);				
				result.setStatus(true);				
			}else{
				info.setStatus(1);	
				billing = BillingCdrInfo.convertBillingCDR(info);
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);
			//save billing info
			billingCdrInfoMapper.insert(billing);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	
	public SubmitResponse sendMessageToMms(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);
		try {
			Map<String, Object> request = new HashMap<String, Object>();			
			request.put("msisdn", recipent);
			request.put("text", content);
			// send message
			String response = ThirdPartyLoginHelper.sendMsgToMms(request, admin);			
			if (StringUtils.isNotBlank(response)) {
				info.setMsgId(response);
				info.setStatus(0);
				result.setOutMsgId(response);
				result.setStatus(true);				
			}else{
				info.setStatus(1);				
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	@Async("sendMsgExecutor")
	public SubmitResponse sendMmsToMK(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		String msgId = info.getMsgId();		
		result.setOutMsgId(msgId);
		result.setStatus(false);
		try {
			Map<String, Object> request = new HashMap<String, Object>();	
			request.put("senderid", sender);
			request.put("isflash", 0);
			String cc = PhoneUtils.getCountryCodeByPhoneStr(recipent);
			request.put("country", Integer.parseInt(cc));
			request.put("msisdn", recipent);
			request.put("msgid", msgId);
			request.put("msgtype", 1);
			String timeStamp = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(), "yyyyMMddHHmmssSSS", "GMT+8");
			request.put("timestamp", Long.parseLong(timeStamp));			
			request.put("data", content);
			
			String appid = admin.getAppId();
            String token = admin.getAppSecurity();
			String md5pwd = ThirdPartyLoginHelper.md5Encrypt(appid+sender+0+cc+recipent+content+1+timeStamp+token);
			request.put("mac", md5pwd);	
			request.put("userid", appid);
			// send message					
			if (ThirdPartyLoginHelper.sendMMSToMK(request, admin)) {
				info.setStatus(0);
				result.setStatus(true);				
			}else{
				info.setStatus(1);				
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
		
	public SubmitResponse sendMyMmsToMK(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);		

		try {
			Map<String, Object> request = new HashMap<String, Object>();			
			request.put("msisdn", recipent);
			request.put("text", content);			
			// send message
			String response = ThirdPartyLoginHelper.sendMYMmsToMK(request, admin);		
			BillingCdrInfo billing = null;
			if (StringUtils.isNotBlank(response)) {
				info.setMsgId(response);
				info.setStatus(5);
				billing = BillingCdrInfo.convertBillingCDR(info);
				billing.setRstatus(0);
				result.setOutMsgId(response);
				result.setStatus(true);				
			}else{				
				info.setStatus(1);	
				billing = BillingCdrInfo.convertBillingCDR(info);
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);						
			//save billing info
			billingCdrInfoMapper.insert(billing);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
		
	}

	private String doSendSMS(String content, String sender, String recipient, int ssid, String url, String token) {
		String response = null;
		try {
			String charset = SmsSendUtil.isUnicode(content)? GmmsMessage.AIC_CS_UCS2:GmmsMessage.AIC_CS_ASCII;
			GmmsMessage message = new GmmsMessage();
			message.setContentType(charset);
			message.setRecipientAddress(recipient);
			message.setSenderAddress(sender);
			message.setTextContent(content);
			String xml = XmlGeneratorUtil.createXml(token, message, ssid);
			response = HttpUtil.post(url, xml);
			logger.info("send sms finished, msisdn: "+recipient+", response: "+response+", msgid="+message.getOutMsgID());
		} catch (Exception e) {
			logger.error("do send sms error, msisdn: "+recipient, e);
		}

		return response;
	}
	
	@Async("sendMsgExecutor")
	public void sendSMS2SimBank(SmsCdrInfo info, AdminInfo rCustomer) {				
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();	
		SubmitResponse result = new SubmitResponse();
		/*String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);*/
		try {
			Map<String, Object> request = new HashMap<String, Object>();
			request.put("type", "send-sms");
			request.put("task_num", "1");			
			List<Map<String, Object>> tasks = new ArrayList<>();
			Map<String, Object> task = new HashMap<String, Object>();
			task.put("tid", info.getMsgId());
			task.put("to", recipent);
			task.put("sms", content);
			task.put("sdr", "1");
			task.put("dr", "1");
			String ports = rCustomer.getPorts();
			if(StringUtils.isNotBlank(ports)){
				if(ports.startsWith("max")){
					ports = ports.substring(3);					
					int size = Integer.parseInt(ports);
					int temp = MessageIdGenerator.queryIndex%size+1;
					task.put("from", ""+temp);
					info.setSender(""+temp);
					if(MessageIdGenerator.queryIndex>Integer.MAX_VALUE-10){
						MessageIdGenerator.queryIndex = 0;
					}else{
						MessageIdGenerator.queryIndex++;
					}
					
				}else{
					List<String> portList = Arrays.asList(ports.split(","));
					int index = (recipent.hashCode() & Integer.MAX_VALUE)%portList.size();
					if(index<0){
						index = Math.abs(index);
					}
					task.put("from", portList.get(index));
					info.setSender(portList.get(index));
				}								
			}
				
			
			tasks.add(task);
			request.put("tasks", tasks);
			// send message
			boolean response = ThirdPartyLoginHelper.sendMsgToSim(request, rCustomer);			
			if (response) {				
				info.setStatus(0);											
			}else{
				AdminInfo oCustomer = GlobalMap.admin.get(String.valueOf(info.getSsid()));
				if(oCustomer!=null){
					ThirdPartyLoginHelper.sendDRToSMSGW(oCustomer.getUrl(), ""+info.getMsgId(), 4);
				}else {
					logger.error("can't found o customer of ssid {} to send dr", info.getSsid());
				}
				info.setStatus(2);				
			}
			//save cdr info
			//info.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			info.setCreateTime(System.currentTimeMillis());
			smsCdrInfoService.insertOrUpdate(info);
			return ;
		} catch (Exception e) {
			info.setStatus(2);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return ;			
					
	}
	
	public SubmitResponse sendOTTMsgToBRAX(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();			
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);		

		try {
			Map<String, Object> request = new HashMap<String, Object>();			
			request.put("from", java.net.URLDecoder.decode(sender,"ASCII"));			
			request.put("type", admin.getAppId());
			request.put("useUnicode", 0);
			/*
			 * Map<String, Object> body = new HashMap<>(); String contents[] =
			 * content.split(":"); if(contents!=null &&contents.length >= 3){
			 * body.put("templateId", contents[0]); Map<String, Object> para = new
			 * HashMap<>(); int useUnicode = 0; if(!contents[1].equals("en")) {
			 * useUnicode=1; } para.put("useUnicode", useUnicode); for(int i =3;
			 * i<contents.length-1; i++){ para.put(contents[i], contents[i+1]); }
			 * body.put("templateData", para); body.put("to", recipent); } List<Map<String,
			 * Object>> msg = new ArrayList(); msg.add(body);
			 */
			request.put("to", recipent);
			request.put("message", content);
			request.put("scheduled", "");
			// send message
			String response = ThirdPartyLoginHelper.sendOTTMsgToBRAX(request, admin);		
			BillingCdrInfo billing = null;
			if (StringUtils.isNotBlank(response)) {
				info.setMsgId(response);
				info.setStatus(0);
				billing = BillingCdrInfo.convertBillingCDR(info);
				billing.setRstatus(0);
				result.setOutMsgId(response);
				result.setStatus(true);				
			}else{				
				info.setStatus(1);	
				billing = BillingCdrInfo.convertBillingCDR(info);
			}
			//save cdr info
			smsCdrInfoService.insertOrUpdate(info);						
			//save billing info
			billingCdrInfoMapper.insert(billing);
			return result;
		} catch (Exception e) {
			info.setStatus(1);						
			smsCdrInfoService.insertOrUpdate(info);
			logger.error("send message failed, the msg is"+info, e);
		}
		return result;
	}
	
	public SubmitResponse sendMsgToAvant(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();			
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);		

		try {
			Map<String, String> request = new HashMap<String, String>();			
			request.put("usuario", admin.getAppId());
			request.put("senha", admin.getAppSecurity());
			request.put("cc_interno", "123");
			request.put("numero", recipent);
			request.put("id_campanha", " ");
			request.put("mensagem", content);
			request.put("serial", msgId);
			/*
			 https://sms.solucoesdigitais.cc/integracao/v4/envio_simples?
			 usuario=USER@&senha=PASSWORD&cc_interno=123&id_campanha=00&
			 campanha=00&numero=55998888888888&mensagem=envio_simples
			 user* = User access to platform
			password* = Password to access platform
			id_campanha* = Identification of the campaign in the system, if it is the first use of the ID, a new campaign will be recorded, if the ID entered already has been registered in the systems, All SMS will be added to the existing campaign.
			campaign = Description of the campaign, if not informed to Description An automatic description will be saved EX. "Sending by API - (id_campanha)"
			cc_interno* = Number of cost center control, this number is informed in the cost center registration.
			number* = DDI + DDD + Recipient/Mobile number that will receive the SMS
			message* = SMS to be sent. Or system accepts SMS greater than 160 Characters. To break the line use the command \n
			serial = sms id, in this field you can enter some id of control of your system, if it is not informed the API automatically creates one, EX: "d2e5-ec3b-eb7a-2997-a9852". Note: This information must be unique.
			*/
			// send message	
			if (ThirdPartyLoginHelper.sendMsgToAvant(request, admin.getUrl())) {
				result.setStatus(true);				
			}			
			return result;
		} catch (Exception e) {
			logger.error("sendMsgToAvant, the msg is {}", info, e);
		}
		return result;
	}
	
	public SubmitResponse sendMsgToCMI(SmsCdrInfo info, AdminInfo admin) throws Exception {
		String content = info.getContent();
		String sender = info.getSender();
		String recipent = info.getMsisdn();			
		SubmitResponse result = new SubmitResponse();
		String msgId = MessageIdGenerator.generateCommonOutMsgID(info.getSsid());
		info.setMsgId(msgId);
		result.setOutMsgId(msgId);
		result.setStatus(false);		

		try {
			Map<String, Object> request = new HashMap<String, Object>();			
			
			Map<String, Object> uipHead = new HashMap<String, Object>();
			request.put("uip_head", uipHead);
			uipHead.put("CHANNEL", admin.getAppId());
			uipHead.put("AUTH_KEY", admin.getAppSecurity());
			uipHead.put("METHOD", "SMS_SEND_REQUEST");
			uipHead.put("SERIAL", GlobalMap.getCMICount());
			uipHead.put("TIME", DateTimeUtil.getCurrentUTCTime());
			
			Map<String, Object> uipbody = new HashMap<String, Object>();
			request.put("uip_body", uipbody);
			uipbody.put("SMS_CONTENT", content);
			uipbody.put("ORIGINAL_ADDR", sender);
			uipbody.put("PRIORITY", 1);
			List recList = new ArrayList<>();
			recList.add(recipent);
			uipbody.put("DESTINATION_ADDR", recList);
			//uipbody.put("ORDER_NUMBER", msgId);
			uipbody.put("CUSTOMER_BODY", msgId);
			request.put("uip_version", 2);
			/*
			 {
			     "uip_head": {
			          "METHOD": "SMS_SEND_REQUEST",
			          "SERIAL": 1,
			          "TIME": "2023-01-01T12:00:00Z",
			          "CHANNEL": "CMI",# Please use your tenant name, which is assigned by CloudSMS 
			          "AUTH_KEY": "AUTH_KEY corresponding to Test"
			     },
			     "uip_body": {
			          "SMS_CONTENT": "Test",
			          "ORDER_NUMBER": "cd000000001",
			          "DESTINATION_ADDR": [
			               "8618888888888"
			          ],
			          "ORIGINAL_ADDR": "CMI",
			          "SIGNATURE": "cmi",
			          "SIGNATURE_TYPE": "HEAD_BOLDFACE_SQUARE",
			          "PRIORITY": 1,
			          "CUSTOMER_BODY": "test"
			     },
			     "uip_version": 2
			}*/
			// send message	
			String resultId = ThirdPartyLoginHelper.sendMsgToCMI(request, admin);
			if (StringUtils.isNoneBlank(resultId)) {
				result.setStatus(true);	
				//result.setOutMsgId(resultId);
				//info.setMsgId(resultId);
			}			
			return result;
		} catch (Exception e) {
			logger.error("sendMsgToCMI, the msg is {}", info, e);
		}
		return result;
	}
}

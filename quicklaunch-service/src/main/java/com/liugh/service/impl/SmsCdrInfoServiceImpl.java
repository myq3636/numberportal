package com.liugh.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.activation.FileDataSource;

import com.liugh.base.Constant;
import com.liugh.base.EnumberDTO;
import com.liugh.base.MonitorCountryReport;
import com.liugh.base.MonitorDuplicateNumResponse;
import com.liugh.base.MonitorLatencyDistributeResponse;
import com.liugh.base.MonitorLatencyReport;
import com.liugh.base.MonitorReport;
import com.liugh.base.MonitorRequest;
import com.liugh.base.SMSLogging;
import com.liugh.base.SMSLoggingField;
import com.liugh.base.SMSLoggingRequestVo;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.BillingCdrInfo;
import com.liugh.entity.NumberInfo;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.entity.RcsSendTask;
import com.liugh.entity.RoutingInfo;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.mapper.BillingCdrInfoMapper;
import com.liugh.mapper.SmsCdrInfoMapper;
import com.liugh.rcs.channel.RcsChannelAdapter;
import com.liugh.rcs.channel.RcsChannelAdapterRegistry;
import com.liugh.rcs.channel.RcsChannelSubmitException;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.channel.infobip.RcsContentParser;
import com.liugh.rcs.model.ContentType;
import com.liugh.rcs.model.RcsContent;
import com.liugh.rcs.model.RcsContentValidator;
import com.liugh.rcs.service.RcsCallbackService;
import com.liugh.rcs.service.RcsCustomerAccessException;
import com.liugh.rcs.service.RcsFileService;
import com.liugh.rcs.service.RcsMessagePersistenceService;
import com.liugh.rcs.service.RcsRouter;
import com.liugh.service.IAdminInfoService;
import com.liugh.service.IBillingCdrInfoService;
import com.liugh.service.INumberInfoService;
import com.liugh.service.IRoutingInfoService;
import com.liugh.service.ISmsCdrInfoService;
import com.liugh.service.entity.SubmitResponse;
import com.liugh.service.util.Convert;
import com.liugh.service.util.GlobalMap;
import com.liugh.service.util.MessageIdGenerator;
import com.liugh.service.util.ThirdPartyLoginHelper;
import com.liugh.util.DateTimeUtil;
import com.liugh.util.ExcelUtil;
import com.liugh.util.PhoneUtils;
import com.liugh.util.QuotaManagerUtil;
import com.liugh.util.StringUtil;

import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;


/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author liugh123
 * @since 2019-06-24
 */
@Service
@Slf4j
public class SmsCdrInfoServiceImpl extends
		ServiceImpl<SmsCdrInfoMapper, SmsCdrInfo> implements ISmsCdrInfoService {
	
	@Autowired
    private RcsMessagePersistenceService persistenceService;
    
    @Autowired
    private RcsRouter rcsRouter;
    
    @Value("${project.url}")
    private String projectUrl;

	@Autowired
	private SmsCdrInfoMapper smsCdrInfoMapper;
	
	@Autowired
    private RcsChannelAdapterRegistry adapterRegistry;
	
	@Autowired
    private RcsCallbackService rcsCallbackService;
	
	@Autowired
	private BillingCdrInfoMapper billingCdrInfoMapper;
	
	@Autowired
	private AsyncService asyncService;
	
	@Autowired
	private INumberInfoService numberInfoService;
	
	@Autowired
	private IAdminInfoService adminInfoService;
	
	@Autowired
	private IRoutingInfoService routing;
	
	@Value("${wa.msg.limit:300}")
	private String msgLimit;
	
	private static int accountIndex=0;
	
	@Autowired
	private ISmsCdrInfoService smsCdrInfoService;
	
	@Autowired
	private IBillingCdrInfoService billingCdrInfoService;
	
	@Autowired
	private RedisService redisService;
	
	@Override
	public List<Map<String, Object>> groupByCdrStatus(String filename) {

		return smsCdrInfoMapper.groupByCdrStatus(filename);
	}

	@Override
	public Page<SmsCdrInfo> selectPageByConditionUser(Page<SmsCdrInfo> page,
			String number, String effectiveStartTime,
			String effectiveEndTime, AdminInfo admin) {
		long unitStartTime = 0L;
		if (StringUtils.isBlank(effectiveStartTime)) {
			String currentDay = QuotaManagerUtil
					.getFormatTimeFromMsWithTimeZone(
							System.currentTimeMillis(), "yyyy-MM-dd", "GMT+8");
			unitStartTime = QuotaManagerUtil.getMsFromFormatTimeWithTimeZone(
					currentDay, "yyyy-MM-dd", "GMT+8");
		} else {
			unitStartTime = Long.parseLong(effectiveStartTime) - 8 * 60 * 60
					* 1000;
		}

		long unitEndTime = 0L;
		if (StringUtils.isBlank(effectiveEndTime)) {
			unitEndTime = System.currentTimeMillis();
		} else {
			unitEndTime = Long.parseLong(effectiveEndTime) + 16 * 60 * 59
					* 1000;
		}
		List<SmsCdrInfo> cdrList = smsCdrInfoMapper.selectPageByConditionCdr(
				page, number, unitStartTime + "", unitEndTime + "",
				admin.getSsid());
		return page.setRecords(cdrList);
	}


	@Override
	public Page<SmsCdrInfo> selectPageBySsid(Page<SmsCdrInfo> page,
			Integer statusCode, String effectiveStartTime, int order,
			String effectiveEndTime, int ssid) {
		return null;
	}

	public String excel(AdminInfo admin, int statusCode, String startTime,
			String endTime, int order, int batch) throws Exception {
		

		return null;
	}

	

	@Override
	public boolean sendMessage(JSONObject requestJson) {
		
		try {
			String calling = requestJson.getString("calling");
			String called = requestJson.getString("called");
			if (!called.startsWith("+")) {
				called = "+"+called;
			}
			String msgBody = requestJson.getString("msgBody");
			String time = requestJson.getString("time");
			NumberInfo numberInfo = numberInfoService.selectOne(new EntityWrapper<NumberInfo>().eq("number", called));
			SmsCdrInfo cdrInfo = new SmsCdrInfo();
			cdrInfo.setContent(msgBody);
			long timemark = QuotaManagerUtil.getMsFromFormatTimeWithTimeZone(time, "yyyyMMddHHmmss", "GMT+8");
			cdrInfo.setReceiveTime(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(timemark,"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			cdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			cdrInfo.setCreateTime(System.currentTimeMillis());
			cdrInfo.setSender(calling);
			cdrInfo.setMsisdn(called);
			if (numberInfo == null) {
				cdrInfo.setStatus(2);								
				smsCdrInfoMapper.insert(cdrInfo);
				return false;
			}else {				
				try {
					AdminInfo adminInfo = adminInfoService.selectById(numberInfo.getCustomerId());
					cdrInfo.setSsid(numberInfo.getCustomerId());				
					asyncService.sendMessage(cdrInfo, adminInfo.getUrl(), adminInfo.getToken());
				} catch (Exception e) {
					log.error("send msg to customer failed, {}", cdrInfo, e);
				}
				
				return true;
			}			
		} catch (Exception e) {
			return false;
		}
		
	}
	
	@Override
	public String sendWaMessage(JSONObject requestJson) {
		String outmsgId="";
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");
			String msgBody = requestJson.getString("Content");			
			List<Long> clientList = new ArrayList();
			clientList.addAll(GlobalMap.waClients);
			Collections.sort(clientList);	
			SmsCdrInfo cdrInfo = new SmsCdrInfo();			
			cdrInfo.setContent(msgBody);
			cdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			
			cdrInfo.setSender(calling);
			cdrInfo.setMsisdn(called);
			AdminInfo adminInfo = null;
			if(clientList.isEmpty()){
				outmsgId = MessageIdGenerator.generateCommonOutMsgID(9);
				cdrInfo.setMsgId(outmsgId);	
				cdrInfo.setStatus(1);
				cdrInfo.setSsid(9);
				smsCdrInfoService.insertOrUpdate(cdrInfo);
				log.info("can't found wa client to send msg, {}, msg: {}", cdrInfo.getMsisdn(), cdrInfo);
				return outmsgId;
			}
			if(GlobalMap.lastSendClientIndex>=clientList.size()){
				adminInfo = adminInfoService.selectById(clientList.get(0));
			}else{
				adminInfo = adminInfoService.selectById(clientList.get(GlobalMap.lastSendClientIndex));
			}			
			
			
			if (adminInfo == null) {
				/*cdrInfo.setStatus(1);								
				smsCdrInfoMapper.insert(cdrInfo);*/
				outmsgId = MessageIdGenerator.generateCommonOutMsgID(9);
				cdrInfo.setMsgId(outmsgId);	
				cdrInfo.setStatus(1);
				cdrInfo.setSsid(9);
				long currentTime = System.currentTimeMillis();
				cdrInfo.setCreateTime(currentTime);			
				smsCdrInfoService.insertOrUpdate(cdrInfo);
				log.info("can't found wa client to send msg, {}, msg: {}", cdrInfo.getMsisdn(), cdrInfo);
				return outmsgId;
			}else {	
				outmsgId = MessageIdGenerator.generateCommonOutMsgID(adminInfo.getSsid());long currentTime = System.currentTimeMillis();
				long expireTime = currentTime+adminInfo.getExpireTime()*1000;
				cdrInfo.setCreateTime(currentTime);			
				cdrInfo.setExpireTime(expireTime);
				cdrInfo.setMsgId(outmsgId);	
				cdrInfo.setSsid(adminInfo.getSsid());
				if(!asyncService.isWaRegister(cdrInfo, adminInfo)){
					log.info("message recipent is not register, {}, msg: {}", cdrInfo.getMsisdn(), cdrInfo);
					cdrInfo.setStatus(1);
					smsCdrInfoService.insertOrUpdate(cdrInfo);
					return outmsgId;
				}
				
				
				/*try {					
					cdrInfo.setSsid(adminInfo.getSsid());				
					asyncService.sendWaMessage(cdrInfo, adminInfo);
				} catch (Exception e) {					
					log.error("send msg to wa failed, {}", cdrInfo, e);
					return false;
				}
				
				GlobalMap.lastSendClient = adminInfo.getId();
    			Long count = GlobalMap.clientCount.get(""+adminInfo.getId());
    			if(count == null){
    				GlobalMap.clientCount.put(""+adminInfo.getId(), 1L);
    			}else{
    				GlobalMap.clientCount.put(""+adminInfo.getId(), count+1);
    			}
				*/
				
				try {					
					//save cdr info
					cdrInfo.setStatus(-1);
					smsCdrInfoService.insertOrUpdate(cdrInfo);
				} catch (Exception e) {					
					log.error("send msg to wa failed, {}", cdrInfo, e);
					return outmsgId;
				}
				
				return outmsgId;
			}			
		} catch (Exception e) {
			log.error("send msg to wa failed, ", e);
			return outmsgId;
		}
		
	}
	
	@Override
	public SubmitResponse sendRCSMessage(JSONObject request) {
		/*
		 * SubmitResponse result = new SubmitResponse(); try { String calling =
		 * requestJson.getString("Sender"); String called =
		 * requestJson.getString("Recipient"); if (!called.startsWith("+")) { called =
		 * "+"+called; } String msgBody = requestJson.getString("Content"); String token
		 * = requestJson.getString("Token"); String contentType =
		 * requestJson.getString("type"); //String token =
		 * requestJson.getString("Token"); AdminInfo adminInfo =
		 * GlobalMap.adminToken.get(token); if (adminInfo == null) {
		 * log.error("can not found the ocustomer info, the sms:{}", requestJson);
		 * return result; } String countrySN = PhoneUtils.getRegionCodeByPhone(called);
		 * int rssid =
		 * GlobalMap.routingTable.get(Integer.parseInt(""+adminInfo.getId()),
		 * countrySN); if (rssid==0) {
		 * log.error("can not found the rcustomer info, the sms:{}, countrySN:{}",
		 * requestJson, countrySN); return result; }
		 * 
		 * AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid)); SmsCdrInfo
		 * smsCdrInfo = new SmsCdrInfo();
		 * smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
		 * smsCdrInfo.setSender(calling); smsCdrInfo.setMsisdn(called);
		 * smsCdrInfo.setContent(msgBody); smsCdrInfo.setMobileType(contentType); if
		 * (!asyncService.isInfoBipRCSRegister(smsCdrInfo, rCustomer)) { return result;
		 * } smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(
		 * System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8")); long currentTime
		 * = System.currentTimeMillis(); long expireTime =
		 * currentTime+rCustomer.getExpireTime()*1000;
		 * smsCdrInfo.setCreateTime(currentTime); smsCdrInfo.setExpireTime(expireTime);
		 * result = asyncService.sendRCSMessageToInfobip(smsCdrInfo, rCustomer);
		 * 
		 * 
		 * //result="yulore123456"; } catch (Exception e) {
		 * log.error("sendMessageToYulore throw error, the sms:{}", requestJson, e); }
		 * 
		 * return result;
		 */	
		SubmitResponse result = new SubmitResponse();
        result.setStatus(false);
        try {
            AdminInfo customer = GlobalMap.admin.get(request.getString("Token"));
            String sender = request.getString("Sender");
            String recipient = request.getString("Recipient");
            if (recipient == null) {
                log.error("rcs send missing recipient");
                return result;
            }
            if (!recipient.startsWith("+")) {
                recipient = "+" + recipient;
            }
            String context = request.getString("Content");
            // Token → 归属客户(ssid)，客户不直传 ssid（与原平台一致）
            int ssid = customer.getSsid();
            String agentId = request.getString("agentId");
            //String trafficType = firstNonBlank(request.getString("trafficType"), "TRANSACTION");
            Integer ttl = request.getInteger("ttl");
            String contentKeywordId = request.getString("contentKeywordId");

            RcsContent content = new RcsContent();
            content.setText(context);
            content.setType(ContentType.TEXT);

            // 路由选通道：ossid(=customer.id) + 收件国家 → rssid → 供应商 admin_info（与 SMS 一致）
            // 供应商记录同时携带 channel_code（选适配器）与连接凭证（url/appId/appSecurity）。
            String country = PhoneUtils.getRegionCodeByPhone(recipient);
            AdminInfo supplier = rcsRouter.route(customer, country, sender, contentKeywordId);
            if (supplier == null) {
                log.error("rcs no supplier routed, ssid:{}, country:{}", ssid, country);
                return result;
            }
            String channelCode = supplier.getChannelCode();
            Integer rssid = toInt(supplier.getId());
            RcsChannelAdapter adapter = adapterRegistry.get(channelCode);
            if (adapter == null) {
                log.error("rcs no adapter for channel {}, rssid:{}", channelCode, rssid);
                return result;
            }
            // sender-first：未传 sender 时用 agent 在该通道的绑定
            if (sender == null && agentId != null) {
                RcsChannelAgent binding = rcsRouter.resolveAgentBinding(agentId, channelCode);
                if (binding != null) {
                    sender = binding.getSender();
                }
            }

            String msgId = MessageIdGenerator.generateCommonOutMsgID(ssid);
            long now = System.currentTimeMillis();

            RCSCdrInfo cdr = new RCSCdrInfo();
            cdr.setSsid(ssid);
            cdr.setRssid(rssid == null ? 0 : rssid);
            cdr.setMsgId(msgId);
            cdr.setMsisdn(recipient);
            cdr.setSender(sender);
            cdr.setContent(context);
            cdr.setMsgType(content.getType().name());
            //cdr.setTrafficType(trafficType);
            cdr.setChannelCode(channelCode);
            cdr.setCountryCode(country);
            //cdr.setThumbnail(extractThumbnail(content));
            //cdr.setSuggestions(extractSuggestionsJson(request));
            cdr.setStatus(-1);
            cdr.setCreateTime(now);
            cdr.setInsubmittime(now);
            cdr.setExpireTime(ttl == null ? 0 : now + ttl * 1000L);
            cdr.setRetryCount(0);
            cdr.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(now, "yyyy-MM-dd HH:mm:ss", "GMT+8"));
            BillingCdrInfo billing = buildBilling(cdr, -1);

            RcsSendCtx ctx = new RcsSendCtx();
            ctx.setSsid(ssid);
            ctx.setSender(sender);
            ctx.setRecipient(recipient);
            ctx.setMsgId(msgId);
            ctx.setNotifyUrl(buildNotifyUrl(channelCode));
            ctx.setTtl(ttl);
            ctx.setChannelCode(channelCode);
            ctx.setRssid(rssid);
            // Infobip: apiKey=admin.appId；RM: user=admin.appId, pass=admin.appSecurity
            ctx.setBaseUrl(supplier.getUrl());
            ctx.setApiKey(supplier.getAppId());
            ctx.setAuthUser(supplier.getAppId());
            ctx.setAuthPass(supplier.getAppSecurity());
            ctx.setSmsSender(supplier.getSmsSender());
            ctx.setFallbackText(request.getString("fallbackText"));
            ctx.setCountryCode(country);
            ctx.setChannelSender(sender);

            persistenceService.acceptDirect(cdr, billing);
            String channelMsgId;
            try {
                channelMsgId = adapter.send(content, ctx);
            } catch (RcsChannelSubmitException e) {
                persistenceService.markFinalFailure(cdr, e.getErrorCode(), e.getErrorReason());
                return result;
            }
            if (channelMsgId != null) {
                if (adapter.isImmediateDelivery()) {
                    // 即时投递通道（如 BLACKHOLE）：跳过 submit→callback 流程，
                    // 直接推进到 status=3 并同步触发客户 DR 通知。
                    persistenceService.markDirectDelivered(cdr, channelMsgId);
                    rcsCallbackService.fireSyntheticDelivery(cdr);
                } else {
                    persistenceService.markSubmitted(cdr, channelMsgId);
                }
                result.setOutMsgId(msgId);
                result.setStatus(true);
            } else {
                persistenceService.markFinalFailure(cdr, "submit rejected");
            }
        } catch (RcsCustomerAccessException e) {
            log.warn("rcs send customer request rejected, error:{}", e.getErrorCode());
        } catch (Exception e) {
            log.error("rcs send failed", e);
        }
        return result;
	}
	
	@Override
	public SubmitResponse sendAvantRCSMessage(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			String contentType = requestJson.getString("type");
			//String token = requestJson.getString("Token");
			AdminInfo adminInfo = GlobalMap.adminToken.get(token);
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = GlobalMap.routingTable.get(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setMobileType(contentType);
			/*
			 * if (!asyncService.isInfoBipRCSRegister(smsCdrInfo, rCustomer)) { return
			 * result; }
			 */		
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+rCustomer.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			result = asyncService.sendRCSMessageToAvant(smsCdrInfo, rCustomer);
			
		} catch (Exception e) {
			log.error("sendMessageToYulore throw error, the sms:{}", requestJson, e);
		}
		
		return result;
		
	}

	@Override
	public SubmitResponse sendMessageToYulore(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");
			if (!called.startsWith("+")) {
				called = "+"+called;
			}
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));
			if (!ThirdPartyLoginHelper.checkNum(rCustomer, called)) {
				return result;
			}
			
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+rCustomer.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			result = asyncService.sendMessageToYulore(smsCdrInfo, rCustomer);
			
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendMessageToYulore throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public SubmitResponse sendMessageToWABA(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));			
			if(msgBody.contains("\r\n")) {
				msgBody = msgBody.replaceAll("\\r\\n", " ");
			}
			if(msgBody.contains("\n")) {
				msgBody = msgBody.replaceAll("\\n", " ");
			}
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+adminInfo.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			smsCdrInfo.setRssid(rssid);
			result = asyncService.sendMessageToWABA(smsCdrInfo, rCustomer);
			
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendMessageToWABA throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public SubmitResponse sendZaloToMK(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));			
			
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+adminInfo.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			smsCdrInfo.setRssid(rssid);
			result = asyncService.sendZaloToMK(smsCdrInfo, rCustomer);
			
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendZaloToMK throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public SubmitResponse sendViberToMK(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));			
			
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+adminInfo.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			smsCdrInfo.setRssid(rssid);
			result = asyncService.sendViberToMK(smsCdrInfo, rCustomer);
			
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendMessageToWABA throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public SubmitResponse sendViberToTelSMS(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));			
			
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+adminInfo.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			smsCdrInfo.setRssid(rssid);
			result = asyncService.sendViberToTelSMS(smsCdrInfo, rCustomer);
			
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendMessageToWABA throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public SubmitResponse sendMessageToCMWABA(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);	
			if(StringUtils.isBlank(countrySN)) {
				String tt = PhoneUtils.getCountryCodeByPhoneStr(called);
				switch (tt) {
				case "212":
					countrySN="MA";
					break;
				default:
					break;
				}
			}
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));			
			
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setRssid(rssid);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+rCustomer.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			result = asyncService.sendMessageToCMWABA(smsCdrInfo, rCustomer);
			
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendMessageToWABA throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public SubmitResponse sendMessageToTelegram(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");			
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);	
			if(StringUtils.isBlank(countrySN)) {
				String tt = PhoneUtils.getCountryCodeByPhoneStr(called);
				switch (tt) {
				case "212":
					countrySN="MA";
					break;
				default:
					break;
				}
			}
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));	
			Map<String, String> map = new HashMap<>();
			String checkNum = "+"+called;
			map.put("phone_number", checkNum);
			Map<String, String> checkResultMap = ThirdPartyLoginHelper.checkTelegramNum(rCustomer, map);
			if (checkResultMap == null) {
				return result;
			}
			
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setRssid(rssid);
			smsCdrInfo.setMsgId(checkResultMap.get("requestId"));
			smsCdrInfo.setProvince(checkResultMap.get("requestCost"));	
			smsCdrInfo.setCity(checkResultMap.get("remainingBalance"));
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+rCustomer.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			result = asyncService.sendMessageToTelegram(smsCdrInfo, rCustomer);			
		} catch (Exception e) {
			log.error("sendMessageToTelegram throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public SubmitResponse sendMessageToMms(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));			
			
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setRssid(rssid);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+rCustomer.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			result = asyncService.sendMessageToMms(smsCdrInfo, rCustomer);
			
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendMessageToMms throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public SubmitResponse sendMYMMSToMK(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));			
			
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setRssid(rssid);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+rCustomer.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			result = asyncService.sendMyMmsToMK(smsCdrInfo, rCustomer);
			
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendMessageToMms throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public SubmitResponse sendMmsToMK(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));			
			
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			String outmsgId = MessageIdGenerator.generateCommonMsgID(Integer.parseInt(""+adminInfo.getId()));
			smsCdrInfo.setMsgId(outmsgId);
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			
			smsCdrInfo.setRssid(rssid);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+rCustomer.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			//result = asyncService.sendMmsToMK(smsCdrInfo, rCustomer);
			//save cdr info
			smsCdrInfo.setStatus(-1);
			smsCdrInfo.setContent(msgBody);
			String json = JSONObject.toJSONString(smsCdrInfo);
			//insert mms size to content for file szie.
			smsCdrInfo.setContent(""+(msgBody.length()-(msgBody.length()/8)*2));
			smsCdrInfoService.insertOrUpdate(smsCdrInfo);
			redisService.setList("mmsMsg", json);			
			result.setStatus(true);
			result.setOutMsgId(outmsgId);
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendMmsToMK throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	
	
	public SubmitResponse sendMessageToTeddy(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");
			if (!called.startsWith("+")) {
				called = "+"+called;
			}
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));			
			
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setRssid(rssid);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+rCustomer.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			result = asyncService.sendMessageToYulore(smsCdrInfo, rCustomer);
			
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendMessageToYulore throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public SubmitResponse sendMessageToSimBank(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");
			if (called.startsWith("+")) {
				called = called.substring(1);
			}
			String msgBody = requestJson.getString("Content");
			String contentType = requestJson.getString("Charset");
			/*if("ASCII".equalsIgnoreCase(contentType)){
				byte tc[] = Convert.convert2GSM(msgBody);
				msgBody = new String(tc);
			}*/			
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			List<Integer> rssids = routing.getRssidsByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssids == null || rssids.isEmpty()) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			if(accountIndex>=rssids.size()){
				accountIndex = 0;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssids.get(accountIndex)));
			SmsCdrInfo cdrInfo = new SmsCdrInfo();			
			cdrInfo.setContent(msgBody);
			cdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+rCustomer.getExpireTime()*1000;
			cdrInfo.setCreateTime(currentTime);			
			cdrInfo.setExpireTime(expireTime);
			cdrInfo.setSender(calling);
			cdrInfo.setMsisdn(called);
			String outmsgId = MessageIdGenerator.generateDecID(3);
			cdrInfo.setMsgId(outmsgId);	
			cdrInfo.setSsid(adminInfo.getSsid());
			cdrInfo.setRssid(rCustomer.getSsid());
			accountIndex++;
			//save cdr info
			cdrInfo.setStatus(-1);
			smsCdrInfoService.insertOrUpdate(cdrInfo);
			String json = JSONObject.toJSONString(cdrInfo);
			redisService.setList("simMsg", json);
			
			result.setStatus(true);
			result.setOutMsgId(outmsgId);
			
		} catch (Exception e) {
			log.error("sendMessageToSimbank throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}

	private List<SmsCdrInfo> domXml(String xml) {
		StringReader reader = null;
		try {
			reader = new StringReader(xml);
			InputSource source = new InputSource(reader);
			SAXBuilder sb = new SAXBuilder();
			Document doc = sb.build(source);
			Element root = doc.getRootElement();
			List node = root.getChildren();
			if (node == null) {
				return null;
			}
			Element et = null;
			String ssid="";
			List<SmsCdrInfo> messages = new ArrayList<>();
			for (int i = 0; i < node.size(); i++) {
				et = (Element) node.get(i);
				if ("AUTHENTICATION".equalsIgnoreCase(et.getName())) {
					ssid = et.getChild("PRODUCTTOKEN").getText();
				}else if ("MSG".equalsIgnoreCase(et.getName())){
					List<Element> childern = et.getChildren();
					SmsCdrInfo cdrInfo = new SmsCdrInfo();
					for(Element child: childern) {						
						if ("FROM".equalsIgnoreCase(child.getName())) {
							cdrInfo.setSender(child.getText());
						}else if ("TO".equalsIgnoreCase(child.getName())) {
							cdrInfo.setMsisdn(child.getText());
						}else if ("BODY".equalsIgnoreCase(child.getName())) {
							cdrInfo.setContent(child.getText());
						}						
					}
					messages.add(cdrInfo);
				}				
			}
			return messages;
		} catch (JDOMException e) {
			log.warn(" dom xml format error and exception is {}", e
					.getMessage());
			return null;
		} catch (Exception e) {
			log.warn("io exception{}", e.getMessage());
			return null;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
				}
			}
		}		
	}

	@Override
	public boolean receiveDRFromYulore(JSONObject requestJson) {
		try {
			int statusCode = requestJson.getIntValue("errCode");
			String msgId = requestJson.getString("msgId");
			long recvTm = requestJson.getLongValue("recvTm");
			SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgId));
			if (smsCdrInfo == null) {
				log.warn("No msg to mapping dr:{}", requestJson);
				return true;
			}
			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
			if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
				smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(recvTm,"yyyy-MM-dd HH:mm:ss", "GMT+8"));
				if (statusCode == 0 || statusCode == 8 ||statusCode == 4) {
					//success ||[DELIVERY_BUT_REPEATED] Delivered but duplicated
					//[B2C_BLOCK] Blocked
					smsCdrInfo.setStatus(3);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), msgId, 3);
				}else {
					smsCdrInfo.setStatus(4);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), msgId, 4);
				}
				this.updateById(smsCdrInfo);
			}else {
				log.warn("receiveDRFromYulore interface, the org msg expired, request:[{}], sms:[{}]", requestJson, smsCdrInfo);
			}
		} catch (Exception e) {
			log.error("receiveDRFromYulore throw error, the sms:{}", requestJson, e);
		}
		return true;
	}
	
	@Override
	public boolean receiveDRFromSIM(JSONObject requestJson) {
		try {
			JSONArray rpts = requestJson.getJSONArray("rpts");
			if(rpts!=null && !rpts.isEmpty()){
				for(int i=0; i< rpts.size(); i++){
					JSONObject jsonObj=rpts.getJSONObject(i);
					int msgId = jsonObj.getInteger("tid");
					int sending = jsonObj.getInteger("sending");
					int sent = jsonObj.getInteger("sent");
					int failed = jsonObj.getInteger("failed");
					int unsent = jsonObj.getInteger("unsent");
					SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgId));
					if (smsCdrInfo == null) {
						log.warn("No msg to mapping dr:{}", requestJson);
						return true;
					}
					AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
					if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
						smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
						if (sent >0 || sending >0 ) {
							//success ||[DELIVERY_BUT_REPEATED] Delivered but duplicated
							//[B2C_BLOCK] Blocked
							smsCdrInfo.setStatus(0);
							//ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 3);
							//TODO waiting DR
						}else {
							smsCdrInfo.setStatus(4);
							ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 4);
						}
						this.updateById(smsCdrInfo);
					}else {
						log.warn("receiveDRFromSIM interface, the org msg expired, request:[{}], sms:[{}]", requestJson, smsCdrInfo);
					}
				}
			}						
			
		} catch (Exception e) {
			log.error("receiveDRFromYulore throw error, the sms:{}", requestJson, e);
		}
		return true;
	}
	
	@Override
	public boolean receiveDRFromMMS(String msgid, String status) {
		try {
			SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgid));
			if (smsCdrInfo == null) {
				log.warn("No msg to mapping dr:{}", msgid);
				return true;
			}
			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
			if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
				smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
				if ("DELIVERED".equalsIgnoreCase(status)) {
					smsCdrInfo.setStatus(3);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), msgid, 3);
				}else {
					smsCdrInfo.setStatus(4);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgid, 4);
				}
				this.updateById(smsCdrInfo);
			}else {
				log.warn("receiveDRFromMMS interface, the org msg expired, request:[{}], sms:[{}]", msgid, smsCdrInfo);
			}
		} catch (Exception e) {
			log.error("receiveDRFromMMS throw error, the sms:{}", msgid, e);
		}
		return true;
	}
	
	@Override
	public boolean receiveMMSDRFromMK(String msgid, String status) {
		try {
			SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgid));
			if (smsCdrInfo == null) {
				log.warn("No msg to mapping dr:{}", msgid);
				return true;
			}
			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
			if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
				smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
				if ("DELIVRD".equalsIgnoreCase(status)) {
					smsCdrInfo.setStatus(3);
					ThirdPartyLoginHelper.sendDRWithDAToSMSGW(adminInfo.getUrl(), msgid, 3, smsCdrInfo.getMsisdn());
				}else {
					smsCdrInfo.setStatus(4);
					ThirdPartyLoginHelper.sendDRWithDAToSMSGW(adminInfo.getUrl(), ""+msgid, 4, smsCdrInfo.getMsisdn());
				}
				this.updateById(smsCdrInfo);
			}else {
				log.warn("receiveMMSDRFromMK interface, the org msg expired, request:[{}], sms:[{}]", msgid, smsCdrInfo);
			}
		} catch (Exception e) {
			log.error("receiveMMSDRFromMK throw error, the sms:{}", msgid, e);
		}
		return true;
	}
	
	@Override
	public boolean receiveDRFromWABA(JSONObject requestJson) {
		try {

			JSONObject jsonObj=requestJson.getJSONObject("message_status");
			if(jsonObj==null || jsonObj.isEmpty()){
				return true;
			}
			String msgId = jsonObj.getString("message_id");
			String status = jsonObj.getString("status");	
			BillingCdrInfo billingcdr = billingCdrInfoService.selectBillingCdr(msgId);
			int drstatus = 0;
			if("ACCEPTED".equalsIgnoreCase(status) 
					||"SENT".equalsIgnoreCase(status)){
				log.warn("receive waba msg {} temp status {}, so ignore", msgId, status);
				drstatus=8;
			}
			if ("DELIVERED".equalsIgnoreCase(status)
					||"READ".equalsIgnoreCase(status)) {
				
				drstatus=3;
				//TODO waiting DR
			}else if ("UNDELIVERED".equalsIgnoreCase(status)) {
				drstatus= 4;
			}
			
			if(billingcdr!=null && billingcdr.getId()>0){
				billingcdr.setRstatus(drstatus);
				billingCdrInfoService.updateById(billingcdr);
			}
			SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgId));
			if (smsCdrInfo == null) {
				log.warn("No msg to mapping dr:{}", requestJson);
				return true;
			}
			if("ACCEPTED".equalsIgnoreCase(status) 
					||"SENT".equalsIgnoreCase(status)){
				log.warn("receive waba msg {} temp status {}, so ignore", msgId, status);
				return true;
			}
			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
			if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
				smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
				if ("ACCEPTED".equalsIgnoreCase(status) 
						||"SENT".equalsIgnoreCase(status)
						||"DELIVERED".equalsIgnoreCase(status)
						||"READ".equalsIgnoreCase(status)) {
					//ACCEPTED ||SENT ||UNDELIVERED|DELIVERED|READ
					//[B2C_BLOCK] Blocked
					smsCdrInfo.setStatus(3);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 3);
					//TODO waiting DR
				}else {
					smsCdrInfo.setStatus(4);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 4);
				}
				this.updateById(smsCdrInfo);
			}else {
				log.warn("receiveDRFromWABA interface, the org msg expired, request:[{}], sms:[{}]", requestJson, smsCdrInfo);
			}
								
			
		} catch (Exception e) {
			log.error("receiveDRFromWABA throw error, the sms:{}", requestJson, e);
		}
		return true;
	}
	
	@Override
	public boolean receiveDRFromCMWABA(JSONObject requestJson) {
		/*{
	        "messages": {
	            "msg": {
	                "received": "[CREATED_S]",
	                "to": "[GSM]",
	                "reference": "[REFERENCE]",
	                "status": {
	                    "code": "[STATUS]",
	                    "errorCode": "[STATUSDESCRIPTION]",
	                    "errorDescription": "[STANDARDERRORTEXT]"
	                },
	                "operator": "[OPERATOR]"
	            }
	        }
	     }*/
		try {

			JSONObject jsonObj=requestJson.getJSONObject("messages");
			if(jsonObj==null || jsonObj.isEmpty()){
				return true;
			}
			JSONObject msg = jsonObj.getJSONObject("msg");
			if(msg==null || msg.isEmpty()){
				return true;
			}
			String msgId = msg.getString("reference");
			JSONObject status = msg.getJSONObject("status");
			if(status==null || status.isEmpty()){
				return true;
			}
			String code = status.getString("code");	
			BillingCdrInfo billingcdr = billingCdrInfoService.selectBillingCdr(msgId);
			int drstatus = 0;
			if ("2".equalsIgnoreCase(code) 
					||"4".equalsIgnoreCase(code)) {
				/*0 = accepted by the operator
						1 = rejected by CM or operator
						2 = delivered
						3 = failed (message was not and will not be delivered)
						4 = Read (Not for SMS, only other channels)*/
				drstatus=3;
				//TODO waiting DR
			}else {
				drstatus=4;
			}
			
			if(billingcdr!=null && billingcdr.getId()>0){
				billingcdr.setRstatus(drstatus);
				billingCdrInfoService.updateById(billingcdr);
			}
			SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgId));
			if (smsCdrInfo == null) {
				log.warn("No msg to mapping dr:{}", requestJson);
				return true;
			}
			if("0".equalsIgnoreCase(code)){
				log.warn("receive waba msg {} temp status {}, so ignore", msgId, status);
				return true;
			}
			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
			if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
				smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
				if ("2".equalsIgnoreCase(code) 
						||"4".equalsIgnoreCase(code)) {
					/*0 = accepted by the operator
							1 = rejected by CM or operator
							2 = delivered
							3 = failed (message was not and will not be delivered)
							4 = Read (Not for SMS, only other channels)*/
					smsCdrInfo.setStatus(3);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 3);
					//TODO waiting DR
				}else {
					smsCdrInfo.setStatus(4);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 4);
				}
				this.updateById(smsCdrInfo);
			}else {
				log.warn("receiveDRFromCMWABA interface, the org msg expired, request:[{}], sms:[{}]", requestJson, smsCdrInfo);
			}
								
			
		} catch (Exception e) {
			log.error("receiveDRFromWABA throw error, the sms:{}", requestJson, e);
		}
		return true;
	}
	
	
	@Override
	public int existTable(String tableName) {
		// TODO Auto-generated method stub
		return smsCdrInfoMapper.existTable(tableName);
	}

	@Override
	public int dropTable(String tableName) {
		// TODO Auto-generated method stub
		return smsCdrInfoMapper.dropTable(tableName);
	}

	@Override
	public int createNewTable(String tableName) {
		// TODO Auto-generated method stub
		return smsCdrInfoMapper.createNewTable(tableName);
	}

	@Override
	public int insertCdr(String tableName, SmsCdrInfo cdr) {
		// TODO Auto-generated method stub		
		return smsCdrInfoMapper.insertCdr(tableName, cdr);
	}

	@Override
	public boolean receiveDRFromSIMBox(String send, String reciver) {
		// TODO Auto-generated method stub
		log.info("receive sim dr msg: sender:{}, receiver:{}", send, reciver);
		SmsCdrInfo cdr = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msisdn", reciver).eq("status", 0).orderBy("createTime", true).last("limit 1"));
		if(cdr != null){
			cdr.setStatus(3);
			AdminInfo oCustomer = GlobalMap.admin.get(String.valueOf(cdr.getSsid()));
			if(oCustomer!=null){
				ThirdPartyLoginHelper.sendDRToSMSGW(oCustomer.getUrl(), ""+cdr.getMsgId(), 3);
			}else {
				log.error("can't found o customer of ssid {} to send dr", cdr.getSsid());
			}
			this.updateById(cdr);
		}		
		return true;
	}

	@Override
	public boolean receiveMsgFromSIM(String send, String reciver, String content) {
		// TODO Auto-generated method stub
		log.info("receive sim msg: sender:{}, receiver:{}, content:{}", send, reciver, content);
		return false;
	}

	@Override
	public boolean receiveDRFromTTS(String msgid, String status) {
		try {
			SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgid));
			if (smsCdrInfo == null) {
				log.warn("No msg to mapping dr:{}", msgid);
				return true;
			}
			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
			if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
				
				smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
				if("3".equalsIgnoreCase(status)) {
					smsCdrInfo.setStatus(3);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), msgid, 3);
				}else {
					smsCdrInfo.setStatus(4);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), msgid, 4);
				}
				
				
				this.updateById(smsCdrInfo);
			}else {
				log.warn("receiveDRFromTTS interface, the org msg expired, request:[{}], sms:[{}]", msgid, smsCdrInfo);
			}
		} catch (Exception e) {
			log.error("receiveDRFromTTS throw error, the sms:{}", msgid, e);
		}
		return true;
	}
	
	@Override
	public SubmitResponse sendTTS(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));			
			
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			String outmsgId = MessageIdGenerator.generateCommonMsgID(Integer.parseInt(""+adminInfo.getId()));
			smsCdrInfo.setMsgId(outmsgId);
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			
			smsCdrInfo.setRssid(rssid);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+rCustomer.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			//result = asyncService.sendMmsToMK(smsCdrInfo, rCustomer);
			//save cdr info
			smsCdrInfo.setStatus(-1);
			smsCdrInfo.setContent(msgBody);
			String json = JSONObject.toJSONString(smsCdrInfo);
			//insert mms size to content for file szie.
			smsCdrInfoService.insertOrUpdate(smsCdrInfo);
			redisService.setList("ttsMsg_"+rssid, json);			
			result.setStatus(true);
			result.setOutMsgId(outmsgId);
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendMmsToMK throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	public static void main(String[] args) {
		String countrySN = "abaddare\ndadaf\r\ndaetae";
		if(countrySN.contains("\r\n")) {
			countrySN = countrySN.replaceAll("\\r\\n", " ");
		}
		if(countrySN.contains("\n")) {
			countrySN = countrySN.replaceAll("\\n", " ");
		}
		System.out.println(countrySN);
	}

	@Override
	public SubmitResponse sendA2PMessage(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));			
			if(msgBody.contains("\r\n")) {
				msgBody = msgBody.replaceAll("\\r\\n", "<br>");
			}
			if(msgBody.contains("\n")) {
				msgBody = msgBody.replaceAll("\\n", "<br>");
			}
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called.substring(2));
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+adminInfo.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			smsCdrInfo.setRssid(rssid);
			result = asyncService.sendA2PMessage(smsCdrInfo, rCustomer);
			
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendA2PMessage throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public SubmitResponse sendBraxisVoiceMessage(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));						
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called.substring(2));
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+adminInfo.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			smsCdrInfo.setRssid(rssid);
			result = asyncService.sendVoiceMsgToBraxis(smsCdrInfo, rCustomer);
			
			
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendA2PMessage throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}

	@Override
	public boolean receiveDRFromFCV(String msgid, String status, String phone) {
		try {
			
			SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgid));
			if (smsCdrInfo == null) {
				log.warn("No msg to mapping dr:{}", msgid);
				return true;
			}
			if("1".equalsIgnoreCase(status) 
					||"3".equalsIgnoreCase(status)
					||"3".equalsIgnoreCase(status)){
				log.warn("receive receiveDRFromFCV msg {} temp status {}, so ignore", msgid, status);
				return true;
			}
			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
			if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
				smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
				if ("4".equalsIgnoreCase(status) ||"9".equalsIgnoreCase(status) ) {
					//1 - msg in queue
					//2 - message scheduled
					//3 - sending
					//4 or 9 - message sent to carrier
					//5 - error found in carrier
					//[B2C_BLOCK] Blocked
					smsCdrInfo.setStatus(3);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgid, 3);
					//TODO waiting DR
				}else {
					smsCdrInfo.setStatus(4);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgid, 4);
				}
				this.updateById(smsCdrInfo);
			}else {
				log.warn("receiveDRFromFCV interface, the org msg expired, request:[{}], sms:[{}]", msgid, smsCdrInfo);
			}
								
			
		} catch (Exception e) {
			log.error("receiveDRFromFCV throw error, the sms:{}", msgid, e);
		}
		return true;
	}

	@Override
	public SubmitResponse sendOTTMessage(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));						
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+adminInfo.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			smsCdrInfo.setRssid(rssid);
			result = asyncService.sendOTTMsgToBRAX(smsCdrInfo, rCustomer);						
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendOTTMessage throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public SubmitResponse sendAvantMessage(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));						
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+adminInfo.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			smsCdrInfo.setRssid(rssid);
			result = asyncService.sendMsgToAvant(smsCdrInfo, rCustomer);						
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendAvantMessage throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}

	@Override
	public boolean receiveDRFromTelegram(JSONObject requestJson) {
		/*result": {
	        "request_id": "107583108491530",
	        "phone_number": "8618500375454",
	        "request_cost": 0,
	        "remaining_balance": 0,
	        "delivery_status": {
	            "status": "sent",
	            "updated_at": 1733367318
	        }
	       }*/
		try {						
			String msgId = requestJson.getString("request_id");
			JSONObject json = requestJson.getJSONObject("delivery_status");			
			String status = json.getString("status");					
			BillingCdrInfo billingcdr = billingCdrInfoService.selectBillingCdr(msgId);
			int drstatus = 3;			
			if(billingcdr!=null && billingcdr.getId()>0){
				billingcdr.setRstatus(drstatus);
				billingCdrInfoService.updateById(billingcdr);
			}
			SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgId));
			if (smsCdrInfo == null) {
				log.warn("No msg to mapping dr:{}", requestJson);
				return true;
			}
			
			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
			if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
				smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
				if ("sent".equalsIgnoreCase(status) || "read".equalsIgnoreCase(status)){					
					smsCdrInfo.setStatus(3);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 3);
					//TODO waiting DR
				}else {
					smsCdrInfo.setStatus(4);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 4);
				}
				this.updateById(smsCdrInfo);
			}else {
				log.warn("receiveDRFromTelegram interface, the org msg expired, request:[{}], sms:[{}]", requestJson, smsCdrInfo);
			}
								
			
		} catch (Exception e) {
			log.error("receiveDRFromTelegram throw error, the sms:{}", requestJson, e);
		}
		return true;
	}
	
	@Override
	public boolean receiveDRFromInfoRCS(JSONObject requestJson) {
		/*{"results": [
			    {
			      "bulkId": "string",
			      "price": {
			        "pricePerMessage": 0,
			        "currency": "string"
			      },
			      "status": {
			        "groupId": 0,
			        "groupName": "ACCEPTED",
			        "id": 0,
			        "name": "string",
			        "description": "string",
			        "action": "string"
			      },
			      "error": {
			        "groupId": 0,
			        "groupName": "OK",
			        "id": 0,
			        "name": "string",
			        "description": "string",
			        "permanent": true
			      },
			      "messageId": "string",
			      "to": "string",
			      "sender": "string",
			      "sentAt": "2024-12-31T03:53:23Z",
			      "doneAt": "2024-12-31T03:53:23Z",
			      "messageCount": 0,
			      "mccMnc": "string",
			      "callbackData": "string",
			      "platform": {
			        "entityId": "string",
			        "applicationId": "string"
			      },
			      "campaignReferenceId": "string"
			    }
			  ]
			}*/
		try {	
			JSONArray jsonArray = requestJson.getJSONArray("results");
			if(jsonArray!=null) {
				for(int i = 0; i<jsonArray.size(); i++) {
					JSONObject obj = jsonArray.getJSONObject(i);
					JSONObject statusObj = obj.getJSONObject("status");
					String status = statusObj.getString("groupName");
					//PENDING, UNDELIVERABLE, DELIVERED, EXPIRED, REJECTED,ACCEPTED
					if("PENDING".equalsIgnoreCase(status) || "ACCEPTED".equalsIgnoreCase(status)) {
						continue;
					}
					int drstatus = 4;
					if("DELIVERED".equalsIgnoreCase(status)) {
						drstatus = 3;
					}
					String msgId = obj.getString("messageId");
					BillingCdrInfo billingcdr = billingCdrInfoService.selectBillingCdr(msgId);
								
					if(billingcdr!=null && billingcdr.getId()>0){
						billingcdr.setRstatus(drstatus);
						billingCdrInfoService.updateById(billingcdr);
					}
					SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgId));
					if (smsCdrInfo == null) {
						log.warn("No msg to mapping dr:{}", requestJson);
						continue;
					}
					
					AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
					if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
						smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
						if ("sent".equalsIgnoreCase(status) || "read".equalsIgnoreCase(status)){					
							smsCdrInfo.setStatus(3);
							ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 3);
							//TODO waiting DR
						}else {
							smsCdrInfo.setStatus(4);
							ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 4);
						}
						this.updateById(smsCdrInfo);
					}else {
						log.warn("receiveDRFromInfoRCS interface, the org msg expired, request:[{}], sms:[{}]", requestJson, smsCdrInfo);
					}
				}
			}		
			
		} catch (Exception e) {
			log.error("receiveDRFromInfoRCS throw error, the sms:{}", requestJson, e);
		}
		return true;
	}	
	
	@Override
	public boolean receiveDRFromAvantRCS(JSONObject requestJson) {
		
		try {						
			String status = requestJson.getString("status");
			//PENDING, UNDELIVERABLE, DELIVERED, EXPIRED, REJECTED,ACCEPTED
			if("SENDED".equalsIgnoreCase(status)) {
				return false;
			}
			int drstatus = 4;
			if("DELIVERED".equalsIgnoreCase(status)) {
				drstatus = 3;
			}
			String msgId = requestJson.getString("key");
			BillingCdrInfo billingcdr = billingCdrInfoService.selectBillingCdr(msgId);
						
			if(billingcdr!=null && billingcdr.getId()>0){
				billingcdr.setRstatus(drstatus);
				billingCdrInfoService.updateById(billingcdr);
			}
			SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgId));
			if (smsCdrInfo == null) {
				log.warn("No msg to mapping dr:{}", requestJson);
				return false;
			}
			
			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
			if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
				smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
				if ("DELIVRD".equalsIgnoreCase(status) || "READED".equalsIgnoreCase(status)
						|| "CLICKED".equalsIgnoreCase(status)){					
					smsCdrInfo.setStatus(3);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 3);
					//TODO waiting DR
				}else {
					smsCdrInfo.setStatus(4);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 4);
				}
				this.updateById(smsCdrInfo);
			}else {
				log.warn("receiveDRFromAvantRCS interface, the org msg expired, request:[{}], sms:[{}]", requestJson, smsCdrInfo);
			}
				
					
			
		} catch (Exception e) {
			log.error("receiveDRFromAvantRCS throw error, the sms:{}", requestJson, e);
		}
		return true;
	}
	
	@Override
	public boolean receiveDRFromTelSMSViber(JSONObject requestJson) {
		
		try {						
			String status = requestJson.getString("state");
			//PENDING, UNDELIVERABLE, DELIVERED, EXPIRED, REJECTED,ACCEPTED
			/*
			 * if("16".equalsIgnoreCase(status)) { return false; }
			 */
			int drstatus = 4;
			if("16".equalsIgnoreCase(status)) {
				drstatus = 3;
			}
			String msgId = requestJson.getString("sendCode");
			BillingCdrInfo billingcdr = billingCdrInfoService.selectBillingCdr(msgId);
						
			if(billingcdr!=null && billingcdr.getId()>0){
				billingcdr.setRstatus(drstatus);
				billingCdrInfoService.updateById(billingcdr);
			}
			SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgId));
			if (smsCdrInfo == null) {
				log.warn("No msg to mapping dr:{}", requestJson);
				return false;
			}
			
			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
			if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
				smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
				if ("16".equalsIgnoreCase(status)){					
					smsCdrInfo.setStatus(3);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 3);
					//TODO waiting DR
				}else {
					smsCdrInfo.setStatus(4);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 4);
				}
				this.updateById(smsCdrInfo);
			}else {
				log.warn("receiveDRFromTelSMSViber interface, the org msg expired, request:[{}], sms:[{}]", requestJson, smsCdrInfo);
			}
				
					
			
		} catch (Exception e) {
			log.error("receiveDRFromTelSMSViber throw error, the sms:{}", requestJson, e);
		}
		return true;
	}
	
	@Override
	public boolean receiveDRFromBraxisVoice(JSONObject requestJson) {
		
		try {						
			String status = requestJson.getString("status");
			//PENDING, UNDELIVERABLE, DELIVERED, EXPIRED, REJECTED,ACCEPTED
			/*
			 * if("16".equalsIgnoreCase(status)) { return false; }
			 */			
			String msgId = requestJson.getString("otp_uuid");
			
			SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgId));
			if (smsCdrInfo == null) {
				log.warn("No msg to mapping dr:{}", requestJson);
				return false;
			}
			
			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
			if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
				smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
				if ("Success".equalsIgnoreCase(status)){					
					smsCdrInfo.setStatus(3);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 3);
					//TODO waiting DR
				}else {
					smsCdrInfo.setStatus(4);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 4);
				}
				this.updateById(smsCdrInfo);
			}else {
				log.warn("receiveDRFromTelSMSViber interface, the org msg expired, request:[{}], sms:[{}]", requestJson, smsCdrInfo);
			}
				
					
			
		} catch (Exception e) {
			log.error("receiveDRFromTelSMSViber throw error, the sms:{}", requestJson, e);
		}
		return true;
	}
	
	@Override
	public boolean receiveDRFromAvant(JSONObject requestJson) {
		/*{
			"token": "xZxZxZxZxZxZxZxZxZ",
			"status": [
		        {
					"status": "C",
					"nro_telefone": "5599999999999",
					"serial": "Vb75rFvG",
					"id_campanha": "Vb7ss",
					"des_historico_dlr": "",
					"operadora": "VIVO",
		            "status_detalhe": ""
				},
				{
					"status": "C",
					"nro_telefone": "5599999999999",
					"serial": "Vb75rFvG",
					"id_campanha": "Vb7ss",
					"des_historico_dlr": "",
					"operadora": "VIVO",
		            "status_detalhe": ""
		}*/
		try {						
			JSONArray drs = requestJson.getJSONArray("status");
			if(drs!=null && !drs.isEmpty()) {
				for (int i =0; i<drs.size(); i++) {
					JSONObject json = drs.getJSONObject(i);
					String status = json.getString("status");
					String msgid = json.getString("serial");
					String ssid = msgid.split("_")[0];
					AdminInfo adminInfo = GlobalMap.admin.get(ssid);
					if(adminInfo!=null) {
						if("A".equalsIgnoreCase(status) 
								||"M".equalsIgnoreCase(status) 
								|| "H".equalsIgnoreCase(status) 
								|| "R".equalsIgnoreCase(status)
								|| "B".equalsIgnoreCase(status) ) {
							continue;
						}
						if ("C".equalsIgnoreCase(status) || "O".equalsIgnoreCase(status)){												
							ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), msgid, 3);
							//TODO waiting DR
						}else {							
							ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), msgid, 4);
						}
					}
				}
			}														
		} catch (Exception e) {
			log.error("receiveDRFromAvant throw error, the sms:{}", requestJson, e);
		}
		return true;
	}
	
	
	@Override
	public boolean receiveDRFromBrax(JSONObject requestJson) {
		/*{

			“msisdn”: “8493670****”,
			
			“requestId”: null,
			
			“sendTime”: “20/02/2023 14:06:58”,
			
			“responseTimeTelco”: “20/02/2023 14:06:34”,
			
			“status”: 2,
			
			“referentId”: null,
			
			“retryCount”: 0,
			
			“callbackType”: “TelcoCallback”

         }*/
		try {
			
			String callbackType = requestJson.getString("callbackType");
			if(StringUtils.isBlank(callbackType)|| !"TelcoCallback".equalsIgnoreCase(callbackType)){
				return true;
			}
			
			String msgId = requestJson.getString("referentId");
			int status = requestJson.getIntValue("status");		
			if(status == 0 || status ==1 ) {
				return true;
			}
			BillingCdrInfo billingcdr = billingCdrInfoService.selectBillingCdr(msgId);
			int drstatus = 0;
			if (status ==2) {
				/*0: message waiting for approval (on hold for advertising content),
				1: has been approved,  
				-1: denied approval or made an error when checking information
				2: Send telco successful,
				-2: Send telco fail.
				*/
				drstatus=3;
				//TODO waiting DR
			}else {
				drstatus=4;
			}
			
			if(billingcdr!=null && billingcdr.getId()>0){
				billingcdr.setRstatus(drstatus);
				billingCdrInfoService.updateById(billingcdr);
			}
			SmsCdrInfo smsCdrInfo = this.selectOne(new EntityWrapper<SmsCdrInfo>().eq("msg_id", msgId));
			if (smsCdrInfo == null) {
				log.warn("No msg to mapping dr:{}", requestJson);
				return true;
			}
			
			AdminInfo adminInfo = GlobalMap.admin.get(""+smsCdrInfo.getSsid());
			if (adminInfo!=null && smsCdrInfo!=null && !smsCdrInfo.getStatus().equals(5)) {
				smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
				if (status ==2){
					/*0 = accepted by the operator
							1 = rejected by CM or operator
							2 = delivered
							3 = failed (message was not and will not be delivered)
							4 = Read (Not for SMS, only other channels)*/
					smsCdrInfo.setStatus(3);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 3);
					//TODO waiting DR
				}else {
					smsCdrInfo.setStatus(4);
					ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), ""+msgId, 4);
				}
				this.updateById(smsCdrInfo);
			}else {
				log.warn("receiveDRFromBrax interface, the org msg expired, request:[{}], sms:[{}]", requestJson, smsCdrInfo);
			}
								
			
		} catch (Exception e) {
			log.error("receiveDRFromBrax throw error, the sms:{}", requestJson, e);
		}
		return true;
	}
	
	@Override
	public SubmitResponse sendCMIMessage(JSONObject requestJson) {
		SubmitResponse result = new SubmitResponse();
		try {
			String calling = requestJson.getString("Sender");
			String called = requestJson.getString("Recipient");			
			String msgBody = requestJson.getString("Content");
			String token = requestJson.getString("Token");
			AdminInfo adminInfo = adminInfoService.selectOne(new EntityWrapper<AdminInfo>().eq("token", token));
			if (adminInfo == null) {
				log.error("can not found the ocustomer info, the sms:{}", requestJson);
				return result;
			}
			String countrySN = PhoneUtils.getRegionCodeByPhone(called);
			int rssid = routing.getRssidByOssidAndCountryCode(Integer.parseInt(""+adminInfo.getId()), countrySN);
			if (rssid==0) {
				log.error("can not found the rcustomer info, the sms:{}, countrySN:{}", requestJson, countrySN);
				return result;
			}
			
			AdminInfo rCustomer = GlobalMap.admin.get(String.valueOf(rssid));						
			SmsCdrInfo smsCdrInfo = new SmsCdrInfo();
			smsCdrInfo.setSsid(Integer.parseInt(""+adminInfo.getSsid()));
			smsCdrInfo.setSender(calling);
			smsCdrInfo.setMsisdn(called);
			smsCdrInfo.setContent(msgBody);
			smsCdrInfo.setTimemark(QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(System.currentTimeMillis(),"yyyy-MM-dd HH:mm:ss", "GMT+8"));
			long currentTime = System.currentTimeMillis();
			long expireTime = currentTime+adminInfo.getExpireTime()*1000;
			smsCdrInfo.setCreateTime(currentTime);			
			smsCdrInfo.setExpireTime(expireTime);
			smsCdrInfo.setRssid(rssid);
			result = asyncService.sendMsgToCMI(smsCdrInfo, rCustomer);						
			//result="yulore123456";
		} catch (Exception e) {
			log.error("sendAvantMessage throw error, the sms:{}", requestJson, e);
		}
		
		return result;
	}
	
	@Override
	public Map<String, Object> receiveDRFromCMI(JSONObject requestJson) {
		/*{
		     "uip_head": {
		          "METHOD": "SMS_PUSH_REPORT",
		          "SERIAL": 1,
		          "TIME": "2023-01-01T12:00:00Z",
		          "CHANNEL": "CMI",# Please use your tenant name, which is assigned by CloudSMS 
		          "AUTH_KEY": "AUTH_KEY corresponding to Test"
		     },
		     "uip_body": {
		          "SMS_UID": "UIP00000000001",
		          "DESTINATION_ADDR": "8618888888888",
		          "ORIGINAL_ADDR": "CMI",
		          "STATUS": "DELIVERED",
		          "COUNT": 5
		     },
		     "uip_version": 2
		}
		*/
		/**
		 * {
			     "uip_version": 2,
			     "uip_head": {
			          "RESULT_CODE": 1,
			          "RESULT_DESC": "OK",
			          "METHOD": "SMS_PUSH_REPORT",
			          "CHANNEL": "CMI",
			          "SERIAL": 1
			     },
			     "uip_body": {
			           "SMS_UID": "UIP00000000001"
			      }
			}

		 */
		int SERIAL = 1;
		String msgid = "";
		Map<String, Object> response = new HashMap<String, Object>();					
		Map<String, Object> uipHead = new HashMap<String, Object>();
		try {			
			JSONObject json = requestJson.getJSONObject("uip_body");
			if(json!=null && !json.isEmpty()) {				
				SERIAL = json.getIntValue("SERIAL");
				String status = json.getString("STATUS");				
				msgid = json.getString("CUSTOMER_BODY");				
				String ssid = msgid.split("_")[0];				
				AdminInfo adminInfo = GlobalMap.admin.get(ssid);
				if(adminInfo!=null) {
					if("ACCEPTED".equalsIgnoreCase(status) || "ENROUTE".equalsIgnoreCase(status)) {
						response.put("uip_head", uipHead);
						uipHead.put("RESULT_CODE", 1);
						uipHead.put("RESULT_DESC", "OK");
						uipHead.put("METHOD", "SMS_PUSH_REPORT");
						uipHead.put("CHANNEL", GlobalMap.getCMICount());
						uipHead.put("SERIAL", SERIAL);
						
						Map<String, Object> uipbody = new HashMap<String, Object>();
						response.put("uip_body", uipbody);
						uipbody.put("SMS_UID", msgid);		
						response.put("uip_version", 2);
						return response;
					}
					if ( "DELIVERED".equalsIgnoreCase(status)){												
						ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), msgid, 3);
						//TODO waiting DR
					}else {							
						ThirdPartyLoginHelper.sendDRToSMSGW(adminInfo.getUrl(), msgid, 4);
					}
				}
			
			}														
		} catch (Exception e) {
			log.error("receiveDRFromCMI throw error, the sms:{}", requestJson, e);
		}
		
		response.put("uip_head", uipHead);
		uipHead.put("RESULT_CODE", 1);
		uipHead.put("RESULT_DESC", "OK");
		uipHead.put("METHOD", "SMS_PUSH_REPORT");
		uipHead.put("CHANNEL", GlobalMap.getCMICount());
		uipHead.put("SERIAL", SERIAL);
		
		Map<String, Object> uipbody = new HashMap<String, Object>();
		response.put("uip_body", uipbody);
		uipbody.put("SMS_UID", msgid);		
		response.put("uip_version", 2);
		return response;
	}
	
	private BillingCdrInfo buildBilling(RCSCdrInfo cdr, int ostatus) {
        BillingCdrInfo billing = new BillingCdrInfo();
        billing.setSsid(cdr.getSsid());
        billing.setRssid(cdr.getRssid());
        billing.setMsgId(cdr.getMsgId());
        billing.setMsisdn(cdr.getMsisdn());
        billing.setSender(cdr.getSender());
        billing.setContent(cdr.getContent());
        billing.setOstatus(ostatus);
        billing.setRstatus(-1);
        billing.setMobileType("RCS:" + cdr.getMsgType());
        billing.setCreateTime(cdr.getCreateTime());
        billing.setTimemark(cdr.getTimemark());
        billing.setInsubmittime(cdr.getInsubmittime());
        return billing;
    }
	
	private String buildNotifyUrl(String channelCode) {
        String base = projectUrl;
        if (base != null && !base.endsWith("/")) {
            base = base + "/";
        }
        return base + "rcs/callback/" + channelCode.toLowerCase();
    }
	
	private static Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }
	
}

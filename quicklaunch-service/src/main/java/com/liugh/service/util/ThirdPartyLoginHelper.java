package com.liugh.service.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.plugins.Page;
import com.liugh.base.ChargeData;
import com.liugh.base.ChargeResponse;
import com.liugh.base.Charges;
import com.liugh.base.DailyReport;
import com.liugh.base.EnumberDTO;
import com.liugh.base.LatencyReport;
import com.liugh.base.MonitorCountryReport;
import com.liugh.base.MonitorDuplicateNumResponse;
import com.liugh.base.MonitorLatencyDistributeResponse;
import com.liugh.base.MonitorLatencyReport;
import com.liugh.base.MonitorReport;
import com.liugh.base.MonitorRequest;
import com.liugh.base.OperatorReportResponse;
import com.liugh.base.ProvinceReportWithOPResponse;
import com.liugh.base.Report;
import com.liugh.base.SMSLogging;
import com.liugh.base.SMSLoggingField;
import com.liugh.base.SMSLoggingRequestVo;
import com.liugh.base.TexterJsonResult;
import com.liugh.base.TexterResponse;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RCSCdrInfo;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.model.ThirdPartyUser;
import com.liugh.util.HttpUtil;
import com.liugh.util.QuotaManagerUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 第三方登录辅助类
 *
 * @author liugh
 * @version 2018年7月25日 下午3:44:45
 */
public final class ThirdPartyLoginHelper {

    private static Logger logger = LoggerFactory.getLogger(ThirdPartyLoginHelper.class);
    
    private static String yuloreToken="";
    private static final Base64.Encoder encoder = Base64.getEncoder();
    private static int mmsIndex= 0;

    
    public static final int rigster2BillingSystem(String customerName, String email, String host) {
        JSONObject json = null;
        try {           
        	HttpPost httpPost = new HttpPost(host);
        	CloseableHttpClient client = HttpClients.createDefault();
        	JSONObject jsonParam = new JSONObject();  
        	jsonParam.put("customer_name", customerName);
        	jsonParam.put("contact_mail_address", email);
        	jsonParam.put("subscript_service", "smsa2p");
        	StringEntity entity = new StringEntity(jsonParam.toString(),"UTF-8");//解决中文乱码问题    
        	entity.setContentEncoding("UTF-8");    
        	entity.setContentType("application/json");    
        	httpPost.setEntity(entity);
            HttpResponse resp = client.execute(httpPost);
            if(resp.getStatusLine().getStatusCode() == 200) {
                 HttpEntity he = resp.getEntity();
                 String response = EntityUtils.toString(he,"UTF-8");
                 logger.info("register billing system response is:"+response);
                 if (response != null && response.indexOf("ssid") > -1) {
                     json = JSON.parseObject(response);
                     return Integer.parseInt(String.valueOf(json.get("ssid")));
                 } else {
                     return 0;
                 }
            }
        } catch (Exception e) {
            logger.error("", e);
        }        
        return 0;
    }
    
    public static final boolean addQuota(int ssid, String host, String quota) {
        try {           
        	HttpPost httpPost = new HttpPost(host);
        	CloseableHttpClient client = HttpClients.createDefault();
        	JSONObject jsonParam = new JSONObject();  
        	jsonParam.put("ssid", ssid);
        	jsonParam.put("quota", quota);
        	logger.info("addQuota request, ssid:{}, quota:{}",ssid, quota);
        	StringEntity entity = new StringEntity(jsonParam.toString(),"UTF-8");//解决中文乱码问题    
        	entity.setContentEncoding("UTF-8");    
        	entity.setContentType("application/json");    
        	httpPost.setEntity(entity);
            HttpResponse resp = client.execute(httpPost);
            if(resp.getStatusLine().getStatusCode() == 200) {
                 HttpEntity he = resp.getEntity();
                 String response = EntityUtils.toString(he,"UTF-8");
                 logger.info("addQuota response is:"+response);
                 if (response != null && response.indexOf("SUCCESS") > -1) {
                     return true;
                 } else {
                     return false;
                 }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return false;
    }
    
    public static final Map<String, Map<String, String>> queryUnitPrice(int ssid, String host) {
        JSONObject json = null;
        //Map<String, Map<String, String>> result = new HashMap<String, List<String>>();
        try {           
            String url = host+"?ssid="+ssid;
             //String response = HttpUtil.get(url);
            //TODO
             String response = "{}";
            //String response ="{\"code\": 0,\"message\": \"SUCCESS\",\"price\": {\"0\": {\"0\": \"0.085\"},\"62\": {\"0\": \"0.028\"}},\"ssid\": \"66\"}";
            // String tokenRes = httpClient(tokenUrl);
            // {"access_token":"2.00AvYzKGWraycB344b3eb242NUbiQB","remind_in":"157679999","expires_in":157679999,"uid":"5659232590"}
            logger.info("queryUnitPrice:"+response);
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                //logger.info("json:"+json);
                if (json.get("price")!=null&&Integer.valueOf(0).equals(json.get("code"))) {
					Map<String, Map<String, String>> priceOfCountryMap = (Map<String, Map<String, String>>)json.get("price");
					return priceOfCountryMap;
				}
            }
        } catch (Exception e) {
            logger.error("queryUnitPrice throw exception", e);
        }
        return null;
    }
    
    public static final String queryCredit(int ssid, String host) {
        JSONObject json = null;
        //Map<String, Map<String, String>> result = new HashMap<String, List<String>>();
        try {           
            String url = host+"/quota/query/left_money?ssid="+ssid;
             String response = HttpUtil.get(url);
             //String response = "{}";
            //String response ="{\"code\": 0,\"message\": \"SUCCESS\",\"price\": {\"0\": {\"0\": \"0.085\"},\"62\": {\"0\": \"0.028\"}},\"ssid\": \"66\"}";
            // String tokenRes = httpClient(tokenUrl);
            // {"access_token":"2.00AvYzKGWraycB344b3eb242NUbiQB","remind_in":"157679999","expires_in":157679999,"uid":"5659232590"}
            logger.info("queryCredit:"+response);
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                //logger.info("json:"+json);
                if (json.get("left_money")!=null&&"SUCCESS".equalsIgnoreCase(String.valueOf(json.get("message")))) {
					return String.valueOf(json.get("left_money"));
				}
            }
        } catch (Exception e) {
            logger.error("queryCredit throw exception", e);
        }
        return null;
    }
    
    public static final Page<DailyReport> queryCdrSummary(AdminInfo admin, String host, long starttime, long endtime) {
        JSONObject json = null;
        int ssid = admin.getSsid();
        try {           
        	Page<DailyReport> pages = new Page<DailyReport>();
            String url = host+"/querymsgsummaryinfo?ssid="+ssid+"&starttime="+starttime/1000+"&endtime="+endtime/1000;
            logger.info("queryCdrSummary request:"+url);
            String response = HttpUtil.get(url);
             logger.info("queryCdrSummary response:"+response);
             List<DailyReport> dailyReports = new ArrayList<DailyReport>();
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("data")!=null&&"SUCCESS".equalsIgnoreCase(String.valueOf(json.get("message")))) {
					List<Map<String, Object>> datas = (List<Map<String, Object>>)json.get("data");
					for (Map<String, Object> map : datas) {
						DailyReport report = new DailyReport();
						report.setCustomerName(admin.getUsername());
						report.setFileLines((int)map.get("totalsubmitNumber"));
						report.setInSuccessCount((int)map.get("DeliverysuccessMsgnumber"));
						report.setInFailedCount((int)map.get("DeliveryFailedMsgNumber"));
						report.setStartTime((String)map.get("date"));
						dailyReports.add(report);
					}
				}
            }
            pages.setRecords(dailyReports);
            pages.setTotal(dailyReports.size());
            pages.setSize(dailyReports.size());
            return pages;
        } catch (Exception e) {
            logger.error("queryCdrSummary throw exception", e);
        }
        return null;
    }
    
   
    
    public static final EnumberDTO enumquery(String number, String host) {
        JSONObject json = null;
        EnumberDTO enumber = new EnumberDTO();
        try{
        	String url = host+"/query/enumquery?number="+number;
        	logger.info("enumquery request:"+url);
            String response = HttpUtil.get(url);
             logger.info("enumquery response:"+response);
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("data")!=null &&"SUCCESS".equalsIgnoreCase(String.valueOf(json.get("message")))) {
                	Map<String, Object> datas = (Map<String, Object>)json.get("data");
                   enumber.setCity(String.valueOf(datas.get("city")));
                   String countryName = (String)CountryInfoDictCacheManager.getInstance().get(String.valueOf(datas.get("country")));
					if (countryName == null) {
						countryName = String.valueOf(datas.get("country"));
					}
                   enumber.setCountry(countryName);
                   enumber.setOperator(String.valueOf(datas.get("operator")));
				}
            }
           return enumber;     
        }catch(Exception e){
        	logger.error("enumquery throw exception", e);
        }
        return enumber;
    }
    
    public static final MonitorReport queryDRSummaryForMonitor(MonitorRequest request, String host) {
        JSONObject json = null;
        try {           
        	MonitorReport report = new MonitorReport();
        	///api/v1/traffic/deliveryvolumn? ossid=<number>&rssid=<number> type=m&starttime=<long>&endtime=<long>
            String url = host+"/traffic/deliveryvolumn?ossid="+request.getOssid()
            		+"&rssid="+request.getRssid()+"&type="+request.getType()+"&country="+request.getCountry()
            		+"&starttime="+request.getStartTime()/1000+"&endtime="+request.getEndTime()/1000;
            logger.info("queryDRSummaryForMonitor request:"+url);
            String response = HttpUtil.get(url);
             logger.info("queryDRSummaryForMonitor response:"+response);
             List<Report> inList = new ArrayList<Report>();
             List<Report> outList = new ArrayList<Report>();
             report.setInDR(inList);
             report.setOutDR(outList);
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("data")!=null&&"SUCCESS".equalsIgnoreCase(String.valueOf(json.get("message")))) {
					List<Map<String, Object>> datas = (List<Map<String, Object>>)json.get("data");
					for (Map<String, Object> timingData : datas) {
						String timing = String.valueOf(timingData.get("time"));
						Map<String, Object> inDR = (Map<String, Object>)timingData.get("inDR");
						Map<String, Object> outDR = (Map<String, Object>)timingData.get("outDR");
						Report inReport  = new Report();
						inReport.setFailNum((int)inDR.get("failedMsgNumber"));
						inReport.setStartTime(timing);
						inReport.setSuccessNum((int)inDR.get("successfulMsgNumber"));
						inReport.setSuccessRatio(String.valueOf(inDR.get("SuccessRatio")));
						inReport.setTotalNum((int)inDR.get("totalMsgNumber"));
						inList.add(inReport);
						Report outReport  = new Report();
						outReport.setFailNum((int)outDR.get("failedMsgNumber"));
						outReport.setStartTime(timing);
						outReport.setSuccessNum((int)outDR.get("successfulMsgNumber"));
						outReport.setSuccessRatio(String.valueOf(outDR.get("SuccessRatio")));
						outReport.setTotalNum((int)outDR.get("totalMsgNumber"));
						outList.add(outReport);
					}
				}
            }
           return ConvertResponseToOutPut(request, report);                        
        } catch (Exception e) {
            logger.error("queryDRSummaryForMonitor throw exception", e);
        }
        return null;
    }
    
    public static final Page<SMSLogging> querySMSLogging(SMSLoggingRequestVo request, String host) {
        JSONObject json = null;
        Page<SMSLogging> pages = new Page<SMSLogging>();
        List<SMSLogging> smsList = new ArrayList<SMSLogging>();
        pages.setRecords(smsList);
        try {           
        	MonitorReport report = new MonitorReport();
        	///api/v1/traffic/deliveryvolumn? ossid=<number>&rssid=<number> type=m&starttime=<long>&endtime=<long>
            String url = host+"/query/troubleshooting";          
            String body = JSONObject.toJSONString(request);
            logger.info("querySMSLogging request:"+url +", body:"+body);
            String response = HttpUtil.postJson(url, body);
             logger.info("querySMSLogging response:"+response);             
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (StringUtils.isNotBlank(json.getString("data"))&&"SUCCESS".equalsIgnoreCase(String.valueOf(json.get("message")))) {
					List<Map<String, Object>> datas = (List<Map<String, Object>>)json.get("data");					
					for (Map<String, Object> smsData : datas) {
						SMSLogging logging = new SMSLogging();
						logging.setMsgID(String.valueOf(smsData.get("MsgID")));
						logging.setInMsgID(String.valueOf(smsData.get("InMsgID")));
						logging.setOutMsgID(String.valueOf(smsData.get("OutMsgID")));
						String customerName = (String)CustomerSSIDDictCacheManager.getInstance().get(String.valueOf(smsData.get("OSsID")));
						String dcName = (String)CustomerSSIDDictCacheManager.getInstance().get(String.valueOf(smsData.get("RSsID")));
						logging.setVendor(dcName);
						logging.setCustomer(customerName);
						logging.setOperator(String.valueOf(smsData.get("Operator")));
						logging.setSenderAddresses(String.valueOf(smsData.get("SenderAddresses")));
						logging.setRecipientAddresses(String.valueOf(smsData.get("RecipientAddresses")));
						String countryName = (String)CountryInfoDictCacheManager.getInstance().get(String.valueOf(smsData.get("RCountry")));
						if (countryName == null) {
							countryName = String.valueOf(smsData.get("RCountry"));
						}
						logging.setCountry(countryName);
						logging.setInContentType(String.valueOf(smsData.get("InContentType")));
						logging.setOutContentType(String.valueOf(smsData.get("OutContentType")));
						logging.setInContent(String.valueOf(smsData.get("InTextContent")));
						logging.setOutContent(String.valueOf(smsData.get("OutTextContent")));
						logging.setInMsgSize(String.valueOf(smsData.get("inmsgsize")));
						logging.setOutMsgSize(String.valueOf(smsData.get("outmsgsize")));						
						String status = String.valueOf(smsData.get("Status"));
						logging.setStatusCode(status);
						String desc = "";
						if (StringUtils.isNotBlank(status) && !"null".equalsIgnoreCase(status)) {
							switch (status) {
							case "1":
								desc= "insubmit success";
								break;
							case "2":
								desc= "insubmit error";
								break;
							case "3":
								desc= "outsubmit success";
								break;
							case "4":
								desc= "outsubmit error";
								break;
							case "5":
								desc= "outdr success";
								break;
							case "6":
								desc= "outdr error";
								break;
							case "7":
								desc= "indr success";
								break;
							case "8":
								desc= "indr error";
								break;
							default:
								break;
							}
						}
						logging.setStatusText(desc);
						String timeFormat = "yyyyMMdd HH:mm:ss";
						String insubmitDate = "";
						if (StringUtils.isNotBlank(String.valueOf(smsData.get("insubmit_time")))
								&& !"null".equalsIgnoreCase(String.valueOf(smsData.get("insubmit_time")))
								&& !"-1".equalsIgnoreCase(String.valueOf(smsData.get("insubmit_time")))) {
							long insubmitTime = Long.parseLong(String.valueOf(smsData.get("insubmit_time")));
							insubmitDate = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(insubmitTime, timeFormat, "GMT+8");							
						}
						logging.setInsubmitTime(insubmitDate);
						String outsubmitDate = "";
						if (StringUtils.isNotBlank(String.valueOf(smsData.get("outsubmit_time")))
								&& !"null".equalsIgnoreCase(String.valueOf(smsData.get("outsubmit_time")))
								&& !"-1".equalsIgnoreCase(String.valueOf(smsData.get("outsubmit_time")))) {
							long outsubmitTime = Long.parseLong(String.valueOf(smsData.get("outsubmit_time")));
							outsubmitDate = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(outsubmitTime, timeFormat, "GMT+8");							
						}
						logging.setOutsubmitTime(outsubmitDate);
						String indrDate = "";
						if (StringUtils.isNotBlank(String.valueOf(smsData.get("in_delivery_time")))
								&& !"null".equalsIgnoreCase(String.valueOf(smsData.get("in_delivery_time")))
								&& !"-1".equalsIgnoreCase(String.valueOf(smsData.get("in_delivery_time")))) {
							long indrTime = Long.parseLong(String.valueOf(smsData.get("in_delivery_time")));
							indrDate = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(indrTime, timeFormat, "GMT+8");							
						}
						logging.setInDeliveryTime(indrDate);
						
						String outdrDate = "";
						if (StringUtils.isNotBlank(String.valueOf(smsData.get("out_deliver_time")))
								&& !"null".equalsIgnoreCase(String.valueOf(smsData.get("out_deliver_time")))
								&& !"-1".equalsIgnoreCase(String.valueOf(smsData.get("out_deliver_time")))) {
							long outdrTime = Long.parseLong(String.valueOf(smsData.get("out_deliver_time")));
							outdrDate = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(outdrTime, timeFormat, "GMT+8");							
						}
						logging.setOutDeliverTime(outdrDate);
						logging.setInDRStatusCode(getStatus(Integer.parseInt(String.valueOf(smsData.get("InDRStatusCode")))));
						logging.setInSubmitStatusCode(getStatus(Integer.parseInt(String.valueOf(smsData.get("InSubmitStatusCode")))));
						logging.setOutDRStatusCode(getStatus(Integer.parseInt(String.valueOf(smsData.get("OutDRStatusCode")))));
						logging.setOutSubmitStatusCode(getStatus(Integer.parseInt(String.valueOf(smsData.get("OutSubmitStatusCode")))));
						String drLatency = String.valueOf(smsData.get("DR_latency"));
						if (StringUtils.isNotBlank(drLatency)) {
							logging.setDrLatency(Long.parseLong(drLatency));
						}
						
						String subLatency = String.valueOf(smsData.get("submit_latency"));
						if (StringUtils.isNotBlank(subLatency)) {
							logging.setSubmitLatency(Long.parseLong(subLatency));																											
						}				
						List<SMSLoggingField> fields = new ArrayList<SMSLoggingField>();
						SMSLoggingField inSubmitStatusCode = new SMSLoggingField();
						inSubmitStatusCode.setField("InSubmitStatus");
						inSubmitStatusCode.setValue(""+logging.getInSubmitStatusCode());
						fields.add(inSubmitStatusCode);
												
						SMSLoggingField outSubmitStatusCode = new SMSLoggingField();
						outSubmitStatusCode.setField("OutSubmitStatus");
						outSubmitStatusCode.setValue(""+logging.getOutSubmitStatusCode());
						fields.add(outSubmitStatusCode);
						

						SMSLoggingField outdrStatusCode = new SMSLoggingField();
						outdrStatusCode.setField("OutDRStatus");
						outdrStatusCode.setValue(""+logging.getOutDRStatusCode());
						fields.add(outdrStatusCode);
						
						SMSLoggingField indrStatusCode = new SMSLoggingField();
						indrStatusCode.setField("InDRStatus");
						indrStatusCode.setValue(""+logging.getInDRStatusCode());
						fields.add(indrStatusCode);
						
						SMSLoggingField field = new SMSLoggingField();
						field.setField("inmsgid");
						field.setValue(logging.getInMsgID());
						fields.add(field);
						SMSLoggingField outmsgid = new SMSLoggingField();
						outmsgid.setField("outmsgid");
						outmsgid.setValue(logging.getOutMsgID());
						fields.add(outmsgid);
						SMSLoggingField vendor = new SMSLoggingField();
						vendor.setField("vendor");
						vendor.setValue(logging.getVendor());
						fields.add(vendor);
						SMSLoggingField country = new SMSLoggingField();
						country.setField("country");
						country.setValue(logging.getCountry());
						fields.add(country);
						
						SMSLoggingField operator = new SMSLoggingField();
						operator.setField("operator");
						operator.setValue(logging.getOperator());
						fields.add(operator);
						
						SMSLoggingField outContent = new SMSLoggingField();
						outContent.setField("outContent");
						outContent.setValue(logging.getOutContent());
						fields.add(outContent);
						
						SMSLoggingField inCharset = new SMSLoggingField();
						inCharset.setField("inCharset");
						inCharset.setValue(logging.getInContentType());
						fields.add(inCharset);
						
						SMSLoggingField outCharset = new SMSLoggingField();
						outCharset.setField("outCharset");
						outCharset.setValue(logging.getOutContentType());
						fields.add(outCharset);												
						SMSLoggingField inmsgsize = new SMSLoggingField();
						inmsgsize.setField("inmsgsize");
						inmsgsize.setValue(logging.getInMsgSize());
						fields.add(inmsgsize);
						
						SMSLoggingField outmsgsize = new SMSLoggingField();
						outmsgsize.setField("outmsgsize");
						outmsgsize.setValue(logging.getOutMsgSize());
						fields.add(outmsgsize);												
						
						SMSLoggingField insubmit_time = new SMSLoggingField();
						insubmit_time.setField("insubmit_time");
						insubmit_time.setValue(logging.getInsubmitTime());
						fields.add(insubmit_time);
						
						SMSLoggingField outsubmit_time = new SMSLoggingField();
						outsubmit_time.setField("outsubmit_time");
						outsubmit_time.setValue(logging.getOutsubmitTime());
						fields.add(outsubmit_time);
						
						SMSLoggingField indr_time = new SMSLoggingField();
						indr_time.setField("indr_time");
						indr_time.setValue(logging.getInDeliveryTime());
						fields.add(indr_time);
						
						SMSLoggingField outdr_time = new SMSLoggingField();
						outdr_time.setField("outdr_time");
						outdr_time.setValue(logging.getOutDeliverTime());
						fields.add(outdr_time);
						
						SMSLoggingField submitLC = new SMSLoggingField();
						submitLC.setField("submit_latency(ms)");
						submitLC.setValue(""+logging.getSubmitLatency());
						fields.add(submitLC);
						
						SMSLoggingField drLC = new SMSLoggingField();
						drLC.setField("outdr_latency(ms)");
						drLC.setValue(""+logging.getDrLatency());
						fields.add(drLC);
											
						
												
						
						logging.setSmsLoggingFields(fields);												
						smsList.add(logging);
					}
					
					pages.setTotal((int)json.get("totalSize"));
					pages.setCurrent(request.getPageIndex());
					pages.setSize(smsList.size());
				}
            }
           return pages;                        
        } catch (Exception e) {
            logger.error("querySMSLogging throw exception", e);
        }
        return pages;
    }

	private static MonitorReport ConvertResponseToOutPut(MonitorRequest request,
			MonitorReport report) {
		long interval = 5*60*1000;
		long startTime = request.getStartTime()/1000/60/5*5*60*1000;
		long endTime = request.getEndTime()/1000/60/5*5*60*1000;
		String timeFormat = "yyyyMMddHHmm";
		String showtimeFormat = "yy-MM-dd HH:mm";
		switch (request.getType()) {
		case "h":
			interval = 60*60*1000;
			timeFormat = "yyyyMMddHH";
			showtimeFormat = "yy-MM-dd HH";
			startTime = request.getStartTime()/1000/60/60*60*60*1000;
		    endTime = request.getEndTime()/1000/60/60*60*60*1000;
			break;
		case "1m":
			interval = 1*60*1000;
			timeFormat = "yyyyMMddHHmm";
			showtimeFormat = "yy-MM-dd HH:mm";
			startTime = request.getStartTime()/1000/60/1*1*60*1000;
		    endTime = request.getEndTime()/1000/60/1*1*60*1000;
			break;
		case "d":
			interval = 24*60*60*1000;
			timeFormat = "yyyyMMdd";
			showtimeFormat = "yy-MM-dd";
			startTime = request.getStartTime()/1000/60/60/24*24*60*60*1000;
		    endTime = request.getEndTime()/1000/60/60/24*24*60*60*1000;
			break;
		default:
			break;
		}            
		MonitorReport reports  = new MonitorReport();
		List<Report> outputInSubmitList = new ArrayList<Report>();
		List<Report> outputOutSubmitList = new ArrayList<Report>();
		List<Report> outputInDRList = new ArrayList<Report>();
		List<Report> outputOutDRList = new ArrayList<Report>();
		reports.setInSubmit(outputInSubmitList);
		reports.setInDR(outputInDRList);
		reports.setOutDR(outputOutDRList);
		reports.setOutSubmit(outputOutSubmitList);
		for (int i = 0; i < (endTime-startTime)/interval; i++) {
			long dayTime = startTime+i*interval;
			String startDate = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(dayTime, timeFormat, "GMT+8");
			Report submit = new Report();
			submit.setStartTime(startDate);
			List<Report> respInSumitList = report.getInSubmit();	
			if (respInSumitList != null) {
				for (Report temp: respInSumitList) {
					if (temp.getStartTime().equalsIgnoreCase(startDate)) {
						submit.setFailNum(temp.getFailNum());
						submit.setSuccessNum(temp.getSuccessNum());
						submit.setSuccessRatio(temp.getSuccessRatio());
						submit.setTotalNum(temp.getTotalNum());
						break;
					}
				}				
			}	
			String showStartDate = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(dayTime, showtimeFormat, "GMT+8");
			submit.setStartTime(showStartDate);
			outputInSubmitList.add(submit);
			//outsubmit
			Report outSubmit = new Report();
			outSubmit.setStartTime(startDate);
			List<Report> respOutSumitList = report.getOutSubmit();
			if (respOutSumitList !=null) {
				for (Report temp: respOutSumitList) {
					if (temp.getStartTime().equalsIgnoreCase(startDate)) {
						outSubmit.setFailNum(temp.getFailNum());
						outSubmit.setSuccessNum(temp.getSuccessNum());
						outSubmit.setSuccessRatio(temp.getSuccessRatio());
						outSubmit.setTotalNum(temp.getTotalNum());
						break;
					}
				}
				
			}	
			outSubmit.setStartTime(showStartDate);
			outputOutSubmitList.add(outSubmit);
			//outDR
			Report outDReport = new Report();
			outDReport.setStartTime(startDate);
			List<Report> respOutDRList = report.getOutDR();
			if (respOutDRList != null) {
				for (Report temp: respOutDRList) {
					if (temp.getStartTime().equalsIgnoreCase(startDate)) {
						outDReport.setFailNum(temp.getFailNum());
						outDReport.setSuccessNum(temp.getSuccessNum());
						outDReport.setSuccessRatio(temp.getSuccessRatio());
						outDReport.setTotalNum(temp.getTotalNum());
						break;
					}
				}
				
			}	
			outDReport.setStartTime(showStartDate);
			outputOutDRList.add(outDReport);
			//inDR
			Report inDReport = new Report();
			inDReport.setStartTime(startDate);			
			List<Report> respInDRList = report.getInDR();	
			if (respInDRList != null) {
				for (Report temp: respInDRList) {
					if (temp.getStartTime().equalsIgnoreCase(startDate)) {
						inDReport.setFailNum(temp.getFailNum());
						inDReport.setSuccessNum(temp.getSuccessNum());
						inDReport.setSuccessRatio(temp.getSuccessRatio());
						inDReport.setTotalNum(temp.getTotalNum());
						break;
					}
				}				
			}
			inDReport.setStartTime(showStartDate);
			outputInDRList.add(inDReport);
		}		
		return reports;
	}
	
	private static MonitorLatencyDistributeResponse ConvertDistributeResponseToOutPut(MonitorRequest request,
			MonitorLatencyDistributeResponse response) {
		long interval = 5*60*1000;
		long startTime = request.getStartTime()/1000/60/5*5*60*1000;
		long endTime = request.getEndTime()/1000/60/5*5*60*1000;
		String timeFormat = "yyyyMMddHHmm";
		String showtimeFormat = "yy-MM-dd HH:mm";
		switch (request.getType()) {
		case "h":
			interval = 60*60*1000;
			timeFormat = "yyyyMMddHH";
			showtimeFormat = "yy-MM-dd HH";
			startTime = request.getStartTime()/1000/60/60*60*60*1000;
		    endTime = request.getEndTime()/1000/60/60*60*60*1000;
			break;
		case "d":
			interval = 24*60*60*1000;
			timeFormat = "yyyyMMdd";
			showtimeFormat = "yy-MM-dd";
			startTime = request.getStartTime()/1000/60/60/24*24*60*60*1000;
		    endTime = request.getEndTime()/1000/60/60/24*24*60*60*1000;
			break;
		default:
			break;
		}            
		MonitorLatencyDistributeResponse report = new MonitorLatencyDistributeResponse();
        List<String> timeList =new ArrayList<String>();
        List<Integer> less1MNum = new ArrayList<Integer>();
    	List<Integer> b1a5MNum = new ArrayList<Integer>();
    	List<Integer> b5a15MNum = new ArrayList<Integer>();
    	List<Integer> morethan15MNum = new ArrayList<Integer>();
    	List<Integer> nodrNum = new ArrayList<Integer>();
        report.setTimeList(timeList);
        report.setB1a5MNum(b1a5MNum);
        report.setB5a15MNum(b5a15MNum);
        report.setLess1MNum(less1MNum);
        report.setMorethan15MNum(morethan15MNum);
        report.setNodrNum(nodrNum);
		for (int i = 0; i < (endTime-startTime)/interval; i++) {
			long dayTime = startTime+i*interval;
			String startDate = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(dayTime, timeFormat, "GMT+8");	
			String showStartDate = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(dayTime, showtimeFormat, "GMT+8");			
			List<String> tList = response.getTimeList();
			boolean isExist = false;
			for (int j=0; j<tList.size(); j++) {
				if (tList.get(j).equalsIgnoreCase(startDate)) {
					isExist = true;
					timeList.add(showStartDate);
					less1MNum.add(response.getLess1MNum().get(j));
					b1a5MNum.add(response.getB1a5MNum().get(j));
					b5a15MNum.add(response.getB5a15MNum().get(j));
					morethan15MNum.add(response.getMorethan15MNum().get(j));
					nodrNum.add(response.getNodrNum().get(j));
					break;
				}
			}
			if (isExist) {
				continue;
			}else {
				timeList.add(showStartDate);
				less1MNum.add(0);
				b1a5MNum.add(0);
				b5a15MNum.add(0);
				morethan15MNum.add(0);
				nodrNum.add(0);
			}
		}		
		return report;
	}
    
	private static MonitorDuplicateNumResponse ConvertDuplicateNumResponseToOutPut(MonitorRequest request,
			MonitorDuplicateNumResponse response) {
		long interval = 5*60*1000;
		long startTime = request.getStartTime()/1000/60/5*5*60*1000;
		long endTime = request.getEndTime()/1000/60/5*5*60*1000;
		String timeFormat = "yyyyMMddHHmm";
		String showtimeFormat = "yy-MM-dd HH:mm";
		switch (request.getType()) {
		case "h":
			interval = 60*60*1000;
			timeFormat = "yyyyMMddHH";
			showtimeFormat = "yy-MM-dd HH";
			startTime = request.getStartTime()/1000/60/60*60*60*1000;
		    endTime = request.getEndTime()/1000/60/60*60*60*1000;
			break;
		case "d":
			interval = 24*60*60*1000;
			timeFormat = "yyyyMMdd";
			showtimeFormat = "yy-MM-dd";
			startTime = request.getStartTime()/1000/60/60/24*24*60*60*1000;
		    endTime = request.getEndTime()/1000/60/60/24*24*60*60*1000;
			break;
		default:
			break;
		}            
		MonitorDuplicateNumResponse report = new MonitorDuplicateNumResponse();
		List<String> timeList = new ArrayList<String>();
        List<String> duplicatePercentList = new ArrayList<String>();
        List<Integer> totalList = new ArrayList<Integer>();
        List<Integer> duplicateList = new ArrayList<Integer>();
        report.setDuplicateNumber(duplicateList);
        report.setDuplicateNumberPercentage(duplicatePercentList);
        report.setTimeList(timeList);
        report.setTotalNumber(totalList);
		for (int i = 0; i < (endTime-startTime)/interval; i++) {
			long dayTime = startTime+i*interval;
			String startDate = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(dayTime, timeFormat, "GMT+8");	
			String showStartDate = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(dayTime, showtimeFormat, "GMT+8");			
			List<String> tList = response.getTimeList();
			boolean isExist = false;
			for (int j=0; j<tList.size(); j++) {
				if (tList.get(j).equalsIgnoreCase(startDate)) {
					isExist = true;
					timeList.add(showStartDate);
					duplicateList.add(response.getDuplicateNumber().get(j));
					duplicatePercentList.add(response.getDuplicateNumberPercentage().get(j));
					totalList.add(response.getTotalNumber().get(j));					
					break;
				}
			}
			if (isExist) {
				continue;
			}else {
				timeList.add(showStartDate);
				duplicateList.add(0);
				duplicatePercentList.add("0");
				totalList.add(0);
			}
		}		
		return report;
	}
	
    public static final List<MonitorCountryReport> queryCountrySummaryForMonitor(MonitorRequest request, String host) {
        JSONObject json = null;
        try {           
        	
        	///api/v1/traffic/sendvolumn? ossid=<number>&rssid=<number> type=m&starttime=<long>&endtime=<long>
            String url = host+"/traffic/countryvolumn?ossid="+request.getOssid()
            		+"&rssid="+request.getRssid()+"&type="+request.getType()+"&country="+request.getCountry()
            		+"&starttime="+request.getStartTime()/1000+"&endtime="+request.getEndTime()/1000;
            logger.info("queryCountrySummaryForMonitor request:"+url);
            String response = HttpUtil.get(url);
             logger.info("queryCountrySummaryForMonitor response:"+response);
             List<MonitorCountryReport> countryReports = new ArrayList<MonitorCountryReport>();
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("data")!=null&&"SUCCESS".equalsIgnoreCase(String.valueOf(json.get("message")))) {
					List<Map<String, Object>> datas = (List<Map<String, Object>>)json.get("data");
					for (Map<String, Object> timingData : datas) {
							MonitorCountryReport report = new MonitorCountryReport();
							String cn = String.valueOf(timingData.get("countryName"));
							report.setCountry(cn);
							Map<String, Object> inSubmit = (Map<String, Object>)timingData.get("inSubmit");
							Map<String, Object> outSubmit = (Map<String, Object>)timingData.get("outSubmit");
							Report inReport  = new Report();
							inReport.setFailNum((int)inSubmit.get("failedMsgNumber"));
							inReport.setSuccessNum((int)inSubmit.get("successfulMsgNumber"));
							inReport.setSuccessRatio(String.valueOf(inSubmit.get("SuccessRatio")));
							inReport.setTotalNum((int)inSubmit.get("totalMsgNumber"));
                            report.setInSubmit(inReport);
							Report outReport  = new Report();
							outReport.setFailNum((int)outSubmit.get("failedMsgNumber"));
							outReport.setSuccessNum((int)outSubmit.get("successfulMsgNumber"));
							outReport.setSuccessRatio(String.valueOf(outSubmit.get("SuccessRatio")));
							outReport.setTotalNum((int)outSubmit.get("totalMsgNumber"));
							report.setOutSubmit(outReport);
							
							Map<String, Object> inDR = (Map<String, Object>)timingData.get("inDR");
							Map<String, Object> outDR = (Map<String, Object>)timingData.get("outDR");
							Report inDRReport  = new Report();
							inDRReport.setFailNum((int)inDR.get("failedMsgNumber"));
							inDRReport.setSuccessNum((int)inDR.get("successfulMsgNumber"));
							inDRReport.setSuccessRatio(String.valueOf(inDR.get("SuccessRatio")));
							inDRReport.setTotalNum((int)inDR.get("totalMsgNumber"));
							report.setInDR(inDRReport);
							Report outDRReport  = new Report();
							outDRReport.setFailNum((int)outDR.get("failedMsgNumber"));
							outDRReport.setSuccessNum((int)outDR.get("successfulMsgNumber"));
							outDRReport.setSuccessRatio(String.valueOf(outDR.get("SuccessRatio")));
							outDRReport.setTotalNum((int)outDR.get("totalMsgNumber"));
							report.setOutDR(outDRReport);
							countryReports.add(report);
						
					}
				}
            }
           return countryReports;
        } catch (Exception e) {
            logger.error("queryCountrySummaryForMonitor throw exception", e);
        }
        return null;
    }
    
    public static final MonitorLatencyReport queryLatencyForMonitor(MonitorRequest request, String host) {
        try {           
        	MonitorLatencyReport report = new MonitorLatencyReport();
        	
        	List<LatencyReport> subList = new ArrayList<LatencyReport>();
            List<LatencyReport> outdrList = new ArrayList<LatencyReport>();
            List<LatencyReport> indrList = new ArrayList<LatencyReport>();
            
            String submitHost = host+"/traffic/submitlatency";
            String outDRHost = host+"/traffic/dcdrlatency";
            String inDRHost = host+"/traffic/jxdrlatency";
            doLatencyQuery(request, submitHost, subList, true);
            doLatencyQuery(request, outDRHost, outdrList, false);
            doLatencyQuery(request, inDRHost, indrList, false);
            
            convertToOutputLatencyReport(request, report, subList, outdrList,
					indrList);
           return report;
        } catch (Exception e) {
            logger.error("queryLatencyForMonitor throw exception", e);
        }
        return null;
    }

	private static void convertToOutputLatencyReport(MonitorRequest request,
			MonitorLatencyReport report, List<LatencyReport> subList,
			List<LatencyReport> outdrList, List<LatencyReport> indrList) {
		long interval = 5*60*1000;
		long startTime = request.getStartTime()/1000/60/5*5*60*1000;
		long endTime = request.getEndTime()/1000/60/5*5*60*1000;
		String timeFormat = "yyyyMMddHHmm";
		String showtimeFormat = "yy-MM-dd HH:mm";
		switch (request.getType()) {
		case "h":
			interval = 60*60*1000;
			timeFormat = "yyyyMMddHH";
			showtimeFormat = "yy-MM-dd HH";
			startTime = request.getStartTime()/1000/60/60*60*60*1000;
		    endTime = request.getEndTime()/1000/60/60*60*60*1000;
			break;
		case "d":
			interval = 24*60*60*1000;
			timeFormat = "yyyyMMdd";
			showtimeFormat = "yy-MM-dd";
			startTime = request.getStartTime()/1000/60/60/24*24*60*60*1000;
		    endTime = request.getEndTime()/1000/60/60/24*24*60*60*1000;
			break;
		default:
			break;
		} 
		List<LatencyReport> outputSubList = new ArrayList<LatencyReport>();
		List<LatencyReport> outputOutdrList = new ArrayList<LatencyReport>();
		List<LatencyReport> outputIndrList = new ArrayList<LatencyReport>();
		report.setSubmitLC(outputSubList);
		report.setOutDRLC(outputOutdrList);
		report.setInDRLC(outputIndrList);
		for (int i = 0; i < (endTime-startTime)/interval; i++) {
			long dayTime = startTime+i*interval;
			String startDate = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(dayTime, timeFormat, "GMT+8");
			LatencyReport insubmit = new LatencyReport();
			insubmit.setStartTime(startDate);
			for (LatencyReport temp: subList) {
				if (temp.getStartTime().equalsIgnoreCase(startDate)) {
					insubmit.setLatency(temp.getLatency());
				}
			}
			String showstartDate = QuotaManagerUtil.getFormatTimeFromMsWithTimeZone(dayTime, showtimeFormat, "GMT+8");
			insubmit.setStartTime(showstartDate);
			outputSubList.add(insubmit);
			
			//dcdr
			LatencyReport dcDReport = new LatencyReport();
			dcDReport.setStartTime(startDate);
			for (LatencyReport temp: outdrList) {
				if (temp.getStartTime().equalsIgnoreCase(startDate)) {
					dcDReport.setLatency(temp.getLatency());
				}
			}
			dcDReport.setStartTime(showstartDate);
			outputOutdrList.add(dcDReport);
			//indr
			LatencyReport inDReport = new LatencyReport();
			inDReport.setStartTime(startDate);
			for (LatencyReport temp: indrList) {
				if (temp.getStartTime().equalsIgnoreCase(startDate)) {
					inDReport.setLatency(temp.getLatency());
				}
			}
			inDReport.setStartTime(showstartDate);
			outputIndrList.add(inDReport);
		}
	}

	private static void doLatencyQuery(MonitorRequest request, String host,
			List<LatencyReport> subList, boolean isSubmit) {
		JSONObject json;
		String url = host+"?ossid="+request.getOssid()
				+"&rssid="+request.getRssid()+"&type="+request.getType()+"&country="+request.getCountry()
				+"&starttime="+request.getStartTime()/1000+"&endtime="+request.getEndTime()/1000;
		logger.info("queryLatencyForMonitor request:"+url);
		String response = HttpUtil.get(url);
		 logger.info("queryLatencyForMonitor response:"+response);             
		if (response != null && StringUtils.isNotBlank(response)) {
		    json = JSON.parseObject(response);
		    if (json.get("data")!=null&&"SUCCESS".equalsIgnoreCase(String.valueOf(json.get("message")))) {
				List<Map<String, Object>> datas = (List<Map<String, Object>>)json.get("data");
				for (Map<String, Object> timingData : datas) {
					// {"time":202003082205,// 表示22 00：05-22 09：59 秒的数据
				   // "submitLatency":150,//ms
				   //},
					String timing = String.valueOf(timingData.get("time"));
					LatencyReport latency = new LatencyReport();
					if (isSubmit) {
						latency.setLatency((int)timingData.get("submitLatency"));
					}else {
						latency.setLatency((int)timingData.get("drLatency"));
					}
					latency.setStartTime(timing);
					subList.add(latency);						
				}
			}
		}
	}
    
    public static final List<ProvinceReportWithOPResponse> queryProvinceReportWithBatch(AdminInfo admin, String host, long starttime, long endtime, int batch) {
        int ssid = admin.getSsid();
        List<ProvinceReportWithOPResponse> list = new ArrayList<ProvinceReportWithOPResponse>();
        try {           
        	
            String url = host+"/successrate/batch_provincebase?ssid="+ssid+"&batchid="+batch+"&starttime="+starttime/1000+"&endtime="+endtime/1000;
            logger.info("queryProvinceReportWithBatch request:"+url);
            String response = HttpUtil.get(url);
            logger.info("queryCdrSummary response:"+response);
           if (response != null && StringUtils.isNotBlank(response)) {
        	   JSONObject json = JSON.parseObject(response);
               if (json.get("data")!=null&&"SUCCESS".equalsIgnoreCase(String.valueOf(json.get("message")))) {
					List<Map<String, Object>> datas = (List<Map<String, Object>>)json.get("data");
					for (Map<String, Object> map : datas) {
						ProvinceReportWithOPResponse province = new ProvinceReportWithOPResponse();
						province.setProvince((String)map.get("province"));
						province.setSuccessfulRate((String)map.get("successfulRate"));
						province.setFailedRate((String)map.get("failedRate"));
						province.setTotalMsgNumber((int)map.get("totalMsgNumber"));
						OperatorReportResponse defalutOP = new OperatorReportResponse();
						province.setCm(defalutOP);
						province.setCt(defalutOP);
						province.setCu(defalutOP);
						List<Map<String, Object>> opDataList = (List<Map<String, Object>>)map.get("opRate");
						if (opDataList != null) {
							for (Map<String, Object> opMap : opDataList) {
								OperatorReportResponse op = new OperatorReportResponse();
								String success = (String)opMap.get("successfulRate");								
								String fail = (String)opMap.get("failedRate");								
				                op.setFailedRate(fail);
				                op.setSuccessfulRate(success);
				                op.setOperator((String)opMap.get("operator"));
				                op.setTotalMsgNumber((int)opMap.get("totalMsgNumber"));
				                if ("移动".equalsIgnoreCase(op.getOperator())) {
									province.setCm(op);
								}else if ("联通".equalsIgnoreCase(op.getOperator())) {
									province.setCu(op);
								}else if ("电信".equalsIgnoreCase(op.getOperator())) {
									province.setCt(op);
								}
							}
						}						
						list.add(province);
					}
				}
           }            
            return list;
        } catch (Exception e) {
            logger.error("queryProvinceReportWithBatch throw exception", e);
        }
        return list;
    }
    
    public static final List<ProvinceReportWithOPResponse> queryProvinceReport(AdminInfo admin, String host, long starttime, long endtime) {
        int ssid = admin.getSsid();
        List<ProvinceReportWithOPResponse> list = new ArrayList<ProvinceReportWithOPResponse>();
        try {                  	
            String url = host+"/successrate/provincebase?ssid="+ssid+"&starttime="+starttime/1000+"&endtime="+endtime/1000;
            logger.info("queryProvinceReportWithBatch request:"+url);
            String response = HttpUtil.get(url);
            logger.info("queryCdrSummary response:"+response);
           if (response != null && StringUtils.isNotBlank(response)) {
        	   JSONObject json = JSON.parseObject(response);
               if (json.get("data")!=null&&"SUCCESS".equalsIgnoreCase(String.valueOf(json.get("message")))) {
					List<Map<String, Object>> datas = (List<Map<String, Object>>)json.get("data");
					for (Map<String, Object> map : datas) {
						ProvinceReportWithOPResponse province = new ProvinceReportWithOPResponse();
						province.setProvince((String)map.get("province"));
						province.setSuccessfulRate((String)map.get("successfulRate"));
						province.setFailedRate((String)map.get("failedRate"));
						province.setTotalMsgNumber((int)map.get("totalMsgNumber"));
						OperatorReportResponse defalutOP = new OperatorReportResponse();
						province.setCm(defalutOP);
						province.setCt(defalutOP);
						province.setCu(defalutOP);
						List<Map<String, Object>> opDataList = (List<Map<String, Object>>)map.get("opRate");
						if (opDataList != null) {
							for (Map<String, Object> opMap : opDataList) {
								OperatorReportResponse op = new OperatorReportResponse();
								String success = (String)opMap.get("successfulRate");								
								String fail = (String)opMap.get("failedRate");								
				                op.setFailedRate(fail);
				                op.setSuccessfulRate(success);
				                op.setOperator((String)opMap.get("operator"));
				                op.setTotalMsgNumber((int)opMap.get("totalMsgNumber"));
				                if ("移动".equalsIgnoreCase(op.getOperator())) {
									province.setCm(op);
								}else if ("联通".equalsIgnoreCase(op.getOperator())) {
									province.setCu(op);
								}else if ("电信".equalsIgnoreCase(op.getOperator())) {
									province.setCt(op);
								}
							}
						}						
						list.add(province);
					}
				}
           }            
            return list;
        } catch (Exception e) {
            logger.error("queryProvinceReportWithBatch throw exception", e);
        }
        return list;
    }
    
    public static final List<OperatorReportResponse> queryOpReport(AdminInfo admin, String host, long starttime, long endtime) {
        int ssid = admin.getSsid();
        List<OperatorReportResponse> list = new ArrayList<OperatorReportResponse>();
        try {                   	
            String url = host+"/successrate/operatorbase?ssid="+ssid+"&starttime="+starttime/1000+"&endtime="+endtime/1000;
            logger.info("queryOpReport request:"+url);
            String response = HttpUtil.get(url);
            logger.info("queryCdrSummary response:"+response);
            if (response != null && StringUtils.isNotBlank(response)) {
            	JSONObject json = JSON.parseObject(response);
               if (json.get("data")!=null&&"SUCCESS".equalsIgnoreCase(String.valueOf(json.get("message")))) {
					List<Map<String, Object>> datas = (List<Map<String, Object>>)json.get("data");
					for (Map<String, Object> map : datas) {
						OperatorReportResponse op = new OperatorReportResponse();
						String success = (String)map.get("successfulRate");
						if (StringUtils.isNotBlank(success)) {
							success=success.substring(0, success.length()-1);
						}else{
							success = "0";
						}
						
						String fail = (String)map.get("failedRate");
						if (StringUtils.isNotBlank(fail)) {
							fail=fail.substring(0, fail.length()-1);
						}else{
							fail = "0";
						}
						
		                op.setFailedRate(fail);
		                op.setSuccessfulRate(success);
		                op.setOperator((String)map.get("operator"));
		                op.setTotalMsgNumber((int)map.get("totalMsgNumber"));
						list.add(op);						
					}
				}
           }
            
            
            /*//String response = HttpUtil.get(url);
            
            for (int i = 0; i < 4; i++) {
            	OperatorReportResponse op = new OperatorReportResponse();
                op.setFailedRate("20%");
                op.setSuccessfulRate("80");
                op.setOperator("电信"+i);
                op.setTotalMsgNumber(100+i);
				list.add(op);
			}*/
            return list;
        } catch (Exception e) {
            logger.error("queryOpReport throw exception", e);
        }
        return list;
    }
    
    public static final Page<SmsCdrInfo> queryCdrDetail(AdminInfo admin, String host, long starttime, long endtime, int pageSize, int pageIndex) {
        
        return null;
    }

	private static int getStatusCode(String statuscode) {
		int status;
		switch (statuscode) {
		case "0":
			status =0;
			break;
		case "10000":
			status =2;
			break;                           
		default:
			status = 3;
			break;
		}
		return status;
	}
    
    
    
    public static final Charges queryCharge(int ssid, String host, int page, int size) {
        JSONObject json = null;
        //Map<String, Map<String, String>> result = new HashMap<String, List<String>>();
        try {           
            String url = host+"?pageIndex="+page+"&pageSize="+size+"&ssid="+ssid;
             String response = HttpUtil.get(url);
             //String response = "{\"code\":200,\"data\":"
             //		+ "{\"current\":1,\"pages\":26,\"records\":"
             //		+ "[{\"id\":687,\"ssid\":80,\"recharge\":\"10000\",\"timemark\":\"2019-08-27 08:02:01\",\"balance\":\"100001\"},"
             //		+ "{\"id\":688,\"ssid\":80,\"recharge\":\"10000\",\"timemark\":\"2019-08-27 08:02:01\",\"balance\":\"100001\"}],"
             //		+ "\"size\":10,\"total\":258},\"message\":\"Success\"}";
            //String response ="{\"code\": 0,\"message\": \"SUCCESS\",\"price\": {\"0\": {\"0\": \"0.085\"},\"62\": {\"0\": \"0.028\"}},\"ssid\": \"66\"}";
            // String tokenRes = httpClient(tokenUrl);
            // {"access_token":"2.00AvYzKGWraycB344b3eb242NUbiQB","remind_in":"157679999","expires_in":157679999,"uid":"5659232590"}
            logger.info("queryCharge response:"+response);
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                Charges charges = JSON.parseObject(response, Charges.class);
                if (charges != null && charges.getRecords() != null) {
					return charges;
				}
            }
        } catch (Exception e) {
            logger.error("queryCharge throw exception", e);
        }
        return null;
    }
    
    public static final TexterResponse texter(String host, String mccmnc, String duration, String token) {
        JSONObject json = null;
        try {           
        	Map<String, String> map = new HashMap<String, String>();  
            map.put("MCCMNC", mccmnc);
            map.put("TestDuration", duration);
            Map<String, String> header = new HashMap<String, String>();
            header.put("X-CM-ProductToken", token);
            String response = HttpUtil.post(host, map, header);
            /*String response ="{\"details\": \"string\",\"mccmnc\": \"45678\","
            		+ "\"test\": {\"id\":\"string\",\"msisdn\":\"string\","
            		+ "\"messageKey\":\"string\",\"preferredAlphaNumericSender\":\"string\",\"preferredNumericSender\":\"string\"}}";*/
            logger.info("query texter message key:"+response);
            if (response != null && response.indexOf("id") > -1
            		&& response.indexOf("test") > -1) {
                json = JSON.parseObject(response);
                TexterResponse texterResponse = json.getObject("test", TexterResponse.class);
                return texterResponse;
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return null;
    }
    
    public static final List<String> queryCurrentavailableoperators(String url,String token) {
        try {   
        	Map<String, String> header = new HashMap<String, String>();
            header.put("X-CM-ProductToken", token);
             String response = HttpUtil.getTexterResult(url, header);
        	//String response="[\"45000\",\"78911\"]";
             logger.info("queryCurrentavailableoperators response:"+response);
            if (response != null && StringUtils.isNotBlank(response)) {
                List<String> mccmncs= JSON.parseObject(response, List.class);
                return mccmncs;
            }
        } catch (Exception e) {
            logger.error("queryCurrentavailableoperators throw exception", e);
        }
        return null;
    }
    
    public static final TexterJsonResult queryTexterResult(String url, String token) {
        try {
        	logger.info("queryTexterResult request:"+url);
        	Map<String, String> header = new HashMap<String, String>();
            header.put("X-CM-ProductToken", token);
             String response = HttpUtil.getTexterResult(url, header);
            //String response="{\"id\":\"string\",\"statusCode\":0,\"status\":\"string\",\"messageKey\":\"string\",\"result\": {\"sender\":\"string\",\"content\":\"string\",\"phoneReceivedDateTime\":\"2017-03-31T14:13:42.518Z\",\"msisdn\":\"string\",\"serviceCenter\":\"string\",\"syncedWithNTP\": true,\"resultReceivedDateTime\":\"2017-03-31T14:13:42.518Z\" }}"; 
        	logger.info("queryTexterResult response:"+response);
            if (response != null && StringUtils.isNotBlank(response)) {
            	TexterJsonResult result= JSON.parseObject(response, TexterJsonResult.class);
                return result;
            }
        } catch (Exception e) {
            logger.error("queryTexterResult throw exception", e);
        }
        return null;
    }  
    
    public static String getStatus(int code) {
        switch (code) {
            case -1:
                return "no-1";
            case -2:
                return "InDelivery-2";
            case 0:
                return "Success-0";
            case 105:
                return "Partial Success-105";
            case 2000:
                return "Client Error-2000";
            case 2010:
                return "Operation Not Available-2010";
            case 2100:
                return "Invalid Message Field-2100";
            case 2110:
                return "Sender Address Error-2110";
            case 2120:
                return "Recipient Address Error-2120";
            case 2130:
                return "Invalid Message Format-2130";
            case 2140:
            	return "Send In_Submit_Resp failed-2140";
            case 2150:
                return "Invalid ServiceTypeID-2150";
            case 2160:
                return "Duplicate Message-2160";
            case 2170:
            	return "Invalid Scheduled Delivery Time-2170";
            case 2200:
                return "Policy Denied-2200";
            case 2210:
                return "Spamed-2210";
            case 2220:
                return "Binary Filter-2220";
            case 2240:
                return "Replace Template Failed-2240";
            case 2300:
                return "Undelivered for Submit-2300";
            case 2500:
                return "Communication Error-2500";
            case 2600:
                return "Throttling Control-2600";
            case 3000:
                return "Server Error-3000";
            case 4000:
                return "Service Error-4000";
            case 5000:
                return "Authentication Error-5000";
            case 5100:
                return "Insufficient balance-5100";
            case 9000:
                return "Submit Unknown Error-9000";
            case 9005:
                return "Fail to query delivery report-9005";
            case 10000:
                return "DR Success-10000";
            case 10001:
                return "DR Accepted-10001";
            case 10105:
                return "DR Enroute-10105";
            case 10200:
                return "DR Retrieved-10200";
            case 10300:
                return "DR Deleted-10300";
            case 10400:
                return "DR Undeliverable-10400";
            case 10500:
                return "DR Rejected-10500";
            case 10600:
                return "DR msg not found-10600";
            case 10700:
                return "DR Rejected by Anti-Spam-10700";
            case 10900:
                return "DR Unknown Error-10900";
            case 11005:
                return "DR send to customer error-11005";
            default:
                return "no mapping error-"+code; 
        }
    }
    
    public static final String refreshYuloreToken(Map<String, Object> request, String host) {
        JSONObject json = null;
        try {           
            String url = host+"/sp/token";          
            String body = JSONObject.toJSONString(request);
            logger.info("refreshYuloreToken request:"+url +", body:"+body);
            String response = HttpUtil.postJson(url, body);
            logger.info("refreshYuloreToken response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("data")!=null&&"success".equalsIgnoreCase(String.valueOf(json.get("info")))) {
                	Map<String, String> result = (Map<String, String>)json.get("data");
                	return result.get("token");
                }                
            }
        }catch (Exception e) {
			logger.error("refresh yulore token throw error", e);			
        }
        return null;
    }
    
    public static final String sendMsgToYulore(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl()+"/sp/send_by_content";          
            String body = JSONObject.toJSONString(request);
            logger.info("sendMsgToYulore request:"+url +", body:"+body+", token:"+admin.getToken());
            String response = HttpUtil.postJsonWithToken(url, body, admin.getToken());
            logger.info("sendMsgToYulore response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("data")!=null&&"200".equalsIgnoreCase(String.valueOf(json.get("status")))) {
                	Map<String, String> result = (Map<String, String>)json.get("data");
                	return result.get("msgId");
                }                
            }
        }catch (Exception e) {
			logger.error("sendMsgToYulore throw error", e);			
        }
        return null;
    }
    
    public static final String sendMsgToWABA(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl();          
            String body = JSONObject.toJSONString(request);
            logger.info("sendMsgToWABA request:"+url +", body:"+body);
            String appid = admin.getAppId();
            String token = admin.getAppSecurity();
            
            String text = appid+":"+token;
            byte[] textByte = text.getBytes("UTF-8");
            String encodedText = encoder.encodeToString(textByte);
            String response = HttpUtil.postJsonWithBasicToken(url, body, encodedText);
            logger.info("sendMsgToWABA response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("message_id")!=null) {
                	String result = String.valueOf(json.get("message_id"));
                	return result;
                }                
            }
        }catch (Exception e) {
			logger.error("sendMsgToWABA throw error", e);			
        }
        return null;
    }
    
    public static final String sendOTTMsgToBRAX(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl();          
            String body = JSONObject.toJSONString(request);
            logger.info("sendOTTMsgToBRAX request:"+url +", body:"+body);
            String token = admin.getToken();
            String response = HttpUtil.postJsonWithToken(url, body, token);
            logger.info("sendOTTMsgToBRAX response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if ("000".equalsIgnoreCase(String.valueOf(json.get("errorCode")))) {
					/*
					 * JSONArray msg = json.getJSONArray("messages"); if(msg!=null &&
					 * !msg.isEmpty()) { JSONObject msgResponse =
					 * JSONObject.parseObject(msg.getString(0)); if(msgResponse!=null &&
					 * "000".equalsIgnoreCase(String.valueOf(msgResponse.get("errorCode")))) {
					 * String result = String.valueOf(msgResponse.get("requestId")); return result;
					 * } }
					 */ 
                	String result = String.valueOf(json.get("referentId")); 
                	return result;
                }                
            }
        }catch (Exception e) {
			logger.error("sendOTTMsgToBRAX throw error", e);			
        }
        return null;
    }
    
    public static final String sendA2PMsg(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl();          
            String body = JSONObject.toJSONString(request);
            logger.info("sendA2PMsg request:"+url +", body:"+body);
            String appid = admin.getAppId();
            String token = admin.getAppSecurity();                        
            String response = HttpUtil.postJsonWithUserHeader(url, body, appid, token);
            logger.info("sendA2PMsg response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("smsid")!=null) {
                	String result = String.valueOf(json.get("smsid"));
                	return result;
                }                
            }
        }catch (Exception e) {
			logger.error("sendA2PMsg throw error", e);			
        }
        return null;
    }
    
    public static final String sendVoiceToBraxis(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl();          
            String body = JSONObject.toJSONString(request);
            logger.info("sendVoiceToBraxis request:"+url +", body:"+body);
            String appid = admin.getAppId();
            String token = admin.getAppSecurity();                        
            String response = HttpUtil.postJsonForBraxis(url, body, appid, token);
            logger.info("sendVoiceToBraxis response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("status")!=null && json.getIntValue("status") == 0) {
                	String result = String.valueOf(json.get("otp_uuid"));
                	return result;
                }                
            }
        }catch (Exception e) {
			logger.error("sendVoiceToBraxis throw error", e);			
        }
        return null;
    }
    
    public static final String sendViberMsgToMK(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl();          
            String body = JSONObject.toJSONString(request);
            logger.info("sendViberMsgToMK request:"+url +", body:"+body);
            String appid = admin.getAppId();
            String token = admin.getAppSecurity();
            
            String text = appid+":"+token;
            byte[] textByte = text.getBytes("UTF-8");
            String encodedText = encoder.encodeToString(textByte);
            String response = HttpUtil.postJsonWithBasicToken(url, body, encodedText);
            logger.info("sendViberMsgToMK response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("message_id")!=null) {
                	String result = String.valueOf(json.get("message_id"));
                	return result;
                }                
            }
        }catch (Exception e) {
			logger.error("sendViberMsgToMK throw error", e);			
        }
        return null;
    }
    
    public static final String sendViberMsgToTelSMS(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl();          
            String body = JSONObject.toJSONString(request);
            logger.info("sendViberMsgToTelSMS request:"+url +", body:"+body);
            String appid = admin.getAppId();
            String token = admin.getAppSecurity();
            
            String text = appid+":"+token;
            byte[] textByte = text.getBytes("UTF-8");
            String encodedText = encoder.encodeToString(textByte);
            String response = HttpUtil.postJsonWithBasicToken(url, body, encodedText);
            logger.info("sendViberMsgToTelSMS response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("message_id")!=null) {
                	String result = String.valueOf(json.get("message_id"));
                	return result;
                }                
            }
        }catch (Exception e) {
			logger.error("sendViberMsgToMK throw error", e);			
        }
        return null;
    }
    
    public static final String sendZaloMsgToMK(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl();          
            String body = JSONObject.toJSONString(request);
            logger.info("sendZaloMsgToMK request:"+url +", body:"+body);
            String appid = admin.getAppId();
            String token = admin.getAppSecurity();
            
            String text = appid+":"+token;
            byte[] textByte = text.getBytes("UTF-8");
            String encodedText = encoder.encodeToString(textByte);
            String response = HttpUtil.postJsonWithBasicToken(url, body, encodedText);
            logger.info("sendZaloMsgToMK response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("message_id")!=null) {
                	String result = String.valueOf(json.get("message_id"));
                	return result;
                }                
            }
        }catch (Exception e) {
			logger.error("sendZaloMsgToMK throw error", e);			
        }
        return null;
    }
    
    public static final String sendMsgToCMWABA(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl();          
            String body = JSONObject.toJSONString(request);
            logger.info("sendMsgToCMWABA request:"+url +", body:"+body);            
            
            String response = HttpUtil.postJson(url, body);
            logger.info("sendMsgToCMWABA response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("messages")!=null && json.getIntValue("errorCode") == 0) {
                	JSONArray jsonArray = json.getJSONArray("messages");
                	if(jsonArray!=null && jsonArray.size()>0) {
                		String result = jsonArray.getJSONObject(0).getString("reference");
                		return result;
                	} 
                	
                }                
            }
        }catch (Exception e) {
			logger.error("sendMsgToCMWABA throw error", e);			
        }
        return null;
    }
    
    public static final String sendMsgToMms(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl()+"/BulkMMS/"; 
            long t = System.currentTimeMillis();
            if(mmsIndex++>=1000){
            	mmsIndex = 0;
            }
            t = t*1000+mmsIndex;
            url = url+t;
            String body = JSONObject.toJSONString(request);
            logger.info("sendMsgToMms request:"+url +", body:"+body);
            String appid = admin.getAppId();
            String token = admin.getAppSecurity();
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(java.nio.charset.Charset.forName("UTF-8"));
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            ContentType contentType = ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8);
            builder.addPart("user", new StringBody(appid, contentType));
            builder.addPart("password", new StringBody(token, contentType));
            builder.addPart("serviceid", new StringBody(admin.getToken(), contentType));
            builder.addPart("recipients", new StringBody("{\""+((String)request.get("msisdn"))+"\"}", contentType));
            builder.addPart("text", new StringBody(((String)request.get("text")), contentType));
            HttpEntity entity = builder.build();
            String response = HttpUtil.postWithHttpEntity(url, entity);
            logger.info("sendMsgToMms response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {            	
                json = JSON.parseObject(response);
                if (json.get("message_id")!=null) {
                	String result = (String)json.get("message_id");
                	return result;
                }                
            }
        }catch (Exception e) {
			logger.error("sendMsgToMms throw error", e);			
        }
        return null;
    }
    /*
    public static final String sendMYMmsToMK(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl()+"/BulkMMS/"; 
            long t = System.currentTimeMillis();
            if(mmsIndex++>=1000){
            	mmsIndex = 0;
            }
            t = t*1000+mmsIndex;
            url = url+t;
            
            String appid = admin.getAppId();
            String token = admin.getAppSecurity();            
            String recN = "[\""+(String)request.get("msisdn")+"\"]";
            String content = new byte[] { 0x01, 0x01}.toString();
            String body = "user="+appid+"&password="+token+"&serviceid="+admin.getToken()+"&recipients="+recN
            		      +"&text="+request.get("text")+"&iscontenturl=0&cotent=0&multimediafiletype=0&subject=0";
            logger.info("sendMYMmsToMK request:"+url +", body:"+body); 
            
            String response = HttpUtil.postMultipart(url, body);
            logger.info("sendMYMmsToMK response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {            	
                json = JSON.parseObject(response);
                if (json.get("message_id")!=null) {
                	String result = (String)json.get("message_id");
                	return result;
                }                
            }
        }catch (Exception e) {
			logger.error("sendMYMmsToMK throw error", e);			
        }
        return null;
    }*/
    
    
    public static final String sendMYMmsToMK(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl()+"/BulkMMS/"; 
            long t = System.currentTimeMillis();
            if(mmsIndex++>=1000){
            	mmsIndex = 0;
            }
            t = t*1000+mmsIndex;
            url = url+t;
            String body = JSONObject.toJSONString(request);
            logger.info("sendMYMmsToMK request:"+url +", body:"+body);
            String appid = admin.getAppId();
            String token = admin.getAppSecurity();
            String textContent = (String)request.get("text");            
            String contenturl = admin.getNumberCheckUrl();
			/*
			 * if(textarray!=null && textarray.length>1) { String contenturlarray[] =
			 * textarray[1].split(" "); contenturl="http"+contenturlarray[0]; }
			 */
            contenturl = JSONObject.toJSONString(contenturl);                        
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(java.nio.charset.Charset.forName("UTF-8"));
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            ContentType contentType = ContentType.create(HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8);
            builder.addPart("user", new StringBody(appid, contentType));
            builder.addPart("password", new StringBody(token, contentType));
            builder.addPart("serviceid", new StringBody(admin.getToken(), contentType));
            builder.addPart("recipients", new StringBody("[\""+(String)request.get("msisdn")+"\"]", contentType));
            builder.addPart("text", new StringBody(textContent, contentType));
            builder.addPart("iscontenturi", new StringBody("1", contentType));
            builder.addPart("content", new StringBody(contenturl, contentType));
            builder.addPart("multimediafiletype", new StringBody("0", contentType));
            builder.addPart("subject", new StringBody("0", contentType));
            HttpEntity entity = builder.build();
            String response = HttpUtil.postWithHttpEntity(url, entity);
            logger.info("sendMYMmsToMK response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {            	
                String msgid[] = response.split("No : ");
                if (msgid.length>1) {
                	String result = msgid[1];
                	result = result.replaceAll("\r\n", "");
                	return result;
                }                
            }
        }catch (Exception e) {
			logger.error("sendMYMmsToMK throw error", e);			
        }
        return null;
    }
    
    public static final boolean sendMMSToMK(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl()+"/api/mt"; 
            String body = JSONObject.toJSONString(request);
            logger.info("sendMMSToMK request:"+url +", body:"+body);
            String response = HttpUtil.postJson(url, body);
            logger.info("sendMMSToMK response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("code")!=null&&"0".equalsIgnoreCase(String.valueOf(json.get("code")))) {                	
                	return true;
                }                
            }            
        }catch (Exception e) {
			logger.error("sendMMSToMK throw error", e);			
        }
        return false;
    }
    
    public static final boolean sendMsgToAvant(Map<String, String> map, String host) {
        JSONObject json = null;
        try {           
        	logger.info("sendMsgToAvant request is:"+map);
            String response = HttpUtil.post(host, map, null);
            logger.info("sendMsgToAvant response is:"+response);
            if (response != null && response.indexOf("status") > -1) {
                json = JSON.parseObject(response);
                boolean result = json.getBooleanValue("status");
                return result;
            } else {
                return false;
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return false;
    }
    
    public static final String sendViberToTelSMS(Map<String, String> map, String host) {
        JSONObject json = null;
        String result = null;
        try {           
        	logger.info("sendViberToTelSMS request is:"+map);
            String response = HttpUtil.post(host, map, null);
            logger.info("sendViberToTelSMS response is:"+response);
            if (response != null && response.indexOf("data") > -1) {
                json = JSON.parseObject(response);
                JSONObject data = json.getJSONObject("data");
                if(data!=null && data.getString("sendCode")!=null && "提交成功".equalsIgnoreCase(json.getString("message"))) {
                	return data.getString("sendCode");
                }               
            } else {
                
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return result;
    }
    
    public static final boolean sendMsgToSim(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl()+"/goip_post_sms.html?username="+admin.getAppId()+"&password="+admin.getAppSecurity();          
            String body = JSONObject.toJSONString(request);
            logger.info("sendMsgToSim request:"+url +", body:"+body);
            String response = HttpUtil.postJson(url, body);
            logger.info("sendMsgToSim response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("status")!=null&&"200".equalsIgnoreCase(String.valueOf(json.get("code")))) {
                	JSONArray jsonArray = json.getJSONArray("status");
                	return "0 OK".equalsIgnoreCase((String)((JSONObject)jsonArray.get(0)).get("status"));
                }                
            }
        }catch (Exception e) {
			logger.error("sendMsgToSim throw error", e);			
        }
        return false;
    }
       
    
    	
    public static final boolean checkNum(AdminInfo admin, String number) {
        JSONObject json = null;
        try { 
        	//http://127.0.0.1:5000/lookup_ori_phone/+918879458226
            String url = admin.getNumberCheckUrl()+"/lookup_ori_phone/"+number;
            String response = HttpUtil.get(url);
            //{"status":200,"info":"success","data":{"type":2}} 
            logger.info("checkNum response:"+response);
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                //logger.info("json:"+json);
                if ("200".equalsIgnoreCase(String.valueOf(json.get("status"))) && json.get("data")!=null) {
					return true;
				}
            }
        } catch (Exception e) {
            logger.error("checkNum throw exception", e);
        }
        return false;
    }
    
    public static final Map<String,String> checkTelegramNum(AdminInfo admin, Map<String,String> request) {
        JSONObject json = null;
        try { 
        	//http://127.0.0.1:5000/lookup_ori_phone/+918879458226
            String url = admin.getNumberCheckUrl()+"/checkSendAbility";
            String body = JSONObject.toJSONString(request);            
            logger.info("checkTelegramNum request:"+url +", body:"+body);
            String response = HttpUtil.postJsonWithBearerToken(url, body, admin.getAppId());
            logger.info("checkTelegramNum response:"+response); 
            /*//{
	            "ok": true,
	            "result": {
	                "request_id": "103703555638483",
	                "phone_number": "6587628281",
	                "request_cost": 0,
	                "remaining_balance": 0
	            }
            }*/
                        
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                //logger.info("json:"+json);
                if ("true".equalsIgnoreCase(String.valueOf(json.get("ok"))) && json.get("result")!=null) {
                	Map<String,String> map = new HashMap<>();
                	JSONObject obj = json.getJSONObject("result");
                	String requestId = obj.getString("request_id");
                	String requestCost = obj.getString("request_cost");
                	String remainingBalance = obj.getString("remaining_balance");
                	map.put("requestId", requestId);
                	map.put("requestCost", requestCost);
                	map.put("remainingBalance", remainingBalance);
                	return map;
				}else {
					return null;
				}
            }
        } catch (Exception e) {
            logger.error("checkTelegramNum throw exception", e);
        }
        return null;
    }
    
    public static final Map<String,String> sendTelegramMsg(AdminInfo admin, Map<String,Object> request) {
        JSONObject json = null;
        try { 
        	//http://127.0.0.1:5000/lookup_ori_phone/+918879458226
            String url = admin.getNumberCheckUrl()+"/sendVerificationMessage";
            String body = JSONObject.toJSONString(request);            
            logger.info("sendTelegramMsg request:"+url +", body:"+body);
            String response = HttpUtil.postJsonWithBearerToken(url, body, admin.getAppId());
            logger.info("sendTelegramMsg response:"+response); 
            /*//{
				    "ok": true,
				    "result": {
				        "request_id": "107583108491530",
				        "phone_number": "8618500375454",
				        "request_cost": 0,
				        "remaining_balance": 0,
				        "delivery_status": {
				            "status": "sent",
				            "updated_at": 1733367318
				        }
				    }
				}
            }*/
                        
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                //logger.info("json:"+json);
                if ("true".equalsIgnoreCase(String.valueOf(json.get("ok"))) && json.get("result")!=null) {
                	Map<String,String> map = new HashMap<>();
                	JSONObject obj = json.getJSONObject("result");
                	JSONObject drstatusjson = obj.getJSONObject("delivery_status");
                	String status = drstatusjson.getString("status");                	
                	map.put("status", status);
                	return map;
				}else {
					return null;
				}
            }
        } catch (Exception e) {
            logger.error("sendTelegramMsg throw exception", e);
        }
        return null;
    }
    
   
    
    public static final boolean sendDRToSMSGW(String host, String msgid, int status) {
        try {  
        	//http://39.104.174.46:80/Gmms/Yulore/DRReply?REFERENCE=yulore123456&STATUS=2&STATUSDESCRIPTION=Delivered
            String url = host+"?REFERENCE="+msgid+"&STATUS="+status;
            logger.info("sendDRToSMSGW request:"+url);
             String response = HttpUtil.get(url);
            logger.info("sendDRToSMSGW response:"+response);
            return true;
        } catch (Exception e) {
            logger.error("sendDRToSMSGW throw exception", e);
            return false;
        }
    }
    
    public static final boolean sendRCSDRToSMSGW(String host, RCSCdrInfo cdr, String token) {
        try {  
        	//http://39.104.174.46:80/Gmms/Yulore/DRReply?REFERENCE=yulore123456&STATUS=2&STATUSDESCRIPTION=Delivered
            
             
             // 1. 构造请求参数
             //String eventId = cdr.;
             String requestId = cdr.getMsgId();
             long timestamp = Instant.now().getEpochSecond(); // 当前 Unix 秒时间戳
             String rawBody = buildJsonBody(cdr, timestamp);

             // 2. 计算签名
             String signature = computeSignature(requestId, timestamp, rawBody, token);
             logger.info("sendRCSDRToSMSGW request:"+host +"," + rawBody);
             String response = HttpUtil.postJsonForRCSDR(host, rawBody, token, requestId, "", ""+timestamp, signature);
            logger.info("sendRCSDRToSMSGW response:"+response);
            return true;
        } catch (Exception e) {
            logger.error("sendDRToSMSGW throw exception", e);
            return false;
        }
    }

		/**
		 * 构建 JSON 请求体
		 */
		private static String buildJsonBody(RCSCdrInfo cdr,long timestamp) {
			String result;
		    switch (cdr.getStatus()) {
		        case 1:
		            result = "REJECT";
		            break;		        
		        case 3:
		            result = "DELIVERED";
		            break;
		        case 4:
		            result = "UNDELIVER";
		            break;
		        case 6:
		            result = "EXPIRED";
		            break;
		        default:
		            result = "Unknown";
		            break;
		    } 
		    
		 // 1. 将秒级时间戳转为 Instant
	        Instant instant = Instant.ofEpochSecond(timestamp);

	        // 2. 指定时区（这里使用东八区 +08:00）
	        ZoneId zoneId = ZoneId.of("Asia/Shanghai"); // 或使用 ZoneOffset.ofHours(8)
	        ZonedDateTime zonedDateTime = instant.atZone(zoneId);

	        // 3. 使用预定义 ISO 格式（输出：2026-07-29T10:00:03+08:00）
	        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	        String formatted = formatter.format(zonedDateTime);

		    
		    return "{"
		            + "\"eventId\": \"\","
		            + "\"type\": \"MESSAGE_DELIVERED\","
		            + "\"occurredAt\": \""+formatted+"\","
		            + "\"senderId\": \""+cdr.getSender()+"\","
		            + "\"messageId\": \""+cdr.getMsgId()+"\","
		            + "\"data\": {"
		            + "\"status\": \""+result+"\""
		            + "}"
		            + "}";
		}
		
		/**
		 * 计算 HMAC-SHA256 签名
		 * 签名原文格式: eventId + "." + timestamp + "." + rawBody
		 * 输出小写十六进制字符串
		 */
		private static String computeSignature(String eventId, long timestamp, String rawBody, String secret) throws Exception {
		    String signContent = eventId + "." + timestamp + "." + rawBody;
		    Mac mac = Mac.getInstance("HmacSHA256");
		    SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
		    mac.init(keySpec);
		    byte[] hmacBytes = mac.doFinal(signContent.getBytes("UTF-8"));
		    return bytesToHex(hmacBytes);
		}
		
		/**
		 * 字节数组转小写十六进制字符串
		 */
		private static String bytesToHex(byte[] bytes) {
		    StringBuilder sb = new StringBuilder();
		    for (byte b : bytes) {
		        sb.append(String.format("%02x", b));
		    }
		    return sb.toString();
		}
    
    
    public static final boolean sendDRWithDAToSMSGW(String host, String msgid, int status, String da) {
        try {  
        	//http://39.104.174.46:80/Gmms/Yulore/DRReply?REFERENCE=yulore123456&STATUS=2&STATUSDESCRIPTION=Delivered
            String url = host+"?REFERENCE="+msgid+"&STATUS="+status+"&DA="+da;
            logger.info("sendDRToSMSGW request:"+url);
             String response = HttpUtil.get(url);
            logger.info("sendDRToSMSGW response:"+response);
            return true;
        } catch (Exception e) {
            logger.error("sendDRToSMSGW throw exception", e);
            return false;
        }
    }
    
    public static final boolean checkWaClientStatus(String host) {
        try {  
        	//http://39.104.174.46:80/Gmms/Yulore/DRReply?REFERENCE=yulore123456&STATUS=2&STATUSDESCRIPTION=Delivered
            String url = host+"/checkstatus";
            String response = HttpUtil.post(url,"");
            logger.info("checkWaClientStatus url {}, response:{}", url, response);
            if(response.contains("CONNECTED")){
            	return true;
            }else{
            	return false;
            }
        } catch (Exception e) {
            logger.error("checkWaClientStatus throw exception", e);
            return false;
        }
    }
    
    public static final boolean isWaRegister(Map<String, Object> request, String host) {
        boolean result = false;
    	try {  
            String url = host+"/isregister";
            String body = JSONObject.toJSONString(request);
            logger.info("isWaRegister request:"+url +", body:"+body);
            String response = HttpUtil.postJsonWithToken(url, body, "");
            logger.info("isWaRegister response:"+response); 
            if("true".equalsIgnoreCase(response)){
            	return true;
            }
        } catch (Exception e) {
            logger.error("isWaRegister throw exception", e);
            return false;
        }
        return result;
    }
    
    public static final boolean isInfoBipRCSRegister(Map<String, Object> request, String host, String token) {
        boolean result = false;
    	try {  
            String url = host+"/rcs/2/capability-check/query";
            String body = JSONObject.toJSONString(request);            
            logger.info("isInfoBipRCSRegister request:"+url +", body:"+body);
            String response = HttpUtil.postJsonWithAuthorization(url, body, token);
            logger.info("isInfoBipRCSRegister response:"+response); 
			/*
			 * { "capabilityCheckResults": [ { "messageId":
			 * "d5c3bdff-2d44-4f74-8a8e-3792fa57dfc8", "phoneNumber": "441134960001",
			 * "code": "ENABLED" }, { "messageId": "b5c3bdff-2d44-4f74-8a8e-3792fa57dfc3",
			 * "phoneNumber": "441134960002", "code": "UNREACHABLE" } ] }
			 */
			/*
			 * Enum: "ENABLED" "UNREACHABLE" "AGENT_NOT_LAUNCHED" "REJECTED_NETWORK"
			 * "REJECTED_ROUTE_NOT_AVAILABLE" "REQUEST_FAILED" "INVALID_DESTINATION_ADDRESS"
			 * "UNKNOWN_CODE"
			 */
            JSONObject json = JSONObject.parseObject(response);            
            if(json!=null){
            	JSONArray array = json.getJSONArray("capabilityCheckResults");
            	if(array!=null&&!array.isEmpty()) {
            		JSONObject object = array.getJSONObject(0);
            		String status = object.getString("code");
            		if("ENABLED".equalsIgnoreCase(status)) {
            			return true;
            		}
            	}            	
            }
        } catch (Exception e) {
            logger.error("isWaRegister throw exception", e);
            return false;
        }
        return result;
    }
    
    public static final String sendInfoBipRCSMsg(Map<String, Object> request, String host, String token) {
        boolean result = false;
    	try {  
            String url = host+"/rcs/2/messages";
            String body = JSONObject.toJSONString(request);            
            logger.info("sendInfoBipRCSMsg request:"+url +", body:"+body);
            String response = HttpUtil.postJsonWithAuthorization(url, body, token);
            logger.info("sendInfoBipRCSMsg response:"+response); 
			/*
			 * {"bulkId": "a28dd97c-2222-4fcf-99f1-0b557ed381da",
				  "messages": [
				    {
				      "messageId": "a28dd97c-1ffb-4fcf-99f1-0b557ed381da",
				      "status": {
				        "groupId": 1,
				        "groupName": "PENDING",
				        "id": 26,
				        "name": "PENDING_ACCEPTED",
				        "description": "Message sent to next instance"
				      },
				      "destination": "441134960001"
				    }
				  ]
				}
			 */
			/*
			 * Enum: "ENABLED" "UNREACHABLE" "AGENT_NOT_LAUNCHED" "REJECTED_NETWORK"
			 * "REJECTED_ROUTE_NOT_AVAILABLE" "REQUEST_FAILED" "INVALID_DESTINATION_ADDRESS"
			 * "UNKNOWN_CODE"
			 */
            JSONObject json = JSONObject.parseObject(response);            
            if(json!=null){
            	JSONArray array = json.getJSONArray("messages");
            	if(array!=null&&!array.isEmpty()) {
            		JSONObject object = array.getJSONObject(0);
            		JSONObject statusJSon = object.getJSONObject("status");
            		String msgid = object.getString("messageId");
            		int status = statusJSon.getIntValue("groupId");
            		if(status==2 || status==4 ||status==5) {
            			return null;
            		}
            		return msgid;
            	}            	
            }
        } catch (Exception e) {
            logger.error("sendInfoBipRCSMsg throw exception", e);
            return null;
        }
        return null;
    }
    
    /**
     * Route Mobile 登录换 JWT。POST {host}/auth/v1/login，Body {username,password}。
     * 成功返回 JWTAUTH，失败返回 null。
     */
    public static final String loginRouteMobile(String host, String user, String pass) {
        try {
            String url = host + "/auth/v1/login";
            JSONObject body = new JSONObject();
            body.put("username", user);
            body.put("password", pass);
            logger.info("loginRouteMobile request:" + url);
            String response = HttpUtil.postJson(url, body.toJSONString());
            logger.info("loginRouteMobile response:" + response);
            JSONObject json = JSONObject.parseObject(response);
            if (json != null) {
                return json.getString("JWTAUTH");
            }
        } catch (Exception e) {
            logger.error("loginRouteMobile throw exception", e);
        }
        return null;
    }

    /**
     * Route Mobile 发送 RCS 消息。POST {host}/rcs/v1/message，Authorization: Bearer {jwt}。
     * 成功返回 202 响应里的 id（channelMsgId），失败返回 null。
     */
    public static final String sendRouteMobileRCSMsg(Map<String, Object> request, String host, String jwt) {
        try {
            String url = host + "/rcs/v1/message";
            String body = JSONObject.toJSONString(request);
            logger.info("sendRouteMobileRCSMsg request:" + url + ", body:" + body);
            String response = HttpUtil.postJsonWithBearerToken(url, body, jwt);
            logger.info("sendRouteMobileRCSMsg response:" + response);
            JSONObject json = JSONObject.parseObject(response);
            if (json != null && json.getString("id") != null) {
                return json.getString("id");
            }
        } catch (Exception e) {
            logger.error("sendRouteMobileRCSMsg throw exception", e);
        }
        return null;
    }

    /** Sinch OAuth2 认证域名：全局固定，不随 Conversation API 的 us./eu. 区域变化，故不进 admin_info。 */
    private static final String SINCH_AUTH_URL = "https://auth.sinch.com/oauth2/token";

    /**
     * Sinch OAuth2 client_credentials 换 access_token。
     * POST https://auth.sinch.com/oauth2/token，Authorization: Basic base64(keyId:keySecret)，
     * form-urlencoded: grant_type=client_credentials。
     * 返回整个 JSON（{access_token, expires_in, token_type, scope}）——令牌有效期由服务端
     * expires_in 决定，故不像 loginRouteMobile 那样只返回令牌串。失败返回 null。
     */
    public static final JSONObject loginSinch(String keyId, String keySecret) {
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("grant_type", "client_credentials");
            String basic = Base64.getEncoder().encodeToString(
                    (keyId + ":" + keySecret).getBytes("UTF-8"));
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Authorization", "Basic " + basic);
            logger.info("loginSinch request:" + SINCH_AUTH_URL);
            String response = HttpUtil.post(SINCH_AUTH_URL, params, headers);
            // 不打印 response 全文：内含 access_token
            JSONObject json = JSONObject.parseObject(response);
            if (json != null && json.getString("access_token") != null) {
                logger.info("loginSinch ok, expires_in:" + json.getInteger("expires_in"));
                return json;
            }
            logger.error("loginSinch failed, response:" + response);
        } catch (Exception e) {
            logger.error("loginSinch throw exception", e);
        }
        return null;
    }

    /**
     * Sinch 发送 RCS 消息。POST {base}/messages:send，Authorization: Bearer {token}。
     * base 形如 https://us.conversation.api.sinch.com/v1/projects/{projectId}（projectId 已含在 admin_info.url 里）。
     * 成功返回响应里的 message_id（channelMsgId），失败返回 null。
     */
    public static final String sendSinchRCSMsg(Map<String, Object> request, String base, String token) {
        try {
            String url = base + "/messages:send";
            String body = JSONObject.toJSONString(request);
            logger.info("sendSinchRCSMsg request:" + url + ", body:" + body);
            String response = HttpUtil.postJsonWithBearerToken(url, body, token);
            logger.info("sendSinchRCSMsg response:" + response);
            JSONObject json = JSONObject.parseObject(response);
            if (json != null && json.getString("message_id") != null) {
                return json.getString("message_id");
            }
        } catch (Exception e) {
            logger.error("sendSinchRCSMsg throw exception", e);
        }
        return null;
    }

    public static final boolean sendAvantRCSMsg(Map<String, Object> request, String host, String token) {
        boolean result = false;
    	try {
            String url = host+"/message/send";
            String body = JSONObject.toJSONString(request);            
            logger.info("sendAvantRCSMsg request:"+url +", body:"+body);
            String response = HttpUtil.postJsonWithAuthorization(url, body, token);
            logger.info("sendAvantRCSMsg response:"+response); 
			/*
			 * {"bulkId": "a28dd97c-2222-4fcf-99f1-0b557ed381da",
				  "messages": [
				    {
				      "messageId": "a28dd97c-1ffb-4fcf-99f1-0b557ed381da",
				      "status": {
				        "groupId": 1,
				        "groupName": "PENDING",
				        "id": 26,
				        "name": "PENDING_ACCEPTED",
				        "description": "Message sent to next instance"
				      },
				      "destination": "441134960001"
				    }
				  ]
				}
			 */
			/*
			 * Enum: "ENABLED" "UNREACHABLE" "AGENT_NOT_LAUNCHED" "REJECTED_NETWORK"
			 * "REJECTED_ROUTE_NOT_AVAILABLE" "REQUEST_FAILED" "INVALID_DESTINATION_ADDRESS"
			 * "UNKNOWN_CODE"
			 */
            JSONObject json = JSONObject.parseObject(response);            
            if(json!=null){
            	result = json.getBooleanValue("success");            		
            }
        } catch (Exception e) {
            logger.error("sendInfoBipRCSMsg throw exception", e);
            return result;
        }
        return result;
    }
    
    public static final boolean sendCMIMsg(Map<String, Object> request, String host, String token) {
        boolean result = false;
    	try {  
            String url = host+"/message/send";
            String body = JSONObject.toJSONString(request);            
            logger.info("sendCMIMsg request:"+url +", body:"+body);
            String response = HttpUtil.postJsonWithAuthorization(url, body, token);
            logger.info("sendCMIMsg response:"+response); 
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
			}
			 */
            JSONObject json = JSONObject.parseObject(response);            
            if(json!=null){
            	result = json.getBooleanValue("success");            		
            }
        } catch (Exception e) {
            logger.error("sendInfoBipRCSMsg throw exception", e);
            return result;
        }
        return result;
    }
    
    public static final String sendWaMsg(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl()+"/sendwamsg";          
            String body = JSONObject.toJSONString(request);
            logger.info("sendWaMsg request:"+url +", body:"+body);
            String response = HttpUtil.postJsonWithToken(url, body, admin.getToken());
            logger.info("sendWaMsg response:"+response); 
            /*if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("data")!=null&&"200".equalsIgnoreCase(String.valueOf(json.get("status")))) {
                	Map<String, String> result = (Map<String, String>)json.get("data");
                	return result.get("msgId");
                }                
            }*/
        }catch (Exception e) {
			logger.error("sendWaMsg throw error", e);			
        }
        return null;
    }
    
    public final static String md5Encrypt(String s) {  
  	  return encrypt(s,"MD5");
    }
    
    /**
     * md5 encryption
     * @param s
     * @return
     */
    public final static String encrypt(String s,String method) {  
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',  
                'a', 'b', 'c', 'd', 'e', 'f' };  
        try {  
            byte[] strTemp = s.getBytes();  
            MessageDigest md = MessageDigest.getInstance(method);   
            md.update(strTemp);  
            byte[] bytes = md.digest();  
            int j = bytes.length;  
            char str[] = new char[j * 2];  
            int k = 0;  
            for (int i = 0; i < j; i++) {  
                byte byte0 = bytes[i];  
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];  
                str[k++] = hexDigits[byte0 & 0xf];  
            }  
            return new String(str);  
        } catch (Exception e) {  
        	logger.warn("encrypt error with method "+method,e);
            return null;  
        }  
    }
    
    public static final String sendMsgToCMI(Map<String, Object> request, AdminInfo admin) {
        JSONObject json = null;
        try {           
            String url = admin.getUrl();          
            String body = JSONObject.toJSONString(request);
            logger.info("sendMsgToCMI request:"+url +", body:"+body);
            String response = HttpUtil.postJson(url, body);
            logger.info("sendMsgToCMI response:"+response); 
            if (response != null && StringUtils.isNotBlank(response)) {
                json = JSON.parseObject(response);
                if (json.get("uip_body")!=null) {
                	JSONObject jsonbody = json.getJSONObject("uip_body");
                 	JSONArray detailList = jsonbody.getJSONArray("DETAIL_LIST");
                	if(detailList!=null && detailList.getJSONObject(0)!=null) {
                		JSONObject detail =detailList.getJSONObject(0);
                		int resultCode = detail.getIntValue("RESULT_CODE");
                		if(resultCode == 0 || resultCode == 1) {
                			return detail.getString("SMS_UID");
                		}
                	}
                }              
            }
        }catch (Exception e) {
			logger.error("sendMsgToSim throw error", e);			
        }
        return "";
    }
    
    public static void main(String[] args) throws Exception {
		/*String response = "{ \"code\":0,     \"message\":\"SUCCESS\",\"ssid\": int     \"price\": {   \"country_code\": [0:float],\"country_code2\": [[10000,float1],[50000,float2],[90000,float3]],}  } ";
		System.out.println(response.indexOf("\"code\": 0"));
		Map<String, Map<String, String>> map = queryUnitPrice(11, "");
		if (map ==null) {
			System.out.println("null");
		}else {
			for (Map.Entry<String, Map<String, String>> entry: map.entrySet()) {
				System.out.println(entry.getKey());
				if (entry.getValue().size() >0) {
					System.out.println(entry.getValue().get("0"));
				}				
			}
		}	*/
    	/*File file = new File("C:\\king\\upload\\test3.txt");
    	List<String> numbers = FileUtils.readLines(file);
    	List<String> numbers = new ArrayList<>();
    	numbers.add("+918879458226");
    	
    	List<String> reList = new ArrayList<>();
    	for (int i = 0; i < numbers.size(); i++) {
    		AdminInfo adminInfo = new AdminInfo();
        	adminInfo.setNumberCheckUrl("http://8.210.140.175:7000/");
            boolean result = checkNum(adminInfo, "+"+numbers.get(i).trim());            
            if (result) {
            	reList.add(numbers.get(i));
            	//break;
            	if (reList.size()>3) {
            		reList.add("+"+numbers.get(i+1));
					break;
				}
			}
		}
    	//System.out.println("check number"+reList);
    	File file1 = new File("C:\\king\\upload\\test1.txt");
    	FileUtils.writeLines(file1, reList);*/
    	
    	/*String response ="{\"code\":200, \"reason\":\"OK\", \"type\":\"task-status\",\"status\":[{\"tid\":-1824437295,\"status\":\"0 OK\"}]}";
    	JSONObject json = JSON.parseObject(response);
        if (json.get("status")!=null&&"200".equalsIgnoreCase(String.valueOf(json.get("code")))) {
        	JSONArray jsonArray = json.getJSONArray("status");
        	System.out.println("0 OK".equalsIgnoreCase((String)((JSONObject)jsonArray.get(0)).get("status")));
        } */
    	String textContent = "dajlataljhttp://awwwe.dt.com/aate bakdakjteatoe";
        String []textarray = textContent.split("http");
        String contenturl = "http://";
        if(textarray!=null && textarray.length>1) {
        	String contenturlarray[] = textarray[1].split(" ");
        	contenturl="http"+contenturlarray[0];
        }
        String error = "1234448585\r\n";
        String result = error.replaceAll("\r\n", "");
    	System.out.println(result);
    	System.out.println(md5Encrypt("dddda32").toUpperCase());
    	
    	String response="{\"uip_head\":{\"METHOD\":\"SMS_SEND_REQUEST\",\"SERIAL\":1,\"CHANNEL\":\"AID_HTTPT1754615147WQUOE\",\"RESULT_CODE\":1,\"RESULT_DESC\":\"OK\"},\"uip_body\":{\"DETAIL_LIST\":[{\"SMS_UID\":\"G1860298CA763E863HCUKUAF8\",\"DESTINATION_ADDR\":\"923105193625\",\"RESULT_CODE\":1,\"RESULT_DESC\":\"OK\"}]},\"uip_version\":2}";
    	JSONObject json = JSON.parseObject(response);
         if (json.get("uip_body")!=null) {
        	 JSONObject jsonbody = json.getJSONObject("uip_body");
         	JSONArray detailList = jsonbody.getJSONArray("DETAIL_LIST");
         	System.out.println(detailList);
         	if(detailList!=null && detailList.getJSONObject(0)!=null) {
         		JSONObject detail =detailList.getJSONObject(0);
         		int resultCode = detail.getIntValue("RESULT_CODE");
         		if(resultCode == 0 || resultCode == 1) {
         			System.out.println(detail.getString("SMS_UID"));
         		}
         	}
         }
    }

}

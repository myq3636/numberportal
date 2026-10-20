package com.liugh.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RequestBody;

import com.liugh.base.EnumberDTO;
import com.liugh.base.MonitorCountryReport;
import com.liugh.base.MonitorDuplicateNumResponse;
import com.liugh.base.MonitorLatencyDistributeResponse;
import com.liugh.base.MonitorLatencyReport;
import com.liugh.base.MonitorReport;
import com.liugh.base.MonitorRequest;
import com.liugh.base.SMSLogging;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.service.entity.SubmitResponse;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author liugh123
 * @since 2019-06-24
 */
public interface ISmsCdrInfoService extends IService<SmsCdrInfo> {

	
	public List<Map<String, Object>> groupByCdrStatus(String filename);
	
	public Page<SmsCdrInfo> selectPageByConditionUser(Page<SmsCdrInfo> page, String number, String effectiveStartTime,
            String effectiveEndTime, AdminInfo admin);
	
	public Page<SmsCdrInfo> selectPageBySsid(Page<SmsCdrInfo> page, Integer statusCode, String effectiveStartTime,
            int order, String effectiveEndTime, int ssid);
	
	
	public String excel(AdminInfo admin, int statusCode, String startTime, String endTime, int order, int batch)
			throws Exception;
	public boolean sendMessage(JSONObject requestJson);
	public String sendWaMessage(JSONObject requestJson);
	public SubmitResponse sendRCSMessage(JSONObject requestJson);
	public SubmitResponse sendMessageToYulore(JSONObject msg);
	public SubmitResponse sendMessageToWABA(JSONObject msg);
	public SubmitResponse sendA2PMessage(JSONObject msg);
	public SubmitResponse sendBraxisVoiceMessage(JSONObject msg);
	public SubmitResponse sendOTTMessage(JSONObject msg);
	public SubmitResponse sendAvantMessage(JSONObject msg);
	public SubmitResponse sendCMIMessage(JSONObject msg);
	public SubmitResponse sendViberToMK(JSONObject msg);
	public SubmitResponse sendViberToTelSMS(JSONObject msg);
	public SubmitResponse sendZaloToMK(JSONObject msg);
	public SubmitResponse sendMessageToCMWABA(JSONObject msg);
	public SubmitResponse sendMessageToTelegram(JSONObject msg);
	public SubmitResponse sendMessageToMms(JSONObject msg);
	public SubmitResponse sendMYMMSToMK(JSONObject msg);
	public SubmitResponse sendMmsToMK(JSONObject msg);
	public SubmitResponse sendTTS(JSONObject msg);
	public SubmitResponse sendMessageToSimBank(JSONObject msg);
	public boolean receiveDRFromYulore(JSONObject msg);
	public boolean receiveDRFromSIM(JSONObject msg);
	public boolean receiveDRFromMMS(String msgid, String status);
	public boolean receiveMMSDRFromMK(String msgid, String status);
	public boolean receiveDRFromWABA(JSONObject msg);
	public boolean receiveDRFromCMWABA(JSONObject msg);
	public boolean receiveDRFromBrax(JSONObject msg);
	public boolean receiveDRFromTelegram(JSONObject msg);
	public boolean receiveDRFromAvantRCS(JSONObject msg);
	public boolean receiveDRFromTelSMSViber(JSONObject msg);
	public boolean receiveDRFromBraxisVoice(JSONObject msg);
	public boolean receiveDRFromInfoRCS(JSONObject msg);
	public boolean receiveDRFromAvant(JSONObject msg);
	public Map<String, Object> receiveDRFromCMI(JSONObject msg);
	public boolean receiveDRFromSIMBox(String send, String reciver);
	public boolean receiveMsgFromSIM(String send, String reciver, String content);
	public boolean receiveDRFromFCV(String msgid, String status, String phone);
	public boolean receiveDRFromTTS(String msgid, String status);
	public SubmitResponse sendAvantRCSMessage(JSONObject requestJson);
	public int existTable(String currTable);

	public int createNewTable(String currTable);

	public int insertCdr(String historyTable, SmsCdrInfo cdr);
	
	public int dropTable(String tableName);
	
}

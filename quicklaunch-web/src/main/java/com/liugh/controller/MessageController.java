package com.liugh.controller;

import java.io.BufferedReader;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.liugh.annotation.AccessLimit;
import com.liugh.annotation.CurrentUser;
import com.liugh.annotation.Log;
import com.liugh.annotation.Pass;
import com.liugh.annotation.ValidationParam;
import com.liugh.base.ChargeResponse;
import com.liugh.base.Constant;
import com.liugh.base.DailyReport;
import com.liugh.base.EnumberDTO;
import com.liugh.base.MonitorChartReport;
import com.liugh.base.MonitorCountryReport;
import com.liugh.base.MonitorCountryReportResponse;
import com.liugh.base.MonitorDuplicateNumResponse;
import com.liugh.base.MonitorLatencyDistributeResponse;
import com.liugh.base.MonitorLatencyReport;
import com.liugh.base.MonitorLatencyReportResponse;
import com.liugh.base.MonitorReport;
import com.liugh.base.MonitorRequest;
import com.liugh.base.OperatorInfoResponse;
import com.liugh.base.OperatorReportResponse;
import com.liugh.base.ProvinceReportWithOPResponse;
import com.liugh.base.PublicResultConstant;
import com.liugh.base.Result;
import com.liugh.base.RoutingInfoResponse;
import com.liugh.base.SMSLogging;
import com.liugh.base.TexterResponse;
import com.liugh.config.IOtools;
import com.liugh.config.ResponseHelper;
import com.liugh.config.ResponseModel;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.NumberInfo;
import com.liugh.entity.SmsCdrInfo;
import com.liugh.service.IAdminInfoService;
import com.liugh.service.INumberInfoService;
import com.liugh.service.ISmsCdrInfoService;
import com.liugh.service.entity.SubmitResponse;
import com.liugh.service.entity.UnitPriceResponse;
import com.liugh.service.util.CountryInfoDictCacheManager;
import com.liugh.util.ComUtil;
import com.liugh.util.DateTimeUtil;
import com.liugh.util.QuotaManagerUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.tools.zip.ZipOutputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author liugh123
 * @since 2019-01-04
 */
@RestController
@Slf4j
public class MessageController {
 
	 
    @Autowired
    IAdminInfoService adminInfoService;	
	
	
	@Autowired
	private ISmsCdrInfoService smsCdrInfoService;
	
	@Autowired
	private INumberInfoService numberInfoService;
	
	
	
	@Value("${backend.monitor.url}")
	private String monitorURL;
	
	@Value("${project.url}")
	 private String projectURL;

	
	@PostConstruct
    public void init(){		
		
	}
	
	@ApiOperation(value="接收状态报告", notes="",produces = "application/json")
	@PostMapping("/message/yuloredr")
	@Pass
	public ResponseModel<Map<String, Object>> receiveDRFromYulore(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/yuloredr' interface:{}",xml);
		boolean result = smsCdrInfoService.receiveDRFromYulore(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendtoyulore")
	@Pass
	public ResponseModel<Map<String, Object>> sendMsgToYulore(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendtoyulore' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendMessageToYulore(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendtomkwa")
	@Pass
	public ResponseModel<Map<String, Object>> sendMsgToMKWABA(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendtomkwa' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendMessageToWABA(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/senda2psms")
	@Pass
	public ResponseModel<Map<String, Object>> sendA2PMsg(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/senda2psms' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendA2PMessage(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendtocmwa")
	@Pass
	public ResponseModel<Map<String, Object>> sendMsgToCMWABA(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendtocmwa' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendMessageToCMWABA(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendtomms")
	@Pass
	public ResponseModel<Map<String, Object>> sendMsgToMms(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendtomms' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendMessageToMms(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendmymmstomk")
	@Pass
	public ResponseModel<Map<String, Object>> sendMYMMsToMK(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendmymmstomk' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendMYMMSToMK(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendmms")
	@Pass
	public ResponseModel<Map<String, Object>> sendMmsToMK(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendMmsToMK' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendMmsToMK(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收MMS状态报告", notes="",produces = "application/json")
	@GetMapping("/message/mmsdr")
	@Pass
	public String receiveMMSDRFromMK(@RequestParam(name = "msgid") String msgid,
			@RequestParam(name = "msisdn") String msisdn, @RequestParam(name = "result") String rs,
			@RequestParam(name = "response") String response, @RequestParam(name = "userid") String userid,
			@RequestParam(name = "timestamp") Long timestamp		
			) throws Exception{
		log.info("received msg from '/message/mmsdr' interface:{},{},{},{}",msgid, timestamp, msisdn, response);
		boolean result = smsCdrInfoService.receiveMMSDRFromMK(msgid, response);
		if (result) {
			return "SUCCESS";
		}else{
			return "failed";
		}

	}
	
	
	@ApiOperation(value="接收SIM消息", notes="",produces = "application/json")
	@PostMapping("/message/sendtosim")
	@Pass
	public ResponseModel<Map<String, Object>> sendMsgToSIMBank(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendtosim' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendMessageToSimBank(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收TeddyMessage", notes="",produces = "application/json")
	@PostMapping("/message/sendtoTeddy")
	@Pass
	public ResponseModel<Map<String, Object>> sendMsgToTeddy(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendtoTeddy' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendMessageToYulore(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收状态报告", notes="",produces = "application/json")
	@PostMapping("/message/wabadr")
	@Pass
	public ResponseModel<Map<String, Object>> receiveDRFromWABA(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/wabadr' interface:{}",xml);
		boolean result = smsCdrInfoService.receiveDRFromWABA(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收CMWA状态报告", notes="",produces = "application/json")
	@PostMapping("/message/cmwadr")
	@Pass
	public ResponseModel<Map<String, Object>> receiveDRFromCMWABA(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/cmwadr' interface:{}",xml);
		boolean result = smsCdrInfoService.receiveDRFromCMWABA(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收BRFCM状态报告", notes="",produces = "application/json")
	@GetMapping("/message/drfromfcm")
	@Pass
	public ResponseModel<Map<String, Object>> receiveDRFromFCM(@RequestParam(name = "idSMS") String msgid,
			@RequestParam(name = "fone") String msisdn, @RequestParam(name = "status") String status) throws Exception{
		log.info("received msg from '/message/drfromfcm' interface:msisdn={},msgid={},status={}",msisdn, msgid, status);
		boolean result = smsCdrInfoService.receiveDRFromFCV(msgid, status, msisdn);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@PostMapping("/message/simmsg")
	@Pass
	public void receiveMsgFromSim(HttpServletRequest request) throws Exception{
		// 直接从HttpServletRequest的Reader流中获取RequestBody
        BufferedReader reader = request.getReader();
        StringBuilder builder = new StringBuilder();
        String line = reader.readLine();
        String content = "";
        while(line != null){
            builder.append(line);
            content = line;
            line = reader.readLine();            
        }
        reader.close();

        String reqBody = builder.toString();
		log.info("received msg from '/message/simmsg' interface:{}",reqBody);
		String sender = request.getParameter("Sender");
		String receiver = request.getParameter("receiver");
		if(content!= null && content.contains("Sms Send to") && content.contains("Success")){
			receiver = content.replaceAll("[^0-9]","");
			smsCdrInfoService.receiveDRFromSIMBox(sender, receiver);
		}else{
			smsCdrInfoService.receiveMsgFromSIM(sender, receiver, content);
		}				
		return;

	}
	
	@ApiOperation(value="接收状态报告", notes="",produces = "application/json")
	@PostMapping("/message/simdr")
	@Pass
	public ResponseModel<Map<String, Object>> receiveDRFromSim(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/simdr' interface:{}",xml);
		boolean result = smsCdrInfoService.receiveDRFromSIM(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}	
	//http://www.yourdomainDNurl/receive.asp?msgID=123296707&msisdn=85212345678&status=DELIVERED
	@ApiOperation(value="接收状态报告", notes="",produces = "application/json")
	@GetMapping("/message/mkmmsdr")
	@Pass
	public ResponseModel<Map<String, Object>> receiveDRFromMMS(@RequestParam(name = "msgID") String msgid,
			@RequestParam(name = "msisdn") String msisdn, @RequestParam(name = "status") String status
			) throws Exception{
		log.info("received msg from '/message/mmsdr' msgId:{}, msisdn:{}, status:{}", msgid, msisdn, status);
		boolean result = smsCdrInfoService.receiveDRFromMMS(msgid, status);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendtts")
	@Pass
	public ResponseModel<Map<String, Object>> sendtts(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendtts' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendTTS(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendtomkzalo")
	@Pass
	public ResponseModel<Map<String, Object>> sendMsgToMKZalo(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendtomkzalo' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendZaloToMK(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendtomkviber")
	@Pass
	public ResponseModel<Map<String, Object>> sendMsgToMKViber(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendtomkviber' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendViberToMK(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收状态报告", notes="",produces = "application/json")
	@PostMapping("/message/zalodr")
	@Pass
	public ResponseModel<Map<String, Object>> receiveDRFromZalo(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/zalodr' interface:{}",xml);
		boolean result = smsCdrInfoService.receiveDRFromWABA(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收状态报告", notes="",produces = "application/json")
	@PostMapping("/message/viberdr")
	@Pass
	public ResponseModel<Map<String, Object>> receiveDRFromVibe(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/viberdr' interface:{}",xml);
		boolean result = smsCdrInfoService.receiveDRFromWABA(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendottsms")
	@Pass
	public ResponseModel<Map<String, Object>> sendottMsg(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendottsms' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendOTTMessage(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收braxottmsg状态报告", notes="",produces = "application/json")
	@PostMapping("/message/braxottdr")
	@Pass
	public ResponseModel<Map<String, Object>> receiveDRFromBrax(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/braxottdr' interface:{}",xml);
		boolean result = smsCdrInfoService.receiveDRFromBrax(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收RCS消息", notes="",produces = "application/json")
	@PostMapping("/rcs/receive")
	@Pass
	public ResponseModel<Map<String, Object>> rcsRecive(@RequestBody JSONObject requestJson) throws Exception{
		log.info("received msg from '/rcs/receive' interface:{}",requestJson);
		SubmitResponse result = smsCdrInfoService.sendRCSMessage(requestJson);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	@ApiOperation(value="接收RCS dr消息", notes="",produces = "application/json")
	@PostMapping("/rec/inforcsdr")
	@Pass
	public ResponseModel<Map<String, Object>> recinforcsDR(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/rec/inforcsdr' interface:{}",xml);		
		boolean result = smsCdrInfoService.receiveDRFromTelegram(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendtotelegram")
	@Pass
	public ResponseModel<Map<String, Object>> sendMsgToTelegram(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendtotelegram' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendMessageToTelegram(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/rec/telegramdr")
	@Pass
	public ResponseModel<Map<String, Object>> recTelegramDR(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/rec/telegramdr' interface:{}",xml);		
		boolean result = smsCdrInfoService.receiveDRFromTelegram(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/rec/avantdr")
	@Pass
	public ResponseModel<Map<String, Object>> recAvantDR(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/rec/avantdr' interface:{}",xml);		
		boolean result = smsCdrInfoService.receiveDRFromAvant(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/avantsms")
	@Pass
	public ResponseModel<Map<String, Object>> sendAvantMsg(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/avantsms' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendAvantMessage(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收avant RCS消息", notes="",produces = "application/json")
	@PostMapping("/avant/rcs")
	@Pass
	public ResponseModel<Map<String, Object>> recavantrcs(@RequestBody JSONObject requestJson) throws Exception{
		log.info("received msg from '/avant/rcs' interface:{}",requestJson);
		SubmitResponse result = smsCdrInfoService.sendAvantRCSMessage(requestJson);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	@ApiOperation(value="接收avant RCS dr消息", notes="",produces = "application/json")
	@PostMapping("/rec/avantrcsdr")
	@Pass
	public ResponseModel<Map<String, Object>> recavantrcsDR(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/rec/avantrcsdr' interface:{}",xml);		
		boolean result = smsCdrInfoService.receiveDRFromAvantRCS(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendtotelsmsviber")
	@Pass
	public ResponseModel<Map<String, Object>> sendMsgToTelSMSViber(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendtotelsmsviber' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendViberToTelSMS(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收Telsms viber dr消息", notes="",produces = "application/json")
	@PostMapping("/rec/telviberdr")
	@Pass
	public ResponseModel<Map<String, Object>> rectelviberDR(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/rec/telviberdr' interface:{}",xml);		
		boolean result = smsCdrInfoService.receiveDRFromTelSMSViber(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/sendbraxisvoice")
	@Pass
	public ResponseModel<Map<String, Object>> sendVoiceToBraxis(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/sendbraxisvoice' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendBraxisVoiceMessage(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收Telsms viber dr消息", notes="",produces = "application/json")
	@PostMapping("/rec/braxisvoicedr")
	@Pass
	public ResponseModel<Map<String, Object>> recbraxisvoiceDR(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/rec/braxisvoicedr' interface:{}",xml);		
		boolean result = smsCdrInfoService.receiveDRFromTelSMSViber(xml);
		if (result) {
			return ResponseHelper.successSimpleResponse("success");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/cmisms")
	@Pass
	public ResponseModel<Map<String, Object>> sendCMIMsg(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/message/cmisms' interface:{}",xml);
		SubmitResponse result = smsCdrInfoService.sendCMIMessage(xml);
		if (result.isStatus()) {
			return ResponseHelper.successSimpleResponse(result.getOutMsgId());
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/rec/cmidr")
	@Pass
	public String recCMIDR(@RequestBody JSONObject xml) throws Exception{
		log.info("received msg from '/rec/cmidr' interface:{}",xml);		
		Map<String,Object> result = smsCdrInfoService.receiveDRFromCMI(xml);
		return JSONObject.toJSONString(result);
	}

}


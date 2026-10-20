package com.liugh.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

import com.liugh.service.entity.UnitPriceResponse;
import com.liugh.service.impl.TTSManager;
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
@Api(description="管理端模块")
@RequestMapping("/admin")
@Slf4j
public class AdminController {
 
	 
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

	@Autowired
	public TTSManager ttsManager;
	
	@PostConstruct
    public void init(){		
		try {
			adminInfoService.loadAdminToSystem();
		} catch (Exception e) {
			log.error("load admininfo to system throw error.", e);
		}
		
		
		try {
			ttsManager.init();
		} catch (Exception e) {
			log.error("TTSManager client init throw error.", e);
		}
		 
	}

    
    @GetMapping("/number/list")
    @ApiOperation(value="获取号码列表", notes="需要header里加入Authorization")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageIndex", value = "第几页"
                    , dataType = "String",paramType="query"),
            @ApiImplicitParam(name = "pageSize", value = "每页多少条"
                    , dataType = "String",paramType="query")            
    })
    public ResponseModel findcdrList(@RequestParam(name = "pageIndex", defaultValue = "1", required = false) Integer pageIndex,
                                     @RequestParam(name = "pageSize", defaultValue = "10", required = false) Integer pageSize,                                
                                     @RequestParam(name = "number",  required = false) String number,                                
                                     @CurrentUser AdminInfo admin) throws Exception{
        //自定义分页关联查询列表
        Page<NumberInfo> numbers = numberInfoService.selectPageBySsid(new Page<NumberInfo>(pageIndex, pageSize), admin, number);
        return ResponseHelper.buildSuccessfulResponseModel(numbers);
    }
    

    
	@ApiOperation(value="管理员注册", notes="body体参数,不需要Authorization",produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "requestJson", value = "{\"username\":\"13888888888\",\"password\":\"123456\"，\"nickname\":\"XiaoMing\",\"imgBase64\":\"xxxxxx\",\"description\":\"管理员\",\"phoneNumber\":\"13888888888\"}"
                    , required = true, dataType = "String",paramType="body")
    })
    @PostMapping("/admin/register")
    @Log(action="管理员注册",modelName= "管理员管理模块",description="管理员注册接口")
    @Pass
    public ResponseModel<Map<String, Object>> register(@RequestBody JSONObject requestJson) throws Exception{
    	try{
    		adminInfoService.register(requestJson);
    	}catch(Exception e){
    		return ResponseHelper.errorResponse(Result.USERNAME_IS_EXISTED);
    	}
        return ResponseHelper.successSimpleResponse("管理员已注册，请等待审批结果！");
    }
	
	@ApiOperation(value="管理员登录", notes="body体参数,不需要Authorization",produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "requestJson", value = "{\"username\":\"13888888888\",\"password\":\"123456\"}"
                    , required = true, dataType = "String",paramType="body")
    })
    @PostMapping("/admin/login")
    @Log(action="管理员登录",modelName= "管理员管理模块",description="管理员登录接口")
    @Pass
    public ResponseModel<Map<String, Object>> login(@RequestBody JSONObject requestJson) throws Exception{
		Map<String, Object> obj = adminInfoService.checkAccountAndPasswd(requestJson);
		if (obj.isEmpty()){
			// 管理员未认证通过
			return ResponseHelper.errorResponse(Result.WAIT_APPLY);
		}else{
			// 登录成功
			return ResponseHelper.buildSuccessfulResponseModel(obj);
		}
    }
	
	@ApiOperation(value="管理员登出", notes="body体参数,不需要Authorization",produces = "application/json")
    @GetMapping("/admin/logout")
    @Log(action="管理员登出",modelName= "管理员管理模块",description="管理员登出接口")
    @Pass
    public ResponseModel<Map<String, Object>> loginout(@CurrentUser AdminInfo admin) throws Exception{
		adminInfoService.logout(admin);
		return ResponseHelper.successSimpleResponse("退出登录成功！");
    }
	
	@GetMapping("/admin/showlist")
	@ApiImplicitParams({
        @ApiImplicitParam(name = "pageIndex", value = "第几页"
                , dataType = "String",paramType="query"),
        @ApiImplicitParam(name = "pageSize", value = "每页多少条"
                , dataType = "String",paramType="query")
	})
    public ResponseModel showAdminList(
    		@RequestParam(name = "pageIndex", defaultValue = "1", required = false) Integer pageIndex,
            @RequestParam(name = "pageSize", defaultValue = "10", required = false) Integer pageSize,
            @CurrentUser AdminInfo admin) throws Exception{
		Page<AdminInfo> adminPage = adminInfoService.selectPageWithCondition(new Page<AdminInfo>(pageIndex, pageSize), admin);
        return ResponseHelper.buildSuccessfulResponseModel(adminPage);
    }
	
	@GetMapping("/admin/list")
	public ResponseModel findAllList(@CurrentUser AdminInfo admin) throws Exception{
		if (admin.getRole()!=1) {
			return ResponseHelper.errorSimpleResponse("权限不够");
		}
		List<AdminInfo> adminPage = adminInfoService.selectList(new EntityWrapper<AdminInfo>().eq("role", 0).eq("status", 1));
        return ResponseHelper.buildSuccessfulResponseModel(adminPage);
    }

	
	@GetMapping("/admin/info")
    @Log(action="查看管理员信息",modelName= "管理员管理模块",description="查看管理员信息接口")
    @Pass
    public ResponseModel<Map<String, Object>> showInfo(@CurrentUser AdminInfo admin) throws Exception{
        return ResponseHelper.buildSuccessfulResponseModel(adminInfoService.showAdminInfo(admin));
    }
	
	@PostMapping("/admin/save")
    @Log(action="修改保存管理员信息",modelName= "管理员管理模块",description="修改保存管理员信息接口")
	@ApiImplicitParams({
        @ApiImplicitParam(name = "requestJson", value = "{\"password\":\"123456\"，\"nickname\":\"XiaoMing\",\"imgBase64\":\"xxxxxx\",\"description\":\"管理员\",\"phoneNumber\":\"13888888888\"}"
                , required = true, dataType = "String",paramType="body")
	})
    @Pass
    public ResponseModel<Map<String, Object>> save(@CurrentUser AdminInfo admin,
            @RequestBody JSONObject requestJson) throws Exception{
		adminInfoService.save(admin, requestJson);
		return ResponseHelper.successSimpleResponse("修改信息成功！");
    }
	
	
	@PostMapping("/admin/credit")
    @Log(action="修改保存管理员信息",modelName= "管理员管理模块",description="修改保存管理员信息接口")
	@ApiImplicitParams({
        @ApiImplicitParam(name = "requestJson", value = "{\"password\":\"123456\"，\"nickname\":\"XiaoMing\",\"imgBase64\":\"xxxxxx\",\"description\":\"管理员\",\"phoneNumber\":\"13888888888\"}"
                , required = true, dataType = "String",paramType="body")
	})
    //@Pass
    public ResponseModel<Map<String, Object>> credit(@CurrentUser AdminInfo admin,
            @RequestBody JSONObject requestJson) throws Exception{
		adminInfoService.credit(admin, requestJson);
		return ResponseHelper.successSimpleResponse("充值成功！");
    }

	
	
	@ApiOperation(value="保存号码", notes="",produces = "application/json")
    @PostMapping("/number/save")
    //@Log(action="发送消息",modelName= "消息管理模块",description="创建消息接口")
	//@Pass
	public ResponseModel<Map<String, Object>> saveNumber(@CurrentUser AdminInfo admin, @RequestParam("file")MultipartFile file,
			@RequestParam("customerId") int customerId, @RequestParam("msisdn") String msisdn, @RequestParam("expireTime") String dateTime) throws Exception{
		AdminInfo adminInfo = adminInfoService.selectById(customerId);
		if (adminInfo == null) {
			ResponseHelper.errorSimpleResponse("找不到对应客户,保存号码失败");
		}
		numberInfoService.saveNumbers(admin, file, customerId, msisdn, dateTime);
        return ResponseHelper.successSimpleResponse("保存成功");
    }

	

    //TODO
	@ApiOperation(value="接收消息", notes="",produces = "application/json")
	@PostMapping("/message/receive")
	@Pass
	public ResponseModel<Map<String, Object>> receive(@RequestBody JSONObject requestJson) throws Exception{
		log.info("received msg from '/message/receive' interface:{}",requestJson);
		if (smsCdrInfoService.sendMessage(requestJson)) {
			return ResponseHelper.successSimpleResponse("OK");
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
	
	@ApiOperation(value="接收whatsapp消息", notes="",produces = "application/json")
	@PostMapping("/whatsapp/receive")
	@Pass
	public ResponseModel<Map<String, Object>> waRecive(@RequestBody JSONObject requestJson) throws Exception{
		log.info("received msg from '/whatsapp/receive' interface:{}",requestJson);
		String outMsgId = smsCdrInfoService.sendWaMessage(requestJson);
		if (StringUtils.isNoneBlank(outMsgId)) {
			return ResponseHelper.successSimpleResponse(outMsgId);
		}else{
			return ResponseHelper.errorSimpleResponse("failed");
		}

	}
		
	
	@GetMapping("/message/list")
    @ApiOperation(value="获取cdr列表", notes="需要header里加入Authorization")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageIndex", value = "第几页"
                    , dataType = "String",paramType="query"),
            @ApiImplicitParam(name = "pageSize", value = "每页多少条"
                    , dataType = "String",paramType="query"),
            @ApiImplicitParam(name = "groupIds", value = "分组查询，组id列表，多个id用','分割"
            , dataType = "String",paramType="query"),
            @ApiImplicitParam(name = "startTime", value = "开始时间"
                    , dataType = "Long",paramType="query"),
            @ApiImplicitParam(name = "endTime", value = "结束时间"
                    , dataType = "Long",paramType="query")
    })
    public ResponseModel findcdrList(@RequestParam(name = "pageIndex", defaultValue = "1", required = false) Integer pageIndex,
                                     @RequestParam(name = "pageSize", defaultValue = "10", required = false) Integer pageSize,                                     
                                     @RequestParam(name = "startTime", defaultValue = "", required = false) String startTime,
                                     @RequestParam(name = "number", required = false) String number,
                                     @RequestParam(name = "endTime", defaultValue = "", required = false) String endTime, @CurrentUser AdminInfo admin) throws Exception{
        //自定义分页关联查询列表
        //TODO
        Page<SmsCdrInfo> userPage = smsCdrInfoService.selectPageByConditionUser(new Page<SmsCdrInfo>(pageIndex, pageSize),number, startTime, endTime, admin);
        return ResponseHelper.buildSuccessfulResponseModel(userPage);
    }
	
	

}


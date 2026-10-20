package com.liugh.controller;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONObject;
import com.liugh.annotation.Pass;
import com.liugh.config.ResponseHelper;
import com.liugh.config.ResponseModel;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsFile;
import com.liugh.rcs.service.RcsAuthHelper;
import com.liugh.rcs.service.RcsCallbackService;
import com.liugh.rcs.service.RcsFileService;
import com.liugh.rcs.service.RcsInfobipSubscriptionEventService;
import com.liugh.rcs.service.RcsMessageService;
import com.liugh.service.entity.SubmitResponse;

import io.swagger.annotations.ApiOperation;

import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * RCS 北向接口（P1 Infobip 通道）：发送 / 文件 / 入站回调。
 * server.context-path=/api/v1 → 实际路径为 /api/v1/rcs/**
 */
@RestController
@Slf4j
public class RcsController {

    @Autowired
    private RcsMessageService rcsMessageService;
    @Autowired
    private RcsCallbackService rcsCallbackService;
    @Autowired
    private RcsFileService rcsFileService;
    @Autowired
    private RcsInfobipSubscriptionEventService
            infobipSubscriptionEventService;
    @Value("${rcs.infobip-subscriptions.callback-token:}")
    private String infobipSubscriptionCallbackToken;

    @ApiOperation(value = "发送RCS消息(文本/富媒体)", notes = "", produces = "application/json")
    @PostMapping("/rcs/message/send")
    @Pass
    public ResponseModel<Map<String, Object>> send(@RequestBody JSONObject request) throws Exception {
        log.info("received request at '/rcs/message/send'");
        SubmitResponse result = rcsMessageService.send(request);
        if (result.isStatus()) {
            return ResponseHelper.successSimpleResponse(result.getOutMsgId());
        }
        return ResponseHelper.errorSimpleResponse("failed");
    }

    @ApiOperation(value = "上传RCS媒体文件", notes = "", produces = "application/json")
    @PostMapping("/rcs/files")
    @Pass
    public ResponseModel<JSONObject> uploadFile(@RequestParam("file") MultipartFile file,
            @RequestParam(value = "token", required = false) String token) throws Exception {
        AdminInfo customer = RcsAuthHelper.resolveCustomer(token);
        if (customer == null || customer.getSsid() == null) {
            return ResponseHelper.errorSimpleResponse("failed");
        }
        RcsFile rf = rcsFileService.upload(customer.getSsid(), file.getBytes(),
                file.getOriginalFilename(), file.getContentType());
        JSONObject data = new JSONObject();
        data.put("name", "files/" + rf.getUid());
        data.put("fileUri", rf.getFileUri());
        return ResponseHelper.buildSuccessfulResponseModel(data);
    }

    @ApiOperation(value = "登记RCS媒体URL(透传)", notes = "", produces = "application/json")
    @PostMapping("/rcs/files/url")
    @Pass
    public ResponseModel<JSONObject> registerFile(@RequestBody JSONObject request) throws Exception {
        String token = request.getString("Token") != null ? request.getString("Token") : request.getString("token");
        AdminInfo customer = RcsAuthHelper.resolveCustomer(token);
        if (customer == null || customer.getSsid() == null) {
            return ResponseHelper.errorSimpleResponse("failed");
        }
        RcsFile rf = rcsFileService.register(customer.getSsid(), request.getString("fileUrl"),
                request.getString("thumbnailUrl"), request.getString("mimeType"));
        JSONObject data = new JSONObject();
        data.put("name", "files/" + rf.getUid());
        data.put("fileUri", rf.getFileUri());
        return ResponseHelper.buildSuccessfulResponseModel(data);
    }

    @ApiOperation(value = "读取RCS媒体文件原始字节", notes = "", produces = "application/octet-stream")
    @GetMapping("/rcs/files/{uid}/raw")
    @Pass
    public void rawFile(@PathVariable("uid") String uid, HttpServletResponse response) throws Exception {
        RcsFile rf = rcsFileService.getByUid(uid);
        byte[] data = rcsFileService.loadRaw(uid);
        if (rf == null || data == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (rf.getMimeType() != null) {
            response.setContentType(rf.getMimeType());
        }
        response.setContentLength(data.length);
        OutputStream os = response.getOutputStream();
        os.write(data);
        os.flush();
    }

    @ApiOperation(value = "接收Infobip RCS回调(DR/已读/上行)", notes = "", produces = "application/json")
    @PostMapping("/rcs/callback/infobip")
    @Pass
    public ResponseModel<Map<String, Object>> callbackInfobip(@RequestBody JSONObject request) throws Exception {
        log.info("received callback at '/rcs/callback/infobip', request: {}", request);
        boolean result = rcsCallbackService.handle("INFOBIP", request);
        if (result) {
            return ResponseHelper.successSimpleResponse("success");
        }
        return ResponseHelper.errorSimpleResponse("failed");
    }

    @ApiOperation(value = "接收Infobip RCS异步能力查询结果", notes = "", produces = "application/json")
    @PostMapping("/rcs/callback/infobip/capabilities")
    @Pass
    public ResponseModel<Map<String, Object>> callbackInfobipCapabilities(
            @RequestBody JSONObject request) throws Exception {
        log.info("received Infobip /rcs/callback/infobip/capabilities, request: {}", request);
        boolean result = rcsCallbackService.handleCapabilityResults("INFOBIP", request);
        if (result) {
            return ResponseHelper.successSimpleResponse("success");
        }
        return ResponseHelper.errorSimpleResponse("failed");
    }

    @ApiOperation(value = "接收 Infobip 订阅管理事件",
            notes = "供应商内部回调，不属于客户接口",
            produces = "application/json")
    @PostMapping("/rcs/callback/infobip/subscriptions/{rssid}")
    @Pass
    public ResponseModel<Map<String, Object>> callbackInfobipSubscription(
            @PathVariable("rssid") Integer rssid,
            @RequestHeader(value = "X-RCS-Subscription-Token",
                    required = false) String callbackToken,
            @RequestBody JSONObject request,
            HttpServletResponse response) throws Exception {
        if (isBlank(infobipSubscriptionCallbackToken)) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return ResponseHelper.errorSimpleResponse("failed");
        }
        if (!secureEquals(infobipSubscriptionCallbackToken, callbackToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return ResponseHelper.errorSimpleResponse("failed");
        }
        boolean result = infobipSubscriptionEventService.handle(rssid, request);
        if (result) {
            return ResponseHelper.successSimpleResponse("success");
        }
        return ResponseHelper.errorSimpleResponse("failed");
    }

    void configureInfobipSubscriptionCallback(
            RcsInfobipSubscriptionEventService service, String token) {
        this.infobipSubscriptionEventService = service;
        this.infobipSubscriptionCallbackToken = token;
    }

    @ApiOperation(value = "接收Route Mobile RCS回调(DR/上行)", notes = "", produces = "application/json")
    @PostMapping("/rcs/callback/routemobile")
    @Pass
    public ResponseModel<Map<String, Object>> callbackRouteMobile(@RequestBody JSONObject request) throws Exception {
        log.info("received callback at '/rcs/callback/routemobile', request: {}", request);
        boolean result = rcsCallbackService.handle("ROUTEMOBILE", request);
        if (result) {
            return ResponseHelper.successSimpleResponse("success");
        }
        return ResponseHelper.errorSimpleResponse("failed");
    }

    @ApiOperation(value = "接收Sinch RCS回调(DR/上行)", notes = "", produces = "application/json")
    @PostMapping("/rcs/callback/sinch")
    @Pass
    public ResponseModel<Map<String, Object>> callbackSinch(@RequestBody JSONObject request) throws Exception {
        log.info("received callback at '/rcs/callback/sinch', request: {}", request);
        boolean result = rcsCallbackService.handle("SINCH", request);
        if (result) {
            return ResponseHelper.successSimpleResponse("success");
        }
        return ResponseHelper.errorSimpleResponse("failed");
    }
    
    @ApiOperation(value = "接收Avant RCS回调(DR)", notes = "", produces = "application/json")
    @PostMapping("/rcs/callback/avant")
    @Pass
    public ResponseModel<Map<String, Object>> callbackAvant(@RequestBody JSONObject request) throws Exception {
        log.info("received callback at '/rcs/callback/avant'");
        boolean result = rcsCallbackService.handle("AVANT", request);
        if (result) {
            return ResponseHelper.successSimpleResponse("success");
        }
        return ResponseHelper.errorSimpleResponse("failed");
    }

    private static boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

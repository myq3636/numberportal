package com.liugh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.liugh.annotation.Pass;
import com.liugh.config.ResponseHelper;
import com.liugh.config.ResponseModel;
import com.liugh.rcs.service.RcsCustomerAccessException;
import com.liugh.rcs.service.RcsCustomerEventQueryService;
import com.liugh.rcs.service.RcsDeliveryConfigService;

@RestController
public class RcsDeliveryConfigController {

    private final RcsDeliveryConfigService configService;
    private final RcsCustomerEventQueryService eventQueryService;

    @Autowired
    public RcsDeliveryConfigController(RcsDeliveryConfigService configService,
            RcsCustomerEventQueryService eventQueryService) {
        this.configService = configService;
        this.eventQueryService = eventQueryService;
    }

    @GetMapping("/rcs/delivery-config/query")
    @Pass
    public ResponseModel<JSONObject> get(
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        try {
            return ResponseHelper.buildSuccessfulResponseModel(
                    configService.get(RcsControllerResponseHelper.auth(
                            upperToken, lowerToken)));
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }

    @PostMapping("/rcs/delivery-config/save")
    @Pass
    public ResponseModel<JSONObject> save(@RequestBody JSONObject request) {
        try {
            return ResponseHelper.buildSuccessfulResponseModel(
                    configService.save(request));
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }

    @PostMapping("/rcs/delivery-config/test")
    @Pass
    public ResponseModel<JSONObject> test(@RequestBody JSONObject request) {
        try {
            return ResponseHelper.buildSuccessfulResponseModel(
                    eventQueryService.createTest(request));
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }
}

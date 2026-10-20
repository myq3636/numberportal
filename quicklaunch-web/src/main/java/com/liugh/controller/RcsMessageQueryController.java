package com.liugh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.liugh.annotation.Pass;
import com.liugh.config.ResponseHelper;
import com.liugh.config.ResponseModel;
import com.liugh.rcs.service.RcsCustomerAccessException;
import com.liugh.rcs.service.RcsMessageQueryService;

@RestController
public class RcsMessageQueryController {

    private final RcsMessageQueryService messageQueryService;

    @Autowired
    public RcsMessageQueryController(RcsMessageQueryService messageQueryService) {
        this.messageQueryService = messageQueryService;
    }

    @GetMapping("/rcs/messages/{messageId}")
    @Pass
    public ResponseModel<JSONObject> get(
            @PathVariable("messageId") String messageId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        try {
            return ResponseHelper.buildSuccessfulResponseModel(
                    messageQueryService.get(RcsControllerResponseHelper.auth(
                            upperToken, lowerToken), messageId));
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }
}

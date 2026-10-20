package com.liugh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.liugh.annotation.Pass;
import com.liugh.config.ResponseHelper;
import com.liugh.config.ResponseModel;
import com.liugh.rcs.service.RcsCapabilityService;
import com.liugh.rcs.service.RcsCustomerAccessException;

import io.swagger.annotations.ApiOperation;

/**
 * Unified customer capability APIs. Customers authenticate with the existing
 * Token/token value and never select or see a provider channel.
 */
@RestController
public class RcsCapabilityController {

    private final RcsCapabilityService capabilityService;

    @Autowired
    public RcsCapabilityController(RcsCapabilityService capabilityService) {
        this.capabilityService = capabilityService;
    }

    @ApiOperation(value = "同步查询RCS号码能力", produces = "application/json")
    @PostMapping("/rcs/capabilities/query")
    @Pass
    public ResponseModel<JSONObject> query(@RequestBody JSONObject request) {
        try {
            return ResponseHelper.buildSuccessfulResponseModel(
                    capabilityService.query(request));
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }

    @ApiOperation(value = "创建异步RCS号码能力查询批次", produces = "application/json")
    @PostMapping("/rcs/capabilities/batches")
    @Pass
    public ResponseModel<JSONObject> createBatch(@RequestBody JSONObject request) {
        try {
            return ResponseHelper.buildSuccessfulResponseModel(
                    capabilityService.createBatch(request));
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }

    @ApiOperation(value = "查询RCS号码能力批次", produces = "application/json")
    @GetMapping("/rcs/capabilities/batches/{batchId}")
    @Pass
    public ResponseModel<JSONObject> getBatch(
            @PathVariable("batchId") String batchId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        JSONObject auth = new JSONObject();
        if (upperToken != null) {
            auth.put("Token", upperToken);
        }
        if (lowerToken != null) {
            auth.put("token", lowerToken);
        }
        try {
            return ResponseHelper.buildSuccessfulResponseModel(
                    capabilityService.getBatch(auth, batchId));
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }
}

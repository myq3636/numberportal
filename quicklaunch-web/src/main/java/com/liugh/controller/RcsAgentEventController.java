package com.liugh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.liugh.annotation.Pass;
import com.liugh.config.ResponseHelper;
import com.liugh.config.ResponseModel;
import com.liugh.rcs.service.RcsAgentEventService;
import com.liugh.rcs.service.RcsCustomerAccessException;

import io.swagger.annotations.ApiOperation;

/**
 * Unified customer API for RCS typing and seen events.
 */
@RestController
public class RcsAgentEventController {

    private final RcsAgentEventService eventService;

    @Autowired
    public RcsAgentEventController(RcsAgentEventService eventService) {
        this.eventService = eventService;
    }

    @ApiOperation(value = "发送RCS输入中或已读事件", produces = "application/json")
    @PostMapping("/rcs/events/send")
    @Pass
    public ResponseModel<JSONObject> send(@RequestBody JSONObject request) {
        try {
            return ResponseHelper.buildSuccessfulResponseModel(
                    eventService.send(request));
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }
}

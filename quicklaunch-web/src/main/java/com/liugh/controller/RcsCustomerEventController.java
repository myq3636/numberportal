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
import com.liugh.rcs.service.RcsCustomerAccessException;
import com.liugh.rcs.service.RcsCustomerEventQueryService;

@RestController
public class RcsCustomerEventController {

    private final RcsCustomerEventQueryService eventService;

    @Autowired
    public RcsCustomerEventController(RcsCustomerEventQueryService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/rcs/events/list")
    @Pass
    public ResponseModel<JSONObject> list(
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        try {
            return ResponseHelper.buildSuccessfulResponseModel(
                    eventService.list(RcsControllerResponseHelper.auth(
                            upperToken, lowerToken), page, pageSize));
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }

    @GetMapping("/rcs/events/{eventId}")
    @Pass
    public ResponseModel<JSONObject> get(
            @PathVariable("eventId") String eventId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        try {
            return ResponseHelper.buildSuccessfulResponseModel(
                    eventService.get(RcsControllerResponseHelper.auth(
                            upperToken, lowerToken), eventId));
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }

    @PostMapping("/rcs/events/{eventId}/retry")
    @Pass
    public ResponseModel<JSONObject> retry(
            @PathVariable("eventId") String eventId,
            @RequestBody JSONObject request) {
        try {
            return ResponseHelper.buildSuccessfulResponseModel(
                    eventService.retry(request, eventId));
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }
}

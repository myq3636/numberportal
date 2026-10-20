package com.liugh.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONObject;
import com.liugh.annotation.Pass;
import com.liugh.config.ResponseHelper;
import com.liugh.config.ResponseModel;
import com.liugh.rcs.service.RcsCustomerAccessException;
import com.liugh.rcs.service.RcsLaunchManagementService;

@RestController
public class RcsLaunchManagementController {

    private final RcsLaunchManagementService service;

    @Autowired
    public RcsLaunchManagementController(RcsLaunchManagementService service) {
        this.service = service;
    }

    @PostMapping("/rcs/senders/{senderId}/launch-requirements")
    @Pass
    public ResponseModel<JSONObject> requirements(
            @PathVariable("senderId") final String senderId,
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.requirements(request, senderId);
            }
        });
    }

    @PostMapping("/rcs/senders/{senderId}/launch-documents")
    @Pass
    public ResponseModel<JSONObject> uploadDocument(
            @PathVariable("senderId") final String senderId,
            @RequestParam("document") MultipartFile document,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        try {
            final byte[] bytes = document.getBytes();
            final String fileName = document.getOriginalFilename();
            final String contentType = document.getContentType();
            final JSONObject auth = RcsControllerResponseHelper.auth(
                    upperToken, lowerToken);
            return respond(new Action() {
                @Override public JSONObject execute() {
                    return service.uploadDocument(auth, senderId, bytes,
                            fileName, contentType);
                }
            });
        } catch (Exception e) {
            return RcsControllerResponseHelper.failure(
                    new RcsCustomerAccessException("INVALID_ARGUMENT"));
        }
    }

    @PostMapping("/rcs/senders/{senderId}/launch-requests/submit")
    @Pass
    public ResponseModel<JSONObject> submit(
            @PathVariable("senderId") final String senderId,
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.submit(request, senderId);
            }
        });
    }

    @GetMapping("/rcs/senders/{senderId}/launch-requests/list")
    @Pass
    public ResponseModel<JSONObject> list(
            @PathVariable("senderId") final String senderId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = RcsControllerResponseHelper.auth(
                upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.list(auth, senderId);
            }
        });
    }

    @GetMapping("/rcs/launch-requests/{requestId}/detail")
    @Pass
    public ResponseModel<JSONObject> get(
            @PathVariable("requestId") final String requestId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = RcsControllerResponseHelper.auth(
                upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.get(auth, requestId);
            }
        });
    }

    @PostMapping("/rcs/launch-requests/{requestId}/update")
    @Pass
    public ResponseModel<JSONObject> update(
            @PathVariable("requestId") final String requestId,
            @RequestBody final JSONObject request) {
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.update(request, requestId);
            }
        });
    }

    @PostMapping("/rcs/launch-requests/{requestId}/cancel")
    @Pass
    public ResponseModel<JSONObject> cancel(
            @PathVariable("requestId") final String requestId,
            @RequestParam(value = "Token", required = false) String upperToken,
            @RequestParam(value = "token", required = false) String lowerToken) {
        final JSONObject auth = RcsControllerResponseHelper.auth(
                upperToken, lowerToken);
        return respond(new Action() {
            @Override public JSONObject execute() {
                return service.cancel(auth, requestId);
            }
        });
    }

    private ResponseModel<JSONObject> respond(Action action) {
        try {
            return ResponseHelper.buildSuccessfulResponseModel(action.execute());
        } catch (RcsCustomerAccessException e) {
            return RcsControllerResponseHelper.failure(e);
        }
    }

    private interface Action {
        JSONObject execute();
    }
}

package com.liugh.controller;

import com.alibaba.fastjson.JSONObject;
import com.liugh.config.ResponseModel;
import com.liugh.rcs.service.RcsCustomerAccessException;

final class RcsControllerResponseHelper {

    private RcsControllerResponseHelper() {
    }

    static ResponseModel<JSONObject> failure(RcsCustomerAccessException error) {
        ResponseModel<JSONObject> response = new ResponseModel<JSONObject>();
        response.setCode(httpCode(error.getErrorCode()));
        response.setResult("FAIL");
        response.setMessage(error.getErrorCode());
        return response;
    }

    static JSONObject auth(String upperToken, String lowerToken) {
        JSONObject auth = new JSONObject();
        if (upperToken != null) {
            auth.put("Token", upperToken);
        }
        if (lowerToken != null) {
            auth.put("token", lowerToken);
        }
        return auth;
    }

    private static int httpCode(String errorCode) {
        if ("INVALID_TOKEN".equals(errorCode)) {
            return 401;
        }
        if ("FORBIDDEN_RESOURCE".equals(errorCode)) {
            return 403;
        }
        if ("RESOURCE_NOT_FOUND".equals(errorCode)) {
            return 404;
        }
        if ("INVALID_ARGUMENT".equals(errorCode)) {
            return 400;
        }
        if ("OPERATION_NOT_SUPPORTED".equals(errorCode)) {
            return 422;
        }
        if ("ROUTE_NOT_CONFIGURED".equals(errorCode)
                || "DUPLICATE_REQUEST".equals(errorCode)) {
            return 409;
        }
        if ("UPSTREAM_REQUEST_REJECTED".equals(errorCode)) {
            return 502;
        }
        return 500;
    }
}

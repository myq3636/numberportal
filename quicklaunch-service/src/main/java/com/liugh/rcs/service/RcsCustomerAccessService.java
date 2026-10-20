package com.liugh.rcs.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.AdminInfo;

/**
 * Shared customer authentication and tenant boundary checks for RCS APIs.
 */
@Service
public class RcsCustomerAccessService {

    private static final String INVALID_TOKEN = "INVALID_TOKEN";
    private static final String INVALID_ARGUMENT = "INVALID_ARGUMENT";
    private static final String FORBIDDEN_RESOURCE = "FORBIDDEN_RESOURCE";
    private static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";

    private boolean phase1Enabled = true;

    @Value("${rcs.phase1.enabled:true}")
    public void setPhase1Enabled(boolean phase1Enabled) {
        this.phase1Enabled = phase1Enabled;
    }

    public void requirePhase1Enabled() {
        if (!phase1Enabled) {
            throw new RcsCustomerAccessException("OPERATION_NOT_SUPPORTED");
        }
    }

    public AdminInfo requireCustomer(JSONObject request) {
        //validateCustomerPayload(request);
        String token = firstNonBlank(request == null ? null : request.getString("Token"),
                request == null ? null : request.getString("token"));
        AdminInfo customer = RcsAuthHelper.resolveCustomer(token);
        if (customer == null || customer.getSsid() == null) {
            throw new RcsCustomerAccessException(INVALID_TOKEN);
        }
        return customer;
    }

    public void validateCustomerPayload(JSONObject request) {
        validateNoRoutingFields(request);
    }

    private void validateNoRoutingFields(Object value) {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            Set<String> keys = object.keySet();
            for (String key : keys) {
                if ("channel".equalsIgnoreCase(key)
                        || "channelCode".equalsIgnoreCase(key)
                        || "rssid".equalsIgnoreCase(key)) {
                    throw new RcsCustomerAccessException(INVALID_ARGUMENT);
                }
                validateNoRoutingFields(object.get(key));
            }
        } else if (value instanceof JSONArray) {
            JSONArray values = (JSONArray) value;
            for (int i = 0; i < values.size(); i++) {
                validateNoRoutingFields(values.get(i));
            }
        }
    }

    public void requireOwned(Integer customerSsid, Integer resourceSsid) {
        if (resourceSsid == null) {
            throw new RcsCustomerAccessException(RESOURCE_NOT_FOUND);
        }
        if (customerSsid == null || !customerSsid.equals(resourceSsid)) {
            throw new RcsCustomerAccessException(FORBIDDEN_RESOURCE);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return null;
    }
}

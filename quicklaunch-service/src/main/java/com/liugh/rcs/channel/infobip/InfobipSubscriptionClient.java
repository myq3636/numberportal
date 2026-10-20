package com.liugh.rcs.channel.infobip;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

/**
 * Client for Infobip subscription profiles and channel subscriptions.
 */
@Component
public class InfobipSubscriptionClient {

    private static final String ROOT = "/subscriptions/1";

    private final InfobipManagementHttpExecutor http;

    @Autowired
    public InfobipSubscriptionClient(InfobipManagementHttpExecutor http) {
        this.http = http;
    }

    public JSONObject getProfile(String baseUrl, String apiKey,
            String profileId) {
        return call("GET", baseUrl, ROOT + "/profiles/"
                + encodeRequired(profileId, "profileId"), apiKey, null);
    }

    public JSONObject createProfile(String baseUrl, String apiKey,
            JSONObject body) {
        return call("POST", baseUrl, ROOT + "/profiles", apiKey, body);
    }

    public JSONObject updateProfile(String baseUrl, String apiKey,
            String profileId, JSONObject body) {
        return call("PUT", baseUrl, ROOT + "/profiles/"
                + encodeRequired(profileId, "profileId"), apiKey, body);
    }

    public JSONObject getSubscription(String baseUrl, String apiKey,
            String channel, String subscriptionId) {
        return call("GET", baseUrl, subscriptionPath(channel) + "/"
                + encodeRequired(subscriptionId, "subscriptionId"), apiKey,
                null);
    }

    public JSONObject createSubscription(String baseUrl, String apiKey,
            String channel, JSONObject body) {
        return call("POST", baseUrl, subscriptionPath(channel), apiKey, body);
    }

    public JSONObject updateSubscription(String baseUrl, String apiKey,
            String channel, String subscriptionId, JSONObject body) {
        return call("PUT", baseUrl, subscriptionPath(channel) + "/"
                + encodeRequired(subscriptionId, "subscriptionId"), apiKey,
                body);
    }

    private JSONObject call(String method, String baseUrl, String path,
            String apiKey, JSONObject body) {
        if (isBlank(baseUrl) || isBlank(apiKey)) {
            throw new IllegalArgumentException(
                    "Infobip baseUrl and apiKey are required");
        }
        String raw = http.request(method, trimTrailingSlash(baseUrl) + path,
                authorization(apiKey), body == null ? null : body.toJSONString());
        if (isBlank(raw)) {
            return new JSONObject();
        }
        Object parsed = JSON.parse(raw);
        if (!(parsed instanceof JSONObject)) {
            throw new IllegalStateException("Invalid JSON response from Infobip");
        }
        return (JSONObject) parsed;
    }

    private static String subscriptionPath(String channel) {
        return ROOT + "/subscription/"
                + encodeRequired(channel, "channel");
    }

    private static String encodeRequired(String value, String name) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(name + " is required");
        }
        try {
            return URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String authorization(String apiKey) {
        String value = apiKey.trim();
        return value.regionMatches(true, 0, "App ", 0, 4)
                ? value : "App " + value;
    }

    private static String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

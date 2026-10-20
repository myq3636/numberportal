package com.liugh.rcs.channel.infobip;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

/**
 * Infobip RCS capability-check client.
 */
@Component
public class InfobipCapabilityClient {

    private static final int SYNC_LIMIT = 500;
    private static final int ASYNC_LIMIT = 10000;

    private final InfobipHttpExecutor httpExecutor;

    @Autowired
    public InfobipCapabilityClient(InfobipHttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
    }

    public JSONObject query(String baseUrl, String apiKey, String sender,
            List<String> phoneNumbers) {
        validate(sender, phoneNumbers, SYNC_LIMIT);
        JSONObject body = baseBody(sender, phoneNumbers);
        return post(baseUrl, "/rcs/2/capability-check/query", apiKey, body);
    }

    public JSONObject notify(String baseUrl, String apiKey, String sender,
            List<String> phoneNumbers, String notifyUrl) {
        validate(sender, phoneNumbers, ASYNC_LIMIT);
        if (isBlank(notifyUrl)) {
            throw new IllegalArgumentException("notifyUrl is required");
        }
        JSONObject body = baseBody(sender, phoneNumbers);
        body.put("notifyUrl", notifyUrl);
        body.put("notifyContentType", "application/json");
        return post(baseUrl, "/rcs/2/capability-check/notify", apiKey, body);
    }

    private JSONObject post(String baseUrl, String path, String apiKey, JSONObject body) {
        if (isBlank(baseUrl) || isBlank(apiKey)) {
            throw new IllegalArgumentException("Infobip baseUrl and apiKey are required");
        }
        String response = httpExecutor.post(trimTrailingSlash(baseUrl) + path,
                authorization(apiKey), body.toJSONString());
        if (isBlank(response)) {
            throw new IllegalStateException("Empty response from Infobip");
        }
        try {
            return JSONObject.parseObject(response);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON response from Infobip", e);
        }
    }

    private static JSONObject baseBody(String sender, List<String> phoneNumbers) {
        JSONObject body = new JSONObject();
        body.put("sender", sender);
        body.put("phoneNumbers", phoneNumbers);
        return body;
    }

    private static void validate(String sender, List<String> phoneNumbers, int limit) {
        if (isBlank(sender)) {
            throw new IllegalArgumentException("sender is required");
        }
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            throw new IllegalArgumentException("phoneNumbers are required");
        }
        if (phoneNumbers.size() > limit) {
            throw new IllegalArgumentException("phoneNumbers exceeds limit " + limit);
        }
    }

    private static String authorization(String apiKey) {
        String value = apiKey.trim();
        return value.regionMatches(true, 0, "App ", 0, 4) ? value : "App " + value;
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

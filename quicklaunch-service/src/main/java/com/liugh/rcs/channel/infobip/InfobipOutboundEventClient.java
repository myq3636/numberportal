package com.liugh.rcs.channel.infobip;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * Infobip RCS outbound event client.
 */
@Component
public class InfobipOutboundEventClient {

    private static final String EVENTS_PATH = "/rcs/1/events";

    private final InfobipHttpExecutor httpExecutor;

    @Autowired
    public InfobipOutboundEventClient(InfobipHttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
    }

    public JSONObject typing(String baseUrl, String apiKey, String sender,
            String recipient) {
        return send(baseUrl, apiKey, sender, recipient,
                content("TYPING_INDICATOR", null));
    }

    public JSONObject seen(String baseUrl, String apiKey, String sender,
            String recipient, String messageId) {
        if (isBlank(messageId)) {
            throw new IllegalArgumentException("messageId is required for SEEN event");
        }
        return send(baseUrl, apiKey, sender, recipient,
                content("SEEN", messageId));
    }

    private JSONObject send(String baseUrl, String apiKey, String sender,
            String recipient, JSONObject content) {
        validate(baseUrl, apiKey, sender, recipient);

        JSONObject destination = new JSONObject();
        destination.put("to", recipient);
        JSONArray destinations = new JSONArray();
        destinations.add(destination);

        JSONObject event = new JSONObject();
        event.put("sender", sender);
        event.put("destinations", destinations);
        event.put("content", content);

        JSONArray events = new JSONArray();
        events.add(event);
        JSONObject body = new JSONObject();
        body.put("events", events);

        String response = httpExecutor.post(trimTrailingSlash(baseUrl) + EVENTS_PATH,
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

    private static JSONObject content(String type, String messageId) {
        JSONObject content = new JSONObject();
        content.put("type", type);
        if (!isBlank(messageId)) {
            content.put("messageId", messageId);
        }
        return content;
    }

    private static void validate(String baseUrl, String apiKey, String sender,
            String recipient) {
        if (isBlank(baseUrl) || isBlank(apiKey)) {
            throw new IllegalArgumentException("Infobip baseUrl and apiKey are required");
        }
        if (isBlank(sender)) {
            throw new IllegalArgumentException("sender is required");
        }
        if (isBlank(recipient)) {
            throw new IllegalArgumentException("recipient is required");
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

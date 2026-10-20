package com.liugh.rcs.channel.avant;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.channel.RcsChannelSubmitException;

public final class AvantResponseMapper {
    private AvantResponseMapper() {
    }

    public static String messageId(AvantHttpResponse response) {
        if (response == null || isBlank(response.getBody())) {
            throw unknown("empty response");
        }
        JSONObject root;
        try {
            root = JSONObject.parseObject(response.getBody());
        } catch (Exception e) {
            throw unknown("invalid JSON response");
        }
        if (root == null) {
            throw unknown("invalid JSON response");
        }
        if (response.getStatusCode() == 200
                && root.getBooleanValue("success")) {
            JSONObject data = root.getJSONObject("data");
            String id = data == null ? null : data.getString("id");
            if (isBlank(id)) {
                throw unknown("missing data.id");
            }
            return id;
        }
        JSONArray notifications = root.getJSONArray("notifications");
        if (notifications != null && !notifications.isEmpty()) {
            JSONObject notification = notifications.getJSONObject(0);
            String core = safeCode(notification.getString("core"));
            String text = limit(notification.getString("text"));
            throw new RcsChannelSubmitException("AVANT_" + core,
                    isBlank(text) ? "Avant request rejected" : text, false);
        }
        throw new RcsChannelSubmitException(
                "AVANT_HTTP_" + response.getStatusCode(),
                "Avant request rejected with HTTP " + response.getStatusCode(),
                false);
    }

    private static RcsChannelSubmitException unknown(String reason) {
        return new RcsChannelSubmitException("AVANT_SUBMIT_UNKNOWN", reason,
                false);
    }

    private static String safeCode(String value) {
        if (isBlank(value)) {
            return "REJECTED";
        }
        String result = value.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        return result.length() > 64 ? result.substring(0, 64) : result;
    }

    private static String limit(String value) {
        return value != null && value.length() > 512
                ? value.substring(0, 512) : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

package com.liugh.rcs.channel.infobip;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * Infobip RCS sender, test-number and template management client.
 */
@Component
public class InfobipRcsManagementClient {

    private static final String SENDERS = "/rcs/1/senders";

    private final InfobipManagementHttpExecutor http;

    @Autowired
    public InfobipRcsManagementClient(InfobipManagementHttpExecutor http) {
        this.http = http;
    }

    public JSONObject createSender(String baseUrl, String apiKey, JSONObject body) {
        return call("POST", baseUrl, SENDERS, apiKey, body);
    }

    public JSONObject listSenders(String baseUrl, String apiKey) {
        return call("GET", baseUrl, SENDERS, apiKey, null);
    }

    public JSONObject getSender(String baseUrl, String apiKey, String senderName) {
        return call("GET", baseUrl, senderPath(senderName), apiKey, null);
    }

    public JSONObject updateSender(String baseUrl, String apiKey,
            String senderName, JSONObject body) {
        return call("PUT", baseUrl, senderPath(senderName), apiKey, body);
    }

    public JSONObject updateSenderPlatformParameters(String baseUrl,
            String apiKey, String senderName, JSONObject body) {
        return call("PATCH", baseUrl,
                senderPath(senderName) + "/platform-parameters", apiKey, body);
    }

    public JSONObject getSenderLaunchStatus(String baseUrl, String apiKey,
            String senderName) {
        return call("GET", baseUrl, senderPath(senderName) + "/launch-status",
                apiKey, null);
    }

    public JSONObject getProviders(String baseUrl, String apiKey, String country) {
        return call("GET", baseUrl,
                SENDERS + "/providers?countryCode=" + encode(country),
                apiKey, null);
    }

    public JSONObject addTestNumber(String baseUrl, String apiKey,
            String senderName, JSONObject body) {
        return call("POST", baseUrl, testNumbersPath(senderName), apiKey, body);
    }

    public JSONObject listTestNumbers(String baseUrl, String apiKey,
            String senderName) {
        return call("GET", baseUrl, testNumbersPath(senderName), apiKey, null);
    }

    public JSONObject getTestNumber(String baseUrl, String apiKey,
            String senderName, String number) {
        return call("GET", baseUrl, testNumberPath(senderName, number),
                apiKey, null);
    }

    public JSONObject refreshTestNumber(String baseUrl, String apiKey,
            String senderName, String number) {
        return call("PUT", baseUrl,
                testNumberPath(senderName, number) + "/refresh", apiKey,
                new JSONObject());
    }

    public JSONObject updatePrimaryTestNumber(String baseUrl, String apiKey,
            String senderName, String number, boolean primary) {
        JSONObject body = new JSONObject();
        body.put("primary", primary);
        return call("PATCH", baseUrl, testNumberPath(senderName, number),
                apiKey, body);
    }

    public JSONObject getTestNumberRequestStatus(String baseUrl, String apiKey,
            String senderName, String number) {
        return call("POST", baseUrl,
                testNumberPath(senderName, number) + "/request-status",
                apiKey, new JSONObject());
    }

    public JSONObject createTemplate(String baseUrl, String apiKey,
            String senderName, JSONObject body) {
        return call("POST", baseUrl, templatesPath(senderName), apiKey, body);
    }

    public JSONObject listTemplates(String baseUrl, String apiKey,
            String senderName) {
        return call("GET", baseUrl, templatesPath(senderName), apiKey, null);
    }

    public JSONObject getTemplate(String baseUrl, String apiKey,
            String senderName, String templateId) {
        return call("GET", baseUrl, templatePath(senderName, templateId),
                apiKey, null);
    }

    public JSONObject updateTemplate(String baseUrl, String apiKey,
            String senderName, String templateId, JSONObject body) {
        return call("PUT", baseUrl, templatePath(senderName, templateId),
                apiKey, body);
    }

    public JSONObject registerTemplate(String baseUrl, String apiKey,
            String senderName, String templateId, JSONObject body) {
        return call("PATCH", baseUrl,
                templatePath(senderName, templateId) + "/register", apiKey,
                body == null ? new JSONObject() : body);
    }

    public JSONObject deleteTemplate(String baseUrl, String apiKey,
            String senderName, String templateId) {
        return call("DELETE", baseUrl, templatePath(senderName, templateId),
                apiKey, null);
    }

    public JSONObject getTemplateStatus(String baseUrl, String apiKey,
            String senderName, String templateId) {
        return call("GET", baseUrl,
                templatePath(senderName, templateId) + "/status", apiKey, null);
    }

    public JSONObject lookupResourceRequirements(String baseUrl, String apiKey,
            JSONObject body) {
        return call("POST", baseUrl, "/resources/1/requirements-lookup",
                apiKey, body);
    }

    public JSONObject submitResourceRequest(String baseUrl, String apiKey,
            JSONObject body) {
        return call("POST", baseUrl, "/resources/1/requests", apiKey, body);
    }

    public JSONObject getResourceRequest(String baseUrl, String apiKey,
            String requestId) {
        return call("GET", baseUrl,
                "/resources/1/requests/" + encodeRequired(requestId,
                        "requestId"),
                apiKey, null);
    }

    public JSONObject updateResourceRequest(String baseUrl, String apiKey,
            String requestId, JSONObject body) {
        return call("PATCH", baseUrl,
                "/resources/1/requests/" + encodeRequired(requestId,
                        "requestId"),
                apiKey, body);
    }

    public JSONObject cancelResourceRequest(String baseUrl, String apiKey,
            String requestId) {
        return call("POST", baseUrl,
                "/resources/1/requests/" + encodeRequired(requestId,
                        "requestId") + "/cancel",
                apiKey, new JSONObject());
    }

    public JSONObject uploadResourceDocument(String baseUrl, String apiKey,
            String fileName, String contentType, byte[] content) {
        if (isBlank(baseUrl) || isBlank(apiKey) || content == null
                || content.length == 0) {
            throw new IllegalArgumentException(
                    "Infobip baseUrl, apiKey and document are required");
        }
        String raw = http.upload(trimTrailingSlash(baseUrl)
                        + "/resources/1/documents",
                authorization(apiKey), fileName, contentType, content);
        if (isBlank(raw)) {
            return new JSONObject();
        }
        return JSONObject.parseObject(raw);
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
        if (parsed instanceof JSONObject) {
            return (JSONObject) parsed;
        }
        if (parsed instanceof JSONArray) {
            JSONObject wrapper = new JSONObject();
            wrapper.put("items", parsed);
            return wrapper;
        }
        throw new IllegalStateException("Invalid JSON response from Infobip");
    }

    private static String senderPath(String senderName) {
        return SENDERS + "/" + encodeRequired(senderName, "senderName");
    }

    private static String testNumbersPath(String senderName) {
        return senderPath(senderName) + "/test-numbers";
    }

    private static String testNumberPath(String senderName, String number) {
        return testNumbersPath(senderName) + "/"
                + encodeRequired(number, "testNumber");
    }

    private static String templatesPath(String senderName) {
        return senderPath(senderName) + "/templates";
    }

    private static String templatePath(String senderName, String templateId) {
        return templatesPath(senderName) + "/"
                + encodeRequired(templateId, "templateId");
    }

    private static String encodeRequired(String value, String name) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(name + " is required");
        }
        return encode(value);
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8")
                    .replace("+", "%20");
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

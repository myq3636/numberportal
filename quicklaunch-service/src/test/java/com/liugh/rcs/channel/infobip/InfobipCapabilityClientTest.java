package com.liugh.rcs.channel.infobip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import com.alibaba.fastjson.JSONObject;

public class InfobipCapabilityClientTest {

    @Test
    public void mapsSynchronousCapabilityQuery() {
        CapturingExecutor executor = new CapturingExecutor(
                "{\"capabilityCheckResults\":[{\"messageId\":\"m1\",\"phoneNumber\":\"8613800138000\",\"code\":\"ENABLED\"}]}");
        InfobipCapabilityClient client = new InfobipCapabilityClient(executor);

        JSONObject result = client.query("https://api.example.com/", "secret", "sender-1",
                Arrays.asList("8613800138000"));

        assertEquals("https://api.example.com/rcs/2/capability-check/query", executor.url);
        assertEquals("App secret", executor.authorization);
        assertEquals("sender-1", executor.body.getString("sender"));
        assertEquals("8613800138000", executor.body.getJSONArray("phoneNumbers").getString(0));
        assertEquals("m1", result.getJSONArray("capabilityCheckResults")
                .getJSONObject(0).getString("messageId"));
    }

    @Test
    public void mapsAsynchronousCapabilityQuery() {
        CapturingExecutor executor = new CapturingExecutor(
                "{\"bulkId\":\"bulk-1\",\"capabilityCheckRequestStates\":[{\"messageId\":\"m2\",\"status\":\"PENDING_ENROUTE\"}]}");
        InfobipCapabilityClient client = new InfobipCapabilityClient(executor);

        JSONObject result = client.notify("https://api.example.com", "App secret", "sender-1",
                Arrays.asList("8613800138000", "8613800138001"),
                "https://platform.example.com/rcs/callback/infobip/capabilities");

        assertEquals("https://api.example.com/rcs/2/capability-check/notify", executor.url);
        assertEquals("App secret", executor.authorization);
        assertEquals("application/json", executor.body.getString("notifyContentType"));
        assertTrue(executor.body.getString("notifyUrl").endsWith("/capabilities"));
        assertEquals(2, executor.body.getJSONArray("phoneNumbers").size());
        assertEquals("bulk-1", result.getString("bulkId"));
    }

    private static final class CapturingExecutor implements InfobipHttpExecutor {
        private final String response;
        private String url;
        private String authorization;
        private JSONObject body;

        private CapturingExecutor(String response) {
            this.response = response;
        }

        @Override
        public String post(String url, String authorization, String body) {
            this.url = url;
            this.authorization = authorization;
            this.body = JSONObject.parseObject(body);
            return response;
        }
    }
}

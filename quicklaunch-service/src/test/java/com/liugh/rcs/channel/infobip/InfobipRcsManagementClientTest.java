package com.liugh.rcs.channel.infobip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSONObject;

public class InfobipRcsManagementClientTest {

    private CapturingExecutor executor;
    private InfobipRcsManagementClient client;

    @Before
    public void setUp() {
        executor = new CapturingExecutor();
        client = new InfobipRcsManagementClient(executor);
    }

    @Test
    public void createsAndUpdatesSenderUsingInfobipServiceManagementApi() {
        JSONObject create = new JSONObject();
        create.put("senderName", "brand sender");

        client.createSender("https://tenant.api.infobip.com/", "secret", create);
        assertRequest("POST", "https://tenant.api.infobip.com/rcs/1/senders",
                "App secret");
        assertEquals("brand sender",
                JSONObject.parseObject(executor.body).getString("senderName"));

        JSONObject patch = new JSONObject();
        patch.put("description", "updated");
        client.updateSender("https://tenant.api.infobip.com", "App secret",
                "brand sender", patch);
        assertRequest("PUT",
                "https://tenant.api.infobip.com/rcs/1/senders/brand%20sender",
                "App secret");

        client.listSenders("https://tenant.api.infobip.com", "secret");
        assertRequest("GET",
                "https://tenant.api.infobip.com/rcs/1/senders",
                "App secret");

        client.getProviders("https://tenant.api.infobip.com", "secret", "GB");
        assertRequest("GET",
                "https://tenant.api.infobip.com/rcs/1/senders/providers?countryCode=GB",
                "App secret");
    }

    @Test
    public void managesTestNumbersWithoutPuttingCredentialsInPayload() {
        JSONObject body = new JSONObject();
        body.put("testNumber", "+8613800138000");

        client.addTestNumber("https://tenant.api.infobip.com", "key",
                "sender/one", body);

        assertRequest("POST",
                "https://tenant.api.infobip.com/rcs/1/senders/sender%2Fone/test-numbers",
                "App key");
        assertFalse(JSONObject.parseObject(executor.body).containsKey("apiKey"));

        client.refreshTestNumber("https://tenant.api.infobip.com", "key",
                "sender/one", "+8613800138000");
        assertRequest("PUT",
                "https://tenant.api.infobip.com/rcs/1/senders/sender%2Fone/test-numbers/%2B8613800138000/refresh",
                "App key");
    }

    @Test
    public void managesTemplateLifecycle() {
        JSONObject update = new JSONObject();
        update.put("content", "value");
        client.updateTemplate("https://tenant.api.infobip.com", "key",
                "sender", "template id", update);
        assertRequest("PUT",
                "https://tenant.api.infobip.com/rcs/1/senders/sender/templates/template%20id",
                "App key");

        client.registerTemplate("https://tenant.api.infobip.com", "key",
                "sender", "template id", new JSONObject());
        assertRequest("PATCH",
                "https://tenant.api.infobip.com/rcs/1/senders/sender/templates/template%20id/register",
                "App key");

        client.deleteTemplate("https://tenant.api.infobip.com", "key",
                "sender", "template id");
        assertRequest("DELETE",
                "https://tenant.api.infobip.com/rcs/1/senders/sender/templates/template%20id",
                "App key");
    }

    @Test
    public void supportsSenderStatusAndPlatformParameterOperations() {
        JSONObject body = new JSONObject();
        body.put("applicationId", "app");
        client.updateSenderPlatformParameters(
                "https://tenant.api.infobip.com", "key", "sender", body);
        assertRequest("PATCH",
                "https://tenant.api.infobip.com/rcs/1/senders/sender/platform-parameters",
                "App key");
    }

    @Test
    public void managesRcsLaunchThroughResourceRequestApi() {
        JSONObject body = new JSONObject();
        body.put("type", "RCS_SENDER_LAUNCH");

        client.lookupResourceRequirements(
                "https://tenant.api.infobip.com", "key", body);
        assertRequest("POST",
                "https://tenant.api.infobip.com/resources/1/requirements-lookup",
                "App key");

        client.submitResourceRequest(
                "https://tenant.api.infobip.com", "key", body);
        assertRequest("POST",
                "https://tenant.api.infobip.com/resources/1/requests",
                "App key");

        client.cancelResourceRequest(
                "https://tenant.api.infobip.com", "key", "request id");
        assertRequest("POST",
                "https://tenant.api.infobip.com/resources/1/requests/request%20id/cancel",
                "App key");
    }

    private void assertRequest(String method, String url, String authorization) {
        assertEquals(method, executor.method);
        assertEquals(url, executor.url);
        assertEquals(authorization, executor.authorization);
    }

    private static final class CapturingExecutor
            implements InfobipManagementHttpExecutor {
        private String method;
        private String url;
        private String authorization;
        private String body;

        @Override
        public String request(String method, String url, String authorization,
                String body) {
            this.method = method;
            this.url = url;
            this.authorization = authorization;
            this.body = body;
            return "{}";
        }

        @Override
        public String upload(String url, String authorization, String fileName,
                String contentType, byte[] content) {
            this.method = "POST";
            this.url = url;
            this.authorization = authorization;
            this.body = null;
            return "{}";
        }
    }
}

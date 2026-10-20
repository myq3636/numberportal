package com.liugh.rcs.channel.infobip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.alibaba.fastjson.JSONObject;

public class InfobipSubscriptionClientTest {

    private CapturingExecutor executor;
    private InfobipSubscriptionClient client;

    @Before
    public void setUp() {
        executor = new CapturingExecutor();
        client = new InfobipSubscriptionClient(executor);
    }

    @Test
    public void managesProfilesAndSubscriptionsOnChannelPaths() {
        JSONObject body = JSONObject.parseObject(
                "{\"subscriptionId\":\"a2p-rcs-901\"}");

        client.getProfile("https://tenant.api.infobip.com/", "secret",
                "a2p-profile-901");
        assertRequest("GET",
                "https://tenant.api.infobip.com/subscriptions/1/profiles/a2p-profile-901");

        client.createProfile("https://tenant.api.infobip.com", "secret", body);
        assertRequest("POST",
                "https://tenant.api.infobip.com/subscriptions/1/profiles");

        client.getSubscription("https://tenant.api.infobip.com", "secret",
                "RCS", "a2p-rcs-901");
        assertRequest("GET",
                "https://tenant.api.infobip.com/subscriptions/1/subscription/RCS/a2p-rcs-901");

        client.createSubscription("https://tenant.api.infobip.com", "secret",
                "REGISTRATION", body);
        assertRequest("POST",
                "https://tenant.api.infobip.com/subscriptions/1/subscription/REGISTRATION");

        client.updateSubscription("https://tenant.api.infobip.com", "secret",
                "RCS", "a2p-rcs-901", body);
        assertRequest("PUT",
                "https://tenant.api.infobip.com/subscriptions/1/subscription/RCS/a2p-rcs-901");
        assertEquals("a2p-rcs-901",
                JSONObject.parseObject(executor.body)
                        .getString("subscriptionId"));
    }

    @Test
    public void preservesHttpStatusForReconciliation() {
        InfobipManagementHttpException error =
                new InfobipManagementHttpException(404,
                        "Infobip management request rejected");
        assertEquals(404, error.getStatusCode());
        assertTrue(error.getMessage().contains("rejected"));
    }

    private void assertRequest(String method, String url) {
        assertEquals(method, executor.method);
        assertEquals(url, executor.url);
        assertEquals("App secret", executor.authorization);
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
            throw new UnsupportedOperationException();
        }
    }
}

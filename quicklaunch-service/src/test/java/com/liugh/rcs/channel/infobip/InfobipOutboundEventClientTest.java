package com.liugh.rcs.channel.infobip;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class InfobipOutboundEventClientTest {

    @Test
    public void mapsTypingIndicatorEvent() {
        CapturingExecutor executor = new CapturingExecutor("{\"status\":\"PENDING\"}");
        InfobipOutboundEventClient client = new InfobipOutboundEventClient(executor);

        JSONObject result = client.typing("https://api.example.com/", "secret",
                "sender-1", "8613800138000");

        assertEquals("https://api.example.com/rcs/1/events", executor.url);
        assertEquals("App secret", executor.authorization);
        JSONObject event = firstEvent(executor.body);
        assertEquals("sender-1", event.getString("sender"));
        assertEquals("8613800138000",
                event.getJSONArray("destinations").getJSONObject(0).getString("to"));
        assertEquals("TYPING_INDICATOR",
                event.getJSONObject("content").getString("type"));
        assertEquals("PENDING", result.getString("status"));
    }

    @Test
    public void mapsSeenEventWithOriginalMessageId() {
        CapturingExecutor executor = new CapturingExecutor("{\"status\":\"PENDING\"}");
        InfobipOutboundEventClient client = new InfobipOutboundEventClient(executor);

        client.seen("https://api.example.com", "App secret",
                "sender-1", "8613800138000", "inbound-message-1");

        JSONObject event = firstEvent(executor.body);
        assertEquals("SEEN", event.getJSONObject("content").getString("type"));
        assertEquals("inbound-message-1",
                event.getJSONObject("content").getString("messageId"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsSeenEventWithoutOriginalMessageIdBeforeCallingInfobip() {
        CapturingExecutor executor = new CapturingExecutor("{\"status\":\"PENDING\"}");
        InfobipOutboundEventClient client = new InfobipOutboundEventClient(executor);

        try {
            client.seen("https://api.example.com", "secret",
                    "sender-1", "8613800138000", " ");
        } finally {
            assertEquals(0, executor.callCount);
        }
    }

    private static JSONObject firstEvent(JSONObject body) {
        JSONArray events = body.getJSONArray("events");
        assertEquals(1, events.size());
        return events.getJSONObject(0);
    }

    private static final class CapturingExecutor implements InfobipHttpExecutor {
        private final String response;
        private String url;
        private String authorization;
        private JSONObject body;
        private int callCount;

        private CapturingExecutor(String response) {
            this.response = response;
        }

        @Override
        public String post(String url, String authorization, String body) {
            this.callCount++;
            this.url = url;
            this.authorization = authorization;
            this.body = JSONObject.parseObject(body);
            return response;
        }
    }
}

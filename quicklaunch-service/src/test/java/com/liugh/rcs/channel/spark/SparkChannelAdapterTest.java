package com.liugh.rcs.channel.spark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.channel.RcsChannelSubmitException;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.model.ContentType;
import com.liugh.rcs.model.RcsContent;

public class SparkChannelAdapterTest {

    @Test
    public void returnsUuidOnSuccess() throws Exception {
        CapturingExecutor exec = new CapturingExecutor(
                "{\"code\":200,\"desc\":\"Mensaje Enviado\",\"uuid\":\"u-123\"}");
        SparkChannelAdapter adapter = new SparkChannelAdapter();
        adapter.setHttpExecutor(exec);

        String id = adapter.send(textContent(), ctx());

        assertEquals("u-123", id);
        assertEquals("https://desarrollo.sms-voices.com.mx:8080/sendRCSMessage", exec.url);
        JSONObject body = JSONObject.parseObject(exec.body);
        assertEquals("u", body.getString("user"));
        assertEquals("5512345678", body.getString("phoneNumber"));
    }

    @Test
    public void throwsSubmitExceptionOnBusinessError() throws Exception {
        CapturingExecutor exec = new CapturingExecutor("{\"code\":401,\"desc\":\"bad creds\"}");
        SparkChannelAdapter adapter = new SparkChannelAdapter();
        adapter.setHttpExecutor(exec);
        try {
            adapter.send(textContent(), ctx());
            fail("expected RcsChannelSubmitException");
        } catch (RcsChannelSubmitException e) {
            assertEquals("SPARK_401", e.getErrorCode());
            assertEquals("bad creds", e.getErrorReason());
            assertEquals(false, e.isRetryable());
        }
    }

    @Test
    public void throwsSubmitUnknownOnNetworkFailure() throws Exception {
        CapturingExecutor exec = new CapturingExecutor(null);
        SparkChannelAdapter adapter = new SparkChannelAdapter();
        adapter.setHttpExecutor(exec);
        try {
            adapter.send(textContent(), ctx());
            fail("expected RcsChannelSubmitException");
        } catch (RcsChannelSubmitException e) {
            assertEquals("SPARK_SUBMIT_UNKNOWN", e.getErrorCode());
        }
    }

    @Test
    public void throwsPayloadInvalidOnBadPhone() throws Exception {
        CapturingExecutor exec = new CapturingExecutor(
                "{\"code\":200,\"desc\":\"ok\",\"uuid\":\"u\"}");
        SparkChannelAdapter adapter = new SparkChannelAdapter();
        adapter.setHttpExecutor(exec);
        RcsSendCtx ctx = ctx();
        ctx.setCountryCode("CN");
        try {
            adapter.send(textContent(), ctx);
            fail("expected RcsChannelSubmitException");
        } catch (RcsChannelSubmitException e) {
            assertEquals("SPARK_PAYLOAD_INVALID", e.getErrorCode());
        }
    }

    private static RcsSendCtx ctx() {
        RcsSendCtx ctx = new RcsSendCtx();
        ctx.setAuthUser("u");
        ctx.setAuthPass("p");
        ctx.setRecipient("+525512345678");
        ctx.setCountryCode("MX");
        ctx.setBaseUrl("https://desarrollo.sms-voices.com.mx:8080");
        ctx.setMsgId("m1");
        ctx.setRssid(903);
        return ctx;
    }

    private static RcsContent textContent() {
        RcsContent c = new RcsContent();
        c.setType(ContentType.TEXT);
        c.setText("hello");
        return c;
    }

    private static final class CapturingExecutor implements SparkHttpExecutor {
        private final String response;
        private String url;
        private String body;

        private CapturingExecutor(String response) {
            this.response = response;
        }

        @Override
        public String postJson(String url, String body) {
            this.url = url;
            this.body = body;
            return response;
        }
    }
}

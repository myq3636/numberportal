package com.liugh.rcs.channel.spark;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.liugh.rcs.channel.RcsChannelSubmitException;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.model.ContentType;
import com.liugh.rcs.model.RcsContent;

public class SparkCredentialRedactionTest {

    @Test
    public void submitExceptionDoesNotContainPassword() throws Exception {
        SparkChannelAdapter adapter = new SparkChannelAdapter();
        adapter.setHttpExecutor(new SparkHttpExecutor() {
            @Override
            public String postJson(String url, String body) {
                return "{\"code\":500,\"desc\":\"system error\"}";
            }
        });
        RcsSendCtx ctx = new RcsSendCtx();
        ctx.setAuthUser("u");
        ctx.setAuthPass("SUPER_SECRET_PASSWORD");
        ctx.setRecipient("+525512345678");
        ctx.setCountryCode("MX");
        ctx.setBaseUrl("https://x.example.com");
        ctx.setMsgId("m1");
        RcsContent c = new RcsContent();
        c.setType(ContentType.TEXT);
        c.setText("hello");

        try {
            adapter.send(c, ctx);
        } catch (RcsChannelSubmitException e) {
            assertFalse(e.getErrorReason() != null
                    && e.getErrorReason().contains("SUPER_SECRET_PASSWORD"));
        }
    }
}

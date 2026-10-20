package com.liugh.rcs.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RcsEventSignatureTest {

    @Test
    public void signsEventIdTimestampAndExactBodyWithHmacSha256() {
        String signature = RcsEventSignature.sign("customer-secret",
                "evt_1", "1722140000", "{\"type\":\"MESSAGE_DELIVERED\"}");

        assertTrue(signature.startsWith("sha256="));
        assertTrue(RcsEventSignature.verify("customer-secret",
                "evt_1", "1722140000",
                "{\"type\":\"MESSAGE_DELIVERED\"}", signature));
        assertFalse(RcsEventSignature.verify("customer-secret",
                "evt_1", "1722140000",
                "{\"type\":\"MESSAGE_SEEN\"}", signature));
    }
}

package com.liugh.rcs.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class RcsEventSignature {

    private static final String ALGORITHM = "HmacSHA256";

    private RcsEventSignature() {
    }

    public static String sign(String secret, String eventId,
            String timestamp, String rawBody) {
        if (isBlank(secret) || isBlank(eventId)
                || isBlank(timestamp) || rawBody == null) {
            throw new IllegalArgumentException(
                    "secret, eventId, timestamp and rawBody are required");
        }
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
                    ALGORITHM));
            byte[] digest = mac.doFinal(canonical(eventId, timestamp, rawBody)
                    .getBytes(StandardCharsets.UTF_8));
            return "sha256=" + hex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign RCS event", e);
        }
    }

    public static boolean verify(String secret, String eventId,
            String timestamp, String rawBody, String signature) {
        if (signature == null) {
            return false;
        }
        byte[] expected = sign(secret, eventId, timestamp, rawBody)
                .getBytes(StandardCharsets.US_ASCII);
        byte[] actual = signature.getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual);
    }

    private static String canonical(String eventId, String timestamp,
            String rawBody) {
        return eventId + "." + timestamp + "." + rawBody;
    }

    private static String hex(byte[] bytes) {
        StringBuilder value = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            value.append(String.format("%02x", b & 0xff));
        }
        return value.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

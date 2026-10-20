package com.liugh.rcs.service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Encrypts customer webhook secrets at rest with AES-GCM.
 */
@Component
public class RcsSecretCipher {

    private static final byte VERSION = 1;
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom random = new SecureRandom();
    private byte[] key;

    public RcsSecretCipher() {
    }

    RcsSecretCipher(byte[] key) {
        setKey(key);
    }

    @Value("${rcs.event-secret-key:}")
    public void setEncodedKey(String encodedKey) {
        if (encodedKey != null && !encodedKey.trim().isEmpty()) {
            setKey(Base64.getDecoder().decode(encodedKey.trim()));
        }
    }

    public byte[] encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            throw new IllegalArgumentException("signatureSecret is required");
        }
        requireKey();
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey(), "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(1 + iv.length + encrypted.length)
                    .put(VERSION).put(iv).put(encrypted).array();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt RCS event secret", e);
        }
    }

    public String decrypt(byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length <= 1 + IV_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted secret");
        }
        requireKey();
        try {
            ByteBuffer value = ByteBuffer.wrap(ciphertext);
            if (value.get() != VERSION) {
                throw new IllegalArgumentException("Unsupported encrypted secret version");
            }
            byte[] iv = new byte[IV_LENGTH];
            value.get(iv);
            byte[] encrypted = new byte[value.remaining()];
            value.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey(), "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to decrypt RCS event secret", e);
        }
    }

    private void setKey(byte[] key) {
        if (key == null || key.length != 32) {
            throw new IllegalArgumentException(
                    "rcs.event-secret-key must decode to 32 bytes");
        }
        this.key = Arrays.copyOf(key, key.length);
    }

    private void requireKey() {
        if (key == null) {
            throw new IllegalStateException("rcs.event-secret-key is not configured");
        }
    }

    private byte[] aesKey() {
        // The legacy JDK 8 runtime may not have the unlimited-strength policy.
        // AES-128-GCM remains authenticated encryption and works without that
        // optional policy while the configured master secret stays 32 bytes.
        return Arrays.copyOf(key, 16);
    }
}

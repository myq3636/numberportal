package com.liugh.rcs.channel.spark;

/**
 * Spark 号码归一：E.164 -> 10 位本地号。P1 仅支持墨西哥（+52 + 10 位）。
 */
public final class SparkPhoneNormalizer {

    private SparkPhoneNormalizer() {
    }

    /**
     * @throws IllegalArgumentException 非 MX 或非 10 位纯数字时抛出。
     */
    public static String normalize(String e164, String countryCode) {
        if (countryCode != null && !"MX".equalsIgnoreCase(countryCode.trim())) {
            throw new IllegalArgumentException("Spark only supports MX, got country:" + countryCode);
        }
        if (e164 == null) {
            throw new IllegalArgumentException("Spark recipient is null");
        }
        String digits = e164.trim();
        if (digits.startsWith("+")) {
            digits = digits.substring(1);
        }
        // 仅剥离格式符（空格/横线/括号/点），字母等非法字符保留并校验失败
        digits = digits.replaceAll("[\\s\\-().]", "");
        if (!digits.matches("[0-9]+")) {
            throw new IllegalArgumentException("Spark requires 10-digit numeric phoneNumber");
        }
        if (digits.startsWith("52") && digits.length() == 12) {
            digits = digits.substring(2);
        }
        if (!digits.matches("[0-9]{10}")) {
            throw new IllegalArgumentException("Spark requires 10-digit numeric phoneNumber");
        }
        return digits;
    }
}

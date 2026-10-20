package com.liugh.rcs.channel.avant;

final class AvantPhoneNormalizer {
    private AvantPhoneNormalizer() {
    }

    static String normalize(String recipient) {
        if (recipient == null) {
            throw new IllegalArgumentException("Avant recipient is required");
        }
        String value = recipient.trim();
        if (value.startsWith("+")) {
            value = value.substring(1);
        }
        if (!value.matches("[1-9][0-9]{7,14}")) {
            throw new IllegalArgumentException("Avant recipient must be an international number");
        }
        return value;
    }
}

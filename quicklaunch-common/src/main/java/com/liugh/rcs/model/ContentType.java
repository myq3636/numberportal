package com.liugh.rcs.model;

/**
 * RCS 消息内容类型（内部规范化）。
 */
public enum ContentType {
    TEXT, FILE, CARD, CAROUSEL, TEMPLATE;

    public static ContentType from(String s) {
        if (s == null) {
            return TEXT;
        }
        try {
            return ContentType.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TEXT;
        }
    }
}

package com.liugh.rcs.model;

/**
 * 建议回复/动作类型（内部规范化，映射到各通道私有取值）。
 */
public enum SuggestionType {
    REPLY,
    OPEN_URL,
    DIAL,
    SHOW_LOCATION,
    REQUEST_LOCATION,
    CALENDAR;

    public static SuggestionType from(String s) {
        if (s == null) {
            return REPLY;
        }
        try {
            return SuggestionType.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return REPLY;
        }
    }
}

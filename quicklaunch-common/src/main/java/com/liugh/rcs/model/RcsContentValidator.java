package com.liugh.rcs.model;

import java.util.List;

/**
 * 规范限额校验。返回 null 表示通过，否则返回可读的错误原因（发送前拦截）。
 */
public final class RcsContentValidator {

    public static final int MAX_TEXT = 3072;
    public static final int MAX_SUGGESTION_TEXT = 25;
    public static final int MAX_POSTBACK = 2048;
    public static final int MAX_CARD_TITLE = 200;
    public static final int MAX_CARD_DESC = 2000;
    public static final int MAX_MSG_SUGGESTIONS = 11;
    public static final int MAX_CARD_SUGGESTIONS = 4;
    public static final int MIN_CAROUSEL = 2;
    public static final int MAX_CAROUSEL = 10;

    private RcsContentValidator() {
    }

    public static String validate(RcsContent c) {
        if (c == null) {
            return "content is null";
        }
        if (c.getType() == null) {
            return "content.type is null";
        }
        switch (c.getType()) {
            case TEXT:
                if (isBlank(c.getText())) {
                    return "TEXT.text is blank";
                }
                if (c.getText().length() > MAX_TEXT) {
                    return "TEXT.text exceeds " + MAX_TEXT;
                }
                return validateSuggestions(c.getSuggestions(), MAX_MSG_SUGGESTIONS, "message");
            case FILE:
                if (c.getFile() == null || isBlank(c.getFile().getUrl())) {
                    return "FILE.file.url is blank";
                }
                return validateSuggestions(c.getSuggestions(), MAX_MSG_SUGGESTIONS, "message");
            case CARD:
                if (c.getCard() == null) {
                    return "CARD.card is null";
                }
                return validateCard(c.getCard());
            case CAROUSEL:
                List<RcsCard> cards = c.getCards();
                if (cards == null || cards.size() < MIN_CAROUSEL || cards.size() > MAX_CAROUSEL) {
                    return "CAROUSEL.cards must be " + MIN_CAROUSEL + "-" + MAX_CAROUSEL;
                }
                for (RcsCard card : cards) {
                    String err = validateCard(card);
                    if (err != null) {
                        return err;
                    }
                }
                return null;
            case TEMPLATE:
                if (isBlank(c.getTemplateId())) {
                    return "TEMPLATE.templateId is blank";
                }
                return validateTemplateParameters(c.getParameters());
            default:
                return "unsupported type " + c.getType();
        }
    }

    private static String validateTemplateParameters(List<RcsTemplateParameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return null;
        }
        for (RcsTemplateParameter p : parameters) {
            if (p == null || isBlank(p.getName())) {
                return "TEMPLATE.parameters contains blank name";
            }
        }
        return null;
    }

    private static String validateCard(RcsCard card) {
        if (card == null) {
            return "card is null";
        }
        if (isBlank(card.getTitle()) && isBlank(card.getDescription())
                && (card.getMedia() == null || isBlank(card.getMedia().getUrl()))) {
            return "card must have title/description/media";
        }
        if (card.getTitle() != null && card.getTitle().length() > MAX_CARD_TITLE) {
            return "card.title exceeds " + MAX_CARD_TITLE;
        }
        if (card.getDescription() != null && card.getDescription().length() > MAX_CARD_DESC) {
            return "card.description exceeds " + MAX_CARD_DESC;
        }
        return validateSuggestions(card.getSuggestions(), MAX_CARD_SUGGESTIONS, "card");
    }

    private static String validateSuggestions(List<RcsSuggestion> list, int max, String scope) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > max) {
            return scope + " suggestions exceed " + max;
        }
        for (RcsSuggestion s : list) {
            if (s.getText() != null && s.getText().length() > MAX_SUGGESTION_TEXT) {
                return "suggestion.text exceeds " + MAX_SUGGESTION_TEXT;
            }
            if (s.getPostbackData() != null && s.getPostbackData().length() > MAX_POSTBACK) {
                return "suggestion.postbackData exceeds " + MAX_POSTBACK;
            }
            if (s.getType() == SuggestionType.OPEN_URL) {
                if (isBlank(s.getUrl())) {
                    return "OPEN_URL suggestion missing url";
                }
                if (!s.getUrl().toLowerCase().startsWith("https://")) {
                    return "OPEN_URL url must be https";
                }
            }
            if (s.getType() == SuggestionType.DIAL && isBlank(s.getPhoneNumber())) {
                return "DIAL suggestion missing phoneNumber";
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

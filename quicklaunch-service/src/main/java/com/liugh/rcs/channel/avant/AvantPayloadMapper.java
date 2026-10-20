package com.liugh.rcs.channel.avant;

import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.model.ContentType;
import com.liugh.rcs.model.RcsCard;
import com.liugh.rcs.model.RcsContent;
import com.liugh.rcs.model.RcsMedia;
import com.liugh.rcs.model.RcsSuggestion;
import com.liugh.rcs.model.SuggestionType;

/** Maps the platform content model to Avant's MessageRCS request. */
public final class AvantPayloadMapper {
    private AvantPayloadMapper() {
    }

    public static JSONObject toPayload(RcsContent content, RcsSendCtx ctx) {
        require(content != null && content.getType() != null,
                "Avant content is required");
        require(ctx != null && notBlank(ctx.getMsgId()),
                "Avant msgId is required");

        JSONObject item = mapItem(content, ctx.getMsgId() + ":0");
        JSONObject root = new JSONObject(true);
        root.put("recipient", AvantPhoneNormalizer.normalize(ctx.getRecipient()));
        root.put("class", messageClass(content));
        root.put("key", ctx.getMsgId());
        JSONArray items = new JSONArray();
        items.add(item);
        root.put("items", items);

        JSONObject fallback = new JSONObject(true);
        boolean useFallback = notBlank(ctx.getFallbackText());
        fallback.put("use", useFallback);
        fallback.put("text", useFallback ? ctx.getFallbackText() : "");
        root.put("fallback", fallback);
        return root;
    }

    private static JSONObject mapItem(RcsContent content, String itemKey) {
        if (content.getType() == ContentType.TEXT) {
            require(notBlank(content.getText()), "Avant text is required");
            if (content.getSuggestions() == null
                    || content.getSuggestions().isEmpty()) {
                JSONObject item = baseItem("Text", itemKey);
                item.put("text", content.getText());
                return item;
            }
            JSONObject item = baseItem("ReplyableText", itemKey);
            item.put("text", content.getText());
            item.put("quickReplyButtons", mapButtons(content.getSuggestions()));
            return item;
        }
        if (content.getType() == ContentType.CARD) {
            return mapCard(content.getCard(), itemKey);
        }
        throw new IllegalArgumentException("Avant unsupported content type: "
                + content.getType());
    }

    private static JSONObject mapCard(RcsCard card, String itemKey) {
        require(card != null, "Avant card is required");
        require(notBlank(card.getTitle()), "Avant card title is required");
        if ("HORIZONTAL".equalsIgnoreCase(card.getOrientation())) {
            throw new IllegalArgumentException("Avant unsupported card orientation: HORIZONTAL");
        }
        JSONObject item = baseItem("Card", itemKey);
        item.put("title", card.getTitle());
        if (card.getDescription() != null) {
            item.put("text", card.getDescription());
        }
        if (card.getMedia() != null) {
            item.put("media", mapMedia(card.getMedia(), card.getTitle()));
        }
        if (card.getSuggestions() != null && !card.getSuggestions().isEmpty()) {
            item.put("buttons", mapButtons(card.getSuggestions()));
        }
        return item;
    }

    private static JSONObject mapMedia(RcsMedia media, String caption) {
        require(notBlank(media.getUrl()), "Avant card media URL is required");
        JSONObject result = new JSONObject(true);
        result.put("url", media.getUrl());
        String height = media.getHeight();
        if (height == null || "MEDIUM".equalsIgnoreCase(height)) {
            result.put("disposition", "TopMedium");
        } else if ("SHORT".equalsIgnoreCase(height)) {
            result.put("disposition", "TopSmall");
        } else {
            throw new IllegalArgumentException("Avant unsupported media height: " + height);
        }
        result.put("caption", caption);
        return result;
    }

    private static JSONArray mapButtons(List<RcsSuggestion> suggestions) {
        JSONArray buttons = new JSONArray();
        for (RcsSuggestion suggestion : suggestions) {
            require(suggestion != null && suggestion.getType() != null,
                    "Avant suggestion type is required");
            JSONObject button = new JSONObject(true);
            if (suggestion.getType() == SuggestionType.REPLY) {
                button.put("type", "Text");
            } else if (suggestion.getType() == SuggestionType.OPEN_URL) {
                require(notBlank(suggestion.getUrl()),
                        "Avant link suggestion URL is required");
                button.put("type", "Link");
                button.put("url", suggestion.getUrl());
            } else {
                throw new IllegalArgumentException(
                        "Avant unsupported suggestion type: " + suggestion.getType());
            }
            require(notBlank(suggestion.getText()),
                    "Avant suggestion text is required");
            button.put("text", suggestion.getText());
            if (suggestion.getPostbackData() != null) {
                button.put("payload", suggestion.getPostbackData());
            }
            buttons.add(button);
        }
        return buttons;
    }

    private static String messageClass(RcsContent content) {
        if (content.getType() == ContentType.TEXT
                && (content.getSuggestions() == null
                    || content.getSuggestions().isEmpty())
                && codePointLength(content.getText()) <= 160) {
            return "Basic";
        }
        return "Single";
    }

    private static JSONObject baseItem(String type, String key) {
        JSONObject item = new JSONObject(true);
        item.put("type", type);
        item.put("key", key);
        return item;
    }

    private static int codePointLength(String value) {
        return value == null ? 0 : value.codePointCount(0, value.length());
    }

    private static boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}

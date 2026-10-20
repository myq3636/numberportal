package com.liugh.rcs.channel.routemobile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liugh.rcs.model.RcsCard;
import com.liugh.rcs.model.RcsContent;
import com.liugh.rcs.model.RcsMedia;
import com.liugh.rcs.model.RcsSuggestion;

/**
 * 内部 RcsContent → Route Mobile POST /rcs/v1/message 请求体（Map，交给 fastjson 序列化）。
 * RM 报文是扁平结构：type + phone_no + bot_name + text/url/card/cards + suggestions。
 */
public final class RouteMobilePayloadMapper {

    private RouteMobilePayloadMapper() {
    }

    /**
     * @param content   规范化内容
     * @param botName   RM bot_name（= 我方 sender）
     * @param phoneNo   收件号码（不带 +，带国家码）
     * @param msgId     我方 msgId → extra，回调原样带回
     * @param ttl       有效期（秒），可为 null
     */
    public static Map<String, Object> toSendPayload(RcsContent content, String botName, String phoneNo,
            String msgId, Integer ttl) {
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("phone_no", phoneNo);
        if (botName != null) {
            root.put("bot_name", botName);
        }
        if (msgId != null) {
            root.put("extra", msgId);
        }
        if (ttl != null && ttl > 0) {
            root.put("ttl", ttl);
        }

        switch (content.getType()) {
            case TEXT:
                root.put("type", "text");
                root.put("text", content.getText());
                putSuggestions(root, content.getSuggestions());
                break;
            case FILE:
                root.put("type", "media");
                RcsMedia media = content.getFile();
                if (media != null) {
                    if (media.getUrl() != null) {
                        root.put("url", media.getUrl());
                    }
                    if (media.getThumbnailUrl() != null) {
                        root.put("thumbnail_url", media.getThumbnailUrl());
                    }
                }
                putSuggestions(root, content.getSuggestions());
                break;
            case CARD:
                root.put("type", "card");
                root.put("card", buildCard(content.getCard()));
                break;
            case CAROUSEL:
                root.put("type", "multiple_cards");
                List<Object> cards = new ArrayList<Object>();
                if (content.getCards() != null) {
                    for (RcsCard card : content.getCards()) {
                        Map<String, Object> wrap = new HashMap<String, Object>();
                        wrap.put("card", buildCard(card));
                        cards.add(wrap);
                    }
                }
                root.put("cards", cards);
                break;
            case TEMPLATE:
                throw new IllegalArgumentException(
                        "Route Mobile unsupported content type: " + content.getType());
            default:
                root.put("type", "text");
                root.put("text", content.getText());
                break;
        }
        return root;
    }

    private static Map<String, Object> buildCard(RcsCard card) {
        Map<String, Object> c = new HashMap<String, Object>();
        if (card == null) {
            return c;
        }
        if (card.getTitle() != null) {
            c.put("title", card.getTitle());
        }
        if (card.getDescription() != null) {
            c.put("description", card.getDescription());
        }
        if (card.getMedia() != null && card.getMedia().getUrl() != null) {
            c.put("url", card.getMedia().getUrl());
        }
        List<Object> sugg = buildSuggestions(card.getSuggestions());
        if (sugg != null) {
            c.put("suggestions", sugg);
        }
        return c;
    }

    private static void putSuggestions(Map<String, Object> root, List<RcsSuggestion> suggestions) {
        List<Object> list = buildSuggestions(suggestions);
        if (list != null) {
            root.put("suggestions", list);
        }
    }

    private static List<Object> buildSuggestions(List<RcsSuggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }
        List<Object> list = new ArrayList<Object>();
        for (RcsSuggestion s : suggestions) {
            list.add(buildSuggestion(s));
        }
        return list;
    }

    private static Map<String, Object> buildSuggestion(RcsSuggestion s) {
        Map<String, Object> m = new HashMap<String, Object>();
        if (s.getText() != null) {
            m.put("text", s.getText());
        }
        if (s.getPostbackData() != null) {
            m.put("postback", s.getPostbackData());
        }
        switch (s.getType()) {
            case REPLY:
                m.put("type", "message");
                break;
            case OPEN_URL:
                m.put("type", "url");
                m.put("url", s.getUrl());
                break;
            case DIAL:
                m.put("type", "dial");
                m.put("call_to", s.getPhoneNumber());
                break;
            case SHOW_LOCATION:
                m.put("type", "location");
                m.put("latitude", s.getLatitude());
                m.put("longitude", s.getLongitude());
                break;
            case CALENDAR:
                m.put("type", "calendar");
                m.put("start", s.getStartTime());
                m.put("end", s.getEndTime());
                m.put("title", s.getCalTitle());
                m.put("description", s.getCalDescription());
                break;
            case REQUEST_LOCATION:
                // RM 无"请求位置"动作 → 降级为普通文本回复
                m.put("type", "message");
                break;
            default:
                m.put("type", "message");
                break;
        }
        return m;
    }
}

package com.liugh.rcs.channel.infobip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liugh.rcs.model.RcsCard;
import com.liugh.rcs.model.RcsContent;
import com.liugh.rcs.model.RcsMedia;
import com.liugh.rcs.model.RcsSuggestion;
import com.liugh.rcs.model.RcsTemplateParameter;

/**
 * 内部 RcsContent → Infobip POST /rcs/2/messages 请求体（Map，交给 fastjson 序列化）。
 */
public final class InfobipPayloadMapper {

    private InfobipPayloadMapper() {
    }

    /**
     * @param content     规范化内容
     * @param sender      Infobip sender（channel_agent.sender）
     * @param recipient   E.164
     * @param msgId       我方 msgId（回填 callbackData，回调原样带回）
     * @param notifyUrl   我方入站回调 URL
     * @param ttlSeconds  有效期（秒），可为 null
     * @param smsSender   smsFailover 发送方，可为 null
     * @param fallbackText smsFailover 文案，可为 null（为 null 则不下发 failover）
     */
    public static Map<String, Object> toSendPayload(RcsContent content, String sender, String recipient,
            String msgId, String notifyUrl, Integer ttlSeconds, String smsSender, String fallbackText) {
        Map<String, Object> root = new HashMap<String, Object>();
        List<Object> messages = new ArrayList<Object>();
        root.put("messages", messages);

        Map<String, Object> msg = new HashMap<String, Object>();
        messages.add(msg);
        if (sender != null) {
            msg.put("sender", sender);
        }
        List<Object> destinations = new ArrayList<Object>();
        Map<String, Object> to = new HashMap<String, Object>();
        to.put("to", recipient);
        destinations.add(to);
        msg.put("destinations", destinations);

        msg.put("content", buildContent(content));

        // options：有效期 + 原生 SMS failover
        Map<String, Object> options = new HashMap<String, Object>();
        if (ttlSeconds != null && ttlSeconds > 0) {
            Map<String, Object> validity = new HashMap<String, Object>();
            validity.put("amount", ttlSeconds);
            validity.put("timeUnit", "SECONDS");
            options.put("validityPeriod", validity);
        }
        if (fallbackText != null && !fallbackText.trim().isEmpty()) {
            Map<String, Object> smsFailover = new HashMap<String, Object>();
            if (smsSender != null) {
                smsFailover.put("sender", smsSender);
            }
            smsFailover.put("text", fallbackText);
            options.put("smsFailover", smsFailover);
        }
        if (!options.isEmpty()) {
            msg.put("options", options);
        }

        // webhooks：投递/已读回调 + callbackData 关联键
        Map<String, Object> webhooks = new HashMap<String, Object>();
        if (notifyUrl != null) {
            Map<String, Object> delivery = new HashMap<String, Object>();
            delivery.put("url", notifyUrl);
            delivery.put("intermediateReport", true);
            webhooks.put("delivery", delivery);
            Map<String, Object> seen = new HashMap<String, Object>();
            seen.put("url", notifyUrl);
            webhooks.put("seen", seen);
        }
        if (msgId != null) {
            webhooks.put("callbackData", msgId);
        }
        if (!webhooks.isEmpty()) {
            msg.put("webhooks", webhooks);
        }
        return root;
    }

    private static Map<String, Object> buildContent(RcsContent content) {
        Map<String, Object> c = new HashMap<String, Object>();
        Map<String, Object> csed = new HashMap<String, Object>();
        switch (content.getType()) {
            case TEXT:
                c.put("type", "TEXT");
                c.put("text", content.getText());
                putSuggestions(c, content.getSuggestions());
                break;
            case FILE:
                c.put("type", "FILE");
                putMediaFlat(c, content.getFile());
                putSuggestions(c, content.getSuggestions());
                break;
            case CARD:
                c.put("type", "CARD");
                c.put("content", csed);
                fillCard(c, csed, content.getCard());
                break;
            case CAROUSEL:
                c.put("type", "CAROUSEL");
                List<Object> cards = new ArrayList<Object>();
                if (content.getCards() != null) {
                    for (RcsCard card : content.getCards()) {
                        Map<String, Object> cm = new HashMap<String, Object>();
                        Map<String, Object> cmsed = new HashMap<String, Object>();
                        fillCard(cm,cmsed, card);
                        cards.add(cmsed);
                    }
                }
                c.put("cardWidth", content.getCardWidth());
                c.put("contents", cards);
                break;
            case TEMPLATE:
                c.put("type", "PROVIDER_TEMPLATE");
                c.put("templateId", content.getTemplateId());
                putTemplateParameters(c, content.getParameters());
                break;
            default:
                c.put("type", "TEXT");
                c.put("text", content.getText());
                break;
        }
        return c;
    }

    private static void putTemplateParameters(Map<String, Object> c, List<RcsTemplateParameter> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }
        List<Object> list = new ArrayList<Object>();
        for (RcsTemplateParameter p : parameters) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("name", p.getName());
            m.put("value", p.getValue());
            list.add(m);
        }
        c.put("parameters", list);
    }

    private static void fillCard(Map<String, Object> c, Map<String, Object> csed, RcsCard card) {
        if (card == null) {
            return;
        }
        if (card.getOrientation() != null) {
            c.put("orientation", card.getOrientation());
        }
        if (card.getAlignment() != null) {
            c.put("alignment", card.getAlignment());
        }
        if (card.getTitle() != null) {
        	csed.put("title", card.getTitle());
        }
        if (card.getDescription() != null) {
        	csed.put("description", card.getDescription());
        }
        if (card.getMedia() != null) {
        	csed.put("media", buildMediaObject(card.getMedia()));
        }
        putSuggestions(csed, card.getSuggestions());
    }

    /** FILE 内容用扁平 file/thumbnail 结构 */
    private static void putMediaFlat(Map<String, Object> c, RcsMedia media) {
        if (media == null) {
            return;
        }
        if (media.getUrl() != null) {
            Map<String, Object> file = new HashMap<String, Object>();
            file.put("url", media.getUrl());
            c.put("file", file);
        }
        if (media.getThumbnailUrl() != null) {
            Map<String, Object> thumb = new HashMap<String, Object>();
            thumb.put("url", media.getThumbnailUrl());
            c.put("thumbnail", thumb);
        }
    }

    /** CARD 内的 media 对象：{file:{url},thumbnail:{url},height} */
    private static Map<String, Object> buildMediaObject(RcsMedia media) {
        Map<String, Object> m = new HashMap<String, Object>();
        if (media.getUrl() != null) {
            Map<String, Object> file = new HashMap<String, Object>();
            file.put("url", media.getUrl());
            m.put("file", file);
        }
        if (media.getThumbnailUrl() != null) {
            Map<String, Object> thumb = new HashMap<String, Object>();
            thumb.put("url", media.getThumbnailUrl());
            m.put("thumbnail", thumb);
        }
        if (media.getHeight() != null) {
            m.put("height", media.getHeight());
        }
        return m;
    }

    private static void putSuggestions(Map<String, Object> c, List<RcsSuggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return;
        }
        List<Object> list = new ArrayList<Object>();
        for (RcsSuggestion s : suggestions) {
            list.add(buildSuggestion(s));
        }
        c.put("suggestions", list);
    }

    private static Map<String, Object> buildSuggestion(RcsSuggestion s) {
        Map<String, Object> m = new HashMap<String, Object>();
        if (s.getText() != null) {
            m.put("text", s.getText());
        }
        if (s.getPostbackData() != null) {
            m.put("postbackData", s.getPostbackData());
        }
        switch (s.getType()) {
            case REPLY:
                m.put("type", "REPLY");
                break;
            case OPEN_URL:
                m.put("type", "OPEN_URL");
                m.put("url", s.getUrl());
                if (s.getWebviewViewMode() != null) {
                    m.put("webviewViewMode", s.getWebviewViewMode());
                }
                break;
            case DIAL:
                m.put("type", "DIAL_PHONE");
                m.put("phoneNumber", s.getPhoneNumber());
                break;
            case SHOW_LOCATION:
                m.put("type", "SHOW_LOCATION");
                m.put("latitude", s.getLatitude());
                m.put("longitude", s.getLongitude());
                if (s.getLabel() != null) {
                    m.put("label", s.getLabel());
                }
                break;
            case REQUEST_LOCATION:
                m.put("type", "REQUEST_LOCATION");
                break;
            case CALENDAR:
                m.put("type", "CREATE_CALENDAR_EVENT");
                m.put("startTime", s.getStartTime());
                m.put("endTime", s.getEndTime());
                m.put("title", s.getCalTitle());
                m.put("description", s.getCalDescription());
                break;
            default:
                m.put("type", "REPLY");
                break;
        }
        return m;
    }
}

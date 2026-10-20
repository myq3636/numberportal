package com.liugh.rcs.channel.sinch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liugh.rcs.model.RcsCard;
import com.liugh.rcs.model.RcsContent;
import com.liugh.rcs.model.RcsMedia;
import com.liugh.rcs.model.RcsSuggestion;

import lombok.extern.slf4j.Slf4j;

/**
 * 内部 RcsContent → Sinch Conversation API POST {base}/messages:send 请求体
 * （Map，交给 fastjson 序列化）。
 *
 * <p>Sinch 是 canonical omnichannel 模型，与我方 RcsContent 几乎一一对应，
 * 故本 Mapper 是三家里翻译量最小的。两个 Sinch 特有约定：
 * ① 建议只能挂在 choice_message / card 上 → TEXT/FILE 带建议时升格为 choice_message；
 * ② ttl 是 Duration 字符串（"3600s"）而非整数。</p>
 */
@Slf4j
public final class SinchPayloadMapper {

    /** RCS 经 Sinch：卡片内 choice 上限 3。 */
    private static final int MAX_CARD_CHOICES = 3;
    /** RCS 经 Sinch：轮播外层 choice 上限 3（文档明确）。 */
    private static final int MAX_CAROUSEL_CHOICES = 3;
    /** Sinch 轮播卡片数 1–10。 */
    private static final int MAX_CAROUSEL_CARDS = 10;

    private SinchPayloadMapper() {
    }

    /**
     * @param content     规范化内容
     * @param appId       Sinch App ID（= 我方 ctx.sender：客户传的 sender 或 agent 绑定）
     * @param identity    收件号码（不带 +，带国家码）
     * @param msgId       我方 msgId → message_metadata + correlation_id，回调原样带回
     * @param ttl         有效期（秒），可为 null
     * @param smsFallback true → channel_priority_order=[RCS,SMS]，由 Sinch 原生多跳降级
     */
    public static Map<String, Object> toSendPayload(RcsContent content, String appId, String identity,
            String msgId, Integer ttl) {
        Map<String, Object> root = new HashMap<String, Object>();
        if (appId != null) {
            root.put("app_id", appId);
        }
        root.put("recipient", buildRecipient(identity));
        root.put("message", buildMessage(content));
        // DISPATCH：无状态投递，不建 conversation/contact——语义与我方"发一条就是一条"一致
        root.put("processing_mode", "DISPATCH");
        root.put("channel_priority_order",Arrays.asList("RCS"));
        if (msgId != null) {
            root.put("message_metadata", msgId);
            root.put("correlation_id", msgId);
        }
        if (ttl != null && ttl > 0) {
            root.put("ttl", ttl + "s");
        }
        return root;
    }

    private static Map<String, Object> buildRecipient(String identity) {
        Map<String, Object> ci = new HashMap<String, Object>();
        ci.put("channel", "RCS");
        ci.put("identity", identity);
        List<Object> identities = new ArrayList<Object>();
        identities.add(ci);
        Map<String, Object> identifiedBy = new HashMap<String, Object>();
        identifiedBy.put("channel_identities", identities);
        Map<String, Object> recipient = new HashMap<String, Object>();
        recipient.put("identified_by", identifiedBy);
        return recipient;
    }

    private static Map<String, Object> buildMessage(RcsContent content) {
        Map<String, Object> msg = new HashMap<String, Object>();
        switch (content.getType()) {
            case TEXT:
                putPlainOrChoice(msg, "text_message", textMessage(content.getText()), content.getSuggestions());
                break;
            case FILE:
                putPlainOrChoice(msg, "media_message", mediaMessage(content.getFile()), content.getSuggestions());
                break;
            case CARD:
                msg.put("card_message", buildCard(content.getCard()));
                break;
            case CAROUSEL:
                msg.put("carousel_message", buildCarousel(content));
                break;
            case TEMPLATE:
                throw new IllegalArgumentException(
                        "Sinch unsupported content type: " + content.getType());
            default:
                msg.put("text_message", textMessage(content.getText()));
                break;
        }
        return msg;
    }

    /**
     * 无建议 → 直接 text_message / media_message；
     * 带建议 → 升格为 choice_message{ <inner>, choices[] }（Sinch 的建议只能挂 choice_message/card）。
     * choice_message 的 choices 数量 Sinch 未对 RCS 给出上限 → 不截断，由我方 RcsContentValidator 把关。
     */
    private static void putPlainOrChoice(Map<String, Object> msg, String innerKey, Map<String, Object> inner,
            List<RcsSuggestion> suggestions) {
        List<Object> choices = buildChoices(suggestions, Integer.MAX_VALUE, null);
        if (choices == null) {
            msg.put(innerKey, inner);
            return;
        }
        Map<String, Object> choiceMessage = new HashMap<String, Object>();
        choiceMessage.put(innerKey, inner);
        choiceMessage.put("choices", choices);
        msg.put("choice_message", choiceMessage);
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
            c.put("media_message", mediaMessage(card.getMedia()));
        }
        // card.orientation / media.height 在 Conversation API 无对应字段 → 忽略
        List<Object> choices = buildChoices(card.getSuggestions(), MAX_CARD_CHOICES, "card");
        if (choices != null) {
            c.put("choices", choices);
        }
        return c;
    }

    private static Map<String, Object> buildCarousel(RcsContent content) {
        Map<String, Object> carousel = new HashMap<String, Object>();
        List<Object> cards = new ArrayList<Object>();
        List<RcsCard> src = content.getCards();
        if (src != null) {
            for (int i = 0; i < src.size(); i++) {
                if (i >= MAX_CAROUSEL_CARDS) {
                    log.warn("sinch carousel has {} cards but Sinch allows {} -> dropped {}",
                            src.size(), MAX_CAROUSEL_CARDS, src.size() - MAX_CAROUSEL_CARDS);
                    break;
                }
                cards.add(buildCard(src.get(i)));
            }
        }
        carousel.put("cards", cards);
        // 外层 choices 取消息级建议（RM 在 CAROUSEL 下丢弃了消息级建议，Sinch 能承载 → 多映射一层）
        List<Object> outer = buildChoices(content.getSuggestions(), MAX_CAROUSEL_CHOICES, "carousel outer");
        if (outer != null) {
            carousel.put("choices", outer);
        }
        return carousel;
    }

    private static Map<String, Object> textMessage(String text) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("text", text);
        return m;
    }

    private static Map<String, Object> mediaMessage(RcsMedia media) {
        Map<String, Object> m = new HashMap<String, Object>();
        if (media == null) {
            return m;
        }
        if (media.getUrl() != null) {
            m.put("url", media.getUrl());
        }
        if (media.getThumbnailUrl() != null) {
            m.put("thumbnail_url", media.getThumbnailUrl());
        }
        // filename_override 在 RCS 通道不支持 → 不发
        return m;
    }

    /** 超过通道上限时截断并打 WARN（不静默丢）。where=null 表示不限量。 */
    private static List<Object> buildChoices(List<RcsSuggestion> suggestions, int max, String where) {
        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }
        List<Object> list = new ArrayList<Object>();
        for (RcsSuggestion s : suggestions) {
            if (list.size() >= max) {
                log.warn("sinch {}: {} suggestions given but RCS/Sinch allows {} -> dropped {}",
                        where, suggestions.size(), max, suggestions.size() - max);
                break;
            }
            list.add(buildChoice(s));
        }
        return list;
    }

    private static Map<String, Object> buildChoice(RcsSuggestion s) {
        Map<String, Object> c = new HashMap<String, Object>();
        switch (s.getType()) {
            case OPEN_URL: {
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("title", s.getText());
                m.put("url", s.getUrl());
                c.put("url_message", m);
                break;
            }
            case DIAL: {
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("title", s.getText());
                m.put("phone_number", s.getPhoneNumber());
                c.put("call_message", m);
                break;
            }
            case SHOW_LOCATION: {
                Map<String, Object> coord = new HashMap<String, Object>();
                coord.put("latitude", s.getLatitude());
                coord.put("longitude", s.getLongitude());
                Map<String, Object> m = new HashMap<String, Object>();
                m.put("title", s.getText());
                if (s.getLabel() != null) {
                    m.put("label", s.getLabel());
                }
                m.put("coordinates", coord);
                c.put("location_message", m);
                break;
            }
            case REQUEST_LOCATION:
            case CALENDAR:
                // Conversation API 无通用的"请求位置"/"日历" choice（Infobip/RM 有）→ 降级为普通文本回复。
                // 属已知能力退化，见 docs/rcs-sinch-integration-design.md §4.5。
                c.put("text_message", textMessage(s.getText()));
                break;
            case REPLY:
            default:
                c.put("text_message", textMessage(s.getText()));
                break;
        }
        if (s.getPostbackData() != null) {
            c.put("postback_data", s.getPostbackData());
        }
        return c;
    }
}

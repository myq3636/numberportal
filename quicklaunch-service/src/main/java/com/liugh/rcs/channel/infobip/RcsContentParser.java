package com.liugh.rcs.channel.infobip;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.model.ContentType;
import com.liugh.rcs.model.RcsCard;
import com.liugh.rcs.model.RcsContent;
import com.liugh.rcs.model.RcsMedia;
import com.liugh.rcs.model.RcsSuggestion;
import com.liugh.rcs.model.RcsTemplateParameter;
import com.liugh.rcs.model.SuggestionType;

/**
 * 北向简化 JSON → 内部 RcsContent。
 * 同时兼容旧格式 {Sender,Recipient,Content,type}（映射为 TEXT/FILE）。
 */
public final class RcsContentParser {

    private RcsContentParser() {
    }

    /**
     * 解析请求体的 content 部分。若不存在 content 字段，则回退到旧格式。
     */
    public static RcsContent parse(JSONObject request) {
        JSONObject content = request.getJSONObject("content");
        if (content == null) {
            return parseLegacy(request);
        }
        RcsContent c = new RcsContent();
        c.setType(ContentType.from(content.getString("type")));
        switch (c.getType()) {
            case TEXT:
                c.setText(content.getString("text"));
                c.setSuggestions(parseSuggestions(content.getJSONArray("suggestions")));
                break;
            case FILE:
                c.setFile(parseMedia(content.getJSONObject("file")));
                c.setSuggestions(parseSuggestions(content.getJSONArray("suggestions")));
                break;
            case CARD:
                c.setCard(parseCard(content.getJSONObject("card")));
                break;
            case CAROUSEL:
                List<RcsCard> cards = new ArrayList<RcsCard>();
                JSONArray arr = content.getJSONArray("cards");
                if (arr != null) {
                    for (int i = 0; i < arr.size(); i++) {
                        cards.add(parseCard(arr.getJSONObject(i)));
                    }
                }
                c.setCardWidth(content.getString("cardWidth")== null? "MEDIUM":content.getString("cardWidth"));
                c.setCards(cards);
                // 消息级 suggestions（轮播外层建议）：Infobip/RM 的 CAROUSEL 不读此字段，行为不变；
                // Sinch 能承载为 carousel_message.choices（外层，上限 3）。
                c.setSuggestions(parseSuggestions(content.getJSONArray("suggestions")));
                break;
            case TEMPLATE:
                c.setTemplateId(content.getString("templateId"));
                c.setParameters(parseTemplateParameters(content.getJSONArray("parameters")));
                break;
            default:
                break;
        }
        return c;
    }

    private static RcsContent parseLegacy(JSONObject request) {
        RcsContent c = new RcsContent();
        String type = request.getString("type");
        String body = request.getString("Content");
        if ("FILE".equalsIgnoreCase(type)) {
            c.setType(ContentType.FILE);
            RcsMedia m = new RcsMedia();
            m.setUrl(body);
            c.setFile(m);
        } else {
            c.setType(ContentType.TEXT);
            c.setText(body);
        }
        return c;
    }

    private static RcsCard parseCard(JSONObject o) {
        if (o == null) {
            return null;
        }
        RcsCard card = new RcsCard();
        card.setTitle(o.getString("title"));
        card.setDescription(o.getString("description"));
        card.setOrientation(o.getString("orientation"));
        card.setAlignment(o.getString("alignment")==null? "LEFT":o.getString("alignment"));
        card.setCardWidth(o.getString("cardWidth")== null? "MEDIUM":o.getString("cardWidth"));
        card.setMedia(parseMedia(o.getJSONObject("media")));
        card.setSuggestions(parseSuggestions(o.getJSONArray("suggestions")));
        return card;
    }

    private static RcsMedia parseMedia(JSONObject o) {
        if (o == null) {
            return null;
        }
        RcsMedia m = new RcsMedia();
        m.setUrl(o.getString("url"));
        m.setThumbnailUrl(o.getString("thumbnailUrl"));
        m.setMimeType(o.getString("mimeType"));
        m.setHeight(o.getString("height"));
        return m;
    }

    private static List<RcsSuggestion> parseSuggestions(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return null;
        }
        List<RcsSuggestion> list = new ArrayList<RcsSuggestion>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);
            RcsSuggestion s = new RcsSuggestion();
            s.setType(SuggestionType.from(o.getString("type")));
            s.setText(o.getString("text"));
            s.setPostbackData(o.getString("postbackData"));
            s.setUrl(o.getString("url"));
            s.setWebviewViewMode(o.getString("webviewViewMode"));
            s.setPhoneNumber(o.getString("phoneNumber"));
            s.setLatitude(o.getDouble("latitude"));
            s.setLongitude(o.getDouble("longitude"));
            s.setLabel(o.getString("label"));
            s.setStartTime(o.getString("startTime"));
            s.setEndTime(o.getString("endTime"));
            s.setCalTitle(o.getString("calTitle"));
            s.setCalDescription(o.getString("calDescription"));
            list.add(s);
        }
        return list;
    }

    private static List<RcsTemplateParameter> parseTemplateParameters(JSONArray arr) {
        if (arr == null || arr.isEmpty()) {
            return null;
        }
        List<RcsTemplateParameter> list = new ArrayList<RcsTemplateParameter>();
        for (int i = 0; i < arr.size(); i++) {
            JSONObject o = arr.getJSONObject(i);
            RcsTemplateParameter p = new RcsTemplateParameter();
            p.setName(o.getString("name"));
            p.setValue(o.getString("value"));
            list.add(p);
        }
        return list;
    }
}

package com.liugh.rcs.channel.spark;

import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.model.RcsCard;
import com.liugh.rcs.model.RcsContent;
import com.liugh.rcs.model.RcsMedia;
import com.liugh.rcs.model.RcsSuggestion;
import com.liugh.rcs.model.SuggestionType;

/**
 * 内部 {@link RcsContent} -> Spark POST /sendRCSMessage 请求体。
 *
 * <p>只做字段翻译与 Spark 专属限额校验；不打印完整 payload（含 password）。
 * 校验失败抛 {@link IllegalArgumentException}，由适配器统一转 {@code RcsChannelSubmitException}。</p>
 */
public final class SparkPayloadMapper {

    private static final int MAX_TITLE = 200;
    private static final int MAX_DESC = 200;
    private static final int MAX_URL_IMAGE = 200;
    private static final int MAX_LINK_TEXT = 25;
    private static final int MIN_CAROUSEL = 2;
    private static final int MAX_CAROUSEL = 5;

    private SparkPayloadMapper() {
    }

    public static JSONObject toPayload(RcsContent content, RcsSendCtx ctx) {
        if (isBlank(ctx.getAuthUser()) || isBlank(ctx.getAuthPass())) {
            throw new IllegalArgumentException("Spark missing user/password");
        }
        JSONObject root = new JSONObject();
        root.put("user", ctx.getAuthUser());
        root.put("password", ctx.getAuthPass());
        root.put("phoneNumber", SparkPhoneNormalizer.normalize(ctx.getRecipient(), ctx.getCountryCode()));
        if (notBlank(ctx.getFallbackText())) {
            root.put("fallbackMessage", ctx.getFallbackText());
        }
        if (notBlank(ctx.getChannelSender())) {
            root.put("mask", ctx.getChannelSender());
        }
        switch (content.getType()) {
            case TEXT:
                root.put("cardHeader", toTextHeader(content));
                break;
            case FILE:
                root.put("cardHeader", toFileHeader(content));
                break;
            case CARD:
                root.put("cardHeader", toCardHeader(content.getCard(), false));
                break;
            case CAROUSEL:
                root.put("carousel", toCarousel(content.getCards()));
                break;
            default:
                throw new IllegalArgumentException("Spark unsupported content type: " + content.getType());
        }
        return root;
    }

    private static JSONObject toTextHeader(RcsContent content) {
        if (isBlank(content.getText())) {
            throw new IllegalArgumentException("Spark TEXT.text is blank");
        }
        JSONObject header = new JSONObject();
        header.put("description", checkLength(content.getText(), MAX_DESC, "TEXT.text"));
        header.put("horizontal", false);
        header.put("bigImages", false);
        return header;
    }

    private static JSONObject toFileHeader(RcsContent content) {
        RcsMedia media = content.getFile();
        if (media == null || isBlank(media.getUrl())) {
            throw new IllegalArgumentException("Spark FILE.file.url is blank");
        }
        if (!isImageOrVideo(media)) {
            throw new IllegalArgumentException("Spark FILE only supports image/video media");
        }
        JSONObject header = new JSONObject();
        header.put("urlImage", checkLength(media.getUrl(), MAX_URL_IMAGE, "FILE.urlImage"));
        if (notBlank(media.getThumbnailUrl())) {
            header.put("urlThumbnail", media.getThumbnailUrl());
        }
        header.put("horizontal", false);
        header.put("bigImages", false);
        return header;
    }

    private static JSONObject toCardHeader(RcsCard card, boolean carouselCard) {
        if (card == null) {
            throw new IllegalArgumentException("Spark CARD.card is null");
        }
        JSONObject header = new JSONObject();
        if (notBlank(card.getTitle())) {
            header.put("title", checkLength(card.getTitle(), MAX_TITLE, "card.title"));
        }
        if (notBlank(card.getDescription())) {
            header.put("description", checkLength(card.getDescription(), MAX_DESC, "card.description"));
        }
        RcsMedia media = card.getMedia();
        if (media != null && notBlank(media.getUrl())) {
            header.put("urlImage", checkLength(media.getUrl(), MAX_URL_IMAGE, "card.urlImage"));
            if (notBlank(media.getThumbnailUrl())) {
                header.put("urlThumbnail", media.getThumbnailUrl());
            }
        }
        JSONObject link = buildFirstLink(card.getSuggestions());
        if (link != null) {
            header.put("link", link);
        }
        header.put("horizontal", isHorizontal(card));
        header.put("bigImages", carouselCard);
        return header;
    }

    private static JSONObject toCarousel(List<RcsCard> cards) {
        if (cards == null || cards.size() < MIN_CAROUSEL || cards.size() > MAX_CAROUSEL) {
            throw new IllegalArgumentException("Spark CAROUSEL.cards must be "
                    + MIN_CAROUSEL + "-" + MAX_CAROUSEL);
        }
        JSONObject carousel = new JSONObject();
        JSONArray arr = new JSONArray();
        for (RcsCard card : cards) {
            arr.add(toCardHeader(card, true));
        }
        carousel.put("cards", arr);
        carousel.put("horizontal", true);
        carousel.put("bigImages", true);
        return carousel;
    }

    private static JSONObject buildFirstLink(List<RcsSuggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return null;
        }
        for (RcsSuggestion s : suggestions) {
            if (s.getType() != SuggestionType.OPEN_URL) {
                continue;
            }
            if (isBlank(s.getUrl())) {
                continue;
            }
            String text = s.getText();
            if (isBlank(text)) {
                // Spark link.text 必填；无文案的 OPEN_URL 无法成 link，跳过
                continue;
            }
            JSONObject link = new JSONObject();
            link.put("text", checkLength(text, MAX_LINK_TEXT, "link.text"));
            link.put("url", s.getUrl());
            return link;
        }
        return null;
    }

    private static boolean isImageOrVideo(RcsMedia media) {
        String mime = media.getMimeType();
        if (mime != null) {
            String lower = mime.toLowerCase();
            if (lower.startsWith("image/") || lower.startsWith("video/")) {
                return true;
            }
        }
        String url = media.getUrl();
        if (url != null) {
            return url.toLowerCase().matches(".*\\.(png|jpg|jpeg|gif|webp|mp4|mov|m4v|avi)(\\?.*)?$");
        }
        return false;
    }

    private static boolean isHorizontal(RcsCard card) {
        return "HORIZONTAL".equalsIgnoreCase(card.getOrientation());
    }

    private static String checkLength(String value, int max, String field) {
        if (value.length() > max) {
            throw new IllegalArgumentException("Spark " + field + " exceeds " + max);
        }
        return value;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

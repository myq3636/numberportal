package com.liugh.rcs.channel.spark;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.model.ContentType;
import com.liugh.rcs.model.RcsCard;
import com.liugh.rcs.model.RcsContent;
import com.liugh.rcs.model.RcsMedia;
import com.liugh.rcs.model.RcsSuggestion;
import com.liugh.rcs.model.SuggestionType;

public class SparkPayloadMapperTest {

    @Test
    public void mapsCardHeader() {
        RcsContent content = new RcsContent();
        content.setType(ContentType.CARD);
        RcsCard card = new RcsCard();
        card.setTitle("title");
        card.setDescription("desc");
        RcsMedia media = new RcsMedia();
        media.setUrl("https://cdn.example.com/a.png");
        media.setThumbnailUrl("https://cdn.example.com/a-thumb.png");
        card.setMedia(media);
        RcsSuggestion s = new RcsSuggestion();
        s.setType(SuggestionType.OPEN_URL);
        s.setText("open");
        s.setUrl("https://example.com");
        card.setSuggestions(Arrays.asList(s));
        card.setOrientation("HORIZONTAL");
        content.setCard(card);

        JSONObject root = SparkPayloadMapper.toPayload(content, ctx());

        assertEquals("SPARK_USER", root.getString("user"));
        assertEquals("SPARK_PASS", root.getString("password"));
        assertEquals("5512345678", root.getString("phoneNumber"));
        assertEquals("fallback sms", root.getString("fallbackMessage"));
        assertEquals("SPARK_MASK", root.getString("mask"));
        JSONObject header = root.getJSONObject("cardHeader");
        assertEquals("title", header.getString("title"));
        assertEquals("desc", header.getString("description"));
        assertEquals("https://cdn.example.com/a.png", header.getString("urlImage"));
        assertEquals("https://cdn.example.com/a-thumb.png", header.getString("urlThumbnail"));
        assertTrue(header.getBooleanValue("horizontal"));
        assertFalse(header.getBooleanValue("bigImages"));
        JSONObject link = header.getJSONObject("link");
        assertEquals("open", link.getString("text"));
        assertEquals("https://example.com", link.getString("url"));
    }

    @Test
    public void mapsCarouselTwoToFiveCards() {
        RcsContent content = new RcsContent();
        content.setType(ContentType.CAROUSEL);
        content.setCards(Arrays.asList(card("t1", "d1"), card("t2", "d2")));

        JSONObject root = SparkPayloadMapper.toPayload(content, ctx());
        JSONObject carousel = root.getJSONObject("carousel");
        assertTrue(carousel.getBooleanValue("horizontal"));
        assertTrue(carousel.getBooleanValue("bigImages"));
        JSONArray cards = carousel.getJSONArray("cards");
        assertEquals(2, cards.size());
        assertEquals("t1", cards.getJSONObject(0).getString("title"));
    }

    @Test
    public void rejectsSingleCardCarousel() {
        RcsContent content = new RcsContent();
        content.setType(ContentType.CAROUSEL);
        content.setCards(Arrays.asList(card("t1", "d1")));
        expectFail(content);
    }

    @Test
    public void rejectsCarouselMoreThanFive() {
        RcsContent content = new RcsContent();
        content.setType(ContentType.CAROUSEL);
        content.setCards(Arrays.asList(
                card("a", "1"), card("b", "2"), card("c", "3"),
                card("d", "4"), card("e", "5"), card("f", "6")));
        expectFail(content);
    }

    @Test
    public void mapsTextToDescription() {
        RcsContent content = new RcsContent();
        content.setType(ContentType.TEXT);
        content.setText("hello");
        JSONObject root = SparkPayloadMapper.toPayload(content, ctx());
        assertEquals("hello", root.getJSONObject("cardHeader").getString("description"));
    }

    @Test
    public void rejectsDescriptionOverTwoHundred() {
        RcsContent content = new RcsContent();
        content.setType(ContentType.CARD);
        RcsCard c = new RcsCard();
        c.setTitle("t");
        c.setDescription(repeat("x", 201));
        content.setCard(c);
        expectFail(content);
    }

    @Test
    public void rejectsMissingCredentials() {
        RcsContent content = new RcsContent();
        content.setType(ContentType.TEXT);
        content.setText("hello");
        RcsSendCtx ctx = ctx();
        ctx.setAuthPass(null);
        try {
            SparkPayloadMapper.toPayload(content, ctx);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    @Test
    public void fileOnlyAcceptsImageOrVideo() {
        RcsContent content = new RcsContent();
        content.setType(ContentType.FILE);
        RcsMedia media = new RcsMedia();
        media.setUrl("https://cdn.example.com/doc.pdf");
        media.setMimeType("application/pdf");
        content.setFile(media);
        expectFail(content);
    }

    private static RcsSendCtx ctx() {
        RcsSendCtx ctx = new RcsSendCtx();
        ctx.setAuthUser("SPARK_USER");
        ctx.setAuthPass("SPARK_PASS");
        ctx.setRecipient("+525512345678");
        ctx.setCountryCode("MX");
        ctx.setChannelSender("SPARK_MASK");
        ctx.setFallbackText("fallback sms");
        return ctx;
    }

    private static RcsCard card(String title, String desc) {
        RcsCard c = new RcsCard();
        c.setTitle(title);
        c.setDescription(desc);
        return c;
    }

    private static void expectFail(RcsContent content) {
        try {
            SparkPayloadMapper.toPayload(content, ctx());
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}

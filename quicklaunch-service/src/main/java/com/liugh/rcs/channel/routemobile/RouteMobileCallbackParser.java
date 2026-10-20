package com.liugh.rcs.channel.routemobile;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.RcsMoInfo;
import com.liugh.rcs.channel.RcsCallbackResult;
import com.liugh.rcs.channel.RcsDrEvent;

/**
 * Route Mobile 回调（单事件裸 JSON，无签名）→ 归一化事件。
 * event_type ∈ {status, text_message, media_message, location, response}。
 * 关联键 extra（= 我方 msgId），回退 message_id。
 */
public final class RouteMobileCallbackParser {

    public static final String CODE = "ROUTEMOBILE";

    private RouteMobileCallbackParser() {
    }

    public static RcsCallbackResult parse(JSONObject raw) {
        RcsCallbackResult result = new RcsCallbackResult();
        String ev = raw.getString("event_type");
        if ("status".equalsIgnoreCase(ev)) {
            result.getDrEvents().add(parseDr(raw));
        } else {
            result.getInboundEvents().add(parseInbound(ev, raw));
        }
        return result;
    }

    private static RcsDrEvent parseDr(JSONObject raw) {
        RcsDrEvent e = new RcsDrEvent();
        e.setOurMsgId(raw.getString("extra"));
        e.setChannelMsgId(raw.getString("message_id"));
        String ms = raw.getString("message_status");
        if ("DELIVERED".equalsIgnoreCase(ms)) {
            e.setStatus(3);
        } else if ("READ".equalsIgnoreCase(ms)) {
            e.setStatus(9);
        } else if ("FAILED".equalsIgnoreCase(ms)) {
            e.setStatus(4);
            e.setErrorReason(raw.getString("failure_reason"));
        } else if ("fallback".equalsIgnoreCase(ms)) {
            e.setStatus(12);
            e.setFallbackMsgId(firstNonBlank(raw.getString("whatsapp_message_id"), raw.getString("sms_message_id")));
        } else {
            e.setStatus(null);
        }
        return e;
    }

    private static RcsMoInfo parseInbound(String ev, JSONObject raw) {
        long now = System.currentTimeMillis();
        RcsMoInfo mo = new RcsMoInfo();
        mo.setChannelCode(CODE);
        mo.setMsisdn(firstNonBlank(raw.getString("phone_no"), raw.getString("from"), raw.getString("msisdn")));
        mo.setClientMsgId(raw.getString("message_id"));
        mo.setRelatedMsgId(raw.getString("extra"));
        mo.setReceiveTime(now);
        mo.setCreateTime(now);
        mo.setPushStatus(0);
        mo.setPushRetry(0);

        if ("text_message".equalsIgnoreCase(ev)) {
            mo.setEventType("USER_MESSAGE");
            mo.setContentType("TEXT");
            mo.setText(firstNonBlank(raw.getString("text"), raw.getString("text_message")));
        } else if ("media_message".equalsIgnoreCase(ev)) {
            mo.setEventType("USER_MESSAGE");
            mo.setContentType("FILE");
            mo.setFileUri(firstNonBlank(raw.getString("media_uri"), raw.getString("url")));
            mo.setMimeType(raw.getString("media_type"));
        } else if ("location".equalsIgnoreCase(ev)) {
            mo.setEventType("USER_MESSAGE");
            mo.setContentType("LOCATION");
            mo.setLatitude(raw.getDouble("latitude"));
            mo.setLongitude(raw.getDouble("longitude"));
        } else if ("response".equalsIgnoreCase(ev)) {
            mo.setEventType("SUGGESTION_RESPONSE");
            mo.setContentType("SUGGESTION");
            mo.setPostbackData(firstNonBlank(raw.getString("response_postback"), raw.getString("postback")));
            mo.setSuggestionText(firstNonBlank(raw.getString("response_text"), raw.getString("text")));
            mo.setSuggestionType(raw.getString("suggestion_type"));
        } else {
            mo.setEventType("USER_MESSAGE");
            mo.setContentType("TEXT");
        }
        return mo;
    }

    private static String firstNonBlank(String... vals) {
        if (vals == null) {
            return null;
        }
        for (String v : vals) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }
}

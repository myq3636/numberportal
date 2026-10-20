package com.liugh.rcs.channel.sinch;

import com.alibaba.fastjson.JSONObject;
import com.liugh.entity.RcsMoInfo;
import com.liugh.rcs.channel.RcsCallbackResult;
import com.liugh.rcs.channel.RcsDrEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Sinch Conversation API 回调（单事件信封，顶层公共字段 + 一个区分性 key）→ 归一化事件。
 * 判别：message_delivery_report → DR；message.contact_message → 上行；其余 trigger P1 忽略。
 * 关联键 message_delivery_report.metadata（= 我方 msgId），回退顶层 message_metadata，再回退 message_id。
 *
 * <p>注意：Sinch 回调带 HMAC-SHA256 签名头（三家里唯一），但 P1 未验签——SPI 的
 * parseCallback(JSONObject) 拿不到 HTTP 头与原始字节，而 HMAC 必须对 raw body 计算。
 * 详见 docs/rcs-sinch-integration-design.md §7.4。</p>
 */
@Slf4j
public final class SinchCallbackParser {

    public static final String CODE = "SINCH";

    private SinchCallbackParser() {
    }

    public static RcsCallbackResult parse(JSONObject raw) {
        RcsCallbackResult result = new RcsCallbackResult();
        if (raw == null) {
            return result;
        }
        JSONObject dr = raw.getJSONObject("message_delivery_report");
        if (dr != null) {
            result.getDrEvents().add(parseDr(dr, raw));
            return result;
        }
        JSONObject message = raw.getJSONObject("message");
        if (message != null && message.getJSONObject("contact_message") != null) {
            result.getInboundEvents().add(parseInbound(message, raw));
            return result;
        }
        // EVENT_DELIVERY / EVENT_INBOUND / CAPABILITY / OPT_IN / OPT_OUT / CONVERSATION_* ...：P1 不处理。
        // 返回空结果而非报错 → 控制器仍回 200，避免 Sinch 判定失败后反复重投。
        log.info("sinch callback ignored because the trigger is unsupported");
        return result;
    }

    private static RcsDrEvent parseDr(JSONObject dr, JSONObject raw) {
        RcsDrEvent e = new RcsDrEvent();
        e.setOurMsgId(firstNonBlank(dr.getString("metadata"), raw.getString("message_metadata")));
        e.setChannelMsgId(dr.getString("message_id"));

        String status = dr.getString("status");
        if ("DELIVERED".equalsIgnoreCase(status)) {
            e.setStatus(3);
        } else if ("READ".equalsIgnoreCase(status)) {
            e.setStatus(9);
        } else if ("FAILED".equalsIgnoreCase(status)) {
            e.setStatus(4);
            JSONObject reason = dr.getJSONObject("reason");
            if (reason != null) {
                e.setErrorCode(reason.getString("code"));
                e.setErrorReason(firstNonBlank(reason.getString("description"), reason.getString("sub_code")));
            }
        } else if ("SWITCHING_CHANNEL".equalsIgnoreCase(status)) {
            // RCS 投递失败、Sinch 正按 channel_priority_order 切下一通道 → 复用 RM 的"已降级"(12) 语义。
            // 非终态：切换后成功还会再来一条 DELIVERED 把 CDR 推到 3。
            e.setStatus(12);
        } else {
            // QUEUED_ON_CHANNEL 及未知：中间态 → 不推进状态、不回推客户（与 Infobip PENDING/ACCEPTED 一致）
            e.setStatus(null);
        }
        // Sinch 回调不回传单价 → cost/currency 不填（Infobip 有 price，RM/Sinch 无）
        return e;
    }

    private static RcsMoInfo parseInbound(JSONObject message, JSONObject raw) {
        long now = System.currentTimeMillis();
        RcsMoInfo mo = new RcsMoInfo();
        mo.setChannelCode(CODE);
        mo.setClientMsgId(message.getString("id"));
        mo.setRelatedMsgId(raw.getString("message_metadata"));
        mo.setReceiveTime(now);
        mo.setCreateTime(now);
        mo.setPushStatus(0);
        mo.setPushRetry(0);

        JSONObject ci = message.getJSONObject("channel_identity");
        if (ci != null) {
            mo.setMsisdn(ci.getString("identity"));
        }

        JSONObject cm = message.getJSONObject("contact_message");
        JSONObject choice = cm.getJSONObject("choice_response_message");
        JSONObject text = cm.getJSONObject("text_message");
        JSONObject media = cm.getJSONObject("media_message");
        JSONObject location = cm.getJSONObject("location_message");

        if (choice != null) {
            mo.setEventType("SUGGESTION_RESPONSE");
            mo.setContentType("SUGGESTION");
            mo.setPostbackData(choice.getString("postback_data"));
            // choice_response_message.message_id = 被回复的那条原消息 id（非本次上行自身 id）
            mo.setRelatedMsgId(firstNonBlank(choice.getString("message_id"), mo.getRelatedMsgId()));
        } else if (text != null) {
            mo.setEventType("USER_MESSAGE");
            mo.setContentType("TEXT");
            mo.setText(text.getString("text"));
        } else if (media != null) {
            mo.setEventType("USER_MESSAGE");
            mo.setContentType("FILE");
            mo.setFileUri(media.getString("url"));
            mo.setThumbnailUri(media.getString("thumbnail_url"));
        } else if (location != null) {
            mo.setEventType("USER_MESSAGE");
            mo.setContentType("LOCATION");
            JSONObject coord = location.getJSONObject("coordinates");
            if (coord != null) {
                mo.setLatitude(coord.getDouble("latitude"));
                mo.setLongitude(coord.getDouble("longitude"));
            }
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

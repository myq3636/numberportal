package com.liugh.rcs.channel.avant;

import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.channel.RcsCallbackResult;
import com.liugh.rcs.channel.RcsDrEvent;

public final class AvantCallbackParser {
    private AvantCallbackParser() {
    }

    public static RcsCallbackResult parse(JSONObject raw) {
        RcsCallbackResult result = new RcsCallbackResult();
        if (raw == null) {
            return result;
        }
        if ("CLICKED".equalsIgnoreCase(raw.getString("status"))) {
            return result;
        }

        RcsDrEvent event = new RcsDrEvent();
        event.setOurMsgId(raw.getString("key"));
        event.setChannelMsgId(raw.getString("id"));
        event.setErrorCode(raw.getString("errorCode"));
        event.setErrorReason(raw.getString("reason"));
        String status = raw.getString("status");
        if ("DELIVRD".equalsIgnoreCase(status)) {
            event.setStatus(3);
        } else if ("UNDELIV".equalsIgnoreCase(status)
                || "REJECTD".equalsIgnoreCase(status)) {
            event.setStatus(4);
        } else if ("EXPIRED".equalsIgnoreCase(status)) {
            event.setStatus(5);
        } else if ("READED".equalsIgnoreCase(status)) {
            event.setStatus(9);
        } else {
            event.setStatus(null);
        }
        result.getDrEvents().add(event);
        return result;
    }
}

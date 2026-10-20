package com.liugh.rcs.channel.avant;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.channel.RcsCallbackResult;
import com.liugh.rcs.channel.RcsChannelAdapter;
import com.liugh.rcs.channel.RcsChannelCapability;
import com.liugh.rcs.channel.RcsChannelSubmitException;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.model.RcsContent;

@Component
public class AvantChannelAdapter implements RcsChannelAdapter {
    public static final String CODE = "AVANT";
    private static final Set<RcsChannelCapability> CAPABILITIES =
            Collections.unmodifiableSet(EnumSet.of(
                    RcsChannelCapability.MESSAGE_SEND));

    private final AvantHttpExecutor http;

    @Autowired
    public AvantChannelAdapter(AvantHttpExecutor http) {
        this.http = http;
    }

    @Override
    public String channelCode() {
        return CODE;
    }

    @Override
    public Set<RcsChannelCapability> capabilities() {
        return CAPABILITIES;
    }

    @Override
    public String send(RcsContent content, RcsSendCtx ctx) throws Exception {
        if (ctx == null || isBlank(ctx.getBaseUrl())
                || isBlank(ctx.getApiKey())) {
            throw new RcsChannelSubmitException("AVANT_CONFIG_INVALID",
                    "Avant baseUrl and token are required", false);
        }
        JSONObject payload;
        try {
            payload = AvantPayloadMapper.toPayload(content, ctx);
        } catch (IllegalArgumentException e) {
            throw new RcsChannelSubmitException("AVANT_PAYLOAD_INVALID",
                    e.getMessage(), false);
        }
        String url = trimTrailingSlash(ctx.getBaseUrl()) + "/message/send";
        try {
            AvantHttpResponse response = http.postJson(url,
                    "alpha " + ctx.getApiKey(), payload.toJSONString());
            return AvantResponseMapper.messageId(response);
        } catch (RcsChannelSubmitException e) {
            throw e;
        } catch (Exception e) {
            throw new RcsChannelSubmitException("AVANT_SUBMIT_UNKNOWN",
                    "Avant request outcome is unknown", false);
        }
    }

    @Override
    public RcsCallbackResult parseCallback(JSONObject raw) {
        return AvantCallbackParser.parse(raw);
    }

    private static String trimTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

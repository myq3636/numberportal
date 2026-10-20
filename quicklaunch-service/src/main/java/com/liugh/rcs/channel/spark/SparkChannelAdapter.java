package com.liugh.rcs.channel.spark;

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

import lombok.extern.slf4j.Slf4j;

/**
 * Spark（Voices Unified Communications）通道适配器。
 * 发送走 POST {base}/sendRCSMessage，认证在 body（user/password）；无回调/DR。
 */
@Component
@Slf4j
public class SparkChannelAdapter implements RcsChannelAdapter {

    public static final String CODE = "SPARK";

    private static final Set<RcsChannelCapability> CAPABILITIES =
            Collections.unmodifiableSet(EnumSet.of(RcsChannelCapability.MESSAGE_SEND));

    @Autowired
    private SparkHttpExecutor httpExecutor;

    // package-private，仅供同包单测注入
    void setHttpExecutor(SparkHttpExecutor httpExecutor) {
        this.httpExecutor = httpExecutor;
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
        String baseUrl = trimTrailingSlash(ctx.getBaseUrl());
        if (baseUrl == null) {
            log.error("spark missing baseUrl, rssid:{}", ctx.getRssid());
            throw new RcsChannelSubmitException("SPARK_PAYLOAD_INVALID", "missing baseUrl");
        }
        JSONObject payload;
        try {
            payload = SparkPayloadMapper.toPayload(content, ctx);
        } catch (IllegalArgumentException e) {
            log.warn("spark payload invalid, msgId:{}, reason:{}", ctx.getMsgId(), e.getMessage());
            throw new RcsChannelSubmitException("SPARK_PAYLOAD_INVALID", e.getMessage());
        }
        String url = baseUrl + "/sendRCSMessage";
        String raw = httpExecutor.postJson(url, payload.toJSONString());
        SparkResponseMapper.Result r = SparkResponseMapper.parse(raw);
        if (r.isSuccess()) {
            if (r.getUuid() == null) {
                log.warn("spark code=200 without uuid, msgId:{}", ctx.getMsgId());
                throw new RcsChannelSubmitException("SPARK_SUBMIT_UNKNOWN", "code=200 without uuid");
            }
            return r.getUuid();
        }
        throw new RcsChannelSubmitException(r.getErrorCode(), r.getDesc());
    }

    @Override
    public RcsCallbackResult parseCallback(JSONObject raw) {
        // Spark 无回调接口，P1 返回空结果，不产生任何 DR/上行事件。
        return new RcsCallbackResult();
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t.isEmpty() ? null : t;
    }
}

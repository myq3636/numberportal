package com.liugh.rcs.channel.sinch;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.liugh.rcs.channel.RcsCallbackResult;
import com.liugh.rcs.channel.RcsChannelAdapter;
import com.liugh.rcs.channel.RcsChannelCapability;
import com.liugh.rcs.channel.RcsSendCtx;
import com.liugh.rcs.model.RcsContent;
import com.liugh.service.util.ThirdPartyLoginHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * Sinch 通道适配器。OAuth2 client_credentials 鉴权（令牌缓存 + expires_in 驱动刷新），
 * 发送走 Conversation API {base}/messages:send（DISPATCH 模式）。
 * 提交失败（无 message_id 返回）时刷新令牌重试一次（覆盖令牌过期/被吊销的 401 场景）。
 */
@Component
@Slf4j
public class SinchChannelAdapter implements RcsChannelAdapter {

    public static final String CODE = "SINCH";

    private static final Set<RcsChannelCapability> CAPABILITIES =
            Collections.unmodifiableSet(EnumSet.of(RcsChannelCapability.MESSAGE_SEND));

    /** admin_info.url 必须含此路径段：projectId 拼在 base 里（见设计文档 §6.1）——配置防呆。 */
    private static final String PROJECT_PATH = "/v1/projects/";

    @Autowired
    private SinchTokenManager tokenManager;

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
        if (baseUrl == null || !baseUrl.contains(PROJECT_PATH)) {
            // 不拼一个必然 404 的地址静默失败，直接指出是哪条供应商配置错了
            log.error("sinch baseUrl must contain {}{}, got:{} -> check admin_info.url of rssid:{}",
                    PROJECT_PATH, "{projectId}", ctx.getBaseUrl(), ctx.getRssid());
            return null;
        }
        if (ctx.getSender() == null) {
            log.error("sinch missing app_id (sender/agent binding), msgId:{}", ctx.getMsgId());
            return null;
        }
        // Sinch 原生多跳降级：请求带 fallbackText 或供应商配了 sms_sender → channel_priority_order=[RCS,SMS]
        //boolean smsFallback = notBlank(ctx.getFallbackText()) || notBlank(ctx.getSmsSender());
        Map<String, Object> payload = SinchPayloadMapper.toSendPayload(
                content, ctx.getChannelSender(), stripPlus(ctx.getRecipient()), ctx.getMsgId(), ctx.getTtl());

        // 连接凭证来自供应商 admin_info：baseUrl=admin.url（含 projectId），
        // keyId=admin.appId（=ctx.authUser），keySecret=admin.appSecurity（=ctx.authPass）
        String key = ctx.getChannelCode();
        String token = tokenManager.getToken(key, ctx.getAuthUser(), ctx.getAuthPass());
        if (token == null) {
            log.error("sinch no token, channel:{}", key);
            return null;
        }
        String id = ThirdPartyLoginHelper.sendSinchRCSMsg(payload, baseUrl, token);
        if (id != null) {
            return id;
        }
        // 无 message_id 返回：可能令牌过期/被吊销 → 刷新后重试一次
        log.warn("sinch send returned no message_id, refresh token and retry once, msgId:{}", ctx.getMsgId());
        tokenManager.invalidate(key);
        token = tokenManager.getToken(key, ctx.getAuthUser(), ctx.getAuthPass());
        if (token == null) {
            return null;
        }
        return ThirdPartyLoginHelper.sendSinchRCSMsg(payload, baseUrl, token);
    }

    @Override
    public RcsCallbackResult parseCallback(JSONObject raw) {
        return SinchCallbackParser.parse(raw);
    }

    private static String stripPlus(String e164) {
        if (e164 != null && e164.startsWith("+")) {
            return e164.substring(1);
        }
        return e164;
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

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}

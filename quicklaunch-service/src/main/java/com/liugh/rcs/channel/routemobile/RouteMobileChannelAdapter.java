package com.liugh.rcs.channel.routemobile;

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
 * Route Mobile 通道适配器。JWT 鉴权（登录换令牌 + 缓存），发送走 /rcs/v1/message。
 * 提交失败（无 id 返回）时刷新令牌重试一次（覆盖令牌过期/被撤销的 401 场景）。
 */
@Component
@Slf4j
public class RouteMobileChannelAdapter implements RcsChannelAdapter {

    public static final String CODE = "ROUTEMOBILE";

    private static final Set<RcsChannelCapability> CAPABILITIES =
            Collections.unmodifiableSet(EnumSet.of(RcsChannelCapability.MESSAGE_SEND));

    @Autowired
    private RouteMobileTokenManager tokenManager;

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
        String phoneNo = stripPlus(ctx.getRecipient());
        Map<String, Object> payload = RouteMobilePayloadMapper.toSendPayload(
                content, ctx.getSender(), phoneNo, ctx.getMsgId(), ctx.getTtl());

        // 连接凭证来自供应商 admin_info：baseUrl=admin.url，user=admin.appId，pass=admin.appSecurity
        String key = ctx.getChannelCode();
        String baseUrl = ctx.getBaseUrl();
        String jwt = tokenManager.getToken(key, baseUrl, ctx.getAuthUser(), ctx.getAuthPass());
        if (jwt == null) {
            log.error("routemobile no token, channel:{}", key);
            return null;
        }
        String id = ThirdPartyLoginHelper.sendRouteMobileRCSMsg(payload, baseUrl, jwt);
        if (id != null) {
            return id;
        }
        // 无 id 返回：可能令牌过期/被撤销 → 刷新后重试一次
        log.warn("routemobile send returned no id, refresh token and retry once, msgId:{}", ctx.getMsgId());
        tokenManager.invalidate(key);
        jwt = tokenManager.getToken(key, baseUrl, ctx.getAuthUser(), ctx.getAuthPass());
        if (jwt == null) {
            return null;
        }
        return ThirdPartyLoginHelper.sendRouteMobileRCSMsg(payload, baseUrl, jwt);
    }

    @Override
    public RcsCallbackResult parseCallback(JSONObject raw) {
        return RouteMobileCallbackParser.parse(raw);
    }

    private static String stripPlus(String e164) {
        if (e164 != null && e164.startsWith("+")) {
            return e164.substring(1);
        }
        return e164;
    }
}

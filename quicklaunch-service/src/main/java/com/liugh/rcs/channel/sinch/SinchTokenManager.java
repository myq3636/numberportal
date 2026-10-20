package com.liugh.rcs.channel.sinch;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.liugh.service.util.ThirdPartyLoginHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * Sinch OAuth2 令牌管理：client_credentials 换 access_token → 进程内缓存 + 提前刷新 + 401 失效。
 * 单实例够用；多实例部署时可把缓存挪到 Redis（与 RouteMobileTokenManager 同结论）。
 *
 * <p>与 RM 的差异：认证端点是全局固定的 auth.sinch.com（不随 admin_info.url 的区域变化），
 * 且有效期由服务端回传的 expires_in 决定，而非硬编码。</p>
 *
 * <p>连接凭证由调用方（适配器）从供应商 admin_info 取出后传入：
 * keyId=admin.appId、keySecret=admin.appSecurity。</p>
 */
@Component
@Slf4j
public class SinchTokenManager {

    /** 安全垫：按服务端 expires_in 提前 5 分钟失效，避免边界上用到刚过期的令牌。 */
    private static final long SKEW_SEC = 300L;
    /** expires_in 缺失时的兜底有效期（55 分钟，对齐 Sinch 文档"令牌可用一小时"）。 */
    private static final long FALLBACK_TTL_SEC = 3300L;

    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<String, Cached>();

    /**
     * 取令牌：命中缓存直接返回，否则重新认证。失败返回 null。
     *
     * @param cacheKey 缓存键（用 channelCode 或 rssid）
     */
    public String getToken(String cacheKey, String keyId, String keySecret) {
        long now = System.currentTimeMillis();
        Cached c = cache.get(cacheKey);
        if (c != null && now < c.expireAt) {
            return c.token;
        }
        if (keyId == null || keySecret == null) {
            log.error("sinch missing credentials for {}", cacheKey);
            return null;
        }
        synchronized (this) {
            c = cache.get(cacheKey);
            if (c != null && System.currentTimeMillis() < c.expireAt) {
                return c.token;
            }
            JSONObject json = ThirdPartyLoginHelper.loginSinch(keyId, keySecret);
            if (json == null) {
                log.error("sinch oauth failed, key:{}", cacheKey);
                return null;
            }
            String token = json.getString("access_token");
            if (token == null) {
                log.error("sinch oauth response has no access_token, key:{}", cacheKey);
                return null;
            }
            cache.put(cacheKey, new Cached(token, System.currentTimeMillis() + ttlMs(json)));
            return token;
        }
    }

    /**
     * 缓存时长 = 服务端 expires_in - 安全垫。
     * expires_in 缺失 → 兜底 55min；短于安全垫（异常配置）→ 取其一半，避免把短命令牌缓存成长命的。
     */
    private static long ttlMs(JSONObject json) {
        Long expiresIn = null;
        try {
            expiresIn = json.getLong("expires_in");
        } catch (Exception e) {
            log.warn("sinch oauth expires_in unparsable, fall back to {}s", FALLBACK_TTL_SEC);
        }
        long sec;
        if (expiresIn == null || expiresIn <= 0) {
            sec = FALLBACK_TTL_SEC;
        } else if (expiresIn <= SKEW_SEC) {
            sec = expiresIn / 2;
        } else {
            sec = expiresIn - SKEW_SEC;
        }
        return sec * 1000L;
    }

    /** 401/提交被拒时强制失效，下次 getToken 重新认证。 */
    public void invalidate(String cacheKey) {
        cache.remove(cacheKey);
    }

    private static final class Cached {
        final String token;
        final long expireAt;

        Cached(String token, long expireAt) {
            this.token = token;
            this.expireAt = expireAt;
        }
    }
}

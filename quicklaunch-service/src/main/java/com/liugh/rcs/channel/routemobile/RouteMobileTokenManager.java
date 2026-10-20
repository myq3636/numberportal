package com.liugh.rcs.channel.routemobile;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.liugh.service.util.ThirdPartyLoginHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * Route Mobile JWT 令牌管理：login 换 JWT（1 小时有效）→ 进程内缓存 + 提前刷新 + 401 失效。
 * 单实例够用；多实例部署时可把缓存挪到 Redis。
 *
 * <p>连接凭证由调用方（适配器）从供应商 admin_info 取出后传入：baseUrl=admin.url、
 * user=admin.appId、pass=admin.appSecurity。</p>
 */
@Component
@Slf4j
public class RouteMobileTokenManager {

    /** 官方 1 小时，留 5 分钟安全垫 → 缓存视为有效 55 分钟。 */
    private static final long TTL_MS = 55L * 60 * 1000;

    private final ConcurrentHashMap<String, Cached> cache = new ConcurrentHashMap<String, Cached>();

    /**
     * 取令牌：命中缓存直接返回，否则登录换新。失败返回 null。
     *
     * @param cacheKey 缓存键（用 channelCode 或 rssid）
     */
    public String getToken(String cacheKey, String baseUrl, String user, String pass) {
        long now = System.currentTimeMillis();
        Cached c = cache.get(cacheKey);
        if (c != null && now < c.expireAt) {
            return c.token;
        }
        if (user == null || pass == null) {
            log.error("routemobile missing credentials for {}", cacheKey);
            return null;
        }
        synchronized (this) {
            c = cache.get(cacheKey);
            if (c != null && System.currentTimeMillis() < c.expireAt) {
                return c.token;
            }
            String token = ThirdPartyLoginHelper.loginRouteMobile(baseUrl, user, pass);
            if (token == null) {
                log.error("routemobile login failed, key:{}", cacheKey);
                return null;
            }
            cache.put(cacheKey, new Cached(token, System.currentTimeMillis() + TTL_MS));
            return token;
        }
    }

    /** 401 时强制失效，下次 getToken 重新登录。 */
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

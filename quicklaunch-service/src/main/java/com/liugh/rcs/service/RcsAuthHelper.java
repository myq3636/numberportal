package com.liugh.rcs.service;

import com.liugh.entity.AdminInfo;
import com.liugh.service.util.GlobalMap;

/**
 * RCS 北向鉴权：与原平台一致，客户传 Token，平台按 Token 解析归属客户（ssid）。
 * GlobalMap.admin 由 loadAdminToSystem() 同时以 id 和 token 为 key 装载。
 */
public final class RcsAuthHelper {

    private RcsAuthHelper() {
    }

    /** Token → 客户；无效/缺失返回 null。 */
    public static AdminInfo resolveCustomer(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        return GlobalMap.admin.get(token);
    }
}

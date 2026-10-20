package com.liugh.rcs.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.liugh.entity.AdminInfo;
import com.liugh.entity.RcsChannelAgent;
import com.liugh.mapper.RcsChannelAgentMapper;
import com.liugh.service.util.GlobalMap;

import lombok.extern.slf4j.Slf4j;

/**
 * RCS 通道路由（与原平台 SMS 一致的 rssid 路由模型）。
 *
 * <p>核心：客户(ossid) + 收件国家 → 路由表 routing_info 选出 rssid → rssid 即供应商
 * admin_info.id，该记录的 channel_code 决定走哪个通道、url/appId/appSecurity 是连接凭证。
 * 即"选中的 rssid 是谁就走对应通道"，且通道类型与连接凭证统一在 admin_info 一处管理。</p>
 *
 * 仅使用平台路由表；未配置路由时返回 null，不隐式选择其他供应商。
 */
@Service
@Slf4j
public class RcsRouter {

    @Autowired
    private RcsChannelAgentMapper rcsChannelAgentMapper;

    /** 仅按国家路由（common 维度）；兼容 agent 事件 / 能力查询等无 sender/content 上下文的调用。 */
    public AdminInfo route(AdminInfo customer, String country) {
        return route(customer, country, null, null);
    }

    /**
     * RCS 权重路由：sender &gt; content &gt; common，同组按 ratio 纯随机选取。
     *
     * @param customer         归属客户（ossid=customer.getId()）
     * @param country          收件国家码
     * @param sender           发送方（type=1 维度，可为空）
     * @param contentKeywordId 内容关键词 id（type=2 维度，可为空）
     */
    public AdminInfo route(AdminInfo customer, String country, String sender, String contentKeywordId) {
        Integer ossid = toInt(customer.getSsid());
        if (ossid == null) {
            return null;
        }
        RcsRoutingSnapshot snapshot = GlobalMap.rcsRoutingSnapshot;
        if (snapshot == null || snapshot.isEmpty()) {
            log.warn("rcs routing snapshot empty, ossid:{}", ossid);
            return null;
        }
        RcsRoutingContext ctx = new RcsRoutingContext(ossid, country, sender, contentKeywordId);
        Integer rssid = snapshot.select(ctx, ThreadLocalRandom.current());
        if (rssid == null || rssid == 0) {
            log.warn("rcs no weighted route ossid:{}, country:{}, sender:{}, keyword:{}",
                    ossid, country, sender, contentKeywordId);
            return null;
        }
        AdminInfo supplier = GlobalMap.rcsadmin.get(String.valueOf(rssid));
        if (!isRcsSupplier(supplier)) {
            log.warn("rcs routed rssid {} is not an RCS supplier (channel_code null?), ossid:{}", rssid, ossid);
            return null;
        }
        return supplier;
    }

    /** 解析 agent 在指定通道上的落地 sender（sender-first 未命中时用）。 */
    public RcsChannelAgent resolveAgentBinding(String agentId, String channelCode) {
        if (agentId == null) {
            return null;
        }
        return first(rcsChannelAgentMapper.selectList(new EntityWrapper<RcsChannelAgent>()
                .eq("agent_id", agentId)
                .eq("channel_code", channelCode)
                .eq("status", 1)));
    }

    /**
     * Resolve the exact supplier account permanently bound to a resource.
     * Resource operations must not re-run country routing after creation.
     */
    public AdminInfo resolveSupplier(RcsChannelAgent binding) {
        if (binding == null || binding.getRssid() == null) {
            return null;
        }
        AdminInfo supplier = GlobalMap.rcsadmin.get(String.valueOf(binding.getRssid()));
        return isRcsSupplier(supplier) ? supplier : null;
    }

    /** RCS 供应商：channel_code 非空且账号有效（status==0 正常）。 */
    private boolean isRcsSupplier(AdminInfo a) {
        return a != null && a.getChannelCode() != null && !a.getChannelCode().trim().isEmpty()
                && (a.getStatus() == null || a.getStatus() == 0);
    }

    private static Integer toInt(Object v) {
        if (v == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return null;
        }
    }

    private static <T> T first(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list.get(0);
    }

}

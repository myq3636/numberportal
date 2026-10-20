package com.liugh.rcs.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import com.liugh.entity.RoutingInfo;

/**
 * RCS 权重路由内存快照（不可变）。按 type 建三类索引：
 * <ul>
 *   <li>type=1 sender：ossid -> sender -> 候选</li>
 *   <li>type=2 content：ossid -> content_keyword_id -> 候选</li>
 *   <li>type=3 common：ossid -> country -> 候选</li>
 * </ul>
 *
 * <p>优先级 sender &gt; content &gt; common；同组内按 ratio 加权纯随机选取。
 * 该快照整体重建、原子替换，读路径无锁。</p>
 */
public final class RcsRoutingSnapshot {

    public static final int DEFAULT_RATIO = 100;

    private final Map<Integer, Map<String, List<RcsWeightedRoute>>> sender;
    private final Map<Integer, Map<String, List<RcsWeightedRoute>>> content;
    private final Map<Integer, Map<String, List<RcsWeightedRoute>>> common;

    private RcsRoutingSnapshot(
            Map<Integer, Map<String, List<RcsWeightedRoute>>> sender,
            Map<Integer, Map<String, List<RcsWeightedRoute>>> content,
            Map<Integer, Map<String, List<RcsWeightedRoute>>> common) {
        this.sender = sender;
        this.content = content;
        this.common = common;
    }

    public static RcsRoutingSnapshot empty() {
        return new RcsRoutingSnapshot(
                new HashMap<Integer, Map<String, List<RcsWeightedRoute>>>(),
                new HashMap<Integer, Map<String, List<RcsWeightedRoute>>>(),
                new HashMap<Integer, Map<String, List<RcsWeightedRoute>>>());
    }

    /**
     * 由 routing_info 构建快照。只保留 status==0 且 rssid 属于 RCS 供应商的行；
     * ratio<=0 视为未配置，按 {@link #DEFAULT_RATIO} 处理。
     */
    public static RcsRoutingSnapshot build(List<RoutingInfo> infos, Predicate<Integer> rcsSupplier) {
        Map<Integer, Map<String, List<RcsWeightedRoute>>> sender =
                new HashMap<Integer, Map<String, List<RcsWeightedRoute>>>();
        Map<Integer, Map<String, List<RcsWeightedRoute>>> content =
                new HashMap<Integer, Map<String, List<RcsWeightedRoute>>>();
        Map<Integer, Map<String, List<RcsWeightedRoute>>> common =
                new HashMap<Integer, Map<String, List<RcsWeightedRoute>>>();

        if (infos != null) {
            for (RoutingInfo info : infos) {
                if (info == null || info.getStatus() != 0) {
                    continue;
                }
                if (info.getOssid() == null || info.getRssid() == null) {
                    continue;
                }
                if (rcsSupplier != null && !rcsSupplier.test(info.getRssid())) {
                    continue;
                }
                int ratio = info.getRatio() > 0 ? info.getRatio() : DEFAULT_RATIO;
                RcsWeightedRoute route = new RcsWeightedRoute(info.getRssid(), ratio);
                Integer type = info.getType();
                if (type != null && type == 1) {
                    add(sender, info.getOssid(), trim(info.getSender()), route);
                } else if (type != null && type == 2) {
                    add(content, info.getOssid(), trim(info.getContentKeywordId()), route);
                } else {
                    add(common, info.getOssid(), normalizeCountry(info.getCountryCode()), route);
                }
            }
        }
        return new RcsRoutingSnapshot(sender, content, common);
    }

    /**
     * 按 sender &gt; content &gt; common 优先级返回命中的 rssid；未命中返回 null。
     */
    public Integer select(RcsRoutingContext ctx, Random random) {
        if (ctx == null) {
            return null;
        }
        Integer rssid = pick(sender, ctx.getOssid(), trim(ctx.getSender()), random);
        if (rssid != null) {
            return rssid;
        }
        rssid = pick(content, ctx.getOssid(), trim(ctx.getContentKeywordId()), random);
        if (rssid != null) {
            return rssid;
        }
        return pick(common, ctx.getOssid(), normalizeCountry(ctx.getCountry()), random);
    }

    public boolean isEmpty() {
        return sender.isEmpty() && content.isEmpty() && common.isEmpty();
    }

    private static Integer pick(
            Map<Integer, Map<String, List<RcsWeightedRoute>>> index, int ossid, String key, Random random) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        Map<String, List<RcsWeightedRoute>> byKey = index.get(ossid);
        if (byKey == null) {
            return null;
        }
        List<RcsWeightedRoute> candidates = byKey.get(key);
        return weightedPick(candidates, random);
    }

    private static Integer weightedPick(List<RcsWeightedRoute> candidates, Random random) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        int total = 0;
        for (RcsWeightedRoute r : candidates) {
            total += Math.max(0, r.getRatio());
        }
        if (total <= 0) {
            return null;
        }
        int pick = random.nextInt(total);
        int acc = 0;
        for (RcsWeightedRoute r : candidates) {
            acc += Math.max(0, r.getRatio());
            if (pick < acc) {
                return r.getRssid();
            }
        }
        return candidates.get(candidates.size() - 1).getRssid();
    }

    private static void add(
            Map<Integer, Map<String, List<RcsWeightedRoute>>> index, int ossid, String key, RcsWeightedRoute route) {
        if (key == null || key.isEmpty()) {
            return;
        }
        Map<String, List<RcsWeightedRoute>> byKey = index.get(ossid);
        if (byKey == null) {
            byKey = new HashMap<String, List<RcsWeightedRoute>>();
            index.put(ossid, byKey);
        }
        List<RcsWeightedRoute> list = byKey.get(key);
        if (list == null) {
            list = new ArrayList<RcsWeightedRoute>();
            byKey.put(key, list);
        }
        list.add(route);
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private static String normalizeCountry(String country) {
        String c = trim(country);
        return c == null ? null : c.toUpperCase();
    }
}

package com.liugh.rcs.service;

/**
 * 单个 RCS 路由候选：目标供应商 rssid + 权重 ratio。
 * 仅用于内存快照，不落库、不暴露给客户。
 */
public final class RcsWeightedRoute {

    private final int rssid;
    private final int ratio;

    public RcsWeightedRoute(int rssid, int ratio) {
        this.rssid = rssid;
        this.ratio = ratio;
    }

    public int getRssid() {
        return rssid;
    }

    public int getRatio() {
        return ratio;
    }
}

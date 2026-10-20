package com.liugh.rcs.service;

/**
 * RCS 路由查询上下文。各维度取值可为空；为空时对应维度不参与匹配。
 */
public final class RcsRoutingContext {

    private final int ossid;
    private final String country;
    private final String sender;
    private final String contentKeywordId;

    public RcsRoutingContext(int ossid, String country, String sender, String contentKeywordId) {
        this.ossid = ossid;
        this.country = country;
        this.sender = sender;
        this.contentKeywordId = contentKeywordId;
    }

    public int getOssid() {
        return ossid;
    }

    public String getCountry() {
        return country;
    }

    public String getSender() {
        return sender;
    }

    public String getContentKeywordId() {
        return contentKeywordId;
    }
}

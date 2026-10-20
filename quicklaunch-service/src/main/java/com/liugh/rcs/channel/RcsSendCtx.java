package com.liugh.rcs.channel;

/**
 * 发送上下文：编排层（RcsMessageService）装配好后交给通道适配器。
 * 适配器只负责造报文 + 发 HTTP，不碰库、不做鉴权/路由。
 *
 * <p>连接凭证统一来自供应商 admin_info（按 rssid 解析）：baseUrl=admin.url、
 * Infobip apiKey=admin.appId、RM 用户名/密码=admin.appId/admin.appSecurity。
 * 与原平台 SMS 一处管理。</p>
 */
public class RcsSendCtx {

    private int ssid;
    /** 发送方：Infobip=sender；Route Mobile=bot_name。已按 sender-first / agent 绑定解析完毕。 */
    private String sender;
    /** 收件人 E.164（带 +），适配器按各自 API 需要自行去 +。 */
    private String recipient;
    /** 我方 msgId：回填到通道的自定义回带字段（Infobip callbackData / RM extra）。 */
    private String msgId;
    /** 我方入站回调 URL（本通道专属）。 */
    private String notifyUrl;
    private Integer ttl;

    /** 通道编码（INFOBIP / ROUTEMOBILE），用于令牌缓存 key / 日志。 */
    private String channelCode;
    /** 路由 rssid（= 供应商 admin_info.id）。 */
    private Integer rssid;

    // ---- 连接凭证（来自供应商 admin_info）----
    private String baseUrl;
    /** Infobip apiKey（admin.appId）。 */
    private String apiKey;
    /** Route Mobile 登录用户名（admin.appId）。 */
    private String authUser;
    /** Route Mobile 登录密码（admin.appSecurity）。 */
    private String authPass;

    private String smsSender;
    private String fallbackText;
    /** Spark mask：RCS 实际发送方/掩码，区别于 smsSender（SMS fallback sender）。 */
    private String channelSender;
    /** 路由出的国家码，供 Spark 等通道做国家限制校验（如 MX）。 */
    private String countryCode;

    public int getSsid() {
        return ssid;
    }

    public void setSsid(int ssid) {
        this.ssid = ssid;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public Integer getRssid() {
        return rssid;
    }

    public void setRssid(Integer rssid) {
        this.rssid = rssid;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getAuthUser() {
        return authUser;
    }

    public void setAuthUser(String authUser) {
        this.authUser = authUser;
    }

    public String getAuthPass() {
        return authPass;
    }

    public void setAuthPass(String authPass) {
        this.authPass = authPass;
    }

    public String getSmsSender() {
        return smsSender;
    }

    public void setSmsSender(String smsSender) {
        this.smsSender = smsSender;
    }

    public String getFallbackText() {
        return fallbackText;
    }

    public void setFallbackText(String fallbackText) {
        this.fallbackText = fallbackText;
    }

    public String getChannelSender() {
        return channelSender;
    }

    public void setChannelSender(String channelSender) {
        this.channelSender = channelSender;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }
}

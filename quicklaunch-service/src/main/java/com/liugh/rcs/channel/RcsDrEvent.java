package com.liugh.rcs.channel;

/**
 * 归一化后的下行回执事件（各通道回调解析后统一成这个）。
 * 适配器只填字段，落库/计费/回推客户由 RcsCallbackService 统一处理。
 */
public class RcsDrEvent {

    /** 关联键：我方 msgId（Infobip callbackData / RM extra），优先用它匹配 CDR。 */
    private String ourMsgId;
    /** 回退关联键：通道 messageId。 */
    private String channelMsgId;
    /**
     * 归一化状态：3 已送达 / 4 送达失败 / 5 过期 / 9 已读 / 12 已降级。
     * null 表示中间态（PENDING/ACCEPTED）——仅回填价格，不推进状态、不回推客户。
     */
    private Integer status;
    private String errorCode;
    private String errorReason;
    /** 通道回传单价。 */
    private String cost;
    private String currency;
    /** RM 降级后的 whatsapp/sms 消息 id（status=12 时）。 */
    private String fallbackMsgId;

    public String getOurMsgId() {
        return ourMsgId;
    }

    public void setOurMsgId(String ourMsgId) {
        this.ourMsgId = ourMsgId;
    }

    public String getChannelMsgId() {
        return channelMsgId;
    }

    public void setChannelMsgId(String channelMsgId) {
        this.channelMsgId = channelMsgId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getFallbackMsgId() {
        return fallbackMsgId;
    }

    public void setFallbackMsgId(String fallbackMsgId) {
        this.fallbackMsgId = fallbackMsgId;
    }
}

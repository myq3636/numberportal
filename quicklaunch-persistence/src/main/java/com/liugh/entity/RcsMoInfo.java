package com.liugh.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;

/**
 * <p>
 * RCS 上行/事件 CDR：USER_MESSAGE / SUGGESTION_RESPONSE / DELIVERED / READ /
 * SUBSCRIBE / UNSUBSCRIBE 等由通道回调归一化后落库，再推送给客户。
 * </p>
 *
 * @author rcs
 */
@TableName("rcs_mo_info")
public class RcsMoInfo extends Model<RcsMoInfo> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer ssid;

    @TableField("channel_code")
    private String channelCode;

    @TableField("agent_id")
    private String agentId;

    private String msisdn;

    /** USER_MESSAGE / SUGGESTION_RESPONSE / DELIVERED / READ / IS_TYPING / SUBSCRIBE / UNSUBSCRIBE */
    @TableField("event_type")
    private String eventType;

    @TableField("client_msg_id")
    private String clientMsgId;

    @TableField("related_msg_id")
    private String relatedMsgId;

    /** TEXT / FILE / LOCATION / SUGGESTION */
    @TableField("content_type")
    private String contentType;

    private String text;

    @TableField("file_uri")
    private String fileUri;

    @TableField("mime_type")
    private String mimeType;

    @TableField("thumbnail_uri")
    private String thumbnailUri;

    private Double latitude;

    private Double longitude;

    @TableField("postback_data")
    private String postbackData;

    @TableField("suggestion_text")
    private String suggestionText;

    @TableField("suggestion_type")
    private String suggestionType;

    @TableField("receive_time")
    private long receiveTime;

    /** 推给客户的状态 0待推 1成功 2失败 */
    @TableField("push_status")
    private Integer pushStatus;

    @TableField("push_retry")
    private Integer pushRetry;

    @TableField("create_time")
    private long createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSsid() {
        return ssid;
    }

    public void setSsid(Integer ssid) {
        this.ssid = ssid;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getClientMsgId() {
        return clientMsgId;
    }

    public void setClientMsgId(String clientMsgId) {
        this.clientMsgId = clientMsgId;
    }

    public String getRelatedMsgId() {
        return relatedMsgId;
    }

    public void setRelatedMsgId(String relatedMsgId) {
        this.relatedMsgId = relatedMsgId;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(String fileUri) {
        this.fileUri = fileUri;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getThumbnailUri() {
        return thumbnailUri;
    }

    public void setThumbnailUri(String thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getPostbackData() {
        return postbackData;
    }

    public void setPostbackData(String postbackData) {
        this.postbackData = postbackData;
    }

    public String getSuggestionText() {
        return suggestionText;
    }

    public void setSuggestionText(String suggestionText) {
        this.suggestionText = suggestionText;
    }

    public String getSuggestionType() {
        return suggestionType;
    }

    public void setSuggestionType(String suggestionType) {
        this.suggestionType = suggestionType;
    }

    public long getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(long receiveTime) {
        this.receiveTime = receiveTime;
    }

    public Integer getPushStatus() {
        return pushStatus;
    }

    public void setPushStatus(Integer pushStatus) {
        this.pushStatus = pushStatus;
    }

    public Integer getPushRetry() {
        return pushRetry;
    }

    public void setPushRetry(Integer pushRetry) {
        this.pushRetry = pushRetry;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    @Override
    protected Serializable pkVal() {
        return this.id;
    }
}

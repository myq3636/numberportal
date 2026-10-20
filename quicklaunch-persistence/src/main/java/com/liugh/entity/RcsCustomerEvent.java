package com.liugh.entity;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("rcs_customer_event")
public class RcsCustomerEvent extends Model<RcsCustomerEvent> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("event_id")
    private String eventId;
    private Integer ssid;
    @TableField("event_type")
    private String eventType;
    @TableField("occurred_at")
    private Date occurredAt;
    @TableField("sender_id")
    private String senderId;
    @TableField("message_id")
    private String messageId;
    @TableField("channel_code")
    private String channelCode;
    @TableField("provider_event_key")
    private String providerEventKey;
    @TableField("payload_json")
    private String payloadJson;
    private Boolean deliverable;
    @TableField("delivery_status")
    private String deliveryStatus;
    @TableField("attempt_count")
    private Integer attemptCount;
    @TableField("next_attempt_time")
    private Date nextAttemptTime;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;

    @Override
    protected Serializable pkVal() {
        return id;
    }
}

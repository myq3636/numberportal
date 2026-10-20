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
@TableName("rcs_avant_callback_receipt")
public class RcsAvantCallbackReceipt extends Model<RcsAvantCallbackReceipt> {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer rssid;
    @TableField("dedupe_hash")
    private String dedupeHash;
    @TableField("provider_message_id")
    private String providerMessageId;
    @TableField("message_key")
    private String messageKey;
    @TableField("item_key")
    private String itemKey;
    private String recipient;
    @TableField("provider_status")
    private String providerStatus;
    @TableField("occurred_at")
    private Date occurredAt;
    @TableField("raw_json")
    private String rawJson;
    @TableField("process_status")
    private String processStatus;
    @TableField("attempt_count")
    private Integer attemptCount;
    @TableField("last_error")
    private String lastError;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;

    @Override
    protected Serializable pkVal() {
        return id;
    }
}

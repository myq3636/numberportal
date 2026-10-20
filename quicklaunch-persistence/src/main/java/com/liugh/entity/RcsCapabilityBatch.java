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
@TableName("rcs_capability_batch")
public class RcsCapabilityBatch extends Model<RcsCapabilityBatch> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("batch_id")
    private String batchId;
    private Integer ssid;
    @TableField("sender_id")
    private String senderId;
    @TableField("channel_code")
    private String channelCode;
    @TableField("upstream_bulk_id")
    private String upstreamBulkId;
    private String status;
    @TableField("total_count")
    private Integer totalCount;
    @TableField("success_count")
    private Integer successCount;
    @TableField("failure_count")
    private Integer failureCount;
    @TableField("requested_at")
    private Date requestedAt;
    @TableField("completed_at")
    private Date completedAt;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;

    @Override
    protected Serializable pkVal() {
        return id;
    }
}

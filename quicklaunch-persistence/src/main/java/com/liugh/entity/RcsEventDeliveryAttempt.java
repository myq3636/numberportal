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
@TableName("rcs_event_delivery_attempt")
public class RcsEventDeliveryAttempt extends Model<RcsEventDeliveryAttempt> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer ssid;
    @TableField("event_id")
    private String eventId;
    @TableField("attempt_no")
    private Integer attemptNo;
    @TableField("request_id")
    private String requestId;
    @TableField("started_at")
    private Date startedAt;
    @TableField("completed_at")
    private Date completedAt;
    @TableField("http_status")
    private Integer httpStatus;
    private String result;
    @TableField("error_summary")
    private String errorSummary;
    @TableField("create_time")
    private Date createTime;

    @Override
    protected Serializable pkVal() {
        return id;
    }
}

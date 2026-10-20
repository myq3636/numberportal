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
@TableName("rcs_capability_result")
public class RcsCapabilityResult extends Model<RcsCapabilityResult> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer ssid;
    @TableField("batch_id")
    private String batchId;
    @TableField("sender_id")
    private String senderId;
    private String recipient;
    private String status;
    @TableField("capabilities_json")
    private String capabilitiesJson;
    @TableField("error_code")
    private String errorCode;
    @TableField("error_summary")
    private String errorSummary;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;

    @Override
    protected Serializable pkVal() {
        return id;
    }
}

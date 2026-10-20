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
@TableName("rcs_delivery_config")
public class RcsDeliveryConfig extends Model<RcsDeliveryConfig> {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private Integer ssid;
    @TableField("event_url")
    private String eventUrl;
    @TableField("signature_secret_ciphertext")
    private byte[] signatureSecretCiphertext;
    @TableField("legacy_dr_enabled")
    private Boolean legacyDrEnabled;
    @TableField("max_retries")
    private Integer maxRetries;
    private Boolean enabled;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;

    @Override
    protected Serializable pkVal() {
        return id;
    }
}

package com.liugh.api.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.enums.IdType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import java.io.Serializable;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author liugh123
 * @since 2018-12-26
 */
@TableName("user_info")
public class LoginRequest extends Model<LoginRequest> {

    private static final long serialVersionUID = 1L;

    /**
     * 用户主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * 登录名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * mac地址支持多个，已；分割
     */
    @TableField("mac_info")
    private String macInfo;
    /**
     * 创建时间
     */
    @TableField("effective_start_time")
    private Long effectiveStartTime;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMacInfo() {
        return macInfo;
    }

    
    public void setMacInfo(String macInfo) {
        this.macInfo = macInfo;
    }

    public Long getEffectiveStartTime() {
        return effectiveStartTime;
    }

    public void setEffectiveStartTime(Long effectiveStartTime) {
        this.effectiveStartTime = effectiveStartTime;
    }

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

    @Override
    public String toString() {
        return "UserInfo{" +
        "id=" + id +
        ", username=" + username +
        ", password=" + password +
        ", macInfo=" + macInfo +
        ", effectiveStartTime=" + effectiveStartTime +
        "}";
    }
}

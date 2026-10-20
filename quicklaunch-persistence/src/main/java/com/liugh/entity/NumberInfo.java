package com.liugh.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.enums.IdType;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author liugh123
 * @since 2019-01-04
 */
@TableName("number_info")
public class NumberInfo extends Model<NumberInfo> {

    private static final long serialVersionUID = 1L;

    /**
     * 用户主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("customer_id")
    private int customerId;
    private String number;
    @TableField("expire_time")
    private String expireTime;
    @TableField("create_time")
    private long createTime;
    @TableField("expire_timemark")
    private long expireTimemark;
    
    /**
     * 0:已生效，1:已过期
     */
    private Integer status;

	@Override
	protected Serializable pkVal() {		
		return id;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}	

	public int getCustomerId() {
		return customerId;
	}

	public void setCustomerId(int customerId) {
		this.customerId = customerId;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

	public String getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(String expireTime) {
		this.expireTime = expireTime;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public long getExpireTimemark() {
		return expireTimemark;
	}

	public void setExpireTimemark(long expireTimemark) {
		this.expireTimemark = expireTimemark;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "NumberInfo [id=" + id + ", customerId=" + customerId + ", number=" + number + ", expireTime="
				+ expireTime + ", createTime=" + createTime + ", expireTimemark=" + expireTimemark + ", status="
				+ status + "]";
	}
    
	

}

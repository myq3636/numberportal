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
@TableName("admin_info")
public class AdminInfo extends Model<AdminInfo> {

    private static final long serialVersionUID = 1L;

    /**
     * 用户主键
     */
    @TableId(value = "id", type = IdType.AUTO)    
    private Long id;
    /**
     * 登录名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 昵称
     */
    private String nickname;
    /**
     * 头像图片压缩编码
     */
    private String imgBase64;
    /**
     * 个人描述
     */
    private String descrption;
    /**
     * 手机号
     */
    @TableField("phone_number")
    private String phoneNumber;
    /**
     * 0:正常，1:过期
     */
    private Integer status;
    /**
     * 0:普通管理员 , 1:超级管理员
     */
    private Integer role;
    
    private Integer ssid;
    @TableField("customer_name")
    private String customerName;
    
    private String credit;
    @TableField("remain_count")
    private Long remainCount;
    /**
     * 0=portal, 1=gateway,2=dc,3=waclient,4=tts
     */
    private int source;
    
    private String url;
    private String token;
    
    @TableField("app_id")
    private String appId;
    @TableField("app_security")
    private String appSecurity;
    @TableField("number_check_url")
    private String numberCheckUrl;

    /** RCS 聚合商类型：INFOBIP / ROUTEMOBILE / SINCH。仅 RCS 供应商记录设置，据此选适配器。 */
    @TableField("channel_code")
    private String channelCode;
    /** RCS smsFailover 发送方（Infobip 降级发 SMS 时的 sender）。 */
    @TableField("sms_sender")
    private String smsSender;
    /** RCS 供应商实际发送方或 Spark mask。 */
    @TableField("channel_sender")
    private String channelSender;

    private String ports;
    //单个消息的发送间隔单位ms
    private long interval;
    @TableField("expire_time")
    private long expireTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getImgBase64() {
        return imgBase64;
    }

    public void setImgBase64(String imgBase64) {
        this.imgBase64 = imgBase64;
    }

    public String getDescrption() {
        return descrption;
    }

    public void setDescrption(String descrption) {
        this.descrption = descrption;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }   
    
    public Integer getSsid() {
		return ssid;
	}

	public void setSsid(Integer ssid) {
		this.ssid = ssid;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	
	
	public String getCredit() {
		return credit;
	}

	public void setCredit(String credit) {
		
		this.credit = credit;
				
	}

	public void deductCredit(float prepaid) {
		
	}
	
	
	public Long getRemainCount() {
		return remainCount;
	}

	public void setRemainCount(Long remainCount) {
		this.remainCount = remainCount;
	}

	@Override
    protected Serializable pkVal() {
        return this.id;
    }
	

    public int getSource() {
		return source;
	}

	public void setSource(int source) {
		this.source = source;
	}	

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}	

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getAppSecurity() {
		return appSecurity;
	}

	public void setAppSecurity(String appSecurity) {
		this.appSecurity = appSecurity;
	}

	public String getNumberCheckUrl() {
		return numberCheckUrl;
	}

	public void setNumberCheckUrl(String numberCheckUrl) {
		this.numberCheckUrl = numberCheckUrl;
	}

	public String getChannelCode() {
		return channelCode;
	}

	public void setChannelCode(String channelCode) {
		this.channelCode = channelCode;
	}

	public String getSmsSender() {
		return smsSender;
	}

	public void setSmsSender(String smsSender) {
		this.smsSender = smsSender;
	}

	public String getChannelSender() {
		return channelSender;
	}

	public void setChannelSender(String channelSender) {
		this.channelSender = channelSender;
	}
	

	public String getPorts() {
		return ports;
	}

	public void setPorts(String ports) {
		this.ports = ports;
	}	

	public long getInterval() {
		return interval;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}
	
	

	public long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	@Override
	public String toString() {
		return "AdminInfo [id=" + id + ", username=" + username + ", password="
				+ password + ", nickname=" + nickname + ", imgBase64="
				+ imgBase64 + ", descrption=" + descrption + ", phoneNumber="
				+ phoneNumber + ", status=" + status + ", role=" + role
				+ ", ssid=" + ssid + ", customerName=" + customerName
				+ ", credit=" + credit + ", remainCount=" + remainCount
				+ ", source=" + source + ", url=" + url + ", token=" + token
				+ ", appId=" + appId + ", appSecurity=" + appSecurity
				+ ", numberCheckUrl=" + numberCheckUrl + ", ports=" + ports
				+ ", interval=" + interval + "]";
	}

	
	

	
}

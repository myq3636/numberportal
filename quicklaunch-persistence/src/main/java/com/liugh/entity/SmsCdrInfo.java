package com.liugh.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 
 * </p>
 *
 * @author liugh123
 * @since 2019-06-24
 */
@TableName("sms_cdr_info")
public class SmsCdrInfo extends Model<SmsCdrInfo> {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    private Integer ssid;
    
    /**
     * 0=发送成功，1=失败, 2=忽略，3=dr成功，4=dr失败，
     * 5=没有dr，等待dr时间失效,6=过期job异常设置的状态,7=[B2C_BLOCK] Blocked Yulore response=4
     * 8=sent
     */
    private Integer status;
    private String content;
    private String msisdn;
    @TableField("msg_id")
    private String msgId;
    private String sender;
    private String province;
    private String city;
    @TableField("mobile_type")
    private String mobileType;
    @TableField("receive_time")
    private String receiveTime;
    @TableField("create_time")
    private long createTime;
    @TableField("retry_count")
    private int retryCount;
    private int rssid;
    
    @TableField("expire_time")
    private long expireTime;
    
    private String timemark;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSsid() {
        return ssid;
    }

    public void setSsid(Integer ssid) {
        this.ssid = ssid;
    }


    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMsisdn() {
        return msisdn;
    }

    public void setMsisdn(String msisdn) {
        this.msisdn = msisdn;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
    
    

    public String getMobileType() {
		return mobileType;
	}

	public void setMobileType(String mobileType) {
		this.mobileType = mobileType;
	}

	@Override
    protected Serializable pkVal() {
        return this.id;
    }	

    public String getSender() {
		return sender;
	}

	public void setSender(String sender) {
		this.sender = sender;
	}

	public String getTimemark() {
		return timemark;
	}

	public void setTimemark(String timemark) {
		this.timemark = timemark;
	}

    public String getReceiveTime() {
        return receiveTime;
    }

    public void setReceiveTime(String receiveTime) {
        this.receiveTime = receiveTime;
    }
    
    

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}
	

	public int getRetryCount() {
		return retryCount;
	}

	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public int getRssid() {
		return rssid;
	}

	public void setRssid(int rssid) {
		this.rssid = rssid;
	}		

	public long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	@Override
	public String toString() {
		return "SmsCdrInfo [id=" + id + ", ssid=" + ssid + ", status=" + status
				+ ", content=" + content + ", msisdn=" + msisdn + ", msgId="
				+ msgId + ", sender=" + sender + ", province=" + province
				+ ", city=" + city + ", mobileType=" + mobileType
				+ ", receiveTime=" + receiveTime + ", createTime=" + createTime
				+ ", retryCount=" + retryCount + ", rssid=" + rssid
				+ ", timemark=" + timemark + "]";
	}
	
    
}

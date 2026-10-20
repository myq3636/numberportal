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
@TableName("rcs_cdr_info")
public class RCSCdrInfo extends Model<RCSCdrInfo> {

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
    @TableField("msg_type")
    private String msgType;
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
    private String thumbnail;
    private String suggestions;
    private String cost;

    // ---- P1 Infobip 集成扩展列（计费 / 通道对接），additive ----
    @TableField("channel_code")
    private String channelCode;          // INFOBIP / ROUTE_MOBILE / SINCH ...
    @TableField("channel_msg_id")
    private String channelMsgId;         // 通道返回的 messageId，DR 关联
    @TableField("country_code")
    private String countryCode;
    @TableField("traffic_type")
    private String trafficType;          // AUTHENTICATION/TRANSACTION/PROMOTION...
    @TableField("billing_category")
    private String billingCategory;      // CONVERSATIONAL / NON_CONVERSATIONAL
    private String currency;             // 通道回传币种
    @TableField("delivered_time")
    private Long deliveredTime;
    @TableField("read_time")
    private Long readTime;
    @TableField("error_code")
    private String errorCode;
    @TableField("error_reason")
    private String errorReason;
    @TableField("fallback_msg_id")
    private String fallbackMsgId;        // Route Mobile 降级后的 whatsapp/sms 消息 id
    
    private long insubmittime;
    private long outsubmittime;
    private long outdrtime;
    private long indrtime;

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

	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}		

	public String getThumbnail() {
		return thumbnail;
	}

	public void setThumbnail(String thumbnail) {
		this.thumbnail = thumbnail;
	}

	public String getSuggestions() {
		return suggestions;
	}

	public void setSuggestions(String suggestions) {
		this.suggestions = suggestions;
	}

	public String getCost() {
		return cost;
	}

	public void setCost(String cost) {
		this.cost = cost;
	}

	public String getChannelCode() {
		return channelCode;
	}

	public void setChannelCode(String channelCode) {
		this.channelCode = channelCode;
	}

	public String getChannelMsgId() {
		return channelMsgId;
	}

	public void setChannelMsgId(String channelMsgId) {
		this.channelMsgId = channelMsgId;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getTrafficType() {
		return trafficType;
	}

	public void setTrafficType(String trafficType) {
		this.trafficType = trafficType;
	}

	public String getBillingCategory() {
		return billingCategory;
	}

	public void setBillingCategory(String billingCategory) {
		this.billingCategory = billingCategory;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public Long getDeliveredTime() {
		return deliveredTime;
	}

	public void setDeliveredTime(Long deliveredTime) {
		this.deliveredTime = deliveredTime;
	}

	public Long getReadTime() {
		return readTime;
	}

	public void setReadTime(Long readTime) {
		this.readTime = readTime;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
	}

	public String getErrorReason() {
		return errorReason;
	}

	public void setErrorReason(String errorReason) {
		this.errorReason = errorReason;
	}

	public String getFallbackMsgId() {
		return fallbackMsgId;
	}

	public void setFallbackMsgId(String fallbackMsgId) {
		this.fallbackMsgId = fallbackMsgId;
	}
	
	

	public long getInsubmittime() {
		return insubmittime;
	}

	public void setInsubmittime(long insubmittime) {
		this.insubmittime = insubmittime;
	}

	public long getOutsubmittime() {
		return outsubmittime;
	}

	public void setOutsubmittime(long outsubmittime) {
		this.outsubmittime = outsubmittime;
	}

	public long getOutdrtime() {
		return outdrtime;
	}

	public void setOutdrtime(long outdrtime) {
		this.outdrtime = outdrtime;
	}

	public long getIndrtime() {
		return indrtime;
	}

	public void setIndrtime(long indrtime) {
		this.indrtime = indrtime;
	}

	@Override
	public String toString() {
		return "SmsCdrInfo [id=" + id + ", ssid=" + ssid + ", status=" + status
				+ ", content=" + content + ", msisdn=" + msisdn + ", msgId="
				+ msgId + ", sender=" + sender + ", province=" + province
				+ ", city=" + city + ", msgType=" + msgType
				+ ", receiveTime=" + receiveTime + ", createTime=" + createTime
				+ ", retryCount=" + retryCount + ", rssid=" + rssid
				+ ", timemark=" + timemark + "]";
	}
	
    
}

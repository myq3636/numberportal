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
@TableName("billing_cdr_info")
public class BillingCdrInfo extends Model<BillingCdrInfo> {

    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    
    private Integer ssid;
    
    /**
     * 0=发送成功，1=失败, 2=忽略，3=dr成功，4=dr失败，
     * 5=没有dr，等待dr时间失效,6=过期job异常设置的状态,7=[B2C_BLOCK] Blocked Yulore response=4
     * 
     */
    private Integer ostatus;
    /**
     * -1=未发送，0=发送成功，1=失败, 2=忽略，3=dr成功，4=dr失败，
     * 5=没有dr，等待dr时间失效,6=过期job异常设置的状态,7=[B2C_BLOCK] Blocked Yulore response=4
     * 8= temp success
     */
    private Integer rstatus=-1;
    private String content;
    private String msisdn;
    @TableField("msg_id")
    private String msgId;
    private String sender;
    private String province;
    private String city;
    @TableField("mobile_type")
    private String mobileType;
    @TableField("receive_Time")
    private String receiveTime;
    @TableField("create_time")
    private long createTime;
    @TableField("retry_count")
    private int retryCount;
    private int rssid;
    
    private String timemark;
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

    public Integer getOstatus() {
		return ostatus;
	}

	public void setOstatus(Integer ostatus) {
		this.ostatus = ostatus;
	}

	public Integer getRstatus() {
		return rstatus;
	}

	public void setRstatus(Integer rstatus) {
		this.rstatus = rstatus;
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
	
	public static BillingCdrInfo convertBillingCDR(SmsCdrInfo smscdr){
		
		BillingCdrInfo billing = new BillingCdrInfo();
		billing.setCity(smscdr.getCity());
		billing.setContent(smscdr.getContent());
		billing.setCreateTime(smscdr.getCreateTime());
		billing.setMobileType(smscdr.getMobileType());
		billing.setMsgId(smscdr.getMsgId());
		billing.setMsisdn(smscdr.getMsisdn());
		billing.setOstatus(smscdr.getStatus());
		billing.setProvince(smscdr.getProvince());
		billing.setReceiveTime(smscdr.getReceiveTime());
		billing.setRetryCount(smscdr.getRetryCount());
		billing.setRssid(smscdr.getRssid());
		billing.setSender(smscdr.getSender());
		billing.setSsid(smscdr.getSsid());
		billing.setTimemark(smscdr.getTimemark());
		return billing;
		
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
		return "BillingCdrInfo [id=" + id + ", ssid=" + ssid + ", ostatus="
				+ ostatus + ", rstatus=" + rstatus + ", content=" + content
				+ ", msisdn=" + msisdn + ", msgId=" + msgId + ", sender="
				+ sender + ", province=" + province + ", city=" + city
				+ ", mobileType=" + mobileType + ", receiveTime=" + receiveTime
				+ ", createTime=" + createTime + ", retryCount=" + retryCount
				+ ", rssid=" + rssid + ", timemark=" + timemark + "]";
	}

	
	
    
}

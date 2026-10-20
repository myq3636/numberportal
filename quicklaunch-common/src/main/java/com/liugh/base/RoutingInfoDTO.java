package com.liugh.base;

import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author liugh123
 * @since 2020-04-10
 */

public class RoutingInfoDTO {

    private static final long serialVersionUID = 1L;

    private Integer id;
    /**
     * 3:common, 1:sender,2:content
     */
    private Integer type;
    private Integer ossid;
    private Integer rssid;
    /**
     * 国家码
     */
    
    private String countryCode;
    /**
     * sender 路由时不能为空，其他路由时无效
     */
    private String sender;
    /**
     * content路由时不能为空，其他路由时无效
     */
    
    private String contentKeywordId;
    /**
     * 模板替换id
     */
    
    private String contentTemplateId;
    private String customerunit;
    private String dcunit;
    
    private String countryName;
    private String customerName;
    private String vendor;
    //0=生效，1=待审批
    private int effect;

    private int ratio;
    

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getOssid() {
        return ossid;
    }

    public void setOssid(Integer ossid) {
        this.ossid = ossid;
    }

    public Integer getRssid() {
        return rssid;
    }

    public void setRssid(Integer rssid) {
        this.rssid = rssid;
    }

    

    public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContentKeywordId() {
        return contentKeywordId;
    }

    public void setContentKeywordId(String contentKeywordId) {
        this.contentKeywordId = contentKeywordId;
    }

    public String getContentTemplateId() {
        return contentTemplateId;
    }

    public void setContentTemplateId(String contentTemplateId) {
        this.contentTemplateId = contentTemplateId;
    }
    
    

    public int getRatio() {
		return ratio;
	}

	public void setRatio(int ratio) {
		this.ratio = ratio;
	}

	public String getCountryName() {
		return countryName;
	}

	public void setCountryName(String countryName) {
		this.countryName = countryName;
	}

	public String getCustomerName() {
		return customerName;
	}

	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}
	
	

	public String getCustomerunit() {
		return customerunit;
	}

	public void setCustomerunit(String customerunit) {
		this.customerunit = customerunit;
	}

	public String getDcunit() {
		return dcunit;
	}

	public void setDcunit(String dcunit) {
		this.dcunit = dcunit;
	}

	public int getEffect() {
		return effect;
	}

	public void setEffect(int effect) {
		this.effect = effect;
	}

	@Override
    public String toString() {
        return "RoutingInfo{" +
        "id=" + id +
        ", type=" + type +
        ", ossid=" + ossid +
        ", rssid=" + rssid +
        ", countryCode=" + countryCode +
        ", sender=" + sender +
        ", contentKeywordId=" + contentKeywordId +
        ", contentTemplateId=" + contentTemplateId +
        "}";
    }
}

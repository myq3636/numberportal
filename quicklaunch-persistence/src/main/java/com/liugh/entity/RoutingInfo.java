package com.liugh.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotations.TableField;
import com.baomidou.mybatisplus.annotations.TableId;
import com.baomidou.mybatisplus.activerecord.Model;
import com.baomidou.mybatisplus.annotations.TableName;
import com.baomidou.mybatisplus.enums.IdType;

import java.io.Serializable;
import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * 
 * </p>
 *
 * @author liugh123
 * @since 2020-04-10
 */
@TableName("routing_info")
public class RoutingInfo extends Model<RoutingInfo> implements Comparable<RoutingInfo>{

    private static final long serialVersionUID = 1L;

    @TableId(value="id",type=IdType.AUTO)    
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
    @TableField("country_code")
    private String countryCode;
    /**
     * sender 路由时不能为空，其他路由时无效
     */
    private String sender;
    /**
     * content路由时不能为空，其他路由时无效
     */
    @TableField("content_keyword_id")
    private String contentKeywordId;
    /**
     * 模板替换id
     */
    @TableField("content_template_id")
    private String contentTemplateId;
    
    /**
     * 混合路由比例
     */
    private int ratio;
    
    /**
     * 0:已生效, 1:待修改,2:待增加，3:待删除
     */
    private int status;


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

    public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public int getRatio() {
		return ratio;
	}

	public void setRatio(int ratio) {
		this.ratio = ratio;
	}

	@Override
    protected Serializable pkVal() {
        return this.id;
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

	@Override
	public int compareTo(RoutingInfo o2) {
		
		if (this!=null && o2!=null && this.getCountryCode()!=null) {
			int i = this.getCountryCode().compareToIgnoreCase(o2.getCountryCode());
			if (i==0) {
				i = this.getRssid().compareTo(o2.getRssid());
				if (i==0) {
					return this.getType().compareTo(o2.getType());
				}
			}
			return i;
		}
		return -1;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (this==obj) {
			return true;
		}
		RoutingInfo other = (RoutingInfo)obj;
		
		if ((StringUtils.isBlank(contentKeywordId) 
				&& StringUtils.isNotBlank(other.getContentKeywordId()))
				||(StringUtils.isNotBlank(contentKeywordId) 
						&& StringUtils.isBlank(other.getContentKeywordId()))
				||(StringUtils.isNotBlank(contentKeywordId) 
						&& StringUtils.isNotBlank(other.getContentKeywordId())
						&& !contentKeywordId.equals(other.getContentKeywordId()))) {
			return false;
		}
		
		/*if ((StringUtils.isBlank(contentTemplateId) 
				&& StringUtils.isNotBlank(other.getContentTemplateId()))
				||(StringUtils.isNotBlank(contentTemplateId) 
						&& StringUtils.isBlank(other.getContentTemplateId()))
				||(StringUtils.isNotBlank(contentTemplateId) 
						&& StringUtils.isNotBlank(other.getContentTemplateId())
						&& !contentTemplateId.equals(other.getContentTemplateId()))) {
			return false;
		}*/
		
		if ((StringUtils.isBlank(countryCode) 
				&& StringUtils.isNotBlank(other.getCountryCode()))
				||(StringUtils.isNotBlank(countryCode) 
						&& StringUtils.isBlank(other.getCountryCode()))
				||(StringUtils.isNotBlank(countryCode) 
						&& StringUtils.isNotBlank(other.getCountryCode())
						&& !countryCode.equals(other.getCountryCode()))) {
			return false;
		}
		
		if ((StringUtils.isBlank(sender) 
				&& StringUtils.isNotBlank(other.getSender()))
				||(StringUtils.isNotBlank(sender) 
						&& StringUtils.isBlank(other.getSender()))
				||(StringUtils.isNotBlank(sender) 
						&& StringUtils.isNotBlank(other.getSender())
						&& !sender.equals(other.getSender()))) {
			return false;
		}
		
		if ((ossid == null 
				&& other.getOssid()!=null)
				||(ossid!=null
						&& other.getOssid() == null)
				||(ossid != null
						&& other.getOssid() != null
						&& !ossid.equals(other.getOssid()))) {
			return false;
		}
		
		if ((rssid == null 
				&& other.getRssid()!=null)
				||(rssid!=null
						&& other.getRssid() == null)
				||(rssid != null
						&& other.getRssid() != null
						&& !rssid.equals(other.getRssid()))) {
			return false;
		}
		
		if ((type == null 
				&& other.getType()!=null)
				||(type!=null
						&& other.getType() == null)
				||(type != null
						&& other.getType() != null
						&& !type.equals(other.getType()))) {
			return false;
		}
		
		if (ratio != other.getRatio()) {
			return false;
		}
		
		return true;
	}
    
	@Override
	public int hashCode() {
		return super.hashCode();
	}
    
}

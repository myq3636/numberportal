package com.liugh.service.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.activerecord.Model;

public class VersionResponse extends Model<VersionResponse>{
	private static final long serialVersionUID = 1L;
    private Long id;
    /**
     * version number 10
     */
    private Integer version;
    /**
     * 1.0
     */
    private String versionDesc;
    private String packageUrl;
    private String description;
    private String uploader;
    private String uploaderTime;
	@Override
	protected Serializable pkVal() {
		return this.id;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public Integer getVersion() {
		return version;
	}
	public void setVersion(Integer version) {
		this.version = version;
	}
	public String getVersionDesc() {
		return versionDesc;
	}
	public void setVersionDesc(String versionDesc) {
		this.versionDesc = versionDesc;
	}
	public String getPackageUrl() {
		return packageUrl;
	}
	public void setPackageUrl(String packageUrl) {
		this.packageUrl = packageUrl;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getUploader() {
		return uploader;
	}
	public void setUploader(String uploader) {
		this.uploader = uploader;
	}
	public String getUploaderTime() {
		return uploaderTime;
	}
	public void setUploaderTime(String uploaderTime) {
		this.uploaderTime = uploaderTime;
	}
}

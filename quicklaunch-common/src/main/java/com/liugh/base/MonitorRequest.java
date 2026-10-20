package com.liugh.base;

public class MonitorRequest {

	private int ossid;
	private int rssid;
	private String country="0";
	private String type= "m";
	private long startTime;
	private long endTime;
	public int getOssid() {
		return ossid;
	}
	public void setOssid(int ossid) {
		this.ossid = ossid;
	}
	public int getRssid() {
		return rssid;
	}
	public void setRssid(int rssid) {
		this.rssid = rssid;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public long getStartTime() {
		return startTime;
	}
	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}
	public long getEndTime() {
		return endTime;
	}
	public void setEndTime(long endTime) {
		this.endTime = endTime;
	}
	
	
}

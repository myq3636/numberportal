package com.liugh.base;

public class SMSLoggingRequestVo {

	private int pageSize;
	private String sender;
	private String recipient;
	private String toCountry;
	private String toOperator;
	private int ossid;
	private int rssid;
	private String msgid;
	private String inMessage;
	private String outMessage;
	private String submitDelayTime;
	private String drDelayTime;
	private String status;
	private long startTime;	
	private long endTime;
	private int pageIndex;
	
	public int getPageSize() {
		return pageSize;
	}
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public String getRecipient() {
		return recipient;
	}
	public void setRecipient(String recipient) {
		this.recipient = recipient;
	}
	public String getToCountry() {
		return toCountry;
	}
	public void setToCountry(String toCountry) {
		this.toCountry = toCountry;
	}
	public String getToOperator() {
		return toOperator;
	}
	public void setToOperator(String toOperator) {
		this.toOperator = toOperator;
	}
	
	public String getMsgid() {
		return msgid;
	}
	public void setMsgid(String msgid) {
		this.msgid = msgid;
	}
	public String getInMessage() {
		return inMessage;
	}
	public void setInMessage(String inMessage) {
		this.inMessage = inMessage;
	}
	public String getOutMessage() {
		return outMessage;
	}
	public void setOutMessage(String outMessage) {
		this.outMessage = outMessage;
	}
	
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	
	public int getPageIndex() {
		return pageIndex;
	}
	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}
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
	
	public String getSubmitDelayTime() {
		return submitDelayTime;
	}
	public void setSubmitDelayTime(String submitDelayTime) {
		this.submitDelayTime = submitDelayTime;
	}
	public String getDrDelayTime() {
		return drDelayTime;
	}
	public void setDrDelayTime(String drDelayTime) {
		this.drDelayTime = drDelayTime;
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

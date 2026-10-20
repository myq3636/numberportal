package com.liugh.base;

public class TexterResult {	

	private String sender;
	private String content;
	private String phoneReceivedDateTime;
	private String serviceCenter;
	private String resultReceivedDateTime;
	private boolean syncedWithNTP;
	public String getSender() {
		return sender;
	}
	public void setSender(String sender) {
		this.sender = sender;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getPhoneReceivedDateTime() {
		return phoneReceivedDateTime;
	}
	public void setPhoneReceivedDateTime(String phoneReceivedDateTime) {
		this.phoneReceivedDateTime = phoneReceivedDateTime;
	}
	public String getServiceCenter() {
		return serviceCenter;
	}
	public void setServiceCenter(String serviceCenter) {
		this.serviceCenter = serviceCenter;
	}
	public String getResultReceivedDateTime() {
		return resultReceivedDateTime;
	}
	public void setResultReceivedDateTime(String resultReceivedDateTime) {
		this.resultReceivedDateTime = resultReceivedDateTime;
	}
	public boolean isSyncedWithNTP() {
		return syncedWithNTP;
	}
	public void setSyncedWithNTP(boolean syncedWithNTP) {
		this.syncedWithNTP = syncedWithNTP;
	}		
	
	
	
}

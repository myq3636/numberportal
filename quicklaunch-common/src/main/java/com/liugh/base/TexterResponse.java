package com.liugh.base;

public class TexterResponse {	

	private String id;
	private String msisdn;
	private String messageKey;
	private String preferredAlphaNumericSender;
	private String preferredNumericSender;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getMsisdn() {
		return msisdn;
	}
	public void setMsisdn(String msisdn) {
		this.msisdn = msisdn;
	}
	public String getMessageKey() {
		return messageKey;
	}
	public void setMessageKey(String messageKey) {
		this.messageKey = messageKey;
	}
	public String getPreferredAlphaNumericSender() {
		return preferredAlphaNumericSender;
	}
	public void setPreferredAlphaNumericSender(String preferredAlphaNumericSender) {
		this.preferredAlphaNumericSender = preferredAlphaNumericSender;
	}
	public String getPreferredNumericSender() {
		return preferredNumericSender;
	}
	public void setPreferredNumericSender(String preferredNumericSender) {
		this.preferredNumericSender = preferredNumericSender;
	}
	
	
}

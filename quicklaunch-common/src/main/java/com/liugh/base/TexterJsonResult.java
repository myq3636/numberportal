package com.liugh.base;

public class TexterJsonResult {	
	
	private String id;
	private String requestDate;
	private int statusCode;
	private String status;
	private String mccMnc;
	private String expirationDate;
	private String msisdn;
	private String messageKey;
	private String preferredAlphaNumericSender;
	private String preferredNumericSender;
	private TexterResult result;
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
	public String getRequestDate() {
		return requestDate;
	}
	public void setRequestDate(String requestDate) {
		this.requestDate = requestDate;
	}
	public int getStatusCode() {
		return statusCode;
	}
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getMccMnc() {
		return mccMnc;
	}
	public void setMccMnc(String mccMnc) {
		this.mccMnc = mccMnc;
	}
	public String getExpirationDate() {
		return expirationDate;
	}
	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}
	public TexterResult getResult() {
		return result;
	}
	public void setResult(TexterResult result) {
		this.result = result;
	}
	
	
	
}

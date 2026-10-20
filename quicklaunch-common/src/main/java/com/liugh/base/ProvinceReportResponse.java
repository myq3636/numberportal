package com.liugh.base;

public class ProvinceReportResponse {

	private String province;
	private int totalMsgNumber;
	private String successfulRate;
	private String failedRate;
	public String getProvince() {
		return province;
	}
	public void setProvince(String province) {
		this.province = province;
	}
	public int getTotalMsgNumber() {
		return totalMsgNumber;
	}
	public void setTotalMsgNumber(int totalMsgNumber) {
		this.totalMsgNumber = totalMsgNumber;
	}
	public String getSuccessfulRate() {
		return successfulRate;
	}
	public void setSuccessfulRate(String successfulRate) {
		this.successfulRate = successfulRate;
	}
	public String getFailedRate() {
		return failedRate;
	}
	public void setFailedRate(String failedRate) {
		this.failedRate = failedRate;
	}
	
	
	
}

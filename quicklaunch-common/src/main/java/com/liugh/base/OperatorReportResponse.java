package com.liugh.base;

public class OperatorReportResponse {

	private String operator;
	private int totalMsgNumber;
	private String successfulRate;
	private String failedRate;
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
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

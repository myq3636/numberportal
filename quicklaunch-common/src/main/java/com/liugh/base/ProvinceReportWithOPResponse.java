package com.liugh.base;

public class ProvinceReportWithOPResponse {

	private String province;
	private int totalMsgNumber;
	private String successfulRate;
	private String failedRate;	
	private OperatorReportResponse cm;
	private OperatorReportResponse ct;
	private OperatorReportResponse cu;
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
	public OperatorReportResponse getCm() {
		return cm;
	}
	public void setCm(OperatorReportResponse cm) {
		this.cm = cm;
	}
	public OperatorReportResponse getCt() {
		return ct;
	}
	public void setCt(OperatorReportResponse ct) {
		this.ct = ct;
	}
	public OperatorReportResponse getCu() {
		return cu;
	}
	public void setCu(OperatorReportResponse cu) {
		this.cu = cu;
	}
	
	
}

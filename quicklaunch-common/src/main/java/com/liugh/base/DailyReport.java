package com.liugh.base;

import java.util.Date;

public class DailyReport {

	private String startTime;
    private int batchCount;
    private int ssid;
    private int fileLines;
    private int inSuccessCount;
    private int inBillingCount;
    private int inFailedCount;
    private int inSendCount;
    private String customerName;
	
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public int getBatchCount() {
		return batchCount;
	}
	public void setBatchCount(int batchCount) {
		this.batchCount = batchCount;
	}
	public int getSsid() {
		return ssid;
	}
	public void setSsid(int ssid) {
		this.ssid = ssid;
	}
	public int getFileLines() {
		return fileLines;
	}
	public void setFileLines(int fileLines) {
		this.fileLines = fileLines;
	}
	public int getInSuccessCount() {
		return inSuccessCount;
	}
	public void setInSuccessCount(int inSuccessCount) {
		this.inSuccessCount = inSuccessCount;
	}
	public int getInFailedCount() {
		return inFailedCount;
	}
	public void setInFailedCount(int inFailedCount) {
		this.inFailedCount = inFailedCount;
	}
	public int getInSendCount() {
		return inSendCount;
	}
	public void setInSendCount(int inSendCount) {
		this.inSendCount = inSendCount;
	}
	public String getCustomerName() {
		return customerName;
	}
	public void setCustomerName(String customerName) {
		this.customerName = customerName;
	}
	public int getInBillingCount() {
		return inBillingCount;
	}
	public void setInBillingCount(int inBillingCount) {
		this.inBillingCount = inBillingCount;
	}
    
    
}

package com.liugh.base;

import java.util.Date;

public class Report {

	private String startTime;
    private int totalNum;
    private int successNum;
    private int failNum;
    private String successRatio="0%";
	
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public int getTotalNum() {
		return totalNum;
	}
	public void setTotalNum(int totalNum) {
		this.totalNum = totalNum;
	}
	public int getSuccessNum() {
		return successNum;
	}
	public void setSuccessNum(int successNum) {
		this.successNum = successNum;
	}
	public int getFailNum() {
		return failNum;
	}
	public void setFailNum(int failNum) {
		this.failNum = failNum;
	}
	public String getSuccessRatio() {
		return successRatio;
	}
	public void setSuccessRatio(String successRatio) {
		this.successRatio = successRatio;
	}
	
    
    
}

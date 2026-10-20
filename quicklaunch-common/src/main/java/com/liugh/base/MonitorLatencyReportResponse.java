package com.liugh.base;

import java.util.List;

public class MonitorLatencyReportResponse {
	private List<String> submitTimeList;
	private List<String> timeList;
	private List<String> outDRTimeList;
	private List<String> inDRTimeList;
	private List<Integer> submitLC;
	private List<Integer> outDRLC;
	private List<Integer> inDRLC;
	public List<String> getSubmitTimeList() {
		return submitTimeList;
	}
	public void setSubmitTimeList(List<String> submitTimeList) {
		this.submitTimeList = submitTimeList;
	}
	public List<String> getOutDRTimeList() {
		return outDRTimeList;
	}
	public void setOutDRTimeList(List<String> outDRTimeList) {
		this.outDRTimeList = outDRTimeList;
	}
	public List<String> getInDRTimeList() {
		return inDRTimeList;
	}
	public void setInDRTimeList(List<String> inDRTimeList) {
		this.inDRTimeList = inDRTimeList;
	}
	public List<Integer> getSubmitLC() {
		return submitLC;
	}
	public void setSubmitLC(List<Integer> submitLC) {
		this.submitLC = submitLC;
	}
	public List<Integer> getOutDRLC() {
		return outDRLC;
	}
	public void setOutDRLC(List<Integer> outDRLC) {
		this.outDRLC = outDRLC;
	}
	public List<Integer> getInDRLC() {
		return inDRLC;
	}
	public void setInDRLC(List<Integer> inDRLC) {
		this.inDRLC = inDRLC;
	}
	public List<String> getTimeList() {
		return timeList;
	}
	public void setTimeList(List<String> timeList) {
		this.timeList = timeList;
	}	
	
	
	   
}

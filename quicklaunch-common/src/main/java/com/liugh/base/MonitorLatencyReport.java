package com.liugh.base;

import java.util.ArrayList;
import java.util.List;

public class MonitorLatencyReport {
	private List<LatencyReport> submitLC;
	private List<LatencyReport> outDRLC;
	private List<LatencyReport> inDRLC;
	public List<LatencyReport> getSubmitLC() {
		return submitLC;
	}
	public void setSubmitLC(List<LatencyReport> submitLC) {
		this.submitLC = submitLC;
	}
	public List<LatencyReport> getOutDRLC() {
		return outDRLC;
	}
	public void setOutDRLC(List<LatencyReport> outDRLC) {
		this.outDRLC = outDRLC;
	}
	public List<LatencyReport> getInDRLC() {
		return inDRLC;
	}
	public void setInDRLC(List<LatencyReport> inDRLC) {
		this.inDRLC = inDRLC;
	}
	
	
	public MonitorLatencyReportResponse convertToResponse(){
		MonitorLatencyReportResponse response = new MonitorLatencyReportResponse();
		List<String> subTimeList = new ArrayList<String>();
		List<String> timeList = new ArrayList<String>();
		List<String> indrTimeList = new ArrayList<String>();
		List<String> outdrTimeList = new ArrayList<String>();
		List<Integer> subList = new ArrayList<Integer>();
		List<Integer> indrList = new ArrayList<Integer>();
		List<Integer> outdrList = new ArrayList<Integer>();
		response.setInDRLC(indrList);
		response.setInDRTimeList(indrTimeList);
		response.setOutDRLC(outdrList);
		response.setOutDRTimeList(outdrTimeList);
		response.setSubmitLC(subList);
		response.setSubmitTimeList(subTimeList);
		response.setTimeList(timeList);
		boolean isTime = false;
		
		if (inDRLC != null) {
			if (timeList.isEmpty()) {
				isTime = true;
			}else{
				isTime = false;
			}
			for ( LatencyReport report : inDRLC) {
				indrList.add(report.getLatency());
				if (isTime) {
					timeList.add(report.getStartTime());
				}
				indrTimeList.add(report.getStartTime());
			}
		}
		
		if (outDRLC != null) {
			if (timeList.isEmpty()) {
				isTime = true;
			}else{
				isTime = false;
			}
			for ( LatencyReport report : outDRLC) {
				outdrList.add(report.getLatency());
				outdrTimeList.add(report.getStartTime());
				if (isTime) {
					timeList.add(report.getStartTime());
				}
			}
		}
		
		if (submitLC != null) {
			if (timeList.isEmpty()) {
				isTime = true;
			}else{
				isTime = false;
			}
			for ( LatencyReport report : submitLC) {
				subList.add(report.getLatency());
				subTimeList.add(report.getStartTime());
				if (isTime) {
					timeList.add(report.getStartTime());
				}
			}
		}
		return response;
	}
	
	
	   
}

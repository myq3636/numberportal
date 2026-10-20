package com.liugh.base;

import java.util.ArrayList;
import java.util.List;

import com.sun.org.apache.bcel.internal.generic.RETURN;

public class MonitorReport {
	private List<Report> inSubmit;
	private List<Report> outSubmit;
	private List<Report> inDR;
	private List<Report> outDR;
	
	public List<Report> getInSubmit() {
		return inSubmit;
	}
	public void setInSubmit(List<Report> inSubmit) {
		this.inSubmit = inSubmit;
	}
	public List<Report> getOutSubmit() {
		return outSubmit;
	}
	public void setOutSubmit(List<Report> outSubmit) {
		this.outSubmit = outSubmit;
	}
	public List<Report> getInDR() {
		return inDR;
	}
	public void setInDR(List<Report> inDR) {
		this.inDR = inDR;
	}
	public List<Report> getOutDR() {
		return outDR;
	}
	public void setOutDR(List<Report> outDR) {
		this.outDR = outDR;
	}
	
	public MonitorChartReport convertToChartReport(){
		MonitorChartReport chart = new MonitorChartReport();
		List<String> timeList = new ArrayList<String>();
		List<String> inSubmitSuccessRatio = new ArrayList<String>();
		List<String> outSubmitSuccessRatio = new ArrayList<String>();
		List<String> outDRSuccessRatio = new ArrayList<String>();
		List<String> inDRSuccessRatio = new ArrayList<String>();
		List<Integer> totalInSubmitNum = new ArrayList<Integer>();
		List<Integer> totalOutSubmitNum = new ArrayList<Integer>();
		List<Integer> totalINDRNum = new ArrayList<Integer>();
		List<Integer> totalOutDRNum = new ArrayList<Integer>();
		chart.setInDRSuccessRatio(inDRSuccessRatio);
		chart.setOutDRSuccessRatio(outDRSuccessRatio);
		chart.setInSubmitSuccessRatio(inSubmitSuccessRatio);
		chart.setOutSubmitSuccessRatio(outSubmitSuccessRatio);
		chart.setTimeList(timeList);
		chart.setTotalINDRNum(totalINDRNum);
		chart.setTotalInSubmitNum(totalInSubmitNum);
		chart.setTotalOutDRNum(totalOutDRNum);
		chart.setTotalOutSubmitNum(totalOutSubmitNum);
		boolean isTime = false;
		if (inSubmit!=null) {			
			if (timeList.isEmpty()) {
				isTime = true;
			}else {
				isTime = false;
			}
			for (Report report: inSubmit) {
				if (isTime) {
					timeList.add(report.getStartTime());
				}				
				totalInSubmitNum.add(report.getTotalNum());
				String ratio = report.getSuccessRatio();
				if (ratio.contains("%")) {
					ratio = ratio.substring(0, ratio.length()-1);
				}
				inSubmitSuccessRatio.add(ratio);
			}
		}
		
		if (outSubmit!=null) {
			if (timeList.isEmpty()) {
				isTime = true;
			}else {
				isTime = false;
			}
			for (Report report: outSubmit) {
				if (isTime) {
					timeList.add(report.getStartTime());
				}
				totalOutSubmitNum.add(report.getTotalNum());
				String ratio = report.getSuccessRatio();
				if (ratio.contains("%")) {
					ratio = ratio.substring(0, ratio.length()-1);
				}
				outSubmitSuccessRatio.add(ratio);
			}
		}
		
		if (inDR!=null) {
			if (timeList.isEmpty()) {
				isTime = true;
			}else {
				isTime = false;
			}
			for (Report report: inDR) {
				if (isTime) {
					timeList.add(report.getStartTime());
				}
				totalINDRNum.add(report.getTotalNum());
				String ratio = report.getSuccessRatio();
				if (ratio.contains("%")) {
					ratio = ratio.substring(0, ratio.length()-1);
				}
				inDRSuccessRatio.add(ratio);
			}
		}
		
		if (outDR!=null) {
			if (timeList.isEmpty()) {
				isTime = true;
			}else {
				isTime = false;
			}
			for (Report report: outDR) {
				if (isTime) {
					timeList.add(report.getStartTime());
				}
				totalOutDRNum.add(report.getTotalNum());
				String ratio = report.getSuccessRatio();
				if (ratio.contains("%")) {
					ratio = ratio.substring(0, ratio.length()-1);
				}
				outDRSuccessRatio.add(ratio);
			}
		}
	
		return chart;
	}
	
	   
}

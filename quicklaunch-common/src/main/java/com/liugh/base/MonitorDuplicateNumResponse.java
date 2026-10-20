package com.liugh.base;

import java.util.List;

public class MonitorDuplicateNumResponse {
	private List<String> timeList;
	private List<String> duplicateNumberPercentage;
	private List<Integer> duplicateNumber;
	private List<Integer> totalNumber;
	
	public List<String> getTimeList() {
		return timeList;
	}
	public void setTimeList(List<String> timeList) {
		this.timeList = timeList;
	}
	public List<String> getDuplicateNumberPercentage() {
		return duplicateNumberPercentage;
	}
	public void setDuplicateNumberPercentage(List<String> duplicateNumberPercentage) {
		this.duplicateNumberPercentage = duplicateNumberPercentage;
	}
	public List<Integer> getDuplicateNumber() {
		return duplicateNumber;
	}
	public void setDuplicateNumber(List<Integer> duplicateNumber) {
		this.duplicateNumber = duplicateNumber;
	}
	public List<Integer> getTotalNumber() {
		return totalNumber;
	}
	public void setTotalNumber(List<Integer> totalNumber) {
		this.totalNumber = totalNumber;
	}
	
	

	
	
	
	   
}

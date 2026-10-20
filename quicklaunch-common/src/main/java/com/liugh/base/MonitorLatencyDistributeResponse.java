package com.liugh.base;

import java.util.List;

public class MonitorLatencyDistributeResponse {
	private List<String> timeList;
	private List<Integer> less1MNum;
	private List<Integer> b1a5MNum;
	private List<Integer> b5a15MNum;
	private List<Integer> morethan15MNum;
	private List<Integer> nodrNum;
	public List<String> getTimeList() {
		return timeList;
	}
	public void setTimeList(List<String> timeList) {
		this.timeList = timeList;
	}
	public List<Integer> getLess1MNum() {
		return less1MNum;
	}
	public void setLess1MNum(List<Integer> less1mNum) {
		less1MNum = less1mNum;
	}
	public List<Integer> getB1a5MNum() {
		return b1a5MNum;
	}
	public void setB1a5MNum(List<Integer> b1a5mNum) {
		b1a5MNum = b1a5mNum;
	}
	public List<Integer> getB5a15MNum() {
		return b5a15MNum;
	}
	public void setB5a15MNum(List<Integer> b5a15mNum) {
		b5a15MNum = b5a15mNum;
	}
	public List<Integer> getMorethan15MNum() {
		return morethan15MNum;
	}
	public void setMorethan15MNum(List<Integer> morethan15mNum) {
		morethan15MNum = morethan15mNum;
	}
	public List<Integer> getNodrNum() {
		return nodrNum;
	}
	public void setNodrNum(List<Integer> nodrNum) {
		this.nodrNum = nodrNum;
	}

	
	
	
	   
}

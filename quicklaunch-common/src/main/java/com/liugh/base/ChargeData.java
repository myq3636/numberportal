package com.liugh.base;

import java.util.List;

public class ChargeData {
	
	private int ssid;
	private List<ChargeResponse> records;
	private int total;
	
	
	public int getSsid() {
		return ssid;
	}
	public void setSsid(int ssid) {
		this.ssid = ssid;
	}
	
	
	public List<ChargeResponse> getRecords() {
		return records;
	}
	public void setRecords(List<ChargeResponse> records) {
		this.records = records;
	}
	
	public int getTotal() {
		return total;
	}
	public void setTotal(int total) {
		this.total = total;
	}
	
	
}

package com.liugh.base;

import java.util.List;


public class Charges {
	private int code;
	private int ssid;
	private List<ChargeResponse> records;
	private int total;
	private String message;
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
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

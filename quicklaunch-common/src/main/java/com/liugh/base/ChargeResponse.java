package com.liugh.base;


public class ChargeResponse {

	int id;
	String charge_money;
	String timemark;
	String balance;//billing balance;
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public String getCharge_money() {
		return charge_money;
	}
	public void setCharge_money(String charge_money) {
		this.charge_money = charge_money;
	}
	public String getTimemark() {
		return timemark;
	}
	public void setTimemark(String timemark) {
		this.timemark = timemark;
	}
	public String getBalance() {
		return balance;
	}
	public void setBalance(String balance) {
		this.balance = balance;
	}
	
	
}

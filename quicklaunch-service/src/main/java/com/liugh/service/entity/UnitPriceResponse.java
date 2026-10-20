package com.liugh.service.entity;

import lombok.Data;

@Data
public class UnitPriceResponse {

	private int id;
	private int ssid;
	private String username;
	private String countryCode;
	private String unitPrice;
	private String customerName;
	private String sn;
	private Way way;
	
}

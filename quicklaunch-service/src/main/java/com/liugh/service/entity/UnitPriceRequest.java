package com.liugh.service.entity;

import lombok.Data;

@Data
public class UnitPriceRequest {

	private int ssid;
	private String username;
	private String countryCode;
	private String unitPrice;
	private String customerName;
	private Way way;
	
}

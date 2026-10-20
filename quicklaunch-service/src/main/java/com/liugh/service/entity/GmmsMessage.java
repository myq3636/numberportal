package com.liugh.service.entity;

import lombok.Data;

@Data
public class GmmsMessage {

	public static final String AIC_CS_ASCII = "ASCII";
	public static final String AIC_CS_UCS2 = "UnicodeBigUnmarked";
	protected String senderAddress = null;
    protected String[] recipientAddresses = null;
    protected String recipientAddress = null;
    protected String textContent = null;
    protected String outMsgID = null;
    protected String contentType = null;
}

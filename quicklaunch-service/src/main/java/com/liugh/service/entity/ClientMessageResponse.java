package com.liugh.service.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.activerecord.Model;

public class ClientMessageResponse extends Model<ClientMessageResponse>{
	private static final long serialVersionUID = 1L;
	private long id;
	private String messageType;
	private String messageText;
	@Override
	protected Serializable pkVal() {
		return this.id;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getMessageType() {
		return messageType;
	}
	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}
	public String getMessageText() {
		return messageText;
	}
	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}
}

package com.liugh.service.entity;

import java.io.Serializable;

import com.baomidou.mybatisplus.activerecord.Model;

public class GroupResponse extends Model<GroupResponse>{
	private static final long serialVersionUID = 1L;
	private long id;
	private String groupName;
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
	public String getGroupName() {
		return groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
}

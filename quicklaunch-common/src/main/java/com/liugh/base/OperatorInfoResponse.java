package com.liugh.base;



public class OperatorInfoResponse implements Comparable{

	private String mccmnc;
	private String info;
	public String getMccmnc() {
		return mccmnc;
	}
	public void setMccmnc(String mccmnc) {
		this.mccmnc = mccmnc;
	}
	public String getInfo() {
		return info;
	}
	public void setInfo(String info) {
		this.info = info;
	}
	
	@Override
	public int compareTo(Object o) {
		OperatorInfoResponse other = (OperatorInfoResponse)o;
		if (this!=null && other!=null && this.info!=null && other.info!=null) {
			return this.info.compareToIgnoreCase(other.info);
		}
		return -1;
	}
}

package com.liugh.base;


public class MonitorCountryReport implements Comparable{
	private Report inSubmit;
	private Report outSubmit;
	private Report inDR;
	private Report outDR;
	private String country;
	
	public Report getInSubmit() {
		return inSubmit;
	}
	public void setInSubmit(Report inSubmit) {
		this.inSubmit = inSubmit;
	}
	public Report getOutSubmit() {
		return outSubmit;
	}
	public void setOutSubmit(Report outSubmit) {
		this.outSubmit = outSubmit;
	}
	public Report getInDR() {
		return inDR;
	}
	public void setInDR(Report inDR) {
		this.inDR = inDR;
	}
	public Report getOutDR() {
		return outDR;
	}
	public void setOutDR(Report outDR) {
		this.outDR = outDR;
	}
	public String getCountry() {
		return country;
	}
	public void setCountry(String country) {
		this.country = country;
	}
	@Override
	public int compareTo(Object o) {
		MonitorCountryReport other = (MonitorCountryReport)o;
		if (this!=null && other!=null && this.inSubmit!= null && other.inSubmit !=null) {
			return this.inSubmit.getTotalNum()-other.inSubmit.getTotalNum();
		}
		return 0;
	}  
}

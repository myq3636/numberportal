package com.liugh.service.entity;

import java.lang.reflect.Method;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 微信返回的订单实体
 * @author hxy
 *
 */

@XmlRootElement(name="MSG")  
@XmlAccessorType(XmlAccessType.FIELD)  
public class JxSubMessage {
	
	@XmlElement(required = true)
	private String FROM;
	@XmlElement(required = true)
	private String TO;
	@XmlElement
	private String BODY;
	@XmlElement
	private String REFERENCE;
	@XmlElement
	private String DCS;
	@XmlElement
	private String MINIMUMNUMBEROFMESSAGEPARTS;
	@XmlElement
	private String MAXIMUMNUMBEROFMESSAGEPARTS;
	@XmlElement
	private String VALIDITY;
	public String getFROM() {
		return FROM;
	}
	public void setFROM(String fROM) {
		FROM = fROM;
	}
	public String getTO() {
		return TO;
	}
	public void setTO(String tO) {
		TO = tO;
	}
	public String getBODY() {
		return BODY;
	}
	public void setBODY(String bODY) {
		BODY = bODY;
	}
	public String getREFERENCE() {
		return REFERENCE;
	}
	public void setREFERENCE(String rEFERENCE) {
		REFERENCE = rEFERENCE;
	}
	public String getDCS() {
		return DCS;
	}
	public void setDCS(String dCS) {
		DCS = dCS;
	}
	public String getMINIMUMNUMBEROFMESSAGEPARTS() {
		return MINIMUMNUMBEROFMESSAGEPARTS;
	}
	public void setMINIMUMNUMBEROFMESSAGEPARTS(String mINIMUMNUMBEROFMESSAGEPARTS) {
		MINIMUMNUMBEROFMESSAGEPARTS = mINIMUMNUMBEROFMESSAGEPARTS;
	}
	public String getMAXIMUMNUMBEROFMESSAGEPARTS() {
		return MAXIMUMNUMBEROFMESSAGEPARTS;
	}
	public void setMAXIMUMNUMBEROFMESSAGEPARTS(String mAXIMUMNUMBEROFMESSAGEPARTS) {
		MAXIMUMNUMBEROFMESSAGEPARTS = mAXIMUMNUMBEROFMESSAGEPARTS;
	}
	public String getVALIDITY() {
		return VALIDITY;
	}
	public void setVALIDITY(String vALIDITY) {
		VALIDITY = vALIDITY;
	}
	
	public boolean setProperty(String propertyName, Object value) {
		boolean bret = false;
		Method[] methods = JxSubMessage.class.getMethods();
		for (Method f : methods) {
			if (f.getName().equalsIgnoreCase("set" + propertyName)) {
				try {
					f.invoke(this, value);
					bret = true;
					break;
				} catch (Exception e) {
					System.out.println("set properyt " + propertyName + " error:" + e.getMessage());
					continue;
				}
				
			}
		}
		return bret;
	}
	
	

}

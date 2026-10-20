package com.liugh.service.util;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import lombok.extern.log4j.Log4j;

import com.liugh.service.entity.GmmsMessage;
import com.liugh.service.entity.JxMessage;
import com.liugh.service.entity.JxSubMessage;
import com.liugh.service.entity.JxToken;

@Log4j
public class XmlGeneratorUtil {

	public static String createXml(String productToken, GmmsMessage message, int batch, int counts) {
        try {
        	StringWriter sw = new StringWriter();
        	 JxMessage jxMessage = new JxMessage();
        	 JxSubMessage jxSubMessage = new JxSubMessage();
        	 JxToken token = new JxToken();
        	 token.setPRODUCTTOKEN(productToken);
        	 jxSubMessage.setDCS(GmmsMessage.AIC_CS_UCS2.equals(message.getContentType())?"8":"0");
        	 jxSubMessage.setFROM(message.getSenderAddress());
        	 jxSubMessage.setBODY(message.getTextContent());
        	 jxSubMessage.setTO(message.getRecipientAddress());
        	 if (message.getOutMsgID() == null) {
        		 String commonMsgID = MessageIdGenerator.generateCommonStringID(batch, counts);
				message.setOutMsgID(commonMsgID);
			}
        	 jxSubMessage.setREFERENCE(message.getOutMsgID());
        	 jxMessage.setAUTHENTICATION(token);
        	 List<JxSubMessage> messages = new ArrayList<>();
        	 messages.add(jxSubMessage);
        	 jxMessage.setMSG(messages);
             JAXBContext jAXBContext = JAXBContext.newInstance(jxMessage.getClass());
             Marshaller marshaller = jAXBContext.createMarshaller();
             marshaller.marshal(jxMessage, sw); 
            return sw.toString();

        } catch (Exception ex) {
        	log.error("create xml exception!", ex);
            return null;
        } 
    }

	public static String createXml(String productToken, GmmsMessage message, int ssid) {
		try {
			StringWriter sw = new StringWriter();
			JxMessage jxMessage = new JxMessage();
			JxSubMessage jxSubMessage = new JxSubMessage();
			JxToken token = new JxToken();
			token.setPRODUCTTOKEN(productToken);
			jxSubMessage.setDCS(GmmsMessage.AIC_CS_UCS2.equals(message.getContentType())?"8":"0");
			jxSubMessage.setFROM(message.getSenderAddress());
			jxSubMessage.setBODY(message.getTextContent());
			jxSubMessage.setTO(message.getRecipientAddress());
			if (message.getOutMsgID() == null) {
				String commonMsgID = MessageIdGenerator.generateCommonInMsgID(ssid);
				message.setOutMsgID(commonMsgID);
			}
			jxSubMessage.setREFERENCE(message.getOutMsgID());
			jxMessage.setAUTHENTICATION(token);
			List<JxSubMessage> messages = new ArrayList<>();
			messages.add(jxSubMessage);
			jxMessage.setMSG(messages);
			JAXBContext jAXBContext = JAXBContext.newInstance(jxMessage.getClass());
			Marshaller marshaller = jAXBContext.createMarshaller();
			marshaller.marshal(jxMessage, sw);
			return sw.toString();

		} catch (Exception ex) {
			log.error("create xml exception!", ex);
			return null;
		}
	}
}

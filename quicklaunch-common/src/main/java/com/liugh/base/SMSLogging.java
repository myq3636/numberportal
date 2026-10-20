package com.liugh.base;


import java.util.Date;
import java.util.List;

/**
 * <p>
 * 
 * </p>
 *
 * @author liugh123
 * @since 2019-03-05
 */
public class SMSLogging {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String CDRType;
    private String InTransID;
    private String OutTransID;
    private String InMsgID;
    private String OutMsgID;
    private Integer OSsID;
    private Integer RSsID;
    private String GmmsMsgType;
    private String MessageType;
    private Integer MessageSize;
    private String ProtocolVersion;
    private String SenderAddrType;
    private String RecipientAddrType;
    private String SenderAddresses;
    private String RecipientAddresses;
    private Date TimeMark;
    private Date ExpiryDate;
    private Integer DeliveryReport;
    private String Priority;
    private String ContentType;
    private String textcontent;
    private String StatusCode;
    private String StatusText;
    private Integer InClientPull;
    private Integer OutClientPull;
    private String DeliveryChannel;
    private Date DateIn;
    private Integer MilterActionCode;
    private Integer oOperator;
    private Integer rOperator;
    private Integer ActionCode;
    private Integer currentA2p;
    private Integer oA2p;
    private Integer rA2p;
    private String MsgID;
    private Integer Split;
    private String msgRefNum;
    private Integer totalSegments;
    private Integer segmentSeqNum;
    private Long CDRTime;
    private Integer InCsm;
    private Integer ServiceTypeID;
    private Date ScheduleDeliveryTime;
    private Integer MessageState;
    private String insubmitTime;
    private String outsubmitTime;
    private String inDeliveryTime;
    private String outDeliverTime;
    private long drLatency;
    private String vendor;
    private String customer;
    private List<SMSLoggingField> smsLoggingFields;
	
	private String country;
    private String inContentType;
    private String outContentType;
    private String inContent;
    private String outContent;
    private String inMsgSize;
    private String outMsgSize;
    private String operator;
    private long submitLatency;
    private String inDRStatusCode;
    private String inSubmitStatusCode;
    private String outDRStatusCode;
    private String outSubmitStatusCode;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCDRType() {
        return CDRType;
    }

    public void setCDRType(String CDRType) {
        this.CDRType = CDRType;
    }

    public String getInTransID() {
        return InTransID;
    }

    public void setInTransID(String InTransID) {
        this.InTransID = InTransID;
    }

    public String getOutTransID() {
        return OutTransID;
    }

    public void setOutTransID(String OutTransID) {
        this.OutTransID = OutTransID;
    }

    public String getInMsgID() {
        return InMsgID;
    }

    public void setInMsgID(String InMsgID) {
        this.InMsgID = InMsgID;
    }

    public String getOutMsgID() {
        return OutMsgID;
    }

    public void setOutMsgID(String OutMsgID) {
        this.OutMsgID = OutMsgID;
    }

    public Integer getOSsID() {
        return OSsID;
    }

    public void setOSsID(Integer OSsID) {
        this.OSsID = OSsID;
    }

    public Integer getRSsID() {
        return RSsID;
    }

    public void setRSsID(Integer RSsID) {
        this.RSsID = RSsID;
    }

    public String getGmmsMsgType() {
        return GmmsMsgType;
    }

    public void setGmmsMsgType(String GmmsMsgType) {
        this.GmmsMsgType = GmmsMsgType;
    }

    public String getMessageType() {
        return MessageType;
    }

    public void setMessageType(String MessageType) {
        this.MessageType = MessageType;
    }

    public Integer getMessageSize() {
        return MessageSize;
    }

    public void setMessageSize(Integer MessageSize) {
        this.MessageSize = MessageSize;
    }

    public String getProtocolVersion() {
        return ProtocolVersion;
    }

    public void setProtocolVersion(String ProtocolVersion) {
        this.ProtocolVersion = ProtocolVersion;
    }

    public String getSenderAddrType() {
        return SenderAddrType;
    }

    public void setSenderAddrType(String SenderAddrType) {
        this.SenderAddrType = SenderAddrType;
    }

    public String getRecipientAddrType() {
        return RecipientAddrType;
    }

    public void setRecipientAddrType(String RecipientAddrType) {
        this.RecipientAddrType = RecipientAddrType;
    }

    public String getSenderAddresses() {
        return SenderAddresses;
    }

    public void setSenderAddresses(String SenderAddresses) {
        this.SenderAddresses = SenderAddresses;
    }

    public String getRecipientAddresses() {
        return RecipientAddresses;
    }

    public void setRecipientAddresses(String RecipientAddresses) {
        this.RecipientAddresses = RecipientAddresses;
    }

    public Date getTimeMark() {
        return TimeMark;
    }

    public void setTimeMark(Date TimeMark) {
        this.TimeMark = TimeMark;
    }

    public Date getExpiryDate() {
        return ExpiryDate;
    }

    public void setExpiryDate(Date ExpiryDate) {
        this.ExpiryDate = ExpiryDate;
    }

    public Integer getDeliveryReport() {
        return DeliveryReport;
    }

    public void setDeliveryReport(Integer DeliveryReport) {
        this.DeliveryReport = DeliveryReport;
    }

    public String getPriority() {
        return Priority;
    }

    public void setPriority(String Priority) {
        this.Priority = Priority;
    }

    public String getContentType() {
        return ContentType;
    }

    public void setContentType(String ContentType) {
        this.ContentType = ContentType;
    }

    public String getTextcontent() {
        return textcontent;
    }

    public void setTextcontent(String textcontent) {
        this.textcontent = textcontent;
    }

    public String getStatusCode() {
        return StatusCode;
    }

    public void setStatusCode(String StatusCode) {
        this.StatusCode = StatusCode;
    }

    public String getStatusText() {
        return StatusText;
    }

    public void setStatusText(String StatusText) {
        this.StatusText = StatusText;
    }

    public Integer getInClientPull() {
        return InClientPull;
    }

    public void setInClientPull(Integer InClientPull) {
        this.InClientPull = InClientPull;
    }

    public Integer getOutClientPull() {
        return OutClientPull;
    }

    public void setOutClientPull(Integer OutClientPull) {
        this.OutClientPull = OutClientPull;
    }

    public String getDeliveryChannel() {
        return DeliveryChannel;
    }

    public void setDeliveryChannel(String DeliveryChannel) {
        this.DeliveryChannel = DeliveryChannel;
    }

    public Date getDateIn() {
        return DateIn;
    }

    public void setDateIn(Date DateIn) {
        this.DateIn = DateIn;
    }

    public Integer getMilterActionCode() {
        return MilterActionCode;
    }

    public void setMilterActionCode(Integer MilterActionCode) {
        this.MilterActionCode = MilterActionCode;
    }

    
    public Integer getoOperator() {
        return oOperator;
    }

    public void setoOperator(Integer oOperator) {
        this.oOperator = oOperator;
    }

    public Integer getrOperator() {
        return rOperator;
    }

    public void setrOperator(Integer rOperator) {
        this.rOperator = rOperator;
    }

    public Integer getActionCode() {
        return ActionCode;
    }

    public void setActionCode(Integer ActionCode) {
        this.ActionCode = ActionCode;
    }

    public Integer getCurrentA2p() {
        return currentA2p;
    }

    public void setCurrentA2p(Integer currentA2p) {
        this.currentA2p = currentA2p;
    }

    public Integer getoA2p() {
        return oA2p;
    }

    public void setoA2p(Integer oA2p) {
        this.oA2p = oA2p;
    }

    public Integer getrA2p() {
        return rA2p;
    }

    public void setrA2p(Integer rA2p) {
        this.rA2p = rA2p;
    }

    public String getMsgID() {
        return MsgID;
    }

    public void setMsgID(String MsgID) {
        this.MsgID = MsgID;
    }

    public Integer getSplit() {
        return Split;
    }

    public void setSplit(Integer Split) {
        this.Split = Split;
    }

    public String getMsgRefNum() {
        return msgRefNum;
    }

    public void setMsgRefNum(String msgRefNum) {
        this.msgRefNum = msgRefNum;
    }

    public Integer getTotalSegments() {
        return totalSegments;
    }

    public void setTotalSegments(Integer totalSegments) {
        this.totalSegments = totalSegments;
    }

    public Integer getSegmentSeqNum() {
        return segmentSeqNum;
    }

    public void setSegmentSeqNum(Integer segmentSeqNum) {
        this.segmentSeqNum = segmentSeqNum;
    }

    public Long getCDRTime() {
        return CDRTime;
    }

    public void setCDRTime(Long CDRTime) {
        this.CDRTime = CDRTime;
    }

    public Integer getInCsm() {
        return InCsm;
    }

    public void setInCsm(Integer InCsm) {
        this.InCsm = InCsm;
    }

    public Integer getServiceTypeID() {
        return ServiceTypeID;
    }

    public void setServiceTypeID(Integer ServiceTypeID) {
        this.ServiceTypeID = ServiceTypeID;
    }

    public Date getScheduleDeliveryTime() {
        return ScheduleDeliveryTime;
    }

    public void setScheduleDeliveryTime(Date ScheduleDeliveryTime) {
        this.ScheduleDeliveryTime = ScheduleDeliveryTime;
    }

    public Integer getMessageState() {
        return MessageState;
    }

    public void setMessageState(Integer MessageState) {
        this.MessageState = MessageState;
    }

    public List<SMSLoggingField> getSmsLoggingFields() {
		return smsLoggingFields;
	}

	public void setSmsLoggingFields(List<SMSLoggingField> smsLoggingFields) {
		this.smsLoggingFields = smsLoggingFields;
	}
	
	

	

	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public String getInsubmitTime() {
		return insubmitTime;
	}

	public void setInsubmitTime(String insubmitTime) {
		this.insubmitTime = insubmitTime;
	}

	public String getOutsubmitTime() {
		return outsubmitTime;
	}

	public void setOutsubmitTime(String outsubmitTime) {
		this.outsubmitTime = outsubmitTime;
	}

	public String getInDeliveryTime() {
		return inDeliveryTime;
	}

	public void setInDeliveryTime(String inDeliveryTime) {
		this.inDeliveryTime = inDeliveryTime;
	}

	public String getOutDeliverTime() {
		return outDeliverTime;
	}

	public void setOutDeliverTime(String outDeliverTime) {
		this.outDeliverTime = outDeliverTime;
	}
	
	

	public long getDrLatency() {
		return drLatency;
	}

	public void setDrLatency(long drLatency) {
		this.drLatency = drLatency;
	}

	public long getSubmitLatency() {
		return submitLatency;
	}

	public void setSubmitLatency(long submitLatency) {
		this.submitLatency = submitLatency;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getCustomer() {
		return customer;
	}

	public void setCustomer(String customer) {
		this.customer = customer;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getInContentType() {
		return inContentType;
	}

	public void setInContentType(String inContentType) {
		this.inContentType = inContentType;
	}

	public String getOutContentType() {
		return outContentType;
	}

	public void setOutContentType(String outContentType) {
		this.outContentType = outContentType;
	}

	public String getInContent() {
		return inContent;
	}

	public void setInContent(String inContent) {
		this.inContent = inContent;
	}

	public String getOutContent() {
		return outContent;
	}

	public void setOutContent(String outContent) {
		this.outContent = outContent;
	}

	public String getInMsgSize() {
		return inMsgSize;
	}

	public void setInMsgSize(String inMsgSize) {
		this.inMsgSize = inMsgSize;
	}

	public String getOutMsgSize() {
		return outMsgSize;
	}

	public void setOutMsgSize(String outMsgSize) {
		this.outMsgSize = outMsgSize;
	}
	
	

	public String getInDRStatusCode() {
		return inDRStatusCode;
	}

	public void setInDRStatusCode(String inDRStatusCode) {
		this.inDRStatusCode = inDRStatusCode;
	}

	public String getInSubmitStatusCode() {
		return inSubmitStatusCode;
	}

	public void setInSubmitStatusCode(String inSubmitStatusCode) {
		this.inSubmitStatusCode = inSubmitStatusCode;
	}

	public String getOutDRStatusCode() {
		return outDRStatusCode;
	}

	public void setOutDRStatusCode(String outDRStatusCode) {
		this.outDRStatusCode = outDRStatusCode;
	}

	public String getOutSubmitStatusCode() {
		return outSubmitStatusCode;
	}

	public void setOutSubmitStatusCode(String outSubmitStatusCode) {
		this.outSubmitStatusCode = outSubmitStatusCode;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public String toString() {
		return "SMSLogging [id=" + id + ", CDRType=" + CDRType + ", InTransID="
				+ InTransID + ", OutTransID=" + OutTransID + ", InMsgID="
				+ InMsgID + ", OutMsgID=" + OutMsgID + ", OSsID=" + OSsID
				+ ", RSsID=" + RSsID + ", GmmsMsgType=" + GmmsMsgType
				+ ", MessageType=" + MessageType + ", MessageSize="
				+ MessageSize + ", ProtocolVersion=" + ProtocolVersion
				+ ", SenderAddrType=" + SenderAddrType + ", RecipientAddrType="
				+ RecipientAddrType + ", SenderAddresses=" + SenderAddresses
				+ ", RecipientAddresses=" + RecipientAddresses + ", TimeMark="
				+ TimeMark + ", ExpiryDate=" + ExpiryDate + ", DeliveryReport="
				+ DeliveryReport + ", Priority=" + Priority + ", ContentType="
				+ ContentType + ", textcontent=" + textcontent
				+ ", StatusCode=" + StatusCode + ", StatusText=" + StatusText
				+ ", InClientPull=" + InClientPull + ", OutClientPull="
				+ OutClientPull + ", DeliveryChannel=" + DeliveryChannel
				+ ", DateIn=" + DateIn + ", MilterActionCode="
				+ MilterActionCode + ", oOperator=" + oOperator
				+ ", rOperator=" + rOperator + ", ActionCode=" + ActionCode
				+ ", currentA2p=" + currentA2p + ", oA2p=" + oA2p + ", rA2p="
				+ rA2p + ", MsgID=" + MsgID + ", Split=" + Split
				+ ", msgRefNum=" + msgRefNum + ", totalSegments="
				+ totalSegments + ", segmentSeqNum=" + segmentSeqNum
				+ ", CDRTime=" + CDRTime + ", InCsm=" + InCsm
				+ ", ServiceTypeID=" + ServiceTypeID
				+ ", ScheduleDeliveryTime=" + ScheduleDeliveryTime
				+ ", MessageState=" + MessageState + ", insubmitTime="
				+ insubmitTime + ", outsubmitTime=" + outsubmitTime
				+ ", inDeliveryTime=" + inDeliveryTime + ", outDeliverTime="
				+ outDeliverTime + ", drLatency=" + drLatency + ", vendor="
				+ vendor + ", customer=" + customer + ", smsLoggingFields="
				+ smsLoggingFields + ", country=" + country
				+ ", inContentType=" + inContentType + ", outContentType="
				+ outContentType + ", inContent=" + inContent + ", outContent="
				+ outContent + ", inMsgSize=" + inMsgSize + ", outMsgSize="
				+ outMsgSize + ", submitLatency=" + submitLatency + "]";
	}

	
}

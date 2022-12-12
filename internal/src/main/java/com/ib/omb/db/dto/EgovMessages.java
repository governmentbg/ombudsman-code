package com.ib.omb.db.dto;


import static javax.persistence.GenerationType.SEQUENCE;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.ib.omb.system.OmbConstants;
import com.ib.system.db.PersistentEntity;


@Entity
@Table(name = "EGOV_MESSAGES")
public class EgovMessages  implements PersistentEntity {
	
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -5658606430363250381L;


	@SequenceGenerator(name = "EgovMessages", sequenceName = "SEQ_EGOV_MESSAGES", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "EgovMessages")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	
	@Column(name = "MSG_GUID")
	private String msgGuid;
	
	@Column(name = "SENDER_GUID")
	private String senderGuid;
	
	@Column(name = "SENDER_NAME")
	private String senderName;
	
	@Column(name = "SENDER_EIK")
	private String senderEik;
	
	@Column(name = "RECIPIENT_GUID")
	private String recepientGuid;
	
	@Column(name = "RECIPIENT_NAME")
	private String recepientName;
	
	@Column(name = "RECIPIENT_EIK")
	private String recepientEik;
	
	@Column(name = "MSG_TYPE")
	private String msgType;
	
	@Column(name = "MSG_DAT")
	private Date msgDate;
	
	@Column(name = "MSG_STATUS")
	private String msgStatus;
	
	@Column(name = "MSG_STATUS_DAT")
	private Date msgStatusDate;
	
	@Column(name = "MSG_REG_DAT")
	private Date msgRegDate;
	
	@Column(name = "MSG_COMMENT")
	private String 	msgComment;
	
	@Column(name = "MSG_URGENT")
	private Integer msgUrgent;
	
	@Column(name = "MSG_INOUT")
	private Integer msgInOut;
	
	@Column(name = "MSG_VERSION")
	private String msgVersion;
	
	@Column(name = "MSG_XML")
	private String 	msgXml;
	
	@Column(name = "PORED")
	private Integer pored;
	
	@Column(name = "NEXT_TRY")
	private Date nextTry;
	
	@Column(name = "HAS_MALWARE")
	private String hasMalware;
	
	@Column(name = "SOURCE")
	private String source;
	
	@Column(name = "REPLY_IDENT")
	private String replyIdent;
	
	@Column(name = "RECIPIENT_TYPE")
	private String recepientType;
	

	
	@Column(name = "MSG_RN")
	private String msgRn;
	
	@Column(name = "MSG_RN_DAT")
	private Date msgRnDate;
	
	@Column(name = "DOC_GUID")
	private String docGuid;
	
	@Column(name = "DOC_DAT")
	private Date docDate;
	
	@Column(name = "DOC_RN")
	private String docRn;
	
	@Column(name = "DOC_URI_REG")
	private String docUriReg;
	
	@Column(name = "DOC_URI_BATCH")
	private String docUriBtch;
	
	@Column(name = "DOC_VID")
	private String docVid;
	
	@Column(name = "DOC_SUBJECT")
	private String 	docSubject;
	
	@Column(name = "DOC_COMMENT")
	private String docComment;
	
	@Column(name = "DOC_SROK")
	private Date docSrok;
	
	@Column(name = "DOC_NASOCHEN")
	private String docNasochen;
	
	@Column(name = "PARENT_GUID")
	private String parrentGuid;
	
	@Column(name = "PARENT_RN")
	private String parrentRn;
	
	@Column(name = "PARENT_DAT")
	private Date parrentDate;
	
	@Column(name = "PARENT_URI_REG")
	private String parrentUriReg;
	
	@Column(name = "PARENT_URI_BATCH")
	private String parrentUriBatch;
	
	@Column(name = "COMM_STATUS")
	private Integer commStatus;
	
	@Column(name = "COMM_ERRROR")
	private String commError;
	
	@Column(name = "PRICHINA")
	private String prichina;
	
	@Column(name = "USER_CREATED")
	private Integer userCreated;
	
	
	/** */
	public EgovMessages() {
		super();
	}

	public Integer getCodeMainObject() {		
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_EGOVMESSAGE;
	}

	@Override
	public boolean isAuditable() { // TODO за момена няма да се журналира
		return false;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getMsgGuid() {
		return msgGuid;
	}

	public void setMsgGuid(String msgGuid) {
		this.msgGuid = msgGuid;
	}

	public String getSenderGuid() {
		return senderGuid;
	}

	public void setSenderGuid(String senderGuid) {
		this.senderGuid = senderGuid;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public String getSenderEik() {
		return senderEik;
	}

	public void setSenderEik(String senderEik) {
		this.senderEik = senderEik;
	}

	public String getRecepientGuid() {
		return recepientGuid;
	}

	public void setRecepientGuid(String recepientGuid) {
		this.recepientGuid = recepientGuid;
	}

	public String getRecepientName() {
		return recepientName;
	}

	public void setRecepientName(String recepientName) {
		this.recepientName = recepientName;
	}

	public String getRecepientEik() {
		return recepientEik;
	}

	public void setRecepientEik(String recepientEik) {
		this.recepientEik = recepientEik;
	}

	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public Date getMsgDate() {
		return msgDate;
	}

	public void setMsgDate(Date msgDate) {
		this.msgDate = msgDate;
	}

	public String getMsgStatus() {
		return msgStatus;
	}

	public void setMsgStatus(String msgStatus) {
		this.msgStatus = msgStatus;
	}

	public Date getMsgStatusDate() {
		return msgStatusDate;
	}

	public void setMsgStatusDate(Date msgStatusDate) {
		this.msgStatusDate = msgStatusDate;
	}

	public Date getMsgRegDate() {
		return msgRegDate;
	}

	public void setMsgRegDate(Date msgRegDate) {
		this.msgRegDate = msgRegDate;
	}

	public String getMsgComment() {
		return msgComment;
	}

	public void setMsgComment(String msgComment) {
		this.msgComment = msgComment;
	}

	public Integer getMsgUrgent() {
		return msgUrgent;
	}

	public void setMsgUrgent(Integer msgUrgent) {
		this.msgUrgent = msgUrgent;
	}

	public Integer getMsgInOut() {
		return msgInOut;
	}

	public void setMsgInOut(Integer msgInOut) {
		this.msgInOut = msgInOut;
	}

	public String getMsgVersion() {
		return msgVersion;
	}

	public void setMsgVersion(String msgVersion) {
		this.msgVersion = msgVersion;
	}

	public String getMsgXml() {
		return msgXml;
	}

	public void setMsgXml(String msgXml) {
		this.msgXml = msgXml;
	}

	public Integer getPored() {
		return pored;
	}

	public void setPored(Integer pored) {
		this.pored = pored;
	}

	public Date getNextTry() {
		return nextTry;
	}

	public void setNextTry(Date nextTry) {
		this.nextTry = nextTry;
	}

	public String getHasMalware() {
		return hasMalware;
	}

	public void setHasMalware(String hasMalware) {
		this.hasMalware = hasMalware;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getReplyIdent() {
		return replyIdent;
	}

	public void setReplyIdent(String replyIdent) {
		this.replyIdent = replyIdent;
	}

	public String getRecepientType() {
		return recepientType;
	}

	public void setRecepientType(String recepientType) {
		this.recepientType = recepientType;
	}

	public String getMsgRn() {
		return msgRn;
	}

	public void setMsgRn(String msgRn) {
		this.msgRn = msgRn;
	}

	public Date getMsgRnDate() {
		return msgRnDate;
	}

	public void setMsgRnDate(Date msgRnDate) {
		this.msgRnDate = msgRnDate;
	}

	public String getDocGuid() {
		return docGuid;
	}

	public void setDocGuid(String docGuid) {
		this.docGuid = docGuid;
	}

	public Date getDocDate() {
		return docDate;
	}

	public void setDocDate(Date docDate) {
		this.docDate = docDate;
	}

	public String getDocRn() {
		return docRn;
	}

	public void setDocRn(String docRn) {
		this.docRn = docRn;
	}

	public String getDocUriReg() {
		return docUriReg;
	}

	public void setDocUriReg(String docUriReg) {
		this.docUriReg = docUriReg;
	}

	public String getDocUriBtch() {
		return docUriBtch;
	}

	public void setDocUriBtch(String docUriBtch) {
		this.docUriBtch = docUriBtch;
	}

	public String getDocVid() {
		return docVid;
	}

	public void setDocVid(String docVid) {
		this.docVid = docVid;
	}

	public String getDocSubject() {
		return docSubject;
	}

	public void setDocSubject(String docSubject) {
		this.docSubject = docSubject;
	}

	public String getDocComment() {
		return docComment;
	}

	public void setDocComment(String docComment) {
		this.docComment = docComment;
	}

	public Date getDocSrok() {
		return docSrok;
	}

	public void setDocSrok(Date docSrok) {
		this.docSrok = docSrok;
	}

	public String getDocNasochen() {
		return docNasochen;
	}

	public void setDocNasochen(String docNasochen) {
		this.docNasochen = docNasochen;
	}

	public String getParrentGuid() {
		return parrentGuid;
	}

	public void setParrentGuid(String parrentGuid) {
		this.parrentGuid = parrentGuid;
	}

	public String getParrentRn() {
		return parrentRn;
	}

	public void setParrentRn(String parrentRn) {
		this.parrentRn = parrentRn;
	}

	public Date getParrentDate() {
		return parrentDate;
	}

	public void setParrentDate(Date parrentDate) {
		this.parrentDate = parrentDate;
	}

	public String getParrentUriReg() {
		return parrentUriReg;
	}

	public void setParrentUriReg(String parrentUriReg) {
		this.parrentUriReg = parrentUriReg;
	}

	public String getParrentUriBatch() {
		return parrentUriBatch;
	}

	public void setParrentUriBatch(String parrentUriBatch) {
		this.parrentUriBatch = parrentUriBatch;
	}

	public Integer getCommStatus() {
		return commStatus;
	}

	public void setCommStatus(Integer commStatus) {
		this.commStatus = commStatus;
	}

	public String getCommError() {
		return commError;
	}

	public void setCommError(String commError) {
		this.commError = commError;
	}

	public String getPrichina() {
		return prichina;
	}

	public void setPrichina(String prichina) {
		this.prichina = prichina;
	}

	public Integer getUserCreated() {
		return userCreated;
	}

	public void setUserCreated(Integer userCreated) {
		this.userCreated = userCreated;
	}

	
	
		
}
package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ib.omb.system.OmbConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Документ движение
 *
 * @author belev
 *
 * 
 */
@Entity
@Table(name = "DOC_DVIJ")
public class DocDvij extends TrackableEntity implements AuditExt{
	/**  */
	private static final long serialVersionUID = 4001758958319186700L;

	@SequenceGenerator(name = "DocDvij", sequenceName = "SEQ_DOC_DVIJ", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DocDvij")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;
	
	@Column(name = "DOC_ID", updatable = false)
	private Integer docId;	
	
	@Column(name = "EKZ_NOMER")
	@JournalAttr(label = "ekzNomer", defaultText = "Екземпляр")
	private Integer ekzNomer;		

	@Column(name = "CODE_REF")
	private Integer codeRef;
	
	@Column(name = "DVIJ_DATE")
	@JournalAttr(label = "dvijDate", defaultText = "Дата на предаване", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date dvijDate;

	@Column(name = "DVIJ_METHOD")
	@JournalAttr(label = "dvijMethod", defaultText = "Начин на предаване",classifID = "112")
	private Integer dvijMethod;
	
	@Column(name = "DVIJ_TEXT")
	@JournalAttr(label = "dvijText", defaultText = "Предаден на")
	private String dvijText;
	
	@Column(name = "DVIJ_EMAIL")
	@JournalAttr(label = "dvijEmail", defaultText = "Емейл")
	private String dvijEmail;

	@Column(name = "DVIJ_INFO")
	@JournalAttr(label = "dvijInfo", defaultText = "Доп. информация")
	private String dvijInfo;
	
	@Column(name = "FOR_REG_ID")
	@JournalAttr(label = "forRegid", defaultText = "За регистриране в ",classifID = ""+OmbConstants.CODE_CLASSIF_REGISTRATURI)
	private Integer forRegid;	
	
	@Column(name = "RETURN_TO_DATE")
	@JournalAttr(label = "returnToDate", defaultText = "Да се върне до", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date returnToDate;
	
	@Column(name = "OTHER_DOC_ID")
	@JournalAttr(label = "otherDocId", defaultText = "Друго ИД")
	private Integer otherDocId;
	
//	@Column(name = "CONFIRM_DATE")
//	private Date confirmDate;
	
	@Column(name = "MESSAGE_ID")
	@JournalAttr(label = "messageId", defaultText = "ИД на съобщение от СЕОС/ССЕВ")
	private Integer messageId;
	
	@Column(name = "STATUS")
	@JournalAttr(label = "status", defaultText = "Статус",classifID = "154")
	private Integer status;
	
	@Column(name = "STATUS_DATE")
	@JournalAttr(label = "statusDate", defaultText = "Дата на статус", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date statusDate;

	@Column(name = "STATUS_TEXT")
	@JournalAttr(label = "statusText", defaultText = "Статус (текст)")
	private String statusText;
	
	@Column(name = "RETURN_DATE")
	@JournalAttr(label = "returnDate", defaultText = "Дата на връщане", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date returnDate;
	
	@Column(name = "RETURN_CODE_REF")
	private Integer returnCodeRef;
	
	@Column(name = "RETURN_TEXT_REF")
	@JournalAttr(label = "returnTextRef", defaultText = "Върнат от")
	private String returnTextRef;
	
	@Column(name = "RETURN_METHOD")
	@JournalAttr(label = "returnMethod", defaultText = "Начин на връщане",classifID = "112")
	private Integer returnMethod;

	@Column(name = "RETURN_INFO")
	@JournalAttr(label = "returnInfo", defaultText = "Текст при връщане")
	private String returnInfo;
	
	@Column(name = "FROM_DVIJ_ID")
	private Integer fromDvijId;

	@Column(name = "jalba_id")
	private Integer jalbaId;

	
	@Transient
	private transient Integer dbCodeRef; // заради определянето на достъп да се пази стария
	
	
	//СЕОС и ССЕВ
	@Transient
	@JournalAttr(label = "uchastnikGuid", defaultText = "Участник СЕОС/ССЕВ - GUID")
	private String uchastnikGuid;	
	@Transient
	@JournalAttr(label = "uchastnikIdent", defaultText = "Участник СЕОС/ССЕВ - идентификатор")
	private String uchastnikIdent;
	@Transient	
	@JournalAttr(label = "uchastnikName", defaultText = "Участник СЕОС/ССЕВ - наименование")
	private String uchastnikName;
	@Transient
	@JournalAttr(label = "uchastnikType", defaultText = "Участник СЕОС/ССЕВ - тип")
	private String uchastnikType;
	
	
	
	
	

	/**  */
	public DocDvij() {
		super();
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getDocId() {
		return docId;
	}

	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	public Integer getEkzNomer() {
		return ekzNomer;
	}

	public void setEkzNomer(Integer ekzNomer) {
		this.ekzNomer = ekzNomer;
	}

	public Integer getCodeRef() {
		return codeRef;
	}

	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	public Date getDvijDate() {
		return dvijDate;
	}

	public void setDvijDate(Date dvijDate) {
		this.dvijDate = dvijDate;
	}

	public Integer getDvijMethod() {
		return dvijMethod;
	}

	public void setDvijMethod(Integer dvijMethod) {
		this.dvijMethod = dvijMethod;
	}

	public String getDvijText() {
		return dvijText;
	}

	public void setDvijText(String dvijText) {
		this.dvijText = dvijText;
	}

	public String getDvijEmail() {
		return dvijEmail;
	}

	public void setDvijEmail(String dvijEmail) {
		this.dvijEmail = dvijEmail;
	}

	public String getDvijInfo() {
		return dvijInfo;
	}

	public void setDvijInfo(String dvijInfo) {
		this.dvijInfo = dvijInfo;
	}

	public Integer getForRegid() {
		return forRegid;
	}

	public void setForRegid(Integer forRegid) {
		this.forRegid = forRegid;
	}

	public Date getReturnToDate() {
		return returnToDate;
	}

	public void setReturnToDate(Date returnToDate) {
		this.returnToDate = returnToDate;
	}

	public Integer getOtherDocId() {
		return otherDocId;
	}

	public void setOtherDocId(Integer otherDocId) {
		this.otherDocId = otherDocId;
	}

//	public Date getConfirmDate() {
//		return confirmDate;
//	}
//
//	public void setConfirmDate(Date confirmDate) {
//		this.confirmDate = confirmDate;
//	}

	

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Date getStatusDate() {
		return statusDate;
	}

	public void setStatusDate(Date statusDate) {
		this.statusDate = statusDate;
	}

	public String getStatusText() {
		return statusText;
	}

	public void setStatusText(String statusText) {
		this.statusText = statusText;
	}

	public Date getReturnDate() {
		return returnDate;
	}

	public void setReturnDate(Date returnDate) {
		this.returnDate = returnDate;
	}

	public Integer getReturnCodeRef() {
		return returnCodeRef;
	}

	public void setReturnCodeRef(Integer returnCodeRef) {
		this.returnCodeRef = returnCodeRef;
	}

	public String getReturnTextRef() {
		return returnTextRef;
	}

	public void setReturnTextRef(String returnTextRef) {
		this.returnTextRef = returnTextRef;
	}

	public Integer getReturnMethod() {
		return returnMethod;
	}

	public void setReturnMethod(Integer returnMethod) {
		this.returnMethod = returnMethod;
	}

	public String getReturnInfo() {
		return returnInfo;
	}

	public void setReturnInfo(String returnInfo) {
		this.returnInfo = returnInfo;
	}

	public Integer getFromDvijId() {
		return fromDvijId;
	}

	public void setFromDvijId(Integer fromDvijId) {
		this.fromDvijId = fromDvijId;
	}

	public Integer getDbCodeRef() {
		return this.dbCodeRef;
	}

	public void setDbCodeRef(Integer dbCodeRef) {
		this.dbCodeRef = dbCodeRef;
	}

	public String getUchastnikGuid() {
		return uchastnikGuid;
	}

	public void setUchastnikGuid(String uchastnikGuid) {
		this.uchastnikGuid = uchastnikGuid;
	}

	

	public String getUchastnikName() {
		return uchastnikName;
	}

	public void setUchastnikName(String uchastnikName) {
		this.uchastnikName = uchastnikName;
	}

	public String getUchastnikIdent() {
		return uchastnikIdent;
	}

	public void setUchastnikIdent(String uchastnikIdent) {
		this.uchastnikIdent = uchastnikIdent;
	}
	
	public String getUchastnikType() {
		return uchastnikType;
	}

	public void setUchastnikType(String uchastnikType) {
		this.uchastnikType = uchastnikType;
	}

	public Integer getMessageId() {
		return messageId;
	}

	public void setMessageId(Integer messageId) {
		this.messageId = messageId;
	}

	@Override
	public String getIdentInfo() throws DbErrorException {
		return dvijText;
	}
	
	
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new  SystemJournal();				
		dj.setCodeObject(getCodeMainObject());
		dj.setIdObject(getId());
		dj.setIdentObject(getIdentInfo());
		dj.setDateAction(new Date());
		if (docId != null) {
			dj.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
			dj.setJoinedIdObject1(docId);
		}
		return dj;
	}

	/** @return the jalbaId */
	public Integer getJalbaId() {
		return this.jalbaId;
	}
	/** @param jalbaId the jalbaId to set */
	public void setJalbaId(Integer jalbaId) {
		this.jalbaId = jalbaId;
	}
}
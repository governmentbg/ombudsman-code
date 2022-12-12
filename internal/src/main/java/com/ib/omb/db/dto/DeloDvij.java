package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

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
 * Преписка движение
 *
 * @author belev
 */
@Entity
@Table(name = "DELO_DVIJ")
public class DeloDvij extends TrackableEntity implements Cloneable,AuditExt {
	/**  */
	private static final long serialVersionUID = 991824739656273001L;

	@SequenceGenerator(name = "DeloDvij", sequenceName = "SEQ_DELO_DVIJ", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DeloDvij")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "DELO_ID", updatable = false)
	private Integer deloId;
	
	@Column(name = "TOM_NOMER")
	@JournalAttr(label = "tomNomer", defaultText = "Номер на том")
	private Integer tomNomer;

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
	
	

	@Column(name = "DVIJ_INFO")
	@JournalAttr(label = "dvijInfo", defaultText = "Доп. информация")
	private String dvijInfo;
	
	
	@Column(name = "RETURN_TO_DATE")
	@JournalAttr(label = "returnToDate", defaultText = "Да се върне до", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date returnToDate;
	
	
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

	@Transient
	private transient Integer dbCodeRef; // заради определянето на достъп да се пази стария
	@Transient
	private transient Date dbDvijDate; // заради свързаните движения

	/**  */
	public DeloDvij() {
		super();
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DVIJ;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
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

	
	public String getDvijInfo() {
		return dvijInfo;
	}

	public void setDvijInfo(String dvijInfo) {
		this.dvijInfo = dvijInfo;
	}

	
	public Date getReturnToDate() {
		return returnToDate;
	}

	public void setReturnToDate(Date returnToDate) {
		this.returnToDate = returnToDate;
	}

	
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

	public Integer getDeloId() {
		return deloId;
	}

	public void setDeloId(Integer deloId) {
		this.deloId = deloId;
	}

	public Integer getTomNomer() {
		return tomNomer;
	}

	public void setTomNomer(Integer tomNomer) {
		this.tomNomer = tomNomer;
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

	public DeloDvij clone(){  
		try {
			return (DeloDvij)super.clone();
		} catch (CloneNotSupportedException e) {
			return new DeloDvij();
		}  
	}
	
	@Override
	public String getIdentInfo() throws DbErrorException {
		return dvijText;
	}
	

	public Date getDbDvijDate() {
		return this.dbDvijDate;
	}
	public void setDbDvijDate(Date dbDvijDate) {
		this.dbDvijDate = dbDvijDate;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new  SystemJournal();				
		dj.setCodeObject(getCodeMainObject());
		dj.setIdObject(getId());
		dj.setIdentObject(getIdentInfo());
		dj.setDateAction(new Date());
		if (deloId != null) {
			dj.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO);
			dj.setJoinedIdObject1(deloId);
		}
		return dj;
	}
	

}
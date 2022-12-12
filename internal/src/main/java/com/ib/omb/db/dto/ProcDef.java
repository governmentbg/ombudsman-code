package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ib.indexui.system.Constants;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Дефиниция на Процедура
 *
 * @author belev
 */
@Entity
@Table(name = "PROC_DEF")
public class ProcDef extends TrackableEntity implements AuditExt {

	/**  */
	private static final long serialVersionUID = -5762167874154817342L;

	@SequenceGenerator(name = "ProcDef", sequenceName = "SEQ_PROC_DEF", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "ProcDef")
	@Column(name = "DEF_ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "REGISTRATURA_ID")
	@JournalAttr(label = "registraturaId", defaultText = "Регистратура", classifID = "" + OmbConstants.CODE_CLASSIF_REGISTRATURI)
	private Integer registraturaId;

	@Column(name = "PROC_NAME")
	@JournalAttr(label = "procName", defaultText = "Наименование")
	private String procName;

	@Column(name = "PROC_INFO")
	@JournalAttr(label = "procInfo", defaultText = "Описание")
	private String procInfo;

	@Column(name = "SROK_DAYS")
	@JournalAttr(label = "srokDays", defaultText = "Срок в дни")
	private Integer srokDays;

	@Column(name = "WORK_DAYS_ONLY")
	@JournalAttr(label = "workDaysOnly", defaultText = "Само работни дни", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	private Integer workDaysOnly;

	@Column(name = "INSTRUCTIONS")
	@JournalAttr(label = "instructions", defaultText = "Указания за изпълнение")
	private String instructions;

	@Column(name = "STATUS")
	@JournalAttr(label = "status", defaultText = "Статус", classifID = "" + OmbConstants.CODE_CLASSIF_PROC_DEF_STAT)
	private Integer status;

	@Column(name = "URI_PROC")
	@JournalAttr(label = "uriProc", defaultText = "УРИ")
	private String uriProc;

	@Column(name = "DOC_TYPE")
	@JournalAttr(label="docType",defaultText = "Тип иницииращ документ",classifID="129")
	private Integer docType;

	@Column(name = "CODE_REF")
	@JournalAttr(label = "codeRef", defaultText = "Отговорен - Служител", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private Integer codeRef;

	@Column(name = "ZVENO")
	@JournalAttr(label = "zveno", defaultText = "Отговорен - Звено", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private Integer zveno;

	@Column(name = "EMPL_POSITION")
	@JournalAttr(label = "emplPosition", defaultText = "Отговорен - Длъжност", classifID = "" + Constants.CODE_CLASSIF_POSITION)
	private Integer emplPosition;

	@Column(name = "BUSINESS_ROLE")
	@JournalAttr(label = "businessRole", defaultText = "Отговорен - Бизнес роля", classifID = "" + OmbConstants.CODE_CLASSIF_PROC_BUSINESS_ROLE)
	private Integer businessRole;

	@Transient
	private transient Integer dbStatus;

	/** */
	public ProcDef() {
		super();
	}

	/** @return the businessRole */
	public Integer getBusinessRole() {
		return this.businessRole;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_DEF;
	}

	/** @return the codeRef */
	public Integer getCodeRef() {
		return this.codeRef;
	}

	/** @return the dbStatus */
	public Integer getDbStatus() {
		return this.dbStatus;
	}

	/** @return the emplPosition */
	public Integer getEmplPosition() {
		return this.emplPosition;
	}

	/** @return the id */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		return this.procName;
	}

	/** @return the instructions */
	public String getInstructions() {
		return this.instructions;
	}

	/** @return the procInfo */
	public String getProcInfo() {
		return this.procInfo;
	}

	/** @return the procName */
	public String getProcName() {
		return this.procName;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @return the srokDays */
	public Integer getSrokDays() {
		return this.srokDays;
	}

	/** @return the status */
	public Integer getStatus() {
		return this.status;
	}

	/** @return the uriProc */
	public String getUriProc() {
		return this.uriProc;
	}

	/** @return the workDaysOnly */
	public Integer getWorkDaysOnly() {
		return this.workDaysOnly;
	}

	/** @return the zveno */
	public Integer getZveno() {
		return this.zveno;
	}

	/** @param businessRole the businessRole to set */
	public void setBusinessRole(Integer businessRole) {
		this.businessRole = businessRole;
	}

	/** @param codeRef the codeRef to set */
	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	/** @param dbStatus the dbStatus to set */
	public void setDbStatus(Integer dbStatus) {
		this.dbStatus = dbStatus;
	}

	/** @param emplPosition the emplPosition to set */
	public void setEmplPosition(Integer emplPosition) {
		this.emplPosition = emplPosition;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param instructions the instructions to set */
	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	/** @param procInfo the procInfo to set */
	public void setProcInfo(String procInfo) {
		this.procInfo = procInfo;
	}

	/** @param procName the procName to set */
	public void setProcName(String procName) {
		this.procName = procName;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** @param srokDays the srokDays to set */
	public void setSrokDays(Integer srokDays) {
		this.srokDays = srokDays;
	}

	/** @param status the status to set */
	public void setStatus(Integer status) {
		this.status = status;
	}

	/** @param uriProc the uriProc to set */
	public void setUriProc(String uriProc) {
		this.uriProc = uriProc;
	}

	/** @param workDaysOnly the workDaysOnly to set */
	public void setWorkDaysOnly(Integer workDaysOnly) {
		this.workDaysOnly = workDaysOnly;
	}

	/** @param zveno the zveno to set */
	public void setZveno(Integer zveno) {
		this.zveno = zveno;
	}
	
	/** @return the docType */
	public Integer getDocType() {
		return this.docType;
	}

	/** @param docType the docType to set */
	public void setDocType(Integer docType) {
		this.docType = docType;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new SystemJournal();
		dj.setCodeObject(getCodeMainObject());
		dj.setIdObject(getId());
		dj.setIdentObject(getIdentInfo());
		return dj;
	}
}
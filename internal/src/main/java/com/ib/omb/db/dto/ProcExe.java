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

import com.ib.indexui.system.Constants;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Изпълнение на Процедура
 *
 * @author belev
 */
@Entity
@Table(name = "PROC_EXE")
public class ProcExe extends TrackableEntity implements AuditExt {

	/**  */
	private static final long serialVersionUID = 3276906560020305603L;

	@JournalAttr(label = "id", defaultText = "Номер")
	@SequenceGenerator(name = "ProcExe", sequenceName = "SEQ_PROC_EXE", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "ProcExe")
	@Column(name = "EXE_ID", unique = true, nullable = false)
	private Integer id;

	@JournalAttr(label = "defId", defaultText = "Дефиниция", classifID = "" + OmbConstants.CODE_CLASSIF_PROCEDURI)
	@Column(name = "DEF_ID", updatable = false)
	private Integer defId;

	@JournalAttr(label = "registraturaId", defaultText = "Регистратура", classifID = "" + OmbConstants.CODE_CLASSIF_REGISTRATURI)
	@Column(name = "REGISTRATURA_ID", updatable = false)
	private Integer registraturaId;

	@JournalAttr(label = "procName", defaultText = "Наименование")
	@Column(name = "PROC_NAME")
	private String procName;

	@JournalAttr(label = "procInfo", defaultText = "Описание")
	@Column(name = "PROC_INFO")
	private String procInfo;

	@JournalAttr(label = "srokDays", defaultText = "Срок в дни")
	@Column(name = "SROK_DAYS")
	private Integer srokDays;

	@JournalAttr(label = "workDaysOnly", defaultText = "Само в раб. дни", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "WORK_DAYS_ONLY")
	private Integer workDaysOnly;

	@JournalAttr(label = "instructions", defaultText = "Указания за изпълнение")
	@Column(name = "INSTRUCTIONS")
	private String instructions;

	@JournalAttr(label = "docId", defaultText = "Инициращ документ", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	@Column(name = "DOC_ID")
	private Integer docId;

	@JournalAttr(label = "status", defaultText = "Статус", classifID = "" + OmbConstants.CODE_CLASSIF_PROC_STAT)
	@Column(name = "STATUS")
	private Integer status;

	@JournalAttr(label = "codeRef", defaultText = "Отговорен за изпълнението", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	@Column(name = "CODE_REF")
	private Integer codeRef;

	@JournalAttr(label = "srokDate", defaultText = "Срок")
	@Column(name = "SROK_DATE")
	private Date srokDate;

	@JournalAttr(label = "beginDate", defaultText = "Начало")
	@Column(name = "BEGIN_DATE")
	private Date beginDate;

	@JournalAttr(label = "endDate", defaultText = "Край")
	@Column(name = "END_DATE")
	private Date endDate;

	@JournalAttr(label = "comments", defaultText = "Забележка по изпълнението")
	@Column(name = "COMMENTS")
	private String comments;

	@JournalAttr(label = "stopReason", defaultText = "Причина за прекратяване")
	@Column(name = "STOP_REASON")
	private String stopReason;

	@Transient
	private Integer docProcessed; // тука ще се вдигне ако при стартиране на нова процедура и ако има регистрирани задачи
									// документа е станал обработен. което ще се използва само за входящ докумен.

	/** */
	public ProcExe() {
		super();
	}

	/** @return the beginDate */
	public Date getBeginDate() {
		return this.beginDate;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE;
	}

	/** @return the codeRef */
	public Integer getCodeRef() {
		return this.codeRef;
	}

	/** @return the comments */
	public String getComments() {
		return this.comments;
	}

	/** @return the defId */
	public Integer getDefId() {
		return this.defId;
	}

	/** @return the docId */
	public Integer getDocId() {
		return this.docId;
	}

	/** @return the docProcessed */
	public Integer getDocProcessed() {
		return this.docProcessed;
	}

	/** @return the endDate */
	public Date getEndDate() {
		return this.endDate;
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

	/** @return the srokDate */
	public Date getSrokDate() {
		return this.srokDate;
	}

	/** @return the srokDays */
	public Integer getSrokDays() {
		return this.srokDays;
	}

	/** @return the status */
	public Integer getStatus() {
		return this.status;
	}

	/** @return the stopReason */
	public String getStopReason() {
		return this.stopReason;
	}

	/** @return the workDaysOnly */
	public Integer getWorkDaysOnly() {
		return this.workDaysOnly;
	}

	/** @param beginDate the beginDate to set */
	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}

	/** @param codeRef the codeRef to set */
	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	/** @param comments the comments to set */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/** @param defId the defId to set */
	public void setDefId(Integer defId) {
		this.defId = defId;
	}

	/** @param docId the docId to set */
	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	/** @param docProcessed the docProcessed to set */
	public void setDocProcessed(Integer docProcessed) {
		this.docProcessed = docProcessed;
	}

	/** @param endDate the endDate to set */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
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

	/** @param srokDate the srokDate to set */
	public void setSrokDate(Date srokDate) {
		this.srokDate = srokDate;
	}

	/** @param srokDays the srokDays to set */
	public void setSrokDays(Integer srokDays) {
		this.srokDays = srokDays;
	}

	/** @param status the status to set */
	public void setStatus(Integer status) {
		this.status = status;
	}

	/** @param stopReason the stopReason to set */
	public void setStopReason(String stopReason) {
		this.stopReason = stopReason;
	}

	/** @param workDaysOnly the workDaysOnly to set */
	public void setWorkDaysOnly(Integer workDaysOnly) {
		this.workDaysOnly = workDaysOnly;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		return journal;
	}
}
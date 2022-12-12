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
 * Изпълнение на етап от процедура
 *
 * @author belev
 */
@Entity
@Table(name = "PROC_EXE_ETAP")
public class ProcExeEtap extends TrackableEntity implements AuditExt {

	/**  */
	private static final long serialVersionUID = 2150363789278993158L;

	@JournalAttr(label = "id", defaultText = "ИД Етап")
	@SequenceGenerator(name = "ProcExeEtap", sequenceName = "SEQ_PROC_EXE_ETAP", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "ProcExeEtap")
	@Column(name = "EXE_ETAP_ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "EXE_ID", updatable = false)
	private Integer exeId;

	@JournalAttr(label = "nomer", defaultText = "Ном. Етап")
	@Column(name = "NOMER")
	private Integer nomer;

	@JournalAttr(label = "etapName", defaultText = "Наименование")
	@Column(name = "ETAP_NAME")
	private String etapName;

	@JournalAttr(label = "etapInfo", defaultText = "Описание")
	@Column(name = "ETAP_INFO")
	private String etapInfo;

	@JournalAttr(label = "conditional", defaultText = "Условен", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "CONDITIONAL")
	private Integer conditional;

	@JournalAttr(label = "srokDays", defaultText = "Срок в дни")
	@Column(name = "SROK_DAYS")
	private Integer srokDays;

	@JournalAttr(label = "srokHours", defaultText = "Срок в часове")
	@Column(name = "SROK_HOURS")
	private Integer srokHours;

	@JournalAttr(label = "instructions", defaultText = "Указания за изпълнение")
	@Column(name = "INSTRUCTIONS")
	private String instructions;

	@JournalAttr(label = "exeEtapIdPrev", defaultText = "ИД предходен етап")
	@Column(name = "EXE_ETAP_ID_PREV")
	private Integer exeEtapIdPrev;

	@JournalAttr(label = "status", defaultText = "Статус", classifID = "" + OmbConstants.CODE_CLASSIF_ETAP_STAT)
	@Column(name = "STATUS")
	private Integer status;

	@JournalAttr(label = "codeRef", defaultText = "Контролиращ", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
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

	@JournalAttr(label = "branchPath", defaultText = "Системен път от разклонение")
	@Column(name = "BRANCH_PATH")
	private String branchPath;

	@JournalAttr(label = "docId", defaultText = "Стартиращ документ", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	@Column(name = "DOC_ID")
	private Integer docId;

//	[0]-id; [1]-nomer; [2]-etapName
	@Transient
	private transient Object[] prev;

//	[0]-defEtapId; [1]-isMerge
	@Transient
	private transient Object[] defEtapData;

	/** */
	public ProcExeEtap() {
		super();
	}

	/** @return the beginDate */
	public Date getBeginDate() {
		return this.beginDate;
	}

	/** @return the branchPath */
	public String getBranchPath() {
		return this.branchPath;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE_ETAP;
	}

	/** @return the codeRef */
	public Integer getCodeRef() {
		return this.codeRef;
	}

	/** @return the comments */
	public String getComments() {
		return this.comments;
	}

	/** @return the conditional */
	public Integer getConditional() {
		return this.conditional;
	}

	/** @return the defEtapData */
	public Object[] getDefEtapData() {
		return this.defEtapData;
	}

	/** @return the docId */
	public Integer getDocId() {
		return this.docId;
	}

	/** @return the endDate */
	public Date getEndDate() {
		return this.endDate;
	}

	/** @return the etapInfo */
	public String getEtapInfo() {
		return this.etapInfo;
	}

	/** @return the etapName */
	public String getEtapName() {
		return this.etapName;
	}

	/** @return the exeEtapIdPrev */
	public Integer getExeEtapIdPrev() {
		return this.exeEtapIdPrev;
	}

	/** @return the exeId */
	public Integer getExeId() {
		return this.exeId;
	}

	/** @return the id */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		return "ИД Изп.Процедура: " + this.exeId + ", Етап № " + this.nomer + " " + this.etapName;
	}

	/** @return the instructions */
	public String getInstructions() {
		return this.instructions;
	}

	/** @return the nomer */
	public Integer getNomer() {
		return this.nomer;
	}

	/** @return the prev */
	public Object[] getPrev() {
		return this.prev;
	}

	/** @return the srokDate */
	public Date getSrokDate() {
		return this.srokDate;
	}

	/** @return the srokDays */
	public Integer getSrokDays() {
		return this.srokDays;
	}

	/** @return the srokHours */
	public Integer getSrokHours() {
		return this.srokHours;
	}

	/** @return the status */
	public Integer getStatus() {
		return this.status;
	}

	/** @param beginDate the beginDate to set */
	public void setBeginDate(Date beginDate) {
		this.beginDate = beginDate;
	}

	/** @param branchPath the branchPath to set */
	public void setBranchPath(String branchPath) {
		this.branchPath = branchPath;
	}

	/** @param codeRef the codeRef to set */
	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	/** @param comments the comments to set */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/** @param conditional the conditional to set */
	public void setConditional(Integer conditional) {
		this.conditional = conditional;
	}

	/** @param defEtapData the defEtapData to set */
	public void setDefEtapData(Object[] defEtapData) {
		this.defEtapData = defEtapData;
	}

	/** @param docId the docId to set */
	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	/** @param endDate the endDate to set */
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	/** @param etapInfo the etapInfo to set */
	public void setEtapInfo(String etapInfo) {
		this.etapInfo = etapInfo;
	}

	/** @param etapName the etapName to set */
	public void setEtapName(String etapName) {
		this.etapName = etapName;
	}

	/** @param exeEtapIdPrev the exeEtapIdPrev to set */
	public void setExeEtapIdPrev(Integer exeEtapIdPrev) {
		this.exeEtapIdPrev = exeEtapIdPrev;
	}

	/** @param exeId the exeId to set */
	public void setExeId(Integer exeId) {
		this.exeId = exeId;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param instructions the instructions to set */
	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	/** @param nomer the nomer to set */
	public void setNomer(Integer nomer) {
		this.nomer = nomer;
	}

	/** @param prev the prev to set */
	public void setPrev(Object[] prev) {
		this.prev = prev;
	}

	/** @param srokDate the srokDate to set */
	public void setSrokDate(Date srokDate) {
		this.srokDate = srokDate;
	}

	/** @param srokDays the srokDays to set */
	public void setSrokDays(Integer srokDays) {
		this.srokDays = srokDays;
	}

	/** @param srokHours the srokHours to set */
	public void setSrokHours(Integer srokHours) {
		this.srokHours = srokHours;
	}

	/** @param status the status to set */
	public void setStatus(Integer status) {
		this.status = status;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE);
		journal.setJoinedIdObject1(this.exeId);

		return journal;
	}
}
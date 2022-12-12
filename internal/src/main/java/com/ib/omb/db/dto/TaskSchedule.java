package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
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
 * Дефиниция на периодична задача
 *
 * @author belev
 */
@Entity
@Table(name = "TASK_SCHEDULE")
public class TaskSchedule extends TrackableEntity implements AuditExt {

	/**  */
	private static final long serialVersionUID = -4515290700380904096L;

	@SequenceGenerator(name = "TaskSchedule", sequenceName = "SEQ_TASK_SCHEDULE", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "TaskSchedule")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@JournalAttr(label = "taskType", defaultText = "Вид задача", classifID = "" + OmbConstants.CODE_CLASSIF_TASK_VID)
	@Column(name = "TASK_TYPE")
	private Integer taskType;

	@JournalAttr(label = "taskInfo", defaultText = "Допълнителна информация")
	@Column(name = "TASK_INFO")
	private String taskInfo;

	@JournalAttr(label = "taskSrok", defaultText = "Срок за изпълнение (в дни)")
	@Column(name = "TASK_SROK")
	private Integer taskSrok;

	@JournalAttr(label = "regPeriod", defaultText = "Вид периодичност", classifID = "" + OmbConstants.CODE_CLASSIF_TASK_PERIOD)
	@Column(name = "REG_PERIOD")
	private Integer regPeriod;

	@JournalAttr(label = "regInterval", defaultText = "Интервал")
	@Column(name = "REG_INTERVAL")
	private Integer regInterval;

	@JournalAttr(label = "regDay", defaultText = "Ден на регистрация")
	@Column(name = "REG_DAY")
	private Integer regDay;

	@JournalAttr(label = "regMonth", defaultText = "Месец на регистрация")
	@Column(name = "REG_MONTH")
	private Integer regMonth;

	@JournalAttr(label = "valid", defaultText = "Активна", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "VALID")
	private Integer valid;

	@JournalAttr(label = "validFrom", defaultText = "Валидност от")
	@Temporal(TemporalType.DATE)
	@Column(name = "VALID_FROM")
	private Date validFrom;

	@JournalAttr(label = "validTo", defaultText = "Валидност до")
	@Temporal(TemporalType.DATE)
	@Column(name = "VALID_TO")
	private Date validTo;

	@JournalAttr(label = "workDaysOnly", defaultText = "Задачата се възлага само за работни дни", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "WORK_DAYS_ONLY")
	private Integer workDaysOnly;

	@JournalAttr(label = "lastRegDate", defaultText = "Дата на последна регистрация")
	@Temporal(TemporalType.DATE)
	@Column(name = "LAST_REG_DATE")
	private Date lastRegDate;

	@JournalAttr(label = "nextRegDate", defaultText = "Дата за следваща регистрация")
	@Temporal(TemporalType.DATE)
	@Column(name = "NEXT_REG_DATE")
	private Date nextRegDate;

	@JournalAttr(label = "codeAssign", defaultText = "Възложител", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	@Column(name = "CODE_ASSIGN")
	private Integer codeAssign;

	@JournalAttr(label = "codeControl", defaultText = "Контролиращ", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	@Column(name = "CODE_CONTROL")
	private Integer codeControl;

	@JournalAttr(label = "codeExec", defaultText = "Изпълнител", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	@Column(name = "CODE_EXEC")
	private Integer codeExec;

	@JournalAttr(label = "zveno", defaultText = "Звено на изпълнителя на задачата", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	@Column(name = "ZVENO")
	private Integer zveno;

	@JournalAttr(label = "emplPosition", defaultText = "Длъжност на изпълнителя на задачата", classifID = "" + Constants.CODE_CLASSIF_POSITION)
	@Column(name = "EMPL_POSITION")
	private Integer emplPosition;

	@JournalAttr(label = "docRequired", defaultText = "Документ при приключване", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "DOC_REQUIRED")
	private Integer docRequired;

	@JournalAttr(label = "docId", defaultText = "Документ на задача", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	@Column(name = "DOC_ID")
	private Integer docId;

	@Transient
	private transient Integer	dbRegPeriod;
	@Transient
	private transient Integer	dbValid;

	@Transient
	private transient Integer	dbRegInterval;
	@Transient
	private transient Integer	dbRegDay;
	@Transient
	private transient Integer	dbRegMonth;

	/**  */
	public TaskSchedule() {
		super();
	}

	/** @return the codeAssign */
	public Integer getCodeAssign() {
		return this.codeAssign;
	}

	/** @return the codeControl */
	public Integer getCodeControl() {
		return this.codeControl;
	}

	/** @return the codeExec */
	public Integer getCodeExec() {
		return this.codeExec;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK_SCHEDULE;
	}

	/** @return the dbRegDay */
	public Integer getDbRegDay() {
		return this.dbRegDay;
	}

	/** @return the dbRegInterval */
	public Integer getDbRegInterval() {
		return this.dbRegInterval;
	}

	/** @return the dbRegMonth */
	public Integer getDbRegMonth() {
		return this.dbRegMonth;
	}

	/** @return the dbRegPeriod */
	public Integer getDbRegPeriod() {
		return this.dbRegPeriod;
	}

	/** @return the dbValid */
	public Integer getDbValid() {
		return this.dbValid;
	}

	/** @return the docId */
	public Integer getDocId() {
		return this.docId;
	}

	/** @return the docRequired */
	public Integer getDocRequired() {
		return this.docRequired;
	}

	/** @return the emplPosition */
	public Integer getEmplPosition() {
		return this.emplPosition;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the lastRegDate */
	public Date getLastRegDate() {
		return this.lastRegDate;
	}

	/** @return the nextRegDate */
	public Date getNextRegDate() {
		return this.nextRegDate;
	}

	/** @return the regDay */
	public Integer getRegDay() {
		return this.regDay;
	}

	/** @return the regInterval */
	public Integer getRegInterval() {
		return this.regInterval;
	}

	/** @return the regMonth */
	public Integer getRegMonth() {
		return this.regMonth;
	}

	/** @return the regPeriod */
	public Integer getRegPeriod() {
		return this.regPeriod;
	}

	/** @return the taskInfo */
	public String getTaskInfo() {
		return this.taskInfo;
	}

	/** @return the taskSrok */
	public Integer getTaskSrok() {
		return this.taskSrok;
	}

	/** @return the taskType */
	public Integer getTaskType() {
		return this.taskType;
	}

	/** @return the valid */
	public Integer getValid() {
		return this.valid;
	}

	/** @return the validFrom */
	public Date getValidFrom() {
		return this.validFrom;
	}

	/** @return the validTo */
	public Date getValidTo() {
		return this.validTo;
	}

	/** @return the workDaysOnly */
	public Integer getWorkDaysOnly() {
		return this.workDaysOnly;
	}

	/** @return the zveno */
	public Integer getZveno() {
		return this.zveno;
	}

	/** @param codeAssign the codeAssign to set */
	public void setCodeAssign(Integer codeAssign) {
		this.codeAssign = codeAssign;
	}

	/** @param codeControl the codeControl to set */
	public void setCodeControl(Integer codeControl) {
		this.codeControl = codeControl;
	}

	/** @param codeExec the codeExec to set */
	public void setCodeExec(Integer codeExec) {
		this.codeExec = codeExec;
	}

	/** @param dbRegDay the dbRegDay to set */
	public void setDbRegDay(Integer dbRegDay) {
		this.dbRegDay = dbRegDay;
	}

	/** @param dbRegInterval the dbRegInterval to set */
	public void setDbRegInterval(Integer dbRegInterval) {
		this.dbRegInterval = dbRegInterval;
	}

	/** @param dbRegMonth the dbRegMonth to set */
	public void setDbRegMonth(Integer dbRegMonth) {
		this.dbRegMonth = dbRegMonth;
	}

	/** @param dbRegPeriod the dbRegPeriod to set */
	public void setDbRegPeriod(Integer dbRegPeriod) {
		this.dbRegPeriod = dbRegPeriod;
	}

	/** @param dbValid the dbValid to set */
	public void setDbValid(Integer dbValid) {
		this.dbValid = dbValid;
	}

	/** @param docId the docId to set */
	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	/** @param docRequired the docRequired to set */
	public void setDocRequired(Integer docRequired) {
		this.docRequired = docRequired;
	}

	/** @param emplPosition the emplPosition to set */
	public void setEmplPosition(Integer emplPosition) {
		this.emplPosition = emplPosition;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param lastRegDate the lastRegDate to set */
	public void setLastRegDate(Date lastRegDate) {
		this.lastRegDate = lastRegDate;
	}

	/** @param nextRegDate the nextRegDate to set */
	public void setNextRegDate(Date nextRegDate) {
		this.nextRegDate = nextRegDate;
	}

	/** @param regDay the regDay to set */
	public void setRegDay(Integer regDay) {
		this.regDay = regDay;
	}

	/** @param regInterval the regInterval to set */
	public void setRegInterval(Integer regInterval) {
		this.regInterval = regInterval;
	}

	/** @param regMonth the regMonth to set */
	public void setRegMonth(Integer regMonth) {
		this.regMonth = regMonth;
	}

	/** @param regPeriod the regPeriod to set */
	public void setRegPeriod(Integer regPeriod) {
		this.regPeriod = regPeriod;
	}

	/** @param taskInfo the taskInfo to set */
	public void setTaskInfo(String taskInfo) {
		this.taskInfo = taskInfo;
	}

	/** @param taskSrok the taskSrok to set */
	public void setTaskSrok(Integer taskSrok) {
		this.taskSrok = taskSrok;
	}

	/** @param taskType the taskType to set */
	public void setTaskType(Integer taskType) {
		this.taskType = taskType;
	}

	/** @param valid the valid to set */
	public void setValid(Integer valid) {
		this.valid = valid;
	}

	/** @param validFrom the validFrom to set */
	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}

	/** @param validTo the validTo to set */
	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}

	/** @param workDaysOnly the workDaysOnly to set */
	public void setWorkDaysOnly(Integer workDaysOnly) {
		this.workDaysOnly = workDaysOnly;
	}

	/** @param zveno the zveno to set */
	public void setZveno(Integer zveno) {
		this.zveno = zveno;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		return journal;
	}

}
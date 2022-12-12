package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.ib.omb.system.OmbConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Задача
 *
 * @author belev
 */
@Entity
@Table(name = "TASK")
//@EntityListeners(TaskEntityListener.class)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Task extends TrackableEntity implements FullTextIndexedEntity,AuditExt{
	/**  */
	private static final long serialVersionUID = 8811578732792908654L;

	@SequenceGenerator(name = "Task", sequenceName = "SEQ_TASK", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "Task")
	@Column(name = "TASK_ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "REGISTRATURA_ID", updatable = false)
	@JournalAttr(label="registraturaId",defaultText = "Регистратура",classifID = ""+OmbConstants.CODE_CLASSIF_REGISTRATURI)
	private Integer registraturaId;

	@Column(name = "DOC_ID")  // Период на регистрация на документ(DOC_DATE), регистрационен номер на документ(RN_DOC)
	@JournalAttr(label="docId",defaultText = "Документ", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer docId;
	
	@Column(name = "END_DOC_ID")
	@JournalAttr(label="endDocId",defaultText = "Документ при приключване", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer endDocId;

	@Column(name = "PARENT_ID")
	private Integer parentId;
	
	@Column(name = "RN_TASK", updatable = false)  // Номер на задача
	@JournalAttr(label="rnTask",defaultText = "Номер на задача")
	private String rnTask;

	@Column(name = "TASK_TYPE") // Вид на задача
	@JournalAttr(label="taskType",defaultText = "Вид на задача",classifID = "105")
	private Integer taskType;
	
	@Column(name = "TASK_INFO")  // Дефиниция на задача
	@JournalAttr(label="taskInfo",defaultText = "Текст на задача")
	private String taskInfo;

	@Column(name = "CODE_ASSIGN")
	@JournalAttr(label="codeAssign",defaultText = "Възложител",classifID = "24")
	private Integer codeAssign;

	@Column(name = "CODE_CONTROL")
	@JournalAttr(label="codeAssign",defaultText = "Контролиращ",classifID = "24")
	private Integer codeControl;

	@Column(name = "DOC_REQUIRED")
	@JournalAttr(label="docRequired",defaultText = "Изисква документ", classifID = ""+ OmbConstants.CODE_CLASSIF_DANE)
	private Integer docRequired;

	@Column(name = "STATUS") // Статус
	@JournalAttr(label="status",defaultText = "Статус на задача",classifID = "121")
	private Integer status;

	@Column(name = "STATUS_DATE")             // Период на регистрация
	@JournalAttr(label="statusDate",defaultText = "Дата на статус", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date statusDate;

	@Column(name = "STATUS_USER_ID")
	@JournalAttr(label="statusUserId",defaultText = "Потребител, сменил статуса",classifID = "16")
	private Integer statusUserId;  

	@Column(name = "STATUS_COMMENTS")
	@JournalAttr(label="statusComments",defaultText = "Коментар по статуса")
	private String statusComments;

	@Column(name = "ASSIGN_DATE")
	@JournalAttr(label="assignDate",defaultText = "Дата на възлагане", dateMask = "dd.MM.yyyy HH:mm")
	private Date assignDate;

	@Column(name = "SROK_DATE")
	@JournalAttr(label="srokDate",defaultText = "Срок на задача", dateMask = "dd.MM.yyyy HH:mm")
	private Date srokDate;
	
	@Column(name = "REAL_START")
	private Date realStart;

	@Column(name = "REAL_END")
	@JournalAttr(label="realEnd",defaultText = "Дата на изпълнение", dateMask = "dd.MM.yyyy HH:mm")
	private Date realEnd;

	@Column(name = "END_OPINION")
	@JournalAttr(label="endOpinion",defaultText = "Мнение при приключване", classifID = "110")
	private Integer endOpinion;

	@Column(name = "PROC_INFO")
	@JournalAttr(label="procInfo",defaultText = "Процедура")
	private String procInfo;

	@JournalAttr(label="signDef",defaultText = "Подписана дефиниция")
	@Column(name = "SIGN_DEF")
	private String signDef;
	
	@JournalAttr(label="signExe",defaultText = "Подписано изпълнение")
	@Column(name = "SIGN_EXE")
	private String signExe;
	
	@Transient
	private transient Boolean auditable; // за да може да се включва и изключва журналирането

	@Transient
	private transient Integer dbEndDocId;

	@Transient
	private transient Integer dbStatus; // стария статус
	@Transient
	private transient Integer dbCodeAssign;  // стария възложител
	@Transient
	private transient Integer dbCodeControl; // стария контролиращ
	@Transient
	private transient Integer hashNotifData; 	// тъй като се прави нотификация за промяна на основни данни, тук ще се пази хеш
												// на тези основни данни и на запис ще се сравнява с новия такъв
												// Objects.hash(taskType, assignDate, srokDate, taskInfo)
	@Transient
	@JournalAttr(label="codeExecs",defaultText = "Изпълнител на задача",classifID = "24")
	private List<Integer>			codeExecs;		// изпълнители ROLE_REF=2 (първият е отговорен изпълнител ROLE_REF=1)
	@Transient
	private transient List<Integer>	dbCodeExecs;	// изпълнителите каквито са били в БД

	@Transient
	private transient List<Integer> newCodeExecs; // новите codeExecs. след запис на задача вече са изчислени

//	@JournalAttr(label="rnDocEnd",defaultText = "Номер на документ при приключване")
	@Transient
	private String rnDocEnd; // док. при приключване

//	@JournalAttr(label="dateDocEnd",defaultText = "Дата на документ при приключване", dateMask = "dd.MM.yyyy HH:mm:ss")
	@Transient
	private Date dateDocEnd; // дата на док. при приключване
	
	/** */
	public Task() {
		super();
	}

	@Override
	public Integer getUserReg() {
		return super.getUserReg();
	}
	
	@Override
	public Date getDateReg() {
		return super.getDateReg();
	}
	
	/** @return the assignDate */
	public Date getAssignDate() {
		return this.assignDate;
	}

	/** @return the codeAssign */
	public Integer getCodeAssign() {
		return this.codeAssign;
	}

	/** @return the codeControl */
	public Integer getCodeControl() {
		return this.codeControl;
	}

	/** @return the codeExecs */
	public List<Integer> getCodeExecs() {
		return this.codeExecs;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK;
	}

	/** @return the dbCodeExecs */
	public List<Integer> getDbCodeExecs() {
		return this.dbCodeExecs;
	}

	/** @return the dbStatus */
	public Integer getDbStatus() {
		return this.dbStatus;
	}

	/** @return the docId */
	public Integer getDocId() {
		return this.docId;
	}

	/** @return the docRequired */
	public Integer getDocRequired() {
		return this.docRequired;
	}

	/** @return the endDocId */
	public Integer getEndDocId() {
		return this.endDocId;
	}

	/** @return the endOpinion */
	public Integer getEndOpinion() {
		return this.endOpinion;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the newCodeExecs */
	public List<Integer> getNewCodeExecs() {
		return this.newCodeExecs;
	}

	/** @return the parentId */
	public Integer getParentId() {
		return this.parentId;
	}

	/** @return the realEnd */
	public Date getRealEnd() {
		return this.realEnd;
	}

	/** @return the realStart */
	public Date getRealStart() {
		return this.realStart;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @return the rnTask */
	public String getRnTask() {
		return this.rnTask;
	}

	/** @return the srokDate */
	public Date getSrokDate() {
		return this.srokDate;
	}

	/** @return the status */
	public Integer getStatus() {
		return this.status;
	}

	/** @return the statusComments */
	public String getStatusComments() {
		return this.statusComments;
	}

	/** @return the statusDate */
	public Date getStatusDate() {
		return this.statusDate;
	}

	/** @return the statusUserId */
	public Integer getStatusUserId() {
		return this.statusUserId;
	}

	/** @return the taskInfo */
	public String getTaskInfo() {
		return this.taskInfo;
	}

	/** @return the taskType */
	public Integer getTaskType() {
		return this.taskType;
	}

	/** @return the auditable */
	@Override
	public boolean isAuditable() {
		return this.auditable == null ? super.isAuditable() : this.auditable.booleanValue();
	}

	/** @param assignDate the assignDate to set */
	public void setAssignDate(Date assignDate) {
		this.assignDate = assignDate;
	}

	/** @param auditable the auditable to set */
	public void setAuditable(Boolean auditable) {
		this.auditable = auditable;
	}

	/** @param codeAssign */
	public void setCodeAssign(Integer codeAssign) {
		this.codeAssign = codeAssign;
	}

	/** @param codeControl */
	public void setCodeControl(Integer codeControl) {
		this.codeControl = codeControl;
	}

	/** @param codeExecs */
	public void setCodeExecs(List<Integer> codeExecs) {
		this.codeExecs = codeExecs;
	}

	/** @param dbCodeExecs the dbCodeExecs to set */
	public void setDbCodeExecs(List<Integer> dbCodeExecs) {
		this.dbCodeExecs = dbCodeExecs;
	}

	/** @param dbStatus the dbStatus to set */
	public void setDbStatus(Integer dbStatus) {
		this.dbStatus = dbStatus;
	}

	/** @param docId the docId to set */
	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	/** @param docRequired the docRequired to set */
	public void setDocRequired(Integer docRequired) {
		this.docRequired = docRequired;
	}

	/** @param endDocId the endDocId to set */
	public void setEndDocId(Integer endDocId) {
		this.endDocId = endDocId;
	}

	/** @param endOpinion the endOpinion to set */
	public void setEndOpinion(Integer endOpinion) {
		this.endOpinion = endOpinion;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param newCodeExecs the newCodeExecs to set */
	public void setNewCodeExecs(List<Integer> newCodeExecs) {
		this.newCodeExecs = newCodeExecs;
	}

	/** @param parentId the parentId to set */
	public void setParentId(Integer parentId) {
		this.parentId = parentId;
	}

	/** @param realEnd the realEnd to set */
	public void setRealEnd(Date realEnd) {
		this.realEnd = realEnd;
	}

	/** @param realStart the realStart to set */
	public void setRealStart(Date realStart) {
		this.realStart = realStart;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** @param rnTask the rnTask to set */
	public void setRnTask(String rnTask) {
		this.rnTask = rnTask;
	}

	/** @param srokDate the srokDate to set */
	public void setSrokDate(Date srokDate) {
		this.srokDate = srokDate;
	}

	/** @param status the status to set */
	public void setStatus(Integer status) {
		this.status = status;
	}

	/** @param statusComments the statusComments to set */
	public void setStatusComments(String statusComments) {
		this.statusComments = statusComments;
	}

	/** @param statusDate the statusDate to set */
	public void setStatusDate(Date statusDate) {
		this.statusDate = statusDate;
	}

	/** @param statusUserId the statusUserId to set */
	public void setStatusUserId(Integer statusUserId) {
		this.statusUserId = statusUserId;
	}

	/** @param taskInfo the taskInfo to set */
	public void setTaskInfo(String taskInfo) {
		this.taskInfo = taskInfo;
	}

	/** @param taskType the taskType to set */
	public void setTaskType(Integer taskType) {
		this.taskType = taskType;
	}
	
	/** @return the dbCodeControl */
	public Integer getDbCodeControl() {
		return this.dbCodeControl;
	}

	/** @param dbCodeControl the dbCodeControl to set */
	public void setDbCodeControl(Integer dbCodeControl) {
		this.dbCodeControl = dbCodeControl;
	}

	/** @return the hashNotifData */
	public Integer getHashNotifData() {
		return this.hashNotifData;
	}

	/** @param hashNotifData the hashNotifData to set */
	public void setHashNotifData(Integer hashNotifData) {
		this.hashNotifData = hashNotifData;
	}

	/** @return the dbCodeAssign */
	public Integer getDbCodeAssign() {
		return this.dbCodeAssign;
	}

	/** @param dbCodeAssign the dbCodeAssign to set */
	public void setDbCodeAssign(Integer dbCodeAssign) {
		this.dbCodeAssign = dbCodeAssign;
	}

	/** @return the dbEndDocId */
	public Integer getDbEndDocId() {
		return this.dbEndDocId;
	}

	/** @param dbEndDocId the dbEndDocId to set */
	public void setDbEndDocId(Integer dbEndDocId) {
		this.dbEndDocId = dbEndDocId;
	}

	public String getRnDocEnd() {
		return rnDocEnd;
	}

	public void setRnDocEnd(String rnDocEnd) {
		this.rnDocEnd = rnDocEnd;
	}

	public Date getDateDocEnd() {
		return dateDocEnd;
	}

	public void setDateDocEnd(Date dateDocEnd) {
		this.dateDocEnd = dateDocEnd;
	}

	public String getProcInfo() {
		return this.procInfo;
	}

	public void setProcInfo(String procInfo) {
		this.procInfo = procInfo;
	}
	
	@Override
	public String getIdentInfo() throws DbErrorException {
		return getRnTask();
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
		if (endDocId != null ) {
			dj.setJoinedCodeObject2(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
			dj.setJoinedIdObject2(endDocId);
		}
		return dj;
	}

	/** @return the signDef */
	public String getSignDef() {
		return this.signDef;
	}
	/** @param signDef the signDef to set */
	public void setSignDef(String signDef) {
		this.signDef = signDef;
	}
	/** @return the signExe */
	public String getSignExe() {
		return this.signExe;
	}
	/** @param signExe the signExe to set */
	public void setSignExe(String signExe) {
		this.signExe = signExe;
	}
}
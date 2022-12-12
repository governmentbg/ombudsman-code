package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ib.indexui.system.Constants;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JPA;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.PersistentEntity;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Дефиниция на задача от етап на Процедура
 *
 * @author belev
 */
@Entity
@Table(name = "PROC_DEF_TASK")
public class ProcDefTask extends TrackableEntity implements AuditExt {

	/**  */
	private static final long serialVersionUID = -3667200286131878661L;

	@SequenceGenerator(name = "ProcDefTask", sequenceName = "SEQ_PROC_DEF_TASK", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "ProcDefTask")
	@Column(name = "DEF_TASK_ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "DEF_ETAP_ID", updatable = false)
	private Integer defEtapId;

	@Column(name = "TASK_TYPE")
	@JournalAttr(label = "taskType", defaultText = "Вид задача", classifID = "" + OmbConstants.CODE_CLASSIF_TASK_VID)
	private Integer taskType;

	@Column(name = "TASK_INFO")
	@JournalAttr(label = "taskInfo", defaultText = "Допълнителна информация")
	private String taskInfo;

	@Column(name = "SROK_DAYS")
	@JournalAttr(label = "srokDays", defaultText = "Срок в дни")
	private Integer srokDays;

	@Column(name = "SROK_HOURS")
	@JournalAttr(label = "srokHours", defaultText = "Срок в часове")
	private Integer srokHours;

	@Column(name = "INDIVIDUAL")
	@JournalAttr(label = "individual", defaultText = "Изпълнява се индивидуално", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	private Integer individual;

	@Column(name = "DOC_REQUIRED")
	@JournalAttr(label = "docRequired", defaultText = "Изисква се документ при приключване", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	private Integer docRequired;

	@Column(name = "ASSIGN_CODE_REF")
	@JournalAttr(label = "assignCodeRef", defaultText = "Възложител - Служител", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private Integer assignCodeRef;

	@Column(name = "ASSIGN_ZVENO")
	@JournalAttr(label = "nomer", defaultText = "Възложител - Звено", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private Integer assignZveno;

	@Column(name = "ASSIGN_EMPL_POSITION")
	@JournalAttr(label = "nomer", defaultText = "Възложител - Длъжност", classifID = "" + Constants.CODE_CLASSIF_POSITION)
	private Integer assignEmplPosition;

	@Column(name = "ASSIGN_BUSINESS_ROLE")
	@JournalAttr(label = "nomer", defaultText = "Възложител - Бизнес роля", classifID = "" + OmbConstants.CODE_CLASSIF_PROC_BUSINESS_ROLE)
	private Integer assignBusinessRole;

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	@JoinColumn(name = "DEF_TASK_ID", referencedColumnName = "DEF_TASK_ID", nullable = false)
	@JournalAttr(label = "codeExecs", defaultText = "Изпълнител на задача")
	private List<ProcDefTaskIzp> taskIzpList;

	@Transient // PROC_DEF_OPINION.OPTYPE=1
	@JournalAttr(label = "opinionOkList", defaultText = "Мнения водещи към следващи етапи (при ДА)", classifID = OmbConstants.CODE_CLASSIF_TASK_OPINION + "")
	private List<Integer> opinionOkList;

	@Transient // PROC_DEF_OPINION.OPTYPE=2
	@JournalAttr(label = "opinionNotList", defaultText = "Мнения водещи към следващи етапи (при НЕ)", classifID = OmbConstants.CODE_CLASSIF_TASK_OPINION + "")
	private List<Integer> opinionNotList;

	@Transient // PROC_DEF_OPINION.OPTYPE=3
	@JournalAttr(label = "opinionOptionalList", defaultText = "Мнения водещи към следващи етапи-опционални", classifID = OmbConstants.CODE_CLASSIF_TASK_OPINION + "")
	private List<Integer> opinionOptionalList;

	@Transient
	private transient Integer dbTaskType;

	@Transient
	private transient List<Integer> realIzpCodes; // реалните определени кодове на база дефиниция

	/** */
	public ProcDefTask() {
		super();
	}

	/** @return the assignBusinessRole */
	public Integer getAssignBusinessRole() {
		return this.assignBusinessRole;
	}

	/** @return the assignCodeRef */
	public Integer getAssignCodeRef() {
		return this.assignCodeRef;
	}

	/** @return the assignEmplPosition */
	public Integer getAssignEmplPosition() {
		return this.assignEmplPosition;
	}

	/** @return the assignZveno */
	public Integer getAssignZveno() {
		return this.assignZveno;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_DEF_TASK;
	}

	/** @return the dbTaskType */
	public Integer getDbTaskType() {
		return this.dbTaskType;
	}

	/** @return the defEtapId */
	public Integer getDefEtapId() {
		return this.defEtapId;
	}

	/** @return the docRequired */
	public Integer getDocRequired() {
		return this.docRequired;
	}

	/** @return the id */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		return "ИД Деф.Етап: " + this.defEtapId;
	}

	/** @return the individual */
	public Integer getIndividual() {
		return this.individual;
	}

	/** @return the opinionNotList */
	public List<Integer> getOpinionNotList() {
		return this.opinionNotList;
	}

	/** @return the opinionOkList */
	public List<Integer> getOpinionOkList() {
		return this.opinionOkList;
	}

	/** @return the opinionOptionalList */
	public List<Integer> getOpinionOptionalList() {
		return this.opinionOptionalList;
	}

	/** @return the realIzpCodes */
	public List<Integer> getRealIzpCodes() {
		return this.realIzpCodes;
	}

	/** @return the srokDays */
	public Integer getSrokDays() {
		return this.srokDays;
	}

	/** @return the srokHours */
	public Integer getSrokHours() {
		return this.srokHours;
	}

	/** @return the taskInfo */
	public String getTaskInfo() {
		return this.taskInfo;
	}

	/** @return the taskIzpList */
	public List<ProcDefTaskIzp> getTaskIzpList() {
		if (this.taskIzpList == null) {
			this.taskIzpList = new ArrayList<>();
		}
		return this.taskIzpList;
	}

	/** @return the taskType */
	public Integer getTaskType() {
		return this.taskType;
	}

	/** @param assignBusinessRole the assignBusinessRole to set */
	public void setAssignBusinessRole(Integer assignBusinessRole) {
		this.assignBusinessRole = assignBusinessRole;
	}

	/** @param assignCodeRef the assignCodeRef to set */
	public void setAssignCodeRef(Integer assignCodeRef) {
		this.assignCodeRef = assignCodeRef;
	}

	/** @param assignEmplPosition the assignEmplPosition to set */
	public void setAssignEmplPosition(Integer assignEmplPosition) {
		this.assignEmplPosition = assignEmplPosition;
	}

	/** @param assignZveno the assignZveno to set */
	public void setAssignZveno(Integer assignZveno) {
		this.assignZveno = assignZveno;
	}

	/** @param dbTaskType the dbTaskType to set */
	public void setDbTaskType(Integer dbTaskType) {
		this.dbTaskType = dbTaskType;
	}

	/** @param defEtapId the defEtapId to set */
	public void setDefEtapId(Integer defEtapId) {
		this.defEtapId = defEtapId;
	}

	/** @param docRequired the docRequired to set */
	public void setDocRequired(Integer docRequired) {
		this.docRequired = docRequired;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param individual the individual to set */
	public void setIndividual(Integer individual) {
		this.individual = individual;
	}

	/** @param opinionNotList the opinionNotList to set */
	public void setOpinionNotList(List<Integer> opinionNotList) {
		this.opinionNotList = opinionNotList;
	}

	/** @param opinionOkList the opinionOkList to set */
	public void setOpinionOkList(List<Integer> opinionOkList) {
		this.opinionOkList = opinionOkList;
	}

	/** @param opinionOptionalList the opinionOptionalList to set */
	public void setOpinionOptionalList(List<Integer> opinionOptionalList) {
		this.opinionOptionalList = opinionOptionalList;
	}

	/** @param realIzpCodes the realIzpCodes to set */
	public void setRealIzpCodes(List<Integer> realIzpCodes) {
		this.realIzpCodes = realIzpCodes;
	}

	/** @param srokDays the srokDays to set */
	public void setSrokDays(Integer srokDays) {
		this.srokDays = srokDays;
	}

	/** @param srokHours the srokHours to set */
	public void setSrokHours(Integer srokHours) {
		this.srokHours = srokHours;
	}

	/** @param taskInfo the taskInfo to set */
	public void setTaskInfo(String taskInfo) {
		this.taskInfo = taskInfo;
	}

	/** @param taskIzpList the taskIzpList to set */
	public void setTaskIzpList(List<ProcDefTaskIzp> taskIzpList) {
		this.taskIzpList = taskIzpList;
	}

	/** @param taskType the taskType to set */
	public void setTaskType(Integer taskType) {
		this.taskType = taskType;
	}

	/** */
	@Override
	public PersistentEntity toAuditSerializeObject(String unitName) {
		ProcDefTask obj;
		try {
			obj = clone();
		} catch (CloneNotSupportedException e) {
			return this;
		}

		JPA jpa = JPA.getUtil(unitName);

		obj.taskIzpList = this.taskIzpList != null && jpa.isLoaded(this.taskIzpList) ? new ArrayList<>(this.taskIzpList) : null;

		return obj;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new SystemJournal();
		dj.setCodeObject(getCodeMainObject());
		dj.setIdObject(getId());
		dj.setIdentObject(getIdentInfo());
		dj.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_DEF_ETAP);
		dj.setJoinedIdObject1(this.defEtapId);
		return dj;
	}

	/** */
	@Override
	protected ProcDefTask clone() throws CloneNotSupportedException {
		return (ProcDefTask) super.clone();
	}

}
package com.ib.omb.db.dto;

import com.ib.omb.db.listeners.TaskEntityListener;
import com.ib.omb.ocr.OCRUtils;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import org.hibernate.search.annotations.*;
import org.hibernate.search.annotations.Index;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK;
import static javax.persistence.GenerationType.SEQUENCE;

/**
 * Задача
 *
 * @author belev
 */
//@Entity
//@Table(name = "TASK")
//@Indexed(index = "taskocr")
public class TaskOcr extends TrackableEntity implements FullTextIndexedEntity{
	/**  */
	private static final long serialVersionUID = 8811578732792908654L;

	@SequenceGenerator(name = "Task", sequenceName = "SEQ_TASK", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "Task")
	@Column(name = "TASK_ID", unique = true, nullable = false)
	@Field(name="task_id")
	private Integer id;

	@Field(analyze = Analyze.NO)
	@Column(name = "REGISTRATURA_ID")
	private Integer registraturaId;

	@Field
	@Column(name = "DOC_ID")  // Период на регистрация на документ(DOC_DATE), регистрационен номер на документ(RN_DOC)
	private Integer docId;

	@Field
	@Column(name = "END_DOC_ID")
	private Integer endDocId;

	@Column(name = "PARENT_ID")
	private Integer parentId;

	@Field(name = "rnTask", analyze = Analyze.NO)
	@Field(name = "rnTaskLike", analyzer = @Analyzer(definition = "NgramBG"))
	@Column(name = "RN_TASK")  // Номер на задача
	private String rnTask;

	@Field
	@Column(name = "TASK_TYPE") // Вид на задача
	private Integer taskType;

//	@Field(analyzer = @Analyzer(definition = "defaultBG"))
	@Field
	@Column(name = "TASK_INFO")  // Дефиниция на задача
	private String taskInfo;

	@Field
	@Column(name = "CODE_ASSIGN")
	private Integer codeAssign;

	@Field
	@Column(name = "CODE_CONTROL")
	private Integer codeControl;

	@Field
	@Column(name = "DOC_REQUIRED")
	private Integer docRequired;

	@Field
	@Column(name = "STATUS") // Статус
	private Integer status;

	@Field
	@DateBridge(resolution = Resolution.DAY)
	@Column(name = "STATUS_DATE")             // Период на регистрация
	private Date statusDate;

	@Field
	@Column(name = "STATUS_USER_ID")
	private Integer statusUserId;

	@Field
	@Column(name = "STATUS_COMMENTS")
	private String statusComments;

	@Field
	@DateBridge(resolution = Resolution.DAY)  // Период на възлагане
	@Column(name = "ASSIGN_DATE")
	private Date assignDate;

	@Field
	@DateBridge(resolution = Resolution.DAY)   // Период на завършване
	@Column(name = "SROK_DATE")
	private Date srokDate;

	@Column(name = "REAL_START")
	private Date realStart;

	@Column(name = "REAL_END")
	private Date realEnd;

	@Column(name = "END_OPINION")
	private Integer endOpinion;

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
	private List<Integer>			codeExecs;		// изпълнители ROLE_REF=2 (първият е отговорен изпълнител ROLE_REF=1)
	@Transient
	private transient List<Integer>	dbCodeExecs;	// изпълнителите каквито са били в БД

	@Transient
	private transient List<Integer> newCodeExecs; // новите codeExecs. след запис на задача вече са изчислени

	@Transient
	private String rnDocEnd; // док. при приключване

	@Transient
	private Date dateDocEnd; // дата на док. при приключване

	/** */
	public TaskOcr() {
		super();
	}

	@Override
	@Field
	public Integer getUserReg() {
		return super.getUserReg();
	}
	
	@Override
	@Field
	public Date getDateReg() {
		return super.getDateReg();
	}

	@IndexedEmbedded
	@Field(name = "ocr",index = Index.YES,store = Store.NO)
	//На мястото на String моиже да е клас, който има само @Field анотации
	public List<String> getOcr(){
		OCRUtils ocrut = new OCRUtils();
		List<String> ocrResults=new ArrayList<>();
		FilesDAO daoF = new FilesDAO(UserData.DEFAULT);
		try {

			List<Files> filesList = daoF.selectByFileObjectDop(this.id, CODE_ZNACHENIE_JOURNAL_TASK); // използва се този метод за зареждане на файлове, за да се заредят доп. полета - лични дании, официален док и т.н.

			for(int i=0;i<filesList.size();i++){
				String result = ocrut.process(daoF.findById(filesList.get(i).getId()));
				ocrResults.add(result);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("next-->" + new Date());
		return ocrResults;

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

	//За пълнотесктово търсене
	public enum FullTextFields{
		rnDocLike,
		rnTaskLike,
		taskInfo,
		statusComments,
		ocr
	}

	public enum FilterFields{
		rnDoc,
		rnTask,
		registraturaId,
		statusDateFrom,
		statusDateTo,
		assignDateFrom,
		assignDateTo,
		srokDateFrom,
		srokDateTo,
		docDateFrom,
		docDateTo,
		codeAssign,
		codeControl,
		codeExecList,
		taskType,
		status
		
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
}
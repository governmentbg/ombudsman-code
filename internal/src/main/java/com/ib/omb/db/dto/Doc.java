package com.ib.omb.db.dto;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AGREED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_SIGNED;
import static javax.persistence.GenerationType.SEQUENCE;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;


/**
 * Документ
 *
 * @author belev
 */
@Entity
@Table(name = "DOC")
//@EntityListeners(DocEntityListener.class)
@XmlRootElement
public class Doc extends TrackableEntity implements FullTextIndexedEntity,AuditExt{
	/**  */
	private static final long serialVersionUID = -4381087985324069934L;

	@SequenceGenerator(name = "Doc", sequenceName = "SEQ_DOC", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "Doc")
	@Column(name = "DOC_ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "REGISTRATURA_ID")
	@JournalAttr(label="registraturaId",defaultText = "Регистратура",classifID = ""+OmbConstants.CODE_CLASSIF_REGISTRATURI)
	private Integer registraturaId;

	@Column(name = "REGISTER_ID")
	@JournalAttr(label="registerId",defaultText = "Регистър",classifID = ""+OmbConstants.CODE_CLASSIF_REGISTRI)
	private Integer registerId;

	@Column(name = "WORK_OFF_ID")
	@JournalAttr(label="workOffId",defaultText = "Работен/официален документ", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer workOffId;

	@Column(name = "CODE_REF_CORRESP")
	@JournalAttr(label="codeRefCorresp",defaultText = "Кореспондент",classifID = ""+OmbConstants.CODE_CLASSIF_REFERENTS)
	private Integer codeRefCorresp;

	@Column(name = "RN_DOC")
	@JournalAttr(label="rnDoc",defaultText = "Регистрационен номер")
	private String rnDoc;

	@Column(name = "RN_PREFIX")
	@JournalAttr(label="rnPrefix",defaultText = "Префикс на номера")
	private String rnPrefix;

	@Column(name = "RN_PORED")
	@JournalAttr(label="rnPored",defaultText = "Пореден номер от номера")
	private Integer rnPored;

	@Column(name = "GUID")
	@JournalAttr(label="guid",defaultText = "GUID")
	private String guid;

	@Column(name = "FOR_REG_ID")
	@JournalAttr(label="forRegId",defaultText = "За регистрация в",classifID = ""+ OmbConstants.CODE_CLASSIF_REGISTRATURI)
	private Integer forRegId;

	@Column(name = "PRE_FOR_REG_ID")
	@JournalAttr(label="preForRegId",defaultText = "Предварително избрана регистртура за регистрация",classifID = ""+ OmbConstants.CODE_CLASSIF_REGISTRATURI)
	private Integer preForRegId;

	@Column(name = "DOC_TYPE")
	@JournalAttr(label="docType",defaultText = "Тип на документа",classifID="129")
	private Integer docType;

	@Column(name = "DOC_VID")
	@JournalAttr(label="docVid",defaultText = "Вид на документа", classifID = "104")
	private Integer docVid;

	@Column(name = "DOC_DATE")
	@JournalAttr(label="docDate",defaultText = "Дата на документ", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date docDate;

	@Column(name = "DOC_NAME")
	@JournalAttr(label="docName",defaultText = "Наименование на документ")
	private String docName;

	@JournalAttr(label="docInfo",defaultText = "Допълнителна информация")
	@Column(name = "DOC_INFO")
	private String docInfo;

	@Column(name = "OTNOSNO")
	@JournalAttr(label="otnosno",defaultText = "Относно")
	private String otnosno;

	@Column(name = "RECEIVE_DATE")
	@JournalAttr(label="receiveDate",defaultText = "Дата на получаване")
	private Date receiveDate;

	@Column(name = "RECEIVE_METHOD")
	@JournalAttr(label="receiveMetod",defaultText = " Начин на получаване",classifID = "112")
	private Integer receiveMethod;

	@Column(name = "RECEIVED_BY")
	@JournalAttr(label="receiveBy",defaultText = "Получен доп.информация")
	private String receivedBy;

	@Column(name = "DELIVERY_METHOD")
	@JournalAttr(label="deliveryMethod",defaultText = "Предоставяне на резултата",classifID = "112")
	private Integer deliveryMethod;

	@Column(name = "WAIT_ANSWER_DATE")
	@JournalAttr(label="waitAnswerDate",defaultText = "Очаквана дата за отговор")
	private Date waitAnswerDate;

	@Column(name = "VALID")
	@JournalAttr(label="valid",defaultText = "Валидност", classifID = ""+ OmbConstants.CODE_CLASSIF_DOC_VALID)
	private Integer valid;

	@Column(name = "VALID_DATE")
	@JournalAttr(label="validDate",defaultText = "Дата на валидност")
	private Date validDate;

	@Column(name = "TEH_NOMER")
	@JournalAttr(label="tehNomer",defaultText = "Техен номер")
	private String tehNomer;

	@Column(name = "TEH_DATE")
	@JournalAttr(label="tehDate",defaultText = "Дата на тех. номер")
	private Date tehDate;

	
	@Column(name = "STATUS")
	@JournalAttr(label="status",defaultText = "Статус", classifID = ""+ OmbConstants.CODE_CLASSIF_DOC_STATUS)
	private Integer status;

	@Column(name = "STATUS_DATE")
	@JournalAttr(label="statusDate",defaultText = "Дата на статус")
	private Date statusDate;

	@Column(name = "URGENT")
	@JournalAttr(label="urgent",defaultText = "Спешност", classifID = ""+ OmbConstants.CODE_CLASSIF_URGENT)
	private Integer urgent;

	@Column(name = "PROCESSED")
	@JournalAttr(label="processed",defaultText = "Обработен", classifID = ""+ OmbConstants.CODE_CLASSIF_DANE)
	private Integer processed;

	@Column(name = "IRREGULAR")
	@JournalAttr(label="irregular",defaultText = "Причини за нередовност", classifID = ""+ OmbConstants.CODE_CLASSIF_DOC_IRREGULAR)
	private Integer irregular;

	@Column(name = "FREE_ACCESS")
	@JournalAttr(label="freeAccess",defaultText = "Свободен достъп", classifID = ""+ OmbConstants.CODE_CLASSIF_DANE)
	private Integer freeAccess;

	@Column(name = "COUNT_FILES")
	@JournalAttr(label="countFiles",defaultText = "Брой файлове")
	private Integer countFiles;

	@Column(name = "COUNT_ORIGINALS")
	@JournalAttr(label="countOriginals",defaultText = "Брой оригинални екз.")
	private Integer countOriginals;

	@Column(name = "COUNT_COPIES")
	@JournalAttr(label="countCopies",defaultText = "Брой копия")
	private Integer countCopies;

	@Column(name = "COUNT_SHEETS")
	@JournalAttr(label="countSheets",defaultText = "Брой листа")
	private Integer countSheets;

	@Column(name = "FROM_MAIL")
	@JournalAttr(label="fromMail",defaultText = "Получен от")
	private String fromMail;

		
	@Column(name = "COMPETENCE")
	@JournalAttr(label="competence",defaultText = "Компетентност", classifID = ""+ OmbConstants.CODE_CLASSIF_COMPETENCE)
	private Integer competence;

	@JournalAttr(label="competenceText",defaultText = "Компетентност - описание")
	@Column(name = "COMPETENCE_TEXT")
	private String competenceText;

	@Column(name = "PROC_DEF")
	private Integer procDef; // статус на дефиницията CODE_CLASSIF_PROC_DEF_STAT
	@Transient
	private Integer procExeStat; // статус на текущото изпълнение CODE_CLASSIF_PROC_STAT

	@Column(name = "SIGN_METHOD")
	@JournalAttr(label="signMethod",defaultText = "Начин на подписване",classifID = ""+ OmbConstants.CODE_CLASSIF_DOC_SIGN_METHOD)
	private Integer signMethod;

	@JournalAttr(label="poredDelo",defaultText = "Пореден номер(#) в преписка")
	@Column(name = "PORED_DELO")	
	private Integer poredDelo;

	@JournalAttr(label="jalba", defaultText = "Жалба")
	@Transient
	private DocJalba jalba; // данни за жалбата

	@JournalAttr(label="docSpec", defaultText = "Проверка по НПМ/ самосезиране")
	@Transient
	private DocSpec docSpec;

	@Transient
	private transient Boolean auditable; // за да може да се включва и изключва журналирането

	@Transient
	private transient List<DocReferent>	history;			// всички изтеглени от БД това няма смисъл да се сериализира при
															// журналиране
	@Transient
	@JournalAttr(label="referentsAuthor",defaultText = "Автор")
	private List<DocReferent>			referentsAuthor;	// автор ROLE_REF=1
	@Transient
	@JournalAttr(label="referentsAgreed",defaultText = "Съгласувал")
	private List<DocReferent>			referentsAgreed;	// съгласувал ROLE_REF=2
	@Transient
	@JournalAttr(label="referentsSigned",defaultText = "Подписал")
	private List<DocReferent>			referentsSigned;	// подписал ROLE_REF=3

	@Transient
	private boolean deloIncluded; // при true значи има връзка с дело в таблица DELO_DOC

	/** [0]-DOC_ID, [1]-RN_DOC, [2]-DOC_DATE, [3]-REGISTRATURA_ID */
	@Transient
	private Object[]	workOffData;	// това са данните за дока през връзката WORK_OFF_ID
	/** [0]-DOC_ID, [1]-RN_DOC, [2]-DOC_DATE */
	@Transient
	private Object[]	protocolData;	// това са данните когато документа е унищожен и има протокол за унищожаване

	@Transient
	private transient Integer dbForRegId; // за да може да се следи кога се появява заявката за деловодна регистрация
											// и да се пусне нотификация за деловодителите
	@Transient
	private transient Integer dbProcessed; // за да може да се следи кога се сменя и да се пусне нотификация към авторите

	@Transient
	private transient Date dbDocDate; // за да се знае нужна ли е валидация на номера

	@JournalAttr(label="dopdata",defaultText = "Допълнителни данни")
	@Transient
	private DocDopdata dopdata; // допълнителни данни за документ.
								// в зависисмост от вида документ може да има допълнители данни, които трябва да се записват

	@Transient
	@JournalAttr(label="topic",defaultText = "Тематика",classifID = "126")
	private List<Integer>			topicList;
	@Transient
	private transient List<Integer>	dbTopicList;	// темите каквито са били в БД

	@Transient
	//@JournalAttr(label="docAccess",defaultText = "Изричен достъп")
	private List<DocAccess>			docAccess;	// Даване на изричен достъп - насочване, за сведение или както там се реши да се казва

	public List<DocAccess> getDocAccess() {
		return docAccess;
	}

	public void setDocAccess(List<DocAccess> docAccess) {
		this.docAccess = docAccess;
	}

	/**  */
	public Doc() {
		super();
	}

	/** */
	@Override
	public Integer getUserReg() {
		return super.getUserReg();
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC;
	}

	/** @return the codeRefCorresp */
	public Integer getCodeRefCorresp() {
		return this.codeRefCorresp;
	}

	/** @return the countCopies */
	public Integer getCountCopies() {
		return this.countCopies;
	}

	/** @return the countFiles */
	public Integer getCountFiles() {
		return this.countFiles;
	}

	/** @return the countOriginals */
	public Integer getCountOriginals() {
		return this.countOriginals;
	}

	/** @return the countSheets */
	public Integer getCountSheets() {
		return this.countSheets;
	}

	/** @return the deliveryMethod */
	public Integer getDeliveryMethod() {
		return this.deliveryMethod;
	}

	/** @return the docDate */
	public Date getDocDate() {
		return this.docDate;
	}

	/** @return the docInfo */
	public String getDocInfo() {
		return this.docInfo;
	}

	/** @return the docName */
	public String getDocName() {
		return this.docName;
	}

	/** @return the docType */
	public Integer getDocType() {
		return this.docType;
	}

	/** @return the docVid */
	public Integer getDocVid() {
		return this.docVid;
	}

	/** @return the forRegId */
	public Integer getForRegId() {
		return this.forRegId;
	}

	/** @return the freeAccess */
	public Integer getFreeAccess() {
		return this.freeAccess;
	}

	/** @return the guid */
	public String getGuid() {
		return this.guid;
	}

	/** @return the history */
	@XmlTransient
	public List<DocReferent> getHistory() {
		return this.history;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the irregular */
	public Integer getIrregular() {
		return this.irregular;
	}

	/** @return the otnosno */
	public String getOtnosno() {
		return this.otnosno;
	}

	/** @return the processed */
	public Integer getProcessed() {
		return this.processed;
	}

	/** @return the receiveDate */
	public Date getReceiveDate() {
		return this.receiveDate;
	}

	/** @return the receivedBy */
	public String getReceivedBy() {
		return this.receivedBy;
	}

	/** @return the receiveMethod */
	public Integer getReceiveMethod() {
		return this.receiveMethod;
	}

	/**
	 * ROLE_REF=2 {@link OmbConstants#CODE_ZNACHENIE_DOC_REF_ROLE_AGREED}
	 *
	 * @return the referentsAgreed
	 */
	public List<DocReferent> getReferentsAgreed() {
		return this.referentsAgreed;
	}

	/**
	 * ROLE_REF=1 {@link OmbConstants#CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR}
	 *
	 * @return the referentsAuthor
	 */
	public List<DocReferent> getReferentsAuthor() {
		return this.referentsAuthor;
	}

	/**
	 * ROLE_REF=3 {@link OmbConstants#CODE_ZNACHENIE_DOC_REF_ROLE_SIGNED}
	 *
	 * @return the referentsSigned
	 */
	public List<DocReferent> getReferentsSigned() {
		return this.referentsSigned;
	}

	/** @return the registerId */
	public Integer getRegisterId() {
		return this.registerId;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @return the rnDoc */
	public String getRnDoc() {
		return this.rnDoc;
	}

	/** @return the rnPored */
	public Integer getRnPored() {
		return this.rnPored;
	}

	/** @return the rnPrefix */
	public String getRnPrefix() {
		return this.rnPrefix;
	}

	/** @return the status */
	public Integer getStatus() {
		return this.status;
	}

	/** @return the tehDate */
	public Date getTehDate() {
		return this.tehDate;
	}

	/** @return the tehNomer */
	public String getTehNomer() {
		return this.tehNomer;
	}

	/** @return the urgent */
	public Integer getUrgent() {
		return this.urgent;
	}

	/** @return the valid */
	public Integer getValid() {
		return this.valid;
	}

	/** @return the validDate */
	public Date getValidDate() {
		return this.validDate;
	}

	/** @return the waitAnswerDate */
	public Date getWaitAnswerDate() {
		return this.waitAnswerDate;
	}

	/** @return the workOffId */
	public Integer getWorkOffId() {
		return this.workOffId;
	}

	/** @return the auditable */
	@Override
	public boolean isAuditable() {
		return this.auditable == null ? super.isAuditable() : this.auditable.booleanValue();
	}

	/** @return the deloIncluded */
	public boolean isDeloIncluded() {
		return this.deloIncluded;
	}

	/**
	 * Разпределя участниците по списъците на база ролята им
	 *
	 * @throws DbErrorException
	 */
	public final void referentsDistribute() throws DbErrorException {

		this.referentsAgreed = new ArrayList<>();
		this.referentsAuthor = new ArrayList<>();
		this.referentsSigned = new ArrayList<>();

		if (this.history == null || this.history.isEmpty()) {
			return;
		}

		for (DocReferent dr : this.history) {
			switch (dr.getRoleRef()) {

			case CODE_ZNACHENIE_DOC_REF_ROLE_SIGNED:
				this.referentsSigned.add(new DocReferent(dr));
				break;

			case CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR:
				this.referentsAuthor.add(new DocReferent(dr));
				break;

			case CODE_ZNACHENIE_DOC_REF_ROLE_AGREED:
				this.referentsAgreed.add(new DocReferent(dr));
				break;

			default: // идват грешни данни от базата
				throw new DbErrorException(dr + " UNKNOWN ROLE_REF");
			}
		}
	}

	/** @param auditable the auditable to set */
	public void setAuditable(Boolean auditable) {
		this.auditable = auditable;
	}

	/** @param codeRefCorresp the codeRefCorresp to set */
	public void setCodeRefCorresp(Integer codeRefCorresp) {
		this.codeRefCorresp = codeRefCorresp;
	}

	/** @param countCopies the countCopies to set */
	public void setCountCopies(Integer countCopies) {
		this.countCopies = countCopies;
	}

	/** @param countFiles the countFiles to set */
	public void setCountFiles(Integer countFiles) {
		this.countFiles = countFiles;
	}

	/** @param countOriginals the countOriginals to set */
	public void setCountOriginals(Integer countOriginals) {
		this.countOriginals = countOriginals;
	}

	/** @param countSheets the countSheets to set */
	public void setCountSheets(Integer countSheets) {
		this.countSheets = countSheets;
	}

	/** @param deliveryMethod the deliveryMethod to set */
	public void setDeliveryMethod(Integer deliveryMethod) {
		this.deliveryMethod = deliveryMethod;
	}

	/** @param deloIncluded the deloIncluded to set */
	public void setDeloIncluded(boolean deloIncluded) {
		this.deloIncluded = deloIncluded;
	}

	/** @param docDate the docDate to set */
	public void setDocDate(Date docDate) {
		this.docDate = docDate;
	}

	/** @param docInfo the docInfo to set */
	public void setDocInfo(String docInfo) {
		this.docInfo = docInfo;
	}

	/** @param docName the docName to set */
	public void setDocName(String docName) {
		this.docName = docName;
	}

	/** @param docType the docType to set */
	public void setDocType(Integer docType) {
		this.docType = docType;
	}

	/** @param docVid the docVid to set */
	public void setDocVid(Integer docVid) {
		this.docVid = docVid;
	}

	/** @param forRegId the forRegId to set */
	public void setForRegId(Integer forRegId) {
		this.forRegId = forRegId;
	}

	/** @param freeAccess the freeAccess to set */
	public void setFreeAccess(Integer freeAccess) {
		this.freeAccess = freeAccess;
	}

	/** @param guid the guid to set */
	public void setGuid(String guid) {
		this.guid = guid;
	}

	/** @param history the history to set */
	public void setHistory(List<DocReferent> history) {
		this.history = history;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param irregular the irregular to set */
	public void setIrregular(Integer irregular) {
		this.irregular = irregular;
	}

	/** @param otnosno the otnosno to set */
	public void setOtnosno(String otnosno) {
		this.otnosno = otnosno;
	}

	/** @param processed the processed to set */
	public void setProcessed(Integer processed) {
		this.processed = processed;
	}

	/** @param receiveDate the receiveDate to set */
	public void setReceiveDate(Date receiveDate) {
		this.receiveDate = receiveDate;
	}

	/** @param receivedBy the receivedBy to set */
	public void setReceivedBy(String receivedBy) {
		this.receivedBy = receivedBy;
	}

	/** @param receiveMethod the receiveMethod to set */
	public void setReceiveMethod(Integer receiveMethod) {
		this.receiveMethod = receiveMethod;
	}

	/** @param referentsAgreed the referentsAgreed to set */
	public void setReferentsAgreed(List<DocReferent> referentsAgreed) {
		this.referentsAgreed = referentsAgreed;
	}

	/** @param referentsAuthor the referentsAuthor to set */
	public void setReferentsAuthor(List<DocReferent> referentsAuthor) {
		this.referentsAuthor = referentsAuthor;
	}

	/** @param referentsSigned the referentsSigned to set */
	public void setReferentsSigned(List<DocReferent> referentsSigned) {
		this.referentsSigned = referentsSigned;
	}

	/** @param registerId the registerId to set */
	public void setRegisterId(Integer registerId) {
		this.registerId = registerId;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** @param rnDoc the rnDoc to set */
	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}

	/** @param rnPored the rnPored to set */
	public void setRnPored(Integer rnPored) {
		this.rnPored = rnPored;
	}

	/** @param rnPrefix the rnPrefix to set */
	public void setRnPrefix(String rnPrefix) {
		this.rnPrefix = rnPrefix;
	}

	/** @param status the status to set */
	public void setStatus(Integer status) {
		this.status = status;
	}

	/** @param tehDate the tehDate to set */
	public void setTehDate(Date tehDate) {
		this.tehDate = tehDate;
	}

	/** @param tehNomer the tehNomer to set */
	public void setTehNomer(String tehNomer) {
		this.tehNomer = tehNomer;
	}

	/** @param urgent the urgent to set */
	public void setUrgent(Integer urgent) {
		this.urgent = urgent;
	}

	/** @param valid the valid to set */
	public void setValid(Integer valid) {
		this.valid = valid;
	}

	/** @param validDate the validDate to set */
	public void setValidDate(Date validDate) {
		this.validDate = validDate;
	}

	/** @param waitAnswerDate the waitAnswerDate to set */
	public void setWaitAnswerDate(Date waitAnswerDate) {
		this.waitAnswerDate = waitAnswerDate;
	}

	/** @param workOffId the workOffId to set */
	public void setWorkOffId(Integer workOffId) {
		this.workOffId = workOffId;
	}

	/** @return the workOffData */
	public Object[] getWorkOffData() {
		return this.workOffData;
	}

	/** @param workOffData the workOffData to set */
	public void setWorkOffData(Object[] workOffData) {
		this.workOffData = workOffData;
	}

	/** @return the protocolData */
	public Object[] getProtocolData() {
		return this.protocolData;
	}

	/** @param protocolData the protocolData to set */
	public void setProtocolData(Object[] protocolData) {
		this.protocolData = protocolData;
	}

	/** @return the statusDate */
	public Date getStatusDate() {
		return this.statusDate;
	}

	/** @param statusDate the statusDate to set */
	public void setStatusDate(Date statusDate) {
		this.statusDate = statusDate;
	}

	/** @return the dbForRegId */
	public Integer getDbForRegId() {
		return this.dbForRegId;
	}

	/** @param dbForRegId the dbForRegId to set */
	public void setDbForRegId(Integer dbForRegId) {
		this.dbForRegId = dbForRegId;
	}

	/** @return the dbProcessed */
	public Integer getDbProcessed() {
		return this.dbProcessed;
	}

	/** @param dbProcessed the dbProcessed to set */
	public void setDbProcessed(Integer dbProcessed) {
		this.dbProcessed = dbProcessed;
	}

	/** @return the dbDocDate */
	public Date getDbDocDate() {
		return this.dbDocDate;
	}

	/** @param dbDocDate the dbDocDate to set */
	public void setDbDocDate(Date dbDocDate) {
		this.dbDocDate = dbDocDate;
	}

	/** @return the fromMail */
	public String getFromMail() {
		return this.fromMail;
	}

	/** @param fromMail the fromMail to set */
	public void setFromMail(String fromMail) {
		this.fromMail = fromMail;
	}

	/** @return the competence */
	public Integer getCompetence() {
		return this.competence;
	}

	/** @param competence the competence to set */
	public void setCompetence(Integer competence) {
		this.competence = competence;
	}

	/** @return the competenceText */
	public String getCompetenceText() {
		return this.competenceText;
	}

	/** @param competenceText the competenceText to set */
	public void setCompetenceText(String competenceText) {
		this.competenceText = competenceText;
	}

	@XmlTransient
	public List<Integer> getDbTopicList() {
		return this.dbTopicList;
	}

	public void setDbTopicList(List<Integer> dbTopicList) {
		this.dbTopicList = dbTopicList;
	}

	public List<Integer> getTopicList() {
		return this.topicList;
	}

	public void setTopicList(List<Integer> topicList) {
		this.topicList = topicList;
	}

	public Integer getProcDef() {
		return this.procDef;
	}

	public void setProcDef(Integer procDef) {
		this.procDef = procDef;
	}

	public Integer getProcExeStat() {
		return this.procExeStat;
	}

	public void setProcExeStat(Integer procExeStat) {
		this.procExeStat = procExeStat;
	}
	/** @return the dopdata */
	public DocDopdata getDopdata() {
		return this.dopdata;
	}
	/** @param dopdata the dopdata to set */
	public void setDopdata(DocDopdata dopdata) {
		this.dopdata = dopdata;
	}

	/** @return the preForRegId */
	public Integer getPreForRegId() {
		return this.preForRegId;
	}
	/** @param preForRegId the preForRegId to set */
	public void setPreForRegId(Integer preForRegId) {
		this.preForRegId = preForRegId;
	}

	@Override
	public String getIdentInfo() throws DbErrorException {
		if (SystemData.isDocPoredDeloGen() && this.poredDelo != null) {
			return rnDoc + "#" + this.poredDelo + "/" + new SimpleDateFormat("dd.MM.yyyy").format(docDate);
		}
		return rnDoc + "/" + new SimpleDateFormat("dd.MM.yyyy").format(docDate);
	}

	public Integer getSignMethod() {
		return signMethod;
	}

	public void setSignMethod(Integer signMethod) {
		this.signMethod = signMethod;
	}
	
	/** @return the poredDelo */
	public Integer getPoredDelo() {
		return this.poredDelo;
	}
	/** @param poredDelo the poredDelo to set */
	public void setPoredDelo(Integer poredDelo) {
		this.poredDelo = poredDelo;
	}

	/** @return the jalba */
	public DocJalba getJalba() {
		return this.jalba;
	}
	/** @param jalba the jalba to set */
	public void setJalba(DocJalba jalba) {
		this.jalba = jalba;
	}

	/** @return the docSpec */
	public DocSpec getDocSpec() {
		return this.docSpec;
	}
	/** @param docSpec the docSpec to set */
	public void setDocSpec(DocSpec docSpec) {
		this.docSpec = docSpec;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new  SystemJournal();				
		dj.setCodeObject(getCodeMainObject());
		dj.setIdObject(getId());
		dj.setIdentObject(getIdentInfo());
		return dj;
	}

	
}

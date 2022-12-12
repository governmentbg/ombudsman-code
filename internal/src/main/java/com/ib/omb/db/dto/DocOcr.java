package com.ib.omb.db.dto;

import com.github.jaiimageio.impl.plugins.tiff.TIFFImageReader;
import com.ib.omb.ocr.OCRUtils;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.exceptions.DbErrorException;
import org.apache.lucene.analysis.bg.BulgarianStemFilterFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Parameter;
import org.hibernate.search.annotations.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.ib.omb.system.OmbConstants.*;
import static javax.persistence.GenerationType.SEQUENCE;

/**
 * Документ
 *
 * @author belev
 */

//@AnalyzerDef(name = "defaultBG",
//		tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
//		filters = {
//				@TokenFilterDef(factory = LowerCaseFilterFactory.class),
//				@TokenFilterDef(factory = BulgarianStemFilterFactory.class)
//		})
//@AnalyzerDef(name = "NgramBG",
//		tokenizer = @TokenizerDef(factory = StandardTokenizerFactory.class),
//		filters = {
//				@TokenFilterDef(factory = LowerCaseFilterFactory.class),
//				@TokenFilterDef(factory = BulgarianStemFilterFactory.class),
//				@TokenFilterDef(factory = NGramFilterFactory.class,
//						params = {
//								@Parameter(name = "minGramSize", value = "2"),
//								@Parameter(name = "maxGramSize", value = "4")})
//		})
//@Entity
//@Table(name = "DOC")
//@Indexed
public class DocOcr extends TrackableEntity implements FullTextIndexedEntity{
	/**  */
	private static final long serialVersionUID = -4381087985324069934L;

	@SequenceGenerator(name = "Doc", sequenceName = "SEQ_DOC", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "Doc")
	@Column(name = "DOC_ID", unique = true, nullable = false)
	@Field(name = "document_id")
	@SortableField(forField = "document_id")
	private Integer id;

	@Field(analyze = Analyze.NO)
	@Column(name = "REGISTRATURA_ID")
	private Integer registraturaId;

	@Field
	@Column(name = "REGISTER_ID")
	private Integer registerId;

	@Column(name = "WORK_OFF_ID")
	private Integer workOffId;

	@Field
	@Column(name = "CODE_REF_CORRESP")
	private Integer codeRefCorresp;

	@Field(name = "rnDoc", analyze = Analyze.NO)
	@Field(name = "rnDocLike", analyzer = @Analyzer(definition = "NgramBG"))
	@Column(name = "RN_DOC")
	private String rnDoc;

	@Column(name = "RN_PREFIX")
	private String rnPrefix;

	@Column(name = "RN_PORED")
	private Integer rnPored;

	@Field(analyze = Analyze.NO)
	@Column(name = "GUID")
	private String guid;

	@Column(name = "FOR_REG_ID")
	private Integer forRegId;

	@Field(analyze = Analyze.NO)
	@Column(name = "DOC_TYPE")
	private Integer docType;

	@Field(analyze = Analyze.NO)
	@Column(name = "DOC_VID")
	private Integer docVid;

	@Field
	@DateBridge(resolution = Resolution.DAY)
	@Column(name = "DOC_DATE")
	private Date docDate;

//	@Field(analyzer = @Analyzer(definition = "defaultBG"))
	@Field()
	@Column(name = "DOC_NAME")
	private String docName;

//	@Field(analyzer = @Analyzer(definition = "defaultBG"))
	@Field()
	@Column(name = "DOC_INFO")
	private String docInfo;

//	@Field(analyzer = @Analyzer(definition = "defaultBG"))
	@Field()
	@Column(name = "OTNOSNO")
	private String otnosno;

	@Field
	@DateBridge(resolution = Resolution.SECOND)
	@Column(name = "RECEIVE_DATE")
	private Date receiveDate;

	@Field
	@Column(name = "RECEIVE_METHOD")
	private Integer receiveMethod;

//	@Field(analyzer = @Analyzer(definition = "defaultBG"))
	@Field()
	@Column(name = "RECEIVED_BY")
	private String receivedBy;

	@Field
	@Column(name = "DELIVERY_METHOD")
	private Integer deliveryMethod;

	@Field
	@DateBridge(resolution = Resolution.DAY)
	@Column(name = "WAIT_ANSWER_DATE")
	private Date waitAnswerDate;

	@Field
	@Column(name = "VALID")
	private Integer valid;

	@Field
	@DateBridge(resolution = Resolution.DAY)
	@Column(name = "VALID_DATE")
	private Date validDate;

	@Field(name = "tehNomer", analyze = Analyze.NO)
	@Field(name = "tehNomerLike", analyzer = @Analyzer(definition = "NgramBG"))
	@Column(name = "TEH_NOMER")
	private String tehNomer;

	@Field
	@DateBridge(resolution = Resolution.DAY)
	@Column(name = "TEH_DATE")
	private Date tehDate;

	@Field
	@Column(name = "STATUS")
	private Integer status;

	@Field
	@DateBridge(resolution = Resolution.DAY)
	@Column(name = "STATUS_DATE")
	private Date statusDate;

	@Field
	@Column(name = "URGENT")
	private Integer urgent;

	@Field
	@Column(name = "PROCESSED")
	private Integer processed;

	@Field
	@Column(name = "IRREGULAR")
	private Integer irregular;

	@Column(name = "FREE_ACCESS")
	private Integer freeAccess;

	@Column(name = "COUNT_FILES")
	private Integer countFiles;

	@Column(name = "COUNT_ORIGINALS")
	private Integer countOriginals;

	@Column(name = "COUNT_COPIES")
	private Integer countCopies;

	@Column(name = "COUNT_SHEETS")
	private Integer countSheets;

	@Column(name = "FROM_MAIL")
	private String fromMail;

	@Column(name = "COMPETENCE")
	private Integer competence;

	@Column(name = "COMPETENCE_TEXT")
	private String competenceText;

	@Transient
	private transient Boolean auditable; // за да може да се включва и изключва журналирането

	@Transient
	private transient List<DocReferent>	history;			// всички изтеглени от БД това няма смисъл да се сериализира при
															// журналиране
	@Transient
	private List<DocReferent>			referentsAuthor;	// автор ROLE_REF=1
	@Transient
	private List<DocReferent>			referentsAgreed;	// съгласувал ROLE_REF=2
	@Transient
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
	private List<Integer>			topicList;
	@Transient
	private transient List<Integer>	dbTopicList;	// темите каквито са били в БД

	@Transient
	private List<DocAccess>			docAccess;	// Даване на изричен достъп - насочване, за сведение или както там се реши да се казва

	@IndexedEmbedded
	@Field(name = "ocr",index = Index.YES,store = Store.NO)
	//На мястото на String моиже да е клас, който има само @Field анотации
	public List<String> getOcr(){
		OCRUtils ocrut = new OCRUtils();
		List<String> ocrResults=new ArrayList<>();
		FilesDAO daoF = new FilesDAO(UserData.DEFAULT);
		try {

			List<Files> filesList = daoF.selectByFileObjectDop(this.id, CODE_ZNACHENIE_JOURNAL_DOC); // използва се този метод за зареждане на файлове, за да се заредят доп. полета - лични дании, официален док и т.н.

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

	public void setOcr(List<String> ocrItems){

	}

	public List<DocAccess> getDocAccess() {
		return docAccess;
	}

	public void setDocAccess(List<DocAccess> docAccess) {
		this.docAccess = docAccess;
	}

	/**  */
	public DocOcr() {
		super();
	}

	/** */
	@Override
	@Field
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

	public enum FullTextFields{
		otnosno,
		docName,
		docInfo,
		rnDocLike,
		tehNomerLike,
		ocr
	}

	public enum FilterFields{
		rnDoc,
		tehNomer,
		docDateOt,
		docDateDo,
		docType,
		registraturaId,
		docVid,
		registerId,
		userReg,
		valid,
		urgent,
		codeRefCorresp,
		receiveDateOt,
		receiveDateDo,
		receiveMethod,
		waitAnswerDateOt,
		waitAnswerDateDo,
		guid
	}
}
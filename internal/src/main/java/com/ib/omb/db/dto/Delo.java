package com.ib.omb.db.dto;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ACTIVE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_TYPE_DOSS;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_14;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_DA;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_NE;
import static javax.persistence.GenerationType.SEQUENCE;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Преписка
 *
 * @author belev
 */
@Entity
@Table(name = "DELO")
//@Indexed
public class Delo extends TrackableEntity implements FullTextIndexedEntity, AuditExt {
	/**  */
	private static final long serialVersionUID = 6434504024864981438L;

	@SequenceGenerator(name = "Delo", sequenceName = "SEQ_DELO", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "Delo")
	@Column(name = "DELO_ID", unique = true, nullable = false)
//	@Field(name= "delo_id")
	private Integer id;

//	@Field
	@Column(name = "REGISTRATURA_ID")  
	@JournalAttr(label="registraturaId",defaultText = "Регистратура",classifID = ""+OmbConstants.CODE_CLASSIF_REGISTRATURI)
	private Integer registraturaId;

	@Column(name = "INIT_DOC_ID")
	@JournalAttr(label="initDocId",defaultText = "Иницииращ док.", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer initDocId;

	@Column(name = "CODE_REF_LEAD")
	@JournalAttr(label = "codeRefLead",defaultText = "Водещ служител", classifID = ""+OmbConstants.CODE_CLASSIF_ADMIN_STR)
	private Integer codeRefLead; //Водещ служител

//	@Field(name = "rnDelo", analyze = Analyze.NO)	
//	@Field(name = "rnDeloLike", analyzer = @Analyzer(definition = "NgramBG"))  // Номер на преписка
	@Column(name = "RN_DELO")
	@JournalAttr(label = "rnDelo",defaultText = "Номер на преписка")
	private String rnDelo;

	@Column(name = "RN_PREFIX")
	@JournalAttr(label="rnPrefix",defaultText = "Префикс на номера")
	private String rnPrefix;

	@Column(name = "RN_PORED")
	@JournalAttr(label="rnPored",defaultText = "Пореден номер от номера")
	private Integer rnPored;
	
//	@Field
//	@DateBridge(resolution = Resolution.DAY)
	@Column(name = "DELO_DATE")   // Период на регистрация
	@JournalAttr(label = "deloDate", defaultText = "Дата")
	private Date deloDate;

	@Column(name = "DELO_YEAR")
	@JournalAttr(label="deloYear",defaultText = "Година")
	private Integer deloYear;

//	@Field
	@Column(name = "DELO_TYPE")  //Тип
	@JournalAttr(label = "deloType", defaultText = "Тип",classifID = ""+OmbConstants.CODE_CLASSIF_DELO_TYPE)
	private Integer deloType;

//	@Field(analyzer = @Analyzer(definition = "defaultBG"))
//	@Field
	@Column(name = "DELO_NAME") 
	@JournalAttr(label = "deloName",defaultText = "Наименование")
	private String deloName;   //Наименование
	
//	@Field(analyzer = @Analyzer(definition = "defaultBG"))
//	@Field
	@Column(name = "DELO_INFO")
	@JournalAttr(label = "deloInfo",defaultText = "Относно")
	private String deloInfo;    // Описание

	@Column(name = "FREE_ACCESS")
	@JournalAttr(label="freeAccess",defaultText = "Свободен достъп", classifID = ""+ OmbConstants.CODE_CLASSIF_DANE)
	private Integer freeAccess;

//	@Field
	@Column(name = "STATUS")  //Статус
	@JournalAttr(label = "status",defaultText = "Статус",classifID = ""+OmbConstants.CODE_CLASSIF_DELO_STATUS)
	private Integer status;

//	@Field
//	@DateBridge(resolution = Resolution.DAY)
	@Column(name = "STATUS_DATE")  //Период на статуса
	@JournalAttr(label="statusDate",defaultText = "Дата на статус")
	private Date statusDate;

	@Column(name = "WITH_SECTION")
	@JournalAttr(label = "withSection",defaultText = "с раздели", classifID = ""+ OmbConstants.CODE_CLASSIF_DANE)
	private Integer withSection;

	@Column(name = "WITH_TOM")
	@JournalAttr(label = "withTom",defaultText = "С томове", classifID = ""+ OmbConstants.CODE_CLASSIF_DANE)
	private Integer withTom;

	@Column(name = "BR_TOM")
	@JournalAttr(label = "brTom",defaultText = "Брой томове")
	private Integer brTom;

	@Column(name = "MAX_BR_SHEETS")
	@JournalAttr(label = "maxBrSheets",defaultText = "Макс. Бр. листа в том")
	private Integer maxBrSheets;

	@Column(name = "PREV_NOM_DELO")
	@JournalAttr(label = "prevNomDelo",defaultText = "Извадена от")
	private String prevNomDelo;

	@Transient
	private transient Boolean auditable; // за да може да се включва и изключва журналирането

	@Transient
	private DeloDelo deloDelo; // това е информация за преписката, в която е вложена текущата

	@Transient
	private transient Integer	dbStatus;
	@Transient
	private transient Integer	dbDeloType;
	@Transient
	private transient String	dbRnDelo;
	@Transient
	private transient Integer	dbDeloYear;
	@Transient
	private transient Integer 	dbCodeRefLead;
	
	@Transient
	private Object[]	protocolData;	// това са данните когато преписката е архивирана и има протокол за архивиране
	
	@Transient
	//@JournalAttr(label="deloAccess",defaultText = "Изричен достъп")
	private List<DeloAccess> deloAccess;	// Даване на изричен достъп - насочване, за сведение или както там се реши да се казва
	

	

	/** */
	public Delo() {
		super();
	}

	/**
	 * Сетва на преписката полетатат от иницииращия документ и с дефолтни настройки.
	 *
	 * @param initDoc
	 * @param sd
	 * @throws DbErrorException
	 */
	public Delo(Doc initDoc, SystemData sd) throws DbErrorException {
		this.registraturaId = initDoc.getRegistraturaId();
		this.initDocId = initDoc.getId();

		this.rnDelo = initDoc.getRnDoc();
		this.rnPrefix = initDoc.getRnPrefix();
		this.rnPored = initDoc.getRnPored();

		this.deloDate = initDoc.getDocDate();
		this.deloName = initDoc.getOtnosno();

		// за омбудсмана трябва да се създаде тип преписка спрямо вид документ
		if (initDoc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA)) {
			this.deloType = OmbConstants.CODE_ZNACHENIE_DELO_TYPE_JALBA;
			if (initDoc.getJalba() != null) {
				this.deloName = "Жалбоподател: " + initDoc.getJalba().getJbpName();
			}
		} else if (initDoc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM)) {
			this.deloType = OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NPM;
		} else if (initDoc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS)) {
			this.deloType = OmbConstants.CODE_ZNACHENIE_DELO_TYPE_SAMOS;
		} else {
			this.deloType = sd.getRegistraturaSetting(initDoc.getRegistraturaId(), CODE_ZNACHENIE_REISTRATURA_SETTINGS_14);
			if (this.deloType == null || this.deloType.equals(0)) { // ако няма такава настройка все пак да не остане празно
				this.deloType = CODE_ZNACHENIE_DELO_TYPE_DOSS;
			}
		}

		this.freeAccess = initDoc.getFreeAccess();

		this.status = CODE_ZNACHENIE_DELO_STATUS_ACTIVE;
		this.statusDate = initDoc.getDocDate();

		String withSections = sd.getSettingsValue("delo.deloWithSections");
		this.withSection = "1".equals(withSections) ? CODE_ZNACHENIE_DA : CODE_ZNACHENIE_NE;

		String withToms = sd.getSettingsValue("delo.deloWithToms");
		this.withTom = "1".equals(withToms) ? CODE_ZNACHENIE_DA : CODE_ZNACHENIE_NE;
		
		this.brTom = 1;
		
		String max = sd.getSettingsValue("delo.deloMaxBrSheetsDefault");
		try {
			this.maxBrSheets = Integer.valueOf(max);
		} catch (Exception e) {
			this.maxBrSheets = 250; // втори default ако е няма настройката
		}
	}

	/** @return the brTom */
	public Integer getBrTom() {
		return this.brTom;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return CODE_ZNACHENIE_JOURNAL_DELO;
	}

	/** @return the codeRefLead */
	public Integer getCodeRefLead() {
		return this.codeRefLead;
	}

	/** @return the dbDeloType */
	public Integer getDbDeloType() {
		return this.dbDeloType;
	}

	/** @return the dbDeloYear */
	public Integer getDbDeloYear() {
		return this.dbDeloYear;
	}

	/** @return the dbRnDelo */
	public String getDbRnDelo() {
		return this.dbRnDelo;
	}

	/** @return the dbStatus */
	public Integer getDbStatus() {
		return this.dbStatus;
	}

	/** @return the deloDate */
	public Date getDeloDate() {
		return this.deloDate;
	}

	/** @return the deloDelo */
	public DeloDelo getDeloDelo() {
		return this.deloDelo;
	}

	/** @return the deloInfo */
	public String getDeloInfo() {
		return this.deloInfo;
	}

	/** @return the deloName */
	public String getDeloName() {
		return this.deloName;
	}

	/** @return the deloType */
	public Integer getDeloType() {
		return this.deloType;
	}

	/** @return the deloYear */
	public Integer getDeloYear() {
		return this.deloYear;
	}

	/** @return the freeAccess */
	public Integer getFreeAccess() {
		return this.freeAccess;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the initDocId */
	public Integer getInitDocId() {
		return this.initDocId;
	}

	/** @return the maxBrSheets */
	public Integer getMaxBrSheets() {
		return this.maxBrSheets;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @return the rnDelo */
	public String getRnDelo() {
		return this.rnDelo;
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

	/** @return the statusDate */
	public Date getStatusDate() {
		return this.statusDate;
	}

	/** @return the withSection */
	public Integer getWithSection() {
		return this.withSection;
	}

	/** @return the withTom */
	public Integer getWithTom() {
		return this.withTom;
	}

	/** @return the auditable */
	@Override
	public boolean isAuditable() {
		return this.auditable == null ? super.isAuditable() : this.auditable.booleanValue();
	}

	/** @param auditable the auditable to set */
	public void setAuditable(Boolean auditable) {
		this.auditable = auditable;
	}

	/** @param brTom the brTom to set */
	public void setBrTom(Integer brTom) {
		this.brTom = brTom;
	}

	/** @param codeRefLead the codeRefLead to set */
	public void setCodeRefLead(Integer codeRefLead) {
		this.codeRefLead = codeRefLead;
	}

	/** @param dbDeloType the dbDeloType to set */
	public void setDbDeloType(Integer dbDeloType) {
		this.dbDeloType = dbDeloType;
	}

	/** @param dbDeloYear the dbDeloYear to set */
	public void setDbDeloYear(Integer dbDeloYear) {
		this.dbDeloYear = dbDeloYear;
	}

	/** @param dbRnDelo the dbRnDelo to set */
	public void setDbRnDelo(String dbRnDelo) {
		this.dbRnDelo = dbRnDelo;
	}

	/** @param dbStatus the dbStatus to set */
	public void setDbStatus(Integer dbStatus) {
		this.dbStatus = dbStatus;
	}

	/** @param deloDate the deloDate to set */
	public void setDeloDate(Date deloDate) {
		this.deloDate = deloDate;
	}

	/** @param deloDelo the deloDelo to set */
	public void setDeloDelo(DeloDelo deloDelo) {
		this.deloDelo = deloDelo;
	}

	/** @param deloInfo the deloInfo to set */
	public void setDeloInfo(String deloInfo) {
		this.deloInfo = deloInfo;
	}

	/** @param deloName the deloName to set */
	public void setDeloName(String deloName) {
		this.deloName = deloName;
	}

	/** @param deloType the deloType to set */
	public void setDeloType(Integer deloType) {
		this.deloType = deloType;
	}

	/** @param deloYear the deloYear to set */
	public void setDeloYear(Integer deloYear) {
		this.deloYear = deloYear;
	}

	/** @param freeAccess the freeAccess to set */
	public void setFreeAccess(Integer freeAccess) {
		this.freeAccess = freeAccess;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param initDocId the initDocId to set */
	public void setInitDocId(Integer initDocId) {
		this.initDocId = initDocId;
	}

	/** @param maxBrSheets the maxBrSheets to set */
	public void setMaxBrSheets(Integer maxBrSheets) {
		this.maxBrSheets = maxBrSheets;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** @param rnDelo the rnDelo to set */
	public void setRnDelo(String rnDelo) {
		this.rnDelo = rnDelo;
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

	/** @param statusDate the statusDate to set */
	public void setStatusDate(Date statusDate) {
		this.statusDate = statusDate;
	}

	/** @param withSection the withSection to set */
	public void setWithSection(Integer withSection) {
		this.withSection = withSection;
	}

	/** @param withTom the withTom to set */
	public void setWithTom(Integer withTom) {
		this.withTom = withTom;
	}
	
	public List<DeloAccess> getDeloAccess() {
		return deloAccess;
	}

	public void setDeloAccess(List<DeloAccess> deloAccess) {
		this.deloAccess = deloAccess;
	}

	/** @return the protocolData */
	public Object[] getProtocolData() {
		return this.protocolData;
	}

	/** @param protocolData the protocolData to set */
	public void setProtocolData(Object[] protocolData) {
		this.protocolData = protocolData;
	}
	
	/** @return the dbCodeRefLead */
	public Integer getDbCodeRefLead() {
		return this.dbCodeRefLead;
	}

	/** @param dbCodeRefLead the dbCodeRefLead to set */
	public void setDbCodeRefLead(Integer dbCodeRefLead) {
		this.dbCodeRefLead = dbCodeRefLead;
	}
	
	/** @return the prevNomDelo */
	public String getPrevNomDelo() {
		return this.prevNomDelo;
	}

	/** @param prevNomDelo the prevNomDelo to set */
	public void setPrevNomDelo(String prevNomDelo) {
		this.prevNomDelo = prevNomDelo;
	}

	//За пълнотесктово търсене
	public enum FullTextFields{
		rnDeloLike,
		deloName,
		deloInfo
	}

	public enum FilterFields{
		rnDelo,
		deloDateFrom,
		deloDateTo,
		registraturaId,
		codeRefLeadList,
		statusDateFrom,
		statusDateTo
	}
	
	@Override
	public String getIdentInfo() throws DbErrorException {
		return rnDelo + "/" + new SimpleDateFormat("dd.MM.yyyy").format(deloDate);
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
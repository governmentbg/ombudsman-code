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
 * Характеристика на вид документ
 *
 * @author belev
 */
@Entity
@Table(name = "DOC_VID_SETTINGS")
public class DocVidSetting extends TrackableEntity implements AuditExt {
	/**  */
	private static final long serialVersionUID = -8360616627093473644L;

	@SequenceGenerator(name = "DocVidSetting", sequenceName = "SEQ_DOC_VID_SETTINGS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DocVidSetting")
	@Column(name = "id", unique = true, nullable = false)
	private Integer id;

//	@JournalAttr(label = "registraturaId", defaultText = "ИД на регистратура")
	@Column(name = "REGISTRATURA_ID", updatable = false)
	private Integer registraturaId;

	@JournalAttr(label = "docVidSetting.shemaId", defaultText = "Индекс на номенклатурно дело за съхранение на документ от този вид", classifID = "" + OmbConstants.CODE_CLASSIF_DOC_SHEMA)
	@Column(name = "SHEMA_ID")
	private Integer shemaId;

	@JournalAttr(label = "docVidSetting.typeDoc", defaultText = "Вид документ", classifID = "" + OmbConstants.CODE_CLASSIF_DOC_VID)
	@Column(name = "DOC_VID")
	private Integer docVid;

	@JournalAttr(label = "docu.register", defaultText = "Регистър", classifID = "" + OmbConstants.CODE_CLASSIF_REGISTRI)
	@Column(name = "REGISTER_ID")
	private Integer registerId;

	@JournalAttr(label = "register.prefix", defaultText = "Индекс")
	@Column(name = "PREFIX")
	private String prefix;

	@JournalAttr(label = "register.begNum", defaultText = "Начален №")
	@Column(name = "BEG_NOMER")
	private Integer begNomer;

	@JournalAttr(label = "register.actNum", defaultText = "Достигнат №")
	@Column(name = "ACT_NOMER")
	private Integer actNomer;

	@JournalAttr(label = "register.step", defaultText = "Стъпка")
	@Column(name = "STEP")
	private Integer step;

	@JournalAttr(label = "docVidSetting.regOfficDocThisType", defaultText = "Регистратура за заявка за деловодна регистрация", classifID = "" + OmbConstants.CODE_CLASSIF_REGISTRATURI)
	@Column(name = "FOR_REG_ID")
	private Integer forRegId;

	@JournalAttr(label = "docVidSetting.createDelo", defaultText = "Инициира преписка", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "CREATE_DELO")
	private Integer createDelo;

	@JournalAttr(label = "docVidSetting.docProceed", defaultText = "Начин на обработка на документ", classifID = "" + OmbConstants.CODE_CLASSIF_DOC_PROCEED)
	@Column(name = "DOC_VID_OKA")
	private Integer docVidOka;

	@JournalAttr(label = "docVidSettings.docVidUri", defaultText = "УРИ на вид документ")
	@Column(name = "DOC_VID_URI")
	private String docVidUri;

	@JournalAttr(label = "docVidSettings.prilUri", defaultText = "УРИ на приложение за визуализация и/или за редактиране")
	@Column(name = "PRIL_URI")
	private String prilUri;

	@JournalAttr(label = "docu.note", defaultText = "Забележка")
	@Column(name = "DOC_VID_INFO")
	private String docVidInfo;

	@JournalAttr(label = "docVidSetting.personSign", defaultText = "Овластено лице, което може да подписва този вид документ", classifID = "" + Constants.CODE_CLASSIF_POSITION)
	@Column(name = "EMPL_POSITION")
	private Integer emplPosition;

	@JournalAttr(label = "missing.procDefIn", defaultText = "Стартира процедура: за входящ документ", classifID = "" + OmbConstants.CODE_CLASSIF_PROCEDURI)
	@Column(name = "PROC_DEF_IN")
	private Integer procDefIn;

	@JournalAttr(label = "missing.procDefOwn", defaultText = "Стартира процедура: за собствен документ", classifID = "" + OmbConstants.CODE_CLASSIF_PROCEDURI)
	@Column(name = "PROC_DEF_OWN")
	private Integer procDefOwn;

	@JournalAttr(label = "missing.procDefWork", defaultText = "Стартира процедура: за работен документ", classifID = "" + OmbConstants.CODE_CLASSIF_PROCEDURI)
	@Column(name = "PROC_DEF_WORK")
	private Integer procDefWork;

	@JournalAttr(label = "docVidSettings.characterSpecDoc", defaultText = "Характер на специализиран документ", classifID = "" + OmbConstants.CODE_CLASSIF_CHARACTER_SPEC_DOC)
	@Column(name = "DOC_HAR")
	private Integer docHar;

	@JournalAttr(label = "docVidSettings.memberActive", defaultText = "Поддържа се списък с участници в документа", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "MEMBERS_ACTIVE")
	private Integer membersActive;

	@JournalAttr(label = "docVidSettings.nameMemberList", defaultText = "Име на списъка с участници в документа")
	@Column(name = "MEMBERS_TAB")
	private String membersTab;

	@Transient
	private transient String dbPrefix;

	/** */
	public DocVidSetting() {
		super();
	}

	/** @return the actNomer */
	public Integer getActNomer() {
		return this.actNomer;
	}

	/** @return the begNomer */
	public Integer getBegNomer() {
		return this.begNomer;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_VID_SETT;
	}

	/** @return the createDelo */
	public Integer getCreateDelo() {
		return this.createDelo;
	}

	/** @return the dbPrefix */
	public String getDbPrefix() {
		return this.dbPrefix;
	}

	/** @return the docHar */
	public Integer getDocHar() {
		return this.docHar;
	}

	/** @return the docVid */
	public Integer getDocVid() {
		return this.docVid;
	}

	/** @return the docVidInfo */
	public String getDocVidInfo() {
		return this.docVidInfo;
	}

	/** @return the docVidOka */
	public Integer getDocVidOka() {
		return this.docVidOka;
	}

	/** @return the docVidUri */
	public String getDocVidUri() {
		return this.docVidUri;
	}

	/** @return the emplPosition */
	public Integer getEmplPosition() {
		return this.emplPosition;
	}

	/** @return the forRegId */
	public Integer getForRegId() {
		return this.forRegId;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the membersActive */
	public Integer getMembersActive() {
		return this.membersActive;
	}

	/** @return the membersTab */
	public String getMembersTab() {
		return this.membersTab;
	}

	/** @return the prefix */
	public String getPrefix() {
		return this.prefix;
	}

	/** @return the prilUri */
	public String getPrilUri() {
		return this.prilUri;
	}

	/** @return the procDefIn */
	public Integer getProcDefIn() {
		return this.procDefIn;
	}

	/** @return the procDefOwn */
	public Integer getProcDefOwn() {
		return this.procDefOwn;
	}

	/** @return the procDefWork */
	public Integer getProcDefWork() {
		return this.procDefWork;
	}

	/** @return the registerId */
	public Integer getRegisterId() {
		return this.registerId;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @return the shemaId */
	public Integer getShemaId() {
		return this.shemaId;
	}

	/** @return the step */
	public Integer getStep() {
		return this.step;
	}

	/** @param actNomer the actNomer to set */
	public void setActNomer(Integer actNomer) {
		this.actNomer = actNomer;
	}

	/** @param begNomer the begNomer to set */
	public void setBegNomer(Integer begNomer) {
		this.begNomer = begNomer;
	}

	/** @param createDelo the createDelo to set */
	public void setCreateDelo(Integer createDelo) {
		this.createDelo = createDelo;
	}

	/** @param dbPrefix the dbPrefix to set */
	public void setDbPrefix(String dbPrefix) {
		this.dbPrefix = dbPrefix;
	}

	/** @param docHar the docHar to set */
	public void setDocHar(Integer docHar) {
		this.docHar = docHar;
	}

	/** @param docVid the docVid to set */
	public void setDocVid(Integer docVid) {
		this.docVid = docVid;
	}

	/** @param docVidInfo the docVidInfo to set */
	public void setDocVidInfo(String docVidInfo) {
		this.docVidInfo = docVidInfo;
	}

	/** @param docVidOka the docVidOka to set */
	public void setDocVidOka(Integer docVidOka) {
		this.docVidOka = docVidOka;
	}

	/** @param docVidUri the docVidUri to set */
	public void setDocVidUri(String docVidUri) {
		this.docVidUri = docVidUri;
	}

	/** @param emplPosition the emplPosition to set */
	public void setEmplPosition(Integer emplPosition) {
		this.emplPosition = emplPosition;
	}

	/** @param forRegId the forRegId to set */
	public void setForRegId(Integer forRegId) {
		this.forRegId = forRegId;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param membersActive the membersActive to set */
	public void setMembersActive(Integer membersActive) {
		this.membersActive = membersActive;
	}

	/** @param membersTab the membersTab to set */
	public void setMembersTab(String membersTab) {
		this.membersTab = membersTab;
	}

	/** @param prefix the prefix to set */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/** @param prilUri the prilUri to set */
	public void setPrilUri(String prilUri) {
		this.prilUri = prilUri;
	}

	/** @param procDefIn the procDefIn to set */
	public void setProcDefIn(Integer procDefIn) {
		this.procDefIn = procDefIn;
	}

	/** @param procDefOwn the procDefOwn to set */
	public void setProcDefOwn(Integer procDefOwn) {
		this.procDefOwn = procDefOwn;
	}

	/** @param procDefWork the procDefWork to set */
	public void setProcDefWork(Integer procDefWork) {
		this.procDefWork = procDefWork;
	}

	/** @param registerId the registerId to set */
	public void setRegisterId(Integer registerId) {
		this.registerId = registerId;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** @param shemaId the shemaId to set */
	public void setShemaId(Integer shemaId) {
		this.shemaId = shemaId;
	}

	/** @param step the step to set */
	public void setStep(Integer step) {
		this.step = step;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA);
		journal.setJoinedIdObject1(this.registraturaId);

		return journal;
	}
}
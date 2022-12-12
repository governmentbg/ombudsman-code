package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.List;

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
 * Дефиниция на етап от Процедура
 *
 * @author belev
 */
@Entity
@Table(name = "PROC_DEF_ETAP")
public class ProcDefEtap extends TrackableEntity implements AuditExt {

	/**  */
	private static final long serialVersionUID = 7629791275777324629L;

	@SequenceGenerator(name = "ProcDefEtap", sequenceName = "SEQ_PROC_DEF_ETAP", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "ProcDefEtap")
	@Column(name = "DEF_ETAP_ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "DEF_ID", updatable = false)
	private Integer defId;

	@Column(name = "NOMER")
	@JournalAttr(label = "nomer", defaultText = "Номер")
	private Integer nomer;

	@Column(name = "NEXT_OK")
	@JournalAttr(label = "nextOk", defaultText = "Следващи етапи (при ДА)")
	private String nextOk;

	@Column(name = "NEXT_NOT")
	@JournalAttr(label = "nextNot", defaultText = "Следващи етапи (при НЕ)")
	private String nextNot;

	@Column(name = "NEXT_OPTIONAL")
	@JournalAttr(label = "nextOptional", defaultText = "Следващи етапи-опционални")
	private String nextOptional;

	@Column(name = "ETAP_NAME")
	@JournalAttr(label = "etapName", defaultText = "Наименование")
	private String etapName;

	@Column(name = "ETAP_INFO")
	@JournalAttr(label = "etapInfo", defaultText = "Описание")
	private String etapInfo;

	@Column(name = "SROK_DAYS")
	@JournalAttr(label = "srokDays", defaultText = "Срок в дни")
	private Integer srokDays;

	@Column(name = "SROK_HOURS")
	@JournalAttr(label = "srokHours", defaultText = "Срок в часове")
	private Integer srokHours;

	@Column(name = "INSTRUCTIONS")
	@JournalAttr(label = "instructions", defaultText = "Указания за изпълнение")
	private String instructions;

	@Column(name = "URI_ETAP")
	@JournalAttr(label = "uriEtap", defaultText = "УРИ на етап")
	private String uriEtap;

	@Column(name = "CODE_REF")
	@JournalAttr(label = "codeRef", defaultText = "Контролиращ - Служител", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private Integer codeRef;

	@Column(name = "ZVENO")
	@JournalAttr(label = "zveno", defaultText = "Контролиращ - Звено", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private Integer zveno;

	@Column(name = "EMPL_POSITION")
	@JournalAttr(label = "emplPosition", defaultText = "Контролиращ - Длъжност", classifID = "" + Constants.CODE_CLASSIF_POSITION)
	private Integer emplPosition;

	@Column(name = "BUSINESS_ROLE")
	@JournalAttr(label = "businessRole", defaultText = "Контролиращ - Бизнес роля", classifID = "" + OmbConstants.CODE_CLASSIF_PROC_BUSINESS_ROLE)
	private Integer businessRole;

	@Column(name = "IS_MERGE")
	@JournalAttr(label = "isMerge", defaultText = "Събирателен", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	private Integer isMerge;

	@Column(name = "ETAP_DOC_MODE")
	@JournalAttr(label = "etapDocMode", defaultText = "Тип стартиращ документ ", classifID = "" + OmbConstants.CODE_CLASSIF_ETAP_DOC_MODE)
	private Integer etapDocMode;

//	[0]-id; [1]-nomer; [2]-etapName
	@Transient
	private transient List<Object[]>	nextOkList;
	@Transient
	private transient List<Object[]>	nextNotList;
	@Transient
	private transient List<Object[]>	nextOptionalList;

	@Transient
	private transient String	dbNextOk;
	@Transient
	private transient String	dbNextNot;
	@Transient
	private transient String	dbNextOptional;

	/** */
	public ProcDefEtap() {
		super();
	}

	/** @return the businessRole */
	public Integer getBusinessRole() {
		return this.businessRole;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_DEF_ETAP;
	}

	/** @return the codeRef */
	public Integer getCodeRef() {
		return this.codeRef;
	}

	/** @return the dbNextNot */
	public String getDbNextNot() {
		return this.dbNextNot;
	}

	/** @return the dbNextOk */
	public String getDbNextOk() {
		return this.dbNextOk;
	}

	/** @return the dbNextOptional */
	public String getDbNextOptional() {
		return this.dbNextOptional;
	}

	/** @return the defId */
	public Integer getDefId() {
		return this.defId;
	}

	/** @return the emplPosition */
	public Integer getEmplPosition() {
		return this.emplPosition;
	}

	/** @return the etapDocMode */
	public Integer getEtapDocMode() {
		return this.etapDocMode;
	}

	/** @return the etapInfo */
	public String getEtapInfo() {
		return this.etapInfo;
	}

	/** @return the etapName */
	public String getEtapName() {
		return this.etapName;
	}

	/** @return the id */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		return "ИД Деф.Процедура: " + this.defId + ", Етап № " + this.nomer + " " + this.etapName;
	}

	/** @return the instructions */
	public String getInstructions() {
		return this.instructions;
	}

	/** @return the isMerge */
	public Integer getIsMerge() {
		return this.isMerge;
	}

	/** @return the nextNot */
	public String getNextNot() {
		return this.nextNot;
	}

	/** @return the nextNotList */
	public List<Object[]> getNextNotList() {
		return this.nextNotList;
	}

	/** @return the nextOk */
	public String getNextOk() {
		return this.nextOk;
	}

	/** @return the nextOkList */
	public List<Object[]> getNextOkList() {
		return this.nextOkList;
	}

	/** @return the nextOptional */
	public String getNextOptional() {
		return this.nextOptional;
	}

	/** @return the nextOptionalList */
	public List<Object[]> getNextOptionalList() {
		return this.nextOptionalList;
	}

	/** @return the nomer */
	public Integer getNomer() {
		return this.nomer;
	}

	/** @return the srokDays */
	public Integer getSrokDays() {
		return this.srokDays;
	}

	/** @return the srokHours */
	public Integer getSrokHours() {
		return this.srokHours;
	}

	/** @return the uriEtap */
	public String getUriEtap() {
		return this.uriEtap;
	}

	/** @return the zveno */
	public Integer getZveno() {
		return this.zveno;
	}

	/** @param businessRole the businessRole to set */
	public void setBusinessRole(Integer businessRole) {
		this.businessRole = businessRole;
	}

	/** @param codeRef the codeRef to set */
	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	/** @param dbNextNot the dbNextNot to set */
	public void setDbNextNot(String dbNextNot) {
		this.dbNextNot = dbNextNot;
	}

	/** @param dbNextOk the dbNextOk to set */
	public void setDbNextOk(String dbNextOk) {
		this.dbNextOk = dbNextOk;
	}

	/** @param dbNextOptional the dbNextOptional to set */
	public void setDbNextOptional(String dbNextOptional) {
		this.dbNextOptional = dbNextOptional;
	}

	/** @param defId the defId to set */
	public void setDefId(Integer defId) {
		this.defId = defId;
	}

	/** @param emplPosition the emplPosition to set */
	public void setEmplPosition(Integer emplPosition) {
		this.emplPosition = emplPosition;
	}

	/** @param etapDocMode the etapDocMode to set */
	public void setEtapDocMode(Integer etapDocMode) {
		this.etapDocMode = etapDocMode;
	}

	/** @param etapInfo the etapInfo to set */
	public void setEtapInfo(String etapInfo) {
		this.etapInfo = etapInfo;
	}

	/** @param etapName the etapName to set */
	public void setEtapName(String etapName) {
		this.etapName = etapName;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param instructions the instructions to set */
	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	/** @param isMerge the isMerge to set */
	public void setIsMerge(Integer isMerge) {
		this.isMerge = isMerge;
	}

	/** @param nextNot the nextNot to set */
	public void setNextNot(String nextNot) {
		this.nextNot = nextNot;
	}

	/** @param nextNotList the nextNotList to set */
	public void setNextNotList(List<Object[]> nextNotList) {
		this.nextNotList = nextNotList;
	}

	/** @param nextOk the nextOk to set */
	public void setNextOk(String nextOk) {
		this.nextOk = nextOk;
	}

	/** @param nextOkList the nextOkList to set */
	public void setNextOkList(List<Object[]> nextOkList) {
		this.nextOkList = nextOkList;
	}

	/** @param nextOptional the nextOptional to set */
	public void setNextOptional(String nextOptional) {
		this.nextOptional = nextOptional;
	}

	/** @param nextOptionalList the nextOptionalList to set */
	public void setNextOptionalList(List<Object[]> nextOptionalList) {
		this.nextOptionalList = nextOptionalList;
	}

	/** @param nomer the nomer to set */
	public void setNomer(Integer nomer) {
		this.nomer = nomer;
	}

	/** @param srokDays the srokDays to set */
	public void setSrokDays(Integer srokDays) {
		this.srokDays = srokDays;
	}

	/** @param srokHours the srokHours to set */
	public void setSrokHours(Integer srokHours) {
		this.srokHours = srokHours;
	}

	/** @param uriEtap the uriEtap to set */
	public void setUriEtap(String uriEtap) {
		this.uriEtap = uriEtap;
	}

	/** @param zveno the zveno to set */
	public void setZveno(Integer zveno) {
		this.zveno = zveno;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new SystemJournal();
		dj.setCodeObject(getCodeMainObject());
		dj.setIdObject(getId());
		dj.setIdentObject(getIdentInfo());
		dj.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_DEF);
		dj.setJoinedIdObject1(this.defId);
		return dj;
	}
}
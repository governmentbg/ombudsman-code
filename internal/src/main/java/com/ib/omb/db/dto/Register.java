package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Регистър
 *
 * @author belev
 */
@Entity
@Table(name = "REGISTRI")
public class Register extends TrackableEntity implements AuditExt {
	/**  */
	private static final long serialVersionUID = 7361881004328869836L;

	@SequenceGenerator(name = "Register", sequenceName = "SEQ_REGISTRI", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "Register")
	@Column(name = "REGISTER_ID", unique = true, nullable = false)
	private Integer id;

//	@JournalAttr(label = "registraturaId", defaultText = "ИД на регистратура")
	@Column(name = "REGISTRATURA_ID", updatable = false)
	private Integer registraturaId;

	@JournalAttr(label = "register.naim", defaultText = "Наименование на регистър")
	@Column(name = "REGISTER")
	private String register;

	@JournalAttr(label = "register.tipReg", defaultText = "Тип регистър", classifID = "" + OmbConstants.CODE_CLASSIF_REGISTER_TYPE)
	@Column(name = "REGISTER_TYPE")
	private Integer registerType;

	@JournalAttr(label = "register.active", defaultText = "Активен", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "VALID")
	private Integer valid;

	@JournalAttr(label = "register.commonForMoreReg", defaultText = "Общ за повече регистратури", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "COMMON")
	private Integer common;

	@JournalAttr(label = "register.tipDoc", defaultText = "Тип документ", classifID = "" + OmbConstants.CODE_CLASSIF_DOC_TYPE)
	@Column(name = "DOC_TYPE")
	private Integer docType;

	@JournalAttr(label = "register.prefix", defaultText = "Индекс")
	@Column(name = "PREFIX")
	private String prefix;

	@JournalAttr(label = "register.alg", defaultText = "Алгоритъм", classifID = "" + OmbConstants.CODE_CLASSIF_ALG)
	@Column(name = "ALG")
	private Integer alg;

	@JournalAttr(label = "register.begNum", defaultText = "Начален №")
	@Column(name = "BEG_NOMER")
	private Integer begNomer;

	@JournalAttr(label = "register.actNum", defaultText = "Достигнат №")
	@Column(name = "ACT_NOMER")
	private Integer actNomer;

	@JournalAttr(label = "register.step", defaultText = "Стъпка")
	@Column(name = "STEP")
	private Integer step;

	@JournalAttr(label = "register.sortNum", defaultText = "Пореден № за сортиране")
	@Column(name = "SORT_NOMER")
	private Integer sortNomer;

	@Transient
	private transient String dein; // иска се допълнително в идентИнфо да се записва в някои случаи какво е действието
	
	@Transient
	private transient Integer	dbCommon;	// старото състояние
	@Transient
	private transient Integer	dbAlg;		// старото състояние
	@Transient
	private transient Integer	dbDocType;	// старото състояние
	@Transient
	private transient String	dbPrefix;

	/**  */
	public Register() {
		super();
	}

	/** @return the actNomer */
	public Integer getActNomer() {
		return this.actNomer;
	}

	/** @return the alg */
	public Integer getAlg() {
		return this.alg;
	}

	/** @return the begNomer */
	public Integer getBegNomer() {
		return this.begNomer;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_REGISTER;
	}

	/** @return the common */
	public Integer getCommon() {
		return this.common;
	}

	/** @return the dbAlg */
	public Integer getDbAlg() {
		return this.dbAlg;
	}

	/** @return the dbCommon */
	public Integer getDbCommon() {
		return this.dbCommon;
	}

	/** @return the dbDocType */
	public Integer getDbDocType() {
		return this.dbDocType;
	}

	/** @return the dbPrefix */
	public String getDbPrefix() {
		return this.dbPrefix;
	}

	/** @return the docType */
	public Integer getDocType() {
		return this.docType;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/**  */
	@Override
	public String getIdentInfo() throws DbErrorException {
		if (this.dein == null) {
			return this.register;
		}
		return this.dein + " " + this.register;
	}

	/** @return the prefix */
	public String getPrefix() {
		return this.prefix;
	}

	/** @return the register */
	public String getRegister() {
		return this.register;
	}

	/** @return the registerType */
	public Integer getRegisterType() {
		return this.registerType;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @return the sortNomer */
	public Integer getSortNomer() {
		return this.sortNomer;
	}

	/** @return the step */
	public Integer getStep() {
		return this.step;
	}

	/** @return the valid */
	public Integer getValid() {
		return this.valid;
	}

	/** @param actNomer the actNomer to set */
	public void setActNomer(Integer actNomer) {
		this.actNomer = actNomer;
	}

	/** @param alg the alg to set */
	public void setAlg(Integer alg) {
		this.alg = alg;
	}

	/** @param begNomer the begNomer to set */
	public void setBegNomer(Integer begNomer) {
		this.begNomer = begNomer;
	}

	/** @param common the common to set */
	public void setCommon(Integer common) {
		this.common = common;
	}

	/** @param dbAlg the dbAlg to set */
	public void setDbAlg(Integer dbAlg) {
		this.dbAlg = dbAlg;
	}

	/** @param dbCommon the dbCommon to set */
	public void setDbCommon(Integer dbCommon) {
		this.dbCommon = dbCommon;
	}

	/** @param dbDocType the dbDocType to set */
	public void setDbDocType(Integer dbDocType) {
		this.dbDocType = dbDocType;
	}

	/** @param dbPrefix the dbPrefix to set */
	public void setDbPrefix(String dbPrefix) {
		this.dbPrefix = dbPrefix;
	}

	/** @param docType the docType to set */
	public void setDocType(Integer docType) {
		this.docType = docType;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param prefix the prefix to set */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/** @param register the register to set */
	public void setRegister(String register) {
		this.register = register;
	}

	/** @param registerType the registerType to set */
	public void setRegisterType(Integer registerType) {
		this.registerType = registerType;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** @param sortNomer the sortNomer to set */
	public void setSortNomer(Integer sortNomer) {
		this.sortNomer = sortNomer;
	}

	/** @param step the step to set */
	public void setStep(Integer step) {
		this.step = step;
	}

	/** @param valid the valid to set */
	public void setValid(Integer valid) {
		this.valid = valid;
	}

	/** @return the dein */
	public String getDein() {
		return this.dein;
	}
	/** @param dein the dein to set */
	public void setDein(String dein) {
		this.dein = dein;
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
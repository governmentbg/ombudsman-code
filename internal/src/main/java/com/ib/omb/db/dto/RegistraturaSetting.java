package com.ib.omb.db.dto;

import static com.ib.system.SysConstants.CODE_CLASSIF_DANE;
import static com.ib.system.utils.SearchUtils.trimToNULL;
import static javax.persistence.GenerationType.SEQUENCE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ib.omb.system.OmbConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Настройки на регистратура
 *
 * @author belev
 */
@Entity
@Table(name = "REGISTRATURA_SETTINGS")
public class RegistraturaSetting extends TrackableEntity implements AuditExt {

	/**  */
	private static final long serialVersionUID = 2335547953871174480L;

	@SequenceGenerator(name = "RegistraturaSetting", sequenceName = "SEQ_REGISTRATURA_SETTINGS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "RegistraturaSetting")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

//	@JournalAttr(label = "registraturaId", defaultText = "ИД на регистратура")
	@Column(name = "REGISTRATURA_ID", updatable = false)
	private Integer registraturaId;

	@JournalAttr(label = "regSettings.setting", defaultText = "Настройка", classifID = "" + OmbConstants.CODE_CLASSIF_REISTRATURA_SETTINGS)
	@Column(name = "SETTING_CODE")
	private Integer settingCode;

	@JournalAttr(label = "regSettings.value", defaultText = "Стойност", classifField = "codeClassif")
	@Column(name = "SETTING_VALUE")
	private Integer settingValue;

	@Column(name = "CODE_CLASSIF")
	private Integer codeClassif;

	@Transient
	private String				tekst;			// взима се от класификацията, защото така и така е наличен и няма смисъл да се
												// разкодира
	@Transient
	private transient Integer	dbSettingValue;	// за да се знае какво е било и ако няма промяна да не се пуска update

	/**  */
	public RegistraturaSetting() {
		super();
	}

	/**
	 * @param registraturaId
	 * @param settingCode
	 * @param codeExt        тука стои за коя класификацията става въпрос
	 * @param tekst
	 */
	public RegistraturaSetting(Integer registraturaId, Integer settingCode, String codeExt, String tekst) {
		this.registraturaId = registraturaId;
		this.settingCode = settingCode;
		this.tekst = tekst;

		codeExt = trimToNULL(codeExt);
		this.codeClassif = codeExt == null ? CODE_CLASSIF_DANE : Integer.parseInt(codeExt);
	}

	/** @return the codeClassif */
	public Integer getCodeClassif() {
		return this.codeClassif;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA_SETT;
	}

	/** @return the dbSettingValue */
	public Integer getDbSettingValue() {
		return this.dbSettingValue;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		return this.tekst;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @return the settingCode */
	public Integer getSettingCode() {
		return this.settingCode;
	}

	/** @return the settingValue */
	public Integer getSettingValue() {
		return this.settingValue;
	}

	/** @return the tekst */
	public String getTekst() {
		return this.tekst;
	}

	/** @return за различните от ДА/НЕ */
	public boolean getValue() {
		return this.settingValue != null;
	}

	/** @return 0,1,2 */
	public String getValueTri() {
		return this.settingValue == null ? "0" : this.settingValue.toString();
	}

	/** @param codeClassif the codeClassif to set */
	public void setCodeClassif(Integer codeClassif) {
		this.codeClassif = codeClassif;
	}

	/** @param dbSettingValue the dbSettingValue to set */
	public void setDbSettingValue(Integer dbSettingValue) {
		this.dbSettingValue = dbSettingValue;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** @param settingCode the settingCode to set */
	public void setSettingCode(Integer settingCode) {
		this.settingCode = settingCode;
	}

	/** @param settingValue the settingValue to set */
	public void setSettingValue(Integer settingValue) {
		this.settingValue = settingValue;
	}

	/** @param tekst the tekst to set */
	public void setTekst(String tekst) {
		this.tekst = tekst;
	}

	/** @param value */
	public void setValue(boolean value) {
		if (!value) {
			this.settingValue = null;

		} else if (this.settingValue == null) {
			this.settingValue = 0;
		}
	}

	/** @param value 0,1,2 */
	public void setValueTri(String value) {
		if (value == null) {
			this.settingValue = null;
		} else {
			if ("0".equals(value)) {
				this.settingValue = null;
			} else {
				this.settingValue = Integer.valueOf(value);
			}
		}
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
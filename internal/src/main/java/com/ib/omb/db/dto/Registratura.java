package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Регистратура
 *
 * @author belev
 */
@Entity
@Table(name = "REGISTRATURI")
public class Registratura extends TrackableEntity implements AuditExt {
	/**  */
	private static final long serialVersionUID = 7953751793731144691L;

	@SequenceGenerator(name = "Registratura", sequenceName = "SEQ_REGISTRATURI", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "Registratura")
	@Column(name = "REGISTRATURA_ID", unique = true, nullable = false)
	private Integer id;

	@JournalAttr(label = "regList.naimReg", defaultText = "Наименование на регистратура")
	@Column(name = "REGISTRATURA")
	private String registratura;

	@JournalAttr(label = "regList.valid", defaultText = "Активна", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "VALID")
	private Integer valid;

	@JournalAttr(label = "regList.eikOrg", defaultText = "ЕИК на организацията")
	@Column(name = "ORG_EIK")
	private String orgEik;

	@JournalAttr(label = "regList.naimOrg", defaultText = "Наименование на организация")
	@Column(name = "ORG_NAME")
	private String orgName;

	@JournalAttr(label = "missing.guidSeos", defaultText = "СЕОС GUID")
	@Column(name = "GUID_SEOS")
	private String guidSeos;

	@JournalAttr(label = "missing.certPathSeos", defaultText = "СЕОС Път до сертификата")
	@Column(name = "CERT_PATH_SEOS")
	private String certPathSeos;

	@JournalAttr(label = "missing.guidSsev", defaultText = "ССЕВ GUID")
	@Column(name = "GUID_SSEV")
	private String guidSsev;

	@JournalAttr(label = "missing.certPathSsev", defaultText = "ССЕВ Път до сертификата")
	@Column(name = "CERT_PATH_SSEV")
	private String certPathSsev;

	@JournalAttr(label = "registratura.adres", defaultText = "Адрес на регистратурата")
	@Column(name = "ADDRESS")
	private String address;

	@JournalAttr(label = "registratura.contacts", defaultText = "Контакти на регистратурата")
	@Column(name = "CONTACTS")
	private String contacts;

	@JournalAttr(label = "missing.ekatte", defaultText = "Населено място", classifID = "" + SysConstants.CODE_CLASSIF_EKATTE)
	@Column(name = "EKATTE")
	private Integer ekatte;

	@JournalAttr(label = "missing.postCode", defaultText = "ПК")
	@Column(name = "POST_CODE")
	private String postCode;

	@JournalAttr(label = "missing.postBox", defaultText = "Пощенска кутия")
	@Column(name = "POST_BOX")
	private String postBox;

	/** */
	public Registratura() {
		super();
	}

	/**
	 * @param id
	 * @param registratura
	 * @param valid
	 * @param orgEik
	 * @param orgName
	 */
	public Registratura(Integer id, String registratura, Integer valid, String orgEik, String orgName) {
		this.id = id;
		this.registratura = registratura;
		this.valid = valid;
		this.orgEik = orgEik;
		this.orgName = orgName;
	}

	/** @return the address */
	public String getAddress() {
		return this.address;
	}

	/** @return the certPathSeos */
	public String getCertPathSeos() {
		return this.certPathSeos;
	}

	/** @return the certPathSsev */
	public String getCertPathSsev() {
		return this.certPathSsev;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA;
	}

	/** @return the contacts */
	public String getContacts() {
		return this.contacts;
	}

	/** @return the ekatte */
	public Integer getEkatte() {
		return this.ekatte;
	}

	/** @return the guidSeos */
	public String getGuidSeos() {
		return this.guidSeos;
	}

	/** @return the guidSsev */
	public String getGuidSsev() {
		return this.guidSsev;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the orgEik */
	public String getOrgEik() {
		return this.orgEik;
	}

	/** @return the orgName */
	public String getOrgName() {
		return this.orgName;
	}

	/** @return the postBox */
	public String getPostBox() {
		return this.postBox;
	}

	/** @return the postCode */
	public String getPostCode() {
		return this.postCode;
	}

	/** @return the registratura */
	public String getRegistratura() {
		return this.registratura;
	}

	/** @return the valid */
	public Integer getValid() {
		return this.valid;
	}

	/** @param address the address to set */
	public void setAddress(String address) {
		this.address = address;
	}

	/** @param certPathSeos the certPathSeos to set */
	public void setCertPathSeos(String certPathSeos) {
		this.certPathSeos = certPathSeos;
	}

	/** @param certPathSsev the certPathSsev to set */
	public void setCertPathSsev(String certPathSsev) {
		this.certPathSsev = certPathSsev;
	}

	/** @param contacts the contacts to set */
	public void setContacts(String contacts) {
		this.contacts = contacts;
	}

	/** @param ekatte the ekatte to set */
	public void setEkatte(Integer ekatte) {
		this.ekatte = ekatte;
	}

	/** @param guidSeos the guidSeos to set */
	public void setGuidSeos(String guidSeos) {
		this.guidSeos = guidSeos;
	}

	/** @param guidSsev the guidSsev to set */
	public void setGuidSsev(String guidSsev) {
		this.guidSsev = guidSsev;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param orgEik the orgEik to set */
	public void setOrgEik(String orgEik) {
		this.orgEik = orgEik;
	}

	/** @param orgName the orgName to set */
	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

	/** @param postBox the postBox to set */
	public void setPostBox(String postBox) {
		this.postBox = postBox;
	}

	/** @param postCode the postCode to set */
	public void setPostCode(String postCode) {
		this.postCode = postCode;
	}

	/** @param registratura the registratura to set */
	public void setRegistratura(String registratura) {
		this.registratura = registratura;
	}

	/** @param valid the valid to set */
	public void setValid(Integer valid) {
		this.valid = valid;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		return journal;
	}
}
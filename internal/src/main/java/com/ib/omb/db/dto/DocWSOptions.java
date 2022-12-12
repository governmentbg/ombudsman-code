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
import com.ib.system.db.PersistentEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

@Entity
@Table(name = "DOC_WS_OPTIONS")
public class DocWSOptions implements PersistentEntity, AuditExt {

	/**  */
	private static final long serialVersionUID = -4317814541794367621L;

	@SequenceGenerator(name = "DocWSOptions", sequenceName = "SEQ_DOC_WS_OPTIONS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DocWSOptions")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

//	@JournalAttr(label = "docRegistratura", defaultText = "ИД на регистратура")
	@Column(name = "DOC_REGISTRATURA")
	private Integer docRegistratura;

	@JournalAttr(label = "docWsOptions.extCode", defaultText = "Външен код")
	@Column(name = "EXTERNAL_CODE")
	private String externalCode;

	@JournalAttr(label = "register.tipDoc", defaultText = "Тип документ", classifID = "" + OmbConstants.CODE_CLASSIF_DOC_TYPE)
	@Column(name = "DOC_TYPE")
	private Integer docType;

	@JournalAttr(label = "docVidSetting.typeDoc", defaultText = "Вид документ", classifID = "" + OmbConstants.CODE_CLASSIF_DOC_VID)
	@Column(name = "DOC_VID")
	private Integer docVid;

	@JournalAttr(label = "docu.freeAccess", defaultText = "Свободен достъп", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "FREE_ACCESS")
	private Integer freeAccess;

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_WS_OPT;
	}

	/** @return the docRegistratura */
	public Integer getDocRegistratura() {
		return this.docRegistratura;
	}

	/** @return the docType */
	public Integer getDocType() {
		return this.docType;
	}

	/** @return the docVid */
	public Integer getDocVid() {
		return this.docVid;
	}

	/** @return the externalCode */
	public String getExternalCode() {
		return this.externalCode;
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

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		return this.externalCode;
	}

	/** @param docRegistratura the docRegistratura to set */
	public void setDocRegistratura(Integer docRegistratura) {
		this.docRegistratura = docRegistratura;
	}

	/** @param docType the docType to set */
	public void setDocType(Integer docType) {
		this.docType = docType;
	}

	/** @param docVid the docVid to set */
	public void setDocVid(Integer docVid) {
		this.docVid = docVid;
	}

	/** @param externalCode the externalCode to set */
	public void setExternalCode(String externalCode) {
		this.externalCode = externalCode;
	}

	/** @param freeAccess the freeAccess to set */
	public void setFreeAccess(Integer freeAccess) {
		this.freeAccess = freeAccess;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA);
		journal.setJoinedIdObject1(this.docRegistratura);

		return journal;
	}
}
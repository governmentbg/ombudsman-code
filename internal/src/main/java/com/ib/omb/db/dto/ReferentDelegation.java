package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Делегиране (заместване,упълномощаване)
 *
 * @author belev
 */
@Entity
@Table(name = "ADM_REF_DELEGATIONS")

public class ReferentDelegation extends TrackableEntity implements AuditExt {
	/**  */
	private static final long serialVersionUID = -8610432591981110791L;

	@SequenceGenerator(name = "ReferentDelegation", sequenceName = "SEQ_ADM_REF_DELEGATIONS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "ReferentDelegation")
	@Column(name = "ID", nullable = false)
	private Integer id;

	@JournalAttr(label = "users.registratura", defaultText = "Регистратура", classifID = "" + OmbConstants.CODE_CLASSIF_REGISTRATURI)
	@Column(name = "REGISTRATURA_ID", updatable = false)
	private Integer registraturaId;

	@JournalAttr(label = "missing.codeRef", defaultText = "Потребител", classifID = "" + SysConstants.CODE_CLASSIF_USERS)
	@Column(name = "CODE_REF")
	private Integer codeRef;

	@JournalAttr(label = "missing.delegationType", defaultText = "Вид делегирано право", classifID = "" + OmbConstants.CODE_CLASSIF_DELEGATES)
	@Column(name = "DELEGATION_TYPE")
	private Integer delegationType;

	@JournalAttr(label = "missing.userId", defaultText = "Потребител", classifID = "" + SysConstants.CODE_CLASSIF_USERS)
	@Column(name = "USER_ID")
	private Integer userId;

	@JournalAttr(label = "refDeleg.dateFrom", defaultText = "От дата")
	@Temporal(TemporalType.DATE)
	@Column(name = "DATE_OT")
	private Date dateOt;

	@JournalAttr(label = "refDeleg.dateTo", defaultText = "До дата")
	@Temporal(TemporalType.DATE)
	@Column(name = "DATE_DO")
	private Date dateTo;

	@JournalAttr(label = "docu.note", defaultText = "Забележка")
	@Column(name = "DELEGATION_INFO")
	private String delegationInfo;

	@Transient
	private transient Integer dbUserId; // стария заместник / упълномощен

	@Transient
	private String identInfo;

	/** */
	public ReferentDelegation() {
		super();
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DELEGATION;
	}

	/** @return the codeRef */
	public Integer getCodeRef() {
		return this.codeRef;
	}

	/** @return the dateOt */
	public Date getDateOt() {
		return this.dateOt;
	}

	/** @return the dateTo */
	public Date getDateTo() {
		return this.dateTo;
	}

	/** @return the dbUserId */
	public Integer getDbUserId() {
		return this.dbUserId;
	}

	/** @return the delegationInfo */
	public String getDelegationInfo() {
		return this.delegationInfo;
	}

	/** @return the delegationType */
	public Integer getDelegationType() {
		return this.delegationType;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		return this.identInfo;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @return the userId */
	public Integer getUserId() {
		return this.userId;
	}

	/** @param codeRef the codeRef to set */
	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	/** @param dateOt the dateOt to set */
	public void setDateOt(Date dateOt) {
		this.dateOt = dateOt;
	}

	/** @param dateTo the dateTo to set */
	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}

	/** @param dbUserId the dbUserId to set */
	public void setDbUserId(Integer dbUserId) {
		this.dbUserId = dbUserId;
	}

	/** @param delegationInfo the delegationInfo to set */
	public void setDelegationInfo(String delegationInfo) {
		this.delegationInfo = delegationInfo;
	}

	/** @param delegationType the delegationType to set */
	public void setDelegationType(Integer delegationType) {
		this.delegationType = delegationType;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param identInfo the identInfo to set */
	public void setIdentInfo(String identInfo) {
		this.identInfo = identInfo;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** @param userId the userId to set */
	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		// няма да се вижда през потребителя, че става проблем като се смени човека

//		journal.setJoinedCodeObject1(Constants.CODE_ZNACHENIE_JOURNAL_USER);
//		journal.setJoinedIdObject1(this.codeRef);
//
//		journal.setJoinedCodeObject2(Constants.CODE_ZNACHENIE_JOURNAL_USER);
//		journal.setJoinedIdObject2(this.userId);

		return journal;
	}
}
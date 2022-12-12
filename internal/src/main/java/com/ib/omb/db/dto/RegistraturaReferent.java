package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.Date;

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
import com.ib.system.db.PersistentEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Референти в групата на регистратирата
 *
 * @author belev
 */
@Entity
@Table(name = "REGISTRATURA_REFERENTS")
public class RegistraturaReferent implements PersistentEntity, AuditExt {
	/**  */
	private static final long serialVersionUID = 554070157104458826L;

	@SequenceGenerator(name = "RegistraturaReferent", sequenceName = "SEQ_REGISTRATURA_REFERENTS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "RegistraturaReferent")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

//	@JournalAttr(label = "groupId", defaultText = "ИД на група")
	@Column(name = "GROUP_ID")
	private Integer groupId;

	@JournalAttr(label = "codeRef", defaultText = "Име", classifField = "codeRefClassif")
	@Column(name = "CODE_REF")
	private Integer codeRef;

	@Column(name = "USER_REG")
	private Integer userReg;

	@Column(name = "DATE_REG")
	private Date dateReg;

	@Transient
	private Integer codeRefClassif; // за да се знае от коя класификация трябва да се разкодира човека в журнала

	/**  */
	public RegistraturaReferent() {
		super();
	}

	/** @param codeRef */
	public RegistraturaReferent(Integer codeRef) {
		this.codeRef = codeRef;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_REG_GROUP_CORRESP;
	}

	/** @return the codeRef */
	public Integer getCodeRef() {
		return this.codeRef;
	}

	/** @return the codeRefClassif */
	public Integer getCodeRefClassif() {
		return this.codeRefClassif;
	}

	/** @return the dateReg */
	public Date getDateReg() {
		return this.dateReg;
	}

	/** @return the groupId */
	public Integer getGroupId() {
		return this.groupId;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the userReg */
	public Integer getUserReg() {
		return this.userReg;
	}

	/** @param codeRef the codeRef to set */
	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	/** @param codeRefClassif the codeRefClassif to set */
	public void setCodeRefClassif(Integer codeRefClassif) {
		this.codeRefClassif = codeRefClassif;
	}

	/** @param dateReg the dateReg to set */
	@Override
	public void setDateReg(Date dateReg) {
		this.dateReg = dateReg;
	}

	/** @param groupId the groupId to set */
	public void setGroupId(Integer groupId) {
		this.groupId = groupId;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param userReg the userReg to set */
	@Override
	public void setUserReg(Integer userReg) {
		this.userReg = userReg;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA_GROUP);
		journal.setJoinedIdObject1(this.groupId);

		return journal;
	}

}
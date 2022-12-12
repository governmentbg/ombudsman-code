package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.ib.omb.system.OmbConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.PersistentEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Връзка между работен протокол за архивиране и преписката, която ще се архивира
 *
 * @author belev
 */
@Entity
@Table(name = "DELO_ARCHIVE")
public class DeloArchive implements PersistentEntity, AuditExt {

	/**  */
	private static final long serialVersionUID = -1835765259208751817L;

	@SequenceGenerator(name = "DeloArchive", sequenceName = "SEQ_DELO_ARCHIVE", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DeloArchive")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@JournalAttr(label = "protocolId", defaultText = "Раб. протокол", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	@Column(name = "PROTOCOL_ID")
	private Integer protocolId;

	@JournalAttr(label = "docId", defaultText = "Ном. дело", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO)
	@Column(name = "DELO_ID")
	private Integer deloId;

	@Column(name = "USER_REG")
	private Integer userReg;

	@Column(name = "DATE_REG")
	private Date dateReg;

	/** */
	public DeloArchive() {
		super();
	}

	/**
	 * @param protocolId
	 * @param deloId
	 */
	public DeloArchive(Integer protocolId, Integer deloId) {
		this.protocolId = protocolId;
		this.deloId = deloId;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_ARCHIVE;
	}

	/** @return the dateReg */
	public Date getDateReg() {
		return this.dateReg;
	}

	/** @return the deloId */
	public Integer getDeloId() {
		return this.deloId;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the protocolId */
	public Integer getProtocolId() {
		return this.protocolId;
	}

	/** @return the userReg */
	public Integer getUserReg() {
		return this.userReg;
	}

	/** @param dateReg the dateReg to set */
	@Override
	public void setDateReg(Date dateReg) {
		this.dateReg = dateReg;
	}

	/** @param deloId the deloId to set */
	public void setDeloId(Integer deloId) {
		this.deloId = deloId;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param protocolId the protocolId to set */
	public void setProtocolId(Integer protocolId) {
		this.protocolId = protocolId;
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

		journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		journal.setJoinedIdObject1(this.protocolId);

		journal.setJoinedCodeObject2(OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO);
		journal.setJoinedIdObject2(this.deloId);

		return journal;
	}
}
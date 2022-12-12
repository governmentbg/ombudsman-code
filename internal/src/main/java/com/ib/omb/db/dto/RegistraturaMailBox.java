package com.ib.omb.db.dto;


import static javax.persistence.GenerationType.SEQUENCE;

import java.util.ArrayList;
import java.util.List;

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


@Entity
@Table(name = "REGISTRATURA_MAILBOXES")
public class RegistraturaMailBox extends TrackableEntity implements AuditExt {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4344967157529487926L;

	@SequenceGenerator(name = "RegistraturaMailBox", sequenceName = "SEQ_REGISTRATURA_MAILBOXES", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "RegistraturaMailBox")
	@Column(name = "MAILBOX_ID", unique = true, nullable = false)
	private Integer id;

//	@JournalAttr(label = "registraturaId", defaultText = "ИД на регистратура")
	@Column(name = "REGISTRATURA_ID", updatable = false)
	private Integer registraturaId;
	
	@JournalAttr(label = "regMailboxes.mailbox", defaultText = "Пощенска кутия")
	@Column(name = "MAILBOX_NAME")
	private String mailboxName;

	@JournalAttr(label = "refDeleg.user", defaultText = "Потребител")
	@Column(name = "MAILBOX_USERNAME")
	private String mailboxUsername;

	@JournalAttr(label = "regMailboxes.mailboxPassword", defaultText = "Парола")
	@Column(name = "MAILBOX_PASSWORD")
	private String mailboxPassword;
	
	@JournalAttr(label = "regData.settings", defaultText = "Настройки")
	@Transient
	private List<RegistraturaMailBoxVar> variables = new ArrayList<>();
	

	@Transient
	private transient Boolean auditable; // за да може да се включва и изключва журналирането

	@Override
	public boolean isAuditable() {
		return this.auditable == null ? super.isAuditable() : this.auditable.booleanValue();
	}
	public void setAuditable(Boolean auditable) {
		this.auditable = auditable;
	}

	public Integer getCodeMainObject() {		
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_MAILBOX;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getRegistraturaId() {
		return registraturaId;
	}

	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	public String getMailboxName() {
		return mailboxName;
	}

	public void setMailboxName(String mailboxName) {
		this.mailboxName = mailboxName;
	}

	public String getMailboxUsername() {
		return mailboxUsername;
	}

	public void setMailboxUsername(String mailboxUsername) {
		this.mailboxUsername = mailboxUsername;
	}

	public String getMailboxPassword() {
		return mailboxPassword;
	}

	public void setMailboxPassword(String mailboxPassword) {
		this.mailboxPassword = mailboxPassword;
	}

	public List<RegistraturaMailBoxVar> getVariables() {
		return variables;
	}

	public void setVariables(List<RegistraturaMailBoxVar> variables) {
		this.variables = variables;
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
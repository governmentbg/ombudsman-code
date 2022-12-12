package com.ib.omb.db.dto;


import static javax.persistence.GenerationType.SEQUENCE;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ib.system.db.JournalAttr;

@Entity
@Table(name = "REGISTRATURA_MAILBOXES_VARS")
public class RegistraturaMailBoxVar implements Serializable {
	
	/**  */
	private static final long serialVersionUID = -4748324141296271516L;

	@JournalAttr(label="id",defaultText = "Системен идентификатор", isId = "true")
	@SequenceGenerator(name = "RegistraturaMailBoxVar", sequenceName = "SEQ_MAILBOXES_VARS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "RegistraturaMailBoxVar")
	@Column(name = "VAR_ID", unique = true, nullable = false)
	private Integer id;

//	@JournalAttr(label = "mailBoxId", defaultText = "ИД на пощенска кутия")
	@Column(name = "MAILBOX_ID")
	private Integer mailBoxId;
		
	@JournalAttr(label="regMailboxes.key",defaultText = "Ключ")
	@Column(name = "MAIL_KEY")
	private String mailKey;
	
	@JournalAttr(label="regSettings.value",defaultText = "Стойност")
	@Column(name = "MAIL_VALUE")
	private String mailValue;
	
	@Transient
	private transient Integer flag;
	
	
	public RegistraturaMailBoxVar() {
		super();		
	}
	
	
	/** */
	public RegistraturaMailBoxVar(String mailKey, String mailValue ) {
		this.mailKey = mailKey;
		this.mailValue = mailValue;
	}
	
	

	public Integer getFlag() {
		return flag;
	}

	public void setFlag(Integer flag) {
		this.flag = flag;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getMailBoxId() {
		return mailBoxId;
	}

	public void setMailBoxId(Integer mailBoxId) {
		this.mailBoxId = mailBoxId;
	}

	public String getMailKey() {
		return mailKey;
	}

	public void setMailKey(String mailKey) {
		this.mailKey = mailKey;
	}

	public String getMailValue() {
		return mailValue;
	}

	public void setMailValue(String mailValue) {
		this.mailValue = mailValue;
	}
	
	
	
	
		
}
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
import com.ib.system.utils.DateUtils;

@Entity
@Table(name = "PRAZNICI")
public class Praznici implements PersistentEntity, AuditExt {

	/**  */
	private static final long serialVersionUID = 3803743262490855490L;

	@SequenceGenerator(name = "Praznici", sequenceName = "SEQ_PRAZNICI", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "Praznici")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@JournalAttr(label = "den", defaultText = "Дата")
	@Column(name = "DEN")
	private Date den;

	public Praznici() {
		super();
	}

	public Praznici(Date den) {
		this.den = den;
	}

	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_PRAZNIK;
	}

	public Date getDen() {
		return this.den;
	}

	@Override
	public Integer getId() {
		return this.id;
	}

	@Override
	public String getIdentInfo() throws DbErrorException {
		return DateUtils.printDate(this.den);
	}

	public void setDen(Date den) {
		this.den = den;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());
		
		// така всички празнични дни ще се оберат от компонентата
		journal.setJoinedCodeObject1(getCodeMainObject()); 
		journal.setJoinedIdObject1(getCodeMainObject());
		
		return journal;
	}
}
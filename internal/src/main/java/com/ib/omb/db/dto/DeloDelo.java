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
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Връзка между преписки
 *
 * @author belev
 */
@Entity
@Table(name = "DELO_DELO")
public class DeloDelo extends TrackableEntity implements AuditExt {
	/**  */
	private static final long serialVersionUID = 712927560823878846L;

	@SequenceGenerator(name = "DeloDelo", sequenceName = "SEQ_DELO_DELO", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DeloDelo")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "DELO_ID", updatable = false)
	@JournalAttr(label = "deloId",defaultText = "Дело, в което се влага", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO)
	private Integer deloId;

	@Column(name = "INPUT_DELO_ID", updatable = false)
	@JournalAttr(label = "inputDeloId",defaultText = "Дело, което е вложено", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO)
	private Integer inputDeloId;

	@Column(name = "INPUT_DATE")
	@JournalAttr(label = "inputDate",defaultText = "Дата на влагане", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date inputDate;

	@Column(name = "TOM_NOMER")
	@JournalAttr(label = "tomNomer",defaultText = "В том №")
	private Integer tomNomer;

	@Transient
	private transient Boolean auditable; // за да може да се включва и изключва журналирането

	@Transient
	private Delo delo; // информация за преписката през deloId

	/** */
	public DeloDelo() {
		super();
	}

	/**
	 * Изпозлва се за влагане на преписка в преписка
	 *
	 * @param deloId
	 * @param inputDeloId
	 * @param tomNomer
	 * @param inputDate
	 */
	public DeloDelo(Integer deloId, Integer inputDeloId, Integer tomNomer, Date inputDate) {
		this.deloId = deloId;
		this.inputDeloId = inputDeloId;
		this.tomNomer = tomNomer;
		this.inputDate = inputDate;
	}

//	/**
//	 * Използва се при търсене на преписката за преписката {@link DeloDeloDAO#findByIdDelo(Integer)}
//	 *
//	 * @param id
//	 * @param inputDate
//	 * @param deloId
//	 * @param initDocId
//	 * @param rnDelo
//	 * @param deloDate
//	 * @param status
//	 * @param statusDate
//	 */
//	public DeloDelo(Integer id, Date inputDate, Integer deloId, Integer initDocId, String rnDelo, Date deloDate, Integer status, Date statusDate) {
//		this.id = id;
//		this.inputDate = inputDate;
//		this.deloId = deloId;
//
//		this.delo = new Delo();
//		this.delo.setId(deloId);
//		this.delo.setInitDocId(initDocId);
//		this.delo.setRnDelo(rnDelo);
//		this.delo.setDeloDate(deloDate);
//		this.delo.setStatus(status);
//		this.delo.setStatusDate(statusDate);
//	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DELO;
	}

	/** @return the delo */
	public Delo getDelo() {
		return this.delo;
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

	/** @return the inputDate */
	public Date getInputDate() {
		return this.inputDate;
	}

	/** @return the inputDeloId */
	public Integer getInputDeloId() {
		return this.inputDeloId;
	}

	/** @return the tomNomer */
	public Integer getTomNomer() {
		return this.tomNomer;
	}

	/** @return the auditable */
	@Override
	public boolean isAuditable() {
		return this.auditable == null ? super.isAuditable() : this.auditable.booleanValue();
	}

	/** @param auditable the auditable to set */
	public void setAuditable(Boolean auditable) {
		this.auditable = auditable;
	}

	/** @param delo the delo to set */
	public void setDelo(Delo delo) {
		this.delo = delo;
	}

	/** @param deloId the deloId to set */
	public void setDeloId(Integer deloId) {
		this.deloId = deloId;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param inputDate the inputDate to set */
	public void setInputDate(Date inputDate) {
		this.inputDate = inputDate;
	}

	/** @param inputDeloId the inputDeloId to set */
	public void setInputDeloId(Integer inputDeloId) {
		this.inputDeloId = inputDeloId;
	}

	/** @param tomNomer the tomNomer to set */
	public void setTomNomer(Integer tomNomer) {
		this.tomNomer = tomNomer;
	}
	
	@Override
	public String getIdentInfo() throws DbErrorException {
		return null;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new  SystemJournal();				
		dj.setCodeObject(getCodeMainObject());
		dj.setIdObject(getId());
		dj.setIdentObject(getIdentInfo());
		dj.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO);
		dj.setJoinedCodeObject2(OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO);
		dj.setJoinedIdObject1(deloId);
		dj.setJoinedIdObject2(inputDeloId);
		return dj;
	}
	
	
}
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
 * Връзка между документ и преписка
 *
 * @author belev
 */
@Entity
@Table(name = "DELO_DOC")
public class DeloDoc extends TrackableEntity implements AuditExt{
	/**  */
	private static final long serialVersionUID = -8757541350583503107L;

	@SequenceGenerator(name = "DeloDoc", sequenceName = "SEQ_DELO_DOC", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DeloDoc")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "DELO_ID", updatable = false)
	@JournalAttr(label = "deloID",defaultText = "Дело", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO)
	private Integer deloId;

	@Column(name = "INPUT_DOC_ID", updatable = false)
	@JournalAttr(label = "inputDocId",defaultText = "Документ", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer inputDocId;

	@Column(name = "INPUT_DATE")
	@JournalAttr(label = "inputDate",defaultText = "Дата на влагане", dateMask = "dd.MM.yyyy HH:mm:ss")
	private Date inputDate;

	@Column(name = "SECTION_TYPE")
	@JournalAttr(label = "sectionType",defaultText = "Раздел", classifID = "108")
	private Integer sectionType;

	@Column(name = "TOM_NOMER")
	@JournalAttr(label = "romNomer",defaultText = "В том №")
	private Integer tomNomer;

	@Column(name = "EKZ_NOMER")
	@JournalAttr(label = "ekzNomer",defaultText = "Номер на екземпляр")
	private Integer ekzNomer;

	@Transient
	private Doc doc; // информация за документа през docId ! в контекста на преписка

	@Transient
	private Delo		delo;		// информация за преписката през deloId ! в контекста на документ
	@Transient
	private DeloDelo	deloDelo;	// тука стои връзката на делото през delo ! в контекста на документ

	/**  */
	public DeloDoc() {
		super();
	}

	/**
	 * Използва се за влагане на док в преписка в контекста на преписка.
	 *
	 * @param deloId
	 * @param inputDocId
	 * @param tomNomer
	 * @param sectionType
	 * @param inputDate
	 */
	public DeloDoc(Integer deloId, Integer inputDocId, Integer tomNomer, Integer sectionType, Date inputDate) {
		this.deloId = deloId;
		this.inputDocId = inputDocId;
		this.tomNomer = tomNomer;
		this.sectionType = sectionType;
		this.inputDate = inputDate;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DOC;
	}

	/** @return the delo */
	public Delo getDelo() {
		return this.delo;
	}

	/** @return the deloDelo */
	public DeloDelo getDeloDelo() {
		return this.deloDelo;
	}

	/** @return the deloId */
	public Integer getDeloId() {
		return this.deloId;
	}

	/** @return the doc */
	public Doc getDoc() {
		return this.doc;
	}

	/** @return the ekzNomer */
	public Integer getEkzNomer() {
		return this.ekzNomer;
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

	/** @return the inputDocId */
	public Integer getInputDocId() {
		return this.inputDocId;
	}

	/** @return the sectionType */
	public Integer getSectionType() {
		return this.sectionType;
	}

	/** @return the tomNomer */
	public Integer getTomNomer() {
		return this.tomNomer;
	}

	/** @param delo the delo to set */
	public void setDelo(Delo delo) {
		this.delo = delo;
	}

	/** @param deloDelo the deloDelo to set */
	public void setDeloDelo(DeloDelo deloDelo) {
		this.deloDelo = deloDelo;
	}

	/** @param deloId the deloId to set */
	public void setDeloId(Integer deloId) {
		this.deloId = deloId;
	}

	/** @param doc the doc to set */
	public void setDoc(Doc doc) {
		this.doc = doc;
	}

	/** @param ekzNomer the ekzNomer to set */
	public void setEkzNomer(Integer ekzNomer) {
		this.ekzNomer = ekzNomer;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param inputDate the inputDate to set */
	public void setInputDate(Date inputDate) {
		this.inputDate = inputDate;
	}

	/** @param inputDocId the inputDocId to set */
	public void setInputDocId(Integer inputDocId) {
		this.inputDocId = inputDocId;
	}

	/** @param sectionType the sectionType to set */
	public void setSectionType(Integer sectionType) {
		this.sectionType = sectionType;
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
		dj.setJoinedCodeObject2(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		dj.setJoinedIdObject1(deloId);
		dj.setJoinedIdObject2(inputDocId);
		return dj;
	}
}
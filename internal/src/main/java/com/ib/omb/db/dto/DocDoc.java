package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.ib.omb.system.OmbConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Връзка между документи
 *
 * @author belev
 */
@Entity
@Table(name = "DOC_DOC")
public class DocDoc extends TrackableEntity implements AuditExt{
	/**  */
	private static final long serialVersionUID = -4702370356031135539L;

	@SequenceGenerator(name = "DocDoc", sequenceName = "SEQ_DOC_DOC", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DocDoc")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "DOC_ID1", updatable = false)
	@JournalAttr(label = "docId1", defaultText = "Документ 1", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer docId1;

	@Column(name = "REL_TYPE")
	@JournalAttr(label = "docId1", defaultText = "Вид на връзка",classifID = "145")
	private Integer relType;

	@Column(name = "DOC_ID2", updatable = false)
	@JournalAttr(label = "docId1", defaultText = "Документ 2", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	private Integer docId2;
	
	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DOC;
	}

	/** @return the docId1 */
	public Integer getDocId1() {
		return this.docId1;
	}

	/** @return the docId2 */
	public Integer getDocId2() {
		return this.docId2;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the relType */
	public Integer getRelType() {
		return this.relType;
	}

	/** @param docId1 the docId1 to set */
	public void setDocId1(Integer docId1) {
		this.docId1 = docId1;
	}

	/** @param docId2 the docId2 to set */
	public void setDocId2(Integer docId2) {
		this.docId2 = docId2;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param relType the relType to set */
	public void setRelType(Integer relType) {
		this.relType = relType;
	}
	
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new  SystemJournal();				
		dj.setCodeObject(getCodeMainObject());
		dj.setIdObject(getId());
		dj.setIdentObject(getIdentInfo());
		dj.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		dj.setJoinedCodeObject2(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		dj.setJoinedIdObject1(docId1);
		dj.setJoinedIdObject2(docId2);
		return dj;
	}
}
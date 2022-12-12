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
 * Връзка между работен протокол за унищожение на документи и документ за унищожение
 *
 * @author belev
 */
@Entity
@Table(name = "DOC_DESTRUCT")
public class DocDestruct extends TrackableEntity implements AuditExt {

	/**  */
	private static final long serialVersionUID = 6501714543350067987L;

	@SequenceGenerator(name = "DocDestruct", sequenceName = "SEQ_DOC_DESTRUCT", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DocDestruct")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@JournalAttr(label = "protocolId", defaultText = "Раб. протокол", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	@Column(name = "PROTOCOL_ID", updatable = false)
	private Integer protocolId;

	@JournalAttr(label = "docId", defaultText = "Документ", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)
	@Column(name = "DOC_ID", updatable = false)
	private Integer docId;

	@JournalAttr(label = "ekzNomera", defaultText = "Екз.№")
	@Column(name = "EKZ_NOMERA")
	private String ekzNomera;

	/** */
	public DocDestruct() {
		super();
	}

	/**
	 * @param protocolId
	 * @param docId
	 */
	public DocDestruct(Integer protocolId, Integer docId) {
		this.protocolId = protocolId;
		this.docId = docId;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DESTRUCT;
	}

	/** @return the docId */
	public Integer getDocId() {
		return this.docId;
	}

	/** @return the ekzNomera */
	public String getEkzNomera() {
		return this.ekzNomera;
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

	/** @param docId the docId to set */
	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	/** @param ekzNomera the ekzNomera to set */
	public void setEkzNomera(String ekzNomera) {
		this.ekzNomera = ekzNomera;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param protocolId the protocolId to set */
	public void setProtocolId(Integer protocolId) {
		this.protocolId = protocolId;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		journal.setJoinedIdObject1(this.protocolId);

		journal.setJoinedCodeObject2(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		journal.setJoinedIdObject2(this.docId);

		return journal;
	}
}
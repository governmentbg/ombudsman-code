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
 * Участници в документ. Страни по договор, Включени в заповед и т.н.
 *
 * @author belev
 */
@Entity
@Table(name = "DOC_MEMBERS")
public class DocMember extends TrackableEntity implements AuditExt {

	/**  */
	private static final long serialVersionUID = -5661637255136944939L;

	@SequenceGenerator(name = "DocMember", sequenceName = "SEQ_DOC_MEMBERS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DocMember")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "DOC_ID", updatable = false)
	private Integer docId;

	@Column(name = "REF_TYPE")
	@JournalAttr(label = "refType", defaultText = "Вид участник", classifID = "" + OmbConstants.CODE_CLASSIF_REF_TYPE)
	private Integer refType;

	@Column(name = "ROLE_REF")
	@JournalAttr(label = "roleRef", defaultText = "Роля на участник", classifID = "" + OmbConstants.CODE_CLASSIF_DOC_MEMBER_ROLES)
	private Integer roleRef;

	@Column(name = "CODE_REF")
	private Integer codeRef;

	@Column(name = "REF_TEXT")
	@JournalAttr(label = "refText", defaultText = "Участник")
	private String refText;

	/** */
	public DocMember() {
		super();
	}

	/**  */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_MEMBER;
	}

	/** @return the codeRef */
	public Integer getCodeRef() {
		return this.codeRef;
	}

	/** @return the docId */
	public Integer getDocId() {
		return this.docId;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		return this.refText;
	}

	/** @return the refText */
	public String getRefText() {
		return this.refText;
	}

	/** @return the refType */
	public Integer getRefType() {
		return this.refType;
	}

	/** @return the roleRef */
	public Integer getRoleRef() {
		return this.roleRef;
	}

	/** @param codeRef the codeRef to set */
	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	/** @param docId the docId to set */
	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param refText the refText to set */
	public void setRefText(String refText) {
		this.refText = refText;
	}

	/** @param refType the refType to set */
	public void setRefType(Integer refType) {
		this.refType = refType;
	}

	/** @param roleRef the roleRef to set */
	public void setRoleRef(Integer roleRef) {
		this.roleRef = roleRef;
	}

	/**  */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		journal.setJoinedIdObject1(this.docId);

		return journal;
	}
}
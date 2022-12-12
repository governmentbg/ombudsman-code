package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.Date;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;

/**
 * Референт в документ
 *
 * @author belev
 */
@Entity
@Table(name = "DOC_REFERENTS")
public class DocReferent extends TrackableEntity {
	/**  */
	private static final long serialVersionUID = 3989370363357510346L;

	@SequenceGenerator(name = "DocReferent", sequenceName = "SEQ_DOC_REFERENTS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DocReferent")
	@Column(name = "ID", unique = true, nullable = false)
	@JournalAttr(label="id",defaultText = "Системен идентификатор", isId = "true")
	private Integer id;

	@Column(name = "DOC_ID", updatable = false)
	private Integer docId;

	@Column(name = "CODE_REF")
	@JournalAttr(label="codeRef",defaultText = "Служител", classifID = "24")
	private Integer codeRef;

	@Column(name = "ROLE_REF")
	private Integer roleRef;

	@Column(name = "EVENT_DATE")
	@JournalAttr(label="codeRef",defaultText = "Дата на събитие")
	private Date eventDate;

	@Column(name = "END_OPINION")
	@JournalAttr(label="endOpinion",defaultText = "Мнение", classifID = "110")
	private Integer endOpinion;

	@Column(name = "COMMENTS")
	@JournalAttr(label="comments",defaultText = "Коментар")
	private String comments;

	@Column(name = "PORED")
	private Integer pored;

	@Transient
	private String tekst;

	/** */
	public DocReferent() {
		super();
	}

	/** @param dr */
	public DocReferent(DocReferent dr) {
		super(dr);

		this.id = dr.id;
		this.docId = dr.docId;
		this.codeRef = dr.codeRef;
		this.roleRef = dr.roleRef;
		this.eventDate = dr.eventDate;
		this.endOpinion = dr.endOpinion;
		this.comments = dr.comments;
		this.tekst = dr.tekst;
	}

	/**
	 * @param codeRef
	 * @param roleRef
	 */
	public DocReferent(Integer codeRef, Integer roleRef) {
		this.codeRef = codeRef;
		this.roleRef = roleRef;
	}

	/** */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DocReferent other = (DocReferent) obj;

		return Objects.equals(this.pored, other.pored) && Objects.equals(this.endOpinion, other.endOpinion) && Objects.equals(this.comments, other.comments) && Objects.equals(other.eventDate, this.eventDate)
			&& Objects.equals(this.roleRef, other.roleRef) && Objects.equals(this.codeRef, other.codeRef) && Objects.equals(this.id, other.id) && Objects.equals(this.docId, other.docId);
	}

	/**
	 * Всички стрингови полета, които са празен стринг ги прави на null
	 */
	public void fixEmptyStringValues() {
		if (this.comments != null && this.comments.length() == 0) {
			this.comments = null;
		}
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return null; // този няма да се журналира самостоятелно, а в контекста на документа
	}

	/** @return the codeRef */
	public Integer getCodeRef() {
		return this.codeRef;
	}

	/** @return the comments */
	public String getComments() {
		return this.comments;
	}

	/** @return the docId */
	public Integer getDocId() {
		return this.docId;
	}

	/** @return the eventDate */
	public Date getEventDate() {
		return this.eventDate;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the pored */
	public Integer getPored() {
		return this.pored;
	}

	/** @return the roleRef */
	public Integer getRoleRef() {
		return this.roleRef;
	}

	/** @return the tekst */
	public String getTekst() {
		return this.tekst;
	}

	/** */
	@Override
	public int hashCode() {
		return Objects.hash(this.pored, this.endOpinion, this.codeRef, this.comments, this.docId, this.eventDate, this.id, this.roleRef);
	}

	/** */
	@Override
	public boolean isAuditable() {
		return false; // ще се журналира в контекста на докумебта. ако се иска това да се включва и изключва трябва да се сложи
						// булева променлива на класа
	}

	/** @return the endOpinion */
	public Integer getEndOpinion() {
		return this.endOpinion;
	}

	/** @param endOpinion the endOpinion to set */
	public void setEndOpinion(Integer endOpinion) {
		this.endOpinion = endOpinion;
	}

	/** @param codeRef the codeRef to set */
	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	/** @param comments the comments to set */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/** @param docId the docId to set */
	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	/** @param eventDate the eventDate to set */
	public void setEventDate(Date eventDate) {
		this.eventDate = eventDate;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param pored the pored to set */
	public void setPored(Integer pored) {
		this.pored = pored;
	}

	/** @param roleRef the roleRef to set */
	public void setRoleRef(Integer roleRef) {
		this.roleRef = roleRef;
	}

	/** @param tekst the tekst to set */
	public void setTekst(String tekst) {
		this.tekst = tekst;
	}
}
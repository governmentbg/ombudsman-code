package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JournalAttr;

/**
 * Резултат на жалба
 *
 * @author belev
 */
@Entity
@Table(name = "doc_jalba_result")
public class DocJalbaResult implements Serializable {

	/**  */
	private static final long serialVersionUID = -7362492818902673755L;

	@SequenceGenerator(name = "DocJalbaResult", sequenceName = "seq_doc_jalba_result", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DocJalbaResult")
	@Column(name = "id", unique = true, nullable = false)
	@JournalAttr(label = "id", defaultText = "Системен идентификатор", isId = "true")
	private Integer id;

	@Column(name = "doc_id", insertable = false, updatable = false)
	private Integer docId;

	@JournalAttr(label = "vidResult", defaultText = "Вид резултат", classifID = "" + OmbConstants.CODE_CLASSIF_JALBA_RES)
	@Column(name = "vid_result")
	private Integer vidResult;

	@JournalAttr(label = "dateResult", defaultText = "Дата на резултата")
	@Column(name = "date_result")
	private Date dateResult;

	@Column(name = "jbp")
	private Integer jbp;

	@Column(name = "code_subject")
	private Integer codeSubject;

	@JournalAttr(label = "textSubject", defaultText = "Орган/лице" )
	@Column(name = "text_subject")
	private String textSubject;

	@JournalAttr(label = "note", defaultText = "Забележка")
	@Column(name = "note")
	private String note;

	/**  */
	public DocJalbaResult() {
		super();
	}

	/** @return the codeSubject */
	public Integer getCodeSubject() {
		return this.codeSubject;
	}

	/** @return the dateResult */
	public Date getDateResult() {
		return this.dateResult;
	}

	/** @return the docId */
	public Integer getDocId() {
		return this.docId;
	}

	/** @return the id */
	public Integer getId() {
		return this.id;
	}

	/** @return the jbp */
	public Integer getJbp() {
		return this.jbp;
	}

	/** @return the note */
	public String getNote() {
		return this.note;
	}

	/** @return the textSubject */
	public String getTextSubject() {
		return this.textSubject;
	}

	/** @return the vidResult */
	public Integer getVidResult() {
		return this.vidResult;
	}

	/** @param codeSubject the codeSubject to set */
	public void setCodeSubject(Integer codeSubject) {
		this.codeSubject = codeSubject;
	}

	/** @param dateResult the dateResult to set */
	public void setDateResult(Date dateResult) {
		this.dateResult = dateResult;
	}

	/** @param docId the docId to set */
	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param jbp the jbp to set */
	public void setJbp(Integer jbp) {
		this.jbp = jbp;
	}

	/** @param note the note to set */
	public void setNote(String note) {
		this.note = note;
	}

	/** @param textSubject the textSubject to set */
	public void setTextSubject(String textSubject) {
		this.textSubject = textSubject;
	}

	/** @param vidResult the vidResult to set */
	public void setVidResult(Integer vidResult) {
		this.vidResult = vidResult;
	}
}

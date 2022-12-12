package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;

/**
 * Допълнителни данни за документ
 *
 * @author belev
 */
@Entity
@Table(name = "DOC_DOPDATA")
public class DocDopdata extends TrackableEntity {

	/**  */
	private static final long serialVersionUID = 2985616956055328972L;

	@JournalAttr(label = "id", defaultText = "Системен идентификатор", isId = "true")
	@SequenceGenerator(name = "DocDopdata", sequenceName = "SEQ_DOC_DOPDATA", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DocDopdata")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "DOC_ID", updatable = false)
	private Integer docId;

	@JournalAttr(label = "nameValue", defaultText = "Име")
	@Column(name = "NAME_VALUE")
	private String nameValue;

	@JournalAttr(label = "srokValue", defaultText = "Срок")
	@Column(name = "SROK_VALUE")
	private Date srokValue;

	@JournalAttr(label = "costValue", defaultText = "Стойност")
	@Column(name = "COST_VALUE")
	private Double costValue;

	/** */
	public DocDopdata() {
		super();
	}

	/**  */
	@Override
	public Integer getCodeMainObject() {
		return null;
	}

	/** @return the costValue */
	public Double getCostValue() {
		return this.costValue;
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

	/** @return the nameValue */
	public String getNameValue() {
		return this.nameValue;
	}

	/** @return the srokValue */
	public Date getSrokValue() {
		return this.srokValue;
	}

	/** */
	@Override
	public boolean isAuditable() {
		return false; // журналира се през документа
	}

	/** @param costValue the costValue to set */
	public void setCostValue(Double costValue) {
		this.costValue = costValue;
	}

	/** @param docId the docId to set */
	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param nameValue the nameValue to set */
	public void setNameValue(String nameValue) {
		this.nameValue = nameValue;
	}

	/** @param srokValue the srokValue to set */
	public void setSrokValue(Date srokValue) {
		this.srokValue = srokValue;
	}
}
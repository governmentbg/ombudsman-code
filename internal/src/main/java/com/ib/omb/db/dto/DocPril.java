package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.text.SimpleDateFormat;
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
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Приложение на външен носител
 *
 * @author belev
 */
@Entity
@Table(name = "DOC_PRIL")
public class DocPril extends TrackableEntity implements AuditExt{
	/**  */
	private static final long serialVersionUID = 6626103267314812266L;

	@SequenceGenerator(name = "DocPril", sequenceName = "SEQ_DOC_PRIL", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DocPril")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "DOC_ID", updatable = false)
	private Integer docId;

	@Column(name = "NOMER")
	@JournalAttr(label = "nomer",defaultText = "Номер на приложение")
	private String nomer;

	@Column(name = "MEDIA_TYPE")
	@JournalAttr(label = "mediaType",defaultText = "Вид носител",classifID = "106")
	private Integer mediaType;

	@Column(name = "PRIL_NAME")
	@JournalAttr(label = "prilTame",defaultText = "Наименование")
	private String prilTame;

	@Column(name = "PRIL_INFO")
	@JournalAttr(label = "prilInfo",defaultText = "Информация")
	private String prilInfo;

	@Column(name = "COUNT_SHEETS")
	@JournalAttr(label = "countSheets",defaultText = "Брой листа")
	private Integer countSheets;

	/** */
	public DocPril() {
		super();
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_PRIL;
	}

	/** @return the countSheets */
	public Integer getCountSheets() {
		return this.countSheets;
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

	/** @return the mediaType */
	public Integer getMediaType() {
		return this.mediaType;
	}

	/** @return the nomer */
	public String getNomer() {
		return this.nomer;
	}

	/** @return the prilInfo */
	public String getPrilInfo() {
		return this.prilInfo;
	}

	/** @return the prilTame */
	public String getPrilTame() {
		return this.prilTame;
	}

	/** @param countSheets the countSheets to set */
	public void setCountSheets(Integer countSheets) {
		this.countSheets = countSheets;
	}

	/** @param docId the docId to set */
	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param mediaType the mediaType to set */
	public void setMediaType(Integer mediaType) {
		this.mediaType = mediaType;
	}

	/** @param nomer the nomer to set */
	public void setNomer(String nomer) {
		this.nomer = nomer;
	}

	/** @param prilInfo the prilInfo to set */
	public void setPrilInfo(String prilInfo) {
		this.prilInfo = prilInfo;
	}

	/** @param prilTame the prilTame to set */
	public void setPrilTame(String prilTame) {
		this.prilTame = prilTame;
	}

	@Override
	public String getIdentInfo() throws DbErrorException {
		
		String ident = "";
		if (nomer != null) {
			ident += nomer + ". ";
		}
		if (prilTame != null) {
			ident += prilTame;
		}
		return ident;
	}
	
	
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new  SystemJournal();				
		dj.setCodeObject(getCodeMainObject());
		dj.setIdObject(getId());
		dj.setIdentObject(getIdentInfo());
		dj.setDateAction(new Date());
		if (docId != null) {
			dj.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
			dj.setJoinedIdObject1(docId);
		}
		return dj;
	}
}
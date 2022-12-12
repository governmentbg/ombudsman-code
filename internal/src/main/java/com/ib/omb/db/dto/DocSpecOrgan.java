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

import com.ib.indexui.system.Constants;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.JournalAttr;

/**
 * Проверяван орган
 *
 * @author belev
 */
@Entity
@Table(name = "doc_spec_organ")
public class DocSpecOrgan implements Serializable {

	/**  */
	private static final long serialVersionUID = 2179491865641679607L;

	@SequenceGenerator(name = "DocSpecOrgan", sequenceName = "seq_doc_spec_organ", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DocSpecOrgan")
	@Column(name = "id", unique = true, nullable = false)
	@JournalAttr(label = "id", defaultText = "Системен идентификатор", isId = "true")
	private Integer id;

	@Column(name = "doc_id", insertable = false, updatable = false)
	private Integer docId;

	@JournalAttr(label = "codeOrgan", defaultText = "Проверен орган", classifID = "" + Constants.CODE_CLASSIF_REFERENTS)
	@Column(name = "code_organ")
	private Integer codeOrgan;

	@JournalAttr(label = "narPrava", defaultText = "Нарушени права", classifID = "" + OmbConstants.CODE_CLASSIF_NAR_PRAVA)
	@Column(name = "nar_prava")
	private Integer narPrava;

	@JournalAttr(label = "zasPrava", defaultText = "Нарушени права", classifID = "" + OmbConstants.CODE_CLASSIF_ZAS_PRAVA)
	@Column(name = "zas_prava")
	private Integer zasPrava;

	@JournalAttr(label = "capacity", defaultText = "Капацитет на проверения орган")
	@Column(name = "capacity")
	private Integer capacity;

	@JournalAttr(label = "nasLica", defaultText = "Реално настанени лица")
	@Column(name = "nas_lica")
	private Integer nasLica;

	@JournalAttr(label = "konstat", defaultText = "Констатации")
	@Column(name = "konstat")
	private String konstat;

	@JournalAttr(label = "prepor", defaultText = "Дадена препоръка", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "prepor")
	private Integer prepor;

	@JournalAttr(label = "vidResult", defaultText = "Резултат от проверка", classifID = "" + OmbConstants.CODE_CLASSIF_ORGAN_RES)
	@Column(name = "vid_result")
	private Integer vidResult;

	@JournalAttr(label = "dateResult", defaultText = "Дата на резултата")
	@Column(name = "date_result")
	private Date dateResult;

	/**  */
	public DocSpecOrgan() {
		super();
	}

	/** @return the capacity */
	public Integer getCapacity() {
		return this.capacity;
	}

	/** @return the codeOrgan */
	public Integer getCodeOrgan() {
		return this.codeOrgan;
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

	/** @return the konstat */
	public String getKonstat() {
		return this.konstat;
	}

	/** @return the narPrava */
	public Integer getNarPrava() {
		return this.narPrava;
	}

	/** @return the nasLica */
	public Integer getNasLica() {
		return this.nasLica;
	}

	/** @return the prepor */
	public Integer getPrepor() {
		return this.prepor;
	}

	/** @return the vidResult */
	public Integer getVidResult() {
		return this.vidResult;
	}

	/** @return the zasPrava */
	public Integer getZasPrava() {
		return this.zasPrava;
	}

	/** @param capacity the capacity to set */
	public void setCapacity(Integer capacity) {
		this.capacity = capacity;
	}

	/** @param codeOrgan the codeOrgan to set */
	public void setCodeOrgan(Integer codeOrgan) {
		this.codeOrgan = codeOrgan;
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

	/** @param konstat the konstat to set */
	public void setKonstat(String konstat) {
		this.konstat = konstat;
	}

	/** @param narPrava the narPrava to set */
	public void setNarPrava(Integer narPrava) {
		this.narPrava = narPrava;
	}

	/** @param nasLica the nasLica to set */
	public void setNasLica(Integer nasLica) {
		this.nasLica = nasLica;
	}

	/** @param prepor the prepor to set */
	public void setPrepor(Integer prepor) {
		this.prepor = prepor;
	}

	/** @param vidResult the vidResult to set */
	public void setVidResult(Integer vidResult) {
		this.vidResult = vidResult;
	}

	/** @param zasPrava the zasPrava to set */
	public void setZasPrava(Integer zasPrava) {
		this.zasPrava = zasPrava;
	}

}

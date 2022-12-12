package com.ib.omb.db.dto;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import com.ib.indexui.system.Constants;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.PersistentEntity;

/**
 * Заповед за проверка по НПМ <br>
 * Решение за проверка по самосезиране
 *
 * @author belev
 */
@Entity
@Table(name = "doc_spec")
public class DocSpec implements PersistentEntity {

	/**  */
	private static final long serialVersionUID = -6722807115411540056L;

	@Id
	@Column(name = "doc_id", unique = true, nullable = false)
	private Integer id; // съвпада с ID от таблица DOC

	@JournalAttr(label = "codeLeader", defaultText = "Ръководител на екип", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	@Column(name = "code_leader")
	private Integer codeLeader;

	@Transient
	@JournalAttr(label = "dopExperts", defaultText = "Допълнителен експерт", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private List<Integer>			dopExpertCodes;
	@Transient
	private transient List<Integer>	dbDopExpertCodes;

	@JournalAttr(label = "startDate", defaultText = "Дата на започване")
	@Column(name = "start_date")
	private Date startDate;

	@JournalAttr(label = "srok", defaultText = "Срок за приключване")
	@Column(name = "srok")
	private Date srok;

	@JournalAttr(label = "sast", defaultText = "Състояние", classifID = "" + OmbConstants.CODE_CLASSIF_PROVERKA_SAST)
	@Column(name = "sast")
	private Integer sast;

	@JournalAttr(label = "sastDate", defaultText = "Дата на състоянието", dateMask = "dd.MM.yyyy HH:mm:ss")
	@Column(name = "sast_date")
	private Date sastDate;

	@JournalAttr(label = "publicVisible", defaultText = "Видима в публичния регистър", classifID = "" + SysConstants.CODE_CLASSIF_DANE)
	@Column(name = "public_visible")
	private Integer publicVisible;

	@JournalAttr(label = "specOrganList", defaultText = "Проверяван орган")
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	@JoinColumn(name = "doc_id", referencedColumnName = "doc_id", nullable = false)
	private List<DocSpecOrgan> specOrganList;

	@Column(name = "user_last_mod")
	private Integer userLastMod;

	@Column(name = "date_last_mod")
	private Date dateLastMod;

	@Transient
	private Integer dbSast;
	
	@Transient
	private Boolean deloStatusChanged; // когато състоянието на документа влияе на статуса на преписката, ще се направи true

	/**  */
	public DocSpec() {
		super();
	}

	/** @return the codeLeader */
	public Integer getCodeLeader() {
		return this.codeLeader;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return null; // запис в контекста на документ
	}

	/** @return the dateLastMod */
	public Date getDateLastMod() {
		return this.dateLastMod;
	}

	/** @return the dbDopExpertCodes */
	@XmlTransient
	public List<Integer> getDbDopExpertCodes() {
		return this.dbDopExpertCodes;
	}

	/** @return the dbSast */
	public Integer getDbSast() {
		return this.dbSast;
	}

	/** @return the dopExpertCodes */
	public List<Integer> getDopExpertCodes() {
		return this.dopExpertCodes;
	}

	/** @return the id */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the publicVisible */
	public Integer getPublicVisible() {
		return this.publicVisible;
	}

	/** @return the sast */
	public Integer getSast() {
		return this.sast;
	}

	/** @return the sastDate */
	public Date getSastDate() {
		return this.sastDate;
	}

	/** @return the specOrganList */
	public List<DocSpecOrgan> getSpecOrganList() {
		return this.specOrganList;
	}

	/** @return the srok */
	public Date getSrok() {
		return this.srok;
	}

	/** @return the startDate */
	public Date getStartDate() {
		return this.startDate;
	}

	/** @return the userLastMod */
	public Integer getUserLastMod() {
		return this.userLastMod;
	}

	/** @param codeLeader the codeLeader to set */
	public void setCodeLeader(Integer codeLeader) {
		this.codeLeader = codeLeader;
	}

	/** @param dateLastMod the dateLastMod to set */
	@Override
	public void setDateLastMod(Date dateLastMod) {
		this.dateLastMod = dateLastMod;
	}

	/** @param dbDopExpertCodes the dbDopExpertCodes to set */
	public void setDbDopExpertCodes(List<Integer> dbDopExpertCodes) {
		this.dbDopExpertCodes = dbDopExpertCodes;
	}

	/** @param dbSast the dbSast to set */
	public void setDbSast(Integer dbSast) {
		this.dbSast = dbSast;
	}

	/** @param dopExpertCodes the dopExpertCodes to set */
	public void setDopExpertCodes(List<Integer> dopExpertCodes) {
		this.dopExpertCodes = dopExpertCodes;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param publicVisible the publicVisible to set */
	public void setPublicVisible(Integer publicVisible) {
		this.publicVisible = publicVisible;
	}

	/** @param sast the sast to set */
	public void setSast(Integer sast) {
		this.sast = sast;
	}

	/** @param sastDate the sastDate to set */
	public void setSastDate(Date sastDate) {
		this.sastDate = sastDate;
	}

	/** @param specOrganList the specOrganList to set */
	public void setSpecOrganList(List<DocSpecOrgan> specOrganList) {
		this.specOrganList = specOrganList;
	}

	/** @param srok the srok to set */
	public void setSrok(Date srok) {
		this.srok = srok;
	}

	/** @param startDate the startDate to set */
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	/** @param userLastMod the userLastMod to set */
	@Override
	public void setUserLastMod(Integer userLastMod) {
		this.userLastMod = userLastMod;
	}

	/** @return the deloStatusChanged */
	public Boolean getDeloStatusChanged() {
		return this.deloStatusChanged;
	}

	/** @param deloStatusChanged the deloStatusChanged to set */
	public void setDeloStatusChanged(Boolean deloStatusChanged) {
		this.deloStatusChanged = deloStatusChanged;
	}
}

package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.List;

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
 * Група на регистратура
 *
 * @author belev
 */
@Entity
@Table(name = "REGISTRATURA_GROUPS")
public class RegistraturaGroup extends TrackableEntity implements AuditExt {
	/**  */
	private static final long serialVersionUID = -8917301483071636008L;

	@SequenceGenerator(name = "RegistraturaGroup", sequenceName = "SEQ_REGISTRATURA_GROUPS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "RegistraturaGroup")
	@Column(name = "GROUP_ID", unique = true, nullable = false)
	private Integer id;

	@JournalAttr(label = "registraturaId", defaultText = "Регистратура", classifID = "" + OmbConstants.CODE_CLASSIF_REGISTRATURI)
	@Column(name = "REGISTRATURA_ID", updatable = false)
	private Integer registraturaId;

	@JournalAttr(label = "groupType", defaultText = "Тип на групата", classifID = "" + OmbConstants.CODE_CLASSIF_REGISTRATURA_GROUP_TYPE)
	@Column(name = "GROUP_TYPE")
	private Integer groupType;

	@JournalAttr(label = "regGrSluj.naimGr", defaultText = "Наименование на групата")
	@Column(name = "GROUP_NAME")
	private String groupName;

	@JournalAttr(label = "regGrSluj.opisGr", defaultText = "Описание на групата")
	@Column(name = "GROUP_INFO")
	private String groupInfo;

	@Transient
	private transient List<Integer> referentIds; // използва се от компонентата за избор

	/**  */
	public RegistraturaGroup() {
		super();
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA_GROUP;
	}

	/** @return the groupInfo */
	public String getGroupInfo() {
		return this.groupInfo;
	}

	/** @return the groupName */
	public String getGroupName() {
		return this.groupName;
	}

	/** @return the groupType */
	public Integer getGroupType() {
		return this.groupType;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		if (this.groupType == null) {
			return this.groupName;
		}
		String groupTypeText;
		if (this.groupType.equals(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP)) {
			groupTypeText = "група кореспонденти";
		} else if (this.groupType.equals(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_EMPL)) {
			groupTypeText = "група служители";
		} else if (this.groupType.equals(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS)) {
			groupTypeText = "група СЕОС";
		} else {
			groupTypeText = "тип непознат";
		}
		return groupTypeText + " - " + this.groupName;
	}

	/** @return the referentIds */
	public List<Integer> getReferentIds() {
		return this.referentIds;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @param groupInfo the groupInfo to set */
	public void setGroupInfo(String groupInfo) {
		this.groupInfo = groupInfo;
	}

	/** @param groupName the groupName to set */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/** @param groupType the groupType to set */
	public void setGroupType(Integer groupType) {
		this.groupType = groupType;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param referentIds the referentIds to set */
	public void setReferentIds(List<Integer> referentIds) {
		this.referentIds = referentIds;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		return journal;
	}

}
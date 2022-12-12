package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

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
 * Схема за съхранение на документи
 *
 * @author belev
 */
@Entity
@Table(name = "DOC_SHEMA")
public class DocShema extends TrackableEntity implements AuditExt {
	/**  */
	private static final long serialVersionUID = -6341640582083271719L;

	@SequenceGenerator(name = "DocShema", sequenceName = "SEQ_DOC_SHEMA", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DocShema")
	@Column(name = "SHEMA_ID", unique = true, nullable = false)
	private Integer id;

	@JournalAttr(label = "missing.shemaName", defaultText = "Наименование")
	@Column(name = "SHEMA_NAME")
	private String shemaName;

	@JournalAttr(label = "register.prefix", defaultText = "Индекс")
	@Column(name = "PREFIX")
	private String prefix;

	@JournalAttr(label = "docSchema.validFrom", defaultText = "Валиден от")
	@Column(name = "FROM_YEAR")
	private Integer fromYear;

	@JournalAttr(label = "docSchema.validTo", defaultText = "Валиден до")
	@Column(name = "TO_YEAR")
	private Integer toYear;

	@JournalAttr(label = "docSchema.termStore", defaultText = "Срок за съхранение", classifID = "" + OmbConstants.CODE_CLASSIF_SHEMA_PERIOD)
	@Column(name = "PERIOD_TYPE")
	private Integer periodType;

	@JournalAttr(label = "docSchema.years", defaultText = "Брой години")
	@Column(name = "YEARS")
	private Integer years;

	@JournalAttr(label = "docSchema.complMethodLong", defaultText = "Начин на приключване на номенклатурно дело", classifID = "" + OmbConstants.CODE_CLASSIF_NOM_DELO_PRIKL)
	@Column(name = "COMPLETE_METHOD")
	private Integer completeMethod;

	@Transient
	private transient String	dbPrefix;
	@Transient
	private transient Integer	dbFromYear;
	@Transient
	private transient Integer	dbToYear;

	/** */
	public DocShema() {
		super();
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_SHEMA;
	}

	/** @return the completeMethod */
	public Integer getCompleteMethod() {
		return this.completeMethod;
	}

	/** @return the dbFromYear */
	public Integer getDbFromYear() {
		return this.dbFromYear;
	}

	/** @return the dbPrefix */
	public String getDbPrefix() {
		return this.dbPrefix;
	}

	/** @return the dbToYear */
	public Integer getDbToYear() {
		return this.dbToYear;
	}

	/** @return the fromYear */
	public Integer getFromYear() {
		return this.fromYear;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		return getPrefix();
	}

	/** @return the periodType */
	public Integer getPeriodType() {
		return this.periodType;
	}

	/** @return the prefix */
	public String getPrefix() {
		return this.prefix;
	}

	/** @return the shemaName */
	public String getShemaName() {
		return this.shemaName;
	}

	/** @return the toYear */
	public Integer getToYear() {
		return this.toYear;
	}

	/** @return the years */
	public Integer getYears() {
		return this.years;
	}

	/** @param completeMethod the completeMethod to set */
	public void setCompleteMethod(Integer completeMethod) {
		this.completeMethod = completeMethod;
	}

	/** @param dbFromYear the dbFromYear to set */
	public void setDbFromYear(Integer dbFromYear) {
		this.dbFromYear = dbFromYear;
	}

	/** @param dbPrefix the dbPrefix to set */
	public void setDbPrefix(String dbPrefix) {
		this.dbPrefix = dbPrefix;
	}

	/** @param dbToYear the dbToYear to set */
	public void setDbToYear(Integer dbToYear) {
		this.dbToYear = dbToYear;
	}

	/** @param fromYear the fromYear to set */
	public void setFromYear(Integer fromYear) {
		this.fromYear = fromYear;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param periodType the periodType to set */
	public void setPeriodType(Integer periodType) {
		this.periodType = periodType;
	}

	/** @param prefix the prefix to set */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/** @param shemaName the shemaName to set */
	public void setShemaName(String shemaName) {
		this.shemaName = shemaName;
	}

	/** @param toYear the toYear to set */
	public void setToYear(Integer toYear) {
		this.toYear = toYear;
	}

	/** @param years the years to set */
	public void setYears(Integer years) {
		this.years = years;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		return journal;
	}

}
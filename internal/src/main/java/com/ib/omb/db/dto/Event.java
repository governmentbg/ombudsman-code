package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

import com.ib.indexui.system.Constants;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Организирано събитие
 *
 * @author belev
 */
@Entity
@Table(name = "EVENTS")
public class Event extends TrackableEntity implements AuditExt {

	/**  */
	private static final long serialVersionUID = 775166789460196878L;

	@SequenceGenerator(name = "Event", sequenceName = "SEQ_EVENTS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "Event")
	@Column(name = "EVENT_ID", unique = true, nullable = false)
	private Integer id;

	@JournalAttr(label = "events.vid", defaultText = "Вид организирано събитие", classifID = "" + OmbConstants.CODE_CLASSIF_VID_EVENT)
	@Column(name = "EVENT_CODE")
	private Integer eventCode;

	@JournalAttr(label = "procDefList.opisProc", defaultText = "Описание")
	@Column(name = "EVENT_INFO")
	private String eventInfo;

	@JournalAttr(label = "docu.note", defaultText = "Забележка")
	@Column(name = "EVENT_NOTE")
	private String eventNote;

	@JournalAttr(label = "procList.begin", defaultText = "Начало", dateMask = "dd.MM.yyyy HH:mm")
	@Column(name = "DATE_OT")
	private Date dateOt;

	@JournalAttr(label = "procList.end", defaultText = "Край", dateMask = "dd.MM.yyyy HH:mm")
	@Column(name = "DATE_DO")
	private Date dateDo;

	@JournalAttr(label = "events.organizator", defaultText = "Организатор", classifID = "" + SysConstants.CODE_CLASSIF_USERS)
	@Column(name = "ORGANIZATOR")
	private Integer organizator;

//	@JournalAttr(label = "dateLastNotif", defaultText = "Време на последна нотификация за нови материали", dateMask = "dd.MM.yyyy HH:mm:ss")
	@Column(name = "DATE_LAST_NOTIF")
	private Date dateLastNotif;

	@JournalAttr(label = "global.country", defaultText = "Държава", classifID = "" + Constants.CODE_CLASSIF_COUNTRIES)
	@Column(name = "ADDR_COUNTRY")
	private Integer addrCountry;

	@JournalAttr(label = "missing.ekatte", defaultText = "Населено място", classifID = "" + SysConstants.CODE_CLASSIF_EKATTE)
	@Column(name = "EKATTE")
	private Integer ekatte;

	@JournalAttr(label = "missing.addrText", defaultText = "Адрес")
	@Column(name = "ADDR_TEXT")
	private String addrText;

	@JournalAttr(label = "procDefEdit.referentsTask", defaultText = "Участници", classifID = "" + SysConstants.CODE_CLASSIF_USERS)
	@Transient
	private List<Integer>			codeRefList;
	@Transient
	private transient List<Integer>	dbCodeRefList;

	@JournalAttr(label = "events.resourses", defaultText = "Ресурси", classifID = "" + OmbConstants.CODE_CLASSIF_EVENT_RESOURCES)
	@Transient
	private List<Integer>			resourcesList;
	@Transient
	private transient List<Integer>	dbResourcesList;

	@Transient
	private transient Integer hashNotifData; // тъй като се прави нотификация за промяна на основни данни, тук ще се пази хеш
												// на тези основни данни и на запис ще се сравнява с новия такъв
												// Objects.hash(eventCode, dateOt, dateDo, eventInfo, eventNote
												// , addrCountry, ekatte, addrText)

	@Transient
	private transient Integer dbOrganizator;

	@Transient
	private transient Boolean auditable; // за да може да се включва и изключва журналирането

	/** */
	public Event() {
		super();
	}

	/** @return the addrCountry */
	public Integer getAddrCountry() {
		return this.addrCountry;
	}

	/** @return the addrText */
	public String getAddrText() {
		return this.addrText;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_EVENT;
	}

	/** @return the codeRefList */
	public List<Integer> getCodeRefList() {
		return this.codeRefList;
	}

	/** @return the dateDo */
	public Date getDateDo() {
		return this.dateDo;
	}

	/** @return the dateLastNotif */
	public Date getDateLastNotif() {
		return this.dateLastNotif;
	}

	/** @return the dateOt */
	public Date getDateOt() {
		return this.dateOt;
	}

	/** @return the dbCodeRefList */
	@XmlTransient
	public List<Integer> getDbCodeRefList() {
		return this.dbCodeRefList;
	}

	/** @return the dbOrganizator */
	public Integer getDbOrganizator() {
		return this.dbOrganizator;
	}

	/** @return the dbResourcesList */
	@XmlTransient
	public List<Integer> getDbResourcesList() {
		return this.dbResourcesList;
	}

	/** @return the ekatte */
	public Integer getEkatte() {
		return this.ekatte;
	}

	/** @return the eventCode */
	public Integer getEventCode() {
		return this.eventCode;
	}

	/** @return the eventInfo */
	public String getEventInfo() {
		return this.eventInfo;
	}

	/** @return the eventNote */
	public String getEventNote() {
		return this.eventNote;
	}

	/** @return the hashNotifData */
	@XmlTransient
	public Integer getHashNotifData() {
		return this.hashNotifData;
	}

	/** @return the id */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the organizator */
	public Integer getOrganizator() {
		return this.organizator;
	}

	/** @return the resourcesList */
	public List<Integer> getResourcesList() {
		return this.resourcesList;
	}

	@Override
	public boolean isAuditable() {
		return this.auditable == null ? super.isAuditable() : this.auditable.booleanValue();
	}

	/** @param addrCountry the addrCountry to set */
	public void setAddrCountry(Integer addrCountry) {
		this.addrCountry = addrCountry;
	}

	/** @param addrText the addrText to set */
	public void setAddrText(String addrText) {
		this.addrText = addrText;
	}

	public void setAuditable(Boolean auditable) {
		this.auditable = auditable;
	}

	/** @param codeRefList the codeRefList to set */
	public void setCodeRefList(List<Integer> codeRefList) {
		this.codeRefList = codeRefList;
	}

	/** @param dateDo the dateDo to set */
	public void setDateDo(Date dateDo) {
		this.dateDo = dateDo;
	}

	/** @param dateLastNotif the dateLastNotif to set */
	public void setDateLastNotif(Date dateLastNotif) {
		this.dateLastNotif = dateLastNotif;
	}

	/** @param dateOt the dateOt to set */
	public void setDateOt(Date dateOt) {
		this.dateOt = dateOt;
	}

	/** @param dbCodeRefList the dbCodeRefList to set */
	public void setDbCodeRefList(List<Integer> dbCodeRefList) {
		this.dbCodeRefList = dbCodeRefList;
	}

	/** @param dbOrganizator the dbOrganizator to set */
	public void setDbOrganizator(Integer dbOrganizator) {
		this.dbOrganizator = dbOrganizator;
	}

	/** @param dbResourcesList the dbResourcesList to set */
	public void setDbResourcesList(List<Integer> dbResourcesList) {
		this.dbResourcesList = dbResourcesList;
	}

	/** @param ekatte the ekatte to set */
	public void setEkatte(Integer ekatte) {
		this.ekatte = ekatte;
	}

	/** @param eventCode the eventCode to set */
	public void setEventCode(Integer eventCode) {
		this.eventCode = eventCode;
	}

	/** @param eventInfo the eventInfo to set */
	public void setEventInfo(String eventInfo) {
		this.eventInfo = eventInfo;
	}

	/** @param eventNote the eventNote to set */
	public void setEventNote(String eventNote) {
		this.eventNote = eventNote;
	}

	/** @param hashNotifData the hashNotifData to set */
	public void setHashNotifData(Integer hashNotifData) {
		this.hashNotifData = hashNotifData;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param organizator the organizator to set */
	public void setOrganizator(Integer organizator) {
		this.organizator = organizator;
	}

	/** @param resourcesList the resourcesList to set */
	public void setResourcesList(List<Integer> resourcesList) {
		this.resourcesList = resourcesList;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		return journal;
	}
}
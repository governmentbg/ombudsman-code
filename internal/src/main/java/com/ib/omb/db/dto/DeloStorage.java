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
 * Съхранение на том на преписка
 *
 * @author belev
 */
@Entity
@Table(name = "DELO_STORAGE")
public class DeloStorage extends TrackableEntity implements AuditExt {
	/**  */
	private static final long serialVersionUID = 712927560823878846L;

	@SequenceGenerator(name = "DeloStorage", sequenceName = "SEQ_DELO_STORAGE", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DeloStorage")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "DELO_ID", updatable = false)
	private Integer deloId;

	@Column(name = "TOM_NOMER")
	@JournalAttr(label = "tomNomer", defaultText = "Том №")
	private Integer tomNomer;

	@Column(name = "ARCH_NOMER")
	@JournalAttr(label = "archNomer", defaultText = "Арх.Ном.")
	private String archNomer;

	@Column(name = "ROOM")
	@JournalAttr(label = "room", defaultText = "Помещение")
	private String room;

	@Column(name = "SHKAF")
	@JournalAttr(label = "shkaf", defaultText = "Шкаф")
	private String shkaf;

	@Column(name = "STILLAGE")
	@JournalAttr(label = "stillage", defaultText = "Стелаж")
	private String stillage;

	@Column(name = "BOX")
	@JournalAttr(label = "box", defaultText = "Кутия")
	private String box;

	@Transient
	private String deloIdent;

	/** */
	public DeloStorage() {
		super();
	}

	/** @return the archNomer */
	public String getArchNomer() {
		return this.archNomer;
	}

	/** @return the box */
	public String getBox() {
		return this.box;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_STORAGE;
	}

	/** @return the deloId */
	public Integer getDeloId() {
		return this.deloId;
	}

	/** @return the deloIdent */
	public String getDeloIdent() {
		return this.deloIdent;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		String ident = "Преписка/дело " + this.deloIdent + ". Том № " + this.tomNomer;
		if (this.archNomer != null && this.archNomer.trim().length() > 0) {
			return ident + ", Арх.Ном. " + this.archNomer + ".";
		}
		return ident;
	}

	/** @return the room */
	public String getRoom() {
		return this.room;
	}

	/** @return the shkaf */
	public String getShkaf() {
		return this.shkaf;
	}

	/** @return the stillage */
	public String getStillage() {
		return this.stillage;
	}

	/** @return the tomNomer */
	public Integer getTomNomer() {
		return this.tomNomer;
	}

	/** @param archNomer the archNomer to set */
	public void setArchNomer(String archNomer) {
		this.archNomer = archNomer;
	}

	/** @param box the box to set */
	public void setBox(String box) {
		this.box = box;
	}

	/** @param deloId the deloId to set */
	public void setDeloId(Integer deloId) {
		this.deloId = deloId;
	}

	/** @param deloIdent the deloIdent to set */
	public void setDeloIdent(String deloIdent) {
		this.deloIdent = deloIdent;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param room the room to set */
	public void setRoom(String room) {
		this.room = room;
	}

	/** @param shkaf the shkaf to set */
	public void setShkaf(String shkaf) {
		this.shkaf = shkaf;
	}

	/** @param stillage the stillage to set */
	public void setStillage(String stillage) {
		this.stillage = stillage;
	}

	/** @param tomNomer the tomNomer to set */
	public void setTomNomer(Integer tomNomer) {
		this.tomNomer = tomNomer;
	}

	/**  */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO);
		journal.setJoinedIdObject1(this.deloId);

		return journal;
	}
}
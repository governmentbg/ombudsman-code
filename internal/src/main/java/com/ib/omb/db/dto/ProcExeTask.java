package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

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
import com.ib.system.db.PersistentEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Задача от Изпълнение на етап от процедура
 *
 * @author belev
 */
@Entity
@Table(name = "PROC_EXE_TASK")
public class ProcExeTask implements PersistentEntity, AuditExt {

	/**  */
	private static final long serialVersionUID = 8921146796535680498L;

	@SequenceGenerator(name = "ProcExeTask", sequenceName = "SEQ_PROC_EXE_TASK", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "ProcExeTask")
	@Column(name = "EXE_TASK_ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "EXE_ETAP_ID")
	private Integer exeEtapId;

	@JournalAttr(label = "taskId", defaultText = "Задача", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK)
	@Column(name = "TASK_ID")
	private Integer taskId;

	@Column(name = "USER_REG")
	private Integer userReg;

	@Column(name = "DATE_REG")
	private Date dateReg;

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE_TASK;
	}

	/** @return the dateReg */
	public Date getDateReg() {
		return this.dateReg;
	}

	/** @return the exeEtapId */
	public Integer getExeEtapId() {
		return this.exeEtapId;
	}

	/** @return the id */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** */
	@Override
	public String getIdentInfo() throws DbErrorException {
		return "ИД Изп.Етап: " + this.exeEtapId;
	}

	/** @return the taskId */
	public Integer getTaskId() {
		return this.taskId;
	}

	/** @return the userReg */
	public Integer getUserReg() {
		return this.userReg;
	}

	/** @param dateReg the dateReg to set */
	@Override
	public void setDateReg(Date dateReg) {
		this.dateReg = dateReg;
	}

	/** @param exeEtapId the exeEtapId to set */
	public void setExeEtapId(Integer exeEtapId) {
		this.exeEtapId = exeEtapId;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param taskId the taskId to set */
	public void setTaskId(Integer taskId) {
		this.taskId = taskId;
	}

	/** @param userReg the userReg to set */
	@Override
	public void setUserReg(Integer userReg) {
		this.userReg = userReg;
	}

	/** */
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal journal = new SystemJournal(getCodeMainObject(), getId(), getIdentInfo());

		journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE_ETAP);
		journal.setJoinedIdObject1(this.exeEtapId);

		return journal;
	}
}
package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.ib.system.db.PersistentEntity;

/**
 * Участник в задача
 *
 * @author belev
 */
@Entity
@Table(name = "TASK_REFERENTS")
public class TaskReferent implements PersistentEntity {
	/**  */
	private static final long serialVersionUID = 2795997555612599590L;

	@SequenceGenerator(name = "TaskReferent", sequenceName = "SEQ_TASK_REFERENTS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "TaskReferent")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "TASK_ID", updatable = false)
	private Integer taskId;

	@Column(name = "CODE_REF")
	private Integer codeRef;

	@Column(name = "ROLE_REF")
	private Integer roleRef;

	@Column(name = "USER_REG")
	private Integer userReg;

	@Column(name = "DATE_REG")
	private Date dateReg;

	/**  */
	public TaskReferent() {
		super();
	}

	/**
	 * @param taskId
	 * @param codeRef
	 * @param roleRef
	 */
	public TaskReferent(Integer taskId, Integer codeRef, Integer roleRef) {
		this.taskId = taskId;
		this.codeRef = codeRef;
		this.roleRef = roleRef;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return null; // този няма да се журналира самостоятелно, а в контекста на задачата
	}

	/** @return the codeRef */
	public Integer getCodeRef() {
		return this.codeRef;
	}

	/** @return the dateReg */
	public Date getDateReg() {
		return this.dateReg;
	}

	/** */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the roleRef */
	public Integer getRoleRef() {
		return this.roleRef;
	}

	/** @return the taskId */
	public Integer getTaskId() {
		return this.taskId;
	}

	/** @return the userReg */
	public Integer getUserReg() {
		return this.userReg;
	}

	/** @see PersistentEntity#isAuditable() */
	@Override
	public boolean isAuditable() {
		return false;
	}

	/** @param codeRef the codeRef to set */
	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	/** @param dateReg the dateReg to set */
	@Override
	public void setDateReg(Date dateReg) {
		this.dateReg = dateReg;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param roleRef the roleRef to set */
	public void setRoleRef(Integer roleRef) {
		this.roleRef = roleRef;
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
}
package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.ib.indexui.system.Constants;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.PersistentEntity;

/**
 * Дефиниция на изпълинител на задача от етап на Процедура
 *
 * @author belev
 */
@Entity
@Table(name = "PROC_DEF_TASK_IZP")
public class ProcDefTaskIzp implements PersistentEntity {

	/**  */
	private static final long serialVersionUID = -2394459429589453157L;

	@SequenceGenerator(name = "ProcDefTaskIzp", sequenceName = "SEQ_PROC_DEF_TASK_IZP", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "ProcDefTaskIzp")
	@Column(name = "ID", unique = true, nullable = false)
	@JournalAttr(label = "id", defaultText = "Системен идентификатор", isId = "true")
	private Integer id;

	@Column(name = "DEF_TASK_ID", insertable = false, updatable = false)
	private Integer defTaskId;

	@Column(name = "IZP_ROLE")
	@JournalAttr(label = "izpRole", defaultText = "Изпълнител - Роля", classifID = "" + OmbConstants.CODE_CLASSIF_TASK_REF_ROLE)
	private Integer izpRole;

	@Column(name = "CODE_REF")
	@JournalAttr(label = "codeRef", defaultText = "Изпълнител - Служител", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private Integer codeRef;

	@Column(name = "ZVENO")
	@JournalAttr(label = "zveno", defaultText = "Изпълнител - Звено", classifID = "" + Constants.CODE_CLASSIF_ADMIN_STR)
	private Integer zveno;

	@Column(name = "EMPL_POSITION")
	@JournalAttr(label = "emplPosition", defaultText = "Изпълнител - Длъжност", classifID = "" + Constants.CODE_CLASSIF_POSITION)
	private Integer emplPosition;

	@Column(name = "BUSINESS_ROLE")
	@JournalAttr(label = "businessRole", defaultText = "Изпълнител - Бизнес роля", classifID = "" + OmbConstants.CODE_CLASSIF_PROC_BUSINESS_ROLE)
	private Integer businessRole;

	/** */
	public ProcDefTaskIzp() {
		super();
	}

	/**
	 * @param izp
	 */
	public ProcDefTaskIzp(ProcDefTaskIzp izp) {
		this.izpRole = izp.izpRole;
		this.codeRef = izp.codeRef;
		this.zveno = izp.zveno;
		this.emplPosition = izp.emplPosition;
		this.businessRole = izp.businessRole;
	}

	/** @return the businessRole */
	public Integer getBusinessRole() {
		return this.businessRole;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return null;
	}

	/** @return the codeRef */
	public Integer getCodeRef() {
		return this.codeRef;
	}

	/** @return the defTaskId */
	public Integer getDefTaskId() {
		return this.defTaskId;
	}

	/** @return the emplPosition */
	public Integer getEmplPosition() {
		return this.emplPosition;
	}

	/** @return the id */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the izpRole */
	public Integer getIzpRole() {
		return this.izpRole;
	}

	/** @return the zveno */
	public Integer getZveno() {
		return this.zveno;
	}

	/** */
	@Override
	public boolean isAuditable() {
		return false;
	}

	/** @param businessRole the businessRole to set */
	public void setBusinessRole(Integer businessRole) {
		this.businessRole = businessRole;
	}

	/** @param codeRef the codeRef to set */
	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	/** @param defTaskId the defTaskId to set */
	public void setDefTaskId(Integer defTaskId) {
		this.defTaskId = defTaskId;
	}

	/** @param emplPosition the emplPosition to set */
	public void setEmplPosition(Integer emplPosition) {
		this.emplPosition = emplPosition;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param izpRole the izpRole to set */
	public void setIzpRole(Integer izpRole) {
		this.izpRole = izpRole;
	}

	/** @param zveno the zveno to set */
	public void setZveno(Integer zveno) {
		this.zveno = zveno;
	}
}
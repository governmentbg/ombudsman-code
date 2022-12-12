package com.ib.omb.db.dto;


import static javax.persistence.GenerationType.SEQUENCE;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JournalAttr;

@Entity
@Table(name = "DELO_ACCESS")
public class DeloAccess implements Serializable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 7552266728587896361L;

	@SequenceGenerator(name = "DeloAccess", sequenceName = "SEQ_DELO_ACCESS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "DeloAccess")
	@Column(name = "ID", unique = true, nullable = false)
	@JournalAttr(label="id",defaultText = "Системен идентификатор", isId = "true")
	private Integer id;

	@Column(name = "DELO_ID")
	private Integer deloId;

	@Column(name = "CODE_REF")
	@JournalAttr(label="codeRef",defaultText = "Служител", classifID = "24")
	private Integer codeRef;
	
	@Column(name = "EXCLUDE")
	@JournalAttr(label="exclude",defaultText = "Блокиран достъп", classifID = ""+ OmbConstants.CODE_CLASSIF_DANE)
	private Integer exclude;

	@Transient
	private Integer flag = 0;
	
	
	public DeloAccess() {
		
	}


	public Integer getId() {
		return id;
	}


	public void setId(Integer id) {
		this.id = id;
	}


	


	public Integer getCodeRef() {
		return codeRef;
	}


	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}


	
	public Integer getExclude() {
		return exclude;
	}


	public void setExclude(Integer exclude) {
		this.exclude = exclude;
	}


	public Integer getFlag() {
		return flag;
	}


	public void setFlag(Integer flag) {
		this.flag = flag;
	}


	public Integer getDeloId() {
		return deloId;
	}


	public void setDeloId(Integer deloId) {
		this.deloId = deloId;
	}


 
	
	
	
	
		
}
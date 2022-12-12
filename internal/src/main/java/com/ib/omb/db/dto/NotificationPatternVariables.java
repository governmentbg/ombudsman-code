package com.ib.omb.db.dto;


import static javax.persistence.GenerationType.SEQUENCE;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity
@Table(name = "NOTIFICATION_PATTERNS_VARS")
public class NotificationPatternVariables implements Serializable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -4748324101296271516L;

	@SequenceGenerator(name = "NotificationPatternVariables", sequenceName = "SEQ_MAIL_PATTERNS_VARIABLES", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "NotificationPatternVariables")
	@Column(name = "VAR_ID", unique = true, nullable = false)
	private Integer id;

//	@Column(name = "PATTERN_ID")
//	private Integer patternId;
		
	@Column(name = "VAR_NAME")
	private String varName;
	
	@Column(name = "VAR_REFL")
	private String varRefl;
	
	@Column(name = "CODE_CLASSIF")
	private Integer codeClassif;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PATTERN_ID")
	private NotificationPatterns pattern;
	

	public NotificationPatterns getPattern() {
		return pattern;
	}

	public void setPattern(NotificationPatterns pattern) {
		this.pattern = pattern;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

//	public Integer getPatternId() {
//		return patternId;
//	}
//
//	public void setPatternId(Integer patternId) {
//		this.patternId = patternId;
//	}

	public String getVarName() {
		return varName;
	}

	public void setVarName(String varName) {
		this.varName = varName;
	}

	public String getVarRefl() {
		return varRefl;
	}

	public void setVarRefl(String varRefl) {
		this.varRefl = varRefl;
	}

	public Integer getCodeClassif() {
		return codeClassif;
	}

	public void setCodeClassif(Integer codeClassif) {
		this.codeClassif = codeClassif;
	}
	
	

	

	
	
	
		
}
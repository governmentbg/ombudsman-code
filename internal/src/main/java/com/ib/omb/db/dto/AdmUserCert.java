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
import com.ib.system.db.TrackableEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;

/**
 * Сертификат на потребител
 *
 * @author belev
 */
@Entity
@Table(name = "ADM_USER_CERTS")
public class AdmUserCert extends TrackableEntity implements AuditExt{

	/**  */
	private static final long serialVersionUID = 3419889707539290898L;

	@SequenceGenerator(name = "AdmUserCert", sequenceName = "SEQ_ADM_USER_CERTS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "AdmUserCert")
	@Column(name = "CERT_ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "USER_ID")
	@JournalAttr(label = "userId", defaultText = "Потребител", codeObject = OmbConstants.CODE_ZNACHENIE_JOURNAL_USER)
	private Integer userId;

	@Column(name = "EMAIL")
	@JournalAttr(label="email",defaultText = "Емейл")
	private String email;

	@Column(name = "ISSUER")
	@JournalAttr(label="issuer",defaultText = "Издател")
	private String issuer;

	@Column(name = "EXP_DATE")
	@JournalAttr(label="expDate",defaultText = "Дата на изтичане")
	private Date expDate;

	@Column(name = "CERT")	
	private byte[] cert;

	@Column(name = "ACTIVE_CERT")
	@JournalAttr(label="activeCert",defaultText = "Активен", classifID = ""+ OmbConstants.CODE_CLASSIF_DANE)
	private Integer activeCert;

	/** */
	public AdmUserCert() {
		super();
	}

	/** @return the activeCert */
	public Integer getActiveCert() {
		return this.activeCert;
	}

	/** @return the cert */
	public byte[] getCert() {
		return this.cert;
	}

	/** */
	@Override
	public Integer getCodeMainObject() {
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_USER_CERT;
	}

	/** @return the email */
	public String getEmail() {
		return this.email;
	}

	/** @return the expDate */
	public Date getExpDate() {
		return this.expDate;
	}

	/** @return the id */
	@Override
	public Integer getId() {
		return this.id;
	}

	/** @return the issuer */
	public String getIssuer() {
		return this.issuer;
	}

	/** @return the userId */
	public Integer getUserId() {
		return this.userId;
	}

	/** @param activeCert the activeCert to set */
	public void setActiveCert(Integer activeCert) {
		this.activeCert = activeCert;
	}

	/** @param cert the cert to set */
	public void setCert(byte[] cert) {
		this.cert = cert;
	}

	/** @param email the email to set */
	public void setEmail(String email) {
		this.email = email;
	}

	/** @param expDate the expDate to set */
	public void setExpDate(Date expDate) {
		this.expDate = expDate;
	}

	/** @param id the id to set */
	public void setId(Integer id) {
		this.id = id;
	}

	/** @param issuer the issuer to set */
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}

	/** @param userId the userId to set */
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	
	@Override
	public String getIdentInfo() throws DbErrorException {
		return issuer;
	}
	

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new  SystemJournal();				
		dj.setCodeObject(getCodeMainObject());
		dj.setIdObject(getId());
		dj.setIdentObject(getIdentInfo());
		dj.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_USER);
		dj.setJoinedIdObject1(userId);
		return dj;
	}
}
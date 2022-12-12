package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

// Generated 2010-5-19 17:04:58 by Hibernate Tools 3.2.2.GA


/**
 * EgovMessagesCoresp @author n.kosev
 */

@Entity
@Table(name = "EGOV_MESSAGES_CORESP")
public class EgovMessagesCoresp implements java.io.Serializable {

	private static final long serialVersionUID = -3063804870630448946L;
	
	@SequenceGenerator(name = "EgovMessagesCoresp", sequenceName = "SEQ_EGOV_MESSAGES_CORESP", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "EgovMessagesCoresp")
	@Column(name = "ID", unique = true, nullable = false)
	private Long id;
	
	@Column(name = "ID_MESSAGE")
	private Integer idMessage;
	
	@Column(name = "IME")
	private String ime;
	
	@Column(name = "EGN")
	private String egn;
	
	@Column(name = "IDCARD")
	private String idCard;
	
	@Column(name = "BULSTAT")
	private String bulstat;
	
	@Column(name = "CITY")
	private String city;
	
	@Column(name = "ADRES")
	private String adres;
	
	@Column(name = "PK")
	private String pk;
	
	@Column(name = "EMAIL")
	private String email;
	
	@Column(name = "PHONE")
	private String phone;
	
	@Column(name = "MOBILE_PHONE")
	private String mobilePhone;
	
	@Column(name = "DOP_INFO")
	private String	dopInfo;
	
	public EgovMessagesCoresp() {
		
	}
	
	

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getIdMessage() {
		return idMessage;
	}

	public void setIdMessage(Integer idMessage) {
		this.idMessage = idMessage;
	}

	public String getIme() {
		return ime;
	}

	public void setIme(String ime) {
		this.ime = ime;
	}

	public String getEgn() {
		return egn;
	}

	public void setEgn(String egn) {
		this.egn = egn;
	}

	public String getIdCard() {
		return idCard;
	}

	public void setIdCard(String idCard) {
		this.idCard = idCard;
	}

	public String getBulstat() {
		return bulstat;
	}

	public void setBulstat(String bulstat) {
		this.bulstat = bulstat;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getAdres() {
		return adres;
	}

	public void setAdres(String adres) {
		this.adres = adres;
	}

	public String getPk() {
		return pk;
	}

	public void setPk(String pk) {
		this.pk = pk;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getMobilePhone() {
		return mobilePhone;
	}

	public void setMobilePhone(String mobilePhone) {
		this.mobilePhone = mobilePhone;
	}

	public String getDopInfo() {
		return dopInfo;
	}

	public void setDopInfo(String dopInfo) {
		this.dopInfo = dopInfo;
	}

	
}

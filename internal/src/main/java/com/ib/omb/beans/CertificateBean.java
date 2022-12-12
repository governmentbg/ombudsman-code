package com.ib.omb.beans;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.omb.db.dao.AdmUserCertDAO;
import com.ib.omb.db.dto.AdmUserCert;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;

@Named
@SessionScoped
public class CertificateBean extends IndexUIbean implements Serializable {

	private static final long serialVersionUID = 468808409920968512L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CertificateBean.class);
	
	private AdmUserCertDAO certDao;
	private List<AdmUserCert> certificates;

	@PostConstruct
	void init() {
		//System.out.println("CERTIFICATES!!");
		this.certDao = new AdmUserCertDAO(getUserData());
		
		try {
			parseCertificates(this.certDao.findCertListByUserId(getUserData().getUserId(), null));
		} catch (DbErrorException e) {
			LOGGER.error("greshka", e);
			//JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
	
	/**
	 * Парсва сертификатите при зареждане на бийна
	 * @param list
	 */
	private void parseCertificates(List<Object[]> list) {
		
		this.certificates = new ArrayList<>();
		
		for(Object[] cert : list) {
			AdmUserCert admCert = new AdmUserCert();
			admCert.setId(cert[0] == null ? null : ((Number) cert[0]).intValue());
			admCert.setUserId(cert[1] == null ? null : ((Number) cert[1]).intValue());
			admCert.setEmail(cert[2] == null ? null : (String) cert[2]);
			admCert.setIssuer(cert[3] == null ? null : (String) cert[3]);
			admCert.setExpDate(cert[4] == null ? null : new Date(((Timestamp) cert[4]).getTime()));
			admCert.setActiveCert(cert[5] == null ? null : ((Number) cert[5]).intValue());
			
			this.certificates.add(admCert);
		}
	}
	
	/**
	 * Вика се при натискане на бутона "Добавяне на сертификат"
	 */
	public void addCertificate() {
		// TODO
	}
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	public List<AdmUserCert> getCertificates() {
		return certificates;
	}

	public void setCertificates(List<AdmUserCert> certificates) {
		this.certificates = certificates;
	}
	
}

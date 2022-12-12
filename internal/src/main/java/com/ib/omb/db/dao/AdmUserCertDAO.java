package com.ib.omb.db.dao;

import java.util.List;

import javax.persistence.Query;

import com.ib.omb.db.dto.AdmUserCert;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;
import com.ib.system.exceptions.DbErrorException;

/**
 * DAO for {@link AdmUserCert}
 *
 * @author belev
 */
public class AdmUserCertDAO extends AbstractDAO<AdmUserCert> {

	/** @param user */
	public AdmUserCertDAO(ActiveUser user) {
		super(AdmUserCert.class, user);
	}

	/**
	 * Ако active != null се прилага като аргумент в търсенето, съответно за активни и неактивни сертификати. Връща данни от вида:
	 * <br>
	 * [0]-CERT_ID<br>
	 * [1]-USER_ID<br>
	 * [2]-EMAIL<br>
	 * [3]-ISSUER<br>
	 * [4]-EXP_DATE<br>
	 * [5]-ACTIVE_CERT<br>
	 *
	 * @param userId
	 * @param active
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> findCertListByUserId(Integer userId, Integer active) throws DbErrorException {
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select CERT_ID, USER_ID, EMAIL, ISSUER, EXP_DATE, ACTIVE_CERT from ADM_USER_CERTS where USER_ID = ?1 ");
			if (active != null) {
				sql.append(" and ACTIVE_CERT = ?2 ");
			}

			Query query = createNativeQuery(sql.toString()).setParameter(1, userId);

			if (active != null) {
				query.setParameter(2, active);
			}

			List<Object[]> result = query.getResultList();

			return result;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на сертификати на потребител!", e);
		}
	}
}
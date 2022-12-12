package com.ib.omb.transform;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SearchUtils;

/**
 * Разни почиствания на данни след като всичко мине
 *
 * @author belev
 */
public class T4Clear {

	private static final Logger LOGGER = LoggerFactory.getLogger(T4Clear.class);

	/**
	 * @param dest
	 * @throws DbErrorException
	 */
	public void transferClear(JPA dest) throws DbErrorException {
		// проверка дали вече е пускан
		Number cnt = (Number) dest.getEntityManager().createNativeQuery("select count(*) from transfer_process where clazz = ?1") //
			.setParameter(1, getClass().getSimpleName()).getResultList().get(0);
		if (cnt.intValue() > 0) {
			LOGGER.info("   ! finished !");
			return; // значи си е свършил работата
		}
		Date startDate = new Date(); // тука всяко пускане прави всичко

		clearDeloUserMod(dest); // да се зачисти user_last_mod на преписки деловодство

		updateDublicateMail(dest); // рябва да се нарпави да няма дублиране на мейл

		createIndexes(dest);

		dest.begin();
		dest.getEntityManager().createNativeQuery( //
			"insert into transfer_process (clazz, start_time, end_time) values (?1, ?2, ?3)") //
			.setParameter(1, getClass().getSimpleName()).setParameter(2, startDate).setParameter(3, new Date()) //
			.executeUpdate();
		dest.commit();
	}

	/** */
	void clearDeloUserMod(JPA dest) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start clear DELO from omb...");

		dest.begin();
		int updateCnt = dest.getEntityManager().createNativeQuery( //
			" update delo set user_last_mod = null where delo_id >= 7000 and delo_id <= 100000 ").executeUpdate();
		dest.commit();

		LOGGER.info("  " + updateCnt);
		LOGGER.info("End clear DELO from omb.");
	}

	/**
	 * създаване на индекси
	 */
	void createIndexes(JPA dest) {
		LOGGER.info("");
		LOGGER.info("Start create indexes");

		List<String> list = new ArrayList<>();
		list.add("CREATE INDEX delo_registratura_id_rn_delo_delo_year	ON delo USING btree (registratura_id, rn_delo, delo_year)");
		list.add("CREATE INDEX doc_registratura_id_rn_doc_doc_date	ON doc USING btree (registratura_id, rn_doc, doc_date)");
		list.add("CREATE INDEX doc_referents_doc_id	ON doc_referents USING btree (doc_id)");
		list.add("CREATE INDEX doc_rn_doc_upper	ON doc USING btree (upper(rn_doc))");
		list.add("CREATE INDEX delo_rn_delo_upper	ON delo USING btree (upper(rn_delo))");
		list.add("CREATE INDEX doc_doc_vid	ON doc USING btree (doc_vid)");
		list.add("CREATE INDEX doc_doc_date	ON doc USING btree (doc_date desc)");
		list.add("CREATE INDEX delo_delo_date	ON delo USING btree (delo_date desc)");
		list.add("CREATE INDEX doc_dvij_dvij_date	ON doc_dvij USING btree (dvij_date desc)");
		list.add("CREATE INDEX doc_jalba_sast	ON doc_jalba USING btree (sast)");
		list.add("CREATE INDEX doc_dvij_doc_id	ON doc_dvij USING btree (doc_id)");
		list.add("CREATE INDEX task_doc_id	ON task USING btree (doc_id)");
		list.add("CREATE INDEX delo_doc_delo_id	ON delo_doc USING btree (delo_id)");
		list.add("CREATE INDEX delo_doc_input_doc_id	ON delo_doc USING btree (input_doc_id)");
		list.add("CREATE INDEX doc_sast_history_doc_id	ON doc_sast_history USING btree (doc_id)");
		list.add("CREATE INDEX doc_referents_code_ref	ON doc_referents USING btree (code_ref)");
		list.add("CREATE INDEX doc_processed_doc_type	ON doc USING btree (processed, doc_type)");
		list.add("CREATE INDEX task_rn_task	ON task USING btree (rn_task)");
		list.add("CREATE INDEX doc_jalba_result_doc_id	ON doc_jalba_result USING btree (doc_id)");

		for (String sql : list) {
			try {
				dest.begin();
				dest.getEntityManager().createNativeQuery(sql).executeUpdate();
				dest.commit();
				LOGGER.info("   " + sql + " -> success");

			} catch (Exception e) {
				dest.rollback();
				LOGGER.warn(e.getMessage());
			}
		}
		LOGGER.info("End create indexes");
	}

	/** */
	void updateDublicateMail(JPA dest) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start clear ADM_USERS from omb...");

		int updateCnt = 0;

		Query updateUser = dest.getEntityManager().createNativeQuery("update adm_users set email = ?1 where user_id = ?2");
		Query updateAdm = dest.getEntityManager().createNativeQuery("update adm_referents set contact_email = ?1 where code = ?2");

		StringBuilder sql = new StringBuilder();
		sql.append(" select u.user_id, u.names, u.email, u.status from adm_users u ");
		sql.append(" where u.email in (select v.email from adm_users v where v.email is not null and v.email <> '' group by v.email having count(*) > 1) ");
		sql.append(" order by u.email, u.status desc, u.user_id desc ");

		@SuppressWarnings("unchecked")
		List<Object[]> rows = dest.getEntityManager().createNativeQuery(sql.toString()).getResultList();

		String current = "-";
		dest.begin();
		for (Object[] row : rows) {

			String mail = SearchUtils.trimToNULL((String) row[2]);

			if (mail != null && !mail.equals(current)) {
				current = mail;

				updateUser.setParameter(1, mail + "-old");
				updateUser.setParameter(2, row[0]);
				updateUser.executeUpdate();

				updateAdm.setParameter(1, mail + "-old");
				updateAdm.setParameter(2, row[0]);
				updateAdm.executeUpdate();

				updateCnt++;
			}
		}
		dest.commit();

		LOGGER.info("  " + updateCnt);
		LOGGER.info("End clear ADM_USERS from omb.");
	}
}
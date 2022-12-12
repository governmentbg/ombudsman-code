package com.ib.omb.quartz;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_BOSS_POSITIONS;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DEF_PRAVA;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_JALBA_FULL_EDIT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOPUST_DOPUST;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_JALBA_EXPIRING;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_VODEST;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletContext;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.experimental.Notification;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.SystemData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.utils.DateUtils;

/**
 * Изпращане на нотификации за жалби с изтичащ срок за разглеждане
 *
 * @author belev
 */
@DisallowConcurrentExecution
public class JalbaExpiringNotifJob implements Job {

	private static final Logger LOGGER = LoggerFactory.getLogger(JalbaExpiringNotifJob.class);

//	public SystemData testData;

	/** */
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			ServletContext servletContext = (ServletContext) context.getScheduler().getContext().get("servletContext");
			if (servletContext == null) {
				LOGGER.info("********** servletcontext is null **********");
				return;
			}

			SystemData systemData = (SystemData) servletContext.getAttribute("systemData"); // this.testData; //

			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(new Date());

			gc.add(Calendar.DAY_OF_YEAR, 3); // за недопустимите 3 дни
			Date dateFrom3 = DateUtils.startDate(gc.getTime());
			Date dateTo3 = DateUtils.endDate(gc.getTime());

			gc.add(Calendar.DAY_OF_YEAR, 4); // за допустимите 7 дни (+4)
			Date dateFrom7 = DateUtils.startDate(gc.getTime());
			Date dateTo7 = DateUtils.endDate(gc.getTime());

			StringBuilder sql = new StringBuilder();
			sql.append(" select d.doc_id, d.rn_doc, d.pored_delo, d.doc_date, j.dopust, j.code_expert, j.code_zveno ");
			sql.append(" from doc_jalba j inner join doc d on d.doc_id = j.doc_id ");
			sql.append(" where j.srok is not null and j.dopust is not null and j.sast <> ?1 ");
			sql.append(" and ( ");
			sql.append(" 			(j.dopust = ?2 and j.srok >= ?3 and j.srok <= ?4) ");
			sql.append(" 		or  (j.dopust <> ?5 and j.srok >= ?6 and j.srok <= ?7) ");
			sql.append(" ) ");

			@SuppressWarnings("unchecked")
			List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
				.setParameter(1, CODE_ZNACHENIE_JALBA_SAST_COMPLETED) //
				.setParameter(2, CODE_ZNACHENIE_DOPUST_DOPUST).setParameter(3, dateFrom7).setParameter(4, dateTo7) //
				.setParameter(5, CODE_ZNACHENIE_DOPUST_DOPUST).setParameter(6, dateFrom3).setParameter(7, dateTo3) //
				.getResultList();

			Set<Integer> fullEditJalbaUsers = new HashSet<>();

			if (!rows.isEmpty()) { // само ако има подобни жалби има смисъл да се търсят хората
				StringBuilder sqlUsers = new StringBuilder();
				sqlUsers.append(" select ur.user_id from adm_user_roles ur where ur.code_classif = :codeClassif and ur.code_role = :codeRole ");
				sqlUsers.append(" union ");
				sqlUsers.append(" select ug.user_id from adm_user_group ug inner join adm_group_roles gr on gr.group_id = ug.group_id ");
				sqlUsers.append(" where gr.code_classif = :codeClassif and gr.code_role = :codeRole ");

				@SuppressWarnings("unchecked")
				List<Object> users = JPA.getUtil().getEntityManager().createNativeQuery(sqlUsers.toString()) //
					.setParameter("codeClassif", CODE_CLASSIF_DEF_PRAVA).setParameter("codeRole", CODE_ZNACHENIE_DEF_PRAVA_JALBA_FULL_EDIT) //
					.getResultList();
				for (Object uid : users) {
					fullEditJalbaUsers.add(((Number) uid).intValue());
				}
			}

			for (Object[] row : rows) {

				StringBuilder note = new StringBuilder();
				note.append("Срокът за разглеждане на жалба " + DocDAO.formRnDocDate(row[1], row[3], null));
				note.append(" изтича след ");
				if (row[4] != null) {
					int dopust = ((Number) row[4]).intValue();
					if (dopust == CODE_ZNACHENIE_DOPUST_DOPUST) {
						note.append("7");
					} else {
						note.append("3");
					}
				}
				note.append(" дни.");

				Set<Integer> adresati = new HashSet<>();
				adresati.addAll(fullEditJalbaUsers); // пълен достъп за актуализация на жалби

				if (row[5] != null) { // + експерт
					adresati.add(((Number) row[5]).intValue());
				}
				if (row[6] != null) { // + ръководителя на звеното към което е разпределена
					List<SystemClassif> children = systemData.getChildrenOnNextLevel(CODE_CLASSIF_ADMIN_STR, ((Number) row[6]).intValue(), null, CODE_DEFAULT_LANG);
					for (SystemClassif item : children) {
						Integer position = (Integer) systemData.getItemSpecific(CODE_CLASSIF_ADMIN_STR, item.getCode(), CODE_DEFAULT_LANG, null, OmbClassifAdapter.ADM_STRUCT_INDEX_POSITION);

						if (position != null && systemData.matchClassifItems(CODE_CLASSIF_BOSS_POSITIONS, position, null)) {
							adresati.add(item.getCode());
						}
					}
				}

				sendNotif(note.toString(), adresati, systemData);
			}

		} catch (Exception e) {
			LOGGER.error("Грешка при формиране на нотификации за жалби с изтичащ срок за разглеждане", e);

		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	void sendNotif(String note, Set<Integer> adresati, SystemData systemData) {
		if (adresati.isEmpty()) {
			return; // няма на кой да кажем
		}

		Notification notif = new Notification(-1, 1, CODE_ZNACHENIE_NOTIFF_JALBA_EXPIRING, CODE_ZNACHENIE_NOTIF_ROLIA_VODEST, systemData);
		notif.setComment(note);

		notif.getAdresati().addAll(adresati);
		notif.send();
	}
}

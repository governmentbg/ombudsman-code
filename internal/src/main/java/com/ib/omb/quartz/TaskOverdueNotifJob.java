package com.ib.omb.quartz;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_TASK_STATUS_ACTIVE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_OVERDUE_ASSIGN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_OVERDUE_CONTROL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_OVERDUE_IZP;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_CONTR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_IZP;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_VAZLOJITEL;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;
import static com.ib.system.utils.SearchUtils.asInteger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dto.Task;
import com.ib.omb.experimental.Notification;
import com.ib.omb.system.SystemData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.utils.DateUtils;

/**
 * Нотификации за просрочени задачи
 *
 * @author belev
 */
@DisallowConcurrentExecution
public class TaskOverdueNotifJob implements Job {

	private static final Logger LOGGER = LoggerFactory.getLogger(TaskOverdueNotifJob.class);

//	private static SystemData testData = new SystemData();
//
//	public static void main(String[] args) {
//		TaskOverdueNotifJob job = new TaskOverdueNotifJob();
//		try {
//			job.execute(null);
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	/** */
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			ServletContext servletContext = (ServletContext) context.getScheduler().getContext().get("servletContext");
			if (servletContext == null) {
				LOGGER.info("********** servletcontext is null **********");
				return;
			}

			SystemData systemData = (SystemData) servletContext.getAttribute("systemData"); // testData;

			// начло и кран на предишния ден
			Date dateFrom = DateUtils.startDate(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
			Date dateTo = DateUtils.endDate(dateFrom);

			// активните статуси
			List<SystemClassif> items = systemData.getSysClassification(CODE_CLASSIF_TASK_STATUS_ACTIVE, dateFrom, CODE_DEFAULT_LANG);
			List<Integer> activeStatusList = new ArrayList<>(items.size());
			for (SystemClassif item : items) {
				activeStatusList.add(item.getCode());
			}

			StringBuilder sql = new StringBuilder();
			sql.append(" select t.TASK_ID, t.CODE_ASSIGN, t.CODE_CONTROL, r.CODE_REF ");
			sql.append(" , t.SROK_DATE, t.RN_TASK, t.TASK_TYPE, t.TASK_INFO ");
			sql.append(" from TASK t ");
			sql.append(" inner join TASK_REFERENTS r on r.TASK_ID = t.TASK_ID ");
			sql.append(" where t.STATUS in (?1) and t.SROK_DATE >= ?2 and t.SROK_DATE <= ?3 ");
			sql.append(" order by 1 ");

			@SuppressWarnings("unchecked")
			List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
				.setParameter(1, activeStatusList).setParameter(2, dateFrom).setParameter(3, dateTo) //
				.getResultList();

			Task task = null; // има размножаване заради изпълнителите
			for (Object[] row : rows) {
				Integer taskId = ((Number) row[0]).intValue();

				if (task == null || !task.getId().equals(taskId)) {
					task = new Task();

					task.setId(taskId);
					task.setSrokDate((Date) row[4]);
					task.setRnTask((String) row[5]);
					task.setTaskType(asInteger(row[6]));
					task.setTaskInfo((String) row[7]);

					if (row[1] != null) {
						sendNotif(task, ((Number) row[1]).intValue(), CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_OVERDUE_ASSIGN //
							, CODE_ZNACHENIE_NOTIF_ROLIA_VAZLOJITEL, systemData);
					}
					if (row[2] != null) {
						sendNotif(task, ((Number) row[2]).intValue(), CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_OVERDUE_CONTROL //
							, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, systemData);
					}
				}

				if (row[3] != null) {
					sendNotif(task, ((Number) row[3]).intValue(), CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_OVERDUE_IZP //
						, CODE_ZNACHENIE_NOTIF_ROLIA_IZP, systemData);
				}
			}

		} catch (Exception e) {
			JPA.getUtil().rollback();
			LOGGER.error("Грешка при формиране на Нотификации за просрочени задачи.", e);

		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	void sendNotif(Task task, Integer adresat, Integer notifCode, Integer roleCode, SystemData systemData) {
		Notification notif = new Notification(-1, null, notifCode, roleCode, systemData);

		notif.setTask(task);
		notif.getAdresati().add(adresat);

		notif.send();
	}
}
package com.ib.omb.quartz;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_MONTH;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_YEAR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_DA;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_NE;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.persistence.Query;
import javax.servlet.ServletContext;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.TaskDAO;
import com.ib.omb.db.dao.TaskScheduleDAO;
import com.ib.omb.db.dto.Task;
import com.ib.omb.db.dto.TaskSchedule;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;

/**
 * Регистриране на периодични задачи
 *
 * @author belev
 */
@DisallowConcurrentExecution
public class TaskScheduleJob implements Job {

	private static final Logger LOGGER = LoggerFactory.getLogger(TaskScheduleJob.class);

//	private static SystemData testData = new SystemData();
//
//	public static void main(String[] args) {
//		TaskScheduleJob job = new TaskScheduleJob();
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

			Date today = DateUtils.startDate(new Date());

			int i = 0;
			List<TaskSchedule> list;
			do {
				// по време на едно пускане се обработват всички редове с настъпила дата на изпълнение
				// след като се обработят редовете се определят новите дати на изпълнение
				// ако процеса дълго време не е работил са се натрупали доста неща и така ще се оправят
				list = runOnce(systemData, today);

				i++; // за всеки случай да не се зацикли

			} while (list.size() > 0 && i <= 10); // така ще се пуска докато има данни, но макс 10 пъти

		} catch (Exception e) {
			JPA.getUtil().rollback();
			LOGGER.error("Грешка при регистриране на периодични задачи", e);

		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/**
	 * Едно изпълнение на процеса
	 *
	 * @param systemData
	 * @param today
	 * @return
	 * @throws DbErrorException
	 */
	List<TaskSchedule> runOnce(SystemData systemData, Date today) throws DbErrorException {
		Query query = JPA.getUtil().getEntityManager().createQuery( //
			"select ts from TaskSchedule ts where ts.valid = ?1 and ts.nextRegDate <= ?2") //
			.setParameter(1, CODE_ZNACHENIE_DA).setParameter(2, today);

		@SuppressWarnings("unchecked")
		List<TaskSchedule> list = query.getResultList();
		if (list.isEmpty()) {
			LOGGER.debug("Няма периодични задачи за регистриране.");
			return list;
		}

		for (TaskSchedule schedule : list) {
			schedule.setDbRegPeriod(schedule.getRegPeriod());
			schedule.setDbValid(schedule.getValid());

			schedule.setDbRegInterval(schedule.getRegInterval());
			schedule.setDbRegDay(schedule.getRegDay());
			schedule.setDbRegMonth(schedule.getRegMonth());

			processSchedule(schedule, today, systemData);
		}
		return list;
	}

	/**
	 * Обработка на реда от дефиницията на периодична задача, като регистрира една или няколко задачи.
	 *
	 * @param schedule
	 * @param today
	 * @param systemData
	 * @throws DbErrorException
	 */
	private void processSchedule(TaskSchedule schedule, Date today, SystemData systemData) throws DbErrorException {

		List<Integer> codeExecs = new ArrayList<>(); // на тези ще се правят задачи
		boolean leastLoaded = false; // ако се вдигне флага и има повече от един човек се дава задача на този, който има най малко
										// задачи

		if (schedule.getCodeExec() != null) { // за конкретен човек
			codeExecs.add(schedule.getCodeExec());

			// когато е до конкретен човек тези не може да ги има
			schedule.setZveno(null);
			schedule.setEmplPosition(null);

		} else if (schedule.getZveno() != null) {

			List<SystemClassif> empls = systemData.getChildrenOnNextLevel(CODE_CLASSIF_ADMIN_STR, schedule.getZveno(), today, CODE_DEFAULT_LANG);

			for (SystemClassif empl : empls) {
				Integer refType = (Integer) systemData.getItemSpecific(CODE_CLASSIF_ADMIN_STR, empl.getCode(), CODE_DEFAULT_LANG, today, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (refType == null || refType.intValue() != OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL) {
					continue; // звената се пропускат
				}

				if (schedule.getEmplPosition() != null) { // само за човек на тази длъжност
					leastLoaded = true;
					Integer position = (Integer) systemData.getItemSpecific(CODE_CLASSIF_ADMIN_STR, empl.getCode(), CODE_DEFAULT_LANG, today, OmbClassifAdapter.ADM_STRUCT_INDEX_POSITION);

					if (schedule.getEmplPosition().equals(position)) {
						codeExecs.add(empl.getCode()); // само до хората в звеното на тази длъжност
					}
				} else {
					codeExecs.add(empl.getCode()); // до всички в звеното
				}
			}
		}

		if (leastLoaded && codeExecs.size() > 1) {
			// При въведено звено и длъжност, регистрира една задача, като избира служител, назначен на посочената длъжност в
			// посоченото звено, който има най-малък брой активни задачи.

			List<SystemClassif> items = systemData.getSysClassification(OmbConstants.CODE_CLASSIF_TASK_STATUS_ACTIVE, today, CODE_DEFAULT_LANG);
			List<Integer> statusList = new ArrayList<>(items.size()); // активните статуси на задачи
			for (SystemClassif item : items) {
				statusList.add(item.getCode());
			}
			StringBuilder sql = new StringBuilder();
			sql.append(" select r.CODE_REF, count (*) cnt ");
			sql.append(" from TASK t ");
			sql.append(" inner join TASK_REFERENTS r on r.TASK_ID = t.TASK_ID ");
			sql.append(" where t.STATUS in (:statusList) and r.CODE_REF in (:codeExecs) ");
			sql.append(" group by r.CODE_REF ");
			sql.append(" order by 2 ");

			@SuppressWarnings("unchecked")
			List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
				.setParameter("statusList", statusList).setParameter("codeExecs", codeExecs) //
				.getResultList();

			if (rows.isEmpty()) { // никой няма задача и тогава е на случаен принцип
				codeExecs.subList(1, codeExecs.size()).clear(); // остава само първия в списъка

			} else if (rows.size() == codeExecs.size()) { // всички имат задачи и тогава е лесно, защото първия в списъка от БД е
															// точния човек
				codeExecs.clear();
				codeExecs.add(((Number) rows.get(0)[0]).intValue());

			} else { // от намерените има някой без задачи и трябва него да избера
				List<Integer> dbCodeExecs = new ArrayList<>(rows.size());
				for (Object[] row : rows) {
					dbCodeExecs.add(((Number) row[0]).intValue());
				}
				codeExecs.removeAll(dbCodeExecs);
				if (codeExecs.size() > 1) {
					codeExecs.subList(1, codeExecs.size()).clear(); // остава само първия в списъка
				}
			}
		}

		UserData userData = new UserData(schedule.getUserReg(), "", "");

		TaskDAO taskDao = new TaskDAO(userData);
		TaskScheduleDAO scheduleDao = new TaskScheduleDAO(userData);

		// ---------------------- транзакция начало ---------------------- //
		JPA.getUtil().begin();

		for (Integer codeExec : codeExecs) { // за всеки от тези отделна задача се прави
			Task task = new Task();

			// регистратурата е взимам от възложителя
			Integer registratura = (Integer) systemData.getItemSpecific(CODE_CLASSIF_ADMIN_STR, schedule.getCodeAssign(), CODE_DEFAULT_LANG, today, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);

			task.setRegistraturaId(registratura);
			task.setDocId(schedule.getDocId());

			task.setStatus(CODE_ZNACHENIE_TASK_STATUS_NEIZP);
			task.setStatusDate(today);
			task.setStatusComments("Задачата е регистрирана въз основа на дефиниция на периодичната задача с № на дефиниция " + schedule.getId());

			task.setTaskType(schedule.getTaskType());
			task.setTaskInfo(schedule.getTaskInfo());
			task.setDocRequired(schedule.getDocRequired() == null ? CODE_ZNACHENIE_NE : schedule.getDocRequired());

			task.setCodeAssign(schedule.getCodeAssign());
			task.setCodeControl(schedule.getCodeControl());
			task.setCodeExecs(Arrays.asList(codeExec));

			if (Objects.equals(schedule.getWorkDaysOnly(), CODE_ZNACHENIE_DA) //
				&& (schedule.getRegPeriod().equals(CODE_ZNACHENIE_TASK_PERIOD_MONTH) || schedule.getRegPeriod().equals(CODE_ZNACHENIE_TASK_PERIOD_YEAR))) {
				// първия работен ден след schedule.getNextRegDate()

				LocalDate temp = DateUtils.startDate(schedule.getNextRegDate()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
				if (temp.getDayOfWeek().getValue() > 5) { // паднало се е събота и неделя
					temp = temp.plusDays(8L - temp.getDayOfWeek().getValue());

					task.setAssignDate(Date.from(temp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()));

				} else {
					task.setAssignDate(schedule.getNextRegDate());
				}

			} else {
				task.setAssignDate(schedule.getNextRegDate());
			}

			if (schedule.getTaskSrok() != null) { // трябва да се изчски и срока като се броят дните
				task.setSrokDate(new Date(task.getAssignDate().getTime() + schedule.getTaskSrok().intValue() * 24 * 60 * 60 * 1000));
			}

			taskDao.save(task, null, systemData);
		}

		schedule.setLastRegDate(schedule.getNextRegDate());

		Date nextRegDate = scheduleDao.findNextRegDate(schedule, systemData);
		if (nextRegDate == null) {
			schedule.setValid(CODE_ZNACHENIE_NE);
		} else {
			schedule.setNextRegDate(nextRegDate);
		}
		scheduleDao.save(schedule, systemData);

		JPA.getUtil().commit();
		// ---------------------- транзакция край ----------------------- //
	}
}
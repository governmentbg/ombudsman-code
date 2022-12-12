package com.ib.omb.db.dao;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_DAY;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_MONTH;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_WEEK;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_YEAR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_ASSIGN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_CONTROL;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ib.omb.db.dto.TaskSchedule;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link TaskSchedule}
 *
 * @author belev
 */
public class TaskScheduleDAO extends AbstractDAO<TaskSchedule> {

	/**
	 * @param user
	 */
	public TaskScheduleDAO(ActiveUser user) {
		super(TaskSchedule.class, user);
	}

	/**
	 * Предостава селект за търсене на дефиниции на периочни задачи<br>
	 * [0]-ID<br>
	 * [1]-REG_PERIOD<br>
	 * [2]-VALID_FROM<br>
	 * [3]-VALID_TO<br>
	 * [4]-VALID<br>
	 * [5]-NEXT_REG_DATE<br>
	 * [6]-TASK_TYPE<br>
	 * [7]-TASK_INFO<br>
	 * [8]-CODE_ASSIGN<br>
	 * [9]-CODE_CONTROL<br>
	 * [10]-CODE_EXEC<br>
	 * [11]-ZVENO<br>
	 * [12]-EMPL_POSITION<br>
	 *
	 * @param regPeriod
	 * @param valid
	 * @param validFrom
	 * @param validTo
	 * @param id
	 * @param nextRegFrom
	 * @param nextRegTo
	 * @param taskType
	 * @param taskInfo
	 * @param codeRef
	 * @param codeRole
	 * @param zveno
	 * @param position
	 * @param rnDoc
	 * @param rnDocEQ
	 * @return
	 */
	public SelectMetadata createSelectScheduleList(Integer regPeriod, Integer valid, Date validFrom, Date validTo, Integer id, Date nextRegFrom, Date nextRegTo, Integer taskType, String taskInfo,
		Integer codeRef, Integer codeRole, Integer zveno, Integer position, String rnDoc, boolean rnDocEQ) {

		Map<String, Object> params = new HashMap<>();

		String select = " select s.ID a0, s.REG_PERIOD a1, s.VALID_FROM a2, s.VALID_TO a3, s.VALID a4, s.NEXT_REG_DATE a5 "
			+ " , s.TASK_TYPE a6, s.TASK_INFO a7, s.CODE_ASSIGN a8, s.CODE_CONTROL a9, s.CODE_EXEC a10, s.ZVENO a11, s.EMPL_POSITION a12 ";
		String from = " from TASK_SCHEDULE s ";
		StringBuilder where = new StringBuilder(" where 1=1 ");

		if (regPeriod != null) {
			where.append(" and s.REG_PERIOD = :regPeriod ");
			params.put("regPeriod", regPeriod);
		}

		if (valid != null) {
			where.append(" and s.VALID = :valid ");
			params.put("valid", valid);
		}
		if (validFrom != null) {
			where.append(" and s.VALID_FROM >= :validFrom ");
			params.put("validFrom", DateUtils.startDate(validFrom));
		}
		if (validTo != null) {
			where.append(" and s.VALID_FROM <= :validTo ");
			params.put("validTo", DateUtils.endDate(validTo));
		}

		if (id != null) {
			where.append(" and s.ID = :id ");
			params.put("id", id);
		}

		if (nextRegFrom != null) {
			where.append(" and s.NEXT_REG_DATE >= :nextRegFrom ");
			params.put("nextRegFrom", DateUtils.startDate(nextRegFrom));
		}
		if (nextRegTo != null) {
			where.append(" and s.NEXT_REG_DATE <= :nextRegTo ");
			params.put("nextRegTo", DateUtils.endDate(nextRegTo));
		}

		if (taskType != null) {
			where.append(" and s.TASK_TYPE = :taskType ");
			params.put("taskType", taskType);
		}
		taskInfo = SearchUtils.trimToNULL_Upper(taskInfo);
		if (taskInfo != null) {
			where.append(" and upper(s.TASK_INFO) like :taskInfo ");
			params.put("taskInfo", "%" + taskInfo + "%");
		}

		if (codeRef != null) {
			if (codeRole == null) { // рябва и за трите да влезне
				where.append(" and (s.CODE_ASSIGN = :codeRef or s.CODE_CONTROL = :codeRef or s.CODE_EXEC = :codeRef) ");

			} else if (codeRole.equals(CODE_ZNACHENIE_TASK_REF_ROLE_ASSIGN)) {
				where.append(" and s.CODE_ASSIGN = :codeRef ");

			} else if (codeRole.equals(CODE_ZNACHENIE_TASK_REF_ROLE_CONTROL)) {
				where.append(" and s.CODE_CONTROL = :codeRef ");
			} else {
				where.append(" and s.CODE_EXEC = :codeRef ");
			}
			params.put("codeRef", codeRef);
		}

		if (zveno != null) {
			where.append(" and s.ZVENO = :zveno ");
			params.put("zveno", zveno);
		}
		if (position != null) {
			where.append(" and s.EMPL_POSITION = :position ");
			params.put("position", position);
		}

		String t = SearchUtils.trimToNULL_Upper(rnDoc);
		if (t != null) {
			if (rnDocEQ) { // пълно съвпадение case insensitive
				from += " inner join DOC d on d.DOC_ID = s.DOC_ID and upper(d.RN_DOC) = :rnDoc ";
				params.put("rnDoc", t);

			} else {
				from += " inner join DOC d on d.DOC_ID = s.DOC_ID and upper(d.RN_DOC) like :rnDoc ";
				params.put("rnDoc", "%" + t + "%");
			}
		}

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSql(select + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/** */
	@Override
	public TaskSchedule findById(Object id) throws DbErrorException {
		TaskSchedule schedule = super.findById(id);
		if (schedule == null) {
			return schedule;
		}

		schedule.setDbRegPeriod(schedule.getRegPeriod());
		schedule.setDbValid(schedule.getValid());

		schedule.setDbRegInterval(schedule.getRegInterval());
		schedule.setDbRegDay(schedule.getRegDay());
		schedule.setDbRegMonth(schedule.getRegMonth());

		return schedule;
	}

	/**
	 * Определя следващата дата на изпълнение
	 *
	 * @param schedule
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	public Date findNextRegDate(TaskSchedule schedule, BaseSystemData systemData) throws DbErrorException {
		Date temp;
		if (schedule.getLastRegDate() != null) {
			temp = DateUtils.startDate(schedule.getLastRegDate());
		} else {
			temp = DateUtils.startDate(schedule.getValidFrom());
		}

		// следващата дата е в зависимост от тази и трябва да е след нея
		LocalDate startDate = temp.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

		LocalDate nextDate;
		switch (schedule.getRegPeriod()) {
		case CODE_ZNACHENIE_TASK_PERIOD_DAY:

			if (schedule.getLastRegDate() == null) { // ако е дневна и няма нито едно регистриране
				nextDate = startDate; // започва винаги от първия ден
			} else {
//				nextDate = startDate.plusDays(schedule.getRegInterval());
				nextDate = startDate.plusDays(1); // всеки ден
			}

			if (Objects.equals(schedule.getWorkDaysOnly(), SysConstants.CODE_ZNACHENIE_DA)) {
				// само за ежедневните се прескачат

				Set<Long> praznici = ((SystemData) systemData).getPraznici();
				while (true) { // тука няма да зацикли, защото все ще има ден, който не е събота и неделя и не е празник
					if (nextDate.getDayOfWeek().getValue() > 5) { // паднало се е събота и неделя
						nextDate = nextDate.plusDays(1);

					} else if (praznici != null // или празничен ден
						&& praznici.contains(nextDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())) {
						nextDate = nextDate.plusDays(1);

					} else {
						break;
					}
				}
			}
			break;

		case CODE_ZNACHENIE_TASK_PERIOD_WEEK:

			if (schedule.getLastRegDate() == null) { // ако е седмична и няма нито едно регистриране
				int diff = schedule.getRegDay().intValue() - startDate.getDayOfWeek().getValue();
				if (diff == 0) {
					nextDate = startDate; // от първия ден
				} else {
					nextDate = startDate.plusDays(diff > 0 ? diff : 7 - Math.abs(diff)); // от следващия ден който е избран
				}
			} else {
				nextDate = startDate.plusWeeks(schedule.getRegInterval());

				GregorianCalendar gc = new GregorianCalendar();
				gc.setTime(schedule.getLastRegDate());
				int lastDOW = gc.get(Calendar.DAY_OF_WEEK) - 1;

				int diff = schedule.getRegDay() - lastDOW;
				if (diff != 0) {
					nextDate = nextDate.plusDays(diff);
				}
			}
			break;

		case CODE_ZNACHENIE_TASK_PERIOD_MONTH:
			if (schedule.getLastRegDate() == null) { // ако е месечна и няма нито едно регистриране
				int diff = schedule.getRegDay().intValue() - startDate.getDayOfMonth();
				if (diff == 0) {
					nextDate = startDate; // от първия ден

				} else { // от следващия ден който е избран

					nextDate = startDate.plusDays(diff > 0 ? diff : startDate.getMonth().maxLength() - Math.abs(diff));
				}
			} else {
				nextDate = startDate.plusMonths(schedule.getRegInterval());

				GregorianCalendar gc = new GregorianCalendar();
				gc.setTime(schedule.getLastRegDate());
				int lastDOM = gc.get(Calendar.DAY_OF_MONTH);

				int diff = schedule.getRegDay() - lastDOM;
				if (diff != 0) {
					nextDate = nextDate.plusDays(diff);
				}
			}
			break;

		case CODE_ZNACHENIE_TASK_PERIOD_YEAR:
			if (schedule.getLastRegDate() == null) { // ако е годишна и няма нито едно регистриране
				int diffDay = schedule.getRegDay().intValue() - startDate.getDayOfMonth();
				int diffMonth = schedule.getRegMonth().intValue() - startDate.getMonthValue();

				if (diffDay == 0 && diffMonth == 0) {
					nextDate = startDate; // от първия ден

				} else { // от следващия ден който е избран
					int year = startDate.getYear();
					if (diffMonth < 0 || diffMonth == 0 && diffDay < 0) {
						year += 1;
					}
					nextDate = LocalDate.of(year, schedule.getRegMonth(), schedule.getRegDay());
				}
			} else {
				nextDate = startDate.plusYears(schedule.getRegInterval());

				GregorianCalendar gc = new GregorianCalendar();
				gc.setTime(schedule.getLastRegDate());

				int lastMonth = gc.get(Calendar.MONTH) + 1;
				int lastDOM = gc.get(Calendar.DAY_OF_MONTH);

				int diffMonth = schedule.getRegMonth() - lastMonth;
				if (diffMonth != 0) {
					nextDate = nextDate.plusMonths(diffMonth);
				}

				int diffDay = schedule.getRegDay() - lastDOM;
				if (diffDay != 0) {
					nextDate = nextDate.plusDays(diffDay);
				}
			}
			break;

		default:
			throw new DbErrorException("Невалиден период: " + schedule.getRegPeriod());
		}

		return Date.from(nextDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * В зависимост от дефиницията на периодичната задача дава N на брой дати на следващи итерации, но само ако не излезе извън
	 * обхвата на периода
	 *
	 * @param schedule
	 * @param n
	 * @param inPeriod
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	public List<Date> listNextNRegistration(TaskSchedule schedule, int n, boolean inPeriod, BaseSystemData systemData) throws DbErrorException {
		List<Date> dates = new ArrayList<>();

		TaskSchedule copy = new TaskSchedule(); // за да не мажа в оригиналното

		copy.setValidFrom(schedule.getValidFrom());
		copy.setValidTo(schedule.getValidTo());

		copy.setLastRegDate(schedule.getLastRegDate());
		copy.setNextRegDate(schedule.getNextRegDate());
		copy.setWorkDaysOnly(schedule.getWorkDaysOnly());

		copy.setRegPeriod(schedule.getRegPeriod());
		copy.setRegInterval(schedule.getRegInterval());
		copy.setRegDay(schedule.getRegDay());
		copy.setRegMonth(schedule.getRegMonth());

		for (int i = 0; i < n; i++) {
			copy.setNextRegDate(findNextRegDate(copy, systemData));
			dates.add(copy.getNextRegDate());

			if (copy.getLastRegDate() == null) { // за първи път и трябва още веднъж
				copy.setLastRegDate(copy.getNextRegDate()); // първо изпълнение

				copy.setNextRegDate(findNextRegDate(copy, systemData)); // следващото изпълнение
				dates.add(copy.getNextRegDate());
			}
			copy.setLastRegDate(copy.getNextRegDate()); // поредното изпълнение
		}

		// само ако се иска и то за месечни и годишни !!!
		boolean workDayOnly = Objects.equals(copy.getWorkDaysOnly(), SysConstants.CODE_ZNACHENIE_DA) //
			&& (copy.getRegPeriod().equals(CODE_ZNACHENIE_TASK_PERIOD_MONTH) || copy.getRegPeriod().equals(CODE_ZNACHENIE_TASK_PERIOD_YEAR));

		if (workDayOnly || inPeriod) { // трябва да се провери дали така измислените дати са точните

			int i = 0;
			while (i < dates.size()) {
				Date date = DateUtils.startDate(dates.get(i));

				if (workDayOnly) { // трябва да се провери дали избраната дата е почивен ден

					LocalDate temp = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
					if (temp.getDayOfWeek().getValue() > 5) { // паднало се е събота и неделя
						temp = temp.plusDays(8L - temp.getDayOfWeek().getValue());

						date = Date.from(temp.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
						dates.set(i, date); // подменям е
					}
				}

				if (date.getTime() > copy.getValidTo().getTime()) {
					dates.subList(i, dates.size()).clear(); // махам всички до края и сме готови
					break;
				}
				i++;
			}
		}
		if (dates.size() > n) {
			dates.subList(n, dates.size()).clear(); // махам всички до края и сме готови
		}
		return dates;
	}

	/**
	 * @param entity
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	public TaskSchedule save(TaskSchedule entity, BaseSystemData systemData) throws DbErrorException {

		boolean periodChanged = !Objects.equals(entity.getRegPeriod(), entity.getDbRegPeriod());
		if (periodChanged) { // за да се изчисли правилно винаги се нулира при смяна на периода
			entity.setLastRegDate(null);
		}

		boolean checkNextDate = entity.getId() == null //
			|| periodChanged //
			|| !Objects.equals(entity.getValid(), entity.getDbValid()) //
			|| !Objects.equals(entity.getRegInterval(), entity.getDbRegInterval()) //
			|| !Objects.equals(entity.getRegDay(), entity.getDbRegDay()) //
			|| !Objects.equals(entity.getRegMonth(), entity.getDbRegMonth());

		if (checkNextDate) {
			Date nextDate = findNextRegDate(entity, systemData);
			entity.setNextRegDate(nextDate);
		}

		if (entity.getNextRegDate().getTime() > entity.getValidTo().getTime()) {
			// това се прави защото вече излиза извън крайната дата на периода
			entity.setValid(SysConstants.CODE_ZNACHENIE_NE);
		}

		TaskSchedule saved = super.save(entity);

		saved.setDbRegPeriod(entity.getRegPeriod());
		saved.setDbValid(entity.getValid());

		saved.setDbRegInterval(entity.getRegInterval());
		saved.setDbRegDay(entity.getRegDay());
		saved.setDbRegMonth(entity.getRegMonth());

		return saved;
	}
}
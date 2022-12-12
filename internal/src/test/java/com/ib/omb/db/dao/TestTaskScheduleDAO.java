package com.ib.omb.db.dao;

import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.TaskScheduleDAO;
import com.ib.omb.db.dto.TaskSchedule;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;

/**
 * Test class for {@link TaskScheduleDAO}
 *
 * @author belev
 */
public class TestTaskScheduleDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestTaskScheduleDAO.class);

	private static TaskScheduleDAO	dao;
	private static SystemData		sd;

	/** @throws java.lang.Exception */
	@BeforeClass
	public static void setUp() throws Exception {
		dao = new TaskScheduleDAO(ActiveUser.DEFAULT);
	}

	private TaskSchedule schedule;

	/**  */
	@Test
	public void testListNextNRegistration() {
		this.schedule = new TaskSchedule();

		this.schedule.setTaskType(1);
		this.schedule.setRegPeriod(OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_MONTH);
		this.schedule.setRegInterval(1);
		this.schedule.setRegDay(10);

		this.schedule.setValid(1);
		this.schedule.setValidFrom(DateUtils.startDate(DateUtils.parse("17.08.2020")));
		this.schedule.setValidTo(DateUtils.startDate(DateUtils.parse("10.03.2021")));

		this.schedule.setWorkDaysOnly(1);

		try {
			List<Date> dates = dao.listNextNRegistration(this.schedule, 10, true, sd);
			for (Date date : dates) {
				LOGGER.info("{}", date);
			}
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/** Test method for {@link TaskScheduleDAO#save(TaskSchedule)}. */
//	@Test
	public void testSaveTaskScheduleDay() {
		this.schedule = new TaskSchedule();

		this.schedule.setTaskType(1);
		this.schedule.setRegPeriod(OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_DAY);
		this.schedule.setRegInterval(3);

		this.schedule.setValid(1);
		this.schedule.setValidFrom(DateUtils.startDate(DateUtils.parse("12.08.2020")));
		this.schedule.setValidTo(DateUtils.startDate(DateUtils.parse("31.08.2020")));

		this.schedule.setWorkDaysOnly(1);

		this.schedule.setCodeAssign(1);
		this.schedule.setCodeExec(1);

		LOGGER.info("1");
		try {
			JPA.getUtil().runInTransaction(() -> this.schedule = dao.save(this.schedule, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("2");
		try {
			JPA.getUtil().runWithClose(() -> this.schedule = dao.findById(this.schedule.getId()));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("3");
		try { // все едно процеса минава
			this.schedule.setLastRegDate(this.schedule.getNextRegDate());

			Date nextDate = dao.findNextRegDate(this.schedule, sd);
			this.schedule.setNextRegDate(nextDate);

			JPA.getUtil().runInTransaction(() -> this.schedule = dao.save(this.schedule, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("4");
		try { // промяна на дните
			this.schedule.setRegInterval(4);

			JPA.getUtil().runInTransaction(() -> this.schedule = dao.save(this.schedule, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("5");
		try {
			JPA.getUtil().runInTransaction(() -> dao.delete(this.schedule));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

//	/** */
//	@Test
//	public void testProcess() {
//		TaskScheduleJob job = new TaskScheduleJob(new SystemData());
//		try {
//			job.execute(null);
//		} catch (JobExecutionException e) {
//			e.printStackTrace();
//		}
//	}

	/** Test method for {@link TaskScheduleDAO#save(TaskSchedule)}. */
//	@Test
	public void testSaveTaskScheduleMonth() {
		this.schedule = new TaskSchedule();

		this.schedule.setTaskType(1);
		this.schedule.setRegPeriod(OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_MONTH);
		this.schedule.setRegInterval(1); // всяки месец
		this.schedule.setRegDay(10); // десето число

		this.schedule.setValid(1);
		this.schedule.setValidFrom(DateUtils.startDate(DateUtils.parse("12.08.2020")));
		this.schedule.setValidTo(DateUtils.startDate(DateUtils.parse("31.08.2021")));

		this.schedule.setWorkDaysOnly(2);

		this.schedule.setCodeAssign(1);
		this.schedule.setCodeExec(1);

		LOGGER.info("1");
		try {
			JPA.getUtil().runInTransaction(() -> this.schedule = dao.save(this.schedule, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("2");
		try {
			JPA.getUtil().runWithClose(() -> this.schedule = dao.findById(this.schedule.getId()));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("3");
		try { // все едно процеса минава
			this.schedule.setLastRegDate(this.schedule.getNextRegDate());

			Date nextDate = dao.findNextRegDate(this.schedule, sd);
			this.schedule.setNextRegDate(nextDate);

			JPA.getUtil().runInTransaction(() -> this.schedule = dao.save(this.schedule, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("4");
		try { // промяна на дните
			this.schedule.setRegInterval(2); // през месец
			this.schedule.setRegDay(15); // на 15

			JPA.getUtil().runInTransaction(() -> this.schedule = dao.save(this.schedule, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("5");
		try {
			JPA.getUtil().runInTransaction(() -> dao.delete(this.schedule));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/** Test method for {@link TaskScheduleDAO#save(TaskSchedule)}. */
//	@Test
	public void testSaveTaskScheduleWeek() {
		this.schedule = new TaskSchedule();

		this.schedule.setTaskType(1);
		this.schedule.setRegPeriod(OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_WEEK);
		this.schedule.setRegInterval(1); // всяка седмица
		this.schedule.setRegDay(3); // сряда

		this.schedule.setValid(1);
		this.schedule.setValidFrom(DateUtils.startDate(DateUtils.parse("13.08.2020")));
		this.schedule.setValidTo(DateUtils.startDate(DateUtils.parse("31.08.2020")));

		this.schedule.setWorkDaysOnly(2);

		this.schedule.setCodeAssign(1);
		this.schedule.setCodeExec(1);

		LOGGER.info("1");
		try {
			JPA.getUtil().runInTransaction(() -> this.schedule = dao.save(this.schedule, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("2");
		try {
			JPA.getUtil().runWithClose(() -> this.schedule = dao.findById(this.schedule.getId()));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("3");
		try { // все едно процеса минава
			this.schedule.setLastRegDate(this.schedule.getNextRegDate());

			Date nextDate = dao.findNextRegDate(this.schedule, sd);
			this.schedule.setNextRegDate(nextDate);

			JPA.getUtil().runInTransaction(() -> this.schedule = dao.save(this.schedule, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("4");
		try { // промяна на дните
			this.schedule.setRegInterval(2); // през седмица
			this.schedule.setRegDay(5); // минава петък

			JPA.getUtil().runInTransaction(() -> this.schedule = dao.save(this.schedule, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("5");
		try {
			JPA.getUtil().runInTransaction(() -> dao.delete(this.schedule));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/** Test method for {@link TaskScheduleDAO#save(TaskSchedule)}. */
//	@Test
	public void testSaveTaskScheduleYear() {
		this.schedule = new TaskSchedule();

		this.schedule.setTaskType(1);
		this.schedule.setRegPeriod(OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_YEAR);
		this.schedule.setRegInterval(1); // всяки месец
		this.schedule.setRegDay(10); // десето число
		this.schedule.setRegMonth(6); // юни

		this.schedule.setValid(1);
		this.schedule.setValidFrom(DateUtils.startDate(DateUtils.parse("12.08.2020")));
		this.schedule.setValidTo(DateUtils.startDate(DateUtils.parse("31.08.2030")));

		this.schedule.setWorkDaysOnly(2);

		this.schedule.setCodeAssign(1);
		this.schedule.setCodeExec(1);

		LOGGER.info("1");
		try {
			JPA.getUtil().runInTransaction(() -> this.schedule = dao.save(this.schedule, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("2");
		try {
			JPA.getUtil().runWithClose(() -> this.schedule = dao.findById(this.schedule.getId()));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("3");
		try { // все едно процеса минава
			this.schedule.setLastRegDate(this.schedule.getNextRegDate());

			Date nextDate = dao.findNextRegDate(this.schedule, sd);
			this.schedule.setNextRegDate(nextDate);

			JPA.getUtil().runInTransaction(() -> this.schedule = dao.save(this.schedule, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("4");
		try { // промяна на дните
			this.schedule.setRegDay(15); // на 15
			this.schedule.setRegMonth(4); // април

			JPA.getUtil().runInTransaction(() -> this.schedule = dao.save(this.schedule, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		LOGGER.info("5");
		try {
			JPA.getUtil().runInTransaction(() -> dao.delete(this.schedule));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}
}
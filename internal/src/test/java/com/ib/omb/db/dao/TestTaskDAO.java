package com.ib.omb.db.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.TaskDAO;
import com.ib.omb.db.dto.Task;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;

/**
 * Test class for {@link TaskDAO}
 *
 * @author belev
 */
public class TestTaskDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestTaskDAO.class);

	private static TaskDAO dao;

	private static SystemData sd;

	/***/
	@BeforeClass
	public static void setUp() {
		dao = new TaskDAO(new UserData(-1, "", ""));
		sd = new SystemData();
	}

	private Task task;

	/**  */
	@Test
	public void testFindTaskEditList() {
		try {
			List<Object[]> edits = dao.findTaskEditList(678, sd);

			for (Object[] row : edits) {
				LOGGER.info(Arrays.toString(row));
			}

		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/**  */
	@Test
	public void testFindTaskStatesList() {
		try {
			List<Object[]> states = dao.findTaskStatesList(601);

			for (Object[] row : states) {
				LOGGER.info(Arrays.toString(row));
			}

		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/**  */
//	@Test
	public void testFullCycle() {

		this.task = new Task();

		this.task.setRegistraturaId(1);
		this.task.setTaskType(1);
		this.task.setStatus(1);
		this.task.setStatusDate(new Date());
		this.task.setStatusUserId(-1);
		this.task.setAssignDate(new Date());
		this.task.setSrokDate(new Date());
		this.task.setTaskInfo("да се направи");

		this.task.setDocRequired(1);
		this.task.setCodeExecs(new ArrayList<>());

		this.task.setCodeAssign(13);
		this.task.setCodeControl(14);
		this.task.getCodeExecs().add(9);

		try {
			JPA.getUtil().runInTransaction(() -> this.task = dao.save(this.task, null, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		this.task.setCodeAssign(22);
		this.task.getCodeExecs().add(23);

		this.task.setStatus(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP);
		this.task.setStatusComments("alabala");

		try {
			JPA.getUtil().runInTransaction(() -> this.task = dao.save(this.task, null, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		this.task.setTaskInfo("да не се прави");

		try {
			JPA.getUtil().runInTransaction(() -> this.task = dao.save(this.task, null, sd));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}

		try {
			JPA.getUtil().runInTransaction(() -> dao.delete(this.task));
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/**  */
//	@Test
	public void testRestCompleteTask() {
		try {
			ArrayList<LinkedHashMap<String, Object>> result = (ArrayList<LinkedHashMap<String, Object>>) dao.restFindTasksByUser(23, 5, 0, sd);

			LOGGER.info("found tasks={}", result.size());

			if (!result.isEmpty()) {
				JPA.getUtil().runInTransaction(() -> this.task = dao.restStatusTaskChange((Integer) result.get(0).get("TASK_ID")
					, OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP, "alabala", null, null, sd));

				LOGGER.info("\t{}", this.task.getStatusComments());
				assertEquals("alabala", this.task.getStatusComments());
			}

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/**  */
//	@Test
	public void testRestFindTasksByUser() {
		try {
			ArrayList<LinkedHashMap<String, Object>> result = (ArrayList<LinkedHashMap<String, Object>>) dao.restFindTasksByUser(23, 5, 0, sd);

			LOGGER.info("found tasks={}", result.size());

			for (LinkedHashMap<String, Object> row : result) {
				LOGGER.info("\t{}", row);
			}

		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
}

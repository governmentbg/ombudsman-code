package com.ib.omb.search;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.omb.search.TaskSearch;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;

/**
 * Тест клас за {@link TaskSearch}
 *
 * @author belev
 */
public class TestTaskSearch {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestTaskSearch.class);

	private static SystemData sd;

	/** @throws Exception */
	@BeforeClass
	public static void setUp() throws Exception {
		JPA.getUtil();
		sd = new SystemData();
		sd.getNameClassification(Constants.CODE_CLASSIF_ADMIN_STR, 1);
	}

	/** */
	@Test
	public void buildQueryTaskList() {
		try {
			Date date = new Date();

			TaskSearch search = new TaskSearch(1);

			search.setCodeAssignList(Arrays.asList(22));

			search.buildQueryTaskList(new UserData(-1, "", ""), true);

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(search, "a0");
			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);

			for (Object[] row : result) {
				LOGGER.info(Arrays.toString(row));

				LOGGER.info("ИД задача = {} ", row[0]);

				LOGGER.info("	Изпълнители = {}", sd.decodeItems(Constants.CODE_CLASSIF_ADMIN_STR, (String) row[9], 1, date));

			}

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}

	/** */
	@Test
	public void testBuildQueryTasksInDoc() {
		try {
			Date date = new Date();
			TaskSearch search = new TaskSearch(1);
			search.setDocId(382);

			search.buildQueryTasksInDoc();

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(search, "a0");
			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);

			for (Object[] row : result) {
				LOGGER.info("ИД задача = {} ", row[0]);

				LOGGER.info("	Изпълнители = {}", sd.decodeItems(Constants.CODE_CLASSIF_ADMIN_STR, (String) row[6], 1, date));
			}

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}
	
//	@Test
	public void testbuildQueryTaskListWithFullText() {
		try {
			Date date = new Date();

			TaskSearch search = new TaskSearch(1);

			search.setCodeAssignList(Arrays.asList(22));

			search.buildQueryTaskListWithFullText(new UserData(-1, "", ""),true);

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(search, "a0");
			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);

			for (Object[] row : result) {
				LOGGER.info(Arrays.toString(row));

				LOGGER.info("ИД задача = {} ", row[0]);

				LOGGER.info("	Изпълнители = {}", sd.decodeItems(Constants.CODE_CLASSIF_ADMIN_STR, (String) row[9], 1, date));

			}

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}

}
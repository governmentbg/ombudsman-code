package com.ib.omb.search;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.omb.search.DeloSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.utils.DateUtils;

/**
 * Тест клас за {@link DeloSearch}
 *
 * @author belev
 */
public class TestDeloSearch {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDeloSearch.class);

	/***/
	@Before
	public void setUp() {
		JPA.getUtil();
	}

	/** */
	@Test
	public void testBuildQueryComp() {
		try {
			DeloSearch search = new DeloSearch(1);
			search.setRnDelo("94-00-425");
			search.setRnDeloEQ(true);

			search.setDeloDateFrom(DateUtils.parse("30.01.2020"));
			search.setDeloDateTo(DateUtils.parse("30.04.2020"));

			search.setDeloTypeArr(new Integer[] { OmbConstants.CODE_ZNACHENIE_DELO_TYPE_DOSS });
			search.setStatusArr(new Integer[] { OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ACTIVE });

			search.buildQueryComp(new UserData(-1, "", ""));

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(search, "a0");
			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);

			for (Object[] row : result) {
				LOGGER.info(Arrays.toString(row));
			}
			LOGGER.info("{}", result.size());

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}

	/** */
	@Test
	public void testBuildQueryDeloList() {
		try {
			DeloSearch search = new DeloSearch(1);
			search.setRnDelo("Ж-19");
			search.setRnDeloEQ(true);

//			search.setDeloDateFrom(DateUtils.parse("30.01.2020"));
//			search.setDeloDateTo(DateUtils.parse("30.04.2020"));
//
//			search.setStatusDateFrom(DateUtils.parse("29.02.2020"));
//			search.setStatusDateTo(DateUtils.parse("31.03.2020"));
//
			search.setDeloTypeArr(new Integer[] { OmbConstants.CODE_ZNACHENIE_DELO_TYPE_DOSS });
			search.setStatusArr(new Integer[] { OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ACTIVE });

			search.setCodeRefLeadList(Arrays.asList(-1));

			search.setDeloName("относно");
			search.setDeloInfo("ла");

			search.buildQueryDeloList(new UserData(-1, "", ""), 0);

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(search, "a0");
			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);

			for (Object[] row : result) {
				LOGGER.info(Arrays.toString(row));
			}
			LOGGER.info("{}", result.size());

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}
	
//	@Test
	public void testBuildQueryDeloListFullText() {
		try {
			DeloSearch search = new DeloSearch(1);
			search.setRnDelo("Ж-19");
			search.setRnDeloEQ(true);

			search.setDeloTypeArr(new Integer[] { OmbConstants.CODE_ZNACHENIE_DELO_TYPE_DOSS });
			search.setStatusArr(new Integer[] { OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ACTIVE });

			search.setCodeRefLeadList(Arrays.asList(-1));

			search.setDeloName("относно");
			search.setDeloInfo("ла");

			search.buildQueryDeloListFullText(new UserData(-1, "", ""),1);

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(search, "a0");
			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);

			for (Object[] row : result) {
				LOGGER.info(Arrays.toString(row));
			}
			LOGGER.info("{}", result.size());

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}
}
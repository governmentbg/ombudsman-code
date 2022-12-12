package com.ib.omb.search;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.omb.search.ExtendedUserSearch;
import com.ib.omb.search.ReferentSearch;
import com.ib.omb.system.SystemData;

/**
 * Тест клас за {@link ReferentSearch}
 *
 * @author belev
 */
public class TestReferentSearch {

	private static SystemData sd;

	private static final Logger LOGGER = LoggerFactory.getLogger(TestReferentSearch.class);

	/***/
	@BeforeClass
	public static void setUp() {
		sd = new SystemData();
	}

	/** */
	@Test
	public void testBuildQuery() {
		try {

			ReferentSearch search = new ReferentSearch();
			search.setKatNarList(Arrays.asList(1));
			search.setVidNarList(Arrays.asList(1));
			search.setTipOrganList(Arrays.asList(4));
			search.calcEkatte(sd); // за да може при въведена област или община да се определи какво да се сложи в селекта
			search.buildQuery();

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(search, "a0");
			List<Object[]> result = lazy.load(0, 10, null, null);

			for (Object[] objects : result) {
				LOGGER.info(Arrays.toString(objects));
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/** */
	@Test
	public void testExtendedUserSearch() {
		try {
			ExtendedUserSearch search = new ExtendedUserSearch();

			search.setRegistratura(1);
			search.setBusinessRole(1);

			search.buildQueryUserList();

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(search, "user_id");
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
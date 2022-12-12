package com.ib.omb.db.dao;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.omb.db.dao.DeloDeloDAO;
import com.ib.system.ActiveUser;
import com.ib.system.db.SelectMetadata;

/**
 * Test class for {@link DeloDeloDAO}
 *
 * @author belev
 */
public class TestDeloDeloDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDeloDeloDAO.class);

	private static DeloDeloDAO dao;

	/** @throws Exception */
	@BeforeClass
	public static void setUp() throws Exception {
		dao = new DeloDeloDAO(ActiveUser.DEFAULT);
	}

	/**
	 * Test method for {@link DeloDeloDAO#createSelectDeloListInDelo(Integer)}.
	 */
	@Test
	public void testCreateSelectDeloListInDelo() {
		try {
			SelectMetadata sm = dao.createSelectDeloListInDelo(285);

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(sm, "a0");
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

package com.ib.omb.db.dao;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.omb.db.dao.DocShemaDAO;
import com.ib.system.ActiveUser;
import com.ib.system.db.SelectMetadata;

/**
 * Test class for {@link DocShemaDAO}
 *
 * @author belev
 */
public class TestDocShemaDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDocShemaDAO.class);

//	private static SystemData sd;

	private static DocShemaDAO dao;

	/**  */
	@BeforeClass
	public static void setUp() {
		dao = new DocShemaDAO(ActiveUser.DEFAULT);

//		sd = new SystemData();
	}

//	private DocShema docShema;

	/** Test method for {@link DocShemaDAO#createSelectMetadataByValid(Integer, boolean)}. */
	@Test
	public void testCreateSelectMetadataByValid() {
		try {
			SelectMetadata sm = dao.createSelectMetadataByValid(1, true);

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

	/** Test method for {@link DocShemaDAO#createSelectMetadataByYear(Integer, String, String)}. */
	@Test
	public void testCreateSelectMetadataByYear() {
		try {
			SelectMetadata sm = dao.createSelectMetadataByYear(2020, null, null);

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

//	/** Test method for {@link DocShemaDAO#delete(DocShema)}. */
//	@Test
//	public void testDelete() {
//		try {
//			JPA.getUtil().runWithClose(() -> this.docShema = dao.findById(1));
//
//			JPA.getUtil().runInTransaction(() -> dao.delete(this.docShema));
//
//		} catch (ObjectInUseException e) {
//			LOGGER.debug(e.getMessage(), e);
//
//		} catch (BaseException e) {
//			fail(e.getMessage());
//			LOGGER.error(e.getMessage(), e);
//		}
//	}

//	/** Test method for {@link DocShemaDAO#save(DocShema)}. */
//	@Test
//	public void testSave() {
//		try {
//			SelectMetadata sm = dao.createSelectMetadataByYear(2020, null, null);
//
//			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(sm, "a0");
//			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null, null);
//
//			if (!result.isEmpty()) {
//				JPA.getUtil().runWithClose(() -> this.docShema = dao.findById(SearchUtils.asInteger(result.get(0)[0])));
//
//				JPA.getUtil().runInTransaction(() -> this.docShema = dao.save(this.docShema, sd));
//			}
//
//		} catch (ObjectInUseException e) {
//			LOGGER.debug(e.getMessage(), e);
//
//		} catch (BaseException e) {
//			fail(e.getMessage());
//			LOGGER.error(e.getMessage(), e);
//		}
//	}
}

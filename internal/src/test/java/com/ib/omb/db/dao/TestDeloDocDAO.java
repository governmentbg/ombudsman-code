package com.ib.omb.db.dao;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.omb.db.dao.DeloDocDAO;
import com.ib.omb.db.dao.DocDocDAO;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SearchUtils;

/**
 * Test class for {@link DeloDocDAO}
 *
 * @author belev
 */
public class TestDeloDocDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDeloDocDAO.class);

	private static SystemData sd;

	private static DeloDocDAO	dao;
	private static DocDocDAO	docDocDao;

	/** @throws Exception */
	@BeforeClass
	public static void setUp() throws Exception {
		dao = new DeloDocDAO(ActiveUser.DEFAULT);
		docDocDao = new DocDocDAO(ActiveUser.DEFAULT);

		sd = new SystemData();
	}

//	private DocDoc docDoc;

	/**
	 * Test method for {@link DeloDocDAO#createSelectDocListInDelo(Integer)}.
	 */
	@Test
	public void testCreateSelectDeloListByDoc() {
		try {
			SelectMetadata sm = dao.createSelectDeloListByDoc(670);

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

	/**
	 * Test method for {@link DeloDocDAO#createSelectDeloListByDoc(Integer)}.
	 */
	@Test
	public void testCreateSelectDocListInDelo() {
		try {
			SelectMetadata sm = dao.createSelectDocListInDelo(314);

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

	/**
	 * Test method for {@link DeloDocDAO#createSelectDeloListByDoc(Integer)}.
	 */
	@Test
	public void testDocDocDAOCreateSelectDocListInDelo() {
		try {
			Integer docId = 313;

			List<Object[]> rows = docDocDao.findDocDocList(docId);

			for (Object[] row : rows) {
				LOGGER.info(Arrays.toString(row));

				if (SearchUtils.asInteger(row[8]).equals(docId)) {
					LOGGER.info("вид на връзката: " //
						+ sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_REL_TYPE, SearchUtils.asInteger(row[1]), 1, null));
				} else {
					LOGGER.info("вид на връзката: " //
						+ sd.decodeItemDopInfo(OmbConstants.CODE_CLASSIF_DOC_REL_TYPE, SearchUtils.asInteger(row[1]), 1, null));
				}
			}

		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

//	/**
//	 * Test method for {@link DeloDocDAO#createSelectDeloListByDoc(Integer)}.
//	 */
//	@Test
//	public void testDocDocDAOSave() {
//		try {
//			JPA.getUtil().runInTransaction(() -> this.docDoc = docDocDao.save(null, 1, 263, 301));
//
//			JPA.getUtil().runInTransaction(() -> this.docDoc = docDocDao.save(this.docDoc.getId(), 2, 263, 301));
//
//			JPA.getUtil().runInTransaction(() -> docDocDao.delete(this.docDoc));
//
//		} catch (BaseException e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
//		}
//	}
}

package com.ib.omb.db.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.DeloDAO;
import com.ib.omb.db.dao.DeloDocDAO;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;

/**
 * Test class for {@link DeloDAO}
 *
 * @author belev
 */
public class TestDeloDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDeloDAO.class);

	private static SystemData sd;

	private static DeloDAO		dao;
	private static DocDAO		docDao;
	private static DeloDocDAO	deloDocDao;

	/** @throws Exception */
	@BeforeClass
	public static void setUp() throws Exception {
		dao = new DeloDAO(ActiveUser.DEFAULT);
		docDao = new DocDAO(ActiveUser.DEFAULT);
		deloDocDao = new DeloDocDAO(ActiveUser.DEFAULT);

		sd = new SystemData();
	}

	private Doc doc;

	private Delo delo;

	/**
	 * Test method for {@link DeloDAO#findById(Object)}.
	 */
	@Test
	public void testFindByIdObject() {
		try {
			JPA.getUtil().runWithClose(() -> this.delo = dao.findById(249));
			if (this.delo != null) {
				assertNotNull(this.delo.getId());
			}
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

//	/** */
//	@Test
//	public void testFullCycle() {
//		this.doc = new Doc();
//
//		this.doc.setRegistraturaId(1);
//		this.doc.setRegisterId(1);
//		this.doc.setOtnosno("тестове на преписка");
//
//		this.doc.setDocType(1);
//		this.doc.setDocVid(1);
//		this.doc.setDocDate(new Date());
//		this.doc.setFreeAccess(1);
//
//		LOGGER.info("-------------------- запис а инит дока, а с него и делото --------------------");
//		try {
//			JPA.getUtil().runInTransaction(() -> this.doc = docDao.save(this.doc, true, null, null, sd));
//		} catch (BaseException e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
//		}
//
//		SelectMetadata sm = deloDocDao.createSelectDeloListByDoc(this.doc.getId());
//		LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(sm, "a0");
//		List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null, null);
//
//		assertEquals(1, result.size()); // един ред трябва да е
//
//		Integer deloId = SearchUtils.asInteger(result.get(0)[4]);
//
//		LOGGER.info("-------------------- взимане на новосъдадената преписка по ИД --------------------");
//		try {
//			JPA.getUtil().runWithClose(() -> this.delo = dao.findById(deloId));
//		} catch (BaseException e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
//		}
//
//		DeloSearch deloSearch = new DeloSearch(1);
//		deloSearch.setUseDost(false);
//		deloSearch.buildQueryComp(new UserData(-1, "", ""));
////		List<Object[]> resultDelo = new LazyDataModelSQL2Array(deloSearch, "a0").load(0, 1, null, null, null);
//
////		DeloDelo deloDelo = new DeloDelo();
////		deloDelo.setDeloId(SearchUtils.asInteger(resultDelo.get(0)[0])); // в това се влага
////		this.delo.setDeloDelo(deloDelo);
//
//		this.delo.setStatus(CODE_ZNACHENIE_DELO_STATUS_COMPLETED);
//		LOGGER.info("-------------------- запис на преписка с влагане в друга  --------------------");
//		try {
//			JPA.getUtil().runInTransaction(() -> this.delo = dao.save(this.delo, false, sd));
//
//		} catch (Exception e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
//		}
//
////		LOGGER.info("-------------------- премахване на връзката с другата преписка  --------------------");
////		try {
////			JPA.getUtil().runInTransaction(() -> this.delo = dao.removeCurrentFromDelo(this.delo, sd));
////
////		} catch (Exception e) {
////			LOGGER.error(e.getMessage(), e);
////			fail(e.getMessage());
////		}
//
//		DocSearch docSearch = new DocSearch(1);
//		docSearch.setUseDost(false);
//		docSearch.buildQueryComp(new UserData(-1, "", ""));
//		List<Object[]> resultDoc = new LazyDataModelSQL2Array(docSearch, "a0").load(0, 10, null, null, null);
//
//		List<Integer> docIdList = new ArrayList<>();
//		for (Object[] row : resultDoc) {
//			docIdList.add(SearchUtils.asInteger(row[0]));
//		}
//
//		LOGGER.info("-------------------- влагане на документи в преписка  --------------------");
//		try {
//			JPA.getUtil().runInTransaction(() -> deloDocDao.saveInDelo(docIdList, this.delo.getId(), 1, OmbConstants.CODE_ZNACHENIE_DELO_SECTION_INTERNAL, new Date()));
//
//		} catch (Exception e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
//		}
//
//		LOGGER.info("-------------------- махане на документи от преписка  --------------------");
//		SelectMetadata sm1 = deloDocDao.createSelectDocListInDelo(this.delo.getId());
//		LazyDataModelSQL2Array lazy1 = new LazyDataModelSQL2Array(sm1, "a0");
//		List<Object[]> result1 = lazy1.load(0, lazy1.getRowCount(), null, null, null);
//		List<Integer> deloDocIdList = new ArrayList<>(result1.size());
//		for (Object[] row : result1) {
//			deloDocIdList.add(SearchUtils.asInteger(row[0]));
//		}
//		try {
//			JPA.getUtil().runInTransaction(() -> deloDocDao.deleteInDelo(deloDocIdList));
//
//		} catch (Exception e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
//		}
//
//		LOGGER.info("-------------------- Изтриване на преписка  --------------------");
//		try {
//			JPA.getUtil().runInTransaction(() -> dao.delete(this.delo));
//
//		} catch (Exception e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
//		}
//
//		LOGGER.info("-------------------- Изтриване на документ  --------------------");
//		try {
//			JPA.getUtil().runInTransaction(() -> docDao.delete(this.doc));
//
//		} catch (Exception e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
//		}
//
//	}
}

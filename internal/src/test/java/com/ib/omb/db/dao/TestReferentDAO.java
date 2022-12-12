package com.ib.omb.db.dao;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.omb.db.dao.ReferentDAO;
import com.ib.omb.db.dao.ReferentDelegationDAO;
import com.ib.omb.db.dto.Referent;
import com.ib.omb.db.dto.ReferentAddress;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SysClassifUtils;

/**
 * Test class for {@link ReferentDAO}
 *
 * @author belev
 */
public class TestReferentDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestReferentDAO.class);

	private static ReferentDAO dao;

	private static ReferentDelegationDAO referentDelegationDao;

	private static SystemData sd;

	/***/
	@BeforeClass
	public static void setUp() {
		dao = new ReferentDAO(ActiveUser.DEFAULT);
		referentDelegationDao = new ReferentDelegationDAO(ActiveUser.DEFAULT);

		sd = new SystemData();
	}

	private Referent referent;

	/** */
	@Test
	public void testClassifAdmStruct() {
		try {
			Map<Integer, String> tabs = new HashMap<>();
			tabs.put(0, "");
			for (int i = 0; i < 20; i++) {
				tabs.put(i + 1, tabs.get(i) + "\t");
			}

			Query query = JPA.getUtil().getEntityManager().createNativeQuery("select distinct DATE_OT from ADM_REFERENTS where CODE_CLASSIF = 24 order by 1");
			@SuppressWarnings("unchecked")
			List<Date> dates = query.getResultList();
			JPA.getUtil().closeConnection();

			for (Date date : dates) {
				List<SystemClassif> list = sd.getSysClassification(CODE_CLASSIF_ADMIN_STR, date, 1);
				SysClassifUtils.doSortClassifPrev(list);
				System.out.println("---------------------------------------------------------------------------------------");
				System.out.println(date);
				for (SystemClassif item : list) {
					System.out.println(tabs.get(item.getLevelNumber()) + item.getCode() + " " + item.getTekst() + " (" + item.getDopInfo() + ")-" + item.getCodeExt());
				}
				System.out.println("---------------------------------------------------------------------------------------");
			}

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}

	/** */
	@Test
	public void testCreateSelectEmployeeList() {
		try {
			SelectMetadata sm = dao.createSelectEmployeeList(9, new Date(), sd);

			if (sm != null) {
				LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(sm, "a1, a2");
				List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);

				for (Object[] row : result) {
					LOGGER.info(Arrays.toString(row));
				}
				LOGGER.info("{}", result.size());
			}

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/** */
	@Test
	public void testFindById() {
		try {
			JPA.getUtil().runWithClose(() -> this.referent = dao.findById(15));

			assertNotNull(this.referent);

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail();
		}
	}

	/** */
	@Test
	public void testFindZvenoList() {
		try {
			List<SystemClassif> zvenoList = dao.findZvenoList(new Date(), sd);

			assertNotNull(zvenoList);

			SysClassifUtils.doSortClassifPrev(zvenoList);

			for (SystemClassif zveno : zvenoList) {
				LOGGER.info(zveno.getTekst());
			}

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail();
		}
	}

	/** */
//	@Test
	public void testFullCycle() {
		this.referent = new Referent();
		this.referent.setRefType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL);
		this.referent.setRefName("Тест НФЛ");

		try {
			JPA.getUtil().runInTransaction(() -> this.referent = dao.save(this.referent));

			assertNotNull(this.referent.getId());
			assertNotNull(this.referent.getCode());

			this.referent.setContactPhone("123456798");

			this.referent.setAddress(new ReferentAddress());
			this.referent.getAddress().setAddrCountry(37);
			this.referent.getAddress().setAddrText("alabala");

			JPA.getUtil().runInTransaction(() -> this.referent = dao.save(this.referent));

			JPA.getUtil().runInTransaction(() -> dao.delete(this.referent));

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail();
		}
	}

	/** */
	@Test
	public void testReferentDelegationCreateSelectMetadata() {
		try {
			SelectMetadata sm = referentDelegationDao.createSelectMetadata(1, -1, 10, true, DateUtils.parse("03.03.2020"), new Date());

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

	/** */
	@Test
	public void testSaveAdmReferent() {
		try {
//			Date date = DateUtils.parse("30.01.2020");

//			JPA.getUtil().runWithClose(() -> this.referent = dao.findByCode(622, date, true));
//
//			this.referent.setRefName("Звено 2 ново");
//
//			JPA.getUtil().runInTransaction(() -> this.referent = dao.saveAdmReferent(this.referent, new Date(), sd));
//
//			assertNotNull(this.referent);

//			this.referent = new Referent();
//			this.referent.setRefRegistratura(1);
//			this.referent.setCodeParent(623);
//			this.referent.setCodePrev(621);

//			this.referent.setRefName("Служител 2");
//			this.referent.setRefType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL);

//			JPA.getUtil().runInTransaction(() -> this.referent = dao.saveAdmReferent(this.referent, date, sd));

//			JPA.getUtil().runInTransaction(() -> this.referent = dao.moveAdmZveno(this.referent, 0, 625, date, false, false, sd));

//			JPA.getUtil().runInTransaction(() -> dao.moveAdmEmployeeList(Arrays.asList(626), 622, date, sd));

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			fail();
		}

	}
}
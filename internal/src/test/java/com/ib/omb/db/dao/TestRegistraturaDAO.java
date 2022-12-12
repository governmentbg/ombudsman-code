package com.ib.omb.db.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.omb.db.dao.DocVidSettingDAO;
import com.ib.omb.db.dao.RegistraturaDAO;
import com.ib.omb.db.dto.Registratura;
import com.ib.omb.db.dto.RegistraturaSetting;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.BaseException;

/**
 * Тест клас за {@link RegistraturaDAO}
 *
 * @author belev
 */
public class TestRegistraturaDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestRegistraturaDAO.class);

	private static SystemData sd;

	private static RegistraturaDAO dao;

	/** @throws Exception */
	@BeforeClass
	public static void setUp() throws Exception {
		sd = new SystemData();
		dao = new RegistraturaDAO(ActiveUser.DEFAULT);
		sd.getNameClassification(Constants.CODE_CLASSIF_ADMIN_STR, SysConstants.CODE_DEFAULT_LANG);
	}

	private Registratura		registratura;
	private List<Registratura>	registraturi;

	private List<RegistraturaSetting> settings;

	/** */
//	@Test
	public void testDelete() {
		try {
			JPA.getUtil().runWithClose(() -> this.registratura = dao.findById(1));
			if (this.registratura != null) {
				assertNotNull(this.registratura.getId());
			}

			JPA.getUtil().runInTransaction(() -> dao.delete(this.registratura));

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	/** */
	@Test
	public void testDocVidSettingDAOcreateSelectMetadataByRegistraturaId() {
		try {
			DocVidSettingDAO settingDao = new DocVidSettingDAO(ActiveUser.DEFAULT);

			SelectMetadata sm = settingDao.createSelectMetadataByRegistraturaId(1);

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(sm, "a1 desc");
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
	public void testFindAll() {
		try {
			JPA.getUtil().runWithClose(() -> this.registraturi = dao.findAllSelected());

			assertNotNull(this.registraturi);

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/** */
	@Test
	public void testFindById() {
		try {
			JPA.getUtil().runWithClose(() -> this.registratura = dao.findById(1));

			if (this.registratura != null) {
				assertNotNull(this.registratura.getId());
			}
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/** */
//	@Test
	public void testFindSaveRegistraturaSettings() {
		try {
			JPA.getUtil().runWithClose(() -> this.settings = dao.findRegistraturaSettings(2, sd));

			assertNotNull(this.settings);

			for (RegistraturaSetting setting : this.settings) {
				LOGGER.info(setting.toString());
			}

			JPA.getUtil().runInTransaction(() -> dao.saveRegistraturaSettings(this.settings));

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

//	/** */
//	@Test
//	public void testLoadAdmStruct() {
//		try {
//			List<SystemClassif> items = dao.loadAdmStruct(1, true, sd);
//
//			for (SystemClassif item : items) {
//				LOGGER.info(item.getTekst());
//			}
//
//		} catch (DbErrorException e) {
//			LOGGER.error(e.getMessage(), e);
//			fail(e.getMessage());
//		}
//	}

	/** */
//	@Test
	public void testRegistraturaSettings() {
		try {
			RegistraturaDAO.RegistraturaSettingDAO settingDao = new RegistraturaDAO.RegistraturaSettingDAO(ActiveUser.DEFAULT);

			JPA.getUtil().runWithClose(() -> this.settings = settingDao.findAll());

			for (RegistraturaSetting setting : this.settings) {
				Integer value = sd.getRegistraturaSetting(setting.getRegistraturaId(), setting.getSettingCode()); // от кеша

				assertEquals(value, setting.getSettingValue());
			}

		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}

	/** */
//	@Test
	public void testSave() {
		try {
			JPA.getUtil().runWithClose(() -> this.registratura = dao.findById(1));
			if (this.registratura != null) {
				assertNotNull(this.registratura.getId());
			}

			String address = "нов адрес";
			this.registratura.setAddress(address);

			JPA.getUtil().runInTransaction(() -> this.registratura = dao.save(this.registratura));

			assertEquals(address, this.registratura.getAddress());

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}
}
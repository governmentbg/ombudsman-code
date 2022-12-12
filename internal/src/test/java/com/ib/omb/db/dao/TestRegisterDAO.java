package com.ib.omb.db.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.RegisterDAO;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;

/**
 * Тест клас за {@link RegisterDAO}
 *
 * @author belev
 */
public class TestRegisterDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestRegisterDAO.class);

	private static RegisterDAO dao;

	/***/
	@BeforeClass
	public static void setUp() {
		dao = new RegisterDAO(ActiveUser.DEFAULT);
	}

	private List<Object[]> registers;

	/** */
	@Test
	public void testFindByRegistraturaId() {
		try {
			JPA.getUtil().runWithClose(() -> this.registers = dao.findByRegistraturaId(1, true));
			assertNotNull(this.registers);

			for (Object[] row : this.registers) {
				LOGGER.info(Arrays.toString(row));
			}

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/** */
	@Test
	public void testFindRegistersCommon() {
		try {
			JPA.getUtil().runWithClose(() -> this.registers = dao.findRegistersCommon(null));
			assertNotNull(this.registers);

			for (Object[] row : this.registers) {
				LOGGER.info(Arrays.toString(row));
			}

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}

	/** */
	@Test
	public void testFindRegistersCopy() {
		try {
			JPA.getUtil().runWithClose(() -> this.registers = dao.findRegistersCopy(1, 2, true));
			assertNotNull(this.registers);

			for (Object[] row : this.registers) {
				LOGGER.info(Arrays.toString(row));
			}

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}
}
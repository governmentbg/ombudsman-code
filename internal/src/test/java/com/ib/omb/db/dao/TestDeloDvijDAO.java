package com.ib.omb.db.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.DeloDvijDAO;
import com.ib.omb.db.dto.DeloDvij;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;

/**
 * Test class for {@link DeloDvijDAO}
 *
 * @author mamun
 */
public class TestDeloDvijDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDeloDvijDAO.class);
	
	private DeloDvij dvij;

	private static SystemData sd;

	/** @throws Exception */
	@BeforeClass
	public static void setUp() throws Exception {
		sd = new SystemData();
	}

//	@Test
	public void testSaveDelete() {
		try {
			
			
			dvij = new DeloDvij();
			dvij.setCodeRef(1);
			//dvij.setConfirmDate(new Date());
			dvij.setDeloId(302);
			dvij.setDvijDate(new Date());
			dvij.setDvijInfo("3");
			dvij.setDvijMethod(1);
			dvij.setDvijText("5 - unit test");			
			dvij.setReturnDate(new Date());
			dvij.setReturnInfo("12");
			dvij.setReturnMethod(1);
			dvij.setReturnToDate(new Date());
			dvij.setStatus(14);
			dvij.setStatusDate(new Date());
			dvij.setStatusText("15");
			
			DeloDvijDAO dao = new DeloDvijDAO(ActiveUser.DEFAULT);
						
			JPA.getUtil().runInTransaction(() -> dvij = dao.save(dvij, sd));
			
			
			assertNotNull(dvij.getId());
					
			JPA.getUtil().runInTransaction(() -> dao.delete(dvij, sd));
			
		
		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}
	
//	@Test
	public void testfindDeloDvij() {
		try {
			
			DeloDvijDAO dao = new DeloDvijDAO(ActiveUser.DEFAULT);
			List<DeloDvij> allDvij = dao.getDeloDvij(792);
			assertNotNull(allDvij);
			System.out.println("DvijSize = "+ allDvij.size());
		
			
		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	
//	@Test
	public void testUpdateVlDvij() {
		try {
			
			DeloDvijDAO dao = new DeloDvijDAO(ActiveUser.DEFAULT);
			
			dvij = dao.findById(3);
			dvij.setStatusText("Промяна на " + new Date());
			JPA.getUtil().runInTransaction(() -> dvij = dao.save(dvij, sd));
		
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	
	
	
}

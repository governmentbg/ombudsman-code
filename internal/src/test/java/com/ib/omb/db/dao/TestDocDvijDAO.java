package com.ib.omb.db.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.DocDvijDAO;
import com.ib.omb.db.dto.DocDvij;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;

/**
 * Test class for {@link DocDvijDAO}
 *
 * @author mamun
 */
public class TestDocDvijDAO {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestDocDvijDAO.class);
	
	private DocDvij dvij;
		
//	@Test
	public void testSaveDelete() {
		try {
			
			
			dvij = new DocDvij();
			dvij.setCodeRef(1);
			//dvij.setConfirmDate(new Date());
			dvij.setDocId(2);
			dvij.setDvijDate(new Date());
			dvij.setDvijInfo("3");
			dvij.setDvijMethod(4);
			dvij.setDvijText("5");
			dvij.setEkzNomer(6);
			dvij.setForRegid(7);
			//dvij.setMessageGuid("8");
			dvij.setOtherDocId(9);
	//		dvij.setRetrunCodeRef(10);
		//	dvij.setRetrunTextRef("11");
			dvij.setReturnDate(new Date());
			dvij.setReturnInfo("12");
			dvij.setReturnMethod(13);
			dvij.setReturnToDate(new Date());
			dvij.setStatus(14);
			dvij.setStatusDate(new Date());
			dvij.setStatusText("15");
			
			DocDvijDAO dao = new DocDvijDAO(ActiveUser.DEFAULT);
						
			JPA.getUtil().runInTransaction(() -> dvij = dao.save(dvij));
			
			
			assertNotNull(dvij.getId());
					
			JPA.getUtil().runInTransaction(() -> dao.delete(dvij));
			
		
		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	@Test
	public void testfindDocDvij() {
		try {
			
			DocDvijDAO dao = new DocDvijDAO(ActiveUser.DEFAULT);
			List<DocDvij> allDvij = dao.getDocDvij(330);
			assertNotNull(allDvij);
			System.out.println("DvijSize = "+ allDvij.size());
		
			
		} catch (Exception e) {
			fail(e.getMessage());
			LOGGER.error(e.getMessage(), e);
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
	
	
}

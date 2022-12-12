package com.ib.omb.db.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.ib.omb.db.dao.RegistraturaMailBoxDAO;
import com.ib.omb.db.dto.RegistraturaMailBox;
import com.ib.omb.db.dto.RegistraturaMailBoxVar;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;

public class TestRegistraturaMailBoxDAO {
	
	
	
//	@Test
	public void testCycle() {
		
		RegistraturaMailBoxDAO dao = new RegistraturaMailBoxDAO(ActiveUser.DEFAULT);
		Integer id = null;
		
		try {
			JPA.getUtil().begin();
			
			RegistraturaMailBox box = new RegistraturaMailBox();
			box.setMailboxName("test");
			box.setMailboxUsername("user");
			box.setMailboxPassword("pass");
			box.setRegistraturaId(1);
			
			RegistraturaMailBoxVar var = new RegistraturaMailBoxVar();
			var.setMailKey("key");
			var.setMailValue("value");
			var.setFlag(2);
			box.getVariables().add(var);
			
			
			
			box = dao.save(box);
			id = box.getId();
			assertNotNull(id);
			
			JPA.getUtil().commit();
		
			
			
		}catch (Exception e) {
			e.printStackTrace();
			JPA.getUtil().rollback();
			fail();
		}finally {
			JPA.getUtil().closeConnection();
		}
		
		try {
			JPA.getUtil().begin();
			
			List<RegistraturaMailBox> list = dao.findRegBoxes(1);
			assertNotNull(list);
			assertTrue(list.size()> 0);
			System.out.println("Size = "+ list.size());
			
			
			RegistraturaMailBox box = dao.findById(id);
			assertNotNull(box);
			dao.delete(box);
			
			JPA.getUtil().commit();
		
			list = dao.findRegBoxes(1);
			assertNotNull(list);			
			System.out.println("Size = "+ list.size());
			
			
		}catch (Exception e) {
			e.printStackTrace();
			JPA.getUtil().rollback();
			fail();
		}finally {
			JPA.getUtil().closeConnection();
		}
		
	}
	
	
//	@Test
	public void testLoadVariables() {
		
		RegistraturaMailBoxDAO dao = new RegistraturaMailBoxDAO(ActiveUser.DEFAULT);
		
		
		try {
			JPA.getUtil().begin();
			
			List<RegistraturaMailBoxVar> vars = dao.findBoxVariables(1);
			assertNotNull(vars);
			System.out.println("Local Size: " + vars.size());
			
			vars = dao.findGlobalVariables();
			assertNotNull(vars);
			System.out.println("Global Size: " + vars.size());
			
			JPA.getUtil().rollback();
		
			
			
		}catch (Exception e) {
			e.printStackTrace();
			JPA.getUtil().rollback();
		}finally {
			JPA.getUtil().closeConnection();
		}
		
		
		
	}
	
	

}

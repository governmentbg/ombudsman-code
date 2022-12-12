package com.ib.omb.db.dao;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;

import com.ib.omb.db.dao.UserNotificationsDAO;
import com.ib.omb.db.dto.UserNotifications;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.InvalidParameterException;



public class TestsUserNotificationsDAO {
	
	
	

	
	@Test
	public void saveUpdateDeleteMessage() throws InvalidParameterException {
		
		
		
		UserNotifications mess = new UserNotifications();
		mess.setDateMessage(new Date());
		mess.setIdUser(-1);
		mess.setTitle("Пробно съобщение");
		mess.setDetails("Някакви детайли ....");
		
		
		try {
			
			JPA.getUtil().begin();
			
			//JPA.getUtil().getEntityManager().persist(mess);
			
			UserNotificationsDAO dao = new UserNotificationsDAO();
			
			dao.save(mess);
			
			assertNotNull(mess.getId());
			
			dao.changeStatusMessage(mess.getId(), 2222);
			
			//dao.delete(mess);
			
			
			
			
			
			JPA.getUtil().commit();
			
		} catch (Exception e) {	
			fail();
			e.printStackTrace()	;		
		}	
			
		System.out.println("Done!");
		
	}
	
	
	
	@Test
	public void findMessages() throws InvalidParameterException {
		
		
		try {
			
			
			
			//JPA.getUtil().getEntityManager().persist(mess);
			
			UserNotificationsDAO dao = new UserNotificationsDAO();
			
			ArrayList<UserNotifications> result = dao.findUserNotifications(-1, OmbConstants.CODE_NOTIF_STATUS_NEPROCHETENA, 1);
			
			assertTrue(result.size() > 0);
			
			
			
		} catch (Exception e) {	
			fail();
			e.printStackTrace()	;		
		}	
			
		System.out.println("Done!");
		
	}
	
	
	
	


}
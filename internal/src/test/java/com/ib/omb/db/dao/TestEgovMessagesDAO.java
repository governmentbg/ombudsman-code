package com.ib.omb.db.dao;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.Query;

import org.junit.Test;

import com.ib.omb.db.dao.EgovMessagesDAO;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SearchUtils;


public class TestEgovMessagesDAO {
	
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testFilter() {
		
		UserData ud = new UserData(-1, "zxc", "Dragancho");
		ud.setUserAccess(-1);
		EgovMessagesDAO dao = new EgovMessagesDAO(ud);
		String guidRegistraturа = "ad9dd268-88ee-e911-80dc-001dd8b71c5b";
		
		try {
			SelectMetadata smd = dao.createFilterEgovMessages(guidRegistraturа,null, null ,false);			
			Query q = JPA.getUtil().getEntityManager().createNativeQuery(smd.getSqlCount());
			if (smd.getSqlParameters() != null) {
				Set<Entry<String, Object>> entryset = smd.getSqlParameters().entrySet();			
				for (Entry<String, Object> entry : entryset) {
					q.setParameter(entry.getKey(), entry.getValue());
				}
			}
			Integer cnt = SearchUtils.asInteger(q.getSingleResult());
			System.out.println("Count:" + cnt);
			
			q = JPA.getUtil().getEntityManager().createNativeQuery(smd.getSql());
			if (smd.getSqlParameters() != null) {
				Set<Entry<String, Object>> entryset = smd.getSqlParameters().entrySet();			
				for (Entry<String, Object> entry : entryset) {
					q.setParameter(entry.getKey(), entry.getValue());
				}
			}
			
			ArrayList<Object[]> rows = (ArrayList<Object[]>) q.getResultList();
			System.out.println("CountSQL:" + rows.size());
			
			assertTrue(cnt == rows.size());
			
		}catch (Exception e) {
			e.printStackTrace();			
			fail();
		}finally {
			JPA.getUtil().closeConnection();
		}
		
	}
	
	
	
	@Test
	public void testLoadError() {
		
		UserData ud = new UserData(-1, "zxc", "Dragancho");
		ud.setUserAccess(-1);
		EgovMessagesDAO dao = new EgovMessagesDAO(ud);
		
		try {
			System.out.println(dao.getMessageError(300108));
		} catch (DbErrorException e) {
			e.printStackTrace();
			fail();
			
		}
		
	}
	
	

}

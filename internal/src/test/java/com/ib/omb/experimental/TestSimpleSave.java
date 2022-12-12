package com.ib.omb.experimental;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TreeSet;

import javax.persistence.Query;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.SyslogicListOpisDAO;
import com.ib.system.db.dao.SystemClassifDAO;
import com.ib.system.db.dao.SystemClassifOpisDAO;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemClassifOpis;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.Multilang;
import com.ib.system.utils.SearchUtils;

public class TestSimpleSave {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestSimpleSave.class);

	
	
	
	
	@Test
	public void testHelloSecret (){
		
		
		try {
			
			JPA.getUtil().begin();
			
			JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO delem(id) 	VALUES(0)").executeUpdate();
			JPA.getUtil().getEntityManager().flush();
			
			try {
				JPA.getUtil().getEntityManager().createNativeQuery("select idd from delem").getResultList();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
			
			
			JPA.getUtil().getEntityManager().createNativeQuery("INSERT INTO delem(id) 	VALUES(1)").executeUpdate();
			JPA.getUtil().getEntityManager().flush();
			
			
			
			JPA.getUtil().commit();
			
			System.out.println("end");
			
		} catch (Exception e) {			
			e.printStackTrace();
			JPA.getUtil().rollback();
		}
		
		
		
		
		
		
	}
	
	
	
	
	
	

}

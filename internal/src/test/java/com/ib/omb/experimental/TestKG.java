package com.ib.omb.experimental;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.xml.bind.JAXBException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.DeloDAO;
import com.ib.omb.db.dao.DeloDeloDAO;
import com.ib.omb.db.dao.DeloDocDAO;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.TaskDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.Task;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.utils.JAXBHelper;

import net.bytebuddy.asm.Advice.This;

public class TestKG {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestKG.class);
	
	private static SystemData sd;

	private static DocDAO		docDao;
	private static DeloDAO		deloDao;
	private static DeloDocDAO	deloDocDao;
	private static DeloDeloDAO	deloDeloDao;
	private static TaskDAO 		taskDao;
	private Doc				doc;
	@Test
	public void alabala() {
		LOGGER.info("This is INFO");
		LOGGER.warn("This is WARN");
		LOGGER.debug("This is DEBUG");
		LOGGER.error("This is ERROR");
	}
	
	@BeforeClass
	public static void setUp() {
		UserData ud = new UserData(-1, "", "");
		docDao = new DocDAO(ud);
		deloDao = new DeloDAO(ud);
		deloDocDao = new DeloDocDAO(ud);
		deloDeloDao = new DeloDeloDAO(ud);
		taskDao = new TaskDAO(ud);
		
		sd = new SystemData();
	}
	
	
	
	@Test
	public void testNewTask() throws Exception {
		Task task=new Task();
		
		task.setRegistraturaId(1);
		task.setTaskType(1);
		task.setStatus(1);
		task.setStatusDate(new Date());
		task.setStatusUserId(-1);
		task.setAssignDate(new Date());
		task.setSrokDate(new Date());
		task.setTaskInfo("да се направи");

		task.setDocRequired(1);
		task.setCodeExecs(new ArrayList<>());

		task.setCodeAssign(13);
		task.setCodeControl(14);
		task.getCodeExecs().add(9);

		try {
			JPA.getUtil().runInTransaction(() -> taskDao.save(task,null,sd));
//			JPA.getUtil().begin();
//			task = taskDao.save(task, null, sd);
			LOGGER.info("--OK-- dao.findById(taks.getId())ID="+task.getId());//+ " "+task.getPnr());
//			
//			JPA.getUtil().commit();
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			fail(e.getMessage());
		}
	}
	
	
	

}

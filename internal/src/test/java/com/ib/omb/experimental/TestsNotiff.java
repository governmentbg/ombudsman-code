package com.ib.omb.experimental;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;

import com.ib.omb.db.dao.MailPatternsDAO;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.NotificationPatternVariables;
import com.ib.omb.db.dto.NotificationPatterns;
import com.ib.omb.db.dto.Task;
import com.ib.omb.db.dto.UserNotifications;
import com.ib.omb.experimental.Notification;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.InvalidParameterException;



public class TestsNotiff {
	
	

	
	@Test
	public void startIt() throws InvalidParameterException {

		
		try {
			
//			SystemData sd = new SystemData();
//			Notification nb = new Notification(sd);
//			
//			//Зареждаме документа
//			Doc d1 = JPA.getUtil().getEntityManager().find(Doc.class, 429);
//						
//			nb.setDoc(d1);
//			
//			Task task = new Task();
//			//task.setTaskInfo("Edno kratko opisanie");
//			task.setRnTask("1111-222");
//			task.setTaskType(1);
//			nb.setTask(task);
//			
//			//Генерираме нотификация ....
//			UserNotifications mess = nb.generateNotif(27, 6);
//			
//			assertNotNull(mess);
//			assertNotNull(mess.getDetails());
//						
//			System.out.println(mess.getDetails());
			
		
			
		} catch (Exception e) {			
			e.printStackTrace()	;	
			fail();
		}
			
		System.out.println("Done!");
		
	}
	
	
	@Test
	public void generateVars() throws InvalidParameterException {

//		MailPatternsDAO dao = new MailPatternsDAO(ActiveUser.DEFAULT);
//		try {
//			JPA.getUtil().begin();
//			SystemData sd = new SystemData();
//			ArrayList<NotificationPatterns> patterns = sd.getAllPatterns();
//			for (NotificationPatterns p: patterns) {
//				if (p.getVariables().size() == 0) {
//					ArrayList<String> rez = getVarsFromTekst(p.getBody());
//					for (String tek : rez) {
//						NotificationPatternVariables v = new NotificationPatternVariables();
//						v.setVarName(tek);
//						v.setPattern(p);
//						p.getVariables().add(v);
//						
//					}
//				}
//				dao.save(p);
//			}
//		
//			JPA.getUtil().commit();
//		} catch (Exception e) {	
//			JPA.getUtil().rollback();
//			e.printStackTrace()	;	
//			fail();
//		}finally {
//			JPA.getUtil().closeConnection();
//		}
			
		System.out.println("Done!");
		
	}
	
	
	@Test
	public void printPatterns() throws InvalidParameterException {

		
		try {
			SystemData sd = new SystemData();
			Notification nb = new Notification(sd);
			ArrayList<NotificationPatterns> patterns = sd.getAllPatterns();
			
			Doc doc1 = new Doc();
			doc1.setRnDoc("94-00-123");
			doc1.setDocDate(new Date());
			doc1.setRegistraturaId(1);
			nb.setDoc(doc1);
			//nb.setDatBeg(new Date());
			
			Thread.sleep(2000);
			
			Doc doc2 = new Doc();
			doc2.setRnDoc("666");
			doc2.setDocDate(new Date());
			doc2.setRegistraturaId(1);
			nb.setOtherDoc(doc2);
			
			Task task = new Task();
			task.setRnTask("123-2020");
			task.setTaskType(1);
			task.setTaskInfo("Някакво инфо ...");
			task.setStatus(2);
			nb.setTask(task);
			//nb.setDatEnd(new Date());
			
			
			task.setCodeExecs(new ArrayList<Integer>());
			task.getCodeExecs().add(-1);
			task.getCodeExecs().add(12);
			task.getCodeExecs().add(13);
			
			
			
			Delo delo = new Delo();
			delo.setRnDelo("1");
			delo.setDeloDate(new Date());
			nb.setDelo(delo);
			
			for (NotificationPatterns p : patterns) {			
				//UserNotifications mess = nb.generateNotif(p.getEventId(), p.getRolia());
				//System.out.println(mess.getDetails());
				System.out.println();
				System.out.println();
				System.out.println("----------------------------------------------------------------------------------------");
				System.out.println();
				System.out.println();
				
				
				
			}
			
			
			
			
			
			
			
			
		} catch (Exception e) {	
			
			e.printStackTrace()	;	
			fail();
		}finally {
			JPA.getUtil().closeConnection();
		}
			
		System.out.println("Done!");
		
	}
	
	
	
	
	
	@Test
	public void testSave()  {

		MailPatternsDAO dao = new MailPatternsDAO(ActiveUser.DEFAULT);
		try {
			JPA.getUtil().begin();
			
			NotificationPatterns mp = new NotificationPatterns();
			mp.setBody("body");
			mp.setEventId(123);
			mp.setRolia(1111);
			mp.setSubject("subject");
			
			NotificationPatternVariables v = new NotificationPatternVariables();
			v.setCodeClassif(111);
			v.setPattern(mp);
			v.setVarName("varname");
			v.setVarRefl("varref");
			mp.getVariables().add(v);
			
			
			dao.save(mp);
			
		
			JPA.getUtil().rollback();
		} catch (Exception e) {	
			JPA.getUtil().rollback();
			e.printStackTrace()	;	
			fail();
		}finally {
			JPA.getUtil().closeConnection();
		}
			
		System.out.println("Done!");
		
	}
	
	
	
	
	private ArrayList<String> getVarsFromTekst(String tekst){
		ArrayList<String> vars = new ArrayList<String>();
		if (tekst == null) {
			return vars;
		}
		
		int index=0;
		int prev=0;
		int cnt = 0;
		while(index != -1) {
			prev = index;
			
			index = tekst.indexOf("&", index+1);
			if (index != -1) {
				cnt++;
				if (cnt%2==0) {
					System.out.println(tekst.substring(prev, index+1));
					vars.add(tekst.substring(prev, index+1));
				}
			}
		}
		
		return vars;
		
	}


}
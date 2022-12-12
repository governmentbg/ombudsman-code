package com.ib.omb.experimental;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.Test;

import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.SystemClassifDAO;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.mail.Mailer;
import com.ib.system.utils.FileUtils;
import com.ib.system.utils.Multilang;

public class TestIMAP {
	
	@Test
	public void TestCreateClassif() {
		
//		int codeClassif = 292;
//		int codePrev = 0;
//		int code = 1;
//		SystemClassifDAO dao = new SystemClassifDAO(ActiveUser.DEFAULT);
//		
//		try {
//		
//			byte[] bytes = FileUtils.getBytesFromFile(new File("d:\\imap.txt"));
//			String[] vars = new String(bytes).split("\r\n");
//			System.out.println("VARS.size = " + vars.length);
//			
//			JPA.getUtil().begin();
//			
//			for (String s : vars) {
//				SystemClassif item = new SystemClassif();
//				item.setCode(code);
//				item.setCodeClassif(codeClassif);
//				item.setCodePrev(codePrev);
//				item.setCodeParent(0);
//				item.setDateOt(new Date());
//				item.setLevelNumber(1);
//				
//				//List<Multilang> list = new ArrayList<Multilang>();
//				
//				Multilang lang = new Multilang();
//				lang.setLang(1);
//				lang.setTekst(s);
//				//list.add(lang);
//				item.getTranslations().add(lang);
//				
//				dao.doSimpleSave(item);
//				
//				codePrev = code;
//				code++;
//				
//				
//			}
//		
//			JPA.getUtil().commit();
//			
//		
//			
//		}catch (Exception e) {
//			e.printStackTrace();
//			fail(e.getMessage());
//		}finally {
//			JPA.getUtil().closeConnection();
//		}
	}
	
	
	@Test
	public void TestCreateProps(){
		
		SystemData sd = new SystemData();
		try {
			Properties prop = sd.getMailProp(1, "dddd");
			if (prop != null) {
				System.out.println(prop.size());
				
				Mailer mailer3 = new Mailer();
				int cnt = mailer3.getMessageCount(prop, prop.getProperty("user.name"), prop.getProperty("user.password"), "INBOX", false);
				System.out.println("*******************  + COUNT: " + cnt);
			}
		} catch (Exception e) {			
			e.printStackTrace();
			fail();
		}
		
		
	}
	
	

}

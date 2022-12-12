package com.ib.omb.system;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;

import org.junit.Test;

import com.ib.omb.system.SystemData;
import com.ib.system.mail.Mailer;
import com.ib.system.mail.Mailer.Content;
import com.ib.system.utils.FileUtils;

public class MailerTestNew {

	

//	@Test - файловете не са достъпни
	public void testSent() {
		Mailer mailer = new Mailer();
		SystemData sd = new SystemData();
		ArrayList<DataSource> filesDS = new ArrayList<DataSource>(); 
		// mailer.connectForSend(props);
		try {
			Properties props = sd.getMailProp(-1, "DEFAULT");
			if (props == null) {				
				fail("Properties за DEFAULT is NULL !!!");
			}
			
			byte[] bytes = FileUtils.getBytesFromFile(new File("d:\\отпуск.jpg"));
			ByteArrayDataSource ds = new ByteArrayDataSource(bytes, "image/jpeg");
			String fileName = "отпуск.jpg";

			ds.setName(fileName);
			filesDS.add(ds);
			
			
			mailer.sent(Content.HTML, props, props.getProperty("user.name"), props.getProperty("user.password"),
					props.getProperty("mail.from"), "Деловодство", "vassil@lirex.com", "test 12", "<p><b>This text is bold</b></p>",
					filesDS);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail();
		
		}
	}
	
}

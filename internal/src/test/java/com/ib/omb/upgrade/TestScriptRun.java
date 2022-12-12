package com.ib.omb.upgrade;

import static org.junit.Assert.fail;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ib.omb.system.SystemData;
import com.ib.omb.system.VersionComparator;
import com.ib.omb.upgrade.DocuScriptRun;
import com.ib.omb.upgrade.DocuScriptRun.Command;
import com.ib.system.db.JPA;
import com.ib.system.utils.FileUtils;
import com.ib.system.utils.StringUtils;



public class TestScriptRun {

	private static SystemData sd;

	/** @throws Exception */
	@BeforeClass
	public static void setUp() throws Exception {
		sd = new SystemData();
	}
	
	/**
	 * теста само на парсването на файла и правене на команди за изпълнение
	 */
	@Test
	public void testCreateCommands() {
		try {

			URL resource = getClass().getResource("/com/ib/docu/upgrade/test.0.00.sql");
			if (resource != null) {
				String fileName = resource.getFile();

				List<Command> commands = new DocuScriptRun().createCommands(fileName);

				printResult(commands);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * тест на изпълнението на упдграде - !!! изпълнява в БД и трябва да се внимава какво се пуска като файл
	 */
	@Test
	public void testUpgrade() {
		try {

			URL resource = getClass().getResource("/com/ib/docu/upgrade/table_test_upgrade.sql");
			if (resource != null) {
				String fileName = resource.getFile();

				List<Command> commands = new DocuScriptRun().upgrade("1.03", fileName, sd);

				printResult(commands);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	/**
	 * @param commands
	 */
	private void printResult(List<Command> commands) {
		for (int i = 0; i < commands.size(); i++) {
			Command command = commands.get(i);
			System.out.println("----------------------------------------------------------------------------------------------------------------------");

			System.out.println("Номер:" + (i + 1));

			if (command.getSql() != null || command.isCommit()) { // долното е приложимо само за такива
				System.out.println("Изпълнена: " + (command.isCompleted() ? "ДА" : "НЕ"));
				System.out.println("Грешка: " + (command.getError() != null ? command.getError() : "-"));
			}

			if (command.getComment() != null) {
				System.out.println(" > COMMENT: " + command.getComment());
			}

			if (command.getSql() != null) {
				System.out.println(" > SQL: " + command.getSql());

			} else if (command.getUserAction() != null) {
				System.out.println(" > USER_ACTION: " + command.getUserAction());

			} else if (command.isCommit()) {
				System.out.println(" > COMMIT");
			}
			System.out.println("----------------------------------------------------------------------------------------------------------------------");
		}
	}

//	@Test
	public void testParseFile() {
		try {
			
			String curVersion = "2.01";
			
			
			
			
			String testDir = "D:\\_tests"; 
			
			File dir = new File(testDir);
			if (! dir.isDirectory()) {
				return;
			}
			File[] allF = dir.listFiles();
			List<String> fileNames = new ArrayList<String>();
			for (File f : allF) {
				if (f.exists() && !f.isDirectory()) {
					fileNames.add(f.getName());
				}
			}
			
			Collections.sort(fileNames, new VersionComparator());
			
					
			boolean found = false;		
			for (String s : fileNames) {
				System.out.print(s);
				if (s.equalsIgnoreCase(curVersion+".sql")){
					found = true;
					System.out.println("\t\tCurrent Version");
					continue;
				}
				if (found) {
					System.out.println("\t\tRunning Script");
					String err = excuteFile(testDir + "\\" + s);
					if (err != null) {
						System.out.println("ГРЕШКААААААААААААААААААА!!!!!!!!!!!!!!!!!");
						System.out.println(err);
						break;
					}
				}else {
					System.out.println("\t\tOld Version");
				}
				
				
				
			}
			
			
			
			
//					
		} catch (Exception e) {			
			e.printStackTrace();
			fail();
		}
	}
	
	
	public static String excuteFile(String fileName) throws Exception {
		
		try {
			byte[] bytes = FileUtils.getBytesFromFile(new File(fileName));
			
			
			String script = new String(bytes);
			
			//Има шанс файла да дойде без LF, за това го докарваме винаги до това
			script = script.replace("\n", "");
			script = script.replace("\r", " \r");
			
			//Махаме коментарите
			int commIndex = script.indexOf("--");
			while ( commIndex != -1) {
				int lineIndex = script.indexOf("\r", commIndex);
				String s = script.substring(commIndex, lineIndex);
				//System.out.println(s);
				script = script.replace(s, "");
				commIndex = script.indexOf("--");
			}
			
			//Ако е генериран с GO, го сменяме с ;
			script = script.replace("\rGO", ";");
			
			//System.out.println(script);
			
			String[] allSqls = script.split(";");
			
			JPA.getUtil().begin();
			String errorTrace = null;
			for (String sqlString : allSqls) {
				
				if (sqlString == null || sqlString.trim().isEmpty()) {
					continue;
				}
				
				sqlString = sqlString.replace("\r", "");
				//System.out.println(sqlString.trim());
				
				try {
					JPA.getUtil().getEntityManager().createNativeQuery(sqlString).executeUpdate() ;					
				} catch (Exception e) {
					errorTrace = "SQLString: " + sqlString + "\r\n " + StringUtils.stack2string(e);
					break;
				}
				
				
			}
			
			if (errorTrace != null) {
				JPA.getUtil().rollback();
				return errorTrace;
			}else {
				//Всичко е минало - комитваме 
				JPA.getUtil().commit();
			}
		} catch (Exception e) {
			//LOGGER
			throw e;
		} 
		
		return null;
	}
	
}

package com.ib.omb.upgrade;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.system.SystemData;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.utils.StringUtils;

/**
 * Клас, който трябва да осигури upgrade на версията на системата от страната на БД.
 */
public class DocuScriptRun {

	/** */
	public static class Command {
		private String comment; // коментар към командата

		// от долу са трите вариянта на команди
		private String	sql;		// заявката, която трябва да се изпълни (може да има коментар)
		private String	userAction;	// трябва да се каже нещо на потребителя, което той трябва да свърши (може да има коментар)
		private boolean	commit;		// ако се вдигне този флаг, значи това е команда, която е само за комит

		private boolean	completed;	// изпълнена. има смисъл само за sql и commit
		private String	error;		// грешка от БД. има смисъл само за sql и commit

		/** @return the comment */
		public String getComment() {
			return this.comment;
		}

		/** @return the error */
		public String getError() {
			return this.error;
		}

		/** @return the sql */
		public String getSql() {
			return this.sql;
		}

		/** @return the userAction */
		public String getUserAction() {
			return this.userAction;
		}

		/** @return the commit */
		public boolean isCommit() {
			return this.commit;
		}

		/** @return the completed */
		public boolean isCompleted() {
			return this.completed;
		}

		/** */
		@Override
		public String toString() {
			return "Command [comment=" + this.comment + ", sql=" + this.sql + ", userAction=" + this.userAction + ", commit=" + this.commit + ", completed=" + this.completed + "]";
		}

		/** @param comment the comment to set */
		void setComment(String comment) {
			this.comment = comment;
		}

		/** @param commit the commit to set */
		void setCommit(boolean commit) {
			this.commit = commit;
		}

		/** @param completed the completed to set */
		void setCompleted(boolean completed) {
			this.completed = completed;
		}

		/** @param error the error to set */
		void setError(String error) {
			this.error = error;
		}

		/** @param sql the sql to set */
		void setSql(String sql) {
			this.sql = sql;
		}

		/** @param userAction the userAction to set */
		void setUserAction(String userAction) {
			this.userAction = userAction;
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(DocuScriptRun.class);

	/**
	 * Изпъпнява скриптовете за upgrade в БД. Маркира версията на системата в БД.<br>
	 * Операцията е успешна ако всички команди са {@link Command#isCompleted()} == true.<br>
	 * При първата която {@link Command#isCompleted()} == false трябва да има и причината в поле {@link Command#getError()}
	 *
	 * @param version
	 * @param fileName
	 * @param sd
	 * @return списък с командите, като се знае коя е изпълнена и коя не
	 * @throws IOException
	 */
	public List<Command> upgrade(String version, String fileName, SystemData sd) throws IOException {
		List<Command> commands = createCommands(fileName);

		Command command = null;
		try { // време е да се пуска към БД

			// ще се добавят и изпълнят две нови команди за промяна на версията

			command = new Command();
			command.setComment("Зачистване на старата версия на системата за БД");
			command.setSql("DELETE FROM VERSION_TABLE");
			commands.add(command);

			command = new Command(); // запис на новата версия
			command.setComment("Преминаване към версия " + version);
			command.setSql("INSERT INTO VERSION_TABLE(CURRENT_VERSION, UPGRADE_TIME) VALUES('" + version + "', " //
				+ DialectConstructor.convertDateToSQLString(JPA.getUtil().getDbVendorName(), new Date()) + ")");
			commands.add(command);

			JPA.getUtil().begin();

			for (int i = 0; i < commands.size(); i++) {
				command = commands.get(i);

				if (command.getSql() != null) { // това отива към БД

					JPA.getUtil().getEntityManager().createNativeQuery(command.getSql()).executeUpdate();

					command.setCompleted(true);

				} else if (command.isCommit()) { // само се прави комит
					JPA.getUtil().commit();
					JPA.getUtil().begin();

					command.setCompleted(true);
				}
			}

			JPA.getUtil().commit();

			sd.setDbVersion(version);

		} catch (Exception e) {
			JPA.getUtil().rollback();

			if (command != null) { // грешката се запазва в командата, при чието изпълнение е гръмнало
				command.setError(StringUtils.stack2string(e));
			}
			LOGGER.error("Грешка при изълнение на команда: " + command, e);

		} finally {
			JPA.getUtil().closeConnection();
		}
		return commands;
	}

	/**
	 * Прави команди от файла
	 *
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	List<Command> createCommands(String fileName) throws IOException {
		List<String> lines = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
			String line;
			while ((line = br.readLine()) != null) {
				lines.add(line.trim()); // може и тука да се вкара долната логика по правенето на команди, но има вероятност да
										// трябва да се гледа следващ ред и малко ще стане кофти тука.
			}
		} catch (IOException e) {
			LOGGER.error("Грешка при изчитане на файл: {}", fileName);
			throw e;
		}
		// надолу в lines всички са трим-нати като празните редове си остават!!!

		List<Command> commands = new ArrayList<>();

		Command command = new Command();
		boolean userAction = false; // ако се вдигне флаг значи сме в таг с потребителски команди IB_BEGIN_UACTION/IB_END_UACTION

		int i = 0;
		while (i < lines.size()) {
			String line = lines.get(i);
			i++;

			if (line.isEmpty()) {
				continue;
			}
			boolean ready = false;

			if (!userAction && line.indexOf("IB_BEGIN_UACTION") != -1) {
				userAction = true; // влизаме
				line = ""; // тои ред не трябва да се показва в резултата
			}
			if (userAction && line.indexOf("IB_END_UACTION") != -1) {
				userAction = false; // излизаме
				line = ""; // тои ред не трябва да се показва в резултата
			}

			if (line.indexOf("IB_COMMIT") != -1) { // тука е най лесно, защото всичко е на един ред
				command.setCommit(true);
				ready = true;

			} else if (line.startsWith("--")) { // ред с коментар
				line = line.substring(2, line.length()).trim();

				if (command.getComment() == null) {
					command.setComment(line);
				} else {
					command.setComment(command.getComment() + System.lineSeparator() + line);
				}
				if (i < lines.size() && lines.get(i).isEmpty()) { // ако следващия ред след коментара е празен смятам че текущата
																	// команда е самo коментар и е готово
					ready = true;
				}

			} else if (userAction) { // потребителска команда

				if (i < lines.size() && (lines.get(i).isEmpty() || lines.get(i).indexOf("--") != -1)) { // в последния ред от
																										// командата сме
					ready = true;
				}
				if (line.length() > 0) {
					if (command.getUserAction() == null) {
						command.setUserAction(line);
					} else {
						command.setUserAction(command.getUserAction() + System.lineSeparator() + line);
					}
				}

			} else { // остава да е ЧАСТ от sql заявка

				if (line.endsWith(";")) { // край на заявка
					ready = true;
					line = line.substring(0, line.length() - 1); // за да отпадне ";" от края на реда

				} else if (line.equalsIgnoreCase("GO")) { // това също сме смята че приключва заявката
					ready = command.getSql() != null; // и то само ако има какво да се приключи
					line = ""; // и не трябва да се пише GO-то
				}

				if (line.length() > 0) {
					if (command.getSql() == null) {
						command.setSql(line);
					} else {
						command.setSql(command.getSql() + " " + line);
					}
				}
			}

			if (ready) {
				commands.add(command);
				command = new Command();
			}
		}
		return commands;
	}
}
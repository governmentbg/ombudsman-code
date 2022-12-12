package com.ib.omb.transform;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.persistence.Query;

import org.hibernate.Session;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.StandardBasicTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;

/**
 * Стартира трансфера
 *
 * @author belev
 */
public class T0Start {

	private static final Logger LOGGER = LoggerFactory.getLogger(T0Start.class);

	/**  */
	static final String	SRC_REG		= "reg";
	/**  */
	static final String	SRC_EDSD	= "edsd";
	/**  */
	static final String	SRC_MSSQL	= "mssql";

	/**  */
	static final Integer	REGISTRATURA			= 1;
	/**  */
	static final int		REGISTER_JALBI			= 1;
	/**  */
	static final int		REGISTER_JALBI_DOC		= 2;
	/**  */
	static final int		REGISTER_DELOVODNA_DOC	= 3;
	/**  */
	static final int		REGISTER_JALBI_OLD		= -1;

	/** Неидентифициран нарушител */
	static final Integer NAR_UNIDENTIFIED = 1000;

	/** tConfig.properties - каквото трябва да има */
	private static final Properties CONFIG;

	static {
		CONFIG = new Properties();

		try (InputStream resource = Thread.currentThread().getContextClassLoader().getResourceAsStream("tConfig.properties")) {

			CONFIG.load(resource);

		} catch (Exception e) {
			LOGGER.error("Грешка при конфигуриране! -> " + e.getMessage(), e);
//			e.printStackTrace();

			System.exit(0);
		}
	}

	/**
	 * Kонфигурационните параметри са в tConfig.properties
	 *
	 * @param args
	 */
	public static void main(String[] args) {
		boolean postgreMysqlAvailable = true; // приемам че винаги е налична само ако не се изключи с false
		if ("false".equals(CONFIG.getProperty("POSTGRE_MYSQL_AVAILABLE"))) {
			postgreMysqlAvailable = false;
		}

		boolean mssqlAvailable = true; // приемам че винаги е налична само ако не се изключи с false
		if ("false".equals(CONFIG.getProperty("MSSQL_AVAILABLE"))) {
			mssqlAvailable = false;
		}

		boolean filingsystemAvailable = true; // приемам че винаги е налична само ако не се изключи с false
		if ("false".equals(CONFIG.getProperty("FILINGSYSTEM_AVAILABLE"))) {
			filingsystemAvailable = false;
		}

		LOGGER.info("==================== sourceReg_DB ====================");
		JPA sourceReg = postgreMysqlAvailable ? JPA.getUtil("sourceReg") : null; // БД източник Регистър жалби на Омбудсмана
		LOGGER.info("");

		LOGGER.info("==================== sourceEdsd_DB ====================");
		JPA sourceEdsd = postgreMysqlAvailable ? JPA.getUtil("sourceEdsd") : null; // БД източник Деловодна система на Омбудсмана
		LOGGER.info("");

		LOGGER.info("==================== sourceMssql_DB ====================");
		JPA sourceMssql = mssqlAvailable ? JPA.getUtil("sourceMssql") : null; // БД източник Стар Регистър жалби/Деловодна система
																				// на Омбудсмана
		LOGGER.info("");

		LOGGER.info("==================== dest_DB ====================");
		JPA dest = JPA.getUtil("dest"); // БД на нашата система
		LOGGER.info("");

		LOGGER.info("==================== config params start ====================");
		Enumeration<?> enums = CONFIG.propertyNames();
		while (enums.hasMoreElements()) {
			String key = (String) enums.nextElement();
			String value = CONFIG.getProperty(key);
			LOGGER.info(key + "=" + value);
		}
		LOGGER.info("==================== config params end   ====================");


		boolean skipYN = args != null && args.length > 0 && "skipYN".equalsIgnoreCase(args[0]);
		if (skipYN) {
			LOGGER.info(">>> check (y/n) is skipped <<<");
			LOGGER.info(">>> Transfer is started ... ");

		} else {

		try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name())) {
			LOGGER.info("");
			String line;
			do {
				LOGGER.info(">>> Are you sure you want to start with this configuration? (y/n)");
				line = scanner.nextLine();

			} while (line == null || line.trim().length() == 0);

			if ("Y".equalsIgnoreCase(line.trim())) {
				LOGGER.info(">>> Transfer is started ... ");
			} else {
				LOGGER.info(">>> Transfer is stopped!");
				System.exit(-1);
			}
		}

		}

		try {
			Date transferDate = DateUtils.parse(CONFIG.getProperty("TRANSFER_DATE"));
			int transferUser = Integer.parseInt(CONFIG.getProperty("TRANSFER_USER"));

			int codeNapusnali = Integer.parseInt(CONFIG.getProperty("ADM_STRUCT_CODE_NAPUSNALI"));
			Date dateDoNapusnali = DateUtils.parse(CONFIG.getProperty("ADM_STRUCT_DATE_NAPUSNALI"));

			String filingsystemFilesPath = CONFIG.getProperty("FILINGSYSTEM_FILES_PATH");
			String filingsystemFileSeparator = CONFIG.getProperty("FILINGSYSTEM_FILE_SEPARATOR");

			if (postgreMysqlAvailable) {

				closeAllConnections(sourceReg, sourceEdsd, sourceMssql, dest);
				LOGGER.info("");
				LOGGER.info("==================== Transfer T1Referents START ====================");
				T1Referents referents = new T1Referents(transferUser, transferDate, codeNapusnali, dateDoNapusnali);
				referents.transfer(sourceReg, sourceEdsd, dest);
				LOGGER.info("==================== Transfer T1Referents END   ====================");

				closeAllConnections(sourceReg, sourceEdsd, sourceMssql, dest);
				LOGGER.info("");
				LOGGER.info("==================== Transfer T2RegisterJalbi START ====================");
				T2RegisterJalbi registerJalbi = new T2RegisterJalbi(transferUser);
				registerJalbi.transfer(sourceReg, dest);
				LOGGER.info("==================== Transfer T2RegisterJalbi END   ====================");

				closeAllConnections(sourceReg, sourceEdsd, sourceMssql, dest);
				LOGGER.info("");
				LOGGER.info("==================== Transfer T3Delovodna START ====================");
				T3Delovodna delovodna = new T3Delovodna(transferUser, transferDate);
				delovodna.transfer(sourceEdsd, dest);
				LOGGER.info("==================== Transfer T3Delovodna END   ====================");

				closeAllConnections(sourceReg, sourceEdsd, sourceMssql, dest);
				LOGGER.info("");
				LOGGER.info("==================== Transfer T4Clear START ====================");
				T4Clear clear = new T4Clear();
				clear.transferClear(dest);
				LOGGER.info("==================== Transfer T4Clear END   ====================");
			}

			if (mssqlAvailable) {

				closeAllConnections(sourceReg, sourceEdsd, sourceMssql, dest);
				LOGGER.info("");
				LOGGER.info("==================== Transfer T6ReferentsMssql START ====================");
				T6ReferentsMssql referentsMssql = new T6ReferentsMssql(transferUser, codeNapusnali, dateDoNapusnali);
				referentsMssql.transfer(sourceMssql, dest);
				LOGGER.info("==================== Transfer T6ReferentsMssql END   ====================");

				closeAllConnections(sourceReg, sourceEdsd, sourceMssql, dest);
				LOGGER.info("");
				LOGGER.info("==================== Transfer T7DocsMssql START ====================");
				T7DocsMssql docsMssql = new T7DocsMssql(transferUser);
				docsMssql.transfer(sourceMssql, dest);
				LOGGER.info("==================== Transfer T7DocsMssql END   ====================");

				closeAllConnections(sourceReg, sourceEdsd, sourceMssql, dest);
				LOGGER.info("");
				LOGGER.info("==================== Transfer T8FilesMssql START ====================");
				T8FilesMssql filesMssql = new T8FilesMssql();
				filesMssql.transfer(sourceMssql, dest);
				LOGGER.info("==================== Transfer T8FilesMssql END   ====================");
			}

			if (filingsystemAvailable) {

				File folder = new File(filingsystemFilesPath + "806885");
				List<String> currentJidFiles = Arrays.asList(folder.list());

				if (currentJidFiles.contains("светослав.zip")) {
					System.out.println("Syso: светослав.zip OK");
					LOGGER.info("светослав.zip OK");
				} else {
					System.out.println("Syso: светослав.zip NOT_FOUND");
					LOGGER.error("светослав.zip NOT_FOUND");
				}

				closeAllConnections(sourceReg, sourceEdsd, sourceMssql, dest);
				LOGGER.info("");
				LOGGER.info("==================== Transfer T5FilesRegister START ====================");
				T5FilesRegister filesRegister = new T5FilesRegister(filingsystemFilesPath.trim(), filingsystemFileSeparator.trim());
				filesRegister.transfer(dest);
				LOGGER.info("==================== Transfer T5FilesRegister END   ====================");
			}

			LOGGER.info("");
			LOGGER.info(">>> Transfer is finished.");

		} catch (Exception e) {
			if (dest != null) {
				dest.rollback();
			}

			LOGGER.error("Грешка при трансфер! -> " + e.getMessage(), e);
//			e.printStackTrace();

		} finally {
			closeAllConnections(sourceReg, sourceEdsd, sourceMssql, dest);
		}
	}

	/**
	 * Подговя заявка за правене на SEQUENCE
	 *
	 * @param dialect
	 * @param sequence
	 * @param startFrom
	 * @return
	 */
	static String createSequenceQuery(String dialect, String sequence, Number startFrom) {
		if (dialect == null) {
			return null;
		}

		String rezult = null;
		if (dialect.indexOf("ORACLE") != -1) {
			rezult = "CREATE SEQUENCE " + sequence + " INCREMENT BY 1 START WITH " + startFrom + " MAXVALUE 2147483647 MINVALUE 0 NOCYCLE NOCACHE NOORDER";
		} else if (dialect.indexOf("INFORMIX") != -1) {
			rezult = ""; // createSequenceQuery-INFORMIX
		} else if (dialect.indexOf("SQLSERVER") != -1) {
			rezult = "CREATE SEQUENCE " + sequence + " AS INT START WITH " + startFrom + " INCREMENT BY 1 MINVALUE 0 MAXVALUE 2147483647 NO CYCLE CACHE 20";
		} else if (dialect.indexOf("POSTGRESQL") != -1) {
			rezult = "CREATE SEQUENCE " + sequence + " INCREMENT BY 1 MINVALUE 0 MAXVALUE 2147483647 START WITH " + startFrom + " CACHE 1 NO CYCLE";
		}
		return rezult;
	}

	/**
	 * Изтрива таблица, в която се предполага че може да има много редове.
	 *
	 * @param jpaTo
	 * @param tableName
	 * @param idCol
	 * @return
	 * @throws DbErrorException
	 */
	static int deleteBigTable(JPA jpaTo, String tableName, String idCol, String and) throws DbErrorException {
		int param = 1000; // това е числото над, което смятам че таблицата ще се трие на части

		String appendWhere = and != null ? " where " + and : "";
		String appendAnd = and != null ? " and " + and : "";

		// първо намирам броя на редовете,мин,макс в таблицата
		@SuppressWarnings("unchecked")
		List<Object[]> stat = jpaTo.getEntityManager().createNativeQuery( //
			"select count(*) CNT, min(" + idCol + ") MIN_ID, max(" + idCol + ") MAX_ID from " + tableName + appendWhere) //
			.getResultList();

		int cnt = ((Number) stat.get(0)[0]).intValue();

		if (cnt < param) { // приемам че са малко и директно се изтриват в една транзакция

			jpaTo.begin();
			int deleted = jpaTo.getEntityManager().createNativeQuery("delete from " + tableName + appendWhere).executeUpdate();
			jpaTo.commit();

			return deleted;
		}

		int min = ((Number) stat.get(0)[1]).intValue();
		int max = ((Number) stat.get(0)[2]).intValue();

		int deleted = 0;
		Query query = jpaTo.getEntityManager().createNativeQuery( //
			"delete from " + tableName + " where " + idCol + " >= ?1 and " + idCol + " <= ?2" + appendAnd);

		do { // трият се на порции в отделни транзакции

			int next = min + param;

			jpaTo.begin();
			deleted += query.setParameter(1, min).setParameter(2, next).executeUpdate();
			jpaTo.commit();

			min = next;

		} while (min < max);

		return deleted;
	}

	/**
	 * Дава мап, през който може да се прекодират само стойности от класификации
	 */
	static Map<Integer, Integer> findDecodeClassifMap(JPA jpa, Integer codeClassif, String src) {
		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = jpa.getEntityManager().createNativeQuery( //
			"select src_id, dest_id from transfer_table where code_classif = ?1 and src = ?2") //
			.setParameter(1, codeClassif).setParameter(2, src).getResultStream();

		Map<Integer, Integer> map = new HashMap<>();
		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			int key = ((Number) row[0]).intValue();
			int value = ((Number) row[1]).intValue();

			if (map.containsKey(key)) {
				LOGGER.warn("findDecodeClassifMap (codeClassif=" + codeClassif + "; src=" + src + ") duplicate key=" + key);
			}
			map.put(key, value);
		}
		stream.close();

		return map;
	}

	/**
	 * Дава мап, през който може да се прекодират обекти в зависимост от подадения код на обект
	 */
	static Map<Integer, Integer> findDecodeObjectMap(JPA jpa, Integer codeObject, String src) {
		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = jpa.getEntityManager().createNativeQuery( //
			"select src_id, dest_id from transfer_table where code_object = ?1 and src = ?2") //
			.setParameter(1, codeObject).setParameter(2, src).getResultStream();

		Map<Integer, Integer> map = new HashMap<>();
		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			int key = ((Number) row[0]).intValue();
			int value = ((Number) row[1]).intValue();

			if (map.containsKey(key)) {
				LOGGER.warn("findDecodeObjectMap (codeObject=" + codeObject + "; src=" + src + ") duplicate key=" + key);
			}
			map.put(key, value);
		}
		stream.close();

		return map;
	}

	/**
	 * Сетва правилно полето в зависимост от вида на БД източник и БД сорс
	 *
	 * @param jpaTo
	 * @param blobTo
	 * @param query
	 * @param index
	 * @param value
	 * @throws SQLException
	 * @throws DbErrorException
	 */
	static void setBinaryQueryParam(JPA jpaTo, boolean blobTo, Query query, int index, Object value) throws SQLException, DbErrorException {
		if (value instanceof String) {
			value = ((String) value).getBytes();
		}

		if (blobTo) { // трябва да се сетне BLOB

			Blob blob = null;
			if (value == null) { // остава си null

			} else if (value instanceof Blob) { // имаме го
				blob = (Blob) value;

			} else if (value instanceof byte[]) { // трябва да си го направим
				Session session = jpaTo.getEntityManager().unwrap(Session.class);
				blob = session.getLobHelper().createBlob((byte[]) value);

			} else {
				throw new DbErrorException("setBinaryQueryParam:value=" + value + ", type=" + value.getClass().getName());
			}

			if (blob != null) { // ако е много мегабайти да се логне поне да се знае що се мота процеса
				double mb = (double) blob.length() / (1024 * 1024);
				if (mb > 15) {
					mb = Math.round(mb * 100.0) / 100.0; // закръгление
					LOGGER.info("  !BLOB:" + mb + "mb transfer!");
				}
			}

			query.setParameter(index, new TypedParameterValue(StandardBasicTypes.BLOB, blob)); // SET

		} else { // трябва да се сетне binary

			byte[] bytes = null;
			if (value == null) { // остава си null

			} else if (value instanceof byte[]) { // имаме си го готов
				bytes = (byte[]) value;

			} else if (value instanceof Blob) { // изчитам го от blob-а
				Blob blob = (Blob) value;
				bytes = blob.getBytes(1, (int) blob.length());

			} else {
				throw new DbErrorException("setBinaryQueryParam:value" + value + ", type=" + value.getClass().getName());
			}

			if (bytes != null) { // ако е много мегабайти да се логне поне да се знае що се мота процеса
				double mb = (double) bytes.length / (1024 * 1024);
				if (mb > 15) {
					mb = Math.round(mb * 100.0) / 100.0; // закръгление
					LOGGER.info("  !BINARY:" + mb + "mb transfer!");
				}
			}

			query.setParameter(index, new TypedParameterValue(StandardBasicTypes.BINARY, bytes)); // SET
		}
	}

	static void wait(int sec) {
		LOGGER.info("	start waiting for " + sec + " seconds ...");
		try {
			TimeUnit.SECONDS.sleep(sec);
//			Thread.sleep(sec * 1000);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		LOGGER.info("	end waiting for " + sec + " seconds.");
	}

	/** */
	private static void closeAllConnections(JPA sourceReg, JPA sourceEdsd, JPA sourceMssql, JPA dest) {
		if (sourceEdsd != null) {
			sourceEdsd.closeConnection();
		}
		if (sourceReg != null) {
			sourceReg.closeConnection();
		}
		if (sourceMssql != null) {
			sourceMssql.closeConnection();
		}
		if (dest != null) {
			dest.closeConnection();
		}
	}
}

package com.ib.omb.transform;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.persistence.Query;

import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.StandardBasicTypes;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * Трансфер на жалби и документи по жалби
 *
 * @author belev
 */
public class T2RegisterJalbi {

	private static final Logger LOGGER = LoggerFactory.getLogger(T2RegisterJalbi.class);

	/** няколко го забивам, защото има грешки в имената при тях и не се откриват */
	private static Map<Integer, Integer> EKATTE_FIXED = new HashMap<>();

	/**  */
	private static Map<String, Integer> SAST_MAP = new HashMap<>();

	static {
		SAST_MAP.put("A", OmbConstants.CODE_ZNACHENIE_JALBA_SAST_FILED);
		SAST_MAP.put("R", OmbConstants.CODE_ZNACHENIE_JALBA_SAST_RAZPR);
		SAST_MAP.put("W", OmbConstants.CODE_ZNACHENIE_JALBA_SAST_CHECK);
		SAST_MAP.put("Z", OmbConstants.CODE_ZNACHENIE_JALBA_SAST_REOPENED);
		SAST_MAP.put("F", OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED);

		EKATTE_FIXED.put(3168, 49432);
		EKATTE_FIXED.put(3632, 59224);
		EKATTE_FIXED.put(5282, 14489);
		EKATTE_FIXED.put(5283, 68708);
		EKATTE_FIXED.put(4637, 70648);
		EKATTE_FIXED.put(5078, 7079);
		EKATTE_FIXED.put(4061, 7079);
		EKATTE_FIXED.put(3002, 47559);
		EKATTE_FIXED.put(2893, 46396);
	}

	private int transferUser;

	/**
	 * @param transferUser
	 */
	public T2RegisterJalbi(int transferUser) {
		this.transferUser = transferUser;
	}

	/**
	 * Методът
	 *
	 * @param sourceReg
	 * @param dest
	 * @throws BaseException
	 * @throws SQLException
	 */
	public void transfer(JPA sourceReg, JPA dest) throws BaseException, SQLException {
		@SuppressWarnings("unchecked")
		List<Object[]> rows = dest.getEntityManager().createNativeQuery( //
			"select clazz, start_time, end_time from transfer_process where clazz = ?1") //
			.setParameter(1, getClass().getSimpleName()).getResultList();

		if (rows.isEmpty()) { // първо пускане

			dropIndexes(dest); // има няколко индекса, които махам и после ще се направят пак

			clearRelatedData(dest); // зачистване на всичко свързано с този клас

			insertRegisters(dest); // малко регистри

			dest.begin();
			dest.getEntityManager().createNativeQuery( //
				"insert into transfer_process (clazz, start_time) values (?1, ?2)") //
				.setParameter(1, getClass().getSimpleName()).setParameter(2, new Date()) //
				.executeUpdate();
			dest.commit();

		} else if (rows.get(0)[2] != null) {
			LOGGER.info("   ! finished !");
			return; // значи си е свършил работата
		}

		T0Start.wait(5);
		// първо тябва да се разбере от къде се започва/продължава -- за жалбите ИД-то съвпада
		Number maxJalba = (Number) dest.getEntityManager().createNativeQuery("select max(doc_id) from doc where register_id = ?1") //
			.setParameter(1, T0Start.REGISTER_JALBI).getResultList().get(0);
		int fromJalbaId = maxJalba != null ? maxJalba.intValue() : 0;

		int rnPored = loadJalbi(sourceReg, dest, fromJalbaId); // трансфер на жалби

		if (rnPored > 0) { // ако в текущото пускане нещо е минало значи оправям брояча
			dest.begin();
			dest.getEntityManager().createNativeQuery("update doc_vid_settings set act_nomer = ? where register_id = ?") //
				.setParameter(1, rnPored + 1).setParameter(2, T0Start.REGISTER_JALBI)//
				.executeUpdate();
			dest.commit();
		}

		T0Start.wait(5);
		// първо тябва да се разбере от къде се започва/продължава -- rn_pored винаги ще съвпада с jalbi_work.id от базата на
		// жалбите
		Number maxJalbaWork = (Number) dest.getEntityManager().createNativeQuery("select max(rn_pored) from doc where register_id = ?1") //
			.setParameter(1, T0Start.REGISTER_JALBI_DOC).getResultList().get(0);
		int fromJalbaWorkId = maxJalbaWork != null ? maxJalbaWork.intValue() : 0;

		loadJalbiWork(sourceReg, dest, fromJalbaWorkId);

		recreateSequences(dest);

		// край!
		dest.begin();
		dest.getEntityManager().createNativeQuery( //
			"update transfer_process set end_time = ?1 where clazz = ?2"). //
			setParameter(1, new Date()).setParameter(2, getClass().getSimpleName()) //
			.executeUpdate();
		dest.commit();
	}

	/**
	 * Изтрива документи и всички свързани таблици, за да се започне зареждането
	 */
	void clearRelatedData(JPA dest) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start clear related data");

		int cnt = T0Start.deleteBigTable(dest, "DELO_DOC", "id", null);
		LOGGER.info("   deleted " + cnt + " rows from table DELO_DOC");

		cnt = T0Start.deleteBigTable(dest, "DELO", "delo_id", null);
		LOGGER.info("   deleted " + cnt + " rows from table DELO");

		cnt = T0Start.deleteBigTable(dest, "DOC_DVIJ", "id", null);
		LOGGER.info("   deleted " + cnt + " rows from table DOC_DVIJ");

		cnt = T0Start.deleteBigTable(dest, "DOC_JALBA_RESULT", "id", null);
		LOGGER.info("   deleted " + cnt + " rows from table DOC_JALBA_RESULT");

		cnt = T0Start.deleteBigTable(dest, "DOC_REFERENTS", "id", null);
		LOGGER.info("   deleted " + cnt + " rows from table DOC_REFERENTS");

		cnt = T0Start.deleteBigTable(dest, "DOC_SAST_HISTORY", "id", null);
		LOGGER.info("   deleted " + cnt + " rows from table DOC_SAST_HISTORY");

		cnt = T0Start.deleteBigTable(dest, "DOC_EXPERTS", "doc_id", null);
		LOGGER.info("   deleted " + cnt + " rows from table DOC_EXPERTS");

		cnt = T0Start.deleteBigTable(dest, "DOC_JALBA", "doc_id", null);
		LOGGER.info("   deleted " + cnt + " rows from table DOC_JALBA");

		cnt = T0Start.deleteBigTable(dest, "FILE_OBJECTS", "id", null);
		LOGGER.info("   deleted " + cnt + " rows from table FILE_OBJECTS");

		cnt = T0Start.deleteBigTable(dest, "FILES", "file_id", null);
		LOGGER.info("   deleted " + cnt + " rows from table FILES");

		cnt = T0Start.deleteBigTable(dest, "DOC", "doc_id", null);
		LOGGER.info("   deleted " + cnt + " rows from table DOC");

		// трябва да се изредят таблиците и да са в правилна последователност
		String[] tables = new String[] { "DOC_VID_SETTINGS", "REGISTRI" }; // тези за по малки като бройки и може и така

		for (String table : tables) {

			dest.begin();
			cnt = dest.getEntityManager().createNativeQuery("delete from " + table).executeUpdate();
			dest.commit();

			LOGGER.info("   deleted " + cnt + " rows from table " + table);
		}
	}

	/**
	 * Махане на индекси
	 */
	void dropIndexes(JPA dest) {
		LOGGER.info("");
		LOGGER.info("Start drop indexes");

		List<String> list = new ArrayList<>();
		list.add("DROP INDEX delo_registratura_id_rn_delo_delo_year");
		list.add("DROP INDEX doc_registratura_id_rn_doc_doc_date");
		list.add("DROP INDEX doc_referents_doc_id");
		list.add("DROP INDEX doc_rn_doc_upper");
		list.add("DROP INDEX delo_rn_delo_upper");
		list.add("DROP INDEX doc_doc_vid");
		list.add("DROP INDEX doc_doc_date");
		list.add("DROP INDEX delo_delo_date");
		list.add("DROP INDEX doc_dvij_dvij_date");
		list.add("DROP INDEX doc_jalba_sast");
		list.add("DROP INDEX doc_dvij_doc_id");
		list.add("DROP INDEX task_doc_id");
		list.add("DROP INDEX delo_doc_delo_id");
		list.add("DROP INDEX delo_doc_input_doc_id");
		list.add("DROP INDEX doc_sast_history_doc_id");
		list.add("DROP INDEX doc_referents_code_ref");
		list.add("DROP INDEX doc_processed_doc_type");
		list.add("DROP INDEX task_rn_task");
		list.add("DROP INDEX doc_jalba_result_doc_id");

		for (String sql : list) {
			try {
				dest.begin();
				dest.getEntityManager().createNativeQuery(sql).executeUpdate();
				dest.commit();
				LOGGER.info("   " + sql + " -> success");

			} catch (Exception e) {
				dest.rollback();
				LOGGER.warn(e.getMessage());
			}
		}
		LOGGER.info("End drop indexes");
	}

	/**
	 * Зарежда регистрите и х-ки по вид документ свъразни с регистър жалби
	 */
	void insertRegisters(JPA dest) throws DbErrorException {
		Query query = dest.getEntityManager().createNativeQuery( //
			"insert into REGISTRI (REGISTER_ID, REGISTRATURA_ID, REGISTER, REGISTER_TYPE, VALID, COMMON, DOC_TYPE" //
				+ " , PREFIX, ALG, BEG_NOMER, ACT_NOMER, STEP, USER_REG, DATE_REG, sort_nomer)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15)");

		Query queryDocVid = dest.getEntityManager().createNativeQuery( //
			"insert into doc_vid_settings (id, registratura_id, doc_vid, register_id, prefix, beg_nomer, act_nomer, step, create_delo, user_reg, date_reg, members_active)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12)"); //

		dest.begin();

//		Регистър жалби
		query.setParameter(1, T0Start.REGISTER_JALBI); // REGISTER_ID
		query.setParameter(2, T0Start.REGISTRATURA); // REGISTRATURA_ID
		query.setParameter(3, "Регистър жалби"); // REGISTER
		query.setParameter(4, OmbConstants.CODE_ZNACHENIE_REGISTER_DNEV); // REGISTER_TYPE
		query.setParameter(5, SysConstants.CODE_ZNACHENIE_DA); // VALID
		query.setParameter(6, SysConstants.CODE_ZNACHENIE_NE); // COMMON
		query.setParameter(7, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // DOC_TYPE
		query.setParameter(8, null); // PREFIX
		query.setParameter(9, OmbConstants.CODE_ZNACHENIE_ALG_VID_DOC); // ALG
		query.setParameter(10, 1); // BEG_NOMER
		query.setParameter(11, 1); // ACT_NOMER
		query.setParameter(12, 1); // STEP
		query.setParameter(13, this.transferUser); // USER_REG
		query.setParameter(14, DateUtils.systemMinDate()); // DATE_REG
		query.setParameter(15, 1); // sort_nomer
		query.executeUpdate();

//		Мигрирани документи по жалби
		query.setParameter(1, T0Start.REGISTER_JALBI_DOC); // REGISTER_ID
		query.setParameter(2, T0Start.REGISTRATURA); // REGISTRATURA_ID
		query.setParameter(3, "Мигрирани документи по жалби"); // REGISTER
		query.setParameter(4, OmbConstants.CODE_ZNACHENIE_REGISTER_DNEV); // REGISTER_TYPE
		query.setParameter(5, SysConstants.CODE_ZNACHENIE_NE); // VALID
		query.setParameter(6, SysConstants.CODE_ZNACHENIE_NE); // COMMON
		query.setParameter(7, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN); // DOC_TYPE
		query.setParameter(8, null); // PREFIX
		query.setParameter(9, OmbConstants.CODE_ZNACHENIE_ALG_FREE); // ALG
		query.setParameter(10, 1); // BEG_NOMER
		query.setParameter(11, 1); // ACT_NOMER
		query.setParameter(12, 1); // STEP
		query.setParameter(13, this.transferUser); // USER_REG
		query.setParameter(14, DateUtils.systemMinDate()); // DATE_REG
		query.setParameter(15, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // sort_nomer
		query.executeUpdate();

//		х-ки по вид документ Регистър жалби
		queryDocVid.setParameter(1, 1); // id
		queryDocVid.setParameter(2, T0Start.REGISTRATURA); // registratura_id
		queryDocVid.setParameter(3, OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA); // doc_vid
		queryDocVid.setParameter(4, T0Start.REGISTER_JALBI); // register_id
		queryDocVid.setParameter(5, null); // prefix
		queryDocVid.setParameter(6, 1); // beg_nomer
		queryDocVid.setParameter(7, 1); // act_nomer
		queryDocVid.setParameter(8, 1); // step
		queryDocVid.setParameter(9, SysConstants.CODE_ZNACHENIE_DA); // create_delo
		queryDocVid.setParameter(10, this.transferUser); // user_reg
		queryDocVid.setParameter(11, DateUtils.systemMinDate()); // date_reg
		queryDocVid.setParameter(12, SysConstants.CODE_ZNACHENIE_NE); // members_active
		queryDocVid.executeUpdate();

		dest.commit();
	}

	/**
	 * Прехвърля жалбите от Регистър жалби
	 *
	 * @return посления пореден номер
	 */
	int loadJalbi(JPA sourceReg, JPA dest, int fromJalbaId) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start loading jalbi from omb_register...");

		Date minDate = DateUtils.systemMinDate();
		Map<Integer, Integer> correspMap = T0Start.findDecodeObjectMap(dest, OmbConstants.CODE_ZNACHENIE_JOURNAL_REFERENT, T0Start.SRC_REG);
		Map<Integer, Integer> userMap = T0Start.findDecodeObjectMap(dest, Constants.CODE_ZNACHENIE_JOURNAL_USER, T0Start.SRC_REG);
		Map<Integer, Integer> zvenoMap = T0Start.findDecodeClassifMap(dest, Constants.CODE_CLASSIF_ADMIN_STR, T0Start.SRC_REG);

		Map<Integer, Integer> dopustMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_DOPUST, T0Start.SRC_REG);
		Map<Integer, Integer> osnNedopustMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_OSN_NEDOPUST, T0Start.SRC_REG);

		Map<Integer, Integer> jalbaFinMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_JALBA_FIN, T0Start.SRC_REG);
		Map<Integer, Integer> jalbaResMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_JALBA_RES, T0Start.SRC_REG);

		Map<Integer, Integer> zasPravaMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_ZAS_PRAVA, T0Start.SRC_REG);
		Map<Integer, Integer> vidOplMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_VID_OPL, T0Start.SRC_REG);

		Map<String, Integer> ekatteNames = new HashMap<>(); // тука са сглобени текстовете за да се търси код по екатте
		@SuppressWarnings("unchecked")
		List<Object[]> ekatteRows = dest.getEntityManager().createNativeQuery("select ekatte, tvm, ime, obstina_ime, oblast_ime from ekatte_att").getResultList();
		for (Object[] ekatte : ekatteRows) {
			StringBuilder key = new StringBuilder();
			if (ekatte[1] != null) {
				key.append(((String) ekatte[1]).trim());
			}
			if (ekatte[2] != null) {
				key.append(" " + ((String) ekatte[2]).trim());
			}
			if (ekatte[3] != null) {
				key.append(" " + ((String) ekatte[3]).trim());
			}
			if (ekatte[4] != null) {
				key.append(" " + ((String) ekatte[4]).trim());
			}
			ekatteNames.put(key.toString().toUpperCase(), ((Number) ekatte[0]).intValue());
		}
		Map<Integer, Integer> ekatteMap = new HashMap<>(); // това се прави спрямо данните като се гледат по текстове

		Number maxDeloDoc = (Number) dest.getEntityManager().createNativeQuery("select max(id) from delo_doc").getResultList().get(0);
		int deloDocId = maxDeloDoc != null ? maxDeloDoc.intValue() : 0;

		Number maxFile = (Number) dest.getEntityManager().createNativeQuery("select max(file_id) from files").getResultList().get(0);
		int fileId = maxFile != null ? maxFile.intValue() : 0;

		Number maxFileObject = (Number) dest.getEntityManager().createNativeQuery("select max(id) from file_objects").getResultList().get(0);
		int fileObjectId = maxFileObject != null ? maxFileObject.intValue() : 0;

		Number maxJalbaResult = (Number) dest.getEntityManager().createNativeQuery("select max(id) from doc_jalba_result").getResultList().get(0);
		int jalbaResultId = maxJalbaResult != null ? maxJalbaResult.intValue() : 0;

		int insertCnt = 0;
		int rnPored = 0;

		Query queryDoc = dest.getEntityManager().createNativeQuery("insert into doc" //
			+ " (doc_id, registratura_id, register_id, rn_doc, rn_pored, doc_type, doc_vid, doc_date, otnosno" //
			+ ", valid, valid_date, processed, free_access, count_files, competence, user_reg, date_reg, pored_delo, doc_info)" //
			+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19)");

		Query queryDocJalba = dest.getEntityManager().createNativeQuery( //
			"insert into doc_jalba (doc_id, jbp_type, jbp_name, jbp_egn, jbp_ekatte, jbp_post, jbp_addr, jbp_phone, jbp_email" //
				+ ", code_nar, zas_prava, vid_opl, jalba_text, request_text, regist_comment, code_zveno, code_expert" //
				+ ", sast, sast_date, srok, dopust, osn_nedopust, public_visible, fin_method, corruption, inst_check, inst_names, jbp_grj, submit_date" //
				+ ", jbp_hidden, user_last_mod, date_last_mod)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20" //
				+ ", ?21, ?22, ?23, ?24, ?25, ?26, ?27, ?28, ?29, ?30, ?31, ?32)");

		Query queryExpert = dest.getEntityManager().createNativeQuery("insert into doc_experts (doc_id, code_ref) values (?1, ?2)");

		Query queryDelo = dest.getEntityManager().createNativeQuery( //
			"insert into delo (delo_id, registratura_id, init_doc_id, rn_delo, rn_pored, delo_date, delo_type, free_access" //
				+ ", status, status_date, with_section, with_tom, br_tom, max_br_sheets, delo_year, delo_name, user_reg, date_reg)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18)"); //

		Query queryDeloDoc = dest.getEntityManager().createNativeQuery( //
			"insert into delo_doc (id, delo_id, input_doc_id, input_date, section_type, tom_nomer, ekz_nomer, user_reg, date_reg)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)"); //

		Query queryFiles = dest.getEntityManager().createNativeQuery( //
			"insert into files (file_id, filename, content_type, content, user_reg, date_reg) values (?1, ?2, ?3, ?4, ?5, ?6)");

		Query queryFileObject = dest.getEntityManager().createNativeQuery( //
			"insert into file_objects (id, object_id, object_code, file_id, user_reg, date_reg) values (?1, ?2, ?3, ?4, ?5, ?6)");

		Query queryJalbaResult = dest.getEntityManager().createNativeQuery( //
			"insert into doc_jalba_result(id, doc_id, vid_result, date_result) values (?1, ?2, ?3, ?4)");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceReg.getEntityManager().createNativeQuery( //
			"select j.id,j.fname,j.mname,j.lname,j.egn" // 0,1,2,3,4
				+ ",case when j.pk > 100000 then null else j.pk end pk" // 5
				+ ",j.address,c.id city,j.tel,j.email,j.documents,j.com,j.cdate,j.status,j.sdate" // 6,7,8,9,10,11,12,13,14
				+ ",j.uid,j.rdid,j.tnaru,j.exp,j.narushitel,j.txt_jalba,j.korupcia" // 15,16,17,18,19,20,21
				+ ",j.dopustimost,j.fin,j.txt_pub,c.mjasto,c.obshtina,c.oblast,dnar.narushitel dnar_name" // 22,23,24,25,26,27,28
				+ " from jalbi j left outer join cities c on c.id = j.city left outer join narushiteli dnar on dnar.id = j.narushiteld" //
				+ " where j.id > ?1 order by j.id")
			.setParameter(1, fromJalbaId) //
			.setHint(QueryHints.HINT_FETCH_SIZE, "" + Integer.MIN_VALUE) // така трябва за да работи с MySQL
			.getResultStream();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			int jId = ((Number) row[0]).intValue(); // id

			rnPored = jId % 100000; // за брояча в регистъра по вид документ
			String rn = String.valueOf(rnPored);

			Date cdate;
			if (row[12] != null) { // cdate
				cdate = (Date) row[12];
			} else {
				cdate = minDate;
			}

			Integer uid;
			if (row[15] != null) { // uid
				int xi = ((Number) row[15]).intValue();

				uid = userMap.get(xi);
				if (uid == null) {
					uid = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! jalbi.id=" + jId + " ! UNKNOWN uid=" + xi);
				}
			} else {
				uid = this.transferUser;
			}

			String otnosno;
			if (row[20] != null) { // txt_jalba
				otnosno = Jsoup.parse((String) row[20]).wholeText();
			} else {
				otnosno = "";
			}

			String docInfo = null;
			if (row[24] != null) { // txt_pub
				docInfo = Jsoup.parse((String) row[24]).wholeText();
			}

			String documents = SearchUtils.trimToNULL((String) row[10]); // documents
			List<String> fnames = null;
			if (documents != null) {
				fnames = prepareFiles(documents);
				if (fnames == null) {
					LOGGER.warn("  ! jalbi.id=" + jId + " ! ERROR documents=" + documents);
				}
			}

			queryDoc.setParameter(1, jId); // doc_id
			queryDoc.setParameter(2, T0Start.REGISTRATURA); // registratura_id
			queryDoc.setParameter(3, T0Start.REGISTER_JALBI); // register_id
			queryDoc.setParameter(4, rn); // rn_doc
			queryDoc.setParameter(5, rnPored); // rn_pored
			queryDoc.setParameter(6, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN); // doc_type
			queryDoc.setParameter(7, OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA); // doc_vid
			queryDoc.setParameter(8, cdate); // doc_date
			queryDoc.setParameter(9, otnosno); // otnosno
			queryDoc.setParameter(10, OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL); // valid
			queryDoc.setParameter(11, cdate); // valid_date
			queryDoc.setParameter(12, SysConstants.CODE_ZNACHENIE_DA); // processed
			queryDoc.setParameter(13, SysConstants.CODE_ZNACHENIE_DA); // free_access
			queryDoc.setParameter(14, fnames == null ? 0 : fnames.size()); // count_files
			queryDoc.setParameter(15, OmbConstants.CODE_ZNACHENIE_COMPETENCE_OUR); // competence
			queryDoc.setParameter(16, uid); // user_reg
			queryDoc.setParameter(17, cdate); // date_reg
			queryDoc.setParameter(18, 1); // pored_delo
			queryDoc.setParameter(19, docInfo); // doc_info
			queryDoc.executeUpdate();

			StringBuilder jbpName = new StringBuilder();
			if (row[1] != null) { // fname
				jbpName.append(row[1]);
			}
			if (row[2] != null) { // mname
				jbpName.append(" " + row[2]);
			}
			if (row[3] != null) { // lname
				jbpName.append(" " + row[3]);
			}

			Integer ekatte = null;
			if (row[7] != null && row[25] != null) { // city && mjasto
				int xi = ((Number) row[7]).intValue();
				ekatte = ekatteMap.get(xi);

				if (ekatte == null) { // все още не е търсено за това
					ekatte = findEkatte(row, ekatteNames);
					if (ekatte == null) {
						ekatte = -xi; // запазвам оригиналния но с минус
						LOGGER.warn("  ! jalbi.id=" + jId + " ! UNKNOWN city=" + xi);
					} else {
						ekatteMap.put(xi, ekatte);
					}
				}
			}

			Integer codeNar = null;
			if (row[19] != null) { // narushitel
				int xi = ((Number) row[19]).intValue();

				codeNar = correspMap.get(xi);
				if (codeNar == null) {
//					codeNar = -xi; // запазвам оригиналния но с минус
//					LOGGER.info("  ! jalbi.id=" + jId + " ! UNKNOWN narushitel=" + xi);
				}
			}

			Date sdate;
			if (row[14] != null) { // sdate
				sdate = (Date) row[14];
			} else {
				sdate = cdate;
			}

			String registComment = null;
			if (row[11] != null) { // com
				registComment = Jsoup.parse((String) row[11]).wholeText();
			}

			Integer codeZveno = null;
			String rdid = SearchUtils.trimToNULL((String) row[16]); // rdid
			if (rdid != null) {
				Integer xi = calcCodeZveno(rdid); // изчислявам стойността от стринга
				if (xi == null) {
					codeZveno = -100; // има нещо но не го разпознаваме
					LOGGER.warn("  ! jalbi.id=" + jId + " ! ERROR rdid=" + rdid);

				} else { // трябва прекодиране през мапинг звена
					codeZveno = zvenoMap.get(xi);
					if (codeZveno == null) {
						codeZveno = -xi; // запазвам оригиналния но с минус
						LOGGER.warn("  ! jalbi.id=" + jId + " ! UNKNOWN rdid=" + xi);
					}
				}
			}

			List<Integer> codeExpertList = new ArrayList<>();
			String exp = SearchUtils.trimToNULL((String) row[18]); // exp
			if (exp != null) {
				List<Integer> xiList = calcCodeExpertList(exp); // изчислявам стойността от стринга
				if (xiList == null || xiList.isEmpty()) {
					codeExpertList.add(-100); // има нещо но не го разпознаваме
					LOGGER.warn("  ! jalbi.id=" + jId + " ! ERROR exp=" + exp);

				} else { // трябва прекодиране през мапинг потребитеи

					for (Integer xi : xiList) {
						Integer codeExpert = userMap.get(xi);
						if (codeExpert == null) {
							codeExpert = -xi; // запазвам оригиналния но с минус
							LOGGER.warn("  ! jalbi.id=" + jId + " ! UNKNOWN exp=" + xi);
						}
						if (!codeExpertList.contains(codeExpert)) {
							codeExpertList.add(codeExpert);
						}
					}
				}
			}

			Integer sast;
			if (row[13] != null) { // status
				String key = row[13] instanceof Character ? String.valueOf(row[13]) : ((String) row[13]).trim();

				sast = SAST_MAP.get(key);
				if (sast == null) {
					sast = -100;
					LOGGER.warn("  ! jalbi.id=" + jId + " ! UNKNOWN status=" + key);

				} else { // има и логика, която още го намества
					if (!sast.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED) && codeZveno == null) {
						// Ако sast <> 8 (приключена) и няма определено звено, то sast = 1 (заведена)
						sast = OmbConstants.CODE_ZNACHENIE_JALBA_SAST_FILED;

					} else if (sast.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_CHECK) && codeExpertList.isEmpty()) {
						// Ако sast = 6 (проверка) и няма определен водещ експерт, то sast = 5 (разпределена)
						sast = OmbConstants.CODE_ZNACHENIE_JALBA_SAST_RAZPR;
					}
				}
			} else {
				sast = -100;
			}

			Integer corruption = null;
			if (row[21] != null) { // korupcia
				String key = row[21] instanceof Character ? String.valueOf(row[21]) : ((String) row[21]).trim();

				if ("Y".equals(key)) {
					corruption = SysConstants.CODE_ZNACHENIE_DA;
				} else {
					corruption = SysConstants.CODE_ZNACHENIE_NE;
				}
			}

			Integer dopust = null;
			Integer osnNedopust = null;
			if (row[22] != null) { // dopustimost
				int xi = ((Number) row[22]).intValue();

				dopust = dopustMap.get(xi);
				if (dopust == null) {
					dopust = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! jalbi.id=" + jId + " ! UNKNOWN dopustimost=" + xi);
				}

				osnNedopust = osnNedopustMap.get(xi);
				if (osnNedopust == null) {
					osnNedopust = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! jalbi.id=" + jId + " ! UNKNOWN dopustimost=" + xi);

				} else if (osnNedopust.intValue() == 0) {
					osnNedopust = null; // жалбата е допустима !!!
				}
			}

			Date srok = null;
			if (!sast.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED)) {

				long days; // 30 календарни дни след датата на жалбата, ако е допустима или 14 календарни дни, ако е недопустима
				if (osnNedopust != null) {
					days = 14L;
				} else {
					days = 30L;
				}
				long plus = days * 24 * 60 * 60 * 1000;
				srok = new Date(cdate.getTime() + plus);
			}

			Integer finMethod = null;
			Integer vidResult = null;
			if (row[23] != null) { // fin
				int xi = ((Number) row[23]).intValue();

				if (xi != 0) {
					finMethod = jalbaFinMap.get(xi);
					if (finMethod == null) {
						finMethod = -xi; // запазвам оригиналния но с минус
						LOGGER.warn("  ! jalbi.id=" + jId + " ! UNKNOWN fin=" + xi);
					}

					vidResult = jalbaResMap.get(xi);
					if (vidResult == null) {
						vidResult = -xi; // запазвам оригиналния но с минус
						LOGGER.warn("  ! jalbi.id=" + jId + " ! UNKNOWN fin=" + xi);
					}
				}
			}

			String dnarName = SearchUtils.trimToNULL((String) row[28]); // dnar_name
			String jalbaText = "-";
			if (dnarName != null) {
				jalbaText = "Друг нарушител: " + dnarName;
			}

			Integer zasPrava = null;
			Integer vidOpl = null;
			if (row[17] != null) { // tnaru
				int xi = ((Number) row[17]).intValue();

				zasPrava = zasPravaMap.get(xi);
				if (zasPrava == null) {
					zasPrava = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! jalbi.id=" + jId + " ! UNKNOWN tnaru=" + xi);
				}

				vidOpl = vidOplMap.get(xi);
				if (vidOpl == null) {
					vidOpl = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! jalbi.id=" + jId + " ! UNKNOWN tnaru=" + xi);
				}
			}

			queryDocJalba.setParameter(1, jId); // doc_id
			queryDocJalba.setParameter(2, OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL); // jbp_type
			queryDocJalba.setParameter(3, jbpName.toString()); // jbp_name
			queryDocJalba.setParameter(4, row[4] == null ? null : String.valueOf(row[4])); // jbp_egn
			queryDocJalba.setParameter(5, new TypedParameterValue(StandardBasicTypes.INTEGER, ekatte)); // jbp_ekatte
			queryDocJalba.setParameter(6, row[5] == null ? null : String.valueOf(row[5])); // jbp_post
			queryDocJalba.setParameter(7, row[6]); // jbp_addr
			queryDocJalba.setParameter(8, row[8]); // jbp_phone
			queryDocJalba.setParameter(9, row[9]); // jbp_email
			queryDocJalba.setParameter(10, new TypedParameterValue(StandardBasicTypes.INTEGER, codeNar)); // code_nar
			queryDocJalba.setParameter(11, new TypedParameterValue(StandardBasicTypes.INTEGER, zasPrava)); // zas_prava
			queryDocJalba.setParameter(12, new TypedParameterValue(StandardBasicTypes.INTEGER, vidOpl)); // vid_opl
			queryDocJalba.setParameter(13, jalbaText); // jalba_text // Друг нарушител: Името/ narushiteld
			queryDocJalba.setParameter(14, null); // request_text
			queryDocJalba.setParameter(15, registComment); // regist_comment
			queryDocJalba.setParameter(16, new TypedParameterValue(StandardBasicTypes.INTEGER, codeZveno)); // code_zveno
			queryDocJalba.setParameter(17, new TypedParameterValue(StandardBasicTypes.INTEGER, codeExpertList.isEmpty() ? null : codeExpertList.get(0))); // code_expert
			queryDocJalba.setParameter(18, sast); // sast
			queryDocJalba.setParameter(19, sdate); // sast_date
			queryDocJalba.setParameter(20, new TypedParameterValue(StandardBasicTypes.TIMESTAMP, srok)); // srok
			queryDocJalba.setParameter(21, new TypedParameterValue(StandardBasicTypes.INTEGER, dopust)); // dopust
			queryDocJalba.setParameter(22, new TypedParameterValue(StandardBasicTypes.INTEGER, osnNedopust)); // osn_nedopust
			queryDocJalba.setParameter(23, SysConstants.CODE_ZNACHENIE_DA); // public_visible
			queryDocJalba.setParameter(24, new TypedParameterValue(StandardBasicTypes.INTEGER, finMethod)); // fin_method
			queryDocJalba.setParameter(25, new TypedParameterValue(StandardBasicTypes.INTEGER, corruption)); // corruption
			queryDocJalba.setParameter(26, SysConstants.CODE_ZNACHENIE_NE); // inst_check
			queryDocJalba.setParameter(27, null); // inst_names
			queryDocJalba.setParameter(28, 37); // jbp_grj
			queryDocJalba.setParameter(29, cdate); // submit_date
			queryDocJalba.setParameter(30, SysConstants.CODE_ZNACHENIE_NE); // jbp_hidden
			queryDocJalba.setParameter(31, uid); // user_last_mod
			queryDocJalba.setParameter(32, sdate); // date_last_mod
			queryDocJalba.executeUpdate();

			if (codeExpertList.size() > 1) { // имаме и допълнителни
				for (int i = 1; i < codeExpertList.size(); i++) { // първият е отделно
					queryExpert.setParameter(1, jId); // doc_id
					queryExpert.setParameter(2, codeExpertList.get(i)); // code_ref
					queryExpert.executeUpdate();
				}
			}

			GregorianCalendar deloYear = new GregorianCalendar();
			deloYear.setTime(cdate);

			int deloStatus = OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ACTIVE;
			if (sast.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED)) {
				deloStatus = OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED;

			} else if (sast.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_REOPENED)) {
				deloStatus = OmbConstants.CODE_ZNACHENIE_DELO_STATUS_CONTINUED;
			}

			queryDelo.setParameter(1, jId); // delo_id
			queryDelo.setParameter(2, T0Start.REGISTRATURA); // registratura_id
			queryDelo.setParameter(3, jId); // init_doc_id
			queryDelo.setParameter(4, rn); // rn_delo
			queryDelo.setParameter(5, rnPored); // rn_pored
			queryDelo.setParameter(6, cdate); // delo_date
			queryDelo.setParameter(7, OmbConstants.CODE_ZNACHENIE_DELO_TYPE_JALBA); // delo_type
			queryDelo.setParameter(8, SysConstants.CODE_ZNACHENIE_DA); // free_access
			queryDelo.setParameter(9, deloStatus); // status
			queryDelo.setParameter(10, sdate); // status_date
			queryDelo.setParameter(11, SysConstants.CODE_ZNACHENIE_DA); // with_section
			queryDelo.setParameter(12, SysConstants.CODE_ZNACHENIE_NE); // with_tom
			queryDelo.setParameter(13, 1); // br_tom
			queryDelo.setParameter(14, 250); // max_br_sheets
			queryDelo.setParameter(15, deloYear.get(Calendar.YEAR)); // delo_year
			queryDelo.setParameter(16, "Жалбоподател: " + jbpName); // delo_name
			queryDelo.setParameter(17, uid); // user_reg
			queryDelo.setParameter(18, cdate); // date_reg
			queryDelo.executeUpdate();

			queryDeloDoc.setParameter(1, ++deloDocId); // id
			queryDeloDoc.setParameter(2, jId); // delo_id
			queryDeloDoc.setParameter(3, jId); // input_doc_id
			queryDeloDoc.setParameter(4, cdate); // input_date
			queryDeloDoc.setParameter(5, OmbConstants.CODE_ZNACHENIE_DELO_SECTION_OFFICIAL); // section_type
			queryDeloDoc.setParameter(6, 1); // tom_nomer
			queryDeloDoc.setParameter(7, 1); // ekz_nomer
			queryDeloDoc.setParameter(8, uid); // user_reg
			queryDeloDoc.setParameter(9, cdate); // date_reg
			queryDeloDoc.executeUpdate();

			if (fnames != null && !fnames.isEmpty()) {
				for (String fname : fnames) {
					queryFiles.setParameter(1, ++fileId); // file_id
					queryFiles.setParameter(2, fname); // filename
					queryFiles.setParameter(3, null); // content_type
					queryFiles.setParameter(4, null); // content - накрая ще се точат и файловете
					queryFiles.setParameter(5, uid); // user_reg
					queryFiles.setParameter(6, cdate); // date_reg
					queryFiles.executeUpdate();

					queryFileObject.setParameter(1, ++fileObjectId); // id
					queryFileObject.setParameter(2, jId); // object_id
					queryFileObject.setParameter(3, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC); // object_code
					queryFileObject.setParameter(4, fileId); // file_id
					queryFileObject.setParameter(5, uid); // user_reg
					queryFileObject.setParameter(6, cdate); // date_reg
					queryFileObject.executeUpdate();
				}
			}

			if (vidResult != null) {
				queryJalbaResult.setParameter(1, ++jalbaResultId); // id
				queryJalbaResult.setParameter(2, jId); // doc_id
				queryJalbaResult.setParameter(3, vidResult); // vid_result
				queryJalbaResult.setParameter(4, sdate); // date_result
				queryJalbaResult.executeUpdate();
			}

			insertCnt++;

			if (insertCnt % 300 == 0) {
				dest.commit();
				dest.begin();
				LOGGER.info("  " + insertCnt);
			}
		}
		dest.commit(); // за последните които не са кратни на 300

		stream.close();
		sourceReg.closeConnection();
		dest.closeConnection();

		LOGGER.info("  " + insertCnt);
		LOGGER.info("End loading jalbi from omb_register.");

		return rnPored;
	}

	/**
	 * Прехвърля документите по жалбите от Регистър жалби
	 */
	void loadJalbiWork(JPA sourceReg, JPA dest, int fromJalbaWorkId) throws DbErrorException, SQLException {
		LOGGER.info("");
		LOGGER.info("Start loading jalbi_work from omb_register...");

		Date minDate = DateUtils.systemMinDate();
		Map<Integer, Integer> userMap = T0Start.findDecodeObjectMap(dest, Constants.CODE_ZNACHENIE_JOURNAL_USER, T0Start.SRC_REG);
		Map<Integer, Integer> docVidMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_DOC_VID, T0Start.SRC_REG);

		SimpleDateFormat wdataFormat = new SimpleDateFormat("dd-MM-yyyy");

		Number maxDeloDoc = (Number) dest.getEntityManager().createNativeQuery("select max(id) from delo_doc").getResultList().get(0);
		int deloDocId = maxDeloDoc != null ? maxDeloDoc.intValue() : 0;

		Number maxDoc = (Number) dest.getEntityManager().createNativeQuery("select max(doc_id) from doc").getResultList().get(0);
		int docId = maxDoc != null ? maxDoc.intValue() : 0;

		Number maxFile = (Number) dest.getEntityManager().createNativeQuery("select max(file_id) from files").getResultList().get(0);
		int fileId = maxFile != null ? maxFile.intValue() : 0;

		Number maxFileObject = (Number) dest.getEntityManager().createNativeQuery("select max(id) from file_objects").getResultList().get(0);
		int fileObjectId = maxFileObject != null ? maxFileObject.intValue() : 0;

		Number maxDocDvij = (Number) dest.getEntityManager().createNativeQuery("select max(id) from doc_dvij").getResultList().get(0);
		int docDvijId = maxDocDvij != null ? maxDocDvij.intValue() : 0;

		Number maxDocRef = (Number) dest.getEntityManager().createNativeQuery("select max(id) from doc_referents").getResultList().get(0);
		int docRefId = maxDocRef != null ? maxDocRef.intValue() : 0;

		int insertCnt = 0;

		Query queryDoc = dest.getEntityManager().createNativeQuery("insert into doc" //
			+ " (doc_id, registratura_id, register_id, rn_doc, rn_pored, doc_type, doc_vid, doc_date, otnosno" //
			+ ", valid, valid_date, processed, free_access, count_files, count_originals, competence, user_reg, date_reg, receive_date)" //
			+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19)");

		Query queryDeloDoc = dest.getEntityManager().createNativeQuery( //
			"insert into delo_doc (id, delo_id, input_doc_id, input_date, section_type, tom_nomer, ekz_nomer, user_reg, date_reg)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)");

		Query queryFiles = dest.getEntityManager().createNativeQuery( //
			"insert into files (file_id, filename, content_type, content, user_reg, date_reg) values (?1, ?2, ?3, ?4, ?5, ?6)");

		Query queryFileObject = dest.getEntityManager().createNativeQuery( //
			"insert into file_objects (id, object_id, object_code, file_id, user_reg, date_reg) values (?1, ?2, ?3, ?4, ?5, ?6)");

		Query queryDocDvij = dest.getEntityManager().createNativeQuery( //
			"insert into doc_dvij (id, doc_id, ekz_nomer, dvij_date, dvij_method, dvij_text, dvij_info, status, status_date, user_reg, date_reg)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11)");

		Query queryDocRef = dest.getEntityManager().createNativeQuery( //
			"insert into doc_referents (id, doc_id, code_ref, role_ref, event_date, end_opinion, user_reg, date_reg, pored)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)");

		StringBuilder sql = new StringBuilder();
		sql.append(" select w.id, w.jid, 1 w, w.uid, w.wdata, w.waction, w.txt, w.documents, j.cdate, w.approved_id, w.approved_data, w.pto, w.pp, w.padr, w.send, w.send_id, w.send_data ");
		sql.append(" from jalbi_work w ");
		sql.append(" inner join jalbi j on j.id = w.jid ");
		sql.append(" where w.id > :workId and ");
		sql.append(" 				(w.waction in (2,6) ");
		sql.append(" 						or (w.waction in (1,3,4,5,7,8,9,13) and (w.approved = 'Y' or w.send_data is not null))) ");
		sql.append(" union ");
		sql.append(" select w.id, w.jid, 2 w, w.uid, w.wdata, w.waction, w.txt, w.documents, j.cdate, w.approved_id, w.approved_data, w.pto, w.pp, w.padr, w.send, w.send_id, w.send_data ");
		sql.append(" from jalbi_work w ");
		sql.append(" inner join jalbi j on j.id = w.jid ");
		sql.append(" where w.id > :workId ");
		sql.append(" and w.id in ( ");
		sql.append(" 	select max(t.id) max_id from jalbi_work t ");
		sql.append(" 	left outer join jalbi_work a on a.jid = t.jid and a.waction = t.waction and a.approved = 'Y' and a.id > t.id ");
		sql.append(" 	where t.waction in (1,3,4,5,7,8,9,13) and t.approved = 'N' and t.send_data is null and a.id is null ");
		sql.append(" 	group by t.jid, t.waction ");
		sql.append(" ) ");
		sql.append(" order by 1 ");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceReg.getEntityManager().createNativeQuery(sql.toString()) //
			.setParameter("workId", fromJalbaWorkId) //
			.setHint(QueryHints.HINT_FETCH_SIZE, "" + Integer.MIN_VALUE) // така трябва за да работи с MySQL
			.getResultStream();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			int wId = ((Number) row[0]).intValue(); // id
			int jId = ((Number) row[1]).intValue(); // jid

			Integer docVid;
			if (row[5] != null) { // waction
				int xi = ((Number) row[5]).intValue();

				docVid = docVidMap.get(xi);
				if (docVid == null) {
					docVid = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! jalbi_work.id=" + wId + " ! UNKNOWN waction=" + xi);
				}
			} else {
				docVid = 0; // задължително е в БД
			}

			Date docDate;
			if (row[4] != null) { // wdata
				docDate = (Date) row[4];
			} else {
				docDate = minDate;
			}
			Date dateReg = docDate;

			Integer userReg;
			if (row[3] != null) { // uid
				int xi = ((Number) row[3]).intValue();

				userReg = userMap.get(xi);
				if (userReg == null) {
					userReg = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! jalbi_work.id=" + wId + " ! UNKNOWN uid=" + xi);
				}
			} else {
				userReg = this.transferUser;
			}

			int processed = SysConstants.CODE_ZNACHENIE_DA;
			int docType = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN;
			int sectionType = OmbConstants.CODE_ZNACHENIE_DELO_SECTION_OFFICIAL;
			Date receiveDate = null;

			if (docVid.equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_VH_PISMO)) {
				docType = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN;
				receiveDate = docDate; // само за входящи

			} else if (docVid.equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_PZAPISKA)) {
				docType = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK;
				sectionType = OmbConstants.CODE_ZNACHENIE_DELO_SECTION_INTERNAL;
			}

			int w = ((Number) row[2]).intValue(); // по това ще се гледа дали да се прави html или не
			String txt = SearchUtils.trimToNULL((String) row[6]); // txt
			List<String> fnames = new ArrayList<>();

			int countHtml = 0;
			String otnosno;
			if (docVid.equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_VH_PISMO) || docVid.equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_PZAPISKA)) {
				if (txt != null) {
					otnosno = Jsoup.parse(txt).wholeText();
				} else {
					otnosno = "";
				}

			} else {
				if (w == 2) {
					countHtml = 1; // jalbi_work.txt отива във файл html
					docType = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK; // сменям ги такива на работни
					processed = SysConstants.CODE_ZNACHENIE_NE; // и стават необработени
				} else {
					fnames.add(wId + "-" + wdataFormat.format(docDate) + ".pdf"); // ще се направи пдф
				}
				otnosno = "По жалба " + jId % 100000 + "/" + DateUtils.printDate((Date) row[8]); // cdate
			}

			String documents = SearchUtils.trimToNULL((String) row[7]); // documents
			if (documents != null) {
				List<String> t = prepareFiles(documents);
				if (t == null) {
					LOGGER.warn("  ! jalbi_work.id=" + wId + " ! ERROR documents=" + documents);
				} else {
					fnames.addAll(t);
				}
			}

			queryDoc.setParameter(1, ++docId); // doc_id
			queryDoc.setParameter(2, T0Start.REGISTRATURA); // registratura_id
			queryDoc.setParameter(3, T0Start.REGISTER_JALBI_DOC); // register_id
			queryDoc.setParameter(4, "Миг-" + wId); // rn_doc
			queryDoc.setParameter(5, wId); // rn_pored
			queryDoc.setParameter(6, docType); // doc_type
			queryDoc.setParameter(7, docVid); // doc_vid
			queryDoc.setParameter(8, docDate); // doc_date
			queryDoc.setParameter(9, otnosno); // otnosno
			queryDoc.setParameter(10, OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL); // valid
			queryDoc.setParameter(11, docDate); // valid_date
			queryDoc.setParameter(12, processed); // processed
			queryDoc.setParameter(13, SysConstants.CODE_ZNACHENIE_DA); // free_access
			queryDoc.setParameter(14, countHtml + fnames.size()); // count_files
			queryDoc.setParameter(15, 1); // count_originals
			queryDoc.setParameter(16, OmbConstants.CODE_ZNACHENIE_COMPETENCE_OUR); // competence
			queryDoc.setParameter(17, userReg); // user_reg
			queryDoc.setParameter(18, dateReg); // date_reg
			queryDoc.setParameter(19, new TypedParameterValue(StandardBasicTypes.TIMESTAMP, receiveDate)); // receive_date
			queryDoc.executeUpdate();

			queryDeloDoc.setParameter(1, ++deloDocId); // id
			queryDeloDoc.setParameter(2, jId); // delo_id
			queryDeloDoc.setParameter(3, docId); // input_doc_id
			queryDeloDoc.setParameter(4, docDate); // input_date
			queryDeloDoc.setParameter(5, sectionType); // section_type
			queryDeloDoc.setParameter(6, 1); // tom_nomer
			queryDeloDoc.setParameter(7, 1); // ekz_nomer
			queryDeloDoc.setParameter(8, userReg); // user_reg
			queryDeloDoc.setParameter(9, dateReg); // date_reg
			queryDeloDoc.executeUpdate();

			if (countHtml > 0) { // txt
				queryFiles.setParameter(1, ++fileId); // file_id
				queryFiles.setParameter(2, wId + "-" + wdataFormat.format(docDate) + ".html"); // filename
				queryFiles.setParameter(3, "text/html"); // content_type
				T0Start.setBinaryQueryParam(dest, false, queryFiles, 4, "<html>" + txt + "</html>"); // content
				queryFiles.setParameter(5, userReg); // user_reg
				queryFiles.setParameter(6, dateReg); // date_reg
				queryFiles.executeUpdate();

				queryFileObject.setParameter(1, ++fileObjectId); // id
				queryFileObject.setParameter(2, docId); // object_id
				queryFileObject.setParameter(3, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC); // object_code
				queryFileObject.setParameter(4, fileId); // file_id
				queryFileObject.setParameter(5, userReg); // user_reg
				queryFileObject.setParameter(6, dateReg); // date_reg
				queryFileObject.executeUpdate();
			}

			for (String fname : fnames) {
				queryFiles.setParameter(1, ++fileId); // file_id
				queryFiles.setParameter(2, fname); // filename
				queryFiles.setParameter(3, null); // content_type
				queryFiles.setParameter(4, null); // content - накрая ще се точат и файловете
				queryFiles.setParameter(5, userReg); // user_reg
				queryFiles.setParameter(6, dateReg); // date_reg
				queryFiles.executeUpdate();

				queryFileObject.setParameter(1, ++fileObjectId); // id
				queryFileObject.setParameter(2, docId); // object_id
				queryFileObject.setParameter(3, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC); // object_code
				queryFileObject.setParameter(4, fileId); // file_id
				queryFileObject.setParameter(5, userReg); // user_reg
				queryFileObject.setParameter(6, dateReg); // date_reg
				queryFileObject.executeUpdate();
			}

			if (row[16] != null // send_data
				&& !docVid.equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_VH_PISMO) //
				&& !docVid.equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_PZAPISKA)) {
				Integer userDvij;
				if (row[15] != null) { // send_id
					int xi = ((Number) row[15]).intValue();

					userDvij = userMap.get(xi);
					if (userDvij == null) {
						userDvij = -xi; // запазвам оригиналния но с минус
						LOGGER.warn("  ! jalbi_work.id=" + wId + " ! UNKNOWN send_id=" + xi);
					}
				} else {
					userDvij = this.transferUser;
				}

				StringBuilder dvijText = new StringBuilder();

				String pto = SearchUtils.trimToNULL((String) row[11]); // pto
				String pp = SearchUtils.trimToNULL((String) row[12]); // pp
				if (pto != null) {
					dvijText.append(pto);
				}
				if (pp != null) {
					if (pp.length() > 0) {
						dvijText.append(", ");
					}
					dvijText.append(pp);
				}

				String padr = SearchUtils.trimToNULL((String) row[13]); // padr

				queryDocDvij.setParameter(1, ++docDvijId); // id
				queryDocDvij.setParameter(2, docId); // doc_id
				queryDocDvij.setParameter(3, 1); // ekz_nomer
				queryDocDvij.setParameter(4, row[16]); // dvij_date
				queryDocDvij.setParameter(5, OmbConstants.CODE_ZNACHENIE_PREDAVANE_POSHTA); // dvij_method
				queryDocDvij.setParameter(6, dvijText.toString()); // dvij_text - има и празни
				queryDocDvij.setParameter(7, padr); // dvij_info
				queryDocDvij.setParameter(8, OmbConstants.DS_SENT); // status
				queryDocDvij.setParameter(9, row[16]); // status_date
				queryDocDvij.setParameter(10, userDvij); // user_reg
				queryDocDvij.setParameter(11, row[16]); // date_reg
				queryDocDvij.executeUpdate();
			}

			if (row[9] != null) { // approved_id
				Integer userApproved;
				int xi = ((Number) row[9]).intValue();

				userApproved = userMap.get(xi);
				if (userApproved == null) {
					userApproved = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! jalbi_work.id=" + wId + " ! UNKNOWN approved_id=" + xi);
				}

				Date eventDate;
				if (row[10] != null) { // approved_data
					eventDate = (Date) row[10];
				} else {
					eventDate = docDate;
				}

				queryDocRef.setParameter(1, ++docRefId); // id
				queryDocRef.setParameter(2, docId); // doc_id
				queryDocRef.setParameter(3, userApproved); // code_ref
				queryDocRef.setParameter(4, OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_SIGNED); // role_ref
				queryDocRef.setParameter(5, eventDate); // event_date
				queryDocRef.setParameter(6, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // end_opinion
				queryDocRef.setParameter(7, userReg); // user_reg
				queryDocRef.setParameter(8, eventDate); // date_reg
				queryDocRef.setParameter(9, 1); // pored
				queryDocRef.executeUpdate();
			}

			if (docType == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN || docType == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) {
				// за тези трябва и автор да се слжои
				queryDocRef.setParameter(1, ++docRefId); // id
				queryDocRef.setParameter(2, docId); // doc_id
				queryDocRef.setParameter(3, userReg); // code_ref
				queryDocRef.setParameter(4, OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR); // role_ref
				queryDocRef.setParameter(5, new TypedParameterValue(StandardBasicTypes.TIMESTAMP, null)); // event_date
				queryDocRef.setParameter(6, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // end_opinion
				queryDocRef.setParameter(7, userReg); // user_reg
				queryDocRef.setParameter(8, dateReg); // date_reg
				queryDocRef.setParameter(9, 1); // pored
				queryDocRef.executeUpdate();
			}

			insertCnt++;

			if (insertCnt % 300 == 0) {
				dest.commit();
				dest.begin();
				LOGGER.info("  " + insertCnt);
			}
		}
		dest.commit(); // за последните които не са кратни на 300

		stream.close();
		sourceReg.closeConnection();
		dest.closeConnection();

		StringBuilder update = new StringBuilder(); // необработените в прикключени преписки трябва да станат обработени
		update.append(" update doc set processed = :prcessedDa where doc_id in ( ");
		update.append(" select doc.doc_id from doc ");
		update.append(" inner join delo_doc dd on dd.input_doc_id = doc.doc_id ");
		update.append(" inner join delo on delo.delo_id = dd.delo_id ");
		update.append(" where doc.processed = :prcessedNe and delo.status = :deloStatus) ");

		dest.begin();
		dest.getEntityManager().createNativeQuery(update.toString()) //
			.setParameter("prcessedDa", SysConstants.CODE_ZNACHENIE_DA) //
			.setParameter("prcessedNe", SysConstants.CODE_ZNACHENIE_NE) //
			.setParameter("deloStatus", OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED).executeUpdate();
		dest.commit();

		// трябва на необработени документи, които автора им е определен за напуснал да се смени с активен потребител
		StringBuilder select = new StringBuilder();
		select.append(" select d.doc_id, dr.id, dr.code_ref, u2.user_id ");
		select.append(" from doc d ");
		select.append(" inner join doc_referents dr on dr.doc_id = d.doc_id and dr.role_ref = :roleAuthor ");
		select.append(" inner join adm_users u on u.user_id = dr.code_ref and u.status <> :statActive ");
		select.append(" inner join adm_users u2 on u2.email = u.email and u2.status = :statActive ");
		select.append(" where d.processed = :prcessedNe  ");
		@SuppressWarnings("unchecked")
		List<Object[]> docRefRows = dest.getEntityManager().createNativeQuery(select.toString()) //
			.setParameter("roleAuthor", OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR) //
			.setParameter("statActive", Constants.CODE_ZNACHENIE_STATUS_ACTIVE) //
			.setParameter("prcessedNe", SysConstants.CODE_ZNACHENIE_NE) //
			.getResultList();

		Query updateDocRef = dest.getEntityManager().createNativeQuery("update doc_referents set code_ref = ?1 where id = ?2");
		dest.begin();
		for (Object[] row : docRefRows) {
			updateDocRef.setParameter(1, row[3]);
			updateDocRef.setParameter(2, row[1]);
			updateDocRef.executeUpdate();
		}
		dest.commit();

		LOGGER.info("  " + insertCnt);
		LOGGER.info("End loading jalbi_work from omb_register.");
	}

	/**
	 * @param dest
	 * @throws DbErrorException
	 */
	void recreateSequences(JPA dest) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Recreate SEQUENCEs");

		StringBuilder sql = new StringBuilder();
		sql.append(" select 'SEQ_DELO_DOC' SEQ_NAME, max (id) MAX_ID from DELO_DOC ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_DELO' SEQ_NAME, max (delo_id) MAX_ID from DELO ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_DOC_DVIJ' SEQ_NAME, max (id) MAX_ID from DOC_DVIJ ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_DOC_REFERENTS' SEQ_NAME, max (id) MAX_ID from DOC_REFERENTS ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_DOC' SEQ_NAME, max (doc_id) MAX_ID from DOC ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_REGISTRI' SEQ_NAME, max (register_id) MAX_ID from REGISTRI ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_DOC_SAST_HISTORY' SEQ_NAME, max (id) MAX_ID from DOC_SAST_HISTORY ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_FILE_OBJECTS' SEQ_NAME, max (id) MAX_ID from FILE_OBJECTS ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_FILES' SEQ_NAME, max (file_id) MAX_ID from FILES ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_DOC_VID_SETTINGS' SEQ_NAME, max (id) MAX_ID from DOC_VID_SETTINGS ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_DOC_JALBA_RESULT' SEQ_NAME, max (id) MAX_ID from DOC_JALBA_RESULT ");

		@SuppressWarnings("unchecked")
		List<Object[]> rows = dest.getEntityManager().createNativeQuery(sql.toString()).getResultList();

		String dialect = dest.getDbVendorName();

		for (Object[] row : rows) {
			String seqName = (String) row[0];

			int seqVal = row[1] == null ? 1 : ((Number) row[1]).intValue() + 1;
			if (seqVal < 1) { // ако има някакви отрицателни ще объркат схемата
				seqVal = 1;
			}
			try {
				dest.begin();
				dest.getEntityManager().createNativeQuery("DROP SEQUENCE " + seqName).executeUpdate();
				dest.commit();
				LOGGER.info("   drop " + seqName + " -success. --> ");

			} catch (Exception e) { // най вероятно е няма
				dest.rollback();
				LOGGER.warn(e.getMessage());
			}

			String sqlRegistratura = T0Start.createSequenceQuery(dialect, seqName, seqVal);
			try {
				dest.begin();
				dest.getEntityManager().createNativeQuery(sqlRegistratura).executeUpdate();
				dest.commit();
				LOGGER.info("create " + seqName + " -success. " + seqVal);

			} catch (Exception e) {
				dest.rollback();
				LOGGER.warn(e.getMessage());
			}
		}
	}

	/**
	 * определя кодoвете на експертите
	 */
	private List<Integer> calcCodeExpertList(String exp) {
		List<Integer> result = new ArrayList<>();
		try {
			if (exp.indexOf(',') == -1) {
				result.add(Integer.valueOf(exp));
			} else {
				String[] expArr = exp.split(",");
				for (String s : expArr) {
					result.add(Integer.valueOf(s));
				}
			}
		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на експерти!", e);
//			e.printStackTrace();
		}
		return result;
	}

	/**
	 * определя кода на звеното
	 */
	private Integer calcCodeZveno(String rdid) {
		try {
			if (rdid.indexOf(',') == -1) {
				return Integer.valueOf(rdid);
			}
			return Integer.valueOf(rdid.split(",")[0]);

		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на звено!", e);
//			e.printStackTrace();
		}
		return null;
	}

	/**
	 * По името на град/село, община, област се опитва да намери реален код по ЕКАТТЕ
	 */
	private Integer findEkatte(Object[] row, Map<String, Integer> ekatteNames) {
		try {
			Integer city = SearchUtils.asInteger(row[7]);
			Integer ekatte = EKATTE_FIXED.get(city); // тези няма да се отркият и са забити след анализ
			if (ekatte != null) {
				return ekatte;
			}

			StringBuilder key = new StringBuilder();
			if (row[25] != null) {
				key.append(((String) row[25]).trim());
			}
			if (row[26] != null) {
				key.append(" " + ((String) row[26]).trim());
			}
			if (row[27] != null) {
				key.append(" " + ((String) row[27]).trim());
			}
			return ekatteNames.get(key.toString().toUpperCase());

		} catch (Exception e) {
			LOGGER.error("Грешка при мапинг на ЕКАТТЕ!", e);
//			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @param documents
	 * @return
	 */
	private List<String> prepareFiles(String documents) {
		try {
			String[] ss = documents.split(",");

			List<String> fnames = new ArrayList<>();
			for (String fname : ss) {
				String t = fname != null ? fname.trim() : "";
				if (t.length() > 0 && !fnames.contains(t)) {
					fnames.add(t);
				}
			}
			return fnames;

		} catch (Exception e) {
			LOGGER.error("Грешка при формиране на имена на файлове!", e);
//			e.printStackTrace();
		}
		return null;
	}
}

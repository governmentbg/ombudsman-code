package com.ib.omb.transform;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_REFERENT;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.persistence.Query;

import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.StandardBasicTypes;
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
 * Трансфер на Стар Регистър жалби/Деловодна система
 *
 * @author belev
 */
public class T7DocsMssql {

	private static final Logger LOGGER = LoggerFactory.getLogger(T7DocsMssql.class);

	private static final SimpleDateFormat SDF = new SimpleDateFormat("dd.MM.yyyy");

	private static Map<Integer, Integer> INIT_MAP = new HashMap<>();

	static {
		INIT_MAP.put(79430, 41316);
		INIT_MAP.put(82752, 41816);
		INIT_MAP.put(138634, 56607);
		INIT_MAP.put(139567, 56839);
		INIT_MAP.put(141679, 57400);
		INIT_MAP.put(148360, 59097);
		INIT_MAP.put(155670, 60995);
		INIT_MAP.put(156887, 61403);
		INIT_MAP.put(162352, 63071);
		INIT_MAP.put(176211, 66750);
	}

	private int transferUser;

	/**
	 * @param transferUser
	 */
	public T7DocsMssql(int transferUser) {
		this.transferUser = transferUser;
	}

	/**
	 * Методът
	 *
	 * @param sourceMssql
	 * @param dest
	 * @throws BaseException
	 * @throws SQLException
	 */
	public void transfer(JPA sourceMssql, JPA dest) throws BaseException, SQLException {
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
		// първо тябва да се разбере от къде се започва/продължава
		Number minDossier = (Number) dest.getEntityManager().createNativeQuery("select min(delo_id) from delo where delo_id < 0") //
			.getResultList().get(0);
		int fromDossierId = minDossier != null ? Math.abs(minDossier.intValue()) : 0;
		loadDossiersWithInitDocs(sourceMssql, dest, fromDossierId); // трансфер на преписки и ницииращата жалба

		T0Start.wait(5);
		Number minJalbaNar = (Number) dest.getEntityManager().createNativeQuery("select min(doc_id) from doc_jalba where code_nar is not null and doc_id < 0") //
			.getResultList().get(0);
		int jalbaIdNar = minJalbaNar != null ? Math.abs(minJalbaNar.intValue()) : 0;
		updateJalbaNar(sourceMssql, dest, Math.abs(jalbaIdNar));

		T0Start.wait(5);
		Number minJalbaInst = (Number) dest.getEntityManager().createNativeQuery("select min(doc_id) from doc_jalba where inst_check = 1 and doc_id < 0") //
			.getResultList().get(0);
		int jalbaIdInst = minJalbaInst != null ? Math.abs(minJalbaInst.intValue()) : 0;
		updateJalbaInst(sourceMssql, dest, Math.abs(jalbaIdInst));

		T0Start.wait(5);
		Number minDoc = (Number) dest.getEntityManager().createNativeQuery("select min(doc_id) from doc where doc_id < 0 and doc_vid <> 1") //
			.getResultList().get(0);
		int fromDocId = minDoc != null ? Math.abs(minDoc.intValue()) : 0;
		loadDossierDocs(sourceMssql, dest, fromDocId); // трансфер на документи в преписките

		T0Start.wait(5);
		Number minDvij = (Number) dest.getEntityManager().createNativeQuery("select min(id) from doc_dvij where id < 0") //
			.getResultList().get(0);
		int fromDvijId = minDvij != null ? Math.abs(minDvij.intValue()) : 0;
		loadDocsDvij(sourceMssql, dest, fromDvijId); // движенията

		recreateSequences(dest);

		createIndexes(dest);

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
//		delo_id < 0
//		doc_id < 0
//		file_id < 0

		LOGGER.info("");
		LOGGER.info("Start clear related data");

		int cnt = T0Start.deleteBigTable(dest, "DELO_DOC", "id", "delo_id < 0");
		LOGGER.info("   deleted " + cnt + " rows from table DELO_DOC");

		cnt = T0Start.deleteBigTable(dest, "DELO", "delo_id", "delo_id < 0");
		LOGGER.info("   deleted " + cnt + " rows from table DELO");

		cnt = T0Start.deleteBigTable(dest, "DOC_DVIJ", "id", "doc_id < 0");
		LOGGER.info("   deleted " + cnt + " rows from table DOC_DVIJ");

		cnt = T0Start.deleteBigTable(dest, "DOC_JALBA_RESULT", "id", "doc_id < 0");
		LOGGER.info("   deleted " + cnt + " rows from table DOC_JALBA_RESULT");

		cnt = T0Start.deleteBigTable(dest, "DOC_REFERENTS", "id", "doc_id < 0");
		LOGGER.info("   deleted " + cnt + " rows from table DOC_REFERENTS");

		cnt = T0Start.deleteBigTable(dest, "DOC_SAST_HISTORY", "id", "doc_id < 0");
		LOGGER.info("   deleted " + cnt + " rows from table DOC_SAST_HISTORY");

		cnt = T0Start.deleteBigTable(dest, "DOC_EXPERTS", "doc_id", "doc_id < 0");
		LOGGER.info("   deleted " + cnt + " rows from table DOC_EXPERTS");

		cnt = T0Start.deleteBigTable(dest, "DOC_JALBA", "doc_id", "doc_id < 0");
		LOGGER.info("   deleted " + cnt + " rows from table DOC_JALBA");

		cnt = T0Start.deleteBigTable(dest, "FILE_OBJECTS", "id", "file_id < 0");
		LOGGER.info("   deleted " + cnt + " rows from table FILE_OBJECTS");

		cnt = T0Start.deleteBigTable(dest, "FILES", "file_id", "file_id < 0");
		LOGGER.info("   deleted " + cnt + " rows from table FILES");

		cnt = T0Start.deleteBigTable(dest, "DOC", "doc_id", "doc_id < 0");
		LOGGER.info("   deleted " + cnt + " rows from table DOC");

		cnt = T0Start.deleteBigTable(dest, "REGISTRI", "register_id", "register_id < 0");
		LOGGER.info("   deleted " + cnt + " rows from table REGISTRI");
	}

	/**
	 * създаване на индекси
	 */
	void createIndexes(JPA dest) {
		LOGGER.info("");
		LOGGER.info("Start create indexes");

		List<String> list = new ArrayList<>();
		list.add("CREATE INDEX delo_registratura_id_rn_delo_delo_year	ON delo USING btree (registratura_id, rn_delo, delo_year)");
		list.add("CREATE INDEX doc_registratura_id_rn_doc_doc_date	ON doc USING btree (registratura_id, rn_doc, doc_date)");
		list.add("CREATE INDEX doc_referents_doc_id	ON doc_referents USING btree (doc_id)");
		list.add("CREATE INDEX doc_rn_doc_upper	ON doc USING btree (upper(rn_doc))");
		list.add("CREATE INDEX delo_rn_delo_upper	ON delo USING btree (upper(rn_delo))");
		list.add("CREATE INDEX doc_doc_vid	ON doc USING btree (doc_vid)");
		list.add("CREATE INDEX doc_doc_date	ON doc USING btree (doc_date desc)");
		list.add("CREATE INDEX delo_delo_date	ON delo USING btree (delo_date desc)");
		list.add("CREATE INDEX doc_dvij_dvij_date	ON doc_dvij USING btree (dvij_date desc)");
		list.add("CREATE INDEX doc_jalba_sast	ON doc_jalba USING btree (sast)");
		list.add("CREATE INDEX doc_dvij_doc_id	ON doc_dvij USING btree (doc_id)");
		list.add("CREATE INDEX task_doc_id	ON task USING btree (doc_id)");
		list.add("CREATE INDEX delo_doc_delo_id	ON delo_doc USING btree (delo_id)");
		list.add("CREATE INDEX delo_doc_input_doc_id	ON delo_doc USING btree (input_doc_id)");
		list.add("CREATE INDEX doc_sast_history_doc_id	ON doc_sast_history USING btree (doc_id)");
		list.add("CREATE INDEX doc_referents_code_ref	ON doc_referents USING btree (code_ref)");
		list.add("CREATE INDEX doc_processed_doc_type	ON doc USING btree (processed, doc_type)");
		list.add("CREATE INDEX task_rn_task	ON task USING btree (rn_task)");
		list.add("CREATE INDEX doc_jalba_result_doc_id	ON doc_jalba_result USING btree (doc_id)");

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
		LOGGER.info("End create indexes");
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

		dest.begin();

//		Регистър жалби - стари
		query.setParameter(1, T0Start.REGISTER_JALBI_OLD); // REGISTER_ID
		query.setParameter(2, T0Start.REGISTRATURA); // REGISTRATURA_ID
		query.setParameter(3, "Регистър жалби - стари"); // REGISTER
		query.setParameter(4, OmbConstants.CODE_ZNACHENIE_REGISTER_DNEV); // REGISTER_TYPE
		query.setParameter(5, SysConstants.CODE_ZNACHENIE_NE); // VALID
		query.setParameter(6, SysConstants.CODE_ZNACHENIE_NE); // COMMON
		query.setParameter(7, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // DOC_TYPE
		query.setParameter(8, null); // PREFIX
		query.setParameter(9, OmbConstants.CODE_ZNACHENIE_ALG_FREE); // ALG
		query.setParameter(10, 1); // BEG_NOMER
		query.setParameter(11, 1); // ACT_NOMER
		query.setParameter(12, 1); // STEP
		query.setParameter(13, this.transferUser); // USER_REG
		query.setParameter(14, DateUtils.systemMinDate()); // DATE_REG
		query.setParameter(15, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // sort_nomer
		query.executeUpdate();

		dest.commit();
	}

	/**
	 * Прехвърля движенията на документите
	 */
	void loadDocsDvij(JPA sourceMssql, JPA dest, int fromDvijId) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start loading DocsDvij from omb_mssql...");

		Date minDate = DateUtils.systemMinDate();
		Map<Integer, Integer> userMap = T0Start.findDecodeObjectMap(dest, Constants.CODE_ZNACHENIE_JOURNAL_USER, T0Start.SRC_MSSQL);
		Map<Integer, Integer> correspMap = T0Start.findDecodeObjectMap(dest, CODE_ZNACHENIE_JOURNAL_REFERENT, T0Start.SRC_MSSQL);

		@SuppressWarnings("unchecked")
		Stream<Object[]> streamCorrespNamesMap = dest.getEntityManager().createNativeQuery( //
			"select distinct r.code, r.ref_name from transfer_table tt inner join adm_referents r on r.code = tt.dest_id" //
				+ " where tt.src = ?1 and tt.code_object = ?2") //
			.setParameter(1, T0Start.SRC_MSSQL).setParameter(2, OmbConstants.CODE_ZNACHENIE_JOURNAL_REFERENT) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();
		Map<Integer, String> correspNamesMap = new HashMap<>();
		Iterator<Object[]> correspIter = streamCorrespNamesMap.iterator();
		while (correspIter.hasNext()) {
			Object[] row = correspIter.next();

			int key = ((Number) row[0]).intValue();

			if (correspNamesMap.containsKey(key)) {
				LOGGER.warn("streamCorrespNamesMap duplicate key=" + key);
			}
			correspNamesMap.put(key, (String) row[1]);
		}
		streamCorrespNamesMap.close();
		LOGGER.info("!found " + correspNamesMap.size() + " correspNames!");

		int insertCnt = 0;

		Query queryDocDvij = dest.getEntityManager().createNativeQuery( //
			"insert into doc_dvij (id, doc_id, ekz_nomer, code_ref, dvij_date, dvij_method, dvij_text, status, status_date, user_reg, date_reg, dvij_info)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12)");

		StringBuilder sql = new StringBuilder();
		sql.append(" select m.ID Dvij_ID, m.Custom_ID, d.ID Doc_ID, m.To_ID Corresp_ID, m.CD, m.CU "); // 0,1,2,3,4,5
		sql.append(" , ocor.ID Organization_ID, pcor.ID Person_ID "); // 6,7
		sql.append(" , pcor.Name PersonName , pcor.Surname PersonSurname, pcor.Family PersonFamily "); // 8,9,10
		sql.append(" from Movement m ");
		sql.append(" inner join Document d on d.ID = m.Item_ID ");
		sql.append(" inner join Referent cor on cor.ID = m.To_ID ");
		sql.append(" left outer join Person pcor on pcor.ID = cor.Person_ID ");
		sql.append(" left outer join Organization ocor on ocor.ID = cor.Organization_ID ");
		sql.append(" where m.ID > ?1 and m.ItemType_ID = 0 ");
		sql.append(" and d.DocumentType_ID in (7,408,406,414,409,413,410,18,774,11) and cor.RefType_ID in (366,367) ");
		sql.append(" order by m.ID ");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceMssql.getEntityManager().createNativeQuery(sql.toString()) //
			.setParameter(1, fromDvijId) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500).getResultStream();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			int docDvijId = ((Number) row[0]).intValue(); // Dvij_ID
			int docId = ((Number) row[2]).intValue(); // Doc_ID

			Date cd;
			if (row[4] != null) { // CD
				cd = (Date) row[4];
			} else {
				cd = minDate;
			}
			Integer cu;
			if (row[5] != null) { // CU
				int xi = ((Number) row[5]).intValue();

				cu = userMap.get(xi);
				if (cu == null) {
					cu = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! Movement.ID=" + docDvijId + " ! UNKNOWN CU=" + xi);
				}
			} else {
				cu = this.transferUser;
			}

			Object[] rnMove = parseCustomId((String) row[1]); // Custom_ID
//			if (rnDoc[0] == null) {
//				rnDoc[0] = String.valueOf(docId); // взимам ИД-то
//				LOGGER.warn("  ! Movement.ID=" + docDvijId + " ! ERROR Custom_ID/Number=" + row[1]);
//			}
			if (rnMove[1] == null) {
				rnMove[1] = cd; // взимам датата на реда
				LOGGER.warn("  ! Movement.ID=" + docDvijId + " ! ERROR Custom_ID/Date=" + row[1]);
			}

			Integer codeRef = null;
			if (row[3] != null && row[6] != null) { // Corresp_ID && Organization_ID - само за НФЛ-та
				int xi = ((Number) row[3]).intValue();

				codeRef = correspMap.get(xi);
				if (codeRef == null) {
					codeRef = -xi; // запазвам оригиналния но с минус
					LOGGER.info("  ! Movement.ID=" + docDvijId + " ! UNKNOWN To_ID=" + xi);
				}
			}

			String dvijInfo = SearchUtils.trimToNULL((String) row[1]); // Custom_ID
			if (dvijInfo == null) {
				dvijInfo = "";
			}

			StringBuilder person = new StringBuilder();
			if (row[7] != null) { // Person_ID
				String t = SearchUtils.trimToNULL((String) row[8]); // Name
				if (t != null) {
					person.append(t);
				}
				t = SearchUtils.trimToNULL((String) row[9]); // Surname
				if (t != null) {
					person.append(" " + t);
				}
				t = SearchUtils.trimToNULL((String) row[10]); // Family
				if (t != null) {
					person.append(" " + t);
				}

				if (row[6] != null) { // има и Organization_ID
					dvijInfo = dvijInfo + " до " + person.toString().trim();
				}
			}

			String correspName;
			if (correspNamesMap.containsKey(codeRef)) {
				correspName = correspNamesMap.get(codeRef);
			} else {
				correspName = person.toString().trim();
			}

			queryDocDvij.setParameter(1, docDvijId * -1); // id
			queryDocDvij.setParameter(2, docId * -1); // doc_id
			queryDocDvij.setParameter(3, 1); // ekz_nomer
			queryDocDvij.setParameter(4, new TypedParameterValue(StandardBasicTypes.INTEGER, codeRef)); // code_ref
			queryDocDvij.setParameter(5, rnMove[1]); // dvij_date
			queryDocDvij.setParameter(6, OmbConstants.CODE_ZNACHENIE_PREDAVANE_POSHTA); // dvij_method
			queryDocDvij.setParameter(7, correspName); // dvij_text
			queryDocDvij.setParameter(8, OmbConstants.DS_SENT); // status
			queryDocDvij.setParameter(9, rnMove[1]); // status_date
			queryDocDvij.setParameter(10, cu); // user_reg
			queryDocDvij.setParameter(11, cd); // date_reg
			queryDocDvij.setParameter(12, dvijInfo); // dvij_info
			queryDocDvij.executeUpdate();

			insertCnt++;

			if (insertCnt % 500 == 0) {
				dest.commit();
				dest.begin();
				LOGGER.info("  " + insertCnt);
			}
		}
		dest.commit(); // за последните които не са кратни на 500

		stream.close();
		sourceMssql.closeConnection();
		dest.closeConnection();

		LOGGER.info("  " + insertCnt);
		LOGGER.info("End loading DocsDvij from omb_mssql.");
	}

	/**
	 * Прехвърля документите от преписките
	 */
	void loadDossierDocs(JPA sourceMssql, JPA dest, int fromDocId) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start loading DossierDocs from omb_mssql...");

		Date minDate = DateUtils.systemMinDate();
		Map<Integer, Integer> userMap = T0Start.findDecodeObjectMap(dest, Constants.CODE_ZNACHENIE_JOURNAL_USER, T0Start.SRC_MSSQL);
		Map<Integer, Integer> docVidMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_DOC_VID, T0Start.SRC_MSSQL);
		Map<Integer, Integer> correspMap = T0Start.findDecodeObjectMap(dest, CODE_ZNACHENIE_JOURNAL_REFERENT, T0Start.SRC_MSSQL);

		Set<Integer> typeDocRef = new HashSet<>(); // за тези типове док от mssql базата ще се записват doc_referents
		typeDocRef.add(7);
		typeDocRef.add(408);
		typeDocRef.add(406);
		typeDocRef.add(414);
		typeDocRef.add(409);
		typeDocRef.add(413);
		typeDocRef.add(410);
		typeDocRef.add(18);
		typeDocRef.add(774);
		typeDocRef.add(11);
		typeDocRef.add(773);

		Number maxDocRef = (Number) dest.getEntityManager().createNativeQuery("select max(id) from doc_referents").getResultList().get(0);
		int docRefId = maxDocRef != null ? maxDocRef.intValue() : 0;

		int insertCnt = 0;

		Query queryDoc = dest.getEntityManager().createNativeQuery("insert into doc" //
			+ " (doc_id, registratura_id, register_id, rn_doc, rn_pored, doc_type, doc_vid, doc_date, otnosno" //
			+ ", valid, valid_date, processed, free_access, count_files, count_originals, competence, user_reg, date_reg" //
			+ ", doc_info, receive_date, code_ref_corresp)" //
			+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20, ?21)");

		Query queryDeloDoc = dest.getEntityManager().createNativeQuery( //
			"insert into delo_doc (id, delo_id, input_doc_id, input_date, section_type, tom_nomer, ekz_nomer, user_reg, date_reg)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)");

		Query queryFiles = dest.getEntityManager().createNativeQuery( //
			"insert into files (file_id, filename, content_type, content, user_reg, date_reg) values (?1, ?2, ?3, ?4, ?5, ?6)");

		Query queryFileObject = dest.getEntityManager().createNativeQuery( //
			"insert into file_objects (id, object_id, object_code, file_id, user_reg, date_reg) values (?1, ?2, ?3, ?4, ?5, ?6)");

		Query queryDocRef = dest.getEntityManager().createNativeQuery( //
			"insert into doc_referents (id, doc_id, code_ref, role_ref, event_date, end_opinion, user_reg, date_reg, pored)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9)");

		StringBuilder sql = new StringBuilder();
		sql.append(" select doc.ID Doc_ID, doc.Custom_ID RN_Doc, doc.DocumentType_ID, doc.CD DocCD, doc.CU DocCU "); // 0,1,2,3,4
		sql.append(" , dd.ID DossDoc_ID, dd.Dossier_ID "); // 5,6
		sql.append(" , f.ID DocFile_ID, f.FileExtension, f.cvtFileName, f.CD FileCD, f.CU FileCU "); // 7,8,9,10,11
		sql.append(" , doc.Annotation, doc.Ref, cor.ID Corresp_ID "); // 12,13,14
		sql.append(" , pcor.ID Person_ID, pcor.Name PersonName , pcor.Surname PersonSurname, pcor.Family PersonFamily "); // 15,16,17,18
		sql.append(" , doc.Developer_ID, doc.Examiner_ID, doc.Approver_ID, doc.ApprovDate "); // 19,20,21,22
		sql.append(" from Document doc ");
		sql.append(" inner join DossierDocs dd on dd.Doc_ID = doc.ID ");
		sql.append(" left outer join SpecifDocument sdoc on sdoc.Doc_ID = doc.ID ");
		sql.append(" left outer join DocFiles f on f.Doc_ID = doc.ID ");
		sql.append(" left outer join Dossier doss on doss.InitDoc_ID = doc.ID ");
		sql.append(" left outer join Movement m on m.Item_ID = doc.ID and m.ItemType_ID = 0 and doc.DocumentType_ID = 8 ");
		sql.append(" left outer join Referent cor on cor.ID = m.From_ID and cor.RefType_ID in (366,367) ");
		sql.append(" left outer join Person pcor on pcor.ID = cor.Person_ID and cor.Organization_ID is not null and cor.ID <> 34824 "); // допълнителен
																																		// подател
		sql.append(" where doc.ID > ?1 ");
		sql.append(" and doc.DocumentType_ID in (74,4,7,8,408,406,414,409,413,410,18,774,773,11) ");
		sql.append(" and doss.ID is null ");
		sql.append(" order by doc.ID ");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceMssql.getEntityManager().createNativeQuery(sql.toString()) //
			.setParameter(1, fromDocId) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500).getResultStream();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			int docId = ((Number) row[0]).intValue(); // Doc_ID

			Date docCD;
			if (row[3] != null) { // DocCD
				docCD = (Date) row[3];
			} else {
				docCD = minDate;
			}
			Integer docCU;
			if (row[4] != null) { // DocCU
				int xi = ((Number) row[4]).intValue();

				docCU = userMap.get(xi);
				if (docCU == null) {
					docCU = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! Document.ID=" + docId + " ! UNKNOWN CU=" + xi);
				}
			} else {
				docCU = this.transferUser;
			}

			Object[] rnDoc = parseCustomId((String) row[1]); // RN_Doc
			if (rnDoc[0] == null) {
				rnDoc[0] = String.valueOf(docId); // взимам ИД-то
				LOGGER.warn("  ! Document.ID=" + docId + " ! ERROR Custom_ID/Number=" + row[1]);
			}
			if (rnDoc[1] == null) {
				rnDoc[1] = docCD; // взимам датата на реда
				LOGGER.warn("  ! Document.ID=" + docId + " ! ERROR Custom_ID/Date=" + row[1]);
			}

			Integer fileId = null;
			if (row[7] != null) { // DocFile_ID
				fileId = ((Number) row[7]).intValue();
			}

			boolean addDocRef = false;
			Integer docVid;
			if (row[2] != null) { // DocumentType_ID
				int xi = ((Number) row[2]).intValue();
				addDocRef = typeDocRef.contains(xi);

				docVid = docVidMap.get(xi);
				if (docVid == null) {
					docVid = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! Document.ID=" + docId + " ! UNKNOWN DocumentType_ID=" + xi);
				}
			} else {
				docVid = 0; // задължително е в БД
			}
			Date receiveDate = null;
			int docType = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN;
			if (docVid.equals(1) || docVid.equals(5)) {
				docType = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN;
				receiveDate = (Date) rnDoc[1]; // CD?
			}

			String otnosno;
			if (row[12] != null) { // Annotation
				otnosno = (String) row[12];
			} else {
				otnosno = "";
			}
			String docInfo = SearchUtils.trimToNULL((String) row[13]); // Ref

			if (row[15] != null) { // Person_ID
				StringBuilder person = new StringBuilder();
				String t = SearchUtils.trimToNULL((String) row[16]); // Name
				if (t != null) {
					person.append(t);
				}
				t = SearchUtils.trimToNULL((String) row[17]); // Surname
				if (t != null) {
					person.append(" " + t);
				}
				t = SearchUtils.trimToNULL((String) row[18]); // Family
				if (t != null) {
					person.append(" " + t);
				}
				if (docInfo == null) {
					docInfo = "Подател: " + person;
				} else {
					docInfo = docInfo + "\nПодател: " + person;
				}
			}

			Integer codeRefCorresp = null;
			if (row[14] != null) { // Corresp_ID - само за входящите може да има тука нещо
				int xi = ((Number) row[14]).intValue();

				codeRefCorresp = correspMap.get(xi);
				if (codeRefCorresp == null) {
					codeRefCorresp = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! Document.ID=" + docId + " ! UNKNOWN Corresp_ID=" + xi);
				}
			}

			queryDoc.setParameter(1, docId * -1); // doc_id
			queryDoc.setParameter(2, T0Start.REGISTRATURA); // registratura_id
			queryDoc.setParameter(3, T0Start.REGISTER_JALBI_DOC); // register_id
			queryDoc.setParameter(4, rnDoc[0]); // rn_doc
			queryDoc.setParameter(5, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // rn_pored
			queryDoc.setParameter(6, docType); // doc_type
			queryDoc.setParameter(7, docVid); // doc_vid
			queryDoc.setParameter(8, rnDoc[1]); // doc_date
			queryDoc.setParameter(9, otnosno); // otnosno
			queryDoc.setParameter(10, OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL); // valid
			queryDoc.setParameter(11, rnDoc[1]); // valid_date
			queryDoc.setParameter(12, SysConstants.CODE_ZNACHENIE_DA); // processed
			queryDoc.setParameter(13, SysConstants.CODE_ZNACHENIE_DA); // free_access
			queryDoc.setParameter(14, fileId == null ? 0 : 1); // count_files
			queryDoc.setParameter(15, 1); // count_originals
			queryDoc.setParameter(16, OmbConstants.CODE_ZNACHENIE_COMPETENCE_OUR); // competence
			queryDoc.setParameter(17, docCU); // user_reg
			queryDoc.setParameter(18, docCD); // date_reg
			queryDoc.setParameter(19, docInfo); // doc_info + подател ако има допълнително ФЛ към организация
			queryDoc.setParameter(20, new TypedParameterValue(StandardBasicTypes.TIMESTAMP, receiveDate)); // receive_date
			queryDoc.setParameter(21, new TypedParameterValue(StandardBasicTypes.INTEGER, codeRefCorresp)); // code_ref_corresp
			queryDoc.executeUpdate();

			int deloDocId = ((Number) row[5]).intValue(); // DossDoc_ID
			int deloId = ((Number) row[6]).intValue(); // Dossier_ID

			queryDeloDoc.setParameter(1, deloDocId * -1); // id
			queryDeloDoc.setParameter(2, deloId * -1); // delo_id
			queryDeloDoc.setParameter(3, docId * -1); // input_doc_id
			queryDeloDoc.setParameter(4, docCD); // input_date
			queryDeloDoc.setParameter(5, OmbConstants.CODE_ZNACHENIE_DELO_SECTION_OFFICIAL); // section_type
			queryDeloDoc.setParameter(6, 1); // tom_nomer
			queryDeloDoc.setParameter(7, 1); // ekz_nomer
			queryDeloDoc.setParameter(8, docCU); // user_reg
			queryDeloDoc.setParameter(9, docCD); // date_reg
			queryDeloDoc.executeUpdate();

			if (fileId != null) {
				Date fileCD;
				if (row[10] != null) { // FileCD
					fileCD = (Date) row[10];
				} else {
					fileCD = docCD;
				}
				Integer fileCU;
				if (row[11] != null) { // FileCU
					int xi = ((Number) row[11]).intValue();

					fileCU = userMap.get(xi);
					if (fileCU == null) {
						fileCU = -xi; // запазвам оригиналния но с минус
						LOGGER.warn("  ! DocFiles.ID=" + fileId + " ! UNKNOWN CU=" + xi);
					}
				} else {
					fileCU = docCU;
				}

				String filename = parseFilename((String) row[1], (String) row[9], (String) row[8]);

				queryFiles.setParameter(1, fileId * -1); // file_id
				queryFiles.setParameter(2, filename); // filename
				queryFiles.setParameter(3, null); // content_type
				queryFiles.setParameter(4, null); // content - накрая ще се точат и файловете
				queryFiles.setParameter(5, fileCU); // user_reg
				queryFiles.setParameter(6, fileCD); // date_reg
				queryFiles.executeUpdate();

				queryFileObject.setParameter(1, fileId * -1); // id
				queryFileObject.setParameter(2, docId * -1); // object_id
				queryFileObject.setParameter(3, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC); // object_code
				queryFileObject.setParameter(4, fileId * -1); // file_id
				queryFileObject.setParameter(5, fileCU); // user_reg
				queryFileObject.setParameter(6, fileCD); // date_reg
				queryFileObject.executeUpdate();
			}

			if (addDocRef) {
				if (row[19] != null) { // Developer_ID - автор = 1
					Integer codeRef;
					int xi = ((Number) row[19]).intValue();

					codeRef = userMap.get(xi);
					if (codeRef == null) {
//						codeRef = -xi; // запазвам оригиналния но с минус
//						LOGGER.warn("  ! Document.ID=" + docId + " ! UNKNOWN Developer_ID=" + xi);
					} else {
						queryDocRef.setParameter(1, ++docRefId); // id
						queryDocRef.setParameter(2, docId * -1); // doc_id
						queryDocRef.setParameter(3, codeRef); // code_ref
						queryDocRef.setParameter(4, OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR); // role_ref
						queryDocRef.setParameter(5, new TypedParameterValue(StandardBasicTypes.TIMESTAMP, null)); // event_date
						queryDocRef.setParameter(6, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // end_opinion
						queryDocRef.setParameter(7, codeRef); // user_reg
						queryDocRef.setParameter(8, docCD); // date_reg
						queryDocRef.setParameter(9, 1); // pored
						queryDocRef.executeUpdate();
					}
				}

				if (row[20] != null) { // Examiner_ID - съгласувал = 2
					Integer codeRef;
					int xi = ((Number) row[20]).intValue();

					codeRef = userMap.get(xi);
					if (codeRef == null) {
//						codeRef = -xi; // запазвам оригиналния но с минус
//						LOGGER.warn("  ! Document.ID=" + docId + " ! UNKNOWN Examiner_ID=" + xi);
					} else {
						queryDocRef.setParameter(1, ++docRefId); // id
						queryDocRef.setParameter(2, docId * -1); // doc_id
						queryDocRef.setParameter(3, codeRef); // code_ref
						queryDocRef.setParameter(4, OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AGREED); // role_ref
						queryDocRef.setParameter(5, new TypedParameterValue(StandardBasicTypes.TIMESTAMP, null)); // event_date
						queryDocRef.setParameter(6, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // end_opinion
						queryDocRef.setParameter(7, codeRef); // user_reg
						queryDocRef.setParameter(8, docCD); // date_reg
						queryDocRef.setParameter(9, 1); // pored
						queryDocRef.executeUpdate();
					}
				}

				if (row[21] != null) { // Approver_ID - подписал = 3
					Integer codeRef;
					int xi = ((Number) row[21]).intValue();

					codeRef = userMap.get(xi);
					if (codeRef == null) {
//						codeRef = -xi; // запазвам оригиналния но с минус
//						LOGGER.warn("  ! Document.ID=" + docId + " ! UNKNOWN Approver_ID=" + xi);
					} else {
						Date eventDate;
						if (row[22] != null) { // ApprovDate
							eventDate = (Date) row[22];
						} else {
							eventDate = docCD;
						}

						queryDocRef.setParameter(1, ++docRefId); // id
						queryDocRef.setParameter(2, docId * -1); // doc_id
						queryDocRef.setParameter(3, codeRef); // code_ref
						queryDocRef.setParameter(4, OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_SIGNED); // role_ref
						queryDocRef.setParameter(5, new TypedParameterValue(StandardBasicTypes.TIMESTAMP, eventDate)); // event_date
						queryDocRef.setParameter(6, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // end_opinion
						queryDocRef.setParameter(7, codeRef); // user_reg
						queryDocRef.setParameter(8, docCD); // date_reg
						queryDocRef.setParameter(9, 1); // pored
						queryDocRef.executeUpdate();
					}
				}
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
		sourceMssql.closeConnection();
		dest.closeConnection();

		LOGGER.info("  " + insertCnt);
		LOGGER.info("End loading DossierDocs from omb_mssql.");
	}

	/**
	 * Прехвърля преписките и жалбите (иницииращи) от Стар Регистър жалби/Деловодна система
	 */
	void loadDossiersWithInitDocs(JPA sourceMssql, JPA dest, int fromDossierId) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start loading DossiersWithInitDocs from omb_mssql...");

		Date minDate = DateUtils.systemMinDate();
		Map<Integer, Integer> userMap = T0Start.findDecodeObjectMap(dest, Constants.CODE_ZNACHENIE_JOURNAL_USER, T0Start.SRC_MSSQL);
		Map<Integer, Integer> zvenoMap = T0Start.findDecodeClassifMap(dest, Constants.CODE_CLASSIF_ADMIN_STR, T0Start.SRC_MSSQL);

		Map<Integer, Integer> dopustMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_DOPUST, T0Start.SRC_MSSQL);
		Map<Integer, Integer> osnNedopustMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_OSN_NEDOPUST, T0Start.SRC_MSSQL);

		Map<Integer, Integer> zasPravaMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_ZAS_PRAVA, T0Start.SRC_MSSQL);
		Map<Integer, Integer> vidOplMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_VID_OPL, T0Start.SRC_MSSQL);
		Map<Integer, Integer> jalbaFinMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_JALBA_FIN, T0Start.SRC_MSSQL);

		int insertCnt = 0;

		Query queryDoc = dest.getEntityManager().createNativeQuery("insert into doc" //
			+ " (doc_id, registratura_id, register_id, rn_doc, rn_pored, doc_type, doc_vid, doc_date, otnosno" //
			+ ", valid, valid_date, processed, free_access, count_files, competence, user_reg, date_reg, pored_delo, doc_info)" //
			+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19)");

		Query queryDocJalba = dest.getEntityManager().createNativeQuery( //
			"insert into doc_jalba (doc_id, jbp_type, jbp_name, jbp_egn, jbp_ekatte, jbp_post, jbp_addr, jbp_phone, jbp_email" //
				+ ", code_nar, zas_prava, vid_opl, jalba_text, request_text, regist_comment, code_zveno, code_expert" //
				+ ", sast, sast_date, srok, dopust, osn_nedopust, public_visible, fin_method, corruption, inst_check, inst_names, jbp_grj, submit_date" //
				+ ", jbp_hidden, user_last_mod, date_last_mod, jbp_pol, date_nar)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20" //
				+ ", ?21, ?22, ?23, ?24, ?25, ?26, ?27, ?28, ?29, ?30, ?31, ?32, ?33, ?34)");

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

		StringBuilder sql = new StringBuilder();
		sql.append(" select doss.ID Doss_ID, doss.Custom_ID RN_Doss, doss.Name DossName, doss.CD DossCD, doss.CU DossCU "); // 0,1,2,3,4
		sql.append(" , doc.ID InitDoc_ID, doc.Custom_ID RN_Doc, doc.CD DocCD, doc.CU DocCU "); // 5,6,7,8
		sql.append(" , dd.ID DossDoc_ID "); // 9
		sql.append(" , jbp.EgnLnc, jbp.Name, jbp.Surname, jbp.Family, jbp.Gender, ac.Ekatte, ac.PostCode "); // 10,11,12,13,14,15,16
		sql.append(" , ac.Country, ac.AddrLine1, ac.AddrLine2, ac.AddrLine3, ac.Phones, ac.Emails "); // 17,18,19,20,21,22
		sql.append(" , f.ID DocFile_ID, f.FileExtension, f.cvtFileName, f.CD FileCD, f.CU FileCU "); // 23,24,25,26,27
		sql.append(" , jbp.IsForeignCitizen, ctry.Name CountryName, sdoss.AssignedTo_ID, expert.Department_ID, sdoss.ReasonToReject "); // 28,29,30,31,32
		sql.append(" , doc.Annotation, doc.Ref, sdoss.ViolationBeginDate, doss.ClosingDate, sdoss.IsCorruption "); // 33,34,35,36,37
		sql.append(" , sdoss.ViolationDescription, sdoss.Request, sdoss.ViolationType_ID, sdoss.Status_ID "); // 38,39,40,41
		sql.append(" from SpecifDossier sdoss ");
		sql.append(" inner join Dossier doss on doss.ID = sdoss.Dossier_ID ");
		sql.append(" inner join Document doc on doc.ID = doss.InitDoc_ID ");
		sql.append(" inner join SpecifDocument sdoc on sdoc.Doc_ID = doc.ID ");
		sql.append(" inner join DossierDocs dd on dd.Doc_ID = doc.ID and dd.Dossier_ID = doss.ID ");
		sql.append(" left outer join DocFiles f on f.Doc_ID = doc.ID ");
		sql.append(" left outer join Referent expert on expert.ID = sdoss.AssignedTo_ID ");
		sql.append(" left outer join Referent comp on comp.ID = sdoc.Complainant_ID ");
		sql.append(" left outer join Person jbp on jbp.ID = comp.Person_ID ");
		sql.append(" left outer join AddressLink alink on alink.Object_ID = jbp.ID and alink.ObjectType = 1 and alink.IsDefault = 1 ");
		sql.append(" left outer join AddressCommunic ac on ac.ID = alink.Address_ID ");
		sql.append(" left outer join Nomenclatures ctry on ctry.ID = ac.Country ");
		sql.append(" where doss.ID > ?1 ");
		sql.append(" order by doss.ID ");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceMssql.getEntityManager().createNativeQuery( //
			sql.toString()).setParameter(1, fromDossierId) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			int docId = ((Number) row[5]).intValue(); // InitDoc_ID
			int deloId = ((Number) row[0]).intValue(); // Doss_ID
			boolean saveDoc = true;
			if (INIT_MAP.containsKey(docId) && !INIT_MAP.get(docId).equals(deloId)) {
				LOGGER.info("  ! Document.ID=" + docId + " ! DUPLICATE InitDoc for DossierID=" + deloId);
				saveDoc = false;
			}

			Date docCD;
			if (row[7] != null) { // DocCD
				docCD = (Date) row[7];
			} else {
				docCD = minDate;
			}
			Integer docCU;
			if (row[8] != null) { // DocCU
				int xi = ((Number) row[8]).intValue();

				docCU = userMap.get(xi);
				if (docCU == null) {
					docCU = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! Document.ID=" + docId + " ! UNKNOWN CU=" + xi);
				}
			} else {
				docCU = this.transferUser;
			}

			Object[] rnDoc = parseCustomId((String) row[6]); // RN_Doc
			if (rnDoc[0] == null) {
				rnDoc[0] = String.valueOf(docId); // взимам ИД-то
				LOGGER.warn("  ! Document.ID=" + docId + " ! ERROR Custom_ID/Number=" + row[6]);
			}
			if (rnDoc[1] == null) {
				rnDoc[1] = docCD; // взимам датата на реда
				LOGGER.warn("  ! Document.ID=" + docId + " ! ERROR Custom_ID/Date=" + row[6]);
			}

			Integer fileId = null;
			if (row[23] != null) { // DocFile_ID
				fileId = ((Number) row[23]).intValue();
			}

			String otnosno;
			if (row[33] != null) { // Annotation
				otnosno = (String) row[33];
			} else {
				otnosno = "";
			}
			String docInfo = SearchUtils.trimToNULL((String) row[34]); // Ref

			if (saveDoc) {

				queryDoc.setParameter(1, docId * -1); // doc_id
				queryDoc.setParameter(2, T0Start.REGISTRATURA); // registratura_id
				queryDoc.setParameter(3, T0Start.REGISTER_JALBI_OLD); // register_id
				queryDoc.setParameter(4, rnDoc[0]); // rn_doc
				queryDoc.setParameter(5, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // rn_pored
				queryDoc.setParameter(6, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN); // doc_type
				queryDoc.setParameter(7, OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA); // doc_vid
				queryDoc.setParameter(8, rnDoc[1]); // doc_date
				queryDoc.setParameter(9, otnosno); // otnosno
				queryDoc.setParameter(10, OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL); // valid
				queryDoc.setParameter(11, rnDoc[1]); // valid_date
				queryDoc.setParameter(12, SysConstants.CODE_ZNACHENIE_DA); // processed
				queryDoc.setParameter(13, SysConstants.CODE_ZNACHENIE_DA); // free_access
				queryDoc.setParameter(14, fileId == null ? 0 : 1); // count_files
				queryDoc.setParameter(15, OmbConstants.CODE_ZNACHENIE_COMPETENCE_OUR); // competence
				queryDoc.setParameter(16, docCU); // user_reg
				queryDoc.setParameter(17, docCD); // date_reg
				queryDoc.setParameter(18, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // pored_delo
				queryDoc.setParameter(19, docInfo); // doc_info
				queryDoc.executeUpdate();
			}

			StringBuilder jbpName = new StringBuilder();
			String t = SearchUtils.trimToNULL((String) row[11]); // Name
			if (t != null) {
				jbpName.append(t);
			}
			t = SearchUtils.trimToNULL((String) row[12]); // Surname
			if (t != null) {
				jbpName.append(" " + t);
			}
			t = SearchUtils.trimToNULL((String) row[13]); // Family
			if (t != null) {
				jbpName.append(" " + t);
			}
			if (jbpName.length() == 0) {
				jbpName.append("Анонимен");
			}

			String ekatte = SearchUtils.trimToNULL((String) row[15]); // ekatte
			Integer ekatteCode = null;
			if (ekatte != null) {
				ekatteCode = parseEkatte(ekatte);
				if (ekatteCode == null) {
					LOGGER.warn("  ! Document.ID=" + docId + " ! ERROR ekatte=" + ekatte);
				}
			}

			StringBuilder jbpAddr = new StringBuilder();
			t = SearchUtils.trimToNULL((String) row[18]); // AddrLine1
			if (t != null) {
				jbpAddr.append(t);
			}
			t = SearchUtils.trimToNULL((String) row[19]); // AddrLine2
			if (t != null) {
				if (jbpAddr.length() > 0) {
					jbpAddr.append("; ");
				}
				jbpAddr.append(t);
			}
			t = SearchUtils.trimToNULL((String) row[20]); // AddrLine3
			if (t != null) {
				if (jbpAddr.length() > 0) {
					jbpAddr.append("; ");
				}
				jbpAddr.append(t);
			}

			boolean bg = true;
			if (Boolean.TRUE.equals(SearchUtils.asBoolean(row[28]))) { // IsForeignCitizen
				bg = false;
			}

			Integer jbpPol = null;
			if (Boolean.FALSE.equals(SearchUtils.asBoolean(row[14]))) { // Gender
				jbpPol = 1; // мъж
			} else if (Boolean.TRUE.equals(SearchUtils.asBoolean(row[14]))) { // Gender
				jbpPol = 2; // жена
			}

			String countryName = null;
			if (row[17] != null && ((Number) row[17]).intValue() != 165) { // Country, само различни от БГ
				countryName = SearchUtils.trimToNULL((String) row[29]); // CountryName
			}

			Integer codeExpert = null;
			if (row[30] != null) { // AssignedTo_ID
				int xi = ((Number) row[30]).intValue();

				codeExpert = userMap.get(xi);
				if (codeExpert == null) {
//					codeExpert = -xi; // запазвам оригиналния но с минус
//					LOGGER.warn("  ! Dossier.ID=" + deloId + " ! UNKNOWN SpecifDossier.AssignedTo_ID=" + xi);
				}
			}

			Integer codeZveno = null;
			if (row[31] != null) { // Department_ID
				int xi = ((Number) row[31]).intValue();

				codeZveno = zvenoMap.get(xi);
				if (codeZveno == null) {
					codeZveno = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! Dossier.ID=" + deloId + " ! UNKNOWN SpecifDossier.AssignedTo_ID.Department_ID=" + xi);
				}
			}

			Integer finMethod = null;
			Integer dopust = OmbConstants.CODE_ZNACHENIE_DOPUST_DOPUST; // ако се определи на долу друго
			Integer osnNedopust = null;
			if (row[32] != null) { // ReasonToReject
				int xi = ((Number) row[32]).intValue();

				dopust = dopustMap.get(xi);
				if (dopust == null) {
					dopust = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! Dossier.ID=" + deloId + " ! UNKNOWN SpecifDossier.ReasonToReject=" + xi);
				}

				osnNedopust = osnNedopustMap.get(xi);
				if (osnNedopust == null) {
					osnNedopust = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! Dossier.ID=" + deloId + " ! UNKNOWN SpecifDossier.ReasonToReject=" + xi);
				}
			} else {
				// Ако SpecifDossier.ReasonToReject is null и SpecifDossier.ViolationType_ID = 828 (техен код за Липса на
				// конкретно нарушение), не се гледа мапинга, а се определя Начин на финализиране = 4 - Без констатирано нарушение
				if (row[40] != null && ((Number) row[40]).intValue() == 828) { // ViolationType_ID
					finMethod = 4;
				}
			}

			Date sastDate;
			if (row[36] != null) { // ClosingDate
				sastDate = (Date) row[36];
			} else {
				sastDate = (Date) rnDoc[1];
			}

			Integer corruption = null;
			if (Boolean.TRUE.equals(SearchUtils.asBoolean(row[37]))) { // IsCorruption
				corruption = SysConstants.CODE_ZNACHENIE_DA;
			} else if (Boolean.FALSE.equals(SearchUtils.asBoolean(row[37]))) { // IsCorruption
				corruption = SysConstants.CODE_ZNACHENIE_NE;
			}

			String jalbaText = (String) row[38]; // ViolationDescription
			if (jalbaText == null) {
				jalbaText = "";
			}
			String requestText = (String) row[39]; // Request
			if (requestText == null) {
				requestText = "";
			}

			Integer zasPrava = null;
			Integer vidOpl = null;
			if (row[40] != null) { // ViolationType_ID
				int xi = ((Number) row[40]).intValue();

				zasPrava = zasPravaMap.get(xi);
				if (zasPrava == null) {
					zasPrava = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! Dossier.ID=" + deloId + " ! UNKNOWN SpecifDossier.ViolationType_ID=" + xi);
				}

				vidOpl = vidOplMap.get(xi);
				if (vidOpl == null) {
					vidOpl = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! Dossier.ID=" + deloId + " ! UNKNOWN SpecifDossier.ViolationType_ID=" + xi);
				}
			}

			if (finMethod == null && row[41] != null) { // Status_ID, но все още да не е определен
				int xi = ((Number) row[41]).intValue();

				finMethod = jalbaFinMap.get(xi);
				if (finMethod == null) {
					finMethod = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! Dossier.ID=" + deloId + " ! UNKNOWN SpecifDossier.Status_ID=" + xi);
				}
			}

			if (saveDoc) {
				queryDocJalba.setParameter(1, docId * -1); // doc_id
				queryDocJalba.setParameter(2, OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL); // jbp_type
				queryDocJalba.setParameter(3, jbpName.toString().trim()); // jbp_name
				queryDocJalba.setParameter(4, row[10] == null ? null : String.valueOf(row[10])); // jbp_egn
				queryDocJalba.setParameter(5, new TypedParameterValue(StandardBasicTypes.INTEGER, ekatteCode)); // jbp_ekatte
				queryDocJalba.setParameter(6, row[16] == null ? null : String.valueOf(row[16])); // jbp_post
				queryDocJalba.setParameter(7, (countryName != null ? countryName + " " : "") + jbpAddr.toString().trim()); // jbp_addr
				queryDocJalba.setParameter(8, row[21]); // jbp_phone
				queryDocJalba.setParameter(9, row[22]); // jbp_email
				queryDocJalba.setParameter(10, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // code_nar - има пост
																											// обработка
				queryDocJalba.setParameter(11, new TypedParameterValue(StandardBasicTypes.INTEGER, zasPrava)); // zas_prava
				queryDocJalba.setParameter(12, new TypedParameterValue(StandardBasicTypes.INTEGER, vidOpl)); // vid_opl
				queryDocJalba.setParameter(13, jalbaText); // jalba_text + друг нарушител?
				queryDocJalba.setParameter(14, requestText); // request_text
				queryDocJalba.setParameter(15, ""); // regist_comment
				queryDocJalba.setParameter(16, new TypedParameterValue(StandardBasicTypes.INTEGER, codeZveno)); // code_zveno
				queryDocJalba.setParameter(17, new TypedParameterValue(StandardBasicTypes.INTEGER, codeExpert)); // code_expert
				queryDocJalba.setParameter(18, OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED); // sast
				queryDocJalba.setParameter(19, sastDate); // sast_date
				queryDocJalba.setParameter(20, new TypedParameterValue(StandardBasicTypes.TIMESTAMP, null)); // srok
				queryDocJalba.setParameter(21, new TypedParameterValue(StandardBasicTypes.INTEGER, dopust)); // dopust
				queryDocJalba.setParameter(22, new TypedParameterValue(StandardBasicTypes.INTEGER, osnNedopust)); // osn_nedopust
				queryDocJalba.setParameter(23, SysConstants.CODE_ZNACHENIE_DA); // public_visible
				queryDocJalba.setParameter(24, new TypedParameterValue(StandardBasicTypes.INTEGER, finMethod)); // fin_method
				queryDocJalba.setParameter(25, new TypedParameterValue(StandardBasicTypes.INTEGER, corruption)); // corruption
				queryDocJalba.setParameter(26, SysConstants.CODE_ZNACHENIE_NE); // inst_check - има пост обработка
				queryDocJalba.setParameter(27, ""); // inst_names - има пост обработка
				queryDocJalba.setParameter(28, new TypedParameterValue(StandardBasicTypes.INTEGER, bg ? 37 : null)); // jbp_grj
				queryDocJalba.setParameter(29, rnDoc[1]); // submit_date
				queryDocJalba.setParameter(30, SysConstants.CODE_ZNACHENIE_NE); // jbp_hidden
				queryDocJalba.setParameter(31, docCU); // user_last_mod
				queryDocJalba.setParameter(32, docCD); // date_last_mod
				queryDocJalba.setParameter(33, new TypedParameterValue(StandardBasicTypes.INTEGER, jbpPol)); // jbp_pol
				queryDocJalba.setParameter(34, new TypedParameterValue(StandardBasicTypes.TIMESTAMP, row[35])); // date_nar
				queryDocJalba.executeUpdate();
			}

			Date dossCD;
			if (row[3] != null) { // DossCD
				dossCD = (Date) row[3];
			} else {
				dossCD = docCD;
			}
			Integer dossCU;
			if (row[4] != null) { // DossCU
				int xi = ((Number) row[4]).intValue();

				if (xi == 3634) {
					dossCU = docCU;
				} else {
					dossCU = userMap.get(xi);
					if (dossCU == null) {
						dossCU = -xi; // запазвам оригиналния но с минус
						LOGGER.warn("  ! Dossier.ID=" + deloId + " ! UNKNOWN CU=" + xi);
					}
				}

			} else {
				dossCU = docCU;
			}

			Object[] rnDelo = parseCustomId((String) row[1]); // RN_Doss
			if (rnDelo[0] == null) {
				rnDelo[0] = String.valueOf(deloId); // взимам ИД-то
				LOGGER.warn("  ! Dossier.ID=" + deloId + " ! ERROR Custom_ID/Number=" + row[1]);
			}
			if (rnDelo[1] == null) {
				rnDelo[1] = dossCD; // взимам датата на реда
				LOGGER.warn("  ! Dossier.ID=" + deloId + " ! ERROR Custom_ID/Date=" + row[1]);
			}

			GregorianCalendar deloYear = new GregorianCalendar();
			deloYear.setTime((Date) rnDelo[1]);

			StringBuilder deloName = new StringBuilder();
			t = SearchUtils.trimToNULL((String) row[2]); // DossName
			if (t != null) {
				deloName.append(t + "\n");
			}
			deloName.append("Жалбоподател: " + jbpName);

			queryDelo.setParameter(1, deloId * -1); // delo_id
			queryDelo.setParameter(2, T0Start.REGISTRATURA); // registratura_id
			queryDelo.setParameter(3, docId * -1); // init_doc_id
			queryDelo.setParameter(4, rnDelo[0]); // rn_delo
			queryDelo.setParameter(5, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // rn_pored
			queryDelo.setParameter(6, rnDelo[1]); // delo_date
			queryDelo.setParameter(7, OmbConstants.CODE_ZNACHENIE_DELO_TYPE_JALBA); // delo_type
			queryDelo.setParameter(8, SysConstants.CODE_ZNACHENIE_DA); // free_access
			queryDelo.setParameter(9, OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED); // status
			queryDelo.setParameter(10, sastDate); // status_date
			queryDelo.setParameter(11, SysConstants.CODE_ZNACHENIE_DA); // with_section
			queryDelo.setParameter(12, SysConstants.CODE_ZNACHENIE_NE); // with_tom
			queryDelo.setParameter(13, 1); // br_tom
			queryDelo.setParameter(14, 250); // max_br_sheets
			queryDelo.setParameter(15, deloYear.get(Calendar.YEAR)); // delo_year
			queryDelo.setParameter(16, deloName.toString()); // delo_name
			queryDelo.setParameter(17, dossCU); // user_reg
			queryDelo.setParameter(18, dossCD); // date_reg
			queryDelo.executeUpdate();

			int deloDocId = ((Number) row[9]).intValue(); // DossDoc_ID

			queryDeloDoc.setParameter(1, deloDocId * -1); // id
			queryDeloDoc.setParameter(2, deloId * -1); // delo_id
			queryDeloDoc.setParameter(3, docId * -1); // input_doc_id
			queryDeloDoc.setParameter(4, docCD); // input_date
			queryDeloDoc.setParameter(5, OmbConstants.CODE_ZNACHENIE_DELO_SECTION_OFFICIAL); // section_type
			queryDeloDoc.setParameter(6, 1); // tom_nomer
			queryDeloDoc.setParameter(7, 1); // ekz_nomer
			queryDeloDoc.setParameter(8, docCU); // user_reg
			queryDeloDoc.setParameter(9, docCD); // date_reg
			queryDeloDoc.executeUpdate();

			if (fileId != null && saveDoc) {
				Date fileCD;
				if (row[26] != null) { // FileCD
					fileCD = (Date) row[26];
				} else {
					fileCD = docCD;
				}
				Integer fileCU;
				if (row[27] != null) { // FileCU
					int xi = ((Number) row[27]).intValue();

					fileCU = userMap.get(xi);
					if (fileCU == null) {
						fileCU = -xi; // запазвам оригиналния но с минус
						LOGGER.warn("  ! DocFiles.ID=" + fileId + " ! UNKNOWN CU=" + xi);
					}
				} else {
					fileCU = docCU;
				}

				String filename = parseFilename((String) row[6], (String) row[25], (String) row[24]);

				queryFiles.setParameter(1, fileId * -1); // file_id
				queryFiles.setParameter(2, filename); // filename
				queryFiles.setParameter(3, null); // content_type
				queryFiles.setParameter(4, null); // content - накрая ще се точат и файловете
				queryFiles.setParameter(5, fileCU); // user_reg
				queryFiles.setParameter(6, fileCD); // date_reg
				queryFiles.executeUpdate();

				queryFileObject.setParameter(1, fileId * -1); // id
				queryFileObject.setParameter(2, docId * -1); // object_id
				queryFileObject.setParameter(3, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC); // object_code
				queryFileObject.setParameter(4, fileId * -1); // file_id
				queryFileObject.setParameter(5, fileCU); // user_reg
				queryFileObject.setParameter(6, fileCD); // date_reg
				queryFileObject.executeUpdate();
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
		sourceMssql.closeConnection();
		dest.closeConnection();

		LOGGER.info("  " + insertCnt);
		LOGGER.info("End loading DossiersWithInitDocs from omb_mssql.");
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
//		sql.append(" union all ");
//		sql.append(" select 'SEQ_DELO' SEQ_NAME, max (delo_id) MAX_ID from DELO ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_DOC_DVIJ' SEQ_NAME, max (id) MAX_ID from DOC_DVIJ ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_DOC_REFERENTS' SEQ_NAME, max (id) MAX_ID from DOC_REFERENTS ");
//		sql.append(" union all ");
//		sql.append(" select 'SEQ_DOC' SEQ_NAME, max (doc_id) MAX_ID from DOC ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_DOC_SAST_HISTORY' SEQ_NAME, max (id) MAX_ID from DOC_SAST_HISTORY ");
		sql.append(" union all ");
		sql.append(" select 'SEQ_FILE_OBJECTS' SEQ_NAME, max (id) MAX_ID from FILE_OBJECTS ");
//		sql.append(" union all ");
//		sql.append(" select 'SEQ_FILES' SEQ_NAME, max (file_id) MAX_ID from FILES ");
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
	 * Сетва разгледана от други институции и имената от Стар Регистър жалби/Деловодна система
	 */
	void updateJalbaInst(JPA sourceMssql, JPA dest, int fromDocId) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start UpdateJalbaInst from omb_mssql...");

		int updateCnt = 0;

		Query updateInst = dest.getEntityManager().createNativeQuery( //
			"update doc_jalba set inst_check = 1, inst_names = coalesce(inst_names,'') || ?1 where doc_id = ?2");

		StringBuilder sql = new StringBuilder();
		sql.append(" select c.ID, d.InitDoc_ID, c.Organization_ID, o.Name ");
		sql.append(" from DossierPartiesConcerned c inner join Dossier d on d.ID = c.Dossier_ID ");
		sql.append(" inner join Organization o on o.ID = c.Organization_ID ");
		sql.append(" where d.InitDoc_ID > ?1 and c.PartyRole_ID = 63 ");
		sql.append(" order by d.InitDoc_ID, c.ID ");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceMssql.getEntityManager().createNativeQuery( //
			sql.toString()).setParameter(1, fromDocId) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			int docId = ((Number) row[1]).intValue(); // InitDoc_ID

			String narName = SearchUtils.trimToNULL((String) row[3]);
			if (narName != null) {
				updateInst.setParameter(1, narName + "; "); // inst_names
				updateInst.setParameter(2, docId * -1); // doc_id
				updateInst.executeUpdate();
			}

			updateCnt++;

			if (updateCnt % 500 == 0) {
				dest.commit();
				dest.begin();
				LOGGER.info("  " + updateCnt);
			}
		}
		dest.commit(); // за последните които не са кратни на 500

		stream.close();
		sourceMssql.closeConnection();
		dest.closeConnection();

		LOGGER.info("  " + updateCnt);
		LOGGER.info("End UpdateJalbaInst from omb_mssql.");
	}

	/**
	 * Сетва нарушители и дописва имената ако са повече от един от Стар Регистър жалби/Деловодна система
	 */
	void updateJalbaNar(JPA sourceMssql, JPA dest, int fromDocId) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start UpdateJalbaNar from omb_mssql...");

		Map<Integer, Integer> narMap = T0Start.findDecodeObjectMap(dest, CODE_ZNACHENIE_JOURNAL_REFERENT * -1, T0Start.SRC_MSSQL);

		int updateCnt = 0;

		Query updateNar = dest.getEntityManager().createNativeQuery("update doc_jalba set code_nar = ?1 where doc_id = ?2");

		Query updateNarDop = dest.getEntityManager().createNativeQuery( //
			"update doc_jalba set jalba_text = coalesce(jalba_text,'') || ?1 where doc_id = ?2");

		StringBuilder sql = new StringBuilder();
		sql.append(" select c.ID, d.InitDoc_ID, c.Organization_ID, o.Name ");
		sql.append(" from DossierPartiesConcerned c inner join Dossier d on d.ID = c.Dossier_ID ");
		sql.append(" inner join Organization o on o.ID = c.Organization_ID ");
		sql.append(" where d.InitDoc_ID > ?1 and c.PartyRole_ID = 62 ");
		sql.append(" order by d.InitDoc_ID, c.ID ");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceMssql.getEntityManager().createNativeQuery( //
			sql.toString()).setParameter(1, fromDocId) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();

		int lastDocId = Integer.MIN_VALUE;

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			int docId = ((Number) row[1]).intValue(); // InitDoc_ID

			if (docId != lastDocId) {
				lastDocId = docId;

				Integer codeNar;
				if (row[2] != null) { // Organization_ID
					int xi = ((Number) row[2]).intValue();

					codeNar = narMap.get(xi);
					if (codeNar == null) {
						codeNar = -xi; // запазвам оригиналния но с минус
						LOGGER.warn("  ! DossierPartiesConcerned.ID=" + row[0] + " ! UNKNOWN Organization_ID=" + xi);
					}
				} else {
					continue; // за всеки случай
				}

				updateNar.setParameter(1, codeNar); // code_nar
				updateNar.setParameter(2, docId * -1); // doc_id
				updateNar.executeUpdate();

			} else {
				String narName = SearchUtils.trimToNULL((String) row[3]);
				if (narName != null) {
					updateNarDop.setParameter(1, "\nДруг нарушител: " + narName); // jalba_text
					updateNarDop.setParameter(2, docId * -1); // doc_id
					updateNarDop.executeUpdate();
				}
			}

			updateCnt++;

			if (updateCnt % 500 == 0) {
				dest.commit();
				dest.begin();
				LOGGER.info("  " + updateCnt);
			}
		}
		dest.commit(); // за последните които не са кратни на 500

		stream.close();
		sourceMssql.closeConnection();
		dest.closeConnection();

		LOGGER.info("  " + updateCnt);
		LOGGER.info("End UpdateJalbaNar from omb_mssql.");
	}

	private Object[] parseCustomId(String customId) {
		Object[] result = new Object[2];

		customId = SearchUtils.trimToNULL(customId);
		if (customId != null) {
			try {
				String[] xs = customId.split("/");
				result[0] = xs[0].trim();
				result[1] = SDF.parse(xs[1].trim());
			} catch (Exception e) {
				LOGGER.error("Грешка при parse на номер на док/дело!", e);
			}
		}
		return result;
	}

	private Integer parseEkatte(String ekatte) {
		try {
			int delimIndex = ekatte.indexOf('-');
			if (delimIndex == -1) {
				return Integer.parseInt(ekatte);
			}
			return Integer.parseInt(ekatte.substring(0, delimIndex).trim());
		} catch (Exception e) {
			LOGGER.error("Грешка при parse на ЕКАТТЕ!", e);
		}
		return null;
	}

	private String parseFilename(String customId, String cvt, String ext) {
		cvt = SearchUtils.trimToNULL(cvt);
		ext = SearchUtils.trimToNULL(ext);
		try {
			if (cvt != null) {
				return cvt.substring(cvt.lastIndexOf("\\") + 1, cvt.length());
			} else if (ext != null) {
				return String.valueOf(customId).replace('/', '-') + "." + ext;
			}
		} catch (Exception e) {
			LOGGER.error("Грешка при parse на cvtFileName!", e);
		}
		return String.valueOf(customId).replace('/', '-');
	}
}

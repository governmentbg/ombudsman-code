package com.ib.omb.transform;

import java.sql.SQLException;
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
 * Трансфер на документи от деловодната система
 *
 * @author belev
 */
public class T3Delovodna {

	private static final Logger LOGGER = LoggerFactory.getLogger(T3Delovodna.class);

	private static final List<Object[]> REGISTRI = new ArrayList<>();

	private static final Map<String, Integer> REGISTRI_MAP = new HashMap<>();

	static {
		REGISTRI.add(new Object[] { "12", 11, "Агенции", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "15", 12, "Асоциации", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "17", 13, "Банки", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "19", 14, "Бюра", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "80", 15, "Вътрешни кореспонденти", OmbConstants.CODE_ZNACHENIE_REGISTER_DNEV });
		REGISTRI.add(new Object[] { "94", 16, "Граждани", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "24", 17, "Дирекции", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "ДФЛ", 18, "Договори с физически лица", OmbConstants.CODE_ZNACHENIE_REGISTER_DNEV });
		REGISTRI.add(new Object[] { "ДЮЛ", 19, "Договори с юридически лица", OmbConstants.CODE_ZNACHENIE_REGISTER_DNEV });
		REGISTRI.add(new Object[] { "26", 20, "Дружества", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "РД-13", 21, "Заповеди за командировка в страната", OmbConstants.CODE_ZNACHENIE_REGISTER_DNEV });
		REGISTRI.add(new Object[] { "РД-12", 22, "Заповеди за командировка в чужбина", OmbConstants.CODE_ZNACHENIE_REGISTER_DNEV });
		REGISTRI.add(new Object[] { "РД-08", 23, "Заповеди по дейността", OmbConstants.CODE_ZNACHENIE_REGISTER_DNEV });
		REGISTRI.add(new Object[] { "33", 24, "Институти", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "27", 25, "Камари", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "20", 26, "Кантори", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "37", 27, "Комисии", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "05", 28, "Комитети", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "64", 29, "Медии /радио, телевизия, издателства, редакции , вестници , списания  и др./", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "04", 30, "Министерства", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "45", 31, "Музеи", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "06", 32, "Областни администрации", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "13", 33, "Обществени посредници", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "08", 34, "Общини", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "11", 35, "Органи на въдебната власт", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "48", 36, "Организации", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "01", 37, "Партии", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "54", 38, "Посолства и представителства", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "53", 39, "Предприятия", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "02", 40, "Президенство , Народно събрание", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "09", 41, "Сметна Палата", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "61", 42, "Съвети", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "62", 43, "Съюзи", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "67", 44, "Училища, университети и др. учебни заведения", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "47", 45, "Фирми", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "69", 46, "Фондации", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "36", 47, "Фондове", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });
		REGISTRI.add(new Object[] { "74", 48, "Центрове", OmbConstants.CODE_ZNACHENIE_REGISTER_COR_GR });

		for (Object[] register : REGISTRI) {
			REGISTRI_MAP.put((String) register[0], (Integer) register[1]);
		}
	}

	private int		transferUser;
	private Date	transferDate;

	/**
	 * @param transferUser
	 * @param transferDate
	 */
	public T3Delovodna(int transferUser, Date transferDate) {
		this.transferUser = transferUser;
		this.transferDate = transferDate;
	}

	/**
	 * Методът
	 *
	 * @param sourceEdsd
	 * @param dest
	 * @throws BaseException
	 * @throws SQLException
	 */
	public void transfer(JPA sourceEdsd, JPA dest) throws BaseException, SQLException {
		@SuppressWarnings("unchecked")
		List<Object[]> rows = dest.getEntityManager().createNativeQuery( //
			"select clazz, start_time, end_time from transfer_process where clazz = ?1") //
			.setParameter(1, getClass().getSimpleName()).getResultList();

		if (rows.isEmpty()) { // първо пускане
			clearRelatedData(dest); // зачистване на всичко свързано с този клас

			insertRegisters(dest); // малко повече регистри

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
		// ИД-то от деловдноата система стови в delo.user_last_mod - като после ще се чисти
		Object[] maxIds = (Object[]) dest.getEntityManager().createNativeQuery( //
			"select max(delo_id) delo_id, max(user_last_mod) prepId from delo where delo_id >= 7000 and delo_id <= 100000") //
			.getResultList().get(0);
		int deloId = maxIds[0] != null ? ((Number) maxIds[0]).intValue() : 7000;
		int fromPrepId = maxIds[1] != null ? ((Number) maxIds[1]).intValue() : 0;

		loadPrepiski(sourceEdsd, dest, fromPrepId, deloId);

		T0Start.wait(5);
		// ИД-тата на документа в деловодната система съвпада с нашата
		Number maxDocId = (Number) dest.getEntityManager().createNativeQuery( //
			"select max(doc_id) from doc where doc_id >= 7000 and doc_id <= 100000") //
			.getResultList().get(0);
		int fromDocId = maxDocId != null ? maxDocId.intValue() : 0;

		loadDocuments(sourceEdsd, dest, fromDocId);

		@SuppressWarnings("unchecked")
		List<Object[]> counters = dest.getEntityManager().createNativeQuery( //
			"select d.register_id, max(d.rn_pored) from doc d where d.register_id in (?1) and d.doc_date >= ?2 group by d.register_id") //
			.setParameter(1, REGISTRI_MAP.values()).setParameter(2, DateUtils.parse("01.01.2022")).getResultList();

		Query updateRegister = dest.getEntityManager().createNativeQuery( //
			"update registri set act_nomer = ?1 where register_id = ?2");

		dest.begin();
		for (Object[] counter : counters) {
			int nomer = 1;
			if (counter[1] != null) {
				nomer += ((Number) counter[1]).intValue();
			}
			updateRegister.setParameter(1, nomer);
			updateRegister.setParameter(2, counter[0]);
			updateRegister.executeUpdate();
		}
		dest.commit();

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

		// смятам че документите на деловодната ще се вместят в диапазона

		int cnt = T0Start.deleteBigTable(dest, "DELO_DOC", "id", "delo_id >= 7000 and delo_id <= 100000");
		LOGGER.info("   deleted " + cnt + " rows from table DELO_DOC");

		cnt = T0Start.deleteBigTable(dest, "DELO", "delo_id", "delo_id >= 7000 and delo_id <= 100000");
		LOGGER.info("   deleted " + cnt + " rows from table DELO");

		cnt = T0Start.deleteBigTable(dest, "DOC_DVIJ", "id", "doc_id >= 7000 and doc_id <= 100000");
		LOGGER.info("   deleted " + cnt + " rows from table DOC_DVIJ");

		cnt = T0Start.deleteBigTable(dest, "DOC_REFERENTS", "id", "doc_id >= 7000 and doc_id <= 100000");
		LOGGER.info("   deleted " + cnt + " rows from table DOC_REFERENTS");

		cnt = T0Start.deleteBigTable(dest, "DOC", "doc_id", "doc_id >= 7000 and doc_id <= 100000");
		LOGGER.info("   deleted " + cnt + " rows from table DOC");

		List<Integer> registersToDelete = new ArrayList<>();
		registersToDelete.add(T0Start.REGISTER_DELOVODNA_DOC);
		registersToDelete.addAll(REGISTRI_MAP.values());

		dest.begin();
		cnt = dest.getEntityManager().createNativeQuery("delete from registri where register_id in (?1)") //
			.setParameter(1, registersToDelete).executeUpdate();
		dest.commit();
		LOGGER.info("   deleted " + cnt + " rows from table REGISTI");
	}

	/**
	 * Зарежда регистрите и х-ки по вид документ свъразни с деловодната
	 */
	void insertRegisters(JPA dest) throws DbErrorException {
		Query query = dest.getEntityManager().createNativeQuery( //
			"insert into REGISTRI (REGISTER_ID, REGISTRATURA_ID, REGISTER, REGISTER_TYPE, VALID, COMMON, DOC_TYPE" //
				+ " , PREFIX, ALG, BEG_NOMER, ACT_NOMER, STEP, USER_REG, DATE_REG)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14)");

		dest.begin();

//		Мигрирани документи от деловодство
		query.setParameter(1, T0Start.REGISTER_DELOVODNA_DOC); // REGISTER_ID
		query.setParameter(2, T0Start.REGISTRATURA); // REGISTRATURA_ID
		query.setParameter(3, "Мигрирани документи от деловодство"); // REGISTER
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
		query.executeUpdate();

		// + определените регистри от анализа на миграцията
		for (Object[] register : REGISTRI) {
			query.setParameter(1, register[1]); // REGISTER_ID
			query.setParameter(2, T0Start.REGISTRATURA); // REGISTRATURA_ID
			query.setParameter(3, register[2]); // REGISTER
			query.setParameter(4, register[3]); // REGISTER_TYPE
			query.setParameter(5, SysConstants.CODE_ZNACHENIE_DA); // VALID
			query.setParameter(6, SysConstants.CODE_ZNACHENIE_NE); // COMMON
			query.setParameter(7, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // DOC_TYPE
			query.setParameter(8, register[0]); // PREFIX
			query.setParameter(9, OmbConstants.CODE_ZNACHENIE_ALG_INDEX_STEP); // ALG
			query.setParameter(10, 1); // BEG_NOMER
			query.setParameter(11, 1); // ACT_NOMER
			query.setParameter(12, 1); // STEP
			query.setParameter(13, this.transferUser); // USER_REG
			query.setParameter(14, DateUtils.systemMinDate()); // DATE_REG
			query.executeUpdate();
		}

		dest.commit();
	}

	/**
	 * Прехвърля документите
	 */
	void loadDocuments(JPA sourceEdsd, JPA dest, int fromDocId) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start loading documents from ombucman_edsd...");

		Date minDate = DateUtils.systemMinDate();
		Map<Integer, Integer> userMap = T0Start.findDecodeObjectMap(dest, Constants.CODE_ZNACHENIE_JOURNAL_USER, T0Start.SRC_EDSD);
		Map<Integer, Integer> correspMap = T0Start.findDecodeObjectMap(dest, OmbConstants.CODE_ZNACHENIE_JOURNAL_REFERENT, T0Start.SRC_EDSD);
		Map<Integer, Integer> docVidMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_DOC_VID, T0Start.SRC_EDSD);

		Number maxDeloDoc = (Number) dest.getEntityManager().createNativeQuery("select max(id) from delo_doc").getResultList().get(0);
		int deloDocId = maxDeloDoc != null ? maxDeloDoc.intValue() : 0;

		Number maxDocDvij = (Number) dest.getEntityManager().createNativeQuery("select max(id) from doc_dvij").getResultList().get(0);
		int docDvijId = maxDocDvij != null ? maxDocDvij.intValue() : 0;

		@SuppressWarnings("unchecked")
		Stream<Object[]> streamDeloMap = dest.getEntityManager().createNativeQuery( //
			"select user_last_mod prep_id, delo_id from delo where delo_id >= 7000 and delo_id <= 100000 and user_last_mod is not null") //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();
		Map<Integer, Integer> deloMap = new HashMap<>();
		Iterator<Object[]> deloIter = streamDeloMap.iterator();
		while (deloIter.hasNext()) {
			Object[] row = deloIter.next();

			int key = ((Number) row[0]).intValue();
			int value = ((Number) row[1]).intValue();

			if (deloMap.containsKey(key)) {
				LOGGER.warn("streamDeloMap duplicate key=" + key);
			}
			deloMap.put(key, value);
		}
		streamDeloMap.close();
		LOGGER.info("!found " + deloMap.size() + " prepiski!");

		@SuppressWarnings("unchecked")
		Stream<Object[]> streamCorrespNamesMap = dest.getEntityManager().createNativeQuery( //
			"select distinct r.code, r.ref_name from transfer_table tt inner join adm_referents r on r.code = tt.dest_id" //
				+ " where tt.src = ?1 and tt.code_object = ?2") //
			.setParameter(1, T0Start.SRC_EDSD).setParameter(2, OmbConstants.CODE_ZNACHENIE_JOURNAL_REFERENT) //
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

		Query queryDoc = dest.getEntityManager().createNativeQuery("insert into doc" //
			+ " (doc_id, registratura_id, register_id, rn_doc, rn_prefix, rn_pored, doc_type, doc_vid, doc_date, otnosno" //
			+ ", valid, valid_date, processed, free_access, count_files, doc_info, pored_delo, user_reg, date_reg, receive_date, receive_method, code_ref_corresp)" //
			+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20, ?21, ?22)");

		Query queryDeloDoc = dest.getEntityManager().createNativeQuery( //
			"insert into delo_doc (id, delo_id, input_doc_id, input_date, section_type, pored, tom_nomer, ekz_nomer, user_reg, date_reg)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)");

		Query updateDeloInit = dest.getEntityManager().createNativeQuery( //
			"update delo set init_doc_id = ?1 where delo_id = ?2 and init_doc_id is null");

		Query queryDocDvij = dest.getEntityManager().createNativeQuery( //
			"insert into doc_dvij (id, doc_id, ekz_nomer, code_ref, dvij_date, dvij_method, dvij_text, status, status_date, user_reg, date_reg)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11)");

		Query updateDocInfo = dest.getEntityManager().createNativeQuery( //
			"update doc set doc_info = coalesce(doc_info,'') || ?1 where doc_id = ?2");

		StringBuilder select = new StringBuilder();
		select.append(" select d.doc_id, d.doc_vid_id, d.doc_type, d.doc_delivery "); // 0,1,2,3
		select.append(" , d.doc_uri_reg_index, d.doc_uri_number, d.doc_uri_date, d.doc_subject "); // 4,5,6,7
		select.append(" , d.doc_date, d.doc_year, d.doc_priority, d.doc_kor_index, d.sfield "); // 8,9,10,11,12
		select.append(" , pd.prep_id, pd.time_included, pd.number, u.usr_id "); // 13,14,15,16
		select.append(" , dk.dko_id, dk.kor_id, dk.is_primary "); // 17,18,19
		select.append(" from documents d ");
		select.append(" left outer join prep_razdeli_docs pd on pd.doc_id = d.doc_id ");
		select.append(" left outer join users u on u.korespondents_kor_id = pd.whoadd_kor_id ");
		select.append(" left outer join doc_kors dk on dk.doc_id = d.doc_id ");
		select.append(" where d.doc_id > ?1 ");
		select.append(" order by d.doc_id, dk.dko_id ");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceEdsd.getEntityManager().createNativeQuery( //
			select.toString()).setParameter(1, fromDocId) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();

		int currentDocId = 0;
		Set<Integer> correspSet = new HashSet<>();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			int docId = ((Number) row[0]).intValue();

			Integer userReg;
			if (row[16] != null) { // usr_id
				int xi = ((Number) row[16]).intValue();

				userReg = userMap.get(xi);
				if (userReg == null) {
					userReg = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! documents.doc_id=" + docId + " ! UNKNOWN usr_id=" + xi);
				}
			} else {
				userReg = this.transferUser;
			}

			Date docDate;
			if (row[6] != null) { // doc_uri_date
				docDate = (Date) row[6];
			} else {
				docDate = minDate;
			}
			Date dateReg;
			if (row[8] != null) { // doc_date
				dateReg = (Date) row[8];
			} else {
				dateReg = docDate;
			}

			Integer korId = null;
			if (row[18] != null) { // kor_id
				int xi = ((Number) row[18]).intValue();

				korId = correspMap.get(xi);
				if (korId == null) {
//					korId = -xi; // запазвам оригиналния но с минус
//					LOGGER.info("  ! documents.doc_id=" + docId + " ! UNKNOWN kor_id=" + xi);
				}
			}

			if (currentDocId != docId) { // размножени са по кореспонденти и за това за док само първия път запис

				currentDocId = docId;
				correspSet.clear(); // на ново да се трупат

				String prefix = SearchUtils.trimToNULL((String) row[4]); // doc_uri_reg_index
				Integer pored = null;
				if (row[5] != null) { // doc_uri_number
					pored = ((Number) row[5]).intValue();
				}
				StringBuilder rn = new StringBuilder();
				if (prefix != null) {
					rn.append(prefix);
				}
				if (pored != null) {
					if (rn.length() > 0) {
						rn.append("-");
					}
					rn.append(pored);
				}

				int docType = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN;
				Date receiveDate = null;
				Integer receiveMethod = null;
				Integer codeRefCorresp = null;

				if (row[2] != null) { // doc_type
					int xi = ((Number) row[2]).intValue(); // вътрешен=1, входящ=2, изходящ=3
					if (xi == 2) {
						docType = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN;
						receiveDate = docDate;
						receiveMethod = OmbConstants.CODE_ZNACHENIE_PREDAVANE_POSHTA;
						codeRefCorresp = korId;
					}
				}

				String otnosno;
				if (row[7] != null) { // doc_subject
					otnosno = Jsoup.parse((String) row[7]).wholeText();
				} else {
					otnosno = "";
				}

				Integer docVid;
				if (row[1] != null) { // doc_vid_id
					int xi = ((Number) row[1]).intValue();

					docVid = docVidMap.get(xi);
					if (docVid == null) {
						docVid = -xi; // запазвам оригиналния но с минус
						LOGGER.warn("  ! documents.doc_id=" + docId + " ! UNKNOWN doc_vid_id=" + xi);
					}
				} else {
					docVid = 21; // задължително е в БД
				}

				Integer poredDelo = null;
				if (row[15] != null) { // number
					poredDelo = ((Number) row[15]).intValue();
				}

				Integer registerId;
				if (prefix != null) {
					registerId = REGISTRI_MAP.get(prefix);

					if (registerId == null) {
						registerId = T0Start.REGISTER_DELOVODNA_DOC;
//						LOGGER.info("  ! documents.doc_id=" + docId + " ! UNKNOWN prefix=" + prefix);
					}
				} else {
					registerId = T0Start.REGISTER_DELOVODNA_DOC;
				}

				queryDoc.setParameter(1, docId); // doc_id
				queryDoc.setParameter(2, T0Start.REGISTRATURA); // registratura_id
				queryDoc.setParameter(3, registerId); // register_id
				queryDoc.setParameter(4, rn.length() > 0 ? rn.toString() : "МигД-" + docId); // rn_doc
				queryDoc.setParameter(5, prefix); // rn_prefix
				queryDoc.setParameter(6, new TypedParameterValue(StandardBasicTypes.INTEGER, pored)); // rn_pored
				queryDoc.setParameter(7, docType); // doc_type
				queryDoc.setParameter(8, docVid); // doc_vid
				queryDoc.setParameter(9, docDate); // doc_date
				queryDoc.setParameter(10, otnosno); // otnosno
				queryDoc.setParameter(11, SysConstants.CODE_ZNACHENIE_DA); // valid
				queryDoc.setParameter(12, docDate); // valid_date
				queryDoc.setParameter(13, SysConstants.CODE_ZNACHENIE_DA); // processed
				queryDoc.setParameter(14, SysConstants.CODE_ZNACHENIE_DA); // free_access
				queryDoc.setParameter(15, 0); // count_files
				queryDoc.setParameter(16, row[11]); // doc_info (doc_kor_index)
				queryDoc.setParameter(17, new TypedParameterValue(StandardBasicTypes.INTEGER, poredDelo)); // pored_delo
				queryDoc.setParameter(18, userReg); // user_reg
				queryDoc.setParameter(19, dateReg); // date_reg
				queryDoc.setParameter(20, new TypedParameterValue(StandardBasicTypes.TIMESTAMP, receiveDate)); // receive_date
				queryDoc.setParameter(21, new TypedParameterValue(StandardBasicTypes.INTEGER, receiveMethod)); // receive_method
				queryDoc.setParameter(22, new TypedParameterValue(StandardBasicTypes.INTEGER, codeRefCorresp)); // code_ref_corresp
				queryDoc.executeUpdate();

				if (row[13] != null) { // prep_id
					int xi = ((Number) row[13]).intValue();

					Integer deloId = deloMap.get(xi);
					if (deloId != null) {
						Date inputDate;
						if (row[14] != null) { // time_included
							inputDate = (Date) row[14];
						} else {
							inputDate = docDate;
						}

						queryDeloDoc.setParameter(1, ++deloDocId); // id
						queryDeloDoc.setParameter(2, deloId); // delo_id
						queryDeloDoc.setParameter(3, docId); // input_doc_id
						queryDeloDoc.setParameter(4, inputDate); // input_date
						queryDeloDoc.setParameter(5, OmbConstants.CODE_ZNACHENIE_DELO_SECTION_OFFICIAL); // section_type
						queryDeloDoc.setParameter(6, new TypedParameterValue(StandardBasicTypes.INTEGER, poredDelo)); // pored
						queryDeloDoc.setParameter(7, 1); // tom_nomer
						queryDeloDoc.setParameter(8, 1); // ekz_nomer
						queryDeloDoc.setParameter(9, userReg); // user_reg
						queryDeloDoc.setParameter(10, inputDate); // date_reg
						queryDeloDoc.executeUpdate();

						if (poredDelo != null && poredDelo.equals(1)) { // първият е иницииращ
							updateDeloInit.setParameter(1, docId); // init_doc_id
							updateDeloInit.setParameter(2, deloId); // delo_id
							updateDeloInit.executeUpdate();
						}

					} else {
						LOGGER.warn("  ! prep_razdeli_docs.doc_id=" + docId + " ! UNKNOWN prep_id=" + xi);
					}
				}
				insertCnt++;
			}

			// doc_type // // вътрешен=1, входящ=2, изходящ=3
			if (korId != null && row[2] != null && !correspSet.contains(korId)) { // има дублирания и за това

				correspSet.add(korId);

				int docType = ((Number) row[2]).intValue();

				String correspName = correspNamesMap.get(korId);
				if (correspName == null) {
					correspName = "";
				}

				if (docType == 1) { // за вътрешни трупам всички кореспонденти
					String append = correspSet.size() == 1 ? " Кореспонденти: " : ", ";

					updateDocInfo.setParameter(1, append + correspName); // append doc_info
					updateDocInfo.setParameter(2, docId); // doc_id
					updateDocInfo.executeUpdate();

				} else if (docType == 3) { // за изходящ правим движение

					queryDocDvij.setParameter(1, ++docDvijId); // id
					queryDocDvij.setParameter(2, docId); // doc_id
					queryDocDvij.setParameter(3, 1); // ekz_nomer
					queryDocDvij.setParameter(4, korId); // code_ref
					queryDocDvij.setParameter(5, docDate); // dvij_date
					queryDocDvij.setParameter(6, OmbConstants.CODE_ZNACHENIE_PREDAVANE_POSHTA); // dvij_method
					queryDocDvij.setParameter(7, correspName); // dvij_text
					queryDocDvij.setParameter(8, OmbConstants.DS_SENT); // status
					queryDocDvij.setParameter(9, dateReg); // status_date
					queryDocDvij.setParameter(10, userReg); // user_reg
					queryDocDvij.setParameter(11, dateReg); // date_reg
					queryDocDvij.executeUpdate();

				} else if (docType == 2 && correspSet.size() > 1) { // за входящ трупам останалите в допИнфо

					String append = correspSet.size() == 2 ? " Кореспонденти: " : ", ";

					updateDocInfo.setParameter(1, append + correspName); // append doc_info
					updateDocInfo.setParameter(2, docId); // doc_id
					updateDocInfo.executeUpdate();
				}
			}

			if (insertCnt % 500 == 0) {
				dest.commit();
				dest.begin();
				LOGGER.info("  " + insertCnt);
			}
		}
		dest.commit(); // за последните които не са кратни на 500

		stream.close();
		sourceEdsd.closeConnection();
		dest.closeConnection();

		LOGGER.info("  " + insertCnt);
		LOGGER.info("End loading documents from ombucman_edsd.");
	}

	/**
	 * Прехвърля преписките
	 */
	void loadPrepiski(JPA sourceEdsd, JPA dest, int fromPrepId, int deloId) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start loading prepiski from ombucman_edsd...");

		Date minDate = DateUtils.systemMinDate();
		Map<Integer, Integer> userMap = T0Start.findDecodeObjectMap(dest, Constants.CODE_ZNACHENIE_JOURNAL_USER, T0Start.SRC_EDSD);

		int insertCnt = 0;

		Query queryDelo = dest.getEntityManager().createNativeQuery( //
			"insert into delo (delo_id, registratura_id, init_doc_id, rn_delo, rn_prefix, rn_pored, delo_date, delo_type, free_access" //
				+ ", status, status_date, with_section, with_tom, br_tom, max_br_sheets, delo_year, delo_name" //
				+ ", user_reg, date_reg, user_last_mod)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15, ?16, ?17, ?18, ?19, ?20)"); //

		StringBuilder select = new StringBuilder();
		select.append(" select p.prep_id, p.prep_name, p.prep_subject, p.prep_time_created "); // 0,1,2,3
		select.append(" , p.uri_index, p.uri_number, p.uri_date, u.usr_id "); // 4,5,6,7,8
		select.append(" from prepiski p ");
		select.append(" left outer join prep_razdeli_docs pd on pd.prep_id = p.prep_id and pd.number = 1 ");
		select.append(" left outer join users u on u.korespondents_kor_id = pd.whoadd_kor_id ");
		select.append(" where p.prep_id > ?1 ");
		select.append(" order by p.prep_id ");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceEdsd.getEntityManager().createNativeQuery( //
			select.toString()).setParameter(1, fromPrepId) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			int prepId = ((Number) row[0]).intValue();

			String prefix = SearchUtils.trimToNULL((String) row[4]); // uri_index
			Integer pored = null;
			if (row[5] != null) { // uri_number
				pored = ((Number) row[5]).intValue();
			}
			StringBuilder rn = new StringBuilder();
			if (prefix != null) {
				rn.append(prefix);
			}
			if (pored != null) {
				if (rn.length() > 0) {
					rn.append("-");
				}
				rn.append(pored);
			}

			Integer userReg;
			if (row[7] != null) { // usr_id
				int xi = ((Number) row[7]).intValue();

				userReg = userMap.get(xi);
				if (userReg == null) {
					userReg = -xi; // запазвам оригиналния но с минус
					LOGGER.warn("  ! prepiski.prep_id=" + prepId + " ! UNKNOWN usr_id=" + xi);
				}
			} else {
				userReg = this.transferUser;
			}

			Date deloDate;
			if (row[6] != null) { // uri_date
				deloDate = (Date) row[6];
			} else {
				deloDate = minDate;
			}
			Date dateReg;
			if (row[3] != null) { // prep_time_created
				dateReg = (Date) row[3];
			} else {
				dateReg = deloDate;
			}
			GregorianCalendar deloYear = new GregorianCalendar();
			deloYear.setTime(deloDate);

			String deloName = null;
			if (row[1] != null) { // prep_name
				deloName = Jsoup.parse((String) row[1]).wholeText();
			}

			queryDelo.setParameter(1, ++deloId); // delo_id
			queryDelo.setParameter(2, T0Start.REGISTRATURA); // registratura_id
			queryDelo.setParameter(3, new TypedParameterValue(StandardBasicTypes.INTEGER, null)); // init_doc_id
			queryDelo.setParameter(4, rn.length() > 0 ? rn.toString() : "МигД-" + prepId); // rn_delo
			queryDelo.setParameter(5, prefix); // rn_prefix
			queryDelo.setParameter(6, new TypedParameterValue(StandardBasicTypes.INTEGER, pored)); // rn_pored
			queryDelo.setParameter(7, deloDate); // delo_date
			queryDelo.setParameter(8, OmbConstants.CODE_ZNACHENIE_DELO_TYPE_DOSS); // delo_type
			queryDelo.setParameter(9, SysConstants.CODE_ZNACHENIE_DA); // free_access
			queryDelo.setParameter(10, OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED); // status
			queryDelo.setParameter(11, this.transferDate); // status_date
			queryDelo.setParameter(12, SysConstants.CODE_ZNACHENIE_DA); // with_section
			queryDelo.setParameter(13, SysConstants.CODE_ZNACHENIE_NE); // with_tom
			queryDelo.setParameter(14, 1); // br_tom
			queryDelo.setParameter(15, 250); // max_br_sheets
			queryDelo.setParameter(16, deloYear.get(Calendar.YEAR)); // delo_year
			queryDelo.setParameter(17, deloName); // delo_name
			queryDelo.setParameter(18, userReg); // user_reg
			queryDelo.setParameter(19, dateReg); // date_reg
			queryDelo.setParameter(20, prepId); // user_last_mod дали да не се използва отделна колона за това. ако остава
												// тука после трябва да се чисти
			queryDelo.executeUpdate();

			insertCnt++;

			if (insertCnt % 500 == 0) {
				dest.commit();
				dest.begin();
				LOGGER.info("  " + insertCnt);
			}
		}
		dest.commit(); // за последните които не са кратни на 500

		stream.close();
		sourceEdsd.closeConnection();
		dest.closeConnection();

		LOGGER.info("  " + insertCnt);
		LOGGER.info("End loading prepiski from ombucman_edsd.");
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
		sql.append(" select 'SEQ_DOC_VID_SETTINGS' SEQ_NAME, max (id) MAX_ID from DOC_VID_SETTINGS ");

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
}

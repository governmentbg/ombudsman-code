package com.ib.omb.transform;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_REFERENT;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
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
import com.ib.system.utils.PasswordUtils;
import com.ib.system.utils.SearchUtils;

/**
 * Трансфер на потребтели, админ. структура и кореспонденти
 *
 * @author belev
 */
public class T1Referents {

	private static final Logger LOGGER = LoggerFactory.getLogger(T1Referents.class);

	private int		transferUser;
	private Date	transferDate;

	private int		codeRef;			// от тука започват кореспондентите. по малките са административна структура
	private int		codeNapusnali;		// клона от админ. структурата в която са неактивните потребители
	private Date	dateDoNapusnali;	// с тази дата се затврят напусналите

	/**
	 * @param transferUser
	 * @param transferDate
	 * @param codeNapusnali
	 * @param dateDoNapusnali
	 */
	public T1Referents(int transferUser, Date transferDate, int codeNapusnali, Date dateDoNapusnali) {
		this.transferUser = transferUser;
		this.transferDate = transferDate;

		this.codeRef = T0Start.NAR_UNIDENTIFIED; // по малките са усери
		this.codeNapusnali = codeNapusnali;
		this.dateDoNapusnali = dateDoNapusnali;
	}

	/**
	 * Методът
	 *
	 * @param sourceReg
	 * @param sourceEdsd
	 * @param dest
	 * @throws BaseException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	public void transfer(JPA sourceReg, JPA sourceEdsd, JPA dest) throws BaseException, NoSuchAlgorithmException, InvalidKeySpecException {
		// проверка дали вече е пускан
		Number cnt = (Number) dest.getEntityManager().createNativeQuery("select count(*) from transfer_process where clazz = ?1") //
			.setParameter(1, getClass().getSimpleName()).getResultList().get(0);
		if (cnt.intValue() > 0) {
			LOGGER.info("   ! finished !");
			return; // значи си е свършил работата
		}
		Date startDate = new Date(); // тука всяко пускане прави всичко

		clearRelatedData(dest);

		@SuppressWarnings("unchecked")
		List<Object> napusnaliList = dest.getEntityManager().createNativeQuery("select distinct code from adm_referents where code_parent = ?1") //
			.setParameter(1, this.codeNapusnali).getResultList();
		Set<Integer> napusnali = new HashSet<>();
		for (Object napusnal : napusnaliList) {
			napusnali.add(((Number) napusnal).intValue());
		}

		Map<Integer, String> loadedUsers = loadUsersReg(sourceReg, dest, napusnali); // от регистъра
		loadUsersEdsd(sourceEdsd, dest, loadedUsers, napusnali); // от деловодната

		dest.begin();
		dest.getEntityManager().createNativeQuery( // трябва да се затворят напусналите заедно със звеното
			"update adm_referents set date_do = :dateDo where code_parent = :codeMigrirani or code = :codeMigrirani") //
			.setParameter("dateDo", this.dateDoNapusnali).setParameter("codeMigrirani", this.codeNapusnali).executeUpdate();
		dest.commit();

		Map<String, Integer> loadedCorresp = loadCorrespReg(sourceReg, dest); // от регистъра
		loadCorrespEdsd(sourceEdsd, dest, loadedCorresp); // от деловодната

		recreateSequences(dest);

		// много важно е да се марира че е изпълнен процеса
		dest.begin();
		dest.getEntityManager().createNativeQuery( //
			"insert into transfer_process (clazz, start_time, end_time) values (?1, ?2, ?3)") //
			.setParameter(1, getClass().getSimpleName()).setParameter(2, startDate).setParameter(3, new Date()) //
			.executeUpdate();
		dest.commit();
	}

	/**
	 * Изтрива ненуждн данни за потребители и групи
	 *
	 * @param dest
	 * @throws DbErrorException
	 */
	void clearRelatedData(JPA dest) throws DbErrorException {
		LOGGER.info("Start clear related data");

		// adm_users всички > 0 - по малки zxc
		dest.begin();
		int cnt = dest.getEntityManager().createNativeQuery("delete from adm_users where user_id > ?1") //
			.setParameter(1, 0).executeUpdate();
		dest.commit();
		LOGGER.info("   deleted " + cnt + " rows from table adm_users");

		dest.begin();
		cnt = dest.getEntityManager().createNativeQuery("delete from adm_ref_addrs").executeUpdate();
		dest.commit();
		LOGGER.info("   deleted " + cnt + " rows from table adm_ref_addrs");

		// adm_referents всички > lastUsedId - по малките са потребители
		dest.begin();
		cnt = dest.getEntityManager().createNativeQuery("delete from adm_referents where code > ?1 or ref_id > ?2") //
			.setParameter(1, T0Start.NAR_UNIDENTIFIED).setParameter(2, T0Start.NAR_UNIDENTIFIED).executeUpdate();
		dest.commit();
		LOGGER.info("   deleted " + cnt + " rows from table adm_referents");

//		+ данните за прекодиране
		dest.begin();
		cnt = dest.getEntityManager().createNativeQuery("delete from transfer_table where code_object = ?1") //
			.setParameter(1, CODE_ZNACHENIE_JOURNAL_REFERENT).executeUpdate();
		dest.commit();
		LOGGER.info("   deleted " + cnt + " rows from table transfer_table (code_object=" + CODE_ZNACHENIE_JOURNAL_REFERENT + ")");

		LOGGER.info("End clear related data");
	}

	/**
	 * Прехвърля кореспондентите от Деловодната система
	 */
	void loadCorrespEdsd(JPA sourceEdsd, JPA dest, Map<String, Integer> loadedCorresp) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start loading Correspondents from ombucman_edsd...");

		int insertCnt = 0;

		Date minDate = DateUtils.systemMinDate();
		Date maxDate = DateUtils.systemMaxDate();

		Query queryRef = dest.getEntityManager().createNativeQuery("insert into ADM_REFERENTS(" //
			+ " REF_ID, CODE, CODE_PARENT, REF_TYPE, REF_NAME, DATE_OT, DATE_DO, USER_REG, DATE_REG, fzl_egn, nfl_eik)" //
			+ " values (?1, ?2, 0, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10)");

		Query queryAddr = dest.getEntityManager().createNativeQuery("insert into ADM_REF_ADDRS(" //
			+ " ADDR_ID, CODE_REF, ADDR_TYPE, ADDR_COUNTRY, ADDR_TEXT, USER_REG, DATE_REG)" //
			+ " values (?1, ?2, 1, 37, ?3, ?4, ?5)");

		Query queryMap = dest.getEntityManager().createNativeQuery( //
			"insert into transfer_table (src, src_id, dest_id, code_object) values (?1, ?2, ?3, ?4)");

		StringBuilder sql = new StringBuilder();
		sql.append(" select distinct k.kor_id, case when k.name_iuridi is not null then 3 else 4 end ref_type ");
		sql.append(" , k.egn, k.bulstat, k.full_name, k.comm_address, k.item_type ");
		sql.append(" from doc_kors dk inner join korespondents k on k.kor_id = dk.kor_id ");
		sql.append(" where k.kor_id not in (87,504,1830003,1830056,1830076,1998121,1998122,1998136,1998150,1998151,1998174,1998175,1998176,1998177,1998178,1998179,1998180,1999563) ");
		sql.append(" order by k.item_type, k.kor_id ");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceEdsd.getEntityManager().createNativeQuery( //
			sql.toString()) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			String refName = SearchUtils.trimToNULL((String) row[4]);
			if (refName == null) {
				continue;
			}
			Integer srcId = ((Number) row[0]).intValue();

			int refType = ((Number) row[1]).intValue();
			if (refType == OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL) { // има глупости и туа се опивам да ги оправя
				if (refName.contains("\"")) {
					refType = OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL;
				}
			}

			String findName = buildFindName(refName);
			Integer destId = loadedCorresp.get(findName);

			if (destId != null) { // ще се преизползва

			} else {
				this.codeRef++;
				destId = this.codeRef; // от новия запис ще бъде

				queryRef.setParameter(1, destId); // REF_ID
				queryRef.setParameter(2, destId); // CODE
				queryRef.setParameter(3, refType); // REF_TYPE
				queryRef.setParameter(4, refName); // REF_NAME
				queryRef.setParameter(5, minDate); // DATE_OT
				queryRef.setParameter(6, maxDate); // DATE_DO
				queryRef.setParameter(7, this.transferUser);
				queryRef.setParameter(8, this.transferDate);
				queryRef.setParameter(9, null); // fzl_egn
				queryRef.setParameter(10, null); // nfl_eik

				queryRef.executeUpdate();

				// това ще се използва за другите за да се направи съответствие между сорс базите
				loadedCorresp.put(findName, destId);

				String addrText = SearchUtils.trimToNULL((String) row[5]);
				if (addrText != null) { // имаме данни за адрес

					queryAddr.setParameter(1, destId); // това ще го използва за ИД в таблицата на адресите
					queryAddr.setParameter(2, destId); // CODE_REF
					queryAddr.setParameter(3, addrText); // ADDR_TEXT
					queryAddr.setParameter(4, this.transferUser);
					queryAddr.setParameter(5, this.transferDate);

					queryAddr.executeUpdate();
				}
			}

			queryMap.setParameter(1, T0Start.SRC_EDSD);
			queryMap.setParameter(2, srcId);
			queryMap.setParameter(3, destId);
			queryMap.setParameter(4, CODE_ZNACHENIE_JOURNAL_REFERENT);
			queryMap.executeUpdate();

			insertCnt++;

			if (insertCnt % 500 == 0) {
				dest.commit();
				dest.begin();
				LOGGER.info("  " + insertCnt);
			}
		}
		dest.commit(); // за последните които не са кратни на 500

		stream.close();

		LOGGER.info("  " + insertCnt);
		LOGGER.info("End loading Correspondents from ombucman_edsd.");
	}

	/**
	 * Прехвърля кореспондентите от Регистър жалби
	 */
	Map<String, Integer> loadCorrespReg(JPA sourceReg, JPA dest) throws DbErrorException {
		Map<String, Integer> loadedCorresp = new HashMap<>(); // key= narushitel.trim.upper и др.
																// value=refId в БД омб при нас
		LOGGER.info("");
		LOGGER.info("Start loading Correspondents from omb_register...");

		int insertCnt = 0;

		Date minDate = DateUtils.systemMinDate();
		Date maxDate = DateUtils.systemMaxDate();
		Map<Integer, Integer> katNarMap = T0Start.findDecodeClassifMap(dest, OmbConstants.CODE_CLASSIF_KAT_NAR, T0Start.SRC_REG);

		Query queryRef = dest.getEntityManager().createNativeQuery("insert into ADM_REFERENTS(" //
			+ " REF_ID, CODE, CODE_PARENT, REF_TYPE, REF_NAME, DATE_OT, DATE_DO, USER_REG, DATE_REG, kat_nar)" //
			+ " values (?1, ?2, 0, ?3, ?4, ?5, ?6, ?7, ?8, ?9)");

		Query queryAddr = dest.getEntityManager().createNativeQuery("insert into ADM_REF_ADDRS(" //
			+ " ADDR_ID, CODE_REF, ADDR_TYPE, ADDR_COUNTRY, ADDR_TEXT, USER_REG, DATE_REG)" //
			+ " values (?1, ?2, 1, 37, ?3, ?4, ?5)");

		Query queryMap = dest.getEntityManager().createNativeQuery( //
			"insert into transfer_table (src, src_id, dest_id, code_object) values (?1, ?2, ?3, ?4)");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceReg.getEntityManager().createNativeQuery( //
			"select id, cat, narushitel, address from narushiteli order by id") //
			.setHint(QueryHints.HINT_FETCH_SIZE, "" + Integer.MIN_VALUE) // така трябва за да работи с MySQL
			.getResultStream();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			Integer srcId = ((Number) row[0]).intValue();

			String refName = SearchUtils.trimToNULL((String) row[2]);
			if (refName == null) {
				queryMap.setParameter(1, T0Start.SRC_REG);
				queryMap.setParameter(2, srcId);
				queryMap.setParameter(3, T0Start.NAR_UNIDENTIFIED);
				queryMap.setParameter(4, CODE_ZNACHENIE_JOURNAL_REFERENT);
				queryMap.executeUpdate();

				continue;
			}

			String findName = buildFindName(refName);
			Integer destId = loadedCorresp.get(findName);

			if (destId != null) { // ще се преизползва

			} else {
				this.codeRef++;
				destId = this.codeRef; // от новия запис ще бъде

				Integer katNar = null;
				if (row[1] != null) { // cat
					int xi = ((Number) row[1]).intValue();

					katNar = katNarMap.get(xi);
					if (katNar == null) {
						katNar = -xi; // запазвам оригиналния но с минус
						LOGGER.warn("  ! narushiteli.id=" + srcId + " ! UNKNOWN cat=" + xi);
					}
				}

				queryRef.setParameter(1, destId); // REF_ID
				queryRef.setParameter(2, destId); // CODE
				queryRef.setParameter(3, OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL); // REF_TYPE
				queryRef.setParameter(4, refName); // REF_NAME
				queryRef.setParameter(5, minDate); // DATE_OT
				queryRef.setParameter(6, maxDate); // DATE_DO
				queryRef.setParameter(7, this.transferUser);
				queryRef.setParameter(8, this.transferDate);
				queryRef.setParameter(9, new TypedParameterValue(StandardBasicTypes.INTEGER, katNar)); // kat_nar

				queryRef.executeUpdate();

				// това ще се използва за другите за да се направи съответствие между сорс базите
				loadedCorresp.put(buildFindName(refName), destId);

				String addrText = SearchUtils.trimToNULL((String) row[3]);
				if (addrText != null) { // имаме данни за адрес

					queryAddr.setParameter(1, destId); // това ще го използва за ИД в таблицата на адресите
					queryAddr.setParameter(2, destId); // CODE_REF
					queryAddr.setParameter(3, addrText); // ADDR_TEXT
					queryAddr.setParameter(4, this.transferUser);
					queryAddr.setParameter(5, this.transferDate);

					queryAddr.executeUpdate();
				}
			}

			queryMap.setParameter(1, T0Start.SRC_REG);
			queryMap.setParameter(2, srcId);
			queryMap.setParameter(3, destId);
			queryMap.setParameter(4, CODE_ZNACHENIE_JOURNAL_REFERENT);
			queryMap.executeUpdate();

			insertCnt++;

			if (insertCnt % 500 == 0) {
				dest.commit();
				dest.begin();
				LOGGER.info("  " + insertCnt);
			}
		}
		dest.commit(); // за последните които не са кратни на 500

		stream.close();

		LOGGER.info("  " + insertCnt);
		LOGGER.info("End loading Correspondents from omb_register.");

		return loadedCorresp;
	}

	/**
	 * Прехвърля потребителите от Деловодната система
	 */
	void loadUsersEdsd(JPA sourceEdsd, JPA dest, Map<Integer, String> loadedUsers, Set<Integer> napusnali) throws DbErrorException, NoSuchAlgorithmException, InvalidKeySpecException {
		LOGGER.info("");
		LOGGER.info("Start loading Users from ombucman_edsd...");

		Date minDate = DateUtils.systemMinDate();

		int insertCnt = 0;

		Map<Integer, Integer> userMap = T0Start.findDecodeObjectMap(dest, Constants.CODE_ZNACHENIE_JOURNAL_USER, T0Start.SRC_EDSD);

		Query queryUser = dest.getEntityManager().createNativeQuery( //
			"insert into ADM_USERS (USER_ID, USERNAME, PASSWORD, NAMES, EMAIL, LANG, STATUS, STATUS_DATE" //
				+ " , LOGIN_ATTEMPTS, PASS_LAST_CHANGE, PASS_IS_NEW, USER_REG, DATE_REG, SYSTEM_ID, CONFIRMED)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15)");

		@SuppressWarnings("unchecked")
		List<Object[]> rows = sourceEdsd.getEntityManager().createNativeQuery( //
			"select usr_id, username from users order by usr_id").getResultList();

		dest.begin();

		Iterator<Object[]> iter = rows.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			Integer srcId = ((Number) row[0]).intValue(); // id

			Integer userId = userMap.get(srcId);
			if (userId == null) {
				LOGGER.warn("  ! ombucman_edsd.users.id=" + srcId + " ! UNKNOWN");
				continue; // не го познаваме този
			}
			if (userId.equals(-1) || loadedUsers.containsKey(userId)) {
				continue; // такъв вече го имаме
			}

			String username = SearchUtils.trimToNULL((String) row[1]); // uname
			if (username == null) {
				username = "username_" + System.currentTimeMillis();

			} else { // има някаква вероятност между двете системи да има дублирани потребителски имена и ако това се случи ще се
						// счупи инсърта
				if (loadedUsers.values().contains(username)) {
					username = username + "_" + System.currentTimeMillis();
				}
			}

			@SuppressWarnings("unchecked")
			List<Object[]> admData = dest.getEntityManager().createNativeQuery( //
				"select ref_name, contact_email from adm_referents where code = ?1").setParameter(1, userId).getResultList();

			String names = null;
			String email = null;
			if (admData.isEmpty()) {
				names = "names_" + System.currentTimeMillis();
			} else {
				names = (String) admData.get(0)[0];
				email = (String) admData.get(0)[1];
			}

			int status = Constants.CODE_ZNACHENIE_STATUS_INACTIVE;
			if (!napusnali.contains(userId)) {
				status = Constants.CODE_ZNACHENIE_STATUS_ACTIVE;
			}

			queryUser.setParameter(1, userId); // USER_ID
			queryUser.setParameter(2, username); // USERNAME
			queryUser.setParameter(3, PasswordUtils.hashPassword(username)); // PASSWORD
			queryUser.setParameter(4, names); // NAMES
			queryUser.setParameter(5, email); // EMAIL
			queryUser.setParameter(6, SysConstants.CODE_DEFAULT_LANG); // LANG
			queryUser.setParameter(7, status); // STATUS
			queryUser.setParameter(8, this.transferDate); // STATUS_DATE
			queryUser.setParameter(9, 0); // LOGIN_ATTEMPTS
			queryUser.setParameter(10, minDate); // PASS_LAST_CHANGE
			queryUser.setParameter(11, true); // PASS_IS_NEW
			queryUser.setParameter(12, this.transferUser); // USER_REG
			queryUser.setParameter(13, this.transferDate); // DATE_REG
			queryUser.setParameter(14, 0); // SYSTEM_ID
			queryUser.setParameter(15, true); // CONFIRMED
			queryUser.executeUpdate();

			loadedUsers.put(userId, username); // за да се знае за следващите които се мигрират
			insertCnt++;
		}
		dest.commit();

		LOGGER.info("  " + insertCnt);
		LOGGER.info("End loading Users from ombucman_edsd.");
	}

	/**
	 * Прехвърля потребителите от Регистър жалби
	 */
	Map<Integer, String> loadUsersReg(JPA sourceReg, JPA dest, Set<Integer> napusnali) throws DbErrorException, NoSuchAlgorithmException, InvalidKeySpecException {
		Map<Integer, String> loadedUsers = new HashMap<>(); // заредените в БД

		LOGGER.info("");
		LOGGER.info("Start loading Users from omb_register...");

		Date minDate = DateUtils.systemMinDate();

		int insertCnt = 0;
		Map<Integer, Integer> userMap = T0Start.findDecodeObjectMap(dest, Constants.CODE_ZNACHENIE_JOURNAL_USER, T0Start.SRC_REG);

		Query queryUser = dest.getEntityManager().createNativeQuery( //
			"insert into ADM_USERS (USER_ID, USERNAME, PASSWORD, NAMES, EMAIL, LANG, STATUS, STATUS_DATE" //
				+ " , LOGIN_ATTEMPTS, PASS_LAST_CHANGE, PASS_IS_NEW, USER_REG, DATE_REG, SYSTEM_ID, CONFIRMED)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15)");

		@SuppressWarnings("unchecked")
		List<Object[]> rows = sourceReg.getEntityManager().createNativeQuery( //
			"select id, name, uname, email, regedon from users order by id").getResultList();

		dest.begin();

		Iterator<Object[]> iter = rows.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			Integer srcId = ((Number) row[0]).intValue(); // id

			Integer userId = userMap.get(srcId);
			if (userId == null) { //
				LOGGER.warn("  ! omb_register.users.id=" + srcId + " ! UNKNOWN");
				continue; // не го познаваме този
			}
			if (userId.equals(-1) || loadedUsers.containsKey(userId)) {
				continue; // такъв вече го имаме
			}

			String username = SearchUtils.trimToNULL((String) row[2]); // uname
			if (username == null) {
				username = "username_" + System.currentTimeMillis();
			}
			String names = SearchUtils.trimToNULL((String) row[1]); // name
			if (names == null) {
				names = "names_" + System.currentTimeMillis();
			}

			Date regedon = (Date) row[4];
			if (regedon == null) {
				regedon = this.transferDate;
			}
			int status = Constants.CODE_ZNACHENIE_STATUS_INACTIVE;
			if (!napusnali.contains(userId)) {
				status = Constants.CODE_ZNACHENIE_STATUS_ACTIVE;
			}

			queryUser.setParameter(1, userId); // USER_ID
			queryUser.setParameter(2, username); // USERNAME
			queryUser.setParameter(3, PasswordUtils.hashPassword(username)); // PASSWORD
			queryUser.setParameter(4, names); // NAMES
			queryUser.setParameter(5, SearchUtils.trimToNULL((String) row[3])); // EMAIL
			queryUser.setParameter(6, SysConstants.CODE_DEFAULT_LANG); // LANG
			queryUser.setParameter(7, status); // STATUS
			queryUser.setParameter(8, regedon); // STATUS_DATE
			queryUser.setParameter(9, 0); // LOGIN_ATTEMPTS
			queryUser.setParameter(10, minDate); // PASS_LAST_CHANGE
			queryUser.setParameter(11, true); // PASS_IS_NEW
			queryUser.setParameter(12, this.transferUser); // USER_REG
			queryUser.setParameter(13, regedon); // DATE_REG
			queryUser.setParameter(14, 0); // SYSTEM_ID
			queryUser.setParameter(15, true); // CONFIRMED
			queryUser.executeUpdate();

			loadedUsers.put(userId, username); // за да се знае за следващите които се мигрират
			insertCnt++;
		}
		dest.commit();

		LOGGER.info("  " + insertCnt);
		LOGGER.info("End loading Users from omb_register.");

		return loadedUsers;
	}

	/**
	 * @param dest
	 * @throws DbErrorException
	 */
	void recreateSequences(JPA dest) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Recreate SEQUENCEs");

		// seq_adm_referents
		// seq_adm_ref_addrs
		// seq_adm_referents_code

		StringBuilder sql = new StringBuilder();
		sql.append(" select 'seq_adm_referents' seq_name, max (ref_id) max_id from adm_referents ");
		sql.append(" union all ");
		sql.append(" select 'seq_adm_ref_addrs' seq_name, max (addr_id) max_id from adm_ref_addrs ");
		sql.append(" union all ");
		sql.append(" select 'seq_adm_referents_code' seq_name, max (code) max_id from adm_referents ");

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
	 * @param refName
	 * @return
	 */
	private String buildFindName(String refName) {
		return refName.toUpperCase().replaceAll("\"", "").replaceAll("-", "").replace("  ", " ");
	}
}
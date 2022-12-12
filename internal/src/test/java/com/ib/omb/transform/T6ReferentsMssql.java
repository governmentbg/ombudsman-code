package com.ib.omb.transform;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_REFERENT;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
 * Трансфер на потребтели и кореспонденти Стар Регистър жалби/Деловодна система
 *
 * @author belev
 */
public class T6ReferentsMssql {

	private static final Logger LOGGER = LoggerFactory.getLogger(T6ReferentsMssql.class);

	private int transferUser;

	private int		codeNapusnali;		// клона от админ. структурата в която са неактивните потребители
	private Date	dateDoNapusnali;	// с тази дата се затврят напусналите

	/**
	 * @param transferUser
	 * @param codeNapusnali
	 * @param dateDoNapusnali
	 */
	public T6ReferentsMssql(int transferUser, int codeNapusnali, Date dateDoNapusnali) {
		this.transferUser = transferUser;
		this.codeNapusnali = codeNapusnali;
		this.dateDoNapusnali = dateDoNapusnali;
	}

	/**
	 * Методът
	 *
	 * @param sourceMssql
	 * @param dest
	 * @throws BaseException
	 * @throws InvalidKeySpecException
	 * @throws NoSuchAlgorithmException
	 */
	public void transfer(JPA sourceMssql, JPA dest) throws BaseException, NoSuchAlgorithmException, InvalidKeySpecException {
		// проверка дали вече е пускан
		Number cnt = (Number) dest.getEntityManager().createNativeQuery("select count(*) from transfer_process where clazz = ?1") //
			.setParameter(1, getClass().getSimpleName()).getResultList().get(0);
		if (cnt.intValue() > 0) {
			LOGGER.info("   ! finished !");
			return; // значи си е свършил работата
		}
		Date startDate = new Date(); // тука всяко пускане прави всичко

		clearRelatedData(dest);

		Map<String, Integer> refMap = findLoadedCorrespNfl(dest);
		loadCorrespNfl(sourceMssql, dest, refMap);

		loadCorrespFzl(sourceMssql, dest);

		Map<String, Integer> userMap = findLoadedUsers(dest);
		loadUsers(sourceMssql, dest, userMap);

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
	 * Изтрива данни от предходни пускания на този процес
	 *
	 * @param dest
	 * @throws DbErrorException
	 */
	void clearRelatedData(JPA dest) throws DbErrorException {
		LOGGER.info("Start clear related data");

		// mig_login_name=mssql
		dest.begin();
		int cnt = dest.getEntityManager().createNativeQuery //
		("delete from adm_ref_addrs where code_ref in (select distinct code from adm_referents where mig_login_name = ?1)") //
			.setParameter(1, T0Start.SRC_MSSQL).executeUpdate();
		dest.commit();
		LOGGER.info("   deleted " + cnt + " rows from table adm_ref_addrs");

		// mig_login_name=mssql
		dest.begin();
		cnt = dest.getEntityManager().createNativeQuery //
		("delete from adm_users where user_id in (select distinct code from adm_referents where mig_login_name = ?1)") //
			.setParameter(1, T0Start.SRC_MSSQL).executeUpdate();
		dest.commit();
		LOGGER.info("   deleted " + cnt + " rows from table adm_users");

		// mig_login_name=mssql
		dest.begin();
		cnt = dest.getEntityManager().createNativeQuery("delete from adm_referents where mig_login_name = ?1") //
			.setParameter(1, T0Start.SRC_MSSQL).executeUpdate();
		dest.commit();
		LOGGER.info("   deleted " + cnt + " rows from table adm_referents");

//		+ данните за прекодиране
		List<Integer> codeObjs = Arrays.asList(CODE_ZNACHENIE_JOURNAL_REFERENT, CODE_ZNACHENIE_JOURNAL_REFERENT * -1, Constants.CODE_ZNACHENIE_JOURNAL_USER);
		dest.begin();
		cnt = dest.getEntityManager().createNativeQuery("delete from transfer_table where code_object in (?1) and src = ?2") //
			.setParameter(1, codeObjs).setParameter(2, T0Start.SRC_MSSQL).executeUpdate();
		dest.commit();
		LOGGER.info("   deleted " + cnt + " rows from table transfer_table (code_object=" + codeObjs + ")");

		LOGGER.info("End clear related data");
	}

	/**
	 * Прехвърля кореспондентите от Стар Регистър жалби/Деловодна система
	 */
	void loadCorrespFzl(JPA sourceMssql, JPA dest) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start loading Correspondents-FZL from omb_mssql...");

		int insertCnt = 0;

		Date minDate = DateUtils.systemMinDate();
		Date maxDate = DateUtils.systemMaxDate();

		Number maxCodeRef = (Number) dest.getEntityManager().createNativeQuery("select max(code) from adm_referents").getResultList().get(0);
		int codeRef = maxCodeRef != null ? maxCodeRef.intValue() : 0;

		Number maxRefId = (Number) dest.getEntityManager().createNativeQuery("select max(ref_id) from adm_referents").getResultList().get(0);
		int refId = maxRefId != null ? maxRefId.intValue() : 0;

		Query queryRef = dest.getEntityManager().createNativeQuery("insert into ADM_REFERENTS(" //
			+ " REF_ID, CODE, CODE_PARENT, REF_TYPE, REF_NAME, DATE_OT, DATE_DO, USER_REG, DATE_REG, mig_login_name, fzl_egn" //
			+ ", ref_grj, contact_phone, contact_email)" //
			+ " values (?1, ?2, 0, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13)");

		Query queryMap = dest.getEntityManager().createNativeQuery( //
			"insert into transfer_table (src, src_id, dest_id, code_object) values (?1, ?2, ?3, ?4)");

		Query queryAddr = dest.getEntityManager().createNativeQuery("insert into ADM_REF_ADDRS(" //
			+ " ADDR_ID, CODE_REF, ADDR_TYPE, ADDR_COUNTRY, ADDR_TEXT, post_code, ekatte, USER_REG, DATE_REG)" //
			+ " values (?1, ?2, 1, ?3, ?4, ?5, ?6, ?7, ?8)");

		StringBuilder sql = new StringBuilder();
		sql.append(" select distinct cor.ID Ref_ID, pcor.ID Person_ID, pcor.EgnLnc, cor.CD ");
		sql.append(" , pcor.Name, pcor.Surname, pcor.Family ");
		sql.append(" , ac.Ekatte, ac.PostCode, ac.Country, ac.AddrLine1, ac.AddrLine2, ac.AddrLine3 "); // 7,8,9,10,11,12
		sql.append(" , ac.Phones, ac.Emails, pcor.IsForeignCitizen, ctry.Name CountryName, ac.ID as Ac_ID "); // 13,14,15,16,17
		sql.append(" from Movement m ");
		sql.append(" inner join Document d on d.ID = m.Item_ID ");
		sql.append(" inner join Referent cor on cor.ID = m.From_ID ");
		sql.append(" inner join Person pcor on pcor.ID = cor.Person_ID ");
		sql.append(" left outer join AddressLink alink on alink.Object_ID = pcor.ID and alink.ObjectType = 1 and alink.IsDefault = 1 ");
		sql.append(" left outer join AddressCommunic ac on ac.ID = alink.Address_ID ");
		sql.append(" left outer join Nomenclatures ctry on ctry.ID = ac.Country ");
		sql.append(" where m.ItemType_ID = 0 and d.DocumentType_ID = 8 and cor.RefType_ID in (366,367) ");
		sql.append(" and cor.Organization_ID is null ");
		sql.append(" order by 1 ");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceMssql.getEntityManager().createNativeQuery(sql.toString()) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			Integer srcRef = ((Number) row[0]).intValue();
			codeRef++;
			refId++;

			Date cd = row[3] != null ? (Date) row[3] : minDate;

			StringBuilder refName = new StringBuilder();
			String t = SearchUtils.trimToNULL((String) row[4]); // Name
			if (t != null) {
				refName.append(t);
			}
			t = SearchUtils.trimToNULL((String) row[5]); // Surname
			if (t != null) {
				refName.append(" " + t);
			}
			t = SearchUtils.trimToNULL((String) row[6]); // Family
			if (t != null) {
				refName.append(" " + t);
			}

			String egn = SearchUtils.trimToNULL((String) row[2]); // EgnLnc

			boolean bg = true;
			if (Boolean.TRUE.equals(SearchUtils.asBoolean(row[15]))) { // IsForeignCitizen
				bg = false;
			}

			queryRef.setParameter(1, refId); // REF_ID
			queryRef.setParameter(2, codeRef); // CODE
			queryRef.setParameter(3, OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL); // REF_TYPE
			queryRef.setParameter(4, refName.toString()); // REF_NAME
			queryRef.setParameter(5, minDate); // DATE_OT
			queryRef.setParameter(6, maxDate); // DATE_DO
			queryRef.setParameter(7, this.transferUser);
			queryRef.setParameter(8, cd);
			queryRef.setParameter(9, T0Start.SRC_MSSQL); // по това ще се знае после кои са дошли от този източник
			queryRef.setParameter(10, egn); // fzl_egn
			queryRef.setParameter(11, new TypedParameterValue(StandardBasicTypes.INTEGER, bg ? 37 : null)); // ref_grj
			queryRef.setParameter(12, row[13]); // contact_phone
			queryRef.setParameter(13, row[14]); // contact_email

			queryRef.executeUpdate();

			queryMap.setParameter(1, T0Start.SRC_MSSQL);
			queryMap.setParameter(2, srcRef);
			queryMap.setParameter(3, codeRef);
			queryMap.setParameter(4, CODE_ZNACHENIE_JOURNAL_REFERENT);
			queryMap.executeUpdate();

			if (row[17] != null) { // Ac_ID - има адрес
				String countryName = null;
				if (row[9] != null && ((Number) row[9]).intValue() != 165) { // Country, само различни от БГ
					countryName = SearchUtils.trimToNULL((String) row[16]); // CountryName
				}

				StringBuilder addr = new StringBuilder();
				t = SearchUtils.trimToNULL((String) row[10]); // AddrLine1
				if (t != null) {
					addr.append(t);
				}
				t = SearchUtils.trimToNULL((String) row[11]); // AddrLine2
				if (t != null) {
					if (addr.length() > 0) {
						addr.append("; ");
					}
					addr.append(t);
				}
				t = SearchUtils.trimToNULL((String) row[12]); // AddrLine3
				if (t != null) {
					if (addr.length() > 0) {
						addr.append("; ");
					}
					addr.append(t);
				}

				String ekatte = SearchUtils.trimToNULL((String) row[7]); // ekatte
				Integer ekatteCode = null;
				if (ekatte != null) {
					ekatteCode = parseEkatte(ekatte);
					if (ekatteCode == null) {
						LOGGER.warn("  ! AddressCommunic.ID=" + row[17] + " ! ERROR ekatte=" + ekatte);
					}
				}

				queryAddr.setParameter(1, codeRef); // ADDR_ID
				queryAddr.setParameter(2, codeRef); // CODE_REF
				queryAddr.setParameter(3, new TypedParameterValue(StandardBasicTypes.INTEGER, bg ? 37 : null)); // ADDR_COUNTRY
				queryAddr.setParameter(4, (countryName != null ? countryName + " " : "") + addr.toString().trim()); // ADDR_TEXT
				queryAddr.setParameter(5, row[8] == null ? null : String.valueOf(row[8])); // post_code
				queryAddr.setParameter(6, new TypedParameterValue(StandardBasicTypes.INTEGER, ekatteCode)); // ekatte
				queryAddr.setParameter(7, this.transferUser); // USER_REG
				queryAddr.setParameter(8, cd); // DATE_REG
				queryAddr.executeUpdate();
			}

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
		LOGGER.info("End loading Correspondents-FZL from omb_mssql.");
	}

	/**
	 * Прехвърля кореспондентите от Стар Регистър жалби/Деловодна система
	 */
	void loadCorrespNfl(JPA sourceMssql, JPA dest, Map<String, Integer> loadedCorresp) throws DbErrorException {
		LOGGER.info("");
		LOGGER.info("Start loading Correspondents-NFL from omb_mssql...");

		int insertCnt = 0;

		Date minDate = DateUtils.systemMinDate();
		Date maxDate = DateUtils.systemMaxDate();

		Number maxCodeRef = (Number) dest.getEntityManager().createNativeQuery("select max(code) from adm_referents").getResultList().get(0);
		int codeRef = maxCodeRef != null ? maxCodeRef.intValue() : 0;

		Number maxRefId = (Number) dest.getEntityManager().createNativeQuery("select max(ref_id) from adm_referents").getResultList().get(0);
		int refId = maxRefId != null ? maxRefId.intValue() : 0;

		Query queryRef = dest.getEntityManager().createNativeQuery("insert into ADM_REFERENTS(" //
			+ " REF_ID, CODE, CODE_PARENT, REF_TYPE, REF_NAME, DATE_OT, DATE_DO, USER_REG, DATE_REG, mig_login_name)" //
			+ " values (?1, ?2, 0, ?3, ?4, ?5, ?6, ?7, ?8, ?9)");

		Query queryMap = dest.getEntityManager().createNativeQuery( //
			"insert into transfer_table (src, src_id, dest_id, code_object) values (?1, ?2, ?3, ?4)");

		StringBuilder sql = new StringBuilder();
		sql.append(" select r.ID Ref_ID, o.ID Organization_ID ");
		sql.append(" , case when o.ID in (342, 348, 2801, 3121) then null else o.Name end OrgName "); // тази отиват в неизвестен
		sql.append(" , r.CD");
		sql.append(" from Organization o ");
		sql.append(" left outer join Referent r on r.Organization_ID = o.ID ");
		sql.append(" where o.ID in (select distinct Organization_ID from DossierPartiesConcerned where PartyRole_ID = 62) "); // само
																																// нарушители
		sql.append(" union "); // добавям и НФЛ кореспонденти за входящи докуметнти

		sql.append(" select cor.ID Ref_ID, ocor.ID Organization_ID ");
		sql.append(" , case when ocor.ID in (342, 348, 2801, 3121) then null else ocor.Name end OrgName ");
		sql.append(" , cor.CD ");
		sql.append(" from Movement m ");
		sql.append(" inner join Document d on d.ID = m.Item_ID ");
		sql.append(" inner join Referent cor on cor.ID = m.From_ID ");
		sql.append(" inner join Organization ocor on ocor.ID = cor.Organization_ID "); // само НФЛ
		sql.append(" where m.ItemType_ID = 0 and d.DocumentType_ID = 8 and cor.RefType_ID in (366,367)");

		sql.append(" union "); // добавям и НФЛ кореспонденти за изходящи докуметнти

		sql.append(" select cor.ID Ref_ID, ocor.ID Organization_ID ");
		sql.append(" , case when ocor.ID in (342, 348, 2801, 3121) then null else ocor.Name end OrgName ");
		sql.append(" , cor.CD ");
		sql.append(" from Movement m ");
		sql.append(" inner join Document d on d.ID = m.Item_ID ");
		sql.append(" inner join Referent cor on cor.ID = m.To_ID ");
		sql.append(" inner join Organization ocor on ocor.ID = cor.Organization_ID "); // само НФЛ
		sql.append(" where m.ItemType_ID = 0 and d.DocumentType_ID in (7,408,406,414,409,413,410,18,774,11) and cor.RefType_ID in (366,367)");

		sql.append(" order by 1 ");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceMssql.getEntityManager().createNativeQuery(sql.toString()) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			Integer srcOrg = ((Number) row[1]).intValue();
			Integer srcRef = null; // не за всяка организация има референт
			if (row[0] != null) {
				srcRef = ((Number) row[0]).intValue();
			}

			String refName = SearchUtils.trimToNULL((String) row[2]);
			if (refName == null) {
				queryMap.setParameter(1, T0Start.SRC_MSSQL);
				queryMap.setParameter(2, srcOrg);
				queryMap.setParameter(3, T0Start.NAR_UNIDENTIFIED);
				queryMap.setParameter(4, CODE_ZNACHENIE_JOURNAL_REFERENT * -1); // с обратен знак, защото после няма как да ги
																				// разпозная
				queryMap.executeUpdate();

				if (srcRef != null) {
					queryMap.setParameter(1, T0Start.SRC_MSSQL);
					queryMap.setParameter(2, srcRef);
					queryMap.setParameter(3, T0Start.NAR_UNIDENTIFIED);
					queryMap.setParameter(4, CODE_ZNACHENIE_JOURNAL_REFERENT);
					queryMap.executeUpdate();
				}
				continue;
			}

			String findName = buildFindName(refName);
			Integer destId = loadedCorresp.get(findName);

			if (destId != null) { // ще се преизползва

			} else {
				Date cd = row[3] != null ? (Date) row[3] : minDate;

				codeRef++;
				refId++;
				destId = codeRef; // от новия запис ще бъде

				queryRef.setParameter(1, refId); // REF_ID
				queryRef.setParameter(2, destId); // CODE
				queryRef.setParameter(3, OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL); // REF_TYPE
				queryRef.setParameter(4, refName); // REF_NAME
				queryRef.setParameter(5, minDate); // DATE_OT
				queryRef.setParameter(6, maxDate); // DATE_DO
				queryRef.setParameter(7, this.transferUser);
				queryRef.setParameter(8, cd);
				queryRef.setParameter(9, T0Start.SRC_MSSQL); // по това ще се знае после кои са дошли от този източник

				queryRef.executeUpdate();

				// това ще се използва за другите за да се направи съответствие между сорс базите
				loadedCorresp.put(findName, destId);
			}

			queryMap.setParameter(1, T0Start.SRC_MSSQL);
			queryMap.setParameter(2, srcOrg);
			queryMap.setParameter(3, destId);
			queryMap.setParameter(4, CODE_ZNACHENIE_JOURNAL_REFERENT * -1); // с обратен знак, защото после няма как да ги
																			// разпозная
			queryMap.executeUpdate();

			if (srcRef != null) {
				queryMap.setParameter(1, T0Start.SRC_MSSQL);
				queryMap.setParameter(2, srcRef);
				queryMap.setParameter(3, destId);
				queryMap.setParameter(4, CODE_ZNACHENIE_JOURNAL_REFERENT);
				queryMap.executeUpdate();
			}

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
		LOGGER.info("End loading Correspondents-NFL from omb_mssql.");
	}

	/**
	 * Прехвърля потребителите от Стар Регистър жалби/Деловодна система
	 */
	void loadUsers(JPA sourceMssql, JPA dest, Map<String, Integer> loadedUsers) throws DbErrorException, NoSuchAlgorithmException, InvalidKeySpecException {
		LOGGER.info("");
		LOGGER.info("Start loading Users from omb_mssql...");

		int insertCnt = 0;

		Date minDate = DateUtils.systemMinDate();

		Number maxCodeRef = (Number) dest.getEntityManager().createNativeQuery("select max(code) from adm_referents where code < ?1") //
			.setParameter(1, T0Start.NAR_UNIDENTIFIED).getResultList().get(0);
		int codeRef = maxCodeRef != null ? maxCodeRef.intValue() : 0;

		Number maxRefId = (Number) dest.getEntityManager().createNativeQuery("select max(ref_id) from adm_referents where ref_id < ?1") //
			.setParameter(1, T0Start.NAR_UNIDENTIFIED).getResultList().get(0);
		int refId = maxRefId != null ? maxRefId.intValue() : 0;

		Number maxCodePrev = (Number) dest.getEntityManager().createNativeQuery("select max(code) from adm_referents where code_parent = ?1") //
			.setParameter(1, this.codeNapusnali).getResultList().get(0);
		int codePrev = maxCodePrev != null ? maxCodePrev.intValue() : 0;

		Query queryRef = dest.getEntityManager().createNativeQuery("insert into ADM_REFERENTS(" //
			+ " REF_ID, CODE, CODE_PREV, CODE_PARENT, CODE_CLASSIF, REF_TYPE, REF_NAME, REF_GRJ, DATE_OT, DATE_DO, USER_REG, DATE_REG, mig_login_name)" //
			+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13)");

		Query queryUser = dest.getEntityManager().createNativeQuery( //
			"insert into ADM_USERS (USER_ID, USERNAME, PASSWORD, NAMES, EMAIL, LANG, STATUS, STATUS_DATE" //
				+ " , LOGIN_ATTEMPTS, PASS_LAST_CHANGE, PASS_IS_NEW, USER_REG, DATE_REG, SYSTEM_ID, CONFIRMED)" //
				+ " values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, ?10, ?11, ?12, ?13, ?14, ?15)");

		Query queryMap = dest.getEntityManager().createNativeQuery( //
			"insert into transfer_table (src, src_id, dest_id, code_object) values (?1, ?2, ?3, ?4)");

		StringBuilder sql = new StringBuilder();
		sql.append(" select r.ID Ref_ID, r.IsActive, r.WindowsUserName, r.Person_ID, p.Name, p.Surname, p.Family, p.CD ");
		sql.append(" from Referent r ");
		sql.append(" inner join Person p on p.ID = r.Person_ID ");
		sql.append(" where r.RefType_ID = 368 order by r.ID ");

		@SuppressWarnings("unchecked")
		Stream<Object[]> stream = sourceMssql.getEntityManager().createNativeQuery(sql.toString()) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();

		dest.begin();

		Iterator<Object[]> iter = stream.iterator();
		while (iter.hasNext()) {
			Object[] row = iter.next();

			Integer srcRef = ((Number) row[0]).intValue();
			Integer destId = null;

			String username = SearchUtils.trimToNULL((String) row[2]);
			if (username != null) {
				destId = loadedUsers.get(username.toUpperCase());
			}

			if (destId != null) { // ще се преизползва

			} else {
				StringBuilder refName = new StringBuilder();
				String t = SearchUtils.trimToNULL((String) row[4]);
				if (t != null) { // Name
					refName.append(t);
				}
				t = SearchUtils.trimToNULL((String) row[5]);
				if (t != null) { // Surname
					refName.append(" " + t);
				}
				t = SearchUtils.trimToNULL((String) row[6]);
				if (t != null) { // Family
					refName.append(" " + t);
				}

				Date cd = row[7] != null ? (Date) row[7] : minDate;

				codeRef++;
				refId++;
				destId = codeRef; // от новия запис ще бъде

				queryRef.setParameter(1, refId); // REF_ID
				queryRef.setParameter(2, destId); // CODE
				queryRef.setParameter(3, codePrev); // CODE_PREV
				queryRef.setParameter(4, this.codeNapusnali); // CODE_PARENT
				queryRef.setParameter(5, Constants.CODE_CLASSIF_ADMIN_STR); // CODE_CLASSIF
				queryRef.setParameter(6, OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL); // REF_TYPE
				queryRef.setParameter(7, refName.toString()); // REF_NAME
				queryRef.setParameter(8, 37); // REF_GRJ
				queryRef.setParameter(9, minDate); // DATE_OT
				queryRef.setParameter(10, this.dateDoNapusnali); // DATE_DO
				queryRef.setParameter(11, this.transferUser);
				queryRef.setParameter(12, cd);
				queryRef.setParameter(13, T0Start.SRC_MSSQL); // по това ще се знае после кои са дошли от този източник
				queryRef.executeUpdate();

				codePrev = destId;

				if (username == null) { // има и такива които нямат
					username = "mig-" + destId;
				}
				queryUser.setParameter(1, destId); // USER_ID
				queryUser.setParameter(2, username); // USERNAME
				queryUser.setParameter(3, PasswordUtils.hashPassword(username)); // PASSWORD
				queryUser.setParameter(4, refName.toString()); // NAMES
				queryUser.setParameter(5, null); // EMAIL
				queryUser.setParameter(6, SysConstants.CODE_DEFAULT_LANG); // LANG
				queryUser.setParameter(7, Constants.CODE_ZNACHENIE_STATUS_INACTIVE); // STATUS
				queryUser.setParameter(8, cd); // STATUS_DATE
				queryUser.setParameter(9, 0); // LOGIN_ATTEMPTS
				queryUser.setParameter(10, cd); // PASS_LAST_CHANGE
				queryUser.setParameter(11, true); // PASS_IS_NEW
				queryUser.setParameter(12, this.transferUser); // USER_REG
				queryUser.setParameter(13, cd); // DATE_REG
				queryUser.setParameter(14, 0); // SYSTEM_ID
				queryUser.setParameter(15, true); // CONFIRMED
				queryUser.executeUpdate();

				// това ще се използва за другите за да се направи съответствие между сорс базите
				loadedUsers.put(username.toUpperCase(), destId);
			}

			queryMap.setParameter(1, T0Start.SRC_MSSQL);
			queryMap.setParameter(2, srcRef);
			queryMap.setParameter(3, destId);
			queryMap.setParameter(4, Constants.CODE_ZNACHENIE_JOURNAL_USER);
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
		sourceMssql.closeConnection();
		dest.closeConnection();

		LOGGER.info("  " + insertCnt);
		LOGGER.info("End loading Users from omb_mssql.");
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

	/**
	 * Намира вече тези които са заредени, с цел да се обединяват с новите
	 */
	private Map<String, Integer> findLoadedCorrespNfl(JPA dest) {
		@SuppressWarnings("unchecked")
		Stream<Object[]> streamRefMap = dest.getEntityManager().createNativeQuery( //
			"select distinct ref_name, code from adm_referents where ref_type = ?1") //
			.setParameter(1, OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL) //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();
		Map<String, Integer> refMap = new HashMap<>();
		Iterator<Object[]> refIter = streamRefMap.iterator();
		while (refIter.hasNext()) {
			Object[] row = refIter.next();

			String key = buildFindName((String) row[0]);
			int value = ((Number) row[1]).intValue();

			if (refMap.containsKey(key)) {
				LOGGER.warn("streamRefMap duplicate key=" + key);
			}
			refMap.put(key, value);
		}
		streamRefMap.close();
		LOGGER.info("!found " + refMap.size() + " nfl!");

		return refMap;
	}

	/**
	 * Намира вече тези които са заредени, с цел да се обединяват с новите
	 */
	private Map<String, Integer> findLoadedUsers(JPA dest) {
		@SuppressWarnings("unchecked")
		Stream<Object[]> streamUserMap = dest.getEntityManager().createNativeQuery( //
			"select distinct username, user_id from adm_users") //
			.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
			.getResultStream();
		Map<String, Integer> userMap = new HashMap<>();
		Iterator<Object[]> userIter = streamUserMap.iterator();
		while (userIter.hasNext()) {
			Object[] row = userIter.next();

			String key = String.valueOf(row[0]).trim().toUpperCase();
			int value = ((Number) row[1]).intValue();

			if (userMap.containsKey(key)) {
				LOGGER.warn("streamUserMap duplicate key=" + key);
			}
			userMap.put(key, value);
		}
		streamUserMap.close();
		LOGGER.info("!found " + userMap.size() + " usr!");

		return userMap;
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
}
package com.ib.omb.system;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.indexui.system.Constants.CODE_CLASSIF_BUSINESS_ROLE;
import static com.ib.indexui.system.Constants.CODE_CLASSIF_COUNTRIES;
import static com.ib.indexui.system.Constants.CODE_CLASSIF_POSITION;
import static com.ib.indexui.system.Constants.CODE_CLASSIF_REFERENTS;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO;
import static com.ib.system.SysConstants.CODE_CLASSIF_EKATTE;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_DA;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_NE;
import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.system.utils.SearchUtils.trimToNULL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.persistence.Query;

import org.hibernate.jpa.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.system.BaseSystemData;
import com.ib.system.SysClassifAdapter;
import com.ib.system.SysConstants;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.StringUtils;

/**
 * Конкретния адаптер за динамични класификации
 *
 * @author belev
 */
public class OmbClassifAdapter extends SysClassifAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(OmbClassifAdapter.class);

	/** индекс от спецификите, в които стои ID на регистратура. Integer ! */
	public static final int	USERS_INDEX_REGISTRATURA	= 0;
	/** индекс от спецификите, в които стои флаг за деловодител (ДА(1)/НЕ(2)). Integer ! */
	public static final int	USERS_INDEX_DELOVODITEL		= 1;
	/** индекс от спецификите, в които стои флаг за Дублиране на съобщения по e-mail (ДА(1)/НЕ(2)). Integer ! */
	public static final int	USERS_INDEX_DUBL_MAIL		= 2;

	/** индекс от спецификите, в които стои вид на участник (служител/звено). Integer ! */
	public static final int	ADM_STRUCT_INDEX_REF_TYPE		= 0;
	/** индекс от спецификите, в които стои ID на регистратура. Integer ! */
	public static final int	ADM_STRUCT_INDEX_REGISTRATURA	= 1;
	/** индекс от спецификите, в които стои имейл за контакт. String ! */
	public static final int	ADM_STRUCT_INDEX_CONTACT_EMAIL	= 2;
	/** индекс от спецификите, в които стои длъжност. Integer ! */
	public static final int	ADM_STRUCT_INDEX_POSITION		= 3;

	/** индекс от спецификите, в които стои вид на участник (фзл/нфл). Integer ! */
	public static final int	REFERENTS_INDEX_REF_TYPE		= 0;
	/** индекс от спецификите, в които стои имейл за контакт. String ! */
	public static final int	REFERENTS_INDEX_CONTACT_EMAIL	= 1;
	/** индекс от спецификите, в които стои ЕИК/ЕГН в зависимост от вида. String ! */
	public static final int	REFERENTS_INDEX_EIK_EGN			= 2;
	/** индекс от спецификите, в които стои Категория на нарушител. Integer ! класификация {@link OmbConstants#CODE_CLASSIF_KAT_NAR} */
	public static final int	REFERENTS_INDEX_KAT_NAR			= 3;
	/** индекс от спецификите, в които стои Вид на нарушител. Integer ! класификация {@link OmbConstants#CODE_CLASSIF_VID_NAR} */
	public static final int	REFERENTS_INDEX_VID_NAR			= 4;

	/** индекс от спецификите, в които стои актуална или не за регистратурата. Integer ! */
	public static final int	REGISTRATURI_INDEX_VALID	= 0;
	/** индекс от спецификите, в които стоят списъка от регистрите разделени със запетая. String ! */
	public static final int	REGISTRATURI_INDEX_REGISTRI	= 1;
	/** индекс от спецификите, в които guid-a за СЕОС! */
	public static final int	REGISTRATURI_INDEX_GUID_SEOS	= 2;
	/** индекс от спецификите, в които guid-a за ССЕВ! */
	public static final int	REGISTRATURI_INDEX_GUID_SSEV	= 3;

	/** индекс от спецификите, в които стои кода на алгоритъма. Integer! */
	public static final int	REGISTRI_INDEX_ALG			= 0;
	/** индекс от спецификите, в които стои вида на регистъра. Integer! */
	public static final int	REGISTRI_INDEX_TYPE			= 1;
	/** индекс от спецификите, в които стои типа на документа за регистъра. Integer! */
	public static final int	REGISTRI_INDEX_DOC_TYPE		= 2;
	/** индекс от спецификите, в които стои в които стои актуален или не за регистъра. Integer! */
	public static final int	REGISTRI_INDEX_VALID		= 3;
	/** индекс от спецификите, в които стои в регистратурата за регистъра (за общите NULL). Integer! */
	public static final int	REGISTRI_INDEX_REGISTRATURA	= 4;

	/** индекс от спецификите, в които стои регистратурата за групата. Integer! */
	public static final int	REG_GROUP_INDEX_REGISTRATURA	= 0;
	/** индекс от спецификите, в които стоят списъка от кодове на членове разделени със запетая. String ! */
	public static final int	REG_GROUP_INDEX_MEMBERS			= 1;
	/** индекс от спецификите, в които стои тип на групата (кореспонденти,сеос,ссев). Integer ! */
	public static final int	REG_GROUP_INDEX_TYPE			= 2;

	/** индекс от спецификите, в които стои ID на регистратура. Integer ! */
	public static final int MAILBOXES_INDEX_REGISTRATURA = 0;

	/** индекс от спецификите, в които стои ЕИК String ! */
	public static final int	ORGANISATIONS_INDEX_EIK			= 0;
	/** индекс от спецификите, в които стои имейл за контакт. String ! */
	public static final int	ORGANISATIONS_INDEX_REC_GUID	= 1;

	/** индекс от спецификите, в които стои регистратура. Integer! */
	public static final int	PROCEDURI_INDEX_REGISTRATURA	= 0;
	/** индекс от спецификите, в които стои статус . Integer! */
	public static final int	PROCEDURI_INDEX_STATUS			= 1;
	/** индекс от спецификите, в които стои статус . Integer! - ако няма дефиниран тип ще стои 0, за да може да се филтрира през специфики */
	public static final int	PROCEDURI_INDEX_DOC_TYPE		= 2;

	OmbClassifAdapter(BaseSystemData sd) {
		super(sd);
	}

	/**
	 * Административна структура -specifics:<br>
	 * [0]=REF_TYPE<br>
	 * [1]=REF_REGISTRATURA<br>
	 * [2]=CONTACT_EMAIL<br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassAdmStruct(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassAdmStruct(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();

		try {
			// селекта за мигрираните и затворени значения
			String migSql = " select r.REF_ID, r.CODE, r.CODE_PREV, r.CODE_PARENT, r.LEVEL_NUMBER, r.REF_NAME, r.DATE_OT, r.DATE_DO, r.DATE_REG, r.DATE_LAST_MOD " //
				+ " from ADM_REFERENTS r where r.CODE_CLASSIF = :codeClassif and r.REF_TYPE = :migRefType ";

			@SuppressWarnings("unchecked")
			Stream<Object[]> migRows = JPA.getUtil().getEntityManager().createNativeQuery(migSql) //
				.setParameter("codeClassif", codeClassif) //
				.setParameter("migRefType", OmbConstants.CODE_ZNACHENIE_REF_TYPE_MIG) //
				.setHint(QueryHints.HINT_FETCH_SIZE, 5000) //
				.getResultStream();

			Iterator<Object[]> migIter = migRows.iterator();
			while (migIter.hasNext()) {
				Object[] row = migIter.next();

				SystemClassif item = new SystemClassif();

				item.setCodeClassif(codeClassif);

				item.setId(((Number) row[0]).intValue());
				item.setCode(((Number) row[1]).intValue());
				item.setCodePrev(((Number) row[2]).intValue());
				item.setCodeParent(((Number) row[3]).intValue());
				item.setLevelNumber(((Number) row[4]).intValue());

				item.setTekst((String) row[5]);
				item.setDateOt((Date) row[6]);
				item.setDateDo((Date) row[7]);

				item.setDateReg((Date) row[8]);
				item.setDateLastMod((Date) row[9]);

				// TODO трябва ли да има специфики, дори и празни !?!? регистратура и в codeExt

				classif.add(item);
			}

			// селекта за първо ниво
			String level1sql = " select r.REF_ID, r.CODE, r.CODE_PARENT, r.REF_NAME, r.DATE_OT, r.DATE_DO "
				+ " , r.REF_TYPE, r.REF_REGISTRATURA, r.CONTACT_EMAIL, r.EMPL_POSITION, NULL ZVENO_NAME, r.DATE_REG, r.DATE_LAST_MOD, r.CODE_PREV " //
				+ " from ADM_REFERENTS r where r.CODE_CLASSIF = :codeClassif and r.CODE_PARENT = 0 and r.REF_TYPE != :migRefType ";

			@SuppressWarnings("unchecked")
			List<Object[]> level1rows = JPA.getUtil().getEntityManager().createNativeQuery(level1sql) //
				.setParameter("codeClassif", codeClassif) //
				.setParameter("migRefType", OmbConstants.CODE_ZNACHENIE_REF_TYPE_MIG) //
				.getResultList();

			// селекта за следващите нива, където трябват данни за родителя
			String sql = " select t.* from ( select r.REF_ID, r.CODE, r.CODE_PARENT, r.REF_NAME " //
				+ " , case when r.DATE_OT < p.DATE_OT then p.DATE_OT else r.DATE_OT end DATE_OT " //
				+ " , case when r.DATE_DO > p.DATE_DO then p.DATE_DO else r.DATE_DO end DATE_DO " //
				+ " , r.REF_TYPE, r.REF_REGISTRATURA, r.CONTACT_EMAIL, r.EMPL_POSITION, p.REF_NAME ZVENO_NAME, r.DATE_REG, r.DATE_LAST_MOD, r.CODE_PREV, p.REF_ID KEY_ID " //
				+ " from ADM_REFERENTS p " //
				+ " inner join ADM_REFERENTS r on r.CODE_PARENT = p.CODE " //
				+ " where p.CODE_CLASSIF = :codeClassif and p.REF_TYPE != :migRefType and r.REF_TYPE != :migRefType) t where t.DATE_OT < t.DATE_DO "
				+ " or (t.DATE_OT = t.DATE_DO and t.REF_TYPE = 2) "; // допълвам и служители с равни дати от-до

			@SuppressWarnings("unchecked")
			Stream<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(sql) //
				.setParameter("codeClassif", codeClassif) //
				.setParameter("migRefType", OmbConstants.CODE_ZNACHENIE_REF_TYPE_MIG) //
				.setHint(QueryHints.HINT_FETCH_SIZE, 500) //
				.getResultStream();

			Map<Integer, List<Object[]>> map = new HashMap<>(); // в този мап ще има всички данни от БД, за да може рекурсивно да
																// се направи дървото и то да е правилно
			Iterator<Object[]> iter = rows.iterator();
			while (iter.hasNext()) {
				Object[] row = iter.next();

				Integer key = asInteger(row[14]);

				List<Object[]> temp = map.get(key);
				if (temp == null) {
					temp = new ArrayList<>();
					map.put(key, temp);
				}
				temp.add(row);
			}

			boolean appendPostion = "1".equals(getSd().getSettingsValue("system.admStructEmplPosition")); // ще се лепи ли
																											// длъжностат към
																											// името на човека
			List<SystemClassif> oneDayEmpl = new ArrayList<>();
			Set<String> added = new HashSet<>();
			for (Object[] row : level1rows) { // рекурсивно трябва цялото дърво да се зареди, като се започва от данните на първо
												// ниво
				SystemClassif item = new SystemClassif();

				item.setCodeClassif(codeClassif);
				item.setLevelNumber(1);

				setAdmStructItemData(row, item, null, appendPostion); // сетвам специфичните данни на елемента

				createAdmStructTree(classif, item, map, added, appendPostion, oneDayEmpl); // рекусрсивно за подчинените
			}

			// иска се и служители с един ден в админ структурата да присъстват за разкодиране
			for (SystemClassif empl : oneDayEmpl) {
				String key = empl.getCode() + "_" + empl.getDateOt().getTime();
				if (!added.add(key)) { // това гарантира че няма да има за тази датаот с друга датадо
					continue;
				}
				classif.add(empl);
			}

		} catch (DbErrorException e) {
			throw e;
		} catch (Exception e) {
			throw new DbErrorException(e);
		}

		return classif;
	}

	/**
	 * Административна структура за справки (+напуснали)
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassAdmStructReports(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassAdmStructReports(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();

		Date today = DateUtils.startDate(new Date());
		Date systemMinDate = DateUtils.systemMinDate();

		List<SystemClassif> items = getSd().getFullClassification(Constants.CODE_CLASSIF_ADMIN_STR, lang, true);

		Set<Integer> actualCodes = new HashSet<>(); // към днешна дата

		Map<Integer, SystemClassif> closedItems = new HashMap<>(); // затворените, като накрая от тях ще останат само напусналите

		Map<Integer, Integer> level1prev = new HashMap<>(); // това ми трябва за да намеря след кой елемент ще се сложи звено
															// напуснали
		for (SystemClassif current : items) {
			if (current.getDateDo().getTime() <= today.getTime()) {

				if (current.getSpecifics() == null // това са мигрираните, които не ги знаем какви са и трябва да влезнат и те
					|| current.getSpecifics()[ADM_STRUCT_INDEX_REF_TYPE].equals(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL)) { // само
																																// служители
					SystemClassif temp = closedItems.get(current.getCode());

					if (temp == null || temp.getDateOt().getTime() < current.getDateOt().getTime()) {
						closedItems.put(current.getCode(), current); // последното им срещане в класификацията
					}
				}
				continue;
			}

			actualCodes.add(current.getCode());

			if (current.getLevelNumber() == 1) { // за момента напусналите ще се слагат на първо ниво на последно място
				level1prev.put(current.getCodePrev(), current.getCode());
			}

			SystemClassif item = new SystemClassif();

			item.setId(current.getId());
			item.setCode(current.getCode());
			item.setCodeParent(current.getCodeParent());
			item.setCodePrev(current.getCodePrev());
			item.setCodeClassif(codeClassif);

			item.setLevelNumber(current.getLevelNumber());
			item.setDateOt(today); // винаги от днешна дата
			item.setTekst(current.getTekst());
			item.setDopInfo(current.getDopInfo());
			item.setDateReg(systemMinDate);
			item.setCodeExt(current.getCodeExt());

			classif.add(item); // тук добавям актуален
		}

		List<SystemClassif> napusnali = new ArrayList<>();
		for (Entry<Integer, SystemClassif> entry : closedItems.entrySet()) {
			if (!actualCodes.contains(entry.getKey())) {
				napusnali.add(entry.getValue());
				actualCodes.add(entry.getKey()); // този ще се добави и реално става актуален
			}
		}

		if (!napusnali.isEmpty()) { // може и да няма, защото това че нещо е затворено не значи че е напуснал
			SystemClassif zvenoNapusnali = new SystemClassif();

			zvenoNapusnali.setId(-1000);
			zvenoNapusnali.setCode(zvenoNapusnali.getId());
			zvenoNapusnali.setCodeParent(0);
			zvenoNapusnali.setCodeClassif(codeClassif);

			for (Integer x : level1prev.values()) {
				if (!level1prev.containsKey(x)) { // ако не се съдържа означава че е последния, защото никой не е казал този код
													// за предходен
					zvenoNapusnali.setCodePrev(x);
					break;
				}
			}
			zvenoNapusnali.setLevelNumber(1);

			zvenoNapusnali.setDateOt(today); // винаги от днешна дата
			zvenoNapusnali.setTekst("Напуснали");
			zvenoNapusnali.setDopInfo("");
			zvenoNapusnali.setDateReg(systemMinDate);

			classif.add(zvenoNapusnali); // корена на напусналите е напослесно място след останалите на първо ниво

			@SuppressWarnings("unchecked") // тука трябва да излезат само тези, които имат само един ред с равни дати !!!
			List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(
				"select distinct r.REF_ID, r.CODE, r.REF_NAME, r.DATE_DO, r.CODE_PARENT, r.EMPL_POSITION from ADM_REFERENTS r"
				+ " where r.CODE_CLASSIF = ?1 and r.REF_TYPE = ?2 and r.DATE_OT = r.DATE_DO"
				+ " and not exists (select v.REF_ID from ADM_REFERENTS v where v.CODE = r.CODE and v.REF_ID <> r.REF_ID)")
				.setParameter(1, Constants.CODE_CLASSIF_ADMIN_STR).setParameter(2, OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL)
				.getResultList();

			for (Object[] row : rows) {
				int code = ((Number) row[1]).intValue();
				if (actualCodes.contains(code)) {
					continue; // това ще гарантира че няма да има никакво повтаряне на кодове
				}
				actualCodes.add(code);
				
				SystemClassif sc = new SystemClassif();
				sc.setId(((Number) row[0]).intValue());
				sc.setCode(code);
				sc.setTekst((String) row[2]);
				sc.setDateDo((Date) row[3]);

				if (row[4] != null) {
					int codeParent = ((Number) row[4]).intValue();

					sc.setDopInfo(getSd().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, codeParent, lang, sc.getDateDo()));
					Integer registratura = (Integer) getSd().getItemSpecific(Constants.CODE_CLASSIF_ADMIN_STR, codeParent, lang, sc.getDateDo(), ADM_STRUCT_INDEX_REGISTRATURA);
					if (registratura != null) {
						sc.setCodeExt(registratura.toString());
					}
				}
				if (row[5] != null) {
					sc.setTekst(sc.getTekst() +" ("+ getSd().decodeItem(CODE_CLASSIF_POSITION, ((Number) row[5]).intValue(), lang, sc.getDateDo()) +")");
				}
				napusnali.add(sc);
			}

			napusnali.sort((sc1, sc2) -> sc1.getTekst().compareTo(sc2.getTekst())); // !!! сортиране по име

			int codePrev = 0;
			for (SystemClassif current : napusnali) {
				SystemClassif item = new SystemClassif();

				item.setId(current.getId());
				item.setCode(current.getCode());
				item.setCodeParent(zvenoNapusnali.getCode());
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(zvenoNapusnali.getLevelNumber() + 1);
				item.setDateOt(today); // винаги от днешна дата
				item.setTekst(current.getTekst());
				item.setDopInfo(current.getDopInfo());
				item.setDateReg(systemMinDate);
				item.setCodeExt(current.getCodeExt());

				codePrev = item.getCode();

				classif.add(item);  // тук добавям напуснал
			}
		}

		return classif;
	}

	/**
	 * Административна структура само звена
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassAdmStructZvena(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassAdmStructZvena(codeClassif={},lang={})", codeClassif, lang);

		// трябват ми само звената и пускам специфика по тип=звено
		Map<Integer, Object> map = Collections.singletonMap(OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE, CODE_ZNACHENIE_REF_TYPE_ZVENO);

		List<SystemClassif> items = getSd().queryClassification(CODE_CLASSIF_ADMIN_STR, null, null, lang, map);

		int id = 0;
		Date systemMinDate = DateUtils.systemMinDate();

		for (SystemClassif item : items) { // липсват служители и трябва да се оправи дървото за да се покаже коректно

			item.setId(++id);
			item.setCodeClassif(codeClassif);
			item.setDateOt(systemMinDate);
			item.setDateReg(systemMinDate);
			
			if (item.getCodePrev() == 0) {
				continue; // щом е първи значи е ОК
			}

			Integer refType = (Integer) getSd().getItemSpecific(CODE_CLASSIF_ADMIN_STR, item.getCodePrev(), lang, null, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);
			if (Objects.equals(refType, CODE_ZNACHENIE_REF_TYPE_EMPL)) {
				item.setCodePrev(0); // ако предходният е служител значи този става първи
			}
		}
		return items;
	}

	/**
	 * Динамична класификация за Схеми за съхранение на документи. <br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassDocShema(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassDocShema(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		try {
			Date systemMinDate = DateUtils.systemMinDate();

			@SuppressWarnings("unchecked")
			List<Object[]> settings = JPA.getUtil().getEntityManager() //
				.createNativeQuery("select s.SHEMA_ID, s.PREFIX, s.SHEMA_NAME, s.FROM_YEAR, s.TO_YEAR, s.DATE_REG, s.DATE_LAST_MOD" //
					+ " from DOC_SHEMA s") //
				.getResultList();

			int codePrev = 0;
			for (Object[] row : settings) {
				SystemClassif item = new SystemClassif();

				String tekst = row[1] + " - " + row[2];

				item.setId(asInteger(row[0]));
				item.setCode(item.getId());
				item.setCodeParent(0);
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);

				Integer fromYear = asInteger(row[3]);
				if (fromYear != null) {
					item.setDateOt(DateUtils.parse("01.01." + fromYear));
				} else {
					item.setDateOt(systemMinDate);
				}

				Integer toYear = asInteger(row[4]);
				if (toYear != null) {
					item.setDateDo(DateUtils.parse("31.12." + toYear));
				}

				item.setTekst(tekst);

				codePrev = item.getCode();

				item.setDateReg((Date) row[5]);
				item.setDateLastMod((Date) row[6]);

				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return classif;
	}

	/**
	 * Динамична класификация за Настройки по вид документ. <br>
	 * specifics:<br>
	 * [0]-SHEMA_ID<br>
	 * [1]-REGISTER_ID<br>
	 * [2]-CREATE_DELO<br>
	 * [3]-FOR_REG_ID<br>
	 * [4]-FILE_OBJECTS.OBJECT_ID !ИД-то на настройката само ако има шаблони!<br>
	 * [5]-PROC_DEF_IN<br>
	 * [6]-PROC_DEF_OWN<br>
	 * [7]-PROC_DEF_WORK<br>
	 * [8]-DOC_HAR<br>
	 * [9]-MEMBERS_TAB<br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassDocVidSettings(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassDocVidSettings(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		try {
			Date systemMinDate = DateUtils.systemMinDate();

			@SuppressWarnings("unchecked")
			List<Object[]> procDefMaxDates = JPA.getUtil().getEntityManager().createNativeQuery("select max (def.DATE_REG) MAX_DEF_REG, max(def.DATE_LAST_MOD) MAX_DEF_MOD from PROC_DEF def")
				.getResultList();
			Date maxDefReg = null;
			Date maxDefMod = null;
			if (!procDefMaxDates.isEmpty()) {
				maxDefReg = (Date) procDefMaxDates.get(0)[0];
				maxDefMod = (Date) procDefMaxDates.get(0)[1];
			}

			StringBuilder sql = new StringBuilder();
			sql.append(" select s.ID, s.REGISTRATURA_ID, s.SHEMA_ID, s.REGISTER_ID, s.DOC_VID, s.CREATE_DELO, s.FOR_REG_ID, fo.OBJECT_ID ");
			sql.append(" , s.DATE_REG, s.DATE_LAST_MOD, max (fo.DATE_REG) MAX_FO_REG, max (fo.DATE_LAST_MOD) MAX_FO_MOD ");
			sql.append(" , defIn.DEF_ID DEFIN_ID, defOwn.DEF_ID DEFOWN_ID, defWork.DEF_ID DEFWORK_ID, s.DOC_HAR, s.MEMBERS_ACTIVE, s.MEMBERS_TAB ");
			sql.append(" from DOC_VID_SETTINGS s ");
			sql.append(" left outer join FILE_OBJECTS fo on fo.OBJECT_ID = s.ID and fo.OBJECT_CODE = :foCode ");
			sql.append(" left outer join PROC_DEF defIn on defIn.DEF_ID = s.PROC_DEF_IN and defIn.STATUS = :defActSts and defIn.DOC_TYPE = 1 ");
			sql.append(" left outer join PROC_DEF defOwn on defOwn.DEF_ID = s.PROC_DEF_OWN and defOwn.STATUS = :defActSts and defOwn.DOC_TYPE = 2 ");
			sql.append(" left outer join PROC_DEF defWork on defWork.DEF_ID = s.PROC_DEF_WORK and defWork.STATUS = :defActSts and defWork.DOC_TYPE = 3 ");
			sql.append(" group by s.ID, s.REGISTRATURA_ID, s.SHEMA_ID, s.REGISTER_ID, s.DOC_VID, s.CREATE_DELO, s.FOR_REG_ID, fo.OBJECT_ID, s.DATE_REG, s.DATE_LAST_MOD ");
			sql.append(" , defIn.DEF_ID, defOwn.DEF_ID, defWork.DEF_ID, s.DOC_HAR, s.MEMBERS_ACTIVE, s.MEMBERS_TAB ");

			@SuppressWarnings("unchecked")
			List<Object[]> settings = JPA.getUtil().getEntityManager() //
				.createNativeQuery(sql.toString()).setParameter("foCode", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_VID_SETT)
				.setParameter("defActSts", OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE) // само активни трябва да са в
																								// класификацията
				.getResultList();

			int codePrev = 0;
			for (Object[] row : settings) {
				SystemClassif item = new SystemClassif();

				String tekst = row[1] + "_" + row[4]; // това ще се използва за името, а като му се направи hashCode() ще се
														// използва за кода
				item.setId(tekst.hashCode());
				item.setCode(item.getId());
				item.setCodeParent(0);
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);
				item.setDateOt(systemMinDate);
				item.setTekst(tekst);

				item.setCodeExt(String.valueOf(row[0])); // когато е нужно някъде ИД-то

				codePrev = item.getCode();

				item.setDateReg((Date) row[8]);

				List<Date> dates = new ArrayList<>(); // тък като в тази класификация са намесени 3 таблици трябва да знам за
														// всяка от тях датите, за да взема макс от всичи, която да се използва за
														// периодичната проверка за промени в кеша
				if (row[9] != null) {
					dates.add((Date) row[9]); // DATE_LAST_MOD
				}
				if (row[10] != null) {
					dates.add((Date) row[10]); // MAX_FO_REG
				}
				if (row[11] != null) {
					dates.add((Date) row[11]); // MAX_FO_MOD
				}

				if (maxDefReg != null) {
					dates.add(maxDefReg); // MAX_DEF_REG
				}
				if (maxDefMod != null) {
					dates.add(maxDefMod); // MAX_DEF_MOD
				}

				if (!dates.isEmpty()) {
					item.setDateLastMod(Collections.max(dates));
				}

				Integer defIn = asInteger(row[12]);
				Integer defOwn = asInteger(row[13]);
				Integer defWork = asInteger(row[14]);

				Integer docHar = asInteger(row[15]);
				Integer membersActive = asInteger(row[16]);
				String membersTab = null; 
				if (membersActive != null && membersActive.equals(SysConstants.CODE_ZNACHENIE_DA)) {
					membersTab = trimToNULL((String) row[17]);
					if (membersTab == null) {
						membersTab = "Участници"; // дефолта ако е включено, но няма име
					}
				}

				item.setSpecifics(new Object[] { asInteger(row[2]), asInteger(row[3]), asInteger(row[5]), asInteger(row[6]), asInteger(row[7]), defIn, defOwn, defWork, docHar, membersTab });
				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return classif;
	}

	/**
	 * EDELIVERY_ORGANISATIONS -specifics:<br>
	 * [0]=EIK<br>
	 * [1]=EMAIL<br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassEdeliveryOrganisations(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassEdeliveryOrganisations(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		try {
			Date systemMinDate = DateUtils.systemMinDate();

			// за тест и за бързодействие го правя на Stream като е доста важно да има QueryHints.HINT_FETCH_SIZE, за да се получи
			// реален ефект от цялата игра
			@SuppressWarnings("unchecked")
			Stream<Object[]> rows = JPA.getUtil().getEntityManager() //
				.createNativeQuery("select ID, EIK, ADMINISTRATIVE_NAME, ADDRESS_STATE, ADDRESS_CITY, ADDRESS_STREET, EMAIL, GUID from EDELIVERY_ORGANISATIONS") //
				.setHint(QueryHints.HINT_FETCH_SIZE, 5000) //
				.getResultStream();

			int codePrev = 0;

			Iterator<Object[]> iter = rows.iterator();
			while (iter.hasNext()) {
				Object[] row = iter.next();

				SystemClassif item = new SystemClassif();

				String eik = trimToNULL((String) row[1]);
				try {
					Long.parseLong(eik);
				} catch (Exception e) {
					eik = null; // не е число и няма как да е ЕИК
				}

				item.setId(asInteger(row[0]));
				item.setCode(item.getId());
				item.setCodeParent(0);
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);
				item.setDateOt(systemMinDate);
				if (eik != null) {
					item.setTekst(eik + " " + row[2]);
				} else {
					item.setTekst((String) row[2]);
				}

				StringBuilder dopInfo = new StringBuilder();

				String state = trimToNULL((String) row[3]);
				String city = trimToNULL((String) row[4]);
				String street = trimToNULL((String) row[5]);

				if (state != null) {
					dopInfo.append(state);

					if (city != null && !state.equalsIgnoreCase(city)) {
						dopInfo.append(", " + city); // за да не става грозно с две еднакви едно след друго
					}

				} else if (city != null) {
					dopInfo.append(city);
				}

				if (street != null) {
					if (dopInfo.length() > 0) {
						dopInfo.append(", ");
					}
					dopInfo.append(street);
				}

				if (dopInfo.length() > 0) { // все пак може и да няма нищо изчислено като адрес
					item.setDopInfo(dopInfo.toString());
				}

				codePrev = item.getCode();

				item.setDateReg(systemMinDate);

				item.setSpecifics(new Object[] { eik, (String) row[7], (String) row[2] });

				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return classif;
	}

	/**
	 * EGOV_ORGANISATIONS -specifics:<br>
	 * [0]=EIK<br>
	 * [1]=EMAIL<br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassEgovOrganisations(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassEgovOrganisations(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		try {
			Date systemMinDate = DateUtils.systemMinDate();

			@SuppressWarnings("unchecked")
			List<String> parents = JPA.getUtil().getEntityManager() //
				.createNativeQuery("select distinct upper(PARRENT_GUID) from EGOV_ORGANISATIONS where PARRENT_GUID is not null") //
				.getResultList();

			String sql = "select ID, EIK, ADMINISTRATIVE_BODY_NAME, upper(GUID), EMAIL, LAST_MOD_DATE from EGOV_ORGANISATIONS";

			@SuppressWarnings("unchecked")
			List<Object[]> orgs = JPA.getUtil().getEntityManager() //
				.createNativeQuery(sql + " where PARRENT_GUID is null").getResultList();

			sql += " where upper(PARRENT_GUID) = ?1"; // за подчинените

			int codePrev = 0;
			for (Object[] row : orgs) {
				SystemClassif item = new SystemClassif();

				String eik = trimToNULL((String) row[1]);

				item.setId(asInteger(row[0]));
				item.setCode(item.getId());
				item.setCodeParent(0); // първо ниво
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1); // първо ниво
				item.setDateOt(systemMinDate);
				item.setTekst(row[2] + " (" + eik + ")");

				item.setDateReg((Date) row[5]);

				codePrev = item.getCode();

				item.setSpecifics(new Object[] { eik, row[3], row[2] });

				createEgovOrgsTree(classif, item, sql, (String) row[3], parents); // рекурсия за подчинените
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return classif;
	}

	/** */
	@Override
	public List<SystemClassif> buildClassEKATTE(Integer codeClassif, Integer lang) throws DbErrorException {
		if (lang != null && lang.equals(SysConstants.CODE_LANG_EN)) {
			return buildClassEKATTEtransliterate(codeClassif);
		}

		LOGGER.debug("buildClassEKATTE(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> list = new ArrayList<>();
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select att.EKATTE, att.TVM, att.IME, att.OBSTINA, att.OBSTINA_IME, att.OBLAST, att.OBLAST_IME, 3 as TIP, att.DATE_OT, att.DATE_DO ");
			sql.append(" from EKATTE_ATT att ");
			sql.append(" union all ");
			sql.append(" select 0, 'обл.', oblasti.IME, null, null, oblasti.OBLAST, oblasti.IME, 1 as TIP, oblasti.DATE_OT, oblasti.DATE_DO ");
			sql.append(" from EKATTE_OBLASTI oblasti ");
			sql.append(" union all ");
			sql.append(" select 0, 'общ.', obstini.IME, obstini.OBSTINA, obstini.IME, ");
			sql.append(DialectConstructor.convertSQLSubstring(JPA.getUtil().getDbVendorName(), "obstini.OBSTINA", 1, 3));
			sql.append(" , obstini.OBLAST_IME, 2 as TIP, obstini.DATE_OT, obstini.DATE_DO ");
			sql.append(" from EKATTE_OBSTINI obstini ");
			sql.append(" order by 8,2,3");

			// за тест и за бързодействие го правя на Stream като е доста важно да има QueryHints.HINT_FETCH_SIZE, за да се получи
			// реален ефект от цялата игра
			@SuppressWarnings("unchecked")
			Stream<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
				.setHint(QueryHints.HINT_FETCH_SIZE, 5000) //
				.getResultStream();

			int id = 0;
			int codeOblObst = 100000;

			Iterator<Object[]> iter = rows.iterator();
			while (iter.hasNext()) {
				Object[] row = iter.next();

				SystemClassif item = new SystemClassif();

				int tip = ((Number) row[7]).intValue();

				final int code;
				if (tip == 3) { // за населените места е кода по еката
					code = ((Number) row[0]).intValue();

				} else { // за областите и общините се генерира
					code = ++codeOblObst;
				}

				item.setId(++id);
				item.setCode(code);
				item.setCodeClassif(codeClassif);

				item.setCodePrev(0); // само аутокомплете
				item.setCodeParent(0); // линейна !!!
				item.setLevelNumber(1);

				item.setDateOt((Date) row[8]);
				item.setDateDo((Date) row[9]);

				item.setTekst(row[1] + " " + row[2]);

				if (tip == 1) { // област

					item.setCodeExt((String) row[5]); // кода на областа

				} else if (tip == 2) { // за общините се казва коя е областа в dopInfo

					item.setCodeExt((String) row[3]); // кода на общината
					item.setDopInfo("обл. " + row[6]);

				} else if (tip == 3) { // за населените места всичко им се казва
					item.setDopInfo("общ. " + row[4] + ", обл. " + row[6]);
				}

				item.setSpecifics(new Object[] { tip });

				list.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return list;
	}

	/**
	 * Дава го транслитерирано на английски
	 * 
	 * @param codeClassif
	 * @return
	 * @throws DbErrorException 
	 */
	private List<SystemClassif> buildClassEKATTEtransliterate(Integer codeClassif) throws DbErrorException {
		List<SystemClassif> items = getSd().getFullClassification(codeClassif, SysConstants.CODE_LANG_BG, true);
		
		for (SystemClassif item : items) {
			if (item.getTekst() != null) {
				item.setTekst(StringUtils.transliterate(item.getTekst()));
			}
			if (item.getDopInfo() != null) {
				item.setDopInfo(StringUtils.transliterate(item.getDopInfo()));
			}
		}
		return items;
	}

	/**
	 * Динамична класификация за Регистрирани отсъствия.
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassEmplReplaces(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassEmplReplaces(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		try {
			@SuppressWarnings("unchecked")
			List<Object[]> settings = JPA.getUtil().getEntityManager() //
				.createNativeQuery("select ID, CODE_REF, USER_ID, DATE_OT, DATE_DO, DATE_REG, DATE_LAST_MOD from ADM_REF_DELEGATIONS where DELEGATION_TYPE = :delegationType") //
				.setParameter("delegationType", OmbConstants.CODE_ZNACHENIE_DELEGATES_ZAMESTVANE) //
				.getResultList();

			int codePrev = 0;
			for (Object[] row : settings) {
				SystemClassif item = new SystemClassif();

				item.setId(asInteger(row[0]));
				item.setCode(asInteger(row[1]));
				item.setCodeParent(0);
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);
				item.setDateOt((Date) row[3]);
				item.setDateDo((Date) row[4]);

				Integer userId = asInteger(row[2]);

				String tekst = getSd().decodeItem(CODE_CLASSIF_ADMIN_STR, item.getCode(), lang, null) //
					+ " се замества от " //
					+ getSd().decodeItem(CODE_CLASSIF_ADMIN_STR, userId, lang, null) //
					+ " в периода " + DateUtils.printDate(item.getDateOt()) + " - " + DateUtils.printDate(item.getDateDo());

				item.setTekst(tekst);
				item.setCodeExt(String.valueOf(userId));

				codePrev = item.getCode();

				item.setDateReg((Date) row[5]);
				item.setDateLastMod((Date) row[6]);

				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return classif;
	}

	/**
	 * Динамична класификация за Групи кореспонденти<br>
	 * specifics:<br>
	 * [0]=REGISTRATURA_ID<br>
	 * [1]=memberCode1,memberCode2,...,<br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassGroupCorrespondents(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassGroupCorrespondents(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		try {
			Date systemMinDate = DateUtils.systemMinDate();

			List<Integer> groupTypeList = Arrays.asList(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP, OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS);

			@SuppressWarnings("unchecked")
			List<Object[]> registri = JPA.getUtil().getEntityManager() //
				.createNativeQuery("select GROUP_ID, GROUP_NAME, REGISTRATURA_ID, GROUP_TYPE, DATE_REG, DATE_LAST_MOD from REGISTRATURA_GROUPS where GROUP_TYPE in (:groupTypeList)") //
				.setParameter("groupTypeList", groupTypeList).getResultList();

			int codePrev = 0;
			for (Object[] row : registri) {
				SystemClassif item = new SystemClassif();

				item.setId(asInteger(row[0]));
				item.setCode(item.getId());
				item.setCodeParent(0);
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);
				item.setDateOt(systemMinDate);
				item.setTekst((String) row[1]);

				codePrev = item.getCode();

				item.setDateReg((Date) row[4]);
				item.setDateLastMod((Date) row[5]);

				item.setSpecifics(new Object[] { asInteger(row[2]), findMemberCodes(item.getId()), asInteger(row[3]) });
				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return classif;
	}

	/**
	 * Динамична класификация за Групи служители<br>
	 * specifics:<br>
	 * [0]=REGISTRATURA_ID<br>
	 * [1]=memberCode1,memberCode2,...,<br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassGroupEmployees(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassGroupEmployees(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		try {
			Date systemMinDate = DateUtils.systemMinDate();

			@SuppressWarnings("unchecked")
			List<Object[]> registri = JPA.getUtil().getEntityManager() //
				.createNativeQuery("select GROUP_ID, GROUP_NAME, REGISTRATURA_ID, GROUP_TYPE, DATE_REG, DATE_LAST_MOD from REGISTRATURA_GROUPS where GROUP_TYPE = :groupType") //
				.setParameter("groupType", OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_EMPL).getResultList();

			int codePrev = 0;
			for (Object[] row : registri) {
				SystemClassif item = new SystemClassif();

				item.setId(asInteger(row[0]));
				item.setCode(item.getId());
				item.setCodeParent(0);
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);
				item.setDateOt(systemMinDate);
				item.setTekst((String) row[1]);

				codePrev = item.getCode();

				item.setDateReg((Date) row[4]);
				item.setDateLastMod((Date) row[5]);

				item.setSpecifics(new Object[] { asInteger(row[2]), findMemberCodes(item.getId()) });
				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return classif;
	}

	/**
	 * Динамична класификация за Пощенски кутии. <br>
	 * specifics:<br>
	 * [0]=REGISTRATURA_ID<br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassMailboxes(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassMailboxes(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		try {
			Date systemMinDate = DateUtils.systemMinDate();

			@SuppressWarnings("unchecked")
			List<Object[]> mailboxes = JPA.getUtil().getEntityManager().createNativeQuery( //
				"select MAILBOX_ID, REGISTRATURA_ID, MAILBOX_NAME, MAILBOX_USERNAME, MAILBOX_PASSWORD, DATE_REG, DATE_LAST_MOD " //
					+ " from REGISTRATURA_MAILBOXES order by REGISTRATURA_ID, MAILBOX_NAME")
				.getResultList();

			int codePrev = 0;
			for (Object[] row : mailboxes) {
				SystemClassif item = new SystemClassif();

				item.setId(asInteger(row[0]));
				item.setCode(item.getId());
				item.setCodeParent(0);
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);
				item.setDateOt(systemMinDate);
				item.setTekst((String) row[2]);

				codePrev = item.getCode();

				item.setDateReg((Date) row[5]);
				item.setDateLastMod((Date) row[6]);

				item.setSpecifics(new Object[] { asInteger(row[1]) });

				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return classif;
	}

	/**
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassProceduri(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassProceduri(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();

		try {
			Date systemMinDate = DateUtils.systemMinDate();

			StringBuilder sql = new StringBuilder();
			sql.append(" select p.DEF_ID, p.REGISTRATURA_ID, p.PROC_NAME, p.PROC_INFO, p.STATUS, p.DATE_REG, p.DATE_LAST_MOD, p.DOC_TYPE ");
			sql.append(" from PROC_DEF p ");
			sql.append(" order by p.PROC_NAME, p.DEF_ID desc ");

			@SuppressWarnings("unchecked")
			List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()).getResultList();

			int codePrev = 0;
			for (Object[] row : rows) {

				SystemClassif item = new SystemClassif();

				item.setId(((Number) row[0]).intValue());
				item.setCode(item.getId());
				item.setCodeParent(0);
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);
				item.setDateOt(systemMinDate);

				item.setTekst((String) row[2] + " (" + item.getId() + ")");
//				item.setDopInfo((String) row[3]);

				item.setDateReg((Date) row[5]);
				item.setDateLastMod((Date) row[6]);

				codePrev = item.getCode();

				int docType = row[7] != null ? ((Number)row[7]).intValue() : 0;
				
				item.setSpecifics(new Object[] { asInteger(row[1]), asInteger(row[4]), docType });

				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}

		return classif;
	}

	/**
	 * Дава списъка като линейна класификация за кореспонденти
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassReferents(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassReferents(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		Date date = new Date();

		try {
			int countryBG = Integer.parseInt(getSd().getSettingsValue("delo.countryBG"));

			Query query = JPA.getUtil().getEntityManager().createNativeQuery(getReferentsBaseSelect() + " order by r.REF_ID");

			@SuppressWarnings("unchecked")
			List<Object[]> rows = query.getResultList();

			for (Object[] row : rows) {

				SystemClassif item = new SystemClassif();

				item.setCodeClassif(codeClassif);
				item.setCodePrev(0); // подредбата тука не се следи
				item.setCodeParent(0);
				item.setLevelNumber(1);

				setReferentItemData(row, item, lang, date, countryBG); // сетвам специфичните данни на елемента

				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return classif;
	}

	/**
	 * Дава списъка като линейна класификация за Органи по НПМ
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassOrgansNpm(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassOrgansNpm(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		Date date = new Date();

		try {
			int countryBG = Integer.parseInt(getSd().getSettingsValue("delo.countryBG"));

			Query query = JPA.getUtil().getEntityManager().createNativeQuery(getReferentsBaseSelect() 
				+ " and r.tip_organ is not null " + " order by r.REF_ID");

			@SuppressWarnings("unchecked")
			List<Object[]> rows = query.getResultList();
			for (Object[] row : rows) {

				SystemClassif item = new SystemClassif();

				item.setCodeClassif(codeClassif);
				item.setCodePrev(0); // подредбата тука не се следи
				item.setCodeParent(0);
				item.setLevelNumber(1);

				setReferentItemData(row, item, lang, date, countryBG); // сетвам специфичните данни на елемента

				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return classif;
	}

	/**
	 * Динамична класификация за Регистратури. <br>
	 * specifics:<br>
	 * [0]=VALID<br>
	 * [1]=registerId1,registerId2,...,<br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassRegistraturi(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassRegistraturi(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		try {
			Date systemMinDate = DateUtils.systemMinDate();

			StringBuilder sql = new StringBuilder("select REGISTRATURA_ID, REGISTRATURA, VALID, DATE_REG, DATE_LAST_MOD, GUID_SEOS, GUID_SSEV, ORG_NAME from REGISTRATURI");

			if (codeClassif.equals(OmbConstants.CODE_CLASSIF_REGISTRATURI_REQDOC)) { // за тази класификация ще се извадят само
																						// активните
				sql.append(" where VALID = :valid");
			}
			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString() + "  order by REGISTRATURA");

			if (codeClassif.equals(OmbConstants.CODE_CLASSIF_REGISTRATURI_REQDOC)) { // да добавя и параметъра
				query.setParameter("valid", CODE_ZNACHENIE_DA);
			}

			@SuppressWarnings("unchecked")
			List<Object[]> registraturi = query.getResultList();

			int codePrev = 0;
			for (Object[] row : registraturi) {
				SystemClassif item = new SystemClassif();

				item.setId(asInteger(row[0]));
				item.setCode(item.getId());
				item.setCodeParent(0);
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);
				item.setDateOt(systemMinDate);
				item.setTekst((String) row[1]);

				item.setDopInfo((String) row[7]); // има на организацията

				codePrev = item.getCode();

				item.setDateReg((Date) row[3]);
				item.setDateLastMod((Date) row[4]);
				
				String guidSeos = (String) row[5];
				String guidSSEV = (String) row[6];

				//item.setCodeExt((String) row[5]);

				Integer valid = asInteger(row[2]);

				String registers;
				if (codeClassif.equals(OmbConstants.CODE_CLASSIF_REGISTRATURI)) {
					registers = findRegisterIds(item.getId()); // само за основната класификация ще го има зареден
				} else {
					registers = "";
				}
				item.setSpecifics(new Object[] { valid, registers, guidSeos, guidSSEV });

				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}

		return classif;
	}

	/**
	 * Динамична класификация за Регистри. <br>
	 * specifics:<br>
	 * [0]=ALG<br>
	 * [1]=REGISTER_TYPE<br>
	 * [2]=DOC_TYPE<br>
	 * [3]=VALID<br>
	 * [4]=REGISTRATURA_ID<br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassRegistri(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassRegistri(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		try {
			Date systemMinDate = DateUtils.systemMinDate();

			@SuppressWarnings("unchecked")
			List<Object[]> registri = JPA.getUtil().getEntityManager() //
				.createNativeQuery(
					"select REGISTER_ID, REGISTER, ALG, REGISTER_TYPE, DOC_TYPE, VALID, DATE_REG, DATE_LAST_MOD, COMMON, REGISTRATURA_ID, PREFIX from REGISTRI order by COMMON, SORT_NOMER")
				.getResultList();

			int codePrev = 0;
			for (Object[] row : registri) {
				SystemClassif item = new SystemClassif();

				item.setId(asInteger(row[0]));
				item.setCode(item.getId());
				item.setCodeParent(0);
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);
				item.setDateOt(systemMinDate);
				item.setTekst((String) row[1]);

				codePrev = item.getCode();

				item.setDateReg((Date) row[6]);
				item.setDateLastMod((Date) row[7]);

				Integer common = asInteger(row[8]);
				Integer registratura = Objects.equals(common, CODE_ZNACHENIE_DA) ? null : asInteger(row[9]);

				item.setDopInfo(trimToNULL((String) row[10]));

				item.setSpecifics(new Object[] { asInteger(row[2]), asInteger(row[3]), asInteger(row[4]), asInteger(row[5]), registratura });
				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return classif;
	}

	/**
	 * Динамична класификация за Регистри - сортирани за регистратура. <br>
	 * <b>!винаги трябва да се пуска специфика по регистратура!</b><br>
	 * specifics:<br>
	 * [0]=ALG<br>
	 * [1]=REGISTER_TYPE<br>
	 * [2]=DOC_TYPE<br>
	 * [3]=VALID<br>
	 * [4]=REGISTRATURA_ID<br>
	 *
	 * @param codeClassif
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<SystemClassif> buildClassRegistriSorted(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassRegistriSorted(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();
		try {
			Date systemMinDate = DateUtils.systemMinDate();

			StringBuilder sql = new StringBuilder();
			sql.append(" select r.REGISTER_ID, r.REGISTER, r.ALG, r.REGISTER_TYPE, r.DOC_TYPE, r.VALID, r.DATE_REG, r.DATE_LAST_MOD, r.COMMON, r.SORT_NOMER, r.REGISTRATURA_ID ");
			sql.append(" from REGISTRI r ");
			sql.append(" union ");
			sql.append(" select r.REGISTER_ID, r.REGISTER, r.ALG, r.REGISTER_TYPE, r.DOC_TYPE, r.VALID, r.DATE_REG, r.DATE_LAST_MOD, r.COMMON, r.SORT_NOMER, rr.REGISTRATURA_ID ");
			sql.append(" from REGISTRI r ");
			sql.append(" inner join REGISTRATURA_REGISTER rr on rr.REGISTER_ID = r.REGISTER_ID ");
			sql.append(" order by 9, 10, 2 "); // COMMON, SORT_NOMER, REGISTER

			@SuppressWarnings("unchecked")
			List<Object[]> registri = JPA.getUtil().getEntityManager() //
				.createNativeQuery(sql.toString()).getResultList();

			Map<Integer, Date> dateOtMap = new HashMap<>(); // за да няма дублиране по дата от за дублираните заради
															// регистратурите регистри
			Map<Integer, Integer> codePrevMap = new HashMap<>(); // за да се направи сортирането за регистратура

			for (Object[] row : registri) {
				Integer registratura = asInteger(row[10]);

				Integer codePrev = codePrevMap.get(registratura);
				if (codePrev == null) {
					codePrev = 0;
					codePrevMap.put(registratura, codePrev);
				}

				SystemClassif item = new SystemClassif();

				item.setId(asInteger(row[0]));
				item.setCode(item.getId());
				item.setCodeParent(0);
				item.setCodePrev(codePrevMap.get(registratura));
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);
				item.setTekst((String) row[1]);

				Date dateOt = dateOtMap.get(item.getCode());
				if (dateOt == null) {
					dateOt = systemMinDate;
				} else {
					dateOt = new Date(dateOt.getTime() + 24 * 60 * 60 * 1000); // следващия ден за да не реве h2-ката
				}
				dateOtMap.put(item.getCode(), dateOt);

				item.setDateOt(dateOt);

				codePrevMap.put(registratura, item.getCode()); // сортировката за всяка регистратура е отделна

				item.setDateReg((Date) row[6]);
				item.setDateLastMod((Date) row[7]);

				item.setSpecifics(new Object[] { asInteger(row[2]), asInteger(row[3]), asInteger(row[4]), asInteger(row[5]), registratura });
				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return classif;
	}

	/**
	 * Допълнително като специфика се добавя и: <br>
	 * [0]=REF_REGISTRATURA<br>
	 */
	@Override
	public List<SystemClassif> buildClassUsers(Integer codeClassif, Integer lang) throws DbErrorException {
		LOGGER.debug("buildClassUsers(codeClassif={},lang={})", codeClassif, lang);

		List<SystemClassif> classif = new ArrayList<>();

		try {
			Date date = new Date(); // за регистратура към момента
			Date systemMinDate = DateUtils.systemMinDate();

			StringBuilder sql = new StringBuilder();
			sql.append(" select distinct u.USER_ID, u.USERNAME, u.NAMES, u.DATE_REG, u.DATE_LAST_MOD, ur.CODE_ROLE DELOVODITEL, ur2.CODE_ROLE DUBL_MAIL ");
			sql.append(" from ADM_USERS u ");
			sql.append(" left outer join ADM_USER_ROLES ur on ur.USER_ID = u.USER_ID and ur.CODE_CLASSIF = :businessRole and ur.CODE_ROLE = :delovoditel ");
			sql.append(" left outer join ADM_USER_ROLES ur2 on ur2.USER_ID = u.USER_ID and ur2.CODE_CLASSIF = :userSettings and ur2.CODE_ROLE = :dublMail");
			sql.append(" order by u.NAMES, u.USERNAME ");

			@SuppressWarnings("unchecked")
			List<Object[]> rez = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString()) //
				.setParameter("businessRole", CODE_CLASSIF_BUSINESS_ROLE).setParameter("delovoditel", OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_DELOVODITEL) //
				.setParameter("userSettings", OmbConstants.CODE_CLASSIF_USER_SETTINGS).setParameter("dublMail", OmbConstants.CODE_ZNACHENIE_USER_SETT_DUBL_MAIL) //
				.getResultList();

			int codePrev = 0;
			for (Object[] row : rez) {
				SystemClassif item = new SystemClassif();

				item.setId(SearchUtils.asInteger(row[0]));
				item.setCode(SearchUtils.asInteger(row[0]));
				item.setCodeParent(0);
				item.setCodePrev(codePrev);
				item.setCodeClassif(codeClassif);

				item.setLevelNumber(1);
				item.setDateOt(systemMinDate);

				item.setTekst(row[2] + " (" + row[1] + ")");

				item.setDateReg(SearchUtils.asDate(row[3]));
				item.setDateLastMod(SearchUtils.asDate(row[4]));

				codePrev = item.getCode();

				Object registraturaId = getSd().getItemSpecific(CODE_CLASSIF_ADMIN_STR, item.getCode(), lang, date, ADM_STRUCT_INDEX_REGISTRATURA);
				Integer delovoditel = row[5] != null ? CODE_ZNACHENIE_DA : CODE_ZNACHENIE_NE;
				Integer dublMail = row[6] != null ? CODE_ZNACHENIE_DA : CODE_ZNACHENIE_NE;

				item.setSpecifics(new Object[] { registraturaId, delovoditel, dublMail });

				classif.add(item);
			}
		} catch (Exception e) {
			throw new DbErrorException(e);
		}

		return classif;
	}

	/**
	 * Дава само един елемент като метода {@link #buildClassReferents(Integer, Integer)}
	 *
	 * @param refId
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	protected SystemClassif findItemClassReferents(Integer refId, Integer lang) throws DbErrorException {
		SystemClassif item = null;
		try {
			Query query = JPA.getUtil().getEntityManager().createNativeQuery(getReferentsBaseSelect() + " and r.REF_ID = :refId ") //
				.setParameter("refId", refId);

			@SuppressWarnings("unchecked")
			List<Object[]> rows = query.getResultList();

			if (rows.isEmpty()) {
				LOGGER.error("ADM_REFERENTS with REF_ID={}, not found!", refId);
				return item;
			}

			item = new SystemClassif();

			item.setCodeClassif(CODE_CLASSIF_REFERENTS);
			item.setCodePrev(0); // подредбата тука не се следи
			item.setCodeParent(0);
			item.setLevelNumber(1);

			int countryBG = Integer.parseInt(getSd().getSettingsValue("delo.countryBG"));

			setReferentItemData(rows.get(0), item, lang, new Date(), countryBG); // сетвам специфичните данни на елемента

		} catch (Exception e) {
			throw new DbErrorException(e);
		}
		return item;
	}

	/**
	 * @param classif
	 * @param element
	 * @param map
	 * @param added
	 * @param appendPostion
	 * @param oneDayEmpl
	 * @throws DbErrorException
	 */
	void createAdmStructTree(List<SystemClassif> classif, SystemClassif element, Map<Integer, List<Object[]>> map, Set<String> added, boolean appendPostion, List<SystemClassif> oneDayEmpl) throws DbErrorException {
		if (element.getDateOt().getTime() == element.getDateDo().getTime()) {
			oneDayEmpl.add(element); // тези накрая ще се добавят, за да не бъркат схемата. ще бъдат само за разкодиране.
			return;
		}

		String key = element.getCode() + "_" + element.getDateOt().getTime();
		if (!added.add(key)) {
			return;
		}

		classif.add(element);

		List<Object[]> rows = map.get(element.getId());
		if (rows == null) {
			return;
		}

		for (Object[] row : rows) {
			SystemClassif item = new SystemClassif();

			item.setCodeClassif(element.getCodeClassif());
			item.setLevelNumber(element.getLevelNumber() + 1);

			// ако подчинените са служители ще се изпозлва регистратурата, която идва от родителя
			setAdmStructItemData(row, item, (Integer) element.getSpecifics()[ADM_STRUCT_INDEX_REGISTRATURA], appendPostion);

			createAdmStructTree(classif, item, map, added, appendPostion, oneDayEmpl);
		}
	}

	/**
	 * @param classif
	 * @param parent
	 * @param sql
	 * @param guid
	 * @param parents
	 */
	void createEgovOrgsTree(List<SystemClassif> classif, SystemClassif parent, String sql, String guid, List<String> parents) {
		classif.add(parent);

		if (guid == null || !parents.contains(guid)) {
			return;
		}

		@SuppressWarnings("unchecked") // за всички подчинени на това звено
		List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(sql) //
			.setParameter(1, guid) //
			.getResultList();

		int codePrev = 0;
		for (Object[] row : rows) {
			SystemClassif item = new SystemClassif();

			String eik = trimToNULL((String) row[1]);

			item.setId(asInteger(row[0]));
			item.setCode(item.getId());
			item.setCodeParent(parent.getCode());
			item.setCodePrev(codePrev);
			item.setCodeClassif(parent.getCodeClassif());

			item.setLevelNumber(parent.getLevelNumber() + 1);
			item.setDateOt(parent.getDateOt());
			item.setTekst(row[2] + " (" + eik + ")");

			codePrev = item.getCode();

			item.setDateReg((Date) row[5]);

			item.setSpecifics(new Object[] { eik, row[3] });

			createEgovOrgsTree(classif, item, sql, (String) row[3], parents);
		}
	}

	/**
	 * По ИД на група за регистратура намира кодовете на участниците, които са свързани с нея. Дава ги като string с разделител
	 * запетая
	 *
	 * @param groupId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	String findMemberCodes(int groupId) {
		String sql = "select distinct CODE_REF from REGISTRATURA_REFERENTS where GROUP_ID = :groupId";
		List<Object> members = JPA.getUtil().getEntityManager().createNativeQuery(sql).setParameter("groupId", groupId).getResultList();

		StringBuilder sb = new StringBuilder();
		for (Object memberId : members) {
			sb.append(asInteger(memberId));
			sb.append(",");
		}
		return sb.toString();
	}

	/**
	 * По ИД на регистратура намира регистрирте, които са свързани с нея. Дава ги като string с разделител запетая
	 *
	 * @param registaturaId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	String findRegisterIds(int registaturaId) {
		String sql = "select r.REGISTER_ID, r.COMMON, r.SORT_NOMER, r.VALID from REGISTRI r" //
			+ " where r.REGISTRATURA_ID = :registaturaId" //
			+ " union" //
			+ " select r.REGISTER_ID, r.COMMON, r.SORT_NOMER, r.VALID from REGISTRATURA_REGISTER rr" //
			+ " inner join REGISTRI r on r.REGISTER_ID = rr.REGISTER_ID" //
			+ " where rr.REGISTRATURA_ID = :registaturaId" //
			+ " order by 2, 3";

		Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql).setParameter("registaturaId", registaturaId);
		List<Object[]> registers = query.getResultList();

		StringBuilder sb = new StringBuilder();
		for (Object[] registerId : registers) {
			sb.append(asInteger(registerId[0]));
			sb.append(",");
		}
		return sb.toString();
	}

	private String getReferentsBaseSelect() {
		return " select r.REF_ID, r.CODE, r.CODE_PARENT, r.REF_NAME, r.DATE_OT, r.DATE_DO" //
			+ ", r.REF_TYPE, r.NFL_EIK, r.FZL_EGN, r.FZL_LNC" //
			+ ", r.CONTACT_EMAIL, a.ADDR_COUNTRY, a.ADDR_TEXT, a.EKATTE" //
			+ ", r.DATE_REG, r.DATE_LAST_MOD, r.kat_nar, r.vid_nar " //
			+ " from ADM_REFERENTS r left outer join ADM_REF_ADDRS a on a.CODE_REF = r.CODE and a.ADDR_TYPE = 1 " //
			+ " where r.CODE_CLASSIF is null ";
	}

	private SystemClassif setAdmStructItemData(Object[] row, SystemClassif item, Integer registratura, boolean appendPostion) throws DbErrorException {
		// r.REF_ID(0), r.CODE(1), r.CODE_PARENT(2), r.REF_NAME(3), r.DATE_OT(4), r.DATE_DO(5), r.REF_TYPE(6),
		// r.REF_REGISTRATURA(7), r.CONTACT_EMAIL(8), r.EMPL_POSITION(9), p.REF_NAME ZVENO_NAME(10), r.DATE_REG(11),
		// r.DATE_LAST_MOD(12)

		item.setCode(asInteger(row[1]));
		item.setCodePrev(asInteger(row[13]));

		item.setId(asInteger(row[0]));
		item.setCodeParent(asInteger(row[2]));
		item.setTekst((String) row[3]);
		item.setDateOt((Date) row[4]);
		item.setDateDo((Date) row[5]);

		Integer refType = asInteger(row[6]);
		String email = trimToNULL((String) row[8]);

		Integer position = null;
		item.setDopInfo(trimToNULL((String) row[10])); // горестоящото звено

		if (Objects.equals(refType, OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO)) {
			registratura = asInteger(row[7]); // за звеното си се взима каквато е на ред, а за служителя каквато дойде от горе
		} else {

			if (row[9] != null) {
				position = ((Number) row[9]).intValue();

				String positionName = getSd().decodeItem(CODE_CLASSIF_POSITION, position, CODE_DEFAULT_LANG, item.getDateOt());

				if (appendPostion) { // трябва да се сложи след името в скоби

					item.setTekst(item.getTekst() + " (" + positionName + ")");

				} else { // трябва да се сложи в доп инфо преди звеното
					item.setDopInfo(positionName + " " + item.getDopInfo());
				}
			}
		}

		item.setDateReg((Date) row[11]);
		item.setDateLastMod((Date) row[12]);

		item.setSpecifics(new Object[] { refType, registratura, email, position });

		item.setCodeExt(registratura+"");

		return item;
	}

	private SystemClassif setReferentItemData(Object[] row, SystemClassif item, Integer lang, Date date, int countryBg) throws DbErrorException {
		// r.REF_ID(0), r.CODE(1), r.CODE_PARENT(2), r.REF_NAME(3), r.DATE_OT(4), r.DATE_DO(5), r.REF_TYPE(6),
		// r.NFL_EIK(7), r.FZL_EGN(8), r.FZL_LNC(9), r.CONTACT_EMAIL(10), a.ADDR_COUNTRY(11),
		// a.ADDR_TEXT(12), a.EKATTE(13), r.DATE_REG(14), r.DATE_LAST_MOD(15)

		item.setId(asInteger(row[0]));
		item.setCode(asInteger(row[1]));
		item.setTekst((String) row[3]);
		item.setDateOt((Date) row[4]);
		item.setDateDo((Date) row[5]);

		Integer refType = asInteger(row[6]);

		boolean eik = false;
		boolean egn = false;
		String number = trimToNULL((String) row[7]); // ЕИК
		if (number == null) {
			number = trimToNULL((String) row[8]); // ЕГН
			if (number == null) {
				number = trimToNULL((String) row[9]); // ЛНЧ
			} else {
				egn = true;
			}
		} else {
			eik = true;
		}

		if (number != null && eik) { // само за ЕИК-то след името
			item.setTekst(item.getTekst() + " (" + number + ")");
		}

		String email = trimToNULL((String) row[10]);

		item.setDateReg((Date) row[14]);
		item.setDateLastMod((Date) row[15]);

		item.setCodeExt(number);

		Integer katNar = asInteger(row[16]);
		Integer vidNar = asInteger(row[17]);
		
		item.setSpecifics(new Object[] { refType, email, number, katNar, vidNar });

		// данните за адреса, които ще се сложат в допълнителната информация
		Integer country = asInteger(row[11]);
		String address = trimToNULL((String) row[12]);
		Integer ekatte = asInteger(row[13]);

		StringBuilder dopInfo = new StringBuilder(); // тука слагам адреса събран с екатте и държава ако не е БГ

		if (number != null && !eik) { // ЕИК/ЕГН
			if (egn) {
				dopInfo.append("ЕГН ");
			} else {
				dopInfo.append("ЛНЧ ");
			}
			dopInfo.append(number);
		}
		
		if (ekatte != null) { // ако има ЕКАТТЕ значи е БГ и не се занимавам с държавите
			String location = getSd().decodeItem(CODE_CLASSIF_EKATTE, ekatte, lang, date);
			if (location != null) {
				if (dopInfo.length() > 0) {
					dopInfo.append(", "); // за да се раздели от ЕИК/ЕГН
				}
				dopInfo.append(location);
			}

		} else if (country != null && !country.equals(countryBg)) { // само ако има и то да е чужда
																	// държава
			String countryText = getSd().decodeItem(CODE_CLASSIF_COUNTRIES, country, lang, date);
			if (countryText != null) {
				if (dopInfo.length() > 0) {
					dopInfo.append(", "); // за да се раздели от ЕИК/ЕГН
				}
				dopInfo.append(countryText);
			}
		}
		if (address != null) {
			if (dopInfo.length() > 0) {
				dopInfo.append(", "); // за да се раздели от реални адрес
			}
			dopInfo.append(address);
		}
		if (dopInfo.length() > 0) { // все пак може и да няма нищо изчислено като адрес
			item.setDopInfo(dopInfo.toString());
		}

		return item;
	}
}

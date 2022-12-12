package com.ib.omb.db.dao;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ACTIVE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM;
import static com.ib.system.db.DialectConstructor.nvl;
import static com.ib.system.db.DialectConstructor.trim;
import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Query;

import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloAccess;
import com.ib.omb.db.dto.DocShema;
import com.ib.omb.system.OmbConstants;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link DocShema}
 *
 * @author belev
 */
public class DocShemaDAO extends AbstractDAO<DocShema> {

	/** @param user */
	public DocShemaDAO(ActiveUser user) {
		super(DocShema.class, user);
	}

	/**
	 * Намира схемите за съхранение на документи, като изтегля само данните от вида:<br>
	 * [0]-SHEMA_ID<br>
	 * [1]-PREFIX<br>
	 * [2]-SHEMA_NAME<br>
	 * [3]-COMPLETE_METHOD<br>
	 * [4]-PERIOD_TYPE<br>
	 * [5]-YEARS<br>
	 * [6]-FROM_YEAR<br>
	 * [7]-TO_YEAR<br>
	 *
	 * @param registraturaId
	 * @param previousValid  <code>true</code>-Валидни индекси на НД, регистрирани за предходната година, <code>false</code>-Други
	 *                       валидни индекси.
	 * @return
	 */
	public SelectMetadata createSelectMetadataByValid(Integer registraturaId, boolean previousValid) {
		int yyyy = LocalDate.now().getYear(); // текущата година
		int prev = yyyy - 1; // и предходната

		Map<String, Object> params = new HashMap<>();

		String select = " select s.SHEMA_ID a0, s.PREFIX a1, s.SHEMA_NAME a2, s.COMPLETE_METHOD a3, s.PERIOD_TYPE a4, s.YEARS a5, s.FROM_YEAR a6, s.TO_YEAR a7 ";
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		from.append(" from DOC_SHEMA s ");

		if (previousValid) {
			from.append(" inner join DELO prev on prev.RN_DELO = s.PREFIX and prev.REGISTRATURA_ID = :registratura and prev.DELO_TYPE = :deloType ");
			from.append(" 					   and prev.DELO_YEAR >= :prev and prev.DELO_YEAR <= :prev ");

			params.put("prev", prev);
		}

		from.append(" left outer join DELO curr on curr.RN_DELO = s.PREFIX and curr.REGISTRATURA_ID = :registratura and curr.DELO_TYPE = :deloType ");
		from.append(" 						    and curr.DELO_YEAR >= :yearFrom and curr.DELO_YEAR <= :yearTo ");

		where.append(" where s.FROM_YEAR <= :yyyy and (s.TO_YEAR is null or s.TO_YEAR >= :yyyy) ");
		where.append(" and curr.DELO_ID is null ");

		params.put("registratura", registraturaId);
		params.put("deloType", CODE_ZNACHENIE_DELO_TYPE_NOM);
		params.put("yyyy", yyyy);

		if (previousValid) { // да няма за текущата
			params.put("yearFrom", yyyy);
			params.put("yearTo", yyyy);

		} else { // да няма за текущата и предходната
			params.put("yearFrom", prev);
			params.put("yearTo", yyyy);
		}

		SelectMetadata sm = new SelectMetadata();

		sm.setSql(select + from + where);
		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * Намира схемите за съхранение на документи, като изтегля само данните от вида:<br>
	 * [0]-SHEMA_ID<br>
	 * [1]-PREFIX<br>
	 * [2]-SHEMA_NAME<br>
	 * [3]-FROM_YEAR<br>
	 * [4]-TO_YEAR<br>
	 * [5]-COMPLETE_METHOD<br>
	 * [6]-PERIOD_TYPE<br>
	 * [7]-YEARS<br>
	 *
	 * @param year
	 * @param prefix 
	 * @param shemaName 
	 * @return
	 */
	public SelectMetadata createSelectMetadataByYear(Integer year, String prefix, String shemaName) {
		Map<String, Object> params = new HashMap<>();

		String select = " select s.SHEMA_ID a0, s.PREFIX a1, s.SHEMA_NAME a2, s.FROM_YEAR a3, s.TO_YEAR a4, s.COMPLETE_METHOD a5, s.PERIOD_TYPE a6, s.YEARS a7 ";
		String from = " from DOC_SHEMA s ";

		StringBuilder where = new StringBuilder();
		if (year != null) {
			where.append(" where s.FROM_YEAR <= :yearArg and (s.TO_YEAR is null or s.TO_YEAR >= :yearArg) ");
			params.put("yearArg", year);
		} else {
			where.append(" where 1=1");
		}
		
		String t = SearchUtils.trimToNULL_Upper(prefix);
		if (t!=null) {
			where.append(" and upper(s.PREFIX) like :prefix ");
			params.put("prefix", "%" + t + "%");
		}
		t = SearchUtils.trimToNULL_Upper(shemaName);
		if (t!=null) {
			where.append(" and upper(s.SHEMA_NAME) like :shemaName ");
			params.put("shemaName", "%" + t + "%");
		}

		SelectMetadata sm = new SelectMetadata();

		sm.setSql(select + from + where);
		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * Прави проверки дали изтриването е позволено и ако не е дава грешка ObjectInUseException, която трябва да се покаже на
	 * екрана
	 *
	 * @param entity
	 * @param systemData
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	@SuppressWarnings("unchecked")
	public void delete(DocShema entity, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		//  Потребителят може да изтрие индекс само ако в базата данни няма преписки от тип „номенклатурно дело“, чийто номер
		// съвпада с този индекс и годината на регистрация на делото е в периода на валидност на индекса. Ако има, извежда
		// съобщение: „Не можете да изтриете индекса, защото в системата има регистрирано номенклатурно дело с този индекс.“.

		int count = countDela(entity.getPrefix(), entity.getFromYear(), entity.getToYear());
		if (count > 0) {
			throw new ObjectInUseException("Не можете да изтриете индекса, защото в системата има регистрирано номенклатурно дело с този индекс.");
		}

		List<Object> settList = null;
		try {
			settList = createNativeQuery("select DOC_VID from DOC_VID_SETTINGS where SHEMA_ID = ?1") //
				.setParameter(1, entity.getId()).getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани обекти към индекса!", e);
		}

		if (settList != null && !settList.isEmpty()) {
			String error = "Изтриването не е позволено, защото за индекса е регистрирана характеристика за вид документ: "
				+ systemData.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, ((Number) settList.get(0)).intValue(), getUserLang(), null);
			throw new ObjectInUseException(error);
		}

		super.delete(entity);
	}

	/** */
	@Override
	public DocShema findById(Object id) throws DbErrorException {
		DocShema entity = super.findById(id);

		if (entity != null) { // за да мога при запис да си правя анализите
			entity.setDbPrefix(entity.getPrefix());
			entity.setDbFromYear(entity.getFromYear());
			entity.setDbToYear(entity.getToYear());
		}

		return entity;
	}

	/**
	 * Регистрира дела за избраните индекси за текущата година
	 *
	 * @param registraturaId
	 * @param previousValid  <code>true</code>-Валидни индекси на НД, регистрирани за предходната година, <code>false</code>-Други
	 *                       валидни индекси.
	 * @param prefixList
	 * @param systemData
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public void registerNomDelaByIndex(Integer registraturaId, boolean previousValid, List<Object[]> prefixList, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		if (prefixList == null || prefixList.isEmpty()) {
			return;
		}

		int yyyy = LocalDate.now().getYear(); // текущата година
		Date deloDate = DateUtils.parse("01.01." + yyyy);

		DeloDAO deloDao = new DeloDAO(getUser());
		for (Object[] row : prefixList) {
			String prefix = (String) row[1];

			Delo delo = new Delo();

			delo.setRegistraturaId(registraturaId);
			delo.setRnDelo(prefix);
			delo.setDeloDate(deloDate);
			delo.setDeloType(CODE_ZNACHENIE_DELO_TYPE_NOM);

			if (previousValid) {
				Delo prev = findNomDelo(registraturaId, prefix, yyyy - 1);
				if (prev == null) {
					throw new DbErrorException("Няма открито номенклатурно дело " + prefix + " за " + (yyyy - 1) + " година!");
				}
				delo.setDeloName(prev.getDeloName());
				delo.setDeloInfo(prev.getDeloInfo());
				delo.setCodeRefLead(prev.getCodeRefLead());

				deloDao.loadDeloAccess(prev); // зареждам изричния достъп на старото дело

				List<DeloAccess> newAccessList = new ArrayList<>();

				for (DeloAccess da : prev.getDeloAccess()) {
					if (Objects.equals(da.getExclude(), SysConstants.CODE_ZNACHENIE_DA)) {
						continue; // които имат забранен няма да ги прехвърлям
					}

					DeloAccess newAccess = new DeloAccess();

					newAccess.setCodeRef(da.getCodeRef());
					newAccess.setFlag(SysConstants.CODE_DEIN_ZAPIS);

					newAccessList.add(newAccess);
				}
				delo.setDeloAccess(newAccessList); // изричния + стрия регистрирал

			} else {
				delo.setDeloName((String) row[2]);
			}

			delo.setFreeAccess(SysConstants.CODE_ZNACHENIE_NE);
			delo.setStatus(CODE_ZNACHENIE_DELO_STATUS_ACTIVE);
			delo.setStatusDate(deloDate);

			delo.setWithSection(SysConstants.CODE_ZNACHENIE_NE);
			delo.setWithTom(SysConstants.CODE_ZNACHENIE_DA);

			deloDao.save(delo, false, systemData, null);
		}
	}

	/**
	 * Допълнително се правят проверки дали може записа да се изпълни
	 *
	 * @param entity
	 * @param sd
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public DocShema save(DocShema entity, BaseSystemData sd) throws DbErrorException, ObjectInUseException {
		if (entity.getPrefix() != null) { // важно е да няма интервали в началото и края
			entity.setPrefix(entity.getPrefix().trim());
		}

		//  При въвеждане на нов или промяна на съществуващ индекс, не се допуска да се получат два индекса с една и съща
		// стойност в поле „Индекс“, чиито периоди на валидност се пресичат

		//  При промяна на стойността в поле „Индекс“ или в поле „Валиден до година“, системата проверява, дали в базата
		// данни има преписка от тип „номенклатурно дело“, чийто номер съвпада с този индекс и годината на регистрация на
		// делото е в периода на валидност на индекса. Ако има, извежда съобщение: „Не можете да промените индекса, защото в
		// системата има регистрирано номенклатурно дело с този индекс.“.

		if (!Objects.equals(entity.getPrefix(), entity.getDbPrefix())) { // сменен е индекса
			int count = 0;
			try { // проверката за пресичане на интервали
				String dialect = JPA.getUtil().getDbVendorName();

				String nvl = nvl(dialect, "TO_YEAR", 2999);

				StringBuilder sql = new StringBuilder();

				sql.append(" select count (SHEMA_ID) CNT from DOC_SHEMA ");
				sql.append(" where upper ( " + trim(dialect, "PREFIX") + " ) = :prefix ");
				sql.append(" and (      (FROM_YEAR <= :fromYear and " + nvl + " >= :fromYear) ");
				sql.append(" 		 or (FROM_YEAR <= :toYear and " + nvl + " >= :toYear) ");
				sql.append("         or (FROM_YEAR >= :fromYear and " + nvl + " <= :toYear) ) ");
				if (entity.getId() != null) {
					sql.append(" and SHEMA_ID != :shemaId ");
				}

				Query query = createNativeQuery(sql.toString()) //
					.setParameter("prefix", trimToNULL_Upper(entity.getPrefix())) //
					.setParameter("fromYear", entity.getFromYear()) //
					.setParameter("toYear", entity.getToYear() == null ? Integer.valueOf(2999) : entity.getToYear());

				if (entity.getId() != null) {
					query.setParameter("shemaId", entity.getId());
				}

				count = ((Number) query.getResultList().get(0)).intValue();

			} catch (Exception e) {
				throw new DbErrorException("Грешка при проверка за пресичане на интервали!", e);
			}
			if (count > 0) {
				throw new ObjectInUseException("Не се допуска да има два индекса с една и съща стойност в поле \"Индекс\"" //
					+ ", чиито периоди на валидност се пресичат.");
			}

			if (entity.getId() != null) {
				count = countDela(entity.getDbPrefix(), entity.getDbFromYear(), entity.getDbToYear());
				if (count > 0) {
					throw new ObjectInUseException("Не можете да промените индекса, защото в системата има" //
						+ " регистрирано номенклатурно дело с този индекс.");
				}
			}
		}

		if (entity.getFromYear() != null && entity.getDbFromYear() != null && //
			entity.getFromYear().intValue() > entity.getDbFromYear().intValue()) { // само ако се мести напред

			int count = countDela(entity.getPrefix(), entity.getDbFromYear(), entity.getFromYear());
			if (count > 0) {
				throw new ObjectInUseException("Не можете да промените индекса, защото в системата има" //
					+ " регистрирано номенклатурно дело с този индекс.");
			}
		}

		if (entity.getDbToYear() == null && entity.getToYear() != null // слага се ограничение
			|| entity.getToYear() != null && entity.getDbToYear() != null // има години
				&& entity.getToYear().intValue() < entity.getDbToYear().intValue()) { // и се мести назад

			int toYear = entity.getToYear() == null ? 2999 : entity.getToYear();

			int count = countDela(entity.getPrefix(), toYear, entity.getDbToYear());
			if (count > 0) {
				throw new ObjectInUseException("Не можете да промените индекса, защото в системата има" //
					+ " регистрирано номенклатурно дело с този индекс.");
			}
		}

		DocShema saved = super.save(entity);

		saved.setDbPrefix(saved.getPrefix());
		saved.setDbFromYear(saved.getFromYear());
		saved.setDbToYear(saved.getToYear());

		return saved;
	}
	
	/**
	 * Намира номенклатурно дело
	 *
	 * @param registraturaId
	 * @param rnDelo
	 * @param yyyy
	 * @return
	 * @throws DbErrorException
	 */
	Delo findNomDelo(Integer registraturaId, String rnDelo, Integer yyyy) throws DbErrorException {
		rnDelo = SearchUtils.trimToNULL_Upper(rnDelo);
		if (rnDelo == null) {
			return null;
		}
		try {
			String sql = "select d from Delo d where d.registraturaId = ?1 and upper(d.rnDelo) = ?2" //
				+ " and d.deloYear = ?3 and d.deloType = ?4";

			Query query = createQuery(sql).setParameter(1, registraturaId).setParameter(2, rnDelo) //
				.setParameter(3, yyyy).setParameter(4, CODE_ZNACHENIE_DELO_TYPE_NOM);

			@SuppressWarnings("unchecked")
			List<Object> list = query.getResultList();
			if (list.isEmpty()) {
				return null;
			}
			return (Delo) list.get(0);

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на номенклатурно дело в регистратура!", e);
		}
	}

	/**
	 * Проверява дали за подадените параметри има регистрирани номенклатурни дела. Ако има дава броят им.
	 *
	 * @param entity
	 * @param prefix
	 * @param fromYear
	 * @param toYear
	 * @return
	 * @throws DbErrorException
	 */
	private int countDela(String prefix, Integer fromYear, Integer toYear) throws DbErrorException {
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select count (*) CNT from DELO d where d.DELO_TYPE = :deloType and ");
			sql.append(" d.RN_DELO = :rnPrefix and d.DELO_YEAR >= :fromYear ");

			if (toYear != null) {
				sql.append(" and d.DELO_YEAR <= :toYear ");
			}

			Query query = createNativeQuery(sql.toString()) //
				.setParameter("deloType", CODE_ZNACHENIE_DELO_TYPE_NOM) //
				.setParameter("rnPrefix", prefix) //
				.setParameter("fromYear", fromYear);

			if (toYear != null) {
				query.setParameter("toYear", toYear);
			}

			Object count = query.getResultList().get(0);

			return ((Number) count).intValue();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на номенклатурни дела по индекс и година!", e);
		}
	}
}
package com.ib.omb.search;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.ib.indexui.search.UserSearch;
import com.ib.indexui.system.Constants;
import com.ib.omb.system.OmbConstants;
import com.ib.system.BaseSystemData;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.utils.DateUtils;

/**
 * Предоставя разширение на стандартното търсене на потребител
 *
 * @author belev
 */
public class ExtendedUserSearch extends UserSearch {

	/**  */
	private static final long serialVersionUID = -6734028006280514175L;

	private Integer registratura;

	private Integer businessRole;

	private List<Integer> defPravaList;

	private List<Integer>				codeRefList;	// Служител
	private Map<Integer, List<Integer>>	given;			// Предоставени права - вид право и стойности, между които се налага
														// условие "и"
	private Map<Integer, List<Integer>>	notGiven;		// Непредоставени права - вид право и стойности, между които се налага
														// условие "и"

	/** */
	public ExtendedUserSearch() {
		super();
	}

	/**
	 * Да се използва когато се иска проврка за избран потребител и/или дадени или недадени права</br>
	 * [0]-USER_ID</br>
	 * [1]-CODE_CLASSIF</br>
	 * [2]-CODE_ROLE</br>
	 * [3]-лично,групово,смесено</br>
	 * [4]-групите</br>
	 * [5]-NAMES</br>
	 * [6]-ZVENO_NAME</br>
	 *
	 * @param systemData
	 * @throws InvalidParameterException
	 * @throws DbErrorException
	 */
	public void buildQueryGrantedRights(BaseSystemData systemData) throws InvalidParameterException, DbErrorException {
		boolean checkGiven = this.given != null && !this.given.isEmpty();
		boolean checkNotGiven = this.notGiven != null && !this.notGiven.isEmpty();

		Date today = DateUtils.startDate(new Date());
		Set<Integer> allClassif = new HashSet<>(); // ще се вземе от настройката
		Set<Integer> specClassif = new HashSet<>(); // ще се вземе от настройката

		String[] tmp;

		String allSett = systemData.getSettingsValue("system.classificationsForAccessControl");
		if (allSett == null) {
			throw new InvalidParameterException("system.classificationsForAccessControl=NULL");
		}
		tmp = allSett.split(",");
		for (String s : tmp) {
			allClassif.add(Integer.valueOf(s));
		}
		// долните ги няма в настройката, защото се поддържат от друго място и за това ръчно се добавят
		allClassif.add(Constants.CODE_CLASSIF_BUSINESS_ROLE);
		allClassif.add(OmbConstants.CODE_CLASSIF_REGISTRI);
		allClassif.add(OmbConstants.CODE_CLASSIF_REGISTRATURI);
		allClassif.add(OmbConstants.CODE_CLASSIF_USER_SETTINGS);
		allClassif.add(OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE);

		String specSett = systemData.getSettingsValue("system.classificationsNotFilteredIfNotGranted");
		if (specSett != null) { // не винги има такива
			tmp = specSett.split(",");
			for (String s : tmp) {
				specClassif.add(Integer.valueOf(s));
			}
		}

		Map<String, Object> params = new HashMap<>();
		StringBuilder sql = new StringBuilder();

		if (checkGiven) { // има дадени права, като няма значение има ли избран усер или не
			int iter = 0;
			for (Entry<Integer, List<Integer>> entry : this.given.entrySet()) {
				if (iter > 0) {
					sql.append(" union all ");
				}
				boolean spec = specClassif.contains(entry.getKey());

				String subsql = createRightsSql(today, entry.getKey(), entry.getValue(), spec, false, params);
				sql.append(subsql);

				iter++;
			}
		} else if (this.codeRefList != null && !this.codeRefList.isEmpty() && !checkNotGiven) { // само усер без нищо друго
																								// избрано
			int iter = 0;
			for (Integer classif : allClassif) {
				if (iter > 0) {
					sql.append(" union all ");
				}
				boolean spec = specClassif.contains(classif);

				String subsql = createRightsSql(today, classif, null, spec, false, params);
				sql.append(subsql);

				iter++;
			}
		} else {
			throw new InvalidParameterException("Невъзможност да се състави резултат!");
		}

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append(" select t.a0, t.a1, t.a2 ");
		select.append(" , case when t.a4 is null then 'лично' when t.a3 is not null and t.a3 > 0 then 'групово' else 'смесено' end a3 ");
		select.append(" , t.a4, z1.REF_NAME a5, z2.REF_NAME a6 ");
		from.append(" from ( " + sql + " ) t ");

		if (checkNotGiven) { // трябва да се изключат избраните права
			from.append(" left outer join ( ");
			int iter = 0;
			for (Entry<Integer, List<Integer>> entry : this.notGiven.entrySet()) {
				if (iter > 0) {
					from.append(" union all ");
				}
				boolean spec = specClassif.contains(entry.getKey());

				String subsql = createRightsSql(today, entry.getKey(), entry.getValue(), spec, true, params);
				from.append(subsql);

				iter++;
			}
			from.append(" ) v on v.USER_ID = t.a0 ");
			where.append(" where v.USER_ID is null ");
		}

		// за да се вземе името на звеното
		from.append(" left outer join ADM_REFERENTS z1 on z1.CODE = t.a0 and z1.DATE_OT <= :refDate and z1.DATE_DO > :refDate ");
		from.append(" left outer join ADM_REFERENTS z2 on z2.CODE = z1.CODE_PARENT and z2.DATE_OT <= :refDate and z2.DATE_DO > :refDate ");
		params.put("refDate", today);

		setSql(select.toString() + from + where);
		setSqlCount(" select count (*) cnt " + from + where);
		setSqlParameters(params);
	}

	/**
	 * Да се използва когато се иска проверка само за недадени права със или без потребител</br>
	 * [0]-USER_ID</br>
	 * [1]-USERNAME</br>
	 * [2]-NAMES</br>
	 * [3]-ZVENO_NAME</br>
	 *
	 * @param systemData
	 * @throws InvalidParameterException
	 * @throws DbErrorException
	 */
	public void buildQueryGrantedRights2(BaseSystemData systemData) throws InvalidParameterException, DbErrorException {
		boolean checkGiven = this.given != null && !this.given.isEmpty();
		boolean checkNotGiven = this.notGiven != null && !this.notGiven.isEmpty();

		if (checkGiven || !checkNotGiven) { // от правата трябва да са само недадените
			throw new InvalidParameterException("Невъзможност да се състави резултат!");
		}
		Date today = DateUtils.startDate(new Date());
		Set<Integer> specClassif = new HashSet<>(); // ще се вземе от настройката

		String specSett = systemData.getSettingsValue("system.classificationsNotFilteredIfNotGranted");
		if (specSett != null) { // не винги има такива
			String[] tmp = specSett.split(",");
			for (String s : tmp) {
				specClassif.add(Integer.valueOf(s));
			}
		}

		Map<String, Object> params = new HashMap<>();
		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append(" select t.USER_ID a0, t.USERNAME a1, t.NAMES a2, z2.REF_NAME a3 ");
		from.append(" from ADM_USERS t left outer join ( ");
		int iter = 0;
		for (Entry<Integer, List<Integer>> entry : this.notGiven.entrySet()) {
			if (iter > 0) {
				from.append(" union all ");
			}
			boolean spec = specClassif.contains(entry.getKey());

			String subsql = createRightsSql(today, entry.getKey(), entry.getValue(), spec, true, params);
			from.append(subsql);

			iter++;
		}
		from.append(" ) v on v.USER_ID = t.USER_ID ");

		// за да се вземе името на звеното
		from.append(" left outer join ADM_REFERENTS z1 on z1.CODE = t.USER_ID and z1.DATE_OT <= :refDate and z1.DATE_DO > :refDate ");
		from.append(" left outer join ADM_REFERENTS z2 on z2.CODE = z1.CODE_PARENT and z2.DATE_OT <= :refDate and z2.DATE_DO > :refDate ");
		params.put("refDate", today);

		where.append(" where v.USER_ID is null ");

		if (this.codeRefList != null && !this.codeRefList.isEmpty()) {
			where.append(" and t.USER_ID in (:codeRefList) "); // параметъра ще се сетне в createRightsSql
		}

		setSql(select.toString() + from + where);
		setSqlCount(" select count (*) cnt " + from + where);
		setSqlParameters(params);
	}

	/** @return the businessRole */
	public Integer getBusinessRole() {
		return this.businessRole;
	}

	/** @return the codeRefList */
	public List<Integer> getCodeRefList() {
		return this.codeRefList;
	}

	/** @return the defPravaList */
	public List<Integer> getDefPravaList() {
		return this.defPravaList;
	}

	/** @return the given */
	public Map<Integer, List<Integer>> getGiven() {
		return this.given;
	}

	/** @return the notGiven */
	public Map<Integer, List<Integer>> getNotGiven() {
		return this.notGiven;
	}

	/** @return the registratura */
	public Integer getRegistratura() {
		return this.registratura;
	}

	/** @param businessRole the businessRole to set */
	public void setBusinessRole(Integer businessRole) {
		this.businessRole = businessRole;
	}

	/** @param codeRefList the codeRefList to set */
	public void setCodeRefList(List<Integer> codeRefList) {
		this.codeRefList = codeRefList;
	}

	/** @param defPravaList the defPravaList to set */
	public void setDefPravaList(List<Integer> defPravaList) {
		this.defPravaList = defPravaList;
	}

	/** @param given the given to set */
	public void setGiven(Map<Integer, List<Integer>> given) {
		this.given = given;
	}

	/** @param notGiven the notGiven to set */
	public void setNotGiven(Map<Integer, List<Integer>> notGiven) {
		this.notGiven = notGiven;
	}

	/** @param registratura the registratura to set */
	public void setRegistratura(Integer registratura) {
		this.registratura = registratura;
	}

	/** */
	@Override
	protected void extendQueryUserList(StringBuilder select, StringBuilder from, StringBuilder where, Map<String, Object> params) {
		select.append(", zveno.REF_REGISTRATURA ");

		from.append(" left outer join ADM_REFERENTS ref on ref.CODE = u.USER_ID and ref.DATE_OT <= :dateArg and ref.DATE_DO > :dateArg ");
		from.append(" left outer join ADM_REFERENTS zveno on zveno.CODE = ref.CODE_PARENT and zveno.DATE_OT <= :dateArg and zveno.DATE_DO > :dateArg ");
		params.put("dateArg", DateUtils.startDate(new Date()));

		if (this.registratura != null) {
			where.append(" and zveno.REF_REGISTRATURA = :registratura "); // това е така, защото регистратурата е в звеното а не в
																			// служителя (вече)
			params.put("registratura", this.registratura);
		}

		if (this.businessRole != null) {
			from.append(" inner join ADM_USER_ROLES rol on rol.USER_ID = u.USER_ID and rol.CODE_CLASSIF = :codeClassif ");
			params.put("codeClassif", Constants.CODE_CLASSIF_BUSINESS_ROLE);

			where.append(" and rol.CODE_ROLE = :businessRole ");
			params.put("businessRole", this.businessRole);
		}

		if (this.defPravaList != null && !this.defPravaList.isEmpty()) {
			where.append(" and EXISTS ( ");
			where.append(" select defu.ROLE_ID from ADM_USER_ROLES defu where defu.USER_ID = u.USER_ID and defu.CODE_CLASSIF = :classifDefp and defu.CODE_ROLE in (:defPravaList) ");
			where.append(" union all ");
			where.append(" select defg.ROLE_ID from ADM_USER_GROUP defug inner join ADM_GROUP_ROLES defg on defg.GROUP_ID = defug.GROUP_ID ");
			where.append(" where defug.USER_ID = u.USER_ID and defg.CODE_CLASSIF = :classifDefp and defg.CODE_ROLE in (:defPravaList) ");
			where.append(" ) ");

			params.put("classifDefp", OmbConstants.CODE_CLASSIF_DEF_PRAVA);
			params.put("defPravaList", this.defPravaList);
		}
	}

	String createRightsSql(Date date, Integer classif, List<Integer> codes, boolean special, boolean exlude, Map<String, Object> params) {
		String dialect = JPA.getUtil().getDbVendorName();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		String scCode;
		if (classif.equals(Constants.CODE_CLASSIF_INF_MODEL)) {
			scCode = "sc.AIS_ID";
		} else if (classif.equals(OmbConstants.CODE_CLASSIF_REGISTRI)) {
			scCode = "sc.REGISTER_ID";
		} else if (classif.equals(OmbConstants.CODE_CLASSIF_REGISTRATURI) || classif.equals(OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS)
			|| classif.equals(OmbConstants.CODE_CLASSIF_REGISTRATURI_REQDOC)) {
			scCode = "sc.REGISTRATURA_ID";
		} else {
			scCode = "sc.CODE";
		}

		if (exlude) {
			select.append(" select u.USER_ID ");

		} else {
			select.append(" select distinct u.USER_ID a0 ");
			select.append(" , case when p.CODE_CLASSIF is not null then p.CODE_CLASSIF else " + classif + " end a1 ");
			select.append(" , case when p.CODE_ROLE is not null then p.CODE_ROLE else " + scCode + " end a2 ");
			select.append(" , (select min(GROUP_ID) FROM V_PRAVA vp where vp.USER_ID = u.USER_ID and vp.CODE_CLASSIF = " + classif + " and vp.CODE_ROLE = " + scCode + ") a3 ");
			select.append(" , " + DialectConstructor.convertToDelimitedString(dialect //
				, "g.GROUP_NAME" //
				, "V_PRAVA vp inner join ADM_GROUPS g on g.GROUP_ID = vp.GROUP_ID where vp.USER_ID = u.USER_ID and vp.CODE_CLASSIF = " + classif + " and vp.CODE_ROLE = " + scCode //
				, "vp.GROUP_ID") + " a4 ");
		}

		from.append(" from ADM_USERS u ");

		if (classif.equals(Constants.CODE_CLASSIF_INF_MODEL)) {
			from.append(" inner join MODEL_AIS sc on 1=1 ");

		} else if (classif.equals(OmbConstants.CODE_CLASSIF_REGISTRI)) {
			from.append(" inner join REGISTRI sc on 1=1 ");

		} else if (classif.equals(OmbConstants.CODE_CLASSIF_REGISTRATURI) || classif.equals(OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS)
			|| classif.equals(OmbConstants.CODE_CLASSIF_REGISTRATURI_REQDOC)) {
			from.append(" inner join REGISTRATURI sc on 1=1 ");

		} else { // обикновена класифиакция
			from.append(" inner join SYSTEM_CLASSIF sc on sc.CODE_CLASSIF = " + classif + " and sc.DATE_OT <= :parDate and (sc.DATE_DO is null or sc.DATE_DO > :parDate) ");
			params.put("parDate", date);
		}

		if (codes != null && !codes.isEmpty()) {
			String scValue = "scCodes" + classif;
			if (exlude) {
				scValue += "e";
			}
			from.append(" and " + scCode + " in (:" + scValue + ") ");
			params.put(scValue, codes);
		}

		if (special) {
			from.append(" left outer join ");
		} else {
			from.append(" inner join ");
		}
		from.append(" V_PRAVA p on p.USER_ID = u.USER_ID and p.CODE_CLASSIF = " + classif + " and p.CODE_ROLE = " + scCode + " ");

		where.append(" where 1=1 ");

		if (this.codeRefList != null && !this.codeRefList.isEmpty()) {
			where.append(" and u.USER_ID in (:codeRefList) ");
			params.put("codeRefList", this.codeRefList);
		}

		where.append(" and ( ");
		if (special) {
			where.append(" 0=(select count (distinct vp.CODE_ROLE) from V_PRAVA vp where vp.USER_ID = u.USER_ID and vp.CODE_CLASSIF = " + classif + ") ");
			where.append(" or ");
		}

		if (codes != null && !codes.isEmpty()) {
			String roleValue = "roleCodes" + classif;
			if (exlude) {
				roleValue += "e";
			}
			where.append(" " + codes.size() // трябва да има достъп до всички изброени
				+ "=(select count (distinct vp.CODE_ROLE) from V_PRAVA vp where vp.USER_ID = u.USER_ID and vp.CODE_CLASSIF = " + classif + " and vp.CODE_ROLE in (:" + roleValue + ")) ");
			params.put(roleValue, codes);

		} else {
			where.append(" p.GROUP_ID is not null "); // или всички до които има достъп през view-то за правата
		}
		where.append(" ) "); // затваря and

		return select.toString() + from.toString() + where.toString();
	}
}
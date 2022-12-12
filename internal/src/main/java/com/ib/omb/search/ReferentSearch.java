package com.ib.omb.search;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO;
import static com.ib.system.SysConstants.CODE_CLASSIF_EKATTE;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;
import static com.ib.system.db.DialectConstructor.trim;
import static com.ib.system.utils.SearchUtils.trimToNULL;
import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.system.BaseSystemData;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;

/**
 * Различни вариянти за търсене на участници в процеса.
 *
 * @see #buildQuery()
 * @author belev
 */
public class ReferentSearch extends SelectMetadata {

	/**  */
	private static final long serialVersionUID = 2969204121616428263L;

	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(ReferentSearch.class);

	/** вид участник */
	private Integer	refType;
	/** име */
	private String	refName;
	/** <code>true</code> пълно съвпадение по име */
	private boolean	exactly;

	/** еик/егн в зависимост от вид участник */
	private String	eikEgn;
	/** лнч само за физически лица */
	private String	lnc;

	private Integer	country;
	private Integer	ekatte;			// местоположение - код от класификация
	/** има смисъл само ако елемента е област или община */
	private String	ekatteExtCode;	// местоположение – външен код

	/** датата към която се търси. ако не се въведе се работи с днешна дата */
	private Date date;

	
	private List<Integer> katNarList;
	private List<Integer> vidNarList;
	private List<Integer> tipOrganList;
	
	/**  */
	public ReferentSearch() {
		super();
	}

	/**
	 * На база входните параметри подготвя селект за получаване на резултат от вида: <br>
	 * [0]-CODE<br>
	 * [1]-REF_TYPE<br>
	 * [2] NFL_EIK/FZL_EGN<br>
	 * [3] FZL_LNC<br>
	 * [4]-REF_NAME<br>
	 * [5]-ADDR_COUNTRY<br>
	 * [6]-TVM<br>
	 * [7]-MIASTO_IME<br>
	 * [8]-OBSTINA_IME<br>
	 * [9]-OBLAST_IME<br>
	 * [10]-ADDR_TEXT<br>
	 * [11]-CONTACT_PHONE<br>
	 * [12]-CONTACT_EMAIL<br>
	 * [13]-kat_nar<br>
	 * [14]-vid_nar<br>
	 * [15]-tip_organ<br>
	 *
	 * @see #calcEkatte(BaseSystemData)
	 */
	@Override
	public void buildQuery() {
		String dialect = JPA.getUtil().getDbVendorName();

		StringBuilder select = new StringBuilder();
		select.append(" select r.CODE 			a0 ");
		select.append(" , r.REF_TYPE 			a1 ");
		select.append(", case when NFL_EIK is null then FZL_EGN else NFL_EIK end	a2 ");
		select.append(", r.FZL_LNC 				a3 ");
		select.append(", r.REF_NAME 			a4 ");
		select.append(", addr.ADDR_COUNTRY 		a5 ");
		select.append(", att.TVM 				a6 ");
		select.append(", att.IME 				a7 ");
		select.append(", att.OBSTINA_IME 		a8 ");
		select.append(", att.OBLAST_IME 		a9 ");
		select.append(", addr.ADDR_TEXT			a10 ");
		select.append(", r.CONTACT_PHONE		a11 ");
		select.append(", r.CONTACT_EMAIL		a12 ");
		select.append(", r.kat_nar a13, r.vid_nar a14, r.tip_organ a15 ");

		StringBuilder from = new StringBuilder(" from ADM_REFERENTS r ");
		from.append(" left outer join ADM_REF_ADDRS addr on addr.CODE_REF = r.CODE and addr.ADDR_TYPE = 1 "); // само един вид
																												// адрес има
		from.append(" left outer join EKATTE_ATT att on att.EKATTE = addr.EKATTE and att.DATE_OT <= :dateArg and att.DATE_DO > :dateArg ");

		StringBuilder where = new StringBuilder(" where r.DATE_OT <= :dateArg and r.DATE_DO > :dateArg ");
		Map<String, Object> params = new HashMap<>();

		Date dateArg = DateUtils.startDate(this.date == null ? new Date() : this.date);
		params.put("dateArg", dateArg);

		if (this.refType != null) {
			where.append(" and r.REF_TYPE = :refType ");
			params.put("refType", this.refType);
		}

		String t = trimToNULL_Upper(this.refName);
		if (t != null) {
			if (this.exactly) { // точно съвпадение
				params.put("refName", t);
				where.append(" and upper(" + trim(dialect, "r.REF_NAME") + ") = :refName ");
			} else {
				where.append(" and upper(r.REF_NAME) like :refName ");
				params.put("refName", "%" + t + "%");
			}
		}

		t = trimToNULL(this.eikEgn);
		if (t != null && this.refType != null) {

			if (this.refType.equals(CODE_ZNACHENIE_REF_TYPE_NFL) || this.refType.equals(CODE_ZNACHENIE_REF_TYPE_ZVENO)) {
				where.append(" and r.NFL_EIK = :eikEgn ");
				params.put("eikEgn", t);

			} else if (this.refType.equals(CODE_ZNACHENIE_REF_TYPE_FZL) || this.refType.equals(CODE_ZNACHENIE_REF_TYPE_EMPL)) {
				where.append(" and r.FZL_EGN = :eikEgn ");
				params.put("eikEgn", t);

			} else {
				LOGGER.warn("Непознат вид участник : {}", this.refType);
			}
		}
		t = trimToNULL(this.lnc);
		if (t != null) {
			where.append(" and r.FZL_LNC = :lnc ");
			params.put("lnc", t);
		}

		if (this.katNarList != null && !this.katNarList.isEmpty()) {
			where.append(" and r.kat_nar in (:katNarList) ");
			params.put("katNarList", this.katNarList);
		}
		if (this.vidNarList != null && !this.vidNarList.isEmpty()) {
			where.append(" and r.vid_nar in (:vidNarList) ");
			params.put("vidNarList", this.vidNarList);
		}
		if (this.tipOrganList != null && !this.tipOrganList.isEmpty()) {
			where.append(" and r.tip_organ in (:tipOrganList) ");
			params.put("tipOrganList", this.tipOrganList);
		}

		if (this.country != null) {
			where.append(" and addr.ADDR_COUNTRY = :country ");
			params.put("country", this.country);
		}
		if (this.ekatte != null) {
			if (this.ekatte.intValue() < 100000) { // търсене по населено място
				where.append(" and addr.EKATTE = :ekatte ");
				params.put("ekatte", this.ekatte);

			} else if (this.ekatteExtCode != null) { // търсене по област или община

				String col = null;
				if (this.ekatteExtCode.length() == 3) { // област
					col = "OBLAST";
				} else if (this.ekatteExtCode.length() == 5) { // община
					col = "OBSTINA";
				}

				if (col != null) {
					where.append(" and att." + col + " = :codeExt ");
					params.put("codeExt", this.ekatteExtCode);
				}
			}
		}

		setSql("" + select + from + where);
		setSqlCount(" select count(*) " + from + where);
		setSqlParameters(params);
	}

	/**
	 * Изчислява стойността на екатте за да може да се търси по област/община/нас.място
	 *
	 * @param sd
	 * @throws DbErrorException
	 */
	public void calcEkatte(BaseSystemData sd) throws DbErrorException {
		if (this.ekatte != null) { // има нещо

			if (this.ekatte.intValue() < 100000) { // населено място
				this.ekatteExtCode = null; // ще се търси по кода от класификацията

			} else { // обл/общ
				SystemClassif item = sd.decodeItemLite(CODE_CLASSIF_EKATTE, this.ekatte, CODE_DEFAULT_LANG, this.date, false);
				this.ekatteExtCode = item != null ? item.getCodeExt() : null; // ще се търси по външния код ако се открие
			}

		} else { // изчистено е и няма как да се търси
			this.ekatteExtCode = null;
		}
	}

	/** @return the country */
	public Integer getCountry() {
		return this.country;
	}

	/** @return the date */
	public Date getDate() {
		return this.date;
	}

	/** @return the eikEgn */
	public String getEikEgn() {
		return this.eikEgn;
	}

	/** @return the ekatte */
	public Integer getEkatte() {
		return this.ekatte;
	}

	/** @return the lnc */
	public String getLnc() {
		return this.lnc;
	}

	/** @return the refName */
	public String getRefName() {
		return this.refName;
	}

	/** @return the refType */
	public Integer getRefType() {
		return this.refType;
	}

	/** @return the exactly */
	public boolean isExactly() {
		return this.exactly;
	}

	/** @param country the country to set */
	public void setCountry(Integer country) {
		this.country = country;
	}

	/** @param date the date to set */
	public void setDate(Date date) {
		this.date = date;
	}

	/** @param eikEgn the eikEgn to set */
	public void setEikEgn(String eikEgn) {
		this.eikEgn = eikEgn;
	}

	/** @param ekatte the ekatte to set */
	public void setEkatte(Integer ekatte) {
		this.ekatte = ekatte;
	}

	/** @param exactly the exactly to set */
	public void setExactly(boolean exactly) {
		this.exactly = exactly;
	}

	/** @param lnc the lnc to set */
	public void setLnc(String lnc) {
		this.lnc = lnc;
	}

	/** @param refName the refName to set */
	public void setRefName(String refName) {
		this.refName = refName;
	}

	/** @param refType the refType to set */
	public void setRefType(Integer refType) {
		this.refType = refType;
	}

	/** @return the katNarList */
	public List<Integer> getKatNarList() {
		return this.katNarList;
	}

	/** @param katNarList the katNarList to set */
	public void setKatNarList(List<Integer> katNarList) {
		this.katNarList = katNarList;
	}

	/** @return the vidNarList */
	public List<Integer> getVidNarList() {
		return this.vidNarList;
	}

	/** @param vidNarList the vidNarList to set */
	public void setVidNarList(List<Integer> vidNarList) {
		this.vidNarList = vidNarList;
	}

	/** @return the tipOrganList */
	public List<Integer> getTipOrganList() {
		return this.tipOrganList;
	}

	/** @param tipOrganList the tipOrganList to set */
	public void setTipOrganList(List<Integer> tipOrganList) {
		this.tipOrganList = tipOrganList;
	}
}

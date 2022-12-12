package com.ib.omb.search;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DEF_PRAVA;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_NPM_FULL_EDIT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_SAMOS_FULL_EDIT;
import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.BaseSystemData;
import com.ib.system.BaseUserData;
import com.ib.system.SysConstants;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * Търсене на заповеди по НПМ и решения за самосезиране
 *
 * @author belev
 */
public class DocSpecSearch extends SelectMetadata {

	/**  */
	private static final long serialVersionUID = 4674497387122653782L;

	private String	rnDoc;			// Входящ номер;
	private boolean	rnDocEQ	= true;

	private Date	docDateFrom;	// Период на датата на регистриране;
	private Date	docDateTo;

	private List<Integer>	sastList;		// Състояние;
	private Date			sastDateFrom;	// Период на датата на състояние;
	private Date			sastDateTo;

	private Integer	codeLeader;		// Ръководител на екип
	private Integer	dopCodeExpert;	// Експерт от екипа

	private Date	startDateFrom;	// Период на датата на започване
	private Date	startDateTo;

	private Date	srokFrom;	// Период на срока за приключване;
	private Date	srokTo;

	private String	otnosno;	// Предмет на проверката
	private String	konstat;	// Констатации
	private String	docInfo;	// Допълнителна информация

	private Integer publicVisible; // Видима в публичния регистър;

	private Integer			codeOrgan;		// Проверен орган -код
	private String			nameOrgan;		// Проверен орган -име
	private List<Integer>	tipOrganList;	// Тип на орган по НПМ
	private List<Integer>	narPravaList;	// Нарушени права - заповеди по НПМ
	private List<Integer>	zasPravaList;	// Нарушени права - решения за самосезиране

	private Integer	vidResult;	// Резултат от проверка
	private Integer	prepor;		// Дадена препоръка

	/**
	 * Системата ще изведе списък със заповеди, които отговарят на зададените условия, като за АКТУАЛИЗАЦИЯ се налага достъп: <br>
	 * - ръководителят и експертите от екипа ще виждат заповедите, по които работят;<br>
	 * - ръководен служител с пълни права ще вижда заповедите, по които се работи в институцията на омбудсмана.<br>
	 * - потребител регистрирал заповедта<br>
	 * <br>
	 * Извеждат се следните <br>
	 * [0]-doc_id<br>
	 * [1]-rn_doc<br>
	 * [2]-doc_date<br>
	 * [3]-otnosno<br>
	 * [4]-code_leader<br>
	 * [5]-dopCodeExpertCodes-кодовете на експертите от екипа с разделител ',' (5,2,20)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date)}<br>
	 * [6]-srok<br>
	 * [7]-sast<br>
	 * [8]-sast_date<br>
	 * [9]-lock_user<br>
	 * [10]-lock_date<br>
	 * [11]-count_files<br>
	 *
	 * @param userData
	 * @param viewMode
	 */
	public void buildQueryNpmList(BaseUserData userData, boolean viewMode) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		select.append("select spec.doc_id a0, " + DocDAO.formRnDocSelect("d.", dialect) + " a1, d.doc_date a2, ");
		select.append(DialectConstructor.limitBigString(dialect, "d.otnosno", 300) + " a3, spec.code_leader a4, ");
		select.append(DialectConstructor.convertToDelimitedString(dialect, "de.code_ref", "doc_experts de where de.doc_id = spec.doc_id", "de.doc_id") + " a5 ");
		select.append(" , spec.srok a6, spec.sast a7, spec.sast_date a8 ");
		select.append(" , z.user_id a9, z.lock_date a10, d.count_files a11 ");

		StringBuilder from = new StringBuilder();
		from.append(" from doc_spec spec inner join doc d on d.doc_id = spec.doc_id ");

		StringBuilder where = new StringBuilder();
		where.append(" where d.doc_vid = :docVid ");
		params.put("docVid", OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM);

		String t = trimToNULL_Upper(this.rnDoc);
		if (t != null) {
			if (this.rnDocEQ) { // пълно съвпадение case insensitive
				where.append(" and upper(d.rn_doc) = :rnDoc ");
				params.put("rnDoc", t);

			} else {
				where.append(" and upper(d.rn_doc) like :rnDoc ");
				params.put("rnDoc", "%" + t + "%");
			}
		}

		if (this.docDateFrom != null) {
			where.append(" and d.doc_date >= :docDateFrom ");
			params.put("docDateFrom", DateUtils.startDate(this.docDateFrom));
		}
		if (this.docDateTo != null) {
			where.append(" and d.doc_date <= :docDateTo ");
			params.put("docDateTo", DateUtils.endDate(this.docDateTo));
		}

		if (this.sastList != null && !this.sastList.isEmpty()) {
			where.append(" and spec.sast in (:sastList) ");
			params.put("sastList", this.sastList);
		}
		if (this.sastDateFrom != null) {
			where.append(" and spec.sast_date >= :sastDateFrom ");
			params.put("sastDateFrom", DateUtils.startDate(this.sastDateFrom));
		}
		if (this.sastDateTo != null) {
			where.append(" and spec.sast_date <= :sastDateTo ");
			params.put("sastDateTo", DateUtils.endDate(this.sastDateTo));
		}

		if (this.codeLeader != null) {
			where.append(" and spec.code_leader = :codeLeader ");
			params.put("codeLeader", this.codeLeader);
		}
		if (this.dopCodeExpert != null) {
			where.append(" and EXISTS (select de.doc_id from doc_experts de where de.doc_id = spec.doc_id and de.code_ref = :dopCodeExpert) ");
			params.put("dopCodeExpert", this.dopCodeExpert);
		}

		if (this.srokFrom != null) {
			where.append(" and spec.srok >= :srokFrom ");
			params.put("srokFrom", DateUtils.startDate(this.srokFrom));
		}
		if (this.srokTo != null) {
			where.append(" and spec.srok <= :srokTo ");
			params.put("srokTo", DateUtils.endDate(this.srokTo));
		}

		if (this.startDateFrom != null) {
			where.append(" and spec.start_date >= :startDateFrom ");
			params.put("startDateFrom", DateUtils.startDate(this.startDateFrom));
		}
		if (this.startDateTo != null) {
			where.append(" and spec.start_date <= :startDateTo ");
			params.put("startDateTo", DateUtils.endDate(this.startDateTo));
		}

		t = trimToNULL_Upper(this.otnosno);
		if (t != null) {
			where.append(" and upper(d.otnosno) like :otnosno ");
			params.put("otnosno", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.docInfo);
		if (t != null) {
			where.append(" and upper(d.doc_info) like :docInfo ");
			params.put("docInfo", "%" + t + "%");
		}
		if (this.publicVisible != null) {
			where.append(" and spec.public_visible = :publicVisible ");
			params.put("publicVisible", this.publicVisible);
		}

		StringBuilder organ = new StringBuilder();
		String organWhere = " where so.doc_id = spec.doc_id ";
		if (this.codeOrgan != null) {

			organ.append((organ.length() == 0 ? organWhere : "") + " and sor.code = :codeOrgan ");
			params.put("codeOrgan", this.codeOrgan);

		} else {
			t = trimToNULL_Upper(this.nameOrgan);
			if (t != null) {
				organ.append((organ.length() == 0 ? organWhere : "") + " and upper(sor.ref_name) like :nameOrgan ");
				params.put("nameOrgan", "%" + t + "%");
			}
		}
		if (this.tipOrganList != null && !this.tipOrganList.isEmpty()) {
			organ.append((organ.length() == 0 ? organWhere : "") + " and sor.tip_organ in (:tipOrganList) ");
			params.put("tipOrganList", this.tipOrganList);
		}
		if (this.narPravaList != null && !this.narPravaList.isEmpty()) {
			organ.append((organ.length() == 0 ? organWhere : "") + " and so.nar_prava in (:narPravaList) ");
			params.put("narPravaList", this.narPravaList);
		}
		if (this.vidResult != null) {
			organ.append((organ.length() == 0 ? organWhere : "") + " and so.vid_result = :vidResult ");
			params.put("vidResult", this.vidResult);
		}
		if (this.prepor != null) {
			organ.append((organ.length() == 0 ? organWhere : "") + " and so.prepor = :prepor ");
			params.put("prepor", this.prepor);
		}

		if (organ.length() > 0) {
			where.append(" and EXISTS (select so.id from doc_spec_organ so left outer join adm_referents sor on sor.code = so.code_organ " + organ + " ) ");
		}

		if (!viewMode && !userData.hasAccess(CODE_CLASSIF_DEF_PRAVA, CODE_ZNACHENIE_DEF_PRAVA_NPM_FULL_EDIT)) {
			addAccessRules(where, params, (UserData) userData); // само за актуализация и ако няма пълен достъп
		}

		// за да се види има ли заключване // TODO нужно ли е в справката да се знае за заключването
		from.append(" left outer join lock_objects z on z.object_tip = :zTip and z.object_id = spec.doc_id and z.user_id != :zUser ");
		params.put("zTip", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		params.put("zUser", userData.getUserId());

		setSql(select.toString() + from.toString() + where.toString());
		setSqlCount(" select count(*) " + from.toString() + where.toString());
		setSqlParameters(params);
	}

	/**
	 * Системата ще изведе списък със Решение за самосезиране, които отговарят на зададените условия, като за АКТУАЛИЗАЦИЯ се
	 * налага достъп: <br>
	 * - потребителят е ръководител на екип<br>
	 * - или потребителят е член на екипа <br>
	 * - или потребителят има дефинитивно право „Пълен достъп за актуализация на решения за самосезиране“<br>
	 * - или потребителят е регистрирал документа.<br>
	 * <br>
	 * Извеждат се следните <br>
	 * [0]-doc_id<br>
	 * [1]-rn_doc<br>
	 * [2]-doc_date<br>
	 * [3]-otnosno<br>
	 * [4]-code_leader<br>
	 * [5]-dopCodeExpertCodes-кодовете на експертите от екипа с разделител ',' (5,2,20)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date)}<br>
	 * [6]-srok<br>
	 * [7]-sast<br>
	 * [8]-sast_date<br>
	 * [9]-lock_user<br>
	 * [10]-lock_date<br>
	 * [11]-count_files<br>
	 *
	 * @param userData
	 * @param viewMode
	 */
	public void buildQuerySamosList(BaseUserData userData, boolean viewMode) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		select.append("select spec.doc_id a0, " + DocDAO.formRnDocSelect("d.", dialect) + " a1, d.doc_date a2, ");
		select.append(DialectConstructor.limitBigString(dialect, "d.otnosno", 300) + " a3, spec.code_leader a4, ");
		select.append(DialectConstructor.convertToDelimitedString(dialect, "de.code_ref", "doc_experts de where de.doc_id = spec.doc_id", "de.doc_id") + " a5 ");
		select.append(" , spec.srok a6, spec.sast a7, spec.sast_date a8 ");
		select.append(" , z.user_id a9, z.lock_date a10, d.count_files a11 ");

		StringBuilder from = new StringBuilder();
		from.append(" from doc_spec spec inner join doc d on d.doc_id = spec.doc_id ");

		StringBuilder where = new StringBuilder();
		where.append(" where d.doc_vid = :docVid ");
		params.put("docVid", OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS);

		String t = trimToNULL_Upper(this.rnDoc);
		if (t != null) {
			if (this.rnDocEQ) { // пълно съвпадение case insensitive
				where.append(" and upper(d.rn_doc) = :rnDoc ");
				params.put("rnDoc", t);

			} else {
				where.append(" and upper(d.rn_doc) like :rnDoc ");
				params.put("rnDoc", "%" + t + "%");
			}
		}

		if (this.docDateFrom != null) {
			where.append(" and d.doc_date >= :docDateFrom ");
			params.put("docDateFrom", DateUtils.startDate(this.docDateFrom));
		}
		if (this.docDateTo != null) {
			where.append(" and d.doc_date <= :docDateTo ");
			params.put("docDateTo", DateUtils.endDate(this.docDateTo));
		}

		if (this.sastList != null && !this.sastList.isEmpty()) {
			where.append(" and spec.sast in (:sastList) ");
			params.put("sastList", this.sastList);
		}
		if (this.sastDateFrom != null) {
			where.append(" and spec.sast_date >= :sastDateFrom ");
			params.put("sastDateFrom", DateUtils.startDate(this.sastDateFrom));
		}
		if (this.sastDateTo != null) {
			where.append(" and spec.sast_date <= :sastDateTo ");
			params.put("sastDateTo", DateUtils.endDate(this.sastDateTo));
		}

		if (this.codeLeader != null) {
			where.append(" and spec.code_leader = :codeLeader ");
			params.put("codeLeader", this.codeLeader);
		}
		if (this.dopCodeExpert != null) {
			where.append(" and EXISTS (select de.doc_id from doc_experts de where de.doc_id = spec.doc_id and de.code_ref = :dopCodeExpert) ");
			params.put("dopCodeExpert", this.dopCodeExpert);
		}

		if (this.srokFrom != null) {
			where.append(" and spec.srok >= :srokFrom ");
			params.put("srokFrom", DateUtils.startDate(this.srokFrom));
		}
		if (this.srokTo != null) {
			where.append(" and spec.srok <= :srokTo ");
			params.put("srokTo", DateUtils.endDate(this.srokTo));
		}

		if (this.startDateFrom != null) {
			where.append(" and spec.start_date >= :startDateFrom ");
			params.put("startDateFrom", DateUtils.startDate(this.startDateFrom));
		}
		if (this.startDateTo != null) {
			where.append(" and spec.start_date <= :startDateTo ");
			params.put("startDateTo", DateUtils.endDate(this.startDateTo));
		}

		t = trimToNULL_Upper(this.otnosno);
		if (t != null) {
			where.append(" and upper(d.otnosno) like :otnosno ");
			params.put("otnosno", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.docInfo);
		if (t != null) {
			where.append(" and upper(d.doc_info) like :docInfo ");
			params.put("docInfo", "%" + t + "%");
		}
		if (this.publicVisible != null) {
			where.append(" and spec.public_visible = :publicVisible ");
			params.put("publicVisible", this.publicVisible);
		}

		StringBuilder organ = new StringBuilder();
		String organWhere = " where so.doc_id = spec.doc_id ";
		if (this.codeOrgan != null) {

			organ.append((organ.length() == 0 ? organWhere : "") + " and sor.code = :codeOrgan ");
			params.put("codeOrgan", this.codeOrgan);

		} else {
			t = trimToNULL_Upper(this.nameOrgan);
			if (t != null) {
				organ.append((organ.length() == 0 ? organWhere : "") + " and upper(sor.ref_name) like :nameOrgan ");
				params.put("nameOrgan", "%" + t + "%");
			}
		}
		if (this.zasPravaList != null && !this.zasPravaList.isEmpty()) {
			organ.append((organ.length() == 0 ? organWhere : "") + " and so.zas_prava in (:zasPravaList) ");
			params.put("zasPravaList", this.zasPravaList);
		}
		if (this.vidResult != null) {
			organ.append((organ.length() == 0 ? organWhere : "") + " and so.vid_result = :vidResult ");
			params.put("vidResult", this.vidResult);
		}
		if (this.prepor != null) {
			organ.append((organ.length() == 0 ? organWhere : "") + " and so.prepor = :prepor ");
			params.put("prepor", this.prepor);
		}

		if (organ.length() > 0) {
			where.append(" and EXISTS (select so.id from doc_spec_organ so left outer join adm_referents sor on sor.code = so.code_organ " + organ + " ) ");
		}

		if (!viewMode && !userData.hasAccess(CODE_CLASSIF_DEF_PRAVA, CODE_ZNACHENIE_DEF_PRAVA_SAMOS_FULL_EDIT)) {
			addAccessRules(where, params, (UserData) userData); // само за актуализация и ако няма пълен достъп
		}

		// за да се види има ли заключване // TODO нужно ли е в справката да се знае за заключването
		from.append(" left outer join lock_objects z on z.object_tip = :zTip and z.object_id = spec.doc_id and z.user_id != :zUser ");
		params.put("zTip", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		params.put("zUser", userData.getUserId());

		setSql(select.toString() + from.toString() + where.toString());
		setSqlCount(" select count(*) " + from.toString() + where.toString());
		setSqlParameters(params);
	}

	/** @return the codeLeader */
	public Integer getCodeLeader() {
		return this.codeLeader;
	}

	/** @return the codeOrgan */
	public Integer getCodeOrgan() {
		return this.codeOrgan;
	}

	/** @return the docDateFrom */
	public Date getDocDateFrom() {
		return this.docDateFrom;
	}

	/** @return the docDateTo */
	public Date getDocDateTo() {
		return this.docDateTo;
	}

	/** @return the docInfo */
	public String getDocInfo() {
		return this.docInfo;
	}

	/** @return the dopCodeExpert */
	public Integer getDopCodeExpert() {
		return this.dopCodeExpert;
	}

	/** @return the konstat */
	public String getKonstat() {
		return this.konstat;
	}

	/** @return the nameOrgan */
	public String getNameOrgan() {
		return this.nameOrgan;
	}

	/** @return the narPravaList */
	public List<Integer> getNarPravaList() {
		return this.narPravaList;
	}

	/** @return the otnosno */
	public String getOtnosno() {
		return this.otnosno;
	}

	/** @return the prepor */
	public Integer getPrepor() {
		return this.prepor;
	}

	/** @return the publicVisible */
	public Integer getPublicVisible() {
		return this.publicVisible;
	}

	/** @return the rnDoc */
	public String getRnDoc() {
		return this.rnDoc;
	}

	/** @return the sastDateFrom */
	public Date getSastDateFrom() {
		return this.sastDateFrom;
	}

	/** @return the sastDateTo */
	public Date getSastDateTo() {
		return this.sastDateTo;
	}

	/** @return the sastList */
	public List<Integer> getSastList() {
		return this.sastList;
	}

	/** @return the srokFrom */
	public Date getSrokFrom() {
		return this.srokFrom;
	}

	/** @return the srokTo */
	public Date getSrokTo() {
		return this.srokTo;
	}

	/** @return the startDateFrom */
	public Date getStartDateFrom() {
		return this.startDateFrom;
	}

	/** @return the startDateTo */
	public Date getStartDateTo() {
		return this.startDateTo;
	}

	/** @return the tipOrganList */
	public List<Integer> getTipOrganList() {
		return this.tipOrganList;
	}

	/** @return the vidResult */
	public Integer getVidResult() {
		return this.vidResult;
	}

	/** @return the zasPravaList */
	public List<Integer> getZasPravaList() {
		return this.zasPravaList;
	}

	/** @return the rnDocEQ */
	public boolean isRnDocEQ() {
		return this.rnDocEQ;
	}

	/**
	 * Публичен регистър на проверки по НПМ.<br>
	 * Винаги в списъка има поне един елемент, като първият елемент е брой резултатати, отговарящи на условията:<br>
	 * map(key="allcount",value=брой резултати отговарящи на условията)<br>
	 * <br>
	 * Останалите елементи в списъка имат следната структура: (като може и да няма такива елементи)<br>
	 * map(key="doc_id",value=ИД)<br>
	 * map(key="rn_doc",value=№ заповед)<br>
	 * map(key="doc_date",value=Дата на заповед)<br>
	 * map(key="predmet",value=Предмет на проверката)<br>
	 * map(key="nar_prava",value=Нарушени права)<br>
	 * map(key="code_organ",value=Проверяван орган)<br>
	 * map(key="capacity",value=Капацитет на проверявания орган) ! само за OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM<br>
	 * map(key="nas_lica",value=Реално настанени лица) ! само за OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM<br>
	 * map(key="konstat",value=Констатации)<br>
	 * map(key="prepor",value=Дадена препоръка)<br>
	 * map(key="vid_result",value=Резултат)<br>
	 * map(key="sast",value=Състояние)<br>
	 *
	 * @param pageSize  брой редове на страница
	 * @param pageIndex номера на страница, започват от 0
	 * @param sortCol   колона за сортиране. пример "doc_date desc". ако не е подадено има дефолт "doc_id desc"
	 * @param docVid    {@link OmbConstants#CODE_ZNACHENIE_DOC_VID_NPM} или {@link OmbConstants#CODE_ZNACHENIE_DOC_VID_SAMOS}
	 * @param sd
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<LinkedHashMap<String, Object>> restRegisterNpmSamos(int pageSize, int pageIndex, String sortCol, int docVid, BaseSystemData sd, int lang) throws DbErrorException {
		List<LinkedHashMap<String, Object>> result = new ArrayList<>();
		if (docVid != OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM && docVid != OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS) {
			return result;
		}
		try {
			String dialect = JPA.getUtil().getDbVendorName();

			Map<String, Object> params = new HashMap<>();

			StringBuilder select = new StringBuilder();
			select.append(" select spec.doc_id, d.rn_doc, d.doc_date, ");
			select.append(DialectConstructor.limitBigString(dialect, "d.otnosno", 500) + " predmet ");
			if (docVid == OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM) {
				select.append(" , organ.nar_prava, organ.code_organ, ");
			} else {
				select.append(" , organ.zas_prava, organ.code_organ, ");
			}
			select.append(DialectConstructor.limitBigString(dialect, "organ.konstat", 500) + " konstat ");
			select.append(" , organ.prepor, organ.vid_result, spec.sast, organ.capacity, organ.nas_lica ");

			String from = " from doc_spec spec inner join doc d on d.doc_id = spec.doc_id left outer join doc_spec_organ organ on organ.doc_id = spec.doc_id ";

			StringBuilder where = new StringBuilder();
			where.append(" where d.doc_vid = :docVid and spec.public_visible = 1 "); // само за тест е махнато
			params.put("docVid", docVid);

			String t = trimToNULL_Upper(this.rnDoc);
			if (t != null) {
				if (this.rnDocEQ) { // пълно съвпадение case insensitive
					where.append(" and upper(d.rn_doc) = :rnDoc ");
					params.put("rnDoc", t);

				} else {
					where.append(" and upper(d.rn_doc) like :rnDoc ");
					params.put("rnDoc", "%" + t + "%");
				}
			}

			if (this.docDateFrom != null) {
				where.append(" and d.doc_date >= :docDateFrom ");
				params.put("docDateFrom", DateUtils.startDate(this.docDateFrom));
			}
			if (this.docDateTo != null) {
				where.append(" and d.doc_date <= :docDateTo ");
				params.put("docDateTo", DateUtils.endDate(this.docDateTo));
			}

			t = trimToNULL_Upper(this.otnosno);
			if (t != null) {
				where.append(" and upper(d.otnosno) like :otnosno ");
				params.put("otnosno", "%" + t + "%");
			}
			t = trimToNULL_Upper(this.konstat);
			if (t != null) {
				where.append(" and upper(organ.konstat) like :konstat ");
				params.put("konstat", "%" + t + "%");
			}

			if (docVid == OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM && this.narPravaList != null && !this.narPravaList.isEmpty()) {
				where.append(" and organ.nar_prava in (:narPravaList) ");
				params.put("narPravaList", this.narPravaList);
			}
			if (docVid == OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS && this.zasPravaList != null && !this.zasPravaList.isEmpty()) {
				where.append(" and organ.zas_prava in (:zasPravaList) ");
				params.put("zasPravaList", this.zasPravaList);
			}
			if (this.codeOrgan != null) {
				where.append(" and organ.code_organ = :codeOrgan ");
				params.put("codeOrgan", this.codeOrgan);
			}

			if (this.prepor != null) {
				where.append(" and organ.prepor = :prepor ");
				params.put("prepor", this.prepor);
			}
			if (this.vidResult != null) {
				where.append(" and organ.vid_result = :vidResult ");
				params.put("vidResult", this.vidResult);
			}
			if (this.sastList != null && !this.sastList.isEmpty()) {
				where.append(" and spec.sast in (:sastList) ");
				params.put("sastList", this.sastList);
			}

			setSql(select + from + where);
			setSqlCount(" select count(*) " + from + where);
			setSqlParameters(params);

			if (sortCol == null || sortCol.trim().length() == 0) {
				sortCol = "doc_id desc";
			}

			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(this, sortCol);

			LinkedHashMap<String, Object> map = new LinkedHashMap<>();
			map.put("allcount", lazy.getRowCount()); // броя
			result.add(map);

			List<Object[]> rows = lazy.load(pageSize * pageIndex, pageSize, null, null);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			for (Object[] row : rows) {
				map = new LinkedHashMap<>();

				map.put("doc_id", row[0]);
				map.put("rn_doc", row[1]);
				map.put("doc_date", row[2] != null ? sdf.format((Date) row[2]) : null);
				map.put("predmet", row[3]);

				if (docVid == OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM) {
					map.put("nar_prava", row[4]);
					map.put("nar_prava_text", sd.decodeItem(OmbConstants.CODE_CLASSIF_NAR_PRAVA, SearchUtils.asInteger(row[4]), lang, null));
				} else {
					map.put("zas_prava", row[4]);
					map.put("zas_prava_text", sd.decodeItem(OmbConstants.CODE_CLASSIF_ZAS_PRAVA, SearchUtils.asInteger(row[4]), lang, null));
				}
				map.put("code_organ", row[5]);
				map.put("code_organ_text", sd.decodeItem(Constants.CODE_CLASSIF_REFERENTS, SearchUtils.asInteger(row[5]), SysConstants.CODE_DEFAULT_LANG, null));

				if (docVid == OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM) {
					map.put("capacity", row[10]);
					map.put("nas_lica", row[11]);
				}
				map.put("konstat", row[6]);
				map.put("prepor", row[7]);
				map.put("prepor_text", sd.decodeItem(SysConstants.CODE_CLASSIF_DANE, SearchUtils.asInteger(row[7]), lang, null));
				map.put("vid_result", row[8]);
				map.put("vid_result_text", sd.decodeItem(OmbConstants.CODE_CLASSIF_ORGAN_RES, SearchUtils.asInteger(row[8]), lang, null));
				map.put("sast", row[9]);
				map.put("sast_text", sd.decodeItem(OmbConstants.CODE_CLASSIF_PROVERKA_SAST, SearchUtils.asInteger(row[9]), lang, null));

				result.add(map);
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсен в Публичен регистър на проверки по НПМ", e);
		}

		return result;
	}

	/** @param codeLeader the codeLeader to set */
	public void setCodeLeader(Integer codeLeader) {
		this.codeLeader = codeLeader;
	}

	/** @param codeOrgan the codeOrgan to set */
	public void setCodeOrgan(Integer codeOrgan) {
		this.codeOrgan = codeOrgan;
	}

	/** @param docDateFrom the docDateFrom to set */
	public void setDocDateFrom(Date docDateFrom) {
		this.docDateFrom = docDateFrom;
	}

	/** @param docDateTo the docDateTo to set */
	public void setDocDateTo(Date docDateTo) {
		this.docDateTo = docDateTo;
	}

	/** @param docInfo the docInfo to set */
	public void setDocInfo(String docInfo) {
		this.docInfo = docInfo;
	}

	/** @param dopCodeExpert the dopCodeExpert to set */
	public void setDopCodeExpert(Integer dopCodeExpert) {
		this.dopCodeExpert = dopCodeExpert;
	}

	/** @param konstat the konstat to set */
	public void setKonstat(String konstat) {
		this.konstat = konstat;
	}

	/** @param nameOrgan the nameOrgan to set */
	public void setNameOrgan(String nameOrgan) {
		this.nameOrgan = nameOrgan;
	}

	/** @param narPravaList the narPravaList to set */
	public void setNarPravaList(List<Integer> narPravaList) {
		this.narPravaList = narPravaList;
	}

	/** @param otnosno the otnosno to set */
	public void setOtnosno(String otnosno) {
		this.otnosno = otnosno;
	}

	/** @param prepor the prepor to set */
	public void setPrepor(Integer prepor) {
		this.prepor = prepor;
	}

	/** @param publicVisible the publicVisible to set */
	public void setPublicVisible(Integer publicVisible) {
		this.publicVisible = publicVisible;
	}

	/** @param rnDoc the rnDoc to set */
	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}

	/** @param rnDocEQ the rnDocEQ to set */
	public void setRnDocEQ(boolean rnDocEQ) {
		this.rnDocEQ = rnDocEQ;
	}

	/** @param sastDateFrom the sastDateFrom to set */
	public void setSastDateFrom(Date sastDateFrom) {
		this.sastDateFrom = sastDateFrom;
	}

	/** @param sastDateTo the sastDateTo to set */
	public void setSastDateTo(Date sastDateTo) {
		this.sastDateTo = sastDateTo;
	}

	/** @param sastList the sastList to set */
	public void setSastList(List<Integer> sastList) {
		this.sastList = sastList;
	}

	/** @param srokFrom the srokFrom to set */
	public void setSrokFrom(Date srokFrom) {
		this.srokFrom = srokFrom;
	}

	/** @param srokTo the srokTo to set */
	public void setSrokTo(Date srokTo) {
		this.srokTo = srokTo;
	}

	/** @param startDateFrom the startDateFrom to set */
	public void setStartDateFrom(Date startDateFrom) {
		this.startDateFrom = startDateFrom;
	}

	/** @param startDateTo the startDateTo to set */
	public void setStartDateTo(Date startDateTo) {
		this.startDateTo = startDateTo;
	}

	/** @param tipOrganList the tipOrganList to set */
	public void setTipOrganList(List<Integer> tipOrganList) {
		this.tipOrganList = tipOrganList;
	}

	/** @param vidResult the vidResult to set */
	public void setVidResult(Integer vidResult) {
		this.vidResult = vidResult;
	}

	/** @param zasPravaList the zasPravaList to set */
	public void setZasPravaList(List<Integer> zasPravaList) {
		this.zasPravaList = zasPravaList;
	}

	/**
	 * налагане на достъп
	 */
	private void addAccessRules(StringBuilder where, Map<String, Object> params, UserData ud) {
		where.append(" and (spec.code_leader = :userAccess or d.user_reg = :userAccess ");
		where.append(" or EXISTS (select dea.doc_id from doc_experts dea where dea.doc_id = spec.doc_id and dea.code_ref = :userAccess) ) ");

		params.put("userAccess", ud.getUserAccess());
	}
}

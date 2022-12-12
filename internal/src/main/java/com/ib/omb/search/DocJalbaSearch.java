package com.ib.omb.search;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DEF_PRAVA;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_JALBA_FULL_EDIT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_KAT_NAR_CHASTEN_SUBEKT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_KAT_NAR_LIPSVA;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_VID_NAR_FZL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_VID_NAR_NEPOS;
import static com.ib.system.SysConstants.CODE_CLASSIF_EKATTE;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;
import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.Query;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.BaseSystemData;
import com.ib.system.BaseUserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * Справка за процеса на обработка на жалба
 *
 * @author belev
 */
public class DocJalbaSearch extends SelectMetadata {

	private static final String OR = " or ";

	/**  */
	private static final long serialVersionUID = 3131646194704349552L;

	private String	rnDoc;			// Входящ номер;
	private boolean	rnDocEQ	= true;

	private Date	docDateFrom;	// Период на датата на регистриране;
	private Date	docDateTo;

	private String	jbpName;	// Имена на жалбоподател;
	private String	jbpEgn;		// ЕГН;
	private String	jbpLnc;		// ЛНЧ;
	private String	jbpEik;		// ЕИК;
	private String	jbpPhone;	// телефон
	private String	jbpEmail;	// е-мейл

	private Integer	jbpType;	// Жалбоподател фзл/нфл
	private Integer	jbpGrj;		// Гражданство
	private Integer	jbpEkatte;	// Населено място

	private Date	dateNarFrom;	// Период на датата на извършване на нарушението;
	private Date	dateNarTo;

	private Integer	codeRefNar;	// кода на нарушител
	private String	nameNar;	// Наименование на нарушител;
	private Integer	katNar;		// Категория на нарушител;
	private Integer	vidNar;		// Вид на нарушител;

	private Integer	zasPrava;	// Засегнати права;
	private Integer	vidOpl;		// Вид оплакване;

	private String	otnosno;		// Описание на жалбата от експерт;
	private String	jalbaText;		// Описание от жалбоподателя;
	private String	requestText;	// Конкретно искане;
	private String	docInfo;		// Допълнителна информация

	private List<Integer>	sastList;		// Състояние на жалбата;
	private Date			sastDateFrom;	// Период на датата на състояние на жалбата;
	private Date			sastDateTo;

	private Date	srokFrom;	// Период на срока за разглеждане;
	private Date	srokTo;

	private Integer publicVisible; // Видима в публичния регистър;

	private Integer finMethod; // Начин на финализиране;

	private Integer	dopust;			// Допустима / Недопустима;
	private Integer	osnNedopust;	// Основание за недопустимост;

	private Integer codeZveno; // Звено, към което е разпределена жалбата;

	private Integer	codeExpert;		// Водещ експерт;
	private Integer	dopCodeExpert;	// Допълнителен експерт;

	private List<Integer>	submitMethodList;	// Начин на подаване;
	private Date			submitDateFrom;		// Период на датата подаване;
	private Date			submitDateTo;

	private Integer corruption; // Свързана с корупция.

	private Integer	vidResult;		// Вид на резултата
	private String	textSubject;	// Имена на жалбоподател или Орган/лице

	private Integer jbpHidden; // Запазена самоличност

	/**
	 * Системата ще изведе списък с жалби, които отговарят на зададените условия, като за АКТУАЛИЗАЦИЯ се налага достъп: <br>
	 * - водещ експерт ще вижда жалбите, по които трябва да работи; <br>
	 * - ръководител на звено ще вижда жалбите, по които се работи в звеното; <br>
	 * - ръководен служител с пълни права ще вижда жалбите, по които се работи в институцията на омбудсмана. <br>
	 * - потребител регистрирал жалбата <br>
	 * <br>
	 * Извеждат се следните данни: <br>
	 * [0]-doc_id<br>
	 * [1]-rn_doc<br>
	 * [2]-doc_date<br>
	 * [3]-jbp_name<br>
	 * [4]-jbp_egn/jbp_lnc/jbp_eik<br>
	 * [5]-jbp_ekatte<br>
	 * [6]-nar.ref_name<br>
	 * [7]-sast<br>
	 * [8]-sast_date<br>
	 * [9]-code_expert<br>
	 * [10]-lock_user<br>
	 * [11]-lock_date<br>
	 * [12]-count_files<br>
	 *
	 * @param userData
	 * @param viewMode
	 * @param sd
	 * @throws DbErrorException
	 */
	public void buildQueryJalbaList(BaseUserData userData, boolean viewMode, BaseSystemData sd) throws DbErrorException {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		boolean joinLock = !viewMode; // само за актуализация

		StringBuilder select = new StringBuilder();
		select.append("select j.doc_id a0, " + DocDAO.formRnDocSelect("d.", dialect) + " a1, d.doc_date a2, j.jbp_name a3 ");
		select.append(" , case when j.jbp_egn is not null then j.jbp_egn when j.jbp_eik is not null then j.jbp_eik else j.jbp_lnc end a4 ");
		select.append(" , j.jbp_ekatte a5, nar.ref_name a6, j.sast a7, j.sast_date a8, j.code_expert a9 ");
		select.append(" , " + (joinLock ? "z.user_id" : "null") + " a10, " + (joinLock ? "z.lock_date" : "null") + " a11, d.count_files a12 ");

		StringBuilder from = new StringBuilder();
		from.append(" from doc d inner join doc_jalba j on j.doc_id = d.doc_id ");

		boolean narBeforCnt = false;
		boolean ekatteBeforCnt = false;

		StringBuilder where = new StringBuilder();
		where.append(" where 1=1 ");

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

		t = trimToNULL_Upper(this.jbpName);
		if (t != null) {
			if (t.indexOf(' ') != -1) {
				t = t.replace(' ', '%');
			}
			where.append(" and upper(j.jbp_name) like :jbpName ");
			params.put("jbpName", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.jbpEgn);
		if (t != null) {
			where.append(" and j.jbp_egn like :jbpEgn ");
			params.put("jbpEgn", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.jbpLnc);
		if (t != null) {
			where.append(" and j.jbp_lnc like :jbpLnc ");
			params.put("jbpLnc", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.jbpEik);
		if (t != null) {
			where.append(" and j.jbp_eik like :jbpEik ");
			params.put("jbpEik", "%" + t + "%");
		}

		t = trimToNULL_Upper(this.jbpPhone);
		if (t != null) {
			where.append(" and upper(j.jbp_phone) like :jbpPhone ");
			params.put("jbpPhone", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.jbpEmail);
		if (t != null) {
			where.append(" and upper(j.jbp_email) like :jbpEmail ");
			params.put("jbpEmail", "%" + t + "%");
		}

		if (this.jbpGrj != null) {
			where.append(" and j.jbp_grj = :jbpGrj ");
			params.put("jbpGrj", this.jbpGrj);
		}

		if (this.jbpEkatte != null) {
			if (this.jbpEkatte.intValue() < 100_000) { // търсене по населено място
				ekatteBeforCnt = true;
				where.append(" and att.EKATTE = :ekatte ");
				params.put("ekatte", this.jbpEkatte);

			} else { // търсене по област или община
				SystemClassif item = sd.decodeItemLite(CODE_CLASSIF_EKATTE, this.jbpEkatte, CODE_DEFAULT_LANG, null, false);

				if (item != null && item.getCodeExt() != null) {
					String col = null;
					if (item.getCodeExt().length() == 3) { // област
						col = "OBLAST";
					} else if (item.getCodeExt().length() == 5) { // община
						col = "OBSTINA";
					}

					if (col != null) {
						ekatteBeforCnt = true;
						where.append(" and att." + col + " = :codeExt ");
						params.put("codeExt", item.getCodeExt());
					}
				}
			}
		}

		if (this.dateNarFrom != null) {
			where.append(" and j.date_nar >= :dateNarFrom ");
			params.put("dateNarFrom", DateUtils.startDate(this.dateNarFrom));
		}
		if (this.dateNarTo != null) {
			where.append(" and j.date_nar <= :dateNarTo ");
			params.put("dateNarTo", DateUtils.endDate(this.dateNarTo));
		}

		if (this.codeRefNar != null) {
			narBeforCnt = true;
			where.append(" and nar.code = :codeRefNar ");
			params.put("codeRefNar", this.codeRefNar);
		} else {
			t = trimToNULL_Upper(this.nameNar);
			if (t != null) {
				narBeforCnt = true;
				where.append(" and upper(nar.ref_name) like :nameNar ");
				params.put("nameNar", "%" + t + "%");
			}
		}
		if (this.katNar != null) {
			narBeforCnt = true;
			where.append(" and nar.kat_nar = :katNar ");
			params.put("katNar", this.katNar);
		}
		if (this.vidNar != null) {
			narBeforCnt = true;
			where.append(" and nar.vid_nar = :vidNar ");
			params.put("vidNar", this.vidNar);
		}

		if (this.zasPrava != null) {
			where.append(" and j.zas_prava = :zasPrava ");
			params.put("zasPrava", this.zasPrava);
		}
		if (this.vidOpl != null) {
			where.append(" and j.vid_opl = :vidOpl ");
			params.put("vidOpl", this.vidOpl);
		}

		t = trimToNULL_Upper(this.otnosno);
		if (t != null) {
			where.append(" and upper(d.otnosno) like :otnosno ");
			params.put("otnosno", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.jalbaText);
		if (t != null) {
			where.append(" and upper(j.jalba_text) like :jalbaText ");
			params.put("jalbaText", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.requestText);
		if (t != null) {
			where.append(" and upper(j.request_text) like :requestText ");
			params.put("requestText", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.docInfo);
		if (t != null) {
			where.append(" and upper(d.doc_info) like :docInfo ");
			params.put("docInfo", "%" + t + "%");
		}

		if (this.sastList != null && !this.sastList.isEmpty()) {
			where.append(" and j.sast in (:sastList) ");
			params.put("sastList", this.sastList);
		}
		if (this.sastDateFrom != null) {
			where.append(" and j.sast_date >= :sastDateFrom ");
			params.put("sastDateFrom", DateUtils.startDate(this.sastDateFrom));
		}
		if (this.sastDateTo != null) {
			where.append(" and j.sast_date <= :sastDateTo ");
			params.put("sastDateTo", DateUtils.endDate(this.sastDateTo));
		}

		if (this.srokFrom != null) {
			where.append(" and j.srok >= :srokFrom ");
			params.put("srokFrom", DateUtils.startDate(this.srokFrom));
		}
		if (this.srokTo != null) {
			where.append(" and j.srok <= :srokTo ");
			params.put("srokTo", DateUtils.endDate(this.srokTo));
		}

		if (this.publicVisible != null) {
			where.append(" and j.public_visible = :publicVisible ");
			params.put("publicVisible", this.publicVisible);
		}

		if (this.finMethod != null) {
			where.append(" and j.fin_method = :finMethod ");
			params.put("finMethod", this.finMethod);
		}

		if (this.dopust != null) {
			where.append(" and j.dopust = :dopust ");
			params.put("dopust", this.dopust);
		}
		if (this.osnNedopust != null) {
			where.append(" and j.osn_nedopust = :osnNedopust ");
			params.put("osnNedopust", this.osnNedopust);
		}

		if (this.codeZveno != null) {
			where.append(" and j.code_zveno = :codeZveno ");
			params.put("codeZveno", this.codeZveno);
		}
		if (this.codeExpert != null) {
			where.append(" and j.code_expert = :codeExpert ");
			params.put("codeExpert", this.codeExpert);
		}
		if (this.dopCodeExpert != null) {
			where.append(" and EXISTS (select de.doc_id from doc_experts de where de.doc_id = j.doc_id and de.code_ref = :dopCodeExpert) ");
			params.put("dopCodeExpert", this.dopCodeExpert);
		}

		if (this.submitMethodList != null && !this.submitMethodList.isEmpty()) {
			where.append(" and j.submit_method in (:submitMethodList) ");
			params.put("submitMethodList", this.submitMethodList);
		}
		if (this.submitDateFrom != null) {
			where.append(" and j.submit_date >= :submitDateFrom ");
			params.put("submitDateFrom", DateUtils.startDate(this.submitDateFrom));
		}
		if (this.submitDateTo != null) {
			where.append(" and j.submit_date <= :submitDateTo ");
			params.put("submitDateTo", DateUtils.endDate(this.submitDateTo));
		}

		if (this.corruption != null) {
			where.append(" and j.corruption = :corruption ");
			params.put("corruption", this.corruption);
		}

		t = trimToNULL_Upper(this.textSubject);
		if (this.vidResult != null || t != null) {
			where.append(" and EXISTS (select djr.id from doc_jalba_result djr where djr.doc_id = j.doc_id ");
			if (this.vidResult != null) {
				where.append(" and djr.vid_result = :vidResult ");
				params.put("vidResult", this.vidResult);
			}
			if (t != null) {
				where.append(" and upper(djr.text_subject) like :textSubject ");
				params.put("textSubject", "%" + t + "%");
			}
			where.append(" ) ");
		}

		if (this.jbpHidden != null) {
			where.append(" and j.jbp_hidden = :jbpHidden ");
			params.put("jbpHidden", this.jbpHidden);
		}

		if (!viewMode) { // за сега в режим на преглед няма налагане на достъп
			addAccessRules(where, params, (UserData) userData);
		}

		if (narBeforCnt) {
			from.append(" left outer join adm_referents nar on nar.code = j.code_nar "); // нарушителя
		}
		if (this.jbpEkatte != null && ekatteBeforCnt) {
			from.append(" left outer join EKATTE_ATT att on att.EKATTE = j.jbp_ekatte ");
		}
		setSqlCount(" select count(*) " + from.toString() + where.toString());

		if (!narBeforCnt) {
			from.append(" left outer join adm_referents nar on nar.code = j.code_nar "); // нарушителя
		}
		if (this.jbpEkatte != null && !ekatteBeforCnt) {
			from.append(" left outer join EKATTE_ATT att on att.EKATTE = j.jbp_ekatte ");
		}

		if (joinLock) { // за да се види има ли заключване - само в актуализация и то след каунта, защото не му влияе
			from.append(" left outer join lock_objects z on z.object_tip = " + OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC //
				+ " and z.object_id = j.doc_id and z.user_id != " + userData.getUserId() + " ");
//			params.put("zTip", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
//			params.put("zUser", userData.getUserId());
		}

		setSql(select.toString() + from.toString() + where.toString());
		setSqlParameters(params);
	}

	/**
	 * При натискане на бутон <Предишни жалби> системата ще извършва справка по ЕГН или ЛНЧ, или телефон или е-мейл/ да се включи
	 * и „име“ на жалбоподателя и при откриване на съвпадение на някоя от тези данни в други жалби, ще изведе списък с намерените
	 * жалби. <br>
	 * <br>
	 * Извеждат се следните <br>
	 * [0]-doc_id<br>
	 * [1]-rn_doc<br>
	 * [2]-doc_date<br>
	 * [3]-jbp_name<br>
	 * [4]-jbp_egn<br>
	 * [5]-jbp_lnc<br>
	 * [6]-jbp_eik<br>
	 * [7]-jbp_phone<br>
	 * [8]-jbp_email<br>
	 * [9]-jbp_ekatte<br>
	 * [10]-jbp_addr<br>
	 * [11]-sast<br>
	 * [12]-code_expert<br>
	 *
	 * @param currentId ид на текущата жалба като може и да е нулл
	 * @throws InvalidParameterException
	 */
	public void buildQueryPrevJalbaList(Integer currentId) throws InvalidParameterException {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		select.append(" select j.doc_id a0, " + DocDAO.formRnDocSelect("d.", dialect) + " a1, d.doc_date a2 ");
		select.append(" , jbp_name a3, jbp_egn a4, jbp_lnc a5, jbp_eik a6, jbp_phone a7, jbp_email a8, jbp_ekatte a9, jbp_addr a10 ");
		select.append(" , j.sast a11, j.code_expert a12 ");

		StringBuilder from = new StringBuilder();
		from.append(" from doc_jalba j left outer join doc d on d.doc_id = j.doc_id ");

		StringBuilder or = new StringBuilder();

		// тука се проверява с пълно съвпадение и OR
		String t = trimToNULL_Upper(this.jbpEgn);
		if (t != null) {
			if (or.length() > 0) {
				or.append(OR);
			}
			or.append(" j.jbp_egn = :jbpEgn ");
			params.put("jbpEgn", t);
		}
		t = trimToNULL_Upper(this.jbpLnc);
		if (t != null) {
			if (or.length() > 0) {
				or.append(OR);
			}
			or.append(" j.jbp_lnc = :jbpLnc ");
			params.put("jbpLnc", t);
		}
		t = trimToNULL_Upper(this.jbpEik);
		if (t != null) {
			if (or.length() > 0) {
				or.append(OR);
			}
			or.append(" j.jbp_eik = :jbpEik ");
			params.put("jbpEik", t);
		}

		t = SearchUtils.trimToNULL(this.jbpPhone);
		if (t != null) {
			if (or.length() > 0) {
				or.append(OR);
			}
			or.append(" j.jbp_phone = :jbpPhone ");
			params.put("jbpPhone", t);
		}
		t = trimToNULL_Upper(this.jbpEmail);
		if (t != null) {
			if (or.length() > 0) {
				or.append(OR);
			}
			or.append(" upper(j.jbp_email) = :jbpEmail ");
			params.put("jbpEmail", t);
		}

		t = trimToNULL_Upper(this.jbpName);
		if (t != null) {
			if (or.length() > 0) {
				or.append(OR);
			}

			if (t.indexOf('\t') != -1) { // таба ще обърка схемата
				t = t.replace("\t", " ");
			}
			while (t.indexOf("  ") != -1) { // не трябва да има двойни интервали, че и те ще объркат схемата
				t = t.replace("  ", " ");
			}

			// търсене по пълно съвпадене по начина по който е изписано
			or.append(" upper(j.jbp_name) = :jbpName ");
			params.put("jbpName", t);

			String[] split = t.split(" ");
			if (split.length > 1) { // ще се добавя и още, за да се изкарат съвпадащи по име и фамилия

				if (split.length == 2) { // име фамилия

					or.append(" or upper(j.jbp_name) like :jbpName2 ");
					params.put("jbpName2", split[0] + " % " + split[1]); // оставам " % " за презиме

				} else if (split.length == 3) { // име презиме фамилия

					or.append(" or upper(j.jbp_name) = :jbpName2 ");
					params.put("jbpName2", split[0] + " " + split[2]); // остава само име + фамилия

				} else { // явно има много имена
					or.append(" or upper(j.jbp_name) = :jbpName2 "); // остава име + последните две имена
					params.put("jbpName2", split[0] + " " + split[split.length - 2] + " " + split[split.length - 1]);
				}
			}
		}

		if (or.length() == 0) {
			throw new InvalidParameterException("Липсват аргумени за търсене.");
		}

		StringBuilder where = new StringBuilder();
		if (currentId == null) {
			where.append(" where " + or);
		} else {
			where.append(" where j.doc_id != :currentId and (" + or + ") ");
			params.put("currentId", currentId);
		}

		setSql(select.toString() + from.toString() + where.toString());
		setSqlCount(" select count(*) " + from.toString() + where.toString());
		setSqlParameters(params);
	}

	/** @return the codeExpert */
	public Integer getCodeExpert() {
		return this.codeExpert;
	}

	/** @return the codeRefNar */
	public Integer getCodeRefNar() {
		return this.codeRefNar;
	}

	/** @return the codeZveno */
	public Integer getCodeZveno() {
		return this.codeZveno;
	}

	/** @return the corruption */
	public Integer getCorruption() {
		return this.corruption;
	}

	/** @return the dateNarFrom */
	public Date getDateNarFrom() {
		return this.dateNarFrom;
	}

	/** @return the dateNarTo */
	public Date getDateNarTo() {
		return this.dateNarTo;
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

	/** @return the dopust */
	public Integer getDopust() {
		return this.dopust;
	}

	/** @return the finMethod */
	public Integer getFinMethod() {
		return this.finMethod;
	}

	/** @return the jalbaText */
	public String getJalbaText() {
		return this.jalbaText;
	}

	/** @return the jbpEgn */
	public String getJbpEgn() {
		return this.jbpEgn;
	}

	/** @return the jbpEik */
	public String getJbpEik() {
		return this.jbpEik;
	}

	/** @return the jbpEkatte */
	public Integer getJbpEkatte() {
		return this.jbpEkatte;
	}

	/** @return the jbpEmail */
	public String getJbpEmail() {
		return this.jbpEmail;
	}

	/** @return the jbpGrj */
	public Integer getJbpGrj() {
		return this.jbpGrj;
	}

	/** @return the jbpHidden */
	public Integer getJbpHidden() {
		return this.jbpHidden;
	}

	/** @return the jbpLnc */
	public String getJbpLnc() {
		return this.jbpLnc;
	}

	/** @return the jbpName */
	public String getJbpName() {
		return this.jbpName;
	}

	/** @return the jbpPhone */
	public String getJbpPhone() {
		return this.jbpPhone;
	}

	/** @return the jbpType */
	public Integer getJbpType() {
		return this.jbpType;
	}

	/** @return the katNar */
	public Integer getKatNar() {
		return this.katNar;
	}

	/** @return the nameNar */
	public String getNameNar() {
		return this.nameNar;
	}

	/** @return the osnNedopust */
	public Integer getOsnNedopust() {
		return this.osnNedopust;
	}

	/** @return the otnosno */
	public String getOtnosno() {
		return this.otnosno;
	}

	/** @return the publicVisible */
	public Integer getPublicVisible() {
		return this.publicVisible;
	}

	/** @return the requestText */
	public String getRequestText() {
		return this.requestText;
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

	/** @return the submitDateFrom */
	public Date getSubmitDateFrom() {
		return this.submitDateFrom;
	}

	/** @return the submitDateTo */
	public Date getSubmitDateTo() {
		return this.submitDateTo;
	}

	/** @return the submitMethodList */
	public List<Integer> getSubmitMethodList() {
		return this.submitMethodList;
	}

	/** @return the textSubject */
	public String getTextSubject() {
		return this.textSubject;
	}

	/** @return the vidNar */
	public Integer getVidNar() {
		return this.vidNar;
	}

	/** @return the vidOpl */
	public Integer getVidOpl() {
		return this.vidOpl;
	}

	/** @return the vidResult */
	public Integer getVidResult() {
		return this.vidResult;
	}

	/** @return the zasPrava */
	public Integer getZasPrava() {
		return this.zasPrava;
	}

	/** @return the rnDocEQ */
	public boolean isRnDocEQ() {
		return this.rnDocEQ;
	}

	/**
	 * Публичен регистър на жалби.<br>
	 * Винаги в списъка има поне един елемент, като първият елемент е брой резултатати, отговарящи на условията:<br>
	 * map(key="allcount",value=брой резултати отговарящи на условията)<br>
	 * <br>
	 * Останалите елементи в списъка имат следната структура: (като може и да няма такива елементи)<br>
	 * map(key="doc_id",value=ИД)<br>
	 * map(key="rn_doc",value=Вх. №)<br>
	 * map(key="doc_date",value=Дата на жалба/сигнал)<br>
	 * map(key="jbp_type",value=Категория на подателя на жалбата/сигнала)<br>
	 * map(key="kat_nar",value=Категория на орган/лице, срещу което е образувана преписката)<br>
	 * map(key="vid_nar",value=Вид на орган/лице, срещу което е образувана преписката)<br>
	 * map(key="zas_prava",value=Засегнати права)<br>
	 * map(key="vid_opl",value=Вид оплакване)<br>
	 * map(key="sast",value=Състояние на жалбата)<br>
	 *
	 * @param pageSize  брой редове на страница
	 * @param pageIndex номера на страница, започват от 0
	 * @param sortCol   колона за сортиране. пример "doc_date desc". ако не е подадено има дефолт "doc_id desc"
	 * @param sd
	 * @param lang
	 * @return
	 * @throws DbErrorException
	 */
	public List<LinkedHashMap<String, Object>> restRegisterJalbi(int pageSize, int pageIndex, String sortCol, BaseSystemData sd, int lang) throws DbErrorException {
		List<LinkedHashMap<String, Object>> result = new ArrayList<>();

		try {
			Map<String, Object> params = new HashMap<>();

			StringBuilder select = new StringBuilder();
			select.append(" select j.doc_id, d.rn_doc, d.doc_date, j.jbp_type ");

			select.append(" , case when nar.ref_type = 4 then " + CODE_ZNACHENIE_KAT_NAR_CHASTEN_SUBEKT);
			select.append(" when nar.kat_nar is null then " + CODE_ZNACHENIE_KAT_NAR_LIPSVA + " else nar.kat_nar end kat_nar ");

			select.append(" , case when nar.ref_type = 4 then " + CODE_ZNACHENIE_VID_NAR_FZL);
			select.append(" when nar.vid_nar is null then " + CODE_ZNACHENIE_VID_NAR_NEPOS + " else nar.vid_nar end vid_nar ");

			select.append(" , j.zas_prava, j.vid_opl, j.sast ");

			String from = //
				" from doc_jalba j inner join doc d on d.doc_id = j.doc_id left outer join adm_referents nar on nar.code = j.code_nar ";

			StringBuilder where = new StringBuilder();
			where.append(" where j.public_visible = 1 ");
//			where.append(" where 1=1 "); // само за тест, защото няма публикувани още

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

			if (this.jbpType != null) {
				where.append(" and j.jbp_type = :jbpType ");
				params.put("jbpType", this.jbpType);
			}

			if (this.katNar != null) {
				if (this.katNar.equals(CODE_ZNACHENIE_KAT_NAR_CHASTEN_SUBEKT)) { // или фзл или точно търсената категория
					where.append(" and (nar.ref_type =:refType or nar.kat_nar = :katNar) ");
					params.put("refType", OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL);
					params.put("katNar", this.katNar);

				} else if (this.katNar.equals(CODE_ZNACHENIE_KAT_NAR_LIPSVA)) { // без нарушител или да е нфл И празна категория
																				// или точно търсената категория
					where.append(" and (nar.ref_type is null or nar.ref_type =:refType) and (nar.kat_nar is null or nar.kat_nar = :katNar) ");
					params.put("refType", OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL);
					params.put("katNar", this.katNar);

				} else { // иначе каквото е въведено
					where.append(" and nar.kat_nar = :katNar ");
					params.put("katNar", this.katNar);
				}
			}

			if (this.vidNar != null) {
				if (this.vidNar.equals(CODE_ZNACHENIE_VID_NAR_FZL)) { // само фзл

					where.append(" and nar.ref_type = :refType ");
					params.put("refType", OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL);

				} else if (this.vidNar.equals(CODE_ZNACHENIE_VID_NAR_NEPOS)) { // без нарушител или да е нфл И празен вид
																				// или точно търсеният
					where.append(" and (nar.ref_type is null or nar.ref_type =:refType) and (nar.vid_nar is null or nar.vid_nar = :vidNar) ");
					params.put("refType", OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL);
					params.put("vidNar", this.vidNar);

				} else { // иначе каквото е въведено
					where.append(" and nar.vid_nar = :vidNar ");
					params.put("vidNar", this.vidNar);
				}
			}

			if (this.zasPrava != null) {
				where.append(" and j.zas_prava = :zasPrava ");
				params.put("zasPrava", this.zasPrava);
			}
			if (this.vidOpl != null) {
				where.append(" and j.vid_opl = :vidOpl ");
				params.put("vidOpl", this.vidOpl);
			}

			if (this.sastList != null && !this.sastList.isEmpty()) {
				where.append(" and j.sast in (:sastList) ");
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
				map.put("jbp_type", row[3]);
				String jbpTypeText = null;
				if (row[3] != null) {
					if (((Number) row[3]).intValue() == OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL) {
						jbpTypeText = lang == SysConstants.CODE_LANG_BG ? "юридическо лице" : "legal entity";
					} else {
						jbpTypeText = lang == SysConstants.CODE_LANG_BG ? "физическо лице" : "individual";
					}
				}
				map.put("jbp_type_text", jbpTypeText);
				map.put("kat_nar", row[4]);
				map.put("kat_nar_text", sd.decodeItem(OmbConstants.CODE_CLASSIF_KAT_NAR, SearchUtils.asInteger(row[4]), lang, null));
				map.put("vid_nar", row[5]);
				map.put("vid_nar_text", sd.decodeItem(OmbConstants.CODE_CLASSIF_VID_NAR, SearchUtils.asInteger(row[5]), lang, null));
				map.put("zas_prava", row[6]);
				map.put("zas_prava_text", sd.decodeItem(OmbConstants.CODE_CLASSIF_ZAS_PRAVA, SearchUtils.asInteger(row[6]), lang, null));
				map.put("vid_opl", row[7]);
				map.put("vid_opl_text", sd.decodeItem(OmbConstants.CODE_CLASSIF_VID_OPL, SearchUtils.asInteger(row[7]), lang, null));
				map.put("sast", row[8]);
				map.put("sast_text", sd.decodeItem(OmbConstants.CODE_CLASSIF_JALBA_SAST, SearchUtils.asInteger(row[8]), lang, null));

				result.add(map);
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсен в Публичен регистър на жалби", e);
		}

		return result;
	}

	/**
	 * Първият елемнт в списъка е жалба/нпм/самосезиране. Вторият е преписката. Останалите са сортирани по [3]-Дата на
	 * регистарция<br>
	 * <br>
	 * [0]-Тип обект<br>
	 * [1]-ИД обект<br>
	 * [2]-Номер/Дата<br>
	 * [3]-Дата на регистарция<br>
	 * [4]-Пояснителен текст<br>
	 * [5]-файлове- List<Object[]> -[0]file_id, [1]filename<br>
	 * [6]- 1=има достъп, 2=няма достъп<br>
	 * [7]-docId само за задачите<br>
	 *
	 * @param initDoc ИД на жалбата
	 * @param sd
	 * @param ud
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> selectProcessObjects(Integer initDoc, BaseSystemData sd, UserData ud) throws DbErrorException {
		List<Object[]> result = new ArrayList<>(); // жалба/нпм/самосезиране + преписката
		List<Object[]> rows = new ArrayList<>(); // всичко друго

		try {
			Map<Integer, String> docData = new HashMap<>(); // key=docId, value=rnDoc/docDate
			Map<Integer, Set<Integer>> docIndex = new HashMap<>(); // key=docId, value= value=index in List<Object[]> rows = new
																	// ArrayList<>();

			// жалба/нпм/самосезиране
			List<Object[]> temp = JPA.getUtil().getEntityManager().createNativeQuery( //
				"select d.doc_id, d.rn_doc, d.doc_date, d.pored_delo, d.date_reg, d.doc_type, d.doc_vid from doc d" //
					+ " where d.doc_id = ?1") //
				.setParameter(1, initDoc).getResultList();
			for (Object[] t : temp) {
				Object[] row = new Object[8];

				Integer docId = ((Number) t[0]).intValue();
				String rnDocDate = DocDAO.formRnDocDate(t[1], t[2], t[3]);

				row[0] = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC;
				row[1] = docId;
				row[2] = rnDocDate;
				row[3] = t[4];

				StringBuilder info = new StringBuilder();
				info.append(sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, ((Number) t[6]).intValue(), SysConstants.CODE_DEFAULT_LANG, null));
				info.append(" (" + sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_TYPE, ((Number) t[5]).intValue(), SysConstants.CODE_DEFAULT_LANG, null) + ")");
				row[4] = info.toString();

				Query fileQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
					"select f.file_id, f.filename from files f inner join file_objects fo on f.file_id = fo.file_id" //
						+ " where fo.object_id = ?1 and fo.object_code = ?2 order by fo.id");
				row[5] = fileQuery.setParameter(1, docId).setParameter(2, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC) //
					.getResultList();
				row[6] = SysConstants.CODE_ZNACHENIE_DA;

				result.add(row);
				docData.put(docId, rnDocDate);
			}

			// преписката
			Integer deloId = null;
			temp = JPA.getUtil().getEntityManager().createNativeQuery( //
				"select d.delo_id, d.rn_delo, d.delo_date, d.date_reg, d.delo_type from delo d where d.init_doc_id = ?1") //
				.setParameter(1, initDoc).getResultList();
			for (Object[] t : temp) {
				Object[] row = new Object[8];

				deloId = ((Number) t[0]).intValue();

				row[0] = OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO;
				row[1] = deloId;
				row[2] = DocDAO.formRnDocDate(t[1], t[2], null); // върши работа и за дело като не подавам поред
				row[3] = t[3];

				StringBuilder info = new StringBuilder();
				info.append(sd.decodeItem(OmbConstants.CODE_CLASSIF_DELO_TYPE, ((Number) t[4]).intValue(), SysConstants.CODE_DEFAULT_LANG, null));
				row[4] = info.toString();

				row[5] = new ArrayList<>();
				row[6] = SysConstants.CODE_ZNACHENIE_DA;

				result.add(row);
			}

			if (deloId != null) { // другите вложените документи в преписката
				temp = JPA.getUtil().getEntityManager().createNativeQuery( //
					"select d.doc_id, d.rn_doc, d.doc_date, d.pored_delo, dd.date_reg, d.doc_type, d.doc_vid, d.code_ref_corresp" //
						+ " from delo_doc dd inner join doc d on d.doc_id = dd.input_doc_id" //
						+ " where dd.delo_id = ?1 and dd.input_doc_id != ?2 order by dd.id") //
					.setParameter(1, deloId).setParameter(2, initDoc).getResultList();
				for (Object[] t : temp) {
					Object[] row = new Object[8];

					Integer docId = ((Number) t[0]).intValue();
					String rnDocDate = DocDAO.formRnDocDate(t[1], t[2], t[3]);

					row[0] = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC;
					row[1] = docId;
					row[2] = rnDocDate;
					row[3] = t[4];

					StringBuilder info = new StringBuilder();
					info.append(sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, ((Number) t[6]).intValue(), SysConstants.CODE_DEFAULT_LANG, null));
					info.append(" (" + sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_TYPE, ((Number) t[5]).intValue(), SysConstants.CODE_DEFAULT_LANG, null) + ")");
					if (t[7] != null) {
						info.append(" изпратен от " + sd.decodeItem(Constants.CODE_CLASSIF_REFERENTS, ((Number) t[7]).intValue(), SysConstants.CODE_DEFAULT_LANG, (Date) t[2]));
					}
					row[4] = info.toString();

					row[5] = new ArrayList<>();
					row[6] = SysConstants.CODE_ZNACHENIE_NE;

					Set<Integer> set = new HashSet<>();
					set.add(rows.size());
					docIndex.put(docId, set);

					rows.add(row);
					docData.put(docId, rnDocDate);
				}
			}

			if (!docData.isEmpty()) {
				// задачите по тези документи
				temp = JPA.getUtil().getEntityManager().createNativeQuery( //
					"select t.task_id, t.rn_task, t.date_reg, t.doc_id, t.task_type, t.status, t.status_user_id, r.code_ref, t.end_opinion" //
						+ " from task t left outer join task_referents r on r.task_id = t.task_id and r.role_ref = ?1 where t.doc_id in (?2) order by t.task_id") //
					.setParameter(1, OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG).setParameter(2, docData.keySet()).getResultList();
				for (Object[] t : temp) {
					Object[] row = new Object[8];

					Integer taskId = ((Number) t[0]).intValue();
					Integer docId = ((Number) t[3]).intValue();

					row[0] = OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK;
					row[1] = taskId;
					row[2] = docData.get(docId);
					row[3] = t[2];
					row[7] = docId;

					StringBuilder info = new StringBuilder();
					info.append(sd.decodeItem(OmbConstants.CODE_CLASSIF_TASK_VID, ((Number) t[4]).intValue(), SysConstants.CODE_DEFAULT_LANG, null));
					if (t[7] != null) {
						info.append(" насочен към " + sd.decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, ((Number) t[7]).intValue(), SysConstants.CODE_DEFAULT_LANG, (Date) t[2]));
					}
					info.append(", статус: " + sd.decodeItem(OmbConstants.CODE_CLASSIF_TASK_STATUS, ((Number) t[5]).intValue(), SysConstants.CODE_DEFAULT_LANG, null));
					if (t[6] != null && !t[6].equals(t[7])) {
						info.append(" определен от " + sd.decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, ((Number) t[6]).intValue(), SysConstants.CODE_DEFAULT_LANG, (Date) t[2]));
					}
					if (t[8] != null) {
						info.append(", мнение: " + sd.decodeItem(OmbConstants.CODE_CLASSIF_TASK_OPINION, ((Number) t[8]).intValue(), SysConstants.CODE_DEFAULT_LANG, null));
					}
					row[4] = info.toString();

					row[5] = new ArrayList<>();
					row[6] = SysConstants.CODE_ZNACHENIE_NE;

					if (docId.equals(initDoc)) { // задача по жалбата и трябва да вземе нейните файлове
						row[5] = result.get(0)[5];
						row[6] = result.get(0)[6];

					} else if (docIndex.containsKey(docId)) { // ще се изчислява през достъпи
						docIndex.get(docId).add(rows.size());
					}

					rows.add(row);
				}

				// движенията по тези документи
				temp = JPA.getUtil().getEntityManager().createNativeQuery( //
					"select dvij.id, dvij.dvij_text, dvij.dvij_date, dvij.doc_id, dvij.dvij_method, d.doc_type, d.doc_vid, d.code_ref_corresp from doc_dvij dvij" //
						+ " inner join doc d on d.doc_id = dvij.doc_id where dvij.doc_id in (?1) order by dvij.dvij_date") //
					.setParameter(1, docData.keySet()).getResultList();
				for (Object[] t : temp) {
					Object[] row = new Object[8];

//					Integer dvijId = ((Number) t[0]).intValue();
					Integer docId = ((Number) t[3]).intValue();

					row[0] = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ;
					row[1] = docId;
					row[2] = docData.get(docId);
					row[3] = t[2];

					StringBuilder info = new StringBuilder();
					info.append(sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, ((Number) t[6]).intValue(), SysConstants.CODE_DEFAULT_LANG, null));
					info.append(" (" + sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_TYPE, ((Number) t[5]).intValue(), SysConstants.CODE_DEFAULT_LANG, null) + ")");
					info.append(" изпратен на " + t[1] + ", начин на изпращане: ");
					info.append(sd.decodeItem(OmbConstants.CODE_CLASSIF_DVIJ_METHOD, ((Number) t[4]).intValue(), SysConstants.CODE_DEFAULT_LANG, null));
					row[4] = info.toString();

					row[5] = new ArrayList<>();
					row[6] = SysConstants.CODE_ZNACHENIE_NE;

					if (docId.equals(initDoc)) { // движение по жалбата и трябва да вземе нейните файлове
						row[5] = result.get(0)[5];
						row[6] = result.get(0)[6];

					} else if (docIndex.containsKey(docId)) { // ще се изчислява през достъпи
						docIndex.get(docId).add(rows.size());
					}

					rows.add(row);
				}
			}

			if (!rows.isEmpty() && !docIndex.isEmpty()) { // на тези трябва да се наложи достъп за файловете
				Map<String, Object> params = new HashMap<>();

				StringBuilder from = new StringBuilder(" from doc d ");
				from.append(" left outer join file_objects fo on fo.object_id = d.doc_id and fo.object_code = :objCode ");
				from.append(" left outer join files f on f.file_id = fo.file_id ");
				params.put("objCode", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);

				StringBuilder where = new StringBuilder(" where d.doc_id in (:docIdSet) ");
				params.put("docIdSet", docIndex.keySet());

				new DocSearch().addAccessRules(where, from, params, ud, true); // налага се достъп по начина който е в справката
				Query accessQuery = JPA.getUtil().getEntityManager().createNativeQuery(" select distinct d.doc_id, f.file_id, f.filename, fo.id " + from + where + " order by d.doc_id, fo.id ");

				for (Entry<String, Object> entry : params.entrySet()) {
					accessQuery.setParameter(entry.getKey(), entry.getValue());
				}
				temp = accessQuery.getResultList();
				for (Object[] t : temp) {
					Integer docId = ((Number) t[0]).intValue();

					Set<Integer> indexSet = docIndex.get(docId);
					if (indexSet != null) {
						for (Integer index : indexSet) {
							if (t[1] != null) {
								((List<Object[]>) rows.get(index)[5]).add(new Object[] { t[1], t[2] });
							}
							rows.get(index)[6] = SysConstants.CODE_ZNACHENIE_DA; // нека този да има достъп
						}
					}
				}
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при изпълнение на справка!", e);
		}

		// сглобявам и сортирам
		rows.sort((Object[] o1, Object[] o2) -> ((Date) o1[3]).compareTo((Date) o2[3]));
		result.addAll(rows);
		return result;
	}

	/** @param codeExpert the codeExpert to set */
	public void setCodeExpert(Integer codeExpert) {
		this.codeExpert = codeExpert;
	}

	/** @param codeRefNar the codeRefNar to set */
	public void setCodeRefNar(Integer codeRefNar) {
		this.codeRefNar = codeRefNar;
	}

	/** @param codeZveno the codeZveno to set */
	public void setCodeZveno(Integer codeZveno) {
		this.codeZveno = codeZveno;
	}

	/** @param corruption the corruption to set */
	public void setCorruption(Integer corruption) {
		this.corruption = corruption;
	}

	/** @param dateNarFrom the dateNarFrom to set */
	public void setDateNarFrom(Date dateNarFrom) {
		this.dateNarFrom = dateNarFrom;
	}

	/** @param dateNarTo the dateNarTo to set */
	public void setDateNarTo(Date dateNarTo) {
		this.dateNarTo = dateNarTo;
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

	/** @param dopust the dopust to set */
	public void setDopust(Integer dopust) {
		this.dopust = dopust;
	}

	/** @param finMethod the finMethod to set */
	public void setFinMethod(Integer finMethod) {
		this.finMethod = finMethod;
	}

	/** @param jalbaText the jalbaText to set */
	public void setJalbaText(String jalbaText) {
		this.jalbaText = jalbaText;
	}

	/** @param jbpEgn the jbpEgn to set */
	public void setJbpEgn(String jbpEgn) {
		this.jbpEgn = jbpEgn;
	}

	/** @param jbpEik the jbpEik to set */
	public void setJbpEik(String jbpEik) {
		this.jbpEik = jbpEik;
	}

	/** @param jbpEkatte the jbpEkatte to set */
	public void setJbpEkatte(Integer jbpEkatte) {
		this.jbpEkatte = jbpEkatte;
	}

	/** @param jbpEmail the jbpEmail to set */
	public void setJbpEmail(String jbpEmail) {
		this.jbpEmail = jbpEmail;
	}

	/** @param jbpGrj the jbpGrj to set */
	public void setJbpGrj(Integer jbpGrj) {
		this.jbpGrj = jbpGrj;
	}

	/** @param jbpHidden the jbpHidden to set */
	public void setJbpHidden(Integer jbpHidden) {
		this.jbpHidden = jbpHidden;
	}

	/** @param jbpLnc the jbpLnc to set */
	public void setJbpLnc(String jbpLnc) {
		this.jbpLnc = jbpLnc;
	}

	/** @param jbpName the jbpName to set */
	public void setJbpName(String jbpName) {
		this.jbpName = jbpName;
	}

	/** @param jbpPhone the jbpPhone to set */
	public void setJbpPhone(String jbpPhone) {
		this.jbpPhone = jbpPhone;
	}

	/** @param jbpType the jbpType to set */
	public void setJbpType(Integer jbpType) {
		this.jbpType = jbpType;
	}

	/** @param katNar the katNar to set */
	public void setKatNar(Integer katNar) {
		this.katNar = katNar;
	}

	/** @param nameNar the nameNar to set */
	public void setNameNar(String nameNar) {
		this.nameNar = nameNar;
	}

	/** @param osnNedopust the osnNedopust to set */
	public void setOsnNedopust(Integer osnNedopust) {
		this.osnNedopust = osnNedopust;
	}

	/** @param otnosno the otnosno to set */
	public void setOtnosno(String otnosno) {
		this.otnosno = otnosno;
	}

	/** @param publicVisible the publicVisible to set */
	public void setPublicVisible(Integer publicVisible) {
		this.publicVisible = publicVisible;
	}

	/** @param requestText the requestText to set */
	public void setRequestText(String requestText) {
		this.requestText = requestText;
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

	/** @param submitDateFrom the submitDateFrom to set */
	public void setSubmitDateFrom(Date submitDateFrom) {
		this.submitDateFrom = submitDateFrom;
	}

	/** @param submitDateTo the submitDateTo to set */
	public void setSubmitDateTo(Date submitDateTo) {
		this.submitDateTo = submitDateTo;
	}

	/** @param submitMethodList the submitMethodList to set */
	public void setSubmitMethodList(List<Integer> submitMethodList) {
		this.submitMethodList = submitMethodList;
	}

	/** @param textSubject the textSubject to set */
	public void setTextSubject(String textSubject) {
		this.textSubject = textSubject;
	}

	/** @param vidNar the vidNar to set */
	public void setVidNar(Integer vidNar) {
		this.vidNar = vidNar;
	}

	/** @param vidOpl the vidOpl to set */
	public void setVidOpl(Integer vidOpl) {
		this.vidOpl = vidOpl;
	}

	/** @param vidResult the vidResult to set */
	public void setVidResult(Integer vidResult) {
		this.vidResult = vidResult;
	}

	/** @param zasPrava the zasPrava to set */
	public void setZasPrava(Integer zasPrava) {
		this.zasPrava = zasPrava;
	}

	/**
	 * налагане на достъп
	 */
	private void addAccessRules(StringBuilder where, Map<String, Object> params, UserData ud) {
		if (ud.hasAccess(CODE_CLASSIF_DEF_PRAVA, CODE_ZNACHENIE_DEF_PRAVA_JALBA_FULL_EDIT)
			|| ud.hasAccess(Constants.CODE_CLASSIF_BUSINESS_ROLE, OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_DELOVODITEL)) {
			return; // пълен или деловодител имат достъп до всичко
		}

		where.append(" and (j.code_expert = :userAccess or d.user_reg = :userAccess ");
		params.put("userAccess", ud.getUserAccess());

		if (ud.getAccessZvenoList() != null) { // ръководител на звено
			where.append(" or j.code_zveno = :userZveno ");
			params.put("userZveno", ud.getZveno());
		}

		where.append(" ) ");
	}
}

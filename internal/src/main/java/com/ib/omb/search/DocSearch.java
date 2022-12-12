package com.ib.omb.search;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG;
import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.system.utils.SearchUtils.trimToNULL;
import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Query;
import javax.xml.bind.annotation.XmlTransient;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.components.CompDocSearch;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dto.DocOcr;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.BaseUserData;
import com.ib.system.SysConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.utils.DateUtils;

/**
 * Търсене на документи
 *
 * @author belev
 */
public class DocSearch extends SelectMetadata implements AuditExt {
	
	/**  */
	private static final String AND = " and ";
	/**  */
	private static final String WHERE = " where ";

	/**  */
	private static final long serialVersionUID = -8747997444646042641L;

	@JournalAttr(label="registraturaId",defaultText = "Регистратура",classifID = ""+OmbConstants.CODE_CLASSIF_REGISTRATURI)
	private Integer			registraturaId;
	
	@JournalAttr(label="registerId",defaultText = "Регистър",classifID = ""+OmbConstants.CODE_CLASSIF_REGISTRI)
	private Integer			registerId;
	
	@JournalAttr(label="registerIdList",defaultText = "Регистър",classifID = ""+OmbConstants.CODE_CLASSIF_REGISTRI)
	private List<Integer>	registerIdList;			// за множествено по регистър
	
	@JournalAttr(label="sendRegistraturaList",defaultText = "Регистратура изпращач",classifID = ""+OmbConstants.CODE_CLASSIF_REGISTRATURI)
	private List<Integer>	sendRegistraturaList; 	// търсене на документи по регистратура изпращач

	@JournalAttr(label="userReg",defaultText = "Потребител, регистрирал документа",classifID = ""+OmbConstants.CODE_CLASSIF_USERS)
	private Integer userReg;

	@JournalAttr(label="codeRefCorresp",defaultText = "Кореспондент",classifID = ""+OmbConstants.CODE_CLASSIF_REFERENTS)
	private Integer	codeRefCorresp;	// по код от класификацията
	
	@JournalAttr(label="nameCorresp",defaultText = "Име на кореспондент")
	private String	nameCorresp;	// по въведено част от името

	@JournalAttr(label="docName",defaultText = "Наименование на документ")
	private String	docName;
	
	@JournalAttr(label="docInfo",defaultText = "Допълнителна информация")
	private String	docInfo;
	
	@JournalAttr(label="otnosno",defaultText = "Относно")
	private String	otnosno;

	@JournalAttr(label="rnDoc",defaultText = "Регистрационен номер")
	private String	rnDoc;
	
	@JournalAttr(label="rnDocEQ",defaultText = "Регистрационен номер (пълно съвпадение)")
	private boolean	rnDocEQ = true;	// ако е true се търси по пълно съвпадение по номер на документ

	@JournalAttr(label="tehNomer",defaultText = "Техен номер")
	private String	tehNomer;	// техен номер
	
	@JournalAttr(label="tehNomerEQ",defaultText = "Техен номер (пълно съвпадение)")
	private boolean	tehNomerEQ;	// ако е true се търси по пълно съвпадение

	@JournalAttr(label="guid",defaultText = "GUID")
	private String guid;

	@JournalAttr(label="docTypeEditList",defaultText = "Тип на документа", classifID = ""+ OmbConstants.CODE_CLASSIF_DOC_TYPE)
	private List<Integer>	docTypeEditList; // списък с позволени за актуализация, като ще се използва той ако нищо не е избрано
	@JournalAttr(label="docTypeArr",defaultText = "Тип на документа", classifID = ""+ OmbConstants.CODE_CLASSIF_DOC_TYPE)
	private Integer[]		docTypeArr	= new Integer[3];	// за да се връзва директно за компоненатата
	
	@JournalAttr(label="docVidList",defaultText = "Вид на документа", classifID = ""+ OmbConstants.CODE_CLASSIF_DOC_VID)
	private List<Integer>	docVidList;
	
	@JournalAttr(label="docTopicList",defaultText = "Тематика", classifID = ""+ OmbConstants.CODE_CLASSIF_DOC_TOPIC)
	private List<Integer>	docTopicList;

	@JournalAttr(label="validArr",defaultText = "Валидност", classifID = ""+ OmbConstants.CODE_CLASSIF_DOC_VALID)
	private Integer[]	validArr;	// валидност
	
	@JournalAttr(label="urgentArr",defaultText = "Спешност", classifID = ""+ OmbConstants.CODE_CLASSIF_URGENT)
	private Integer[]	urgentArr;	// спешен
	
	@JournalAttr(label="statusArr",defaultText = "Статус", classifID = ""+ OmbConstants.CODE_CLASSIF_DOC_STATUS)
	private Integer[]	statusArr;	// статус

	@JournalAttr(label="competence",defaultText = "Компетентност", classifID = ""+ OmbConstants.CODE_CLASSIF_COMPETENCE)
	private Integer 	competence; // компететност
	
	@JournalAttr(label="competence",defaultText = "Компетентност", classifID = ""+ OmbConstants.CODE_CLASSIF_DOC_IRREGULAR)
	private Integer[] 	irregularArr; // причини за нередовност

	
	// период на дата на документа
	@JournalAttr(label="docDateFrom",defaultText = "Дата на документ - от")
	private Date	docDateFrom;
	
	@JournalAttr(label="docDateTo",defaultText = "Дата на документ - до")
	private Date	docDateTo;

	@JournalAttr(label="receivedBy",defaultText = "Получен доп.информация")
	private String		receivedBy;

	// период на дата на получаване
	@JournalAttr(label="receiveDateFrom",defaultText = "Дата на получаване - от")
	private Date		receiveDateFrom;
	
	@JournalAttr(label="receiveDateTo",defaultText = "Дата на получаване - до")
	private Date		receiveDateTo;
	
	@JournalAttr(label="receiveMethodArr",defaultText = " Начин на получаване",classifID = "112")
	private Integer[]	receiveMethodArr;

	// период на дата на очакване на отговор
	@JournalAttr(label="waitAnswerDateFrom",defaultText = "Дата на очакване на отговор - от")
	private Date	waitAnswerDateFrom;
	
	@JournalAttr(label="waitAnswerDateTo",defaultText = "Дата на очакване на отговор - до")
	private Date	waitAnswerDateTo;

	// период на дата на поставяне
	@JournalAttr(label="taskDateFrom",defaultText = "Дата на поставяне")
	private Date			taskDateFrom;
	
	@JournalAttr(label="taskDateTo",defaultText = "Дата на поставяне")
	private Date			taskDateTo;
	
	
	@JournalAttr(label="taskReferentList",defaultText = "Документи с поставена задача за",classifID = ""+OmbConstants.CODE_CLASSIF_ADMIN_STR)
	private List<Integer>	taskReferentList;	// докмент с поставена задача на

	@JournalAttr(label="docReferent",defaultText = "Служител, работил по документа",classifID = ""+OmbConstants.CODE_CLASSIF_ADMIN_STR)
	private Integer		docReferent;		// служител
	
	@JournalAttr(label="docReferentRoles",defaultText = "Роля на служител в документ",classifID = ""+OmbConstants.CODE_CLASSIF_DOC_REF_ROLE)
	private Integer[]	docReferentRoles;	// роли (автор,съгласувал,подписал)

	@JournalAttr(label="rnDelo",defaultText = "Номер на дело")
	private String	rnDelo;		// документите да са в подадената преписка
	
	@JournalAttr(label="deloYear",defaultText = "Година на дело")
	private Integer	deloYear;	// за конкретната година

	@JournalAttr(label="nullWorkOffId",defaultText = "Да не са регистрирани като официални")
	private Boolean	nullWorkOffId;	// при TRUE се геда да няма стойност в полето, при FALSE се гледа да има стойност в полето,
									// ако е NULL нищо не се гледа
	
	@JournalAttr(label="forRegId",defaultText = "Регистратура за регистрация",classifID = ""+OmbConstants.CODE_CLASSIF_REGISTRATURI)
	private Integer	forRegId;		// избрана регистратура за регистрация

	private Integer	notInDocId;		// ако има нещо се смята, че намерените документи не трябва да са във връзка DOC_DOC с този
									// док, както notInDocId да не се върне в резултата
	private Integer	notInDeloId;	// ако има нещо се смята, че намерените документи не трябва да са във връзка DELO_DOC с това
									// дело
	private Integer	markRelDocId;	// ако е подадено нещо се маркра(ДА/НЕ) дали подадения док е във връзка с намерените през таблица DOC_DOC
	
	private Integer notInProtocolId; // ако има нещо се смята, че намерените документи не трябва да са във връзка DOC_DESTRUCT с
										// този протокол
	@JournalAttr(label="notIncluded",defaultText = "Да не е вложен")
	private Boolean notIncluded; // true-не е вложен никъде
	
	@JournalAttr(label="fileText",defaultText = "Текст на документа")
	private String	fileText; //текст на документа

	// данни за търсене при отнемане и изтриване на отнемане на достъп
	private Serializable[] checkUserAccess; // [0]-codeRef
								 			// [1]-zveno
											// [2]=null- има достъп, [2]!=null- отнет достъп
											// [3]-ако е е на ръководна длъжност списък със звената, до които има достъп "2,4,9"

	@JournalAttr(label="dvijNotReturned",defaultText = "Предадени документи, които не са върнати")
	private Boolean dvijNotReturned; 	// Предадени документи, които не са върнати
	@JournalAttr(label="dvijToText",defaultText = "Предадени на")
	private String 	dvijToText; 		// Предадени на

	private Boolean updWorkRegOffForbidden; // забранено е да се актуализират работни документи, които са регистрирани като официални

		
	@JournalAttr(label="docReferent",defaultText = "Участник в документ")
	private String			docMemberName;
	@JournalAttr(label="docReferentRoles",defaultText = "Роля на участник в документ",classifID = ""+OmbConstants.CODE_CLASSIF_DOC_MEMBER_ROLES)
	private List<Integer>	docMemberRoleList;

	@JournalAttr(label="useDost",defaultText = "Прилага ли се достъп")
	private boolean useDost = true;   // Дали да се добавят условия за достъп
	
	
	/** ако флага е вдигнат се изключват от търсенето документите жалба,нпм,самосезиране */
	private boolean excludeSpecDocs = true;
	
	
	private StringBuilder sqlSelect ;
	private StringBuilder sqlFrom ;
	private StringBuilder sqlWhere ;
	private Map<String, Object> sqlParams;
	
	//Това са малко изкуствени атрибути, за да не се преписва рефлекшъна за супер класовете
	
	// като са с еднакви имена няма да го има 2 пъти в xml-a
	@JournalAttr(label="sql",defaultText = "SQL зявка")
	private String	sql;
	@JournalAttr(label="sqlCount",defaultText = "SQL зявка за брой")
	private String	sqlCount;
	
	/** */
	public DocSearch() {
		super();
	}

	/** */
	@Override
	public void setSql(String sql) {
		this.sql = sql;
		super.setSql(sql);
	}
	/** */
	@Override
	public void setSqlCount(String sqlCount) {
		this.sqlCount = sqlCount;
		super.setSqlCount(sqlCount);
	}
	

	/**
	 * Винаги се търси в контекста на регистратура. Ако все пак се иска без регистратура да се подаде NULL в конструктора
	 *
	 * @param registraturaId
	 */
	public DocSearch(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/**
	 * Търсене което се използва от компонентата {@link CompDocSearch}<br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида:<br>
	 * [0]-DOC_ID<br>
	 * [1]-RN_DOC<br>
	 * [2]-DOC_TYPE<br>
	 * [3]-DOC_VID<br>
	 * [4]-DOC_DATE<br>
	 * [5]-OTNOSNO (String)<br>
	 * [6]-VALID<br>
	 * [7]-TEH_NOMER<br>
	 * [8]-TEH_DATE<br>
	 * [9]-PROCESSED<br>
	 * [10]-CODE_REF_CORRESP<br>
	 * [11]-IF (this.markRelDocId==null) -> NULL		ELSE -> изчислено ДА/НЕ
	 * @param userData това е този които изпълнява търсенето
	 */
	public void buildQueryComp(BaseUserData userData) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		String distinct = this.useDost ? "distinct" : ""; // само ако има достъп, защото там може да го размножи
		
		select.append("select "+distinct+" d.DOC_ID a0, "+DocDAO.formRnDocSelect("d.", dialect)+" a1, d.DOC_TYPE a2, d.DOC_VID a3, d.DOC_DATE a4, ");
		select.append(DialectConstructor.limitBigString(dialect, "d.OTNOSNO", 300) + " a5, d.VALID a6, d.TEH_NOMER a7, d.TEH_DATE a8, d.PROCESSED a9, d.CODE_REF_CORRESP a10 "); // max 300!
		
		if (this.markRelDocId == null) {
			select.append(" , null a11 "); // няма смисъл нищо повече да се прави
		} else {
			select.append(" , case when EXISTS ");
			select.append(" (select dd.ID from DOC_DOC dd where (dd.DOC_ID1 = "+this.markRelDocId+" and dd.DOC_ID2 = d.DOC_ID) or (dd.DOC_ID1 = d.DOC_ID and dd.DOC_ID2 = "+this.markRelDocId+") ) ");
			select.append(" then " + SysConstants.CODE_ZNACHENIE_DA);
			select.append(" else " + SysConstants.CODE_ZNACHENIE_NE);
			select.append(" end a11 ");
		}
		
		from.append(" from DOC d ");

//		if (this.registraturaId != null) {
//			where.append((where.length() == 0 ? WHERE : AND) + " d.REGISTRATURA_ID = :registraturaId ");
//			params.put("registraturaId", this.registraturaId);
//		}

		String t = trimToNULL_Upper(this.rnDoc);
		if (t != null) {
			if (this.rnDocEQ) { // пълно съвпадение case insensitive
				where.append((where.length() == 0 ? WHERE : AND) + " upper(d.RN_DOC) = :rnDoc ");
				params.put("rnDoc", t);

			} else {
				where.append((where.length() == 0 ? WHERE : AND) + " upper(d.RN_DOC) like :rnDoc ");
				params.put("rnDoc", "%" + t + "%");
			}
		}

		t = trimToNULL_Upper(this.tehNomer);
		if (t != null) {
			if (this.tehNomerEQ) { // пълно съвпадение
				where.append((where.length() == 0 ? WHERE : AND) + " upper(d.TEH_NOMER) = :tehNomer ");
				params.put("tehNomer", t);

			} else {
				where.append((where.length() == 0 ? WHERE : AND) + " upper(d.TEH_NOMER) like :tehNomer ");
				params.put("tehNomer", "%" + t + "%");
			}
		}

		if (this.markRelDocId != null) { // този трябва да не се връща
			where.append(" and d.DOC_ID != :markRelDocId ");
			params.put("markRelDocId", this.markRelDocId);
		}

		if (this.codeRefCorresp != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.CODE_REF_CORRESP = :codeRefCorresp ");
			params.put("codeRefCorresp", this.codeRefCorresp);
		}

		if (this.docTypeArr != null && this.docTypeArr.length != 0 && this.docTypeArr.length < 3) { // да има нещо но да не са
																									// всички
			where.append((where.length() == 0 ? WHERE : AND) + " d.DOC_TYPE in (:docTypeList) ");
			params.put("docTypeList", Arrays.asList(this.docTypeArr));
		}
		if (this.docVidList != null && !this.docVidList.isEmpty()) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DOC_VID in (:docVidList) ");
			params.put("docVidList", this.docVidList);
		}

		if (this.docDateFrom != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DOC_DATE >= :docDateFrom ");
			params.put("docDateFrom", DateUtils.startDate(this.docDateFrom));
		}
		if (this.docDateTo != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DOC_DATE <= :docDateTo ");
			params.put("docDateTo", DateUtils.endDate(this.docDateTo));
		}

		t = trimToNULL_Upper(this.otnosno);
		if (t != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " upper(d.OTNOSNO) like :otnosno ");
			params.put("otnosno", "%" + t + "%");
		}

		if (this.notInDeloId != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " NOT EXISTS ");
			where.append(" (select dd.ID from DELO_DOC dd where dd.DELO_ID = :notInDeloId and dd.INPUT_DOC_ID = d.DOC_ID ) ");
			params.put("notInDeloId", this.notInDeloId);
		}
		if (this.notInDocId != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DOC_ID != :notInDocId ");

			where.append(" and NOT EXISTS ");
			where.append(" (select dd.ID from DOC_DOC dd where (dd.DOC_ID1 = :notInDocId and dd.DOC_ID2 = d.DOC_ID) or (dd.DOC_ID1 = d.DOC_ID and dd.DOC_ID2 = :notInDocId) ) ");
			params.put("notInDocId", this.notInDocId);
		}

		if (this.notInProtocolId != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " NOT EXISTS ");
			where.append(" (select dd.ID from DOC_DESTRUCT dd where dd.PROTOCOL_ID = :notInProtocolId and dd.DOC_ID = d.DOC_ID ) ");
			params.put("notInProtocolId", this.notInProtocolId);
		}

		if (Boolean.TRUE.equals(this.nullWorkOffId)) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.WORK_OFF_ID is null ");
		}
		
		if (this.validArr != null && this.validArr.length != 0) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.VALID in (:validList) ");
			params.put("validList", Arrays.asList(this.validArr));
		}

		t = trimToNULL_Upper(this.rnDelo);
		if (t != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " EXISTS ( ");
			where.append(" select dd.ID from DELO d inner join DELO_DOC dd on dd.DELO_ID = d.DELO_ID ");
			where.append(" where upper(d.RN_DELO) = :rnDelo and dd.INPUT_DOC_ID = d.DOC_ID ");
			if (this.deloYear != null) {
				where.append(" and d.DELO_YEAR = :deloYear ");
				params.put("deloYear", this.deloYear);
			}
			where.append(" ) ");
			params.put("rnDelo", t);
		}

		if (useDost) {
			addAccessRules(where, from, params, (UserData)userData, true);
		}
		
		setSql(select.toString() + from.toString() + where.toString());
		setSqlCount(" select count("+distinct+" d.DOC_ID) " + from.toString() + where.toString());
		setSqlParameters(params);
		
	}

	/**
	 * Използва се от основния екран за търсене на документи <br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида:<br>
	 * [0]-DOC_ID (DVIJ_ID - само ако this.dvijNotReturned==true)<br>
	 * [1]-RN_DOC<br>
	 * [2]-DOC_TYPE<br>
	 * [3]-DOC_VID<br>
	 * [4]-DOC_DATE<br>
	 * [5]-REGISTER_ID<br>
	 * [6]-OTNOSNO (String)<br>
	 * [7]-URGENT<br>
	 * [8]-CODE_REF_CORRESP<br>
	 * [9]-AUTHORS_CODES-кодовете на авторите с разделител запетая - пример (1,6,18)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date)}<br>
	 * [10]-LOCK_USER<br>
	 * [11]-LOCK_DATE<br>
	 * [12]-COUNT_FILES<br>
	 * [13]-OTHER.REGISTRATURA_ID<br>
	 * [14]-DVIJ_METHOD - само ако this.dvijNotReturned==true<br>
	 * [15]-DVIJ_TEXT 	- само ако this.dvijNotReturned==true<br>
	 * [16]-DVIJ_DATE 	- само ако this.dvijNotReturned==true<br>
	 * [17]-DOC_ID 		- само ако this.dvijNotReturned==true<br>
	 * [18]-EKZ_NOMER 	- само ако this.dvijNotReturned==true<br>
	 *
	 * @param userData това е този които изпълнява търсенето
	 * @param viewMode
	 */
	public void buildQueryDocList(BaseUserData userData, boolean viewMode) {
		buildQueryDocListInner(userData, viewMode, false);
	}

	/**
	 * @see DocSearch#buildQueryDocList(BaseUserData, boolean)
	 * @param userData
	 * @param viewMode
	 */
	private void buildQueryDocListInner(BaseUserData userData, boolean viewMode, boolean isFullText) {
		String dialect = JPA.getUtil().getDbVendorName();

		sqlParams = new HashMap<>();

		sqlFrom = new StringBuilder();
		sqlSelect = new StringBuilder();
		sqlWhere = new StringBuilder();

		boolean joinLock = !viewMode; // само за актуализация
		
		String firstCol = "d.DOC_ID";
		if (Boolean.TRUE.equals(this.dvijNotReturned)) {
			firstCol = "dvij.ID";
		}

		sqlSelect.append("select distinct "+firstCol+" a0, "+DocDAO.formRnDocSelect("d.", dialect)+" a1, d.DOC_TYPE a2, d.DOC_VID a3, d.DOC_DATE a4, d.REGISTER_ID a5, ");
		sqlSelect.append(DialectConstructor.limitBigString(dialect, "d.OTNOSNO", 300) + " a6 "); // max 300!
		sqlSelect.append(" , d.URGENT a7, d.CODE_REF_CORRESP a8 ");

		sqlSelect.append(" , CAST (avtor.CODE_REF AS VARCHAR) a9 "); // ще се вади само първия, но остава пак към стринг, за да не се чупи логиката
//		sqlSelect.append(" , CASE WHEN d.DOC_TYPE = 1 THEN null ELSE "); // за входящите авторите не се теглят
//		sqlSelect.append(DialectConstructor.convertToDelimitedString(dialect, "dr.CODE_REF", "DOC_REFERENTS dr where dr.DOC_ID = d.DOC_ID and dr.ROLE_REF = 1", "dr.PORED"));
//		sqlSelect.append(" END a9 ");
		sqlSelect.append(", "+ (joinLock ? "z.user_id" : "null") +" a10, "+ (joinLock ? "z.lock_date" : "null") +" a11, d.COUNT_FILES a12, null a13 ");

		if (Boolean.TRUE.equals(this.dvijNotReturned)) {
			sqlSelect.append(" , dvij.DVIJ_METHOD a14, dvij.DVIJ_TEXT a15, dvij.DVIJ_DATE a16, d.DOC_ID a17, dvij.EKZ_NOMER a18 ");
		}

		sqlFrom.append(" from DOC d ");

		if (this.excludeSpecDocs) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DOC_VID not in (:excludeSpecDocs) ");
			sqlParams.put("excludeSpecDocs", Arrays.asList(OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA, OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM, OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS));
		}
	
		
		String t = trimToNULL_Upper(this.rnDoc);
		if (t != null) {
			if (this.rnDocEQ) { // пълно съвпадение case insensitive
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.RN_DOC) = :rnDoc ");
				sqlParams.put("rnDoc", t);

			} else {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.RN_DOC) like :rnDoc ");
				sqlParams.put("rnDoc", "%" + t + "%");
			}
		}

//		if (this.registraturaId != null) {
//			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.REGISTRATURA_ID = :registraturaId ");
//			sqlParams.put("registraturaId", this.registraturaId);
//		}
		if (this.registerId != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.REGISTER_ID = :registerId ");
			sqlParams.put("registerId", this.registerId);
		}
		if (this.registerIdList != null && !this.registerIdList.isEmpty()) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.REGISTER_ID in (:registerIdList) ");
			sqlParams.put("registerIdList", this.registerIdList);
		}

		if (this.docTypeArr != null && this.docTypeArr.length != 0 && this.docTypeArr.length < 3) { // да има нещо но да не са
																									// всички
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DOC_TYPE in (:docTypeList) ");
			sqlParams.put("docTypeList", Arrays.asList(this.docTypeArr));

		} else if (this.docTypeEditList != null && !this.docTypeEditList.isEmpty()) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DOC_TYPE in (:docTypeList) ");
			sqlParams.put("docTypeList", this.docTypeEditList);
		}

		if (this.docVidList != null && !this.docVidList.isEmpty()) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DOC_VID in (:docVidList) ");
			sqlParams.put("docVidList", this.docVidList);
		}
		if (this.docTopicList != null && !this.docTopicList.isEmpty()) {
			sqlFrom.append(" inner join DOC_TOPIC dt on dt.DOC_ID = d.DOC_ID and dt.TOPIC in (:docTopicList) ");
			sqlParams.put("docTopicList", this.docTopicList);
		}
		if (this.validArr != null && this.validArr.length != 0) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.VALID in (:validList) ");
			sqlParams.put("validList", Arrays.asList(this.validArr));
		}
		if (this.urgentArr != null && this.urgentArr.length != 0) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.URGENT in (:urgentList) ");
			sqlParams.put("urgentList", Arrays.asList(this.urgentArr));
		}
		if (this.statusArr != null && this.statusArr.length != 0) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.STATUS in (:statusList) ");
			sqlParams.put("statusList", Arrays.asList(this.statusArr));
		}

		if (this.competence != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.COMPETENCE = :competence ");
			sqlParams.put("competence", this.competence);
		}
		if (this.irregularArr != null && this.irregularArr.length != 0) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.IRREGULAR in (:irregularList) ");
			sqlParams.put("irregularList", Arrays.asList(this.irregularArr));
		}

		if (this.userReg != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.USER_REG = :userReg ");
			sqlParams.put("userReg", this.userReg);
		}

		t = trimToNULL_Upper(this.docName);
		if (t != null&&!isFullText) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.DOC_NAME) like :docName ");
			sqlParams.put("docName", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.otnosno);
		if (t != null && !isFullText) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.OTNOSNO) like :otnosno ");
			sqlParams.put("otnosno", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.docInfo);
		if (t != null&&!isFullText) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.DOC_INFO) like :docInfo ");
			sqlParams.put("docInfo", "%" + t + "%");
		}

		t = trimToNULL_Upper(this.tehNomer);
		if (t != null) {
			if (this.tehNomerEQ) { // пълно съвпадение
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.TEH_NOMER) = :tehNomer ");
				sqlParams.put("tehNomer", t);

			} else {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.TEH_NOMER) like :tehNomer ");
				sqlParams.put("tehNomer", "%" + t + "%");
			}
		}

		t = trimToNULL_Upper(this.guid);
		if (t != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.GUID) = :guid ");
			sqlParams.put("guid", t);
		}

		if (this.docDateFrom != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DOC_DATE >= :docDateFrom ");
			sqlParams.put("docDateFrom", DateUtils.startDate(this.docDateFrom));
		}
		if (this.docDateTo != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DOC_DATE <= :docDateTo ");
			sqlParams.put("docDateTo", DateUtils.endDate(this.docDateTo));
		}

		t = trimToNULL_Upper(this.receivedBy);
		if (t != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.RECEIVED_BY) like :receivedBy ");
			sqlParams.put("receivedBy", "%" + t + "%");
		}
		
		if (this.receiveDateFrom != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.RECEIVE_DATE >= :receiveDateFrom ");
			sqlParams.put("receiveDateFrom", DateUtils.startDate(this.receiveDateFrom));
		}
		if (this.receiveDateTo != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.RECEIVE_DATE <= :receiveDateTo ");
			sqlParams.put("receiveDateTo", DateUtils.endDate(this.receiveDateTo));
		}

		if (this.waitAnswerDateFrom != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.WAIT_ANSWER_DATE >= :waitAnswerDateFrom ");
			sqlParams.put("waitAnswerDateFrom", DateUtils.startDate(this.waitAnswerDateFrom));
		}
		if (this.waitAnswerDateTo != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.WAIT_ANSWER_DATE <= :waitAnswerDateTo ");
			sqlParams.put("waitAnswerDateTo", DateUtils.endDate(this.waitAnswerDateTo));
		}

		if (this.nullWorkOffId != null) {
			if (this.nullWorkOffId.booleanValue()) {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " (d.WORK_OFF_ID is null or d.DOC_TYPE != :docTypeWrk) ");
			} else {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " (d.WORK_OFF_ID is not null or d.DOC_TYPE != :docTypeWrk) ");
			}
			sqlParams.put("docTypeWrk", OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK);
		}
		if (this.forRegId != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.FOR_REG_ID = :forRegId ");
			sqlParams.put("forRegId", this.forRegId);
		}

		if (this.codeRefCorresp != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.CODE_REF_CORRESP = :codeRefCorresp ");
			sqlParams.put("codeRefCorresp", this.codeRefCorresp);
		}
		t = trimToNULL_Upper(this.nameCorresp);
		if (t != null) {
			sqlFrom.append(" inner join ADM_REFERENTS r on r.CODE = d.CODE_REF_CORRESP and upper(r.REF_NAME) like :nameCorresp ");
			sqlParams.put("nameCorresp", "%" + t + "%");
		}

		if (this.receiveMethodArr != null && this.receiveMethodArr.length != 0) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.RECEIVE_METHOD in (:receiveMethodList) ");
			sqlParams.put("receiveMethodList", Arrays.asList(this.receiveMethodArr));
		}

		if (Boolean.TRUE.equals(this.notIncluded)) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " NOT EXISTS ");
			sqlWhere.append(" (select dd.ID from DELO_DOC dd where dd.INPUT_DOC_ID = d.DOC_ID ) ");
		}

		boolean joinTask = false;
		if (this.taskReferentList != null && !this.taskReferentList.isEmpty()) {
			joinTask = true;

			sqlFrom.append(" inner join TASK t on t.DOC_ID = d.DOC_ID ");
			sqlFrom.append(" inner join TASK_REFERENTS tr on tr.TASK_ID = t.TASK_ID ");

			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " tr.CODE_REF in (:taskReferentList) and tr.ROLE_REF in ( :roleExecOtg , :roleExec ) ");
			sqlParams.put("taskReferentList", this.taskReferentList);
			sqlParams.put("roleExecOtg", CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG);
			sqlParams.put("roleExec", CODE_ZNACHENIE_TASK_REF_ROLE_EXEC);
		}

		if (this.taskDateFrom != null) {
			if (!joinTask) {
				sqlFrom.append(" inner join TASK t on t.DOC_ID = d.DOC_ID ");
				joinTask = true;
			}
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.ASSIGN_DATE >= :taskDateFrom ");
			sqlParams.put("taskDateFrom", DateUtils.startDate(this.taskDateFrom));
		}
		if (this.taskDateTo != null) {
			if (!joinTask) {
				sqlFrom.append(" inner join TASK t on t.DOC_ID = d.DOC_ID ");
			}
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.ASSIGN_DATE <= :taskDateTo ");
			sqlParams.put("taskDateTo", DateUtils.endDate(this.taskDateTo));
		}

		if (this.docReferent != null) {

			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " EXISTS ");

			sqlWhere.append(" (select dr.ID from DOC_REFERENTS dr where dr.DOC_ID = d.DOC_ID and dr.CODE_REF = :docReferent ");
			sqlParams.put("docReferent", this.docReferent);

			if (this.docReferentRoles != null && this.docReferentRoles.length != 0 && this.docReferentRoles.length < 3) { // има и
																															// роли
				sqlWhere.append(" and dr.ROLE_REF in (:docReferentRoles) ");
				sqlParams.put("docReferentRoles", Arrays.asList(this.docReferentRoles));

			}
			sqlWhere.append(" ) "); // това затваря EXISTS !
		}

		if (!viewMode && Boolean.TRUE.equals(this.updWorkRegOffForbidden)) { // TODO може ли тука да се оптимизира само ако не е избрано нищо или е избран работен
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " (d.WORK_OFF_ID is null or d.DOC_TYPE != "+OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK+") ");
		}

		// аргументи свързани с движението
		t = trimToNULL_Upper(this.dvijToText);
		if (Boolean.TRUE.equals(this.dvijNotReturned)) {
			sqlFrom.append(" inner join DOC_DVIJ dvij on dvij.DOC_ID = d.DOC_ID and dvij.RETURN_DATE is null ");
			if (t != null) {
				sqlFrom.append(" and upper(dvij.DVIJ_TEXT) like :dvijToText ");
				sqlParams.put("dvijToText", "%" + t + "%");
			}

		} else if (t != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " EXISTS (select dvij.ID from DOC_DVIJ dvij where dvij.DOC_ID = d.DOC_ID and upper(dvij.DVIJ_TEXT) like :dvijToText) ");
			sqlParams.put("dvijToText", "%" + t + "%");
		}

		t = trimToNULL_Upper(this.docMemberName);
		if (t != null || (this.docMemberRoleList != null && !this.docMemberRoleList.isEmpty())) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " EXISTS (select dm.ID from DOC_MEMBERS dm where dm.DOC_ID = d.DOC_ID ");

			if (t != null) {
				sqlWhere.append(" and upper(dm.REF_TEXT) like :docMemberName ");
				sqlParams.put("docMemberName", "%" + t + "%");
			}
			if (this.docMemberRoleList != null && !this.docMemberRoleList.isEmpty()) {		
				sqlWhere.append(" and dm.ROLE_REF in (:docMemberRoleList) ");
				sqlParams.put("docMemberRoleList", docMemberRoleList);

			}
			sqlWhere.append(" ) "); // това затваря EXISTS !
		}

		// това се иска винаги да се прави за да се извади регистратура изпращач (other.REGISTRATURA_ID)
		// TODO може да се сложи след count (*), но само ако няма въведено нищо в this.sendRegistraturaList
//		sqlFrom.append(" left outer join DOC_DVIJ dvijother on dvijother.OTHER_DOC_ID = d.DOC_ID ");
//		sqlFrom.append(" left outer join DOC other on other.DOC_ID = dvijother.DOC_ID ");

		if (this.sendRegistraturaList != null && !this.sendRegistraturaList.isEmpty()) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " other.REGISTRATURA_ID in (:sendRegistraturaList) ");
			sqlParams.put("sendRegistraturaList", this.sendRegistraturaList);
		}

		if (this.checkUserAccess != null) {
			appendCheckUserAccessSql(sqlWhere, sqlParams);
		}

		if (useDost) {
			addAccessRules(sqlWhere, sqlFrom, sqlParams, (UserData) userData, viewMode);
		}


		super.setSqlCount(" select count(distinct "+firstCol+") " + sqlFrom.toString() + sqlWhere.toString());
		
		// долното не трябва на каунта и затова тук се слага, като идеята е да се вземе само първия автор
		sqlFrom.append(" left outer join DOC_REFERENTS avtor on d.doc_type <> 1 and avtor.DOC_ID = d.DOC_ID and avtor.ROLE_REF = 1 and avtor.PORED = 1 ");

		if (joinLock) { // за да се види има ли заключване - само в актуализация
			sqlFrom.append(" left outer join LOCK_OBJECTS z on z.OBJECT_TIP = "+OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC
				+ " and z.OBJECT_ID = d.DOC_ID and z.USER_ID != "+userData.getUserId()+" ");
//			sqlParams.put("zTip", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
//			sqlParams.put("zUser", userData.getUserId());
		}

		super.setSql(sqlSelect.toString() + sqlFrom.toString() + sqlWhere.toString());
		setSqlParameters(sqlParams);
	}

	/**
	 * Използва се от основния екран за пълнотекстово търсене на документи <br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида:<br>
	 * [0]-DOC_ID<br>
	 * [1]-RN_DOC<br>
	 * [2]-DOC_TYPE<br>
	 * [3]-DOC_VID<br>
	 * [4]-DOC_DATE<br>
	 * [5]-REGISTER_ID<br>
	 * [6]-OTNOSNO (String)<br>
	 * [7]-URGENT<br>
	 * [8]-CODE_REF_CORRESP<br>
	 * [9]-AUTHORS_CODES-кодовете на авторите с разделител запетая - пример (1,6,18)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date)}<br>
	 * @param viewMode
	 */
	public void buildQueryDocListWithFulltext(BaseUserData userData, boolean viewMode) {
		Map<String, String> searchFields = new HashMap<>();
		Map<String, Object> filterFields = new HashMap<>();

		buildQueryDocListInner(userData,viewMode, true);

		String firstCol = "d.DOC_ID";
		if (Boolean.TRUE.equals(this.dvijNotReturned)) {
			firstCol = "dvij.ID";
		}

		filterFields.put(DocOcr.FilterFields.registraturaId.name(),new HashSet());
		if (this.registraturaId != null) {
			((Collection)filterFields.get(DocOcr.FilterFields.registraturaId.name())).add(this.registraturaId);
		}

		String t = trimToNULL_Upper(this.docName);
		if (t != null) {
			searchFields.put(DocOcr.FullTextFields.docName.name(), t);
		}
		t = trimToNULL_Upper(this.fileText);
		if (t != null) {
			searchFields.put(DocOcr.FullTextFields.ocr.name(), t);
		}
		t = trimToNULL_Upper(this.otnosno);
		if (t != null) {
			searchFields.put(DocOcr.FullTextFields.otnosno.name(), t);
//			searchFields.put("ocr",t);
		}
		t = trimToNULL_Upper(this.docInfo); 
		if (t != null) {
			searchFields.put(DocOcr.FullTextFields.docInfo.name(), t);
		}

		if (this.docDateFrom != null) {
			filterFields.put(DocOcr.FilterFields.docDateOt.name(),DateUtils.startDate(this.docDateFrom));
		}
		if (this.docDateTo != null) {
			filterFields.put(DocOcr.FilterFields.docDateDo.name(),DateUtils.endDate(this.docDateTo));
		}

		if (this.receiveDateFrom != null) {
			filterFields.put(DocOcr.FilterFields.receiveDateOt.name(),DateUtils.startDate(this.receiveDateFrom));
		}
		if (this.receiveDateTo != null) {
			filterFields.put(DocOcr.FilterFields.receiveDateDo.name(),DateUtils.endDate(this.receiveDateTo));
		}

		if(sqlParams.get("viewRegistraturiSet")!=null) {
			((Collection) filterFields.get(DocOcr.FilterFields.registraturaId.name())).addAll((Collection) sqlParams.get("viewRegistraturiSet"));
		}

		if(sqlParams.get("registraturaId")!=null){
			((Collection) filterFields.get(DocOcr.FilterFields.registraturaId.name())).add(sqlParams.get("registraturaId"));
		}


		Map<Integer, Map<Integer, Boolean>> accessValues = userData.getAccessValues();
		if (accessValues!=null) {
			((Collection) filterFields.get(DocOcr.FilterFields.registraturaId.name())).addAll(accessValues.get(CODE_CLASSIF_REGISTRATURI_OBJACCESS).keySet());
		}

		if(searchFields.isEmpty()) {
			return;
		} else {
			Set<Integer> result = new DocFulltextSearch().searchFullText(filterFields, searchFields);
			Set<Integer> docIds = null;
			try {
				if (result != null && !result.isEmpty()) {
					docIds = getDocIdListSQL(result, sqlFrom.toString() + sqlWhere.toString(), sqlParams);
				}
			} catch (InvalidParameterException e) {
				JSFUtils.addErrorMessage(e.getMessage());
			}

			if (docIds != null && !docIds.isEmpty()) {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DOC_ID in (:docIdList) ");
				sqlParams.put("docIdList", docIds);
			} else {
				//зададено е условие, но NOSQL-а не е върнал резултат, тогава и SQL-а няма да върне
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DOC_ID in (-1) ");
			}
		}

		super.setSql(sqlSelect.toString() + sqlFrom.toString() + sqlWhere.toString());
		super.setSqlCount(" select count(distinct "+firstCol+") " + sqlFrom.toString() + sqlWhere.toString());
		setSqlParameters(sqlParams);


	}

	private Set<Integer> getDocIdListSQL(Set<Integer> result, String sql, Map<String, Object> params) throws InvalidParameterException {
		Query querySelect = JPA.getUtil().getEntityManager().createNativeQuery("select d.DOC_ID " + sql);
		if (params != null) {
			for (Map.Entry<String, Object> pair : params.entrySet()) {
				querySelect.setParameter(pair.getKey(), pair.getValue());
			}
		}

		List sqlRes = querySelect.getResultList();
			Set<Integer> filtered = (Set<Integer>) sqlRes.stream().map(r->((Number)r).intValue()).filter(r -> result.contains(r)).collect(Collectors.toSet());
		int boundary = 1000;
		if(filtered.size()>boundary) throw new InvalidParameterException(IndexUIbean.getMessageResourceString(IndexUIbean.UI_beanMessages, "search.invalidParameters",boundary));
		return filtered;
	}

	/**
	   Основен sql  с полета  за търсене на документи за опис
	 * [0]-DOC_ID
	 * [1] null 
	 * [2]-RN_DOC
	 * [3] rn_prefix
     * [4] rn_pored
     * [5] doc_date
     * [6] doc_type
     * [7] doc_vid
     * [8] date_pred
     * [9] metod_pred
     * [10] code_ref
     * [11] text_pred
     * [12] nom_ekz
     * [13] user_pred
     * [14] doc_teh_nom
     * [15] doc_teh_data 
     * [16]-REGISTER_ID
	 * [17]-OTNOSNO (String)
	 * [18]-URGENT<br>
	 * [19]-CODE_REF_CORRESP
	 * [20]	AUTHORS_CODES-кодовете на авторите с разделител запетая - пример (1,6,18)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date) 	
	 * @param dialect
	 * @return
	 */
	public String getSqlStrForOpis (String dialect) {
		
		StringBuilder select = new StringBuilder();
	//	String dialect = JPA.getUtil().getDbVendorName();

		select.append("select distinct d.DOC_ID A0, null A1, "+DocDAO.formRnDocSelect("d.", dialect)+" A2, d.RN_PREFIX A3, d.RN_PORED A4, d.DOC_DATE A5, d.DOC_TYPE A6, d.DOC_VID A7 ");
		select.append(", " + DialectConstructor.convertDateToSQLString(dialect, new Date())  + " A8 ");  // Дата на предаване 
		select.append(", " + String.valueOf(OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA) + " A9 ");   // Предаване на ръка
		select.append(", null A10, ' ' A11, 1 A12, null A13 ");   // Предаден на (код), номер екз.(1), null
		select.append(", d.TEH_NOMER A14, d.TEH_DATE A15, d.REGISTER_ID A16 ");
		select.append(", " + DialectConstructor.limitBigString(dialect, "d.OTNOSNO", 300) + " A17 "); // max 300!
		select.append(" , d.URGENT A18, d.CODE_REF_CORRESP A19 ");
		select.append(" , CASE WHEN d.DOC_TYPE = 1 THEN null ELSE "); // за входящите авторите не се теглят
		select.append(DialectConstructor.convertToDelimitedString(dialect, "dr.CODE_REF", "DOC_REFERENTS dr where dr.DOC_ID = d.DOC_ID and dr.ROLE_REF = 1", "dr.PORED"));
		select.append(" END A20 ");
		select.append(",  d.PROCESSED  A21 ");    // Ново поле
		
		return select.toString();
		
	}
	
	/**
	 * Използва се от основния екран за търсене на документи за формиране на опис <br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида:<br>
	 * [0]-DOC_ID
	 * [1] null 
	 * [2]-RN_DOC
	 * [3] rn_prefix
     * [4] rn_pored
     * [5] doc_date
     * [6] doc_type
     * [7] doc_vid
     * [8] date_pred
     * [9] metod_pred
     * [10] code_ref
     * [11] text_pred
     * [12] nom_ekz
     * [13] user_pred
     * [14] doc_teh_nom
     * [15] doc_teh_data 
     * [16]-REGISTER_ID
	 * [17]-OTNOSNO (String)
	 * [18]-URGENT<br>
	 * [19]-CODE_REF_CORRESP
	 * [20]	AUTHORS_CODES-кодовете на авторите с разделител запетая - пример (1,6,18)
	 * [21]  doc.PROCESSED
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date) 	 	
	 * @param userData това е този които изпълнява търсенето
	 * List<Object[]> allDocs - вече избрани документи, които трябва да се изключат
	 * nastrWithEkz - дали ще има екземпляри - true
	 */
	
	public void buildQueryDocListForOpis(BaseUserData userData, List<Object[]> allDocs, boolean nastrWithEkz) {
		String dialect = JPA.getUtil().getDbVendorName();
		
		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();
		
		select.append(getSqlStrForOpis (dialect));
						
		from.append(" from DOC d ");
		
		// Търсят се Документи, които са в регистратурата - при nastrWithEkz = false - без екземпляри
		if (!nastrWithEkz) {
			String sqlDocInRegistr = " (select count(dv.ID) from doc_dvij dv where dv.DOC_ID = d.DOC_ID and dv.DVIJ_DATE <= " +  DialectConstructor.convertDateToSQLString(dialect, new Date());
			sqlDocInRegistr +=  " and (dv.RETURN_DATE is null or dv.RETURN_DATE > " +    DialectConstructor.convertDateToSQLString(dialect, new Date()) + ")) = 0 ";   
			
			where.append((where.length() == 0 ? WHERE : AND) + sqlDocInRegistr);
		}	
		
		// При поредно търсене зададените id На документи в allDocs  трябва да се пропуснат
		if (allDocs != null && !allDocs.isEmpty()) {
			String s0 = "";  
		 	String s1 = "";
		 	String s = ""; 
		 	s = "";
			if (allDocs.size() <= 1000) {
				for (int i = 0; i < allDocs.size(); i++) {
					Object[] obj = allDocs.get(i);
					Integer idDoc = asInteger(obj[0]);
		    		if (!s.isEmpty())  s+= ",";
		    		s += String.valueOf(idDoc);
		    	}
				s = "(" + s + ") ";
				s0 =" d.DOC_ID not in " + s;
			} else {
				
				s = "";
						
	         int j = 0;
	         int step = 1000;
	         
	         List <String>  strL = new ArrayList<> () ; 
			
			     while (j < allDocs.size())  {
			    	 
			    	 s = "";
			    	 int k = j + step;
					
			    	 if (k < allDocs.size()) { 
						for (int i = j; i < k; i++) {
							Object[] obj = allDocs.get(i);
							Integer idDoc = asInteger(obj[0]);
				    		if (!s.isEmpty())  s+= ",";
				    		s += String.valueOf(idDoc);
				    	}
			    	 } else {
			    		 for (int i = j; i < allDocs.size(); i++) {
			    			 Object[] obj = allDocs.get(i);
								Integer idDoc = asInteger(obj[0]);
					    		if (!s.isEmpty())  s+= ",";
					    		s += String.valueOf(idDoc);
					     }
			    						    		 
			    	 }
			    	 strL.add(s);
			    	 j = k;
			     } 
			
			     if (!strL.isEmpty() ) {
			    	 s = "";
			    	 for (int i = 0; i < strL.size(); i++) {
			    		 if (!s.isEmpty())  s+= " and ";
			    		 s += " d.DOC_ID not in (" + strL.get(i) + ") ";
			    	 }
			    	s0 = "("+  s + ") "; 
			     }
			     
			}
			
			
			where.append((where.length() == 0 ? WHERE : AND) + s0);
		}
		

		String t = trimToNULL_Upper(this.rnDoc);
		if (t != null) {
			if (this.rnDocEQ) { // пълно съвпадение case insensitive
				where.append((where.length() == 0 ? WHERE : AND) + " upper(d.RN_DOC) = :rnDoc ");
				params.put("rnDoc", t);

			} else {
				where.append((where.length() == 0 ? WHERE : AND) + " upper(d.RN_DOC) like :rnDoc ");
				params.put("rnDoc", "%" + t + "%");
			}
		}

		if (this.registraturaId != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.REGISTRATURA_ID = :registraturaId ");
			params.put("registraturaId", this.registraturaId);
		}
		if (this.registerId != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.REGISTER_ID = :registerId ");
			params.put("registerId", this.registerId);
		}
		if (this.registerIdList != null && !this.registerIdList.isEmpty()) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.REGISTER_ID in (:registerIdList) ");
			params.put("registerIdList", this.registerIdList);
		}

		if (this.docTypeArr != null && this.docTypeArr.length != 0 && this.docTypeArr.length < 3) { // да има нещо но да не са
																									// всички
			where.append((where.length() == 0 ? WHERE : AND) + " d.DOC_TYPE in (:docTypeList) ");
			params.put("docTypeList", Arrays.asList(this.docTypeArr));
		}
		if (this.docVidList != null && !this.docVidList.isEmpty()) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DOC_VID in (:docVidList) ");
			params.put("docVidList", this.docVidList);
		}
		if (this.validArr != null && this.validArr.length != 0) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.VALID in (:validList) ");
			params.put("validList", Arrays.asList(this.validArr));
		}
		if (this.urgentArr != null && this.urgentArr.length != 0) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.URGENT in (:urgentList) ");
			params.put("urgentList", Arrays.asList(this.urgentArr));
		}
		if (this.statusArr != null && this.statusArr.length != 0) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.STATUS in (:statusList) ");
			params.put("statusList", Arrays.asList(this.statusArr));
		}

		if (this.userReg != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.USER_REG = :userReg ");
			params.put("userReg", this.userReg);
		}

		t = trimToNULL_Upper(this.docName);
		if (t != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " upper(d.DOC_NAME) like :docName ");
			params.put("docName", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.otnosno);
		if (t != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " upper(d.OTNOSNO) like :otnosno ");
			params.put("otnosno", "%" + t + "%");
		}
		t = trimToNULL_Upper(this.docInfo);
		if (t != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " upper(d.DOC_INFO) like :docInfo ");
			params.put("docInfo", "%" + t + "%");
		}

		t = trimToNULL_Upper(this.tehNomer);
		if (t != null) {
			if (this.tehNomerEQ) { // пълно съвпадение
				where.append((where.length() == 0 ? WHERE : AND) + " upper(d.TEH_NOMER) = :tehNomer ");
				params.put("tehNomer", t);

			} else {
				where.append((where.length() == 0 ? WHERE : AND) + " upper(d.TEH_NOMER) like :tehNomer ");
				params.put("tehNomer", "%" + t + "%");
			}
		}

		t = trimToNULL_Upper(this.guid);
		if (t != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " upper(d.GUID) = :guid ");
			params.put("guid", t);
		}

		if (this.docDateFrom != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DOC_DATE >= :docDateFrom ");
			params.put("docDateFrom", DateUtils.startDate(this.docDateFrom));
		}
		if (this.docDateTo != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DOC_DATE <= :docDateTo ");
			params.put("docDateTo", DateUtils.endDate(this.docDateTo));
		}

		if (this.receiveDateFrom != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.RECEIVE_DATE >= :receiveDateFrom ");
			params.put("receiveDateFrom", DateUtils.startDate(this.receiveDateFrom));
		}
		if (this.receiveDateTo != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.RECEIVE_DATE <= :receiveDateTo ");
			params.put("receiveDateTo", DateUtils.endDate(this.receiveDateTo));
		}

		if (this.waitAnswerDateFrom != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.WAIT_ANSWER_DATE >= :waitAnswerDateFrom ");
			params.put("waitAnswerDateFrom", DateUtils.startDate(this.waitAnswerDateFrom));
		}
		if (this.waitAnswerDateTo != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.WAIT_ANSWER_DATE <= :waitAnswerDateTo ");
			params.put("waitAnswerDateTo", DateUtils.endDate(this.waitAnswerDateTo));
		}

		if (this.nullWorkOffId != null) {
			if (this.nullWorkOffId.booleanValue()) {
				where.append((where.length() == 0 ? WHERE : AND) + " d.WORK_OFF_ID is null ");
			} else {
				where.append((where.length() == 0 ? WHERE : AND) + " d.WORK_OFF_ID is not null ");
			}
		}
		if (this.forRegId != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.FOR_REG_ID = :forRegId ");
			params.put("forRegId", this.forRegId);
		}

		if (this.codeRefCorresp != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.CODE_REF_CORRESP = :codeRefCorresp ");
			params.put("codeRefCorresp", this.codeRefCorresp);
		}
		t = trimToNULL_Upper(this.nameCorresp);
		if (t != null) {
			from.append(" inner join ADM_REFERENTS r on r.CODE = d.CODE_REF_CORRESP and upper(r.REF_NAME) like :nameCorresp ");
			params.put("nameCorresp", "%" + t + "%");
		}

		if (this.receiveMethodArr != null && this.receiveMethodArr.length != 0) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.RECEIVE_METHOD in (:receiveMethodList) ");
			params.put("receiveMethodList", Arrays.asList(this.receiveMethodArr));
		}

		if (Boolean.TRUE.equals(this.notIncluded)) {
			where.append((where.length() == 0 ? WHERE : AND) + " NOT EXISTS ");
			where.append(" (select dd.ID from DELO_DOC dd where dd.INPUT_DOC_ID = d.DOC_ID ) ");
		}

		boolean joinTask = false;
		if (this.taskReferentList != null && !this.taskReferentList.isEmpty()) {
			joinTask = true;

			from.append(" inner join TASK t on t.DOC_ID = d.DOC_ID ");
			from.append(" inner join TASK_REFERENTS tr on tr.TASK_ID = t.TASK_ID ");

			where.append((where.length() == 0 ? WHERE : AND) + " tr.CODE_REF in (:taskReferentList) and tr.ROLE_REF in ( :roleExecOtg , :roleExec ) ");
			params.put("taskReferentList", this.taskReferentList);
			params.put("roleExecOtg", CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG);
			params.put("roleExec", CODE_ZNACHENIE_TASK_REF_ROLE_EXEC);
		}

		if (this.taskDateFrom != null) {
			if (!joinTask) {
				from.append(" inner join TASK t on t.DOC_ID = d.DOC_ID ");
				joinTask = true;
			}
			where.append((where.length() == 0 ? WHERE : AND) + " t.ASSIGN_DATE >= :taskDateFrom ");
			params.put("taskDateFrom", DateUtils.startDate(this.taskDateFrom));
		}
		if (this.taskDateTo != null) {
			if (!joinTask) {
				from.append(" inner join TASK t on t.DOC_ID = d.DOC_ID ");
			}
			where.append((where.length() == 0 ? WHERE : AND) + " t.ASSIGN_DATE <= :taskDateTo ");
			params.put("taskDateTo", DateUtils.endDate(this.taskDateTo));
		}

		if (this.docReferent != null) {

			where.append((where.length() == 0 ? WHERE : AND) + " EXISTS ");

			where.append(" (select dr.ID from DOC_REFERENTS dr where dr.DOC_ID = d.DOC_ID and dr.CODE_REF = :docReferent ");
			params.put("docReferent", this.docReferent);

			if (this.docReferentRoles != null && this.docReferentRoles.length != 0 && this.docReferentRoles.length < 3) { // има и
																															// роли
				where.append(" and dr.ROLE_REF in (:docReferentRoles) ");
				params.put("docReferentRoles", Arrays.asList(this.docReferentRoles));

			}
			where.append(" ) "); // това затваря EXISTS !
		}

		if (useDost) {
			addAccessRules(where, from, params, (UserData)userData, true);
		}

		from.append(" left outer join LOCK_OBJECTS z on z.OBJECT_TIP = :zTip and z.OBJECT_ID = d.DOC_ID and z.USER_ID != :zUser ");
		params.put("zTip", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		params.put("zUser", userData.getUserId());

		setSql(select.toString() + from.toString() + where.toString());
		setSqlCount(" select count(distinct d.DOC_ID) " + from.toString() + where.toString());
		setSqlParameters(params);
	}

	/**
	 * Използва се от основния екран за пълнотекстово търсене на документи за формиране на опис  <br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида:<br>
	 * [0]-DOC_ID
	 * [1] null 
	 * [2]-RN_DOC
	 * [3] rn_prefix
     * [4] rn_pored
     * [5] doc_date
     * [6] doc_type
     * [7] doc_vid
     * [8] date_pred
     * [9] metod_pred
     * [10] code_ref
     * [11] text_pred
     * [12] nom_ekz
     * [13] user_pred
     * [14] doc_teh_nom
     * [15] doc_teh_data 
     * [16]-REGISTER_ID
	 * [17]-OTNOSNO (String)
	 * [18]-URGENT<br>
	 * [19]-CODE_REF_CORRESP	 
	 * [20]	AUTHORS_CODES-кодовете на авторите с разделител запетая - пример (1,6,18)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date) 
	 * List<Object[]> allDocs - вече избрани документи, които трябва да се изключат
	 */
	public void buildQueryDocListWithFulltextForOpis(BaseUserData userData, List<Object[]> allDocs) {
		Map<String, String> searchFields = new HashMap<>();
		Map<String, Object> filterFields = new HashMap<>();
		String t = trimToNULL(this.rnDoc);
		if (t != null) {
			if (this.rnDocEQ) { // пълно съвпадение
				filterFields.put(DocOcr.FilterFields.rnDoc.name(), t);
			} else {
				searchFields.put(DocOcr.FullTextFields.rnDocLike.name(), t);
			}
		}
		if (this.registraturaId != null) {
			filterFields.put(DocOcr.FilterFields.registraturaId.name(), this.registraturaId);
		}
		if (this.registerId != null) {
			filterFields.put(DocOcr.FilterFields.registerId.name(), this.registerId);
		}
		if (this.registerIdList != null && !this.registerIdList.isEmpty()) {
			filterFields.put(DocOcr.FilterFields.registerId.name(), this.registerIdList);
		}

		if (this.docTypeArr != null && this.docTypeArr.length != 0 && this.docTypeArr.length < 3) { // да има нещо но да не са
			filterFields.put(DocOcr.FilterFields.docType.name(), Arrays.asList(this.docTypeArr));
		}
		if (this.docVidList != null && !this.docVidList.isEmpty()) {
			filterFields.put(DocOcr.FilterFields.docVid.name(), this.docVidList);
		}
		if (this.validArr != null && this.validArr.length != 0) {
			filterFields.put(DocOcr.FilterFields.valid.name(), Arrays.asList(this.validArr));
		}
		if (this.urgentArr != null && this.urgentArr.length != 0) {
			filterFields.put(DocOcr.FilterFields.urgent.name(), Arrays.asList(this.urgentArr));
		}
		if (this.userReg != null) {
			filterFields.put(DocOcr.FilterFields.userReg.name(), this.userReg);
		}

		t = trimToNULL_Upper(this.docName);
		if (t != null) {
			searchFields.put(DocOcr.FullTextFields.docName.name(), t);
		}
		t = trimToNULL_Upper(this.otnosno);
		if (t != null) {
			searchFields.put(DocOcr.FullTextFields.otnosno.name(), t);
		}
		t = trimToNULL_Upper(this.docInfo);
		if (t != null) {
			searchFields.put(DocOcr.FullTextFields.docInfo.name(), t);
		}

		t = trimToNULL(this.tehNomer);
		if (t != null) {
			if (this.tehNomerEQ) { // пълно съвпадение
				filterFields.put(DocOcr.FilterFields.tehNomer.name(), t);
			} else {
				searchFields.put(DocOcr.FullTextFields.tehNomerLike.name(), t);
			}
		}

		t = trimToNULL_Upper(this.guid);
		if (t != null) {
			filterFields.put(DocOcr.FilterFields.guid.name(), this.guid);
		}

		if (this.docDateFrom != null) {
			filterFields.put(DocOcr.FilterFields.docDateOt.name(), DateUtils.startDate(this.docDateFrom));
		}
		if (this.docDateTo != null) {
			filterFields.put(DocOcr.FilterFields.docDateDo.name(), DateUtils.endDate(this.docDateTo));
		}

		if (this.receiveDateFrom != null) {
			filterFields.put(DocOcr.FilterFields.receiveDateOt.name(), DateUtils.startDate(this.receiveDateFrom));
		}
		if (this.receiveDateTo != null) {
			filterFields.put(DocOcr.FilterFields.receiveDateDo.name(), DateUtils.endDate(this.receiveDateTo));
		}

		if (this.waitAnswerDateFrom != null) {
			filterFields.put(DocOcr.FilterFields.waitAnswerDateOt.name(), DateUtils.startDate(this.waitAnswerDateFrom));
		}
		if (this.waitAnswerDateTo != null) {
			filterFields.put(DocOcr.FilterFields.waitAnswerDateDo.name(), DateUtils.endDate(this.waitAnswerDateTo));
		}

		if (this.codeRefCorresp != null) {
			filterFields.put(DocOcr.FilterFields.codeRefCorresp.name(), this.codeRefCorresp);
		}

		if (this.receiveMethodArr != null && this.receiveMethodArr.length != 0) {
			filterFields.put(DocOcr.FilterFields.receiveMethod.name(), Arrays.asList(this.receiveMethodArr));
		}

		String dialect = JPA.getUtil().getDbVendorName();
		
		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();
	
		
		Map<String, Object> params = new HashMap<>();

		select.append(getSqlStrForOpis (dialect));
	
		from.append(" from DOC d ");
		
		// Търсят се Документи, които са в регистратурата
		String sqlDocInRegistr = " (select count(dv.ID) from doc_dvij dv where dv.DOC_ID = d.DOC_ID and dv.DVIJ_DATE <= " +  DialectConstructor.convertDateToSQLString(dialect, new Date());
		sqlDocInRegistr +=  " and (dv.RETURN_DATE is null or dv.RETURN_DATE > " +    DialectConstructor.convertDateToSQLString(dialect, new Date()) + ")) = 0 ";   
		
		where.append((where.length() == 0 ? WHERE : AND) + sqlDocInRegistr);
		
		// При поредно търсене зададените id На документи в allDocs  трябва да се пропуснат
		if (allDocs != null && !allDocs.isEmpty()) {
			String s0 = "";  
		 	String s1 = "";
		 	String s = ""; 
		 	s = "";
			if (allDocs.size() <= 1000) {
				for (int i = 0; i < allDocs.size(); i++) {
					Object[] obj = allDocs.get(i);
					Integer idDoc = asInteger(obj[0]);
		    		if (!s.isEmpty())  s+= ",";
		    		s += String.valueOf(idDoc);
		    	}
				s = "(" + s + ") ";
				s0 =" d.DOC_ID not in " + s;
			} else {
				
				s = "";
						
	         int j = 0;
	         int step = 1000;
	         
	         List <String>  strL = new ArrayList<> () ; 
			
			     while (j < allDocs.size())  {
			    	 
			    	 s = "";
			    	 int k = j + step;
					
			    	 if (k < allDocs.size()) { 
						for (int i = j; i < k; i++) {
							Object[] obj = allDocs.get(i);
							Integer idDoc = asInteger(obj[0]);
				    		if (!s.isEmpty())  s+= ",";
				    		s += String.valueOf(idDoc);
				    	}
			    	 } else {
			    		 for (int i = j; i < allDocs.size(); i++) {
			    			 Object[] obj = allDocs.get(i);
								Integer idDoc = asInteger(obj[0]);
					    		if (!s.isEmpty())  s+= ",";
					    		s += String.valueOf(idDoc);
					     }
			    						    		 
			    	 }
			    	 strL.add(s);
			    	 j = k;
			     } 
			
			     if (!strL.isEmpty() ) {
			    	 s = "";
			    	 for (int i = 0; i < strL.size(); i++) {
			    		 if (!s.isEmpty())  s+= " and ";
			    		 s += " d.DOC_ID not in (" + strL.get(i) + ") ";
			    	 }
			    	s0 = "("+  s + ") "; 
			     }
			     
			}
			
			
			where.append((where.length() == 0 ? WHERE : AND) + s0);
		}

		Set<Integer> result = new DocFulltextSearch().searchFullText(filterFields, searchFields);
		     

		if (!result.isEmpty()) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DOC_ID in (:docIdList) ");
			params.put("docIdList", result);
		} else {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DOC_ID in (-1) ");
		}

		t = trimToNULL_Upper(this.nameCorresp);
		if (t != null) {
			from.append(" inner join ADM_REFERENTS r on r.CODE = d.CODE_REF_CORRESP and upper(r.REF_NAME) like :nameCorresp ");
			params.put("nameCorresp", "%" + t + "%");
		}

		boolean joinTask = false;
		if (this.taskReferentList != null && !this.taskReferentList.isEmpty()) {
			joinTask = true;

			from.append(" inner join TASK t on t.DOC_ID = d.DOC_ID ");
			from.append(" inner join TASK_REFERENTS tr on tr.TASK_ID = t.TASK_ID ");

			where.append((where.length() == 0 ? WHERE : AND) + " tr.CODE_REF in (:taskReferentList) and tr.ROLE_REF in ( :roleExecOtg , :roleExec ) ");
			params.put("taskReferentList", this.taskReferentList);
			params.put("roleExecOtg", CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG);
			params.put("roleExec", CODE_ZNACHENIE_TASK_REF_ROLE_EXEC);
		}

		if (this.taskDateFrom != null) {
			if (!joinTask) {
				from.append(" inner join TASK t on t.DOC_ID = d.DOC_ID ");
				joinTask = true;
			}
			where.append((where.length() == 0 ? WHERE : AND) + " t.ASSIGN_DATE >= :taskDateFrom ");
			params.put("taskDateFrom", DateUtils.startDate(this.taskDateFrom));
		}
		if (this.taskDateTo != null) {
			if (!joinTask) {
				from.append(" inner join TASK t on t.DOC_ID = d.DOC_ID ");
			}
			where.append((where.length() == 0 ? WHERE : AND) + " t.ASSIGN_DATE <= :taskDateTo ");
			params.put("taskDateTo", DateUtils.endDate(this.taskDateTo));
		}

		if (this.docReferent != null) {

			where.append((where.length() == 0 ? WHERE : AND) + " EXISTS ");

			where.append(" (select dr.ID from DOC_REFERENTS dr where dr.DOC_ID = d.DOC_ID and dr.CODE_REF = :docReferent ");
			params.put("docReferent", this.docReferent);

			if (this.docReferentRoles != null && this.docReferentRoles.length != 0 && this.docReferentRoles.length < 3) { // има и
				// роли
				where.append(" and dr.ROLE_REF in (:docReferentRoles) ");
				params.put("docReferentRoles", Arrays.asList(this.docReferentRoles));

			}
			where.append(" ) "); // това затваря EXISTS !
		}

		if (useDost) {
			addAccessRules(where, from, params, (UserData)userData, true);
		}
		
		setSql(select.toString() + from.toString() + where.toString());
		setSqlCount(" select count(distinct d.DOC_ID) " + from.toString() + where.toString());
		setSqlParameters(params);

	}
	
	

	/** @return the codeRefCorresp */
	public Integer getCodeRefCorresp() {
		return this.codeRefCorresp;
	}

	/** @return the deloYear */
	public Integer getDeloYear() {
		return this.deloYear;
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

	/** @return the docName */
	public String getDocName() {
		return this.docName;
	}

	/** @return the docReferent */
	public Integer getDocReferent() {
		return this.docReferent;
	}

	/** @return the docReferentRoles */
	public Integer[] getDocReferentRoles() {
		return this.docReferentRoles;
	}

	/** @return the docTypeArr */
	public Integer[] getDocTypeArr() {
		return this.docTypeArr;
	}

	/** @return the docVidList */
	
	public List<Integer> getDocVidList() {
		return this.docVidList;
	}

	/** @return the forRegId */
	public Integer getForRegId() {
		return this.forRegId;
	}

	/** @return the guid */
	public String getGuid() {
		return this.guid;
	}

	/** @return the nameCorresp */
	public String getNameCorresp() {
		return this.nameCorresp;
	}

	/** @return the notInDeloId */
	public Integer getNotInDeloId() {
		return this.notInDeloId;
	}

	/** @return the notInDocId */
	public Integer getNotInDocId() {
		return this.notInDocId;
	}

	/** @return the notInProtocolId */
	public Integer getNotInProtocolId() {
		return this.notInProtocolId;
	}

	/** @return the nullWorkOffId */
	public Boolean getNullWorkOffId() {
		return this.nullWorkOffId;
	}

	/** @return the otnosno */
	public String getOtnosno() {
		return this.otnosno;
	}

	/** @return the receiveDateFrom */
	public Date getReceiveDateFrom() {
		return this.receiveDateFrom;
	}

	/** @return the receiveDateTo */
	public Date getReceiveDateTo() {
		return this.receiveDateTo;
	}

	/** @return the receiveMethodArr */
	public Integer[] getReceiveMethodArr() {
		return this.receiveMethodArr;
	}

	/** @return the registerId */
	public Integer getRegisterId() {
		return this.registerId;
	}

	/** @return the registerIdList */
	public List<Integer> getRegisterIdList() {
		return this.registerIdList;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @return the rnDelo */
	public String getRnDelo() {
		return this.rnDelo;
	}

	/** @return the rnDoc */
	public String getRnDoc() {
		return this.rnDoc;
	}

	/** @return the taskDateFrom */
	public Date getTaskDateFrom() {
		return this.taskDateFrom;
	}

	/** @return the taskDateTo */
	public Date getTaskDateTo() {
		return this.taskDateTo;
	}

	/** @return the taskReferentList */
	public List<Integer> getTaskReferentList() {
		return this.taskReferentList;
	}

	/** @return the tehNomer */
	public String getTehNomer() {
		return this.tehNomer;
	}

	/** @return the urgentArr */
	public Integer[] getUrgentArr() {
		return this.urgentArr;
	}

	/** @return the userReg */
	public Integer getUserReg() {
		return this.userReg;
	}

	/** @return the validArr */
	public Integer[] getValidArr() {
		return this.validArr;
	}

	/** @return the waitAnswerDateFrom */
	public Date getWaitAnswerDateFrom() {
		return this.waitAnswerDateFrom;
	}

	/** @return the waitAnswerDateTo */
	public Date getWaitAnswerDateTo() {
		return this.waitAnswerDateTo;
	}

	/** @return the rnDocEQ */
	public boolean isRnDocEQ() {
		return this.rnDocEQ;
	}

	/** @return the tehNomerEQ */
	public boolean isTehNomerEQ() {
		return this.tehNomerEQ;
	}

	/** @param codeRefCorresp the codeRefCorresp to set */
	public void setCodeRefCorresp(Integer codeRefCorresp) {
		this.codeRefCorresp = codeRefCorresp;
	}

	/** @param deloYear the deloYear to set */
	public void setDeloYear(Integer deloYear) {
		this.deloYear = deloYear;
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

	/** @param docName the docName to set */
	public void setDocName(String docName) {
		this.docName = docName;
	}

	/** @param docReferent the docReferent to set */
	public void setDocReferent(Integer docReferent) {
		this.docReferent = docReferent;
	}

	/** @param docReferentRoles the docReferentRoles to set */
	public void setDocReferentRoles(Integer[] docReferentRoles) {
		this.docReferentRoles = docReferentRoles;
	}

	/** @param docTypeArr the docTypeArr to set */
	public void setDocTypeArr(Integer[] docTypeArr) {
		this.docTypeArr = docTypeArr;
	}

	/** @param docVidList the docVidList to set */
	public void setDocVidList(List<Integer> docVidList) {
		this.docVidList = docVidList;
	}

	/** @param forRegId the forRegId to set */
	public void setForRegId(Integer forRegId) {
		this.forRegId = forRegId;
	}

	/** @param guid the guid to set */
	public void setGuid(String guid) {
		this.guid = guid;
	}

	/** @param nameCorresp the nameCorresp to set */
	public void setNameCorresp(String nameCorresp) {
		this.nameCorresp = nameCorresp;
	}

	/** @param notInDeloId the notInDeloId to set */
	public void setNotInDeloId(Integer notInDeloId) {
		this.notInDeloId = notInDeloId;
	}

	/** @param notInDocId the notInDocId to set */
	public void setNotInDocId(Integer notInDocId) {
		this.notInDocId = notInDocId;
	}

	/** @param notInProtocolId the notInProtocolId to set */
	public void setNotInProtocolId(Integer notInProtocolId) {
		this.notInProtocolId = notInProtocolId;
	}

	/** @param nullWorkOffId the nullWorkOffId to set */
	public void setNullWorkOffId(Boolean nullWorkOffId) {
		this.nullWorkOffId = nullWorkOffId;
	}

	/** @param otnosno the otnosno to set */
	public void setOtnosno(String otnosno) {
		this.otnosno = otnosno;
	}

	/** @param receiveDateFrom the receiveDateFrom to set */
	public void setReceiveDateFrom(Date receiveDateFrom) {
		this.receiveDateFrom = receiveDateFrom;
	}

	/** @param receiveDateTo the receiveDateTo to set */
	public void setReceiveDateTo(Date receiveDateTo) {
		this.receiveDateTo = receiveDateTo;
	}

	/** @param receiveMethodArr the receiveMethodArr to set */
	public void setReceiveMethodArr(Integer[] receiveMethodArr) {
		this.receiveMethodArr = receiveMethodArr;
	}

	/** @param registerId the registerId to set */
	public void setRegisterId(Integer registerId) {
		this.registerId = registerId;
	}

	/** @param registerIdList the registerIdList to set */
	public void setRegisterIdList(List<Integer> registerIdList) {
		this.registerIdList = registerIdList;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** @param rnDelo the rnDelo to set */
	public void setRnDelo(String rnDelo) {
		this.rnDelo = rnDelo;
	}

	/** @param rnDoc the rnDoc to set */
	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}

	/** @param rnDocEQ the rnDocEQ to set */
	public void setRnDocEQ(boolean rnDocEQ) {
		this.rnDocEQ = rnDocEQ;
	}

	/** @param taskDateFrom the taskDateFrom to set */
	public void setTaskDateFrom(Date taskDateFrom) {
		this.taskDateFrom = taskDateFrom;
	}

	/** @param taskDateTo the taskDateTo to set */
	public void setTaskDateTo(Date taskDateTo) {
		this.taskDateTo = taskDateTo;
	}

	/** @param taskReferentList the taskReferentList to set */
	public void setTaskReferentList(List<Integer> taskReferentList) {
		this.taskReferentList = taskReferentList;
	}

	/** @param tehNomer the tehNomer to set */
	public void setTehNomer(String tehNomer) {
		this.tehNomer = tehNomer;
	}

	/** @param tehNomerEQ the tehNomerEQ to set */
	public void setTehNomerEQ(boolean tehNomerEQ) {
		this.tehNomerEQ = tehNomerEQ;
	}

	/** @param urgentArr the urgentArr to set */
	public void setUrgentArr(Integer[] urgentArr) {
		this.urgentArr = urgentArr;
	}

	/** @param userReg the userReg to set */
	public void setUserReg(Integer userReg) {
		this.userReg = userReg;
	}

	/** @param validArr the validArr to set */
	public void setValidArr(Integer[] validArr) {
		this.validArr = validArr;
	}

	/** @param waitAnswerDateFrom the waitAnswerDateFrom to set */
	public void setWaitAnswerDateFrom(Date waitAnswerDateFrom) {
		this.waitAnswerDateFrom = waitAnswerDateFrom;
	}

	/** @param waitAnswerDateTo the waitAnswerDateTo to set */
	public void setWaitAnswerDateTo(Date waitAnswerDateTo) {
		this.waitAnswerDateTo = waitAnswerDateTo;
	}

	/** @return the notIncluded */
	public Boolean getNotIncluded() {
		return this.notIncluded;
	}

	/** @param notIncluded the notIncluded to set */
	public void setNotIncluded(Boolean notIncluded) {
		this.notIncluded = notIncluded;
	}

	/** @return the markRelDocId */
	public Integer getMarkRelDocId() {
		return this.markRelDocId;
	}

	/** @param markRelDocId the markRelDocId to set */
	public void setMarkRelDocId(Integer markRelDocId) {
		this.markRelDocId = markRelDocId;
	}

	/** @return the statusArr */
	public Integer[] getStatusArr() {
		return this.statusArr;
	}

	/** @param statusArr the statusArr to set */
	public void setStatusArr(Integer[] statusArr) {
		this.statusArr = statusArr;
	}

	/** @return the docTypeEditList */
	public List<Integer> getDocTypeEditList() {
		return this.docTypeEditList;
	}

	/** @param docTypeEditList the docTypeEditList to set */
	public void setDocTypeEditList(List<Integer> docTypeEditList) {
		this.docTypeEditList = docTypeEditList;
	}

	/** @return the checkUserAccess */
	@XmlTransient 
	public Serializable[] getCheckUserAccess() {
		return this.checkUserAccess;
	}

	/** @param checkUserAccess the checkUserAccess to set */
	public void setCheckUserAccess(Serializable[] checkUserAccess) {
		this.checkUserAccess = checkUserAccess;
	}

	/** @return the competence */
	public Integer getCompetence() {
		return this.competence;
	}

	/** @param competence the competence to set */
	public void setCompetence(Integer competence) {
		this.competence = competence;
	}

	/** @return the irregularArr */
	public Integer[] getIrregularArr() {
		return this.irregularArr;
	}

	/** @param irregularArr the irregularArr to set */
	public void setIrregularArr(Integer[] irregularArr) {
		this.irregularArr = irregularArr;
	}

	/** @return the dvijNotReturned */
	public Boolean getDvijNotReturned() {
		return this.dvijNotReturned;
	}

	/** @param dvijNotReturned the dvijNotReturned to set */
	public void setDvijNotReturned(Boolean dvijNotReturned) {
		this.dvijNotReturned = dvijNotReturned;
	}

	/** @return the dvijToText */
	public String getDvijToText() {
		return this.dvijToText;
	}

	/** @param dvijToText the dvijToText to set */
	public void setDvijToText(String dvijToText) {
		this.dvijToText = dvijToText;
	}
	
	/** @return the updWorkRegOffForbidden */
	public Boolean getUpdWorkRegOffForbidden() {
		return this.updWorkRegOffForbidden;
	}

	/** @param updWorkRegOffForbidden the updWorkRegOffForbidden to set */
	public void setUpdWorkRegOffForbidden(Boolean updWorkRegOffForbidden) {
		this.updWorkRegOffForbidden = updWorkRegOffForbidden;
	}
	
	/** @return the docTopicList */
	public List<Integer> getDocTopicList() {
		return this.docTopicList;
	}

	/** @param docTopicList the docTopicList to set */
	public void setDocTopicList(List<Integer> docTopicList) {
		this.docTopicList = docTopicList;
	}

	/** @return the sendRegistraturaList */
	public List<Integer> getSendRegistraturaList() {
		return this.sendRegistraturaList;
	}

	/** @param sendRegistraturaList the sendRegistraturaList to set */
	public void setSendRegistraturaList(List<Integer> sendRegistraturaList) {
		this.sendRegistraturaList = sendRegistraturaList;
	}

	/** Добава условия в зависимост от параметрите подадени в дейността за отнемане на достъп
	 * @param where
	 */
	private void appendCheckUserAccessSql(StringBuilder where, Map<String, Object> params) {
		
		if (this.checkUserAccess[2] == null) { // иска се да има достъп

			where.append((where.length() == 0 ? WHERE : AND) + " EXISTS ");
			where.append(" ( select dal.DOC_ID from DOC_ACCESS_ALL dal where dal.DOC_ID = d.DOC_ID and ");
			
			if (this.checkUserAccess[3] == null) { // обикновен усер
				where.append(" dal.CODE_REF in (" + this.checkUserAccess[0] + "," + this.checkUserAccess[1] + ") ) ");	

			} else { // ръководител
				where.append(" ( CODE_REF in (" + this.checkUserAccess[0] + "," + this.checkUserAccess[1] + ")");
				where.append(" or CODE_STRUCT in (" + this.checkUserAccess[3] + ") ) ) ");
			}
			
			// и да не му е отнет
			where.append(" and NOT EXISTS ( select da.DOC_ID from DOC_ACCESS da where da.DOC_ID = d.DOC_ID and da.CODE_REF = :cuacr and da.EXCLUDE = 1 ) ");
			
		} else { // иска се да му е отнет
			where.append((where.length() == 0 ? WHERE : AND) //
				+ " EXISTS ( select da.DOC_ID from DOC_ACCESS da where da.DOC_ID = d.DOC_ID and da.CODE_REF = :cuacr and da.EXCLUDE = 1 ) ");
		}
		
		params.put("cuacr", this.checkUserAccess[0]);
	}
	
	/** Метод за добавяне на достъпа до докумнета
	 * @param where - Условия
	 * @param from - таблици
	 * @param userData - обкет от UserData
	 * @param viewMode
	 */
	public void addAccessRules(StringBuilder where, StringBuilder from, Map<String, Object> params, UserData userData, boolean viewMode) {
		// TODO тука стана доста мазало и е добре като се приключи с изискванията да се оптимизира малко
		// 1. ако има само една позволена регистратура за преглед може да е = вместо in
		// 2. ако позволените регистратури за преглед са равни на броя на регистратурите в системата няма нужда от увловие по регистратури
		// 3. ако вместо inner join DOC_ACCESS_ALL се сложи EXISTS няма нужда в основния селект да се прави с distinct. трябва да се тества дали е по добре
		// 4. където се слагат userAccess и звеното може да се направи с параметри вместо с числата директно
		// 5. ...
		
		if (userData.isDocAccessDenied()) { // има документи, до които му е отказан достъпа
			from.append(" left outer join DOC_ACCESS denied on denied.DOC_ID = d.DOC_ID and denied.CODE_REF = "+userData.getUserAccess()+" and denied.EXCLUDE = 1 ");
			where.append((where.length() == 0 ? WHERE : AND) + " denied.ID is null ");
		}
		
		if (viewMode) {			
			if (userData.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW)
				|| userData.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT)) {
				// режим преглед - пълен достъп

//				if (this.registraturaId == null) { // не е избрана и трябва да се върнат всички документи от позволените му регсистратури 
//													// + !!! документите, до които има достъп, който може да са от други регистратури !!!
//	
//					where.append((where.length() == 0 ? WHERE : AND) + " (d.REGISTRATURA_ID in (:viewRegistraturiSet) "); // позволените
//					params.put("viewRegistraturiSet", userData.getAccessValues().get(CODE_CLASSIF_REGISTRATURI_OBJACCESS).keySet());
//
//
//					// + тези до които има достъп
//					where.append(" or EXISTS (select dost.DOC_ID from DOC_ACCESS_ALL dost where dost.DOC_ID = d.DOC_ID and ");
//					if (userData.getAccessZvenoList() == null) {
//						where.append(" dost.CODE_REF in ("+userData.getUserAccess()+","+userData.getZveno()+"))) ");
//					} else { // шеф
//						where.append(" (dost.CODE_REF in ("+userData.getUserAccess()+","+userData.getZveno() +") or dost.CODE_STRUCT in (" + userData.getAccessZvenoList() + ")))) ");
//					}
//				}
				return;
			}

		} else if (userData.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT)) {
			return; // режим актуализация - пълен достъп, но само в текущата регистратура, която е сетната в търсенето
		}
		
		String accessJoin = viewMode ? " left outer join " : " inner join ";
		from.append(accessJoin + " DOC_ACCESS_ALL on DOC_ACCESS_ALL.DOC_ID = d.DOC_ID ");
		
		StringBuilder dop = new StringBuilder();
	    dop.append("DOC_ACCESS_ALL.CODE_REF in (" + userData.getUserAccess() + "," + userData.getZveno() + ")");

	    if (viewMode && userData.getAccessZvenoList() != null) { // ако е шеф и то само за режим преглед
	    	dop.append(" OR DOC_ACCESS_ALL.CODE_STRUCT in (" +  userData.getAccessZvenoList() + ")") ;
	    }
	    
	    if (viewMode) { // общодостъпен играе роля само за преглед
//		    if (this.registraturaId != null) {
			    dop.append(" OR d.FREE_ACCESS = :freeDa ");
//		    } else { // общодостъпните само за неговата регистратура
//		    	dop.append(" OR (d.FREE_ACCESS = :freeDa and d.REGISTRATURA_ID = :registraturaId) ");
//		    	params.put("registraturaId", userData.getRegistratura());
//		    }
		    params.put("freeDa", SysConstants.CODE_ZNACHENIE_DA);
	    }
	    
	    where.append((where.length() == 0 ? WHERE : AND) + " ( " + dop + " ) ");
	}

	public boolean isUseDost() {
		return useDost;
	}

	public void setUseDost(boolean useDost) {
		this.useDost = useDost;
	}

	public String getFileText() {
		return fileText;
	}

	public void setFileText(String fileText) {
		this.fileText = fileText;
	}

	public String getDocMemberName() {
		return this.docMemberName;
	}
	public void setDocMemberName(String docMemberName) {
		this.docMemberName = docMemberName;
	}
	public List<Integer> getDocMemberRoleList() {
		return this.docMemberRoleList;
	}
	public void setDocMemberRoleList(List<Integer> docMemberRoleList) {
		this.docMemberRoleList = docMemberRoleList;
	}
	
	/** @return the excludeSpecDocs */
	public boolean isExcludeSpecDocs() {
		return this.excludeSpecDocs;
	}
	/** @param excludeSpecDocs the excludeSpecDocs to set */
	public void setExcludeSpecDocs(boolean excludeSpecDocs) {
		this.excludeSpecDocs = excludeSpecDocs;
	}

	public String getReceivedBy() {
		return this.receivedBy;
	}
	public void setReceivedBy(String receivedBy) {
		this.receivedBy = receivedBy;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new  SystemJournal();				
		dj.setCodeObject(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		//dj.setIdObject(getId());
		//dj.setIdentObject("Търсене на документи");
		return dj;
	}
}
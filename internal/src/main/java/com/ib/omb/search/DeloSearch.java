package com.ib.omb.search;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS;
import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

import java.io.Serializable;
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
import com.ib.omb.components.CompDeloSearch;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.system.OmbConstants;
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
import com.ib.system.utils.SearchUtils;

/**
 * Търсене на преписки
 *
 * @author belev
 */
public class DeloSearch extends SelectMetadata implements AuditExt {

	/**  */
	private static final String AND = " and ";
	/**  */
	private static final String WHERE = " where ";

	/**  */
	private static final long serialVersionUID = -4363991799689317766L;

	@JournalAttr(label="registraturaId",defaultText = "Регистратура",classifID = ""+OmbConstants.CODE_CLASSIF_REGISTRATURI)
	private Integer registraturaId;

	@JournalAttr(label="codeRefLeadList",defaultText = "Водещ служител",classifID = ""+OmbConstants.CODE_CLASSIF_ADMIN_STR)
	private List<Integer> codeRefLeadList; // водещ служител (множествено)

	@JournalAttr(label="rnDelo",defaultText = "Регистрационен номер")
	private String	rnDelo;
	@JournalAttr(label="rnDeloEQ",defaultText = "Регистрационен номер (пълно съвпадение)")
	private boolean	rnDeloEQ = true;	// ако е true се търси по пълно съвпадение по номер

	// период на дата на регистратция
	@JournalAttr(label="deloDateFrom",defaultText = "Дата на дело - от")
	private Date	deloDateFrom;
	@JournalAttr(label="deloDateTo",defaultText = "Дата на дело - до")
	private Date	deloDateTo;

	// период на дата на статус
	@JournalAttr(label="statusDateFrom",defaultText = "Дата на статус - от")
	private Date	statusDateFrom;
	@JournalAttr(label="statusDateFrom",defaultText = "Дата на статус - до")
	private Date	statusDateTo;

	@JournalAttr(label="deloTypeArr",defaultText = "Тип на делото",classifID = ""+OmbConstants.CODE_CLASSIF_DELO_TYPE)
	private Integer[]	deloTypeArr;
	
	private Integer		notInTip; // ако има нещо значи няма да връща от този тип преписките
	
	@JournalAttr(label="statusArr",defaultText = "Статус на делото",classifID = ""+OmbConstants.CODE_CLASSIF_DELO_STATUS)
	private Integer[]	statusArr;

	@JournalAttr(label="deloName",defaultText = "Наименование на дело")
	private String	deloName;
	
	@JournalAttr(label="deloInfo",defaultText = "Описание")
	private String	deloInfo;	// описание

	private Integer	notRelDocId;	// ако има нещо се смята, че намерените преписки не трябва да са във връзка DELO_DOC с този
									// док
	private Integer	notRelDeloId;	// ако има нещо се смята, че намерените преписки не трябва да са във връзка DELO_DELO с това
									// дело

	private Integer notInProtocolId; // ако има нещо се смята, че намерените дела не трябва да са във връзка DELO_ARCHIVE с
										// този протокол

	@JournalAttr(label="notIncluded",defaultText = "Да не е вложена")
	private Boolean notIncluded; // true-не е вложена никъде
	
	private boolean enableStorageSearch; 	// ако се вдигне флага ще се гледат и аргументите за търсене на съхранени преписки
	
	@JournalAttr(label="room",defaultText = "Помещение")
	private String 	room; 					// помощение
	@JournalAttr(label="shkaf",defaultText = "Шкаф")
	private String 	shkaf; 					// шкаф
	@JournalAttr(label="stillage",defaultText = "Стелаж")
	private String 	stillage; 				// стелаж
	@JournalAttr(label="box",defaultText = "Кутия")
	private String 	box; 					// кутия

	private Boolean storage; 				// NULL-без значение; true-имат съхранение; false-нямат съхранение

	// данни за търсене при отнемане и изтриване на отнемане на достъп
	private Serializable[] checkUserAccess; // [0]-codeRef
								 			// [1]-zveno
											// [2]=null- има достъп, [2]!=null- отнет достъп
											// [3]-ако е е на ръководна длъжност списък със звената, до които има достъп "2,4,9"

	@JournalAttr(label="dvijNotReturned",defaultText = "Предадени преписки/дела, които не са върнати")
	private Boolean dvijNotReturned; 	// Предадени документи, които не са върнати
	@JournalAttr(label="dvijToText",defaultText = "Предадени на")
	private String 	dvijToText; 		// Предадени на
	@JournalAttr(label="useDost",defaultText = "Прилага ли се достъп")
	private boolean useDost = true;   // Дали да се добавят условия за достъп

	private boolean addDeloAccess; // да се добавят в резултата и преписките, до които
									// усера има изричен достъп без значение от регистратура

	private StringBuilder sqlSelect ;
	private StringBuilder sqlFrom ;
	private StringBuilder sqlWhere ;
	private Map<String, Object> sqlParams;
	
	
	// като са с еднакви имена няма да го има 2 пъти в xml-a
	@JournalAttr(label="sql",defaultText = "SQL зявка")
	private String	sql;
	@JournalAttr(label="sqlCount",defaultText = "SQL зявка за брой")
	private String	sqlCount;
	
	/** */
	public DeloSearch() {
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
	public DeloSearch(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/**
	 * Търсене което се използва от компонентата {@link CompDeloSearch}<br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида: <br>
	 * [0]-DELO_ID<br>
	 * [1]-RN_DELO<br>
	 * [2]-DELO_DATE<br>
	 * [3]-STATUS<br>
	 * [4]-DELO_NAME<br>
	 * [5]-INIT_DOC_ID<br>
	 * [6]-REGISTER_ID<br>
	 * [7]-DELO_INFO<br>
	 * [8]-DELO_TYPE<br>
	 * [9]-WITH_SECTION<br>
	 * [10]-WITH_TOM<br>
	 * [11]-BR_TOM<br>
	 * [12]-FREE_ACCESS<br>
	 *
	 * @param userData това е този които изпълнява търсенето
	 */
	public void buildQueryComp(BaseUserData userData) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		String distinct = this.useDost ? "distinct" : ""; // само ако има достъп, защото там може да го размножи
		
		select.append(" select  "+distinct+" d.DELO_ID a0, d.RN_DELO a1, d.DELO_DATE a2, d.STATUS a3, ");
		select.append(DialectConstructor.limitBigString(dialect, "d.DELO_NAME", 300) + " a4 "); // max 300!
		select.append(" , init.DOC_ID a5, init.REGISTER_ID a6, "+DialectConstructor.limitBigString(dialect, "d.DELO_INFO", 300)+" a7, d.DELO_TYPE a8 ");
		select.append(" , d.WITH_SECTION a9, d.WITH_TOM a10, d.BR_TOM a11, d.FREE_ACCESS a12 ");
		
		from.append(" from DELO d ");

//		if (this.registraturaId != null) {
//			if (this.addDeloAccess) {
//				where.append((where.length() == 0 ? WHERE : AND) 
//					+ " (d.REGISTRATURA_ID = :registraturaId or EXISTS (select dae.ID from DELO_ACCESS dae where dae.DELO_ID = d.DELO_ID and dae.CODE_REF = :cre and dae.EXCLUDE = 2)) ");				
//				params.put("cre", ((UserData)userData).getUserAccess());
//			} else {
//				where.append((where.length() == 0 ? WHERE : AND) + " d.REGISTRATURA_ID = :registraturaId ");
//			}
//			params.put("registraturaId", this.registraturaId);
//		}

		String t = SearchUtils.trimToNULL_Upper(this.rnDelo);
		if (t != null) {
			if (this.rnDeloEQ) { // пълно съвпадение case insensitive
				where.append((where.length() == 0 ? WHERE : AND) + " upper(d.RN_DELO) = :rnDelo ");
				params.put("rnDelo", t);

			} else {
				where.append((where.length() == 0 ? WHERE : AND) + " upper(d.RN_DELO) like :rnDelo ");
				params.put("rnDelo", "%" + t + "%");
			}
		}

		if (this.deloTypeArr != null && this.deloTypeArr.length != 0) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DELO_TYPE in (:deloTypeList) ");
			params.put("deloTypeList", Arrays.asList(this.deloTypeArr));
		} else if (this.notInTip != null) { // ако нищо не е въведено и трябва да изключа този тип
			where.append((where.length() == 0 ? WHERE : AND) + " d.DELO_TYPE != :notInTip ");
			params.put("notInTip", this.notInTip);
		}
		if (this.statusArr != null && this.statusArr.length != 0) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.STATUS in (:statusList) ");
			params.put("statusList", Arrays.asList(this.statusArr));
		}

		if (this.deloDateFrom != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DELO_DATE >= :deloDateFrom ");
			params.put("deloDateFrom", DateUtils.startDate(this.deloDateFrom));
		}
		if (this.deloDateTo != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DELO_DATE <= :deloDateTo ");
			params.put("deloDateTo", DateUtils.endDate(this.deloDateTo));
		}

		if (this.notRelDocId != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " NOT EXISTS ");
			where.append(" (select dd.ID from DELO_DOC dd where dd.DELO_ID = d.DELO_ID and dd.INPUT_DOC_ID = :notRelDocId ) ");
			params.put("notRelDocId", this.notRelDocId);
		}

		if (this.notRelDeloId != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " d.DELO_ID != :notRelDeloId ");

			where.append(" and NOT EXISTS ");
			if (Boolean.TRUE.equals(this.notIncluded)) {
				where.append(" (select dd.ID from DELO_DELO dd where dd.INPUT_DELO_ID = d.DELO_ID) ");
			} else {
				where.append(" (select dd.ID from DELO_DELO dd where (dd.DELO_ID = d.DELO_ID and dd.INPUT_DELO_ID = :notRelDeloId) or (dd.DELO_ID = :notRelDeloId and dd.INPUT_DELO_ID = d.DELO_ID) ) ");
			}
			params.put("notRelDeloId", this.notRelDeloId);
		}

		if (this.notInProtocolId != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " NOT EXISTS ");
			where.append(" (select da.ID from DELO_ARCHIVE da where da.PROTOCOL_ID = :notInProtocolId and da.DELO_ID = d.DELO_ID ) ");
			params.put("notInProtocolId", this.notInProtocolId);
		}

		
		
		if (useDost) {
			addAccessRules(where, from, params, (UserData)userData, true);
		}

		setSqlCount(" select count("+distinct+" d.DELO_ID) " + from.toString() + where.toString());

		// !!! тъй като по DOC не се слага where този join не влияе на count() и за да не товари излишно го слагам тук !!!
		from.append(" left outer join DOC init on init.DOC_ID = d.INIT_DOC_ID ");

		setSql(select.toString() + from.toString() + where.toString());

		setSqlParameters(params);
	}

	/**
	 * Използва се от основния екран за търсене на преписки и дела <br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида: <br>
	 * [0]-DELO_ID (DVIJ_ID - само ако this.dvijNotReturned==true)<br>
	 * [1]-RN_DELO<br>
	 * [2]-DELO_DATE<br>
	 * [3]-DELO_TYPE<br>
	 * [4]-STATUS<br>
	 * [5]-CODE_REF_LEAD<br>
	 * [6]-DELO_NAME (String)<br>
	 * [7]-LOCK_USER<br>
	 * [8]-LOCK_DATE<br>
	 * [9]-WITH_TOM<br>
	 * [10]-BR_TOM<br>
	 * [11]-DELO_ID 	- само ако this.dvijNotReturned==true<br>
	 * [12]-DVIJ_METHOD - само ако this.dvijNotReturned==true<br>
	 * [13]-DVIJ_TEXT 	- само ако this.dvijNotReturned==true<br>
	 * [14]-DVIJ_DATE 	- само ако this.dvijNotReturned==true<br>
	 *
	 * @param userData това е този които изпълнява търсенето
	 * @param mode : 0-актуализация, 1-разглеждане, 2-пререгистрация, 3-съхранение, 4-прехвърляне в друга регистратура
	 */
	public void buildQueryDeloList(BaseUserData userData, int mode) {
		buildQueryDeloListInner(userData, mode, false);
	}
	
	private void buildQueryDeloListInner(BaseUserData userData, int mode, boolean isFullText) {
		String dialect = JPA.getUtil().getDbVendorName();

		sqlParams = new HashMap<>();

		sqlSelect = new StringBuilder();
		sqlFrom = new StringBuilder();
		sqlWhere = new StringBuilder();
		
		sqlFrom.append(" from DELO d ");
		
		String firstCol = "d.DELO_ID";
		if (Boolean.TRUE.equals(this.dvijNotReturned)) {
			firstCol = "dvij.ID"; 
		}

		boolean joinLock = mode != 1; // винаги ако не е разглеждане

		sqlSelect.append(" select distinct "+firstCol+" a0, d.RN_DELO a1, d.DELO_DATE a2, d.DELO_TYPE a3, d.STATUS a4, d.CODE_REF_LEAD a5, ");
		sqlSelect.append(DialectConstructor.limitBigString(dialect, "d.DELO_NAME", 300) + " a6 "); // max 300!
		sqlSelect.append(", "+ (joinLock ? "z.user_id" : "null") +" a7, "+ (joinLock ? "z.lock_date" : "null") +" a8, d.WITH_TOM a9, d.BR_TOM a10 ");

		if (Boolean.TRUE.equals(this.dvijNotReturned)) {
			sqlSelect.append(" , d.DELO_ID a11, dvij.DVIJ_METHOD a12, dvij.DVIJ_TEXT a13, dvij.DVIJ_DATE a14 ");
		}

//		if (this.registraturaId != null) {
//			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.REGISTRATURA_ID = :registraturaId ");
//			sqlParams.put("registraturaId", this.registraturaId);
//		}

		String t = SearchUtils.trimToNULL_Upper(this.rnDelo);
		if (t != null) {
			if (this.rnDeloEQ) { // пълно съвпадение case insensitive
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.RN_DELO) = :rnDelo ");
				sqlParams.put("rnDelo", t);

			} else {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.RN_DELO) like :rnDelo ");
				sqlParams.put("rnDelo", "%" + t + "%");
			}
		}

		if (this.codeRefLeadList != null && !this.codeRefLeadList.isEmpty()) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.CODE_REF_LEAD in (:codeRefLeadList) ");
			sqlParams.put("codeRefLeadList", this.codeRefLeadList);
		}

		if (this.deloTypeArr != null && this.deloTypeArr.length != 0) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DELO_TYPE in (:deloTypeList) ");
			sqlParams.put("deloTypeList", Arrays.asList(this.deloTypeArr));
		} else if (mode == 2) { // за пререгистрация не трябва да се връщат номенклатурните
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DELO_TYPE not in (:deloTypeNom) ");
			
			sqlParams.put("deloTypeNom", Arrays.asList(OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM, OmbConstants.CODE_ZNACHENIE_DELO_TYPE_JALBA
				, OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NPM, OmbConstants.CODE_ZNACHENIE_DELO_TYPE_SAMOS));
		}

		if (this.statusArr != null && this.statusArr.length != 0) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.STATUS in (:statusList) ");
			sqlParams.put("statusList", Arrays.asList(this.statusArr));

		} else if (mode == 0 || mode == 3) { // за актуализация или съхранение статуса е спрямо правата на усера ако нищо не е избрано

			if (!userData.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_UA)) {
				// ако няма достъп да поддържа архив не трянва и да вижда архивни
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.STATUS not in (:statusList) ");
				sqlParams.put("statusList", Arrays.asList(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_DA, OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_UA));
			}
		}

		if (this.deloDateFrom != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DELO_DATE >= :deloDateFrom ");
			sqlParams.put("deloDateFrom", DateUtils.startDate(this.deloDateFrom));
		}
		if (this.deloDateTo != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DELO_DATE <= :deloDateTo ");
			sqlParams.put("deloDateTo", DateUtils.endDate(this.deloDateTo));
		}

		if (this.statusDateFrom != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.STATUS_DATE >= :statusDateFrom ");
			sqlParams.put("statusDateFrom", DateUtils.startDate(this.statusDateFrom));
		}
		if (this.statusDateTo != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.STATUS_DATE <= :statusDateTo ");
			sqlParams.put("statusDateTo", DateUtils.endDate(this.statusDateTo));
		}

		if (Boolean.TRUE.equals(this.notIncluded)) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " NOT EXISTS ");
			sqlWhere.append(" (select dd.ID from DELO_DELO dd where dd.INPUT_DELO_ID = d.DELO_ID ) ");
		}

		t = SearchUtils.trimToNULL_Upper(this.deloName);
		if (t != null && !isFullText) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.DELO_NAME) like :deloName ");
			sqlParams.put("deloName", "%" + t + "%");
		}
		t = SearchUtils.trimToNULL_Upper(this.deloInfo);
		if (t != null && !isFullText) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.DELO_INFO) like :deloInfo ");
			sqlParams.put("deloInfo", "%" + t + "%");
		}
		
		if (this.enableStorageSearch) { // ще се изпълнява търсене за дейност съхранение на преписки
			sqlFrom.append(" left outer join DELO_STORAGE ds on ds.DELO_ID = d.DELO_ID ");
			
			t = SearchUtils.trimToNULL_Upper(this.room);
			if (t != null) {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(ds.ROOM) like :room ");
				sqlParams.put("room", "%" + t + "%");
			}
			
			t = SearchUtils.trimToNULL_Upper(this.shkaf);
			if (t != null) {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(ds.SHKAF) like :shkaf ");
				sqlParams.put("shkaf", "%" + t + "%");
			}
			
			t = SearchUtils.trimToNULL_Upper(this.stillage);
			if (t != null) {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(ds.STILLAGE) like :stillage ");
				sqlParams.put("stillage", "%" + t + "%");
			}
			
			t = SearchUtils.trimToNULL_Upper(this.box);
			if (t != null) {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(ds.BOX) like :box ");
				sqlParams.put("box", "%" + t + "%");
			}
			
			if (this.storage != null) {
				if (this.storage.booleanValue()) { // трябва да има
					sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " ds.ID is not null ");
					
				} else { // трябва да няма ред за съхранението
					sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " ds.ID is null ");
				}
			}

			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " EXISTS ( ");
			sqlWhere.append(" select t1.DELO_ID from DELO_DOC t1 where t1.DELO_ID = d.DELO_ID and t1.TOM_NOMER is not null union ");
			sqlWhere.append(" select t2.DELO_ID from DELO_DELO t2 where t2.DELO_ID = d.DELO_ID and t2.TOM_NOMER is not null union ");
			sqlWhere.append(" select t3.DELO_ID from DELO_STORAGE t3 where t3.DELO_ID = d.DELO_ID ");
			sqlWhere.append(" ) ");
		}
		if (mode == 5) {//LM
//			where.append((where.length() == 0 ? WHERE : AND) + " d.WITH_TOM=1 ");
			sqlFrom.append("LEFT OUTER  join delo_doc ddoc on ddoc.DELO_ID =d.DELO_ID and ddoc.TOM_NOMER IS NOT NULL ");
			sqlFrom.append("LEFT OUTER  join delo_delo dd on dd.DELO_ID =d.DELO_ID and dd.TOM_NOMER IS NOT NULL ");
			
		}
		
		// аргументи свързани с движението
		t = trimToNULL_Upper(this.dvijToText);
		if (Boolean.TRUE.equals(this.dvijNotReturned)) {
			sqlFrom.append(" inner join DELO_DVIJ dvij on dvij.DELO_ID = d.DELO_ID and dvij.RETURN_DATE is null ");
			if (t != null) {
				sqlFrom.append(" and upper(dvij.DVIJ_TEXT) like :dvijToText ");
				sqlParams.put("dvijToText", "%" + t + "%");
			}
			
		} else if (t != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " EXISTS (select dvij.ID from DELO_DVIJ dvij where dvij.DELO_ID = d.DELO_ID and upper(dvij.DVIJ_TEXT) like :dvijToText) ");
			sqlParams.put("dvijToText", "%" + t + "%");
		}
		
		if (this.checkUserAccess != null) {
			appendCheckUserAccessSql(sqlWhere, sqlParams);
		}

		if (useDost) {
			addAccessRules(sqlWhere, sqlFrom, sqlParams, (UserData)userData, mode==1);
		}

		if (joinLock) {
			sqlFrom.append(" left outer join LOCK_OBJECTS z on z.OBJECT_TIP = :zTip and z.OBJECT_ID = d.DELO_ID and z.USER_ID != :zUser ");
			sqlParams.put("zTip", OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO);
			sqlParams.put("zUser", userData.getUserId());
		}

		super.setSql(sqlSelect.toString() + sqlFrom.toString() + sqlWhere.toString());
		super.setSqlCount(" select count(distinct d.DELO_ID) " + sqlFrom.toString() + sqlWhere.toString());
		setSqlParameters(sqlParams);
		
	}
	
	/**
	 * Пълнотекстово търсене
	 * 
	 * Използва се от основния екран за търсене на преписки и дела <br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида: <br>
	 * [0]-DELO_ID<br>
	 * [1]-RN_DELO<br>
	 * [2]-DELO_DATE<br>
	 * [3]-DELO_TYPE<br>
	 * [4]-STATUS<br>
	 * [5]-CODE_REF_LEAD<br>
	 * [6]-DELO_NAME (String)<br>
	 *
	 * @param userData това е този които изпълнява търсенето
	 */
	public void buildQueryDeloListFullText(BaseUserData userData,int mode) {
		Map<String, String> searchFields = new HashMap<>();
		Map<String, Object> filterFields = new HashMap<>();
		
		buildQueryDeloListInner(userData,mode, true);
		
		String firstCol = "d.DELO_ID";
		if (Boolean.TRUE.equals(this.dvijNotReturned)) {
			firstCol = "dvij.ID";
		}
		
		filterFields.put(Delo.FilterFields.registraturaId.name(),new HashSet());
		if (this.registraturaId != null) {
			((Collection)filterFields.get(Delo.FilterFields.registraturaId.name())).add(this.registraturaId);
		}
		
		String t = trimToNULL_Upper(this.deloName);
		if (t != null) {
			searchFields.put(Delo.FullTextFields.deloName.name(), t);
		}

		t = trimToNULL_Upper(this.deloInfo); 
		if (t != null) {
			searchFields.put(Delo.FullTextFields.deloInfo.name(), t);
		}
		
		if (this.deloDateFrom != null) {
			filterFields.put(Delo.FilterFields.deloDateFrom.name(),DateUtils.startDate(this.deloDateFrom));
		}
		if (this.deloDateTo != null) {
			filterFields.put(Delo.FilterFields.deloDateTo.name(),DateUtils.endDate(this.deloDateTo));
		}
		
		if(sqlParams.get("registraturaId")!=null){
			((Collection) filterFields.get(Delo.FilterFields.registraturaId.name())).add(sqlParams.get("registraturaId"));
		}

		Map<Integer, Map<Integer, Boolean>> accessValues = userData.getAccessValues();
		if(accessValues!=null) {
			((Collection) filterFields.get(Delo.FilterFields.registraturaId.name())).addAll(accessValues.get(CODE_CLASSIF_REGISTRATURI_OBJACCESS).keySet());
		}

		if(searchFields.isEmpty()) {
			return;
		} else {
			Set<Integer> result = new DocFulltextSearch().searchDeloFullText(filterFields, searchFields);
			Set<Integer> deloIds = null;
			try {
				if (result != null && !result.isEmpty()) {
					deloIds = getDeloIdListSQL(result, sqlFrom.toString() + sqlWhere.toString(), sqlParams);
				}
			} catch (InvalidParameterException e) {
				JSFUtils.addErrorMessage(e.getMessage());
			}

			if (deloIds != null && !deloIds.isEmpty()) {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DELO_ID in (:deloIdList) ");
				sqlParams.put("deloIdList", deloIds);
			} else {
				//зададено е условие, но NOSQL-а не е върнал резултат, тогава и SQL-а няма да върне
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DELO_ID in (-1) ");
			}
		}

		super.setSql(sqlSelect.toString() + sqlFrom.toString() + sqlWhere.toString());
		super.setSqlCount(" select count(distinct "+firstCol+") " + sqlFrom.toString() + sqlWhere.toString());
		setSqlParameters(sqlParams);
	}
	
	private Set<Integer> getDeloIdListSQL(Set<Integer> result, String sql, Map<String, Object> params) throws InvalidParameterException {
		Query querySelect = JPA.getUtil().getEntityManager().createNativeQuery("select d.DELO_ID " + sql);
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
	
	

	/** @return the codeRefLeadList */
	public List<Integer> getCodeRefLeadList() {
		return this.codeRefLeadList;
	}

	/** @return the deloDateFrom */
	public Date getDeloDateFrom() {
		return this.deloDateFrom;
	}

	/** @return the deloDateTo */
	public Date getDeloDateTo() {
		return this.deloDateTo;
	}

	/** @return the deloInfo */
	public String getDeloInfo() {
		return this.deloInfo;
	}

	/** @return the deloName */
	public String getDeloName() {
		return this.deloName;
	}

	/** @return the deloTypeArr */
	public Integer[] getDeloTypeArr() {
		return this.deloTypeArr;
	}

	/** @return the notRelDeloId */
	public Integer getNotRelDeloId() {
		return this.notRelDeloId;
	}

	/** @return the notRelDocId */
	public Integer getNotRelDocId() {
		return this.notRelDocId;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @return the rnDelo */
	public String getRnDelo() {
		return this.rnDelo;
	}

	/** @return the statusArr */
	public Integer[] getStatusArr() {
		return this.statusArr;
	}

	/** @return the statusDateFrom */
	public Date getStatusDateFrom() {
		return this.statusDateFrom;
	}

	/** @return the statusDateTo */
	public Date getStatusDateTo() {
		return this.statusDateTo;
	}

	/** @return the rnDeloEQ */
	public boolean isRnDeloEQ() {
		return this.rnDeloEQ;
	}

	/** @param codeRefLeadList the codeRefLeadList to set */
	public void setCodeRefLeadList(List<Integer> codeRefLeadList) {
		this.codeRefLeadList = codeRefLeadList;
	}

	/** @param deloDateFrom the deloDateFrom to set */
	public void setDeloDateFrom(Date deloDateFrom) {
		this.deloDateFrom = deloDateFrom;
	}

	/** @param deloDateTo the deloDateTo to set */
	public void setDeloDateTo(Date deloDateTo) {
		this.deloDateTo = deloDateTo;
	}

	/** @param deloInfo the deloInfo to set */
	public void setDeloInfo(String deloInfo) {
		this.deloInfo = deloInfo;
	}

	/** @param deloName the deloName to set */
	public void setDeloName(String deloName) {
		this.deloName = deloName;
	}

	/** @param deloTypeArr the deloTypeArr to set */
	public void setDeloTypeArr(Integer[] deloTypeArr) {
		this.deloTypeArr = deloTypeArr;
	}

	/** @param notRelDeloId the notRelDeloId to set */
	public void setNotRelDeloId(Integer notRelDeloId) {
		this.notRelDeloId = notRelDeloId;
	}

	/** @param notRelDocId the notRelDocId to set */
	public void setNotRelDocId(Integer notRelDocId) {
		this.notRelDocId = notRelDocId;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** @param rnDelo the rnDelo to set */
	public void setRnDelo(String rnDelo) {
		this.rnDelo = rnDelo;
	}

	/** @param rnDeloEQ the rnDeloEQ to set */
	public void setRnDeloEQ(boolean rnDeloEQ) {
		this.rnDeloEQ = rnDeloEQ;
	}

	/** @param statusArr the statusArr to set */
	public void setStatusArr(Integer[] statusArr) {
		this.statusArr = statusArr;
	}

	/** @param statusDateFrom the statusDateFrom to set */
	public void setStatusDateFrom(Date statusDateFrom) {
		this.statusDateFrom = statusDateFrom;
	}

	/** @param statusDateTo the statusDateTo to set */
	public void setStatusDateTo(Date statusDateTo) {
		this.statusDateTo = statusDateTo;
	}

	/** @return the notInProtocolId */
	public Integer getNotInProtocolId() {
		return this.notInProtocolId;
	}

	/** @param notInProtocolId the notInProtocolId to set */
	public void setNotInProtocolId(Integer notInProtocolId) {
		this.notInProtocolId = notInProtocolId;
	}
	
	/** @return the notIncluded */
	public Boolean getNotIncluded() {
		return this.notIncluded;
	}

	/** @param notIncluded the notIncluded to set */
	public void setNotIncluded(Boolean notIncluded) {
		this.notIncluded = notIncluded;
	}

	/** @return the enableStorageSearch */
	public boolean isEnableStorageSearch() {
		return this.enableStorageSearch;
	}

	/** @param enableStorageSearch the enableStorageSearch to set */
	public void setEnableStorageSearch(boolean enableStorageSearch) {
		this.enableStorageSearch = enableStorageSearch;
	}

	/** @return the room */
	public String getRoom() {
		return this.room;
	}

	/** @param room the room to set */
	public void setRoom(String room) {
		this.room = room;
	}

	/** @return the shkaf */
	public String getShkaf() {
		return this.shkaf;
	}

	/** @param shkaf the shkaf to set */
	public void setShkaf(String shkaf) {
		this.shkaf = shkaf;
	}

	/** @return the stillage */
	public String getStillage() {
		return this.stillage;
	}

	/** @param stillage the stillage to set */
	public void setStillage(String stillage) {
		this.stillage = stillage;
	}

	/** @return the box */
	public String getBox() {
		return this.box;
	}

	/** @param box the box to set */
	public void setBox(String box) {
		this.box = box;
	}

	/** @return the storage */
	public Boolean getStorage() {
		return this.storage;
	}

	/** @param storage the storage to set */
	public void setStorage(Boolean storage) {
		this.storage = storage;
	}

	/** @return the notInTip */
	public Integer getNotInTip() {
		return this.notInTip;
	}

	/** @param notInTip the notInTip to set */
	public void setNotInTip(Integer notInTip) {
		this.notInTip = notInTip;
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

	/** Добава условия в зависимост от параметрите подадени в дейността за отнемане на достъп
	 * @param where
	 */
	private void appendCheckUserAccessSql(StringBuilder where, Map<String, Object> params) {
		
		if (this.checkUserAccess[2] == null) { // иска се да има достъп

			where.append((where.length() == 0 ? WHERE : AND) + " EXISTS ");
			where.append(" ( select dal.DELO_ID from DELO_ACCESS_ALL dal where dal.DELO_ID = d.DELO_ID and ");
			
			if (this.checkUserAccess[3] == null) { // обикновен усер
				where.append(" dal.CODE_REF in (" + this.checkUserAccess[0] + "," + this.checkUserAccess[1] + ") ) ");	

			} else { // ръководител
				where.append(" ( CODE_REF in (" + this.checkUserAccess[0] + "," + this.checkUserAccess[1] + ")");
				where.append(" or CODE_STRUCT in (" + this.checkUserAccess[3] + ") ) ) ");
			}
			
			// и да не му е отнет
			where.append(" and NOT EXISTS ( select da.DELO_ID from DELO_ACCESS da where da.DELO_ID = d.DELO_ID and da.CODE_REF = :cuacr and da.EXCLUDE = 1 ) ");
			
		} else { // иска се да му е отнет
			where.append((where.length() == 0 ? WHERE : AND) //
				+ " EXISTS ( select da.DELO_ID from DELO_ACCESS da where da.DELO_ID = d.DELO_ID and da.CODE_REF = :cuacr and da.EXCLUDE = 1 ) ");
		}

		params.put("cuacr", this.checkUserAccess[0]);
	}

	/** Метод за добавяне на достъпа до докумнета
	 * @param where - Условия
	 * @param from - таблици
	 * @param userData - обкет от UserData
	 * @param viewMode
	 */
	void addAccessRules(StringBuilder where, StringBuilder from, Map<String, Object> params, UserData userData, boolean viewMode) {
		
		if (userData.isDeloAccessDenied()) { // има преписки, до които му е отказан достъпа
			
			from.append(" left outer join DELO_ACCESS denied on denied.DELO_ID = d.DELO_ID and denied.CODE_REF = "+userData.getUserAccess()+" and denied.EXCLUDE = 1 ");
			where.append((where.length() == 0 ? WHERE : AND) + " denied.ID is null ");
		}
		
		if (viewMode) {
			if (userData.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW)
				|| userData.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT)) {
				// режим преглед - пълен достъп

//				if (this.registraturaId == null) { // не е избрана и трябва да се върнат всички преписки от позволените му регсистратури 
//													// + !!! преписките, до които има достъп, който може да са от други регистратури !!!
//	
//					where.append((where.length() == 0 ? WHERE : AND) + " (d.REGISTRATURA_ID in (:viewRegistraturiSet) "); // позволените
//					params.put("viewRegistraturiSet", userData.getAccessValues().get(CODE_CLASSIF_REGISTRATURI_OBJACCESS).keySet());
//					
//					// + тези до които има достъп
//					where.append(" or EXISTS (select dost.DELO_ID from DELO_ACCESS_ALL dost where dost.DELO_ID = d.DELO_ID and ");
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
		from.append(accessJoin + " DELO_ACCESS_ALL on DELO_ACCESS_ALL.DELO_ID = d.DELO_ID ");

		StringBuilder dop = new StringBuilder();
	    dop.append("DELO_ACCESS_ALL.CODE_REF in (" + userData.getUserAccess() + "," + userData.getZveno() + ")");

	    if (viewMode && userData.getAccessZvenoList() != null) { // ако е шеф и то само за режим преглед
	    	dop.append(" OR DELO_ACCESS_ALL.CODE_STRUCT in (" +  userData.getAccessZvenoList() + ")") ;
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
	
	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new  SystemJournal();				
		dj.setCodeObject(OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO);
		//dj.setIdObject(getId());
		//dj.setIdentObject("Търсене на документи");
		return dj;
	}



	/** @return the addDeloAccess */
	public boolean isAddDeloAccess() {
		return this.addDeloAccess;
	}

	/** @param addDeloAccess the addDeloAccess to set */
	public void setAddDeloAccess(boolean addDeloAccess) {
		this.addDeloAccess = addDeloAccess;
	}
}
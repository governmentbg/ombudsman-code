package com.ib.omb.search;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS;
import static com.ib.system.utils.SearchUtils.trimToNULL;
import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Query;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.components.TaskData;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dto.TaskOcr;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.BaseUserData;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * Търсене на задачи
 *
 * @author belev
 */
public class TaskSearch extends SelectMetadata {

	/**  */
	private static final String AND = " and ";
	/**  */
	private static final String WHERE = " where ";

	/**  */
	private static final long serialVersionUID = 6198203775464191186L;

	private Integer			registraturaId;
	private Boolean			fullEditInRegistratura;	// всички задачи на регистратурата, като може да се клиа само ако потребителя
													// има дефинитивно право "Пълен достъп за актуализация на задачи"

	private Integer	docId;	// за задачи в контекста на док
	private Integer	taskId;	// за конкретна задача състояния

	private String	rnTask;
	private boolean	rnTaskEQ = true;	// ако е true се търси по пълно съвпадение по номер на задача

	private List<Integer>	taskTypeList;
	private List<Integer>	statusList;
	private List<Integer>	endOpinionList;
	private String			taskInfo;		// описание

	// период на дата на регистрация
	private Date	regDateFrom;
	private Date	regDateTo;

	// период на дата на възлагане
	private Date	assignDateFrom;
	private Date	assignDateTo;

	// период на срок за завършване
	private Date	srokDateFrom;
	private Date	srokDateTo;
	private Boolean	withoutSrok;	// задачи без срок

	// период на дата на статус
	private Date	statusDateFrom;
	private Date	statusDateTo;

	private Boolean	subordinates;	// задачи на мои подчинени
	private Boolean	withDoc;		// задачи с документ
	private Boolean	withoutDoc;		// задачи без документ

	private List<Integer>	codeAssignList;		// възложители
	private List<Integer>	codeControlList;	// контролиращи
	private List<Integer>	codeExecList;		// изпълнители
	private Boolean			execOtgovoren;		// при true се търси подадените да са само отговорни
	private List<Integer>	userStatusList;		// Служител, определил статуса (Множествен избор)

	// когато се искат задачи които са за документи
	private String	rnDoc;
	private boolean	rnDocEQ = true;	// ако е true се търси по пълно съвпадение по номер на документ
	private Date	docDateFrom;
	private Date	docDateTo;

	private boolean checkAccess = true; // да се проверява ли достъпа през референтите в задачата
	
	private StringBuilder sqlSelect ;
	private StringBuilder sqlFrom ;
	private StringBuilder sqlWhere ;
	private Map<String, Object> sqlParams;
	private String statusComments;
	
	private String	fileText; //текст на документа
	
	/**
	 * Винаги се търси в контекста на регистратура. Ако все пак се иска без регистратура да се подаде NULL в конструктора
	 *
	 * @param registraturaId
	 */
	public TaskSearch(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/**
	 * Използва се от основния екран за търсене на задачи <br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида: <br>
	 * [0]-TASK_ID<br>
	 * [1]-RN_TASK<br>
	 * [2]-TASK_TYPE<br>
	 * [3]-STATUS<br>
	 * [4]-SROK_DATE<br>
	 * [5]-ASSIGN_DATE<br>
	 * [6]-COMMENTS<br>STATUS_COMMENTS
	 * [7]-STATUS_USER_ID<br>
	 * [8]-CODE_ASSIGN<br>
	 * [9]-CODE_EXECS-кодовете на изпълнителите с разделител ',' (5,2,20)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date)}<br>
	 * [10]-TASK_INFO<br>
	 * [11]-DOC_ID<br>
	 * [12]-RN_DOC<br>
	 * [13]-DOC_DATE<br>
	 * [14]-DOC_VID<br>
	 * [15]-LOCK_USER<br>
	 * [16]-LOCK_DATE<br>
	 * [17]-REGISTRATURA_ID<br>
	 * [18]-END_OPINION<br>
	 * [19]-STATUS_USER_ID<br>
	 * [20]-STATUS_DATE<br>
	 *
	 * @param userData това е този които изпълнява търсенето
	 * @param viewMode <code>true</code>-разглеждане, <code>false</code>-актуализация
	 */
	public void buildQueryTaskList(BaseUserData userData, boolean viewMode) {
		
		buildQueryTaskListInner(userData, viewMode, false);
		
	}
	
	
	private void buildQueryTaskListInner(BaseUserData userData, boolean viewMode, boolean isFullText) {
		
		String dialect = JPA.getUtil().getDbVendorName();

		sqlParams = new HashMap<>();

		sqlSelect = new StringBuilder();
		sqlFrom = new StringBuilder();
		sqlWhere = new StringBuilder();

		sqlSelect.append(" select t.TASK_ID a0, t.RN_TASK a1, t.TASK_TYPE a2, t.STATUS a3, t.SROK_DATE a4, t.ASSIGN_DATE a5, ");
		sqlSelect.append(DialectConstructor.limitBigString(dialect, "t.STATUS_COMMENTS", 300) + " a6, t.STATUS_USER_ID a7, t.CODE_ASSIGN a8, ");
		sqlSelect.append(DialectConstructor.convertToDelimitedString(dialect, "tr.CODE_REF", "TASK_REFERENTS tr where tr.TASK_ID = t.TASK_ID", "tr.ROLE_REF, tr.ID") + " a9, ");
		sqlSelect.append(DialectConstructor.limitBigString(dialect, "t.TASK_INFO", 300) + " a10, d.DOC_ID a11, "+DocDAO.formRnDocSelect("d.", dialect)+" a12, d.DOC_DATE a13, d.DOC_VID a14 ");
		sqlSelect.append(", z.USER_ID a15, z.LOCK_DATE a16, t.REGISTRATURA_ID a17, t.END_OPINION a18, t.STATUS_USER_ID a19, t.STATUS_DATE a20 ");

		sqlFrom.append(" from TASK t ");

//		if (this.registraturaId != null) {
//			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.REGISTRATURA_ID = :registraturaId ");
//			sqlParams.put("registraturaId", this.registraturaId);
//		}

		String t = trimToNULL(this.rnTask);
		if (t != null) {
			if (this.rnTaskEQ) { // пълно съвпадение
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.RN_TASK = :rnTask ");
				sqlParams.put("rnTask", t);

			} else {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(t.RN_TASK) like :rnTask ");
				sqlParams.put("rnTask", "%" + t.toUpperCase() + "%");
			}
		}

		if (this.taskTypeList != null && !this.taskTypeList.isEmpty()) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.TASK_TYPE in (:taskTypeList) ");
			sqlParams.put("taskTypeList", this.taskTypeList);
		}
		if (this.statusList != null && !this.statusList.isEmpty()) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.STATUS in (:statusList) ");
			sqlParams.put("statusList", this.statusList);
		}
		if (this.endOpinionList != null && !this.endOpinionList.isEmpty()) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.end_opinion in (:endOpinionList) ");
			sqlParams.put("endOpinionList", this.endOpinionList);
		}
		t = SearchUtils.trimToNULL_Upper(this.statusComments);
		if (t != null && !isFullText) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(t.STATUS_COMMENTS) like :statusComments ");
			sqlParams.put("statusComments", "%" + t + "%");
		}
		
		t = SearchUtils.trimToNULL_Upper(this.taskInfo);
		if (t != null && !isFullText) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(t.TASK_INFO) like :taskInfo ");
			sqlParams.put("taskInfo", "%" + t + "%");
		}

		if (this.regDateFrom != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.DATE_REG >= :regDateFrom ");
			sqlParams.put("regDateFrom", DateUtils.startDate(this.regDateFrom));
		}
		if (this.regDateTo != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.DATE_REG <= :regDateTo ");
			sqlParams.put("regDateTo", DateUtils.endDate(this.regDateTo));
		}

		if (this.assignDateFrom != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.ASSIGN_DATE >= :assignDateFrom ");
			sqlParams.put("assignDateFrom", DateUtils.startDate(this.assignDateFrom));
		}
		if (this.assignDateTo != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.ASSIGN_DATE <= :assignDateTo ");
			sqlParams.put("assignDateTo", DateUtils.endDate(this.assignDateTo));
		}

		if (Boolean.TRUE.equals(this.withoutSrok)) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.SROK_DATE is NULL ");
		} else {
			if (this.srokDateFrom != null) {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.SROK_DATE >= :srokDateFrom ");
				sqlParams.put("srokDateFrom", DateUtils.startDate(this.srokDateFrom));
			}
			if (this.srokDateTo != null) {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.SROK_DATE <= :srokDateTo ");
				sqlParams.put("srokDateTo", DateUtils.endDate(this.srokDateTo));
			}
		}

		if (this.statusDateFrom != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.STATUS_DATE >= :statusDateFrom ");
			sqlParams.put("statusDateFrom", DateUtils.startDate(this.statusDateFrom));
		}
		if (this.statusDateTo != null) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.STATUS_DATE <= :statusDateTo ");
			sqlParams.put("statusDateTo", DateUtils.endDate(this.statusDateTo));
		}

		if (Boolean.TRUE.equals(this.withoutDoc)) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.DOC_ID is NULL ");
		}
		if (Boolean.TRUE.equals(this.withDoc)) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.DOC_ID is not NULL ");
		}

		if (this.codeAssignList != null && !this.codeAssignList.isEmpty()) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.CODE_ASSIGN in (:codeAssignList) ");
			sqlParams.put("codeAssignList", this.codeAssignList);
		}
		if (this.codeControlList != null && !this.codeControlList.isEmpty()) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.CODE_CONTROL in (:codeControlList) ");
			sqlParams.put("codeControlList", this.codeControlList);
		}
		if (this.userStatusList != null && !this.userStatusList.isEmpty()) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.STATUS_USER_ID in (:userStatusList) ");
			sqlParams.put("userStatusList", this.userStatusList);
		}

		if (this.codeExecList != null && !this.codeExecList.isEmpty()) {
			sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " EXISTS ");

			sqlWhere.append(" (select tr.ID from TASK_REFERENTS tr where tr.TASK_ID = t.TASK_ID and tr.CODE_REF in (:codeExecList) ");
			sqlParams.put("codeExecList", this.codeExecList);

			if (Boolean.TRUE.equals(this.execOtgovoren)) {
				sqlWhere.append(" and tr.ROLE_REF = :execOtgovoren ");
				sqlParams.put("execOtgovoren", OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG);
			}
			sqlWhere.append(" ) "); // това затваря EXISTS !
		}

		boolean addDocJoin = false; // ако има някакво условие по документа ще се вдигне флага, за да не се омаже count()-та

		t = SearchUtils.trimToNULL_Upper(this.rnDoc);
		if (t != null || this.docDateFrom != null || this.docDateTo != null) { // по дока в задачата
			sqlFrom.append(" inner join DOC d on d.DOC_ID = t.DOC_ID ");
			addDocJoin = true;

			if (t != null) {
				if (this.rnDocEQ) { // пълно съвпадение case insensitive
					sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.RN_DOC) = :rnDoc ");
					sqlParams.put("rnDoc", t);

				} else {
					sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " upper(d.RN_DOC) like :rnDoc ");
					sqlParams.put("rnDoc", "%" + t + "%");
				}
			}
			if (this.docDateFrom != null) {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DOC_DATE >= :docDateFrom ");
				sqlParams.put("docDateFrom", DateUtils.startDate(this.docDateFrom));
			}
			if (this.docDateTo != null) {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " d.DOC_DATE <= :docDateTo ");
				sqlParams.put("docDateTo", DateUtils.endDate(this.docDateTo));
			}
		}

		UserData ud = (UserData) userData;

		if (viewMode && Boolean.TRUE.equals(this.subordinates) && ud.getAccessZvenoList() != null) {

			addViewZvenoListAccessRules(sqlWhere, sqlParams, ud); // задачи на мои подчинени, само в разглеждане и само за шефове
		} else if (this.checkAccess) { // все пак може да е изключен

			addAccessRules(sqlWhere, sqlParams, ud, viewMode); // налагат се правата на логнатия
		}

		sqlFrom.append(" left outer join LOCK_OBJECTS z on z.OBJECT_TIP = :zTip and z.OBJECT_ID = t.TASK_ID and z.USER_ID != :zUser ");
		sqlParams.put("zTip", OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK);
		sqlParams.put("zUser", userData.getUserId());

		setSqlCount(" select count(distinct t.TASK_ID) " + sqlFrom + sqlWhere); // на този етап бройката е готова

		if (!addDocJoin) { // за да не се дублира че щя гръмне
			// !!! тъй като по DOC не се слага where този join не влияе на count() и за да не товари излишно го слагам тук !!!
			sqlFrom.append(" left outer join DOC d on d.DOC_ID = t.DOC_ID ");
		}

		setSql(sqlSelect.toString() + sqlFrom.toString() + sqlWhere.toString());
		setSqlParameters(sqlParams);
		
	}

	/**
	 * Метод за пълнотекстово търсене 
	 * 
	 * Използва се от основния екран за търсене на задачи <br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида: <br>
	 * [0]-TASK_ID<br>
	 * [1]-RN_TASK<br>
	 * [2]-TASK_TYPE<br>
	 * [3]-STATUS<br>
	 * [4]-SROK_DATE<br>
	 * [5]-ASSIGN_DATE<br>
	 * [6]-COMMENTS<br>
	 * [7]-STATUS_USER_ID<br>
	 * [8]-CODE_ASSIGN<br>
	 * [9]-CODE_EXECS-кодовете на изпълнителите с разделител ',' (5,2,20)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date)}<br>
	 * [10]-TASK_INFO<br>
	 * [11]-DOC_ID<br>
	 * [12]-RN_DOC<br>
	 * [13]-DOC_DATE<br>
	 * [14]-DOC_VID<br>
	 *
	 * @param userData това е този които изпълнява търсенето
	 */
	public void buildQueryTaskListWithFullText(BaseUserData userData, boolean viewMode) {
		
		Map<String, String> searchFields = new HashMap<>();
		Map<String, Object> filterFields = new HashMap<>();
		
		buildQueryTaskListInner(userData, viewMode, true);	
		
		filterFields.put(TaskOcr.FilterFields.registraturaId.name(),new HashSet());
		if (this.registraturaId != null) {
			((Collection)filterFields.get(TaskOcr.FilterFields.registraturaId.name())).add(this.registraturaId);
		}
		
		String t = trimToNULL_Upper(this.taskInfo);
		if (t != null) {
			searchFields.put(TaskOcr.FullTextFields.taskInfo.name(), t);
		}
		
		t = trimToNULL_Upper(this.statusComments);
		if (t != null) {
			searchFields.put(TaskOcr.FullTextFields.statusComments.name(), t);
		}

		t = trimToNULL_Upper(this.fileText);
		if (t != null) {
			searchFields.put(TaskOcr.FullTextFields.ocr.name(), t);
		}

		if (this.assignDateFrom != null) {
			filterFields.put(TaskOcr.FilterFields.assignDateFrom.name(),DateUtils.startDate(this.assignDateFrom));
		}
		if (this.assignDateTo != null) {
			filterFields.put(TaskOcr.FilterFields.assignDateTo.name(),DateUtils.endDate(this.assignDateTo));
		}
		
		if(sqlParams.get("registraturaId")!=null){
			((Collection) filterFields.get(TaskOcr.FilterFields.registraturaId.name())).add(sqlParams.get("registraturaId"));
		}

		Map<Integer, Map<Integer, Boolean>> accessValues = userData.getAccessValues();
		if (accessValues != null) {
			((Collection) filterFields.get(TaskOcr.FilterFields.registraturaId.name())).addAll(accessValues.get(CODE_CLASSIF_REGISTRATURI_OBJACCESS).keySet());
		}

		if(searchFields.isEmpty()) {
			return;
		} else {
			Set<Integer> result = new DocFulltextSearch().searchTaskFullText(filterFields, searchFields);
			Set<Integer> taskIds = null;
			try {
				if (result != null && !result.isEmpty()) {
					taskIds = getTaskIdListSQL(result, sqlFrom.toString() + sqlWhere.toString(), sqlParams);
				}
			} catch (InvalidParameterException e) {
				JSFUtils.addErrorMessage(e.getMessage());
			}

			if (taskIds != null && !taskIds.isEmpty()) {
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.TASK_ID in (:taskIdList) ");
				sqlParams.put("taskIdList", taskIds);
			} else {
				//зададено е условие, но NOSQL-а не е върнал резултат, тогава и SQL-а няма да върне
				sqlWhere.append((sqlWhere.length() == 0 ? WHERE : AND) + " t.TASK_ID in (-1) ");
			}
		}
		

		// TODO може да се сложи след count (*), но трябва да се сложат аргументите zTip и zUser директно в заявката
		setSql(sqlSelect.toString() + sqlFrom.toString() + sqlWhere.toString());
		setSqlCount(" select count(distinct t.TASK_ID) " + sqlFrom.toString() + sqlWhere.toString());
		setSqlParameters(sqlParams);
	}
	
	
	private Set<Integer> getTaskIdListSQL(Set<Integer> result, String sql, Map<String, Object> params) throws InvalidParameterException {
		Query querySelect = JPA.getUtil().getEntityManager().createNativeQuery("select t.TASK_ID " + sql);
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
	 * Търсене на задачи по документ. Използва се от компонентата {@link TaskData}<br>
	 * На база входните параметри подготвя селект за получаване на резултат от вида: <br>
	 * [0]-TASK_ID<br>
	 * [1]-RN_TASK<br>
	 * [2]-TASK_TYPE<br>
	 * [3]-STATUS<br>
	 * [4]-SROK_DATE<br>
	 * [5]-ASSIGN_DATE<br>
	 * [6]-CODE_EXECS-кодовете на изпълнителите с разделител ',' (5,2,20)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date)}<br>
	 * [7]-REGISTRATURA_ID<br>
	 * [8]-CODE_ASSIGN<br>
	 * [9]-STATUS_COMMENTS<br>
	 * [10]-END_OPINION<br>
	 * [11]-TASK_INFO<br>
	 * [12]-RN_DOC<br>
	 * [13]-DOC_DATE<br>
	 * [14]-END_DOC_ID<br>
	 * [15]-STATUS_USER_ID<br>
	 */
	public void buildQueryTasksInDoc() {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append(" select t.TASK_ID a0, t.RN_TASK a1, t.TASK_TYPE a2, t.STATUS a3, t.SROK_DATE a4, t.ASSIGN_DATE a5, ");
		select.append(DialectConstructor.convertToDelimitedString(dialect, "tr.CODE_REF", "TASK_REFERENTS tr where tr.TASK_ID = t.TASK_ID", "tr.ROLE_REF, tr.ID") + " a6, t.REGISTRATURA_ID a7, t.CODE_ASSIGN a8, ");
		select.append(DialectConstructor.limitBigString(dialect, "t.STATUS_COMMENTS", 300) + " a9, t.END_OPINION a10, "+DialectConstructor.limitBigString(dialect, "t.TASK_INFO", 300)+" a11 ");
		select.append(" , "+DocDAO.formRnDocSelect("d.", dialect)+" a12 ,"+DialectConstructor.convertSQLDateTimeToString(dialect,"d.DOC_DATE")+" a13, t.END_DOC_ID a14, t.STATUS_USER_ID a15 ");
		from.append(" from TASK t ");

//		if (this.registraturaId != null) {
//			where.append((where.length() == 0 ? WHERE : AND) + " t.REGISTRATURA_ID = :registraturaId ");
//			params.put("registraturaId", this.registraturaId);
//		}
		if (this.docId != null) {
			where.append((where.length() == 0 ? WHERE : AND) + " t.DOC_ID = :docId ");
			params.put("docId", this.docId);
		}

		setSqlCount(" select count(*) " + from + where);
		from.append("  LEFT JOIN DOC d ON d.DOC_ID = t.END_DOC_ID ");
		setSql(select.toString() + from.toString() + where.toString());
		setSqlParameters(params);
	}

	/** @return the assignDateFrom */
	public Date getAssignDateFrom() {
		return this.assignDateFrom;
	}

	/** @return the assignDateTo */
	public Date getAssignDateTo() {
		return this.assignDateTo;
	}

	/** @return the codeAssignList */
	public List<Integer> getCodeAssignList() {
		return this.codeAssignList;
	}

	/** @return the codeControlList */
	public List<Integer> getCodeControlList() {
		return this.codeControlList;
	}

	/** @return the codeExecList */
	public List<Integer> getCodeExecList() {
		return this.codeExecList;
	}

	/** @return the docDateFrom */
	public Date getDocDateFrom() {
		return this.docDateFrom;
	}

	/** @return the docDateTo */
	public Date getDocDateTo() {
		return this.docDateTo;
	}

	/** @return the docId */
	public Integer getDocId() {
		return this.docId;
	}

	/** @return the execOtgovoren */
	public Boolean getExecOtgovoren() {
		return this.execOtgovoren;
	}

	/** @return the fullEditInRegistratura */
	public Boolean getFullEditInRegistratura() {
		return this.fullEditInRegistratura;
	}

	/** @return the regDateFrom */
	public Date getRegDateFrom() {
		return this.regDateFrom;
	}

	/** @return the regDateTo */
	public Date getRegDateTo() {
		return this.regDateTo;
	}

	/** @return the registraturaId */
	public Integer getRegistraturaId() {
		return this.registraturaId;
	}

	/** @return the rnDoc */
	public String getRnDoc() {
		return this.rnDoc;
	}

	/** @return the rnTask */
	public String getRnTask() {
		return this.rnTask;
	}

	/** @return the srokDateFrom */
	public Date getSrokDateFrom() {
		return this.srokDateFrom;
	}

	/** @return the srokDateTo */
	public Date getSrokDateTo() {
		return this.srokDateTo;
	}

	/** @return the statusList */
	public List<Integer> getStatusList() {
		return this.statusList;
	}

	/** @return the subordinates */
	public Boolean getSubordinates() {
		return this.subordinates;
	}

	/** @return the taskId */
	public Integer getTaskId() {
		return this.taskId;
	}

	/** @return the taskInfo */
	public String getTaskInfo() {
		return this.taskInfo;
	}

	/** @return the taskTypeList */
	public List<Integer> getTaskTypeList() {
		return this.taskTypeList;
	}

	/** @return the withDoc */
	public Boolean getWithDoc() {
		return this.withDoc;
	}

	/** @return the withoutDoc */
	public Boolean getWithoutDoc() {
		return this.withoutDoc;
	}

	/** @return the withoutSrok */
	public Boolean getWithoutSrok() {
		return this.withoutSrok;
	}

	/** @return the checkAccess */
	public boolean isCheckAccess() {
		return this.checkAccess;
	}

	/** @return the rnDocEQ */
	public boolean isRnDocEQ() {
		return this.rnDocEQ;
	}

	/** @return the rnTaskEQ */
	public boolean isRnTaskEQ() {
		return this.rnTaskEQ;
	}

	/** @param assignDateFrom the assignDateFrom to set */
	public void setAssignDateFrom(Date assignDateFrom) {
		this.assignDateFrom = assignDateFrom;
	}

	/** @param assignDateTo the assignDateTo to set */
	public void setAssignDateTo(Date assignDateTo) {
		this.assignDateTo = assignDateTo;
	}

	/** @param checkAccess the checkAccess to set */
	public void setCheckAccess(boolean checkAccess) {
		this.checkAccess = checkAccess;
	}

	/** @param codeAssignList the codeAssignList to set */
	public void setCodeAssignList(List<Integer> codeAssignList) {
		this.codeAssignList = codeAssignList;
	}

	/** @param codeControlList the codeControlList to set */
	public void setCodeControlList(List<Integer> codeControlList) {
		this.codeControlList = codeControlList;
	}

	/** @param codeExecList the codeExecList to set */
	public void setCodeExecList(List<Integer> codeExecList) {
		this.codeExecList = codeExecList;
	}

	/** @param docDateFrom the docDateFrom to set */
	public void setDocDateFrom(Date docDateFrom) {
		this.docDateFrom = docDateFrom;
	}

	/** @param docDateTo the docDateTo to set */
	public void setDocDateTo(Date docDateTo) {
		this.docDateTo = docDateTo;
	}

	/** @param docId the docId to set */
	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	/** @param execOtgovoren the execOtgovoren to set */
	public void setExecOtgovoren(Boolean execOtgovoren) {
		this.execOtgovoren = execOtgovoren;
	}

	/** @param fullEditInRegistratura the fullEditInRegistratura to set */
	public void setFullEditInRegistratura(Boolean fullEditInRegistratura) {
		this.fullEditInRegistratura = fullEditInRegistratura;
	}

	/** @param regDateFrom the regDateFrom to set */
	public void setRegDateFrom(Date regDateFrom) {
		this.regDateFrom = regDateFrom;
	}

	/** @param regDateTo the regDateTo to set */
	public void setRegDateTo(Date regDateTo) {
		this.regDateTo = regDateTo;
	}

	/** @param registraturaId the registraturaId to set */
	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	/** @param rnDoc the rnDoc to set */
	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}

	/** @param rnDocEQ the rnDocEQ to set */
	public void setRnDocEQ(boolean rnDocEQ) {
		this.rnDocEQ = rnDocEQ;
	}

	/** @param rnTask the rnTask to set */
	public void setRnTask(String rnTask) {
		this.rnTask = rnTask;
	}

	/** @param rnTaskEQ the rnTaskEQ to set */
	public void setRnTaskEQ(boolean rnTaskEQ) {
		this.rnTaskEQ = rnTaskEQ;
	}

	/** @param srokDateFrom the srokDateFrom to set */
	public void setSrokDateFrom(Date srokDateFrom) {
		this.srokDateFrom = srokDateFrom;
	}

	/** @param srokDateTo the srokDateTo to set */
	public void setSrokDateTo(Date srokDateTo) {
		this.srokDateTo = srokDateTo;
	}

	/** @param statusList the statusList to set */
	public void setStatusList(List<Integer> statusList) {
		this.statusList = statusList;
	}

	/** @param subordinates the subordinates to set */
	public void setSubordinates(Boolean subordinates) {
		this.subordinates = subordinates;
	}

	/** @param taskId the taskId to set */
	public void setTaskId(Integer taskId) {
		this.taskId = taskId;
	}

	/** @param taskInfo the taskInfo to set */
	public void setTaskInfo(String taskInfo) {
		this.taskInfo = taskInfo;
	}

	/** @param taskTypeList the taskTypeList to set */
	public void setTaskTypeList(List<Integer> taskTypeList) {
		this.taskTypeList = taskTypeList;
	}

	/** @param withDoc the withDoc to set */
	public void setWithDoc(Boolean withDoc) {
		this.withDoc = withDoc;
	}

	/** @param withoutDoc the withoutDoc to set */
	public void setWithoutDoc(Boolean withoutDoc) {
		this.withoutDoc = withoutDoc;
	}

	/** @param withoutSrok the withoutSrok to set */
	public void setWithoutSrok(Boolean withoutSrok) {
		this.withoutSrok = withoutSrok;
	}

	private void addAccessRules(StringBuilder where, Map<String, Object> params, UserData userData, boolean viewMode) {
		if (viewMode && (userData.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW)
						|| userData.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_TASK_FULL_EDIT))) {

			if (this.registraturaId == null) { // трябва да се добави условие да е измежду позволените му регистратури

				where.append((where.length() == 0 ? WHERE : AND) + " ( t.REGISTRATURA_ID in (:viewRegistraturiSet) or ");
				
				// като има пълен достъп за разглеждане в избрани регистратури трябва да се добавят и задачите, в
				// които той е референт, защото може да са от други регистратури
				where.append(" ( t.USER_REG = :userReg or t.CODE_ASSIGN = :userAccess or t.CODE_CONTROL = :userAccess ");
				where.append(" or EXISTS (select tr.ID from TASK_REFERENTS tr where tr.TASK_ID = t.TASK_ID and tr.CODE_REF = :userAccess) ) ) ");

				params.put("userAccess", userData.getUserAccess());
				params.put("userReg", userData.getUserId());
				params.put("viewRegistraturiSet", userData.getAccessValues().get(CODE_CLASSIF_REGISTRATURI_OBJACCESS).keySet());
			}

			return; // пълен достъп за разглеждане за позволените му регистратури
		}
		if (Boolean.TRUE.equals(this.fullEditInRegistratura) && !viewMode // избрано в режим актуализация
			&& userData.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_TASK_FULL_EDIT)) {

			if (this.registraturaId == null) { // ако е има вече ще е сетнато и сме ОК
				where.append((where.length() == 0 ? WHERE : AND) + " t.REGISTRATURA_ID = :registraturaId ");
				params.put("registraturaId", userData.getRegistratura());
			}
			return; // пълен достъп за актуализация за неговата регистратура
		}

		if (this.codeAssignList != null && this.codeAssignList.size() == 1 && userData.getUserAccess().equals(this.codeAssignList.get(0))) {
			return; // текущия усер пита за себе си като възложител и няма смисъл какво повече да се налага
		}
		if (this.codeControlList != null && this.codeControlList.size() == 1 && userData.getUserAccess().equals(this.codeControlList.get(0))) {
			return; // текущия усер пита за себе си като контролиращ и няма смисъл какво повече да се налага
		}
		if (this.codeExecList != null && this.codeExecList.size() == 1 && userData.getUserAccess().equals(this.codeExecList.get(0))) {
			return; // текущия усер пита за себе си като изпълнител и няма смисъл какво повече да се налага
		}

		where.append(where.length() == 0 ? WHERE : AND);

		where.append(" ( t.USER_REG = :userReg or t.CODE_ASSIGN = :userAccess or t.CODE_CONTROL = :userAccess ");
		where.append(" or EXISTS (select tr.ID from TASK_REFERENTS tr where tr.TASK_ID = t.TASK_ID and tr.CODE_REF = :userAccess) ) ");

		params.put("userAccess", userData.getUserAccess());
		params.put("userReg", userData.getUserId());
	}

	private void addViewZvenoListAccessRules(StringBuilder where, Map<String, Object> params, UserData userData) {
		where.append(where.length() == 0 ? WHERE : AND);
		where.append(" EXISTS ( select ar.REF_ID, tr.ID ");
		where.append(" 			from ADM_REFERENTS ar, TASK_REFERENTS tr ");
		where.append(" 			where ar.CODE_PARENT in (" + userData.getAccessZvenoList() + ") and ar.REF_TYPE = :emplRefType and tr.TASK_ID = t.TASK_ID ");
		where.append(" 				and ar.CODE = tr.CODE_REF and tr.CODE_REF != " + userData.getUserAccess());
		where.append("              and ar.DATE_OT <= t.STATUS_DATE and ar.DATE_DO > t.STATUS_DATE ) ");

		params.put("emplRefType", OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL);

		if (this.registraturaId == null) { // трябва да се добави условие да е измежду позволените му регистратури

			where.append(" and t.REGISTRATURA_ID in (:viewRegistraturiSet) ");
			params.put("viewRegistraturiSet", userData.getAccessValues().get(CODE_CLASSIF_REGISTRATURI_OBJACCESS).keySet());
		}
	}

	public List<Integer> getEndOpinionList() {
		return endOpinionList;
	}

	public void setEndOpinionList(List<Integer> endOpinionList) {
		this.endOpinionList = endOpinionList;
	}

	public String getStatusComments() {
		return statusComments;
	}

	public void setStatusComments(String statusComments) {
		this.statusComments = statusComments;
	}

	public String getFileText() {
		return fileText;
	}

	public void setFileText(String fileText) {
		this.fileText = fileText;
	}

	/** @return the userStatusList */
	public List<Integer> getUserStatusList() {
		return this.userStatusList;
	}
	/** @param userStatusList the userStatusList to set */
	public void setUserStatusList(List<Integer> userStatusList) {
		this.userStatusList = userStatusList;
	}

	/** @return the statusDateFrom */
	public Date getStatusDateFrom() {
		return this.statusDateFrom;
	}
	/** @param statusDateFrom the statusDateFrom to set */
	public void setStatusDateFrom(Date statusDateFrom) {
		this.statusDateFrom = statusDateFrom;
	}
	/** @return the statusDateTo */
	public Date getStatusDateTo() {
		return this.statusDateTo;
	}
	/** @param statusDateTo the statusDateTo to set */
	public void setStatusDateTo(Date statusDateTo) {
		this.statusDateTo = statusDateTo;
	}
}
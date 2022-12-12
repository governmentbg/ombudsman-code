package com.ib.omb.beans;

import static com.ib.system.utils.SearchUtils.asInteger;

import java.io.Serializable;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.view.ViewScoped;
//	import org.omnifaces.cdi.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.primefaces.PrimeFaces;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.model.StreamedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelMailList;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.DocJalbaDAO;
import com.ib.omb.db.dao.EgovMessagesDAO;
import com.ib.omb.db.dao.LockObjectDAO;
import com.ib.omb.db.dao.TaskDAO;
import com.ib.omb.db.dao.UserNotificationsDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.EgovMessages;
import com.ib.omb.db.dto.Task;
import com.ib.omb.experimental.GlobalHolder;
import com.ib.omb.experimental.RegistratureDocHolder;
import com.ib.omb.notifications.PersonalMessages;
import com.ib.omb.search.DocJalbaSearch;
import com.ib.omb.search.TaskSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.omb.utils.DashboardUtils;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.mail.Mailer;
import com.ib.system.mail.MyMessage;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.SignatureUtils;
import com.ib.system.utils.VerifySignature;

/**
 * Работен плот
 * 
 */
@Named("dashboard")
@ViewScoped
public class Dashboard extends IndexUIbean implements Serializable {

	private static final long serialVersionUID = -505634514290701139L;

	private static final Logger LOGGER = LoggerFactory.getLogger(Dashboard.class);

	private LazyDataModelSQL2Array objectList;

	private Object[] selectedObject;

	private List<Files> filesListDoc = new ArrayList<>(); // spisyk s fajlove kym dokumnet

	private Integer idDoc; // identifikator na dokument
	private Integer idTask; // identifikator na zadacha
	private Date dateDoc;

	private String titleDoc; // sydyrja noomer i data na dokumenta izpolzwa se pri pokazvane na spisyk s
								// failove kym dok

	private UserData ud;

	private Integer selectedCodeMenuObject;

	private List<ColumnModel> columns;

	private int rows;

	private Map<Long, String> mapCountsOptions1;

	// aktivni statusi na zadachi
	private List<Integer> statusList;
	// statusi na izpylneni zatachi
	private List<Integer> statusTaskNotActive;
	// неизпълнени статуси на задачи
	private List<Integer> neizpalneniStatusList;
	// всички останали статуси от активните без неизпълнените
	private List<Integer> otherStatusList;

	private String statusTask;

	private boolean showTaskModuleOption;
	private boolean showDocTaskModuleOption;
	private boolean showDeloDocModuleOption;
	private boolean showJalbiOption;
	private boolean showProverkiOption;
	
	private boolean showDocTaskNew;
	
	private boolean showGroupDocTaskNew;
	
	private String docInfo;
	private String docCorespAvtor;

	private Task tmpTask;
	private List<SystemClassif> opinionLst;

	private boolean gotoDocPage;
	private boolean gotoJalbaPage;

	private List<String> mailBoxLst;

	private LazyDataModelMailList mailList;
	private MyMessage selectedMail;
	private String selectMailBox;
	private String textReject;

	@Inject
	private Flash flash;

	@Inject
	private GuestPreferencesDashboard gpd;

	private boolean displayModalSelObj;

	public static final String FA_SPINER = "<i class='fa fa-spinner fa-spin' ></i>";
	public static final String ID_TABLE_LIST = ":dashboard:tableList";
	public static final String MAIL_FOLDER_READ = "mail.folder.read";
	public static final String SORT_BY_A2 = "a2 desc";
	public static final String SORT_BY = "sortBy";
	public static final String GENERAL_EXCEPTION = "general.exception";
	public static final String TASK_EXPIRE_DAYS = "delo.taskExpireDays";
	public static final String GENERAL_FORMATEXC = "general.formatExc";
	
	public static final String DB_COL200 = "dbCol200";
	public static final String A1_DESC = "a1 desc";
	public static final String A2_DESC = "a2 desc";
	public static final String A5_DOC_SROK = "A5DOCSROK";
	
	
	private String textSearch;
	private Boolean textSearchFull;

	private boolean showForCompetence;
	private boolean forCompetence;
	private Doc doc = null;

	private String taskSendInfo;
	private String taskCommentInfo;
	private String taskMnenie;

	private boolean showNewTask;

	private Map<Integer, Integer> typeSignAction;

	private List<Object[]> taskHistoryList;

	private boolean showSignFileOptions;

	private boolean showFileUpload;

	private boolean showTaskSpr;

	private LazyDataModelSQL2Array tasksDocList;
	private List<Object[]> linkDocList;

	private Integer extDaysTask;

	private Integer idEgov;
	
	private Map<Long, Boolean> selectedDocs;
	
	private Map<Long, Map<Long , Boolean>> selectedTaskDocs;
	
	private List<Long> idDocs;	
	
	private boolean groupTaskAccess = false; 
	
	private boolean showGroupDocSign = false;

	
	private List<Integer> idTasks;	
	
	
	private StreamedContent streamFile;
	
	private Integer jalbaExpert;
	/**
	 * служители от  звено на потребителя, който работи в момента 
	 */
	private List<SystemClassif> zvenoSlList;
	
	private String titleRegDoc = "Обработка";
	
	private String allJlb;
	
	@PostConstruct
	void initData() {

		displayModalSelObj = false;

		if (gpd.isModalDisplay()) {
			displayModalSelObj = true;
		}

		rows = 5;

		ud = getUserData(UserData.class);

		mapCountsOptions1 = new HashMap<>();
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_REZOL), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_SAGL), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_PODPIS), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_NONREG), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_EXEC), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_ASSIGN), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_CONTROL), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_GOING_LATE), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_LATE), FA_SPINER);

		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_EMAIL), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_CEOC), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_EL_VR), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_NAS), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_REG_OF), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_DIF_REG), FA_SPINER);

		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DELO_DOC), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_TASK_DOC), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_TASK), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_COMPETENCE), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_ZA_ZAPOZNAVANE), FA_SPINER);
		
		
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_OBRABOTKA), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_OTGOVOR), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_PRECENKA), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_RAZPR), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_OPR_EXP), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_PROVERKA), FA_SPINER);
		
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_AKT_NPM), FA_SPINER);
		mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_AKT_SAMOS), FA_SPINER);
		
		// активни статуси на задачи
		statusList = new ArrayList<>();
		// всички останали статуси от активните без неизпълнените
		otherStatusList = new ArrayList<>();
		try {
			List<SystemClassif> rez = getSystemData(SystemData.class)
					.getSysClassification(OmbConstants.CODE_CLASSIF_TASK_STATUS_ACTIVE, new Date(), getCurrentLang());
			for (SystemClassif item : rez) {
				statusList.add(item.getCode()); // System.out.println("item.getCode() -> "+item.getCode());

				if (item.getCode() != OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP
						&& item.getCode() != OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEPRIETA) {
					otherStatusList.add(item.getCode());
				}
			}
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при извличнане на системна класификация 'Активни статуси на задача'! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		// статуси на изпълнени задачи
		statusTaskNotActive = new ArrayList<>();
		statusTaskNotActive.add(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP);
		statusTaskNotActive.add(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP_SROK);

		// статуси на неизпълнени задачи
		neizpalneniStatusList = new ArrayList<>();
		neizpalneniStatusList.add(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP);
		neizpalneniStatusList.add(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEPRIETA);

		// статуси на задача по подразбиране
		statusTask = "1"; // активни

		typeSignAction = new HashMap<>();
		typeSignAction.put(OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_SAGL, 1);
		typeSignAction.put(OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_PODPIS, 2);

		// init moduleOptions
		clearParams();
		
		allJlb = "1"; // всички жалби - по подразбиране
		// -------------
		try {
			PersonalMessages pm = (PersonalMessages) JSFUtils.getManagedBean("pMessages");
			pm.actionLoadMessages();
		} catch (Exception e) {
			LOGGER.error("Грешка при извличане на клас  PersonalMessages", e);
		}

		try {
			extDaysTask = Integer.valueOf(getSystemData().getSettingsValue(TASK_EXPIRE_DAYS));
		} catch (Exception e) {
			LOGGER.error("Грешка при извличане на настройка 'delo.taskExpireDays' ", e);
			extDaysTask = Integer.valueOf(7);
		}
		
		//право за поставяне на групови задачи - без бутонът за гр. задача!!!!  
		// groupTaskAccess = ud.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_GROUP_TASK);
		
		
//		//rest за извличане на ел. подписи - test
//		try {
//			String resutstr = SystemRestClient.getInstance("rest.client.other").get("certList", String.class);
//			
//			
//			System.out.println(resutstr);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}

	/**
	 * при излизане от страницата - отключва обекта и да го освобождава за
	 * актуализация от друг потребител
	 */
	@PreDestroy
	public void unlockDoc() {

		LockObjectDAO daoL = new LockObjectDAO();
		try {

			JPA.getUtil().runInTransaction(() -> daoL.unlock(ud.getUserId()));

		} catch (BaseException e) {
			LOGGER.error("Грешка при отключване на документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}

	}

	private void clearParams() {
		selectedObject = null;
		idDoc = null;
		setDocInfo(null);
		showDocTaskNew = false;
		showTaskModuleOption = false;
		showDocTaskModuleOption = false;
		showDeloDocModuleOption = false;
		showJalbiOption = false;
		showProverkiOption = false;
		
		tmpTask = null;
		gotoDocPage = false;
		gotoJalbaPage = false;
		dateDoc = null;
		mailList = null;
		objectList = null;
		selectMailBox = null;
		selectedMail = null;
		textSearch = null;
		textSearchFull = Boolean.FALSE;
		showForCompetence = false;
		taskSendInfo = null;
		taskCommentInfo = null;
		taskHistoryList = new ArrayList<>();
		setShowSignFileOptions(false);
		taskMnenie = null;
		setShowFileUpload(false);
		showTaskSpr = false;
		textReject = "";
		idEgov = null;
		titleDoc ="";
		selectedDocs = new HashMap<>();
		selectedTaskDocs = new HashMap<>();
		selectedTaskDocs.put(-1L,  new HashMap<>());
		showGroupDocTaskNew = false;
		showGroupDocSign = false;
		idDocs = new ArrayList<>();
		idTasks = new ArrayList<>();
	}

	private void clearSortTable() {
		//UIComponent table = FacesContext.getCurrentInstance().getViewRoot().findComponent(ID_TABLE_LIST);
		//table.setValueExpression(SORT_BY, null);

		DataTable datatable= (DataTable)FacesContext.getCurrentInstance().getViewRoot().findComponent(ID_TABLE_LIST);
		datatable.reset();
	}

	private void changeCountSection(int option, int decreaseBy) {

		String count = mapCountsOptions1.get(Long.valueOf(option));
		Integer countI = Integer.valueOf(count);
		countI = countI.intValue() - decreaseBy;

		mapCountsOptions1.put(Long.valueOf(option), countI.toString());
	}

	/**
	 * Метод за ограничаване на броя редове на таблицата
	 * 
	 */
	public void actionDsplayModalSelObj() {

		displayModalSelObj = !displayModalSelObj;

		rows = 5;
		if (displayModalSelObj) {
			rows = 15;
		}

		// System.out.println("displayModalSelObj-----> "+displayModalSelObj);
	}
	
	
	/**
	 * Метод за извличане на броя жалби
	 * 
	 */
	public void loadingCountJalbi() {

		try { // System.out.println(">>>>loadingCountDocSec");

			new DashboardUtils().calculateJalbaSection(ud, mapCountsOptions1);

		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на бройките в секция  документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}

	}
	
	/**
	 * Метод за извличане на брой нпм/самосезиране
	 * 
	 */
	public void loadingCountProverki() {

		try { // System.out.println(">>>>loadingCountDocSec");

			new DashboardUtils().calculateNpmSamosSection(ud, mapCountsOptions1);

		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на бройките в секция  документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}

	}
	
	/**
	 * Метод за извличане на брой документи
	 * 
	 */
	public void loadingCountDocSec() {

		try { // System.out.println(">>>>loadingCountDocSec");

			new DashboardUtils().calculateDocSection(ud, mapCountsOptions1, statusList);

		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на бройките в секция  документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}

	}

	/**
	 * Метод за извличане на брой задачи
	 * 
	 */
	public void loadingCountTaskSec() {

		try { // System.out.println(">>>>loadingCountTaskSec");

			new DashboardUtils().calculateTasksSection(ud.getUserAccess(), mapCountsOptions1, true, true, true,
					extDaysTask, statusList);

		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на бройките в секция задачи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}

	}

	/**
	 * Метод за извличане на брой документи
	 * 
	 */
	public void loadingCountDeloDocSec() {

		try {
			// System.out.println(">>>>loadingCountDeloDocSec ---> ");

			GlobalHolder gh = (GlobalHolder) JSFUtils.getManagedBean("globalDocHolder");

			RegistratureDocHolder rdh = gh.getRegInfo(ud.getRegistratura());

			mailBoxLst = ud.getMailboxNames(); // за даденият потребител

			if (mailBoxLst == null) {
				mailBoxLst = new ArrayList<>();
			}

			Integer countMailBox = 0;
			
			boolean maliAccess = ud.hasAccess(OmbConstants.CODE_CLASSIF_MENU,OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_EMAIL);
			
			//samo ako ima dostyp shte hodim da prowerqwame
			if(maliAccess) {
				for (String box : mailBoxLst) {
	
					countMailBox += rdh.getCouterEmails(box);
				}
			}

			// -------- samo za testowe
//	    	mailBoxLst =  new ArrayList<>(); 
//	    	for(Object[] box:getSystemData(SystemData.class).getMailboxes()) {
//	    		mailBoxLst.add((String)box[1]);
//	    		
//	    	}
			// ------------------------------
			Integer egov = rdh.getCounterSEOS() + rdh.getCounterEDelivery();
			mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_EMAIL), countMailBox.toString());
			mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_CEOC), egov.toString());
			//mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_EL_VR), rdh.getCounterEDelivery().toString());// seos i ssev са заедно
			mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_NAS), rdh.getCounterNasochvane().toString());
			mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_REG_OF), rdh.getCounterOfficial().toString());
			mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_DIF_REG), rdh.getCounterOtherReg().toString());
			mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_COMPETENCE), rdh.getCounterForCompetence().toString());

			Integer count = (countMailBox + rdh.getCounterSEOS() + rdh.getCounterEDelivery()
					+ rdh.getCounterNasochvane() + rdh.getCounterOfficial() + rdh.getCounterOtherReg());
			mapCountsOptions1.put(Long.valueOf(OmbConstants.CODE_ZNACHENIE_DASHBOARD_DELO_DOC), count.toString());

		} catch (Exception e) {
			LOGGER.error("Грешка при зареждане на бройките в секция деловодни документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
		}

	}

	
	/**
	 * Зареждане на задачите на потребителя
	 * @param codeMenuObject
	 */
	public void actionLoadTaskUser(Integer codeMenuObject) {

		try {
			titleRegDoc = "Обработка на задача";
			rows = 5;

			selectedCodeMenuObject = codeMenuObject;

			clearSortTable();
			clearParams();
			showTaskModuleOption = true;

			statusTask = "1";

			// generate columns table model
			columns = new ArrayList<>();
			
			if(groupTaskAccess &&   codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_EXEC   ) {
				
//				|| codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_ASSIGN 
//				|| codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_CONTROL
				
				columns.add(new ColumnModel("", "11", 6, "20", null, ""));
				// ColumnModel(String header, String property, Integer columnTypeValue, String width, String sortColumn, String classWidth) 
			}
			
			columns.add(new ColumnModel("!", "18", 7, "30", "a18", ""));
			
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "tasks.nomer"), "1", "80", "a1", ""));
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.vid"), "2", 1, "100", OmbConstants.CODE_CLASSIF_TASK_VID, "a2", ""));
			
			
			switch (codeMenuObject.intValue()) {

				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_EXEC: // на които съм изпълнител
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_GOING_LATE: // с изтичащ срок
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_LATE: // с изтекъл срок
				
				
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "tasks.srok"), "4", 2, "80", "a4", ""));
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.nomDoc"), "12", "100", "a12", ""));
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.datDoc"), "13", 2, "90", "a13", ""));
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "general.otnosno"), "19", "200", "a19", ""));
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "audit.dopInfo"), "10", "200", "a10", ""));
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "tasks.assignCode"), "8", 1, "200", OmbConstants.CODE_CLASSIF_ADMIN_STR, "a8", ""));
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.vazlojena"), "5", 3, "110", "a5", "")); 
				
					break;
	
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_ASSIGN: // на които съм възложител
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_CONTROL: // на които съм контролиращ
					
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.vazlojena"), "5", 3, "110", "a5", ""));
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "tasks.srok"), "4", 2, "80", "a4", ""));
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "tasks.exec"), "9", 5, "200", OmbConstants.CODE_CLASSIF_ADMIN_STR, "a9", ""));
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "audit.dopInfo"), "10", "200", "a10", ""));
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.nomDoc"), "12", "100", "a12", ""));
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.datDoc"), "13", 2, "90", "a13", ""));
					break;

			}
			
			
			
			objectList = new LazyDataModelSQL2Array(new DashboardUtils().sqlTasksSection(codeMenuObject.intValue(),
					ud.getUserAccess(), extDaysTask, neizpalneniStatusList, null, false), A1_DESC);

			unlockDoc();
		} catch (Exception e) {
			LOGGER.error("Грешка при зареждане на секция задачи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
		}

	}

	/**
	 * Зареждане на задачи към документ
	 * @param codeMenuObject
	 */
	public void actionLoadDocsTask(Integer codeMenuObject) {
		try {
			titleRegDoc = "Обработка";
			rows = 5;

			selectedCodeMenuObject = codeMenuObject;

			clearSortTable();
			clearParams();

			showDocTaskModuleOption = true;

			statusTask = "1"; 

			// generate columns table model
			columns = new ArrayList<>();
			
			/*	if(groupTaskAccess &&  (codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_REZOL 
			|| codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_SAGL 
			|| codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_PODPIS
			|| codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_ZA_ZAPOZNAVANE) )
			 */ 	
		/*	 Скриваме груповото подписване до 2-ро нареждане !!!  
		  if(( codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_SAGL 
				|| codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_PODPIS) ) {
	
				columns.add(new ColumnModel("", "0", 6, "20", null, ""));
				// ColumnModel(String header, String property, Integer columnTypeValue, String width, String sortColumn, String classWidth) 
			}
			*/
			
			
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.nomDoc"), "1", "110", "a1", ""));
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.datDoc"), "2", 2, "100", "a2", ""));
			columns.add( new ColumnModel(getMessageResourceString(LABELS, "general.otnosno"), "5", "*", "a5", DB_COL200));
			// columns.add(new ColumnModel("Идва от/Автори","",4,"200","a6")); //скрита след
			// обсъждане на 14.06
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.vid"), "3", 1, "110", OmbConstants.CODE_CLASSIF_DOC_VID, "a3", ""));

			switch (codeMenuObject.intValue()) {

			case OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_REZOL: // За резолюция
				columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.rezol"), "9", "130", "a9", "")); // реално  е прекрит номер на задача
				columns.add(new ColumnModel("!", "13", 7, "30", "a13", "")); 

				break;
			case OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_SAGL: // За съгласуване
			case OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_PODPIS: // За подпис
				columns.add(new ColumnModel(getMessageResourceString(LABELS, "tasks.srok"), "10", 2, "90", "a10", ""));
				// columns.add(new
				// ColumnModel("Възложител","11",1,"*",OmbConstants.CODE_CLASSIF_ADMIN_STR,"a11"));
				// //скрита след обсъждане на 14.06
				columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.nomTask"), "9", "120", "a9", ""));
				columns.add(new ColumnModel("!", "12", 7, "30", "a12", "")); 
				
				columns.add(new ColumnModel("ﮏ", "13", 8, "40", "a13", ""));

				break;
			case OmbConstants.CODE_ZNACHENIE_DASHBOARD_ZA_ZAPOZNAVANE: // за запознаване
				columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.istruct"), "8", "*", "a8", DB_COL200));
				columns.add(new ColumnModel("!", "11", 7, "30", "c11", "")); 

				break;
			case OmbConstants.CODE_ZNACHENIE_DASHBOARD_NONREG: // Нерегистрирани работни
				columns.add(new ColumnModel("!", "7", 7, "30", "a7", "")); 
				gotoDocPage = true;
				rows = 15;
				break;
			}
			// System.out.println("ud.getUserAccess()---> "+ud.getUserAccess());
			if (codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_ZA_ZAPOZNAVANE) {
				objectList = new LazyDataModelSQL2Array( new DashboardUtils().sqlDocZapoznavane(ud.getUserAccess(), null, false), SORT_BY_A2);
			} else {
				objectList = new LazyDataModelSQL2Array(new DashboardUtils().sqlDocSection(codeMenuObject.intValue(), ud, statusList, null, false), SORT_BY_A2);
			}

			unlockDoc();
		} catch (Exception e) {
			LOGGER.error("Грешка при зареждане на  секция документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
		}
	}

	/**
	 * Зареждане на документи
	 * @param codeMenuObject
	 */
	public void actionLoadDocsDelo(Integer codeMenuObject) {
		try {
			
			rows = 15;

			selectedCodeMenuObject = codeMenuObject;

			clearSortTable();
			clearParams();
			showDeloDocModuleOption = true;
			gotoDocPage = true;
	
			// generate columns table model
			columns = new ArrayList<>();
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.nomDoc"), "1", "110", "a1", ""));
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.datDoc"), "2", 2, "100", "a2", ""));
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "general.otnosno"), "5", "*", "a5", DB_COL200));
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.idvaotAvtor"), "", 4, "200", "a6", ""));
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.vid"), "3", 1, "120", OmbConstants.CODE_CLASSIF_DOC_VID, "a3", ""));

			if (codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_COMPETENCE) {
				titleRegDoc = "Обработка";
				columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashbord.forCompetence"), "8", "*", "a8", DB_COL200));
				columns.add(new ColumnModel("!", "9", 7, "30", "a9", "")); 
			}

			if (codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_DIF_REG) {
				titleRegDoc = "Регистриране на деловоден документ";
				columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashbord.izpNa"), "9", "*", "a9", DB_COL200));
				columns.add(new ColumnModel("!", "11", 7, "30", "a11", "")); 
			}
			
			if (codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_NAS || codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_REG_OF) {				
				titleRegDoc = "Обработка";
				columns.add(new ColumnModel("!", "8", 7, "30", "a8", "")); 
			}

			objectList = new LazyDataModelSQL2Array(new DashboardUtils().sqlDelovodSection(codeMenuObject.intValue(), ud.getRegistratura(), null, false), SORT_BY_A2);

			unlockDoc();
		} catch (Exception e) {
			LOGGER.error("Грешка при зареждане на  секция деловодни документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
		}

	}

	/**
	 * Зареждане на документи СЕОС
	 * @param codeMenuObject
	 */
	public void actionLoadDocsDeloCEOC(Integer codeMenuObject) {
		try {
			titleRegDoc = "Регистриране на деловоден документ";
			rows = 15;
 
			selectedCodeMenuObject = codeMenuObject;

			clearSortTable();
			clearParams();
			showDeloDocModuleOption = true;
			gotoDocPage = true;
			gotoJalbaPage = true;
			// generate columns table model
			columns = new ArrayList<>();
			
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "docu.dateDoc"), "2", 2, "100", "A3MSGREGDAT", ""));   //dashboard.ceRegDat
			
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.ceIztochnik"), "7", "100", "A8DESC", ""));
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.idvaot"), "1", "200", "A2SENDERNAME", ""));
			
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "general.otnosno"), "3", "*", "A4DOCSUBJECT", DB_COL200));
			
//			columns.add(new ColumnModel(getMessageResourceString(LABELS, "dashboard.ceSrokDat"), "4", 2, "100", A5_DOC_SROK, ""));

			columns.add(new ColumnModel(getMessageResourceString(LABELS, "docList.theirNum"), "8", "120", "A9", ""));
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "docu.dateDocD"), "9", 2, "100", "A10", ""));

			String guidSeos = SearchUtils.asString(getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRATURI, ud.getRegistratura(), SysConstants.CODE_DEFAULT_LANG, new Date(), OmbClassifAdapter.REGISTRATURI_INDEX_GUID_SEOS)) ;
			String guidSSEV = SearchUtils.asString(getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRATURI, ud.getRegistratura(), SysConstants.CODE_DEFAULT_LANG, new Date(), OmbClassifAdapter.REGISTRATURI_INDEX_GUID_SSEV)) ;

			// System.out.println("guidSeos-->" +guidSeos);

			objectList = new LazyDataModelSQL2Array(new EgovMessagesDAO(ud).createFilterEgovMessages(guidSeos, guidSSEV, null,false), A5_DOC_SROK);

			unlockDoc();
		} catch (Exception e) {
			LOGGER.error("Грешка при зареждане на  секция деловодни документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
		}

	}

	/**
	 * Зареждане на имейли
	 * @param codeMenuObject
	 * @param mailBox
	 */
	public void actionLoadDocsEmail(Integer codeMenuObject, String mailBox) {
		try {
			titleRegDoc = "Регистриране на деловоден документ";
			rows = 15;
			
			clearParams();
			showDeloDocModuleOption = true;
			gotoDocPage = true;

			selectMailBox = mailBox;
			selectedCodeMenuObject = codeMenuObject;

			clearSortTable();

			Properties prop = getSystemData(SystemData.class).getMailProp(ud.getRegistratura(), selectMailBox);

			mailList = new LazyDataModelMailList(prop, prop.getProperty(MAIL_FOLDER_READ, getSystemData().getSettingsValue(MAIL_FOLDER_READ)));

		} catch (Exception e) {
			LOGGER.error("Грешка при извличане на пощенски кутии !!!!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
		} finally {
			JPA.getUtil().closeConnection(); // getMailProp - go iska
		}

		unlockDoc();

	}
	
	/**
	 * Зареждане на жалби
	 * @param codeMenuObject
	 */
	public void actionLoadJalbi(Integer codeMenuObject) {
		
		try {
			titleRegDoc = "Обработка";
			rows = 5;

			selectedCodeMenuObject = codeMenuObject;

			clearSortTable();
			clearParams();
			showJalbiOption = true;
			gotoDocPage = true;
			// generate columns table model
			columns = new ArrayList<>();
			
			if(codeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_OPR_EXP) {
				columns.add(new ColumnModel("", "0", 6, "20", null, "")); // гр. определяне на водещ експерт
			}
			
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "jalba.vhNomer"), "1", "70", "a1", ""));            //Вх.номер
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "docu.docDatReg"), "2", 2, "80", "a2", ""));     //Дата рег
			columns.add( new ColumnModel(getMessageResourceString(LABELS, "jl.jalbopodatel"), "3", "130", "a3", "")); //Жалбоподател (имената)
			
			columns.add( new ColumnModel(getMessageResourceString(LABELS, "complaint.narushitel"), "4", "150", "a4", "")); //Нарушител 
			
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "jp.prava"), "14", 1, "180", OmbConstants.CODE_CLASSIF_ZAS_PRAVA, "a14", "")); // засегнати права
			
			switch (codeMenuObject.intValue()) {

				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_OBRABOTKA: // 
					//•	Начин на подаване
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "jp.podavane"), "8", 1, "110", OmbConstants.CODE_CLASSIF_SUBMIT_METHOD, "a8", ""));
					//•	Дата на подаване
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "jp.podavaneDate"), "9", 2, "110", "a9", "")); 
				break;
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_OTGOVOR: // 
					//•	Допустимост
					//•	Дата на нарушението
					//•	Коментар при завеждане 
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_PRECENKA: //
					//•	Допустимост
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "jp.dopust"), "5", 1, "110", OmbConstants.CODE_CLASSIF_DOPUST, "a5", ""));
					//•	Дата на нарушението
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "jbp.narushenieDate"), "6", 2, "110", "a6", "")); 
					//•	Коментар при завеждане
					columns.add( new ColumnModel(getMessageResourceString(LABELS, "jp.commentExp"), "7", "*", "a7", DB_COL200)); 
				break;
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_RAZPR: //
					//•	Коментар при завеждане
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_OPR_EXP: // 
					//•	Коментар при завеждане
					columns.add( new ColumnModel(getMessageResourceString(LABELS, "jp.commentExp"), "7", "*", "a7", DB_COL200)); 
					
				break;
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_PROVERKA: // 
					allJlb = "1";
					//•	Състояние
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "complaint.sastoianie"), "10", 1, "110", OmbConstants.CODE_CLASSIF_JALBA_SAST, "a10", ""));
					//•	Срок
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "tasks.srok"), "11",2, "110", "a11", "")); 
					//•	Водещ експерт
					columns.add(new ColumnModel(getMessageResourceString(LABELS, "jp.vodeshExp"), "12", 1, "150", OmbConstants.CODE_CLASSIF_ADMIN_STR, "a12", ""));
					//•	Допълнителна информация
					columns.add( new ColumnModel(getMessageResourceString(LABELS, "journal.dopInfo"), "13", "*", "a13", DB_COL200)); 
				break;	
					

			}
			
			
			
			objectList = new LazyDataModelSQL2Array(new DashboardUtils().sqlJalbaSection(codeMenuObject.intValue(), ud, textSearch, textSearchFull, true), A2_DESC);

			//unlockDoc();
		} catch(InvalidParameterException e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage());
		} catch (Exception e) {
			LOGGER.error("Грешка при зареждане на секция жалби! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
		}
	}
	
	/**
	 * Зареждане на проверки
	 * @param codeMenuObject
	 */
	public void actionLoadProverki(Integer codeMenuObject) {
		
		try {
			titleRegDoc = "Обработка";
			rows = 5;

			selectedCodeMenuObject = codeMenuObject;

			clearSortTable();
			clearParams();
			showProverkiOption = true;
			gotoDocPage = true;
			// generate columns table model
			columns = new ArrayList<>();
			
			//Вх.номер
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "docDestruction.regNum"), "1", "100", "a1", ""));      
			//Дата рег
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "docu.docDatReg"), "2", 2, "110", "a2", "")); 
			// Ръководител на екипа
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "docspec.expLider"), "3", 1, "150", OmbConstants.CODE_CLASSIF_ADMIN_STR, "a3", ""));
			// Дата на започване
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "docspec.startDate"), "4", 2, "150", "a4", "")); 
			// Срок за приключване
			columns.add(new ColumnModel(getMessageResourceString(LABELS, "docspec.endDate"), "5", 2, "170", "a5", "")); 
			//Предмет на проверката 
			columns.add( new ColumnModel(getMessageResourceString(LABELS, "docspec.predmet"), "6", "*", "a6", DB_COL200)); 
			
			
//			switch (codeMenuObject.intValue()) {
//
//				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_AKT_NPM:  //
//				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_AKT_SAMOS: // 
//			}
			
			
			
			objectList = new LazyDataModelSQL2Array(new DashboardUtils().sqlNpmSamosSection(codeMenuObject.intValue(), ud, textSearch, textSearchFull), A2_DESC);

			//unlockDoc();
		} catch(InvalidParameterException e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage());
		}  catch (Exception e) {
			LOGGER.error("Грешка при зареждане на секция жалби! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
		}
	}
	
	/**
	 * Метод за търсене 
	 * 
	 */
	public void actionSearch() {

		clearSortTable();
		selectedObject = null;

		if (textSearch == null || textSearch.trim().isEmpty()) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(beanMessages, "dashboard.textempty"));
		} else {
			try {
				switch (selectedCodeMenuObject.intValue()) {
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_REZOL: // За резолюция
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_SAGL: // За съгласуване
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_PODPIS: // За подпис
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_NONREG:
					objectList = new LazyDataModelSQL2Array(
							new DashboardUtils().sqlDocSection(selectedCodeMenuObject.intValue(), ud,
									(statusTask.equals("1") ? statusList : statusTaskNotActive), textSearch, textSearchFull), SORT_BY_A2);
					break;

				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_ZA_ZAPOZNAVANE: // за запознаване
					objectList = new LazyDataModelSQL2Array( new DashboardUtils().sqlDocZapoznavane(ud.getUserAccess(), textSearch, textSearchFull), SORT_BY_A2);
					break;

				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_EXEC: // на които съм изпълнител
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_GOING_LATE: // с изтичащ срок
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_LATE: // с изтекъл срок
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_ASSIGN: // на които съм възложител
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_CONTROL: // на които съм контролиращ
					objectList = new LazyDataModelSQL2Array(
							new DashboardUtils().sqlTasksSection(selectedCodeMenuObject.intValue(), ud.getUserAccess(),
									7, (statusTask.equals("1") ? neizpalneniStatusList : otherStatusList), textSearch, textSearchFull), A1_DESC);
					break;

				// case OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_EMAIL:
				// case OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_EL_VR:
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_NAS:
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_REG_OF:
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_DIF_REG:
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_COMPETENCE:
					objectList = new LazyDataModelSQL2Array( new DashboardUtils().sqlDelovodSection(selectedCodeMenuObject.intValue(), ud.getRegistratura(), textSearch, textSearchFull), SORT_BY_A2);
					break;

				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_CEOC:
					String guidSeos = SearchUtils.asString(getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRATURI, ud.getRegistratura(), SysConstants.CODE_DEFAULT_LANG, new Date(), OmbClassifAdapter.REGISTRATURI_INDEX_GUID_SEOS)) ;
					String guidSSEV = SearchUtils.asString(getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRATURI, ud.getRegistratura(), SysConstants.CODE_DEFAULT_LANG, new Date(), OmbClassifAdapter.REGISTRATURI_INDEX_GUID_SSEV)) ;
					
					objectList = new LazyDataModelSQL2Array(new EgovMessagesDAO(ud).createFilterEgovMessages(guidSeos, guidSSEV, textSearch ,textSearchFull), A5_DOC_SROK);

					break;
					
					
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_OBRABOTKA: // 
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_OTGOVOR: // 
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_PRECENKA: //
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_RAZPR: //
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_OPR_EXP: // 
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_PROVERKA: // 
					try {
						allJlb = "1";
						objectList = new LazyDataModelSQL2Array(new DashboardUtils().sqlJalbaSection(selectedCodeMenuObject.intValue(), ud, textSearch, textSearchFull, true), A2_DESC);
					} catch(InvalidParameterException e) {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage());
					} 
					break;
				
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_AKT_NPM: // 
				case OmbConstants.CODE_ZNACHENIE_DASHBOARD_AKT_SAMOS: // 
					try {
						objectList = new LazyDataModelSQL2Array(new DashboardUtils().sqlNpmSamosSection(selectedCodeMenuObject.intValue(), ud, textSearch, textSearchFull), A2_DESC);
					} catch(InvalidParameterException e) {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "", e.getMessage());
					} 
					break;
				}

			} catch (Exception e) {
				LOGGER.error("Грешка при търсене -> actionSearch! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
			}
		}

	}

	/**
	 * Смяна на статуси на задачи към документи
	 * 
	 */
	public void actionChangeStatusesTaskDoc() {

		clearSortTable();
		selectedObject = null;

		try {
			// За резолюция //За съгласуване //За подпис
			objectList = new LazyDataModelSQL2Array(
					new DashboardUtils().sqlDocSection(selectedCodeMenuObject.intValue(), ud,
							(statusTask.equals("1") ? statusList : statusTaskNotActive), null, false), SORT_BY_A2);
		} catch (Exception e) {
			LOGGER.error("Грешка при търсене -> actionChangeStatusesTask! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
		}

	}

	
	/**
	 * Смяна на статусите на задачи
	 * 
	 */
	public void actionChangeStatusesTaskTask() {

		clearSortTable();
		selectedObject = null;

		try {
			objectList = new LazyDataModelSQL2Array(
					new DashboardUtils().sqlTasksSection(selectedCodeMenuObject.intValue(), ud.getUserAccess(), 7,
							(statusTask.equals("1") ? neizpalneniStatusList : otherStatusList), null, false),
					A1_DESC);
		} catch (Exception e) {
			LOGGER.error("Грешка при търсене -> actionChangeStatusesTask! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
		}
	}

	/**
	 * Жалби - Презарежда списъка "в процес на проверка" - "всички" или "в които съм водещ" 
	 */
	public void actionChangeJalbiList() {

		clearSortTable();
		selectedObject = null;

		try {
			objectList = new LazyDataModelSQL2Array(new DashboardUtils().sqlJalbaSection(selectedCodeMenuObject.intValue(), ud, textSearch, textSearchFull, (allJlb.equals("1") ? true : false)), A2_DESC);
		} catch (Exception e) {
			LOGGER.error("Грешка при търсене -> actionChangeJalbiList! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
		}

	}
	
	/**
	 * Метод за избор на обект
	 * 
	 */
	public void actionSelectObject() { // SelectEvent<Object[]> event

		boolean loadDocData = false;
		Integer typeDoc = null;
		showSignFileOptions = false;
		showTaskSpr = false;

		switch (selectedCodeMenuObject.intValue()) {

		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_EXEC: // на които съм изпълнител
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_GOING_LATE: // с изтичащ срок
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_LATE: // с изтекъл срок
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_ASSIGN: // на които съм възложител
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_CONTROL: // на които съм контролиращ
			idDoc = SearchUtils.asInteger(selectedObject[11]);
			idTask = SearchUtils.asInteger(selectedObject[0]);
			dateDoc = SearchUtils.asDate(selectedObject[13]);
			typeDoc = SearchUtils.asInteger(selectedObject[15]);
			if (idDoc != null) {
				titleDoc = getMessageResourceString(LABELS, "dashboard.document") + " " + selectedObject[12] + "/ "
						+ convertDateToString(dateDoc);
				loadDocData = true;
				showTaskSpr = true;
			}

			break;

		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_REZOL: // За резолюция
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_SAGL: // За съгласуване
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_PODPIS: // За подпис
			// case OmbConstants.CODE_ZNACHENIE_DASHBOARD_NONREG: //Нерегистрирани работни

			idDoc = SearchUtils.asInteger(selectedObject[0]);
			idTask = SearchUtils.asInteger(selectedObject[8]);
			dateDoc = SearchUtils.asDate(selectedObject[2]);
			typeDoc = SearchUtils.asInteger(selectedObject[4]);

			titleDoc = getMessageResourceString(LABELS, "dashboard.document") + " " + selectedObject[1] + "/ "
					+ convertDateToString(dateDoc);

			loadDocData = true;

			setShowNewTask(true);

			setShowFileUpload(false);
			if (selectedCodeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_SAGL
					&& typeDoc.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) {
				setShowFileUpload(true);
			} 

			// ще сареждаме задачата без да се показва на екрана
			tmpTask = null;
			if (idTask != null) { // && selectedCodeMenuObject.intValue() ==
									// DashboardUtils.CODE_ZNACHENIE_DASHBOARD_FOR_REZOL
				try {
					showTaskSpr = true;

					JPA.getUtil().runWithClose(() -> tmpTask = new TaskDAO(getUserData()).findById(idTask));
					// tmpTask.getTaskInfo();

					taskSendInfo = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR,
							tmpTask.getCodeAssign(), getUserData().getCurrentLang(), new Date());
					taskCommentInfo = tmpTask.getStatusComments();
					taskMnenie = null;

					if (selectedCodeMenuObject.intValue() != OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_REZOL) {
						opinionLst = getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_TASK_TYPE_TASK_OPINION,
								tmpTask.getTaskType(), getCurrentLang(), new Date());
						// Полетата за мнение и коментар да не се зареждат предварително, защото тези,
						// които идват от базата, не са на човека, който ще подписва / съгласува
						// Над реда с бутона <Съгласуван> да се изведе текст, формиран по следния начин:
						// Документът е насочен от <имената на TASK.STATUS_USER_ID>. И ако има стойност
						// в TASK.STATUS_COMMENTS се добавя: Коментар:<TASK.STATUS_COMMENTS>

						if (tmpTask.getStatusComments() != null && !tmpTask.getStatusComments().isEmpty()) {
							tmpTask.setStatusComments("");
						}

						if (tmpTask.getEndOpinion() != null) {

							for (SystemClassif sc : opinionLst) {
								if (sc.getCode() == tmpTask.getEndOpinion().intValue()) {
									taskMnenie = sc.getTekst();
								}
							}
							tmpTask.setEndOpinion(null);
						}

						// подписване на файлове

						// if(ud.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA,
						// OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_DIGITAL_SIGN) &&
						// SearchUtils.asInteger(selectedObject[4]).intValue() !=
						// OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN) {
						// rosi - Dani каза, че имало редки случаи, в които можели да съгласуват и
						// подписват и входящи документи - междуведомсвени документи
						if (ud.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA,
								OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_DIGITAL_SIGN)) {
							showSignFileOptions = true;
						}
						
					} else {
						// dali e whodqsht dokument ako e za rezol
						if (SearchUtils.asInteger(selectedObject[4])
								.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN) {
							Integer commpe = SearchUtils.asInteger(selectedObject[12]);
							if (commpe != null && commpe.intValue() == OmbConstants.CODE_ZNACHENIE_COMPETENCE_OUR) {
								setShowForCompetence(true);
							}
						}
					}

					if (tmpTask != null) {
						tmpTask.setRealEnd(new Date()); // винаги днешна дата по подразбиране
					}

				} catch (BaseException e) {
					LOGGER.error("Грешка при зареждане на данни за задача! ", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
							getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
				}

			}
			break;
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_ZA_ZAPOZNAVANE: // за запознаване

			idDoc = SearchUtils.asInteger(selectedObject[0]);
			dateDoc = SearchUtils.asDate(selectedObject[2]);
			typeDoc = SearchUtils.asInteger(selectedObject[4]);
			titleDoc = getMessageResourceString(LABELS, "dashboard.document") + " " + selectedObject[1] + "/ "
					+ convertDateToString(dateDoc);
			loadDocData = true;

			setShowNewTask(true);
			Integer regDoc = SearchUtils.asInteger(selectedObject[10]);
			if (!ud.getRegistratura().equals(regDoc)) {
				setShowNewTask(false);
			}
			
			
			showTaskSpr = true;
			
			break;

		}

		if (loadDocData) {

			try {
				if (!new DocDAO(getUserData()).hasDocAccessDashboard(idDoc)) {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,
							getMessageResourceString(beanMessages, "dashboard.noDocAccess"));
					selectedObject = null;
					return;
				}

				setDocInfo(new DashboardUtils().decodeDocOtnostno(idDoc));

				docCorespAvtor = valuecolumnConverter(selectedObject);

				if (typeDoc != null) {
					titleDoc += " (" + getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DOC_TYPE,
							SearchUtils.asInteger(typeDoc), getCurrentLang(), new Date()) + ") ";
				}

			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане данните на документа! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ""),
						e.getMessage());
			} finally {
				JPA.getUtil().closeConnection();
			}

			loadDocDataFiles();
		}

	}

	/**
	 * Метод за избор на имейл
	 * 
	 */
	public void actionSelectMail() {

		try {
			long messUID = Long.valueOf(JSFUtils.getRequestParameter("messUID"));

			Properties prop = getSystemData(SystemData.class).getMailProp(ud.getRegistratura(), selectMailBox);
			selectedMail = new Mailer().getMessage(prop, messUID,
					prop.getProperty(MAIL_FOLDER_READ, getSystemData().getSettingsValue(MAIL_FOLDER_READ)));

		} catch (DbErrorException e) {
			LOGGER.error("Грешка при изчитане на проп настройките от систем дата имейли! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} catch (MessagingException e) {
			LOGGER.error("Грешка при изчитане на имейл! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} catch (NumberFormatException e) {
			LOGGER.error("Грешка при формат на числа! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, GENERAL_FORMATEXC), e.getMessage());
		} catch (Exception e) {
			LOGGER.error("Грешка при зареждане на избрано електронно съобщение! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
		} finally {
			JPA.getUtil().closeConnection(); // getMailProp - go iska
		}
	}

	/**
	 * Отказ от регистрация на документ по мейл
	 * 
	 */
	public void actionRejectMail() {

		if (SearchUtils.isEmpty(textReject)) {
			JSFUtils.addMessage("dashboard:txtMailReject", FacesMessage.SEVERITY_ERROR, getMessageResourceString(
					UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.txtRejected")));
		} else if (selectedMail != null) {

			// Проверка за закючване
			if (checkForLock((int) selectedMail.getMessUID(), OmbConstants.CODE_ZNACHENIE_JOURNAL_MAILBOX)) {

				try {
					// Проверка за вече регистриран документ
					Object[] rez = new DocDAO(getUserData())
							.findDocDataFromMail(selectedMail.getMessUID() + " " + selectMailBox);
					if (rez == null) {
						Properties prop = getSystemData(SystemData.class).getMailProp(ud.getRegistratura(),
								selectMailBox);

						String subject = "FW:" + (selectedMail.getSubject() != null ? selectedMail.getSubject() : "");

						StringBuilder body = new StringBuilder();

						body.append(getMessageResourceString(LABELS, "doc.Otkaz"));
						body.append(" \n  " + getMessageResourceString(LABELS, "dashboard.dateOtkaz") + ": "
								+ DateUtils.printDate(new Date()));
						body.append(" \n  " + getMessageResourceString(LABELS, "dashboard.prichinaOtkaz") + ": "
								+ textReject);
						body.append(" \n  " + getMessageResourceString(LABELS, "regData.registratura") + ": "
								+ getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REGISTRATURI,
										ud.getRegistratura(), ud.getCurrentLang(), new Date()));

						Mailer mm = new Mailer();
						mm.forward(prop, selectedMail.getMessUID(),
								prop.getProperty(MAIL_FOLDER_READ, getSystemData().getSettingsValue(MAIL_FOLDER_READ)),
								subject, body.toString());
						// mm.replay(prop, selectedMail.getMessUID(), prop.getProperty(mailFolderRead,
						// getSystemData().getSettingsValue(mailFolderRead)), subject, body.toString());

						mm.moveMailUIDRejected(prop, selectedMail.getMessUID(), getSystemData());

						selectedMail = null;

						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
								getMessageResourceString(UI_beanMessages, "general.succesSaveMsg"));

					} else {
						// има регистриран документ вече
						String msg = ": " + rez[1] + " / " + convertDateToString((Date) rez[2]);
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,
								getMessageResourceString(LABELS, "registred.mail") + msg);
					}
				} catch (DbErrorException e) {
					LOGGER.error("Грешка при отказ от регистрация на документ по е-маил! ", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
							getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
				} catch (MessagingException e) {
					LOGGER.error("Грешка при преместване на е-мейл в папка -отказани! ", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
							getMessageResourceString(UI_beanMessages, GENERAL_FORMATEXC), e.getMessage());
				} catch (Exception e) {
					LOGGER.error("Грешка при отказ от регистрация на документ по е-маил ", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
							getMessageResourceString(UI_beanMessages, GENERAL_EXCEPTION), e.getMessage());
				} finally {
					JPA.getUtil().closeConnection(); // getMailProp - go iska
				}

			}

		} else {
			// TODO mess
		}
	}

	/**
	 * Изтриване на мейл
	 * 
	 */
	public void actionRemoveMail() {

		if (selectedMail != null) {

			// Проверка за закючване
			if (checkForLock((int) selectedMail.getMessUID(), OmbConstants.CODE_ZNACHENIE_JOURNAL_MAILBOX)) {

				try {
					// Проверка за вече регистриран документ
					Object[] rez = new DocDAO(getUserData())
							.findDocDataFromMail(selectedMail.getMessUID() + " " + selectMailBox);
					if (rez == null) {
						Properties prop = getSystemData(SystemData.class).getMailProp(ud.getRegistratura(),
								selectMailBox);
						new Mailer().delete(prop, selectedMail.getMessUID(),
								prop.getProperty(MAIL_FOLDER_READ, getSystemData().getSettingsValue(MAIL_FOLDER_READ)));

						selectedMail = null;

						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
								getMessageResourceString(UI_beanMessages, "general.succesSaveMsg"));
					} else {
						// има регистриран документ вече
						String msg = ": " + rez[1] + " / " + convertDateToString((Date) rez[2]);
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,
								getMessageResourceString(LABELS, "registred.mail") + msg);
					}
				} catch (DbErrorException e) {
					LOGGER.error("Грешка при изтриване на имейл! ", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
							IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
				} catch (MessagingException e) {
					LOGGER.error("Грешка при изтриване на е-мейл! ", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
							getMessageResourceString(UI_beanMessages, GENERAL_FORMATEXC), e.getMessage());
				} finally {
					JPA.getUtil().closeConnection(); // getMailProp - go iska
				}

			}

		} else {
			// TODO mess
		}
	}

	/**
	 * Метод за зареждане на файлове
	 * 
	 */
	private void loadDocDataFiles() {

		try {

			// load files
			FilesDAO daoF = new FilesDAO(getUserData());
			JPA.getUtil().runWithClose(
					() -> filesListDoc = daoF.selectByFileObjectDop(idDoc, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC));

		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане данните на документа! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на документа! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}

	}

	/**
	 * Презареждане на файлове
	 * 
	 */
	public void reloadDocDataFile() {

		if (idDoc != null) {
			loadDocDataFiles();
		}

	}

	/**
	 * Приключване на резолюцията
	 * 
	 */
	public void actionSaveRezol() {

		if (tmpTask != null) {

			try {

				if (tmpTask.getStatusComments() == null || tmpTask.getStatusComments().isEmpty()) {
					if (showForCompetence && forCompetence) {
						JSFUtils.addMessage("dashboard:comentar", FacesMessage.SEVERITY_ERROR,
								IndexUIbean.getMessageResourceString(beanMessages, "dashboard.competence"));
						return;
					} else {
						tmpTask.setStatusComments(IndexUIbean.getMessageResourceString(LABELS, "dashboard.rezolu"));
					}
				}

				tmpTask.setStatus(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP);
				tmpTask.setRealEnd(new Date());

				JPA.getUtil().runInTransaction(
						() -> new TaskDAO(getUserData()).save(tmpTask, null, (SystemData) getSystemData()));

				if (forCompetence) {
					
					JPA.getUtil().runWithClose(() -> doc = new DocDAO(getUserData()).findById(tmpTask.getDocId()));
					doc.setCompetence(OmbConstants.CODE_ZNACHENIE_COMPETENCE_FOR_SEND);
					doc.setCompetenceText(tmpTask.getStatusComments());

					JPA.getUtil().runInTransaction(() -> new DocDAO(getUserData()).saveSysOkaJournal(doc
									, "Документ "+doc.getIdentInfo()+" е маркиран за изпращане по компететност с резолюция "+tmpTask.getRnTask()+"."));
				}
				// коментирах го, за да остане на екрана след съгласуване и да се позоволи
				// веднага да се въведе нова задача към документа, например да се насочи за
				// подпсиване
//				clearSortTable();
//		        
//		        selectedObject = null;

				changeCountSection(OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_REZOL, 1);

				taskCommentInfo = tmpTask.getStatusComments();

				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
						IndexUIbean.getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

			} catch (BaseException e) {
				LOGGER.error("actionSaveRezol - > Грешка при запис на задача! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}
		} else {
			LOGGER.error("actionSaveRezol - > Грешка  задачата е нулл! ");
		}
	}

	/**
	 * Запис на съгласуване
	 * 
	 */
	public void actionSaveSgl() {
		if (tmpTask != null) {

			boolean flag = true;

			if (tmpTask.getEndOpinion() == null) {
				JSFUtils.addMessage("dashboard:opinion", FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(beanMessages, "task.msgOpinion"));
				flag = false;
			} else {

				try {
					if (getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_TASK_OPINION_WITH_COMMENT, tmpTask.getEndOpinion(), new Date())
						&& 	(tmpTask.getStatusComments() == null || tmpTask.getStatusComments().isEmpty()) ) {
						
						JSFUtils.addMessage("dashboard:comentar", FacesMessage.SEVERITY_ERROR,
								IndexUIbean.getMessageResourceString(beanMessages, "task.msgStComment"));
						flag = false;
						
					}
				} catch (DbErrorException e) {
					LOGGER.error("Грешка - мнения, коментар! ", e);
				}
			}

			Date enddate = DateUtils.startDate(tmpTask.getRealEnd());
			Date adate = DateUtils.startDate(tmpTask.getAssignDate());
			if (enddate == null || enddate.before(adate)) {// дали не е преди дата на възлагaнe!
				JSFUtils.addMessage("dashboard:exeDat", FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(beanMessages, "task.msgDateRealEnd"));
				flag = false;
			}

			if (flag) {
				try {
					tmpTask.setStatus(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP);

					JPA.getUtil().runInTransaction(
							() -> new TaskDAO(getUserData()).save(tmpTask, null, (SystemData) getSystemData()));

					// коментирах го, за да остане на екрана след съгласуване и да се позоволи
					// веднага да се въведе нова задача към документа, например да се насочи за
					// подпсиване
					// clearSortTable(); // rosi

					// selectedObject = null; // rosi

					changeCountSection(OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_SAGL, 1);

					taskCommentInfo = tmpTask.getStatusComments();

					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
							IndexUIbean.getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
				} catch (BaseException e) {
					LOGGER.error("actionSaveSgl - > Грешка при запис на задача! ", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
							IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
				}

			}
		} else {
			LOGGER.error("actionSaveSgl - >Грешка  задачата е нулл! ");
		}
	}

	/**
	 * Запис на задача
	 * 
	 */
	public void actionSavePodps() {
		if (tmpTask != null) {

			boolean flag = true;

			if (tmpTask.getEndOpinion() == null) {
				JSFUtils.addMessage("dashboard:opinion", FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(beanMessages, "task.msgOpinion"));
				flag = false;
			} else {

				try {
					if (getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_TASK_OPINION_WITH_COMMENT, tmpTask.getEndOpinion(), new Date())
							&& (tmpTask.getStatusComments() == null || tmpTask.getStatusComments().isEmpty()) ) {
						
						JSFUtils.addMessage("dashboard:comentar", FacesMessage.SEVERITY_ERROR,
								IndexUIbean.getMessageResourceString(beanMessages, "task.msgStComment"));
						flag = false;
						
					}
				} catch (DbErrorException e) {
					LOGGER.error("Грешка - мнения, коментар! ", e);
				}
			}

			Date enddate = DateUtils.startDate(tmpTask.getRealEnd());
			Date adate = DateUtils.startDate(tmpTask.getAssignDate());
			if (enddate == null || enddate.before(adate)) {// дали не е преди дата на възлагaнe!
				JSFUtils.addMessage("dashboard:exeDat", FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(beanMessages, "task.msgDateRealEnd"));
				flag = false;
			}

			if (flag) {
				try {
					tmpTask.setStatus(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP);

					JPA.getUtil().runInTransaction(
							() -> new TaskDAO(getUserData()).save(tmpTask, null, (SystemData) getSystemData()));

					// коментирах го, за да остане на екрана след подпис и да се позоволи веднага да
					// се въведе нова задача към документа, например да се насочи към друго лице за
					// подпис или за сведение...
					// clearSortTable(); //rosi

					// selectedObject = null; //rosi

					changeCountSection(OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_PODPIS, 1);

					taskCommentInfo = tmpTask.getStatusComments();

					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
							IndexUIbean.getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

				} catch (BaseException e) {
					LOGGER.error("actionSavePodps -> Грешка при запис на задача! ", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
							IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
				}

			}
		} else {
			LOGGER.error("actionSavePodps - >Грешка  задачата е нулл! ");
		}
	}

	/**
	 * Запознаване
	 * 
	 */
	public void actionZapoznavane() {

		try {
			JPA.getUtil().begin();
			new UserNotificationsDAO().changeStatusMessage(SearchUtils.asInteger(selectedObject[9]),
					OmbConstants.CODE_ZNACHENIE_DA);

			// трябва да се журналира и това действие
			Integer docId = SearchUtils.asInteger(selectedObject[0]);
			String ident = "Запознат с документ: " + selectedObject[1] + "/" + DateUtils.printDate((Date) selectedObject[2]) + ".";
			SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, docId, ident);

			journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
			journal.setDateAction(new Date());
			journal.setIdUser(getCurrentUserId());

			new DocDAO(this.ud).saveAudit(journal);
			
			JPA.getUtil().commit();
			// setBrNotif(getBrNotif() - 1);

//			if ("1".equals(getSystemData().getSettingsValue("delo.mobileAvailable"))) {
//				FirebaseClient.sendNotifCount(((UserData)getUserData()).getUserAccess(), -1, null);
//			}

			clearSortTable();

			selectedObject = null;

			changeCountSection(OmbConstants.CODE_ZNACHENIE_DASHBOARD_ZA_ZAPOZNAVANE, 1);

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
					IndexUIbean.getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
		} catch (Exception e) {
			JPA.getUtil().rollback();
			LOGGER.error("actionZapoznavane -> Грешка при смяна на статусна нотификация! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/**
	 * Зареждане на историята на задача
	 * 
	 */
	public void actionHistoryTask() {
		try {
			taskHistoryList = new TaskDAO(getUserData()).findTaskStatesList(idTask);
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на списък История на промените за задача! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			taskHistoryList = new ArrayList<>();
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/**
	 *Задачи към документ
	 * 
	 */
	public void actionLoadTaskDocSpr() {

		try {
			TaskSearch tmpTs = new TaskSearch (null); // new TaskSearch(ud.getRegistratura());
			tmpTs.setDocId(this.idDoc);
			tmpTs.buildQueryTasksInDoc();
			tasksDocList = new LazyDataModelSQL2Array(tmpTs, "a0 asc");
		} catch (Exception e) {
			LOGGER.error("Грешка при зареждане на задачи към документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			tasksDocList = null;
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/**
	 * справка за свързани документи
	 */
	public void actionLoadLinkDocSpr() {

		try {
			linkDocList= new DocDAO(getUserData()).selectDocAndFiles(this.idDoc, getSystemData(), (UserData) getUserData());			
		} catch (Exception e) {
			LOGGER.error("Грешка при зареждане на свързани документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			setLinkDocList(null);
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
	

	 /**
	  * download на файлове от справка за свързани документи
	  * @param files
	  */
	public void download(Integer idFile) { 
		try {
			Files file = null;
			if (idFile != null){
		
				FilesDAO dao = new FilesDAO(getUserData());	
				try {
					file = dao.findById(idFile);	
				} finally {
					JPA.getUtil().closeConnection();
				}
				
				if(file.getContent() == null){					
					file.setContent(new byte[0]);
				}
			} else {
				return; // няма как да има файл
			}

			FacesContext facesContext = FacesContext.getCurrentInstance();
			ExternalContext externalContext = facesContext.getExternalContext();

			HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
			String agent = request.getHeader("user-agent");

			String codedfilename = "";
			if (null != agent && (-1 != agent.indexOf("MSIE") || -1 != agent.indexOf("Mozilla") && -1 != agent.indexOf("rv:11") || -1 != agent.indexOf("Edge"))) {
				codedfilename = URLEncoder.encode(file.getFilename(), "UTF8");
			} else if (null != agent && -1 != agent.indexOf("Mozilla")) {
				codedfilename = MimeUtility.encodeText(file.getFilename(), "UTF8", "B");
			} else {
				codedfilename = URLEncoder.encode(file.getFilename(), "UTF8");
			}

			externalContext.setResponseHeader("Content-Type", "application/x-download");
			externalContext.setResponseHeader("Content-Length", file.getContent().length + "");
			externalContext.setResponseHeader("Content-Disposition", "attachment;filename=\"" + codedfilename + "\"");
			externalContext.getResponseOutputStream().write(file.getContent());

			facesContext.responseComplete();

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	
	public void actionNewTaskDoc() {
		showDocTaskNew = true;
	}

	public void actionCancelTaskDoc() {
		showDocTaskNew = false;
	}

	public LazyDataModelSQL2Array getObjectList() {
		return objectList;
	}

	public void setObjectList(LazyDataModelSQL2Array objectList) {
		this.objectList = objectList;
	}

	public Integer getSelectedCodeMenuObject() {
		return selectedCodeMenuObject;
	}

	public void setSelectedCodeMenuObject(Integer selectedCodeMenuObject) {
		this.selectedCodeMenuObject = selectedCodeMenuObject;
	}

	public List<ColumnModel> getColumns() {
		return columns;
	}

	public void setColumns(List<ColumnModel> columns) {
		this.columns = columns;
	}

	public String valuecolumnConverter(Object[] row) {

		StringBuilder return_ = new StringBuilder("");

		switch (selectedCodeMenuObject.intValue()) {

		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_REZOL:
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_SAGL:
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_PODPIS:
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_NONREG:
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_ZA_ZAPOZNAVANE:
			try {
				Integer docType = SearchUtils.asInteger(row[4]);
				if (docType.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN) {
					String item = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS,
							SearchUtils.asInteger(row[6]), getCurrentLang(), new Date());
					return_.append((item != null ? item : ""));
				} else if (docType.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN
						|| docType.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) {
					String item = ((SystemData) getSystemData()).decodeItems(OmbConstants.CODE_CLASSIF_ADMIN_STR,
							SearchUtils.asString(row[7]), getCurrentLang(), new Date());
					return_.append((item != null ? item : ""));
				}
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при разкодиране на значение! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}

			break;

		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_NAS:
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_REG_OF:
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_DIF_REG:
			try {
				Integer docType = SearchUtils.asInteger(row[4]);
				if (docType.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN) {
					String item = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS,
							SearchUtils.asInteger(row[6]), getCurrentLang(), new Date());
					return_.append((item != null ? item : ""));
				} else if (docType.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN
						|| docType.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) {
					String item = ((SystemData) getSystemData()).decodeItems(OmbConstants.CODE_CLASSIF_ADMIN_STR,
							SearchUtils.asString(row[7]), getCurrentLang(), new Date());
					return_.append((item != null ? item : ""));
				}

				// ---
				if (OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_DIF_REG == selectedCodeMenuObject.intValue()
						&& row[10] != null) {
					// <h:outputText value="#{labels['docList.sendFromReg']}: "
					// rendered="#{row[13]!=null}"/>
					// <h:outputText
					// value="#{systemData.decodeItem(OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS,
					// row[13], docList.currentLang, now)}" />
					String item = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS,
							SearchUtils.asInteger(row[10]), getCurrentLang(), new Date());
					return_.append((item != null ? getMessageResourceString(LABELS, "docList.sendFromReg") + ": " + item
							: ""));
				}
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при разкодиране на значение! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}
			break;
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_EXEC: // на които съм изпълнител
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_GOING_LATE: // с изтичащ срок
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_LATE: // с изтекъл срок
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_ASSIGN: // на които съм възложител
		case OmbConstants.CODE_ZNACHENIE_DASHBOARD_CONTROL: // на които съм контролиращ
			try {
				Integer docType = SearchUtils.asInteger(row[15]);
				if (docType.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN) {
					String item = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS,
							SearchUtils.asInteger(row[16]), getCurrentLang(), new Date());
					return_.append((item != null ? item : ""));
				} else if (docType.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN
						|| docType.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) {
					String item = ((SystemData) getSystemData()).decodeItems(OmbConstants.CODE_CLASSIF_ADMIN_STR,
							SearchUtils.asString(row[17]), getCurrentLang(), new Date());
					return_.append((item != null ? item : ""));
				}
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при разкодиране на значение! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}

			break;
		}

		return return_.toString();
	}

	/*
	 * regJalba = true - ще се регистрира жалба; false - друг деловоден документ (изпозва се за ССЕВ/СЕОС)
	 */
	public String actionGoto(Object[] row, boolean regJalba) {

		String result = "";
		if (selectedCodeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_REG_OF) {
			result = "docEdit.jsf?faces-redirect=true&idObj=" + asInteger(row[0]) + "&fw=1";
		} else if (selectedCodeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_DIF_REG) {
			result = "docEdit.jsf?faces-redirect=true&idObj=" + asInteger(row[0]) + "&fw=2&idDvig=" + asInteger(row[8]);
		} else if (selectedCodeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_DOC_CEOC) {
			idEgov = asInteger(row[0]);
			EgovMessagesDAO daoEgov = new EgovMessagesDAO(ud);
			Object[] dataGuid = null;
			try {
				dataGuid = daoEgov.isDblGuid(SearchUtils.asString(row[6]), ud.getRegistratura());
			} catch (Exception e) {
				LOGGER.error("Грешка при проверка за вече регистриран документ със същият гуид! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			} finally {
				JPA.getUtil().closeConnection();
			}
			if (dataGuid != null) {
				textReject = getMessageResourceString(LABELS, "dashboard.dblGuid");
				titleDoc = getMessageResourceString(LABELS, "dashboard.dblGuidDoc") + ": " + dataGuid[1] + "/ " + convertDateToString(SearchUtils.asDate(dataGuid[2]));
				
				PrimeFaces.current().executeScript("PF('rejectModalEGOV').show()");
				result = null;
			} else {
				if(regJalba) {
					result = "docEdit.jsf?faces-redirect=true&idEgovJ=" + idEgov; // рег. на жалба
				}else {
					result = "docEdit.jsf?faces-redirect=true&idEgov=" + idEgov;	
				}
			}
		} else {
			result = "docEdit.jsf?faces-redirect=true&idObj=" + asInteger(row[0]);
		}

		// System.out.println("----> "+result);
		return result;
	}

	public String actionGotoViewDoc() {

		return "docView.xhtml?faces-redirect=true&idObj=" + idDoc;
	}

	/**
	 * 
	 * @param regJalba = true - ще се регистрира жалба; false - друг деловоден документ
	 * @return
	 */
	public String actionGotoRegMali(boolean regJalba) {

		// Проверка за закючване
		if (checkForLock((int) selectedMail.getMessUID(), OmbConstants.CODE_ZNACHENIE_JOURNAL_MAILBOX)) {
			try {
				// Проверка за вече регистриран документ
				Object[] rez = new DocDAO(getUserData())
						.findDocDataFromMail(selectedMail.getMessUID() + "" + selectMailBox);
				if (rez == null) {
					Properties prop = getSystemData(SystemData.class).getMailProp(ud.getRegistratura(), selectMailBox);
					flash.put("mailMessage", selectedMail);
					flash.put("prop", prop);
					flash.put("selectMailBox", selectMailBox);			
					selectedMail = null; // ако се откаже, при връщане в раб. плот - да няма избран мейл
					return regJalba ? "docEdit.jsf?faces-redirect=true&fw=5" : "docEdit.jsf?faces-redirect=true&fw=3";
				} else {
					// има регистриран документ вече
					String msg = ": " + rez[1] + " / " + convertDateToString((Date) rez[2]);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,
							getMessageResourceString(LABELS, "registred.mail") + msg);
				}
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при извличане на пощенски кутии !!!!", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			} finally {
				JPA.getUtil().closeConnection(); // getMailProp - go iska
			}
		}

		return null;
	}
	
	
	/**
	 * Пренасочване към подписване
	 * 
	 */
	public String actionGotoGroupDocSign() {
		
		idDocs = new ArrayList<>();
		for (HashMap.Entry<Long, Boolean> entry : selectedDocs.entrySet()) { 
			if(entry.getValue().booleanValue()) {
				idDocs.add(entry.getKey());				
			}	       
	    }
		
		if(idDocs.isEmpty()) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Моля , изберете документи от списъка!");
			return "";
		
		} else {			
			HttpSession session = (HttpSession) JSFUtils.getExternalContext().getSession(false);
			session.setAttribute("idDocs", idDocs);
			session.setAttribute("selMenu", selectedCodeMenuObject);
			session.setAttribute("idTasks", idTasks);
		
			return "dashboardGroupDocSign.jsf?faces-redirect=true";
		}
		
	}
	
	
	public void actionRejectCEOC() {
		if(idEgov!=null) {
			try {
				JPA.getUtil().runInTransaction(() -> { 
					
					EgovMessagesDAO daoEgov = new EgovMessagesDAO(ud);
					EgovMessages egovMess = daoEgov.findById(idEgov);
					egovMess.setMsgRn(null);
					egovMess.setMsgRnDate(null);
					egovMess.setMsgStatus("DS_REJECTED");
					egovMess.setPrichina(textReject);
					egovMess.setMsgStatusDate(new Date());
				
					// запис на промяната на статуса в таблица Egov_Messages
					egovMess = daoEgov.save(egovMess);		
					
					// Отговор - само за СЕОС
					if(Objects.equals("S_SEOS", egovMess.getSource())) {
						daoEgov.saveStatusResponseOtkazMessage(egovMess, textReject, ud);
					}
					
				});
			} catch (BaseException e) {
				LOGGER.error("Грешка при формиране на отказ от регистрация - СЕОС! ", e); 
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}
			
			//презареждане на списъка
			try {
				String guidSeos = SearchUtils.asString(getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRATURI, ud.getRegistratura(), SysConstants.CODE_DEFAULT_LANG, new Date(), OmbClassifAdapter.REGISTRATURI_INDEX_GUID_SEOS));
				String guidSSEV = SearchUtils.asString(getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRATURI, ud.getRegistratura(), SysConstants.CODE_DEFAULT_LANG, new Date(), OmbClassifAdapter.REGISTRATURI_INDEX_GUID_SSEV)) ;

				objectList = new LazyDataModelSQL2Array(new EgovMessagesDAO(ud).createFilterEgovMessages(guidSeos, guidSSEV,  textSearch ,textSearchFull), A5_DOC_SROK);
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при извличнане на системна настройка (getItemSpecific) guidSeos ,guidSSEV ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}
		}
	}

	/**
	 * Проверка за заключен документ / имейл
	 * 
	 * @param idObj
	 * @return
	 */
	private boolean checkForLock(Integer idObj, Integer codeObj) {
		boolean res = true;
		LockObjectDAO daoL = new LockObjectDAO();
		try {
			Object[] obj = daoL.check(ud.getUserId(), codeObj, idObj);
			if (obj != null) {
				res = false;
				String msg = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR,
						Integer.valueOf(obj[0].toString()), getUserData().getCurrentLang(), new Date()) + " / "
						+ DateUtils.printDate((Date) obj[1]);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,
						getMessageResourceString(LABELS, "docu.docLocked"), msg);
			}
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при проверка за заключен обект! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
		return res;
	}

	public Map<Long, String> getMapCountsOptions1() {
		return mapCountsOptions1;
	}

	/**
	 * подскзака в списъка със задачи - мнение при приключване + коментар
	 * 
	 * @param comment
	 * @param opinion
	 * @return
	 */
	public String titleInfo(Integer opinion, String comment) {
		StringBuilder title = new StringBuilder();
		if (opinion != null) {
			try {
				String opinionTxt = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_TASK_OPINION, opinion,
						getUserData().getCurrentLang(), new Date());
				title.append(getMessageResourceString(LABELS, "docu.modalRefMnenie") + ": " + opinionTxt + "; ");
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане на данни за документ! ", e);
			}
		}
		if (!SearchUtils.isEmpty(comment)) {
			title.append(comment);
		}
		return title.toString();
	}

	
	
	/*******  Жалба - запис на файловете от мейла в избрана жалба	 *****/

	private LazyDataModelSQL2Array jalbiList;
	private String jalbaRN;
	private Integer jalbaPeriodR;
	private Date jalbaDateFrom;
	private Date jalbaDateTo;
	
	/**
	 * от модалния за жалби 
	 */
	public void changeJalbaPeriodR () {
	   	if (this.jalbaPeriodR != null) {
				Date[] di;
				di = DateUtils.calculatePeriod(this.jalbaPeriodR);
				setJalbaDateFrom(di[0]);
				setJalbaDateTo(di[1]);		
	   	} else {
	   		setJalbaDateFrom(null);
	   		setJalbaDateTo(null);
		}
    }
   
	/**
	 * от модалния за жалби 
	 */
	public void changeJalbaDate() { 
		this.setJalbaPeriodR(null);
	}
	
	/**
	 * При отваряне на модалния за жалба, към която да запише файловете от емейла
	 */
	public void actionClearJalba() {
		jalbaPeriodR = 18;
		changeJalbaPeriodR();
		jalbiList = null;
		jalbaRN = null;	
	}
	
	/**
	 * Търси жалба, към която да запише файловете от емейла
	 */
	public void actionFindJalba() {
		try {
			if(!SearchUtils.isEmpty(jalbaRN)) {
				DocJalbaSearch tmp = new DocJalbaSearch();
				tmp.setDocDateFrom(jalbaDateFrom);
				tmp.setDocDateTo(jalbaDateTo);
				tmp.setRnDoc(jalbaRN.trim());
				tmp.setRnDocEQ(true); 
				tmp.buildQueryJalbaList(getUserData(), true, getSystemData());
			    setJalbiList(new LazyDataModelSQL2Array(tmp, "a1 desc")); 			
			}
			//else {
				//JSFUtils.addMessage("dashboard:jRn",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "jp.jalbaRn")));
			//}
		} catch (DbErrorException e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,  e.getMessage());
			LOGGER.info(e.getMessage(), e);
		}
		
	}
	
	/**
	 * Разглеждане на жалба (от модалния)
	 */
	public String actionGotoViewJ(Integer idObj) {
		return "docView.xhtml?faces-redirect=true&idObj=" + idObj;
	}
	
	/**
	 * записва файловете от мейла в избраната жалба
	 * @param idDoc - ид на жалба
	 * @return
	 */
	public String actionAddFilesТоJalba(Integer idDoc)  {		
		if(idDoc != null) {  		
			try {
				JPA.getUtil().runInTransaction(() -> { 
					// файлове от мейла... 
					List<Files> filesList = new ArrayList<>();
					if(selectedMail.getAttachements() != null && !selectedMail.getAttachements().isEmpty()) {
						Iterator<String> it = selectedMail.getAttachements().keySet().iterator();
						Files newFile;
						while (it.hasNext()) {
							String keyVar = it.next();
							newFile =  loadFilesFromMsg(keyVar, "application/x-download", selectedMail.getAttachements().get(keyVar));
							filesList.add(newFile);	
						}
					}	
					
					if (filesList != null && !filesList.isEmpty()) {
						FilesDAO filesDao = new FilesDAO(getUserData());
						for (Files f : filesList) {
							filesDao.saveFileObject(f, idDoc, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
						}
					}
					// TODO  да пратя на жалбоподателя, че файловете са приети!!!
					
					//да обнови броя на файловете в документа и да прати нотификация
					DocDAO dao = new DocDAO(getUserData());
					dao.updateCountFiles(idDoc, filesList.size(), OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA, getSystemData());
				});
				
				// да махна е-майла от списъка
				Properties prop = getSystemData(SystemData.class).getMailProp(ud.getRegistratura(),selectMailBox);
				new Mailer().moveMailUIDRejected(prop, selectedMail.getMessUID(), getSystemData());
				selectedMail = null;
				
			} catch (BaseException e) {
				LOGGER.error("Грешка при запис на нови файлове от е-мейл към жалбата! BaseException", e);				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG),e.getMessage());
			} catch (MessagingException e) {
				LOGGER.error("Грешка при преместване на е-мейл в папка -отказани (бутон Жалба - нови файлове)! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, GENERAL_FORMATEXC), e.getMessage());
			}
		}
		return null;
	}
	
	/**
	 * подготвя файловете от емейл-а да бъдат записани в жалбата 
	 * @param fileName
	 * @param fileCType
	 * @param content
	 * @return
	 */
	private Files loadFilesFromMsg(String fileName, String fileCType, byte[] content) {
		
		Files newFile = new Files();
		newFile.setFilename(fileName);
		newFile.setContentType(fileCType);
		newFile.setContent(content);
		newFile.setOfficial(OmbConstants.CODE_ZNACHENIE_DA);
								
		boolean bb = verifySignFile(fileName, content);
		
		if(bb) {
			newFile.setSigned(Constants.CODE_ZNACHENIE_DA);
		} else {
			newFile.setSigned(Constants.CODE_ZNACHENIE_NE);
		}
	
		return newFile;
	}
	
	/**
	 * проверка за подписан файл
	 * @param filename
	 * @param content
	 * @return
	 */
	private boolean verifySignFile(String filename, byte[] content )  {
		
		boolean bb = false;
		try {
			
			Set<X509Certificate> trust = getSystemData().getTrustedCerts();
			
			if(filename.endsWith(".docx")) {			
				List<VerifySignature> rez = new SignatureUtils().verifyWordExcelSignatures(content, trust);
				if(rez!=null && !rez.isEmpty()) {
					bb = true;
				} 
			} else if(filename.endsWith(".pdf")) {			
				List<VerifySignature> rez = new SignatureUtils().verifyPDFSignatures(content, trust);
				if(rez!=null && !rez.isEmpty()) {
					bb = true;
				}
			}
			
		} catch (UnexpectedResultException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  "Неочаквана Грешка при извличане данни за сертификатите на файл!", e.getMessage());
		}
		
		return bb;
	}
	
	/***  End - жалби ***/
	
	
	
	
	
	
	public void setMapCountsOptions1(Map<Long, String> mapCountsOptions1) {
		this.mapCountsOptions1 = mapCountsOptions1;
	}

	public Object[] getSelectedObject() {
		return selectedObject;
	}

	public void setSelectedObject(Object[] selectedObject) {
		this.selectedObject = selectedObject;
	}

	public boolean isShowTaskModuleOption() {
		return showTaskModuleOption;
	}

	public void setShowTaskModuleOption(boolean showTaskModuleOption) {
		this.showTaskModuleOption = showTaskModuleOption;
	}

	public List<Files> getFilesListDoc() {
		return filesListDoc;
	}

	public void setFilesListDoc(List<Files> filesListDoc) {
		this.filesListDoc = filesListDoc;
	}

	public String convertDateToString(Date date) {

		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");

		return sdf.format(date);
	}

	public Integer getIdDoc() {
		return idDoc;
	}

	public void setIdDoc(Integer idDoc) {
		this.idDoc = idDoc;
	}

	public String getTitleDoc() {
		return titleDoc;
	}

	public void setTitleDoc(String titleDoc) {
		this.titleDoc = titleDoc;
	}

	public boolean isShowDocTaskModuleOption() {
		return showDocTaskModuleOption;
	}

	public void setShowDocTaskModuleOption(boolean showDocTaskModuleOption) {
		this.showDocTaskModuleOption = showDocTaskModuleOption;
	}

	public boolean isShowDocTaskNew() {
		return showDocTaskNew;
	}

	public void setShowDocTaskNew(boolean showDocTaskNew) {
		this.showDocTaskNew = showDocTaskNew;
	}

	public Integer getIdTask() {
		return idTask;
	}

	public void setIdTask(Integer idTask) {
		this.idTask = idTask;
	}

	public String getDocInfo() {
		return docInfo;
	}

	public void setDocInfo(String docInfo) {
		this.docInfo = docInfo;
	}

	public Task getTmpTask() {
		return tmpTask;
	}

	public void setTmpTask(Task tmpTask) {
		this.tmpTask = tmpTask;
	}

	public boolean isGotoDocPage() {
		return gotoDocPage;
	}

	public void setGotoDocPage(boolean gotoDocPage) {
		this.gotoDocPage = gotoDocPage;
	}

	public Date getDateDoc() {
		return dateDoc;
	}

	public void setDateDoc(Date dateDoc) {
		this.dateDoc = dateDoc;
	}

	public List<SystemClassif> getOpinionLst() {
		return opinionLst;
	}

	public void setOpinionLst(List<SystemClassif> opinionLst) {
		this.opinionLst = opinionLst;
	}

	public String getDocCorespAvtor() {
		return docCorespAvtor;
	}

	public void setDocCorespAvtor(String docCorespAvtor) {
		this.docCorespAvtor = docCorespAvtor;
	}

	public List<String> getMailBoxLst() {
		return mailBoxLst;
	}

	public void setMailBoxLst(List<String> mailBoxLst) {
		this.mailBoxLst = mailBoxLst;
	}

	public boolean isShowDeloDocModuleOption() {
		return showDeloDocModuleOption;
	}

	public void setShowDeloDocModuleOption(boolean showDeloDocModuleOption) {
		this.showDeloDocModuleOption = showDeloDocModuleOption;
	}

	public LazyDataModelMailList getMailList() {
		return mailList;
	}

	public void setMailList(LazyDataModelMailList mailList) {
		this.mailList = mailList;
	}

	public MyMessage getSelectedMail() {
		return selectedMail;
	}

	public void setSelectedMail(MyMessage selectedMail) {
		this.selectedMail = selectedMail;
	}

	public String getSelectMailBox() {
		return selectMailBox;
	}

	public void setSelectMailBox(String selectMailBox) {
		this.selectMailBox = selectMailBox;
	}

	public String getTextReject() {
		return textReject;
	}

	public void setTextReject(String textReject) {
		this.textReject = textReject;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public boolean isDisplayModalSelObj() {
		return displayModalSelObj;
	}

	public void setDisplayModalSelObj(boolean displayModalSelObj) {
		this.displayModalSelObj = displayModalSelObj;
	}

	public GuestPreferencesDashboard getGpd() {
		return gpd;
	}

	public void setGpd(GuestPreferencesDashboard gpd) {
		this.gpd = gpd;
	}

	public String getTextSearch() {
		return textSearch;
	}

	public void setTextSearch(String textSearch) {
		this.textSearch = textSearch;
	}

	public boolean isForCompetence() {
		return forCompetence;
	}

	public void setForCompetence(boolean forCompetence) {
		this.forCompetence = forCompetence;
	}

	public boolean isShowForCompetence() {
		return showForCompetence;
	}

	public void setShowForCompetence(boolean showForCompetence) {
		this.showForCompetence = showForCompetence;
	}

	public String getTaskSendInfo() {
		return taskSendInfo;
	}

	public void setTaskSendInfo(String taskSendInfo) {
		this.taskSendInfo = taskSendInfo;
	}

	public String getTaskCommentInfo() {
		return taskCommentInfo;
	}

	public void setTaskCommentInfo(String taskCommentInfo) {
		this.taskCommentInfo = taskCommentInfo;
	}

	public boolean isShowNewTask() {
		return showNewTask;
	}

	public void setShowNewTask(boolean showNewTask) {
		this.showNewTask = showNewTask;
	}

	public Map<Integer, Integer> getTypeSignAction() {
		return typeSignAction;
	}

	public void setTypeSignAction(Map<Integer, Integer> typeSignAction) {
		this.typeSignAction = typeSignAction;
	}

	public List<Object[]> getTaskHistoryList() {
		return taskHistoryList;
	}

	public void setTaskHistoryList(List<Object[]> taskHistoryList) {
		this.taskHistoryList = taskHistoryList;
	}

	public boolean isShowSignFileOptions() {
		return showSignFileOptions;
	}

	public void setShowSignFileOptions(boolean showSignFileOptions) {
		this.showSignFileOptions = showSignFileOptions;
	}

	public List<Integer> getStatusTaskNotActive() {
		return statusTaskNotActive;
	}

	public void setStatusTaskNotActive(List<Integer> statusTaskNotActive) {
		this.statusTaskNotActive = statusTaskNotActive;
	}

	public String getStatusTask() {
		return statusTask;
	}

	public void setStatusTask(String statusTask) {
		this.statusTask = statusTask;
	}

	public String getTaskMnenie() {
		return taskMnenie;
	}

	public void setTaskMnenie(String taskMnenie) {
		this.taskMnenie = taskMnenie;
	}

	public boolean isShowFileUpload() {
		return showFileUpload;
	}

	public void setShowFileUpload(boolean showFileUpload) {
		this.showFileUpload = showFileUpload;
	}

	public boolean isShowTaskSpr() {
		return showTaskSpr;
	}

	public void setShowTaskSpr(boolean showTaskSpr) {
		this.showTaskSpr = showTaskSpr;
	}

	public LazyDataModelSQL2Array getTasksDocList() {
		return tasksDocList;
	}

	public void setTasksDocList(LazyDataModelSQL2Array tasksDocList) {
		this.tasksDocList = tasksDocList;
	}

	public List<Integer> getOtherStatusList() {
		return otherStatusList;
	}

	public void setOtherStatusList(List<Integer> otherStatusList) {
		this.otherStatusList = otherStatusList;
	}

	public List<Integer> getNeizpalneniStatusList() {
		return neizpalneniStatusList;
	}

	public void setNeizpalneniStatusList(List<Integer> neizpalneniStatusList) {
		this.neizpalneniStatusList = neizpalneniStatusList;
	}

	public Boolean getTextSearchFull() {
		return textSearchFull;
	}

	public void setTextSearchFull(Boolean textSearchFull) {
		this.textSearchFull = textSearchFull;
	}

	public Integer getExtDaysTask() {
		return extDaysTask;
	}

	public void setExtDaysTask(Integer extDaysTask) {
		this.extDaysTask = extDaysTask;
	}

	public Integer getIdEgov() {
		return idEgov;
	}

	public void setIdEgov(Integer idEgov) {
		this.idEgov = idEgov;
	}

	public Map<Long, Boolean> getSelectedDocs() {
		return selectedDocs;
	}

	public void setSelectedDocs(Map<Long, Boolean> selectedDocs) {
		this.selectedDocs = selectedDocs;
	}
	
	//Това се налага защото jsf HashMap не работи с integer
	public Long castLong(Integer i) {
		
		if(i == null) return -1L;
		
		return Long.valueOf(i); 
		
	}
	
	public boolean isShowGroupDocTaskNew() {
		return showGroupDocTaskNew;
	}

	public void setShowGroupDocTaskNew(boolean showGroupDocTaskNew) {
		this.showGroupDocTaskNew = showGroupDocTaskNew;
	}
	
	public void showGroupTaskBnt(Object[] row) {
		showGroupDocTaskNew = false;
		showGroupDocSign = false;
			
		for (HashMap.Entry<Long, Boolean> entry : selectedDocs.entrySet()) {
			if(entry.getValue().booleanValue()) {
				showGroupDocTaskNew = true;
				break;
			}
		}
		
		// За съгласуване  // За подпис да се показва бутона за гр. подписване
		if(showGroupDocTaskNew  && 
				(selectedCodeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_SAGL 
				 || selectedCodeMenuObject.intValue() == OmbConstants.CODE_ZNACHENIE_DASHBOARD_FOR_PODPIS)) {
			showGroupDocSign = true;
		
			 //подържаме и списък с ид та на задачи
			 Long key = SearchUtils.asLong(row[0]);
			 Boolean item = selectedDocs.get(key);
			
			 if(item!=null && item.booleanValue()) {
				 idTasks.add(SearchUtils.asInteger(row[8]));
			 } else {
				 idTasks.remove(SearchUtils.asInteger(row[8]));
			 }
		}
	}
	
	public void actionShowGropuMpTask() {
		idDocs = new ArrayList<>();
		for (HashMap.Entry<Long, Boolean> entry : selectedDocs.entrySet()) {
			if(entry.getValue().booleanValue()) {
				idDocs.add(entry.getKey());
				
			}
	       
	    }
		
		if(idDocs.isEmpty()) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Моля , изберете документи от списъка!");
		} else {
			
			PrimeFaces.current().executeScript("PF('groupTaskMP').show();");
			
		}
		
	}
	
	public void actionHideGroupTaskMP() {
		selectedDocs.clear();
		idDocs.clear();
		idTasks.clear();
		
		showGroupDocTaskNew = false;
		
		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(beanMessages, "dashboard.taskSuccess"));
	}
	
	/**
	 * групово определяне на експерт по жалба
	 */
	public void actionShowGropJalbiExp() {
		jalbaExpert = null;
		if(zvenoSlList == null) {
		  try {
			zvenoSlList = getSystemData().getChildrenOnNextLevel(OmbConstants.CODE_CLASSIF_ADMIN_STR, ud.getZveno(), new Date(), getCurrentLang());
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане на служители от избрано звено ", e);		
			}
		}
		idDocs = new ArrayList<>();
		for (HashMap.Entry<Long, Boolean> entry : selectedDocs.entrySet()) {
			if(entry.getValue().booleanValue()) {
				idDocs.add(entry.getKey());				
			}
	       
	    }
		
		if(idDocs.isEmpty()) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Моля , изберете жалби от списъка!");
		} else {
			PrimeFaces.current().executeScript("PF('groupJalbiExp').show();");
		}
	}
	
	
	/**
	 * групово определяне на експерт по жалба - save
	 */
	public void actionSaveGroupJalbiExp() {		
		if (jalbaExpert != null) {			
			//save
			int br = idDocs.size();
			DocJalbaDAO daoJ = new DocJalbaDAO(getUserData());			
			try {
				JPA.getUtil().runInTransaction(() -> { 
					daoJ.groupSetExpert(idDocs, jalbaExpert, getSystemData());				
				});				
				selectedDocs.clear();
				idDocs.clear();
				showGroupDocTaskNew = false;
				PrimeFaces.current().executeScript("PF('groupJalbiExp').hide();");
				changeCountSection(OmbConstants.CODE_ZNACHENIE_DASHBOARD_OPR_EXP, br);
				changeCountSection(OmbConstants.CODE_ZNACHENIE_DASHBOARD_PROVERKA, -br);
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(beanMessages, "dashboard.jalbaExpSuccess"));
			} catch (BaseException  e) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,  e.getMessage());
				LOGGER.info(e.getMessage(), e);
			}
		}
	}
	
	
	
	public List<Long> getIdDocs() {
		return idDocs;
	}

	public void setIdDocs(List<Long> idDocs) {
		this.idDocs = idDocs;
	}

	public boolean isShowGroupDocSign() {
		return showGroupDocSign;
	}

	public void setShowGroupDocSign(boolean showGroupDocSign) {
		this.showGroupDocSign = showGroupDocSign;
	}

	public Map<Long, Map<Long , Boolean>> getSelectedTaskDocs() {
		return selectedTaskDocs;
	}

	public void setSelectedTaskDocs(Map<Long, Map<Long , Boolean>> selectedTaskDocs) {
		this.selectedTaskDocs = selectedTaskDocs;
	}

	public List<Integer> getIdTasks() {
		return idTasks;
	}

	public void setIdTasks(List<Integer> idTasks) {
		this.idTasks = idTasks;
	}

	public StreamedContent getStreamFile() {
		return streamFile;
	}

	public void setStreamFile(StreamedContent streamFile) {
		this.streamFile = streamFile;
	}
	
	public void actionStreamFile(String key) {
		
		byte [] dataContent = selectedMail.getAttachements().get(key);
		
//		 setStreamFile(DefaultStreamedContent.builder()
//	                .name("key")
//	                .contentType("application/x-download")
//	                .stream(() -> new ByteArrayInputStream(dataContent))
//	                .build());
		if(dataContent!= null) {
			try { 
			 
				FacesContext facesContext = FacesContext.getCurrentInstance();
				ExternalContext externalContext = facesContext.getExternalContext();
	
				HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
				String agent = request.getHeader("user-agent");
	
				String codedfilename = "";
	
				if (null != agent && (-1 != agent.indexOf("MSIE") || -1 != agent.indexOf("Mozilla") && -1 != agent.indexOf("rv:11") || -1 != agent.indexOf("Edge"))) {
					codedfilename = URLEncoder.encode(key, "UTF8");
				} else if (null != agent && -1 != agent.indexOf("Mozilla")) {
					codedfilename = MimeUtility.encodeText(key, "UTF8", "B");
				} else {
					codedfilename = URLEncoder.encode(key, "UTF8");
				}
	
				externalContext.setResponseHeader("Content-Type", "application/x-download");
				externalContext.setResponseHeader("Content-Length", dataContent.length + "");
				externalContext.setResponseHeader("Content-Disposition", "attachment;filename=\"" + codedfilename + "\"");
				externalContext.getResponseOutputStream().write(dataContent);
	
				facesContext.responseComplete();
	
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,"general.fileError"));
				
			}
			
		} else {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,"general.fileError"));
			
		}
		 
		 
	}
	
	
	public LazyDataModelSQL2Array getJalbiList() {
		return jalbiList;
	}

	public void setJalbiList(LazyDataModelSQL2Array jalbiList) {
		this.jalbiList = jalbiList;
	}

	public String getJalbaRN() {
		return jalbaRN;
	}

	public void setJalbaRN(String jalbaRN) {
		this.jalbaRN = jalbaRN;
	}


	public Integer getJalbaPeriodR() {
		return jalbaPeriodR;
	}

	public void setJalbaPeriodR(Integer jalbaPeriodR) {
		this.jalbaPeriodR = jalbaPeriodR;
	}

	public Date getJalbaDateFrom() {
		return jalbaDateFrom;
	}

	public void setJalbaDateFrom(Date jalbaDateFrom) {
		this.jalbaDateFrom = jalbaDateFrom;
	}

	public Date getJalbaDateTo() {
		return jalbaDateTo;
	}

	public void setJalbaDateTo(Date jalbaDateTo) {
		this.jalbaDateTo = jalbaDateTo;
	}



	public boolean isShowJalbiOption() {
		return showJalbiOption;
	}

	public void setShowJalbiOption(boolean showJalbiOption) {
		this.showJalbiOption = showJalbiOption;
	}



	public boolean isShowProverkiOption() {
		return showProverkiOption;
	}

	public void setShowProverkiOption(boolean showProverkiOption) {
		this.showProverkiOption = showProverkiOption;
	}





	public List<Object[]> getLinkDocList() {
		return linkDocList;
	}

	public void setLinkDocList(List<Object[]> linkDocList) {
		this.linkDocList = linkDocList;
	}





	public boolean isGotoJalbaPage() {
		return gotoJalbaPage;
	}

	public void setGotoJalbaPage(boolean gotoJalbaPage) {
		this.gotoJalbaPage = gotoJalbaPage;
	}



	public Integer getJalbaExpert() {
		return jalbaExpert;
	}

	public void setJalbaExpert(Integer jalbaExpert) {
		this.jalbaExpert = jalbaExpert;
	}



	public List<SystemClassif> getZvenoSlList() {
		return zvenoSlList;
	}

	public void setZvenoSlList(List<SystemClassif> zvenoSlList) {
		this.zvenoSlList = zvenoSlList;
	}



	public String getTitleRegDoc() {
		return titleRegDoc;
	}

	public void setTitleRegDoc(String titleRegDoc) {
		this.titleRegDoc = titleRegDoc;
	}



	public String getAllJlb() {
		return allJlb;
	}

	public void setAllJlb(String allJlb) {
		this.allJlb = allJlb;
	}



	static public class ColumnModel implements Serializable {

		private static final long serialVersionUID = 4314195886286451513L;
		private String header;
		private String property;
		private String width;
		private String classWidth;

		// 0-текст, 1-класификация, 2-дата, 3-дата час, 4-сбирщина, 5- множествено, 6- чекбокс за селектиране, 7 - спешност , 8 - начин на подписване
		// разкодиране от класификация
		private Integer columnTypeValue;
		private Integer codeClassif;
		private String sortColumn;

		public ColumnModel(String header, String property, String width, String sortColumn, String classWidth) {
			this.header = header;
			this.property = property;
			this.columnTypeValue = 0; // по подразбиране текст
			this.width = width;
			this.sortColumn = sortColumn;
			this.classWidth = classWidth;
		}

		public ColumnModel(String header, String property, Integer columnTypeValue, String width, String sortColumn, String classWidth) {
			this.header = header;
			this.property = property;
			this.columnTypeValue = columnTypeValue;
			this.width = width;
			this.sortColumn = sortColumn;
			this.classWidth = classWidth;
		}

		public ColumnModel(String header, String property, Integer columnTypeValue, String width, Integer codeClassif,String sortColumn, String classWidth) {
			this.header = header;
			this.property = property;
			this.columnTypeValue = columnTypeValue;
			this.width = width;
			this.codeClassif = codeClassif;
			this.sortColumn = sortColumn;
			this.classWidth = classWidth;
		}

		public String getHeader() {
			return header;
		}

		public String getProperty() {
			return property;
		}

		public Integer getColumnTypeValue() {
			return columnTypeValue;
		}

		public void setColumnTypeValue(Integer columnTypeValue) {
			this.columnTypeValue = columnTypeValue;
		}

		public String getWidth() {
			return width;
		}

		public void setWidth(String width) {
			this.width = width;
		}

		public Integer getCodeClassif() {
			return codeClassif;
		}

		public void setCodeClassif(Integer codeClassif) {
			this.codeClassif = codeClassif;
		}

		public String getSortColumn() {
			return sortColumn;
		}

		public void setSortColumn(String sortColumn) {
			this.sortColumn = sortColumn;
		}

		public String getClassWidth() {
			return classWidth;
		}

		public void setClassWidth(String classWidth) {
			this.classWidth = classWidth;
		}
		
	}

}

package com.ib.omb.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.faces.view.facelets.FaceletContext;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.component.export.PDFOptions;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.TaskDAO;
import com.ib.omb.db.dto.Task;
import com.ib.omb.search.TaskSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;


@Named
@ViewScoped
public class TasksList  extends IndexUIbean{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7022253329017403534L;
	private static final Logger LOGGER = LoggerFactory.getLogger(TasksList.class);
	public static final String	UIBEANMESSAGES	= "ui_beanMessages";
	public static final String  MSGPLSINS		= "general.pleaseInsert";
	public static final String  ERRDATABASEMSG 	= "general.errDataBaseMsg";
	public static final String  SUCCESSAVEMSG 	= "general.succesSaveMsg";
	public static final String  OBJINUSEMSG 	= "general.objectInUse";
	public static final String  VALIDATTRIBS    = "validAttribs";
	
	private Task task = new Task();
	private List<SelectItem> vidTask = new ArrayList<>();
	private Integer[] selectedVidTask;
	private List<SelectItem> statusTaskList = new ArrayList<>();
	private Integer[] selectedStatusTask;
	private List<SelectItem> endOpinionTaskList = new ArrayList<>();
	private Integer[] selectedEndOpinionTask;
	private Integer periodDoc = null;
	private Integer periodR = null;
	private Integer periodV = null;
	private Integer periodZ = null;
	
	private Integer slujitel;
	private List<SelectItem> vidSlujList = new ArrayList<>();
	private Integer[] selectedVidSluj;
	
	private LazyDataModelSQL2Array tasksListResult;
	private TaskSearch taskSearch;
	private List<SystemClassif> scList = new ArrayList<>();
	private List<SystemClassif> scList2 = new ArrayList<>();
	private List<SystemClassif> scList3 = new ArrayList<>();
	private List<SystemClassif> selectedEmplStatus = new ArrayList<>();
	private List<SelectItem> registraturaList = new ArrayList<>();
	private boolean showRegistaturiList = false;
	private transient  List<Object[]> taskSelectedAllM = new ArrayList<>();
	private transient  List<Object[]> taskSelectedTmp = new ArrayList<>();
	
	private Date dateGrupovoSnemane;
	private String textGrupovoSnemane;
	private String textGrupovoIzpalnenie;
	private transient TaskDAO tDao;
	private List<Integer> listExecsGrSmqna = new ArrayList<>();
	private List<SystemClassif> scListGrSmqna = new ArrayList<>();
	private Integer endOpinion;
	private List<SelectItem> opinionLst = new ArrayList<>();
	private String srokPattern;

	private boolean showOpinionList = false;
	private boolean isViewBool = false;
	private boolean hasAccessTaskRegis = true;
	private int registraturaRadioBtn = 1;
	private boolean showGrupoviDeinosti = false;
	private boolean showGroupDeleteBtn = false;
	private boolean isOkToRender; // да се показва ли бутон за пълнотекстово търсене
	
	private boolean fillTextSearch = false;
	
	private boolean showTasksMySubordinates = false; // "Задачи на мои подчинени" да се вижда само ако потребителят е на ръководна длъжност 
	
	@PostConstruct
	public void initData() {
		taskSearch = new TaskSearch(null);
		tDao = new TaskDAO(getUserData(UserData.class));
		try {
			vidTask = createItemsList(false, OmbConstants.CODE_CLASSIF_TASK_VID, taskSearch.getRegDateTo() == null ? new Date() : taskSearch.getRegDateTo(), true);
			statusTaskList = createItemsList(false, OmbConstants.CODE_CLASSIF_TASK_STATUS, taskSearch.getRegDateTo() == null ? new Date() : taskSearch.getRegDateTo(), true);
			endOpinionTaskList = createItemsList(false, OmbConstants.CODE_CLASSIF_TASK_OPINION, taskSearch.getRegDateTo() == null ? new Date() : taskSearch.getRegDateTo(), true);
			vidSlujList = createItemsList(false, OmbConstants.CODE_CLASSIF_TASK_REF_ROLE , new Date(), true);
			showRegistaturiList = getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW);
			showGrupoviDeinosti = getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_GROUP_TASK);
			showGroupDeleteBtn  = getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_DELETE_TASK);

			registraturaList = createItemsList(true, OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS , new Date(), false);
			FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
			String isViewStr = (String) faceletContext.getAttribute("isView"); // 0 - актуализациял 1 - разглеждане
			if(isViewStr != null && !isViewStr.contentEquals("null") && isViewStr.contentEquals("1")) {
				isViewBool = true;
			}
			if(getUserData(UserData.class).getAccessZvenoList()!=null) {
				showTasksMySubordinates = true;
			}
			//LOGGER.debug(isViewStr);
			//String fullTextSetting = getSystemData().getSettingsValue("general.fulltextsearch.enabled");
			//isOkToRender = !("0".equals(fullTextSetting));
			isOkToRender = false;
			// да се виждат ли часове и минути в срока на задачата
			// взема се настройкaт на регистратурата на потребителя - за сега, в списъка,  ще се определя само от текущата регистртура!
			Integer s1 = ((SystemData) getSystemData()).getRegistraturaSetting(getUserData(UserData.class).getRegistratura(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_15);
			if(Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_DA)) {
				setSrokPattern("dd.MM.yyyy HH:mm");
			}else {
				setSrokPattern("dd.MM.yyyy");
			}
			
		} catch (DbErrorException | UnexpectedResultException e) {
			LOGGER.error("Грешка при инициализиране на филтър за търсене на задачи! ", e);
		}
		hasAccessTaskRegis = getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_TASK_FULL_EDIT);
	}
	
	public void search() {
		
		taskSelectedAllM.clear();
		if (taskSelectedTmp != null) {
			taskSelectedTmp.clear();
		}
		taskSearch.setTaskTypeList(Arrays.asList(selectedVidTask));
		taskSearch.setStatusList(Arrays.asList(selectedStatusTask));
		taskSearch.setEndOpinionList(Arrays.asList(getSelectedEndOpinionTask()));

		setRegistratira();
		taskSearch.buildQueryTaskList(getUserData(), isViewBool);
		tasksListResult = new LazyDataModelSQL2Array(taskSearch, "a1 desc"); 
	}
	
	public void actionSearchFullText() {
		
		taskSelectedAllM.clear();
		if (taskSelectedTmp != null) {
			taskSelectedTmp.clear();
		}
		taskSearch.setTaskTypeList(Arrays.asList(selectedVidTask));
		taskSearch.setStatusList(Arrays.asList(selectedStatusTask));
		taskSearch.setEndOpinionList(Arrays.asList(getSelectedEndOpinionTask()));
		
		setRegistratira();
		taskSearch.buildQueryTaskListWithFullText(getUserData(), isViewBool);
		tasksListResult = new LazyDataModelSQL2Array(taskSearch, "a1 desc"); 
	}
	
	private void setRegistratira() {
		boolean bb = registraturaRadioBtn == 1;
		taskSearch.setFullEditInRegistratura(!bb); 
	}
	
	public void actionClear() {
		taskSearch = new TaskSearch(null);
		tasksListResult = null;
		periodDoc = null;
		periodR = null;
		periodV = null;
		periodZ = null;
		selectedVidTask = null;
		selectedStatusTask = null;
		setSelectedEndOpinionTask(null);
		selectedVidSluj = null;
		scList = new ArrayList<>();
		scList2 = new ArrayList<>();
		scList3 =new ArrayList<>();
		selectedEmplStatus = new ArrayList<>();
		taskSelectedAllM = new ArrayList<>();
		taskSelectedTmp = new ArrayList<>();
		registraturaRadioBtn = 1;
		fillTextSearch = false;
	}

	
	public void onItemUnselect(UnselectEvent<?> event) {
		FacesContext context = FacesContext.getCurrentInstance();
         
        FacesMessage msg = new FacesMessage();
        msg.setSummary("Item unselected: " + event.getObject().toString());
        msg.setSeverity(FacesMessage.SEVERITY_INFO);
         
        context.addMessage(null, msg);
    }
	
	public void changePeriodDoc () {
		
    	if (this.periodDoc != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodDoc);
			taskSearch.setDocDateFrom(di[0]);
			taskSearch.setDocDateTo(di[1]);		
    	} else {
    		taskSearch.setDocDateFrom(null);
    		taskSearch.setDocDateTo(null);
		}
    }

	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	public void changePeriodR () {
		
    	if (this.periodR != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodR);
			taskSearch.setRegDateFrom(di[0]);
			taskSearch.setRegDateTo(di[1]);		
    	} else {
    		taskSearch.setRegDateFrom(null);
    		taskSearch.setRegDateTo(null);
		}
    }
	
	/** Метод за смяна на датите при избор на период за възлагане при търсене.
	 * 
	 * 
	 */
	public void changePeriodV () {
		
    	if (this.periodV != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodV);
			taskSearch.setAssignDateFrom(di[0]);
			taskSearch.setAssignDateTo(di[1]);		
    	} else {
    		taskSearch.setAssignDateFrom(null);
    		taskSearch.setAssignDateTo(null);
		}
    }
	
	public void changePeriodZ () {
		
    	if (this.periodZ != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodZ);
			taskSearch.setSrokDateFrom(di[0]);
			taskSearch.setSrokDateTo(di[1]);		
    	} else {
    		taskSearch.setSrokDateFrom(null);
    		taskSearch.setSrokDateTo(null);
		}
    }
	
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "tasks.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, null , null, null);		
     
	}
	

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "tasks.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title,  null, null, null);	
					
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);			
		} 
	}
	
	/**
	 * за експорт в pdf 
	 * @return
	 */
	public PDFOptions pdfOptions() {
		PDFOptions pdfOpt = new CustomExpPreProcess().pdfOptions(null, null, null);
        return pdfOpt;
	}
	
	
	/**
	 * Препраща към екрана за актуализация на задача
	 * @param i
	 * @param idObj
	 * @param idDoc
	 * @param rnDoc
	 * @param docDate
	 * @param registratura
	 * @return
	 */
	public String actionGoto(int i, Integer idObj, Integer idDoc,   Integer registratura, String nomTask) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("?faces-redirect=true&idObj=");
		sb.append(idObj + (idDoc != null ? "&idDoc=" + idDoc : ""));
		sb.append(registratura != null ? "&r=" + registratura: "");
		sb.append(nomTask != null ? "&nt=" + nomTask: "");
		
		if(idDoc != null) {
			// Проверка за достъп до документа, защото междувременно дотъспът на потребителя може да е отнет, независмо, че има задача!
			try {
				if(!new DocDAO(getUserData()).hasDocAccessDashboard(idDoc)) {
					sb.append("&accDoc=0");
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,getMessageResourceString(beanMessages, "dashboard.noDocAccess"));
				}
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при проверка за достъп до документа! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,ERRDATABASEMSG), e.getMessage());
			} finally {
				 JPA.getUtil().closeConnection();
			}
		}
		
		String result = sb.toString();
	
		if(result != null) {
			if(i == 0) {
				result = "taskEdit.jsf" + result;
			} else if(i == 1){
				result = "taskView.xhtml"+ result;
			}
		}
	
		return result;
	}
	
	
	
	
	/*
	 * Множествен избор на задачи
	 */
	/**
	 * Избира всички редове от текущата страница
	 * @param event
	 */
	  public void onRowSelectAll(ToggleSelectEvent event) {    
    	List<Object[]> tmpL = new ArrayList<>();
    	tmpL.addAll(getTaskSelectedAllM());
    	if(event.isSelected()) {
    		for (Object[] obj : getTaskSelectedTmp()) {
    			if(obj != null && obj.length > 0) {
	    			boolean bb = true;
	    			Long l2 = Long.valueOf(obj[0].toString());
	    			for (Object[] j : tmpL) { 
	        			Long l1 = Long.valueOf(j[0].toString());        			
	    	    		if(l1.equals(l2)) {    	    			
	    	    			bb = false;
	    	    			break;
	    	    		}
	        		}
	    			if(bb) {
	    				tmpL.add(obj);
	    			}
    			}
    		}    		
    	}else {
	    	List<Object[]> tmpLPageC =  tasksListResult.getResult();// rows from current page....    		
			for (Object[] obj : tmpLPageC) {
				if(obj != null && obj.length > 0) {
					Long l2 = Long.valueOf(obj[0].toString());
					for (Object[] j : tmpL) { 
		    			Long l1 = Long.valueOf(j[0].toString());        			
			    		if(l1.equals(l2)) {    	    			
			    			tmpL.remove(j);
			    			break;
			    		}	
		    		}
				}
			}    		
		}
		setTaskSelectedAllM(tmpL);	    	
	}
		    
    /** 
     * Select one row
     * @param event
     */
    public void onRowSelect(SelectEvent<?> event) {    	
    	if(event!=null  && event.getObject()!=null) {
    		List<Object[]> tmpList =  getTaskSelectedAllM();
    		
    		Object[] obj = (Object[]) event.getObject();
    		if(obj != null && obj.length > 0) {
    			boolean bb = true;
	    		Integer l2 = Integer.valueOf(obj[0].toString());
				for (Object[] j : tmpList) { 
					Integer l1 = Integer.valueOf(j[0].toString());        			
		    		if(l1.equals(l2)) {
		    			bb = false;
		    			break;
		    		}
		   		}
				if(bb) {
					tmpList.add(obj);
					setTaskSelectedAllM(tmpList);   
				}
    		}
    	}	    	
    }
		 
		    
    /**
     * unselect one row
     * @param event
     */
    public void onRowUnselect(UnselectEvent<?> event) {
    	if(event!=null  && event.getObject()!=null) {
    		Object[] obj = (Object[]) event.getObject();
    		List<Object[] > tmpL = new ArrayList<>();
    		tmpL.addAll(getTaskSelectedAllM());
    		for (Object[] j : tmpL) {
    			if(j != null && j.length > 0 
        			&& obj != null && obj.length > 0) {
	    			Integer l1 = Integer.valueOf(j[0].toString());
	    			Integer l2 = Integer.valueOf(obj[0].toString());
		    		if(l1.equals(l2)) {
		    			tmpL.remove(j);
		    			setTaskSelectedAllM(tmpL);
		    			break;
		    		}
    			}
    		}
    	}
    }

    /**
     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга
     */
    public void   onPageUpdateSelected(){
    	if (getTaskSelectedAllM() != null && !getTaskSelectedAllM().isEmpty()) {
    		getTaskSelectedTmp().clear();
    		getTaskSelectedTmp().addAll(getTaskSelectedAllM());
    	}	    	
    }
	
	public void showModalSnemane() {
		dateGrupovoSnemane = null;
		textGrupovoSnemane = null;
		
	}
	
	/**
	 * Групово снемане на задачи 
	 */
    public void grupovoSnemaneTasks() {
    	if(dateGrupovoSnemane == null) {
    		JSFUtils.addMessage("tasksListForm:dateGroupSnemane", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "tasks.dateSnemane")) );
    		PrimeFaces.current().ajax().addCallbackParam(VALIDATTRIBS, false);
    		return;
    	}
    	int count = 0;
    	for (int i = 0; i < taskSelectedAllM.size(); i++) {
			try {
				Object[] tmp = taskSelectedAllM.get(i);
				task = tDao.findById(Integer.valueOf(tmp[0].toString()));
				if(task.getAssignDate() != null && ( dateGrupovoSnemane.getTime() < task.getAssignDate().getTime())) {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, " Задача: " + task.getRnTask() + " не може да бъде приключена групово, защото датата на приключване е по-голяма от датата на възлагане!");
		    		continue;
				}
				task.setStatus(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_SNETA);
				task.setRealEnd(dateGrupovoSnemane);
				task.setStatusComments(textGrupovoSnemane);
				JPA.getUtil().runInTransaction(() ->  task = tDao.save(task, null, (SystemData) getSystemData()));
			} catch (BaseException  e) {
				count += 1;
				if (e.getCause() != null) { // системна грешка от записа
					LOGGER.error("Грешка при групово снемане на задачи!", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
					
				} else { // нещо което трябва да се каже на потребителя
					LOGGER.error("Групово снемане на задачи -{}", e.getMessage());
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, e.getMessage());
				}
			}
		}
    	if(count < taskSelectedAllM.size()) {
        	JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,IndexUIbean.getMessageResourceString(UIBEANMESSAGES, SUCCESSAVEMSG));
    	}
    	PrimeFaces.current().ajax().addCallbackParam(VALIDATTRIBS, true);
    }
    
    public void showModalIzpSmqna() {
    	listExecsGrSmqna = new ArrayList<>();
	}
	
    /**
     * Групова смяна на изпълнители на задачи
     */
    public void grupovoIzpSmqnaTasks() {
    	if(listExecsGrSmqna == null || listExecsGrSmqna.isEmpty()) {
    		JSFUtils.addMessage("tasksListForm:lstIzpsGrSmqna", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "tasks.exec")) );
    		PrimeFaces.current().ajax().addCallbackParam(VALIDATTRIBS, false);
    		return;
    	}
    	int count = 0;
		for (int i = 0; i < taskSelectedAllM.size(); i++) {
			try {
				Object[] tmp = taskSelectedAllM.get(i);
				task = tDao.findById(Integer.valueOf(tmp[0].toString()));
				task.getCodeExecs().clear();
				task.getCodeExecs().addAll(listExecsGrSmqna);
				JPA.getUtil().runInTransaction(() ->  task = tDao.save(task, null, (SystemData)getSystemData()));
			} catch (BaseException e) {
				count += 1;
				if (e.getCause() != null) { // системна грешка от записа
					LOGGER.error("Грешка при групова смяна на изпълнители на задачи!", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());

				} else { // нещо което трябва да се каже на потребителя
					LOGGER.error("Групова смяна на изпълнители на задачи -{}", e.getMessage());
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, e.getMessage());
				}
			} 
    	}
		if(count < taskSelectedAllM.size()) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,IndexUIbean.getMessageResourceString(UIBEANMESSAGES, SUCCESSAVEMSG));
		}
		search();
		PrimeFaces.current().ajax().addCallbackParam(VALIDATTRIBS, true);
    }
    
    /**
     * Групово изтриване на задачи
     */
    public void grupovoIztrivaneTasks() {
    	StringBuilder msg = new StringBuilder();
    	
    	for (int i = 0; i < taskSelectedAllM.size(); i++) {
			try {
				Object[] tmp = taskSelectedAllM.get(i);
				
				JPA.getUtil().runInTransaction(() -> {
					task = tDao.findById(Integer.valueOf(tmp[0].toString()));
					tDao.delete(task);
				});
				
				tDao.notifTaskDelete(task, null, (SystemData) getSystemData());
				
				if (msg.length() > 0) {
					msg.append(", ");
				}
				msg.append(tmp[1]);

			} catch (ObjectInUseException e) {
				LOGGER.error("Групово изтриване на задачи -{}", e.getMessage());
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} catch (BaseException e) {
				LOGGER.error("Грешка при групово изтриване на задачи от базата данни!", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
			} 
		}

    	if (msg.length() > 0) {
    		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
    			IndexUIbean.getMessageResourceString(UIBEANMESSAGES, "general.successDeleteMsg"), "Задачи: " + msg + ".");
    	}

    	
    	search();
    	taskSelectedAllM = new ArrayList<>();
		taskSelectedAllM = new ArrayList<>();
    }
    
    public void showModalIzpTasksModal() {
    	dateGrupovoSnemane = null;
		textGrupovoSnemane = null;
    	if(taskSelectedAllM.size() > 1) {
    		showOpinionList = false;
    		return;
    	}
		try {
			if(taskSelectedAllM.isEmpty())
				return;
			Object[] tmp = taskSelectedAllM.get(0);
	    	List<SystemClassif> tmpOpinionLst = getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_TASK_TYPE_TASK_OPINION, Integer.valueOf(tmp[2].toString()), getCurrentLang(),  new Date());
    		List<SelectItem> items = new ArrayList<>(tmpOpinionLst.size());
			for (SystemClassif x : tmpOpinionLst) {
				items.add(new SelectItem(x.getCode(), x.getTekst()));		
				showOpinionList = true;
			}		
			setOpinionLst(items);
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при четене на задача от базата данни!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
		} 
	}
    
    /**
     * Групово изпълнение на задачи
     */
    public void grupovoIzpTasks() {
    	boolean willSave = true;
    	if(dateGrupovoSnemane == null) {
    		JSFUtils.addMessage("tasksListForm:dateGroupIzpulnenie", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "tasks.dateExec")) );
    		willSave = false;
    		PrimeFaces.current().ajax().addCallbackParam(VALIDATTRIBS, willSave);
    		return;
    	}
    		
    	int count = 0;
    	for (int i = 0; i < taskSelectedAllM.size(); i++) {
			try {
				Object[] tmp = taskSelectedAllM.get(i);
		    	List<SystemClassif> tmpOpinionLst = getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_TASK_TYPE_TASK_OPINION, Integer.valueOf(tmp[2].toString()), getCurrentLang(),  new Date());
		    	if(tmpOpinionLst == null || tmpOpinionLst.isEmpty()) {
					task = tDao.findById(Integer.valueOf(tmp[0].toString()));
					if(task.getAssignDate() != null && ( DateUtils.endDate(dateGrupovoSnemane).getTime() < task.getAssignDate().getTime())) {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, " Задача: " + task.getRnTask() + " не може да бъде изпълнена групово, защото датата на приключване е по-голяма от датата на възлагане!");
			    		continue;
					}
					task.setStatus(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP);
					task.setRealEnd(dateGrupovoSnemane);
					task.setStatusComments(textGrupovoSnemane);
					if(showOpinionList)
						task.setEndOpinion(endOpinion);
					JPA.getUtil().runInTransaction(() ->  task = tDao.save(task, null, (SystemData) getSystemData()));
		    	}else {
		    		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, " Задача с номер: " + tmp[1].toString() + " не може да бъде изпълнена групово, защото за нея се изисква мнение при приключване! Моля, отворете задачата, за да я приключите!");
		    		count += 1;
		    	}
			} catch (BaseException e) {
				count += 1;
				if (e.getCause() != null) { // системна грешка от записа
					LOGGER.error("Грешка при групово изпълнение на задачи!", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
				
				} else { // нещо което трябва да се каже на потребителя
					LOGGER.error("Групово изпълнение на задачи -{}", e.getMessage());
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, e.getMessage());
				}
			} 
		}
    	if(count < taskSelectedAllM.size())
    		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,IndexUIbean.getMessageResourceString(UIBEANMESSAGES, SUCCESSAVEMSG));
    	PrimeFaces.current().ajax().addCallbackParam(VALIDATTRIBS, willSave);
    }
    
	public void changeDateDoc() { 
		this.setPeriodDoc(null);
	}
	
	public void changeDate() { 
		this.setPeriodR(null);
	}
	
	public void changeDateV() { 
		this.setPeriodV(null);
	}
	
	public void changeDateZ() { 
		this.setPeriodZ(null);
	}

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public List<SelectItem> getVidTask() {
		return vidTask;
	}

	public void setVidTask(List<SelectItem> vidTask) {
		this.vidTask = vidTask;
	}

	public List<SelectItem> getStatusTaskList() {
		return statusTaskList;
	}

	public void setStatusTaskList(List<SelectItem> statusTaskList) {
		this.statusTaskList = statusTaskList;
	}

	public Integer getPeriodR() {
		return periodR;
	}

	public void setPeriodR(Integer periodR) {
		this.periodR = periodR;
	}

	public Integer getPeriodV() {
		return periodV;
	}

	public void setPeriodV(Integer periodV) {
		this.periodV = periodV;
	}


	public Integer getPeriodZ() {
		return periodZ;
	}

	public void setPeriodZ(Integer periodZ) {
		this.periodZ = periodZ;
	}

	public Integer getSlujitel() {
		return slujitel;
	}

	public void setSlujitel(Integer slujitel) {
		this.slujitel = slujitel;
	}

	public List<SelectItem> getVidSlujList() {
		return vidSlujList;
	}

	public void setVidSlujList(List<SelectItem> vidSlujList) {
		this.vidSlujList = vidSlujList;
	}

	
	public TaskSearch getTaskSearch() {
		return taskSearch;
	}

	public void setTaskSearch(TaskSearch taskSearch) {
		this.taskSearch = taskSearch;
	}

	public Integer[] getSelectedVidTask() {
		return selectedVidTask;
	}

	public void setSelectedVidTask(Integer[] selectedVidTask) {
		this.selectedVidTask = selectedVidTask;
	}

	public Integer[] getSelectedStatusTask() {
		return selectedStatusTask;
	}

	public void setSelectedStatusTask(Integer[] selectedStatusTask) {
		this.selectedStatusTask = selectedStatusTask;
	}

	public Integer[] getSelectedVidSluj() {
		return selectedVidSluj;
	}

	public void setSelectedVidSluj(Integer[] selectedVidSluj) {
		this.selectedVidSluj = selectedVidSluj;
	}

	public List<SystemClassif> getScList() {
		return scList;
	}

	public void setScList(List<SystemClassif> scList) {
		this.scList = scList;
	}

	public Integer getPeriodDoc() {
		return periodDoc;
	}

	public void setPeriodDoc(Integer periodDoc) {
		this.periodDoc = periodDoc;
	}

	public List<SelectItem> getRegistraturaList() {
		return registraturaList;
	}

	public void setRegistraturaList(List<SelectItem> registraturaList) {
		this.registraturaList = registraturaList;
	}

	public boolean isShowRegistaturiList() {
		return showRegistaturiList;
	}

	public void setShowRegistaturiList(boolean showRegistaturiList) {
		this.showRegistaturiList = showRegistaturiList;
	}

	public List<Object[]> getTaskSelectedTmp() {
		return taskSelectedTmp;
	}

	public void setTaskSelectedTmp(List<Object[]> taskSelectedTmp) {
		this.taskSelectedTmp = taskSelectedTmp;
	}

	public List<Object[]> getTaskSelectedAllM() {
		return taskSelectedAllM;
	}

	public void setTaskSelectedAllM(List<Object[]> taskSelectedAllM) {
		this.taskSelectedAllM = taskSelectedAllM;
	}

	public Date getDateGrupovoSnemane() {
		return dateGrupovoSnemane;
	}

	public void setDateGrupovoSnemane(Date dateGrupovoSnemane) {
		this.dateGrupovoSnemane = dateGrupovoSnemane;
	}

	public String getTextGrupovoSnemane() {
		return textGrupovoSnemane;
	}

	public void setTextGrupovoSnemane(String textGrupovoSnemane) {
		this.textGrupovoSnemane = textGrupovoSnemane;
	}

	public List<Integer> getListExecsGrSmqna() {
		return listExecsGrSmqna;
	}

	public void setListExecsGrSmqna(List<Integer> listExecsGrSmqna) {
		this.listExecsGrSmqna = listExecsGrSmqna;
	}

	public List<SystemClassif> getScListGrSmqna() {
		return scListGrSmqna;
	}

	public void setScListGrSmqna(List<SystemClassif> scListGrSmqna) {
		this.scListGrSmqna = scListGrSmqna;
	}

	public Integer getEndOpinion() {
		return endOpinion;
	}

	public void setEndOpinion(Integer endOpinion) {
		this.endOpinion = endOpinion;
	}

	public List<SelectItem> getOpinionLst() {
		return opinionLst;
	}

	public void setOpinionLst(List<SelectItem> opinionLst) {
		this.opinionLst = opinionLst;
	}

	public String getTextGrupovoIzpalnenie() {
		return textGrupovoIzpalnenie;
	}

	public void setTextGrupovoIzpalnenie(String textGrupovoIzpalnenie) {
		this.textGrupovoIzpalnenie = textGrupovoIzpalnenie;
	}

	public boolean isShowOpinionList() {
		return showOpinionList;
	}

	public void setShowOpinionList(boolean showOpinionList) {
		this.showOpinionList = showOpinionList;
	}

	public boolean isHasAccessTaskRegis() {
		return hasAccessTaskRegis;
	}

	public void setHasAccessTaskRegis(boolean hasAccessTaskRegis) {
		this.hasAccessTaskRegis = hasAccessTaskRegis;
	}

	public int getRegistraturaRadioBtn() {
		return registraturaRadioBtn;
	}

	public void setRegistraturaRadioBtn(int registraturaRadioBtn) {
		this.registraturaRadioBtn = registraturaRadioBtn;
	}

	public boolean isShowGrupoviDeinosti() {
		return showGrupoviDeinosti;
	}

	public void setShowGrupoviDeinosti(boolean showGrupoviDeinosti) {
		this.showGrupoviDeinosti = showGrupoviDeinosti;
	}

	public boolean isShowGroupDeleteBtn() {
		return showGroupDeleteBtn;
	}

	public void setShowGroupDeleteBtn(boolean showGroupDeleteBtn) {
		this.showGroupDeleteBtn = showGroupDeleteBtn;
	}

	public List<SystemClassif> getScList2() {
		return scList2;
	}

	public void setScList2(List<SystemClassif> scList2) {
		this.scList2 = scList2;
	}

	public List<SystemClassif> getScList3() {
		return scList3;
	}

	public void setScList3(List<SystemClassif> scList3) {
		this.scList3 = scList3;
	}

	public List<SelectItem> getEndOpinionTaskList() {
		return endOpinionTaskList;
	}

	public void setEndOpinionTaskList(List<SelectItem> endOpinionTaskList) {
		this.endOpinionTaskList = endOpinionTaskList;
	}

	public Integer[] getSelectedEndOpinionTask() {
		return selectedEndOpinionTask;
	}

	public void setSelectedEndOpinionTask(Integer[] selectedEndOpinionTask) {
		this.selectedEndOpinionTask = selectedEndOpinionTask;
	}

	public String getSrokPattern() {
		return srokPattern;
	}

	public void setSrokPattern(String srokPattern) {
		this.srokPattern = srokPattern;
	}
	public boolean getIsOkToRender() {
		return isOkToRender;
	}

	public void setIsOkToRender(boolean isOkToRender) {
		this.isOkToRender = isOkToRender;
	}

	public boolean isFillTextSearch() {
		return fillTextSearch;
	}

	public void setFillTextSearch(boolean fillTextSearch) {
		this.fillTextSearch = fillTextSearch;
	}
	
	public void actionSearchNew() {
		if(fillTextSearch) {
			actionSearchFullText();
		} else {
			search();
		}
	}

	public LazyDataModelSQL2Array getTasksListResult() {
		return tasksListResult;
	}

	public void setTasksListResult(LazyDataModelSQL2Array tasksListResult) {
		this.tasksListResult = tasksListResult;
	}
	
	public String titleLock(Integer from, Date dateL) {
		StringBuilder sb = new StringBuilder();
		if(from != null && dateL != null) {
			sb.append(getMessageResourceString(LABELS, "docu.taskLocked")+" ");		
			try {
				sb.append(getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, from , getCurrentLang(), new Date()));
				sb.append(" / "+ DateUtils.printDateFull(dateL));
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при извличане на инфромация за заключена задача! ", e);
			}
		}
		return sb.toString(); 
	}

	public boolean isShowTasksMySubordinates() {
		return showTasksMySubordinates;
	}

	public void setShowTasksMySubordinates(boolean showTasksMySubordinates) {
		this.showTasksMySubordinates = showTasksMySubordinates;
	}

	public List<SystemClassif> getSelectedEmplStatus() {
		return selectedEmplStatus;
	}

	public void setSelectedEmplStatus(List<SystemClassif> selectedEmplStatus) {
		this.selectedEmplStatus = selectedEmplStatus;
	}

}

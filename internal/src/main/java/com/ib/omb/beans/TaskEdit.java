package com.ib.omb.beans;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.components.TaskData;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.LockObjectDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.search.DocSearch;
import com.ib.omb.search.TaskSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;


@Named
@ViewScoped
public class TaskEdit  extends IndexUIbean{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7022253329017403534L;
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskEdit.class);
	private UserData ud;
	private String rnFullDoc;
	private Integer idTask;
	private Integer idDoc;
	private LazyDataModelSQL2Array tasksList;
	private Integer registraturaId;
	private Object[] docObj;
	private String rnDoc;
	private Date dateDoc;
	private int searchFlag = 0; // за да предоврати минаването през searchDoc два пъти, ако след въвеждане на номер на док. веднага се натисне лупата
	private boolean taskWithDoc = false;
	private Doc document;
	private String nomTask;
	private String srokPattern;
	
	private Integer accDoc;
	
	@PostConstruct
	public void init() {	
		accDoc = null; 
		ud = getUserData(UserData.class);
		setRegistraturaId(getUserData(UserData.class).getRegistratura());	
		String param = JSFUtils.getRequestParameter("accDoc");
		if(!SearchUtils.isEmpty(param)) {
			accDoc = 0;  // ако е подаден този параметър => достъпът до документа, към който е задачата е отнет и затова не трябва да се отваря и самата задача!
		} else {
			param = JSFUtils.getRequestParameter("idObj");	
			if (!SearchUtils.isEmpty(param)){
				idTask = Integer.valueOf(param);
				String idD = JSFUtils.getRequestParameter("idDoc");	
				if (!SearchUtils.isEmpty(idD)){				
					this.idDoc = Integer.valueOf(idD);
					
					loadRnDoc();
					
					loadTaskList();// списък задачи към документ, ако има такъв
				}else {
					this.idDoc = null;
					setTasksList(null);
				}
				nomTask = JSFUtils.getRequestParameter("nt"); // номер на задача - да се изпозлва като етикет на табчето	
			}
			loadSrokPattern();
		}
	}
	
	/**
	 * зарежда данни за документа
	 */
	private void loadRnDoc() {
		try {
			JPA.getUtil().runWithClose(() -> {
				document = new DocDAO(getUserData()).findById(idDoc);	
			});
			if(document != null) {
				rnFullDoc = DocDAO.formRnDocDate(this.document.getRnDoc(), this.document.getDocDate(), this.document.getPoredDelo());
				this.dateDoc =  this.document.getDocDate();
				setRegistraturaId(document.getRegistraturaId());
			}
		} catch (BaseException e) {
			LOGGER.error("Грешка при отключване на задача! ", e);
		}
	}
	
	
	/**
	 * при излизане от страницата - отключва обекта и да го освобождава за актуализация от друг потребител
	 * Заключването на задача се извършва в комп.taskData
	 */
	@PreDestroy
	public void unlockTask(){		
        if (!ud.isReloadPage()) {
    		LockObjectDAO daoL = new LockObjectDAO();		
    		try { 
			
				JPA.getUtil().runInTransaction(() -> daoL.unlock(ud.getUserId()) );
    			 
    		} catch (BaseException e) {
    			LOGGER.error("Грешка при отключване на задача! ", e);
    		}
        	ud.setPreviousPage(null);
        }          
        ud.setReloadPage(false);
	}

	/**
	 * 	 списък задачи към документ
	 */
	private void loadTaskList() {
		try {
			TaskSearch tmpTs = new TaskSearch(getRegistraturaId()); // това трябва да е регистртурата на документа!!!
			tmpTs.setDocId(this.idDoc);			
			tmpTs.buildQueryTasksInDoc();
			setTasksList(new LazyDataModelSQL2Array(tmpTs, "a0 asc"));
		}catch (Exception e) {
			LOGGER.error("Грешка при зареждане списък задачи - актуализаиция на  задача! ", e);
		}
	}
	
	/**
    * Избор на задача от списъка - ид на зад. да се подаде на комп. taskData
    */
    public void actionSelectTask(Object[] row) {	  	 
		idTask = Integer.valueOf(row[0].toString());	    
    }
	  
    /**
     * Рефрешва списъка със задачи, при запис и изтриване от компонентата
     */
    public void actionRefreshTaskList() {
       if(getTasksList() != null) {
    	   getTasksList().loadCountSql();
       }
    }
    
    
	/** 
	 * избор на документ
	 * Търси документ, към който да се закачи задачата
	 */
	public void actionSearchDocBtn(boolean bb) {
		if (SearchUtils.isEmpty(rnDoc)) {
			setDateDoc(null);
			setIdDoc(null);
			setDocObj(null);			
		}
		if(searchFlag == 0){
			searchDocRn(rnDoc, bb);			
		}else {
			searchFlag = 0;
		}
	}

	
	/**
	 * избор на документ
	 * ръчно въвеждане на рег. номер на документ
	 */
	private Object[] searchDocRn(String rnDoc, boolean bb) {
		Object[] sDoc = null;

		if (SearchUtils.isEmpty(rnDoc)) { 
			setRnDoc("");	
		}else if (!bb){
			bb = true;
		}

		if (bb) {
			DocSearch tmp = new DocSearch(getUserData(UserData.class).getRegistratura());
			tmp.setRnDoc(rnDoc);
			
			tmp.buildQueryComp(getUserData());
	
			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(tmp, "a1 desc");
			int res = lazy.getRowCount();
			
			if (res == 0 ) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Не е намерен документ с посочения номер!");
				}
				setDateDoc(null);
				setIdDoc(null);
				setDocObj(null);
				searchFlag = 1;
				setTasksList(null);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(beanMessages, "doc.srchDocDocErrT"));
			}else if (res == 1) {
				List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);
				sDoc = result.get(0);
				setIdDoc(SearchUtils.asInteger(sDoc[0]));
				setDateDoc(SearchUtils.asDate(sDoc[4]));
				setRnDoc(SearchUtils.asString(sDoc[1]));
				rnFullDoc = SearchUtils.asString(sDoc[1]) + "/" + DateUtils.printDate(SearchUtils.asDate(sDoc[4]));
				
				String dialogWidgetVar = "PF('confirmWV').show();";
				PrimeFaces.current().executeScript(dialogWidgetVar);
				searchFlag++;
			}else {
				sDoc = new Object[5];
				String dialogWidgetVar = "PF('mDoc').show();";
				PrimeFaces.current().executeScript(dialogWidgetVar);
			}
		}
		return sDoc;
	}



	/**
	 * избор на документ
	 * Затваряне на модалния за избор на документ
	 */
   public void actionHideModal() {
	 
	   if(docObj != null && docObj[0] != null) {
		   // да заредя полетата
		    idDoc = Integer.valueOf(docObj[0].toString()); // id_to na DOC object
			dateDoc = SearchUtils.asDate(docObj[4]);
			rnDoc = SearchUtils.asString(docObj[1]);
			rnFullDoc = SearchUtils.asString(docObj[1]) + "/" + DateUtils.printDate(SearchUtils.asDate(docObj[4]));
		    
		    initTaskWithDoc();		    
	   }
	   setDocObj(null); 
	   searchFlag = 0;
	  
   }
   
	/**
	 * избор на документ
	 * потвърждава избора на документ при ръчно писане на номера
	 */
	public void actionConfirmDoc() {

		try {
			setDocObj(null);
			searchFlag = 0;
			initTaskWithDoc();
			
		} catch (Exception e) {
			LOGGER.error("Грешка при избор на документ!", e);
		}
	}
	
	private void initTaskWithDoc() {
		loadTaskList();
		idTask = null;
		((TaskData)FacesContext.getCurrentInstance().getViewRoot().findComponent("taskEditForm").findComponent("compTaskId")).initTaskComp();
		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
				"Новата задача ще бъде към документ: "+rnFullDoc);
	}

	/**
	 * избор на документ
	 * нулира стойностите след избор и отказ 
	 */
	public void cancel(boolean flag) {		
		if(flag) {
			setRnDoc(null);
			setDateDoc(null);
			setIdDoc(null);
		}
		setIdTask(null);
		setDocObj(null);
		searchFlag=0;
	}
	
	/**
	 * избор на документ
	 * задача към документ / задача без документ
	 */
	public void actionChangeWithDoc() {
		setRnDoc(null);
		setDateDoc(null);
		setIdDoc(null);
		setIdTask(null);
		setDocObj(null);
		setTasksList(null);
		if(!taskWithDoc) {
			((TaskData)FacesContext.getCurrentInstance().getViewRoot().findComponent("taskEditForm").findComponent("compTaskId")).initTaskComp();
		}
	}

	
	
	/**
	 * разглеждане на документа, към който е задачата
	 * @param i
	 * @return
	 */
	public String actionGotoViewDoc() {
		return "docView.xhtml?faces-redirect=true&idObj=" + idDoc;
	}
	
	   
	/**
	 * подскзака в списъка със задачи - мнение при приключване + коментар
	 * @param comment
	 * @param opinion
	 * @return
	 */
	public String titleInfo(Integer opinion, String comment ) {
		StringBuilder title = new StringBuilder();
		if(opinion != null) {
			try {
				String opinionTxt=getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_TASK_OPINION, opinion, getUserData().getCurrentLang(), new Date());
				title.append(getMessageResourceString(LABELS, "docu.modalRefMnenie")+": " + opinionTxt+ "; ");
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане на данни за документ! ", e);
			}
		}
		if(!SearchUtils.isEmpty(comment)) {
			title.append(comment);
		}
		return title.toString();
	}
	
   /**
    * Да се виждат ли часове и минути в срока на задачите 
    */
   private void loadSrokPattern() {
	   	// да се виждат ли часове и минути в срока на задачата
		// взема се настройкaт на регистратурата на потребителя - за сега, в списъка,  ще се определя само от текущата регистртура!
		try {
			Integer s1 = ((SystemData) getSystemData()).getRegistraturaSetting(getUserData(UserData.class).getRegistratura(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_15);
			if(Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_DA)) {
				setSrokPattern("dd.MM.yyyy HH:mm");
			}else {
				setSrokPattern("dd.MM.yyyy");
			}
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при извличане на настройки на регистртура (време в срок на задача)! ", e);
		}
   }
	

	public String getRnFullDoc() {
		return rnFullDoc;
	}



	public void setRnFullDoc(String rnFullDoc) {
		this.rnFullDoc = rnFullDoc;
	}



	public Integer getIdTask() {
		return idTask;
	}



	public void setIdTask(Integer idTask) {
		this.idTask = idTask;
	}



	public Integer getIdDoc() {
		return idDoc;
	}



	public void setIdDoc(Integer idDoc) {
		this.idDoc = idDoc;
	}


	public LazyDataModelSQL2Array getTasksList() {
		return tasksList;
	}



	public void setTasksList(LazyDataModelSQL2Array tasksList) {
		this.tasksList = tasksList;
	}
	
	public UserData getUd() {
		return ud;
	}


	public void setUd(UserData ud) {
		this.ud = ud;
	}



	public Integer getRegistraturaId() {
		return registraturaId;
	}



	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}



	public Object[] getDocObj() {
		return docObj;
	}



	public void setDocObj(Object[] docObj) {
		this.docObj = docObj;
	}



	public Date getDateDoc() {
		return dateDoc;
	}



	public void setDateDoc(Date dateDoc) {
		this.dateDoc = dateDoc;
	}



	public int getSearchFlag() {
		return searchFlag;
	}



	public void setSearchFlag(int searchFlag) {
		this.searchFlag = searchFlag;
	}



	public String getRnDoc() {
		return rnDoc;
	}



	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}



	public boolean isTaskWithDoc() {
		return taskWithDoc;
	}



	public void setTaskWithDoc(boolean taskWithDoc) {
		this.taskWithDoc = taskWithDoc;
	}

	public String getNomTask() {
		return nomTask;
	}

	public void setNomTask(String nomTask) {
		this.nomTask = nomTask;
	}

	public String getSrokPattern() {
		return srokPattern;
	}

	public void setSrokPattern(String srokPattern) {
		this.srokPattern = srokPattern;
	}

	public Integer getAccDoc() {
		return accDoc;
	}

	public void setAccDoc(Integer accDoc) {
		this.accDoc = accDoc;
	}
	
}

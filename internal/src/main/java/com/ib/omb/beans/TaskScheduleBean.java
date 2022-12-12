package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.TaskScheduleDAO;
import com.ib.omb.db.dto.TaskSchedule;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.X;

@Named
@ViewScoped
public class TaskScheduleBean extends IndexUIbean  implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7767572564756526112L;
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskScheduleBean.class);
	
	private Date decodeDate = new Date();
	private transient TaskScheduleDAO taskDao;
	private TaskSchedule task;
	private boolean valid;
	private boolean workDays;
	//private boolean wihtoutTerm;
	private boolean docFinish;
	private List<SelectItem> daysWeek;
	private List<SelectItem> months;
	private Map<Integer, Object> specificsAdm;
	private Map<Integer, Object> specificsAdmZveno;
	
	private List<Date> datesRegList = new ArrayList<>();//дати на следващите регистрации
	private int n = 10; // брой на датите на следващите регистрации
	
	private String rnDoc;
	private Object[] docObj;
	private int searchFlag = 0;
	private String rnFullDoc;
	private Integer registraturaId;
	
	private Date dateBegOld; //за да знам дали при редакция са променили началната дата и да проверя дали новата,която са въвели не е по-малка от днешна дата
	
	@PostConstruct
	void initData() {
		
		
		LOGGER.debug("PostConstruct!!!");	
		taskDao = new TaskScheduleDAO(getUserData());
		setRegistraturaId(getUserData(UserData.class).getRegistratura());	
		specificsAdm = Collections.singletonMap(OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE
				, X.of(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL));
		specificsAdmZveno = Collections.singletonMap(OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE
				, X.of(OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO)); 
		
		daysWeek = new ArrayList<>();
		SelectItem item = new SelectItem(1, "Понеделник");
		daysWeek.add(item);	
		item = new SelectItem(2, "Вторник");
		daysWeek.add(item);	
		item = new SelectItem(3, "Сряда");
		daysWeek.add(item);	
		item = new SelectItem(4, "Четвъртък");
		daysWeek.add(item);	
		item = new SelectItem(5, "Петък");
		daysWeek.add(item);	
		item = new SelectItem(6, "Събота");
		daysWeek.add(item);	
		item = new SelectItem(7, "Неделя");
		daysWeek.add(item);	
		
		months = new ArrayList<>();
		SelectItem month = new SelectItem(1, "Януари");
		months.add(month);
		month = new SelectItem(2, "Фервруари");
		months.add(month);
		month = new SelectItem(3, "Март");
		months.add(month);
		month = new SelectItem(4, "Април");
		months.add(month);
		month = new SelectItem(5, "Май");
		months.add(month);
		month = new SelectItem(6, "Юни");
		months.add(month);
		month = new SelectItem(7, "Юли");
		months.add(month);
		month = new SelectItem(8, "Август");
		months.add(month);
		month = new SelectItem(9, "Септември");
		months.add(month);
		month = new SelectItem(10, "Октомври");
		months.add(month);
		month = new SelectItem(11, "Ноември");
		months.add(month);
		month = new SelectItem(12, "Декември");
		months.add(month);
		
		
		String param = JSFUtils.getRequestParameter("idObj");
		if ( SearchUtils.isEmpty(param)){
			actionNew();
		}else {
			try {
				task = taskDao.findById(Integer.valueOf(param));
				dateBegOld = task.getValidFrom();
				if(task.getValid()==null || task.getValid().intValue() == OmbConstants.CODE_ZNACHENIE_NE) {
					valid =false;
				}else {
					valid = true;
				}
				
				if(task.getWorkDaysOnly()==null || task.getWorkDaysOnly().intValue() == OmbConstants.CODE_ZNACHENIE_NE) {
					workDays =false;
				}else {
					workDays = true;
				}
				
				if(task.getDocRequired()==null || task.getDocRequired().intValue() == OmbConstants.CODE_ZNACHENIE_NE) {
					docFinish =false;
				}else {
					docFinish = true;
				}
				
				if(task.getDocId()!=null) {
					docObj = new DocDAO(getUserData()).findDocData(task.getDocId());
					if(docObj!=null) {
						rnDoc = SearchUtils.asString(docObj[1]);
					}
				}
				
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при извличане на периодична задача! ", e);
				 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}					
	}
	
	public void actionNew() {
		task = new TaskSchedule();
		task.setTaskType(OmbConstants.CODE_ZNACHENIE_TASK_TYPE_DEFAULT);
		actNew();
	}
	
	private void actNew() {
		valid = true;
		workDays = true;
		docFinish = false;
		rnDoc = "";
		rnFullDoc = "";
		
		
				
	}
	
	private boolean checkData() {
		boolean flagSave = false;
		
		if( task.getValidFrom()==null ) {
			JSFUtils.addMessage("taskForm:dateOtValid", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "taskSchedule.validFromDef")));
			flagSave = true;
		}else if((task.getId()==null && task.getValidFrom().before(decodeDate)) || (task.getId() !=null && !task.getValidFrom().equals(dateBegOld) && task.getValidFrom().before(decodeDate))) {
			JSFUtils.addMessage("taskForm:dateOtValid", FacesMessage.SEVERITY_ERROR,getMessageResourceString( beanMessages, "taskSchedule.dateFrom"));			
			flagSave = true;
		}
		
		if( task.getValidTo()==null ) {
			JSFUtils.addMessage("taskForm:dateToValid", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "taskSchedule.validToDef")));
			flagSave = true;
		}else if(task.getValidTo().before(task.getValidFrom()) || task.getValidTo().equals(task.getValidFrom())) {
			JSFUtils.addMessage("taskForm:dateToValid", FacesMessage.SEVERITY_ERROR,getMessageResourceString( beanMessages, "taskSchedule.dateTo"));			
			flagSave = true;
		}
		
		if(task.getRegPeriod()==null) {
			JSFUtils.addMessage("taskForm:regPeriod", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "taskSchedule.regPeriod")));
			flagSave = true;
		}else {
			if(task.getRegPeriod()==OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_DAY) {
				task.setRegInterval(1);
			}else if(task.getRegPeriod()==OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_WEEK ||task.getRegPeriod()==OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_MONTH
					||task.getRegPeriod()==OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_YEAR) {
				if(task.getRegInterval()==null) {
					JSFUtils.addMessage("taskForm:interval", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "taskSchedule.interval")));
					flagSave = true;
				}
				
				if(task.getRegDay()==null) {
					if(task.getRegPeriod()==OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_WEEK) {
						
						JSFUtils.addMessage("taskForm:regDayMenu", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "taskSchedule.day")));				
						flagSave = true;
					}else if(task.getRegPeriod()==OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_MONTH ||task.getRegPeriod()==OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_YEAR) {						
						JSFUtils.addMessage("taskForm:regDaySpinner", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "taskSchedule.day")));					
						flagSave = true;
					}
				}	
				
			}
			
			if(task.getRegPeriod()==OmbConstants.CODE_ZNACHENIE_TASK_PERIOD_YEAR && task.getRegMonth()==null) {
					JSFUtils.addMessage("taskForm:regMonthMenu", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "taskScedule.monthReg")));
					flagSave = true;
			}	
		}
						
		if(task.getTaskType()==null) {
			JSFUtils.addMessage("taskForm:taskType", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "taskSchedule.vidZad")));
			flagSave = true;
		}
		
//		if( task.getTaskSrok()==null) {
//			JSFUtils.addMessage("taskForm:term", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "taskSchedule.term")));
//			flagSave = true;
//		}
		
		if(!flagSave) {
			
			actionNextDatesReg();	
			if(datesRegList == null || datesRegList.isEmpty()) {
				JSFUtils.addMessage(null, FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "taskSchedule.invalidDefinition"));
				flagSave = true;
			}
		}
		
		if(task.getTaskInfo()==null || "".equals(task.getTaskInfo())) {
			JSFUtils.addMessage("taskForm:dopInfo", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.dopInfo")));
			flagSave = true;
		}
		
		if(task.getCodeAssign()==null) {
			JSFUtils.addMessage("taskForm:assignCode:аutoCompl_input", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "tasks.assignCode")));
			flagSave = true;
		}
		
		if(task.getZveno()==null && task.getCodeExec()==null) {
			JSFUtils.addMessage("taskForm:execCode:аutoCompl_input", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "taskSchedule.insertCodeExec" ));
			flagSave = true;
		}
		
		
		return flagSave;
	}
	
	public void actionChangeCodeExec() {
		if(task.getCodeExec()!=null) {
			task.setZveno(null);
			task.setEmplPosition(null);
		}
	}
	
	public void save() {
		
		if(checkData()) {
			return;
		}
		
		if(task.getZveno()!=null && task.getEmplPosition()==null) {
			PrimeFaces.current().executeScript("PF('hiddenZveno').jq.click();");
		}else if(task.getZveno()!=null && task.getEmplPosition()!=null) {
			PrimeFaces.current().executeScript("PF('hiddenZvenoPosition').jq.click();");
		}else {
			actionSave();
		}
	}
			
	public void actionSave() {
		
			
		if(valid) {
			task.setValid(SysConstants.CODE_ZNACHENIE_DA);
		}else {
			task.setValid(SysConstants.CODE_ZNACHENIE_NE);
		}
		
		if(workDays) {
			task.setWorkDaysOnly(SysConstants.CODE_ZNACHENIE_DA);
		}else {
			task.setWorkDaysOnly(SysConstants.CODE_ZNACHENIE_NE);
		}
		
		if(docFinish) {
			task.setDocRequired(SysConstants.CODE_ZNACHENIE_DA);
		}else {
			task.setDocRequired(SysConstants.CODE_ZNACHENIE_NE);
		}
	
		try {
				
			JPA.getUtil().runInTransaction(() -> task = this.taskDao.save(task, getSystemData()));		
		
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.succesSaveMsg"));	
			dateBegOld = task.getValidFrom();
		} catch (ObjectInUseException  e) {					
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на периодична задача! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}			

	}
	
	public void actionDelete() {
		
		try {			
			JPA.getUtil().runInTransaction(() -> this.taskDao.delete(task));				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
		} catch (ObjectInUseException  e) {					
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на периодична задача! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	
		actionNew();
	}
	
	public void actionNextDatesReg() {
		try {
			datesRegList = taskDao.listNextNRegistration(task, n, true, getSystemData());
			
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при получаване на дати на следващи регистрации! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void actionChangeZveno() {
		if(task.getZveno()==null) {
			task.setEmplPosition(null);
		}else {
			//Проверка дали избраното звено от адм.структура е служител 
			try {							
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, task.getZveno(), getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);
				if (refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL))) {
					task.setZveno(null);
				}
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}			
		}
	}
		
	public void actionSearchDocBtn(boolean rnEQ) {
		boolean om = true;  
		if (SearchUtils.isEmpty(rnDoc)) {
			
			task.setDocId(null);
			setDocObj(null);
			om = !rnEQ; 
		} else {
			om = true;
		}
		if(om && searchFlag == 0){
			searchDocRn(rnDoc, rnEQ);			
		}else {
			searchFlag = 0;
		}
	}
	
	
	private Object[] searchDocRn(String rnDoc, boolean rnEQ) {
		
		Object[] sDoc = null;

		if (rnDoc == null) {
			rnDoc = "";			
		}

		DocSearch tmp = new DocSearch(registraturaId);
		tmp.setRnDoc(rnDoc);
		tmp.setRnDocEQ(rnEQ);
	
		tmp.buildQueryComp(getUserData());

		LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(tmp, "a1 desc");
		int res = lazy.getRowCount();
		boolean om = true;
		if (res == 0 && rnEQ) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Не е намерен документ с посочения номер!");
			}
			
			task.setDocId(null);
			docObj = null;
			searchFlag = 1;
		
		} else if (res == 1 && rnEQ) {
			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);
			sDoc = result.get(0);
			task.setDocId(SearchUtils.asInteger(sDoc[0]));
			rnFullDoc = SearchUtils.asString(sDoc[1]) + "/" + DateUtils.printDate(SearchUtils.asDate(sDoc[4]));
			String dialogWidgetVar = "PF('confirmWV').show();";
			PrimeFaces.current().executeScript(dialogWidgetVar);
			searchFlag++;
			om = false;
		} 
		
		if(om) {
			sDoc = new Object[5];
			String dialogWidgetVar = "PF('mDoc').show();";
			PrimeFaces.current().executeScript(dialogWidgetVar);
		}
		return sDoc;

	}

	public void actionConfirmDoc() {

		try {
			
			docObj = null;
			searchFlag = 0;
					
		} catch (Exception e) {
			LOGGER.error("Грешка при избор на документ!", e);
		}
	}
	
	public void cancel(boolean flag) {		
		if(flag) {
			rnDoc = null;
			
			task.setDocId(null);
		}
		docObj = null;
		searchFlag=0;
	}
	
	 public void actionHideModal() {
		 
		   if(docObj != null && docObj[0] != null) {
			  
			   task.setDocId(Integer.valueOf(docObj[0].toString())); 			
			   rnDoc = SearchUtils.asString(docObj[1]);
			   rnFullDoc = SearchUtils.asString(docObj[1]) + "/" + DateUtils.printDate(SearchUtils.asDate(docObj[4]));
			    		   	    
		   }
		   docObj = null;	 
		   searchFlag = 0;		  
	   }
	
		
	public TaskSchedule getTask() {
		return task;
	}

	public void setTask(TaskSchedule task) {
		this.task = task;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public boolean isWorkDays() {
		return workDays;
	}

	public void setWorkDays(boolean workDays) {
		this.workDays = workDays;
	}

	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}

	public boolean isDocFinish() {
		return docFinish;
	}

	public void setDocFinish(boolean docFinish) {
		this.docFinish = docFinish;
	}

	public List<SelectItem> getDaysWeek() {
		return daysWeek;
	}

	public void setDaysWeek(List<SelectItem> daysWeek) {
		this.daysWeek = daysWeek;
	}

	public List<SelectItem> getMonths() {
		return months;
	}

	public void setMonths(List<SelectItem> months) {
		this.months = months;
	}

	public List<Date> getDatesRegList() {
		return datesRegList;
	}

	public void setDatesRegList(List<Date> datesRegList) {
		this.datesRegList = datesRegList;
	}

	public int getN() {
		return n;
	}

	public void setN(int n) {
		this.n = n;
	}

	public Map<Integer, Object> getSpecificsAdm() {
		return specificsAdm;
	}

	public void setSpecificsAdm(Map<Integer, Object> specificsAdm) {
		this.specificsAdm = specificsAdm;
	}

	public Map<Integer, Object> getSpecificsAdmZveno() {
		return specificsAdmZveno;
	}

	public void setSpecificsAdmZveno(Map<Integer, Object> specificsAdmZveno) {
		this.specificsAdmZveno = specificsAdmZveno;
	}

	public String getRnDoc() {
		return rnDoc;
	}

	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}

	public Object[] getDocObj() {
		return docObj;
	}

	public void setDocObj(Object[] docObj) {
		this.docObj = docObj;
	}

	public String getRnFullDoc() {
		return rnFullDoc;
	}

	public void setRnFullDoc(String rnFullDoc) {
		this.rnFullDoc = rnFullDoc;
	}

	public Integer getRegistraturaId() {
		return registraturaId;
	}

	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}
		
}
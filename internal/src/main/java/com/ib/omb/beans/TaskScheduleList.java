package com.ib.omb.beans;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.TaskScheduleDAO;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.SelectMetadata;
import com.ib.system.utils.DateUtils;

@Named
@ViewScoped
public class TaskScheduleList  extends IndexUIbean   {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5221195703372565169L;

	private static final Logger LOGGER = LoggerFactory.getLogger(TaskScheduleList.class);

	private Date decodeDate=new Date();
	private transient TaskScheduleDAO taskDao;
	private LazyDataModelSQL2Array taskList;    
	private Integer period=null;
	private Integer periodNextReg=null;
	private Integer regPeriod=null;
	private int valid = 1;
	private Date validFrom; 
	private Date validTo; 
	private Date nextRegFrom; 
	private Date nextRegTo;
	private Integer taskType;
	private String taskInfo;
	private Integer codeRef;
	private Integer codeRole;
	private Integer zveno;
	private Integer position;
	private String rnDoc;
	private Integer id;
	private boolean rnDocEQ = false;
	
	/** */
	@PostConstruct
	void initData() {
		
		taskDao = new TaskScheduleDAO(getUserData());
	}

	public void actionSearch() {
		try {
			Integer validV = null;
			if(valid==1) {
				validV = OmbConstants.CODE_ZNACHENIE_DA;
			}else if(valid == 2) {
				validV = OmbConstants.CODE_ZNACHENIE_NE;
			}
			
			SelectMetadata smd = this.taskDao.createSelectScheduleList(regPeriod, validV, validFrom, validTo, id, nextRegFrom, nextRegTo, taskType, taskInfo, codeRef, codeRole, zveno, position, rnDoc, rnDocEQ);	
			String defaultSortColumn = "A0";	
			this.taskList = new LazyDataModelSQL2Array(smd, defaultSortColumn);						
		}catch (Exception e) {
			LOGGER.error("Грешка при зареждане данните за дефиниции на периодични задачи!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void actionClear() {
		
		period = null;
		periodNextReg = null;
		regPeriod = null;
		position = null;
		zveno = null;
		codeRef = null;
		codeRole = null;
		taskInfo = null;
		taskType = null;		
		valid = 1;
		validFrom = null; 
		validTo = null; 
		nextRegFrom = null; 
		nextRegTo = null;	
		taskList = null;
		rnDoc = "";
		rnDocEQ = false;
		id = null;
	}
	
	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	public void changePeriod () {
		
    	if (this.period != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.period);
			validFrom = di[0];
			validTo = di[1];		
    	} else {
    		validFrom = null;
    		validTo = null;			
		}
    }
	
	public void changeDate() { 
		this.setPeriod(null);
	}
	
	public void changePeriodReg () {
		
    	if (this.periodNextReg != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodNextReg);
			nextRegFrom = di[0];
			nextRegTo = di[1];		
    	} else {
    		nextRegFrom = null;
    		nextRegTo = null;			
		}
    }
	
public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "taskSheduleList.titleReport");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport() , null, null);		
     
	}
	
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "taskSheduleList.titleReport");		
			new CustomExpPreProcess().preProcessPDF(document, title,  dopInfoReport(), null, null);		
						
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(),e);			
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

	public Object[] dopInfoReport() {
		Object[] dopInf = null;
		dopInf = new Object[2];
		if(validFrom != null && validTo != null) {
			dopInf[0] = "период: "+DateUtils.printDate(validFrom) + " - "+ DateUtils.printDate(validTo);
		} 
	
		return dopInf;
	}
	
	public void changeDateReg() { 
		this.setRegPeriod(null);
	}
	

	public Integer getPeriod() {
		return period;
	}

	public void setPeriod(Integer period) {
		this.period = period;
	}

	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}

	public LazyDataModelSQL2Array getTaskList() {
		return taskList;
	}

	public void setTaskList(LazyDataModelSQL2Array taskList) {
		this.taskList = taskList;
	}

	public Integer getRegPeriod() {
		return regPeriod;
	}

	public void setRegPeriod(Integer regPeriod) {
		this.regPeriod = regPeriod;
	}

	public int getValid() {
		return valid;
	}

	public void setValid(int valid) {
		this.valid = valid;
	}

	public Date getValidFrom() {
		return validFrom;
	}

	public void setValidFrom(Date validFrom) {
		this.validFrom = validFrom;
	}

	public Date getValidTo() {
		return validTo;
	}

	public void setValidTo(Date validTo) {
		this.validTo = validTo;
	}

	public Date getNextRegFrom() {
		return nextRegFrom;
	}

	public void setNextRegFrom(Date nextRegFrom) {
		this.nextRegFrom = nextRegFrom;
	}

	public Date getNextRegTo() {
		return nextRegTo;
	}

	public void setNextRegTo(Date nextRegTo) {
		this.nextRegTo = nextRegTo;
	}

	public Integer getTaskType() {
		return taskType;
	}

	public void setTaskType(Integer taskType) {
		this.taskType = taskType;
	}

	public String getTaskInfo() {
		return taskInfo;
	}

	public void setTaskInfo(String taskInfo) {
		this.taskInfo = taskInfo;
	}

	public Integer getCodeRef() {
		return codeRef;
	}

	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	public Integer getCodeRole() {
		return codeRole;
	}

	public void setCodeRole(Integer codeRole) {
		this.codeRole = codeRole;
	}

	public Integer getZveno() {
		return zveno;
	}

	public void setZveno(Integer zveno) {
		this.zveno = zveno;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}

	public Integer getPeriodNextReg() {
		return periodNextReg;
	}

	public void setPeriodNextReg(Integer periodNextReg) {
		this.periodNextReg = periodNextReg;
	}

	public String getRnDoc() {
		return rnDoc;
	}

	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}

	public boolean isRnDocEQ() {
		return rnDocEQ;
	}

	public void setRnDocEQ(boolean rnDocEQ) {
		this.rnDocEQ = rnDocEQ;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}	
}
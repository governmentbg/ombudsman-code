package com.ib.omb.beans;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.ib.omb.db.dao.ProcExeDAO;
import com.ib.omb.db.dto.ProcExe;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;

@Named
@ViewScoped
public class ProcList extends IndexUIbean   {

	/**
	 * Списък с процедури
	 * 
	 */
	private static final long serialVersionUID = -938452973061764103L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcList.class);
	
	private Integer idReg;
	private Integer exeId;
	private Integer defId;
	private String procName;
	private String procInfo;
	private String rnDoc;
	private Integer periodBegin = null;
	private Date beginFrom;
	private Date beginTo;
	private Integer periodSrok = null;
	private Date srokFrom;
	private Date srokTo;
	private Integer periodEnd = null;
	private Date endFrom;
	private Date endTo;
	private List<Integer> procCodeRefList;
	private List<SystemClassif> scCodeRefList; 
	private Integer etapCodeRef;
	private boolean overdue = false;
	
	private LazyDataModelSQL2Array proceduresList;	
	
	private Date decodeDate;
	
	private Integer idDefProc;
	private List<SystemClassif> proceduriList;
	private ProcExe procExe;
	
	private List<SystemClassif> defProcList;
	
	private List<SystemClassif> statusListClassif;
	private List<Integer> statusList;
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct!!!");
		
		this.idReg = getUserData(UserData.class).getRegistratura();
		
		this.periodBegin = null;
		this.periodSrok = null;
		this.periodEnd = null;
		this.overdue = false;
		this.procCodeRefList = new ArrayList<>();
		this.scCodeRefList = new ArrayList<>();
		this.decodeDate = new Date();	
		
		this.proceduriList = new ArrayList<>();
		this.procExe = new ProcExe();
		
		this.defProcList = new ArrayList<>(); 
		
		this.statusListClassif = new ArrayList<>();
		this.statusList = new ArrayList<>();
		
		actionFindAllDefProcedures(false);
	}
	
	/** 
	 * Списък с процедури по зададени критерии 
	 * 
	 */
	public void actionSearch(){
		
		SelectMetadata smd = new ProcExeDAO(getUserData()).createSelectProcExeList(this.idReg, this.exeId, this.defId, this.procName, this.procInfo, this.rnDoc, this.statusList, this.beginFrom, this.beginTo, this.srokFrom, this.srokTo, this.endFrom, this.endTo, this.procCodeRefList, this.etapCodeRef, this.overdue, (UserData) getUserData());
		String defaultSortColumn = "a0";
		this.proceduresList = new LazyDataModelSQL2Array(smd, defaultSortColumn);			
	} 
	
	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClear() {		
		
		this.idReg = getUserData(UserData.class).getRegistratura();
		this.exeId = null;
		this.defId = null;
		this.procName = null;
		this.procInfo = null;
		this.rnDoc = null;
		this.periodBegin = null;
		this.beginFrom = null;
		this.beginTo = null;
		this.periodSrok = null;
		this.srokFrom = null;
		this.srokTo = null;
		this.periodEnd = null;
		this.endFrom = null;
		this.endTo = null;
		this.procCodeRefList = new ArrayList<>();
		this.scCodeRefList = new ArrayList<>();
		this.etapCodeRef = null;
		this.overdue = false;
		
		this.proceduresList = null;
		
		this.statusListClassif = new ArrayList<>();
		this.statusList = new ArrayList<>();
	}
	
	/** Методи за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	public void changePeriodBegin() {
		
    	if (this.periodBegin != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodBegin);
			this.beginFrom = di[0];
			this.beginTo = di[1];		
    	} else {
    		this.beginFrom = null;
    		this.beginTo = null;			
		}
    }
	
	public void changeDateBegin() { 
		this.setPeriodBegin(null);
	}	
	
	public void changePeriodSrok() {
		
    	if (this.periodSrok != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodSrok);
			this.srokFrom = di[0];
			this.srokTo = di[1];		
    	} else {
    		this.srokFrom = null;
    		this.srokTo = null;			
		}
    }
	
	public void changeDateSrok() { 
		this.setPeriodSrok(null);
	}
	
	public void changePeriodEnd() {
		
    	if (this.periodEnd != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodEnd);
			this.endFrom = di[0];
			this.endTo = di[1];		
    	} else {
    		this.endFrom = null;
    		this.endTo = null;			
		}
    }
	
	public void changeDateEnd() { 
		this.setPeriodEnd(null);
	}

	public String actionGoto(Integer idObj) {
		
		return "procExeEdit.jsf?faces-redirect=true&idObj=" + idObj;
	}
	
	public String actionCreateProc() {
		
		try {
			
			JPA.getUtil().runInTransaction(() -> this.procExe = new ProcExeDAO(getUserData()).startProc(this.idDefProc, null, getSystemData()));
			
			actionSearch();		
			
		} catch (BaseException e) {			
			LOGGER.error("Грешка при стратиране на процедура! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
		return "procExeEdit.jsf?faces-redirect=true&idObj=" + this.procExe.getId();
	}
	
	public void actionFindAllDefProcedures(boolean createProc) {
		
		try {
			
			if (createProc) {
			
				this.proceduriList = new ArrayList<>(); 
					
				Map<Integer, Object> specifics = new HashMap<>();
				specifics.put(OmbClassifAdapter.PROCEDURI_INDEX_REGISTRATURA, getUserData(UserData.class).getRegistratura()); // специфика за процедури по ид на регистратура
				specifics.put(OmbClassifAdapter.PROCEDURI_INDEX_STATUS, OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE); // специфика по статус активна за процедурите
				specifics.put(OmbClassifAdapter.PROCEDURI_INDEX_DOC_TYPE, 0); // специфика по тип инициращ документ
		
				this.proceduriList = getSystemData().queryClassification(OmbConstants.CODE_CLASSIF_PROCEDURI, null, this.decodeDate, getCurrentLang(), specifics);
			
			} else {
				
				this.defProcList = new ArrayList<>(); 
				
				Map<Integer, Object> specifics = new HashMap<>();
				specifics.put(OmbClassifAdapter.PROCEDURI_INDEX_REGISTRATURA, this.idReg); // специфика за процедури по ид на регистратура
				specifics.put(OmbClassifAdapter.PROCEDURI_INDEX_STATUS, OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE); // специфика по статус активна за процедурите
		
				this.defProcList = getSystemData().queryClassification(OmbConstants.CODE_CLASSIF_PROCEDURI, null, this.decodeDate, getCurrentLang(), specifics);
			}
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на списъка с процедури! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	
	}
	
/******************************** EXPORTS **********************************/
	
	public Integer getIdReg() {
		return idReg;
	}

	public void setIdReg(Integer idReg) {
		this.idReg = idReg;
	}
	
	public Integer getExeId() {
		return exeId;
	}

	public void setExeId(Integer exeId) {
		this.exeId = exeId;
	}

	public Integer getDefId() {
		return defId;
	}

	public void setDefId(Integer defId) {
		this.defId = defId;
	}

	public String getProcName() {
		return procName;
	}

	public void setProcName(String procName) {
		this.procName = procName;
	}

	public String getProcInfo() {
		return procInfo;
	}

	public void setProcInfo(String procInfo) {
		this.procInfo = procInfo;
	}

	public String getRnDoc() {
		return rnDoc;
	}

	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}

	public Integer getPeriodBegin() {
		return periodBegin;
	}

	public void setPeriodBegin(Integer periodBegin) {
		this.periodBegin = periodBegin;
	}	

	public Date getBeginFrom() {
		return beginFrom;
	}

	public void setBeginFrom(Date beginFrom) {
		this.beginFrom = beginFrom;
	}

	public Date getBeginTo() {
		return beginTo;
	}

	public void setBeginTo(Date beginTo) {
		this.beginTo = beginTo;
	}
	
	public Integer getPeriodSrok() {
		return periodSrok;
	}

	public void setPeriodSrok(Integer periodSrok) {
		this.periodSrok = periodSrok;
	}	

	public Date getSrokFrom() {
		return srokFrom;
	}

	public void setSrokFrom(Date srokFrom) {
		this.srokFrom = srokFrom;
	}

	public Date getSrokTo() {
		return srokTo;
	}

	public void setSrokTo(Date srokTo) {
		this.srokTo = srokTo;
	}
	
	public Integer getPeriodEnd() {
		return periodEnd;
	}

	public void setPeriodEnd(Integer periodEnd) {
		this.periodEnd = periodEnd;
	}

	public Date getEndFrom() {
		return endFrom;
	}

	public void setEndFrom(Date endFrom) {
		this.endFrom = endFrom;
	}

	public Date getEndTo() {
		return endTo;
	}

	public void setEndTo(Date endTo) {
		this.endTo = endTo;
	}

	public List<Integer> getProcCodeRefList() {
		return procCodeRefList;
	}

	public void setProcCodeRefList(List<Integer> procCodeRefList) {
		this.procCodeRefList = procCodeRefList;
	}

	public List<SystemClassif> getScCodeRefList() {
		return scCodeRefList;
	}

	public void setScCodeRefList(List<SystemClassif> scCodeRefList) {
		this.scCodeRefList = scCodeRefList;
	}

	public Integer getEtapCodeRef() {
		return etapCodeRef;
	}

	public void setEtapCodeRef(Integer etapCodeRef) {
		this.etapCodeRef = etapCodeRef;
	}

	public boolean isOverdue() {
		return overdue;
	}

	public void setOverdue(boolean overdue) {
		this.overdue = overdue;
	}	

	public LazyDataModelSQL2Array getProceduresList() {
		return proceduresList;
	}

	public void setProceduresList(LazyDataModelSQL2Array proceduresList) {
		this.proceduresList = proceduresList;
	}

	public Date getDecodeDate() {
		return new Date(decodeDate.getTime()) ;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
	}	

	public Integer getIdDefProc() {
		return idDefProc;
	}

	public void setIdDefProc(Integer idDefProc) {
		this.idDefProc = idDefProc;
	}

	public List<SystemClassif> getProceduriList() {
		return proceduriList;
	}

	public void setProceduriList(List<SystemClassif> proceduriList) {
		this.proceduriList = proceduriList;
	}

	public ProcExe getProcExe() {
		return procExe;
	}

	public void setProcExe(ProcExe procExe) {
		this.procExe = procExe;
	}

	public List<SystemClassif> getDefProcList() {
		return defProcList;
	}

	public void setDefProcList(List<SystemClassif> defProcList) {
		this.defProcList = defProcList;
	}

	public List<SystemClassif> getStatusListClassif() {
		return statusListClassif;
	}

	public void setStatusListClassif(List<SystemClassif> statusListClassif) {
		this.statusListClassif = statusListClassif;
	}

	public List<Integer> getStatusList() {
		return statusList;
	}

	public void setStatusList(List<Integer> statusList) {
		this.statusList = statusList;
	}

	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за списък с процедури за изпълнение
	 */
	public void postProcessXLSProcList(Object document) {
		
		String title = getMessageResourceString(LABELS, "procList.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport(), null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за за списък с процедури за изпълнение
	 */
	public void preProcessPDFProcList(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "procList.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title, dopInfoReport(), null, null);		
						
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
	
	/**
	 * подзаглавие за екпсорта 
	 */
	public Object[] dopInfoReport() {
		
		Object[] dopInf = null;
		dopInf = new Object[3];
		
		if(this.beginFrom != null && this.beginTo != null) {
			dopInf[0] = "период на начало на изпълнение: "+ DateUtils.printDate(this.beginFrom) + " - "+ DateUtils.printDate(this.beginTo);
		} 
		
		if(this.srokFrom != null && this.srokTo != null) {			
			dopInf[1] = "период на срок за изпълнение: "+ DateUtils.printDate(this.srokFrom) + " - "+ DateUtils.printDate(this.srokTo);
		} 
		
		if(this.endFrom != null && this.endTo != null) {
			dopInf[2] = "период на край на изпълнение: "+ DateUtils.printDate(this.endFrom) + " - "+ DateUtils.printDate(this.endTo);
		} 
	
		return dopInf;
	}

}
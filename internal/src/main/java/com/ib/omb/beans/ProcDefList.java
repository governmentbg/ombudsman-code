package com.ib.omb.beans;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.ib.omb.db.dao.ProcDefDAO;
import com.ib.omb.db.dto.ProcDef;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;

@Named
@ViewScoped
public class ProcDefList extends IndexUIbean   {

	/**
	 * Списък с дефиниции на процедури
	 * 
	 */
	private static final long serialVersionUID = -4029018534201157809L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcDefList.class);
	
	private Integer idReg;
	private Integer nomerProc;
	private String nameProc;
	private String opisProc;
	private Integer status;
	private Integer sluj; 
	private Integer zveno;
	private Integer dlajnost;
	private Integer businessRole;
	
	private LazyDataModelSQL2Array defProcList;	
	
	private Date decodeDate;
	
	private Integer fromRegistratura;
	private List<SystemClassif> classifRegsList;
	private Integer defId;
	private ProcDef newProc;
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct!!!");
		
		this.classifRegsList = new ArrayList<>();
		this.newProc = new ProcDef();
		this.decodeDate = new Date();	
	}
	
	/** 
	 * Списък с дефиниции на процедури по зададени критерии 
	 * 
	 */
	public void actionSearch(){
	
		SelectMetadata smd = new ProcDefDAO(getUserData()).createSelectDefProceduri(idReg, nomerProc, nameProc, opisProc, status, sluj, zveno, dlajnost, businessRole);
		String defaultSortColumn = "a0";
		this.defProcList = new LazyDataModelSQL2Array(smd, defaultSortColumn);			
	} 
	
	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClear() {		
		
		this.idReg = null;
		this.nomerProc = null;
		this.nameProc = null;
		this.opisProc = null;
		this.status = null;
		this.sluj = null; 
		this.zveno = null;
		this.dlajnost = null;
		this.businessRole = null;
		
		
		this.defProcList = null;
	}
	
	public void actionSelectSluj() {
		
		try {
			
			if (this.sluj != null) {
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.sluj, getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (!refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL))) {				
					this.sluj = null;
					JSFUtils.addMessage("formProcDefList" + ":selectSluj:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procDefList.choiceSluj"));
				}			
			} 
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на служител! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	public void actionSelectZveno() {
		
		try {
			
			if (this.zveno != null) {
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.zveno, getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (!refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO))) {				
					this.zveno = null;
					JSFUtils.addMessage("formProcDefList" + ":selectZveno:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procDefList.choiceZveno"));
				}			
			} 
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на звено! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void actionFindAllRegistraturi(Integer idDef) {
		
		try {
			
			this.defId = idDef;
			
			this.classifRegsList = getSystemData().queryClassification(OmbConstants.CODE_CLASSIF_REGISTRATURI, null, new Date(), getCurrentLang());
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на списъка с регистратурите без регистратурата на процедурата! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public String actionCopyDefProc() {
		
		try {
			
			JPA.getUtil().runInTransaction(() -> this.newProc = new ProcDefDAO(getUserData()).copyProcDef(this.defId, null, fromRegistratura, getSystemData()));

			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_PROCEDURI, false, false);
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS, false, false);
			
			actionSearch();
			
//			String dialogWidgetVar = "PF('modalCopyDefProc').hide();";
//			PrimeFaces.current().executeScript(dialogWidgetVar);			
			
		} catch (BaseException e) {			
			LOGGER.error("Грешка при копиране на дефиниция на процедура! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
		return "procDefEdit.jsf?faces-redirect=true&idObj=" + this.newProc.getId();
	}
	
	public String actionGoto(Integer idObj) {
		
		return "procDefEdit.jsf?faces-redirect=true&idObj=" + idObj;
	}
	
	public String actionGotoNew() {
		
		return "procDefEdit.jsf?faces-redirect=true";	
	}
	
	
/******************************** EXPORTS **********************************/
	
	public Integer getIdReg() {
		return idReg;
	}

	public void setIdReg(Integer idReg) {
		this.idReg = idReg;
	}
		
	public Integer getNomerProc() {
		return nomerProc;
	}

	public void setNomerProc(Integer nomerProc) {
		this.nomerProc = nomerProc;
	}

	public String getNameProc() {
		return nameProc;
	}

	public void setNameProc(String nameProc) {
		this.nameProc = nameProc;
	}

	public String getOpisProc() {
		return opisProc;
	}

	public void setOpisProc(String opisProc) {
		this.opisProc = opisProc;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Integer getSluj() {
		return sluj;
	}

	public void setSluj(Integer sluj) {
		this.sluj = sluj;
	}

	public Integer getZveno() {
		return zveno;
	}

	public void setZveno(Integer zveno) {
		this.zveno = zveno;
	}

	public Integer getDlajnost() {
		return dlajnost;
	}

	public void setDlajnost(Integer dlajnost) {
		this.dlajnost = dlajnost;
	}

	public Integer getBusinessRole() {
		return businessRole;
	}

	public void setBusinessRole(Integer businessRole) {
		this.businessRole = businessRole;
	}

	public LazyDataModelSQL2Array getDefProcList() {
		return defProcList;
	}

	public void setDefProcList(LazyDataModelSQL2Array defProcList) {
		this.defProcList = defProcList;
	}	
	
	public Date getDecodeDate() {
		return new Date(decodeDate.getTime()) ;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
	}	
	
	public Integer getFromRegistratura() {
		return fromRegistratura;
	}

	public void setFromRegistratura(Integer fromRegistratura) {
		this.fromRegistratura = fromRegistratura;
	}

	public List<SystemClassif> getClassifRegsList() {
		return classifRegsList;
	}

	public void setClassifRegsList(List<SystemClassif> classifRegsList) {
		this.classifRegsList = classifRegsList;
	}

	public Integer getDefId() {
		return defId;
	}

	public void setDefId(Integer defId) {
		this.defId = defId;
	}

	public ProcDef getNewProc() {
		return newProc;
	}

	public ProcDef setNewProc(ProcDef newProc) {
		this.newProc = newProc;
		return newProc;
	}

	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за кореспондентите в групата
	 */
	public void postProcessXLSProcDefList(Object document) {
		
		String title = getMessageResourceString(LABELS, "procDefList.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport(), null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за кореспондентите в групата
	 */
	public void preProcessPDFProcDefList(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "procDefList.reportTitle");		
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
	 * подзаглавие за екпсорта - дали да остане???
	 */
	public Object[] dopInfoReport() {
		Object[] dopInf = null;
		dopInf = new Object[2];
	
		return dopInf;
	}

}
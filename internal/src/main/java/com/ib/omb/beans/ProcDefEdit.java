package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.navigation.Navigation;
import com.ib.indexui.navigation.NavigationData;
import com.ib.indexui.navigation.NavigationDataHolder;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.ProcDefDAO;
import com.ib.omb.db.dto.ProcDef;
import com.ib.omb.db.dto.ProcDefEtap;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.SearchUtils;

@Named
@ViewScoped
public class ProcDefEdit extends IndexUIbean  implements Serializable {	
	
	
	/**
	 * Въвеждане / актуализация на дефиниция на процедура 
	 * 
	 */
	private static final long serialVersionUID = -6080711597503598046L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcDefEdit.class);
	
	private static final String ID_OBJ = "idObj";
	private static final String FORM_PROC_DEF = "formProcDef";
	
	private Date decodeDate = new Date();
	
	private ProcDef procDef;	
	private List<ProcDefEtap> defEtapsList;
	
	private transient ProcDefDAO procDAO;
	
	private List<Files> filesList;
	
	private Integer oldStatus;
	
	private List<SelectItem> emplPositionList;
	private List<SelectItem> businessRoleList;
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct!!!");
		
		try {
		
			this.procDAO = new ProcDefDAO(getUserData());
		
			this.procDef = new ProcDef();
			this.defEtapsList = new ArrayList<>();	
			
			this.emplPositionList = new ArrayList<>();
			this.businessRoleList = new ArrayList<>();
			
			List<SystemClassif> itemsEmplPos = getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_POSITION, new Date(), getCurrentLang());
			List<SystemClassif> itemsBusRole = getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_PROC_BUSINESS_ROLE, new Date(), getCurrentLang());
						
			for (SystemClassif item : itemsEmplPos) {
				this.emplPositionList.add(new SelectItem(item.getCode(), item.getTekst()));				
			}
			
			Collections.sort(this.emplPositionList, compatator);
			
			for (SystemClassif item : itemsBusRole) {
				this.businessRoleList.add(new SelectItem(item.getCode(), item.getTekst()));				
			}
			
			Collections.sort(this.businessRoleList, compatator);
			
			if (JSFUtils.getRequestParameter(ID_OBJ) != null && !"".equals(JSFUtils.getRequestParameter(ID_OBJ))) {
				
				Integer idObj = Integer.valueOf(JSFUtils.getRequestParameter(ID_OBJ));	
				
				if (idObj != null) {
					
					JPA.getUtil().runWithClose(() -> {
						
						this.procDef = this.procDAO.findById(idObj);					
	
						// извличане на файловете от таблица с файловете
						this.filesList = new FilesDAO(getUserData()).selectByFileObject(this.procDef.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_DEF);
						
						this.defEtapsList = this.procDAO.selectDefEtapList(this.procDef.getId(), null);
						
						this.oldStatus = this.procDef.getStatus();
					});
				}				
			
			} else {
				
				this.procDef.setStatus(OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_DEV );
				this.procDef.setWorkDaysOnly(OmbConstants.CODE_ZNACHENIE_NE);
				this.oldStatus = this.procDef.getStatus();
			}			
					
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на дефиниция на процедура ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	public void actionNew() {
		
		this.procDef = new ProcDef();
		this.procDef.setStatus(OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_DEV );
		this.procDef.setWorkDaysOnly(OmbConstants.CODE_ZNACHENIE_NE);
		this.defEtapsList = new ArrayList<>();		
	}
	
	private boolean checkDataForProc() {
		
		boolean save = false;	
		
		if (this.procDef.getRegistraturaId() == null) {
			JSFUtils.addMessage(FORM_PROC_DEF + ":regId", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "regData.registratura")));
			save = true;
		}
		
		if(SearchUtils.isEmpty(this.procDef.getProcName())) {
			JSFUtils.addMessage(FORM_PROC_DEF + ":procName", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefList.nameProc")));
			save = true;
		}
		
		if(SearchUtils.isEmpty(this.procDef.getProcInfo())) {
			JSFUtils.addMessage(FORM_PROC_DEF + ":procInfo", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefList.opisProc")));
			save = true;
		}
		
		if (this.procDef.getSrokDays() == null) {
			JSFUtils.addMessage(FORM_PROC_DEF + ":srokDays", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefList.srokDni")));
			save = true;
		}
		
		if (this.procDef.getWorkDaysOnly() == null) {
			JSFUtils.addMessage(FORM_PROC_DEF + ":workDaysOnly", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefEdit.workDaysOnly")));
			save = true;
		}
		
		if (this.procDef.getStatus() == null) {
			JSFUtils.addMessage(FORM_PROC_DEF + ":activeDef", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefList.activeDef")));
			save = true;
		}
		
		if (this.procDef.getCodeRef() == null && this.procDef.getZveno() == null && this.procDef.getEmplPosition() == null && this.procDef.getBusinessRole() == null) {		
			JSFUtils.addMessage(FORM_PROC_DEF + ":otgIzpal", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "procDefEdit.choiceOtgIzpal", getMessageResourceString(LABELS, "procDefEdit.particInProc")));
			save = true; 
		
		} else {
			
			if (this.procDef.getCodeRef() == null && this.procDef.getZveno() != null && this.procDef.getEmplPosition() == null && this.procDef.getBusinessRole() == null) { 
				JSFUtils.addMessage(FORM_PROC_DEF + ":otgIzpal", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "procDefEdit.choiceDlajOrBussRole"));
				save = true;			
			} 
			
			if (this.procDef.getCodeRef() == null && this.procDef.getZveno() == null && this.procDef.getEmplPosition() != null) {
				JSFUtils.addMessage(FORM_PROC_DEF + ":selectZveno:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefList.zveno")));
				save = true;
			}			
		}
		
		return save;		
	}	
	
	public void actionSave() {
		
		if(checkDataForProc()) {
			return;
		}
		
		try {
			
			JPA.getUtil().runInTransaction(() -> this.procDef = this.procDAO.save(this.procDef, getSystemData()));			

			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_PROCEDURI, false, false);			
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS, false, false);
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
			
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на дефиниция на процедура! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			
			if (!this.oldStatus.equals(this.procDef.getStatus())) {
				this.procDef.setStatus(this.oldStatus);	 			
			}
		}
	}
	
	public void actionValidate() {
		
		try {
			
			JPA.getUtil().runWithClose(() ->  {
				
				String err = this.procDAO.validate(this.procDef, getSystemData());
				
				if (err == null) {					
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "procDefEdit.validDefProc"));					
				
				} else {	
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, err);
				}
			});	
			
		} catch (BaseException e) {			
			LOGGER.error("Грешка при валидиране на дефиниция на процедура! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void actionDelete() {
		
		try {			
			
			JPA.getUtil().runInTransaction(() ->  this.procDAO.delete(this.procDef));
			
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_PROCEDURI, false, false);		
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS, false, false);
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			
			actionNew();
			
		    
		} catch (ObjectInUseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());		
	
		} catch (BaseException e) {
			LOGGER.error("Грешка при изтриване на процедура! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
		Navigation navHolder = new Navigation();			
	    int i = navHolder.getNavPath().size();	
	   
	    NavigationDataHolder dataHoslder = (NavigationDataHolder) JSFUtils.getManagedBean("navigationSessionDataHolder");
	    Stack<NavigationData> stackPath = dataHoslder.getPageList();
	    NavigationData nd = stackPath.get(i-2);
	    Map<String, Object> mapV = nd.getViewMap();
	    
	    ProcDefList procList = (ProcDefList) mapV.get("procDefList");	    
	    procList.actionSearch();
		
	} 
	
	public void actionSelectSlujForProc() {
		
		try {
			
			if (this.procDef.getCodeRef() != null) {
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.procDef.getCodeRef(), getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (!refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL))) {				
					this.procDef.setCodeRef(null);
					JSFUtils.addMessage(FORM_PROC_DEF + ":selectSluj:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procDefList.choiceSluj"));
				}			
			} 
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на служител! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	public void actionSelectZvenoForProc() {
		
		try {
			
			if (this.procDef.getZveno() != null) {
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.procDef.getZveno(), getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (!refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO))) {				
					this.procDef.setZveno(null);
					JSFUtils.addMessage(FORM_PROC_DEF + ":selectZveno:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procDefList.choiceZveno"));
				}			
			} 
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на звено! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public String actionGotoEtap(Integer idObj) {
		
		return "procDefEtapEdit.jsf?faces-redirect=true&idObj=" + idObj + "&idProc=" + this.procDef.getId();
	}
	
	public String actionGotoNewEtap() {
		
		return "procDefEtapEdit.jsf?faces-redirect=true&idProc=" + this.procDef.getId();
	}
	
	/******************************************************* GET & SET *******************************************************/	
	
	public Date getDecodeDate() {
		return new Date(decodeDate.getTime()) ;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
	}

	public ProcDef getProcDef() {
		return procDef;
	}

	public void setProcDef(ProcDef procDef) {
		this.procDef = procDef;
	}	

	public List<ProcDefEtap> getDefEtapsList() {
		return defEtapsList;
	}

	public void setDefEtapsList(List<ProcDefEtap> defEtapsList) {
		this.defEtapsList = defEtapsList;
	}
	
	public List<Files> getFilesList() {
		return filesList;
	}

	public void setFilesList(List<Files> filesList) {
		this.filesList = filesList;
	}

	public Integer getOldStatus() {
		return oldStatus;
	}

	public void setOldStatus(Integer oldStatus) {
		this.oldStatus = oldStatus;
	}

	public List<SelectItem> getEmplPositionList() {
		return emplPositionList;
	}

	public void setEmplPositionList(List<SelectItem> emplPositionList) {
		this.emplPositionList = emplPositionList;
	}	
	
	public List<SelectItem> getBusinessRoleList() {
		return businessRoleList;
	}

	public void setBusinessRoleList(List<SelectItem> businessRoleList) {
		this.businessRoleList = businessRoleList;
	}

	/**************************************************** END GET & SET ****************************************************/	
	
	transient Comparator<SelectItem> compatator = new Comparator<SelectItem>() {
		public int compare(SelectItem s1, SelectItem s2) {
			return (s1.getLabel().toUpperCase().compareTo(s2.getLabel().toUpperCase()));
		}
	};
}
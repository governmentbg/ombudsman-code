package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.RegistraturaGroupDAO;
import com.ib.omb.db.dto.RegistraturaGroup;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;

@Named
@ViewScoped
public class RegistraturaGroupSluj extends IndexUIbean  implements Serializable {
	
	/**
	 * Въвеждане / актуализация на групи служители
	 * 
	 */
	private static final long serialVersionUID = 2589004956717285049L;
	private static final Logger LOGGER = LoggerFactory.getLogger(RegistraturaGroupSluj.class);
	
	private static final String FORM_REG_GROUP_REF = "formRegGroupRef";
//	private static final String ERROR_KLASSIF = "general.errorClassif";
	
	private RegistraturaGroup groupSluj;
	private transient RegistraturaGroupDAO regGrDAO;
	private Integer idReg;
	private List<RegistraturaGroup> regGrSlujList;
	
	// това за момента няма да се използва зареждане на класификацията по специфика, заради пренасянето на групите служители като отделна опция
	//private Map<Integer, Object> specificsReferents; // за включени служители
	private List<SystemClassif>	scReferentsList = new ArrayList<>();
	private LazyDataModelSQL2Array slujList;
	
	private List<SelectItem> registraturiList;	
	private Integer codeRef;
	private String name;
	private String info;
	private List<Integer> usersRegList;
	private boolean fromSave;
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct!!!");	
			
		this.regGrDAO = new RegistraturaGroupDAO(getUserData());
		
		this.idReg = getUserData(UserData.class).getRegistratura();
		this.regGrSlujList = new ArrayList<>();
		
		this.registraturiList = new ArrayList<>();
		this.codeRef = null;
		this.name = null;
		this.info = null;
		this.usersRegList = new ArrayList<>();
		this.fromSave = false;
		
//		try {
//			
//			if (getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_REG_GROUP_ADM)) {	
//				
//				this.registraturiList = createItemsList(false, OmbConstants.CODE_CLASSIF_REGISTRATURI, new Date(), false);
//				
//				for (SelectItem reg : this.registraturiList) {
//					this.usersRegList.add(SearchUtils.asInteger(reg.getValue()));					
//				}
//			
//			} else {
//				
//				if (getUserData(UserData.class).isDelovoditel()) {
//					
//					this.registraturiList = createItemsList(true, OmbConstants.CODE_CLASSIF_REGISTRATURI, new Date(), true);
//					
//					for (SelectItem reg : this.registraturiList) {
//						this.usersRegList.add(SearchUtils.asInteger(reg.getValue()));					
//					}
//				
//				} else {
//					
//					String nameReg = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REGISTRATURI, getUserData(UserData.class).getRegistratura(), getCurrentLang(), new Date());
//						
//					this.registraturiList.add(new SelectItem(getUserData(UserData.class).getRegistratura(), nameReg));
//					
//					this.usersRegList.add(getUserData(UserData.class).getRegistratura());					
//					
//				}
//			}
//		
//		} catch (DbErrorException | UnexpectedResultException e) {
//			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
//			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
//		}
			
//		this.specificsReferents  = Collections.singletonMap(OmbClassifAdapter.REFERENTS_INDEX_REGISTRATURA, this.idReg); 
			
	}
	
	/** 
	 * Списък с групи по зададени критерии 
	 * 
	 */
	public void actionSearch(){
		
		try {
			
			if (this.idReg != null) {
				this.usersRegList = new ArrayList<>();
				this.usersRegList.add(this.idReg);
			}
			
			JPA.getUtil().runWithClose(() -> this.regGrSlujList = this.regGrDAO.findByRegistraturaId(this.usersRegList, Arrays.asList(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_EMPL), this.codeRef, this.name, this.info));
			
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на групите! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}		
	} 
	
	/**
	 * Премахва избраните критерии за търсене
	 */
	public void actionClear() {		
		
		this.codeRef = null;
		this.name = null;
		this.info = null;
		this.idReg = null;
		
		this.regGrSlujList.clear();
		this.usersRegList = new ArrayList<>();
		
		if (!this.fromSave) {
			this.groupSluj = new RegistraturaGroup();
			this.groupSluj = null;
		}
		
	}

	
	public void actionEdit(Integer idObj) {
		
		try {
			
			if (idObj != null) {
		
				JPA.getUtil().runWithClose(() -> this.groupSluj = this.regGrDAO.findById(idObj));					
				
				this.scReferentsList = new ArrayList<>();
				
				List<SystemClassif> tmpLst = new ArrayList<>();
				
				if(this.groupSluj.getReferentIds() != null && !this.groupSluj.getReferentIds().isEmpty()) {
					
					for (Integer item : this.groupSluj.getReferentIds()) {
						
						String tekst = "";
						SystemClassif scItem = new SystemClassif();
						
						try {
							
							scItem.setCodeClassif(OmbConstants.CODE_CLASSIF_ADMIN_STR);
							scItem.setCode(item);
							tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, item, getUserData().getCurrentLang(), new Date());		
							scItem.setTekst(tekst);
							tmpLst.add(scItem);
						
						} catch (DbErrorException e) {
							LOGGER.error("Грешка при зареждане на включени служители! ", e);
						}						
					}
				}
				
				setScReferentsList(tmpLst); 
				
				actionSearch(idObj);
			}
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на регистър! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	public void actionSearch(Integer groupId) {
		
		SelectMetadata smd = this.regGrDAO.findReferentsByIdGroup(groupId, OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_EMPL, getUserData());
		String defaultSortColumn = "A0";
		this.slujList = new LazyDataModelSQL2Array(smd, defaultSortColumn);		
	}
	
	public void actionSelectSluj() {

		if (this.groupSluj.getReferentIds() != null) {
				
			try {
				
				List<Integer> tmpListRef = new ArrayList<>();
				
				for (Integer refCode : this.groupSluj.getReferentIds()) {
					
					Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, refCode, getCurrentLang(), new Date(), OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);
				
					if (refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO))) {
						tmpListRef.add(refCode);
					}
				}
				
				if (!tmpListRef.isEmpty()) {
					
					for (Integer ref : tmpListRef) {
						this.groupSluj.getReferentIds().remove(ref);											
					}					
				}				

				JPA.getUtil().runInTransaction(() -> this.regGrDAO.saveRegistraturaReferent(this.groupSluj, this.groupSluj.getReferentIds(), getSystemData()));

				getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_GROUP_EMPL, false, false);

				actionSearch(this.groupSluj.getId());
				
				this.groupSluj.getReferentIds().clear();
				
				this.scReferentsList.clear();
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
			
			} catch (ObjectInUseException e) {
				LOGGER.error(e.getMessage());
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
			
			} catch (BaseException e) {
				LOGGER.error("Грешка при запис на служители към групата! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}
	}
	
	public void actionDeleteSluj(Integer idObj) {

		try {

			if (idObj != null) {

				JPA.getUtil().runInTransaction(() -> this.regGrDAO.deleteRegistraturaReferent(this.groupSluj, idObj));
				
				getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_GROUP_EMPL, false, false);
				
				actionSearch(this.groupSluj.getId());
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			}

		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на регистър! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}

	}
	
	public void actionNew() {	
				
		this.groupSluj = new RegistraturaGroup();
		this.scReferentsList.clear();
		this.slujList = null;
	}
	
	private boolean checkData() {
		
		boolean save = false;		

		if(this.groupSluj.getGroupName() == null || this.groupSluj.getGroupName().isEmpty()) {
			JSFUtils.addMessage(FORM_REG_GROUP_REF + ":naimGr", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString("labels", "regGrSluj.naimGr")));
			save = true;
		}
		
//		if(this.groupSluj.getRegistraturaId() == null) {
//			JSFUtils.addMessage(FORM_REG_GROUP_REF + ":idRegGr", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString("labels", "regData.registratura")));
//			save = true;
//		}
//		
//		if (this.groupSluj.getReferentIds() == null || this.groupSluj.getReferentIds().isEmpty()) {
//			JSFUtils.addMessage("formRegGroupRef:refList:autoComplM", FacesMessage.SEVERITY_ERROR, getMessageResourceString("ui_beanMessages", "general.pleaseInsert", getMessageResourceString("labels", "regGrSluj.inclSluj")));
//			save = true;
//		}
		
		if (save) {
			PrimeFaces.current().executeScript("scrollToErrors()");
		}
		
		return save;		
	}
	
	public void actionSave() {
		
		if(checkData()) {
			return;
		}
		
		try {
			
			this.groupSluj.setRegistraturaId(this.idReg); 
			
			this.groupSluj.setGroupType(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_EMPL);
			
			JPA.getUtil().runInTransaction(() -> this.groupSluj = this.regGrDAO.save(this.groupSluj));
		
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_GROUP_EMPL, false, false);
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
			
//			actionNew();
			
			this.fromSave = true;

			actionClear();
		   
			JPA.getUtil().runWithClose(() -> this.regGrSlujList = this.regGrDAO.findByRegistraturaId(this.usersRegList, Arrays.asList(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_EMPL), this.codeRef, this.name, this.info));
		
			PrimeFaces.current().executeScript("scrollToErrors()");
			
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на групи служители! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	public void actionDelete() {
		
		try {
			
			JPA.getUtil().runInTransaction(() -> this.regGrDAO.delete(this.groupSluj));
			
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_GROUP_EMPL, false, false);
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			
			actionNew();
			
			actionClear();
			
			JPA.getUtil().runWithClose(() -> this.regGrSlujList = this.regGrDAO.findByRegistraturaId(this.usersRegList, Arrays.asList(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_EMPL), this.codeRef, this.name, this.info));
			
			PrimeFaces.current().executeScript("scrollToErrors()");
			
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на група служители! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}

	public RegistraturaGroup getGroupSluj() {
		return groupSluj;
	}

	public void setGroupSluj(RegistraturaGroup groupSluj) {
		this.groupSluj = groupSluj;
	}

	public Integer getIdReg() {
		return idReg;
	}

	public void setIdReg(Integer idReg) {
		this.idReg = idReg;
	}

	public List<RegistraturaGroup> getRegGrSlujList() {
		return regGrSlujList;
	}

	public void setRegGrSlujList(List<RegistraturaGroup> regGrSlujList) {
		this.regGrSlujList = regGrSlujList;
	}

//	public Map<Integer, Object> getSpecificsReferents() {
//		return specificsReferents;
//	}
//
//	public void setSpecificsReferents(Map<Integer, Object> specificsReferents) {
//		this.specificsReferents = specificsReferents;
//	}

	public List<SystemClassif> getScReferentsList() {
		this.scReferentsList = new ArrayList<>();
		return scReferentsList;
	}

	public void setScReferentsList(List<SystemClassif> scReferentsList) {		
		this.scReferentsList = scReferentsList;
	}

	public LazyDataModelSQL2Array getSlujList() {
		return slujList;
	}

	public void setSlujList(LazyDataModelSQL2Array slujList) {
		this.slujList = slujList;
	}
	
	public List<SelectItem> getRegistraturiList() {
		return registraturiList;
	}

	public void setRegistraturiList(List<SelectItem> registraturiList) {
		this.registraturiList = registraturiList;
	}

	public Integer getCodeRef() {
		return codeRef;
	}

	public void setCodeRef(Integer codeRef) {
		this.codeRef = codeRef;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}
	
	public List<Integer> getUsersRegList() {
		return usersRegList;
	}

	public void setUsersRegList(List<Integer> usersRegList) {
		this.usersRegList = usersRegList;
	}
	
	/******************************** EXPORTS **********************************/
	
	public boolean isFromSave() {
		return fromSave;
	}

	public void setFromSave(boolean fromSave) {
		this.fromSave = fromSave;
	}

	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за групите служители
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "regGrSluj.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, null, null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за групите служители
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "regGrSluj.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title, null, null, null);		
						
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
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за служители в групата
	 */
	public void postProcessXLSInclSluj(Object document) {
		
		String title = getMessageResourceString(LABELS, "regGrInclSluj.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport(), null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за служители в групата
	 */
	public void preProcessPDFInclSluj(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "regGrInclSluj.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title, dopInfoReport(), null, null);		
						
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(),e);			
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);			
		} 
	}
	
	/**
	 * подзаглавие за екпсорта - дали да остане???
	 */
	public Object[] dopInfoReport() {
		Object[] dopInf = null;
		dopInf = new Object[2];
	
		dopInf[0] = this.groupSluj.getGroupName();
	
		return dopInf;
	}
	
}
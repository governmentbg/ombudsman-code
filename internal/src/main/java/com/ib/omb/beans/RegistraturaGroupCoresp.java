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
public class RegistraturaGroupCoresp extends IndexUIbean implements Serializable {
	
	/**
	 * Въвеждане / актуализация на групи кореспонденти
	 * 
	 */
	private static final long serialVersionUID = -7096159358940586900L;
	private static final Logger LOGGER = LoggerFactory.getLogger(RegistraturaGroupCoresp.class);
	
	private static final String FORM_REG_GROUP_CORESP = "formRegGroupCoresp";
	private static final String ERROR_KLASSIF = "general.errorClassif";

	private RegistraturaGroup groupCoresp;
	private transient RegistraturaGroupDAO regGrDAO;
	private Integer idReg;
	private List<RegistraturaGroup> regGrCorespList;

	private LazyDataModelSQL2Array correspList;
	private Integer codeCorresp;
	private String txtCorresp;
	
	private List<SystemClassif>	scGrFromSEOSList = new ArrayList<>();
	private LazyDataModelSQL2Array uchFromSEOSList;

	private int countryBG; // ще се инициализира в инита през системна настройка: delo.countryBG
	
	private List<SelectItem> registraturiList;	
	private Integer codeRef;
	private String name;
	private String info;
	private List<Integer> usersRegList;
	
	private List<SelectItem> grTypesList;
	private Integer grType;
	
	private boolean fromSave;

	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {

		LOGGER.debug("PostConstruct!!!");

		try {
			
			this.countryBG = Integer.parseInt(getSystemData().getSettingsValue("delo.countryBG"));
		
		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на код на държава България от настройка: delo.countryBG", e);
		}

		this.regGrDAO = new RegistraturaGroupDAO(getUserData());

		this.idReg = getUserData(UserData.class).getRegistratura();
		this.regGrCorespList = new ArrayList<>();	
		
		this.registraturiList = new ArrayList<>();
		this.codeRef = null;
		this.name = null;
		this.info = null;
		this.usersRegList = new ArrayList<>();
		this.grTypesList = new ArrayList<>();
		this.fromSave = false;		
		
		try {
			
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
			
			List<SystemClassif> itemsGrType = getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_REGISTRATURA_GROUP_TYPE, new Date(), getCurrentLang());
			
			for (SystemClassif item : itemsGrType) {
				if (item.getCode() == OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP || item.getCode() == OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS) {
					this.grTypesList.add(new SelectItem(item.getCode(), item.getTekst()));
				}								
			}			
		
		} catch (DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		
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
			
			List<Integer> groupTypeList = new ArrayList<>();
			if (this.grType != null) {				
				groupTypeList.add(this.grType);
			} else {
				groupTypeList.add(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP);
				groupTypeList.add(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS);
			}
			
			JPA.getUtil().runWithClose(() -> this.regGrCorespList = this.regGrDAO.findByRegistraturaId(this.usersRegList, groupTypeList, this.codeRef, this.name, this.info));
			
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
		
		this.regGrCorespList.clear();
		this.usersRegList = new ArrayList<>();
		
		if (!this.fromSave) {
			this.groupCoresp = new RegistraturaGroup();
			this.groupCoresp = null;
		}
	}

	public void actionEdit(Integer idObj) {

		try {

			if (idObj != null) {

				JPA.getUtil().runWithClose(() -> this.groupCoresp = this.regGrDAO.findById(idObj));
				
				this.scGrFromSEOSList = new ArrayList<>();
				
				List<SystemClassif> tmpLst = new ArrayList<>();
				
				if(this.groupCoresp.getReferentIds() != null && !this.groupCoresp.getReferentIds().isEmpty()) {
					
					for (Integer item : this.groupCoresp.getReferentIds()) {
						
						String tekst = "";
						SystemClassif scItem = new SystemClassif();
						
						try {
							
							scItem.setCodeClassif(OmbConstants.CODE_CLASSIF_EGOV_ORGANISATIONS);
							scItem.setCode(item);
							tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_EGOV_ORGANISATIONS, item, getUserData().getCurrentLang(), new Date());		
							scItem.setTekst(tekst);
							tmpLst.add(scItem);
						
						} catch (DbErrorException e) {
							LOGGER.error("Грешка при зареждане на включени служители! ", e);
						}						
					}
				}
				
				setScGrFromSEOSList(tmpLst); 
				
				actionSearch(idObj);
			}

		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на регистър! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}

	}

	public void actionNew() {

		this.groupCoresp = new RegistraturaGroup();
		this.groupCoresp.setGroupType(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP); 
		this.correspList = null;
	}
	
	public void actionChangeGroupType() {
		
		Integer typeGroup = groupCoresp.getGroupType();
		
		this.groupCoresp = new RegistraturaGroup();
		this.correspList = null;
		
		if (typeGroup.equals(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP)) {
			this.groupCoresp.setGroupType(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP);			
		
		} else if (typeGroup.equals(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS)) {
			this.groupCoresp.setGroupType(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS);			
		}		
		
	}
	
	public void actionSearch(Integer groupId) {
		
		SelectMetadata smd = this.regGrDAO.findReferentsByIdGroup(groupId, this.groupCoresp.getGroupType(), getUserData());
		String defaultSortColumn = "A0";
		
		if (this.groupCoresp.getGroupType().equals(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP)) {
			this.correspList = new LazyDataModelSQL2Array(smd, defaultSortColumn);		
		
		} else if (this.groupCoresp.getGroupType().equals(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS)) {		
			this.uchFromSEOSList = new LazyDataModelSQL2Array(smd, defaultSortColumn);		
		}
	}

	private boolean checkData() {

		boolean save = false;

		if (this.groupCoresp.getGroupName() == null || this.groupCoresp.getGroupName().isEmpty()) {
			JSFUtils.addMessage(FORM_REG_GROUP_CORESP + ":naimGr", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString("labels", "regGrSluj.naimGr")));
			save = true;
		}
		
//		if(this.groupCoresp.getRegistraturaId() == null) {
//			JSFUtils.addMessage(FORM_REG_GROUP_CORESP + ":idRegGr", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString("labels", "regData.registratura")));
//			save = true;
//		}

//		if (this.groupCoresp.getReferentIds() == null || this.groupCoresp.getReferentIds().isEmpty()) {
//			JSFUtils.addMessage("formRegGroupCoresp:refList:autoComplM", FacesMessage.SEVERITY_ERROR, getMessageResourceString("ui_beanMessages", "general.pleaseInsert", getMessageResourceString("labels", "regGrSluj.inclSluj")));
//			save = true;
//		}
		
		if (save) {
			PrimeFaces.current().executeScript("scrollToErrors()");
		}

		return save;
	}

	public void actionSave() {

		if (checkData()) {
			return;
		}

		try {
			
			this.groupCoresp.setRegistraturaId(this.idReg);
			
//			this.groupCoresp.setGroupType(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP);

			JPA.getUtil().runInTransaction(() -> this.groupCoresp = this.regGrDAO.save(this.groupCoresp));

			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_GROUP_CORRESP, false, false);
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

//			actionNew();
			
			this.fromSave = true;

			actionClear();
			
			JPA.getUtil().runWithClose(() -> this.regGrCorespList = this.regGrDAO.findByRegistraturaId(this.usersRegList, Arrays.asList(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP, OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS), codeRef, name, info));

			PrimeFaces.current().executeScript("scrollToErrors()");
			
		} catch (BaseException e) {
			LOGGER.error("Грешка при запис на групи кореспонденти! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}

	public void actionDelete() {

		try {

			JPA.getUtil().runInTransaction(() -> this.regGrDAO.delete(this.groupCoresp));

			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_GROUP_CORRESP, false, false);
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));

			actionNew();

			actionClear();
			
			JPA.getUtil().runWithClose(() -> this.regGrCorespList = this.regGrDAO.findByRegistraturaId(this.usersRegList, Arrays.asList(OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP, OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS), codeRef, name, info));

			PrimeFaces.current().executeScript("scrollToErrors()");
			
		} catch (BaseException e) {
			LOGGER.error("Грешка при изтриване на групи кореспонденти! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}

	public void actionDeleteCorr(Integer idObj) {

		try {

			if (idObj != null) {

				JPA.getUtil().runInTransaction(() -> this.regGrDAO.deleteRegistraturaReferent(this.groupCoresp, idObj));
				
				getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_GROUP_CORRESP, false, false);
				
				actionSearch(this.groupCoresp.getId());
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			}

		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на регистър! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}

	}
	
	public void actionSelectCorresp() {
		
		List<Integer> correspListTemp = new ArrayList<>();

		if (this.codeCorresp != null) {
			
			correspListTemp.add(codeCorresp);

			try {

				JPA.getUtil().runInTransaction(() -> this.regGrDAO.saveRegistraturaReferent(this.groupCoresp, correspListTemp, getSystemData()));
				
				getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_GROUP_CORRESP, false, false);
				
				actionSearch(this.groupCoresp.getId());
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));

			} catch (ObjectInUseException e) {
				LOGGER.error(e.getMessage());
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
			} catch (BaseException e) {
				LOGGER.error("Грешка при запис на кореспондент към групата! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}

		this.txtCorresp = null;
		this.codeCorresp = null;
	}
	
	public void actionDeleteUchFromSEOS(Integer idObj) {

		try {

			if (idObj != null) {

				JPA.getUtil().runInTransaction(() -> this.regGrDAO.deleteRegistraturaReferent(this.groupCoresp, idObj));
				
				getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_GROUP_CORRESP, false, false);
				
				actionSearch(this.groupCoresp.getId());
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			}

		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на регистър! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}

	}
	
	public void actionSelectFromSEOS() {

		if (this.groupCoresp.getReferentIds() != null) {
				
			try {		

				JPA.getUtil().runInTransaction(() -> this.regGrDAO.saveRegistraturaReferent(this.groupCoresp, this.groupCoresp.getReferentIds(), getSystemData()));

				getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_GROUP_CORRESP, false, false);

				actionSearch(this.groupCoresp.getId());
				
				this.scGrFromSEOSList.clear();
				
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


	public RegistraturaGroup getGroupCoresp() {
		return groupCoresp;
	}

	public void setGroupCoresp(RegistraturaGroup groupCoresp) {
		this.groupCoresp = groupCoresp;
	}

	public Integer getIdReg() {
		return idReg;
	}

	public void setIdReg(Integer idReg) {
		this.idReg = idReg;
	}

	public List<RegistraturaGroup> getRegGrCorespList() {
		return regGrCorespList;
	}

	public void setRegGrCorespList(List<RegistraturaGroup> regGrCorespList) {
		this.regGrCorespList = regGrCorespList;
	}

	public LazyDataModelSQL2Array getCorrespList() {
		return correspList;
	}

	public void setCorrespList(LazyDataModelSQL2Array correspList) {
		this.correspList = correspList;
	}

	public Integer getCodeCorresp() {
		return codeCorresp;
	}

	public void setCodeCorresp(Integer codeCorresp) {
		this.codeCorresp = codeCorresp;
	}

	public String getTxtCorresp() {
		return txtCorresp;
	}

	public void setTxtCorresp(String txtCorresp) {
		this.txtCorresp = txtCorresp;
	}
	
/******************************** EXPORTS **********************************/
	
	public List<SystemClassif> getScGrFromSEOSList() {
		return scGrFromSEOSList;
	}

	public void setScGrFromSEOSList(List<SystemClassif> scGrFromSEOSList) {
		this.scGrFromSEOSList = scGrFromSEOSList;
	}

	public LazyDataModelSQL2Array getUchFromSEOSList() {
		return uchFromSEOSList;
	}

	public void setUchFromSEOSList(LazyDataModelSQL2Array uchFromSEOSList) {
		this.uchFromSEOSList = uchFromSEOSList;
	}
	
	public int getCountryBG() {
		return this.countryBG;
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
	
	public List<SelectItem> getGrTypesList() {
		return grTypesList;
	}

	public void setGrTypesList(List<SelectItem> grTypesList) {
		this.grTypesList = grTypesList;
	}

	public Integer getGrType() {
		return grType;
	}

	public void setGrType(Integer grType) {
		this.grType = grType;
	}

	public boolean isFromSave() {
		return fromSave;
	}

	public void setFromSave(boolean fromSave) {
		this.fromSave = fromSave;
	}

	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за групите кореспонденти
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "regGrCorresp.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, null, null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за групите кореспонденти
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "regGrCorresp.reportTitle");		
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
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за кореспондентите в групата
	 */
	public void postProcessXLSInclCorresp(Object document) {
		
		String title = getMessageResourceString(LABELS, "regGrInclCorresp.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport(), null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за кореспондентите в групата
	 */
	public void preProcessPDFInclCorresp(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "regGrInclCorresp.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title, dopInfoReport(), null, null);		
						
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(),e);			
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);			
		} 
	}
	

	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за служители в групата
	 */
	public void postProcessXLSInclFromSEOS(Object document) {
		
		String title = getMessageResourceString(LABELS, "regGrInclFromSEOS.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport(), null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за служители в групата
	 */
	public void preProcessPDFInclFromSEOS(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "regGrInclFromSEOS.reportTitle");		
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
	
		dopInf[0] = this.groupCoresp.getGroupName();
	
		return dopInf;
	}
	
}
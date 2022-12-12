package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.RegisterDAO;
import com.ib.omb.db.dao.RegistraturaDAO;
import com.ib.omb.db.dto.Register;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;

@Named
@ViewScoped
public class RegisterData extends IndexUIbean  implements Serializable {			
	
	/**
	 * Въвеждане / актуализация на регистри
	 * 
	 */
	private static final long serialVersionUID = 7678301480569513396L;
	private static final Logger LOGGER = LoggerFactory.getLogger(RegisterData.class);
	
	private static final String FORM_TABS_REG = "formRegistratura:tabsRegistraturi"; 
	
	private Register register = new Register();
	private transient RegisterDAO regDAO;
	private Integer idReg;
	private List<Object[]> registriList;
	private boolean showDataForReg = false;
	
	private boolean activeRegister = true;	
	private boolean commonRegister = false;
	private boolean useCommonRegisters = false;
	
	private List<SystemClassif> classifRegsList = new ArrayList<>();
	private Integer fromRegistratura;	
	private List<Object[]> copyRegistriList = new ArrayList<>();
	private List<Object[]> selectedForCopyRegList = new ArrayList<>();
	
	private List<Object[]> commonRegistriList = new ArrayList<>();
	private List<Object[]> selectedForAddCommonRegList = new ArrayList<>();
	
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct - RegisterData!!!");	
		
		try {
			
			this.regDAO = new RegisterDAO(getUserData());
			this.registriList = new ArrayList<>();
			
			this.idReg = (Integer) JSFUtils.getFlashScopeValue("idReg");
			
			if ("1".equals(getSystemData().getSettingsValue("delo.useCommonRegisters"))) {
				this.useCommonRegisters = true;
			} else {
				this.useCommonRegisters = false;
			}
			
			if (idReg != null) {
			
				JPA.getUtil().runWithClose(() -> this.registriList = this.regDAO.findByRegistraturaId(this.idReg, this.useCommonRegisters));
			}
			
			this.showDataForReg = false;			
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на регистрите! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	/** 
	 * Метод за редакция
	 * @param idObj
	 **/
	public void actionEdit(Integer idObj) {
		
		this.showDataForReg = true;
		
		try {
			
			if (idObj != null) {
		
				JPA.getUtil().runWithClose(() -> this.register = this.regDAO.findById(idObj));
				
				if(Objects.equals(this.register.getValid(), OmbConstants.CODE_ZNACHENIE_DA)) {
					this.activeRegister = true;
				
				} else {
					this.activeRegister = false;
				}
				
				if(Objects.equals(this.register.getCommon(), OmbConstants.CODE_ZNACHENIE_DA)) {
					this.commonRegister = true;
				
				} else {
					this.commonRegister = false;
				}
			}
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на регистър! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	public boolean checkData() {
		
		boolean save = false;
		
		if(this.register.getRegister() == null || this.register.getRegister().isEmpty()) {
			JSFUtils.addMessage(FORM_TABS_REG + ":naimRegister", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "register.naim")));
			save = true;
		}
		
		if(this.register.getRegisterType() == null) {
			JSFUtils.addMessage(FORM_TABS_REG + ":tipReg", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "register.tipReg")));
			save = true;
		}
		
		if(this.register.getAlg() == null) {
			JSFUtils.addMessage(FORM_TABS_REG + ":algReg", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "register.alg")));
			save = true;
		}
		
		if(this.register.getBegNomer() == null && (this.register.getAlg() != null && this.register.getAlg().equals(OmbConstants.CODE_ZNACHENIE_ALG_INDEX_STEP))) { 
			JSFUtils.addMessage(FORM_TABS_REG + ":begNum", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "register.begNum")));
			save = true;
		}
		
		if(this.register.getActNomer() == null && (this.register.getAlg() != null && this.register.getAlg().equals(OmbConstants.CODE_ZNACHENIE_ALG_INDEX_STEP))) {
			JSFUtils.addMessage(FORM_TABS_REG + ":actNum", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "register.actNum")));
			save = true;
		}
		
		if(this.register.getStep() == null && (this.register.getAlg() != null && this.register.getAlg().equals(OmbConstants.CODE_ZNACHENIE_ALG_INDEX_STEP))) {
			JSFUtils.addMessage(FORM_TABS_REG + ":step", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "register.step")));
			save = true;
		}		
		
		return save;		
	}
	
	/** 
	 * Зачистване на параметрите
	 * 
	 **/
	public void actionNew() {	
		
		this.showDataForReg = true;
		this.register = new Register();
		this.register.setRegistraturaId(this.idReg); 
		
	}
	
	/** 
	 * Метод за запис
	 * 
	 **/
	public void actionSave() {
		
		if(checkData()) {
			return;
		}
		
		try {
			
			if (activeRegister) {
				this.register.setValid(Constants.CODE_ZNACHENIE_DA);
			} else {
				this.register.setValid(Constants.CODE_ZNACHENIE_NE);
			}
			
			if (commonRegister) {
				this.register.setCommon(Constants.CODE_ZNACHENIE_DA);
			} else {
				this.register.setCommon(Constants.CODE_ZNACHENIE_NE);
			}		
		
			JPA.getUtil().runInTransaction(() -> this.register = this.regDAO.save(this.register, null));
			
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRI, false, false);
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRI_SORTED, false, false);
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRATURI, false, false);
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.succesSaveMsg"));
			
//			actionNew();

		    JPA.getUtil().runWithClose(() -> this.registriList = this.regDAO.findByRegistraturaId(this.idReg, this.useCommonRegisters));
		    
		    // достъпвам таба с характеристики на документи, за да променя списъка с регистрите, тъй като няма да мине през initData()!	
		    JSFUtils.addFlashScopeValue("idReg", this.idReg);
	    	DocVidSettings dvSett = (DocVidSettings) JSFUtils.getManagedBean("docVidSettings");
		    
//	    	if (dvSett.getRegistriList() != null && !dvSett.getRegistriList().isEmpty()) {		    		
	    		dvSett.initData();
	    		dvSett.setRegistriList(new ArrayList<>());
				dvSett.setRegistriList(dvSett.registriListByIdReg(this.idReg));				
//			}
		
		} catch (ObjectInUseException e) {
			LOGGER.error("Грешка при запис на регистър - {}", e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на регистър! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	/** 
	 * Изтриване на регистър
	 * 
	 **/
	public void actionDelete() {
		
		try {
			
			JPA.getUtil().runInTransaction(() -> this.regDAO.delete(this.idReg, this.register, getSystemData()));
		
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRI, false, false);
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRI_SORTED, false, false);
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRATURI, false, false);
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			
			actionNew();	
			
			JPA.getUtil().runWithClose(() -> this.registriList = this.regDAO.findByRegistraturaId(this.idReg, this.useCommonRegisters));
			
			// достъпвам таба с характеристики на документи, за да променя списъка с регистрите, тъй като няма да мине през initData()!
			DocVidSettings dvSett = (DocVidSettings) JSFUtils.getManagedBean("docVidSettings");
		    
			if (dvSett.getRegistriList() != null && !dvSett.getRegistriList().isEmpty()) {		    	
				dvSett.setRegistriList(new ArrayList<>());
				dvSett.setRegistriList(dvSett.registriListByIdReg(this.idReg));
			}
		    
		} catch (ObjectInUseException e) {
			LOGGER.error(e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());		
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на регистър! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	/** 
	 * Метод за намиране на всички реистратури
	 * 
	 **/
	public void actionFindAllRegistraturi() {
		
		try {
			
			List<Integer> notInCodes = new ArrayList<>();			
			notInCodes.add(this.idReg);		
		
			this.classifRegsList = getSystemData().queryClassification(OmbConstants.CODE_CLASSIF_REGISTRATURI, null, new Date(), getCurrentLang(), notInCodes);
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на списъка с регистратурите без текущата регистратура!! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	/** 
	 * Зареждане на списък с регистри на друга регистратура
	 * 
	 **/
	public void actionFindRegistersForCopy() {
		
		this.copyRegistriList = new ArrayList<>();
		this.selectedForCopyRegList = new ArrayList<>();
		
		if (this.fromRegistratura != null) {
			
			try {
				
				if (!this.fromRegistratura.equals(this.idReg)) {
					JPA.getUtil().runWithClose(() -> this.copyRegistriList = this.regDAO.findRegistersCopy(this.fromRegistratura, this.idReg, this.useCommonRegisters));	
				
//				} else {
//					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString("beanMessages", "register.registersFromSameReg"));			 	
				}			
			
			} catch (BaseException e) {
				LOGGER.error("Грешка при зареждане списъка с регистрите на друга регистратура! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}
			
		}
		
	}
	
	/** 
	 * Копиране на регистри
	 * 
	 **/
	public void actionCopyRegisters() {
		
		try {
		
			if (!this.selectedForCopyRegList.isEmpty()) {
				
				List<Object[]> tmpList = new ArrayList<>();
				
				for (Object[] reg : this.registriList) {
					
					for (Object[] selReg : this.selectedForCopyRegList) {
						
						if (reg[2].equals(selReg[2])) {
							tmpList.add(selReg);						
						}						
					}
				}
				
				for (Object[] tmp : tmpList) {
					this.selectedForCopyRegList.remove(tmp);				
				}
				
				if (!this.selectedForCopyRegList.isEmpty()) { 
					
					JPA.getUtil().runInTransaction(() -> new RegistraturaDAO(getUserData()).copyRegisters(this.idReg, this.selectedForCopyRegList, getSystemData()));
					
					getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRI, false, false);
					getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRI_SORTED, false, false);
					getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRATURI, false, false);

					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "register.succesCopyMsg"));	
					
					actionNew();

				    JPA.getUtil().runWithClose(() -> this.registriList = this.regDAO.findByRegistraturaId(this.idReg, this.useCommonRegisters));
				    
				    String dialogWidgetVar = "PF('dlgCopyReg').hide();";
					PrimeFaces.current().executeScript(dialogWidgetVar);
				
//				} else {
//					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString("beanMessages", "register.existRegForCopy"));						
				}
			
			} else {				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "register.selectOneRegCopy"));	
			}
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при копиране на регистри! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	
	}
	
	public void actionFindCommonRegisters() {
		
		this.commonRegistriList = new ArrayList<>();
		this.selectedForAddCommonRegList = new ArrayList<>();
			
		try {			
			
			JPA.getUtil().runWithClose(() -> this.commonRegistriList = this.regDAO.findRegistersCommon(this.idReg));		
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане списъка с общите регистри! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	/** 
	 * Добавяне на общ регистър
	 * 
	 **/
	public void actionAddCommonRegisters() {
		
		try {
		
			if (!this.selectedForAddCommonRegList.isEmpty()) {
				
				List<Object[]> tmpList = new ArrayList<>();
				
				for (Object[] reg : this.registriList) {
					
					for (Object[] selReg : this.selectedForAddCommonRegList) {
						
						if (reg[2].equals(selReg[2])) {
							tmpList.add(selReg);						
						}						
					}
				}
				
				for (Object[] tmp : tmpList) {
					this.selectedForAddCommonRegList.remove(tmp);				
				}
				
				if (!this.selectedForAddCommonRegList.isEmpty()) { 
				
					JPA.getUtil().runInTransaction(() -> new RegistraturaDAO(getUserData()).copyRegisters(this.idReg, this.selectedForAddCommonRegList, getSystemData()));
					
					getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRI, false, false);
					getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRI_SORTED, false, false);
					getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRATURI, false, false);

					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "register.succesCopyMsg"));	
					
					actionNew();
	
				    JPA.getUtil().runWithClose(() -> this.registriList = this.regDAO.findByRegistraturaId(this.idReg, this.useCommonRegisters));
				    
				    String dialogWidgetVar = "PF('dlgAddCommonReg').hide();";
					PrimeFaces.current().executeScript(dialogWidgetVar);
				
//				} else {
//					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString("beanMessages", "register.existCommonReg"));	
				}
			
			} else {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "register.selectOneCommonReg"));					
			}
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при добавяне на общ регистър! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	
	}
	
	public void actionClearField() {
		
		//this.register.setSortNomer(null);
		this.register.setPrefix(null);
		this.register.setBegNomer(1);
		this.register.setActNomer(1);
		this.register.setStep(1);			
	}

	public Register getRegister() {
		return register;
	}

	public void setRegister(Register register) {
		this.register = register;
	}

	public Integer getIdReg() {
		return idReg;
	}

	public void setIdReg(Integer idReg) {
		this.idReg = idReg;
	}

	public List<Object[]> getRegistriList() {
		return registriList;
	}

	public void setRegistriList(List<Object[]> registriList) {
		this.registriList = registriList;
	}

	public boolean isShowDataForReg() {
		return showDataForReg;
	}

	public void setShowDataForReg(boolean showDataForReg) {
		this.showDataForReg = showDataForReg;
	}

	public boolean isActiveRegister() {
		return activeRegister;
	}

	public void setActiveRegister(boolean activeRegister) {
		this.activeRegister = activeRegister;
	}

	public boolean isCommonRegister() {
		return commonRegister;
	}

	public void setCommonRegister(boolean commonRegister) {
		this.commonRegister = commonRegister;
	}

	public boolean isUseCommonRegisters() {
		return useCommonRegisters;
	}

	public void setUseCommonRegisters(boolean useCommonRegisters) {
		this.useCommonRegisters = useCommonRegisters;
	}

	public List<SystemClassif> getClassifRegsList() {
		return classifRegsList;
	}

	public void setClassifRegsList(List<SystemClassif> classifRegsList) {
		this.classifRegsList = classifRegsList;
	}

	public Integer getFromRegistratura() {
		return fromRegistratura;
	}

	public void setFromRegistratura(Integer fromRegistratura) {
		this.fromRegistratura = fromRegistratura;
	}

	public List<Object[]> getCopyRegistriList() {
		return copyRegistriList;
	}

	public void setCopyRegistriList(List<Object[]> copyRegistriList) {
		this.copyRegistriList = copyRegistriList;
	}

	public List<Object[]> getSelectedForCopyRegList() {
		return selectedForCopyRegList;
	}

	public void setSelectedForCopyRegList(List<Object[]> selectedForCopyRegList) {
		this.selectedForCopyRegList = selectedForCopyRegList;
	}

	public List<Object[]> getCommonRegistriList() {
		return commonRegistriList;
	}

	public void setCommonRegistriList(List<Object[]> commonRegistriList) {
		this.commonRegistriList = commonRegistriList;
	}

	public List<Object[]> getSelectedForAddCommonRegList() {
		return selectedForAddCommonRegList;
	}

	public void setSelectedForAddCommonRegList(List<Object[]> selectedForAddCommonRegList) {
		this.selectedForAddCommonRegList = selectedForAddCommonRegList;
	}
	
/******************************** EXPORTS **********************************/
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за регистрите
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "register.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, null, null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за регистрите
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "register.reportTitle");		
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

}
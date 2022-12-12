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
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.RegistraturaDAO;
import com.ib.omb.db.dto.RegistraturaSetting;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;

@Named
@ViewScoped
public class RegistraturaSettings extends IndexUIbean implements Serializable {

	/**
	 * Въвеждане / актуализация на настройки на регистратурата
	 * 
	 */
	private static final long serialVersionUID = -1783904759110160518L;
	private static final Logger LOGGER = LoggerFactory.getLogger(RegistraturaSettings.class);
	
	private Integer idReg;
	private transient RegistraturaDAO regDAO;
	private List<RegistraturaSetting> settingsList;
	
	private List<SystemClassif> classifRegsList = new ArrayList<>();
	private Integer fromRegistratura;
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct - RegistraturaSettings!!!");	
		
		try {
			
			this.regDAO = new RegistraturaDAO(getUserData());
			
			this.idReg = (Integer) JSFUtils.getFlashScopeValue("idReg");

			JPA.getUtil().runWithClose(() -> this.settingsList = this.regDAO.findRegistraturaSettings(this.idReg, getSystemData())); 
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на настройките! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	public void actionSave() {	
		
		try {
			
			JPA.getUtil().runInTransaction(() -> this.regDAO.saveRegistraturaSettings(this.settingsList));

			getSystemData().reloadSystemOptions(); // настройките на регистратура се подвизават при системните настройки
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString("ui_beanMessages", "general.succesSaveMsg"));
			
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на настройки на регистратура! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	
		
	}
	
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
	
	public void actionCopySettings() {	
		
		if (this.fromRegistratura != null) {
			
			try {
				
				JPA.getUtil().runInTransaction(() -> {
				List<RegistraturaSetting> otherList = this.regDAO.findRegistraturaSettings(this.fromRegistratura, getSystemData());
				
				// за да се сетне стойността от другата регистратура. търсенето е по код на настройка
				for(RegistraturaSetting current : this.settingsList) {
					for(RegistraturaSetting other : otherList) {
						
						if (Objects.equals(current.getSettingCode(), other.getSettingCode())) {
							current.setSettingValue(other.getSettingValue());
							break;
						}
					}	
				}
					this.regDAO.saveRegistraturaSettings(this.settingsList);
				});
	
				getSystemData().reloadSystemOptions(); // настройките на регистратура се подвизават при системните настройки
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString("beanMessages", "regSettings.succesCopyMsg"));
				
				String dialogWidgetVar = "PF('dlgCopySet').hide();";
				PrimeFaces.current().executeScript(dialogWidgetVar);		
			
			} catch (BaseException e) {
				LOGGER.error("Грешка при копиране на настройките! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}			
		}
	}

	public Integer getIdReg() {
		return idReg;
	}

	public List<RegistraturaSetting> getSettingsList() {
		return settingsList;
	}

	public List<SystemClassif> getClassifRegsList() {
		return classifRegsList;
	}

	public Integer getFromRegistratura() {
		return fromRegistratura;
	}

	public void setFromRegistratura(Integer fromRegistratura) {
		this.fromRegistratura = fromRegistratura;
	}
	
/******************************** EXPORTS **********************************/
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за настройките
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "regSettings.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, null, null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за настройките
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "regSettings.reportTitle");		
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
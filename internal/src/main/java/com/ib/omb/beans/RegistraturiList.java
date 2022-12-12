package com.ib.omb.beans;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.system.IndexUIbean;
import com.ib.omb.db.dao.RegistraturaDAO;
import com.ib.omb.db.dto.Registratura;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;

@Named
@ViewScoped
public class RegistraturiList  extends IndexUIbean   {

	/**
	 * Списък с регистратури
	 * 
	 */
	private static final long serialVersionUID = -3935969879619314264L;
	private static final Logger LOGGER = LoggerFactory.getLogger(RegistraturiList.class);
	
	private List<Registratura> regsList;
	
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	void initData() {
		
		LOGGER.debug("PostConstruct!!!");
	
		try {
			
			JPA.getUtil().runWithClose(() -> this.regsList = new RegistraturaDAO(getUserData()).findAllSelected());
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на списъка с регистратури! ", e);		
		}
	}
	
	public String actionGoto(Integer idObj) {
		
		return "registraturaEdit.jsf?faces-redirect=true&idObj=" + idObj;
	}
	
	public String actionGotoNew() {
		
		return "registraturaEdit.jsf?faces-redirect=true";
	}

	public List<Registratura> getRegsList() {
		return regsList;
	}
	
/******************************** EXPORTS **********************************/
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за регистратурите
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "regList.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, null, null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за регистратурите
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "regList.reportTitle");		
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
package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import com.ib.omb.search.ExtendedUserSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.utils.SearchUtils;


/**
 *  Справка за предоставени права
 */
@Named
@ViewScoped
public class GrantedRightsReport extends IndexUIbean  implements Serializable {


	private static final long serialVersionUID = 7041611359833487296L;

	private static final Logger LOGGER = LoggerFactory.getLogger(GrantedRightsReport.class);
	

	private Date decodeDate;
	private ExtendedUserSearch userSearch;
	private List<Object[]> rightsList = new ArrayList<>();
	private List<SystemClassif> selectedEmplClassif = new ArrayList<>();
	private LazyDataModelSQL2Array grantedRightsList;  
	private boolean onlyNotGiven = false;

	@PostConstruct
	void initData() {
				
		userSearch = new ExtendedUserSearch();
		decodeDate = new Date();
		try {
			Object [] item = new Object [6];
			item[0] = OmbConstants.CODE_CLASSIF_BUSINESS_ROLE; //кода
			item[1] = new ArrayList<>(); //избрани предоставени права
			item[2] = new ArrayList<>(); // избрани непредоставени
			item[3] = new ArrayList<>(); // предоставени SystemClassif заради autocomple
			item[4] = new ArrayList<>(); // непредоставени SystemClassif заради autocomple
			item[5] = getSystemData().getNameClassification(OmbConstants.CODE_CLASSIF_BUSINESS_ROLE,getCurrentLang());	
			rightsList.add(item);
			
//			item = new Object [6];
//			item[0] = OmbConstants.CODE_CLASSIF_REGISTRATURI;
//			item[1] = new ArrayList<>();
//			item[2] = new ArrayList<>();
//			item[3] = new ArrayList<>();		
//			item[4] = new ArrayList<>();
//			item[5] = getSystemData().getNameClassification(OmbConstants.CODE_CLASSIF_REGISTRATURI,getCurrentLang());
//			rightsList.add(item);
			
			item = new Object [6];
			item[0] = OmbConstants.CODE_CLASSIF_REGISTRI;
			item[1] = new ArrayList<>();
			item[2] = new ArrayList<>();
			item[3] = new ArrayList<>();
			item[4] = new ArrayList<>();
			item[5] = getSystemData().getNameClassification(OmbConstants.CODE_CLASSIF_REGISTRI,getCurrentLang());
			rightsList.add(item);
			
			
			String accessControlList = getSystemData().getSettingsValue("system.classificationsForAccessControl");
			if (accessControlList != null) { 
			
				String[] classifIDs = accessControlList.split(",");
				for (String id : classifIDs) {
					
					Integer codeClassif = Integer.valueOf(id);				
					item = new Object [6];
					item[0] = codeClassif;
					item[1] = new ArrayList<>();
					item[2] = new ArrayList<>();
					item[3] = new ArrayList<>();
					item[4] = new ArrayList<>();
					item[5] =getSystemData().getNameClassification(codeClassif, getCurrentLang());
					rightsList.add(item);
				}
			}
			
			item = new Object [6];
			item[0] = OmbConstants.CODE_CLASSIF_USER_SETTINGS;
			item[1] = new ArrayList<>();
			item[2] = new ArrayList<>();
			item[3] = new ArrayList<>();
			item[4] = new ArrayList<>();
			item[5] = getSystemData().getNameClassification(OmbConstants.CODE_CLASSIF_USER_SETTINGS,getCurrentLang());
			rightsList.add(item);
			
			item = new Object [6];
			item[0] = OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE;
			item[1] = new ArrayList<>();
			item[2] = new ArrayList<>();
			item[3] = new ArrayList<>();
			item[4] = new ArrayList<>();
			item[5] = getSystemData().getNameClassification(OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE,getCurrentLang());	
			rightsList.add(item);
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при ! ", e);
		}
		
	}


	/**
	 *  Метод за търсене
	 */
	public void actionSearch() {
		userSearch.setGiven(new HashMap<>());
		userSearch.setNotGiven(new HashMap<>());
		for(Object [] item: rightsList) {
				List <Integer> givenList = (List<Integer>) item[1];	
				if(givenList!=null && !givenList.isEmpty()) {
					userSearch.getGiven().put(SearchUtils.asInteger(item[0]),givenList);
				}
			
			
				List <Integer> notGivenList = (List<Integer>) item[2];	
				if(notGivenList!=null && !notGivenList.isEmpty()) {				
				userSearch.getNotGiven().put(SearchUtils.asInteger(item[0]),notGivenList);
			}
		}
		
		if((userSearch.getCodeRefList()==null || userSearch.getCodeRefList().isEmpty()) && userSearch.getGiven().isEmpty() && userSearch.getNotGiven().isEmpty()){
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "general.criteriiMs"));
			grantedRightsList = null;
			return;
		}
		
		if(userSearch.getGiven().isEmpty() && !userSearch.getNotGiven().isEmpty()) { //само недадени или недадени и служител
			try {
				onlyNotGiven = true;
				userSearch.buildQueryGrantedRights2(getSystemData());
				grantedRightsList = new LazyDataModelSQL2Array(userSearch, "a2 asc"); 
			} catch (InvalidParameterException e) {
				LOGGER.error("Невалиден параметър! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при търсене на предоставени права! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
			
		}else {
			try {
				onlyNotGiven = false;
				userSearch.buildQueryGrantedRights(getSystemData());
				grantedRightsList = new LazyDataModelSQL2Array(userSearch, "a5 asc"); 
			} catch (InvalidParameterException e) {
				LOGGER.error("Невалиден параметър! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при търсене на предоставени права! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}
	}		
	

	/**
	 *  Метод за зачистване на критериите за търсене
	 */
	public void actionClearFilter() {
		userSearch = new ExtendedUserSearch();
		selectedEmplClassif = new ArrayList<>();
		for(Object [] item: rightsList) {
			item[1] = new ArrayList<>();
			item[2] = new ArrayList<>();
			item[3] = new ArrayList<>();
			item[4] = new ArrayList<>(); 
		}
		grantedRightsList = null;
	}
	

	/**
	 *  Експорт els 
	 *  @param document
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "grantedRights.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, null , null, null);		
     
	}
	
	/**
	 *  Експорт pdf 
	 *  @param document
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "grantedRights.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title,  null, null, null);		
						
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

	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}


	public ExtendedUserSearch getUserSearch() {
		return userSearch;
	}


	public void setUserSearch(ExtendedUserSearch userSearch) {
		this.userSearch = userSearch;
	}


	public List<Object[]> getRightsList() {
		return rightsList;
	}


	public void setRightsList(List<Object[]> rightsList) {
		this.rightsList = rightsList;
	}


	public List<SystemClassif> getSelectedEmplClassif() {
		return selectedEmplClassif;
	}


	public void setSelectedEmplClassif(List<SystemClassif> selectedEmplClassif) {
		this.selectedEmplClassif = selectedEmplClassif;
	}


	public LazyDataModelSQL2Array getGrantedRightsList() {
		return grantedRightsList;
	}


	public void setGrantedRightsList(LazyDataModelSQL2Array grantedRightsList) {
		this.grantedRightsList = grantedRightsList;
	}


	public boolean isOnlyNotGiven() {
		return onlyNotGiven;
	}


	public void setOnlyNotGiven(boolean onlyNotGiven) {
		this.onlyNotGiven = onlyNotGiven;
	}


}
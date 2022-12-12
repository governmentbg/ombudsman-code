package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.utils.DocDostUtils;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.useractivity.UserActivityData;
import com.ib.system.utils.SearchUtils;

@Named
@ViewScoped
public class RedefineAccess extends IndexUIbean  implements Serializable {
	
	/**
	 * Преопределяне на достъп на служители
	 * 
	 */
	private static final long serialVersionUID = 3443639657904698838L;
	private static final Logger LOGGER = LoggerFactory.getLogger(RedefineAccess.class);
	
	private List<SystemClassif>	scReferentsList = new ArrayList<>();	
	private List<Object[]> slujList;
	private Date decodeDate = new Date();
	
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct - RedefineAccess!!!");	
		
		this.slujList = new ArrayList<>();
			
	}
	
	public void actionSelectSluj() {

		boolean existSluj = false;

		try {
			
			for (SystemClassif tmpSluj : scReferentsList) {

				if (!this.slujList.isEmpty()) {
					
					existSluj = false;

					for (Object[] refCode : this.slujList) {

						// ако този служител е избран преди това - да не се добавя пак в таблицата
						if (Integer.valueOf(tmpSluj.getCode()).equals(SearchUtils.asInteger(refCode[0]))) {
							existSluj = true;
							break;
						}
					}
				}

				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, tmpSluj.getCode(), getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (!existSluj && refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL))) {

					Object[] sluj = new Object[4];

					sluj[0] = tmpSluj.getCode();
					sluj[1] = tmpSluj.getTekst();
					sluj[2] = tmpSluj.getDopInfo();
					
					boolean existUser = getSystemData().matchClassifItems(SysConstants.CODE_CLASSIF_USERS, tmpSluj.getCode(), this.decodeDate);
					
					if (existUser) {
						sluj[3] = getSystemData().decodeItem(SysConstants.CODE_CLASSIF_USERS, tmpSluj.getCode(), getCurrentLang(), this.decodeDate);						
					} else {
						sluj[3] = "";
					}

					this.slujList.add(sluj);
				}
			}

			this.scReferentsList.clear();

		} catch (BaseException e) {
			LOGGER.error("Грешка при избиране на служители за преопределяне на достъп! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}
	
	public void actionDeleteSluj(Integer code) {
		
		List<Object[]> slujDel = new ArrayList<>();
		
		for (Object[] obj : this.slujList) {
			
			if (SearchUtils.asInteger(obj[0]).equals(code)) {
				slujDel.add(obj);
				break;
			}			
		}
		
		if(!slujDel.isEmpty()) {
			for (Object[] delSluj : slujDel) {
				this.slujList.remove(delSluj);	
			}
		}		
	}
	
	public void actionRedefineAccess() {
		
		// списък със служители, които са логнати в системата и не може да им се преопределя достъпа - трупат се, за да се изведе съобщение на екрана на кои няма да се преопредели достъп
		List<String> slujLogInSystem = new ArrayList<>();
		// мап с код на служителите и код на структурата, които остават за преопределяне на достъпа след проверка дали са логнати в системата
		Map<Integer, Integer> slujAndCodeStruc = new HashMap<>();
		
		try {
			
			if (!slujList.isEmpty()) {
			
				for (Object[] sluj : slujList) {				
					
					if (SearchUtils.asString(sluj[3]).isEmpty()) { // ако не е потребител - няма да проверявам дали е логнат в системата и се добавя за преопределяне на достъп
						
						SystemClassif sc = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_ADMIN_STR, SearchUtils.asInteger(sluj[0]), getCurrentLang(), decodeDate, false);
						slujAndCodeStruc.put(SearchUtils.asInteger(sluj[0]), Integer.valueOf(sc.getCodeParent()));
	
					} else { // ако е потребител
						
						UserActivityData uad = getSystemData().getActiveUsers().check("fakeIP", SearchUtils.asInteger(sluj[0]));
						
						if (uad == null ) { // не е логнат в системата и се добавя за преопределяне на достъп
							
							SystemClassif sc = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_ADMIN_STR, SearchUtils.asInteger(sluj[0]), getCurrentLang(), decodeDate, false);
							slujAndCodeStruc.put(SearchUtils.asInteger(sluj[0]), Integer.valueOf(sc.getCodeParent()));
							
						} else { // логнат е в системата и ще се трупа в един Стринг, за да излезе съобщение
						
							if (!slujLogInSystem.isEmpty()) {
								slujLogInSystem.add(", "); 
							}
							
							slujLogInSystem.add(SearchUtils.asString(sluj[1]));					
						}					
					}
				}
				
				if (!slujLogInSystem.isEmpty()) { // Съобщение за потребителите, които за логнати в системата и няма да им се преопределя достъпа
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString(beanMessages, "redefineAccess.notRedefineForSluj", slujLogInSystem.toString().replace("[", "").replace("]", "")));			
									
				}
				
				if (!slujAndCodeStruc.isEmpty()) {
				
					//метод на Васко за преопределяне на достъпа				
					JPA.getUtil().runInTransaction(() -> {
					
						for (Entry<Integer, Integer> sluj : slujAndCodeStruc.entrySet()) {		
								
							new DocDostUtils().recreateUserAccessList(sluj.getKey(), getSystemData(), getUserData());									
						}
					
					}); 
					
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "redefineAccess.successRedefineAccess"));					
				}
				
				this.slujList = new ArrayList<>();
				
			} else {
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "redefineAccess.choiceSluj"));
			}
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при преопределяне на достъп на служители! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	public List<SystemClassif> getScReferentsList() {
		this.scReferentsList = new ArrayList<>();
		return scReferentsList;
	}

	public void setScReferentsList(List<SystemClassif> scReferentsList) {		
		this.scReferentsList = scReferentsList;
	}

	public List<Object[]> getSlujList() {
		return slujList;
	}

	public void setSlujList(List<Object[]> slujList) {
		this.slujList = slujList;
	}
	
	public Date getDecodeDate() {
		return new Date(decodeDate.getTime()) ;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
	}	
	
	/******************************** EXPORTS **********************************/	

	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за служители в групата
	 */
	public void postProcessXLSInclSluj(Object document) {
		
		String title = getMessageResourceString(LABELS, "redefineAccess.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, null, null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за служители в групата
	 */
	public void preProcessPDFInclSluj(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "redefineAccess.reportTitle");		
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
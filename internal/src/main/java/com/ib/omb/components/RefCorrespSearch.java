package com.ib.omb.components;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;

import org.primefaces.PrimeFaces;
import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.search.ReferentSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;

/** */
@FacesComponent(value = "refCorrespSearch", createTag = true)
public class RefCorrespSearch extends UINamingContainer {
	
	private enum PropertyKeys {
		REFSEARCH, CODEREF, SHOWME, CORRESPLIST, EKATTE, SEEPERSONALDATA, KATNARSC, VIDNARSC, TIPORGANSC 
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(RefCorrespSearch.class);


	private SystemData	systemData	= null;
	private Date		dateClassif	= null;

	private int countryBG; // ще се инициализира в getter-а през системна настройка: delo.countryBG
	
	private UserData userData = null;
	
	/**
	 * Разширеното търсене - инциализира компонентата   <f:event type="preRenderComponent" listener="#{cc.initRefCorrespS(true)}" />
	 */	
	public void initRefCorrespS(boolean bb) {

		boolean modal = (Boolean) getAttributes().get("modal"); // обработката е в модален диалог (true) или не (false)
		boolean correspForget = (Boolean) getAttributes().get("correspForget");

		ReferentSearch tmpRefS1 = new ReferentSearch();

		if (!modal) {
			tmpRefS1 = getRefSearch();
		}

		ReferentSearch tmpRefS = new ReferentSearch();
		
		tmpRefS.setRefType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL);
		
		if (correspForget) {
			tmpRefS.setRefType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL); 
		}
		
		tmpRefS.setDate(getDateClassif());
		// tmpRefS.setCountry(getCountryBG());
		
		setSeePersonalData(getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_SEE_PERSONAL_DATA));
		
		if (bb) { // при отваряне на модалния
			setShowMe(true);
			String srchTxt = (String) getAttributes().get("searchTxt");
			if (srchTxt != null) {
				tmpRefS.setRefName(srchTxt);
			}
		
		} else { // при изчистване...
			ValueExpression expr2 = getValueExpression("searchTxt");
			ELContext ctx2 = getFacesContext().getELContext();
			if (expr2 != null) {
				expr2.setValue(ctx2, null);
			}

			tmpRefS1 = null;
		}

		if (tmpRefS1 == null || tmpRefS1.getRefName() == null) {

			setRefSearch(tmpRefS);
			setCorrespList(null);
		}
		
		LOGGER.debug("initRefCorrespS>>>> ");

	}
	 
   /** 
    * разширено търсене на кореспондент - изп. се само, ако е в модален прозорец
    * изивква се при затваряне на модалния прозореца (onhide) 
    * 
    */
   public void actionHideModal() {		
	   setRefSearch(null);
	   setCorrespList(null);
	   setShowMe(false);
	   LOGGER.debug("actionHideModal>>>> ");
	}

   
	
   
   
   /** 
    * разширено търсене на кореспондент -  - бутон "Търсене"
    */
   public void actionSearchCorresp() {	 	
	   try {
		   getRefSearch().calcEkatte(getSystemData());
	   } catch (DbErrorException e) {
		   LOGGER.error("Грешка при определяне на област/община", e);
	   }
	   getRefSearch().buildQuery();
	   setCorrespList(new LazyDataModelSQL2Array(getRefSearch(), "a1 desc")); 
	   LOGGER.debug("actionSearchCoresp>>>> {}",getCorrespList().getRowCount());
	}
   
   /** 
    * разширено търсене на кореспондент - бутон "Изчисти"
    */
   public void actionClearCorresp() {		
	   initRefCorrespS(false);
	   setKatNarSc(null);
	   setVidNarSc(null);
	   setTipOrganSc(null);
	   LOGGER.debug("actionClearCoresp>>>> ");
	}
   
  
   
   /**
    * Разширено търсене - избор на кореспондент
    */
   public void actionModalSelectCorr(Object[] row) {	  
	   
	    if( row != null && row[0] != null) {
	    	 LOGGER.debug("actionModalSelectCorr>>>> {}",row[0]);
		   //връща id на избрания кореспондент
		    ValueExpression expr2 = getValueExpression("codeRef");
			ELContext ctx2 = getFacesContext().getELContext();
			if (expr2 != null) {
				expr2.setValue(ctx2, Integer.valueOf(row[0].toString()));
			}	
	    }
	// извиква remoteCommnad - ако има такава....
		String remoteCommnad = (String) getAttributes().get("onComplete");
		if (remoteCommnad != null && !"".equals(remoteCommnad)) {
			PrimeFaces.current().executeScript(remoteCommnad);
		}		
   }
   
   /**
    * Актуализация на избрания кореспондент
    */
   public String actionGotoEditCorr(Object[] row) {
	   
	   boolean correspForget = (Boolean) getAttributes().get("correspForget");
	   
	   String result = "";		
	    
		if( row != null && row[0] != null) {	
			LOGGER.debug("actionGotoEditCorr >>>> {}", row[0]);
			
			if (correspForget) {
				result = "correspEditForget.jsf?faces-redirect=true&codeRef=" + row[0];
			} else {
				result = "correspEdit.jsf?faces-redirect=true&codeRef=" + row[0];
			}
		} 
		
		return result;
	}  
   
   /**
    * Въвеждане на нов кореспондент
    */
   public String actionGotoNewCorr() {
	   
	   LOGGER.debug("actionGotoNewCorr >>>> ");
		
	   return "correspEdit.jsf?faces-redirect=true";
	}  

   /**
    * Разширеното търсене - смяна на лице - физическо/юридическо
    */
   public void actionChTypRef() { 
	   getRefSearch().setEikEgn(null);	   
	   getRefSearch().setLnc(null);	
	   setCorrespList(null);  
   }

	/**
	 * Подсказка за ред от таблицата с резултати
	 * @param adrTxt
	 * @param phone
	 * @param email
	 * @return
	 */
   public String  titleContancts(String adrTxt, String phone, String email) {
	   StringBuilder title = new StringBuilder();
	   if(adrTxt != null && !adrTxt.trim().isEmpty()) {
		   title.append(adrTxt);
	   }
	   if(phone != null && !phone.trim().isEmpty()) {
		   title.append(" тел.: "+phone);
	   } 
	   if(email != null && !email.trim().isEmpty()) {
		   title.append(" e-mail: "+email);
	   }
	   return title.toString();
   }

	/** @return */
	public boolean isShowMe() {
		return (Boolean) getStateHelper().eval(PropertyKeys.SHOWME, false);
	}
	
	/** @param showMe */
	public void setShowMe(boolean showMe) {
		getStateHelper().put(PropertyKeys.SHOWME, showMe);
	}
	
	/** @return */
	public boolean isSeePersonalData() {
		return (Boolean) getStateHelper().eval(PropertyKeys.SEEPERSONALDATA, false);
	}
	
	/** @param seePersonalData */
	public void setSeePersonalData(boolean seePersonalData) {
		getStateHelper().put(PropertyKeys.SEEPERSONALDATA, seePersonalData);
	}

	public ReferentSearch getRefSearch() {
		ReferentSearch eval = (ReferentSearch) getStateHelper().eval(PropertyKeys.REFSEARCH, null);
		return eval != null ? eval : new ReferentSearch();
	}


	public void setRefSearch(ReferentSearch refSearch) {
		getStateHelper().put(PropertyKeys.REFSEARCH, refSearch);
	}



	/** @return the dateClassif */
	private Date getDateClassif() {
		if (this.dateClassif == null) {
			this.dateClassif = (Date) getAttributes().get("dateClassif");
			if (this.dateClassif == null) {
				this.dateClassif = new Date();
			}
		}
		return this.dateClassif;
	}

	/** @return */
	public Integer getLang() {
		return SysConstants.CODE_DEFAULT_LANG;
	}
	
	/** @return */
	public Date getCurrentDate() {
		return getDateClassif();
	}

	
	public LazyDataModelSQL2Array getCorrespList() {
		return (LazyDataModelSQL2Array) getStateHelper().eval(PropertyKeys.CORRESPLIST, null);
	}


	public void setCorrespList(LazyDataModelSQL2Array correspList) {
		getStateHelper().put(PropertyKeys.CORRESPLIST, correspList);
	}

	
	public Integer getCodeRefC() {
		return (Integer) getStateHelper().eval(PropertyKeys.CODEREF, null);
	}

	public void setCodeRefC(Integer codeRef) {
		getStateHelper().put(PropertyKeys.CODEREF, codeRef);
	}
	
	private SystemData getSystemData() {
		if (this.systemData == null) {
			this.systemData =  (SystemData) JSFUtils.getManagedBean("systemData");
		}
		return this.systemData;
	}
	
	public List<SystemClassif> getKatNarSc() {
		return (List<SystemClassif>) getStateHelper().eval(PropertyKeys.KATNARSC, null);
	}

	public void setKatNarSc(List<SystemClassif> katNarSc) {
		getStateHelper().put(PropertyKeys.KATNARSC, katNarSc);
	}

	public List<SystemClassif> getVidNarSc() {
		return (List<SystemClassif>) getStateHelper().eval(PropertyKeys.VIDNARSC, null);
	}

	public void setVidNarSc(List<SystemClassif> vidNarSc) {
		getStateHelper().put(PropertyKeys.VIDNARSC, vidNarSc);
	}

	public List<SystemClassif> getTipOrganSc() {
		return (List<SystemClassif>) getStateHelper().eval(PropertyKeys.TIPORGANSC, null);
	}

	public void setTipOrganSc(List<SystemClassif> tipOrganSc) {
		getStateHelper().put(PropertyKeys.TIPORGANSC, tipOrganSc);
	}

	public int getCountryBG() {
		if (this.countryBG == 0) {
			try {
				this.countryBG = Integer.parseInt(getSystemData().getSettingsValue("delo.countryBG"));
			} catch (Exception e) {
				LOGGER.error("Грешка при определяне на код на държава България от настройка: delo.countryBG", e);
			}
		}
		return this.countryBG;
	}
	
	/** @return the userData */
	private UserData getUserData() {
		if (this.userData == null) {
			this.userData = (UserData) JSFUtils.getManagedBean("userData");
		}
		return this.userData;
	}
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
	 */
	public void postProcessXLS(Object coresp) {
		new CustomExpPreProcess().postProcessXLS(coresp, "Списък кореспонденти", null , null, null);		
    }
	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
	 */
	public void preProcessPDF(Object coresp)  {
		try{
			
			new CustomExpPreProcess().preProcessPDF(coresp, "Списък кореспонденти", null , null, null);		
						
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
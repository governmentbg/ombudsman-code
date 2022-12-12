package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.omb.search.DeloDvijSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;


/** 5.9.5 	Разглеждане на движения на дела/преписка
 * @author yonchoy*/

@Named("deloPrepDvijList")
@ViewScoped
public class DeloPrepDvijList extends IndexUIbean implements Serializable {

	private static final long serialVersionUID = 4925066572517965265L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DeloPrepDvijList.class);
	
	private LazyDataModelSQL2Array docsList;      // списъка с  преписки 
	private Date currentDate;
	
	private DeloDvijSearch searchDeloPrep; // парам. за списъка док. - главните полета на документа

	private Integer periodPredavane; // период за предаване на документа
	private Integer periodStatus; //период за статус на предаване
	private Integer periodSrok;//период на срок за връщане
	private Integer periodVrastane;//период на статус на връщане
	private Integer periodPrep;//период на дата на документа
	
	private List<SystemClassif> docsRegistriClassif;
	private List<SelectItem> predavaneTypeList;
	private List<SelectItem> vrashtaneTypeList;
	
	
	//Множествен избор кореспонденти предаден на
	private List<SystemClassif> scList = new ArrayList<>();
	private List<Integer> codeRefCorresp;
	private Integer codeRefCorrSrch;
	
	//Множествен избор кореспонденти върнат на
	private List<SystemClassif> scListV = new ArrayList<>();
	private List<Integer> codeRefCorrespV;
	private Integer codeRefCorrSrchV;
	
	// Komponenti za mnovestwen izbor
	private List<SystemClassif> predadenClassif;
	private List<Integer>	predadenList;
	private List<SystemClassif> varnatClassif;
	private List<Integer>	varnatList;

	
	private List<SelectItem>	  docTypeList; //вид на документа
	private List<SelectItem>	  docStatusList; //статус на документа
	
	private boolean disableRegList = false;//дали потребителят има пълен достъп за разглеждане
	private boolean showRegistaturiList = true; //ако е само една регистратура,да не се показва
	private boolean withTome=false;
	
	private Map<Integer, Object> specificsRegister;  // само за текушата регистратура + общите
	
	//съдържа информация за модалните за допълнителна информация
	private String[] informationText = new String[2];
	private Integer idRefModal = null; //използва се в модалния за разглеждане на данни на физическо/юрид. лице
	
	
	
	/** */
	@PostConstruct
	void initData() {
	
		try {
						
			currentDate = new Date();
			clearFilter();
			//periodR = 9; // ако искаме да зададем период по подразбиране във филтъра
			//changePeriodPredavane();
			//зарежда вида документи
			docTypeList = createItemsList(false, OmbConstants.CODE_CLASSIF_DELO_TYPE, this.currentDate, true);
			docStatusList = createItemsList(false, OmbConstants.CODE_CLASSIF_DELO_STATUS, this.currentDate, true);
			//зарежда правата спрямо регистратурата
			disableRegList=getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW);
			List<SelectItem> regList = createItemsList(false, OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS, this.currentDate, true);
			
			//зарежда вида на предаване
			this.predavaneTypeList = // премахват се от списъка двете опции - имейл и сеос
			createItemsList(false, OmbConstants.CODE_CLASSIF_DVIJ_METHOD, new Date(), true)
			.stream()
			.filter(item -> (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS
							&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV
							&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_WEB_EAU
							&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL)
			.collect(Collectors.toList());
			
			
			//зарежда вида на връщане
			this.vrashtaneTypeList = // премахват се от списъка двете опции - имейл и сеос
			createItemsList(false, OmbConstants.CODE_CLASSIF_DVIJ_METHOD, new Date(), true)
			.stream()
			.filter(item -> (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS
							&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV
							&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_WEB_EAU
							&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL)
			.collect(Collectors.toList());

			if(regList==null || regList.size()<2) {
				showRegistaturiList = false;
			}
			
			this.withTome=Objects.equals(getSystemData().getSettingsValue("delo.deloWithToms"),String.valueOf(OmbConstants.CODE_ZNACHENIE_DA));

		} catch (DbErrorException e) {
			LOGGER.error("Грешка при инициализиране на филтър за търсене на документи! ", e);
		} catch (UnexpectedResultException e) {
			LOGGER.error("Грешка при инициализиране на филтър за търсене на документи! ", e);
		}
	}

	public String actionGoto( long idObj) {
		String result = "deloView.xhtml?faces-redirect=true&idObj=" + idObj;
		return result;
	}

	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClear() {
		clearFilter();
		changePeriodPredavane();
		docsList = null;
	}
	
	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionSearch() {
		//за предадени
		searchDeloPrep.setPredaden(new ArrayList<>());
		searchDeloPrep.getPredaden().addAll(predadenList);
		searchDeloPrep.getPredaden().addAll(codeRefCorresp);
		
		//за варнати
		searchDeloPrep.setVarnat(new ArrayList<>());
		searchDeloPrep.getVarnat().addAll(varnatList);
		searchDeloPrep.getVarnat().addAll(codeRefCorrespV);
		
		searchDeloPrep.buildQueryDvijPrepList(getUserData());
		docsList = new LazyDataModelSQL2Array(searchDeloPrep, "a05 desc"); 

	}
	
	public void clearFilter() {

		searchDeloPrep = new DeloDvijSearch(getUserData(UserData.class).getRegistratura());	
		specificsRegister = Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA, Optional.of(getUserData(UserData.class).getRegistratura()));
		
		//зачистване на периодите
		this.periodPredavane= null ;
		this.periodStatus= null;
		this.periodSrok= null;
		this.periodVrastane= null;
		this.periodPrep= null;
		
		scList = null;
		codeRefCorresp = new ArrayList<>();
		
		scListV = null;
		codeRefCorrespV = new ArrayList<>();
		
		//за предаден
		predadenClassif = null;
		predadenList= new ArrayList<>();
		
		//за върнат
		varnatClassif = null;
		varnatList = new ArrayList<>();
		
		// за сега стойностите по подразбиране са - вх. и собствени 
		//  Да се добавят ограничения в зависмисот от правата; в зависимост от това от къде се вика филтъра...
//		searchDeloPrep.getDocTypeArr()[0] = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN; 
//		searchDeloPrep.getDocTypeArr()[1] = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN;	
//		periodR = null;
//		periodRes = null;
//		periodAnswer = null;
//		periodAssign = null;
//		docsRegistriClassif = null;
//		docsVidClassif = null;
//		docsTaskAssignClassif = null;
//		txtCorresp = "";
	}
	
	
	public void changePeriodPredavane () {
		
    	if (this.periodPredavane != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodPredavane);
			searchDeloPrep.setDatPredavaneOt(di[0]);
			searchDeloPrep.setDatPredavaneDo(di[1]);		
    	} else {
    		searchDeloPrep.setDatPredavaneOt(null);
			searchDeloPrep.setDatPredavaneDo(null);
		}
    }
	
	public void changeDatePredavane() { 
		this.setPeriodPredavane(null);
	}
	
	

	public void changePeriodStatus () {
		
    	if (this.periodStatus != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodStatus);
			searchDeloPrep.setDatStatusOt(di[0]);
			searchDeloPrep.setDatStatusDo(di[1]);		
    	} else {
    		searchDeloPrep.setDatStatusOt(null);
			searchDeloPrep.setDatStatusDo(null);
		}
    }
	
	public void changeDateStatus() { 
		this.setPeriodStatus(null);
	}

	public void changePeriodSrok () {
		
    	if (this.periodSrok != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodSrok);
			searchDeloPrep.setDatSrokOt(di[0]);
			searchDeloPrep.setDatSrokDo(di[1]);		
    	} else {
    		searchDeloPrep.setDatSrokOt(null);
			searchDeloPrep.setDatSrokDo(null);
		}
    }
	
	public void changeDateSrok() { 
		this.setPeriodSrok(null);
	}
	
	public void changePeriodVrastane () {
		
    	if (this.periodVrastane != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodVrastane);
			searchDeloPrep.setDatVrastaneOt(di[0]);
			searchDeloPrep.setDatVrastaneDo(di[1]);		
    	} else {
    		searchDeloPrep.setDatVrastaneOt(null);
			searchDeloPrep.setDatVrastaneDo(null);
		}
    }
	
	public void changeDateVrastane() { 
		this.setPeriodVrastane(null);
	}
	
	public void changePeriodPrep () {
		
    	if (this.periodPrep != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodPrep);
			searchDeloPrep.setDatPrepOt(di[0]);
			searchDeloPrep.setDatPrepDo(di[1]);		
    	} else {
    		searchDeloPrep.setDatPrepOt(null);
			searchDeloPrep.setDatPrepDo(null);
		}
    }
	
	public void changeDateDoc() { 
		this.setPeriodPrep(null);
	}
	

	
	
	public  void actionAddSelectRef(){
		LOGGER.info("TODO - да се прехвърли избрания код в selectManyModalA");
		if(codeRefCorrSrch != null) {
		
			String tekst;
			try {
				SystemClassif tmpSc = new SystemClassif();
				tmpSc.setCodeClassif(OmbConstants.CODE_CLASSIF_REFERENTS);
				tmpSc.setCode(codeRefCorrSrch);
				tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS, codeRefCorrSrch, getUserData().getCurrentLang(), new Date());
				tmpSc.setTekst(tekst);
				scList.add(tmpSc);
				codeRefCorresp.add(codeRefCorrSrch);	
			} catch (DbErrorException e) {
				//  Auto-generated catch block
				LOGGER.error(e.getMessage(),e);	
			}
			
			codeRefCorrSrch = null;
		}
		//ид-то трябва да се смени за всяка инстанция на компонентата
		String  cmdStr = "PF('mCorrSPredadeni').hide();";
		PrimeFaces.current().executeScript(cmdStr);	
	}
	
	public  void actionAddSelectRefV(){
		LOGGER.info("TODO - да се прехвърли избрания код в selectManyModalA, actionAddSelectRefV");
		if(codeRefCorrSrchV != null) {
		
			String tekst;
			try {
				SystemClassif tmpSc = new SystemClassif();
				tmpSc.setCodeClassif(OmbConstants.CODE_CLASSIF_REFERENTS);
				tmpSc.setCode(codeRefCorrSrchV);
				tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS, codeRefCorrSrchV, getUserData().getCurrentLang(), new Date());
				tmpSc.setTekst(tekst);
				scListV.add(tmpSc);
				codeRefCorrespV.add(codeRefCorrSrchV);			
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(),e);	
			}
			codeRefCorrSrchV = null;
		}
		//ид-то трябва да се смени за всяка инстанция на компонентата
		String  cmdStr = "PF('mCorrSVarnati').hide();";
		PrimeFaces.current().executeScript(cmdStr);	
	}
	
	public void actionChangeReg() {
		
		if(searchDeloPrep.getRegistraturaId()!=null) {		
			specificsRegister = Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA,Optional.of(searchDeloPrep.getRegistraturaId()));
		}else {
			specificsRegister=null;
		}
		docsRegistriClassif = null;
	}
	
	
	/**
	 * При показване на иконки до метода на предаване
	 * @param methodCode
	 */
	public String getMethodIcon(int methodCode) {
		switch(methodCode) {
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA : return "fas fa-hand-paper";
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_POSHTA : return "fas fa-envelope";
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_FAX : return "fas fa-fax";
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL : return "fas fa-at";
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_KURIER : return "fas fa-mail-bulk";
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA : return "fas fa-file-export";
			//case OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV : return "fas fa-shield-alt";
			default : return "fas fa-location-arrow";
		}
	}
	
	/**
	 * Показва кратка версия на текстовете за информация, подобно на подсказка (tooltip)
	 */
	public String getShortInfo(String text, int lengthToShow) {
		//return (text != null)?(text.length() < lengthToShow ? text : text.substring(0, lengthToShow) + "...") : null;
		if(text != null) {
			if(text.length() < lengthToShow) return text;
			else return text.substring(0, lengthToShow) + "...";
		}
		else {
			return null;
		}
	}
	public String getInfoDateReturn(String label, Date  date) {
		SimpleDateFormat sdf= new SimpleDateFormat("dd.MM.yyyy");
		return label+" : "+sdf.format(date);
	}
	
	/*============================================================================== Exports=======================================*/
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "deloPrepDvijList.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport() , null, null);		
     
	}
	

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "deloPrepDvijList.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title,  dopInfoReport(), null, null);		
						
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(),e);			
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);			
		} 
	}
	
	/**
	 * подзаглавие за екпсорта - .............//
	 */
	public Object[] dopInfoReport() {
		Object[] dopInf = null;
		dopInf = new Object[2];
		if(searchDeloPrep.getDatPrepOt() != null && searchDeloPrep.getDatPrepDo() != null) {
			dopInf[0] = "период: "+DateUtils.printDate(searchDeloPrep.getDatPrepOt()) + " - "+ DateUtils.printDate(searchDeloPrep.getDatPrepDo());
		} 
	
		return dopInf;
	}
	
	/*============================================================================== Getters& Setters=======================================*/
	

	public LazyDataModelSQL2Array getDocsList() {
		return docsList;
	}

	public void setDocsList(LazyDataModelSQL2Array docsList) {
		this.docsList = docsList;
	}

	public DeloDvijSearch getSearchDeloPrep() {
		return searchDeloPrep;
	}

	public void setSearchDeloPrep(DeloDvijSearch searchDeloPrep) {
		this.searchDeloPrep = searchDeloPrep;
	}

	public List<SelectItem> getPredavaneTypeList() {
		return predavaneTypeList;
	}

	public void setPredavaneTypeList(List<SelectItem> predavaneTypeList) {
		this.predavaneTypeList = predavaneTypeList;
	}

	public List<SelectItem> getVrashtaneTypeList() {
		return vrashtaneTypeList;
	}


	public void setVrashtaneTypeList(List<SelectItem> vrashtaneTypeList) {
		this.vrashtaneTypeList = vrashtaneTypeList;
	}


	public Date getCurrentDate() {
		return currentDate;
	}

	public void setCurrentDate(Date currentDate) {
		this.currentDate = currentDate;
	}

	public Integer getPeriodPredavane() {
		return periodPredavane;
	}

	public void setPeriodPredavane(Integer periodPredavane) {
		this.periodPredavane = periodPredavane;
	}

	public Integer getPeriodStatus() {
		return periodStatus;
	}

	public void setPeriodStatus(Integer periodStatus) {
		this.periodStatus = periodStatus;
	}

	public Integer getPeriodSrok() {
		return periodSrok;
	}

	public void setPeriodSrok(Integer periodSrok) {
		this.periodSrok = periodSrok;
	}

	public Integer getPeriodVrastane() {
		return periodVrastane;
	}

	public void setPeriodVrastane(Integer periodVrastane) {
		this.periodVrastane = periodVrastane;
	}

	public Integer getPeriodPrep() {
		return periodPrep;
	}

	public void setPeriodPrep(Integer periodPrep) {
		this.periodPrep = periodPrep;
	}

	public List<SystemClassif> getDocsRegistriClassif() {
		return docsRegistriClassif;
	}

	public void setDocsRegistriClassif(List<SystemClassif> docsRegistriClassif) {
		this.docsRegistriClassif = docsRegistriClassif;
	}

	public List<SystemClassif> getScList() {
		return scList;
	}

	public void setScList(List<SystemClassif> scList) {
		this.scList = scList;
	}

	public List<Integer> getCodeRefCorresp() {
		return codeRefCorresp;
	}

	public void setCodeRefCorresp(List<Integer> codeRefCorresp) {
		this.codeRefCorresp = codeRefCorresp;
	}

	public Integer getCodeRefCorrSrch() {
		return codeRefCorrSrch;
	}

	public void setCodeRefCorrSrch(Integer codeRefCorrSrch) {
		this.codeRefCorrSrch = codeRefCorrSrch;
	}

	public List<SystemClassif> getScListV() {
		return scListV;
	}

	public void setScListV(List<SystemClassif> scListV) {
		this.scListV = scListV;
	}

	public List<Integer> getCodeRefCorrespV() {
		return codeRefCorrespV;
	}

	public void setCodeRefCorrespV(List<Integer> codeRefCorrespV) {
		this.codeRefCorrespV = codeRefCorrespV;
	}

	public Integer getCodeRefCorrSrchV() {
		return codeRefCorrSrchV;
	}

	public void setCodeRefCorrSrchV(Integer codeRefCorrSrchV) {
		this.codeRefCorrSrchV = codeRefCorrSrchV;
	}

	public List<SystemClassif> getPredadenClassif() {
		return predadenClassif;
	}

	public void setPredadenClassif(List<SystemClassif> predadenClassif) {
		this.predadenClassif = predadenClassif;
	}

	public List<Integer> getPredadenList() {
		return predadenList;
	}

	public void setPredadenList(List<Integer> predadenList) {
		this.predadenList = predadenList;
	}

	public List<SystemClassif> getVarnatClassif() {
		return varnatClassif;
	}

	public void setVarnatClassif(List<SystemClassif> varnatClassif) {
		this.varnatClassif = varnatClassif;
	}

	public List<Integer> getVarnatList() {
		return varnatList;
	}

	public void setVarnatList(List<Integer> varnatList) {
		this.varnatList = varnatList;
	}

	public List<SelectItem> getDocTypeList() {
		return docTypeList;
	}

	public void setDocTypeList(List<SelectItem> docTypeList) {
		this.docTypeList = docTypeList;
	}

	public List<SelectItem> getDocStatusList() {
		return docStatusList;
	}

	public void setDocStatusList(List<SelectItem> docStatusList) {
		this.docStatusList = docStatusList;
	}

	public boolean isDisableRegList() {
		return disableRegList;
	}

	public void setDisableRegList(boolean disableRegList) {
		this.disableRegList = disableRegList;
	}

	public boolean isWithTome() {
		return withTome;
	}


	public void setWithTome(boolean withTome) {
		this.withTome = withTome;
	}


	public boolean isShowRegistaturiList() {
		return showRegistaturiList;
	}

	public void setShowRegistaturiList(boolean showRegistaturiList) {
		this.showRegistaturiList = showRegistaturiList;
	}

	public Map<Integer, Object> getSpecificsRegister() {
		return specificsRegister;
	}

	public void setSpecificsRegister(Map<Integer, Object> specificsRegister) {
		this.specificsRegister = specificsRegister;
	}

	public String[] getInformationText() {
		return informationText;
	}

	public void setInformationText(String title, String conetnt) {
		this.informationText = new String[] {title, conetnt};
	}

	public Integer getIdRefModal() {
		return idRefModal;
	}

	public void setIdRefModal(Integer idRefModal) {
		this.idRefModal = idRefModal;
	}
	
	/**
     * Проверка дали бутона за разглеждане нa данни за кореспондент да се покаже -  ако е физ. лице, в екрана за разгл. са скрити всички лични данни 
	 * @param idRefForModal
	 * @return
	 */
	public boolean viewRefModal(Integer idRefForModal) {
		boolean bb = false;
		try {
			// кода на референта в от класификация на физ./юридически лица
			SystemClassif koresp = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_REFERENTS, idRefForModal, SysConstants.CODE_DEFAULT_LANG, new Date(), true);
			if(koresp != null) {
				bb = (int) koresp.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_REF_TYPE] == OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL	||
					 (int) koresp.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_REF_TYPE] == OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL;
					 //((int) koresp.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_REF_TYPE] == OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL &&	getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_SEE_PERSONAL_DATA));
			}
		} catch (DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "general.errorClassif"), e);
		}
		return bb;
	}

}

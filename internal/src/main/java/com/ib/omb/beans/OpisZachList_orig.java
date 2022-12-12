package com.ib.omb.beans;
import static com.ib.system.utils.SearchUtils.asDate;
import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.system.utils.SearchUtils.asString;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.persistence.Query;

import org.primefaces.PrimeFaces;
import org.primefaces.component.export.PDFOptions;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDvijDAO;
import com.ib.omb.db.dao.OpisDAO;
import com.ib.omb.db.dao.RegistraturaDAO;
import com.ib.omb.db.dto.DocDvij;
import com.ib.omb.db.dto.Registratura;
import com.ib.omb.print.OpisiExport;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectNotFoundException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;

@Named
@ViewScoped  
public class OpisZachList_orig extends IndexUIbean implements Serializable  {
	
	/**
	 * Списък с документи за формиране на опис със зачисление
	 * 
	 */
	private static final long serialVersionUID = 6129774408749824460L;
	private static final Logger LOGGER = LoggerFactory.getLogger(OpisZachList.class);
		
	private DocSearch searchDoc;	     // Компонент за търсене на документи -  парам. за списъка док.
	private Integer periodR;
	private Integer periodRes; //период на дата на получаване
	private Integer periodAnswer;//период на дата на очакване на отговор
	private Integer periodAssign;//период на дата на поставяне
	
	private List<SelectItem>	  calendarList;
	private List<SelectItem>	  docTypeList;
	private List<SystemClassif> docsVidClassif;  // заради autocomple (selectManyModalA)
	
	private List<Integer> docVidList = new ArrayList<Integer> ();
	private List<SystemClassif> docsRegistriClassif;
	private List<SystemClassif> docsTaskAssignClassif;
	private boolean disableRegList = false;//дали потребителят има пълен достъп за разглеждане
	private boolean showRegistaturiList = true; //ако е само една регистратура,да не се показва
	private String triCheckWork = "0"; //
	private boolean visibleCheckWork = false;
	private Map<Integer, Object> specificsRegister;  // Регистри само за текушата регистратура + общите
		
	private boolean checkUserAccess;
	private Date decodeDate=new Date();
//	private List<Integer> codesPredNaList = new ArrayList<Integer> ();
//	private List<SystemClassif> listPredNaClassif;
	
	private Integer userRegDvij;
	
	private String textPredNa = null;
	private Integer nachinPred = null;   // Начин на предаване за избрани документи
	private Integer vidSort = Integer.valueOf(0);
	
	private Integer tipPoluch = Integer.valueOf(1);    // Тип на получател за избрани документи - 1 - от Адм. структура, 2 - кореспондент
	
	private String zaglModal = null;
	
	private Integer period;	
	private Date dateFrom;
	private Date dateTo;  
	/**
	 * настройки на системата
	 */
	private boolean nastrWithEkz;    // true - ще има номера на екземпляри; false - няма номера на екземпляри
	private SystemData sd;	
	 private UserData ud;
	private Integer nomEkzPred = null;    // Номер на екземпляр за предаване
	
	// Единичен избор от Адм. структура
	private Integer codePredNa = null;
	private String namePredNa = null;
	
	// За избор на кореспондент
	// За единичен избор - при избор на документи
	private Integer codeRefCorresp = null;
	private String txtCorrespDoc;
	private String nameCorrespDoc = null;
	
	// За единичен избор - при задаване на получател
	private Integer codeRefCorrespPred = null;
	private String txtCorrespPred;
	private String nameCorrespPred;
	
	
	
		
	private Integer idUser = null;
	private Integer registrPotreb = null;
	private String nameRegistrPotreb = null;
	
	private List<Object[]> docSelectedAllModal=new ArrayList<Object[]>();   // Тук остават селектираните документи от модалния прозорец, които се прехвърлят в  docSelectedAllM
	private List<Object[]> docSelectedTmpModal=new ArrayList<Object[]>();
	private List<Object[]> docSelectedAll = new ArrayList<Object[]>();      // Общо събраните документи в основния екран
	private List<Object[]> docSelectedAllM=new ArrayList<Object[]>();   // Тук остават селектираните документи за едно селектиране - за една група 
	private List<Object[]> docSelectedTmp=new ArrayList<Object[]>();
	private List<Object[]> docSelectedAllMOpis=new ArrayList<Object[]>(); // Тук са всички групи селектирани за описа
	private List<Object[]> docSelectedAllMOpisT=new ArrayList<Object[]>();   // docSelectedAllMOpis с допълнени текстове за тип док. и вид док.
	private HashMap<Integer, Boolean> mapObrId= new HashMap<Integer, Boolean> ();
	private boolean prClearLists = false;     // true - когато поредната извадка с документи е зачислена - формирани са движения; false - има само формирана извадка в this.docSelectedAllMOpisT
	
	private LazyDataModelSQL2Array docsList;             // Избрани документи в модален панел за избор на документи при един пас
	private LazyDataModelSQL2Array docsSelList;          // Събрани всички избрани документи в основния екран
	
	 private  SelectMetadata smdDocsList = null;          // Формиран smd за docsList при последния избор на предадени документи
	
	private transient DocDvij dvijDoc;
	private String labels = "labels";
	private String beanmess = "beanMessages";
	private String sortA5A0 = "A5,A0";
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	void initData() {
		
		LOGGER.debug("PostConstruct!!!");
		this.sd = (SystemData) getSystemData();
		this.ud = getUserData(UserData.class);					
	        
	// общосистемни настройки - Дали системата обработва екземпляри
		allSystemSettings();
	
		this.idUser = getUserData(UserData.class).getUserId();		
		this.registrPotreb = getUserData(UserData.class).getRegistratura();	
		
					try {
						Registratura r = new RegistraturaDAO(getUserData()).findById(this.registrPotreb);
						this.nameRegistrPotreb = r.getRegistratura();
					} catch (DbErrorException e1) {
						
						e1.printStackTrace();
						this.nameRegistrPotreb = null;
					}
		
		
		// Четене на параметри
		
//		FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
//		String param3 = (String) faceletContext.getAttribute("isView"); // 0 - актуализациял 1 - разглеждане
//		Integer isView=0;
//		if(!SearchUtils.isEmpty(param3)) {
//			isView = Integer.valueOf(param3);
//		}
		
				
		this.nachinPred = Integer.valueOf(OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA);
		
		
		try {
			
			decodeDate = new Date();
					
			docTypeList = createItemsList(false, OmbConstants.CODE_CLASSIF_DOC_TYPE, this.decodeDate, true);
			calendarList = createItemsList(false, OmbConstants.CODE_CLASSIF_PERIOD_NOFUTURE, this.decodeDate, true);
			
			setDisableRegList(getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW));
			List<SelectItem> regList = null;	
					 regList = createItemsList(false, OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS, this.decodeDate, true);
			if(regList==null || regList.size()<2) {
				setShowRegistaturiList(false);
			}
		
			
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при инициализиране на филтър за търсене на документи! ", e);
		} catch (UnexpectedResultException e) {
			LOGGER.error("Грешка при инициализиране на филтър за търсене на документи! ", e);
		}
		

		
		actionClear();

	}
	
	
	
	
	public void clearFilter() {

		searchDoc = new DocSearch(this.registrPotreb);	// В searchDoc.registraturaId е текущата регистратура 
	 
		specificsRegister = Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA, Optional.of(getUserData(UserData.class).getRegistratura()));
		// за сега стойностите по подразбиране са - вх. и собствени 
		//   Да се добавят ограничения в зависмисот от правата; в зависимост от това от къде се вика филтъра...
//		searchDoc.getDocTypeArr()[0] = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN; 
//		searchDoc.getDocTypeArr()[1] = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN;	
		periodR = null;
		periodRes = null;
		periodAnswer = null;
		periodAssign = null;
		docsRegistriClassif = null;
		docsVidClassif = null;
		docsTaskAssignClassif = null;
		txtCorrespDoc = "";
		triCheckWork = "0";
		visibleCheckWork = false;
		
		periodR = 9; // този месец	
		changePeriodR();
	}
	
	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	public void changePeriodR () {
		
    	if (this.periodR != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodR);
			searchDoc.setDocDateFrom(di[0]);
			searchDoc.setDocDateTo(di[1]);		
    	} else {
    		searchDoc.setDocDateFrom(null);
			searchDoc.setDocDateTo(null);
		}
    }
	
	public void changeDate() { 
		this.setPeriodR(null);
	}
	
	public void changePeriodRes () {
		
    	if (this.periodRes != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodRes);
			searchDoc.setReceiveDateFrom(di[0]);
			searchDoc.setReceiveDateTo(di[1]);		
    	} else {
    		searchDoc.setReceiveDateFrom(null);
			searchDoc.setReceiveDateTo(null);
		}
    }
	
	public void changeDateRes() { 
		this.setPeriodRes(null);
	}
	
   public void changePeriodAnswer () {
		
    	if (this.periodAnswer!= null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodAnswer);
			searchDoc.setWaitAnswerDateFrom(di[0]);
			searchDoc.setWaitAnswerDateTo(di[1]);	
    	} else {
    		searchDoc.setWaitAnswerDateFrom(null);
			searchDoc.setWaitAnswerDateTo(null);
		}
    }
	
	public void changeDateAnswer() { 
		this.setPeriodAnswer(null);
	}

     public void changePeriodAssign () {
		
    	if (this.periodAssign!= null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodAssign);
			searchDoc.setTaskDateFrom(di[0]);
			searchDoc.setTaskDateTo(di[1]);	
    	} else {
    		searchDoc.setTaskDateFrom(null);
			searchDoc.setTaskDateTo(null);
		}
    }
	
	public void changeDateAssign() { 
		this.setPeriodAssign(null);
	}
	
   public void actionChangeReg() {
		
		if(searchDoc.getRegistraturaId()!=null) {		
			specificsRegister = Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA,Optional.of(searchDoc.getRegistraturaId()));
		}else {
			specificsRegister=null;
		}
		searchDoc.setRegisterIdList(null);
		docsRegistriClassif = null;
	}
	
   /**
    * Избор на кореспондент във филтъра за търсене на документи 
    */
	public void actionSelectCorresp() {
		
		if(searchDoc.getCodeRefCorresp()!=null) {
			try {
				txtCorrespDoc = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS, searchDoc.getCodeRefCorresp(), getCurrentLang(), decodeDate);
				searchDoc.setNameCorresp(txtCorrespDoc);
				this.codeRefCorresp = searchDoc.getCodeRefCorresp();
				this.setNameCorrespDoc(txtCorrespDoc);
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при разкодиране на кореспондент! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}	
	}
	
	public void actionChangeTxtCorrespDoc() {    // Не се изпълнява
		
//		searchDoc.setCodeRefCorresp(null) ;
		searchDoc.setNameCorresp(txtCorrespDoc);
	}
   
	
	/**
	 * Проверка за въведени дати
	 * @return
	 */
//	public boolean provDates () {
//		boolean prRet = true;
//		if (this.dateFrom == null && this.dateTo == null) {
//			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(this.beanmess, "opis.noDates"));					
//			prRet = false;
//		}
//		if (this.dateFrom != null && this.dateTo != null  && this.dateFrom.compareTo(this.dateTo) > 0 ) {
//			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(this.beanmess, "opis.errInpDates"));
//			prRet = false;
//		}
//		
//		return prRet; 
//	}
	
	
	//**************************************************************************************************************
	
	/**
	 *  Бутон ИЗТРИВАНЕ  За основнияя екран
	 */
	public void actionClearAll() {		
		actionClear();
		clearResults();
		this.docSelectedAll.clear();
		clearMarkAllDocs();
		clearAllIzbForOpis();
		
		this.mapObrId.clear();
		this.prClearLists = false;
		
	}
		
	/**
	 * премахва избраните критерии за търсене - в модалния панел
	 */
	public void actionClear() {		
		clearSelDocs();
		clearResultsModal();
		    
	}
	
	public void clearSelDocs () {
			
			this.setVidSort(Integer.valueOf(0));
			this.prClearLists = false;
			clearFilter();
					
	}
	
	public void clearResultsModal () {
			
		this.docsList = null;
		this.smdDocsList = null;
		this.docSelectedAllModal.clear();
		this.docSelectedTmpModal.clear();
				
	}
	
	public void clearResults () {
		this.docsSelList = null;
		clearMarkAllDocs ();
		 
	}
	
	public void clearMarkAllDocs () {
		this.docSelectedAllM.clear();
		this.docSelectedTmp.clear();
		
	}
	
	public void clearAllIzbForOpis () {
		this.docSelectedAllMOpis.clear();
		this.docSelectedAllMOpisT.clear();
		
	}
	
	//******************************************************************************************************
	// За получател
	
	public void actionTipPoluch () {
		actionClearOtAdmStr();
		actionClearCorrespPred ();
	}
	
	public void actionOpenModalPoluch () {
		actionClearOtAdmStr();
		actionClearCorrespPred ();
		this.tipPoluch = Integer.valueOf(1);
		this.nomEkzPred = null;
		
		String  cmdStr = "PF('modalZadPoluch').show();";
		PrimeFaces.current().executeScript(cmdStr);	
	}
	
	/**
	 * Връщане от модалния екран зя задаване на получател за избрани документи (и на номер на екземпляр за предаване)
	 */
	 public void handleCloseDialogZadPoluch ()  {
		if (this.tipPoluch == null 
		  || (this.tipPoluch.intValue() == 1 && this.codePredNa == null)
		  ||  (this.tipPoluch.intValue() == 2 && this.codeRefCorrespPred == null)
		  ) {
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(this.labels, "opis.noPoluch")); 
			return;    
		  }
//		if (this.nastrWithEkz)
//			if (this.nomEkzPred == null || this.nomEkzPred.intValue() <= 0) {
//				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(this.labels, "opis.noEkzPred")); 
//				return;  
//			}
//	
		// Задаване получател
		for (int i = 0; i < this.docSelectedAllM.size(); i++ ) {
			Object[] obj = this.docSelectedAllM.get(i);
			if ((this.tipPoluch.intValue() == 1 && this.codePredNa != null)) {
				obj[9] = this.nachinPred;      // Начин на предаване
				obj[10] = this.codePredNa;     // Код от Адм. структура
				obj[11] = this.namePredNa;     //  Име от Адм. структура
				obj[8] = new Date();           //  Дата на предаване
				
			} else if ((this.tipPoluch.intValue() == 2 && this.codeRefCorrespPred != null)) {
				obj[9] = this.nachinPred;         // Начин на предаване
				obj[10] = this.codeRefCorrespPred;     // Код на кореспондент
				obj[11] = this.nameCorrespPred;        //  Име на кореспондент
				obj[8] = new Date();              //  Дата на предаване

				
			}
			
			obj[12] = null;      // Номер на екземпляр - когато няма екземпляри той остава null
			if (this.nastrWithEkz && (this.nomEkzPred != null && this.nomEkzPred.intValue() > 0))	obj[12] = this.nomEkzPred;   // Номер на екземпляр за предаване за избраните записи
			
			 this.docSelectedAllM.set (i, obj);
			 this.mapObrId.put(asInteger(obj[0]), Boolean.TRUE);
		}
		
		this.docSelectedAllMOpis.addAll(this.docSelectedAllM);
		this.docSelectedAllM.clear();
		this.docSelectedTmp.clear();
	}
	
	
	// За избор от Адм. структура - за получател
	
	public void actionClearOtAdmStr () {
		this.codePredNa = null;
		this.namePredNa = null;
		
	}
	
	// За избор на 	един кореспондент - за получател

	public void actionClearCorrespPred () {    // За единичен избор на кореспондент
		this.codeRefCorrespPred = null;
		this.txtCorrespPred = null;
		this.nameCorrespPred = null;
	
	}
		
    public void actionSelectCorrespPred() {
		
		if(getCodeRefCorrespPred()!=null) {
			try {
				txtCorrespPred = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS, getCodeRefCorrespPred(), getCurrentLang(), decodeDate);
				setNameCorrespPred(txtCorrespPred);
				
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при разкодиране на кореспондент! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}	 
	 }
	
	public void actionChangeTxtCorrespPred() {   // Не се изпълнява
		
//		setCodeRefCorrespPred(null) ;
		setNameCorrespPred(txtCorrespPred);
	}
	
	//***********************************************************************************************************
	 
	
	// За множествен избор на кореспонденти
	public  void actionAddSelectRef(){
		LOGGER.info("TODO - да се прехвърли избрания код в selectManyModalA");
//		if(codeRefCorrSrch != null) {
//			if (this.codeRefCorrespList == null) this.codeRefCorrespList = new ArrayList<Integer> ();
//			String tekst;
//			try {
//				SystemClassif tmpSc = new SystemClassif();
//				tmpSc.setCodeClassif(OmbConstants.CODE_CLASSIF_REFERENTS);
//				tmpSc.setCode(codeRefCorrSrch);
//				tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS, codeRefCorrSrch, getUserData().getCurrentLang(), new Date());
//				tmpSc.setTekst(tekst);
//				scList.add(tmpSc);
//				
//				this.codeRefCorrespList.add(codeRefCorrSrch);
//			} catch (DbErrorException e) {
//				//  Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//			codeRefCorrSrch = null;
//		}
		
		String  cmdStr = "PF('mCorrS').hide();";
		PrimeFaces.current().executeScript(cmdStr);	
	}
	
	
	
	
	/**
	 * Премахване на документ от списъка със запазване на запазени id за вече формирани описи
	 * @param id
	 */
	public void actionDeleteDoc (Object id ) {
	     if (id == null) return;
	     this.docSelectedAll = deleteFromList (id, this.docSelectedAll);
	     if (this.docSelectedAll.isEmpty()) {
	    	 clearResults ();	
	    	 return;
	     }
	     actionSearchAllDocsBezClearSaveId();
		 
	}
	
	/**
	 * Изтриване на вече въведен предаден документ от общия списък по id на движението
	 * @param ob
	 * @param listM
	 * @return
	 */
	public List <Object[]> deleteFromList (Object ob, List <Object[]> listM ) {
		if (ob == null)  return listM;
		List<Object[]> listNew = new ArrayList<Object[]> ();
		for (int i = 0; i < listM.size(); i++ ) {
			Object[] obj =  listM.get(i);
			Integer idDvijRow = asInteger(ob);
			Integer idDvij = asInteger(obj[0]);
//			if (idDvijRow.intValue() == idDvij.intValue())   // Този ред се изтрива 
//			  continue;
//			else {
//				listNew.add(obj);
//				continue;
//			}
		    
			if (idDvijRow.intValue() != idDvij.intValue())
				listNew.add(obj);
		}
		
		return listNew;
		
	}
	

	
	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
//	public void changePeriod() {
//		
//    	if (this.period != null) {
//			Date[] di;
//			di = DateUtils.calculatePeriod(this.period);
//			setDateFrom(di[0]);
//			setDateTo(di[1]);				
//    	} else {
//    		setDateFrom(null);
//    		setDateTo(null);
//		}
//    }
//	
//	public void changeDate() { 
//		this.setPeriod(null);
//	}

	
	//*******************************************************************************************************************
	
	/**
	 * Отваряне на модалния екран за търсене на документи
	 */
	public void actionSelDocs() {

		clearSelDocs();
		clearResultsModal();
				
		this.zaglModal = getMessageResourceString(this.labels,"opis.selDocs") + " ";
			
					
		actionOpenModalSelDocs();
		
	}
	
	// Управление на модален екран за търсене на предадени документи
	
			public void actionOpenModalSelDocs() {
				
					String  cmdStr = "PF('modalSelDocs').show();";
					PrimeFaces.current().executeScript(cmdStr);	
						
			}
			
				
			
			/** 
			 * Списък с  документи по зададени критерии за съставяне на опис със зачисляване - изпълнява се в модалния прозорец
			 * 
			 */
			public void actionSearchInModal(){
		 
	
				 if("0".equals(triCheckWork)) {
					searchDoc.setNullWorkOffId(null);
				 }else if("1".equals(triCheckWork)) {
					searchDoc.setNullWorkOffId(false);
				 }else if("2".equals(triCheckWork)) {
					searchDoc.setNullWorkOffId(true);
				 }
				 
				 searchDoc.buildQueryDocListForOpis(getUserData(), this.docSelectedAll, this.nastrWithEkz);
				 
				   this.smdDocsList = searchDoc;
					clearResultsModal(); 
													
					String defaultSortColumn = sortA5A0; 
					
					/*
					 *   Съдържание на полетата  от A0 до A21
					 * select distinct d.DOC_ID A0, null A1, d.RN_DOC A2, d.RN_PREFIX A3, d.RN_PORED A4, d.DOC_DATE A5, d.DOC_TYPE A6, d.DOC_VID A7 , TO_DATE('2021-01-27 18:59:10','yyyy-mm-dd HH24:MI:SS') A8 , 1 A9 , null A10, ' ' A11, 1 A12, null A13 , d.TEH_NOMER A14, d.TEH_DATE A15, d.REGISTER_ID A16
					 *  ,  CASE WHEN LENGTH(d.OTNOSNO) > 300 THEN DBMS_LOB.SUBSTR(d.OTNOSNO,300,1) || '...' ELSE TO_CHAR(d.OTNOSNO) END  A17  , d.URGENT A18, d.CODE_REF_CORRESP A19  , CASE WHEN d.DOC_TYPE = 1 THEN null ELSE  (select LISTAGG (dr.CODE_REF, ',') WITHIN GROUP (ORDER BY dr.PORED) FROM DOC_REFERENTS dr where dr.DOC_ID = d.DOC_ID and dr.ROLE_REF = 1)  END A20  
			           , d.processed A21
			            from DOC d
					 * 
					 */
					
					this.docsList = new LazyDataModelSQL2Array(searchDoc, defaultSortColumn);	
					
				 
			}
			
			 public void actionSearchFullTextInModal(){
	
				searchDoc.buildQueryDocListWithFulltextForOpis(getUserData(), this.docSelectedAll);
				this.smdDocsList = searchDoc;
				clearResultsModal(); 
												
				String defaultSortColumn = sortA5A0; 
				
				this.docsList = new LazyDataModelSQL2Array(searchDoc, defaultSortColumn);	
				return;

			}
			
			public String actionGoto(Integer idObj) {
				
				String result = "";
				result = "docView.xhtml?faces-redirect=true&idObj=" + idObj;
				
				return result;
			}
			
			public void changeDocType() {
				visibleCheckWork = false;
				for(Integer item:searchDoc.getDocTypeArr()) {
					if(item!=null && item.intValue()==OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) {
						visibleCheckWork = true;
						break;
					}
				}
				
				if(!visibleCheckWork) {
					triCheckWork="0";
				}
			}
			
			
			/**
			 * Връщане от модалния за избор на документи с добавяне на всички новоизброни
			 */
			public void handleSelDocsOKAll () {
				// Първо запис в this.docSelectedAllModal всички намерени документи в this.docs
				if (this.docsList.getRowCount() > 0 ) {
					List<Object[]> listObj = null;
					try {
						
						
						String sqlStr = this.searchDoc.getSql();
						Map<String, Object> params = this.searchDoc.getSqlParameters();
											
						listObj = new OpisDAO(getUserData()).findDataWithSqlStringParams(sqlStr, params,  sortA5A0);
						
					} catch (DbErrorException e) {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getLocalizedMessage());
						return;
					}
					
		              this.docSelectedAllModal.clear();
		              this.docSelectedAllModal.addAll(listObj);
					
				}  
				
						
						
				handleSelDocsOK ();
			
				
			}
				
			/**
			 * Връщане от модалния за избор на документи с добавяне на маркирани новоизбрани
			 */
			public void handleSelDocsOK () {
				if (!this.docSelectedAllModal.isEmpty()) {
					for (Object[] obj : this.docSelectedAllModal)
						this.docSelectedAll.add(obj);
				}
				
				actionSearchAllDocs();	   // Търсене на документи по id на документи с обновения списък
				
				handleCloseDialogSelDocs ();
				
			}
			
			/**
			 *  Връщане от модалния за избор на документи с игнориране на новоизбрани 
			 */
			public void handleCloseDialogSelDocs () {
		          clearSelDocs(); 
				  clearResultsModal();
				  actionRazm ();      // Размаркиране в основния екран
			}
			
	
	
	//*********************************************************************************************************
	//     За основния екран		
			
	/**
	 * Търсене на  документи по idDoc  от DOC - те се в docSelectedAll
	 */
	public void actionSearchAllDocs(){
		
		clearResults();
		 SelectMetadata smd = null;
		 smd = new OpisDAO(getUserData()).createSelectOpisZach(this.docSelectedAll);
	
		String defaultSortColumn = sortA5A0;   
		this.docsSelList = new LazyDataModelSQL2Array(smd, defaultSortColumn);	
			
	} 	
	
	/**
	 * Търсене на  документи по idDoc от DOC със запазване на запазени вече id за формирани описи
	 */
	public void actionSearchAllDocsBezClearSaveId(){
		
		this.docsSelList = null;
		 SelectMetadata smd = null;
		 smd = new OpisDAO(getUserData()).createSelectOpisZach (this.docSelectedAll);
	
		String defaultSortColumn = sortA5A0;   
		this.docsSelList = new LazyDataModelSQL2Array(smd, defaultSortColumn);
		actionRazm();
		
	} 
	

	
	
//  Избрани записи за опис - те са в this.docSelectedAllM
	
	public void actionIzbrForOpis() {
//		setMapObrId ();
//		this.docSelectedAllM.clear();
//		this.docSelectedTmp.clear();
		
		if (!this.docSelectedAllM.isEmpty()) {
	//		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(this.labels, "opis.messForPoluch"));			
			actionOpenModalPoluch ();
			return;
		}
		
		
		this.docSelectedAllMOpisT.clear();
		this.docSelectedAllMOpisT = addTekstsForClass(this.docSelectedAllMOpis);   // Добавяне 2 разкодирани текстови полета
		this.docSelectedAllMOpisT = addNomEkz1 (this.docSelectedAllMOpisT);    // Добавяне на екземпляр 1 за празните номера
					
		doSortForOpis(this.docSelectedAllMOpisT, 0);
		this.setVidSort(Integer.valueOf(0));
		this.prClearLists = false;
		this.zaglModal = getMessageResourceString(this.labels,"opis.izbrDocZach") + ": ";
				
		this.zaglModal += String.valueOf(this.docSelectedAllMOpis.size());	
		
		actionOpenModalFormOpis();
		
	}
	
	/**
	 * Запазване на idDoc на документи , определени за  опис
	 */
	public void setMapObrId () {
		for (Object[] obj: this.docSelectedAllM) {
			this.mapObrId.put(asInteger(obj[0]), Boolean.TRUE);
		}
		
	}
	
	
	/**
	 * Добавяне на разкодирани текстове за тип документ и вид документ към избрани за опис записи
	 * @param objList
	 * @return
	 */
	List <Object[]> addTekstsForClass (List<Object[]> objList) {
		
		List <Object[]> objListNew = new ArrayList<Object[]> ();
		
		for (Object[] obj : objList) {
			Object[] obj1 = new Object[obj.length+2];
			for (int i = 0; i < obj.length; i++)
				obj1[i] = obj[i];
			
			try {
				obj1[obj.length] = (String)getSystemData().decodeItem(Integer.valueOf(OmbConstants.CODE_CLASSIF_DOC_TYPE), asInteger(obj[6]), getCurrentLang(), new Date());
			} catch (DbErrorException e) {
				//  Auto-generated catch block
				obj1[obj.length] =(String)asInteger(obj[6]).toString();
			}   //  Разкодиране Тип док.
			
			try {
				obj1[obj.length+1] = (String)getSystemData().decodeItem(Integer.valueOf(OmbConstants.CODE_CLASSIF_DOC_VID), asInteger(obj[7]), getCurrentLang(), new Date());
			} catch (DbErrorException e) {
				//  Auto-generated catch block
				obj1[obj.length+1] = (String)asInteger(obj[7]).toString();
			}   //  Разкодиране Тип док.
			
			
		
			objListNew.add(obj1);
		}
		
		if (objListNew.isEmpty()) objListNew.addAll(objList);
		
		return objListNew;
		
	}
	
	/**
	 *  Добавяне на номер екземпляр 1 преди първо показване на избраните записи за описа
	 * @param objList
	 * @return
	 */
   List <Object[]> addNomEkz1 (List<Object[]> objList) {
			
		for (int i = 0; i <  objList.size(); i++) {
		      Object[] objL = objList.get(i);
		      Integer nom = null;
			if (objL[12] instanceof String)  
				nom = Integer.valueOf((String)objL[12]);
	        else nom = asInteger(objL[12]); 
			
		    if (nom == null || nom.intValue() <= 0)   nom = Integer.valueOf(1);
		     objL[12] = nom;
			objList.set(i,  objL);
				
		  }	
				
		return objList;
	}	
	
	/**
	 * Запомняне номер на екземпляр в основния списък
	 * @param objListWithNom - с номер на екземпляри
	 * @param objList
	 * @return
	 */
	
	List <Object[]> updateNomEkz (List<Object[]> objListWithNom, List<Object[]> objList) {
		
		/*
		 *   Съдържание на полетата на Object[] - от A0 до A21
		 * select distinct d.DOC_ID A0, null A1, d.RN_DOC A2, d.RN_PREFIX A3, d.RN_PORED A4, d.DOC_DATE A5, d.DOC_TYPE A6, d.DOC_VID A7 , TO_DATE('2021-01-27 18:59:10','yyyy-mm-dd HH24:MI:SS') A8 , 1 A9 , null A10, ' ' A11, 1 A12, null A13 , d.TEH_NOMER A14, d.TEH_DATE A15, d.REGISTER_ID A16 ,  CASE WHEN LENGTH(d.OTNOSNO) > 300 THEN DBMS_LOB.SUBSTR(d.OTNOSNO,300,1) || '...' ELSE TO_CHAR(d.OTNOSNO) END  A17  , d.URGENT A18, d.CODE_REF_CORRESP A19 
		 *   , CASE WHEN d.DOC_TYPE = 1 THEN null ELSE  (select LISTAGG (dr.CODE_REF, ',') WITHIN GROUP (ORDER BY dr.PORED) FROM DOC_REFERENTS dr where dr.DOC_ID = d.DOC_ID and dr.ROLE_REF = 1)  END A20  
             , d.processed A21
            from DOC d
		 * 
		 */
		
		List <Object[]> objListNew = new ArrayList<Object[]> ();
		
		for (Object[] objNom : objListWithNom) {
			Integer docId = asInteger(objNom[0]);
					
			// Сравнение по docId
			for (int i = 0; i < objList.size(); i++) {
				Object[] obj = objList.get(i);
				Integer docId0 = asInteger(obj[0]);
			
				if ((docId0 == null && docId != null) || (docId0 != null && docId == null)) continue;
							
				if ((docId0 != null && docId != null && docId0.compareTo(docId) != 0) )  continue;
				
				if (objNom[12] instanceof String)  
					obj[12] = Integer.valueOf((String)objNom[12]);
		        else obj[12] = asInteger(objNom[12]); 
			
				objList.set(i,  obj);
				break;
			}
			
		}	
		
		
		return objList;
	}	
		
	/**
	 * Сортиране на избрани за опис  записи
	 */
	
	public void actionSort () {
		
		if (this.docSelectedAllMOpisT.isEmpty() || this.vidSort == null )  return;
		doSortForOpis(this.docSelectedAllMOpisT, this.vidSort.intValue());
				
	}
	
	
	// Управление на модален екран за показване на избрани документи за опис
	
		public void actionOpenModalFormOpis() {
			
				String  cmdStr = "PF('modalFormOpis').show();";
				PrimeFaces.current().executeScript(cmdStr);	
					
		}
	
	
		/**
		 * Затваряне на модален с избрани за опис записи (action за бутон  ИЗХОД)
		 */
	public void handleCloseDialogFormOpis () {
		
		if (this.nastrWithEkz) {
			// Запазване въведени номера на екземпляри
			if (this.docSelectedAllMOpisT != null &&  !this.docSelectedAllMOpisT.isEmpty() && this.docSelectedAllMOpis != null && !this.docSelectedAllMOpis.isEmpty())
			    this.docSelectedAllMOpis = updateNomEkz (this.docSelectedAllMOpisT, this.docSelectedAllMOpis);
		}
		if (this.docSelectedAllMOpisT != null) {
			this.docSelectedAllMOpisT.clear();
		}
		if (this.prClearLists) {           // Зачислени са избраните записи за описа
//			setMapObrId ();
			this.docSelectedAllM.clear();
			this.docSelectedTmp.clear();
			if (this.docSelectedAllMOpis != null) {
				this.docSelectedAllMOpis.clear();   // Ще се формира нов опис
			}
			this.prClearLists = false;
		}		
	}
	
	//********************************************************************************************************************
	
	//  Избор на документи в модалния modalSelDocs
	/*
	 * Множествен избор на документи 
	 */
	/**
	 * Избира всички редове от текущата страница
	 * @param event
	 */
	  public void onRowSelectAllModal(ToggleSelectEvent event) {    
    	List<Object[]> tmpL = new ArrayList<>();
    	tmpL.addAll(getDocSelectedAllModal());
    	if(event.isSelected()) {
    		for (Object[] obj : getDocSelectedTmpModal()) {
    			if (obj != null && obj.length > 0) {
	    			boolean bb = true;
	    			Long l2 = Long.valueOf(obj[0].toString());
	    			for (Object[] j : tmpL) { 
	        			Long l1 = Long.valueOf(j[0].toString());        			
	    	    		if(l1.equals(l2)) {    	    			
	    	    			bb = false;
	    	    			break;
	    	    		}
	        		}
	    			if(bb) {
	    				tmpL.add(obj);
	    			}
    			}	
    		}    		
    	}else {
	    	List<Object[]> tmpLPageC =  docsList.getResult();// rows from current page....    		
			for (Object[] obj : tmpLPageC) {
				if (obj != null && obj.length > 0) {
					Long l2 = Long.valueOf(obj[0].toString());
					for (Object[] j : tmpL) { 
		    			Long l1 = Long.valueOf(j[0].toString());        			
			    		if(l1.equals(l2)) {    	    			
			    			tmpL.remove(j);
			    			break;
			    		}	
		    		}
				}	
			}    		
		}
		setDocSelectedAllModal(tmpL);	    	
		   
	}
		    
    /** 
     * Select one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowSelectModal(SelectEvent event) {    	
    	if(event!=null  && event.getObject()!=null) {
    		
    		List<Object[]> tmpList =  getDocSelectedAllModal();
    		
    		Object[] obj = (Object[]) event.getObject();
    		boolean bb = true;
    		Integer l2 = Integer.valueOf(obj[0].toString());
			for (Object[] j : tmpList) { 
				Integer l1 = Integer.valueOf(j[0].toString());        			
	    		if(l1.equals(l2)) {
	    			bb = false;
	    			break;
	    		}
	   		}
			if(bb) {
				tmpList.add(obj);
				setDocSelectedAllModal(tmpList);   
			}
    	}	    	
    	
    }
		 
		    
    /**
     * unselect one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowUnselectModal(UnselectEvent event) {
    	if(event!=null  && event.getObject()!=null) {
    		Object[] obj = (Object[]) event.getObject();
    		List<Object[] > tmpL = new ArrayList<>();
    		tmpL.addAll(getDocSelectedAllModal());
    		for (Object[] j : tmpL) {
    			Integer l1 = Integer.valueOf(j[0].toString());
    			Integer l2 = Integer.valueOf(obj[0].toString());
	    		if(l1.equals(l2)) {
	    			tmpL.remove(j);
	    			setDocSelectedAllModal(tmpL);
	    			break;
	    		}
    		}
    		
    	}
    }

    /**
     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга
     */
    public void   onPageUpdateSelectedModal(){
    	if (getDocSelectedAllModal() != null && !getDocSelectedAllModal().isEmpty()) {
    		getDocSelectedTmpModal().clear();
    		getDocSelectedTmpModal().addAll(getDocSelectedAllModal());
    	}	    	
    
    }
	
	
    /**
     * Премахва маркиране за избор в модалния панел за избор на документи
     */
	public void actionRazmModal () {
		
		this.docSelectedAllModal.clear();
		this.docSelectedTmpModal.clear();
		
	}
	
	
	
	//********************************************************************************************************************
	
	/**
	 * Премахва маркиране за въведени документи - на основния екран
	 */
	   public void actionRazm () {
			
			this.docSelectedAllM.clear();
			this.docSelectedTmp.clear();
			
		}	
	
	/*
	 * Множествен избор на документи за опис
	 */
	/**
	 * Избира всички редове от текущата страница
	 * @param event
	 */
	  public void onRowSelectAll(ToggleSelectEvent event) {    
    	List<Object[]> tmpL = new ArrayList<>();
    	tmpL.addAll(getDocSelectedAllM());
    	if(event.isSelected()) {
    		for (Object[] obj : getDocSelectedTmp()) {
    			if (obj != null && obj.length > 0) {
	    			boolean bb = true;
	    			Long l2 = Long.valueOf(obj[0].toString());
	    			for (Object[] j : tmpL) { 
	        			Long l1 = Long.valueOf(j[0].toString());        			
	    	    		if(l1.equals(l2)) {    	    			
	    	    			bb = false;
	    	    			break;
	    	    		}
	        		}
	    			if(bb) {
	    				tmpL.add(obj);
	    			}
    			}	
    		}    		
    	}else {
	    	List<Object[]> tmpLPageC =  docsSelList.getResult();// rows from current page....    		
			for (Object[] obj : tmpLPageC) {
				if (obj != null && obj.length > 0) {
					Long l2 = Long.valueOf(obj[0].toString());
					for (Object[] j : tmpL) { 
		    			Long l1 = Long.valueOf(j[0].toString());        			
			    		if(l1.equals(l2)) {    	    			
			    			tmpL.remove(j);
			    			break;
			    		}	
		    		}
				}	
			}    		
		}
		setDocSelectedAllM(tmpL);	    	
		    	   
	}
		    
    /** 
     * Select one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowSelect(SelectEvent event) {    	
    	if(event!=null  && event.getObject()!=null) {
    		
    		List<Object[]> tmpList =  getDocSelectedAllM();
    		
    		Object[] obj = (Object[]) event.getObject();
    		boolean bb = true;
    		Integer l2 = Integer.valueOf(obj[0].toString());
			for (Object[] j : tmpList) { 
				Integer l1 = Integer.valueOf(j[0].toString());        			
	    		if(l1.equals(l2)) {
	    			bb = false;
	    			break;
	    		}
	   		}
			if(bb) {
				tmpList.add(obj);
				setDocSelectedAllM(tmpList);   
			}
    	}	    	
    	
    }
		
    
//    public void onRowSelectModal(SelectEvent event) {    	
//    	if(event!=null  && event.getObject()!=null) {
//    		
//    		List<Object[]> tmpList =  getDocSelectedAllModal();
//    		
//    		Object[] obj = (Object[]) event.getObject();
//    		boolean bb = true;
//    		Integer l2 = Integer.valueOf(obj[0].toString());
//			for (Object[] j : tmpList) { 
//				Integer l1 = Integer.valueOf(j[0].toString());        			
//	    		if(l1.equals(l2)) {
//	    			bb = false;
//	    			break;
//	    		}
//	   		}
//			if(bb) {
//				tmpList.add(obj);
//				setDocSelectedAllModal(tmpList);   
//			}
//    	}	    	
//    	
//    }
		    
    /**
     * unselect one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowUnselect(UnselectEvent event) {
    	if(event!=null  && event.getObject()!=null) {
    		Object[] obj = (Object[]) event.getObject();
    		List<Object[] > tmpL = new ArrayList<>();
    		tmpL.addAll(getDocSelectedAllM());
    		for (Object[] j : tmpL) {
    			Integer l1 = Integer.valueOf(j[0].toString());
    			Integer l2 = Integer.valueOf(obj[0].toString());
	    		if(l1.equals(l2)) {
	    			tmpL.remove(j);
	    			setDocSelectedAllM(tmpL);
	    			break;
	    		}
    		}
    	//	LOGGER.debug( "onRowUnselectIil->>"+getDocSelectedAllM().size());
    	}
    }

    /**
     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга - не се използува
     */
    public void   onPageUpdateSelected(){
    	if (getDocSelectedAllM() != null && !getDocSelectedAllM().isEmpty()) {
    		getDocSelectedTmp().clear();
    		getDocSelectedTmp().addAll(getDocSelectedAllM());
    	}	    	
    
    }
	
   	 
    /**
     * Проверка за забрана на селектиране, ако вече този запис е бил включен във формиран опис    
     * @param idDvij
     * @return
     */
    public boolean getVisCheck (Object idDvij) {
		if (this.mapObrId.isEmpty()) return false;
		Integer id = asInteger(idDvij);
		if (!this.mapObrId.containsKey(id)) return false;
		return true;
		
	}
    
      
    /**
     * Сортиране на List с избрани записи за опис
     * @param ekzList
     * @param p
     */
	private void doSortForOpis(List<Object[]> dvijList, final int p) 
{ 
 // Подреждане записи за ekz 
 // p = 0		- получател,  регистр. номер(rnPref, rnPored), номер екз.  
// p = 1  - 		получател, тип документ,  регистр. номер(rnPref, rnPored), номер екз. 
// p = 2  - 		получател, вид документ,  регистр. номер(rnPref, rnPored), номер екз.
	
  Comparator<Object> c = new Comparator<Object>() 
  { 
      public int compare(Object o1, Object o2) 
      { 
//    	    GregorianCalendar gc = new GregorianCalendar();
//    		GregorianCalendar gc1 = new GregorianCalendar();
//    		GregorianCalendar gc2 = new GregorianCalendar();
//    	  
    		Object dvijList1[] = new Object[1];
    	  	Object dvijList2[] = new Object[1];
    	  			
    	  	    dvijList1 =  (Object[]) o1;
    	  	    dvijList2 =  (Object[]) o2;
			
    	
				//LM
    	  	    Integer idDvij1 = asInteger( dvijList1[0]);
    	  	    Integer idDvij2 = asInteger( dvijList2[0]);
    	  	    
    	  	    String tPol1 = asString(dvijList1[11]);
    	  	    if (tPol1 != null)  tPol1 = tPol1.trim();
    	  	    String tPol2 = asString(dvijList2[11]);
    	  	    if (tPol2 != null)  tPol2 = tPol2.trim();
    	  	    
				String rnDoc1 = asString(dvijList1[2]);
				if (rnDoc1 != null)  rnDoc1 = rnDoc1.trim();
				String rnDoc2 = asString(dvijList2[2]);
				if (rnDoc2 != null)  rnDoc2 = rnDoc2.trim();

				String pref1 = asString(dvijList1[3]);
				if (pref1 != null) pref1 = pref1.trim();
				String pref2 = asString(dvijList2[3]);
				if (pref2 != null) pref2 = pref2.trim();
				
				Integer pored1 = asInteger(dvijList1[4]);
				Integer pored2 = asInteger(dvijList2[4]);
				
				Date datDoc1 = asDate(dvijList1[5]);
				Date datDoc2 = asDate(dvijList2[5]);
//				Integer tipDoc1 =  asInteger(dvijList1[6]);
//				Integer tipDoc2 = asInteger(dvijList2[6]);
//				Integer vidDoc1 = asInteger(dvijList1[7]);
//				Integer vidDoc2 = asInteger(dvijList2[7]);
				
				String tipDocT1 = asString(dvijList1[dvijList1.length-2]);
				if (tipDocT1 != null) tipDocT1 = tipDocT1.trim();
				String tipDocT2 = asString(dvijList2[dvijList2.length-2]);
				if (tipDocT2 != null) tipDocT2 = tipDocT2.trim();
				
				String vidDocT1 = asString(dvijList1[dvijList1.length-1]);
				if (vidDocT1 != null)  vidDocT1 = vidDocT1.trim();
				String vidDocT2 = asString(dvijList2[dvijList2.length-1]);
				if (vidDocT2 != null)  vidDocT2 = vidDocT2.trim();
                
				Integer idDoc1 = asInteger(dvijList1[1]); 
				Integer idDoc2 = asInteger(dvijList2[1]);
				
				//  Ако се зададени директно номера на екземпляри от екрана - тип на полето е String
				  if (dvijList1[12] instanceof String) 
					  dvijList1[12] =	Integer.valueOf((String)dvijList1[12]);
				  if (dvijList2[12] instanceof String) 
					  dvijList2[12] =	Integer.valueOf((String)dvijList2[12]);
				
				Integer nomEkz1 = asInteger(dvijList1[12]); 
				Integer nomEkz2 = asInteger(dvijList2[12]);
				
				int i  = 0;
//				 if (idPol1 != null && idPol2 != null && (idPol1.longValue() > 0 || idPol2.longValue() > 0)) {
//					 i = idPol1.compareTo(idPol2);
//			        	if (i != 0){ 
//			        		return i;
//			        	}
//
//				 } else {
//					 if (tPol1 != null && tPol2 != null) {
//						 i = tPol1.compareTo(tPol2);
//						 if (i != 0) {
//				        		return i;
//						 }
//					 }

//				 }
					 
					 if (tPol1 != null && tPol2 != null) {
						 i =  tPol1.compareToIgnoreCase(tPol2);
						 if (i != 0) {
				        		return i;
						 }
					 }
				 
				 if (p == 1 ) {
//					 if (tipDoc1 != null && tipDoc2 != null && (tipDoc1.longValue() > 0 || tipDoc2.longValue() > 0)) {
//						 i = tipDoc1.compareTo(tipDoc2);
//				        	if (i != 0) {
//				        		return i;
//				        	}
//					 } 
					 if (tipDocT1 != null && tipDocT2 != null) {
						 i = tipDocT1.compareToIgnoreCase(tipDocT2);
						 if (i != 0) {
				        		return i;
						 }
					 }
					 
				 } else {
					 if (p == 2 ) {
//						 if (vidDoc1 != null && vidDoc2 != null && (vidDoc1.longValue() > 0 || vidDoc2.longValue() > 0)) {
//							 i = vidDoc1.compareTo(vidDoc2);
//					        	if (i != 0) {
//					        		return i;
//					        	}
//						 } 
						 
						 if (vidDocT1 != null && vidDocT2 != null) {
							 i = vidDocT1.compareToIgnoreCase(vidDocT2);
							 if (i != 0) {
					        		return i;
							 }
						 }
					 }
				 }
				 
//			 	  if (datDoc1 != null && datDoc2 != null) {
//		    		  gc.setTime(datDoc1);
//		    		 gc1.set(gc.get(GregorianCalendar.YEAR), gc.get(GregorianCalendar.MONTH), gc.get(GregorianCalendar.DAY_OF_MONTH));
//		    		 gc.setTime(datDoc2);
//		    		  gc2.set(gc.get(GregorianCalendar.YEAR), gc.get(GregorianCalendar.MONTH), gc.get(GregorianCalendar.DAY_OF_MONTH));
//		    		  i = gc1.compareTo(gc2);
//	
//		    		  if (i != 0 ) {
//						return i;
//		    		  }
//	    	  }	  	
				
				 // Сортиране по рег. номер
//		         if (rnDoc1 != null && rnDoc2 != null) {
//			           	i = rnDoc1.compareTo(rnDoc2);
//			        	if (i != 0) {
//			        		return i;
//			        	}
//			     }	
//		          if (idDnev1 != null && idDnev2 != null) {
//		           	i = idDnev1.compareTo(idDnev2);
//		        	if (i != 0) {
//		        		return i;
//		        	}
//		          }	 else {
//				   	if (idDnev1 == null && idDnev != null)
//					   return -1;
//				   	else if (idDnev1 != null && idDnev2 == null)
//				   	return 1;	
//               }
//				 
				  
				 if (pref1 != null && pref2 != null) {
					 i = pref1.compareToIgnoreCase(pref2);
					 if (i != 0)  return i;
					 
				 } else {
					 if (pref1 == null && pref2 != null)
						 return -1;
					 else if (pref1 != null && pref2 == null )
						 return 1;
				 } 
				 if (pored1 != null && pored2 != null) {
					 i = pored1.compareTo(pored2);
					 if (i != 0)  return i;
				 } else {
					 if (pored1 == null && pored2 != null)
						 return -1;
					 else if (pored1 != null && pored2 == null)
						 return 1;
				 }
					

				 //  Накрая подреждане по idDoc, nomEkz
    			 if (idDoc1 != null && idDoc2 != null) {
			        	i = idDoc1.compareTo(idDoc2);
			        	if (i != 0) {
			        		return i;
			        	}
    			 }   	
				 
        			 if (nomEkz1 != null && nomEkz2 != null) {
				        	i = nomEkz1.compareTo(nomEkz2);
				        	if (i != 0) {
				        		return i;
				        	}
        			 }   	
				
//        			 if (idDvij1 != null && idDvij2 != null) {
//				        	i = idDvij1.compareTo(idDvij2);
//				        	if (i != 0) {
//				        		return i;
//				        	}
//     			     }   
			     
				 // При равенство след сравнение на главния параметър
                return 0;
    	        
      }  
      
  };
 Collections.sort(dvijList, c); 
}   	
  	
/******************************** EXPORTS **********************************/
	
	//**************************************************************************************************************************
	//    Директни експорти на списъка с избрани предадени документи
	//**************************************************************************************************************************
		
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
	 */
	public void postProcessXLS(Object document) {
		
		String title = null;
		title = getMessageResourceString(LABELS, "opis.zaglRaznZach");	
		
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfo() , null, null);		
     
	}
	

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = null;
			title = getMessageResourceString(LABELS, "opis.zaglRaznZach");
			
			new CustomExpPreProcess().preProcessPDF(document, title,  dopInfo(), null, null);		
						
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
	 * подзаглавие за експорта 
	 */
	public Object[] dopInfo() {
		return null;
		
//		Object[] dopInf = null;
//		dopInf = new Object[2];
//		if(this.dateFrom != null && this.dateTo != null) {
//			dopInf[0] = getMessageResourceString(this.beanmess,"opis.period") + ": " + DateUtils.printDate(this.dateFrom) + " - "+ DateUtils.printDate(this.dateTo);
//		} else {
//			if (this.dateFrom != null) dopInf[0] = getMessageResourceString(this.beanmess,"opis.period") + " " + getMessageResourceString(this.beanmess,"general.ot") + " " + DateUtils.printDate(this.dateFrom);
//			else if (this.dateTo != null) dopInf[0] = getMessageResourceString(this.beanmess,"opis.period") + " " + getMessageResourceString(this.beanmess,"general.do") + " " +  DateUtils.printDate(this.dateTo);
//		}
	   
	
//		return dopInf;
	}
	
	/**
	 * Формиране на движения за предаване на документите от this.docSelectedAllMOpisT
	 */
	public void actionZachislDocs () {
		if (this.docSelectedAllMOpisT==null || this.docSelectedAllMOpisT.isEmpty())  return;
		// Задаване номер на екземпляр, когато няма екземпляри
		if (!this.nastrWithEkz)
			for (int i = 0; i < this.docSelectedAllMOpisT.size(); i++) {
	            Object[] obj = this.docSelectedAllMOpisT.get(i);
	            Integer docRegId = asInteger(obj[0]);
	            
	             
	            
	//            obj[12] = null;          // Номер на екземпляр - null
	            obj[12] = Integer.valueOf(1);   // Номер на екземпляр  1 по подразбиране 
				this.docSelectedAllMOpisT.set(i, obj); 
			}    
		
		else {
			// Проверка дали са зададени за всеки ред номера на екземпляри
			for (int i = 0; i < this.docSelectedAllMOpisT.size(); i++) {
	            Object[] obj = this.docSelectedAllMOpisT.get(i);
	            if (obj[12] == null  || (obj[12] instanceof String && obj[12].toString().trim().isEmpty()) ) {
	            	JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(this.beanmess,"opis.lipsvaEkz"));	
	            	return;
	            }
			}   
		}
		
		DocDvijDAO dao = new DocDvijDAO (getUserData());
		
			
		try {
			
		//	  JPA.getUtil().runInTransaction(() -> {     // Начало на транзакция 
			      JPA.getUtil().begin();   // Начало на транзакция 
			      
					for (int i = 0; i < this.docSelectedAllMOpisT.size(); i++) {
		                Object[] obj = this.docSelectedAllMOpisT.get(i);
		                Integer docRegId = asInteger(obj[0]);    // doc.doc_id
		                Integer docType =   asInteger(obj[6]);   // doc.doc_type
		                Integer docProcess = asInteger(obj[21]);     // doc.processed
		                this.dvijDoc = new DocDvij();
		                
		                this.dvijDoc.setDocId(asInteger(obj[0]));
		                this.dvijDoc.setCodeRef(asInteger(obj[10]));
		                this.dvijDoc.setDvijText(asString(obj[11]));
		                this.dvijDoc.setDvijDate(asDate(obj[8]));
		                this.dvijDoc.setDvijMethod(asInteger(obj[9]));
		                if (obj[12] instanceof String) {   //  Това е само при  this.nastrWithEkz  = true
		                  obj[12] =	Integer.valueOf((String)obj[12]);
			         //     this.dvijDoc.setEkzNomer(Integer.valueOf((String)obj[12]));
			            } 
		                this.dvijDoc.setEkzNomer(asInteger(obj[12])); 
		            		                
		                this.dvijDoc.setUserReg(this.idUser);
		                this.dvijDoc.setDateReg(new Date());
		                this.dvijDoc.setDvijInfo(getMessageResourceString(this.beanmess, "opis.dvijInfo"));
		                
		                try {
		        			String email = null;   
		        			
		        			if(isAdmStr(asInteger(obj[10]))) {
		        				SystemClassif empl = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_ADMIN_STR, asInteger(obj[10]), SysConstants.CODE_DEFAULT_LANG, new Date(), true);
		        				if(empl != null) {
		        					if (empl.getSpecifics() != null)
		        					  email = (String) empl.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_CONTACT_EMAIL];
		        				}
		        			}
		        			else if(isKoresp(asInteger(obj[10]))) {
		        				SystemClassif empl = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_REFERENTS, asInteger(obj[10]), SysConstants.CODE_DEFAULT_LANG, new Date(), true);
		        				if(empl != null) {
		        					if (empl.getSpecifics() != null)
		        					  email = (String) empl.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_CONTACT_EMAIL];
		        				}
		        			}
		        			 this.dvijDoc.setDvijEmail(email);
		        			
		        		} catch (DbErrorException e) {
//		        			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
//		        			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		        		}
						
		              
							 this.dvijDoc =  dao.save( this.dvijDoc, (SystemData) getSystemData() , docRegId);
					          
							
							 if (docType.intValue() == OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN  && docProcess.intValue() == OmbConstants.CODE_ZNACHENIE_NE) {
								     // За входящи документи - трябва да се актуализира  поле doc.processed - трябва да стане да - обработено	
								
								 Query q = JPA.getUtil().getEntityManager().createNativeQuery("UPDATE DOC SET PROCESSED = :DA  WHERE DOC_ID = :IDD");
									q.setParameter("DA",  OmbConstants.CODE_ZNACHENIE_DA);
									q.setParameter("IDD", docRegId);
									q.executeUpdate();
									
									String docIdent = obj[2] + "/" + DateUtils.printDate((Date) obj[5]);								 
									SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, docRegId
										, "Документ "+docIdent+" е маркиран за обработен, при включването му в опис със зачисляване.");

									journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
									journal.setDateAction(new Date());
									journal.setIdUser(getUserData().getUserId());

									dao.saveAudit(journal);

									 docProcess = Integer.valueOf(OmbConstants.CODE_ZNACHENIE_DA);
									 obj[21] =  docProcess;        // Променено е  поле processed
							 }	 
						
					 	obj[1] = this.dvijDoc.getId();          // id на новото движение за документа - това поле първоначално е null
						this.docSelectedAllMOpisT.set(i, obj);     
				    }	// Край на for

					
		//	   });
			  
					JPA.getUtil().commit();      // Успешно завършване на транзакцията
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
					getMessageResourceString("ui_beanMessages", SUCCESSAVEMSG));
			this.prClearLists = true;

		} catch (Exception e) {
			JPA.getUtil().rollback();
			LOGGER.error("Грешка при запис на движение за документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString("ui_beanMessages", ERRDATABASEMSG) + "-"+ e.getLocalizedMessage(), e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
		
				
	}
	
	/**
	 * Прверява дали подаден код на референт е част от класификацията с административната структура.
	 * @param codeRef
	 */
	public boolean isAdmStr(Integer codeRef) {
		if(codeRef == null) return false;
		else
			try {
				return getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, codeRef, new Date());
			} catch (DbErrorException e) {
//				LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
//				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
				return false;
			}
	}
	
	/**
	 * Прверява дали подаден код на референт е на кореспондент, а не от адм. структура.
	 * @param codeRef
	 */
	public boolean isKoresp(Integer codeRef) {
		if(codeRef == null) return false;
		else
			try {
				return getSystemData().matchClassifItems(Constants.CODE_CLASSIF_REFERENTS, codeRef, new Date());
			} catch (DbErrorException e) {
//				LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
//				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
				return false;
			}
		
		
	}
	
	/**
	 *  общосистемни настройки
	 */
	private void allSystemSettings() {
		try {
			setNastrWithEkz(false);
			String param1 = sd.getSettingsValue("delo.docWithExemplars"); // да се работи с екземпляри
			if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
				setNastrWithEkz(true);
			}
					
		 } catch (DbErrorException e) {
			 LOGGER.error("Грешка при извличане на системни настройки! ", e);
			 setNastrWithEkz(false);
			 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
	}
	

	//*****************************************************************************************************************
	//               Exports  opis - Html, Word, Excel
	//*****************************************************************************************************************
	    public String htmlExport() throws IOException, ObjectNotFoundException, DbErrorException {	
           exportOpis(OmbConstants.EXPORT_HTML) ;
		   return null;
		}
	
		
		public String wordExport()  throws IOException, ObjectNotFoundException, DbErrorException {	
			 exportOpis(OmbConstants.EXPORT_WORD) ;
			   return null;
		}
		
		 public String excelExport()  throws IOException, ObjectNotFoundException, DbErrorException {
			   exportOpis(OmbConstants.EXPORT_EXCEL) ;
			   return null;
			
		}
		 
		 public void exportOpis (int expType) throws IOException, ObjectNotFoundException, DbErrorException {
					
//			String  title =  getMessageResourceString(this.beanmess,"opis.raznOpisNom")+ " ................ / ................ "+getMessageResourceString(this.beanmess,"opis.naDoc") + "  " + getMessageResourceString(this.beanmess,"opis.withZach");
			 String  title =  getMessageResourceString(this.beanmess,"opis.opisWithZach");				 
			  String title1 = getTitlePeriodZach ();
				  
		//	    this.prClearLists = true;    // Това се прави зачисляване на избраните документи преди експорта
		
				OpisiExport pExport = new OpisiExport();
				pExport.opisExport(this.docSelectedAllMOpisT, expType, title, title1, 5, this.registrPotreb, null, null);
				
			   
		 }
		 
		 
		 public String getTitlePeriodZach () {
			 String title2 = "";
			 Date dOt = null, 
					 dDo = null;
			
			 // Получаване интервал за дата на предаване за избраните движения за документи
			 for (Object[] obj : this.docSelectedAllMOpisT) {
				 // Определяне мин и макс за дата на документ
				 Date dDoc = asDate(obj[5]);
				 if (dOt == null && dDo == null) {
					 dOt = dDoc;
					 dDo = dDoc;
//					 continue;
				 } else if (dOt != null && dDo != null) {
				 
					 if (dOt.compareTo(dDoc) > 0) {
						 dOt = dDoc;  
//						 continue;
					 } else if (dDo.compareTo(dDoc) < 0) {
						 dDo = dDoc;  
//						 continue;
					 }
				 }	 
			 }
			 

			 if (dOt != null && dDo != null) {
				  title2 = getMessageResourceString(this.beanmess,"opis.periodDataDoc") + " " + getMessageResourceString(this.beanmess,"general.ot") + " ";
				  title2 +=  new SimpleDateFormat("dd.MM.yyyy").format(dOt) + " "
					 + getMessageResourceString(this.beanmess,"general.do") + " " + new SimpleDateFormat("dd.MM.yyyy").format(dDo); 
			 }
			 
			 return title2;
		 }
	   
	//*****************************************************************************************************************
	  
	public Integer getPeriod() {
		return period;
	}

	public void setPeriod(Integer period) {
		this.period = period;
	}

	public Date getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(Date dateFrom) {
		this.dateFrom = dateFrom;
	}

	public Date getDateTo() {
		return dateTo;
	}

	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}

	

	public LazyDataModelSQL2Array getDocsList() {
		return docsList;
	}

	public void setDocsList(LazyDataModelSQL2Array docsList) {
		this.docsList = docsList;
	}

	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}

	
	public String getTextPredNa() {
		return textPredNa;
	}

	public void setTextPredNa(String textPredNa) {
		this.textPredNa = textPredNa;
	}

	public List<Object[]> getDocSelectedAllM() {
		return docSelectedAllM;
	}

	public void setDocSelectedAllM(List<Object[]> docSelectedAllM) {
		this.docSelectedAllM = docSelectedAllM;
	}

	public List<Object[]> getDocSelectedTmp() {
		return docSelectedTmp;
	}

	public void setDocSelectedTmp(List<Object[]> docSelectedTmp) {
		this.docSelectedTmp = docSelectedTmp;
	}

	public boolean isCheckUserAccess() {
		return checkUserAccess;
	}

	public void setCheckUserAccess(boolean checkUserAccess) {
		this.checkUserAccess = checkUserAccess;
	}

	public HashMap<Integer, Boolean> getMapObrId() {
		return mapObrId;
	}

	public void setMapObrId(HashMap<Integer, Boolean> mapObrId) {
		this.mapObrId = mapObrId;
	}

	public List<Integer> getDocVidList() {
		return docVidList;
	}

	public void setDocVidList(List<Integer> docVidList) {
		this.docVidList = docVidList;
	}

	public List<SystemClassif> getDocsVidClassif() {
		return docsVidClassif;
	}

	public void setDocsVidClassif(List<SystemClassif> docsVidClassif) {
		this.docsVidClassif = docsVidClassif;
	}

	public Integer getUserRegDvij() {
		return userRegDvij;
	}

	public void setUserRegDvij(Integer userRegDvij) {
		this.userRegDvij = userRegDvij;
	}

	
	public Integer getVidSort() {
		return vidSort;
	}

	public void setVidSort(Integer vidSort) {
		this.vidSort = vidSort;
	}



	public Integer getCodeRefCorresp() {
		return codeRefCorresp;
	}

	public void setCodeRefCorresp(Integer codeRefCorresp) {
		this.codeRefCorresp = codeRefCorresp;
	}

	
	public List<Object[]> getDocSelectedAllModal() {
		return docSelectedAllModal;
	}

	public void setDocSelectedAllModal(List<Object[]> docSelectedAllModal) {
		this.docSelectedAllModal = docSelectedAllModal;
	}

	public List<Object[]> getDocSelectedAll() {
		return docSelectedAll;
	}

	public void setDocSelectedAll(List<Object[]> docSelectedAll) {
		this.docSelectedAll = docSelectedAll;
	}

	public List<Object[]> getDocSelectedTmpModal() {
		return docSelectedTmpModal;
	}

	public void setDocSelectedTmpModal(List<Object[]> docSelectedTmpModal) {
		this.docSelectedTmpModal = docSelectedTmpModal;
	}

	
	public LazyDataModelSQL2Array getDocsSelList() {
		return docsSelList;
	}

	public void setDocsSelList(LazyDataModelSQL2Array docsSelList) {
		this.docsSelList = docsSelList;
	}
	
	
	public String getZaglModal() {
		return zaglModal;
	}

	public void setZaglModal(String zaglModal) {
		this.zaglModal = zaglModal;
	}



	public Integer getTipPoluch() {
		return tipPoluch;
	}



	public void setTipPoluch(Integer tipPoluch) {
		this.tipPoluch = tipPoluch;
	}



	public Integer getCodePredNa() {
		return codePredNa;
	}



	public void setCodePredNa(Integer codePredNa) {
		this.codePredNa = codePredNa;
	}



	public String getNamePredNa() {
		return namePredNa;
	}



	public void setNamePredNa(String namePredNa) {
		this.namePredNa = namePredNa;
	}



	public DocSearch getSearchDoc() {
		return searchDoc;
	}



	public void setSearchDoc(DocSearch searchDoc) {
		this.searchDoc = searchDoc;
	}



	public Integer getPeriodR() {
		return periodR;
	}



	public void setPeriodR(Integer periodR) {
		this.periodR = periodR;
	}



	public Integer getPeriodRes() {
		return periodRes;
	}



	public void setPeriodRes(Integer periodRes) {
		this.periodRes = periodRes;
	}



	public Integer getPeriodAnswer() {
		return periodAnswer;
	}



	public void setPeriodAnswer(Integer periodAnswer) {
		this.periodAnswer = periodAnswer;
	}



	public Integer getPeriodAssign() {
		return periodAssign;
	}



	public void setPeriodAssign(Integer periodAssign) {
		this.periodAssign = periodAssign;
	}



	public List<SelectItem> getDocTypeList() {
		return docTypeList;
	}



	public void setDocTypeList(List<SelectItem> docTypeList) {
		this.docTypeList = docTypeList;
	}



	public List<SystemClassif> getDocsRegistriClassif() {
		return docsRegistriClassif;
	}



	public void setDocsRegistriClassif(List<SystemClassif> docsRegistriClassif) {
		this.docsRegistriClassif = docsRegistriClassif;
	}



	public List<SystemClassif> getDocsTaskAssignClassif() {
		return docsTaskAssignClassif;
	}



	public void setDocsTaskAssignClassif(List<SystemClassif> docsTaskAssignClassif) {
		this.docsTaskAssignClassif = docsTaskAssignClassif;
	}



	public List<Object[]> getDocSelectedAllMOpis() {
		return docSelectedAllMOpis;
	}



	public void setDocSelectedAllMOpis(List<Object[]> docSelectedAllMOpis) {
		this.docSelectedAllMOpis = docSelectedAllMOpis;
	}



	public List<Object[]> getDocSelectedAllMOpisT() {
		return docSelectedAllMOpisT;
	}



	public void setDocSelectedAllMOpisT(List<Object[]> docSelectedAllMOpisT) {
		this.docSelectedAllMOpisT = docSelectedAllMOpisT;
	}


	public String getTxtCorrespDoc() {
		return txtCorrespDoc;
	}


	public void setTxtCorrespDoc(String txtCorrespDoc) {
		this.txtCorrespDoc = txtCorrespDoc;
	}




	public Integer getNachinPred() {
		return nachinPred;
	}




	public void setNachinPred(Integer nachinPred) {
		this.nachinPred = nachinPred;
	}




	public String getNameRegistrPotreb() {
		return nameRegistrPotreb;
	}




	public void setNameRegistrPotreb(String nameRegistrPotreb) {
		this.nameRegistrPotreb = nameRegistrPotreb;
	}




	public boolean isShowRegistaturiList() {
		return showRegistaturiList;
	}




	public void setShowRegistaturiList(boolean showRegistaturiList) {
		this.showRegistaturiList = showRegistaturiList;
	}




	public boolean isDisableRegList() {
		return disableRegList;
	}




	public void setDisableRegList(boolean disableRegList) {
		this.disableRegList = disableRegList;
	}




	public String getTriCheckWork() {
		return triCheckWork;
	}




	public void setTriCheckWork(String triCheckWork) {
		this.triCheckWork = triCheckWork;
	}




	public boolean isVisibleCheckWork() {
		return visibleCheckWork;
	}




	public void setVisibleCheckWork(boolean visibleCheckWork) {
		this.visibleCheckWork = visibleCheckWork;
	}




	public Map<Integer, Object> getSpecificsRegister() {
		return specificsRegister;
	}




	public void setSpecificsRegister(Map<Integer, Object> specificsRegister) {
		this.specificsRegister = specificsRegister;
	}




	public Integer getRegistrPotreb() {
		return registrPotreb;
	}




	public void setRegistrPotreb(Integer registrPotreb) {
		this.registrPotreb = registrPotreb;
	}




	public Integer getCodeRefCorrespPred() {
		return codeRefCorrespPred;
	}




	public void setCodeRefCorrespPred(Integer codeRefCorrespPred) {
		this.codeRefCorrespPred = codeRefCorrespPred;
	}




	public String getTxtCorrespPred() {
		return txtCorrespPred;
	}




	public void setTxtCorrespPred(String txtCorrespPred) {
		this.txtCorrespPred = txtCorrespPred;
		setNameCorrespPred(txtCorrespPred);
	}




	public String getNameCorrespPred() {
		return nameCorrespPred;
	}




	public void setNameCorrespPred(String nameCorrespPred) {
		this.nameCorrespPred = nameCorrespPred;
	}




	public String getNameCorrespDoc() {
		return nameCorrespDoc;
	}




	public void setNameCorrespDoc(String nameCorrespDoc) {
		this.nameCorrespDoc = nameCorrespDoc;
	}




	public List<SelectItem> getCalendarList() {
		return calendarList;
	}




	public void setCalendarList(List<SelectItem> calendarList) {
		this.calendarList = calendarList;
	}




	public boolean isPrClearLists() {
		return prClearLists;
	}




	public void setPrClearLists(boolean prClearLists) {
		this.prClearLists = prClearLists;
	}




	public boolean isNastrWithEkz() {
		return nastrWithEkz;
	}




	public void setNastrWithEkz(boolean nastrWithEkz) {
		this.nastrWithEkz = nastrWithEkz;
	}




	public Integer getNomEkzPred() {
		return nomEkzPred;
	}




	public void setNomEkzPred(Integer nomEkzPred) {
		this.nomEkzPred = nomEkzPred;
	}

	
		
}


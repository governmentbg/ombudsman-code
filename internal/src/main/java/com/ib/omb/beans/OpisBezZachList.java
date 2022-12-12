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
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.component.export.PDFOptions;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.OpisDAO;
import com.ib.omb.db.dao.RegistraturaDAO;
import com.ib.omb.db.dto.Registratura;
import com.ib.omb.print.OpisiExport;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectNotFoundException;
import com.ib.system.utils.DateUtils;

@Named
@ViewScoped
public class OpisBezZachList extends IndexUIbean implements Serializable  {

	/**
	 * Списък с предадени документи за формиране на опис без зачисление
	 * 
	 */
	private static final long serialVersionUID = 6129774408749824460L;
	private static final Logger LOGGER = LoggerFactory.getLogger(OpisBezZachList.class);
	
	private int tipOpis = 1;    // Тип на описа - 1 - разносен без зачисляване;  2 - опис за куриер

	private DocSearch docSearch;         // Компонент за търсене на документи - не се използува
	private boolean checkUserAccess;
	private Date decodeDate=new Date();
	private List<Integer> codesPredNaList = new ArrayList<Integer> ();
	private List<SystemClassif> listPredNaClassif;
	private List<Integer> docVidList = new ArrayList<Integer> ();
	private List<SystemClassif> docsVidClassif;
	private Integer userRegDvij;
	
	/**
	 * настройки на системата
	 */
	private boolean nastrWithEkz;    // true - ще има номера на екземпляри; false - няма номера на екземпляри
	private SystemData sd;	
	 private UserData ud;
	
	private String textPredNa = null;
	private List<Integer> nachiniPred = new ArrayList<Integer>();   // Начини на предаване за предадени документи
	private Integer vidSort = Integer.valueOf(0);
	
	private String zaglModal = null;
	
	private Integer period;	
	private Date dateFrom;
	private Date dateTo;  
	
	// За избор на кореспондент
	// За единичен избор
	private Integer codeRefCorresp = null;
	private String nameCorresp = null;
	private String txtCorresp = null;
	
	// За множествен избор на кореспонденти
	private List<SystemClassif> scList = new ArrayList<SystemClassif>();
	private List<Integer> codeRefCorrespList;
	private Integer codeRefCorrSrch;
	
	
	private Integer idUser = null;
	private Integer registrPotreb = null;
	private String nameRegistrPotreb = null;
	
	private List<Object[]> docSelectedAllModal=new ArrayList<Object[]>();   // Тук остават селектираните документи от модалния прозорец, които се прехвърлят в  docSelectedAllM
	private List<Object[]> docSelectedTmpModal=new ArrayList<Object[]>();
	private List<Object[]> docSelectedAll = new ArrayList<Object[]>();      // Общо събраните документи
	private List<Object[]> docSelectedAllM=new ArrayList<Object[]>();   // Тук остават селектираните документи за конкретен опис
	private List<Object[]> docSelectedTmp=new ArrayList<Object[]>();
	private List<Object[]> docSelectedAllMT=new ArrayList<Object[]>();   // docSelectedAllM с допълнени текстове за тип док. и вид док.
	private HashMap<Integer, Boolean> mapObrId= new HashMap<Integer, Boolean> ();
	private boolean prClearLists = false;
	
	private LazyDataModelSQL2Array docsList;             // Избрани документи в модален панел за избор на документи при един пас
	private LazyDataModelSQL2Array docsSelList;          // Събрани всички избрани документи в основния екран
	
	private  SelectMetadata smdDocsList = null;          // Формиран smd за docsList при последния избор на предадени документи
		  
	
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
			this.setNameRegistrPotreb(r.getRegistratura());
		} catch (DbErrorException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			this.setNameRegistrPotreb(null);
		}
		
		// Четене на параметри
		
//		FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
//		String param3 = (String) faceletContext.getAttribute("isView"); // 0 - актуализациял 1 - разглеждане
//		Integer isView=0;
//		if(!SearchUtils.isEmpty(param3)) {
//			isView = Integer.valueOf(param3);
//		}
		
		// По подразбиране се приема опис без зачисляване
		String param = JSFUtils.getRequestParameter("tipOp");
		if (param == null)  this.tipOpis = 1;    // Опис без зачисляване
		else {
			Integer par = Integer.valueOf(param);
			if (par == null) 
				this.tipOpis = 1; 
			else {
				this.tipOpis = par.intValue();
				if (this.tipOpis != 1 && this.tipOpis != 2)  this.tipOpis = 1;
			}
		}
		
		// Тип на описа
		
		if (this.tipOpis == 2) {  // Опис за куриер
			 this.nachiniPred.add(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_PREDAVANE_KURIER));    // Търсят се документи, предадени на ръка
			 this.nachiniPred.add(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_PREDAVANE_POSHTA));    // И предадени по поща
		} else {	
		      this.nachiniPred.add(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA));    // Търсят се документи, предадени на ръка
		}      
		
		
	
		
//		try {
//			
//			String vidDoc = getSystemData().getSettingsValue("delo.destructionProtocol");
//			this.docVid = Integer.valueOf(vidDoc);
//		
//		} catch (BaseException e) {
//			LOGGER.error("Грешка при зареждане на вид на документ 'протокол за унищожаване на документи'! ", e);		
//		}
		
		actionClear();
	}
	
	
	
	/**
	 * Проверка за въведени дати
	 * @return
	 */
	public boolean provDates () {
		boolean prRet = true;
		if (this.dateFrom == null && this.dateTo == null) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString("beanMessages", "opis.noDates"));					
			prRet = false;
		}
		if (this.dateFrom != null && this.dateTo != null  && this.dateFrom.compareTo(this.dateTo) > 0 ) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString("beanMessages", "opis.errInpDates"));
			prRet = false;
		}
		
		return prRet;  
	}
	
	/**
	 *  Бутон ИЗТРИВАНЕ  За основнияя екран
	 */
	public void actionClearAll() {		
		actionClear();
		clearResults();
		this.docSelectedAll.clear();
	}
		
	/**
	 * премахва избраните критерии за търсене - в модалния панел
	 */
	public void actionClear() {		
		clearSelDocs();
		clearResultsModal();
		    
	}
	
	public void clearSelDocs () {
		changePeriod();
		this.period = null;
		this.dateFrom = null;
		this.dateTo = null;
		this.codesPredNaList = new ArrayList<Integer> ();
		this.listPredNaClassif = new ArrayList<SystemClassif>();
		this.textPredNa = null;
		this.docVidList = new ArrayList<Integer> ();
		this.docsVidClassif = new ArrayList<SystemClassif>();
		this.userRegDvij = null;
		this.setVidSort(Integer.valueOf(0));
		this.prClearLists = false;
	     
		actionClearCorresp();
		actionClearCorrespList();
	
				
//		setDocSearch(new DocSearch(getUserData(UserData.class).getRegistratura()));
//		setCheckUserAccess(getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW));
		
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
		this.docSelectedAllMT.clear();
		this.mapObrId.clear();
		this.prClearLists = false;
	}
	
	public void actionClearCorresp () {    // За единичен избор на кореспондент
		this.codeRefCorresp = null;
		this.nameCorresp = null;
		this.txtCorresp = null;
	
	}
	
	public void actionClearCorrespList () {   // За множествен избор на кореспонденти
		this.scList.clear();
		 this.codeRefCorrespList = null;
		 this.codeRefCorrSrch = null;
	}
	
	// За избор на 	един кореспондент
    public void actionSelectCorresp() {
		
		if(getCodeRefCorresp()!=null) {
			try {
				txtCorresp = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS, getCodeRefCorresp(), getCurrentLang(), decodeDate);
				setNameCorresp(null);
				
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при разкодиране на кореспондент! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}	
	 }
	
	public void actionChangeTxtCorresp() {
		
		setCodeRefCorresp(null) ;
		setNameCorresp(txtCorresp);
	}
	
	// За множествен избор на кореспонденти
	public  void actionAddSelectRef(){
		LOGGER.info("TODO - да се прехвърли избрания код в selectManyModalA");
		if(codeRefCorrSrch != null) {
			if (this.codeRefCorrespList == null) this.codeRefCorrespList = new ArrayList<Integer> ();
			String tekst;
			try {
				SystemClassif tmpSc = new SystemClassif();
				tmpSc.setCodeClassif(OmbConstants.CODE_CLASSIF_REFERENTS);
				tmpSc.setCode(codeRefCorrSrch);
				tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS, codeRefCorrSrch, getUserData().getCurrentLang(), new Date());
				tmpSc.setTekst(tekst);
				scList.add(tmpSc);
				
				this.codeRefCorrespList.add(codeRefCorrSrch);
			} catch (DbErrorException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			codeRefCorrSrch = null;
		}
		
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
	     if (this.docSelectedAll.size() == 0) {
	    	 clearResults ();	
	    	 return;
	     }
	     actionSearchAllDocsBezClearSaveId();
		 return;
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
			if (idDvijRow.intValue() == idDvij.intValue())   // Този ред сеизтрива 
			  continue;
			else {
				listNew.add(obj);
				continue;
			}
		}
		
		return listNew;
		
	}
	
//	public String actionGoto(Integer idObj) {
//
//		String result = "docDestruction.jsf?faces-redirect=true&idObj=" + idObj;			
//		
//		return result;
//	}
//	
//	public String actionGotoNew() {
//		
//		String result = "docDestruction.jsf?faces-redirect=true";		
//		
//		return result;
//	}
	
	
	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	public void changePeriod() {
		
    	if (this.period != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.period);
			setDateFrom(di[0]);
			setDateTo(di[1]);				
    	} else {
    		setDateFrom(null);
    		setDateTo(null);
		}
    }
	
	public void changeDate() { 
		this.setPeriod(null);
	}

	
	//*******************************************************************************************************************
	
	/**
	 * Отваряне на модалния екран за търсене на документи
	 */
	public void actionSelDocs() {

		
		clearResultsModal();
				
		this.zaglModal = getMessageResourceString("labels","opis.selPredDoc") + " ";
		
		if (this.tipOpis == 2)
			this.zaglModal += "(" + getMessageResourceString("labels","opis.cur") + ") "; 
		else	
			this.zaglModal += "(" + getMessageResourceString("labels","opis.bezZach") + ") " ;
					
		actionOpenModalSelDocs();
		
	}
	
	// Управление на модален екран за търсене на предадени документи
	
			public void actionOpenModalSelDocs() {
				
					String  cmdStr = "PF('modalSelDocs').show();";
					PrimeFaces.current().executeScript(cmdStr);	
						
			}
			
			
			/** 
			 * Списък с  документи по зададени критерии за съставяне на опис без зачисляване - изпълнява се в модалния прозорец
			 * 
			 */
			public void actionSearchInModal(){
				if (!provDates()) return;
				
				this.smdDocsList = null;
				clearResultsModal();
				
				// За избор на един кореспондент
//				if (this.codeRefCorresp == null )
//				   smd = new OpisDAO(getUserData()).createSelectOpisBezZach (this.nachiniPred, this.registrPotreb, this.dateFrom, this.dateTo, this.codesPredNaList, this.textPredNa, this.docVidList, this.userRegDvij, this.docSelectedAll );
//				else {
//					List<Integer> predNaList = new ArrayList<Integer> ();
//					if (this.codesPredNaList != null && !this.codesPredNaList.isEmpty())  predNaList.addAll(this.codesPredNaList);
//					predNaList.add(this.codeRefCorresp);
//					smd = new OpisDAO(getUserData()).createSelectOpisBezZach (this.nachiniPred, this.registrPotreb, this.dateFrom, this.dateTo, predNaList, this.textPredNa, this.docVidList, this.userRegDvij,  this.docSelectedAll );
//				}
				
				// При множествен избор на кореспонденти
				if (this.codeRefCorrespList == null || this.codeRefCorrespList.isEmpty())
					this.smdDocsList = new OpisDAO(getUserData()).createSelectOpisBezZach (this.nachiniPred, this.registrPotreb, this.dateFrom, this.dateTo, this.codesPredNaList, this.textPredNa, this.docVidList, this.userRegDvij, this.docSelectedAll );
					else {
						List<Integer> predNaList = new ArrayList<Integer> ();
						if (this.codesPredNaList != null && !this.codesPredNaList.isEmpty())  predNaList.addAll(this.codesPredNaList);
						predNaList.addAll(this.codeRefCorrespList);
						this.smdDocsList = new OpisDAO(getUserData()).createSelectOpisBezZach (this.nachiniPred, this.registrPotreb, this.dateFrom, this.dateTo, predNaList, this.textPredNa, this.docVidList, this.userRegDvij,  this.docSelectedAll );
					}
				
				
				String defaultSortColumn = "A11,A1,A0,A12"; 
						
				this.docsList = new LazyDataModelSQL2Array(this.smdDocsList, defaultSortColumn);	
				return;
				
				
			} 	
			
			
			/**
			 * Връщане от модалния за избор на документи с добавяне на всички новоизброни
			 */
			public void handleSelDocsOKAll () {
				// Първо запис в this.docSelectedAllModal всички намерени документи в this.docs
				if (this.docsList.getRowCount() > 0 ) {
					List<Object[]> listObj = null;
					try {
						listObj = new OpisDAO(getUserData()).findDataWithSqlString(this.smdDocsList.getSql(), "A11,A1,A0,A12", false);
						
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
				if (this.docSelectedAllModal.size() > 0) {
					for (Object[] obj : this.docSelectedAllModal)
						this.docSelectedAll.add(obj);
				}
				
				actionSearchAllDocs();	   // Търсене на документи по id на движенията с обновения списък
				
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
	 * Търсене на предадени документи по id от DOC_DVIJ
	 */
	public void actionSearchAllDocs(){
		
		clearResults();
		 SelectMetadata smd = null;
		 smd = new OpisDAO(getUserData()).createSelectOpisBezZach (this.docSelectedAll);
	
		String defaultSortColumn = "A11,A1,A0,A12";   
		this.docsSelList = new LazyDataModelSQL2Array(smd, defaultSortColumn);	
		
		return;
		
		
	} 	
	
	/**
	 * Търсене на предадени документи по id от DOC_DVIJ със запазване на запазени вече id за формирани описи
	 */
	public void actionSearchAllDocsBezClearSaveId(){
		
		this.docsSelList = null;
		 SelectMetadata smd = null;
		 smd = new OpisDAO(getUserData()).createSelectOpisBezZach (this.docSelectedAll);
	
		String defaultSortColumn = "A11,A1,A0,A12";   
		this.docsSelList = new LazyDataModelSQL2Array(smd, defaultSortColumn);
		actionRazm();
		return;
		
		
	} 
	

	
	
//  Избрани записи за опис - те са в this.docSelectedAllM
	
	public void actionIzbrForOpis() {
//		setMapObrId ();
//		this.docSelectedAllM.clear();
//		this.docSelectedTmp.clear();
		
		this.docSelectedAllMT.clear();
		this.docSelectedAllMT = addTekstsForClass(this.docSelectedAllM);
	   	doSortForOpis(this.docSelectedAllMT, 0);
		this.setVidSort(Integer.valueOf(0));

		this.prClearLists = false;
		this.zaglModal = getMessageResourceString("labels","opis.izbrPredDoc") + " ";
		if (this.tipOpis == 2)
			this.zaglModal += "(" + getMessageResourceString("labels","opis.cur") + "): "; 
		else	
			this.zaglModal += "(" + getMessageResourceString("labels","opis.bezZach") + "): " ;
		
		this.zaglModal += String.valueOf(this.docSelectedAllM.size());	
		
		actionOpenModalFormOpis();
		
	}
	
	/**
	 * Запазване на id на движения за  документи, формирали вече опис
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
				// TODO Auto-generated catch block
				obj1[obj.length] =(String)asInteger(obj[6]).toString();
			}   //  Разкодиране Тип док.
			
			try {
				obj1[obj.length+1] = (String)getSystemData().decodeItem(Integer.valueOf(OmbConstants.CODE_CLASSIF_DOC_VID), asInteger(obj[7]), getCurrentLang(), new Date());
			} catch (DbErrorException e) {
				// TODO Auto-generated catch block
				obj1[obj.length+1] = (String)asInteger(obj[7]).toString();
			}   //  Разкодиране Тип док.
			
			
		
			objListNew.add(obj1);
		}
		
		if (objListNew.size() == 0) objListNew.addAll(objList);
		
		return objListNew;
		
	}
	
	/**
	 * Сортиране на избрани за опис  записи
	 */
	
	public void actionSort () {
		
		if (this.docSelectedAllMT.isEmpty() || this.vidSort == null )  return;
		doSortForOpis(this.docSelectedAllMT, this.vidSort.intValue());
				
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
		
		this.docSelectedAllMT.clear();
		if (this.prClearLists) {
			setMapObrId ();
			this.docSelectedAllM.clear();
			this.docSelectedTmp.clear();
			this.prClearLists = false;
		}		
	}
	
	//********************************************************************************************************************
	
	//  Избор на документи в модалния modalSelDocs 
	/*
	 * Множествен избор на документи 
	 */
	/**
	 * Избира всички редове от текущата страница - избор на документи
	 * @param event
	 */
	  public void onRowSelectAllModal(ToggleSelectEvent eventT) {    
    	List<Object[]> tmpL = new ArrayList<>();
    	tmpL.addAll(getDocSelectedAllModal());
    	if(eventT.isSelected()) {
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
	    	List<Object[]> tmpLPageC =  docsList.getResult(); // rows from current page....    		
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
	 * Премахва маркиране за въведени документи - на основния екран със събраните документи
	 */
	   public void actionRazm () {
			
			this.docSelectedAllM.clear();
			this.docSelectedTmp.clear();
			this.docSelectedAllMT.clear();
			
		}	
	
	/*
	 * Множествен избор на документи за опис
	 */
	/**
	 * Избира всички редове от текущата страница - - за страницата в модалния, която съдържа всички избрани документи на няколко паса
	 * @param event
	 */
	  public void onRowSelectAll(ToggleSelectEvent event) {    
    	List<Object[]> tmpL = new ArrayList<>();
    	tmpL.addAll(getDocSelectedAllM());
    	if(event.isSelected()) {
    		for (Object[] obj : getDocSelectedTmp()) {
    			if  (obj != null && obj.length > 0) {
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
				if  (obj != null && obj.length > 0) {
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
    	;
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
    
//    public void onCancelRelation(CloseEvent event) {
//		event = null;
//		
//	}
    
    
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
		if (this.tipOpis == 2)
		   title = getMessageResourceString(LABELS, "opis.zaglCurier");
		else
			title = getMessageResourceString(LABELS, "opis.zaglRaznBez");	
		
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfo() , null, null);		
     
	}
	

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = null;
			if (this.tipOpis == 2)
			   title = getMessageResourceString(LABELS, "opis.zaglCurier");
			else
				title = getMessageResourceString(LABELS, "opis.zaglRaznBez");
			
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
//			dopInf[0] = getMessageResourceString("beanMessages","opis.period") + ": " + DateUtils.printDate(this.dateFrom) + " - "+ DateUtils.printDate(this.dateTo);
//		} else {
//			if (this.dateFrom != null) dopInf[0] = getMessageResourceString("beanMessages","opis.period") + " " + getMessageResourceString("beanMessages","general.ot") + " " + DateUtils.printDate(this.dateFrom);
//			else if (this.dateTo != null) dopInf[0] = getMessageResourceString("beanMessages","opis.period") + " " + getMessageResourceString("beanMessages","general.do") + " " +  DateUtils.printDate(this.dateTo);
//		}
	   
	
//		return dopInf;
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
			//  String title =  getMessageResourceString(LABELS, "opis.zaglRaznBez");
			 String title = "Опис";
			 if (this.tipOpis == 1)
//			    title =  getMessageResourceString("beanMessages","opis.raznOpisNom")+ " ................ / ................ "+getMessageResourceString("beanMessages","opis.naDoc") + "  " + getMessageResourceString("beanMessages","opis.bezZach");
				 title = getMessageResourceString("beanMessages","opis.opisBezZach");
			 else if (tipOpis == 2)
//				 title =  getMessageResourceString("beanMessages","opis.curier");
				 title =  getMessageResourceString("beanMessages","opis.opisCur");
				 
			  String title1 = getTitlePeriodZach ();
				  
			    this.prClearLists = true; 
		
				OpisiExport pExport = new OpisiExport();
				pExport.opisExport(this.docSelectedAllMT, expType, title, title1, this.tipOpis, this.registrPotreb, null, null);
						         
			    return; 
			 
			 
		 }
		 
		 
		 public String getTitlePeriodZach () {
			 String title2 = "";
			 Date dOt = null, dDo = null;
			
			 // Получаване интервал за дата на предаване за избраните движения за документи
			 for (Object[] obj : this.docSelectedAllMT) {
				 // Определяне мин и макс за дата на предаване
				 Date dPred = asDate(obj[8]);
				 if (dOt == null && dDo == null) {
					 dOt = dPred;
					 dDo = dPred;
					 continue;
				 } else if (dOt != null && dDo != null) {
				 
					 if (dOt.compareTo(dPred) > 0) {
						 dOt = dPred;  continue;
					 } else if (dDo.compareTo(dPred) < 0) {
						 dDo = dPred;
						 continue;
					 }
				 }	 
			 }
			 
//			 if (this.dateFrom != null && this.dateTo != null) {
//				  title2 = getMessageResourceString("beanMessages","opis.periodPredOt") + " " ;
//				  title2 +=  new SimpleDateFormat("dd.MM.yyyy").format(this.dateFrom) + " "
//					 + getMessageResourceString("beanMessages","general.do") + " " + new SimpleDateFormat("dd.MM.yyyy").format(this.dateTo); 
//			 } else {
//			   if (this.dateFrom != null) {
//			      title2 = getMessageResourceString("beanMessages","opis.periodPredOt") + " " ;
//			      title2 +=  new SimpleDateFormat("dd.MM.yyyy").format(this.dateFrom) + " ";
//				  
//			   } else if (this.dateTo != null) {
//				   title2 = getMessageResourceString("beanMessages","opis.periodPredDo") + " " ;
//				      title2 +=   " " + new SimpleDateFormat("dd.MM.yyyy").format(this.dateTo); 
//			   }
//			 }

			 if (dOt != null && dDo != null) {
				  title2 = getMessageResourceString("beanMessages","opis.periodPredOt") + " " ;
				  title2 +=  new SimpleDateFormat("dd.MM.yyyy").format(dOt) + " "
					 + getMessageResourceString("beanMessages","general.do") + " " + new SimpleDateFormat("dd.MM.yyyy").format(dDo); 
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

	public List<SystemClassif> getListPredNaClassif() {
		return listPredNaClassif;
	}

	public void setListPredNaClassif(List<SystemClassif> listPredNaClassif) {
		this.listPredNaClassif = listPredNaClassif;
	}

	public List<Integer> getCodesPredNaList() {
		return codesPredNaList;
	}

	public void setCodesPredNaList(List<Integer> codesPredNaList) {
		this.codesPredNaList = codesPredNaList;
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

	public DocSearch getDocSearch() {
		return docSearch;
	}

	public void setDocSearch(DocSearch docSearch) {
		this.docSearch = docSearch;
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

	public List<Object[]> getDocSelectedAllMT() {
		return docSelectedAllMT;
	}

	public void setDocSelectedAllMT(List<Object[]> docSelectedAllMT) {
		this.docSelectedAllMT = docSelectedAllMT;
	}

	public Integer getCodeRefCorresp() {
		return codeRefCorresp;
	}

	public void setCodeRefCorresp(Integer codeRefCorresp) {
		this.codeRefCorresp = codeRefCorresp;
	}

	public String getNameCorresp() {
		return nameCorresp;
	}

	public void setNameCorresp(String nameCorresp) {
		this.nameCorresp = nameCorresp;
	}

	public String getTxtCorresp() {
		return txtCorresp;
	}

	public void setTxtCorresp(String txtCorresp) {
		this.txtCorresp = txtCorresp;
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
	
	public int getTipOpis() {
		return tipOpis;
	}

	public void setTipOpis(int tipOpis) {
		this.tipOpis = tipOpis;
	}

	public String getZaglModal() {
		return zaglModal;
	}

	public void setZaglModal(String zaglModal) {
		this.zaglModal = zaglModal;
	}

	public List<Integer> getCodeRefCorrespList() {
		return codeRefCorrespList;
	}

	public void setCodeRefCorrespList(List<Integer> codeRefCorrespList) {
		this.codeRefCorrespList = codeRefCorrespList;
	}

	public List<SystemClassif> getScList() {
		return scList;
	}

	public void setScList(List<SystemClassif> scList) {
		this.scList = scList;
	}

	public Integer getCodeRefCorrSrch() {
		return codeRefCorrSrch;
	}

	public void setCodeRefCorrSrch(Integer codeRefCorrSrch) {
		this.codeRefCorrSrch = codeRefCorrSrch;
	}



	public String getNameRegistrPotreb() {
		return nameRegistrPotreb;
	}



	public void setNameRegistrPotreb(String nameRegistrPotreb) {
		this.nameRegistrPotreb = nameRegistrPotreb;
	}



	public boolean isNastrWithEkz() {
		return nastrWithEkz;
	}



	public void setNastrWithEkz(boolean nastrWithEkz) {
		this.nastrWithEkz = nastrWithEkz;
	}
	
	
	
}
package com.ib.omb.beans;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_SECTION_INTERNAL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN;
import static com.ib.system.utils.SearchUtils.isEmpty;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DeloDeloDAO;
import com.ib.omb.db.dao.DeloDocDAO;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloDelo;
import com.ib.omb.db.dto.DeloDoc;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.search.DeloSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;



/**
 * Влагане на документ в преписка - актуализация на документ
 *
 * @author rosi
 */
@Named
@ViewScoped
public class DocDataPrep   extends IndexUIbean  implements Serializable {

	/**  */
	private static final long serialVersionUID = 8191901936895268740L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DocDataPrep.class);
	public static final String  DOCFORMTABS = "docForm:tabsDoc";
	public static final String  MSGPLSINSV = "general.pleaseInsertV";
	
		
	private Date decodeDate = new Date();	
	private Doc docEdit;

	private DeloDoc deloDocPrep; 
	private DeloDelo deloDelo;
	
	private transient  Object[] selectedDeloP;					// таб "влагане в преписка" - връща резултата при избор на преписка за влагане на док.
	private String rnFullDoc; 
	private transient  DeloDocDAO	deloDocDao;
	private boolean endDelo;
	private boolean endTask; 					//  да се приключат ли задчите с приключване на преписката 
	private boolean accessFinishTask; 				// true -  има право да приключва задачи
	
	private boolean endDeloDelo;
	private LazyDataModelSQL2Array prepList; 		// списък преписки, в които е вложен документа 
	private SelectMetadata sm; 
	private boolean viewPrepList;
	private String deloName1;
	private String deloName2;
	private SystemData sd;	
	
	
	/**
	 * настройки на системата
	 */
	/**
	 * с екземпляри
	 */
	private boolean nastrWithEkz;
	/**
	 * с томове
	 */
	private boolean nastrWithTom;
	/**
	 * с раздели на преписки
	 */
	private boolean nastrWithSection;
	
	/**
	 * док. в преписка - брой томове в избраната преписка/дело - 
	 */
	private Integer tmpBrTom;
	/**
	 * преписка в дело - брой томове в избраната преписка/дело
	 */
	private Integer tmpBrTom2;


	/**
	 * При отваряне на таба - винаги минава от тук
	 */
	public void initTab() {
		DocData bean = (DocData) JSFUtils.getManagedBean("docData"); 
		
		if(bean != null && !Objects.equals(bean.getRnFullDoc(), rnFullDoc)){ // значи за 1-ви път се отваря табчето 
				sd = (SystemData) getSystemData();
				deloDocDao = new DeloDocDAO(getUserData());
				docEdit =   bean.getDocument();
				rnFullDoc = bean.getRnFullDoc();
				nastrWithEkz = bean.isNastrWithEkz(); // дали системата работи с екземляри
				deloSystemSettings();				  // настройки за регистратура - томове и раздели!	
				initDeloDocLink();	
				sm = deloDocDao.createSelectDeloListByDoc(docEdit.getId()); // списък преписки, в които е вложен документа
				prepList = new LazyDataModelSQL2Array(sm, "a0");		
			    Integer br = prepList.getRowCount();
				if(br > 0) { // да се зареди директно първата в списъка
					//намерена е една преписка
					List<Object[]> result = prepList.load(0, prepList.getRowCount(), null, null);
					loadDataFromPrepList(result.get(0));				
				} 
				if(br > 1) {
					viewPrepList = true;
				} else {
					viewPrepList = false;
				}
		}
	}

	
	/**
	 *  общосистемни настройки
	 */
	private void deloSystemSettings() {
		try {
			nastrWithTom = false;
			String param1 = sd.getSettingsValue("delo.deloWithToms"); // да се работи с томове
			if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
				nastrWithTom = true;
			}
			
			nastrWithSection = false;
			param1 = sd.getSettingsValue("delo.deloWithSections"); // да се работи с раздели
			if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
				nastrWithSection = true;
			}
		 } catch (DbErrorException e) {
			 LOGGER.error("Грешка при извличане на системни настройк! ", e);
			 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
	}

  /**
   *  Инициализира / Премахва отлагането на документа в преписката - при ръчно изписване на номера на документа
   */
   public void initDeloDocLink() {
	   selectedDeloP = null;
	   deloDocPrep = new DeloDoc();	   
	   deloDocPrep.setDelo(new Delo());
	   deloDocPrep.setEkzNomer(1);
	   deloDocPrep.setInputDate(new Date());
	   deloName1 = null;
	   deloName2 = null;
	   tmpBrTom = null;
   }
   
	
   /**
   * Инициализира / Премахва отлагането на преписка в преписката 
   */
   public void initDeloDeloLink2() {
	    selectedDeloP = null;
	   	deloDocPrep.setDeloDelo(new DeloDelo());
	   	deloDocPrep.getDeloDelo().setDelo(new Delo());
	   	deloDocPrep.getDeloDelo().setInputDate(new Date());
	    deloName2 = null;
	    tmpBrTom2 = null;
	}
   
   public void actionNew(){	   
	   initDeloDocLink();
	   initDeloDeloLink2();
	   if(prepList.getRowCount()==1) {
		   viewPrepList = true; //да се покаже списъка, защото ако има само един ред в него - не се вижда
	   }
   }
   
  
	/**
	 * [0]-DELO_DOC.ID<br>
	 * [1]-INPUT_DATE<br>
	 * [2]-EKZ_NOMER<br>
	 * [3]-TOM_NOMER<br>
	 * [4]-DELO_ID<br>
	 * [5]-INIT_DOC_ID<br>
	 * [6]-RN_DELO<br>
	 * [7]-DELO_DATE<br>
	 * [8]-STATUS<br>
	 * [9]-STATUS_DATE<br>
	 * [10]-DELO_TYPE<br>
	 * [11]-DELO_NАМЕ<br>
	 * [12]-SECTION_TYPE<br>
	 * [13]-WITH_SECTION<br>
	 * [14]-WITH_TOM<br>
	 * [15]-PREV_NOM_DELO
	 *
	 */
   public void loadDataFromPrepList(Object[] sDelo) {	  
	
		Integer deloDocId = SearchUtils.asInteger(sDelo[0]);
		try {
			JPA.getUtil().runWithClose(() -> {
				deloDocPrep = deloDocDao.findById(deloDocId); // зареждаме го
				deloDocPrep.setDelo(new Delo());
				// ако преписката е в дело...
				deloDelo =  new DeloDeloDAO(getUserData()).findByDeloId(deloDocPrep.getDeloId());				
			});
			if(deloDelo == null) {
				initDeloDeloLink2(); // препискта на док. не е вложена в дело
			}else {
				deloDocPrep.setDeloDelo(deloDelo);
				deloName2 = deloDelo.getDelo().getDeloName();
			}
		
			// само данните от конкретния ред
			deloDocPrep.getDelo().setRnDelo((String)sDelo[6]);
			deloDocPrep.getDelo().setDeloDate((Date)sDelo[7]);
			deloDocPrep.getDelo().setStatus(SearchUtils.asInteger(sDelo[8]));
			deloDocPrep.getDelo().setStatusDate((Date)sDelo[9]);
			deloDocPrep.getDelo().setDeloType(SearchUtils.asInteger(sDelo[10]));
			deloDocPrep.getDelo().setInitDocId(SearchUtils.asInteger(sDelo[5]));
			
			deloDocPrep.setEkzNomer(SearchUtils.asInteger(sDelo[2]));
			deloDocPrep.setTomNomer(SearchUtils.asInteger(sDelo[3]));
			deloDocPrep.setSectionType(SearchUtils.asInteger(sDelo[12]));
			
			deloDocPrep.getDelo().setWithSection(SearchUtils.asInteger(sDelo[13])); 
			deloDocPrep.getDelo().setWithTom(SearchUtils.asInteger(sDelo[14]));
			deloDocPrep.getDelo().setPrevNomDelo((String)sDelo[15]);
			
			deloName1 = (String)sDelo[11];
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на преписка! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		
		//TODO ако е от минала година???? Съобщение?
	}
   
   /**
    * Търсене на преписка по номер
    * @param rnDelo
    * @param varModal
    * @param rnEQ
    * @param nastr
    * @param inpDate
    * @param docOrPrep
    * @return
    */
   private Object[] searchRnDelo(String rnDelo, String varModal, boolean rnEQ, Date inpDate, int docOrPrep, boolean addDeloAccess) {
	   Object[] sDelo = null;
    
	   DeloSearch  tmp = new DeloSearch(getUserData(UserData.class).getRegistratura()); // регистртура на документа.
	   tmp.setUseDost(false); // да не се ограничава достъпа!! За сега
	   tmp.setRnDelo(rnDelo==null ? "" : rnDelo);
	   tmp.setRnDeloEQ(rnEQ);
	   tmp.setNotRelDocId(docEdit.getId()); // за да изключи преписките, в които вече е вложен документа
	   tmp.setNotRelDeloId(deloDocPrep.getDeloId()); 
	   tmp.setAddDeloAccess(addDeloAccess);
	   
	   tmp.buildQueryComp(getUserData());
	   LazyDataModelSQL2Array lazy =   new LazyDataModelSQL2Array(tmp, "a1 desc");
	   int rc = lazy.getRowCount();
	   if (rc == 0 && rnEQ) {
		   searchRnDeloFail(docOrPrep);	// пълно съвпадение - нищо не е намерено 	   	   
	   } else if(rc == 1 && rnEQ) { 	// пълно съвпадение на номера и един намерен резултат
		   sDelo = searchRnDeloOne(lazy, docOrPrep, inpDate);
	   } else {
		   sDelo = new Object[8];		
		   String  cmdStr = "PF('"+varModal+"').show();";
		   PrimeFaces.current().executeScript(cmdStr);
	   }
	  return sDelo;		  
   }
   
   /**
    * При пълно съвпадение на номера:
    * Не е намерена преписка с посочения номер
    *  @param docOrPrep
    */
   private void searchRnDeloFail(int docOrPrep){
	   if(docOrPrep == 1) {
		   initDeloDocLink(); /// от панела....документ - преписка
	   } else {
		   initDeloDeloLink2(); /// от панела....преписка - преписка
	   }
	   if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Не е намерена преписка с посочения номер!");
	   }
	   JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, "Не е намерена преписка/дело с посочения номер или влагането не е позволено!");
   }
   

	 /**
	 * пълно съвпадение на номера и един намерен резултат
	 * @param lazy
	 * @param docOrPrep
	 * @param inpDate
	 * @return
	 */
   private  Object[]  searchRnDeloOne(LazyDataModelSQL2Array lazy, int docOrPrep, Date inpDate){
	   List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);
	   Object[] sDelo = new Object[8];
	   if(result != null) {
	       sDelo =  result.get(0);		
		   if(docOrPrep == 1) {
			   loadDataFromDeloS(sDelo, inpDate);   ///от панела....документ - преписка
		   } else {
			   loadDataFromDeloS2(sDelo, inpDate);  
		   }
	   }	
	   if (LOGGER.isDebugEnabled()) { 
			LOGGER.debug("Намерена е само една преписка с този рег. номер - данните да се заредят без да излиза модалния");
	   }
	   return sDelo;
   }
  
   
   /**
    * Зарежда данните за избраната преписка 
    * [0]-DELO_ID<br>
	* [1]-RN_DELO<br>
	* [2]-DELO_DATE<br>
	* [3]-STATUS<br>
	* [4]-DELO_NAME<br>
	* [5]-INIT_DOC_ID<br>
	* [6]-REGISTER_ID<br>
	* [7]-DELO_INFO<br>
	* [8]-DELO_TYPE<br>
	* [9]-WITH_SECTION<br>
	* [10]-WITH_TOM<br>
	* [11]-BR_TOM<br>
    */
   private void loadDataFromDeloS(Object[] sDelo, Date inpDate) {	  
	   try {
		deloDocPrep.setDeloId(SearchUtils.asInteger(sDelo[0])); 
		deloDocPrep.getDelo().setDeloDate((Date)sDelo[2]);
		if(inpDate == null) {
			inpDate = new Date();
		}
		deloDocPrep.setInputDate(inpDate);
		deloDocPrep.getDelo().setStatus(SearchUtils.asInteger(sDelo[3]));
		deloDocPrep.getDelo().setRnDelo((String)sDelo[1]);		
		deloDocPrep.getDelo().setDeloType(SearchUtils.asInteger(sDelo[8]));
		deloDocPrep.getDelo().setWithSection(SearchUtils.asInteger(sDelo[9])); 
		deloDocPrep.getDelo().setWithTom(SearchUtils.asInteger(sDelo[10]));
	
		tmpBrTom = SearchUtils.asInteger(sDelo[11]); 
		if(deloDocPrep.getTomNomer() == null || deloDocPrep.getTomNomer().compareTo(tmpBrTom)>0) {	
			deloDocPrep.setTomNomer(1);  // по подразбиране
		}
		
		if(deloDocPrep.getEkzNomer() == null) {	
			deloDocPrep.setEkzNomer(1);  // по подразбиране винаги зареждам екз. номер 1
		}
		
		deloName1 = (String)sDelo[4];
		if(Objects.equals(deloDocPrep.getDelo().getWithSection(),OmbConstants.CODE_ZNACHENIE_DA) &&
				!Objects.equals(docEdit.getDocType(), CODE_ZNACHENIE_DOC_TYPE_IN)) {
			deloDocPrep.setSectionType(CODE_ZNACHENIE_DELO_SECTION_INTERNAL); // за собствени и работни - по подразбиране в раздел "вътрешен"
		}
			
				
		JPA.getUtil().runWithClose(() -> 
			// ако преписката е в дело...
			deloDelo =  new DeloDeloDAO(getUserData()).findByDeloId(deloDocPrep.getDeloId())				
		);	
		if(deloDelo == null) {
			initDeloDeloLink2(); // преписката на док. не е вложена в дело
		}else {
			deloDocPrep.setDeloDelo(deloDelo);
			deloName2 = deloDelo.getDelo().getDeloName();
		}
	
		String msg =  deloDocPrep.getDelo().getRnDelo() +" / "+ DateUtils.printDate(deloDocPrep.getDelo().getDeloDate()) +
						"; "+ sd.decodeItem(OmbConstants.CODE_CLASSIF_DELO_STATUS, deloDocPrep.getDelo().getStatus(), getUserData().getCurrentLang(), new Date());
		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docu.addDeloMsg1", msg) );
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при разкодиране на статус на преписка! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на данни за преписка! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		
		//ако е от минала година???? Съобщение?
	}
   
   
   /**
    * Зарежда данните за избраната преписка -> преписка в дело/преписка
    * [0]-DELO_ID<br>
	* [1]-RN_DELO<br>
	* [2]-DELO_DATE<br>
	* [3]-STATUS<br>
	* [4]-DELO_NAME<br>
	* [5]-INIT_DOC_ID<br>
	* [6]-REGISTER_ID<br>
	* [7]-DELO_INFO<br>
	* [8]-DELO_TYPE<br>
	* [9]-WITH_SECTION<br>
	* [10]-WITH_TOM<br>
	* [11]-BR_TOM<br>
    */
   private void loadDataFromDeloS2(Object[] sDelo, Date inpDate) {	  
		deloDocPrep.setDeloDelo(new DeloDelo());
	   	deloDocPrep.getDeloDelo().setDelo(new Delo());	
	    
	    // prepiska- prepiska...
	
		deloDocPrep.getDeloDelo().setDeloId(SearchUtils.asInteger(sDelo[0])); 
		deloDocPrep.getDeloDelo().getDelo().setDeloDate((Date)sDelo[2]);
		if(inpDate == null) {
			inpDate = new Date();
		}
		deloDocPrep.getDeloDelo().setInputDate(inpDate);
		deloDocPrep.getDeloDelo().getDelo().setStatus(SearchUtils.asInteger(sDelo[3]));
		deloDocPrep.getDeloDelo().getDelo().setRnDelo((String)sDelo[1]);				
		deloDocPrep.getDeloDelo().getDelo().setDeloType(SearchUtils.asInteger(sDelo[8]));

	//	deloDocPrep.getDeloDelo().getDelo().setWithSection(SearchUtils.asInteger(sDelo[9]));  // при валгането на преписка в дело - няма раздели....
		deloDocPrep.getDeloDelo().getDelo().setWithTom(SearchUtils.asInteger(sDelo[10]));
		
		tmpBrTom2 = SearchUtils.asInteger(sDelo[11]); 
		if(deloDocPrep.getDeloDelo().getTomNomer() == null || deloDocPrep.getDeloDelo().getTomNomer().compareTo(tmpBrTom)>0) {	
			deloDocPrep.getDeloDelo().setTomNomer(1); // по подразбиране
			
		}
		
		deloName2 = (String)sDelo[4];
		try {
			String msg =  deloDocPrep.getDeloDelo().getDelo().getRnDelo() +" / "+ DateUtils.printDate(deloDocPrep.getDeloDelo().getDelo().getDeloDate()) +
							"; "+  getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DELO_STATUS, deloDocPrep.getDeloDelo().getDelo().getStatus(), getUserData().getCurrentLang(), new Date());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docu.addDeloMsg2", msg) );
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при разкодиране на статус на преписка! ", e);
		}
		
	}
   
 

	//********************************************** Влагане на док. в преписка / дело */
	
	
	//	deloDocInfo = document.getDeloDoc().getDelo().getRnDelo() +" / "+ DateUtils.printDate(document.getDeloDoc().getDelo().getDeloDate()) ;

	/** таб "Влагане в преписка"
	* Ръчно въвеждане на номер на преписка
	* Търсене на преписка с въведения номер - при излизане от полето
	*/
	public void actionSearchRnDeloP(boolean rnEQ) {	
	   if(!isEmpty(deloDocPrep.getDelo().getRnDelo())) {
		    searchRnDelo( deloDocPrep.getDelo().getRnDelo(),  "mDeloSP",  rnEQ, new Date(), 1, true);
	   }else {
		   initDeloDocLink();
	   }
	}
	
	
	
   /**
    * Tab "Влагане в преписка" 
    * Търси преписка по номер  - бутон "Търси" (Влагане нa преписка в преписка/дело)
    */
   public void actionSearchRnDeloBtnP() {
	   selectedDeloP = null;
	   searchRnDelo( deloDocPrep.getDelo().getRnDelo(),  "mDeloSP",  false,  new Date(), 1, true);
	}
	
	/**
	 *Tab "Влагане в преписка" 
	 * Затваряне на модалния за избор на преписка - ръчно въвеждане на номера на преписката
	 */
   public void actionHideModalDeloP() {
	   if(selectedDeloP != null && selectedDeloP[0] != null) {
		   // да заредя полетата
		   loadDataFromDeloS(selectedDeloP,  deloDocPrep.getInputDate()); 
		   selectedDeloP = null;
	   }
   }
   
   /**
    * Запис на влагане на документ.............
    */
   public void actionSavePrepIn() {

		if(deloDocPrep != null && checkDataPrep()) { 			
			
			try {
				String dvijMsg = "";
				JPA.getUtil().begin();
				
				Object[] newEntries = deloDocDao.saveInDoc(deloDocPrep, this.docEdit, endDelo, endDeloDelo,  endTask);
				
				if (newEntries != null) { // ако има значи има новозаписани влагания, за които трябва да се прави проверка за движенията
					Integer s9 = sd.getRegistraturaSetting(this.docEdit.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_9);

					if (s9 != null && s9.equals(SysConstants.CODE_ZNACHENIE_DA)) {
						if (newEntries[0] != null) { // ново влагане на документ в преписка
							String tmp = deloDocDao.checkCreateDvij((DeloDoc) newEntries[0], sd);
							if (tmp != null) {
								dvijMsg = " " + tmp;
							}
						}
						if (newEntries[1] != null) { // ново влагане на преписка в преписка
							String tmp = new DeloDeloDAO(getUserData()).checkCreateDvij((DeloDelo) newEntries[1], sd);
							if (tmp != null) {
								dvijMsg = dvijMsg + " " + tmp;
							}
						}
					}
				}
				JPA.getUtil().commit();
				
				if (!dvijMsg.equals("")) { // има нови данни
					DocDataDvijenia ddd = (DocDataDvijenia) JSFUtils.getManagedBean("docDataDvijenia");
					if (ddd != null) {
						ddd.setRnFullDoc(null); // има ново движение и трябва да се зареди пак списъка
					}
				}
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) + dvijMsg);
				prepList = new LazyDataModelSQL2Array(sm, "a0"); //да презареди списъка
				if(endDelo) {
					deloDocPrep.getDelo().setStatus(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED);
				}
				if(endDeloDelo) {
					deloDocPrep.getDeloDelo().getDelo().setStatus(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED);
				}
				endTask = false;
				accessFinishTask = false;
				tmpBrTom = null;
				tmpBrTom2 = null;
			} catch (Exception e) {		
				JPA.getUtil().rollback();
				LOGGER.error("Грешка при запис на влагане на документ в преписка! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			} finally {
				JPA.getUtil().closeConnection();
			}
			
		}
   }

  
   /**
    * проверка за валидни данни
    */
   private boolean checkDataPrep() {
	    boolean flagSave = true;	 		
	    Date prepDate = DateUtils.startDate(deloDocPrep.getDelo().getDeloDate());
		Date idate    = DateUtils.startDate( deloDocPrep.getInputDate());	
 	    Date docDate  = DateUtils.startDate(docEdit.getDocDate());
	    if( isEmpty(deloDocPrep.getDelo().getRnDelo())) {
			JSFUtils.addMessage(DOCFORMTABS+":prepN",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.docAddPrep")));
			flagSave = false;
		}
			
		boolean dd = idate == null || prepDate==null || idate.before(docDate) || idate.before(prepDate) || idate.after(new Date()); 
		if(flagSave && dd) {
			JSFUtils.addMessage(DOCFORMTABS+":inpDat_input",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINSV,getMessageResourceString(LABELS, "docu.deloInpDate")));
			flagSave = false;
		}
		
		if(deloDocPrep.getTomNomer() == null) {
			deloDocPrep.setTomNomer(1);
		}
		if(tmpBrTom != null && deloDocPrep.getTomNomer().compareTo(tmpBrTom)>0) {
			JSFUtils.addMessage(DOCFORMTABS+":tomN",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "delo.brTomsMax", tmpBrTom) );
			flagSave = false;
		}
		
		// преписка, в преписка....
		if(flagSave) {
			flagSave = checkDataPrepPrep(prepDate);
		}
	   return flagSave;
   }
	
   /**
    * проверка за валидни данни  - преписка, в преписка....
    * @param prepDate
    * @return
    */
  private boolean checkDataPrepPrep(Date prepDate) {
	    boolean flagSave1 = true;
		Date deloDate =  DateUtils.startDate(deloDocPrep.getDeloDelo().getDelo().getDeloDate());
		Date idate  = DateUtils.startDate( deloDocPrep.getDeloDelo().getInputDate());
		boolean dd = !isEmpty(deloDocPrep.getDeloDelo().getDelo().getRnDelo());
		boolean dd1 = dd && (idate == null || prepDate==null || idate.before(deloDate) || idate.before(prepDate) || idate.after(new Date()));		
		if(dd1) {
			JSFUtils.addMessage(DOCFORMTABS+":inpDatD_input",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINSV,getMessageResourceString(LABELS, "docu.deloInpDate")));
			flagSave1 = false;			
		}
		
		if(deloDocPrep.getDeloDelo().getTomNomer() == null) {
			deloDocPrep.getDeloDelo().setTomNomer(1); // 1-том по подразбиране
		}
		if(tmpBrTom2 != null && deloDocPrep.getDeloDelo().getTomNomer().compareTo(tmpBrTom2)>0) {
			JSFUtils.addMessage(DOCFORMTABS+":tomN2",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "delo.brTomsMax", tmpBrTom2) );
			flagSave1 = false;
		}
	  return flagSave1;
  }
   
   
   /**
    *  Tab "Влагане в преписка" 
    * Изваждане на док. от преписка + директен ЗАПИС на изваждането!
    */
   public void actionDelDocLinkP(){	   
	   try {
	 	    JPA.getUtil().runInTransaction(() -> deloDocDao.deleteInDoc(deloDocPrep, true, false, this.docEdit));
			DocDataDvijenia ddd = (DocDataDvijenia) JSFUtils.getManagedBean("docDataDvijenia");
			if (ddd != null) {
				ddd.setRnFullDoc(null); // може да има промяна в движенията
			}

			initDeloDocLink();
	    	initDeloDeloLink2();
	    	prepList = new LazyDataModelSQL2Array(sm, "a0");   //да презареди списъка	    
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docu.successDeloOutMsg1") );
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изваждане на документ от преписка! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}	  
   }
   
   
   
   //****** ПРЕПИСКА В ДЕЛО*/
   
	/** таб "Влагане в преписка"
	* Ръчно въвеждане на номер на дело/преписка
	* Търсене на преписка с въведения номер - при излизане от полето
	*/
	public void actionSearchRnDeloP2(boolean rnEQ) {	
	   if(!isEmpty(deloDocPrep.getDeloDelo().getDelo().getRnDelo())) {
		    searchRnDelo( deloDocPrep.getDeloDelo().getDelo().getRnDelo(),  "mDeloSPD",  rnEQ,  new Date(), 2, false);
	   }else {
		  initDeloDeloLink2();	 
	   }
	}

	/**
	 *Tab "Влагане в преписка" 
	 * Затваряне на модалния за избор на преписка - ръчно въвеждане на номера дело/преписка
	 */
   public void actionHideModalDeloP2() {
	   if(selectedDeloP != null && selectedDeloP[0] != null) {
		   // да заредя полетата   
		   loadDataFromDeloS2(selectedDeloP,   deloDocPrep.getDeloDelo().getInputDate());  
		   selectedDeloP = null;
	   }
   }
  
   /**
    * Tab "Влагане на преписка в дело/преписка" 
    * Търси преписка по номер  - бутон "Търси" 
    */
   public void actionSearchRnDeloBtnP2() {
	 
	   searchRnDelo( deloDocPrep.getDeloDelo().getDelo().getRnDelo(),  "mDeloSPD",  false, new Date(), 2, false);
	   
   }
   
   /**
    *  Tab "Влагане в преписка" 
    * Изваждане на преписка от преписка/дело + директен ЗАПИС на изваждането!
    */
   public void actionDelDeloLinkP(){
	   if (deloDocPrep.getDelo().getStatus()!=null &&
			(deloDocPrep.getDelo().getStatus()==OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_UA ||
			 deloDocPrep.getDelo().getStatus()==OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_DA)) {
		     //"Преписката е включена в протокол за архивиране и не може да бъде извадена."
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "docu.warnDeloOutMsg1"));
		}else {
		   try {
			   JPA.getUtil().runInTransaction(() -> deloDocDao.deleteInDoc(deloDocPrep, false, true, this.docEdit));
				DocDataDvijenia ddd = (DocDataDvijenia) JSFUtils.getManagedBean("docDataDvijenia");
				if (ddd != null) {
					ddd.setRnFullDoc(null); // може да има промяна в движенията
				}

			   initDeloDeloLink2();	 
			   JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docu.successDeloOutMsg2") );
			} catch (BaseException e) {			
				LOGGER.error("Грешка при изваждане на преписка от преписка! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}	
		}
	   
   }
   
//******************************************/

	
	/**
	 * прикючване на преписката, в която е вложен документа
	 */
	public void actionEndDelo() {
		accessFinishTask = false;
		if(endDelo) {
			try {
				// При приключване на преписка се приключват задачите към нейните документи и вложените преписки - колко навътре става това????
				Integer s1 = sd.getRegistraturaSetting(docEdit.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_10);
				if( Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_DA) &&
					getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_COMPLETE_REGISTRATURA_TASKS)) {
					accessFinishTask = true; 
				}
				
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при извичане на настройка CODE_ZNACHENIE_REISTRATURA_SETTINGS_10 на регистратура: {} ", docEdit.getRegistraturaId(), e);
			}
		} else {
			endTask = false;
		}
	}
	
	public String actionGoto(Integer idObj) {
		return "deloView.xhtml?faces-redirect=true&idObj=" + idObj +"&acc=0";
	}

	public Object[] getSelectedDeloP() {
		return selectedDeloP;
	}


	public void setSelectedDeloP(Object[] selectedDeloP) {
		this.selectedDeloP = selectedDeloP;
	}



	public DeloDoc getDeloDocPrep() {
		return deloDocPrep;
	}


	public void setDeloDocPrep(DeloDoc deloDocPrep) {
		this.deloDocPrep = deloDocPrep;
	}


	public String getRnFullDoc() {
		return rnFullDoc;
	}


	public void setRnFullDoc(String rnFullDoc) {
		this.rnFullDoc = rnFullDoc;
	}


	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}


	public Doc getDocEdit() {
		return docEdit;
	}


	public void setDocEdit(Doc docEdit) {
		this.docEdit = docEdit;
	}


   public DeloDocDAO getDeloDocDao() {
		return deloDocDao;
	}


	public void setDeloDocDao(DeloDocDAO deloDocDao) {
		this.deloDocDao = deloDocDao;
	}


	public boolean isEndDelo() {
		return endDelo;
	}


	public void setEndDelo(boolean endDelo) {
		this.endDelo = endDelo;
	}


	public boolean isEndDeloDelo() {
		return endDeloDelo;
	}


	public void setEndDeloDelo(boolean endDeloDelo) {
		this.endDeloDelo = endDeloDelo;
	}


	public LazyDataModelSQL2Array getPrepList() {
		return prepList;
	}


	public void setPrepList(LazyDataModelSQL2Array prepList) {
		this.prepList = prepList;
	}


	public SelectMetadata getSm() {
		return sm;
	}


	public void setSm(SelectMetadata sm) {
		this.sm = sm;
	}


	public boolean isViewPrepList() {
		return viewPrepList;
	}


	public void setViewPrepList(boolean viewPrepList) {
		this.viewPrepList = viewPrepList;
	}


	public String getDeloName1() {
		return deloName1;
	}


	public void setDeloName1(String deloName1) {
		this.deloName1 = deloName1;
	}


	public String getDeloName2() {
		return deloName2;
	}


	public void setDeloName2(String deloName2) {
		this.deloName2 = deloName2;
	}


	public boolean isEndTask() {
		return endTask;
	}


	public void setEndTask(boolean endTask) {
		this.endTask = endTask;
	}

	public boolean isAccessFinishTask() {
		return accessFinishTask;
	}

	public void setAccessFinishTask(boolean accessFinishTask) {
		this.accessFinishTask = accessFinishTask;
	}


	public SystemData getSd() {
		return sd;
	}


	public void setSd(SystemData sd) {
		this.sd = sd;
	}

	public boolean isNastrWithEkz() {
		return nastrWithEkz;
	}

	public void setNastrWithEkz(boolean nastrWithEkz) {
		this.nastrWithEkz = nastrWithEkz;
	}

	public boolean isNastrWithTom() {
		return nastrWithTom;
	}

	public void setNastrWithTom(boolean nastrWithTom) {
		this.nastrWithTom = nastrWithTom;
	}

	public boolean isNastrWithSection() {
		return nastrWithSection;
	}

	public void setNastrWithSection(boolean nastrWithSection) {
		this.nastrWithSection = nastrWithSection;
	}


	public Integer getTmpBrTom() {
		return tmpBrTom;
	}


	public void setTmpBrTom(Integer tmpBrTom) {
		this.tmpBrTom = tmpBrTom;
	}


	public Integer getTmpBrTom2() {
		return tmpBrTom2;
	}


	public void setTmpBrTom2(Integer tmpBrTom2) {
		this.tmpBrTom2 = tmpBrTom2;
	}
	
}
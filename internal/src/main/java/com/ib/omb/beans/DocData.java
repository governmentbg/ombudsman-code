package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.faces.view.facelets.FaceletContext;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.event.TabChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.navigation.Navigation;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.components.CompAccess;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.DocDocDAO;
import com.ib.omb.db.dao.EgovMessagesDAO;
import com.ib.omb.db.dao.LockObjectDAO;
import com.ib.omb.db.dao.TaskDAO;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloDoc;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocAccess;
import com.ib.omb.db.dto.DocDopdata;
import com.ib.omb.db.dto.DocJalba;
import com.ib.omb.db.dto.DocReferent;
import com.ib.omb.db.dto.EgovMessages;
import com.ib.omb.db.dto.EgovMessagesCoresp;
import com.ib.omb.db.dto.EgovMessagesFiles;
import com.ib.omb.db.dto.Task;
import com.ib.omb.search.DeloSearch;
import com.ib.omb.search.DocSearch;
import com.ib.omb.search.TaskSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.mail.Mailer;
import com.ib.system.mail.Mailer.Content;
import com.ib.system.mail.MyMessage;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.SignatureUtils;
import com.ib.system.utils.VerifySignature;
import com.ib.system.utils.X;

/**
 * Актуализация на документ
 *
 * @author rosi
 */
@Named
@ViewScoped
public class DocData   extends IndexUIbean  implements Serializable {

	/**  */
	private static final long serialVersionUID = 8191901936895268740L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DocData.class);
	public  static final String DOCFORMTABS      = "docForm:tabsDoc";
	public  static final String	OBJECTINUSE		 = "general.objectInUse";
	public  static final String	MSGVALIDDATES	 = "docu.validDates";
	public  static final String TXTREJECTED 	 = "docu.txtRejected";
	public  static final String TXTREJECTEDID   = "docForm:tabsDoc:txtReject";
	
	/**
	 * обработка на стандартен деловоден документ
	 */
	public  static final int INCL_REGULAR = 0;
	
	/**
	 * обработка на жалба
	 */
	public  static final int INCL_COMPLAINT = 1;
	
	/**
	 * заповед за проверка по НПМ
	 */
	public  static final int INCL_NPM = 2;
	
	/**
	 * решение за самосезиране
	 */
	public  static final int INCL_SAMOSEZ= 3;
	/**
	 * за актуализация;
	 */
	public  static final int UPDATE_DOC = 0; 	
	/**
	 * за рег. като офицален;
	 */
	public  static final int REG_WRK_DOC = 1; 
	/**
	 * за приемане от друга регистратура
	 */
	public  static final int REG_OTHER_R = 2; 	
	/**
	 * регистриране от е-мейл
	 */
	public  static final int REG_FROM_MAIL = 3; 
	/**
	 * регистриране от СЕОС или ССЕВ
	 */
	public static final int REG_FROM_EGOV= 4; 
	public static final String S_SEOS = "S_SEOS";
	public static final int REG_FROM_EGOV_JALBA = 44; 
	
	/**
	 * регистриране от е-мейл - жалба
	 */
	public  static final int REG_FROM_MAIL_JALBA = 5; 
	
	/**
	 * Статуси на съобщения - СЕОС, 
	 * @author rosi
	 *
	 */
	private enum EgovStatusType {
		DS_REJECTED,   
	    DS_REGISTERED,
	    DS_WAIT_REGISTRATION
	    //, DS_STOPPED,DS_CLOSED,DS_NOT_FOUND, DS_ALREADY_RECEIVED
	}
	
	
	private Doc document;
	private transient DocDAO	dao;
	private SystemData sd;	
	private UserData ud;
	/**
	 * true -  автоматично генериране на номер
	 */
	private boolean avtomNo = true;
	/**
	 * true - да се забрани достъпа до бутона за автоматичното генериране на номер
	 */
	private boolean avtomNoDisabled = false;          
	
	/**
	 *  свободен достъп - false; ограничен достъп = true
	 */
	private boolean limitedAccessCh = false;  	
	/**
	 * true - да се създаде преписка
	 */
	private boolean createPrep = false;     
	private boolean createPrepOld = false; // използвам го, ако се натисне бутон "нов документ със запазване на данни"  
	/**
	 * true - обработен; false - необработен
	 */
	private boolean processedCh = false;   		 
		
	private Date decodeDate = new Date();
		
	private List<SelectItem>	 docTypeList;	
	private List<SystemClassif>  scTopicList; // заради autocomplete
	
	private List<SystemClassif> classifProceduri; 
	
	private transient Map<Integer, Object> specificsProc;  
	private boolean enableProc;
	private boolean notFinishedProc;

	/**
	 * за списъка с допустими регистри, в зависимост от типа документ
	 */
	private transient Map<Integer, Object> specificsRegister;  
	
	/**
	 *  дали списъкът с регистри да се филтрира в зависимост от правата на потребителя
	 */
	private boolean regUserFiltered;  				 
	/**
	 * списък файлове към документа
	 */
	private List<Files> filesList;
	
	/**
	 * шаблони към вид документ
	 */
	private List<Files> templatesList;
	/**
	 * id от таблицата с настройки по вид документ 
	 */
	private Integer docSettingId;
	
	/**
	 * характер на спец. документ, ако има такъв посочен в описание на характеристиките на документа 
	 */
	private Integer docSettingHar;
	/**
	 * Таб - участници, само, ако е посочено в характеристиките на вид документ
	 */
	private String membersTab;
	/**
	 * ще бъде true само, ако при актуализация на док. е бил сменен вида на документа и предипния вид е имал таб "Участници", в който вече е било записано нещо!
	 * това е нужно, за да може да бъдат изтрити на записа на документа 
	 */
	private boolean membersTabForDel; 
	


	/**
	 * брой официални файлове за изпращане
	 */
	private int countOfficalFiles; 
	private String  txtCorresp;
	/**
	 * преписка, в която се влага документа при ръчно писане на рег. номера - за нов документ
	 */
	private DeloDoc deloDocPrep; 					
	/**
	 * избор на преписка при ръчно въвеждане на номер
	 */

	private transient Object[] selectedDelo;					
	
	/**
	 * Задачи към документ
	 */
	private String rnFullDoc; 
	private LazyDataModelSQL2Array tasksList;
	private Integer idTask;
	private String srokPattern;
	
	/**
	 * "За запознаване" - индивидуални задачи от тип резолюция
	 */
	private List<SystemClassif> rezolExecClassif;
	
	private transient Map<Integer, Object> specificsAdm;	
	private Task rezolTask;
	
	private transient Object[] codeExtCheck;
	
	/**
	 * Указания - "за запознаване" - изричен достъп до документ + указания за нотификацията
	 */
	private String noteAccess;
	
	/**
	 * За работните документи - Готов за регистриране като официален
	 */
	private boolean readyForOfficial= false;   
	private List<SelectItem>	dopRegistraturiList;	

	// Ако за вида док. има посочена регистртура къде да бъде рег. официалния - тя се зарежда в - forRegId
	// Ако за вида док., няма изрично посочена регистртура - зарежда се текущата.
	// Ако потребителя има допълнителни регистртури - излиза списък за избор
	// За регистрация - остава последното избрано!
	
	/**
	 * рег.ном./дата на протокол за унищожаване на документ
	 */
	private String rnFullProtocol;
	
	
	/**
	 * рег.ном./дата на свързания с текущия документ - работен/офицален/от другата регистратура
	 */
	private String rnFullDocOther;
	private boolean fromOtherReg; // ако е от друга регистратура - да скрия линка за разглеждане
	

	private String rnFullDocEdit; 

	/**
	 * Приемане на документи от друга регистратура - id на движението, с което е изпратен документа
	 */
	private Integer sourceRegDvijId;
	
	//private transient Object[] dvijData;
	private Properties propMail;
    private Long messUID;
    private String selectMailBox;
    private String messFromRef;
    private String messSubject;

	/**
	 * за какво действие е извикан екрана
	 */
	private int flagFW;
	private int isView;
		
	private int viewBtnProcessed;  // Бутонът "Обработен" -  за входящ документ да се вижда само ако служителят, който работи (userAccess) има роля деловодител, а за работен документ - деловодител или автор на документа. За собствен документ бутонът не се вижда.
	
	// да проверя, ако отворя за редакция документ, който е в конкретен регистър, а нямам право да въвеждам в този регистър
    //     (има ограничение в правата) - какво се случва!!!! 
	
	/**
	 * Свързан с документ по техен номер
	 */

	private transient List<Object[]> selectedDocsTn; //избрани документи
	private int searchFlagTn=0; // за да предоврати минаването през searchTehNomer два пъти, ако след въвеждане на номер на док. веднага се натисне лупата
	private int tnRez = 0;      // бр. резултати при търсенен по техен номер
	
	/**
	 * настройки на системата
	 */
	private boolean nastrWithEkz;
	private boolean scanModuleExist;
	private boolean nastrZadnaData;
	
	
	/**
	 * Отказ от регистрация - причина за отказ
	 */
	private String textReject;
	

	/**
	 * true - да се изпрати по компетентност
	 */
	private boolean competence;     	
	
	/**
	 * съобщение от СЕОС, ССЕВ
	 */
	private EgovMessages egovMess;
	private List<EgovMessagesCoresp> egovMessCoresp;
	
	/**
	 * справка за достъп
	 */
	private LazyDataModelSQL2Array docAccessList;
	
	/**
	 * да се вижда ли поле "Кореспондент" в собствени документи
	 */
	private boolean showCoresp;
	
	/**
	 * Допуска се въвеждане на съгласувал и подписал в работен документ
	 */
	private boolean editReferentsWRK;
	
	/**
	 * Допуска се въвеждане на начин на подписване за собсвен и  работен документ
	 */
	private Integer showSignMethod;
	
	/**
	 * рег. номер и дата на документа за подпис
	 */
	private String rnFullDocSign;
	
	/**
	 * В зависимост от  типа на кореспондента и правото на потребителя дали да вижда лични данни или не  - какво да се вижда в полето адрес
	 *  
	 */
	private String dopInfoAdres;
	
	
	
	private Integer docVidInclude; // коя страница да вмъкне
	// ако сме в екрана за обикновен деловоден документ - да не се позволява въвеждане на жалба!
	
	private Integer specDocId; // ако има стойност - това е нов работен в преписката на жалбата/ нпм/ самосезиране
	private Integer specVidDoc; // вид док. - жалба/нпм/самосезиране
	private Integer codeClassifVidDoc;
	private List<SystemClassif> specVidDocList;
	private String  specRnFullDoc;
	private String  specRnDoc;
	
	private Integer workDocId; // ид на раб. док., от който се регистрира официалания - за да може новия официален да влезе в преписката на работния!!
	private boolean viewChboxCrPrep;   // дали да се вижда Checkbox за иницииране на нова преписка (изпозлва се при рег. на официален от работен - Checkbox да се скрие, ако преписката на раб. е жалба, нпм, самосез.)
	
	/** */
	@PostConstruct
	void initData() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("PostConstruct!" );
		}
		try {
			long t = System.currentTimeMillis(); //test
			
			codeClassifVidDoc = OmbConstants.CODE_CLASSIF_DOC_VID;
			
			String paramEgov = JSFUtils.getRequestParameter("idEgov"); // рег. на обикновен док. от СЕОС/ССЕВ;
			String paramEgovJ = JSFUtils.getRequestParameter("idEgovJ"); // рег. на жалба. от СЕОС/ССЕВ; 
			// flag от къде идва - 0 -за актуализация; 1 - за рег. като офицален; 2- от друга рег; 3- e-mail ; 5 - e-mail Жалба; 
			String param2 = JSFUtils.getRequestParameter("fw");
			flagFW = UPDATE_DOC; // актуализация на док.
			if(!SearchUtils.isEmpty(param2)) {
				flagFW = Integer.valueOf(param2);
			}else if(!SearchUtils.isEmpty(paramEgov)){
				flagFW = REG_FROM_EGOV;
			}else if(!SearchUtils.isEmpty(paramEgovJ)){
				flagFW = REG_FROM_EGOV_JALBA;
				paramEgov = paramEgovJ; // за да не променям нищо по-надолу
			}
			
			isSpecialDoc();	//  да определи дали е жалба, нпм или самосезеринае
			
			ud = getUserData(UserData.class);					
			sd = (SystemData) getSystemData();
			dao = new DocDAO(getUserData());
			viewChboxCrPrep = true;
			// общосистемни настройки   
			allSystemSettings();
			
			if(docVidInclude == INCL_REGULAR &&  specDocId == null) { // само за обикн. деловoдни док.! 
				// само за нов документ
				// филтрира се в зависимост от правата на потребителя 
				this.docTypeList = createItemsList(true,  OmbConstants.CODE_CLASSIF_DOC_TYPE, this.decodeDate, true);
				// деловодителите да могат да въвеждат работни документи само в тяхната основна регистртура!
				Integer mainRegId = (Integer)sd.getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, ud.getUserId(), OmbConstants.CODE_DEFAULT_LANG, new Date(), OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
				if(!ud.getRegistratura().equals(mainRegId)) {
					this.docTypeList  = this.docTypeList.stream()
					.filter(item -> (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK)
					.collect(Collectors.toList());
				}
			} 
		
			//проверка дали потребителя има права за въвеждане на необходимите типове документи - може да се получи, ако правата му не са зададени правилно! 
			boolean flagA = (flagFW == REG_WRK_DOC || flagFW == REG_OTHER_R ) && checkAccessTypeDoc( OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN);
			if((flagFW == REG_FROM_MAIL || flagFW == REG_FROM_MAIL_JALBA)  && checkAccessTypeDoc( OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN)) {
				// рег. от е-маил
				initDocMail(); 
			} else if (flagFW == UPDATE_DOC || flagA){
				initDoc();	
			} else if ((flagFW == REG_FROM_EGOV  || flagFW == REG_FROM_EGOV_JALBA) && checkAccessTypeDoc( OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN)) {
				initDocEgovMsg(Integer.valueOf(paramEgov));	
			} 

			if(document != null) {
				if (Objects.equals(document.getDocVid(), OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA)) {
					docVidInclude = INCL_COMPLAINT; 
				}else if (Objects.equals(document.getDocVid(), OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM)) {
					docVidInclude = INCL_NPM; 
				}else if (Objects.equals(document.getDocVid(), OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS)) {
					docVidInclude = INCL_SAMOSEZ; 
				}else {
					docVidInclude = INCL_REGULAR; // 0 - всички останали
				}
				docRegSettings(); // настройки на регистратура
			}
			
			selectedDelo = null;		
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("ALL Doc inited: {}ms.", System.currentTimeMillis() - t);
			}
			
		} catch (DbErrorException | UnexpectedResultException e) {
			LOGGER.error("Грешка при зареждане на данни за документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} 
	}

	
	private boolean checkAccessTypeDoc( Integer typeDoc) {
		boolean flagA = false;  
		if(docTypeList != null && docTypeList.size()<3) {
			for  (SelectItem si: docTypeList) {
				if(Objects.equals(si.getValue(), typeDoc)){
					flagA = true;
				}
			}
		} else {
			flagA = true;
		}
		return flagA;
	}
	
	
	
	
	/**
	 * Зарежда данни за документ, ако е нов документ, за актуализация, за приеман от др. регистратура, рег. на офицален от работен 
	 * @return
	 */
	private void initDoc() {
	
		propMail = null;
		String param = JSFUtils.getRequestParameter("idObj");
		if ( SearchUtils.isEmpty(param)){
			actionNewDocument(false, false);	
		} else {
			initDocObj(param);
		}		
		
	}
	
	/**
	 * зарежда данни за обект документ, подаден е idObj
	 * @param paramIdObj
	 */
	private void initDocObj(String paramIdObj) {
		boolean fLockOk = true;
		
		FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
		String param3 = (String) faceletContext.getAttribute("isView"); // 0 - актуализациял 1 - разглеждане
		Integer	docId = Integer.valueOf(paramIdObj);				
		isView = !SearchUtils.isEmpty(param3) ? Integer.valueOf(param3) : 0;
		
		if(isView == 0) { 
			Integer idLock = docId;
			Integer codeObj = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC;
			
			if(flagFW == REG_OTHER_R ) {
				param3 = JSFUtils.getRequestParameter("idDvig");	
				this.sourceRegDvijId = Integer.valueOf(param3);	//id на движението,за да се коригират данните в него! 
				idLock = this.sourceRegDvijId;
				codeObj = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ;
			}
			// проверка за заключен док. + закл. на текущия обект.
			fLockOk = checkAndLockDoc(idLock, codeObj);
		}
		
		//За сега няма да викам различен метода за зареждане на док при разгледжане. 
		if (fLockOk) {
			if(flagFW == UPDATE_DOC) {
				fLockOk = loadDocumentEdit(docId); // зарежда данните за документа за актуализация или разглеждане
			}else if(flagFW == REG_WRK_DOC) {
				workDocId = docId; 
				fLockOk = loadDocFromOtherDoc(docId, REG_WRK_DOC); // зарежда данните на работен документ за регистриране като официален
			}
			//else if(flagFW == REG_OTHER_R ) {
				//loadDocFromOtherDoc(docId, REG_OTHER_R ); // зарежда данните на  документ зa приемане от друга регистратура
			//}
			if(fLockOk) {
				// спецификите за списъка с регистри - зависимост от типа документ
				specTypeDocProc(document.getDocType());
				
			}
			//  журналира отварянето на обекта ///// Ще го правим ли????
		}
		
   		if(isView == 1 && fLockOk) {
   			viewMode();
   		}
	}
	
	/**
	 * Регистриране на док. от е-мейл
	 * Регистриране на жалба от е-мейл
	 * @return
	 */
	private boolean  initDocMail() {	
		boolean fLockOk = true;
		MyMessage mailMsg = (MyMessage)JSFUtils.getFlashScopeValue("mailMessage");
		propMail = (Properties)JSFUtils.getFlashScopeValue("prop");
		selectMailBox = (String)JSFUtils.getFlashScopeValue("selectMailBox");
		if(mailMsg != null) {
			messUID =  Long.valueOf(mailMsg.getMessUID());			
			// проверка за заключване + закл. на текущия обект.
			fLockOk = checkAndLockDoc(messUID.intValue(), OmbConstants.CODE_ZNACHENIE_JOURNAL_MAILBOX);
			if(fLockOk) {	
				actionNewDocument(false, false);		
				messFromRef = mailMsg.getFrom();
				messSubject = mailMsg.getSubject(); // трябва ми за отговора..
				
				String noHTMLString="";
				String body = mailMsg.getBody();
				if(body != null){
					noHTMLString= Jsoup.parse(body).text();
				}
				
				if(flagFW == REG_FROM_MAIL_JALBA) {
					document.getJalba().setSubmitDate(mailMsg.getReceivedDate());
					document.getJalba().setSubmitMethod(OmbConstants.CODE_ZNACHENIE_SUBMIT_METHOD_EMAIL);				
					document.getJalba().setJbpEmail(messFromRef);
					document.getJalba().setJalbaText(messSubject + " \n\n" + noHTMLString); // описание от жалбоподател
					document.setDeliveryMethod(OmbConstants.CODE_ZNACHENIE_ANSWER_METHOD_EMAIL); // ако идва от емейл - отговрът също да е по емейл
				}else {
					document.setDocType(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN);
					// спецификите за списъка с регистри - зависимост от типа документ
					specTypeDocProc(document.getDocType());
					document.setReceiveDate(mailMsg.getReceivedDate());
					document.setReceiveMethod(OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL);
					document.setReceivedBy(messFromRef);// + " \n" + mailMsg.getFromName());			
					document.setOtnosno(messSubject + " \n\n" + noHTMLString); 
				}	
				
	  		    // файлове от мейла... 
				filesList = new ArrayList<>();
				if(mailMsg.getAttachements() != null && !mailMsg.getAttachements().isEmpty()) {
					Iterator<String> it = mailMsg.getAttachements().keySet().iterator();
					Files newFile;
					while (it.hasNext()) {
						String keyVar = it.next();
						newFile =  loadFilesFromMsg(keyVar, "application/x-download", mailMsg.getAttachements().get(keyVar));
						filesList.add(newFile);	
					}
				}		
				
				document.setFromMail(messUID.toString()+""+selectMailBox.trim().toUpperCase()); //необходимо е, защото от работния плот се прави проверка дали вече този е-мейл не региструран
			}
			
		}
		return fLockOk;
	}
	 

	
	
	/**
	 * Зарежда в новия документ файловете от емайл, сеос, ссев и прави проверка дали са подписани
	 * @param fileName
	 * @param fileCType
	 * @param content
	 * @return
	 */
	private Files loadFilesFromMsg(String fileName, String fileCType, byte[] content) {
		
		Files newFile = new Files();
		newFile.setFilename(fileName);
		newFile.setContentType(fileCType);
		newFile.setContent(content);
		newFile.setOfficial(OmbConstants.CODE_ZNACHENIE_DA);
		boolean bb = false;	
		try {
			bb = verifySignFile(fileName, content);
		} catch (Exception e) {
			LOGGER.error("Грешка при извличане на подпис от файл! ", e.getMessage());
		}	
		if(bb) {
			newFile.setSigned(Constants.CODE_ZNACHENIE_DA);
		} else {
			newFile.setSigned(Constants.CODE_ZNACHENIE_NE);
		}
		return newFile;
	}
	
	
    
	/**
	 * проверка за ел. подпис за файл, който идва от емйл, сеос, ссев
	 * @param filename
	 * @param cont
	 * @throws IOException 
	 */
	private boolean verifySignFile(String filename, byte[] content )  {
		
		boolean bb = false;
		try {
			
			Set<X509Certificate> trust = getSystemData().getTrustedCerts();
			
			if(filename.endsWith(".docx")) {			
				List<VerifySignature> rez = new SignatureUtils().verifyWordExcelSignatures(content, trust);
				if(rez!=null && !rez.isEmpty()) {
					bb = true;
				} 
			} else if(filename.endsWith(".pdf")) {			
				List<VerifySignature> rez = new SignatureUtils().verifyPDFSignatures(content, trust);
				if(rez!=null && !rez.isEmpty()) {
					bb = true;
				}
			}
			
		} catch (UnexpectedResultException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  "Неочаквана Грешка при извличане данни за сертификатите на файл!", e.getMessage());
		}
		
		return bb;
	}
	
	
	
	/** 
	 * регистриране на документи от СЕОС или ССЕВ
	 * @param idEgov - id на съобщение, изпратено през СЕОС или ССЕВ
	 * @return
	 */
	private boolean initDocEgovMsg(Integer idEgov) {
		boolean fLockOk = checkAndLockDoc(idEgov, OmbConstants.CODE_ZNACHENIE_JOURNAL_EGOVMESSAGE);	
		// проверка за заключване + закл. на текущия обект - съобщението от СЕОС
		if(fLockOk) {		
			try {
				actionNewDocument(false, false);
				egovMessCoresp = null;
				JPA.getUtil().runWithClose(() -> {
					EgovMessagesDAO daoEgov = new EgovMessagesDAO(getUserData());
					egovMess = daoEgov.findById(idEgov); // xml ??? da se mahne!!
					// още една проверка за статуса на съобщениеото  - трябва да е "Чака регистрация" - само тогава се допуска да продължи отварянето на екрана за регистрация на документ
					if (Objects.equals(egovMess.getMsgStatus(), EgovStatusType.DS_WAIT_REGISTRATION.toString())) {
						//файлове от съобщението... 
						loadFilesEgovMess(idEgov, daoEgov);
						// load coresp  - ако ги има да ги сложи в полето доп инфо като текст..........
						egovMessCoresp = loadCorespEgovMess(idEgov, daoEgov);
					}else {
						String txt = "Статус: " +egovMess.getMsgStatus();
						if(egovMess.getMsgRn() != null && egovMess.getMsgRnDate() != null) {
							txt = "Регистрирано под номер: "+egovMess.getMsgRn() + "/" + DateUtils.printDate(egovMess.getMsgRnDate());
						}						
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,getMessageResourceString(LABELS, "docu.egovMsgErrSt"),txt);
						egovMess = null; // това може да се случи, ако деловодител 1 си е отворил списъкът със съобщения и дълго нищо не прав, а междувременно деловодител 2 е обработил съобщението и едва тогава делов. 1 решава да рег. същото съобшение...
					}
				});
				
				if(egovMess != null) {
					initDocEgovMsg1();
				} else {
					document = null;
				}
			} catch (BaseException e) {
				LOGGER.error("Грешка при зареждане на съобщение от СЕОС/ССЕВ! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}	
		}
		return fLockOk;
	}

	/**
	 * зарежда данните от egоv съобщението в обект документ 
	 * @throws DbErrorException
	 */
	private void initDocEgovMsg1() throws DbErrorException{
		
		document.setDocType(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN);
		// спецификите за списъка с регистри - зависимост от типа документ  и списък процедури
		specTypeDocProc(document.getDocType());
		
		document.setGuid(egovMess.getDocGuid().toUpperCase());
		if(flagFW == REG_FROM_EGOV_JALBA) {
			document.getJalba().setSubmitDate(egovMess.getMsgDate());
			document.getJalba().setSubmitMethod(OmbConstants.CODE_ZNACHENIE_SUBMIT_METHOD_SSEV); 
			document.setDeliveryMethod(OmbConstants.CODE_ZNACHENIE_ANSWER_METHOD_SSEV); // ако идва от ссев - отговрът също да е по ссев			
		}else {
			document.setReceiveDate(egovMess.getMsgDate());
			if(Objects.equals(S_SEOS, egovMess.getSource())){
				document.setReceiveMethod(OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS); 
			}else {
				document.setReceiveMethod(OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV); 
			}
		}

		//търси кореспондента по ЕИК
		docEgovMsgCoresp();
		
		if(!SearchUtils.isEmpty(egovMess.getDocSubject())) {
			if(flagFW == REG_FROM_EGOV_JALBA) {
				document.getJalba().setJalbaText(egovMess.getDocSubject().replaceAll("\r", ""));// описание от жалбоподател
			}else {
				document.setOtnosno(egovMess.getDocSubject().replaceAll("\r", ""));
			}
		}
				
		// зарежда вид док., ако го намери и доп. информация 
		String docInfo = docEgovMsgVD();  
		
		// данни за кореспондети в съобщението - ако има такива
		String docInfo2 = loadDataCorespFromEgov();
		
		document.setDocInfo(docInfo + docInfo2);
		// техен номер/дата
		docEgovMsgTN();					
		document.setCountFiles(filesList.size());
	}
	
	private String loadDataCorespFromEgov() {
		StringBuilder docInfo2 = new StringBuilder("\r\n");
		if(egovMessCoresp != null && egovMessCoresp.size()>0) {
			// да заредя данните в полето dopinfo като текст
			int i = 1;
	    	for (EgovMessagesCoresp cor : egovMessCoresp){
	    		docInfo2.append("Кореспондент "+ i +  ": " + cor.getIme() + "\r\n");
	    		if (cor.getEgn() != null){
	    			docInfo2.append("ЕГН: " + cor.getEgn() + "\r\n");
	    		}
	    		if (cor.getBulstat() != null){
	    			docInfo2.append("БУЛСТАТ: " + cor.getBulstat() + "\r\n");
	    		}
	    		if (cor.getIdCard() != null){
	    			docInfo2.append( "Лична карта: " + cor.getIdCard() + "\r\n");
	    		}
	    		if (cor.getCity() != null){
	    			docInfo2.append( "Град/с.: " + cor.getCity() + "\r\n");
	    		}
	    		if (cor.getAdres() != null){
	    			docInfo2.append("Адрес: " + cor.getAdres() + "\r\n");
	    		}
	    		if (cor.getEmail() != null){
	    			docInfo2.append("Email: " + cor.getEmail() + "\r\n");
	    		}
	    		if (cor.getPhone() != null){
	    			docInfo2.append("Tелефон: " + cor.getPhone() + "\r\n");
	    		}	    		
	    		if (cor.getMobilePhone() != null){
	    			docInfo2.append( "Мобилен тел.: " + cor.getMobilePhone() + "\r\n");
	    		}
	    		docInfo2.append("\r\n");
	    		i++;
	    	}		

		}
		return docInfo2.toString();
	}
	
	
	
	/**
	 * Зарежда вид документ, ако го намери
	 * зарежда доп.информация
	 * @return
	 * @throws DbErrorException
	 */
	private String docEgovMsgVD() throws DbErrorException {
		StringBuilder docInfo = new StringBuilder();
		if (!SearchUtils.isEmpty(egovMess.getDocVid())){	
			List<SystemClassif> res = sd.getItemsByTekst(OmbConstants.CODE_CLASSIF_DOC_VID, egovMess.getDocVid().trim(),getCurrentLang(), new Date());
			if(res != null && !res.isEmpty()){
				SystemClassif sc = res.get(0);
				document.setDocVid(sc.getCode()); // ако успея да го намеря в класификацията....
			} else{
				docInfo.append("Вид документ: " + egovMess.getDocVid()+"\n");
			}
		}
		if(Objects.equals(S_SEOS, egovMess.getSource()) && 
				  !SearchUtils.isEmpty(egovMess.getDocComment()) && !"N/A".equals(egovMess.getDocComment())) {
			docInfo.append(egovMess.getDocComment()); 
		}else if(!Objects.equals(S_SEOS, egovMess.getSource()) &&
				 !SearchUtils.isEmpty(egovMess.getMsgXml()) ) {
			// това е съдържанието от ССЕВ - като string!!
			docInfo.append(egovMess.getMsgXml()); 
		}
		return docInfo.toString();
	}
	
	/**
	 * Търси кореспондента по ЕИК
	 * @return
	 * @throws DbErrorException
	 */	
	private void docEgovMsgCoresp() throws DbErrorException {
		boolean cc = true;
		String eikStr = null;
		int tlice = OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL;
		if(!SearchUtils.isEmpty(egovMess.getSenderEik())) {
			eikStr = egovMess.getSenderEik();
			tlice = OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL;
			if (flagFW != REG_FROM_EGOV_JALBA){
				List<SystemClassif> sender = sd.getItemsByCodeExt(OmbConstants.CODE_CLASSIF_REFERENTS, egovMess.getSenderEik(), getCurrentLang(), new Date());
				if(sender != null && !sender.isEmpty()){
					SystemClassif sc = sender.get(0);
					document.setCodeRefCorresp(sc.getCode()); // ако успея да го намеря в класификацията....
					cc = false;
				}	
			} 
		}
		if (cc && !SearchUtils.isEmpty(egovMess.getSenderName())){			
			if(flagFW == REG_FROM_EGOV_JALBA) {
				document.getJalba().setJbpName(egovMess.getSenderName()); 
				document.getJalba().setJbpEik(eikStr);
				document.getJalba().setJbpType(tlice);
			}else {
				document.setReceivedBy("Идва от: " + egovMess.getSenderName()+" ЕИК:" +eikStr+"\n");
			}
		}
		
	}
	
	/**
	 * EgovMsg - техен номер/дата
	 */
	private void docEgovMsgTN() {
		if (!SearchUtils.isEmpty(egovMess.getDocRn())){							
			document.setTehNomer(egovMess.getDocRn());
			document.setTehDate(egovMess.getDocDate());
			//търсене по техен номер като включва и дата!! и кореспондент, ако е намерен! 
			searchTehNomerSql(egovMess.getDocRn(), egovMess.getDocDate(), document.getCodeRefCorresp());
			
		}else if (!SearchUtils.isEmpty(egovMess.getDocUriReg())){
			document.setTehNomer(egovMess.getDocUriReg() + "egovMess"+egovMess.getDocUriBtch());
			document.setTehDate(egovMess.getDocDate());
		}
	}
	
	/**
	 * Файлове към съобщения от СЕОС/ССЕВ
	 * @param idEgov
	 * @param daoEgov
	 */
	private void loadFilesEgovMess(Integer idEgov, EgovMessagesDAO daoEgov) {
		//  само ако има файлове към документа
		try {			
			List<EgovMessagesFiles> lstEgovFiles = 	daoEgov.findFilesByMessage(idEgov);
			if(lstEgovFiles != null && !lstEgovFiles.isEmpty() ) {
				Files newFile = null;  // GUID и останалите полета ????
				for(EgovMessagesFiles egovF: lstEgovFiles) { 
					newFile =  loadFilesFromMsg(egovF.getFilename(), egovF.getMime(), egovF.getBlobcontent());
					filesList.add(newFile);	
				}
				
			} else {
				String msg="За съобщение с id= "+idEgov+", липсват прикачени файлове!"; 
				LOGGER.error(msg);
			}
			
		} catch (BaseException e) {
			LOGGER.error("Грешка при извличане на файловете към съобщение от СЕОС/ССЕВ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
	}
	
	/**
	 * зарежда, ако има коресп. в съобщението
	 * @param idEgov
	 * @param daoEgov
	 * @return
	 */
	private List<EgovMessagesCoresp> loadCorespEgovMess(Integer idEgov, EgovMessagesDAO daoEgov){
		List<EgovMessagesCoresp> list = new ArrayList<EgovMessagesCoresp>();
		try {			
			egovMessCoresp = new ArrayList<EgovMessagesCoresp>();
			list = 	daoEgov.findCorespsByIdMessage(idEgov);
			if(list != null && !list.isEmpty() ) {
				for(EgovMessagesCoresp item: list) { 
					egovMessCoresp.add(item);	
				}
				
			} else {
				String msg="За съобщение с id= "+idEgov+", липсват прикачени файлове!"; 
				LOGGER.error(msg);
			}
			
		} catch (BaseException e) {
			LOGGER.error("Грешка при извличане на файловете към съобщение от СЕОС/ССЕВ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		return list;
	}
	
	/**
	 * проверка за заключен документ / движение / съобщение от е-мейл
	 * заключване на съответния обект  
	 * @param idLock
	 * @param codeObj
	 * @return true - OK 
	 */
	private boolean checkAndLockDoc(Integer idLock, Integer codeObj) {	
		// проверка за заключен документ / движение / съобщение от е-мейл
		boolean fLockOk = checkForLockDoc(idLock, codeObj);
		if (fLockOk) {	
			// заключване на док., за да не може да се актуализира от друг и отключване на всички други обекти за потребителя(userId) и 
			// при рег. на официални от работни -  заключвам раб.,  Така ще избегнем дублиране, ако се работи едновременно
			// при приемане на док. от друга рег. - заключвам движението, с което е предаден документа
			// при рег. от е-мейл - заключва се messId, с код на обект CODE_ZNACHENIE_JOURNAL_MAILBOX
			// при рег. от СЕОС - заключва се id на съобщението, обект CODE_ZNACHENIE_JOURNAL_EGOVMESSAGE
			lockDoc(idLock, codeObj);					
		}
		return fLockOk;
	}
	
	
	
	/**
	 *  общосистемни настройки
	 */
	private void allSystemSettings() {
		try {
			nastrWithEkz = false;
			String param1 = sd.getSettingsValue("delo.docWithExemplars"); // да се работи с екземпляри
			if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
				nastrWithEkz = true;
			}
			scanModuleExist = false;
			param1 = sd.getSettingsValue("system.scanModuleExist"); // 	Системата има модул за сканиране на документи. 0-няма, 1-има
			if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
				scanModuleExist = true;
			}
			nastrZadnaData = false;
			param1 = sd.getSettingsValue("delo.regOldDateDoc"); // 	Позволява се въвеждане на документи със задна дата 1-да/ 0-не
			if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
				nastrZadnaData = true;
			}
			
		 } catch (DbErrorException e) {
			 LOGGER.error("Грешка при извличане на системни настройки! ", e);
			 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
	}


	
	/**
	 * Проверка за заключен документ 
	 * @param idObj
	 * @return true - ОК (док. не е заключен)
	 */
	private boolean checkForLockDoc(Integer idObj, Integer codeObj) {
		boolean res = true;	
		try { 
			Object[] lockObj =  new LockObjectDAO().check(ud.getUserId(), codeObj, idObj);
			if (lockObj != null) {
				 res = false;
				 String msg = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, Integer.valueOf(lockObj[0].toString()), getUserData().getCurrentLang(), new Date())   
						       + " / " + DateUtils.printDateFull((Date)lockObj[1]);
				 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,getMessageResourceString(LABELS, "docu.docLocked"), msg);
			}
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при проверка за заключен документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		return res;
	}
	
	/**
	 * Заключване на документ, като преди това отключва всички обекти, заключени от потребителя
	 * @param idObj
	 */
	public void lockDoc(Integer idObj, Integer codeObj) {	
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("lockDoc! {}", ud.getPreviousPage() );
		}
		LockObjectDAO daoL = new LockObjectDAO();		
		try { 
			JPA.getUtil().runInTransaction(() -> 
				daoL.lock(ud.getUserId(), codeObj, idObj, null)
			);
		} catch (BaseException e) {
			LOGGER.error("Грешка при заключване на документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} 		
	}

	
	/**
	 * при излизане от страницата - отключва обекта и да го освобождава за актуализация от друг потребител
	 */
	@PreDestroy
	public void unlockDoc(){
        if (!ud.isReloadPage()) {
        	unlockAll(true);
        	ud.setPreviousPage(null);
        }          
        ud.setReloadPage(false);
	}
	
	
	/**
	 * отключва всички обекти на потребителя - при излизане от страницата или при натискане на бутон "Нов"
	 */
	private void unlockAll(boolean all) {
		LockObjectDAO daoL = new LockObjectDAO();		
		try { 
			if (all) {
				JPA.getUtil().runInTransaction(() -> 
					daoL.unlock(ud.getUserId())
			);
			} else {
				JPA.getUtil().runInTransaction(() -> 
					daoL.unlock(getUserData().getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK)
				);
			}
		} catch (BaseException e) {
			LOGGER.error("Грешка при отключване на документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
	}
	
	/**
	 *  Зарежда данните на документа за актуализация или разглеждане
	 *   
	 * @param docId
	 * @return
	 */
	private boolean loadDocumentEdit(Integer docId) {
		boolean flagOk = true;
		try {
			regUserFiltered = isView == 0;
			JPA.getUtil().runWithClose(() -> {
				document = this.dao.findById(docId);				
				if(document != null && this.dao.hasDocAccess(document, regUserFiltered, getSystemData())) {	//проверка за достъп до документа
					loadFilesList(document.getId(), UPDATE_DOC ); // 	load files
				} else {
					document = null;
				}
			});
			
		    if (document == null) {
		    	flagOk = false; // потребителят няма достъп до документа
		       	JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,getMessageResourceString(LABELS, "docu.docAccessDenied"));
		    } else {
		    
		    	rnFullDocEdit = DocDAO.formRnDoc(this.document.getRnDoc(), this.document.getPoredDelo());
		    	
		    	actionChangeDocVid(false); // извлича настройките по вид документ
		    	setLimitedAccessCh(!Objects.equals(document.getFreeAccess(), OmbConstants.CODE_ZNACHENIE_DA));
		    	docProcessed(Objects.equals(document.getProcessed(), OmbConstants.CODE_ZNACHENIE_DA));
				docCompetence(); // компетентност
				
				docWorkOffId();// връзка офицален - работен; официален - друга регистратура
				
				// протокол за унищожение
				if(document.getProtocolData() != null) {
					rnFullProtocol = "  ПУ "+ document.getProtocolData()[1] + "/" + DateUtils.printDate((Date)document.getProtocolData()[2]) ;
					// ако има пореден номер - той е слепен предварително
				}
				
				deloDocPrep = new DeloDoc();
				//за значения от класификации, които трябва да се разкодират към датата на документа
				this.setDecodeDate(document.getDocDate());
							
				btnObraboten(false, false); // да се вижда ли бутона "Обработен"
				
				//зарежда референтите към документ
				loadReferentsData(UPDATE_DOC);
					
				// ако има техен номер - да се провери има ли други док. със същия тех.номер
				if(!SearchUtils.isEmpty(document.getTehNomer())) {
					searchTehNomerSql(document.getTehNomer(), null, null);
				}
				
				// тематики
				loadScTopicList();
				
				// зарежда допълнителните регистратури за раб. док. - зависи от правата на потребителя 
				loadDopRegistraturiList();
				if(document.getId() != null) {
					String param1 = sd.getSettingsValue("delo.journalOpenDeloDoc"); // да се журналира ли отварянето
					if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
						// запис в журнала, че док. е отоврен
						JPA.getUtil().runInTransaction(() -> this.dao.saveAudit(document, SysConstants.CODE_DEIN_OPEN));	
					}
				}
				
		    }
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на документа! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		return flagOk;
	}
	

	/**
	 * връзка офицален - работен; официален - друга регистратура
	 * @throws DbErrorException 
	 */
	private void docWorkOffId() throws DbErrorException {
		readyForOfficial = false;
		boolean tvdWrk = Objects.equals(document.getDocType(), OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK);
		if(document.getWorkOffId() != null) {	
			Integer reg = Integer.valueOf((document.getWorkOffData()[3]).toString());
			String tvd  = ""; 
			String fromRegistratura = "";
			if(tvdWrk) {
				// в работен док. сме
				tvd  = getMessageResourceString(LABELS, "docu.officDoc");
			} else if(Objects.equals(reg, document.getRegistraturaId())) {
				// в официален, регистриран от работен
				tvd = getMessageResourceString(LABELS, "docu.wrkDoc"); 
				setFromOtherReg(false);
			} else { // от друга регистратура 
				fromRegistratura = " "+ getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REGISTRATURI, reg, getUserData().getCurrentLang(), new Date());
				setFromOtherReg(Objects.equals(document.getDocType(), OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN)); // ако е входящ - линка се вижда до техен номер, ако е собствен - тогава занчие, че е рег. на официален от работен! 
			}
			
			rnFullDocOther = tvd +" " +document.getWorkOffData()[1] + "/" + DateUtils.printDate((Date)document.getWorkOffData()[2]) + fromRegistratura+"  ";
			// ако има пореден номер - той е слепен предварително 
		} else if(tvdWrk &&  document.getForRegId() != null ) {
			readyForOfficial = true;
		}
	}
	
	/**
	 * зарежда допълнителните регистратури - само за раб. док, който не е рег. като официален
	 * филтрира се в зависимост от правата на потребителя
	 * @throws DbErrorException
	 * @throws UnexpectedResultException
	 */
	private void loadDopRegistraturiList() throws DbErrorException, UnexpectedResultException {
		if(this.dopRegistraturiList == null && 
			Objects.equals(document.getDocType(), OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) &&
			document.getWorkOffId() == null) {
			// контролата за избор на доп.регистратура да се види амо, ако резултатът този списък е > 1
			// Допълнителни регистратури за заявка за извеждане на документи - включва и основната!!		
			this.dopRegistraturiList = createItemsList(true,  OmbConstants.CODE_CLASSIF_REGISTRATURI_REQDOC, new Date(), true); 
			if(document.getPreForRegId()==null) {
				document.setPreForRegId(getUserData(UserData.class).getRegistratura()); // текуща регистртурата
			}
		}
	}
		
	
	/**
	 * разглеждане на работен/офицален док.; протокол за унищожение
	 * @param i
	 * @return
	 */
	public String actionGotoViewDoc(int i) {
		Integer idObj = null;
		if(i==1) {
			idObj = this.document.getWorkOffId(); // работен - офицален 
		} else{
			idObj = Integer.valueOf(this.document.getProtocolData()[0].toString()); //протокол за унищожение
		}
		return "docView.xhtml?faces-redirect=true&idObj=" + idObj;
	}
	
	public String actionGotoViewJalba() {
		return "docView.xhtml?faces-redirect=true&idObj=" + specDocId;
	}
	
	/**
	 * Дали да се вижда бутона "обработен"
	 * @param newDoc
	 * @param fSave true - извиква се от actionSave
	 */
	private void btnObraboten(boolean newDoc, boolean fSave) {
		
		if(fSave && !newDoc  && createPrep ) {
			// за да се обнови таб "Вложен в преписки"
			DocDataPrep beanTabPrep = (DocDataPrep) JSFUtils.getManagedBean("docDataPrep");
			if(beanTabPrep != null){ 
				beanTabPrep.setRnFullDoc(null);
			}
		}
		
		if (!Objects.equals(document.getDocType(), OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN) && ud.isDelovoditel()){
	    	// за работен и входящ, ако съм в роля деловодител 
	    	viewBtnProcessed ++;
	    	if(newDoc) {
	    		docProcessed(Objects.equals(document.getProcessed(), OmbConstants.CODE_ZNACHENIE_DA)); // ако има процедура и след запис е създадена задача
	    	}
	    }	else {
	    	viewBtnProcessed = 0;
	    }
	}
	
	/**
	 * зарежда референтите към документ - без входящи
	 * @param flag - 0(актуализаиця); 1(рег. на офиц. от раб.); 2(от друга регистратура)
	 */
	private void loadReferentsData(int flag) {
		if(!Objects.equals(document.getDocType(), OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN)) {
			// само за собствени документи
			loadNameReferentsList(document.getReferentsAuthor(), flag,  OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR);
			loadNameReferentsList(document.getReferentsAgreed(), flag,  OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AGREED);
			loadNameReferentsList(document.getReferentsSigned(), flag,  OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_SIGNED);
		}
	}
	
	
	/**	
	 * Извлича списъка с файлове към документ
	 * @param wrkDocId - id  на документ
	 * @param @param flag - 0(актуализаиця); 1(рег. на офиц. от раб.); 2(от друга регистратура)
	 */
	private void loadFilesList(Integer wrkDocId, Integer flag ) {
	
		try {				
			FilesDAO daoF = new FilesDAO(getUserData());		
			filesList = daoF.selectByFileObjectDop(wrkDocId, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC); // използва се този метод за зареждане на файлове, за да се заредят доп. полета - лични дании, официален док и т.н.
		
			if (flag != UPDATE_DOC && filesList != null && !filesList.isEmpty() ) {
				onlyMarkFiles();
			}
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при извличане на файловете към документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}	
				
	}
	
	/**
	 * да се извлекат само файловете, маркирани като официални
	 */
	private void onlyMarkFiles() {
		List<Files> fList = new ArrayList<>();
		for (Files f : filesList) {
			// да се вземат само маркираните като официални от работния документ или при рег. от други регистратура
			if(Objects.equals(f.getOfficial(),OmbConstants.CODE_ZNACHENIE_DA)) {
				fList.add(f);
			}
		}
		filesList.clear();
		if(!fList.isEmpty()) {
			filesList.addAll(fList);
		}
	}
	
	/**
	 * зарежда тематиките на документа
	 */
	private void loadScTopicList(){
		List<SystemClassif> tmpLst = new ArrayList<>();		
		if(document.getTopicList() != null) {
			for( Integer item : document.getTopicList()) {
				String tekst = "";
				SystemClassif scItem = new SystemClassif();
				try {
					scItem.setCodeClassif(OmbConstants.CODE_CLASSIF_DOC_TOPIC);
					scItem.setCode(item);
					tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DOC_TOPIC, item, getUserData().getCurrentLang(), new Date());		
					scItem.setTekst(tekst);
					tmpLst.add(scItem);
				} catch (DbErrorException e) {
					LOGGER.error("Грешка при зареждане на тематики на документ! ", e);
				}		
			}				
		}
		setScTopicList(tmpLst); // тематики
	}

	
	/**
	 * 
	 * @param wrkDocId:
	 * id на предаден от друга рег. документ, който ще бъде регистриран като официален
	 * id на работен документ, който ще бъде регистриран като официален
	 * @param fw: 1(рег. на офиц. от раб.); 2(от друга регистратура)
	 */	
	public boolean loadDocFromOtherDoc(Integer wrkDocId, int fw) {
		boolean flagOk = true; 
		try {
			regUserFiltered = true;
			// зачиства данните за преписката, ако има такава 
			clearDeloDocLink();
			JPA.getUtil().runWithClose(() -> {
				document = this.dao.findById(wrkDocId);
				if(document != null && this.dao.hasDocAccess(document, false, getSystemData())) {	//проверка за достъп до документа (както при разглеждане)

					// load files 
					loadFilesList(wrkDocId,fw);
					/*if(this.sourceRegDvijId != null) {
						dvijData = dao.findDocDvijData(this.sourceRegDvijId);
					}*/
			
					if(fw == REG_WRK_DOC) {
						Integer typePrep = null;
						Object[] prep = this.dao.findFirstInclDelo(wrkDocId);
						if(prep != null && prep[0] != null && prep[1] != null && prep[2] != null && prep[3] != null) {
							// раб. док. е вложен в преписка и трябва по подразбиране офиц. да вземе нейния номер и да влезе в преписката!!
							
							deloDocPrep.setDeloId(Integer.valueOf(prep[0].toString())); // id на преписката - необходимо е за записа
							deloDocPrep.getDelo().setRnDelo(prep[1].toString()); // номера на преписката трябва да се прехвърлi в номера на документа!!
							typePrep = Integer.valueOf(prep[3].toString());
							String msg =  getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DELO_TYPE, typePrep, getUserData().getCurrentLang(), new Date()) +
									" "+deloDocPrep.getDelo().getRnDelo() +" / "+ DateUtils.printDate((Date)prep[2]);
							
							JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docu.addDeloMsg1r", msg) );
						}
						if(Objects.equals(typePrep, OmbConstants.CODE_ZNACHENIE_DELO_TYPE_JALBA) || 
						   Objects.equals(typePrep, OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NPM) ||
						   Objects.equals(typePrep, OmbConstants.CODE_ZNACHENIE_DELO_TYPE_SAMOS)) {
							viewChboxCrPrep = false;	
						}else {
							viewChboxCrPrep = true;	
						}
					} 
				} else {
					document = null;
				}			
			});
		    if (document == null) {
		    	flagOk = false; // потребителят няма достъп до документа
		       	JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,getMessageResourceString(LABELS, "docu.docAccessDenied"));
		    } else {
		    	// формирам инф. за показване на екрана: рег.номер/дата на предадения документ + регистратура, от която идва 
				String fromRegistratura ="";
				/*if(fw == REG_OTHER_R ) {
					fromRegistratura =  " "+getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REGISTRATURI, document.getRegistraturaId(), getUserData().getCurrentLang(), new Date());
					setFromOtherReg(true);				
				}*/
				rnFullDocOther = DocDAO.formRnDocDate(this.document.getRnDoc(), this.document.getDocDate(), this.document.getPoredDelo())+fromRegistratura;
				
				// прехвърлям id на док. източник в поле за връзка workOffId 	
				document.setWorkOffId(wrkDocId);
				// да нулирам ид-то
				document.setId(null);
				// общи настр. за екрана - нов документ - да сменя типа на документа, дата на рег. и др.
				if(fw == REG_WRK_DOC) {
					//зарежда референтите (автор/подписал/съгалсувал) към документ и нулира id на връзката
					loadReferentsData(REG_OTHER_R );
					// типа е "собствен" и смяната на тип документ е забранена
					newDocSettings(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN, false);
					document.setDocInfo(null);			
				} 
				/*else if (fw == REG_OTHER_R ) {
					loadFromOtherDocR(fromRegistratura);	
				}*/
					
				//зачиства специфичини полета в документа- когато от док. източник, трябва да направя нов официален
				clearDocSpecData(deloDocPrep.getDelo().getRnDelo());
				
				// 	настройки на регистртура - нов документ 
				if(fw == REG_WRK_DOC) {
					newDocRegSettings(false);// Само, ако е регистриране на официален от работен - да взема достъпа на работния!!!
				}
				/*else {
					newDocRegSettings(true);
				}*/
				
				// нулирам регистъра - освен, ако не е ясно по подразбиране кой е регистъра... 
				document.setRegisterId(null);
				
				// да взема настройките по тип и вид документ, ако има такива	
				nastrDocType(document.getDocType());
			    actionChangeDocVid(true);
			   
			    document.setValid(OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL);
			    if(deloDocPrep.getDelo().getRnDelo() != null) {
			    	// ще влезе тук, ако раб. док. е вложен в преписка и трябва по подразбиране офиц. да вземе нейния номер и да влезе в преписката!!   
			    	// слагам го в края на метода, за да не променям горните методи - в тях avtoNом се зарежда в зависимост от настройките, а това тук е изключение!!!
			    	avtomNo = false; // да се махен автом. регистрация;
			    }
		    	
		    }
			
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане данните на раб. док. при регистриране на официален! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на раб. док. при регистриране на официален!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		return flagOk;
	}
	
	/**
	 * зарежда данните за док. - идва от друга регистртура
	 * @param fromRegistratura
	 */
	/*
	private void loadFromOtherDocR(String fromRegistratura) {
		String tmpDopInfo = "";	
		if(dvijData != null) {
			if(dvijData[1]!=null) {
				fromRegistratura += "; Изпратен на "+ dvijData[1].toString();
			}
			if(dvijData[2]!=null) {
				tmpDopInfo = dvijData[2].toString()+"\r\n";						
			}
			if(dvijData[3]!=null) {
				tmpDopInfo += "Да се върне до:" +DateUtils.printDate((Date)dvijData[3]);
			}
		}
		if(SearchUtils.isEmpty(tmpDopInfo)) {
			tmpDopInfo = null;
		}
		document.setDocInfo(tmpDopInfo);
		document.setTehNomer(this.document.getRnDoc());
		document.setTehDate(this.document.getDocDate());
		// типа е "входящ" и  смяната на тип документ е забранена
		newDocSettings(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN, false);
		document.setReceiveMethod(OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA);
		document.setReceiveDate(new Date());
		document.setReceivedBy(fromRegistratura);
		// да се провери има ли други док. със същия тех.номер
		searchTehNomerSql(document.getTehNomer(), null, null);
		// новият документ е "входящ" - да се зачистят данните за автор/подписал/съгласувал, които идват от изпратения от другата рег. документ (ако има такива)
		document.setReferentsAuthor(null);
		document.setReferentsAgreed(null);
		document.setReferentsSigned(null);
	} 
	*/
	
	/**
	 * Зарежда(разкодира) имената на референтите в списъците - автор, подписал, съгласувал
	 * @param listRef
	 * @param flag - 0(актуализаиця); 1(рег. на офиц. от раб.) 
	 */
	private void loadNameReferentsList(List<DocReferent> listRef, int flag, int role) {
		if (listRef == null || listRef.isEmpty()) {
			return;
		}
		for( DocReferent drItem: listRef ) {
			String tekst = "";
			try {				
				tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, drItem.getCodeRef(), getUserData().getCurrentLang(), document.getDocDate());
				drItem.setTekst(tekst);
				if(flag != UPDATE_DOC) {
					//ако е рег. на официален от работен и др.... 
					drItem.setId(null);
					drItem.setDocId(null);
					drItem.setUserLastMod(null);
					drItem.setDateLastMod(null);
				}else if ( role == OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR &&
						Objects.equals(document.getDocType(), OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) &&
						ud.getUserAccess().equals(drItem.getCodeRef())) {
					viewBtnProcessed ++; // за работен документ, ако съм автор - да видя бутон "обработен" - ще се види само при актуализация!
				}
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане на референти (автор, подписал, съгласувал)! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}		
		}
	}
	
	
	
	/** Нов документ
	 *  flagS = true - ако документа се отваря през точка от менюто "Нов документ със запазване на данни"
	 *  flag2 = true - натиснат е бутон Нов вътре в екрана
	 * @return
	 */
	public void actionNewDocument(boolean flagS, boolean flag2 ) {
				
		Integer typeD = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN;
		if(docTypeList != null && docTypeList.size()<3) {
			typeD = (Integer) docTypeList.get(0).getValue();
		}
	
		Doc docC = null;
		if(flagS) { 
			//запомняне на данни от предишния док. //типа се помни винаги 
			docC = document;
		} 
		if(flag2) {
			flagFW = UPDATE_DOC; 
			typeD = document.getDocType();
		}
		
		this.document = new Doc();	
		rnFullDocEdit = null;
		rnFullDoc = null;
		rnFullDocOther = null;
		rnFullProtocol = null;
		sourceRegDvijId = null; 
		viewBtnProcessed = 0;	
		setScTopicList(new ArrayList<>()); // тематики
		// общи настр. за екрана - нов документ		
		viewChboxCrPrep = true; 
		newDocSettings(typeD, flagS);
		if(flagS) { 
			createPrep = createPrepOld;
		} 
		// настройки в зависимост от типа на документа
		nastrDocType(typeD);	
	    // да позволи избор на процедура 
		enableProc();
		// зачиства данните за преписката
		clearDeloDocLink();
		
		// 	настройки на регистртура - нов документ
		newDocRegSettings(true);
	
		// зачиства данни за връзка по техен номер
		selectedDocsTn = null;
		//само за работния
		readyForOfficial = false;
		
		filesList = new ArrayList<>();
		
		// "за резолюция"
		rezolTask = new Task();	
		rezolExecClassif = new ArrayList<>();
		tnRez = 0; // тех.ном.
		
		// прехвъляне на запомнените данни от предишния док.
		// вид, относно, спешност, регистър, бр. листа - за всички документи 
	
		// за вх. документи -  връзка с док., идва от, кореспондент, насочването на документа
		//  - за собствените - връзка с док., идва от, кореспондент, подписал, съгласувал, автор	

		if(docC != null) {
			document.setDocVid(docC.getDocVid());  		
			document.setRegisterId(docC.getRegisterId());
			document.setOtnosno(docC.getOtnosno());
			document.setUrgent(docC.getUrgent());
			document.setCountSheets(docC.getCountSheets());
		}
		
		
		if (flagFW != REG_FROM_MAIL && flagFW != REG_FROM_MAIL_JALBA  && flagFW != REG_FROM_EGOV) {
			unlockAll(true); // да се отключи предишния документ, но да не маха от UserData previousPage
		}
		if(flagS) {
			ud.setReloadPage(false); //при натискане на бутон "нов документ" - вътре в екрана
		}
		
		document.setValid(OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL);
		newSpecDoc();
	}
	
	/**
	 * зарежда специфични данни за нова жалба, нпм, самосезиране или нов работен от тях
	 */
	private void newSpecDoc() {		
		// Да зареди тип и вид на документ, ако е  жалба ; нпм ;  самосезиране - Регистъра се взема от настройки по вид документ
		if(specDocId != null) {
			// Нов работен документ от жалба/ нпм/ самосезиране
			
			document.setDocType(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK);
			nastrDocType(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK);
			document.setFreeAccess(OmbConstants.CODE_ZNACHENIE_DA); 			// Достъпът да е свободен!
		 	setLimitedAccessCh(false);
			document.setValidDate(new Date());
						
			// да се сложи в специален регистър с прозив. номер
			// за този рег. да се позволи да има дублиране на номерата, без да са в офиц. раздел на праписката
			// регистърът е еднакъв за всички
		 	document.setRegisterId(OmbConstants.ID_REGISTER_FREE_RAB_NO_VALIDATE); 
			actionChangeRegister();
			document.setRnDoc(specRnDoc); //работен от жалба, нпм, самосезиране - номера на работния да бъде - "Р"+номера на спец. док. 
			 
		}else if (docVidInclude != INCL_REGULAR) {
			//да зареди стойности по подразбиране за тип и вид документ			
			document.setValidDate(new Date());
			document.setFreeAccess(OmbConstants.CODE_ZNACHENIE_DA); 			// Достъпът да е свободен!
		 	setLimitedAccessCh(false);
			if(docVidInclude == INCL_COMPLAINT) { 
				document.setDocType(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN);
				document.setDocVid(OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA);
				if(flagFW == REG_FROM_MAIL_JALBA || flagFW == REG_FROM_EGOV_JALBA) {
					document.setJalba(new DocJalba());
				}
			} else if(docVidInclude == INCL_NPM) { 
				document.setDocType(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN);
				document.setDocVid(OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM);
			} else if(docVidInclude == INCL_SAMOSEZ) { 
				document.setDocType(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN);
				document.setDocVid(OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS);
			}
			actionChangeDocVid(true);
		}
	}
	
	/**
	 * общи настр. за екрана - нов документ
	 * @param typeDoc - тип на документ по подразбиране
	 * @param newDocCopy - true - със запазване на данни от предишен док.
	 */
	private void newDocSettings(Integer typeDoc, boolean newDocCopy) {
		this.setDecodeDate(new Date());	
		this.document.setRegistraturaId(getUserData(UserData.class).getRegistratura());		
		this.document.setDocDate(new Date());
		this.document.setDocType(typeDoc);
		regUserFiltered = true;
		rnFullDoc = null;
		processedCh = false;
		competence = false;
		createPrep = false;
		if(!newDocCopy) {
			this.document.setCountOriginals(1); // поподразбиране - 1 екземпляр
			avtomNoDisabled = false;
			templatesList = null;
			docSettingId = null;
			docSettingHar = null;
			membersTab = null;
		}
	
	}
		
   /**
    * настройки в зависимост от типа на документа 
    * @param typeDoc - тип на док.
    */
   private void nastrDocType(Integer typeDoc) {
	// да се филтрира списъка с регистри в зависимост от типа на документа 
	    document.setRegisterId(null);
	    avtomNoDisabled =  false;
	    avtomNo = true;
	    document.setDocType(typeDoc);
	    
	 // спецификите за списъка с регистри - зависимост от типа документ и списък процедури
	  
	    classifProceduri = null;
	   	document.setProcDef(null);
       	document.setProcExeStat(null); 
		specTypeDocProc(typeDoc);
		
		if(document.getId() == null && showSignMethod != null) {
			document.setSignMethod(showSignMethod);
		}else {
			document.setSignMethod(null);
		}
			
	    // за нов работен - да се зареди по подразбиране автор - userSave
	    if(document.getId() == null && Objects.equals(typeDoc, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK)) {
			try {
				DocReferent drItem = new DocReferent();
				drItem.setCodeRef(ud.getUserSave());	
				drItem.setRoleRef(OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR);
				String tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, drItem.getCodeRef(), getUserData().getCurrentLang(), document.getDocDate());
				drItem.setTekst(tekst);
				List<DocReferent> tmp = new ArrayList<>();
				tmp.add(drItem);
				document.setReferentsAuthor(tmp);
				loadDopRegistraturiList();
			} catch (DbErrorException | UnexpectedResultException e) {
				LOGGER.error("Грешка при зареждане в работен документ на автор по подразбиране или списък с допълнителни регистртури!", e);	
			}
	    }else {
	    	dopRegistraturiList = null;
	    }
   }
   
   /**
    * 1. Спец. по тип документ
    * 2. специфики и списък с процедури
    * @param typeDoc
    */
    private void specTypeDocProc(Integer typeDoc){
    	if(specificsRegister == null) {
			specificsRegister = new HashMap<>();
			specificsRegister.put(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA, Optional.of(getUserData(UserData.class).getRegistratura())); //?? трябва ли да го има
			specificsRegister.put(OmbClassifAdapter.REGISTRI_INDEX_VALID, SysConstants.CODE_ZNACHENIE_DA);
		}
    	if (Objects.equals(typeDoc, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK)) { // изрично само за работни
    		specificsRegister.put(OmbClassifAdapter.REGISTRI_INDEX_DOC_TYPE, typeDoc);
    		
    	} else { // за конкретния тип док + тези регистри , за които не е зададен тип документ
    		specificsRegister.put(OmbClassifAdapter.REGISTRI_INDEX_DOC_TYPE, Optional.of(typeDoc));
    	}
      	if(document.getId() == null && document.getRegisterId() == null) {
			// само за нов документ и няма избран регистър;
			// ако в списъка има само един регистър - да се зареди по подразбиране
			try {
				// стандартно зареждане с  отчитане права на потребителя
				List<SystemClassif> tmpRList = getSystemData().queryClassification(getUserData(), OmbConstants.CODE_CLASSIF_REGISTRI, null, new Date(), getCurrentLang(), specificsRegister);
				if(tmpRList != null && tmpRList.size() == 1){
					document.setRegisterId(tmpRList.get(0).getCode());
				}
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при извличане на регистри по специфики за тип документ!", e);	
			}
		}
    	enableProc();
    	specProcedure();
    }
    
    /**
     * специфики и списък с процедури
     */
    private void specProcedure() {
//    	try {
//    		if(classifProceduri == null && enableProc) { 		
//				specificsProc = new HashMap<>();
//				specificsProc.put(OmbClassifAdapter.PROCEDURI_INDEX_REGISTRATURA, getUserData(UserData.class).getRegistratura()); 
//				specificsProc.put(OmbClassifAdapter.PROCEDURI_INDEX_STATUS, OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE);
//				specificsProc.put(OmbClassifAdapter.PROCEDURI_INDEX_DOC_TYPE, this.document.getDocType());
//				classifProceduri = getSystemData().queryClassification(OmbConstants.CODE_CLASSIF_PROCEDURI, null, new Date(), getCurrentLang(), specificsProc);
//			}
//		} catch (DbErrorException e) {
//			LOGGER.error("Грешка при зареждане на списък с процедури!", e);	
//		}
    	
    }
     
    /**
     * Определя дали да се разреши избор на процедура
     */
    private void enableProc() {
//    	//дали разрешава стартиране на процедура в зависимост от стауса й, ако вече док. е стартиран по процедура,
//    	// notFinishedProc=true - раб. по нея не е приключила и не се позвлоява стартиране на друга проц. по документа - бутона "Избор на нова процедура" е скрит
//    	notFinishedProc = Objects.equals(OmbConstants.CODE_ZNACHENIE_PROC_STAT_WAIT,document.getProcExeStat()) ||
//    					  Objects.equals(OmbConstants.CODE_ZNACHENIE_PROC_STAT_EXE, document.getProcExeStat());
//    	
//    	enableProc = document.getProcExeStat() == null; 
//    	// за нов документ - винаги трябва да се вижда контролата за избор на процедура    	
    }
    
    /**
     * Натиснат е бутона "Нова процедура", който се вижда само, ако enableProc=false;
     */
    public void actionNewProcedure() {
//    	enableProc = true;
//    	notFinishedProc = false; 
//      document.setProcDef(null);
//      document.setProcExeStat(null); // за да мине записa!
//    	specProcedure();
    }
    
    /**
     * Извежда съобщение, ако е избрана процедура
     */	
    public void changeProcedure() {
    	if(document.getProcDef() != null) {
    		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docu.startProcedure") );
    	}
    }
    /**
     * Настройки на регистрaтура - без значение дали е за нов документ или е актуализация
     */
    private void  docRegSettings() {
 	  try {
 		    //В собствен документ да се поддържа за кого се отнася, т.е да се вижда ли полето "Кореспондент" или да не се вижда
 			Integer s1 = ((SystemData) getSystemData()).getRegistraturaSetting(document.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_8);
 			if(Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_DA)) {
 				showCoresp = true;
 			}else {
 				showCoresp = false;
 			}
 			//Допуска се ръчно въвеждане на съгласувал и подписал в работен документ
 			s1 = ((SystemData) getSystemData()).getRegistraturaSetting(document.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_4);
 			if(Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_DA)) {
 				editReferentsWRK = true;
 			}else {
 				editReferentsWRK = false;
 			}
 			
 			//Изпълнител на задача може да бъде от друга регистратура - тук се използва за "бързите" задачи от тип резолюция
			s1 = sd.getRegistraturaSetting(ud.getRegistratura(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_16);
			if(Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_NE)) {
				Object[] codeExt = new Object[] {OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA, ud.getRegistratura().toString(), IndexUIbean.getMessageResourceString(beanMessages,"task.msgCodeExt")};
				setCodeExtCheck(codeExt);
			}else {
				setCodeExtCheck(null);
			}
			
			//да се позволи въвеждане на начин на подписване на официалните документи
			showSignMethod = null;
			s1 = ((SystemData) getSystemData()).getRegistraturaSetting(document.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_17);
 			if(s1 != null) {
 				showSignMethod = s1;
 				if(s1!= 0  && document.getId() == null) {
 					document.setSignMethod(s1);
 				} 				
 			}
 			
 		
 		} catch (DbErrorException e) {
 			LOGGER.error("Грешка при извичане на настройки на регистратура: {} ", document.getRegistraturaId()+" ! ", e);
 			
 		}
    }
    
   
    
	/**
	 * настройки на регистртура - нов документ
	 */
	private void newDocRegSettings(boolean freeA) {
		try {
			avtomNo = true; 
			// Код на значение "Включен по подразбиране чек-бокс за генериране на рег. номер на документ" класификация "Настройки на регистратура" 151
			Integer s1 = sd.getRegistraturaSetting(document.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_1);
			if( Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_NE)) {
				avtomNo = false; 
			}
			
			if(freeA) {
				this.document.setFreeAccess(OmbConstants.CODE_ZNACHENIE_NE);
				//limitedAccessCh = true;
				// Код на значение "Документи и преписки по подразбиране са с ограничен достъп" класификация "Настройки на регистратура" 151
				s1 = sd.getRegistraturaSetting(document.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_12);
				if( Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_NE)) {
					//limitedAccessCh = false; 
					this.document.setFreeAccess(OmbConstants.CODE_ZNACHENIE_DA);
				}
			}
			setLimitedAccessCh(!Objects.equals(document.getFreeAccess(), OmbConstants.CODE_ZNACHENIE_DA));
						
			// ? В собствен документ да се поддържа за кого се отнася (да се вижда кореспондента)
			
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при извичане на настройка CODE_ZNACHENIE_REISTRATURA_SETTINGS_1 на регистратура: {} ", document.getRegistraturaId()+" ! ", e);
		}
	}
	
	/**
	 * зачиства специфичини полета в документа- когато от раб. док., трябва да направя нов официален 
	 */
	private void clearDocSpecData(String newRnDoc){

		// да нулирам рег. номер и дата
		document.setRnDoc(newRnDoc); 
		document.setRnPored(null);
		document.setRnPrefix(null);
		document.setDeloIncluded(false);
		document.setWorkOffData(null);
		document.setDocAccess(null);
		document.setHistory(null);
		document.setUserLastMod(null);
		document.setDateLastMod(null);
		document.setGuid(null);
		if(this.flagFW == REG_OTHER_R || this.flagFW  == REG_WRK_DOC) {
			// да нулирам полетата за статус, валидност и т.н.
			document.setStatus(null);
			document.setStatusDate(null);
			document.setValid(null);
			document.setValidDate(null);
			//document.setDocInfo(null); // забележката да не се прехвърля! В нея се записва доп. информация от движението при пререг. от друга рег.!
		}
		processedCh = false;
		readyForOfficial = false;
	}

		
	/**
	 * Запис на документ
	 */
	public void actionSave() {
	
		//Integer alg  = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRI, document.getRegisterId(), getUserData().getCurrentLang(), new Date(), OmbClassifAdapter.REGISTRI_INDEX_ALG);
		//  проверки дали избрания тип док. и регистър са съвместими - повреме на записа - излиза съобщение 
		
		if(checkDataDoc()) { 			
			try {
				
				documentParamsAPC();
				
				// файловете за нов документ - да се запишат с него
				// ако е редакция на документ - се записват при upload
				boolean newDoc = this.document.getId() == null;
				
				//TODO Трябва да се измисли първо как ще се индексират прикачените файлове
//				List<String> ocrDocs = this.document.getOcr(this.filesList);
			
				saveDoc(newDoc);
				  			 				
				clearDeloDocLink(); // махам връзката, ако се натисне втори път запис;  само за нов документ deloDocPrep.getId() би могло да е != null
				createPrepOld = createPrep; // ако ще се запазват данни за док. "Нов док. със запазване на данни"
				if(newDoc || createPrep) {
					rnFullDocEdit = DocDAO.formRnDoc(this.document.getRnDoc(), this.document.getPoredDelo());
			    }
				
				btnObraboten(newDoc, true);			
				createPrep = false;
				
			    saveVrazkiTn(); // запис на връзките - от техен номер
				
				if(newDoc) {// само за нов документ	
					
					// заключване на док.
					lockDoc(this.document.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
					
					newDocFromMail();
										
					flagFW = UPDATE_DOC;
				}
				// да обнови статуса на процедурата
				enableProc();	
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );		
			} catch (ObjectInUseException e) {
				LOGGER.error("Грешка при запис на документа! ObjectInUseException "); 
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} catch (BaseException e) {			
				LOGGER.error("Грешка при запис на документа! BaseException", e);				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} catch (Exception e) {
				LOGGER.error("Грешка при запис на документа! ", e);					
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}
	}
	
	
	/**
	 * зарежда параметрите на документа - access(достъп), processed(обработен), competence(по компетентност)
	 */
	private void documentParamsAPC(){
		if(limitedAccessCh) {
			this.document.setFreeAccess(OmbConstants.CODE_ZNACHENIE_NE); //ограничен достъп
		}else {
			this.document.setFreeAccess(OmbConstants.CODE_ZNACHENIE_DA); // свободен достъп (общодостъпен)
		}	
		
		if(Objects.equals(document.getDocType(), OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) && 
		   Objects.equals(document.getDocVid(), OmbConstants.CODE_ZNACHENIE_DOC_VID_PZAPISKA) ) {
			this.document.setProcessed(OmbConstants.CODE_ZNACHENIE_DA); // ако е работен и е паметна запискa - винаги да е "обработен"
		}else if(processedCh) {
			this.document.setProcessed(OmbConstants.CODE_ZNACHENIE_DA);
		}else {
			this.document.setProcessed(OmbConstants.CODE_ZNACHENIE_NE);
		}	
		
		
		if(!competence){
			this.document.setCompetence(OmbConstants.CODE_ZNACHENIE_COMPETENCE_OUR);
			this.document.setCompetenceText(null);
		} else if(!Objects.equals(this.document.getCompetence(), OmbConstants.CODE_ZNACHENIE_COMPETENCE_SENT)) {
			this.document.setCompetence(OmbConstants.CODE_ZNACHENIE_COMPETENCE_FOR_SEND);
		}
	}
	
	/**
	 * извиква метода за запис на документ и файловете в обща транзакция
	 * @param newDoc
	 * @throws BaseException
	 */
	private void saveDoc(boolean newDoc) throws BaseException {
		JPA.getUtil().runInTransaction(() -> { 
			document.setCountFiles(filesList == null ? 0 : filesList.size());
			
			if(specDocId != null  && document.getId() == null) {
				this.document = this.dao.saveNew(document, specDocId, specVidDoc, getSystemData());
			} else if(workDocId != null  && document.getId() == null && avtomNo) {
				// рег. на официален от работен - само, ако е оставено автом. генер. на номера - официалния да влезе в оф. раздел на преписката на работния
				this.document = this.dao.saveNew(document, workDocId, null, getSystemData());
			} else {	
				this.document = this.dao.save(document, createPrep, deloDocPrep.getDeloId(), sourceRegDvijId, getSystemData());
			}
			if (newDoc && filesList != null && !filesList.isEmpty()) {
				// при регистриране на офицален от работен!!!
				// да направи връзка към  новия документ
				FilesDAO filesDao = new FilesDAO(getUserData());
				for (Files f : filesList) {
					filesDao.saveFileObject(f, this.document.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
				}
			}	
			
			if(membersTabForDel) {
				 //имало е таб участници за предишния вид. док., а сега няма
				 //проверка дали преди това е имало въведени вече участници - ако да - трябва да се изтрият на записа на документа
				 int brMem = dao.findDocMembersCount(document.getId());
				 if(brMem > 0) {
					 dao.deleteDocMembers(document.getId());
					 DocDataMembers bean = (DocDataMembers) JSFUtils.getManagedBean("docDataMembers");
					 if(bean != null) {
						 bean.setRnFullDoc(null); // за да е сигурно, че ако пак този таб е видим - ще се зареди всичко наново!
					 }
				 }
			}
			
			if(egovMess != null) {
				// 1. да се направи update на съобщението в table EGOV_MESSAGES
				// 2. да се изпрати отговор, че е регистрирано!
				createReturnEGOV(true);
			}
		});
	}

	
	
	/**
	 * проверка за задължителни полета при запис на документ
	 */
	public boolean checkDataDoc() {
		boolean flagSave =  checkDates();
		
		if(!avtomNo && SearchUtils.isEmpty(document.getRnDoc())) {
			JSFUtils.addMessage(DOCFORMTABS+":regN",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "repDoc.regnom")));
			flagSave = false;
		}
		
		if(document.getDocVid() == null) {
			JSFUtils.addMessage(DOCFORMTABS+":dVid:аutoCompl",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.vid")));
			flagSave = false;
		}else if ( Objects.equals(document.getDocType(), OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN)) {
			// за собствените - полето "Обработен" не се вижда, но да има стойност "ДА"
			document.setProcessed(OmbConstants.CODE_ZNACHENIE_DA);
		}
		
		if(document.getRegisterId() == null) {
			JSFUtils.addMessage(DOCFORMTABS+":registerId:аutoCompl",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.register")));
			flagSave = false;
		}
		
		if(SearchUtils.isEmpty(document.getOtnosno())) {
			JSFUtils.addMessage(DOCFORMTABS+":otnosno",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.otnosno")));
			flagSave = false;
		}
		
		
		// проверка за валиден номер - в метода за запис в дао...
		
		return flagSave ;
	}

	/**
	 * Проверка за валидни дати в документа
	 * @return
	 */
	private boolean checkDates() {
		int flagDatesOk = 0;// всичко е ок
		
		Date docDate = DateUtils.startDate(document.getDocDate());
		if(docDate == null || docDate.after(DateUtils.startDate(new Date()))) {
			JSFUtils.addMessage(DOCFORMTABS+":regDat",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.docDate")));
			flagDatesOk ++;
		} else { 
			//дата на техен номер - преди дата на документа
			Date tmpDate = DateUtils.startDate(document.getTehDate());
			flagDatesOk += checkDatesA(tmpDate, docDate, ":tehDat", "docu.datTehNom", MSGVALIDDATES);
		
			// дата на получаване - преди дата на документа
			tmpDate = DateUtils.startDate(document.getReceiveDate());
			flagDatesOk += checkDatesA(tmpDate, docDate, ":receiveDat", "docu.receiveDate", MSGVALIDDATES);
						
			// дата на валидност - след дата на документа
			tmpDate = DateUtils.startDate(document.getValidDate());
			if(tmpDate == null || document.getDocType().equals(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK)) {
				document.setValidDate(docDate);
			} else {
				flagDatesOk += checkDatesB(tmpDate, docDate, ":validDat", "docu.validDate",MSGVALIDDATES);
			}
			// дата на статус на обработка - след дата на документа
			tmpDate = DateUtils.startDate(document.getStatusDate());
			flagDatesOk += checkDatesB(tmpDate, docDate, ":statusDat", "docu.statusDate", MSGVALIDDATES);

			// дата за връщане на отговор - след дата на документа
			tmpDate = DateUtils.startDate(document.getWaitAnswerDate());
			flagDatesOk += checkDatesB(tmpDate, docDate, ":replayDat", "docu.answerDate", MSGVALIDDATES);

		}
		return flagDatesOk == 0;
	}
	
	
	/**
	 *  проверка за валидност на дата - преди дата на документа
	 * @param tmpDate
	 * @param docDate
	 * @param idComp
	 * @param msg
	 * @return
	 */
	public int checkDatesA(Date tmpDate, Date docDate, String idComp, String msg, String msgValid) {
		int bb = 0;
		if(tmpDate != null && tmpDate.after(docDate)){
			JSFUtils.addMessage(DOCFORMTABS+idComp,FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,msgValid,getMessageResourceString(LABELS, msg)));
			bb++;
		}
		return bb;
	}
	
	
	/**
	 * проверка за валидност на дата - след дата на документа
	 * @param tmpDate
	 * @param docDate
	 * @param idComp
	 * @param msg
	 * @return
	 */
	public int checkDatesB(Date tmpDate, Date docDate, String idComp, String msg, String msgValid ) {
		int bb = 0;
		if(tmpDate != null && tmpDate.before(docDate)){
			JSFUtils.addMessage(DOCFORMTABS+idComp,FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,msgValid,getMessageResourceString(LABELS, msg)));
			bb++;
		}
		return bb;
	}
	
	/**
	 * Изтриване на деловоден документ
	 * права на потребителя за изтриване
	 * 
	 */
	public void actionDelete() {
		try {
	
			deleteDoc(document.getId());
			this.dao.notifDocDelete(document, sd);

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,  IndexUIbean.getMessageResourceString(UI_beanMessages, "general.successDeleteMsg") );
			
			actionNewDocument(false, true);			
			createPrep = false;
		} catch (ObjectInUseException e) {
			// ако инициира преписка и има други връзки 
			LOGGER.error("Грешка при изтриване на документа!", e); 
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, OBJECTINUSE), e.getMessage());
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на документа!", e);			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} 
	}
	
	
	/**
	 * Изтрива документа
	 * @throws BaseException 
	 */
	public void deleteDoc(Integer idDoc) throws BaseException  {
		JPA.getUtil().runInTransaction(() ->  {
			this.dao.deleteById(idDoc);
			
			if (filesList != null && !filesList.isEmpty()) {
			
				FilesDAO filesDao = new FilesDAO(getUserData());
				for (Files f : filesList) {
					filesDao.deleteFileObject(f);	
				}
			}
		}); 
	}
	
	/**
	 * "За резолюция" - нови индивидуални задачи от тип "резолюция"
	 */
	public void actionTaskRezol() {
	//		String path = DOCFORMTABS+":lstIzpR:dialogButtonM";
	//		String  cmdStr = "document.getElementById('"+path+"').click()";
		if(rezolTask == null ) {
			// да се запазят стойностите,само ако задачата е нова и е натиснат бутон "потвърждение" - иначе се възприема като отказ
			rezolTask = new Task();   
			rezolExecClassif = new ArrayList<>();
		}
		String  cmdStr = "PF('modalRezol').show();";
		PrimeFaces.current().executeScript(cmdStr);	
	}
	
	/**
	 * "За резолюция" бутон потвърждение от модалния - индивидуални задачи от тип "резолюция"
	 */
	public void actionSaveRezol() {
		if(rezolTask.getCodeExecs() != null && !rezolTask.getCodeExecs().isEmpty()) {
			saveRezol(); //  да записва веднага - бутона "за резолюция" се показва само за вече записани документи
			String  cmdStr = "PF('modalRezol').hide();";
			PrimeFaces.current().executeScript(cmdStr);
			
			docProcessed(Objects.equals(document.getProcessed(), OmbConstants.CODE_ZNACHENIE_DA)); 
		} else {	
			JSFUtils.addMessage(DOCFORMTABS+":lstIzpR:autoComplM",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS, getMessageResourceString(LABELS, "task.rezolExec")));						
		}
	}
	
	/**
	 * "За резолюция" - запис на индивидуални задачи от тип "резолюция" - без срок
	 */
	public void  saveRezol() {		

		rezolTask.setAssignDate(new Date());
		rezolTask.setStatusDate(new Date());
		rezolTask.setStatus(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP); // неизпълнена 
		rezolTask.setDocId(this.document.getId());
		rezolTask.setRegistraturaId(ud.getRegistratura());
		rezolTask.setTaskType(OmbConstants.CODE_ZNACHENIE_TASK_TYPE_REZOL); // за резолюция code-1
		rezolTask.setDocRequired(OmbConstants.CODE_ZNACHENIE_NE); // изисква ли се документ при приключване на задачата
		rezolTask.setCodeAssign(ud.getUserSave());  //  за възложител по подразбиране да е текущия потребител или този делегирани права!!!
		
	    try {
			JPA.getUtil().runInTransaction(() -> rezolTask = new TaskDAO(getUserData()).save(rezolTask, true, document, sd));
					
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docu.msgSaveRezol") );
			
			rezolTask = new Task();	
			rezolExecClassif = new ArrayList<>();		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на резолюция (задача)! ", e);			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}	
			
	}
	
	
	
	
	
	/**
	 * Шаблони за вида документ 
	 */
	public void actionDocTemplate() {
		if(docSettingId != null &&  templatesList == null) {
			try {
				JPA.getUtil().runWithClose(() -> {
					FilesDAO daoF = new FilesDAO(getUserData());		
					templatesList = daoF.selectByFileObject(docSettingId, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_VID_SETT); 
				 
				});
			} catch (BaseException e) {
				LOGGER.error("Грешка при зареждане на шаблони на документи ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}
		}
	
		if (templatesList != null) {
			if(templatesList.size() > 1) {
				String  cmdStr = "PF('modalDocTml').show();";
				PrimeFaces.current().executeScript(cmdStr);
			}else if( templatesList.size() == 1) {
				//ако само един файл - да не отваря модалния				
				loadFilesFromTemplate(templatesList.get(0));
			}
		}
	}
	
		
	/**
	 * извлича шаблона и директно го записва в документа
	 * @param ftmpl
	 */
	public void loadFilesFromTemplate(Files ftmpl) {	
		Files newFile = new Files();
		FilesDAO dao = new FilesDAO(getUserData());	
		try {		
			ftmpl = dao.findById(ftmpl.getId());	
			if(ftmpl != null) { 
				if( ftmpl.getContent() == null){					
					ftmpl.setContent(new byte[0]);
				}
				
				newFile.setFilename(ftmpl.getFilename());
				newFile.setContentType(ftmpl.getContentType());
				newFile.setContent(ftmpl.getContent());
				newFile.setOfficial(OmbConstants.CODE_ZNACHENIE_DA);
				filesList.add(newFile);
				
				if(document.getId() != null) {
					JPA.getUtil().runInTransaction(() -> dao.saveFileObject(newFile, document.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC));
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.succesSaveFileMsg") );
					actionChangeFiles();
				}
			}
		} catch (BaseException e) {
			LOGGER.error("Грешка при извличане на шаблон на документ! ", e);
		} finally {
			JPA.getUtil().closeConnection();
		}
		
	
	
	}
	
	
	/**
	 * "За запознаване" - изричен достъп + нотификация
	 */
	public void actionDocAccess() {
		try {
			if (document.getDocAccess()==null || document.getDocAccess().isEmpty()) {
				JPA.getUtil().runWithClose(() -> this.dao.loadDocAccess(document));
			}
			((CompAccess)FacesContext.getCurrentInstance().getViewRoot().findComponent(DOCFORMTABS).findComponent("docAccessComp")).initAutoCompl();
		
		} catch (BaseException e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		}
		String  cmdStr = "PF('modalAccess').show();";
		PrimeFaces.current().executeScript(cmdStr);
	}

	/**
	 * Изричен достъп - справка за достъп
	 */
	public void actionFillDocAccessList() {

		try {
			
			SelectMetadata sm = dao.createSelectDocAccessList(document.getId(), sd);
			docAccessList = new LazyDataModelSQL2Array(sm, "A2, A1");
		} catch (DbErrorException e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		}

		String  cmdStr = "PF('modalAccessSpr').show();";
		PrimeFaces.current().executeScript(cmdStr);
	}
	
   /** "Изричен достъп" - запис на целия документ 
    * 
    */
   public void actionConfirmAccess() {
	    int flagOk = 2;
	    // указания при насочване на документ	
		boolean noteNotEmpty = !SearchUtils.isEmpty(noteAccess); //&&	document.getDocAccess()!=null && !document.getDocAccess().isEmpty() ;
		for(DocAccess item: document.getDocAccess() ) {
			if(item.getId() == null && noteNotEmpty) { // само нови да добави указанията
				item.setNote(noteAccess);
				flagOk = 0;
			}else if(item.getId() == null && !noteNotEmpty) {
				flagOk = 1;
				break;
			}
		}

	    if(flagOk == 1){
	    	JSFUtils.addMessage(DOCFORMTABS+":noteAcc",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS, getMessageResourceString(LABELS, "docu.msgZaZapoznavane") ));
	    }else if( flagOk == 2 && noteNotEmpty)	{
	    	// има указания, няма лица.... docAccessComp:tblDeloList_head
	    	JSFUtils.addMessage(DOCFORMTABS+":docAccessComp:tblDeloList",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS, getMessageResourceString(LABELS, "docu.msgZaZapoznavane2") ));
		}else if(flagOk == 0 || (flagOk == 2 && !noteNotEmpty)) { 
			String  cmdStr = "PF('modalAccess').hide();";
			PrimeFaces.current().executeScript(cmdStr);
			actionSave(); // да запиша целия документ - ако има нови или изтрити - заради достъпа!!
			noteAccess = null;
		}
   }
	   
	
   /**При промяна на типа документ
    * @return
    */
   public void actionChangeDocType(ValueChangeEvent event) {
	   Integer newTypeDoc = (Integer)event.getNewValue();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("actionChangeDocType= {}", newTypeDoc);
		}
	    nastrDocType(newTypeDoc);
	    actionChangeDocVid(true);	    	    
   }


   /**Изключва автоматичното генериране на номера
    * 
    */
   public void actionChangeAvtomNo() {
	   if(this.avtomNo) {
		   this.document.setRnDoc(null); 
	   }	
 	   // да се махне и връзката с преписката!!!
	   clearDeloDocLink();
   }
   
  
   /**
    * ДА / НЕ -  потвърждание за смяна на състоянието "Обработен / Необработен"
    * @param bProcessed - новото състояние на бутона
    * @param f - 1- "Да"
    */
   public void actionConfirmProcessed(boolean bProcessed, int f) {
	   if(f==OmbConstants.CODE_ZNACHENIE_DA && document.getDocType().equals(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK)) { 
		   //ако потвърдят - да док. да стане "Не е готов за регистрация"
		   //ако е маркирано "Готов за регистрация", документа винаги е "Необработен"
		   //обработен става след регистрирането на работния като официален
		   this.document.setForRegId(null);
		   this.readyForOfficial = false;
	   } else if (f==OmbConstants.CODE_ZNACHENIE_NE){ // ne
		   docProcessed(!bProcessed);
	   }
	   
   }
   
   /**
    * Обработен/не обработен
    * @param bProcessed
    */
   private void docProcessed( boolean bProcessed) {
	   if(bProcessed) {
		   processedCh = true;
	   }else {
		   processedCh = false;
	   }
   }

   /**
    * Компетентност
    */
   private void docCompetence() {
	   boolean bCompetence = document.getCompetence() == null || Objects.equals(document.getCompetence(), OmbConstants.CODE_ZNACHENIE_COMPETENCE_OUR); // наша комп. или ако е null
	   if(bCompetence) {
		   competence = false;
	   }else {
		   competence = true; // за изпращане
	   }
   }
   
   
  /**
   * Премахва отлагането на документа в преписката - при ръчно изписване на номера на документа
   */
   public void clearDeloDocLink() {
	  selectedDelo = null;
	  deloDocPrep = new DeloDoc();
	  deloDocPrep.setDelo(new Delo());
   }
   
	/**
	* Ръчно въвеждане на номер на документ
    * Търсене на преписка с въведения номер - при излизане от полето
     * @param rnEQ  - true- пълно съвпадение на номера
	 */
   public void actionSearchRnDelo(boolean rnEQ) {
	   clearDeloDocLink();	 
	   if(!SearchUtils.isEmpty(document.getRnDoc()) || !rnEQ) {
		   selectedDelo =  searchRnDelo( document.getRnDoc(),  "mDeloS",  rnEQ, new Date());
	   }
	}
   
   /**
    * Търси преписка по номер - ръчно въвеждане на номера - бутон "Търси"
    */
   public void actionSearchRnDeloBtn() {
	   if(selectedDelo == null) { // ако е намерена вече преписка да не се пуска пак търсенето
		   actionSearchRnDelo(false);
	    }
   }
   /**
    * Търсене на преписка по номер
    * @param rnDelo
    * @param varModal
    * @param rnEQ
    * @param nastr
    * @param inpDate
    * @return
    */
   private Object[] searchRnDelo(String rnDelo, String varModal, boolean rnEQ, Date inpDate) {
	   Object[] sDelo = null;
	   rnDelo  =  SearchUtils.trimToNULL_Upper(rnDelo);
       DeloSearch  tmp = new DeloSearch(document.getRegistraturaId());
       tmp.setUseDost(false); // да не се ограничава достъпа!! За сега
	   tmp.setRnDelo(rnDelo);
	   tmp.setRnDeloEQ(rnEQ);
	   tmp.buildQueryComp(getUserData());
	
	   LazyDataModelSQL2Array lazy =   new LazyDataModelSQL2Array(tmp, "a1 desc");
	   if(lazy.getRowCount() == 0 && rnEQ) {
		  
		   clearDeloDocLink(); 
		   //	LOGGER.debug("Не е намерена преписка с посочения номер!");
		   
	   } else if(lazy.getRowCount() == 1 && rnEQ) { // само при пълно съвпадение на номера
		   
		   List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);
		   sDelo = new Object[8];
		   if(result != null) {
			    sDelo =  result.get(0);		
			   	loadDataFromDeloS(sDelo, inpDate); // документ - в преписка
			   
		   }	
		   
			LOGGER.debug("Намерена е само една преписка с този рег. номер - данните да се заредят без да излиза модалния");
		   
	   } else {
		   sDelo = new Object[8];		
		   String  cmdStr = "PF('"+varModal+"').show();";
		   PrimeFaces.current().executeScript(cmdStr);
	   }
	  return sDelo;		  
   }
   
   
   /**
    * Зарежда данните за избраната преписка 
    * nastr = true - ръчно въвеждане на номера на документа
    * 0]-DELO_ID<br>
	* [1]-RN_DELO<br>
	* [2]-DELO_DATE<br>
	* [3]-STATUS<br>
	* [4]-DELO_NAME<br>
	* [5]-INIT_DOC_ID<br>
	* [6]-REGISTER_ID<br>
	* [12]-FREE_ACCESS
    */
   private void loadDataFromDeloS(Object[] sDelo, Date inpDate) {	  
	    deloDocPrep = new DeloDoc();
	    deloDocPrep.setDelo(new Delo());	    
		if(sDelo[0] != null) {
			deloDocPrep.setDeloId(Integer.valueOf(sDelo[0].toString())); 
		}
		
		Date datd = (Date)sDelo[2];
		if(datd != null ){
			deloDocPrep.getDelo().setDeloDate(datd);
		}
		
		if(inpDate == null) {
			inpDate = new Date();
		}
		deloDocPrep.setInputDate(inpDate);
		deloDocPrep.getDelo().setStatus(Integer.valueOf(sDelo[3].toString()));
		
		String tmpstr = (String)sDelo[1];
		deloDocPrep.getDelo().setRnDelo(tmpstr);	
		

		deloDocPrep.setTomNomer(1);  // по подразбиране
		deloDocPrep.setEkzNomer(1);  //по подразбиране- 1-ви екз. ; раздела се зарежда при записа - за входящи - офицаилен, за собств. и раб. - вътршен
		
	
		// извиква се през полета за ръчно въвеждане на номер на документ
		 document.setRnDoc(tmpstr);
		// иницииращ документ- регистър 	
		
		if(sDelo[6] != null ){
			Integer initDocReg = Integer.valueOf(sDelo[6].toString());
			if( getUserData().hasAccess(OmbConstants.CODE_CLASSIF_REGISTRI, initDocReg)) {
				//само, ако има право да въвежда в този регистър
				document.setRegisterId(initDocReg); // винаги сменям регисъра, ако е върнат....
				actionChangeRegister();// да извлека настройките на регистъра
			}
		}	
		
		// "При влагане на нов документ в преписка, в него да се копира „относно“ на последния документ от преписката" - nastrojka???
		//  Как ще се използва??? ТODO
   	    //  относно - наименование на преписката  
		tmpstr = (String)sDelo[4];
		if(!SearchUtils.isEmpty(tmpstr)  ){
		   if (!SearchUtils.isEmpty(document.getOtnosno())) {
			   tmpstr += "\n"+ document.getOtnosno();
		   }
		   document.setOtnosno(tmpstr);
		}
		
	
		if(sDelo[12] != null ){
			// новият документ да е със същия дотъп като на прписката, в която се влага
			document.setFreeAccess( Integer.valueOf(sDelo[12].toString()));
			setLimitedAccessCh(!Objects.equals(document.getFreeAccess(), OmbConstants.CODE_ZNACHENIE_DA));
		}
		
		
		try {
			String msg =  deloDocPrep.getDelo().getRnDelo() +" / "+ DateUtils.printDate(deloDocPrep.getDelo().getDeloDate()) +
							"; "+  getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DELO_STATUS, deloDocPrep.getDelo().getStatus(), getUserData().getCurrentLang(), new Date());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docu.addDeloMsg1", msg) );
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при разкодиране на статус на преписка ! ", e);
		}
		
		// ако е от минала година???? Съобщение? -   настройка на регистратура
	}
   
   
	/**
	 * Затваряне на модалния за избор на преписка - ръчно въвеждане на номера
	 */
   public void actionHideModalDelo() {
	   if(selectedDelo != null && selectedDelo[0] != null) {
		   // да заредя полетата
		   loadDataFromDeloS(selectedDelo,  null); // ръчно въвеждане на рег. номер
		
	   } else {
		   selectedDelo = null;
	   }
   }
   
   
  
   /**
    * Извлича настройките по вид документ
    * @param flagCh = true - при промяна на вида документ; false = зареждане за актуализация
    */
   public void actionChangeDocVid(boolean flagCh) {	
	   // За нов документ  - Да променям ли регистъра, ако предварително е избран - да!
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(" actionChangeDocVid= {}", document.getDocVid());
		}
	    createPrep = false; 
	    docSettingId = null;
	    templatesList = null;
		try {						
		   Integer oldDocSettingHar = docSettingHar; 
		   docSettingHar = null;
		   membersTabForDel = membersTab != null ? true : false; // true - имало е таб "Участници" преди смяната на вида документ
		   membersTab = null;
		   Object[] docSettings = dao.findDocSettings(document.getRegistraturaId(),document.getDocVid(),getSystemData());
		   if(docSettings != null) {
 			    // само за нов док.
			    docSettingsForNewDoc(docSettings);
			    
				// регистратура по подразб. за рег. на офц док. от работен 
				if(docSettings[3] != null && 
				   flagCh && Objects.equals(document.getDocType(), OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) && document.getWorkOffId() == null){
					//само, ако е работен и вече не е регистриран!! 
					this.document.setForRegId(null); // регистртура, в която да се регистрира официалния
					this.readyForOfficial = false;
					document.setPreForRegId(Integer.valueOf(docSettings[3].toString())); 
				}
				
				// ИД-то на настройката само ако има шаблони!
				if(docSettings[4] != null) { 
					docSettingId = Integer.valueOf(docSettings[4].toString());
				}

				// Характеристики на специализиран документ 
				docSettingsDopData(docSettings,  oldDocSettingHar,  flagCh);
			
				// Име на таба участници
				if(docSettings[9] != null) { 
					membersTab = docSettings[9].toString();
					membersTabForDel = false;
				}
		   }
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при промяна на вид документ)! ", e);
		}	
   }
   
   /**
    * Настройки по вид док. - само, за нов докуменът
    * @param docSettings
    * @throws DbErrorException
    */
   private void  docSettingsForNewDoc(Object[] docSettings) throws DbErrorException{
		if(document.getId() == null) {  		
			if(docSettings[1] != null ) {
				// При актуализация да не се променя регистъра. Това ще става през дейност "Пререгистрация"
				Integer nReg = Integer.valueOf(docSettings[1].toString());
				loadRegNewDoc(nReg);					
			}				
			// зарежда процедура по подразбиране, ако има такава
			loadProcedure(docSettings);
			// да създава преписка - за работния документ да се игнорира!
			boolean tvdWrk = Objects.equals(document.getDocType(), OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK);
			if( !tvdWrk  && Objects.equals(docSettings[2], OmbConstants.CODE_ZNACHENIE_DA)) {
				createPrep = true;
			}
		}
   }

   /**
    *  Характеристики на специализиран документ 
    * @param docSettings
    * @param oldDocSettingHar
    * @param flagCh
    */
   private void docSettingsDopData(Object[] docSettings, Integer oldDocSettingHar, boolean flagCh) {
		// Характеристики на специализиран документ 
		if(docSettings[8] != null) {
			document.setDopdata(new DocDopdata());
			docSettingHar = Integer.valueOf(docSettings[8].toString());
			boolean bb  = flagCh ? Objects.equals(docSettingHar, oldDocSettingHar) : true;
			if(document.getId() != null && bb ) {
				try {
					JPA.getUtil().runWithClose(() -> {
						DocDopdata dopData = this.dao.findDocDopdata(document.getId());
						if(dopData != null) {
							document.setDopdata(dopData);
						}
					});
				} catch (BaseException e) {
					LOGGER.error("Грешка при извличане допълнителните данни за специализиран документ! ", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
				}
			}
		}else {	//DocDopData - да се нулира, ако за вида документ не е указано, че е специализиран!
			document.setDopdata(null);
		}
   }
   
   /**
    * при смяна на вид документ 
    * само за нов документ. При актуализация да не се променя регистъра. Това ще става през дейност "Пререгистрация"
    * @param nReg
    * @throws DbErrorException
    */
   private void loadRegNewDoc(Integer nReg) throws DbErrorException {
		if( getUserData().hasAccess(OmbConstants.CODE_CLASSIF_REGISTRI, nReg)) {
			// само, ако има право да въвежда в този регистър
			Integer docTypeReg  = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRI, nReg, getUserData().getCurrentLang(), new Date(), OmbClassifAdapter.REGISTRI_INDEX_DOC_TYPE);
			if((docTypeReg == null && !Objects.equals(document.getDocType(),OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK)) 
				|| Objects.equals(docTypeReg, document.getDocType())) { // само, ако регистъра е за вх. и собствени (БЕЗ работните) или за избрания тип документ
					document.setRegisterId(nReg);//REGISTER_ID
			}
		}else if(docVidInclude != INCL_REGULAR ){
			String msg="specDoc.regJalbi";
			if(docVidInclude == INCL_NPM) {
				 msg="specDoc.regNpm";
			}else if(docVidInclude == INCL_SAMOSEZ) {
				 msg="specDoc.regSamosez";
			}
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(beanMessages, "specDoc.regSpecErr", getMessageResourceString(LABELS, msg)));// "Липсва регистър! За потребителя не е указан регистър ");			
		}
   }
   
 /**
  * Зарежда процедура по подразбиране, ако има такава за вида документ
  * @param docSettings
  */
   private void loadProcedure(Object[] docSettings) {
	   if(docSettings[5]!=null && document.getDocType().equals(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN)){
		   document.setProcDef(Integer.valueOf(docSettings[5].toString()));
	   }else if(docSettings[6]!=null && document.getDocType().equals(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN)){
		   document.setProcDef(Integer.valueOf(docSettings[6].toString()));
	   }else if(docSettings[7]!=null && document.getDocType().equals(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK)){
		   document.setProcDef(Integer.valueOf(docSettings[7].toString()));
	   }else {
		   document.setProcDef(null);
	   }
   }
   
   /**
    * работен документ - при смяна на "Готов за регистриране като официален"
    */
   public void actionChangeReadyFO() { 
	   if(this.readyForOfficial) {
		   try {	
				if(this.document.getPreForRegId() == null) { //тази проверка е само да се презастраховам, не би трявало да влиза тук, ако всичко е ОК 
				   // няма избрана предварително рег. - проверяваме настройките
				   // настройки по вид документ  - ако върне регистратура за регистрация - вземам нея
				   Object[] docSettings = dao.findDocSettings(document.getRegistraturaId(),document.getDocVid(),getSystemData());
				   if(docSettings != null && docSettings[3] != null) {
					  this.document.setPreForRegId(Integer.valueOf(docSettings[3].toString()));				
				   }  else {
					  this.document.setPreForRegId(getUserData(UserData.class).getRegistratura()); // текуща регистртурата);   
				   }
				} 			
               	// ако  полето ForRegId != null => док. е готов за рег. като официален 
               	this.document.setForRegId(this.document.getPreForRegId());
 			    this.processedCh = false; //раб. документ да се маркира като "Необработен"
		   } catch (DbErrorException e) {
				LOGGER.error("Грешка при извличане на настройки по вид документ)! ", e);
		   } 
			 
	   } else {
		   // Не е готов;
		   this.document.setForRegId(null);   
	   }	 
   }


   /**При промяна на регистъра 
    * 
    */
   public void actionChangeRegister() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug(" actionChangeRegister= {}", document.getRegisterId());
		}
		Integer alg = null;
		avtomNoDisabled =  false;
		try {
			 if(document.getRegisterId() != null) {
				 alg  = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRI, document.getRegisterId(), getUserData().getCurrentLang(), new Date(), OmbClassifAdapter.REGISTRI_INDEX_ALG);
			 }
			 if(alg != null && alg.equals(OmbConstants.CODE_ZNACHENIE_ALG_FREE)) {
				 this.avtomNo = false; // да се забрани автом. генер. на номера! Да се прави проверка за въведен номер, ако алгоритъмът е "произволен рег.номер"
				 avtomNoDisabled =  true;
			 }else if(SearchUtils.isEmpty(document.getRnDoc())){
				 this.avtomNo = true; // да се промени според регистъра, само ако вече няма нищо въведено в полето за номер на документ 
			 }else if(specDocId != null) {
				 document.setRnDoc(null);; // само, ако идва от жалаба, нпм, самосез. и е сменен регистъра по подразбиране и той не е с произв. номер! - да се нулира rndoc на докумета
			 }
			 
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при промяна на регистъра на деловоден документ )! ", e);
		}						

   }
 
   /**При промяна на файловете
    * обновяване на броя файловете - само за стар документ
    */
   public void actionChangeFiles() {
	    if(document.getId() != null) {  
			try {
				JPA.getUtil().runInTransaction(() -> { 
					Integer countFiles = (filesList == null ) ? 0 : filesList.size(); 
					dao.updateCountFiles(document,  countFiles);
					
					// TODO тази нотификация тука няма да се случва
//					try {
//						Query q = JPA.getUtil().getEntityManager().createNativeQuery("select CODE_REF from DOC_ACCESS_ALL where doc_id = :IDD"); //TODO - da se premesti ot tuk...
//						q.setParameter("IDD", document.getId());
//						
//						ArrayList<Object> all = (ArrayList<Object>) q.getResultList();
//						ArrayList<Integer> allI = new ArrayList<> ();
//						for (Object obj : all) {
//							allI.add(SearchUtils.asInteger(obj));
//						}
//						
//						Notification notif = new Notification(((UserData)getUserData()).getUserAccess(), null
//								, OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_FILE_CHANGE, OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_ALL_DOST, getSd());
//							notif.setDoc(document);
//							notif.setAdresati(allI);
//							notif.send();
//					} catch (Exception e) {
//						LOGGER.error("Грешка при изпращане на нотификация за променено файлово съдържание");
//						throw new DbErrorException("Грешка при изпращане на нотификация за променено файлово съдържание", e);
//					}
					
				});
			} catch (BaseException e) {
				LOGGER.error("Грешка при обновяване броя на файлове в документа! ", e);			
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG),e.getMessage());
			} 
	    }
   }
   
   private Integer reloadFile;
   
   public Integer getReloadFile() {
		return reloadFile;
	}
   
   
   //метода се извиква от компонентата за сканиране след успешно извършен запис на файл
   public void setReloadFile(Integer reloadFile) {
		
	//	System.out.println("setReloadFile ->"+reloadFile);
		
		if(reloadFile!=null) {
			reloadDocDataFile(); 
			actionChangeFiles();
		}
		
		this.reloadFile = reloadFile;
	}
   
   
   /**
    * Избор на задача от списъка - ид на зад. да се подаде на комп. taskData
    */
    public void actionSelectTask(Object[] row) {	  	 
		idTask = Integer.valueOf(row[0].toString());	    
    }
  


	/**
	 * търсене по техен номер - бутон 
	 */
	public void actionSearchDocBtnTn() {
		if(SearchUtils.isEmpty(this.document.getTehNomer())) {		
			actionCancelRelTn(true);
		} else if(searchFlagTn == 0){
			searchTehNomer(this.document.getTehNomer());
		}else {
			searchFlagTn = 0;
		}
	}
	
	
	/**
	 * Търсене по по техен номер
	 * извиква се
	 * 1. при въвеждане / промяна на техен номер
	 * 2. при първоначално зареждане на входящ док., ако има нещо в полето техен номер	 * 
	 * @param tehNomer
	 * @param tehDate
	 * @param coresp
	 */
	private void searchTehNomerSql(String tehNomer, Date tehDate, Integer coresp) {
		
		if (tehNomer == null) { 
			tehNomer = "";
		}
		
		DocSearch tmp = new DocSearch(getUserData(UserData.class).getRegistratura());
		tmp.setTehNomer(tehNomer);
		tmp.setDocDateTo(tehDate);
		tmp.setDocDateFrom(tehDate);
		tmp.setCodeRefCorresp(coresp);
		tmp.setTehNomerEQ(true);
		tmp.setMarkRelDocId(this.document.getId()); //Ако има вече създадена връзка - само да са маркирани. Не искам да се скриват, за да се видят всички док. които имат същия техен номер.
		// Но трябва да се изключи подадения!!! 
		//tmp.setUseDost(true);
		
		tmp.buildQueryComp(getUserData());
		LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(tmp, "a1 desc");
		this.tnRez =  lazy.getRowCount(); // бутонът с лупата ще се виждам само, ако има намерено нещо
		
	}
	
	/**
	 * Търсене по техен номер
	 */
	private Object[] searchTehNomer(String tehNomer) {
		
		Object[] sDoc = null;
		searchTehNomerSql(tehNomer, null, null);
		if (this.tnRez == 0 ) {			
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Не е намерен документ с посочения номер!");
			}
			JSFUtils.addMessage(DOCFORMTABS+":btnDocIn",FacesMessage.SEVERITY_INFO,	getMessageResourceString(LABELS, "docu.tehNMsgNotFound"));
			
		} else  {	
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Намерени са  документ! Дори да е един - да се отвори таблицата");
			}
			sDoc = new Object[5];
			String dialogWidgetVar = "PF('docTehNVar').show();";
			PrimeFaces.current().executeScript(dialogWidgetVar);
			searchFlagTn++;
		}		
		return sDoc;
	}
	
	/**
	 * премахва все още незаписани връзки 
	 */
	public void actionCancelRelTn(boolean clrRez) {
		selectedDocsTn.clear();	
		searchFlagTn=0;
		if(clrRez) {
			tnRez = 0;
		}
	}	
	

   /**
    * Рефрешва списъка със задачи при запис и изтриване от компонентата
    */
    public void actionRefreshTaskList() {
	   getTasksList().loadCountSql();
    }


	/**
	 *  запис на връзките - от техен номер
	 */
     public void saveVrazkiTn() {

		boolean saveVr = this.selectedDocsTn != null && !this.selectedDocsTn.isEmpty();
		try {
			if (saveVr) {
				JPA.getUtil().runInTransaction(() -> { 
					// запис на връзките - от техен номер
					DocDocDAO vrazkiDAO = new DocDocDAO(getUserData());
					Integer idObj = null;
					for (Object[] obj : this.selectedDocsTn) {
						idObj = SearchUtils.asInteger(obj[0]);
						vrazkiDAO.save(null, OmbConstants.CODE_ZNACHENIE_DOC_REL_TYPE_VRAZKA, this.document.getId(), idObj, false);
					}
					selectedDocsTn.clear();
				});
			
				//Задава параметър за рефрешване на списъка със свързани документи при запис на връзки по 'техен номер'
				DocDataVrazki docDataVrBean = (DocDataVrazki) JSFUtils.getManagedBean("docDataVrazki");
		 		if(	docDataVrBean != null) {
		 			docDataVrBean.setRefreshList(true);
		 		}
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docu.SuccesMsgVrTn") );
			}
		} catch (BaseException e) {
			LOGGER.error("Грешка при запис на връзки по техен номер! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG),e.getMessage());
		}			
		
     }
     
     
	/**
	 * търсене по техен номер - при скриване на модалния
	 */
	public void onHideModalTehN() {
		searchFlagTn = 0;
	}

	/**
	 * отказ от регистрация
	 */
	public void actionReject() {
		boolean rejectOk = false;
		if (flagFW == REG_OTHER_R ) {
			rejectOk = rejectOtherR(); //при рег. от други регистратури 
		} else if (flagFW == REG_FROM_MAIL || flagFW == REG_FROM_MAIL_JALBA) {
			rejectOk = rejectFromMail(); //при регистриране от e-mail
		}else if (flagFW == REG_FROM_EGOV ) {
			rejectOk = rejectFromEGOV(); //при регистриране от СЕОС или ССЕВ
		}
		if(rejectOk) {
			Navigation navHolder = new Navigation();
			navHolder.goBack();   //връща към предходната страница
		}
	}
	
	/**
	 * Отказ от регистрация  - друга регистратура
	 */
	private boolean rejectOtherR() {
		boolean rejectOk = false;
		if( SearchUtils.isEmpty(textReject)) {
			JSFUtils.addMessage(TXTREJECTEDID,FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, TXTREJECTED)));
		} else {
			// save reject
		  try {
				JPA.getUtil().runInTransaction(() -> this.dao.rejectDocAcceptance(this.sourceRegDvijId, textReject, ud.getRegistratura(), sd));
						
			//	JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );
				rejectOk = true;
		  } catch (BaseException e) {			
				LOGGER.error("Грешка при отказ от регистрация на документ! ", e);			
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		  }	
					
		}
		return rejectOk;
	}
	
	/**
	 * При регистрация от е-мейл - да върне отговр 
	 */
	public void newDocFromMail() {
		if(flagFW == REG_FROM_MAIL  || flagFW == REG_FROM_MAIL_JALBA) {
			// 1.мейла да се премести в друга папка					
			// 2. да се върне отговор! 						
			createReturnMail(true);		        
		} else if((flagFW == REG_FROM_EGOV || flagFW == REG_FROM_EGOV_JALBA) && egovMess != null) {
			egovMess = null;
		}
	}
	
	/**
	 * Отказ от регистрация - рег. на док. от е-маил
	 */
	private boolean rejectFromMail() {
		boolean rejectOk = false;
		if( SearchUtils.isEmpty(textReject)) {
			JSFUtils.addMessage(TXTREJECTEDID,FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, TXTREJECTED)));
		} else {
			createReturnMail(false);
			rejectOk = true;
		}	
		return rejectOk;
	}
	
	/**
	 * Формира и изпраща е-мейл за отговор
	 * Премества мейлите в папка "Inbox.registered" или  в "Inbox.rejected"
	 * @param registred - true : регистриран, false - отказ
	 */
	private void createReturnMail(boolean registred) {
		Mailer mm = new Mailer();
		try {
			String txtJ = "";
			String subject = "FW:"+ (messSubject!=null?messSubject:"") ;
			StringBuilder body = new StringBuilder();
			String emailTo = messFromRef;
			boolean b1 = false;
			if (document.getReceivedBy() != null && !document.getReceivedBy().trim().isEmpty() && !Objects.equals(document.getReceivedBy(), messFromRef)){
				emailTo = document.getReceivedBy().trim();
				b1 = true;
			}
			String bodyPlainText = mailBodyTextPlain(b1);
			if(registred) {
				body.append(bodyPlainText);
				txtJ = " Изпратен е е-мейл за регистрация до подателя ("+emailTo+").";
			}else {
				body.append(" " +getSystemData().decodeItemDopInfo(OmbConstants.CODE_CLASSIF_REGISTRATURI,ud.getRegistratura(), ud.getCurrentLang(), new Date())); // име на организация
				body.append(" \n  Отказ от регистрация ");
				body.append(" \n  Дата на отказ: " + DateUtils.printDate(new Date()));
				body.append(" \n  Причина за отказ: " + textReject);
				txtJ = " Изпратен е е-мейл за отказ от регистрaция до подателя ("+emailTo+").";
			}
			if(b1) {
				// e-mail е сменен ръчно....!!
				subject = "Re:"+ (messSubject!=null?messSubject:"") ;
				Properties props = sd.getMailProp(-1, "DEFAULT"); // за да вземе  от настройките на системата
				
				String user = props.getProperty("user.name");
	            String pass = props.getProperty("user.password");
	            String from = props.getProperty("mail.from","noreply@delovodstvo.com");
				
	            mm.sent(Content.HTML, props, user, pass, from, "Деловодство", emailTo, subject, body.toString(), null);
			} else {
				mm.forward(this.propMail, this.messUID, this.propMail.getProperty("mail.folder.read", getSystemData().getSettingsValue("mail.folder.read")), subject, body.toString());
			}
					
			if(registred) {
				mm.moveMailUIDRegistred(this.propMail, this.messUID, sd);
			}else {
				mm.moveMailUIDRejected(this.propMail, this.messUID, sd);	
			}
			
			// запис в журнала, че e изпратен е-мейл за регистрация
			SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, this.document.getId(), this.getRnFullDocEdit()+txtJ);
			journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
			journal.setDateAction(new Date());
			journal.setIdUser(getUserData().getUserId());
			JPA.getUtil().runInTransaction(() -> this.dao.saveAudit(journal));	
						
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "jp.sentMailMsg") );	// изпратен, е но ако мейла на подателя е грешен - няма как да сме сигурни,че е успешно....
			
		} catch (BaseException | MessagingException e) {
			LOGGER.error("Грешка при формиране на е-мейл и връщане на е-мейл към подателя! ", e); 
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, OBJECTINUSE), e.getMessage());
		} 
	}
	
	/**
	 * Формира текста на съобщението, което се връща по е-майл или чрез еВръчване (ССЕВ), при успешно регистриран документ
	 * @return
	 * @throws DbErrorException 
	 */
	public String mailBodyTextPlain(boolean typeHtml) throws DbErrorException {
		StringBuilder tmp= new StringBuilder();
		String orgName = sd.decodeItemDopInfo(OmbConstants.CODE_CLASSIF_REGISTRATURI, document.getRegistraturaId(), SysConstants.CODE_DEFAULT_LANG, new Date());
		String regN = this.document.getRnDoc() + "/" + DateUtils.printDate(this.document.getDocDate());
		String param = sd.getSettingsValue("emailReplayMsg");
		if(SearchUtils.isEmpty(param)) {
			// формира текст по подразбиране, ако липсва настройка
			tmp.append(orgName);
			tmp.append("\n  Вашият документ е регистриран успешно! ");
			tmp.append("\n  Уникален регистрационен № на документа/материала  ");
			tmp.append(regN);
		}else {
			if (!typeHtml) {
				param = param.replace("<br/>","\n"); // ако това го има - се губят новите редове, ако е Content type == Content.HTML
			}
			param = param.replace("$regnom$", regN+" ");
			param = param.replace("$admSlujba$", orgName +" ");
			if(docVidInclude == INCL_COMPLAINT) { 
				int l = document.getJalba().getJalbaText().length();
				int l1 = l > 200 ? 200 : l;
				param = param.replace("$anot$", document.getJalba().getJalbaText().substring(0, l1)+"..."); // за жалбите, вместо относно да се сложи част от описанието от жалбоподателя - той е задължителен за въвеждане
			} else {
				param = param.replace("$anot$", document.getOtnosno()+" ");
			}
			String tmplat = "";
			if(param.contains("$transliterate$")) {
				param = param.replace("$transliterate$", "");
				tmplat = com.ib.system.utils.StringUtils.transliterate(param);			
			}
			tmp.append(param);
			tmp.append(tmplat);
			
//		<br/>Вашият документ е регистриран успешно!<br/> $admSlujba$<br/> Уникален регистрационен № на документа/материала $regnom$<br/> Относно: $anot$ <br/><br/> $transliterate$
		}		
		return tmp.toString();
	}
	
	
	
	

	/**
	 * Отказ от регистрация - рег. на док. от СЕОС или ССЕВ
	 */
	private boolean rejectFromEGOV() {
		boolean rejectOk = false;
		if( SearchUtils.isEmpty(textReject)) {
			JSFUtils.addMessage(DOCFORMTABS+":txtReject",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, TXTREJECTED)));
		} else {			
			try {
				JPA.getUtil().runInTransaction(() -> 
					//  да се направи update на съобщението в table EGOV_MESSAGES
					// Да се върне съобщение за отказ
					createReturnEGOV(false)			
				);
			} catch (BaseException e) {
				LOGGER.error("Грешка при формиране на отказ от регистрация - СЕОС или ССЕВ! ", e); 
			}
			rejectOk = true;
		}	
		return rejectOk;
	}
	

	/**
	 * update на статуса на съобщението в table EGOV_MESSAGES
	 * Формира и изпраща съобщение през СЕОС за отговор 
	 * @param registred - true : регистриран, false - отказ
	 */
	public void createReturnEGOV(boolean registred) {
		try {
			if(registred) {
				egovMess.setMsgRn(document.getRnDoc());
				egovMess.setMsgRnDate(document.getDocDate());
				egovMess.setMsgStatus(EgovStatusType.DS_REGISTERED.toString());		
				
			}else {			
				egovMess.setMsgRn(null);
				egovMess.setMsgRnDate(null);
				egovMess.setMsgStatus(EgovStatusType.DS_REJECTED.toString());
				egovMess.setPrichina(textReject);
			}
			egovMess.setMsgStatusDate(new Date());
			EgovMessagesDAO daoEgov = new EgovMessagesDAO(getUserData());
			// запис на промяната на статуса в таблица Egov_Messages
			egovMess = daoEgov.save(egovMess);		
			
			// Отговор 
			if(Objects.equals(S_SEOS, egovMess.getSource())) {
				createReturnMsgEGOV(registred, daoEgov);
			}else if(Objects.equals("S_EDELIVERY", egovMess.getSource())){ 
				createReturnMsgЕDelivery(daoEgov);
			}
		
		} catch (DbErrorException  e) {
			LOGGER.error("Грешка при формиране на отговор - egovMsg! ", e); 
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, OBJECTINUSE), e.getMessage());
		} 
	}
	
	/**
	 * формира съобщение отговор -  за СЕОС
	 * @param registred - true : регистриран, false - отказ
	 * @throws DbErrorException 
	 */
	private void createReturnMsgEGOV(boolean registred, EgovMessagesDAO daoEgov) throws DbErrorException {
		if(registred) { 
			daoEgov.saveStatusResponseRegisteredMessage(egovMess, document.getRnDoc(), document.getDocDate(), ud);			
		}else {
			daoEgov.saveStatusResponseOtkazMessage(egovMess, textReject, ud);			
		}
	}
	

	/**
	 * формира съобщение отговор за успешна регистрация -  за еDelivery (ССЕВ)
	 * @throws DbErrorException 
	 */
	private void createReturnMsgЕDelivery(EgovMessagesDAO daoEgov) throws DbErrorException {
		String bodyTextPlain = mailBodyTextPlain(false);
		String subject = getMessageResourceString(beanMessages, "docu.confSucRegDoc"); 		
		daoEgov.saveDeliverySuccessMess(egovMess, document.getRnDoc(), document.getDocDate(), document.getDocVid(), bodyTextPlain, subject, sd, ud);
	}
					
		
	
   /**
    * При смяна на таб
    */

    public void onTabChange(TabChangeEvent<?> event) {
	   	if(event != null) {
	   		if (LOGGER.isDebugEnabled()) {
	   			LOGGER.debug("onTabChange Active Tab: {}", event.getTab().getId());
	   		}
			rnFullDoc = DocDAO.formRnDocDate(this.document.getRnDoc(), this.document.getDocDate(), this.document.getPoredDelo());
			String activeTab =  event.getTab().getId();
			if(activeTab.equals("tabTasks")) {
				getSrokPattern();
				// списък задачи към документ
				TaskSearch tmpTs = new TaskSearch(document.getRegistraturaId()); 
				tmpTs.setDocId(document.getId());			
				tmpTs.buildQueryTasksInDoc();
				setTasksList(new LazyDataModelSQL2Array(tmpTs, "a0 asc"));
				
			} else if (activeTab.equals("tabMain")) {				
				if(Objects.equals(document.getDocType(), OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) &&  document.getForRegId() != null ) {
					readyForOfficial = true;
				}
				docProcessed(Objects.equals(document.getProcessed(), OmbConstants.CODE_ZNACHENIE_DA)); // ако се върне от задача или движения  и е променено
				docCompetence();
			} else if (activeTab.equals("tabDvig")) {
				countOfficialFiles();								
			}else if (activeTab.equals("tabPrep") && (docVidInclude == INCL_COMPLAINT) ) {
				
				//когато статуса на жалбата влияе на статуса на преписката, DeloStatusChanged е true
				DocDataComplaint beanJ = (DocDataComplaint) JSFUtils.getManagedBean("docDataComplaint"); 
				//когато статуса на нпм/самосезиране влияе на статуса на преписката, DeloStatusChanged е true
				DocDataSpec beanS = (DocDataSpec) JSFUtils.getManagedBean("docDataSpec"); 
				if(beanJ != null && Objects.equals(beanJ.getDocument().getJalba().getDeloStatusChanged(), Boolean.TRUE )) {
					DocDataPrep beanP = (DocDataPrep) JSFUtils.getManagedBean("docDataPrep"); 
					if(beanP != null) {
						beanP.setRnFullDoc(null);// за да презреди всичко в таба за преписки...
						beanJ.getDocument().getJalba().setDeloStatusChanged(null);
					}
				} else if(beanS != null && Objects.equals(beanS.getDocument().getDocSpec().getDeloStatusChanged(), Boolean.TRUE ))	 {
					DocDataPrep beanP = (DocDataPrep) JSFUtils.getManagedBean("docDataPrep"); 
					if(beanP != null) {
						beanP.setRnFullDoc(null);// за да презреди всичко в таба за преписки...
						beanS.getDocument().getJalba().setDeloStatusChanged(null);
					}
				}
						
			}
			
			if(isView == 1) {
	   			viewMode();
	   		} else	if(!activeTab.equals("tabTasks") && idTask != null) {// && tasksList != null && tasksList.getRowCount() > 0) {
				idTask = null;
				unlockAll(false);//Да отключа задачите към документа 
			}
			
	   	}
   }
	
    /**
     * брой файлове, маркирани като официални
     */
   private void countOfficialFiles() {
	   countOfficalFiles = 0;
	   if ( filesList != null && !filesList.isEmpty()) {
			// да преброя колко са официални за изпращане
			for (Files f : filesList) {
				if(Objects.equals(f.getOfficial(), OmbConstants.CODE_ZNACHENIE_DA)) {
					countOfficalFiles  ++;
				}
			}
	   }
   }
    
   /**
    * Да се виждат ли часове и минути в срока на задачите 
    */
   private void loadSrokPattern() {
	   	// да се виждат ли часове и минути в срока на задачата
		// взема се настройкaт на регистратурата на потребителя - за сега, в списъка,  ще се определя само от текущата регистртура!
	   
		try {
			Integer s1 = ((SystemData) getSystemData()).getRegistraturaSetting(document.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_15);
			if(Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_DA)) {
				setSrokPattern("dd.MM.yyyy HH:mm");
			}else {
				setSrokPattern("dd.MM.yyyy");
			}
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при извичане на настройка CODE_ZNACHENIE_REISTRATURA_SETTINGS_15 на регистратура: {} ", document.getRegistraturaId()+" ! ", e);
		}
	  
   }
   
   /**
    * Режим - разглеждане на документ
    * @param activeTab
    */
   private void viewMode() {

	    String  cmdStr;  

		// 1. забранявам всички инпутполета
		cmdStr = "$(':input').attr('readonly','readonly')";
		PrimeFaces.current().executeScript(cmdStr);
		
   }
   
	/**
	 * подскзака в списъка със задачи - мнение при приключване + коментар
	 * @param comment
	 * @param opinion
	 * @return
	 */
	public String titleInfoTask(Integer opinion, String comment ) {
		StringBuilder title = new StringBuilder();
		if(opinion != null) {
			try {
				String opinionTxt=getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_TASK_OPINION, opinion, getUserData().getCurrentLang(), new Date());
				title.append(getMessageResourceString(LABELS, "docu.modalRefMnenie")+": " + opinionTxt+ "; ");
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане на данни за документ! ", e);
			}
		}
		if(!SearchUtils.isEmpty(comment)) {
			title.append(comment); //getMessageResourceString(LABELS, "tasks.comment")+": "
		}
		return title.toString();
	}
	
	
	public Doc getDocument() {		
		return document;
	}

	public void setDocument(Doc document) {
		this.document = document;
	}

	/** @return the docTypeList */
	public List<SelectItem> getDocTypeList() {
		return this.docTypeList;
	}

	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}

	public boolean isAvtomNo() {
		return avtomNo;
	}

	public void setAvtomNo(boolean avtomNo) {
		this.avtomNo = avtomNo;
	}


	public boolean isCreatePrep() {
		return createPrep;
	}

	public void setCreatePrep(boolean createPrep) {
		this.createPrep = createPrep;
	}


	public boolean isProcessedCh() {
		return processedCh;
	}

	public void setProcessedCh(boolean processedCh) {
		this.processedCh = processedCh;
	}

	

	
	public DocDAO getDao() {
		return dao;
	}

	public void setDao(DocDAO dao) {
		this.dao = dao;
	}
	

	public List<Files> getFilesList() {
		return filesList;
	}


	public void setFilesList(List<Files> filesList) {
		this.filesList = filesList;
	}




	public String getTxtCorresp() {
		return txtCorresp;
	}


	public void setTxtCorresp(String txtCorresp) {
		this.txtCorresp = txtCorresp;
	}


	public boolean isAvtomNoDisabled() {
		return avtomNoDisabled;
	}


	public void setAvtomNoDisabled(boolean avtomNoDisabled) {
		this.avtomNoDisabled = avtomNoDisabled;
	}


	public Map<Integer, Object> getSpecificsRegister() {
		return specificsRegister;
	}


	public void setSpecificsRegister(Map<Integer, Object> specificsRegister) {
		this.specificsRegister = specificsRegister;
	}


	public boolean isRegUserFiltered() {
		return regUserFiltered;
	}


	public void setRegUserFiltered(boolean regUserFiltered) {
		this.regUserFiltered = regUserFiltered;
	}


	public Object[] getSelectedDelo() {
		return selectedDelo;
	}


	public void setSelectedDelo(Object[] selectedDelo) {
		this.selectedDelo = selectedDelo;
	}


	public String getRnFullDoc() {
		return rnFullDoc;
	}


	public void setRnFullDoc(String rnFullDoc) {
		this.rnFullDoc = rnFullDoc;
	}

	
	public DeloDoc getDeloDocPrep() {
		return deloDocPrep;
	}


	public void setDeloDocPrep(DeloDoc deloDocPrep) {
		this.deloDocPrep = deloDocPrep;
	}




	public LazyDataModelSQL2Array getTasksList() {
		return tasksList;
	}


	public void setTasksList(LazyDataModelSQL2Array tasksList) {
		this.tasksList = tasksList;
	}


	public Integer getIdTask() {
		return idTask;
	}


	public void setIdTask(Integer idTask) {
		this.idTask = idTask;
	}


	public SystemData getSd() {
		return sd;
	}


	public void setSd(SystemData sd) {
		this.sd = sd;
	}


	public boolean isReadyForOfficial() {
		return readyForOfficial;
	}


	public void setReadyForOfficial(boolean readyForOfficial) {
		this.readyForOfficial = readyForOfficial;
	}

	
	public List<SelectItem> getDopRegistraturiList() {
		return dopRegistraturiList;
	}


	public void setDopRegistraturiList(List<SelectItem> dopRegistraturiList) {
		this.dopRegistraturiList = dopRegistraturiList;
	}



	public String getRnFullDocOther() {
		return rnFullDocOther;
	}

		
	public void setRnFullDocOther(String rnFullDocOther) {
		this.rnFullDocOther = rnFullDocOther;
	}


	public int getFlagFW() {
		return flagFW;
	}


	public void setFlagFW(int flagFW) {
		this.flagFW = flagFW;
	}


	public int getIsView() {
		return isView;
	}


	public void setIsView(int isView) {
		this.isView = isView;
	}


	public UserData getUd() {
		return ud;
	}


	public void setUd(UserData ud) {
		this.ud = ud;
	}


	public int getViewBtnProcessed() {
		return viewBtnProcessed;
	}


	public void setViewBtnProcessed(int viewBtnProcessed) {
		this.viewBtnProcessed = viewBtnProcessed;
	}

	public Map<Integer, Object> getSpecificsAdm() {
		if(specificsAdm == null) {
			Object[][] obj = {{OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE, X.of(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL)},};
			specificsAdm = Stream.of(obj).collect(Collectors.toMap(data -> (Integer) data[0], data ->  data[1]));  // X.of() -> така ще дава само служители през аутокомплете, а в дървото ще е цялата
		}
		return specificsAdm;
	}

	public void setSpecificsAdm(Map<Integer, Object> specificsAdm) {
		this.specificsAdm = specificsAdm;
	}
	
	public List<SystemClassif> getRezolExecClassif() {
		return rezolExecClassif;
	}


	public void setRezolExecClassif(List<SystemClassif> rezolExecClassif) {
		this.rezolExecClassif = rezolExecClassif;
	}



	public Task getRezolTask() {
		return rezolTask;
	}


	public void setRezolTask(Task rezolTask) {
		this.rezolTask = rezolTask;
	}


	public List<Object[]> getSelectedDocsTn() {
		return selectedDocsTn;
	}


	public void setSelectedDocsTn(List<Object[]> selectedDocsTn) {
		this.selectedDocsTn = selectedDocsTn;
	}





	public String getNoteAccess() {
		return noteAccess;
	}


	public void setNoteAccess(String noteAccess) {
		this.noteAccess = noteAccess;
	}


	public int getSearchFlagTn() {
		return searchFlagTn;
	}


	public void setSearchFlagTn(int searchFlagTn) {
		this.searchFlagTn = searchFlagTn;
	}


	public int getTnRez() {
		return tnRez;
	}


	public void setTnRez(int tnRez) {
		this.tnRez = tnRez;
	}


	public boolean isLimitedAccessCh() {
		return limitedAccessCh;
	}


	public void setLimitedAccessCh(boolean limitedAccessCh) {
		this.limitedAccessCh = limitedAccessCh;
	}


	public boolean isNastrWithEkz() {
		return nastrWithEkz;
	}


	public void setNastrWithEkz(boolean nastrWithEkz) {
		this.nastrWithEkz = nastrWithEkz;
	}

	public Integer getSourceRegDvijId() {
		return sourceRegDvijId;
	}


	public void setSourceRegDvijId(Integer sourceRegDvijId) {
		this.sourceRegDvijId = sourceRegDvijId;
	}


	public String getTextReject() {
		return textReject;
	}


	public void setTextReject(String textReject) {
		this.textReject = textReject;
	}


	public Properties getPropMail() {
		return propMail;
	}


	public void setPropMail(Properties propMail) {
		this.propMail = propMail;
	}


	public Long getMessUID() {
		return messUID;
	}


	public void setMessUID(Long messUID) {
		this.messUID = messUID;
	}


	public String getMessSubject() {
		return messSubject;
	}


	public void setMessSubject(String messSubject) {
		this.messSubject = messSubject;
	}


	public String getSelectMailBox() {
		return selectMailBox;
	}


	public void setSelectMailBox(String selectMailBox) {
		this.selectMailBox = selectMailBox;
	}


	public String getMessFromRef() {
		return messFromRef;
	}


	public void setMessFromRef(String messFromRef) {
		this.messFromRef = messFromRef;
	}


	public Date getCurrentDate() {
		return new Date();
	}


	public String getRnFullProtocol() {
		return rnFullProtocol;
	}


	public void setRnFullProtocol(String rnFullProtocol) {
		this.rnFullProtocol = rnFullProtocol;
	}


	public int getCountOfficalFiles() {
		return countOfficalFiles;
	}


	public void setCountOfficalFiles(int countOfficalFiles) {
		this.countOfficalFiles = countOfficalFiles;
	}


	public boolean isFromOtherReg() {
		return fromOtherReg;
	}


	public void setFromOtherReg(boolean fromOtherReg) {
		this.fromOtherReg = fromOtherReg;
	}

	public boolean isCompetence() {
		return competence;
	}

	public void setCompetence(boolean competence) {
		this.competence = competence;
	}

	
	public int getUpdateDoc() {
		return UPDATE_DOC; 
	}
	
	
	public  int getRegOtherR() {
		return  REG_OTHER_R; 
	}


	public  int getRegFromMail() {
		return REG_FROM_MAIL; 
	}
	
	public  int getRegFromMailJalba() {
		return REG_FROM_MAIL_JALBA; 
	}
	
	
	public  int getRegFromEgov() {
		return REG_FROM_EGOV; 
	}
	
	public void reloadDocDataFile() {
		
		if(document!=null && document.getId()!=null) {
			try {
				JPA.getUtil().runWithClose(() -> {
					if(this.dao.hasDocAccess(document, regUserFiltered, getSystemData())) {	//проверка за достъп до документа
						loadFilesList(document.getId(), UPDATE_DOC); // 	load files
					} 
				});
			} catch (BaseException e) {
				LOGGER.error("Грешка при зареждане на файлове след подписване! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}
		}
		
	}

	public boolean isCreatePrepOld() {
		return createPrepOld;
	}

	public void setCreatePrepOld(boolean createPrepOld) {
		this.createPrepOld = createPrepOld;
	}

	public boolean isScanModuleExist() {
		return scanModuleExist;
	}

	public void setScanModuleExist(boolean scanModuleExist) {
		this.scanModuleExist = scanModuleExist;
	}

	public EgovMessages getEgovMess() {
		return egovMess;
	}

	public void setEgovMess(EgovMessages egovMess) {
		this.egovMess = egovMess;
	}

	public List<SystemClassif> getScTopicList() {
		return scTopicList;
	}

	public void setScTopicList(List<SystemClassif> scTopicList) {
		this.scTopicList = scTopicList;
	}
	
	/**
	 * Разглеждане на документ
	 * При отваряне в нов таб, като title на таба, да излезе номера и дата на документа
	 * @return
	 */
	public String getRnFullViewDoc() {
		if (document != null) {
			return DocDAO.formRnDocDate(this.document.getRnDoc(), this.document.getDocDate(), this.document.getPoredDelo());
		} else {
			return "Документ";
		}
	}

	public LazyDataModelSQL2Array getDocAccessList() {
		return docAccessList;
	}

	public void setDocAccessList(LazyDataModelSQL2Array docAccessList) {
		this.docAccessList = docAccessList;
	}

	public String getSrokPattern() {
		if(srokPattern == null && document != null) {
			loadSrokPattern();
		}
		return srokPattern;
	}

	public void setSrokPattern(String srokPattern) {
		this.srokPattern = srokPattern;
	}

	public boolean isShowCoresp() {
		return showCoresp;
	}

	public void setShowCoresp(boolean showCoresp) {
		this.showCoresp = showCoresp;
	}

	public boolean isEditReferentsWRK() {
		return editReferentsWRK;
	}

	public void setEditReferentsWRK(boolean editReferentsWRK) {
		this.editReferentsWRK = editReferentsWRK;
	}

	public Integer getDocSettingId() {
		return docSettingId;
	}

	public void setDocSettingId(Integer docSettingId) {
		this.docSettingId = docSettingId;
	}

	public List<Files> getTemplatesList() {
		return templatesList;
	}

	public void setTemplatesList(List<Files> templatesList) {
		this.templatesList = templatesList;
	}

	public Object[] getCodeExtCheck() {
		return codeExtCheck;
	}

	public void setCodeExtCheck(Object[] codeExtCheck) {
		this.codeExtCheck = codeExtCheck;
	}

	public List<SystemClassif> getClassifProceduri() {
		return classifProceduri;
	}

	public void setClassifProceduri(List<SystemClassif> classifProceduri) {
		this.classifProceduri = classifProceduri;
	}

	public Map<Integer, Object> getSpecificsProc() {
		return specificsProc;
	}

	public void setSpecificsProc(Map<Integer, Object> specificsProc) {
		this.specificsProc = specificsProc;
	}

	public boolean isEnableProc() {
		return enableProc;
	}

	public void setEnableProc(boolean enableProc) {
		this.enableProc = enableProc;
	}

	public boolean isNotFinishedProc() {
		return notFinishedProc;
	}

	public void setNotFinishedProc(boolean notFinishedProc) {
		this.notFinishedProc = notFinishedProc;
	}
	
//	/**
//	 * брой на редове в полето относно 
//	 * @return
//	 */
//	public int rowsOtnosno() {
//		int rows = Objects.equals(document.getDocType(),OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN) ? 16 : 12;
//		boolean bb = isView == 1 ? document.getProcDef() == null :  document.getProcDef() == null && (classifProceduri == null || classifProceduri.isEmpty());
//		if(bb ) {
//			rows = rows-3;
//		}
//		return rows;
//	}
	

	/* 
	 * Известие за доставка Обратна разписка /кореспонденти шаблон 04.2018  - ivanc
	*/
		/*public void izvestieDostav()  {
			
					
			try{
				
				// 1. Зарежда лиценза за работа с MS Word documents.
				
				License license = new License();
				String nameLic="Aspose.Words.lic";
				
				InputStream inp = getClass().getClassLoader().getResourceAsStream(nameLic);
				license.setLicense(inp);
				
				// 2. Чете файл-шаблон от БД

				Files fileShabl = new FilesDAO(getUserData()).findById(Integer.valueOf(-111));

				// 3. Създава празен MS Word документ от шаблона 
				
				Document docEmptyShablon = new Document(new ByteArrayInputStream(fileShabl.getContent()));

				
				// 4. Създава попълнен документ от шаблона
				Document docFilledShablon = null;
	
				docFilledShablon = new FillDocShablon().fillDocShabl243 (document, docEmptyShablon, sd, ud);
			
				ByteArrayOutputStream dstStream = new ByteArrayOutputStream();
				docFilledShablon.save(dstStream, SaveFormat.DOCX);
				byte [] bytearray = null;
				bytearray = dstStream.toByteArray();
				// 5. Създава файла от създадения MS Word документ и го показва
				if (bytearray !=null){ 
					String fileName = "Izvestie_Dostavka";
					
					SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
					fileName =  fileName.split("\\.")[0] + "_" + sdf.format(new Date())+".docx";
				
					//	Показва попълнения шаблон		
					FacesContext ctx = FacesContext.getCurrentInstance();
					HttpServletResponse response = (HttpServletResponse) ctx.getExternalContext().getResponse();
					HttpServletRequest request = (HttpServletRequest) ctx.getExternalContext().getRequest();
		
					String agent = request.getHeader("USER-AGENT");
					if (null != agent && -1 != agent.indexOf("MSIE")) {
						String codedfilename = URLEncoder.encode(fileName,
								"UTF8");
						response.setContentType("application/x-download");
						response.setHeader("Content-Disposition",
								"attachment;filename=" + codedfilename);
					} else if (null != agent && -1 != agent.indexOf("Mozilla")) {
						String codedfilename = MimeUtility.encodeText(fileName,
								"UTF8", "B");
						response.setContentType("application/x-download");
						response.setHeader("Content-Disposition",
								"attachment;filename=" + codedfilename);
					} else {
						response.setContentType("application/x-download");
						response.setHeader("Content-Disposition",
								"attachment;filename=" + fileName);
					}
		
					ServletOutputStream out = null;
					out = response.getOutputStream();
					if (bytearray != null)
						out.write(bytearray);
		
					out.flush();
					out.close();
		
					ctx.responseComplete();
				}
		
			 } catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			 } catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,"Грешка при четене на файл шаблон!", e.getMessage());
			}
			
		}*/
		
	/**
	 * За журнала! 
	 * @return
	 */
	public String getRnFullDocAudit() {
		//Не изпозлвам rnFullDoc, защото може да се обърка отварянето на табовете....
		String rnAudit = null;
		if(document != null && document.getId() != null) {
			rnAudit = "Документ: "+DocDAO.formRnDocDate(this.document.getRnDoc(), this.document.getDocDate(), this.document.getPoredDelo());
		}
		return rnAudit;
	}

	public Integer getDocSettingHar() {
		return docSettingHar;
	}

	public void setDocSettingHar(Integer docSettingHar) {
		this.docSettingHar = docSettingHar;
	}


	public String getMembersTab() {
		return membersTab;
	}

	public void setMembersTab(String membersTab) {
		this.membersTab = membersTab;
	}


	public boolean isMembersTabForDel() {
		return membersTabForDel;
	}

	public void setMembersTabForDel(boolean membersTabForDel) {
		this.membersTabForDel = membersTabForDel;
	}

	public String getRnFullDocSign() {
		
		if(document.getRnDoc()!=null && document.getDocDate()!=null) {
			rnFullDocSign = document.getRnDoc() + "/" + DateUtils.printDate(document.getDocDate());
		} else {
			rnFullDocSign = "-1";
		}
		return rnFullDocSign;
	}

	public void setRnFullDocSign(String rnFullDocSign) {
		this.rnFullDocSign = rnFullDocSign;
	}

	public String getDopInfoAdres() {
		if(this.dopInfoAdres == null) {
			loadDopInfoAdres();
		}
		return dopInfoAdres;
	}

	public void setDopInfoAdres(String dopInfoAdres) {
		this.dopInfoAdres = dopInfoAdres;
	}

	/**
	 * да зачисти информацията за полето адрес на кореспонднет
	 */
	public void clearInfoAdres() {
		if(isView != 1) {
			this.dopInfoAdres = null;
		}
	}
	
	/**
	 * зарежда адреса на кореспондента
	 */
	public void loadDopInfoAdres() {
		if(document.getCodeRefCorresp() != null) {
			// ако нямам права да виждам лини данни
			// заради достъпа до личните данни - в допълнителната информаиця за физическите лица да остане само населеното място!!
			try {				
				this.dopInfoAdres = sd.decodeItemDopInfo(OmbConstants.CODE_CLASSIF_REFERENTS, document.getCodeRefCorresp(), getCurrentLang(), new Date());
				if(this.dopInfoAdres != null &&
					(int) sd.getItemSpecific(OmbConstants.CODE_CLASSIF_REFERENTS, document.getCodeRefCorresp() ,  getCurrentLang(), new Date(), OmbClassifAdapter.REFERENTS_INDEX_REF_TYPE) == OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL) {
				
					if(!getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_SEE_PERSONAL_DATA) ) {
						// да остане само град или село  
						int i1 = this.dopInfoAdres.indexOf("гр.");
						if(i1 == -1) {
							i1 = this.dopInfoAdres.indexOf("с.");
						}
						if(i1 != -1) {						
							int i2 = this.dopInfoAdres.indexOf(", ", i1);
							if(i2 != -1) {
								this.dopInfoAdres = this.dopInfoAdres.substring(i1, i2);
							}else {
								// има само град или село...
								this.dopInfoAdres = this.dopInfoAdres.substring(i1);
							}
						}else {
							this.dopInfoAdres = null;
						}
					}else { // да махна ЕГН, за да остане само адреса
						int i1 = this.dopInfoAdres.indexOf("ЕГН");
						if(i1 != -1) {	
							//има егн
							int i2 = this.dopInfoAdres.indexOf(", ", i1);
							if(i2 != -1) {
								this.dopInfoAdres = this.dopInfoAdres.substring(i2+1);
							}else {
								this.dopInfoAdres = null; // има само егн...
							}
						}
					}
				}			
			} catch (Exception e) {
				LOGGER.error("Грешка при формиране на адрес на кореспонднета за показване в документа! ", e);
			}
			
		}else {
			this.dopInfoAdres = null; 
		}
	}

	public Integer getShowSignMethod() {
		return showSignMethod;
	}

	public void setShowSignMethod(Integer showSignMethod) {
		this.showSignMethod = showSignMethod;
	}

	public String getRnFullDocEdit() {
		return rnFullDocEdit;
	}

	public void setRnFullDocEdit(String rnFullDocEdit) {
		this.rnFullDocEdit = rnFullDocEdit;
	}
	
	
	
	
//***** Жлаби, НПМ, Самосезиране ***
	

	public Integer getDocVidInclude() {
		return docVidInclude;
	}

	public void setDocVidInclude(Integer docVidInclude) {
		this.docVidInclude = docVidInclude;
	}
	
	public Integer getInclRegular() {
		return INCL_REGULAR;
	}

	public Integer getInclComplaint() {
		return INCL_COMPLAINT;
	}
	
	public Integer getInclNpm() {
		return INCL_NPM;
	}
	
	public Integer getInclSamosez() {
		return INCL_SAMOSEZ;
	}

	/**
	 * определя от къде се вика - от регистър жалби, от нов док., от e-mail! 
	 */
	private void isSpecialDoc(){		
		String param = null;
		
		if ( flagFW == REG_FROM_MAIL_JALBA  || flagFW == REG_FROM_EGOV_JALBA) {
			docVidInclude = INCL_COMPLAINT;
		}else {
			FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
			param = (String) faceletContext.getAttribute("isSpec"); //  1 - жалба ; 2 нпм; 3 - самосезиране			
			docVidInclude = !SearchUtils.isEmpty(param) ? Integer.valueOf(param) : INCL_REGULAR;
		}		
		//ако има стойност - това е нов работен в преписката на жалбата/нпм/самосезиране
		specDocId = null;
		specRnFullDoc = null;
		specVidDocList = null;
		param = JSFUtils.getRequestParameter("idJ"); 
		if(!SearchUtils.isEmpty(param)) {
			String param2 = JSFUtils.getRequestParameter("v"); 
			if(!SearchUtils.isEmpty(param2)){
				Integer vid = Integer.valueOf(param2);
			    if(vid.equals(INCL_COMPLAINT)) {
			    	loadDataWrkDocSpec(param, OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA, OmbConstants.CODE_CLASSIF_JALBA_DOCS); // жалба
			    } else if(vid.equals(INCL_NPM)){
			    	loadDataWrkDocSpec(param, OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM, OmbConstants.CODE_CLASSIF_NPM_DOCS);  // нпм
			    } else if(vid.equals(INCL_SAMOSEZ)){
			    	loadDataWrkDocSpec(param, OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS, OmbConstants.CODE_CLASSIF_SAMOS_DOCS);  // самосезиране
			    }
			}
		}
	}

	
	private void loadDataWrkDocSpec(String param, int svDoc, int classif) {
		specDocId = Integer.valueOf(param);
		specVidDoc = svDoc;
		codeClassifVidDoc = classif;
		try {
			// стандартно зареждане - без да отчита права на потребителя
			setSpecVidDocList(getSystemData().getSysClassification(classif, new Date(), getCurrentLang()));
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на допустими видове документи, когато се рег. раб. от жалба/нпм/самосезиране ! ", e);
		}
		
		String paramRn = JSFUtils.getRequestParameter("jStr");
		if(!SearchUtils.isEmpty(paramRn)) {
			specRnFullDoc = "към преп. на "+ paramRn ;
			int i = paramRn.indexOf("#");
			specRnDoc = "Р-"+paramRn.substring(0,i);		//работен от жалба, нпм, самосезиране - номера на работния да бъде - "Р"+номера на спец. док .	
		}	
	}
	
	
	 
	 /**
	  * download на файлове от Справка - жалба, нпм, самосезиране
	  * @param files
	  */
	public void download(Integer idFile) { 
		try {
			Files file = null;
			if (idFile != null){
		
				FilesDAO dao = new FilesDAO(getUserData());	
				try {
					file = dao.findById(idFile);	
				} finally {
					JPA.getUtil().closeConnection();
				}
				
				if(file.getContent() == null){					
					file.setContent(new byte[0]);
				}
			} else {
				return; // няма как да иа файл
			}

			FacesContext facesContext = FacesContext.getCurrentInstance();
			ExternalContext externalContext = facesContext.getExternalContext();

			HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
			String agent = request.getHeader("user-agent");

			String codedfilename = "";
			if (null != agent && (-1 != agent.indexOf("MSIE") || -1 != agent.indexOf("Mozilla") && -1 != agent.indexOf("rv:11") || -1 != agent.indexOf("Edge"))) {
				codedfilename = URLEncoder.encode(file.getFilename(), "UTF8");
			} else if (null != agent && -1 != agent.indexOf("Mozilla")) {
				codedfilename = MimeUtility.encodeText(file.getFilename(), "UTF8", "B");
			} else {
				codedfilename = URLEncoder.encode(file.getFilename(), "UTF8");
			}

			externalContext.setResponseHeader("Content-Type", "application/x-download");
			externalContext.setResponseHeader("Content-Length", file.getContent().length + "");
			externalContext.setResponseHeader("Content-Disposition", "attachment;filename=\"" + codedfilename + "\"");
			externalContext.getResponseOutputStream().write(file.getContent());

			facesContext.responseComplete();

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
	

	
	
	
	public Integer getSpecDocId() {
		return specDocId;
	}


	public void setSpecDocId(Integer specDocId) {
		this.specDocId = specDocId;
	}


	public Integer getCodeClassifVidDoc() {
		if(codeClassifVidDoc == null) {
			codeClassifVidDoc = OmbConstants.CODE_CLASSIF_DOC_VID;
		}
		return codeClassifVidDoc;
	}


	public void setCodeClassifVidDoc(Integer codeClassifVidDoc) {
		this.codeClassifVidDoc = codeClassifVidDoc;
	}


	public String getSpecRnFullDoc() {
		return specRnFullDoc;
	}


	public void setSpecRnFullDoc(String specRnFullDoc) {
		this.specRnFullDoc = specRnFullDoc;
	}

	public Integer getSpecVidDoc() {
		return specVidDoc;
	}


	public void setSpecVidDoc(Integer specVidDoc) {
		this.specVidDoc = specVidDoc;
	}


	public List<SystemClassif> getSpecVidDocList() {
		return specVidDocList;
	}


	public void setSpecVidDocList(List<SystemClassif> specVidDocList) {
		this.specVidDocList = specVidDocList;
	}


	public Integer getWorkDocId() {
		return workDocId;
	}


	public void setWorkDocId(Integer workDocId) {
		this.workDocId = workDocId;
	}


	public boolean isViewChboxCrPrep() {
		return viewChboxCrPrep;
	}


	public void setViewChboxCrPrep(boolean viewChboxCrPrep) {
		this.viewChboxCrPrep = viewChboxCrPrep;
	}


	public List<EgovMessagesCoresp> getEgovMessCoresp() {
		return egovMessCoresp;
	}


	public void setEgovMessCoresp(List<EgovMessagesCoresp> egovMessCoresp) {
		this.egovMessCoresp = egovMessCoresp;
	}


	public boolean isNastrZadnaData() {
		return nastrZadnaData;
	}


	public void setNastrZadnaData(boolean nastrZadnaData) {
		this.nastrZadnaData = nastrZadnaData;
	}


	public String getSpecRnDoc() {
		return specRnDoc;
	}


	public void setSpecRnDoc(String specRnDoc) {
		this.specRnDoc = specRnDoc;
	}


	

}
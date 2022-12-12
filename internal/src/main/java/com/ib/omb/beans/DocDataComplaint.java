package com.ib.omb.beans;

import static com.ib.system.utils.SearchUtils.isEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.inject.Named;
import javax.mail.MessagingException;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.DocDocDAO;
import com.ib.omb.db.dao.DocJalbaDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocJalba;
import com.ib.omb.db.dto.DocJalbaResult;
import com.ib.omb.search.DocJalbaSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysClassifAdapter;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.mail.Mailer;
import com.ib.system.mail.Mailer.Content;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.ValidationUtils;

import bg.government.regixclient.RegixClient;
import bg.government.regixclient.requests.grao.GraoOperation;
import bg.government.regixclient.requests.grao.nbd.PersonDataRequestType;
import bg.government.regixclient.requests.grao.nbd.PersonDataResponseType;

/**
 * Актуализация на документ жалба
 * Достъп по следните правила:
 *	потребителят е водещ експерт на жалбата
 *	или потребителят е ръководител на звеното, на което е разпределена жалбата
 *	или потребителят има дефинитивно право „Пълен достъп за актуализация на жалби“
 *	или потребителят има бизнес роля Деловодител
 *	или потребителят е регистрирал документа.
 *
 * @author rosi
 */
@Named
@ViewScoped
public class DocDataComplaint   extends IndexUIbean   implements Serializable {
	/**  */
	private static final long serialVersionUID = 8191901936895268740L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocDataComplaint.class);
	
	public  static final String DOCFORMTABS      = "docForm:tabsDoc";
	public  static final String	MSGVALIDDATES	 = "docu.validDates";
	private static final String MSGVALIDEGN = "refCorr.msgValidEgn"; 
	
	/**
	 * Идентификатор за жалбоподателя - ЕГН или ЛНЧ
	 *
	 */
	private enum IdentEgnOrLnch {
		ID_EGN,   
	    ID_LNCH
	}
	
	private int countryBG; // ще се инициализира в getter-а през системна настройка: delo.countryBG
	
	/**
	 * ЕГН/ЛНЧ- кое поле да се покаже за въвждане
	 */
	private String identJbp;

	private Doc document;
	/**
	 * списък файлове към документа
	 */
	private List<Files> filesList;
	
	private String rnFullDocEdit; 
	/**
	 * рег.ном./дата на протокол за унищожаване на документ
	 */
	private String rnFullProtocol;
	private Date decodeDate = new Date();


	private transient DocDAO	dao;
	private SystemData sd;	
	private UserData ud;
	private Integer docVidInclude; // коя страница да вмъкне
	private DocData docDataBean;
	private boolean scanModuleExist;
	private transient Map<Integer, Object> specificsAdm;	
	/**
	 * за какво действие е извикан екрана
	 */
	private int flagFW;
	private int isView;
	
	/**
	 * запазване на самоличност - 
	 */
	private boolean jbpHiddenB; 
	
	
	/**
	 * Свързан с корупция - да/не
	 */
	private boolean corruptionB;

	
	/**
	 *	Описаният проблем разглеждан ли е от други институции - да/не
	 */
	private boolean instCheckB;
	
	/**
	 * Идентифициран нарушител - името като текст.....
	 */
	private String txtNar;
	/**
	 * Категория и вид на нарушител като текст
	 */
	private String txtNarCategVid;
	/**
	 * true - дата на нарушението е преди 2 години
	 */
	private boolean warnDateNar;
	
	private transient Map<Integer, Object> specificsEKATTE;
	
	/**
	 * Списък с допустими състояния на жалбата
	 */
	private List<SelectItem> sastList;
	
	/**
	 * последно (предишно) състояние
	 */
	private Integer  sastLast;

	private Integer  pravaPrev;
	/**
	 * Вид оплакване - зависи от "засегнати права"
	 */
	private List<SystemClassif> vidOplList;
	
	
	/**
	 * Вид резултат - зависи от начина на финализиране
	 */
	private List<SystemClassif> vidResultList;
	
	/**
	 * Основание за недопустимост
	 */
	private List<SystemClassif> osnNeDopustList;
	
	/**
	 * Допълнителни експерти
	 */
	private List<SystemClassif>  scDopExpertsList; // заради autocomplete
	
	/**
	 * служители от избраното звено 
	 */
	private List<SystemClassif> zvenoSlList;
	
	/**
	 * справка за други жалби
	 */
	private LazyDataModelSQL2Array docPreviosList;	
	private List<Object[]> selPreviosJalbi; 
	private List<Object[]> selPreviosJalbiAll;

	/**
	 * ид на жалба, от която е породена текущата - при натискане на бутон Нова жалба със запазване на данни
	 */
	private Integer idPrevJ; 
	
	/**
	 * Справка за документи в преписката по жалбата, какви задачи са изпълнявани по тези документи, кои документи са изпратени и до кого.
	 */
	private List<Object[]> spravkaList;	
	
	/**
	 * Справка - история на състоянията на жалабата
	 */
	private List<Object[]> sastHistoryList;	
	
	/**
	 * код на орган в списъка с резултати - изп. се за корекция на данни за избран орган
	 */
	private Integer rowOrgan;
	
	private String nedopNarMsgWarn;
	
	private String fSize;
	
	
	/**
	 * пълен достъп за актуализация на жалби	 - през правата
	 */
	private boolean fullAccessJ;
	
	/**
	 * ръководител на звеното, на което е разпределена жалбата
	*/
	private boolean zvenoLiderJ;

	/**
	 *   водещ експерт на жалбата
	 */
	private boolean expertLider;
	
	/**
	 * потребител регистрирал жалбата
	 */
	private boolean userRegJalba;
	
	/**
	 * Право да редактира определени полета 
	 */
	//	Ако потребителят няма дефинитивно право „Пълен достъп за актуализация на жалби“, не е ръководител на звеното, към което е разпределена жалбата и не е водещ експерт, в екрана на жалба (вкл. при нова жалба) се забраняват за редакция следните полета: 
	//	Публикувана/Непубликувана
	//	Описание на жалбата от експерт
	//  Допълнителна информация
	//	Полетата в секция Финализиране и резултати
	//	Състоянието на жалбата, ако е различно от „заведена“ в БД (т.е. това, което е имала при отварянето на жалбата в екрана)????.
	private boolean editSpecFields;
	
	/**
	 * true - да се изпрати по компетентност
	 */
	private boolean competence;   
	
	/**
	 * рег. номер и дата на документа за подпис
	 */
	private String rnFullDocSign;
	
	private Integer countPorodeni;
	
	
	/** */
	@PostConstruct
	void initData() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("PostConstruct! Complaint" );
		}
		
		ud = getUserData(UserData.class);					
		sd = (SystemData) getSystemData();
		dao = new DocDAO(getUserData());
		
		DocData bean = (DocData) JSFUtils.getManagedBean("docData"); 
		if(bean != null){
			setDocVidInclude(DocData.INCL_COMPLAINT); 
			setDocument(bean.getDocument());
			setDocDataBean(bean);
			setScanModuleExist(bean.isScanModuleExist());
			setFilesList(bean.getFilesList());
			setRnFullProtocol(bean.getRnFullProtocol());
			setDecodeDate(document.getDocDate());
			setFlagFW(docDataBean.getFlagFW());
			setCompetence(bean.isCompetence());
			if(document.getId() == null) {
				actionNewDocJ(0, false, null);
			 // да зареди специфичните неща за нова жалбата!!
			}else {
				fSize="180"; // таблицата за файловете
				if(document.getJalba() == null) {
					document.setJalba(new DocJalba()); // не би трябвало да се случвa
					sastLast = OmbConstants.CODE_ZNACHENIE_JALBA_SAST_FILED;
				}else {
					loadSpecJalba();
					if(isView != 1) {
						checkForPorodeni();
					}
				}
			}
			getSastList();		
			loadSpecRights();
			
		}else {
			document = null;
			LOGGER.error("Грешка при зареждане на данните за документ от docData bean!");
		}
	}

	private void loadSpecRights() {
		fullAccessJ = ud.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_JALBA_FULL_EDIT);
		zvenoLiderJ = document.getId() == null  || ud.getAccessZvenoList() != null && ud.getZveno().equals(document.getJalba().getCodeZveno());
		expertLider = document.getId() == null  || ud.getUserAccess().equals(document.getJalba().getCodeExpert());
		userRegJalba = document.getId() == null || ud.getUserAccess().equals(document.getUserReg());
		//потребител с бизнес роля „Деловодител“	 - има булева променлива : userData.isDelovoditel()
		editSpecFields = fullAccessJ  || zvenoLiderJ || expertLider;
	}
	
	/**
	 * зарежда специфични за жалбата атрибути
	 */
	private void loadSpecJalba() {
		
     	  rnFullDocEdit = DocDAO.formRnDocDate(this.document.getRnDoc(), this.document.getDateReg(), this.document.getPoredDelo());

		  if(Objects.equals(document.getJalba().getJbpHidden(), OmbConstants.CODE_ZNACHENIE_DA)) {
			   jbpHiddenB = true;
		  }else {
			   jbpHiddenB = false;
		  }
	
		  if(Objects.equals(document.getJalba().getCorruption(), OmbConstants.CODE_ZNACHENIE_DA)) {
			  corruptionB = true;
		  }else {
			  corruptionB = false;
		  }
		  if(Objects.equals(document.getJalba().getInstCheck(), OmbConstants.CODE_ZNACHENIE_DA)) {
			  instCheckB = true;
		  }else {
			  instCheckB = false;
		  }
		  
		  //има вероятност от мобилното някой да въведе ЕГН и ЛНЧ едновременно - затова, ако са въведени и двете - да остане видимо ЕГН-то
		  if(document.getJalba().getJbpLnc() != null) {
		     identJbp = String.valueOf(IdentEgnOrLnch.ID_LNCH);
		  }
		  if(document.getJalba().getJbpEgn() != null) {
			 identJbp = String.valueOf(IdentEgnOrLnch.ID_EGN); 
		  }
		  
		  if(document.getJalba().getSast() != null) {
			  sastLast = document.getJalba().getSast(); 
		  }
		  if(document.getJalba().getZasPrava() != null) {
			  pravaPrev = document.getJalba().getZasPrava(); 
		  }
		
		  try {
			  
			  if(document.getJalba().getDopust() == null) {
				  setOsnNeDopustList(null);
				  document.getJalba().setDopust(OmbConstants.CODE_ZNACHENIE_DOPUST_DOPUST);
			  }else {
				  setOsnNeDopustList(getSd().getClassifByListVod(OmbConstants.CODE_LIST_DOPUST_OSN_NEDOPUST, document.getJalba().getDopust() , getCurrentLang(), new Date()));
			  }
			  if(document.getJalba().getZasPrava() == null) {
				  setVidOplList(null);
			  }else {
				  setVidOplList(getSd().getClassifByListVod(OmbConstants.CODE_LIST_ZAS_PRAVA_VID_OPL, document.getJalba().getZasPrava() , getCurrentLang(), new Date()));
			  }
			  if(document.getJalba().getFinMethod() == null) {
				  setVidResultList(null);
			  }else {
				  setVidResultList(getSd().getClassifByListVod(OmbConstants.CODE_LIST_JALBA_FIN_JALBA_RES, document.getJalba().getFinMethod() , getCurrentLang(), new Date()));
			  }
			  
			  if(document.getJalba().getCodeZveno() == null) {
				  zvenoSlList = null;
				  document.getJalba().setCodeExpert(null);
			  }else {
				  zvenoSlList = sd.getChildrenOnNextLevel(OmbConstants.CODE_CLASSIF_ADMIN_STR, document.getJalba().getCodeZveno(), new Date(), getCurrentLang());
			  }
			  // допълнителни експерти
			  loadScDopExpertsList();
  			  // списък резултати 
			  processScRezultList(true);
			  
			  checkDateNar(false);  
			  checkNar(false);			 
			
		  } catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане на жалба", e);				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG),e.getMessage());
		  }
		  
	}
	
	
	/**
	 * зарежда допълнителни експерти
	 */
	private void loadScDopExpertsList(){
		List<SystemClassif> tmpLst = new ArrayList<>();		
		if(document.getJalba().getDopExpertCodes() != null) {
			for( Integer item : document.getJalba().getDopExpertCodes()) {
				String tekst = "";
				SystemClassif scItem = new SystemClassif();
				try {
					scItem.setCodeClassif(OmbConstants.CODE_CLASSIF_ADMIN_STR);
					scItem.setCode(item);
					tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, item, getUserData().getCurrentLang(), new Date());		
					scItem.setTekst(tekst);
					tmpLst.add(scItem);
				} catch (DbErrorException e) {
					LOGGER.error("Грешка при зареждане на допълнителни експерти - жалба! ", e);
				}		
			}				
		}
		setScDopExpertsList(tmpLst); 
	}
	
	/** Нов документ - жалба... 
	 * flagCopy - 0 - без запомняне; 1- със запомняне на данни за жалбоподател; 2 -  със запомняне на данни за нарушение
	 * @return
	 */
	public void actionNewDocument(int flagCopy) {
		Doc docC = null;
		if(flagCopy > 0) {
			//запомняне на данни от предишната жалба //типа се помни винаги 
			docC = document;
		}
		docDataBean.actionNewDocument(false, true);	 // само, за да е еднакво с другите докуметни
		setDocument(docDataBean.getDocument());
		setFilesList(docDataBean.getFilesList());
		rnFullDocEdit = null;
		docDataBean.setRnFullDocEdit(null);
		countPorodeni = null;
		actionNewDocJ(flagCopy, true, docC);
	}
	
	/**
	 * Нова жалба
	 * стойности по подразбиране
	 * flagCopy -  0 - без запомняне; 1 - със запомняне на данни за жалбоподател; 2 - със запомняне на данни за нарушение
	 * flag2 = true - натиснат е бутон Нов вътре в екрана
	 * docC
	 */
	public void actionNewDocJ(int flagCopy, boolean flag2, Doc docC) {
		docDataBean.setCreatePrep(true); 
		if(flagFW != DocData.REG_FROM_MAIL_JALBA && flagFW != DocData.REG_FROM_EGOV_JALBA) {
			document.setJalba(new DocJalba());
			//email2 = null;
		} 
//			else {
//			email2 = document.getJalba().getJbpEmail();
//		}
		document.getJalba().setPublicVisible(OmbConstants.CODE_ZNACHENIE_NE);
		if(flagCopy != 1) {
			document.getJalba().setJbpGrj(getCountryBG());
			identJbp = String.valueOf(IdentEgnOrLnch.ID_EGN);
			if(flagFW != DocData.REG_FROM_EGOV_JALBA || document.getJalba().getJbpType() == null) {
				document.getJalba().setJbpType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL);
			}
		}
		if(flagCopy != 0) {
			if(docC.getJalba().getSubmitMethod() != null && !docC.getJalba().getSubmitMethod().equals(OmbConstants.CODE_ZNACHENIE_SUBMIT_METHOD_SSEV )) {
				document.getJalba().setSubmitMethod(docC.getJalba().getSubmitMethod()); // начин на подаване;
			}
			document.getJalba().setSubmitDate(new Date()); // дата на подаване; 
			document.setDeliveryMethod(docC.getDeliveryMethod()); // начин на получаване на отговора;
			document.getJalba().setRegistComment(docC.getJalba().getRegistComment()); // коментар при завеждане
		}
		document.getJalba().setDopust(OmbConstants.CODE_ZNACHENIE_DOPUST_DOPUST);
		document.getJalba().setSrok(DateUtils.addDays(new Date(), 30, true)); //  по подразбиране 30 дни (допустима) - 1 месеца	
		document.getJalba().setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_FILED);
		document.getJalba().setSastDate(new Date());
		sastLast = OmbConstants.CODE_ZNACHENIE_JALBA_SAST_FILED;
		
		if(flagCopy == 1) {
			// запазва данни за жалбоподателя
			document.getJalba().setJbpHidden(docC.getJalba().getJbpHidden());
			document.getJalba().setJbpType(docC.getJalba().getJbpType());
			document.getJalba().setJbpLnc(docC.getJalba().getJbpLnc());
			document.getJalba().setJbpEgn(docC.getJalba().getJbpEgn());
			document.getJalba().setJbpEik(docC.getJalba().getJbpEik()); // едно от 3-те винаги ще е празно, но така няма смисъл да проверявам типа на лицето
			document.getJalba().setJbpName(docC.getJalba().getJbpName());
			document.getJalba().setJbpGrj(docC.getJalba().getJbpGrj());
			document.getJalba().setJbpAddr(docC.getJalba().getJbpAddr());
			document.getJalba().setJbpPost(docC.getJalba().getJbpPost());
			document.getJalba().setJbpEkatte(docC.getJalba().getJbpEkatte());
			document.getJalba().setJbpEmail(docC.getJalba().getJbpEmail());
			document.getJalba().setJbpPhone(docC.getJalba().getJbpPhone());
			document.getJalba().setJbpPol(docC.getJalba().getJbpPol());
			document.getJalba().setJbpAge(docC.getJalba().getJbpAge());			
		//	document.getJalba().setRegistComment("Породена от: "+ docC.getRnDoc() + "/"+DateUtils.printDate((Date)docC.getDateReg())+"; "); 
		}else if(flagCopy == 2) {
			// запазва данни за описание  на жалбата и искането
			document.getJalba().setJalbaText(docC.getJalba().getJalbaText());
			document.getJalba().setRequestText(docC.getJalba().getRequestText());
			document.setOtnosno(docC.getOtnosno()); // описание от експерт
			// нарушение и корупция; звено и водещ и допълнителни експ., ако има;
			document.getJalba().setDateNar(docC.getJalba().getDateNar());
			document.getJalba().setSubjectNar(docC.getJalba().getSubjectNar());
			document.getJalba().setCodeNar(docC.getJalba().getCodeNar());
			document.getJalba().setZasPrava(docC.getJalba().getZasPrava());
			document.getJalba().setVidOpl(docC.getJalba().getVidOpl());
			document.getJalba().setCodeZveno(docC.getJalba().getCodeZveno());
			document.getJalba().setCorruption(docC.getJalba().getCorruption());
			document.getJalba().setInstNames(docC.getJalba().getInstNames());
			document.getJalba().setCodeExpert(docC.getJalba().getCodeExpert());
			document.getJalba().setDopExpertCodes(docC.getJalba().getDopExpertCodes());
			// допустимост
			document.getJalba().setDopust(docC.getJalba().getDopust());
			document.getJalba().setOsnNedopust(docC.getJalba().getOsnNedopust());
			if(document.getJalba().getCodeExpert() != null ) {
			  document.getJalba().setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_CHECK); // защото има експерт......... дали трябва да е така???
			  sastLast = OmbConstants.CODE_ZNACHENIE_JALBA_SAST_RAZPR;
			} else if(document.getJalba().getCodeZveno() != null ) {
			  document.getJalba().setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_RAZPR); // има звено, но няма експерт - да отиде в  от раб. плот "за определянв на експерт " 			
			}
		}		
		sastList = null;
		pravaPrev = null;
		if(flag2) {
			setVidResultList(null);
			setDocPreviosList(null);
			setSelPreviosJalbi(null);
			setSelPreviosJalbiAll(null);
		
			if(flagCopy != 1) {
				jbpHiddenB = false;
			}
			if(flagCopy != 2) {
				setScDopExpertsList(null);		
				setOsnNeDopustList(null);
				setTxtNarCategVid(null);
				setVidOplList(null);
				corruptionB = false;
				instCheckB = false;
				warnDateNar = false;
				nedopNarMsgWarn = null;
			}
			if(flagCopy == 0) {
				idPrevJ = null; // без запомняне				
			}  else {
				// да се извлече винаги, защото няма гаранция, че нещо не е променено през таб връзки....		
				// търси основаната, 1-та жалба - тази, от която са поредени следващите
				DocJalbaDAO daoJ = new DocJalbaDAO(getUserData());
				try {
					idPrevJ = daoJ.findOsnPorodenaId(docC.getId());
				} catch (DbErrorException e) {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,  e.getMessage());
					LOGGER.info(e.getMessage(), e);
				}	finally {
					JPA.getUtil().closeConnection();
				}
			}
		}
		docC = null;
		fSize="350"; //за таблицата с файлове..
	}
	
	
	
	/**
	 * Нов работен към преписката на текущата жалба
	 * Препраща към екрана на раб. док. където видовете док. са само  от класификация "Документи при обработка на жалба"
	 * Раб. документ влиза в раздел "вътрешен" на преписката на жалбата 
	 * @returnna 
	 */
	public String actionGotoWrkDoc() {
		return "docNew.jsf?faces-redirect=true&idJ=" + document.getId()+"&jStr="+rnFullDocEdit+"&v="+docVidInclude;
	}
	
	/**
	 * Код на гражданство(държавата) България
	 * @return
	 */
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
	


	/**
	 * Проверка за валидно ЕГН
	 */
	public boolean  actionValidateEGN() {
		boolean ok = true;
		boolean b1 = !isEmpty(document.getJalba().getJbpEgn());
		if (b1 && !ValidationUtils.isValidEGN(document.getJalba().getJbpEgn())) {
			JSFUtils.addMessage(DOCFORMTABS + ":egn", FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(beanMessages, MSGVALIDEGN));
			ok = false;
		}else if(b1){
			int pol = Integer.parseInt( document.getJalba().getJbpEgn().trim().substring(8, 9));
			pol = pol % 2;
			if (pol == 0) {
				document.getJalba().setJbpPol(1); //мъж
			}else {
				document.getJalba().setJbpPol(2); //жена
			}
		}
		return ok;
	}
  
	/**
	 * Проверка по ЕГН в RegiX 
	 * Зарежда Имената на жалбоподателя
	 */
	public void actionCheckEGN() {
		if (SearchUtils.isEmpty(document.getJalba().getJbpEgn())) {
			return; 
		}
			try {
				RegixClient client = getSystemData(SystemData.class).getRegixClient();
				PersonDataRequestType request = new PersonDataRequestType();
				request.setEGN(document.getJalba().getJbpEgn().trim());

				PersonDataResponseType response = (PersonDataResponseType) client.executeOperation(GraoOperation.PERSON_DATA_SEARCH, request);

				if (response != null) {
					String names = response.getPersonNames().getFirstName() + " " + response.getPersonNames().getSurName() + " " +response.getPersonNames().getFamilyName();
				
				//	Date dDate = DateUtils.toDate(response.getDeathDate());
				//	if(dDate != null) {
						// да излезе съобщение, че лицето е починало - TODO за сега го коментирам
				//		JSFUtils.addMessage(DOCFORMTABS + ":egn", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages,"jp.egnRegixMsg",DateUtils.printDate(dDate)));
				//	}else {
						document.getJalba().setJbpName(names);
				//	}
					// това е всичко което се връща
					//System.out.println(com.ib.system.utils.JAXBHelper.objectToXml(response, true));
				}
				
			} catch (Exception e) {
				LOGGER.error("Грешка при проверка по ЕГН в RegiX ", e);				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, "Грешка при проверка по ЕГН в RegiX"),e.getMessage());
			}
	}
	
	/**
	 * проверява дали е избран нарушител и дали за него има определени категория и вид
	 */
	public void checkPublicVisible() {		
		if(document.getJalba().getPublicVisible().equals(OmbConstants.CODE_ZNACHENIE_NE) &&
		  (document.getJalba().getCodeNar() == null || SearchUtils.isEmpty(txtNarCategVid))) {
			JSFUtils.addMessage(DOCFORMTABS+":refCorrInp",FacesMessage.SEVERITY_WARN,getMessageResourceString(beanMessages,"jp.publicVisibleMsg"));
		}
	}
	
	/**
	 * Запис на документ
	 */
	public void actionSave() {
			
		if(checkDataDoc()) { 			
			try {
				jalbaBParam();
				processScRezultList(false);
				Boolean tmpPorodeni =  document.getJalba().getCompletePorodeni();
				
				// файловете за нов документ - да се запишат с него
				// ако е редакция на документ - се записват при upload
				boolean newDoc = getDocument().getId() == null;

				if(!competence){
					this.document.setCompetence(OmbConstants.CODE_ZNACHENIE_COMPETENCE_OUR);
					this.document.setCompetenceText(null);
				} else if(!Objects.equals(this.document.getCompetence(), OmbConstants.CODE_ZNACHENIE_COMPETENCE_SENT)) {
					this.document.setCompetence(OmbConstants.CODE_ZNACHENIE_COMPETENCE_FOR_SEND);
				}
				
				saveDoc(newDoc);
				  			 				
				docDataBean.clearDeloDocLink(); //  махам връзката, ако се натисне втори път запис;  само за нов документ deloDocPrep.getId() би могло да е != null
					
				if(newDoc) {// само за нов документ	
					saveVrazkiPrev();
					rnFullDocEdit = DocDAO.formRnDocDate(this.document.getRnDoc(), this.document.getDateReg(), this.document.getPoredDelo());
					docDataBean.setRnFullDocEdit(rnFullDocEdit);
			   
					// заключване на док.
					docDataBean.lockDoc(getDocument().getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
					docDataBean.setCreatePrep(false);
					
					docDataBean.newDocFromMail();
					
					docDataBean.setFlagFW(DocData.UPDATE_DOC); 
					setFlagFW(DocData.UPDATE_DOC);						
 
				}
				//променям списък със състояния в зависимост от последното записано
				sastLast = document.getJalba().getSast();
				sastList = null; // за да се презареди;
				
				processScRezultList(true); // ако измисля нещо по-умно ще го променя.. :( Правя всичко това, защото иначе не се разкодира името....
				loadSpecRights();
				if(Objects.equals(tmpPorodeni, true)) {
					//породените жалби трябва да са приключени при последния запис => да се махне чекбокса 
					countPorodeni = null;
				}
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );		
			} catch (ObjectInUseException e) {
				LOGGER.error("Грешка при запис на жалба! ObjectInUseException "); 
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} catch (BaseException e) {			
				LOGGER.error("Грешка при запис на жалба! BaseException", e);				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  e.getMessage());
			} catch (Exception e) {
				LOGGER.error("Грешка при запис на жалба! ", e);					
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  e.getMessage());
			}
		}
	}
	
	/**
	 * при промяна на е-мейл на жалбоподателя, да го прехвърли и в това поле - за да е еднакво с другите документи при изпращане емейл!!
	 */
	public void copyMailJp() {
		document.setReceivedBy(document.getJalba().getJbpEmail());  
	}
	
	/**
	 * Формира и изпраща е-мейл за отговор
	 */
	public void createReturnMailJ() {
		if( !SearchUtils.isEmpty(document.getJalba().getJbpEmail()) ){
			Mailer mm = new Mailer();
			try {
				String subject = getMessageResourceString(beanMessages, "jp.mailSubject");					
				String bodyPlainText = docDataBean.mailBodyTextPlain(true);
				
				Properties props = sd.getMailProp(-1, "DEFAULT"); // за да вземе  от настройките на системата
				
				String user = props.getProperty("user.name");
	            String pass = props.getProperty("user.password");
	            String from = props.getProperty("mail.from","noreply@delovodstvo.com");
				
	            mm.sent(Content.HTML, props, user, pass, from, "Деловодство", document.getJalba().getJbpEmail(), subject, bodyPlainText, null);
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "jp.sentMailMsg") );	// изпратен, е но ако мейла на подателя е грешен - няма как да сме сигурни,че е успешно....
				
				// запис в журнала, че e изпратен е-мейл за регистрация
				SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, this.document.getId(), this.getRnFullDocEdit()+" Изпратен е е-мейл за регистрация до подателя ("+ document.getJalba().getJbpEmail()+").");
				journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
				journal.setDateAction(new Date());
				journal.setIdUser(getUserData().getUserId());
				JPA.getUtil().runInTransaction(() -> this.dao.saveAudit(journal));	
				
			} catch (  BaseException | MessagingException e) {
				LOGGER.error("Грешка при формиране на е-мейл и връщане на е-мейл към подателя! ", e); 
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			} 
		}
	}
	
	/**
	 * специфични параметри за жалбата
	 */
	private void jalbaBParam() {
		if(jbpHiddenB) {
			document.getJalba().setJbpHidden(OmbConstants.CODE_ZNACHENIE_DA);
		} else {
			document.getJalba().setJbpHidden(OmbConstants.CODE_ZNACHENIE_NE);
		}
	
		if(corruptionB) {
			document.getJalba().setCorruption(OmbConstants.CODE_ZNACHENIE_DA);
		} else {
			document.getJalba().setCorruption(OmbConstants.CODE_ZNACHENIE_NE);
		}
		if(instCheckB) {
			document.getJalba().setInstCheck(OmbConstants.CODE_ZNACHENIE_DA);
		} else {
			document.getJalba().setInstCheck(OmbConstants.CODE_ZNACHENIE_NE);
		}
		//Ако жалбата е в състояние „разпределена“ и се въведе водещ експерт, състоянието се променя на „проверка“.
		if(Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_RAZPR, document.getJalba().getSast()) && document.getJalba().getCodeExpert() != null){
			document.getJalba().setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_CHECK);
		}
		//Ако жалбата е в състояние „проверка“  се изисква задължително да се въведе водещия експерт!!!
		
	}
	
	/**
	 * извиква метода за запис на документ и файловете в обща транзакция
	 * @param newDoc
	 * @throws BaseException
	 */
	private void saveDoc(boolean newDoc) throws BaseException {
		JPA.getUtil().runInTransaction(() -> { 
			getDocument().setCountFiles(getFilesList() == null ? 0 :getFilesList().size());
			
			setDocument(getDao().save(getDocument(), docDataBean.isCreatePrep(), null, null, getSystemData()));
			if (newDoc && filesList != null && !filesList.isEmpty()) {
				// при регистриране на офицален от работен!!!
				// да направи връзка към  новия документ
				FilesDAO filesDao = new FilesDAO(getUserData());
				for (Files f : filesList) {
					filesDao.saveFileObject(f, this.document.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
				}
			}	
			
			if(docDataBean.getEgovMess() != null) {
				// 1. да се направи update на съобщението в table EGOV_MESSAGES
				// 2. да се изпрати отговор, че е регистрирано!
				docDataBean.createReturnEGOV(true);
				//TDDO - запис на файловете!!!!
			}
		});
	}

	
	/**
	 * проверка за задължителни полета при запис на документ
	 */
	public boolean checkDataDoc() {
		boolean flagSave = true;
		boolean openContactArea = false;
		if(document.getRegisterId() == null) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(beanMessages, "specDoc.regSpecErr", getMessageResourceString(LABELS, "specDoc.regJalbi")));// "Липсва регистър! За потребителя не е указан регистър ");
			flagSave = false;
		}
				
		if(SearchUtils.isEmpty(document.getJalba().getJbpName())) {
			JSFUtils.addMessage(DOCFORMTABS+":nameA",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "jp.namesJp")));
			flagSave = false;
		}
		
		if (!isEmpty(document.getJalba().getJbpEmail()) && !ValidationUtils.isEmailValid(document.getJalba().getJbpEmail())) {			   
			JSFUtils.addMessage(DOCFORMTABS+":jpMail",FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(UI_beanMessages, "general.validE-mail"));
			flagSave = false;
		    openContactArea = true;
	    }

		if(SearchUtils.isEmpty(document.getJalba().getJalbaText())) {
			JSFUtils.addMessage(DOCFORMTABS+":otnosnoA",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "jp.opisJlp")));
			flagSave = false;
		}
	
			
		//Само потребител с дефинитивно право „Пълен достъп за актуализация на жалби“ или ръководител на звеното, на което е разпределена 
		//жалбата може да променя състоянието на жалбата от „приключена“ на „повторно отворена“.
		boolean b1 =  Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_REOPENED, document.getJalba().getSast()) ;
		boolean b2 =  Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED, sastLast);
		boolean b3 = fullAccessJ || zvenoLiderJ; 
		if(b1 && b2 && !b3) {
			JSFUtils.addMessage(DOCFORMTABS+":status",FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(beanMessages, "jp.msgValidStatReopen"));
			flagSave = false;
		}		
		
		// Aко няма засегнати права - може да бъде само в състояние: заведена, за  разпределяне, за преценка
		// Жалбата може да е в състояние "разпределена", ако има определени  "Засегнати права" и звено.
		// Жалбата може да е в състояние "проверка" и "повторно отворена", ако има определени  "Засегнати права", звено и водещ експерт.
		if(document.getJalba().getZasPrava() == null && 
		   !(Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_FILED, document.getJalba().getSast()) ||
			Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_ZA_RAZPR, document.getJalba().getSast()) ||
			Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_ZA_PREC, document.getJalba().getSast()) ) ){
			JSFUtils.addMessage(DOCFORMTABS+":prava:аutoCompl",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "jp.prava")));
			flagSave = false;
		}
		
		if (Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_CHECK, document.getJalba().getSast()) || Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_REOPENED, document.getJalba().getSast())  ) {
			if( document.getJalba().getCodeZveno() == null ){
				zvenoSlList = null;
				// Не би трябвало да се случва, ако има засеганти права!!!
				JSFUtils.addMessage(DOCFORMTABS+":zveno:аutoCompl",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "jp.zvenoRazp")));
				flagSave = false;
			}
			if (document.getJalba().getCodeExpert() == null) {
				JSFUtils.addMessage(DOCFORMTABS+":expert1",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "jp.vodeshExp")));
				flagSave = false;
			}
		}
		
		if (Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_ZA_OTG, document.getJalba().getSast()) &&
			Objects.equals(OmbConstants.CODE_ZNACHENIE_DOPUST_DOPUST, document.getJalba().getDopust()) ) {
			JSFUtils.addMessage(DOCFORMTABS+":dopust",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"jp.dopustValidMsg"));
			flagSave = false;	
		}

		//проверка, ако е приключена - да има и попълнен начин на финализиране
		if (Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED, document.getJalba().getSast())){
			if( document.getJalba().getFinMethod() == null ){
				JSFUtils.addMessage(DOCFORMTABS+":final",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"jp.finalReq"));
				flagSave = false;
			}
		}
		// Допълнителни проверки
		// да се допуска въвеждане на експерти, само ако е въведено звено
		if( document.getJalba().getCodeZveno() == null  && 
		   (document.getJalba().getCodeExpert() != null || (document.getJalba().getDopExpertCodes() !=null && !document.getJalba().getDopExpertCodes().isEmpty()) )){
			JSFUtils.addMessage(DOCFORMTABS+":zveno:аutoCompl",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"jp.zvenoExpert"));
			flagSave = false;
		}
	
		if(Objects.equals(document.getDeliveryMethod(), OmbConstants.CODE_ZNACHENIE_ANSWER_METHOD_POST)) {
		// ако начина на предоставяне е по пощата - да има задължително въведен адрес и населено място на жалбоподател
			if(document.getJalba().getJbpEkatte() == null) {
				JSFUtils.addMessage(DOCFORMTABS+":mestoC:аutoCompl",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"jp.anspost1"));
				flagSave = false;	
				openContactArea = true;
			}
			if(SearchUtils.isEmpty(document.getJalba().getJbpAddr())) {
				JSFUtils.addMessage(DOCFORMTABS+":pgAddrA",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"jp.anspost2"));
				flagSave = false;	
				openContactArea = true;
			}
		} else if (Objects.equals(document.getDeliveryMethod(), OmbConstants.CODE_ZNACHENIE_ANSWER_METHOD_EMAIL) 
				&& SearchUtils.isEmpty(document.getJalba().getJbpEmail())){
		// ако начина на предоставяне е по e-mail - да има задължително въведен e-mail
			JSFUtils.addMessage(DOCFORMTABS+":mailA",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"jp.ansmail"));
			flagSave = false;	
			openContactArea = true;
		}else if (Objects.equals(document.getDeliveryMethod(), OmbConstants.CODE_ZNACHENIE_ANSWER_METHOD_PHONE)
				&& SearchUtils.isEmpty(document.getJalba().getJbpPhone())){
		// ако начина на предоставяне е по телефон - да има задължително въведен телефон			 
			JSFUtils.addMessage(DOCFORMTABS+":phoneA",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"jp.ansphone"));
			flagSave = false;	
			openContactArea = true;
		}
//		else if (!Objects.equals(document.getDeliveryMethod(), OmbConstants.CODE_ZNACHENIE_SUBMIT_METHOD_SSEV) &&
//				   Objects.equals(document.getDeliveryMethod(), OmbConstants.CODE_ZNACHENIE_ANSWER_METHOD_SSEV) &&
//				   SearchUtils.isEmpty(document.getJalba().getJbpEgn())){
//			//Ако е избрано "ССЕВ", но жалбата НЕ е полуена през ссев, да се изиска да е въведен ЕГН 
//		    JSFUtils.addMessage(DOCFORMTABS+":egn",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"jp.ansssev"));
//			flagSave = false;	
//			openContactArea = true;
//		}
		if(openContactArea) {
			PrimeFaces.current().executeScript("toggleSection(this, '#docForm:tabsDoc:p1RegD2')"); // панела може да е  затворен - да го отворя!
		}
		
		boolean flagSave1 =  checkDates();
		return flagSave && flagSave1;
	}

	/**
	 * Проверка за валидни дати в жалбата
	 * @return
	 */
	private boolean checkDates() {
		int flagDatesOk = 0;// всичко е ок
		
		Date docDate = DateUtils.startDate(document.getDocDate());
		// дата на нарушение - да се сложи проверка за недопустима жалба – нарушението е извършено преди повече от 2 години 
		// Само съобщение - без да забранява записа!!
		checkDateNar(true);
		if(Objects.equals(document.getJalba().getDopust(), OmbConstants.CODE_ZNACHENIE_DOPUST_DOPUST)) {
			checkNar(true);
		}
		// срок за обработка - след дата на документа
		Date tmpDate = DateUtils.startDate(document.getJalba().getSrok());
		if(tmpDate == null ) {
			srokByDefault();
		} else {
			flagDatesOk += docDataBean.checkDatesB(tmpDate, docDate, ":srokDate", "jp.srok", MSGVALIDDATES);
		}
		
		// дата на подаване
		tmpDate = DateUtils.startDate(document.getJalba().getSubmitDate());
		if(tmpDate == null ) {
			document.getJalba().setSubmitDate(docDate);
		} else {
			flagDatesOk += docDataBean.checkDatesA(tmpDate, docDate, ":podavaneDat", "jp.podavaneDate", MSGVALIDDATES);
		}		
		// дата на валидност - след дата на документа
		tmpDate = DateUtils.startDate(document.getValidDate());
		if(tmpDate == null ) {
			document.setValidDate(docDate);
		} else {
			flagDatesOk += docDataBean.checkDatesB(tmpDate, docDate, ":validDat", "docu.validDate", MSGVALIDDATES);
		}

		 //Финализиране на резултата  - полето за вид резултат е задължително! Датите в резултата!
		 List<DocJalbaResult> tmpL = document.getJalba().getJalbaResultList();
		 if(tmpL != null && !tmpL.isEmpty()) {
			 if (document.getJalba().getFinMethod() == null) {
				 JSFUtils.addMessage(DOCFORMTABS+":final",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "jp.final")));
				 flagDatesOk++;
			 }
			 for (DocJalbaResult row: tmpL) {
				if(row.getVidResult() == null) {
					JSFUtils.addMessage(DOCFORMTABS+":rezultFinalLst",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "jp.resultVid")));
					flagDatesOk++;	
					break;
				}
				if(row.getDateResult() != null){
					tmpDate = DateUtils.startDate(row.getDateResult());	
					if(docDataBean.checkDatesB(tmpDate, docDate, ":rezultFinalLst", "jp.resultDate", MSGVALIDDATES) > 0){
						flagDatesOk++;
						break;
					}
				}
			 }
		 }

		return flagDatesOk == 0;
	}
	
	/**
	 *  проверка за дата на нарушение - ако е  преди 2 години - да излезе иконка за предупреждение
	 */
	public void checkDateNar(boolean msg) {		
		Date docDate = (document.getDocDate() == null) ? DateUtils.startDate(new Date()): DateUtils.startDate(document.getDocDate());
		// дата на нарушение - да се сложи проверка за недопустима жалба – нарушението е извършено преди повече от 2 години 
		// Само съобщение - без да забранява записа!!
		Date tmpDate = DateUtils.startDate(document.getJalba().getDateNar());
		if(tmpDate != null && tmpDate.before(DateUtils.addDays(docDate, -731, false)) ) {
			if(msg && document.getId() == null) {
				// съобщение - само при запис и то, ако е  за нов документ
				JSFUtils.addMessage(DOCFORMTABS+":nDat",FacesMessage.SEVERITY_WARN,getMessageResourceString(beanMessages,"jp.dateNarBefore2Y",getMessageResourceString(LABELS, "jbp.narushenieDate")));
			}
			warnDateNar = true;
		}else {
			warnDateNar = false;
		}	
	}

	/**
	 * Проверка дали въведения като текст нарушител е от допустимите
	 * Ако не е - излиза съобщение
	 */
	public void checkNar(boolean msg) {
		String txtNar = "";
		nedopNarMsgWarn = null;
		if(!SearchUtils.isEmpty(document.getJalba().getSubjectNar())) {
			String[] txtNar1 = document.getJalba().getSubjectNar().trim().toUpperCase().split(" ");
			for(String item : txtNar1) {
				txtNar += item+" " ; 
			}
			txtNar = txtNar.trim();
			// Органи, недопустими за разглеждане
			try {
				List<SystemClassif> classifList = getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_ORGAN_NEDOPUST, new Date(), getCurrentLang());
				for( SystemClassif item : classifList) {
					String ss = item.getTekst().toUpperCase();
					if(txtNar.contains(ss)) { // за сега търсим по пълно съвпадение
						nedopNarMsgWarn = getMessageResourceString(beanMessages,"jp.nedopustimNar");
						if(msg && document.getId() == null) {
							// съобщение - само при запис и то, ако е  за нов документ
							JSFUtils.addMessage(DOCFORMTABS+":sNar",FacesMessage.SEVERITY_WARN,getMessageResourceString(beanMessages,"jp.nedopustimNar",getMessageResourceString(LABELS, "jbp.narushenieDate")));
						}
					}
				}
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при извличане на класификация:  Органи, недопустими за разглеждане ", e);		
			}
		}
	}

	/**
	 *  запис на връзката с основната жалба, от която е породената текущата
	 *  само за нов документ
	 */
     public void saveVrazkiPrev() {
		try {
			if (idPrevJ != null) {
				JPA.getUtil().runInTransaction(() -> { 
					new DocDocDAO(getUserData()).save(null, OmbConstants.CODE_ZNACHENIE_DOC_REL_E_PORODENA, this.document.getId(), idPrevJ, false);
				});
		
				//Задава параметър за рефрешване на списъка със свързани документи при запис 
				DocDataVrazki docDataVrBean = (DocDataVrazki) JSFUtils.getManagedBean("docDataVrazki");
		 		if(	docDataVrBean != null) {
		 			docDataVrBean.setRefreshList(true);
		 		}
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "jp.SuccesMsgVrJPrev") );
				idPrevJ = null; 
			}
		} catch (BaseException e) {
			LOGGER.error("Грешка при запис на връзката с основната жалба, от която е породената текущата! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG),e.getMessage());
		}		
     }
	
	/**
	 * Изтриване на деловоден документ
	 * права на потребителя за изтриване
	 * 
	 */
	public void actionDelete() {
		try {
	
			getDocDataBean().deleteDoc(document.getId());
		
			getDao().notifDocDelete(getDocument(), getSd());

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,  IndexUIbean.getMessageResourceString(UI_beanMessages, "general.successDeleteMsg") );
			
			actionNewDocument(0);			
			docDataBean.setCreatePrep(true); 
		} catch (ObjectInUseException e) {
			// ако инициира преписка и има други връзки 
			LOGGER.error("Грешка при изтриване на жалба!", e); 
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, DocData.OBJECTINUSE), e.getMessage());
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на жалба!", e);			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} 
	}

		
	/**
	 * Засегнати права 
	 * Само ако жалбата е допустима:
	 * да се презареди списъкът за вид оплакване
	 * да зареди звеното
	 * да смени състоянието, само ако предишното е било - "заведена"  
	 */
	 public void actionChangePrava() {
		try {
			//	Ако потребителят няма дефинитивно право „Пълен достъп за актуализация на жалби“
			//и няма бизнес роля „Деловодител“ , 
			//и не е регистрирал жалбата, 
			//при опит за промяна на засегнати права към такива, за които не отговаря звеното, към което е разпределена жалбата,
			//то засегнатите права не се променят, а се извежда съобщение, че няма прав
			
			Integer selectedPrava = document.getJalba().getZasPrava();
			Integer newZveno = null;
			SystemClassif scPrava = null;
			if(selectedPrava != null) {
				 scPrava = sd.decodeItemLite(OmbConstants.CODE_CLASSIF_ZAS_PRAVA, selectedPrava, getCurrentLang(), new Date(), false);
				 if(!SearchUtils.isEmpty(scPrava.getCodeExt())) {
					 newZveno = Integer.valueOf(scPrava.getCodeExt());
				 }
			}
			boolean zv = Objects.equals(newZveno,  document.getJalba().getCodeZveno());
			if(zv || fullAccessJ || ud.isDelovoditel()  || userRegJalba ) {
				document.getJalba().setVidOpl(null);
				if(selectedPrava == null) {
					setVidOplList(null);
					// звеното да се зачисти, само ако  жалбата е допустима
					if(Objects.equals(OmbConstants.CODE_ZNACHENIE_DOPUST_DOPUST, document.getJalba().getDopust())) {
						document.getJalba().setCodeZveno(null);
						zvenoSlList = null;
						document.getJalba().setCodeExpert(null);
					}
				}else {
					// вид оплакване
					setVidOplList(getSd().getClassifByListVod(OmbConstants.CODE_LIST_ZAS_PRAVA_VID_OPL, selectedPrava , getCurrentLang(), new Date()));
					changeZveno(sastLast, false, scPrava);
				} 
				pravaPrev = selectedPrava;
			} else {
				JSFUtils.addMessage(DOCFORMTABS+":prava",FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(beanMessages, "jp.msgValidChPrava"));
				PrimeFaces.current().executeScript("cmdZasPrava()");
				document.getJalba().setZasPrava(pravaPrev); // връщам правата както са били преди
			}
			
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при избор на засегнати права", e);				
		//	JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG),e.getMessage());
		}		    	    
	 }
	 
	
	 /**
	  * зарежда звеното и състоянието в зависимост от избраните права
	  * @param sast
	  * @param chDop - извиква се при промяна на полето "Допустимост"
	  */
	 private void changeZveno(Integer sast, boolean chDop, SystemClassif scPrava) {
		 Integer zvenoOld = document.getJalba().getCodeZveno();
		// автом. избор на звено   		
		 boolean bSast  = Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_FILED, sast);
		 boolean bSast2 = Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_ZA_OTG, sast);
		 if(Objects.equals(OmbConstants.CODE_ZNACHENIE_DOPUST_DOPUST, document.getJalba().getDopust()) ) {
			 // допустима
			try {
				if(scPrava == null) {
					 scPrava = sd.decodeItemLite(OmbConstants.CODE_CLASSIF_ZAS_PRAVA, document.getJalba().getZasPrava(), getCurrentLang(), new Date(), false);
				}
			    if(scPrava != null && !SearchUtils.isEmpty(scPrava.getCodeExt())) {
			    	document.getJalba().setCodeZveno(Integer.valueOf(scPrava.getCodeExt()));
			    }
			    if(bSast){
					 document.getJalba().setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_RAZPR);
				}
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане на звеното и състоянието в зависимост от избраните засегнати права", e);				
			}	
		} else	if(chDop && (bSast || bSast2)){
			// недопустима, извикано е при промяна на полето "допустимост" 
			document.getJalba().setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_ZA_OTG);
			loadZvenoPrimena();
		}
		 actionLoadSlZveno(zvenoOld);
	 }

	 
	 public void actionLoadSlZveno(Integer zvenoOld) {
		    if(document.getJalba().getCodeZveno() != null) {
				try {
					zvenoSlList = sd.getChildrenOnNextLevel(OmbConstants.CODE_CLASSIF_ADMIN_STR, document.getJalba().getCodeZveno(), new Date(), getCurrentLang());
					if(!Objects.equals(document.getJalba().getCodeZveno(), zvenoOld)) {
						document.getJalba().setCodeExpert(null);
					}
				} catch (DbErrorException e) {
					LOGGER.error("Грешка при зареждане на служители от избрано звено ", e);		
				}
			} else {
				zvenoSlList = null;
				document.getJalba().setCodeExpert(null);
			}
	 }
	 
	 
	 /**
	  * Зарежда в звено - „Приемна и деловодство“
	  */
	 private void loadZvenoPrimena() {
		try {
			String	primena = sd.getSettingsValue("omb.priemnaDelovodstvo");//  код на отдел „Приемна и деловодство“
			if(!SearchUtils.isEmpty(primena)) {
				document.getJalba().setCodeZveno(Integer.valueOf(primena));
				//document.getJalba().setZasPrava(null); дали е нужно? ако отиде към приемната а не махна засегнати права????
			}
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при извличане на настройка - код на отдел „Приемна и деловодство“", e);		
		} 
	 }
	 
		
	 /**
	 *  Ако е избрано "Отговор за недопустимост" 
	 *  и ако жалбата е недопустима, зареждаме звено = отдел „Приемна и деловодство“.
	 *  Ако новото състояние е "приключена" - да провери за породени жалби 
	 */
	 public void actionChangeSast() {
		 boolean d = !Objects.equals(OmbConstants.CODE_ZNACHENIE_DOPUST_DOPUST, document.getJalba().getDopust());
		 boolean s = Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_ZA_OTG, document.getJalba().getSast()); 
		 countPorodeni = 0;
		 if (s && d) {
			 loadZvenoPrimena();
		 } else {
			 checkForPorodeni();
		 }
	 }
	 
	 /**
	  * проверка дали има неприключени породени жалби
	  */
	 private void checkForPorodeni() { //  вика се само в актуализация
		 if(Objects.equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED, document.getJalba().getSast())) {
			 //търси породени жалби, ако намери - предлага да бъдат приключени заедно с текущата жалба  
			 // трябва да са към един и същи нарушител....
			 // състоянието на жалбата трябва да е различно от 'повторно отворена' и 'приключена'
			DocJalbaDAO daoJ = new DocJalbaDAO(getUserData());
			try {
				countPorodeni = daoJ.selectPorodeniCount(document.getId(), document.getJalba().getCodeNar());
			} catch (DbErrorException e) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,  e.getMessage());
				LOGGER.info(e.getMessage(), e);
			}	finally {
				JPA.getUtil().closeConnection();
			}
			document.getJalba().setCompletePorodeni(false);
		 }
	 }
	 
	 /**
	 * Допустимост -  да се презареди списъкът за Основание за недопустимост
	 * Ако жалбата е допустима - да презареди звеното, в завсимост от правата
	 * Ако жалбата е недопустима и състоянието е "заведена", зареждаме звено = отдел „Приемна и деловодство“ и  сетваме състояние = "за отговор за недопустимост".
	 */
	 public void actionChangeDopust() {
		 try {
//		 	if(!Objects.equals( document.getJalba().getDopust(), OmbConstants.CODE_ZNACHENIE_DOPUST_DOPUST)) { // 
//		 		 document.getJalba().setFinMethod(OmbConstants.CODE_ZNACHENIE_JALBA_FIN_NEDOPUST);
//		 		//TODO какво ще стане със списъка с резултати, ако вече нещо е въведено???
//		 	}
			document.getJalba().setOsnNedopust(null);
			if(document.getJalba().getDopust() == null) {
				setOsnNeDopustList(null);
		 	}else {
		 		setOsnNeDopustList(getSd().getClassifByListVod(OmbConstants.CODE_LIST_DOPUST_OSN_NEDOPUST, document.getJalba().getDopust() , getCurrentLang(), new Date()));
		 		changeZveno(document.getJalba().getSast(), true, null);
		 		srokByDefault();
		 	}
		 } catch (DbErrorException e) {
			LOGGER.error("Грешка при презареждане списък за основание за недопустимост", e);				
			//	JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG),e.getMessage());
		 }		    
	 }
	 
	 /**
	  * зарежда срок за разглеждане по подразбиране в зависимост от допустимостта
	  */
	 private void srokByDefault() {
		Date docDate = DateUtils.startDate(document.getDocDate());
		if(Objects.equals(OmbConstants.CODE_ZNACHENIE_DOPUST_DOPUST, document.getJalba().getDopust())) {
			document.getJalba().setSrok(DateUtils.addDays(docDate, 30, true)); // срок по подразбиране, ако е допустима - 30 дни
		}else {
			document.getJalba().setSrok(DateUtils.addDays(docDate, 14, true)); // срок по подразбиране, ако е не е допустима - 14 дни
		}
	 }

		
	/** 
	 * Начин на финализиране -  да се презареди списъкът за вид резултат
	 */
	 public void actionChangeFinal() {
		 try {
			  if(document.getJalba().getFinMethod() == null) {
				 setVidResultList(null);
			  }else {
				 setVidResultList(getSd().getClassifByListVod(OmbConstants.CODE_LIST_JALBA_FIN_JALBA_RES, document.getJalba().getFinMethod() , getCurrentLang(), new Date()));
			  }
			  // ако вече има избрани значения в списъка с резултати - да ги нулирам!
			  List<DocJalbaResult> tmpL = document.getJalba().getJalbaResultList();
			  if(tmpL != null) {
				 for (DocJalbaResult row: tmpL) {
					row.setVidResult(null);
				 }
			  }
		 } catch (DbErrorException e) {
			LOGGER.error("Грешка при презареждане списък за вид резултат", e);				
			//	JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG),e.getMessage());
		 }		    
	 }

	 
	 /**
	  * Необходим е само заради "орган/лице", за което няма код, а само свободен текст 
	  * @param load = true - при зареждане за актуализация; false - преди записа
	  */
	 public void  processScRezultList(boolean load){
		 List<DocJalbaResult> tmpL = document.getJalba().getJalbaResultList();
		 if(tmpL != null) {
			 for (DocJalbaResult row: tmpL) {
				if(load && row.getCodeSubject() == null) {
					row.setCodeSubject(getDefaultCode());
				}else if(!load &&  getDefaultCode().equals(row.getCodeSubject())) {
					row.setCodeSubject(null);
				}
			 }
		 }
	 }
	 
	 /**
	 * Необходимо е в autocomplete за поле "Орган/лице" в списък с резултати 
	 * Там се допуска да изберат от класификацията на кореспондентите, но може и да напишат свободен текст!!
	 * Aко е въведен свободен текст, за да се визуализира правилно при актуализация и да се инициализира компонентата, 
	 * в selectedCode трябва да се подаде DEFAULTCODE = -999999999;  
	 */
	public Integer getDefaultCode() {
		return -999999999; 
	}
	 
	/**
	 * Нов резултат при финализиране
	 */
	 public void  actionNewResult() {
		 DocJalbaResult jr = new DocJalbaResult();
		 if (document.getJalba().getJalbaResultList() == null ) {
			 document.getJalba().setJalbaResultList(new ArrayList<DocJalbaResult>());
		 }
		 document.getJalba().getJalbaResultList().add(jr);
	 }
	 
	 /**
	  * Премахване на ред от таблицата с резултати
	  */
	public String actionRemoveResult(int key){
		if(document.getJalba().getJalbaResultList()  != null){
			document.getJalba().getJalbaResultList().remove(key);
		}		
		return null;		
	}
	
	
	
	/**
	 * Зарежда името на жалбоподателя в полето "орган/лице"
	 * @param key
	 * @return
	 */
	public String actionSetJbpR(int key){
		if(document.getJalba().getJalbaResultList()  != null){
			DocJalbaResult row =  document.getJalba().getJalbaResultList().get(key);
			row.setCodeSubject(getDefaultCode());
			row.setTextSubject(document.getJalba().getJbpName());
			row.setJbp(OmbConstants.CODE_ZNACHENIE_DA); // маркира, че е жалбоподател
		}	
		return null;		
	}
	
	/**
	 * да зачисти маркера, че резултата е до жалбоподател, т.е. че е бил натиснат бутона - до жалбоподател
	 */
	public void clearRSubject(int key){
		if(document.getJalba().getJalbaResultList()  != null){
			DocJalbaResult row =  document.getJalba().getJalbaResultList().get(key);
			row.setJbp(OmbConstants.CODE_ZNACHENIE_NE); // маркира, че е жалбоподател
		}
	}
	
	/**
	 * да зачисти информацията за категория и вид на нарушителя
	 */
	public void clearInfoNar() {
		if(isView != 1) {
			setTxtNarCategVid(null);
		}
	}
	
	/**
	 * зарежда категория и вид на нарушителя
	 */
	public void loadDopInfoNar() {
 		if(document.getJalba().getCodeNar() != null) {
 			setTxtNarCategVid("");
 			try {
 				Object[] spec = (Object[]) sd.getItemSpecifics(OmbConstants.CODE_CLASSIF_REFERENTS, document.getJalba().getCodeNar(), getUserData().getCurrentLang(), null);
 				Integer katNar = (Integer) spec[OmbClassifAdapter.REFERENTS_INDEX_KAT_NAR];
 				Integer vidNar = (Integer) spec[OmbClassifAdapter.REFERENTS_INDEX_VID_NAR];
 				String strK	 = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_KAT_NAR, katNar, getUserData().getCurrentLang(), new Date());
 				String strV	 = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_VID_NAR, vidNar, getUserData().getCurrentLang(), new Date());
 				if(!SearchUtils.isEmpty(strK)) {
 					txtNarCategVid = strK+"";
 				}
 				if(!SearchUtils.isEmpty(strV)) { 
 					txtNarCategVid += " --->" + strV +" ";
 				}
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане на категория и вид нарушител", e);				
				//	JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG),e.getMessage());
			}
 			
		}else {
			setTxtNarCategVid(null); 
		}
	}


	/**
	 * смяна на типа на жалбоподавателя
	 * Нулира полетата, които сa само за физическо лице или само за нефизическо 
	 */
	public void actionChTypeApplicant() {
		if(Objects.equals(document.getJalba().getJbpType(), OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL)) {
			document.getJalba().setJbpEgn(null);		// ЕГН;
			document.getJalba().setJbpLnc(null);		// ЛНЧ;
			document.getJalba().setJbpPol(null);  		// Пол;
			document.getJalba().setJbpAge(null);  		// възраст;
			document.getJalba().setJbpHidden(null);  	// запазена самоличност;
			document.getJalba().setJbpGrj(null);		// гражданство
		}else{
			document.getJalba().setJbpEik(null);			// ЕИК;
			document.getJalba().setJbpGrj(getCountryBG());	// БГ - по подразбиране
		}		
	}

	
	/**
	 * Справка - история на състоянията на жалбата
	 */
	public void actionSprSast() {
		DocJalbaDAO daoJ = new DocJalbaDAO(getUserData());
		try {
			setSastHistoryList(daoJ.selectSastHistory(document.getId()));
		} catch (DbErrorException e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,  e.getMessage());
			LOGGER.info(e.getMessage(), e);
		}	
	}
	
	/**
	 * Справка за документи в преписката по жалбата, какви задачи са изпълнявани по тези документи, кои документи са изпратени и до кого.
	 */
	public void actionSpravka() {
		DocJalbaSearch tmp = new DocJalbaSearch();
		try {
			setSpravkaList(tmp.selectProcessObjects(document.getId(), getSd(), getUd())); 
			String  cmdStr = "PF('modalSpravka').show();";
			PrimeFaces.current().executeScript(cmdStr);
		} catch (Exception e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,  e.getMessage());
			LOGGER.info(e.getMessage(), e);
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
	
	/**
	 * Справка за други жалби
	 * 
	 */
	public void actionSprPrevios() {
		DocJalbaSearch tmp = new DocJalbaSearch();
		try {
			tmp.setJbpName(document.getJalba().getJbpName());	// Имена на жалбоподател;
			tmp.setJbpEgn(document.getJalba().getJbpEgn());		// ЕГН;
			tmp.setJbpLnc(document.getJalba().getJbpLnc());		// ЛНЧ;
			tmp.setJbpEik(document.getJalba().getJbpEik());		// ЕИК;
			tmp.setJbpPhone(document.getJalba().getJbpPhone());	// телефон
			tmp.setJbpEmail(document.getJalba().getJbpEmail());	
			tmp.buildQueryPrevJalbaList(document.getId());
			docPreviosList = new LazyDataModelSQL2Array(tmp, "a1 desc"); 
			
			setSelPreviosJalbiAll(null);
			setSelPreviosJalbi(null);
					
			String  cmdStr = "PF('modalPreviosSpr').show();";
			PrimeFaces.current().executeScript(cmdStr);
		} catch (InvalidParameterException e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,  e.getMessage());
			LOGGER.info(e.getMessage(), e);
		}
	}
	
	/**
	 * справка други жалби
	 * Избира всички редове от текущата страница  
	 * @param event
	 */
	public void onRowSelectAll(ToggleSelectEvent event) {    
    	List<Object[]> tmpL = new ArrayList<>();
    	tmpL.addAll(getSelPreviosJalbiAll());
    	if(event.isSelected()) {
    		onRowSelectAllAddItem(tmpL);
    	}else {
    		onRowSelectAllDelItem(tmpL);	
		}
    	setSelPreviosJalbiAll(tmpL);	
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("onToggleSelect->>");
		}
	}
		
	
    /** 
     * справка други жалби
     * Select one row 
     * @param event
     */
    public void onRowSelect(SelectEvent<?> event) {    	
    	if(event!=null  && event.getObject()!=null) {
    		List<Object[]> tmpList =  getSelPreviosJalbiAll();
    		
    		Object[] obj = (Object[]) event.getObject();
    		if(obj != null && obj.length > 0) {
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
    				setSelPreviosJalbiAll(tmpList);   
    			}
    		}
    	}	    	
    	if (LOGGER.isDebugEnabled()) {
    		LOGGER.debug("1 onRowSelect->>{}",getSelPreviosJalbiAll().size());
    	}
    }
		 
		    
    /**
     * справка други жалби
     * unselect one row 
     * @param event
     */
    public void onRowUnselect(UnselectEvent<?> event) {
    	if(event!=null  && event.getObject()!=null) {
    		Object[] obj = (Object[]) event.getObject();
    		List<Object[] > tmpL = new ArrayList<>();
    		tmpL.addAll(getSelPreviosJalbiAll());
    		for (Object[] j : tmpL) {
    			Integer l1 = Integer.valueOf(j[0].toString());
    			Integer l2 = Integer.valueOf(obj[0].toString());
	    		if(l1.equals(l2)) {
	    			tmpL.remove(j);
	    			setSelPreviosJalbiAll(tmpL);
	    			break;
	    		}
    		}
    		if (LOGGER.isDebugEnabled()) {
    			LOGGER.debug( "onRowUnselectIil->>{}",getSelPreviosJalbiAll().size());
    		}
    	}
    }

    /**
     * справка други жалби
     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга
     */
    public void   onPageUpdateSelected(){
    	if (getSelPreviosJalbiAll() != null && !getSelPreviosJalbiAll().isEmpty()) {
    		getSelPreviosJalbi().clear();
    		getSelPreviosJalbi().addAll(getSelPreviosJalbiAll());
    	}	
    	if (LOGGER.isDebugEnabled()) {
    		LOGGER.debug( " onPageUpdateSelected->>{}",getSelPreviosJalbi().size());
    	}
    }

	
		  
	/**
	*   справка други жалби
	*   Добавя избраните редове в общия масив
	* @param tmpL
	*/
	private void onRowSelectAllAddItem(List<Object[]> tmpL ){
		for (Object[] obj : getSelPreviosJalbi()) {
			if(obj != null && obj.length > 0) {
				boolean bb = true;
				Integer l2 = Integer.valueOf(obj[0].toString());
				for (Object[] j : tmpL) { 
					Integer l1 = Integer.valueOf(j[0].toString());        			
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
	}
	
	/**
	 *  справка други жалби
	 * Изважда редовете от текущата страница 
	 * @param tmpL
	 */
	private void onRowSelectAllDelItem(List<Object[]> tmpL ) {
		List<Object[]> tmpLPageC =  getDocPreviosList().getResult();// rows from current page....    		
		for (Object[] obj : tmpLPageC) {
			if(obj != null && obj.length > 0) {
				Integer l2 = Integer.valueOf(obj[0].toString());
				for (Object[] j : tmpL) { 
					Integer l1 = Integer.valueOf(j[0].toString());        			
		    		if(l1.equals(l2)) {    	    			
		    			tmpL.remove(j);
		    			break;
		    		}	
	    		}
			}
		}    
	}
	

   /**
    * справка други жалби
    * Връща списъка с избрани жалби
    * Формира стринг с рег. номер и дата на избраните жалби го слага в полето "Коментар при завеждане"
    * затваря модалния
    */
   public void actionConfirmSelJalbi() {		
	   if(selPreviosJalbiAll != null && !selPreviosJalbiAll.isEmpty()) {
		   StringBuilder  sb = new StringBuilder(getMessageResourceString(LABELS, "jp.sprPreviosB") + ": ");
		   for (Object[] obj : selPreviosJalbiAll) {			   
			   sb.append(obj[1].toString());
			   sb.append("/"+DateUtils.printDate((Date)obj[2])); //Не се прави връзка между документи!!!!
			   try {
					sb.append(" ("+sd.decodeItem(OmbConstants.CODE_CLASSIF_JALBA_SAST,Integer.valueOf(obj[11].toString()), getCurrentLang(), new Date())+") ");
					if(obj[12] != null) {
						sb.append(" при " + sd.decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR,Integer.valueOf(obj[12].toString()), getCurrentLang(), new Date())+"; ");
					}
			   } catch (NumberFormatException | DbErrorException e) {
					LOGGER.error("Грешка при разкодиране на значения от класификация - състояние на жалба и/или адм. структура", e); 
			   }
			   
		   }
		   String tmp2 =  document.getJalba().getRegistComment() + sb.toString();
		   document.getJalba().setRegistComment(tmp2);
	   }
	   String  cmdStr = "PF('modalPreviosSpr').hide();";
	   PrimeFaces.current().executeScript(cmdStr);
	}
    
    
   /**
    * от "Други жалби"- копира адреса, емейл, телефон в полетата за контакт
    * @param row
    */
   public void actionCopyAddrJalba(Object[] row) {
	  if (row[9] != null) {
		  document.getJalba().setJbpEkatte(Integer.valueOf(row[9].toString()));
	  }
	  if (row[10] != null) {
		  document.getJalba().setJbpAddr(row[10].toString());
	  }
	  if (row[8] != null && Objects.equals(document.getJalba().getSubmitMethod(), OmbConstants.CODE_ZNACHENIE_SUBMIT_METHOD_EMAIL)) {
		  // ako e рег. по емейл - да не се променя!
		  document.getJalba().setJbpEmail(row[8].toString());	  
	  }
	  if (row[7] != null) {
		  document.getJalba().setJbpPhone(row[7].toString());
	  }
	  if(getSelPreviosJalbiAll() != null) {
		  actionConfirmSelJalbi();
	  }
   }
   
   
   /**
	 * разглеждане на жалба, преписка, задачи
	 * @param i
	 * @return
	 */
	public String actionGotoViewJ(Integer idObj, Integer typeObj, Integer idDoc) {
		String result = null; 
		if(Objects.equals(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, typeObj)){
			result = "docView.xhtml?faces-redirect=true&idObj=" + idObj;
		} else if(Objects.equals(OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO, typeObj)){
			result = "deloView.xhtml?faces-redirect=true&idObj=" + idObj;
		} else if(Objects.equals(OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK, typeObj)){
			result = "taskView.xhtml?faces-redirect=true&idObj=" + idObj+"&idDoc=" + idDoc; // задачи
		}else if(Objects.equals(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ, typeObj)){
			result = "docView.xhtml?faces-redirect=true&idObj=" + idObj; // разглеждане на док., към който е движението....
		}
		return result;
	}
	
	
	
	/*************       */


	/**
	 * Да обнови файловете
	 */
	 public void actionChangeFilesJ() {
		 docDataBean.actionChangeFiles();
	 }
	 
	 /**
	 * Да изпрати нотификация, че има нови файлове - изрично натискане на бутон !! 
	 * 0 няма нови файлове
	 *	>0 броя на новите файлове
	 * == 0 - няма нови файлове
	 *	-1 грешка при формиране на нотифиакция
	 *  -2 няма водещ експерт на жалбата или ръководителя на звеното, на което е разпределена
	 */
	 public void actionNotifFilesJ() {	
		 try {
			 
			 JPA.getUtil().begin();
			 int result = new DocJalbaDAO(getUserData()).notifAddFilesInDoc(document.getId(), sd);
			 JPA.getUtil().commit();
			 
			 if (result > 0 ) {
				 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "jp.sentNotifFiles") );		
			 }	else if(result == 0){
				 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString(beanMessages, "jp.notSentNotifNoFiles") );
			 } else if(result == -2){
				 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString(beanMessages, "jp.notSentNotifFiles") );
			 }else if(result == -1){
				 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString(beanMessages, "jp.errSentNotifFiles") );
			 }

		} catch (Exception e) {
			JPA.getUtil().rollback();

			LOGGER.error("Грешка при нотификация за нови файлове", e); 
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		
		} finally {
			JPA.getUtil().closeConnection();
		}
	 }
	 
	 /**
	  * download на файлове от Справка - жалба
	  * @param files
	  */
	public void download(Integer idFile) { 
		docDataBean.download(idFile);
	}

	public List<SystemClassif> getVidResultList() {
		return vidResultList;
	}

	public void setVidResultList(List<SystemClassif> vidResultList) {
		this.vidResultList = vidResultList;
	}
	
	public List<SystemClassif> getVidOplList() {
		return vidOplList;
	}

	public void setVidOplList(List<SystemClassif> vidOplList) {
		this.vidOplList = vidOplList;
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


	public int getUpdateDoc() {
		return DocData.UPDATE_DOC; 
	}



	public String getRnFullDocEdit() {
		return rnFullDocEdit;
	}


	public void setRnFullDocEdit(String rnFullDocEdit) {
		this.rnFullDocEdit = rnFullDocEdit;
	}



	public boolean isScanModuleExist() {
		return scanModuleExist;
	}



	public void setScanModuleExist(boolean scanModuleExist) {
		this.scanModuleExist = scanModuleExist;
	}



	public List<Files> getFilesList() {
		return filesList;
	}



	public void setFilesList(List<Files> filesList) {
		this.filesList = filesList;
	}



	public String getRnFullProtocol() {
		return rnFullProtocol;
	}



	public void setRnFullProtocol(String rnFullProtocol) {
		this.rnFullProtocol = rnFullProtocol;
	}



	public Date getDecodeDate() {
		return decodeDate;
	}



	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}
	
	
	public DocData getDocDataBean() {
		return docDataBean;
	}



	public void setDocDataBean(DocData docDataBean) {
		this.docDataBean = docDataBean;
	}



	public Doc getDocument() {
		return document;
	}



	public void setDocument(Doc document) {
		this.document = document;
	}



	public DocDAO getDao() {
		return dao;
	}



	public void setDao(DocDAO dao) {
		this.dao = dao;
	}



	public SystemData getSd() {
		return sd;
	}



	public void setSd(SystemData sd) {
		this.sd = sd;
	}



	public UserData getUd() {
		return ud;
	}



	public void setUd(UserData ud) {
		this.ud = ud;
	}


	public Integer getDocVidInclude() {
		return docVidInclude;
	}



	public void setDocVidInclude(Integer docVidInclude) {
		this.docVidInclude = docVidInclude;
	}



	public String getTxtNar() {
		return txtNar;
	}



	public void setTxtNar(String txtNar) {
		this.txtNar = txtNar;
	}





	public List<SystemClassif> getOsnNeDopustList() {
		return osnNeDopustList;
	}



	public void setOsnNeDopustList(List<SystemClassif> osnNeDopustList) {
		this.osnNeDopustList = osnNeDopustList;
	}





	public boolean isJbpHiddenB() {
		return jbpHiddenB;
	}



	public void setJbpHiddenB(boolean jbpHiddenB) {
		this.jbpHiddenB = jbpHiddenB;
	}


	public boolean isCorruptionB() {
		return corruptionB;
	}



	public void setCorruptionB(boolean corruptionB) {
		this.corruptionB = corruptionB;
	}



	public boolean isInstCheckB() {
		return instCheckB;
	}



	public void setInstCheckB(boolean instCheckB) {
		this.instCheckB = instCheckB;
	}


	public String getIdentJbp() {
		return identJbp;
	}


	public void setIdentJbp(String identJbp) {
		this.identJbp = identJbp;
	}
	
	public Map<Integer, Object> getSpecificsEKATTE() {
		if(specificsEKATTE == null) {
			specificsEKATTE = Collections.singletonMap(SysClassifAdapter.EKATTE_INDEX_TIP, 3);
		}
		return specificsEKATTE;
	}

	public void setSpecificsEKATTE(Map<Integer, Object> specificsEKATTE) {
		this.specificsEKATTE = specificsEKATTE;
	}
	


	public List<SystemClassif> getScDopExpertsList() {
		return scDopExpertsList;
	}


	public void setScDopExpertsList(List<SystemClassif> scDopExpertsList) {
		this.scDopExpertsList = scDopExpertsList;
	}


	public String getTxtNarCategVid() {
		if(this.txtNarCategVid == null) {
			loadDopInfoNar();
		}
		return txtNarCategVid;
	}

	public void setTxtNarCategVid(String txtNarCategVid) {
		this.txtNarCategVid = txtNarCategVid;
	}



	
	public List<SelectItem> getSastList() {
		if(sastList == null) {
			try {
				sastList = docDataBean.createItemsListLogicalVod(OmbConstants.CODE_LIST_JALBA_SAST_NEW_SAST, sastLast,  new Date(), false);
			} catch (DbErrorException | UnexpectedResultException e) {
				LOGGER.error("Грешка при зареждане на списъка със състояния на жалба", e);				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG),e.getMessage());
			} 
		}
		return sastList;
	}

	public void setSastList(List<SelectItem> sastList) {
		this.sastList = sastList;
	}

	public LazyDataModelSQL2Array getDocPreviosList() {
		return docPreviosList;
	}

	public void setDocPreviosList(LazyDataModelSQL2Array docPreviosList) {
		this.docPreviosList = docPreviosList;
	}

	public List<Object[]> getSelPreviosJalbi() {
		if(selPreviosJalbi == null) {
			selPreviosJalbi =  new ArrayList<>();
		}
		return selPreviosJalbi;
	}

	public void setSelPreviosJalbi(List<Object[]> selPreviosJalbi) {
		this.selPreviosJalbi = selPreviosJalbi;
	}

	public List<Object[]> getSelPreviosJalbiAll() {
		if(selPreviosJalbiAll == null) {
			selPreviosJalbiAll =  new ArrayList<>();
		}		
		return selPreviosJalbiAll;
	}

	public void setSelPreviosJalbiAll(List<Object[]> selPreviosJalbiAll) {
		this.selPreviosJalbiAll = selPreviosJalbiAll;
	}

	public Integer getSastLast() {
		return sastLast;
	}

	public void setSastLast(Integer sastLast) {
		this.sastLast = sastLast;
	}

	public boolean isWarnDateNar() {
		return warnDateNar;
	}

	public void setWarnDateNar(boolean warnDateNar) {
		this.warnDateNar = warnDateNar;
	}

	public Integer getIdPrevJ() {
		return idPrevJ;
	}

	public void setIdPrevJ(Integer idPrevJ) {
		this.idPrevJ = idPrevJ;
	}

	public List<Object[]> getSpravkaList() {
		return spravkaList;
	}

	public void setSpravkaList(List<Object[]> spravkaList) {
		this.spravkaList = spravkaList;
	}

	public Integer getRowOrgan() {
		return rowOrgan;
	}

	public void setRowOrgan(Integer rowOrgan) {
		this.rowOrgan = rowOrgan;
	}

	public Map<Integer, Object> getSpecificsAdm() {
		if(specificsAdm == null && docDataBean != null) {
			specificsAdm =  docDataBean.getSpecificsAdm();
		}
		return specificsAdm;
	}

	public void setSpecificsAdm(Map<Integer, Object> specificsAdm) {
		this.specificsAdm = specificsAdm;
	}

	public List<Object[]> getSastHistoryList() {
		return sastHistoryList;
	}

	public void setSastHistoryList(List<Object[]> sastHistoryList) {
		this.sastHistoryList = sastHistoryList;
	}

	public String getNedopNarMsgWarn() {
		return nedopNarMsgWarn;
	}

	public void setNedopNarMsgWarn(String nedopNarMsgWarn) {
		this.nedopNarMsgWarn = nedopNarMsgWarn;
	}

	public String getfSize() {
		return fSize; //max-height: 350px; overflow: auto;
	}

	public void setfSize(String fSize) {
		this.fSize = fSize;
	}

	public boolean isZvenoLiderJ() {
		return zvenoLiderJ;
	}

	public void setZvenoLiderJ(boolean zvenoLiderJ) {
		this.zvenoLiderJ = zvenoLiderJ;
	}

	public boolean isFullAccessJ() {
		return fullAccessJ;
	}

	public void setFullAccessJ(boolean fullAccessJ) {
		this.fullAccessJ = fullAccessJ;
	}


	public boolean isExpertLider() {
		return expertLider;
	}


	public void setExpertLider(boolean expertLider) {
		this.expertLider = expertLider;
	}


	public boolean isUserRegJalba() {
		return userRegJalba;
	}


	public void setUserRegJalba(boolean userRegJalba) {
		this.userRegJalba = userRegJalba;
	}

	public Integer getPravaPrev() {
		return pravaPrev;
	}

	public void setPravaPrev(Integer pravaPrev) {
		this.pravaPrev = pravaPrev;
	}

	public boolean isEditSpecFields() {
		return editSpecFields;
	}

	public void setEditSpecFields(boolean editSpecFields) {
		this.editSpecFields = editSpecFields;
	}

	public boolean isCompetence() {
		return competence;
	}

	public void setCompetence(boolean competence) {
		this.competence = competence;
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

	public List<SystemClassif> getZvenoSlList() {
		return zvenoSlList;
	}

	public void setZvenoSlList(List<SystemClassif> zvenoSlList) {
		this.zvenoSlList = zvenoSlList;
	}
	
	/**
	 * да се позволи ли корекция на засегнати права
	 * @return
	 */
	public boolean getPravaCh() {
		boolean bb = false;
		if( document.getJalba().getCodeExpert() != null) {
			boolean bb1 = document.getJalba().getZasPrava() != null; 
			boolean bb2 = !(fullAccessJ || zvenoLiderJ || Objects.equals(document.getJalba().getCodeExpert(), ud.getUserId())); 
			if(bb1 && bb2 ){
				bb = true; // zabranqwa se korekciqta!.
			}
		}
		return bb;
	}

	public Integer getCountPorodeni() {
		return countPorodeni;
	}

	public void setCountPorodeni(Integer countPorodeni) {
		this.countPorodeni = countPorodeni;
	}
	
   private Integer reloadFile;
   
   public Integer getReloadFile() {
		return reloadFile;
	}
   
   
   //метода се извиква от компонентата за сканиране след успешно извършен запис на файл
   public void setReloadFile(Integer reloadFile) {
		
	//	System.out.println("setReloadFile ->"+reloadFile);
		
		if(reloadFile!=null) {
			DocData bean = getDocDataBean();
			bean.reloadDocDataFile(); 
			bean.actionChangeFiles();
			setFilesList(bean.getFilesList());
		}
		
		this.reloadFile = reloadFile;
	}
}
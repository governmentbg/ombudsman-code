package com.ib.omb.beans;
import static com.ib.system.SysConstants.CODE_CLASSIF_EKATTE;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.event.Event;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.faces.view.facelets.FaceletContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.internet.MimeUtility;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aspose.words.Document;
import com.aspose.words.DocumentBuilder;
import com.aspose.words.License;
import com.aspose.words.SaveFormat;
import com.ib.indexui.SelectManyModalA;
import com.ib.indexui.SelectOneModalA;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.DocDvijDAO;
import com.ib.omb.db.dao.DocJalbaDAO;
import com.ib.omb.db.dao.EgovMessagesDAO;
import com.ib.omb.db.dao.ReferentDAO;
import com.ib.omb.db.dao.RegistraturaDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocDvij;
import com.ib.omb.db.dto.DocJalba;
import com.ib.omb.db.dto.Referent;
import com.ib.omb.db.dto.ReferentAddress;
import com.ib.omb.db.dto.Registratura;
import com.ib.omb.export.BaseExport;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectNotFoundException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.ValidationUtils;
import com.lowagie.text.pdf.Barcode128;

/**
 * Актуализация на документ, таб Движения
 *
 * @author n.kanev
 */
@Named("docDataDvijenia")
@ViewScoped
public class DocDataDvijenia extends IndexUIbean implements Serializable {
	
	private static final long serialVersionUID = 55031952536385790L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocDataDvijenia.class);
	private static final int TYPE_SOME_EMAIL = -2;
	private static final int TYPE_SOME_KORESP = -3;
	private static final int TYPE_ADM_STR_ZVENO = -4;
	private static final int TYPE_ADM_STR_LICE = -5;
	private static final int TYPE_SEOS_KORESP = -6;
	private static final int TYPE_SSEV_EGN = -7;
	private static final int TYPE_SSEV_EIK = -8;
	private static final String ERROR_KLASSIF = 		"general.errorClassif";
	private static final String NEW_RETURN_TO_DATE = 	"docForm:tabsDoc:calendar-vrashtane";
	private static final String NEW_SEND_DATE = 		"docForm:tabsDoc:calendar-predavane";
	private static final String NEW_EMAIL = 			"docForm:tabsDoc:input-email";
	private static final String NEW_EGN = 				"docForm:tabsDoc:input-egn";
	private static final String NEW_EIK = 				"docForm:tabsDoc:input-eik";
	private static final String RETURN_RETURN_DATE = 	"docForm:tabsDoc:calendar-return";
	private static final String RETURN_TEXT_KORESP = 	"docForm:tabsDoc:selectTextReturn";
	private static final String RETURN_ADM_STR = 		"docForm:tabsDoc:selectAdmStrReturn:аutoCompl_input";
	private static final String RETURN_KORESP = 		"docForm:tabsDoc:selectKorespReturn:аutoCompl_input";
	private static final String EDIT_SEND_DATE = 		"docForm:tabsDoc:calendar-edit-predavane";
	private static final String EDIT_RETURN_TO_DATE = 	"docForm:tabsDoc:calendar-edit-vrashtane-do";
	private static final String EDIT_ADM_STR = 			"docForm:tabsDoc:selectAdmStrEdit1:аutoCompl_input";
	private static final String EDIT_KORESP = 			"docForm:tabsDoc:selectKorespEdit1:аutoCompl_input";
	private static final String EDIT_REFERENT_TEKST = 	"docForm:tabsDoc:selectTextEdit1";
	private static final String EDIT_RETURN_DATE = 		"docForm:tabsDoc:calendar-edit-vrashtane";
	private static final String EDIT_EXEMPLAR = 		"docForm:tabsDoc:input-edit-exemplar";
	private static final String BUTTON_SAVE_NEW = 		"docForm:tabsDoc:btnSaveNewDvij";
	
	// Print Post covers	
	private static final String VERIFY_COR_NAME = 		"docForm:tabsDoc:dialog-env-print:corName";
	private static final String VERIFY_SEND_NAME = 		"docForm:tabsDoc:dialog-env-print:sendName";
	private static final String VERIFY_COR_ADR = 		"docForm:tabsDoc:dialog-env-print:corAdr";
	private static final String VERIFY_SEND_ADR = 		"docForm:tabsDoc:dialog-env-print:sendAdr";
	private static final String VERIFY_COR_OBL = 		"docForm:tabsDoc:dialog-env-print:corObl";
	private static final String VERIFY_SEND_OBL = 		"docForm:tabsDoc:dialog-env-print:sendObl";
//	private static final String VERIFY_COR_PC = 		"docForm:tabsDoc:dialog-env-print:corPC";
	private static final String VERIFY_SEND_PC = 		"docForm:tabsDoc:dialog-env-print:sendPC";
	private static final String VERIFY_COR_NM = 		"docForm:tabsDoc:dialog-env-print:corNM";
	private static final String VERIFY_SEND_NM = 		"docForm:tabsDoc:dialog-env-print:sendNM";
//	private static final String VERIFY_COR_COUNTRY = 	"docForm:tabsDoc:dialog-env-print:corCountr";
//	private static final String VERIFY_SEND_COUNTRY = 	"docForm:tabsDoc:dialog-env-print:sendCountr";
	
	@Inject
	Event<DocDvij> dvijEvent;
	private Doc document;
	private String rnFullDoc;
	private Integer isView;
	
	private List<SelectItem> predavaneTypeList;
	private transient DocDvijDAO dvijenieDao;
	private List<DocDvij> dvijenia; // досегашните движения, измъкнати от базата
	private transient List<Dvijenie> newDvijenia; // новите незаписани движения в таблицата
	private DocDvij tempDvij;
	private Map<Integer, Date> copyDate;
	private Date backupDateVrashtane;
	private SystemData systemData;
	private boolean withExemplars;
	private int newKorespType;
	private List<Object[]> zaOtgovor;
	private Object[] jalbaDanni;
	
	private boolean viewNewPanel = false;
	private boolean viewEditPanel = false;
	private boolean viewReturnPanel = false;
	private Date dataPredavane;
	private Date dataVrashtane;
	private int newMetod;
	private String newEmailText;
	private String newEmailName;
	private String newKorespText;
	private String newPredavaneInfo;
	private Integer newPredavaneEkz;
	private String newEgn;
	private String newEik;
	private List<Integer> selectedAdmStruct = new ArrayList<>();
	private Integer selectedGroupSluj;
	private Integer selectedKoresp;
	private Integer editedKoresp;
	private Integer selectedGroupKoresp;
	private Integer selectedGroupKorespSeos;
	private List<Integer> selectedSeos = new ArrayList<>();
	private List<Integer> selectedAdmOrgan = new ArrayList<>();
	private transient SelectOneModalA bindGrupiSluj;
	private transient SelectOneModalA bindGrupiKoresp;
	private transient SelectOneModalA bindGrupiKorespSeos;
	private transient SelectManyModalA bindAdmStruct;
	private transient SelectOneModalA bindEikName;
	private transient SelectManyModalA bindSeos;
	private transient SelectManyModalA bindSsev;
	private String[] informationText = new String[2];
	private String seosError;
	private int countOfficalFiles; // брой файлове за изпращане
	private boolean displayEditQuestion;
	
	private Map<Integer, Object> specGroup;
	private Map<Integer, Object> specGroupSeos;
	
	private List<SystemClassif> napusnaliAdmStrukt;
	
	// Print envelopes / Deliver Notice
	private ReferentAddress adr=null;
	private Referent ref=null;
	private Integer formatPlik=null;
	private boolean recommended = false;
	private String corespName=null;
	private String corespTel=null;
	private String corespAddress=null;
	private String corespPostCode=null; 
	private String corespPBox=null;
	private String corespObl=null;
	private String corespNM=null;
	
	private String senderName=null;
	private String senderTel=null;
	private String senderAddress=null;
	private String senderPostCode=null; 
	private String senderPBox=null;
	private String senderObl=null;
	private String senderNM=null;
	private Integer countryBg=null;
	private String corespCountry=null;
	private String senderCountry=null;
	private Integer idRegistratura = null;
	
	private Integer idRefModal = null; //използва се в модалния за разглеждане на данни на физическо/юрид. лице
	private Map<Integer, Object> specRegistratura; // специфика за група служители - да се филтрират по регистратура	- задължително да се подаде sortByName="true"
	
//	private int envNotice;
//	private boolean viewPlik=false;
	
	

	/**
	 * При отваряне на таба - винаги минава от тук
	 */
	public void initTab() {
		DocData bean = (DocData) JSFUtils.getManagedBean("docData");
		FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
		String param = (String) faceletContext.getAttribute("isView"); // 0 - актуализациял 1 - разглеждане
		this.isView = 0;
		if(!SearchUtils.isEmpty(param)) {
			this.isView = Integer.valueOf(param);
		}
		this.systemData = (SystemData) getSystemData();
		if(bean != null) {
			this.specRegistratura = Collections.singletonMap(OmbClassifAdapter.REG_GROUP_INDEX_REGISTRATURA, ((UserData) getUserData()).getRegistratura());
			this.specGroup  = new HashMap<>();
			specGroup.putAll(specRegistratura);
			specGroup.put(OmbClassifAdapter.REG_GROUP_INDEX_TYPE, OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP);
			
			this.specGroupSeos = Collections.singletonMap(OmbClassifAdapter.REG_GROUP_INDEX_TYPE, OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS);
			this.countOfficalFiles = bean.getCountOfficalFiles(); 
			
			this.document = bean.getDocument(); // смятам че всеки път трябва да се дава реалното състояние на документа,
												// защото след корекция на док се подменя инстанцията и тука ще остане старата
			
			if(!Objects.equals(bean.getRnFullDoc(), this.rnFullDoc)) { // значи за 1-ви път се отваря табчето 
				
				this.rnFullDoc = bean.getRnFullDoc();
				this.newDvijenia = new ArrayList<>();
				this.dvijenieDao = new DocDvijDAO(getUserData());
				
				try {
					String param1 = this.systemData.getSettingsValue("delo.docWithExemplars");
					this.withExemplars = Objects.equals(param1, String.valueOf(OmbConstants.CODE_ZNACHENIE_DA));
				} catch(DbErrorException e) {
					LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
					this.withExemplars = false;
				}
				
				try {
					loadDvijenia();
					
					// допълнителни данни в случай, че документът е класиран в преписка от тип „преписка по жалба“
					this.jalbaDanni = this.dvijenieDao.findDocJalbaData(this.document.getId());
					
					// разглеждане от менюто Справки
					if(this.isView == 1) {
						this.newMetod = OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL;
						
						this.predavaneTypeList = // допуща се само имейл
							createItemsList(true, OmbConstants.CODE_CLASSIF_DVIJ_METHOD, new Date(), true)
							.stream()
							.filter(item -> (Integer)item.getValue() == OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL)
							.collect(Collectors.toList());
					}
					// актуализация
					else {
						this.predavaneTypeList =
							createItemsList(true, OmbConstants.CODE_CLASSIF_DVIJ_METHOD, new Date(), true)
							.stream()
							.filter(item -> (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_WEB_EAU)
							.collect(Collectors.toList());
						
						// СЕОС да се вижда само за Входящи и Собствени документи
						if((this.document.getDocType() != OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN && this.document.getDocType() != OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN)
								|| Integer.valueOf(this.systemData.getSettingsValue("system.useSEOS")) != OmbConstants.CODE_ZNACHENIE_DA) {
							this.predavaneTypeList.removeIf(item -> (Integer)item.getValue() == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS);
						}
							
						// ССЕВ да се вижда само за Входящи и Собствени документи
						if((this.document.getDocType() != OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN && this.document.getDocType() != OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN)
								|| Integer.valueOf(this.systemData.getSettingsValue("system.useSSEV")) != OmbConstants.CODE_ZNACHENIE_DA) {
							this.predavaneTypeList.removeIf(item -> (Integer)item.getValue() == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV);
						}
					}
					
					// проверява се изрично дали има права да праща по имейл
					if(!getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_SEND_MAIL)) {
						this.predavaneTypeList.removeIf(item -> (Integer)item.getValue() == OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL);
					}
						
				} catch (DbErrorException | UnexpectedResultException e) {
					LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
				}
			}
		}
	}
	
	/**
	 * Зарежда движенията от базата. public, понеже се вика и от страницата
	 */
	public void loadDvijenia() {
		try {
			JPA.getUtil().runWithClose(() ->
				this.dvijenia = this.dvijenieDao.getDocDvij(this.document.getId()));
		} catch (BaseException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaZarejdaneDvijenia"), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}
	
	/**
	 * Кликнат е бутонът 'Ново предаване'.
	 * Показва панела за въвеждане.
	 */
	public void actionNew() {
		this.viewNewPanel = true;
		this.newDvijenia = new ArrayList<>();
		this.viewEditPanel = false;
		this.viewReturnPanel = false;
		this.displayEditQuestion = true;
		setInitValues();
	}
	
	/**
	 * Избрана е 'Актуализация' на вече записано движение в таблицата
	 * @param index индексът на реда
	 */
	public void actionBeginEdit(int index) {
		this.tempDvij = getDvijenieCopy(index);
		this.backupDateVrashtane = tempDvij.getReturnDate();
		this.viewEditPanel = true;
		this.viewNewPanel = false;
		this.viewReturnPanel = false;
	}
	
	/**
	 * Избран е 'Запис' на актуализацията
	 */
	public void actionSaveEdit() {
		
		if(!validateEdit()) return;
		
		DocDvij dvij = this.dvijenia.stream().filter(a -> a.getId().equals(this.tempDvij.getId())).findFirst().orElse(null);
		if(dvij == null) return;
		
		// редактирани са само данните за предаване
		if(dvij.getReturnDate() == null) {
						
			dvij.setDvijDate(this.tempDvij.getDvijDate());
			dvij.setReturnToDate(this.tempDvij.getReturnToDate());
			
			dvij.setDvijMethod(this.tempDvij.getDvijMethod());
			dvij.setCodeRef(this.tempDvij.getCodeRef());
			dvij.setDvijInfo(this.tempDvij.getDvijInfo() != null ? this.tempDvij.getDvijInfo().trim() : null);
			dvij.setDvijEmail(this.tempDvij.getDvijMethod() == OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL ? this.tempDvij.getDvijEmail() : null);
			dvij.setEkzNomer(this.tempDvij.getEkzNomer());
			
			if(this.tempDvij.getCodeRef() == null) {
				if(this.tempDvij.getDvijText() != null)	{
					dvij.setDvijText(this.tempDvij.getDvijText().trim());
				}
			}
			else {
				try {
					SystemClassif classif = null;
					if(isAdmStr(this.tempDvij.getCodeRef())) {
						classif = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, this.tempDvij.getCodeRef(), SysConstants.CODE_DEFAULT_LANG, new Date(), true);
					}
					else {
						classif = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_REFERENTS, this.tempDvij.getCodeRef(), SysConstants.CODE_DEFAULT_LANG, new Date(), true);
					}
					dvij.setDvijText(classif.getTekst());
				} catch (DbErrorException | NullPointerException e) {
					LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
				}
			}
			
			// ако междувременно е маркирано за изпращане по компетенстност, при записа да се смени статуса
			if (this.document != null && this.document.getCompetence() != null && this.document.getCompetence().equals(OmbConstants.CODE_ZNACHENIE_COMPETENCE_FOR_SEND)){
				this.document.setCompetence(OmbConstants.CODE_ZNACHENIE_COMPETENCE_SENT);
				try {
					JPA.getUtil().runInTransaction(() -> updateCompetenceDoc(OmbConstants.CODE_ZNACHENIE_COMPETENCE_SENT, dvij));
				} catch (BaseException e) {
					LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaAktualiziranePredavane"), e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
				}
			}
			
		}
		// редактирани са само данните за връщане
		else {
			dvij.setReturnDate(this.tempDvij.getReturnDate());
			dvij.setReturnMethod(this.tempDvij.getReturnMethod());
			dvij.setReturnInfo(this.tempDvij.getReturnInfo());
		}
		
		try {
			JPA.getUtil().runInTransaction(() -> this.dvijenieDao.save(dvij, this.systemData, this.document.getRegistraturaId()));
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dvijenie.redakciaZapisana"));
			loadDvijenia();
		} catch (BaseException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaAktualiziranePredavane"), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}

		this.viewEditPanel = false;

	}
	
	/**
	 * Валидира дали актуализацията е попълнена с правилни данни при кликване на 'Запис'
	 */
	private boolean validateEdit() {
		
		Date docDate = this.document.getDocDate();
		Date predavane = this.tempDvij.getDvijDate();
		Date vrashtaneDo = this.tempDvij.getReturnToDate();
		Date vrashtane = this.tempDvij.getReturnDate();
		Date dnes = new Date();
		
		DocDvij dvij = this.dvijenia.stream().filter(a -> a.getId().equals(this.tempDvij.getId())).findFirst().orElse(null);
		if(dvij == null) return false;
		
		// редактирани са само данните за предаване
		if(dvij.getReturnDate() == null) {
			// валидиране на датите
			if(predavane == null) {
				JSFUtils.addMessage(EDIT_SEND_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavaneZadaljitelna"));
				scrollToMessages();
				return false;
			}
			
			if(predavane.compareTo(docDate) < 0) {
				JSFUtils.addMessage(EDIT_SEND_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavanePrediDokument"));
				scrollToMessages();
				return false;
			}
			
			if(dnes.compareTo(predavane) < 0) {
				JSFUtils.addMessage(EDIT_SEND_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavaneBadeshta"));
				scrollToMessages();
				return false;
			}
			
			if(vrashtaneDo != null && vrashtaneDo.compareTo(predavane) < 0) {
				JSFUtils.addMessage(EDIT_RETURN_TO_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneDoRanna"));
				scrollToMessages();
				return false;
			}
			
			if(this.tempDvij.getEkzNomer() == null) {
				JSFUtils.addMessage(EDIT_EXEMPLAR, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "docDvij.exemplarZadaljitelen"));
				scrollToMessages();
				return false;
			}
			
			if(this.tempDvij.getEkzNomer() < 1) {
				JSFUtils.addMessage(EDIT_EXEMPLAR, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "docDvij.nevalidenExemplar"));
				scrollToMessages();
				return false;
			}
			
			Integer oldCodeRef = dvij.getCodeRef();
			// трябва да има попълнен код на референт, но няма
			if(this.tempDvij.getCodeRef() == null && oldCodeRef != null) {
				
				String componentId = "";
				if(isAdmStr(oldCodeRef)) componentId = EDIT_ADM_STR;
				else if(isKoresp(oldCodeRef)) componentId = EDIT_KORESP;
				
				JSFUtils.addMessage(componentId, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.poleZadaljitelno"));
				this.tempDvij.setCodeRef(oldCodeRef);
				scrollToMessages();
				return false;
			}
			// трябва да има попълнен текст на референт, но няма
			else if(this.tempDvij.getCodeRef() == null 
					&& (this.tempDvij.getDvijText() == null || this.tempDvij.getDvijText().trim().isEmpty())
					&& this.tempDvij.getDvijEmail() == null) {
				JSFUtils.addMessage(EDIT_REFERENT_TEKST, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.poleZadaljitelno"));
				scrollToMessages();
				return false;
			}
		}
		
		// редактирани са само данните за връщане
		else {
			if(vrashtane == null) {
				JSFUtils.addMessage(EDIT_RETURN_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneZadaljitelna"));
				this.tempDvij.setReturnDate(this.backupDateVrashtane);
				scrollToMessages();
				return false;
			}
			
			if(dnes.compareTo(vrashtane) < 0) {
				JSFUtils.addMessage(EDIT_RETURN_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneBadeshta"));
				scrollToMessages();
				return false;
			}
			
			if(vrashtane.compareTo(predavane) < 0) {
				JSFUtils.addMessage(EDIT_RETURN_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneRanna"));
				scrollToMessages();
				return false;
			}
			// не се валидират референтите, защото не се променят
		}
		
		return true;
	}
	
	/**
	 * Натиснат е бутонът 'Изтриване на връщане'
	 */
	public void actionDeleteReturn() {
		DocDvij dvij = this.dvijenia.stream().filter(a -> a.getId().equals(this.tempDvij.getId())).findFirst().orElse(null);
		if(dvij == null) return;
		
		dvij.setReturnDate(null);
		dvij.setReturnCodeRef(null);
		dvij.setReturnTextRef(null);
		dvij.setReturnMethod(null);
		dvij.setReturnInfo(null);
		
		try {
			JPA.getUtil().runInTransaction(() -> this.dvijenieDao.save(dvij, this.systemData, this.document.getRegistraturaId()));
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dvijenie.danniVrashtaneIztriti"));
			loadDvijenia();
		} catch (BaseException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaIztrivaneVrashtane"), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			scrollToMessages();
		}
		
		this.viewEditPanel = false;
	}
	
	/**
	 * Избран е 'Отказ' на актуализацията
	 */
	public void actionCancelEdit() {
		this.viewEditPanel = false;
	}
	
	/**
	 * Избрана е 'Връщане' на вече записано движение в таблицата
	 * @param index индексът на реда
	 */
	public void actionBeginReturn(int index) {
		
		this.tempDvij = getDvijenieCopy(index);
		
		// настройки по подразбиране
		this.tempDvij.setReturnDate(new Date());
		this.tempDvij.setReturnMethod(this.tempDvij.getDvijMethod());
		this.tempDvij.setReturnCodeRef(this.tempDvij.getCodeRef());
		this.tempDvij.setReturnTextRef(this.tempDvij.getDvijText());
		
		this.viewReturnPanel = true;
		this.viewNewPanel = false;
		this.viewEditPanel = false;
	}
	
	/**
	 * Избран е 'Запис' на връщането
	 */
	public void actionSaveReturn() {
		
		if(!validateReturn()) return;
		
		DocDvij dvij = this.dvijenia.stream().filter(a -> a.getId().equals(this.tempDvij.getId())).findFirst().orElse(null);
		if(dvij == null) return;
		
		dvij.setReturnDate(this.tempDvij.getReturnDate());
		dvij.setReturnMethod(this.tempDvij.getReturnMethod());
		dvij.setReturnCodeRef(this.tempDvij.getReturnCodeRef());
		dvij.setReturnInfo(this.tempDvij.getReturnInfo());

		if(this.tempDvij.getCodeRef() == null) {
			dvij.setReturnTextRef(this.tempDvij.getReturnTextRef().trim());
		}
		else {
			try {
				SystemClassif classif = null;
				if(isAdmStr(this.tempDvij.getReturnCodeRef())) {
					classif = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, this.tempDvij.getReturnCodeRef(), SysConstants.CODE_DEFAULT_LANG, new Date(), true);
				}
				else {
					classif = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_REFERENTS, this.tempDvij.getReturnCodeRef(), SysConstants.CODE_DEFAULT_LANG, new Date(), true);
				}
				dvij.setReturnTextRef(classif.getTekst());
			} catch (DbErrorException | NullPointerException e) {
				LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
				scrollToMessages();
			}
		}
		
		try {
			JPA.getUtil().runInTransaction(() -> this.dvijenieDao.save(dvij, this.systemData, this.document.getRegistraturaId()));
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dvijenie.vrashtaneZapisano"));
			loadDvijenia();
		} catch (BaseException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaZapisVrashtane"), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		
		this.viewReturnPanel = false;
	}
	
	/**
	 * Валидира дали връщането е попълнено с правилни данни при кликване на 'Запис'
	 */
	private boolean validateReturn() {
		// валидиране на датата
		if(this.tempDvij.getReturnDate() == null) {
			JSFUtils.addMessage(RETURN_RETURN_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneZadaljitelna"));
			return false;
		}
		
		Date predavane = this.tempDvij.getDvijDate();
		Date vrashtane = this.tempDvij.getReturnDate();
		Date dnes = new Date();
		
		if(vrashtane.compareTo(predavane) < 0) {
			JSFUtils.addMessage(RETURN_RETURN_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneRanna"));
			scrollToMessages();
			return false;
		}
		if(dnes.compareTo(vrashtane) < 0) {
			JSFUtils.addMessage(RETURN_RETURN_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneBadeshta"));
			scrollToMessages();
			return false;
		}
		
		// трябва да има попълнен код на референт, но няма
		if(this.tempDvij.getCodeRef() != null && this.tempDvij.getReturnCodeRef() == null) {
			
			String componentId = "";
			if(isAdmStr(this.tempDvij.getCodeRef())) componentId = RETURN_ADM_STR;
			else if(isKoresp(this.tempDvij.getCodeRef())) componentId = RETURN_KORESP;
			
			JSFUtils.addMessage(componentId, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.poleZadaljitelno"));
			scrollToMessages();
			return false;
		}
		// трябва да има попълнен текст на референт, но няма
		else if(this.tempDvij.getCodeRef() == null 
				&& (this.tempDvij.getReturnTextRef() == null || this.tempDvij.getReturnTextRef().trim().isEmpty())) {
			JSFUtils.addMessage(RETURN_TEXT_KORESP, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.poleZadaljitelno"));
			scrollToMessages();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Избран е 'Отказ' на връщането
	 */
	public void actionCancelReturn() {
		this.viewReturnPanel = false;
	}
	
	/**
	 * При клик на бутона 'Запис' се вика това, то при нужда показва ConfirmDialog или вика окончателно запис
	 */
	public void actionSaveNewClicked() {
		if(this.countOfficalFiles == 0) {
			if(this.newDvijenia.stream().anyMatch(
					d -> d.metodPredavane == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS
						|| d.metodPredavane == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV
						|| d.metodPredavane == OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL)) {
				JSFUtils.addMessage(BUTTON_SAVE_NEW, FacesMessage.SEVERITY_ERROR, 
									getMessageResourceString(beanMessages, "dvijenie.predavaneNeIzvarsheno"),
									getMessageResourceString(beanMessages, "docDvij.niamaFailove"));
				scrollToMessages();
			}
			
			else if(this.newDvijenia.stream().anyMatch(d -> d.metodPredavane == OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA)) {
				PrimeFaces.current().executeScript("PF('hiddenButton').jq.click();");
			}
			
			else {
				actionSaveNewDvij();
			}
		}
		else {
			actionSaveNewDvij();
		}
	}
	
	/**
	 * Кликнат е бутонът 'Запис' и е минал успешно методът actionSaveNewClicked() или е потвърдено изпращането без файлове.
	 * Записва новите предавания.
	 * Скрива панела за въвеждане.
	 */
	public void actionSaveNewDvij() {
		
		if(this.useExemplar()) {
			boolean invalid = this.newDvijenia.stream().anyMatch(dvij -> dvij.exemplar == null);
			if(invalid) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "docDvij.exemplarZadaljitelenNavsiakade"));
				scrollToMessages();
				return;
			}
		}
		
		try {
			int newDvijSaved = 0;
			DocDvij first = null; // първото с което ще се журналира за смяна на данни в док (ако има нужда)
			for(Dvijenie d : this.newDvijenia) {
				
				// проверка дали вече има движение на този документ към другата регистратура, което да е 'регистрирано' или да 'чака регистрация'
				if(this.newMetod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA) {
					SystemClassif empl = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, d.codeReferent, SysConstants.CODE_DEFAULT_LANG, new Date(), true);
					
					Integer drugaRegistratura = (Integer) empl.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA];
					
					boolean alreadySent = this.dvijenieDao.alreadySentToForeignReg(this.document.getId(), drugaRegistratura);
					if(alreadySent) {
						String drugaRegistraturaIme = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_REGISTRATURI, drugaRegistratura, SysConstants.CODE_DEFAULT_LANG, new Date(), true).getTekst();
						JSFUtils.addGlobalMessage(
								FacesMessage.SEVERITY_ERROR,
								String.format(getMessageResourceString(beanMessages, "docDvij.dokumentVechePredadenNaRegistratura"), drugaRegistraturaIme));
						scrollToMessages();
						continue;
					}
				}
				
				DocDvij dvij = new DocDvij();
				dvij.setCodeRef(d.codeReferent); // CODE_REF
				dvij.setDocId(this.document.getId()); // DOC_ID
				dvij.setDvijDate(d.dataPredavane); // DVIJ_DATE
				dvij.setReturnToDate(d.dataVrashtane); //RETURN_TO_DATE
				dvij.setDvijMethod(d.metodPredavane); // DVIJ_METHOD
				dvij.setDvijEmail((d.getMetodPredavane() == OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL 
									|| d.getMetodPredavane() == OmbConstants.CODE_ZNACHENIE_PREDAVANE_MAIL_CUSTOM) ? d.getEmail() : null); // DVIJ_EMAIL
				dvij.setDvijText(d.name); // DVIJ_TEXT
				dvij.setDvijInfo(d.information); // DVIJ_INFO
				dvij.setEkzNomer(d.exemplar);
				dvij.setUchastnikIdent(d.getUchastnikIdent());
				dvij.setUchastnikGuid(d.getUchastnikGuid());
				dvij.setUchastnikName(d.getUchastnikName());
				dvij.setUchastnikType(d.getUchastnikType());
				dvij.setJalbaId(d.getJalbaId());
				
				JPA.getUtil().runInTransaction(() -> this.dvijenieDao.save(dvij, this.systemData, this.document.getRegistraturaId()));
				dvijEvent.fireAsync(dvij); // прихваща се в класа DvijObserver
				
				newDvijSaved++;

				if (first == null) {
					first = dvij;
				}
			}
			
			if (this.document != null && (this.document.getProcessed() == null || this.document.getProcessed().equals(OmbConstants.CODE_ZNACHENIE_NE))){
				this.document.setProcessed(OmbConstants.CODE_ZNACHENIE_DA);
				this.document.setDbProcessed(this.document.getProcessed());
				
				final DocDvij dvij = first;
				JPA.getUtil().runInTransaction(() -> updateProcessedDoc(OmbConstants.CODE_ZNACHENIE_DA, dvij));
			}
			
			if (this.document != null && this.document.getCompetence() != null && this.document.getCompetence().equals(OmbConstants.CODE_ZNACHENIE_COMPETENCE_FOR_SEND)){
				this.document.setCompetence(OmbConstants.CODE_ZNACHENIE_COMPETENCE_SENT);

				final DocDvij dvij = first;
				JPA.getUtil().runInTransaction(() -> updateCompetenceDoc(OmbConstants.CODE_ZNACHENIE_COMPETENCE_SENT, dvij));
			}
			
			// за да не се показва съобщение 'записът успешен', ако нищо ново не е записано 
			// (например ако при пращане към друга рег. е видяло, че вече е изпратено и е дало continue)
			if(newDvijSaved > 0) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dvijenie.zapisUspeshen"));
				loadDvijenia();
			}
			
		} catch(BaseException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaZapisNoviDvij"), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			scrollToMessages();
		}
		
		setInitValues();
		this.viewNewPanel = false;
		this.newDvijenia.clear();
	}

	/**
	 * сменя стойността на колона обработен + журнал
	 */
	private void updateProcessedDoc(int processed, DocDvij dvij) throws DbErrorException {
		JPA.getUtil().getEntityManager().createNativeQuery("update DOC set PROCESSED = ?1 where DOC_ID = ?2")
			.setParameter(1, processed).setParameter(2, this.document.getId()).executeUpdate();
		if (dvij != null) {
			StringBuilder ident = new StringBuilder();
			ident.append("Документ "+this.document.getIdentInfo()+" е маркиран за обработен, при регистриране на предаване (Id="+dvij.getId()+") на документа на ");
			ident.append(dvij.getDvijText() + " на " + DateUtils.printDate(dvij.getDvijDate()) + ".");
			
			SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, this.document.getId(), ident.toString());

			journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
			journal.setDateAction(new Date());
			journal.setIdUser(getUserData().getUserId());

			new DocDAO(getUserData()).saveAudit(journal);
		}
	}

	/**
	 * сменя стойността на колона компететност + журнал
	 */
	private void updateCompetenceDoc(int competence, DocDvij dvij) throws DbErrorException {
		JPA.getUtil().getEntityManager().createNativeQuery("update DOC set COMPETENCE = ?1 where DOC_ID = ?2")
			.setParameter(1, competence).setParameter(2, this.document.getId()).executeUpdate();
		if (dvij != null) {
			StringBuilder ident = new StringBuilder();
			ident.append("Документ "+this.document.getIdentInfo()+" е маркиран за изпратен по компетентност, при регистриране на предаване (Id="+dvij.getId()+") на документа на ");
			ident.append(dvij.getDvijText() + " на " + DateUtils.printDate(dvij.getDvijDate()) + ".");
			
			SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, this.document.getId(), ident.toString());

			journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
			journal.setDateAction(new Date());
			journal.setIdUser(getUserData().getUserId());

			new DocDAO(getUserData()).saveAudit(journal);
		}
	}

	/**
	 * Избран е 'Отказ' на новото движение
	 */
	public void actionCancelNew() {
		this.viewNewPanel = false;
	}

	/**
	 * Задава началните данни на панела и изчиства попълнени неща.
	 */
	private void setInitValues() {
		this.dataPredavane = new Date();
		this.dataVrashtane = null;
		if(this.isView == 1) {
			this.newMetod = OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL;
		}
		else {
			this.newMetod = OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA;
		}
		
		this.newEmailText = "";
		this.newEmailName = "";
		this.newKorespText = "";
		this.newPredavaneInfo = "";
		this.newPredavaneEkz = 1;
		this.newDvijenia.clear();
		this.copyDate = new HashMap<>();
	}
	
	/**
	 * Трие избран референт от таблицата
	 */
	public void actionDeleteNewDvij(int index){
		this.newDvijenia.remove(index);
	}
	
	/**
	 * Нова "копетентност" в документа при изтриване на движение 
	 */
	private Integer newCompetence;
	
	/**
	 * Изтрива вече въведено движение в горната таблица
	 */
	public void actionDeleteDvij(int index) {
		DocDvij d = this.dvijenia.get(index);
		try {
			JPA.getUtil().runInTransaction(() -> newCompetence = this.dvijenieDao.delete(d, this.systemData));
			loadDvijenia();
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dvijenie.iztrito"));
			
			// ако се опитваме да изтрием движение, което в момента е отворено за редакция/връщане, скрива панела 
			if(this.tempDvij != null && this.tempDvij.getId().intValue() == d.getId().intValue()) {
				this.viewEditPanel = false;
				this.viewReturnPanel = false;
			}
			// ако "компетентността" е променена - да сетна новия статус
			if (newCompetence!=null && this.document != null && this.document.getCompetence() != null){
				this.document.setCompetence(newCompetence);
			}
			
		} catch (BaseException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaIztrivane"), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.greshkaIztrivane"));
			scrollToMessages();
		}
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
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV : return "fas fa-shield-alt";
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_MAIL_CUSTOM : return "fas fa-envelope-square";
			default : return "fas fa-location-arrow";
		}
	}
	
	/**
	 * При показване на иконки до метода на предаване
	 * @param methodCode
	 */
	public String getMethodText(int methodCode) {
		return this.predavaneTypeList.stream()
				.filter(s -> (int) s.getValue() == methodCode)
				.map(SelectItem::getLabel)
				.findFirst()
				.orElse("");
	}
	
	/**
	 * При показване на иконки за вида получатели - кореспондент нфл/фзл, потребител от адм. структура, произволен имейл
	 */
	public String getTypeIcon(int type) {
		switch(type) {
			case OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL : return "fas fa-building";
			case OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL : return "fas fa-people-carry";
			case TYPE_SOME_EMAIL: 		return "fas fa-at"; // въведен е ръчно имейл в полето и никой не знае кой стои зад него
			case TYPE_SOME_KORESP: 		return "fas fa-user"; // въведен е ръчно текст в полето
			case TYPE_ADM_STR_LICE: 	return "fas fa-user";
			case TYPE_SSEV_EGN: 		return "fas fa-user";
			case TYPE_SSEV_EIK: 		return "fas fa-building";
			case TYPE_ADM_STR_ZVENO: 	return "fas fa-laptop-house";
			default : 					return "fas fa-user"; // неочакван случай - нито е от адм. стр., нито е кореспондент, нито произволен имейл
		}
	}
	
	/**
	 * Текстът при посочване на иконката за вида получатели - кореспондент нфл/фзл, потребител от адм. структура, произволен имейл
	 */
	public String getTypeString(int type) {
		SystemClassif classif = null;
		try {
			classif = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_REF_TYPE, type, SysConstants.CODE_DEFAULT_LANG, new Date(), false);			
		} catch (DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		
		if(classif != null) { // това е валидно само за кореспондентите нфл/фзл
			return classif.getTekst();
		}
		else if(type == TYPE_ADM_STR_LICE) {
			return "Лице от административната структура";
		}
		else if(type == TYPE_ADM_STR_ZVENO) {
			return "Звено от административната структура";
		}
		else if(type == TYPE_SOME_EMAIL) {
			return "Имейл адрес - свободен текст";
		}
		else if(type == TYPE_SOME_KORESP) {
			return "Кореспондент - свободен текст";
		}
		else if(type == TYPE_SSEV_EGN) {
			return "ЕГН";
		}
		else if(type == TYPE_SSEV_EIK) {
			return "ЕИК";
		}
		else { // неочакван случай - нито е от адм. стр., нито е кореспондент, нито произволен имейл
			return "";
		}
	}
	
	/**
	 * Получава код на референт и връща името му
	 */
	public String decodeCodeRefName(Integer codeRef) {
		if(codeRef == null) {
			return null;
		}
		
		try {
			Date date = new Date();
			
			if(isAdmStr(codeRef)) {
				SystemClassif adm = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, codeRef, SysConstants.CODE_DEFAULT_LANG, date, true);
				return adm.getTekst();
			}
			else if(isKoresp(codeRef)) {
				SystemClassif koresp = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_REFERENTS, codeRef, SysConstants.CODE_DEFAULT_LANG, date, true);
				return koresp.getTekst();
			}
			else { // кодът не е намерен нито в адм. структура, нито в кореспондентите, сигурно е счупен
				return null;
			}
		} catch (DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			scrollToMessages();
		}
		return null;
	}
	
	/**
	 * Получава код на референт и връща името му от класификацията с кореспонденти от СЕОС
	 */
	private String decodeCodeRefNameSeos(Integer codeRef) {
		if(codeRef == null) {
			return null;
		}
		
		try {
			Date date = new Date();
			SystemClassif adm = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_EGOV_ORGANISATIONS, codeRef, SysConstants.CODE_DEFAULT_LANG, date, true);
			return adm.getTekst();
		} catch (DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			scrollToMessages();
		}
		return null;
	}
	
	/**
	 * Прверява дали подаден код на референт е част от класификацията с административната структура.
	 * @param codeRef
	 */
	public boolean isAdmStr(Integer codeRef) {
		if(codeRef == null) return false;
		else
			try {
				return getSystemData().matchClassifItems(Constants.CODE_CLASSIF_ADMIN_STR, codeRef, new Date());
			} catch (DbErrorException e) {
				LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
				return false;
			}
	}
	
	/**
	 * Прверява дали подаден код на референт е Напуснал административната структура.
	 * @param codeRef
	 * @return
	 */
	public Boolean isAdmStrNapusnal(Integer codeRef) {
		if(codeRef == null) return null;
		else {
			try {
				if(this.napusnaliAdmStrukt == null) {
					// Това го взех от AdmStruct.java
					this.napusnaliAdmStrukt = this.systemData.getChildrenOnNextLevel(OmbConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, -1000, new Date(), getCurrentLang());
				}
				
				return !this.napusnaliAdmStrukt
						.stream()
						.filter(a -> a.getCode() == codeRef)
						.collect(Collectors.toList())
						.isEmpty();
			}
			catch (DbErrorException e) {
				LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
				return false;
			}
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
				LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
				return false;
			}
	}
	
	/**
	 * Връша true, ако системната настойка за работа с екземпляри е ДА
	 */
	public boolean useExemplar() {
		return this.withExemplars;
	}
	
	/**
	 * Дали да се покаже бутонът за изтриване на ред в горната таблица
	 */
	public boolean showDeleteButton(DocDvij dvij) {
		// документът вече е изпратен по електронна поща
		if(dvij.getDvijMethod() == OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL && dvij.getStatus() == OmbConstants.DS_SENT) {
			return false;
		}
		
		// документът вече е регистриран в друга регистратура
		if(dvij.getOtherDocId() != null) {
			return false;
		}
		
		// документът вече е регистриран при изпращане чрез СЕОС / ССЕВ
		if((dvij.getDvijMethod() == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS || dvij.getDvijMethod() == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV)
				&& dvij.getDvijMethod() == OmbConstants.DS_REGISTERED) {
			return false;
		}
		
		// има нещо в полето FROM_DVIJ_ID
		if(dvij.getFromDvijId() != null) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Валидира датата на предаване да не е по-ранна от датата на документа 
	 */
	public boolean validateDataPredavane() {		
		
		Date predavane = this.dataPredavane;
		Date dokument = this.document.getDocDate();
		Date dnes = new Date();
		
		if(predavane == null) {
			JSFUtils.addMessage(NEW_SEND_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavaneZadaljitelna"));
			return false;
		}
		if(dnes.compareTo(predavane) < 0) { // датата на предаване е по-късна от днешната
			JSFUtils.addMessage(NEW_SEND_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavaneBadeshta"));
			scrollToMessages();
			return false;
		}
		if(predavane.compareTo(dokument) < 0) { // датата на предаване е преди тази на документа
			JSFUtils.addMessage(NEW_SEND_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavanePrediDokument"));
			scrollToMessages();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Валидира датата на връшане ДО да не е по-ранна от датата на предаване
	 */
	public boolean validateDataVrashtaneDo() {
		
		Date vrashtane = this.dataVrashtane;
		Date predavane = this.dataPredavane;
		
		if (vrashtane == null) return true; 
		
		if(vrashtane.compareTo(predavane) < 0) { // датата на връщане е преди тази на предаване
			JSFUtils.addMessage(NEW_RETURN_TO_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneRanna"));
			scrollToMessages();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Валидира полетата за дата на 'предаване' и 'връщане до' при създаване на ново предаване.
	 * @param predavane дали да валидира полето 'дата на предаване'
	 * @param vrashtane дали да валидира полето 'да се върне до'
	 * @return
	 */
	private boolean validateDates(boolean predavane, boolean vrashtane) {
		boolean result = true;
		
		if(predavane) {
			result &= validateDataPredavane();
		}
		
		if(vrashtane) {
			result &= validateDataVrashtaneDo();
		}
		
		return result;
	}
	
	/**
	 * Преди да се извърши редакция на ред запазва данните на движението.
	 * Използва се, за да върне оригиналната дата, ако се сложи нещо невалидно.
	 * @param dvij
	 */
	public void onBeginRowEdit(Dvijenie dvij) {
		this.copyDate.put(dvij.hashCode(), new Date(dvij.getDataPredavane().getTime()));
	}
	
	/**
	 * Вика се при завършване на редакция на таблицата с нови движения. Валидира двете дати и екземпляра.
	 */
	public void onRowEdit(Dvijenie dvij) {
		boolean valid = true;
		
		valid &= validatePredavane(dvij);
		valid &= validateVrashtaneDo(dvij);
		valid &= validateExemplar(dvij);		
		
		if(!valid) {
			scrollToMessages();
		}
	}
	
	/**
	 * Вика се, след като се промени стойността на полето за номер на екземпляр. Валидира и изкарва съобщение.
	 */
	public void validateEkzempliarInput() {
		if(!validateExemplar(this.newPredavaneEkz)) {
			this.newPredavaneEkz = 1;
			scrollToMessages();
		}
	}
	
	/**
	 * Валидира при редактиране на датите директно в таблицата да не се допускат грешки
	 */
	private boolean validatePredavane(Dvijenie dvij) {
		
		Date predavane = dvij.dataPredavane;
		Date dokument = this.document.getDocDate();
		Date dnes = new Date();
		
		if(dnes.compareTo(predavane) < 0) { // датата на предаване е по-късна от днешната
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavaneBadeshta"));
			dvij.dataPredavane = this.copyDate.get(dvij.hashCode());
			dvij.dataVrashtane = null;
			return false;
		}
		if(predavane.compareTo(dokument) < 0) { // датата на предаване е преди тази на документа
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavanePrediDokument"));
			dvij.dataPredavane = this.copyDate.get(dvij.hashCode());
			dvij.dataVrashtane = null;
			return false;
		}
		
		return true;
	}
	
	/**
	 * Валидира при редактиране на датите директно в таблицата да не се допускат грешки
	 */
	private boolean validateVrashtaneDo(Dvijenie dvij) {
		
		Date vrashtane = dvij.dataVrashtane;
		Date predavane = dvij.dataPredavane;
		
		if(vrashtane != null && vrashtane.compareTo(predavane) < 0) { // датата на връщане е преди тази на предаване
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneDoRanna"));
			dvij.dataVrashtane = null;
			return false;
		}
		
		return true;
	}
	
	/**
	 * Валидира екземпляра в таблицата преди запис на новите движения
	 */
	private boolean validateExemplar(Dvijenie dvij) {
		if(this.useExemplar()) {
			return validateExemplar(dvij.exemplar);
		}
		
		else
			return true;
	}
	
	/**
	 * Извършва валидацията дали подадено число е валиден номер на екземпляр. Показва съобщения при грешка.
	 * @param ekz
	 * @return
	 */
	private boolean validateExemplar(Integer ekz) {
		if(ekz == null) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "docDvij.exemplarZadaljitelen"));
			return false;
		}
		
		if(ekz < 1) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "docDvij.nevalidenExemplar"));
			return false;
		}
		
		return true;
	}
	
	/**
	 * Връща deep копие на движението, за да се редактира, без да се разваля оригиналният обект.
	 * В полето id записва подадения index, за да може после по него да се извлече оригиналният обект от масива this.dvijenia
	 * @param index индексът в this.dvijenia
	 * @return
	 */
	private DocDvij getDvijenieCopy(int index) {
		DocDvij dvij = this.dvijenia.get(index);
		DocDvij copy = new DocDvij();
		
		copy.setId(dvij.getId());
		copy.setCodeRef(dvij.getCodeRef());
		copy.setDvijDate(dvij.getDvijDate());
		copy.setDvijEmail(dvij.getDvijEmail());
		copy.setDvijInfo(dvij.getDvijInfo());
		copy.setDvijMethod(dvij.getDvijMethod());
		copy.setDvijText(dvij.getDvijText());
		copy.setReturnCodeRef(dvij.getReturnCodeRef());
		copy.setReturnDate(dvij.getReturnDate());
		copy.setReturnInfo(dvij.getReturnInfo());
		copy.setReturnMethod(dvij.getReturnMethod());
		copy.setReturnTextRef(dvij.getReturnTextRef());
		copy.setReturnToDate(dvij.getReturnToDate());
		copy.setEkzNomer(dvij.getEkzNomer());
		
		return copy;
	}
	
	/**
	 * Връща само откъс от подаден стринг с '...' накрая. Ползва се при tooltip-овете за допълнителна информация.
	 * @param text целият стринг
	 * @param lengthToShow колко символа да покаже
	 * @return
	 */
	public String getShortInfo(String text, int lengthToShow) {
		if(text != null) {
			if(text.length() < lengthToShow) return text;
			else return text.substring(0, lengthToShow) + "...";
		}
		else {
			return null;
		}
	}
	
	/**
	 * Чете от базата съобщението за грешка на движение по СЕОС.
	 * @param messageId
	 */
	public void getErrorTextSeos(Integer messageId) {
		EgovMessagesDAO egovDao = new EgovMessagesDAO(getUserData());
		try {
			this.seosError = egovDao.getMessageError(messageId);
		} catch (DbErrorException e) {
			e.printStackTrace();
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "deloDvij.greshkaIzvlichaneNaGreshka"));
			scrollToMessages();
		}
	}
	
	/**
	 * Вика функцията scrollToErrors на страницата, за да се скролне екранът към съобщенията за грешка.
	 * Сложено е, защото иначе съобщенията може да са извън видимия екран и user изобшо да не разбере,
	 * че е излязла грешка, и каква.
	 */
	private void scrollToMessages() {
		PrimeFaces.current().executeScript("scrollToErrors()");
	}
	
	/**
	 * Вика се при кликване на иконката "Искане за нов статус" в таблицата.
	 * @param dvijenie - редът от таблицата
	 */
	public void requestStatus(DocDvij dvijenie) {
		EgovMessagesDAO egovDao = new EgovMessagesDAO(getUserData());
		try {
			JPA.getUtil().runInTransaction(() -> egovDao.saveStatusRequestMessage(dvijenie, this.systemData));
		} catch (BaseException e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.greshkaIskaneNovStatus"));
			scrollToMessages();
			e.printStackTrace();
		}
	}
	
	/**
	 * Вика се при кликване на иконката "Изпрати отново" в таблицата.
	 * @param dvijenie - редът от таблицата
	 */
	public void resend(DocDvij dvijenie) {
		EgovMessagesDAO egovDao = new EgovMessagesDAO(getUserData());
		try {
			JPA.getUtil().runInTransaction(() -> egovDao.resetOutgoingMessages(dvijenie.getMessageId()));
		} catch (BaseException e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.greshkaPovtornoIzprashtane"));
			scrollToMessages();
			e.printStackTrace();
		}
	}
	
	/**
	 * Вика се при кликване на бутона "За отговор"
	 * Извлича свързани документи, дошли по ел. връчване
	 */
	public void ssevGetAllDocCoresps() throws DbErrorException {
		String guidSSEV = SearchUtils.asString(
				getSystemData().getItemSpecific(
					OmbConstants.CODE_CLASSIF_REGISTRATURI,
					this.document.getRegistraturaId(),
					SysConstants.CODE_DEFAULT_LANG,
					new Date(),
					OmbClassifAdapter.REGISTRATURI_INDEX_GUID_SSEV));
		
		EgovMessagesDAO egovDao = new EgovMessagesDAO(getUserData());
		this.zaOtgovor = egovDao.getAllDocCoresps(Long.valueOf(this.document.getId()), guidSSEV);
	}
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	/* * * * * * * * * * * * * * Тук са всичките методи при избор на ново движение * * * * * * * * * * * * * * * * * */
	
	/**
	 * Написан е директен имейл адрес в полето.
	 * Накрая изчиства полето.
	 */
	public void actionSelectEmailText() {
		if(!validateDates(true, true)) return;
		
		this.newEmailText = this.newEmailText.trim();
		this.newEmailName = this.newEmailName.trim();
		
		if(!ValidationUtils.isEmailValid(this.newEmailText)) {
			if(!this.newEmailText.isEmpty()) {
				JSFUtils.addMessage(NEW_EMAIL, FacesMessage.SEVERITY_ERROR, 
						String.format(getMessageResourceString(beanMessages, "dvijenie.emailNevaliden"), this.newEmailText));
				scrollToMessages();
			}
		}
		else {
			
			Dvijenie predavane = new Dvijenie();
			predavane.setCodeReferent(null); // слагам това, понеже няма идентификатор
			predavane.setName(!this.newEmailName.isEmpty() ? this.newEmailName.trim() : null);
			predavane.setEmail(this.newEmailText);
			predavane.setDopInfo(null);
			predavane.setInformation(this.newPredavaneInfo.trim().isEmpty() ? null : this.newPredavaneInfo.trim());
			predavane.setType(TYPE_SOME_EMAIL);
			predavane.setMetodPredavane(this.newMetod);
			predavane.setDataPredavane(this.dataPredavane);
			predavane.setDataVrashtane(this.dataVrashtane); // не правя проверка дали е СЕОС или ССЕВ, понеже методът тук е Имейл
			predavane.setExemplar(this.newPredavaneEkz);
			
			if(this.jalbaDanni != null && this.jalbaDanni[2] != null && this.jalbaDanni[2].equals(this.newEmailText)) {
				predavane.setJalbaId(((BigInteger) this.jalbaDanni[0]).intValue());
			}
			
			addNewDvijenie(predavane, false);
			
			this.newEmailText = "";
			this.newEmailName = "";
		}
	}
	
	/**
	 * Направен е избор в полето 'Административна структура'.
	 * Накрая изчиства полето.
	 */
	@SuppressWarnings("rawtypes")
	public void actionSelectAdmStruct() {
		addNewDvijeniaByCodeAdm(this.selectedAdmStruct);
		
		// изчиства се стойността от компонента
		((List) this.bindAdmStruct.getAttributes().get("selectedClassifs")).clear();
		this.bindAdmStruct.clearInput();
	}
	
	/**
	 * Направен е избор в полето 'Групи служители'.
	 * Обхожда служителите в групата и ги добавя като накрая изчиства полето.
	 */
	public void actionSelectGroupSluj() {
		
		try {
			// в групата има специфика на всички служители (кодовете с разделител ',')
			String emplCodes = (String) getSystemData().getItemSpecific(
					OmbConstants.CODE_CLASSIF_GROUP_EMPL, 
					this.selectedGroupSluj, 
					SysConstants.CODE_DEFAULT_LANG, 
					new Date(), 
					OmbClassifAdapter.REG_GROUP_INDEX_MEMBERS);
			
			if (emplCodes != null) {
				String[] codes = emplCodes.split(",");
				addNewDvijeniaByCodeAdm(Stream.of(codes).map(Integer::valueOf).collect(Collectors.toList()));
			}
		} catch(DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaZarejdeneGrupaSluj"), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			scrollToMessages();
		}

		// изчиства се стойността от компонента
		this.bindGrupiSluj.clearInput();
	}
	
	/**
	 * Извлича данни на лица по код на референт и го добавя към новите движения.
	 * @param codes кодовете от административната структура
	 */
	private void addNewDvijeniaByCodeAdm(List<Integer> codes) {
		/*
		 * повече информация можете да намерите на:
		 * com.ib.omb.system.TestSystemData.testClassifGroupEmployees()
		*/
		
		if(!validateDates(true, true)) return;
		
		try {
			for (int code : codes) {
				SystemClassif empl = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, code, SysConstants.CODE_DEFAULT_LANG, new Date(), true);
				if(empl == null) continue; // ако записът е счупен и липсва в базата
				
				Integer drugaRegistratura = (Integer) empl.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA];
				
				if(this.newMetod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA) {
					
					// ако начинът е 'от/на друга регистратура', а е избран получател от същата, не го добавя и показва съобщение
					if(drugaRegistratura.equals(getUserData(UserData.class).getRegistratura())) {
						JSFUtils.addGlobalMessage(
								FacesMessage.SEVERITY_ERROR,
								String.format(
										getMessageResourceString(beanMessages, "docDvij.poluchatelOtSashtataRegistratura"), 
										((Integer) empl.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE]) == 1 ? "Звеното" : "Лицето", 
										empl.getTekst()));
						scrollToMessages();
						continue;
					}
				}
				
				Dvijenie predavane = new Dvijenie();
				
				predavane.setCodeReferent(Integer.valueOf(code));
				predavane.setName(empl.getTekst()); // името
				predavane.setEmail((String) empl.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_CONTACT_EMAIL]);
				predavane.setDopInfo(empl.getDopInfo()); // длъжността
				predavane.setInformation(this.newPredavaneInfo.isEmpty() ? null : this.newPredavaneInfo.trim());
				predavane.setType((Integer) empl.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE] == 1 ? TYPE_ADM_STR_ZVENO : TYPE_ADM_STR_LICE);
				predavane.setMetodPredavane(this.newMetod);
				predavane.setDataPredavane(this.dataPredavane);
				predavane.setDataVrashtane((this.newMetod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS || this.newMetod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV) ? null : this.dataVrashtane);
				predavane.setExemplar(this.newPredavaneEkz);
				
				addNewDvijenie(predavane, false);

			}
		} catch(DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}

	/**
	 * Направен е избор в полето 'ЕИК, име'.
	 * Накрая изчиства полето.
	 */
	public void actionSelectKoresp() {
		if(validateDates(true, this.newMetod != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV)) {
			addNewDvijeniaByCodeKoresp(Collections.singletonList(this.selectedKoresp));
		}
		
		// изчиства се стойността от компонента
		this.bindEikName.clearInput();
	}
	
	/**
	 * Направена е редакция на кореспондент, който е имал липсващи данни.
	 */
	public void actionSelectKorespEdited() {
		if(!validateDates(true, this.newMetod != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV)) return;
		this.displayEditQuestion = false; // независимо дали са въведени правилни данни, няма да показва повече съобщението за редакция, а само грешките отгоре
		if(this.editedKoresp != null) { // кореспондентът е null, Когато е дадено 'изтриване' а не редакция
			addNewDvijeniaByCodeKoresp(Collections.singletonList(this.editedKoresp));
		}
	}
	
	/**
	 * Направен е избор в полето 'Групи кореспонденти'.
	 * Обхожда кореспондентите в групата и ги добавя като накрая изчиства полето.
	 */
	public void actionSelectGroupKoresp() {
		if(validateDates(true, true)) {
			try {
				// в групата има специфика на всички служители (кодовете с разделител ',')
				String korespCodes = (String) getSystemData().getItemSpecific(
						OmbConstants.CODE_CLASSIF_GROUP_CORRESP, 
						this.selectedGroupKoresp,
						SysConstants.CODE_DEFAULT_LANG, 
						new Date(), 
						OmbClassifAdapter.REG_GROUP_INDEX_MEMBERS);
				
				if (korespCodes != null) {
					String[] codes = korespCodes.split(",");
					addNewDvijeniaByCodeKoresp(Stream.of(codes).map(Integer::valueOf).collect(Collectors.toList()));
				}
			} catch(DbErrorException e) {
				LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaZarejdeneGrupaSluj"), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
				scrollToMessages();
			}
		}
			
		// изчиства се стойността от компонента
		this.bindGrupiKoresp.clearInput();
	}
	
	/**
	 * Извлича данни на лица по код на кореспондент и го добавя към новите движения.
	 * @param codes кодовете от административната структура
	 */
	private void addNewDvijeniaByCodeKoresp(List<Integer> codes) {
		/*
		 * повече информация можете да намерите на:
		 * com.ib.omb.system.TestSystemData.testClassifReferents()
		*/
		
		Date date = new Date();
		
		try {
			for (int code : codes) {
				SystemClassif koresp = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_REFERENTS, code, SysConstants.CODE_DEFAULT_LANG, date, true);
				if(koresp == null) continue; // ако записът е счупен и липсва в базата
				
				// ако начинът на предаване е ССЕВ, прави проверка дали лицето е фзл/нфл, намира съответното егн/еик и проверява дали е празно
				// ако е празно, изкарва съобщение, иначе го прави на стринг, за да се покаже в таблицата (predavane.setIdentifikatorString)
				String identifikatorString = null;
				String type = null;
				String eikEgn = SearchUtils.asString(koresp.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_EIK_EGN]);
				if(this.newMetod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV) {

					if((int) koresp.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_REF_TYPE] == OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL) {
						identifikatorString = "ЕИК";
						type = OmbConstants.SSEV_TYPE_LEGALPERSON;
					}
					else if((int) koresp.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_REF_TYPE] == OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL) {
						identifikatorString = "ЕГН"; // това остава - заради ССЕВ
						type = OmbConstants.SSEV_TYPE_PERSON;
					}
					
					if(eikEgn != null) {
						identifikatorString += " " + eikEgn;
					}
				}
				
				// заради достъпа до личните данни - в допълнителната информаиця за физическите лица да остане само населеното място!!
				// Тази информация не се записва в базата - тя е само за екрана в помощната таблица
				String dopInfo = koresp.getDopInfo();
				if(dopInfo != null && (int) koresp.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_REF_TYPE] == OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL) {
					int i1 = dopInfo.indexOf("гр.");
					if(i1 == -1) {
						i1 = dopInfo.indexOf("с.");
					}
					if(i1 != -1) {						
						int i2 = dopInfo.indexOf(", ", i1);
						if(i2 != -1) {
							dopInfo = dopInfo.substring(i1, i2);
						}else {
							// има само град или село...
							dopInfo = dopInfo.substring(i1);
						}
					}else {
						dopInfo = null;
					}
				}
				
				Dvijenie predavane = new Dvijenie();
				
				predavane.setCodeReferent(Integer.valueOf(code));
				predavane.setName(koresp.getTekst()); // името
				predavane.setEmail((String) koresp.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_CONTACT_EMAIL]);
				predavane.setDopInfo(dopInfo);
				predavane.setInformation(this.newPredavaneInfo.isEmpty() ? null : this.newPredavaneInfo.trim());
				predavane.setType((int) koresp.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_REF_TYPE]);
				predavane.setMetodPredavane(this.newMetod);
				predavane.setDataPredavane(this.dataPredavane);
				predavane.setDataVrashtane((this.newMetod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS || this.newMetod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV) ? null : this.dataVrashtane);
				predavane.setExemplar(this.newPredavaneEkz);
				predavane.setIdentifikatorString(identifikatorString);
				
				predavane.setUchastnikIdent(eikEgn);
				predavane.setUchastnikName(koresp.getTekst());
				predavane.setUchastnikType(type);
				
				addNewDvijenie(predavane, codes.size() == 1); // вторият параметър ще извика прозореца за редакция, само ако е добавен само един-единствен кореспондент
			}
		} catch(DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}
	
	/**
	 * Направен е избор в полето 'Кореспондент - свободен текст'.
	 * Накрая изчиства полето.
	 */
	public void actionSelectKorespText() {
		if(!validateDates(true, true)) return;
		
		this.newKorespText = this.newKorespText.trim();
		
		if(!this.newKorespText.isEmpty()) {
			
			Dvijenie predavane = new Dvijenie();
			
			predavane.setCodeReferent(null);
			predavane.setName(this.newKorespText); // вкарания текст го попълвам в полето name
			predavane.setEmail(null);
			predavane.setDopInfo(null);
			predavane.setInformation(this.newPredavaneInfo.isEmpty() ? null : this.newPredavaneInfo.trim());
			predavane.setType(TYPE_SOME_KORESP);
			predavane.setMetodPredavane(this.newMetod);
			predavane.setDataPredavane(this.dataPredavane);
			predavane.setDataVrashtane(this.dataVrashtane); // не правя проверка дали е СЕОС или ССЕВ, понеже методът тук не е СЕОС, ССЕВ или Имейл
			predavane.setExemplar(this.newPredavaneEkz);
			
			if(this.jalbaDanni != null && this.jalbaDanni[1] != null && this.jalbaDanni[1].equals(this.newKorespText)) {
				predavane.setJalbaId(((BigInteger) this.jalbaDanni[0]).intValue());
			}
			
			addNewDvijenie(predavane, false);
			this.newKorespText = "";
		}
	}
	
	/**
	 * Направен е избор в полето 'ЕИК, Наименование' за пращане по Сеос.
	 * Накрая изчиства полето.
	 */
	@SuppressWarnings("rawtypes")
	public void actionSelectSeos() {
		addNewDvijeniaSeos(this.selectedSeos);
		
		// изчиства се стойността от компонента
		((List) this.bindSeos.getAttributes().get("selectedClassifs")).clear();
		this.bindSeos.clearInput();
	}
	
	/**
	 * Направен е избор в полето 'Групи кореспонденти' за начин на изпращане СЕОС.
	 * Обхожда кореспондентите в групата и ги добавя като накрая изчиства полето.
	 */
	public void actionSelectGroupKorespSeos() {
		try {
			// в групата има специфика на всички служители (кодовете с разделител ',')
			String korespCodes = (String) getSystemData().getItemSpecific(
					OmbConstants.CODE_CLASSIF_GROUP_CORRESP, 
					this.selectedGroupKorespSeos,
					SysConstants.CODE_DEFAULT_LANG, 
					new Date(), 
					OmbClassifAdapter.REG_GROUP_INDEX_MEMBERS);
			
			if (korespCodes != null) {
				String[] codes = korespCodes.split(",");
				addNewDvijeniaSeos(Stream.of(codes).map(Integer::valueOf).collect(Collectors.toList()));
			}
		} catch(DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaZarejdeneGrupaSluj"), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			scrollToMessages();
		}

		// изчиства се стойността от компонента
		this.bindGrupiKorespSeos.clearInput();
	}
	
	/**
	 * Извлича данни по код на референт и го добавя към новите движения.
	 * @param codes кодовете от класификацията за сеос
	 * @param codeClassif класификацията - сеос
	 */
	private void addNewDvijeniaSeos(List<Integer> codes) { //  TODO
		if(!validateDates(true, false)) return;
		
		Date date = new Date();
		
		try {
			for (int code : codes) {
				SystemClassif empl = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_EGOV_ORGANISATIONS, code, SysConstants.CODE_DEFAULT_LANG, date, true);
				if(empl == null) {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages,"docDvij.korespondentNeNameren"));
					continue;
				}
				
				Dvijenie predavane = new Dvijenie();
				
				predavane.setCodeReferent(null);
				predavane.setName(empl.getTekst());
				predavane.setDopInfo(empl.getDopInfo());
				predavane.setInformation(this.newPredavaneInfo.isEmpty() ? null : this.newPredavaneInfo.trim());
				predavane.setType(TYPE_SEOS_KORESP);
				predavane.setMetodPredavane(this.newMetod);
				predavane.setDataPredavane(this.dataPredavane);
				predavane.setExemplar(this.newPredavaneEkz);
				
				if (empl.getSpecifics() != null && empl.getSpecifics().length > 2) {
					predavane.setUchastnikIdent(SearchUtils.asString(empl.getSpecifics()[0]));
					predavane.setUchastnikGuid(SearchUtils.asString(empl.getSpecifics()[1]));
					predavane.setUchastnikName(SearchUtils.asString(empl.getSpecifics()[2]));
				}
				
				addNewDvijenie(predavane, true);
			}
		} catch(DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}
	
	/**
	 * Направен е избор в полето 'Административен орган' за пращане по Ссев.
	 * Накрая изчиства полето.
	 */
	@SuppressWarnings("rawtypes")
	public void actionSelectAdmOrgan() {
		if(!validateDates(true, false)) return;
		
		Date date = new Date();
		
		try {
			Integer code = this.selectedAdmOrgan.get(0);
			SystemClassif empl = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_EDELIVERY_ORGANISATIONS, code, SysConstants.CODE_DEFAULT_LANG, date, true);
			
			if(empl == null) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages,"docDvij.korespondentNeNameren"));
				return;
			}
			
			Dvijenie predavane = new Dvijenie();
			
			predavane.setCodeReferent(null);
			predavane.setName(empl.getTekst());
			predavane.setDopInfo(empl.getDopInfo());
			predavane.setInformation(this.newPredavaneInfo.isEmpty() ? null : this.newPredavaneInfo.trim());
			predavane.setType(TYPE_SEOS_KORESP);
			predavane.setMetodPredavane(this.newMetod);
			predavane.setDataPredavane(this.dataPredavane);
			predavane.setExemplar(this.newPredavaneEkz);
			
			if (empl.getSpecifics() != null && empl.getSpecifics().length > 2) {
				predavane.setUchastnikIdent(SearchUtils.asString(empl.getSpecifics()[0]));
				predavane.setUchastnikGuid(SearchUtils.asString(empl.getSpecifics()[1]));
				predavane.setUchastnikName(SearchUtils.asString(empl.getSpecifics()[2]));
			}
			predavane.setUchastnikType(OmbConstants.SSEV_TYPE_ADMINISTRATION);
			
			addNewDvijenie(predavane, true);
		} catch(DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		
		// изчиства се стойността от компонента
		((List) this.bindSsev.getAttributes().get("selectedClassifs")).clear();
		this.bindSsev.clearInput();
	}
	
	/**
	 * Написан е Егн в полето 'ЕГН' за пращане по Ссев.
	 * Накрая изчиства полето.
	 */
	public void actionSelectEgn() {
		if(!validateDates(true, false)) return;
		
		this.newEgn = this.newEgn.trim();
		
		if(this.newEgn.isEmpty()) {
			return;
		}
		
		if(ValidationUtils.isValidEGN(this.newEgn)) {
			Dvijenie predavane = new Dvijenie();
			
			predavane.setCodeReferent(null);
			predavane.setName(this.newEgn);
			predavane.setDopInfo(null);
			predavane.setInformation(this.newPredavaneInfo.isEmpty() ? null : this.newPredavaneInfo.trim());
			predavane.setType(TYPE_SSEV_EGN);
			predavane.setMetodPredavane(this.newMetod);
			predavane.setDataPredavane(this.dataPredavane);
			predavane.setExemplar(this.newPredavaneEkz);
			
			predavane.setUchastnikIdent(this.newEgn);
			predavane.setUchastnikName(this.newEgn);
			predavane.setUchastnikType(OmbConstants.SSEV_TYPE_PERSON);
			
			addNewDvijenie(predavane, false);
		}
		else {
			JSFUtils.addMessage(NEW_EGN, FacesMessage.SEVERITY_ERROR,
					String.format(getMessageResourceString(beanMessages, "dvijenie.egnNevaliden"), this.newEgn.trim()));
			scrollToMessages();
		}
		
		this.newEgn = "";
	}
	
	/**
	 * Написан е Еик в полето 'ЕИК' за пращане по Ссев.
	 * Накрая изчиства полето.
	 */
	public void actionSelectEik() {
		if(!validateDates(true, false)) return;
		
		this.newEik = this.newEik.trim();
		
		if(this.newEik.isEmpty()) {
			return;
		}
			
		if(ValidationUtils.isValidBULSTAT(this.newEik)) {
			Dvijenie predavane = new Dvijenie();
			
			predavane.setCodeReferent(null);
			predavane.setName(this.newEik);
			predavane.setDopInfo(null);
			predavane.setInformation(this.newPredavaneInfo.isEmpty() ? null : this.newPredavaneInfo.trim());
			predavane.setType(TYPE_SSEV_EIK);
			predavane.setMetodPredavane(this.newMetod);
			predavane.setDataPredavane(this.dataPredavane);
			predavane.setExemplar(this.newPredavaneEkz);
			
			predavane.setUchastnikIdent(this.newEik);
			predavane.setUchastnikName(this.newEik);
			predavane.setUchastnikType(OmbConstants.SSEV_TYPE_LEGALPERSON);
			
			addNewDvijenie(predavane, false);
		}
		else {
			JSFUtils.addMessage(NEW_EIK, FacesMessage.SEVERITY_ERROR,
					String.format(getMessageResourceString(beanMessages, "dvijenie.eikNevaliden"), this.newEik.trim()));
			scrollToMessages();
		}
		
		this.newEik = "";
	}
	
	/**
	 * Избран е получател от прозореца 'За отговор'
	 * @param index - индекс на получения обект в this.zaOtgovor. Обектът е с полета:
	 * <ul>
	 * 	 <li>0 - MSG_GUID (String)</li>
	 *   <li>1 - SENDER_NAME (String)</li>
	 *   <li>2 - RN_DOC (String)</li>
	 *   <li>3 - DOC_DATE (Timestamp)</li>
	 * </ul>
	 */
	public void actionSelectZaOtgovor(int index) {
		if(!validateDates(true, false)) return;
		Object[] poluchatel = this.zaOtgovor.get(index);
		
		Dvijenie predavane = new Dvijenie();
		
		predavane.setName(poluchatel[1] != null ? (String) poluchatel[1] : null);
		predavane.setInformation(this.newPredavaneInfo.isEmpty() ? null : this.newPredavaneInfo.trim());
		predavane.setMetodPredavane(this.newMetod);
		predavane.setDataPredavane(this.dataPredavane);
		predavane.setExemplar(this.newPredavaneEkz);
		
		predavane.setUchastnikIdent(poluchatel[0] != null ? (String) poluchatel[0] : null);
		predavane.setUchastnikName(poluchatel[1] != null ? (String) poluchatel[1] : null);
		predavane.setUchastnikType(OmbConstants.SSEV_TYPE_REPLY);
		
		addNewDvijenie(predavane, false);
	}
	
	/**
	 * Ако избраният референт го няма вече в таблицата, го добавя.
	 * Иначе може да покаже съобщение.
	 * @param askForEdit - може да показва прозореца за редакция на кореспондент само ако се подаде true
	 */
	private void addNewDvijenie(Dvijenie d, boolean askForEdit) {
		// тази проверка е нужна, ако вкарваме получател от адм. структура или кореспондентите
		// при ръчно написаните имейли вече е направено и се прескача
		boolean emailIsBad = d.email == null || d.email.isEmpty() || !ValidationUtils.isEmailValid(d.email);
		if((this.newMetod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL || this.newMetod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_MAIL_CUSTOM)
				&& d.codeReferent != null && emailIsBad) {
			
			// тук методът се разклонява
			
			// ако потребителят има права да редактира кореспонденти, се показва прозорец дали иска да въведе имейл моментално
			// проверява се и дали this.displayEditQuestion е true, защото иначе ще влезе в безкраен цикъл
			// 
			if(getUserData().hasAccess(OmbConstants.CODE_CLASSIF_MENU, OmbConstants.CODE_ZNACHENIE_MENU_CORESP) && this.displayEditQuestion && askForEdit) {
				this.editedKoresp = this.selectedKoresp;
				PrimeFaces.current().executeScript("PF('hiddenButtonEmailInvalid').jq.click();");
				return;
			}
			// ако няма права, методът си продължава валидацията и изкрава съобщение за грешка (или displayEditQuestion e false, ако сме ползвали диалоговия)
			else {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						String.format(getMessageResourceString(beanMessages, "dvijenie.poluchatelEmailNevaliden"), d.name));
				scrollToMessages();
				this.displayEditQuestion = true; // за да може следващия път отново да се покаже съобщението
				return;
			}
		}
		
		// проверка за липсващ егн/еик и показване на confirmDialog и компонент за редакция
		if(this.newMetod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV && d.getUchastnikIdent() == null) {
			
			// тук методът се разклонява
			
			// ако потребителят има права да редактира кореспонденти, се показва прозорец дали иска да въведе егн/еик моментално
			// проверява се и дали this.displayEditQuestion е true, защото иначе ще влезе в безкраен цикъл
			// 
			if(getUserData().hasAccess(OmbConstants.CODE_CLASSIF_MENU, OmbConstants.CODE_ZNACHENIE_MENU_CORESP) && this.displayEditQuestion && askForEdit) {
				 
				this.editedKoresp = this.selectedKoresp;
				PrimeFaces.current().executeScript("PF('hiddenButtonEgnEikInvalid').jq.click();");
				return;
			}
			// ако няма права, методът си продължава валидацията и изкрава съобщение за грешка (или displayEditQuestion e false, ако сме ползвали диалоговия)
			else {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						String.format(getMessageResourceString(beanMessages, "dvijenie.niamaPopalnenEgnEik"), d.getName(), d.getIdentifikatorString()));
				scrollToMessages();
				this.displayEditQuestion = true; // за да може следващия път отново да се покаже съобщението
				return;
			}

		}

		// d е въведен ръчно имейл, който вече присъства като вкаран герой от адм. структура или кореспондент
		boolean vavedenESCodeRef = 
				this.newDvijenia
				.stream()
				.anyMatch(dvij -> (dvij.email != null) && (dvij.email.equals(d.email) && (d.codeReferent == null) && (dvij.codeReferent != null)));
		// d е герой от адм. стр. или кореспондент с имейл, който вече е вкаран ръчно
		boolean vavedenERachnoEmail =  
				this.newDvijenia
				.stream()
				.anyMatch(dvij -> (dvij.email != null) && (dvij.email.equals(d.email) && (d.codeReferent != null) && (dvij.codeReferent == null)));
		
		// получателят не е добавен вече, нито е добавен имейлът му като свободен текст, нито се вкарва имейл, който вече е присъства
		if(!this.newDvijenia.contains(d) && !vavedenESCodeRef && !vavedenERachnoEmail) {
			this.newDvijenia.add(d);
		}
		// въвели сме например лицето Ефрем Стоев с имейл efrem@abv.bg, а тук се подава в полето за ръчно писане на имейл същият този имейл
		else if(!this.newDvijenia.contains(d) && vavedenESCodeRef) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
					String.format(getMessageResourceString(beanMessages, "dvijenie.emailVecheVaveden"), d.getEmail()));
			scrollToMessages();
		}
		// написали сме ръчно имейла efrem@abv.bg, а после се опитваме да изберем лицето Ефрем Стоев със същия имейл от адм. структура или коресп.
		else if(!this.newDvijenia.contains(d) && vavedenERachnoEmail && d.metodPredavane == OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
					String.format(getMessageResourceString(beanMessages, "dvijenie.emailNaVecheVaveden"), decodeCodeRefName(d.codeReferent)));
			scrollToMessages();
		}
		else {
			if(d.type == TYPE_SEOS_KORESP) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
						String.format(getMessageResourceString(beanMessages, "dvijenie.poluchatelVaveden"), decodeCodeRefNameSeos(d.codeReferent)));
			}
			else {
				String s = (d.email == null ? d.name : d.email);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
						String.format(getMessageResourceString(beanMessages, "dvijenie.poluchatelVaveden"), 
								(d.codeReferent == null) ? s : decodeCodeRefName(d.codeReferent)));
			}
			scrollToMessages();
		}
	}
	
	public void chooseJalbopodatelEmail() {
		this.setNewEmailText((String)this.jalbaDanni[2]);
		
		if(this.jalbaDanni[1] != null && !((String) this.jalbaDanni[1]).isEmpty()) {
			this.setNewEmailName((String) this.jalbaDanni[1]);
		}
		
		actionSelectEmailText();
	}
	
	public void chooseJalbopodatelIme() {
		if(this.jalbaDanni[1] != null && !((String) this.jalbaDanni[1]).isEmpty()) {
			this.setNewKorespText((String) this.jalbaDanni[1]);
		}
		
		actionSelectKorespText();
	}
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	public class Dvijenie {
		private Integer codeReferent;
		private String name;
		private String email;
		private String dopInfo; // за лицата от адм. структ е длъжността, за кореспондентите е информацията им
		private String information;
		private int type; // тип - нфл или фзл
		private int metodPredavane;
		private Date dataPredavane;
		private Date dataVrashtane;
		private Integer exemplar;
		private String identifikatorString;
		
		private String uchastnikGuid;
		private String uchastnikIdent;
		private String uchastnikName;
		private String uchastnikType;
		private Integer jalbaId;

		/**
		 * Когато се добавя ново изпращане тук се проверява дали вече не е добавен такъв запис.
		 * Например, за да не се изпраща няколко пъти към един и същ имейл или получател.
		 */
		@Override
		public boolean equals(Object obj) {
			
			if(obj == null) { 
				return false;
			}
			
			if (obj == this) { 
				return true;
			}
			
	        if (!(obj instanceof Dvijenie)) {
	        	return false;
	        }
	        
	        Dvijenie d = (Dvijenie) obj;
	        
	        if (this.codeReferent == null && d.codeReferent == null) { // ако кодовете са null, значи са вкарани ръчно имейли/кореспонденти
        		if(this.email != null && d.email != null) { // значи са ръчно вкарани имейли
        			return this.email.contentEquals(d.email);
        		}
        		else if(this.name != null && d.name != null) { // значи са ръчно вкарани кореспонденти
        			return this.name.contentEquals(d.name);
        		}
        		else { // остава единствено да са ръчно написани хем един такъв, хем един друг
        			return false;
        		}
	        }
	        else if(this.codeReferent == null || d.codeReferent == null) { // единият код е null, другият не е
	        	return false;
	        }
	        else // кодовете не са null, значи не са вкарани ръчно като имейли/кореспонденти и трябва да ги има в системата
	        	return this.codeReferent.compareTo(d.codeReferent) == 0;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.codeReferent, this.email, this.name);
		}

		public Integer getCodeReferent() {
			return codeReferent;
		}

		public void setCodeReferent(Integer codeReferent) {
			this.codeReferent = codeReferent;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getEmail() {
			return email;
		}

		public void setEmail(String email) {
			this.email = email;
		}

		public String getDopInfo() {
			return dopInfo;
		}

		public void setDopInfo(String dopInfo) {
			this.dopInfo = dopInfo;
		}

		public String getInformation() {
			return information;
		}

		public void setInformation(String information) {
			this.information = information;
		}

		public int getType() {
			return type;
		}

		public void setType(int type) {
			this.type = type;
		}

		public int getMetodPredavane() {
			return metodPredavane;
		}

		public void setMetodPredavane(int metodPredavane) {
			this.metodPredavane = metodPredavane;
		}

		public Date getDataPredavane() {
			return dataPredavane;
		}

		public void setDataPredavane(Date dataPredavane) {
			this.dataPredavane = dataPredavane;
		}

		public Date getDataVrashtane() {
			return dataVrashtane;
		}

		public void setDataVrashtane(Date dataVrashtane) {
			this.dataVrashtane = dataVrashtane;
		}

		public Integer getExemplar() {
			return exemplar;
		}

		public void setExemplar(Integer exemplar) {
			this.exemplar = exemplar;
		}

		public String getIdentifikatorString() {
			return identifikatorString;
		}

		public void setIdentifikatorString(String identifikatorString) {
			this.identifikatorString = identifikatorString;
		}

		public String getUchastnikGuid() {
			return uchastnikGuid;
		}

		public void setUchastnikGuid(String uchastnikGuid) {
			this.uchastnikGuid = uchastnikGuid;
		}

		
		public String getUchastnikName() {
			return uchastnikName;
		}

		public void setUchastnikName(String uchastnikName) {
			this.uchastnikName = uchastnikName;
		}

		public String getUchastnikIdent() {
			return uchastnikIdent;
		}

		public void setUchastnikIdent(String uchastnikIdent) {
			this.uchastnikIdent = uchastnikIdent;
		}

		public String getUchastnikType() {
			return uchastnikType;
		}

		public void setUchastnikType(String uchastnikType) {
			this.uchastnikType = uchastnikType;
		}

		public Integer getJalbaId() {
			return jalbaId;
		}

		public void setJalbaId(Integer jalbaId) {
			this.jalbaId = jalbaId;
		}
		
	}


	public Doc getDocument() {
		return document;
	}

	public void setDocument(Doc document) {
		this.document = document;
	}

	public String getRnFullDoc() {
		return rnFullDoc;
	}

	public void setRnFullDoc(String rnFullDoc) {
		this.rnFullDoc = rnFullDoc;
	}

	public List<SelectItem> getPredavaneTypeList() {
		return predavaneTypeList;
	}

	public void setPredavaneTypeList(List<SelectItem> predavaneTypeList) {
		this.predavaneTypeList = predavaneTypeList;
	}
	
	public List<SelectItem> getPredavaneTypeListNoEmail() {
		return predavaneTypeList
				.stream()
				.filter(item -> (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL)
				.collect(Collectors.toList());
	}
	
	public List<SelectItem> getPredavaneTypeListNoEmailSystem() {
		return predavaneTypeList
				.stream()
				.filter(item -> (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL 
							&& (Integer)item.getValue()!= OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA)
				.collect(Collectors.toList());
	}

	public List<DocDvij> getDvijenia() {
		return dvijenia;
	}

	public void setDvijenia(List<DocDvij> dvijenia) {
		this.dvijenia = dvijenia; 
	}

	public List<Dvijenie> getNewDvijenia() {
		return newDvijenia;
	}

	public void setNewDvijenia(List<Dvijenie> newDvijenia) {
		this.newDvijenia = newDvijenia;
	}

	public SelectOneModalA getBindGrupiSluj() {
		return bindGrupiSluj;
	}

	public void setBindGrupiSluj(SelectOneModalA bindGrupiSluj) {
		this.bindGrupiSluj = bindGrupiSluj;
	}

	public SelectOneModalA getBindGrupiKoresp() {
		return bindGrupiKoresp;
	}

	public void setBindGrupiKoresp(SelectOneModalA bindGrupiKoresp) {
		this.bindGrupiKoresp = bindGrupiKoresp;
	}

	public SelectOneModalA getBindGrupiKorespSeos() {
		return bindGrupiKorespSeos;
	}

	public void setBindGrupiKorespSeos(SelectOneModalA bindGrupiKorespSeos) {
		this.bindGrupiKorespSeos = bindGrupiKorespSeos;
	}

	public SelectManyModalA getBindAdmStruct() {
		return bindAdmStruct;
	}

	public void setBindAdmStruct(SelectManyModalA bindAdmStruct) {
		this.bindAdmStruct = bindAdmStruct;
	}

	public SelectOneModalA getBindEikName() {
		return bindEikName;
	}

	public void setBindEikName(SelectOneModalA bindEikName) {
		this.bindEikName = bindEikName;
	}

	public SelectManyModalA getBindSeos() {
		return bindSeos;
	}

	public void setBindSeos(SelectManyModalA bindSeos) {
		this.bindSeos = bindSeos;
	}

	public SelectManyModalA getBindSsev() {
		return bindSsev;
	}

	public void setBindSsev(SelectManyModalA bindSsev) {
		this.bindSsev = bindSsev;
	}

	public boolean isViewNewPanel() {
		return viewNewPanel;
	}

	public void setViewNewPanel(boolean viewNewPanel) {
		this.viewNewPanel = viewNewPanel;
	}

	public boolean isViewEditPanel() {
		return viewEditPanel;
	}

	public void setViewEditPanel(boolean viewEditPanel) {
		this.viewEditPanel = viewEditPanel;
	}

	public boolean isViewReturnPanel() {
		return viewReturnPanel;
	}

	public void setViewReturnPanel(boolean viewReturnPanel) {
		this.viewReturnPanel = viewReturnPanel;
	}

	public Integer getNewMetod() {
		return newMetod;
	}

	public void setNewMetod(Integer newMetod) {
		this.newMetod = newMetod;
	}

	public Date getDataPredavane() {
		return dataPredavane;
	}

	public void setDataPredavane(Date dataPredavane) {
		this.dataPredavane = dataPredavane;
	}

	public Date getDataVrashtane() {
		return dataVrashtane;
	}

	public void setDataVrashtane(Date dataVrashtane) {
		this.dataVrashtane = dataVrashtane;
	}

	public List<Integer> getSelectedAdmStruct() {
		return selectedAdmStruct;
	}

	public void setSelectedAdmStruct(List<Integer> selectedAdmStruct) {
		this.selectedAdmStruct = selectedAdmStruct;
	}

	public Integer getSelectedGroupSluj() {
		return selectedGroupSluj;
	}

	public void setSelectedGroupSluj(Integer selectedGroupSluj) {
		this.selectedGroupSluj = selectedGroupSluj;
	}

	public Integer getSelectedKoresp() {
		return selectedKoresp;
	}

	public void setSelectedKoresp(Integer selectedKoresp) {
		this.selectedKoresp = selectedKoresp;
	}

	public Integer getSelectedGroupKoresp() {
		return selectedGroupKoresp;
	}

	public void setSelectedGroupKoresp(Integer selectedGroupKoresp) {
		this.selectedGroupKoresp = selectedGroupKoresp;
	}

	public Integer getSelectedGroupKorespSeos() {
		return selectedGroupKorespSeos;
	}

	public void setSelectedGroupKorespSeos(Integer selectedGroupKorespSeos) {
		this.selectedGroupKorespSeos = selectedGroupKorespSeos;
	}

	public List<Integer> getSelectedSeos() {
		return selectedSeos;
	}

	public void setSelectedSeos(List<Integer> selectedSeos) {
		this.selectedSeos = selectedSeos;
	}

	public List<Integer> getSelectedAdmOrgan() {
		return selectedAdmOrgan;
	}

	public void setSelectedAdmOrgan(List<Integer> selectedAdmOrgan) {
		this.selectedAdmOrgan = selectedAdmOrgan;
	}

	public String getNewEmailText() {
		return newEmailText;
	}

	public void setNewEmailText(String newEmailText) {
		this.newEmailText = newEmailText;
	}

	public String getNewEmailName() {
		return newEmailName;
	}

	public void setNewEmailName(String newEmailName) {
		this.newEmailName = newEmailName;
	}

	public String getNewKorespText() {
		return newKorespText;
	}

	public void setNewKorespText(String newKorespText) {
		this.newKorespText = newKorespText;
	}

	public String getNewPredavaneInfo() {
		return newPredavaneInfo;
	}

	public void setNewPredavaneInfo(String newPredavaneInfo) {
		this.newPredavaneInfo = newPredavaneInfo;
	}

	public Integer getNewPredavaneEkz() {
		return newPredavaneEkz;
	}

	public void setNewPredavaneEkz(Integer newPredavaneEkz) {
		this.newPredavaneEkz = newPredavaneEkz;
	}

	public DocDvij getTempDvij() {
		return tempDvij;
	}

	public void setTempDvij(DocDvij tempDvij) {
		this.tempDvij = tempDvij;
	}

	public String[] getInformationText() {
		return informationText;
	}
	
	public void setInformationText(String header, DocDvij dvijenie) {
		String info = dvijenie.getStatusText();
		
		try {
			if(dvijenie.getForRegid() != null && dvijenie.getStatus() == OmbConstants.DS_REGISTERED) {
				info += "<br>Регистратура: " + getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_REGISTRATURI, dvijenie.getForRegid(), SysConstants.CODE_DEFAULT_LANG, new Date(), false).getTekst();
			}
		}
		catch(DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
		}
		
		this.informationText = new String[] { header, info };
	}
	
	public void setInformationText(String header, String info) {
		this.informationText = new String[] { header, info };
	}

	public int getCountOfficalFiles() {
		return countOfficalFiles;
	}

	public void setCountOfficalFiles(int countOfficalFiles) {
		this.countOfficalFiles = countOfficalFiles;
	}

	public Map<Integer, Object> getSpecGroup() {
		return this.specGroup;
	}

	public Map<Integer, Object> getSpecGroupSeos() {
		return specGroupSeos;
	}

	public int getNewKorespType() {
		return newKorespType;
	}

	public void setNewKorespType(int newKorespType) {
		this.newKorespType = newKorespType;
	}

	public List<Object[]> getZaOtgovor() {
		return zaOtgovor;
	}

	public void setZaOtgovor(List<Object[]> zaOtgovor) {
		this.zaOtgovor = zaOtgovor;
	}

	public Integer getEditedKoresp() {
		return editedKoresp;
	}

	public void setEditedKoresp(Integer editedKoresp) {
		this.editedKoresp = editedKoresp;
	}

	public boolean isDisplayEditQuestion() {
		return displayEditQuestion;
	}

	public void setDisplayEditQuestion(boolean displayEditQuestion) {
		this.displayEditQuestion = displayEditQuestion;
	}

	public String getNewEgn() {
		return newEgn;
	}

	public void setNewEgn(String newEgn) {
		this.newEgn = newEgn;
	}

	public String getNewEik() {
		return newEik;
	}

	public void setNewEik(String newEik) {
		this.newEik = newEik;
	}

	public String getSeosError() {
		return seosError;
	}

	public void setSeosError(String seosError) {
		this.seosError = seosError;
	}
	
	
	/** 
	 * Отпечатване на плик/Обратна разписка  - //ivanc
	 */
	public String preparePostCoverNotice(int index) {		
		clearPostCover();
		DocDvij forSend = this.dvijenia.get(index);
		if (null==forSend) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при вземане на данни за кореспондент!");
			return null;
		}
		
		try {
			
			this.setCountryBg(Integer.parseInt(getSystemData().getSettingsValue("delo.countryBG"))); 	
			
			if(forSend.getCodeRef() != null){ // Взема адреса на кореспондента
				this.setRef(new ReferentDAO(getUserData()).findByCode(forSend.getCodeRef(), forSend.getStatusDate(), true));
				this.setAdr(getRef().getAddress());
				
			}else {
				this.setRef(null);
				this.setAdr(null);
		
				if (null!=forSend.getDvijText() && ! forSend.getDvijText().trim().equals("")) {
					this.setCorespName(forSend.getDvijText().trim());
				}
				if(forSend.getJalbaId() != null) {
					// До жалбоподател!  Да се извлече адреса от жалбата
					DocJalba jalba = new DocJalbaDAO(getUserData()).findJbpDataPlik(forSend.getJalbaId());
					Referent tmpRef = new Referent(); // няма да сетвам името - то е сетанта малко по-нагоре////
					tmpRef.setContactPhone(jalba.getJbpPhone());
					this.setRef(tmpRef);
					ReferentAddress tmpAdr = new ReferentAddress();
					tmpAdr.setEkatte(jalba.getJbpEkatte());
					tmpAdr.setAddrText(jalba.getJbpAddr());
					tmpAdr.setPostCode(jalba.getJbpPost());
					tmpAdr.setAddrCountry(this.getCountryBg()); // само в рамките на БГ!				
					this.setAdr(tmpAdr);
				}
		
			}
		
			// Data Corespondent
			prepareCorespData();
			
			
			// Data Sender
			prepareSenderData(); // from user Registratura
		
	
		} catch (BaseException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "Грешка при вземане на адреса!"), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		
		
		return null;
	}
	
	public void clearPostCover() {
		
		this.setFormatPlik(null);
		this.setRecommended(false);
		this.setCorespName(null);
		this.setCorespTel(null);
		this.setCorespAddress(null);
		this.setCorespPostCode(null); 
		this.setCorespPBox(null);
		this.setCorespObl(null);
		this.setCorespNM(null);
		this.setSenderName(null);
		this.setSenderTel(null);
		this.setSenderAddress(null);
		this.setSenderPostCode(null); 
		this.setSenderPBox(null);
		this.setSenderObl(null);
		this.setSenderNM(null);
		this.setCorespCountry(null);
		this.setSenderCountry(null);
		this.setIdRegistratura(null);

	}
		
	/**
	 * данни за получател - кореспондент
	 */
	public void prepareCorespData() {
				
		try {
			
			// Corespondents
			if(null != getRef()) {
				if(null != this.getRef().getRefName() && !this.getRef().getRefName().trim().equals("")){// Name
					this.setCorespName(this.getRef().getRefName().trim()); 
				}
				if(null != this.getRef().getContactPhone() && !this.getRef().getContactPhone().trim().equals("")){//Tel
					this.setCorespTel(this.getRef().getContactPhone().trim()); 
				}
			}
			
			
			if(null != this.getAdr()) {
				if(null != this.getAdr().getAddrText() && !this.getAdr().getAddrText().trim().equals("")){// Address
					this.setCorespAddress(this.getAdr().getAddrText().trim());	
				}else if (null != this.getAdr().getPostBox() && !this.getAdr().getPostBox().trim().equals("")) {
					this.setCorespAddress("Пощенска кутия "+this.getAdr().getPostBox().trim());	
				}
	
				if(null != this.getAdr().getPostCode() && !this.getAdr().getPostCode().trim().equals("")){//PostCode - BG
					this.setCorespPostCode(this.getAdr().getPostCode().trim()); 
				} 
				
				if(null != this.getAdr().getEkatte()){
					String obstObl = getSystemData().decodeItemLite(CODE_CLASSIF_EKATTE, this.getAdr().getEkatte(), CODE_DEFAULT_LANG,new Date(), false).getDopInfo();// Obst and Obl
					if (null!=obstObl) { 
						String[] deco = obstObl.split(",");
						if (deco.length==2 && null!=deco[1]) {
							this.setCorespObl(deco[1].trim());// Oblast
						}
					}
					
					//NM
					this.setCorespNM(getSystemData().decodeItem(CODE_CLASSIF_EKATTE, this.getAdr().getEkatte(), CODE_DEFAULT_LANG, this.getRef().getDateReg()));
	
				}

				if(null != getAdr().getAddrCountry()){//Country
					this.setCorespCountry(getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_COUNTRIES, this.getAdr().getAddrCountry(), CODE_DEFAULT_LANG, this.getRef().getDateReg())); 
				}
			}
			
			
			
		} catch (BaseException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "Грешка при вземане на данни за кореспондент!"), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}	
		
	}
	
	/**
	 * данни за подател
	 */
	public void prepareSenderData() {
		Registratura registratura=null;
		try {
			
			this.setIdRegistratura(((UserData)getUserData()).getRegistratura());
			
			if (null!=this.getIdRegistratura()) {
				registratura = new RegistraturaDAO(getUserData()).findById(this.getIdRegistratura());
				if (null!=registratura) {
				
					if (null!=registratura.getAddress() && ! registratura.getAddress().trim().equals("")) {
						this.setSenderAddress(registratura.getAddress().trim());
					}else if (null != registratura.getPostBox() && !registratura.getPostBox().trim().equals("")) {
						this.setSenderAddress("Пощенска кутия "+registratura.getPostBox().trim());	
					}
					
					
					if (null!=registratura.getOrgName() && ! registratura.getOrgName().trim().equals("")) {
						this.setSenderName(registratura.getOrgName().trim());
					}
					
					if (null!=registratura.getContacts() && ! registratura.getContacts().trim().equals("")) {
						this.setSenderTel(registratura.getContacts().trim());
					}
					
					if(null!=registratura.getPostCode() && !registratura.getPostCode().trim().equals("")){
						this.setSenderPostCode(registratura.getPostCode().trim());
					}
					
					if(null != registratura.getEkatte()){
						String obstOblS = getSystemData().decodeItemLite(CODE_CLASSIF_EKATTE, registratura.getEkatte(), CODE_DEFAULT_LANG, new Date(), false).getDopInfo();// Obst and Obl
						if (null!=obstOblS) { 
							String[] deco = obstOblS.split(",");
							if (deco.length==2 && null!=deco[1]) {
								this.setSenderObl(deco[1].trim());// Oblast
							}
						}
						
						//NM
						this.setSenderNM(getSystemData().decodeItem(CODE_CLASSIF_EKATTE, registratura.getEkatte(), CODE_DEFAULT_LANG, new Date()));
		
					}
				}
				
			}
		
		} catch (BaseException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "Грешка при вземане на данни за подател!"), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}	
		
		
	}

	/**
	 * Печат на  пощ. плик
	 * @param envNotice
	 */
	public void actionPrint(int envNotice) {
		
		if(!validatePrint()) {
			scrollToMessages();
			return;
		} 
		
		String regN="";
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
		    	
    	if (null!=this.document.getRnDoc())
    		regN +=this.document.getRnDoc().trim();
    	if (null!=this.document.getDocDate())
    		regN +="/"+sdf.format(this.document.getDocDate());
		
		
		
		if (envNotice==1) {//Печат пощ. плик
			String correspData="";
			String senderData="";
			
			correspData = CorrDataEnd();
			senderData = SenderDataEnd();

			// Print
			try {
				BaseExport exp= new BaseExport();
				String rPlik = "";
				
		    	if(null!=this.getFormatPlik()){
		    		rPlik = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_POST_ENVELOPS, this.getFormatPlik(), CODE_DEFAULT_LANG, new Date(), false).getDopInfo();
		    	}

				exp.printPlikCorespondent(rPlik, this.getCorespName(), correspData, this.getSenderName(), senderData, regN, this.isRecommended());

			
		    }catch (DbErrorException e) {
		        LOGGER.error("Грешка при отпечатване на плик!",e);
		        JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при отпечатване на плик!", e.getMessage());
		    }
		}else {//Печат Обратна разписка
			
			try{
				
				// 1. Зарежда лиценза за работа с MS Word documents.
				
				License license = new License();
				String nameLic="Aspose.Words.lic";
				InputStream inp = getClass().getClassLoader().getResourceAsStream(nameLic);
				license.setLicense(inp);
				
				Document docEmptyShablon = null; 
				// 2. Чете файл-шаблон и създава празен Aspose Document за попълване 
				String namIzv="/resources/docs/"+"Известие доставка 243.docx";
				ServletContext context_ = (ServletContext)FacesContext.getCurrentInstance().getExternalContext().getContext();
				FileInputStream  fis=null;
				fis = new FileInputStream(context_.getRealPath("")+namIzv);
		       /* int size = fis.available();
		        byte[] baR = new byte[size];
		        fis.read(baR);
		        fis.close();*/
 		        if (null==fis || fis.available()==0) {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при четене на файл шаблон!");
					return;
				}
 		       docEmptyShablon = new Document((InputStream)fis);
 		        
//		        docEmptyShablon = new Document(new ByteArrayInputStream(baR));
		     
		        // 2. Или чете файл-шаблон от БД и създава празен Asoose Document за попълване
		        /*Files fileShabl = new FilesDAO(getUserData()).findById(Integer.valueOf(-111));
				if (null==fileShabl || null==fileShabl.getContent()) {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при четене на файл шаблон от БД!");
					return;
				}
				docEmptyShablon = new Document(new ByteArrayInputStream(fileShabl.getContent()));*/
				
				// 4. Създава попълнен документ от шаблона
				Document docFilledShablon = null;
	
				docFilledShablon = fillDocShabl243(docEmptyShablon);
			
				ByteArrayOutputStream dstStream = new ByteArrayOutputStream();
				docFilledShablon.save(dstStream, SaveFormat.DOCX);
				byte [] bytearray = null;
				bytearray = dstStream.toByteArray();
				// 5. Създава файла от създадения MS Word документ и го показва
				if (bytearray !=null){ 
					String fileName = "Izvestie_Dostavka";
					
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
			
			
		}

		clearPostCover();
	}
	
	
	public String CorrDataEnd() {
		String retDat="";
		
		// Corespondent
		
		if(null != this.getCorespAddress() && !this.getCorespAddress().trim().equals(""))
			retDat+=this.getCorespAddress();
		
		if(null != this.getCorespTel() && !this.getCorespTel().trim().equals("")){
			if(null!=this.getAdr() && !this.getCountryBg().equals(this.getAdr().getAddrCountry())) {
				retDat+=" phone "+ this.getCorespTel();
			}else{
				retDat+=" тел. "+ this.getCorespTel();
			}
		}

		if(null != this.getCorespObl() && !this.getCorespObl().trim().equals("")) {
			if (! this.getCorespObl().trim().contains("обл")) {
				retDat+="\n"+"обл. "+this.getCorespObl().trim();
			}else {
				retDat+="\n"+this.getCorespObl().trim();
			}
		}
	
		if(null != this.getCorespPostCode() && !this.getCorespPostCode().trim().equals(""))
			retDat+="\n"+this.getCorespPostCode();
		
		if(null != this.getCorespNM() && !this.getCorespNM().trim().equals(""))
			retDat+=" "+this.getCorespNM();
							  	  
		if(null!=this.getAdr() && !this.getCountryBg().equals(this.getAdr().getAddrCountry()) && null != this.getCorespCountry() && !this.getCorespCountry().trim().equals(""))
			retDat+="\n"+this.getCorespCountry();
			
						
		
		return retDat;
	}
	
	
	public String SenderDataEnd () {
		String retDat="";
		// Sender
		
		if(null != this.getSenderAddress() && !this.getSenderAddress().trim().equals(""))
			retDat+=this.getSenderAddress();
		
		if(null != this.getSenderTel() && !this.getSenderTel().trim().equals("")){
			if(null!=this.getAdr() && ! this.getAdr().getAddrCountry().equals(this.getCountryBg())){
				retDat+=" phone "+ this.getSenderTel();
			}else{
				retDat+=" тел. "+ this.getSenderTel();
			}
		}

		if(null != this.getSenderObl() && !this.getSenderObl().trim().equals("")){
			if (! this.getSenderObl().trim().contains("обл")) {
				retDat+="\n"+"обл. "+this.getSenderObl().trim();
			}else {
				retDat+="\n"+this.getSenderObl().trim();
			}
		}

		if(null != this.getSenderPostCode() && !this.getSenderPostCode().trim().equals(""))
			retDat+="\n"+this.getSenderPostCode();
		
		if(null != this.getSenderNM() && !this.getCorespNM().trim().equals(""))
			retDat+=" "+this.getSenderNM();

		if(null!=this.getAdr() && !this.getCountryBg().equals(this.getAdr().getAddrCountry()) && null != this.getSenderCountry() && !this.getSenderCountry().trim().equals(""))
			retDat+="\n"+this.getSenderCountry();
		
		return retDat;
		
	}
	
	
	
	public Document fillDocShabl243 (com.aspose.words.Document pattern) throws DbErrorException  {
		
		try{
			
			// 1. Данни на Получател 
			
			if (null != this.getCorespName() && pattern.getRange().getBookmarks().get("poluchatel") !=null){// Name
				pattern.getRange().getBookmarks().get("poluchatel").setText(this.getCorespName());
			}	
			
			if (null!=this.getCorespAddress() && pattern.getRange().getBookmarks().get("adres") !=null){//Addres
				String coradr=this.getCorespAddress();
				if(null != this.getCorespTel() && !this.getCorespTel().trim().equals("")){
					if(null!=this.getAdr() && !this.getCountryBg().equals(this.getAdr().getAddrCountry())) {
						coradr+=" phone "+ this.getCorespTel();
					}else{
						coradr+=" тел. "+ this.getCorespTel();
					}
				}
				
				pattern.getRange().getBookmarks().get("adres").setText(coradr);
			}
									
			if (null != this.getCorespNM() && pattern.getRange().getBookmarks().get("nasMesto") !=null){
				pattern.getRange().getBookmarks().get("nasMesto").setText(this.getCorespNM());
			}
				
			if (null != this.getCorespPostCode()){
				String pkAdressat=this.getCorespPostCode().trim();
				if (pattern.getRange().getBookmarks().get("pk1") !=null)
					pattern.getRange().getBookmarks().get("pk1").setText(pkAdressat.substring(0, 1));
				if (pattern.getRange().getBookmarks().get("pk2") !=null)
					pattern.getRange().getBookmarks().get("pk2").setText(pkAdressat.substring(1, 2));
				if (pattern.getRange().getBookmarks().get("pk3") !=null)
					pattern.getRange().getBookmarks().get("pk3").setText(pkAdressat.substring(2, 3));
				if (pattern.getRange().getBookmarks().get("pk4") !=null)
					pattern.getRange().getBookmarks().get("pk4").setText(pkAdressat.substring(3, 4));
				if (pattern.getRange().getBookmarks().get("pk") !=null)
					pattern.getRange().getBookmarks().get("pk").setText(pkAdressat);
			}

			
			// 2. Данни на адресанта(подател) - от регистратурата
			
	            if (null!= this.getSenderName() && pattern.getRange().getBookmarks().get("podatel") !=null){
					pattern.getRange().getBookmarks().get("podatel").setText(this.getSenderName());
				}

	            if (null!=this.getSenderAddress() && pattern.getRange().getBookmarks().get("adresPod") !=null){
	            	String sendadr=this.getSenderAddress();
	            	if(null != this.getSenderTel() && !this.getSenderTel().trim().equals("")){
	    				if(null!=this.getAdr() && !this.getCountryBg().equals(this.getAdr().getAddrCountry())) {
	    					sendadr+=" phone "+ this.getSenderTel();
	    				}else{
	    					sendadr+=" тел. "+ this.getSenderTel();
	    				}
	    			}
	            	
					pattern.getRange().getBookmarks().get("adresPod").setText(sendadr);
				}
				

				//NM
				if (null!=this.getSenderNM() && pattern.getRange().getBookmarks().get("nasMestoPod") !=null)
					pattern.getRange().getBookmarks().get("nasMestoPod").setText(this.getSenderNM());

				if (null!=this.getSenderObl() && pattern.getRange().getBookmarks().get("oblPod") !=null)
					pattern.getRange().getBookmarks().get("oblPod").setText(this.getSenderObl());
				

				
				if(null!=this.getSenderPostCode()){
					String pkAdressant=this.getSenderPostCode().trim();
					if (pattern.getRange().getBookmarks().get("pkp1") !=null)
						pattern.getRange().getBookmarks().get("pkp1").setText(pkAdressant.substring(0, 1));
					if (pattern.getRange().getBookmarks().get("pkp2") !=null)
						pattern.getRange().getBookmarks().get("pkp2").setText(pkAdressant.substring(1, 2));
					if (pattern.getRange().getBookmarks().get("pkp3") !=null)
						pattern.getRange().getBookmarks().get("pkp3").setText(pkAdressant.substring(2, 3));
					if (pattern.getRange().getBookmarks().get("pkp4") !=null)
						pattern.getRange().getBookmarks().get("pkp4").setText(pkAdressant.substring(3, 4));
					if (pattern.getRange().getBookmarks().get("pkp") !=null)
						pattern.getRange().getBookmarks().get("pkp").setText(pkAdressant);
					
					
					if (null!=pattern.getRange().getBookmarks().get("barCod")){
						Barcode128 barcode = new Barcode128();
						barcode.setCodeType(com.lowagie.text.pdf.Barcode128.POSTNET);
						barcode.setCode(this.getCorespPostCode());
//						barcode.setAltText("Пощенски код");
						barcode.setGenerateChecksum(true);
						Image code128Image = barcode.createAwtImage(Color.BLACK, Color.WHITE);

						DocumentBuilder builder = new DocumentBuilder(pattern);
						builder.moveToBookmark("barCod",true,false);
						BufferedImage bufferedImage = new BufferedImage(code128Image.getWidth(null), code128Image.getHeight(null),  BufferedImage.TYPE_INT_ARGB);
						Graphics2D bGr = bufferedImage.createGraphics();
						bGr.drawImage(code128Image, 0, 0, null);
						bGr.dispose();
						builder.insertImage(bufferedImage,120,80);
						
					}
						
					// Създаване на баркод
					// Insert QR BarCode 
					/*BarcodeQRCode qrCode = new BarcodeQRCode(pkAdressat,1,1,null);
					Image codeQrImage = qrCode.createAwtImage(Color.BLACK, Color.WHITE);
					BufferedImage bufferedImage = new BufferedImage(codeQrImage.getWidth(null), codeQrImage.getHeight(null),  BufferedImage.TYPE_INT_RGB);
			 
					 DocumentBuilder builder = new DocumentBuilder(pattern);
					 builder.moveToBookmark("barCod",true,false);
					 Graphics2D bGr = bufferedImage.createGraphics();
					 bGr.drawImage(codeQrImage, 0, 0, null);
					 bGr.dispose();
					 builder.insertImage(bufferedImage,100, 100); */
				}
							
			
			 	
		} catch (ObjectNotFoundException e) {
	 		LOGGER.error(e.getMessage(), e);
	 		throw new DbErrorException(e.getMessage());
		
		} catch (Exception e) {
			LOGGER.error(e.getMessage(),e);
			throw new DbErrorException(e.getMessage());
		
		} 
		return pattern;
		
	}
	


	public boolean validatePrint() {
		boolean err=true;
		
		if(null == this.getCorespName() || this.getCorespName().trim().equals("")){
			JSFUtils.addMessage(VERIFY_COR_NAME, FacesMessage.SEVERITY_ERROR, "Въведете получател!");
			err=false;
		}
		
		if(null == this.getSenderName() || this.getSenderName().trim().equals("")){
			JSFUtils.addMessage(VERIFY_SEND_NAME, FacesMessage.SEVERITY_ERROR, "Въведете подател!");
			err=false;
		}
		
		if(null == this.getCorespAddress() || this.getCorespAddress().trim().equals("")){
			JSFUtils.addMessage(VERIFY_COR_ADR, FacesMessage.SEVERITY_ERROR, "Въведете Адрес/Пощ.кутия/До поискване на получател!");
			err=false;
		}
		
		if(null == this.getSenderAddress() || this.getSenderAddress().trim().equals("")){
			JSFUtils.addMessage(VERIFY_SEND_ADR, FacesMessage.SEVERITY_ERROR, "Въведете Адрес/Пощ.кутия/До поискване на подател!");
			err=false;
		}
				
		if(null == this.getCorespNM() || this.getCorespNM().trim().equals("")){
			JSFUtils.addMessage(VERIFY_COR_NM, FacesMessage.SEVERITY_ERROR, "Въведете нас.место на получател!");
			err=false;
		}
		
		if(null == this.getSenderNM() || this.getSenderNM().trim().equals("")){
			JSFUtils.addMessage(VERIFY_SEND_NM, FacesMessage.SEVERITY_ERROR, "Въведете нас.место на подател!");
			err=false;
		}
		
		if (null==this.getAdr() || null==getAdr().getAddrCountry() || (this.getAdr().getAddrCountry().equals(this.getCountryBg()) || null==getAdr().getAddrCountry())) {
			
			if(null == this.getSenderPostCode() || this.getSenderPostCode().trim().equals("")){
				JSFUtils.addMessage(VERIFY_SEND_PC, FacesMessage.SEVERITY_ERROR, "Въведете пощ.код на подател!");
				err=false;
			}else if(this.getSenderPostCode().trim().length()!=4) {
				JSFUtils.addMessage(VERIFY_SEND_PC, FacesMessage.SEVERITY_ERROR, "Пощ.код на подател трябва да има 4 цифри!");
				err=false;
			}
			
			if(null == this.getCorespPostCode() || this.getCorespPostCode().trim().equals("")){
				JSFUtils.addMessage(VERIFY_SEND_PC, FacesMessage.SEVERITY_ERROR, "Въведете пощ.код на получател!");
				err=false;
			}else if(this.getCorespPostCode().trim().length()!=4) {
				JSFUtils.addMessage(VERIFY_SEND_PC, FacesMessage.SEVERITY_ERROR, "Пощ.код на получател трябва да има 4 цифри!");
				err=false;
			}
			
			if(null == this.getCorespObl() || this.getCorespObl().trim().equals("")){
				JSFUtils.addMessage(VERIFY_COR_OBL, FacesMessage.SEVERITY_ERROR, "Въведете Област на получател!");
				err=false;
			}
			
			if(null == this.getSenderObl() || this.getSenderObl().trim().equals("")){
				JSFUtils.addMessage(VERIFY_SEND_OBL, FacesMessage.SEVERITY_ERROR, "Въведете Област на подател!");
				err=false;
			}
			
		}
		
//		 Towa ne e OK. Нако трябва да се избере държава, различна от БГ - не е направено както трябва
//
//		if (null!=this.getAdr() && !this.getAdr().getAddrCountry().equals(this.getCountryBg())) {
//			
//			if(null == this.getCorespCountry() || this.getCorespCountry().trim().equals("")){
//				JSFUtils.addMessage(VERIFY_COR_COUNTRY, FacesMessage.SEVERITY_ERROR, "Въведете държава на получател!");
//				err=false;
//			}
//			
//			if(null == this.getSenderCountry() || this.getSenderCountry().trim().equals("")){
//				JSFUtils.addMessage(VERIFY_SEND_COUNTRY, FacesMessage.SEVERITY_ERROR, "Въведете държава на подател!");
//				err=false;
//			}
//			
//		}
		
		return err;
	}

	public ReferentAddress getAdr() {
		return adr;
	}

	public void setAdr(ReferentAddress adr) {
		this.adr = adr;
	}

	public Referent getRef() {
		return ref;
	}

	public void setRef(Referent ref) {
		this.ref = ref;
	}

	public Integer getFormatPlik() {
		return formatPlik;
	}

	public void setFormatPlik(Integer formatPlik) {
		this.formatPlik = formatPlik;
	}

	public boolean isRecommended() {
		return recommended;
	}

	public void setRecommended(boolean recommended) {
		this.recommended = recommended;
	}

	public String getCorespName() {
		return corespName;
	}

	public void setCorespName(String corespName) {
		this.corespName = corespName;
	}

	public String getCorespTel() {
		return corespTel;
	}

	public void setCorespTel(String corespTel) {
		this.corespTel = corespTel;
	}

	public String getCorespAddress() {
		return corespAddress;
	}

	public void setCorespAddress(String corespAddress) {
		this.corespAddress = corespAddress;
	}

	public String getCorespPostCode() {
		return corespPostCode;
	}

	public void setCorespPostCode(String corespPostCode) {
		this.corespPostCode = corespPostCode;
	}

	public String getCorespPBox() {
		return corespPBox;
	}

	public void setCorespPBox(String corespPBox) {
		this.corespPBox = corespPBox;
	}

	public String getCorespObl() {
		return corespObl;
	}

	public void setCorespObl(String corespObl) {
		this.corespObl = corespObl;
	}

	public String getCorespNM() {
		return corespNM;
	}

	public void setCorespNM(String corespNM) {
		this.corespNM = corespNM;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public String getSenderTel() {
		return senderTel;
	}

	public void setSenderTel(String senderTel) {
		this.senderTel = senderTel;
	}

	public String getSenderAddress() {
		return senderAddress;
	}

	public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
	}

	public String getSenderPostCode() {
		return senderPostCode;
	}

	public void setSenderPostCode(String senderPostCode) {
		this.senderPostCode = senderPostCode;
	}

	public String getSenderPBox() {
		return senderPBox;
	}

	public void setSenderPBox(String senderPBox) {
		this.senderPBox = senderPBox;
	}

	public String getSenderObl() {
		return senderObl;
	}

	public void setSenderObl(String senderObl) {
		this.senderObl = senderObl;
	}

	public String getSenderNM() {
		return senderNM;
	}

	public void setSenderNM(String senderNM) {
		this.senderNM = senderNM;
	}

	public Integer getCountryBg() {
		return countryBg;
	}

	public void setCountryBg(Integer countryBg) {
		this.countryBg = countryBg;
	}

	public String getCorespCountry() {
		return corespCountry;
	}

	public void setCorespCountry(String corespCountry) {
		this.corespCountry = corespCountry;
	}

	public String getSenderCountry() {
		return senderCountry;
	}

	public void setSenderCountry(String senderCountry) {
		this.senderCountry = senderCountry;
	}

	public Integer getIdRegistratura() {
		return idRegistratura;
	}

	public void setIdRegistratura(Integer idRegistratura) {
		this.idRegistratura = idRegistratura;
	}

	public Integer getNewCompetence() {
		return newCompetence;
	}

	public void setNewCompetence(Integer newCompetence) {
		this.newCompetence = newCompetence;
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
					 //((int) koresp.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_REF_TYPE] == OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL && getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_SEE_PERSONAL_DATA));
			}
		} catch (DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
		}
		return bb;
	}



	public Map<Integer, Object> getSpecRegistratura() {
		return specRegistratura;
	}

	public void setSpecRegistratura(Map<Integer, Object> specRegistratura) {
		this.specRegistratura = specRegistratura;
	}

	public Object[] getJalbaDanni() {
		return jalbaDanni;
	}

	public void setJalbaDanni(Object[] jalbaDanni) {
		this.jalbaDanni = jalbaDanni;
	}
	
}

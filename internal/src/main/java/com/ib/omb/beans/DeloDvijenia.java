package com.ib.omb.beans;

import java.io.Serializable;
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
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.SelectManyModalA;
import com.ib.indexui.SelectOneModalA;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DeloDvijDAO;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloDvij;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;

/**
 * Актуализация на документ, таб Движения
 *
 * @author n.kanev
 */
@Named("deloDvijenia")
@ViewScoped
public class DeloDvijenia extends IndexUIbean implements Serializable {
	
	@Inject
	Event<DeloDvij> dvijEvent;
	
	
	private static final long serialVersionUID = 55031952536385790L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DeloDvijenia.class);
	private static final int TYPE_SOME_EMAIL = -2;
	private static final int TYPE_SOME_KORESP = -3;
	private static final int TYPE_ADM_STR_ZVENO = -4;
	private static final int TYPE_ADM_STR_LICE = -5;
	private static final String ERROR_KLASSIF = 		"general.errorClassif";
	private static final String NEW_RETURN_TO_DATE = 	"deloForm:calendar-vrashtane";
	private static final String NEW_SEND_DATE = 		"deloForm:calendar-predavane";
	private static final String RETURN_RETURN_DATE = 	"deloForm:calendar-return";
	private static final String RETURN_TEXT_KORESP = 	"deloForm:selectTextReturn";
	private static final String RETURN_ADM_STR = 		"deloForm:selectAdmStrReturn:аutoCompl_input";
	private static final String RETURN_KORESP = 		"deloForm:selectKorespReturn:аutoCompl_input";
	private static final String EDIT_SEND_DATE = 		"deloForm:calendar-edit-predavane";
	private static final String EDIT_RETURN_TO_DATE = 	"deloForm:calendar-edit-vrashtane-do";
	private static final String EDIT_ADM_STR = 			"deloForm:selectAdmStrEdit1:аutoCompl_input";
	private static final String EDIT_KORESP = 			"deloForm:selectKorespEdit1:аutoCompl_input";
	private static final String EDIT_REFERENT_TEKST = 	"deloForm:selectTextEdit1";
	private static final String EDIT_RETURN_DATE = 		"deloForm:calendar-edit-vrashtane";
	private static final String BUTTON_SAVE_NEW = 		"deloForm:btnSaveNewDvij";
	
	private Delo delo;
	private String rnFullDelo;
	
	private List<SelectItem> predavaneTypeList;
	private transient DeloDvijDAO dvijenieDao;
	private List<DeloDvij> dvijenia; // досегашните движения, измъкнати от базата
	private transient List<Dvijenie> newDvijenia; // новите незаписани движения в таблицата
	private DeloDvij tempDvij;
	private Date copyDate;
//	private Date backupDatePredavane;
//	private Date backupDateVrashtaneDo;
//	private Date backupDateVrashtane;
	
	private boolean viewNewPanel = false;
	private boolean viewEditPanel = false;
	private boolean viewReturnPanel = false;
	private Date dataPredavane;
	private Date dataVrashtane;
	private Integer tomNomer;
	private int newMetod = OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA;
	private String newEmailText;
	private String newKorespText;
	private String newPredavaneInfo;
	private List<Integer> selectedAdmStruct = new ArrayList<>();
	private Integer selectedGroupSluj;
	private Integer selectedKoresp;
	private Integer selectedGroupKoresp;
	private transient SelectOneModalA bindGrupiSluj;
	private transient SelectOneModalA bindGrupiKoresp;
	private transient SelectManyModalA bindAdmStruct;
	private transient SelectOneModalA bindEikName;
	private String[] informationText = new String[2];
	private boolean withTomove;
	
	private Map<Integer, Object> specGroup;
	private Map<Integer, Object> specRegistratura; // специфика за група служители - да се филтрират по регистратура - задължително да се подаде sortByName="true"
	private Integer idRefModal = null; //използва се в модалния за разглеждане на данни на физическо/юрид. лице
	
	/**
	 * При отваряне на таба - винаги минава от тук
	 */
	public void initTab() {
		DeloBean bean = (DeloBean) JSFUtils.getManagedBean("deloBean");
		
		if(bean != null) {
			if(!Objects.equals(bean.getRnFullDelo(), this.rnFullDelo)){ // значи за 1-ви път се отваря табчето
				this.specRegistratura = Collections.singletonMap(OmbClassifAdapter.REG_GROUP_INDEX_REGISTRATURA, ((UserData) getUserData()).getRegistratura());
				this.specGroup  = new HashMap<>();
				specGroup.putAll(specRegistratura);
				specGroup.put(OmbClassifAdapter.REG_GROUP_INDEX_TYPE, OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP);
				
				this.rnFullDelo = bean.getRnFullDelo();
				this.delo = bean.getDelo();
				this.newDvijenia = new ArrayList<>();
				this.dvijenieDao = new DeloDvijDAO(getUserData());
				
				try {
					String param1 = ((SystemData) getSystemData()).getSettingsValue("delo.deloWithToms");
					this.withTomove = Objects.equals(param1, String.valueOf(OmbConstants.CODE_ZNACHENIE_DA));
				} catch(DbErrorException e) {
					LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
					this.withTomove = false;
				}
				
				try {
					loadDvijenia();
					this.predavaneTypeList = // премахват се от списъка няколко опции - имейл, сеос, ссев, уеб еау и 'на друга регистратура'
						createItemsList(true, OmbConstants.CODE_CLASSIF_DVIJ_METHOD, new Date(), true)
						.stream()
						.filter(item -> (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS
										&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV
										&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_WEB_EAU
										&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL
										&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA)
						.collect(Collectors.toList());
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
				this.dvijenia = this.dvijenieDao.getDeloDvij(this.delo.getId()));
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
		setInitValues();
	}
	
	/**
	 * Избрана е 'Актуализация' на вече записано движение в таблицата
	 * @param index индексът на реда
	 */
	public void actionBeginEdit(int index) {
		this.tempDvij = getDvijenieCopy(index);
		// тези три променливи са нужни, понеже календарът иначе трие часовете от датите
//		this.backupDatePredavane = tempDvij.getDvijDate(); 
//		this.backupDateVrashtaneDo = tempDvij.getReturnToDate();
//		this.backupDateVrashtane = tempDvij.getReturnDate();
		
		this.viewEditPanel = true;
		this.viewNewPanel = false;
		this.viewReturnPanel = false;
	}
	
	/**
	 * Избран е 'Запис' на актуализацията
	 */
	public void actionSaveEdit() {
		
		if(!validateEdit()) return;
		
		DeloDvij dvij = this.dvijenia.stream().filter(a -> a.getId().equals(this.tempDvij.getId())).findFirst().orElse(null);
		if(dvij == null) return;
		
		// редактирани са само данните за предаване
		if(dvij.getReturnDate() == null) {
			// това странно нещо се прави, защото p:calendar нулира часа на датите в tempDvij
			// часът се пази в променливите backupDatePredavane и backupDateVrashtaneDo и тук се задава обратно в tempDvij
			//this.tempDvij.setDvijDate(this.setTime(this.tempDvij.getDvijDate(), this.backupDatePredavane));
			//this.tempDvij.setReturnToDate(this.setTime(this.tempDvij.getReturnToDate(), this.backupDateVrashtaneDo));
			
			dvij.setDvijDate(this.tempDvij.getDvijDate());
			dvij.setReturnToDate(this.tempDvij.getReturnToDate());
			
			dvij.setDvijMethod(this.tempDvij.getDvijMethod());
			dvij.setCodeRef(this.tempDvij.getCodeRef());
			dvij.setDvijInfo(this.tempDvij.getDvijInfo() != null ? this.tempDvij.getDvijInfo().trim() : null);
			
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
		}
		// редактирани са само данните за връшане
		else {
		//	this.tempDvij.setReturnDate(this.setTime(this.tempDvij.getReturnDate(), this.backupDateVrashtane));
			dvij.setReturnDate(this.tempDvij.getReturnDate());
			dvij.setReturnMethod(this.tempDvij.getReturnMethod());
			dvij.setReturnInfo(this.tempDvij.getReturnInfo());
		}
		
		try {
			JPA.getUtil().runInTransaction(() -> this.dvijenieDao.save(dvij, getSystemData()));
			
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
		
		Date deloDate = this.delo.getDeloDate(); //DateUtils.startDate(this.delo.getDeloDate()); // да се включи и часа
		Date predavane = this.tempDvij.getDvijDate(); //DateUtils.startDate(this.tempDvij.getDvijDate());
		Date vrashtaneDo = this.tempDvij.getReturnToDate(); //DateUtils.startDate(this.tempDvij.getReturnToDate());
		Date vrashtane = this.tempDvij.getReturnDate(); //DateUtils.startDate(this.tempDvij.getReturnDate());
		Date dnes = new Date(); // DateUtils.startDate(new Date());
		
		DeloDvij dvij = this.dvijenia.stream().filter(a -> a.getId().equals(this.tempDvij.getId())).findFirst().orElse(null);
		if(dvij == null) return false;
		
		// редактирани са само данните за предаване
		if(dvij.getReturnDate() == null) {
			// валидиране на датите
			if(predavane == null) {
				JSFUtils.addMessage(EDIT_SEND_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavaneZadaljitelna"));
				scrollToMessages();
				return false;
			}
			
			if(predavane.compareTo(deloDate) < 0) {
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
					&& (this.tempDvij.getDvijText() == null || this.tempDvij.getDvijText().trim().isEmpty())) {
				JSFUtils.addMessage(EDIT_REFERENT_TEKST, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.poleZadaljitelno"));
				scrollToMessages();
				return false;
			}
		}
		
		// редактирани са само данните за връщане
		else {
			if(vrashtane == null) {
				JSFUtils.addMessage(EDIT_RETURN_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneZadaljitelna"));
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
		DeloDvij dvij = this.dvijenia.stream().filter(a -> a.getId().equals(this.tempDvij.getId())).findFirst().orElse(null);
		if(dvij == null) return;
		
		dvij.setReturnDate(null);
		dvij.setReturnCodeRef(null);
		dvij.setReturnTextRef(null);
		dvij.setReturnMethod(null);
		dvij.setReturnInfo(null);
		
		try {
			JPA.getUtil().runInTransaction(() -> this.dvijenieDao.save(dvij, getSystemData()));
			
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
		
		DeloDvij dvij = this.dvijenia.stream().filter(a -> a.getId().equals(this.tempDvij.getId())).findFirst().orElse(null);
		if(dvij == null) return;

		// това странно нещо се прави, защото p:calendar нулира часа на датите в tempDvij
		// часът се пази в променливата backupDateVrashtane и тук се задава обратно в tempDvij
		//this.tempDvij.setReturnDate(this.setTime(this.tempDvij.getReturnDate(), this.backupDateVrashtane));
		
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
			JPA.getUtil().runInTransaction(() -> this.dvijenieDao.save(dvij, getSystemData()));
			
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
		
		Date predavane = this.tempDvij.getDvijDate(); // DateUtils.startDate(this.tempDvij.getDvijDate()); // да включи и часа
		Date vrashtane = this.tempDvij.getReturnDate(); //DateUtils.startDate(this.tempDvij.getReturnDate());
		Date dnes	   = new Date(); //DateUtils.startDate(new Date());
		
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
		
		if(this.useTomove() && this.delo.getWithTom() == OmbConstants.CODE_ZNACHENIE_DA) {
			boolean invalid = this.newDvijenia.stream().anyMatch(dvij -> !this.validateTomNomer(dvij.tomNomer));
			if(invalid) {
				JSFUtils.addMessage(BUTTON_SAVE_NEW, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "deloDvij.imaNevalidenNomerNaTom"));
				scrollToMessages();
				return;
			}
			
		}
		
		if(this.useTomove() && this.newDvijenia.stream().anyMatch(d -> d.tomNomer == null)) {
			PrimeFaces.current().executeScript("PF('hiddenButton').jq.click();");
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
		
		try {
			for(Dvijenie d : this.newDvijenia) {
				
				DeloDvij dvij = new DeloDvij();
				dvij.setCodeRef(d.codeReferent); // CODE_REF
				dvij.setDeloId(this.delo.getId()); // DELO_ID
//				dvij.setDvijDate(setTime(d.dataPredavane, new Date())); // DVIJ_DATE
//				dvij.setReturnToDate(setTime(d.dataVrashtane, new Date())); // RETURN_TO_DATE
				dvij.setDvijDate(d.dataPredavane); // DVIJ_DATE
				dvij.setReturnToDate(d.dataVrashtane); // RETURN_TO_DATE
				dvij.setDvijMethod(d.metodPredavane); // DVIJ_METHOD
				dvij.setDvijText(d.name); // DVIJ_TEXT
				dvij.setDvijInfo(d.information); // DVIJ_INFO
				dvij.setTomNomer(d.getTomNomer()); // TOM_NOMER
				
				JPA.getUtil().runInTransaction(() -> this.dvijenieDao.save(dvij, getSystemData()));
				
				dvijEvent.fireAsync(dvij);
			}
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dvijenie.zapisUspeshen"));
			loadDvijenia();
			
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
		this.newEmailText = "";
		this.newKorespText = "";
		this.newPredavaneInfo = "";
		this.tomNomer = null;
		this.newDvijenia.clear();
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
	 * Направен е избор в полето 'ЕИК, име'.
	 * Накрая изчиства полето.
	 */
	public void actionSelectEikName() {
		if(validateDates(true, true)) {
			addNewDvijeniaByCodeKoresp(Collections.singletonList(this.selectedKoresp));
		}
		// изчиства се стойността от компонента
		this.bindEikName.clearInput();
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
			predavane.setDopInfo(null);
			predavane.setInformation(this.newPredavaneInfo.isEmpty() ? null : this.newPredavaneInfo.trim());
			predavane.setTomNomer(this.tomNomer);
			predavane.setType(TYPE_SOME_KORESP);
			predavane.setMetodPredavane(this.newMetod);
			predavane.setDataPredavane(this.dataPredavane);
			predavane.setDataVrashtane(this.dataVrashtane); // не правя проверка дали е СЕОС или ССЕВ, понеже методът тук не е СЕОС, ССЕВ или Имейл
			
			addNewDvijenie(predavane);
			this.newKorespText = "";
		}
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
		
		Date date = new Date();
		
		try {
			for (int code : codes) {
				SystemClassif empl = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, code, SysConstants.CODE_DEFAULT_LANG, date, true);
				if(empl == null) continue; // ако записът е счупен и липсва в базата
				
				Dvijenie predavane = new Dvijenie();
				
				predavane.setCodeReferent(Integer.valueOf(code));
				predavane.setName(empl.getTekst()); // името
				predavane.setDopInfo(empl.getDopInfo()); // длъжността
				predavane.setInformation(this.newPredavaneInfo.isEmpty() ? null : this.newPredavaneInfo.trim());
				predavane.setTomNomer(this.tomNomer);
				predavane.setType((Integer) empl.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE] == 1 ? TYPE_ADM_STR_ZVENO : TYPE_ADM_STR_LICE);
				predavane.setMetodPredavane(this.newMetod);
				predavane.setDataPredavane(this.dataPredavane);
				predavane.setDataVrashtane(this.dataVrashtane);
				
				addNewDvijenie(predavane);
			}
		} catch(DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
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
				
				predavane.setCodeReferent(code);
				predavane.setName(koresp.getTekst()); // името
				predavane.setDopInfo(dopInfo);
				predavane.setInformation(this.newPredavaneInfo.isEmpty() ? null : this.newPredavaneInfo.trim());
				predavane.setTomNomer(this.tomNomer);
				predavane.setType((int) koresp.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_REF_TYPE]);
				predavane.setMetodPredavane(this.newMetod);
				predavane.setDataPredavane(this.dataPredavane);
				predavane.setDataVrashtane(this.dataVrashtane);
				
				addNewDvijenie(predavane);
			}
		} catch(DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}
	
	/**
	 * Ако избраният референт го няма вече в таблицата, го добавя.
	 * Иначе може да покаже съобщение.
	 */
	private void addNewDvijenie(Dvijenie d) {

		if(!this.newDvijenia.contains(d)) {
			this.newDvijenia.add(d);
		}
		else {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
					String.format(getMessageResourceString(beanMessages, "dvijenie.poluchatelVaveden"), 
							(d.codeReferent == null) ? d.name :	decodeCodeRefName(d.codeReferent)));
			scrollToMessages();
		}
	}
	
	/**
	 * Трие избран референт от таблицата
	 */
	public void actionDeleteNewDvij(int index){
		this.newDvijenia.remove(index);
	}
	
	/**
	 * Изтрива вече въведено движение в горната таблица
	 */
	public void actionDeleteDvij(int index) {
		DeloDvij d = this.dvijenia.get(index);
		try {
			JPA.getUtil().runInTransaction(() -> this.dvijenieDao.delete(d, (SystemData) getSystemData()));
			loadDvijenia();
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dvijenie.iztrito"));

			// ако се опитваме да изтрием движение, което в момента е отворено за редакция/връщане, скрива панела 
			if(this.tempDvij != null && this.tempDvij.getId().intValue() == d.getId().intValue()) {
				this.viewEditPanel = false;
				this.viewReturnPanel = false;
			}
		} catch (BaseException e) {
			LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaIztrivane"), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.greshkaIztrivane"));
			scrollToMessages();
		}
	}
	
	/**
	 * Дали избраният метод е 'През системата'
	 */
	public boolean selectedMetodSystem() {
		return this.newMetod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA;
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
			// въведен е ръчно имейл в полето и никой не знае кой стои зад него
			case TYPE_SOME_EMAIL: return "fas fa-at";
			// въведен е ръчно текст в полето
			case TYPE_SOME_KORESP: return "far fa-user";
			case TYPE_ADM_STR_LICE:	return "fas fa-user";
			case TYPE_ADM_STR_ZVENO: return "fas fa-laptop-house";
			// неочакван случай - нито е от адм. стр., нито е кореспондент, нито произволен имейл
			default : return "fas fa-user";
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
	 * Валидира датата на предаване да не е по-ранна от датата на документа 
	 */
	public boolean validateDataPredavane() {		
		
		Date predavane = this.dataPredavane; //DateUtils.startDate(this.dataPredavane);
		Date dokument = this.delo.getDeloDate(); //DateUtils.startDate(this.delo.getDeloDate());
		Date dnes =  new Date();; //DateUtils.startDate(new Date());
		
		if(predavane == null) {
			JSFUtils.addMessage(NEW_SEND_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavaneZadaljitelna"));
			this.dataPredavane = new Date();
			scrollToMessages();
			return false;
		}
		if(dnes.compareTo(predavane) < 0) { // датата на предаване е по-късна от днешната
			JSFUtils.addMessage(NEW_SEND_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavaneBadeshta"));
			this.dataPredavane = new Date();
			this.dataVrashtane = null;
			scrollToMessages();
			return false;
		}
		if(predavane.compareTo(dokument) < 0) { // датата на предаване е преди тази на документа
			JSFUtils.addMessage(NEW_SEND_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavanePrediDokument"));
			this.dataPredavane = new Date();
			this.dataVrashtane = null;
			scrollToMessages();
			return false;
		}

		return true;
	}
	
	/**
	 * Валидира датата на връщане ДО да не е по-ранна от датата на предаване
	 */
	public boolean validateDataVrashtaneDo() {
		
		Date vrashtane = this.dataVrashtane; //DateUtils.startDate(this.dataVrashtane);
		Date predavane =  this.dataPredavane; //DateUtils.startDate(this.dataPredavane);
		
		if (vrashtane == null) return true; 
		
		if(vrashtane.compareTo(predavane) < 0) { // датата на връщане е преди тази на предаване
			JSFUtils.addMessage(NEW_RETURN_TO_DATE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneRanna"));
		//	this.dataVrashtane = this.dataPredavane;
			scrollToMessages();
			return false;
		}

		return true;
	}
	
	/**
	 * Преди да се извърши редакция на ред запазва данните на движението.
	 * Използва се, за да върне оригиналната дата, ако се сложи нещо невалидно.
	 * @param dvij
	 */
	public void onBeginRowEdit(Dvijenie dvij) {
		this.copyDate = new Date(dvij.getDataPredavane().getTime());
	}
	
	/**
	 * Вика се при завършване на редакция на таблицата с нови движения. Валидира двете дати и екземпляра.
	 */
	public void onRowEdit(Dvijenie dvij) {
		boolean valid = true;
		
		valid &= validatePredavane(dvij);
		valid &= validateVrashtaneDo(dvij);
		valid &= validateTomNomer(dvij);		
		
		if(!valid) {
			scrollToMessages();
		}
	}
	
	/**
	 * Вика се, след като се промени стойността на полето за номер на екземпляр. Валидира и изкарва съобщение.
	 */
	public void validateTomNomerInput() {
		if(!validateTomNomer(this.tomNomer)) {
			this.tomNomer = null;
			JSFUtils.addMessage("deloForm:inputTomNomer", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "deloDvij.nevalidenNomerNaTom"));
			scrollToMessages();
		}
	}
	
	/**
	 * Валидира при редактиране на датите директно в таблицата да не се допускат грешки
	 */
	private boolean validatePredavane(Dvijenie dvij) {
		
		Date predavane = dvij.dataPredavane; //DateUtils.startDate(dvij.dataPredavane); // да включи и часа!
		Date dokument  = this.delo.getDeloDate(); //DateUtils.startDate(this.delo.getDeloDate());
		Date dnes      = new Date(); //DateUtils.startDate(new Date());
		
		if(dnes.compareTo(predavane) < 0) { // датата на предаване е по-късна от днешната (бъдеща)
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavaneBadeshta"));
			dvij.dataPredavane = this.copyDate;
			dvij.dataVrashtane = null;
			return false;
		}
		if(predavane.compareTo(dokument) < 0) { // датата на предаване е преди тази на документа
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavanePrediDokument"));
			dvij.dataPredavane = this.copyDate;
			dvij.dataVrashtane = null;
			return false;
		}
		
		return true;
	}
	
	/**
	 * Валидира при редактиране на датите директно в таблицата да не се допускат грешки
	 */
	private boolean validateVrashtaneDo(Dvijenie dvij) {
		
		Date vrashtane = dvij.dataVrashtane; //DateUtils.startDate(dvij.dataVrashtane); // да се вкл. и часа
		Date predavane = dvij.dataPredavane; //DateUtils.startDate(dvij.dataPredavane);
		
		if(vrashtane != null && vrashtane.compareTo(predavane) < 0) { // датата на връщане е преди тази на предаване
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneDoRanna"));
			dvij.dataVrashtane = null;
			return false;
		}
		
		return true;
	}
	
	/**
	 * Валидира номера на том в таблицата преди запис на новите движения
	 */
	private boolean validateTomNomer(Dvijenie dvij) {
		
		boolean valid = true;
		
		if(this.useTomove() && this.delo.getWithTom() == OmbConstants.CODE_ZNACHENIE_DA) {
			if(!validateTomNomer(dvij.tomNomer)) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "deloDvij.nevalidenNomerNaTom"));
				valid = false;
			}
		}
		
		return valid;
	}
	
	/**
	 * Валидира полето 'Номер на том'
	 */
	public boolean validateTomNomer(Integer tom) {
		
		// това позволено ли е?
		// if(this.delo.getBrTom() == null) {
		// 	return true;
		// }
		
		if(tom == null) {
			return true;
		}

		if(tom < 1 || tom > this.delo.getBrTom()) {
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
	private DeloDvij getDvijenieCopy(int index) {
		DeloDvij dvij = this.dvijenia.get(index);
		DeloDvij copy = new DeloDvij();
		
		copy.setId(dvij.getId());
		copy.setCodeRef(dvij.getCodeRef());
		copy.setDvijDate(dvij.getDvijDate());
		copy.setTomNomer(dvij.getTomNomer());
		copy.setDvijInfo(dvij.getDvijInfo());
		copy.setDvijMethod(dvij.getDvijMethod());
		copy.setDvijText(dvij.getDvijText());
		copy.setReturnCodeRef(dvij.getReturnCodeRef());
		copy.setReturnDate(dvij.getReturnDate());
		copy.setReturnInfo(dvij.getReturnInfo());
		copy.setReturnMethod(dvij.getReturnMethod());
		copy.setReturnTextRef(dvij.getReturnTextRef());
		copy.setReturnToDate(dvij.getReturnToDate());
		
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
	 * Вика функцията scrollToErrors на страницата, за да се скролне екранът към съобщенията за грешка.
	 * Сложено е, защото иначе съобщенията може да са извън видимия екран и user изобшо да не разбере,
	 * че е излязла грешка, и каква.
	 */
	private void scrollToMessages() {
		PrimeFaces.current().executeScript("scrollToErrors()");
	}
	
	/**
	 * Взима от първия аргумент дата, месец, година, а от втория параметър час, минути, секунди, милисекунди.
	 * @param dateTo датата, от която взима дата
	 * @param dateTime датата, от която взема часа
	 * @return
	 */
//	private Date setTime(Date dateDate, Date dateTime) {
//		if(dateDate == null) {
//			return null;
//		}
//		if(dateTime == null) {
//			dateTime = new Date();
//		}
//		
//		Calendar result = new GregorianCalendar();
//		Calendar time = new GregorianCalendar();
//		result.setTime(dateDate);
//		time.setTime(dateTime);
//		
//		result.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
//		result.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
//		result.set(Calendar.SECOND, time.get(Calendar.SECOND));
//		result.set(Calendar.MILLISECOND, time.get(Calendar.MILLISECOND));
//		
//		return result.getTime();
//	}
//	
	/**
	 * Връша true, ако системната настойка за работа с томове е ДА
	 */
	public boolean useTomove() {
		return this.withTomove;
	}
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */	
	
	public class Dvijenie {
		private Integer codeReferent;
		private String name;
		private Integer tomNomer;
		private String dopInfo; // за лицата от адм. структ е длъжността, за кореспондентите е информацията им
		private String information;
		private int type; // тип - нфл или фзл
		private int metodPredavane;
		private Date dataPredavane;
		private Date dataVrashtane;

		/**
		 * Когато се добавя ново изпращане тук се проверява дали вече не е добавен такъв запис.
		 * Например, за да не се изпраща няколко пъти към един и същ имейл или получател.
		 */
		@Override
		public boolean equals(Object obj) {
			if(obj == null) return false;
			if (obj == this) return true;
	        if (!(obj instanceof Dvijenie)) {
	            return false;
	        }
	        Dvijenie d = (Dvijenie) obj;
	        
	        if (this.codeReferent == null && d.codeReferent == null) { // ако кодовете са null, значи са вкарани ръчно имейли/корепонденти
        		if(this.name != null && d.name != null) { // значи са ръчно вкарани кореспонденти
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
			return Objects.hash(this.codeReferent, this.name);
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

		public Integer getTomNomer() {
			return tomNomer;
		}

		public void setTomNomer(Integer tomNomer) {
			this.tomNomer = tomNomer;
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
	}


	public Delo getDelo() {
		return delo;
	}

	public void setDelo(Delo delo) {
		this.delo = delo;
	}

	public String getRnFullDelo() {
		return rnFullDelo;
	}

	public void setRnFullDelo(String rnFullDelo) {
		this.rnFullDelo = rnFullDelo;
	}

	public List<SelectItem> getPredavaneTypeList() {
		return predavaneTypeList;
	}

	public void setPredavaneTypeList(List<SelectItem> predavaneTypeList) {
		this.predavaneTypeList = predavaneTypeList;
	}

	public List<DeloDvij> getDvijenia() {
		return dvijenia;
	}

	public void setDvijenia(List<DeloDvij> dvijenia) {
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

	public String getNewEmailText() {
		return newEmailText;
	}

	public void setNewEmailText(String newEmailText) {
		this.newEmailText = newEmailText;
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

	public DeloDvij getTempDvij() {
		return tempDvij;
	}

	public void setTempDvij(DeloDvij tempDvij) {
		this.tempDvij = tempDvij;
	}

	public String[] getInformationText() {
		return informationText;
	}

	public void setInformationText(String[] informationText) {
		this.informationText = informationText;
	}
	
	public void setInformationText(String header, String info) {
		this.informationText = new String[] {header, info};
	}

	public Integer getTomNomer() {
		return tomNomer;
	}

	public void setTomNomer(Integer tomNomer) {
		this.tomNomer = tomNomer;
	}

	public boolean isWithTomove() {
		return withTomove;
	}

	public void setWithTomove(boolean withTomove) {
		this.withTomove = withTomove;
	}
	
	/** @return the specGroup */
	public Map<Integer, Object> getSpecGroup() {
		return this.specGroup;
	}

	public Map<Integer, Object> getSpecRegistratura() {
		return specRegistratura;
	}

	public void setSpecRegistratura(Map<Integer, Object> specRegistratura) {
		this.specRegistratura = specRegistratura;
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

	public Integer getIdRefModal() {
		return idRefModal;
	}

	public void setIdRefModal(Integer idRefModal) {
		this.idRefModal = idRefModal;
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
	
}

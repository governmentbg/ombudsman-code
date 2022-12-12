package com.ib.omb.beans;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.indexui.system.Constants.CODE_CLASSIF_BUSINESS_ROLE;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRATURI;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRI;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_DELOVODITEL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.mail.MessagingException;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.beans.UserEdit;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.indexui.db.dto.AdmUserRole;
import com.ib.indexui.system.Constants;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.ReferentDAO;
import com.ib.omb.db.dto.Referent;
import com.ib.omb.quartz.SendMailJob;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.mail.Mailer;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.X;

/**
 * Предоставя разширение на стандартния екран за потребител
 *
 * @author belev
 */
@Named("userEdit")
@ViewScoped
public class ExtendedUserEdit extends UserEdit {

	/**  */
	private static final long serialVersionUID = 8923973254687230924L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedUserEdit.class);

	private boolean freeAcces; // ако флага е вдигнат значи при неизбрани регистри се дава право до всички

	private String				registratura;		// член на регистратура
	private List<SelectItem>	registri;			// всички възможни регистри за основната регистратурата
	private List<String>		selectedRegistri;	// кодовете на избраните регистри за основната регистратурата
	private List<Integer>	    registerIdList;	    // за множествен избор на регистър	
	private List<SystemClassif>	selRegistersList;
	private String				admStructInfo;		// къде е назначен и длъжността

	private boolean delovoditel; // дали е деловодител

	private List<SelectItem>	registraturiList;			// списък с регистратури, които могат да се добавят
	private boolean				showAddRegistraturiButton;	// за бутончето

	/** key=ИД Регистратура, value=списък с избрани регистри */
	private Map<Integer, List<Integer>> selectedRegistraturi; // избраните, които може да обслужва като деловодител
	
	private Integer				registraturaForEdit;	// избраната на екрана (от допълнителните) за добавяне на регистри
	private List<Integer>		selectedRegistriForEdit;
	private List<Integer>	    registerForEditList;	    

	private Map<Integer, Object> specificsEmployeesOnly;
	
	// за пощенските кутии към потребителя
	private List<Object[]> usersMailBoxes;
	// регистрите само за текушата регистратура + общите
	private Map<Integer, Object> specificsRegister;  
	// регистрите само за избраната допълнителна регистратура + общите
	private Map<Integer, Object> specificsRegisterForEdit;  
	
	private List<Integer> buzinessRoleList;
	private List<SystemClassif> buzinessRoleClassif;	
	
	// ще се вдигне флага ако логина е настроен през лдап и тогава на екрана и в логиката има специфики заради паролите
	private boolean ldapLogin;

	private boolean quitUser; // ще се вдигне ако е в режим на корекция и потребителя е напуснал
	
	private boolean refreshNotif; // за да се знае след запис дали има пипано в класифиакцията с активни нотификации
	private boolean refreshSetting; // за да се знае след запис дали има пипано в потребителските настройки
	
	/** 
	 * това е името на лицето, което се показва в режим актуализация на екрана причините да е така са:
	 * 1. За активни потребители се разкодира през класификацията на Админ Структурата, а там се вижда и длъжността
	 * 2. За неактивни потребители се използва класификацията за Напуснали, а там се вижда до коя дата е напуснал
	 * 3. За мигрирани потребители, които не са изобщо в админ структурата ще се вижда името от adm_users
	 */
	private String liceNames;
	
	/** */
	@Override
	@PostConstruct
	protected void initData() {
		super.initData();

		try {
			if (getClassifList() != null) {
				getClassifList().add(new SelectItem(OmbConstants.CODE_CLASSIF_USER_SETTINGS, getSystemData().getNameClassification(OmbConstants.CODE_CLASSIF_USER_SETTINGS, getCurrentLang())));
				getClassifList().add(new SelectItem(OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE, getSystemData().getNameClassification(OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE, getCurrentLang())));
			}

//			this.ldapLogin = OmbConstants.LOGIN_TYPE_LDAP.equals(getSystemData().getSettingsValue(OmbConstants.LOGIN_TYPE));
			if (this.ldapLogin) {
				setChangePass(false);
			}
			
			this.quitUser = getUser().getId() != null && !getSystemData().matchClassifItems(Constants.CODE_CLASSIF_ADMIN_STR, getUser().getId(), getCurrentDate());
			
			if (getUser().getId() != null) {
				if (getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, getUser().getId(), getCurrentDate())) {

					this.liceNames = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, getUser().getId(), getCurrentLang(), getCurrentDate());
				
				} else {
					this.liceNames = getUser().getNames();
				}
			} else {
				this.liceNames = null;
			}

			if (this.quitUser) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString(beanMessages, "extendedUserEdit.quitUser"));
			}

			this.specificsEmployeesOnly = Collections.singletonMap(OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE, X.of(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL));

			this.freeAcces = false;//isClassifFreeAccess(CODE_CLASSIF_REGISTRI);

			this.registri = new ArrayList<>();
			this.selectedRegistri = new ArrayList<>();
			this.registerIdList = new ArrayList<>();			
			this.selRegistersList = new ArrayList<>();
			this.usersMailBoxes = new ArrayList<>();

			this.selectedRegistraturi = new LinkedHashMap<>();
			
			this.buzinessRoleList = new ArrayList<>();
			this.buzinessRoleClassif = new ArrayList<>();
			
			Set<Integer> otherRegistraturi = new HashSet<>(); // да имам кодовете на допълнителните регистратури			

			Integer id = getUser().getReferentId() != null ? getUser().getReferentId() : getUser().getId();
			Integer registraturaId = (Integer) getSystemData().getItemSpecific(CODE_CLASSIF_ADMIN_STR, id, getCurrentLang(), getCurrentDate(), OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);

			if (registraturaId == null && this.quitUser) {
				try {
					SystemClassif item = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, id, getCurrentLang(), getCurrentDate(), false);
					if (item != null && item.getCodeExt() != null && item.getCodeExt().length() > 0) {
						registraturaId = Integer.valueOf(item.getCodeExt());
					}
				} catch (Exception e) {
					LOGGER.error("Грешка при определяне на регистратура на напуснал служител!", e);
				}
			}

			if (getUser().getId() != null) {
				getUser().setReferentId(getUser().getId());
			}

			if (registraturaId != null) { // ако e член на регистратура зареждаме регистрите
				this.registratura = getSystemData().decodeItem(CODE_CLASSIF_REGISTRATURI, registraturaId, getCurrentLang(), getCurrentDate());

				// зареждаме регистрите за основната регистратура
				this.registri.addAll(getRegistersByRegistratura(registraturaId, true));
				
				this.specificsRegister = Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA, registraturaId);
				
				this.selRegistersList = new ArrayList<>();
				
				List<SystemClassif> tmpLst = new ArrayList<>();
				
				if (!this.registerIdList.isEmpty()) {
					
					for (Integer reg : this.registerIdList) {
						
						SystemClassif scItem = new SystemClassif();
						
						scItem.setCodeClassif(OmbConstants.CODE_CLASSIF_REGISTRI);
						scItem.setCode(reg);
						scItem.setTekst(getSystemData().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, reg, getUserData().getCurrentLang(), new Date()));
						
						tmpLst.add(scItem);						
					}					
				}
				
				setSelRegistersList(tmpLst); 
			}

			// ако има административна структура
			this.admStructInfo = getSystemData().decodeItemDopInfo(CODE_CLASSIF_ADMIN_STR, id, getCurrentLang(), getCurrentDate());
			if (this.quitUser && SearchUtils.isEmpty(this.admStructInfo)) {
				this.admStructInfo = getSystemData().decodeItemDopInfo(OmbConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, id, getCurrentLang(), getCurrentDate());
			}

			for (AdmUserRole userRole : getUser().getUserRoles()) { // проверка дали е деловодител
				
				if (userRole.getCodeClassif().equals(CODE_CLASSIF_BUSINESS_ROLE)) {
					
					this.buzinessRoleList.add(userRole.getCodeRole());
					
					if(userRole.getCodeRole().equals(CODE_ZNACHENIE_BUSINESS_ROLE_DELOVODITEL)) {
						this.delovoditel = true;
					}
				}
				
			}
				
			if (!this.buzinessRoleList.isEmpty()) {
				
				List<SystemClassif> tmpLst = new ArrayList<>();
				
				for (Integer role : this.buzinessRoleList) {
					
					SystemClassif scItem = new SystemClassif();
					
					scItem.setCodeClassif(OmbConstants.CODE_CLASSIF_BUSINESS_ROLE);
					scItem.setCode(role);
					scItem.setTekst(getSystemData().decodeItem(Constants.CODE_CLASSIF_BUSINESS_ROLE, role, getUserData().getCurrentLang(), new Date()));
					
					tmpLst.add(scItem);						
				}	
				
				setBuzinessRoleClassif(tmpLst);		
			}
			

			// зареждаме всички регистратури и премахваме основната за човека и тези, до които той вече има достъп
			this.registraturiList = createItemsList(false, CODE_CLASSIF_REGISTRATURI, getCurrentDate(), true);

			// достъп през тези от правата
			List<Integer> userRegistraturi = getUser().getUserRoles().stream().filter(a -> a.getCodeClassif().equals(CODE_CLASSIF_REGISTRATURI)).map(AdmUserRole::getCodeRole)
				.collect(Collectors.toList());
			if (registraturaId != null && !userRegistraturi.contains(registraturaId)) { // +основната
				userRegistraturi.add(registraturaId);
			}

			Iterator<SelectItem> iterator = this.registraturiList.iterator();
			while (iterator.hasNext()) {
				SelectItem item = iterator.next();
				if (userRegistraturi.contains(item.getValue())) {
					iterator.remove();
				}
			}
			if (registraturaId == null) {
				this.showAddRegistraturiButton = this.registraturiList.size() > 1;
			} else {
				this.showAddRegistraturiButton = !this.registraturiList.isEmpty(); // ако няма повече трябва бутона да не се вижда
																					// за добавяне на нова
			}

			if (this.delovoditel) {
				for (AdmUserRole role : getUser().getUserRoles()) {
					if (!role.getCodeClassif().equals(CODE_CLASSIF_REGISTRATURI)) {
						continue; // другите не ме интересуват
					}
					
					otherRegistraturi.add(role.getCodeRole());

					List<SelectItem> allRegistri = getRegistersByRegistratura(role.getCodeRole(), false);

					Set<Integer> value = new HashSet<>();
					if (allRegistri != null) {
						List<Integer> allRegistriInt = allRegistri.stream().map(x -> Integer.valueOf((String) x.getValue())).collect(Collectors.toList());

						for (AdmUserRole registriRole : getUser().getUserRoles()) {
							if (registriRole.getCodeClassif().equals(CODE_CLASSIF_REGISTRI) && allRegistriInt.contains(registriRole.getCodeRole())) {
								value.add(registriRole.getCodeRole());
							}
						}
					}
					this.selectedRegistraturi.put(role.getCodeRole(), new ArrayList<>(value));
				}
			}
			
			List<SystemClassif> mailBoxes = getSystemData().getFullClassification(OmbConstants.CODE_CLASSIF_MAILBOXES, getCurrentLang(), true);			 
			
			for (SystemClassif sc : mailBoxes) {
				
				Object[] mailbox = new Object[4];

				mailbox[0] = sc.getCode();
				mailbox[1] = sc.getTekst();
				mailbox[2] = SearchUtils.asInteger(sc.getSpecifics()[OmbClassifAdapter.MAILBOXES_INDEX_REGISTRATURA]);
				mailbox[3] = Boolean.FALSE;
				
				for (AdmUserRole role : getUser().getUserRoles()) {
					if (role.getCodeClassif().equals(OmbConstants.CODE_CLASSIF_MAILBOXES)) {
						if(role.getCodeRole().equals(mailbox[0])) {
							mailbox[3] = Boolean.TRUE;
							break;
						}
					}				
				}
				
				if (registraturaId != null && registraturaId.equals(mailbox[2])) {					
					this.usersMailBoxes.add(mailbox);					
				}
				
				if (this.delovoditel && !otherRegistraturi.isEmpty()) {
					for (Integer regId : otherRegistraturi) {
						if (regId.equals(mailbox[2])) {							
							this.usersMailBoxes.add(mailbox);	
						}
					}
				}
			}

		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		}
	}

	/** При избор от модалния, ако се избере деловодител да се покаже избора на допълнителни регистратури */
	public void changeDelovoditel() {
		
		if(this.buzinessRoleList.contains(CODE_ZNACHENIE_BUSINESS_ROLE_DELOVODITEL)) {
			this.delovoditel = true;
		} else {
			this.delovoditel = false;
		}

		/** Превключването на бутона да/не */
//		if (this.delovoditel) {
//			getUser().getUserRoles().add(new AdmUserRole(CODE_CLASSIF_BUSINESS_ROLE, CODE_ZNACHENIE_BUSINESS_ROLE_DELOVODITEL));
//		} else {
//			for (AdmUserRole userRole : getUser().getUserRoles()) {
//				if (userRole.getCodeClassif().equals(CODE_CLASSIF_BUSINESS_ROLE) && userRole.getCodeRole().equals(CODE_ZNACHENIE_BUSINESS_ROLE_DELOVODITEL)) {
//					getUser().getUserRoles().remove(userRole);
//					break;
//				}
//			}
//		}	
	
	}

	/** Избора на друга допълнителна регистратира */
	public void confirmChangeRegistratura() {
		for (Entry<Integer, List<Integer>> entry : this.selectedRegistraturi.entrySet()) {
			if (entry.getKey().equals(this.registraturaForEdit)) {
				entry.setValue(this.selectedRegistriForEdit);
				break;
			}
		}	
	}
	
	/** */
	@Override
	public void loadAdmStrData() {
		try {
			AdmUser user = getDao().findById(getUser().getReferentId());
			if (user != null) { // намерен е и ще се отваря в редакция
				JSFUtils.addFlashScopeValue("objectID", user.getId());
				initData();

				setChangePass(false);

			} else { // нов ще се прави

				JSFUtils.addFlashScopeValue("refID", getUser().getReferentId());
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, getUser().getReferentId(), getCurrentLang(), getCurrentDate(), OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);
				
				if (refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL))) {
				
					initData();
	
					String mail = (String) getSystemData().getItemSpecific(CODE_CLASSIF_ADMIN_STR, getUser().getReferentId(), getCurrentLang(), getCurrentDate(),
						OmbClassifAdapter.ADM_STRUCT_INDEX_CONTACT_EMAIL);
					getUser().setEmail(mail);

					@SuppressWarnings("unchecked")
					List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery(
						"select REF_ID, REF_NAME, DATE_OT from ADM_REFERENTS where CODE = ? order by DATE_OT desc, REF_ID desc")
						.setParameter(1, getUser().getReferentId()).setMaxResults(1).getResultList();
					if (rows.isEmpty()) { // ако по някаква чудна причина се окаже че няма си остава по стария начин
						getUser().setNames(getSystemData().decodeItem(CODE_CLASSIF_ADMIN_STR, getUser().getReferentId(), getCurrentLang(), getCurrentDate()));
					} else {
						getUser().setNames((String) rows.get(0)[1]);
					}
				
				} else {
					
					getUser().setReferentId(null);
					JSFUtils.addMessage("formUserEdit" + ":chooseAdmStr:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "extendedUserEdit.plsChoiceOnlySluj"));
					
				}
			}

			PrimeFaces.current().ajax().update("formUserEdit");

		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/** */
	@Override
	public void newUser(int mode) {
		super.newUser(mode);

		if (this.ldapLogin) {
			setChangePass(false); // така за нов няма да има полетата с паролите
		}
		this.quitUser = false;
		this.liceNames = null;

		try {
			getUser().setStatus(Constants.CODE_ZNACHENIE_STATUS_ACTIVE); // така ще се правят новите
			this.registratura = null;
			this.registri = new ArrayList<>();
			this.selectedRegistri = new ArrayList<>();
			this.admStructInfo = null;

			this.delovoditel = false;

			this.registraturiList = createItemsList(false, CODE_CLASSIF_REGISTRATURI, getCurrentDate(), true);
			this.showAddRegistraturiButton = this.registraturiList.size() > 1;

			this.selectedRegistraturi = new LinkedHashMap<>();

			this.registraturaForEdit = null;
			this.selectedRegistriForEdit = new ArrayList<>();
			this.usersMailBoxes = new ArrayList<>();
			this.setTypePanelData(1); 
			
			this.buzinessRoleList = new ArrayList<>();
			this.buzinessRoleClassif = new ArrayList<>();

			Integer refID = (Integer) JSFUtils.getFlashScopeValue("refID");
			if (refID != null) {
				getUser().setReferentId(refID);
			}

		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"), e);
			LOGGER.error(e.getMessage(), e);
		}
	}

	/** @param code */
	public void removeRegistratura(Integer code) {
		for (Entry<Integer, List<Integer>> entry : this.selectedRegistraturi.entrySet()) {
			if (entry.getKey().equals(code)) {
				this.selectedRegistraturi.remove(entry.getKey()); // махам от избраните

				try { // добавя се като възможност за избор
					SelectItem item = new SelectItem(code, decodeItem(CODE_CLASSIF_REGISTRATURI, code, getCurrentDate()));

					this.registraturiList.add(item);
					
					List<Object[]> removeMailboxes = new ArrayList<>();
						
						for (Object[] obj : this.usersMailBoxes) {						
							
							if (item.getValue().equals(obj[2])) {								
								removeMailboxes.add(obj);			
							}
						}
						
						for (Object[] remove : removeMailboxes) {
							this.usersMailBoxes.remove(remove);
						}

				} catch (DbErrorException e) {
					JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"), e);
					LOGGER.error(e.getMessage(), e);
				}
				break;
			}
		}
	}

	/** @param item */
	public void selectRegistratura() {

		Iterator<SelectItem> iterator = this.registraturiList.iterator();
		while (iterator.hasNext()) {
			SelectItem temp = iterator.next();

			if (temp.getValue().equals(this.registraturaForEdit)) { // намираме подадената в списъка

				this.selectedRegistraturi.put((Integer) temp.getValue(), null); // добавяме я към избраните		
				
				this.specificsRegisterForEdit = Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA, this.registraturaForEdit);
				
				try {
					
					List<SystemClassif> mailBoxes = getSystemData().getFullClassification(OmbConstants.CODE_CLASSIF_MAILBOXES, getCurrentLang(), true);
					
					for (SystemClassif sc : mailBoxes) {
						
						Object[] mailbox = new Object[4];
		
						mailbox[0] = sc.getCode();
						mailbox[1] = sc.getTekst();
						mailbox[2] = SearchUtils.asInteger(sc.getSpecifics()[OmbClassifAdapter.MAILBOXES_INDEX_REGISTRATURA]);
						mailbox[3] = Boolean.FALSE;
										
						if (temp.getValue().equals(mailbox[2])) {
							
							boolean existMail = false;
							for (Object[] mail : this.usersMailBoxes) {
								if (mail[0].equals(mailbox[0])) {
									existMail = true;									
								}
							}
							if (!existMail) {
								 this.usersMailBoxes.add(mailbox);
							}
												
						}
					}
				
				} catch (DbErrorException e) {
					JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"), e);
					LOGGER.error(e.getMessage(), e);
				}
				
				iterator.remove(); // и я премахваме от списъка в popup-а
				break;
			}
		}
	}

	/**
	 * При натикскане на бутона за избор на регистри за допълнителни регистртури
	 * @param idRegistratura
	 */
	public void loadSpecificsDopReg(Integer idRegistratura) {
		this.registraturaForEdit = idRegistratura;
		this.specificsRegisterForEdit = Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA, idRegistratura);
		this.registerForEditList = new ArrayList<>();
		
		for (Entry<Integer, List<Integer>> row : this.selectedRegistraturi.entrySet()) {
			if (row.getKey().equals(idRegistratura)) {
				this.registerForEditList = row.getValue();
				break;
			}
		}
	}
	
	/**
	 * При натикскане на бутона за избор на всички регистри за допълнителни регистртури
	 * @param idRegistratura
	 */
	public void loadAllRegistriForDopReg(Integer idRegistratura) {
		this.registraturaForEdit = idRegistratura;
		
		try {
			
			List<SelectItem> allRegistri = getRegistersByRegistratura(idRegistratura, false);
		
			if (allRegistri != null) {
				List<Integer> allRegistriInt = allRegistri.stream().map(x -> Integer.valueOf((String) x.getValue())).collect(Collectors.toList());
				
				this.registerForEditList = new ArrayList<>();
				
				for (Integer integer : allRegistriInt) { 
					this.registerForEditList.add(integer);
				}
				
				this.selectedRegistraturi.put(idRegistratura, this.registerForEditList);
				
				this.registraturaForEdit = null;
				
			}
		
		} catch (DbErrorException e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"), e);
			LOGGER.error(e.getMessage(), e);
		}

	}
	
	/**
	 * Връща данните за избраните регистри при затваряне на модалния
	 * @param idRegistratura
	 */
	public void selRegisterDopReg() {
		
		selectedRegistraturi.put(this.registraturaForEdit, this.registerForEditList);
		
		this.registraturaForEdit = null;
	}
	
	public void actionSetRegistriForReg() {
		
		for (Entry<Integer, List<Integer>> row : this.selectedRegistraturi.entrySet()) {
			if (row.getKey().equals(this.registraturaForEdit)) {
				this.selectedRegistriForEdit = row.getValue();
				break;
			}
		}		
	}	
	
	/**
	 * При промяна на типа документ
	 * 
	 * @return
	 */
	public void actionChangeTypePanel(ValueChangeEvent event) {
		Integer newTypeDoc = (Integer) event.getNewValue();
		this.setTypePanelData(newTypeDoc);
		LOGGER.debug("actionChangeTypePanel = {} ", newTypeDoc);	
	}

	

	/** */
	@Override
	protected Set<Integer> selectUserRoles() {
		Set<Integer> changed = super.selectUserRoles();

		this.refreshNotif = changed != null && changed.contains(OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE);
		this.refreshSetting = changed != null && changed.contains(OmbConstants.CODE_CLASSIF_USER_SETTINGS);

		try {
			// всички възможни регистри за главната регистратура
			List<Integer> mainRegistri = this.registri.stream().map(a -> Integer.valueOf((String) a.getValue())).collect(Collectors.toList());

			Set<Integer> rolesRegistri = new HashSet<>(); // регистрите през правата на потребителя
			Set<Integer> rolesRegistraturi = new HashSet<>(); // регистратурите през правата на потребителя
			
			Set<Integer> rolesMailBoxes = new HashSet<>(); // пощенски кутии през правата на потребителя
			
			Set<Integer> rolesBuzinessRole = new HashSet<>(); // бизнес роли за потребителя

			Iterator<AdmUserRole> iterator = getUser().getUserRoles().iterator();
			while (iterator.hasNext()) {
				AdmUserRole role = iterator.next();

				if (role.getCodeClassif().equals(CODE_CLASSIF_REGISTRI)) {

					if (mainRegistri.contains(role.getCodeRole()) && !this.selectedRegistri.contains(String.valueOf(role.getCodeRole()))) {
						iterator.remove(); // този явно е деселектнат и в послдествие ще се трие

					} else {
						rolesRegistri.add(role.getCodeRole());
					}

				} else if (role.getCodeClassif().equals(CODE_CLASSIF_REGISTRATURI)) {
					rolesRegistraturi.add(role.getCodeRole());
				
				} else if (role.getCodeClassif().equals(OmbConstants.CODE_CLASSIF_MAILBOXES)) {
					
					rolesMailBoxes.add(role.getCodeRole());
				
				} else if (role.getCodeClassif().equals(OmbConstants.CODE_CLASSIF_BUSINESS_ROLE)) {
					
					rolesBuzinessRole.add(role.getCodeRole());					
				}
			}

			for (String selected : this.selectedRegistri) { // новодобавените регистри
				Integer codeRole = Integer.valueOf(selected);

				if (!rolesRegistri.contains(codeRole)) {
					getUser().getUserRoles().add(new AdmUserRole(CODE_CLASSIF_REGISTRI, codeRole));
					rolesRegistri.add(codeRole); // поддържам го и това актуално
				}
			}
			
			for (Object[] obj : this.usersMailBoxes) {
				Integer codeRole = (Integer) obj[0]; 
				
				if (!rolesMailBoxes.contains(codeRole) && Boolean.TRUE.equals(obj[3])) {
					getUser().getUserRoles().add(new AdmUserRole(OmbConstants.CODE_CLASSIF_MAILBOXES, codeRole));
					rolesMailBoxes.add(codeRole);					
				}
			}
			
			for (Integer codeRole : this.buzinessRoleList) {
				
				if (!rolesBuzinessRole.contains(codeRole)) {
					getUser().getUserRoles().add(new AdmUserRole(OmbConstants.CODE_CLASSIF_BUSINESS_ROLE, codeRole));
					rolesBuzinessRole.add(codeRole);					
				}
			}

			// тука надолу кода е в случаите на повече от една регистратура

			boolean clearRegistraturi = false; // за да се знае че не трябва да останат избрани допълнителни регистратури и
												// регистри

			if (this.delovoditel) { // за деловодител може да има допълнителни регистратури съответно регистри

				if (this.selectedRegistraturi.isEmpty()) { // щом няма нито една избрана ще се чистят всички
					clearRegistraturi = true;

				} else {

					Set<Integer> selectedRegistraturiCodes = new HashSet<>(); // допълнителните
					Set<Integer> selectedRegistriCodes = new HashSet<>(); // допълнителните

					// добавяне на новоизбрани регистратури и регистри
					for (Entry<Integer, List<Integer>> entry : this.selectedRegistraturi.entrySet()) {
						Integer key = entry.getKey(); // регистратура

						selectedRegistraturiCodes.add(key);

						if (!rolesRegistraturi.contains(key)) {
							getUser().getUserRoles().add(new AdmUserRole(CODE_CLASSIF_REGISTRATURI, key));
							rolesRegistraturi.add(key); // поддържам го и това актуално
						}

						List<Integer> value = entry.getValue(); // регистри
						if (value != null) {
							for (Integer selected : value) {
								Integer codeRole = selected;

								selectedRegistriCodes.add(codeRole);

								if (!rolesRegistri.contains(codeRole)) {
									getUser().getUserRoles().add(new AdmUserRole(CODE_CLASSIF_REGISTRI, codeRole));
									rolesRegistri.add(codeRole); // поддържам го и това актуално
								}
							}
						}
					}

					// премахваме ненужните регистратури и регистри
					Iterator<AdmUserRole> iterator2 = getUser().getUserRoles().iterator();
					while (iterator2.hasNext()) {
						AdmUserRole role = iterator2.next();

						// намираме премахнатата регистратура
						if (role.getCodeClassif().equals(CODE_CLASSIF_REGISTRATURI) && !selectedRegistraturiCodes.contains(role.getCodeRole())) {
							iterator2.remove();
						} else if (role.getCodeClassif().equals(CODE_CLASSIF_REGISTRI) && !selectedRegistriCodes.contains(role.getCodeRole()) && !mainRegistri.contains(role.getCodeRole())) {
							iterator2.remove();
						}
					}
				}
			} else { // ако не е деловодител се махат всички допълнителни регистратури, съответно регистри
				clearRegistraturi = true;
			}

			if (clearRegistraturi) { // или не е деловодител или всичко е премахнато
				Iterator<AdmUserRole> iterator2 = getUser().getUserRoles().iterator();
				while (iterator2.hasNext()) {
					AdmUserRole userRole = iterator2.next();

					if (userRole.getCodeClassif().equals(CODE_CLASSIF_REGISTRATURI) // регистратурите
						// или регистри, които ги няма в основната регистратура
						|| userRole.getCodeClassif().equals(CODE_CLASSIF_REGISTRI) && !mainRegistri.contains(userRole.getCodeRole())) {
						iterator2.remove();
					}
				}
			}
			
			Iterator<AdmUserRole> iterator3 = getUser().getUserRoles().iterator();
			while (iterator3.hasNext()) {
				AdmUserRole userRole = iterator3.next();
				if (userRole.getCodeClassif().equals(OmbConstants.CODE_CLASSIF_MAILBOXES))  {
					for (Object[] obj : this.usersMailBoxes) {						
						
						if (userRole.getCodeRole().equals(SearchUtils.asInteger(obj[0])) && Boolean.FALSE.equals(obj[3])) {
							iterator3.remove();	
							break;
						}
					}						
				}
			}
			
			Iterator<AdmUserRole> iterator4 = getUser().getUserRoles().iterator();
			while (iterator4.hasNext()) {
				AdmUserRole userRole = iterator4.next();
				if (userRole.getCodeClassif().equals(OmbConstants.CODE_CLASSIF_BUSINESS_ROLE))  {						
					if (!this.buzinessRoleList.contains(userRole.getCodeRole())) {
						iterator4.remove();	
						break;
					}				
				}
			}

		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"), e);
			LOGGER.error(e.getMessage(), e);
		}
		
		return changed;
	}

	private List<SelectItem> getRegistersByRegistratura(Integer registraturaId, boolean mainRegistratura) throws DbErrorException {
		String specific = (String) getSystemData().getItemSpecific(CODE_CLASSIF_REGISTRATURI, registraturaId, getCurrentLang(), getCurrentDate(), OmbClassifAdapter.REGISTRATURI_INDEX_REGISTRI);
		if (SearchUtils.isEmpty(specific)) {
			return new ArrayList<>();
		}

		List<SelectItem> items = new ArrayList<>();

		String[] registersIds = specific.split(",");

		for (String r : registersIds) {
			String name = getSystemData().decodeItem(CODE_CLASSIF_REGISTRI, Integer.valueOf(r), getCurrentLang(), getCurrentDate());
			items.add(new SelectItem(r, name));
		}

		if (mainRegistratura) {

			for (AdmUserRole userRole : getUser().getUserRoles()) {

				if (userRole.getCodeClassif().equals(CODE_CLASSIF_REGISTRI)) {
					String codeRole = String.valueOf(userRole.getCodeRole());

					for (String r : registersIds) {

						if (userRole.getCodeRole().equals(Integer.valueOf(r)) && !this.selectedRegistri.contains(codeRole)) {
							this.selectedRegistri.add(codeRole);
						}
						
						if (userRole.getCodeRole().equals(Integer.valueOf(r)) && !this.registerIdList.contains(userRole.getCodeRole())) {
							this.registerIdList.add(userRole.getCodeRole());
						}
					}
				}
			}
		}
		return items;
	}
	
	@Override
	public void actionSave() {
		boolean newUser = getUser().getId() == null;
		String newPass = isChangePass() ? SearchUtils.trimToNULL(getPassPlain()) : null;
		String email = SearchUtils.trimToNULL(getUser().getEmail());
		
		super.actionSave();
		
		syncAdmStruct(email);

		try {
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_MAILBOXES, false, false);
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при рефреш на касифиакция Пощенски кутии (159)", e);
		} 

		if (this.refreshNotif) {
			((SystemData) getSystemData()).setUserNotifications(null);
		}

		if ((this.refreshNotif || this.refreshSetting) 
			&& getUser().getId().equals(getUserData(UserData.class).getUserAccess())) { // става много интересно защото редактира себе си
			
			List<Integer> selectedClassif = new ArrayList<>(); // и трябва да се рефрешне в узер датата
			if (this.refreshNotif) {
				selectedClassif.add(OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE);
				getUserData().getAccessValues().remove(OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE);
			}
			if (this.refreshSetting) {
				selectedClassif.add(OmbConstants.CODE_CLASSIF_USER_SETTINGS);
				getUserData().getAccessValues().remove(OmbConstants.CODE_CLASSIF_USER_SETTINGS);
			}

			try {
				Map<Integer, Map<Integer, Boolean>> replacment = getDao().findUserAccessMap(getUser().getId(), selectedClassif);
				
				getUserData().getAccessValues().putAll(replacment);
			
			} catch (Exception e) {
				LOGGER.error("Грешка при синхронизиране на избрани настройки.", e);
			} finally {
				JPA.getUtil().closeConnection();
			}
		}

		try {
			if (email != null && (newUser || newPass != null)) {
				String setting = getSystemData().getSettingsValue("delo.newUserSendMail");
				
				boolean error = false; // ще се занимаваме с мейлите само ако няма грешка от записа
				List<FacesMessage> msgList = FacesContext.getCurrentInstance().getMessageList();
				if (msgList != null && !msgList.isEmpty()) {
					for (FacesMessage msg : msgList) {
						if (FacesMessage.SEVERITY_ERROR.equals(msg.getSeverity())) {
							error = true;
							break;
						}
					}
				}

				if (!error && "1".equals(setting)) {
					
					if (newUser) {
						StringBuilder text = new StringBuilder();
						text.append("Здравейте, Вие сте регистриран(а) като потребител на система „е-Омбудсман“ с потребителско име ");
						text.append(getUser().getUsername());
						if (this.ldapLogin) {
							text.append(".");
						} else {
							text.append(" и парола "+newPass+".");
						}

						sendMail(email, "Регистриран потребител", text.toString());
					
					} else if (newPass != null) {
						sendMail(email
							, "Промяна на парола"
							, "Здравейте, Вашата парола за вход в система „е-Омбудсман“ е променена. Новата Ви парола е "+newPass+".");
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Грешка при изпращане на мейл до потребител!", e);
		}
	}

	/**
	 * @param email
	 */
	private void syncAdmStruct(String email) {
		List<FacesMessage> msgList = FacesContext.getCurrentInstance().getMessageList();
		if (msgList != null && !msgList.isEmpty()) {
			for (FacesMessage msg : msgList) {
				if (FacesMessage.SEVERITY_ERROR.equals(msg.getSeverity())) {
					return; // има грешка от записа и няма как да се случи това надолу
				}
			}
		}

		try {
			SystemClassif emplItem = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, getUser().getId(), getCurrentLang(), getCurrentDate(), true);
			if (emplItem == null) {
				return;
			}
			this.liceNames = emplItem.getTekst();

			if (!Objects.equals(email, emplItem.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_CONTACT_EMAIL])) {
				// има разлика и трябва да се синхроницира мейла
				ReferentDAO dao = new ReferentDAO(getUserData());

				JPA.getUtil().runInTransaction(()-> {
					Referent referent = dao.findByCode(getUser().getId(), getCurrentDate(), true);
					if (referent != null) {
						referent.setContactEmail(email);
						dao.save(referent);
					}
				});

				getSystemData().reloadClassif(Constants.CODE_CLASSIF_ADMIN_STR, false, false);
				getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, false, false);
			}
		} catch (Exception e) {
			LOGGER.error("Грешка при синхронизиране на данни с административната структура", e);
		} 
	}
	
	private void sendMail(String to, String subject, String body) throws DbErrorException, InvalidParameterException, MessagingException {
		try {
	        Properties mailProps = new Properties();
	        for(String mailProperty : SendMailJob.mailPropertyKeys){
	            String mailPropertyValue = getSystemData().getSettingsValue(mailProperty);

	            if(mailPropertyValue != null) {
	                mailProps.put(mailProperty, mailPropertyValue);
	            }
	        }
	        new Mailer().sendHTMLMail(mailProps, to, subject, body, null);
			
	        JSFUtils.addInfoMessage("Успешно изпратен email за "+subject+" до " + to);
	        
		} catch (DbErrorException | InvalidParameterException | MessagingException e) { // хващам само за да кажа на усера че мейла не е станал
			JSFUtils.addMessage(null, FacesMessage.SEVERITY_WARN
				, "Възникна грешка при изпращане на email за "+subject+" до " + to, e.getMessage());
			throw e; // и който го вика да се оправя
		}
    }

	public void actionSelectRegister() {	
		
		if (this.selRegistersList != null) {

			this.selectedRegistri.clear();

			for (SystemClassif reg : this.selRegistersList) {

				this.selectedRegistri.add(SearchUtils.asString(reg.getCode()));
			}
		}
	}
	
	public void actionSelectALLRegisters() {	
		
		if (this.selRegistersList != null) { 

			this.selectedRegistri.clear();
			this.selRegistersList.clear();

			for (SelectItem reg : this.registri) {

				this.selectedRegistri.add(SearchUtils.asString(reg.getValue()));
				
				SystemClassif sc = new SystemClassif();
				sc.setTekst(reg.getLabel());
				String code = SearchUtils.asString(reg.getValue());			    
				sc.setCode(Integer.valueOf(code)); 
				
				this.selRegistersList.add(sc);
				
			}
		}
	}
	
	public void actionRemoveRegister(Integer reg) {
		
		if (reg != null) {
			
			this.selectedRegistri.remove(SearchUtils.asString(reg));
		}		
	}	
	
	/** @return the admStructInfo */
	public String getAdmStructInfo() {
		return this.admStructInfo;
	}

	/** */
	@Override
	public String getDivStatusExplainClass() {
		return DIV_CLASS_P_COL_8;
	}

	/** @return the registratura */
	public String getRegistratura() {
		return this.registratura;
	}

	/** @return the registraturiList */
	public List<SelectItem> getRegistraturiList() {
		return this.registraturiList;
	}

	/** @return the registri */
	public List<SelectItem> getRegistri() {
		return this.registri;
	}

	public Map<Integer, List<Integer>> getSelectedRegistraturi() {
		return selectedRegistraturi;
	}

	public void setSelectedRegistraturi(Map<Integer, List<Integer>> selectedRegistraturi) {
		this.selectedRegistraturi = selectedRegistraturi;
	}

	/** @return the selectedRegistri */
	public List<String> getSelectedRegistri() {
		return this.selectedRegistri;
	}

	/** @return the selectedRegistriForEdit */
	public List<Integer> getSelectedRegistriForEdit() {
		return this.selectedRegistriForEdit;
	}

	/** @return the specificsEmployeesOnly */
	public Map<Integer, Object> getSpecificsEmployeesOnly() {
		return this.specificsEmployeesOnly;
	}

	/** @return the delovoditel */
	public boolean isDelovoditel() {
		return this.delovoditel;
	}

	/**  */
	@Override
	public boolean isExtended() {
		return true;
	}

	/** @return the freeAcces */
	public boolean isFreeAcces() {
		return this.freeAcces;
	}

	/** */
	@Override
	public boolean isRenderUserType() {
		return false; // за деловодството няма да има вид потребител в този екран
	}

	/** @return the showAddRegistraturiButton */
	public boolean isShowAddRegistraturiButton() {
		return this.showAddRegistraturiButton;
	}

	/** @param delovoditel the delovoditel to set */
	public void setDelovoditel(boolean delovoditel) {
		this.delovoditel = delovoditel;
	}

	/** @param selectedRegistri the selectedRegistri to set */
	public void setSelectedRegistri(List<String> selectedRegistri) {
		this.selectedRegistri = selectedRegistri;
	}

	public List<Integer> getRegisterIdList() {
		return registerIdList;
	}

	public void setRegisterIdList(List<Integer> registerIdList) {
		this.registerIdList = registerIdList;
	}

	/** @param selectedRegistriForEdit the selectedRegistriForEdit to set */
	public void setSelectedRegistriForEdit(List<Integer> selectedRegistriForEdit) {
		this.selectedRegistriForEdit = selectedRegistriForEdit;
	}
	
	public Integer getRegistraturaForEdit() {
		return registraturaForEdit;
	}

	public void setRegistraturaForEdit(Integer registraturaForEdit) {
		this.registraturaForEdit = registraturaForEdit;
	}

	public List<Object[]> getUsersMailBoxes() {
		return usersMailBoxes;
	}

	public void setUsersMailBoxes(List<Object[]> usersMailBoxes) {
		this.usersMailBoxes = usersMailBoxes;
	}
	
	
	public Map<Integer, Object> getSpecificsRegister() {
		return specificsRegister;
	}

	public void setSpecificsRegister(Map<Integer, Object> specificsRegister) {
		this.specificsRegister = specificsRegister;
	}

	public Map<Integer, Object> getSpecificsRegisterForEdit() {
		return specificsRegisterForEdit;
	}

	public void setSpecificsRegisterForEdit(Map<Integer, Object> specificsRegisterForEdit) {
		this.specificsRegisterForEdit = specificsRegisterForEdit;
	}

	public List<Integer> getRegisterForEditList() {
		return registerForEditList;
	}

	public void setRegisterForEditList(List<Integer> registerForEditList) {
		this.registerForEditList = registerForEditList;
	}

	public List<SystemClassif> getSelRegistersList() {
		return selRegistersList;
	}

	public void setSelRegistersList(List<SystemClassif> selRegistersList) {
		this.selRegistersList = selRegistersList;
	}

	public List<Integer> getBuzinessRoleList() {
		return buzinessRoleList;
	}

	public void setBuzinessRoleList(List<Integer> buzinessRoleList) {
		this.buzinessRoleList = buzinessRoleList;
	}

	public List<SystemClassif> getBuzinessRoleClassif() {
		return buzinessRoleClassif;
	}

	public void setBuzinessRoleClassif(List<SystemClassif> buzinessRoleClassif) {
		this.buzinessRoleClassif = buzinessRoleClassif;
	}

	/** @return the ldapLogin */
	public boolean isLdapLogin() {
		return this.ldapLogin;
	}
	
	/** @return the quitUser */
	public boolean isQuitUser() {
		return this.quitUser;
	}

	/** @return the liceNames */
	public String getLiceNames() {
		return this.liceNames;
	}
}
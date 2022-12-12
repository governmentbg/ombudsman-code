package com.ib.omb.beans;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_BOSS_POSITIONS;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRATURI;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRI;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELEGATES_ZAMESTVANE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO;
import static com.ib.system.SysConstants.CODE_CLASSIF_USERS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.navigation.NavigationDataHolder;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.ReferentDAO;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.omb.system.UserData.DelegatedEmployee;
import com.ib.omb.utils.DashboardUtils;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectNotFoundException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.SysClassifUtils;
import com.ib.system.utils.X;

/**
 * В този клас са показани начините за достъпване на х-ки от т.н. потребителски контекст.
 *
 * @author belev
 */
@Named
@SessionScoped
public class UserContext extends IndexUIbean {

	/**  */
	private static final long serialVersionUID = 1400459123152353504L;

	private static final Logger LOGGER = LoggerFactory.getLogger(UserContext.class);

	private List<SelectItem>	registraturiDelovoditel;	// Регистратури, които обслужва като деловодител

	private boolean customNotif;
	private List<SelectItem> optionsNotifList;
	private Integer [] selectedNotifList;
		
	private Integer [] selectedNstrLst;
	private List<SelectItem> optionsNstrLst;
	
	private boolean ldapLogin;

	private Set<Integer> activeTaskStatus;
	
	/**  */
	public UserContext() {
		try {
//			this.ldapLogin = OmbConstants.LOGIN_TYPE_LDAP.equals(getSystemData().getSettingsValue(OmbConstants.LOGIN_TYPE));
	
			List<Integer> statuses = getSystemData().getSysClassificationCodes(OmbConstants.CODE_CLASSIF_TASK_STATUS_ACTIVE, null, SysConstants.CODE_DEFAULT_LANG);
			this.activeTaskStatus = new HashSet<>(statuses);

		} catch (DbErrorException e) { //
			LOGGER.error("ldapLogin check error!", e);
		}
	}

	/** @return the activeTaskStatus */
	public Set<Integer> getActiveTaskStatus() {
		return this.activeTaskStatus;
	}

	/** Делегиране на права от избрания служител към текущия логнат в системата */
	public void actionDelegate() {
		LOGGER.debug("- actionDelegate() -");

		UserData ud = getUserData(UserData.class);
		SystemData sd = getSystemData(SystemData.class);

		ud.setOtImetoNa(null); // надолу ще се изчисли правилно
		
		String setting = null;
		try { // това казва кои класификации трябва да се подменят при това действие!!!
			setting = getSystemData().getSettingsValue("delo.classificationsForUserReplacement");
			if (setting == null) {
				throw new ObjectNotFoundException("SETTING=delo.classificationsForUserReplacement is not defined!");
			}
		} catch (BaseException e) {
			ud.setUserAccess(ud.getUserId());
			ud.setUserSave(ud.getUserId());

			LOGGER.error(e.getMessage(), e);
			JSFUtils.addErrorMessage(e.getMessage(), e);

			return;
		}

		// първо трябва да намеря кой елемент е избран от списъка със замествания
		Optional<DelegatedEmployee> emp = ud.getDelegatedEmployees().stream().filter(x -> Objects.equals(ud.getUserAccess(), x.getCodeRef())).findFirst();
		if (!emp.isPresent()) {
			ud.setUserAccess(ud.getUserId());
			ud.setUserSave(ud.getUserId());

			LOGGER.error("DelegatedEmployee with codeRef={} not found!", ud.getUserAccess());
			JSFUtils.addErrorMessage("DelegatedEmployee with codeRef=" + ud.getUserAccess() + " not found!");

			return;
		}

		X<Map<Integer, Map<Integer, Boolean>>> mapPack = X.empty();
		try { // намирам правата по избрания служител
			JPA.getUtil().runWithClose(() -> mapPack.set(new AdmUserDAO(ud).findUserAccessMap(emp.get().getCodeRef())));

			if (!mapPack.isPresent()) {
				throw new ObjectNotFoundException("Access map for user " + emp.get().getCodeRef() + " is not found!");
			}

		} catch (BaseException e) {
			ud.setUserAccess(ud.getUserId());
			ud.setUserSave(ud.getUserId());

			LOGGER.error(e.getMessage(), e);
			JSFUtils.addErrorMessage(e.getMessage(), e);

			return;
		}

		if (!ud.getUserId().equals(ud.getUserAccess())) {
			ud.setOtImetoNa(emp.get().getNameRef());
		}
		
		Map<Integer, Map<Integer, Boolean>> accessMap = mapPack.get();

		// TODO това тука никак не е добре. ако има как да се опрости !!!

		if (accessMap.get(CODE_CLASSIF_REGISTRI) != null) { // !!! това е много важно, защото запазвам реалните права дадени
															// до регистри под ключ с отрицателна стойност. това ми позволява
															// в последствие да мога да подменям регистрите спрямо
															// регистратура, като винаги знам оригиналния набор !!!
			ud.getAccessValues().put(CODE_CLASSIF_REGISTRI * -1, new HashMap<>(accessMap.get(CODE_CLASSIF_REGISTRI)));
		}

		if (Objects.equals(emp.get().getCodeType(), CODE_ZNACHENIE_DELEGATES_ZAMESTVANE)) {

			ud.setUserSave(ud.getUserId()); // при заместване потребителя работи от свое име
		} else {
			ud.setUserSave(ud.getUserAccess()); // при упълномощаване потребителя работи от името на упълномощаващия
		}

		// подменям правата спрямо този който се замества
		Stream.of(setting.split(",")).map(Integer::parseInt).forEach(x -> ud.getAccessValues().put(x, accessMap.get(x)));

		Date decodeDate = DateUtils.startDate(new Date());

		try {
			ud.setRegistratura((Integer) sd.getItemSpecific(CODE_CLASSIF_ADMIN_STR, ud.getUserAccess(), getCurrentLang(), decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA));

			sd.filterDocVidByNoPageReg(ud.getAccessValues()); // оправям и видовете документи

			// оправям и регистрите
			sd.filterRegistersByRegistratura(ud.getRegistratura(), ud.getAccessValues());
			sd.calculateAdditionalRegistraturi(ud, ud.getAccessValues());

			// сетвам звената и подчинените звена на userData от избрания userAccess
			SystemClassif row = sd.decodeItemLite(CODE_CLASSIF_ADMIN_STR, ud.getUserAccess(), getCurrentLang(), decodeDate, true);
			if (row != null) {
				ud.setRegistratura((Integer) row.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA]);
				ud.setZveno(row.getCodeParent()); // CODE_PARENT се явява звеното на човека

				Integer position = (Integer) row.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_POSITION];

				StringBuilder accessZvenoList = new StringBuilder();

				if (position != null && ud.getZveno() != null) {
					boolean boss = sd.matchClassifItems(CODE_CLASSIF_BOSS_POSITIONS, position, decodeDate);
					if (boss) { // трябва да се изтеглят и звената до които има достъп !!!

						// трябват ми само звената и за това пускам през специфика да се отрежат
						Map<Integer, Object> specifics = Collections.singletonMap(OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE, CODE_ZNACHENIE_REF_TYPE_ZVENO);

						List<SystemClassif> items = sd.queryClassification(CODE_CLASSIF_ADMIN_STR, null, decodeDate, ud.getCurrentLang(), specifics);
						List<SystemClassif> zvena = SysClassifUtils.getChildren(items, ud.getZveno(), null);

						accessZvenoList.append(ud.getZveno());

						for (SystemClassif zveno : zvena) {
							accessZvenoList.append("," + zveno.getCode());
						}
					}
				}
				
				if (accessZvenoList.length() > 0) {
					ud.setAccessZvenoList(accessZvenoList.toString());
				} else { // важно е ако новия няма да се зачистят на стария
					ud.setAccessZvenoList(null);
				}
			}
			
			// вдигане на флаговете за изрично отнетия достъп
			Number docCount = (Number) JPA.getUtil().getEntityManager().createNativeQuery( //
				"select count (*) cnt from DOC_ACCESS where CODE_REF = :codeRef and EXCLUDE = :da") //
				.setParameter("codeRef", ud.getUserAccess()).setParameter("da", SysConstants.CODE_ZNACHENIE_DA) //
				.getResultList().get(0);
			ud.setDocAccessDenied(docCount.intValue() > 0);	

			Number deloCount = (Number) JPA.getUtil().getEntityManager().createNativeQuery( //
				"select count (*) cnt from DELO_ACCESS where CODE_REF = :codeRef and EXCLUDE = :da") //
				.setParameter("codeRef", ud.getUserAccess()).setParameter("da", SysConstants.CODE_ZNACHENIE_DA) //
				.getResultList().get(0);
			ud.setDeloAccessDenied(deloCount.intValue() > 0);	

			Number eventsCount = (Number) JPA.getUtil().getEntityManager().createNativeQuery( //
				"select count (*) cnt from EVENTS e inner join EVENT_REFERENTS r on r.EVENT_ID = e.EVENT_ID where r.CODE_REF = :codeRef and e.DATE_DO >= :currentDate") //
				.setParameter("codeRef", ud.getUserAccess()).setParameter("currentDate", new Date()) //
				.getResultList().get(0);
			if (eventsCount != null && eventsCount.intValue() > 0) {
				ud.setEventsCount(eventsCount.intValue());
			} else {
				ud.setEventsCount(null);
			}

		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addErrorMessage(e.getMessage(), e);
		}

		this.registraturiDelovoditel = null; // за да се презаредят списъците
	}

	/** Смяна на регистратурата, в която работи в случай на деловодите */
	public void actionDelovoditelChangeRegistratura() {
		LOGGER.debug("- actionDelovoditelChangeRegistratura() -");

		UserData ud = getUserData(UserData.class);
		SystemData sd = getSystemData(SystemData.class);

		try {
			Date decodeDate = DateUtils.startDate(new Date());

			sd.filterRegistersByRegistratura(ud.getRegistratura(), ud.getAccessValues());

			ud.setMailboxNames(null); // ако има ще се изчислят правилно спрямо новата регистратура
			
			// позволените за потребителя пощенски кутии
			Map<Integer, Boolean> userMailboxes = ud.getAccessValues().get(OmbConstants.CODE_CLASSIF_MAILBOXES);
			if (userMailboxes != null && !userMailboxes.isEmpty()) { // само ако има зададени

				// пощенските кутии за регистратурата
				List<SystemClassif> items = sd.queryClassification(OmbConstants.CODE_CLASSIF_MAILBOXES, null, decodeDate, getCurrentLang()
					, Collections.singletonMap(OmbClassifAdapter.MAILBOXES_INDEX_REGISTRATURA, ud.getRegistratura()));
				
				List<String> mailboxNames = new ArrayList<>();
				for (SystemClassif item : items) { // добавям само тези, за които има достъп за текущата му регистратура
					if (userMailboxes.containsKey(item.getCode())) {
						mailboxNames.add(item.getTekst());
					}
				}
				if (!mailboxNames.isEmpty()) {
					ud.setMailboxNames(mailboxNames);
				}
			}
			
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addErrorMessage(e.getMessage(), e);

			return;
		}

		this.registraturiDelovoditel = null; // за да се презаредят списъците
	}

	//----------------------------------------------------------------------------------
	private boolean delovoditelChangeRegistraturaChange;
	private boolean delegateChange;
	private boolean showModalNastr;
	
	public void actionDelovoditelChangeRegistraturaChange() {
		delovoditelChangeRegistraturaChange = true;
	}
	
	public void  actionDelegateChange() {
		delegateChange = true;
	}
	
	public void actionSaveNastrUesr() {
		
		
//		System.out.println("actionSaveNastrUesr->  "+selectedNstrLst.length);
//		for(Integer test: selectedNstrLst) {
//			System.out.println("selectedNstrLst: "+test);
//		}
//		
//		System.out.println("actionSaveNastrUesr->  "+selectedNotifList.length);
//		for(Integer test: selectedNotifList) {
//			System.out.println("selectedNotifLst: "+test);
//		}
		
		if(delegateChange)
			actionDelegate();
		
		if(delovoditelChangeRegistraturaChange)
			actionDelovoditelChangeRegistratura();
		
		
		try {
			UserData ud = getUserData(UserData.class);
			Map<Integer, Map<Integer, Boolean>> rez = new HashMap<>();
			
			X<Boolean> refreshSettings = X.of(Boolean.FALSE);
			X<Boolean> refreshNotifs = X.of(Boolean.FALSE);
			
			JPA.getUtil().runInTransaction(() -> rez.putAll(new ReferentDAO(ud).saveUserSettings(ud.getUserId(),  Arrays.asList(selectedNstrLst), Arrays.asList(selectedNotifList), getSystemData(), refreshSettings, refreshNotifs)));
			
			if (refreshSettings.isPresent() && refreshSettings.get()) {
				getSystemData().reloadClassif(CODE_CLASSIF_USERS, false, false);
			}
			if (refreshNotifs.isPresent() && refreshNotifs.get()) {
				((SystemData) getSystemData()).setUserNotifications(null);
			}

			ud.getAccessValues().putAll(rez); //за да ги презареди в УД
			
		} catch (BaseException e1) {
			LOGGER.error(e1.getMessage(), e1);
			JSFUtils.addErrorMessage(e1.getMessage(), e1);
		}
		
		
		ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
		try {
			context.redirect(context.getRequestContextPath() + "/pages/dashboard.xhtml");
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			actionClearNastrUesr();
		}
		
	}
	
	public void actionClearNastrUesr() {
		delegateChange = false;
		delovoditelChangeRegistraturaChange = false;
		showModalNastr = false;
		
		selectedNotifList = null;
		selectedNstrLst = null;
	}
	
	public void actionShowModalNstr() {
		showModalNastr = true;
		
		UserData ud = getUserData(UserData.class);
		
		if(selectedNotifList ==null) {
			
			List<Integer> selectedNotifListTmp = new ArrayList<>();
			Map<Integer, Boolean> userNotifList = ud.getAccessValues().get(Integer.valueOf(OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE));
			
			if(userNotifList != null) {
			
				for(Entry<Integer, Boolean> entry  :userNotifList.entrySet()) {
					
					//System.out.println("entry -> "+entry.getKey() +" / value->"+entry.getValue());
					if(entry.getValue() !=null && entry.getValue().booleanValue()) {
						selectedNotifListTmp.add(entry.getKey());
					}
				}
			}
			selectedNotifList = new Integer[selectedNotifListTmp.size()];
			selectedNotifList = selectedNotifListTmp.toArray(selectedNotifList);
			
			//System.out.println("selectedNotifList--> "+selectedNotifList.length);
			
			customNotif=false;
			if(selectedNotifList.length != getOptionsNotifList().size()) {
				customNotif = true;
			}
			
		}
		if(selectedNstrLst ==null) {
			
			//selectedNstrLst = new Integer[0];
			List<Integer> selectedNstrListTmp = new ArrayList<>();
			Map<Integer, Boolean> userNastrList = ud.getAccessValues().get(Integer.valueOf(OmbConstants.CODE_CLASSIF_USER_SETTINGS));
			if(userNastrList!=null ) {
				for(Entry<Integer, Boolean> entry  :userNastrList.entrySet()) {
					System.out.println("entry -> "+entry.getKey() +" / value->"+entry.getValue());
					if(entry.getValue() !=null && entry.getValue().booleanValue()) {
						selectedNstrListTmp.add(entry.getKey());
					}
				}
			}
			selectedNstrLst = new Integer[selectedNstrListTmp.size()];
			selectedNstrLst = selectedNstrListTmp.toArray(selectedNstrLst);
			
			
		}
		
	}
	//-------------------------------------------------------------------------------------
	
	/** @return the registraturiDelovoditel */
	public List<SelectItem> getRegistraturiDelovoditel() {
		if (this.registraturiDelovoditel == null) {
			try {
				this.registraturiDelovoditel = createItemsList(true, CODE_CLASSIF_REGISTRATURI, new Date(), null);
			} catch (BaseException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return this.registraturiDelovoditel;
	}

	public boolean isShowModalNastr() {
		return showModalNastr;
	}

	public void setShowModalNastr(boolean showModalNastr) {
		this.showModalNastr = showModalNastr;
	}

	public List<SelectItem> getOptionsNotifList() {
		if(optionsNotifList == null) {
			
			try {
				optionsNotifList =   createItemsList(false, OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE,  new Date(), false);  //getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE , new Date(), getCurrentLang());
			} catch (DbErrorException | UnexpectedResultException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		
		return optionsNotifList;
	}

	public void setOptionsNotifList(List<SelectItem> optionsNotifList) {
		this.optionsNotifList = optionsNotifList;
	}

	public boolean isCustomNotif() {
		return customNotif;
	}

	public void setCustomNotif(boolean customNotif) {
		this.customNotif = customNotif;
	}

	public Integer [] getSelectedNotifList() {
		return selectedNotifList;
	}

	public void setSelectedNotifList(Integer [] selectedNotifList) {
		this.selectedNotifList = selectedNotifList;
	}

	public List<SelectItem> getOptionsNstrLst() {
		if(optionsNstrLst == null) {
			
			try {
				optionsNstrLst =   createItemsList(false, OmbConstants.CODE_CLASSIF_USER_SETTINGS,  new Date(), false);  //getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_USER_SETTINGS , new Date(), getCurrentLang());
			} catch (DbErrorException | UnexpectedResultException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return optionsNstrLst;
	}

	public void setOptionsNstrLst(List<SelectItem> optionsNstrLst) {
		this.optionsNstrLst = optionsNstrLst;
	}

	public Integer [] getSelectedNstrLst() {
		return selectedNstrLst;
	}

	public void setSelectedNstrLst(Integer [] selectedNstrLst) {
		this.selectedNstrLst = selectedNstrLst;
	}
	
	public void actionChngeNotif() {
		
		if(!customNotif) {
			selectedNotifList = new Integer[optionsNotifList.size()];
			int i=0;
			for(SelectItem item: optionsNotifList) {
				selectedNotifList[i] = (Integer) item.getValue();
				i++;
			}
		}
	}


	//------------------ global search-----------------------
	
	private Object[] glText;// = new Object[6];
	
	public List<Object[]> actionGlobalSearch(String glText) {
		
		if(glText==null || glText.trim().isEmpty()  ) {
			//mess
			
			return  new ArrayList<>();
		}
		
		try {
			return new DashboardUtils().searchObjectsByRN(glText, getUserData(UserData.class));
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при глобално търсене! ", e);			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} finally {
			 JPA.getUtil().closeConnection(); 
		}
		
		return  new ArrayList<>();
	}
	
	
	public void onGlItemSelect(SelectEvent<Object[]> event) {
		
        // FacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Item Selected", ));  
		
		//"docView.xhtml?faces-redirect=true&idObj="+idDoc;
		//result = "docEdit.jsf?faces-redirect=true&idObj=" + asInteger(row[0]);
		
		Object[] element = event.getObject();
		if(element !=null) {
			String idObj = SearchUtils.asString(element[0]);
			Integer type = SearchUtils.asInteger(element[3]); //1-док,2-дело,3-задача
			Integer mode = SearchUtils.asInteger(element[4]); //1-актуализация,0-преглед
			
			
			ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
			
			StringBuilder url = new StringBuilder(context.getRequestContextPath());
			url.append("/pages/");
			
			switch (type.intValue()) {
			
				case 1: //док
					if(mode.intValue()==1) {
						url.append("docEdit.xhtml?faces-redirect=true&idObj=");
						url.append(idObj);
					} else {
						url.append("docView.xhtml?faces-redirect=true&idObj=");
						url.append(idObj);
					}
				break;
				case 2: //дело
					if(mode.intValue()==1) {
						url.append("deloEdit.xhtml?faces-redirect=true&idObj=");
						url.append(idObj);
					} else {
						url.append("deloView.xhtml?faces-redirect=true&idObj=");
						url.append(idObj);
					}
				break;
				case 3: //задача
					String idDoc = "";
					if(element[5]!=null && !SearchUtils.asString(element[5]).equals("-100")) {
						idDoc = "&idDoc="+SearchUtils.asString(element[5]);
					} 
					
					if(mode.intValue()==1) {
						url.append("taskEdit.xhtml?faces-redirect=true&idObj=");
						url.append(idObj);
						url.append(idDoc);
					} else {
						url.append("taskView.xhtml?faces-redirect=true&idObj=");
						url.append(idObj);
						url.append(idDoc);
					}
				break;
				
				default:
					url = null;
					return;
			}
			
			
			
			if(url!=null) {
				try {
					if(mode.intValue()==1) {
						// следващите два реда зачистват навигацията, защото  Request-a e ajax.
						// това блокира извикването на навигацията и в екрана на обекта, бутона "Обратно" връща в логин страницата,ако преди това не е била отворено нищо друго, освен работния плот.
						// за да избегна това, най-лесно да зачистя навигацията и по този начин да скрия бутона "обратно" - поведението става аналогично на отваряне на дейност от менюто
						NavigationDataHolder holder = (NavigationDataHolder) JSFUtils.getManagedBean("navigationSessionDataHolder");
						holder.getPageList().clear();
						
						context.redirect(url.toString());
					}else {						
						Map<String, String> reqMap = context.getRequestHeaderMap();
						String origin = reqMap.get("origin");						
						PrimeFaces.current().executeScript("window.open('"+origin+url.toString()+"','_blank')");
					}
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
    }
	
	public void onGlItemUnSelect(SelectEvent<Object[]> event) {
		//LOGGER.error("onGlItemUnSelect-->");
	}
	
	
	public Object[] getGlText() {
		return glText;
	}

	public void setGlText(Object[] glText) {
		this.glText = glText;
	}

	/** @return the ldapLogin */
	public boolean isLdapLogin() {
		return this.ldapLogin;
	}
}
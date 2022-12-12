package com.ib.omb.system;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_BOSS_POSITIONS;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRI;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_USER_LOCKED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_CONTR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.omb.db.dao.ReferentDelegationDAO;
import com.ib.omb.experimental.Notification;
import com.ib.system.ActiveUser;
import com.ib.system.BaseUserData;
import com.ib.system.SysConstants;
import com.ib.system.auth.DBCredential;
import com.ib.system.auth.IBIdentityStore;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.AuthenticationException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SysClassifUtils;

/**
 * Прави логин чрез имплементираните по потребителско име и парола
 *
 * @author belev
 */
@ApplicationScoped
public class OmbIdentityStore extends IBIdentityStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(OmbIdentityStore.class);

	@Inject
	private ServletContext servletContext;

	/** */
	@Override
	protected Optional<BaseUserData> findUserDB(DBCredential credential) throws AuthenticationException {
		String username = credential.getCaller();
		String password = credential.getPasswordAsString();

		if (username == null || password == null) {
			return Optional.empty();
		}

		SystemData systemData = (SystemData) this.servletContext.getAttribute("systemData");
		try {

			AdmUserDAO dao = new AdmUserDAO(ActiveUser.DEFAULT);

			AdmUser user = dao.validateUser(systemData, username, password, false, false); // намира потребителя и валидира дали
																							// може да се направи логин
			BaseUserData userData = createUserData(systemData, user);

			return Optional.of(userData);

		} catch (AuthenticationException e) {
			if (e.getCode() == AuthenticationException.CODE_USER_LOCKED) {
				try {
					String setting = systemData.getSettingsValue("delo.unauthorizedNotifUser");
					if (setting != null) { // нотификция
						sendLockedUserNotif(e.getUserId(), username, setting.split(","), systemData);
					}
				} catch (Exception ne) {
					LOGGER.error("Грешка при формиране на нотификация: Заключен потребител.", ne);
				}
			}

			LOGGER.error(e.getMessage());
			throw e; // трябва да се знае че това е проблема

		} catch (Exception e) {
			LOGGER.error("DBCredential login ERROR!", e);
			throw new AuthenticationException(e);

		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	/** формира и изпраща нотификация: Заключен потребител
	 */
	private void sendLockedUserNotif(Integer lockedUser, String username, String[] userCodes, SystemData sd) {
		String msg = "Потребител "+username+" е заключен поради надвишаване на допустимия брой опити за вход в системата.";
		
		List<Integer> adresati = new ArrayList<>();
		for (String userCode : userCodes) {
			adresati.add(Integer.valueOf(userCode));
		}
		
		Notification notif = new Notification(lockedUser, null //
			, CODE_ZNACHENIE_NOTIFF_EVENTS_USER_LOCKED, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, sd);
		notif.setComment(msg);
		notif.setAdresati(adresati);
		notif.send();
	}

//	/** */
//	@Override
//	protected Optional<BaseUserData> findUserEAuth(EAuthCredential credential) throws AuthenticationException {
//		Integer userId = credential.getUserId();
//
//		if (userId == null) {
//			throw new AuthenticationException(new InvalidParameterException("UserId can not be null!"));
//		}
//
//		try {
//			SystemData systemData = (SystemData) this.servletContext.getAttribute("systemData");
//
//			// TODO може да се наложи да се вика нещо подобно на AdmUserDAO.validateUser заради брой опити за достъп, проверка на
//			// статуси и т.н.
//			AdmUser user = JPA.getUtil().getEntityManager().find(AdmUser.class, userId); // логин
//
//			// initialize user's accessValues
//			AdmUserDAO admUserDAO = new AdmUserDAO(ActiveUser.of(userId));
//			final Map<Integer, Map<Integer, Boolean>> userAccessMap = admUserDAO.findUserAccessMap(userId);
//			user.setAccessValues(userAccessMap);
//
//			BaseUserData userData = createUserData(systemData, user);
//
//			return Optional.of(userData);
//
//		} catch (Exception e) {
//			LOGGER.error(e.getMessage(), e);
//			throw new AuthenticationException(e);
//
//		} finally {
//			JPA.getUtil().closeConnection();
//		}
//	}

//	/** */
//	@Override
//	protected Optional<BaseUserData> findUserLDAP(LDAPCredential credential) throws AuthenticationException {
//		SystemData systemData = (SystemData) this.servletContext.getAttribute("systemData");
//
//		InitialContext ldapContext = null;
//		try {
//			LOGGER.debug("Begin findUserLDAP...");
//			
//			// get default domain of the user from system_options
//			String defaultDomain = systemData.getSettingsValue(OmbConstants.DEFAULT_LDAP_DOMAIN);
//
//			// setup properties for the initial context
//			Hashtable<String, String> ldapContextProperties = new Hashtable<>();
//
//			ldapContextProperties.put(Context.PROVIDER_URL, systemData.getSettingsValue(Context.PROVIDER_URL));// url of the ldap server
//			ldapContextProperties.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");// initial context factory
//			ldapContextProperties.put(Context.SECURITY_AUTHENTICATION, systemData.getSettingsValue(Context.SECURITY_AUTHENTICATION));
//
//			// username of the user
//			ldapContextProperties.put(Context.SECURITY_PRINCIPAL, defaultDomain +"\\"+ credential.getCaller());
//
//			// password of the user
//			ldapContextProperties.put(Context.SECURITY_CREDENTIALS, credential.getPasswordAsString());
//
//			// if we can create the context it means that user is present into the ldap directory
//			ldapContext = new InitialDirContext(ldapContextProperties);
//
//			LOGGER.debug("End findUserLDAP ... done!");
//			
//		// TODO тука с грешките надолу не е добре
//		} catch (javax.naming.AuthenticationException e) {
//			LOGGER.error(e.getMessage());
//			throw new AuthenticationException(AuthenticationException.CODE_USER_UNKNOWN, null);
//
//		} catch (Exception e) {
//			LOGGER.error("LDAPCredential login ERROR!", e);
//			throw new AuthenticationException(e);
//
//		} finally {
//			if (ldapContext != null) {
//				try {
//					ldapContext.close();
//				} catch (NamingException e) {
//					// ignore
//				}
//			}
//		}
		  
//		try { // load user data from database
//
//			AdmUserDAO dao = new AdmUserDAO(ActiveUser.DEFAULT);
//
//			AdmUser user = dao.validateUser(systemData, null, credential.getCaller());
//			
//			BaseUserData userData = createUserData(systemData, user);
//
//			return Optional.of(userData);
//
//		// TODO тука с грешките надолу не е добре
//		} catch (AuthenticationException e) {
//			LOGGER.error(e.getMessage());
//			if (e.getCode() == AuthenticationException.CODE_UNAUTHORIZED_STATUS) {
//				throw e;
//			}
//			throw new AuthenticationException("Потребителят не е регистриран в системата.", null);
//
//		} catch (Exception e) {
//			LOGGER.error("LDAPCredential login ERROR - db select!", e);
//			throw new AuthenticationException(e);
//
//		} finally {
//			JPA.getUtil().closeConnection();
//		}
//	}

	/**
	 * Създава усерДатата и сетва всичко което е нужно за работата на системата
	 *
	 * @param sd
	 * @param user
	 * @return
	 * @throws DbErrorException
	 */
	public UserData createUserData(SystemData sd, AdmUser user) throws DbErrorException {
		UserData userData = new UserData(user.getId(), user.getUsername(), user.getNames());

		Date today = DateUtils.startDate(new Date());

		SystemClassif row = sd.decodeItemLite(CODE_CLASSIF_ADMIN_STR, user.getId(), userData.getCurrentLang(), today, true);

		if (row != null) {

			userData.setRegistratura((Integer) row.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA]);
			userData.setZveno(row.getCodeParent()); // CODE_PARENT се явява звеното на човека

			Integer position = (Integer) row.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_POSITION];

			if (position != null && userData.getZveno() != null) {
				boolean boss = sd.matchClassifItems(CODE_CLASSIF_BOSS_POSITIONS, position, today);
				if (boss) { // трябва да се изтеглят и звената до които има достъп !!!

					// трябват ми само звената и за това пускам през специфика да се отрежат
					Map<Integer, Object> specifics = Collections.singletonMap(OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE, CODE_ZNACHENIE_REF_TYPE_ZVENO);

					List<SystemClassif> items = sd.queryClassification(CODE_CLASSIF_ADMIN_STR, null, today, userData.getCurrentLang(), specifics);
					List<SystemClassif> zvena = SysClassifUtils.getChildren(items, userData.getZveno(), null);

					StringBuilder accessZvenoList = new StringBuilder();
					accessZvenoList.append(userData.getZveno());

					for (SystemClassif zveno : zvena) {
						accessZvenoList.append("," + zveno.getCode());

					}
					userData.setAccessZvenoList(accessZvenoList.toString());
				}
			}
		}

		// TODO по длъжността на човека може би трябва да се тегли нещо от таблица DOC_VID_SETTINGS и съответно да се вкарат като
		// кодове на документи !!! Като най кофти е, че това при заместване и упълномощаване ще трябва да се гледа пак !!!

		if (userData.getRegistratura() != null) { // пускаме само ако текущия работи в регистратура

			// замествания и делегирания
			ReferentDelegationDAO delegationDao = new ReferentDelegationDAO(userData);
			userData.getDelegatedEmployees().addAll(delegationDao.findDelegatedEmployees(userData.getRegistratura(), user.getId(), today, sd));

			Map<Integer, Boolean> map = user.getAccessValues().get(CODE_CLASSIF_REGISTRI);
			if (map != null) { // !!! това е много важно, защото запазвам реалните права дадени до регистри под ключ с отрицателна
								// стойност. това ми позволява в последствие да мога да подменям регистрите спрямо регистратура,
								// като винаги знам оригиналния набор !!!
				user.getAccessValues().put(CODE_CLASSIF_REGISTRI * -1, new HashMap<>(map));
			}
			sd.filterRegistersByRegistratura(userData.getRegistratura(), user.getAccessValues());

			sd.calculateAdditionalRegistraturi(userData, user.getAccessValues());

			sd.filterDocVidByNoPageReg(user.getAccessValues());

			// позволените за потребителя пощенски кутии
			Map<Integer, Boolean> userMailboxes = user.getAccessValues().get(OmbConstants.CODE_CLASSIF_MAILBOXES);
			if (userMailboxes != null && !userMailboxes.isEmpty()) { // само ако има зададени

				// пощенските кутии за регистратурата
				List<SystemClassif> items = sd.queryClassification(OmbConstants.CODE_CLASSIF_MAILBOXES, null, today, userData.getCurrentLang()
					, Collections.singletonMap(OmbClassifAdapter.MAILBOXES_INDEX_REGISTRATURA, userData.getRegistratura()));
				
				List<String> mailboxNames = new ArrayList<>();
				for (SystemClassif item : items) { // добавям само тези, за които има достъп за текущата му регистратура
					if (userMailboxes.containsKey(item.getCode())) {
						mailboxNames.add(item.getTekst());
					}
				}
				if (!mailboxNames.isEmpty()) {
					userData.setMailboxNames(mailboxNames);
				}
			}
		}

		try { // вдигане на флаговете за изрично отнетия достъп

			Number docCount = (Number) JPA.getUtil().getEntityManager().createNativeQuery( //
				"select count (*) cnt from DOC_ACCESS where CODE_REF = :codeRef and EXCLUDE = :da") //
				.setParameter("codeRef", userData.getUserId()).setParameter("da", SysConstants.CODE_ZNACHENIE_DA) //
				.getResultList().get(0);
			userData.setDocAccessDenied(docCount.intValue() > 0);	

			Number deloCount = (Number) JPA.getUtil().getEntityManager().createNativeQuery( //
				"select count (*) cnt from DELO_ACCESS where CODE_REF = :codeRef and EXCLUDE = :da") //
				.setParameter("codeRef", userData.getUserId()).setParameter("da", SysConstants.CODE_ZNACHENIE_DA) //
				.getResultList().get(0);
			userData.setDeloAccessDenied(deloCount.intValue() > 0);	

			Number eventsCount = (Number) JPA.getUtil().getEntityManager().createNativeQuery( //
				"select count (*) cnt from EVENTS e inner join EVENT_REFERENTS r on r.EVENT_ID = e.EVENT_ID where r.CODE_REF = :codeRef and e.DATE_DO >= :currentDate") //
				.setParameter("codeRef", userData.getUserId()).setParameter("currentDate", new Date()) //
				.getResultList().get(0);
			if (eventsCount != null && eventsCount.intValue() > 0) {
				userData.setEventsCount(eventsCount.intValue());
			} else {
				userData.setEventsCount(null);
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при определяне на флаг за изрично отнет достъп.", e);
		}

		userData.setAccessValues(new HashMap<>(user.getAccessValues())); // задавам правата
		user.setAccessValues(null); // зачиствам ги от ентитито!

		return userData;
	}
}
package com.ib.omb.system;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_REFERENTS;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DOC_NO_PAGE_REG;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DOC_VID;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRATURI;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRATURI_REQDOC;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRI;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;
import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.system.utils.SearchUtils.asString;
import static com.ib.system.utils.SearchUtils.trimToNULL;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.omb.db.dto.NotificationPatterns;
import com.ib.omb.db.dto.Referent;
import com.ib.omb.db.dto.UserNotifications;
import com.ib.omb.experimental.Notification;
import com.ib.omb.notifications.PushBean;
import com.ib.system.BaseSystemData;
import com.ib.system.H2DataContainer;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.BaseSystemOption;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemOption;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;

import bg.government.regixclient.RegixClient;
import bg.government.regixclient.RegixClientException;


/**
 * Конкретната за системата
 */
public class SystemData extends BaseSystemData {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SystemData.class);

	/** с този префикс ще са настройките на регистратура в SYSTEM_OPTIONS */
	private static final String REG_SETTING_PREFIX = "REG_";

	/** */
	private static final long serialVersionUID = 5432305666789502131L;

	@Inject
	Event<Notification> msgEvent;
	
	@Inject
	PushBean pushBean;
	
	/** Шаблони за нотификации */
	private HashMap<Integer, HashMap<Integer, NotificationPatterns>> patterns;
	
	
	
	
	/** Настройка на пощенски кутии към регистратури*/
	private HashMap<Integer, HashMap<String, Properties>> mailProps;
	/** Списък на кутии  --- Това май трябва да е временно, докато се направят към потребител
	[0] - id на регистратура
	[1] - име на кутия
	[2] - userName
	[3] - password
	*/
	private ArrayList<Object[]> mailboxes;
	
	
	/** Потребител/Нотификации */
	private HashMap<Integer, ArrayList<Integer>> userNotifications;
	
	private Set<Long> praznici; // празничните дани като милисекунди, за да може да се прави set.contains()
								// НЕ трябва да има часове минути секунди и милисекудни. ако от БД дойдат ще се чистят
	
	/** допуска се деловодител да обслужва повече регистратури */
	private boolean dopDelovoditelRegistraturi; // това го правя така за да се достъпва директно през булевата,
												// защото на запис на док/дело/задача трябва да знам какво може деловодителя

	/** за някои институции е възможно отчетния период да не е календарната година. пример 15.09 - учебната година.
	 * тези числа ще се използват при валидациите на номера на документи и преписки.
	 * не е добре да са статик, но се викат на места където няма системДата (findById) */
	private static int	docuDay 	= 1;	// ден начало на отчетн период
	private static int	docuMonth 	= 1;	// месец начало на отчетен период

	/** на много места ако това е включено номера трябва да се визуализира от вида ВХ-123#2,
	 * където #2 е поредения номер ако е класитан в преписка и документа е взел нейния номер.
	 * Използва се и при запис, където това поле се поддържа. Булевата се определя от настройка 'delo.docPoredDelo' */
	private static boolean docPoredDeloGen;
	
	/** Съответно версиите за БД и сорскода.<br>
	 * Ако 1. srcVersion = dbVersion - системата е ОК<br>
	 * Ако 2. srcVersion != dbVersion - е нужно да се направи упграде<br>
	 * 
	 * В БД за разработка тъй като се правят ръчни промени и БД винаги е актуална е необходимо dbVersion да се актуализира, когато се сменя srcVersion.
	 */
	private String srcVersion	= "";
	private String dbVersion 	= "";

	private RegixClient regixClient;

	/**  */
	public SystemData() {
		super();

		try {
			this.dopDelovoditelRegistraturi = "1".equals(getSettingsValue("delo.dopDelovoditelRegistraturi"));
		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на системна настройка 'delo.dopDelovoditelRegistraturi'", e);
		}

		try {
			this.srcVersion = getSettingsValue("omb.src.version");
			
			@SuppressWarnings("unchecked")
			List<Object> rows = JPA.getUtil().getEntityManager().createNativeQuery("select CURRENT_VERSION from VERSION_TABLE").getResultList();
			if (!rows.isEmpty()) {
				this.dbVersion = (String) rows.get(0);
			}
		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на 'docu.src/db.version'", e);
		}

		try {
			String setting = getSettingsValue("delo.beginDayOfPeriod");
			if (setting != null) { // ако е няма ще си остане дефолтно началото на годината
				String[] dayMonth = setting.split("\\.");
				
				int day = Integer.parseInt(dayMonth[0]);
				int month = Integer.parseInt(dayMonth[1]);
				if (day < 1 || day > 31 || month < 1 || month > 12) {
					throw new InvalidParameterException("Невалидна стойност=" + setting);
				}
				docuDay = day;
				docuMonth = month;
			}
		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на системна настройка 'delo.beginDayOfPeriod'", e);
		}

		try {
			docPoredDeloGen = "1".equals(getSettingsValue("delo.docPoredDelo"));
		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на системна настройка 'delo.docPoredDelo'", e);
		}
	}

	/**
	 * допълнително да се нулират и разни други кешове
	 */
	@Override
	public boolean reset(boolean altR) throws DbErrorException {
		boolean reset = super.reset(altR);

		setUserNotifications(null);
		setPraznici(null);
		
		return reset;
	}



	public ArrayList<Object[]> getMailboxes() throws DbErrorException {
		
		if (mailboxes == null) {
			loadProps();
		}
		
		return mailboxes;
	}

	public void setMailboxes(ArrayList<Object[]> mailboxes) {
		this.mailboxes = mailboxes;
	}
	
	/**
	 * Изчислява се дали текущия е деловодител и ако е да, се оправя списъка на регистратурите, в които може да извежда документи.
	 * Причината за това е че текущата регистратура, не е във списъка със всички. Друга причина е че при заместване и делегиране
	 * на права трябва пак да се изчисли спрямо новите му права.
	 *
	 * Изчислява списъка на допълнителните регистратури ("Допълнителни регистратури за заявка за деловодна регистрация на
	 * документи" и "Допълнителни регистратури за достъп до обекти"). Причината за това е че текущата регистратура, не е във
	 * списъка със всички. Друга причина е че при заместване и делегиране на права трябва пак да се изчисли спрямо новите му
	 * права. Изчислението се изпълнява само ако допълнителните регистратури присъстват в системната настройка
	 * "system.classificationsForAccessControl"
	 *
	 * Ако е деловодител добавя в регистратурите му за достъп до обекти и регистратурите, в който може да извежда документи
	 * 
	 * @param userData
	 * @param accessValues
	 * @throws DbErrorException
	 */
	public void calculateAdditionalRegistraturi(UserData userData, Map<Integer, Map<Integer, Boolean>> accessValues) throws DbErrorException {
		Map<Integer, Boolean> roles = accessValues.get(Constants.CODE_CLASSIF_BUSINESS_ROLE);
		if (roles != null && roles.containsKey(OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_DELOVODITEL)) { // значи е деловодител

			userData.setDelovoditel(true);

			// трябва да се добави текущата регистратура в спистка на обслужващите го
			Map<Integer, Boolean> regs = accessValues.get(OmbConstants.CODE_CLASSIF_REGISTRATURI);
			if (regs == null) { // може и да няма нито една
				regs = new HashMap<>();
				accessValues.put(OmbConstants.CODE_CLASSIF_REGISTRATURI, regs);
			}
			regs.put(userData.getRegistratura(), Boolean.TRUE);

		} else { // няма как да е деловодител
			userData.setDelovoditel(false);
			accessValues.remove(OmbConstants.CODE_CLASSIF_REGISTRATURI);
		}

		// няма значение дали това е в настройката, защото се използва за справки.
//		if (isClassifInSetting("system.classificationsForAccessControl", CODE_CLASSIF_REGISTRATURI_OBJACCESS)) {
			Map<Integer, Boolean> objaccess = accessValues.get(CODE_CLASSIF_REGISTRATURI_OBJACCESS);
			if (objaccess == null) {
				objaccess = new HashMap<>();
				accessValues.put(CODE_CLASSIF_REGISTRATURI_OBJACCESS, objaccess);
			}
			objaccess.put(userData.getRegistratura(), Boolean.TRUE); // винаги има достъп до собствената си
		
			if (userData.isDelovoditel()) { // това е новото за да има достъп и до тях
				accessValues.get(CODE_CLASSIF_REGISTRATURI_OBJACCESS).putAll(accessValues.get(OmbConstants.CODE_CLASSIF_REGISTRATURI));
			}
//		}

		if (isClassifInSetting("system.classificationsForAccessControl", CODE_CLASSIF_REGISTRATURI_REQDOC)) {
			Map<Integer, Boolean> reqdoc = accessValues.get(CODE_CLASSIF_REGISTRATURI_REQDOC);
			if (reqdoc == null) {
				reqdoc = new HashMap<>();
				accessValues.put(CODE_CLASSIF_REGISTRATURI_REQDOC, reqdoc);
			}
			reqdoc.put(userData.getRegistratura(), Boolean.TRUE); // винаги има достъп до собствената си
		}
	}

	/**
	 * Разкодира значения (кодове с разделител ',') по език към конкретна дата като в разкодирания резултата пак са с разделител
	 * запетая
	 *
	 * @param codeClassif
	 * @param codes
	 * @param lang
	 * @param date
	 * @return
	 * @throws DbErrorException
	 */
	public String decodeItems(Integer codeClassif, String codes, Integer lang, Date date) throws DbErrorException {
		if (codes == null || codes.length() == 0) {
			return null;
		}
		if (codes.indexOf(',') == -1) { // само един е
			return decodeItem(codeClassif, Integer.valueOf(codes), lang, date);
		}

		String[] arr = codes.split(",");

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < arr.length; i++) {
			String code = arr[i];

			sb.append(decodeItem(codeClassif, Integer.valueOf(code), lang, date));
			if (i < arr.length - 1) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	/**
	 * Филтрира допустимите докуметни в зависимост от класификация "Документи, които не се регистрират през екрана за документ"
	 *
	 * @param accessValues
	 * @throws DbErrorException
	 */
	public void filterDocVidByNoPageReg(Map<Integer, Map<Integer, Boolean>> accessValues) throws DbErrorException {
		Map<Integer, Boolean> docVidMap = accessValues.get(CODE_CLASSIF_DOC_VID);

		if (docVidMap == null || docVidMap.isEmpty()) { // щом няма нищо дадено значи ще гледам настройката
			boolean exist = isClassifInSetting("system.classificationsNotFilteredIfNotGranted", CODE_CLASSIF_DOC_VID);

			if (exist) { // и тази класификация е там

				accessValues.put(CODE_CLASSIF_DOC_VID, new HashMap<>()); // за да има къде да се трупат

				List<Integer> docVidCodes = getSysClassificationCodes(CODE_CLASSIF_DOC_VID, null, CODE_DEFAULT_LANG);
				List<Integer> noPageRegCodes = getSysClassificationCodes(CODE_CLASSIF_DOC_NO_PAGE_REG, null, CODE_DEFAULT_LANG);
				
				docVidCodes.removeAll(noPageRegCodes); // щом не се съдържа може да се добави
				
				for (Integer code : docVidCodes) { // значи ще има достъп до цялата класификация с изключени на елементите описани
													// в класификация "Документи, които не се регистрират през екрана за документ"
					accessValues.get(CODE_CLASSIF_DOC_VID).put(code, Boolean.TRUE);
				}
			}

		} else { // има нещо зададено но все пак трябва да се ореже през класификация "Документи, които не се регистрират през
					// екрана за документ"
			List<Integer> noPageRegCodes = getSysClassificationCodes(CODE_CLASSIF_DOC_NO_PAGE_REG, null, CODE_DEFAULT_LANG);
			for (Integer code : noPageRegCodes) {
				if (docVidMap.containsKey(code)) {
					docVidMap.remove(code);
				}
			}
		}
	}

	/**
	 * Филтрира всички допустими регистри от правата на потребителя, за подадената регистратура
	 *
	 * @param registratura
	 * @param accessValues
	 * @throws DbErrorException
	 */
	public void filterRegistersByRegistratura(Integer registratura, Map<Integer, Map<Integer, Boolean>> accessValues) throws DbErrorException {
		List<Integer> byRegistratura = new ArrayList<>(); // това ще се зареди от специфика на класификация регистратури
		String registers = trimToNULL((String) getItemSpecific(CODE_CLASSIF_REGISTRATURI, registratura, CODE_DEFAULT_LANG, null, OmbClassifAdapter.REGISTRATURI_INDEX_REGISTRI));
		if (registers != null) { // трябва да ги направя на списък
			String[] registerIds = registers.split(",");
			for (String registerId : registerIds) {
				byRegistratura.add(Integer.parseInt(registerId));
			}
		}

		accessValues.put(CODE_CLASSIF_REGISTRI, new HashMap<>()); // тука ще остане филтриран списък по регистратура

		Map<Integer, Boolean> byUser = accessValues.get(CODE_CLASSIF_REGISTRI * -1); // взимаме от пълния списък без оглед на
																						// регистратура

		if (byUser != null && !byUser.isEmpty()) { // филтрираме по правата на потребителя, ако има нещо зададено

			for (Integer x : byRegistratura) {
				if (byUser.containsKey(x)) {
					accessValues.get(CODE_CLASSIF_REGISTRI).put(x, Boolean.TRUE);
				}
			}
			if (!accessValues.get(CODE_CLASSIF_REGISTRI).isEmpty()) { // ако има данни значи сме готови
				return;
			}
		}

		// щом няма нищо дадено значи ще гледам настройката
		boolean exist = isClassifInSetting("system.classificationsNotFilteredIfNotGranted", CODE_CLASSIF_REGISTRI);
		if (exist) { // значи има достъп до всички
			for (Integer x : byRegistratura) {
				accessValues.get(CODE_CLASSIF_REGISTRI).put(x, Boolean.TRUE);
			}
		}
	}

	/**
	 * Намира системна настройка по регистратура и код на настройка. Ако няма дава NULL
	 *
	 * @param registratura
	 * @param settingCode
	 * @return
	 * @throws DbErrorException
	 */
	public Integer getRegistraturaSetting(Integer registratura, int settingCode) throws DbErrorException {
		String key = REG_SETTING_PREFIX + registratura + "_" + settingCode; // сформирам ключа
		String value = getSettingsValue(key); // и минавам през системните настройки
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Прави подмяна в класификация само на променени данни. В случая подменя данни за участник в процеса в динамична класификация
	 * {@link Constants#CODE_CLASSIF_REFERENTS}
	 *
	 * @param referent
	 * @param delete
	 * @throws DbErrorException
	 * @see #mergeClassifItem(SystemClassif, Integer, boolean)
	 */
	public void mergeReferentsClassif(Referent referent, boolean delete) throws DbErrorException {
		SystemClassif item;

		if (delete) { // изтривам го и ще направя един фиктивен елемент, за да сработ изтриването
			item = new SystemClassif();

			item.setId(referent.getId());
			item.setCode(referent.getCode());
			item.setCodeClassif(CODE_CLASSIF_REFERENTS);

		} else { // трябват ми реалните данни и ще се търси от БД

			OmbClassifAdapter adapter = (OmbClassifAdapter) this.classifAdapter;

			boolean isActive = JPA.getUtil().getEntityManager().getTransaction().isActive();
			try {
				item = adapter.findItemClassReferents(referent.getId(), CODE_DEFAULT_LANG);
			} finally {
				if (!isActive) {
					JPA.getUtil().closeConnection();
				}
			}
		}

		mergeClassifItem(item, CODE_DEFAULT_LANG, delete);

		if (referent.getTipOrgan() != null) {
			item.setCodeClassif(OmbConstants.CODE_CLASSIF_ORGAN_NPM);
			mergeClassifItem(item, CODE_DEFAULT_LANG, delete);
		}
	}

	/**
	 * Допълнително добавя и настройките на регистратура
	 *
	 * @see #getRegistraturaSetting(Integer, int)
	 */
	@Override
	public List<BaseSystemOption> selectOptionsList() throws DbErrorException {
		List<BaseSystemOption> list = super.selectOptionsList();

		// ако сме влезли тука в активна транзакция накрая не трябва да се вика close()
		boolean isActive = JPA.getUtil().getEntityManager().getTransaction().isActive();

		try { // сега и настройките за регистратурата
			@SuppressWarnings("unchecked")
			List<Object[]> rows = JPA.getUtil().getEntityManager().createNativeQuery("select REGISTRATURA_ID, SETTING_CODE, SETTING_VALUE from REGISTRATURA_SETTINGS").getResultList();

			for (Object[] row : rows) {
				String optionLabel = REG_SETTING_PREFIX + asInteger(row[0]) + "_" + asInteger(row[1]);
				Integer optionValue = asInteger(row[2]);

				list.add(new SystemOption(optionLabel, optionValue != null ? String.valueOf(optionValue) : null, null));
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при зареждане на настройки за регистратура", e);

		} finally {
			if (!isActive) {
				JPA.getUtil().closeConnection();
			}
		}

		return list;
	}

	/** @see BaseSystemData#createDynamicAdapterInstance() */
	@Override
	protected Object createDynamicAdapterInstance() {
		return new OmbClassifAdapter(this);
	}

	/** @return the h2dc */
	H2DataContainer getH2dc() {
		return this.h2dc;
	}
	
	
	
	
	/** Изпращане на нотификация
	 * @param msg
	 */
	public void sendNotification(Notification nb){
		try {
			msgEvent.fireAsync(nb);
		} catch (Exception e) {
			LOGGER.error("Грешка при изпращане на съобщение с msgEvent !", e);
		}
	}
	
	/** Изпращане на нотификация
	 * @param title
	 * @param details
	 * @param severity
	 * @param user
	 */
	public void sendNotification(String title, String details, String severity, Integer user) {
		
		Notification notif = new Notification(this);
		notif.setuMessage(new UserNotifications(null,title,details,severity));
		notif.getAdresati().add(user);
		notif.send();
	}
	
	/** Изпращане на нотификация
	 * @param title
	 * @param details
	 * @param severity
	 * @param users
	 */
	public void sendNotification(String title, String details, String severity, ArrayList<Integer> users) {
		
		Notification notif = new Notification(this);
		notif.setuMessage(new UserNotifications(null,title,details,severity));
		notif.getAdresati().addAll(users);
		notif.send();
	}
	

	
	
	/**Извлича шаблон за нотификация
	 * @param eventId ид на събития
	 * @param rolia роля
	 * @return
	 * @throws DbErrorException
	 */
	public NotificationPatterns getPattern(Integer eventId, Integer rolia) throws DbErrorException {

		if (patterns == null) {
			loadPatterns();
		}
		HashMap<Integer, NotificationPatterns> map = patterns.get(eventId);
		if (map == null) {
			return null;
		} else {
			NotificationPatterns pattern = map.get(rolia);
			if (pattern == null) {
				pattern = map.get(OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_ALL_DOST);
			}
			return pattern;
		}
	}

	/** */
	@Override
	public void reloadSystemOptions() throws DbErrorException {
		super.reloadSystemOptions();
		
		try { // ако е сменяна настройката да влезе новата стойност в сила
			this.dopDelovoditelRegistraturi = "1".equals(getSettingsValue("delo.dopDelovoditelRegistraturi"));
		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на системна настройка 'delo.dopDelovoditelRegistraturi'", e);
		}

		try {
			String setting = getSettingsValue("delo.beginDayOfPeriod");
			if (setting != null) { // ако е няма ще си остане дефолтно началото на годината
				String[] dayMonth = setting.split("\\.");
				
				int day = Integer.parseInt(dayMonth[0]);
				int month = Integer.parseInt(dayMonth[1]);
				if (day < 1 || day > 31 || month < 1 || month > 12) {
					throw new InvalidParameterException("Невалидна стойност=" + setting);
				}
				docuDay = day;
				docuMonth = month;
			}
		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на системна настройка 'delo.beginDayOfPeriod'", e);
		}
		
		try {
			docPoredDeloGen = "1".equals(getSettingsValue("delo.docPoredDelo"));
		} catch (Exception e) {
			LOGGER.error("Грешка при определяне на системна настройка 'delo.docPoredDelo'", e);
		}
	}


	/** @return the dopDelovoditelRegistraturi */
	public boolean isDopDelovoditelRegistraturi() {
		return this.dopDelovoditelRegistraturi;
	}

	/** @return the docuDay */
	public static int getDocuDay() {
		return docuDay;
	}

	/** @return the docuMonth */
	public static int getDocuMonth() {
		return docuMonth;
	}

	/** @return the docPoredDeloGen */
	public static boolean isDocPoredDeloGen() {
		return docPoredDeloGen;
	}

	/**Връща инстанция на пушбийна
	 * @return
	 */
	public PushBean getPushBean() {
		return pushBean;
	}
	

	/** Зарежда шаблоните за нотификации в системдатата
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	private void loadPatterns() throws DbErrorException {

		List<NotificationPatterns> list;
		try {
			list = JPA.getUtil().getEntityManager().createQuery("from NotificationPatterns").getResultList();
		} catch (Exception e) {
			LOGGER.error("Грешка при извличане на шаблони за нотификации", e);
			throw new DbErrorException("Грешка при извличане на шаблони за нотификации", e);
		}
		patterns = new HashMap<Integer, HashMap<Integer,NotificationPatterns>>();
		for (NotificationPatterns patt : list) {
			patt.getVariables().size();
			HashMap<Integer, NotificationPatterns> tek = patterns.get(patt.getEventId());
			if (tek == null) {
				tek = new HashMap<Integer, NotificationPatterns>();
			}
			tek.put(patt.getRolia(), patt);
			patterns.put(patt.getEventId(), tek);
		}
		
	}
	
	/** Връща всички шаблони на нотификации
	 * @return
	 * @throws DbErrorException
	 */
	public ArrayList<NotificationPatterns> getAllPatterns() throws DbErrorException {
		
		if (patterns == null) {
			loadPatterns();
		}

		ArrayList<NotificationPatterns> allPatterns = new  ArrayList<NotificationPatterns>();
		for (Entry<Integer, HashMap<Integer, NotificationPatterns>> entry : patterns.entrySet()) {
			for (Entry<Integer, NotificationPatterns> patt : entry.getValue().entrySet()) {
				allPatterns.add(patt.getValue());
			}
		}
		
		return allPatterns;
	}
	
	
	/**Взима настройка за връзка към пощенска кутия по ид на регистратура и име на кутия
	 * @param regId - системен идентификатор на регистратура
	 * @param mailBox име на пощенска кутия
	 * @return елемент от тип Properties
	 * @throws DbErrorException
	 */
	public Properties getMailProp(Integer regId, String mailBox) throws DbErrorException {
		
		if (mailProps == null) {
			loadProps();
		}

		HashMap<String, Properties> map = mailProps.get(regId);
		if (map == null) {
			return (Properties) mailProps.get(-1).get("DEFAULT").clone();
		}else {
			Properties prop = map.get(mailBox);
			if (prop == null) {
				return (Properties) mailProps.get(-1).get("DEFAULT").clone();
			}else {
				return prop;
			}
		}
	}
	
	
//	public HashMap<Integer, HashMap<String, Properties>> getMailProps() throws DbErrorException {
//		
//		if (mailProps == null) {
//			loadProps();
//		}
//		
//		return mailProps;
//	}
	
	
	/** Зарежда настройите за връзване на пощенските кутии на регистратурите
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	private void loadProps() throws DbErrorException {
		
		mailProps = new HashMap<Integer, HashMap<String, Properties>>();
		
		try {
			
			mailboxes = (ArrayList<Object[]>) JPA.getUtil().getEntityManager().createNativeQuery("select REGISTRATURA_ID, MAILBOX_NAME, MAILBOX_USERNAME, MAILBOX_PASSWORD from REGISTRATURA_MAILBOXES").getResultList();
			
			
			List<Object[]> vars = JPA.getUtil().getEntityManager().createNativeQuery("select REGISTRATURA_ID, MAILBOX_NAME, MAIL_KEY, MAIL_VALUE from REGISTRATURA_MAILBOXES_VARS join REGISTRATURA_MAILBOXES on REGISTRATURA_MAILBOXES_VARS.MAILBOX_ID = REGISTRATURA_MAILBOXES.MAILBOX_ID"
					+ " union "
					+ " select -1, 'DEFAULT', OPTION_LABEL, OPTION_VALUE from SYSTEM_OPTIONS where OPTION_LABEL like 'mail.%' or OPTION_LABEL like 'user.%'").getResultList();
			for (Object[] row : vars) {
				HashMap<String, Properties> map = mailProps.get(asInteger(row[0]));
				if (map == null) {
					map= new HashMap<String, Properties>();
				}
				Properties prop = map.get(asString(row[1]));
				if (prop == null) {
					prop = new Properties();
				}
				
				
				if (row[2] != null && row[3] != null) {
					prop.put(asString(row[2]), asString(row[3]));
				}
				map.put(asString(row[1]), prop);
				mailProps.put(asInteger(row[0]), map);
			}
			
			
			//i kakwo prawim s tezi pk koito nqmat REGISTRATURA_MAILBOXES_VARS definirani (defoltni toest)
			Properties defaultPtop =(Properties) mailProps.get(-1).get("DEFAULT").clone();
			
			if(defaultPtop == null) {
				LOGGER.debug("Няма описани дефолтни настройки за пощата!!!!!");
			} else {
				for(Object[] mail:mailboxes) {
					
					Integer regId = asInteger(mail[0]);
					String mailBoxName = asString(mail[1]);
					String userName = asString(mail[2]);
					String passord = asString(mail[3]);
					
					if(!mailProps.containsKey(regId) || !mailProps.get(regId).containsKey(mailBoxName)) {
						//ще се опитам да сложа всички настройки от дефолтните които са описани и ще подменя mail.from ,user.name ,user.password
						
						Properties curPtop = new Properties();
						for(Entry <Object,Object>item:defaultPtop.entrySet()) {
							curPtop.put(item.getKey(), item.getValue());
							
						}
						
						curPtop.put("mail.from", mailBoxName);
						curPtop.put("user.name", userName);
						curPtop.put("user.password", passord);
						
						if(mailProps.containsKey(regId)) {
							mailProps.get(regId).put(mailBoxName, curPtop);
						} else {
							HashMap<String, Properties> mapProps = new HashMap<String, Properties>();
							mapProps.put(mailBoxName, curPtop);
							mailProps.put(regId, mapProps);
						}
						
					}
				
				}
			}
			
		}catch (Exception e) {
			LOGGER.error("Грешка при извличане на променливи за поща", e);
			throw new DbErrorException("Грешка при извличане на променливи за поща", e);
		}
		
		
	}

	
    public boolean checkUserNotifSettings(Integer idUser, Integer codeNotif) throws DbErrorException {
    	if (userNotifications == null) {
    		loadNotifMap();
    	}
    	
    	ArrayList<Integer> list = userNotifications.get(idUser);
    	if (list == null || list.size() == 0) {
    		//Няма зададени ограничения, т.е. получава всички
    		return true;
    	}else {
    		return list.contains(codeNotif);
    	}
    	
    	
    }

	/** @param userNotifications the userNotifications to set */
	public void setUserNotifications(HashMap<Integer, ArrayList<Integer>> userNotifications) {
		this.userNotifications = userNotifications;
	}

	@SuppressWarnings("unchecked")
	private void loadNotifMap() throws DbErrorException {
		userNotifications = new HashMap<Integer, ArrayList<Integer>>();
		try {
			ArrayList<Object[]> listAll = (ArrayList<Object[]>) JPA.getUtil().getEntityManager().createNativeQuery("select USER_ID, CODE_ROLE from ADM_USER_ROLES where CODE_CLASSIF = :CC").setParameter("CC", OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE).getResultList();
			for (Object[] row : listAll) {
				Integer userId = asInteger(row[0]);
				Integer codeRole = asInteger(row[1]);
				ArrayList<Integer> userList = userNotifications.get(userId);
				if (userList == null) {
					userList = new ArrayList<Integer>();
				}
				userList.add(codeRole);
				userNotifications.put(userId, userList);
			}
			
			
		}catch (Exception e) {
			LOGGER.error("Грешка при извличане на потребителски настройки за нотификации", e);
			throw new DbErrorException("Грешка при извличане на потребителски настройки за нотификации", e);
		}
		
	}

	/** @return the praznici */
	public Set<Long> getPraznici() {
		if (this.praznici == null) {
			this.praznici = new HashSet<>();
			
			boolean isActive = JPA.getUtil().getEntityManager().getTransaction().isActive();
			try {
				@SuppressWarnings("unchecked")
				List<Object> objs = JPA.getUtil().getEntityManager().createNativeQuery("select distinct DEN from PRAZNICI").getResultList();
				
				for (Object obj : objs) {
					Date date = (Date) obj; // ако в БД започнат да влизат и часове минути и т.н. тука трябва да се изчистят
										    // преди да се вземат милисекундите
					this.praznici.add(date.getTime());
				}
			} catch (Exception e) {
				LOGGER.error("Грешка при кеширане на празнични дни!", e);
			} finally {
				if (!isActive) {
					JPA.getUtil().closeConnection();
				}
			}
		}
		return this.praznici;
	}

	/** @param praznici the praznici to set */
	public void setPraznici(Set<Long> praznici) {
		this.praznici = praznici;
	}

	/** @return the dbVersion */
	public String getDbVersion() {
		return this.dbVersion;
	}
	/** @return the srcVersion */
	public String getSrcVersion() {
		return this.srcVersion;
	}
	/** @param dbVersion the dbVersion to set */
	public void setDbVersion(String dbVersion) {
		this.dbVersion = dbVersion;
	}
	
	/**
	 * @return
	 * @throws RegixClientException
	 * @throws DbErrorException 
	 */
	public RegixClient getRegixClient() throws RegixClientException, DbErrorException {
		if (this.regixClient == null) { // иницализация трябва
			LOGGER.info("START RegixClient initialization ...");

			String keystore = getSettingsValue("regix.keystore.location");
			if (keystore == null) {
				throw new RegixClientException("Missing Setting regix.keystore.location!");
			}
			String password = getSettingsValue("regix.keystore.password");
			if (password == null) {
				throw new RegixClientException("Missing Setting regix.keystore.password!");
			}
			String type = getSettingsValue("regix.keystore.type");
			if (type == null) {
				throw new RegixClientException("Missing Setting regix.keystore.type!");
			}
			String wsdl = getSettingsValue("regix.wsdl.url");
			if (wsdl == null) {
				throw new RegixClientException("Missing Setting regix.wsdl.url!");
			}
			LOGGER.info("RegixClient wsdl={}", wsdl);
			
			try (InputStream input = new FileInputStream(keystore)) {

				this.regixClient = RegixClient.create(new URL(wsdl), input, password.toCharArray(), type);

				LOGGER.info("END RegixClient initialization ... success");
			} catch (Exception e) {
				LOGGER.error("RegixClient init ERROR", e);
				throw new RegixClientException(e);
			} 
		}
		return this.regixClient;
	}
}
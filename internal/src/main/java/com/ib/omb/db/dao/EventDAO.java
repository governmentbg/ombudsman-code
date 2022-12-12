package com.ib.omb.db.dao;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_VID_EVENT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_PARTICIPATION;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_EVENT_CHANGE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_EVENT_IN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_EVENT_OUT;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.omb.db.dto.Event;
import com.ib.omb.experimental.Notification;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.BaseUserData;
import com.ib.system.SysConstants;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link Event}
 *
 * @author belev
 */
public class EventDAO extends AbstractDAO<Event> {

	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(EventDAO.class);

	/**
	 * @param user
	 */
	public EventDAO(ActiveUser user) {
		super(Event.class, user);
	}

	/**
	 * [0]-EVENT_ID<br>
	 * [1]-EVENT_CODE<br>
	 * [2]-EVENT_INFO<br>
	 * [3]-DATE_OT<br>
	 * [4]-DATE_DO<br>
	 * [5]-ORGANIZATOR<br>
	 * [6]-USER_REG<br>
	 * [7]-CODE_REF-кодовете на участниците с разделител запетая - пример (1,6,18)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date)}<br>
	 * [8]-RESOURCE-кодовете на ресурси с разделител запетая - пример (1,6,18)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date)}<br>
	 * [9]-EVENT_NOTE<br>
	 *
	 * @param ud
	 * @param eventCodeList
	 * @param dateFrom
	 * @param dateTo
	 * @param eventInfo
	 * @param organizator
	 * @param codeRefList
	 * @param resourcesList
	 * @param current
	 * @return
	 */
	public SelectMetadata createSelectFilter(BaseUserData ud, List<Integer> eventCodeList, Date dateFrom, Date dateTo, String eventInfo, Integer organizator, List<Integer> codeRefList,
		List<Integer> resourcesList, Boolean current) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		select.append(" select distinct e.EVENT_ID a0, e.EVENT_CODE a1, " + DialectConstructor.limitBigString(dialect, "e.EVENT_INFO", 300)
			+ " a2, e.DATE_OT a3, e.DATE_DO a4, e.ORGANIZATOR a5, e.USER_REG a6, ");
		select.append(DialectConstructor.convertToDelimitedString(dialect, "r.CODE_REF", "EVENT_REFERENTS r where r.EVENT_ID = e.EVENT_ID", "r.EVENT_ID") + " a7, ");
		select.append(DialectConstructor.convertToDelimitedString(dialect, "r.CODE_RES", "EVENT_RESOURCES r where r.EVENT_ID = e.EVENT_ID", "r.EVENT_ID") + " a8 ");
		select.append(" , " + DialectConstructor.limitBigString(dialect, "e.EVENT_NOTE", 300) + " a9");

		StringBuilder from = new StringBuilder(" from EVENTS e ");

		StringBuilder where = new StringBuilder(" where 1=1 ");

		if (eventCodeList != null && !eventCodeList.isEmpty()) {
			where.append(" and e.EVENT_CODE in (:eventCodeList) ");
			params.put("eventCodeList", eventCodeList);
		}

		if (dateFrom != null) {
			where.append(" and (e.DATE_OT >= :dateFrom or e.DATE_DO >= :dateFrom) ");
			params.put("dateFrom", DateUtils.startDate(dateFrom));
		}
		if (dateTo != null) {
			where.append(" and (e.DATE_OT <= :dateTo or e.DATE_DO <= :dateTo) ");
			params.put("dateTo", DateUtils.endDate(dateTo));
		}

		if (Boolean.TRUE.equals(current)) {
			where.append(" and e.DATE_DO >= :dateCurrent ");
			params.put("dateCurrent", DateUtils.startDate(new Date()));
		}

		String t = SearchUtils.trimToNULL_Upper(eventInfo);
		if (t != null) {
			where.append(" and upper(e.EVENT_INFO) like :eventInfo ");
			params.put("eventInfo", "%" + t + "%");
		}

		if (organizator != null) {
			where.append(" and e.ORGANIZATOR = :organizator ");
			params.put("organizator", organizator);
		}

		if (codeRefList != null && !codeRefList.isEmpty()) {
			from.append(" inner join EVENT_REFERENTS ref on ref.EVENT_ID = e.EVENT_ID and ref.CODE_REF in (:codeRefList) ");
			params.put("codeRefList", codeRefList);
		}
		if (resourcesList != null && !resourcesList.isEmpty()) {
			from.append(" inner join EVENT_RESOURCES res on res.EVENT_ID = e.EVENT_ID and res.CODE_RES in (:resourcesList) ");
			params.put("resourcesList", resourcesList);
		}

		boolean addAccess = !ud.hasAccess(Constants.CODE_CLASSIF_BUSINESS_ROLE, OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_EVENT_ADMIN);
		if (addAccess) { // добавя се достъп само ако не е админ събития
			where.append(" and (e.ORGANIZATOR = :userAccess ");
			where.append(" or EXISTS (select vr.EVENT_ID from EVENT_REFERENTS vr where vr.EVENT_ID = e.EVENT_ID and vr.CODE_REF = :userAccess) ");
			where.append(" or e.USER_REG = :userId) ");
			params.put("userAccess", ((UserData) ud).getUserAccess());
			params.put("userId", ud.getUserId());
		}

		SelectMetadata sm = new SelectMetadata();

		sm.setSql(select.toString() + from + where);
		sm.setSqlCount(" select count(distinct e.EVENT_ID) " + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * [0]-EVENT_ID<br>
	 * [1]-EVENT_CODE<br>
	 * [2]-EVENT_INFO<br>
	 * [3]-DATE_OT<br>
	 * [4]-DATE_DO<br>
	 * [5]-ORGANIZATOR<br>
	 * [6]-USER_REG<br>
	 * [7]-CODE_REF-кодовете на участниците с разделител запетая - пример (1,6,18)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date)}<br>
	 * [8]-CODE_RES<br>
	 * [9]-EVENT_NOTE<br>
	 *
	 * @param eventId
	 * @param resourcesList
	 * @return
	 */
	public SelectMetadata createSelectResourcesReport(Integer eventId, List<Integer> resourcesList) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		select.append(" select e.EVENT_ID a0, e.EVENT_CODE a1, e.EVENT_INFO a2, e.DATE_OT a3, e.DATE_DO a4, e.ORGANIZATOR a5, e.USER_REG a6, ");
		select.append(DialectConstructor.convertToDelimitedString(dialect, "r.CODE_REF", "EVENT_REFERENTS r where r.EVENT_ID = e.EVENT_ID", "r.EVENT_ID") + " a7 ");
		select.append(" , res.CODE_RES a8, e.EVENT_NOTE a9 ");

		StringBuilder from = new StringBuilder(" from EVENTS e ");

		String where = " where e.DATE_DO >= :dateCurrent ";

		params.put("dateCurrent", DateUtils.startDate(new Date()));

		from.append(" inner join EVENT_RESOURCES res on res.EVENT_ID = e.EVENT_ID and res.CODE_RES in (:resourcesList) ");
		params.put("resourcesList", resourcesList);

		SelectMetadata sm = new SelectMetadata();

		sm.setSql(select.toString() + from + where);
		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * @param entity
	 * @param systemData
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public void delete(Event entity, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		if (entity.getCodeRefList() != null && !entity.getCodeRefList().isEmpty()) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

			StringBuilder comment = new StringBuilder();
			comment.append("Вече не сте включен в провеждането на събитие от вид \"");
			comment.append(systemData.decodeItem(CODE_CLASSIF_VID_EVENT, entity.getEventCode(), getUserLang(), null) + "\"");
			comment.append(" в периода от ");
			comment.append(sdf.format(entity.getDateOt()) + " до " + sdf.format(entity.getDateDo()) + ".");

			String t = SearchUtils.trimToNULL(entity.getEventInfo());
			if (t != null) {
				comment.append(" Описание: " + t + ".");
			}
			t = SearchUtils.trimToNULL(entity.getEventNote());
			if (t != null) {
				comment.append(" Забележка: " + t + ".");
			}

			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_PARTICIPATION, CODE_ZNACHENIE_NOTIF_ROLIA_EVENT_OUT, (SystemData) systemData);
			notif.setComment(comment.toString());
			notif.setAdresati(entity.getCodeRefList());
			notif.send();
		}
		super.delete(entity);
	}

	/** */
	@SuppressWarnings("unchecked")
	@Override
	public Event findById(Object id) throws DbErrorException {
		Event event = super.findById(id);

		if (event == null) {
			return event;
		}

		try {
			event.setHashNotifData(Objects.hash(event.getEventCode(), event.getDateOt(), event.getDateDo(), event.getEventInfo(), event.getEventNote() //
				, event.getAddrCountry(), event.getEkatte(), event.getAddrText()));

			event.setDbOrganizator(event.getOrganizator());

			List<Object> codeRefRows = createNativeQuery("select CODE_REF from EVENT_REFERENTS where EVENT_ID = ?1") //
				.setParameter(1, event.getId()).getResultList();
			if (codeRefRows.size() > 0) {
				List<Integer> codeRefList = new ArrayList<>(codeRefRows.size());
				for (Object row : codeRefRows) {
					codeRefList.add(((Number) row).intValue());
				}
				event.setCodeRefList(codeRefList);
				event.setDbCodeRefList(new ArrayList<>(codeRefList));
			}

			List<Object> resourcesRows = createNativeQuery("select CODE_RES from EVENT_RESOURCES where EVENT_ID = ?1") //
				.setParameter(1, event.getId()).getResultList();
			if (resourcesRows.size() > 0) {
				List<Integer> resourcesList = new ArrayList<>(resourcesRows.size());
				for (Object row : resourcesRows) {
					resourcesList.add(((Number) row).intValue());
				}
				event.setResourcesList(resourcesList);
				event.setDbResourcesList(new ArrayList<>(resourcesList));
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани обекти по събитието!", e);
		}

		return event;
	}

	/**
	 * @param event
	 * @param newFiles   ако има нещо ще трябва и нотификация за файлове
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public Event save(Event event, List<Object> newFiles, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		int codeAction = event.getId() == null ? SysConstants.CODE_DEIN_ZAPIS : SysConstants.CODE_DEIN_KOREKCIA;
		event.setAuditable(false); // за да не запише ред в журнала при записа на обекта

//		if (event.getResourcesList() != null && !event.getResourcesList().isEmpty()) { // валидации дали ресурса е свободен за
//																						// избрания интервал
//			StringBuilder sql = new StringBuilder();
//			sql.append(" select r.CODE_RES from EVENTS e ");
//			sql.append(" inner join EVENT_RESOURCES r on r.EVENT_ID = e.EVENT_ID ");
//			sql.append(" where ");
//			if (event.getId() != null) {
//				sql.append(" e.EVENT_ID != :eventId and ");
//			}
//			sql.append(" r.CODE_RES in (:resourcesList) ");
//			sql.append(" and (e.DATE_OT >= :dateFrom or e.DATE_DO >= :dateFrom) and (e.DATE_OT <= :dateTo or e.DATE_DO <= :dateTo) ");
//
//			Query query = createNativeQuery(sql.toString()) //
//				.setParameter("resourcesList", event.getResourcesList()) //
//				.setParameter("dateFrom", event.getDateOt()).setParameter("dateTo", event.getDateDo()); //
//
//			if (event.getId() != null) {
//				query.setParameter("eventId", event.getId());
//			}
//
//			@SuppressWarnings("unchecked")
//			List<Object> rows = query.getResultList();
//
//			if (!rows.isEmpty()) {
//				StringBuilder error = new StringBuilder();
//				error.append(rows.size() == 1 ? "Ресурс " : "Ресурси ");
//				for (Object row : rows) {
//					String resname = systemData.decodeItem(CODE_CLASSIF_EVENT_RESOURCES, ((Number) row).intValue(), getUserLang(), null);
//					error.append("\"" + resname + "\"");
//				}
//				error.append(rows.size() == 1 ? " е ангажиран " : " са ангажирани ");
//				error.append("в посочения период.");
//				throw new ObjectInUseException(error.toString());
//			}
//		}

		event.setEventNote(SearchUtils.trimToNULL(event.getEventNote()));

		Integer oldHashNotifData = event.getHashNotifData();

		Event saved = super.save(event);

		event.setHashNotifData(Objects.hash(event.getEventCode(), event.getDateOt(), event.getDateDo(), event.getEventInfo(), event.getEventNote() //
			, event.getAddrCountry(), event.getEkatte(), event.getAddrText()));
		boolean mainDataChanged = !Objects.equals(event.getHashNotifData(), oldHashNotifData);

		boolean notifResChanged = saveResources(event, saved, systemData);
		if (notifResChanged) { // и това влияе за промяна в основни данни на събититето
			mainDataChanged = true;
		}
		saveReferents(event, saved, mainDataChanged, newFiles, (SystemData) systemData);

		saveAudit(saved, codeAction); // тука вече трябва да всичко да е насетвано и записа в журнала е ОК

		saved.setHashNotifData(event.getHashNotifData()); // JPA MERGE
		saved.setDbOrganizator(event.getOrganizator());

		return saved;
	}

	/**
	 * @param event
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	@SuppressWarnings("unchecked")
	public Event sendFilesNotif(Event event, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		List<Object> newFiles = null;
		try {
			Date checkDate = event.getDateLastNotif() != null ? event.getDateLastNotif() : event.getDateReg();
			newFiles = createNativeQuery( //
				"select f.FILENAME from FILE_OBJECTS fo inner join FILES f on f.FILE_ID = fo.FILE_ID" //
					+ " where fo.OBJECT_ID = ?1 and fo.OBJECT_CODE = ?2 and fo.DATE_REG >= ?3") //
						.setParameter(1, event.getId()).setParameter(2, event.getCodeMainObject()).setParameter(3, checkDate) //
						.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при проверка за нови файлове в събитие!", e);
		}
		event.setDateLastNotif(new Date());

		return save(event, newFiles, systemData);
	}

	/**
	 * Преди да се изтрие събитието трябва да се изтрият и други данни, които не са мапнати дирекно през JPA
	 */
	@Override
	protected void remove(Event entity) throws DbErrorException, ObjectInUseException {
		try {
			int deleted = createNativeQuery("delete from EVENT_REFERENTS where EVENT_ID = ?1").setParameter(1, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} участника за събитие с EVENT_ID={}", deleted, entity.getId());

			deleted = createNativeQuery("delete from EVENT_RESOURCES where EVENT_ID = ?1").setParameter(1, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} ресурси за събитие с EVENT_ID={}", deleted, entity.getId());

		} catch (Exception e) {
			throw new DbErrorException("Грешка при изтриване на свързани обекти на събитие!", e);
		}
		super.remove(entity);
	}

	/**
	 * Запис на участниците
	 *
	 * @param event           този от екрана, който идва
	 * @param saved           този който е след merge на jpa
	 * @param mainDataChanged ако има промяна на старите трябва да им се прати нотификация
	 * @param newFiles        ако има нещо ще трябва и нотификация за файлове
	 * @param systemData
	 * @throws DbErrorException
	 */
	void saveReferents(Event event, Event saved, boolean mainDataChanged, List<Object> newFiles, SystemData systemData) throws DbErrorException {
		Integer countryBG = null;
		String setting = systemData.getSettingsValue("delo.countryBG");
		if (setting != null) {
			countryBG = Integer.valueOf(setting);
		}

		List<Integer> notifAdded = null;
		List<Integer> notifRemoved = null;
		List<Integer> unchangedList = null; // тука ще стоят непроменените участници и ще се изпозлва ако mainDataChanged=true

		if (event.getCodeRefList() == null || event.getCodeRefList().isEmpty()) { // на екрана няма
			if (event.getDbCodeRefList() != null && !event.getDbCodeRefList().isEmpty()) { // трие се ако има нещо от БД

				createNativeQuery("delete from EVENT_REFERENTS where EVENT_ID = ?1").setParameter(1, saved.getId()).executeUpdate();
				notifRemoved = event.getDbCodeRefList();
				event.setDbCodeRefList(null);
			}

		} else { // на екрана има
			Query insert = null;

			if (event.getDbCodeRefList() == null || event.getDbCodeRefList().isEmpty()) { // но няма стари от БД

				insert = createNativeQuery("insert into EVENT_REFERENTS(EVENT_ID,CODE_REF) values(?1,?2)");
				for (Integer codeRef : event.getCodeRefList()) { // всички се явяват нови
					insert.setParameter(1, saved.getId()).setParameter(2, codeRef).executeUpdate();
				}
				notifAdded = event.getCodeRefList();

			} else { // прави се анализ дали има промяна

				notifAdded = new ArrayList<>();
				unchangedList = new ArrayList<>();

				for (Integer codeRef : event.getCodeRefList()) {
					if (event.getDbCodeRefList().remove(codeRef)) {
						unchangedList.add(codeRef);

					} else { // значи е за нов запис
						if (insert == null) {
							insert = createNativeQuery("insert into EVENT_REFERENTS(EVENT_ID,CODE_REF) values(?1,?2)");
						}
						insert.setParameter(1, saved.getId()).setParameter(2, codeRef).executeUpdate();
						notifAdded.add(codeRef);
					}
				}
				if (!event.getDbCodeRefList().isEmpty()) { // ако нещо е останало е за изтриване
					createNativeQuery("delete from EVENT_REFERENTS where EVENT_ID = ?1 and CODE_REF in (?2)") //
						.setParameter(1, saved.getId()).setParameter(2, event.getDbCodeRefList()).executeUpdate();
					notifRemoved = event.getDbCodeRefList();
				}
			}
			event.setDbCodeRefList(new ArrayList<>(event.getCodeRefList()));
		}

		// заради JPA трябва да се оправи и обекта, който ще се върне
		saved.setCodeRefList(event.getCodeRefList());
		saved.setDbCodeRefList(event.getDbCodeRefList());

		// ако има пипане на организатора трябва да оправя ! notifAdded, notifRemoved, unchangedList !
		if (event.getDbOrganizator() != null || event.getOrganizator() != null) {

			Map<Integer, Set<Integer>> notifMap = new HashMap<>();

			notifMap.put(1, notifAdded == null ? new HashSet<>() : new HashSet<>(notifAdded));
			notifMap.put(2, notifRemoved == null ? new HashSet<>() : new HashSet<>(notifRemoved));
			notifMap.put(3, unchangedList == null ? new HashSet<>() : new HashSet<>(unchangedList));

			fixNotifOrganizator(event, notifMap);

			notifAdded = new ArrayList<>(notifMap.get(1));
			notifRemoved = new ArrayList<>(notifMap.get(2));
			unchangedList = new ArrayList<>(notifMap.get(3));
		}

		if (notifAdded != null && !notifAdded.isEmpty()) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

			StringBuilder comment = new StringBuilder();
			comment.append("Включен сте в провеждането на събитие от вид \"");
			comment.append(systemData.decodeItem(CODE_CLASSIF_VID_EVENT, saved.getEventCode(), getUserLang(), null) + "\"");
			comment.append(" в периода от ");
			comment.append(sdf.format(saved.getDateOt()) + " до " + sdf.format(saved.getDateDo()) + ".");

			String t = SearchUtils.trimToNULL(saved.getEventInfo());
			if (t != null) {
				comment.append(" Описание: " + t + ".");
			}
			t = SearchUtils.trimToNULL(saved.getEventNote());
			if (t != null) {
				comment.append(" Забележка: " + t + ".");
			}

			String newText = genNewTextNotif(saved, systemData, countryBG);

			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_PARTICIPATION, CODE_ZNACHENIE_NOTIF_ROLIA_EVENT_IN, systemData);
			notif.setComment(comment.toString() + newText);
			notif.setAdresati(notifAdded);
			notif.send();

		}

		if (notifRemoved != null && !notifRemoved.isEmpty()) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

			StringBuilder comment = new StringBuilder();
			comment.append("Вече не сте включен в провеждането на събитие от вид \"");
			comment.append(systemData.decodeItem(CODE_CLASSIF_VID_EVENT, saved.getEventCode(), getUserLang(), null) + "\"");
			comment.append(" в периода от ");
			comment.append(sdf.format(saved.getDateOt()) + " до " + sdf.format(saved.getDateDo()) + ".");

			String t = SearchUtils.trimToNULL(saved.getEventInfo());
			if (t != null) {
				comment.append(" Описание: " + t + ".");
			}
			t = SearchUtils.trimToNULL(saved.getEventNote());
			if (t != null) {
				comment.append(" Забележка: " + t + ".");
			}

			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_PARTICIPATION, CODE_ZNACHENIE_NOTIF_ROLIA_EVENT_OUT, systemData);
			notif.setComment(comment.toString());
			notif.setAdresati(notifRemoved);
			notif.send();

		}

		if (mainDataChanged && unchangedList != null && !unchangedList.isEmpty()) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

			StringBuilder comment = new StringBuilder();
			comment.append("Има промяна в данните за организирано събитие, в което участвате. ");
			comment.append("Новата информация е: Вид събитие \"");
			comment.append(systemData.decodeItem(CODE_CLASSIF_VID_EVENT, saved.getEventCode(), getUserLang(), null) + "\"");
			comment.append(", период на провеждане от ");
			comment.append(sdf.format(saved.getDateOt()) + " до " + sdf.format(saved.getDateDo()) + ".");

			String t = SearchUtils.trimToNULL(saved.getEventInfo());
			if (t != null) {
				comment.append(" Описание: " + t + ".");
			}
			t = SearchUtils.trimToNULL(saved.getEventNote());
			if (t != null) {
				comment.append(" Забележка: " + t + ".");
			}

			String newText = genNewTextNotif(saved, systemData, countryBG);

			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_PARTICIPATION, CODE_ZNACHENIE_NOTIF_ROLIA_EVENT_CHANGE, systemData);
			notif.setComment(comment.toString() + newText);
			notif.setAdresati(unchangedList);
			notif.send();
		}

		if (newFiles != null && !newFiles.isEmpty()) {
			boolean added = notifAdded != null && !notifAdded.isEmpty();
			boolean unchanged = unchangedList != null && !unchangedList.isEmpty();

			if (added || unchanged) { // ще има нотификация за нови файлове
				SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");

				StringBuilder comment = new StringBuilder();
				comment.append("Има добавени материали за събитие от вид \"");
				comment.append(systemData.decodeItem(CODE_CLASSIF_VID_EVENT, saved.getEventCode(), getUserLang(), null) + "\"");
				comment.append(" в периода от ");
				comment.append(sdf.format(saved.getDateOt()) + " до " + sdf.format(saved.getDateDo()) + ". Файлове: ");
				for (int i = 0; i < newFiles.size(); i++) {
					if (i > 0) {
						comment.append(", ");
					}
					comment.append(newFiles.get(i));
				}
				comment.append(".");

				Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
					, CODE_ZNACHENIE_NOTIFF_EVENTS_PARTICIPATION, CODE_ZNACHENIE_NOTIF_ROLIA_EVENT_CHANGE, systemData);
				notif.setComment(comment.toString());
				if (added) {
					notif.getAdresati().addAll(notifAdded);
				}
				if (unchanged) {
					notif.getAdresati().addAll(unchangedList);
				}
				notif.send();
			}
		}
	}

	/**
	 * Запис на ресурсите
	 *
	 * @param event този от екрана, който идва
	 * @param saved този който е след merge на jpa
	 * @return <code>true</code> ако има промяна в ресурсите, за които се изпраща нотификация
	 * @throws DbErrorException
	 */
	boolean saveResources(Event event, Event saved, BaseSystemData sd) throws DbErrorException {
		boolean notifResChanged = false;

		if (event.getResourcesList() == null || event.getResourcesList().isEmpty()) { // на екрана няма
			if (event.getDbResourcesList() != null && !event.getDbResourcesList().isEmpty()) { // трие се ако има нещо от БД

				createNativeQuery("delete from EVENT_RESOURCES where EVENT_ID = ?1").setParameter(1, saved.getId()).executeUpdate();

				for (Integer r : event.getDbResourcesList()) {
					if (sd.matchClassifItems(OmbConstants.CODE_CLASSIF_EVENT_RESOURCES_NOTIF, r, null)) {
						notifResChanged = true;
						break;
					}
				}
				event.setDbResourcesList(null);
			}

		} else { // на екрана има
			Query insert = null;

			if (event.getDbResourcesList() == null || event.getDbResourcesList().isEmpty()) { // но няма стари от БД

				insert = createNativeQuery("insert into EVENT_RESOURCES(EVENT_ID,CODE_RES) values(?1,?2)");
				for (Integer resource : event.getResourcesList()) { // всички се явяват нови
					insert.setParameter(1, saved.getId()).setParameter(2, resource).executeUpdate();

					if (sd.matchClassifItems(OmbConstants.CODE_CLASSIF_EVENT_RESOURCES_NOTIF, resource, null)) {
						notifResChanged = true;
					}
				}

			} else { // прави се анализ дали има промяна

				for (Integer resource : event.getResourcesList()) {
					if (!event.getDbResourcesList().remove(resource)) { // значи е за нов запис
						if (insert == null) {
							insert = createNativeQuery("insert into EVENT_RESOURCES(EVENT_ID,CODE_RES) values(?1,?2)");
						}
						insert.setParameter(1, saved.getId()).setParameter(2, resource).executeUpdate();

						if (sd.matchClassifItems(OmbConstants.CODE_CLASSIF_EVENT_RESOURCES_NOTIF, resource, null)) {
							notifResChanged = true;
						}
					}
				}
				if (!event.getDbResourcesList().isEmpty()) { // ако нещо е останало е за изтриване
					createNativeQuery("delete from EVENT_RESOURCES where EVENT_ID = ?1 and CODE_RES in (?2)") //
						.setParameter(1, saved.getId()).setParameter(2, event.getDbResourcesList()).executeUpdate();

					for (Integer r : event.getDbResourcesList()) {
						if (sd.matchClassifItems(OmbConstants.CODE_CLASSIF_EVENT_RESOURCES_NOTIF, r, null)) {
							notifResChanged = true;
							break;
						}
					}
				}
			}
			event.setDbResourcesList(new ArrayList<>(event.getResourcesList()));
		}

		// заради JPA трябва да се оправи и обекта, който ще се върне
		saved.setResourcesList(event.getResourcesList());
		saved.setDbResourcesList(event.getDbResourcesList());

		return notifResChanged;
	}

	/**
	 * една от двете стойности за организатор винаги е != нулл
	 *
	 * @param event
	 * @paramn otifMap
	 */
	private void fixNotifOrganizator(Event event, Map<Integer, Set<Integer>> notifMap) {
		Set<Integer> notifAdded = notifMap.get(1);
		Set<Integer> notifRemoved = notifMap.get(2);
		Set<Integer> unchangedList = notifMap.get(3);

		if (event.getDbOrganizator() == null) { // нов

			if (!unchangedList.contains(event.getOrganizator())) {
				boolean removed = notifRemoved.remove(event.getOrganizator());
				if (removed) { // ако е решен за махане
					unchangedList.add(event.getOrganizator());
				} else {
					notifAdded.add(event.getOrganizator());
				}
			}

		} else if (event.getOrganizator() == null) { // премахнат

			if (!unchangedList.contains(event.getDbOrganizator())) {
				boolean added = notifAdded.remove(event.getDbOrganizator());
				if (added) { // ако е решен за добавяне
					unchangedList.add(event.getDbOrganizator());
				} else {
					notifRemoved.add(event.getDbOrganizator());
				}
			}

		} else if (event.getDbOrganizator().equals(event.getOrganizator())) { // без промяна

			notifRemoved.remove(event.getOrganizator());
			notifAdded.remove(event.getOrganizator());
			unchangedList.add(event.getOrganizator());

		} else { // сменен

			if (!unchangedList.contains(event.getOrganizator())) {
				boolean removed = notifRemoved.remove(event.getOrganizator());
				if (removed) { // ако е решен за махане
					unchangedList.add(event.getOrganizator());
				} else {
					notifAdded.add(event.getOrganizator());
				}
			}
			if (!unchangedList.contains(event.getDbOrganizator())) {
				boolean added = notifAdded.remove(event.getDbOrganizator());
				if (added) { // ако е решен за добавяне
					unchangedList.add(event.getDbOrganizator());
				} else {
					notifRemoved.add(event.getDbOrganizator());
				}
			}
		}
	}

	/**
	 * @param saved
	 * @param systemData
	 * @param countryBG
	 * @return
	 * @throws DbErrorException
	 */
	private String genNewTextNotif(Event saved, SystemData systemData, Integer countryBG) throws DbErrorException {
		StringBuilder newText = new StringBuilder();
		if (countryBG != null && saved.getAddrCountry() != null && !saved.getAddrCountry().equals(countryBG)) {
			newText.append(" Адрес: ");
			newText.append(systemData.decodeItem(Constants.CODE_CLASSIF_COUNTRIES, saved.getAddrCountry(), getUserLang(), null));
		}
		if (saved.getEkatte() != null) {
			newText.append(newText.length() == 0 ? " Адрес: " : ", ");
			newText.append(systemData.decodeItem(SysConstants.CODE_CLASSIF_EKATTE, saved.getEkatte(), getUserLang(), null));
		}
		String addrText = SearchUtils.trimToNULL(saved.getAddrText());
		if (addrText != null) {
			newText.append(newText.length() == 0 ? " Адрес: " : ", ");
			newText.append(addrText);
		}
		if (newText.length() > 0) {
			newText.append("."); // край на адреса ако има
		}

		if (saved.getResourcesList() != null && !saved.getResourcesList().isEmpty()) {
			for (Integer r : saved.getResourcesList()) {
				if (systemData.matchClassifItems(OmbConstants.CODE_CLASSIF_EVENT_RESOURCES_NOTIF, r, null)) {
					newText.append(" " + systemData.decodeItem(OmbConstants.CODE_CLASSIF_EVENT_RESOURCES, r, getUserLang(), null) + ";");
				}
			}
			newText.replace(newText.length() - 1, newText.length(), ".");
		}
		return newText.toString();
	}
}
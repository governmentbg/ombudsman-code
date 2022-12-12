package com.ib.omb.db.dao;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DEF_PRAVA;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_STATUS_CONTINUED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DELO_ACCESS;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DELO_END;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_UNAUTHORIZED_OBJECT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_CONTR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_DELOVODITEL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_SLUJIT_DOST;
import static com.ib.system.SysConstants.CODE_DEIN_KOREKCIA;
import static com.ib.system.SysConstants.CODE_DEIN_ZAPIS;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_DA;
import static com.ib.system.utils.SearchUtils.asInteger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloAccess;
import com.ib.omb.db.dto.DeloAccessJournal;
import com.ib.omb.db.dto.DeloDelo;
import com.ib.omb.db.dto.Task;
import com.ib.omb.experimental.Notification;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.omb.utils.DocDostUtils;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.X;

/**
 * DAO for {@link Delo}
 *
 * @author belev
 */
public class DeloDAO extends AbstractDAO<Delo> {

	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(DeloDAO.class);

	/** @param user */
	public DeloDAO(ActiveUser user) {
		super(Delo.class, user);
	}
	
	/**
	 * Предостава селект за търсене на служители, които имат достъп до преписка<br>
	 * 
	 * [0]-CODE<br>
	 * [1]-REF_NAME<br>
	 * [2]-EMPL_POSITION<br>
	 * [3]-CODE_ZVENO<br>
	 * 
	 * @param deloId
	 * @param sd 
	 * @return
	 * @throws DbErrorException 
	 */
	public SelectMetadata createSelectDeloAccessList(Integer deloId, BaseSystemData sd) throws DbErrorException {
		@SuppressWarnings("unchecked")
		List<Object[]> all = createNativeQuery( // първо взимам аксес листа
			"select distinct CODE_REF, CODE_STRUCT from DELO_ACCESS_ALL where DELO_ID = ?1")
			.setParameter(1, deloId).getResultList();

		Date date = new Date(); // определям ръководните длъжности и шефовете към днешна дата, защото днес се кои са с достъп

		// трябва да сглоба списък от хора, които имат достъп директно или защото са шефове
		
		Set<Integer> refCodes = new HashSet<>();
		Set<Integer> zvena = new HashSet<>();
		for (Object[] row : all) {
			int refCode = ((Number)row[0]).intValue();
			refCodes.add(refCode); // това винаги присъства

			Integer zveno = SearchUtils.asInteger(row[1]);
			if (zveno != null && zveno.intValue() != refCode && !zvena.contains(zveno)) { 
				zvena.add(zveno); // трябва да добавя всички шефове, които имат достъп до това звено защото са шефове
				
				// освен всичко трябва да добавя и на това звено всички звена нагоре
				Integer parent = sd.getItemParentCode(Constants.CODE_CLASSIF_ADMIN_STR, zveno, date);
				while (parent != null && parent.intValue() != 0) {
					if (zvena.contains(parent)) {
						parent = null; // за да излезе от цикъла защото вече е минат по този път
					} else {
						zvena.add(parent);
						parent = sd.getItemParentCode(Constants.CODE_CLASSIF_ADMIN_STR, parent, date);
					}
				}
			}
		}

		if (!zvena.isEmpty()) { // за всеки случай иначе няма как да е празно

			for (Integer zveno : zvena) { // трява да намеря всички ръководни хора в това звено
				List<SystemClassif> items = sd.getChildrenOnNextLevel(Constants.CODE_CLASSIF_ADMIN_STR, zveno, date
																					, SysConstants.CODE_DEFAULT_LANG);
				for (SystemClassif item : items) {
					if (refCodes.contains(item.getCode())) {
						continue; // този дори и да е шеф го има директно
					}
					
					Integer position = (Integer) sd.getItemSpecific(Constants.CODE_CLASSIF_ADMIN_STR, item.getCode()
											, SysConstants.CODE_DEFAULT_LANG, date, OmbClassifAdapter.ADM_STRUCT_INDEX_POSITION);
					
					if (position != null && sd.matchClassifItems(OmbConstants.CODE_CLASSIF_BOSS_POSITIONS, position, date)) {
						refCodes.add(item.getCode()); // този е шеф и е получил достъп заради подчинен
					}
				}
			}
		}
		
		if (refCodes.isEmpty()) { // няма логика никой да няма достъп но все пак
			refCodes.add(Integer.MIN_VALUE);
		}
		
		String select = " select distinct r.CODE a0, r.REF_NAME a1, r.EMPL_POSITION a2, r.CODE_PARENT a3";
		
		String from = " from ADM_REFERENTS r"
			+ " left outer join DELO_ACCESS da on da.DELO_ID = :deloId and da.CODE_REF = r.CODE and da.EXCLUDE = :da";
		
		String where = " where r.CODE in (:codes)"
			+ " and r.DATE_OT = (select max (v.DATE_OT) from ADM_REFERENTS v where v.CODE = r.CODE and v.DATE_OT <> v.DATE_DO)"
			+ " and da.ID is null";

		Map<String, Object> params = new HashMap<>();
		params.put("deloId", deloId);
		params.put("da", SysConstants.CODE_ZNACHENIE_DA);
		params.put("codes", refCodes);
		
		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(distinct r.CODE) " + from + where);
		sm.setSql(select + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/** */
	@Override
	public void delete(Delo entity) throws DbErrorException, ObjectInUseException {
		try {
			Integer cnt = asInteger( // DELO_DOC.DELO_ID
				createNativeQuery("select count (*) as cnt from DELO_DOC where DELO_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Преписка "+entity.getIdentInfo()+" има връзка с документи и не може да бъде изтрита!");
			}

			cnt = asInteger( // DELO_DELO.DELO_ID
				createNativeQuery("select count (*) as cnt from DELO_DELO where DELO_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Преписка "+entity.getIdentInfo()+" има връзка с други преписки и не може да бъде изтрита!");
			}
			cnt = asInteger( // DELO_DELO.INPUT_DELO_ID
				createNativeQuery("select count (*) as cnt from DELO_DELO where INPUT_DELO_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Преписка "+entity.getIdentInfo()+" има връзка с други преписки и не може да бъде изтрита!");
			}

			cnt = asInteger( // DELO_DVIJ.DELO_ID
				createNativeQuery("select count (*) as cnt from DELO_DVIJ where DELO_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("За преписка "+entity.getIdentInfo()+" има регистрирано движение и не може да бъде изтрита!");
			}

			cnt = asInteger( // DELO_ARCHIVE.DELO_ID
				createNativeQuery("select count (*) as cnt from DELO_ARCHIVE where DELO_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Преписка "+entity.getIdentInfo()+" е включена в протокол за архивиране и не може да бъде изтрита!");
			}

			cnt = asInteger( // DELO_STORAGE.DELO_ID
				createNativeQuery("select count (*) as cnt from DELO_STORAGE where DELO_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Преписка "+entity.getIdentInfo()+" е съхранена и не може да бъде изтрита!");
			}

		} catch (ObjectInUseException e) {
			throw e; // за да не се преопакова

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани обекти към преписка!", e);
		}

		super.delete(entity);
	}

	/** Допънително тегли и други данни. */
	@Override
	public Delo findById(Object id) throws DbErrorException {
		Delo delo = super.findById(id);
		if (delo == null) {
			return delo;
		}

		// за последващите анализи при запис
		delo.setDbStatus(delo.getStatus());
		delo.setDbDeloType(delo.getDeloType());
		delo.setDbRnDelo(delo.getRnDelo());
		delo.setDbDeloYear(delo.getDeloYear());
		delo.setDbCodeRefLead(delo.getCodeRefLead());

		// ако преписката е вложена, в горния край на екрана се извежда информация за съдържащата преписка
		DeloDelo deloDelo = new DeloDeloDAO(getUser()).findByDeloId(delo.getId());
		delo.setDeloDelo(deloDelo);

		if (Objects.equals(delo.getStatus(), OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_DA)
				|| Objects.equals(delo.getStatus(), OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_UA)) { 
			
			// архивирана и има протокол
			
			StringBuilder sql = new StringBuilder();

			sql.append(" select s.DOC_ID, s.RN_DOC, s.DOC_DATE, s.PORED_DELO ");
			sql.append(" from DELO_ARCHIVE da ");
			sql.append(" inner join DOC r on da.PROTOCOL_ID = r.DOC_ID "); // работния
			sql.append(" inner join DOC s on s.DOC_ID = r.WORK_OFF_ID "); // официалния
			sql.append(" where da.DELO_ID = ?1 order by s.DOC_DATE desc ");

			Query protocolQuery = createNativeQuery(sql.toString()).setParameter(1, delo.getId());

			@SuppressWarnings("unchecked")
			List<Object[]> protocolData = protocolQuery.getResultList();
			if (!protocolData.isEmpty()) {
				delo.setProtocolData(protocolData.get(0));
				
				// това ще направи поредения номер слепен до номера на док
				delo.getProtocolData()[1] = DocDAO.formRnDoc(delo.getProtocolData()[1], delo.getProtocolData()[3]);
			}
		}

		return delo;
	}

	/**
	 * Връща списък с валидните индекси за текущата година, по които все още няма регистрирани номенклатурни дела за годината
	 * в регистратурата. Подава се днешна дата. Ако се подаде дата от предходна или следваща година ще работи към съответната година.
	 * Връща дани от вида: <br>
	 * [0]-SHEMA_ID<br>
	 * [1]-SHEMA_NAME<br>
	 * [2]-PREFIX<br>
	 * [3]-FROM_YEAR<br>
	 * [4]-TO_YEAR<br>
	 * [5]-PERIOD_TYPE (CODE_CLASSIF_SHEMA_PERIOD=119)<br>
	 * [6]-YEARS<br>
	 * [7]-COMPLETE_METHOD (CODE_CLASSIF_NOM_DELO_PRIKL=113)<br>
	 * 
	 * @param registraturaId
	 * @param date 
	 * @return
	 * @throws DbErrorException
	 */
	public List<Object[]> findValidNomDeloIndexList(Integer registraturaId, Date date) throws DbErrorException {
		try {
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(date);
			int yyyy = gc.get(Calendar.YEAR);
			
			StringBuilder sql = new StringBuilder();
			sql.append(" select s.SHEMA_ID, s.SHEMA_NAME, s.PREFIX, s.FROM_YEAR, s.TO_YEAR, s.PERIOD_TYPE, s.YEARS, s.COMPLETE_METHOD ");
			sql.append(" from DOC_SHEMA s ");
			sql.append(" left outer join DELO d on d.REGISTRATURA_ID = :rId and d.DELO_YEAR = :yyyy and d.DELO_TYPE = :nomDelo and upper(d.RN_DELO) = upper(s.PREFIX)  ");
			sql.append(" where s.FROM_YEAR <= :yyyy and (s.TO_YEAR is null or s.TO_YEAR >= :yyyy) "); // валидни за текуата година
			sql.append(" and d.DELO_ID is null "); // и да няма такова ном.дело в регистратурата

			Query query = createNativeQuery(sql.toString())
				.setParameter("rId", registraturaId)
				.setParameter("nomDelo", OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM)
				.setParameter("yyyy", yyyy);
			
			@SuppressWarnings("unchecked")
			List<Object[]> result = query.getResultList();
			return result;
			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на валидните индекси за номенклатурно дело!", e);
		}
	}
	
	/**
	 * Връща данните на индекса, по който е регистрирано делото. В списъка би трябвало да има само един ред.
	 * Връща дани от вида: <br>
	 * [0]-SHEMA_ID<br>
	 * [1]-SHEMA_NAME<br>
	 * [2]-PREFIX<br>
	 * [3]-FROM_YEAR<br>
	 * [4]-TO_YEAR<br>
	 * [5]-PERIOD_TYPE (CODE_CLASSIF_SHEMA_PERIOD=119)<br>
	 * [6]-YEARS<br>
	 * [7]-COMPLETE_METHOD (CODE_CLASSIF_NOM_DELO_PRIKL=113)<br>
	 * 
	 * @param deloId
	 * @return
	 * @throws DbErrorException
	 */
	public List<Object[]> findNomDeloIndexData(Integer deloId) throws DbErrorException {
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select s.SHEMA_ID, s.SHEMA_NAME, s.PREFIX, s.FROM_YEAR, s.TO_YEAR, s.PERIOD_TYPE, s.YEARS, s.COMPLETE_METHOD ");
			sql.append(" from DOC_SHEMA s ");
			sql.append(" inner join DELO d on upper(d.RN_DELO) = upper(s.PREFIX) ");
			sql.append(" where d.DELO_ID = :deloId and s.FROM_YEAR <= d.DELO_YEAR and (s.TO_YEAR is null or s.TO_YEAR >= d.DELO_YEAR) ");

			Query query = createNativeQuery(sql.toString())
				.setParameter("deloId", deloId);
			
			@SuppressWarnings("unchecked")
			List<Object[]> result = query.getResultList();
			return result;
			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на данни на индекса, по който е регистрирано дело!", e);
		}
	}
	
	/**
	 * Извличане на данни за преписка за за даване на достъп през задача
	 * 
	 * @param deloId
	 * @return
	 * @throws DbErrorException
	 */
	public Delo taskAccessFindDelo(Integer deloId) throws DbErrorException {
		Delo delo = super.findById(deloId);
		if (delo != null) {
			loadDeloAccess(delo);
		}
		return delo;
	}
	
	/**
	 * Запис на достъп до преписка през задача
	 * 
	 * @param delo
	 * @param systemData 
	 * @return
	 * @throws DbErrorException
	 */
	public Delo taskAccessSaveDelo(Delo delo, SystemData systemData) throws DbErrorException {
		Map<Integer, Integer> accessMap = new HashMap<>(); // key=codeRef
														// value 0=стар; 1=нов; 2=премахнат; 3=отказант достъп;
		boolean registraturaChanged = false;
		if (systemData.isDopDelovoditelRegistraturi()) { // трябва да проверя регистриралия какъв достъп е получил
			Integer admReg = (Integer) systemData.getItemSpecific(Constants.CODE_CLASSIF_ADMIN_STR, delo.getUserReg()
				, SysConstants.CODE_DEFAULT_LANG, delo.getDateReg(), OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
			if (admReg == null || delo.getRegistraturaId().equals(admReg)) { // само ако регистратора е работил в своята регистратура
				accessMap.put(delo.getUserReg(), 0); // регистратора като стар
			} else {
				registraturaChanged = true;
			}
		} else { // масовия случай
			accessMap.put(delo.getUserReg(), 0); // регистратора като стар
		}

		if (delo.getCodeRefLead() != null) {
			accessMap.put(delo.getCodeRefLead(), 0); // водещия като стар ако го има
		}

		deloAccessSave(CODE_DEIN_KOREKCIA, delo, delo.getDeloAccess(), accessMap);
		
		addRemoveDeloDost(delo, CODE_DEIN_KOREKCIA, accessMap, systemData, registraturaChanged);
		
		return delo;
	}

	
	/**
	 * <code>true</code> ако има изрично отнет достъп, иначе <code>false</code> 
	 * 
	 * @param deloId
	 * @param userId за усера, който се проверява (не за логнатия)
	 * @return
	 * @throws DbErrorException
	 */
	public boolean isDeloAccessDenied(Integer deloId, Integer userId) throws DbErrorException {
		try {
			@SuppressWarnings("unchecked")
			List<Object> list = createNativeQuery( //
				"select ID from DELO_ACCESS where DELO_ID = :deloId and CODE_REF = :codeRef and EXCLUDE = :da") //
				.setParameter("deloId", deloId).setParameter("codeRef", userId).setParameter("da", SysConstants.CODE_ZNACHENIE_DA) //
				.getResultList();

			return !list.isEmpty();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при проверка за достъп до дело/преписка.", e);
		}
	}
	
	/**
	 * Проверява дали усера, който е в ДАО-то има достъп до преписката
	 * @param delo
	 * @param editMode 
	 * @param sd 
	 * @return
	 * @throws DbErrorException
	 */
	public boolean hasDeloAccess(Delo delo, boolean editMode, BaseSystemData sd) throws DbErrorException {
		UserData ud = (UserData) getUser();

		if (ud.isDeloAccessDenied()) { // трябва да се провери дали не му е отнет достъп до тази преписка
			try {
				@SuppressWarnings("unchecked")
				List<Object> list = createNativeQuery( //
					"select ID from DELO_ACCESS where DELO_ID = :deloId and CODE_REF = :codeRef and EXCLUDE = :da") //
					.setParameter("deloId", delo.getId()).setParameter("codeRef", ud.getUserAccess()).setParameter("da", SysConstants.CODE_ZNACHENIE_DA) //
					.getResultList();
				if (!list.isEmpty()) {
					sendUnauthorizedObjectAccessNotif(delo, editMode, ud, sd);
					return false; // отнет достъп
				}
			} catch (Exception e) {
				throw new DbErrorException("Грешка при проверка за достъп до дело/преписка.", e);
			}
		}

		if (!editMode && Objects.equals(delo.getFreeAccess(), CODE_ZNACHENIE_DA)) { // FREE_ACCESS само за преглед
			if (delo.getRegistraturaId().equals(ud.getRegistratura())) {
				return true; // текущата регистратура
			}
			if (ud.hasAccess(OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS, delo.getRegistraturaId())) {
				return true; // ако е от позволените
			}
		}
		
		if (editMode) {
			if (!delo.getRegistraturaId().equals(ud.getRegistratura())) {
				sendUnauthorizedObjectAccessNotif(delo, editMode, ud, sd);
				return false; // няма значение какъв е, защото в друга не може актуализация
			}
			if (ud.hasAccess(CODE_CLASSIF_DEF_PRAVA, CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT)) {
				return true; // има Пълен достъп за актуализация на преписки/дела и документи
			}
		} else if (ud.hasAccess(CODE_CLASSIF_DEF_PRAVA, CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW) || ud.hasAccess(CODE_CLASSIF_DEF_PRAVA, CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT)) {
			// има Пълен достъп за разглеждане
			if (ud.hasAccess(OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS, delo.getRegistraturaId())) {
				return true; // само ако регистратурата е в позволените му
			}
		}

		if (delo.getUserReg().equals(ud.getUserAccess())) {
			return true; // регистриралия има достъп и не е нужно да се пуска селекта
		}
		if (delo.getCodeRefLead() != null && delo.getCodeRefLead().equals(ud.getUserAccess())) {
			return true; // водещия служител по преписката има достъп
		}
		
		Number count;
		try { // ще се пуска селект
			StringBuilder sql = new StringBuilder();

			sql.append(" select count (*) from DELO_ACCESS_ALL where DELO_ID = ?1 and");
			if (editMode || ud.getAccessZvenoList() == null) {
				sql.append(" CODE_REF in (" + ud.getUserAccess() + "," + ud.getZveno() + ") ");	
			} else {
				sql.append(" ( CODE_REF in (" + ud.getUserAccess() + "," + ud.getZveno() + ")");
				sql.append(" or CODE_STRUCT in (" + ud.getAccessZvenoList() + ") )");
			}

			Query query = createNativeQuery(sql.toString()).setParameter(1, delo.getId());

			count = (Number) query.getResultList().get(0);

		} catch (Exception e) {
			throw new DbErrorException("Грешка при проверка за достъп до дело/преписка.", e);
		}
		if (count.intValue() > 0) {
			return true;
		}
		sendUnauthorizedObjectAccessNotif(delo, editMode, ud, sd);
		return false;
	}

	/** формира и изпраща нотификация: Опит за неоторизиран достъп до обект
	 */
	private void sendUnauthorizedObjectAccessNotif(Delo delo, boolean editMode, UserData ud, BaseSystemData sd) {
		if (!editMode) { // в момента за преглед няма да се праща, защото има разни места по екраните,
							// от които може да се навигира и реално не е неоторизиран достъп
			return;
		}
		try {
			String setting = sd.getSettingsValue("delo.unauthorizedNotifUser");
			if (setting == null) {
				return; // не се иска да се праща
			}
			
			StringBuilder msg = new StringBuilder();
			msg.append("Потребител " + ud.getLoginName() + " се опитва да отвори в режим ");
//			msg.append(editMode ? "актуализация " : "преглед ");
			msg.append("актуализация ");
			msg.append(sd.decodeItem(OmbConstants.CODE_CLASSIF_DELO_TYPE, delo.getDeloType(), SysConstants.CODE_DEFAULT_LANG, delo.getDeloDate()));
			msg.append(" № " + delo.getRnDelo() + "/" + delo.getDeloYear());
			msg.append(" с ИД=" + delo.getId() + ".");
			
			List<Integer> adresati = new ArrayList<>();
			for (String userCode : setting.split(",")) {
				adresati.add(Integer.valueOf(userCode));
			}
			
			Notification notif = new Notification(ud.getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_UNAUTHORIZED_OBJECT, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, (SystemData) sd);
			notif.setComment(msg.toString());
			notif.setAdresati(adresati);
			notif.send();
		
		} catch (Exception e) {
			LOGGER.error("Грешка при формиране на нотификация: Опит за неоторизиран достъп до обект.", e);
		}
	}

	/**
	 * Зарежда данни за изричен достъп в обекта
	 *
	 * @param delo
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public void loadDeloAccess(Delo delo) throws DbErrorException {
		try {
			Query query = createQuery("select a from DeloAccess a where a.deloId = :deloId order by exclude desc, id") //
				.setParameter("deloId", delo.getId());

			delo.setDeloAccess(query.getResultList());

		} catch (Exception e) {
			throw new DbErrorException("Грешка при зареждане на изричен достъп за преписка!", e);
		}
	}

	/**
	 * Ако текущата е вложена в друга се визуализира бутон „Изваждане“, при натискането на който, след потвърждение, преписката се
	 * изтрива от съдържащата я и ако е била приключена, статусът й се променя на „продължена“ (В този случай да се извежда
	 * съобщение на екрана за потвърждение – „Изваждането на преписката ще промени статусът й на „продължена“. Сигурни ли сте, че
	 * искате да продължите?“).
	 *
	 * @param delo
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public Delo removeCurrentFromDelo(Delo delo, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {

		DeloDelo deloDelo = delo.getDeloDelo();
		if (deloDelo == null || deloDelo.getId() == null) { // няма какво да се изтрива
			LOGGER.error("removeCurrentFromDelo -> Delo.deloDelo not saved!");

			return delo;
		}

		deloDelo.setAuditable(true);
		new DeloDeloDAO(getUser()).delete(deloDelo);
		delo.setDeloDelo(new DeloDelo());
		delo.getDeloDelo().setDelo(new Delo());

		// проверявам със статуса в БД, за да не е сменено нещо по екрана преди да е натиснат бутона за изваждането
		if (Objects.equals(delo.getDbStatus(), CODE_ZNACHENIE_DELO_STATUS_COMPLETED)
				&& Objects.equals(deloDelo.getDelo().getDeloType(), CODE_ZNACHENIE_DELO_TYPE_NOM)) { // само за това изваждане има логика
			delo.setStatus(CODE_ZNACHENIE_DELO_STATUS_CONTINUED);
			delo.setStatusDate(new Date());

			StringBuilder sb = new StringBuilder();
			sb.append(deloDelo.getDelo().getRnDelo()+"/");
			sb.append(DateUtils.printDate(deloDelo.getDelo().getDeloDate()));
			if (deloDelo.getTomNomer() != null) {
				sb.append(",т." + deloDelo.getTomNomer());
			}
			sb.append(" на " + DateUtils.printDate(new Date()));

			delo.setPrevNomDelo(sb.toString());

			save(delo, false, systemData, null);
		}
		return delo;
	}

	/** */
	@Override
	public Delo save(Delo delo) throws DbErrorException {

		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(delo.getDeloDate());
		delo.setDeloYear(gc.get(Calendar.YEAR)); // за да си е има и в БД

		return super.save(delo);
	}

	/**
	 * Запис на дело. Прави влагане на делото в преписка при въведени данни в delo.deloDelo.
	 *
	 * @param delo
	 * @param systemData
	 * @param closeTasks при <code>true</code> се затварят всички задачи за документите вложени в преписка
	 * @param dvijMsg това ще стои като холдер на съобщението, което трябва да са се покаже ако има влгане и се служи движение
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public Delo save(Delo delo, boolean closeTasks, BaseSystemData systemData, X<String> dvijMsg) throws DbErrorException, ObjectInUseException {
		SystemData sd = (SystemData) systemData;
		
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(delo.getDeloDate());
		delo.setDeloYear(gc.get(Calendar.YEAR)); // за да си е има и в БД

		Map<Integer, Integer> accessMap = new HashMap<>(); // key=codeRef
		   													// value 0=стар; 1=нов; 2=премахнат; 3=отказант достъп;

		boolean registraturaChanged = false; // ще се вдигне флаг ако е деловодител и прави запис на ново дело в различна 
											// регистратура от която е назначен като служител !?!?

		boolean changedStatus, changedRnDelo, changedDeloType, changedDeloYear;
		int codeAction;

		if (delo.getId() == null) {
			changedStatus = true;
			changedRnDelo = true;
			changedDeloType = true;
			changedDeloYear = true;

			codeAction = CODE_DEIN_ZAPIS;

			if (sd.isDopDelovoditelRegistraturi()) { // възможно е да се работи от други регистратури спрямо назначението
				Integer admReg = (Integer) sd.getItemSpecific(Constants.CODE_CLASSIF_ADMIN_STR, getUserId()
					, SysConstants.CODE_DEFAULT_LANG, null, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
				if (delo.getRegistraturaId().equals(admReg)) {
					accessMap.put(getUserId(), 1); // регистратора като нов
				} else {
					registraturaChanged = true; // има смяна и регистратора ще влезне само с личен достъп без звено
				}
			} else { // масовия случай
				accessMap.put(getUserId(), 1); // регистратора като нов
			}
			if (delo.getCodeRefLead() != null) {
				accessMap.put(delo.getCodeRefLead(), 1); // водещия като нов
			}

		} else {
			changedStatus = !Objects.equals(delo.getStatus(), delo.getDbStatus());
			changedRnDelo = !Objects.equals(delo.getRnDelo(), delo.getDbRnDelo());
			changedDeloType = !Objects.equals(delo.getDeloType(), delo.getDbDeloType());
			changedDeloYear = !Objects.equals(delo.getDeloYear(), delo.getDbDeloYear());

			codeAction = CODE_DEIN_KOREKCIA;
			
			if (sd.isDopDelovoditelRegistraturi()) { // трябва да проверя регистриралия какъв достъп е получил
				Integer admReg = (Integer) sd.getItemSpecific(Constants.CODE_CLASSIF_ADMIN_STR, delo.getUserReg()
					, SysConstants.CODE_DEFAULT_LANG, delo.getDateReg(), OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
				if (admReg == null || delo.getRegistraturaId().equals(admReg)) { // само ако регистратора е работил в своята регистратура
					accessMap.put(delo.getUserReg(), 0); // регистратора като стар
				} else {
					registraturaChanged = true;
				}
			} else { // масовия случай
				accessMap.put(delo.getUserReg(), 0); // регистратора като стар
			}
			addCodeRefLeadAccessMap(delo, accessMap);
		}

		if (delo.getId() == null || changedRnDelo || changedDeloType || changedDeloYear) { // в рамките на годината
																							// регистрационният номер е уникален
			String error = validateRnDelo(delo);
			if (error != null) {
				throw new ObjectInUseException(error);
			}
		}

		boolean endDelo = false;
		if (changedStatus && Objects.equals(delo.getStatus(), CODE_ZNACHENIE_DELO_STATUS_COMPLETED)) {
			completeDelo(delo, closeTasks, null);

			endDelo = true;
		}

		delo.setAuditable(false); // за да не запише ред в журнала при записа на обекта

		Integer oldCodeRefLead = delo.getDbCodeRefLead();

		DeloDelo deloDelo = delo.getDeloDelo(); // вложено в преписка
		if (delo.getPrevNomDelo() != null 
			&& deloDelo != null && deloDelo.getId() == null && deloDelo.getDeloId() != null
			&& Objects.equals(deloDelo.getDelo().getDeloType(), CODE_ZNACHENIE_DELO_TYPE_NOM)) {

			delo.setPrevNomDelo(null); // преписка се влага в номенклатурно дело
		}
		
		Delo saved = super.save(delo); // основния запис
		
		if (deloDelo != null && deloDelo.getId() == null && deloDelo.getDeloId() != null) {
			// избрана е преписка, в която трябва да се вложи текущата

			deloDelo.setInputDeloId(saved.getId());

			if (deloDelo.getInputDate() == null) { // дори и при нов може да е въведно и не трябва да се замазва
				deloDelo.setInputDate(DateUtils.startDate(new Date()));
			}
			if (deloDelo.getTomNomer() == null) {
				deloDelo.setTomNomer(1);
			}

			deloDelo.setAuditable(true);

			DeloDeloDAO deloDeloDao = new DeloDeloDAO(getUser());
			deloDeloDao.save(deloDelo);
			if (dvijMsg != null) { // само ако отвън който го вика иска
				Integer s9 = sd.getRegistraturaSetting(delo.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_9);
				if (s9 != null && s9.equals(SysConstants.CODE_ZNACHENIE_DA)) { // и настройката е включена
					String msg = deloDeloDao.checkCreateDvij(deloDelo, systemData);
					dvijMsg.set(msg); // и ако има ще се покаже на екрана
				}
			}
		}

		// статуса на преписката не променя нищо по жалбата
//		if (changedStatus 
//			&& saved.getStatus().equals(CODE_ZNACHENIE_DELO_STATUS_CONTINUED)
//			&& saved.getInitDocId() != null) {
//
//			reopenJalbaDoc(saved.getInitDocId(), saved.getDeloType());
//		}

		saved.setDeloDelo(deloDelo); // заради merge JPA

		saved.setProtocolData(delo.getProtocolData()); // JPA merge !!!

		// за последващите анализи при запис
		saved.setDbStatus(saved.getStatus());
		saved.setDbDeloType(saved.getDeloType());
		saved.setDbRnDelo(saved.getRnDelo());
		saved.setDbDeloYear(saved.getDeloYear());
		saved.setDbCodeRefLead(saved.getCodeRefLead());

		List<Integer> newAccessCodes = deloAccessSave(codeAction, saved, delo.getDeloAccess(), accessMap);

		saveAudit(saved, codeAction); // тука вече трябва да всичко да е насетвано и записа в журнала е ОК
		
		addRemoveDeloDost(saved, codeAction, accessMap, sd, registraturaChanged); // граденето на достъп

		// !!! ДЖУРКАНЕТО НА НОТИФИКАЦИИ !!!
		boolean notifDeloEndFlag = endDelo && (saved.getDeloDelo() == null || saved.getDeloDelo().getId() == null); // прикл и не
		notifDeloSave(saved, oldCodeRefLead, notifDeloEndFlag, newAccessCodes, sd);

		return saved;
	}

	/**
	 * Повторно отваряне на жалба/нпм/самосезиране
	 */
	private boolean reopenJalbaDoc(Integer docId, Integer deloType) throws DbErrorException {
		String tableName;
		String journalMsg;
		if (deloType.equals(OmbConstants.CODE_ZNACHENIE_DELO_TYPE_JALBA)) {
			tableName = "doc_jalba";
			journalMsg = "Жалбата е повторно отворена поради подновяване на работа по преписката.";
		} else if (deloType.equals(OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NPM)) {
			tableName = "doc_spec";
			journalMsg = "Заповедта за проверка по НПМ е повторно отворена поради подновяване на работа по преписката.";
		} else if (deloType.equals(OmbConstants.CODE_ZNACHENIE_DELO_TYPE_SAMOS)) {
			tableName = "doc_spec";
			journalMsg = "Решението за проверка по самосезиране е повторно отворено поради подновяване на работа по преписката.";
		} else {
			return false;
		}

		Integer userId = getUserId();
		Date date = new Date();
		
		int cnt = createNativeQuery("update "+tableName+" set sast = :newSast, sast_date = :newDate, date_last_mod = :newDate, user_last_mod = :userMod"
			+ " where doc_id = :docId and sast != :newSast")
			.setParameter("newSast", OmbConstants.CODE_ZNACHENIE_JALBA_SAST_REOPENED)
			.setParameter("newDate", date).setParameter("userMod", userId)
			.setParameter("docId", docId)
			.executeUpdate();
		if (cnt == 0) {
			return false;
		}
		SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, docId
			, journalMsg);

		journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
		journal.setDateAction(date);
		journal.setIdUser(userId);

		saveAudit(journal);

		return true;
	}
	
	/**
	 * Приключване на жалба/нпм/самосезиране
	 */
	private boolean completeJalbaDoc(Integer docId, Integer deloType) throws DbErrorException {
		String tableName;
		String journalMsg;
		if (deloType.equals(OmbConstants.CODE_ZNACHENIE_DELO_TYPE_JALBA)) {
			tableName = "doc_jalba";
			journalMsg = "Жалбата е приключена поради приключване на работа по преписката.";
		} else if (deloType.equals(OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NPM)) {
			tableName = "doc_spec";
			journalMsg = "Заповедта за проверка по НПМ е приключена поради приключване на работа по преписката.";
		} else if (deloType.equals(OmbConstants.CODE_ZNACHENIE_DELO_TYPE_SAMOS)) {
			tableName = "doc_spec";
			journalMsg = "Решението за проверка по самосезиране е приключено поради приключване на работа по преписката.";
		} else {
			return false;
		}

		Integer userId = getUserId();
		Date date = new Date();
		
		int cnt = createNativeQuery("update "+tableName+" set sast = :newSast, sast_date = :newDate, date_last_mod = :newDate, user_last_mod = :userMod"
			+ " where doc_id = :docId and sast != :newSast")
			.setParameter("newSast", OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED)
			.setParameter("newDate", date).setParameter("userMod", userId)
			.setParameter("docId", docId)
			.executeUpdate();
		if (cnt == 0) {
			return false;
		}
		SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, docId
			, journalMsg);

		journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
		journal.setDateAction(date);
		journal.setIdUser(userId);

		saveAudit(journal);

		return true;
	}
	
	/**
	 * Групова смяна на статус - row[0] = DELO_ID, row[4] = STATUS
	 * 
	 * @param selected
	 * @param newStatus 
	 * @param statusDate да се избира от екрана или направо да се пуска днешна
	 * @param closeTasks 
	 * @param systemData 
	 * @throws DbErrorException
	 * @throws ObjectInUseException 
	 */
	public void groupStatusChange(List<Object[]> selected, Integer newStatus, Date statusDate, boolean closeTasks, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		if (Objects.equals(CODE_ZNACHENIE_DELO_STATUS_COMPLETED, newStatus)) { // за тези е по различно
			for (Object[] row : selected) {
				if (Objects.equals(CODE_ZNACHENIE_DELO_STATUS_COMPLETED, SearchUtils.asInteger(row[4]))) {
					continue; // вече приключените се пропускат
				}
				
				Delo delo = findById(SearchUtils.asInteger(row[0]));
				
				if (!Objects.equals(CODE_ZNACHENIE_DELO_STATUS_COMPLETED, delo.getStatus())) { // може междувременно да е приключена
																					// защото е вложена в друга от избраните
					delo.setStatus(newStatus);
					delo.setStatusDate(statusDate);

					save(delo, closeTasks, systemData, null);
				}
			}
			
		} else { // обикновена смяна на статус
			
			String ident = "Статусът на преписката е променен на "
								+ systemData.decodeItem(OmbConstants.CODE_CLASSIF_DELO_STATUS, newStatus, getUserLang(), null) 
								+ " при групова операция с преписки.";

			for (Object[] row : selected) {
				Integer deloId = SearchUtils.asInteger(row[0]);
				
				Delo delo = getEntityManager().find(Delo.class, deloId); // за да не влачи всичко от findById
				
				delo.setStatus(newStatus);
				delo.setStatusDate(statusDate);
				
				saveSysOkaJournal(delo, ident);
			}
		}
	}

	/**
	 * Прави анализ и създава нотификациите за преписка. В момента се вика в ДАО-то накрая на записа. Ако в бъдеще се реши че
	 * трябва да се вика в бийна след записа някой трябва да се погржи да има необходимите параметри.
	 *
	 * @param delo
	 * @param oldCodeRefLead
	 * @param notifDeloEndFlag
	 * @param newAccessCodes 
	 * @param systemData
	 * @throws DbErrorException
	 */
	public void notifDeloSave(Delo delo, Integer oldCodeRefLead, boolean notifDeloEndFlag, List<Integer> newAccessCodes, SystemData systemData) throws DbErrorException {
//		if (oldCodeRefLead == null) {
//			if (delo.getCodeRefLead() != null) { // нов водещ
//
//				Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
//					, CODE_ZNACHENIE_NOTIFF_EVENTS_DELO_REF_NEW, CODE_ZNACHENIE_NOTIF_ROLIA_VODEST, systemData);
//				notif.setDelo(delo);
//				notif.setAdresat(delo.getCodeRefLead());
//				notif.send();
//			}
//		} else if (!oldCodeRefLead.equals(delo.getCodeRefLead())) { // разлика в водещите
//
//			Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
//				, CODE_ZNACHENIE_NOTIFF_EVENTS_DELO_REF_CHANGE, CODE_ZNACHENIE_NOTIF_ROLIA_VODEST, systemData);
//			notif.setDelo(delo);
//			notif.setAdresat(oldCodeRefLead);
//			notif.send();
//
//			if (delo.getCodeRefLead() != null) { // нов водещ
//
//				Notification notif2 = new Notification(((UserData)getUser()).getUserAccess() , null
//					, CODE_ZNACHENIE_NOTIFF_EVENTS_DELO_REF_NEW, CODE_ZNACHENIE_NOTIF_ROLIA_VODEST, systemData);
//				notif2.setDelo(delo);
//				notif2.setAdresat(delo.getCodeRefLead());
//				notif2.send();
//			}
//		}

		if (notifDeloEndFlag) {
			Map<Integer, Object> specifics = new HashMap<>();
			specifics.put(OmbClassifAdapter.USERS_INDEX_REGISTRATURA, delo.getRegistraturaId());
			specifics.put(OmbClassifAdapter.USERS_INDEX_DELOVODITEL, SysConstants.CODE_ZNACHENIE_DA);

			// така ще извади от кеша само потребители, които са деловодители в търсената регистратура
			List<SystemClassif> items = systemData.queryClassification(SysConstants.CODE_CLASSIF_USERS, null, delo.getDeloDate(), getUserLang(), specifics);

			if (items.isEmpty()) {
				LOGGER.warn("OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DELO_END ! няма налични деловодители !");

			} else {
				List<Integer> delovoditeli = new ArrayList<>(items.size());
				for (SystemClassif item : items) {
					delovoditeli.add(item.getCode());
				}

				Notification notif = new Notification(((UserData)getUser()).getUserAccess(), delo.getRegistraturaId()
					, CODE_ZNACHENIE_NOTIFF_EVENTS_DELO_END, CODE_ZNACHENIE_NOTIF_ROLIA_DELOVODITEL, systemData);
				notif.setDelo(delo);
				notif.setAdresati(delovoditeli);
				notif.send();
			}
		}
		
		if (newAccessCodes != null && !newAccessCodes.isEmpty()) {

			Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
				, CODE_ZNACHENIE_NOTIFF_EVENTS_DELO_ACCESS, CODE_ZNACHENIE_NOTIF_ROLIA_SLUJIT_DOST, systemData);
			notif.setDelo(delo);
			notif.setAdresati(newAccessCodes);
			notif.send();
		}

	}

	/**
	 * Валидира се номера. Ако номера е ОК връща NULL, иначе тескта на причината за невалидност, която може да се покаже на
	 * екрана.
	 *
	 * @param delo
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public String validateRnDelo(Delo delo) throws DbErrorException {
		if (delo.getRnDelo() != null) { // важно е да няма интервали в началото и края
			delo.setRnDelo(delo.getRnDelo().trim());
		} else {
			return "Моля, въведете номер.";
		}
		if (delo.getDeloType() == null) {
			return "Моля, въведете тип.";
		}

		GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(delo.getDeloDate());
		int yyyy = gc.get(Calendar.YEAR);
		
		boolean typeNom = delo.getDeloType().equals(CODE_ZNACHENIE_DELO_TYPE_NOM);
		try {
			int docuMonth = SystemData.getDocuMonth();
			int docuDay = SystemData.getDocuDay();

			if ((docuMonth == 1 && docuDay == 1) // отчетния период е календарна година
				|| typeNom) { // и винаги ако е номенклатурно дело

				StringBuilder sql = new StringBuilder();
				sql.append("select d.DELO_ID from DELO d where d.REGISTRATURA_ID = ?1 and upper(d.RN_DELO) = ?2 and d.DELO_YEAR = ?3 and d.DELO_TYPE = ?4");
				if (delo.getId() != null) { // корекция и да се изключи текущото
					sql.append(" and d.DELO_ID != ?5");
				}

				Query query = createNativeQuery(sql.toString()) //
					.setParameter(1, delo.getRegistraturaId()).setParameter(2, delo.getRnDelo().toUpperCase()).setParameter(3, yyyy) //
					.setParameter(4, delo.getDeloType());
				if (delo.getId() != null) {
					query.setParameter(5, delo.getId());
				}

				List<Object> list = query.getResultList();
				if (!list.isEmpty()) {
					if (typeNom) {
						return "Вече има регистрирано номенклатурно дело с номер " + delo.getRnDelo() + " за " + yyyy + " година.";
					}
					return "Вече има регистрирана преписка с номер " + delo.getRnDelo() + " за " + yyyy + " година.";
				}
			} else { // периода е от-до произволна дата

				StringBuilder sql = new StringBuilder();
				sql.append("select d.DELO_ID from DELO d where d.REGISTRATURA_ID = ?1 and upper(d.RN_DELO) = ?2");
				sql.append(" and d.DELO_DATE >= ?3 and d.DELO_DATE <= ?4 and d.DELO_TYPE = ?5");
				if (delo.getId() != null) { // корекция и да се изключи текущото
					sql.append(" and d.DELO_ID != ?6");
				}

				Query query = createNativeQuery(sql.toString()) //
					.setParameter(1, delo.getRegistraturaId()).setParameter(2, delo.getRnDelo().toUpperCase());

				gc.set(yyyy, docuMonth - 1, docuDay, 0, 0, 0); // началото на периода

				if (gc.getTimeInMillis() > delo.getDeloDate().getTime()) {
					yyyy -= 1; // периода е започнал през миналата година
					gc.set(Calendar.YEAR, yyyy);
				}
				query.setParameter(3, gc.getTime());
				Date from = gc.getTime(); // ако има грешка да се изолзва

				gc.set(yyyy + 1, docuMonth - 1, docuDay - 1, 23, 59, 59); // предходния ден на следващата година
				query.setParameter(4, gc.getTime());

				query.setParameter(5, delo.getDeloType());
				if (delo.getId() != null) {
					query.setParameter(6, delo.getId());
				}

				List<Object> list = query.getResultList();
				if (!list.isEmpty()) {
					return "Вече има регистрирана преписка с номер " + delo.getRnDelo() + " в периода " + DateUtils.printDate(from) + "-" + DateUtils.printDate(gc.getTime()) + ".";
				}
			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при валидация на номер на дело/преписка!", e);
		}

		if (typeNom) { // тука трябва да се гледа има ли индекс в таблицата DOC_SHEMA
			try {
				String sql = "select count (*) cnt from DOC_SHEMA where PREFIX = :prefix and FROM_YEAR <= :yyyy and (TO_YEAR is null or TO_YEAR >= :yyyy)";
				Query query = createNativeQuery(sql) //
					.setParameter("prefix", delo.getRnDelo()).setParameter("yyyy", yyyy);

				Number cnt = (Number) query.getResultList().get(0);
				if (cnt.intValue() == 0) {
					return "Въведеният номер на номенклатурно дело не е валиден индекс за посочената година." //
						+ " Моля, проверете кои са валидните индекси чрез дейност \"Схема за съхранение на документи\"." //
						+ " Ако е необходимо, най-напред дефинирайте желаният индекс там.";
				}

			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на индекс за номенклатурно дело!", e);
			}
		}
		return null; // това значи че нещата са ОК
	}

	/**
	 * Преди да се изтрие преписката трябва да се изтрият и други данни, които не са мапнати дирекно през JPA
	 */
	@Override
	protected void remove(Delo entity) throws DbErrorException, ObjectInUseException {
		try {
			// DELO_ACCESS
			int deleted = createNativeQuery("delete from DELO_ACCESS where DELO_ID = ?1").setParameter(1, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} DELO_ACCESS за преписка с DELO_ID={}", deleted, entity.getId());

			// DELO_ACCESS_ALL
			deleted = createNativeQuery("delete from DELO_ACCESS_ALL where DELO_ID = ?1").setParameter(1, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} DELO_ACCESS_ALL за преписка с DELO_ID={}", deleted, entity.getId());

			// DOC_ACCESS_ALL
			deleted = createNativeQuery("delete from DOC_ACCESS_ALL where OBJECT_CODE = ?1 and OBJECT_ID = ?2")
				.setParameter(1, OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO).setParameter(2, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} реда от DOC_ACCESS_ALL за дело с DELO_ID={}", deleted, entity.getId());

		} catch (Exception e) {
			throw new DbErrorException("Грешка при изтриване на свързани обекти на преписка!", e);
		}

		super.remove(entity);
	}

	/**
	 * Приключване на преписка
	 *
	 * @param delo
	 * @param closeTasks при <code>true</code> се затварят всички задачи за документите вложени в преписка
	 * @param activeStatusList списък с активните статуси. ако не са подадени ще се определят
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	void completeDelo(Delo delo, boolean closeTasks, List<Integer> activeStatusList) throws DbErrorException {
		Date date = new Date();
		
		if (closeTasks) { // трябва задачите по документите вложени в преписката да се затворят
			
			if (activeStatusList == null) { // трябва да се вземе от класификацията, но нямам налична тук систем датата
				activeStatusList = Arrays.asList(1,2,3,4,5);
			}
			
			List<Task> docTasks;
			try {
				StringBuilder sql = new StringBuilder();
				sql.append(" select t from Task t ");
				sql.append(" inner join DeloDoc dd on dd.inputDocId = t.docId ");
				sql.append(" where dd.deloId = :deloId and t.status in (:activeStatusList) ");
				
				docTasks = createQuery(sql.toString()) //
					.setParameter("deloId", delo.getId()).setParameter("activeStatusList", activeStatusList) //
					.getResultList();
				
			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на задачи за документи вложени в преписка!", e);
			}
			
			TaskDAO dao = new TaskDAO(getUser());
			for (Task task : docTasks) {
				
				task.setStatus(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_AUTO_PRIKL);
				task.setStatusComments("Приключена при приключване на преписка");
				task.setStatusDate(date);
				task.setStatusUserId(((UserData)getUser()).getUserSave());

				task.setAuditable(false); // ще се журналира по друг начин след запис
				dao.save(task); // прост запис

				String ident = "Задача "+task.getRnTask()+" е приключена автоматично, при приключване на преписка "+delo.getIdentInfo()+
														", защото е регистрирана по документ, вложен в тази преписка.";
				
				SystemJournal journal = new SystemJournal(task.getCodeMainObject(), task.getId(), ident);

				journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
				journal.setDateAction(new Date());
				journal.setIdUser(getUserId());

				journal.setJoinedIdObject1(task.getDocId());
				journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
				if (task.getEndDocId() != null) {
					journal.setJoinedIdObject2(task.getEndDocId());
					journal.setJoinedCodeObject2(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
				}
				saveAudit(journal);
			}
		}
		
		// статуса на преписката не променя нищо по жалбата
//		if (delo.getInitDocId() != null) {
//			completeJalbaDoc(delo.getInitDocId(), delo.getDeloType());
//		}

		// трябва да се приключат и всички вложени преписки
		List<Delo> includedDeloList;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select d from Delo d ");
			sql.append(" inner join DeloDelo dd on dd.inputDeloId = d.id ");
			sql.append(" where dd.deloId = :deloId and d.status != :statusCompleted ");
			
			includedDeloList = createQuery(sql.toString()) //
				.setParameter("deloId", delo.getId()).setParameter("statusCompleted", CODE_ZNACHENIE_DELO_STATUS_COMPLETED) //
				.getResultList();
			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на вложени преписки!", e);
		}

		for (Delo includedDelo : includedDeloList) {
			includedDelo.setStatus(CODE_ZNACHENIE_DELO_STATUS_COMPLETED);
			includedDelo.setStatusDate(date);
			saveSysOkaJournal(includedDelo, "Преписка "+includedDelo.getIdentInfo()+" е приключена, при приключване на съдържащата я преписка "+delo.getIdentInfo()+".");
			
			completeDelo(includedDelo, closeTasks, activeStatusList);
		}
	}

	/**
	 * Запис на изричния достъп
	 *
	 * @param dein
	 * @param saved
	 * @param deloAccess
	 * @throws DbErrorException
	 */
	List<Integer> deloAccessSave(int dein, Delo saved, List<DeloAccess> deloAccess, Map<Integer, Integer> accessMap) throws DbErrorException {
		if (dein == CODE_DEIN_ZAPIS && (deloAccess == null || deloAccess.isEmpty())) {
			return null; // нов запис и няма
		}

		if (deloAccess == null) { // корекция и не е бил зареден и за това пускам селект да се изтегли каквото има, за да сработи
									// определянето на достъпа
			// трябва да проверя дали няма някаква промяна по хората в преписката, защото ако има трябва и това да се зареди, за да се прави анализ
			boolean loadit = false;
			for (Integer value : accessMap.values()) {
				if (value.intValue() != 0) {
					loadit = true;
					break;
				} 
			}
			if (!loadit) { // няма да се зарежда, защото няма да повлияе на определянето на достъпа
				return null; 
			}

			loadDeloAccess(saved);
			deloAccess = saved.getDeloAccess(); // за да сработи надолу логиката
		}

		if (deloAccess.isEmpty()) {
			return null; // зареден е но няма и няма какво да се прави като анализ
		}

		Query deleteQuery = null;

		List<Integer> newAccessCodes = new ArrayList<>();
		
		int i = 0;
		DeloAccessJournal deloAccessJournal = new DeloAccessJournal();
		deloAccessJournal.setIdDelo(saved.getId());
		deloAccessJournal.setIdentObject(saved.getIdentInfo());
		boolean hasChange = false;
		while (i < deloAccess.size()) {
			DeloAccess access = deloAccess.get(i);

			if (Objects.equals(access.getExclude(), SysConstants.CODE_ZNACHENIE_DA)) {
				i++;
				continue; // достъпа е отнет и не се занимавам тука с него
			}

			if (Objects.equals(access.getFlag(), SysConstants.CODE_DEIN_IZTRIVANE)) {

				if (deleteQuery == null) {
					deleteQuery = createNativeQuery("delete from DELO_ACCESS where ID = ?1");
				}
				deleteQuery.setParameter(1, access.getId()).executeUpdate();
				hasChange = true;
				deloAccess.remove(i);

				Integer temp = accessMap.get(access.getCodeRef());
				if (temp != null) { // има го
					if (temp.intValue() == 1) { // ако го има като нов и иска да се трие остава непроменен
						accessMap.put(access.getCodeRef(), 0);
					}
				} else {
					accessMap.put(access.getCodeRef(), 2);
				}

			} else {
				deloAccessJournal.getPersons().add(access);
				if (access.getId() == null) { // само нов запис има тука без корекция !!!
					access.setDeloId(saved.getId());
					if (access.getExclude() == null) {
						access.setExclude(SysConstants.CODE_ZNACHENIE_NE);
					}
					getEntityManager().persist(access);
					hasChange = true;
					newAccessCodes.add(access.getCodeRef());
					
					Integer temp = accessMap.get(access.getCodeRef());
					if (temp != null) { // има го
						if (temp.intValue() == 2) { // ако го има като изтрит и сега влиза като нов значи остава без промяна
							accessMap.put(access.getCodeRef(), 0);
						}
					} else { // влиза като нов
						accessMap.put(access.getCodeRef(), 1);
					}

				} else {
					accessMap.put(access.getCodeRef(), 0); // тука винаги може да си се добави, защото няма да има промяна
				}
				i++;
			}
		}

		saved.setDeloAccess(deloAccess); // да се знае на екрана новото състояние
		if (hasChange) {
			SystemJournal journal = deloAccessJournal.toSystemJournal();
			journal.setIdUser(getUserId());
			saveAudit(journal);		
		}
		
		return newAccessCodes;
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> GetToms(Integer docId) throws DbErrorException {
		if (docId == null)  return null;
		try { 
			String sql = "SELECT DISTINCT delo_doc.TOM_NOMER tomNomer " + 
					"	FROM " + 
					"	delo_doc " + 
					"	WHERE delo_doc.DELO_ID =:deloId"   ;
			sql+= " union ";
			sql+= "SELECT DISTINCT delo_delo.TOM_NOMER tomNomer " + 
					"	FROM " + 
					"	delo_delo " + 
					"	WHERE delo_delo.DELO_ID =:deloId"   ;
								
			Query q = JPA.getUtil().getEntityManager().createNativeQuery(sql);
			
			q.setParameter("deloId", docId);
			
			return q.getResultList();		

		} catch (Exception e) {			
			LOGGER.error("Грешка при търсене на несъответствия",e);	
			throw new DbErrorException("Грешка при търсене на несъответствия");
		}	
	}

	/** Добавя информация за водещия за да се знае дали ще се променят правата или не. */
	private void addCodeRefLeadAccessMap(Delo delo, Map<Integer, Integer> accessMap) {
		if (!Objects.equals(delo.getCodeRefLead(), delo.getDbCodeRefLead())) { // има разлика

			if (delo.getCodeRefLead() != null) { // този е нов
				Integer temp = accessMap.get(delo.getCodeRefLead());
				if (temp != null) { // има го
					if (temp.intValue() == 2) { // ако го има като изтрит и сега влиза като нов значи остава без промяна
						accessMap.put(delo.getCodeRefLead(), 0);
					}
				} else { // трябва да си влезне като нов
					accessMap.put(delo.getCodeRefLead(), 1);
				}
			}

			if (delo.getDbCodeRefLead() != null) { // този е за триене
				Integer temp = accessMap.get(delo.getDbCodeRefLead());
				if (temp != null) { // има го
					if (temp.intValue() == 1) { // ако го има като нов и иска да се трие остава непроменен
						accessMap.put(delo.getDbCodeRefLead(), 0);
					}
				} else { // влиза си за триене
					accessMap.put(delo.getDbCodeRefLead(), 2);
				}
			}
		} else if (delo.getCodeRefLead() != null) { // няма промяна, но трябва да го сложа като стар
			accessMap.put(delo.getCodeRefLead(), 0);
		}
	}
	
	/** Определя какво в достъпа е променено и вика частично преизграждане */
	private void addRemoveDeloDost(Delo delo, int codeAction, Map<Integer, Integer> accessMap, SystemData sd, boolean registraturaChanged) throws DbErrorException {
		DocDostUtils du = new DocDostUtils();
		
		Set<Integer> newAccess;
		Set<Integer> delAccess;
		boolean isNew;
		if (codeAction == CODE_DEIN_ZAPIS) {
			newAccess = accessMap.keySet();
			delAccess = null;
			isNew = true;

			if (registraturaChanged) { // само за нов запис
				du.addDeloAccessUserReg(delo.getId(), delo.getUserReg(), CODE_ZNACHENIE_JOURNAL_DELO, delo.getId());
			}

		} else {
			newAccess = new HashSet<>();
			delAccess = new HashSet<>();
			isNew = false;
			
			for (Entry<Integer, Integer> entry : accessMap.entrySet()) {
				if (entry.getValue().intValue() == 1) {
					newAccess.add(entry.getKey());
					
				} else if (entry.getValue().intValue() == 2 || entry.getValue().intValue() == 3) {
					delAccess.add(entry.getKey());
				}
			}
		}

//		System.out.println("newAccess="+newAccess);
//		System.out.println("delAccess="+delAccess);

		// само частично преизграждане в зависимост от промените !!!
		du.addRemoveDeloDost(delo.getId(), isNew, CODE_ZNACHENIE_JOURNAL_DELO, delo.getId(), delAccess, newAccess, sd);
	}
	
	
	/**
	 * Дава информация за запълненост на томове
	 * [0]-TOM_NOMER<br>
	 * [1]-COUNT_SHEETS<br>
	 * 
	 * @param deloId
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Integer[]> selectTomSheetsCount(Integer deloId) throws DbErrorException {
		List<Integer[]> result = new ArrayList<>();
		
		try { // първо намирам директно вложените документи с колко страници са за всеки том
			StringBuilder sql = new StringBuilder();
			sql.append(" select S.TOM_NOMER, sum(s.DOC_SHEETS) DOC_SHEETS, sum(s.PRIL_SHEETS) PRIL_SHEETS from ( ");
			sql.append(" select ddoc.TOM_NOMER, d.COUNT_SHEETS DOC_SHEETS, sum(p.COUNT_SHEETS) PRIL_SHEETS, d.DOC_ID ");
			sql.append(" from DELO_DOC ddoc ");
			sql.append(" left outer join DOC d on d.DOC_ID = ddoc.INPUT_DOC_ID and d.COUNT_SHEETS is not null ");
			sql.append(" left outer join DOC_PRIL p on p.DOC_ID = ddoc.INPUT_DOC_ID and p.COUNT_SHEETS is not null ");
			sql.append(" where ddoc.DELO_ID = ?1 and ddoc.TOM_NOMER is not null and (d.DOC_ID is not null or p.ID is not null) ");
			sql.append(" group by ddoc.TOM_NOMER, d.DOC_ID, d.COUNT_SHEETS ");
			sql.append(" ) S group by S.TOM_NOMER ");
			sql.append(" order by 1 ");
			
			List<Object[]> rows = createNativeQuery(sql.toString()).setParameter(1, deloId).getResultList();
			for (Object[] row : rows) {
				int count = 0;
				if (row[1] != null) { // през док
					count += ((Number) row[1]).intValue();
				}
				if (row[2] != null) { // през приложения
					count += ((Number) row[2]).intValue();
				}
				result.add(new Integer[] {asInteger(row[0]), count});
			}
		
			// после всички вложени преписки по томове
			rows = createNativeQuery(
				"select distinct ddelo.TOM_NOMER, ddelo.INPUT_DELO_ID from DELO_DELO ddelo"
				+ " where ddelo.DELO_ID = ?1 and ddelo.TOM_NOMER is not null order by 1")
				.setParameter(1, deloId).getResultList();
			for (Object[] row : rows) {
				
				Integer tomNomer = asInteger(row[0]);

				Integer[] found = null;
				for (Integer[] temp : result) {
					if (Objects.equals(temp[0], tomNomer)) {
						found = temp;
						break;
					}
				}
				if (found == null) { // все още няма в този том нищо
					found = new Integer[] {tomNomer, 0};
					result.add(found);
				}

				Set<Integer> included = new HashSet<>();
				included.add(deloId); // тука ще се трупат за да не се зациклим

				selectTomSheetsCountRecursive(asInteger(row[1]), found, included);
			}
			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при определяне на запълненост на томове за дело.", e);
		}
		
		result.sort((Integer[] o1, Integer[] o2) -> o1[0].compareTo(o2[0]));
		return result;
	}

	/**
	 * @param asInteger
	 * @param found
	 * @param included
	 */
	@SuppressWarnings("unchecked")
	private void selectTomSheetsCountRecursive(Integer inputDeloId, Integer[] found, Set<Integer> included) {
		if (included.contains(inputDeloId)) {
			return; // за де зациклим
		}
		included.add(inputDeloId);
		
		// взимам страниците за подаденото дело пак по томове, но после всичко се брои заедно
		StringBuilder sql = new StringBuilder();
		sql.append(" select S.TOM_NOMER, sum(s.DOC_SHEETS) DOC_SHEETS, sum(s.PRIL_SHEETS) PRIL_SHEETS from ( ");
		sql.append(" select ddoc.TOM_NOMER, d.COUNT_SHEETS DOC_SHEETS, sum(p.COUNT_SHEETS) PRIL_SHEETS, d.DOC_ID ");
		sql.append(" from DELO_DOC ddoc ");
		sql.append(" left outer join DOC d on d.DOC_ID = ddoc.INPUT_DOC_ID and d.COUNT_SHEETS is not null ");
		sql.append(" left outer join DOC_PRIL p on p.DOC_ID = ddoc.INPUT_DOC_ID and p.COUNT_SHEETS is not null ");
		sql.append(" where ddoc.DELO_ID = ?1 and ddoc.TOM_NOMER is not null and (d.DOC_ID is not null or p.ID is not null) ");
		sql.append(" group by ddoc.TOM_NOMER, d.DOC_ID, d.COUNT_SHEETS ");
		sql.append(" ) S group by S.TOM_NOMER ");
		sql.append(" order by 1 ");
		
		List<Object[]> rows = createNativeQuery(sql.toString()).setParameter(1, inputDeloId).getResultList();
		for (Object[] row : rows) {
			int count = 0;
			if (row[1] != null) { // през док
				count += ((Number) row[1]).intValue();
			}
			if (row[2] != null) { // през приложения
				count += ((Number) row[2]).intValue();
			}
			found[1] = found[1] + count;
		}

		// вложените преписки пак по томове, но после пак се брои всичко заедно
		rows = createNativeQuery(
			"select distinct ddelo.TOM_NOMER, ddelo.INPUT_DELO_ID from DELO_DELO ddelo"
			+ " where ddelo.DELO_ID = ?1 and ddelo.TOM_NOMER is not null order by 1")
			.setParameter(1, inputDeloId).getResultList();
		for (Object[] row : rows) {
			selectTomSheetsCountRecursive(asInteger(row[1]), found, included);
		}
	}
	
	/**
	 * Запис само на основни данни на преписка, като се прави журналиране на системна обработка.
	 * 
	 * @param delo
	 * @param ident
	 * @return
	 * @throws DbErrorException
	 */
	public Delo saveSysOkaJournal(Delo delo, String ident) throws DbErrorException {
		delo.setAuditable(false); // ще се журналира по друг начин след записа
		delo = super.save(delo);

		SystemJournal journal = new SystemJournal(delo.getCodeMainObject(), delo.getId(), ident);

		journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
		journal.setDateAction(new Date());
		journal.setIdUser(getUserId());

		saveAudit(journal);

		return delo;
	}
}
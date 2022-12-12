package com.ib.omb.db.dao;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.indexui.system.Constants.CODE_CLASSIF_BUSINESS_ROLE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_PROC_ADMIN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ETAP_STAT_STOP;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_CONTROL_PROC;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_PROC_STOP;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_CONTR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_STAT_EXE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_STAT_IZP;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_STAT_IZP_SROK;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_STAT_STOP;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_STAT_WAIT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_STATUS_AUTO_PRIKL;
import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.jpa.QueryHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.omb.db.dto.ProcDef;
import com.ib.omb.db.dto.ProcDefEtap;
import com.ib.omb.db.dto.ProcExe;
import com.ib.omb.db.dto.ProcExeEtap;
import com.ib.omb.db.dto.Task;
import com.ib.omb.experimental.Notification;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.JAXBHelper;

/**
 * DAO for {@link ProcExe}
 *
 * @author belev
 */
public class ProcExeDAO extends AbstractDAO<ProcExe> {

	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcExeDAO.class);

	/** @param user */
	public ProcExeDAO(ActiveUser user) {
		super(ProcExe.class, user);
	}

	/**
	 * Определяне на отговорен служител
	 *
	 * @param exe
	 * @param newCodeRef
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	public ProcExe changeCodeRef(ProcExe exe, Integer newCodeRef, BaseSystemData systemData) throws DbErrorException {
		if (newCodeRef == null || Objects.equals(exe.getCodeRef(), newCodeRef)) {
			return exe; // няма какво да се парави
		}
		Integer oldCodeRef = exe.getCodeRef();

		boolean startFirstEtap = false;
		exe.setCodeRef(newCodeRef);
		if (exe.getStatus().equals(CODE_ZNACHENIE_PROC_STAT_WAIT)) {
			exe.setStatus(CODE_ZNACHENIE_PROC_STAT_EXE);
			exe.setComments(null);
			startFirstEtap = true;
		}
		exe = save(exe); // !!! основния запис !!!

		if (startFirstEtap) {
			List<ProcDefEtap> defEtapList = new ProcDefDAO(getUser()).selectDefEtapList(exe.getDefId(), 1); // само първия

			if (!defEtapList.isEmpty()) {
				new ProcExeEtapDAO(getUser()).startEtap(exe, 0, defEtapList.get(0), systemData, null, exe.getDocId());
			}
		}

//		Вие сте отговорен за изпълнението на процедура № <PROC_EXE.EXE_ID> <PROC_EXE.PROC_NAME>.
		Notification notif1 = new Notification(((UserData) getUser()).getUserAccess(), null //
			, CODE_ZNACHENIE_NOTIFF_EVENTS_CONTROL_PROC, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, (SystemData) systemData);
		notif1.setProcId(exe.getId());
		notif1.setComment("Вие сте отговорен за изпълнението на процедура №" + exe.getId() + " " + exe.getProcName() + ".");
		notif1.getAdresati().add(newCodeRef);
		notif1.send();

		if (oldCodeRef != null) {
//			Вече не сте отговорен за изпълнението на процедура № <PROC_EXE.EXE_ID> <PROC_EXE.PROC_NAME>.
			Notification notif2 = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_CONTROL_PROC, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, (SystemData) systemData);
			notif2.setProcId(exe.getId());
			notif2.setComment("Вече не сте отговорен за изпълнението на процедура №" + exe.getId() + " " + exe.getProcName() + ".");
			notif2.getAdresati().add(oldCodeRef);
			notif2.send();
		}

		return exe;
	}

	/**
	 * "Бутон <Прекратяване на процедурата> се вижда, когато <b>процедурата е в статус „чака изпълнение“ или „изпълнява се“</b>.
	 * Вижда се от администратор на процедури, от отговорния за изпълнението (ако е определен) и от <b>контролиращите на етапи в
	 * статус „изпълнява се“ или „чака решение“</b>.<br>
	 * Бутон <Възобновяване на процедурата> се вижда, когато <b>процедурата е в статус „прекратена“</b> и дефиницията на
	 * процедурата е „активна“. Вижда се от администратор на процедури, от отговорния за изпълнението и от <b>контролиращите на
	 * етапи в статус „прекратен“</b>."
	 *
	 * @param exeId
	 * @param exeStatus
	 * @return
	 * @throws DbErrorException
	 */
	public boolean checkEtapControlUser(Integer exeId, Integer exeStatus) throws DbErrorException {
		if (exeId == null || exeStatus == null) {
			return false;
		}

		List<Integer> etapStatus;
		if (exeStatus.equals(OmbConstants.CODE_ZNACHENIE_PROC_STAT_WAIT) || exeStatus.equals(OmbConstants.CODE_ZNACHENIE_PROC_STAT_EXE)) {
			etapStatus = Arrays.asList(OmbConstants.CODE_ZNACHENIE_ETAP_STAT_EXE, OmbConstants.CODE_ZNACHENIE_ETAP_STAT_DECISION);

		} else if (exeStatus.equals(OmbConstants.CODE_ZNACHENIE_PROC_STAT_STOP)) {
			etapStatus = Arrays.asList(OmbConstants.CODE_ZNACHENIE_ETAP_STAT_STOP);

		} else {
			return false;
		}

		Number count;
		try {
			count = (Number) createNativeQuery( //
				"select count (*) CNT from PROC_EXE_ETAP where EXE_ID = ?1 and CODE_REF = ?2 and STATUS in (?3)") //
					.setParameter(1, exeId).setParameter(2, ((UserData) getUser()).getUserAccess()) //
					.setParameter(3, etapStatus) //
					.getResultList().get(0);
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на етапи на процедура.", e);
		}
		return count != null && count.intValue() > 0;
	}

	/**
	 * Търсене на изпълнение етапи на процедура<br>
	 * [0]-EXE_ETAP_ID<br>
	 * [1]-NOMER<br>
	 * [2]-ETAP_NAME<br>
	 * [3]-ETAP_INFO<br>
	 * [4]-CONDITIONAL<br>
	 * [5]-PREV.EXE_ETAP_ID<br>
	 * [6]-PREV.NOMER<br>
	 * [7]-PREV.ETAP_NAME<br>
	 * [8]-BEGIN_DATE<br>
	 * [9]-SROK_DATE<br>
	 * [10]-END_DATE<br>
	 * [11]-STATUS<br>
	 * [12]-CODE_REF<br>
	 * [13]-COMMENTS<br>
	 * [14]-INSTRUCTIONS<br>
	 * [15]-DOC_ID<br>
	 * [16]-RN_DOC<br>
	 * [17]-DOC_DATE<br>
	 * [18]-IS_MERGE<br>
	 *
	 * @param exeId
	 * @param defId
	 * @return
	 */
	public SelectMetadata createSelectEtapExeList(Integer exeId, Integer defId) {
		String dialect = JPA.getUtil().getDbVendorName();

		StringBuilder select = new StringBuilder();
		select.append(" select ee.EXE_ETAP_ID a0, ee.NOMER a1, ee.ETAP_NAME a2, " //
			+ DialectConstructor.limitBigString(dialect, "ee.ETAP_INFO", 300) + " a3, ee.CONDITIONAL a4 ");
		select.append(" , prev.EXE_ETAP_ID a5, prev.NOMER a6, prev.ETAP_NAME a7 ");
		select.append(" , ee.BEGIN_DATE a8 , ee.SROK_DATE a9, ee.END_DATE a10 ");
		select.append(" , ee.STATUS a11, ee.CODE_REF a12, ee.COMMENTS a13, " //
			+ DialectConstructor.limitBigString(dialect, "ee.INSTRUCTIONS", 300) + " a14 ");
		select.append(" , d.DOC_ID a15, "+DocDAO.formRnDocSelect("d.", dialect)+" a16, d.DOC_DATE a17, de.IS_MERGE a18 ");

		StringBuilder from = new StringBuilder();
		from.append(" from PROC_EXE_ETAP ee ");
		from.append(" left outer join PROC_EXE_ETAP prev on prev.EXE_ETAP_ID = ee.EXE_ETAP_ID_PREV ");
		from.append(" left outer join DOC d on d.DOC_ID = ee.DOC_ID ");
		from.append(" inner join PROC_DEF_ETAP de on de.NOMER = ee.NOMER and de.DEF_ID = :defId ");

		String where = " where ee.EXE_ID = :exeId ";

		Map<String, Object> params = new HashMap<>();
		params.put("exeId", exeId);
		params.put("defId", defId);

		SelectMetadata sm = new SelectMetadata();

		sm.setSql(select.toString() + from + where);
		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSqlParameters(params);

		return sm;

	}

	/**
	 * Търсене на изпълнение на процедури<br>
	 * [0]-EXE_ID<br>
	 * [1]-DEF_ID<br>
	 * [2]-PROC_NAME<br>
	 * [3]-PROC_INFO<br>
	 * [4]-CODE_REF<br>
	 * [5]-BEGIN_DATE<br>
	 * [6]-SROK_DATE<br>
	 * [7]-END_DATE<br>
	 * [8]-STATUS<br>
	 * [9]-REGISTRATURA_ID<br>
	 * [10]-DOC_ID<br>
	 * [11]-RN_DOC<br>
	 * [12]-DOC_DATE<br>
	 * [13]-INSTRUCTIONS<br>
	 * [14]-COMMENTS<br>
	 *
	 * @param registraturaId
	 * @param exeId
	 * @param defId
	 * @param procName
	 * @param procInfo
	 * @param rnDoc
	 * @param statusList
	 * @param beginFrom
	 * @param beginTo
	 * @param srokFrom
	 * @param srokTo
	 * @param endFrom
	 * @param endTo
	 * @param procCodeRefList
	 * @param etapCodeRef
	 * @param overdue
	 * @param ud
	 * @return
	 */
	public SelectMetadata createSelectProcExeList( //
		Integer registraturaId, Integer exeId, Integer defId, String procName, String procInfo //
		, String rnDoc, List<Integer> statusList, Date beginFrom, Date beginTo, Date srokFrom, Date srokTo, Date endFrom, Date endTo //
		, List<Integer> procCodeRefList, Integer etapCodeRef, Boolean overdue, UserData ud) {

		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		String select = " select e.EXE_ID a0, e.DEF_ID a1, e.PROC_NAME a2, " //
			+ DialectConstructor.limitBigString(dialect, "e.PROC_INFO", 300) + " a3, e.CODE_REF a4, e.BEGIN_DATE a5" //
			+ ", e.SROK_DATE a6, e.END_DATE a7, e.STATUS a8, d.REGISTRATURA_ID a9" //
			+ ", doc.DOC_ID a10, "+DocDAO.formRnDocSelect("doc.", dialect)+" a11, doc.DOC_DATE a12, e.INSTRUCTIONS a13, e.COMMENTS a14" //
			+ ", z.USER_ID a15, z.LOCK_DATE a16";

		StringBuilder from = new StringBuilder(" from PROC_EXE e inner join PROC_DEF d on d.DEF_ID = e.DEF_ID ");
		from.append(" left outer join DOC doc on doc.DOC_ID = e.DOC_ID ");

		StringBuilder where = new StringBuilder(" where 1=1 ");

		if (registraturaId != null) {
			where.append(" and d.REGISTRATURA_ID = :registraturaId ");
			params.put("registraturaId", registraturaId);
		}
		if (exeId != null) {
			where.append(" and e.EXE_ID = :exeId ");
			params.put("exeId", exeId);
		}
		if (defId != null) {
			where.append(" and e.DEF_ID = :defId ");
			params.put("defId", defId);
		}
		if (statusList != null && !statusList.isEmpty()) {
			where.append(" and e.STATUS in (:statusList) ");
			params.put("statusList", statusList);
		}

		String t = trimToNULL_Upper(procName);
		if (t != null) {
			where.append(" and upper(e.PROC_NAME) like :procName ");
			params.put("procName", "%" + t.toUpperCase() + "%");
		}
		t = trimToNULL_Upper(procInfo);
		if (t != null) {
			where.append(" and upper(e.PROC_INFO) like :procInfo ");
			params.put("procInfo", "%" + t.toUpperCase() + "%");
		}
		t = trimToNULL_Upper(rnDoc);
		if (t != null) {
			where.append(" and upper(doc.RN_DOC) like :rnDoc ");
			params.put("rnDoc", "%" + t.toUpperCase() + "%");
		}

		if (beginFrom != null) {
			where.append(" and e.BEGIN_DATE >= :beginFrom ");
			params.put("beginFrom", DateUtils.startDate(beginFrom));
		}
		if (beginTo != null) {
			where.append(" and e.BEGIN_DATE <= :beginTo ");
			params.put("beginTo", DateUtils.endDate(beginTo));
		}
		if (srokFrom != null) {
			where.append(" and e.SROK_DATE >= :srokFrom ");
			params.put("srokFrom", DateUtils.startDate(srokFrom));
		}
		if (srokTo != null) {
			where.append(" and e.SROK_DATE <= :srokTo ");
			params.put("srokTo", DateUtils.endDate(srokTo));
		}
		if (endFrom != null) {
			where.append(" and e.END_DATE >= :endFrom ");
			params.put("endFrom", DateUtils.startDate(endFrom));
		}
		if (endTo != null) {
			where.append(" and e.END_DATE <= :endTo ");
			params.put("endTo", DateUtils.endDate(endTo));
		}

		if (procCodeRefList != null && !procCodeRefList.isEmpty()) {
			where.append(" and e.CODE_REF in (:procCodeRefList) ");
			params.put("procCodeRefList", procCodeRefList);
		}
		if (etapCodeRef != null) {
			where.append(" and EXISTS (select etap.EXE_ETAP_ID from PROC_EXE_ETAP etap where etap.EXE_ID = e.EXE_ID and etap.CODE_REF = :etapCodeRef) ");
			params.put("etapCodeRef", etapCodeRef);
		}

		if (Boolean.TRUE.equals(overdue)) { // само просрочени
			where.append(" and END_DATE is null and e.SROK_DATE is not null and e.SROK_DATE <= :overdueDate ");
			params.put("overdueDate", DateUtils.endDate(new Date()));
		}

		if (!ud.hasAccess(Constants.CODE_CLASSIF_BUSINESS_ROLE, OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_PROC_ADMIN)) {
			// ако не е админ на процедутите може да ги вижда само ако участва като отговорник или контролиращ етап

			where.append(" and ( ");
			where.append(" e.CODE_REF = :userAccess ");
			where.append(" or ");
			where.append(" EXISTS (select etap.EXE_ETAP_ID from PROC_EXE_ETAP etap where etap.EXE_ID = e.EXE_ID and etap.CODE_REF = :userAccess) ");
			where.append(" ) ");

			params.put("userAccess", ud.getUserAccess());
		}

		from.append(" left outer join LOCK_OBJECTS z on z.OBJECT_TIP = :zTip and z.OBJECT_ID = e.EXE_ID and z.USER_ID != :zUser ");
		params.put("zTip", OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE);
		params.put("zUser", ud.getUserId());

		SelectMetadata sm = new SelectMetadata();

		sm.setSql(select + from + where);
		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * Възобновяване на процедурата
	 *
	 * @param exe
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 */
	@SuppressWarnings("unchecked")
	public ProcExe restartProc(ProcExe exe, BaseSystemData systemData) throws DbErrorException, InvalidParameterException {
		if (!Objects.equals(exe.getStatus(), CODE_ZNACHENIE_PROC_STAT_STOP)) {
			throw new InvalidParameterException("Може да се възобнови само прекратена процедура.");
		}

		Integer defStatus = (Integer) systemData.getItemSpecific(OmbConstants.CODE_CLASSIF_PROCEDURI, exe.getDefId() //
			, getUserLang(), null, OmbClassifAdapter.PROCEDURI_INDEX_STATUS);
		if (!Objects.equals(defStatus, OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE)) {
			throw new InvalidParameterException("Може да се възобнови само процедура, чиято дефиниция е активна.");
		}

		ProcExe oldExe = null; // трябва от журнала да намеря старото състояние за да взема статуса и коментара
		try { // правя го със stream защото журнала е голям и не е ясно колко реда ще има за този обект

			Stream<String> history = createQuery("select s.objectXml from SystemJournal s" //
				+ " where s.codeObject = ?1 and s.idObject = ?2 and s.objectXml is not null order by id desc") //
					.setParameter(1, OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE).setParameter(2, exe.getId()) //
					.setHint(QueryHints.HINT_FETCH_SIZE, 3) //
					.getResultStream();
			Iterator<String> iter = history.iterator();
			while (iter.hasNext()) {
				String objectXml = iter.next();

				ProcExe temp = JAXBHelper.xmlToObject(ProcExe.class, objectXml);

				if (!Objects.equals(temp.getStatus(), CODE_ZNACHENIE_PROC_STAT_STOP)) { // трябва ми първият различен от
																						// прекратена
					oldExe = temp;
					break;
				}
			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на предишно състояние на процедура.", e);
		}
		if (oldExe == null) {
			throw new InvalidParameterException("Процедурата не може да бъде възобновена, защото липсват данни за предишно състояние.");
		}
		exe = save(oldExe); // процедурата е върната едно назад

		List<Integer> exeEtapIds; // прекратените етапи, на които ще се търси състояние
		List<ProcExeEtap> oldExeEtapList = new ArrayList<>(); // състоянието към коеро трябва да се върнем
		try {
			exeEtapIds = createQuery( // прекратените етапи
				"select distinct e.id from ProcExeEtap e where e.exeId = ?1 and e.status = ?2") //
					.setParameter(1, exe.getId()).setParameter(2, CODE_ZNACHENIE_ETAP_STAT_STOP).getResultList();
			if (exeEtapIds.isEmpty()) { // може и да няма, защото е спряна в самото начало
				return exe;
			}
			Set<Integer> set = new HashSet<>(exeEtapIds); // ще се махат като някой се намери

			Stream<String> history = createQuery("select s.objectXml from SystemJournal s" //
				+ " where s.codeObject = ?1 and s.idObject in (?2) and s.objectXml is not null order by id desc") //
					.setParameter(1, OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE_ETAP).setParameter(2, exeEtapIds) //
					.setHint(QueryHints.HINT_FETCH_SIZE, 5) //
					.getResultStream();
			Iterator<String> iter = history.iterator();
			while (iter.hasNext()) {
				String objectXml = iter.next();

				ProcExeEtap temp = JAXBHelper.xmlToObject(ProcExeEtap.class, objectXml);

				if (set.contains(temp.getId()) //
					&& !Objects.equals(temp.getStatus(), CODE_ZNACHENIE_ETAP_STAT_STOP)) { // трябва ми първият различен от
																							// прекратен
					oldExeEtapList.add(temp);
					set.remove(temp.getId());
				}
				if (set.isEmpty()) {
					break; // за всички е открито старото състояние
				}
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на предишно състояние на етапи на процедура.", e);
		}
		ProcExeEtapDAO exeEtapDao = new ProcExeEtapDAO(getUser());
		for (ProcExeEtap oldExeEtap : oldExeEtapList) {
			exeEtapDao.save(oldExeEtap); // всички етапи се връщат едно назад
		}

		// и накрая и на задачите по тези етапи
		List<Number> taskIds;
		try {
			taskIds = createNativeQuery("select t.TASK_ID from PROC_EXE_TASK e inner join TASK t on t.TASK_ID = e.TASK_ID" //
				+ " where e.EXE_ETAP_ID in (?1) and t.STATUS = ?2") //
					.setParameter(1, exeEtapIds).setParameter(2, CODE_ZNACHENIE_TASK_STATUS_AUTO_PRIKL) //
					.getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на предишно състояние на задачи от етапи на процедура.", e);
		}

		TaskDAO taskDao = new TaskDAO(getUser());
		for (Number taskId : taskIds) {
			Task task = taskDao.findById(taskId.intValue());

			task.setStatus(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP);
			task.setStatusComments("Задачата е поставена отново за изпълнение поради възобновяване изпълнението на процедура" //
				+ " №" + exe.getId() + " " + exe.getProcName() + ".");

			taskDao.save(task, null, (SystemData) systemData);
		}

		return exe;
	}

	/**
	 * Стартиране на изпълнение на процедура
	 *
	 * @param defId
	 * @param docId
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public ProcExe startProc(Integer defId, Integer docId, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		ProcDefDAO defDao = new ProcDefDAO(getUser());

		ProcDef def = defDao.findById(defId); // ПЪРВО ми трябва дефиницията за която иска стартиране
		if (def == null) {
			throw new ObjectInUseException("Не е намерена дефиниция на процедура с ИД=" + defId + "!");
		}
		if (!Objects.equals(def.getStatus(), CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE)) {
			throw new ObjectInUseException("Дефиниция на процедура с ИД=" + defId + " не е активна и не може да бъде стартирана!");
		}

		ProcExe exe = new ProcExe();

		exe.setDefId(defId);
		exe.setRegistraturaId(def.getRegistraturaId());
		exe.setDocId(docId);

		// прекопирвам данните в изпълнението
		exe.setProcName(def.getProcName());
		exe.setProcInfo(def.getProcInfo());
		exe.setSrokDays(def.getSrokDays());
		exe.setWorkDaysOnly(def.getWorkDaysOnly());
		exe.setInstructions(def.getInstructions());

		ProcExeEtapDAO exeEtapDao = new ProcExeEtapDAO(getUser());

		try { // ПОСЛЕ определям отговорника за изпълнението на процедурата
			Integer codeRef = exeEtapDao.findCodeEmployee(exe.getDocId(), def.getCodeRef(), def.getZveno(), def.getEmplPosition(), def.getBusinessRole(), def.getRegistraturaId(), systemData, true);

			exe.setStatus(CODE_ZNACHENIE_PROC_STAT_EXE);
			exe.setCodeRef(codeRef);

		} catch (InvalidParameterException e) {
			LOGGER.debug("Очаква се определяне на отговорен за изпълнението на процедурата: {}", e.getMessage());
			exe.setStatus(CODE_ZNACHENIE_PROC_STAT_WAIT);
			exe.setComments("Очаква се определяне на отговорен за изпълнението на процедурата.");
		}

		// НАКРАЯ разните дати и срокове
		exe.setBeginDate(new Date());
		exe.setSrokDate(exeEtapDao.calcSrokDate(exe.getBeginDate(), exe.getSrokDays(), null, exe.getWorkDaysOnly(), systemData));

		exe = save(exe); // !!! основния запис !!!

		if (exe.getCodeRef() != null) { // етапите се стартират също само ако има отговорник

			List<ProcDefEtap> defEtapList = defDao.selectDefEtapList(def.getId(), 1); // само първия

			if (!defEtapList.isEmpty()) {
				exeEtapDao.startEtap(exe, 0, defEtapList.get(0), systemData, null, exe.getDocId());
			}

//			Вие сте отговорен за изпълнението на процедура № <PROC_EXE.EXE_ID> <PROC_EXE.PROC_NAME>.
			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_CONTROL_PROC, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, (SystemData) systemData);
			notif.setProcId(exe.getId());
			notif.setComment("Вие сте отговорен за изпълнението на процедура №" + exe.getId() + " " + exe.getProcName() + ".");
			notif.getAdresati().add(exe.getCodeRef());
			notif.send();

		} else {

			// Изпраща се нотификация до потребител с бизнес роля "Администратор на процедури". Вид събитие за нотификация:
			// "За определяне на отговорен за изпълнение на процедура" с текст: „Необходимо е да се определи служител, отговорен
			// за изпълнение на процедура № <PROC_EXE.EXE_ID> <PROC_EXE.PROC_NAME>.“

			@SuppressWarnings("unchecked")
			List<Object> rows = createNativeQuery("select USER_ID from ADM_USER_ROLES where CODE_CLASSIF = ?1 and CODE_ROLE = ?2") //
				.setParameter(1, CODE_CLASSIF_BUSINESS_ROLE).setParameter(2, CODE_ZNACHENIE_BUSINESS_ROLE_PROC_ADMIN) //
				.getResultList();

			if (!rows.isEmpty()) { // ако няма никой до кой да се праща нотификация

				Integer adminProc = null;
				if (rows.size() > 1) { // трябва да се отркие този който е за текущата регистратура
					for (Object row : rows) {
						int tmpCode = ((Number) row).intValue();

						Integer registratura = (Integer) systemData.getItemSpecific(CODE_CLASSIF_ADMIN_STR, tmpCode, getUserLang(), null //
							, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);

						if (exe.getRegistraturaId().equals(registratura)) {
							adminProc = tmpCode;
							break;
						}
					}
				}

				if (adminProc == null) {
					adminProc = ((Number) rows.get(0)).intValue();
				}

				Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
					, CODE_ZNACHENIE_NOTIFF_EVENTS_CONTROL_PROC, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, (SystemData) systemData);
				notif.setProcId(exe.getId());
				notif.setComment("Необходимо е да се определи служител, отговорен за изпълнение на процедура №" //
					+ exe.getId() + " " + exe.getProcName() + ".");
				notif.getAdresati().add(adminProc);
				notif.send();
			}
		}
		return exe;
	}

	/**
	 * Прекратяване на процедурата
	 *
	 * @param exe
	 * @param stopReason
	 * @param bySystem
	 * @param notifToCodeRef
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	public ProcExe stopProc(ProcExe exe, String stopReason, boolean bySystem, Integer notifToCodeRef, BaseSystemData systemData) throws DbErrorException {
		exe.setStatus(CODE_ZNACHENIE_PROC_STAT_STOP);
		exe.setStopReason(stopReason);

		Date now = new Date();
		exe.setEndDate(now);

		StringBuilder comments = new StringBuilder();
		if (bySystem) {
			comments.append("Процедурата е прекратена от системата на " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(now) + ".");

		} else {
			SystemClassif empl = systemData.decodeItemLite(CODE_CLASSIF_ADMIN_STR, getUserId(), getUserLang(), now, false);
			if (empl != null) {
				comments.append("Процедурата е прекратена от " + empl.getTekst());
				comments.append(" от " + systemData.decodeItem(CODE_CLASSIF_ADMIN_STR, empl.getCodeParent(), getUserLang(), now));
				comments.append(" на " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(now) + ".");
			}
		}
		exe.setComments(comments.toString());

		ProcExe saved = save(exe);

		@SuppressWarnings("unchecked")
		List<ProcExeEtap> exeEtapList = createQuery("select e from ProcExeEtap e where e.exeId = ?1 and e.status in (?2)") //
			.setParameter(1, exe.getId()).setParameter(2, ProcExeEtapDAO.EXE_ETAP_ACTIVE_STATUS_LIST) //
			.getResultList();

		Set<Integer> notifTo = new HashSet<>(); // контролиращи етапи(които се прекратяват) и отговорник на процедурата

		if (!exeEtapList.isEmpty()) {
			String etapComments = "Етапът е прекратен поради прекратяване изпълнението на процедурата.";
			String taskComments = "Задачата е приключена автоматично поради прекратяване изпълнението на процедура" //
				+ " №" + exe.getId() + " " + exe.getProcName() + ".";

			ProcExeEtapDAO exeEtapDao = new ProcExeEtapDAO(getUser());
			for (ProcExeEtap exeEtap : exeEtapList) {
				exeEtapDao.cancelEtap(CODE_ZNACHENIE_ETAP_STAT_STOP, exeEtap, etapComments, taskComments, systemData);

				if (exeEtap.getCodeRef() != null) {
					notifTo.add(exeEtap.getCodeRef());
				}
			}
		}

		if (notifToCodeRef != null) {
			notifTo.add(notifToCodeRef);
		}
		if (exe.getCodeRef() != null) {
			notifTo.add(exe.getCodeRef());
		}
		if (!notifTo.isEmpty()) { // нотификация за прекратяването на процедурата
			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_PROC_STOP, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, (SystemData) systemData);
			notif.setProcId(exe.getId());
			notif.setComment("Прекратено е изпълнението на процедура №" + exe.getId() + " " + exe.getProcName() + ".");
			notif.getAdresati().addAll(notifTo);
			notif.send();
		}

		return saved;
	}

	/**
	 * Приклщчване на процедура
	 *
	 * @param exe
	 * @throws DbErrorException
	 */
	void completeProc(ProcExe exe) throws DbErrorException {
		Number cnt = (Number) createNativeQuery("select count(*) cnt from PROC_EXE_ETAP where EXE_ID = ?1 and STATUS in (?2)") //
			.setParameter(1, exe.getId()).setParameter(2, ProcExeEtapDAO.EXE_ETAP_ACTIVE_STATUS_LIST) //
			.getResultList().get(0);

		if (cnt.intValue() > 0) {
			return; // има етапи по които се работи в тази процедура и нищо не се прави
		}

		// явно е готова

		exe.setEndDate(new Date());

		if (exe.getSrokDate() != null && exe.getSrokDate().getTime() < exe.getEndDate().getTime()) {
			exe.setStatus(CODE_ZNACHENIE_PROC_STAT_IZP_SROK);
		} else {
			exe.setStatus(CODE_ZNACHENIE_PROC_STAT_IZP);
		}
		save(exe);
	}
}

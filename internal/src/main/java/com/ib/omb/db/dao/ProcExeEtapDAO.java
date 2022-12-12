package com.ib.omb.db.dao;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.indexui.system.Constants.CODE_CLASSIF_BUSINESS_ROLE;
import static com.ib.indexui.system.Constants.CODE_CLASSIF_POSITION;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_BOSS_POSITIONS;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_PROC_BUSINESS_ROLE;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_TASK_ONE_EMPL;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_TASK_STATUS_ACTIVE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ETAP_DOC_MODE_PREV_IN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ETAP_DOC_MODE_PREV_OUT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ETAP_STAT_CANCEL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ETAP_STAT_DECISION;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ETAP_STAT_EXE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ETAP_STAT_IZP;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ETAP_STAT_IZP_SROK;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ETAP_STAT_STOP;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ETAP_STAT_WAIT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_CONTROL_ETAP;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DECISION_PROC;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_PROC;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_CONTR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_BUSINESS_ROLE_AUTHOR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_BUSINESS_ROLE_AUTHOR_BOSS;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_BUSINESS_ROLE_AUTHOR_KOLEGA;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_STAT_EXE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_STATUS_AUTO_PRIKL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_DA;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_NE;
import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.system.utils.SearchUtils.trimToNULL;
import static java.util.Calendar.DAY_OF_WEEK;
import static java.util.Calendar.DAY_OF_YEAR;
import static java.util.Calendar.SATURDAY;
import static java.util.Calendar.SUNDAY;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dto.ProcDefEtap;
import com.ib.omb.db.dto.ProcDefTask;
import com.ib.omb.db.dto.ProcDefTaskIzp;
import com.ib.omb.db.dto.ProcExe;
import com.ib.omb.db.dto.ProcExeEtap;
import com.ib.omb.db.dto.ProcExeTask;
import com.ib.omb.db.dto.Task;
import com.ib.omb.experimental.Notification;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.utils.DateUtils;

/**
 * DAO for {@link ProcExeEtap}
 *
 * @author belev
 */
public class ProcExeEtapDAO extends AbstractDAO<ProcExeEtap> {

	/** DAO for {@link ProcExeTask} */
	static class ProcExeTaskDAO extends AbstractDAO<ProcExeTask> {

		/** @param user */
		ProcExeTaskDAO(ActiveUser user) {
			super(ProcExeTask.class, user);
		}
	}

	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcExeEtapDAO.class);

	/**  */
	protected static final List<Integer> EXE_ETAP_ACTIVE_STATUS_LIST = Arrays.asList( //
		CODE_ZNACHENIE_ETAP_STAT_WAIT//
		, CODE_ZNACHENIE_ETAP_STAT_EXE//
		, CODE_ZNACHENIE_ETAP_STAT_DECISION);

	/** @param user */
	public ProcExeEtapDAO(ActiveUser user) {
		super(ProcExeEtap.class, user);
	}

	/**
	 * Активира повторно изпълнението на условен етап, за който тряба да се вземе решение
	 *
	 * @param exeEtap
	 * @param comments
	 * @return
	 * @throws DbErrorException
	 */
	public ProcExeEtap activateDecisionEtap(ProcExeEtap exeEtap, String comments) throws DbErrorException {
		if (exeEtap == null || !exeEtap.getStatus().equals(CODE_ZNACHENIE_ETAP_STAT_DECISION)) {
			return exeEtap; // само в изброените статуси може да повтори
		}

		exeEtap.setStatus(CODE_ZNACHENIE_ETAP_STAT_EXE);
		exeEtap.setComments(comments);

		exeEtap = save(exeEtap);

		return exeEtap;
	}

	/**
	 * @param begin
	 * @param srokDays
	 * @param srokHours
	 * @param workDaysOnly
	 * @param systemData
	 * @return
	 */
	public Date calcSrokDate(Date begin, Integer srokDays, Integer srokHours, Integer workDaysOnly, BaseSystemData systemData) {
		if (Objects.equals(workDaysOnly, CODE_ZNACHENIE_DA)) {
			if (srokDays == null) {
				return null; // гледат се само дните, така че ако ги няма - няма дата
			}
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(begin);

			gc.set(Calendar.HOUR_OF_DAY, 0);
			gc.set(Calendar.MINUTE, 0);
			gc.set(Calendar.SECOND, 0);
			gc.set(Calendar.MILLISECOND, 0);

			Set<Long> praznici = ((SystemData) systemData).getPraznici();
			int i = 0;
			while (i < srokDays) {
				gc.set(DAY_OF_YEAR, gc.get(DAY_OF_YEAR) + 1);

				if (gc.get(DAY_OF_WEEK) == SATURDAY || gc.get(DAY_OF_WEEK) == SUNDAY) {
					// пропускам
				} else if (praznici != null && praznici.contains(gc.getTimeInMillis())) {
					// пропускам
				} else {
					i++;
				}
			}

			gc.set(Calendar.HOUR_OF_DAY, 23);
			gc.set(Calendar.MINUTE, 59);
			gc.set(Calendar.SECOND, 59);

			return gc.getTime();
		}

		// периода е непрекъснат и се добавят директно дни и часове към началната дата
		long srokMillis = 0;
		if (srokDays != null) {
			srokMillis += srokDays * 24 * 60 * 60 * 1000;
		}
		if (srokHours != null) {
			srokMillis += srokHours * 60 * 60 * 1000;
		}
		return srokMillis > 0 ? new Date(begin.getTime() + srokMillis) : null;
	}

	/**
	 * Определяне на контролиращ
	 *
	 * @param exe
	 * @param exeEtap
	 * @param newCodeRef
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	public ProcExeEtap changeCodeRef(ProcExe exe, ProcExeEtap exeEtap, Integer newCodeRef, BaseSystemData systemData) throws DbErrorException {
		if (newCodeRef == null || Objects.equals(exeEtap.getCodeRef(), newCodeRef)) {
			return exeEtap; // няма какво да се парави
		}

		boolean regTasks = false;
		exeEtap.setCodeRef(newCodeRef);
		if (exeEtap.getStatus().equals(CODE_ZNACHENIE_ETAP_STAT_WAIT)) {
			exeEtap.setStatus(CODE_ZNACHENIE_ETAP_STAT_EXE);
			exeEtap.setComments(null);
			regTasks = true;
		}
		exeEtap = save(exeEtap); // !!! основния запис !!!

		if (regTasks) {
			List<ProcDefTask> defTaskList = new ProcDefEtapDAO(getUser()).selectDefTaskList(exe.getDefId(), exeEtap.getNomer());
			registerEtapTasks(exe, exeEtap, defTaskList, systemData);
		}
		return exeEtap;
	}

	/**
	 * Приклщчване на етап от процедура
	 *
	 * @param sd
	 * @param exeEtap
	 * @param ok           true-NEXT_OK, false-NEXT_NOT
	 * @param optional     по каквото дойде
	 * @param comments
	 * @param taskEndDocId
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public ProcExeEtap completeEtap(ProcExeEtap exeEtap, SystemData sd, Boolean ok, String optional, String comments, Integer taskEndDocId) throws DbErrorException {
		if (exeEtap == null //
			|| !(exeEtap.getStatus().equals(CODE_ZNACHENIE_ETAP_STAT_EXE) || exeEtap.getStatus().equals(CODE_ZNACHENIE_ETAP_STAT_DECISION))) {
			return exeEtap; // само в изброените статуси може да се приключи
		}

		exeEtap.setEndDate(new Date());
		exeEtap.setComments(comments);
		if (exeEtap.getSrokDate() != null && exeEtap.getSrokDate().getTime() < exeEtap.getEndDate().getTime()) {
			exeEtap.setStatus(CODE_ZNACHENIE_ETAP_STAT_IZP_SROK);
		} else {
			exeEtap.setStatus(CODE_ZNACHENIE_ETAP_STAT_IZP);
		}
		exeEtap = save(exeEtap); // този е готов

		// и трябва да се види какво се случва с изпълнението на процедурата и следващи етапи ако има

		ProcExeDAO exeDao = new ProcExeDAO(getUser());
		ProcExe exe = exeDao.findById(exeEtap.getExeId()); // изпълнението

		String next;
		if (ok != null) { // по да или по не
			List<Object> nextList = createNativeQuery("select " + (ok.booleanValue() ? "NEXT_OK" : "NEXT_NOT") //
				+ " from PROC_DEF_ETAP where DEF_ID = ?1 and NOMER = ?2") //
					.setParameter(1, exe.getDefId()).setParameter(2, exeEtap.getNomer()) //
					.getResultList();

			next = nextList.isEmpty() ? null : trimToNULL((String) nextList.get(0));

		} else { // или кой каквото му е на сърце
			next = trimToNULL(optional);
		}

		if (next == null) { // няма следващи етапи и процедурата би могла да е готова
			exeDao.completeProc(exe);

		} else {
			List<ProcDefEtap> defEtapList = createQuery( //
				"select e from ProcDefEtap e where e.defId = ?1 and e.nomer in (" + next + ")") //
					.setParameter(1, exe.getDefId()).getResultList();

			String branchPath = trimToNULL(exeEtap.getBranchPath()); // за да знам дали текущия е следствие на разклонение

			for (ProcDefEtap defEtap : defEtapList) {
				boolean start = true; // има ситуации, в които не трябва да се стартира следващият
				String nextBranchPath = null;

				if (branchPath != null // текущия е следствие на разклонение
					&& Objects.equals(defEtap.getIsMerge(), CODE_ZNACHENIE_DA)) { // което се влива в събирателен етап

					Number cnt = (Number) createNativeQuery( //
						"select count(*) cnt from PROC_EXE_ETAP where EXE_ID = ?1 and STATUS in (?2) and BRANCH_PATH like ?3 ") //
							.setParameter(1, exe.getId()).setParameter(2, EXE_ETAP_ACTIVE_STATUS_LIST) //
							.setParameter(3, "%" + branchPath + "%") //
							.getResultList().get(0);

					if (cnt.intValue() > 0) {
						start = false; // не може да се стартира следващ, защото по разклонението има все още незавършени етапи

					} else { // ще се стартира и тъй като е събирателен трябва да му мина една стъпка назад справмо текущия който
								// приключва
						nextBranchPath = branchPath.substring(0, branchPath.lastIndexOf('|'));
						nextBranchPath = nextBranchPath.substring(0, nextBranchPath.lastIndexOf('|'));
					}
				} else { // следващия ще се стартира безусловно

					if (defEtapList.size() > 1) { // текущият който приключва се явява разклонение на няколко други

						if (branchPath == null) { // първо разклонение
							nextBranchPath = "|" + exeEtap.getNomer() + "|";
						} else { // пореното
							nextBranchPath = branchPath + "|" + exeEtap.getNomer() + "|";
						}
					} else {
						nextBranchPath = branchPath; // само си се предава каквато е пътеката на разклоненията
					}
				}

				if (start) {
					Number cnt = (Number) createNativeQuery( //
						"select count (*) CNT from PROC_EXE_ETAP where EXE_ID = ?1 and NOMER = ?2 and STATUS in (?3)") //
							.setParameter(1, exeEtap.getId()).setParameter(2, defEtap.getNomer()) //
							.setParameter(3, EXE_ETAP_ACTIVE_STATUS_LIST) //
							.getResultList().get(0);

					if (cnt.intValue() == 0) { // винаги трябва да се провери дали няма активен етап с този номер, защото тогава
												// не се стартира за да се избегне дублирането
						Integer docId;

						if (defEtap.getEtapDocMode() == null) {
							docId = exe.getDocId(); // ако няма изискване го взимам от процедурата каквото има там

						} else if (defEtap.getEtapDocMode().equals(CODE_ZNACHENIE_ETAP_DOC_MODE_PREV_IN)) {
							docId = exeEtap.getDocId();

						} else if (defEtap.getEtapDocMode().equals(CODE_ZNACHENIE_ETAP_DOC_MODE_PREV_OUT)) {
							docId = taskEndDocId != null ? taskEndDocId : exeEtap.getDocId(); // ако е празно взимам от стартиращ
																								// предходния етап
						} else {
							docId = null;
						}
						startEtap(exe, exeEtap.getId(), defEtap, sd, nextBranchPath, docId);
					}
				}
			}
		}

		return exeEtap;
	}

	/**
	 * Свързва задачата с етапа
	 *
	 * @param taskId
	 * @param exeEtapId
	 * @param procInfo
	 * @throws DbErrorException
	 */
	public void connectTaskToEtap(Integer taskId, Integer exeEtapId, String procInfo) throws DbErrorException {
		ProcExeTaskDAO exeTaskDao = new ProcExeTaskDAO(getUser());

		ProcExeTask exeTask = new ProcExeTask();
		exeTask.setExeEtapId(exeEtapId);
		exeTask.setTaskId(taskId);
		exeTaskDao.save(exeTask);

		// за да се сетне в задачата текста от процедурата и етапа
		createNativeQuery("update TASK set PROC_INFO = ? where TASK_ID = ?") //
			.setParameter(1, procInfo).setParameter(2, taskId).executeUpdate();
	}

	/**
	 * @param id
	 * @param mainData при <code>true</code> се зареждат данни, който ще се покажат на Секция „Основни данни“
	 * @return
	 * @throws DbErrorException
	 */
	public ProcExeEtap findById(Object id, boolean mainData) throws DbErrorException {
		ProcExeEtap entity = super.findById(id);
		if (entity == null) {
			return entity;
		}

		if (mainData && entity.getExeEtapIdPrev() != null && entity.getExeEtapIdPrev().intValue() != 0) {

			@SuppressWarnings("unchecked")
			List<Object[]> prevList = createNativeQuery( //
				"select EXE_ETAP_ID, NOMER, ETAP_NAME from PROC_EXE_ETAP where EXE_ETAP_ID = ?1") //
					.setParameter(1, entity.getExeEtapIdPrev()).getResultList();
			if (!prevList.isEmpty()) {
				entity.setPrev(prevList.get(0));
			}
		}
		if (mainData) {
			@SuppressWarnings("unchecked")
			List<Object[]> defList = createNativeQuery( //
				"select de.DEF_ETAP_ID, de.IS_MERGE from PROC_DEF_ETAP de inner join PROC_EXE e on e.DEF_ID = de.DEF_ID where de.NOMER = ?1 and e.EXE_ID = ?2") //
					.setParameter(1, entity.getNomer()).setParameter(2, entity.getExeId()) //
					.getResultList();
			if (!defList.isEmpty()) {
				entity.setDefEtapData(defList.get(0));
			}
		}
		return entity;
	}

	/**
	 * В зависисмост от параметрите зададени в дефиницията, намира служител, който да стане участник в процедурата. Ако няма дава
	 * InvalidParameterException с текст за причината.
	 *
	 * @param docId
	 * @param codeRef
	 * @param zveno
	 * @param emplPosition
	 * @param role
	 * @param registratura
	 * @param sd
	 * @param onlyUser
	 * @return
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 */
	public Integer findCodeEmployee(Integer docId, Integer codeRef, Integer zveno, Integer emplPosition, Integer role, Integer registratura, BaseSystemData sd, boolean onlyUser)
		throws DbErrorException, InvalidParameterException {
		Date date = new Date();

// 		 	само служител
//			звено и длъжност
//			звено и бизнес роля
//			само бизнес роля

		if (codeRef != null) { // САМО СЛУЖИТЕЛ

			if (onlyUser) { // но трябва да бъде задължително потребител
				if (sd.matchClassifItems(SysConstants.CODE_CLASSIF_USERS, codeRef, date)) {
					return codeRef;
				}
				throw new InvalidParameterException("Потребител с код=" + codeRef + " не е регистриран в системата!");
			}

			if (sd.matchClassifItems(CODE_CLASSIF_ADMIN_STR, codeRef, date)) {
				return codeRef;
			}
			throw new InvalidParameterException("Служител с код=" + codeRef + " не съществува в административната структура!");
		}

		if (role != null) { // това се прави защото ролята в процедурата може да има съответсвие
			SystemClassif item = sd.decodeItemLite(CODE_CLASSIF_PROC_BUSINESS_ROLE, role, CODE_DEFAULT_LANG, date, false);
			if (item == null) {
				throw new InvalidParameterException("Не е намерена стойност за бизнес роля в процедура с код=" + role);
			}

			String codeExt = trimToNULL(item.getCodeExt());
			if (codeExt == null) { // специфичен алгоритъм
				return findCodeEmployeeAlg(docId, role, date, sd);
			}
			role = Integer.valueOf(codeExt); // има съответсвие с класификацията бизнес роля и си продължава по стария начин
		}

		if (zveno != null) {
			List<SystemClassif> items = sd.getChildrenOnNextLevel(CODE_CLASSIF_ADMIN_STR, zveno, date, CODE_DEFAULT_LANG); // всички
																															// в
																															// звеното
			if (emplPosition != null) { // ЗВЕНО И ДЛЪЖНОСТ

				List<Integer> found = new ArrayList<>();
				for (SystemClassif item : items) {
					Integer refType = (Integer) sd.getItemSpecific(CODE_CLASSIF_ADMIN_STR, item.getCode(), CODE_DEFAULT_LANG, date, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);
					if (refType == null || refType.intValue() != CODE_ZNACHENIE_REF_TYPE_EMPL //
						|| (onlyUser && !sd.matchClassifItems(SysConstants.CODE_CLASSIF_USERS, item.getCode(), date))) {
						continue; // само служители ми трябват или се иска потребител, но не е
					}
					Integer itemPosition = (Integer) sd.getItemSpecific(CODE_CLASSIF_ADMIN_STR, item.getCode(), CODE_DEFAULT_LANG, date, OmbClassifAdapter.ADM_STRUCT_INDEX_POSITION);
					if (emplPosition.equals(itemPosition)) {
						found.add(item.getCode());
					}
				}
				if (found.isEmpty()) {
					throw new InvalidParameterException("Служител със звено '" //
						+ sd.decodeItem(CODE_CLASSIF_ADMIN_STR, zveno, CODE_DEFAULT_LANG, date) //
						+ "' и длъжност '" //
						+ sd.decodeItem(CODE_CLASSIF_POSITION, emplPosition, CODE_DEFAULT_LANG, date) //
						+ "' не съществува в административната структура!");
				}
				return found.size() == 1 ? found.get(0) : found.get(ThreadLocalRandom.current().nextInt(found.size()));
			}

			if (role != null) { // ЗВЕНО И БИЗНЕС РОЛЯ

				List<Integer> emplCodes = new ArrayList<>();
				for (SystemClassif item : items) {
					Integer refType = (Integer) sd.getItemSpecific(CODE_CLASSIF_ADMIN_STR, item.getCode(), CODE_DEFAULT_LANG, date, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);
					if (refType == null || refType.intValue() != CODE_ZNACHENIE_REF_TYPE_EMPL) {
						continue; // звената се пропускат
					}
					emplCodes.add(item.getCode());
				}
				if (emplCodes.isEmpty()) {
					throw new InvalidParameterException("Служител със звено '" //
						+ sd.decodeItem(CODE_CLASSIF_ADMIN_STR, zveno, CODE_DEFAULT_LANG, date) //
						+ "' и бизнес роля '" //
						+ sd.decodeItem(CODE_CLASSIF_BUSINESS_ROLE, role, CODE_DEFAULT_LANG, date) //
						+ "' не съществува в административната структура!");
				}

				// ако има трябва да се пусне селект да се види кои са в тази бизнес роля
				@SuppressWarnings("unchecked")
				List<Object> found = createNativeQuery( //
					"select USER_ID from ADM_USER_ROLES where USER_ID in (?1) and CODE_CLASSIF = ?2 and CODE_ROLE = ?3") //
						.setParameter(1, emplCodes).setParameter(2, CODE_CLASSIF_BUSINESS_ROLE).setParameter(3, role) //
						.getResultList();
				if (found.isEmpty()) {
					throw new InvalidParameterException("Служител със звено '" //
						+ sd.decodeItem(CODE_CLASSIF_ADMIN_STR, zveno, CODE_DEFAULT_LANG, date) //
						+ "' и бизнес роля '" //
						+ sd.decodeItem(CODE_CLASSIF_BUSINESS_ROLE, role, CODE_DEFAULT_LANG, date) //
						+ "' не съществува в административната структура!");
				}
				Object result = found.size() == 1 ? found.get(0) : found.get(ThreadLocalRandom.current().nextInt(found.size()));
				return ((Number) result).intValue();
			}
		}

		if (role != null) { // САМО БИЗНЕС РОЛЯ

			@SuppressWarnings("unchecked")
			List<Object> emplCodes = createNativeQuery( //
				"select USER_ID from ADM_USER_ROLES where CODE_CLASSIF = ?1 and CODE_ROLE = ?2") //
					.setParameter(1, CODE_CLASSIF_BUSINESS_ROLE).setParameter(2, role) //
					.getResultList();
			if (emplCodes.isEmpty()) {
				throw new InvalidParameterException("Служител с бизнес роля '" //
					+ sd.decodeItem(CODE_CLASSIF_BUSINESS_ROLE, role, CODE_DEFAULT_LANG, date) //
					+ "' не съществува в административната структура!");
			}
			if (emplCodes.size() == 1) { // ако е само един регистратурата няма какво да се гледа
				return ((Number) emplCodes.get(0)).intValue();
			}

			List<Integer> found = new ArrayList<>(); // ако са повече от един трябва да се гледа регистратура
			for (Object emplCode : emplCodes) {
				Integer code = ((Number) emplCode).intValue();
				Integer codeRegistratura = (Integer) sd.getItemSpecific(CODE_CLASSIF_ADMIN_STR, code, CODE_DEFAULT_LANG, date, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
				if (codeRegistratura != null && codeRegistratura.equals(registratura)) {
					found.add(code);
				}
			}

			if (found.isEmpty()) { // няма нито един в търсената реистратура и давам без значения в коя е
				return ((Number) emplCodes.get(0)).intValue();
			}
			return found.size() == 1 ? found.get(0) : found.get(ThreadLocalRandom.current().nextInt(found.size()));
		}

		throw new InvalidParameterException("Липсват данни за да бъде определен служител!");
	}

	/**
	 * Бутон <Стартирай етапа>
	 *
	 * @param exe
	 * @param exeEtap
	 * @param defTaskList
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	public ProcExeEtap restartEtap(ProcExe exe, ProcExeEtap exeEtap, List<ProcDefTask> defTaskList, BaseSystemData systemData) throws DbErrorException {
		if (exeEtap == null || !exeEtap.getStatus().equals(CODE_ZNACHENIE_ETAP_STAT_WAIT) || exeEtap.getCodeRef() == null) {
			return exeEtap; // само след горните правила се продължава със задачите
		}

		exeEtap.setStatus(CODE_ZNACHENIE_ETAP_STAT_EXE);
		exeEtap.setComments(null);

		exeEtap = save(exeEtap); // !!! основния запис !!!

		registerEtapTasks(exe, exeEtap, defTaskList, systemData);

		return exeEtap;
	}

	/**
	 * Връщане на изпълнението
	 *
	 * @param exe
	 * @param selected
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws InvalidParameterException
	 */
	@SuppressWarnings("unchecked")
	public ProcExeEtap returnEtap(ProcExe exe, ProcExeEtap selected, BaseSystemData systemData) throws DbErrorException, InvalidParameterException {
		if (exe == null || !exe.getStatus().equals(CODE_ZNACHENIE_PROC_STAT_EXE)) {
			return selected; // само в изброените статуси може да се върнем
		}
		if (selected == null //
			|| !(selected.getStatus().equals(CODE_ZNACHENIE_ETAP_STAT_IZP) || selected.getStatus().equals(CODE_ZNACHENIE_ETAP_STAT_IZP_SROK))) {
			return selected; // само в изброените статуси може да се върнем
		}

		Set<Integer> nextIdSet = new HashSet<>(); // трябва да се намерят всички етапи след текущия и са следвтвие неговото
													// изпълнение
		recursiveFindNextExeEtapIdList(selected.getId(), nextIdSet);

		Integer exeEtapIdPrev = 0; // този ще дойде предходен за новото изпълнение

		List<ProcExeEtap> nextActiveList = null;
		if (!nextIdSet.isEmpty()) {
			exeEtapIdPrev = Collections.max(nextIdSet); // ще прдължи след посленият

			nextActiveList = createQuery( //
				"select e from ProcExeEtap e where e.id in (?1) and e.status in (?2)") //
					.setParameter(1, nextIdSet).setParameter(2, ProcExeEtapDAO.EXE_ETAP_ACTIVE_STATUS_LIST) //
					.getResultList();
		}

		if (nextActiveList != null && !nextActiveList.isEmpty()) {
			Date now = new Date();
			StringBuilder etapComments = new StringBuilder();

			SystemClassif empl = systemData.decodeItemLite(CODE_CLASSIF_ADMIN_STR, getUserId(), getUserLang(), now, false);
			if (empl != null) {
				etapComments.append("Етапът е отменен от " + empl.getTekst());
				etapComments.append(" от " + systemData.decodeItem(CODE_CLASSIF_ADMIN_STR, empl.getCodeParent(), getUserLang(), now));
				etapComments.append(" на " + new SimpleDateFormat("dd.MM.yyyy HH:mm").format(now));
				etapComments.append(" поради връщане изпълнението на процедурата на етап №" + selected.getNomer());
				etapComments.append(" " + selected.getEtapName() + ".");
			}

			String taskComments = "Задачата е приключена автоматично поради връщане изпълнението на процедура" //
				+ " №" + exe.getId() + " " + exe.getProcName() + " на предходен етап.";

			for (ProcExeEtap nextActive : nextActiveList) {
				cancelEtap(CODE_ZNACHENIE_ETAP_STAT_CANCEL, nextActive, etapComments.toString(), taskComments, systemData);
			}
		}

		// намирам дефиницията и мога да е стартирам вече
		List<ProcDefEtap> defEtapList = createQuery( //
			"select e from ProcDefEtap e where e.defId = ?1 and e.nomer = ?2") //
				.setParameter(1, exe.getDefId()).setParameter(2, selected.getNomer()) //
				.getResultList();

		if (defEtapList.isEmpty()) {
			throw new InvalidParameterException("Не е намерена дефиниция на етап с номер=" + selected.getNomer() //
				+ " и дефиниция на процедура с ИД=" + exe.getDefId());
		}
		ProcExeEtap restarted = startEtap(exe, exeEtapIdPrev, defEtapList.get(0), systemData, null, selected.getDocId());
		return restarted;
	}

	/**
	 * Връща информация за Таблица „Таблица „Дефинирани задачи“<br>
	 *
	 * @param exe
	 * @param etapNomer
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	public List<ProcDefTask> selectDefTaskList(ProcExe exe, Integer etapNomer, BaseSystemData systemData) throws DbErrorException {
		// първо ги намирам през дефиицията
		List<ProcDefTask> defTaskList = new ProcDefEtapDAO(getUser()).selectDefTaskList(exe.getDefId(), etapNomer);

		for (ProcDefTask defTask : defTaskList) { // после зареждам реалните определени кодове на изпълнителите
			defTask.setRealIzpCodes(new ArrayList<>());

			for (ProcDefTaskIzp izp : defTask.getTaskIzpList()) {
				try {
					defTask.getRealIzpCodes()
						.add(findCodeEmployee(exe.getDocId(), izp.getCodeRef(), izp.getZveno(), izp.getEmplPosition(), izp.getBusinessRole(), exe.getRegistraturaId(), systemData, false));

				} catch (InvalidParameterException e) { // явно няма да може да се определи
				}
			}
		}
		return defTaskList;
	}

	/**
	 * Връща информация за Таблица „Регистрирани задачи“<br>
	 * [0]-TASK_ID<br>
	 * [1]-RN_TASK<br>
	 * [2]-TASK_TYPE<br>
	 * [3]-TASK_INFO<br>
	 * [4]-ASSIGN_DATE<br>
	 * [5]-SROK_DATE<br>
	 * [6]-REAL_END<br>
	 * [7]-STATUS<br>
	 * [8]-STATUS_DATE<br>
	 * [9]-STATUS_COMMENTS<br>
	 * [10]-CODE_ASSIGN<br>
	 * [11]-CODE_CONTROL<br>
	 * [12]-CODE_EXECS-кодовете на изпълнителите с разделител ',' (5,2,20)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date)}<br>
	 * [13]-END_DOC_ID<br>
	 * [14]-END_RN_DOC<br>
	 * [15]-END_DOC_DATE<br>
	 * [16]-END_OPINION<br>
	 * [17]-STATUS_USER_ID<br>
	 *
	 * @param exeEtapId
	 * @return
	 * @throws DbErrorException
	 */
	public List<Object[]> selectRegTaskList(Integer exeEtapId) throws DbErrorException {
		try {
			String dialect = JPA.getUtil().getDbVendorName();

			StringBuilder sql = new StringBuilder();
			sql.append(" select t.TASK_ID a0, t.RN_TASK a1, t.TASK_TYPE a2, t.TASK_INFO a3 ");
			sql.append(" , t.ASSIGN_DATE a4, t.SROK_DATE a5, t.REAL_END a6 ");
			sql.append(" , t.STATUS a7, t.STATUS_DATE a8, t.STATUS_COMMENTS a9 ");
			sql.append(" , t.CODE_ASSIGN a10, t.CODE_CONTROL a11,  ");
			sql.append(DialectConstructor.convertToDelimitedString(dialect, "tr.CODE_REF", "TASK_REFERENTS tr where tr.TASK_ID = t.TASK_ID", "tr.ROLE_REF, tr.ID") + " a12 ");
			sql.append(" , ed.DOC_ID a13, "+DocDAO.formRnDocSelect("ed.", dialect)+" a14, ed.DOC_DATE a15, t.END_OPINION a16, t.STATUS_USER_ID a17 ");
			sql.append(" from PROC_EXE_TASK et inner join TASK t on t.TASK_ID = et.TASK_ID ");
			sql.append(" left outer join DOC ed on ed.DOC_ID = t.END_DOC_ID ");
			sql.append(" where et.EXE_ETAP_ID = ?1 ");

			Query query = createNativeQuery(sql.toString()).setParameter(1, exeEtapId);

			@SuppressWarnings("unchecked")
			List<Object[]> list = query.getResultList();
			return list;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на регистрирани задачи за етап!", e);
		}
	}

	/**
	 * Прекратяване на етап
	 *
	 * @param newStatus
	 * @param exeEtap
	 * @param etapComments
	 * @param taskComments
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	ProcExeEtap cancelEtap(Integer newStatus, ProcExeEtap exeEtap, String etapComments, String taskComments, BaseSystemData systemData) throws DbErrorException {
		exeEtap.setStatus(newStatus);
		exeEtap.setComments(etapComments);

		exeEtap = save(exeEtap);

		List<Integer> actStat = systemData.getSysClassificationCodes(CODE_CLASSIF_TASK_STATUS_ACTIVE, null, getUserLang());

		@SuppressWarnings("unchecked")
		List<Object> taskIdList = createNativeQuery( //
			"select t.TASK_ID from PROC_EXE_TASK et inner join TASK t on t.TASK_ID = et.TASK_ID where et.EXE_ETAP_ID = ?1 and t.STATUS in (?2)") //
				.setParameter(1, exeEtap.getId()).setParameter(2, actStat).getResultList();

		if (!taskIdList.isEmpty()) {

			TaskDAO taskDao = new TaskDAO(getUser());
			for (Object taskId : taskIdList) {
				Task task = taskDao.findById(((Number) taskId).intValue());

				task.setStatus(CODE_ZNACHENIE_TASK_STATUS_AUTO_PRIKL);
				task.setStatusComments(taskComments);

				taskDao.save(task, null, (SystemData) systemData);
			}
		}
		return exeEtap;
	}

	/**
	 * Управлява процедурата, когато задачата е по процедура и преминава от активен статус в неактивен или обратно
	 *
	 * @param task
	 * @param exeEtapId ИД на етапа, с който е свързана задачата
	 * @param sd
	 * @throws DbErrorException
	 */
	void processTask(Task task, int exeEtapId, SystemData sd) throws DbErrorException {

		@SuppressWarnings("unchecked")
		List<Object[]> rows = createNativeQuery( // намирам всички задачи по етапа (без текущата)
			"select t.TASK_ID, t.STATUS, t.TASK_TYPE, t.END_OPINION from PROC_EXE_TASK et inner join TASK t on t.TASK_ID = et.TASK_ID where et.EXE_ETAP_ID = ?1 and et.TASK_ID != ?2") //
				.setParameter(1, exeEtapId).setParameter(2, task.getId()).getResultList();

		boolean stopProcExist = false; // има ли поне едно мнение "прекратявам процедурата"
		boolean allStopProc = true; // Когато всички мнения са "прекратявам процедурата" системата прекратява процедурата.
		Map<Integer, Set<Integer>> opinionMap = new HashMap<>(); // тука ще се съберат всички мнения при приключване на задачите в
																	// текущия етап, разделени по вид задача
		if (task.getEndOpinion() != null) { // мнението на текущата задача
			Set<Integer> set = new HashSet<>();
			set.add(task.getEndOpinion());
			opinionMap.put(task.getTaskType(), set); // влиза си със вида

			allStopProc &= task.getEndOpinion().equals(OmbConstants.CODE_ZNACHENIE_TASK_OPINION_STOP_PROC);

			if (task.getEndOpinion().equals(OmbConstants.CODE_ZNACHENIE_TASK_OPINION_STOP_PROC)) {
				stopProcExist = true;
			}
		}

		boolean finished = true;
		for (Object[] row : rows) {
			Integer status = ((Number) row[1]).intValue();
			if (sd.matchClassifItems(CODE_CLASSIF_TASK_STATUS_ACTIVE, status, task.getStatusDate())) {
				finished = false; // има наприключена задача и няма как етапа да е готов
				break;
			}
			if (row[3] != null) { // има мнение
				Integer type = ((Number) row[2]).intValue();
				Set<Integer> set = opinionMap.get(type);
				if (set == null) {
					set = new HashSet<>();
					opinionMap.put(type, set);
				}
				int opinion = ((Number) row[3]).intValue();
				set.add(opinion);

				allStopProc &= opinion == OmbConstants.CODE_ZNACHENIE_TASK_OPINION_STOP_PROC;

				if (opinion == OmbConstants.CODE_ZNACHENIE_TASK_OPINION_STOP_PROC) {
					stopProcExist = true;
				}
			}
		}

		if (!finished) {
			return; // чакаме последната задача
		}
		ProcExeEtap exeEtap = findById(exeEtapId);

		if (!exeEtap.getStatus().equals(CODE_ZNACHENIE_ETAP_STAT_EXE)) {
			return; // този вече е приключил
		}

		if (!opinionMap.isEmpty() && allStopProc) { // автоматично прекратяване на процедура
			exeEtap.setComments("Етапът е прекратен поради прекратяване изпълнението на процедурата.");
			exeEtap.setStatus(CODE_ZNACHENIE_ETAP_STAT_STOP);
			save(exeEtap); // този е изпълнен

			ProcExeDAO exeDao = new ProcExeDAO(getUser());

			ProcExe exe = exeDao.findById(exeEtap.getExeId());
			exeDao.stopProc(exe, "Процедурата е прекратена от системата въз основа на единодушно мнение" //
				+ " при приключване на задачите от етапа \"прекратявам процедурата\"", true, exeEtap.getCodeRef(), sd);

		} else if (Objects.equals(exeEtap.getConditional(), CODE_ZNACHENIE_DA)) { // решаване на условен етап
			String[] next = processTaskDecision(exeEtap, opinionMap); // [0]-номерата,[1]-коментар

			if (next != null && next[0] != null) { // !!! условният е автоматично изчислен на къде да замине.
				completeEtap(exeEtap, sd, null, next[0], next[1], task.getEndDocId());

			} else { // ще си го решава потребител
				exeEtap.setStatus(CODE_ZNACHENIE_ETAP_STAT_DECISION);
				save(exeEtap);

				// Изпраща се нотификация до контролиращия на етапа. Вид събитие за нотификация: „За вземане на решение
				// по изпълнението на процедура“ с текст: „Необходимо е да се вземе решение по кой път да продължи
				// изпълнението на процедура № <PROC_EXE.EXE_ID> <PROC_EXE.PROC_NAME>, във връзка с приключването на етап №
				// <PROC_EXE_ETAP.NOMER> <PROC_EXE_ETAP.ETAP_NAME>.“

				@SuppressWarnings("unchecked")
				List<Object> names = createNativeQuery("select PROC_NAME from PROC_EXE where EXE_ID = ?1") //
					.setParameter(1, exeEtap.getExeId()).getResultList();

				Object procName = names.isEmpty() ? "<PROC_EXE.PROC_NAME>" : names.get(0);

				StringBuilder comment = new StringBuilder();
				comment.append("Необходимо е да се вземе решение по кой път да продължи изпълнението на процедура №" + exeEtap.getExeId());
				comment.append(" " + procName);
				comment.append(", във връзка с приключването на етап №" + exeEtap.getNomer());
				comment.append(" " + exeEtap.getEtapName() + ".");

				Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
					, CODE_ZNACHENIE_NOTIFF_EVENTS_DECISION_PROC, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, sd);
				notif.setProcId(exeEtap.getExeId());
				notif.setComment(comment.toString());
				notif.getAdresati().add(exeEtap.getCodeRef());
				notif.send();
			}

		} else if (!opinionMap.isEmpty() && stopProcExist) { // линеен етап, в който има мнение за прекратяване, но има и други
																// мнения
			exeEtap.setStatus(CODE_ZNACHENIE_ETAP_STAT_DECISION);
			save(exeEtap);

			@SuppressWarnings("unchecked")
			List<Object> names = createNativeQuery("select PROC_NAME from PROC_EXE where EXE_ID = ?1") //
				.setParameter(1, exeEtap.getExeId()).getResultList();

			Object procName = names.isEmpty() ? "<PROC_EXE.PROC_NAME>" : names.get(0);

			StringBuilder comment = new StringBuilder();
			comment.append("Необходимо е да се вземе решение как да продължи изпълнението на процедура №" + exeEtap.getExeId());
			comment.append(" " + procName);
			comment.append(", във връзка с приключването на етап №" + exeEtap.getNomer());
			comment.append(" " + exeEtap.getEtapName() + ".");

			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_DECISION_PROC, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, sd);
			notif.setProcId(exeEtap.getExeId());
			notif.setComment(comment.toString());
			notif.getAdresati().add(exeEtap.getCodeRef());
			notif.send();

		} else { // тука вече може и да се го приключим
			completeEtap(exeEtap, sd, true, null, null, task.getEndDocId());
		}
	}

	/**
	 * Стартиране на етап от процедура
	 *
	 * @param exe           (id,docId,workDaysOnly)
	 * @param exeEtapIdPrev
	 * @param defEtap
	 * @param systemData
	 * @param branchPath
	 * @param docId
	 * @return
	 * @throws DbErrorException
	 */
	ProcExeEtap startEtap(ProcExe exe, Integer exeEtapIdPrev, ProcDefEtap defEtap, BaseSystemData systemData, String branchPath, Integer docId) throws DbErrorException {
		ProcExeEtap exeEtap = new ProcExeEtap();

		exeEtap.setExeId(exe.getId()); // изпълнението
		exeEtap.setExeEtapIdPrev(exeEtapIdPrev); // и предходния етап
		exeEtap.setBranchPath(branchPath);
		exeEtap.setDocId(docId);

		// прекопирвам данните в изпълнението
		exeEtap.setNomer(defEtap.getNomer());
		exeEtap.setEtapName(defEtap.getEtapName());
		exeEtap.setEtapInfo(defEtap.getEtapInfo());

		String nextNot = trimToNULL(defEtap.getNextNot());
		String nextOptional = trimToNULL(defEtap.getNextOptional());

		exeEtap.setConditional(nextNot != null || nextOptional != null ? CODE_ZNACHENIE_DA : CODE_ZNACHENIE_NE);

		exeEtap.setSrokDays(defEtap.getSrokDays());
		exeEtap.setSrokHours(defEtap.getSrokHours());
		exeEtap.setInstructions(defEtap.getInstructions());

		try {
			Integer codeRef = findCodeEmployee(exeEtap.getDocId(), defEtap.getCodeRef(), defEtap.getZveno(), defEtap.getEmplPosition(), defEtap.getBusinessRole(), exe.getRegistraturaId(), systemData,
				true);

			exeEtap.setStatus(CODE_ZNACHENIE_ETAP_STAT_EXE);
			exeEtap.setCodeRef(codeRef);

		} catch (InvalidParameterException e) {
			LOGGER.debug("Очаква се определяне на контролиращ на етапа: {}", e.getMessage());
			exeEtap.setStatus(CODE_ZNACHENIE_ETAP_STAT_WAIT);
			exeEtap.setComments("Очаква се определяне на контролиращ на етапа.");

			// Изпраща се нотификация до отговорния за изпълнението на процедурата. Вид събитие за нотификация: "За
			// определяне на контролиращ на етап от процедура" с текст: „Необходимо е да се определи служител, контролиращ етап №
			// <PROC_EXE_ETAP.NOMER> <PROC_EXE_ETAP.ETAP_NAME> от процедура № <PROC_EXE.EXE_ID> <PROC_EXE.PROC_NAME>.“

			StringBuilder comment = new StringBuilder();
			comment.append("Необходимо е да се определи служител, контролиращ етап №" + exeEtap.getNomer());
			comment.append(" " + exeEtap.getEtapName());
			comment.append(" от процедура №" + exe.getId());
			comment.append(" " + exe.getProcName() + ".");

			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_CONTROL_ETAP, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, (SystemData) systemData);
			notif.setProcId(exe.getId());
			notif.setComment(comment.toString());
			notif.getAdresati().add(exe.getCodeRef());
			notif.send();
		}

		exeEtap.setBeginDate(new Date());
		exeEtap.setSrokDate(calcSrokDate(exeEtap.getBeginDate(), exeEtap.getSrokDays(), exeEtap.getSrokHours(), exe.getWorkDaysOnly(), systemData));

		exeEtap = save(exeEtap); // !!! основния запис !!!

		if (exeEtap.getCodeRef() != null) { // трябва да има и отгворник етап за да се пуснат задачите
			List<ProcDefTask> defTaskList = new ProcDefEtapDAO(getUser()).selectDefTaskList(defEtap.getId());

			registerEtapTasks(exe, exeEtap, defTaskList, systemData);
		}

		return exeEtap;
	}

	/**
	 * @param exeEtap
	 * @param defTask
	 * @param codeAssign
	 * @param workDaysOnly
	 * @return
	 */
	private Task createTask(ProcExeEtap exeEtap, ProcDefTask defTask, Integer codeAssign, Integer workDaysOnly, BaseSystemData systemData) {
		Task task = new Task();

		task.setCodeAssign(codeAssign);
		if (!exeEtap.getCodeRef().equals(codeAssign)) {
			task.setCodeControl(exeEtap.getCodeRef()); // контролиращия на етапа се използва само ако той не се явява
														// възложителя на задачата
		}

		task.setStatus(CODE_ZNACHENIE_TASK_STATUS_NEIZP);
		task.setTaskType(defTask.getTaskType());
		task.setTaskInfo(defTask.getTaskInfo());
		if (defTask.getDocRequired() == null) {
			task.setDocRequired(SysConstants.CODE_ZNACHENIE_NE);
		} else {
			task.setDocRequired(defTask.getDocRequired());
		}
		task.setAssignDate(new Date());

		if (defTask.getSrokHours() == null && defTask.getSrokDays() == null) { // в задачата няма и ги изчислявам от полетата в
																				// етапа
			task.setSrokDate(calcSrokDate(task.getAssignDate(), exeEtap.getSrokDays(), exeEtap.getSrokHours(), workDaysOnly, systemData));
		} else {
			task.setSrokDate(calcSrokDate(task.getAssignDate(), defTask.getSrokDays(), defTask.getSrokHours(), workDaysOnly, systemData));
		}
		task.setCodeExecs(new ArrayList<>());

		return task;
	}

	/**
	 * Специфична логика в зависимост от алгоритъма. Вероятно ще е много мазало в този метод.
	 *
	 * @param docId
	 * @param alg
	 * @param date
	 * @param sd
	 * @return
	 * @throws InvalidParameterException
	 * @throws DbErrorException
	 */
	private Integer findCodeEmployeeAlg(Integer docId, Integer alg, Date date, BaseSystemData sd) throws InvalidParameterException, DbErrorException {
		if (alg.equals(CODE_ZNACHENIE_PROC_BUSINESS_ROLE_AUTHOR_BOSS) // Ръководител на автора на документ
			|| alg.equals(CODE_ZNACHENIE_PROC_BUSINESS_ROLE_AUTHOR)) { // или // Ръководител на автора на документ
			if (docId == null) {
				throw new InvalidParameterException("Липсва документ.");
			}

			@SuppressWarnings("unchecked")
			List<Object> rows = createNativeQuery("select CODE_REF from DOC_REFERENTS where DOC_ID = ?1 and ROLE_REF = ?2 order by PORED") //
				.setParameter(1, docId).setParameter(2, CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR).getResultList();
			if (rows.isEmpty()) {
				throw new InvalidParameterException("За документ с ИД=" + docId + ", не е намерен автор в БД.");
			}

			SystemClassif author = null;
			for (Object row : rows) {
				SystemClassif item = sd.decodeItemLite(CODE_CLASSIF_ADMIN_STR, ((Number) row).intValue(), CODE_DEFAULT_LANG, date, true);
				if (item != null) {
					Integer refType = (Integer) item.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE];
					if (Objects.equals(refType, CODE_ZNACHENIE_REF_TYPE_EMPL)) {
						author = item;
						break;
					}
				}
			}
			if (author == null) {
				throw new InvalidParameterException("За документ с ИД=" + docId + ", не е намерен автор в Административната структура.");
			}

			if (alg.equals(CODE_ZNACHENIE_PROC_BUSINESS_ROLE_AUTHOR)) {
				return author.getCode(); // ако ни трябва само автора сме готови
			}

			Integer codeParent; // в това звено ще се търси шефа
			Integer authorPosition = (Integer) author.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_POSITION];

			if (sd.matchClassifItems(CODE_CLASSIF_BOSS_POSITIONS, authorPosition, date)) { // този е шеф и неговия шеф е от
																							// горното звено
				codeParent = sd.getItemParentCode(CODE_CLASSIF_ADMIN_STR, author.getCodeParent(), date);
			} else {
				codeParent = author.getCodeParent();
			}
			if (codeParent == null) {
				throw new InvalidParameterException("За служител с КОД=" + author.getCode() + " не е намерен ръководител в Административната структура.");
			}

			List<SystemClassif> items = sd.getChildrenOnNextLevel(CODE_CLASSIF_ADMIN_STR, codeParent, date, CODE_DEFAULT_LANG);
			for (SystemClassif item : items) {
				Integer itemPosition = (Integer) sd.getItemSpecific(CODE_CLASSIF_ADMIN_STR, item.getCode(), CODE_DEFAULT_LANG, date, OmbClassifAdapter.ADM_STRUCT_INDEX_POSITION);
				if (itemPosition != null && sd.matchClassifItems(CODE_CLASSIF_BOSS_POSITIONS, itemPosition, date)) {
					return item.getCode(); // само тука сме ОК
				}
			}
			throw new InvalidParameterException("За служител с КОД=" + author.getCode() + " не е намерен ръководител в Административната структура.");

		} else if (alg.equals(CODE_ZNACHENIE_PROC_BUSINESS_ROLE_AUTHOR_KOLEGA)) { // Колега на автора на документ
			throw new InvalidParameterException("Бизнес роля в процедура 'Колега на автора на документ' не е разработена.");

		} else {
			throw new InvalidParameterException("Непозната бизнес роля в процедура " + alg);
		}
	}

	/**
	 * В зависимост от мненията на приключване на задачите в етапа и избраните мнения в дефиницията на етапа, може да се определи
	 * автоматично на къде да се предвижи процеса
	 *
	 * @param exeEtap
	 * @param opinionMap
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	private String[] processTaskDecision(ProcExeEtap exeEtap, Map<Integer, Set<Integer>> opinionMap) throws DbErrorException {
		if (opinionMap.isEmpty()) {
			return null;
		}

		// key=TASK_TYPE, value=(key=OPINION, value=OPTYPE)
		Map<Integer, Map<Integer, Integer>> defMap = new HashMap<>(); // дефиницията на условния етап по вид задачи и т.н.
		List<Object[]> rows;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select distinct o.TASK_TYPE, o.OPINION, o.OPTYPE, de.NEXT_OK, de.NEXT_NOT, de.NEXT_OPTIONAL, de.DEF_ID ");
			sql.append(" from PROC_EXE_ETAP ee ");
			sql.append(" inner join PROC_EXE e on e.EXE_ID = ee.EXE_ID ");
			sql.append(" inner join PROC_DEF_ETAP de on de.DEF_ID = e.DEF_ID and de.NOMER = ee.NOMER ");
			sql.append(" inner join PROC_DEF_OPINION o on o.DEF_ETAP_ID = de.DEF_ETAP_ID ");
			sql.append(" where ee.EXE_ETAP_ID = ?1 ");

			rows = createNativeQuery(sql.toString()).setParameter(1, exeEtap.getId()).getResultList();
			for (Object[] row : rows) {
				defMap.computeIfAbsent(asInteger(row[0]), HashMap::new).put(asInteger(row[1]), asInteger(row[2]));
			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на мнения на приключени задачи в етап.", e);
		}
		if (defMap.isEmpty()) {
			return null;
		}

		int ok = 0;
		int not = 0;
		int opt = 0;

		int check = 0; // общата сума при която може да се реши
		for (Entry<Integer, Set<Integer>> entry : opinionMap.entrySet()) {
			check += entry.getValue().size();

			Map<Integer, Integer> defOpinion = defMap.get(entry.getKey());

			if (defOpinion == null) {
				break; // ако за някоя вид задача няма дефинирани мнения не може да се определи
			}

			for (Integer value : entry.getValue()) {
				Integer defOpt = defOpinion.get(value);

				if (defOpt == null) {
					//
				} else if (defOpt.equals(1)) {
					ok++;
				} else if (defOpt.equals(2)) {
					not++;
				} else {
					opt++;
				}
			}
		}

		String next = null;
		if (ok == check) {
			next = trimToNULL((String) rows.get(0)[3]); // NEXT_OK

		} else if (not == check) {
			next = trimToNULL((String) rows.get(0)[4]); // NEXT_NOT

		} else if (opt == check) {
			next = trimToNULL((String) rows.get(0)[5]); // NEXT_OPTIONAL
		}
		if (next == null) {
			return null;
		}

		String comments = null; // ще се придвижда, но трябва да се определи и коментара
		try {
			StringBuilder sb = new StringBuilder();
			List<Object[]> nextEtapList = createNativeQuery( //
				"select NOMER, ETAP_NAME from PROC_DEF_ETAP where NOMER in (" + next + ") and DEF_ID = ?1 order by NOMER") //
					.setParameter(1, rows.get(0)[6]).getResultList();

			if (nextEtapList.size() == 1) {
				sb.append("За следващ етап е определен №" + nextEtapList.get(0)[0] + " " + nextEtapList.get(0)[1]);

			} else {
				for (Object[] nextEtap : nextEtapList) {
					if (sb.length() == 0) {
						sb.append("За следващи етапи са определени ");
					} else {
						sb.append(", ");
					}
					sb.append("№" + nextEtap[0] + " " + nextEtap[1]);
				}
			}
			comments = sb.toString() + ". Решението е взето на " + DateUtils.printDateFull(new Date()) //
				+ " от Системата въз основа на мнения при приключване на задачите.";

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на следващи етапи при автоматично придвижване на етап.", e);
		}
		return new String[] { next, comments };
	}

	private void recursiveFindNextExeEtapIdList(Integer exeEtapId, Set<Integer> nextIdSet) {
		@SuppressWarnings("unchecked")
		List<Object> found = createNativeQuery("select EXE_ETAP_ID from PROC_EXE_ETAP where EXE_ETAP_ID_PREV = ?1") //
			.setParameter(1, exeEtapId).getResultList();
		if (found.isEmpty()) {
			return;
		}

		for (Object tmp : found) {
			Integer id = ((Number) tmp).intValue();
			nextIdSet.add(id);

			recursiveFindNextExeEtapIdList(id, nextIdSet);
		}
	}

	/**
	 * Регистрира задачите за етапа
	 *
	 * @param exe
	 * @param exeEtap
	 * @param defTaskList
	 * @param systemData
	 * @throws DbErrorException
	 */
	private void registerEtapTasks(ProcExe exe, ProcExeEtap exeEtap, List<ProcDefTask> defTaskList, BaseSystemData systemData) throws DbErrorException {
		List<Task> taskList = new ArrayList<>(); // тука ще се трупат накрая ще се записват, защото ако нещо не е
													// валидно не трябва да се запише нито една
		String error = null; // ако има грешка ще се праща нотификация

		for (ProcDefTask defTask : defTaskList) {
			Integer codeAssign = null;
			try {
				codeAssign = findCodeEmployee(exeEtap.getDocId(), defTask.getAssignCodeRef(), defTask.getAssignZveno(), defTask.getAssignEmplPosition(), defTask.getAssignBusinessRole(),
					exe.getRegistraturaId(), systemData, true);
			} catch (InvalidParameterException e) { // ако не може да се определи го взимам от етапа
				LOGGER.debug("Възложителят на задачата се взима от Контролиращ етап: {}", e.getMessage());
				codeAssign = exeEtap.getCodeRef();
			}

			if (defTask.getRealIzpCodes() != null && !defTask.getRealIzpCodes().isEmpty()) { // има явни изпълнители и те трябва
																								// да се изпозлват
				if (defTask.getRealIzpCodes().size() > 1 //
					&& (Objects.equals(defTask.getIndividual(), CODE_ZNACHENIE_DA) //
						|| systemData.matchClassifItems(CODE_CLASSIF_TASK_ONE_EMPL, defTask.getTaskType(), null))) {

					for (Integer code : defTask.getRealIzpCodes()) {
						Task task = createTask(exeEtap, defTask, codeAssign, exe.getWorkDaysOnly(), systemData);
						task.getCodeExecs().add(code);
						taskList.add(task);
					}

				} else {
					Task task = createTask(exeEtap, defTask, codeAssign, exe.getWorkDaysOnly(), systemData);
					task.getCodeExecs().addAll(defTask.getRealIzpCodes());
					taskList.add(task);
				}

			} else if (defTask.getTaskIzpList().size() > 1 //
				&& (Objects.equals(defTask.getIndividual(), CODE_ZNACHENIE_DA) //
					|| systemData.matchClassifItems(CODE_CLASSIF_TASK_ONE_EMPL, defTask.getTaskType(), null))) {

				for (ProcDefTaskIzp izp : defTask.getTaskIzpList()) { // тука трябва да се напряви отделна задача за всеки
																		// изпънител
					Task task = createTask(exeEtap, defTask, codeAssign, exe.getWorkDaysOnly(), systemData);
					try {
						Integer code = findCodeEmployee(exeEtap.getDocId(), izp.getCodeRef(), izp.getZveno(), izp.getEmplPosition(), izp.getBusinessRole(), exe.getRegistraturaId(), systemData, false);
						task.getCodeExecs().add(code);
					} catch (InvalidParameterException e) {
						LOGGER.debug("Очаква се определяне на изпълнители на задачи от етапа: {}", e.getMessage());
						error = "Очаква се определяне на изпълнители на задачи от етапа.";
						break;
					}
					taskList.add(task);
				}

			} else {
				Task task = createTask(exeEtap, defTask, codeAssign, exe.getWorkDaysOnly(), systemData); // само една задача със
																											// всички изпълнители
				for (ProcDefTaskIzp izp : defTask.getTaskIzpList()) {
					try {
						Integer code = findCodeEmployee(exeEtap.getDocId(), izp.getCodeRef(), izp.getZveno(), izp.getEmplPosition(), izp.getBusinessRole(), exe.getRegistraturaId(), systemData, false);
						task.getCodeExecs().add(code);
					} catch (InvalidParameterException e) {
						LOGGER.debug("Очаква се определяне на изпълнители на задачи от етапа: {}", e.getMessage());
						error = "Очаква се определяне на изпълнители на задачи от етапа.";
						break;
					}
				}
				taskList.add(task);
			}
		}

		if (error != null) {
			exeEtap.setStatus(CODE_ZNACHENIE_ETAP_STAT_WAIT);
			exeEtap.setComments(error);
			// Изпраща се нотификация до контролиращия на етапа. Вид събитие за нотификация: "За определяне на изпълнител
			// на задача по процедура" с текст: „Необходимо е да се определят служители, изпълнители на задачи от етап №
			// <PROC_EXE_ETAP.NOMER> <PROC_EXE_ETAP.ETAP_NAME> от процедура № <PROC_EXE.EXE_ID> <PROC_EXE.PROC_NAME>.“

			StringBuilder comment = new StringBuilder();
			comment.append("Необходимо е да се определят служители, изпълнители на задачи от етап №" + exeEtap.getNomer());
			comment.append(" " + exeEtap.getEtapName());
			comment.append(" от процедура №" + exe.getId());
			comment.append(" " + exe.getProcName() + ".");

			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_PROC, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, (SystemData) systemData);
			notif.setProcId(exe.getId());
			notif.setComment(comment.toString());
			notif.getAdresati().add(exeEtap.getCodeRef());
			notif.send();

		} else {
//			TaskDAO taskDao = new TaskDAO(new UserData(exe.getCodeRef(), "", "")); // по този начин регистриралия задачата ще е
//																					// отговорния на процедурата
			TaskDAO taskDao = new TaskDAO(getUser());

			ProcExeTaskDAO exeTaskDao = new ProcExeTaskDAO(getUser());

			StringBuilder info = new StringBuilder();
			info.append("Процедура №" + exe.getId() + " " + exe.getProcName() + ", Етап №" + exeEtap.getNomer() + " " + exeEtap.getEtapName());

			for (Task task : taskList) {
				task.setDocId(exeEtap.getDocId()); // винаги задачите са по документа на етапа с който са свързани
				task.setRegistraturaId(exe.getRegistraturaId());
				task.setProcInfo(info.toString());

				taskDao.save(task, null, (SystemData) systemData);

				// връзката на задачата с изпълнението на етапа на процедурата
				ProcExeTask exeTask = new ProcExeTask();
				exeTask.setExeEtapId(exeEtap.getId());
				exeTask.setTaskId(task.getId());
				exeTaskDao.save(exeTask);

				if (exeEtap.getDocId() != null) {
					@SuppressWarnings("unchecked")
					List<Object[]> rows = createNativeQuery("select PROCESSED, RN_DOC, DOC_DATE, PORED_DELO from DOC where DOC_ID = ?1 and DOC_TYPE = ?2") //
						.setParameter(1, exeEtap.getDocId()).setParameter(2, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN) //
						.getResultList();

					if (!rows.isEmpty() && !Objects.equals(CODE_ZNACHENIE_DA, asInteger(rows.get(0)[0]))) { // става обработен

						getEntityManager().createNativeQuery("update DOC set PROCESSED = ?1 where DOC_ID = ?2") //
							.setParameter(1, CODE_ZNACHENIE_DA).setParameter(2, exeEtap.getDocId()).executeUpdate();

						String docIdent = DocDAO.formRnDocDate(rows.get(0)[1], rows.get(0)[2], rows.get(0)[3]);
						SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, exeEtap.getDocId(),
							"Документ " + docIdent + " е маркиран за обработен, при регистриране на задача " + task.getRnTask() + ".");

						journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
						journal.setDateAction(new Date());
						journal.setIdUser(getUserId());

						saveAudit(journal);

						exe.setDocProcessed(CODE_ZNACHENIE_DA);
					}
				}
			}
		}
	}
}

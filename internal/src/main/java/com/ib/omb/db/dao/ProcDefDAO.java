package com.ib.omb.db.dao;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_DEV;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_STAT_EXE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_PROC_STAT_WAIT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dto.ProcDef;
import com.ib.omb.db.dto.ProcDefEtap;
import com.ib.omb.db.dto.ProcDefTask;
import com.ib.omb.db.dto.ProcDefTaskIzp;
import com.ib.omb.system.OmbConstants;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link ProcDef}
 *
 * @author belev
 */
public class ProcDefDAO extends AbstractDAO<ProcDef> {

	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcDefDAO.class);

	/** @param user */
	public ProcDefDAO(ActiveUser user) {
		super(ProcDef.class, user);
	}

	/**
	 * Прави копие на процедурата
	 *
	 * @param sourceDefId
	 * @param newName
	 * @param registraturaId
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public ProcDef copyProcDef(Integer sourceDefId, String newName, Integer registraturaId, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		ProcDef copy = super.findById(sourceDefId);
		if (copy == null) {
			return copy;
		}

		// при различни регистратур трябва да се зачисти цялата инфотмация за хората по процедурата
		boolean clearReferents = !copy.getRegistraturaId().equals(registraturaId);

		JPA.getUtil().getEntityManager().detach(copy);
		copy.setId(null);
		copy.setDateLastMod(null);
		copy.setUserLastMod(null);
		copy.setStatus(CODE_ZNACHENIE_PROC_DEF_STAT_DEV);
		copy.setRegistraturaId(registraturaId);
		if (newName != null) {
			copy.setProcName(newName);
		}

		if (clearReferents) {
			copy.setCodeRef(null);
			copy.setZveno(null);
			copy.setEmplPosition(null);
			copy.setBusinessRole(null);
		}
		copy = save(copy, systemData);

		ProcDefEtapDAO defEtapDao = new ProcDefEtapDAO(getUser());
		ProcDefTaskDAO defTaskDao = new ProcDefTaskDAO(getUser());

		List<ProcDefEtap> defEtapList = selectDefEtapList(sourceDefId, null);
		for (ProcDefEtap defEtap : defEtapList) {
			JPA.getUtil().getEntityManager().detach(defEtap);

			List<ProcDefTask> defTaskList = defEtapDao.selectDefTaskList(defEtap.getId());

			defEtap.setId(null);
			defEtap.setDateLastMod(null);
			defEtap.setUserLastMod(null);
			defEtap.setDefId(copy.getId());

			if (clearReferents) {
				defEtap.setCodeRef(null);
				defEtap.setZveno(null);
				defEtap.setEmplPosition(null);
				defEtap.setBusinessRole(null);
			}
			defEtapDao.save(defEtap);

			for (ProcDefTask defTask : defTaskList) {
				List<ProcDefTaskIzp> copyIzpList = null;
				if (!clearReferents) { // само ако не трябва да се зачиствам ги прекопирвам
					copyIzpList = new ArrayList<>();
					for (ProcDefTaskIzp izp : defTask.getTaskIzpList()) {
						copyIzpList.add(new ProcDefTaskIzp(izp));
					}
				}

				JPA.getUtil().getEntityManager().detach(defTask);

				defTask.setTaskIzpList(copyIzpList);

				defTask.setId(null);
				defTask.setDateLastMod(null);
				defTask.setUserLastMod(null);
				defTask.setDefEtapId(defEtap.getId());

				if (clearReferents) {
					defTask.setAssignCodeRef(null);
					defTask.setAssignZveno(null);
					defTask.setAssignEmplPosition(null);
					defTask.setAssignBusinessRole(null);
				}
				defTaskDao.save(defTask);
			}
		}

		// копиране и на файловете само като връзка с новата процедура
		FilesDAO filesDao = new FilesDAO(getUser());
		List<Files> files = filesDao.selectByFileObject(sourceDefId, OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_DEF);
		for (Files f : files) {
			filesDao.saveFileObject(f, copy.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_DEF);
		}

		return copy;
	}

	/**
	 * Конструира селект за търсене на дефиниции на процедури, като изтегля само данните от вида:<br>
	 * [0]-DEF_ID<br>
	 * [1]-PROC_NAME<br>
	 * [2]-PROC_INFO<br>
	 * [3]-STATUS<br>
	 * [4]-SROK_DAYS<br>
	 * [5]-REGISTRATURA_ID<br>
	 * [6]-CODE_REF<br>
	 * [7]-ZVENO<br>
	 * [8]-EMPL_POSITION<br>
	 * [9]-BUSINESS_ROLE<br>
	 *
	 * @param registraturaId
	 * @param defId
	 * @param procName
	 * @param procInfo
	 * @param status
	 * @param codeRef
	 * @param zveno
	 * @param emplPosition
	 * @param businessRole
	 * @return
	 */
	public SelectMetadata createSelectDefProceduri(Integer registraturaId, Integer defId, String procName, String procInfo, Integer status //
		, Integer codeRef, Integer zveno, Integer emplPosition, Integer businessRole) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		String select = " select p.DEF_ID a0, p.PROC_NAME a1, " //
			+ DialectConstructor.limitBigString(dialect, "p.PROC_INFO", 300) + " a2, p.STATUS a3, p.SROK_DAYS a4, p.REGISTRATURA_ID a5" //
			+ ", p.CODE_REF a6, p.ZVENO a7, p.EMPL_POSITION a8, p.BUSINESS_ROLE a9 ";
		String from = " from PROC_DEF p ";
		StringBuilder where = new StringBuilder(" where 1=1 ");

		if (registraturaId != null) {
			where.append(" and p.REGISTRATURA_ID = :registraturaId ");
			params.put("registraturaId", registraturaId);
		}
		if (defId != null) {
			where.append(" and p.DEF_ID = :defId ");
			params.put("defId", defId);
		}
		if (status != null) {
			where.append(" and p.STATUS = :status ");
			params.put("status", status);
		}
		if (codeRef != null) {
			where.append(" and p.CODE_REF = :codeRef ");
			params.put("codeRef", codeRef);
		}
		if (zveno != null) {
			where.append(" and p.ZVENO = :zveno ");
			params.put("zveno", zveno);
		}
		if (emplPosition != null) {
			where.append(" and p.EMPL_POSITION = :emplPosition ");
			params.put("emplPosition", emplPosition);
		}
		if (businessRole != null) {
			where.append(" and p.BUSINESS_ROLE = :businessRole ");
			params.put("businessRole", businessRole);
		}

		String t = SearchUtils.trimToNULL_Upper(procName);
		if (t != null) {
			where.append(" and upper(p.PROC_NAME) like :procName ");
			params.put("procName", "%" + t + "%");
		}
		t = SearchUtils.trimToNULL_Upper(procInfo);
		if (t != null) {
			where.append(" and upper(p.PROC_INFO) like :procInfo ");
			params.put("procInfo", "%" + t + "%");
		}

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSql(select + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/** */
	@Override
	public void delete(ProcDef entity) throws DbErrorException, ObjectInUseException {
		try {
			Number cnt = (Number) createNativeQuery("select count (*) CNT from PROC_EXE where DEF_ID = ?1") //
				.setParameter(1, entity.getId()) //
				.getResultList().get(0);
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("За процедурата има регистрирани изпълнения и не може да бъде изтрита.");
			}
			cnt = (Number) createNativeQuery("select count (*) CNT from DOC_VID_SETTINGS where PROC_DEF_IN = :defId or PROC_DEF_OWN = :defId or PROC_DEF_WORK = :defId") //
				.setParameter("defId", entity.getId()) //
				.getResultList().get(0);
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("За процедурата има регистрирани настройки за вид докумнет и не може да бъде изтрита.");
			}

		} catch (ObjectInUseException e) {
			throw e; // за да не се преопакова

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани обекти за дефиницията на процедура!", e);
		}

		List<ProcDefEtap> etapList = selectDefEtapList(entity.getId(), null);
		if (!etapList.isEmpty()) {
			ProcDefEtapDAO etapDao = new ProcDefEtapDAO(getUser());
			for (ProcDefEtap etap : etapList) {
				etapDao.delete(etap);
			}
		}
		super.delete(entity);
	}

	/** */
	@Override
	public ProcDef findById(Object id) throws DbErrorException {
		ProcDef entity = super.findById(id);

		if (entity != null) {
			entity.setDbStatus(entity.getStatus());
		}
		return entity;
	}

	/**
	 * @param entity
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public ProcDef save(ProcDef entity, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		if (!entity.getStatus().equals(entity.getDbStatus())) { // смяна на статус

			if (entity.getStatus().equals(CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE)) {

				String error = validate(entity, systemData); // винаги към активна има валидация
				if (error != null) {
					throw new ObjectInUseException(error);
				}

			} else if (entity.getId() != null && entity.getStatus().equals(CODE_ZNACHENIE_PROC_DEF_STAT_DEV)) {
				// корекция и преминава към разработва се - трябва да се провери дали има активни изпълнения

				Number cnt = (Number) createNativeQuery("select count (*) CNT from PROC_EXE where DEF_ID = ?1 and STATUS in (?2,?3)") //
					.setParameter(1, entity.getId()) //
					.setParameter(2, CODE_ZNACHENIE_PROC_STAT_WAIT).setParameter(3, CODE_ZNACHENIE_PROC_STAT_EXE) //
					.getResultList().get(0);
				if (cnt != null && cnt.intValue() > 0) {
					throw new ObjectInUseException("Статусът не може да бъде сменен на 'разработва се', защото за процедурата има активни изпълнения!");
				}
			}
		}

		ProcDef saved = super.save(entity);

		saved.setDbStatus(saved.getStatus()); // JPA MERGE
		return saved;
	}

	/**
	 * Връща всички дефиниции на етапи за процедурата сортирани по НОМЕР
	 *
	 * @param defId
	 * @param maxResult колко макс да се върнат
	 * @return
	 * @throws DbErrorException
	 */
	public List<ProcDefEtap> selectDefEtapList(Integer defId, Integer maxResult) throws DbErrorException {
		try {
			Query query = createQuery("select e from ProcDefEtap e where e.defId = :defIdArg order by e.nomer") //
				.setParameter("defIdArg", defId);
			if (maxResult != null) {
				query.setMaxResults(maxResult);
			}

			@SuppressWarnings("unchecked")
			List<ProcDefEtap> list = query.getResultList();
			return list;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на дефиниции на етапи за процедура!", e);
		}
	}

	/**
	 * Валидира по спецификация дали дефиницията може да се активира
	 *
	 * @param entity
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	public String validate(ProcDef entity, BaseSystemData systemData) throws DbErrorException {
		if (!isValidReferentData(entity.getCodeRef(), entity.getZveno(), entity.getEmplPosition(), entity.getBusinessRole())) {
			return "За процедурата няма дефиниран отговорен за изпълнението.";
		}
		List<ProcDefEtap> defEtapList = selectDefEtapList(entity.getId(), null);
		if (defEtapList.isEmpty()) {
			return "За процедурата няма дефинирани етапи.";
		}

		ProcDefEtapDAO defEtapDao = new ProcDefEtapDAO(getUser());

		Map<Integer, Set<Integer>> mapNextOk = new HashMap<>(); // key=etap.nomer; value=nextOk

		Set<Integer> allNextOk = new HashSet<>();
		Set<Integer> allNextOther = new HashSet<>();
		try {
			for (ProcDefEtap defEtap : defEtapList) {
				if (!isValidReferentData(defEtap.getCodeRef(), defEtap.getZveno(), defEtap.getEmplPosition(), defEtap.getBusinessRole())) {
					return "За етап с номер " + defEtap.getNomer() + ", няма дефиниран контролиращ.";
				}
				List<ProcDefTask> defTaskList = defEtapDao.selectDefTaskList(defEtap.getId());
				if (defTaskList.isEmpty()) {
					return "За етап с номер " + defEtap.getNomer() + ", няма дефинирани задачи.";
				}
				for (ProcDefTask defTask : defTaskList) {
					if (defTask.getTaskIzpList().isEmpty()) {
						return "За етап с номер " + defEtap.getNomer() + ", е дефинирана задача без изпълнител.";
					}
					for (ProcDefTaskIzp izp : defTask.getTaskIzpList()) {
						if (!isValidReferentData(izp.getCodeRef(), izp.getZveno(), izp.getEmplPosition(), izp.getBusinessRole())) {
							return "За етап с номер " + defEtap.getNomer() + ", е дефинирана задача без изпълнител.";
						}
					}
				}

				Set<Integer> value = new HashSet<>();
				mapNextOk.put(defEtap.getNomer(), value);

				String ok = SearchUtils.trimToNULL(defEtap.getNextOk());
				if (ok != null) { // ще се валидира само този път за зацикляния
					String[] ts = ok.split(",");
					for (String t : ts) {
						Integer nomer = Integer.valueOf(t.trim());
						if (nomer.equals(defEtap.getNomer())) {
							return "В Следващи етапи не се допуска да се въведе текущия етап.";
						}
						value.add(nomer);
					}
					allNextOk.addAll(value);
				}

				String not = SearchUtils.trimToNULL(defEtap.getNextNot());
				if (not != null) { // такива ще се трупат за да не би да са въведени номера за етапи, които ги няма
					if (ok == null) {
						return "За етап с номер " + defEtap.getNomer() + ", не е въведена стойност за 'Следващи етапи (при ДА)'.";
					}
					String[] ts = not.split(",");
					for (String t : ts) {
						Integer nomer = Integer.valueOf(t.trim());
						if (nomer.equals(defEtap.getNomer())) {
							return "В Следващи етапи не се допуска да се въведе текущия етап.";
						}
						allNextOther.add(nomer);
					}
				}

				String optional = SearchUtils.trimToNULL(defEtap.getNextOptional());
				if (optional != null) { // такива ще се трупат за да не би да са въведени номера за етапи, които ги няма
					if (ok == null) {
						return "За етап с номер " + defEtap.getNomer() + ", не е въведена стойност за 'Следващи етапи (при ДА)'.";
					}
					String[] ts = optional.split(",");
					for (String t : ts) {
						Integer nomer = Integer.valueOf(t.trim());
						if (nomer.equals(defEtap.getNomer())) {
							return "В Следващи етапи не се допуска да се въведе текущия етап.";
						}
						allNextOther.add(nomer);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Въведени са невалидни стойности на следващи етапи.", e);
			return "Въведени са невалидни стойности на следващи етапи.";
		}

		Integer nomer1 = defEtapList.get(0).getNomer(); // от този винаги се започва

		Set<Integer> redundant = new HashSet<>();
		for (ProcDefEtap defEtap : defEtapList) {
			if (nomer1.equals(defEtap.getNomer())) {
				continue;
			}
			if (!allNextOk.contains(defEtap.getNomer()) && !allNextOther.contains(defEtap.getNomer())) {
				redundant.add(defEtap.getNomer());
			}
		}
		if (!redundant.isEmpty()) {
			return "Етапи, които не могат да се активират по време на изпълнение на процедурата: " + redundant;
		}

		allNextOther.removeAll(mapNextOk.keySet());
		if (!allNextOther.isEmpty()) {
			return "Дефинирани номера за следващи етапи, които не съществуват: " + allNextOther;
		}

		List<Integer> path = new ArrayList<>();
		path.add(nomer1);

		String error = null;
		try { // завърта се рекурсивно за да се изчислят всички възможни пътища по ОК, като се проверява дали няма зацикляне или
				// дупка
			validateRecursive(mapNextOk, nomer1, path);

		} catch (InvalidParameterException e) {
			error = e.getMessage();
		}
		return error;
	}

	private boolean isValidReferentData(Integer codeRef, Integer zveno, Integer emplPosition, Integer businessRole) {
		if (codeRef != null) {
			return true;
		}
		if (zveno != null && (emplPosition != null || businessRole != null)) {
			return true;
		}
		if (businessRole != null) {
			return true;
		}
		return false;
	}

	private void validateRecursive(Map<Integer, Set<Integer>> map, Integer nomer, List<Integer> path) throws InvalidParameterException {

		Set<Integer> nextSet = map.get(nomer);

		for (Integer next : nextSet) {
			if (!map.containsKey(next)) {
				throw new InvalidParameterException("За етап с номер=" + nomer + " е дефиниран следващ етап с номер=" //
					+ next + ", който не съществува.");
			}
			if (path.contains(next)) {
				throw new InvalidParameterException("За етап с номер=" + nomer + " е дефиниран следващ етап с номер=" //
					+ next + ", който ще доведе до зацикляне.");
			}

			List<Integer> newPath = new ArrayList<>(path);
			newPath.add(next);

			validateRecursive(map, next, newPath);
		}
	}
}
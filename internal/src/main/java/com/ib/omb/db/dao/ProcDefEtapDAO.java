package com.ib.omb.db.dao;

import static com.ib.system.utils.SearchUtils.trimToNULL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ib.omb.db.dto.ProcDefEtap;
import com.ib.omb.db.dto.ProcDefTask;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link ProcDefEtap}
 *
 * @author belev
 */
public class ProcDefEtapDAO extends AbstractDAO<ProcDefEtap> {

	/** @param user */
	public ProcDefEtapDAO(ActiveUser user) {
		super(ProcDefEtap.class, user);
	}

	/** */
	@Override
	public void delete(ProcDefEtap entity) throws DbErrorException, ObjectInUseException {
		List<ProcDefTask> taskList = selectDefTaskList(entity.getId());
		if (!taskList.isEmpty()) {
			ProcDefTaskDAO taskDao = new ProcDefTaskDAO(getUser());
			for (ProcDefTask task : taskList) {
				taskDao.delete(task);
			}
		}
		super.delete(entity);
	}

	/** */
	@Override
	public ProcDefEtap findById(Object id) throws DbErrorException {
		ProcDefEtap entity = super.findById(id);
		if (entity == null) {
			return entity;
		}

		// на запис ми трябва да знам дали са променяни
		entity.setDbNextOk(entity.getNextOk());
		entity.setDbNextNot(entity.getNextNot());
		entity.setDbNextOptional(entity.getNextOptional());

		return entity;
	}

	/**
	 * @param id
	 * @param byDefinition при <code>true</code> се зареждат данни, който ще се покажат на Бутон <Дефиниция>
	 * @return
	 * @throws DbErrorException
	 */
	public ProcDefEtap findById(Object id, boolean byDefinition) throws DbErrorException {
		ProcDefEtap entity = findById(id);
		if (entity == null) {
			return entity;
		}

		if (byDefinition) { // иска се да се заредят данни, който ще се покажат на Бутон <Дефиниция>
			entity.setNextOkList(findNextList(entity.getDefId(), entity.getNextOk()));
			entity.setNextNotList(findNextList(entity.getDefId(), entity.getNextNot()));
			entity.setNextOptionalList(findNextList(entity.getDefId(), entity.getNextOptional()));
		}
		return entity;
	}

	/**
	 * Проверка за номер
	 *
	 * @param defId
	 * @param nomer
	 * @return
	 * @throws DbErrorException
	 */
	public boolean isEtapNomerExist(Integer defId, Integer nomer) throws DbErrorException {
		try {
			List<?> list = createNativeQuery("select DEF_ETAP_ID from PROC_DEF_ETAP where DEF_ID = ?1 and NOMER = ?2") //
				.setParameter(1, defId).setParameter(2, nomer).getResultList();

			return !list.isEmpty();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при проверка номер на етап!", e);
		}
	}

	/**
	 *
	 */
	@Override
	public ProcDefEtap save(ProcDefEtap entity) throws DbErrorException {

		Map<Integer, ProcDefTask> defTaskMap = new HashMap<>();

		if (trimToNULL(entity.getDbNextOk()) != null && trimToNULL(entity.getNextOk()) == null) { // зачистени са
			resetDefTaskOpinionLists(entity.getId(), 1, defTaskMap); // PROC_DEF_OPINION.OPTYPE=1
		}
		if (trimToNULL(entity.getDbNextNot()) != null && trimToNULL(entity.getNextNot()) == null) { // зачистени са
			resetDefTaskOpinionLists(entity.getId(), 2, defTaskMap); // PROC_DEF_OPINION.OPTYPE=2
		}
		if (trimToNULL(entity.getDbNextOptional()) != null && trimToNULL(entity.getNextOptional()) == null) { // зачистени са
			resetDefTaskOpinionLists(entity.getId(), 3, defTaskMap); // PROC_DEF_OPINION.OPTYPE=3
		}
		if (!defTaskMap.isEmpty()) {
			ProcDefTaskDAO defTaskDao = new ProcDefTaskDAO(getUser());

			boolean resetAll = trimToNULL(entity.getNextNot()) == null && trimToNULL(entity.getNextOptional()) == null;
			for (ProcDefTask defTask : defTaskMap.values()) {
				if (resetAll) { // трябва да се зачисти и по ДА
					defTask.setOpinionOkList(new ArrayList<>());
				}
				defTaskDao.save(defTask);
			}
		}

		ProcDefEtap saved = super.save(entity);

		saved.setDbNextOk(saved.getNextOk());
		saved.setDbNextNot(saved.getNextNot());
		saved.setDbNextOptional(saved.getNextOptional());

		return saved;
	}

	/**
	 * Връща всички дефиниции на задачи за етапа
	 *
	 * @param defEtapId
	 * @return
	 * @throws DbErrorException
	 */
	public List<ProcDefTask> selectDefTaskList(Integer defEtapId) throws DbErrorException {
		try {
			@SuppressWarnings("unchecked")
			List<ProcDefTask> list = createQuery("select t from ProcDefTask t where t.defEtapId = ?1 order by t.id") //
				.setParameter(1, defEtapId).getResultList();
			return list;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на дефиниции на задачи за етап!", e);
		}
	}

	/**
	 * Връща всички дефиниции на задачи за етапа
	 *
	 * @param defId
	 * @param etapNomer
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	List<ProcDefTask> selectDefTaskList(Integer defId, Integer etapNomer) throws DbErrorException {

		List<Object> defEtapIdList;
		try { // първо трябва да идентифицирам етапа
			defEtapIdList = createNativeQuery("select DEF_ETAP_ID from PROC_DEF_ETAP where DEF_ID = ?1 and NOMER = ?2") //
				.setParameter(1, defId).setParameter(2, etapNomer).getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на ИД на етап!", e);
		}

		if (defEtapIdList.isEmpty()) {
			return new ArrayList<>();
		}
		return selectDefTaskList(((Number) defEtapIdList.get(0)).intValue());
	}

	/**
	 * Връща списък от следващи етапи зарадени като [0]-id; [1]-nomer; [2]-etapName
	 *
	 * @param defId
	 * @param next
	 * @return
	 * @throws DbErrorException
	 */
	private List<Object[]> findNextList(Integer defId, String next) throws DbErrorException {
		next = SearchUtils.trimToNULL(next);
		if (next == null) {
			return new ArrayList<>();
		}
		try {
			@SuppressWarnings("unchecked")
			List<Object[]> nextList = createNativeQuery( //
				"select DEF_ETAP_ID, NOMER, ETAP_NAME from PROC_DEF_ETAP where DEF_ID = ?1 and NOMER in (" + next + ")") //
					.setParameter(1, defId).getResultList();

			return nextList;
		} catch (Exception e) {
			throw new DbErrorException("Грека при търсене на следващи етапи!", e);
		}
	}

	/**
	 * @param defEtapId
	 * @param optype
	 * @param defTaskMap
	 * @throws DbErrorException
	 */
	private void resetDefTaskOpinionLists(Integer defEtapId, int optype, Map<Integer, ProcDefTask> defTaskMap) throws DbErrorException {
		@SuppressWarnings("unchecked")
		List<Object> list = createNativeQuery( //
			"select distinct t.DEF_TASK_ID from PROC_DEF_TASK t" //
				+ " inner join PROC_DEF_OPINION o on o.TASK_TYPE = t.TASK_TYPE and o.DEF_ETAP_ID = t.DEF_ETAP_ID" //
				+ " where t.DEF_ETAP_ID = ?1 and o.OPTYPE = ?2") //
					.setParameter(1, defEtapId).setParameter(2, optype).getResultList();

		if (list.isEmpty()) {
			return;
		}
		ProcDefTaskDAO defTaskDao = new ProcDefTaskDAO(getUser());

		for (Object t : list) {
			int id = ((Number) t).intValue();

			ProcDefTask defTask = defTaskMap.get(id);
			if (defTask == null) {
				defTask = defTaskDao.findById(id);
				JPA.getUtil().getEntityManager().detach(defTask);

				defTaskMap.put(id, defTask);
			}
			if (optype == 1) {
				defTask.setOpinionOkList(new ArrayList<>());
			} else if (optype == 2) {
				defTask.setOpinionNotList(new ArrayList<>());
			} else {
				defTask.setOpinionOptionalList(new ArrayList<>());
			}
		}
	}
}
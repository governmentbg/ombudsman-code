package com.ib.omb.db.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dto.ProcDefTask;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;

/**
 * DAO for {@link ProcDefTask}
 *
 * @author belev
 */
public class ProcDefTaskDAO extends AbstractDAO<ProcDefTask> {

	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcDefTaskDAO.class);

	/** @param user */
	public ProcDefTaskDAO(ActiveUser user) {
		super(ProcDefTask.class, user);
	}

	/** */
	@Override
	public void delete(ProcDefTask entity) throws DbErrorException, ObjectInUseException {
		try { // ако за етапа няма друга задача от този вид трябва да се изтрият и мненията

			int del = createNativeQuery("delete from PROC_DEF_OPINION where DEF_ETAP_ID = ?1 and not exists" //
				+ " (select DEF_TASK_ID from PROC_DEF_TASK where DEF_ETAP_ID = ?2 and DEF_TASK_ID != ?3 and TASK_TYPE = ?4)" //
				+ " and TASK_TYPE = ?5") //
					.setParameter(1, entity.getDefEtapId()).setParameter(2, entity.getDefEtapId()) //
					.setParameter(3, entity.getId()).setParameter(4, entity.getTaskType()).setParameter(5, entity.getTaskType()) //
					.executeUpdate();
			LOGGER.debug("ProcDefTask.delete->{} opinions deleted for defEtapId={}, taskType={}.", del, entity.getDefEtapId(), entity.getTaskType());

		} catch (Exception e) {
			throw new DbErrorException("Грешка при изтриване на мнения по вид задача.", e);
		}
		super.delete(entity);
	}

	/** */
	@Override
	public ProcDefTask findById(Object id) throws DbErrorException {
		ProcDefTask entity = super.findById(id);

		if (entity != null) {
			setOpinionLists(entity);
			entity.setDbTaskType(entity.getTaskType());
		}
		return entity;
	}

	/** */
	@Override
	public ProcDefTask save(ProcDefTask entity) throws DbErrorException {
		ProcDefTask merged = entity;
		if (entity.getId() != null) { // така при merge ще влезне в журнала каквото трябва
			merged = merge(entity);

			merged.setOpinionOkList(entity.getOpinionOkList());
			merged.setOpinionNotList(entity.getOpinionNotList());
			merged.setOpinionOptionalList(entity.getOpinionOptionalList());
		}

		try { // трябва да се запишат и мнения по вид задача

			if (entity.getDbTaskType() != null && !entity.getDbTaskType().equals(entity.getTaskType())) {
				// има смяна на вид задача и трябва да зачистя мненията ако номя друга задача от стария вид

				int del = createNativeQuery("delete from PROC_DEF_OPINION where DEF_ETAP_ID = :defEtapId and TASK_TYPE = :oldTaskType and not exists" //
					+ " (select DEF_TASK_ID from PROC_DEF_TASK where DEF_ETAP_ID = :defEtapId and DEF_TASK_ID != :defTaskId and TASK_TYPE = :oldTaskType)") //
						.setParameter("defEtapId", entity.getDefEtapId()).setParameter("oldTaskType", entity.getDbTaskType()) //
						.setParameter("defTaskId", entity.getId()) //
						.executeUpdate();
				LOGGER.debug("ProcDefTask.save->{} opinions deleted for defEtapId={}, taskType={}.", del, entity.getDefEtapId(), entity.getDbTaskType());
			}

			// за текущия тип правим DELETE
			int del = createNativeQuery("delete from PROC_DEF_OPINION where DEF_ETAP_ID = ?1 and TASK_TYPE = ?2") //
				.setParameter(1, entity.getDefEtapId()).setParameter(2, entity.getTaskType()).executeUpdate();
			LOGGER.debug("ProcDefTask.save->{} opinions deleted for defEtapId={}, taskType={}.", del, entity.getDefEtapId(), entity.getTaskType());

			// INSERT
			Query query = createNativeQuery( //
				"INSERT INTO PROC_DEF_OPINION (DEF_ETAP_ID, TASK_TYPE, OPINION, OPTYPE) VALUES(?1, ?2, ?3, ?4)");

			if (entity.getOpinionOkList() != null && !entity.getOpinionOkList().isEmpty()) {
				for (Integer opinion : entity.getOpinionOkList()) {
					query.setParameter(1, entity.getDefEtapId()).setParameter(2, entity.getTaskType()) //
						.setParameter(3, opinion).setParameter(4, 1) //
						.executeUpdate();
				}
			}
			if (entity.getOpinionNotList() != null && !entity.getOpinionNotList().isEmpty()) {
				for (Integer opinion : entity.getOpinionNotList()) {
					query.setParameter(1, entity.getDefEtapId()).setParameter(2, entity.getTaskType()) //
						.setParameter(3, opinion).setParameter(4, 2) //
						.executeUpdate();
				}
			}
			if (entity.getOpinionOptionalList() != null && !entity.getOpinionOptionalList().isEmpty()) {
				for (Integer opinion : entity.getOpinionOptionalList()) {
					query.setParameter(1, entity.getDefEtapId()).setParameter(2, entity.getTaskType()) //
						.setParameter(3, opinion).setParameter(4, 3) //
						.executeUpdate();
				}
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при запис на мнения по вид задача.", e);
		}

		merged.setDbTaskType(merged.getTaskType());

		return super.save(merged);
	}

	/**
	 * Разпределя списъците на мнения за приключване на задача в обекта
	 *
	 * @param entity
	 * @throws DbErrorException
	 */
	public void setOpinionLists(ProcDefTask entity) throws DbErrorException {
		entity.setOpinionOkList(new ArrayList<>());
		entity.setOpinionNotList(new ArrayList<>());
		entity.setOpinionOptionalList(new ArrayList<>());

		if (entity.getTaskType() == null) {
			return;
		}
		try {
			@SuppressWarnings("unchecked")
			List<Object[]> opnionList = createNativeQuery( //
				"select distinct OPINION, OPTYPE from PROC_DEF_OPINION where DEF_ETAP_ID = ?1 and TASK_TYPE = ?2") //
					.setParameter(1, entity.getDefEtapId()).setParameter(2, entity.getTaskType()).getResultList();

			for (Object[] opinion : opnionList) {
				int optype = ((Number) opinion[1]).intValue();
				if (optype == 1) {
					entity.getOpinionOkList().add(((Number) opinion[0]).intValue());
				} else if (optype == 2) {
					entity.getOpinionNotList().add(((Number) opinion[0]).intValue());
				} else {
					entity.getOpinionOptionalList().add(((Number) opinion[0]).intValue());
				}
			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при извличане на мнения по вид задача.", e);
		}
	}
}

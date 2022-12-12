package com.ib.omb.db.dao;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_TASK_STATUS;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_TASK_STATUS_ACTIVE;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_TASK_VID;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AGREED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_SIGNED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ETAP_STAT_DECISION;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ETAP_STAT_EXE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_EXEC_CHANGE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_NASOCH;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_NASOCH_CHANGE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_NASOCH_NEW;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_PODPIS;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_SAGL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_ASIGN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_COMPLETE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_DATA_CHANGE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_EXEC_CHANGE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_RETURN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_ALL_DOST;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_CONTR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_IZP;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_VAZLOJITEL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_15;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_OPINION_PODPIS;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_OPINION_PODPIS_MNENIE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_ASSIGN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_CONTROL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_STATUS_AUTO_PRIKL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_STATUS_INSTRUCTIONS;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP_SROK;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEPRIETA;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_STATUS_SNETA;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_TYPE_PODPIS;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_TYPE_REZOL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_TASK_TYPE_SAGL;
import static com.ib.system.SysConstants.CODE_DEIN_KOREKCIA;
import static com.ib.system.SysConstants.CODE_DEIN_ZAPIS;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_DA;
import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.system.utils.SearchUtils.trimToNULL;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.ParameterMode;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocDoc;
import com.ib.omb.db.dto.DocReferent;
import com.ib.omb.db.dto.Task;
import com.ib.omb.db.dto.TaskReferent;
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
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.PersistentEntity;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.JAXBHelper;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link Task}
 *
 * @author belev
 */
public class TaskDAO extends AbstractDAO<Task> {

	/** DAO for {@link TaskReferent} */
	static class TaskReferentDAO extends AbstractDAO<TaskReferent> {

		/** @param user */
		TaskReferentDAO(ActiveUser user) {
			super(TaskReferent.class, user);
		}
	}

	/** Това ще даде възможност да се генерира номер в отделна транзакция  
	 * @author belev */
	private class GenTransact extends Thread {
		private Task task;
		Exception ex; // и като е в отделна нишка и гръмне няма как да знам и за това тука ще се пази грешката ако е има
		 
		/** @param task */
		GenTransact(Task task) {
			this.task = task;
		}

		@Override
		public void run() {
			try {
				JPA.getUtil().begin();
				
				genRnTask(this.task);
				
				JPA.getUtil().commit();
				
			} catch (Exception e) {
				JPA.getUtil().rollback();
				this.ex = e;

			} finally {
				JPA.getUtil().closeConnection(); // това си е в отделна нишка и задължително трябва да си се затвори само
			}
		}
	}
	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskDAO.class);

	/** @param user */
	public TaskDAO(ActiveUser user) {
		super(Task.class, user);
	}

	/** Изписват се валидации преди реално да се извика изтриванто. Ако не е позволено да се трие се дава ObjectInUseException */
	@Override
	public void delete(Task entity) throws DbErrorException, ObjectInUseException {
		try {
			Integer cnt = asInteger( // TASK.PARENT_ID
				createNativeQuery("select count (*) as cnt from TASK where PARENT_ID = :taskId") //
					.setParameter("taskId", entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("За задачата има регистрирани подчинени задачи и не може да бъде изтрита!");
			}

			cnt = asInteger( // PROC_EXE_TASK.TASK_ID
				createNativeQuery("select count (*) as cnt from PROC_EXE_TASK where TASK_ID = :taskId") //
					.setParameter("taskId", entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Задача с номер: "+entity.getRnTask()+" е регистрирана по етап на процедура и не може да бъде изтрита!");
			}

		} catch (ObjectInUseException e) {
			throw e; // за да не се преопакова

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на подчинени задачи!", e);
		}

		if (entity.getEndDocId() != null && entity.getDocId() != null) {
			relDocByTask(entity.getEndDocId(), entity.getDocId(), entity.getId(), false); // този вече не е
		}
		
		super.delete(entity);
	}

	/** @see AbstractDAO#findById(Object) */
	@SuppressWarnings("unchecked")
	@Override
	public Task findById(Object id) throws DbErrorException {
		Task task = super.findById(id);
		if (task == null) {
			return task;
		}

		task.setDbEndDocId(task.getEndDocId());

		task.setDbStatus(task.getStatus());
		task.setDbCodeAssign(task.getCodeAssign());
		task.setDbCodeControl(task.getCodeControl());
		task.setHashNotifData(Objects.hash(task.getTaskType(), task.getAssignDate(), task.getSrokDate(), task.getTaskInfo()));

		try {
			Query query = createQuery("select tr.codeRef from TaskReferent tr where tr.taskId = ?1 order by tr.roleRef, tr.id").setParameter(1, task.getId());

			task.setCodeExecs(query.getResultList());
			task.setDbCodeExecs(new ArrayList<>(task.getCodeExecs()));

			if (task.getEndDocId() != null) {
				List<Object[]> rows = createNativeQuery("select RN_DOC, DOC_DATE, PORED_DELO from DOC where DOC_ID = ?1") //
					.setParameter(1, task.getEndDocId()).getResultList();
				if (rows.size() > 0) {
					Object[] endDoc = rows.get(0);
					task.setRnDocEnd(DocDAO.formRnDoc(endDoc[0], endDoc[2]));
					task.setDateDocEnd((Date) endDoc[1]);
				}
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на изпълнители на задачата!", e);
		}

		return task;
	}

	/**
	 * Дава информация на реалните промени на основните данни по задачата. Дава резултат от вида:<br>
	 * [0]-SYSTEM_JOURNAL.ID<br>
	 * [1]-SYSTEM_JOURNAL.ID_USER<br>
	 * [2]-SYSTEM_JOURNAL.CODE_ACTION<br>
	 * [3]-SYSTEM_JOURNAL.DATE_ACTION<br>
	 * [4]-TASK.ASSIGN_DATE<br>
	 * [5]-TASK.SROK_DATE<br>
	 * [6]-TASK.TASK_INFO<br>
	 * [7]-TASK.възложител (текст)<br>
	 * [8]-TASK.контролиращ (текст)<br>
	 * [9]-TASK.изпълнители (текст)<br>
	 * [10]-TASK.TASK_TYPE<br>
	 * [11]-TASK.DOC_REQUIRED<br>
	 * 
	 * @param taskId
	 * @param sd
	 * @return
	 * @throws DbErrorException
	 */
	public List<Object[]> findTaskEditList(Integer taskId, BaseSystemData sd) throws DbErrorException {
		List<Object[]> edits = new ArrayList<>();
		try {
			Query query = createQuery( //
				"select j from SystemJournal j" //
					+ " where j.codeObject = :codeObject and j.idObject = :idObject and j.objectXml is not null order by j.id") //
						.setParameter("codeObject", OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK).setParameter("idObject", taskId);

			@SuppressWarnings("unchecked")
			List<SystemJournal> rows = query.getResultList();
			if (!rows.isEmpty()) {
				SystemJournal current = rows.get(0);
				Task currentTask = JAXBHelper.xmlToObject(Task.class, current.getObjectXml());

				String assign = currentTask.getCodeAssign() != null //
					? sd.decodeItem(CODE_CLASSIF_ADMIN_STR, currentTask.getCodeAssign(), getUserLang(), current.getDateAction()) //
					: "";
				String control = currentTask.getCodeControl() != null //
					? sd.decodeItem(CODE_CLASSIF_ADMIN_STR, currentTask.getCodeControl(), getUserLang(), current.getDateAction()) //
					: "";
				StringBuilder execs = new StringBuilder();
				for (int c = 0; c < currentTask.getCodeExecs().size(); c++) {
					Integer exec = currentTask.getCodeExecs().get(c);
					execs.append(sd.decodeItem(CODE_CLASSIF_ADMIN_STR, exec, getUserLang(), current.getDateAction()));
					if (c < currentTask.getCodeExecs().size() - 1) {
						execs.append(", ");
					}
				}

				// първият винаги трябва да го има
				edits.add(new Object[] { current.getId(), current.getIdUser(), current.getCodeAction(), current.getDateAction() //
					, currentTask.getAssignDate(), currentTask.getSrokDate(), currentTask.getTaskInfo() //
					, "Възложител: " + assign, "Контролиращ: " + control, "Изпълнители: " + execs //
					, currentTask.getTaskType() });

				boolean setting = Objects.equals(((SystemData)sd).getRegistraturaSetting(currentTask.getRegistraturaId(), CODE_ZNACHENIE_REISTRATURA_SETTINGS_15), CODE_ZNACHENIE_DA);
				SimpleDateFormat sdf = new SimpleDateFormat(setting?"dd.MM.yyyy HH:mm":"dd.MM.yyyy");

				for (int i = 0; i < rows.size() - 1; i++) { // тука се сравнява със следващите и ако има разлика само тогава влиза
															// в резултата
					current = rows.get(i);
					SystemJournal next = rows.get(i + 1);

					currentTask = JAXBHelper.xmlToObject(Task.class, current.getObjectXml());
					Task nextTask = JAXBHelper.xmlToObject(Task.class, next.getObjectXml());

					currentTask.setTaskInfo(trimToNULL(currentTask.getTaskInfo()));
					nextTask.setTaskInfo(trimToNULL(nextTask.getTaskInfo()));

					boolean srokDif = false;
					try {
						srokDif = !Objects.equals(sdf.format(currentTask.getSrokDate()), sdf.format(nextTask.getSrokDate()));
					} catch (Exception e) {
						srokDif = currentTask.getSrokDate() != null || nextTask.getSrokDate() != null;
					}

					if (!Objects.equals(sdf.format(currentTask.getAssignDate()), sdf.format(nextTask.getAssignDate())) //
						|| srokDif
						|| !Objects.equals(currentTask.getTaskInfo(), nextTask.getTaskInfo()) //
						|| !Objects.equals(currentTask.getCodeAssign(), nextTask.getCodeAssign()) //
						|| !Objects.equals(currentTask.getCodeControl(), nextTask.getCodeControl()) //
						|| !Objects.equals(currentTask.getCodeExecs(), nextTask.getCodeExecs()) //
						|| !Objects.equals(currentTask.getDocRequired(), nextTask.getDocRequired()) //
						|| !Objects.equals(currentTask.getTaskType(), nextTask.getTaskType())) {

						assign = nextTask.getCodeAssign() != null //
							? sd.decodeItem(CODE_CLASSIF_ADMIN_STR, nextTask.getCodeAssign(), getUserLang(), current.getDateAction()) //
							: "";
						control = nextTask.getCodeControl() != null //
							? sd.decodeItem(CODE_CLASSIF_ADMIN_STR, nextTask.getCodeControl(), getUserLang(), current.getDateAction()) //
							: "";
						execs = new StringBuilder();
						for (int c = 0; c < nextTask.getCodeExecs().size(); c++) {
							Integer exec = nextTask.getCodeExecs().get(c);
							execs.append(sd.decodeItem(CODE_CLASSIF_ADMIN_STR, exec, getUserLang(), current.getDateAction()));
							if (c < nextTask.getCodeExecs().size() - 1) {
								execs.append(", ");
							}
						}

						edits.add(new Object[] { next.getId(), next.getIdUser(), next.getCodeAction(), next.getDateAction() //
							, nextTask.getAssignDate(), nextTask.getSrokDate(), nextTask.getTaskInfo() //
							, "Възложител: " + assign, "Контролиращ: " + control, "Изпълнители: " + execs //
							, nextTask.getTaskType(), nextTask.getDocRequired() });
					}
				}
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на промени по задачата!", e);
		}
		return edits;
	}

	/**
	 * Дава информация за промени по статус на задачата. Дава резултат от вида:<br>
	 * [0]-TASK_STATES.ID<br>
	 * [1]-STATUS<br>
	 * [2]-STATUS_USER_ID<br>
	 * [3]-STATUS_COMMENTS<br>
	 * [4]-STATUS_DATE<br>
	 * [5]-END_DOC_ID<br>
	 * [6]-END_OPINION<br>
	 *
	 * @param taskId
	 * @return
	 * @throws DbErrorException
	 */
	public List<Object[]> findTaskStatesList(Integer taskId) throws DbErrorException {
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select ts.ID a0, ts.STATUS a1, ts.STATUS_USER_ID a2, ts.STATUS_COMMENTS a3, ts.STATUS_DATE a4, ts.END_DOC_ID a5, ts.END_OPINION a6 ");
			sql.append(" from TASK_STATES ts where ts.TASK_ID = :taskId ");
			sql.append(" union ");
			sql.append(" select null, t.STATUS, t.STATUS_USER_ID, t.STATUS_COMMENTS, t.STATUS_DATE, t.END_DOC_ID, t.END_OPINION ");
			sql.append(" from TASK t where t.TASK_ID = :taskId ");
			sql.append(" order by a4 ");

			@SuppressWarnings("unchecked")
			List<Object[]> states = createNativeQuery(sql.toString()).setParameter("taskId", taskId).getResultList();

			return states;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на промени по статус на задачата!", e);
		}
	}

	/**
	 * Дава информация на ролите в които текущия потребител се намира в задачата. За регистратор в задача се изпозлва роля=0
	 *
	 * @param task
	 * @return
	 */
	public Set<Integer> findUserRolesInTask(Task task) {
		UserData ud = (UserData) getUser(); // заради разните му замествания и делегиране на права

		Set<Integer> roles = new HashSet<>();
		if (Objects.equals(task.getCodeAssign(), ud.getUserAccess())) {
			roles.add(CODE_ZNACHENIE_TASK_REF_ROLE_ASSIGN);
		}
		if (Objects.equals(task.getCodeControl(), ud.getUserAccess())) {
			roles.add(CODE_ZNACHENIE_TASK_REF_ROLE_CONTROL);
		}
		if (Objects.equals(task.getUserReg(), ud.getUserId())) {
			roles.add(0); // това е ролята за регистратор в задачата
		}

		if (task.getCodeExecs() == null || task.getCodeExecs().isEmpty()) {
			return roles;
		}

		int index = task.getCodeExecs().indexOf(ud.getUserAccess());
		if (index == 0) {
			roles.add(CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG);
		} else if (index > 0) {
			roles.add(CODE_ZNACHENIE_TASK_REF_ROLE_EXEC);
		}

		return roles;
	}

	/**
	 * Прави анализ и създава нотификациите за изтриване на задача.
	 *
	 * @param task
	 * @param doc
	 * @param systemData
	 * @throws DbErrorException
	 */
	public void notifTaskDelete(Task task, Doc doc, SystemData systemData) throws DbErrorException {
		int taskType = task.getTaskType().intValue();

		if (task.getDocId() != null //
			&& (taskType == CODE_ZNACHENIE_TASK_TYPE_REZOL || taskType == CODE_ZNACHENIE_TASK_TYPE_SAGL || taskType == CODE_ZNACHENIE_TASK_TYPE_PODPIS)) {

			if (doc == null || doc.getId() == null) { // трябва да се изтегли
				doc = getEntityManager().find(Doc.class, task.getDocId());
			}

			Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
				, CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_EXEC_CHANGE, CODE_ZNACHENIE_NOTIF_ROLIA_IZP, systemData);
			notif.setDoc(doc);
			notif.setTask(task);
			notif.setAdresati(task.getCodeExecs());
			notif.send();

		} else {

			boolean activeStatus = systemData.matchClassifItems(CODE_CLASSIF_TASK_STATUS_ACTIVE, task.getStatus(), task.getStatusDate());
			if (activeStatus) {
				Notification fake = new Notification(systemData);

				fake.generatеFakeNotif(OmbConstants.CODE_FAKE_NOTIF_RELOAD_TASK);

				fake.getAdresati().addAll(task.getCodeExecs());
				fake.getAdresati().add(task.getCodeAssign());
				if (task.getCodeControl() != null) {
					fake.getAdresati().add(task.getCodeControl());
				}

				fake.send();
			}

			Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
				, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_EXEC_CHANGE, CODE_ZNACHENIE_NOTIF_ROLIA_IZP, systemData);
			notif.setTask(task);
			notif.setAdresati(task.getCodeExecs());
			notif.send();
		}
	}

	/**
	 * Прави анализ и създава нотификациите за задачи и документи в задачите. В момента се вика в ДАО-то накрая на записа. Ако в
	 * бъдеще се реши че трябва да се вика в бийна след записа някой трябва да се погржи да има необходимите параметри.
	 *
	 * @param task
	 * @param doc
	 * @param oldStatus
	 * @param oldCodeControl
	 * @param oldHashNotifData
	 * @param systemData
	 * @param deleteCodeExecs
	 * @throws DbErrorException
	 */
	public void notifTaskSave(Task task, Doc doc, Integer oldStatus, Integer oldCodeControl, Integer oldHashNotifData, List<Integer> deleteCodeExecs, SystemData systemData) throws DbErrorException {
		boolean statusChange = oldStatus == null || !oldStatus.equals(task.getStatus());

		int taskType = task.getTaskType().intValue();
		int taskStatus = task.getStatus().intValue();

		Set<Integer> fakeTo = new TreeSet<>();

		if (task.getDocId() != null //
			&& (taskType == CODE_ZNACHENIE_TASK_TYPE_REZOL || taskType == CODE_ZNACHENIE_TASK_TYPE_SAGL || taskType == CODE_ZNACHENIE_TASK_TYPE_PODPIS)) {

			if (task.getNewCodeExecs() != null && !task.getNewCodeExecs().isEmpty()) {

				if (doc == null || doc.getId() == null) { // трябва да се изтегли
					doc = getEntityManager().find(Doc.class, task.getDocId());
				}

				Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
					, CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_NASOCH, CODE_ZNACHENIE_NOTIF_ROLIA_IZP, systemData);
				notif.setDoc(doc);
				notif.setTask(task);
				notif.setAdresati(task.getNewCodeExecs());
				notif.send();
			}

			if (deleteCodeExecs != null && !deleteCodeExecs.isEmpty()) {

				if (doc == null || doc.getId() == null) { // трябва да се изтегли
					doc = getEntityManager().find(Doc.class, task.getDocId());
				}

				Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
					, CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_EXEC_CHANGE, CODE_ZNACHENIE_NOTIF_ROLIA_IZP, systemData);
				notif.setDoc(doc);
				notif.setTask(task);
				notif.setAdresati(deleteCodeExecs);
				notif.send();
			}

			if (statusChange && oldStatus != null) {

				Integer event = null;
				if (taskType == CODE_ZNACHENIE_TASK_TYPE_SAGL //
					&& (taskStatus == CODE_ZNACHENIE_TASK_STATUS_IZP || taskStatus == CODE_ZNACHENIE_TASK_STATUS_IZP_SROK)) {

					event = CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_SAGL;

				} else if (taskType == CODE_ZNACHENIE_TASK_TYPE_PODPIS //
					&& (taskStatus == CODE_ZNACHENIE_TASK_STATUS_IZP || taskStatus == CODE_ZNACHENIE_TASK_STATUS_IZP_SROK)) {

					event = CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_PODPIS;

				} else if (taskType == CODE_ZNACHENIE_TASK_TYPE_REZOL //
					&& (taskStatus == CODE_ZNACHENIE_TASK_STATUS_IZP || taskStatus == CODE_ZNACHENIE_TASK_STATUS_IZP_SROK)) {

					fakeTo.addAll(task.getCodeExecs());
				}

				if (event != null) {

					Set<Integer> adresati = new HashSet<>();

					if (doc == null || doc.getId() == null) { // трябва да се изтегли
						doc = getEntityManager().find(Doc.class, task.getDocId());

						@SuppressWarnings("unchecked") // теглим и авторите
						List<Integer> authors = createQuery("select dr.codeRef from DocReferent dr where dr.docId = ?1 and dr.roleRef = ?2")
							.setParameter(1, task.getDocId()).setParameter(2, CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR).getResultList();
						adresati.addAll(authors);
						
					} else { // документа вече е зареден и авторите са налични в него
						if (doc.getReferentsAuthor() != null && !doc.getReferentsAuthor().isEmpty()) {
							for (DocReferent dr : doc.getReferentsAuthor()) {
								adresati.add(dr.getCodeRef());
							}
						}
					}

					Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null, event, CODE_ZNACHENIE_NOTIF_ROLIA_VAZLOJITEL, systemData);
					notif.setDoc(doc);
					notif.setTask(task);
					adresati.add(task.getCodeAssign()); // възложител

					// + до всички изпълнители на задачи от съответния тип
					@SuppressWarnings("unchecked")
					List<Object> others = createNativeQuery(
						"select r.CODE_REF from TASK t inner join TASK_REFERENTS r on r.TASK_ID = t.TASK_ID where t.TASK_ID != ?1 and t.DOC_ID = ?2 and t.TASK_TYPE = ?3")
						.setParameter(1, task.getId()).setParameter(2, task.getDocId()).setParameter(3, taskType).getResultList();
					if (!others.isEmpty()) {
						for (Object other : others) {
							adresati.add(((Number) other).intValue());
						}
					}

					notif.getAdresati().addAll(adresati);
					notif.send();

					fakeTo.addAll(task.getCodeExecs());
				}

				event = null;
				if (taskStatus == CODE_ZNACHENIE_TASK_STATUS_NEPRIETA) {

					event = CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_NASOCH_NEW;

				} else if (taskStatus == CODE_ZNACHENIE_TASK_STATUS_SNETA || taskStatus == CODE_ZNACHENIE_TASK_STATUS_AUTO_PRIKL) {

					event = CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_TASK_NASOCH_CHANGE;
				}
				if (event != null) {

					if (doc == null || doc.getId() == null) { // трябва да се изтегли
						doc = getEntityManager().find(Doc.class, task.getDocId());
					}

					Notification notif = new Notification(((UserData)getUser()).getUserAccess(), null, event, CODE_ZNACHENIE_NOTIF_ROLIA_IZP, systemData);
					notif.setDoc(doc);
					notif.setTask(task);
					notif.setAdresati(task.getCodeExecs());
					notif.send();
				}
			}

		} else { // ДВЗ -друг вид задача (по спецификация)

			boolean activeStatus = systemData.matchClassifItems(CODE_CLASSIF_TASK_STATUS_ACTIVE, task.getStatus(), task.getStatusDate());

			if (activeStatus) {
				boolean newExecs = task.getNewCodeExecs() != null && !task.getNewCodeExecs().isEmpty();

				if (oldStatus == null) { // нова задача
					fakeTo.add(task.getCodeAssign());
				}

				if (newExecs) {

					Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
						, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_ASIGN, CODE_ZNACHENIE_NOTIF_ROLIA_IZP, systemData);
					notif.setTask(task);
					notif.setAdresati(task.getNewCodeExecs());
					notif.send();
				}

				if (deleteCodeExecs != null && !deleteCodeExecs.isEmpty()) {

					Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
						, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_EXEC_CHANGE, CODE_ZNACHENIE_NOTIF_ROLIA_IZP, systemData);
					notif.setTask(task);
					notif.setAdresati(deleteCodeExecs);
					notif.send();
				}

				if (oldCodeControl == null) {
					if (task.getCodeControl() != null) { // нов контролиращ

						Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
							, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_ASIGN, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, systemData);
						notif.setTask(task);
						notif.setAdresat(task.getCodeControl());
						notif.send();
					}
				} else if (!oldCodeControl.equals(task.getCodeControl())) { // разлика в контролиращите

					Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
						, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_EXEC_CHANGE, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, systemData);
					notif.setTask(task);
					notif.setAdresat(oldCodeControl);
					notif.send();

					if (task.getCodeControl() != null) { // нов контролиращ

						Notification notif2 = new Notification(((UserData)getUser()).getUserAccess() , null
							, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_ASIGN, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, systemData);
						notif2.setTask(task);
						notif2.setAdresat(task.getCodeControl());
						notif2.send();
					}
				}

				if (oldHashNotifData != null && !oldHashNotifData.equals(task.getHashNotifData())) {
					List<Integer> temp;
					if (newExecs) { // има нови и се праща само на старите
						temp = new ArrayList<>(task.getCodeExecs());
						temp.removeAll(task.getNewCodeExecs());

					} else { // няма нови и се праща на всички
						temp = task.getCodeExecs();
					}

					if (!temp.isEmpty()) { // току виж няма никой останал стар

						Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
							, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_DATA_CHANGE, CODE_ZNACHENIE_NOTIF_ROLIA_IZP, systemData);
						notif.setTask(task);
						notif.setAdresati(temp);
						notif.send();
					}
				}

				if (statusChange && oldStatus != null //
					&& !systemData.matchClassifItems(CODE_CLASSIF_TASK_STATUS_ACTIVE, oldStatus, task.getStatusDate())) {

					Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
						, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_RETURN, CODE_ZNACHENIE_NOTIF_ROLIA_IZP, systemData);
					notif.setTask(task);
					notif.setAdresati(task.getCodeExecs());
					notif.send();

					fakeTo.add(task.getCodeAssign());
					if (task.getCodeControl() != null) {
						fakeTo.add(task.getCodeControl());
					}
				}
			}

			if (statusChange) {
				if (taskStatus == CODE_ZNACHENIE_TASK_STATUS_IZP || taskStatus == CODE_ZNACHENIE_TASK_STATUS_IZP_SROK) {
					fakeTo.addAll(task.getCodeExecs());

					Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
						, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_COMPLETE, CODE_ZNACHENIE_NOTIF_ROLIA_ALL_DOST, systemData);
					notif.setTask(task);
					notif.setAdresat(task.getCodeAssign());
					notif.send();

					if (task.getCodeControl() != null) {
						Notification notif2 = new Notification(((UserData)getUser()).getUserAccess() , null
							, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_COMPLETE, CODE_ZNACHENIE_NOTIF_ROLIA_ALL_DOST, systemData);
						notif2.setTask(task);
						notif2.setAdresat(task.getCodeControl());
						notif2.send();
					}
				}

				if (taskStatus == CODE_ZNACHENIE_TASK_STATUS_SNETA || taskStatus == CODE_ZNACHENIE_TASK_STATUS_AUTO_PRIKL) {
					fakeTo.add(task.getCodeAssign());
					if (task.getCodeControl() != null) {
						fakeTo.add(task.getCodeControl());
					}

					Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
						, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_COMPLETE, CODE_ZNACHENIE_NOTIF_ROLIA_ALL_DOST, systemData);
					notif.setTask(task);
					notif.setAdresati(task.getCodeExecs());
					notif.send();
				}

//				if (taskStatus == CODE_ZNACHENIE_TASK_STATUS_INSTRUCTIONS) {
//					Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
//						, CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_INSTRUCTIONS, CODE_ZNACHENIE_NOTIF_ROLIA_VAZLOJITEL, systemData);
//					notif.setTask(task);
//					notif.getAdresati().add(task.getCodeAssign());
//					if (task.getCodeControl() != null) {
//						notif.getAdresati().add(task.getCodeControl());
//					}
//					notif.send();
//				}
			}
		}

		if (!fakeTo.isEmpty()) {
			Notification fake = new Notification(systemData);
			fake.generatеFakeNotif(OmbConstants.CODE_FAKE_NOTIF_RELOAD_TASK);
			fake.getAdresati().addAll(fakeTo);
			fake.send();
		}
	}

	/**
	 * Изпълняване на задача за мобилното.
	 *
	 * @param taskId
	 * @param newStatus 
	 * @param statusComments
	 * @param realEnd 
	 * @param endOpinion 
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 */
	public Task restStatusTaskChange(Integer taskId, Integer newStatus, String statusComments
		, Date realEnd, Integer endOpinion, SystemData systemData) throws DbErrorException {

		if (taskId == null || newStatus == null) {
			LOGGER.error("restStatusTaskChange-> (taskId == null || newStatus == null)");
			return null;
		}
		Task task = findById(taskId);
		if (task == null) {
			throw new IllegalArgumentException("Task with ID=" + taskId + " not found!");
		}
		task.setStatus(newStatus);
		task.setStatusComments(statusComments);
		
		if (realEnd != null) {
			task.setRealEnd(realEnd);
		}
		if (endOpinion != null) {
			task.setEndOpinion(endOpinion);
		}

		// ако така насестваните стойности са неконсистентни се налага лека промяна
		if (newStatus.equals(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP) && task.getRealEnd() == null) {
			task.setRealEnd(new Date());
		}
		// ... може да се налагат и други
		
		task = save(task, null, systemData);
		return task;
	}

	/**
	 * Зарежда данни за конкретна задача
	 *
	 * @param taskId
	 * @param sd
	 * @return
	 * @throws DbErrorException
	 */
	public Map<String, Object> restFindById(Integer taskId, BaseSystemData sd) throws DbErrorException {
		Map<String, Object> task = new LinkedHashMap<>();

		if (taskId == null) {
			LOGGER.error("restLoadTaskById->taskId=NULL!");
			return task;
		}

		try {
			String dialect = JPA.getUtil().getDbVendorName();

			StringBuilder sql = new StringBuilder();
			sql.append(" select t.TASK_ID, t.RN_TASK, t.TASK_TYPE, t.STATUS, t.SROK_DATE, t.ASSIGN_DATE, t.STATUS_COMMENTS, ");
			sql.append(DialectConstructor.convertToDelimitedString(dialect, "e.CODE_REF", "TASK_REFERENTS e where e.TASK_ID = t.TASK_ID", "e.ROLE_REF, e.ID"));
			sql.append(" , t.DOC_ID, t.CODE_ASSIGN, t.CODE_CONTROL, t.TASK_INFO ");
			
			sql.append(" from TASK t where t.TASK_ID = :taskId ");

			Query query = createNativeQuery(sql.toString());
			query.setParameter("taskId", taskId);

			@SuppressWarnings("unchecked")
			List<Object[]> rows = query.getResultList();
			
			if (rows.isEmpty()) {
				LOGGER.error("restLoadTaskById-> task not found! TASK_ID={}", taskId);
				return task;
			}
			Object[] row = rows.get(0);

			Date assignDate = (Date) row[5]; // ще се разкодира всичко към датата на възлагане

			task.put("TASK_ID", taskId);
			task.put("RN_TASK", row[1]);
			task.put("DOC_ID", asInteger(row[8]));

			Integer taskType = asInteger(row[2]);
			task.put("TASK_TYPE", taskType);
			task.put("TASK_TYPE_TEXT", sd.decodeItem(CODE_CLASSIF_TASK_VID, taskType, getUserLang(), assignDate));

			Integer status = asInteger(row[3]);
			task.put("STATUS", status);
			task.put("STATUS_TEXT", sd.decodeItem(CODE_CLASSIF_TASK_STATUS, status, getUserLang(), assignDate));

			Date srokDate = (Date) row[4];
			task.put("SROK_DATE", srokDate);
			task.put("ASSIGN_DATE", assignDate);
			task.put("COMMENTS", row[6]);

			StringBuilder execList = new StringBuilder();

			Integer execCodeOtg = null;
			String codesString = trimToNULL((String) row[7]);
			if (codesString != null) {
				String[] codes = codesString.split(",");
				for (int i = 0; i < codes.length; i++) {
					Integer code = Integer.valueOf(codes[i]);
					execList.append(sd.decodeItem(CODE_CLASSIF_ADMIN_STR, code, getUserLang(), assignDate));
					if (i < codes.length - 1) {
						execList.append(", ");
					}
					if (i == 0) { // само ако е първи е отговорен
						execCodeOtg = code;
					}
				}
			}
			task.put("EXEC_LIST", execList.toString());
			task.put("EXEC_CODE_OTG", execCodeOtg); // кода на отговорника

			Integer codeAssign = null;
			String textAssign = null;
			if (row[9] != null) {
				codeAssign = ((Number) row[9]).intValue();
				textAssign = sd.decodeItem(CODE_CLASSIF_ADMIN_STR, codeAssign, getUserLang(), assignDate);
			}
			task.put("ASSIGN_CODE", codeAssign);
			task.put("ASSIGN_TEXT", textAssign);

			Integer codeControl = null;
			String textControl = null;
			if (row[10] != null) {
				codeControl = ((Number) row[10]).intValue();
				textControl = sd.decodeItem(CODE_CLASSIF_ADMIN_STR, codeControl, getUserLang(), assignDate);
			}
			task.put("CONTROL_CODE", codeControl);
			task.put("CONTROL_TEXT", textControl);

			task.put("TASK_INFO", row[11]);

			@SuppressWarnings("unchecked")
			List<Object[]> fileRows = createNativeQuery("select f.FILE_ID, f.FILENAME from FILE_OBJECTS fo"
				+ " inner join FILES f on f.FILE_ID = fo.FILE_ID where fo.OBJECT_ID = ?1 and fo.OBJECT_CODE = ?2 order by f.FILE_ID")
				.setParameter(1, taskId).setParameter(2, OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK).getResultList();
			List<LinkedHashMap<String, Object>> files = new ArrayList<>();
			for (Object[] fileRow : fileRows) {
				LinkedHashMap<String, Object> file = new LinkedHashMap<>();
				file.put("FILE_ID"	, ((Number)fileRow[0]).intValue());
				file.put("FILENAME"	, fileRow[1]);
				
				files.add(file);
			}
			task.put("FILES" , files);

		} catch (DbErrorException e) {
			throw e;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при зареждане на данни за задача!", e);
		}
		return task;
	}

	/**
	 * Дава задачи в определени статуси по подадените аргументи. Сортира ги по {@link Task#getSrokDate()} в нарастващ ред. За
	 * подадения потребител се гледа дали е изпълнител или контролиращ на задача.
	 *
	 * @param userId    - user
	 * @param pageSize  - брой задачи на страница.
	 * @param pageIndex - номера на страница. започват от 0.
	 * @param sd
	 * @return
	 * @throws DbErrorException
	 */
	public List<LinkedHashMap<String, Object>> restFindTasksByUser(Integer userId, Integer pageSize, Integer pageIndex, BaseSystemData sd) throws DbErrorException {
		ArrayList<LinkedHashMap<String, Object>> result = new ArrayList<>();

		if (userId == null) {
			LOGGER.error("restFindTasksByUser->userId=NULL!");
			return result;
		}

		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select t.TASK_ID, t.RN_TASK, t.TASK_TYPE, t.STATUS, t.SROK_DATE, t.ASSIGN_DATE, t.STATUS_COMMENTS, t.DOC_ID, t.TASK_INFO");
			sql.append(" from TASK t ");
			sql.append(" inner join TASK_REFERENTS tr on tr.TASK_ID = t.TASK_ID ");
			sql.append(" where tr.CODE_REF = :userId "); // само където подадения е изпълнител
			sql.append(" and t.STATUS in (1,2,3,4,5)"); // кои са статусите!
			sql.append(" order by t.TASK_ID desc ");

			Query query = createNativeQuery(sql.toString());
			query.setParameter("userId", userId);

			if (pageSize != null && pageIndex != null) { // ако не са подадени ще извади всичко

				// странициране !!!
				query.setFirstResult(pageSize * pageIndex);
				query.setMaxResults(pageSize);
			}

			@SuppressWarnings("unchecked")
			List<Object[]> rows = query.getResultList();
			for (Object[] row : rows) {
				Date assignDate = (Date) row[5];

				LinkedHashMap<String, Object> task = new LinkedHashMap<>();

				task.put("TASK_ID", ((Number)row[0]).intValue());
				task.put("RN_TASK", row[1]);
				task.put("DOC_ID", asInteger(row[7]));

				Integer taskType = asInteger(row[2]);
				task.put("TASK_TYPE", taskType);
				task.put("TASK_TYPE_TEXT", sd.decodeItem(CODE_CLASSIF_TASK_VID, taskType, getUserLang(), assignDate));

				Integer status = asInteger(row[3]);
				task.put("STATUS", status);
				task.put("STATUS_TEXT", sd.decodeItem(CODE_CLASSIF_TASK_STATUS, status, getUserLang(), assignDate));

				task.put("SROK_DATE", row[4]);
				task.put("ASSIGN_DATE", assignDate);
				task.put("COMMENTS", row[6]);
				task.put("TASK_INFO", row[8]);

				result.add(task);
			}

		} catch (DbErrorException e) {
			throw e;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при извличане на задачаи по потребител!", e);
		}

		return result;
	}

	/**
	 * Запис на задачата
	 *
	 * @param task
	 * @param multiplyEmployees при нов запис ако е <code>true</code> записва задача за всеки служител
	 * @param doc               doc ако задачата е по документ и ако го има зареден, иначе NULL
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @see AbstractDAO#save(PersistentEntity)
	 */
	public Task save(Task task, boolean multiplyEmployees, Doc doc, SystemData systemData) throws DbErrorException {
		Task saved = null;

		if (multiplyEmployees && task.getId() == null // само ако се иска и е нов запис на задача
			&& task.getCodeExecs() != null && task.getCodeExecs().size() > 1) { // и да има повече от един изпълнител

			List<Integer> codeExecs = task.getCodeExecs();

			for (Integer code : codeExecs) {

				if (task.getId() != null && JPA.getUtil().getEntityManager().contains(task)) {
					JPA.getUtil().getEntityManager().detach(task);
				}

				task.setId(null);
				task.setCodeExecs(Arrays.asList(code));

				saved = save(task, doc, systemData);
			}

		} else {
			saved = save(task, doc, systemData); // стандартния запис
		}

		return saved;
	}

	/**
	 * Запис на задачата
	 *
	 * @param task
	 * @param doc        ако задачата е по документ и ако го има зареден, иначе NULL
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @see AbstractDAO#save(PersistentEntity)
	 */
	public Task save(Task task, Doc doc, SystemData systemData) throws DbErrorException {

		boolean statusChanged;
		if (task.getId() == null) {

			// при нов запис винаги зачиствам всички помощни полета, защото има ситуации при които се записват няколко задачи на веднъж
			// тогава се използва един и същ обект и става объркване
			task.setDbEndDocId(null);
			task.setDbStatus(null);
			task.setDbCodeAssign(null);
			task.setDbCodeControl(null);
			task.setHashNotifData(null);
			task.setDbCodeExecs(null);
			task.setNewCodeExecs(null);
			
			if (JPA.getUtil().getDbVendorName().indexOf("POSTGRESQL") != -1) {
				
				GenTransact gt;
				try { // ще се генерира в отделна нишка защото в процедурите на постгрето няма вътрешно управление на транзакции
					gt = new GenTransact(task);
					gt.start();
					gt.join();	

				} catch (Exception e) {
					throw new DbErrorException("Системна грешка при генериране на регистров номер на задача ! POSTGRESQL !", e);
				}
				if (gt.ex != null) {
					if (gt.ex instanceof DbErrorException) {
						throw (DbErrorException) gt.ex;
					}
					throw new DbErrorException(gt.ex); // някакво друго чудо е
				}
				
			} else { // тука в БД си се прави бегин комит и т.н.
				genRnTask(task);
			}
			statusChanged = true;

		} else {
			checkTaskInSrok(task, systemData); // как е със срока задачата

			statusChanged = !Objects.equals(task.getStatus(), task.getDbStatus());

			if (task.getProcInfo() != null && task.getProcInfo().length() > 0) {
				processEtapTask(task, statusChanged, systemData);
			}
		}

		if (statusChanged) { // нов статус нова дата
			task.setStatusDate(new Date());
		}

		Integer statusUserId;
		try {
			statusUserId = ((UserData) getUser()).getUserSave();
		} catch (ClassCastException e) { // ако се вика през рест не е сигурно какъв точно усер ще дойде
			statusUserId = getUserId();
		}
		task.setStatusUserId(statusUserId);

		task.setAuditable(false); // за да не запише ред в журнала при записа на обекта

		Map<Integer, Integer> accessMap = null;
		if (task.getDocId() != null || task.getEndDocId() != null || task.getDbEndDocId() != null) { // само при тези се гради
																										// достъп
			accessMap = new HashMap<>(); // key=codeRef
											// value 0=стар; 1=нов; 2=премахнат; 3=отказант достъп;
		}

		boolean registraturaChanged = false; // ще се вдигне флаг ако е деловодител и прави запис на нова задача в различна 
											// регистратура от която е назначен като служител !?!?
		int codeAction;
		if (task.getId() == null) {
			codeAction = CODE_DEIN_ZAPIS;

			if (accessMap != null) {
				if (systemData.isDopDelovoditelRegistraturi()) { // възможно е да се работи от други регистратури спрямо назначението
					Integer admReg = (Integer) systemData.getItemSpecific(Constants.CODE_CLASSIF_ADMIN_STR, getUserId()
						, SysConstants.CODE_DEFAULT_LANG, null, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
					if (task.getRegistraturaId().equals(admReg)) {
						accessMap.put(getUserId(), 1); // регистратора като нов
					} else {
						registraturaChanged = true; // има смяна и регистратора ще влезне само с личен достъп без звено
					}
				} else { // масовия случай
					accessMap.put(getUserId(), 1); // регистратора като нов
				}
			}

		} else {
			codeAction = CODE_DEIN_KOREKCIA;

			if (accessMap != null) {
				if (systemData.isDopDelovoditelRegistraturi()) { // трябва да проверя регистриралия какъв достъп е получил
					Integer admReg = (Integer) systemData.getItemSpecific(Constants.CODE_CLASSIF_ADMIN_STR, task.getUserReg()
						, SysConstants.CODE_DEFAULT_LANG, task.getDateReg(), OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
					if (admReg == null || task.getRegistraturaId().equals(admReg)) { // само ако регистратора е работил в своята регистратура
						accessMap.put(task.getUserReg(), 0); // регистратора като стар
					} else {
						registraturaChanged = true;
					}
				} else { // масовия случай
					accessMap.put(task.getUserReg(), 0); // регистратора като стар
				}
			}
		}

		Integer oldEndDocId = task.getDbEndDocId();

		Integer oldStatus = task.getDbStatus();
		Integer oldCodeControl = task.getDbCodeControl();
		Integer oldHashNotifData = task.getHashNotifData();

		Task saved = super.save(task); // основния запис !!!

		List<Integer> deleteCodeExecs = referentsSave(task, saved, accessMap, registraturaChanged); // изпълнителите, като изтритите трябват за
																				// нотификациите

		if (task.getDocId() != null // задача по документ от вид подписване или съгласуване в статус изпълнена или изпълнена след
									// срок
			&& (task.getTaskType().equals(CODE_ZNACHENIE_TASK_TYPE_PODPIS) || task.getTaskType().equals(CODE_ZNACHENIE_TASK_TYPE_SAGL))
			&& (task.getStatus().equals(CODE_ZNACHENIE_TASK_STATUS_IZP) || task.getStatus().equals(CODE_ZNACHENIE_TASK_STATUS_IZP_SROK))) {

			transferDocReferent(saved, doc, systemData);
		}

		if (doc != null && doc.getId() != null // задача по входящ документ, който все още не е обработен -> трявва да стане
												// обработен
			&& Objects.equals(doc.getDocType(), CODE_ZNACHENIE_DOC_TYPE_IN) && !Objects.equals(doc.getProcessed(), CODE_ZNACHENIE_DA)) {

			doc.setProcessed(CODE_ZNACHENIE_DA);
			doc.setDbProcessed(doc.getProcessed());

			JPA.getUtil().getEntityManager().createNativeQuery("update DOC set PROCESSED = ?1 where DOC_ID = ?2")
				.setParameter(1, CODE_ZNACHENIE_DA).setParameter(2, doc.getId()).executeUpdate();

			SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, doc.getId()
				, "Документ "+doc.getIdentInfo()+" е маркиран за обработен, при регистриране на задача " + saved.getRnTask() + ".");

			journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
			journal.setDateAction(new Date());
			journal.setIdUser(getUserId());

			new DocDAO(getUser()).saveAudit(journal);
		}

		saved.setDbEndDocId(task.getEndDocId());

		saved.setDbStatus(task.getStatus()); // заради JPA мерге
		saved.setDbCodeAssign(task.getCodeAssign());
		saved.setDbCodeControl(task.getCodeControl()); // заради JPA мерге
		saved.setRnDocEnd(task.getRnDocEnd());
		saved.setDateDocEnd(task.getDateDocEnd());

		saved.setHashNotifData(Objects.hash(task.getTaskType(), task.getAssignDate(), task.getSrokDate(), task.getTaskInfo()));

		saveAudit(saved, codeAction); // тука вече трябва да всичко да е насетвано и записа в журнала е ОК

		if (accessMap != null) { // гради достъпа за документите в задачата
			docAccessAddRemove(task, systemData, accessMap, codeAction, oldEndDocId, registraturaChanged);
		}

//		//Първи стъпки към достъпа
//		if (task.getDocId() != null) {
//			if (codeAction == CODE_DEIN_ZAPIS) {
//				//Първоначален запис
//				//Достъпът се гради само от основните данни на документа ...
//				new DocDostUtils().createTaskDocDost(task, true, systemData);
//			}else {
//				new DocDostUtils().createTaskDocDost(task, false, systemData);
//			}
//		}

		// !!! ДЖУРКАНЕТО НА НОТИФИКАЦИИ !!!
		notifTaskSave(saved, doc, oldStatus, oldCodeControl, oldHashNotifData, deleteCodeExecs, systemData);

		if (saved.getDocId() != null) { // само за задачи по документ
			if (oldEndDocId == null) {
				if (saved.getEndDocId() != null) { // добавен е документ при приключване на задача
					relDocByTask(saved.getEndDocId(), saved.getDocId(), saved.getId(), true);
				} 
			} else if (!oldEndDocId.equals(saved.getEndDocId())) {
				relDocByTask(oldEndDocId, saved.getDocId(), saved.getId(), false); // oldEndDocId този вече не е
				
				if (saved.getEndDocId() != null) { // добавен е друг документ при приключване на задача
					relDocByTask(saved.getEndDocId(), saved.getDocId(), saved.getId(), true);
				}
			}
		}

		return saved;
	}

	/**
	 * Обработка на задача по етап на процедура
	 * 
	 * @param task
	 * @param statusChanged
	 * @param systemData
	 * @throws DbErrorException
	 */
	private void processEtapTask(Task task, boolean statusChanged, SystemData systemData) throws DbErrorException {
		if (statusChanged && task.getStatus().equals(CODE_ZNACHENIE_TASK_STATUS_AUTO_PRIKL)) {
			return; // автоматично приключване на задача от процеса и тука нищо повече не се случва
		}

		@SuppressWarnings("unchecked")
		List<Object[]> etapRows = createNativeQuery(
			"select ee.EXE_ETAP_ID, ee.STATUS from PROC_EXE_TASK et inner join PROC_EXE_ETAP ee on ee.EXE_ETAP_ID = et.EXE_ETAP_ID where et.TASK_ID = ?1")
			.setParameter(1, task.getId()).getResultList();
		if (etapRows.isEmpty() || etapRows.get(0)[1] == null) {
			return; // няма логика, но все пак
		}
		int etapStatus = ((Number)etapRows.get(0)[1]).intValue();
		if (etapStatus != CODE_ZNACHENIE_ETAP_STAT_EXE && etapStatus != CODE_ZNACHENIE_ETAP_STAT_DECISION) {
			throw new DbErrorException("Задача с номер: "+task.getRnTask()+" не може да се редактира, защото е по неактивен етап от процедура.");
		}

		if (statusChanged) {
			boolean oldActive = systemData.matchClassifItems(CODE_CLASSIF_TASK_STATUS_ACTIVE, task.getDbStatus(), task.getStatusDate());
			boolean newActive = systemData.matchClassifItems(CODE_CLASSIF_TASK_STATUS_ACTIVE, task.getStatus(), task.getStatusDate());
			
			if (oldActive && !newActive) { // преминава от активен в неактивен
				new ProcExeEtapDAO(getUser()).processTask(task, ((Number)etapRows.get(0)[0]).intValue(), systemData);
			}
		}
	}

	/**
	 * Преди да се изтрие задачата трябва да се изтрият и други данни, които не са мапнати дирекно през JPA
	 */
	@Override
	protected void remove(Task entity) throws DbErrorException, ObjectInUseException {
		try {
			if (entity.getCodeExecs() != null && !entity.getCodeExecs().isEmpty()) { // само ако има
				int deleted = createNativeQuery("delete from TASK_REFERENTS where TASK_ID = ?1").setParameter(1, entity.getId()).executeUpdate();
				LOGGER.debug("Изтрити са {} изпълнители на задача с TASK_ID={}", deleted, entity.getId());
			}

			int deleted = createNativeQuery("delete from TASK_STATES where TASK_ID = ?1").setParameter(1, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} състояния на задача с TASK_ID={}", deleted, entity.getId());
	
			// DOC_ACCESS_ALL
			deleted = createNativeQuery("delete from DOC_ACCESS_ALL where OBJECT_CODE = ?1 and OBJECT_ID = ?2")
				.setParameter(1, OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK).setParameter(2, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} реда от DOC_ACCESS_ALL за задача с TASK_ID={}", deleted, entity.getId());

		} catch (Exception e) {
			throw new DbErrorException("Грешка при изтриване на свързани обекти на задача!", e);
		}

		super.remove(entity);
	}

	/**
	 * Гради достъпа за документите в задачата
	 */
	void docAccessAddRemove(Task task, SystemData systemData, Map<Integer, Integer> accessMap, int codeAction, Integer oldEndDocId, boolean registraturaChanged) throws DbErrorException {
		Set<Integer> newAccess = null;
		Set<Integer> delAccess = null;
		DocDostUtils du = new DocDostUtils();
		
		if (task.getDocId() != null) { // гради се достъп за документа в задачата, защото го има !!!

			if (codeAction == CODE_DEIN_ZAPIS) {
				newAccess = accessMap.keySet();

				if (registraturaChanged) { // в този случай регистриралия получава личен достъп
					du.addDocAccessUserReg(task.getDocId(), task.getUserReg(), CODE_ZNACHENIE_JOURNAL_TASK, task.getId());
				}

			} else {
				newAccess = new HashSet<>();
				delAccess = new HashSet<>();

				for (Entry<Integer, Integer> entry : accessMap.entrySet()) {
					if (entry.getValue().intValue() == 1) {
						newAccess.add(entry.getKey());

					} else if (entry.getValue().intValue() == 2 || entry.getValue().intValue() == 3) {
						delAccess.add(entry.getKey());
					}
				}
			}

			if (!newAccess.isEmpty() || delAccess != null && !delAccess.isEmpty()) { // само частично преизграждане
																						// в зависимост от промените и то ако ги
																						// има !!!
				du.addRemoveDocDost(task.getDocId(), CODE_ZNACHENIE_JOURNAL_TASK, task.getId(), delAccess, newAccess, systemData);
			}
		}

		// проверките дали ще се гради достъп за резултатния документ на задачата

		if (oldEndDocId == null) {

			if (task.getEndDocId() != null) { // даване на всички за първи път

				Set<Integer> newAccessEndDoc = new HashSet<>();
				for (Entry<Integer, Integer> entry : accessMap.entrySet()) {
					if (entry.getValue().intValue() == 0 || entry.getValue().intValue() == 1) {
						newAccessEndDoc.add(entry.getKey());
					}
				}
				du.addRemoveDocDost(task.getEndDocId(), CODE_ZNACHENIE_JOURNAL_TASK, task.getId(), null, newAccessEndDoc, systemData);

				if (registraturaChanged) { // в този случай регистриралия получава личен достъп
					du.addDocAccessUserReg(task.getEndDocId(), task.getUserReg(), CODE_ZNACHENIE_JOURNAL_TASK, task.getId());
				}
			}

		} else if (oldEndDocId.equals(task.getEndDocId())) { // няма промяна в дока, но може да има промяна в хората по задачата

			if (newAccess == null || delAccess == null) { // само ако не е вече по документ, защото иначе ще са определени правилно
				newAccess = new HashSet<>();
				delAccess = new HashSet<>();

				for (Entry<Integer, Integer> entry : accessMap.entrySet()) {
					if (entry.getValue().intValue() == 1) {
						newAccess.add(entry.getKey());

					} else if (entry.getValue().intValue() == 2 || entry.getValue().intValue() == 3) {
						delAccess.add(entry.getKey());
					}
				}
			}

			if (!newAccess.isEmpty() || !delAccess.isEmpty()) {
				du.addRemoveDocDost(task.getEndDocId(), CODE_ZNACHENIE_JOURNAL_TASK, task.getId(), delAccess, newAccess, systemData);
			}
			
		} else { // сменен е дока при приключване

			// отнемане на всички референти по задачата достъпа от стария документ
			du.removeDocDostFromObject(oldEndDocId, CODE_ZNACHENIE_JOURNAL_TASK, task.getId());

			if (task.getEndDocId() != null) { // даване на всички за първи път
				Set<Integer> newAccessEndDoc = new HashSet<>();
				for (Entry<Integer, Integer> entry : accessMap.entrySet()) {
					if (entry.getValue().intValue() == 0 || entry.getValue().intValue() == 1) {
						newAccessEndDoc.add(entry.getKey());
					}
				}
				du.addRemoveDocDost(task.getEndDocId(), CODE_ZNACHENIE_JOURNAL_TASK, task.getId(), null, newAccessEndDoc, systemData);

				if (registraturaChanged) { // в този случай регистриралия получава личен достъп
					du.addDocAccessUserReg(task.getEndDocId(), task.getUserReg(), CODE_ZNACHENIE_JOURNAL_TASK, task.getId());
				}
			}
		}
	}

	/**
	 * Генериране на регистров номер на задача
	 *
	 * @param task
	 * @throws DbErrorException
	 */
	private void genRnTask(Task task) throws DbErrorException {
		int yyyy = LocalDate.now().getYear();
		try {
			StoredProcedureQuery storedProcedure = getEntityManager().createStoredProcedureQuery("gen_nom_task") //
				.registerStoredProcedureParameter(0, Integer.class, ParameterMode.IN) //
				.registerStoredProcedureParameter(1, String.class, ParameterMode.OUT); //

			storedProcedure.setParameter(0, yyyy);

			storedProcedure.execute();

			task.setRnTask((String) storedProcedure.getOutputParameterValue(1));

		} catch (Exception e) {
			throw new DbErrorException("Грешка при генериране на регистров номер на задача!", e);
		}
	}

	/**
	 * Запис на участниците в задачата. В това число и изтриване на отпаднали.
	 *
	 * @return списъка с изтрити изпълнители
	 * @throws DbErrorException
	 */
	List<Integer> referentsSave(Task task, Task saved, Map<Integer, Integer> accessMap, boolean registraturaChanged) throws DbErrorException {
		saved.setNewCodeExecs(null); // ако има нови ще се изчислят

		List<Integer> current = task.getCodeExecs();

		List<Integer> dbList = task.getDbCodeExecs(); // ако нещо остане тука най накрая ще се трие
		List<Integer> toInsert = null; // само ако има нужда

		Integer currentOtg = null;
		if (dbList == null || dbList.isEmpty()) { // нов запис
			toInsert = current;
			currentOtg = current.get(0);

		} else if (current == null || current.isEmpty()) { // всички са изтрии
			// и всички db ще се трият

		} else if (!Objects.equals(dbList, current)) { // има разлика в списъците и във всеки има по нещо
			toInsert = new ArrayList<>(current);

			Integer dbOtg = dbList.get(0);
			currentOtg = current.get(0);

			if (!dbOtg.equals(currentOtg)) { // имаме смяна на отговорен
				List<Integer> toUpdate = new ArrayList<>();

				if (current.contains(dbOtg)) {
					toUpdate.add(dbOtg);
				}
				if (dbList.contains(currentOtg)) {
					toUpdate.add(currentOtg);
				}
				if (!toUpdate.isEmpty()) {
					// единсвтеното което може да се смени е ролята : от отговорен изпълнител на обикновен изпълнител
					Query query = createNativeQuery("update TASK_REFERENTS set ROLE_REF = (case when ROLE_REF = 1 then 2 else 1 end) " //
						+ " , USER_REG = :userReg, DATE_REG = :dateReg " //
						+ " where TASK_ID = :taskId and CODE_REF in (:codeRefList) ") //
							.setParameter("userReg", getUserId()).setParameter("dateReg", new Date())//
							.setParameter("taskId", saved.getId()).setParameter("codeRefList", toUpdate);

					int count = query.executeUpdate();

					LOGGER.debug("UPDATE {} TASK_REFERENTS for TASK_ID={}", count, saved.getId());
				}
			}

			toInsert.removeAll(dbList);
			dbList.removeAll(current);

		} else {
			// заради MERGE на JPA това се прави, че ги замазва
			saved.setCodeExecs(current);
			saved.setDbCodeExecs(new ArrayList<>(current));

			if (accessMap != null) {
				for (Integer codeRef : current) {
					accessMap.put(codeRef, 0); // тука винаги може да си се добави, защото няма да има промяна
				}
				addAssignControlToAccessMap(task, accessMap, registraturaChanged);
			}

			return null; // няма никаква промяна
		}

		if (toInsert != null && !toInsert.isEmpty()) {
			saved.setNewCodeExecs(toInsert); // за да се знаят за разните му тотификации

			// записите на всички нови
			TaskReferentDAO taskReferentDao = new TaskReferentDAO(getUser());
			for (Integer codeRef : toInsert) {
				int roleRef = codeRef.equals(currentOtg) ? CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG : CODE_ZNACHENIE_TASK_REF_ROLE_EXEC;

				TaskReferent taskReferent = new TaskReferent(saved.getId(), codeRef, roleRef);
				taskReferentDao.save(taskReferent);

				if (accessMap != null) {
					Integer temp = accessMap.get(taskReferent.getCodeRef());
					if (temp != null) { // има го
						if (temp.intValue() == 2) { // ако го има като изтрит и сега влиза като нов значи остава без промяна
							accessMap.put(taskReferent.getCodeRef(), 0);
						}
					} else { // трябва да си влезне като нов
						accessMap.put(taskReferent.getCodeRef(), 1);
					}
				}
			}
		}
		if (dbList != null && !dbList.isEmpty()) {
			Query query = createNativeQuery(" delete from TASK_REFERENTS where TASK_ID = :taskId and CODE_REF in (:codeRefList) ") //
				.setParameter("taskId", saved.getId()).setParameter("codeRefList", dbList);

			int count = query.executeUpdate();

			LOGGER.debug("Delete {} TASK_REFERENTS for TASK_ID={}", count, saved.getId());

			if (accessMap != null) {
				for (Integer codeRef : dbList) {
					Integer temp = accessMap.get(codeRef);
					if (temp != null) { // има го
						if (temp.intValue() == 1) { // ако го има като нов и иска да се трие остава непроменен
							accessMap.put(codeRef, 0);
						}
					} else { // влиза си за триене
						accessMap.put(codeRef, 2);
					}
				}
			}
		}

		if (accessMap != null) {
			addAssignControlToAccessMap(task, accessMap, registraturaChanged);
		}

		// заради MERGE на JPA това се прави, че ги замазва
		saved.setCodeExecs(current);
		if (current != null) {
			saved.setDbCodeExecs(new ArrayList<>(current));
		} else {
			saved.setDbCodeExecs(null);
		}

		return dbList;
	}

	/**
	 * Прехвърля данни при приключване на задача за подпис или за резолюция в документа
	 *
	 * @param task
	 * @param systemData
	 * @throws DbErrorException
	 */
	void transferDocReferent(Task task, Doc doc, SystemData systemData) throws DbErrorException {
		DocDAO docDao = new DocDAO(getUser());

		boolean docArg = true;
		if (doc == null) {
			docArg = false; // за да се знае че не е дошъл документ в метода и не трябва накравя да се синхронизира нищо
			doc = docDao.findById(task.getDocId());
		}

		if (doc == null || Objects.equals(doc.getDocType(), CODE_ZNACHENIE_DOC_TYPE_IN)) {
			return;
		}

		Integer role;
		List<DocReferent> docRefs;

		boolean forReg = false;

		if (task.getTaskType().equals(CODE_ZNACHENIE_TASK_TYPE_PODPIS)) {
			role = CODE_ZNACHENIE_DOC_REF_ROLE_SIGNED;

			if (doc.getReferentsSigned() == null) {
				doc.setReferentsSigned(new ArrayList<>());
			}
			docRefs = doc.getReferentsSigned();

			if (Objects.equals(doc.getDocType(), CODE_ZNACHENIE_DOC_TYPE_WRK) && task.getEndOpinion() != null
				&& (task.getEndOpinion().equals(CODE_ZNACHENIE_TASK_OPINION_PODPIS) || task.getEndOpinion().equals(CODE_ZNACHENIE_TASK_OPINION_PODPIS_MNENIE))) {
				
				StringBuilder sql = new StringBuilder();
				sql.append(" select t.TASK_ID from TASK t where t.DOC_ID = :docId and t.TASK_TYPE = :taskType ");
				sql.append(" and (t.STATUS not in (:izp,:izpSrok,:sneta) or (t.STATUS in (:izp,:izpSrok) and t.END_OPINION not in (:podpis,:podpisMnenie) ");
				sql.append("			 and not exists (select tr.id from task_referents tr where tr.task_id = t.task_id and tr.code_ref = :codeExec) ) ) ");
				
				Integer codeExec;
				if (task.getCodeExecs() != null && !task.getCodeExecs().isEmpty()) {
					codeExec = task.getCodeExecs().get(0);
				} else {
					codeExec = ((UserData)getUser()).getUserAccess(); // няма причина но все пак
				}
				
				@SuppressWarnings("unchecked")
				List<Object> rows = createNativeQuery(sql.toString()) //
					.setParameter("docId", doc.getId()).setParameter("taskType", CODE_ZNACHENIE_TASK_TYPE_PODPIS) //
					.setParameter("izp", CODE_ZNACHENIE_TASK_STATUS_IZP).setParameter("izpSrok", CODE_ZNACHENIE_TASK_STATUS_IZP_SROK) //
					.setParameter("sneta", CODE_ZNACHENIE_TASK_STATUS_SNETA)
					.setParameter("podpis", CODE_ZNACHENIE_TASK_OPINION_PODPIS).setParameter("podpisMnenie", CODE_ZNACHENIE_TASK_OPINION_PODPIS_MNENIE) //
					.setParameter("codeExec", codeExec) // 
					.getResultList();
				forReg = rows.isEmpty(); // ако няма други е готов
			}
		} else {
			role = CODE_ZNACHENIE_DOC_REF_ROLE_AGREED;

			if (doc.getReferentsAgreed() == null) {
				doc.setReferentsAgreed(new ArrayList<>());
			}
			docRefs = doc.getReferentsAgreed();
		}

		Integer execCode = task.getCodeExecs() == null || task.getCodeExecs().isEmpty() //
			? task.getStatusUserId() //
			: task.getCodeExecs().get(0);

		DocReferent docRef = null;
		if (!docRefs.isEmpty()) { // трябва да се провери дали вече този не участва в документа

			for (DocReferent temp : docRefs) {
				if (temp.getCodeRef().equals(execCode)) {
					docRef = temp; // този ще се актуализира
					break;
				}
			}
		}
		if (docRef == null) {
			docRef = new DocReferent(execCode, role);
			docRef.setTekst(systemData.decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, execCode, getUserLang(), task.getRealEnd()));
			docRefs.add(docRef);

		} else { // открит е някакъв и ако няма реална промяна

			// ""=null, за да не се правят излишни записи по документа
			task.setStatusComments(SearchUtils.trimToNULL(task.getStatusComments()));
			docRef.setComments(SearchUtils.trimToNULL(docRef.getComments()));

			if (!forReg && Objects.equals(task.getStatusComments(), docRef.getComments()) //
				&& Objects.equals(task.getRealEnd(), docRef.getEventDate()) //
				&& Objects.equals(task.getEndOpinion(), docRef.getEndOpinion())) {
				return; // няма да се прави запис на документа !!!
			}
		}

		docRef.setComments(task.getStatusComments());
		docRef.setEventDate(task.getRealEnd());
		docRef.setEndOpinion(task.getEndOpinion());

		Doc saved;
		try {
			if (forReg) {
				if(doc.getPreForRegId() != null) {
					doc.setForRegId(doc.getPreForRegId());
				}else {
					doc.setForRegId(doc.getRegistraturaId());
				}
				
//				Object[] settings = docDao.findDocSettings(doc.getRegistraturaId(), doc.getDocVid(), systemData);
//				if (settings != null && settings[3] != null) { // има зададена по вид документ
//					doc.setForRegId((Integer) settings[3]);
//				} else { // остава си в тази, в която е документа
//					doc.setForRegId(doc.getRegistraturaId());
//				}
			}
			saved = docDao.save(doc, false, null, null, systemData);
		} catch (ObjectInUseException e) {
			throw new DbErrorException(e);
		}
		if (docArg) {
			fixDocAfterMerge(doc, saved);
		}
	}

	 
	/**
	 * Създава или изтрива връзка между документи, в зависимост от док при приключване на задача.
	 * Нов запис се прави само ако няма такава връзка между документите.
	 * Изтрива се само ако няма други задачи, които да правят тази връзка.
	 * 
	 * @param docId1
	 * @param docId2
	 * @param create <code>true</code> запис, <code>false</code> изриване
	 * @param taskId
	 * @throws DbErrorException 
	 */
	@SuppressWarnings("unchecked")
	private void relDocByTask(Integer docId1, Integer docId2, Integer taskId, boolean create) throws DbErrorException {
		try {
			if (create) { // ако ще се прави нова трябва да няма вече такава направена
			
				List<Object> cnt = createNativeQuery("select count (*) from DOC_DOC where DOC_ID1 = ?1 and DOC_ID2 = ?2 and REL_TYPE = ?3")
					.setParameter(1, docId1).setParameter(2, docId2).setParameter(3, OmbConstants.CODE_ZNACHENIE_DOC_REL_TYPE_BY_TASK)
					.getResultList();
				if (cnt.isEmpty() || ((Number)cnt.get(0)).intValue() == 0) { // щом няма се прави нова
					DocDoc docDoc = new DocDoc();
					docDoc.setDocId1(docId1);
					docDoc.setDocId2(docId2);
					docDoc.setRelType(OmbConstants.CODE_ZNACHENIE_DOC_REL_TYPE_BY_TASK);
					new DocDocDAO(getUser()).save(docDoc);
				}
			
			} else { // ще се трие ако няма задача различна от тази която да е държи

				List<Object> ids = createNativeQuery("select dd.ID from DOC_DOC dd"
					+ " left outer join TASK t on t.END_DOC_ID = dd.DOC_ID1 and t.DOC_ID = dd.DOC_ID2 and t.TASK_ID <> ?4"
					+ " where dd.DOC_ID1 = ?1 and dd.DOC_ID2 = ?2 and dd.REL_TYPE = ?3 and t.TASK_ID is null")
					.setParameter(1, docId1).setParameter(2, docId2).setParameter(3, OmbConstants.CODE_ZNACHENIE_DOC_REL_TYPE_BY_TASK)
					.setParameter(4, taskId).getResultList();
				if (!ids.isEmpty()) { // трябва да се трие
					DocDocDAO docDocDao = new DocDocDAO(getUser());
					for (Object id : ids) {
						docDocDao.deleteById(((Number)id).intValue());
					}
				}
			}
			
		} catch (DbErrorException e) {
			throw e;
		}catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на връзки между документи!", e);
		}
	}
	
	/**
	 * Добавя информация за контролиращ и вълзожител за да се знае ще се променят ли правата или не. Има смисъл да се вика само
	 * ако задача е по документ.
	 */
	private void addAssignControlToAccessMap(Task task, Map<Integer, Integer> accessMap, boolean registraturaChanged) {
		if (!Objects.equals(task.getCodeControl(), task.getDbCodeControl())) { // има разлика в контролиращите

			if (task.getCodeControl() != null) { // този е нов
				Integer temp = accessMap.get(task.getCodeControl());
				if (temp != null) { // има го
					if (temp.intValue() == 2) { // ако го има като изтрит и сега влиза като нов значи остава без промяна
						accessMap.put(task.getCodeControl(), 0);
					}
				} else { // трябва да си влезне като нов
					accessMap.put(task.getCodeControl(), 1);
				}
			}

			if (task.getDbCodeControl() != null) { // този е за триене
				Integer temp = accessMap.get(task.getDbCodeControl());
				if (temp != null) { // има го
					if (temp.intValue() == 1) { // ако го има като нов и иска да се трие остава непроменен
						accessMap.put(task.getDbCodeControl(), 0);
					}
				} else { // влиза си за триене
					accessMap.put(task.getDbCodeControl(), 2);
				}
			}
		} else if (task.getCodeControl() != null) { // няма промяна, но трябва да го сложа като стар
			accessMap.put(task.getCodeControl(), 0);
		}

		boolean b1 = registraturaChanged && task.getUserReg().equals(task.getCodeAssign());
		boolean b2 = registraturaChanged && task.getUserReg().equals(task.getDbCodeAssign());
		
		if (!task.getCodeAssign().equals(task.getDbCodeAssign())) { // има разлика в възложителите

			if (task.getCodeAssign() != null && !b1) { // този е нов
				Integer temp = accessMap.get(task.getCodeAssign());
				if (temp != null) { // има го
					if (temp.intValue() == 2) { // ако го има като изтрит и сега влиза като нов значи остава без промяна
						accessMap.put(task.getCodeAssign(), 0);
					}
				} else { // трябва да си влезне като нов
					accessMap.put(task.getCodeAssign(), 1);
				}
			}

			if (task.getDbCodeAssign() != null && !b2) { // този е за триене
				Integer temp = accessMap.get(task.getDbCodeAssign());
				if (temp != null) { // има го
					if (temp.intValue() == 1) { // ако го има като нов и иска да се трие остава непроменен
						accessMap.put(task.getDbCodeAssign(), 0);
					}
				} else { // влиза си за триене
					accessMap.put(task.getDbCodeAssign(), 2);
				}
			}
		} else { // няма промяна, но трябва да го сложа като стар
			if (!b1) {
				accessMap.put(task.getCodeAssign(), 0);
			}
		}
	}

	/**
	 * Гледа срока спрямо изпълнението и е възможно пак да се смени статуса.
	 */
	private void checkTaskInSrok(Task task, SystemData systemData) throws DbErrorException {
		if (task.getSrokDate() == null) {
			if (Objects.equals(task.getStatus(), CODE_ZNACHENIE_TASK_STATUS_IZP_SROK)) {
				// зачистен е срока и няма как да остане изпълнена след срок
				task.setStatus(CODE_ZNACHENIE_TASK_STATUS_IZP);
			}

		} else if (Objects.equals(task.getStatus(), CODE_ZNACHENIE_TASK_STATUS_IZP)) {

			boolean setting = Objects.equals(systemData.getRegistraturaSetting(task.getRegistraturaId(), CODE_ZNACHENIE_REISTRATURA_SETTINGS_15), CODE_ZNACHENIE_DA);

			ChronoUnit chronoUnit = setting ? ChronoUnit.MINUTES : ChronoUnit.DAYS;

			Instant srokInstant = task.getSrokDate().toInstant().truncatedTo(chronoUnit);
			Instant endInstant = task.getRealEnd().toInstant().truncatedTo(chronoUnit);

			if (srokInstant.isBefore(endInstant)) { // просрочена
				task.setStatus(CODE_ZNACHENIE_TASK_STATUS_IZP_SROK);
			}

		} else if (Objects.equals(task.getStatus(), CODE_ZNACHENIE_TASK_STATUS_IZP_SROK)) {

			boolean setting = Objects.equals(systemData.getRegistraturaSetting(task.getRegistraturaId(), CODE_ZNACHENIE_REISTRATURA_SETTINGS_15), CODE_ZNACHENIE_DA);

			ChronoUnit chronoUnit = setting ? ChronoUnit.MINUTES : ChronoUnit.DAYS;

			Instant srokInstant = task.getSrokDate().toInstant().truncatedTo(chronoUnit);
			Instant endInstant = task.getRealEnd().toInstant().truncatedTo(chronoUnit);

			if (!srokInstant.isBefore(endInstant)) { // явно вече не е просрочена
				task.setStatus(CODE_ZNACHENIE_TASK_STATUS_IZP);
			}
		}
	}

	/**
	 * @param doc
	 * @param saved
	 */
	private void fixDocAfterMerge(Doc doc, Doc saved) {
		doc.setHistory(saved.getHistory());
		doc.setReferentsAgreed(saved.getReferentsAgreed());
		doc.setReferentsAuthor(saved.getReferentsAuthor());
		doc.setReferentsSigned(saved.getReferentsSigned());

		doc.setDeloIncluded(saved.isDeloIncluded());

		doc.setWorkOffData(saved.getWorkOffData());
		doc.setProtocolData(saved.getProtocolData());

		doc.setDocAccess(saved.getDocAccess());
	}
}
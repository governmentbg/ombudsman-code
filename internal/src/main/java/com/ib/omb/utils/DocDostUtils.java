package com.ib.omb.utils;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ACTIVE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_STATUS_CONTINUED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DVIJ;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_ACCESS;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_DA;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_NE;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.system.Constants;
import com.ib.omb.db.dao.DeloDAO;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dto.DeloAccess;
import com.ib.omb.db.dto.DocAccess;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.BaseSystemData;
import com.ib.system.BaseUserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;

/**
 * Utils for create/recreate access to doc/prep
 *
 * @author mamun
 */
public class DocDostUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocDostUtils.class);

	/**  */
	public DocDostUtils() {
		super();
	}

	/**
	 * Дава достъп до преписката само на регистриралия. Като особеното е че за код на звено слага кода на регистриралия и така
	 * няма да се даде достъп на шефовете във звеното
	 *
	 * @param deloId
	 * @param userReg
	 * @param objectCode
	 * @param objectId
	 * @throws DbErrorException
	 */
	public void addDeloAccessUserReg(Integer deloId, Integer userReg, Integer objectCode, Integer objectId) throws DbErrorException {
		try {
			Query insQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
				"insert into DELO_ACCESS_ALL(DELO_ID, CODE_REF, CODE_STRUCT, OBJECT_CODE, OBJECT_ID) values(?1, ?2, ?3, ?4, ?5)");

			insQuery.setParameter(1, deloId);
			insQuery.setParameter(2, userReg);
			insQuery.setParameter(3, userReg);
			insQuery.setParameter(4, objectCode);
			insQuery.setParameter(5, objectId);

			insQuery.executeUpdate();

		} catch (Exception e) {
			LOGGER.error("Грешка при изграждане достъп на дело/преписка!");
			throw new DbErrorException(e);
		}
	}

	/**
	 * Дава достъп до преписката на всички хора, които имат достъп до преписката, в която тя се влага.
	 *
	 * @param deloId
	 * @param fromDeloId
	 * @param set        за да не се зациклим
	 * @throws DbErrorException
	 */
	public void addDeloDostFromDelo(Integer deloId, Integer fromDeloId, Set<Integer> set) throws DbErrorException {
		set.add(fromDeloId);
		try {
			EntityManager em = JPA.getUtil().getEntityManager();

			Query delQuery = em.createNativeQuery( //
				"delete from DELO_ACCESS_ALL where DELO_ID = ?1 and OBJECT_CODE = ?2 and OBJECT_ID = ?3") //
				.setParameter(1, deloId).setParameter(2, CODE_ZNACHENIE_JOURNAL_DELO).setParameter(3, fromDeloId);
			delQuery.executeUpdate();

			Query insQuery = em.createNativeQuery( //
				"insert into DELO_ACCESS_ALL(DELO_ID, CODE_REF, CODE_STRUCT, OBJECT_CODE, OBJECT_ID)" //
					+ " select distinct " + deloId + ", CODE_REF, CODE_STRUCT, " + CODE_ZNACHENIE_JOURNAL_DELO + ", " + fromDeloId //
					+ " from DELO_ACCESS_ALL where DELO_ID = ?1 and OBJECT_CODE = ?2")
				.setParameter(1, fromDeloId).setParameter(2, CODE_ZNACHENIE_JOURNAL_DELO); // само през дела трябва тука
			insQuery.executeUpdate();

			recursiveDeloDost(deloId, set); // пускаме рекурсивно надолу

		} catch (Exception e) {
			LOGGER.error("Грешка при изграждане достъп на дело от дело/преписка!");
			throw new DbErrorException(e);
		}
	}

	/**
	 * Дава достъп до документа само на регистриралия. Като особеното е че за код на звено слага кода на регистриралия и така няма
	 * да се даде достъп на шефовете във звеното
	 *
	 * @param docId
	 * @param userReg
	 * @param objectCode
	 * @param objectId
	 * @throws DbErrorException
	 */
	public void addDocAccessUserReg(Integer docId, Integer userReg, Integer objectCode, Integer objectId) throws DbErrorException {
		try {
			Query insQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
				"insert into DOC_ACCESS_ALL(DOC_ID, CODE_REF, CODE_STRUCT, OBJECT_CODE, OBJECT_ID) values(?1, ?2, ?3, ?4, ?5)");

			insQuery.setParameter(1, docId);
			insQuery.setParameter(2, userReg);
			insQuery.setParameter(3, userReg);
			insQuery.setParameter(4, objectCode);
			insQuery.setParameter(5, objectId);

			insQuery.executeUpdate();

		} catch (Exception e) {
			LOGGER.error("Грешка при изграждане достъп на документ!");
			throw new DbErrorException(e);
		}
	}

	/**
	 * Дава достъп до документа на всички хора, които имат достъп до преписката, в която той се влага
	 *
	 * @param docId
	 * @param fromDeloId
	 * @throws DbErrorException
	 */
	public void addDocDostFromDelo(Integer docId, Integer fromDeloId) throws DbErrorException {
		try {
			EntityManager em = JPA.getUtil().getEntityManager();

			Query delQuery = em.createNativeQuery( //
				"delete from DOC_ACCESS_ALL where DOC_ID = ?1 and OBJECT_CODE = ?2 and OBJECT_ID = ?3") //
				.setParameter(1, docId).setParameter(2, CODE_ZNACHENIE_JOURNAL_DELO).setParameter(3, fromDeloId);
			delQuery.executeUpdate();

			Query insQuery = em.createNativeQuery( //
				"insert into DOC_ACCESS_ALL(DOC_ID, CODE_REF, CODE_STRUCT, OBJECT_CODE, OBJECT_ID)" //
					+ " select distinct " + docId + ", CODE_REF, CODE_STRUCT, " + CODE_ZNACHENIE_JOURNAL_DELO + ", " + fromDeloId //
					+ " from DELO_ACCESS_ALL where DELO_ID = ?1 and OBJECT_CODE = ?2")
				.setParameter(1, fromDeloId).setParameter(2, CODE_ZNACHENIE_JOURNAL_DELO); // само през дела трябва тука
			insQuery.executeUpdate();

		} catch (Exception e) {
			LOGGER.error("Грешка при изграждане достъп на документ от дело/преписка!");
			throw new DbErrorException(e);
		}
	}

	/**
	 * Метод за промяна на достъп до дело
	 *
	 * @param deloId     - системен идентификатор на обект
	 * @param isNew      - ако е запис на нова преписка
	 * @param codeObject - код на обекта, от който идва достъпа
	 * @param idObject   - ид на обект, от който идва достъпа
	 * @param forDelete  - списък референти за изтриване от достъпа
	 * @param forInsert  - списък референти за добавяне към достъпа
	 * @param sd         - SystemData
	 * @throws DbErrorException - грешка при изграждане на достъп
	 */
	public void addRemoveDeloDost(Integer deloId, boolean isNew, Integer codeObject, Integer idObject, Set<Integer> forDelete, Set<Integer> forInsert, SystemData sd) throws DbErrorException {
		try {
			boolean changed = false; // ще се вдигне ако има реална промяна в хората

			if (forDelete != null && forDelete.size() > 0) {
				changed = true;

				Query delQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
					"delete from DELO_ACCESS_ALL where DELO_ID = ?1 and OBJECT_CODE = ?2 and OBJECT_ID = ?3 and CODE_REF in (?4) and CODE_REF != CODE_STRUCT") //
					.setParameter(1, deloId).setParameter(2, codeObject).setParameter(3, idObject).setParameter(4, forDelete);
				delQuery.executeUpdate();
			}

			if (forInsert != null && forInsert.size() > 0) {
				changed = true;

				Date date = new Date(); // тука винаги е към днешна дата защото са само новите в момента на възникаван

				Query insQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
					"insert into DELO_ACCESS_ALL(DELO_ID, CODE_REF, CODE_STRUCT, OBJECT_CODE, OBJECT_ID) values(?1, ?2, ?3, ?4, ?5)");

				for (Integer tek : forInsert) {
					Integer parent = sd.getItemParentCode(CODE_CLASSIF_ADMIN_STR, tek, date);
					if (parent == null) {
						parent = -200; // към момента човека е напуснал и няма как да му се определи звеното
					}
					insQuery.setParameter(1, deloId);
					insQuery.setParameter(2, tek);
					insQuery.setParameter(3, parent);
					insQuery.setParameter(4, codeObject);
					insQuery.setParameter(5, idObject);

					insQuery.executeUpdate();
				}
			}

			if (!isNew && changed) { // само ако не е нова и има някаква промяна се прави рекурсивното
				Set<Integer> set = new HashSet<>();
				set.add(deloId);

				recursiveDeloDost(deloId, set); // пускаме рекурсивно надолу
			}

		} catch (Exception e) {
			LOGGER.error("Грешка при изграждане достъп на дело!");
			throw new DbErrorException(e);
		}
	}

	/**
	 * Метод за промяна на достъп до документ през обект
	 *
	 * @param docId      - системен идентификатор на обект
	 * @param codeObject - код на обекта, от който идва достъпа
	 * @param idObject   - ид на обект, от който идва достъпа
	 * @param forDelete  - списък референти за изтриване от достъпа
	 * @param forInsert  - списък референти за добавяне към достъпа
	 * @param sd         - SystemData
	 * @throws DbErrorException - грешка при изграждане на достъп
	 */
	public void addRemoveDocDost(Integer docId, Integer codeObject, Integer idObject, Set<Integer> forDelete, Set<Integer> forInsert, SystemData sd) throws DbErrorException {
		if (docId == null) {
			return;
		}
		try {
			if (forDelete != null && forDelete.size() > 0) {
				Query delQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
					"delete from DOC_ACCESS_ALL where DOC_ID = ?1 and OBJECT_CODE = ?2 and OBJECT_ID = ?3 and CODE_REF in (?4) and CODE_REF != CODE_STRUCT") //
					.setParameter(1, docId).setParameter(2, codeObject).setParameter(3, idObject).setParameter(4, forDelete);
				delQuery.executeUpdate();
			}

			if (forInsert != null && forInsert.size() > 0) {
				Date date = new Date(); // тука винаги е към днешна дата защото са само новите в момента на възникаван

				Query insQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
					"insert into DOC_ACCESS_ALL(DOC_ID, CODE_REF, CODE_STRUCT, OBJECT_CODE, OBJECT_ID) values(?1, ?2, ?3, ?4, ?5)");

				for (Integer tek : forInsert) {
					Integer parent = sd.getItemParentCode(CODE_CLASSIF_ADMIN_STR, tek, date);
					if (parent == null) {
						parent = -200; // към момента човека е напуснал и няма как да му се определи звеното
					}
					insQuery.setParameter(1, docId);
					insQuery.setParameter(2, tek);
					insQuery.setParameter(3, parent);
					insQuery.setParameter(4, codeObject);
					insQuery.setParameter(5, idObject);

					insQuery.executeUpdate();
				}
			}

		} catch (Exception e) {
			LOGGER.error("Грешка при изграждане достъп на документ!");
			throw new DbErrorException(e);
		}
	}

	/**
	 * 5.8.5.3 Отнемане на достъп - Изрично отнемане до преписка
	 *
	 * @param deloIds
	 * @param codeRef
	 * @param sd
	 * @param ud
	 * @return
	 * @throws DbErrorException
	 */
	public int denyDeloAccess(List<Integer> deloIds, Integer codeRef, BaseSystemData sd, BaseUserData ud) throws DbErrorException {
		int lead = 0; // броя на преписките, за които служителя е водещ
		try {
			DeloDAO deloDao = new DeloDAO(ud);

			EntityManager em = JPA.getUtil().getEntityManager();

			List<Integer> deloIdsDenyDocAccess = new ArrayList<>(); // тука ще са тези за които реално е отнет

			Query query = em.createQuery("select da from DeloAccess da where da.deloId = ?1 and da.codeRef = ?2");
			Query leadQuery = em.createQuery("select d.codeRefLead from Delo d where d.id = ?1");
			for (Integer deloId : deloIds) {

				@SuppressWarnings("unchecked")
				List<Integer> leadList = leadQuery.setParameter(1, deloId).getResultList();
				if (!leadList.isEmpty() && codeRef.equals(leadList.get(0))) {
					lead++;
					continue; // пропуска се тази, защото е водещ
				}

				deloIdsDenyDocAccess.add(deloId);

				@SuppressWarnings("unchecked")
				List<DeloAccess> list = query.setParameter(1, deloId).setParameter(2, codeRef).getResultList();

				if (list.isEmpty()) {
					DeloAccess da = new DeloAccess();
					da.setCodeRef(codeRef);
					da.setDeloId(deloId);
					da.setExclude(CODE_ZNACHENIE_DA);

					em.persist(da);

				} else { // има и трябва да се упдате-не а отнет

					DeloAccess da = list.get(0);
					da.setExclude(CODE_ZNACHENIE_DA);

					em.merge(da);

					if (list.size() > 1) { // а ако са повече от един по някаква причина - другите се изтриват
						for (int i = 1; i < list.size(); i++) {
							em.remove(list.get(i));
						}
					}
				}

				@SuppressWarnings("unchecked")
				List<Object[]> deloData = em.createNativeQuery( //
					"select RN_DELO, DELO_DATE from DELO where DELO_ID = ?1").setParameter(1, deloId).getResultList();

				StringBuilder ident = new StringBuilder();
				ident.append("Отнет е достъпа на ");
				ident.append(sd.decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, codeRef, ud.getCurrentLang(), null));
				ident.append(" от " + sd.decodeItemDopInfo(Constants.CODE_CLASSIF_ADMIN_STR, codeRef, ud.getCurrentLang(), null));
				ident.append(" до преписка ");
				if (deloData.isEmpty()) {
					ident.append("с ИД=" + deloId);
				} else {
					ident.append(deloData.get(0)[0] + "/" + DateUtils.printDate((Date) deloData.get(0)[1]));
				}
				ident.append(".");

				SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO, deloId, ident.toString());
				journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
				journal.setDateAction(new Date());
				journal.setIdUser(ud.getUserId());
				deloDao.saveAudit(journal);
			}

			if (!deloIdsDenyDocAccess.isEmpty()) {
				@SuppressWarnings("unchecked")
				List<Integer> docIds = JPA.getUtil().getEntityManager().createQuery("select dd.inputDocId from DeloDoc dd where dd.deloId in (:deloIds)").setParameter("deloIds", deloIdsDenyDocAccess)
					.getResultList();

				denyDocAccess(docIds, codeRef, sd, ud);
			}

		} catch (Exception e) {
			LOGGER.error("Грешка при Отнеманене на достъп", e);
			throw new DbErrorException("Грешка при Отнеманене на достъп", e);
		}
		return lead;
	}

	/**
	 * 5.8.5.3 Отнемане на достъп - Изрично отнемане до документ
	 *
	 * @param docIds
	 * @param codeRef
	 * @param sd
	 * @param ud
	 * @throws DbErrorException
	 */
	public void denyDocAccess(List<Integer> docIds, Integer codeRef, BaseSystemData sd, BaseUserData ud) throws DbErrorException {
		try {
			DocDAO docDao = new DocDAO(ud);

			EntityManager em = JPA.getUtil().getEntityManager();

			Query query = em.createQuery("select da from DocAccess da where da.docId = ?1 and da.codeRef = ?2");
			Query deleteNotif = em.createNativeQuery( //
				"delete from USER_NOTIFICATIONS where USER_ID = :userId and MESSAGE_TYPE = :msgType and OBJECT_CODE = :objCode and OBJECT_ID = :objId and READ = 2");
			for (Integer docId : docIds) {

				@SuppressWarnings("unchecked")
				List<DocAccess> list = query.setParameter(1, docId).setParameter(2, codeRef).getResultList();

				if (list.isEmpty()) {
					DocAccess da = new DocAccess();
					da.setCodeRef(codeRef);
					da.setDocId(docId);
					da.setExclude(CODE_ZNACHENIE_DA);

					em.persist(da);

				} else { // има и трябва да се упдате-не а отнет

					DocAccess da = list.get(0);
					da.setExclude(CODE_ZNACHENIE_DA);

					em.merge(da);

					if (list.size() > 1) { // а ако са повече от един по някаква причина - другите се изтриват
						for (int i = 1; i < list.size(); i++) {
							em.remove(list.get(i));
						}
					}
				}

				// изтривам и нотификации "за запознаване"
				deleteNotif.setParameter("userId", codeRef);
				deleteNotif.setParameter("msgType", CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_ACCESS);
				deleteNotif.setParameter("objCode", CODE_ZNACHENIE_JOURNAL_DOC);
				deleteNotif.setParameter("objId", docId);

				deleteNotif.executeUpdate();

				@SuppressWarnings("unchecked")
				List<Object[]> docData = em.createNativeQuery( //
					"select RN_DOC, DOC_DATE, PORED_DELO from DOC where DOC_ID = ?1").setParameter(1, docId).getResultList();

				StringBuilder ident = new StringBuilder();
				ident.append("Отнет е достъпа на ");
				ident.append(sd.decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, codeRef, ud.getCurrentLang(), null));
				ident.append(" от " + sd.decodeItemDopInfo(Constants.CODE_CLASSIF_ADMIN_STR, codeRef, ud.getCurrentLang(), null));
				ident.append(" до документ ");
				if (docData.isEmpty()) {
					ident.append("с ИД=" + docId);
				} else {
					ident.append(DocDAO.formRnDocDate(docData.get(0)[0], docData.get(0)[1], docData.get(0)[2]));
				}
				ident.append(".");

				SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, docId, ident.toString());
				journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
				journal.setDateAction(new Date());
				journal.setIdUser(ud.getUserId());
				docDao.saveAudit(journal);
			}
		} catch (Exception e) {
			LOGGER.error("Грешка при Отнеманене на достъп", e);
			throw new DbErrorException("Грешка при Отнеманене на достъп", e);
		}
	}

	/**
	 * 5.8.5.3 Отнемане на достъп - Изтриване
	 *
	 * @param codeRef
	 * @throws ObjectInUseException
	 * @throws DbErrorException
	 */
	public void eraseAccess(Integer codeRef) throws ObjectInUseException, DbErrorException {
		try {
			EntityManager em = JPA.getUtil().getEntityManager();

			// първо проверявам дали не е водещ на активни преписки
			Number deloCnt = (Number) em.createNativeQuery( //
				"select count (*) CNT from DELO where CODE_REF_LEAD = :codeRef and STATUS in ( :stat1 , :stat2 )").setParameter("codeRef", codeRef) //
				.setParameter("stat1", CODE_ZNACHENIE_DELO_STATUS_ACTIVE).setParameter("stat2", CODE_ZNACHENIE_DELO_STATUS_CONTINUED) //
				.getResultList().get(0);

			if (deloCnt.intValue() > 0) {
				throw new ObjectInUseException("Достъпът на служителя не може да бъде изтрит, защото е определен за водещ на " + deloCnt
					+ " активни преписки. Моля, сменете водещият служител на тези преписки, след което ще можете да" + " изтриете достъпа на избрания служител.");
			}

			int cnt;

			cnt = em.createNativeQuery("delete from DOC_ACCESS where CODE_REF = ?1").setParameter(1, codeRef).executeUpdate();
			LOGGER.debug("delete {} rows from DOC_ACCESS for CODE_REF={}", cnt, codeRef);

			cnt = em.createNativeQuery("delete from DOC_ACCESS_ALL where CODE_REF = ?1").setParameter(1, codeRef).executeUpdate();
			LOGGER.debug("delete {} rows from DOC_ACCESS_ALL for CODE_REF={}", cnt, codeRef);

			cnt = em.createNativeQuery("delete from DELO_ACCESS where CODE_REF = ?1").setParameter(1, codeRef).executeUpdate();
			LOGGER.debug("delete {} rows from DELO_ACCESS for CODE_REF={}", cnt, codeRef);

			cnt = em.createNativeQuery("delete from DELO_ACCESS_ALL where CODE_REF = ?1").setParameter(1, codeRef).executeUpdate();
			LOGGER.debug("delete {} rows from DELO_ACCESS_ALL for CODE_REF={}", cnt, codeRef);

		} catch (ObjectInUseException e) {
			throw e; // трябва да е види усера на екрана
		} catch (Exception e) {
			LOGGER.error("Грешка при Изтриване на достъп", e);
			throw new DbErrorException("Грешка при Изтриване на достъп", e);
		}
	}

	/**
	 * Изтриване на достъпа на всички до това дело/преписка.
	 *
	 * @param deloId
	 * @throws DbErrorException
	 */
	public void eraseDeloAccess(Integer deloId) throws DbErrorException {
		try {
			EntityManager em = JPA.getUtil().getEntityManager();

			em.createNativeQuery("delete from DELO_ACCESS where DELO_ID = ?1").setParameter(1, deloId).executeUpdate();
			em.createNativeQuery("delete from DELO_ACCESS_ALL where DELO_ID = ?1").setParameter(1, deloId).executeUpdate();

		} catch (Exception e) {
			LOGGER.error("Грешка при изтриване на достъп на дело/преписка!");
			throw new DbErrorException(e);
		}
	}

	/**
	 * Изтриване на достъпа на всички до този документ.
	 *
	 * @param docId
	 * @throws DbErrorException
	 */
	public void eraseDocAccess(Integer docId) throws DbErrorException {
		try {
			EntityManager em = JPA.getUtil().getEntityManager();

			em.createNativeQuery("delete from DOC_ACCESS where DOC_ID = ?1").setParameter(1, docId).executeUpdate();
			em.createNativeQuery("delete from DOC_ACCESS_ALL where DOC_ID = ?1").setParameter(1, docId).executeUpdate();

		} catch (Exception e) {
			LOGGER.error("Грешка при изтриване на достъп на документ!");
			throw new DbErrorException(e);
		}
	}

	/**
	 * Преизгражда достъпа на потребител
	 *
	 * @param codeRef
	 * @param sd
	 * @param userData - този който изпълнява действието
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public void recreateUserAccessList(Integer codeRef, BaseSystemData sd, BaseUserData userData) throws DbErrorException {
		try {
			Date migDate = null; // дата на миграция като може и да няма ако при клиента не се налага миграция
			String migDateSetting = sd.getSettingsValue("delo.migrationDate");
			if (migDateSetting != null) {
				migDate = DateUtils.parse(migDateSetting);
			}

			EntityManager em = JPA.getUtil().getEntityManager();

			// ПЪРВО трябва да се оправи достъпа до преписките

			int deleted = em.createNativeQuery("delete from DELO_ACCESS_ALL where CODE_REF = :codeRef") //
				.setParameter("codeRef", codeRef).executeUpdate();
			LOGGER.debug("deleted {} rows DELO_ACCESS_ALL: CODE_REF={}", deleted, codeRef);

			StringBuilder sql = new StringBuilder();
			sql.append(" select d.DELO_ID, " + CODE_ZNACHENIE_JOURNAL_DELO + " OBJECT_CODE, d.DELO_ID OBJECT_ID, d.DATE_REG, d.DATE_LAST_MOD, 1 URF, d.REGISTRATURA_ID ");
			sql.append(" from DELO d where d.USER_REG = :codeRef "); // регистрирал
			sql.append(" union ");
			sql.append(" select d.DELO_ID, " + CODE_ZNACHENIE_JOURNAL_DELO + " OBJECT_CODE, d.DELO_ID OBJECT_ID, d.DATE_REG, d.DATE_LAST_MOD, 0 URF, d.REGISTRATURA_ID ");
			sql.append(" from DELO d where d.CODE_REF_LEAD = :codeRef "); // водещ
			sql.append(" union "); // + изричния достъп. само който не е забранен
			sql.append(" select da.DELO_ID, " + CODE_ZNACHENIE_JOURNAL_DELO + " OBJECT_CODE, d.DELO_ID OBJECT_ID, d.DATE_REG, d.DATE_LAST_MOD, 0 URF, d.REGISTRATURA_ID ");
			sql.append(" from DELO_ACCESS da inner join DELO d on d.DELO_ID = da.DELO_ID where da.CODE_REF = :codeRef and (da.EXCLUDE is null or da.EXCLUDE != 1) ");
			sql.append(" union "); // + движение на преписки
			sql.append(" select dd.DELO_ID, " + CODE_ZNACHENIE_JOURNAL_DELO_DVIJ + " OBJECT_CODE, dd.ID OBJECT_ID, dd.DATE_REG, dd.DATE_LAST_MOD, 0 URF, -1 REGISTRATURA_ID ");
			sql.append(" from DELO_DVIJ dd where dd.CODE_REF = :codeRef ");

			sql.append(" order by 1, 2, 3, 4 "); // това е важно

			// key=zveno,value=deloIds
			Map<Integer, Set<Integer>> deloIdMap = new HashMap<>(); // текущия с кое звено до кои преписки има достъп

			List<Object[]> rows = em.createNativeQuery(sql.toString()).setParameter("codeRef", codeRef).getResultList();

			Query insQuery = em.createNativeQuery("insert into DELO_ACCESS_ALL(DELO_ID, CODE_REF, CODE_STRUCT, OBJECT_CODE, OBJECT_ID) values(?1, ?2, ?3, ?4, ?5)");

			Set<String> keys = new HashSet<>(); // DELO_ID.OBJECT_CODE.OBJECT_ID.ZVENO
			for (Object[] row : rows) {
				int deloId = ((Number) row[0]).intValue();
				int objectCode = ((Number) row[1]).intValue();

				Integer zveno = recreateUserAccessFindZveno(codeRef, sd, migDate, row);

				String key = deloId + "." + objectCode + "." + row[2] + "." + zveno;
				if (keys.contains(key)) {
					continue;
				}
				keys.add(key);

				insQuery.setParameter(1, deloId);
				insQuery.setParameter(2, codeRef);
				insQuery.setParameter(3, zveno);
				insQuery.setParameter(4, objectCode);
				insQuery.setParameter(5, row[2]);

				insQuery.executeUpdate();

				if (objectCode == CODE_ZNACHENIE_JOURNAL_DELO) { // за другите няма да се прави рекурсия за вложените
					Set<Integer> set = deloIdMap.get(zveno);
					if (set == null) {
						set = new HashSet<>();
						deloIdMap.put(zveno, set);
					}
					set.add(deloId);
				}
			}

			// ПОСЛЕ и до документите

			deleted = em.createNativeQuery("delete from DOC_ACCESS_ALL where CODE_REF = :codeRef") //
				.setParameter("codeRef", codeRef).executeUpdate();
			LOGGER.debug("deleted {} rows DOC_ACCESS_ALL: CODE_REF={}", deleted, codeRef);

			sql.setLength(0);

			sql.append(" select d.DOC_ID, " + CODE_ZNACHENIE_JOURNAL_DOC + " OBJECT_CODE, d.DOC_ID OBJECT_ID, d.DATE_REG, d.DATE_LAST_MOD, 1 URF, d.REGISTRATURA_ID ");
			sql.append(" from DOC d where d.USER_REG = :codeRef "); // регистрирал
			sql.append(" union ");
			sql.append(" select dr.DOC_ID, " + CODE_ZNACHENIE_JOURNAL_DOC + " OBJECT_CODE, dr.DOC_ID OBJECT_ID, dr.DATE_REG, dr.DATE_LAST_MOD, 0 URF, -1 REGISTRATURA_ID ");
			sql.append(" from DOC_REFERENTS dr where dr.CODE_REF = :codeRef "); // хората по документа
			sql.append(" union "); // изричния достъп. само който не е забранен
			sql.append(" select da.DOC_ID, " + CODE_ZNACHENIE_JOURNAL_DOC + " OBJECT_CODE, d.DOC_ID OBJECT_ID, d.DATE_REG, d.DATE_LAST_MOD, 0 URF, d.REGISTRATURA_ID");
			sql.append(" from DOC_ACCESS da inner join DOC d on d.DOC_ID = da.DOC_ID where da.CODE_REF = :codeRef and (da.EXCLUDE is null or da.EXCLUDE != 1) ");

			sql.append(" union "); // движение на документ
			sql.append(" select dd.DOC_ID, " + CODE_ZNACHENIE_JOURNAL_DOC_DVIJ + " OBJECT_CODE, dd.ID OBJECT_ID, dd.DATE_REG, dd.DATE_LAST_MOD, 0 URF, -1 REGISTRATURA_ID ");
			sql.append(" from DOC_DVIJ dd where dd.CODE_REF = :codeRef ");

			sql.append(" union "); // всички от задачите по документа
			sql.append(" select t.DOC_ID, " + CODE_ZNACHENIE_JOURNAL_TASK + " OBJECT_CODE, t.TASK_ID OBJECT_ID, t.DATE_REG, t.DATE_LAST_MOD, 1 URF, t.REGISTRATURA_ID ");
			sql.append(" from TASK T where t.USER_REG = :codeRef and t.DOC_ID is not null "); // регистрирал
			sql.append(" union ");
			sql.append(" select t.DOC_ID, " + CODE_ZNACHENIE_JOURNAL_TASK + " OBJECT_CODE, t.TASK_ID OBJECT_ID, t.DATE_REG, t.DATE_LAST_MOD, 0 URF, t.REGISTRATURA_ID ");
			sql.append(" from TASK T where t.CODE_ASSIGN = :codeRef and t.CODE_ASSIGN != t.USER_REG and t.DOC_ID is not null "); // възложител
			sql.append(" union ");
			sql.append(" select t.DOC_ID, " + CODE_ZNACHENIE_JOURNAL_TASK + " OBJECT_CODE, t.TASK_ID OBJECT_ID, t.DATE_REG, t.DATE_LAST_MOD, 0 URF, t.REGISTRATURA_ID ");
			sql.append(" from TASK T where t.CODE_CONTROL = :codeRef and t.DOC_ID is not null "); // контролиращ
			sql.append(" union "); // + изпълнителите
			sql.append(" select t.DOC_ID, " + CODE_ZNACHENIE_JOURNAL_TASK + " OBJECT_CODE, t.TASK_ID OBJECT_ID, tr.DATE_REG, t.DATE_LAST_MOD, 0 URF, t.REGISTRATURA_ID ");
			sql.append(" from TASK_REFERENTS tr inner join TASK T on t.TASK_ID = tr.TASK_ID where tr.CODE_REF = :codeRef and t.DOC_ID is not null ");

			sql.append(" union "); // всички от задачите по документа, където документа е резултатен
			sql.append(" select t.END_DOC_ID, " + CODE_ZNACHENIE_JOURNAL_TASK + " OBJECT_CODE, t.TASK_ID OBJECT_ID, t.DATE_REG, t.DATE_LAST_MOD, 1 URF, t.REGISTRATURA_ID ");
			sql.append(" from TASK T where t.USER_REG = :codeRef and t.END_DOC_ID is not null "); // регистрирал
			sql.append(" union ");
			sql.append(" select t.END_DOC_ID, " + CODE_ZNACHENIE_JOURNAL_TASK + " OBJECT_CODE, t.TASK_ID OBJECT_ID, t.DATE_REG, t.DATE_LAST_MOD, 0 URF, t.REGISTRATURA_ID ");
			sql.append(" from TASK T where t.CODE_ASSIGN = :codeRef and t.CODE_ASSIGN != t.USER_REG and t.END_DOC_ID is not null "); // възложител
			sql.append(" union ");
			sql.append(" select t.END_DOC_ID, " + CODE_ZNACHENIE_JOURNAL_TASK + " OBJECT_CODE, t.TASK_ID OBJECT_ID, t.DATE_REG, t.DATE_LAST_MOD, 0 URF, t.REGISTRATURA_ID ");
			sql.append(" from TASK T where t.CODE_CONTROL = :codeRef and t.END_DOC_ID is not null "); // контролиращ
			sql.append(" union "); // + изпълнителите
			sql.append(" select t.END_DOC_ID, " + CODE_ZNACHENIE_JOURNAL_TASK + " OBJECT_CODE, t.TASK_ID OBJECT_ID, tr.DATE_REG, t.DATE_LAST_MOD, 0 URF, t.REGISTRATURA_ID ");
			sql.append(" from TASK_REFERENTS tr inner join TASK T on t.TASK_ID = tr.TASK_ID where tr.CODE_REF = :codeRef and t.END_DOC_ID is not null ");

			sql.append(" order by 1, 2, 3, 4 "); // това е важно

			rows = em.createNativeQuery(sql.toString()).setParameter("codeRef", codeRef).getResultList();

			insQuery = em.createNativeQuery("insert into DOC_ACCESS_ALL(DOC_ID, CODE_REF, CODE_STRUCT, OBJECT_CODE, OBJECT_ID) values(?1, ?2, ?3, ?4, ?5)");

			keys.clear(); // DOC_ID.OBJECT_CODE.OBJECT_ID.ZVENO
			for (Object[] row : rows) {

				Integer zveno = recreateUserAccessFindZveno(codeRef, sd, migDate, row);

				String key = row[0] + "." + row[1] + "." + row[2] + "." + zveno;
				if (keys.contains(key)) {
					continue;
				}
				keys.add(key);

				insQuery.setParameter(1, row[0]);
				insQuery.setParameter(2, codeRef);
				insQuery.setParameter(3, zveno);
				insQuery.setParameter(4, row[1]);
				insQuery.setParameter(5, row[2]);

				insQuery.executeUpdate();
			}

			// И НАКРАЯ за вложените преписки и документи през влаганията и звеното определено за основната преписка
			for (Entry<Integer, Set<Integer>> entry : deloIdMap.entrySet()) {
				Set<Integer> processedIds = new HashSet<>(); // за да не зациклим

				for (Integer deloId : entry.getValue()) {

					recreateUserAccessRecursive(codeRef, entry.getKey(), deloId, processedIds); // рекурсивно за вложените
																								// преписки
				}
			}

			StringBuilder ident = new StringBuilder();
			ident.append("Достъпът на потребител ");
			ident.append(sd.decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, codeRef, userData.getCurrentLang(), null));
			ident.append(" от " + sd.decodeItemDopInfo(Constants.CODE_CLASSIF_ADMIN_STR, codeRef, userData.getCurrentLang(), null));
			ident.append(" е преопределен. ");

			SystemJournal journal = new SystemJournal(Constants.CODE_ZNACHENIE_JOURNAL_USER, codeRef, ident.toString());

			journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
			journal.setDateAction(new Date());
			journal.setIdUser(userData.getUserId());

			new AdmUserDAO(userData).saveAudit(journal);

		} catch (Exception e) {
			throw new DbErrorException("Грешка при преизграждане на достъп на потребител с CODE=" + codeRef, e);
		}
	}

	/**
	 * Премахва достъпа до преписка на хората, които са получили достъп за това че имат достъп до преписката в която е вложена
	 *
	 * @param deloId
	 * @param fromDeloId
	 * @param set        за да не се зациклим
	 * @throws DbErrorException
	 */
	public void removeDeloDostFromDelo(Integer deloId, Integer fromDeloId, Set<Integer> set) throws DbErrorException {
		set.add(fromDeloId);
		try {
			Query query = JPA.getUtil().getEntityManager().createNativeQuery( //
				"delete from DELO_ACCESS_ALL where DELO_ID = ?1 and OBJECT_CODE = ?2 and OBJECT_ID = ?3") //
				.setParameter(1, deloId).setParameter(2, CODE_ZNACHENIE_JOURNAL_DELO).setParameter(3, fromDeloId);
			query.executeUpdate();

			recursiveDeloDost(deloId, set); // пускаме рекурсивно надолу

		} catch (Exception e) {
			LOGGER.error("Грешка при премахване достъп на дело от дело/преписка!");
			throw new DbErrorException(e);
		}
	}

	/**
	 * 5.8.5.3 Отнемане на достъп - Премахване на Изрично отнемане до документ/преписка
	 *
	 * @param selectedIds
	 * @param codeRef
	 * @param forDoc      <code>true</code> - DOC_ACCESS, <code>false</code> - DELO_ACCESS
	 * @param sd
	 * @param ud
	 * @throws DbErrorException
	 */
	public void removeDeniedAccess(List<Integer> selectedIds, Integer codeRef, boolean forDoc, BaseSystemData sd, BaseUserData ud) throws DbErrorException {
		try {
			DeloDAO deloDao = new DeloDAO(ud); // без значение е дали е док или дело даото
			EntityManager em = JPA.getUtil().getEntityManager();

			String sql;
			if (forDoc) {
				sql = "select da, d.id, d.rnDoc, d.docDate, d.poredDelo from DocAccess da inner join Doc d on d.id = da.docId where d.id in (?1) and da.codeRef = ?2";
			} else {
				sql = "select da, d.id, d.rnDelo, d.deloDate from DeloAccess da inner join Delo d on d.id = da.deloId where d.id in (?1) and da.codeRef = ?2";
			}
			@SuppressWarnings("unchecked")
			List<Object[]> rows = em.createQuery(sql).setParameter(1, selectedIds).setParameter(2, codeRef).getResultList();
			for (Object[] row : rows) {
				em.remove(row[0]); // изтривам отнетия достъп

				StringBuilder ident = new StringBuilder();
				ident.append("Премахнато е изричното отнемане на достъп на ");
				ident.append(sd.decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, codeRef, ud.getCurrentLang(), null));
				ident.append(" от " + sd.decodeItemDopInfo(Constants.CODE_CLASSIF_ADMIN_STR, codeRef, ud.getCurrentLang(), null));
				if (forDoc) {
					ident.append(" до документ ");
					ident.append(DocDAO.formRnDocDate(row[2], row[3], row[4]));
				} else {
					ident.append(" до преписка ");
					ident.append(row[2] + "/" + DateUtils.printDate((Date) row[3]));
					
				}
				ident.append(".");

				int cnst = forDoc ? OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC : OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO;

				SystemJournal journal = new SystemJournal(cnst, ((Number) row[1]).intValue(), ident.toString());
				journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
				journal.setDateAction(new Date());
				journal.setIdUser(ud.getUserId());
				deloDao.saveAudit(journal);
			}

			if (!forDoc) { // трява да се премахне и до вложените документи в преписките
				@SuppressWarnings("unchecked")
				List<Integer> docIds = em.createQuery("select dd.inputDocId from DeloDoc dd where dd.deloId in (:deloIds)").setParameter("deloIds", selectedIds).getResultList();

				if (!docIds.isEmpty()) {
					removeDeniedAccess(docIds, codeRef, true, sd, ud);
				}
			}

		} catch (Exception e) {
			LOGGER.error("Грешка при Премахване на отнет на достъп", e);
			throw new DbErrorException("Грешка при Премахване на отнет на достъп", e);
		}
	}

	/**
	 * Премахва достъпа до документ на хората, които са получили достъп за това че имат достъп до подадения обект
	 *
	 * @param docId
	 * @param codeObject
	 * @param idObject
	 * @throws DbErrorException
	 */
	public void removeDocDostFromObject(Integer docId, Integer codeObject, Integer idObject) throws DbErrorException {
		try {
			Query query = JPA.getUtil().getEntityManager().createNativeQuery( //
				"DELETE FROM DOC_ACCESS_ALL WHERE DOC_ID = ?1 and OBJECT_CODE = ?2 and OBJECT_ID = ?3") //
				.setParameter(1, docId).setParameter(2, codeObject).setParameter(3, idObject);
			query.executeUpdate();

		} catch (Exception e) {
			LOGGER.error("Грешка при премахване достъп на документ от обект({})!", codeObject);
			throw new DbErrorException(e);
		}
	}

	/**
	 * 5.8.5.2 Прехвърляне на достъп
	 *
	 * @param codeRefFrom
	 * @param codeRefTo
	 * @param systemData
	 * @param keepAccess  да се пази ли достъпа на codeRefFrom - да/не
	 * @param userData    - този който изпълнява действието
	 * @throws DbErrorException
	 */
	public void transferAccess(Integer codeRefFrom, Integer codeRefTo, BaseSystemData systemData, boolean keepAccess, BaseUserData userData) throws DbErrorException {
		Integer zvenoTo = systemData.getItemParentCode(CODE_CLASSIF_ADMIN_STR, codeRefTo, new Date());

		try {
			EntityManager em = JPA.getUtil().getEntityManager();

			StringBuilder sqlDoc = new StringBuilder();
			sqlDoc.append(" select distinct a.DOC_ID ");
			sqlDoc.append(" from DOC_ACCESS_ALL a ");
			sqlDoc.append(" left outer join DOC_ACCESS otnet on otnet.DOC_ID = a.DOC_ID and otnet.CODE_REF = a.CODE_REF and otnet.EXCLUDE = :da ");
			sqlDoc.append(" where a.CODE_REF = :codeRefFrom ");
			sqlDoc.append(" and a.DOC_ID not in ( ");
			sqlDoc.append(" 	select DOC_ID from DOC_ACCESS a2         where a2.CODE_REF = :codeRefTo ");
			sqlDoc.append(" 	union ");
			sqlDoc.append(" 	select DOC_ID from DOC_ACCESS_ALL all2   where all2.CODE_REF = :codeRefTo ");
			sqlDoc.append(" ) ");
			sqlDoc.append(" and otnet.ID is null "); // да няма отнет достъп

			@SuppressWarnings("unchecked")
			List<Object> docIdList = em.createNativeQuery(sqlDoc.toString()).setParameter("codeRefFrom", codeRefFrom).setParameter("codeRefTo", codeRefTo).setParameter("da", CODE_ZNACHENIE_DA)
				.getResultList();

			Query docAccessAll = em.createNativeQuery( //
				"INSERT INTO DOC_ACCESS_ALL(DOC_ID, CODE_REF, CODE_STRUCT, OBJECT_CODE, OBJECT_ID) VALUES (?1, ?2, ?3, ?4, ?5)");

			for (Object id : docIdList) {
				DocAccess da = new DocAccess();

				da.setCodeRef(codeRefTo);
				da.setExclude(CODE_ZNACHENIE_NE);
				da.setDocId(((Number) id).intValue());

				em.persist(da);

				// запис в DOC_ACCESS_ALL
				docAccessAll.setParameter(1, da.getDocId());
				docAccessAll.setParameter(2, codeRefTo);
				docAccessAll.setParameter(3, zvenoTo);
				docAccessAll.setParameter(4, CODE_ZNACHENIE_JOURNAL_DOC);
				docAccessAll.setParameter(5, da.getDocId());
				docAccessAll.executeUpdate();
			}

			StringBuilder sqlDelo = new StringBuilder();
			sqlDelo.append(" select distinct a.DELO_ID ");
			sqlDelo.append(" from DELO_ACCESS_ALL a ");
			sqlDelo.append(" left outer join DELO_ACCESS otnet on otnet.DELO_ID = a.DELO_ID and otnet.CODE_REF = a.CODE_REF and otnet.EXCLUDE = :da ");
			sqlDelo.append(" where a.CODE_REF = :codeRefFrom ");
			sqlDelo.append(" and a.DELO_ID not in ( ");
			sqlDelo.append(" 	select DELO_ID from DELO_ACCESS a2         where a2.CODE_REF = :codeRefTo ");
			sqlDelo.append(" 	union ");
			sqlDelo.append(" 	select DELO_ID from DELO_ACCESS_ALL all2   where all2.CODE_REF = :codeRefTo ");
			sqlDelo.append(" ) ");
			sqlDelo.append(" and otnet.ID is null "); // да няма отнет достъп

			@SuppressWarnings("unchecked")
			List<Object> deloIdList = em.createNativeQuery(sqlDelo.toString()) //
				.setParameter("codeRefFrom", codeRefFrom).setParameter("codeRefTo", codeRefTo) //
				.setParameter("da", CODE_ZNACHENIE_DA).getResultList();

			Query deloAccessAll = em.createNativeQuery("INSERT INTO DELO_ACCESS_ALL(DELO_ID, CODE_REF, CODE_STRUCT, OBJECT_CODE, OBJECT_ID) VALUES(?1, ?2, ?3, ?4, ?5)");

			for (Object id : deloIdList) {
				DeloAccess da = new DeloAccess();

				da.setCodeRef(codeRefTo);
				da.setExclude(CODE_ZNACHENIE_NE);
				da.setDeloId(((Number) id).intValue());

				em.persist(da);

				// запис в DELO_ACCESS_ALL
				deloAccessAll.setParameter(1, da.getDeloId());
				deloAccessAll.setParameter(2, codeRefTo);
				deloAccessAll.setParameter(3, zvenoTo);
				deloAccessAll.setParameter(4, CODE_ZNACHENIE_JOURNAL_DELO);
				deloAccessAll.setParameter(5, da.getDeloId());
				deloAccessAll.executeUpdate();
			}

			if (!keepAccess) { // трия на първия всичко
				int cnt;

				cnt = em.createNativeQuery("delete from DOC_ACCESS where CODE_REF = ?1").setParameter(1, codeRefFrom).executeUpdate();
				LOGGER.debug("delete {} rows from DOC_ACCESS for CODE_REF={}", cnt, codeRefFrom);

				cnt = em.createNativeQuery("delete from DOC_ACCESS_ALL where CODE_REF = ?1").setParameter(1, codeRefFrom).executeUpdate();
				LOGGER.debug("delete {} rows from DOC_ACCESS_ALL for CODE_REF={}", cnt, codeRefFrom);

				cnt = em.createNativeQuery("delete from DELO_ACCESS where CODE_REF = ?1").setParameter(1, codeRefFrom).executeUpdate();
				LOGGER.debug("delete {} rows from DELO_ACCESS for CODE_REF={}", cnt, codeRefFrom);

				cnt = em.createNativeQuery("delete from DELO_ACCESS_ALL where CODE_REF = ?1").setParameter(1, codeRefFrom).executeUpdate();
				LOGGER.debug("delete {} rows from DELO_ACCESS_ALL for CODE_REF={}", cnt, codeRefFrom);
			}

			AdmUserDAO userDao = new AdmUserDAO(userData);

			StringBuilder ident = new StringBuilder();
			ident.append("Достъпът на потребител ");
			ident.append(systemData.decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, codeRefFrom, userData.getCurrentLang(), null));
			ident.append(" от " + systemData.decodeItemDopInfo(Constants.CODE_CLASSIF_ADMIN_STR, codeRefFrom, userData.getCurrentLang(), null));
			ident.append(" е прехвърлен на ");
			ident.append(systemData.decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, codeRefTo, userData.getCurrentLang(), null));
			ident.append(" от " + systemData.decodeItemDopInfo(Constants.CODE_CLASSIF_ADMIN_STR, codeRefTo, userData.getCurrentLang(), null));
			ident.append(".");

			SystemJournal jFrom = new SystemJournal(Constants.CODE_ZNACHENIE_JOURNAL_USER, codeRefFrom, ident.toString());
			jFrom.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
			jFrom.setDateAction(new Date());
			jFrom.setIdUser(userData.getUserId());
			userDao.saveAudit(jFrom);

			SystemJournal jTo = new SystemJournal(Constants.CODE_ZNACHENIE_JOURNAL_USER, codeRefTo, ident.toString());
			jTo.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
			jTo.setDateAction(new Date());
			jTo.setIdUser(userData.getUserId());
			userDao.saveAudit(jTo);

		} catch (Exception e) {
			LOGGER.error("Грешка при Прехвърляне на достъп", e);
			throw new DbErrorException("Грешка при Прехвърляне на достъп", e);
		}
	}

	/**
	 * По време на преизграждането на достъпа определя правилното звено, което трябва да се зададе в достъпа
	 */
	private Integer recreateUserAccessFindZveno(Integer codeRef, BaseSystemData sd, Date migDate, Object[] row) throws DbErrorException {
		Date dateReg = (Date) row[3];

		if (migDate == null || migDate.getTime() < dateReg.getTime()) {
			int userRegFlag = ((Number) row[5]).intValue();
			if (userRegFlag == 1) { // това е регистриралия и трябва да проверя каква е била регистратурата му по време на
									// регистриране на обекта
				Integer admRegistratura = (Integer) sd.getItemSpecific(CODE_CLASSIF_ADMIN_STR, codeRef, SysConstants.CODE_DEFAULT_LANG, dateReg, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
				if (admRegistratura != null && !admRegistratura.equals(((Number) row[6]).intValue())) {
					return codeRef; // личен достъп
				}
			}

			Integer zveno = sd.getItemParentCode(CODE_CLASSIF_ADMIN_STR, codeRef, dateReg); // пробвам към дата рег
			if (zveno != null) {
				return zveno;
			}
			zveno = sd.getItemParentCode(CODE_CLASSIF_ADMIN_STR, codeRef, (Date) row[4]); // после към дата мод
			if (zveno != null) {
				return zveno;
			}
			zveno = sd.getItemParentCode(CODE_CLASSIF_ADMIN_STR, codeRef, null); // и накрая към днешна дата
			if (zveno != null) {
				return zveno;
			}
			return -200;// този е някакъв фантом
		}

		return -100; // значи са данни от миграция
	}

	/**
	 * Преизграждане на достъп за служител през вложени преписки/документи
	 */
	private void recreateUserAccessRecursive(Integer codeRef, Integer zveno, Integer deloId, Set<Integer> processedIds) throws DbErrorException {
		if (processedIds.contains(deloId)) {
			return;
		}
		processedIds.add(deloId);

		EntityManager em = JPA.getUtil().getEntityManager();

		@SuppressWarnings("unchecked")
		List<Object> inputDeloIds = em.createNativeQuery("select distinct INPUT_DELO_ID from DELO_DELO where DELO_ID = ?1") //
			.setParameter(1, deloId).getResultList();

		for (Object t : inputDeloIds) {
			int inputDeloId = ((Number) t).intValue();

			Query deloInsQuery = em.createNativeQuery("insert into DELO_ACCESS_ALL(DELO_ID, CODE_REF, CODE_STRUCT, OBJECT_CODE, OBJECT_ID) VALUES(?1, ?2, ?3, ?4, ?5)") //
				.setParameter(1, inputDeloId).setParameter(2, codeRef).setParameter(3, zveno) //
				.setParameter(4, CODE_ZNACHENIE_JOURNAL_DELO).setParameter(5, deloId);
			deloInsQuery.executeUpdate();

			recreateUserAccessRecursive(codeRef, zveno, inputDeloId, processedIds);
		}

		// прекопирам достъпа и на всички вложени документи в текущата преписка
		StringBuilder docIns = new StringBuilder();
		docIns.append(" insert into DOC_ACCESS_ALL(DOC_ID, CODE_REF, CODE_STRUCT, OBJECT_CODE, OBJECT_ID) ");
		docIns.append(" select distinct dd.INPUT_DOC_ID, " + codeRef + ", " + zveno + ", " + CODE_ZNACHENIE_JOURNAL_DELO + ", dd.DELO_ID ");
		docIns.append(" from DELO_DOC dd where dd.DELO_ID = ?1");

		Query docInsQuery = em.createNativeQuery(docIns.toString()).setParameter(1, deloId);
		docInsQuery.executeUpdate();
	}

	/**
	 * @param deloId
	 * @param set
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	private void recursiveDeloDost(Integer deloId, Set<Integer> set) throws DbErrorException {
		EntityManager em = JPA.getUtil().getEntityManager();

		// дела/преписки
		List<Object> ids = em.createNativeQuery("select INPUT_DELO_ID from DELO_DELO where DELO_ID = ?1") //
			.setParameter(1, deloId).getResultList();
		for (Object id : ids) {
			int tmp = ((Number) id).intValue();
			if (set.contains(tmp)) {
				continue; // ако през нещо вече е минато няма да се продължава
			}
			addDeloDostFromDelo(tmp, deloId, set);
		}

		// документи
		ids = em.createNativeQuery("select INPUT_DOC_ID from DELO_DOC where DELO_ID = ?2") //
			.setParameter(2, deloId).getResultList();
		for (Object id : ids) {
			addDocDostFromDelo(((Number) id).intValue(), deloId);
		}
	}

}
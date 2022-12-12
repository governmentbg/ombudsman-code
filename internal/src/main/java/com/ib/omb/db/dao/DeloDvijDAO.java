package com.ib.omb.db.dao;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Query;

import org.hibernate.jpa.TypedParameterValue;
import org.hibernate.type.StandardBasicTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.omb.db.dto.DeloDvij;
import com.ib.omb.db.dto.DocDvij;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.utils.DocDostUtils;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link DeloDvij}
 *
 * @author belev
 */
public class DeloDvijDAO extends AbstractDAO<DeloDvij> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DeloDvijDAO.class);
	
	/** @param user */
	public DeloDvijDAO(ActiveUser user) {
		super(DeloDvij.class, user);
	}
	
	/** */
	@Override
	public DeloDvij findById(Object id) throws DbErrorException {
		DeloDvij dvij = super.findById(id);
		if (dvij != null) {
			dvij.setDbCodeRef(dvij.getCodeRef());
			dvij.setDbDvijDate(dvij.getDvijDate());
		}
		return dvij;
	}

	
	@SuppressWarnings("unchecked")
	public List<DeloDvij> getDeloDvij(Integer deloId) throws DbErrorException{
		try {
			List<DeloDvij> list = JPA.getUtil().getEntityManager().createQuery("from DeloDvij where deloId = :idd order by dvijDate").setParameter("idd", deloId).getResultList();
			if (list.isEmpty()) {
				return list;
			}
			for (DeloDvij dvij : list) { // не е много добре така, но не се вика финдБъИд на отваряне
				dvij.setDbCodeRef(dvij.getCodeRef());
				dvij.setDbDvijDate(dvij.getDvijDate());
			}
			return list;
		} catch (Exception e) {
			LOGGER.error("Грешка при извличане на движения на дела !");
			throw new DbErrorException("Грешка при извличане на движения на дела !",e);
		}
		
	}
	
	
	@SuppressWarnings("unchecked")
	public void delete(DeloDvij entity, SystemData systemData) throws DbErrorException, ObjectInUseException {
		List<DeloDvij> listDeloDvij = null;
		List<DocDvij> listDocDvij = null;
		try {
			listDeloDvij = createQuery(
				"select dd from DeloDvij dd where dd.fromDvijId = ?1").setParameter(1, entity.getId()).getResultList();
			listDocDvij = createQuery(
				"select dd from DocDvij dd where dd.fromDvijId = ?1").setParameter(1, entity.getId()).getResultList();
			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани движения!", e);
		}

		// първо изтривм ако има произлезли от това движение
		if (!listDeloDvij.isEmpty()) {
			for (DeloDvij deloDvij : listDeloDvij) {
				super.delete(deloDvij);
			}
		}
		if (!listDocDvij.isEmpty()) {
			DocDvijDAO docDvijDao = new DocDvijDAO(getUser());
			for (DocDvij docDvij : listDocDvij) {
				docDvijDao.delete(docDvij, systemData);
			}
		}
		
		super.delete(entity);
	}

	public DeloDvij save(DeloDvij dvij, BaseSystemData systemData) throws DbErrorException {
		dvij = save(dvij, false, (SystemData) systemData);
		
		dvij.setDbCodeRef(dvij.getCodeRef());
		dvij.setDbDvijDate(dvij.getDvijDate());

		return dvij;
	}
	
	
	
	private DeloDvij save(DeloDvij dvij, boolean isSimpleSave, SystemData systemData) throws DbErrorException {
		
		boolean isNew = false;
		if (dvij.getId() == null) {
			isNew = true;
			dvij.setStatus(OmbConstants.DS_SENT);
			dvij.setStatusDate(new Date());
		}
		
		Integer oldCodeRef = dvij.getDbCodeRef(); // мерге го замазва
		Date oldDvijDate = dvij.getDbDvijDate(); // мерге го замазва

		dvij = super.save(dvij);
		
		if (isSimpleSave) { // обикновен запис на свързано дело от рекурсията
			if (isNew // само за нов
				&& dvij.getCodeRef() != null // ако има
				&& systemData.matchClassifItems(Constants.CODE_CLASSIF_ADMIN_STR, dvij.getCodeRef(), dvij.getDvijDate())) { // само ако е от АДМ 

				// тука пускам като ново дело за да не прави рекурсията с достъпите
				new DocDostUtils().addRemoveDeloDost(dvij.getDeloId(), true, dvij.getCodeMainObject(), dvij.getId()
					, null, Collections.singleton(dvij.getCodeRef()), systemData);
			}
		} else { // запис извикан от екрана
			Set<Integer> forInsert = null;
			Set<Integer> forDelete = null;
			
			if (dvij.getCodeRef() != null // само ако има
				&& (isNew || !dvij.getCodeRef().equals(oldCodeRef)) // нов или промяна
				&& systemData.matchClassifItems(Constants.CODE_CLASSIF_ADMIN_STR, dvij.getCodeRef(), dvij.getDvijDate())) { // само ако е от АДМ 

				forInsert = Collections.singleton(dvij.getCodeRef());
				
				if (oldCodeRef != null) {
					forDelete = Collections.singleton(oldCodeRef);
				}

				new DocDostUtils().addRemoveDeloDost(dvij.getDeloId(), true, dvij.getCodeMainObject(), dvij.getId()
					, forDelete, forInsert , systemData);
			}
			
			if(isNew) {
				Set<Integer> deloIds = new HashSet<>();
				deloIds.add(dvij.getDeloId());
	
				recInsertDvigs(dvij.getDeloId(), dvij, deloIds, dvij.getTomNomer(), systemData);
			} else {
				updateVlDela(dvij, forInsert, forDelete, systemData, oldDvijDate);
			}
		}
		
		return dvij;
		
	}



	private DocDvij convertDeloToDocDvij(DeloDvij deloDvij, Integer docId, Integer ekzNomer) {
		
		if (deloDvij == null) {
			return null;
		}
		
		DocDvij docDvij = new DocDvij();
		
		docDvij.setCodeRef(deloDvij.getCodeRef());
		docDvij.setDvijDate(deloDvij.getDvijDate());
		docDvij.setDvijEmail(null);
		docDvij.setDvijInfo(deloDvij.getDvijInfo());
		docDvij.setDvijMethod(deloDvij.getDvijMethod());
		docDvij.setDvijText(deloDvij.getDvijText());		
		docDvij.setForRegid(null);
		//docDvij.setMessageGuid(null);
		docDvij.setOtherDocId(null);
		docDvij.setReturnCodeRef(deloDvij.getReturnCodeRef());
		docDvij.setReturnDate(deloDvij.getReturnDate());
		docDvij.setReturnInfo(deloDvij.getReturnInfo());
		docDvij.setReturnMethod(deloDvij.getReturnMethod());
		docDvij.setReturnTextRef(deloDvij.getReturnTextRef());
		docDvij.setReturnToDate(deloDvij.getReturnToDate());
		docDvij.setStatus(deloDvij.getStatus());
		docDvij.setStatusDate(deloDvij.getStatusDate());
		docDvij.setStatusText(deloDvij.getStatusText());
		
		docDvij.setDocId(docId);
		docDvij.setEkzNomer(ekzNomer);
		
		return docDvij;
	}
	
	
	
	@SuppressWarnings("unchecked")
	private void recInsertDvigs(Integer deloId, DeloDvij deloDvij, Set<Integer> deloIds, Integer tomNomer, SystemData systemData) throws DbErrorException {
		
		if (deloDvij == null) {
			return;
		}
		
		Query q;
		
		if (tomNomer == null) {
			q = JPA.getUtil().getEntityManager().createNativeQuery("select distinct INPUT_DELO_ID from DELO_DELO where DELO_ID = :IDD");
			q.setParameter("IDD", deloId);
		} else {
			q = JPA.getUtil().getEntityManager().createNativeQuery("select distinct INPUT_DELO_ID from DELO_DELO where DELO_ID = :IDD and TOM_NOMER = :TOMNOM");
			q.setParameter("IDD", deloId).setParameter("TOMNOM", tomNomer);
		}
		
		List<Object> resultIds = q.getResultList();
		for (Object tek : resultIds) {
			Integer id = SearchUtils.asInteger(tek);
			if (!deloIds.contains(id)) {
				
				DeloDvij newDvij = deloDvij.clone();
				newDvij.setId(null);
				newDvij.setTomNomer(null); // вътрешните занимават целите
				newDvij.setDeloId(id);
				if (deloDvij.getFromDvijId() == null) {
					newDvij.setFromDvijId(deloDvij.getId());
				} else {
					newDvij.setFromDvijId(deloDvij.getFromDvijId()); // така предаваме при влагане !!!
				}
				save(newDvij, true, systemData);
				
				deloIds.add(id);
				recInsertDvigs(id, deloDvij, deloIds, null, systemData); // на вътре тома не играе
			}
		}
		
		
		if (tomNomer == null) {
			q = JPA.getUtil().getEntityManager().createNativeQuery("select distinct INPUT_DOC_ID, EKZ_NOMER from DELO_DOC where DELO_ID = :IDD");
			q.setParameter("IDD", deloId);
		} else {
			q = JPA.getUtil().getEntityManager().createNativeQuery("select distinct INPUT_DOC_ID, EKZ_NOMER from DELO_DOC where DELO_ID = :IDD and TOM_NOMER = :TOMNOM");
			q.setParameter("IDD", deloId).setParameter("TOMNOM", tomNomer);
		}
		
		List<Object[]> resultRows = q.getResultList();
		for (Object[] tek : resultRows) {
			Integer id = SearchUtils.asInteger(tek[0]);
			Integer ekzNomer = SearchUtils.asInteger(tek[1]);
			
			DocDvij newDvij = convertDeloToDocDvij(deloDvij, id, ekzNomer);
			newDvij.setId(null);	
			if (deloDvij.getFromDvijId() == null) {
				newDvij.setFromDvijId(deloDvij.getId());
			} else {
				newDvij.setFromDvijId(deloDvij.getFromDvijId()); // така предаваме при влагане !!!
			}
			new DocDvijDAO(getUser()).save(newDvij, systemData, null);	
		}
	}
	
	@SuppressWarnings("unchecked")
	private void updateVlDela(DeloDvij dvij, Set<Integer> forInsert, Set<Integer> forDelete, SystemData sd, Date oldDvijDate) throws DbErrorException {
		String dateSql = "		DVIJ_DATE= ?2, ";
		boolean dateParams = false;
		
		if (oldDvijDate != null && dvij.getDvijDate() != null && oldDvijDate.getTime() != dvij.getDvijDate().getTime()) {
			dateParams = true; // има смяна на датата на движението
			dateSql = " DVIJ_DATE = ( case when DVIJ_DATE = ?18 or DVIJ_DATE < ?19 then ?2 else DVIJ_DATE end ), ";
		}
		try {
			String sql = "UPDATE DELO_DVIJ "  
					+ "	SET CODE_REF= ?1, "
					+ dateSql
					+ "		DVIJ_METHOD= ?3, "
					+ "		DVIJ_TEXT= ?4, "
					+ "		DVIJ_INFO= ?5, "
					+ "		RETURN_TO_DATE= ?6, "
					+ "		STATUS= ?7, "
					+ "		STATUS_DATE= ?8, "
					+ "		STATUS_TEXT= ?9, "
					+ "		RETURN_DATE= ?10, "
					+ "		RETURN_CODE_REF= ?11, "
					+ "		RETURN_TEXT_REF= ?12, "
					+ "		RETURN_METHOD= ?13, "
					+ "		RETURN_INFO= ?14, "
					+ "		USER_LAST_MOD= ?15, "
					+ "		DATE_LAST_MOD = ?16 "			 
					+ "	WHERE FROM_DVIJ_ID = ?17"; 
			
			Query q = JPA.getUtil().getEntityManager().createNativeQuery(sql);
			q.setParameter(1, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getCodeRef()) );
			q.setParameter(2, new TypedParameterValue(StandardBasicTypes.DATE, dvij.getDvijDate()));
			q.setParameter(3, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getDvijMethod()));
			q.setParameter(4, new TypedParameterValue(StandardBasicTypes.STRING, dvij.getDvijText()));
			q.setParameter(5, new TypedParameterValue(StandardBasicTypes.STRING, dvij.getDvijInfo()));
			q.setParameter(6, new TypedParameterValue(StandardBasicTypes.DATE, dvij.getReturnToDate()));
			q.setParameter(7, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getStatus()));
			q.setParameter(8, new TypedParameterValue(StandardBasicTypes.DATE, dvij.getStatusDate()));
			q.setParameter(9, new TypedParameterValue(StandardBasicTypes.STRING, dvij.getStatusText()));
			q.setParameter(10, new TypedParameterValue(StandardBasicTypes.DATE, dvij.getReturnDate()));
			q.setParameter(11, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getReturnCodeRef()));
			q.setParameter(12, new TypedParameterValue(StandardBasicTypes.STRING, dvij.getReturnTextRef()));
			q.setParameter(13, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getReturnMethod()));
			q.setParameter(14, new TypedParameterValue(StandardBasicTypes.STRING, dvij.getReturnInfo()));
			q.setParameter(15, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getUserLastMod()));
			q.setParameter(16, new TypedParameterValue(StandardBasicTypes.DATE, dvij.getDateLastMod()));
			q.setParameter(17, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getId()));

			if (dateParams) {
				q.setParameter(18, new TypedParameterValue(StandardBasicTypes.DATE, oldDvijDate));
				q.setParameter(19, new TypedParameterValue(StandardBasicTypes.DATE, dvij.getDvijDate()));
			}

			int deloDvijUpd = q.executeUpdate();
			
			
			sql = "UPDATE DOC_DVIJ "  
					+ "	SET CODE_REF= ?1, "
					+ dateSql
					+ "		DVIJ_METHOD= ?3, "
					+ "		DVIJ_TEXT= ?4, "
					+ "		DVIJ_INFO= ?5, "
					+ "		RETURN_TO_DATE= ?6, "
					+ "		STATUS= ?7, "
					+ "		STATUS_DATE= ?8, "
					+ "		STATUS_TEXT= ?9, "
					+ "		RETURN_DATE= ?10, "
					+ "		RETURN_CODE_REF= ?11, "
					+ "		RETURN_TEXT_REF= ?12, "
					+ "		RETURN_METHOD= ?13, "
					+ "		RETURN_INFO= ?14, "
					+ "		USER_LAST_MOD= ?15, "
					+ "		DATE_LAST_MOD = ?16 "			 
					+ "	WHERE FROM_DVIJ_ID = ?17"; 
			
			q = JPA.getUtil().getEntityManager().createNativeQuery(sql);
			q.setParameter(1, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getCodeRef()) );
			q.setParameter(2, new TypedParameterValue(StandardBasicTypes.DATE, dvij.getDvijDate()));
			q.setParameter(3, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getDvijMethod()));
			q.setParameter(4, new TypedParameterValue(StandardBasicTypes.STRING, dvij.getDvijText()));
			q.setParameter(5, new TypedParameterValue(StandardBasicTypes.STRING, dvij.getDvijInfo()));
			q.setParameter(6, new TypedParameterValue(StandardBasicTypes.DATE, dvij.getReturnToDate()));
			q.setParameter(7, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getStatus()));
			q.setParameter(8, new TypedParameterValue(StandardBasicTypes.DATE, dvij.getStatusDate()));
			q.setParameter(9, new TypedParameterValue(StandardBasicTypes.STRING, dvij.getStatusText()));
			q.setParameter(10, new TypedParameterValue(StandardBasicTypes.DATE, dvij.getReturnDate()));
			q.setParameter(11, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getReturnCodeRef()));
			q.setParameter(12, new TypedParameterValue(StandardBasicTypes.STRING, dvij.getReturnTextRef()));
			q.setParameter(13, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getReturnMethod()));
			q.setParameter(14, new TypedParameterValue(StandardBasicTypes.STRING, dvij.getReturnInfo()));
			q.setParameter(15, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getUserLastMod()));
			q.setParameter(16, new TypedParameterValue(StandardBasicTypes.DATE, dvij.getDateLastMod()));
			q.setParameter(17, new TypedParameterValue(StandardBasicTypes.INTEGER, dvij.getId()));

			if (dateParams) {
				q.setParameter(18, new TypedParameterValue(StandardBasicTypes.DATE, oldDvijDate));
				q.setParameter(19, new TypedParameterValue(StandardBasicTypes.DATE, dvij.getDvijDate()));
			}

			int docDvijUpd = q.executeUpdate();
			
//			if (forInsert != null || forDelete != null) {
				DocDostUtils du = new DocDostUtils();
				if (deloDvijUpd > 0) { // има дела които са свързан и трябва да им се оправи достъпа зададен от движението им
					List<Object[]> list = createNativeQuery(
						"select dvij.DELO_ID, dvij.ID, d.RN_DELO, d.DELO_DATE, di.RN_DELO IDENT_RN, di.DELO_DATE IDENT_DATE from DELO_DVIJ dvij"
						+ " left outer join DELO d on d.DELO_ID = ?1 inner join DELO di on di.DELO_ID = dvij.DELO_ID where dvij.FROM_DVIJ_ID = ?2")
						.setParameter(1, dvij.getDeloId()).setParameter(2, dvij.getId()).getResultList();
					for (Object[] row : list) {
						int deloId = ((Number)row[0]).intValue();
						int dvijId = ((Number)row[1]).intValue();
						du.addRemoveDeloDost(deloId, true
							, OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DVIJ, dvijId, forDelete, forInsert, sd);

						String deloIdent = row[4] + "/" + DateUtils.printDate((Date) row[5]);
						String ident = "Движението с ИД="+dvijId+
							" на преписка "+deloIdent+" е коригирано във връзка с корекция на движение с ИД="+dvij.getId()+
							" на преписка "+row[2]+"/"+DateUtils.printDate((Date) row[3])+".";

						SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DVIJ, dvijId, ident);

						journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
						journal.setDateAction(new Date());
						journal.setIdUser(getUserId());
						journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO);
						journal.setJoinedIdObject1(deloId);

						saveAudit(journal);
					}
				}
				if (docDvijUpd > 0) { // има документи които са свързан и трябва да им се оправи достъпа зададен от движението им
					List<Object[]> list = createNativeQuery(
						"select dvij.DOC_ID, dvij.ID, d.RN_DELO, d.DELO_DATE, di.RN_DOC IDENT_RN, di.DOC_DATE IDENT_DATE, di.PORED_DELO from DOC_DVIJ dvij"
						+ " left outer join DELO d on d.DELO_ID = ?1 inner join DOC di on di.DOC_ID = dvij.DOC_ID where dvij.FROM_DVIJ_ID = ?2")
						.setParameter(1, dvij.getDeloId()).setParameter(2, dvij.getId()).getResultList();
					for (Object[] row : list) {
						int docId = ((Number)row[0]).intValue();
						int dvijId = ((Number)row[1]).intValue();

						du.addRemoveDocDost(docId
							, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ, dvijId, forDelete, forInsert, sd);

						String docIdent = DocDAO.formRnDocDate(row[4], row[5], row[6]);
						String ident = "Движението с ИД="+dvijId+
										" на документ "+docIdent+" е коригирано във връзка с корекция на движение с ИД="+dvij.getId()+
										" на преписка "+row[2]+"/"+DateUtils.printDate((Date) row[3])+".";

						SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ, dvijId, ident);

						journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
						journal.setDateAction(new Date());
						journal.setIdUser(getUserId());
						journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
						journal.setJoinedIdObject1(docId);

						saveAudit(journal);
					}
				}
//			}
			
		} catch (Exception e) {
			LOGGER.error("Грешка при промяна на вложени дела и документи !");
			throw new DbErrorException("Грешка при промяна на вложени дела и документи !",e);
		}
	}

	/**
	 * при изтриване на движението трябва да се изтрие и достъп до каквото то е дало
	 */
	@Override
	protected void remove(DeloDvij entity) throws DbErrorException, ObjectInUseException {
		try {
			// DELO_ACCESS_ALL
			if (entity.getCodeRef() != null) {
				int deleted = createNativeQuery("delete from DELO_ACCESS_ALL where OBJECT_CODE = ?1 and OBJECT_ID = ?2")
					.setParameter(1, OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO_DVIJ).setParameter(2, entity.getId()).executeUpdate();
				LOGGER.debug("Изтрити са {} реда от DELO_ACCESS_ALL за движение с ID={}", deleted, entity.getId());
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при изтриване на свързани обекти на движение!", e);
		}

		super.remove(entity);
	}
}
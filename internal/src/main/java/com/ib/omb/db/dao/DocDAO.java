package com.ib.omb.db.dao;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DEF_PRAVA;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DOC_VALID_DESTRUCT;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRI;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ALG_FREE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ALG_INDEX_STEP;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_ALG_VID_DOC;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_JALBA_FULL_EDIT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_NPM_FULL_EDIT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_SAMOS_FULL_EDIT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DELOVODNA_REQUEST;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_ACCESS;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_REGIST;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_UNAUTHORIZED_OBJECT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_AVTOR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_CONTR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_DELOVODITEL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_SLUJIT_DOST;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;
import static com.ib.system.SysConstants.CODE_DEIN_KOREKCIA;
import static com.ib.system.SysConstants.CODE_DEIN_ZAPIS;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_DA;
import static com.ib.system.utils.SearchUtils.asInteger;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.persistence.ParameterMode;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloDoc;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocAccess;
import com.ib.omb.db.dto.DocAccessJournal;
import com.ib.omb.db.dto.DocDopdata;
import com.ib.omb.db.dto.DocDvij;
import com.ib.omb.db.dto.DocJalba;
import com.ib.omb.db.dto.DocMember;
import com.ib.omb.db.dto.DocReferent;
import com.ib.omb.db.dto.DocSpec;
import com.ib.omb.db.dto.ProcExe;
import com.ib.omb.experimental.Notification;
import com.ib.omb.search.DocSearch;
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
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link Doc}
 *
 * @author belev
 */
public class DocDAO extends AbstractDAO<Doc> { 

	/** DAO for {@link DocReferent} */
	static class DocReferentDAO extends AbstractDAO<DocReferent> {

		/** @param user */
		DocReferentDAO(ActiveUser user) {
			super(DocReferent.class, user);
		}
	}

	/** DAO for {@link DocDopdata} */
	static class DocDopdataDAO extends AbstractDAO<DocDopdata> {

		/** @param user */
		DocDopdataDAO(ActiveUser user) {
			super(DocDopdata.class, user);
		}
	}

	/** Това ще даде възможност да се генерира номер в отделна транзакция 
	 * @author belev */
	private class GenTransact extends Thread {
		private Doc doc;
		private int alg;
		Exception ex; // и като е в отделна нишка и гръмне няма как да знам и за това тука ще се пази грешката ако е има
		/**
		 * @param doc
		 * @param alg
		 */
		GenTransact(Doc doc, int alg) {
			this.doc = doc;
			this.alg = alg;
		}

		@Override
		public void run() {
			try {
				JPA.getUtil().begin();
				
				if (this.alg == CODE_ZNACHENIE_ALG_VID_DOC) {
					genRnDocByVidDoc(this.doc);
				} else {
					genRnDocByRegister(this.doc);
				}
				
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
	private static final Logger LOGGER = LoggerFactory.getLogger(DocDAO.class);

	/**
	 * @param user
	 */
	public DocDAO(ActiveUser user) {
		super(Doc.class, user);
	}
	
	/**
	 * Предостава селект за търсене на документи по кореспондент<br>
	 * 
	 * [0]-DOC_ID<br>
	 * [1]-RN_DOC<br>
	 * [2]-DOC_TYPE<br>
	 * [3]-DOC_VID<br>
	 * [4]-DOC_DATE<br>
	 * [5]-REGISTER_ID<br>
	 * [6]-REGISTRATURA_ID<br>
	 * [7]-OTNOSNO (String)<br>
	 * [8]-COUNT_FILES<br>
	 * 
	 * @param codeRefCorresp 
	 * @return 
	 * @throws DbErrorException 
	 */
	public SelectMetadata createSelectCorrespondentDocs(Integer codeRefCorresp) throws DbErrorException {
		String dialect = JPA.getUtil().getDbVendorName();

		String select = "select d.DOC_ID a0, "+DocDAO.formRnDocSelect("d.", dialect)+" a1, d.DOC_TYPE a2, d.DOC_VID a3, d.DOC_DATE a4, d.REGISTER_ID a5, d.REGISTRATURA_ID a6, "
			+ DialectConstructor.limitBigString(dialect, "d.OTNOSNO", 300) + " a7, d.COUNT_FILES a8 ";
		String from = " from DOC d ";
		String where = " where d.CODE_REF_CORRESP = :codeRefCorresp ";

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSql(select + from + where);
		sm.setSqlParameters(Collections.singletonMap("codeRefCorresp", codeRefCorresp));

		return sm;
	}
	
	/**
	 * Предостава селект за търсене на служители, които имат достъп до документ<br>
	 * 
	 * [0]-CODE<br>
	 * [1]-REF_NAME<br>
	 * [2]-EMPL_POSITION<br>
	 * [3]-CODE_ZVENO<br>
	 * 
	 * @param docId
	 * @param sd 
	 * @return
	 * @throws DbErrorException 
	 */
	public SelectMetadata createSelectDocAccessList(Integer docId, BaseSystemData sd) throws DbErrorException {
		@SuppressWarnings("unchecked")
		List<Object[]> all = createNativeQuery( // първо взимам аксес листа
			"select distinct CODE_REF, CODE_STRUCT from DOC_ACCESS_ALL where DOC_ID = ?1")
			.setParameter(1, docId).getResultList();

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
			+ " left outer join DOC_ACCESS da on da.DOC_ID = :docId and da.CODE_REF = r.CODE and da.EXCLUDE = :da";
		
		String where = " where r.CODE in (:codes)"
			+ " and r.DATE_OT = (select max (v.DATE_OT) from ADM_REFERENTS v where v.CODE = r.CODE and v.DATE_OT <> v.DATE_DO)"
			+ " and da.ID is null";

		Map<String, Object> params = new HashMap<>();
		params.put("docId", docId);
		params.put("da", SysConstants.CODE_ZNACHENIE_DA);
		params.put("codes", refCodes);
		
		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(distinct r.CODE) " + from + where);
		sm.setSql(select + from + where);
		sm.setSqlParameters(params);

		return sm;
	}
	
	/** Изписват се валидации преди реално да се извика изтриванто. Ако не е позволено да се трие се дава ObjectInUseException */
	@Override
	public void delete(Doc entity) throws DbErrorException, ObjectInUseException {
		try {
			Integer cnt;

			cnt = asInteger( // DELO.INIT_DOC_ID
				createNativeQuery("select count (*) as cnt from DELO where INIT_DOC_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Документ "+entity.getIdentInfo()+" е иницииращ на преписка и не може да бъде изтрит!");
			}

			cnt = asInteger( // DELO_DOC.INPUT_DOC_ID
				createNativeQuery("select count (*) as cnt from DELO_DOC where INPUT_DOC_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Документ "+entity.getIdentInfo()+" е вложен в преписка и не може да бъде изтрит!");
			}

			cnt = asInteger( // TASK.DOC_ID
				createNativeQuery("select count (*) as cnt from TASK where DOC_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("За документ "+entity.getIdentInfo()+" има регистрирани задачи и не може да бъде изтрит!");
			}
			@SuppressWarnings("unchecked")
			List<Object> rnTaskList = // TASK.END_DOC_ID
				createNativeQuery("select RN_TASK as cnt from TASK where END_DOC_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList();
			if (rnTaskList != null && !rnTaskList.isEmpty()) {
				StringBuilder sb = new StringBuilder("Документ "+entity.getIdentInfo()+" е избран като документ при приключване на следните задачи: ");
				for (int i = 0; i < rnTaskList.size(); i++) {
					sb.append(rnTaskList.get(i));
					if (i < rnTaskList.size()-1) {
						sb.append(", ");
					}
				}
				throw new ObjectInUseException(sb.toString() + " и не може да бъде изтрит!");
			}

			cnt = asInteger( // TASK_STATES.END_DOC_ID
				createNativeQuery("select count (*) as cnt from TASK_STATES where END_DOC_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("За документ "+entity.getIdentInfo()+" има регистрирани задачи и не може да бъде изтрит!");
			}

			cnt = asInteger( // DOC_PRIL.DOC_ID
				createNativeQuery("select count (*) as cnt from DOC_PRIL where DOC_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("За документ "+entity.getIdentInfo()+" има регистрирани приложения и не може да бъде изтрит!");
			}

			cnt = asInteger( // DOC_DVIJ.DOC_ID
				createNativeQuery("select count (*) as cnt from DOC_DVIJ where DOC_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("За документ "+entity.getIdentInfo()+" има регистрирано движение и не може да бъде изтрит!");
			}

			cnt = asInteger( // DOC_DOC.DOC_ID1(DOC_ID2)
				createNativeQuery("select count (*) as cnt from DOC_DOC where DOC_ID1 = ?1 or DOC_ID2 = ?2") //
					.setParameter(1, entity.getId()).setParameter(2, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Документ "+entity.getIdentInfo()+" има връзка с други документи и не може да бъде изтрит!");
			}

			cnt = asInteger( // DOC_DESTRUCT.DOC_ID
				createNativeQuery("select count (*) as cnt from DOC_DESTRUCT where DOC_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Документ "+entity.getIdentInfo()+" е включен в протокол за унищожавне и не може да бъде изтрит!");
			}

			cnt = asInteger( // DELO_ARCHIVE.PROTOCOL_ID
				createNativeQuery("select count (*) as cnt from DELO_ARCHIVE where PROTOCOL_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Документ "+entity.getIdentInfo()+" е протокол за архивиране на преписки и не може да бъде изтрит!");
			}

			@SuppressWarnings("unchecked")
			List<Object[]> workOffData =  // DOC.WORK_OFF_ID_ID
				createNativeQuery("select DOC_ID, RN_DOC, DOC_DATE, PORED_DELO from DOC where WORK_OFF_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList();
			if (!workOffData.isEmpty()) {
			
				if (entity.getDocType().equals(CODE_ZNACHENIE_DOC_TYPE_OWN)) {
					// за собствен трябва да се нулира връзката
					createNativeQuery("update DOC set WORK_OFF_ID = null, PROCESSED = ?1 where WORK_OFF_ID = ?2") //
						.setParameter(1, SysConstants.CODE_ZNACHENIE_NE).setParameter(2, entity.getId()).executeUpdate();

					StringBuilder ident = new StringBuilder();
					ident.append("Работен документ " + formRnDocDate(workOffData.get(0)[1], workOffData.get(0)[2], workOffData.get(0)[3]));
					ident.append(" е маркиран за \"необработен\" при премахване на връзката му със със собствен документ ");
					ident.append(entity.getIdentInfo() + ", поради изтриване на собствения документ.");

					SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC
						, ((Number)workOffData.get(0)[0]).intValue(), ident.toString());

					journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
					journal.setDateAction(new Date());
					journal.setIdUser(getUserId());

					saveAudit(journal);

				} else {
					throw new ObjectInUseException("Документ "+entity.getIdentInfo()+" е регистриран като официален и не може да бъде изтрит!");
				}
			}

			cnt = asInteger( // PROC_EXE.DOC_ID
				createNativeQuery("select count (*) as cnt from PROC_EXE where DOC_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Документ "+entity.getIdentInfo()+" е свързан с изпълнение на процедура и не може да бъде изтрит!");
			}

			cnt = asInteger( // TASK_SCHEDULE.DOC_ID
				createNativeQuery("select count (*) as cnt from TASK_SCHEDULE where DOC_ID = ?1") //
					.setParameter(1, entity.getId()) //
					.getResultList().get(0));
			if (cnt != null && cnt.intValue() > 0) {
				throw new ObjectInUseException("Документ "+entity.getIdentInfo()+" е свързан с дефиниция на периодична задача и не може да бъде изтрит!");
			}

		} catch (ObjectInUseException e) {
			throw e; // за да не се преопакова

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани обекти към документа!", e);
		}

		super.delete(entity);
	}

	/**
	 * Допълнително Разпределя участниците по списъците на база ролята им
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Doc findById(Object id) throws DbErrorException {
		Doc doc = super.findById(id);
		if (doc == null) {
			return doc;
		}

		try {
			doc.setDbForRegId(doc.getForRegId()); // за последващи анализи при правенето на нотификации
			doc.setDbProcessed(doc.getProcessed()); // за последващи анализи при правенето на нотификации
			doc.setDbDocDate(doc.getDocDate());

			if (!Objects.equals(doc.getDocType(), CODE_ZNACHENIE_DOC_TYPE_IN)) { // този вид няма такива данни
				Query refQuery = createQuery("select dr from DocReferent dr where dr.docId = ?1 order by dr.pored").setParameter(1, doc.getId());
				doc.setHistory(refQuery.getResultList());
				doc.referentsDistribute();
			}

			if (doc.getPoredDelo() != null || doc.getDocType().equals(CODE_ZNACHENIE_DOC_TYPE_WRK)) {
				doc.setDeloIncluded(true); // за такива е ясно че няма как да станат иницииращи на преписка
			} else {
				GregorianCalendar gc = new GregorianCalendar();
				gc.setTime(doc.getDocDate());
				int yyyy = gc.get(Calendar.YEAR);

				int docuMonth = SystemData.getDocuMonth();
				int docuDay = SystemData.getDocuDay();

				if (docuMonth == 1 && docuDay == 1) { // отчетния период е календарна година
					Query query = createNativeQuery( //
						"select DELO_ID from DELO where REGISTRATURA_ID = ?1 and RN_DELO = ?2 and DELO_YEAR = ?3 ") //
							.setParameter(1, doc.getRegistraturaId()).setParameter(2, doc.getRnDoc()).setParameter(3, yyyy);
					doc.setDeloIncluded(!query.getResultList().isEmpty());

				} else { // периода е от-до произволна дата
					Query query = createNativeQuery( //
						"select DELO_ID from DELO where REGISTRATURA_ID = ?1 and RN_DELO = ?2 and DELO_DATE >= ?3 and DELO_DATE <= ?4") //
							.setParameter(1, doc.getRegistraturaId()).setParameter(2, doc.getRnDoc());

					gc.set(yyyy, docuMonth-1, docuDay, 0, 0, 0); // началото на периода
					
					if (gc.getTimeInMillis() > doc.getDocDate().getTime()) {
						yyyy-=1; // периода е започнал през миналата година
						gc.set(Calendar.YEAR, yyyy);
					}
					query.setParameter(3, gc.getTime());
					
					gc.set(yyyy+1, docuMonth-1, docuDay-1, 23, 59, 59); // предходния ден на следващата година
					query.setParameter(4, gc.getTime());
					
					doc.setDeloIncluded(!query.getResultList().isEmpty());
				}
			}

			if (doc.getWorkOffId() != null) { // трябва да се качат данни и за свързания документ
				Query workOffQuery = createNativeQuery("select DOC_ID, RN_DOC, DOC_DATE, REGISTRATURA_ID, PORED_DELO from DOC where DOC_ID = ?1") //
					.setParameter(1, doc.getWorkOffId());

				List<Object[]> workOffData = workOffQuery.getResultList();
				if (!workOffData.isEmpty()) {
					doc.setWorkOffData(workOffData.get(0));
					
					// това ще направи поредения номер слепен до номера на док
					doc.getWorkOffData()[1] = formRnDoc(doc.getWorkOffData()[1], doc.getWorkOffData()[4]);
				}
			}

			if (Objects.equals(doc.getValid(), CODE_CLASSIF_DOC_VALID_DESTRUCT)) { // унищожен и има протокол
				StringBuilder sql = new StringBuilder();

				sql.append(" select s.DOC_ID, s.RN_DOC, s.DOC_DATE, s.PORED_DELO ");
				sql.append(" from DOC_DESTRUCT dd ");
				sql.append(" inner join DOC r on dd.PROTOCOL_ID = r.DOC_ID "); // работния
				sql.append(" inner join DOC s on s.DOC_ID = r.WORK_OFF_ID "); // официалния
				sql.append(" where dd.DOC_ID = ?1 order by s.DOC_DATE desc ");

				Query protocolQuery = createNativeQuery(sql.toString()).setParameter(1, doc.getId());

				List<Object[]> protocolData = protocolQuery.getResultList();
				if (!protocolData.isEmpty()) {
					doc.setProtocolData(protocolData.get(0));
					
					// това ще направи поредения номер слепен до номера на док
					doc.getProtocolData()[1] = formRnDoc(doc.getProtocolData()[1], doc.getProtocolData()[3]);
				}
			}

//			// зареждане на данни за тематиките
//			List<Object> topicRows = createNativeQuery("select TOPIC from DOC_TOPIC where DOC_ID = ?1") //
//				.setParameter(1, doc.getId()).getResultList();
//			if (topicRows.size() > 0) {
//				List<Integer> topicList = new ArrayList<>(topicRows.size());
//				for (Object row : topicRows) {
//					topicList.add(((Number)row).intValue());
//				}
//				doc.setTopicList(topicList);
//				doc.setDbTopicList(new ArrayList<>(topicList));
//			}

			if (doc.getProcDef() != null) { // за документа има процедура и трябва да се намери статуса и
				List<Object> statList = createNativeQuery(
					"select STATUS from PROC_EXE where DOC_ID = ?1 and DEF_ID = ?2 order by EXE_ID desc")
					.setParameter(1, doc.getId()).setParameter(2, doc.getProcDef())
					.setMaxResults(1).getResultList();
				if (!statList.isEmpty()) {
					doc.setProcExeStat(asInteger(statList.get(0)));
				}
			}

			if (doc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA)) {
				doc.setJalba(new DocJalbaDAO(getUser()).findById(id));

			} else 	if (doc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM)) {
				doc.setDocSpec(new DocSpecDAO(getUser()).findById(id));
				
			} else 	if (doc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS)) {
				doc.setDocSpec(new DocSpecDAO(getUser()).findById(id));
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на свързани обекти по документа!", e);
		}

		return doc;
	}
	
	/** 
	 * Tърсене на допълнителни данни по документа
	 * 
	 * @param docId
	 * @return
	 * @throws DbErrorException
	 */
	public DocDopdata findDocDopdata(Integer docId) throws DbErrorException {
		try {
			@SuppressWarnings("unchecked")
			List<DocDopdata> list = createQuery("select dd from DocDopdata dd where dd.docId = ?1")
				.setParameter(1, docId).getResultList();
			if (list.isEmpty()) {
				return null;
			}
			return list.get(0);
			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на допълнителни данни по документа!", e);
		}
	}

	/**
	 * @param userData
	 * @param rnDoc
	 * @param docDate
	 * @param docIdArg ако има се търси само за него, като другите се игнорират
	 * @param sd 
	 * @return
	 * @throws DbErrorException
	 */
	public List<LinkedHashMap<String, Object>> restFindDocData(UserData userData, String rnDoc, Date docDate, Integer docIdArg, SystemData sd) throws DbErrorException {
		List<LinkedHashMap<String, Object>> result = new ArrayList<>();

		if (docIdArg == null) {
			if (userData == null) {
				LOGGER.error("restFindDocData->userData=NULL!");
				return result;
			}
			rnDoc = SearchUtils.trimToNULL_Upper(rnDoc);
			if (rnDoc == null) {
				LOGGER.error("restFindDocData->rnDoc=NULL!");
				return result;
			}
		}

		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder from = new StringBuilder();
		StringBuilder select = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append("select distinct d.DOC_ID, d.RN_DOC, d.DOC_DATE, ");
		select.append(DialectConstructor.limitBigString(dialect, "d.OTNOSNO", 300) + " OTNOSNO "); // max 300!
		select.append(" , f.FILE_ID, f.FILENAME, d.DOC_TYPE, d.CODE_REF_CORRESP ");
		select.append(" , CASE WHEN d.DOC_TYPE = 1 THEN null ELSE "); // за входящите авторите не се теглят
		select.append(DialectConstructor.convertToDelimitedString(dialect, "dr.CODE_REF", "DOC_REFERENTS dr where dr.DOC_ID = d.DOC_ID and dr.ROLE_REF = 1", "dr.PORED"));
		select.append(" END AUTHORS, d.DOC_VID ");

		from.append(" from DOC d ");
		from.append(" left outer join FILE_OBJECTS fo on fo.OBJECT_ID = d.DOC_ID and fo.OBJECT_CODE = :objectCode ");
		from.append(" left outer join FILES f on f.FILE_ID = fo.FILE_ID ");
		params.put("objectCode", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);

		if (docIdArg == null) {
			where.append(" where upper(d.RN_DOC) = :rnDoc ");
			params.put("rnDoc", rnDoc);

			if (docDate != null) {
				where.append(" and d.DOC_DATE >= :docDateFrom and d.DOC_DATE <= :docDateTo ");
				params.put("docDateFrom", DateUtils.startDate(docDate));
				params.put("docDateTo", DateUtils.endDate(docDate));
			}

			new DocSearch(null).addAccessRules(where, from, params, userData, true);
		} else {
			where.append(" where d.DOC_ID = :docIdArg ");
			params.put("docIdArg", docIdArg);
		}

		try {
			Query query = createNativeQuery(select.toString() + from + where + " order by d.DOC_DATE desc ");
			
			for(Entry<String, Object> entry : params.entrySet()) {
				query.setParameter(entry.getKey(), entry.getValue());
			}

			LinkedHashMap<String, Object> doc;
			List<LinkedHashMap<String, Object>> files = new ArrayList<>();
			
			Set<Integer> docIdSet = new HashSet<>(); // има размножаване по файлове
			
			@SuppressWarnings("unchecked")
			List<Object[]> rows = query.getResultList();
			for (Object[] row : rows) {
				Integer docId = asInteger(row[0]);
				
				if (!docIdSet.contains(docId)) {
					docIdSet.add(docId); // първо срещане на дока
					
					doc = new LinkedHashMap<>();
					files = new ArrayList<>();
					
					doc.put("DOC_ID"	, docId);
					doc.put("RN_DOC"	, row[1]);
					Date date = (Date) row[2];
					doc.put("DOC_DATE"	, date);

					Integer docType = asInteger(row[6]);
					doc.put("DOC_TYPE"		, docType);
					doc.put("DOC_TYPE_TEXT"	, sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_TYPE, docType, getUserLang(), date));

					Integer docVid = asInteger(row[9]);
					doc.put("DOC_VID"		, docVid);
					doc.put("DOC_VID_TEXT"	, sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, docVid, getUserLang(), date));

					doc.put("OTNOSNO"	, row[3]);

					String corresp = null;
					if (row[7] != null) {
						corresp = sd.decodeItem(Constants.CODE_CLASSIF_REFERENTS, ((Number)row[7]).intValue(), getUserLang(), date);
					} else {
						corresp = "";
					}
					doc.put("CORRESP_TEXT"	, corresp);
					
					String authors = null;
					if (row[8] != null) {
						authors = sd.decodeItems(Constants.CODE_CLASSIF_ADMIN_STR, (String)row[8], getUserLang(), date);
					} else {
						authors = "";
					}
					doc.put("AUTHORS_TEXT"	, authors);

					doc.put("FILES"		, files);
					
					result.add(doc);
				}
				
				if (row[4] != null) { // FILE_ID
					LinkedHashMap<String, Object> file = new LinkedHashMap<>();
					file.put("FILE_ID"	, ((Number)row[4]).intValue());
					file.put("FILENAME"	, row[5]);
					
					files.add(file);
				}
			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на данни за документ!", e);
		}
		return result;
	}

	/**
	 * Проверява дали усера, който е в ДАО-то има достъп до документа
	 * @param doc
	 * @param editMode 
	 * @param sd 
	 * @return
	 * @throws DbErrorException
	 */
	public boolean hasDocAccess(Doc doc, boolean editMode, BaseSystemData sd) throws DbErrorException {
		UserData ud = (UserData) getUser();

		if (ud.isDocAccessDenied()) { // трябва да се провери дали не му е отнет достъп до този документ
			try {
				@SuppressWarnings("unchecked")
				List<Object> list = createNativeQuery( //
					"select ID from DOC_ACCESS where DOC_ID = :docId and CODE_REF = :codeRef and EXCLUDE = :da") //
					.setParameter("docId", doc.getId()).setParameter("codeRef", ud.getUserAccess()).setParameter("da", SysConstants.CODE_ZNACHENIE_DA) //
					.getResultList();
				if (!list.isEmpty()) {
					sendUnauthorizedObjectAccessNotif(doc, editMode, ud, sd);
					return false; // отнет достъп
				}
			} catch (Exception e) {
				throw new DbErrorException("Грешка при проверка за достъп до документ.", e);
			}
		}

		if (doc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA)) {
			return hasJalbaAccess(doc, editMode, ud, sd);
		}
		if (doc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM) || doc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS)) {
			return hasNpmSamosAccess(doc, editMode, ud, sd);
		}

		if (!editMode && Objects.equals(doc.getFreeAccess(), CODE_ZNACHENIE_DA)) { // FREE_ACCESS само за преглед
			if (doc.getRegistraturaId().equals(ud.getRegistratura())) {
				return true; // текущата регистратура
			}
			if (ud.hasAccess(OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS, doc.getRegistraturaId())) {
				return true; // ако е от позволените
			}
		}

		if (editMode) {
			if (!doc.getRegistraturaId().equals(ud.getRegistratura())) {
				sendUnauthorizedObjectAccessNotif(doc, editMode, ud, sd);
				return false; // няма значение какъв е, защото в друга не може актуализация
			}
			if (ud.hasAccess(CODE_CLASSIF_DEF_PRAVA, CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT)) {
				return true; // има Пълен достъп за актуализация на преписки/дела и документи
			}
			
			Map<Integer, Boolean> allowedEditDocTypes = ud.getAccessValues().get(OmbConstants.CODE_CLASSIF_DOC_TYPE);
			if (allowedEditDocTypes != null && !allowedEditDocTypes.isEmpty()) {
				if (!ud.hasAccess(OmbConstants.CODE_CLASSIF_DOC_TYPE, doc.getDocType())) {
					sendUnauthorizedObjectAccessNotif(doc, editMode, ud, sd);
					return false; // няма достъп до актуализация на такива типове документи
				}
			}
			
		} else if (ud.hasAccess(CODE_CLASSIF_DEF_PRAVA, CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW) || ud.hasAccess(CODE_CLASSIF_DEF_PRAVA, CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT)) {
			// има Пълен достъп за разглеждане
			if (ud.hasAccess(OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS, doc.getRegistraturaId())) {
				return true; // само ако регистратурата е в позволените му
			}
		}

		if (doc.getUserReg().equals(ud.getUserAccess())) {
			return true; // регистриралия има достъп и не е нужно да се пуска селекта
		}
		if (doc.getHistory() != null && !doc.getHistory().isEmpty()) {
			for (DocReferent dr : doc.getHistory()) {
				if (dr.getCodeRef().equals(ud.getUserAccess())) {
					return true; // ако е някой измежду хората в документа има достъп и селекта няма нужда да се пуска
				}
			}
		}
		
		Number count;
		try { // ще се пуска селект
			StringBuilder sql = new StringBuilder();

			sql.append(" select count (*) from DOC_ACCESS_ALL where DOC_ID = ?1 and");
			if (editMode || ud.getAccessZvenoList() == null) {
				sql.append(" CODE_REF in (" + ud.getUserAccess() + "," + ud.getZveno() + ") ");	
			} else {
				sql.append(" ( CODE_REF in (" + ud.getUserAccess() + "," + ud.getZveno() + ")");
				sql.append(" or CODE_STRUCT in (" + ud.getAccessZvenoList() + ") )");
			}

			Query query = createNativeQuery(sql.toString()).setParameter(1, doc.getId());

			count = (Number) query.getResultList().get(0);

		} catch (Exception e) {
			throw new DbErrorException("Грешка при проверка за достъп до документ.", e);
		}
		if (count.intValue() > 0) {
			return true;
		}
		sendUnauthorizedObjectAccessNotif(doc, editMode, ud, sd);
		return false;
	}
	
	private boolean hasJalbaAccess(Doc doc, boolean editMode, UserData ud, BaseSystemData sd) {
		if (ud.hasAccess(CODE_CLASSIF_DEF_PRAVA, CODE_ZNACHENIE_DEF_PRAVA_JALBA_FULL_EDIT)
			|| ud.hasAccess(Constants.CODE_CLASSIF_BUSINESS_ROLE, OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_DELOVODITEL)) {
			return true; // потребителят има дефинитивно право „Пълен достъп за актуализация на жалби“ или е деловодител
		}
		if (editMode) {
			if (ud.getUserAccess().equals(doc.getUserReg()) || ud.getUserAccess().equals(doc.getJalba().getCodeExpert())) {
				return true; // потребителят е регистрирал документа или потребителят е водещ експерт на жалбата
			}
			if (ud.getAccessZvenoList() != null && ud.getZveno().equals(doc.getJalba().getCodeZveno())) {
				return true; // потребителят е ръководител на звеното, на което е разпределена жалбата
			}
			sendUnauthorizedObjectAccessNotif(doc, editMode, ud, sd);
			return false;
		} 
		return ud.hasAccess(Constants.CODE_CLASSIF_MENU, OmbConstants.CODE_ZNACHENIE_MENU_SPR_JALBA); // за преглед е нужно само достъп до меню:справки->Жалби
	}

	private boolean hasNpmSamosAccess(Doc doc, boolean editMode, UserData ud, BaseSystemData sd) {
		int codeMenu;
		int codeFullEdit;
		DocSpec spec;
		if (doc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM)) {
			codeMenu = OmbConstants.CODE_ZNACHENIE_MENU_SPR_NPM; // меню:справки->Заповеди по НПМ
			codeFullEdit = CODE_ZNACHENIE_DEF_PRAVA_NPM_FULL_EDIT;
			spec = doc.getDocSpec();
		} else {
			codeMenu = OmbConstants.CODE_ZNACHENIE_MENU_SPR_SAMOS; // меню:справки->Решения за самосезиране
			codeFullEdit = CODE_ZNACHENIE_DEF_PRAVA_SAMOS_FULL_EDIT;
			spec = doc.getDocSpec();
		}
		
		if (ud.hasAccess(CODE_CLASSIF_DEF_PRAVA, codeFullEdit)) {
			return true; // потребителят има дефинитивно право „Пълен достъп“ спрямо изчисленото отгоре
		}
		if (editMode) {
			if (ud.getUserAccess().equals(doc.getUserReg()) || ud.getUserAccess().equals(spec.getCodeLeader())) {
				return true; // потребителят е регистрирал документа или потребителят е ръководител на екип
			}
			if (spec.getDopExpertCodes() != null && spec.getDopExpertCodes().contains(ud.getUserAccess())) { 
				return true; // потребителят е член на екипа
			}
			sendUnauthorizedObjectAccessNotif(doc, editMode, ud, sd);
			return false;
		} 
		return ud.hasAccess(Constants.CODE_CLASSIF_MENU, codeMenu); // за преглед е нужно само достъп до съотвтенотно меню справки
	}
	
	/** формира и изпраща нотификация: Опит за неоторизиран достъп до обект
	 */
	private void sendUnauthorizedObjectAccessNotif(Doc doc, boolean editMode, UserData ud, BaseSystemData sd) {
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
			msg.append(" документ № " + formRnDocDate(doc.getRnDoc(), doc.getDocDate(), doc.getPoredDelo()));
			msg.append(" с ИД=" + doc.getId() + ".");
			
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
	 * Проверява дали усера, който е в ДАО-то има достъп до документа през работният плот (отнет достъп)
	 * @param docId
	 * @return
	 * @throws DbErrorException
	 */
	public boolean hasDocAccessDashboard(Integer docId) throws DbErrorException {
		UserData ud = (UserData) getUser();

		if (ud.isDocAccessDenied()) { // трябва да се провери дали не му е отнет достъп до този документ
			try {
				@SuppressWarnings("unchecked")
				List<Object> list = createNativeQuery( //
					"select ID from DOC_ACCESS where DOC_ID = :docId and CODE_REF = :codeRef and EXCLUDE = :da") //
					.setParameter("docId", docId).setParameter("codeRef", ud.getUserAccess()).setParameter("da", SysConstants.CODE_ZNACHENIE_DA) //
					.getResultList();
				if (!list.isEmpty()) {
					return false; // отнет достъп
				}
			} catch (Exception e) {
				throw new DbErrorException("Грешка при проверка за достъп до документ.", e);
			}
		}
		
		return true;
	}
	
	/**
	 * Намира основни данни за документа. Резултата е от вида:<br>
	 * [0]-DOC_ID<br>
	 * [1]-RN_DOC<br>
	 * [2]-DOC_DATE<br>
	 * [3]-DOC_VID<br>
	 * [4]-OTNOSNO (String)<br>
	 * [5]-PORED_DELO<br>
	 *
	 * @param id
	 * @return намерения док или NULL ако за това ИД няма данни
	 * @throws DbErrorException
	 */
	public Object[] findDocData(Integer id) throws DbErrorException {
		try {
			String dialect = JPA.getUtil().getDbVendorName();

			StringBuilder sql = new StringBuilder();
			sql.append(" select DOC_ID, RN_DOC, DOC_DATE, DOC_VID, ");
			sql.append(DialectConstructor.limitBigString(dialect, "OTNOSNO", 300) + " OTNOSNO, PORED_DELO "); // max 300!
			sql.append(" from DOC where DOC_ID = :docId ");

			@SuppressWarnings("unchecked")
			List<Object[]> result = createNativeQuery(sql.toString()).setParameter("docId", id).getResultList();
			if (result.size() == 1) {
				return result.get(0);
			}

			LOGGER.error("findDocData(docId={}) NOT FOUND!", id);
			return null;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на основни данни за документ!", e);
		}
	}

	/**
	 * Намира основни данни за документа. Резултата е от вида:<br>
	 * [0]-DOC_ID<br>
	 * [1]-RN_DOC<br>
	 * [2]-DOC_DATE<br>
	 * [3]-DOC_VID<br>
	 * [4]-DOC_TYPE<br>
	 *
	 * @param fromMail
	 * @return намерения док или NULL ако за fromMail няма данни
	 * @throws DbErrorException
	 */
	public Object[] findDocDataFromMail(String fromMail) throws DbErrorException {
		if (fromMail == null || fromMail.length() == 0) {
			return null;
		}
		try {
			@SuppressWarnings("unchecked")
			List<Object[]> result = createNativeQuery( //
				"select DOC_ID, RN_DOC, DOC_DATE, DOC_VID, DOC_TYPE from DOC where upper(FROM_MAIL) = :fromMail") //
					.setParameter("fromMail", fromMail.trim().toUpperCase()).getResultList();

			return result.isEmpty() ? null : result.get(0);

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на основни данни за документ по данни за мейл!", e);
		}
	}

	/**
	 * Намира настройки по вид документ. Ако няма ред в таблицата по подадените аргументи връща <code>null</code>. Иначе: <br>
	 * [0]-SHEMA_ID<br>
	 * [1]-REGISTER_ID<br>
	 * [2]-CREATE_DELO<br>
	 * [3]-FOR_REG_ID<br>
	 * [4]-FILE_OBJECTS.OBJECT_ID !ИД-то на настройката само ако има шаблони!<br>
	 * [5]-PROC_DEF_IN<br>
	 * [6]-PROC_DEF_OWN<br>
	 * [7]-PROC_DEF_WORK<br>
	 * [8]-DOC_HAR<br>
	 * [9]-MEMBERS_TAB<br>
	 *
	 * @param registratura
	 * @param docVid
	 * @param sd
	 * @return
	 * @throws DbErrorException
	 */
	public Object[] findDocSettings(Integer registratura, Integer docVid, BaseSystemData sd) throws DbErrorException {
		int code = (registratura + "_" + docVid).hashCode();
		return (Object[]) sd.getItemSpecifics(CODE_CLASSIF_DOC_VID_SETTINGS, code, CODE_DEFAULT_LANG, null);
	}

	/**
	 * Зарежда данни за изричен достъп в обекта
	 *
	 * @param doc
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public void loadDocAccess(Doc doc) throws DbErrorException {
		try {
			Query query = createQuery("select a from DocAccess a where a.docId = :docId order by exclude desc, id") //
				.setParameter("docId", doc.getId());

			doc.setDocAccess(query.getResultList());

		} catch (Exception e) {
			throw new DbErrorException("Грешка при зареждане на изричен достъп за документ!", e);
		}
	}

	/**
	 * Прави анализ и създава нотификациите за изтриване на документ.
	 *
	 * @param doc
	 * @param systemData
	 * @throws DbErrorException
	 */
	public void notifDocDelete(Doc doc, SystemData systemData) throws DbErrorException {
		if (doc.getDocType().equals(CODE_ZNACHENIE_DOC_TYPE_WRK)) {
			
			if (Objects.equals(doc.getProcessed(), SysConstants.CODE_ZNACHENIE_NE) //
				&& doc.getReferentsAuthor() != null && !doc.getReferentsAuthor().isEmpty()) {
				
				List<Integer> adresati = new ArrayList<>(doc.getReferentsAuthor().size());
				for (DocReferent dr : doc.getReferentsAuthor()) {
					adresati.add(dr.getCodeRef());
				}

				Notification fake = new Notification(systemData);
				fake.generatеFakeNotif(OmbConstants.CODE_FAKE_NOTIF_RELOAD_DOC);
				fake.setAdresati(adresati);
				fake.send();
			}
		}
	}

	/**
	 * Прави анализ и създава нотификациите за документи. В момента се вика в ДАО-то накрая на записа. Ако в бъдеще се реши че
	 * трябва да се вика в бийна след записа някой трябва да се погржи да има необходимите параметри.
	 *
	 * @param doc
	 * @param notifDelovodnaRequestFlag
	 * @param notifOtherDoc
	 * @param sourceRegDvijId 
	 * @param newAccessEmpls 
	 * @param oldProcessed 
	 * @param systemData
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public void notifDocSave(Doc doc, boolean notifDelovodnaRequestFlag, Doc notifOtherDoc, Integer sourceRegDvijId, 
		List<DocAccess> newAccessEmpls, Integer oldProcessed, SystemData systemData) throws DbErrorException {
		
		boolean genFakeNeregRab = false; // за опресняване в работния плот

		if (notifDelovodnaRequestFlag) { // !!! ЗАЯВКА ЗА ДЕЛОВОДНА РЕГИСТРАЦИЯ !!!

			Map<Integer, Object> specifics = new HashMap<>();
			specifics.put(OmbClassifAdapter.USERS_INDEX_REGISTRATURA, doc.getForRegId());
			specifics.put(OmbClassifAdapter.USERS_INDEX_DELOVODITEL, SysConstants.CODE_ZNACHENIE_DA);

			// така ще извади от кеша само потребители, които са деловодители в търсената регистратура
			List<SystemClassif> items = systemData.queryClassification(SysConstants.CODE_CLASSIF_USERS, null, doc.getDocDate(), getUserLang(), specifics);

			if (items.isEmpty()) {
				LOGGER.warn("OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DELOVODNA_REQUEST ! няма налични деловодители !");

			} else {
				List<Integer> delovoditeli = new ArrayList<>(items.size());
				for (SystemClassif item : items) {
					delovoditeli.add(item.getCode());
				}

				Notification notif = new Notification(((UserData)getUser()).getUserAccess() , doc.getForRegId()
					, CODE_ZNACHENIE_NOTIFF_EVENTS_DELOVODNA_REQUEST, CODE_ZNACHENIE_NOTIF_ROLIA_DELOVODITEL, systemData);
				notif.setDoc(doc);
				notif.setAdresati(delovoditeli);
				notif.send();
			}

			genFakeNeregRab = true;
		}

		if (notifOtherDoc != null) {
			Map<Integer, Object> specifics = new HashMap<>(); // надолу се сетва правилната регистратура в зависимост от казуса
			specifics.put(OmbClassifAdapter.USERS_INDEX_DELOVODITEL, SysConstants.CODE_ZNACHENIE_DA);
			
			if (sourceRegDvijId == null) { // !!! РЕГИСТРАЦИЯ !!! 
				
				List<Integer> authorCodeList;
				try { // търся авторите на работния, които може да са различни от авторите на собствения !!
					Query query = createQuery("select dr.codeRef from DocReferent dr where dr.docId = ?1 and dr.roleRef = ?2") //
						.setParameter(1, notifOtherDoc.getId()).setParameter(2, CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR);
					authorCodeList = query.getResultList();
				} catch (Exception e) {
					throw new DbErrorException("Грешка при търсене на автори на работен документ!", e);
				}

				if (authorCodeList.isEmpty()) {
					LOGGER.warn("OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_REGIST ! за документа няма въведен автор !");

				} else {
					Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
						, CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_REGIST, CODE_ZNACHENIE_NOTIF_ROLIA_AVTOR, systemData);
					notif.setDoc(notifOtherDoc);
					notif.setOtherDoc(doc);
					notif.setAdresati(authorCodeList);
					notif.send();
				}
				
			} else { // !!! ПРЕ-РЕГИСТРАЦИЯ !!!
				
//				specifics.put(OmbClassifAdapter.USERS_INDEX_REGISTRATURA, notifOtherDoc.getRegistraturaId());
//				// така ще извади от кеша само потребители, които са деловодители в търсената регистратура
//				List<SystemClassif> items = systemData.queryClassification(SysConstants.CODE_CLASSIF_USERS, null, doc.getDocDate(), getUserLang(), specifics);
//				if (items.isEmpty()) {
//					LOGGER.warn("OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_RE_REGIST ! няма налични деловодители !");
//
//				} else {
//					List<Integer> delovoditeli = new ArrayList<>(items.size());
//					for (SystemClassif item : items) {
//						delovoditeli.add(item.getCode());
//					}
//					
//					Notification notif = new Notification(((UserData)getUser()).getUserAccess() ,notifOtherDoc.getRegistraturaId()
//						, CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_RE_REGIST, CODE_ZNACHENIE_NOTIF_ROLIA_DELOVODITEL, systemData);
//					notif.setDoc(notifOtherDoc);
//					notif.setOtherDoc(doc);
//					notif.setAdresati(delovoditeli);
//					notif.send();
//				}
			}

			// фиктнивно за деловодителите за да им се оправят списъците
			
			specifics.put(OmbClassifAdapter.USERS_INDEX_REGISTRATURA, doc.getRegistraturaId());
			// така ще извади от кеша само потребители, които са деловодители в търсената регистратура
			List<SystemClassif> items = systemData.queryClassification(SysConstants.CODE_CLASSIF_USERS, null, doc.getDocDate(), getUserLang(), specifics);
			if (items.isEmpty()) {
				LOGGER.warn("OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_REGIST/CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_RE_REGIST ! няма налични деловодители !");

			} else {
				List<Integer> delovoditeli = new ArrayList<>(items.size());
				for (SystemClassif item : items) {
					delovoditeli.add(item.getCode());
				}
				
				Notification fake = new Notification(systemData);
				fake.generatеFakeNotif(OmbConstants.CODE_FAKE_NOTIF_RELOAD_DELOVOD);
				fake.setAdresati(delovoditeli);
				fake.send();
			}
		}
		
		if (newAccessEmpls != null && !newAccessEmpls.isEmpty()) {

			Notification notif = new Notification(((UserData)getUser()).getUserAccess() , null
				, CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_ACCESS, CODE_ZNACHENIE_NOTIF_ROLIA_SLUJIT_DOST, systemData);
			notif.setDoc(doc);
			notif.setComment(newAccessEmpls.get(0).getNote());
			notif.setDocAccess(newAccessEmpls.get(0));
			List<Integer> adresati = new ArrayList<>(newAccessEmpls.size());
			for (DocAccess access : newAccessEmpls) {
				adresati.add(access.getCodeRef());
			}
			notif.setAdresati(adresati);
			notif.send();
		}
		
		if (doc.getDocType().equals(CODE_ZNACHENIE_DOC_TYPE_WRK)) {
			boolean genFake = false;
			if (oldProcessed == null) {
				genFake = Objects.equals(doc.getProcessed(), SysConstants.CODE_ZNACHENIE_NE);

			} else if (!oldProcessed.equals(doc.getProcessed())) { // има смяна
			
				genFake = oldProcessed.equals(SysConstants.CODE_ZNACHENIE_NE) // било е НЕ и става ДА
					|| doc.getWorkOffId() == null; // или било е ДА и става НЕ и само ако не е свързан със собствен
			}
			genFakeNeregRab |= genFake;
		}
		
		if (genFakeNeregRab) { // за автори и изпълнители на задачи за съглуване и подпис
			
			List<Object> tmpList = createNativeQuery(
				"select distinct r.CODE_REF from TASK t inner join TASK_REFERENTS r on r.TASK_ID = t.TASK_ID where t.DOC_ID = ?1 and t.TASK_TYPE in (?2,?3)")
				.setParameter(1, doc.getId())
				.setParameter(2, OmbConstants.CODE_ZNACHENIE_TASK_TYPE_SAGL)
				.setParameter(3, OmbConstants.CODE_ZNACHENIE_TASK_TYPE_PODPIS).getResultList();
			
			Set<Integer> adresati = new HashSet<>();
			for (Object tmp : tmpList) {
				adresati.add(((Number)tmp).intValue());
			}
			
			if (doc.getReferentsAuthor() != null && !doc.getReferentsAuthor().isEmpty()) {
				for (DocReferent dr : doc.getReferentsAuthor()) {
					adresati.add(dr.getCodeRef());
				}
			}
			
			if (!adresati.isEmpty()) {
				Notification fake = new Notification(systemData);
				fake.generatеFakeNotif(OmbConstants.CODE_FAKE_NOTIF_RELOAD_DOC);
				fake.getAdresati().addAll(adresati);
				fake.send();
			}
		}
	}


	/**
	 * Запис на нов док и влагане в преписката, за която иницииращ документ е otherDocId.
	 * 
	 * @param doc
	 * @param otherDocId
	 * @param otherDocVid
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public Doc saveNew(Doc doc, Integer otherDocId, Integer otherDocVid, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		doc = save(doc, false, null, null, systemData); 		// стандартния запис на док
		
		Integer deloId;
		try { 		
			String sql;
			if (otherDocVid != null) { // трябва да намеря преписката, за която иницииращ документ е otherDocId
				sql = "select delo_id from delo where init_doc_id = ?1";
				
			} else { // иначе само където в вложен
				sql = "select delo_id from delo_doc where input_doc_id = ?1 order by id";
			}

			@SuppressWarnings("unchecked")
			List<Object> deloIdList = createNativeQuery(sql)
											.setParameter(1, otherDocId).getResultList();
			if (deloIdList.isEmpty()) {
				return doc;
			}
			deloId = ((Number) deloIdList.get(0)).intValue();
			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на преписка по иницииращ документ.", e);
		}
		
		// и влагането в преписка
		DeloDoc deloDoc = new DeloDoc();
		deloDoc.setDeloId(deloId);

		new DeloDocDAO(getUser()).saveInDoc(deloDoc, doc, false, false, false);
		
		return doc;
	}
	
	/**
	 * Намира данни за преписката, в която подадения документ е вложен за първи път. Ако няма преписка дава NULL<br>
	 * [0]-delo_id<br>
	 * [1]-rn_delo<br>
	 * [2]-delo_date<br>
	 * [3]-delo_type<br>
	 * 
	 * @param docId
	 * @return
	 * @throws DbErrorException
	 */
	public Object[] findFirstInclDelo(Integer docId) throws DbErrorException {
		if (docId == null) {
			return null;
		}
		try {
			@SuppressWarnings("unchecked")
			List<Object[]> rows = createNativeQuery(
				"select d.delo_id, d.rn_delo, d.delo_date, d.delo_type from delo_doc dd inner join delo d on d.delo_id = dd.delo_id where dd.input_doc_id = ?1 order by dd.id")
				.setParameter(1, docId).getResultList();

			return rows.isEmpty() ? null : rows.get(0);
			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на данни за преписката по вложен документ.", e);
		}
	}

	/**
	 * Реализира сетването на trackable информацията за свързаните обекти
	 *
	 * @param doc
	 * @param createDelo      при <code>true</code> създава преписка като текущия документ става иницииращ
	 * @param includeInDeloId ако има нещо значи се вкарва дока в тази преписка
	 * @param sourceRegDvijId ИД на движение от регистратура източник при предаване на документ
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public Doc save(Doc doc, boolean createDelo, Integer includeInDeloId, Integer sourceRegDvijId, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		SystemData sd = (SystemData) systemData;

		if (doc.getId() == null && doc.getRnDoc() != null) { // при нов запис ако има въведен номер трябва да се валидира
			if (doc.getRnDoc().indexOf('#') != -1) {
				throw new ObjectInUseException("Въведен е недопустим символ за номер.");
			}

			doc.setPoredDelo(null); // при нов запис винаги това трябва да е празно

			String errorRnDoc = validateRnDoc(doc, includeInDeloId, sd, false);
			if (errorRnDoc != null) {
				throw new ObjectInUseException(errorRnDoc);
			}
		}
		if (doc.getDbDocDate() != null && !doc.getDocDate().equals(doc.getDbDocDate())) { // разлика в датаДок при корекция
			boolean validate = false; // трябва да се провери ако новата дата излиза от отчетния период на старата
										// да се пусне валидация дали номера няма да дублира
			int docuMonth = SystemData.getDocuMonth();
			int docuDay = SystemData.getDocuDay();

			GregorianCalendar dbCal = new GregorianCalendar();
			dbCal.setTime(doc.getDbDocDate());
			int dbYear = dbCal.get(Calendar.YEAR);

			GregorianCalendar cal = new GregorianCalendar();
			cal.setTime(doc.getDocDate());

			if (docuMonth == 1 && docuDay == 1) { // отчетния период е календарна година
				
				validate = dbYear != cal.get(Calendar.YEAR); // само при разлика в годините
				
			} else { // периода е от-до произволна дата и е мазало
				dbCal.set(dbYear, docuMonth-1, docuDay, 0, 0, 0); // началото на периода
				
				if (dbCal.getTimeInMillis() > doc.getDbDocDate().getTime()) {
					dbYear-=1; // периода е започнал през миналата година
					dbCal.set(Calendar.YEAR, dbYear);
				}
				Date from = dbCal.getTime(); // от тука 
				
				dbCal.set(dbYear+1, docuMonth-1, docuDay-1, 23, 59, 59); // предходния ден на следващата година
				Date to = dbCal.getTime(); // до тука трябва да е новата дата иначе валидация
				
				// на новата дата трябва да се нулиат за да е вярно сравнението
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.MILLISECOND, 0);
				
				validate = cal.getTimeInMillis() < from.getTime() || cal.getTimeInMillis() > to.getTime();
			}
			
			if (validate) {
				String errorRnDoc = validateRnDoc(doc, null, sd, false);
				if (errorRnDoc != null) {
					throw new ObjectInUseException(errorRnDoc);
				}
			}
		}
		
		if (!doc.getDocType().equals(CODE_ZNACHENIE_DOC_TYPE_WRK) // работния няма ГУИД
			&& SearchUtils.isEmpty(doc.getGuid())) { // Градим гуид

			doc.setGuid("{" + UUID.randomUUID().toString().toUpperCase() + "}");
		}

		if (SearchUtils.isEmpty(doc.getRnDoc())) { // Градим номер на документа
			generateRnDoc(doc, sd);
			
			doc.setPoredDelo(null); // при генериране винаги това трябва да е празно
			
			String errorRnDoc = validateRnDoc(doc, null, systemData, true);
			if (errorRnDoc != null) {
				
				doc.setRnDoc(null);
				doc.setRnPrefix(null);
				doc.setRnPored(null);
				
				throw new ObjectInUseException(errorRnDoc 
					+ " Моля, обърнете се към администратор, който да провери стойността на 'Първи свободен №' в избрания регистър.");
			}
		}

		doc.setAuditable(false); // за да не запише ред в журнала при записа на обекта

		boolean inDoc = doc.getDocType().equals(CODE_ZNACHENIE_DOC_TYPE_IN);

		Map<Integer, Integer> accessMap = new HashMap<>(); // key=codeRef
		   												   // value 0=стар; 1=нов; 2=премахнат; 3=отказант достъп;

		boolean registraturaChanged = false; // ще се вдигне флаг ако е деловодител и прави запис на ново дело в различна 
												// регистратура от която е назначен като служител !?!?

		int codeAction;
		if (doc.getId() == null) {
			codeAction = CODE_DEIN_ZAPIS;

			doc.setValid(CODE_CLASSIF_DOC_VALID_ACTUAL); // другите се управляват през архивиране и унищожаване
			doc.setValidDate(doc.getDocDate());

			if (sd.isDopDelovoditelRegistraturi()) { // възможно е да се работи от други регистратури спрямо назначението
				Integer admReg = (Integer) sd.getItemSpecific(Constants.CODE_CLASSIF_ADMIN_STR, getUserId()
					, SysConstants.CODE_DEFAULT_LANG, null, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
				if (doc.getRegistraturaId().equals(admReg)) {
					accessMap.put(getUserId(), 1); // регистратора като нов
				} else {
					registraturaChanged = true; // има смяна и регистратора ще влезне само с личен достъп без звено
				}
			} else { // масовия случай
				accessMap.put(getUserId(), 1); // регистратора като нов
			}

			if (inDoc) {
				doc.setCompetence(OmbConstants.CODE_ZNACHENIE_COMPETENCE_OUR);
			}

		} else {
			codeAction = CODE_DEIN_KOREKCIA;

			if (sd.isDopDelovoditelRegistraturi()) { // трябва да проверя регистриралия какъв достъп е получил
				Integer admReg = (Integer) sd.getItemSpecific(Constants.CODE_CLASSIF_ADMIN_STR, doc.getUserReg()
					, SysConstants.CODE_DEFAULT_LANG, doc.getDateReg(), OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
				if (admReg == null || doc.getRegistraturaId().equals(admReg)) { // само ако регистратора е работил в своята регистратура
					accessMap.put(doc.getUserReg(), 0); // регистратора като стар
				} else {
					registraturaChanged = true;
				}
			} else { // масовия случай
				accessMap.put(doc.getUserReg(), 0); // регистратора като стар
			}
		}

//		това не се иска да се прави
//		if (codeAction == CODE_DEIN_KOREKCIA // само за корекция
//			&& inDoc && !Objects.equals(doc.getProcessed(), CODE_ZNACHENIE_DA) // входящ, който не е обработен
//			&& createDelo) { // и прави нова преписка
//
//			doc.setProcessed(CODE_ZNACHENIE_DA); // става обработен
//		}

		Integer oldProcessed = doc.getDbProcessed(); // трябва ми за нотификациите да се знае има ли смяна

		Delo notifDeloInclude = null; // ако се определи такова ще се направи нотификация за класиране на док в преписка
										// като ще се гледа и типа на определената преписка

		if (SystemData.isDocPoredDeloGen()) {
			if (createDelo && includeInDeloId == null) {
				doc.setPoredDelo(1); // създава преписка и е #1

			} else if (includeInDeloId != null) {
				// влиза в преписка с нейния номер и трябва да се изчисли
				@SuppressWarnings("unchecked")
				List<Object> maxPoredList = createNativeQuery(
					"select max (DOC.PORED_DELO) MAX_PORED from DELO_DOC dd inner join DELO on DELO.DELO_ID = dd.DELO_ID"
					+ " inner join DOC on DOC.DOC_ID = dd.INPUT_DOC_ID where dd.DELO_ID = ?1 and DOC.RN_DOC = DELO.RN_DELO")
					.setParameter(1, includeInDeloId).getResultList();
				if (maxPoredList.isEmpty() || maxPoredList.get(0) == null) {
					doc.setPoredDelo(1); // първият и е #1
				} else {
					doc.setPoredDelo(((Number)maxPoredList.get(0)).intValue() + 1); // пореден и е #макс+1
				}

				if (inDoc) { // необходими са и данни за преписката в която се влага заради специфичните типове преписка
					@SuppressWarnings("unchecked")
					List<Object[]> deloList = createNativeQuery(
						"select delo_type, rn_delo, delo_date, init_doc_id from delo where delo_id = ?1 and init_doc_id is not null")
						.setParameter(1, includeInDeloId).getResultList();
					if (!deloList.isEmpty()) {
						notifDeloInclude = new Delo();
						notifDeloInclude.setId(includeInDeloId);
						notifDeloInclude.setDeloType(((Number)deloList.get(0)[0]).intValue());
						notifDeloInclude.setRnDelo((String)deloList.get(0)[1]);
						notifDeloInclude.setDeloDate((Date)deloList.get(0)[2]);
						notifDeloInclude.setInitDocId(((Number)deloList.get(0)[3]).intValue());
					}
				}
			}
		}

		if (doc.getId() == null && inDoc && !createDelo && includeInDeloId != null) {
			doc.setProcessed(CODE_ZNACHENIE_DA); // нов входящ, който влиза в преписка но не е иницииращ става обработен
		}
		
		Doc saved = super.save(doc); // основния запис

		if (!inDoc) { // този вид няма такива данни

			List<DocReferent> current = new ArrayList<>();
			// тука освен събирането им се дава и пореден номер (могат да ги разместват в компонентата), за да излизат всеки път
			// по екрана по един и същ начин.
			mergeReferentLists(current, doc.getReferentsAuthor());
			mergeReferentLists(current, doc.getReferentsAgreed());
			mergeReferentLists(current, doc.getReferentsSigned());

			referentsSave(doc, current, saved, accessMap); // свързаните участници в процеса
		}

//		topicSave(doc, saved);

		if (createDelo && includeInDeloId == null) { // трябва да се прави преписка, където текущя документ ще бъде иницииращ
														// само ако не се иска да се класира в друга!

			saved.setDeloIncluded(true); // !!! вече няма да може на друга да е иницииращ

			DeloDAO deloDao = new DeloDAO(getUser());

			Delo delo = new Delo(doc, sd);

			delo = deloDao.save(delo, false, sd, null); // записа на новата преписка

			DeloDoc deloDoc = new DeloDoc();
			deloDoc.setDeloId(delo.getId());
			deloDoc.setDelo(delo);

			new DeloDocDAO(getUser()).saveInDoc(deloDoc, saved, false, false, false);

		} else if (includeInDeloId != null) {
			// при нов запис имаме преписка, в която трябва да се вложи документа
			DeloDoc deloDoc = new DeloDoc();
			deloDoc.setDeloId(includeInDeloId);

			new DeloDocDAO(getUser()).saveInDoc(deloDoc, saved, false, false, false);

			saved.setDeloIncluded(true); // вече няма да може на друга да е иницииращ, защото е приел номера на някоя, която вече
											// има иницииращ
		} else {
			saved.setDeloIncluded(doc.isDeloIncluded()); // запазвам както си е било зради JPA
		}

		saved.setWorkOffData(doc.getWorkOffData()); // JPA merge !!!
		saved.setProtocolData(doc.getProtocolData()); // JPA merge !!!
		saved.setProcExeStat(doc.getProcExeStat()); // JPA merge !!!

		boolean notifDelovodnaRequestFlag = doc.getDbForRegId() == null && doc.getForRegId() != null;
		Doc notifOtherDoc = null;

		if (codeAction == CODE_DEIN_ZAPIS //
			&& (Objects.equals(saved.getDocType(), CODE_ZNACHENIE_DOC_TYPE_OWN) || sourceRegDvijId != null) //
			&& saved.getWorkOffId() != null) { // нов запис на док който трябва да се свърже към друг

			notifOtherDoc = connectWithOtherDoc(saved, sourceRegDvijId);
		}

		saved.setDbForRegId(saved.getForRegId()); // за последващите анализи при запис
		saved.setDbProcessed(saved.getProcessed()); // за последващите анализи при запис
		saved.setDbDocDate(saved.getDocDate());

		List<DocAccess> newAccessEmpls = docAccessSave(codeAction, saved, doc.getDocAccess(), accessMap);

		if (doc.getDopdata() != null) { // допълнителните полета
			doc.getDopdata().setDocId(doc.getId());
			DocDopdata dopdata = new DocDopdataDAO(getUser()).save(doc.getDopdata());
			saved.setDopdata(dopdata); // JPA merge !!!
		}

		if (doc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA) && doc.getJalba() != null) {
			boolean newSave = doc.getJalba().getId() == null;
			if (newSave) {
				doc.getJalba().setId(doc.getId());
			}
			DocJalba jalba = new DocJalbaDAO(getUser()).save(doc, newSave, sd);
			saved.setJalba(jalba); // JPA merge

		} else if (doc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM) && doc.getDocSpec() != null) {
			if (doc.getDocSpec().getId() == null) {
				doc.getDocSpec().setId(doc.getId());
			}
			DocSpec npm = new DocSpecDAO(getUser()).save(doc);
			saved.setDocSpec(npm); // JPA merge

		} else if (doc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS) && doc.getDocSpec() != null) {
			if (doc.getDocSpec().getId() == null) {
				doc.getDocSpec().setId(doc.getId());
			}
			DocSpec samos = new DocSpecDAO(getUser()).save(doc);
			saved.setDocSpec(samos); // JPA merge
		}

		saveAudit(saved, codeAction); // тука вече трябва да всичко да е насетвано и записа в журнала е ОК

		DocDostUtils du = new DocDostUtils();

		Set<Integer> newAccess;
		Set<Integer> delAccess;
		if (codeAction == CODE_DEIN_ZAPIS) {
			newAccess = accessMap.keySet();
			delAccess = null;

			if (registraturaChanged) { // само за нов запис
				du.addDocAccessUserReg(saved.getId(), saved.getUserReg(), CODE_ZNACHENIE_JOURNAL_DOC, saved.getId());
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
		
//		System.out.println("newAccess="+newAccess);
//		System.out.println("delAccess="+delAccess);
		
		// само частично преизграждане в зависимост от промените !!!
		du.addRemoveDocDost(saved.getId(), CODE_ZNACHENIE_JOURNAL_DOC, saved.getId(), delAccess, newAccess, sd);
		
		// !!! ДЖУРКАНЕТО НА НОТИФИКАЦИИ !!! - дали да не се разбият на отделни методи?
		notifDocSave(saved, notifDelovodnaRequestFlag, notifOtherDoc, sourceRegDvijId, newAccessEmpls, oldProcessed, sd);

		if (notifDeloInclude != null) { // влиза нов входящ документ в преписка, който взима нейния номер
													// и ако е специална преписка

			if (notifDeloInclude.getDeloType().equals(OmbConstants.CODE_ZNACHENIE_DELO_TYPE_JALBA)) {
				// нотификацията се изпраща до водещия експерт на жалбата и до ръководителя на звеното, на което е разпределена жалбата
				new DocJalbaDAO(getUser()).notifNewInDocInDelo(saved, notifDeloInclude, systemData);
				
			} else if (notifDeloInclude.getDeloType().equals(OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NPM)
				|| notifDeloInclude.getDeloType().equals(OmbConstants.CODE_ZNACHENIE_DELO_TYPE_SAMOS)) {
				// нотификацията се изпраща до ръководителя и до експертите от екипа, който извършва проверката
				new DocSpecDAO(getUser()).notifNewInDocInDelo(saved, notifDeloInclude, systemData);
			}
		}

		if (doc.getProcDef() != null && doc.getProcExeStat() == null) { // стартирам избраната процедура
			ProcExeDAO procExeDao = new ProcExeDAO(getUser());
			ProcExe procExe = procExeDao.startProc(doc.getProcDef(), doc.getId(), systemData);
			saved.setProcExeStat(procExe.getStatus());

			if (procExe.getDocProcessed() != null) {
				saved.setProcessed(procExe.getDocProcessed());
				saved.setDbProcessed(procExe.getDocProcessed());
			}
		}

		return saved;
	}

	/**
	 * Актуализира брой прикачени файлове за документа
	 *
	 * @param doc
	 * @param countFiles
	 * @throws DbErrorException
	 */
	public void updateCountFiles(Doc doc, Integer countFiles) throws DbErrorException {
		try {
			createNativeQuery("update DOC set COUNT_FILES = ?1 where DOC_ID = ?2") //
				.setParameter(1, countFiles).setParameter(2, doc.getId()) //
				.executeUpdate();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при актуализиране на брой прикачени файлове за документ.", e);
		}

		doc.setCountFiles(countFiles); // за да се ОК при последващ запис
	}
	
	/**
	 * Актуализира брой прикачени файлове за документа
	 *
	 * @param docId
	 * @param addCount - новодобавените
	 * @param docVid - вид на док, като за жалба се праща нотификация
	 * @param systemData - трябва за нотификацията
	 * @throws DbErrorException
	 */
	public void updateCountFiles(Integer docId, Integer addCount, Integer docVid, BaseSystemData systemData) throws DbErrorException {
		try {
			createNativeQuery(
				"update doc set count_files = (case when count_files is null then :addCount else count_files + :addCount end) where doc_id = :docId")
				.setParameter("addCount", addCount).setParameter("docId", docId)
				.executeUpdate();
			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при актуализиране на брой прикачени файлове за документ.", e);
		}

		if (docVid != null && docVid.equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA)) {
			new DocJalbaDAO(getUser()).notifAddFilesInDoc(docId, systemData);
		}
	}

	/**
	 * Валидира се номера. Ако номера е ОК връща NULL, иначе тескта на причината за невалидност, която може да се покаже на
	 * екрана.
	 *
	 * @param doc
	 * @param includeInDeloId
	 * @param sd
	 * @param generated true означава че номера е генериран през БД в този момент
	 * @return
	 * @throws DbErrorException
	 */
	public String validateRnDoc(Doc doc, Integer includeInDeloId, BaseSystemData sd, boolean generated) throws DbErrorException {
		String rnDoc = (doc.getRnDoc() != null ? doc.getRnDoc().trim() : null);
		doc.setRnDoc(rnDoc); // важно е да няма интервали в началото и края
		if (doc.getRnDoc() == null || doc.getRnDoc().length() == 0) {
			return null; // няма въведен номер, значи ще се генерира според регистъра
		}

		if (Objects.equals(doc.getRegisterId(), OmbConstants.ID_REGISTER_FREE_RAB_NO_VALIDATE)) {
			return null; // за този регистър няма валидация за номер
		}

		try {
			if (includeInDeloId != null) { // включва се в преписка и взима няйния номер

				@SuppressWarnings("unchecked")
				List<Object[]> rows = createNativeQuery("select RN_PREFIX, RN_PORED from DELO where DELO_ID = ?1") //
					.setParameter(1, includeInDeloId) //
					.getResultList();

				if (!rows.isEmpty()) {
					Object[] row = rows.get(0);

					doc.setRnPrefix((String) row[0]);
					doc.setRnPored(SearchUtils.asInteger(row[1]));
					doc.setPoredDelo(null); // ще се изчисли на запис
				}

				return null; // няма проблем че има съвпадение
			}
			
			String notIdQuery = "";
			String apendPoredDelo = "";

			if (doc.getId() == null) { // пуска се валидация на номер при определени промени и в корекция
										// и долните проверки са само за нов запис

				if (!generated) { // само ако не е е генериран
					
					// валидиране спрямо регистъра и вид документ
					String errorRnDoc = validateRnDocRegister(doc, sd);
					if (errorRnDoc != null) {
						return errorRnDoc;
					}
				}

			} else {
				notIdQuery = " and DOC_ID != ?5 "; // за корекция да изключи себе си

				if (!generated && SystemData.isDocPoredDeloGen() && doc.getPoredDelo() != null) {
					// ако номера не е генериран и работим с пореден номер в дело и е въведен трябва да се валидира и по него да няма дублиране
					apendPoredDelo = " and PORED_DELO = ?6 ";
				}
			}

			// не трябва да има такъв документ в БД по регистратура, номер и година
			GregorianCalendar gc = new GregorianCalendar();
			gc.setTime(doc.getDocDate());

			int yyyy = gc.get(Calendar.YEAR);

			Query query = createNativeQuery( //
				"select count (*) cnt from DOC where REGISTER_ID = ?1 and upper(RN_DOC) = ?2 and DOC_DATE >= ?3 and DOC_DATE <= ?4" + notIdQuery + apendPoredDelo) //
					.setParameter(1, doc.getRegisterId()).setParameter(2, doc.getRnDoc().toUpperCase());

			int docuMonth = SystemData.getDocuMonth();
			int docuDay = SystemData.getDocuDay();
			
			if (docuMonth == 1 && docuDay == 1) { // отчетния период е календарна година

				gc.set(yyyy, Calendar.JANUARY, 1, 0, 0, 0);
				query.setParameter(3, gc.getTime());

				gc.set(yyyy, Calendar.DECEMBER, 31, 23, 59, 59);
				query.setParameter(4, gc.getTime());
				
				if (doc.getId() != null) {
					query.setParameter(5, doc.getId());
					if (apendPoredDelo.length() > 0) {
						query.setParameter(6, doc.getPoredDelo());
					}
				}
				
				Number count = (Number) query.getResultList().get(0);

				if (count.intValue() > 0) {
					return "Невалиден регистров номер. Вече съществува документ с номер " + formRnDoc(doc.getRnDoc(), doc.getPoredDelo()) + " за година " + yyyy + ".";
				}

			} else { // периода е от-до произволна дата
				gc.set(yyyy, docuMonth-1, docuDay, 0, 0, 0); // началото на периода
		
				if (gc.getTimeInMillis() > doc.getDocDate().getTime()) {
					yyyy-=1; // периода е започнал през миналата година
					gc.set(Calendar.YEAR, yyyy);
				}
				query.setParameter(3, gc.getTime());
				Date from = gc.getTime(); // ако има грешка да се изолзва
				
				gc.set(yyyy+1, docuMonth-1, docuDay-1, 23, 59, 59); // предходния ден на следващата година
				query.setParameter(4, gc.getTime());

				if (doc.getId() != null) {
					query.setParameter(5, doc.getId());
					if (apendPoredDelo.length() > 0) {
						query.setParameter(6, doc.getPoredDelo());
					}
				}

				Number count = (Number) query.getResultList().get(0);

				if (count.intValue() > 0) {
					return "Невалиден регистров номер. Вече съществува документ с номер " + formRnDoc(doc.getRnDoc(), doc.getPoredDelo()) 
						+ " за период " + DateUtils.printDate(from) + "-" + DateUtils.printDate(gc.getTime()) + ".";
				}
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при валидация на номер на документ!", e);
		}

		return null;
	}

	/**
	 * Отказ на приемане на документ от движение + правене на нотификация
	 * 
	 * @param sourceRegDvijId
	 * @param statusText
	 * @param currentRegistraturaId 
	 * @param systemData 
	 * @throws DbErrorException
	 */
	public void rejectDocAcceptance(Integer sourceRegDvijId, String statusText, Integer currentRegistraturaId, SystemData systemData) throws DbErrorException {
		DocDvijDAO dvijDao = new DocDvijDAO(getUser());
		
		DocDvij dvij = dvijDao.findById(sourceRegDvijId);
		if (dvij == null) {
			return;
		}

		dvij.setStatus(OmbConstants.DS_REJECTED);
		dvij.setStatusDate(new Date());
		dvij.setStatusText(statusText);
		dvij.setOtherDocId(null);
			
		dvijDao.save(dvij); 

		@SuppressWarnings("unchecked")
		List<Object[]> list = createNativeQuery("select REGISTRATURA_ID, "+DocDAO.formRnDocSelect("", JPA.getUtil().getDbVendorName())+" RN_DOC, DOC_DATE from DOC where DOC_ID = ?1")
			.setParameter(1, dvij.getDocId()).getResultList();
		if (list.isEmpty()) {
			return;
		}
//		Integer sourceRegistratura = ((Number)list.get(0)[0]).intValue();
//		String rnDoc = (String) list.get(0)[1];
		Date docDate = (Date) list.get(0)[2];
		
		// трябва да се направят нотификации за деловодителите, както и фиктивни за работния плот
		
		Map<Integer, Object> specifics = new HashMap<>(); // надолу се сетва правилната регистратура в зависимост от казуса
		specifics.put(OmbClassifAdapter.USERS_INDEX_DELOVODITEL, SysConstants.CODE_ZNACHENIE_DA);
		
//		specifics.put(OmbClassifAdapter.USERS_INDEX_REGISTRATURA, sourceRegistratura);
		// така ще извади от кеша само потребители, които са деловодители в търсената регистратура
		List<SystemClassif> items; // = systemData.queryClassification(SysConstants.CODE_CLASSIF_USERS, null, docDate, getUserLang(), specifics);
//		if (items.isEmpty()) {
//			LOGGER.warn("OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_REGIST_REGECT ! няма налични деловодители !");
//
//		} else {
//			List<Integer> delovoditeli = new ArrayList<>(items.size());
//			for (SystemClassif item : items) {
//				delovoditeli.add(item.getCode());
//			}
//			
//			Notification notif = new Notification(((UserData)getUser()).getUserAccess() , sourceRegistratura
//				, CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_REGIST_REGECT, CODE_ZNACHENIE_NOTIF_ROLIA_DELOVODITEL, systemData);
//			Doc doc = new Doc(); // за да се формира правилна нотификация трябва да се направи така че данните за документа да са
//								 // си коректни, но регистратурата да е тази която отказва регистрацията
//			doc.setRegistraturaId(currentRegistraturaId);
//			doc.setDocDate(docDate);
//			doc.setRnDoc(rnDoc);
//			
//			notif.setDoc(doc);
//			notif.setDocDvij(dvij);
//			notif.setAdresati(delovoditeli);
//			notif.send();
//		}
		
		// фиктнивно за деловодителите за да им се оправят списъците
		
		specifics.put(OmbClassifAdapter.USERS_INDEX_REGISTRATURA, currentRegistraturaId);
		// така ще извади от кеша само потребители, които са деловодители в търсената регистратура
		items = systemData.queryClassification(SysConstants.CODE_CLASSIF_USERS, null, docDate, getUserLang(), specifics);
		if (items.isEmpty()) {
			LOGGER.warn("OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_REGIST_REGECT ! няма налични деловодители !");

		} else {
			List<Integer> delovoditeli = new ArrayList<>(items.size());
			for (SystemClassif item : items) {
				delovoditeli.add(item.getCode());
			}
			
			Notification fake = new Notification(systemData);
			fake.generatеFakeNotif(OmbConstants.CODE_FAKE_NOTIF_RELOAD_DELOVOD);
			fake.setAdresati(delovoditeli);
			fake.send();
		}
	}

	/**
	 * @param idDoc
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<Integer> getDocIgnoreList(Integer idDoc) throws DbErrorException {
		
		try {		
			Query q = JPA.getUtil().getEntityManager().createQuery("select da.codeRef from DocAccess da where da.exclude = 1 and da.docId = :IDD");
			q.setParameter("IDD", idDoc);
			return (ArrayList<Integer>) q.getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на лица с отнет достъп!", e);
		}		
	}
	
	/**
	 * Дава данни за движението:<br>
	 * [0]-ID<br>
	 * [1]-DVIJ_TEXT<br>
	 * [2]-DVIJ_INFO<br>
	 * [3]-RETURN_TO_DATE<br>
	 * @param dvijId
	 * @return
	 * @throws DbErrorException
	 */
	public Object[] findDocDvijData(Integer dvijId) throws DbErrorException {
		try {
			@SuppressWarnings("unchecked")
			List<Object[]> list = createNativeQuery("select ID, DVIJ_TEXT, DVIJ_INFO, RETURN_TO_DATE from DOC_DVIJ where ID = ?1")
				.setParameter(1, dvijId).getResultList();
			return list.isEmpty() ? null : list.get(0);
		} catch (Exception e) {
			throw new DbErrorException("Грешка при извличане данни за движение!", e);
		}
	}

	/**
	 * Преди да се изтрие документа трябва да се изтрият и други данни, които не са мапнати дирекно през JPA
	 */
	@Override
	protected void remove(Doc entity) throws DbErrorException, ObjectInUseException {
		try {
			if (entity.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA) && entity.getJalba() != null) {
				new DocJalbaDAO(getUser()).delete(entity.getJalba());

			} else if (entity.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM) && entity.getDocSpec() != null) {
				new DocSpecDAO(getUser()).delete(entity.getDocSpec());

			} else if (entity.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS) && entity.getDocSpec() != null) {
				new DocSpecDAO(getUser()).delete(entity.getDocSpec());
			}

			// DOC_REFERENTS
			int deleted = createNativeQuery("delete from DOC_REFERENTS where DOC_ID = ?1").setParameter(1, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} участника по документ с DOC_ID={}", deleted, entity.getId());

			// DOC_ACCESS
			deleted = createNativeQuery("delete from DOC_ACCESS where DOC_ID = ?1").setParameter(1, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} DOC_ACCESS за документ с DOC_ID={}", deleted, entity.getId());

			// DOC_ACCESS_ALL
			deleted = createNativeQuery("delete from DOC_ACCESS_ALL where DOC_ID = ?1").setParameter(1, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} DOC_ACCESS_ALL за документ с DOC_ID={}", deleted, entity.getId());

//			// DOC_TOPIC
//			deleted = createNativeQuery("delete from DOC_TOPIC where DOC_ID = ?1").setParameter(1, entity.getId()).executeUpdate();
//			LOGGER.debug("Изтрити са {} DOC_TOPIC за документ с DOC_ID={}", deleted, entity.getId());

			// ако го има само в DOC_DVIJ.OTHER_DOC_ID трябва да с се актуализират данните в движението
			@SuppressWarnings("unchecked")
			List<DocDvij> dvijList = createQuery("select dd from DocDvij dd where dd.otherDocId = ?1") //
				.setParameter(1, entity.getId()).getResultList();
			if (!dvijList.isEmpty()) {
				DocDvijDAO dvijDao = new DocDvijDAO(getUser());
					
				for (DocDvij dvij : dvijList) {
					
					dvij.setStatus(OmbConstants.DS_WAIT_REGISTRATION);
					dvij.setStatusDate(dvij.getDvijDate());
					dvij.setStatusText(null);
					dvij.setOtherDocId(null);
					
					dvijDao.save(dvij);
				}
			}

			// DOC_MEMBERS
			deleted = createNativeQuery("delete from DOC_MEMBERS where DOC_ID = ?1").setParameter(1, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} DOC_MEMBERS за документ с DOC_ID={}", deleted, entity.getId());

			// DOC_DOPDATA
			deleted = createNativeQuery("delete from DOC_DOPDATA where DOC_ID = ?1").setParameter(1, entity.getId()).executeUpdate();
			LOGGER.debug("Изтрити са {} DOC_DOPDATA за документ с DOC_ID={}", deleted, entity.getId());

		} catch (Exception e) {
			throw new DbErrorException("Грешка при изтриване на свързани обекти на документ!", e);
		}

		super.remove(entity);
	}

	/**
	 * Свързване на документ с друг документ
	 *
	 * @param doc
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	Doc connectWithOtherDoc(Doc doc, Integer sourceRegDvijId) throws DbErrorException, ObjectInUseException {
		Doc other = super.findById(doc.getWorkOffId()); // за да не му се навлича всичко

		if (sourceRegDvijId == null) { // регистриране на официален от работен
			
			if (!Objects.equals(other.getDocType(), CODE_ZNACHENIE_DOC_TYPE_WRK)) {
				throw new ObjectInUseException("Документ " + other.getRnDoc() + " не е от тип работен!");
			}

			other.setWorkOffId(doc.getId());
			other.setProcessed(SysConstants.CODE_ZNACHENIE_DA);

			saveSysOkaJournal(other, "Документ "+other.getIdentInfo()+" е регистриран като официален "+doc.getIdentInfo()+".");
		
		} else { // регистриране в друга регистратура като ще се маже по движението
			DocDvijDAO dvijDao = new DocDvijDAO(getUser());
		
			DocDvij dvij = dvijDao.findById(sourceRegDvijId);
			if (dvij != null) {
				dvij.setStatus(OmbConstants.DS_REGISTERED);
				dvij.setStatusDate(new Date());
				dvij.setStatusText(formRnDocDate(doc.getRnDoc(), doc.getDocDate(), doc.getPoredDelo()));
				dvij.setOtherDocId(doc.getId());
				
				dvijDao.save(dvij);
			}
		}

		doc.setWorkOffData(new Object[] { other.getId(), other.getRnDoc(), other.getDocDate(), other.getRegistraturaId() });

		return other; // трябва за нотификацията
	}

	/**
	 * Запис на изричния достъп
	 *
	 * @param dein
	 * @param saved
	 * @param docAccess
	 * @return списъка с новодобавените
	 * @throws DbErrorException
	 */
	List<DocAccess> docAccessSave(int dein, Doc saved, List<DocAccess> docAccess, Map<Integer, Integer> accessMap) throws DbErrorException {
		if (dein == CODE_DEIN_ZAPIS && (docAccess == null || docAccess.isEmpty())) {
			return null; // нов запис и няма
		}

		if (docAccess == null) { // корекция и не е бил зареден и за това пускам селект да се изтегли каквото има, за да сработи
									// определянето на достъпа
			// трябва да проверя дали няма някаква промяна по хората в документа, защото ако има трябва и това да се зареди, за да се прави анализ
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

			loadDocAccess(saved);
			docAccess = saved.getDocAccess(); // за да сработи надолу логиката
		}

		if (docAccess.isEmpty()) {
			return null; // зареден е но няма и няма какво да се прави като анализ
		}

		Query deleteQuery = null;
		Query deleteNotifQuery = null;

		List<DocAccess> newAccessEmpls = new ArrayList<>();
		
		int i = 0;
		DocAccessJournal docAccessJournal = new DocAccessJournal();
		docAccessJournal.setIdDoc(saved.getId());
		docAccessJournal.setIdentObject(saved.getIdentInfo());
		boolean hasChange = false;
		while (i < docAccess.size()) {
			DocAccess access = docAccess.get(i);

			if (Objects.equals(access.getExclude(), SysConstants.CODE_ZNACHENIE_DA)) {
				i++;
				continue; // достъпа е отнет и не се занимавам тука с него
			}

			if (Objects.equals(access.getFlag(), SysConstants.CODE_DEIN_IZTRIVANE)) {

				if (deleteQuery == null) {
					deleteQuery = createNativeQuery("delete from DOC_ACCESS where ID = ?1");
				}
				if (deleteNotifQuery == null) {
					deleteNotifQuery = createNativeQuery(
						"delete from USER_NOTIFICATIONS where USER_ID = :userId and MESSAGE_TYPE = :msgType and OBJECT_CODE = :objCode and OBJECT_ID = :objId and READ = 2");
				}
				deleteQuery.setParameter(1, access.getId()).executeUpdate();

				// изтривам и нотификации "за запознаване"
				deleteNotifQuery.setParameter("userId", access.getCodeRef());
				deleteNotifQuery.setParameter("msgType", OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_ACCESS);
				deleteNotifQuery.setParameter("objCode", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
				deleteNotifQuery.setParameter("objId", saved.getId());
				deleteNotifQuery.executeUpdate();
				hasChange = true;
				docAccess.remove(i);

				Integer temp = accessMap.get(access.getCodeRef());
				if (temp != null) { // има го
					if (temp.intValue() == 1) { // ако го има като нов и иска да се трие остава непроменен
						accessMap.put(access.getCodeRef(), 0);
					}
				} else {
					accessMap.put(access.getCodeRef(), 2);
				}

			} else {
				
				docAccessJournal.getPersons().add(access);
				if (access.getId() == null) { // само нов запис има тука без корекция !!!
					access.setDocId(saved.getId());
					if (access.getExclude() == null) {
						access.setExclude(SysConstants.CODE_ZNACHENIE_NE);
					}
					getEntityManager().persist(access);
					hasChange = true;
					newAccessEmpls.add(access);

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
		
		
		saved.setDocAccess(docAccess); // да се знае на екрана новото състояние
		
		if (hasChange) {
			SystemJournal journal = docAccessJournal.toSystemJournal();
			journal.setIdUser(getUserId());
			saveAudit(journal);		
		}
		return newAccessEmpls;
	}

	/**
	 * Генериране на регистров номер на документ
	 *
	 * @param doc
	 * @param sd
	 * @throws DbErrorException
	 * @throws ObjectInUseException 
	 */
	void generateRnDoc(Doc doc, BaseSystemData sd) throws DbErrorException, ObjectInUseException {
		if (doc.getRegisterId() == null) {
			throw new DbErrorException("Грешка при генериране на регистров номер на документ. Липсва регистър!");
		}

		Integer alg = (Integer) sd.getItemSpecific(CODE_CLASSIF_REGISTRI, doc.getRegisterId(), CODE_DEFAULT_LANG, doc.getDocDate(), OmbClassifAdapter.REGISTRI_INDEX_ALG);
		if (alg == null) {
			throw new DbErrorException("Грешка при генериране на регистров номер на документ. Липсва алгоритъм!");
		}

		if (JPA.getUtil().getDbVendorName().indexOf("POSTGRESQL") != -1) {
			GenTransact gt;
			try { // ще се генерира в отделна нишка защото в процедурите на постгрето няма вътрешно управление на транзакции
				gt = new GenTransact(doc, alg);
				gt.start();
				gt.join();	

			} catch (Exception e) {
				throw new DbErrorException("Системна грешка при генериране на регистров номер на документ ! POSTGRESQL !", e);
			}
			if (gt.ex != null) {
				if (gt.ex instanceof ObjectInUseException) {
					throw (ObjectInUseException) gt.ex;
				} else if (gt.ex instanceof DbErrorException) {
					throw (DbErrorException) gt.ex;
				}
				throw new DbErrorException(gt.ex); // някакво друго чудо е
			}

		} else { // тука в БД си се прави бегин комит и т.н.
		
			if (alg.equals(CODE_ZNACHENIE_ALG_VID_DOC)) {
				genRnDocByVidDoc(doc);
			} else {
				genRnDocByRegister(doc);
			}
		}
	}

	/**
	 * Запис на участниците в документа. В това число и изтриване на отпаднали.
	 *
	 * @param doc         документа преди да и е напрвен запис
	 * @param currentList списъка с участници, които трябва да остане
	 * @param saved       вече съхранения
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	void referentsSave(Doc doc, List<DocReferent> currentList, Doc saved, Map<Integer, Integer> accessMap) throws DbErrorException, ObjectInUseException {
		DocReferentDAO refDao = new DocReferentDAO(getUser());

		int foundCount = 0; // броя на намерените по ИД
		int unchangedCount = 0; // броя на непроменените

		List<DocReferent> history = doc.getHistory();
		if (history != null && !history.isEmpty()) { // само ако има нещо старо към документа

			for (DocReferent old : history) {
				DocReferent found = null;
				int foundIndex = -1;

				for (int i = 0; i < currentList.size(); i++) {
					DocReferent current = currentList.get(i);

					if (Objects.equals(old.getId(), current.getId())) {
						foundCount++;

						found = current;
						foundIndex = i;
						break;
					}
				}

				if (found == null) { // за изтриване
					refDao.delete(old);
					
					Integer temp = accessMap.get(old.getCodeRef());
					if (temp != null) { // има го
						if (temp.intValue() == 1) { // ако го има като нов и иска да се трие остава непроменен
							accessMap.put(old.getCodeRef(), 0);
						}
					} else { // влиза си за триене
						accessMap.put(old.getCodeRef(), 2);
					}
					
				} else { // за корекция
					found.fixEmptyStringValues();

					if (old.equals(found)) {
						unchangedCount++;

					} else {
						DocReferent savedRef = refDao.save(found);

						// заради MERGE-а на JPA-то се правят долните врътки
						savedRef.setTekst(found.getTekst());
						currentList.set(foundIndex, savedRef);

					}

					accessMap.put(old.getCodeRef(), 0); // тука винаги може да си се добави, защото няма да има промяна
				}
			}
		}

		if (foundCount < currentList.size()) { // само в този случай има нови
			for (DocReferent newRef : currentList) {
				if (newRef.getId() == null) { // за нов запис
					newRef.setDocId(saved.getId());

					newRef.fixEmptyStringValues();

					refDao.save(newRef); // при нов запис няма нужда се правят никакви врътки
					
					Integer temp = accessMap.get(newRef.getCodeRef());
					if (temp != null) { // има го
						if (temp.intValue() == 2) { // ако го има като изтрит и сега влиза като нов значи остава без промяна
							accessMap.put(newRef.getCodeRef(), 0);
						}
					} else { // трябва да си влезне като нов
						accessMap.put(newRef.getCodeRef(), 1);
					}
				}
			}
		}

		// и пак да се оправят списъците
		saved.setHistory(currentList);

		if (unchangedCount == currentList.size()) { // няма никаква промяна и могат да се сетнат дирекно
			saved.setReferentsAgreed(doc.getReferentsAgreed());
			saved.setReferentsAuthor(doc.getReferentsAuthor());
			saved.setReferentsSigned(doc.getReferentsSigned());

		} else {
			saved.referentsDistribute();
		}
	}

	/**
	 * Запис на темите
	 * @param doc този от екрана, който идва
	 * @param saved този който е след merge на jpa
	 */
	void topicSave(Doc doc, Doc saved) {
		
		if (doc.getTopicList() == null || doc.getTopicList().isEmpty()) { // на екрана няма
			if (doc.getDbTopicList() != null && !doc.getDbTopicList().isEmpty()) {  // трие се ако има нещо от БД

				createNativeQuery("delete from DOC_TOPIC where DOC_ID = ?1").setParameter(1, saved.getId()).executeUpdate();
				doc.setDbTopicList(null);
			}
			
		} else { // на екрана има
			Query insert = null;
			
			if (doc.getDbTopicList() == null || doc.getDbTopicList().isEmpty()) {  // но няма стари от БД
				
				insert = createNativeQuery("insert into DOC_TOPIC(DOC_ID,TOPIC) values(?1,?2)");
				for (Integer topic : doc.getTopicList()) { // всички се явяват нови
					insert.setParameter(1, saved.getId()).setParameter(2, topic).executeUpdate();
				}
				
			} else { // прави се анализ дали има промяна

				for (Integer topic : doc.getTopicList()) {
					if (!doc.getDbTopicList().remove(topic)) { // значи е за нов запис
						if (insert == null) {
							insert = createNativeQuery("insert into DOC_TOPIC(DOC_ID,TOPIC) values(?1,?2)");
						}
						insert.setParameter(1, saved.getId()).setParameter(2, topic).executeUpdate();
					} 
				}
				if (!doc.getDbTopicList().isEmpty()) { // ако нещо е останало е за изтриване
					createNativeQuery("delete from DOC_TOPIC where DOC_ID = ?1 and TOPIC in (?2)")
						.setParameter(1, saved.getId()).setParameter(2, doc.getDbTopicList()).executeUpdate();
				}
			} 
			doc.setDbTopicList(new ArrayList<>(doc.getTopicList()));
		}
		
		// заради JPA трябва да се оправи и обекта, който ще се върне
		saved.setTopicList(doc.getTopicList());
		saved.setDbTopicList(doc.getDbTopicList());
	}
	
	/**
	 * Валидиране на въведен номер на документ спрямо регистъра. При нов запис и пререгистриране, когато се дава нов номер на
	 * документ.
	 *
	 * @param doc
	 * @param sd
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	String validateRnDocRegister(Doc doc, BaseSystemData sd) throws DbErrorException {
		Integer alg = (Integer) sd.getItemSpecific(CODE_CLASSIF_REGISTRI, doc.getRegisterId(), CODE_DEFAULT_LANG, doc.getDocDate(), OmbClassifAdapter.REGISTRI_INDEX_ALG);
		if (alg == null) {
			return "Грешка при валидиране на регистров номер на документ. Липсва алгоритъм!";
		}
		if (alg.equals(CODE_ZNACHENIE_ALG_FREE)) {
			return null;
		}

		int delimiterIndex = doc.getRnDoc().lastIndexOf('-');

		String dbPrefix;
		int dbActNomer;

		try { // трябва да се проверява какво е в БД, за да се провери коректен ли е номера

			if (alg.equals(CODE_ZNACHENIE_ALG_INDEX_STEP)) {
				List<Object[]> rows = createNativeQuery("select PREFIX, ACT_NOMER from REGISTRI where REGISTER_ID = ?1") //
					.setParameter(1, doc.getRegisterId()) //
					.getResultList();
				if (rows.isEmpty()) {
					return "Невалиден регистров номер. За избрания регистър липсва информация в БД!";
				}
				dbPrefix = SearchUtils.trimToNULL((String) rows.get(0)[0]);
				dbActNomer = ((Number) rows.get(0)[1]).intValue();

			} else if (alg.equals(CODE_ZNACHENIE_ALG_VID_DOC)) {
				List<Object[]> rows = createNativeQuery("select PREFIX, ACT_NOMER from DOC_VID_SETTINGS where REGISTER_ID = ?1 and DOC_VID = ?2") //
					.setParameter(1, doc.getRegisterId()).setParameter(2, doc.getDocVid()) //
					.getResultList();
				if (rows.isEmpty()) {
					return "Невалиден регистров номер. За избрания вид документ липсва информация в БД!";
				}
				dbPrefix = SearchUtils.trimToNULL((String) rows.get(0)[0]);
				dbActNomer = ((Number) rows.get(0)[1]).intValue();

			} else {
				return "Грешка при валидиране на регистров номер на документ. Неразпознат алгоритъм: " + alg;
			}

		} catch (Exception e) {
			throw new DbErrorException("Системна грешка при валидиране на регистров номер на документ!", e);
		}

		String rnPrefix = delimiterIndex == -1 ? null : doc.getRnDoc().substring(0, delimiterIndex); // може и да няма въведен префикс
		if (!Objects.equals(dbPrefix, rnPrefix)) {
			String tmp = alg.equals(CODE_ZNACHENIE_ALG_INDEX_STEP) ? "регистър" : "вид документ";
			String tmpPrefix = dbPrefix == null ? " не се допуска префикс" : (" трябва да въведете префикс: " + dbPrefix);
			return "Невалиден регистров номер. За избрания " + tmp + tmpPrefix + ".";
		}

		int rnPored;
		try {
			rnPored = Integer.parseInt(doc.getRnDoc().substring(delimiterIndex + 1, doc.getRnDoc().length()));
		} catch (Exception e) {
			return "Невалиден регистров номер. За пореден номер трябва да въведете цяло число.";
		}
		if (rnPored == dbActNomer) {
			return "Невалиден регистров номер. Моля, включете автоматичното генериране на номер.";
		}
		if (rnPored > dbActNomer) { // ?? ако документа е от минала година трябва ли да се гледа пак спрямо брояча?
			return "Невалиден регистров номер. За пореден номер трябва да въведете число по-малко от " + dbActNomer + ", което да е свободно.";
		}

		// всичко е точно
		doc.setRnPrefix(rnPrefix);
		doc.setRnPored(rnPored);

		return null;
	}

	/**
	 * Генериране на регистров номер на документ по регистър
	 *
	 * @param doc
	 * @throws DbErrorException
	 */
	private void genRnDocByRegister(Doc doc) throws DbErrorException {
		try {
			StoredProcedureQuery storedProcedure = getEntityManager().createStoredProcedureQuery("gen_nom_register") //
				.registerStoredProcedureParameter(0, Integer.class, ParameterMode.IN) //
				.registerStoredProcedureParameter(1, String.class, ParameterMode.OUT) //
				.registerStoredProcedureParameter(2, String.class, ParameterMode.OUT) //
				.registerStoredProcedureParameter(3, Integer.class, ParameterMode.OUT);

			storedProcedure.setParameter(0, doc.getRegisterId());

			storedProcedure.execute();

			doc.setRnDoc((String) storedProcedure.getOutputParameterValue(1));
			doc.setRnPrefix((String) storedProcedure.getOutputParameterValue(2));
			doc.setRnPored((Integer) storedProcedure.getOutputParameterValue(3));

		} catch (Exception e) {
			throw new DbErrorException("Грешка при генериране на регистров номер на документ по регистър!", e);
		}
	}

	/**
	 * Генериране на регистров номер на документ по вид документ
	 *
	 * @param doc
	 * @throws ObjectInUseException 
	 */
	private void genRnDocByVidDoc(Doc doc) throws ObjectInUseException {
		try {
			StoredProcedureQuery storedProcedure = getEntityManager().createStoredProcedureQuery("gen_nom_vid_doc") //
				.registerStoredProcedureParameter(0, Integer.class, ParameterMode.IN) //
				.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN) //
				.registerStoredProcedureParameter(2, String.class, ParameterMode.OUT) //
				.registerStoredProcedureParameter(3, String.class, ParameterMode.OUT) //
				.registerStoredProcedureParameter(4, Integer.class, ParameterMode.OUT); //

			storedProcedure.setParameter(0, doc.getDocVid());
			storedProcedure.setParameter(1, doc.getRegistraturaId());

			storedProcedure.execute();

			doc.setRnDoc((String) storedProcedure.getOutputParameterValue(2));
			doc.setRnPrefix((String) storedProcedure.getOutputParameterValue(3));
			doc.setRnPored((Integer) storedProcedure.getOutputParameterValue(4));

		} catch (Exception e) {
			throw new ObjectInUseException("За избрания регистър и вид документ не може да бъде генериран регистрационен номер.");
		}
		
		if (doc.getRnDoc() == null || doc.getRnDoc().length() == 0) {
			throw new ObjectInUseException("За избрания регистър и вид документ не може да бъде генериран регистрационен номер.");
		}
	}

	/**
	 * Оправя поредния номер в списъците и ги събира в един. Поредния номер е в рамките на вид участник в документа
	 *
	 * @param all
	 * @param selected
	 */
	private void mergeReferentLists(List<DocReferent> all, List<DocReferent> selected) {
		if (selected == null || selected.isEmpty()) {
			return;
		}
		for (int i = 0; i < selected.size(); i++) {
			DocReferent dr = selected.get(i);

			dr.setPored(i + 1);
			all.add(dr);
		}
	}
	
	
	/**
	 * Намира основни данни за документа. Резултата е от вида:<br>
	 * [0]-OTNOSNO<br>
	 * [1]-DOC_VID <br>
	 * [2]-GUID<br>
	 * [3]-RN_DOC<br>
	 * [4]-DOC_DATE<br>
	 * [5]-CODE_REF_CORRESP <br>
	 * [6]-GUID_SEOS <br>
	 * [7]-GUID_SSEV <br> 
	 * [8]-ORG_EIK <br> 
	 * [9]-ORG_NAME <br>
	
	
	 * @param id
	 * @return намерения док или NULL ако за това ИД няма данни
	 * @throws DbErrorException
	 */
	public Object[] findDocDataForSeos(Integer id) throws DbErrorException {
		try {
			

			@SuppressWarnings("unchecked")
			List<Object[]> result = createNativeQuery("select OTNOSNO, DOC_VID, GUID, RN_DOC, DOC_DATE, CODE_REF_CORRESP, REGISTRATURI.GUID_SEOS, REGISTRATURI.GUID_SSEV, REGISTRATURI.ORG_EIK, REGISTRATURI.ORG_NAME from doc join REGISTRATURI on doc.REGISTRATURA_ID  =  REGISTRATURI.REGISTRATURA_ID where DOC_ID = :docId").setParameter("docId", id).getResultList();
			if (result.size() == 1) {
				return result.get(0);
			}

			LOGGER.error("findDocDataForSeos(docId={}) NOT FOUND!", id);
			return null;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на основни данни за документ!", e);
		}
	}
	
	/**
	 * Ако има връща броят им, ако няма връща 0.
	 * 
	 * @param docId
	 * @return
	 * @throws DbErrorException
	 */
	public int findDocMembersCount(Integer docId) throws DbErrorException {
		try {
			Number count = (Number) createNativeQuery("select count (*) cnt from DOC_MEMBERS where DOC_ID = ?1")
				.setParameter(1, docId).getSingleResult();
			return count.intValue();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на участници по документа!", e);
		}
	}
	
	/**
	 * Изтрива всички участници по документа
	 * 
	 * @param docId
	 * @throws DbErrorException 
	 * @throws ObjectInUseException 
	 */
	public void deleteDocMembers(Integer docId) throws DbErrorException, ObjectInUseException {
		DocMemberDAO docMemberDao = new DocMemberDAO(getUser());
		
		List<DocMember> docMembers = docMemberDao.findByDoc(docId);
		if (docMembers != null && !docMembers.isEmpty()) {
			for (DocMember docMember : docMembers) {
				docMemberDao.delete(docMember);
			}
		}
	}
	
	/**
	 * Запис само на основни данни на документ, като се прави журналиране на системна обработка.
	 * 
	 * @param doc
	 * @param ident
	 * @return
	 * @throws DbErrorException
	 */
	public Doc saveSysOkaJournal(Doc doc, String ident) throws DbErrorException {
		doc.setAuditable(false); // ще се журналира по друг начин след записа
		doc = super.save(doc);

		SystemJournal journal = new SystemJournal(doc.getCodeMainObject(), doc.getId(), ident);

		journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
		journal.setDateAction(new Date());
		journal.setIdUser(getUserId());

		saveAudit(journal);

		return doc;
	}
	
	/**
	 * като се подаде елементите от резултата на търсенето, ще се формира правилен номер за визуализиране
	 * 
	 * @param rnDoc
	 * @param docDate
	 * @param pored
	 * @return
	 */
	public static String formRnDocDate(Object rnDoc, Object docDate, Object pored) {
		if (SystemData.isDocPoredDeloGen() && pored != null) {
			return rnDoc + "#" + pored + "/" + DateUtils.printDate((Date) docDate);
		}
		return rnDoc + "/" + DateUtils.printDate((Date) docDate);
	}
	
	/**
	 * като се подаде елементите от резултата на търсенето, ще се формира правилен номер за визуализиране
	 * 
	 * @param rnDoc
	 * @param pored
	 * @return
	 */
	public static String formRnDoc(Object rnDoc, Object pored) {
		if (SystemData.isDocPoredDeloGen() && pored != null) {
			return rnDoc + "#" + pored;
		}
		return (String) rnDoc;
	}
	
	/** 
	 * Формира част от селекът за да се слепи поредния номер. Гледа се и настройката.
	 * 
	 * @param alias трябва да се подава заедно с точката
	 * @param dialect
	 * @return
	 */
	public static String formRnDocSelect(String alias, String dialect) {
		
		if (SystemData.isDocPoredDeloGen()) {
			if ("SQLSERVER".equals(dialect)) {
				return "case when "+alias+"PORED_DELO is null then "+alias+"RN_DOC else concat("+alias+"RN_DOC ,'#', "+alias+"PORED_DELO) end";
			} 
			return "case when "+alias+"PORED_DELO is null then "+alias+"RN_DOC else "+alias+"RN_DOC || '#' || "+alias+"PORED_DELO end";
		}
		return alias+"RN_DOC";
	}
	
	
	/**
	 * [0]-Тип обект<br>
	 * [1]-ИД обект<br>
	 * [2]-Номер/Дата<br>
	 * [3]-Дата на регистарция<br>
	 * [4]-Пояснителен текст<br>
	 * [5]-файлове- List<Object[]> -[0]file_id, [1]filename<br>
	 * [6]- 1=има достъп, 2=няма достъп<br>
	 * 
	 * @param argDocId
	 * @param sd
	 * @param ud 
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> selectDocAndFiles(Integer argDocId, BaseSystemData sd, UserData ud) throws DbErrorException {
		List<Object[]> rows = new ArrayList<>();

		try {
			// преписката, в която е вложен документа
			List<Object[]> docDeloRows = JPA.getUtil().getEntityManager().createNativeQuery(
				"select doc.rn_doc, doc.doc_date, doc.pored_delo, delo.delo_id from doc"
				+ " left outer join delo_doc dd on dd.input_doc_id = doc.doc_id"
				+ " left outer join delo on delo.delo_id = dd.delo_id and delo.delo_type <> ?1"
				+ " where doc.doc_id = ?2 order by dd.id")
				.setParameter(1, OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM).setParameter(2, argDocId).getResultList();
			if (docDeloRows.isEmpty()) {
				return rows; // не е открит документа
			}

			String rnDoc = formRnDocDate(docDeloRows.get(0)[0], docDeloRows.get(0)[1], docDeloRows.get(0)[2]);
			Integer deloId = null;
			for (Object[] docDelo : docDeloRows) {
				if (docDelo[3] != null) { // търсим първото което не е номенклатуртно дело
					deloId = ((Number) docDelo[3]).intValue();
					break;
				}
			}
			
			StringBuilder sql = new StringBuilder();
			if (deloId != null) {
				sql.append(" select d.doc_id, d.rn_doc, d.doc_date, d.pored_delo, dd.date_reg, d.doc_type, d.doc_vid, d.code_ref_corresp, d.work_off_id, 0 rel_type, 0 prava ");
				sql.append(" from delo_doc dd inner join doc d on d.doc_id = dd.input_doc_id where dd.delo_id = :deloId and dd.input_doc_id <> :docId ");
				sql.append(" union ");
			}
			sql.append(" select d1.doc_id, d1.rn_doc, d1.doc_date, d1.pored_delo, dd.date_reg, d1.doc_type, d1.doc_vid, d1.code_ref_corresp, d1.work_off_id, dd.rel_type, 2 prava ");
			sql.append(" from doc_doc dd inner join doc d1 on d1.doc_id = dd.doc_id1 where dd.doc_id2 = :docId ");
			sql.append(" union ");
			sql.append(" select d2.doc_id, d2.rn_doc, d2.doc_date, d2.pored_delo, dd.date_reg, d2.doc_type, d2.doc_vid, d2.code_ref_corresp, d2.work_off_id, dd.rel_type, 1 prava ");
			sql.append(" from doc_doc dd inner join doc d2 on d2.doc_id = dd.doc_id2 where dd.doc_id1 = :docId ");
			sql.append(" order by 6, 5 "); // важно е да е сортирано по тип документ, за да са първи собствените
			
			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql.toString())
				.setParameter("docId", argDocId);
			if (deloId != null) {
				query.setParameter("deloId", deloId);
			}
			
			Map<Integer, Integer> map = new HashMap<>(); // key=docId, value=index in List<Object[]> rows = new ArrayList<>();
			
			List<Object[]> temp = query.getResultList();
			for (Object[] t : temp) {
				Object[] row = new Object[7];

				Integer docId = ((Number) t[0]).intValue();
				Integer workOffId = asInteger(t[8]);
				Integer docType = asInteger(t[5]);
				
				Integer relType = asInteger(t[9]);
				String relName = null;
				if (relType != null && relType.intValue() != 0) {
					relName = ((Number)t[9]).intValue() == 1 // права или обратна връзка
						? sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_REL_TYPE, relType, getUserLang(), null) 
						: sd.decodeItemDopInfo(OmbConstants.CODE_CLASSIF_DOC_REL_TYPE, relType, getUserLang(), null);
				}
				
				boolean overlap = false;
				if (Objects.equals(docType, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) // работен
					&& workOffId != null && map.containsKey(workOffId)) { // чиито собствен вече присъства в резултатата
					
					overlap = true; // припокриваме работния
					map.put(docId, map.get(workOffId)); // вече и работния присъства с индекса на собствения
				}
				
				if (map.containsKey(docId)) {

					if (relName != null && !overlap) { // трябва да се гледа дали има връзка и то в случаите само, когато не се припокрили
						Object[] prev = rows.get(map.get(docId));
						if (prev != null) {
							prev[4] = prev[4] + ", " + relName + " " + rnDoc;
						}
					}
					continue;
				}
				map.put(docId, rows.size());
				
				row[0] = OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC;
				row[1] = docId;
				row[2] = DocDAO.formRnDocDate(t[1], t[2], t[3]);
				row[3] = t[4];

				StringBuilder info = new StringBuilder();
				info.append(sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, ((Number) t[6]).intValue(), getUserLang(), null));
				info.append(" (" + sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_TYPE, docType, getUserLang(), null) + ")");
				if (t[7] != null) {
					info.append(", кореспондент: " + sd.decodeItem(Constants.CODE_CLASSIF_REFERENTS, ((Number) t[7]).intValue(), getUserLang(), (Date) t[2]));
				}
				if (relName != null) {
					info.append(", " + relName + " " + rnDoc);
				}
				row[4] = info.toString();

				// ще се извлича само за тези, до които има достъп
//				Query fileQuery = JPA.getUtil().getEntityManager().createNativeQuery( //
//					"select f.file_id, f.filename from files f inner join file_objects fo on f.file_id = fo.file_id" //
//						+ " where fo.object_id = ?1 and fo.object_code = ?2 order by fo.id");
//				row[5] = fileQuery.setParameter(1, docId).setParameter(2, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC) //
//					.getResultList();
				row[5] = new ArrayList<>();
				
				row[6] = SysConstants.CODE_ZNACHENIE_NE; // на този етап никой няма
				
				rows.add(row);
			}
			
			if (rows.isEmpty()) {
				return rows;
			}

			// иска се налагане и на достъп, но само да се знае и в последствие да не се дават файловете. !?!?
			Map<String, Object> params = new HashMap<>();
			
			StringBuilder from = new StringBuilder(" from doc d ");
			from.append(" left outer join file_objects fo on fo.object_id = d.doc_id and fo.object_code = :objCode ");
			from.append(" left outer join files f on f.file_id = fo.file_id ");
			params.put("objCode", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);

			StringBuilder where = new StringBuilder(" where d.doc_id in (:docIdSet) ");
			params.put("docIdSet", map.keySet());
			
			new DocSearch().addAccessRules(where, from, params, ud, true); // налага се достъп по начина който е в справката
			Query accessQuery = createNativeQuery(" select distinct d.doc_id, f.file_id, f.filename, fo.id " 
													+ from + where + " order by d.doc_id, fo.id ");
			
			for(Entry<String, Object> entry : params.entrySet()) {
				accessQuery.setParameter(entry.getKey(), entry.getValue());
			}
			temp = accessQuery.getResultList();
			for (Object[] t : temp) {
				Integer docId = ((Number)t[0]).intValue();
				
				Integer index = map.get(docId);
				if (index != null) {
					if (t[1] != null) {
						((List<Object[]>)rows.get(index)[5]).add(new Object[]{t[1],t[2]});
					}
					rows.get(index)[6] = SysConstants.CODE_ZNACHENIE_DA; // нека този да има достъп
				}
			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при изпълнение на справка!", e);
		}

		rows.sort((Object[] o1, Object[] o2) -> ((Date) o1[3]).compareTo((Date) o2[3]));
		return rows;
	}
}

package com.ib.omb.db.dao;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_STATUS_CONTINUED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_IN_NEW_IN_DELO;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_JALBA_ADD_FILES;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_JALBA_DISTRIBUT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_UPALNOMOSTEN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_VODEST;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocJalba;
import com.ib.omb.db.dto.DocJalbaResult;
import com.ib.omb.experimental.Notification;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link DocJalba}
 *
 * @author belev
 */
public class DocJalbaDAO extends AbstractDAO<DocJalba> {

	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(DocJalbaDAO.class);

	/** @param user */
	public DocJalbaDAO(ActiveUser user) {
		super(DocJalba.class, user);
	}

	/** */
	@Override
	public DocJalba findById(Object id) throws DbErrorException {
		DocJalba jalba = super.findById(id);

		if (jalba == null) {
			return jalba;
		}

		try {
			jalba.setDbSast(jalba.getSast());
			jalba.setDbCodeZveno(jalba.getCodeZveno());
			jalba.setDbCodeExpert(jalba.getCodeExpert());

			jalba.getJalbaResultList().size(); // lazy

			@SuppressWarnings("unchecked")
			List<Object> expertCodes = createNativeQuery("select code_ref from doc_experts where doc_id = ?1") //
				.setParameter(1, jalba.getId()).getResultList();
			if (!expertCodes.isEmpty()) {
				List<Integer> list = new ArrayList<>();
				for (Object row : expertCodes) {
					list.add(((Number) row).intValue());
				}
				jalba.setDopExpertCodes(list);
				jalba.setDbDopExpertCodes(new ArrayList<>(list));
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на допълнителни експерти!", e);
		}

		return jalba;
	}

	/**
	 * Връща обекта зареден с даннните само от таблица DOC_JALBA, без връзка с документ и списъчните полета.
	 *
	 * @param docId
	 * @return
	 * @throws DbErrorException
	 */
	public DocJalba findJbpDataPlik(Integer docId) throws DbErrorException {
		return super.findById(docId);
	}

	/**
	 * Дава ИД на основна жалба, от която е породена текущата или ИД на текущата ако тя не е породена от друга.
	 *
	 * @param docId ИД на текущата жалба, от която ще се прави породена
	 * @return
	 * @throws DbErrorException
	 */
	public Integer findOsnPorodenaId(Integer docId) throws DbErrorException {
		if (docId == null) {
			return docId;
		}
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select dd.id, dd.doc_id1, dd.doc_id2, dd.rel_type ");
			sql.append(" from doc_doc dd ");
			sql.append(" left outer join doc_jalba j1 on j1.doc_id = dd.doc_id1 and dd.doc_id2 = :docId and dd.rel_type = :r16 ");
			sql.append(" left outer join doc_jalba j2 on j2.doc_id = dd.doc_id2 and dd.doc_id1 = :docId and dd.rel_type = :r15 ");
			sql.append(" where j1.doc_id is not null or j2.doc_id is not null ");

			@SuppressWarnings("unchecked")
			List<Object[]> porodeni = createNativeQuery(sql.toString()) //
				.setParameter("docId", docId) //
				.setParameter("r15", OmbConstants.CODE_ZNACHENIE_DOC_REL_E_PORODENA) //
				.setParameter("r16", OmbConstants.CODE_ZNACHENIE_DOC_REL_PORAJDA) //
				.getResultList();

			if (porodeni.isEmpty()) {
				return docId; // тази не е породена от друга
			}

			Object[] row = porodeni.get(0);

			if (row[1] != null && !docId.equals(((Number) row[1]).intValue())) {
				return ((Number) row[1]).intValue();
			}
			if (row[2] != null && !docId.equals(((Number) row[2]).intValue())) {
				return ((Number) row[2]).intValue();
			}
			return docId; // не би трябвало да се стигне до тук

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на породени жалби!", e);
		}
	}

	/**
	 * След потвърждение, този служител да се определи за водещ експерт на избраните жалби, състоянието на жалбите да се промени
	 * на „проверка“ и да се актуализира датата на състоянието на текуща дата.
	 *
	 * @param selected
	 * @param codeExpert
	 * @param systemData
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public void groupSetExpert(List<Long> selected, Integer codeExpert, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		DocDAO docDao = new DocDAO(getUser());

		for (Long docId : selected) {
			Doc doc = docDao.findById(docId.intValue()); // правя го със финд/саве, защото трябва да се формират нотификации и
															// също така, за да си влезне в журнала както трябва

			if (doc.getJalba() != null && doc.getJalba().getId() != null) { // за всеки случай

				doc.getJalba().setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_CHECK);
				doc.getJalba().setSastDate(new Date());
				doc.getJalba().setCodeExpert(codeExpert);

				docDao.save(doc, false, null, null, systemData);
			}
		}
	}

	/**
	 * Нотификацията се изпраща до водещия експерт на жалбата и до ръководителя на звеното, на което е разпределена жалбата (ако
	 * повече служители са на ръководна длъжност в това звено – до всеки от тях).
	 *
	 * @param docId
	 * @param sd
	 * @return @return 0 няма нови файлове, >0 броя на новите файлове, -1 грешка при формиране на нотифиакция, -2 няма водещия
	 *         експерт на жалбата или ръководителя на звеното, на което е разпределена
	 * @throws DbErrorException
	 */
	public int notifAddFilesInDoc(Integer docId, BaseSystemData sd) throws DbErrorException {
		Object[] row;
		Set<Integer> adresati = new HashSet<>();
		try { // първо трябва да се изрови до кой се праща и дали има какво да се праща

			StringBuilder sql = new StringBuilder();
			sql.append(" select d.rn_doc, d.pored_delo, d.doc_date, j.code_expert, j.code_zveno, count (*) cnt ");
			sql.append(" from doc_jalba j ");
			sql.append(" inner join doc d on d.doc_id = j.doc_id ");
			sql.append(" inner join file_objects fo on fo.object_id = d.doc_id and fo.object_code = ?1 ");
			sql.append(" where j.doc_id = ?2 and (j.date_last_notif is null or fo.date_reg > j.date_last_notif) ");
			sql.append(" group by d.rn_doc, d.pored_delo, d.doc_date, j.code_expert, j.code_zveno ");

			@SuppressWarnings("unchecked")
			List<Object[]> rows = createNativeQuery(sql.toString()) //
				.setParameter(1, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC).setParameter(2, docId).getResultList();
			if (rows.isEmpty()) {
				return 0; // няма нови файлове
			}
			row = rows.get(0);

			if (row[3] != null) { // има експерт
				adresati.add(((Number) row[3]).intValue());
			}
			if (row[4] != null) { // има звено към което е разпределена и тука е по тегаво
				List<SystemClassif> children = sd.getChildrenOnNextLevel(CODE_CLASSIF_ADMIN_STR, ((Number) row[4]).intValue(), null, CODE_DEFAULT_LANG);
				for (SystemClassif item : children) {
					Integer position = (Integer) sd.getItemSpecific(CODE_CLASSIF_ADMIN_STR, item.getCode(), CODE_DEFAULT_LANG, null, OmbClassifAdapter.ADM_STRUCT_INDEX_POSITION);

					if (position != null && sd.matchClassifItems(OmbConstants.CODE_CLASSIF_BOSS_POSITIONS, position, null)) {
						adresati.add(item.getCode());
					}
				}
			}

		} catch (Exception e) {
			LOGGER.error("Грешка при търсене на жалба/адресати за нотификация {}", CODE_ZNACHENIE_NOTIFF_EVENTS_JALBA_ADD_FILES, e);
			return -1; // грешка при формиране на нотифиакция
		}

		int sent;
		if (adresati.isEmpty()) {
			sent = -2; // няма до кого да се прати
		} else {
			Doc doc = new Doc();
			doc.setId(docId);
			doc.setRnDoc((String) row[0]);
			doc.setPoredDelo(SearchUtils.asInteger(row[1]));
			doc.setDocDate((Date) row[2]);

			sent = ((Number) row[5]).intValue();

			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_JALBA_ADD_FILES, CODE_ZNACHENIE_NOTIF_ROLIA_VODEST, (SystemData) sd);

			notif.setDoc(doc);
			notif.getAdresati().addAll(adresati);
			notif.send();

			try {
				createNativeQuery("update doc_jalba set date_last_notif = ?1 where doc_id = ?2") //
					.setParameter(1, new Date()).setParameter(2, docId).executeUpdate();
			} catch (Exception e) {
				throw new DbErrorException("Грешка при актуализиране на дата на последно изпращане на нотификация за файлове по жалба.", e);
			}
		}
		return sent;
	}

	/**
	 * В екрана на жалба, при преход към състояние на жалбата = приключена, се проверява дали жалбата има породени жалби, които са
	 * в състояние <> приключена и са срещу нарушителя в основната жалба. Ако има, се появява чек бокс „Да се приключат породените
	 * жалби“ (може би е добре да е включен по подразбиране).
	 *
	 * @param docId
	 * @param codeNar
	 * @return
	 * @throws DbErrorException
	 */
	public int selectPorodeniCount(Integer docId, Integer codeNar) throws DbErrorException {
		if (codeNar == null) {
			return 0; // няма как да се открият ако не е ясен нарушителя
		}
		List<Object[]> list = selectPorodeniList(docId, codeNar);
		return list.size();
	}

	/**
	 * Всички промени на състояние на жалба/нпм/самосезиране<br>
	 * [0]-sast<br>
	 * [1]-sast_date<br>
	 * [2]-sast_user<br>
	 *
	 * @param docId
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> selectSastHistory(Integer docId) throws DbErrorException {
		if (docId == null) {
			return new ArrayList<>();
		}
		try {
			return createNativeQuery("select sast, sast_date, sast_user from doc_sast_history where doc_id = ?1 order by id") //
				.setParameter(1, docId).getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на история на състояние!", e);
		}
	}

	/**
	 * Преди да се изтрие жалбата трябва да се изтрият и други данни, които не са мапнати дирекно през JPA
	 */
	@Override
	protected void remove(DocJalba entity) throws DbErrorException, ObjectInUseException {
		// doc_sast_history
		int deleted = createNativeQuery("delete from doc_sast_history where doc_id = ?1").setParameter(1, entity.getId()).executeUpdate();
		LOGGER.debug("Изтрити са {} история на състояния по жалба с DOC_ID={}", deleted, entity.getId());

		// doc_experts
		deleted = createNativeQuery("delete from doc_experts where doc_id = ?1").setParameter(1, entity.getId()).executeUpdate();
		LOGGER.debug("Изтрити са {} експерти по жалба с DOC_ID={}", deleted, entity.getId());

		super.remove(entity);
	}

	/**
	 * Приключване на преписка
	 */
	boolean completeDelo(Integer docId, String msg) throws DbErrorException {
		@SuppressWarnings("unchecked")
		List<Delo> deloList = createQuery("select d from Delo d where d.initDocId = ?1") //
			.setParameter(1, docId).getResultList();
		if (deloList.isEmpty()) {
			return false;
		}
		Delo delo = deloList.get(0);
		if (delo.getStatus() != null && delo.getStatus().equals(CODE_ZNACHENIE_DELO_STATUS_COMPLETED)) {
			return false; // вече е приключено
		}

		DeloDAO deloDao = new DeloDAO(getUser());

		delo.setStatus(CODE_ZNACHENIE_DELO_STATUS_COMPLETED);
		delo.setStatusDate(new Date());

		boolean closeTasks = false; // какво се прави със задачите
		deloDao.completeDelo(delo, closeTasks, null); // приключване на делото
		deloDao.saveSysOkaJournal(delo, "Преписка " + delo.getIdentInfo() + " е приключена поради приключване на работа по " + msg + ".");

		return true;
	}

	/**
	 * Продължаване на преписка
	 */
	boolean continueDelo(Integer docId, String msg) throws DbErrorException {
		@SuppressWarnings("unchecked")
		List<Delo> deloList = createQuery("select d from Delo d where d.initDocId = ?1") //
			.setParameter(1, docId).getResultList();
		if (deloList.isEmpty()) {
			return false;
		}
		Delo delo = deloList.get(0);
		if (delo.getStatus() != null && delo.getStatus().equals(CODE_ZNACHENIE_DELO_STATUS_CONTINUED)) {
			return false; // вече е продължена
		}

		DeloDAO deloDao = new DeloDAO(getUser());

		delo.setStatus(CODE_ZNACHENIE_DELO_STATUS_CONTINUED);
		delo.setStatusDate(new Date());

		deloDao.saveSysOkaJournal(delo, "Преписка " + delo.getIdentInfo() + " е продължена поради подновяване на работа по " + msg + ".");
		return true;
	}

	/**
	 * нотификацията се изпраща до водещия експерт на жалбата и до ръководителя на звеното, на което е разпределена жалбата (ако
	 * повече служители са на ръководна длъжност в това звено – до всеки от тях)
	 */
	void notifNewInDocInDelo(Doc doc, Delo delo, BaseSystemData sd) {
		String expertName = null;
		Set<Integer> adresati = new HashSet<>();
		try { // първо трябва да се изрови до кой се праща

			@SuppressWarnings("unchecked")
			List<Object[]> rows = createNativeQuery( //
				"select j.doc_id, j.code_zveno, j.code_expert from doc_jalba j where j.doc_id = ?1") //
					.setParameter(1, delo.getInitDocId()).getResultList();
			if (rows.isEmpty()) {
				return; // няма данни за жалба по тази преписка
			}
			Object[] row = rows.get(0);

			if (row[2] != null) { // има експерт
				adresati.add(((Number) row[2]).intValue());

				expertName = sd.decodeItem(CODE_CLASSIF_ADMIN_STR, ((Number) row[2]).intValue(), CODE_DEFAULT_LANG, null);
			}
			if (row[1] != null) { // има звено към което е разпределена и тука е по тегаво
				List<SystemClassif> children = sd.getChildrenOnNextLevel(CODE_CLASSIF_ADMIN_STR, ((Number) row[1]).intValue(), null, CODE_DEFAULT_LANG);
				for (SystemClassif item : children) {
					Integer position = (Integer) sd.getItemSpecific(CODE_CLASSIF_ADMIN_STR, item.getCode(), CODE_DEFAULT_LANG, null, OmbClassifAdapter.ADM_STRUCT_INDEX_POSITION);

					if (position != null && sd.matchClassifItems(OmbConstants.CODE_CLASSIF_BOSS_POSITIONS, position, null)) {
						adresati.add(item.getCode());
					}
				}
			}

		} catch (Exception e) {
			LOGGER.error("Грешка при търсене на адресати за нотификация {}", CODE_ZNACHENIE_NOTIFF_EVENTS_IN_NEW_IN_DELO, e);
			return; // няма как това да спре регистрацията на документа
		}

		if (!adresati.isEmpty()) {
			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_EVENTS_IN_NEW_IN_DELO, CODE_ZNACHENIE_NOTIF_ROLIA_VODEST, (SystemData) sd);

			String comment;
			if (expertName == null) {
				comment = " Жалбата не е разпределена на водещ експерт.";
			} else {
				comment = " Жалбата е разпределена на " + expertName + ".";
			}

			notif.setDoc(doc);
			notif.setDelo(delo);
			notif.setComment(comment);
			notif.getAdresati().addAll(adresati);
			notif.send();
		}
	}

	/**
	 * Разработка на 4 нотификации за разпределена жалба
	 */
	void notifZvenoExpert(Integer oldZveno, Integer oldExpert, Doc doc, SystemData sd) {
		DocJalba jalba = doc.getJalba();

		if (oldExpert == null) {
			if (jalba.getCodeExpert() != null) { // нов водещ

				Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
					, CODE_ZNACHENIE_NOTIFF_JALBA_DISTRIBUT, CODE_ZNACHENIE_NOTIF_ROLIA_VODEST, sd);
				notif.setDoc(doc);
				notif.setAdresat(jalba.getCodeExpert());
				notif.send();
			}
		} else if (!oldExpert.equals(jalba.getCodeExpert())) { // разлика в водещите

			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, CODE_ZNACHENIE_NOTIFF_JALBA_DISTRIBUT, -CODE_ZNACHENIE_NOTIF_ROLIA_VODEST, sd);
			notif.setDoc(doc);
			notif.setAdresat(oldExpert);
			notif.send();

			if (jalba.getCodeExpert() != null) { // нов водещ

				Notification notif2 = new Notification(((UserData) getUser()).getUserAccess(), null //
					, CODE_ZNACHENIE_NOTIFF_JALBA_DISTRIBUT, CODE_ZNACHENIE_NOTIF_ROLIA_VODEST, sd);
				notif2.setDoc(doc);
				notif2.setAdresat(jalba.getCodeExpert());
				notif2.send();
			}
		}

		if (oldZveno == null) {
			if (jalba.getCodeZveno() != null) { // новo звено

				Set<Integer> adresati = findZvenoBossCodes(jalba.getCodeZveno(), sd);
				if (!adresati.isEmpty()) {
					Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
						, CODE_ZNACHENIE_NOTIFF_JALBA_DISTRIBUT, CODE_ZNACHENIE_NOTIF_ROLIA_UPALNOMOSTEN, sd);
					notif.setDoc(doc);
					notif.getAdresati().addAll(adresati);
					notif.send();
				}
			}
		} else if (!oldZveno.equals(jalba.getCodeZveno())) { // разлика в звената

			Set<Integer> adresati = findZvenoBossCodes(oldZveno, sd);
			if (!adresati.isEmpty()) {
				Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
					, CODE_ZNACHENIE_NOTIFF_JALBA_DISTRIBUT, -CODE_ZNACHENIE_NOTIF_ROLIA_UPALNOMOSTEN, sd);
				notif.setDoc(doc);
				notif.getAdresati().addAll(adresati);
				notif.send();
			}

			if (jalba.getCodeZveno() != null) { // новo звено

				adresati = findZvenoBossCodes(jalba.getCodeZveno(), sd);
				if (!adresati.isEmpty()) {
					Notification notif2 = new Notification(((UserData) getUser()).getUserAccess(), null //
						, CODE_ZNACHENIE_NOTIFF_JALBA_DISTRIBUT, CODE_ZNACHENIE_NOTIF_ROLIA_UPALNOMOSTEN, sd);
					notif2.setDoc(doc);
					notif2.getAdresati().addAll(adresati);
					notif2.send();
				}
			}
		}
	}

	/**
	 * При Запис на основната жалба, при включен чек бокс, се приключват породените жалби: <br>
	 *  Състояние = приключена <br>
	 *  Дата на състояние и Начин на финализиране се взимат от основната жалба <br>
	 *  Ако в основната жалба има въведен резултат до жалбоподателя (jbp=да), той се копира (с нов id) като се подменят имената
	 * на жалбоподателя в поле text_subject <br>
	 *  Ако има и други редове за резултат, те се копират (с нов id). <br>
	 * Накрая се приключва и преписката на породената жалба.
	 *
	 * @throws ObjectInUseException
	 * @throws DbErrorException
	 */
	void priklPorodeni(DocJalba osnJalba, SystemData sd) throws DbErrorException, ObjectInUseException {
		if (osnJalba.getCodeNar() == null) {
			return; // няма как да се открият ако не е ясен нарушителя
		}
		DocDAO docDao = new DocDAO(getUser());

		List<Object[]> porodeni = selectPorodeniList(osnJalba.getId(), osnJalba.getCodeNar());

		for (Object[] row : porodeni) {
			Doc doc = null; // трябва да се открие кой е другия документ във връзката

			if (row[1] != null && !osnJalba.getId().equals(((Number) row[1]).intValue())) {
				doc = docDao.findById(((Number) row[1]).intValue());

			} else if (row[2] != null && !osnJalba.getId().equals(((Number) row[2]).intValue())) {
				doc = docDao.findById(((Number) row[2]).intValue());
			}

			if (doc == null || doc.getJalba() == null || doc.getJalba().getId() == null) {
				continue; // за всеки случай да не изгърмим
			}
			DocJalba porodena = doc.getJalba();

			porodena.setSast(osnJalba.getSast());
			porodena.setSastDate(osnJalba.getSastDate());
			porodena.setFinMethod(osnJalba.getFinMethod());

			if (osnJalba.getJalbaResultList() != null && !osnJalba.getJalbaResultList().isEmpty()) {

				if (porodena.getJalbaResultList() == null) {
					porodena.setJalbaResultList(new ArrayList<>());
				}

				for (DocJalbaResult osnResult : osnJalba.getJalbaResultList()) {
					DocJalbaResult copy = new DocJalbaResult();

					copy.setDocId(porodena.getId());
					copy.setVidResult(osnResult.getVidResult());
					copy.setDateResult(osnResult.getDateResult());
					copy.setJbp(osnResult.getJbp());

					if (Objects.equals(osnResult.getJbp(), SysConstants.CODE_ZNACHENIE_DA)) {
						copy.setTextSubject(porodena.getJbpName());
					} else {
						copy.setTextSubject(osnResult.getTextSubject());
					}

					copy.setCodeSubject(osnResult.getCodeSubject());
					copy.setNote(osnResult.getNote());

					porodena.getJalbaResultList().add(copy);
				}
			}
			docDao.save(doc, false, null, null, sd); // трябва да сме готови
		}
	}

	/**
	 * Запис на жалба в контекста на документ. newSave е нужно защото жалбата има ИД-то на док и реално винаги е сетнато
	 *
	 * @param doc
	 * @param newSave <code>true</code> нов запис, <code>false</code> корекция
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	DocJalba save(Doc doc, boolean newSave, SystemData sd) throws DbErrorException, ObjectInUseException {
		DocJalba jalba = doc.getJalba();

		// за да не влизат празни стрингове в БД
		if ("".equals(jalba.getJbpEgn())) {
			jalba.setJbpEgn(null);
		}
		if ("".equals(jalba.getJbpLnc())) {
			jalba.setJbpLnc(null);
		}
		if ("".equals(jalba.getJbpEik())) {
			jalba.setJbpEik(null);
		}

		if (newSave) {
			if (jalba.getJalbaResultList() == null) { // иначе след нов запис ако се направи повторен дава грешка
				jalba.setJalbaResultList(new ArrayList<>());
			}
			if (jalba.getSast() == null) {
				jalba.setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_FILED);
			}
			if (SearchUtils.isEmpty(jalba.getJbpName())) {
				jalba.setJbpName("Анонимен");
			}

			if (jalba.getZasPrava() != null && jalba.getCodeZveno() == null) { // трябва да се изчисли звеното
				SystemClassif item = sd.decodeItemLite(OmbConstants.CODE_CLASSIF_ZAS_PRAVA, jalba.getZasPrava(), CODE_DEFAULT_LANG, null, false);
				if (item != null && !SearchUtils.isEmpty(item.getCodeExt())) {
					jalba.setCodeZveno(Integer.valueOf(item.getCodeExt().trim()));
				}
			}
		}

		boolean changeSast = !Objects.equals(jalba.getDbSast(), jalba.getSast());
		if (changeSast) {
			jalba.setSastDate(new Date()); // системна дата
		}

		if (jalba.getJbpName() != null) {
			String t = jalba.getJbpName().trim();

			if (t.indexOf('\t') != -1) { // таба ще обърка схемата
				t = t.replace("\t", " ");
			}
			while (t.indexOf("  ") != -1) { // не трябва да има двойни интервали, че и те ще объркат схемата
				t = t.replace("  ", " ");
			}

			jalba.setJbpName(t);
		}

		DocJalba saved = super.save(jalba);

		try {
			saveDopExperts(jalba, saved);
		} catch (Exception e) {
			throw new DbErrorException("Грешка при запис на допълнителни експерти!", e);
		}

		if (changeSast) {
			Boolean deloStatusChanged = null;
			if (saved.getSast().equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED)) {
				deloStatusChanged = completeDelo(saved.getId(), "жалбата");

			} else if (saved.getSast().equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_REOPENED)) {
				deloStatusChanged = continueDelo(saved.getId(), "жалбата");
			}
			jalba.setDeloStatusChanged(deloStatusChanged); // за всеки случай !
			saved.setDeloStatusChanged(deloStatusChanged);
		}

		notifZvenoExpert(jalba.getDbCodeZveno(), jalba.getDbCodeExpert(), doc, sd);

		if (saved.getSast().equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED) // това трябва винаги да е
			&& Boolean.TRUE.equals(jalba.getCompletePorodeni())) { // а тука само ако се поиска, като се взима от подадният обект
																	// от екрана, защото полето е transient
			priklPorodeni(saved, sd);
			jalba.setCompletePorodeni(false);
		}

		saved.setDbSast(saved.getSast());
		saved.setDbCodeZveno(saved.getCodeZveno());
		saved.setDbCodeExpert(saved.getCodeExpert());
		return saved;
	}

	/**
	 * Запис на експертите
	 */
	void saveDopExperts(DocJalba jalba, DocJalba saved) {
		if (jalba.getDopExpertCodes() == null || jalba.getDopExpertCodes().isEmpty()) { // на екрана няма

			if (jalba.getDbDopExpertCodes() != null && !jalba.getDbDopExpertCodes().isEmpty()) { // трие се ако има нещо от БД

				createNativeQuery("delete from DOC_EXPERTS where DOC_ID = ?1").setParameter(1, saved.getId()).executeUpdate();
				jalba.setDbDopExpertCodes(null);
			}

		} else { // на екрана има
			Query insert = null;

			if (jalba.getDbDopExpertCodes() == null || jalba.getDbDopExpertCodes().isEmpty()) { // но няма стари от БД

				insert = createNativeQuery("insert into DOC_EXPERTS(DOC_ID,CODE_REF) values(?1,?2)");
				for (Integer codeRef : jalba.getDopExpertCodes()) { // всички се явяват нови
					insert.setParameter(1, saved.getId()).setParameter(2, codeRef).executeUpdate();
				}

			} else { // прави се анализ дали има промяна

				for (Integer codeRef : jalba.getDopExpertCodes()) {
					if (jalba.getDbDopExpertCodes().remove(codeRef)) {
						//
					} else { // значи е за нов запис
						if (insert == null) {
							insert = createNativeQuery("insert into DOC_EXPERTS(DOC_ID,CODE_REF) values(?1,?2)");
						}
						insert.setParameter(1, saved.getId()).setParameter(2, codeRef).executeUpdate();
					}
				}
				if (!jalba.getDbDopExpertCodes().isEmpty()) { // ако нещо е останало е за изтриване
					createNativeQuery("delete from DOC_EXPERTS where DOC_ID = ?1 and CODE_REF in (?2)") //
						.setParameter(1, saved.getId()).setParameter(2, jalba.getDbDopExpertCodes()).executeUpdate();
				}
			}
			jalba.setDbDopExpertCodes(new ArrayList<>(jalba.getDopExpertCodes()));
		}

		// заради JPA трябва да се оправи и обекта, който ще се върне
		saved.setDopExpertCodes(jalba.getDopExpertCodes());
		saved.setDbDopExpertCodes(jalba.getDbDopExpertCodes());
	}

	/**
	 * Намира ръководител/ите на звеното
	 */
	private Set<Integer> findZvenoBossCodes(Integer codeZveno, BaseSystemData sd) {
		Set<Integer> adresati = new HashSet<>();
		try {
			List<SystemClassif> children = sd.getChildrenOnNextLevel(CODE_CLASSIF_ADMIN_STR, codeZveno, null, CODE_DEFAULT_LANG);
			for (SystemClassif item : children) {
				Integer position = (Integer) sd.getItemSpecific(CODE_CLASSIF_ADMIN_STR, item.getCode(), CODE_DEFAULT_LANG, null, OmbClassifAdapter.ADM_STRUCT_INDEX_POSITION);

				if (position != null && sd.matchClassifItems(OmbConstants.CODE_CLASSIF_BOSS_POSITIONS, position, null)) {
					adresati.add(item.getCode());
				}
			}
		} catch (Exception e) {
			LOGGER.error("Грешка при търсене на адресати за нотификация {}", CODE_ZNACHENIE_NOTIFF_JALBA_DISTRIBUT, e);
		}
		return adresati;
	}

	/**
	 * Намира данни за породените жалби.<br>
	 * [0]-id<br>
	 * [1]-doc_id1<br>
	 * [2]-doc_id2<br>
	 * [3]-rel_type<br>
	 */
	private List<Object[]> selectPorodeniList(Integer docId, Integer codeNar) throws DbErrorException {
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select dd.id, dd.doc_id1, dd.doc_id2, dd.rel_type ");
			sql.append(" from doc_doc dd ");
			sql.append(" left outer join doc_jalba j1 on j1.doc_id = dd.doc_id1 and dd.doc_id2 = :docId and dd.rel_type = :r15 ");
			sql.append(" 				and j1.code_nar = :codeNar and j1.sast <> :sast7 and j1.sast <> :sast8 ");
			sql.append(" left outer join doc_jalba j2 on j2.doc_id = dd.doc_id2 and dd.doc_id1 = :docId and dd.rel_type = :r16 ");
			sql.append(" 				and j2.code_nar = :codeNar and j2.sast <> :sast7 and j2.sast <> :sast8 ");
			sql.append(" where j1.doc_id is not null or j2.doc_id is not null ");

			@SuppressWarnings("unchecked")
			List<Object[]> porodeni = createNativeQuery(sql.toString()) //
				.setParameter("docId", docId) //
				.setParameter("r15", OmbConstants.CODE_ZNACHENIE_DOC_REL_E_PORODENA) //
				.setParameter("r16", OmbConstants.CODE_ZNACHENIE_DOC_REL_PORAJDA) //
				.setParameter("sast7", OmbConstants.CODE_ZNACHENIE_JALBA_SAST_REOPENED) //
				.setParameter("sast8", OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED) //
				.setParameter("codeNar", codeNar) //
				.getResultList();
			return porodeni;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на породени жалби!", e);
		}
	}
}

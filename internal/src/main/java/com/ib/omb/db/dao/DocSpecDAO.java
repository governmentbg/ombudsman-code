package com.ib.omb.db.dao;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_IN_NEW_IN_DELO;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_VODEST;

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
import com.ib.omb.db.dto.DocSpec;
import com.ib.omb.experimental.Notification;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.db.AbstractDAO;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;

/**
 * DAO for {@link DocSpec}
 *
 * @author belev
 */
public class DocSpecDAO extends AbstractDAO<DocSpec> {

	/**  */
	private static final Logger LOGGER = LoggerFactory.getLogger(DocSpecDAO.class);

	/** @param user */
	public DocSpecDAO(ActiveUser user) {
		super(DocSpec.class, user);
	}

	/** */
	@Override
	public DocSpec findById(Object id) throws DbErrorException {
		DocSpec spec = super.findById(id);

		if (spec == null) {
			return spec;
		}

		try {
			spec.setDbSast(spec.getSast());

			spec.getSpecOrganList().size(); // lazy

			@SuppressWarnings("unchecked")
			List<Object> expertCodes = createNativeQuery("select code_ref from doc_experts where doc_id = ?1") //
				.setParameter(1, spec.getId()).getResultList();
			if (!expertCodes.isEmpty()) {
				List<Integer> list = new ArrayList<>();
				for (Object row : expertCodes) {
					list.add(((Number) row).intValue());
				}
				spec.setDopExpertCodes(list);
				spec.setDbDopExpertCodes(new ArrayList<>(list));
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на допълнителни експерти!", e);
		}

		return spec;
	}

	/** */
	DocSpec save(Doc doc) throws DbErrorException {
		DocSpec spec = doc.getDocSpec();

		if (spec.getSpecOrganList() == null) { // иначе след нов запис ако се направи повторен дава грешка
			spec.setSpecOrganList(new ArrayList<>());
		}

		boolean changeSast = !Objects.equals(spec.getDbSast(), spec.getSast());
		if (changeSast) {
			spec.setSastDate(new Date()); // системна дата
		}

		DocSpec saved = super.save(spec);

		try {
			saveDopExperts(spec, saved);
		} catch (Exception e) {
			throw new DbErrorException("Грешка при запис на допълнителни експерти!", e);
		}

		if (changeSast) {
			Boolean deloStatusChanged = null;
			if (saved.getSast().equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED)) {

				String msg = doc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM) //
					? "заповедта за проверка по НПМ"
					: "решението за проверка по самосезиране";

				deloStatusChanged = new DocJalbaDAO(getUser()).completeDelo(saved.getId(), msg);

			} else if (saved.getSast().equals(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_REOPENED)) {
				String msg = doc.getDocVid().equals(OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM) //
					? "заповедта за проверка по НПМ"
					: "решението за проверка по самосезиране";

				deloStatusChanged = new DocJalbaDAO(getUser()).continueDelo(saved.getId(), msg);
			}
			spec.setDeloStatusChanged(deloStatusChanged); // за всеки случай !
			saved.setDeloStatusChanged(deloStatusChanged);
		}

		saved.setDbSast(saved.getSast());
		return saved;
	}

	/**
	 * Преди да се изтрие трябва да се изтрият и други данни, които не са мапнати дирекно през JPA
	 */
	@Override
	protected void remove(DocSpec entity) throws DbErrorException, ObjectInUseException {
		// doc_experts
		int deleted = createNativeQuery("delete from doc_experts where doc_id = ?1").setParameter(1, entity.getId()).executeUpdate();
		LOGGER.debug("Изтрити са {} експерти по документ с DOC_ID={}", deleted, entity.getId());

		super.remove(entity);
	}

	/**
	 * нотификацията се изпраща до ръководителя и до експертите от екипа, който извършва проверката
	 *
	 * @param saved
	 * @param notifDeloInclude
	 * @param systemData
	 */
	void notifNewInDocInDelo(Doc doc, Delo delo, BaseSystemData sd) {
		Set<Integer> adresati = new HashSet<>();
		try { // първо трябва да се изрови до кой се праща

			@SuppressWarnings("unchecked")
			List<Object> rows = createNativeQuery( //
				"select code_leader from doc_spec where doc_id = :docId and code_leader is not null union select code_ref from doc_experts where doc_id = :docId") //
					.setParameter("docId", delo.getInitDocId()).getResultList();
			if (rows.isEmpty()) {
				return; // няма данни за иницииращ документ по тази преписка
			}
			for (Object row : rows) {
				adresati.add(((Number) row).intValue());
			}

		} catch (Exception e) {
			LOGGER.error("Грешка при търсене на адресати за нотификация {}", CODE_ZNACHENIE_NOTIFF_EVENTS_IN_NEW_IN_DELO, e);
			return; // няма как това да спре регистрацията на документа
		}

		Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
			, CODE_ZNACHENIE_NOTIFF_EVENTS_IN_NEW_IN_DELO, CODE_ZNACHENIE_NOTIF_ROLIA_VODEST, (SystemData) sd);

		notif.setDoc(doc);
		notif.setDelo(delo);
		notif.setComment("");
		notif.getAdresati().addAll(adresati);
		notif.send();
	}

	/**
	 * Запис на експертите
	 */
	void saveDopExperts(DocSpec spec, DocSpec saved) {
		if (spec.getDopExpertCodes() == null || spec.getDopExpertCodes().isEmpty()) { // на екрана няма

			if (spec.getDbDopExpertCodes() != null && !spec.getDbDopExpertCodes().isEmpty()) { // трие се ако има нещо от БД

				createNativeQuery("delete from DOC_EXPERTS where DOC_ID = ?1").setParameter(1, saved.getId()).executeUpdate();
				spec.setDbDopExpertCodes(null);
			}

		} else { // на екрана има
			Query insert = null;

			if (spec.getDbDopExpertCodes() == null || spec.getDbDopExpertCodes().isEmpty()) { // но няма стари от БД

				insert = createNativeQuery("insert into DOC_EXPERTS(DOC_ID,CODE_REF) values(?1,?2)");
				for (Integer codeRef : spec.getDopExpertCodes()) { // всички се явяват нови
					insert.setParameter(1, saved.getId()).setParameter(2, codeRef).executeUpdate();
				}

			} else { // прави се анализ дали има промяна

				for (Integer codeRef : spec.getDopExpertCodes()) {
					if (spec.getDbDopExpertCodes().remove(codeRef)) {
						//
					} else { // значи е за нов запис
						if (insert == null) {
							insert = createNativeQuery("insert into DOC_EXPERTS(DOC_ID,CODE_REF) values(?1,?2)");
						}
						insert.setParameter(1, saved.getId()).setParameter(2, codeRef).executeUpdate();
					}
				}
				if (!spec.getDbDopExpertCodes().isEmpty()) { // ако нещо е останало е за изтриване
					createNativeQuery("delete from DOC_EXPERTS where DOC_ID = ?1 and CODE_REF in (?2)") //
						.setParameter(1, saved.getId()).setParameter(2, spec.getDbDopExpertCodes()).executeUpdate();
				}
			}
			spec.setDbDopExpertCodes(new ArrayList<>(spec.getDopExpertCodes()));
		}

		// заради JPA трябва да се оправи и обекта, който ще се върне
		saved.setDopExpertCodes(spec.getDopExpertCodes());
		saved.setDbDopExpertCodes(spec.getDbDopExpertCodes());
	}
}

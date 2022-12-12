package com.ib.omb.db.dao;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DEF_PRAVA;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import com.ib.omb.db.dto.DocDoc;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link DocDoc}
 *
 * @author belev
 */
public class DocDocDAO extends AbstractDAO<DocDoc> {

	/** @param user */
	public DocDocDAO(ActiveUser user) {
		super(DocDoc.class, user);
	}

	/**
	 * [0]-DOC_DOC.ID<br>
	 * [1]-REL_TYPE<br>
	 * [2]-REL_DATE<br>
	 * [3]-D1_DOC_ID<br>
	 * [4]-D1_RN_DOC<br>
	 * [5]-D1_DOC_DATE<br>
	 * [6]-D2_DOC_ID<br>
	 * [7]-D2_RN_DOC<br>
	 * [8]-D2_DOC_DATE<br>
	 * [9]-D1_OTNOSNO<br>
	 * [10]-D2_OTNOSNO<br>
	 *
	 * @param userData
	 * @param relFrom
	 * @param relTo
	 * @param rel
	 * @param d1From
	 * @param d1To
	 * @param d1Tip
	 * @param d1Vid
	 * @param d1Otnosno
	 * @param d2From
	 * @param d2To
	 * @param d2Tip
	 * @param d2Vid
	 * @param d2Otnosno
	 * @return
	 */
	public SelectMetadata createSelectReport(UserData userData, Date relFrom, Date relTo, List<Integer> rel //
		, Date d1From, Date d1To, Integer[] d1Tip, List<Integer> d1Vid, String d1Otnosno //
		, Date d2From, Date d2To, Integer[] d2Tip, List<Integer> d2Vid, String d2Otnosno) {

		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append(" select distinct dd.ID a0, dd.REL_TYPE a1, dd.DATE_REG a2, d1.DOC_ID a3, "
			+DocDAO.formRnDocSelect("d1.", dialect)+" a4, d1.DOC_DATE a5, d2.DOC_ID a6, "
			+DocDAO.formRnDocSelect("d2.", dialect)+" a7, d2.DOC_DATE a8 ");
		select.append(" , " + DialectConstructor.limitBigString(dialect, "d1.OTNOSNO", 300) + " a9 ");
		select.append(" , " + DialectConstructor.limitBigString(dialect, "d2.OTNOSNO", 300) + " a10 ");

		from.append(" from DOC_DOC dd inner join DOC d1 on d1.DOC_ID = dd.DOC_ID1 inner join DOC d2 on d2.DOC_ID = dd.DOC_ID2 ");

		where.append(" where d1.REGISTRATURA_ID = :registratura and d2.REGISTRATURA_ID = :registratura ");
		params.put("registratura", userData.getRegistratura());

		if (relFrom != null) {
			where.append(" and dd.DATE_REG >= :relFrom ");
			params.put("relFrom", DateUtils.startDate(relFrom));
		}
		if (relTo != null) {
			where.append(" and dd.DATE_REG <= :relTo ");
			params.put("relTo", DateUtils.endDate(relTo));
		}
		if (rel != null && !rel.isEmpty()) {
			where.append(" and dd.REL_TYPE in (:rel) ");
			params.put("rel", rel);
		}

		if (d1From != null) {
			where.append(" and d1.DOC_DATE >= :d1From ");
			params.put("d1From", DateUtils.startDate(d1From));
		}
		if (d1To != null) {
			where.append(" and d1.DOC_DATE <= :d1To ");
			params.put("d1To", DateUtils.endDate(d1To));
		}
		if (d1Tip != null && d1Tip.length != 0 && d1Tip.length < 3) {
			where.append(" and d1.DOC_TYPE in (:d1Tip) ");
			params.put("d1Tip", Arrays.asList(d1Tip));
		}
		if (d1Vid != null && !d1Vid.isEmpty()) {
			where.append(" and d1.DOC_VID in (:d1Vid) ");
			params.put("d1Vid", d1Vid);
		}
		d1Otnosno = SearchUtils.trimToNULL_Upper(d1Otnosno);
		if (d1Otnosno != null) {
			where.append(" and upper(d1.OTNOSNO) like :d1Otnosno ");
			params.put("d1Otnosno", "%" + d1Otnosno + "%");
		}

		if (d2From != null) {
			where.append(" and d2.DOC_DATE >= :d2From ");
			params.put("d2From", DateUtils.startDate(d2From));
		}
		if (d2To != null) {
			where.append(" and d2.DOC_DATE <= :d2To ");
			params.put("d2To", DateUtils.endDate(d2To));
		}
		if (d2Tip != null && d2Tip.length != 0 && d2Tip.length < 3) {
			where.append(" and d2.DOC_TYPE in (:d2Tip) ");
			params.put("d2Tip", Arrays.asList(d2Tip));
		}
		if (d2Vid != null && !d2Vid.isEmpty()) {
			where.append(" and d2.DOC_VID in (:d2Vid) ");
			params.put("d2Vid", d2Vid);
		}
		d2Otnosno = SearchUtils.trimToNULL_Upper(d2Otnosno);
		if (d2Otnosno != null) {
			where.append(" and upper(d2.OTNOSNO) like :d2Otnosno ");
			params.put("d2Otnosno", "%" + d2Otnosno + "%");
		}

		addAccessRules(where, from, params, userData, "d1");
		addAccessRules(where, from, params, userData, "d2");

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(distinct dd.ID) " + from + where);
		sm.setSql(select.toString() + from.toString() + where.toString());
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * Търсене на връзки между документи. Дава резултат от вида:<br>
	 * [0]-DOC_DOC.ID<br>
	 * [1]-REL_TYPE<br>
	 * [2]-DOC_ID - ИД на другия документ във връзката<br>
	 * [3]-RN_DOC<br>
	 * [4]-DOC_DATE<br>
	 * [5]-DOC_VID<br>
	 * [6]-DOC_TYPE<br>
	 * [7]-OTNOSNO<br>
	 * [8]-DOC_ID1 ако тази стойност е равна на подадения аргумент docId, REL_TYPE се разкодира с
	 * {@link BaseSystemData#decodeItem(Integer, Integer, Integer, Date)} иначе REL_TYPE се разкодира с
	 * {@link BaseSystemData#decodeItemDopInfo(Integer, Integer, Integer, Date)}<br>
	 * [9]-DOC_ID2<br>
	 * [10]-TEH_NOMER<br>
	 * [11]-TEH_DATE<br>
	 *
	 * @param docId
	 * @return
	 * @throws DbErrorException
	 */
	public List<Object[]> findDocDocList(Integer docId) throws DbErrorException {
		try {
			String dialect = JPA.getUtil().getDbVendorName();

			StringBuilder sql = new StringBuilder();

			sql.append(" select dd.ID a0, dd.REL_TYPE a1, d1.DOC_ID a2, "+DocDAO.formRnDocSelect("d1.", dialect)+" a3, d1.DOC_DATE a4, d1.DOC_VID a5, d1.DOC_TYPE a6, ");
			sql.append(DialectConstructor.limitBigString(dialect, "d1.OTNOSNO", 300) + " a7 ");
			sql.append(" , dd.DOC_ID1 a8, dd.DOC_ID2 a9, d1.TEH_NOMER a10, d1.TEH_DATE a11 ");
			sql.append(" from DOC_DOC dd inner join DOC d1 on d1.DOC_ID = dd.DOC_ID1 where dd.DOC_ID2 = :docId ");

			sql.append(" union ");

			sql.append(" select dd.ID, dd.REL_TYPE, d2.DOC_ID, "+DocDAO.formRnDocSelect("d2.", dialect)+", d2.DOC_DATE, d2.DOC_VID, d2.DOC_TYPE, ");
			sql.append(DialectConstructor.limitBigString(dialect, "d2.OTNOSNO", 300) + " a7 ");
			sql.append(" , dd.DOC_ID1, dd.DOC_ID2, d2.TEH_NOMER, d2.TEH_DATE ");
			sql.append(" from DOC_DOC dd inner join DOC d2 on d2.DOC_ID = dd.DOC_ID2 where dd.DOC_ID1 = :docId");

			sql.append(" order by 1");

			@SuppressWarnings("unchecked")
			List<Object[]> states = createNativeQuery(sql.toString()).setParameter("docId", docId).getResultList();

			return states;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на връзки между документи!", e);
		}
	}

	/**
	 * Изпълнява запис и корекция на ред от таблица DOC_DOC. Ако се иска нов запис се подава docDocId=NULL, иначе конкретния за
	 * корекция.
	 *
	 * @param docDocId
	 * @param relType
	 * @param docId1
	 * @param docId2
	 * @param validate
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public DocDoc save(Integer docDocId, Integer relType, Integer docId1, Integer docId2, boolean validate) throws DbErrorException, ObjectInUseException {
		if (relType == null) {
			return null; // няма как да стане ако това липсва
		}

		if (validate) {
			Integer osnovna = null;
			Integer porodena = null;
			if (relType.equals(OmbConstants.CODE_ZNACHENIE_DOC_REL_E_PORODENA)) {
				osnovna = docId2;
				porodena = docId1;
			} else if (relType.equals(OmbConstants.CODE_ZNACHENIE_DOC_REL_PORAJDA)) {
				osnovna = docId1;
				porodena = docId2;
			}
			if (osnovna != null && porodena != null) {
				String error = validatePorodenaRelation(docDocId, osnovna, porodena);
				if (error != null) {
					throw new ObjectInUseException(error);
				}
			}
		}

		DocDoc docDoc;
		if (docDocId == null) {
			docDoc = new DocDoc();
		} else {
			docDoc = findById(docDocId);
			if (docDoc == null) {
				throw new DbErrorException("DOC_DOC with ID=" + docDocId + " not found!");
			}
		}

		docDoc.setRelType(relType);
		docDoc.setDocId1(docId1);
		docDoc.setDocId2(docId2);
		docDoc = save(docDoc);

		return docDoc;
	}

	/**
	 * добавя достъпа в режим преглед
	 *
	 * @param where
	 * @param from
	 * @param params
	 * @param userData
	 * @param d        алиас на док таблицата
	 */
	private void addAccessRules(StringBuilder where, StringBuilder from, Map<String, Object> params, UserData userData, String d) {

		if (userData.isDocAccessDenied()) { // има документи, до които му е отказан достъпа
			String exclude = "exclude_" + d;

			from.append(" left outer join DOC_ACCESS " + exclude + " on ");
			from.append(exclude + ".DOC_ID = " + d + ".DOC_ID and ");
			from.append(exclude + ".CODE_REF = " + userData.getUserAccess() + " and ");
			from.append(exclude + ".EXCLUDE = 1 ");

			where.append(" and " + exclude + ".ID is null ");
		}

		if (userData.hasAccess(CODE_CLASSIF_DEF_PRAVA, CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW) //
			|| userData.hasAccess(CODE_CLASSIF_DEF_PRAVA, CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT)) {
			return; // режим преглед - пълен достъп
		}

		String access = "access_" + d;
		from.append(" inner join DOC_ACCESS_ALL " + access + " on " + access + ".DOC_ID = " + d + ".DOC_ID ");

		StringBuilder dop = new StringBuilder();
		dop.append(access + ".CODE_REF in (" + userData.getUserAccess() + "," + userData.getZveno() + ")");

		if (userData.getAccessZvenoList() != null) { // ако е шеф
			dop.append(" OR " + access + ".CODE_STRUCT in (" + userData.getAccessZvenoList() + ")");
		}
		dop.append(" OR " + d + ".FREE_ACCESS = :freeDa_" + d);

		params.put("freeDa_" + d, SysConstants.CODE_ZNACHENIE_DA);

		where.append(" and ( " + dop + " ) ");
	}

	/**
	 * двата документа във връзката трабва да са от вид жалба <br>
	 * основната не може да е породена <br>
	 * породената не може да е основна <br>
	 * породената може да има само една основна <br>
	 *
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	private String validatePorodenaRelation(Integer docDocId, Integer osnovna, Integer porodena) throws DbErrorException {
		try {
			List<Object> byVid = createNativeQuery("select doc_id from doc where doc_id in (?1, ?2) and doc_vid = ?3") //
				.setParameter(1, osnovna).setParameter(2, porodena).setParameter(3, OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA) //
				.getResultList();
			if (byVid.size() != 2) {
				return "Двата документа във връзката трабва да са от вид жалба.";
			}

			StringBuilder sql = new StringBuilder();
			sql.append(" select dd.id from doc_doc dd ");
			sql.append(" left outer join doc_jalba j1 on j1.doc_id = dd.doc_id1 and dd.doc_id2 = :docId and dd.rel_type = :r16 ");
			sql.append(" left outer join doc_jalba j2 on j2.doc_id = dd.doc_id2 and dd.doc_id1 = :docId and dd.rel_type = :r15 ");
			sql.append(" where (j1.doc_id is not null or j2.doc_id is not null) ");

			List<Object> byOsnovna = createNativeQuery(sql.toString()).setParameter("docId", osnovna) //
				.setParameter("r15", OmbConstants.CODE_ZNACHENIE_DOC_REL_E_PORODENA) //
				.setParameter("r16", OmbConstants.CODE_ZNACHENIE_DOC_REL_PORAJDA).getResultList();
			if (!byOsnovna.isEmpty()) {
				return "Основната жалба не може да е породена от друга жалба.";
			}

			List<Object> byPorodena = createNativeQuery(sql.toString()).setParameter("docId", porodena) //
				// разменяме вида на връзките в този случай
				.setParameter("r16", OmbConstants.CODE_ZNACHENIE_DOC_REL_E_PORODENA) //
				.setParameter("r15", OmbConstants.CODE_ZNACHENIE_DOC_REL_PORAJDA).getResultList();
			if (!byPorodena.isEmpty()) {
				return "От породена жалба не може да се пораждат други жалби.";
			}

			Query byPorodena2Query;
			if (docDocId == null) {
				byPorodena2Query = createNativeQuery(sql.toString());
			} else {
				byPorodena2Query = createNativeQuery(sql.toString() + " and dd.id <> :docDocId ").setParameter("docDocId", docDocId);
			}
			List<Object> byPorodena2 = byPorodena2Query.setParameter("docId", porodena) //
				.setParameter("r15", OmbConstants.CODE_ZNACHENIE_DOC_REL_E_PORODENA) //
				.setParameter("r16", OmbConstants.CODE_ZNACHENIE_DOC_REL_PORAJDA).getResultList();
			if (!byPorodena2.isEmpty()) {
				return "Породената жалба може да е във връзка само с една основна жалба, от която е породена.";
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на породени жалби!", e);
		}
		return null;
	}
}

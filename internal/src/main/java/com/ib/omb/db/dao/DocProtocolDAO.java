package com.ib.omb.db.dao;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK;
import static com.ib.system.utils.SearchUtils.trimToNULL_Upper;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Query;

import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloArchive;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocDestruct;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
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
 * DAO отговарящо за протокол за унищожаване на документи. Направено е като наследник на DocDAO, защото довлича много код, който е
 * много специфичен и то само за този вид докумен. Би могло да се използва и за архивиране на преписки!
 *
 * @author belev
 */
public class DocProtocolDAO extends DocDAO {

	/** DAO for {@link DeloArchive} */
	static class DeloArchiveDAO extends AbstractDAO<DeloArchive> {

		/** @param user */
		DeloArchiveDAO(ActiveUser user) {
			super(DeloArchive.class, user);
		}
	}

	/** DAO for {@link DocDestruct} */
	static class DocDestructDAO extends AbstractDAO<DocDestruct> {

		/** @param user */
		DocDestructDAO(ActiveUser user) {
			super(DocDestruct.class, user);
		}
	}

	/** 1=унищожване на документи , 2=архивиране на номенклатурни дела, 3=??? */
	private final int ptotocolType;

	/**
	 * @param ptotocolType
	 * @param user
	 */
	public DocProtocolDAO(int ptotocolType, ActiveUser user) {
		super(user);

		this.ptotocolType = ptotocolType;
	}

	/**
	 * Добавя дела към протокола
	 *
	 * @param protocol
	 * @param selectedDeloIds
	 * @throws DbErrorException
	 */
	public void addArchiveProtocolDela(Doc protocol, List<Integer> selectedDeloIds) throws DbErrorException {
		DeloArchiveDAO archiveDao = new DeloArchiveDAO(getUser());

		for (Integer deloId : selectedDeloIds) {
			DeloArchive entity = new DeloArchive(protocol.getId(), deloId);
			archiveDao.save(entity);
		}
	}

	/**
	 * Добавя документи към протокола
	 *
	 * @param protocol
	 * @param selectedDocIds
	 * @param check          при <code>true</code> се проверява дали документа не е включен в друг протокол
	 * @return ако e != null значи трябва да се покаже на потребителя. Това са документите, които вече са в включени в протоколи
	 * @throws DbErrorException
	 */
	public String addDestructProtocolDocs(Doc protocol, List<Integer> selectedDocIds, boolean check) throws DbErrorException {
		StringBuilder msg = new StringBuilder();

		Map<Integer, Object[]> found = new HashMap<>();
		if (check) {
			try {
				@SuppressWarnings("unchecked")
				List<Object[]> rows = createNativeQuery( //
					"select distinct d.DOC_ID, d.RN_DOC, d.DOC_DATE, d.PORED_DELO from DOC_DESTRUCT dd inner join DOC d on d.DOC_ID = dd.DOC_ID where d.DOC_ID in (?1)") //
						.setParameter(1, selectedDocIds).getResultList();
				for (Object[] row : rows) {
					found.put(((Number) row[0]).intValue(), row);
				}
			} catch (Exception e) {
				throw new DbErrorException("Грешка при проверка на избраните документи!", e);
			}
		}

		DocDestructDAO destructDao = new DocDestructDAO(getUser());

		for (Integer docId : selectedDocIds) {
			if (check && found.containsKey(docId)) {
				if (msg.length() > 0) {
					msg.append(", ");
				}
				Object[] row = found.get(docId);
				msg.append(DocDAO.formRnDocDate(row[1], row[2], row[3]));

			} else {
				DocDestruct entity = new DocDestruct(protocol.getId(), docId);
				destructDao.save(entity);
			}
		}

		return check && msg.length() > 0 ? msg.toString() : null;
	}

	/**
	 * Маркира делата за архивиране, които са в този протокол
	 *
	 * @param protocol
	 * @param meta     мета информация за вид протокол статус на дело
	 * @param sd
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public void completeArchiveProtocol(Doc protocol, Map<Integer, Integer> meta, BaseSystemData sd) throws DbErrorException {

		List<Delo> dela;
		try { // трябва да открия всички връзки, които се маркират за архивирани

			Query query = createQuery("select d from DeloArchive da inner join Delo d on d.id = da.deloId" //
				+ " where da.protocolId = :protId and (d.status is null or d.status != :archived)") //
					.setParameter("protId", protocol.getId()) //
					.setParameter("archived", meta.get(protocol.getDocVid()));

			dela = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при определяне на дела за архивиране!", e);
		}

		StringBuilder protIdent = new StringBuilder();
		if (protocol.getWorkOffData() != null && protocol.getWorkOffData().length > 2) {
			protIdent.append(protocol.getWorkOffData()[1] + "/");
			protIdent.append(DateUtils.printDate((Date) protocol.getWorkOffData()[2]));
		}
		String prodVid = sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, protocol.getDocVid(), getUserLang(), null);

		Set<Integer> archDeloIds = new HashSet<>();

		for (Delo delo : dela) { // пряко свързните с протокола
			delo.setStatus(meta.get(protocol.getDocVid()));
			delo.setStatusDate((Date) protocol.getWorkOffData()[2]);

			archiveDeloContent(delo, archDeloIds, prodVid, protIdent.toString());
		}
	}

	/**
	 * Маркира документите за унищожаване, които са в този протокол
	 *
	 * @param protocol
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public void completeDestructProtocol(Doc protocol) throws DbErrorException {

		List<Doc> docs = null;
		try { // трябва да открия всички връзки, които се маркират за унищожени

			Query query = createQuery("select d from DocDestruct dd inner join Doc d on d.id = dd.docId" //
				+ " where dd.protocolId = :protId and (d.valid is null or d.valid != :destruct)") //
					.setParameter("protId", protocol.getId()) //
					.setParameter("destruct", OmbConstants.CODE_CLASSIF_DOC_VALID_DESTRUCT);

			docs = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при определяне на документи за унищожаване!", e);
		}

		StringBuilder ident = new StringBuilder(" е унищожен с протокол за унищожаване ");
		if (protocol.getWorkOffData() != null && protocol.getWorkOffData().length > 2) {
			ident.append(protocol.getWorkOffData()[1] + "/");
			ident.append(DateUtils.printDate((Date) protocol.getWorkOffData()[2]));
		}
		ident.append(".");

		for (Doc doc : docs) {
			// този го минавам по по лекия път финд-саве, без всичите чудеса по свързани обектит
			doc.setValid(OmbConstants.CODE_CLASSIF_DOC_VALID_DESTRUCT);
			doc.setValidDate((Date) protocol.getWorkOffData()[2]);

			saveSysOkaJournal(doc, "Документ " + doc.getIdentInfo() + ident.toString());
		}
	}

	/**
	 * Свързване на проткола с официален документ
	 *
	 * @param protocol
	 * @param officialDocId
	 * @param systemData
	 * @param protType
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public Doc connectWithOfficial(Doc protocol, Integer officialDocId, BaseSystemData systemData, String protType) throws DbErrorException, ObjectInUseException {
		protocol.setWorkOffId(officialDocId);
		protocol.setProcessed(SysConstants.CODE_ZNACHENIE_DA);

		Doc saved = save(protocol, false, null, null, systemData);

		// този го минавам по по лекия път финд-саве, без всичите чудеса по свързани обектит
		Doc official = getEntityManager().find(Doc.class, officialDocId);

		StringBuilder ident = new StringBuilder("Документ " + official.getIdentInfo() + " е свързан с работен протокол за " + protType);
		ident.append(" " + saved.getIdentInfo());
		ident.append(".");

		official.setWorkOffId(protocol.getId());
		saveSysOkaJournal(official, ident.toString());

		saved.setWorkOffData(new Object[] { official.getId(), official.getRnDoc(), official.getDocDate() });

		return saved;
	}

	/**
	 * Търсене на протоколи за архивиране<br>
	 * Конструира селект, като изтегля само данните от вида:<br>
	 * [0]-DOC_ID (работен)<br>
	 * [1]-RN_DOC (работен)<br>
	 * [2]-DOC_DATE (работен)<br>
	 * [3]-DOC_ID (собствен)<br>
	 * [4]-RN_DOC (собствен)<br>
	 * [5]-DOC_DATE (собствен)<br>
	 * [6]-архивирани (да/не)<br>
	 * [7]-DOC_VID (работен)<br>
	 *
	 * @param registraturaId
	 * @param docVid          ако не е подадено търси за всички видове в meta, иначе за конкретния
	 * @param dateFromR
	 * @param dateToR
	 * @param withoutOfficial
	 * @param meta            мета информация за вид протокол статус на дело
	 * @param rnDocR
	 * @param rnDocReq
	 * @param dateFromS
	 * @param dateToS
	 * @param rnDocS
	 * @param rnDocSeq
	 * @return
	 */
	public SelectMetadata createSelectArchiveProtocol(Integer registraturaId, Integer docVid, Date dateFromR, Date dateToR, Boolean withoutOfficial, Map<Integer, Integer> meta //
		, String rnDocR, boolean rnDocReq, Date dateFromS, Date dateToS, String rnDocS, boolean rnDocSeq) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append(" select r.DOC_ID a0, " + DocDAO.formRnDocSelect("r.", dialect) + " a1, r.DOC_DATE a2, s.DOC_ID a3, " + DocDAO.formRnDocSelect("s.", dialect) + " a4, s.DOC_DATE a5 ");
		select.append(" , case when x.ALL_COUNT is null or x.ALL_COUNT > x.ARCH_COUNT then 2 else 1 end a6 ");
		select.append(" , r.DOC_VID a7 ");

		from.append(" from DOC r "); // работния
		from.append(" left outer join DOC s on s.DOC_ID = r.WORK_OFF_ID "); // собствения

		where.append(" where r.REGISTRATURA_ID = :registraturaId and r.DOC_TYPE = :docType and r.DOC_VID in (:docVidList) ");

		params.put("registraturaId", registraturaId);
		params.put("docType", CODE_ZNACHENIE_DOC_TYPE_WRK);
		if (docVid == null) {
			params.put("docVidList", meta.keySet()); // всички налични видове за тази дейност
		} else {
			params.put("docVidList", Arrays.asList(docVid));
		}

		// за работния
		if (dateFromR != null) {
			where.append(" and r.DOC_DATE >= :dateFromR ");
			params.put("dateFromR", DateUtils.startDate(dateFromR));
		}
		if (dateToR != null) {
			where.append(" and r.DOC_DATE <= :dateToR ");
			params.put("dateToR", DateUtils.endDate(dateToR));
		}
		String t = trimToNULL_Upper(rnDocR);
		if (t != null) {
			if (rnDocReq) { // пълно съвпадение case insensitive
				where.append(" and upper(r.RN_DOC) = :rnDocR ");
				params.put("rnDocR", t);

			} else {
				where.append(" and upper(r.RN_DOC) like :rnDocR ");
				params.put("rnDocR", "%" + t + "%");
			}
		}

		// за собствения
		if (dateFromS != null) {
			where.append(" and s.DOC_DATE >= :dateFromS ");
			params.put("dateFromS", DateUtils.startDate(dateFromS));
		}
		if (dateToS != null) {
			where.append(" and s.DOC_DATE <= :dateToS ");
			params.put("dateToS", DateUtils.endDate(dateToS));
		}
		t = trimToNULL_Upper(rnDocS);
		if (t != null) {
			if (rnDocSeq) { // пълно съвпадение case insensitive
				where.append(" and upper(s.RN_DOC) = :rnDocS ");
				params.put("rnDocS", t);

			} else {
				where.append(" and upper(s.RN_DOC) like :rnDocS ");
				params.put("rnDocS", "%" + t + "%");
			}
		}

		if (Boolean.TRUE.equals(withoutOfficial)) {
			where.append(" and r.WORK_OFF_ID is null ");
		}

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(*) " + from + where);

		// това надолу не влияе на count-а и за това е тука!!!

		from.append(" left outer join ( "); // изчисляването дали е архивиран или не
		from.append(" select p.DOC_ID, count (*) ALL_COUNT ");
		from.append(" , sum ( case ");
		for (Entry<Integer, Integer> entry : meta.entrySet()) {
			from.append(" when p.DOC_VID = " + entry.getKey() + " and d.STATUS = " + entry.getValue() + " then 1 ");
		}
		from.append(" else 0 end) ARCH_COUNT ");
		from.append(" from DOC p ");
		from.append(" inner join DELO_ARCHIVE da on da.PROTOCOL_ID = p.DOC_ID ");
		from.append(" inner join DELO d on d.DELO_ID = da.DELO_ID ");
		from.append(" group by p.DOC_ID ");
		from.append(" ) x on x.DOC_ID = r.DOC_ID ");

		sm.setSql(select.toString() + from.toString() + where.toString());
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * Търсене на списък дела по работен протокол за архивиране<br>
	 * Конструира селект, като изтегля само данните от вида:<br>
	 * [0]-DELO_ARCHIVE.ID<br>
	 * [1]-DELO_ID<br>
	 * [2]-RN_DELO<br>
	 * [3]-DELO_DATE<br>
	 * [4]-BR_TOM<br>
	 * [5]-STATUS<br>
	 * [6]-DELO_NAME<br>
	 * [7]-DATE_REG<br>
	 * [8]-PERIOD_TYPE<br>
	 * [9]-YEARS<br>
	 *
	 * @param protocol
	 * @param archived ако е NULL се игнорира, иначе при <code>true</code> търси само архивирани, при <code>false</code> различни
	 *                 от архивирани
	 * @param meta     мета информация за вид протокол статус на дело
	 * @return
	 */
	public SelectMetadata createSelectArchiveProtocolDela(Doc protocol, Boolean archived, Map<Integer, Integer> meta) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder(" where da.PROTOCOL_ID = :protocolId ");

		select.append(" select da.ID a0, d.DELO_ID a1, d.RN_DELO a2, d.DELO_DATE a3, d.BR_TOM a4, d.STATUS a5, ");
		select.append(DialectConstructor.limitBigString(dialect, "d.DELO_NAME", 300) + " a6 "); // max 300!
		select.append(" , da.DATE_REG a7, s.PERIOD_TYPE a8, s.YEARS a9 ");

		from.append(" from DELO_ARCHIVE da ");
		from.append(" inner join DELO d on d.DELO_ID = da.DELO_ID ");

		params.put("protocolId", protocol.getId());

		if (archived != null) {
			if (archived.booleanValue()) {
				where.append(" and d.STATUS = :archived ");
			} else {
				where.append(" and (d.STATUS is null or d.STATUS != :archived) ");
			}
			params.put("archived", meta.get(protocol.getDocVid()));
		}

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(*) " + from + where);

		from.append(" left outer join DOC_SHEMA s on s.PREFIX = d.RN_DELO and s.FROM_YEAR <= d.DELO_YEAR and (s.TO_YEAR is null or s.TO_YEAR >= d.DELO_YEAR) ");

		sm.setSql(select.toString() + from.toString() + where);

		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * Търсене на протоколи за унищожване<br>
	 * Конструира селект, като изтегля само данните от вида:<br>
	 * [0]-DOC_ID (работен)<br>
	 * [1]-RN_DOC (работен)<br>
	 * [2]-DOC_DATE (работен)<br>
	 * [3]-DOC_ID (собствен)<br>
	 * [4]-RN_DOC (собствен)<br>
	 * [5]-DOC_DATE (собствен)<br>
	 * [6]-унищожени (да/не)<br>
	 *
	 * @param registraturaId
	 * @param docVid
	 * @param dateFromR
	 * @param dateToR
	 * @param withoutOfficial
	 * @param rnDocR
	 * @param rnDocReq
	 * @param dateFromS
	 * @param dateToS
	 * @param rnDocS
	 * @param rnDocSeq
	 * @param incRnDoc
	 * @param incDocDate
	 * @return
	 */
	public SelectMetadata createSelectDestructProtocol(Integer registraturaId, Integer docVid, Date dateFromR, Date dateToR, Boolean withoutOfficial //
		, String rnDocR, boolean rnDocReq, Date dateFromS, Date dateToS, String rnDocS, boolean rnDocSeq, String incRnDoc, Date incDocDate) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		select.append(" select distinct r.DOC_ID a0, " + DocDAO.formRnDocSelect("r.", dialect) + " a1, r.DOC_DATE a2, s.DOC_ID a3, " + DocDAO.formRnDocSelect("s.", dialect) + " a4, s.DOC_DATE a5 ");
		select.append(", case when x.ALL_COUNT is null or x.ALL_COUNT > x.DESTRUCT_COUNT then 2 else 1 end a6 ");

		from.append(" from DOC r "); // работния
		from.append(" left outer join DOC s on s.DOC_ID = r.WORK_OFF_ID "); // собствения

		where.append(" where r.REGISTRATURA_ID = :registraturaId and r.DOC_TYPE = :docType and r.DOC_VID = :docVid ");

		params.put("registraturaId", registraturaId);
		params.put("docType", CODE_ZNACHENIE_DOC_TYPE_WRK);
		params.put("docVid", docVid);

		// за работния
		if (dateFromR != null) {
			where.append(" and r.DOC_DATE >= :dateFromR ");
			params.put("dateFromR", DateUtils.startDate(dateFromR));
		}
		if (dateToR != null) {
			where.append(" and r.DOC_DATE <= :dateToR ");
			params.put("dateToR", DateUtils.endDate(dateToR));
		}
		String t = trimToNULL_Upper(rnDocR);
		if (t != null) {
			if (rnDocReq) { // пълно съвпадение case insensitive
				where.append(" and upper(r.RN_DOC) = :rnDocR ");
				params.put("rnDocR", t);

			} else {
				where.append(" and upper(r.RN_DOC) like :rnDocR ");
				params.put("rnDocR", "%" + t + "%");
			}
		}

		// за собствения
		if (dateFromS != null) {
			where.append(" and s.DOC_DATE >= :dateFromS ");
			params.put("dateFromS", DateUtils.startDate(dateFromS));
		}
		if (dateToS != null) {
			where.append(" and s.DOC_DATE <= :dateToS ");
			params.put("dateToS", DateUtils.endDate(dateToS));
		}
		t = trimToNULL_Upper(rnDocS);
		if (t != null) {
			if (rnDocSeq) { // пълно съвпадение case insensitive
				where.append(" and upper(s.RN_DOC) = :rnDocS ");
				params.put("rnDocS", t);

			} else {
				where.append(" and upper(s.RN_DOC) like :rnDocS ");
				params.put("rnDocS", "%" + t + "%");
			}
		}

		if (Boolean.TRUE.equals(withoutOfficial)) {
			where.append(" and r.WORK_OFF_ID is null ");
		}

		t = trimToNULL_Upper(incRnDoc);
		if (t != null) {
			from.append(" inner join DOC_DESTRUCT idd on idd.PROTOCOL_ID = r.DOC_ID ");
			from.append(" inner join DOC id on id.DOC_ID = idd.DOC_ID ");
			where.append(" and upper(id.RN_DOC) = :incRnDoc ");
			params.put("incRnDoc", t);

			if (incDocDate != null) {
				where.append(" and id.DOC_DATE >= :incDocDateFrom ");
				params.put("incDocDateFrom", DateUtils.startDate(incDocDate));

				where.append(" and id.DOC_DATE <= :incDocDateTo ");
				params.put("incDocDateTo", DateUtils.endDate(incDocDate));
			}
		}

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(distinct r.DOC_ID) " + from + where);

		// това надолу не влияе на count-а и за това е тука!!!

		from.append(" left outer join ( "); // изчисляването дали е унищожен или не
		from.append(" 	select dd.PROTOCOL_ID, count (*) ALL_COUNT, sum (case when d.VALID = " //
			+ OmbConstants.CODE_CLASSIF_DOC_VALID_DESTRUCT + " then 1 else 0 end) DESTRUCT_COUNT ");
		from.append(" 	from DOC_DESTRUCT dd ");
		from.append(" 	inner join DOC d on d.DOC_ID = dd.DOC_ID ");
		from.append(" 	group by dd.PROTOCOL_ID ");
		from.append(" ) x on x.PROTOCOL_ID = r.DOC_ID ");

		sm.setSql(select.toString() + from.toString() + where.toString());
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * Търсене на списък документи по работен протокол за унищожаване<br>
	 * Конструира селект, като изтегля само данните от вида:<br>
	 * [0]-DOC_DESTRUCT.ID<br>
	 * [1]-DOC_ID<br>
	 * [2]-RN_DOC<br>
	 * [3]-DOC_DATE<br>
	 * [4]-DOC_TYPE<br>
	 * [5]-DOC_VID<br>
	 * [6]-VALID<br>
	 * [7]-OTNOSNO<br>
	 * [8]-CODE_REF_CORRESP<br>
	 * [9]-AUTHORS_CODES-кодовете на авторите с разделител запетая - пример (1,6,18)
	 * {@link SystemData#decodeItems(Integer, String, Integer, Date)}<br>
	 * [10]-DATE_REG<br>
	 * [11]-EKZ_NOMERA<br>
	 * [12]-COUNT_SHEETS<br>
	 *
	 * @param protocolId
	 * @param destructed ако е NULL се игнорира, иначе при <code>true</code> търси само унищожени, при <code>false</code> различни
	 *                   от унищожени
	 * @return
	 */
	public SelectMetadata createSelectDestructProtocolDocs(Integer protocolId, Boolean destructed) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder(" where dd.PROTOCOL_ID = :protocolId ");

		select.append(" select dd.ID a0, d.DOC_ID a1, " + DocDAO.formRnDocSelect("d.", dialect) + " a2, d.DOC_DATE a3, d.DOC_TYPE a4, d.DOC_VID a5, d.VALID a6, ");
		select.append(DialectConstructor.limitBigString(dialect, "d.OTNOSNO", 300) + " a7 "); // max 300!
		select.append(" , d.CODE_REF_CORRESP a8 ");
		select.append(" , CASE WHEN d.DOC_TYPE = 1 THEN null ELSE "); // за входящите авторите не се теглят
		select.append(DialectConstructor.convertToDelimitedString(dialect, "dr.CODE_REF", "DOC_REFERENTS dr where dr.DOC_ID = d.DOC_ID and dr.ROLE_REF = 1", "dr.PORED"));
		select.append(" END a9 ");
		select.append(" , dd.DATE_REG a10, dd.EKZ_NOMERA a11, d.COUNT_SHEETS a12 ");

		from.append(" from DOC_DESTRUCT dd ");
		from.append(" inner join DOC d on d.DOC_ID = dd.DOC_ID ");

		params.put("protocolId", protocolId);

		if (destructed != null) {
			if (destructed.booleanValue()) {
				where.append(" and d.VALID = :destructed ");
			} else {
				where.append(" and (d.VALID is null or d.VALID != :destructed) ");
			}
			params.put("destructed", OmbConstants.CODE_CLASSIF_DOC_VALID_DESTRUCT);
		}

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(*) " + from + where);

		sm.setSql(select.toString() + from.toString() + where);

		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * Развързване на протокола от официален документ
	 *
	 * @param protocol
	 * @param meta
	 * @param systemData
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public Doc disconnectFromOfficial(Doc protocol, Map<Integer, Integer> meta, BaseSystemData systemData) throws DbErrorException, ObjectInUseException {
		Integer officialDocId = protocol.getWorkOffId();

		protocol.setWorkOffId(null); // нулирам
		protocol.setWorkOffData(null); // нулирам
		protocol.setProcessed(SysConstants.CODE_ZNACHENIE_NE);

		Doc saved = save(protocol, false, null, null, systemData);

		// този го минавам по по лекия път финд-саве, без всичите чудеса по свързани обектит
		Doc official = getEntityManager().find(Doc.class, officialDocId);
		official.setWorkOffId(null); // ще се случи merge защото е изтеглен в тази сесия!

		String prodVid = systemData.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, protocol.getDocVid(), getUserLang(), null);

		String ident = "Премахната е връзката на документ " + official.getIdentInfo() + " с работен " + prodVid + " " + saved.getIdentInfo() + ".";
		saveSysOkaJournal(official, ident);

		// трябва да се оправят и делата/документите, които са включени в протокола

		if (this.ptotocolType == 1) { // документи
			disconnectFromOfficialIncludedDoc(saved, official.getIdentInfo(), systemData);

		} else if (this.ptotocolType == 2) { // дела
			disconnectFromOfficialIncludedDelo(protocol, meta, official.getIdentInfo(), systemData);
		}

		return saved;
	}

	/**
	 * Премахва дело от протокола за архивиране
	 *
	 * @param protocol
	 * @param id       ИД на връзката !!!
	 * @param status   статус на делото във връзката
	 * @param meta
	 * @param sd
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public void removeIncludedDelo(Doc protocol, Integer id, Integer status, Map<Integer, Integer> meta, BaseSystemData sd) throws DbErrorException, ObjectInUseException {
		DeloArchiveDAO archiveDao = new DeloArchiveDAO(getUser());

		DeloArchive entity = archiveDao.findById(id);
		archiveDao.delete(entity);

		StringBuilder protIdent = new StringBuilder();
		if (protocol.getWorkOffData() != null && protocol.getWorkOffData().length > 2) {
			protIdent.append(protocol.getWorkOffData()[1] + "/");
			protIdent.append(DateUtils.printDate((Date) protocol.getWorkOffData()[2]));
		}

		if (Objects.equals(status, meta.get(protocol.getDocVid()))) {

			Object[] row;
			try {
				StringBuilder sql = new StringBuilder();
				sql.append(" select d, s.docDate, p.docVid from Delo d ");
				sql.append(" left outer join DeloArchive da on da.deloId = d.id and da.id != :daId ");
				sql.append(" left outer join Doc p on p.id = da.protocolId ");
				sql.append(" left outer join Doc s on s.id = p.workOffId ");
				sql.append(" where d.id = :deloId order by s.docDate desc ");

				Query query = createQuery(sql.toString()).setParameter("deloId", entity.getDeloId()).setParameter("daId", id);

				row = (Object[]) query.getResultList().get(0);

			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на протоколи, в които текущото дело участва.", e);
			}

			Delo delo = (Delo) row[0];

			Integer newDeloStatus;
			if (row[1] == null) { // няма друг официален протокол !датата не се пипа!

				delo.setStatus(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ACTIVE);

				newDeloStatus = OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED;

			} else { // има и това означава че датата се взима от датата на официалния протокол
				delo.setStatusDate((Date) row[1]);
				delo.setStatus(meta.get(SearchUtils.asInteger(row[2]))); // статуса се изчислява от вида на работния

				newDeloStatus = delo.getStatus();
			}

			String prodVid = sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, protocol.getDocVid(), getUserLang(), null);

			String statusMsg = "Статусът на номенклатурно дело " + delo.getIdentInfo() + " е променен на \"" //
				+ sd.decodeItem(OmbConstants.CODE_CLASSIF_DELO_STATUS, delo.getStatus(), getUserLang(), null) + "\", след изтриването му";

			// оправям всички вложени преписки/документи и е страшно
			restoreDeloContent(delo, newDeloStatus, new HashSet<>(), prodVid, protIdent.toString(), statusMsg, sd);
		}
	}

	/**
	 * Премахва документ от протокола за унищожаване
	 *
	 * @param protocol
	 * @param id       ИД на връзката !!!
	 * @param valid    валидност на докуменат във връзката
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public void removeIncludedDoc(Doc protocol, Integer id, Integer valid) throws DbErrorException, ObjectInUseException {
		DocDestructDAO destructDao = new DocDestructDAO(getUser());

		DocDestruct entity = destructDao.findById(id);
		destructDao.delete(entity);

		if (Objects.equals(valid, OmbConstants.CODE_CLASSIF_DOC_VALID_DESTRUCT)) {
			// щом документа е унищожен трябва да се смени на действащ

			Object[] row;
			try {
				StringBuilder sql = new StringBuilder();
				sql.append(" select d, s.docDate from Doc d ");
				sql.append(" left outer join DocDestruct dd on dd.docId = d.id and dd.id != :ddId ");
				sql.append(" left outer join Doc p on p.id = dd.protocolId ");
				sql.append(" left outer join Doc s on s.id = p.workOffId ");
				sql.append(" where d.id = :docId order by s.docDate desc ");

				Query query = createQuery(sql.toString()).setParameter("docId", entity.getDocId()).setParameter("ddId", id);

				row = (Object[]) query.getResultList().get(0);

			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на протоколи, в които текущият документ участва.", e);
			}

			StringBuilder ident = new StringBuilder();

			Doc doc = (Doc) row[0];

			if (row[1] == null) { // няма друг протокол
				doc.setValid(OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL);
				doc.setValidDate(doc.getDocDate());

				ident.append("Валидността на документ " + doc.getIdentInfo() + " е променена на \"активен\" след като");

			} else { // има и това означава че датата се взима от датата на официалния протокол
				doc.setValidDate((Date) row[1]);

				ident.append("Документ " + doc.getIdentInfo());
			}

			ident.append(" е изтрит от протокол за унищожаване ");
			if (protocol.getWorkOffData() != null && protocol.getWorkOffData().length > 2) {
				ident.append(protocol.getWorkOffData()[1] + "/");
				ident.append(DateUtils.printDate((Date) protocol.getWorkOffData()[2]));
			}
			ident.append(".");

			saveSysOkaJournal(doc, ident.toString());
		}
	}

	/**
	 * Актуализира данни за включение документ в архивирането
	 *
	 * @param id        ИД на връзката !!!
	 * @param ekzNomera
	 * @throws DbErrorException
	 */
	public void updateIncludedDoc(Integer id, String ekzNomera) throws DbErrorException {
		DocDestructDAO destructDao = new DocDestructDAO(getUser());

		DocDestruct entity = destructDao.findById(id);

		entity.setEkzNomera(ekzNomera);

		destructDao.save(entity);
	}

	/** */
	@SuppressWarnings("unchecked")
	@Override
	protected void remove(Doc protocol) throws DbErrorException, ObjectInUseException {

		if (this.ptotocolType == 1) { // ПРОТОКОЛ ЗА УНИЩОЖАВАНЕ

			try {
				StringBuilder sql = new StringBuilder();
				sql.append(" select dd, d, s.docDate from Doc d ");
				sql.append(" inner join DocDestruct dd on dd.docId = d.id "); // реда който ще се трие
				sql.append(" left outer join DocDestruct dds on dds.docId = d.id and dds.id != dd.id "); // други редове ако
																											// документа участва в
																											// протоколи
				sql.append(" left outer join Doc p on p.id = dds.protocolId "); // протокола (работен)
				sql.append(" left outer join Doc s on s.id = p.workOffId "); // собствения протоол (официален)
				sql.append(" where dd.protocolId = :protId order by s.docDate desc ");

				Query query = createQuery(sql.toString()).setParameter("protId", protocol.getId());
				List<Object[]> rows = query.getResultList();

				Set<Integer> deletedIds = new HashSet<>();
				if (!rows.isEmpty()) {
					DocDestructDAO destructDao = new DocDestructDAO(getUser());

					for (Object[] row : rows) {
						DocDestruct entity = (DocDestruct) row[0];

						if (!deletedIds.add(entity.getId())) {
							continue; // вече е оправен този
						}

						Doc doc = (Doc) row[1];

						if (Objects.equals(doc.getValid(), OmbConstants.CODE_CLASSIF_DOC_VALID_DESTRUCT)) {

							if (row[2] == null) { // няма друг протокол
								doc.setValid(OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL);
								doc.setValidDate(doc.getDocDate());

							} else { // има и това означава че датата се взима от датата на официалния протокол
								doc.setValidDate((Date) row[2]);
							}

							saveSysOkaJournal(doc, "изтрит протокол за унищожаване");
						}

						destructDao.delete(entity); // това е връзката
					}
				}

			} catch (Exception e) {
				throw new DbErrorException("Грешка при изтриване връзка между протокол за унищожаване и документи!", e);
			}
		}

		if (this.ptotocolType == 2) { // ПРОТКОЛ ЗА АРХИВИРАНЕ
			List<DeloArchive> list;
			try {
				Query query = createQuery("select da from DeloArchive da where da.protocolId = :protId") //
					.setParameter("protId", protocol.getId());

				list = query.getResultList();

			} catch (Exception e) {
				throw new DbErrorException("Грешка при изтриване връзка между протокол за архивиране и дела!", e);
			}
			if (!list.isEmpty()) {
				DeloArchiveDAO archiveDao = new DeloArchiveDAO(getUser());
				for (DeloArchive da : list) {
					archiveDao.delete(da);
				}
			}
		}

		super.remove(protocol);
	}

	/**
	 * Рейурсивно архивира всичко за номенклатурното дело
	 *
	 * @param parent
	 * @param archDeloIds
	 * @param deloDao
	 * @param docDao
	 * @param protIdent
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	void archiveDeloContent(Delo parent, Set<Integer> archDeloIds, String prodVId, String protIdent) throws DbErrorException {
		if (!archDeloIds.add(parent.getId())) {
			return; // за да не се закили
		}
		DeloDAO deloDao = new DeloDAO(getUser());
		DocDAO docDao = new DocDAO(getUser());

		StringBuilder deloIdent = new StringBuilder();
		if (archDeloIds.size() == 1) {
			deloIdent.append("Номенклатурно дело " + parent.getIdentInfo() + " е архивирано с ");
		} else {
			deloIdent.append("Преписка " + parent.getIdentInfo() + " е архивирана с ");
		}
		deloIdent.append(prodVId + " " + protIdent + ".");

		deloDao.saveSysOkaJournal(parent, deloIdent.toString());

		List<Doc> docs; // и после ако има документи в него
		try {
			Query query = createQuery( //
				"select d from Doc d inner join DeloDoc dd on dd.inputDocId = d.id" //
					+ " where dd.deloId = ?1 and (d.valid is null or d.valid = ?2)") //
						.setParameter(1, parent.getId()).setParameter(2, OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL);
			docs = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на вложени документи!", e);
		}
		for (Doc doc : docs) {
			doc.setValid(OmbConstants.CODE_CLASSIF_DOC_VALID_ARCH);
			doc.setValidDate(parent.getStatusDate());

			String docInfo = SearchUtils.trimToNULL(doc.getDocInfo());
			if (docInfo == null) {
				doc.setDocInfo("Протокол за архивиране " + protIdent + ".");
			} else {
				doc.setDocInfo(docInfo + ". Протокол за архивиране " + protIdent + ".");
			}

			StringBuilder docIdent = new StringBuilder();

			docIdent.append("Документ " + doc.getIdentInfo() + " е архивиран, като вложен в ");
			if (archDeloIds.size() == 1) {
				docIdent.append("архивирано ном.дело ");
			} else {
				docIdent.append("архивирана преписка ");
			}
			docIdent.append(parent.getIdentInfo() + " с " + prodVId + " " + protIdent + ".");

			docDao.saveSysOkaJournal(doc, docIdent.toString());
		}

		List<Delo> content;
		try {
			Query query = createQuery( //
				"select d from Delo d inner join DeloDelo dd on dd.inputDeloId = d.id where dd.deloId = ?1") //
					.setParameter(1, parent.getId());
			content = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на вложени преписки!", e);
		}

		for (Delo delo : content) {
			delo.setStatus(parent.getStatus());
			delo.setStatusDate(parent.getStatusDate());

			String deloInfo = SearchUtils.trimToNULL(delo.getDeloInfo());
			if (deloInfo == null) {
				delo.setDeloInfo("Протокол за архивиране " + protIdent + ".");
			} else {
				delo.setDeloInfo(deloInfo + ". Протокол за архивиране " + protIdent + ".");
			}

			archiveDeloContent(delo, archDeloIds, prodVId, protIdent);
		}
	}

	/**
	 * Развързване на протокола от официален документ. Оправяне на статусите на делата
	 *
	 * @param protocol
	 * @param meta
	 * @param oficialInfo
	 * @param sd
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	void disconnectFromOfficialIncludedDelo(Doc protocol, Map<Integer, Integer> meta, String oficialInfo, BaseSystemData sd) throws DbErrorException {

		List<Object[]> daDelos; // [0]-DELO_ARCHIVE.ID; [1]-DELO_ID
		try { // намирам всички дела, маркирани за архивирани в протокола

			daDelos = createNativeQuery("select da.ID, d.DELO_ID from DELO_ARCHIVE da" //
				+ " inner join DELO d on d.DELO_ID = da.DELO_ID where da.PROTOCOL_ID = ?1 and d.STATUS = ?2") //
					.setParameter(1, protocol.getId()).setParameter(2, meta.get(protocol.getDocVid())) //
					.getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на включени дела в протокола.", e);
		}

		for (Object[] dаDelo : daDelos) {

			int id = ((Number) dаDelo[0]).intValue();
			int deloId = ((Number) dаDelo[1]).intValue();

			Object[] row;
			try {
				StringBuilder sql = new StringBuilder();
				sql.append(" select d, s.docDate, p.docVid from Delo d ");
				sql.append(" left outer join DeloArchive da on da.deloId = d.id and da.id != :daId ");
				sql.append(" left outer join Doc p on p.id = da.protocolId ");
				sql.append(" left outer join Doc s on s.id = p.workOffId ");
				sql.append(" where d.id = :deloId order by s.docDate desc ");

				Query query = createQuery(sql.toString()).setParameter("deloId", deloId).setParameter("daId", id);

				row = (Object[]) query.getResultList().get(0);

			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на протоколи, в които текущото дело участва.", e);
			}

			Delo delo = (Delo) row[0];

			Integer newDeloStatus;
			if (row[1] == null) { // няма друг официален протокол !датата не се пипа!

				delo.setStatus(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ACTIVE);

				newDeloStatus = OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED;

			} else { // има и това означава че датата се взима от датата на официалния протокол
				delo.setStatusDate((Date) row[1]);
				delo.setStatus(meta.get(SearchUtils.asInteger(row[2]))); // статуса се изчислява от вида на работния

				newDeloStatus = delo.getStatus();
			}

			String statusMsg = "Статусът на номенклатурно дело " + delo.getIdentInfo() + " е променен на \"" //
				+ sd.decodeItem(OmbConstants.CODE_CLASSIF_DELO_STATUS, delo.getStatus(), getUserLang(), null) + "\", след като";

			String protIdent = " е премахната връзката на документ " + oficialInfo + " с работен " //
				+ sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, protocol.getDocVid(), getUserLang(), null) + " " + protocol.getIdentInfo() + ".";

			// оправям всички вложени преписки/документи и е страшно
			restoreDeloContentDisconect(delo, newDeloStatus, new HashSet<>(), protIdent, statusMsg, sd);
		}
	}

	/**
	 * Развързване на протокола от официален документ. Оправяне на валидността на документите
	 *
	 * @param protocol
	 * @param oficialInfo
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	void disconnectFromOfficialIncludedDoc(Doc protocol, String oficialInfo, BaseSystemData sd) throws DbErrorException {

		List<Object[]> ddDocs; // [0]-DOC_DESTRUCT.ID; [1]-DOC_ID
		try { // намирам всички документи, маркирани за унищожени в протокола

			ddDocs = createNativeQuery("select dd.ID, d.DOC_ID from DOC_DESTRUCT dd" //
				+ " inner join DOC d on d.DOC_ID = dd.DOC_ID where dd.PROTOCOL_ID = ?1 and d.VALID = ?2") //
					.setParameter(1, protocol.getId()).setParameter(2, OmbConstants.CODE_CLASSIF_DOC_VALID_DESTRUCT) //
					.getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на включени документи в протокола.", e);
		}

		for (Object[] ddDoc : ddDocs) {

			int id = ((Number) ddDoc[0]).intValue();
			int docId = ((Number) ddDoc[1]).intValue();

			Doc doc = null;
			try {
				StringBuilder sql = new StringBuilder();
				sql.append(" select d from Doc d ");
				sql.append(" left outer join DocDestruct dd on dd.docId = d.id and dd.id != :ddId ");
				sql.append(" left outer join Doc p on p.id = dd.protocolId ");
				sql.append(" left outer join Doc s on s.id = p.workOffId ");
				sql.append(" where d.id = :docId and s.docDate is null order by s.docDate desc ");

				List<Doc> docs = createQuery(sql.toString()).setParameter("docId", docId).setParameter("ddId", id).getResultList();

				if (!docs.isEmpty()) {
					doc = docs.get(0);
				}

			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на протоколи, в които текущият документ участва.", e);
			}
			if (doc == null) {
				continue; // няма да се пипа този, защото е унищожен и го има в друг протокол
			}

			doc.setValid(OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL);
			doc.setValidDate(doc.getDocDate());

			String prodVid = sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, protocol.getDocVid(), getUserLang(), null);

			StringBuilder ident = new StringBuilder();
			ident.append("Валидността на документ " + doc.getIdentInfo() + " е променена на \"активен\" след като");
			ident.append(" е премахната връзката на документ " + oficialInfo);
			ident.append(" с работен " + prodVid + " " + protocol.getIdentInfo() + ".");

			saveSysOkaJournal(doc, ident.toString());
		}
	}

	/**
	 * Рекурсивно оправя всичко за номенклатурното дело при махането му от протокол
	 *
	 * @param parent
	 * @param newDeloStatus
	 * @param archDeloIds
	 * @param deloDao
	 * @param docDao
	 * @param protIdent
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	void restoreDeloContent(Delo parent, Integer newDeloStatus, Set<Integer> archDeloIds, String prodVid, String protIdent, String statusMsg, BaseSystemData sd) throws DbErrorException {
		if (!archDeloIds.add(parent.getId())) {
			return; // за да не се закили
		}
		DeloDAO deloDao = new DeloDAO(getUser());
		DocDAO docDao = new DocDAO(getUser());

		StringBuilder identDelo = new StringBuilder();
		if (statusMsg != null) {
			identDelo.append(statusMsg);
		} else {
			identDelo.append("Преписка " + parent.getIdentInfo() + " е изтрита");
		}
		identDelo.append(" от " + prodVid + " " + protIdent + ".");

		deloDao.saveSysOkaJournal(parent, identDelo.toString());

		List<Doc> docs; // и после ако има документи в него
		try {
			Query query = createQuery( //
				"select d from Doc d inner join DeloDoc dd on dd.inputDocId = d.id" //
					+ " where dd.deloId = ?1 and d.valid = ?2") //
						.setParameter(1, parent.getId()).setParameter(2, OmbConstants.CODE_CLASSIF_DOC_VALID_ARCH);
			docs = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на вложени документи!", e);
		}
		for (Doc doc : docs) {

			if (newDeloStatus.equals(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED)) {
				// зачиства се информацията от архивирането

				doc.setValid(OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL);
				doc.setValidDate(doc.getDocDate());

				StringBuilder ident = new StringBuilder();
				ident.append("Валидността на документ " + doc.getIdentInfo() + " е променена на \"активен\" след като");
				if (archDeloIds.size() == 1) {
					ident.append(" съдържащото го ном.дело " + parent.getIdentInfo() + " е изтрито");
				} else {
					ident.append(" съдържаща го преписка " + parent.getIdentInfo() + " е изтрита");
				}
				ident.append(" от " + prodVid + " " + protIdent + ".");

				docDao.saveSysOkaJournal(doc, ident.toString());

			} else { // трябва да се асоциират с другия протокол
				doc.setValidDate(parent.getStatusDate());
			}
		}

		List<Delo> content;
		try {
			Query query = createQuery( //
				"select d from Delo d inner join DeloDelo dd on dd.inputDeloId = d.id where dd.deloId = ?1") //
					.setParameter(1, parent.getId());
			content = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на вложени преписки!", e);
		}

		for (Delo delo : content) {
			delo.setStatus(newDeloStatus);

			if (!newDeloStatus.equals(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED)) {
				// ако е този вид датата на статус не се променя

				delo.setStatusDate(parent.getStatusDate());
			}

			statusMsg = "Статусът на преписка " + delo.getIdentInfo() + " е променен на \"" //
				+ sd.decodeItem(OmbConstants.CODE_CLASSIF_DELO_STATUS, delo.getStatus(), getUserLang(), null) + "\", след изтриването и";

			restoreDeloContent(delo, newDeloStatus, archDeloIds, prodVid, protIdent, statusMsg, sd);
		}
	}

	/**
	 * Рекурсивно оправя всичко за номенклатурното дело при Развързване на протокола от официален документ
	 *
	 * @param parent
	 * @param newDeloStatus
	 * @param archDeloIds
	 * @param deloDao
	 * @param docDao
	 * @param protIdent
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	private void restoreDeloContentDisconect(Delo parent, Integer newDeloStatus, Set<Integer> archDeloIds, String protIdent, String statusMsg, BaseSystemData sd) throws DbErrorException {
		if (!archDeloIds.add(parent.getId())) {
			return; // за да не се закили
		}
		DeloDAO deloDao = new DeloDAO(getUser());
		DocDAO docDao = new DocDAO(getUser());

		String identDelo = statusMsg + protIdent;
		deloDao.saveSysOkaJournal(parent, identDelo);

		List<Doc> docs; // и после ако има документи в него
		try {
			Query query = createQuery( //
				"select d from Doc d inner join DeloDoc dd on dd.inputDocId = d.id" //
					+ " where dd.deloId = ?1 and d.valid = ?2") //
						.setParameter(1, parent.getId()).setParameter(2, OmbConstants.CODE_CLASSIF_DOC_VALID_ARCH);
			docs = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на вложени документи!", e);
		}
		for (Doc doc : docs) {

			if (newDeloStatus.equals(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED)) {
				// зачиства се информацията от архивирането

				doc.setValid(OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL);
				doc.setValidDate(doc.getDocDate());

				String identDoc = "Валидността на документ " + doc.getIdentInfo() + " е променена на \"активен\" след като" + protIdent;
				docDao.saveSysOkaJournal(doc, identDoc);

			} else { // трябва да се асоциират с другия протокол
				doc.setValidDate(parent.getStatusDate());
			}
		}

		List<Delo> content;
		try {
			Query query = createQuery( //
				"select d from Delo d inner join DeloDelo dd on dd.inputDeloId = d.id where dd.deloId = ?1") //
					.setParameter(1, parent.getId());
			content = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на вложени преписки!", e);
		}

		for (Delo delo : content) {
			delo.setStatus(newDeloStatus);

			if (!newDeloStatus.equals(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED)) {
				// ако е този вид датата на статус не се променя

				delo.setStatusDate(parent.getStatusDate());
			}

			statusMsg = "Статусът на преписка " + delo.getIdentInfo() + " е променен на \"" //
				+ sd.decodeItem(OmbConstants.CODE_CLASSIF_DELO_STATUS, delo.getStatus(), getUserLang(), null) + "\", след като";

			restoreDeloContentDisconect(delo, newDeloStatus, archDeloIds, protIdent, statusMsg, sd);
		}
	}
}
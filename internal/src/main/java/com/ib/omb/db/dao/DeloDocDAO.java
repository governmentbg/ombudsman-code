package com.ib.omb.db.dao;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_SECTION_INTERNAL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_SECTION_OFFICIAL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_STATUS_CONTINUED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_DA;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloDelo;
import com.ib.omb.db.dto.DeloDoc;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocDvij;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.utils.DocDostUtils;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.BaseUserData;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link DeloDoc}
 *
 * @author belev
 */
public class DeloDocDAO extends AbstractDAO<DeloDoc> { 

	/**  */
	static final Logger LOGGER = LoggerFactory.getLogger(DeloDocDAO.class);

	/** @param user */
	public DeloDocDAO(ActiveUser user) {
		super(DeloDoc.class, user);
	}

	/**
	 * Документа в кои преписки се намира<br>
	 * Конструира селект за търсене на връзките на документа с преписки за подадения docId, като изтегля само данните от вида:<br>
	 * [0]-DELO_DOC.ID<br>
	 * [1]-INPUT_DATE<br>
	 * [2]-EKZ_NOMER<br>
	 * [3]-TOM_NOMER<br>
	 * [4]-DELO_ID<br>
	 * [5]-INIT_DOC_ID<br>
	 * [6]-RN_DELO<br>
	 * [7]-DELO_DATE<br>
	 * [8]-STATUS<br>
	 * [9]-STATUS_DATE<br>
	 * [10]-DELO_TYPE<br>
	 * [11]-DELO_NAME (String)<br>
	 * [12]-SECTION_TYPE<br>
	 * [13]-WITH_SECTION<br>
	 * [14]-WITH_TOM<br>
	 * [15]-PREV_NOM_DELO<br>
	 *
	 * @param docId
	 * @return
	 */
	public SelectMetadata createSelectDeloListByDoc(Integer docId) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		String select = " select dd.ID a0, dd.INPUT_DATE a1, dd.EKZ_NOMER a2, dd.TOM_NOMER a3, dd.DELO_ID a4" //
			+ ", d.INIT_DOC_ID a5, d.RN_DELO a6, d.DELO_DATE a7, d.STATUS a8, d.STATUS_DATE a9, d.DELO_TYPE a10, " //
			+ DialectConstructor.limitBigString(dialect, "d.DELO_NAME", 300) + " a11, dd.SECTION_TYPE a12, d.WITH_SECTION a13, d.WITH_TOM a14, d.PREV_NOM_DELO a15 ";

		StringBuilder from = new StringBuilder(" from DELO_DOC dd ");
		String where = " where dd.INPUT_DOC_ID = :docId ";

		params.put("docId", docId);

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(*) " + from + where); // тука е готово

		from.append(" inner join DELO d on d.DELO_ID = dd.DELO_ID "); // това не влияе на каунта и е затов е след него

		sm.setSql(select + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * Търсене на документи в преписка<br>
	 * Конструира селект за търсене на документи в преписка за подаденото deloId, като изтегля само данните от вида:<br>
	 * [0]-DELO_DOC.ID<br>
	 * [1]-INPUT_DOC_ID<br>
	 * [2]-RN_DOC<br>
	 * [3]-DOC_DATE<br>
	 * [4]-DOC_VID<br>
	 * [5]-DOC_TYPE<br>
	 * [6]-INPUT_DATE<br>
	 * [7]-EKZ_NOMER<br>
	 * [8]-TOM_NOMER<br>
	 * [9]-SECTION_TYPE<br>
	 * [10]-OTNOSNO (String)<br>
	 *
	 * @param deloId
	 * @return
	 */
	public SelectMetadata createSelectDocListInDelo(Integer deloId) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		select.append(" select dd.ID a0, d.DOC_ID a1, "+DocDAO.formRnDocSelect("d.", dialect)+" a2, d.DOC_DATE a3, d.DOC_VID a4, d.DOC_TYPE a5 ");
		select.append(" , dd.INPUT_DATE a6, dd.EKZ_NOMER a7, dd.TOM_NOMER a8, dd.SECTION_TYPE a9, ");
		select.append(DialectConstructor.limitBigString(dialect, "d.OTNOSNO", 300) + " a10 "); // max 300!

		StringBuilder from = new StringBuilder(" from DELO_DOC dd ");

		String where = " where dd.DELO_ID = :deloId ";
		params.put("deloId", deloId);

		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(*) " + from + where); // тука е готово

		from.append(" inner join DOC d on d.DOC_ID = dd.INPUT_DOC_ID "); // това не влияе на каунта и е затов е след него

		sm.setSql(select.toString() + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * Изтрива влагания на документ и преписка в <b>контекста на документ</b>.
	 *
	 * @param deloDoc
	 * @param deleteDeloDoc  при <code>true</code> трие съответната връзка
	 * @param deleteDeloDelo при <code>true</code> трие съответната връзка
	 * @param docEdit 
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public DeloDoc deleteInDoc(DeloDoc deloDoc, boolean deleteDeloDoc, boolean deleteDeloDelo, Doc docEdit) throws DbErrorException, ObjectInUseException {
		if (deleteDeloDelo && deloDoc.getDeloDelo() != null && deloDoc.getDeloDelo().getId() != null) {
			// изтрива се връзката на преписката преписката

			if (Objects.equals(deloDoc.getDelo().getStatus(), CODE_ZNACHENIE_DELO_STATUS_COMPLETED)
					&& Objects.equals(deloDoc.getDeloDelo().getDelo().getDeloType(), CODE_ZNACHENIE_DELO_TYPE_NOM)) {
				// тази която се изважа е била приключена и трябва да се смени на продължена
				Delo delo = getEntityManager().find(Delo.class, deloDoc.getDeloId()); // за да се вземе само обекта, а не всичко
																						// натворено във финдБъИд
				delo.setStatus(CODE_ZNACHENIE_DELO_STATUS_CONTINUED);
				delo.setStatusDate(new Date());

				StringBuilder sb = new StringBuilder();
				sb.append(deloDoc.getDeloDelo().getDelo().getRnDelo()+"/");
				sb.append(DateUtils.printDate(deloDoc.getDeloDelo().getDelo().getDeloDate()));
				if (deloDoc.getDeloDelo().getTomNomer() != null) {
					sb.append(",т." + deloDoc.getDeloDelo().getTomNomer());
				}
				sb.append(" на " + DateUtils.printDate(new Date()));

				delo.setPrevNomDelo(sb.toString());

				DeloDAO deloDao = new DeloDAO(getUser());
				deloDao.saveSysOkaJournal(delo, "Преписка "+delo.getIdentInfo()+" е продължена, при изваждането й от номенклатурно дело "+deloDoc.getDeloDelo().getDelo().getIdentInfo()+".");

				deloDoc.getDelo().setStatus(CODE_ZNACHENIE_DELO_STATUS_CONTINUED); // да се види и на екрана
				deloDoc.getDelo().setPrevNomDelo(sb.toString());
			}

			new DeloDeloDAO(getUser()).delete(deloDoc.getDeloDelo());
			
			deloDoc.setDeloDelo(null);

		} else if (deleteDeloDoc && deloDoc != null && deloDoc.getId() != null) { // маха се преписката на документа

			if (Objects.equals(deloDoc.getDelo().getInitDocId(), deloDoc.getInputDocId())) {
				// маха се иницииеащия документ от преписката, така че той не може вече да е инцииращ
				DeloDAO deloDao = new DeloDAO(getUser());

				Delo delo = getEntityManager().find(Delo.class, deloDoc.getDeloId());
				delo.setInitDocId(null);
				deloDao.saveSysOkaJournal(delo, "Иницииращият документ "+docEdit.getIdentInfo()+" е изваден от преписка "+delo.getIdentInfo()+".");
			}

			delete(deloDoc);

			return null; // изтрива се и няма как да има нещо
		}
		
		
		
		
		return deloDoc;
	}
	
	/**
	 * Влагане на списък документи в <b>контекста на преписка</b>. В обекта deloDoc се задава екземпляр 1
	 *
	 * @param seleted  {@link DocSearch#buildQueryComp(BaseUserData)}
	 * @param delo
	 * @param tomNomer
	 * @param sectionType
	 * @param inputDate
	 * @param ekzNomer 
	 * @return новозаписаните елементи
	 * @throws DbErrorException
	 */
	public List<DeloDoc> saveInDeloRow(List<Object[]> seleted, Delo delo, Integer tomNomer, Integer sectionType, Date inputDate, Integer ekzNomer) throws DbErrorException {
		List<DeloDoc> list = new ArrayList<>();
		DocDAO docDao = null;
		for (Object[] row : seleted) {
			Integer docId = SearchUtils.asInteger(row[0]);
			
			DeloDoc deloDoc = new DeloDoc(delo.getId(), docId, tomNomer, sectionType, inputDate);
			if (ekzNomer != null) {
				deloDoc.setEkzNomer(ekzNomer);
			} else {
				deloDoc.setEkzNomer(1);
			}
			if (deloDoc.getTomNomer() == null) {
				deloDoc.setTomNomer(1);
			}
			save(deloDoc); // запис
			list.add(deloDoc);

			if (Objects.equals(SearchUtils.asInteger(row[2]), CODE_ZNACHENIE_DOC_TYPE_IN) // входящ
				&& !Objects.equals(SearchUtils.asInteger(row[9]), CODE_ZNACHENIE_DA)) { // необработен
				
				if (docDao == null) {
					docDao = new DocDAO(getUser());
				}
				Doc doc = getEntityManager().find(Doc.class, docId);
				doc.setProcessed(CODE_ZNACHENIE_DA);
				
				docDao.saveSysOkaJournal(doc, "Документ "+doc.getIdentInfo()+" е маркиран за обработен, при влагането му в преписка "+delo.getIdentInfo()+".");
			}
		}
		return list;
	}

	/**
	 * Запис на връзка в <b>контекста на документ</b>. Ако при нов запис в обекта deloDoc няма зададен екземпляр се слага 1, иначе
	 * конкретния номер.
	 *
	 * @param deloDoc
	 * @param doc
	 * @param completeDocDelo  <code>true</code> ще направи преписката на документа приключена
	 * @param completeDeloDelo <code>true</code> ще направи преписката на преписката приключена
	 * @param closeTasks при <code>true</code> се затварят всички задачи за документите вложени в преписка
	 * @return [0]-DeloDoc, [1]-DeloDelo. Има нещо само ако са новозаписани
	 * @throws DbErrorException
	 */
	public Object[] saveInDoc(DeloDoc deloDoc, Doc doc, boolean completeDocDelo, boolean completeDeloDelo, boolean closeTasks) throws DbErrorException {
		Object[] newEntries = null;
		DeloDelo deloDelo = deloDoc.getDeloDelo(); // преписката на преписката

		boolean inDoc = Objects.equals(doc.getDocType(), CODE_ZNACHENIE_DOC_TYPE_IN);
		
		if (deloDoc.getId() == null) {
			deloDoc.setInputDocId(doc.getId());
			if (deloDoc.getEkzNomer() == null) {
				deloDoc.setEkzNomer(1);
			}
			if (deloDoc.getSectionType() == null) {
				if (inDoc || doc.getDocType().equals(CODE_ZNACHENIE_DOC_TYPE_OWN)) {
					deloDoc.setSectionType(CODE_ZNACHENIE_DELO_SECTION_OFFICIAL);
				} else {
					deloDoc.setSectionType(CODE_ZNACHENIE_DELO_SECTION_INTERNAL);
				}
			}
			if (deloDoc.getInputDate() == null) { // дори и при нов може да е въведно и не трябва да се замазва
				deloDoc.setInputDate(new Date());
			}
			if (deloDoc.getTomNomer() == null) {
				deloDoc.setTomNomer(1);
			}

			save(deloDoc); // persist - OK
			newEntries = new Object[2];
			newEntries[0] = deloDoc;
		} else {
			Delo delo = deloDoc.getDelo(); // заради JPA merge !
			deloDoc = save(deloDoc);
			deloDoc.setDelo(delo);
		}

		if (completeDocDelo) { // трябва да се приключи преписката на документа
			DeloDAO deloDao = new DeloDAO(getUser());

			Delo delo = getEntityManager().find(Delo.class, deloDoc.getDeloId()); // за да се вземе само обекта, а не всичко
																					// натворено във финдБъИд
			delo.setStatus(CODE_ZNACHENIE_DELO_STATUS_COMPLETED);
			delo.setStatusDate(new Date());

			deloDao.completeDelo(delo, closeTasks, null); // приключване на делото
			delo = deloDao.saveSysOkaJournal(delo, "Преписка "+delo.getIdentInfo()+" е приключена по искане на потребителя, при влагане на последен документ."); // и после запис

			deloDoc.setDelo(delo);

//			if (doc.getJalba() != null && doc.getJalba().getId() != null && deloDoc.getDelo() != null && doc.getId().equals(deloDoc.getDelo().getInitDocId())) {
//				// за да са видими на екрана промените и в таба за док
//				doc.getJalba().setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED);
//				doc.getJalba().setDbSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED);
//				doc.getJalba().setSastDate(new Date());
//			}
//			if (doc.getDocSpec() != null && doc.getDocSpec().getId() != null && deloDoc.getDelo() != null && doc.getId().equals(deloDoc.getDelo().getInitDocId())) {
//				// за да са видими на екрана промените и в таба за док
//				doc.getDocSpec().setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED);
//				doc.getDocSpec().setDbSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED);
//				doc.getDocSpec().setSastDate(new Date());
//			}
		}

		if (deloDelo != null && deloDelo.getDeloId() != null) { // има нещо и требва да се записва
			DeloDeloDAO deloDeloDao = new DeloDeloDAO(getUser());

			deloDelo.setAuditable(false);

			if (deloDelo.getId() == null) {
				deloDelo.setInputDeloId(deloDoc.getDeloId());

				if (deloDelo.getInputDate() == null) { // дори и при нов може да е въведно и не трябва да се замазва
					deloDelo.setInputDate(new Date());
				}
				if (deloDelo.getTomNomer() == null) {
					deloDelo.setTomNomer(1);
				}

				deloDeloDao.save(deloDelo); // persist - OK
				if (newEntries == null) {
					newEntries = new Object[2];
				}
				newEntries[1] = deloDelo;

				if (Objects.equals(deloDelo.getDelo().getDeloType(), CODE_ZNACHENIE_DELO_TYPE_NOM)) {
					DeloDAO deloDao = new DeloDAO(getUser());

					Delo delo = getEntityManager().find(Delo.class, deloDoc.getDeloId()); // за да се вземе само обекта, а не всичко
																							// натворено във финдБъИд
					if (!SearchUtils.isEmpty(delo.getPrevNomDelo())) {

						String ident = "При влагане на преписка "+delo.getIdentInfo()+" в номенклатурно дело " 
							+ deloDelo.getDelo().getIdentInfo() + " е нулирано предходното й местоположение: " + delo.getPrevNomDelo() + ".";

						delo.setPrevNomDelo(null); // преписка се влага в номенклатурно дело
						delo = deloDao.saveSysOkaJournal(delo, ident);
						deloDoc.setDelo(delo);
					}
				}

			} else {
				Delo delo = deloDelo.getDelo(); // заради JPA merge !
				deloDelo = deloDeloDao.save(deloDelo);
				deloDelo.setDelo(delo);
			}
			deloDoc.setDeloDelo(deloDelo); // за да имаме на екрана всичко което ни трябва

			if (completeDeloDelo) { // трябва да се приключи преписката на преписката
				DeloDAO deloDao = new DeloDAO(getUser());

				Delo delo = getEntityManager().find(Delo.class, deloDelo.getDeloId()); // за да се вземе само обекта, а не всичко
																						// натворено във финдБъИд
				delo.setStatus(CODE_ZNACHENIE_DELO_STATUS_COMPLETED);
				delo.setStatusDate(new Date());

				deloDao.completeDelo(delo, closeTasks, null); // приключване на делото
				delo = deloDao.saveSysOkaJournal(delo, "Преписка "+delo.getIdentInfo()+" е приключена по искане на потребителя, при влагане на последен документ.");

				deloDelo.setDelo(delo);

//				if (doc.getJalba() != null && doc.getJalba().getId() != null && deloDoc.getDelo() != null && doc.getId().equals(deloDoc.getDelo().getInitDocId())) {
//					// за да са видими на екрана промените и в таба за док
//					doc.getJalba().setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED);
//					doc.getJalba().setDbSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED);
//					doc.getJalba().setSastDate(new Date());
//				}
//				if (doc.getDocSpec() != null && doc.getDocSpec().getId() != null && deloDoc.getDelo() != null && doc.getId().equals(deloDoc.getDelo().getInitDocId())) {
//					// за да са видими на екрана промените и в таба за док
//					doc.getDocSpec().setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED);
//					doc.getDocSpec().setDbSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_COMPLETED);
//					doc.getDocSpec().setSastDate(new Date());
//				}
			}
		}
		
		if (inDoc && !Objects.equals(doc.getProcessed(), CODE_ZNACHENIE_DA) // трябва да стане обработен 
			&& deloDoc.getDelo() != null && deloDoc.getDelo().getUserReg() == null) { // !!! това го добавям за да не се прави при иницииране на преписка при запис на нов док
			
			DocDAO docDao = new DocDAO(getUser());
			
			doc.setProcessed(CODE_ZNACHENIE_DA);
			
			docDao.saveSysOkaJournal(doc, "Документ "+doc.getIdentInfo()+" е маркиран за обработен, при влагането му в преписка "+deloDoc.getDelo().getIdentInfo()+".");
		}
		
		return newEntries;
	}

	/** */
	@Override
	public void delete(DeloDoc entity) throws DbErrorException, ObjectInUseException {
		super.delete(entity);
		
		new DocDostUtils().removeDocDostFromObject(entity.getInputDocId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO, entity.getDeloId());

		try { // ако има движение заради преписката трябва да се нулира връзката FROM_DVIJ_ID
			StringBuilder sql = new StringBuilder();
			sql.append(" update DOC_DVIJ set FROM_DVIJ_ID = null where ID in ( ");
			sql.append(" select distinct docD.ID ");
			sql.append(" from DOC_DVIJ docD ");
			sql.append(" inner join DELO_DVIJ deloD on deloD.DELO_ID = :deloId and (deloD.ID = docD.FROM_DVIJ_ID or deloD.FROM_DVIJ_ID = docD.FROM_DVIJ_ID) ");
			sql.append(" where docD.DOC_ID = :docId and docD.RETURN_DATE is null and docD.FROM_DVIJ_ID is not null ) ");
			
			int cnt = createNativeQuery(sql.toString()) //
				.setParameter("deloId", entity.getDeloId()).setParameter("docId", entity.getInputDocId()) //
				.executeUpdate();
			if (cnt > 0) {
				LOGGER.debug("set FROM_DVIJ_ID=NULL to {} rows DOC_DVIJ for DOC_ID={}", cnt, entity.getInputDocId());
			}
		} catch (Exception e) {
			throw new DbErrorException("Грешка при обработка на движения.", e);
		}
	}

	/** */
	@Override
	public DeloDoc save(DeloDoc entity) throws DbErrorException {
		boolean isNew = entity.getId() == null;

		DeloDoc saved = super.save(entity);

		if (isNew) {
			new DocDostUtils().addDocDostFromDelo(entity.getInputDocId(), entity.getDeloId());
		}
		return saved;
	}
	
	
	/**
	 * Прехвърляне на документи от папка в папка
	 * @param current текущото дело от което се мести
	 * @param selected целия избран ред на екарана
	 * @param deloId в тази преписка ще се местят
	 * @param tomNomer 
	 * @param inputDate 
	 * @return новозаписаните елементи
	 * @throws DbErrorException
	 * @throws ObjectInUseException 
	 */
	public List<DeloDoc> transferToDelo(Delo current, List<Object[]> selected, Integer deloId, Integer tomNomer, Date inputDate) throws DbErrorException, ObjectInUseException {
		List<DeloDoc> list = new ArrayList<>();

		for (Object[] row : selected) {
			DeloDoc ddDel = findById(((Number)row[0]).intValue());
			if (ddDel == null) {
				continue; // няма логика, но за всеки случай да не изгърмим
			}
			delete(ddDel);

			if (Objects.equals(ddDel.getInputDocId(), current.getInitDocId())) {
				// мести се иницииращия документ
				current.setInitDocId(null);
				
				StringBuilder ident = new StringBuilder();
				ident.append("Иницииращият документ ");
				ident.append(row[2] + "/" + DateUtils.printDate((Date) row[3]));
				ident.append(" е изваден от преписка "+current.getIdentInfo()+".");
				
				Delo delo = getEntityManager().find(Delo.class, ddDel.getDeloId());
				delo.setInitDocId(null);
				new DeloDAO(getUser()).saveSysOkaJournal(delo, ident.toString());
			}
			
			DeloDoc ddNew = new DeloDoc(deloId, ddDel.getInputDocId(), tomNomer, ddDel.getSectionType(), inputDate);
			ddNew.setEkzNomer(ddDel.getEkzNomer());

			save(ddNew);
			list.add(ddNew);
		}
		return list;
	}
	
	/**
	 * @param deloDoc
	 * @param systemData 
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public String checkCreateDvij(DeloDoc deloDoc, BaseSystemData systemData) throws DbErrorException {
		List<Object[]> rows;
		try {
			rows = createNativeQuery( //
				"select ID, DVIJ_DATE, CODE_REF, DVIJ_TEXT, FROM_DVIJ_ID from DELO_DVIJ" //
					+ " where DELO_ID = ?1 and (TOM_NOMER = ?2 or TOM_NOMER is null) and DVIJ_METHOD = ?3 and RETURN_DATE is null" //
					+ " order by 2 desc, 1 desc") //
						.setParameter(1, deloDoc.getDeloId()).setParameter(2, deloDoc.getTomNomer()) //
						.setParameter(3, OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA) //
						.setMaxResults(1).getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на движения за преписка.", e);
		}
		if (rows.isEmpty()) {
			return null;
		}
		Object[] row = rows.get(0);

		DocDvij dvij = new DocDvij();

		dvij.setDocId(deloDoc.getInputDocId());
		dvij.setDvijDate(deloDoc.getInputDate());
		dvij.setEkzNomer(deloDoc.getEkzNomer());
		dvij.setDvijMethod(OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA);

		dvij.setCodeRef(SearchUtils.asInteger(row[2]));
		dvij.setDvijText((String) row[3]);
		dvij.setDvijInfo("Регистрирано е автоматично предаване при влагане на документа в предадена на ръка преписка/том.");

		dvij.setStatus(OmbConstants.DS_SENT);
		dvij.setStatusDate(new Date());

		if (row[4] == null) { // основно става намереното движение
			dvij.setFromDvijId(SearchUtils.asInteger(row[0]));

		} else { // предаваме на долу основното движение
			dvij.setFromDvijId(SearchUtils.asInteger(row[4]));
		}

		new DocDvijDAO(getUser()).save(dvij, (SystemData) systemData, null);
		return "Регистрирано е автоматично предаване при влагане на документ в предадена на ръка преписка/том.";
	}
}

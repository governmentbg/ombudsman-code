package com.ib.omb.db.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloDelo;
import com.ib.omb.db.dto.DeloDvij;
import com.ib.omb.search.DeloSearch;
import com.ib.omb.system.OmbConstants;
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
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link DeloDelo}
 *
 * @author belev
 */
public class DeloDeloDAO extends AbstractDAO<DeloDelo> {

	/**  */
	static final Logger LOGGER = LoggerFactory.getLogger(DeloDeloDAO.class);

	/** @param user */
	public DeloDeloDAO(ActiveUser user) {
		super(DeloDelo.class, user);
	}

	/**
	 * Търсене на преписки в преписка<br>
	 * Конструира селект за търсене на преписки в преписка за подаденото deloId, като изтегля само данните от вида:<br>
	 * [0]-DELO_DELO.ID<br>
	 * [1]-INPUT_DELO_ID<br>
	 * [2]-RN_DELO<br>
	 * [3]-DELO_DATE<br>
	 * [4]-STATUS<br>
	 * [5]-TOM_NOMER<br>
	 * [6]-DELO_NAME (String)<br>
	 * [7]-INPUT_DATE<br>
	 *
	 * @param deloId
	 * @return
	 */
	public SelectMetadata createSelectDeloListInDelo(Integer deloId) {
		String dialect = JPA.getUtil().getDbVendorName();

		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		select.append(" select dd.ID a0, d.DELO_ID a1, d.RN_DELO a2, d.DELO_DATE a3, d.STATUS a4, dd.TOM_NOMER a5, ");
		select.append(DialectConstructor.limitBigString(dialect, "d.DELO_NAME", 300) + " a6 "); // max 300!
		select.append(" , dd.INPUT_DATE a7 ");

		String from = " from DELO d inner join DELO_DELO dd on dd.INPUT_DELO_ID = d.DELO_ID ";

		String where = " where dd.DELO_ID = :deloId ";
		params.put("deloId", deloId);

		SelectMetadata sm = new SelectMetadata();

		sm.setSql(select + from + where);
		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/** */
	@Override
	public DeloDelo save(DeloDelo entity) throws DbErrorException {
		if (entity.getTomNomer() == null) {
			entity.setTomNomer(1); // за да не остане празно без значение кой какво прави преди това
		}
		boolean isNew = entity.getId() == null;
		
		DeloDelo saved = super.save(entity);
		
		if (isNew) {
			new DocDostUtils().addDeloDostFromDelo(entity.getInputDeloId(), entity.getDeloId(), new HashSet<>());
		}
		return saved;
	}

	/**
	 * Намира данни за преписката, в която се намира подадената преписка.
	 *
	 * @param inputDeloId
	 * @return или обекта или NULL ако няма
	 * @throws DbErrorException
	 */
	public DeloDelo findByDeloId(Integer inputDeloId) throws DbErrorException {
		try { // търся да заредя коя е преписката

			StringBuilder sql = new StringBuilder();

			sql.append(" select dd, d.initDocId, d.rnDelo, d.deloDate, d.status, d.statusDate, d.deloType, d.deloName,  d.withTom ");
			sql.append(" from Delo d ");
			sql.append(" inner join DeloDelo dd on dd.deloId = d.id ");
			sql.append(" where dd.inputDeloId = :inputDeloId ");
			sql.append(" order by dd.inputDate desc ");

			@SuppressWarnings("unchecked")
			List<Object[]> result = createQuery(sql.toString()).setParameter("inputDeloId", inputDeloId) //
				.getResultList();

			if (result.isEmpty()) {
				return null;
			}
			Object[] row = result.get(0);

			DeloDelo deloDelo = (DeloDelo) row[0];

			Delo delo = new Delo();

			delo.setId(deloDelo.getDeloId());
			delo.setInitDocId((Integer) row[1]);
			delo.setRnDelo((String) row[2]);
			delo.setDeloDate((Date) row[3]);
			delo.setStatus((Integer) row[4]);
			delo.setStatusDate((Date) row[5]);
			delo.setDeloType((Integer) row[6]);
			delo.setDeloName((String) row[7]);
			delo.setWithTom((Integer) row[8]);
			
			deloDelo.setDelo(delo);

			return deloDelo;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на преписка за преписка!", e);
		}
	}

	/**  */
	@Override
	public void delete(DeloDelo entity) throws DbErrorException, ObjectInUseException {
		super.delete(entity);
		
		new DocDostUtils().removeDeloDostFromDelo(entity.getInputDeloId(), entity.getDeloId(), new HashSet<>());

		checkDvij(entity);
	}

	/**
	 * ако има движения заради преписката в която е вложена трябва да се нулира
	 * връзката FROM_DVIJ_ID за движения на вложени преписки и документи
	 * 
	 * @param entity
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	void checkDvij(DeloDelo entity) throws DbErrorException {
		List<Object> fromDvijIds;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select distinct ch.FROM_DVIJ_ID ");
			sql.append(" from DELO_DVIJ ch ");
			sql.append(" inner join DELO_DVIJ par on par.DELO_ID = :deloId and (par.ID = ch.FROM_DVIJ_ID or par.FROM_DVIJ_ID = ch.FROM_DVIJ_ID) ");
			sql.append(" where ch.DELO_ID = :inputDeloId and ch.RETURN_DATE is null and ch.FROM_DVIJ_ID is not null ");
			
			fromDvijIds = createNativeQuery(sql.toString()) //
				.setParameter("deloId", entity.getDeloId()).setParameter("inputDeloId", entity.getInputDeloId()) //
				.getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при обработка на движения.", e);
		}
		if (fromDvijIds.isEmpty()) {
			return;
		}
		
		try {
			Set<Integer> deloDvijIds = new HashSet<>(); // тези ще се нулират

			for (Object fromDvijId : fromDvijIds) { // трява за всички вложени да се нулира връзката с движението
				Set<Integer> deloIds = new HashSet<>(); // за да не зациклим
				
				// вътре ще се оправят документите за всяка преписка
				recursiveNullFromDvijId(entity.getInputDeloId(), ((Number)fromDvijId).intValue(), deloDvijIds, deloIds);
			}
			
			if (!deloDvijIds.isEmpty()) {
				int cnt = createNativeQuery("update DELO_DVIJ set FROM_DVIJ_ID = null where ID in (:deloDvijIds)")
					.setParameter("deloDvijIds", deloDvijIds).executeUpdate();
				if (cnt > 0) {
					LOGGER.debug("set recursive FROM_DVIJ_ID=NULL to {} rows DELO_DVIJ for DELO_ID={}", cnt, entity.getInputDeloId());
				}
			}
			
			// накрая и това което се маха да се нулира
			int cnt = createNativeQuery("update DELO_DVIJ set FROM_DVIJ_ID = null where DELO_ID = :deloId and FROM_DVIJ_ID in (:fromDvijIds)")
				.setParameter("deloId", entity.getInputDeloId()).setParameter("fromDvijIds", fromDvijIds)
				.executeUpdate();
			if (cnt > 0) {
				LOGGER.debug("set FROM_DVIJ_ID=NULL to {} rows DELO_DVIJ for DELO_ID={} and FROM_DVIJ_ID in ({})"
					, cnt, entity.getInputDeloId(), fromDvijIds);
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при обработка на движения.", e);
		}
	}

	/**
	 * @param deloId
	 * @param fromDvijId
	 * @param deloDvijIds
	 * @param deloIds
	 */
	private void recursiveNullFromDvijId(Integer deloId, int fromDvijId, Set<Integer> deloDvijIds, Set<Integer> deloIds) {
		deloIds.add(deloId);
		
		StringBuilder sql = new StringBuilder();
		sql.append(" select distinct dvij.ID, dvij.DELO_ID ");
		sql.append(" from DELO_DELO dd ");
		sql.append(" inner join DELO_DVIJ dvij on dvij.DELO_ID = dd.INPUT_DELO_ID ");
		sql.append(" where dd.DELO_ID = :deloId and dvij.FROM_DVIJ_ID = :fromDvijId and dvij.RETURN_DATE is null ");
		
		@SuppressWarnings("unchecked")
		List<Object[]> rows = createNativeQuery(sql.toString())
			.setParameter("deloId", deloId).setParameter("fromDvijId", fromDvijId)
			.getResultList();
		
		for (Object[] row : rows) {
			Integer inputDeloId = ((Number)row[1]).intValue();
			if (deloIds.contains(inputDeloId)) {
				continue;
			}
			deloDvijIds.add(((Number)row[0]).intValue()); // трупаме за накрая
			
			recursiveNullFromDvijId(inputDeloId, fromDvijId, deloDvijIds, deloIds);
		}
		
		StringBuilder update = new StringBuilder(); // а за документите директно може да се пусне упдате
		update.append(" update DOC_DVIJ set FROM_DVIJ_ID = null where ID in ( ");
		update.append(" select distinct dvij.ID ");
		update.append(" from DELO_DOC dd ");
		update.append(" inner join DOC_DVIJ dvij on dvij.DOC_ID = dd.INPUT_DOC_ID ");
		update.append(" where dd.DELO_ID = :deloId and dvij.FROM_DVIJ_ID = :fromDvijId and dvij.RETURN_DATE is null) ");
		
		int cnt = createNativeQuery(update.toString())
			.setParameter("deloId", deloId).setParameter("fromDvijId", fromDvijId)
			.executeUpdate();
		if (cnt > 0) {
			LOGGER.debug("set FROM_DVIJ_ID=NULL to {} rows DOC_DVIJ for DELO_ID={} and FROM_DVIJ_ID={}", cnt, deloId, fromDvijId);
		}
	}

	/**
	 * Влагане на списък преписки в на преписка.
	 *
	 * @param selected  списък избрани елементи като резултат от {@link DeloSearch#buildQueryComp(BaseUserData)}
	 * @param delo
	 * @param tomNomer
	 * @param inputDate
	 * @return 
	 * @throws DbErrorException
	 */
	public List<DeloDelo> save(List<Object[]> selected, Delo delo, Integer tomNomer, Date inputDate) throws DbErrorException {
		List<DeloDelo> list = new ArrayList<>();
		for (Object[] row : selected) {
			DeloDelo deloDelo = new DeloDelo(delo.getId(), SearchUtils.asInteger(row[0]), tomNomer, inputDate);

			save(deloDelo); // запис
			list.add(deloDelo);

			if (Objects.equals(delo.getDeloType(), OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM)) {
				DeloDAO deloDao = new DeloDAO(getUser());

				Delo child = getEntityManager().find(Delo.class, deloDelo.getInputDeloId()); // за да се вземе само обекта, а не всичко
																						// натворено във финдБъИд
				if (!SearchUtils.isEmpty(child.getPrevNomDelo())) {

					String ident = "При влагане на преписка "+child.getIdentInfo()+" в номенклатурно дело " 
						+ delo.getIdentInfo() + " е нулирано предходното й местоположение: " + child.getPrevNomDelo() + ".";

					child.setPrevNomDelo(null); // преписка се влага в номенклатурно дело

					deloDao.saveSysOkaJournal(child, ident);
				}
			}
		}
		return list;
	}

	/**
	 * Прехвърляне на преписки от папка в папка
	 * 
	 * @param selected целия избран ред на екарана
	 * @param deloId в тази преписка ще се местят
	 * @param tomNomer 
	 * @param inputDate 
	 * @return новозаписаните
	 * @throws DbErrorException
	 * @throws ObjectInUseException 
	 */
	public List<DeloDelo> transferToDelo(List<Object[]> selected, Integer deloId, Integer tomNomer, Date inputDate) throws DbErrorException, ObjectInUseException {
		List<DeloDelo> list = new ArrayList<>();
		for (Object[] row : selected) {
			DeloDelo ddDel = findById(((Number)row[0]).intValue());
			if (ddDel == null) {
				continue; // няма логика, но за всеки случай да не изгърмим
			}
			delete(ddDel);

			DeloDelo ddNew = new DeloDelo(deloId, ddDel.getInputDeloId(), tomNomer, inputDate);

			save(ddNew);
			
			list.add(ddNew);
		}
		return list;
	}

	/**
	 * @param deloDelo
	 * @param systemData 
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public String checkCreateDvij(DeloDelo deloDelo, BaseSystemData systemData) throws DbErrorException {
		List<Object[]> rows;
		try {
			rows = createNativeQuery( //
				"select ID, DVIJ_DATE, CODE_REF, DVIJ_TEXT, FROM_DVIJ_ID from DELO_DVIJ" //
					+ " where DELO_ID = ?1 and (TOM_NOMER = ?2 or TOM_NOMER is null) and DVIJ_METHOD = ?3 and RETURN_DATE is null" //
					+ " order by 2 desc, 1 desc") //
						.setParameter(1, deloDelo.getDeloId()).setParameter(2, deloDelo.getTomNomer()) //
						.setParameter(3, OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA) //
						.setMaxResults(1).getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на движения за преписка.", e);
		}
		if (rows.isEmpty()) {
			return null;
		}
		Object[] row = rows.get(0);

		DeloDvij dvij = new DeloDvij();

		dvij.setDeloId(deloDelo.getInputDeloId());
		dvij.setDvijDate(deloDelo.getInputDate());
		dvij.setTomNomer(null); // движи се цялата, която се влага !!!
		dvij.setDvijMethod(OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA);

		dvij.setCodeRef(SearchUtils.asInteger(row[2]));
		dvij.setDvijText((String) row[3]);
		dvij.setDvijInfo("Регистрирано е автоматично предаване при влагане на преписката в предадена на ръка преписка/том.");

		dvij.setStatus(OmbConstants.DS_SENT);
		dvij.setStatusDate(new Date());

		if (row[4] == null) { // основно става намереното движение
			dvij.setFromDvijId(SearchUtils.asInteger(row[0]));

		} else { // предаваме на долу основното движение
			dvij.setFromDvijId(SearchUtils.asInteger(row[4]));
		}

		new DeloDvijDAO(getUser()).save(dvij, systemData);
		return "Регистрирано е автоматично предаване при влагане на преписка в предадена на ръка преписка/том.";
	}
}
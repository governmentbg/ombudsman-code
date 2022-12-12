package com.ib.omb.db.dao;

import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import com.ib.omb.db.dto.DeloStorage;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link DeloStorage}
 *
 * @author belev
 */
public class DeloStorageDAO extends AbstractDAO<DeloStorage> {

	/** @param user */
	public DeloStorageDAO(ActiveUser user) {
		super(DeloStorage.class, user);
	}

	/**
	 * Намира данни за съхранение на подадените преписки, като изтегля само данните от вида:<br>
	 * [0]-DELO_ID<br>
	 * [1]-RN_DELO<br>
	 * [2]-DELO_DATE<br>
	 * [3]-DELO_TYPE<br>
	 * [4]-TOM_NOMER<br>
	 * [5]-DELO_STORAGE.ID<br>
	 * [6]-ARCH_NOMER<br>
	 * [7]-ROOM<br>
	 * [8]-SHKAF<br>
	 * [9]-STILLAGE<br>
	 * [10]-BOX<br>
	 *
	 * @param deloIdList
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> createSelectLoadStorageData(List<Integer> deloIdList) throws DbErrorException {

		StringBuilder select = new StringBuilder();
		select.append(" select d.DELO_ID a0, d.RN_DELO a1, d.DELO_DATE a2, d.DELO_TYPE a3 ");
		select.append(" , dd.TOM_NOMER a4 ");
		select.append(" , ds.ID a5, ds.ARCH_NOMER a6, ds.ROOM a7, ds.SHKAF a8, ds.STILLAGE a9, ds.BOX a10 ");

		StringBuilder from = new StringBuilder();
		from.append(" from DELO d ");
		from.append(" left outer join ( ");
		from.append(" 	select t1.DELO_ID, t1.TOM_NOMER from DELO_DOC t1 where t1.DELO_ID in (:deloIdList) and t1.TOM_NOMER is not null union ");
		from.append(" 	select t2.DELO_ID, t2.TOM_NOMER from DELO_DELO t2 where t2.DELO_ID in (:deloIdList) and t2.TOM_NOMER is not null union ");
		from.append(" 	select t3.DELO_ID, t3.TOM_NOMER from DELO_STORAGE t3 where t3.DELO_ID in (:deloIdList) ");
		from.append(" ) dd on dd.DELO_ID = d.DELO_ID ");
		from.append(" left outer join DELO_STORAGE ds on ds.DELO_ID = d.DELO_ID and ds.TOM_NOMER = dd.TOM_NOMER ");

		String where = " where d.DELO_ID in (:deloIdList) order by a4 ";

//		SelectMetadata sm = new SelectMetadata();
//
//		sm.setSql(select.toString() + from + where);
//		sm.setSqlCount(" select count(*) " + from + where);
//		sm.setSqlParameters(Collections.singletonMap("deloIdList", deloIdList));
//
//		return sm;

		try {
			Query query = createNativeQuery(select.toString() + from + where).setParameter("deloIdList", deloIdList);

			return query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на съхранени преписки", e);
		}
	}

	/**
	 * Намира данни за рално съхранение за подадената преписката, като изтегля само данните от вида:<br>
	 * [0]-ID<br>
	 * [1]-TOM_NOMER<br>
	 * [2]-ARCH_NOMER<br>
	 * [3]-ROOM<br>
	 * [4]-SHKAF<br>
	 * [5]-STILLAGE<br>
	 * [6]-BOX<br>
	 *
	 * @param deloId
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> findDeloStorageData(Integer deloId) throws DbErrorException {

		String select = " select ID a0, TOM_NOMER a1, ARCH_NOMER a3, ROOM a4, SHKAF a5, STILLAGE a6, BOX a7 ";

		String from = " from DELO_STORAGE ";

		String where = " where DELO_ID = :deloId order by a1 ";

//		SelectMetadata sm = new SelectMetadata();
//
//		sm.setSql(select + from + where);
//		sm.setSqlCount(" select count(*) " + from + where);
//		sm.setSqlParameters(Collections.singletonMap("deloId", deloId));
//
//		return sm;

		try {
			Query query = createNativeQuery(select + from + where).setParameter("deloId", deloId);

			return query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на съхранение за преписка", e);
		}
	}

	/**
	 * Отразява промените в съхранение на преписка
	 *
	 * @param selected
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public void storeDeloData(List<Object[]> selected) throws DbErrorException, ObjectInUseException {

		for (Object[] row : selected) {

			Integer id = SearchUtils.asInteger(row[5]);
			String room = SearchUtils.trimToNULL((String) row[7]);

			if (room == null) {

				if (id != null) {
					DeloStorage storage = findById(id);

					storage.setDeloIdent(row[1] + "/" + DateUtils.printDate((Date) row[2]));
					delete(storage);

					row[5] = null;
				}

			} else {

				DeloStorage storage;

				if (id == null) {
					storage = new DeloStorage();

					storage.setDeloId(SearchUtils.asInteger(row[0]));
					storage.setTomNomer(SearchUtils.asInteger(row[4]));

				} else {
					storage = findById(id);
				}

				storage.setArchNomer((String) row[6]);
				storage.setRoom((String) row[7]);
				storage.setShkaf((String) row[8]);
				storage.setStillage((String) row[9]);
				storage.setBox((String) row[10]);

				storage.setDeloIdent(row[1] + "/" + DateUtils.printDate((Date) row[2]));
				save(storage);

				row[5] = storage.getId();
			}
		}
	}
}
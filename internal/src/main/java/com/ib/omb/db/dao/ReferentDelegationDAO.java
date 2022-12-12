package com.ib.omb.db.dao;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ADMIN_STR;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DELEGATES;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELEGATES_UPYLNOMOSHTAVANE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELEGATES_ZAMESTVANE;
import static com.ib.system.SysConstants.CODE_CLASSIF_USERS;
import static com.ib.system.utils.SearchUtils.asInteger;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Query;

import com.ib.omb.db.dto.ReferentDelegation;
import com.ib.omb.experimental.Notification;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.omb.system.UserData.DelegatedEmployee;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link ReferentDelegation}
 *
 * @author belev
 */
public class ReferentDelegationDAO extends AbstractDAO<ReferentDelegation> {

	/** @param user */
	public ReferentDelegationDAO(ActiveUser user) {
		super(ReferentDelegation.class, user);
	}

	/**
	 * Намира делегираните права на база входни параметри, като изтегля само данните от вида:<br>
	 * [0]-ID<br>
	 * [1]-CODE_REF<br>
	 * [2]-USER_ID<br>
	 * [3]-DELEGATION_TYPE<br>
	 * [4]-DATE_OT<br>
	 * [5]-DATE_DO<br>
	 * [6]-DELEGATION_INFO<br>
	 * [7]-USER_REG_MOD-изчислено<br>
	 * [8]-DATE_REG_MOD-изчислено<br>
	 *
	 * @param registratura
	 * @param codeRef
	 * @param userId
	 * @param selectOld
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	public SelectMetadata createSelectMetadata(Integer registratura, Integer codeRef, Integer userId, boolean selectOld, Date fromDate, Date toDate) {
		if (registratura == null) {
			throw new IllegalArgumentException("createSelectMetadata() -> registratura required!");
		}

		Map<String, Object> params = new HashMap<>();

		String select = " select d.ID a0, d.CODE_REF a1, d.USER_ID a2, d.DELEGATION_TYPE a3, d.DATE_OT a4, d.DATE_DO a5, d.DELEGATION_INFO a6 " //
			+ ", case when d.USER_LAST_MOD is not null then d.USER_LAST_MOD else d.USER_REG end a7 " //
			+ ", case when d.DATE_LAST_MOD is not null then d.DATE_LAST_MOD else d.DATE_REG end a8 " //
			+ ", d.USER_REG a9";

		String from = " from ADM_REF_DELEGATIONS d ";
		StringBuilder where = new StringBuilder(" where d.REGISTRATURA_ID = :registratura ");

		params.put("registratura", registratura);

		if (codeRef != null) {
			where.append(" and d.CODE_REF = :codeRef ");
			params.put("codeRef", codeRef);
		}
		if (userId != null) {
			where.append(" and d.USER_ID = :userId ");
			params.put("userId", userId);
		}
		if (selectOld) { // ако има дати трябва и те да се гледат, като се налага на дата от на реда
			if (fromDate != null) {
				where.append(" and ( d.DATE_OT >= :fromDate or ( d.DATE_OT <= :fromDate and ( d.DATE_DO is null or d.DATE_DO >= :fromDate ) ) ) ");
				params.put("fromDate", DateUtils.startDate(fromDate));
			}
			if (toDate != null) {
				where.append(" and d.DATE_OT <= :toDate ");
				params.put("toDate", DateUtils.endDate(toDate));
			}

		} else { // само актуалните се искат
			where.append(" and (d.DATE_DO is null or d.DATE_DO >= :currentDate) ");
			params.put("currentDate", DateUtils.startDate(new Date()));
		}

		SelectMetadata sm = new SelectMetadata();

		sm.setSql(select + from + where);
		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * @param entity
	 * @param sd
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public void delete(ReferentDelegation entity, BaseSystemData sd) throws DbErrorException, ObjectInUseException {
		Integer rolia = Objects.equals(entity.getDelegationType(), CODE_ZNACHENIE_DELEGATES_ZAMESTVANE) //
			? OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_ZAM
			: OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_UPALNOMOSTEN;

		Notification notif2 = new Notification(((UserData) getUser()).getUserAccess(), null //
			, OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DELEGATE_REMOVE, rolia, (SystemData) sd);
		notif2.setRefD(entity);
		notif2.setAdresat(entity.getUserId());
		notif2.send(); // за стария

		super.delete(entity);
	}

	/** */
	@Override
	public ReferentDelegation findById(Object id) throws DbErrorException {
		ReferentDelegation entity = super.findById(id);
		if (entity != null) {
			entity.setDbUserId(entity.getUserId());
		}
		return entity;
	}

	/**
	 * Използва се при ЛОГИН. Намира Служители, които замества за период и Служители, от чието име е упълномощен да работи. Търси
	 * се в контекста на регистратура
	 *
	 * @param registratura
	 * @param userId
	 * @param date
	 * @param sd
	 * @return
	 * @throws DbErrorException
	 */
	public List<DelegatedEmployee> findDelegatedEmployees(Integer registratura, Integer userId, Date date, BaseSystemData sd) throws DbErrorException {
		try {
			// Служители, които замества за период и Служители, от чието име е упълномощен да работи
			Query query = JPA.getUtil(getUnitName()).getEntityManager().createNativeQuery( //
				"select d.CODE_REF, d.DELEGATION_TYPE from ADM_REF_DELEGATIONS d" //
					+ " where d.USER_ID = :userId and d.REGISTRATURA_ID = :registratura" //
					+ " and d.DATE_OT <= :dateArg and (d.DATE_DO is null or d.DATE_DO >= :dateArg) order by d.DELEGATION_TYPE desc");

			query.setParameter("registratura", registratura);
			query.setParameter("userId", userId);
			query.setParameter("dateArg", date);

			@SuppressWarnings("unchecked")
			List<Object[]> list = query.getResultList();
			List<DelegatedEmployee> result = new ArrayList<>(list.size());

			for (Object[] row : list) {
				Integer codeRef = asInteger(row[0]);

				Integer otherRegistratura = (Integer) sd.getItemSpecific(CODE_CLASSIF_ADMIN_STR, codeRef, getUserLang(), date, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);

				if (!registratura.equals(otherRegistratura)) {
					continue; // ако са в различни регистратури вече няма как да се заместват
				}

				String nameRef = sd.decodeItem(CODE_CLASSIF_ADMIN_STR, codeRef, getUserLang(), date);
				Integer delegationType = asInteger(row[1]);

				result.add(new DelegatedEmployee(codeRef //
					, (Objects.equals(delegationType, CODE_ZNACHENIE_DELEGATES_UPYLNOMOSHTAVANE) ? "упълномощен от " : "заместник на ") + nameRef//
					, delegationType)); //
			}
			return result;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на заместване/упълномощаване!", e);
		}
	}

	/**
	 * Допълнително се правят валидации
	 *
	 * @param entity
	 * @param sd
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	@SuppressWarnings("unchecked")
	public ReferentDelegation save(ReferentDelegation entity, BaseSystemData sd) throws DbErrorException, ObjectInUseException {

		Integer rolia = OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_UPALNOMOSTEN;
		if (Objects.equals(entity.getDelegationType(), CODE_ZNACHENIE_DELEGATES_ZAMESTVANE)) {
			rolia = OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_ZAM;
//			// Един служител може да има само едно активно заместване в определен период в качеството си на заместван
//			List<Object[]> found = null;
//			try {
//				StringBuilder sql = new StringBuilder();
//
//				sql.append(" select USER_ID, DATE_OT, DATE_DO ");
//				sql.append(" from ADM_REF_DELEGATIONS ");
//				sql.append(" where CODE_REF = :codeRef and DELEGATION_TYPE = :delegationType ");
//				sql.append(" and ((DATE_OT <= :dateOt and DATE_DO >= :dateOt) or (DATE_OT <= :dateDo and DATE_DO >= :dateDo) or (DATE_OT >= :dateOt and DATE_DO <= :dateDo)) ");
//				if (entity.getId() != null) {
//					sql.append(" and ID != :id ");
//				}
//
//				Query query = createNativeQuery(sql.toString()).setParameter("codeRef", entity.getCodeRef()).setParameter("delegationType", CODE_ZNACHENIE_DELEGATES_ZAMESTVANE) //
//					.setParameter("dateOt", entity.getDateOt()).setParameter("dateDo", entity.getDateTo());
//				if (entity.getId() != null) {
//					query.setParameter("id", entity.getId());
//				}
//
//				found = query.getResultList();
//
//			} catch (Exception e) {
//				throw new DbErrorException("Грешка при търсене на активни замествания!", e);
//			}
//
//			if (found != null && !found.isEmpty()) {
//				Object[] row = found.get(0);
//
//				StringBuilder sb = new StringBuilder();
//				sb.append("За " + sd.decodeItem(CODE_CLASSIF_ADMIN_STR, entity.getCodeRef(), getUserLang(), entity.getDateOt()));
//				sb.append(" е регистрирано заместване от потребител " + sd.decodeItem(CODE_CLASSIF_USERS, asInteger(row[0]), getUserLang(), entity.getDateOt()));
//				sb.append(" в периода " + DateUtils.printDate((Date) row[1]));
//				sb.append(" - " + DateUtils.printDate((Date) row[2]));
//
//				throw new ObjectInUseException(sb.toString());
//			}
		}

		Date checkDateTo;
		if (entity.getDateTo() != null) {
			checkDateTo = entity.getDateTo();
		} else {
			checkDateTo = new Date(DateUtils.systemMaxDate().getTime()); // защото макс дате връща java.sql.Date и да няма драми в селекта
		}
		
//		Ако служител А замества Б, то в същия период Б не може да замества А.
//		Ако служител А е упълномощил Б, то в същия период Б не може да упълномощи А.
		List<Object[]> check1 = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select DATE_OT, DATE_DO from ADM_REF_DELEGATIONS ");
			sql.append(" where CODE_REF = :codeRef and USER_ID = :userId and DELEGATION_TYPE = :delegationType ");
			sql.append(" and ((DATE_OT <= :dateOt and DATE_DO >= :dateOt) or (DATE_OT <= :dateDo and DATE_DO >= :dateDo) or (DATE_OT >= :dateOt and DATE_DO <= :dateDo)) ");

			Query query = createNativeQuery(sql.toString()) //
				.setParameter("codeRef", entity.getUserId()).setParameter("userId", entity.getCodeRef()) // разменяме
				.setParameter("delegationType", entity.getDelegationType()) //
				.setParameter("dateOt", entity.getDateOt()).setParameter("dateDo", checkDateTo);

			check1 = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на текущи замествания/упълномощавания!", e);
		}
		if (check1 != null && !check1.isEmpty()) {
			Object[] row = check1.get(0);

			StringBuilder sb = new StringBuilder();
			sb.append("В периода " + DateUtils.printDate((Date) row[0]) + "-" + DateUtils.printDate((Date) row[1]) + " ");
			sb.append(sd.decodeItem(CODE_CLASSIF_USERS, entity.getUserId(), getUserLang(), null) + " ");
			sb.append(sd.decodeItem(CODE_CLASSIF_DELEGATES, entity.getDelegationType(), getUserLang(), null));
			sb.append(" " + sd.decodeItem(CODE_CLASSIF_USERS, entity.getCodeRef(), getUserLang(), null) + ".");

			throw new ObjectInUseException(sb.toString());
		}

//		Да не може за един период А да замества и да е упълномощен от Б.
		List<Object[]> check2 = null;
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select DATE_OT, DATE_DO, DELEGATION_TYPE from ADM_REF_DELEGATIONS ");
			sql.append(" where CODE_REF = :codeRef and USER_ID = :userId and DELEGATION_TYPE = :delegationType ");
			sql.append(" and ((DATE_OT <= :dateOt and DATE_DO >= :dateOt) or (DATE_OT <= :dateDo and DATE_DO >= :dateDo) or (DATE_OT >= :dateOt and DATE_DO <= :dateDo)) ");

			int otherType = entity.getDelegationType().equals(CODE_ZNACHENIE_DELEGATES_ZAMESTVANE) //
				? CODE_ZNACHENIE_DELEGATES_UPYLNOMOSHTAVANE
				: CODE_ZNACHENIE_DELEGATES_ZAMESTVANE;

			Query query = createNativeQuery(sql.toString()) //
				.setParameter("codeRef", entity.getCodeRef()).setParameter("userId", entity.getUserId()) //
				.setParameter("delegationType", otherType) //
				.setParameter("dateOt", entity.getDateOt()).setParameter("dateDo", checkDateTo);

			check2 = query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на текущи замествания/упълномощавания!", e);
		}
		if (check2 != null && !check2.isEmpty()) {
			Object[] row = check2.get(0);

			StringBuilder sb = new StringBuilder();
			sb.append("В периода " + DateUtils.printDate((Date) row[0]) + "-" + DateUtils.printDate((Date) row[1]) + " ");
			sb.append(sd.decodeItem(CODE_CLASSIF_USERS, entity.getCodeRef(), getUserLang(), null) + " ");
			sb.append(sd.decodeItem(CODE_CLASSIF_DELEGATES, SearchUtils.asInteger(row[2]), getUserLang(), null));
			sb.append(" " + sd.decodeItem(CODE_CLASSIF_USERS, entity.getUserId(), getUserLang(), null) + ".");

			throw new ObjectInUseException(sb.toString());
		}

		Integer oldUserId = entity.getDbUserId(); // за нотификации ще ми трябва

		StringBuilder ident = new StringBuilder();
		ident.append(sd.decodeItem(CODE_CLASSIF_USERS, entity.getCodeRef(), getUserLang(), entity.getDateOt()));
		ident.append(" " + sd.decodeItem(CODE_CLASSIF_DELEGATES, entity.getDelegationType(), getUserLang(), entity.getDateOt()) + " ");
		ident.append(sd.decodeItem(CODE_CLASSIF_USERS, entity.getUserId(), getUserLang(), entity.getDateOt()));

		if (entity.getId() != null) {
			entity = merge(entity); // това ще гарантира, че като се извика корекция и се журналира IdentInfo ще е налична
		}
		entity.setIdentInfo(ident.toString()); // тука е сетвам в persistent обекта

		entity = super.save(entity);

		if (oldUserId == null) { // нов запис

			Notification notif = new Notification(((UserData) getUser()).getUserAccess(), null //
				, OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_REF_DELEGATE, rolia, (SystemData) sd);
			notif.setRefD(entity);
			notif.setAdresat(entity.getUserId());
			notif.send();

		} else if (!oldUserId.equals(entity.getUserId())) { // корекция с промяна на човека

			Notification notif1 = new Notification(((UserData) getUser()).getUserAccess(), null //
				, OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_REF_DELEGATE, rolia, (SystemData) sd);
			notif1.setRefD(entity);
			notif1.setAdresat(entity.getUserId());
			notif1.send(); // за новия

			Notification notif2 = new Notification(((UserData) getUser()).getUserAccess(), null //
				, OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DELEGATE_REMOVE, rolia, (SystemData) sd);
			notif2.setRefD(entity);
			notif2.setAdresat(oldUserId);
			notif2.send(); // за стария
		}

		entity.setDbUserId(entity.getUserId());
		return entity;
	}
}
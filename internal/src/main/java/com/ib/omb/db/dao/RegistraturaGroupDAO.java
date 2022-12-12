package com.ib.omb.db.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Query;

import com.ib.indexui.system.Constants;
import com.ib.omb.db.dto.RegistraturaGroup;
import com.ib.omb.db.dto.RegistraturaReferent;
import com.ib.omb.system.OmbConstants;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.BaseUserData;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link RegistraturaGroup}
 *
 * @author belev
 */
public class RegistraturaGroupDAO extends AbstractDAO<RegistraturaGroup> {

	/**
	 * DAO for {@link RegistraturaReferent}
	 *
	 * @author belev
	 */
	static class RegistraturaReferentDAO extends AbstractDAO<RegistraturaReferent> {
		/** @param user */
		RegistraturaReferentDAO(ActiveUser user) {
			super(RegistraturaReferent.class, user);
		}

		/**
		 * сетва с коя класификация трябва да се разкодира за журнала
		 *
		 * @param entity
		 * @param groupType
		 * @throws DbErrorException
		 * @throws ObjectInUseException
		 */
		void delete(RegistraturaReferent entity, Integer groupType) throws DbErrorException, ObjectInUseException {
			if (Objects.equals(groupType, OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_EMPL)) {
				entity.setCodeRefClassif(Constants.CODE_CLASSIF_ADMIN_STR);

			} else if (Objects.equals(groupType, OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS)) {
				entity.setCodeRefClassif(OmbConstants.CODE_CLASSIF_EGOV_ORGANISATIONS);

			} else {
				entity.setCodeRefClassif(Constants.CODE_CLASSIF_REFERENTS);
			}
			super.delete(entity);
		}

		/**
		 * Намира редовете към тази група
		 *
		 * @param groupId
		 * @param codeRef
		 * @param codeRefList
		 * @return
		 * @throws DbErrorException
		 */
		@SuppressWarnings("unchecked")
		List<RegistraturaReferent> findInGroup(Integer groupId, Integer codeRef, List<Integer> codeRefList) throws DbErrorException {
			try {
				StringBuilder sql = new StringBuilder();
				sql.append(" select rr from RegistraturaReferent rr where rr.groupId = :groupId ");
				if (codeRef != null) {
					sql.append(" and rr.codeRef = :codeRef ");

				} else if (codeRefList != null && !codeRefList.isEmpty()) {
					sql.append(" and rr.codeRef in (:codeRefList) ");
				}

				Query query = createQuery(sql.toString()).setParameter("groupId", groupId);

				if (codeRef != null) {
					query.setParameter("codeRef", codeRef);

				} else if (codeRefList != null && !codeRefList.isEmpty()) {
					query.setParameter("codeRefList", codeRefList);
				}

				return query.getResultList();

			} catch (Exception e) {
				throw new DbErrorException("Грешка при търсене на кореспондент в група!", e);
			}
		}
	}

	/** @param user */
	public RegistraturaGroupDAO(ActiveUser user) {
		super(RegistraturaGroup.class, user);
	}

	/** Предварително изтрива и участницитв в групата */
	@Override
	public void delete(RegistraturaGroup entity) throws DbErrorException, ObjectInUseException {
		RegistraturaReferentDAO rrDao = new RegistraturaReferentDAO(getUser());

		List<RegistraturaReferent> list = rrDao.findInGroup(entity.getId(), null, null);
		if (!list.isEmpty()) {
			for (RegistraturaReferent rr : list) {
				rrDao.delete(rr, entity.getGroupType());
			}
		}

		super.delete(entity);
	}

	/**
	 * Изтрива връзка с кореспондент от група кореспонденти
	 *
	 * @param group
	 * @param codeRef
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public void deleteRegistraturaReferent(RegistraturaGroup group, Integer codeRef) throws DbErrorException, ObjectInUseException {
		RegistraturaReferentDAO rrDao = new RegistraturaReferentDAO(getUser());

		List<RegistraturaReferent> list = rrDao.findInGroup(group.getId(), codeRef, null);
		if (!list.isEmpty()) {
			rrDao.delete(list.get(0), group.getGroupType());
		}
	}

	/**
	 * Търсене на групи служители/кореспонденти по регистратура
	 *
	 * @param registraturaList
	 * @param groupTypeList
	 * @param codeRef 
	 * @param nameArg 
	 * @param infoArg 
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<RegistraturaGroup> findByRegistraturaId(List<Integer> registraturaList, List<Integer> groupTypeList, Integer codeRef, String nameArg, String infoArg) throws DbErrorException {
		try {
			StringBuilder sql = new StringBuilder();
			sql.append(" select g from RegistraturaGroup g ");
			if (codeRef != null) {
				sql.append(" inner join RegistraturaReferent rr on rr.groupId = g.id and rr.codeRef = :codeRef ");
			}
			sql.append(" where 1=1 ");
			if (registraturaList != null && !registraturaList.isEmpty()) {
				sql.append(" and g.registraturaId in (:registraturaList) ");
			} 
			if (groupTypeList != null && groupTypeList.size() > 0) {
				sql.append(" and g.groupType in (:groupTypeList) ");
			}
			nameArg = SearchUtils.trimToNULL_Upper(nameArg);
			if (nameArg != null) {
				sql.append(" and upper(g.groupName) like :nameArg ");
			}
			infoArg = SearchUtils.trimToNULL_Upper(infoArg);
			if (infoArg != null) {
				sql.append(" and upper(g.groupInfo) like :infoArg ");
			}
			sql.append(" order by g.registraturaId, g.groupName ");

			Query query = createQuery(sql.toString());
			if (registraturaList != null && !registraturaList.isEmpty()) {
				query.setParameter("registraturaList", registraturaList);
			}
			if (groupTypeList != null && groupTypeList.size() > 0) {
				query.setParameter("groupTypeList", groupTypeList);
			}
			if (codeRef != null) {
				query.setParameter("codeRef", codeRef);
			}
			if (nameArg != null) {
				query.setParameter("nameArg", "%" + nameArg + "%");
			}
			if (infoArg != null) {
				query.setParameter("infoArg", "%" + infoArg + "%");
			}

			return query.getResultList();

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на групи служители/кореспонденти за регистратура!", e);
		}
	}

	/**
	 * Дава резултата в следния вид<br>
	 * <br>
	 * <b>Група кореспонденти:</b><br>
	 * [0]-REGISTRATURA_REFERENTS.ID<br>
	 * [1]-CODE_REF<br>
	 * [2]-REF_NAME<br>
	 * [3]-REF_TYPE<br>
	 * [4]-NFL_EIK<br>
	 * [5]-FZL_EGN<br>
	 * [6]-ADDR_COUNTRY<br>
	 * [7]-ADDR_TEXT<br>
	 * [8]-TVM<br>
	 * [9]-IME<br>
	 * [10]-OBSTINA_IME<br>
	 * [11]-OBLAST_IME<br>
	 * <br>
	 * <b>Група служители:</b><br>
	 * [0]-REGISTRATURA_REFERENTS.ID<br>
	 * [1]-CODE_REF<br>
	 * [2]-REF_NAME<br>
	 * [3]-EMPL_POSITION<br>
	 * [4]-ZVENO_NAME<br>
	 * <br>
	 * <b>Група СЕОС:</b><br>
	 * [0]-REGISTRATURA_REFERENTS.ID<br>
	 * [1]-EGOV_ORGANISATIONS.ID<br>
	 * [2]-EIK<br>
	 * [3]-ADMINISTRATIVE_BODY_NAME<br>
	 * [4]-EMAIL<br>
	 * [5]-GUID<br>
	 *
	 * @param groupId
	 * @param groupType
	 * @param ud
	 * @return
	 */
	public SelectMetadata findReferentsByIdGroup(Integer groupId, Integer groupType, BaseUserData ud) {
		Map<String, Object> params = new HashMap<>();

		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();

		if (Objects.equals(groupType, OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS)) {
			select.append(" select rr.ID a0, e.ID a1, e.EIK a2, e.ADMINISTRATIVE_BODY_NAME a3, e.EMAIL a4, e.GUID a5 ");
			from.append(" from REGISTRATURA_REFERENTS rr ");
			from.append(" inner join EGOV_ORGANISATIONS e on e.ID = rr.CODE_REF ");
			where.append(" where rr.GROUP_ID = :groupId ");

			params.put("groupId", groupId);

		} else {
			from.append(" from REGISTRATURA_REFERENTS rr ");
			from.append(" inner join ADM_REFERENTS r on r.CODE = rr.CODE_REF ");

			if (Objects.equals(groupType, OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_CORRESP)) {
				select.append(" select rr.ID a0, rr.CODE_REF a1, r.REF_NAME a2, r.REF_TYPE a3, r.NFL_EIK a4 ");
				if (ud.hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_SEE_PERSONAL_DATA)) {
					select.append(" , r.FZL_EGN a5 ");
				} else {
					select.append(" , '' a5 ");
				}
				select.append(" , a.ADDR_COUNTRY a6, a.ADDR_TEXT a7 ");
				select.append(" , att.TVM a8, att.IME a9, att.OBSTINA_IME a10, att.OBLAST_IME a11 ");

				from.append(" left outer join ADM_REF_ADDRS a on a.CODE_REF = r.CODE and a.ADDR_TYPE = :addrTypeCorresp ");
				from.append(" left outer join EKATTE_ATT att on att.EKATTE = a.EKATTE and att.DATE_OT <= :dateArg and att.DATE_DO > :dateArg ");

				params.put("addrTypeCorresp", OmbConstants.CODE_ZNACHENIE_ADDR_TYPE_CORRESP);

			} else {
				select.append(" select rr.ID a0, rr.CODE_REF a1, r.REF_NAME a2, r.EMPL_POSITION a3, p.REF_NAME a4 ");
				from.append(" left outer join ADM_REFERENTS p on p.CODE = r.CODE_PARENT and p.DATE_OT <= :dateArg and p.DATE_DO > :dateArg ");
			}

			where.append(" where rr.GROUP_ID = :groupId and r.DATE_OT <= :dateArg and r.DATE_DO > :dateArg ");

			params.put("groupId", groupId);
			params.put("dateArg", DateUtils.startDate(new Date()));
		}

		SelectMetadata sm = new SelectMetadata();

		sm.setSql(select.toString() + from.toString() + where.toString());
		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/**
	 * Добавя нови връзки с кореспонденти в група кореспонденти
	 *
	 * @param group
	 * @param codeRefList
	 * @param sd
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public void saveRegistraturaReferent(RegistraturaGroup group, List<Integer> codeRefList, BaseSystemData sd) throws DbErrorException, ObjectInUseException {
		if (codeRefList == null || codeRefList.isEmpty()) {
			return;
		}

		RegistraturaReferentDAO rrDao = new RegistraturaReferentDAO(getUser());

		List<Integer> saveList = new ArrayList<>(codeRefList);

		List<RegistraturaReferent> foundList = rrDao.findInGroup(group.getId(), null, codeRefList);
		if (foundList.size() == codeRefList.size()) { // всички са добавени
			throw new ObjectInUseException("Избраните участници вече са добавени в групата!");

		} else if (!foundList.isEmpty()) { // има някакви и трябва да ги махна за да не изгърми
			for (RegistraturaReferent found : foundList) {
				saveList.remove(found.getCodeRef());
			}
		}

		for (Integer codeRef : saveList) {
			RegistraturaReferent rr = new RegistraturaReferent(codeRef);
			rr.setGroupId(group.getId());

			if (Objects.equals(group.getGroupType(), OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_EMPL)) {
				rr.setCodeRefClassif(Constants.CODE_CLASSIF_ADMIN_STR);

			} else if (Objects.equals(group.getGroupType(), OmbConstants.CODE_ZNACHENIE_REG_GROUP_TYPE_SEOS)) {
				rr.setCodeRefClassif(OmbConstants.CODE_CLASSIF_EGOV_ORGANISATIONS);

			} else {
				rr.setCodeRefClassif(Constants.CODE_CLASSIF_REFERENTS);
			}
			rrDao.save(rr);
		}
	}
}
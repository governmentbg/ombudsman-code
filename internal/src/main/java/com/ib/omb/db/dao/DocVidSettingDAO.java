package com.ib.omb.db.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.Query;

import com.ib.omb.db.dto.DocVidSetting;
import com.ib.omb.system.OmbConstants;
import com.ib.system.ActiveUser;
import com.ib.system.BaseSystemData;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;

/**
 * DAO for {@link DocVidSetting}
 *
 * @author belev
 */
public class DocVidSettingDAO extends AbstractDAO<DocVidSetting> {

	/** @param user */
	public DocVidSettingDAO(ActiveUser user) {
		super(DocVidSetting.class, user);
	}

	/**
	 * Намира х-ките за подадената регистраура, като изтегля само данните от вида:<br>
	 * [0]-ID<br>
	 * [1]-DOC_VID<br>
	 * [2]-REGISTER_ID<br>
	 * [3]-ACT_NOMER<br>
	 * [4]-PROC_DEF_IN<br>
	 * [5]-PROC_DEF_OWN<br>
	 * [6]-PROC_DEF_WORK<br>
	 *
	 * @param registraturaId
	 * @return
	 */
	public SelectMetadata createSelectMetadataByRegistraturaId(Integer registraturaId) {
		Map<String, Object> params = new HashMap<>();

		String select = " select s.ID a0, s.DOC_VID a1, s.REGISTER_ID a2, s.ACT_NOMER a3, defIn.DEF_ID a4, defOwn.DEF_ID a5, defWork.DEF_ID a6 ";
		StringBuilder from = new StringBuilder(" from DOC_VID_SETTINGS s ");

		from.append(" left outer join PROC_DEF defIn on defIn.DEF_ID = s.PROC_DEF_IN and defIn.STATUS = :defActSts and defIn.DOC_TYPE = 1 ");
		from.append(" left outer join PROC_DEF defOwn on defOwn.DEF_ID = s.PROC_DEF_OWN and defOwn.STATUS = :defActSts and defOwn.DOC_TYPE = 2 ");
		from.append(" left outer join PROC_DEF defWork on defWork.DEF_ID = s.PROC_DEF_WORK and defWork.STATUS = :defActSts and defWork.DOC_TYPE = 3 ");

		String where = " where s.REGISTRATURA_ID = :registratura ";

		params.put("registratura", registraturaId);
		params.put("defActSts", OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE);
		
		SelectMetadata sm = new SelectMetadata();

		sm.setSql(select + from + where);
		sm.setSqlCount(" select count(*) " + from + where);
		sm.setSqlParameters(params);

		return sm;
	}

	/** */
	@Override
	public void delete(DocVidSetting entity) throws DbErrorException, ObjectInUseException {
		if (entity.getRegisterId() != null) { // ако е празно няма смисъл тази валидация
			Number cnt = (Number) createNativeQuery("select count (*) as cnt from DOC where REGISTER_ID = :regId and DOC_VID = :docVid") //
				.setParameter("regId", entity.getRegisterId()).setParameter("docVid", entity.getDocVid()) //
				.getResultList().get(0);
			if (cnt.intValue() > 0) {
				throw new ObjectInUseException("В регистъра по вид документ има регистрирани документи и не може да бъде изтрит!");
			}
		}

		super.delete(entity);
	}

	/** */
	@Override
	public DocVidSetting findById(Object id) throws DbErrorException {
		DocVidSetting setting = super.findById(id);

		if (setting == null) {
			return setting;
		}
		setting.setDbPrefix(setting.getPrefix());

		return setting;
	}

	/**
	 * @param entity
	 * @param sd
	 * @return
	 * @throws DbErrorException
	 * @throws ObjectInUseException
	 */
	public DocVidSetting save(DocVidSetting entity, BaseSystemData sd) throws DbErrorException, ObjectInUseException {
		if (entity.getPrefix() != null) { // важно е да няма интервали в началото и края
			entity.setPrefix(entity.getPrefix().trim());
		}

		if (entity.getId() != null) {
			if (!Objects.equals(entity.getPrefix(), entity.getDbPrefix())) { // трябва да се провери дали няма документи с този
																				// регистър
				Number cnt; // ако има вече документи трябва да се даде грешка
				try {
					cnt = (Number) createNativeQuery("select count (*) as cnt from DOC where REGISTER_ID = :regId and DOC_VID = :docVid") //
						.setParameter("regId", entity.getRegisterId()).setParameter("docVid", entity.getDocVid()) //
						.getResultList().get(0);
				} catch (Exception e) {
					throw new DbErrorException("Грешка при търсене на документи за регистър по вид документ!", e);
				}
				if (cnt.intValue() > 0) {
					String error = "Префиксът не може да бъде променен, защото в регистъра по вид документ има регистрирани документи!";

					throw new ObjectInUseException(error);
				}
			}
		}

		DocVidSetting saved = super.save(entity);

		saved.setDbPrefix(entity.getPrefix());

		return saved;
	}

	/**
	 * Избира регистри с алгоритъм индекс по вид документи:<br>
	 * @param registraturaId
	 * 
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> selectRegistriHDanul(Integer registraturaId, Boolean valid) {
		

		String select = " select s.ID a0, s.DOC_VID a1, s.REGISTER_ID a2, s.ACT_NOMER a3 "
				+ "from DOC_VID_SETTINGS s";
		 		if (null!=valid)	
		 			select+=" JOIN REGISTRI r on r.REGISTER_ID = s.REGISTER_ID and r.VALID = :val ";
			  
		 		select += " where s.BEG_NOMER is not null and s.STEP > 0 and s.ACT_NOMER > 1 ";
		
			   if (null!=registraturaId) 
				   select += " and s.REGISTRATURA_ID = :registratura ";
		

		Query query = createNativeQuery(select);
		if (null!=valid)
			query.setParameter("val", valid);

		if (registraturaId != null) 
			query.setParameter("registratura", registraturaId);
		
	
		return query.getResultList();
		
		
	}

}
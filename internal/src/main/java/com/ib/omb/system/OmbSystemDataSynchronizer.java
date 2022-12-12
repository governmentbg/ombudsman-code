package com.ib.omb.system;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.system.BaseSystemData;
import com.ib.system.SysConstants;
import com.ib.system.SystemDataSynchronizer;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;

/**
 * Допълнително си синхронизира динамичните класификации
 *
 * @author belev
 */
public class OmbSystemDataSynchronizer extends SystemDataSynchronizer {

	private static final Logger LOGGER = LoggerFactory.getLogger(OmbSystemDataSynchronizer.class);

	/**
	 * @param systemData
	 * @param seconds
	 */
	OmbSystemDataSynchronizer(BaseSystemData systemData, Long seconds) {
		super(systemData, seconds);
	}

	/** */
	@Override
	protected void processDynamicClassif(Date h2Date) throws DbErrorException {
		super.processDynamicClassif(h2Date);

		// TODO има ли как да се хванат системните настройки както и настройки на регистратура
		
		// TODO да се проверят
		// CODE_CLASSIF_EDELIVERY_ORGANISATIONS
		// CODE_CLASSIF_EGOV_ORGANISATIONS
		// CODE_CLASSIF_EKATTE

		List<?> list = null; // ще се използва за всички
		Set<Integer> codes = new HashSet<>(); // тък се събират тези, които трябва да се рефрешнат

		String h2DateParam = "h2Date";

//		ADM_USERS - SysConstants.CODE_CLASSIF_USERS
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.USER_ID) as syncme from ADM_USERS a having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(SysConstants.CODE_CLASSIF_USERS);
			codes.add(Constants.CODE_CLASSIF_ADMIN_STR);

			((SystemData) this.systemData).setUserNotifications(null); // ще трябва да се презаредят наново
		}

//		ADM_REFERENTS - Constants.CODE_CLASSIF_ADMIN_STR
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.REF_ID) as syncme from ADM_REFERENTS a where a.CODE_CLASSIF = :codeClassif having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter("codeClassif", Constants.CODE_CLASSIF_ADMIN_STR).setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(Constants.CODE_CLASSIF_ADMIN_STR);
			codes.add(SysConstants.CODE_CLASSIF_USERS);
			codes.add(OmbConstants.CODE_CLASSIF_ADMIN_STR_REPORTS);
			codes.add(OmbConstants.CODE_CLASSIF_ADMIN_STR_ZVENA);
		}

//		ADM_REFERENTS - Constants.CODE_CLASSIF_REFERENTS
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.REF_ID) as syncme from ADM_REFERENTS a where a.CODE_CLASSIF is null having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(Constants.CODE_CLASSIF_REFERENTS);
			codes.add(OmbConstants.CODE_CLASSIF_ORGAN_NPM);
		}

//		REGISTRATURA_GROUPS
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.GROUP_ID) as syncme from REGISTRATURA_GROUPS a having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(OmbConstants.CODE_CLASSIF_GROUP_EMPL);
			codes.add(OmbConstants.CODE_CLASSIF_GROUP_CORRESP);
		}

//		REGISTRI
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.REGISTER_ID) as syncme from REGISTRI a having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(OmbConstants.CODE_CLASSIF_REGISTRI);
			codes.add(OmbConstants.CODE_CLASSIF_REGISTRI_SORTED);
			codes.add(OmbConstants.CODE_CLASSIF_REGISTRATURI); // заради списъка от регистри като специфика
		} else {
//		REGISTRATURI
			list = JPA.getUtil().getEntityManager().createNativeQuery( //
				"select max (a.REGISTRATURA_ID) as syncme from REGISTRATURI a having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
				.setParameter(h2DateParam, h2Date).getResultList();
			if (!list.isEmpty()) {
				codes.add(OmbConstants.CODE_CLASSIF_REGISTRATURI);
				codes.add(OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS);
				codes.add(OmbConstants.CODE_CLASSIF_REGISTRATURI_REQDOC);
			}
		}

//		DOC_VID_SETTINGS - OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.ID) as syncme from DOC_VID_SETTINGS a having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS);
		}
//		DOC_VID_SETTINGS - OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS - през файловете заради шаблоните
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.ID) as syncme from FILE_OBJECTS a where a.OBJECT_CODE = :objCode having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter("objCode", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_VID_SETT).setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS);
		}

//		ADM_REF_DELEGATIONS - Constants.CODE_CLASSIF_EMPL_REPLACES
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.ID) as syncme from ADM_REF_DELEGATIONS a having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(Constants.CODE_CLASSIF_EMPL_REPLACES);
		}

//		DOC_SHEMA - OmbConstants.CODE_CLASSIF_DOC_SHEMA
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.SHEMA_ID) as syncme from DOC_SHEMA a having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(OmbConstants.CODE_CLASSIF_DOC_SHEMA);
		}

//		REGISTRATURA_MAILBOXES - OmbConstants.CODE_CLASSIF_MAILBOXES
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.MAILBOX_ID) as syncme from REGISTRATURA_MAILBOXES a having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(OmbConstants.CODE_CLASSIF_MAILBOXES);
		}

//		PROC_DEF - OmbConstants.CODE_CLASSIF_PROCEDURI
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.DEF_ID) as syncme from PROC_DEF a having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(OmbConstants.CODE_CLASSIF_PROCEDURI);
			codes.add(OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS);
		}

//		MODEL_AIS - Constants.CODE_CLASSIF_INF_MODEL
		list = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select max (a.AIS_ID) as syncme from MODEL_AIS a having max (a.DATE_REG) > :h2Date or max (a.DATE_LAST_MOD) > :h2Date") //
			.setParameter(h2DateParam, h2Date).getResultList();
		if (!list.isEmpty()) {
			codes.add(Constants.CODE_CLASSIF_INF_MODEL);
			this.systemData.getModel().reset();
		}

		if (!codes.isEmpty()) { // само ако има нещо
			LOGGER.info("Start reset dynamic classifications SystemData={}, h2Date={}", this.systemData.getClass().getName(), h2Date);

			for (Integer code : codes) {
				LOGGER.info("\tCODE_CLASSIF={}", code);

				this.systemData.reloadClassif(code, false, true);
			}
		}
	}
}
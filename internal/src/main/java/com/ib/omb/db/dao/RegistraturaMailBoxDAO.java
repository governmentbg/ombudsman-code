package com.ib.omb.db.dao;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dto.RegistraturaMailBox;
import com.ib.omb.db.dto.RegistraturaMailBoxVar;
import com.ib.system.ActiveUser;
import com.ib.system.SysConstants;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.SearchUtils;

/**
 * DAO for {@link RegistraturaMailBox}
 *
 * @author mamun
 */
public class RegistraturaMailBoxDAO extends AbstractDAO<RegistraturaMailBox> {
	
	static final Logger LOGGER = LoggerFactory.getLogger(RegistraturaMailBox.class);

	public RegistraturaMailBoxDAO(ActiveUser user) {
		super(user);		
	}

	@SuppressWarnings("unchecked")
	public List<RegistraturaMailBox> findRegBoxes(Integer regId) throws DbErrorException{
		
		try {
			return JPA.getUtil().getEntityManager().createQuery("from RegistraturaMailBox where registraturaId = :RI").setParameter("RI", regId).getResultList();
		} catch (Exception e) {
			LOGGER.error("Грешка при извличане на пощенски кутии !", e);
			throw new DbErrorException("Грешка при извличане на пощенски кутии !", e);
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public List<RegistraturaMailBoxVar> findBoxVariables(Integer idMailBox) throws DbErrorException{
		
		try {
			return JPA.getUtil().getEntityManager().createQuery("from RegistraturaMailBoxVar where mailBoxId = :BI").setParameter("BI", idMailBox).getResultList();
		} catch (Exception e) {
			LOGGER.error("Грешка при извличане на променливи на пощенски кутии !", e);
			throw new DbErrorException("Грешка при извличане на променливи на пощенски кутии !", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public List<RegistraturaMailBoxVar> findGlobalVariables() throws DbErrorException{
		
		List<RegistraturaMailBoxVar> result = new ArrayList<RegistraturaMailBoxVar>();
		try {
			List<Object[]> all = JPA.getUtil().getEntityManager().createNativeQuery("select OPTION_LABEL, OPTION_VALUE from SYSTEM_OPTIONS where OPTION_LABEL like 'mail.%'").getResultList();
			for (Object[] row : all ) {
				result.add(new RegistraturaMailBoxVar(SearchUtils.asString(row[0]), SearchUtils.asString(row[1])));
			}
		} catch (Exception e) {
			LOGGER.error("Грешка при извличане на променливи на пощенски кутии !", e);
			throw new DbErrorException("Грешка при извличане на променливи на пощенски кутии !", e);
		}
		
		return result;
	}
	
	
	
	
	public RegistraturaMailBox findById(Integer idMailBox) throws DbErrorException {
		
		RegistraturaMailBox mailBox = super.findById(idMailBox);
		if (mailBox != null) {
			mailBox.setVariables(findBoxVariables(idMailBox));
		}
		
		return mailBox;
	}
	
	
	public RegistraturaMailBox save(RegistraturaMailBox mailBox) throws DbErrorException {
		int codeAction = mailBox.getId() == null ? SysConstants.CODE_DEIN_ZAPIS : SysConstants.CODE_DEIN_KOREKCIA;
		mailBox.setAuditable(false); // за да не запише ред в журнала при записа на обекта
		
		super.save(mailBox);
		if (mailBox.getVariables() != null) {
			ArrayList<RegistraturaMailBoxVar> forDelete = new ArrayList<RegistraturaMailBoxVar>();
			for (RegistraturaMailBoxVar var : mailBox.getVariables()){
				if (var.getFlag() != null ) {
					if (var.getFlag().equals(3)) {
						//Изтриване
						forDelete.add(var);
						if (var.getId() != null) {
							deleteMailBoxVar(var);
						}
					}else {
						if (var.getFlag().equals(2) || var.getId() == null) {
							//Запис/промяна
							var.setMailBoxId(mailBox.getId());
							if (var.getId() == null) {
								JPA.getUtil().getEntityManager().persist(var);
							}else {
								JPA.getUtil().getEntityManager().merge(var);								
							}
						}
					}
				}
			}
			mailBox.getVariables().removeAll(forDelete);
		}
		
		saveAudit(mailBox, codeAction); // тука вече трябва да всичко да е насетвано и записа в журнала е ОК

		return mailBox;
	}
	
	
	public void deleteMailBoxVar(RegistraturaMailBoxVar var) throws DbErrorException {
		try {
			boolean attached = getEntityManager().contains(var);
			if (!attached) {
				LOGGER.debug("ATTACH dettached object before DELETE: {}", var);
				var = JPA.getUtil().getEntityManager().find(RegistraturaMailBoxVar.class, var.getId());
			}
			JPA.getUtil().getEntityManager().remove(var);
		}catch (Exception e) {
			LOGGER.error("Грешка при изтриване на променлива на пощенска кутия !", e);
			throw new DbErrorException("Грешка при изтриване на променлива на пощенска кутия !", e);
		}
	}
	
	
	public void delete(RegistraturaMailBox mailBox) throws DbErrorException, ObjectInUseException {
		if (mailBox.getVariables() != null) {
			for (RegistraturaMailBoxVar var : mailBox.getVariables()) {
				if (var.getId() != null) {
					deleteMailBoxVar(var);
				}
			}
		}
		super.delete(mailBox);
	}
	
	
}
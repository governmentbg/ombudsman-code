package com.ib.omb.experimental;

import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.system.utils.SearchUtils.asString;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Query;
import javax.servlet.ServletContext;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.notifications.LoggedUser;
import com.ib.omb.notifications.PushBean;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.mail.Mailer;
import com.ib.system.utils.SearchUtils;

/**
 * Клас, който да се стартира периодично и да събира данни за използване от деловодителките
 * @author krasi
 *
 */
public class CheckNewData implements Job {
	 private static final Logger LOGGER = LoggerFactory.getLogger(CheckNewData.class);
	
//	 @Inject
// 	 GlobalHolder holder;
	 
//	 @Inject
//	 SystemData systemData;
	 
	 
	 @Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		LOGGER.info("CheckNewData execute START");
		try {
			ServletContext servletContext = (ServletContext) context.getScheduler().getContext().get("servletContext");
			if (servletContext==null) {
				LOGGER.debug("********** There is a problem !!!!! servletcontext is null **********");
				throw new Exception("servletcontext is nul!!!l");
			}
			
			GlobalHolder holder = (GlobalHolder) servletContext.getAttribute("globalDocHolder");
			SystemData systemData = (SystemData) servletContext.getAttribute("systemData");
			PushBean pushBean = null;
			if (systemData != null) {
				pushBean = systemData.getPushBean();
			}
			
			
			//PushBean pushBean = (PushBean) servletContext.getAttribute("pushBean");
			
			
			
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("======>" + holder);
				LOGGER.debug("======>" + systemData);
				LOGGER.debug("======>" + pushBean);
				LOGGER.debug("----------> pushBean.size = " + pushBean.getLoggedIn().size());
			}
			if (pushBean.getLoggedIn().size() > 0) {
				Set<Entry<Integer, LoggedUser>> entrySetLU = pushBean.getLoggedIn().entrySet();
				for (Entry<Integer, LoggedUser> entryLU : entrySetLU) {
					LoggedUser lu = entryLU.getValue();
//					LOGGER.debug("*** "  + lu.getUserId() + "|" + lu.getRegistratureId() + " ***");
					LOGGER.debug("*** {}|{} ***",lu.getUserId(),lu.getRegistratureId() );
				}
			}
			
			
		
		
			holder.clearChangeFlag();

			boolean result = false;
		
			result = fillEgovData(holder) || result ;
			result = fillDocData(holder) || result;
			result = fillMailData(holder, systemData) || result;
			
			if (result) {	
				
				Set<Entry<Integer, RegistratureDocHolder>> all = holder.getRegMap().entrySet();
				for (Entry<Integer, RegistratureDocHolder> entry : all) {
					if (entry.getValue().regHasChange) {
						
						Integer idReg = entry.getKey();
						LOGGER.debug("***** WORKING WITH idReg = {}", idReg);
						
						//Тук само за да видиш че се ползва стрийм ама не ме кефи ....
						List<LoggedUser> allFromReg = pushBean.getLoggedIn().entrySet().stream()
								  .filter(e -> Objects.equals(e.getValue().getRegistratureId(), idReg)) // може регистратурата на узера да е НУЛЛ
								  .map(Map.Entry::getValue)
								  .collect(Collectors.toList());
						
						LOGGER.debug("***** allFromReg.size = {}", allFromReg.size());
						
						
						ArrayList<Integer> delovoditeli = new ArrayList<Integer> ();
						for (LoggedUser lu : allFromReg) {							
							LOGGER.debug("***** Checking user with id = {}", lu.getUserId());							
							Integer flag = (Integer) systemData.getItemSpecific(OmbConstants.CODE_CLASSIF_USERS, lu.getUserId(), OmbConstants.CODE_DEFAULT_LANG, new Date(), OmbClassifAdapter.USERS_INDEX_DELOVODITEL);
							LOGGER.debug("**** flag = {}", flag);
							boolean isDelovoditel = flag != null && flag == OmbConstants.CODE_ZNACHENIE_DA;											
							if (isDelovoditel) {
								delovoditeli.add(lu.getUserId());									
							}
							
						}
						if (delovoditeli.size() > 0) {
							Notification notif = new Notification(systemData);
							notif.generatеFakeNotif(OmbConstants.CODE_FAKE_NOTIF_RELOAD_DELOVOD);
							notif.setAdresati(delovoditeli);
							notif.send();
						}
									
						
						
					}
				}
			}
			
		}catch (Exception e) {
			LOGGER.error("Грешка при обновяване на информация за работен плот!", e);
		}
		
		
		LOGGER.info("CheckNewData execute END");
		
	}
	
	
	
	
	@SuppressWarnings("unchecked")
	public boolean fillEgovData(GlobalHolder holder) throws DbErrorException {
		boolean result = false;
		
		try {
			
			String sql = "select REGISTRATURI.REGISTRATURA_ID, count(REGISTRATURI.REGISTRATURA_ID) cnt from EGOV_MESSAGES join REGISTRATURI on EGOV_MESSAGES.RECIPIENT_GUID = REGISTRATURI.GUID_SEOS\n" + 
							" where MSG_STATUS = 'DS_WAIT_REGISTRATION' and MSG_TYPE = 'MSG_DocumentRegistrationRequest'\n" + 
							" group by REGISTRATURI.REGISTRATURA_ID"; 
							
			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql);
			
			ArrayList<Object[]> rows = (ArrayList<Object[]>) query.getResultList();
			for (Object[] row : rows) {				
				Integer regId = SearchUtils.asInteger(row[0]);				
				Integer cnt = SearchUtils.asInteger(row[1]);
				LOGGER.debug("RESULT -------> {}", result);
				LOGGER.debug("СЕОС *************************** {} --> ",  regId, cnt);
				
				RegistratureDocHolder regHolder = holder.getRegInfo(regId);
				result = regHolder.setCounterSEOS(cnt) || result;
				holder.setRegInfo(regId, regHolder);
			}
			
			
			sql = "select REGISTRATURI.REGISTRATURA_ID, count(REGISTRATURI.REGISTRATURA_ID) cnt from EGOV_MESSAGES join REGISTRATURI on EGOV_MESSAGES.RECIPIENT_GUID = REGISTRATURI.GUID_SSEV\n" + 
					"where MSG_STATUS = 'DS_WAIT_REGISTRATION'\n" + 
					"group by REGISTRATURI.REGISTRATURA_ID";
			query = JPA.getUtil().getEntityManager().createNativeQuery(sql);
			
			rows = (ArrayList<Object[]>) query.getResultList();
			for (Object[] row : rows) {				
				Integer regId = SearchUtils.asInteger(row[0]);				
				Integer cnt = SearchUtils.asInteger(row[1]);
				LOGGER.debug("RESULT -------> {}", result);
				LOGGER.debug("ССЕВ *************************** {}  --> {}",  regId , cnt);
				
				RegistratureDocHolder regHolder = holder.getRegInfo(regId);
				result = regHolder.setCounterEDelivery(cnt) || result;
				holder.setRegInfo(regId, regHolder);
			}
			
			
			
			
		}catch (Exception e) {
			LOGGER.error("Грешка при четене в таблица EGOV_MESSAGES", e);
			throw new DbErrorException("Грешка при четене в таблица EGOV_MESSAGES", e);
		}
		
		return result;
		
	}
	
	
	@SuppressWarnings("unchecked")
	public boolean fillDocData(GlobalHolder holder) throws DbErrorException {
		
		boolean result = false;
		try {
			
			String sql = "select REGISTRATURA_ID, count(REGISTRATURA_ID) cnt  from doc where DOC_TYPE = 1 and PROCESSED = 2 group by REGISTRATURA_ID";
			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sql);
			
			ArrayList<Object[]> rows = (ArrayList<Object[]>) query.getResultList();
			for (Object[] row : rows) {				
				Integer regId = SearchUtils.asInteger(row[0]);				
				Integer cnt = SearchUtils.asInteger(row[1]);
				
				RegistratureDocHolder regHolder = holder.getRegInfo(regId);
				result = regHolder.setCounterNasochvane(cnt) || result;
				holder.setRegInfo(regId, regHolder);
			}
			
			sql = "select FOR_REG_ID, count(REGISTRATURA_ID) cnt  from doc where DOC_TYPE =  3 and WORK_OFF_ID is null and FOR_REG_ID is not null group by FOR_REG_ID";
			query = JPA.getUtil().getEntityManager().createNativeQuery(sql);
			
			rows = (ArrayList<Object[]>) query.getResultList();
			for (Object[] row : rows) {				
				Integer regId = SearchUtils.asInteger(row[0]);				
				Integer cnt = SearchUtils.asInteger(row[1]);
				
				RegistratureDocHolder regHolder = holder.getRegInfo(regId);
				result = regHolder.setCounterOfficial(cnt) || result;
				holder.setRegInfo(regId, regHolder);
			}
			
			sql = "select doc_dvij.FOR_REG_ID, count(doc_dvij.FOR_REG_ID) from doc_dvij join DOC on doc_dvij.DOC_ID = doc.DOC_ID where doc_dvij.FOR_REG_ID <> doc.REGISTRATURA_ID and doc_dvij.status = 3 group by doc_dvij.FOR_REG_ID";
			query = JPA.getUtil().getEntityManager().createNativeQuery(sql);
			
			rows = (ArrayList<Object[]>) query.getResultList();
			for (Object[] row : rows) {				
				Integer regId = SearchUtils.asInteger(row[0]);				
				Integer cnt = SearchUtils.asInteger(row[1]);
				
				RegistratureDocHolder regHolder = holder.getRegInfo(regId);
				result = regHolder.setCounterOtherReg(cnt) || result;
				holder.setRegInfo(regId, regHolder);
			}
			
			
			sql = "select REGISTRATURA_ID, count(REGISTRATURA_ID) cnt  from doc where COMPETENCE = "+OmbConstants.CODE_ZNACHENIE_COMPETENCE_FOR_SEND 	+" group by REGISTRATURA_ID";
			query = JPA.getUtil().getEntityManager().createNativeQuery(sql);
			
			rows = (ArrayList<Object[]>) query.getResultList();
			for (Object[] row : rows) {				
				Integer regId = SearchUtils.asInteger(row[0]);				
				Integer cnt = SearchUtils.asInteger(row[1]);
				
				RegistratureDocHolder regHolder = holder.getRegInfo(regId);
				result = regHolder.setCounterForCompetence(cnt) || result;
				holder.setRegInfo(regId, regHolder);
			}
			
			
			
		}catch (Exception e) {
			LOGGER.error("Грешка при четене в таблица DOC", e);
			throw new DbErrorException("Грешка при четене в таблица DOC", e);
		}
		
		return result;
		
	}
	
	
	
	public boolean fillMailData(GlobalHolder holder, SystemData systemData) throws DbErrorException {
		
		boolean result = false;
		Mailer mailer3 = new Mailer();
		try {
			LOGGER.debug("***************** Start fillMailData ***************************");
			
			
			for (Object[] mailbox : systemData.getMailboxes()) {
				
				Integer regId = asInteger(mailbox[0]);
				String mailBoxName = asString(mailbox[1]);
				String userName = asString(mailbox[2]);
				String passord = asString(mailbox[3]);
					
				LOGGER.debug("******** regId= {}", regId);
				LOGGER.debug("******** mailBoxName= {}" , mailBoxName);
				
				RegistratureDocHolder regHolder = holder.getRegInfo(regId);
				
				Properties prop = systemData.getMailProp(regId, mailBoxName);
				if (userName != null && !userName.trim().isEmpty()) {
					prop.put("user.name", userName);
				}
				if (passord != null && !passord.trim().isEmpty()) {
					prop.put("user.password", passord);
				}
				
				
				Integer cnt = -2;
				try {
					LOGGER.debug("Get messages from mail with props:");
					for (Object key: prop.keySet()) {
						LOGGER.debug("key={} , valye={}",key,prop.getProperty(key.toString()));
			        }
					cnt = mailer3.getMessageCount(prop, prop.getProperty("user.name"), prop.getProperty("user.password"), "INBOX", false);
				}catch (Exception e) {
					LOGGER.error("Грешка при извличане на поща от кутия с име {}", mailBoxName, e);
				}
				LOGGER.debug("******** Count= {} Before --> {}", cnt , result);
				result = regHolder.setCounterEmails(mailBoxName, cnt) || result;
				LOGGER.debug("******** Count= {}  After --> {}", cnt , result);
			}
			
			
		}catch (Exception e) {
			LOGGER.error("Грешка при извличане на пощенски кутии !!!!", e);
			throw new DbErrorException("Грешка при извличане на пощенски кутии !!!!", e);
		}
		
		LOGGER.debug("***************** End fillMailData ***************************");
		return result;
	}
	
	
	

}

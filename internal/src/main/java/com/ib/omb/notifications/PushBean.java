package com.ib.omb.notifications;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.inject.Named;

import org.omnifaces.cdi.Eager;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;
//import org.omnifaces.cdi.Push;
//import org.omnifaces.cdi.PushContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.UserNotificationsDAO;
import com.ib.omb.db.dto.UserNotifications;
import com.ib.omb.experimental.Notification;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SearchUtils;

/**Клас, отговорен за същинското изпращане и записване на всички съобщения
 * @author krasi
 *
 */
@Named
@Eager
@ApplicationScoped
public class PushBean {
	

	private static final Logger LOGGER = LoggerFactory.getLogger(PushBean.class);

	@Inject
	@Push
    private PushContext kgLoggedUsers;
	
	@Inject
	@Push
    private PushContext kgMainMessages;
	
	
	
	
	/**
	 * Списък с кодове на деловодител
	 */
	List<Integer> deloNotifList = Arrays.asList(new Integer[]{9,14,15,39});
	
	/**
	 * Логнатите потребители. Попълва се от {@link SocketObserver}
	 */
	private Map<Integer,LoggedUser> loggedIn=new HashMap<Integer,LoggedUser>();
	
	
	public PushBean() {
		super();
		LOGGER.debug("PushBean constructor");
	}


	public Map<Integer,LoggedUser> getLoggedIn() {
		return loggedIn;
	}


	public void setLoggedIn(Map<Integer,LoggedUser> loggedIn) {
		LOGGER.debug("setLoggedIn");
		this.loggedIn = loggedIn;
	}
	public void addToLoggedIn(Integer userId) {
		LOGGER.debug("addTo loggedIn:{}",userId);
		loggedIn.put(userId, new LoggedUser(userId, null));
		
		printLoggedInUsers();
	}


	private void printLoggedInUsers() {
		LOGGER.debug("=================== LoggedUSers =================");
		for (Map.Entry<Integer, LoggedUser> entry : loggedIn.entrySet()) {
		    LOGGER.debug("{1} / {2}",entry.getKey() ,entry.getValue().getUserId());
		}
		LOGGER.debug("=================== =========== =================");
	}
	public void removeFromLoggedIn(Integer userId) {
		LOGGER.debug("remowe from loggedIn");
		if (loggedIn.containsKey(userId)) {
			loggedIn.remove(userId);
		}
		
		printLoggedInUsers();
	}


	public void sendMsgTo(Integer userId, String message) {
		LOGGER.debug("sendMsgTo:userId={} ,message={}",userId,message);
		//Set<Future<Void>> send;
		try {
			if (userId!=null) {
				/* send = */kgLoggedUsers.send(message, userId);
			}else {
				/* send= */kgLoggedUsers.send(message);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOGGER.error("",e);
		}
	}
	
	public void setUserRegistrature(Integer userId,Integer registratureId) {
		LOGGER.debug("setUserRegistrature:userId={} ,registratureId={}",userId,registratureId);
		LoggedUser lu = new LoggedUser(userId, registratureId);
		loggedIn.put(userId, lu);
	}
	
	public void refreshUsers() {
		LOGGER.debug("refreshUsers");
		kgLoggedUsers.send("updateUsers",getLoggedIn().keySet());
	}
	
	
	/**Праща нотификации към екрана
	 * @param msg
	 */
	public void sendMainMessage(@ObservesAsync UMsgHolder msg) {
		LOGGER.debug("sendMainMessage:{}",msg);
		try {
			if (msg.getUsers().size()==1 &&  msg.getUsers().get(0).equals(0)) {
				printLoggedInUsers();
				/* Map<Integer, Set<Future<Void>>> send = */kgMainMessages.send(msg.getMessage(),getLoggedIn().keySet());
			}else {
				/* Map<Integer, Set<Future<Void>>> send = */kgMainMessages.send(msg.getMessage(),msg.getUsers());
			}
//			 Set<Future<Void>> send2 = kgMainMessages.send(msg.getMessage(),"-1");
//			 Map<Integer, Set<Future<Void>>> send3 = kgMainMessages.send("тест3",getLoggedIn().keySet());
//			 
//			 
//			 Map<Integer, Set<Future<Void>>> send4 = kgLoggedUsers2.send(msg.getMessage(),getLoggedIn().keySet());
//			 
//			 kgMainMessages.send("тест1",-1);
//			 kgMainMessages.send("тест2","-1");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOGGER.error("",e);
		}
	}

	
	/**Записва нотификация в базата
	 * @param msg
	 */
	public void saveMainMessage(@ObservesAsync UMsgHolder msg) {
		LOGGER.debug("saveMainMessage");
		

		if(msg==null) {
			LOGGER.error("Something strange happened!!! msg is null.");
			return;
		}
		
		if ( msg.getMessage() != null && msg.getMessage().getMessageType() != null && msg.getMessage().getMessageType().intValue() == -1) {
			return;
		}
		
		
		try {
			UserNotificationsDAO dao = new UserNotificationsDAO();
			List<Integer> users = msg.getUsers();
			
			for (Integer userId : users) {
				
							
					JPA.getUtil().begin();
					
					//JPA.getUtil().getEntityManager().persist(mess);
					
					
					msg.getMessage().setIdUser(userId);
					dao.save(msg.getMessage());
					
					JPA.getUtil().commit();
			}
		} catch (DbErrorException e) {
			LOGGER.error("Error saving user message",e);
		}
	}
	
	
	/**Праща нотификации към екрана
	 * @param msg
	 */
	public void processNotificationAsync(@ObservesAsync Notification nb) {
		
		
		
		
		
		
		Set<Integer> visibleList = new TreeSet<Integer>();
		Set<Integer> invisibleList = new TreeSet<Integer>();
		Set<Integer> mailList = new TreeSet<Integer>();
		
		try {
			
			if (nb.getNotificationCode() != null ) {
				//Проверка дали нотификацията е активна
				if (!nb.getSd().matchClassifItems(OmbConstants.CODE_CLASSIF_NOTIFF_EVENTS_ACTIVE, nb.getNotificationCode(), new Date())) {
					return;
				}
	
				if (nb.getuMessage() == null) {
					nb.generateNotif();
				}
				
				LOGGER.debug("getuMessage:{} " , nb.getuMessage());
				LOGGER.debug("Брой: {}", nb.getAdresati().size());
				
				ArrayList<Integer> ignoreList = new ArrayList<Integer>();
				if (nb.getDoc() != null && nb.getDoc().getId() != null) {
					DocDAO dao = new DocDAO(ActiveUser.DEFAULT);
					ignoreList = dao.getDocIgnoreList(nb.getDoc().getId());
				}
				
				for (Integer referent : nb.getAdresati()) {
										
					if (! nb.getSd().matchClassifItems(OmbConstants.CODE_CLASSIF_USERS, referent, new Date())) {
						//референтът не е потребител - ако е за запознаване и е до структура - разбиваме
						mailList.add(referent);
						if (nb.getNotificationCode() != null && nb.getNotificationCode().equals(OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_ACCESS) ) {
							
							List<SystemClassif> children = nb.getSd().getChildrenOnNextLevel(OmbConstants.CODE_CLASSIF_ADMIN_STR , referent, new Date(), OmbConstants.CODE_DEFAULT_LANG);
							if (children != null) {
								
								
								
								for (SystemClassif item : children) {
									
									if (ignoreList.contains(item.getCode())) {
										continue;
									}
									
									if (nb.getSd().checkUserNotifSettings(item.getCode(), nb.getNotificationCode())) {
										visibleList.add(item.getCode());
									}else {
										invisibleList.add(item.getCode());
									}
								}
							}
						}
						
					}else {
						//референтът е потребител						
						if (nb.getSd().checkUserNotifSettings(referent, nb.getNotificationCode())) {
							visibleList.add(referent);
						}else {
							invisibleList.add(referent);
						}
						
					}
				}
			}else {
				visibleList.addAll(nb.getAdresati());
			}
			
			//-------------------------------------------
			
			if (! deloNotifList.contains(nb.getNotificationCode()) && nb.getUserId() != null && visibleList.contains(nb.getUserId())) {
				
					visibleList.remove(nb.getUserId());
					invisibleList.add(nb.getUserId());
				
			}
			
			
			
			
			LOGGER.debug("VisibleList.size: {}", visibleList.size());
			if (!visibleList.isEmpty()) {
				if (nb.getNotificationCode()!= null) {
					nb.getuMessage().setMessageType(nb.getNotificationCode());
				}
				saveMainMessage(nb.getuMessage(), visibleList, nb.getIdRegistratura(), nb.getSd());
				sendMainMessage(nb.getuMessage(), visibleList);
			}
			
			LOGGER.debug("InvisibleList.size:{} ", invisibleList.size());
			if (!invisibleList.isEmpty()) {
				if (nb.getNotificationCode()!= null) {					
					nb.getuMessage().setMessageType(-nb.getNotificationCode());
				}				
				saveMainMessage(nb.getuMessage(), invisibleList, nb.getIdRegistratura(), nb.getSd());
				sendMainMessage(nb.getuMessage(), invisibleList);
			}
			
			
			LOGGER.debug("mailList.size:{} ", mailList.size());
			if (!mailList.isEmpty()) {
				
				saveEmailMessage(nb.getuMessage(), mailList, nb.getSd());
			}
			
			
			
		} catch (DbErrorException e) {
			LOGGER.error("Error saving user message",e);
		}
		
		
		
		//
		
		
	}
	
	
	
	/**Праща нотификации към екрана
	 * @param msg
	 */
	public void sendMainMessage(UserNotifications msg, Set<Integer> users) {
		LOGGER.debug("sendMainMessage:{}",msg);
		try {
			if (users.size()==1 &&  users.iterator().next().equals(0)) {
				LOGGER.debug("=================== LoggedUSers =================");
				for (Map.Entry<Integer, LoggedUser> entry : loggedIn.entrySet()) {
				    LOGGER.debug("{0} / {1}", entry.getKey() ,entry.getValue());
				}
				LOGGER.debug("=================== =========== =================");
				/* Map<Integer, Set<Future<Void>>> send = */ kgMainMessages.send(msg,getLoggedIn().keySet());
			}else {
				/* Map<Integer, Set<Future<Void>>> send = */kgMainMessages.send(msg,users);
			}
//			 Set<Future<Void>> send2 = kgMainMessages.send(msg.getMessage(),"-1");
//			 Map<Integer, Set<Future<Void>>> send3 = kgMainMessages.send("тест3",getLoggedIn().keySet());
//			 
//			 
//			 Map<Integer, Set<Future<Void>>> send4 = kgLoggedUsers2.send(msg.getMessage(),getLoggedIn().keySet());
//			 
//			 kgMainMessages.send("тест1",-1);
//			 kgMainMessages.send("тест2","-1");
		} catch (Exception e) {
			LOGGER.error("",e);
		}
	}

	
	/**Записва нотификация в базата
	 * @param msg
	 */
	public void saveMainMessage(UserNotifications msg, Set<Integer> users, Integer registraturaId, SystemData sd) {
		LOGGER.debug("saveMainMessage");
		
		if(msg==null) {
			LOGGER.error("Something strange happened!!! msg is null.");
			return;
		}
		
		if (msg.getMessageType() != null) {
				
				if (msg.getMessageType().intValue() == - OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DOC_ACCESS
					|| msg.getMessageType().intValue() == - OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_CONTROL_PROC
					|| msg.getMessageType().intValue() == - OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_CONTROL_ETAP
					|| msg.getMessageType().intValue() == - OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_DECISION_PROC
					|| msg.getMessageType().intValue() == - OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_TASK_PROC
					|| msg.getMessageType().intValue() == - OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_PROC_STOP) {
					//Изрично го правим + за справките и го записваме
					msg.setMessageType(msg.getMessageType() * -1);
				}
			
				if (msg.getMessageType().intValue() < 0) {
					return;
				}
			
		}
		
		if (users == null || users.isEmpty()) {
			return;
		}
		
		try {
			UserNotificationsDAO dao = new UserNotificationsDAO();
			
			
			for (Integer userId : users) {
				
					String email = null;
					
					UserNotifications curMsg = msg.cloneAsNew();
					
					SystemClassif empl = sd.decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, userId, SysConstants.CODE_DEFAULT_LANG, new Date(), true);
					if (empl != null) {
						email = SearchUtils.asString( empl.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_CONTACT_EMAIL]);
					}
					
					SystemClassif user = sd.decodeItemLite(Constants.CODE_CLASSIF_USERS, userId, SysConstants.CODE_DEFAULT_LANG, new Date(), true);
					if (user != null) {
						Integer dubl = SearchUtils.asInteger( user.getSpecifics()[OmbClassifAdapter.USERS_INDEX_DUBL_MAIL]);
						if (dubl != null && email != null && dubl.equals(OmbConstants.CODE_ZNACHENIE_DA)) {
							curMsg.setEmailTo(email);
							curMsg.setSentToEmail(OmbConstants.CODE_ZNACHENIE_STATUS_MAIL_NOT_SEND);
						}
					}
					
					JPA.getUtil().begin();
					
					//JPA.getUtil().getEntityManager().persist(mess);
					
					
					curMsg.setIdUser(userId);
					if (registraturaId != null) {
						curMsg.setRegistraturaId(registraturaId);
					}else {
							
						}
					dao.save(curMsg);
					
					JPA.getUtil().commit();

//					if ("1".equals(sd.getSettingsValue("delo.mobileAvailable"))) { // може да се изведе като булева за да не се рови все в H2-ката
//						FirebaseClient.sendNotifCount(userId, 1, null);
//					}
			}
		} catch (Exception e) {
			JPA.getUtil().rollback();
			LOGGER.error("Error saving user message",e);
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
	
	
	
	
	/**Записва нотификация в базата
	 * @param msg
	 */
	public void saveEmailMessage(UserNotifications msg, Set<Integer> slujit, SystemData sd) {
		LOGGER.debug("saveEmailMessage");
		
		if(msg==null) {
			LOGGER.error("Something strange happened!!! msg is null.");
			return;
		}
		
		if (slujit == null || slujit.isEmpty()) {
			return;
		}
		
		try {
			UserNotificationsDAO dao = new UserNotificationsDAO();
			
			
			for (Integer slujitId : slujit) {
				
					SystemClassif empl = sd.decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, slujitId, SysConstants.CODE_DEFAULT_LANG, new Date(), true);
					if (empl == null) {
						LOGGER.debug("Служител с код " + slujitId + " не е намерен към " + new Date());
						continue;
					}
				
					String email = SearchUtils.asString( empl.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_CONTACT_EMAIL]);
					if (email == null) {
						continue;
					}
					
					UserNotifications curMsg = msg.cloneAsNew();
					curMsg.setEmailTo(email);
					curMsg.setSentToEmail(OmbConstants.CODE_ZNACHENIE_STATUS_MAIL_NOT_SEND);
					JPA.getUtil().begin();
					
					dao.save(curMsg);
					
					JPA.getUtil().commit();
			}
		} catch (Exception e) {
			JPA.getUtil().rollback();
			LOGGER.error("Error saving user message",e);
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
	
	
		
	
	
	
	
}

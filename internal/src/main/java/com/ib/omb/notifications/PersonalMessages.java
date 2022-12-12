package com.ib.omb.notifications;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.omb.db.dao.UserNotificationsDAO;
import com.ib.omb.db.dto.UserNotifications;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;

@Named("pMessages")
@ViewScoped
public class PersonalMessages extends IndexUIbean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8982132289750610070L;

	private static final Logger LOGGER = LoggerFactory.getLogger(PersonalMessages.class);
	private Integer brNotif = 0;
	private List<UserNotifications> listNotif = new ArrayList<UserNotifications>();

	private HashMap<Long, Boolean> selectedNotif;
	
	private boolean selectAllPM;
	
	/** налично ли е мобилно приложение за системата. през системна настройка */
	private boolean mobileAvailable; // ако има мобилно при премахване на нотидикация трябва да се уведомява

	@PostConstruct
	void initData() {

		try {

			UserNotificationsDAO dao = new UserNotificationsDAO();

			setBrNotif(dao.countUserNotifications(getUserData(UserData.class).getUserAccess(), OmbConstants.CODE_NOTIF_STATUS_NEPROCHETENA, getUserData(UserData.class).getRegistratura()));

			this.mobileAvailable = false; // "1".equals(getSystemData().getSettingsValue("delo.mobileAvailable"));

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	public void actionLoadMessages() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("actionLoadMessages");
		}
		
		try {

			UserNotificationsDAO dao = new UserNotificationsDAO();

			ArrayList<UserNotifications> messages = dao.findUserNotifications(getUserData(UserData.class).getUserAccess(),OmbConstants.CODE_NOTIF_STATUS_NEPROCHETENA, getUserData(UserData.class).getRegistratura());

			setListNotif(messages);
			setBrNotif(messages.size());

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	public void actionMarkAsRed(Integer notifId) {
		LOGGER.info("actionMarkAsRed:notifId=" + notifId);
		try {
			UserNotificationsDAO dao = new UserNotificationsDAO();
			JPA.getUtil().begin();
			dao.changeStatusMessage(notifId, 1);

			JPA.getUtil().commit();
			setBrNotif(getBrNotif() - 1);

			if (this.mobileAvailable) {
				FirebaseClient.sendNotifCount(((UserData)getUserData()).getUserAccess(), null, getBrNotif());
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	public void actionMarkAsRedDashboard(Integer notifId) {
		LOGGER.info("actionMarkAsRed:notifId=" + notifId);
		try {
			UserNotificationsDAO dao = new UserNotificationsDAO();
			JPA.getUtil().begin();
			dao.changeStatusMessage(notifId, 1);

			JPA.getUtil().commit();
			setBrNotif(getBrNotif()-1);

			if (this.mobileAvailable) {
				FirebaseClient.sendNotifCount(((UserData)getUserData()).getUserAccess(), null, getBrNotif());
			}

			// za da prezaredq spisyka
			// actionLoadMessages();
			for (UserNotifications um : listNotif) {

				if (um.getId().equals(notifId)) {
					listNotif.remove(um);
					break;
				}
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
	
	public void actionMarkAsRedAllDashboard() {
		LOGGER.info("actionMarkAsRedAllDashboard");
		try {
			UserNotificationsDAO dao = new UserNotificationsDAO();
			JPA.getUtil().begin();
			dao.changeStatusMessagesUser(getUserData(UserData.class).getUserAccess(), 1);

			JPA.getUtil().commit();
			setBrNotif(0);

			if (this.mobileAvailable) {
				FirebaseClient.sendNotifCount(((UserData)getUserData()).getUserAccess(), null, getBrNotif());
			}

			// za da prezaredq spisyka
			// actionLoadMessages();
			listNotif.clear();

		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
	
	public void actionMarkAsRedSelectedDashboard() {
		
		LOGGER.info("actionMarkAsRedSelectedDashboard");
		
		List <Integer> idMessages = new ArrayList<>();
		for(Long idKey : selectedNotif.keySet()) {
			
			if(selectedNotif.get(idKey)) {
				idMessages.add(idKey.intValue());
			}
		}
		
		if(idMessages.size()>0) {
			try {
				UserNotificationsDAO dao = new UserNotificationsDAO();
				JPA.getUtil().begin();
				dao.changeStatusMessages(idMessages, 1);
	
				JPA.getUtil().commit();
				
				int downSize = idMessages.size();
				setBrNotif(getBrNotif()-downSize);
	
				if (this.mobileAvailable) {
					FirebaseClient.sendNotifCount(((UserData)getUserData()).getUserAccess(), null, getBrNotif());
				}

				// za da prezaredq spisyka
				// actionLoadMessages();
				List <UserNotifications> uml = new ArrayList<>();
				for (UserNotifications um : listNotif) {
					if(idMessages.size()==0) {
						break;
					}
					if (idMessages.contains(um.getId())) {
						uml.add(um);
						idMessages.remove(um.getId());
					}
				}
				
				for (UserNotifications umtmp : uml) {
					listNotif.remove(umtmp);
				}
				//
				selectedNotif.clear();
				
				PrimeFaces.current().executeScript("downNumberCustom("+downSize+")");
				
				
			} catch (Exception e) {
				LOGGER.error(e.getMessage());
			} finally {
				JPA.getUtil().closeConnection();
			}
		
		}
	}
	

	public Integer getBrNotif() {
		return brNotif;
	}

	public void setBrNotif(Integer brNotif) {
		this.brNotif = brNotif;
	}

	public List<UserNotifications> getListNotif() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("getListNotif");

		}
		return listNotif;
	}

	public void setListNotif(List<UserNotifications> listNotif) {
		this.listNotif = listNotif;
	}
	
	public String actionGotoTask(Integer idTask ,Integer idDoc) {
		StringBuilder linkB = new StringBuilder();
		
		linkB.append("taskEdit.jsf?faces-redirect=true&idObj=");
		linkB.append(idTask);
		
		if(idDoc!=null) {
			linkB.append("&idDoc=");
			linkB.append(idDoc);
		}
		
		return linkB.toString();
	}
	
	public String actionGoto(UserNotifications uNotif) {
		
		if(uNotif.getCodeObject() == OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE) {
		
			StringBuilder linkB = new StringBuilder();
			
			linkB.append("procExeEdit.xhtml?faces-redirect=true&idObj=");
			linkB.append(uNotif.getIdObject());
			
			
			
			return linkB.toString();
		} else {
			return actionGotoTask(uNotif.getTaskId() ,uNotif.getTaskDocId());
		}
	}

	public HashMap<Long, Boolean> getSelectedNotif() {
		if(selectedNotif==null) {
			selectedNotif = new HashMap<Long, Boolean>();
		}
		return selectedNotif;
	}

	public void setSelectedNotif(HashMap<Long, Boolean> selectedNotif) {
		this.selectedNotif = selectedNotif;
	}
	
	//Това се налага защото jsf HashMap не работи с integer
	public Long castLong(Integer i) {
		
		if(i == null) return null;
		
		return Long.valueOf(i); 
		
	}

	public boolean isSelectAllPM() {
		return selectAllPM;
	}

	public void setSelectAllPM(boolean selectAllPM) {
		this.selectAllPM = selectAllPM;
	}
	
	public void actionSelectAll() {
		
		for(Long idKey : selectedNotif.keySet()) {
			
			//selectedNotif.put(idKey, Boolean.valueOf(selectAllPM));
			selectedNotif.replace(idKey, Boolean.valueOf(selectAllPM));
			
		}
		
	}
}

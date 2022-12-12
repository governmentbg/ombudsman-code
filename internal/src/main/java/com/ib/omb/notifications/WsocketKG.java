package com.ib.omb.notifications;

import javax.enterprise.event.Event;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

@Named
@ViewScoped
public class WsocketKG implements Serializable {
	private static final Logger LOGGER = LoggerFactory.getLogger(WsocketKG.class);
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -491200834572504834L;
	private Integer userId;
	private String title;
	private String message;
	private String severity="info";
	
	@Inject
	PushBean pushBean;
	
	@Inject
	Event<UMsgHolder> msgEvent;
	public void actionSendMsg() {
		LOGGER.info("actionSendMsg:userId="+userId+" ,message="+message);
		pushBean.sendMsgTo(userId,message);
	}
	
	public void actionSendMsgMain() {
		LOGGER.info("actionSendMsg[START]:userId="+userId+" ,message="+message);
		UMsgHolder msg=new UMsgHolder(title, message, severity, userId);
		msgEvent.fireAsync(msg);
//		pushBean.sendMsgTo(userId,message);
		LOGGER.info("actionSendMsg[END]");
	}
	
	
	
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		LOGGER.info("setUserId");
		this.userId = userId;
	}
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		
		return message;
	}
	public void setMessage(String message) {
		LOGGER.info("setMessage");
		this.message = message;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}
	
	
	
	
}

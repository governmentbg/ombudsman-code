package com.ib.omb.db.dto;

import static javax.persistence.GenerationType.SEQUENCE;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.ib.omb.system.OmbConstants;

/**
 * за Васил: искам една таблица USER_NOTIFICATIONS (id,id_user, id_message, title, details, severity, isReaded) + date
 *със съответните методи за работа
 *
 * @author Васил ;)
 */
@Entity
@Table(name = "USER_NOTIFICATIONS")
public class UserNotifications implements Serializable {
	
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 8701345563309737046L;

	@SequenceGenerator(name = "UserM", sequenceName = "SEQ_USER_MESSAGES", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "UserM")
	@Column(name = "ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "USER_ID")
	private Integer idUser ;

	@Column(name = "TITLE")
	private String title;

	@Column(name = "DETAILS")
	private String details;

	@Column(name = "SEVERITY")
	private String severity = "info";

	@Column(name = "READ")
	private Integer read = OmbConstants.CODE_NOTIF_STATUS_NEPROCHETENA;

	@Column(name = "DATE_MESSAGE")
	private Date dateMessage=new Date();

	@Column(name = "TASK_ID")
	private Integer taskId;
	
	@Column(name = "OBJECT_CODE")
	private Integer codeObject;
	
	@Column(name = "OBJECT_ID")
	private Integer idObject;
	
	@Column(name = "EMAIL_TO")
	private String emailTo;	
	
	@Column(name = "SENT_TO_EMAIL")
	private Integer sentToEmail;
	
	@Column(name = "MESSAGE_TYPE")
	private Integer messageType;
	
	@Column(name = "REGISTRATURA_ID")
	private Integer registraturaId;
	
	@Transient
	private Integer taskStatus;
	
	@Transient
	private Integer taskDocId;
	
	

	/**  */
	public UserNotifications() {
		super();		
	}

	public UserNotifications(Integer id, String title, String details, String severity) {
		super();
		this.id = id;
		this.title = title;
		this.details = details;
		this.severity = severity;
	}
	

	public Integer getId() {
		return id;
	}



	public void setId(Integer id) {
		this.id = id;
	}



	public Integer getIdUser() {
		return idUser;
	}



	public void setIdUser(Integer idUser) {
		this.idUser = idUser;
	}



	public String getTitle() {
		return title;
	}



	public void setTitle(String title) {
		this.title = title;
	}



	public String getDetails() {
		return details;
	}



	public void setDetails(String details) {
		this.details = details;
	}



	public String getSeverity() {
		return severity;
	}



	public void setSeverity(String severity) {
		this.severity = severity;
	}



	public Integer getRead() {
		return read;
	}



	public void setRead(Integer read) {
		this.read = read;
	}



	public Date getDateMessage() {
		return dateMessage;
	}



	public void setDateMessage(Date dateMessage) {
		this.dateMessage = dateMessage;
	}



	public Integer getTaskId() {
		return taskId;
	}



	public void setTaskId(Integer taskId) {
		this.taskId = taskId;
	}



	public Integer getCodeObject() {
		return codeObject;
	}



	public void setCodeObject(Integer codeObject) {
		this.codeObject = codeObject;
	}



	public Integer getIdObject() {
		return idObject;
	}



	public void setIdObject(Integer idObject) {
		this.idObject = idObject;
	}



	public String getEmailTo() {
		return emailTo;
	}



	public void setEmailTo(String emailTo) {
		this.emailTo = emailTo;
	}



	public Integer getSentToEmail() {
		return sentToEmail;
	}



	public void setSentToEmail(Integer sentToEmail) {
		this.sentToEmail = sentToEmail;
	}


	public Integer getMessageType() {
		return messageType;
	}



	public void setMessageType(Integer messageType) {
		this.messageType = messageType;
	}

	public Integer getRegistraturaId() {
		return registraturaId;
	}

	public void setRegistraturaId(Integer registraturaId) {
		this.registraturaId = registraturaId;
	}

	public Integer getTaskStatus() {
		return taskStatus;
	}

	public void setTaskStatus(Integer taskStatus) {
		this.taskStatus = taskStatus;
	}

	public Integer getTaskDocId() {
		return taskDocId;
	}

	public void setTaskDocId(Integer taskDocId) {
		this.taskDocId = taskDocId;
	}
	
	
	public UserNotifications cloneAsNew() {
		UserNotifications newN = new UserNotifications();
		newN.setCodeObject(codeObject);
		newN.setDateMessage(dateMessage);
		newN.setDetails(details);
		newN.setEmailTo(emailTo);
		newN.setIdObject(idObject);
		newN.setMessageType(messageType);
		newN.setIdUser(idUser);
		newN.setRead(read);
		newN.setRegistraturaId(registraturaId);
		newN.setSentToEmail(sentToEmail);
		newN.setSeverity(severity);
		newN.setTaskDocId(taskDocId);
		newN.setTaskId(taskId);
		newN.setTaskStatus(taskStatus);
		newN.setTitle(title);
		
		return newN;
	}
	
	
}
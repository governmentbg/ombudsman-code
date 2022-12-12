package com.ib.omb.db.dto;


import static javax.persistence.GenerationType.SEQUENCE;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

import com.ib.omb.system.OmbConstants;
import com.ib.system.db.PersistentEntity;

@Entity
@Table(name = "NOTIFICATION_PATTERNS")
public class NotificationPatterns implements PersistentEntity {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4344957157529487926L;

	@SequenceGenerator(name = "NotificationPatterns", sequenceName = "SEQ_MAIL_PATTERNS", allocationSize = 1)
	@Id
	@GeneratedValue(strategy = SEQUENCE, generator = "NotificationPatterns")
	@Column(name = "PATTERN_ID", unique = true, nullable = false)
	private Integer id;

	@Column(name = "EVENT_ID")
	private Integer eventId;
	
	@Column(name = "ROLIA")
	private Integer rolia;
	
	@Column(name = "SUBJECT")
	private String subject;
	
	@Column(name = "BODY")
	private String body;
	
	@OneToMany(mappedBy = "pattern", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	private List<NotificationPatternVariables> variables = new ArrayList<NotificationPatternVariables>();
	

	public List<NotificationPatternVariables> getVariables() {
		return variables;
	}

	public void setVariables(List<NotificationPatternVariables> variables) {
		this.variables = variables;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getEventId() {
		return eventId;
	}

	public void setEventId(Integer eventId) {
		this.eventId = eventId;
	}

	public Integer getRolia() {
		return rolia;
	}

	public void setRolia(Integer rolia) {
		this.rolia = rolia;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	@Override
	public Integer getCodeMainObject() {		
		return OmbConstants.CODE_ZNACHENIE_JOURNAL_NOTIF_PATTERN;
	}
	
	

	
	
	
		
}
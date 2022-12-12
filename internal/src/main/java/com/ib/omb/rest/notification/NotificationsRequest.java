package com.ib.omb.rest.notification;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NotificationsRequest {
	
	private String jwtToken;

	public NotificationsRequest() { }

	public NotificationsRequest(String jwtToken) {
		this.jwtToken = jwtToken;
	}

	public String getJwtToken() {
		return jwtToken;
	}

	public void setJwtToken(String jwtToken) {
		this.jwtToken = jwtToken;
	}

}

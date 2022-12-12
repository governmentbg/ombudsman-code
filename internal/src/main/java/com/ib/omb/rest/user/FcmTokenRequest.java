package com.ib.omb.rest.user;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FcmTokenRequest implements Serializable {
	
	private static final long serialVersionUID = -2671653206068278726L;
	
	private String jwtToken;
	private String fcmDeviceToken;
	
	public FcmTokenRequest() {
		
	}
	
	public FcmTokenRequest(String jwtToken, String fcmDeviceToken) {
		this.jwtToken = jwtToken;
		this.fcmDeviceToken = fcmDeviceToken;
	}

	public String getJwtToken() {
		return jwtToken;
	}

	public void setJwtToken(String jwtToken) {
		this.jwtToken = jwtToken;
	}

	public String getFcmDeviceToken() {
		return fcmDeviceToken;
	}

	public void setFcmDeviceToken(String fcmDeviceToken) {
		this.fcmDeviceToken = fcmDeviceToken;
	}
	
}

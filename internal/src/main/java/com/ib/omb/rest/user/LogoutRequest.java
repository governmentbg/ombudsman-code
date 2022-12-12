package com.ib.omb.rest.user;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class is used for logout requests
 * @author ilukov
 */
@XmlRootElement
public class LogoutRequest {
    
	private String jwtToken;
    private String deviceToken;

    public LogoutRequest() {
    }

    public LogoutRequest(String jwtToken, String deviceToken) {
        this.jwtToken = jwtToken;
        this.deviceToken = jwtToken;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

	public String getDeviceToken() {
		return deviceToken;
	}

	public void setDeviceToken(String deviceToken) {
		this.deviceToken = deviceToken;
	}
    
}

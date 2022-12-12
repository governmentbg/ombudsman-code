package com.ib.omb.rest.user;

import javax.xml.bind.annotation.XmlRootElement;

import com.ib.omb.rest.DocuWorkRestResponse;

import java.io.Serializable;

/**
 * Class is used as wrapper for response of login request
 * @author ilukov
 */
@XmlRootElement
public class LoginResponse extends DocuWorkRestResponse implements Serializable {
    private String jwtToken;
    private Integer userId;
    private Integer notificationCount;

    public LoginResponse(){}

    public LoginResponse(DocuWorkRestResponse.STATUS status, String jwtToken, String message, Integer userId, Integer notificationCount) {
        super(status, message);
        this.jwtToken = jwtToken;
        this.userId = userId;
        this.notificationCount = notificationCount;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getNotificationCount() {
		return notificationCount;
	}

	public void setNotificationCount(Integer notificationCount) {
		this.notificationCount = notificationCount;
	}
    
}

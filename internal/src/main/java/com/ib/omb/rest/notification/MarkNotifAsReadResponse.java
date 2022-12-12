package com.ib.omb.rest.notification;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.ib.omb.rest.DocuWorkRestResponse;

public class MarkNotifAsReadResponse extends DocuWorkRestResponse {
	
	private String jwtToken;
	
	public MarkNotifAsReadResponse() { }

	public MarkNotifAsReadResponse(DocuWorkRestResponse.STATUS status, String jwtToken, String message) {
        super(status, message);
        this.jwtToken = jwtToken;
    }

	public String getJwtToken() {
		return jwtToken;
	}

	public void setJwtToken(String jwtToken) {
		this.jwtToken = jwtToken;
	}

}

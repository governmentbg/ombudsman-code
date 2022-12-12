package com.ib.omb.rest.notification;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import com.ib.omb.rest.DocuWorkRestResponse;

public class NotificationsResponse extends DocuWorkRestResponse {

	private String jwtToken;
	private ArrayList<LinkedHashMap<String,Object>> notificationList;
	
	public NotificationsResponse() { }
	
	public NotificationsResponse(DocuWorkRestResponse.STATUS status, String jwtToken, String message, ArrayList<LinkedHashMap<String,Object>> notificationList) {
        super(status, message);
        this.jwtToken = jwtToken;
        this.notificationList = notificationList;
    }
	
	 public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

	public ArrayList<LinkedHashMap<String, Object>> getNotificationList() {
		return notificationList;
	}

	public void setNotificationList(ArrayList<LinkedHashMap<String, Object>> notificationList) {
		this.notificationList = notificationList;
	}
    
}

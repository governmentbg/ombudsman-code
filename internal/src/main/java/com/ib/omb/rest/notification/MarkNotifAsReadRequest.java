package com.ib.omb.rest.notification;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonCreator;

@XmlRootElement
public class MarkNotifAsReadRequest {
	
	private String jwtToken;
	
	private List<Integer> idList;
	
	public MarkNotifAsReadRequest() { }
	
	public MarkNotifAsReadRequest(String jwtToken, List<Integer> idList) {
		this.jwtToken = jwtToken;
		this.idList = idList;
	}
	
	public String getJwtToken() {
		return jwtToken;
	}
	public void setJwtToken(String jwtToken) {
		this.jwtToken = jwtToken;
	}
	public List<Integer> getIdList() {
		return idList;
	}
	
	public void setIdList(List<Integer> idList) {
		this.idList = idList;
	}
	
}

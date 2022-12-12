package com.ib.omb.notifications;

public class LoggedUser {
	
	private Integer userId;
	private Integer registratureId;
	
	
	public LoggedUser(Integer userId, Integer registratureId) {
		this.userId = userId;
		this.registratureId = registratureId;
	}
	
	public Integer getUserId() {
		return userId;
	}
	public void setUserId(Integer userId) {
		this.userId = userId;
	}
	public Integer getRegistratureId() {
		return registratureId;
	}
	public void setRegistratureId(Integer registratureId) {
		this.registratureId = registratureId;
	}
	
	
	

}

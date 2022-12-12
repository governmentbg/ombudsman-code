package com.ib.omb.notifications;

import java.util.ArrayList;
import java.util.List;

import com.ib.omb.db.dto.UserNotifications;
import com.ib.omb.experimental.Notification;

/**Обвиваща структура на съобщение + потребителите до които трябва да се изпрати
 * @author krasi
 *
 */
public class UMsgHolder {
	
	
	

	
	private Notification nb;
	private UserNotifications message;
	private List<Integer> users=new ArrayList<Integer>();
	
	
	public UMsgHolder(String title, String details, String severity, List<Integer> users) {
		super();
		this.setMessage(new UserNotifications(null,title,details,severity));
		this.setUsers(users);
	}

	public UMsgHolder(String title, String details, String severity, Integer user) {
		super();
		this.setMessage(new UserNotifications(null,title,details,severity));
		this.getUsers().add(user);
	}
	
	public UMsgHolder(Notification nb, Integer user) {
		super();
		this.nb = nb;
		this.getUsers().add(user);
	}
	
	public UMsgHolder(Notification nb, List<Integer> users) {
		super();
		this.nb = nb;
		this.setUsers(users);
	}
	
	public UserNotifications getMessage() {
		return message;
	}

	public void setMessage(UserNotifications messages) {
		this.message = messages;
	}
	public List<Integer> getUsers() {
		return users;
	}

	public void setUsers(List<Integer> users) {
		this.users = users;
	}
	
	@Override
	public String toString() {
		return "UMsgHolder [message=" + message + ", users=" + users + "]";
	}

	
	
}

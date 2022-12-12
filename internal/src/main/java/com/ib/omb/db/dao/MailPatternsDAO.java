package com.ib.omb.db.dao;

import com.ib.omb.db.dto.NotificationPatterns;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;

/**
 * DAO for {@link NotificationPatterns}
 *
 * @author mamun
 */
public class MailPatternsDAO extends AbstractDAO<NotificationPatterns> {

	/** @param user */
	public MailPatternsDAO(ActiveUser user) {
		super(NotificationPatterns.class, user);
	}

	
}
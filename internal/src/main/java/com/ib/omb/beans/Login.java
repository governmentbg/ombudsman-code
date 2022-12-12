package com.ib.omb.beans;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.enterprise.AuthenticationStatus;
import javax.security.enterprise.SecurityContext;
import javax.security.enterprise.authentication.mechanism.http.AuthenticationParameters;
import javax.security.enterprise.credential.Credential;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexLoginBean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.system.SystemData;
import com.ib.system.BaseUserData;
import com.ib.system.IBUserPrincipal;
import com.ib.system.auth.DBCredential;
import com.ib.system.exceptions.AuthenticationException;

/**
 * Логин със SecurityContext
 *
 * @author belev
 */
@Named
@ViewScoped
public class Login extends IndexLoginBean {

	/**  */
	private static final long serialVersionUID = 8191901936895268740L;

	private static final Logger LOGGER = LoggerFactory.getLogger(Login.class);

	@Inject
	private SecurityContext	securityContext;
	@Inject
	private ExternalContext	externalContext;
	@Inject
	private FacesContext	facesContext;

	/** ако се вдигне флага трябва да се направи upgrade и не трябва да се допуска вход в систамата */
	private boolean upgrade;
	
	/** */
	public Login() {
		super();
	}

	/** */
	@PostConstruct
	void initData() {
		SystemData sd = (SystemData) getSystemData();
		
		this.upgrade = !Objects.equals(sd.getSrcVersion(), sd.getDbVersion());
	}

	/**
	 * Логин със SecurityContext
	 */
	@Override
	protected BaseUserData login() {
		LOGGER.info("---> SecurityContext login start <---");

		HttpServletRequest request = (HttpServletRequest) this.externalContext.getRequest();
		HttpServletResponse response = (HttpServletResponse) this.externalContext.getResponse();

		try { // винаги е нов логин. може и да има и по хитър начин!
			request.logout();
		} catch (ServletException e) {
			LOGGER.error(e.getMessage(), e);
		}

		AuthenticationStatus status = continueAuthentication(request, response);

		switch (status) {

		case SEND_CONTINUE:
			LOGGER.debug("actionLogin-SEND_CONTINUE");

			this.facesContext.responseComplete();

			break;

		case SEND_FAILURE:
			LOGGER.debug("actionLogin-SEND_FAILURE");

			JSFUtils.addErrorMessage( getMessageResourceString(UI_beanMessages, "login.fail"));

			break;

		case SUCCESS:
			LOGGER.debug("actionLogin-SUCCESS");

			try {
				this.externalContext.redirect(this.externalContext.getRequestContextPath() + "/pages/dashboard.xhtml");
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}

			break;

		case NOT_DONE:
			// тука няма да влезне
		}

		// трябва да върна данните за логнатия потребител
		Set<IBUserPrincipal> principals = this.securityContext.getPrincipalsByType(IBUserPrincipal.class);

		if (principals.isEmpty()) {
			return null; // явно не се е логнал успешно
		}
		LOGGER.info("---> SecurityContext login END <---");

		return principals.iterator().next().getUserData(); // ето го усера
	}

	private AuthenticationStatus continueAuthentication(HttpServletRequest request, HttpServletResponse response) {

		try {
			Credential credential = null;
			// get login type from system_option table
//			String loginType = getSystemData().getSettingsValue(OmbConstants.LOGIN_TYPE);
//
//			if (OmbConstants.LOGIN_TYPE_DATABASE.equals(loginType)) {
//				credential = new DBCredential(getUsername(), getPassword());
//			} else if (OmbConstants.LOGIN_TYPE_LDAP.equals(loginType)) {
//				credential = new LDAPCredential(getUsername(), getPassword());
//			} else{
				credential = new DBCredential(getUsername(), getPassword());// default
//			}

			AuthenticationParameters parameters = AuthenticationParameters.withParams().credential(credential);

			return this.securityContext.authenticate(request, response, parameters);

		} catch (AuthenticationException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			return AuthenticationStatus.SEND_FAILURE;
		}
	}

	/** @return the upgrade */
	public boolean isUpgrade() {
		return this.upgrade;
	}
}

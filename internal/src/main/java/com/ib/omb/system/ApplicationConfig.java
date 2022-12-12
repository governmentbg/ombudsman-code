package com.ib.omb.system;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.faces.annotation.FacesConfig;
import javax.inject.Inject;
import javax.inject.Named;
import javax.security.enterprise.SecurityContext;
import javax.security.enterprise.authentication.mechanism.http.CustomFormAuthenticationMechanismDefinition;
import javax.security.enterprise.authentication.mechanism.http.LoginToContinue;

import com.ib.system.BaseUserData;
import com.ib.system.IBUserPrincipal;

/**
 * Настройки на системата
 *
 * @author belev
 */
@CustomFormAuthenticationMechanismDefinition(loginToContinue = @LoginToContinue(loginPage = "/login.xhtml", useForwardToLogin = false, errorPage = ""))
@FacesConfig(version = FacesConfig.Version.JSF_2_3)
@ApplicationScoped
public class ApplicationConfig {

	@Inject
	private SecurityContext securityContext; // дава ни информация за логнатия потребител

	/** */
	public ApplicationConfig() {
		super();
	}

	/**
	 * Дава информация за текущия логнат потребител. Може да се използва за inject-ване!
	 *
	 * @return
	 */
	@Produces
	@Named("userData")
	public BaseUserData getUserData() {
		return ((IBUserPrincipal) this.securityContext.getCallerPrincipal()).getUserData();
	}
}
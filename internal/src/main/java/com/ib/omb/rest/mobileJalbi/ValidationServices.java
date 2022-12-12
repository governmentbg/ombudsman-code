package com.ib.omb.rest.mobileJalbi;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonSyntaxException;

/**
 * Класът се използва от ValidationServiceApi
 * 
 * @author n.kanev
 *
 */
public class ValidationServices extends RestApiImplementation {
	
private static final Logger LOGGER = LoggerFactory.getLogger(ValidationServices.class.getName());
	
	public ValidationServices(ServletContext context, Integer lang) {
		super(context);
		setMessagesLocale(lang);
	}

	public Response validateEgn(String request) {
		try {
			return RestUtils.validateRequest(request, "egn", 1);
		}
		catch(JsonSyntaxException e) {
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restValid.errValidation"));
		}
		catch(ClassCastException e) {
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restValid.errValueType"));
		}
	}
	
	public Response validateLnch(String request) {
		try {
			return RestUtils.validateRequest(request, "lnch", 2);
		}
		catch(JsonSyntaxException e) {
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restValid.errValidation"));
		}
		catch(ClassCastException e) {
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restValid.errValueType"));
		}
	}
	
	public Response validateEik(String request) {
		try {
			return RestUtils.validateRequest(request, "eik", 3);
		}
		catch(JsonSyntaxException e) {
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restValid.errValidation"));
		}
		catch(ClassCastException e) {
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restValid.errValueType"));
		}
	}
	
	public Response validateEmail(String request) {
		try {
			return RestUtils.validateRequest(request, "email", 4);
		}
		catch(JsonSyntaxException e) {
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restValid.errValidation"));
		}
		catch(ClassCastException e) {
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restValid.errValueType"));
		}
	}

}

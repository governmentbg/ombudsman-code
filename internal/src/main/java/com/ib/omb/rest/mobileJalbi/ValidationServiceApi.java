package com.ib.omb.rest.mobileJalbi;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Класът се използва от мобилните приложения за подаване на жалби и от сайта.
 * 
 * @author n.kanev
 *
 */
@Path("ombValidationServices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ValidationServiceApi extends RestApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(ValidationServiceApi.class.getName());
	
	@POST
	@Path("validateEgn")
	public Response validateEgn(String request,
			@QueryParam("lang") Integer lang,
			@HeaderParam("User-Agent") String agent) {
		LOGGER.debug("--- validateEgn begin ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		ValidationServices s = new ValidationServices(getContext(), lang);
		Response response = s.validateEgn(request);
		LOGGER.debug("--- validateEgn end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	@POST
	@Path("validateLnch")
	public Response validateLnch(String request,
			@QueryParam("lang") Integer lang,
			@HeaderParam("User-Agent") String agent) {
		LOGGER.debug("--- validateLnch begin ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		ValidationServices s = new ValidationServices(getContext(), lang);
		Response response = s.validateLnch(request);
		LOGGER.debug("--- validateLnch end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	@POST
	@Path("validateEik")
	public Response validateEik(String request,
			@QueryParam("lang") Integer lang,
			@HeaderParam("User-Agent") String agent) {
		LOGGER.debug("--- validateEik begin ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		ValidationServices s = new ValidationServices(getContext(), lang);
		Response response = s.validateEik(request);
		LOGGER.debug("--- validateEik end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	@POST
	@Path("validateEmail")
	public Response validateEmail(String request,
			@QueryParam("lang") Integer lang,
			@HeaderParam("User-Agent") String agent) {
		LOGGER.debug("--- validateEmail begin ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		ValidationServices s = new ValidationServices(getContext(), lang);
		Response response = s.validateEmail(request);
		LOGGER.debug("--- validateEmail end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
}

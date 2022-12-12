package com.ib.omb.rest.mobileJalbi.v1;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.ib.omb.rest.mobileJalbi.RestApi;
import com.ib.omb.rest.mobileJalbi.RestUtils;
import com.ib.omb.system.OmbConstants;

/**
 * Класът се използва от мобилните приложения за подаване на жалби и от сайта.
 * 
 * @author n.kanev
 *
 */
@Path("v1/ombJalbiServices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JalbiServicesApi extends RestApi {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JalbiServicesApi.class.getName());
	
	@POST
	@Path("submitJalba")
	public Response submitJalba(String request,
			@QueryParam("lang") Integer lang,
			@HeaderParam("User-Agent") String agent) {
		LOGGER.debug("--- submitJalba begin ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		JalbiServices s = new JalbiServices(getContext(), lang);
		Response response = s.saveDocJalba(request, OmbConstants.CODE_ZNACHENIE_SUBMIT_METHOD_INTERNET);
		LOGGER.debug("--- submitJalba end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	@POST
	@Path("submitJalbaMobile")
	public Response submitJalbaMobile(String request,
			@QueryParam("lang") Integer lang,
			@HeaderParam("User-Agent") String agent) {
		LOGGER.debug("--- submitJalbaMobile begin ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		JalbiServices s = new JalbiServices(getContext(), lang);
		Response response = s.saveDocJalba(request, OmbConstants.CODE_ZNACHENIE_SUBMIT_METHOD_MOBILE);
		LOGGER.debug("--- submitJalbaMobile end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	@POST
	@Path("submitJalbaDetsa")
	public Response submitJalbaDetsa(String request,
			@QueryParam("lang") Integer lang,
			@HeaderParam("User-Agent") String agent) {
		LOGGER.debug("--- submitJalbaDetsa begin ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		JalbiServices s = new JalbiServices(getContext(), lang);
		Response response = s.saveDocJalbaDeca(request, OmbConstants.CODE_ZNACHENIE_SUBMIT_METHOD_INTERNET);
		LOGGER.debug("--- submitJalbaDetsa end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	@POST
	@Path("submitJalbaDetsaMobile")
	public Response submitJalbaDetsaMobile(String request,
			@QueryParam("lang") Integer lang,
			@HeaderParam("User-Agent") String agent) {
		LOGGER.debug("--- submitJalbaDetsaMobile begin ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		JalbiServices s = new JalbiServices(getContext(), lang);
		Response response = s.saveDocJalbaDeca(request, OmbConstants.CODE_ZNACHENIE_SUBMIT_METHOD_MOBILE);
		LOGGER.debug("--- submitJalbaDetsaMobile end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	@POST
	@Path("submitJalbaFormData")
	@Consumes(MediaType.MULTIPART_FORM_DATA + ";charset=UTF-8;")
	public Response submitJalbaMultipart(
			MultipartFormDataInput input,
			@QueryParam("lang") Integer lang,
			@HeaderParam("User-Agent") String agent) {
		
		LOGGER.debug("--- submitJalbaMultipart begin ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		JalbiServices s = new JalbiServices(getContext(), lang);
		Response response = s.saveJalbaMultipart(input, OmbConstants.CODE_ZNACHENIE_SUBMIT_METHOD_INTERNET);
		LOGGER.debug("--- submitJalbaMultipart end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	@POST
	@Path("testFormData")
	@Consumes(MediaType.MULTIPART_FORM_DATA + ";charset=UTF-8;")
	public Response testFormData(MultipartFormDataInput input) throws IOException {
		
		LOGGER.debug("--- testFormData begin ---");
		Map<String, String> parsed = new HashMap<>();
		Map<String, List<InputPart>> map = input.getFormDataMap();
		for(String key : map.keySet()) {
			List<InputPart> part = map.get(key);
			parsed.put(key, part.get(0).getBodyAsString());
		}
		
		Response response = RestUtils.returnSuccess(new Gson().toJson(parsed));
		LOGGER.debug("--- testFormData end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
}

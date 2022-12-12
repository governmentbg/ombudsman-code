package com.ib.omb.rest.mobileJalbi.v1;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.rest.mobileJalbi.RestApi;

/**
 * Класът се използва от мобилните приложения за подаване на жалби и от сайта.
 * 
 * @author n.kanev
 *
 */
@Path("v1/ombClassifServices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SysClassifServicesApi extends RestApi {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SysClassifServicesApi.class.getName());

	/**
	 * Връща цяла системна класификация.
	 * @param codeClassif кодът на класификацията
	 * @param lang езикът на класификацията, ако липсва, по подразбиране се приема 1=български
	 * @return
	 */
	@GET
	@Path("getClassif/{codeClassif}")
	public Response getClassif(
			@PathParam("codeClassif") Integer codeClassif, 
			@QueryParam("lang") Integer lang,
			@QueryParam("sort") String sort,
			@HeaderParam("User-Agent") String agent) {
		
		LOGGER.debug("--- getClassif begin: " + codeClassif + " ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		SysClassifServices s = new SysClassifServices(getContext(), lang);
		Response response = s.getClassif(codeClassif, lang, sort);
		LOGGER.debug("--- getClassif end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	/**
	 * Връща опции от логически списък по водещо значение.
	 * @param codeLogList кодът на списъка
	 * @param codeClassif кодът на водещото значение
	 * @param lang езикът на класификацията, ако липсва, по подразбиране се приема 1=български
	 * @return
	 */
	@GET
	@Path("getLogicListByCode/{codeLogList}/{codeClassif}")
	public Response getLogicListByCode(
			@PathParam("codeLogList") Integer codeLogList,
			@PathParam("codeClassif") Integer codeClassif, 
			@QueryParam("lang") Integer lang,
			@QueryParam("sort") String sort,
			@HeaderParam("User-Agent") String agent) {
	
		LOGGER.debug("--- getLogicListByCode begin: " + codeLogList + ", " + codeClassif + " ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		SysClassifServices s = new SysClassifServices(getContext(), lang);
		Response response = s.getLogicListByCode(codeLogList, codeClassif, lang, sort);
		LOGGER.debug("--- getLogicListByCode end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	/**
	 * Връща само значения от системна класификация, които съдържат даден стринг query.
	 * @param codeClassif кодът на класификацията
	 * @param query стрингът, по който се филтрира
	 * @param lang езикът на класификацията, ако липсва, по подразбиране се приема 1=български
	 * @return
	 */
	@GET
	@Path("filterClassif/{codeClassif}")
	public Response filterSystemClassif(
			@PathParam("codeClassif") Integer codeClassif, 
			@QueryParam("query") String query, 
			@QueryParam("lang") Integer lang,
			@QueryParam("sort") String sort,
			@HeaderParam("User-Agent") String agent) {
	
		LOGGER.debug("--- filterSystemClassif begin: " + codeClassif + ", " + query + " ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		SysClassifServices s = new SysClassifServices(getContext(), lang);
		Response response = s.filterClassif(codeClassif, query, lang, sort);
		LOGGER.debug("--- filterSystemClassif end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	/**
	 * Връща населени  места от класификацията ЕКАТТЕ, филтрирани по даден стринг query. Връща само нас. места, а не общини и области.
	 * @param query стрингът, по който се филтрира
	 * @param lang езикът на класификацията, ако липсва, по подразбиране се приема 1=български
	 * @return
	 */
	@GET
	@Path("getNasMiasto")
	public Response getNaselenoMiasto(
			@QueryParam("query") String query, 
			@QueryParam("lang") Integer lang,
			@QueryParam("sort") String sort,
			@HeaderParam("User-Agent") String agent) {
		
		LOGGER.debug("--- getNaselenoMiasto begin: " + query + " ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		SysClassifServices s = new SysClassifServices(getContext(), lang);
		Response response = s.getNaselenoMiasto(query, lang, sort);
		LOGGER.debug("--- getNaselenoMiasto end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
}

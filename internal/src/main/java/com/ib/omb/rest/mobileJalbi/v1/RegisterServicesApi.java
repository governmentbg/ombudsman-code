package com.ib.omb.rest.mobileJalbi.v1;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
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
@Path("v1/ombRegisterServices")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegisterServicesApi extends RestApi {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RegisterServicesApi.class.getName());
	
	@GET
	@Path("getRegisterJalbi")
	public Response getRegisterJalbi(
			@QueryParam("pageSize") Integer pageSize,	// брой резултати на странца
			@QueryParam("pageIndex") Integer pageIndex,	// индекс на страницата
			@QueryParam("sortCol") String sortCol,		// колона, по която се сортира
			@QueryParam("lang") Integer lang,			// език
			@QueryParam("rnDoc") String rnDoc,			// Вх. № 
			@QueryParam("docDateOt") Long docDateOt,	// Дата на жалба/сигнал
			@QueryParam("docDateDo") Long docDateDo,	// Дата на жалба/сигнал
			@QueryParam("jbpType") Integer jbpType,		// Категория на подателя на жалбата/сигнала
			@QueryParam("katNar") Integer katNar,		// Категория на орган/лице, срещу което е образувана преписката
			@QueryParam("vidNar") Integer vidNar,		// Вид на орган/лице, срещу което е образувана преписката 
			@QueryParam("zasPrava") Integer zasPrava,	// Засегнати права
			@QueryParam("vidOpl") Integer vidOpl,		// Вид оплакване 
			@QueryParam("sast") List<Integer> sast,		// Състояние на жалбата 
			@HeaderParam("User-Agent") String agent) {
		LOGGER.debug("--- getRegisterJalbi begin ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		RegisterServices s = new RegisterServices(getContext(), lang);
		Response response = s.searchRegisterJalbi(pageSize,	pageIndex, sortCol, lang, rnDoc, docDateOt, docDateDo, jbpType, katNar, vidNar, zasPrava, vidOpl, sast);
		LOGGER.debug("--- getRegisterJalbi end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	@GET
	@Path("getRegisterNpm")
	public Response getRegisterNpm(
			@QueryParam("pageSize") Integer pageSize,		// брой резултати на странца
			@QueryParam("pageIndex") Integer pageIndex,		// индекс на страницата
			@QueryParam("sortCol") String sortCol,			// колона, по която се сортира
			@QueryParam("lang") Integer lang,				// език
			@QueryParam("rnDoc") String rnDoc,				// № заповед
			@QueryParam("docDateOt") Long docDateOt,		// Дата на заповед
			@QueryParam("docDateDo") Long docDateDo,		// Дата на заповед
			@QueryParam("predmet") String predmet, 			// Предмет на проверката
			@QueryParam("narPrava") List<Integer> narPrava,	// Нарушени права 
			@QueryParam("codeOrgan") Integer codeOrgan,		// Проверяван орган
			@QueryParam("konstat") String konstat,			// Констатации
			@QueryParam("prepor") Integer prepor,			// Дадена препоръка 
			@QueryParam("vidResult") Integer vidResult,		// Резултат 
			@QueryParam("sast") List<Integer> sast,			// Състояние 
			@HeaderParam("User-Agent") String agent) {
		LOGGER.debug("--- getRegisterNpm begin ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		RegisterServices s = new RegisterServices(getContext(), lang);
		Response response = s.searchRegisterNpm(pageSize, pageIndex, sortCol, lang, rnDoc, docDateOt, docDateDo, predmet, narPrava, codeOrgan, konstat, prepor, vidResult, sast);
		LOGGER.debug("--- getRegisterNpm end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
	
	@GET
	@Path("getRegisterSamosez")
	public Response getRegisterSamosez(
			@QueryParam("pageSize") Integer pageSize,		// брой резултати на странца
			@QueryParam("pageIndex") Integer pageIndex,		// индекс на страницата
			@QueryParam("sortCol") String sortCol,			// колона, по която се сортира
			@QueryParam("lang") Integer lang,				// език
			@QueryParam("rnDoc") String rnDoc,				// № решение
			@QueryParam("docDateOt") Long docDateOt,		// Дата на решение
			@QueryParam("docDateDo") Long docDateDo,		// Дата на решение
			@QueryParam("predmet") String predmet, 			// Предмет на проверката
			@QueryParam("zasPrava") List<Integer> zasPrava,	// Засегнати права
			@QueryParam("codeOrgan") Integer codeOrgan,		// Проверяван орган
			@QueryParam("konstat") String konstat,			// Констатации
			@QueryParam("prepor") Integer prepor,			// Дадена препоръка 
			@QueryParam("vidResult") Integer vidResult,		// Резултат 
			@QueryParam("sast") List<Integer> sast,			// Състояние 
			@HeaderParam("User-Agent") String agent) {
		LOGGER.debug("--- getRegisterSamosez begin ---");
		LOGGER.debug("--- agent: " + agent + " ---");
		
		RegisterServices s = new RegisterServices(getContext(), lang);
		Response response = s.searchRegisterSamosez(pageSize, pageIndex, sortCol, lang, rnDoc, docDateOt, docDateDo, predmet, zasPrava, codeOrgan, konstat, prepor, vidResult, sast);
		LOGGER.debug("--- getRegisterSamosez end: " + response.getStatusInfo().getStatusCode() + " ---");
		
		return response;
	}
}

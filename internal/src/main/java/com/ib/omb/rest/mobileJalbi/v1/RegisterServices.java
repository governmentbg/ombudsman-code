package com.ib.omb.rest.mobileJalbi.v1;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.omb.rest.mobileJalbi.RestApiImplementation;
import com.ib.omb.rest.mobileJalbi.RestUtils;
import com.ib.omb.search.DocJalbaSearch;
import com.ib.omb.search.DocSpecSearch;
import com.ib.omb.system.OmbConstants;

/**
 * Класът се използва от RegisterServicesApi
 * 
 * @author n.kanev
 *
 */
public class RegisterServices extends RestApiImplementation {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RegisterServices.class.getName());
	private static final int DEFAULT_PAGE_SIZE = 10;
	
	public RegisterServices(ServletContext context, Integer lang) {
		super(context);
		setMessagesLocale(lang);
	}
	
	public Response searchRegisterJalbi(
			Integer pageSize,
			Integer pageIndex,
			String sortCol,
			Integer lang,
			String rnDoc, 
			Long docDateOt, 
			Long docDateDo, 
			Integer jbpType, 
			Integer katNar, 
			Integer vidNar, 
			Integer zasPrava, 
			Integer vidOpl, 
			List<Integer> sast) {
		
		DocJalbaSearch search = new DocJalbaSearch();
		search.setRnDoc(rnDoc == null ? null : rnDoc.trim());
		search.setDocDateFrom(docDateOt == null ? null : new Date(docDateOt));
		search.setDocDateTo(docDateDo == null ? null : new Date(docDateDo));
		search.setJbpType(jbpType);
		search.setKatNar(katNar);
		search.setVidNar(vidNar);
		search.setZasPrava(zasPrava);
		search.setVidOpl(vidOpl);
		search.setSastList(sast);
		if(pageSize == null) pageSize = DEFAULT_PAGE_SIZE;
		if(pageIndex == null) pageIndex = 0;
		
		if(lang == null) lang = OmbConstants.CODE_LANG_BG;		
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			List<LinkedHashMap<String, Object>> results = search.restRegisterJalbi(pageSize, pageIndex, sortCol, getSystemData(), lang);
			Map<String, Object> resultMap = convertResultsToMap(results);
			
			String json = objectMapper.writeValueAsString(resultMap);
			
			Response response = RestUtils.returnSuccess(json);
			return response;
					
		} 
		catch (Exception e) {
			LOGGER.error("--- error searching register jalbi ---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restRegister.errResult"));
		}
	}
	
	public Response searchRegisterNpm(
			Integer pageSize, 
			Integer pageIndex, 
			String sortCol, 
			Integer lang,
			String rnDoc, 
			Long docDateOt, 
			Long docDateDo, 
			String predmet, 
			List<Integer> narPrava, 
			Integer codeOrgan,
			String konstat, 
			Integer prepor, 
			Integer vidResult, 
			List<Integer> sast) {
		
		DocSpecSearch search = new DocSpecSearch();
		search.setRnDoc(rnDoc);
		search.setDocDateFrom(docDateOt == null ? null : new Date(docDateOt));
		search.setDocDateTo(docDateDo == null ? null : new Date(docDateDo));
		search.setOtnosno(predmet);
		search.setNarPravaList(narPrava);
		search.setCodeOrgan(codeOrgan);
		search.setKonstat(konstat);
		search.setPrepor(prepor);
		search.setVidResult(vidResult);
		search.setSastList(sast);
		if(pageSize == null) pageSize = DEFAULT_PAGE_SIZE;
		if(pageIndex == null) pageIndex = 0;

		if(lang == null) lang = OmbConstants.CODE_LANG_BG;
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			List<LinkedHashMap<String, Object>> results = search.restRegisterNpmSamos(
					pageSize, pageIndex, sortCol, OmbConstants.CODE_ZNACHENIE_DOC_VID_NPM, getSystemData(), lang);
			Map<String, Object> resultMap = convertResultsToMap(results);
			
			String json = objectMapper.writeValueAsString(resultMap);
			
			Response response = RestUtils.returnSuccess(json);
			return response;
		}
		catch (Exception e) {
			LOGGER.error("--- error searching register NPM ---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restRegister.errResult"));
		}

	}

	public Response searchRegisterSamosez(
			Integer pageSize, 
			Integer pageIndex, 
			String sortCol,
			Integer lang,
			String rnDoc, 
			Long docDateOt,
			Long docDateDo,
			String predmet, 
			List<Integer> zasPrava, 
			Integer codeOrgan, 
			String konstat, 
			Integer prepor, 
			Integer vidResult, 
			List<Integer> sast) {
		
		DocSpecSearch search = new DocSpecSearch();
		search.setRnDoc(rnDoc);
		search.setDocDateFrom(docDateOt == null ? null : new Date(docDateOt));
		search.setDocDateTo(docDateDo == null ? null : new Date(docDateDo));
		search.setOtnosno(predmet);
		search.setZasPravaList(zasPrava);
		search.setCodeOrgan(codeOrgan);
		search.setKonstat(konstat);
		search.setPrepor(prepor);
		search.setVidResult(vidResult);
		search.setSastList(sast);
		if(pageSize == null) pageSize = DEFAULT_PAGE_SIZE;
		if(pageIndex == null) pageIndex = 0;

		if(lang == null) lang = OmbConstants.CODE_LANG_BG;
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
			List<LinkedHashMap<String, Object>> results = search.restRegisterNpmSamos(
					pageSize, pageIndex, sortCol, OmbConstants.CODE_ZNACHENIE_DOC_VID_SAMOS, getSystemData(), lang);
			Map<String, Object> resultMap = convertResultsToMap(results);
			
			String json = objectMapper.writeValueAsString(resultMap);
			
			Response response = RestUtils.returnSuccess(json);
			return response;
		}
		catch (Exception e) {
			LOGGER.error("--- error searching register Samos ---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restRegister.errResult"));
		}
	}
	
	/**
	 * Привежда резултатите в следния вид:
	 * <pre>
	 * {
	 *   "allCount" : int,
	 *   "results" : [
	 *     {
	 *       doc_id : 100,
	 *       ...            // всички останали полета от резултата
	 *     },
	 *     ...
	 *   ]
	 * }
	 * </pre>
	 * @param results
	 * @return
	 */
	private Map<String, Object> convertResultsToMap(List<LinkedHashMap<String, Object>> results) {
		Map<String, Object> resultMap = new HashMap<>();
		
		LinkedHashMap<String, Object> count = results.get(0);
		resultMap.put("allCount", count.get("allcount"));

		List<Object> resultList = new ArrayList<>();
		for(int i = 1; i < results.size(); i++) {
			resultList.add(results.get(i));
		}
		resultMap.put("results", resultList);

		return resultMap;
	}

}

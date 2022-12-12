package com.ib.omb.rest.mobileJalbi.v1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.omb.rest.mobileJalbi.RestApiImplementation;
import com.ib.omb.rest.mobileJalbi.RestUtils;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysClassifAdapter;
import com.ib.system.db.dto.SystemClassif;

/**
 * Класът се използва от SysClassifServicesApi
 * 
 * @author n.kanev
 *
 */
public class SysClassifServices extends RestApiImplementation {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SysClassifServices.class.getName());
	
	public SysClassifServices(ServletContext context, Integer lang) {
		super(context);
		setMessagesLocale(lang);
	}
	
	public Response getClassif(Integer codeClassif, Integer lang, String sort) {
		
		if(codeClassif == null) {
			return RestUtils.returnEmptyJson();
		}
		
		ObjectMapper objectMapper = new ObjectMapper();
		
        try {
        	if(lang == null) lang = OmbConstants.CODE_LANG_BG;
        	
			List<SystemClassif> list = getSystemData().getSysClassification(codeClassif, new Date(), lang);
			sortClassifs(list, sort);
			if(codeClassif == OmbConstants.CODE_CLASSIF_COUNTRIES) placeBgOnTop(list);
			
			List<Map<String, Object>> classifList = convertClassifToMap(list, false);
    		
    		String json = objectMapper.writeValueAsString(classifList);
    		
    		Response response = RestUtils.returnSuccess(json);
			RestUtils.addCacheHeader(response, 3600);
			return response;
			
		}
        catch (Exception e) {
			LOGGER.error("--- error getting sysCLassif ---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restClassif.errClassif"));
		}
	}
	
	public Response getLogicListByCode(Integer codeLogList, Integer codeClassif, Integer lang, String sort) {
		
		if(codeLogList == null || codeClassif == null) {
			return RestUtils.returnEmptyJson();
		}
		
		ObjectMapper objectMapper = new ObjectMapper();
		
		try {
        	if(lang == null) lang = OmbConstants.CODE_LANG_BG;
        	
        	// Зареждат се предварително на английски, за да не връща [ ], ако не са кеширани още
        	if(lang == OmbConstants.CODE_LANG_EN) {
        		/** Код на класификация "Засегнати права" */
        		getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_ZAS_PRAVA, new Date(), lang);
        		/** Код на класификация "Вид оплакване" */
        		getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_VID_OPL, new Date(), lang);
        	}
        	
			List<SystemClassif> list = getSystemData().getClassifByListVod(codeLogList, codeClassif, lang, new Date());
			sortClassifs(list, sort);
			List<Map<String, Object>> classifList = convertClassifToMap(list, false);
    		
    		String json = objectMapper.writeValueAsString(classifList);

			Response response = RestUtils.returnSuccess(json);
			RestUtils.addCacheHeader(response, 3600);
			return response;
			
		}
		catch (Exception e) {
			LOGGER.error("--- error getting logicList ---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restClassif.errLogList"));
		}
	}
	
	public Response filterClassif(Integer codeClassif, String query, Integer lang, String sort) {
		if(codeClassif == null) {
			return RestUtils.returnEmptyJson();
		}
		
		ObjectMapper objectMapper = new ObjectMapper();
	
		try {

        	if(lang == null) lang = OmbConstants.CODE_LANG_BG;
        	
			List<SystemClassif> list = getSystemData().queryClassification(codeClassif, query, new Date(), lang, new ArrayList<Integer>());
			sortClassifs(list, sort);
			List<Map<String, Object>> classifList = convertClassifToMap(list, false);
			
    		String json = objectMapper.writeValueAsString(classifList);
    		
    		Response response = RestUtils.returnSuccess(json);
			RestUtils.addCacheHeader(response, 3600);
			return response;
		} 
		catch (Exception e) {
			LOGGER.error("--- error filtering sysClassif---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restClassif.errClassif"));
		}
	}
	
	public Response getNaselenoMiasto(String query, Integer lang, String sort) {
		
		ObjectMapper objectMapper = new ObjectMapper();
	
		try {
        	
        	if(lang == null) lang = OmbConstants.CODE_LANG_BG;
        	int codeClassif = OmbConstants.CODE_CLASSIF_EKATTE;
        	Map<Integer, Object> specifics = Collections.singletonMap(SysClassifAdapter.EKATTE_INDEX_TIP, 3);
        			
			List<SystemClassif> list = getSystemData().queryClassification(codeClassif, query, new Date(), lang, specifics);
			sortClassifs(list, sort);
			List<Map<String, Object>> classifList = convertClassifToMap(list, true);
    		
    		String json = objectMapper.writeValueAsString(classifList);
    		
    		Response response = RestUtils.returnSuccess(json);
			RestUtils.addCacheHeader(response, 604800); // 7 дни
			return response;
		}
		catch (Exception e) {
			LOGGER.error("--- error getting ekatte ---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(BEAN_MESSAGES, "restClassif.errClassif"));
		}
	}

	private void sortClassifs(List<SystemClassif>list, String sort) {
		
		int sortMode = 1;
		if(sort == null || sort.equals("asc")) sortMode = 1;
		else if(sort.equals("desc")) sortMode = -1;
		final int factor = sortMode; // глупаво създаване на променлива заради ламбдата
		
		Collections.sort(list, (s1,  s2) -> 
			s1.getTekst().toUpperCase().compareTo(s2.getTekst().toUpperCase()) * factor
		);
	}
	
	private void placeBgOnTop(List<SystemClassif> list) {
		OptionalInt indexOpt = 
				IntStream.range(0, list.size())
			     .filter(i -> list.get(i).getCode() == OmbConstants.CODE_ZNACHENIE_COUNTRY_BG)
			     .findFirst();
		
		indexOpt.ifPresent(index -> {
			SystemClassif bg = list.remove(index);
			list.add(0, bg);
		});
	}
	
	private List<Map<String, Object>> convertClassifToMap(List<SystemClassif> classifs, boolean getDopInfo) {
		List<Map<String, Object>> classifList = new ArrayList<>();
		
		for(SystemClassif c : classifs) {
			Map<String, Object> classif = new HashMap<>();
			classif.put("code", c.getCode());
			classif.put("tekst", c.getTekst());
			if(getDopInfo) classif.put("dopInfo", c.getDopInfo());
			classifList.add(classif);
		}
	
		return classifList;
	}
	
}

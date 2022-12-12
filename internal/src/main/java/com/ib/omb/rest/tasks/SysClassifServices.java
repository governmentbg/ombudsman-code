package com.ib.omb.rest.tasks;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.omb.rest.DocuWorkAndroidEndpoint;
import com.ib.omb.rest.DocuWorkRestResponse;
import com.ib.omb.rest.user.UsersServicesRespository;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;

/**
 * Използва се от мобилното приложение, за да си свали и запамети 
 * от системата класификации и логически списъци. Логиката за четене/писане
 * на завките и аутентикацията е копирана от {@link com.ib.omb.rest.tasks.TasksServices TasksServices }.
 * @author n.kanev
 */
@Path("sysclassifservices")
public class SysClassifServices extends DocuWorkAndroidEndpoint {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SysClassifServices.class.getName());
	
	@Inject
    private UsersServicesRespository usersServicesRespository;
	
	@Inject
    private ServletContext context;
	
	@POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("getSysClassif")
    public String getSysClassif(String getSysClassifRequestJson) {
		
		ObjectMapper objectMapper = new ObjectMapper();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		GetSysClassifRequest getSysClassifRequest = null;
		
		try {
			
			LOGGER.info("Begin getSysClassif ...");
            if(getSysClassifRequestJson == null || getSysClassifRequestJson.trim().isEmpty()){
                return "";
            }
            
            getSysClassifRequest = new ObjectMapper().readValue(getSysClassifRequestJson, GetSysClassifRequest.class);
            GetSysClassifResponse getSysClassifResponse = getSysClassif(getSysClassifRequest);
            objectMapper.writeValue(outputStream, getSysClassifResponse);

            LOGGER.info("Begin getSysClassif ...done!");
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            
		}
		catch(Exception e) {
			LOGGER.error(e.getMessage());
            return buildErrorResponse(
            		objectMapper, 
            		outputStream, 
            		new GetSysClassifResponse(
            				DocuWorkRestResponse.STATUS.ERROR,
            				getSysClassifRequest != null ? getSysClassifRequest.getJwtToken() : null,e.getMessage(),
            				null));
		}
		finally {
			JPA.getUtil().closeConnection();
		}
	}
	
	@POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("getLogicList")
	public String getLogicList(String getLogicListRequestJson) {
		ObjectMapper objectMapper = new ObjectMapper();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		GetLogicListRequest getLogicListRequest = null;
		
		try {
			
			LOGGER.info("Begin getLogicList ...");
            if(getLogicListRequestJson == null || getLogicListRequestJson.trim().isEmpty()){
                return "";
            }
            
            getLogicListRequest = new ObjectMapper().readValue(getLogicListRequestJson, GetLogicListRequest.class);
            GetSysClassifResponse getLogicListResponse = getLogicList(getLogicListRequest);
            objectMapper.writeValue(outputStream, getLogicListResponse);

            LOGGER.info("Begin getLogicList ...done!");
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            
		}
		catch(Exception e) {
			LOGGER.error(e.getMessage());
            return buildErrorResponse(
            		objectMapper, 
            		outputStream, 
            		new GetSysClassifResponse(
            				DocuWorkRestResponse.STATUS.ERROR,
            				getLogicListRequest != null ? getLogicListRequest.getJwtToken() : null, e.getMessage(),
            				null));
		}
		finally {
			JPA.getUtil().closeConnection();
		}
	}
	
	/**
	 * Създава Response обект на заявката за получаване на системна класификация.
	 * Аутентикира jwt токена и зарежда значенията.
	 * @param request парсната заявка
	 * @return
	 * @throws Exception
	 */
	private GetSysClassifResponse getSysClassif(GetSysClassifRequest request) throws Exception {
		if (request == null) {
            return new GetSysClassifResponse();
        }

        if (request.getJwtToken() == null) {
            return new GetSysClassifResponse();
        }
        
        int userId = extractUserIdFromJwtToken(request.getJwtToken());
        if(usersServicesRespository.containsUser(userId)) {

            SystemData sd = getSystemData(context);
            List<SystemClassif> list = sd.getSysClassification(request.getCodeClassif(), new Date(), 1);

            List<LinkedHashMap<String, Object>> classifList = new ArrayList<>();
    		for(SystemClassif c : list) {
    			LinkedHashMap<String, Object> classif = new LinkedHashMap<>();
    			classif.put("CODE_CLASSIF", c.getCodeClassif());
    			classif.put("CODE", c.getCode());
    			classif.put("TEKST", c.getTekst());
    			
    			classifList.add(classif);
    		}
            
            LOGGER.info("Begin getSysClassif ... done!");

            // load user details and access values to refresh jwtToken
            final AdmUser admUser = JPA.getUtil().getEntityManager().find(AdmUser.class, userId);
            // build the response
            return new GetSysClassifResponse(DocuWorkRestResponse.STATUS.SUCCESS, buildJwtToken(admUser, usersServicesRespository),  null, classifList);
        }
        else {
            return new GetSysClassifResponse(DocuWorkRestResponse.STATUS.ERROR, request.getJwtToken(), "Потребителят не е логнат!", null);
        }
	}
	
	/**
	 * Създава Response обект на заявката за получаване на логически списък.
	 * Аутентикира jwt токена и зарежда значенията.
	 * @param request парсната заявка
	 * @return
	 * @throws Exception
	 */
	private GetSysClassifResponse getLogicList(GetLogicListRequest request) throws Exception {
		if (request == null) {
            return new GetSysClassifResponse();
        }

        if (request.getJwtToken() == null) {
            return new GetSysClassifResponse();
        }
        
        int userId = extractUserIdFromJwtToken(request.getJwtToken());
        if(usersServicesRespository.containsUser(userId)) {
        	
        	Integer codeClassifVod = null;
        	switch(request.codeList) {
	        	case 2 : {
	        		codeClassifVod = OmbConstants.CODE_CLASSIF_TASK_REF_ROLE;
	        		break;
	        	}
	        	case 3 : {
	        		codeClassifVod = OmbConstants.CODE_CLASSIF_TASK_VID;
	        		break;
	        	}
        	
        	}

            SystemData sd = getSystemData(context);
            List<SystemClassif> classifVod = sd.getSysClassification(codeClassifVod, new Date(), 1);
            List<LinkedHashMap<String, Object>> logicList = new ArrayList<>();
            
            for(SystemClassif vodZnachenie : classifVod) {
            	List<SystemClassif> list = sd.getClassifByListVod(request.codeList, vodZnachenie.getCode(), 1, new Date());
            	
        		for(SystemClassif c : list) {
        			LinkedHashMap<String, Object> listItem = new LinkedHashMap<>();
        			listItem.put("CODE_LIST", request.codeList);
        			listItem.put("CODE_CLASSIF_VOD", codeClassifVod);
        			listItem.put("CODE_VOD", vodZnachenie.getCode());
        			listItem.put("CODE_CLASSIF_ZNACH", c.getCodeClassif());
        			listItem.put("CODE_ZNACH", c.getCode());
        			
        			logicList.add(listItem);
        		}
           
            }
            
            LOGGER.info("Begin getSysClassif ... done!");

            // load user details and access values to refresh jwtToken
            final AdmUser admUser = JPA.getUtil().getEntityManager().find(AdmUser.class, userId);
            // build the response
            return new GetSysClassifResponse(DocuWorkRestResponse.STATUS.SUCCESS, buildJwtToken(admUser, usersServicesRespository),  null, logicList);
            
        }
        else {
            return new GetSysClassifResponse(DocuWorkRestResponse.STATUS.ERROR, request.getJwtToken(), "Потребителят не е логнат!", null);
        }
	}
	
	/**
	 * Обектът Request - заявка за получаване на системна класификация.
	 * Автоматично се мапва от json body-то на заявката.
	 */
	private static class GetSysClassifRequest {
		
		private String jwtToken;
		private Integer codeClassif;
		
		public GetSysClassifRequest() {
			
		}

		public GetSysClassifRequest(String jwtToken, Integer codeClassif) {
			this.jwtToken = jwtToken;
			this.codeClassif = codeClassif;
		}

		public String getJwtToken() {
			return jwtToken;
		}

		public void setJwtToken(String jwtToken) {
			this.jwtToken = jwtToken;
		}

		public Integer getCodeClassif() {
			return codeClassif;
		}

		public void setCodeClassif(Integer codeClassif) {
			this.codeClassif = codeClassif;
		}
		
	}
	
	/**
	 * Обектът Response на заявката за получаване на системна класификация.
	 * Автоматично се мапва към json.
	 */
	private static class GetSysClassifResponse extends DocuWorkRestResponse {
		
		private String jwtToken;
		private List<LinkedHashMap<String, Object>> classifList;
		
		public GetSysClassifResponse() {
			
		}
		
		public GetSysClassifResponse(DocuWorkRestResponse.STATUS status, String jwtToken, String message, List<LinkedHashMap<String, Object>> classifList) {
	        super(status, message);
	        this.jwtToken = jwtToken;
	        this.classifList = classifList;
	    }

		public String getJwtToken() {
			return jwtToken;
		}

		public void setJwtToken(String jwtToken) {
			this.jwtToken = jwtToken;
		}

		public List<LinkedHashMap<String, Object>> getClassifList() {
			return classifList;
		}

		public void setClassifList(List<LinkedHashMap<String, Object>> classifList) {
			this.classifList = classifList;
		}
		
	}

	/**
	 * Обектът Request - заявка за получаване на логически списък.
	 * Автоматично се мапва от json body-то на заявката.
	 */
	private static class GetLogicListRequest {
		
		private String jwtToken;
		private Integer codeList;
		
		public GetLogicListRequest() {
			
		}

		public GetLogicListRequest(String jwtToken, Integer codeList) {
			this.jwtToken = jwtToken;
			this.codeList = codeList;
		}

		public String getJwtToken() {
			return jwtToken;
		}

		public void setJwtToken(String jwtToken) {
			this.jwtToken = jwtToken;
		}

		public Integer getCodeList() {
			return codeList;
		}

		public void setCodeList(Integer codeList) {
			this.codeList = codeList;
		}
	}
}

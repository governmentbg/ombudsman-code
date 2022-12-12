package com.ib.omb.rest.documents;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Objects;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocJalba;
import com.ib.omb.rest.DocuWorkAndroidEndpoint;
import com.ib.omb.rest.DocuWorkRestResponse;
import com.ib.omb.rest.user.UsersServicesRespository;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.OmbIdentityStore;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;

@Path("docservices")
public class DocumentServices extends DocuWorkAndroidEndpoint {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentServices.class.getName());
	
    @Inject
    private ServletContext context;

	@Inject
    private UsersServicesRespository usersServicesRespository;

	/**
     * Връща списък с документи по подаден номер и дата.
     * @param docRequestJson body на заявката; съдържа jwtToken, docNr, docDate.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("getDoc")
    public String getDoc(String docRequestJson) {
    	ObjectMapper objectMapper = new ObjectMapper();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FindDocumentRequest findDocumentRequest = null;
        try {
            LOGGER.info("Begin getDoc ...");
            if(docRequestJson == null || docRequestJson.trim().isEmpty()){
                return "";
            }

            findDocumentRequest = new ObjectMapper().readValue(docRequestJson, FindDocumentRequest.class);
            FindDocumentResponse findDocumentResponse = findDocument(findDocumentRequest);
            objectMapper.writeValue(outputStream, findDocumentResponse);

            LOGGER.info("Begin getDoc ...done!");
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
        catch (Exception e){
            LOGGER.error(e.getMessage());
            return buildErrorResponse(objectMapper, outputStream, new FindDocumentResponse(
            			DocuWorkRestResponse.STATUS.ERROR,
            			findDocumentRequest != null ? findDocumentRequest.getJwtToken() : null, 
    					e.getMessage(), 
    					null)
            		);
        }
        finally {
            JPA.getUtil().closeConnection();
        }
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("getFile")
    public String getFile(String jsonRequest) throws JsonMappingException, JsonProcessingException {
    	LOGGER.info("Begin getFile ...");
    	ObjectMapper objectMapper = new ObjectMapper();
    	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    	HashMap<String, Object> map = objectMapper.readValue(jsonRequest, HashMap.class);
    	
    	String jwtToken = null;
    	Integer fileID = null;
    	try {
	    	jwtToken = (String) map.get("jwtToken");
	    	fileID = (Integer) map.get("fileId");
    	}
    	catch(Exception e) { }
    	
    	if (jsonRequest == null || jsonRequest.trim().isEmpty()) {
            return "";
        }
    	
        try {
            FindFileResponse findFileResponse = findFile(jwtToken, fileID);
            objectMapper.writeValue(outputStream, findFileResponse);

            LOGGER.info("Begin getFile ...done!");
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
        catch (Exception e){
            LOGGER.error(e.getMessage());
            return buildErrorResponse(objectMapper, outputStream, new FindDocumentResponse(
            			DocuWorkRestResponse.STATUS.ERROR,
            			jwtToken, 
    					e.getMessage(), 
    					null)
            		);
        }
        finally {
            JPA.getUtil().closeConnection();
        }
        
    }
	
    private FindDocumentResponse findDocument(FindDocumentRequest findDocumentRequest) throws Exception {
    	if (findDocumentRequest == null || findDocumentRequest.getJwtToken() == null) {
            return new FindDocumentResponse();
        }

        // get user id from jwt token or throw exception
        int userId = extractUserIdFromJwtToken(findDocumentRequest.getJwtToken());

        if(usersServicesRespository.containsUser(userId)) {
            // load user details and access values to refresh jwtToken
            AdmUser admUser = JPA.getUtil().getEntityManager().find(AdmUser.class, userId);

            String jwtToken = buildJwtToken(admUser, usersServicesRespository);

			UserData ud = new OmbIdentityStore().createUserData(getSystemData(context), admUser);

			DocDAO docDAO = new DocDAO(ud);

            ArrayList<LinkedHashMap<String, Object>> taskList = (ArrayList<LinkedHashMap<String, Object>>) 
            		docDAO.restFindDocData(
            				ud, 
            				findDocumentRequest.getDocNr(), 
            				findDocumentRequest.getDocDate(), 
            				findDocumentRequest.getDocId(), 
            				getSystemData(context));
            LOGGER.info("Begin findDocument ... done!");

			return new FindDocumentResponse(DocuWorkRestResponse.STATUS.SUCCESS, jwtToken,  null, taskList);
        }
        else {
            return new FindDocumentResponse(DocuWorkRestResponse.STATUS.ERROR, findDocumentRequest.getJwtToken(),  "Потребителят не е логнат!", null);
        }
    }
    
    private FindFileResponse findFile(String jwtToken, Integer fileId) throws Exception {
    	if (jwtToken == null || fileId == null) {
            return new FindFileResponse();
        }
    	
    	int userId = extractUserIdFromJwtToken(jwtToken);
    	
    	if(usersServicesRespository.containsUser(userId)) {
            // load user details and access values to refresh jwtToken
            AdmUser admUser = JPA.getUtil().getEntityManager().find(AdmUser.class, userId);

            String newJwtToken = buildJwtToken(admUser, usersServicesRespository);

			UserData ud = new OmbIdentityStore().createUserData(getSystemData(context), admUser);

			FilesDAO fileDAO = new FilesDAO(ud);

            Files f = fileDAO.findById(fileId);
            f.getContent();

			return new FindFileResponse(DocuWorkRestResponse.STATUS.SUCCESS, newJwtToken,  null, f.getContent());
        }
        else {
            return new FindFileResponse(DocuWorkRestResponse.STATUS.ERROR, jwtToken,  "Потребителят не е логнат!", null);
        }
    }
    
    
	/**
	 * Това трябва да се извика от подходящата рест услуга
	 * 
	 * Запис на жалба
	 * 
	 * @param request
	 * @param systemData 
	 * @return
	 */
	public String saveDocJalba(String request, SystemData systemData) {
		Doc doc = new Doc();
		DocJalba jalba = new DocJalba();
		doc.setJalba(jalba);
		
		UserData ud = new UserData(-1, "", ""); // TODO от името на кой усер се записват жалбите от мобилното
		DocDAO dao = new DocDAO(ud);
		Integer registraturaId = 1; 
		
		String response;
		try {
			
			// СТАТИЧНИ ДАННИ
			doc.setRegistraturaId(registraturaId);
			doc.setDocVid(OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA);
			doc.setDocType(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN);
			doc.setDocDate(new Date());
			doc.setOtnosno(""); // TODO трябва ли нещо да дойде от жалбата тука
			
			doc.setReceiveDate(new Date());
			doc.setReceiveMethod(null); // TODO трябва да е ново от класификацията
			
			doc.setValid(1); // TODO константа ?
			doc.setValidDate(new Date());
			
			doc.setProcessed(SysConstants.CODE_ZNACHENIE_NE);
			doc.setFreeAccess(SysConstants.CODE_ZNACHENIE_DA);
			doc.setCompetence(OmbConstants.CODE_ZNACHENIE_COMPETENCE_OUR);
			
			doc.setCountFiles(0);

			jalba.setSast(1);  // TODO константа ?
			jalba.setSastDate(new Date());
			jalba.setPublicVisible(SysConstants.CODE_ZNACHENIE_NE);
			jalba.setInstCheck(SysConstants.CODE_ZNACHENIE_NE);
			
			
			
			// ДАННИ ОТ REQUEST-А
			jalba.setJbpType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL); // TODO може и НФЛ
			jalba.setJalbaText("JalbaText");
			
			jalba.setJbpName("JbpName");
			jalba.setJbpHidden(SysConstants.CODE_ZNACHENIE_NE);
//			jalba.setJbp... TODO всички налични данни за жалбоподателя
			
			// TODO + всичко което се попълва за жалбата от мобилното
			
			
			
			
			// ДАННИ КОИТО СЕ ВЗИМАТ ОТ Х-КИ ПО ВИД ДОКУМЕНТ
			Object[] settings = dao.findDocSettings(doc.getRegistraturaId(), doc.getDocVid(), systemData);

			doc.setRegisterId((Integer) settings[1]);
			boolean createDelo = Objects.equals(SysConstants.CODE_ZNACHENIE_DA, settings[2]);
			
			
			
			
			JPA.getUtil().begin();
			dao.save(doc, createDelo, null, null, systemData);
			JPA.getUtil().commit();
			
			response = "OK"; // TODO да се направи каквото трявба
			
		} catch (Exception e) {
			JPA.getUtil().rollback();

			LOGGER.error("Грешка при запис на жалба.", e);
			
			response = "ERROR"; // TODO да се направи каквото трявба
			
		} finally {
			JPA.getUtil().closeConnection();
		}
		return response;
	}
}

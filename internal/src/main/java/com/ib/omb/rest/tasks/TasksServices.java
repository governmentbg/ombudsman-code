package com.ib.omb.rest.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.omb.db.dao.TaskDAO;
import com.ib.omb.db.dto.Task;
import com.ib.omb.rest.DocuWorkAndroidEndpoint;
import com.ib.omb.rest.DocuWorkRestResponse;
import com.ib.omb.rest.user.UsersServicesRespository;
import com.ib.omb.system.UserData;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.security.enterprise.SecurityContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("taskservices")
public class TasksServices extends DocuWorkAndroidEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(TasksServices.class.getName());
    @Inject
    private ServletContext context;
    @Context
    private SecurityContext securityContext;
    @Context
    private HttpServletRequest request;
    @Context
    private HttpServletResponse response;

    @Inject
    private UsersServicesRespository usersServicesRespository;
    /**
     * Method returns tasks for given user by pages and given page size
     * @param findTasksByUserRequest - object wrapper for example {jwtToken:'...', pageSize:'10', pageIndex:'1'}
     * @return FindTasksByUserResponse- all tasks for provided user by username and password
     */
    @POST
    @Consumes({MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_XML})
    @Path("findTasksByUserXML")
    public FindTasksByUserResponse findTasksByUserXML(FindTasksByUserRequest findTasksByUserRequest){
        try {
            LOGGER.debug("Begin findTasksByUserXML ...");
            if(findTasksByUserRequest == null){
                return new FindTasksByUserResponse(DocuWorkRestResponse.STATUS.WARNING,null, "Няма заявка!",null);
            }

            FindTasksByUserResponse findTasksByUserResponse = findTasksByUser(findTasksByUserRequest);
            LOGGER.debug("Begin findTasksByUserXML ...done!");
            return findTasksByUserResponse;
         }catch (Exception e){
            LOGGER.error(e.getMessage());
            if(findTasksByUserRequest != null) {
                return new FindTasksByUserResponse(DocuWorkRestResponse.STATUS.ERROR,findTasksByUserRequest.getJwtToken(),  e.getMessage(), null);
            }else{
                return new FindTasksByUserResponse(DocuWorkRestResponse.STATUS.ERROR,null, e.getMessage(), null);
            }
        }finally {
            JPA.getUtil().closeConnection();
        }
    }

    /**
     * Method returns tasks for given user by pages and given page size
     * @param findTasksByUserRequestJson - object wrapper for example {jwtToken:'...', pageSize:'10', pageIndex:'1'}
     * @return FindTasksByUserResponse- all tasks for provided user by username and password
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("findTasksByUserJson")
    public String findTasksByUserJson(String findTasksByUserRequestJson){
        ObjectMapper objectMapper = new ObjectMapper();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FindTasksByUserRequest findTasksByUserRequest = null;
        try {
            LOGGER.debug("Begin findTasksByUserJson ...");
            if(findTasksByUserRequestJson == null || "".equals(findTasksByUserRequestJson) || findTasksByUserRequestJson.trim().length() == 0){
                return "";
            }

            findTasksByUserRequest = new ObjectMapper().readValue(findTasksByUserRequestJson, FindTasksByUserRequest.class);
            FindTasksByUserResponse findTasksByUserResponse = findTasksByUser(findTasksByUserRequest);
            objectMapper.writeValue(outputStream, findTasksByUserResponse);

            LOGGER.debug("Begin findTasksByUserJson ...done!");
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }catch (Exception e){
            LOGGER.error(e.getMessage());
            return buildErrorResponse(objectMapper, outputStream, new FindTasksByUserResponse(DocuWorkRestResponse.STATUS.ERROR,
                    findTasksByUserRequest!=null?findTasksByUserRequest.getJwtToken():null,e.getMessage(),null));
        }finally {
            JPA.getUtil().closeConnection();
        }
    }

    /*
     *
     */
    private FindTasksByUserResponse findTasksByUser(FindTasksByUserRequest findTasksByUserRequest) throws Exception {
        if (findTasksByUserRequest == null) {
            return new FindTasksByUserResponse();
        }

        if (findTasksByUserRequest.getJwtToken() == null) {
            return new FindTasksByUserResponse();
        }

        // get user id from jwt token or throw exception
        int userId = extractUserIdFromJwtToken(findTasksByUserRequest.getJwtToken());

        if(usersServicesRespository.containsUser(userId)) {

            TaskDAO taskDAO = new TaskDAO(ActiveUser.of(userId));

            // load tasks all tasks list using taskDAO methods
            ArrayList<LinkedHashMap<String, Object>> taskList = (ArrayList<LinkedHashMap<String, Object>>) taskDAO.restFindTasksByUser(userId, findTasksByUserRequest.getPageSize(), findTasksByUserRequest.getPageIndex(), getSystemData(context));

            LOGGER.debug("Begin findTasksByUserXML ... done!");

            // load user details and access values to refresh jwtToken
            final AdmUser admUser = JPA.getUtil().getEntityManager().find(AdmUser.class, userId);
            // build the response
            return new FindTasksByUserResponse(DocuWorkRestResponse.STATUS.SUCCESS, buildJwtToken(admUser, usersServicesRespository),  null, taskList);
        }else{
            //
            return new FindTasksByUserResponse(DocuWorkRestResponse.STATUS.ERROR, findTasksByUserRequest.getJwtToken(),  "Потребителят не е логнат!", null);
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    @Path("findTaskByIdXML")
    public FindTaskByIdResponse findTaskByIdXML(FindTaskByIdRequest findTaskByIdRequest) {
        try{
            LOGGER.debug("Begin findTaskByIdXML ...");
            if(findTaskByIdRequest == null){
                return new FindTaskByIdResponse(DocuWorkRestResponse.STATUS.WARNING, "Няма заявка!",null,null);
            }

            if(usersServicesRespository.containsUser(extractUserIdFromJwtToken(findTaskByIdRequest.getJwtToken()))) {

                FindTaskByIdResponse findTasksByUserResponse = findTaskById(findTaskByIdRequest);

                LOGGER.debug("Begin findTaskByIdXML ...done!");
                return findTasksByUserResponse;
            }else{
                return new FindTaskByIdResponse(DocuWorkRestResponse.STATUS.WARNING, "Няма логнат потребител!", null, null);
            }
        }catch (Exception e){
            LOGGER.error(e.getMessage());
            return new FindTaskByIdResponse(DocuWorkRestResponse.STATUS.ERROR, findTaskByIdRequest!=null?findTaskByIdRequest.getJwtToken():null, e.getMessage(),   null);
        }finally {
            JPA.getUtil().closeConnection();
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("findTaskByIdJson")
    public String findTaskByIdJson(String findTaskByIdRequestJson){

    	ObjectMapper objectMapper = new ObjectMapper();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        FindTaskByIdRequest findTaskByIdRequest = null;
        try {
            LOGGER.debug("Begin findTaskByIdJson ...");
            if(findTaskByIdRequestJson == null || findTaskByIdRequestJson.trim().isEmpty()){
                return "";
            }

            findTaskByIdRequest = new ObjectMapper().readValue(findTaskByIdRequestJson, FindTaskByIdRequest.class);
            FindTaskByIdResponse findTaskByIdResponse = findTaskById(findTaskByIdRequest);
            objectMapper.writeValue(outputStream, findTaskByIdResponse);

            LOGGER.debug("Begin findTaskByIdJson ...done!");
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e){
            LOGGER.error(e.getMessage());
            return buildErrorResponse(objectMapper, outputStream, new FindTaskByIdResponse(DocuWorkRestResponse.STATUS.ERROR,
            		findTaskByIdRequest != null ? findTaskByIdRequest.getJwtToken() : null, e.getMessage(), null));
        } finally {
            JPA.getUtil().closeConnection();
        }
        
    }

    private FindTaskByIdResponse findTaskById(FindTaskByIdRequest findTaskByIdRequest) throws Exception{
        if (findTaskByIdRequest == null) {
            return new FindTaskByIdResponse();
        }

        if (findTaskByIdRequest.getJwtToken() == null) {
            return new FindTaskByIdResponse();
        }

        int userId = extractUserIdFromJwtToken(findTaskByIdRequest.getJwtToken());
        
        if(usersServicesRespository.containsUser(userId)) {
        	
        	TaskDAO taskDAO = new TaskDAO(ActiveUser.of(userId));

        	Map<String, Object> task = taskDAO.restFindById(findTaskByIdRequest.getTaskId(), getSystemData(context));
        	
            AdmUser admUser = JPA.getUtil().getEntityManager().find(AdmUser.class, userId);
            return new FindTaskByIdResponse(DocuWorkRestResponse.STATUS.SUCCESS, buildJwtToken(admUser, usersServicesRespository),  null, task);
        	
        }
        else {
            return new FindTaskByIdResponse(DocuWorkRestResponse.STATUS.ERROR, findTaskByIdRequest.getJwtToken(),  "Потребителят не е логнат!", null);
        }
       

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("updateTaskStatusJson")
    public String updateTaskStatusJson(String updateTaskStatusRequestString) {
        ObjectMapper objectMapper = new ObjectMapper();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        LOGGER.debug("Begin updateTaskStatusJson ...");
        if(updateTaskStatusRequestString == null || updateTaskStatusRequestString.trim().length() == 0){
            UpdateTaskStatusResponse updateTaskStatusResponse = new UpdateTaskStatusResponse(DocuWorkRestResponse.STATUS.ERROR, null,"Няма заявка");
            try {
                objectMapper.writeValue(outputStream, updateTaskStatusResponse);
            } catch (IOException e) {
                // ignore
            }
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }

        UpdateTaskStatusRequest updateTaskStatusRequest = null;
        try {
            updateTaskStatusRequest = objectMapper.readValue(updateTaskStatusRequestString, UpdateTaskStatusRequest.class);

            if(usersServicesRespository.containsUser(extractUserIdFromJwtToken(updateTaskStatusRequest.getJwtToken()))) {

                UpdateTaskStatusResponse updateTaskStatusResponse = updateTaskStatus(updateTaskStatusRequest);
                objectMapper.writeValue(outputStream, updateTaskStatusResponse);

                LOGGER.debug("Begin updateTaskStatusJson ...done!");
                return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            } else {
                UpdateTaskStatusResponse findTasksByUserResponse = new UpdateTaskStatusResponse(DocuWorkRestResponse.STATUS.ERROR, null ,"Липсва логнат потребител");
                objectMapper.writeValue(outputStream, findTasksByUserResponse);
                return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return buildErrorResponse(objectMapper, outputStream, new UpdateTaskStatusResponse(DocuWorkRestResponse.STATUS.ERROR,
                    updateTaskStatusRequest!=null?updateTaskStatusRequest.getJwtToken():null, e.getMessage()));

        } finally {
            JPA.getUtil().closeConnection();
        }
    }

    @POST
    @Consumes({MediaType.APPLICATION_XML})
    @Produces(MediaType.APPLICATION_XML)
    @Path("updateTaskStatusXML")
    public UpdateTaskStatusResponse updateTaskStatusXML(UpdateTaskStatusRequest updateTaskStatusRequest){
        LOGGER.debug("Begin updateTaskStatusXML...");
        if(updateTaskStatusRequest == null){
            return new UpdateTaskStatusResponse(DocuWorkRestResponse.STATUS.WARNING, null ,"Няма заявка!");
        }

        try {
            if(usersServicesRespository.containsUser(extractUserIdFromJwtToken(updateTaskStatusRequest.getJwtToken()))) {

                UpdateTaskStatusResponse updateTaskStatusResponse = updateTaskStatus(updateTaskStatusRequest);

                LOGGER.debug("Begin updateTaskStatusXML ...done!");
                return updateTaskStatusResponse;
            }else{
                return new UpdateTaskStatusResponse(DocuWorkRestResponse.STATUS.WARNING, "Няма логнат потребител!", null);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return new UpdateTaskStatusResponse(DocuWorkRestResponse.STATUS.ERROR, updateTaskStatusRequest.getJwtToken(),e.getMessage());
        } finally {
            JPA.getUtil().closeConnection();
        }
    }

    private UpdateTaskStatusResponse updateTaskStatus(UpdateTaskStatusRequest updateTaskStatusRequest) throws Exception{

        int userId = extractUserIdFromJwtToken(updateTaskStatusRequest.getJwtToken());

        AdmUser admUser = JPA.getUtil().getEntityManager().find(AdmUser.class, userId);

        // status has two possible values - open or closed
        //updateTaskStatusRequest.getStatus()

        TaskDAO taskDAO = new TaskDAO(new UserData(admUser.getId(), admUser.getUsername(), admUser.getNames()));
        
        JPA.getUtil().runInTransaction(() -> 
        	taskDAO.restStatusTaskChange(
        			updateTaskStatusRequest.getTaskId(), 
        			updateTaskStatusRequest.getStatus(), 
        			updateTaskStatusRequest.getComment(), 
        			updateTaskStatusRequest.getStatusDate(), 
        			updateTaskStatusRequest.getMnenie(), 
        			getSystemData(context)));

        // it might be necessary to update jwtToekn
        return new UpdateTaskStatusResponse(DocuWorkRestResponse.STATUS.SUCCESS, buildJwtToken(admUser, usersServicesRespository), "Статусът на задачата е променен!");
    }
}

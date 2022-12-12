package com.ib.omb.rest.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.indexui.system.Constants;
import com.ib.omb.db.dao.UserNotificationsDAO;
import com.ib.omb.rest.DocuWorkAndroidEndpoint;
import com.ib.omb.rest.DocuWorkRestResponse;
import com.ib.omb.rest.user.UsersServicesRespository;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.security.enterprise.SecurityContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;

@Path("notificationservices")
public class NotificationServices extends DocuWorkAndroidEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServices.class.getName());
    
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

    
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("getNotifications")
    public String getNotifications(String notificationsRequestJson){
        ObjectMapper objectMapper = new ObjectMapper();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        NotificationsRequest notificationsRequest = null;
        try {
            LOGGER.info("Begin getNotifications ...");
            if(notificationsRequestJson == null || notificationsRequestJson.trim().isEmpty()){
                return "";
            }

            notificationsRequest = new ObjectMapper().readValue(notificationsRequestJson, NotificationsRequest.class);
            NotificationsResponse notificationsResponse = findNotifications(notificationsRequest);
            objectMapper.writeValue(outputStream, notificationsResponse);

            LOGGER.info("Begin getNotifications ...done!");
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }catch (Exception e){
            LOGGER.error(e.getMessage());
            return buildErrorResponse(objectMapper, outputStream, 
            		new NotificationsResponse(DocuWorkRestResponse.STATUS.ERROR, notificationsRequest != null ? notificationsRequest.getJwtToken() : null, e.getMessage(), null));
        }finally {
            JPA.getUtil().closeConnection();
        }
    }
    
    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("markNotificationAsRead")
    public String markNotificationAsRead(String markAsReadRequestJson){
        ObjectMapper objectMapper = new ObjectMapper();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MarkNotifAsReadRequest markAsReadRequest = null;
        try {
            LOGGER.info("Begin markNotificationAsRead ...");
            if(markAsReadRequestJson == null || markAsReadRequestJson.trim().isEmpty()){
                return "";
            }

            markAsReadRequest = new ObjectMapper().readValue(markAsReadRequestJson, MarkNotifAsReadRequest.class);
            MarkNotifAsReadResponse markAsReadResponse = markNotificationsAsRead(markAsReadRequest);
            objectMapper.writeValue(outputStream, markAsReadResponse);

            LOGGER.info("Begin markNotificationAsRead ...done!");
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }catch (Exception e){
            LOGGER.error(e.getMessage());
            return buildErrorResponse(objectMapper, outputStream, 
            		new NotificationsResponse(DocuWorkRestResponse.STATUS.ERROR, markAsReadRequest != null ? markAsReadRequest.getJwtToken() : null, e.getMessage(), null));
        }finally {
            JPA.getUtil().closeConnection();
        }
    }
    
    
    private NotificationsResponse findNotifications(NotificationsRequest notificationsRequest) throws Exception {
    	if (notificationsRequest == null) {
            return new NotificationsResponse();
        }

        if (notificationsRequest.getJwtToken() == null) {
            return new NotificationsResponse();
        }
        
     // get user id from jwt token or throw exception
        int userId = extractUserIdFromJwtToken(notificationsRequest.getJwtToken());
        
        if(usersServicesRespository.containsUser(userId)) {

            Integer registraturaId = (Integer) getSystemData(context)
            	.getItemSpecific(Constants.CODE_CLASSIF_ADMIN_STR, userId, SysConstants.CODE_DEFAULT_LANG, null
            		, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);

        	UserNotificationsDAO notifDao = new UserNotificationsDAO();
            ArrayList<LinkedHashMap<String, Object>> notifList = (ArrayList<LinkedHashMap<String, Object>>) notifDao.restFindUserNotifications(userId, OmbConstants.CODE_NOTIF_STATUS_NEPROCHETENA, registraturaId);
            final AdmUser admUser = JPA.getUtil().getEntityManager().find(AdmUser.class, userId);
            return new NotificationsResponse(DocuWorkRestResponse.STATUS.SUCCESS, buildJwtToken(admUser, usersServicesRespository),  null, notifList);
        }
        else {
            return new NotificationsResponse(DocuWorkRestResponse.STATUS.ERROR, notificationsRequest.getJwtToken(),  "Потребителят не е логнат!", null);
        }
    }
    
    private MarkNotifAsReadResponse markNotificationsAsRead(MarkNotifAsReadRequest markAsReadRequest) throws Exception {
    	if (markAsReadRequest == null) {
            return new MarkNotifAsReadResponse();
        }

        if (markAsReadRequest.getJwtToken() == null) {
            return new MarkNotifAsReadResponse();
        }
        
        int userId = extractUserIdFromJwtToken(markAsReadRequest.getJwtToken());
        
        if(usersServicesRespository.containsUser(userId)) {

        	UserNotificationsDAO notifDao = new UserNotificationsDAO();
        	JPA.getUtil().runInTransaction(() -> notifDao.restChangeNotificationsStatus(userId, OmbConstants.CODE_NOTIF_STATUS_PROCHETENA, null, markAsReadRequest.getIdList()));
            final AdmUser admUser = JPA.getUtil().getEntityManager().find(AdmUser.class, userId);
            return new MarkNotifAsReadResponse(DocuWorkRestResponse.STATUS.SUCCESS, buildJwtToken(admUser, usersServicesRespository),  null);
        }
        else {
            return new MarkNotifAsReadResponse(DocuWorkRestResponse.STATUS.ERROR, markAsReadRequest.getJwtToken(),  "Потребителят не е логнат!");
        }
    }
    
    
    
    
    
    /*
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sendNotificationToAndroidJson")
    public Response sendNotificationToAndroidJson(
            @FormParam("jwtToken") String jwtToken,
            @FormParam("title") String title,
            @FormParam("notification") String notification,
            @FormParam("redirectURL") String redirectURL){


        // TODO get user data that has to be sent as notification
        if(jwtToken == null || "".equals(jwtToken) || jwtToken.trim().length() == 0){
            return null;
        }

        // TODO example code
        JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
        jsonObjectBuilder.add("to","username-iliyan-lukov"); // TODO this is the token
        jsonObjectBuilder.add("collapse_key", "collapse_key");// TODO this is some value
        jsonObjectBuilder.add("priority", "normal");// TODO setup the value
        jsonObjectBuilder.add("time_to_live", 1000);// TODO setup the value
        jsonObjectBuilder.add("notification",
                Json.createObjectBuilder()
                        .add("title", title)
                        .add("body", notification)
                        .add("color", "#FF0000")
                        .add("icon", "")
        );
        jsonObjectBuilder.add("data",
                Json.createObjectBuilder()
                        .add("dannid", "IDvali")
                        .add("dannival","data value")
        );

        try {
            return Response.status(Response.Status.OK).entity(jsonObjectBuilder.build().toString())
                    .location(new URI(redirectURL)).build();
        } catch (URISyntaxException e) {
            LOGGER.error(e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

    }
    */
    
}

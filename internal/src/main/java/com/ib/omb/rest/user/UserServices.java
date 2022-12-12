package com.ib.omb.rest.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.indexui.system.Constants;
import com.ib.omb.db.dao.UserNotificationsDAO;
import com.ib.omb.rest.DocuWorkAndroidEndpoint;
import com.ib.omb.rest.DocuWorkRestResponse;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.system.ActiveUser;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.AuthenticationException;
import com.ib.system.exceptions.DbErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.persistence.Query;
import javax.security.enterprise.SecurityContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import static com.ib.system.SysConstants.CODE_DEIN_LOGIN;
import static com.ib.system.SysConstants.CODE_DEIN_LOGOUT;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Path("userservices")
public class UserServices extends DocuWorkAndroidEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServices.class.getName());
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
     * Method tries to log in the user with given <code>username</code> and <code>password</code>
     * @param loginRequest - object wrapper for example {username: 'aaa',password:{bbb}, deviceToken:{ccc}}
     * @return - result of the operation
     */
    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    @Path("loginXML")
    public LoginResponse loginXML(LoginRequest loginRequest) {
        try {
            LOGGER.info("Begin loginXML in ...");
            if(loginRequest == null){
                return new LoginResponse(DocuWorkRestResponse.STATUS.ERROR, "", "Липсва заявка !", null, null);
            }

            LoginResponse response = loginImplementation(loginRequest.getUsername(), loginRequest.getPassword());

            LOGGER.info("Begin loginXML in ...done!");
            return response;
        }catch (Exception e){
            LOGGER.error(e.getMessage());
            return new LoginResponse(DocuWorkRestResponse.STATUS.ERROR,"", e.getMessage(), null, null);
        } finally {
			JPA.getUtil().closeConnection();
		}
    }

    /**
     * Method tries to log in the user with given <code>username</code> and <code>password</code>
     * @param loginRequestJSon - object wrapper for example {username: 'aaa',password:{bbb}, deviceToken:{ccc}}
     * @return - result of the operation
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("loginJson")
    public String loginJson(String loginRequestJSon) {
        ObjectMapper objectMapper = new ObjectMapper();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {

            LOGGER.info("Begin loginJson in ...");
            if(loginRequestJSon == null){
                //return Response.status(Response.Status.BAD_REQUEST).entity("No data in the request body!").build();
                objectMapper.writeValue(outputStream, new LoginResponse(DocuWorkRestResponse.STATUS.WARNING,"",  "Липсва заявка!", null, null));
                return new String(outputStream.toByteArray());
            }

            LoginRequest loginRequestAsObject = objectMapper.readValue(loginRequestJSon, LoginRequest.class);

            LoginResponse loginResponse = loginImplementation(loginRequestAsObject.getUsername(), loginRequestAsObject.getPassword());
            objectMapper.writeValue(outputStream, loginResponse);

            LOGGER.info("Begin loginJson in ...done!");
            
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
            
            //return new String(outputStream.toByteArray());
        }catch (Exception e){
            LOGGER.error(e.getMessage());
            return buildErrorResponse(objectMapper, outputStream, new LoginResponse(DocuWorkRestResponse.STATUS.ERROR,null,e.getMessage(), null, null));
        } finally {
        	JPA.getUtil().closeConnection();
		}
    }

    private LoginResponse loginImplementation(String username, String password) throws Exception {
        // validate the incoming parameters
        AdmUserDAO admUserDAO  = new AdmUserDAO(ActiveUser.DEFAULT);
        AdmUser admUser;
        try {
        	admUser = admUserDAO.validateUser(getSystemData(context), username, password, Boolean.TRUE, Boolean.FALSE);
		} catch (AuthenticationException e) {
			String errorMessage;

			switch (e.getCode()) {
			case AuthenticationException.CODE_FREE_TEXT:
				errorMessage = e.getMessage();
				break;

			case AuthenticationException.CODE_USER_UNKNOWN:
			case AuthenticationException.CODE_WRONG_PASSWORD:
				errorMessage = "Невалидно потребителско име и/или парола.";
				break;

			case AuthenticationException.CODE_USER_LOCKED:
				errorMessage = "Неуспешен вход. Потребител \""+username+"\" е блокиран!";
				break;

			case AuthenticationException.CODE_UNAUTHORIZED_STATUS:
				errorMessage = "Неуспешен вход. Потребител \""+username+"\" не е в активен статус!";
				break;

			case AuthenticationException.CODE_CHANGE_PASSWORD:
				errorMessage = "Необходима е смяна на паролата Ви. Моля, сменете паролата си през система е-Омбудсман.";
				break;

			default:
				errorMessage = "Unknown error code=" + e.getCode();
				break;
			}

			return new LoginResponse(DocuWorkRestResponse.STATUS.ERROR, null, errorMessage, 0, null);
		}
        
        // TODO here we have to check deviceToken parameter into other table linking user id to device token
        // 1. if it exists, do nothing
        // 2. if it doesn't exist, insert the value

        // build jwt token
        String jwtToken = buildJwtToken(admUser, usersServicesRespository);

        Integer registraturaId = (Integer) getSystemData(context)
        	.getItemSpecific(Constants.CODE_CLASSIF_ADMIN_STR, admUser.getId(), SysConstants.CODE_DEFAULT_LANG, null
        		, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
        
        Integer notificationCount = new UserNotificationsDAO()
        	.countUserNotifications(admUser.getId(), OmbConstants.CODE_NOTIF_STATUS_NEPROCHETENA, registraturaId);

        return new LoginResponse(DocuWorkRestResponse.STATUS.SUCCESS, jwtToken, null, admUser.getId(), notificationCount);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("logoutJson")
    public String logoutJson(String logoutRequestString){
        ObjectMapper objectMapper = new ObjectMapper();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
        	LogoutRequest logoutRequest = objectMapper.readValue(logoutRequestString, LogoutRequest.class);

            final int userId = extractUserIdFromJwtToken(logoutRequest.getJwtToken());
            usersServicesRespository.logoff(userId);

            LogoutResponse logoutResponse = new LogoutResponse(DocuWorkRestResponse.STATUS.SUCCESS,"Успешен изход!");
            
            if(logoutRequest.getDeviceToken() != null) {
            	JPA.getUtil().runInTransaction(() -> deleteDeviceId(logoutRequest.getDeviceToken(), userId));
            }

            objectMapper.writeValue(outputStream, logoutResponse);
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return buildErrorResponse(objectMapper, outputStream, new LogoutResponse(DocuWorkRestResponse.STATUS.ERROR, e.getMessage()));
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    @Path("logoutXML")
    public LogoutResponse logoutXML(LogoutRequest logoutRequest){
        try {
            final int userId = extractUserIdFromJwtToken(logoutRequest.getJwtToken());
            usersServicesRespository.logoff(userId);

            return new LogoutResponse(DocuWorkRestResponse.STATUS.SUCCESS,"Успешен изход!");
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return new LogoutResponse( DocuWorkRestResponse.STATUS.ERROR,e.getMessage());
        }
    }    
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("receiveFcmDeviceToken")
    public String receiveFcmDeviceToken(String fcmTokenRequest) {
    	ObjectMapper objectMapper = new ObjectMapper();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try {
        	
            LOGGER.info("Begin receiveFcmDeviceToken in ...");
          
            FcmTokenRequest loginRequestAsObject = objectMapper.readValue(fcmTokenRequest, FcmTokenRequest.class);
            DocuWorkRestResponse fcmTokenResponse = handleNewTokenRequest(loginRequestAsObject);
            objectMapper.writeValue(outputStream, fcmTokenResponse);
            
            LOGGER.info("Begin receiveFcmDeviceToken in ...done!");
            
            return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
        }
        catch (Exception e){
            LOGGER.error(e.getMessage());
            return buildErrorResponse(objectMapper, outputStream, new DocuWorkRestResponse(DocuWorkRestResponse.STATUS.ERROR,e.getMessage()));
        } 
        finally {
        	JPA.getUtil().closeConnection();
		}
    }
    
    
    private DocuWorkRestResponse handleNewTokenRequest(FcmTokenRequest fcmTokenRequest) {
    	try {
	    	int userId = extractUserIdFromJwtToken(fcmTokenRequest.getJwtToken());
	    	JPA.getUtil().runInTransaction(() -> insertDeviceId(fcmTokenRequest.getFcmDeviceToken(), userId));
	    	return new DocuWorkRestResponse(DocuWorkRestResponse.STATUS.SUCCESS, null);
    	}
    	catch(Exception e) {
    		LOGGER.error(e.getMessage());
    		return new DocuWorkRestResponse(DocuWorkRestResponse.STATUS.ERROR, e.getMessage());
    	}
    	
    }
    
    private void insertDeviceId(String deviceId, Integer userId) throws DbErrorException {
    	try {
			JPA.getUtil().getEntityManager().createNativeQuery("delete from MOBILE_LOGINS where DEVICE_ID = ?1")
				.setParameter(1, deviceId).executeUpdate();
			
			JPA.getUtil().getEntityManager().createNativeQuery(
				"INSERT INTO MOBILE_LOGINS(DEVICE_ID, USER_ID, LOGIN_TIME) VALUES(?1, ?2, ?3)")
				.setParameter(1, deviceId).setParameter(2, userId).setParameter(3, new Date())
				.executeUpdate();

			// журналиране вход
			String identObject = "DEVICE_ID=" + deviceId;
			SystemJournal journal = new SystemJournal(userId, CODE_DEIN_LOGIN, Constants.CODE_ZNACHENIE_JOURNAL_USER, userId, identObject, null);
			
			new AdmUserDAO(ActiveUser.of(userId)).saveAudit(journal);

    	} catch (Exception e) {
			throw new DbErrorException("Грешка при запис на вход през мобилно устройство", e);
		}
    }
    
    private void deleteDeviceId(String deviceId, Integer userId) throws DbErrorException {
    	try {
			JPA.getUtil().getEntityManager().createNativeQuery("delete from MOBILE_LOGINS where DEVICE_ID = ?1")
				.setParameter(1, deviceId).executeUpdate();

			// журналиране изход
			String identObject = "DEVICE_ID=" + deviceId;
			SystemJournal journal = new SystemJournal(userId, CODE_DEIN_LOGOUT, Constants.CODE_ZNACHENIE_JOURNAL_USER, userId, identObject, null);
			
			new AdmUserDAO(ActiveUser.of(userId)).saveAudit(journal);
			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при запис на изход през мобилно устройство", e);
		}
    }
}
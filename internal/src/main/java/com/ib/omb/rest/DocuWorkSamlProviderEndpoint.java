package com.ib.omb.rest;

import com.ib.system.BaseSystemData;
import com.ib.system.auth.EAuthCredential;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.rest.SamlRestEndpoint;
import com.ib.system.saml.SamlConstants;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.security.enterprise.SecurityContext;
import javax.security.enterprise.credential.Credential;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Properties;

/**
 * Current Saml Rest Implementation
 * @author ilukov
 */
@Path("/eauth")
public class DocuWorkSamlProviderEndpoint extends SamlRestEndpoint {


    @Inject
    private ServletContext context;

    @Inject
    private SecurityContext securityContext;

    @Context
    private HttpServletRequest request;

    @Context
    private HttpServletResponse response;

    @Context
    private UriInfo info;

    @Override
    protected Properties loadSamlProviderPropeties() throws IllegalAccessException {
        Properties properties = new Properties();
        for(Field field : SamlConstants.class.getFields()) {
            String fieldValue = field.get(null).toString();// static access
            try {
                String settingValue = getSystemData().getSettingsValue(fieldValue);
                if(settingValue != null) {
                    properties.put(fieldValue, settingValue);
                }
            } catch (DbErrorException ignored) {}

        }
        return properties;
    }

    @Override
    public void logOperation(Long codeAction, Long codeObject, Long idObject, Long idUser, byte[] objectContent, String objectXml) {
        SystemJournal systemJournal = new SystemJournal();
        if(codeAction != null) {
            systemJournal.setCodeAction(codeAction.intValue());
        }
        if(codeObject != null) {
            systemJournal.setCodeObject(codeObject.intValue());
        }
        systemJournal.setDateAction(new Date());
        if(idObject != null) {
            systemJournal.setIdObject(idObject.intValue());
        }
        if(idUser != null) {
            systemJournal.setIdUser(idUser.intValue());
        }
        systemJournal.setObjectContent(objectContent);
        systemJournal.setObjectXml(objectXml);

        EntityManager entityManager = JPA.getUtil().getEntityManager();
        try {
            JPA.getUtil().begin();
            entityManager.persist(systemJournal);
            JPA.getUtil().commit();
        } catch (DbErrorException e) {
            JPA.getUtil().rollback();
        }
    }

    @Override
    protected BaseSystemData getSystemData() {
        return (BaseSystemData) this.context.getAttribute("systemData");
    }

    protected SecurityContext getSecurityContext(){
        return securityContext;
    }

    @Override
    protected HttpServletRequest getHttpServletRequest() {
        return request;
    }

    @Override
    protected HttpServletResponse getHttpServletResponse() {
        return response;
    }

    /*
     * Method had to be overridden due to different column names into adm_user table
     */
    @Override
    protected Credential findUserByEmail(EntityManager manager, String email) throws DbErrorException {
    	//Query q = manager.createNativeQuery("select u.user_id from ADM_USERS u join ADM_USER_CERTS uc on uc.user_id=u.user_id and uc.email=?").setParameter(1, email);
    	Query q = manager.createNativeQuery("select u.user_id from ADM_USERS u where u.email=?").setParameter(1, email);
        //Number userIdNumber = (Number) q.getSingleResult();
    	if(q.getResultList() == null || q.getResultList().isEmpty())
    		throw new DbErrorException("Няма намерен потребител!");
    		
    	Number userIdNumber = (Number) q.getResultList().get(0);
        if(userIdNumber != null) {
            return new EAuthCredential(userIdNumber.intValue());
        }else{
            throw new DbErrorException("Няма намерен потребител!");
        }
    }

    @Override
    protected String getSuccessPage() {
        return "/pages/dashboard.xhtml";
    }

    @Override
    protected String getErrorPage() {
        return "/loginError.xhtml";
    }
}

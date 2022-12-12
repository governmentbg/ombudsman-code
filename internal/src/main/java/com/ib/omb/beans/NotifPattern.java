package com.ib.omb.beans;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.MailPatternsDAO;
import com.ib.omb.db.dto.NotificationPatternVariables;
import com.ib.omb.db.dto.NotificationPatterns;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.persistence.EntityTransaction;
import java.io.Serializable;

@ViewScoped
@Named
public class NotifPattern extends IndexUIbean implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 5633567469833894258L;

	private static final Logger LOGGER = LoggerFactory.getLogger(NotifPattern.class);

    private NotificationPatterns notifPattern;
	private MailPatternsDAO notifPatternDAO = new MailPatternsDAO(ActiveUser.of(getUserData().getUserId()));

	// for edit
    private String varName;
    private String varRefl;
    private Integer codeClassif;

    private NotificationPatternVariables editVariable;

    @PostConstruct
    public void postConstruct(){
        notifPattern = (NotificationPatterns) JSFUtils.getFlashScopeValue("notifPattern");
    }

    public void addNewNotifPatternVariable(){
        // setup edit variable
        varName = null;
        varRefl = null;
        codeClassif = null;
    }

    public void editNotifPattern(NotificationPatternVariables mailPattern){
        editVariable = mailPattern;
    }

    public void saveNotifPattern(){
        EntityTransaction entityTransaction = JPA.getUtil().getEntityManager().getTransaction();
        try {

            if(notifPattern.getEventId() == null){
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, "Моля въведете събитие за нотификацията");
                return;
            }

            if(notifPattern.getRolia() == null){
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, "Моля въведете роля за нотификацията");
                return;
            }

            if(notifPattern.getSubject() == null || notifPattern.getSubject().trim().length() == 0){
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, "Моля въведете заглавие за нотификацията");
                return;
            }

            if(notifPattern.getBody() == null || notifPattern.getBody().trim().length() == 0){
                JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, "Моля въведете текст за нотификацията");
                return;
            }

            if(notifPattern.getVariables() != null){
                for(NotificationPatternVariables variables : notifPattern.getVariables()){
                    if(variables.getVarName() == null || variables.getVarName().trim().length() == 0){
                        JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, "Моля въведете име на променливата в нотификацията");
                        return;
                    }

                    if(variables.getVarRefl() == null || variables.getVarRefl().trim().length() == 0){
                        JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, "Моля въведете стойност на променливата в нотификацията");
                        return;
                    }
                }
            }

            entityTransaction.begin();
            notifPattern = notifPatternDAO.save(notifPattern);
            entityTransaction.commit();

            JSFUtils.addInfoMessage("Записът е успешен");

            PrimeFaces primeFaces = PrimeFaces.current();
            primeFaces.executeScript("PF('newNotifPatternVariables').hide();");
        } catch (DbErrorException e) {
            entityTransaction.rollback();
            JSFUtils.addErrorMessage("Записът е неуспешен");
            LOGGER.error(e.getMessage(), e);
        } finally {
            JPA.getUtil().closeConnection();
        }
    }

    public void saveNotifPatternVariable(){
        // check if there is editVariable that has been initialized before
        // if the variable is initialized and is not present into the variable list, add it
        EntityTransaction entityTransaction = JPA.getUtil().getEntityManager().getTransaction();
        try {
            if (editVariable.getVarName() == null || editVariable.getVarName().trim().length() == 0) {
                JSFUtils.addMessage("notifPatternForm:varName",FacesMessage.SEVERITY_WARN, "Моля въведете име на променливата в нотификацията");
                return;
            }
            if (editVariable.getVarRefl() == null || editVariable.getVarRefl().trim().length() == 0) {
                JSFUtils.addMessage("notifPatternForm:varRefl",FacesMessage.SEVERITY_WARN, "Моля въведете стойност на променливата в нотификацията");
                return;
            }

            entityTransaction.begin();
            notifPattern = notifPatternDAO.save(notifPattern);
            entityTransaction.commit();

            JSFUtils.addInfoMessage("Записът е успешен");

            PrimeFaces primeFaces = PrimeFaces.current();
            primeFaces.executeScript("PF('newEditNotifPatternVariables').hide();");

        }catch (Exception e){
            entityTransaction.rollback();
            JSFUtils.addErrorMessage("Записът на променлива е неуспешен");
            LOGGER.error(e.getMessage(), e);
        } finally {
            JPA.getUtil().closeConnection();
        }

    }

    public void saveNewNotifPatternVariable(){
        NotificationPatternVariables notificationPatternVariables = new NotificationPatternVariables();

        if(varName == null || varName.trim().length() == 0){
            JSFUtils.addMessage("notifPatternForm:varNameNew", FacesMessage.SEVERITY_WARN, "Моля въведете име на променливата в нотификацията");
            return;
        }

        if(varRefl == null || varRefl.trim().length() == 0){
            JSFUtils.addMessage("notifPatternForm:varNameNew", FacesMessage.SEVERITY_WARN, "Моля въведете стойност на променливата в нотификацията");
            return;
        }

        notificationPatternVariables.setPattern(notifPattern);
        notificationPatternVariables.setVarName(varName);
        notificationPatternVariables.setVarRefl(varRefl);
        notificationPatternVariables.setCodeClassif(codeClassif);

        notifPattern.getVariables().add(notificationPatternVariables);

        EntityTransaction entityTransaction = JPA.getUtil().getEntityManager().getTransaction();
        try{
            entityTransaction.begin();
            notifPattern = notifPatternDAO.save(notifPattern);
            entityTransaction.commit();

            JSFUtils.addInfoMessage("Записът е успешен");

            PrimeFaces primeFaces = PrimeFaces.current();
            primeFaces.executeScript("PF('newNotifPatternVariables').hide();");
        }catch (Exception e){
            entityTransaction.rollback();
            JSFUtils.addErrorMessage("Записът на променлива е неуспешен");
            LOGGER.error(e.getMessage(), e);
        } finally {
            JPA.getUtil().closeConnection();
        }
    }

    public void deleteNotifVariable(NotificationPatternVariables mailVariable){
        // delete variable
    	notifPattern.getVariables().remove(mailVariable);
        EntityTransaction entityTransaction = JPA.getUtil().getEntityManager().getTransaction();
        try {
            entityTransaction.begin();
            notifPattern = notifPatternDAO.save(notifPattern);
            entityTransaction.commit();

            JSFUtils.addInfoMessage("Променливата е изтрита");
        } catch (DbErrorException e) {
            entityTransaction.rollback();
            JSFUtils.addErrorMessage("Променливата не е изтрита");
            LOGGER.error(e.getMessage(),e);
        } finally {
            JPA.getUtil().closeConnection();
        }
    }
    
    public NotificationPatterns getNotifPattern() {
		return notifPattern;
	}

	public void setNotifPattern(NotificationPatterns notifPattern) {
		this.notifPattern = notifPattern;
	}

    public NotificationPatternVariables getEditVariable() {
        return editVariable;
    }

    public void setEditVariable(NotificationPatternVariables editVariable) {
        this.editVariable = editVariable;
    }

    public String getVarName() {
        return varName;
    }

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public String getVarRefl() {
        return varRefl;
    }

    public void setVarRefl(String varRefl) {
        this.varRefl = varRefl;
    }

    public Integer getCodeClassif() {
        return codeClassif;
    }

    public void setCodeClassif(Integer codeClassif) {
        this.codeClassif = codeClassif;
    }
}

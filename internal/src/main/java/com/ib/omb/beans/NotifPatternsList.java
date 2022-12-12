package com.ib.omb.beans;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.MailPatternsDAO;
import com.ib.omb.db.dto.NotificationPatterns;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import java.io.Serializable;
import java.util.List;

@ViewScoped
@Named
public class NotifPatternsList extends IndexUIbean implements Serializable {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotifPatternsList.class);

    private MailPatternsDAO mailPatternsDAO = new MailPatternsDAO(ActiveUser.of(getCurrentUserId()));

    private List<NotificationPatterns> notifPatterns;
    private Integer eventId;
    private Integer rolia;

    public String newNotifPattern(){
        String outcome = null;
        // check if there is already mail pattern with given eventId and rolia
        // if the pattern exists then modify it, else create new instance
        EntityManager entityManager = JPA.getUtil().getEntityManager();
        try {
            entityManager.createQuery("select mp from NotificationPatterns mp where mp.eventId=:eventId and mp.rolia=:rolia", NotificationPatterns.class)
                    .setParameter("eventId", eventId).setParameter("rolia", rolia).getSingleResult();

            // we are not fine
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, "В базата съществува нотификация за избраново събитие и роля!");
        }catch (NoResultException e) {
            // we are fine
            notifPatterns = null;
            NotificationPatterns notificationPatterns = new NotificationPatterns();
            notificationPatterns.setEventId(eventId);
            notificationPatterns.setRolia(rolia);
            JSFUtils.addFlashScopeValue("notifPattern", notificationPatterns);
            // go to the next page
            outcome = "notifPattern.xhtml?faces-redirect=true";
        } catch (NonUniqueResultException e){
            // database inconsistency
            LOGGER.error(e.getMessage(),e);
            JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, "Проблем в базата! Съществуват повече от една нотификация за избраното събитие и роля!");
        } finally {
            JPA.getUtil().closeConnection();
        }

        return outcome;
    }

    public String editNotifPattern(NotificationPatterns notifPattern){
        JSFUtils.addFlashScopeValue("notifPattern", notifPattern);
        return "notifPattern.xhtml?faces-redirect=true";
    }

    public void deleteNotifPattern(Integer notifPatternId){
        EntityTransaction entityTransaction = JPA.getUtil().getEntityManager().getTransaction();
        try {
            entityTransaction.begin();
            mailPatternsDAO.deleteById(notifPatternId);
            entityTransaction.commit();

            notifPatterns = null; // reload the list

            JSFUtils.addInfoMessage("Темплейтът е изтрит");
        } catch (DbErrorException | ObjectInUseException e) {
            entityTransaction.rollback();
            LOGGER.error(e.getMessage(),e);
            JSFUtils.addErrorMessage("Темплейтът е неизтрит");
        } finally {
            JPA.getUtil().closeConnection();
        }
    }

    public Integer getEventId() {
        return eventId;
    }

    public void setEventId(Integer eventId) {
        this.eventId = eventId;
    }

    public Integer getRolia() {
        return rolia;
    }

    public void setRolia(Integer rolia) {
        this.rolia = rolia;
    }

    public List<NotificationPatterns> getNotifPatterns() {
        if(notifPatterns == null){
            try {
                notifPatterns = JPA.getUtil().getEntityManager().createQuery("select np from NotificationPatterns np order by np.rolia asc", NotificationPatterns.class).getResultList();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(),e);
                JSFUtils.addErrorMessage("Грешка при изтегляне на темплейти");
            } finally {
                JPA.getUtil().closeConnection();
            }
        }
        return notifPatterns;
    }

    public void setNotifPatterns(List<NotificationPatterns> notifPatterns) {
        this.notifPatterns = notifPatterns;
    }
}

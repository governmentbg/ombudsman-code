package com.ib.omb.quartz;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.persistence.Query;
import javax.servlet.ServletContext;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.TaskDAO;
import com.ib.omb.db.dto.Task;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.mail.Mailer;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.StringUtils;

/**
 * Class sends mails to predefined destinations
 * @author ilukov
 */
@DisallowConcurrentExecution
public class SendMailJob implements Job {

    public static final String MAIL_SMTP_HOST = "mail.smtp.host";
    public static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
    public static final String MAIL_SMTP_PORT = "mail.smtp.port";
    public static final String MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";

    public static final String MAIL_IMAP_HOST = "mail.imap.host";
    public static final String MAIL_IMAP_AUTH = "mail.imap.auth";
    public static final String MAIL_IMAP_PORT = "mail.imap.port";
    public static final String MAIL_IMAP_STARTTLS_ENABLE = "mail.imap.starttls.enable";

    public static final String MAIL_TRANSPORT_PROTOCOL ="mail.transport.protocol";
    public static final String MAIL_STORE_PROTOCOL = "mail.store.protocol";


    public static final String MAIL_FROM = "mail.from";
    public static final String USER_NAME = "user.name";
    public static final String USER_PASSWORD = "user.password";

    // used in loop for easier getting of mail property values
    public static final String[] mailPropertyKeys = new String[] {
            MAIL_SMTP_HOST,
            MAIL_SMTP_AUTH,
            MAIL_SMTP_PORT,
            MAIL_SMTP_STARTTLS_ENABLE,
            MAIL_IMAP_HOST,
            MAIL_IMAP_AUTH,
            MAIL_IMAP_PORT,
            MAIL_IMAP_STARTTLS_ENABLE,
            MAIL_TRANSPORT_PROTOCOL,
            MAIL_STORE_PROTOCOL,
            MAIL_FROM,
            USER_NAME,
            USER_PASSWORD
    };


    private static final Logger LOGGER = LoggerFactory.getLogger(SendMailJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("==== Start SendMailJob ===");
        executeTask(context);
        LOGGER.info("==== End SendMailJob ===");
    }

    @SuppressWarnings("unchecked")
	public void executeTask(JobExecutionContext context) throws JobExecutionException{
        try {
            ServletContext servletContext = (ServletContext) context.getScheduler().getContext().get("servletContext");
            if (servletContext == null) {
                LOGGER.info("********** servletcontext is null **********");
                return;
            }

            SystemData sd = (SystemData) servletContext.getAttribute("systemData");
            try {

                Properties mailProps = new Properties();
                for(String mailProperty : mailPropertyKeys){
                    String mailPropertyValue = sd.getSettingsValue(mailProperty);

                    if(mailPropertyValue != null) {
                        mailProps.put(mailProperty, mailPropertyValue);
                    }
                }

                Mailer mailer3 = new Mailer();

                String sqlUserNotifications = "SELECT" +
                        " ID, " +
                        " USER_ID," +                        
                        " TITLE, " +
                        " DETAILS, " +                        
                        " TASK_ID, " +
                        " OBJECT_CODE, " +
                        " OBJECT_ID, " +
                        " EMAIL_TO " +
                        " FROM USER_NOTIFICATIONS " +
                        " WHERE SENT_TO_EMAIL=" + OmbConstants.CODE_ZNACHENIE_STATUS_MAIL_NOT_SEND;

                Query qMails = JPA.getUtil().getEntityManager().createNativeQuery(sqlUserNotifications);
                List<Object[]> mailsForSending = (List<Object[]>)qMails.getResultList();


                JPA.getUtil().begin();

                for(Object[] rowMail: mailsForSending) {

                	ArrayList<DataSource> filesDS = new ArrayList<DataSource>();
                    String error = "";

                    Integer id  = SearchUtils.asInteger(rowMail[0]);
                    Integer idUser  = SearchUtils.asInteger(rowMail[1]);
                    String messageTitle = SearchUtils.asString(rowMail[2]);
                    String messageDetails = SearchUtils.asString(rowMail[3]);
                    Integer taskId  = SearchUtils.asInteger(rowMail[4]);
                    Integer objectCode  = SearchUtils.asInteger(rowMail[5]);
                    Integer objectId  = SearchUtils.asInteger(rowMail[6]);                    
                    String messageMailTo = SearchUtils.asString(rowMail[7]);
                    
                    if (idUser == null) {
                    	
                    	FilesDAO fdao = new FilesDAO(ActiveUser.DEFAULT);
                    	SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                    	
                    	//Нотификация до хора, които не са потребители на системата.
                    	if (taskId != null) {
                    		
                    		TaskDAO tdao = new TaskDAO(ActiveUser.DEFAULT);
                    		
                    		Task task = tdao.findById(taskId);
                    		if (task != null) {
                    			messageDetails += "<br><br>";
                    			String vazlojitel = sd.decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR_REPORTS, task.getCodeAssign(), OmbConstants.CODE_DEFAULT_LANG, task.getAssignDate());
                    			//LOGGER.error("----------> VAZLOJITEL="+vazlojitel);
                    			if (vazlojitel != null) {
                    				messageDetails += "Възложител: " +  vazlojitel + "<br>";
                    			}
                    			messageDetails += "Дата на възлагане: " +  sdf.format(task.getAssignDate()) + "<br>";
                    			if (task.getSrokDate() != null) {
                    				messageDetails += "Срок за изпълнение: " +  sdf.format(task.getSrokDate()) + "<br>";
                    			}
                    			if (task.getDocRequired() != null && task.getDocRequired().equals(OmbConstants.CODE_ZNACHENIE_DA)) {
                    				messageDetails += "Изисква се документ при приключване на задачата.<br>";
                    			}
                    			
                    			List<Files> files = fdao.selectByFileObject(taskId, OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK);
                    			for (Files curFile : files) {
                    				curFile = fdao.findById(curFile.getId());
                    				
                    				ByteArrayDataSource ds = new ByteArrayDataSource(curFile.getContent(), curFile.getContentType());
                					String fileName = SearchUtils.asString(curFile.getFilename());
                					
                					ds.setName(fileName);
                					filesDS.add(ds);                    			
                				}
                    			
                    			if (task.getDocId() != null) {
	                    			files = fdao.selectByFileObject(task.getDocId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
	                    			for (Files curFile : files) {
	                    				curFile = fdao.findById(curFile.getId());
	                    				
	                    				ByteArrayDataSource ds = new ByteArrayDataSource(curFile.getContent(), curFile.getContentType());
	                					String fileName = SearchUtils.asString(curFile.getFilename());
	                					
	                					ds.setName(fileName);
	                					filesDS.add(ds);      
	                    			}
                				}
                    			
                    			
                    		}
                    	}
                    	
                    	if (objectCode != null && objectId != null && objectCode.equals( OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC)) {
                    		List<Files> files = fdao.selectByFileObject(objectId, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
                			for (Files curFile : files) {
                				curFile = fdao.findById(curFile.getId());
                				
                				ByteArrayDataSource ds = new ByteArrayDataSource(curFile.getContent(), curFile.getContentType());
            					String fileName = SearchUtils.asString(curFile.getFilename());
            					
            					ds.setName(fileName);
            					filesDS.add(ds);                    			
            				}
                    	}
                    }
                    
                    messageDetails = messageDetails.replace("\r\n", "<br>");
                    messageDetails = messageDetails.replace("\n", "<br>");

                    long status = OmbConstants.CODE_ZNACHENIE_STATUS_MAIL_SEND;
                    try {
                        ArrayList<String> mail = new ArrayList(Arrays.asList(messageMailTo.split(";")));
                        mailer3.sent(Mailer.Content.HTML, mailProps, mailProps.getProperty("user.name"), mailProps.getProperty("user.password"),
                                mailProps.getProperty("mail.from"), " ", mail, messageTitle, messageDetails, filesDS);
                    } catch (Exception e) {
                        LOGGER.error(e.getClass().getName(), e);
                        error = StringUtils.stack2string(e);
                        status = OmbConstants.CODE_ZNACHENIE_STATUS_MAIL_ERROR;
                    } 

                    Query queryUpdate = JPA.getUtil().getEntityManager().createNativeQuery ("update USER_NOTIFICATIONS set SENT_TO_EMAIL=? where id =?");
                    queryUpdate.setParameter(1, status ); // status
//                        queryUpdate.setParameter(2, new Date() );
//                        queryUpdate.setParameter(3, error );
//                        queryUpdate.setParameter(4,  count);
                    queryUpdate.setParameter(2,  id); // id
                    queryUpdate.executeUpdate();

                    JPA.getUtil().flush();



                }

                JPA.getUtil().commit();

            } catch (Exception e) {
                LOGGER.error("Error saving mail Object", e);
                JPA.getUtil().rollback();
            }


        }catch (Exception e){
            LOGGER.error("Възникна грешка при извличане на съобщения за изпращене!!!", e);
            throw new JobExecutionException(e);
        } finally {
            JPA.getUtil().closeConnection();
        }
    }
}

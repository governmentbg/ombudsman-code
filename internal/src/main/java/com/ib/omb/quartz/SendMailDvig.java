package com.ib.omb.quartz;

import java.sql.Blob;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;
import javax.persistence.Query;
import javax.servlet.ServletContext;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.ReferentDAO;
import com.ib.omb.db.dao.RegistraturaDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocReferent;
import com.ib.omb.db.dto.EgovMessages;
import com.ib.omb.db.dto.EgovMessagesCoresp;
import com.ib.omb.db.dto.EgovMessagesFiles;
import com.ib.omb.db.dto.Referent;
import com.ib.omb.db.dto.Registratura;
import com.ib.omb.seos.MessageType;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;
import com.ib.system.mail.Mailer;
import com.ib.system.mail.Mailer.Content;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.StringUtils;



/**
 * Изпращане на движения по eMail
 * @author krasi
 *
 */
public class SendMailDvig implements Job {
	private static final Logger LOGGER = LoggerFactory.getLogger(SendMailDvig.class);
	
	
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {		
	
		LOGGER.debug("Start of SendMailDvig.execute()");
		
					
		SystemData systemData = null;
		
			try {
				ServletContext servletContext = (ServletContext) context.getScheduler().getContext().get("servletContext");
				if (servletContext==null) {
					LOGGER.debug("********** There is a problem !!!!! servletcontext is null **********");
					throw new JobExecutionException("There is a problem !!!!! servletcontext is null");
				}			
				systemData = (SystemData) servletContext.getAttribute("systemData");
				
				if (systemData == null) {
					LOGGER.error("SystemData is NULL !!!");
					throw new JobExecutionException("SystemData is NULL !!!");
				}
			} catch (SchedulerException e) {
				LOGGER.error("Грешка при извличане на ServletContext", e);
				throw new JobExecutionException("Грешка при извличане на ServletContext", e);
			}			
			
		
			
		//Екземпляри по емейл
		processEmail(systemData);
		processCEOC(systemData);
		//СЕОС
		
		

			
		
		LOGGER.debug("End of SendMailDvig.execute()");
	}
	
	
	
	/*
	 * генериране на основния текст на документа, който се изпраща...... КОПИРАНО ПО ИСКАНЕ ОТ СТАРОТО ДЕЛОВОДСТВО
	 */
	public String[] generateOsnMsgDoc(Doc tmpDoc, SystemData sd) throws DbErrorException {
		
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
		
		
		
		
		 String corespIme = null;
		if (tmpDoc.getCodeRefCorresp() != null) {
			Referent coresp = new ReferentDAO(ActiveUser.DEFAULT).findByCode(tmpDoc.getCodeRefCorresp(), tmpDoc.getDocDate(), false);
			corespIme = coresp.getRefName();
		}
		
		String dopAnot = "N: " + tmpDoc.getRnDoc() + "/" + sdf.format(tmpDoc.getDocDate()) ;
		if (corespIme != null) {
			dopAnot += ", Кореспондент: " + corespIme;
		} 
		
		
		String[] strMsgArr = new String[] {"",""};		
		String  strMsgDoc = "";
		//---------------- generirane teksta na dokumenta koito shte se izprashta ----------------------------------------------------
		
		strMsgDoc= "<table border=\"0\">"+
		    "    <tr>"+
		    "		<td style=\"width: 37px; \" align=left>"+
			"           Тип:" +
			"		</td>"+
			"       <td  style=\"width: 110px; color: navy\"><strong>"+
					sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_TYPE, tmpDoc.getDocType(), OmbConstants.CODE_DEFAULT_LANG, tmpDoc.getDocDate()) 	+					
			"       </strong></td>"+
		    "        <td  align=left>"+
		    "            Номер/дата документ: " +
		    "		 </td>"+
		    "        <td style=\"width: 260px; \">"+
		    "            <strong style=\"color: navy\">"+
		    	tmpDoc.getRnDoc() + "/" + sdf.format(tmpDoc.getDocDate()) + " &nbsp; " +
		    "            </strong> " +
		    "		 </td>"+
		    "    </tr>"+
		    "  </table>"+
			"  <table border=\"0\">"+    
			
			"   <tr>"+
			"       <td align=\"left\" style=\"width: 150px\">"+
			"                Вид на документа: " +
			"		</td>"+
			"       <td  style=\"color: navy\">"+
			sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, tmpDoc.getDocVid(), OmbConstants.CODE_DEFAULT_LANG, tmpDoc.getDocDate()) 	+
			"		</td>"+
			"   </tr>";
		if (corespIme != null) {
			strMsgDoc += "   <tr>"+
			"       <td align=\"left\" style=\"width: 150px\">"+
			"                Кореспондент: " +
			"		</td>"+
			"       <td  style=\"color: navy\">"+
			corespIme +
			"		</td>"+
			"   </tr>";
		}
		

		if (tmpDoc.getReceivedBy() != null && tmpDoc.getReceiveMethod() != null && tmpDoc.getReceiveMethod().equals(OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL)) {
			strMsgDoc += "   <tr>"+
			"       <td align=\"left\" style=\"width: 150px\">"+
			"                Идва от:" +
			"		</td>"+
			"       <td  style=\"color: navy\">"+
				tmpDoc.getReceivedBy();
			if (tmpDoc.getReceiveDate() != null) {
				strMsgDoc += "/"+ sdf.format(tmpDoc.getReceiveDate());
			}
			strMsgDoc +="		</td>"+
			"   </tr>";
		}
		
		if (tmpDoc.getReferentsAuthor() != null && tmpDoc.getReferentsAuthor().size() > 0) {
			String avtors = "";
			for (DocReferent ref : tmpDoc.getReferentsAuthor()) {
				avtors += sd.decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, ref.getCodeRef(), OmbConstants.CODE_DEFAULT_LANG, tmpDoc.getDocDate()) + "; ";
			}
			strMsgDoc += "   <tr>"+
			"       <td align=\"left\" style=\"width: 150px\">"+
			"                Автор: " +
			"		</td>"+
			"       <td  style=\"color: navy\">"+
			avtors+
			"		</td>"+
			"   </tr>";
		}
		
		strMsgDoc +=	"   <tr>"+
			"       <td style=\"width: 150px;\"  align=left valign=top>"+
			"                Относно:" +
			"		</td>"+
			"       <td  style=\"color: navy\">"+
				tmpDoc.getOtnosno()+ 
			"       </td>		"+			            
			"   </tr>"+
			"  </table>";
			
		
		System.out.println(strMsgDoc);
		strMsgArr[0] = strMsgDoc;
		strMsgArr[1] = dopAnot;
		return strMsgArr;
	}
	
	
	@SuppressWarnings("unchecked")
	public void processCEOC(SystemData sd) throws JobExecutionException  {		
		try {
			LOGGER.debug("Start of processCEOC");
			
			DocDAO dao = new DocDAO(ActiveUser.DEFAULT);
						
			Query q = JPA.getUtil().getEntityManager().createNativeQuery("select ID, DOC_ID, DVIJ_EMAIL from DOC_DVIJ where DVIJ_METHOD = :DM and status = :STAT order by DOC_ID");
			q.setParameter("DM", OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS);
			q.setParameter("STAT", OmbConstants.DS_WAIT_PROCESSING);
			ArrayList<Object[]> rows = (ArrayList<Object[]>) q.getResultList();
			LOGGER.debug("Number of dvigs for SEOS forming: " + rows.size());
			
			if (rows.size() == 0) {
				return;
			}
			
			
			Doc doc = null;
			ArrayList<Object[]> files = new ArrayList<Object[]> ();		
			for (Object[] row : rows) {		
				
				Integer newStatus= OmbConstants.DS_WAIT_SENDING;
				String statusInfo = null;
				Integer docId = SearchUtils.asInteger(row[1]);	
				Integer dvijId = SearchUtils.asInteger(row[0]);	
				
				try {	
					JPA.getUtil().begin();
					
					LOGGER.debug("Preparing dvij with id = " + dvijId);
					
					
					if (doc == null || !docId.equals(doc.getId())) {
						doc = dao.findById(docId);
						JPA.getUtil().getEntityManager().detach(doc);
						
						q = JPA.getUtil().getEntityManager().createNativeQuery("select FILENAME, CONTENT_TYPE, CONTENT from FILE_OBJECTS join FILES on  FILE_OBJECTS.FILE_ID = FILES.FILE_ID    where OBJECT_ID = :DOCID and OBJECT_CODE = :CO");
						q.setParameter("DOCID", docId);
						q.setParameter("CO", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
						
						files = (ArrayList<Object[]>) q.getResultList();						
					}
					
					EgovMessages mess = convertDoc2eGovMessage(doc, sd);
					if (mess != null) {
						JPA.getUtil().getEntityManager().persist(mess);
					}
										
					EgovMessagesCoresp newCoresp = null;
					ArrayList<EgovMessagesFiles> eFiles = new ArrayList<EgovMessagesFiles>();
					for (Object[] file : files) {
						EgovMessagesFiles eFile = new EgovMessagesFiles();
						eFile.setFilename(SearchUtils.asString(file[0]));
						eFile.setIdMessage(mess.getId());
						eFile.setMime(SearchUtils.asString(file[1]));
						if (file[2] != null) {
							Blob blob = (Blob) file[2];						
							int blobLength = (int) blob.length();  
							eFile.setBlobcontent(blob.getBytes(1, blobLength));							
							blob.free();
						}
						eFiles.add(eFile);
					}
					for (EgovMessagesFiles eFile : eFiles) {
						JPA.getUtil().getEntityManager().persist(eFile);
					}
					
					if (doc.getCodeRefCorresp() != null) {
						Referent coresp = new ReferentDAO(ActiveUser.DEFAULT).findByCode(doc.getCodeRefCorresp(), doc.getDocDate(), false);
						
						newCoresp = new EgovMessagesCoresp();
						
						if (coresp.getAddress() != null){							
							
							if (coresp.getAddress().getEkatte() != null){
								
								String item = sd.decodeItem(OmbConstants.CODE_CLASSIF_EKATTE, coresp.getAddress().getEkatte(), OmbConstants.CODE_LANG_BG, doc.getDocDate());
								if (item != null) {
									newCoresp.setCity(item);
								}else{
									newCoresp.setCity("Няма въведено!");
								}
								
								newCoresp.setAdres(coresp.getAddress().getAddrText());
							
							}else{
								newCoresp.setCity("Няма въведено!");								
							}						
						}
						
						newCoresp.setBulstat(coresp.getNflEik());				
						//newCoresp.setDopInfo(coresp.get);
						newCoresp.setEgn(coresp.getFzlEgn());
						newCoresp.setEmail(coresp.getContactEmail());
						newCoresp.setIme(coresp.getRefName());
						newCoresp.setPhone(coresp.getContactPhone());
						newCoresp.setIdMessage(mess.getId());
						
						JPA.getUtil().getEntityManager().persist(newCoresp);
					}
					
					JPA.getUtil().commit();
					JPA.getUtil().getEntityManager().detach(mess);
					if (newCoresp != null) {
						JPA.getUtil().getEntityManager().detach(newCoresp);
					}
					for (EgovMessagesFiles eFile : eFiles) {
						JPA.getUtil().getEntityManager().detach(eFile);
					}
					
				} catch (Exception e) {
					LOGGER.error("Грешка при генериране на съобщение за СЕОС" , e);
					newStatus= OmbConstants.DS_RETURNED_ERROR;
					statusInfo = "Грешка при генериране на съобщение за СЕОС\r\n" + StringUtils.stack2string(e);	
					System.out.println("------------------->" + statusInfo.length());
				}finally {
					JPA.getUtil().closeConnection();
				}
						
				//Промяна на движението на екземпляра
				
				try {	
					JPA.getUtil().begin();
					q = JPA.getUtil().getEntityManager().createNativeQuery("update doc_dvij set status = :STAT, STATUS_TEXT = :STATTEXT where id = :IDD");
					q.setParameter("STAT", newStatus);
					q.setParameter("IDD", dvijId);
					q.setParameter("STATTEXT", statusInfo);
					q.executeUpdate();

					StringBuilder ident = new StringBuilder();
					ident.append("Смяна на статуса на движение на документа на \"");
					ident.append(sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_PREDAVANE_STATUS, newStatus, SysConstants.CODE_DEFAULT_LANG, null) + "\".");
					if (statusInfo != null) {
						ident.append(" Допълнителна информация: " + statusInfo + ".");
					}
					SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ, dvijId, ident.toString());

					journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
					journal.setDateAction(new Date());
					journal.setIdUser(-1);
					journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
					journal.setJoinedIdObject1(docId);

					dao.saveAudit(journal);

					JPA.getUtil().commit();
				} catch (Exception e) {
					LOGGER.error("Грешка при смяна на статус на движение !" , e);
					JPA.getUtil().rollback();
					throw new JobExecutionException("Грешка при смяна на статус на движение !" , e);
				}finally {
					JPA.getUtil().closeConnection();
				}
			}
			
			
			
		} catch (JobExecutionException e) {			
			throw e;
			
		} catch (Exception e) {
			LOGGER.error("Грешка при изпълнение на метод за пращане на мейли !!!" , e);
			throw new JobExecutionException("Грешка при изпълнение на метод за пращане на мейли !!!" , e);
			
		}
		
		LOGGER.debug("End of processEmail");
	}
	
	@SuppressWarnings("unchecked")
	public void processEmail(SystemData sd) throws JobExecutionException  {		
		try {
			LOGGER.debug("Start of processEmail");
			
			DocDAO dao = new DocDAO(ActiveUser.DEFAULT);
			Mailer mailer = new Mailer();
			
			
			Properties props = sd.getMailProp(-1, "DEFAULT");
			if (props == null) {
				LOGGER.error("Properties за DEFAULT is NULL !!!");
				throw new JobExecutionException("Properties за DEFAULT is NULL !!!");
			}
			
			Query q = JPA.getUtil().getEntityManager().createNativeQuery("select ID, DOC_ID, DVIJ_EMAIL from DOC_DVIJ where DVIJ_EMAIL is not null and DVIJ_METHOD = :DM and status = :STAT order by DOC_ID");
			q.setParameter("DM", OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL);
			q.setParameter("STAT", OmbConstants.DS_WAIT_SENDING);
			ArrayList<Object[]> rows = (ArrayList<Object[]>) q.getResultList();
			LOGGER.debug("Number of dvigs for send: " + rows.size());
			Doc doc = null;
			String[] shablon = null;
			ArrayList<DataSource> filesDS = null;
			
			for (Object[] row : rows) {		
				
				Integer newStatus= OmbConstants.DS_SENT;
				String statusInfo = null;
				Integer docId = SearchUtils.asInteger(row[1]);	
				Integer dvijId = SearchUtils.asInteger(row[0]);	
				String email = SearchUtils.asString(row[2]);
				boolean hasError = false;
				try {	
					LOGGER.debug("Preparing dvij with id = " + dvijId);
					
					
					if (doc == null || !docId.equals(doc.getId())) {
						doc = dao.findById(docId);						
						shablon = generateOsnMsgDoc(doc, sd);
						JPA.getUtil().getEntityManager().detach(doc);
						q = JPA.getUtil().getEntityManager().createNativeQuery("select FILENAME, CONTENT_TYPE, CONTENT from FILE_OBJECTS join FILES on  FILE_OBJECTS.FILE_ID = FILES.FILE_ID    where OBJECT_ID = :DOCID and OBJECT_CODE = :CO");
						q.setParameter("DOCID", docId);
						q.setParameter("CO", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
						
						ArrayList<Object[]> files = (ArrayList<Object[]>) q.getResultList();
						if (files.size() > 0) {
							filesDS = new ArrayList<DataSource>();
							for (Object[] file : files) {
								byte[] blobAsBytes = null;
								if (file[2] != null) {
									Blob blob = (Blob) file[2];								
									int blobLength = (int) blob.length();  
									blobAsBytes = blob.getBytes(1, blobLength);
									blob.free();
								}
								ByteArrayDataSource ds = new ByteArrayDataSource(blobAsBytes, SearchUtils.asString(file[1]));
								String fileName = SearchUtils.asString(file[0]);
								
								ds.setName(fileName);
	    						filesDS.add(ds);
							}
						}
					}	
				}catch (Exception ex) {
					LOGGER.error("Грешка при подготвяне на мейл за изпращане" , ex);					
					hasError = true;
					newStatus= OmbConstants.DS_RETURNED_ERROR;
					statusInfo =  StringUtils.stack2string(ex);
				}
				
				if (!hasError) {
					//Ще изпращаме				
					try {
						String user = props.getProperty("user.name");
			            String pass = props.getProperty("user.password");
			            String from = props.getProperty("mail.from","noreply@delovodstvo.com");
						
						mailer.sent(Content.HTML, props, user, pass, from, "Деловодство", email, shablon[1], shablon[0], filesDS);
						
					}catch (Exception ex) {
						LOGGER.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!  Грешка при изпращане на поща до " + email, ex);
						newStatus= OmbConstants.DS_RETURNED_ERROR;
						statusInfo = StringUtils.stack2string(ex);
						
					}
				}
				
				try {
					JPA.getUtil().begin();
					q = JPA.getUtil().getEntityManager().createNativeQuery("update doc_dvij set status = :STAT, STATUS_TEXT = :STATTEXT where id = :IDD");
					q.setParameter("STAT", newStatus);
					q.setParameter("IDD", dvijId);
					q.setParameter("STATTEXT", statusInfo);
					q.executeUpdate();

					StringBuilder ident = new StringBuilder();
					ident.append("Смяна на статуса на движение на документа на \"");
					ident.append(sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_PREDAVANE_STATUS, newStatus, SysConstants.CODE_DEFAULT_LANG, null) + "\".");
					if (statusInfo != null) {
						ident.append(" Допълнителна информация: " + statusInfo + ".");
					}
					SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ, dvijId, ident.toString());

					journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
					journal.setDateAction(new Date());
					journal.setIdUser(-1);
					journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
					journal.setJoinedIdObject1(docId);

					dao.saveAudit(journal);

					JPA.getUtil().commit();
				} catch (Exception e) {
					LOGGER.error("Грешка при смяна на статус на движение !" , e);
					JPA.getUtil().rollback();
					throw new JobExecutionException("Грешка при смяна на статус на движение !" , e);
				}finally {
					JPA.getUtil().closeConnection();
				}
			}
			
			
			
		} catch (JobExecutionException e) {			
			throw e;
			
		} catch (Exception e) {
			LOGGER.error("Грешка при изпълнение на метод за пращане на мейли !!!" , e);
			throw new JobExecutionException("Грешка при изпълнение на метод за пращане на мейли !!!" , e);
			
		}
		
		LOGGER.debug("End of processEmail");
	}
	
	
	/**Конвертира Message от тип MSG_DOCUMENT_STATUS_REQUEST до ЕgovMessages 
	 * @param eGovMess
	 * @return
	 * @throws InvalidParameterException
	 * @throws DbErrorException 
	 */
	public EgovMessages convertDoc2eGovMessage(Doc doc, SystemData sd) throws InvalidParameterException, DbErrorException{
			
			EgovMessages mess = new EgovMessages();
			
			
			
			//ИНИЦИАЛИЗАЦИЯ
			mess.setMsgInOut(2);		
			mess.setMsgRegDate(new Date());
			mess.setMsgRn(null);
			mess.setMsgRnDate(null);			
			mess.setMsgStatusDate(new Date());
			mess.setMsgType(MessageType.MSG_DOCUMENT_REGISTRATION_REQUEST.value());
			mess.setMsgUrgent(1);
			mess.setCommStatus(1);
			mess.setMsgXml(null);
			mess.setMsgDate(new Date());
			mess.setMsgGuid("{"+java.util.UUID.randomUUID().toString().toUpperCase()+"}");
			mess.setMsgStatus("DS_WAIT_SENDING");
			mess.setSource("S_SEOS");
			mess.setDocSubject(doc.getOtnosno());
			
			
			
			if (doc.getDocVid() != null) {
				String vidDoc = sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, doc.getDocVid(), OmbConstants.CODE_LANG_BG, doc.getDocDate());
				if (vidDoc != null) {
					mess.setDocVid(vidDoc);
				}else {
					mess.setDocVid("ПИСМО");
				}
			}else {
				mess.setDocVid("ПИСМО");
			}
			
			
			Registratura reg = new RegistraturaDAO(ActiveUser.DEFAULT).findById(doc.getRegistraturaId());
			if (reg == null || reg.getGuidSeos() == null || reg.getOrgName() == null || reg.getOrgEik() == null) {
				throw new InvalidParameterException("За тази регистратура няма описани всички СЕОС настройки !");
			}
			mess.setSenderGuid(reg.getGuidSeos().toUpperCase());	
			mess.setSenderName(reg.getOrgName());
			mess.setSenderEik(reg.getOrgEik());
			
//			mess.setRecepientGuid(eGovMess.getHeader().getRecipient().getGUID().toUpperCase());	
//			mess.setRecepientName(eGovMess.getHeader().getRecipient().getAdministrativeBodyName());
//			mess.setRecepientEik(eGovMess.getHeader().getRecipient().getIdentifier());
//			

			
			mess.setDocDate(doc.getDocDate());
			mess.setDocGuid(doc.getGuid());	
			mess.setDocRn(doc.getRnDoc());
			
			return mess;			
	}
	
	

}

package com.ib.omb.observers;

import java.sql.Blob;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataSource;
import javax.enterprise.event.ObservesAsync;
import javax.inject.Inject;
import javax.mail.util.ByteArrayDataSource;
import javax.persistence.Query;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.EgovMessagesDAO;
import com.ib.omb.db.dao.ReferentDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocDvij;
import com.ib.omb.db.dto.Referent;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.ActiveUser;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.mail.Mailer;
import com.ib.system.mail.Mailer.Content;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.StringUtils;

//@Named
//@Eager
//@ApplicationScoped
public class DvijObserver {
	
	@Inject
	private ServletContext servletContext;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DvijObserver.class);
	
	public DvijObserver() {
		super();
		LOGGER.debug("DvijObserver constructor");
	}
	
	
	
	
	public void proccessDvij(@ObservesAsync DocDvij dvij) {
		
		try {
			if (dvij == null || dvij.getDvijMethod() == null) {
				return;
			}

			SystemData systemData = (SystemData) this.servletContext.getAttribute("systemData");

			switch(dvij.getDvijMethod()) {
				  case OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL:
					sendEmail(dvij, systemData);
				    break;
				  case OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS:					  
					  JPA.getUtil().runInTransaction(() -> new EgovMessagesDAO(ActiveUser.DEFAULT).saveRegistrationRequestMessage(dvij, systemData));
					  
				    break;
				  case OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV:
					  JPA.getUtil().runInTransaction(() -> new EgovMessagesDAO(ActiveUser.DEFAULT).saveNewSSEVMessage(dvij, systemData));				    break;
			}
			
			
			LOGGER.debug(systemData + " **************************************** Observes dvij to :"+dvij.getDvijText());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOGGER.error("!!!!",e);
		}
		
	}
	
	
	/*
	 * генериране на основния текст на документа, който се изпраща...... КОПИРАНО ПО ИСКАНЕ ОТ СТАРОТО ДЕЛОВОДСТВО
	 */
	private String[] generateOsnMsgDoc(Doc tmpDoc, SystemData sd) throws DbErrorException {
		
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
		    "            Номер/дата на документ: " +
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
		
	/*	
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
		*/
		
		strMsgDoc +=	"   <tr>"+
			"       <td style=\"width: 150px;\"  align=left valign=top>"+
			"                Относно:" +
			"		</td>"+
			"       <td  style=\"color: navy\">"+
				tmpDoc.getOtnosno()+ 
			"       </td>		"+			            
			"   </tr>"+
			"  </table>";
			
		
		LOGGER.debug("Message for mail:"+strMsgDoc);
		strMsgArr[0] = strMsgDoc;
		strMsgArr[1] = dopAnot;
		return strMsgArr;
	}
	
	
	
	@SuppressWarnings("unchecked")
	private void sendEmail(DocDvij dvij, SystemData systemData) throws DbErrorException {
		
		
		Integer newStatus= OmbConstants.DS_SENT;
		String statusInfo = null;
		Integer docId = SearchUtils.asInteger(dvij.getDocId());	
		Integer dvijId = SearchUtils.asInteger(dvij.getId());	
		String email = SearchUtils.asString(dvij.getDvijEmail());
		Mailer mailer = new Mailer();
		
		if (email == null || email.isEmpty()) {
			return;
		}
		
		
		boolean hasError = false;
		String[] shablon = null;
		
		Properties props = systemData.getMailProp(-1, "DEFAULT");
		if (props == null) {
			LOGGER.error("Properties за DEFAULT is NULL !!!");
			return;
		}

		DocDAO docDao = new DocDAO(ActiveUser.DEFAULT);

		ArrayList<DataSource> filesDS = null;
		try {	
			LOGGER.debug("Preparing dvij with id = " + dvijId);
			
			Doc doc = docDao.findById(docId);
			shablon = generateOsnMsgDoc(doc, systemData);
			if (shablon == null) {
				return;
			}
			
			Query q = JPA.getUtil().getEntityManager().createNativeQuery("select FILENAME, CONTENT_TYPE, CONTENT from FILE_OBJECTS join FILES on  FILE_OBJECTS.FILE_ID = FILES.FILE_ID    where OBJECT_ID = :DOCID and OBJECT_CODE = :CO and OFFICIAL = :ISOFFICIAL");
			q.setParameter("DOCID", docId);
			q.setParameter("CO", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
			q.setParameter("ISOFFICIAL", OmbConstants.CODE_ZNACHENIE_DA);
			
			ArrayList<Object[]> files = (ArrayList<Object[]>) q.getResultList();
			if (files.size() > 0) {
				
				String dialect = JPA.getUtil().getDbVendorName();
				filesDS  = new ArrayList<DataSource>();
				
				for (Object[] file : files) {
					
					byte[] contentAsBytes = null;
					
					if (dialect.indexOf("ORACLE") != -1) {
						Blob blob = (Blob) file[2];
						if (blob != null) {
							int blobLength = (int) blob.length();  
							contentAsBytes = blob.getBytes(1, blobLength);
							blob.free();
						}
					}
					else if (dialect.indexOf("POSTGRESQL") != -1) {
						contentAsBytes = (byte[]) file[2];
					}
					else {
						// друга база, да се напише според случая
						Blob blob = (Blob) file[2];
						if (blob != null) {
							int blobLength = (int) blob.length();  
							contentAsBytes = blob.getBytes(1, blobLength);
							blob.free();
						}
						
					}
					
					ByteArrayDataSource ds = new ByteArrayDataSource(contentAsBytes, SearchUtils.asString(file[1]));
					String fileName = SearchUtils.asString(file[0]);

					ds.setName(fileName);
					filesDS.add(ds);
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
//	            String escaped = StringEscapeUtils.escapeHtml("");
				mailer.sent(Content.HTML, props, user, pass, from, "Деловодство", email, shablon[1],
						 shablon[0] , filesDS);
				
			}catch (Exception ex) {
				LOGGER.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!  Грешка при изпращане на поща до " + email, ex);
				newStatus= OmbConstants.DS_RETURNED_ERROR;
				statusInfo = StringUtils.stack2string(ex);
				
			}
		}
		
		try {
			JPA.getUtil().begin();
			Query q = JPA.getUtil().getEntityManager().createNativeQuery("update doc_dvij set status = :STAT, STATUS_TEXT = :STATTEXT where id = :IDD");
			q.setParameter("STAT", newStatus);
			q.setParameter("IDD", dvijId);
			q.setParameter("STATTEXT", statusInfo);
			q.executeUpdate();

			StringBuilder ident = new StringBuilder();
			ident.append("Смяна на статуса на движение на документа на \"");
			ident.append(systemData.decodeItem(OmbConstants.CODE_CLASSIF_DOC_PREDAVANE_STATUS, newStatus, SysConstants.CODE_DEFAULT_LANG, null) + "\".");
			if (statusInfo != null) {
				ident.append(" Допълнителна информация: " + statusInfo + ".");
			}
			SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ, dvij.getId(), ident.toString());

			journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
			journal.setDateAction(new Date());
			journal.setIdUser(-1);
			journal.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
			journal.setJoinedIdObject1(dvij.getDocId());
			
			docDao.saveAudit(journal);

			JPA.getUtil().commit();
		} catch (Exception e) {
			LOGGER.error("Грешка при смяна на статус на движение !" , e);
			JPA.getUtil().rollback();
			throw e;
		}finally {
			JPA.getUtil().closeConnection();
		}
	
	}
	
	
	

}

package com.ib.omb.db.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dto.DocDvij;
import com.ib.omb.db.dto.EgovMessages;
import com.ib.omb.db.dto.EgovMessagesCoresp;
import com.ib.omb.db.dto.EgovMessagesFiles;
import com.ib.omb.db.dto.Referent;
import com.ib.omb.seos.MessageType;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.ActiveUser;
import com.ib.system.db.AbstractDAO;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;



public class EgovMessagesDAO extends AbstractDAO<EgovMessages> {

	
	private static final Logger LOGGER = LoggerFactory.getLogger(EgovMessagesDAO.class);
	
	/** @param user */
	public EgovMessagesDAO(ActiveUser user) {
		super(EgovMessages.class, user);
	}
	
	/** Изгражда sql за филтъра на чакащите за регистрация документи
	 * @param guidRegistraturа - guid на регистратура 
	 * @return SelectMetadata
	 */
	public SelectMetadata createFilterEgovMessages(String guidSeos, String guidSSEV, String tehNom ,boolean fullEq){
		
		SelectMetadata smd = new SelectMetadata();
		String dialect = JPA.getUtil().getDbVendorName().toUpperCase();
		
		StringBuilder select = new StringBuilder();
		StringBuilder from = new StringBuilder();
		StringBuilder where = new StringBuilder();
		Map<String, Object> params = new HashMap<String, Object>();
		
		
				
		if (guidSeos == null && guidSSEV == null) {		
			return null;
		}
		
		ArrayList<String> guids = new ArrayList<String>();
		if (guidSeos != null) {
			guids.add(guidSeos.toUpperCase());
		}
		if (guidSSEV != null) {
			guids.add(guidSSEV.toUpperCase());
		}
				
		select.append(" SELECT DISTINCT EM.ID A1ID, EM.SENDER_NAME A2SENDERNAME, EM.MSG_REG_DAT A3MSGREGDAT, ");
		select.append(DialectConstructor.limitBigString(dialect, "EM.DOC_SUBJECT", 300) + " A4DOCSUBJECT, "); // това заменя долното
//		if (dialect.indexOf("ORACLE") != -1){
//			select.append(" dbms_lob.substr( EM.DOC_SUBJECT, 300, 1 ) || CASE  WHEN dbms_lob.getlength(EM.DOC_SUBJECT)>300  THEN '...'  END A4DOCSUBJECT,");    		
//    		
//    	}else{
//    		select.append(" EM.DOC_SUBJECT A4DOCSUBJECT, ") ;
//    	} 
		
		select.append(" EM.DOC_SROK A5DOCSROK, LOCK_OBJECTS.USER_ID A6LOCK, EM.DOC_GUID A7GUID,   EGOV_NOMENKLATURI.DESCRIPTION A8DESC, ") ;
		select.append(" EM.DOC_RN A9, EM.DOC_DAT A10 " ) ;
	    
		from.append("	FROM EGOV_MESSAGES EM");
		from.append(" 		LEFT OUTER JOIN EGOV_NOMENKLATURI on EM.SOURCE = EGOV_NOMENKLATURI.STATUS_TEKST ");
		from.append(" 		LEFT OUTER JOIN LOCK_OBJECTS ON EM.ID = LOCK_OBJECTS.OBJECT_ID AND  LOCK_OBJECTS.OBJECT_TIP = :TIP AND LOCK_OBJECTS.USER_ID <> :USERID") ;
		
		where.append(" WHERE EM.MSG_TYPE = 'MSG_DocumentRegistrationRequest' AND EM.MSG_STATUS = 'DS_WAIT_REGISTRATION'  AND EM.MSG_INOUT = 1  AND ");
		
		if (guids.size() == 2) {
			where.append("upper(EM.RECIPIENT_GUID) in (:GUIDS)");
		}else {
			where.append("upper(EM.RECIPIENT_GUID) = :GUIDS");
		}
				
				
		
		
		if (tehNom != null && !tehNom.trim().isEmpty()) {
			if(fullEq) {
				if (!tehNom.contains("-")) {
					where.append(" and em.doc_rn = '"+tehNom.trim()+"'");
				}else {
					String[] parts = tehNom.split("-");
					if (parts.length == 2) {
						where.append(" and em.doc_rn = '"+tehNom.trim()+"' or (em.doc_uri_reg = '" + parts[0].trim()+ "' and em.doc_uri_batch = '"+parts[1].trim()+"' ) ");
					}else {
						where.append(" and em.doc_rn = '"+tehNom.trim()+"'");
					}
				}
			
			} else {
				where .append(" AND upper(em.doc_rn) LIKE '%"+tehNom.trim().toUpperCase()+"%' ");
				
			}
		}
		
		
		params.put("TIP", OmbConstants.CODE_ZNACHENIE_JOURNAL_EGOVMESSAGE);
		params.put("USERID", ((UserData) getUser()).getUserAccess());
		
		if (guids.size() == 2) {
			params.put("GUIDS", guids);
		}else {
			params.put("GUIDS", guids.get(0));
		}
		
		
		smd.setSqlParameters(params);
		smd.setSql(select.toString() + from.toString() + where.toString());
		smd.setSqlCount(" select count(distinct EM.ID) " + from.toString() + where.toString());
		
		return smd;
	}
	
	/**
	 * @param idMessage
	 * @return
	 * @throws DbErrorException
	 */
	public List<EgovMessagesFiles> findFilesByMessage(Integer idMessage) throws DbErrorException {
		if (idMessage == null) {
			return new ArrayList<>();
		}
		
		try {
			@SuppressWarnings("unchecked")
			List<EgovMessagesFiles> files = createQuery(
				"select x from EgovMessagesFiles x where x.idMessage = ?1")
				.setParameter(1, idMessage).getResultList();
			
			return files;

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на файлове за съобщение!", e);
		}
	}
	
	
	/** Връща съобщение по guid
	 * @param guid - guid на съобщение
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public EgovMessages findMessageByGuid(String guid) throws DbErrorException {
		if (guid == null) {
			return  null;
		}
		
		try {			
			List<EgovMessages> messages = createQuery("from EgovMessages where msgGuid = :guid")
				.setParameter("guid", guid).getResultList();
			
			if (messages.size() > 0) {
				return messages.get(0);
			}else {
				return null;
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на съобщение!", e);
		}
	}
	
	
	
	
	
	/** за справка Статус пакети за обмен СЕОС / ССЕВ
	*/
	@SuppressWarnings("unchecked")
	public ArrayList<Object[]>  createMsgTypesList() throws DbErrorException {
		
		try {		
			Query q = createNativeQuery("select STATUS_TEKST, DESCRIPTION from EGOV_NOMENKLATURI where STATUS_TEKST like 'MSG_%' order by DESCRIPTION");
			
			return (ArrayList<Object[]>) q.getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при зареждане на тип на съобщения сеос!", e);
		}		
	}
	
	

	@SuppressWarnings("unchecked")
	public ArrayList<Object[]> createMsgStatusList() throws  DbErrorException{

		try {		
			Query q = createNativeQuery("select STATUS_TEKST, DESCRIPTION from EGOV_NOMENKLATURI where STATUS_TEKST like 'DS_%' order by DESCRIPTION");
			
			return (ArrayList<Object[]>) q.getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на статус на съобщения сеос!", e);
		}	
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<Object[]> createCommStatusList() throws  DbErrorException{
		
		try {		
			Query q = createNativeQuery("select STATUS, DESCRIPTION from EGOV_NOMENKLATURI where status is not null order by status");
			
			return (ArrayList<Object[]>) q.getResultList();
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на статус на изпращане сеос!", e);
		}
		
	}
	

	@SuppressWarnings("unchecked")
	/**
	 * Връща Guid на организацията по подадено id
	 * @return
	 * @throws DbErrorException
	 */
	public String findEgovOrgGuidById(Integer id) throws DbErrorException {
		
		try {			
			List<String> org = createNativeQuery("SELECT GUID from EGOV_ORGANISATIONS where id = :IDOrg")
				.setParameter("IDOrg", id).getResultList();
			
			if (org.size() > 0) {
				return org.get(0);
			}else {
				return null;
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на EGOV_ORGANISATIONS по id!", e);
		}		
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * Връща Guid на организацията от EDELIVERY_ORGANISATIONS по подадено id
	 * @return
	 * @throws DbErrorException
	 */
	public String findEgovDeliveryOrgGuidById(Integer id) throws DbErrorException {
		
		try {			
			List<String> org = createNativeQuery("SELECT GUID from EDELIVERY_ORGANISATIONS where id = :IDOrg")
				.setParameter("IDOrg", id).getResultList();
			
			if (org.size() > 0) {
				return org.get(0);
			}else {
				return null;
			}

		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на EDELIVERY_ORGANISATIONS по id!", e);
		}		
	}
	
	
	
	/**
	 * Заявка за справка "Статус пакети за обмен СЕОС / ССЕВ"
	 * @param sender
	 * @param recepient
	 * @param msgType
	 * @param docStatus
	 * @param sentStatus
	 * @param inOut
	 * @param dateOt
	 * @param dateDo
	 * @param source
	 * @return
	 */
	public SelectMetadata createFilterMsgSQL(String sender, String recepient, String msgType, String docStatus, Integer sentStatus, Integer inOut, Date dateOt, Date dateDo, String source, String nameSender,String nameRecepient) {
		
		String dialect = JPA.getUtil().getDbVendorName();
		
		Map<String, Object> params = new HashMap<>();
		StringBuilder whereClause = new  StringBuilder("");
		ArrayList<String> uslovia = new ArrayList<String>();
		
		String selectClause = "SELECT EGOV_MESSAGES.ID 	A0, "+
							"	MSG_TYPE			A1, " + 
				 			"  	nom3.DESCRIPTION    A2, " +	
						    " 	MSG_DAT 			A3, " +				
						    " 	SENDER_NAME 		A4, " +
						    " 	RECIPIENT_NAME 		A5, " +
						    " 	DOC_RN 		  		A6, " +
						    "  	DOC_DAT 			A7, " +						    
						    "  	MSG_RN 		 		A8, " +
						    "  	MSG_RN_DAT 			A9, " +
						    DialectConstructor.limitBigString(dialect, "DOC_SUBJECT", 300) +" A10, " +
						    " 	nom2.DESCRIPTION 	A11, " +
						    "  	nom1.DESCRIPTION    A12, " +						    
						    "  	COMM_ERRROR  		A13, " +
						    "  	PRICHINA  		    A14 ";
		
		String fromClause = " FROM " +
						    " EGOV_MESSAGES join EGOV_NOMENKLATURI nom1 on EGOV_MESSAGES.COMM_STATUS = nom1.STATUS " +
						    "               join EGOV_NOMENKLATURI nom2 on EGOV_MESSAGES.MSG_STATUS = nom2.STATUS_TEKST "+
						    "				join EGOV_NOMENKLATURI nom3 on EGOV_MESSAGES.MSG_TYPE = nom3.STATUS_TEKST ";
		
		
		if (!SearchUtils.isEmpty(sender)){
			uslovia.add("SENDER_GUID = :sender ");
			params.put("sender", sender);
		}
		
		if (!SearchUtils.isEmpty(recepient)){
			uslovia.add("RECIPIENT_GUID = :recepient ");
			params.put("recepient", recepient);
		}
		
		if (!SearchUtils.isEmpty(msgType)){
			uslovia.add("MSG_TYPE = :msgType ");
			params.put("msgType", msgType);
		}
		
		if (!SearchUtils.isEmpty(docStatus)){
			uslovia.add("MSG_STATUS = :docStatus ");
			params.put("docStatus", docStatus);
		}		
		
		if (sentStatus != null){
			uslovia.add("COMM_STATUS = :sentStatus " );
			params.put("sentStatus", sentStatus);
		}
		
		if (inOut != null){
			uslovia.add("MSG_INOUT =:inOut " );
			params.put("inOut", inOut);
		}
		
		if (dateOt != null) {
			uslovia.add("MSG_DAT >= :dateOt ");
			params.put("dateOt", DateUtils.startDate(dateOt));
		}

		if (dateDo != null) {
			uslovia.add("MSG_DAT <= :dateDo");
			params.put("dateDo", DateUtils.endDate(dateDo));
		}
		
		if (!SearchUtils.isEmpty(source)) {
			uslovia.add("SOURCE = :source ");
			params.put("source", source);
		}
		
		if (!SearchUtils.isEmpty(nameSender)) {
			uslovia.add("UPPER(TRIM(SENDER_NAME)) LIKE '%"+nameSender.trim().toUpperCase() + "%'");
		}
		
		if (!SearchUtils.isEmpty(nameRecepient)) {
			uslovia.add("UPPER(TRIM(RECIPIENT_NAME)) LIKE '%"+ nameRecepient.trim().toUpperCase()+ "%'");
		}
		
			
		if (!uslovia.isEmpty()) {
			whereClause.append(" WHERE ");
			for (int i = 0; i < uslovia.size(); i++) {
				whereClause.append(uslovia.get(i));
				if (i != (uslovia.size() - 1)) {
					whereClause.append(" AND ");
				}
			}
		}
		
		SelectMetadata sm = new SelectMetadata();

		sm.setSqlCount(" select count(*) " + fromClause + whereClause.toString());

		sm.setSql(selectClause + fromClause + whereClause.toString());
		sm.setSqlParameters(params);
		
		return sm;
	}
	
	
	
	/** Създава и записва съобщение от тип MSG_DocumentRegistrationRequest

	 * @return 
	 * @throws DbErrorException 
	 */
	public void saveRegistrationRequestMessage(DocDvij dvij, SystemData sd) throws DbErrorException{
		
		EgovMessages mess = new EgovMessages();
		ArrayList<EgovMessagesFiles> files = new ArrayList<EgovMessagesFiles>();
		ArrayList<EgovMessagesCoresp> corespondenti = new ArrayList<EgovMessagesCoresp>();
		
		FilesDAO fdao = new FilesDAO(ActiveUser.DEFAULT);
		
		
		//Doc doc1 = new DocDAO(ActiveUser.DEFAULT).findById();		
		Object[] docData = new DocDAO(ActiveUser.DEFAULT).findDocDataForSeos(dvij.getDocId());
		
		
		SystemClassif item = sd.decodeItemLite(OmbConstants.CODE_CLASSIF_DOC_PREDAVANE_STATUS, OmbConstants.DS_WAIT_SENDING, OmbConstants.CODE_DEFAULT_LANG, new Date(), false);
		String status = item.getCodeExt();
		
		//ИНИЦИАЛИЗАЦИЯ
		mess.setMsgInOut(2); //Изходящо		
		mess.setMsgRegDate(new Date());
		mess.setMsgRn(null);
		mess.setMsgRnDate(null);		
		mess.setMsgStatus(status);		
		mess.setMsgStatusDate(new Date());
		mess.setMsgType(MessageType.MSG_DOCUMENT_REGISTRATION_REQUEST.value());
		mess.setMsgUrgent(1);
		mess.setMsgDate(new Date());
		mess.setMsgGuid("{"+java.util.UUID.randomUUID().toString().toUpperCase()+"}");
		
		mess.setRecepientEik(dvij.getUchastnikIdent());
		mess.setRecepientGuid(dvij.getUchastnikGuid());
		mess.setRecepientName(dvij.getUchastnikName());
		
		
		mess.setSenderGuid(SearchUtils.asString(docData[6]));
		mess.setSenderEik (SearchUtils.asString(docData[8]));
		mess.setSenderName(SearchUtils.asString(docData[9]));
				
		
		//Основни данни за документа
		mess.setMsgComment("N/A");
		mess.setDocComment (null);
		mess.setDocNasochen(null);
		
		String vidDoc = sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, SearchUtils.asInteger(docData[1]), OmbConstants.CODE_DEFAULT_LANG, SearchUtils.asDate(docData[4]));				
		if (vidDoc == null || vidDoc.trim().isEmpty()) {
			vidDoc = "Писмо";
		}
		mess.setDocVid(vidDoc);
		mess.setDocSubject (SearchUtils.asString(docData[0]));
		mess.setDocSrok(null);
		mess.setDocGuid(SearchUtils.asString(docData[2]));
		mess.setDocRn(SearchUtils.asString(docData[3]));
		mess.setDocDate(SearchUtils.asDate(docData[4]));
		mess.setCommStatus(1);
		mess.setSource("S_SEOS");
		//
		
		if (docData[5] != null) {
			
			Referent coresp = new ReferentDAO(ActiveUser.DEFAULT).findByCode(SearchUtils.asInteger(docData[5]), SearchUtils.asDate(docData[4]), false );
			
			
			EgovMessagesCoresp tek = new EgovMessagesCoresp();
			if (coresp.getAddress() != null) {
				if (coresp.getAddress().getEkatte() != null){
					
					String  mesto = sd.decodeItem(OmbConstants.CODE_CLASSIF_EKATTE, coresp.getAddress().getEkatte(), OmbConstants.CODE_DEFAULT_LANG, new Date());
					
					if (mesto != null){
						tek.setCity(mesto);
					}else{
						tek.setCity("Няма въведено!");
					}
						
					
				}else{
					tek.setCity("Няма въведено!");
				}
				
				tek.setAdres(coresp.getAddress().getAddrText());
				tek.setPk(coresp.getAddress().getPostCode());
			}
			
			
			
			tek.setBulstat(coresp.getNflEik());				
			tek.setDopInfo(coresp.getRefInfo());
			tek.setEgn(coresp.getFzlEgn());
			tek.setEmail(coresp.getContactEmail());
			tek.setIme(coresp.getRefName());
			tek.setPhone(coresp.getContactPhone());
			
			corespondenti.add(tek);
			
			
		}	
		
		
		// Взимат се само файловете, които са маркирани за изпращане
		String SELECT_BY_FILE_OBJECT_SQL =
				"select f"
				+ " from Files f"
				+ " inner join FileObject fo on fo.fileId = f.id"
				+ " where fo.objectId = :objectIdArg and fo.objectCode = :objectCodeArg and fo.official = :isOfficial";

		TypedQuery<Files> typedQuery = getEntityManager().createQuery(SELECT_BY_FILE_OBJECT_SQL, Files.class);
		typedQuery.setParameter("objectIdArg", dvij.getDocId());
		typedQuery.setParameter("objectCodeArg", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		typedQuery.setParameter("isOfficial", OmbConstants.CODE_ZNACHENIE_DA);
		List<Files> filesList = typedQuery.getResultList();
		
		if (filesList != null && filesList.size() > 0) {
			
			for (Files att : filesList){
				
				EgovMessagesFiles file = new EgovMessagesFiles();
				file.setFilename(att.getFilename());		        
		        file.setMime(att.getContentType());
		        
		        if (att.getContent() == null) {
		        	att = fdao.findById(att.getId()) ;
		        }
		        
		        file.setBlobcontent(att.getContent());
		        
		        files.add(file);
			}
			
		}
			
		
		
		try {
			
			
			
			save(mess);
			
			if (corespondenti != null && corespondenti.size() == 1) { 
				EgovMessagesCoresp egovCoresp = corespondenti.get(0);
				egovCoresp.setIdMessage(mess.getId());
				JPA.getUtil().getEntityManager().persist(egovCoresp);
			}
			
			
			for (EgovMessagesFiles att : files){				
				att.setIdMessage(mess.getId());				
		        JPA.getUtil().getEntityManager().persist(att);
			}
			
			
			JPA.getUtil().getEntityManager().createNativeQuery("update DOC_DVIJ set MESSAGE_ID = :mid where ID = :IDD").setParameter("mid", mess.getId()).setParameter("IDD", dvij.getId()).executeUpdate();
			
			
		
		}catch (Exception e) {
			LOGGER.error("Грешка изграждане на СЕОС съобщение!", e);			
			throw new DbErrorException("Грешка изграждане на СЕОС съобщение!", e);
		}
		
		
		
		
	}
	
	
	
	
	
	@SuppressWarnings("unchecked")
	/*
	 * Проверка за вече регистриран докумет с този GUID
	 */
	public Object[] isDblGuid(String guid, Integer idRegistratura)
			throws DbErrorException {

		try {

			String sqlString = " select DOC_ID,  RN_DOC,  DOC_DATE FROM DOC where  GUID = :GUIDC and REGISTRATURA_ID = :IDR";

			
			Query q = JPA.getUtil().getEntityManager().createNativeQuery(sqlString);				
			
			q.setParameter("GUIDC", guid);
			q.setParameter("IDR", idRegistratura);
			
			ArrayList<Object[]> rez = (ArrayList<Object[]>) q.getResultList();

			if (rez.size() > 0) {
				return rez.get(0);

			} else {
				return null;
			}

		} catch (HibernateException e) {

			throw new DbErrorException(
					"Грешка при проверка за дублиран GUID !");

		}

	}
	
	
	/** Създава и записва съобщение за искане на нов статус на движението/изпращането

	 * @return 
	 * @throws DbErrorException 
	 */
	public void saveStatusRequestMessage(DocDvij dvij, SystemData sd) throws DbErrorException{
		
			EgovMessages incommingMess = null;
		
			if (dvij.getMessageId() == null) {
				throw new DbErrorException("Движението няма записанo id на съобщение !");
			}else {
				incommingMess = findById(dvij.getMessageId());
				if (incommingMess == null) {
					throw new DbErrorException("Не се намира изпратено съобщение по id= "+dvij.getMessageId()+" !");
				}
			}
			
		
			
			EgovMessages mess = new EgovMessages();
			
			
			
			//ИНИЦИАЛИЗАЦИЯ
			mess.setMsgInOut(1);		
			mess.setMsgRegDate(new Date());
			mess.setMsgRn(null);
			mess.setMsgRnDate(null);			
			mess.setMsgStatusDate(new Date());
			mess.setMsgType(MessageType.MSG_DOCUMENT_STATUS_REQUEST.value());
			mess.setMsgUrgent(1);
			mess.setCommStatus(1);
			mess.setMsgStatus("DS_WAIT_SENDING");
			
			mess.setMsgDate(new Date());
			mess.setMsgGuid("{"+java.util.UUID.randomUUID().toString().toUpperCase()+"}");
			
			mess.setRecepientGuid(incommingMess.getRecepientGuid());
			mess.setRecepientEik(incommingMess.getRecepientEik());
			mess.setRecepientName(incommingMess.getRecepientName());
						
			mess.setSenderGuid(incommingMess.getSenderGuid());			
			mess.setSenderEik(incommingMess.getSenderEik());
			mess.setSenderName(incommingMess.getSenderName());
			
			mess.setDocGuid(incommingMess.getDocGuid());
			mess.setDocDate(incommingMess.getDocDate());
			mess.setDocUriBtch(incommingMess.getDocUriBtch());
			mess.setDocUriReg(incommingMess.getDocUriReg());
			mess.setDocRn(incommingMess.getDocRn());
			
			mess.setSource("S_SEOS");
				
			save(mess);
				
					
	}
	
	
	/** Изпраща съобщението отново
	 * @param idMessage
	 * @throws DbErrorException
	 */
	public void resetOutgoingMessages(Integer messageId) throws DbErrorException{

		try {

			
			
			JPA.getUtil().getEntityManager().createNativeQuery("update EGOV_MESSAGES set COMM_STATUS = 1, COMM_ERRROR = null where ID  = :mId").setParameter("mId", messageId).executeUpdate();
			
			
		} catch (Exception e) {
			throw new DbErrorException("Грешка при ресет на изходящо съобщение !", e);
		}
	}
	
	
	
	/** Записва отказ на съобщението - за СЕОС
	 * @param incommingMess - съобщението, което се отказва
	 * @param prichina - причина за отказа
	 * @param ud
	 * @throws DbErrorException
	 */
	public void saveStatusResponseOtkazMessage(EgovMessages incommingMess, String prichina, UserData ud) throws DbErrorException{
		
		if (incommingMess == null) {
			throw new DbErrorException("Съобщението, на което трябва да се отговори е null");
		}
		
		
		EgovMessages mess = new EgovMessages();
		
		mess.setCommError(null);
		mess.setCommStatus(1);		
		
		mess.setDocGuid(incommingMess.getDocGuid());
		
		mess.setMsgDate(new Date());
		mess.setMsgGuid("{"+java.util.UUID.randomUUID().toString().toUpperCase()+"}");
		mess.setMsgInOut(2);
		mess.setMsgRegDate(new Date());
		mess.setMsgStatusDate(new Date());
		mess.setMsgType(MessageType.MSG_DOCUMENT_STATUS_RESPONSE.value());
		mess.setMsgUrgent(1);
		mess.setMsgVersion("1");
		mess.setMsgXml(null); 
		mess.setMsgComment("Отказан от " +  ud.getLiceNames());
		mess.setMsgStatus("DS_REJECTED");
		mess.setPrichina(prichina);
			
		
		mess.setRecepientName(incommingMess.getSenderName());
		mess.setRecepientEik(incommingMess.getSenderEik());
		mess.setRecepientGuid(incommingMess.getSenderGuid());
		
		mess.setSenderEik(incommingMess.getRecepientEik());
		mess.setSenderGuid(incommingMess.getRecepientGuid());
		mess.setSenderName(incommingMess.getRecepientName());
		
		mess.setUserCreated(ud.getUserId());
		
		mess.setSource("S_SEOS");
		
		save(mess);
		
	}

	/**
	 * Записва отговор на съобщението, че е регистрирано - за СЕОС
	 * @param incommingMess - на което трябва да се отговори 
	 * @param rnDoc - номера на регистрирания в деловодството документ
	 * @param datDoc -дата да деловоден док.
	 * @param ud
	 * @throws DbErrorException
	 */
	public void saveStatusResponseRegisteredMessage(EgovMessages incommingMess, String rnDoc, Date datDoc, UserData ud) throws DbErrorException{
		
		if (incommingMess == null) {
			throw new DbErrorException("Съобщението, на което трябва да се отговори е null");
		}
		
		
		EgovMessages mess = new EgovMessages();
		
		mess.setCommError(null);
		mess.setCommStatus(1);		
		
		mess.setDocGuid(incommingMess.getDocGuid());
		
		mess.setMsgDate(new Date());
		mess.setMsgGuid("{"+java.util.UUID.randomUUID().toString().toUpperCase()+"}");
		mess.setMsgInOut(2);
		mess.setMsgRegDate(new Date());
		mess.setMsgStatusDate(new Date());
		mess.setMsgType(MessageType.MSG_DOCUMENT_STATUS_RESPONSE.value());
		mess.setMsgUrgent(1);
		mess.setMsgVersion("1");
		mess.setMsgXml(null); 
		mess.setMsgComment("регистриран от " +  ud.getLiceNames());
		mess.setMsgStatus("DS_REGISTERED");
		mess.setDocRn(rnDoc);
		mess.setDocDate(datDoc);
			
		
		mess.setRecepientName(incommingMess.getSenderName());
		mess.setRecepientEik(incommingMess.getSenderEik());
		mess.setRecepientGuid(incommingMess.getSenderGuid());
		
		mess.setSenderEik(incommingMess.getRecepientEik());
		mess.setSenderGuid(incommingMess.getRecepientGuid());
		mess.setSenderName(incommingMess.getRecepientName());
		
		mess.setUserCreated(ud.getUserId());
		
		mess.setSource("S_SEOS");
		
		save(mess);
		
	}
	
	
	/**Търси свързани документи дошли по ел. връчване
	 * @param docId ид на документ
	 * @param guid гуид на регистратурата във връчването
	 * @return
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public List<Object[]> getAllDocCoresps(Long docId, String guid) throws DbErrorException {
		
		if (guid == null) {
			return new ArrayList<Object[]>();
		}
		
		
		String sqlString = 	" select msg_guid, SENDER_NAME, doc.RN_DOC, doc.DOC_DATE from EGOV_MESSAGES, doc, DOC_DOC where EGOV_MESSAGES.DOC_GUID = doc.GUID and doc.doc_id = doc_doc.doc_id1 and doc_doc.doc_id2 = :IDD and EGOV_MESSAGES.SOURCE = :SRC and upper(RECIPIENT_GUID) = :GUID" + 
				" union " + 
						" select msg_guid, SENDER_NAME, doc.RN_DOC, doc.DOC_DATE from EGOV_MESSAGES, doc, DOC_DOC where EGOV_MESSAGES.DOC_GUID = doc.GUID and doc.doc_id = doc_doc.doc_id2 and doc_doc.doc_id1 = :IDD and EGOV_MESSAGES.SOURCE = :SRC and upper(RECIPIENT_GUID) = :GUID" + 
				" union " + 
						" select EGOV_MESSAGES.MSG_GUID, SENDER_NAME, doc.RN_DOC, doc.DOC_DATE from doc, EGOV_MESSAGES, DELO_DOC where doc.GUID = EGOV_MESSAGES.DOC_GUID and doc.DOC_ID = DELO_DOC.INPUT_DOC_ID   and  DELO_ID in ( select DELO_ID from DELO_DOC where INPUT_DOC_ID = :IDD ) and doc.DOC_ID <> :IDD  and EGOV_MESSAGES.SOURCE = :SRC and upper(RECIPIENT_GUID) = :GUID";
		
		try {

			
			
			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sqlString);
			
			query.setParameter("IDD", docId);
			query.setParameter("SRC", "S_EDELIVERY");
			query.setParameter("GUID", guid.trim().toUpperCase());
			
			return query.getResultList();
			
			
		} catch (HibernateException e) {
			throw new DbErrorException("Грешка търсене на кореспонденти !", e);
		}
		
		
		
	}

	
	
	/** Създава и записва обект от за връчваето в БД	
	 * @throws DbErrorException 
	 */
	public void saveNewSSEVMessage(DocDvij dvij, SystemData sd) throws DbErrorException {
		
		
		ArrayList<EgovMessagesFiles> files = new ArrayList<EgovMessagesFiles>();
		FilesDAO fdao = new FilesDAO(ActiveUser.DEFAULT);
		
		
		Object[] docData = new DocDAO(ActiveUser.DEFAULT).findDocDataForSeos(dvij.getDocId());
		
		
		//ИНИЦИАЛИЗАЦИЯ
		EgovMessages mess = new EgovMessages();
		
		mess.setSenderGuid(SearchUtils.asString(docData[7]));
		mess.setSenderEik (SearchUtils.asString(docData[8]));
		mess.setSenderName(SearchUtils.asString(docData[9]));
		
		mess.setRecepientType(dvij.getUchastnikType());
		mess.setRecepientName(dvij.getUchastnikName());
		if (mess.getRecepientType().equals("LegalPerson") || mess.getRecepientType().equals("Institution") || mess.getRecepientType().equals("Person")) {
			mess.setRecepientEik(dvij.getUchastnikIdent());
		}else {
			if (mess.getRecepientType().equals("Reply")){
				mess.setReplyIdent(dvij.getUchastnikIdent());
			}
		}
		
		
		String vidDoc = sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, SearchUtils.asInteger(docData[1]), OmbConstants.CODE_DEFAULT_LANG, SearchUtils.asDate(docData[4]));				
		if (vidDoc == null || vidDoc.trim().isEmpty()) {
			vidDoc = "Писмо";
		}
		mess.setDocVid(vidDoc);
		
		
		
		
		
		mess.setMsgType("MSG_DocumentRegistrationRequest");
		mess.setMsgDate(new Date());
		mess.setMsgStatus("DS_WAIT_SENDING");
		mess.setMsgStatusDate(new Date());
		mess.setDocGuid(SearchUtils.asString(docData[2]));
		mess.setDocDate(new Date());		
		mess.setDocSubject (SearchUtils.asString(docData[0]));
		
		mess.setMsgXml(null);
		mess.setCommStatus(1);
		mess.setSource("S_EDELIVERY");
		mess.setMsgInOut(2);		
		mess.setDocRn(SearchUtils.asString(docData[3]));
		mess.setDocDate(SearchUtils.asDate(docData[4]));
		
		// Взимат се само файловете, които са маркирани за изпращане
		String SELECT_BY_FILE_OBJECT_SQL =
				"select f"
				+ " from Files f"
				+ " inner join FileObject fo on fo.fileId = f.id"
				+ " where fo.objectId = :objectIdArg and fo.objectCode = :objectCodeArg and fo.official = :isOfficial";

		TypedQuery<Files> typedQuery = getEntityManager().createQuery(SELECT_BY_FILE_OBJECT_SQL, Files.class);
		typedQuery.setParameter("objectIdArg", dvij.getDocId());
		typedQuery.setParameter("objectCodeArg", OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		typedQuery.setParameter("isOfficial", OmbConstants.CODE_ZNACHENIE_DA);
		List<Files> filesList = typedQuery.getResultList();

		if (filesList != null && filesList.size() > 0) {
			
			for (Files att : filesList){
				
				EgovMessagesFiles file = new EgovMessagesFiles();
				file.setFilename(att.getFilename());		        
		        file.setMime(att.getContentType());
		        
		        if (att.getContent() == null) {
		        	att = fdao.findById(att.getId()) ;
		        }
		        
		        file.setBlobcontent(att.getContent());
		        
		        files.add(file);
			}
			
		}
			
		
		
		try {
			
			
			
			save(mess);
			
			for (EgovMessagesFiles att : files){				
				att.setIdMessage(mess.getId());				
		        JPA.getUtil().getEntityManager().persist(att);
			}
			
			
			JPA.getUtil().getEntityManager().createNativeQuery("update DOC_DVIJ set MESSAGE_ID = :mid where ID = :IDD").setParameter("mid", mess.getId()).setParameter("IDD", dvij.getId()).executeUpdate();
			
			
		
		}catch (Exception e) {
			LOGGER.error("Грешка изграждане на ССЕВ съобщение!", e);			
			throw new DbErrorException("Грешка изграждане на ССЕВ съобщение!", e);
		}
		
	}
	
	
	/** Извлича получената грешка от съобщение 
	 * @param messageId - ид на съобщение
	 *
	 * @return грешката
	 * @throws DbErrorException
	 */
	
	public String getMessageError(Integer messageId) throws DbErrorException {
		
		if (messageId == null) {
			return null;
		}
		
		
		String sqlString = 	"select COMM_ERRROR from EGOV_MESSAGES where id = :MID";
		
		try {

			
			
			Query query = JPA.getUtil().getEntityManager().createNativeQuery(sqlString);
			
			query.setParameter("MID", messageId);
			
			
			return SearchUtils.asString(query.getSingleResult());
			
			
		} catch (HibernateException e) {
			throw new DbErrorException("Грешка търсене на кореспонденти !", e);
		}
		
		
		
	}
	
	/**
	 * Записва съобщение за успешна регистрация по еDelivery	
	 * @param incommingMess - на което трябва да се отговори 
	 * @param rnDoc -  номера на регистрирания в деловодството документ
	 * @param datDoc - дата да деловоден док.
	 * @param vidDoc - вида да деловоден док.
	 * @param bodyTextPlain - текста на самото съобщение
	 * @param subject 
	 * @param sd
	 * @param ud
	 * @throws DbErrorException
	 */
	public void saveDeliverySuccessMess(EgovMessages incommingMess, String rnDoc, Date datDoc, Integer vidDoc, String bodyTextPlain, String subject, SystemData sd, UserData ud) throws DbErrorException {

		if (incommingMess == null) {
			throw new DbErrorException("Съобщението, на което трябва да се отговори е null");
		}

		EgovMessages mess = new EgovMessages();		
		
		mess.setSenderEik(incommingMess.getRecepientEik());
		mess.setSenderGuid(incommingMess.getRecepientGuid());
		mess.setSenderName(incommingMess.getRecepientName());
		
		mess.setRecepientName("Unknown");
		mess.setRecepientEik("Unknown");
		
		mess.setRecepientType("Reply");
		mess.setReplyIdent(incommingMess.getMsgGuid());
					
		mess.setMsgType("MSG_DocumentRegistrationRequest");
		mess.setMsgDate(new Date());
		mess.setMsgStatus("DS_WAIT_SENDING");
		mess.setMsgStatusDate(new Date());
		mess.setDocDate(new Date());		
		mess.setDocGuid(incommingMess.getDocGuid());			
		mess.setCommStatus(1);
		mess.setSource("S_EDELIVERY");
		mess.setMsgInOut(2);		
		mess.setDocRn(rnDoc);
		mess.setDocDate(datDoc);
		String vidDocStr = null;
		try {
			vidDocStr = sd.decodeItem(OmbConstants.CODE_CLASSIF_DOC_VID, vidDoc, OmbConstants.CODE_DEFAULT_LANG, new Date());
		} catch (DbErrorException e) {
			vidDocStr = "Писмо";
		}
		if (vidDocStr == null || vidDocStr.trim().isEmpty()) {
			vidDocStr = "Писмо";
		}
		mess.setDocVid(vidDocStr);
		mess.setMsgXml(bodyTextPlain);
		mess.setDocSubject(subject);
		
		mess.setUserCreated(ud.getUserId());

		save(mess);
	}
	
	
	/**
	 * Кореспонденти по ид на съобщение
	 * @param idMessage
	 * @return
	 * @throws DbErrorException
	 */
	public List<EgovMessagesCoresp> findCorespsByIdMessage(Integer idMessage) throws DbErrorException {
		if (idMessage == null) {
			return new ArrayList<>();
		}
		try {
			@SuppressWarnings("unchecked")
			List<EgovMessagesCoresp> corespList = createQuery("select c from EgovMessagesCoresp c where c.idMessage = ?1")
				.setParameter(1, idMessage).getResultList();
			return corespList;
		} catch (Exception e) {
			throw new DbErrorException("Грешка при търсене на кореспонденти за съобщение.", e);
		}
	}
}

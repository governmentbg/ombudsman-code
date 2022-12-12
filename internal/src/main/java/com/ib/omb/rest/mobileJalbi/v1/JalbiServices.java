package com.ib.omb.rest.mobileJalbi.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.omb.beans.DocData;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocJalba;
import com.ib.omb.rest.mobileJalbi.RestApiImplementation;
import com.ib.omb.rest.mobileJalbi.RestUtils;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.mail.Mailer;
import com.ib.system.mail.Mailer.Content;
import com.ib.system.utils.Base64;
import com.ib.system.utils.SearchUtils;

import static com.ib.omb.rest.mobileJalbi.RestParseUtils.parseInteger;
import static com.ib.omb.rest.mobileJalbi.RestParseUtils.parseBoolean;
import static com.ib.omb.rest.mobileJalbi.RestParseUtils.parseStringMillisToDate;
import static com.ib.omb.rest.mobileJalbi.RestParseUtils.parseBooleanToYesNo;
import static com.ib.omb.rest.mobileJalbi.RestParseUtils.isStringNullOrEmpty;
import static com.ib.omb.rest.mobileJalbi.RestUtils.getMultipartFormValue;

/**
 * Класът се използва от JalbiServicesApi
 * 
 * @author n.kanev
 *
 */
public class JalbiServices extends RestApiImplementation {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JalbiServices.class.getName());
	
	public JalbiServices(ServletContext context, Integer lang) {
		super(context);
		setMessagesLocale(lang);
	}
	
	/**
	 * Запис на жалба
	 * Получава данните от формата като Base64 кодиран json.
	 * 
	 * @param request
	 * @return
	 * @throws Exception 
	 */
	protected Response saveDocJalba(String request, int submitMetod) {
		JalbaJson jalbaJson = getGson().fromJson(request, JalbaJson.class);
		
		Doc doc;
		try {
			
			doc = createNewDocument(jalbaJson.getFiles().size(), submitMetod);
			UserData ud = new UserData(-1, "", ""); // TODO от името на кой усер се записват жалбите от мобилното
			DocDAO dao = new DocDAO(ud);
			
			// ДАННИ ОТ REQUEST-А
			doc.getJalba().setJbpType(jalbaJson.getPersType());
			doc.getJalba().setJbpName(jalbaJson.getName());
			doc.getJalba().setJbpEgn(jalbaJson.getEgn());
			doc.getJalba().setJbpLnc(jalbaJson.getLnch());
			doc.getJalba().setJbpEik(jalbaJson.getEik());
			doc.getJalba().setJbpPol(jalbaJson.getSex());
			doc.getJalba().setJbpAge(jalbaJson.getAge());
			doc.getJalba().setJbpGrj(jalbaJson.getNation());
			doc.getJalba().setJbpEkatte(jalbaJson.getCity());
			doc.getJalba().setJbpPost(jalbaJson.getPCode());
			doc.getJalba().setJbpAddr(jalbaJson.getAddress());
			doc.getJalba().setJbpPhone(jalbaJson.getPhone());
			doc.getJalba().setJbpEmail(jalbaJson.getEmail());
			if(jalbaJson.getProtect() != null) {
				doc.getJalba().setJbpHidden(jalbaJson.getProtect() ? 
							SysConstants.CODE_ZNACHENIE_DA : 
							SysConstants.CODE_ZNACHENIE_NE);
			}
			doc.getJalba().setDateNar(jalbaJson.getDate() != null ? new Date(jalbaJson.getDate()) : null);
			doc.getJalba().setSubjectNar(jalbaJson.getDefendant());
			doc.getJalba().setZasPrava(jalbaJson.getRights());
			doc.getJalba().setVidOpl(jalbaJson.getVid());
			doc.getJalba().setJalbaText(jalbaJson.getDescr());
			doc.getJalba().setRequestText(jalbaJson.getRequest());
			if(jalbaJson.getConsidered() != null) {
				doc.getJalba().setInstCheck(jalbaJson.getConsidered() ?
							SysConstants.CODE_ZNACHENIE_DA : 
							SysConstants.CODE_ZNACHENIE_NE);
			}
			doc.getJalba().setInstNames(jalbaJson.getConsideredBy());
			doc.setDeliveryMethod(jalbaJson.getAnswer());
			
			// Валидираме жалбата, за да не се разчита само на това, че устройствата пращат валидирани данни. 
			List<String> errors = validateJalba(doc, false);
			if(errors.size() > 0) {
				LOGGER.debug("--- invalid jalba, not persisting ---");
				return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, errors);
			}
			else {
				// ДАННИ КОИТО СЕ ВЗИМАТ ОТ Х-КИ ПО ВИД ДОКУМЕНТ
				Object[] settings = dao.findDocSettings(doc.getRegistraturaId(), doc.getDocVid(), getSystemData());
				doc.setRegisterId((Integer) settings[1]);
				boolean createDelo = Objects.equals(SysConstants.CODE_ZNACHENIE_DA, settings[2]);
				
				LOGGER.debug("--- persisting new document (jalba) ---");
				List<Files> parsedFiles = new ArrayList<>();
				for(JalbaFile jf : jalbaJson.getFiles()) {
					Files f = new Files();
					f.setFilename(jf.getFilename());
					f.setContentType(jf.getMime());
					f.setContent(Base64.decode(jf.getContent()));
					parsedFiles.add(f);
				}
				persistDoc(doc, parsedFiles, dao, createDelo, ud);
			}			
		}
		catch (DbErrorException | ObjectInUseException e) {
			JPA.getUtil().rollback();
			LOGGER.error("--- error persisting new document (jalba) ---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(UI_BEANMESSAGES, "general.errDataBaseMsg"));
		}
		catch(Exception e) {
			LOGGER.error("--- error persisting new document (jalba) ---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		finally {
			JPA.getUtil().closeConnection();
		}
		
		createReturnMailJ(doc, getSystemData());
		
		Map<String, String> response = Collections.singletonMap("rnDoc", doc.getRnDoc());
		return RestUtils.returnSuccess(getGson().toJson(response));
	}
	
	/**
	 * Запис на жалба от деца.
	 * Получава данните от формата като Base64 кодиран json.
	 * 
	 * @param request
	 * @return
	 * @throws Exception 
	 */
	protected Response saveDocJalbaDeca(String request, int submitMetod) {
		JalbaJson jalbaJson = getGson().fromJson(request, JalbaJson.class);
		
		Doc doc;
		
		try {
			
			doc = createNewDocument(jalbaJson.getFiles().size(), submitMetod);
			UserData ud = new UserData(-1, "", ""); // TODO от името на кой усер се записват жалбите от мобилното
			DocDAO dao = new DocDAO(ud);
			
			// ДАННИ ОТ REQUEST-А
			doc.getJalba().setJbpType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL);
			doc.getJalba().setJbpName(jalbaJson.getName());
			doc.getJalba().setJbpPol(jalbaJson.getSex());
			doc.getJalba().setJbpAge(jalbaJson.getAge());
			doc.getJalba().setJbpGrj(Integer.parseInt(getSystemData().getSettingsValue("delo.countryBG")));
			doc.getJalba().setJbpEkatte(jalbaJson.getCity());
			doc.getJalba().setJbpAddr(jalbaJson.getAddress());
			doc.getJalba().setJbpPost(jalbaJson.getPCode());
			doc.getJalba().setJbpPhone(jalbaJson.getPhone());
			doc.getJalba().setJbpEmail(jalbaJson.getEmail());
			doc.getJalba().setJbpHidden(OmbConstants.CODE_ZNACHENIE_DA);
			doc.getJalba().setZasPrava(OmbConstants.CODE_ZNACHENIE_PRAVA_PRAVA_DECA);
			SystemClassif scPrava = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_ZAS_PRAVA, OmbConstants.CODE_ZNACHENIE_PRAVA_PRAVA_DECA, ud.getCurrentLang(), new Date(), false);
			if(!SearchUtils.isEmpty(scPrava.getCodeExt())) {
				doc.getJalba().setCodeZveno(Integer.valueOf(scPrava.getCodeExt()));
			}
			doc.getJalba().setJalbaText(jalbaJson.getDescr());
			doc.getJalba().setRequestText(jalbaJson.getRequest());
			doc.setDeliveryMethod(jalbaJson.getAnswer());
			
			// Валидираме жалбата, за да не се разчита само на това, че устройствата пращат валидирани данни. 
			List<String> errors = validateJalba(doc, true);
			if(errors.size() > 0) {
				LOGGER.debug("--- invalid jalba, not persisting ---");
				return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, errors);
			}
			else {
				// ДАННИ КОИТО СЕ ВЗИМАТ ОТ Х-КИ ПО ВИД ДОКУМЕНТ
				Object[] settings = dao.findDocSettings(doc.getRegistraturaId(), doc.getDocVid(), getSystemData());
				doc.setRegisterId((Integer) settings[1]);
				boolean createDelo = Objects.equals(SysConstants.CODE_ZNACHENIE_DA, settings[2]);
				
				LOGGER.debug("--- persisting new document (jalbaDetsa) ---");
				List<Files> parsedFiles = new ArrayList<>();
				for(JalbaFile jf : jalbaJson.getFiles()) {
					Files f = new Files();
					f.setFilename(jf.getFilename());
					f.setContentType(jf.getMime());
					f.setContent(Base64.decode(jf.getContent()));
					parsedFiles.add(f);
				}
				persistDoc(doc, parsedFiles, dao, createDelo, ud);
			}			
		}
		catch (DbErrorException | ObjectInUseException e) {
			JPA.getUtil().rollback();
			LOGGER.error("--- error persisting new document (jalba) ---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, getMessage(UI_BEANMESSAGES, "general.errDataBaseMsg")); 
		}
		catch(Exception e) {
			LOGGER.error("--- error persisting new document (jalba) ---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		finally {
			JPA.getUtil().closeConnection();
		}
		
		createReturnMailJ(doc, getSystemData());
		
		Map<String, String> response = Collections.singletonMap("rnDoc", doc.getRnDoc());
		return RestUtils.returnSuccess(getGson().toJson(response));
	}
	
	/**
	 * Запис на жалба от деца.
	 * Получава данните от формата като multipart/form-data заавка.
	 *  
	 * @param input
	 * @param origin
	 * @return
	 */
	protected Response saveJalbaMultipart(MultipartFormDataInput input, int submitMetod) {
		
		Doc doc;
		
		try {
			List<Files> files = RestUtils.getMultipartFormFiles(input, "files");
			doc = createNewDocument(files.size(), submitMetod);
			UserData ud = new UserData(-1, "", ""); // TODO от името на кой усер се записват жалбите от мобилното
			DocDAO dao = new DocDAO(ud);
			
			Boolean jalbaOtDete = parseBoolean(getMultipartFormValue(input, "fromChild"));
			if(jalbaOtDete == null) jalbaOtDete = false;
			
			// ДАННИ ОТ REQUEST-А
			doc.getJalba().setJbpType(		parseInteger(getMultipartFormValue(input, "persType")));
			doc.getJalba().setJbpName(		getMultipartFormValue(input, "name"));
			doc.getJalba().setJbpEgn(		getMultipartFormValue(input, "egn"));
			doc.getJalba().setJbpLnc(		getMultipartFormValue(input, "lnch"));
			doc.getJalba().setJbpEik(		getMultipartFormValue(input, "eik"));
			doc.getJalba().setJbpPol(		parseInteger(getMultipartFormValue(input, "sex")));
			doc.getJalba().setJbpAge(		parseInteger(getMultipartFormValue(input, "age")));
			doc.getJalba().setJbpGrj(		parseInteger(getMultipartFormValue(input, "nation")));
			doc.getJalba().setJbpEkatte(	parseInteger(getMultipartFormValue(input, "city")));
			doc.getJalba().setJbpPost(		getMultipartFormValue(input, "pCode"));
			doc.getJalba().setJbpAddr(		getMultipartFormValue(input, "address"));
			doc.getJalba().setJbpPhone(		getMultipartFormValue(input, "phone"));
			doc.getJalba().setJbpEmail(		getMultipartFormValue(input, "email"));
			doc.getJalba().setJbpHidden(	parseBooleanToYesNo(getMultipartFormValue(input, "protect")));
			if(doc.getJalba().getJbpHidden() == null) doc.getJalba().setJbpHidden(OmbConstants.CODE_ZNACHENIE_NE); 
			doc.getJalba().setDateNar(		parseStringMillisToDate(getMultipartFormValue(input, "date")));
			doc.getJalba().setSubjectNar(	getMultipartFormValue(input, "defendant"));
			doc.getJalba().setZasPrava(		parseInteger(getMultipartFormValue(input, "rights")));
			doc.getJalba().setVidOpl(		parseInteger(getMultipartFormValue(input, "vid")));
			doc.getJalba().setJalbaText(	getMultipartFormValue(input, "descr"));
			doc.getJalba().setRequestText(	getMultipartFormValue(input, "request"));
			doc.getJalba().setInstCheck(	parseBooleanToYesNo(getMultipartFormValue(input, "considered")));
			if(doc.getJalba().getInstCheck() == null) doc.getJalba().setInstCheck(OmbConstants.CODE_ZNACHENIE_NE); 
			doc.getJalba().setInstNames(	getMultipartFormValue(input, "consideredBy"));
			doc.setDeliveryMethod(			parseInteger(getMultipartFormValue(input, "answer")));
			
			if(jalbaOtDete) {
				doc.getJalba().setJbpType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL);
				doc.getJalba().setJbpGrj(Integer.parseInt(getSystemData().getSettingsValue("delo.countryBG")));
				doc.getJalba().setJbpHidden(OmbConstants.CODE_ZNACHENIE_DA);
				doc.getJalba().setZasPrava(OmbConstants.CODE_ZNACHENIE_PRAVA_PRAVA_DECA);
				SystemClassif scPrava = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_ZAS_PRAVA, OmbConstants.CODE_ZNACHENIE_PRAVA_PRAVA_DECA, ud.getCurrentLang(), new Date(), false);
				if(!SearchUtils.isEmpty(scPrava.getCodeExt())) {
					doc.getJalba().setCodeZveno(Integer.valueOf(scPrava.getCodeExt()));
				}
				doc.getJalba().setInstCheck(OmbConstants.CODE_ZNACHENIE_NE);
			}
			
			// Валидираме жалбата, за да не се разчита само на това, че устройствата пращат валидирани данни. 
			List<String> errors = validateJalba(doc, jalbaOtDete);
			if(errors.size() > 0) {
				LOGGER.debug("--- invalid jalba, not persisting ---");
				return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, errors);
			}
			else {
				// ДАННИ КОИТО СЕ ВЗИМАТ ОТ Х-КИ ПО ВИД ДОКУМЕНТ
				Object[] settings = dao.findDocSettings(doc.getRegistraturaId(), doc.getDocVid(), getSystemData());
				doc.setRegisterId((Integer) settings[1]);
				boolean createDelo = Objects.equals(SysConstants.CODE_ZNACHENIE_DA, settings[2]);
				
				LOGGER.debug("--- persisting new document (jalba) ---");
				persistDoc(doc, files, dao, createDelo, ud);
			}
			
		} 
		catch(IOException | IllegalArgumentException e) {
			LOGGER.error("--- error persisting new document (jalba) ---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		catch (DbErrorException | ObjectInUseException e) {
			JPA.getUtil().rollback();
			LOGGER.error("--- error persisting new document (jalba) ---");
			LOGGER.error(e.getMessage(), e);
			return RestUtils.returnError(Status.INTERNAL_SERVER_ERROR, e.getMessage());
		}
		finally {
			JPA.getUtil().closeConnection();
		}
		
		createReturnMailJ(doc, getSystemData());
		
		Map<String, String> response = Collections.singletonMap("rnDoc", doc.getRnDoc());
		return RestUtils.returnSuccess(getGson().toJson(response));
			
	}
	
	private List<String> validateJalba(Doc doc, boolean jalbaOtDete) {
		
		List<String> errors = new ArrayList<>();
		
		// наименование при нефизическо лице е задължително
		if(doc.getJalba().getJbpType() == null) {
			errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errNoTipJalbopodatel"));
		}
		else if (doc.getJalba().getJbpType() == OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL
                && isStringNullOrEmpty(doc.getJalba().getJbpName())) {
            errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errNoName"));
        }
		
		// лице/орган, срещу което се подава жалбата, е задължително
		// но не и от формата за деца!
		if(!jalbaOtDete) {
			if (isStringNullOrEmpty(doc.getJalba().getSubjectNar())) {
				errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errNoOrgan"));
	        }
		}

        // описанието е задължително
        if (isStringNullOrEmpty(doc.getJalba().getJalbaText())) {
        	errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errNoOpisanie"));
        }

        // датата е бъдеща
        if (!jalbaOtDete 
        		&& doc.getJalba().getDateNar() != null
        		&& doc.getJalba().getDateNar().getTime() > (new Date()).getTime()) {
            errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errFutureDate"));
        }

        // ако е избран начин 'по поща', трябва да има въведен адрес и нас. място
        if (doc.getDeliveryMethod() != null && doc.getDeliveryMethod() == OmbConstants.CODE_ZNACHENIE_ANSWER_METHOD_POST) {
            if(isStringNullOrEmpty(doc.getJalba().getJbpAddr()) || doc.getJalba().getJbpEkatte() == null) {
                errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errNoAddress"));
            }
        }
        // ако е избран начин 'ел. поща', трябва да има въведен имейл
        else if (doc.getDeliveryMethod() != null && doc.getDeliveryMethod() == OmbConstants.CODE_ZNACHENIE_ANSWER_METHOD_EMAIL) {
            if(isStringNullOrEmpty(doc.getJalba().getJbpEmail())) {
        		errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errNoEmail"));
            }
        }
        // ако е избран начин 'по телефона', трябва да има въведен телефонен номер
        else if (doc.getDeliveryMethod() != null && doc.getDeliveryMethod() == OmbConstants.CODE_ZNACHENIE_ANSWER_METHOD_PHONE) {
            if(isStringNullOrEmpty(doc.getJalba().getJbpPhone())) {
            	errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errNoPhone"));
            }
        }
        // в останалите случаи адрес/телефон/имейл е задължителен
        else {
            if (isStringNullOrEmpty(doc.getJalba().getJbpAddr())
                    && isStringNullOrEmpty(doc.getJalba().getJbpEmail())
                    && isStringNullOrEmpty(doc.getJalba().getJbpPhone())) {
            	errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errNoEmailAdressPhone"));
            }
        }

        // ако е избран начин 'ссев', трябва да има въведен егн
        // но не и от формата за деца!
        if (!jalbaOtDete && doc.getDeliveryMethod() != null && doc.getDeliveryMethod() == OmbConstants.CODE_ZNACHENIE_ANSWER_METHOD_SSEV) {
            if(isStringNullOrEmpty(doc.getJalba().getJbpEgn())) {
            	errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errNoEgn"));
            }
        }

        // невалиден ЕГН
        if(!isStringNullOrEmpty(doc.getJalba().getJbpEgn()) && !RestUtils.validateParameter(doc.getJalba().getJbpEgn(), 1)) {
        	errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errInvalidEgn"));
        }

        // невалиден ЛНЧ
        if(!isStringNullOrEmpty(doc.getJalba().getJbpLnc()) && !RestUtils.validateParameter(doc.getJalba().getJbpLnc(), 2)) {
        	errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errInvalidLnch"));
        }

        // невалиден ЕИК
        if(!isStringNullOrEmpty(doc.getJalba().getJbpEik()) && !RestUtils.validateParameter(doc.getJalba().getJbpEik(), 3)) {
        	errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errInvalidEik"));
        }

        // невалиден имейл
        if(!isStringNullOrEmpty(doc.getJalba().getJbpEmail()) && !RestUtils.validateParameter(doc.getJalba().getJbpEmail(), 4)) {
        	errors.add(getMessage(BEAN_MESSAGES, "restJalbi.errInvalidEmail"));
        }
        
        return errors;
	}
	
	/**
	 * Тук се вика същинският save на жалбата и файловете.
	 */
	private void persistDoc(Doc doc, List<Files> files, DocDAO dao, boolean createDelo, UserData ud) throws DbErrorException, ObjectInUseException {
		JPA.getUtil().begin();
		
		doc = dao.save(doc, createDelo, null, null, getSystemData());
		
		if(doc.getCountFiles() > 0) {
			
			FilesDAO filesDao = new FilesDAO(ud);
			
			for(Files file : files) {
				filesDao.saveFileObject(file, doc.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
			}
			
		}
		
		JPA.getUtil().commit();
	}
	
	private Doc createNewDocument(int filesCount, int submitMetod) throws DbErrorException {
		Doc doc = new Doc();
		DocJalba jalba = new DocJalba();
		doc.setJalba(jalba);
		
		Integer registraturaId = 1; 
			
		// СТАТИЧНИ ДАННИ
		doc.setRegistraturaId(registraturaId);
		doc.setDocVid(OmbConstants.CODE_ZNACHENIE_DOC_VID_JALBA);
		doc.setDocType(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN);
		doc.setDocDate(new Date());
		doc.setOtnosno("");
		
		doc.setValid(OmbConstants.CODE_CLASSIF_DOC_VALID_ACTUAL);
		doc.setValidDate(new Date());
		
		doc.setFreeAccess(SysConstants.CODE_ZNACHENIE_DA);
		doc.setCompetence(OmbConstants.CODE_ZNACHENIE_COMPETENCE_OUR);
		
		doc.setCountFiles(filesCount);

		jalba.setSubmitMethod(submitMetod);
		jalba.setSubmitDate(new Date());
		jalba.setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_FILED);
		jalba.setSastDate(new Date());
		jalba.setPublicVisible(SysConstants.CODE_ZNACHENIE_NE);
		jalba.setInstCheck(SysConstants.CODE_ZNACHENIE_NE);

		return doc;
	}
	
	/**
	 * Формира и изпраща е-мейл за отговор
	 * @param document 
	 * @param sd 
	 */
	public void createReturnMailJ(Doc document, SystemData sd) {
		if( !SearchUtils.isEmpty(document.getJalba().getJbpEmail()) ) {
			
			LOGGER.debug("--- sending email to: " + document.getJalba().getJbpEmail() + " ---");
			
			Mailer mm = new Mailer();
			try {
				String subject = "Подадена жалба до Омбудсман на РБ"; // getMessageResourceString(beanMessages, "jp.mailSubject");		
				
				// за да се използва кода в бийна
				DocData docDataBean = new DocData();
				docDataBean.setDocument(document);
				docDataBean.setSd(sd);
				docDataBean.setDocVidInclude(DocData.INCL_COMPLAINT);

				String bodyPlainText = docDataBean.mailBodyTextPlain(true);
	
				docDataBean.setDocument(null);
				docDataBean.setSd(null);

				Properties props = sd.getMailProp(-1, "DEFAULT"); // за да вземе  от настройките на системата
				
				String user = props.getProperty("user.name");
	            String pass = props.getProperty("user.password");
	            String from = props.getProperty("mail.from","noreply@delovodstvo.com");
				
				mm.sent(Content.HTML, props, user, pass, from, "Деловодство", document.getJalba().getJbpEmail(), subject, bodyPlainText, null);
//				System.out.println("OK");
				// TODO нужна ли е информация за потребителя че му е изпратен мейл
				
//				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "jp.sentMailMsg") );	// изпратен, е но ако мейла на подателя е грешен - няма как да сме сигурни,че е успешно....
				
			} catch (Exception e) { // TODO трябва ли нещо да се казва пак на потребителя
				LOGGER.error("Грешка при формиране на е-мейл и връщане на е-мейл към подателя! ", e); 
//				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			} 
		}
	}

}

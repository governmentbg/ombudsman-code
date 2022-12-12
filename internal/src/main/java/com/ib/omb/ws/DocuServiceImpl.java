package com.ib.omb.ws;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DOC_TYPE;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DOC_VID;
import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_REGISTRATURI;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_DA;
import static com.ib.system.SysConstants.CODE_ZNACHENIE_NE;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.jws.WebService;
import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.ReferentDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocWSOptions;
import com.ib.omb.db.dto.Referent;
import com.ib.omb.db.dto.ReferentAddress;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.omb.ws.doc.RegisterDocFile;
import com.ib.omb.ws.doc.RegisterDocRequestType;
import com.ib.omb.ws.doc.RegisterDocResponseType;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.PasswordUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.ValidationUtils;

/**
 * Имплементацията
 */
//@WebService(targetNamespace = "http://reg.docu.indexbg.com/", serviceName = "DocuService", portName = "DocuServicePort", endpointInterface = "com.ib.omb.ws.DocuService")
public class DocuServiceImpl implements DocuService {

	static final Logger LOGGER = LoggerFactory.getLogger(DocuServiceImpl.class);

	@Inject
	private ServletContext servletContext;

	/**  */
	public DocuServiceImpl() {
		super();
	}

	/** */
	@Override
	public RegisterDocResponseType registerDocument(RegisterDocRequestType request, String username, String password) throws DocuServiceFault {
		if (request == null) {
			throw new DocuServiceFault("Невалидни входни данни", "Липсва заявка");
		}
		String externalCode = SearchUtils.trimToNULL(request.getExternalCode());
		if (externalCode == null) {
			throw new DocuServiceFault("Невалидни входни данни", "Липсва външен код");
		}

		username = SearchUtils.trimToNULL(username);
		if (username == null) {
			throw new DocuServiceFault("Непълни данни за идентификация", "Липсва потребителско име");
		}
		password = SearchUtils.trimToNULL(password);
		if (password == null) {
			throw new DocuServiceFault("Непълни данни за идентификация", "Липсва потребителска парола");
		}

		SystemData systemData = (SystemData) this.servletContext.getAttribute("systemData");
		Doc doc;
		try {
			UserData userData = validateUser(username, password);

			// настройка за уеб услуга по външен код
			@SuppressWarnings("unchecked")
			List<DocWSOptions> wsOptions = JPA.getUtil().getEntityManager().createQuery( //
				"select s from DocWSOptions s where upper(s.externalCode) = ?1").setParameter(1, externalCode.toUpperCase()) //
				.getResultList();
			if (wsOptions.isEmpty()) {
				throw new DocuServiceFault("Невалидни данни", "Не е открита настройка за уеб услуга с вънешн код: " + externalCode);
			}
			DocWSOptions wsOption = wsOptions.get(0);

			// настройка по вид документ и регистратура
			Object[] docVidSetting = new DocDAO(userData).findDocSettings(wsOption.getDocRegistratura(), wsOption.getDocVid(), systemData);
			if (docVidSetting == null || docVidSetting[1] == null) {
				StringBuilder detail = new StringBuilder();
				detail.append("Не е открита настройка/регистър за вид документ ");
				detail.append(systemData.decodeItem(CODE_CLASSIF_DOC_VID, wsOption.getDocVid(), CODE_DEFAULT_LANG, null) + " (code=" + wsOption.getDocVid() + ")");
				detail.append(" в регистратура ");
				detail.append(systemData.decodeItem(CODE_CLASSIF_REGISTRATURI, wsOption.getDocRegistratura(), CODE_DEFAULT_LANG, null) + " (id=" + wsOption.getDocRegistratura() + ")");

				throw new DocuServiceFault("Невалидни данни", detail.toString());
			}

			JPA.getUtil().begin();
			doc = createDoc(userData, request, wsOption, docVidSetting, systemData);
			JPA.getUtil().commit();

		} catch (DocuServiceFault e) {
			JPA.getUtil().rollback();
			LOGGER.error(e.toString());
			throw e; // няма да се преопакова

		} catch (Exception e) {
			JPA.getUtil().rollback();
			LOGGER.error("Възникна грешка при регистрация", e);
			throw new DocuServiceFault("Възникна грешка при регистрация", e.getMessage());

		} finally {
			JPA.getUtil().closeConnection();
		}

		// формиране на резултата
		RegisterDocResponseType response = new RegisterDocResponseType();
		try {
			response.setRegistraturaId(doc.getRegistraturaId());

			response.setDocNumber(doc.getRnDoc());
			response.setDocTypeCode(doc.getDocType());
			response.setDocVidCode(doc.getDocVid());

			response.setOtnosno(doc.getOtnosno());

			response.setDocDate(DateUtils.toGregorianCalendar(doc.getDocDate()));

			response.setDocTypeText(systemData.decodeItem(CODE_CLASSIF_DOC_TYPE, doc.getDocType(), CODE_DEFAULT_LANG, doc.getDocDate()));
			response.setDocVidText(systemData.decodeItem(CODE_CLASSIF_DOC_VID, doc.getDocVid(), CODE_DEFAULT_LANG, doc.getDocDate()));
			response.setRegistraturaText(systemData.decodeItem(CODE_CLASSIF_REGISTRATURI, doc.getRegistraturaId(), CODE_DEFAULT_LANG, doc.getDocDate()));

		} catch (Exception e) { // тука дори да гръмне нещо, записа вече е минал и няма как да се даде грешка
			LOGGER.error("Грешка при формиране на резултат", e);
		}
		return response;
	}

	/** */
	Doc createDoc(UserData userData, RegisterDocRequestType request, DocWSOptions option, Object[] docVidSetting, SystemData systemData) throws DbErrorException, ObjectInUseException {
		DocDAO docDao = new DocDAO(userData);

		Doc doc = new Doc();

		doc.setDocDate(new Date());

		// тези от настройките за уеб услуга
		doc.setRegistraturaId(option.getDocRegistratura());
		doc.setDocType(option.getDocType());
		doc.setDocVid(option.getDocVid());
		doc.setFreeAccess(option.getFreeAccess());

		// тези от настройките по вид документ
		doc.setRegisterId((Integer) docVidSetting[1]);
		boolean createDelo = Objects.equals(docVidSetting[2], CODE_ZNACHENIE_DA);
		doc.setPreForRegId((Integer) docVidSetting[3]);

		if (Objects.equals(doc.getDocType(), CODE_ZNACHENIE_DOC_TYPE_IN) && docVidSetting[5] != null) {
			doc.setProcDef((Integer) docVidSetting[5]);

		} else if (Objects.equals(doc.getDocType(), CODE_ZNACHENIE_DOC_TYPE_OWN) && docVidSetting[6] != null) {
			doc.setProcDef((Integer) docVidSetting[6]);

		} else if (Objects.equals(doc.getDocType(), CODE_ZNACHENIE_DOC_TYPE_WRK) && docVidSetting[7] != null) {
			doc.setProcDef((Integer) docVidSetting[7]);
		}

		// тези от заявката
		doc.setTehNomer(request.getTehDocNumber());
		doc.setTehDate(DateUtils.toDate(request.getTehDocDate()));
		doc.setOtnosno(request.getAnnotation());
		doc.setCountFiles(request.getRegisterDocFiles().size());

		doc.setCodeRefCorresp(processCorrespondent(userData, request, systemData));
		if (doc.getCodeRefCorresp() == null) { // ако не е определн кореспондента формирам текст, който да влезне в допИнфо на
												// документа
			doc.setDocInfo(formDocInfoByRequest(request));
		}

		doc = docDao.save(doc, createDelo, null, null, systemData);

		if (request.getRegisterDocFiles().isEmpty()) {
			return doc; // щом няма файлове е готово
		}

		FilesDAO filesDao = new FilesDAO(userData);
		for (RegisterDocFile file : request.getRegisterDocFiles()) {
			Files entity = new Files();

			if (SearchUtils.isEmpty(file.getFilename()) || file.getBinaryContent() == null) {
				LOGGER.warn("RegisterDocFile with EMPTY Filename or BinaryContent");
				continue;
			}

			entity.setFilename(file.getFilename());
			entity.setContentType(file.getContentType());
			entity.setContent(file.getBinaryContent());

			if (file.getSigned() != null) {
				entity.setSigned(file.getSigned().booleanValue() ? CODE_ZNACHENIE_DA : CODE_ZNACHENIE_NE);
			}
			if (file.getPersonalData() != null) {
				entity.setPersonalData(file.getPersonalData().booleanValue() ? CODE_ZNACHENIE_DA : CODE_ZNACHENIE_NE);
			}
			if (file.getOfficial() != null) {
				entity.setOfficial(file.getOfficial().booleanValue() ? CODE_ZNACHENIE_DA : CODE_ZNACHENIE_NE);
			}

			filesDao.saveFileObject(entity, doc.getId(), doc.getCodeMainObject());
		}
		return doc;
	}

	/**
	 * взима данните от заявката за кореспондент и формира текст
	 */
	private String formDocInfoByRequest(RegisterDocRequestType request) {
		StringBuilder dopInfo = new StringBuilder();

		if (request.getCorrespCountry() != null && request.getCorrespCountry().trim().length() > 0) {
			if (dopInfo.length() > 0) {
				dopInfo.append(", ");
			}
			dopInfo.append("Държава: " + request.getCorrespCountry());
		}
		if (request.getCorrespEikEgn() != null && request.getCorrespEikEgn().trim().length() > 0) {
			if (dopInfo.length() > 0) {
				dopInfo.append(", ");
			}
			dopInfo.append("ЕИК/ЕГН: " + request.getCorrespEikEgn());
		}
		if (request.getCorrespNames() != null && request.getCorrespNames().trim().length() > 0) {
			if (dopInfo.length() > 0) {
				dopInfo.append(", ");
			}
			dopInfo.append("Имена: " + request.getCorrespNames());
		}
		if (request.getCorrespEmail() != null && request.getCorrespEmail().trim().length() > 0) {
			if (dopInfo.length() > 0) {
				dopInfo.append(", ");
			}
			dopInfo.append("Имейл: " + request.getCorrespEmail());
		}
		if (request.getCorrespPhone() != null && request.getCorrespPhone().trim().length() > 0) {
			if (dopInfo.length() > 0) {
				dopInfo.append(", ");
			}
			dopInfo.append("Телефон: " + request.getCorrespPhone());
		}
		if (request.getCorrespAddress() != null && request.getCorrespAddress().trim().length() > 0) {
			if (dopInfo.length() > 0) {
				dopInfo.append(", ");
			}
			dopInfo.append("Адрес: " + request.getCorrespAddress());
		}

		return dopInfo.toString();
	}

	/**
	 * Идентифицира или прави запис на нов кореспондент
	 */
	private Integer processCorrespondent(UserData userData, RegisterDocRequestType request, SystemData systemData) throws DbErrorException {
		String eikEgn = SearchUtils.trimToNULL(request.getCorrespEikEgn());

		if (eikEgn != null) {
			StringBuilder correspSql = new StringBuilder();
			correspSql.append("select CODE from ADM_REFERENTS where REF_TYPE in (:refType) and ");
			if (eikEgn.length() == 10) {
				correspSql.append(" (FZL_EGN = :eikEgn or FZL_LNC = :eikEgn) ");

			} else { // ако не е валидно няма и да намери
				correspSql.append(" NFL_EIK = :eikEgn ");
			}
			correspSql.append(" order by REF_ID desc ");

			@SuppressWarnings("unchecked")
			List<Object> refCodeList = JPA.getUtil().getEntityManager().createNativeQuery(correspSql.toString()) //
				.setParameter("refType", Arrays.asList(CODE_ZNACHENIE_REF_TYPE_FZL, CODE_ZNACHENIE_REF_TYPE_NFL)) //
				.setParameter("eikEgn", eikEgn).getResultList();

			if (!refCodeList.isEmpty()) { // идентифициран е
				return SearchUtils.asInteger(refCodeList.get(0));
			}
		}

		String refName = SearchUtils.trimToNULL(request.getCorrespNames());
		if (refName == null) {
			return null; // няма как да запишем без имена
		}

		Referent referent = new Referent();
		referent.setRefName(refName);

		if (ValidationUtils.isValidEGN(eikEgn)) {
			referent.setRefType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL);
			referent.setFzlEgn(eikEgn);

		} else if (ValidationUtils.isValidBULSTAT(eikEgn)) {
			referent.setRefType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL);
			referent.setFzlLnc(eikEgn);

		} else if (ValidationUtils.isValidLNCH(eikEgn)) {
			referent.setRefType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL);
			referent.setNflEik(eikEgn);
		}

		if (referent.getRefType() != null) { // само ако е определен кода че е валиден

			String settingBG = systemData.getSettingsValue("delo.countryBG"); // по подразбиране ще е БГ
			Integer countryCode = settingBG != null ? Integer.valueOf(settingBG) : null;

			// код на държава, койото трябва да се търси във външния код на класификацията на държавите
			String requestCountry = SearchUtils.trimToNULL(request.getCorrespCountry());
			if (requestCountry != null) {
				// TODO няма метод за търсене в класификация по външен код, който да се изпозлва тука
				// в момента трябва да се вземе цялата класификация и да се цикли до като се намери търсеното
			}

			referent.setContactEmail(request.getCorrespEmail());
			referent.setContactPhone(request.getCorrespPhone());
			referent.setRefGrj(countryCode);

			String addrText = SearchUtils.trimToNULL(request.getCorrespAddress());
			if (addrText != null) {
				referent.setAddress(new ReferentAddress());

				referent.getAddress().setAddrCountry(countryCode);
				referent.getAddress().setAddrText(addrText);
			}
			referent = new ReferentDAO(userData).save(referent);
			systemData.mergeReferentsClassif(referent, false);
		}

		return referent.getCode(); // ако не е минало през записа пак ще е НУЛЛ
	}

	/**
	 * Проверява дали го има и прави усердата по даните в усера. Ако има проблем си прави грешка с текст.
	 */
	private UserData validateUser(String username, String password) throws DocuServiceFault, NoSuchAlgorithmException, InvalidKeySpecException {

		@SuppressWarnings("unchecked")
		List<Object[]> user = JPA.getUtil().getEntityManager().createNativeQuery( //
			"select USER_ID, NAMES, PASSWORD from ADM_USERS where USERNAME = ?1").setParameter(1, username).getResultList();
		if (user.isEmpty()) {
			throw new DocuServiceFault("Неуспешна идентификация", "Невалидно потребителско име");
		}

		boolean passValid = PasswordUtils.validatePassword(password, (String) user.get(0)[2]);
		if (!passValid) {
			throw new DocuServiceFault("Неуспешна идентификация", "Невалидна парола");
		}
		int userId = ((Number) user.get(0)[0]).intValue();

		return new UserData(userId, username, (String) user.get(0)[1]);
	}
}

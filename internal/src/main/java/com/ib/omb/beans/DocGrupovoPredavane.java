package com.ib.omb.beans;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.event.Event;
import javax.faces.application.FacesMessage;
import javax.faces.context.Flash;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.component.autocomplete.AutoComplete;
import org.primefaces.component.commandbutton.CommandButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.SelectOneModalA;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDvijDAO;
import com.ib.omb.db.dto.DocDvij;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.ValidationUtils;

@Named
@ViewScoped
public class DocGrupovoPredavane extends IndexUIbean {

	private static final long serialVersionUID = -6234152418111362902L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocGrupovoPredavane.class);
	
	public static final int SELECTED_ADM_STR = 1;
	public static final int SELECTED_KORESP = 2;
	public static final int SELECTED_EMAIL = 3;
	public static final int SELECTED_TEXT = 4;
	
	private static final String ERROR_KLASSIF = "general.errorClassif";
	private static final String DATE_SEND = "data";
	private static final String DATE_RETURN_TO = "dataDo";
	
	private List<SelectItem> predavaneTypeList;
	private List<Dokument> selectedDocs;
	private DocDvijDAO dvijenieDao;

	private Date dvijDate;
	private Date returnToDate;
	private Integer dvijMethod;
	private String dvijInfo;
	private Integer codeKorresp;
	private String textKorresp;
	private Integer codeAdmStr;
	private Integer selectionType;
	private String freeEmail;
	private String freeKoresp;
	private String codeRefEmail;
	
	private transient SelectOneModalA admStr;
	private transient SelectOneModalA koresp;
	private transient AutoComplete autoKoresp;
	private transient AutoComplete autoAdmStr;
	private transient CommandButton btnAdmStr;
	boolean btnKorrespDisable;
	private boolean withExemplars;
	
	
	@Inject
	private Flash flash;
	@Inject
	Event<DocDvij> dvijEvent;
	
	@PostConstruct
	void init() {
		this.dvijenieDao = new DocDvijDAO(getUserData());
		
		readDocuments();
		loadMethods();
		initFields();
	}
	
	/**
	 * Чете и парсва списъка с документи, предадени от предната страница с флаша
	 */
	private void readDocuments() {
		@SuppressWarnings("unchecked")
		List<Object[]> list = (List<Object[]>) flash.get(DocList.SELECTED_DOCS);
		this.selectedDocs = new ArrayList<>();
		
		for(Object[] dok : list) {
			Dokument d = new Dokument();
			d.setDocId(dok[0] == null ? null : ((Number) dok[0]).intValue());
			d.setRnDoc((String) dok[1]);
			d.setDocType(dok[2] == null ? null : ((Number) dok[2]).intValue());
			d.setDocVid(dok[3] == null ? null : ((Number) dok[3]).intValue());
			d.setDocDate(dok[4] == null ? null : new Date(((Timestamp) dok[4]).getTime()));
			d.setRegisterId(dok[5] == null ? null : ((Number) dok[5]).intValue());
			d.setOtnosno((String) dok[6]);
			d.setUrgent(dok[7] == null ? null : ((Number) dok[7]).intValue());
			d.setCodeRefKorresp(dok[8] == null ? null : ((Number) dok[8]).intValue());
			d.setAuthorsCodes((String) dok[9]);
			d.setExemplar(1);
			if(12 <= dok.length) { // последният индекс е 9, ако е добито чрез пълнотекстово търсене
				d.setFilesCount(dok[12] == null ? null : ((Number) dok[12]).intValue()); 
			}
			
			this.selectedDocs.add(d);
		}
	}
	
	/**
	 * Зарежда списъка с начини на предаване и филтрира никому непотребните.
	 */
	private void loadMethods() {
		try {
			this.predavaneTypeList = // премахват се от списъка  трите опции
				createItemsList(true, OmbConstants.CODE_CLASSIF_DVIJ_METHOD, new Date(), true)
				.stream()
				.filter(item -> (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS
								&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV
								&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_WEB_EAU)
				.collect(Collectors.toList());
		} catch (DbErrorException | UnexpectedResultException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}
	
	/**
	 * Задава начални стойности на полетата.
	 */
	private void initFields() {
		this.dvijDate = new Date();
		this.returnToDate = null;
		this.dvijMethod = OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA;
		this.dvijInfo = "";
		this.selectionType = null;
		this.freeKoresp = "";
		this.freeEmail = "";
		this.codeRefEmail = null;
		
		try {
			String param1 = ((SystemData) getSystemData()).getSettingsValue("delo.docWithExemplars");
			this.withExemplars = Objects.equals(param1, String.valueOf(OmbConstants.CODE_ZNACHENIE_DA));
		} catch(DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			this.withExemplars = false;
		}
	}
	
	/**
	 * Кликнат е бутонът предаване.
	 * Валидира и запраща документите на получателя или показва съобщение за грешка.
	 */
	public void actionPredavane() {
		
		if(validate()) {
			
			SystemData systemData = (SystemData) getSystemData();
			Integer codeRef = null;
			String name = null;
			if(this.selectionType == SELECTED_ADM_STR) {
				codeRef = this.codeAdmStr;
				name = decodeCodeRefName(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.codeAdmStr);
			}
			else if(this.selectionType == SELECTED_KORESP) {
				codeRef = this.codeKorresp;
				name = decodeCodeRefName(OmbConstants.CODE_CLASSIF_REFERENTS, this.codeKorresp);
			}
			else if(this.selectionType == SELECTED_TEXT) {
				name = this.freeKoresp;
			}
			
			boolean success = true;
			for(Dokument dok : this.selectedDocs) {
				DocDvij dvij = new DocDvij();
				
				dvij.setCodeRef(codeRef); // CODE_REF
				dvij.setDocId(dok.getDocId()); // DOC_ID
				dvij.setDvijDate(this.dvijDate); // DVIJ_DATE
				dvij.setReturnToDate(this.returnToDate); //RETURN_TO_DATE
				dvij.setDvijMethod(this.dvijMethod); // DVIJ_METHOD
				
				dvij.setDvijEmail(this.codeRefEmail); // DVIJ_EMAIL
				dvij.setDvijText(name); // DVIJ_TEXT
				dvij.setDvijInfo(this.dvijInfo); // DVIJ_INFO
				if(this.useExemplar()) { // EKZ_NOMER
					dvij.setEkzNomer(dok.exemplar);
				}
				else {
					dvij.setEkzNomer(1);
				}
				
				try {
					JPA.getUtil().runInTransaction(() -> this.dvijenieDao.save(dvij, systemData, getUserData(UserData.class).getRegistratura()));
				} catch (BaseException e) {
					LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaZapisNoviDvij"), e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
					success = false;
				}
				dvijEvent.fireAsync(dvij);
				
			}
			
			if(success) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dvijenie.zapisUspeshen"));
				initFields();
				disableKorresp(false);
				disableAdmStr(false);
			}
			scrollToMessages();
		}
		else {
			scrollToMessages();
		}
	}
	
	/**
	 * Валидира данните - дати, имейли, наличие на получател
	 */
	private boolean validate() {

		//Date docDate = DateUtils.startDate(this.document.getDocDate());
		Date predavane = DateUtils.startDate(this.dvijDate);
		Date vrashtaneDo = DateUtils.startDate(this.returnToDate);
		Date dnes = DateUtils.startDate(new Date());
		
		if(predavane == null) {
			JSFUtils.addMessage(DATE_SEND, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavaneZadaljitelna"));
			return false;
		}
		
		// проверява датата на изпращане да не е по-ранна от датата на някой от документите (затова от документите намира най-късния)
		Date latestDate = new Date();
		
		Optional<Dokument> optional =
				this.selectedDocs
				.stream()
				.reduce((a, b) -> DateUtils.startDate(a.docDate).compareTo(DateUtils.startDate(b.docDate)) > 0 ? a : b);
		if(optional.isPresent()) {
			latestDate = optional.get().getDocDate();
		}

		latestDate = DateUtils.startDate(latestDate);
		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");

		if(predavane.compareTo(latestDate) < 0) {
			JSFUtils.addMessage(DATE_SEND, FacesMessage.SEVERITY_ERROR, 
					String.format(getMessageResourceString(beanMessages, "grupDvij.dataPredavanePoRannaOt"), format.format(latestDate)));
			return false;
		}
		
		if(dnes.compareTo(predavane) < 0) {
			JSFUtils.addMessage(DATE_SEND, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataPredavaneBadeshta"));
			return false;
		}
		
		if(vrashtaneDo != null && vrashtaneDo.compareTo(predavane) < 0) {
			JSFUtils.addMessage(DATE_RETURN_TO, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneDoRanna"));
			return false;
		}
		
		// проверки на получателите
		if(this.selectionType == null) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "grupDvij.niamaIzbranPoluchatel"));
			return false;
		}
		else if(this.selectionType == SELECTED_ADM_STR && this.dvijMethod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL) {
			return checkReferentEmail(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.codeAdmStr, OmbClassifAdapter.ADM_STRUCT_INDEX_CONTACT_EMAIL);
			
			// TODO документи без файлове?
		}
		else if(this.selectionType == SELECTED_KORESP && this.dvijMethod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL) {
			return checkReferentEmail(OmbConstants.CODE_CLASSIF_REFERENTS, this.codeKorresp, OmbClassifAdapter.REFERENTS_INDEX_CONTACT_EMAIL);
			
			// TODO документи без файлове?
		}
		else if(this.selectionType == SELECTED_EMAIL) {
			if(this.freeEmail == null || this.freeEmail.trim().isEmpty()) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "grupDvij.niamaIzbranPoluchatel"));
				return false;
			}
			else if (!ValidationUtils.isEmailValid(this.freeEmail)) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						String.format(getMessageResourceString(beanMessages, "dvijenie.emailNevaliden"), this.freeEmail));
				return false;
			}
			else { // ако имейлът е валиден, тук го сетвам за после
				this.codeRefEmail = this.freeEmail;
			}
		}
		else if(this.selectionType == SELECTED_TEXT && (this.freeKoresp == null || this.freeKoresp.trim().isEmpty())) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "grupDvij.niamaIzbranPoluchatel"));
			return false;
		}
		// ако начинът е 'от/на друга регистратура', а е избран получател от същата, не го добавя и показва съобщение
		else if(dvijMethod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA) {
			
			SystemClassif empl = null;
			try {
				empl = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.codeAdmStr, SysConstants.CODE_DEFAULT_LANG, new Date(), true);
			} catch (DbErrorException e) {
				e.printStackTrace();
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "grupDvij.poluchatelNevaliden"));
				return false;
			}
			if(empl == null) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "grupDvij.poluchatelNevaliden"));
				return false; // ако записът е счупен и липсва в базата
			}
			
			if(empl.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA] == getUserData(UserData.class).getRegistratura()) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
				String.format(getMessageResourceString(beanMessages, "docDvij.poluchatelOtSashtataRegistratura"), 
							((Integer) empl.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE]) == 1 ? "Звеното" : "Лицето", 
							empl.getTekst()));
				scrollToMessages();
				return false;
			}
		}
		
		// екземпляри
		for(Dokument dok : this.selectedDocs) {
			if(!this.validateExemplar(dok.getExemplar())) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Вика се при завършване на редакция на таблицата.
	 * @param row
	 */
	public void onRowEdit(Dokument row) {
		boolean valid = validateExemplar(row.getExemplar());		
		
		if(!valid) {
			scrollToMessages();
		}
	}
	
	/**
	 * Извършва валидацията дали подадено число е валиден номер на екземпляр. Показва съобщения при грешка.
	 * @param ekz
	 * @return
	 */
	private boolean validateExemplar(Integer ekz) {
		// TODO да се валидира по-подробно екземплярът
		boolean valid = true;
		if(this.useExemplar()) {
			if(ekz == null) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "docDvij.exemplarZadaljitelen"));
				valid = false;
			}
			else if(ekz < 1) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "docDvij.nevalidenExemplar"));
				valid = false;
			}
		}
		
		return valid;
	}
	
	/**
	 * Връша true, ако системната настойка за работа с екземпляри е ДА
	 */
	public boolean useExemplar() {
		return this.withExemplars;
	}
	
	/**
	 * Вика се само при избран начин на предаване Имейл. 
	 * Проверява дали лицето от адм. стр. или коресп. го има в базата и има валиден имейл.
	 * @param codeClassif или OmbConstants.CODE_CLASSIF_ADMIN_STR или OmbConstants.CODE_CLASSIF_REFERENTS
	 * @param codeReferent кода на референта
	 * @param emailIndex или OmbClassifAdapter.ADM_STRUCT_INDEX_CONTACT_EMAIL или OmbClassifAdapter.REFERENTS_INDEX_CONTACT_EMAIL
	 * @return
	 */
	private boolean checkReferentEmail(int codeClassif, Integer codeReferent, int emailIndex) {
		SystemClassif referent = null;
		try {
			referent = getSystemData().decodeItemLite(codeClassif, codeReferent, SysConstants.CODE_DEFAULT_LANG, new Date(), true);
		} catch (DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			return false;
		}
		
		if(referent == null) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "grupDvij.poluchatelNevaliden"));
			return false;
		}
		
		String name = referent.getTekst();
		String email = (String) referent.getSpecifics()[emailIndex];
		boolean emailIsBad = (email == null || email.isEmpty() || !ValidationUtils.isEmailValid(email));
		
		if(emailIsBad) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					String.format(getMessageResourceString(beanMessages, "dvijenie.poluchatelEmailNevaliden"), name));
			return false;
		}
		else { // ако имейлът е валиден, тук го сетвам за после, за да не се обръщам пак към базата преди запис
			this.codeRefEmail = email;
		}
		
		return true;
	}
	
	/**
	 * Получава код на референт и връща името му
	 */
	public String decodeCodeRefName(Integer codeClassif, Integer codeRef) {
		if(codeRef == null) {
			return null;
		}
		
		try {
			Date date = new Date();
			SystemClassif adm = getSystemData().decodeItemLite(codeClassif, codeRef, SysConstants.CODE_DEFAULT_LANG, date, true);
			return adm.getTekst();

		} catch (DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			scrollToMessages();
		}
		return null;
	}
	
	/**
	 * Вика функцията scrollToErrors на страницата, за да се скролне екранът към съобщенията за грешка.
	 * Сложено е, защото иначе съобщенията може да са извън видимия екран и user изобшо да не разбере,
	 * че е излязла грешка, и каква.
	 */
	private void scrollToMessages() {
		PrimeFaces.current().executeScript("scrollToErrors()");
	}
	
	/**
	 * Кликнат е бутонът изтриване на документ в таблицата
	 */
	public void actionDelete(int index) {
		this.selectedDocs.remove(index);
	}
	
	/**
	 * Избран е получател от полето с административната структура.
	 * Изключва кореспондента да не се пише в него; имейл и свободният текст сами се disable-ват 
	 * с методите {@link #disabledText()} и {@link #disabledEmail()}
	 */
	public void actionAdmStrComplete() {
		this.selectionType = SELECTED_ADM_STR;
		disableKorresp(true);		
	}
	
	/**
	 * Изтрит е изборът в полето с административната структура.
	 * Включва кореспондента; имейл и свободният текст сами се enable-ват 
	 * с методите {@link #disabledText()} и {@link #disabledEmail()}
	 */
	public void actionAdmStrClear() {
		this.selectionType = null;
		disableKorresp(false);
	}
	
	/**
	 * Избран е получател от полето с кореспондент.
	 * Изключва адм. структура да не се пише в нея; имейл и свободният текст сами се disable-ват 
	 * с методите {@link #disabledText()} и {@link #disabledEmail()}
	 */
	public void actionKorrespComplete() {
		this.selectionType = SELECTED_KORESP;
		disableAdmStr(true);
	}
	
	/**
	 * Изтрит е изборът в полето с кореспондент.
	 * Включва адм. структура; имейл и свободният текст сами се enable-ват 
	 * с методите {@link #disabledText()} и {@link #disabledEmail()}
	 */
	public void actionKorrespClear() {
		this.selectionType = null;
		disableAdmStr(false);
	}
	
	/**
	 * При blur събитие при писане в полето за кореспондент (своб. текст).
	 * Изчиства стойности и disable-ва останалите полета при наличие на текст или ги enable-ва при изтрит текст.
	 */
	public void actionKorrespBlur() {
		actionBlur(SELECTED_TEXT, this.freeKoresp);
	}
	
	/**
	 * При blur събитие при писане в полето за адм. структура.
	 * Изчиства стойности и disable-ва останалите полета при наличие на текст или ги enable-ва при изтрит текст.
	 */
	public void actionEmailBlur() {
		actionBlur(SELECTED_EMAIL, this.freeEmail);
	}
	
	/**
	 * Вика се вътрешно. Задава вида на избрания получател и включва/изключва другите полета за избор.
	 * @param inputType {@link #SELECTED_TEXT} при писане на коресп. (свободен текст) или {@link #SELECTED_EMAIL} за ръчно вкаран имейл
	 * @param inputValue стойността на текста в полето
	 */
	private void actionBlur(int inputType, String inputValue) {
		if(inputValue.trim().isEmpty()) {
			this.selectionType = null;
			this.disableAdmStr(false);
			this.disableKorresp(false);
		}
		else {			
			this.selectionType = inputType;
			this.disableAdmStr(true);
			this.disableKorresp(true);
		}
	}
	
	/**
	 * Условието, при което да се показва компонентът за кореспондент (своб. текст)
	 * @return true, ако полето трябва да се покаже
	 */
	public boolean freeKorespRendered() {
		return this.dvijMethod != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS
			&& this.dvijMethod != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV
			&& this.dvijMethod != OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL
			&& this.dvijMethod != OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA;
	}
	
	/**
	 * Изключва/включва полето за избор от адм. структура. Това действа върху полето за писане и бутона.
	 * @param disable
	 */
	private void disableAdmStr(boolean disable) {
		if(this.autoAdmStr == null) {
			this.autoAdmStr = (AutoComplete) this.admStr.findComponent("аutoCompl");
		}
		this.autoAdmStr.setDisabled(disable);
		
		if(this.btnAdmStr == null) { 
			this.btnAdmStr = (CommandButton) this.admStr.findComponent("dialogButton");
		}
		this.btnAdmStr.setDisabled(disable);
		
		this.codeAdmStr = null;
	}
	
	/**
	 * Изключва/включва полето за избор на кореспондент. Това действа върху полето за писане и бутона.
	 * @param disable
	 */
	private void disableKorresp(boolean disable) {
		if(this.autoKoresp == null) {
			this.autoKoresp = (AutoComplete) this.koresp.findComponent("аutoCompl");
		}
		this.autoKoresp.setDisabled(disable);
		if(disable) {
			this.btnKorrespDisable = true;
		}
		else {
			this.btnKorrespDisable = false;
		}
		
		this.codeKorresp = null;
		this.textKorresp = null;
	}
	
	/**
	 * Дали да бъде изключено полето за писане на кореспондент (своб. текст)
	 * @return
	 */
	public boolean disabledText() {
		return this.selectionType != null && this.selectionType != SELECTED_TEXT;
	}
	
	/**
	 * Дали да бъде изключено полето за писане на имейл
	 * @return
	 */
	public boolean disabledEmail() {
		return this.selectionType != null && this.selectionType != SELECTED_EMAIL;
	}
	
	/**
	 * Ако се сменя начинът на изпращане и ще се промени видимостта на поле за коресп. или имейл, 
	 * преди това затрива данните и показва/скрива полета за писане.
	 */
	public void actionMethodChange() {
		if(this.selectionType != null) {
			// Досега е имало текст в полето за коресп. (свободен текст), но начинът е сменен и трябва да се скрие
			if(this.selectionType == SELECTED_TEXT && 
					(this.dvijMethod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL) || (this.dvijMethod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA)) {
				this.selectionType = null;
				this.freeKoresp = "";
				disableAdmStr(false);
				disableKorresp(false);
			}
			// Досега е имало текст в полето за имейл, но начинът е сменен и трябва да се скрие
			else if(this.selectionType == SELECTED_EMAIL && this.dvijMethod != OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL) {
				this.selectionType = null;
				this.freeEmail = "";
				disableAdmStr(false);
				disableKorresp(false);
			}
		}
	}
	
	/**
	 * При показване на иконки до метода на предаване
	 * @param methodCode
	 */
	public String getMethodText(int methodCode) {
		return this.predavaneTypeList.stream()
				.filter(s -> (int) s.getValue() == methodCode)
				.map(SelectItem::getLabel)
				.findFirst()
				.orElse("");
	}
	
	/**
	 * При показване на иконки до метода на предаване
	 * @param methodCode
	 */
	public String getMethodIcon(int methodCode) {
		switch(methodCode) {
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA : return "fas fa-hand-paper";
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_POSHTA : return "fas fa-envelope";
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_FAX : return "fas fa-fax";
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL : return "fas fa-at";
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_KURIER : return "fas fa-mail-bulk";
			case OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA : return "fas fa-file-export";
			default : return "fas fa-location-arrow";
		}
	}
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	public class Dokument {
		private Integer docId;
		private String rnDoc;
		private Integer docType;
		private Integer docVid;
		private Date docDate;
		private Integer registerId;
		private String otnosno;
		private Integer urgent;
		private Integer codeRefKorresp;
		private String authorsCodes;
		private Integer filesCount;
		private Integer exemplar;
		
		public Integer getDocId() {
			return docId;
		}
		public void setDocId(Integer docId) {
			this.docId = docId;
		}
		public String getRnDoc() {
			return rnDoc;
		}
		public void setRnDoc(String rnDoc) {
			this.rnDoc = rnDoc;
		}
		public Integer getDocType() {
			return docType;
		}
		public void setDocType(Integer docType) {
			this.docType = docType;
		}
		public Integer getDocVid() {
			return docVid;
		}
		public void setDocVid(Integer docVid) {
			this.docVid = docVid;
		}
		public Date getDocDate() {
			return docDate;
		}
		public void setDocDate(Date docDate) {
			this.docDate = docDate;
		}
		public Integer getRegisterId() {
			return registerId;
		}
		public void setRegisterId(Integer registerId) {
			this.registerId = registerId;
		}
		public String getOtnosno() {
			return otnosno;
		}
		public void setOtnosno(String otnosno) {
			this.otnosno = otnosno;
		}
		public Integer getUrgent() {
			return urgent;
		}
		public void setUrgent(Integer urgent) {
			this.urgent = urgent;
		}
		public Integer getCodeRefKorresp() {
			return codeRefKorresp;
		}
		public void setCodeRefKorresp(Integer codeRefKorresp) {
			this.codeRefKorresp = codeRefKorresp;
		}
		public String getAuthorsCodes() {
			return authorsCodes;
		}
		public void setAuthorsCodes(String authorsCodes) {
			this.authorsCodes = authorsCodes;
		}
		public Integer getFilesCount() {
			return filesCount;
		}
		public void setFilesCount(Integer filesCount) {
			this.filesCount = filesCount;
		}
		public Integer getExemplar() {
			return exemplar;
		}
		public void setExemplar(Integer exemplar) {
			this.exemplar = exemplar;
		}
	}
	
	public List<Dokument> getSelectedDocs() {
		return selectedDocs;
	}
	
	public void setSelectedDocs(List<Dokument> selectedDocs) {
		this.selectedDocs = selectedDocs;
	}

	public Date getDvijDate() {
		return dvijDate;
	}

	public void setDvijDate(Date dvijDate) {
		this.dvijDate = dvijDate;
	}

	public Date getReturnToDate() {
		return returnToDate;
	}

	public void setReturnToDate(Date returnToDate) {
		this.returnToDate = returnToDate;
	}

	public List<SelectItem> getPredavaneTypeList() {
		return predavaneTypeList;
	}

	public void setPredavaneTypeList(List<SelectItem> predavaneTypeList) {
		this.predavaneTypeList = predavaneTypeList;
	}

	public Integer getDvijMethod() {
		return dvijMethod;
	}

	public void setDvijMethod(Integer dvijMethod) {
		this.dvijMethod = dvijMethod;
	}

	public String getDvijInfo() {
		return dvijInfo;
	}

	public void setDvijInfo(String dvijInfo) {
		this.dvijInfo = dvijInfo;
	}

	public Integer getSelectionType() {
		return selectionType;
	}

	public void setSelectionType(Integer selectionType) {
		this.selectionType = selectionType;
	}

	public SelectOneModalA getAdmStr() {
		return admStr;
	}

	public void setAdmStr(SelectOneModalA admStr) {
		this.admStr = admStr;
	}

	public SelectOneModalA getKoresp() {
		return koresp;
	}

	public void setKoresp(SelectOneModalA koresp) {
		this.koresp = koresp;
	}

	public Integer getCodeKorresp() {
		return codeKorresp;
	}

	public void setCodeKorresp(Integer codeKorresp) {
		this.codeKorresp = codeKorresp;
	}

	public Integer getCodeAdmStr() {
		return codeAdmStr;
	}

	public void setCodeAdmStr(Integer codeAdmStr) {
		this.codeAdmStr = codeAdmStr;
	}

	public String getTextKorresp() {
		return textKorresp;
	}

	public void setTextKorresp(String textKorresp) {
		this.textKorresp = textKorresp;
	}

	public String getFreeEmail() {
		return freeEmail;
	}

	public void setFreeEmail(String freeEmail) {
		this.freeEmail = freeEmail;
	}

	public String getFreeKoresp() {
		return freeKoresp;
	}

	public void setFreeKoresp(String freeKoresp) {
		this.freeKoresp = freeKoresp;
	}

	public boolean isBtnKorrespDisable() {
		return btnKorrespDisable;
	}

	public void setBtnKorrespDisable(boolean btnKorrespDisable) {
		this.btnKorrespDisable = btnKorrespDisable;
	}

	public boolean isWithExemplars() {
		return withExemplars;
	}

	public void setWithExemplars(boolean withExemplars) {
		this.withExemplars = withExemplars;
	}
	
}

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

@Named
@ViewScoped
public class DocGrupovoVrashtane extends IndexUIbean {

	private static final long serialVersionUID = -1235894495023949629L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocGrupovoVrashtane.class);
	
	public static final int SELECTED_ADM_STR = 1;
	public static final int SELECTED_KORESP = 2;
	public static final int SELECTED_TEXT = 4;
	
	private static final String ERROR_KLASSIF = "general.errorClassif";
	private static final String DATE_RETURN = "data";
	
	private List<SelectItem> predavaneTypeList;
	private List<DokumentDvijenie> selectedDvij;
	private List<DokumentDvijenie> zabranenoVrashtane;
	private DocDvijDAO dvijenieDao;
	
	private Date returnDate;
	private Integer returnMethod;
	private String returnInfo;
	private Integer codeKorresp;
	private String textKorresp;
	private Integer codeAdmStr;
	private Integer selectionType;
	private String freeKoresp;
	
	private transient SelectOneModalA admStr;
	private transient SelectOneModalA koresp;
	private transient AutoComplete autoKoresp;
	private transient AutoComplete autoAdmStr;
	private transient CommandButton btnAdmStr;
	private boolean btnKorrespDisable;
	private boolean hideInput;
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
		this.hideInput = false;
	}
	
	/**
	 * Чете и парсва списъка с документи, предадени от предната страница с флаша
	 */
	private void readDocuments() {
		
		@SuppressWarnings("unchecked")
		List<Object[]> list = (List<Object[]>) flash.get(DocList.SELECTED_DOCS);
		this.selectedDvij = new ArrayList<>();
		this.zabranenoVrashtane = new ArrayList<>();
		
		for(Object[] dok : list) {
			DokumentDvijenie d = new DokumentDvijenie();
			d.setDvijId(dok[0] == null ? null : ((Number) dok[0]).intValue());
			d.setRnDoc((String) dok[1]);
			d.setDocType(dok[2] == null ? null : ((Number) dok[2]).intValue());
			d.setDocVid(dok[3] == null ? null : ((Number) dok[3]).intValue());
			d.setDocDate(dok[4] == null ? null : new Date(((Timestamp) dok[4]).getTime()));
			d.setRegisterId(dok[5] == null ? null : ((Number) dok[5]).intValue());
			d.setOtnosno((String) dok[6]);
			d.setUrgent(dok[7] == null ? null : ((Number) dok[7]).intValue());
			d.setCodeRefKorresp(dok[8] == null ? null : ((Number) dok[8]).intValue());
			d.setAuthorsCodes((String) dok[9]);
			d.setFilesCount(dok[12] == null ? null : ((Number) dok[12]).intValue());
			d.setDocId(dok[13] == null ? null : ((Number) dok[13]).intValue());
			d.setDvijMethod(dok[14] == null ? null : ((Number) dok[14]).intValue());
			d.setDvijText(dok[15] == null ? null : (String) dok[15]);
			d.setDvijDate(dok[16] == null ? null : new Date(((Timestamp) dok[16]).getTime()));
			d.setExemplar(dok[18] == null ? 1 : ((Number) dok[18]).intValue());
			if(zabranenoVrashtane(d)) {
				this.zabranenoVrashtane.add(d);
			}
			else {
				this.selectedDvij.add(d);
			}
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
								&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_WEB_EAU
								&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL)
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
		this.returnDate = new Date();
		this.returnMethod = OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA;
		this.returnInfo = "";
		this.selectionType = null;
		this.freeKoresp = "";
		
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
	 * Връша true, ако системната настойка за работа с екземпляри е ДА
	 */
	public boolean useExemplar() {
		return this.withExemplars;
	}
	
	/**
	 * Проверката кога едно избрано движение ще бъде преместено в списъка със забранените за връщане.
	 * @param d
	 * @return true, когато връщането е забранено
	 */
	private boolean zabranenoVrashtane(DokumentDvijenie d) {
		return
			d.dvijMethod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS
				||	d.dvijMethod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV
				||	d.dvijMethod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL;
		
	}
	
	/**
	 * Кликнат е бутонът връщане.
	 * Валидира и завръща документите на получателя или показва съобщение за грешка.
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
			for(DokumentDvijenie dok : this.selectedDvij) {
				try {
					DocDvij dvij = this.dvijenieDao.findById(dok.dvijId);
					
					dvij.setReturnCodeRef(codeRef); // RETURN_CODE_REF
					dvij.setReturnDate(this.returnDate); // RETURN_DATE
					dvij.setReturnMethod(this.returnMethod); // RETURN_METHOD
					dvij.setReturnTextRef(name); // RETURN_TEXT_REF
					dvij.setReturnInfo(this.returnInfo); // RETURN_INFO
				
					JPA.getUtil().runInTransaction(() -> this.dvijenieDao.save(dvij, systemData, getUserData(UserData.class).getRegistratura()));
					dvijEvent.fireAsync(dvij);
				} catch (BaseException e) {
					LOGGER.error(getMessageResourceString(beanMessages, "dvijenie.greshkaZapisVrashtane"), e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
					success = false;
					break;
				}
			}
			
			if(success) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "dvijenie.zapisUspeshen"));
				initFields();
				disableKorresp(false);
				disableAdmStr(false);
				this.hideInput = true;
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
		Date vrashtane = DateUtils.startDate(this.returnDate);
		Date dnes = DateUtils.startDate(new Date());
				
		if(vrashtane == null) {
			JSFUtils.addMessage(DATE_RETURN, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneZadaljitelna"));
			return false;
		}
				
		// проверява датата на връщане да не е по-ранна от датата на изпращането (затова намира най-късното)
		Date latestDvijDate = new Date();
		
		Optional<DokumentDvijenie> optional = 
				this.selectedDvij
				.stream()
				.reduce((a, b) -> DateUtils.startDate(a.dvijDate).compareTo(DateUtils.startDate(b.dvijDate)) > 0 ? a : b);
		if(optional.isPresent()) {
			latestDvijDate = optional.get().getDvijDate();
		}

		latestDvijDate = DateUtils.startDate(latestDvijDate);
		SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");

		if(vrashtane.compareTo(latestDvijDate) < 0) {
			JSFUtils.addMessage(DATE_RETURN, FacesMessage.SEVERITY_ERROR, 
					String.format(getMessageResourceString(beanMessages, "grupDvij.dataVrashtanePoRannaOt"), format.format(latestDvijDate)));
			return false;
		}
				
		if(dnes.compareTo(vrashtane) < 0) {
			JSFUtils.addMessage(DATE_RETURN, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneBadeshta"));
			return false;
		}
				
		// проверки на получателите
		if(this.selectionType == null) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "grupDvij.niamaIzbranPoluchatel"));
			return false;
		}	
		else if(this.selectionType == SELECTED_TEXT && (this.freeKoresp == null || this.freeKoresp.isEmpty())) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "grupDvij.niamaIzbranPoluchatel"));
			return false;
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
		this.selectedDvij.remove(index);
	}
	
	/**
	 * Избран е получател от полето с административната структура.
	 * Изключва кореспондента да не се пише в него; свободният текст сам се disable-ва
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
		if(this.freeKoresp.isEmpty()) {
			this.selectionType = null;
			this.disableAdmStr(false);
			this.disableKorresp(false);
		}
		else {			
			this.selectionType = SELECTED_TEXT;
			this.disableAdmStr(true);
			this.disableKorresp(true);
		}
	}
	
	/**
	 * Условието, при което да се показва компонентът за кореспондент (своб. текст)
	 * @return true, ако полето трябва да се покаже
	 */
	public boolean freeKorespRendered() {
		return this.returnMethod != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS
			&& this.returnMethod != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV
			&& this.returnMethod != OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA;
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
	 * Ако се сменя начинът на изпращане и ще се промени видимостта на поле за писане на свободен кореспондент, 
	 * преди това затрива данните и показва/скрива полето за писане.
	 */
	public void actionMethodChange() {
		if(this.selectionType != null) {
			// Досега е имало текст в полето за коресп. (свободен текст), но начинът е сменен и трябва да се скрие
			if(this.selectionType == SELECTED_TEXT && this.returnMethod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA) {
				this.selectionType = null;
				this.freeKoresp = "";
				disableAdmStr(false);
				disableKorresp(false);
			}
			// Досега е имало текст в полето за кореспондент, но начинът е сменен и трябва да се скрие
			else if(this.selectionType == SELECTED_KORESP && this.returnMethod == OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA) {
				actionKorrespClear();
				this.codeKorresp = null;
				this.textKorresp = null;
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
	
	public class DokumentDvijenie {
		private Integer dvijId;
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
		private Integer docId;
		private Integer dvijMethod;
		private String dvijText;
		private Date dvijDate;
		
		public Integer getDvijId() {
			return dvijId;
		}
		public void setDvijId(Integer dvijId) {
			this.dvijId = dvijId;
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
		public Integer getDocId() {
			return docId;
		}
		public void setDocId(Integer docId) {
			this.docId = docId;
		}
		public Integer getDvijMethod() {
			return dvijMethod;
		}
		public void setDvijMethod(Integer dvijMethod) {
			this.dvijMethod = dvijMethod;
		}
		public String getDvijText() {
			return dvijText;
		}
		public void setDvijText(String dvijText) {
			this.dvijText = dvijText;
		}
		public Date getDvijDate() {
			return dvijDate;
		}
		public void setDvijDate(Date dvijDate) {
			this.dvijDate = dvijDate;
		}
	}
	

	public List<SelectItem> getPredavaneTypeList() {
		return predavaneTypeList;
	}

	public void setPredavaneTypeList(List<SelectItem> predavaneTypeList) {
		this.predavaneTypeList = predavaneTypeList;
	}

	public List<DokumentDvijenie> getSelectedDvij() {
		return selectedDvij;
	}

	public void setSelectedDvij(List<DokumentDvijenie> selectedDvij) {
		this.selectedDvij = selectedDvij;
	}

	public List<DokumentDvijenie> getZabranenoVrashtane() {
		return zabranenoVrashtane;
	}

	public void setZabranenoVrashtane(List<DokumentDvijenie> zabranenoVrashtane) {
		this.zabranenoVrashtane = zabranenoVrashtane;
	}

	public Date getReturnDate() {
		return returnDate;
	}

	public void setReturnDate(Date returnDate) {
		this.returnDate = returnDate;
	}

	public Integer getReturnMethod() {
		return returnMethod;
	}

	public void setReturnMethod(Integer returnMethod) {
		this.returnMethod = returnMethod;
	}

	public String getReturnInfo() {
		return returnInfo;
	}

	public void setReturnInfo(String returnInfo) {
		this.returnInfo = returnInfo;
	}

	public Integer getCodeKorresp() {
		return codeKorresp;
	}

	public void setCodeKorresp(Integer codeKorresp) {
		this.codeKorresp = codeKorresp;
	}

	public String getTextKorresp() {
		return textKorresp;
	}

	public void setTextKorresp(String textKorresp) {
		this.textKorresp = textKorresp;
	}

	public Integer getCodeAdmStr() {
		return codeAdmStr;
	}

	public void setCodeAdmStr(Integer codeAdmStr) {
		this.codeAdmStr = codeAdmStr;
	}

	public Integer getSelectionType() {
		return selectionType;
	}

	public void setSelectionType(Integer selectionType) {
		this.selectionType = selectionType;
	}

	public String getFreeKoresp() {
		return freeKoresp;
	}

	public void setFreeKoresp(String freeKoresp) {
		this.freeKoresp = freeKoresp;
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

	public boolean isBtnKorrespDisable() {
		return btnKorrespDisable;
	}

	public void setBtnKorrespDisable(boolean btnKorrespDisable) {
		this.btnKorrespDisable = btnKorrespDisable;
	}

	public boolean isHideInput() {
		return hideInput;
	}

	public void setHideInput(boolean hideInput) {
		this.hideInput = hideInput;
	}

	public boolean isWithExemplars() {
		return withExemplars;
	}

	public void setWithExemplars(boolean withExemplars) {
		this.withExemplars = withExemplars;
	}
	
}

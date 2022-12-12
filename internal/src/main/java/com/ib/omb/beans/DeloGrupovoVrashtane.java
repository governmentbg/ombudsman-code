package com.ib.omb.beans;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import com.ib.omb.db.dao.DeloDvijDAO;
import com.ib.omb.db.dto.DeloDvij;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;

@Named
@ViewScoped
public class DeloGrupovoVrashtane extends IndexUIbean {

	private static final long serialVersionUID = -1235894495023949629L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocGrupovoVrashtane.class);
	
	public static final int SELECTED_ADM_STR = 1;
	public static final int SELECTED_KORESP = 2;
	public static final int SELECTED_TEXT = 4;
	
	private static final String ERROR_KLASSIF = "general.errorClassif";
	private static final String DATE_RETURN = "data";
	
	private List<SelectItem> predavaneTypeList;
	private List<DeloDvijenie> selectedDvij;
	private List<DeloDvijenie> zabranenoVrashtane;
	private DeloDvijDAO dvijenieDao;
	
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
	

	@Inject
	private Flash flash;
	@Inject
	Event<DeloDvij> dvijEvent;
	
	@PostConstruct
	void init() {
		this.dvijenieDao = new DeloDvijDAO(getUserData());
		
		readDelo();
		loadMethods();
		initFields();
		this.hideInput = false;
	}
	
	/**
	 * Чете и парсва списъка с дела/преписки, предадени от предната страница с флаша
	 */
	private void readDelo() {
		
		@SuppressWarnings("unchecked")
		List<Object[]> list = (List<Object[]>) flash.get(DeloList.SELECTED_DELO);
		this.selectedDvij = new ArrayList<>();
		this.zabranenoVrashtane = new ArrayList<>();
		
		for(Object[] dok : list) {
			DeloDvijenie d = new DeloDvijenie();

			d.setDvijId(dok[0] == null ? null : ((Number) dok[0]).intValue());
			d.setRnDelo((String) dok[1]);
			d.setDeloDate(dok[2] == null ? null : new Date(((Timestamp) dok[2]).getTime()));
			d.setDeloType(dok[3] == null ? null : ((Number) dok[3]).intValue());
			d.setStatus(dok[4] == null ? null : ((Number) dok[4]).intValue());
			d.setDeloName((String) dok[6]);
			d.setBrTom(dok[10] == null ? null : ((Number) dok[10]).intValue());
			d.setDeloId(dok[11] == null ? null : ((Number) dok[11]).intValue());
			d.setDvijMethod(dok[12] == null ? null : ((Number) dok[12]).intValue());
			d.setDvijText(dok[13] == null ? null : (String) dok[13]);
			d.setDvijDate(dok[14] == null ? null : new Date(((Timestamp) dok[14]).getTime()));
			
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
			this.predavaneTypeList =
				createItemsList(true, OmbConstants.CODE_CLASSIF_DVIJ_METHOD, new Date(), true)
				.stream()
				.filter(item -> (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SEOS
								&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_SSEV
								&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_WEB_EAU
								&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_EMAIL
								&& (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_PREDAVANE_DRUGA_REGISTRATURA)
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
	}
	
	/**
	 * Проверката кога едно избрано движение ще бъде преместено в списъка със забранените за връщане.
	 * @param d
	 * @return true, когато връщането е забранено
	 */
	private boolean zabranenoVrashtane(DeloDvijenie d) {
		return d.deloType == OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM;
	}
	
	/**
	 * Кликнат е бутонът връщане.
	 * Валидира и завръща преписките/делата на получателя или показва съобщение за грешка.
	 */
	public void actionPredavane() {

		if(validate()) {
			
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
			for(DeloDvijenie dok : this.selectedDvij) {
				try {
					DeloDvij dvij = this.dvijenieDao.findById(dok.dvijId);
					
					dvij.setReturnCodeRef(codeRef); // RETURN_CODE_REF
					dvij.setReturnDate(this.returnDate); // RETURN_DATE
					dvij.setReturnMethod(this.returnMethod); // RETURN_METHOD
					dvij.setReturnTextRef(name); // RETURN_TEXT_REF
					dvij.setReturnInfo(this.returnInfo); // RETURN_INFO
				
					JPA.getUtil().runInTransaction(() -> this.dvijenieDao.save(dvij, getSystemData()));
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
	 * Валидира данните - дати, наличие на получател
	 */
	private boolean validate() {
		
		Date vrashtane = DateUtils.startDate(this.returnDate);
		Date dnes = DateUtils.startDate(new Date());
				
		if(vrashtane == null) {
			JSFUtils.addMessage(DATE_RETURN, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "dvijenie.dataVrashtaneZadaljitelna"));
			return false;
		}
				
		// проверява датата на връщане да не е по-ранна от датата на изпращането (затова намира най-късното)
		Date latestDvijDate = new Date();
		
		Optional<DeloDvijenie> optional = 
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
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "grupDvij.niamaIzbranoLicePrepiska"));
			return false;
		}	
		else if(this.selectionType == SELECTED_TEXT && (this.freeKoresp == null || this.freeKoresp.isEmpty())) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "grupDvij.niamaIzbranoLicePrepiska"));
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
	 * Кликнат е бутонът изтриване на дело в таблицата
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
	 * Включва кореспондента; свободният текст сам се enable-ва 
	 * с методите {@link #disabledText()} и {@link #disabledEmail()}
	 */
	public void actionAdmStrClear() {
		this.selectionType = null;
		disableKorresp(false);
	}
	
	/**
	 * Избран е получател от полето с кореспондент.
	 * Изключва адм. структура да не се пише в нея; свободният текст сам се disable-ва 
	 * с методите {@link #disabledText()} и {@link #disabledEmail()}
	 */
	public void actionKorrespComplete() {
		this.selectionType = SELECTED_KORESP;
		disableAdmStr(true);
	}
	
	/**
	 * Изтрит е изборът в полето с кореспондент.
	 * Включва адм. структура; свободният текст сам се enable-ва 
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
	
	public class DeloDvijenie {
		private Integer dvijId;
		private String rnDelo;
		private Date deloDate;
		private Integer deloType;
		private Integer status;
		private String deloName;
		private Integer deloId;
		private Integer dvijMethod;
		private String dvijText;
		private Date dvijDate;
		private Integer brTom;
		 
		public Integer getDvijId() {
			return dvijId;
		}
		public void setDvijId(Integer dvijId) {
			this.dvijId = dvijId;
		}
		public String getRnDelo() {
			return rnDelo;
		}
		public void setRnDelo(String rnDelo) {
			this.rnDelo = rnDelo;
		}
		public Date getDeloDate() {
			return deloDate;
		}
		public void setDeloDate(Date deloDate) {
			this.deloDate = deloDate;
		}
		public Integer getDeloType() {
			return deloType;
		}
		public void setDeloType(Integer deloType) {
			this.deloType = deloType;
		}
		public Integer getStatus() {
			return status;
		}
		public void setStatus(Integer status) {
			this.status = status;
		}
		public String getDeloName() {
			return deloName;
		}
		public void setDeloName(String deloName) {
			this.deloName = deloName;
		}
		public Integer getDeloId() {
			return deloId;
		}
		public void setDeloId(Integer deloId) {
			this.deloId = deloId;
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
		public Integer getBrTom() {
			return brTom;
		}
		public void setBrTom(Integer brTom) {
			this.brTom = brTom;
		}
	}

	public List<SelectItem> getPredavaneTypeList() {
		return predavaneTypeList;
	}

	public void setPredavaneTypeList(List<SelectItem> predavaneTypeList) {
		this.predavaneTypeList = predavaneTypeList;
	}

	public List<DeloDvijenie> getSelectedDvij() {
		return selectedDvij;
	}

	public void setSelectedDvij(List<DeloDvijenie> selectedDvij) {
		this.selectedDvij = selectedDvij;
	}

	public List<DeloDvijenie> getZabranenoVrashtane() {
		return zabranenoVrashtane;
	}

	public void setZabranenoVrashtane(List<DeloDvijenie> zabranenoVrashtane) {
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
}

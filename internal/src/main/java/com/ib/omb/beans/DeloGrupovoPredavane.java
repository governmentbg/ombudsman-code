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
public class DeloGrupovoPredavane extends IndexUIbean {

	private static final long serialVersionUID = -5250657837155736350L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DeloGrupovoPredavane.class);
	
	public static final int SELECTED_ADM_STR = 1;
	public static final int SELECTED_KORESP = 2;
	public static final int SELECTED_TEXT = 3;
	
	private static final String ERROR_KLASSIF = "general.errorClassif";
	private static final String DATE_SEND = "data";
	private static final String DATE_RETURN_TO = "dataDo";
	
	private List<SelectItem> predavaneTypeList;
	private List<DeloPredavane> selectedDelo;
	private List<DeloPredavane> zabranenoPredavane;
	private DeloDvijDAO dvijenieDao;
	
	private Date dvijDate;
	private Date returnToDate;
	private Integer dvijMethod;
	private String dvijInfo;
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
	boolean btnKorrespDisable;
	private boolean withTomove;

	@Inject
	private Flash flash;
	@Inject
	Event<DeloDvij> dvijEvent;
	
	@PostConstruct
	void init() {
		this.dvijenieDao = new DeloDvijDAO(getUserData());
		
		try {
			String param1 = ((SystemData) getSystemData()).getSettingsValue("delo.deloWithToms");
			this.withTomove = Objects.equals(param1, String.valueOf(OmbConstants.CODE_ZNACHENIE_DA));
		} catch(DbErrorException e) {
			LOGGER.error(getMessageResourceString(beanMessages, ERROR_KLASSIF), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			this.withTomove = false;
		}
		
		readDelo();
		loadMethods();
		initFields();
	}
	
	/**
	 * Чете и парсва списъка с дела, предадени от предната страница с флаша
	 */
	private void readDelo() {
		@SuppressWarnings("unchecked")
		List<Object[]> list = (List<Object[]>) flash.get(DeloList.SELECTED_DELO);
		this.selectedDelo = new ArrayList<>();
		this.zabranenoPredavane = new ArrayList<>();
		
		for(Object[] dok : list) {
			DeloPredavane d = new DeloPredavane();
			d.setDeloId(dok[0] == null ? null : ((Number) dok[0]).intValue());
			d.setRnDelo((String) dok[1]);
			d.setDeloDate(dok[2] == null ? null : new Date(((Timestamp) dok[2]).getTime()));
			d.setDeloType(dok[3] == null ? null : ((Number) dok[3]).intValue());
			d.setStatus(dok[4] == null ? null : ((Number) dok[4]).intValue());
			d.setCodeRefLead(dok[5] == null ? null : ((Number) dok[5]).intValue());
			d.setDeloName((String) dok[6]);
			d.setWithTom(dok[9] == null ? null : ((Number) dok[9]).intValue() == OmbConstants.CODE_ZNACHENIE_DA);
			d.setBrTom(dok[10] == null ? null : ((Number) dok[10]).intValue());
			
			if(zabranenoPredavane(d)) {
				this.zabranenoPredavane.add(d);
			}
			else {
				this.selectedDelo.add(d);
			}
		}
	}
	
	/**
	 * Зарежда списъка с начини на предаване и филтрира никому непотребните.
	 */
	private void loadMethods() {
		try {
			this.predavaneTypeList = // премахват се от списъка няколко опции - имейл, сеос, ссев, уеб еау и 'на друга регистратура'
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
		this.dvijDate = new Date();
		this.returnToDate = null;
		this.dvijMethod = OmbConstants.CODE_ZNACHENIE_PREDAVANE_NA_RAKA;
		this.dvijInfo = "";
		this.selectionType = null;
		this.freeKoresp = "";
	}
	
	/**
	 * Проверката кога едно избрано дело ще бъде преместено в списъка със забранените за предаване.
	 * @param d
	 * @return true, когато предаването е забранено
	 */
	private boolean zabranenoPredavane(DeloPredavane d) {
		return d.getDeloType() == OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM;
	}
	
	/**
	 * Вика се, когато се редактира ред в таблицата.
	 * @param row
	 */
	public void onRowEdit(DeloPredavane row) {
		if(row.isWithTom() && !validateTom(row)) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "deloDvij.nevalidenNomerNaTom"));
			scrollToMessages();
			row.setTomPredavane(null);
		}
	}
	
	/**
	 * Валидира написания номер на том за предаване.
	 * @param delo
	 * @return
	 */
	private boolean validateTom(DeloPredavane delo) {
		if(delo.tomPredavane == null) {
			return true;
		}
		else {
			return !(delo.tomPredavane < 1 || delo.tomPredavane > delo.getBrTom());
		}
	}
	
	/**
	 * Кликнат е бутонът предаване.
	 * Валидира и запраща делата на получателя или показва съобщение за грешка.
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
			for(DeloPredavane delo : this.selectedDelo) {
				DeloDvij dvij = new DeloDvij();
				
				dvij.setCodeRef(codeRef); // CODE_REF
				dvij.setDeloId(delo.getDeloId()); // DOC_ID
				dvij.setDvijDate(this.dvijDate); // DVIJ_DATE
				dvij.setReturnToDate(this.returnToDate); //RETURN_TO_DATE
				dvij.setDvijMethod(this.dvijMethod); // DVIJ_METHOD
				dvij.setDvijText(name); // DVIJ_TEXT
				dvij.setDvijInfo(this.dvijInfo); // DVIJ_INFO
				dvij.setTomNomer(delo.tomPredavane); // TOM_NOMER
				
				try {
					JPA.getUtil().runInTransaction(() -> this.dvijenieDao.save(dvij, getSystemData()));
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
	 * Валидира данните - дати, наличие на получател
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
		
		// проверява датата на изпращане да не е по-ранна от датата на някое от делата (затова от делата намира най-късното)
		Date latestDate = new Date();
		
		Optional<DeloPredavane> optional =
				this.selectedDelo
				.stream()
				.reduce((a, b) -> DateUtils.startDate(a.getDeloDate()).compareTo(DateUtils.startDate(b.getDeloDate())) > 0 ? a : b);
		if(optional.isPresent()) {
			latestDate = optional.get().getDeloDate();
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
		else if(this.selectionType == SELECTED_TEXT && (this.freeKoresp == null || this.freeKoresp.trim().isEmpty())) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "grupDvij.niamaIzbranPoluchatel"));
			return false;
		}
		
		// проверка на томовете
		if(this.useTomove()) {
			for(DeloPredavane delo : this.selectedDelo) {
				if(delo.isWithTom() && !validateTom(delo)) {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "deloDvij.imaNevalidenNomerNaTom"));
					return false;
				}
			}
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
		this.selectedDelo.remove(index);
	}
	
	/**
	 * Избран е получател от полето с административната структура.
	 * Изключва кореспондента да не се пише в него; свободният текст сам се disable-ва 
	 * с метода {@link #disabledText()}
	 */
	public void actionAdmStrComplete() {
		this.selectionType = SELECTED_ADM_STR;
		disableKorresp(true);		
	}
	
	/**
	 * Изтрит е изборът в полето с административната структура.
	 * Включва кореспондента; свободният текст сам се enable-ва 
	 * с метода {@link #disabledText()}
	 */
	public void actionAdmStrClear() {
		this.selectionType = null;
		disableKorresp(false);
	}
	
	/**
	 * Избран е получател от полето с кореспондент.
	 * Изключва адм. структура да не се пише в нея; свободният текст сам се disable-ва 
	 * с метода {@link #disabledText()}
	 */
	public void actionKorrespComplete() {
		this.selectionType = SELECTED_KORESP;
		disableAdmStr(true);
	}
	
	/**
	 * Изтрит е изборът в полето с кореспондент.
	 * Включва адм. структура; свободният текст сам се enable-ва 
	 * с метода {@link #disabledText()}
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
		if(this.freeKoresp.trim().isEmpty()) {
			this.selectionType = null;
			this.freeKoresp = this.freeKoresp.trim();
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
	
	/**
	 * Връша true, ако системната настойка за работа с томове е ДА
	 */
	public boolean useTomove() {
		return this.withTomove;
	}
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	public class DeloPredavane {
		private Integer deloId;
		private String rnDelo;
		private Date deloDate;
		private Integer deloType;
		private Integer status;
		private Integer codeRefLead;
		private String deloName;
		private boolean withTom;
		private Integer brTom;
		private Integer tomPredavane;
		
		public Integer getDeloId() {
			return deloId;
		}
		public void setDeloId(Integer deloId) {
			this.deloId = deloId;
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
		public Integer getCodeRefLead() {
			return codeRefLead;
		}
		public void setCodeRefLead(Integer codeRefLead) {
			this.codeRefLead = codeRefLead;
		}
		public String getDeloName() {
			return deloName;
		}
		public void setDeloName(String deloName) {
			this.deloName = deloName;
		}
		public boolean isWithTom() {
			return withTom;
		}
		public void setWithTom(boolean withTom) {
			this.withTom = withTom;
		}
		public Integer getBrTom() {
			return brTom;
		}
		public void setBrTom(Integer brTom) {
			this.brTom = brTom;
		}
		public Integer getTomPredavane() {
			return tomPredavane;
		}
		public void setTomPredavane(Integer tomPredavane) {
			this.tomPredavane = tomPredavane;
		}
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

	public List<SelectItem> getPredavaneTypeList() {
		return predavaneTypeList;
	}

	public void setPredavaneTypeList(List<SelectItem> predavaneTypeList) {
		this.predavaneTypeList = predavaneTypeList;
	}

	public List<DeloPredavane> getSelectedDelo() {
		return selectedDelo;
	}

	public void setSelectedDelo(List<DeloPredavane> selectedDelo) {
		this.selectedDelo = selectedDelo;
	}

	public List<DeloPredavane> getZabranenoPredavane() {
		return zabranenoPredavane;
	}

	public void setZabranenoPredavane(List<DeloPredavane> zabranenoPredavane) {
		this.zabranenoPredavane = zabranenoPredavane;
	}

	public boolean isWithTomove() {
		return withTomove;
	}

	public void setWithTomove(boolean withTomove) {
		this.withTomove = withTomove;
	}
}

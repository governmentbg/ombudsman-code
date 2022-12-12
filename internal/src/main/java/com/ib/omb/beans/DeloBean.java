package com.ib.omb.beans;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_STATUS_CONTINUED;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM;
import static com.ib.system.utils.SearchUtils.isEmpty;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.faces.view.facelets.FaceletContext;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.component.export.PDFOptions;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.TabChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.components.CompAccess;
import com.ib.omb.db.dao.DeloDAO;
import com.ib.omb.db.dao.DeloDeloDAO;
import com.ib.omb.db.dao.DeloDocDAO;
import com.ib.omb.db.dao.DeloStorageDAO;
import com.ib.omb.db.dao.LockObjectDAO;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloDelo;
import com.ib.omb.db.dto.DeloDoc;
import com.ib.omb.search.DeloSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.X;




/**
 * Въвеждане и актуализация на Преписка/Дело
 * 
 * @author s.marinov
 *
 */
@Named
@ViewScoped
public class DeloBean   extends IndexUIbean  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2934740148410006322L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DeloBean.class);
	public static final String  DELOFORM = "deloForm";
	public static final String  MSGPLSINS = "general.pleaseInsert";
	public static final String  ERRDATABASEMSG = "general.errDataBaseMsg";
	public static final String  SUCCESSAVEMSG = "general.succesSaveMsg";
	public static final String  deloMaxBrSheetsDefault = "delo.deloMaxBrSheetsDefault";
	
	
	private transient Delo delo;
	private transient Integer originalStatus;
	private transient DeloDAO		dao;
	private UserData ud;
	
	private boolean obshtodostap=false;
	private boolean hasRazdeli=true;
	private boolean hasToms=true;
	
	
	private DeloDoc currentDeloDoc=new DeloDoc();
	private transient DeloDocDAO		daoDoc;
	private LazyDataModelSQL2Array deloDocList;
	
	private DeloDelo currentDeloDelo=new DeloDelo();
	private transient DeloDeloDAO		daoDeloDelo;
	private LazyDataModelSQL2Array deloDeloList;
	
	private Date decodeDate = new Date(); 
	private Object[] selectedDeloP=null;
	
	private ArrayList<Object[]> selectedDeloMulty =new ArrayList<Object[]>();
	private ArrayList<Object[]> selectedDocsMulty =new ArrayList<Object[]>();
	
	private String deloName2;
	private String mess=null;

	private Integer originalDeloType=null;
	private boolean endTask;
	private boolean accessFinishTask;
	private boolean hideAll=false;
	
	private String protocolArchive;
	
	private String rnFullDelo;  
	
	//за съхранение
	private List<Object[]> deloStorageList;
	
	private List<Integer> zvenaCodesList=new ArrayList<Integer>();
	
	
	private ArrayList<Object[]> deloIndexList =new ArrayList<Object[]>();
	
	/**
	 * справка за достъп
	 */
	private LazyDataModelSQL2Array docAccessList;
	
	/**
	 * Прехвърляне от папка в папка
	 */
	private List <Object[]> selectedDeloList = new ArrayList<Object[]>();
	private Date dateTransfer = new Date();
	private Integer deloId;
	private Integer tomNomer;
	private boolean visibleTransfer = false;
	private String rnDelo;
	private Object[] selectedDelo;
	private Date selectedDeloDate;
	private boolean visibleTom = true;
	private boolean visibleTransferDocs = false;
	private boolean visibleMoveDocs = false;
	private Integer brTomove;
	/**
	 * Запълненост на томове
	 */
	private List<Integer[]> tomAndSheets;
	
	private Integer accView = 0; // ако е 0 => да позволи и в режим а разглеждане да се дава изричен достъп. Използва се когата преписката се извика от актуализаиця на документ
	
	/** */
	@PostConstruct
	void initData() {
		LOGGER.debug("!!! PostConstruct DeloBean !!!");

		setUd(getUserData(UserData.class));
		dao = new DeloDAO(getUserData());
		daoDoc=new DeloDocDAO(getUserData());
		daoDeloDelo=new DeloDeloDAO(getUserData());
			
		String param = JSFUtils.getRequestParameter("idObj");
		Integer deloId = null;
		if (param != null && !param.isEmpty()){
			deloId = Integer.valueOf(param);
		}
		
//		param="305";
		FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
		String param3 = (String) faceletContext.getAttribute("isView"); // 0 - актуализациял 1 - разглеждане
		Integer isView=0;
		if(!SearchUtils.isEmpty(param3)) {
			isView = Integer.valueOf(param3);
		}
				
		accView = isView; 							 // използва се само за комп. за изричен достъп!!
		param = JSFUtils.getRequestParameter("acc"); // този параметър има смисъл само в режим на разглеждане
		if (param != null && !param.isEmpty() && isView == 1){
			accView = Integer.valueOf(param);  
		}
		
		boolean fLockOk=true;
		if(isView == 0 && deloId!=null) { 
			// проверка за заключен документ
			fLockOk = checkForLock(deloId);
			if (fLockOk) {
				lockDelo(deloId);
			// отключване на всички обекти за потребителя(userId) и заключване на док., за да не може да се актуализира от друг
			}				
		}
		
		if (deloId!=null && fLockOk){
			
			loadDelo(deloId); // зарежда данните за документа
			
			//  журналира отварянето на обекта ///// Ще го правим ли????
			
			loadDeloDocsList();
			loadDeloDeloList();
			this.originalDeloType=this.delo.getDeloType();
			
			//извличане на данните за съхранение
			
			try {
				deloStorageList = new DeloStorageDAO(getUserData()).findDeloStorageData(deloId);			
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
			
		} else {
			actionDeloNew(); 
//			String paramJ = JSFUtils.getRequestParameter("idObjJournal"); // id na obekta ot jurnala   //TODO разглеждане от журнала
//			Integer docIdJ = null;
//			if (paramJ != null && !paramJ.isEmpty()){
//				docIdJ = Integer.valueOf(paramJ);
//			}
//			if(docIdJ == null) { 
//				actionDeloNew();  
//			}
		}	
		
		if (this.delo.getDeloDelo()==null) {
			this.delo.setDeloDelo(new DeloDelo());
			this.delo.getDeloDelo().setDelo(new Delo());
		}
		
		actionStatusDeloChange(false);
		try {
			if(Integer.valueOf(getSystemData().getSettingsValue("delo.deloWithToms"))!=OmbConstants.CODE_ZNACHENIE_DA) {
				visibleTom = false;
			}
			if(delo.getId() != null) {
				if(!this.dao.hasDeloAccess(delo, (isView!=1), getSystemData())) {	//проверка за достъп до преписката
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,getMessageResourceString(beanMessages, "delo.accessDenied"));
					actionDeloNew();
					setHideAll(true);
				} else {
					String param1 = getSystemData().getSettingsValue("delo.journalOpenDeloDoc"); // да се журналира ли отварянето
					if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
						// запис в журнала, че преписката/делото е отворено
						JPA.getUtil().runInTransaction(() -> this.dao.saveAudit(delo, SysConstants.CODE_DEIN_OPEN));
					}
				}
			}
			
		} catch ( DbErrorException e) {
			LOGGER.error(String.format("Грешка при извичане на достъп до дело/преписка: %s%s", this.delo.getId(),"! "),e);
		} catch (BaseException e) {
			LOGGER.error(String.format("Грешка при запис в журнала - отвaряне на дело/преписка: %s%s", this.delo.getId(),"! "),e);
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
	
	@SuppressWarnings("rawtypes")
	public void onTabChange(TabChangeEvent event) {
		if(event != null) {
			LOGGER.debug("onTabChange"+  "Active Tab: " + event.getTab().getId());
			rnFullDelo = this.delo.getRnDelo() + "/" + DateUtils.printDate(this.delo.getDeloDate());
			String activeTab =  event.getTab().getId();
			if(activeTab.equals("tabDocsDelo") && currentDeloDoc != null) {
				currentDeloDoc.setInputDate(new Date());
			}if(activeTab.equals("tabDeloDelo") && currentDeloDelo != null) {
				currentDeloDelo.setInputDate(new Date());
			}
		}
		
	}
	
	/**
	 * проверка за настройката и дали потребителя има нужните права - да приключи зад. към докумените, вложени в препидката
	 */
	public void actionStatusDeloChange(boolean check) {
		if (check) {
			if (delo.getStatus()!=null && (delo.getStatus()==OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_DA || delo.getStatus()==OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_UA)) {
				delo.setStatus(originalStatus);
				JSFUtils.addMessage(DELOFORM+":statusDelo",FacesMessage.SEVERITY_ERROR,"Не могат да бъдат избирани статуси: \"предадена в УА\" или \"предадена в ДА\"");
			}
		}
		originalStatus=delo.getStatus();
		accessFinishTask = false;
		if (Objects.equals(this.delo.getStatus(),OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED)) {
			try {
				//настройка -  При приключване на преписка се приключват задачите към нейните документи и вложените преписки 
				Integer s1 = ((SystemData)getSystemData()).getRegistraturaSetting(this.delo.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_10);
				if( Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_DA) &&
					getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_COMPLETE_REGISTRATURA_TASKS)) {
					accessFinishTask = true; 
				}
				
			} catch (DbErrorException e) {
				LOGGER.error(String.format("Грешка при извичане на настройка CODE_ZNACHENIE_REISTRATURA_SETTINGS_10 на регистратура: %s%s", this.delo.getRegistraturaId(),"! "),e);
			}
		}else {
			endTask=false;
		}
	}
	 
	
	/**
	 *  Зарежда данните на преписка/дело за редактиране
	 *   
	 * @param docId
	 * @return
	 */
	private void loadDelo(Integer deloId) {
		
		
		if(deloId != null){			
			try {

				JPA.getUtil().runWithClose(() -> delo = this.dao.findById(deloId));
			
				originalStatus=delo.getStatus();
				if (this.delo.getDeloType()!=null && this.delo.getDeloType()==OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM) {
					this.hasRazdeli=false;
					this.hasToms=true;
				}else {
					try {
						if ("1".equals(getSystemData().getSettingsValue("delo.deloWithToms"))) {
							if (delo.getWithTom()==1) {
								this.hasToms = true;				
							} else {
								this.hasToms = false;
							}				
						} else {
							this.hasToms = false;
						}
					
						if ("1".equals(getSystemData().getSettingsValue("delo.deloWithSections"))) {
							if (delo.getWithSection()==1) {
								this.hasRazdeli = true;
							} else {
								this.hasRazdeli = false;
							}
						} else {
							this.hasRazdeli = false;
						}
						
						if (delo.getFreeAccess()!=null && delo.getFreeAccess().equals(OmbConstants.CODE_ZNACHENIE_DA)) {
							this.obshtodostap=false;
						}else {
							this.obshtodostap=true;
						}
					} catch (DbErrorException e) {
						JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
						LOGGER.error(e.getMessage(), e);
					}
				}
				
				
				
				if(delo.getProtocolData() != null) {
					protocolArchive = "  Протокол за архивиране "+ delo.getProtocolData()[1] + "/" + DateUtils.printDate((Date)delo.getProtocolData()[2]) ;
				}
				 		
			} catch (BaseException e) {				
				JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
				LOGGER.error(e.getMessage(), e);
			}
	
		}
	}
	
	/**
	 * Проверка за заключен документ 
	 * @param idObj
	 * @return
	 */
	private boolean checkForLock(Integer idObj) {
		boolean res = true;
		LockObjectDAO daoL = new LockObjectDAO();		
		try { 
			Object[] obj = daoL.check(getUd().getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO, idObj);
			if (obj != null) {
				 res = false;
				 String msg = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, Integer.valueOf(obj[0].toString()), getUserData().getCurrentLang(), new Date())   
						       + " / " + DateUtils.printDate((Date)obj[1]);
				 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,getMessageResourceString(beanMessages, "delo.lockedDelo"), msg);
			}
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при проверка за заключена преписка! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		return res;
	}
	
	/**
	 * Заключване на дело, като преди това отключва всички обекти, заключени от потребителя
	 * @param idObj
	 */
	public void lockDelo(Integer idObj) {	
		
		LockObjectDAO daoL = new LockObjectDAO();		
		try { 
			JPA.getUtil().runInTransaction(() -> 
				daoL.lock(getUd().getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO, idObj, null)
			);
		} catch (BaseException e) {
			LOGGER.error("Грешка при заключване на документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}			
	}

	
	/**
	 * при излизане от страницата - отключва обекта и да го освобождава за актуализация от друг потребител
	 */
	@PreDestroy
	public void unlockDelo(){
        if (!getUd().isReloadPage()) {
        	LockObjectDAO daoL = new LockObjectDAO();	
        	try { 
	        	
	        	JPA.getUtil().runInTransaction(() -> 
					daoL.unlock(ud.getUserId())
				);
        	} catch (BaseException e) {
    			LOGGER.error("Грешка при отключване на документ! ", e);
    			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
    		}
        	getUd().setPreviousPage(null);
        	
        }          
        getUd().setReloadPage(false);
	}
	
	private void loadDeloDocsList() {
		setDeloDocList(new LazyDataModelSQL2Array(getDaoDoc().createSelectDocListInDelo(delo.getId()), "a2 desc"));	
	}
	
	private void loadDeloDeloList() {
		setDeloDeloList(new LazyDataModelSQL2Array(getDaoDeloDelo().createSelectDeloListInDelo(delo.getId()), "a2 desc"));	
	}
	
	
	/** Ново дело
	 * 
	 * @return
	 */
	public boolean actionDeloNew() {
		boolean bb = true;
			
		
		try {
			this.delo = new Delo();
			this.delo.setRegistraturaId(getUserData(UserData.class).getRegistratura());
			this.delo.setStatus(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ACTIVE);
			this.delo.setDeloDate(new Date());
			this.delo.setStatusDate(new Date());
			this.delo.setBrTom(1);
			if (getSystemData().getSettingsValue(deloMaxBrSheetsDefault)!=null) {
				this.delo.setMaxBrSheets(Integer.valueOf(getSystemData().getSettingsValue(deloMaxBrSheetsDefault)));	
			}else {
				this.delo.setMaxBrSheets(1);
			}
			
			this.delo.setDeloDelo(new DeloDelo()); 
			this.delo.getDeloDelo().setDelo(new Delo());
		
			if ("1".equals(getSystemData().getSettingsValue("delo.deloWithToms"))) {
				this.hasToms = true;				
			} else {
				this.hasToms = false;
			}
			
			if ("1".equals(getSystemData().getSettingsValue("delo.deloWithSections"))) {
				this.hasRazdeli = true;
			} else {
				this.hasRazdeli = false;
			}
			
			obshtodostap = true; 
			Integer s1 = ((SystemData)getSystemData()).getRegistraturaSetting(delo.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_12);
			if( Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_NE)) {
				obshtodostap = false; 
				this.delo.setFreeAccess(OmbConstants.CODE_ZNACHENIE_DA);
			}
			
			Integer tipDelo = ((SystemData)getSystemData()).getRegistraturaSetting(this.delo.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_14);
			if(tipDelo!=null && !tipDelo.equals(0)) {
				this.delo.setDeloType(tipDelo); 
			}
			
			selectedDeloP=null;
			unlockDelo();
			this.rnFullDelo = null;
		} catch (DbErrorException e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		}
		
		
		return bb;
	}
	
	
	public void actionSearchRnDelo() {
		if (delo.getRnDelo()!=null && !delo.getRnDelo().equals("") && delo.getDeloDate()!=null && delo.getDeloType()!=null) {
			try {
				JPA.getUtil().runWithClose(() -> mess=dao.validateRnDelo(this.delo));

				if (mess!=null) {
					JSFUtils.addMessage(DELOFORM+":regNomer",FacesMessage.SEVERITY_ERROR,mess);
				}	
			} catch (BaseException e) {				
				JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
				LOGGER.error(e.getMessage(), e);
			}
		
		}		
	}
	
	public String actionGotoViewDoc() {
		return "docView.xhtml?faces-redirect=true&idObj=" + Integer.valueOf(this.delo.getProtocolData()[0].toString());
	}
		
	/**
	 * Запис на дело
	 */
	public void actionSave() {
		
	
		if (obshtodostap) {
			this.delo.setFreeAccess(OmbConstants.CODE_ZNACHENIE_NE);
		}else {
			this.delo.setFreeAccess(OmbConstants.CODE_ZNACHENIE_DA);
		} 
		if (hasRazdeli) {
			this.delo.setWithSection(OmbConstants.CODE_ZNACHENIE_DA);
		}else {
			this.delo.setWithSection(OmbConstants.CODE_ZNACHENIE_NE);
		} 
		if (hasToms) {
			this.delo.setWithTom(OmbConstants.CODE_ZNACHENIE_DA);
		}else {
			this.delo.setWithTom(OmbConstants.CODE_ZNACHENIE_NE);
		} 
		
		
		try {
			if(checkDataDelo()) { 			
			
				boolean isNewDelo=false;
				if (this.delo.getId()==null) {
					isNewDelo=true;
				}	
				
				X<String> msg = X.empty(); // така ще си получа и съобщението от записа
				JPA.getUtil().runInTransaction(() -> this.delo = this.dao.save(delo,endTask,getSystemData(),msg)); 
			
				String dvijMsg;
				if (msg.isPresent()) {
					DeloDvijenia ddd = (DeloDvijenia) JSFUtils.getManagedBean("deloDvijenia");
					if (ddd != null) {
						ddd.setRnFullDelo(null); // има ново движение и трябва да се зареди пак списъка
					}
					dvijMsg = " " + msg.get();
				} else {
					dvijMsg = "";
				}
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) + dvijMsg);
				if (isNewDelo) {
					lockDelo(this.delo.getId());	
				}
				originalStatus=delo.getStatus();
			}		
		} catch (ObjectInUseException e) {
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			LOGGER.error(e.getMessage(), e);
		} catch (BaseException e) {			
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		} 
			
		
	}
	
	public void actionConfirmAccess() {
		String  cmdStr = "PF('modalAccess').hide();";
		PrimeFaces.current().executeScript(cmdStr);
		actionSave();
	}
	
	/**
	 * проверка за задължителни полета при запис на дело
	 * @throws DbErrorException 
	 */
	public boolean checkDataDelo() throws DbErrorException {
		boolean flagSave = true;	
	
		if(delo.getRnDelo() == null || delo.getRnDelo().isEmpty()) {
			JSFUtils.addMessage(DELOFORM+":regNomer",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "repDoc.regnom")));
			flagSave = false;
		}
		if(delo.getDeloDate()==null) {
			JSFUtils.addMessage(DELOFORM+":regDate",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.dateDoc")));
			flagSave = false;
		}else {
			if (DateUtils.startDate(delo.getDeloDate()).after(DateUtils.startDate(new Date()))) {
				JSFUtils.addMessage(DELOFORM+":regDate",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"delo.dateAfterToday"));
				flagSave = false;
			}
			
			if (this.delo.getDeloDelo()!=null && this.delo.getDeloDelo().getInputDate()!=null && DateUtils.startDate(delo.getDeloDate()).after(DateUtils.startDate(this.delo.getDeloDelo().getInputDate()))) {
				JSFUtils.addMessage(DELOFORM+":inpDatDelo",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"delo.dateVlaganeAfterDatReg"));
				flagSave = false;
			}
		}
		if(delo.getDeloType() == null) {
			JSFUtils.addMessage(DELOFORM+":typeDelo",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.type")));
			flagSave = false;
		}
		if(delo.getStatus() == null) {
			JSFUtils.addMessage(DELOFORM+":statusDelo",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.status")));
			flagSave = false;
		}
		if(delo.getStatusDate() == null) {
			JSFUtils.addMessage(DELOFORM+":statusDate",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "delo.dateStatus")));
			flagSave = false;
		}else {
			
			if (delo.getDeloDate()!=null && DateUtils.startDate(delo.getDeloDate()).after(DateUtils.startDate(delo.getStatusDate()))) {
				JSFUtils.addMessage(DELOFORM+":statusDate",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"delo.dateAfterStatusDate"));
				flagSave = false;
			}
			if (delo.getDeloDate()!=null && DateUtils.startDate(new Date()).before(DateUtils.startDate(delo.getStatusDate()))) {
				JSFUtils.addMessage(DELOFORM+":statusDate",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"delo.dateStatusAfterToday"));
				flagSave = false;
			}
		}
		
		if(delo.getDeloName() == null || delo.getDeloName().isEmpty()) {
			JSFUtils.addMessage(DELOFORM+":naimenovanie",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.nameDelo")));
			flagSave = false;
		}
		
		if (this.delo.getWithTom()!=null && this.delo.getWithTom()==OmbConstants.CODE_ZNACHENIE_DA) {
			if (this.delo.getBrTom()==null || this.delo.getBrTom()<1) {
				JSFUtils.addMessage(DELOFORM+":brToms",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "delo.brToms")));
				flagSave = false;	
			}
			if (this.delo.getMaxBrSheets()==null || this.delo.getMaxBrSheets()<1) {
				JSFUtils.addMessage(DELOFORM+":brPages",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "delo.brPages")));
				flagSave = false;	
			}
		}
		
		if (hasToms && delo.getDeloDelo()!=null && delo.getDeloDelo().getDeloId()!=null && (delo.getDeloDelo().getTomNomer()==null || delo.getDeloDelo().getTomNomer()<1)) {
			JSFUtils.addMessage(DELOFORM+":tomNomer1",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "delo.tomNomer")));
			flagSave = false;
		}else {
			if (hasToms && delo.getDeloDelo()!=null && delo.getDeloDelo().getDelo().getBrTom()!=null && delo.getDeloDelo().getDelo().getBrTom()<delo.getDeloDelo().getTomNomer()) {
				JSFUtils.addMessage(DELOFORM+":tomNomer",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "delo.brTomsMax", delo.getDeloDelo().getDelo().getBrTom()) );
				flagSave = false;	
			}	
		}
		
		if (this.delo.getId()!=null && this.delo.getCodeRefLead()!=null) {
			if (this.dao.isDeloAccessDenied(this.delo.getId(), this.delo.getCodeRefLead())) {
				JSFUtils.addMessage(DELOFORM+":vodeshtSluj",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "delo.vodeshtAccess") );
				flagSave = false;	
			}	
		}
		
		
		
		
		
		return flagSave ;
	}

	/**
	 * Изтриване на дело
	 * 
	 * 
	 */
	public void actionDelete() {
		LOGGER.debug("Изтриване на дело/преписка >> actionDelete");
		try {
	
			JPA.getUtil().runInTransaction(() ->  this.dao.delete(delo)); 
		
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,  IndexUIbean.getMessageResourceString(UI_beanMessages, "general.successDeleteMsg") );
			actionDeloNew();
			
		}  catch (ObjectInUseException e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, e.getMessage());
		} catch (BaseException e) {			
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	 
	public void actionTypeChange() {
		
		if (this.delo.getId()!=null && originalDeloType!=null 
				&& (originalDeloType==OmbConstants.CODE_ZNACHENIE_DELO_TYPE_JALBA
				|| originalDeloType==OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NPM
				|| originalDeloType==OmbConstants.CODE_ZNACHENIE_DELO_TYPE_SAMOS
						) && (this.delo.getDeloType()==null || !originalDeloType.equals(this.delo.getDeloType()))) {
			JSFUtils.addMessage(DELOFORM+":typeDelo",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"delo.deloTypeChange"));
			this.delo.setDeloType(this.originalDeloType);
		}else {
			if (this.delo.getDeloType()!=null 
					&& (this.delo.getDeloType()==OmbConstants.CODE_ZNACHENIE_DELO_TYPE_JALBA
					|| this.delo.getDeloType()==OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NPM
					|| this.delo.getDeloType()==OmbConstants.CODE_ZNACHENIE_DELO_TYPE_SAMOS
							)) {
				JSFUtils.addMessage(DELOFORM+":typeDelo",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"delo.deloType"));
				this.delo.setDeloType(originalDeloType);
			}
		}
		
		if (this.delo.getId()!=null && originalDeloType!=null && this.delo.getDeloType()!=null
				&& this.delo.getDeloType()==OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM && originalDeloType!=OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM) {
			//nqma pravo saobshtenie
			JSFUtils.addMessage(DELOFORM+":typeDelo",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"delo.statusType"));
			this.delo.setDeloType(this.originalDeloType);
		}else {
			this.delo.setBrTom(1);
			
			try {
				if (getSystemData().getSettingsValue(deloMaxBrSheetsDefault)!=null) {
					this.delo.setMaxBrSheets(Integer.valueOf(getSystemData().getSettingsValue(deloMaxBrSheetsDefault)));	
				}else {
					this.delo.setMaxBrSheets(1);
				}
			}  catch (DbErrorException e) {
				JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
				LOGGER.error(e.getMessage(), e);
			}
			if (this.delo.getDeloType()!=null && this.delo.getDeloType()==OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM) {
				this.hasRazdeli=false;
				this.hasToms=true;
			}else {
				try {
					if ("1".equals(getSystemData().getSettingsValue("delo.deloWithToms"))) {
						this.hasToms = true;				
					} else {
						this.hasToms = false;
					}
				
					if ("1".equals(getSystemData().getSettingsValue("delo.deloWithSections"))) {
						this.hasRazdeli = true;
					} else {
						this.hasRazdeli = false;
					}
				} catch (DbErrorException e) {
					JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
					LOGGER.error(e.getMessage(), e);
				}
			}
			actionSearchRnDelo();
			this.originalDeloType=this.delo.getDeloType();
		}
	}
	
   
	       
	public Date getToday() {
		return new Date();
	}
   
	/***************** NASHTO DELO STUFF ************************/
	public void actionRemoveCurrentDeloFromDelo() {
		
		if (this.delo.getStatus()!=null && (this.delo.getStatus()==OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_UA || this.delo.getStatus()==OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_DA)) {
			 //"Преписката е включена в протокол за архивиране и не може да бъде извадена."
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "docu.warnDeloOutMsg1"));
		}else {
			try {
				JPA.getUtil().runInTransaction(() ->  this.delo=dao.removeCurrentFromDelo(this.delo, getSystemData()));
				
				DeloDvijenia ddd = (DeloDvijenia) JSFUtils.getManagedBean("deloDvijenia");
				if (ddd != null) {
					ddd.setRnFullDelo(null); // за всеки случай да се презаредят движенията, защото
											 // изтриване на влагане може да доведе до променя в движенията
				}

				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,  IndexUIbean.getMessageResourceString(beanMessages, "docu.successDeloOutMsg2") );
				
			} catch (BaseException e) {
				JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
				LOGGER.error(e.getMessage(), e);
			}
		}
		
	}
	public void actionDostap() {
		
		if (delo.getDeloAccess()==null || delo.getDeloAccess().isEmpty()) {
			try {
				JPA.getUtil().runWithClose(() -> this.dao.loadDeloAccess(delo));
			} catch (BaseException e) {
				JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
				LOGGER.error(e.getMessage(), e);
			}
		}
		//DOCFORMTABS     = "docForm:tabsDoc";
		try {
			if (zvenaCodesList.isEmpty()) {
				Map<Integer, Object> specs = new HashMap<>();
		        specs.put(OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE, OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO);
		        
		        List<SystemClassif> zvena = getSystemData().queryClassification(OmbConstants.CODE_CLASSIF_ADMIN_STR, null, decodeDate, getUserData().getCurrentLang(), specs);
				for (int i = 0; i < zvena.size(); i++) {
					this.zvenaCodesList.add(zvena.get(i).getCode());
				}				
			}
			
			((CompAccess)FacesContext.getCurrentInstance().getViewRoot().findComponent("deloForm:tabsDelo").findComponent("deloAccessComp")).initAutoCompl();
		} catch (DbErrorException e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		}
		
		String  cmdStr = "PF('modalAccess').show();";
		PrimeFaces.current().executeScript(cmdStr);
	}
	
	public void actionSastoqnieToms() {
		//TODO
	}
	
	public void actionVlagane() {
		searchRnDelo( delo.getDeloDelo().getDelo().getRnDelo(),  "modalDeloSearch",  false);
	}
	
	/** таб "Влагане в преписка"
	* Ръчно въвеждане на номер на дело/преписка
	* Търсене на преписка с въведения номер - при излизане от полето
	*/
	public void actionSearchRnDeloP2(boolean rnEQ) {	
	   if(!isEmpty(delo.getDeloDelo().getDelo().getRnDelo())) {
		    searchRnDelo(delo.getDeloDelo().getDelo().getRnDelo(),  "modalDeloSearch",  rnEQ);
	   }else {
		  initDeloDeloLink2();	 
	   }
	}
	
	/**
    * Търсене на преписка по номер
    * @param rnDelo
    * @param varModal
    * @param rnEQ
    * @param nastr
    * @param inpDate
    * @return
    */
   private Object[] searchRnDelo(String rnDelo, String varModal, boolean rnEQ) {
	  Object[] sDelo = null;
	  
       if(rnDelo == null){
    	   rnDelo = "";  
       }
	   DeloSearch  tmp = new DeloSearch(getUserData(UserData.class).getRegistratura()); // регистртура на документа.
	   tmp.setRnDelo(rnDelo);
	   tmp.setRnDeloEQ(rnEQ);
	   tmp.setNotRelDeloId(delo.getId()); 
	   tmp.buildQueryComp(getUserData());
	   
	   LazyDataModelSQL2Array lazy =   new LazyDataModelSQL2Array(tmp, "a1 desc");
	   int rc = lazy.getRowCount();
	   if (rc== 0 && rnEQ) {
			  
			   initDeloDeloLink2(); /// от панела....преписка - преписка
		  
		   LOGGER.debug("Не е намерена преписка с посочения номер!");
		   JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, "Не е намерена преписка/дело с посочения номер или влагането не е позволено!"); 
		   
	   } else if(rc == 1 && rnEQ) { // само при пълно съвпадение на номера
		   
		   List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);
		   sDelo = new Object[8];
		   if(result != null) {
		       sDelo =  result.get(0);		
			    
		       setSelectedDeloP(sDelo);  
			   
		   }	
		   LOGGER.debug("Намерена е само една преписка с този рег. номер - данните да се заредят без да излиза модалния");
	   } else {
		   sDelo = new Object[8];		
		   String  cmdStr = "PF('"+varModal+"').show();";
		   PrimeFaces.current().executeScript(cmdStr);
	   }
	  return sDelo;		  
   }
	   
   /**
    * Инициализира / Премахва отлагането на преписка в преписката 
    */
    public void initDeloDeloLink2() {
 	    selectedDeloP = null;
 	   	delo.setDeloDelo(new DeloDelo());
 	   	delo.getDeloDelo().setDelo(new Delo());
 	    deloName2 = null;
 	}

	
//	public void actionSahranqvane() {
//		//
//	}
	
	 
	
	
   /**************** DELO DOC STUFF ***********************/
    public void validateInputDateDoc(FacesContext context, UIComponent component,
        Object value) throws ValidatorException{
    	
	    	if (value==null) {
				JSFUtils.addMessage(""+component.getClientId(),FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.deloInpDate")));
				  FacesMessage msg = new FacesMessage("");
		            throw new ValidatorException(msg);
			}else {
				if (delo.getDeloDate()!=null && DateUtils.startDate(delo.getDeloDate()).after(DateUtils.startDate(SearchUtils.asDate(value)))) {
					JSFUtils.addMessage(""+component.getClientId(),FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"delo.dateVlaganeAfterDatReg"));
					  FacesMessage msg = new FacesMessage("");
			            throw new ValidatorException(msg);
				}
			}
          
        
    }
    public void validateInputTomDoc(FacesContext context, UIComponent component,
    		Object value) throws ValidatorException{
    	if (this.hasToms && (value==null || value.toString().contentEquals("") || Integer.valueOf(value.toString())<1)) {
			JSFUtils.addMessage(""+component.getClientId(),FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "delo.tomNomer")));
			FacesMessage msg = new FacesMessage("");
    		throw new ValidatorException(msg);
    	}else {
	    	if (this.hasToms && delo.getBrTom()!=null && Integer.valueOf(value.toString())>delo.getBrTom()) {
				JSFUtils.addMessage(""+component.getClientId(),FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "delo.brTomsMax", delo.getBrTom()) );
				FacesMessage msg = new FacesMessage("");
	    		throw new ValidatorException(msg);
			}
    	}
    	
    }
    
    public boolean checkDocsInDeloData() {
    	 
    	boolean go=true;
    	if (this.currentDeloDoc.getInputDate()==null) {
			JSFUtils.addMessage(DELOFORM+":inpDatDeloDoc",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.deloInpDate")));
			go=false;
		}else {
			if (delo.getDeloDate()!=null && DateUtils.startDate(delo.getDeloDate()).after(DateUtils.startDate(this.currentDeloDoc.getInputDate()))) {
				JSFUtils.addMessage(DELOFORM+":inpDatDeloDoc",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"delo.dateVlaganeAfterDatReg"));
				go = false;
			}
		}
		if (this.hasToms && (this.currentDeloDoc.getTomNomer()==null || this.currentDeloDoc.getTomNomer()<1)) {
				JSFUtils.addMessage(DELOFORM+":tomNomerDeloDoc",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "delo.tomNomer")));
				go=false;
		}else {
			if (this.hasToms && delo.getBrTom()!=null && currentDeloDoc.getTomNomer()>delo.getBrTom()) {
				JSFUtils.addMessage(DELOFORM+":tomNomerDeloDoc",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "delo.brTomsMax", delo.getBrTom()) );
				go = false;	
				
			}
		}
		
		if (this.hasRazdeli && (this.currentDeloDoc.getSectionType()==null)) {
			JSFUtils.addMessage(DELOFORM+":typeDocInDeloRazdel",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "delo.razdel")));
			go=false;
		}
		
		 
		
		return go;
    }
	
	public void actionOpenModalDocs() {
		
		 
		if (checkDocsInDeloData()) {
			String  cmdStr = "PF('modalDocSearchM').show();";
			PrimeFaces.current().executeScript(cmdStr);	
		}
			
		
	}
	
	 
	public void actionSaveEditDeloDocRow(RowEditEvent<Object[]> event) {
		try {
			 
			
			currentDeloDoc=daoDoc.findById(Integer.valueOf(event.getObject()[0].toString()));
			if (event.getObject()[6]!=null) {
				currentDeloDoc.setInputDate(SearchUtils.asDate(event.getObject()[6]));	
			}else {
				currentDeloDoc.setInputDate(null);
			}
			if (event.getObject()[7]!=null) {
				currentDeloDoc.setEkzNomer(Integer.valueOf(event.getObject()[7].toString()));	
			}else {
				currentDeloDoc.setEkzNomer(null);
			}
			if (event.getObject()[8]!=null) {
				currentDeloDoc.setTomNomer(Integer.valueOf(event.getObject()[8].toString()));	
			}else {
				currentDeloDoc.setTomNomer(null);
			}
			if (event.getObject()[9]!=null) {
				currentDeloDoc.setSectionType(Integer.valueOf(event.getObject()[9].toString()));	
			}else {
				currentDeloDoc.setSectionType(null);
			}
			
			if (checkDocsInDeloData()) {
				JPA.getUtil().runInTransaction(() -> daoDoc.save(currentDeloDoc));
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );
				this.currentDeloDoc=new DeloDoc();
			}
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис/актуализация на влагане на документ в преписка! ", e);
			
		}
		
		
	}
	public void actionSaveMyltyDeloDoc() {
		
		try {
//			List<Integer> lst=new ArrayList<Integer>();
//
//			for (int i = 0; i < selectedDocsMulty.size(); i++) {
//				
//				lst.add(SearchUtils.asInteger(selectedDocsMulty.get(i)[0]));
//			}
			Integer s9 = ((SystemData)getSystemData()).getRegistraturaSetting(this.delo.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_9);
			String dvijMsg = "";

			JPA.getUtil().begin();
			List<DeloDoc> list = daoDoc.saveInDeloRow(selectedDocsMulty, this.delo, currentDeloDoc.getTomNomer(), currentDeloDoc.getSectionType(), currentDeloDoc.getInputDate(), currentDeloDoc.getEkzNomer());
			if (s9 != null && s9.equals(SysConstants.CODE_ZNACHENIE_DA)) {
				for (DeloDoc deloDoc : list) {
					 String msg = daoDoc.checkCreateDvij(deloDoc, getSystemData());
					 if (msg != null) { // не за всяка може да има съобщение
						 dvijMsg = " " + msg;
					 }
				}
			}
			JPA.getUtil().commit();
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) + dvijMsg);
			
		} catch (Exception e) {
			JPA.getUtil().rollback();
			LOGGER.error("Грешка при запис на влагане на документ в преписка! ", e);
			
		} finally {
			JPA.getUtil().closeConnection();	
		}
		
		if (this.deloDocList==null) {
			loadDeloDocsList();
		}else {
			this.deloDocList.loadCountSql();
		}
	}
	
	public String actionViewDeloDoc(Integer deloDocId) {
		return "docView.xhtml?faces-redirect=true&idObj=" + deloDocId;
	}
	
	public void actionRemoveDeloDoc(Object[] row) {
		
		try {
			Integer deloDocId = ((Number)row[0]).intValue();	
			Integer docId = ((Number)row[1]).intValue();	
			
			JPA.getUtil().runInTransaction(() -> {
				daoDoc.deleteById(deloDocId);
			
				if (docId.equals(delo.getInitDocId())) { // маха се иницииращия док
					delo.setInitDocId(null);
					
					JPA.getUtil().getEntityManager().createNativeQuery(
						"update DELO set INIT_DOC_ID = null where DELO_ID = ?1")
						.setParameter(1, delo.getId()).executeUpdate();
					
					StringBuilder ident = new StringBuilder();
					ident.append("Иницииращият документ ");
					ident.append(row[2] + "/" + DateUtils.printDate((Date) row[3]));
					ident.append(" е изваден от преписка "+delo.getIdentInfo()+".");

					SystemJournal journal = new SystemJournal(OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO, delo.getId(), ident.toString());
					journal.setCodeAction(SysConstants.CODE_DEIN_SYS_OKA);
					journal.setDateAction(new Date());
					journal.setIdUser(getUserData().getUserId());
					dao.saveAudit(journal);
				}
			});
			
//			loadDeloDocsList();
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );
			this.deloDocList.loadCountSql();
		} catch (BaseException e) {			
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);

		}
	}
	
	/**************** DELO DELO STUFF ***********************/
	
	public void validateInputDateDelo(FacesContext context, UIComponent component,
	        Object value) throws ValidatorException{
		if (value==null) {
			JSFUtils.addMessage(""+component.getClientId(),FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.deloInpDate")));
			FacesMessage msg = new FacesMessage("");
            throw new ValidatorException(msg);
		}else {
			if (delo.getDeloDate()!=null && DateUtils.startDate(delo.getDeloDate()).after(DateUtils.startDate(SearchUtils.asDate(value)))) {
				JSFUtils.addMessage(""+component.getClientId(),FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"delo.dateVlaganeAfterDatReg"));
				FacesMessage msg = new FacesMessage("");
	            throw new ValidatorException(msg);
			}
		}
    }
	
  	public void validateInputTomDelo(FacesContext context, UIComponent component,
	    		Object value) throws ValidatorException{
    	if (this.hasToms && (value==null || value.toString().contentEquals("") || Integer.valueOf(value.toString())<1)) {
			JSFUtils.addMessage(""+component.getClientId(),FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "delo.tomNomer")));
			FacesMessage msg = new FacesMessage("");
    		throw new ValidatorException(msg);
		}else {
			if (this.hasToms && delo.getBrTom()!=null && Integer.valueOf(value.toString())>delo.getBrTom()) {
				JSFUtils.addMessage(""+component.getClientId(),FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "delo.brTomsMax", delo.getBrTom()) );
				FacesMessage msg = new FacesMessage("");
	    		throw new ValidatorException(msg);
			}	
		}
    }
	
	public void actionOpenModalDeloDelo() {
		
		boolean go=true;
		if (this.currentDeloDelo.getInputDate()==null) {
			JSFUtils.addMessage(DELOFORM+":inpDatDeloDelo",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.deloInpDate")));
			go=false;
		}else {
			if (delo.getDeloDate()!=null && DateUtils.startDate(delo.getDeloDate()).after(DateUtils.startDate(this.currentDeloDelo.getInputDate()))) {
				JSFUtils.addMessage(DELOFORM+":inpDatDeloDelo",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,"delo.dateVlaganeAfterDatReg"));
				go = false;
			}
		}
		if (this.hasToms &&  (this.currentDeloDelo.getTomNomer()==null || this.currentDeloDelo.getTomNomer()<1)) {
				JSFUtils.addMessage(DELOFORM+":tomNomerDeloDelo",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "delo.tomNomer")));
				go=false;
		}else {
			if (this.hasToms && delo.getBrTom()!=null && currentDeloDelo.getTomNomer()>delo.getBrTom()) {
				JSFUtils.addMessage(DELOFORM+":tomNomerDeloDelo",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "delo.brTomsMax", delo.getBrTom()) );
				go = false;	
				
			}	
		}
		
		
		if (go) {
			String  cmdStr = "PF('modalDeloSearchMulty').show();";
			PrimeFaces.current().executeScript(cmdStr);	
		}
		
		
		
	}
	
	public void actionSaveDeloDeloMulty() {
		
		try {
			Integer s9 = ((SystemData)getSystemData()).getRegistraturaSetting(this.delo.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_9);
			String dvijMsg = "";
			
			JPA.getUtil().begin();
			List<DeloDelo> list = daoDeloDelo.save(selectedDeloMulty, this.delo, currentDeloDelo.getTomNomer(), currentDeloDelo.getInputDate());
			if (s9 != null && s9.equals(SysConstants.CODE_ZNACHENIE_DA)) {
				for (DeloDelo deloDelo : list) {
					 String msg = daoDeloDelo.checkCreateDvij(deloDelo, getSystemData());
					 if (msg != null) { // не за всяка може да има съобщение
						 dvijMsg = " " + msg;
					 }
				}
			}
			JPA.getUtil().commit();
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) + dvijMsg);
			
			
		} catch (Exception e) {
			JPA.getUtil().rollback();
			LOGGER.error("Грешка при запис на влагане на преписка в преписка! ", e);
		} finally {
			JPA.getUtil().closeConnection();
		}

		if (this.deloDeloList==null) {
			loadDeloDeloList();
		}else {
			this.deloDeloList.loadCountSql();	
		}
	}
	
	public void actionEditDeloDelo(Integer deloDeloId) {
		 
		try {
			currentDeloDelo=daoDeloDelo.findById(deloDeloId);
		} catch (DbErrorException e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		}
		
	}
	
	
	public void actionSaveEditDeloDeloRow(RowEditEvent<Object[]> event) {
		try {
			 
			
			currentDeloDelo=daoDeloDelo.findById(Integer.valueOf(event.getObject()[0].toString()));
			currentDeloDelo.setInputDate(SearchUtils.asDate(event.getObject()[7]));
			if (event.getObject()[5]!=null && !event.getObject()[5].toString().isEmpty()) {
				currentDeloDelo.setTomNomer(Integer.valueOf(event.getObject()[5].toString()));	
			}
			

			JPA.getUtil().runInTransaction(() -> daoDeloDelo.save(currentDeloDelo));
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );
			this.currentDeloDelo=new DeloDelo();
		} catch (BaseException e) {			
			LOGGER.error("Грешка при актуализация на преписка в преписка! ", e);
			
		}
		
		
	}
	
	
	public String actionViewDeloDelo(Integer deloDeloId) {
		return "deloView.xhtml?faces-redirect=true&idObj=" + deloDeloId;
	}
	
	public void actionRemoveDeloDelo(Object[] row) {
		if (SearchUtils.asInteger(row[4])!=null && (SearchUtils.asInteger(row[4])==OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_UA || SearchUtils.asInteger(row[4])==OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_DA)) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,"Преписката е включена в протокол за архивиране и не може да бъде извадена.");
		}else {
			try {
				
				JPA.getUtil().runInTransaction(() -> {
					daoDeloDelo.deleteById(((Number)row[0]).intValue());
					
					Integer childStatus = SearchUtils.asInteger(row[4]);
					
					if (Objects.equals(childStatus, CODE_ZNACHENIE_DELO_STATUS_COMPLETED)
						&& Objects.equals(this.delo.getDeloType(), CODE_ZNACHENIE_DELO_TYPE_NOM)) {
						
						Integer childDeloId = SearchUtils.asInteger(row[1]);
						// тази която се изважа е била приключена и трябва да се смени на продължена
						Delo child = JPA.getUtil().getEntityManager().find(Delo.class, childDeloId); // за да се вземе само обекта, а не всичко
																							// натворено във финдБъИд
						child.setStatus(CODE_ZNACHENIE_DELO_STATUS_CONTINUED);
						child.setStatusDate(new Date());
	
						StringBuilder sb = new StringBuilder();
						sb.append(this.delo.getRnDelo()+"/");
						sb.append(DateUtils.printDate(this.delo.getDeloDate()));
						if (row[5] != null) {
							sb.append(",т." + row[5]);
						}
						sb.append(" на " + DateUtils.printDate(new Date()));
	
						child.setPrevNomDelo(sb.toString());
	
						this.dao.saveSysOkaJournal(child, "Преписка "+child.getIdentInfo()+" е продължена, при изваждането й от номенклатурно дело "+this.delo.getIdentInfo()+".");
					}
				});
				
				
	//			loadDeloDocsList();
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docu.successDeloOutMsg2") );
				this.deloDeloList.loadCountSql();
			} catch (BaseException e) {			
				JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
				LOGGER.error(e.getMessage(), e);
				
			}
		}
	}
	
	/******************* INDEXES na nomenklaturno delo **************/
	
	public void loadIndexes() {
		
		try {
			if (this.delo.getId()!=null) {
				deloIndexList=(ArrayList<Object[]>) dao.findNomDeloIndexData(this.delo.getId());
			}else {
				deloIndexList=(ArrayList<Object[]>) dao.findValidNomDeloIndexList(getUserData(UserData.class).getRegistratura(), this.delo.getDeloDate());
			}
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		
		String  cmdStr = "PF('indexPanel').show();";
		PrimeFaces.current().executeScript(cmdStr);
		
	}

	/**
	 * Справка за достъп
	 */
	public void actionFillDeloAccessList() {

		try {
			
			SelectMetadata sm = dao.createSelectDeloAccessList(delo.getId(), getSystemData());
			docAccessList = new LazyDataModelSQL2Array(sm, "A2, A1");
		} catch (DbErrorException e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		}

		String  cmdStr = "PF('mdlDeloAccessSpr').show();";
		PrimeFaces.current().executeScript(cmdStr);
	}
 
	
	protected DeloDAO getDao() {
		return dao;
	}



	public Delo getDelo() {
		return delo;
	}


	public void setDelo(Delo delo) {
		this.delo = delo;
	}


	public boolean isObshtodostap() {
		return obshtodostap;
	}


	public void setObshtodostap(boolean obshtodostap) {
		this.obshtodostap = obshtodostap;
	}


	public boolean isHasToms() {
		return hasToms;
	}


	public void setHasToms(boolean hasToms) {
		this.hasToms = hasToms;
	}


	public boolean isHasRazdeli() {
		return hasRazdeli;
	}


	public void setHasRazdeli(boolean hasRazdeli) {
		this.hasRazdeli = hasRazdeli;
	}


	public DeloDoc getCurrentDeloDoc() {
		return currentDeloDoc;
	}


	public void setCurrentDeloDoc(DeloDoc currentDeloDoc) {
		this.currentDeloDoc = currentDeloDoc;
	}


	protected DeloDocDAO getDaoDoc() {
		return daoDoc;
	}




	public LazyDataModelSQL2Array getDeloDocList() {
		return deloDocList;
	}


	public void setDeloDocList(LazyDataModelSQL2Array deloDocList) {
		this.deloDocList = deloDocList;
	}


	public DeloDelo getCurrentDeloDelo() {
		return currentDeloDelo;
	}


	public void setCurrentDeloDelo(DeloDelo currentDeloDelo) {
		this.currentDeloDelo = currentDeloDelo;
	}


	protected DeloDeloDAO getDaoDeloDelo() {
		return daoDeloDelo;
	}




	public LazyDataModelSQL2Array getDeloDeloList() {
		return deloDeloList;
	}


	public void setDeloDeloList(LazyDataModelSQL2Array deloDeloList) {
		this.deloDeloList = deloDeloList;
	}


	public Date getDecodeDate() {
		return decodeDate;
	}


	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}


	public Object[] getSelectedDeloP() {
		return selectedDeloP;
	}
	
	
	public void setSelectedDeloP(Object[] selectedDeloP) {
		this.selectedDeloP = selectedDeloP;
		if (selectedDeloP!=null && selectedDeloP[0]!=null) {
			this.delo.getDeloDelo().setDeloId(Integer.valueOf(selectedDeloP[0].toString()));
			String tmpstr = (String)selectedDeloP[1];
			if (this.delo.getDeloDelo().getDelo()==null) {
				this.delo.getDeloDelo().setDelo(new Delo());
			}
			this.delo.getDeloDelo().getDelo().setRnDelo(tmpstr);
			this.delo.getDeloDelo().getDelo().setStatus(SearchUtils.asInteger(selectedDeloP[3]));
			this.delo.getDeloDelo().getDelo().setDeloType(SearchUtils.asInteger(selectedDeloP[8]));
			this.delo.getDeloDelo().getDelo().setDeloDate((Date)selectedDeloP[2]);
			this.delo.getDeloDelo().getDelo().setDeloDate((Date)selectedDeloP[2]);
			this.delo.getDeloDelo().getDelo().setBrTom(SearchUtils.asInteger(selectedDeloP[11]));

			
			try {
				String msg = this.delo.getDeloDelo().getDelo().getRnDelo() +" / "+ DateUtils.printDate(this.delo.getDeloDelo().getDelo().getDeloDate()) +
						"; "+  getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DELO_STATUS, this.delo.getDeloDelo().getDelo().getStatus(), getUserData().getCurrentLang(), new Date());
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docu.addDeloMsg2", msg) );
				
				
			//MANTIS- belejka Rosi 13/05/2020 14:42 
//			actionSave();
				
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при разкодиране на статус на преписка ! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}
		}
	}
	
	
	public void actionHideModalDelo() {
		if(selectedDelo!=null && selectedDelo[0]!=null) {
			deloId = SearchUtils.asInteger(selectedDelo[0]);
			rnDelo = SearchUtils.asString(selectedDelo[1])+"/ " + DateUtils.printDate(SearchUtils.asDate(selectedDelo[2]));
			selectedDeloDate = SearchUtils.asDate(selectedDelo[2]);
			selectedDelo = null;
			try {
				Delo deloSel = dao.findById(SearchUtils.asInteger(deloId));
				brTomove = deloSel.getBrTom();
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}
	}
	
	public void actionSaveTransfer() {
		if(checkTransferData()) {
			return;
		}
		try {
			Integer s9 = ((SystemData)getSystemData()).getRegistraturaSetting(this.delo.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_9);

			DeloDeloDAO deloDeloDao = new DeloDeloDAO(getUserData());
			String dvijMsg = "";

			JPA.getUtil().begin();
			List<DeloDelo> list = deloDeloDao.transferToDelo(selectedDeloList, deloId, tomNomer, dateTransfer);
			if (s9 != null && s9.equals(SysConstants.CODE_ZNACHENIE_DA)) {
				for (DeloDelo deloDelo : list) {
					 String msg = deloDeloDao.checkCreateDvij(deloDelo, getSystemData());
					 if (msg != null) { // не за всяка може да има съобщение
						 dvijMsg = " " + msg;
					 }
				}
			}
			JPA.getUtil().commit();
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "transferDeloDoc.success") + dvijMsg);
			clearTransfData();
			loadDeloDeloList();
		} catch (Exception e) {
			JPA.getUtil().rollback();
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
	
	public void clearTransfData() {
		deloId = null;
		rnDelo = "";
		dateTransfer = new Date();
		selectedDeloDate = null;
		clearMoveData();
	}
	
	public void clearMoveData() {
		selectedDeloList = new ArrayList<Object[]>();
		tomNomer = null;
		
	}
	
	public void actionSaveTransferDocs() {
		if(checkTransferDocsData()) {
			return;
		}
		try {
			Integer s9 = ((SystemData)getSystemData()).getRegistraturaSetting(this.delo.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_9);
			String dvijMsg = "";
			
			JPA.getUtil().begin();
			List<DeloDoc> list = daoDoc.transferToDelo(delo, selectedDeloList, deloId, tomNomer, dateTransfer);			
			if (s9 != null && s9.equals(SysConstants.CODE_ZNACHENIE_DA)) {
				for (DeloDoc deloDoc : list) {
					 String msg = daoDoc.checkCreateDvij(deloDoc, getSystemData());
					 if (msg != null) { // не за всяка може да има съобщение
						 dvijMsg = " " + msg;
					 }
				}
			}
			JPA.getUtil().commit();
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "transferDeloDoc.success") + dvijMsg);
			clearTransfData();
			loadDeloDocsList();
		} catch (Exception e) {
			JPA.getUtil().rollback();
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
	
	public void actionSaveMoveDocs() {
		if(checkMoveDocsData()) {
			return;
		}
		try {
			
			for(Object[] item: selectedDeloList) {
				DeloDoc doc = daoDoc.findById(SearchUtils.asInteger(item[0]));
				doc.setTomNomer(tomNomer);
				JPA.getUtil().runInTransaction(() -> daoDoc.save(doc));
			}
				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "admStruct.successMove"));
			clearMoveData();
			loadDeloDocsList();
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		} catch (ObjectInUseException e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			LOGGER.error(e.getMessage(), e);
		} catch (BaseException e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	private boolean checkMoveDocsData() {
		boolean flagSave = false;
		
		
		
		if(tomNomer==null && visibleTom) {
			JSFUtils.addMessage(DELOFORM+":tomNomerMoveDocs", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "delo.tomNomer")));
			flagSave = true;
		}
		
		if(tomNomer!=null && visibleTom &&delo.getBrTom()!=null && tomNomer > delo.getBrTom()) {
			JSFUtils.addMessage(DELOFORM+":tomNomerMoveDocs",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "delo.brTomsMax", delo.getBrTom()) );
			flagSave = true;	
		}
		
		
		if(selectedDeloList==null || selectedDeloList.isEmpty()) {		
			JSFUtils.addMessage(null, FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloBean.selectDoc"));
			flagSave= true;
		}
		
		return flagSave;
		
	}
	
	private boolean checkTransferData() {
		boolean flagSave = false;
		
		if(dateTransfer==null ) {
			JSFUtils.addMessage(DELOFORM+":dateTransfer", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "delo.dateTransfer")));
			flagSave = true;
		}else if(dateTransfer.after(new Date())) {
			JSFUtils.addMessage(DELOFORM+":dateTransfer", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloBean.futureDateTransfer"));
			flagSave = true;		
		}else if(selectedDeloDate!=null && dateTransfer.before(selectedDeloDate)) {
			JSFUtils.addMessage(DELOFORM+":dateTransfer", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloBean.dateBeforeDeloDate"));
			flagSave = true;
			
		}
		
		if(tomNomer==null && visibleTom) {
			JSFUtils.addMessage(DELOFORM+":tomNomerTransf", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "delo.tomNomer")));
			flagSave = true;
		}
		
		
		if(tomNomer!=null && visibleTom && brTomove !=null && tomNomer > brTomove) {
			JSFUtils.addMessage(DELOFORM+":tomNomerTransf",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "delo.brTomsMax",brTomove) );
			flagSave = true;	
		}
		
		if(deloId==null ) {
			JSFUtils.addMessage(DELOFORM+":prepN", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "general.delo")));
			flagSave = true;
		}
		
		if(selectedDeloList==null || selectedDeloList.isEmpty()) {		
			JSFUtils.addMessage(null, FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloBean.selectDelo"));
			flagSave= true;
		}
		
		return flagSave;
		
	}
	
	private boolean checkTransferDocsData() {
		boolean flagSave = false;
		
		if(dateTransfer==null ) {
			JSFUtils.addMessage(DELOFORM+":dateTransferDocs", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "delo.dateTransfer")));
			flagSave = true;
		}else if(dateTransfer.after(new Date())) {
			JSFUtils.addMessage(DELOFORM+":dateTransferDocs", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloBean.futureDateTransfer"));
			flagSave = true;		
		}else if(selectedDeloDate!=null && dateTransfer.before(selectedDeloDate)) {
			JSFUtils.addMessage(DELOFORM+":dateTransferDocs", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloBean.dateBeforeDeloDate"));
			flagSave = true;
			
		}
		
		if(tomNomer==null && visibleTom) {
			JSFUtils.addMessage(DELOFORM+":tomNomerTransfDocs", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "delo.tomNomer")));
			flagSave = true;
		}
		
		if(tomNomer!=null && visibleTom && brTomove!=null && tomNomer > brTomove) {
			JSFUtils.addMessage(DELOFORM+":tomNomerTransfDocs",FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "delo.brTomsMax", brTomove) );
			flagSave = true;	
		}
		
		if(deloId==null ) {
			JSFUtils.addMessage(DELOFORM+":prepNDocs", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "general.delo")));
			flagSave = true;
		}
		
		if(selectedDeloList==null || selectedDeloList.isEmpty()) {		
			JSFUtils.addMessage(null, FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloBean.selectDoc"));
			flagSave= true;
		}
		
		return flagSave;
		
	}
	
	public void actionCheckTomAndSheets() {
		
		try {
		
			this.tomAndSheets = new ArrayList<>();
			
			JPA.getUtil().runWithClose(() -> this.tomAndSheets = this.dao.selectTomSheetsCount(this.delo.getId()));
		
		} catch (BaseException e) {				
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
	 */
	public void postProcessXLSDocs(Object delo) {
		
		String title = getMessageResourceString(LABELS, "delo.reportDocsInDeloTitle");		  
    	new CustomExpPreProcess().postProcessXLS(delo, title, dopInfoReport() , null, null);		
     
	}
	

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
	 */
	public void preProcessPDFDocs(Object delo)  {
		try{
			
			String title = getMessageResourceString(LABELS, "delo.reportDocsInDeloTitle");		
			new CustomExpPreProcess().preProcessPDF(delo, title,  dopInfoReport(), null, null);		
						
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(),e);			
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);			
		} 
	}
	
	/**
	 * подзаглавие за екпсорта - .............
	 */
	public Object[] dopInfoReport() {
		Object[] dopInf = null;
		dopInf = new Object[2];
		SimpleDateFormat sdf=new SimpleDateFormat("dd.MM.yyyy");
		if (this.delo.getRnDelo()!=null && this.delo.getDeloDate()!=null) {
			dopInf[0] = this.delo.getRnDelo()+"/"+sdf.format(this.delo.getDeloDate());
		}
	
		return dopInf;
	}
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
	 */
	public void postProcessXLSDela(Object delo) {
		
		String title = getMessageResourceString(LABELS, "delo.deloVlojvPrep");		  
		new CustomExpPreProcess().postProcessXLS(delo, title, dopInfoReport() , null, null);		
		
	}
	
	
	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
	 */
	public void preProcessPDFDela(Object delo)  {
		try{
			
			String title = getMessageResourceString(LABELS, "delo.deloVlojvPrep");		
			new CustomExpPreProcess().preProcessPDF(delo, title,  dopInfoReport(), null, null);		
			
		} catch (UnsupportedEncodingException e) {
			LOGGER.error(e.getMessage(),e);			
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);			
		} 
	}
	
	/**
	 * за експорт в pdf 
	 * @return
	 */
	public PDFOptions pdfOptions() {
		PDFOptions pdfOpt = new CustomExpPreProcess().pdfOptions(null, null, null);
        return pdfOpt;
	}
	
	
	 
	

	public List<Object[]> getSelectedDeloMulty() {
		return selectedDeloMulty;
	}


	public void setSelectedDeloMulty(ArrayList<Object[]> selectedDeloMulty) {
		this.selectedDeloMulty = selectedDeloMulty;
		actionSaveDeloDeloMulty();
	}


	public List<Object[]> getSelectedDocsMulty() {
		return selectedDocsMulty;
	}


	public void setSelectedDocsMulty(ArrayList<Object[]> selectedDocsMulty) {
		this.selectedDocsMulty = selectedDocsMulty;
		actionSaveMyltyDeloDoc();
	}


	public String getDeloName2() {
		return deloName2;
	}


	public void setDeloName2(String deloName2) {
		this.deloName2 = deloName2;
	}


	public String getMess() {
		return mess;
	}


	public void setMess(String mess) {
		this.mess = mess;
	}


	public UserData getUd() {
		return ud;
	}


	public void setUd(UserData ud) {
		this.ud = ud;
	}


	public Integer getOriginalDeloType() {
		return originalDeloType;
	}


	public void setOriginalDeloType(Integer originalDeloType) {
		this.originalDeloType = originalDeloType;
	}


	public boolean isEndTask() {
		return endTask;
	}


	public void setEndTask(boolean endTask) {
		this.endTask = endTask;
	}


	public boolean isAccessFinishTask() {
		return accessFinishTask;
	}


	public void setAccessFinishTask(boolean accessFinishTask) {
		this.accessFinishTask = accessFinishTask;
	}

	public String getProtocolArchive() {
		return protocolArchive;
	}

	public void setProtocolArchive(String protocolArchive) {
		this.protocolArchive = protocolArchive;
	}

	public String getRnFullDelo() {
		return rnFullDelo;
	}

	public void setRnFullDelo(String rnFullDelo) {
		this.rnFullDelo = rnFullDelo;
	}

	public List<Object[]> getDeloStorageList() {
		return deloStorageList;
	}

	public void setDeloStorageList(List<Object[]> deloStorageList) {
		this.deloStorageList = deloStorageList;
	}

	public List<Integer> getZvenaCodesList() {
		return zvenaCodesList;
	}

	public void setZvenaCodesList(List<Integer> zvenaCodesList) {
		this.zvenaCodesList = zvenaCodesList;
	}

	public ArrayList<Object[]> getDeloIndexList() {
		return deloIndexList;
	}

	public void setDeloIndexList(ArrayList<Object[]> deloIndexList) {
		this.deloIndexList = deloIndexList;
	}
	
	/**
	 * Разглеждане на delo
	 * При отваряне в нов таб, като title на таба, да излезе номера и дата на документа
	 * @return
	 */
	public String getRnFullDeloView() {
		if (delo != null && delo.getId()!=null) {
			return this.delo.getRnDelo() + "/" + DateUtils.printDate(this.delo.getDeloDate());
		} else {
			return "Дело/преписка";
		}
		
	}

	public LazyDataModelSQL2Array getDocAccessList() {
		return docAccessList;
	}

	public void setDocAccessList(LazyDataModelSQL2Array docAccessList) {
		this.docAccessList = docAccessList;
	}

	public List <Object[]> getSelectedDeloList() {
		return selectedDeloList;
	}

	public void setSelectedDeloList(List <Object[]> selectedDeloList) {
		this.selectedDeloList = selectedDeloList;
	}

	public Date getDateTransfer() {
		return dateTransfer;
	}

	public void setDateTransfer(Date dateTransfer) {
		this.dateTransfer = dateTransfer;
	}

	public Integer getDeloId() {
		return deloId;
	}

	public void setDeloId(Integer deloId) {
		this.deloId = deloId;
	}

	public Integer getTomNomer() {
		return tomNomer;
	}

	public void setTomNomer(Integer tomNomer) {
		this.tomNomer = tomNomer;
	}

	public boolean isVisibleTransfer() {
		return visibleTransfer;
	}

	public void setVisibleTransfer(boolean visibleTransfer) {
		this.visibleTransfer = visibleTransfer;
	}

	public String getRnDelo() {
		return rnDelo;
	}

	public void setRnDelo(String rnDelo) {
		this.rnDelo = rnDelo;
	}

	public Object[] getSelectedDelo() {
		return selectedDelo;
	}

	public void setSelectedDelo(Object[] selectedDelo) {
		
		this.selectedDelo = selectedDelo;
	}

	public boolean isVisibleTransferDocs() {
		return visibleTransferDocs;
	}

	public void setVisibleTransferDocs(boolean visibleTransferDocs) {
		this.visibleTransferDocs = visibleTransferDocs;
	}

	public boolean isVisibleTom() {
		return visibleTom;
	}

	public void setVisibleTom(boolean visibleTom) {
		this.visibleTom = visibleTom;
	}

	public List<Integer[]> getTomAndSheets() {
		return tomAndSheets;
	}

	public void setTomAndSheets(List<Integer[]> tomAndSheets) {
		this.tomAndSheets = tomAndSheets;
	}

	public boolean isVisibleMoveDocs() {
		return visibleMoveDocs;
	}

	public void setVisibleMoveDocs(boolean visibleMoveDocs) {
		this.visibleMoveDocs = visibleMoveDocs;
	}
	
	/**
	 * За журнала! 
	 * @return
	 */
	public String getRnFullDeloAudit() {
		//Не изпозлвам rnFullDoc, защото може да се обърка отварянето на табовете....
		String rnAudit = null;
		if(delo != null && delo.getId() != null) {
			   
			try {
				rnAudit = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DELO_TYPE,delo.getDeloType(), getCurrentLang(), new Date())+": "+this.delo.getRnDelo() + "/" + DateUtils.printDate(this.delo.getDeloDate());
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}
		return rnAudit;
	}

	public boolean isHideAll() {
		return hideAll;
	}

	public void setHideAll(boolean hideAll) {
		this.hideAll = hideAll;
	}

	public Integer getOriginalStatus() {
		return originalStatus;
	}

	public void setOriginalStatus(Integer originalStatus) {
		this.originalStatus = originalStatus;
	}


	public Integer getAccView() {
		return accView;
	}

	public void setAccView(Integer accView) {
		this.accView = accView;
	}


}
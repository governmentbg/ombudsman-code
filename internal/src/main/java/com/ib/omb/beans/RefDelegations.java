package com.ib.omb.beans;

import static com.ib.omb.system.OmbConstants.CODE_CLASSIF_DELEGATES;
import static com.ib.system.SysConstants.CODE_CLASSIF_USERS;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.ReferentDelegationDAO;
import com.ib.omb.db.dto.ReferentDelegation;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;

/**
 * Въвеждане / актуализация на заместване и упълномощаване
 * 
 */
@Named
@ViewScoped
public class RefDelegations extends IndexUIbean  implements Serializable {


	
	private static final long serialVersionUID = -4638838474408734003L;
	private static final Logger LOGGER = LoggerFactory.getLogger(RefDelegations.class);
	
	//private List<ReferentDelegation> delegList = new ArrayList<ReferentDelegation>();
	private LazyDataModelSQL2Array delegListRepl; // за диалога "Кого замествам"
	private LazyDataModelSQL2Array delegList;
	private transient ReferentDelegationDAO refDao;
	private ReferentDelegation delegRule = null;
	private String liceNames = "";
	
	private boolean replaced = true; //дали е избрано заместване или упълномощаване
	private boolean showAutoriz = false; // дали потребителят има право да упълномощава 
	private boolean delegRight = false; // дали потребителят има право да делегира чужди права
	private boolean old = false; // дали да се показват неактивните замествания/упълномощавания
	private boolean oldDialog = false;
	private String radioActive ="1";
	private String radioActiveDialog ="1";
	
	private transient Map<Integer,Object> specifics;
	//private Map<Integer,Object> specificsAdmStr = new HashMap<Integer,Object>();
	private Integer registratura;
	
	private Date dataOt;
	private Date dataDo;
	private Date dataOtRepl;
	private Date dataDoRepl;
	
	private static final String DATE_TO = "delegForm:dateTo"; 
	private static final String UI_BEAN_MESSAGES = "ui_beanMessages";
	private static final String MSG_PLEASE_INSERT = "general.pleaseInsert";
	private static final String LABELS = "labels";
	private static final String BEAN_MESSAGES = "beanMessages";
	public  static final  String DATE_END_BEFORE_BEG   = "refDeleg.dateEndBeforeDateBeg";
	
	@PostConstruct
	void initData() {
				
		LOGGER.debug("PostConstruct!!!");	
		
		try {
			
			registratura = getUserData(UserData.class).getRegistratura();
			specifics = Collections.singletonMap(OmbClassifAdapter.USERS_INDEX_REGISTRATURA,registratura);
			//specificsAdmStr.put( OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA, registratura);
			//specificsAdmStr.put( OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE , OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL);
			refDao = new ReferentDelegationDAO(getUserData());
			liceNames = getUserData().getLiceNames();
			showAutoriz  = getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_AUTHORIZE);
			delegRight = getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA,OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_DELEGATE_FOREIGN);
			
			SelectMetadata smd;			
			if(delegRight) { // когато има право да делегира чужди права се извеждат всички от регистратурата
				smd = this.refDao.createSelectMetadata(registratura, null,null, old,null,null);
				//JPA.getUtil().runWithClose(() -> this.delegList = this.refDao.findDelegations( registratura, null,null, null , old));
			}else {
				smd = this.refDao.createSelectMetadata(registratura, getUserData().getUserId(),null, old,null,null);
				//JPA.getUtil().runWithClose(() -> this.delegList = this.refDao.findDelegations( registratura, getUserData().getUserId(),null, null , old));
			}	
			String defaultSortColumn = "A0";	
			this.delegList = new LazyDataModelSQL2Array(smd, defaultSortColumn);
		} catch (Exception e) {
			LOGGER.error("Грешка при зареждане данните за заместване и упълномощаване! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
			
	}
	
	/**
	 * Инициализация на ново заместване
	 * 
	 */
	public void actionNewSubst() {
		replaced = true;
		delegRule = new ReferentDelegation();
		delegRule.setRegistraturaId(getUserData(UserData.class).getRegistratura());
		if(!delegRight) {
			delegRule.setCodeRef(getUserData().getUserId());
		}
		delegRule.setDelegationType(OmbConstants.CODE_ZNACHENIE_DELEGATES_ZAMESTVANE );
	}
	
	/**
	 * Инициализация на ново упълномощаване
	 * 
	 */
	public void actionNewAuthorization() {
		replaced =false;
		delegRule = new ReferentDelegation();
		delegRule.setRegistraturaId(getUserData(UserData.class).getRegistratura());
		if(!delegRight) {
			delegRule.setCodeRef(getUserData().getUserId());
		}
		delegRule.setDelegationType(OmbConstants.CODE_ZNACHENIE_DELEGATES_UPYLNOMOSHTAVANE );
//		Calendar cal = Calendar.getInstance();
//		cal.setTime(new Date());
//		cal.set(Calendar.MONTH, Calendar.DECEMBER);
//		cal.set(Calendar.DAY_OF_MONTH, 31);
//		delegRule.setDateTo(cal.getTime());
	}
	
	/**
	 * Избор от активни и стари замествания/ упълномощавания
	 * 
	 */
	public void actionChangeActive() {
		if("1".equals(radioActive)) {
			old=false;
		}else if("2".equals(radioActive)) {
			old=true;
		}
		delegRule = null;
		actionSearch();
		dataOt = null;
		dataDo = null;
	}
	
	
	public void actionChangeActiveDialog() {
		 
		if("1".equals(radioActiveDialog)) {
			oldDialog=false;
		}else if("2".equals(radioActiveDialog)) {
			oldDialog=true;
		}
		actionSearchDialog();
		dataOtRepl=null;
		dataDoRepl = null;
	}

	/**
	 * Търсене на заместване и упълномощаване
	 * 
	 */
	public void actionSearch() {
		
		try {
			SelectMetadata smd;
			
			if(delegRight) { // когато има право да делегира чужди права се извеждат всички от регистратурата
				smd = this.refDao.createSelectMetadata(registratura, null,null, old,dataOt,dataDo);
				//JPA.getUtil().runWithClose(() -> this.delegList = this.refDao.findDelegations( registratura, null,null, null , old));
			}else {
				smd = this.refDao.createSelectMetadata(registratura, getUserData().getUserId(),null, old,dataOt,dataDo);
				//JPA.getUtil().runWithClose(() -> this.delegList = this.refDao.findDelegations( registratura, getUserData().getUserId(),null, null , old));
			}	
			String defaultSortColumn = "A0";	
			this.delegList = new LazyDataModelSQL2Array(smd, defaultSortColumn);
		}catch (Exception e) {
			LOGGER.error("Грешка при зареждане данните за заместване и упълномощаване! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}	
	
	public void actionSearchButt() {
		if(dataOt==null) {
			JSFUtils.addMessage("delegForm:dateOtSearch", FacesMessage.SEVERITY_ERROR,getMessageResourceString(BEAN_MESSAGES, "refDeleg.insertDateOt"));
		    return;
		}
		
		if(dataDo!=null && dataDo.before(dataOt)) {
			JSFUtils.addMessage("delegForm:dateOtSearch", FacesMessage.SEVERITY_ERROR,getMessageResourceString(BEAN_MESSAGES, DATE_END_BEFORE_BEG));
			return;
		}
		
		actionSearch();
	}
	
	public void actionSearchDialog() {
		
		SelectMetadata smd = this.refDao.createSelectMetadata(registratura, null,getUserData().getUserId(), oldDialog,dataOtRepl,getDataDoRepl());
		String defaultSortColumn = "A0";	
		this.delegListRepl = new LazyDataModelSQL2Array(smd, defaultSortColumn);
		//JPA.getUtil().runWithClose(() -> this.setDelegListRepl(this.refDao.findDelegations( registratura, null,getUserData().getUserId(), null , oldDial)));
	}	
	
	public void actionSearchButtDial() {
		if(dataOtRepl==null) {
			JSFUtils.addMessage("delegForm:dateOtSearchDial", FacesMessage.SEVERITY_ERROR,getMessageResourceString(BEAN_MESSAGES, "refDeleg.insertDateOt"));
		    return;
		}
		if(dataDoRepl!=null && dataDoRepl.before(dataOtRepl)) {
			JSFUtils.addMessage("delegForm:dateOtSearchDial", FacesMessage.SEVERITY_ERROR,getMessageResourceString(BEAN_MESSAGES, DATE_END_BEFORE_BEG));
			return;
		}
		actionSearchDialog();
	}
	
	public boolean checkData() {
		
		boolean save = false;

		if(delegRight && delegRule.getCodeRef()==null) {
			JSFUtils.addMessage("delegForm:userChoose:аutoCompl_input", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_BEAN_MESSAGES, MSG_PLEASE_INSERT, getMessageResourceString(LABELS, "refDeleg.repEmployee")));
			save = true;
		}
		
		if(delegRule.getUserId()==null) {
			JSFUtils.addMessage("delegForm:zamestnik:аutoCompl_input", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_BEAN_MESSAGES, MSG_PLEASE_INSERT, getMessageResourceString(LABELS, "refDeleg.zamestvashtSluj")));
			save = true;
		}
		
		if(delegRule.getCodeRef()!=null && delegRule.getUserId()!=null && delegRule.getUserId().equals(delegRule.getCodeRef())) {
			JSFUtils.addMessage("delegForm:userChoose:аutoCompl_input", FacesMessage.SEVERITY_ERROR,getMessageResourceString(BEAN_MESSAGES, "refDeleg.userRepl"));
			save = true;
		}	
		
		Date startDate= DateUtils.startDate(new Date());
		if(delegRule.getDateOt()==null) {
			JSFUtils.addMessage("delegForm:dateOt", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_BEAN_MESSAGES, MSG_PLEASE_INSERT, getMessageResourceString(LABELS, "refDeleg.dateFrom")));
			save = true;			
		}else if (delegRule.getId()==null && delegRule.getDateOt().before(startDate)){ 
			JSFUtils.addMessage("delegForm:dateOt", FacesMessage.SEVERITY_ERROR,getMessageResourceString(BEAN_MESSAGES, "refDeleg.dateBeforeToday"));
			save = true;
		}
		if(delegRule.getDateTo()==null && delegRule.getDelegationType().intValue() == OmbConstants.CODE_ZNACHENIE_DELEGATES_ZAMESTVANE ) {
			JSFUtils.addMessage(DATE_TO, FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_BEAN_MESSAGES, MSG_PLEASE_INSERT, getMessageResourceString(LABELS, "refDeleg.dateTo")));
			save = true;
		}
        if(delegRule.getDateTo()!=null && delegRule.getDateTo().before(startDate)) {
			JSFUtils.addMessage(DATE_TO, FacesMessage.SEVERITY_ERROR,getMessageResourceString(BEAN_MESSAGES, "refDeleg.endDateBeforeToday"));
			save = true;
		}
		
		if(delegRule.getDateOt()!=null && delegRule.getDateTo()!=null && delegRule.getDateTo().before(delegRule.getDateOt())) {
			JSFUtils.addMessage(DATE_TO, FacesMessage.SEVERITY_ERROR,getMessageResourceString(BEAN_MESSAGES, DATE_END_BEFORE_BEG));
			save = true;
		}
				
		return save;		
	}
	
	/**
	 * Запис на заместване и упълномощаване
	 * 
	 */
	public void actionSave() {
		
		if(checkData()) {
			return;
		}
			 boolean isInUse = false;			
		try {
				
			JPA.getUtil().runInTransaction(() -> this.delegRule = this.refDao.save(this.delegRule, getSystemData()));
		
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_EMPL_REPLACES, false, false);
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_BEAN_MESSAGES, "general.succesSaveMsg"));
				
		} catch (ObjectInUseException  e) {		
			isInUse = true;
			LOGGER.error("ObjectInUseException-> {}", e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} catch (BaseException e) {	
			isInUse = true;
			LOGGER.error("Грешка при запис на делегирано право! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		if(!isInUse) {
			actionSearch();	
		}
	}
	
	/**
	 * Редакция на заместване/ упълномощаване
	 * 
	 */
	public void actionEdit(Integer id) {
		try {
			JPA.getUtil().runWithClose(() -> this.delegRule = this.refDao.findById(id));
		} catch (BaseException e) {
			LOGGER.error("Грешка при търсене на делегирано право! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	
		if(delegRule!=null) {
			if(delegRule.getDelegationType().intValue() == OmbConstants.CODE_ZNACHENIE_DELEGATES_ZAMESTVANE) {
				replaced=true;
			}else if(delegRule.getDelegationType().intValue() == OmbConstants.CODE_ZNACHENIE_DELEGATES_UPYLNOMOSHTAVANE) {
				replaced = false; 
			}
		}	
	}
	
	/**
	 * Изтриване на заместване и упълномощаване
	 * 
	 */
	public void actionDelete(Integer id) {
		
//		Date startDate= DateUtils.startDate(new Date());
//		 if ( row.getDateOt()!=null && (startDate.equals(row.getDateOt()) || startDate.after(row.getDateOt()))){ 
//			JSFUtils.addMessage("delegForm:dateOt", FacesMessage.SEVERITY_ERROR,getMessageResourceString("beanMessages", "refDeleg.activePeriod"));
//			return;
//		}
		boolean isInUse = false;
		try {	
			
			JPA.getUtil().runInTransaction(() -> { 
				ReferentDelegation tmp = this.refDao.findById(id);
				
				StringBuilder ident = new StringBuilder();
				ident.append(getSystemData().decodeItem(CODE_CLASSIF_USERS, tmp.getCodeRef(), getCurrentLang(), tmp.getDateOt()));
				ident.append(" " + getSystemData().decodeItem(CODE_CLASSIF_DELEGATES, tmp.getDelegationType(), getCurrentLang(), tmp.getDateOt()) + " ");
				ident.append(getSystemData().decodeItem(CODE_CLASSIF_USERS, tmp.getUserId(), getCurrentLang(), tmp.getDateOt()));
				tmp.setIdentInfo(ident.toString());
				
				this.refDao.delete(tmp, getSystemData());
			});
		
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_EMPL_REPLACES, false, false);
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_BEAN_MESSAGES, "general.successDeleteMsg"));
						    
		} catch (ObjectInUseException e) {
			isInUse = true;
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());		
		
		} catch (BaseException e) {	
			isInUse = true;
			LOGGER.error("Грешка при изтриване на делегирано право! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		if(!isInUse) {
			delegRule= null;
			actionSearch();
		}
	}
	
	/**
	 * Зачистване на критериите за търсене 
	 * 
	 */
	public void actionClear() {
		dataOt = null;
		dataDo = null;
		actionSearch();
	}
	
	/**
	 * Зачистване на критериите за търсене в модалния прозорец
	 * 
	 */
	public void actionClearDialog() {
		dataOtRepl = null;
		dataDoRepl = null;
		actionSearchDialog();
	}
	
	public ReferentDelegation getDelegRule() {
		return delegRule;
	}

	public void setDelegRule(ReferentDelegation delegRule) {
		this.delegRule = delegRule;
	}
	
    public Date getToday() {
    	return new Date();
    }

	public boolean isReplaced() {
		return replaced;
	}

	public void setReplaced(boolean replaced) {
		this.replaced = replaced;
	}

	public String getLiceNames() {
		return liceNames;
	}

	public void setLiceNames(String liceNames) {
		this.liceNames = liceNames;
	}

	public boolean isShowAutoriz() {
		return showAutoriz;
	}

	public void setShowAutoriz(boolean showAutoriz) {
		this.showAutoriz = showAutoriz;
	}

	public boolean isDelegRight() {
		return delegRight;
	}

	public void setDelegRight(boolean delegRight) {
		this.delegRight = delegRight;
	}

	public Map<Integer,Object> getSpecifics() {
		return specifics;
	}

	public void setSpecifics(Map<Integer,Object> specifics) {
		this.specifics = specifics;
	}

	public boolean isOld() {
		return old;
	}

	public void setOld(boolean old) {
		this.old = old;
	}

	public String getRadioActive() {
		return radioActive;
	}

	public void setRadioActive(String radioActive) {
		this.radioActive = radioActive;
	}

	public String getRadioActiveDialog() {
		return radioActiveDialog;
	}

	public void setRadioActiveDialog(String radioActiveDialog) {
		this.radioActiveDialog = radioActiveDialog;
	}

	public LazyDataModelSQL2Array getDelegList() {
		return delegList;
	}

	public LazyDataModelSQL2Array setDelegList(LazyDataModelSQL2Array delegList) {
		this.delegList = delegList;
		return delegList;
	}

	public LazyDataModelSQL2Array getDelegListRepl() {
		return delegListRepl;
	}

	public LazyDataModelSQL2Array setDelegListRepl(LazyDataModelSQL2Array delegListRepl) {
		this.delegListRepl = delegListRepl;
		return delegListRepl;
	}

	public Date getDataOt() {	
		return dataOt ;
	}

	public void setDataOt(Date dataOt) {
		this.dataOt = dataOt != null ? new Date(dataOt.getTime()) : null;
	}

	public Date getDataDo() {
		return dataDo;
	}

	public void setDataDo(Date dataDo) {
		this.dataDo = dataDo != null ? new Date(dataDo.getTime()) : null;
	}

	public boolean isOldDialog() {
		return oldDialog;
	}

	public void setOldDialog(boolean oldDialog) {
		this.oldDialog = oldDialog;
	}

	public Date getDataOtRepl() {
		return dataOtRepl;
	}

	public void setDataOtRepl(Date dataOtRepl) {
		this.dataOtRepl = dataOtRepl != null ? new Date(dataOtRepl.getTime()) : null;
	}

	public Date getDataDoRepl() {
		return dataDoRepl;
	}

	public void setDataDoRepl(Date dataDoRepl) {
		this.dataDoRepl = dataDoRepl != null ? new Date(dataDoRepl.getTime()) : null;
	}
}
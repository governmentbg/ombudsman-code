package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.search.DeloSearch;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.omb.utils.DocDostUtils;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.useractivity.UserActivityData;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.SysClassifUtils;

/**
 * Отнемане на достъп
 */
@Named
@ViewScoped
public class TakeAwayAccess  extends IndexUIbean   {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 3966557716503313584L;

	private static final Logger LOGGER = LoggerFactory.getLogger(TakeAwayAccess.class);
	
	private static final String SCROLLTOERRORS = "scrollToErrors()";

	private Date decodeDate = new Date();
	private transient DocDostUtils dostUtils;
	private Integer codeTakeAwayAccess;
	private String userNameForTakeAway;
		
	private Integer userRegistratura;
	private boolean userLeave;
	
	//за дела/преписки
	private boolean renderedDelo = false;
	private DeloSearch deloSearch;
	private Serializable[] checkUserAccessDela;
	private Integer period = null;
	private Integer periodStat = null;	
	private LazyDataModelSQL2Array deloList;
	private String radioAccessDela ="1";
	
	private List<Object[]> deloSelectedAllM;
	private List<Object[]> deloSelectedTmp;	
	
	//за документи	
	private boolean renderedDoc = false; 
	private DocSearch searchDoc;
	private Serializable[] checkUserAccessDocs;
	private Integer periodR;
	private List<SystemClassif> docsVidClassif;
	private List<SystemClassif> docsRegistriClassif;
	private Map<Integer, Object> specificsRegisterSource; //за текущата регистратура
	private LazyDataModelSQL2Array docsList;      // списък документи
	private String radioAccessDocs ="1";
	
	private List<Object[]> docSelectedAllM;
	private List<Object[]> docSelectedTmp;
	
	/** */
	@PostConstruct
	public void initData() {

		this.userRegistratura = getUserData(UserData.class).getRegistratura();
		this.dostUtils = new DocDostUtils();
		this.deloSearch = new DeloSearch(this.userRegistratura);
		this.searchDoc = new DocSearch(this.userRegistratura);
		
		List<Integer> notInCodes = new ArrayList<>();
		notInCodes.add(this.userRegistratura);// без текущата
		
		this.deloSelectedAllM = new ArrayList<>();
		this.deloSelectedTmp = new ArrayList<>();
		
		this.docsVidClassif = new ArrayList<>();
		this.docsRegistriClassif = new ArrayList<>();
		this.docSelectedAllM = new ArrayList<>();
		this.docSelectedTmp = new ArrayList<>();
		
		setSpecificsRegisterSource(Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA, Optional.of(this.userRegistratura)));	
	}
	
	/**
	 * Избор на служител
	 */
	public void actionSelectSlujForTakeAway() {
		
		try {
			
			if (this.codeTakeAwayAccess != null) {
				
				this.renderedDelo = false;
				actionClearDelo();
				this.renderedDoc = false;
				actionClearDoc();
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.codeTakeAwayAccess, getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL))) {
			
					this.userLeave = getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.codeTakeAwayAccess, this.decodeDate);
					boolean existUser = getSystemData().matchClassifItems(SysConstants.CODE_CLASSIF_USERS, this.codeTakeAwayAccess, this.decodeDate);
					
					if (existUser) {
						this.userNameForTakeAway = getSystemData().decodeItem(SysConstants.CODE_CLASSIF_USERS, this.codeTakeAwayAccess, getCurrentLang(), this.decodeDate);						
					}
					
					// да се сетне checkUserAccess след избора на служителя:
					// [0]-codeRef; [1]-zveno; [2]=null- има достъп, [2]!=null- отнет достъп; [3]-ако е е на ръководна длъжност списък със звената, до които има достъп "2,4,9"
					this.checkUserAccessDela = new Serializable[4];
					this.checkUserAccessDocs = new Serializable[4];
					
					this.checkUserAccessDela[0] = SearchUtils.asInteger(this.codeTakeAwayAccess);
					this.checkUserAccessDocs[0] = SearchUtils.asInteger(this.codeTakeAwayAccess);
					
					SystemClassif sc = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.codeTakeAwayAccess, getCurrentLang(), decodeDate, true);
					this.checkUserAccessDela[1] = SearchUtils.asInteger(sc.getCodeParent());
					this.checkUserAccessDocs[1] = SearchUtils.asInteger(sc.getCodeParent());					
					
					Integer position = SearchUtils.asInteger(sc.getSpecifics()[OmbClassifAdapter.ADM_STRUCT_INDEX_POSITION]);
					if (position != null) {
						
						boolean boss = getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_BOSS_POSITIONS, position, this.decodeDate);
						
						if (boss) {
							
							// трябват ми само звената и за това пускам през специфика да се отрежат
							Map<Integer, Object> specifics = Collections.singletonMap(OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE, OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO);

							List<SystemClassif> items = getSystemData().queryClassification(OmbConstants.CODE_CLASSIF_ADMIN_STR, null, this.decodeDate, getCurrentLang(), specifics);
							List<SystemClassif> zvena = SysClassifUtils.getChildren(items, sc.getCodeParent(), null);

							StringBuilder accessZvenoList = new StringBuilder();
							accessZvenoList.append(sc.getCodeParent());

							for (SystemClassif zveno : zvena) {
								accessZvenoList.append("," + zveno.getCode());
							}
							
							this.checkUserAccessDela[3] = accessZvenoList;
							this.checkUserAccessDocs[3] = accessZvenoList;
						}
					}
				
				} else {
					
					this.codeTakeAwayAccess = null;
					this.checkUserAccessDela = null;
					this.checkUserAccessDocs = null;
					JSFUtils.addMessage("formTakeAwayAccess" + ":takeAwayAccess:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "transferAccess.choiceSluj"));
				}
				
			} else {
				this.userNameForTakeAway = "";
				this.checkUserAccessDela = null;
				this.checkUserAccessDocs = null;
			}
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на името на потребителя! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
		
	}


	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClearDelo() {
		
		this.deloList = null;
		this.period = null;
		this.periodStat = null;
		this.deloSearch = new DeloSearch(this.userRegistratura);
		this.radioAccessDela ="1";
		this.deloSelectedTmp.clear();
		this.deloSelectedAllM.clear();
	}
	
	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClearDoc() {
		
		this.docsList = null;
		this.periodR = null;	
		this.searchDoc = new DocSearch(userRegistratura);	
		this.radioAccessDocs = "1";
		this.docSelectedTmp.clear();
		this.docSelectedAllM.clear();		
	}
	
	public void actionChangeRadioAccessDela() {
		
		this.deloList = null;
		this.deloSelectedTmp.clear();
		this.deloSelectedAllM.clear();
	}
	
	public void actionChangeRadioAccessDocs() {
		
		this.docsList = null;
		this.docSelectedTmp.clear();
		this.docSelectedAllM.clear();		
	}
	
	public void selectDeloSearch() {
		
		this.renderedDelo = true;
		this.renderedDoc = false;
		actionClearDoc();
	}
	
	public void selectDocSearch() {
		
		this.renderedDelo = false;
		this.renderedDoc = true;
		actionClearDelo();
	}
	
	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	public void changePeriod () {
		
    	if (this.period != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.period);
			this.deloSearch.setDeloDateFrom(di[0]);
			this.deloSearch.setDeloDateTo(di[1]);		
    	} else {
    		this.deloSearch.setDeloDateFrom(null);
    		this.deloSearch.setDeloDateTo(null);
		}
    }
	
	public void changeDate() { 
		this.setPeriod(null);
	}
	
	public void changeDateR() { 
		this.setPeriodR(null);
	}
	
	/** Метод за смяна на датите при избор на период за търсене na status.
	 * 
	 * 
	 */
	public void changePeriodStat () {
		
    	if (this.periodStat != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodStat);
			this.deloSearch.setStatusDateFrom(di[0]);
			this.deloSearch.setStatusDateTo(di[1]);		
    	} else {
    		this.deloSearch.setStatusDateFrom(null);
    		this.deloSearch.setStatusDateTo(null);
		}
    }
	
	/** Метод за смяна на датите при избор на период за търсене на документи.
	 * 
	 * 
	 */
	public void changePeriodR () {
		
    	if (this.periodR != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodR);
			this.searchDoc.setDocDateFrom(di[0]);
			this.searchDoc.setDocDateTo(di[1]);		
    	} else {
    		this.searchDoc.setDocDateFrom(null);
    		this.searchDoc.setDocDateTo(null);
		}
    }
	
	public void changeDateStat() { 
		this.setPeriodStat(null);
	}	
	
	/**
	 * Проверка дали служител е логнат в системата 
	 * @param codeSluj
	 * @param fromBtn
	 */
	public boolean actionCheckLogInSystem(Integer codeSluj, Integer fromBtn) {
		
		boolean logIn = false;
		
		try {
			
			boolean existUser = getSystemData().matchClassifItems(SysConstants.CODE_CLASSIF_USERS, codeSluj, this.decodeDate);
			
			if (existUser) {
				
				String userNameSluj = getSystemData().decodeItem(SysConstants.CODE_CLASSIF_USERS, codeSluj, getCurrentLang(), this.decodeDate);	
				
				UserActivityData uad = getSystemData().getActiveUsers().check("fakeIP", codeSluj);
				
				if (uad != null ) { // логнат е в системата и ще излезе съобщение
					logIn = true;
					if (fromBtn == 1) {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "takeAwayAccess.notDelAccessUserLogIn", userNameSluj));
					
					} else if (fromBtn == 2) {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "takeAwayAccess.notDenyDelaUserLogIn", userNameSluj));
					
					} else if (fromBtn == 3) {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "takeAwayAccess.notRemoveDenyDelaUserLogIn", userNameSluj));
					
					} else if (fromBtn == 4) {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "takeAwayAccess.notDenyDocsUserLogIn", userNameSluj));
					
					} else if (fromBtn == 5) {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "takeAwayAccess.notRemoveDenyDocsUserLogIn", userNameSluj));
					}					
				}				
			}			
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при проверка дали служителя е логнат в системата! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
		return logIn; 		
	}
	
	/**
	 * Търсене на дело
	 */
	
	public void actionSearchDelo(){
		 
		this.deloSearch.setUseDost(false);
		
		if("1".equals(this.radioAccessDela)) {
			this.checkUserAccessDela[2] = null;
		} else if("2".equals(this.radioAccessDela)) {
			this.checkUserAccessDela[2] = 0;
		}
		
		this.deloSearch.setCheckUserAccess(this.checkUserAccessDela);		 
		
		this.deloSearch.buildQueryDeloList(getUserData(), 1);
		this.deloList = new LazyDataModelSQL2Array(this.deloSearch, "a1 desc"); 
	}	
	
	/**
	 * Търсене на документ
	 */
	public void actionSearchDoc(){
		 
		this.searchDoc.setUseDost(false);
		
		if("1".equals(this.radioAccessDocs)) {
			this.checkUserAccessDocs[2] = null;
		} else if("2".equals(this.radioAccessDocs)) {
			this.checkUserAccessDocs[2] = 0;
		}
		
		this.searchDoc.setCheckUserAccess(this.checkUserAccessDocs);
		
		this.searchDoc.buildQueryDocList(getUserData(), true);
		this.docsList = new LazyDataModelSQL2Array(this.searchDoc, "a4 desc"); 
	}	
	
	/**
	 * Премахване на достъпа на избрания служител
	 */
	public void actionDeleteAccess() {
		
		try {
			
			if (!actionCheckLogInSystem(codeTakeAwayAccess, 1)) {
				
				JPA.getUtil().runInTransaction(() ->  this.dostUtils.eraseAccess(codeTakeAwayAccess));
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "takeAwayAccess.eraseAccess"));				
			}
			
			PrimeFaces.current().executeScript(SCROLLTOERRORS);
		
		} catch (ObjectInUseException e) {
			LOGGER.error("Грешка при изтриване на достъп на служител - {}", e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на достъп на служител! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	/**
	 * Изрично отнемане на достъп до преписка
	 */
	public void actionExplicitТакеAccessDela() {		
		
		try {
			
			if (!actionCheckLogInSystem(codeTakeAwayAccess, 2)) {
				
				List<Integer> delaIds = new ArrayList<>();
				
				for (Object[] dela : this.deloSelectedAllM) {			
					delaIds.add(SearchUtils.asInteger(dela[0]));			
				}
			
			
				JPA.getUtil().runInTransaction(() -> { 
					
					int lead = this.dostUtils.denyDeloAccess(delaIds, this.codeTakeAwayAccess, getSystemData(), getUserData());
					
					if (lead > 0) {					
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString(beanMessages, "takeAwayAccess.leadDelaForSluj"));
						
					} else {					
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "takeAwayAccess.denyDeloAccess"));
					}
					
				});		
			}
			
			PrimeFaces.current().executeScript(SCROLLTOERRORS);	
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изрично отнемане на достъп до преписка! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	 

	}
	
	/**
	 * Премахване на отнетия достъп на служител до преписки/дела
	 */
	public void actionRemoveExplicitТакеAccessDela() {

		try {
			
			if (!actionCheckLogInSystem(codeTakeAwayAccess, 3)) {
				
				List<Integer> delaIds = new ArrayList<>();
				
				for (Object[] dela : this.deloSelectedAllM) {			
					delaIds.add(SearchUtils.asInteger(dela[0]));			
				}	
			
				JPA.getUtil().runInTransaction(() ->  this.dostUtils.removeDeniedAccess(delaIds, this.codeTakeAwayAccess, false, getSystemData(), getUserData()));
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "takeAwayAccess.removeDeniedDeloAccess"));
			}
			
			PrimeFaces.current().executeScript(SCROLLTOERRORS);
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при премахнахване на отнетия достъп на служителя до преписки/дела! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}

	}
	
	/**
	 * Изрично премахване на достъп до документ 
	 */
	public void actionExplicitТакеAccessDocs() {
		
		try {
			
			if (!actionCheckLogInSystem(codeTakeAwayAccess, 4)) {
				
				List<Integer> docIds = new ArrayList<>();
				
				for (Object[] doc : this.docSelectedAllM) {			
					docIds.add(SearchUtils.asInteger(doc[0]));			
				}
			
				JPA.getUtil().runInTransaction(() ->  this.dostUtils.denyDocAccess(docIds, this.codeTakeAwayAccess, getSystemData(), getUserData()));
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "takeAwayAccess.denyDocAccess"));
			}
			
			PrimeFaces.current().executeScript(SCROLLTOERRORS);
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изрично отнемане на достъп до документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	 

	}
	
	/**
	 * Премахване на отнетия достъп на служителя до документи
	 */
	public void actionRemoveExplicitТакеAccessDocs() {

		try {
			
			if (!actionCheckLogInSystem(codeTakeAwayAccess, 5)) {
				
				List<Integer> docIds = new ArrayList<>();
				
				for (Object[] doc : this.docSelectedAllM) {			
					docIds.add(SearchUtils.asInteger(doc[0]));			
				}
			
				JPA.getUtil().runInTransaction(() ->  this.dostUtils.removeDeniedAccess(docIds, this.codeTakeAwayAccess, true, getSystemData(), getUserData()));
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "takeAwayAccess.removeDeniedDocAccess"));
			}
			
			PrimeFaces.current().executeScript(SCROLLTOERRORS);
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при премахнахване на отнетия достъп на служителя до документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}

	}	 
		
	/**
	 * Множествен избор на дело/преписка
	 */
	/**
	 * Избира всички редове от текущата страница
	 * @param event
	 */
	  public void onRowSelectAll(ToggleSelectEvent event) {    
    	
			List<Object[]> tmpL = new ArrayList<>();
			tmpL.addAll(getDeloSelectedAllM());
			
			if (event.isSelected()) {
				for (Object[] obj : getDeloSelectedTmp()) {
					if(obj != null && obj.length > 0) {
						boolean bb = true;
						Long l2 = Long.valueOf(obj[0].toString());
						for (Object[] j : tmpL) {
							Long l1 = Long.valueOf(j[0].toString());
							if (l1.equals(l2)) {
								bb = false;
								break;
							}
						}
						if (bb) {
							tmpL.add(obj);
						}
					}
				}
			
			} else {
				
				List<Object[]> tmpLPageC = getDeloList().getResult();// rows from current page....
				for (Object[] obj : tmpLPageC) {
					if(obj != null && obj.length > 0) {
						Long l2 = Long.valueOf(obj[0].toString());
						for (Object[] j : tmpL) {
							Long l1 = Long.valueOf(j[0].toString());
							if (l1.equals(l2)) {
								tmpL.remove(j);
								break;
							}
						}
					}
				}
			}
			setDeloSelectedAllM(tmpL);
			LOGGER.debug("onToggleSelect->>");
	  }
		    
    /** 
     * Select one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowSelect(SelectEvent event) {    	
    	
    	if(event != null  && event.getObject() != null) {
    		
    		List<Object[]> tmpList = getDeloSelectedAllM();
    		
    		Object[] obj = (Object[]) event.getObject();
    		boolean bb = true;
    		Integer l2 = Integer.valueOf(obj[0].toString());
			for (Object[] j : tmpList) { 
				Integer l1 = Integer.valueOf(j[0].toString());        			
	    		if(l1.equals(l2)) {
	    			bb = false;
	    			break;
	    		}
	   		}
			
			if(bb) {
				tmpList.add(obj);
				setDeloSelectedAllM(tmpList);   
			}
    	}	    	
    	
    	LOGGER.debug("1 onRowSelectIil {}", getDeloSelectedAllM().size());
    }
		 
		    
    /**
     * unselect one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowUnselect(UnselectEvent event) {
    	
    	if(event != null  && event.getObject() != null) {
    		
    		Object[] obj = (Object[]) event.getObject();
    		List<Object[] > tmpL = new ArrayList<>();
    		tmpL.addAll(getDeloSelectedAllM());
    		
    		for (Object[] j : tmpL) {
    			Integer l1 = Integer.valueOf(j[0].toString());
    			Integer l2 = Integer.valueOf(obj[0].toString());
	    		if(l1.equals(l2)) {
	    			tmpL.remove(j);
	    			setDeloSelectedAllM(tmpL);
	    			break;
	    		}
    		}
    		
    		LOGGER.debug( "onRowUnselectIil {}", getDeloSelectedAllM().size());
    	}
    }

    /**
     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга
     */
    public void   onPageUpdateSelected(){
    	
    	if (getDeloSelectedAllM() != null && !getDeloSelectedAllM().isEmpty()) {
    		getDeloSelectedTmp().clear();
    		getDeloSelectedTmp().addAll(getDeloSelectedAllM());
    	}	    	
    	
    	LOGGER.debug( "onPageUpdateSelected {}", getDeloSelectedTmp().size());
    }
    
    
    /*
	 * Множествен избор на документ
	 */
	/**
	 * Избира всички редове от текущата страница
	 * @param event
	 */
	  public void onRowSelectAllDoc(ToggleSelectEvent event) {    
    	
		  List<Object[]> tmpL = new ArrayList<>();
		  tmpL.addAll(getDocSelectedAllM());
	    	
		  if(event.isSelected()) {
	    		
				for (Object[] obj : getDocSelectedTmp()) {
					if (obj != null && obj.length > 0) {
						boolean bb = true;
						Long l2 = Long.valueOf(obj[0].toString());

						for (Object[] j : tmpL) {
							Long l1 = Long.valueOf(j[0].toString());
							if (l1.equals(l2)) {
								bb = false;
								break;
							}
						}

						if (bb) {
							tmpL.add(obj);
						}
					}
				}	
	    	
		  } else {
		    	
			  List<Object[]> tmpLPageC =  getDocsList().getResult();// rows from current page....    		
				for (Object[] obj : tmpLPageC) {
					if(obj != null && obj.length > 0) {
						Long l2 = Long.valueOf(obj[0].toString());
						for (Object[] j : tmpL) { 
			    			Long l1 = Long.valueOf(j[0].toString());        			
				    		if(l1.equals(l2)) {    	    			
				    			tmpL.remove(j);
				    			break;
				    		}	
			    		}
					}
				}    		
			}
			
		  setDocSelectedAllM(tmpL);	    	
		  LOGGER.debug("onToggleSelect->>");	    	   
		}
		    
    /** 
     * Select one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowSelectDoc(SelectEvent event) {    	
    	
    	if(event != null  && event.getObject() != null) {
    		
    		List<Object[]> tmpList =  getDocSelectedAllM();
    		
    		Object[] obj = (Object[]) event.getObject();
    		boolean bb = true;
    		Integer l2 = Integer.valueOf(obj[0].toString());
			
    		for (Object[] j : tmpList) { 
				Integer l1 = Integer.valueOf(j[0].toString());        			
	    		if(l1.equals(l2)) {
	    			bb = false;
	    			break;
	    		}
	   		}
			
    		if(bb) {
				tmpList.add(obj);
				setDocSelectedAllM(tmpList);   
			}
    	}	    	
    	
    	LOGGER.debug("1 onRowSelectIil {}", getDocSelectedAllM().size());
    }
		 
		    
    /**
     * unselect one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowUnselectDoc(UnselectEvent event) {
    	
    	if(event != null  && event.getObject() != null) {
    		
    		Object[] obj = (Object[]) event.getObject();
    		List<Object[] > tmpL = new ArrayList<>();
    		tmpL.addAll(getDocSelectedAllM());
    		
    		for (Object[] j : tmpL) {
    			Integer l1 = Integer.valueOf(j[0].toString());
    			Integer l2 = Integer.valueOf(obj[0].toString());
	    		if(l1.equals(l2)) {
	    			tmpL.remove(j);
	    			setDocSelectedAllM(tmpL);
	    			break;
	    		}
    		}
    		
    		LOGGER.debug( "onRowUnselectIil {}", getDocSelectedAllM().size());
    	}
    }

    /**
     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга
     */
    public void   onPageUpdateSelectedDoc(){
    	
    	if (getDocSelectedAllM() != null && !getDocSelectedAllM().isEmpty()) {
    		getDocSelectedTmp().clear();
    		getDocSelectedTmp().addAll(getDocSelectedAllM());
    	}	    	
    	
    	LOGGER.debug( "onPageUpdateSelected {}", getDocSelectedTmp().size());
    }
    
    public Date getDecodeDate() {
		return new Date(decodeDate.getTime()) ;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
	}	

	public Integer getCodeTakeAwayAccess() {
		return codeTakeAwayAccess;
	}

	public void setCodeTakeAwayAccess(Integer codeTakeAwayAccess) {
		this.codeTakeAwayAccess = codeTakeAwayAccess;
	}

	public String getUserNameForTakeAway() {
		return userNameForTakeAway;
	}

	public void setUserNameForTakeAway(String userNameForTakeAway) {
		this.userNameForTakeAway = userNameForTakeAway;
	}

	public DeloSearch getDeloSearch() {
		return deloSearch;
	}

	public void setDeloSearch(DeloSearch deloSearch) {
		this.deloSearch = deloSearch;
	}

	public LazyDataModelSQL2Array getDeloList() {
		return deloList;
	}

	public void setDeloList(LazyDataModelSQL2Array deloList) {
		this.deloList = deloList;
	}

	public String getRadioAccessDela() {
		return radioAccessDela;
	}

	public void setRadioAccessDela(String radioAccessDela) {
		this.radioAccessDela = radioAccessDela;
	}

	public Integer getPeriod() {
		return period;
	}

	public void setPeriod(Integer period) {
		this.period = period;
	}

	public List<Object[]> getDeloSelectedAllM() {
		return deloSelectedAllM;
	}

	public void setDeloSelectedAllM(List<Object[]> deloSelectedAllM) {
		this.deloSelectedAllM = deloSelectedAllM;
	}

	public List<Object[]> getDeloSelectedTmp() {
		return deloSelectedTmp;
	}

	public void setDeloSelectedTmp(List<Object[]> deloSelectedTmp) {
		this.deloSelectedTmp = deloSelectedTmp;
	}

	public Integer getPeriodStat() {
		return periodStat;
	}

	public void setPeriodStat(Integer periodStat) {
		this.periodStat = periodStat;
	}

	public boolean isRenderedDelo() {
		return renderedDelo;
	}

	public void setRenderedDelo(boolean renderedDelo) {
		this.renderedDelo = renderedDelo;
	}

	public boolean isRenderedDoc() {
		return renderedDoc;
	}

	public void setRenderedDoc(boolean renderedDoc) {
		this.renderedDoc = renderedDoc;
	}

	public DocSearch getSearchDoc() {
		return searchDoc;
	}

	public void setSearchDoc(DocSearch searchDoc) {
		this.searchDoc = searchDoc;
	}

	public Integer getPeriodR() {
		return periodR;
	}

	public void setPeriodR(Integer periodR) {
		this.periodR = periodR;
	}

	public List<SystemClassif> getDocsVidClassif() {
		return docsVidClassif;
	}

	public void setDocsVidClassif(List<SystemClassif> docsVidClassif) {
		this.docsVidClassif = docsVidClassif;
	}

	public List<SystemClassif> getDocsRegistriClassif() {
		return docsRegistriClassif;
	}

	public void setDocsRegistriClassif(List<SystemClassif> docsRegistriClassif) {
		this.docsRegistriClassif = docsRegistriClassif;
	}

	public Map<Integer, Object> getSpecificsRegisterSource() {
		return specificsRegisterSource;
	}

	public void setSpecificsRegisterSource(Map<Integer, Object> specificsRegisterSource) {
		this.specificsRegisterSource = specificsRegisterSource;
	}

	public LazyDataModelSQL2Array getDocsList() {
		return docsList;
	}

	public void setDocsList(LazyDataModelSQL2Array docsList) {
		this.docsList = docsList;
	}

	public String getRadioAccessDocs() {
		return radioAccessDocs;
	}

	public void setRadioAccessDocs(String radioAccessDocs) {
		this.radioAccessDocs = radioAccessDocs;
	}

	public List<Object[]> getDocSelectedAllM() {
		return docSelectedAllM;
	}

	public void setDocSelectedAllM(List<Object[]> docSelectedAllM) {
		this.docSelectedAllM = docSelectedAllM;
	}

	public List<Object[]> getDocSelectedTmp() {
		return docSelectedTmp;
	}

	public void setDocSelectedTmp(List<Object[]> docSelectedTmp) {
		this.docSelectedTmp = docSelectedTmp;
	}

	public Integer getUserRegistratura() {
		return userRegistratura;
	}

	public void setUserRegistratura(Integer userRegistratura) {
		this.userRegistratura = userRegistratura;
	}

	public boolean isUserLeave() {
		return userLeave;
	}

	public void setUserLeave(boolean userLeave) {
		this.userLeave = userLeave;
	}

	public Serializable[] getCheckUserAccessDela() {
		return checkUserAccessDela;
	}

	public void setCheckUserAccessDela(Serializable[] checkUserAccessDela) {
		this.checkUserAccessDela = checkUserAccessDela;
	}

	public Serializable[] getCheckUserAccessDocs() {
		return checkUserAccessDocs;
	}

	public void setCheckUserAccessDocs(Serializable[] checkUserAccessDocs) {
		this.checkUserAccessDocs = checkUserAccessDocs;
	}	

}
package com.ib.omb.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.RegistraturaDAO;
import com.ib.omb.search.DeloSearch;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;

@Named
@ViewScoped
public class TransferDeloDoc  extends IndexUIbean   {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9036780118235758278L;

	private static final Logger LOGGER = LoggerFactory.getLogger(TransferDeloDoc.class);

	private Date decodeDate=new Date();
	private transient RegistraturaDAO regDao;
	
	//за основния филтър 
	private Integer registratura;
	private Integer register;
	private Map<Integer, Object> specificsRegister = new HashMap<>();  
	private List<SystemClassif> classifRegsList;
	private Integer userRegistratura;
	
	//за дела/преписки
	private DeloSearch deloSearch;	              
	private Integer period=null;
	private Integer periodStat=null;
	
	private List<Object[]> deloSelectedAllM=new ArrayList<>();
	private List<Object[]> deloSelectedTmp=new ArrayList<>();
	
	private LazyDataModelSQL2Array deloList;  
	
	private boolean renderedDelo = false;
	
	//за документи
	
	private boolean renderedDoc = false; 
	private DocSearch searchDoc;
	private Integer periodR;
	private List<SystemClassif> docsVidClassif = new ArrayList<>();
	private List<SystemClassif> docsRegistriClassif = new ArrayList<>();
	private Map<Integer, Object> specificsRegisterSource; //за текущата регистратура
	private LazyDataModelSQL2Array docsList;      // списък документи 
	
	private List<Object[]> docSelectedAllM=new ArrayList<>();
	private List<Object[]> docSelectedTmp=new ArrayList<>();
	
	/** */
	@PostConstruct
	void initData() {
		
		try {
			userRegistratura = getUserData(UserData.class).getRegistratura();
			regDao = new RegistraturaDAO(getUserData());
			deloSearch=new DeloSearch(userRegistratura);
			deloSearch.setNotIncluded(true);
			List <Integer> notInCodes = new ArrayList<>();			
			notInCodes.add(userRegistratura);//без текущата				
			this.classifRegsList = getSystemData().queryClassification(OmbConstants.CODE_CLASSIF_REGISTRATURI, null, new Date(), getCurrentLang(), notInCodes);
			searchDoc = new DocSearch(userRegistratura);
			searchDoc.setNotIncluded(true);
			setSpecificsRegisterSource(Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA,Optional.of(userRegistratura)));
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на списъка с регистратурите без текущата регистратура!! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}


	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClearDelo() {
		deloList=null;
		period=null;
		periodStat = null;
		deloSearch=new DeloSearch(userRegistratura);		
		deloSearch.setNotIncluded(true);
		deloSelectedTmp.clear();
		deloSelectedAllM.clear();
	}
	
	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClearDoc() {
		docsList=null;
		periodR=null;	
		searchDoc = new DocSearch(userRegistratura);
		searchDoc.setNotIncluded(true);
		docSelectedTmp.clear();
		docSelectedAllM.clear();		
	}
	
	public void selectDeloSearch() {
		renderedDelo = true;
		renderedDoc = false;
		actionClearDoc();
	}
	
	public void selectDocSearch() {
		renderedDelo = false;
		renderedDoc = true;
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
			deloSearch.setDeloDateFrom(di[0]);
			deloSearch.setDeloDateTo(di[1]);		
    	} else {
    		deloSearch.setDeloDateFrom(null);
			deloSearch.setDeloDateTo(null);
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
			deloSearch.setStatusDateFrom(di[0]);
			deloSearch.setStatusDateTo(di[1]);		
    	} else {
    		deloSearch.setStatusDateFrom(null);
			deloSearch.setStatusDateTo(null);
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
			searchDoc.setDocDateFrom(di[0]);
			searchDoc.setDocDateTo(di[1]);		
    	} else {
    		searchDoc.setDocDateFrom(null);
			searchDoc.setDocDateTo(null);
		}
    }
	
	public void changeDateStat() { 
		this.setPeriodStat(null);
	}
	
	  
	public void actionSearchDelo(){
		 deloSelectedAllM.clear();
		 deloSelectedTmp.clear();
		 deloSearch.setUseDost(false);		
		 deloSearch.buildQueryDeloList(getUserData(),4);
		 deloList = new LazyDataModelSQL2Array(deloSearch, "a1 desc"); 

	}	
	
	
	public void actionSearchDoc(){
		 docSelectedAllM.clear();
		 docSelectedTmp.clear();
		 searchDoc.setUseDost(false);		
		 searchDoc.buildQueryDocList(getUserData(), true);
		 docsList = new LazyDataModelSQL2Array(searchDoc, "a4 desc"); 

	}	
	
	public void actionTransferDela() {
		try { 
			JPA.getUtil().runInTransaction(() -> regDao.transferDeloList(deloSelectedAllM, registratura, register, getSystemData()));
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "transferDeloDoc.success"));			
		
			
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void actionTransferDocs() {
		try { 
			JPA.getUtil().runInTransaction(() -> regDao.transferDocList(docSelectedAllM, registratura, register, getSystemData()));
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "transferDeloDoc.success"));			
		
			
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	//При смяна на избрана регистратура
	public void actionChangeReg() {
			
			if(this.registratura!=null) {
				specificsRegister.put( OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA, this.registratura);
				specificsRegister.put( OmbClassifAdapter.REGISTRI_INDEX_ALG , OmbConstants.CODE_ZNACHENIE_ALG_FREE);
				//setSpecificsRegister(Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA,Optional.of(this.registratura)));
			}else {
				specificsRegister = new HashMap<>();
			}
			register = null;
	}
	 
	 
		
	/*
	 * Множествен избор на дело/преписка
	 */
	/**
	 * Избира всички редове от текущата страница
	 * @param event
	 */
	  public void onRowSelectAll(ToggleSelectEvent event) {    
    	List<Object[]> tmpL = new ArrayList<>();
    	tmpL.addAll(getDeloSelectedAllM());
    	if(event.isSelected()) {
    		for (Object[] obj : getDeloSelectedTmp()) {
    			if(obj != null && obj.length > 0) {
	    			boolean bb = true;
	    			Long l2 = Long.valueOf(obj[0].toString());
	    			for (Object[] j : tmpL) { 
	        			Long l1 = Long.valueOf(j[0].toString());        			
	    	    		if(l1.equals(l2)) {    	    			
	    	    			bb = false;
	    	    			break;
	    	    		}
	        		}
	    			if(bb) {
	    				tmpL.add(obj);
	    			}
    			}
    		}    		
    	}else {
	    	List<Object[]> tmpLPageC =  getDeloList().getResult();// rows from current page....    		
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
		setDeloSelectedAllM(tmpL);	    	
		LOGGER.debug("onToggleSelect->>");	    	   
	}
		    
    /** 
     * Select one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowSelect(SelectEvent event) {    	
    	if(event!=null  && event.getObject()!=null) {
    		List<Object[]> tmpList =  getDeloSelectedAllM();
    		
    		Object[] obj = (Object[]) event.getObject();
    		if(obj != null && obj.length > 0) {
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
    	}	    	
    	LOGGER.debug("1 onRowSelectIil->>"+getDeloSelectedAllM().size());
    }
		 
		    
    /**
     * unselect one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowUnselect(UnselectEvent event) {
    	if(event!=null  && event.getObject()!=null) {
    		Object[] obj = (Object[]) event.getObject();
    		List<Object[] > tmpL = new ArrayList<>();
    		tmpL.addAll(getDeloSelectedAllM());
    		for (Object[] j : tmpL) {
    			if(j != null && j.length > 0 && obj != null && obj.length > 0) {
	    			Integer l1 = Integer.valueOf(j[0].toString());
	    			Integer l2 = Integer.valueOf(obj[0].toString());
		    		if(l1.equals(l2)) {
		    			tmpL.remove(j);
		    			setDeloSelectedAllM(tmpL);
		    			break;
		    		}
	    		}
    		}
    		LOGGER.debug( "onRowUnselectIil->>"+getDeloSelectedAllM().size());
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
    	LOGGER.debug( " onPageUpdateSelected->>"+getDeloSelectedTmp().size());
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
    			boolean bb = true;
    			Long l2 = Long.valueOf(obj[0].toString());
    			for (Object[] j : tmpL) { 
        			Long l1 = Long.valueOf(j[0].toString());        			
    	    		if(l1.equals(l2)) {    	    			
    	    			bb = false;
    	    			break;
    	    		}
        		}
    			if(bb) {
    				tmpL.add(obj);
    			}
    		}    		
    	}else {
	    	List<Object[]> tmpLPageC =  getDocsList().getResult();// rows from current page....    		
			for (Object[] obj : tmpLPageC) {
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
		setDocSelectedAllM(tmpL);	    	
		LOGGER.debug("onToggleSelect->>");	    	   
	}
		    
    /** 
     * Select one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowSelectDoc(SelectEvent event) {    	
    	if(event!=null  && event.getObject()!=null) {
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
    	LOGGER.debug("1 onRowSelectIil->>"+getDocSelectedAllM().size());
    }
		 
		    
    /**
     * unselect one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowUnselectDoc(UnselectEvent event) {
    	if(event!=null  && event.getObject()!=null) {
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
    		LOGGER.debug( "onRowUnselectIil->>"+getDocSelectedAllM().size());
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
    	LOGGER.debug( " onPageUpdateSelected->>"+getDocSelectedTmp().size());
    }
    
	public Date getDecodeDate() {
		return decodeDate;
	}


	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
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


	public Integer getRegistratura() {
		return registratura;
	}


	public void setRegistratura(Integer registratura) {
		this.registratura = registratura;
	}


	public Integer getRegister() {
		return register;
	}


	public void setRegister(Integer register) {
		this.register = register;
	}


	public Map<Integer, Object> getSpecificsRegister() {
		return specificsRegister;
	}


	public void setSpecificsRegister(Map<Integer, Object> specificsRegister) {
		this.specificsRegister = specificsRegister;
	}


	public List<SystemClassif> getClassifRegsList() {
		return classifRegsList;
	}


	public void setClassifRegsList(List<SystemClassif> classifRegsList) {
		this.classifRegsList = classifRegsList;
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

}
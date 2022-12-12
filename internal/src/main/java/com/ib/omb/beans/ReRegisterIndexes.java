package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.ib.omb.db.dao.DocShemaDAO;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.ObjectInUseException;

@Named
@ViewScoped
public class ReRegisterIndexes extends IndexUIbean  implements Serializable {

	/**
	 * Пререгистране на номенклатурни дела
	 * 
	 */
	private static final long serialVersionUID = 8824073816436215746L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ReRegisterIndexes.class);
	
	private LazyDataModelSQL2Array schemaList;
	private transient DocShemaDAO schemaDao;
	private String radioValid;
	private boolean previousValid=true;
	private List<Object[]> selectedIndexes = new ArrayList<>();
	private List<Object[]> selectedIndexesAll = new ArrayList<>();
						
	@PostConstruct
	void initData() {
				
		LOGGER.debug("PostConstruct!!!");	
		schemaDao = new DocShemaDAO(getUserData());
		actionSearch();				
	}
	
	/**
	 * Зареждане на данните за индекси на номенклатурни дела
	 * 
	 */	
	public void actionSearch() {
		if("1".equals(radioValid)) {
			previousValid=true;
		}else if("2".equals(radioValid)) {
			previousValid=false;
		}
		try {
			SelectMetadata smd = this.schemaDao.createSelectMetadataByValid(getUserData(UserData.class).getRegistratura(), previousValid);			
			String defaultSortColumn = "A0";	
			this.schemaList = new LazyDataModelSQL2Array(smd, defaultSortColumn);						
		}catch (Exception e) {
			LOGGER.error("Грешка при зареждане данните за индекси на номенклатурни дела!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
		if(schemaList==null || schemaList.getRowCount()==0) {
			if(previousValid) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "regIndexes.validIndexes"));								
			}else {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "regIndexes.otherIndexes"));		
			}
		}
	}		
	
	/**
	 * Регистриране на дела за избраните индекси за текущата година
	 * 
	 */	
	public void actionSave() {
		
		if(selectedIndexesAll==null || selectedIndexesAll.isEmpty()){
			JSFUtils.addMessage(null, FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "regIndexes.selectIndex"));
			
		}else {
			PrimeFaces.current().executeScript("PF('hiddenButton').jq.click();");
		}
		
		
	}
	
	/**
	 * Запис на номенклатурни дела
	 * 
	 */
	public void save(){
		try {
			
			JPA.getUtil().runInTransaction(() -> this.schemaDao.registerNomDelaByIndex(getUserData(UserData.class).getRegistratura(),previousValid,selectedIndexesAll,getSystemData()));		
		
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.succesSaveMsg"));					
		} catch (ObjectInUseException  e) {					
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} catch (BaseException e) {	
		
			LOGGER.error("Грешка при регистриране на дела за избраните индекси за текущата година! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}			
		actionSearch();
	}
	
	/**
	 * Множествен избор 
	 */
	/**
	 * Избира всички редове от текущата страница
	 * @param event
	 */
	  public void onRowSelectAll(ToggleSelectEvent event) {    
    	List<Object[]> tmpL = new ArrayList<>();
    	tmpL.addAll(selectedIndexesAll);
    	if(event.isSelected()) {
    		for (Object[] obj : selectedIndexes) {
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
	    	List<Object[]> tmpLPageC =  schemaList.getResult();// rows from current page....    		
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
		setSelectedIndexesAll(tmpL);	    	
		LOGGER.debug("onToggleSelect->>");	    	   
	}
		    
    /** 
     * Select one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowSelect(SelectEvent event) {    	
    	if(event!=null  && event.getObject()!=null) {
    		List<Object[]> tmpList =  selectedIndexesAll;
    		
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
				setSelectedIndexesAll(tmpList);   
			}
    	}	    	
    	LOGGER.debug("1 onRowSelectIil->>"+selectedIndexesAll.size());
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
    		tmpL.addAll(selectedIndexesAll);
    		for (Object[] j : tmpL) {
    			Integer l1 = Integer.valueOf(j[0].toString());
    			Integer l2 = Integer.valueOf(obj[0].toString());
	    		if(l1.equals(l2)) {
	    			tmpL.remove(j);
	    			setSelectedIndexesAll(tmpL);
	    			break;
	    		}
    		}
    		LOGGER.debug( "onRowUnselectIil->>"+selectedIndexesAll.size());
    	}
    }

    /**
     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга
     */
    public void   onPageUpdateSelected(){
    	if (selectedIndexesAll != null && !selectedIndexesAll.isEmpty()) {
    		selectedIndexes.clear();
    		selectedIndexes.addAll(selectedIndexesAll);
    	}	    	
    	LOGGER.debug( " onPageUpdateSelected->>"+selectedIndexes.size());
    }
    
	public LazyDataModelSQL2Array getSchemaList() {
		return schemaList;
	}

	public void setSchemaList(LazyDataModelSQL2Array schemaList) {
		this.schemaList = schemaList;
	}

	public Date getToday() {
    	return new Date();
    }

	public String getRadioValid() {
		return radioValid;
	}

	public void setRadioValid(String radioValid) {
		this.radioValid = radioValid;
	}


	public List<Object[]> getSelectedIndexes() {
		return selectedIndexes;
	}


	public void setSelectedIndexes(List<Object[]> selectedIndexes) {
		this.selectedIndexes = selectedIndexes;
	}


	public List<Object[]> getSelectedIndexesAll() {
		return selectedIndexesAll;
	}


	public void setSelectedIndexesAll(List<Object[]> selectedIndexesAll) {
		this.selectedIndexesAll = selectedIndexesAll;
	}
	
}
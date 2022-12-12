package com.ib.omb.components;

import static com.ib.system.utils.SearchUtils.isEmpty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.model.SelectItem;

import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.search.DeloSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SysClassifUtils;

/** */
@FacesComponent(value = "compDeloSearch", createTag = true)
public class CompDeloSearch extends UINamingContainer {
	
	private enum PropertyKeys {
		DELOSEARCH, DELOLIST, SHOWME, STATUSLIST, TYPELIST, DELOSELTMP, DELOSEL, NASTRWITHTOM
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CompDeloSearch.class);


	private UserData	userData	= null;
	private SystemData	systemData	= null;
	private Date		dateClassif	= null;
	private Integer		periodR;
	private TimeZone timeZone = TimeZone.getDefault();
	
	/**
	 * Разширеното търсене - инциализира компонентата   <f:event type="preRenderComponent" listener="#{cc.initDeloSearch(true)}" />
	 */	
	public void initDeloSearch(boolean bb) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("initDeloSearch>>>> ");
		}
		
		Integer idReg = (Integer) getAttributes().get("idRegistratura");
		if(idReg == null) {
			idReg = getUserData().getRegistratura();
		}		
		Integer idDoc = (Integer) getAttributes().get("idDoc");
		Integer idDeloDoc = (Integer) getAttributes().get("idDeloDoc");
		Integer idProtocol = (Integer) getAttributes().get("idProtocol");
		Integer typeDelo = (Integer) getAttributes().get("typeDelo");
		Boolean notIncluded = (Boolean) getAttributes().get("notIncluded");
		Integer notInTip = (Integer) getAttributes().get("notInTip");
		Boolean addDeloAccess = (Boolean) getAttributes().get("addDeloAccess");
		Integer[] typeArr=null;
		if(typeDelo != null) {
			typeArr = new Integer[1];
			typeArr[0] = typeDelo;
		}
		
		DeloSearch srchDelo = new DeloSearch(idReg);		
		srchDelo.setNotRelDocId(idDoc); 		 // за да изключи преписките, в които вече е вложен документа
		srchDelo.setNotRelDeloId(idDeloDoc);	 //за да изключи делата/преписките, в които вече е вложенa преписката
		srchDelo.setDeloTypeArr(typeArr);
		srchDelo.setNotInProtocolId(idProtocol);
		srchDelo.setUseDost(false); // да не се ограничава достъпа!! За сега
		srchDelo.setNotIncluded(notIncluded);
		srchDelo.setNotInTip(notInTip); // да се ограничи по тип - обикн да се изключат номенклатурните дела
		srchDelo.setAddDeloAccess(addDeloAccess); //Включва и преписките, до които потребителя има изричен достъп, без значение на Регистратурата
	
		if(bb) { // при отваряне на модалния
			String tmp = (String) getAttributes().get("searchRnDelo"); 
			srchDelo.setRnDelo(tmp);
			setDeloSearch(srchDelo);
			initDeloSearchOM(tmp, notInTip);
		}else { // при изчистване...
			ValueExpression expr2 = getValueExpression("searchRnDelo");
			ELContext ctx2 = getFacesContext().getELContext();
			if (expr2 != null) {
				expr2.setValue(ctx2, null);
			}	
			setDeloSearch(srchDelo);
			setDeloList(null);
		}				
	}
	
	/**
	 * при отваряне на модалния (инциализира компонентата )
	 * @param tmpRn - номер на преписка
	 * @param notInTip - да изключи опредлен тип 
	 */
	private void initDeloSearchOM(String tmpRn, Integer notInTip) {
	    if(!isEmpty(tmpRn)) {
			actionSearchDelo(); // ако има подаден номер - търсенето да се пусне при инциализиране на комп.
		} else {
			setDeloList(null);
		}
		try { 
			setNastrWithTom(false);
			String param1 = getSystemData().getSettingsValue("delo.deloWithToms"); // да се работи с томове
			if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
				setNastrWithTom(true);
			}
			if(getStatusList()==null || getTypeList().isEmpty()) {
				setTypeList(createItemsListDS( OmbConstants.CODE_CLASSIF_DELO_TYPE, getDateClassif(), true, notInTip));
				setStatusList(createItemsListDS( OmbConstants.CODE_CLASSIF_DELO_STATUS,getDateClassif(), true, null));	
				
				//Integer[]	tmpArr = new Integer[1];
				//tmpArr[0] = OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ACTIVE; //???? да се зарежда ли стойност по подразбиране???
				//getDeloSearch().setStatusArr(tmpArr);
			}
			setShowMe(true);
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на класиф. за статус и/или тип на преписка! ", e);
		} 
		
	}
	
	
	public List<SelectItem> createItemsListDS(Integer codeClassif, Date date, Boolean sortByName, Integer notInTip) throws DbErrorException, UnexpectedResultException {
		List<SystemClassif> list;
		
		list = getSystemData().getSysClassification(codeClassif, date, getLang());
		
		if (sortByName != null) { // само при необходимост

			if (sortByName.booleanValue()) {
				SysClassifUtils.doSortClassifTekst(list);
			} else {
				SysClassifUtils.doSortClassifPrev(list);
			}
		}

		List<SelectItem> items = new ArrayList<>(list.size());
		for (SystemClassif x : list) {
			if (!Objects.equals(notInTip, x.getCode()) ) {
				items.add(new SelectItem(x.getCode(), x.getTekst()));
			}
		}
		return items;
	}
	
	 
   /** 
    * разширено търсене на дело/преписка - изп. се само, ако е в модален прозорец
    * изивква се при затваряне на модалния прозореца (onhide) 
    * 
    */
   public void actionHideModal() {		
	   setDeloSearch(null);
	   setDeloList(null);
	   setShowMe(false);
	}

   
	
   
   
   /** 
    * разширено търсене на преписка/дело -  - бутон "Търсене"
    */
   public void actionSearchDelo() {		
	   getDeloSearch().buildQueryComp(getUserData());
	   setDeloList(new LazyDataModelSQL2Array(getDeloSearch(), "a2 desc")); 
	   if (LOGGER.isDebugEnabled()) {
		   LOGGER.debug("actionSearchDelo>>>дело/преписка> {}",getDeloList().getRowCount());
	   }
	}
   
   /** 
    * разширено търсене на  - бутон "Изчисти"
    */
   public void actionClearDelo() {	
	   setDeloSelectedAllM(null);
	   setDeloSelectedTmp(null);
	   initDeloSearch(false);
	   periodR = null;
	}
   
  
   
   /**
    * Разширено търсене - единичен избор на дело/преписка
    */
   public void actionModalSelectDelo(Object[] row) {	  
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("actionModalSelectDelo>>>> {}",row[0]);
		}
	    if( row != null && row[0] != null) {
		   //връща обект SearchDelo
		    ValueExpression expr2 = getValueExpression("searchDeloS");
			ELContext ctx2 = getFacesContext().getELContext();
			if (expr2 != null) {
				expr2.setValue(ctx2, row);
			}	
	    }
	// извиква remoteCommnad - ако има такава....
		String remoteCommnad = (String) getAttributes().get("onComplete");
		if (remoteCommnad != null && !remoteCommnad.equals("")) {
			PrimeFaces.current().executeScript(remoteCommnad);
		}		
   }   

	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	 public void changePeriodR () {
			
	   	if (this.periodR != null) {
				Date[] di;
				di = DateUtils.calculatePeriod(this.periodR);
				getDeloSearch().setDeloDateFrom(di[0]);
				getDeloSearch().setDeloDateTo(di[1]);		
	   	} else {
	   		getDeloSearch().setDeloDateFrom(null);
	   		getDeloSearch().setDeloDateTo(null);
		}
    }
	
	public void changeDate() { 
		this.setPeriodR(null);
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
    		onRowSelectAllAddItem(tmpL);    		 		
    	}else {
    		onRowSelectAllDelItem(tmpL);	
		}
		setDeloSelectedAllM(tmpL);	
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("onToggleSelect->>");
		}
	}
	  
  /**
   *   Добавя избраните редове в общия масив
   * @param tmpL
   */
	private void onRowSelectAllAddItem(List<Object[]> tmpL ){
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
	}
	  
	/**
	 * Изважда редовете от текущата страница 
	 * @param tmpL
	 */
	private void onRowSelectAllDelItem(List<Object[]> tmpL ) {
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
	
	
    /** 
     * Select one row
     * @param event
     */
    public void onRowSelect(SelectEvent<?> event) {    	
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
    	if (LOGGER.isDebugEnabled()) {
    		LOGGER.debug("1 onRowSelect->>{}",getDeloSelectedAllM().size());
    	}
    }
		 
		    
    /**
     * unselect one row
     * @param event
     */
    public void onRowUnselect(UnselectEvent<?> event) {
    	if(event!=null  && event.getObject()!=null) {
    		Object[] obj = (Object[]) event.getObject();
    		Integer l2 = Integer.valueOf(obj[0].toString());
    		List<Object[] > tmpL = new ArrayList<>();
    		tmpL.addAll(getDeloSelectedAllM());
    		for (Object[] j : tmpL) {
    			Integer l1 = Integer.valueOf(j[0].toString());    		
	    		if(l1.equals(l2)) {
	    			tmpL.remove(j);
	    			setDeloSelectedAllM(tmpL);
	    			break;
	    		}
    		}
    		if (LOGGER.isDebugEnabled()) {
    			LOGGER.debug( "onRowUnselect->>{}",getDeloSelectedAllM().size());
    		}
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
    	if (LOGGER.isDebugEnabled()) {
    		LOGGER.debug( " onPageUpdateSelected->>{}", getDeloSelectedTmp().size());
    	}
    }


   /**
    * Връща списъка с избрани преписки/дела 
    */
   public void actionConfirmSelected() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("actionConfirmSelected{}", getDeloSelectedAllM().size());
		}
	   //връща обект списък от SearchDelo
	    ValueExpression expr2 = getValueExpression("selectedDeloList");
		ELContext ctx2 = getFacesContext().getELContext();
		if (expr2 != null) {
			expr2.setValue(ctx2, getDeloSelectedAllM());
		}	
		actionClearDelo();
	 // извиква remoteCommnad - ако има такава....
		String remoteCommnad = (String) getAttributes().get("onComplete");
		if (remoteCommnad != null && !remoteCommnad.equals("")) {
			PrimeFaces.current().executeScript(remoteCommnad);
		}	
			
	}
   
   
	
	
	/** @return */
	public boolean isShowMe() {
		return (Boolean) getStateHelper().eval(PropertyKeys.SHOWME, false);
	}
	/** @param showMe */
	public void setShowMe(boolean showMe) {
		getStateHelper().put(PropertyKeys.SHOWME, showMe);
	}

	public DeloSearch getDeloSearch() {
		DeloSearch eval = (DeloSearch) getStateHelper().eval(PropertyKeys.DELOSEARCH, null);
		return eval != null ? eval : new DeloSearch(getUserData().getRegistratura());
	}


	public void setDeloSearch(DeloSearch deloSearch) {
		getStateHelper().put(PropertyKeys.DELOSEARCH, deloSearch);
	}



	/** @return the dateClassif */
	private Date getDateClassif() {
		if (this.dateClassif == null) {
			this.dateClassif = (Date) getAttributes().get("dateClassif");
			if (this.dateClassif == null) {
				this.dateClassif = new Date();
			}
		}
		return this.dateClassif;
	}


	/** @return the userData */
	private UserData getUserData() {
		if (this.userData == null) {
			this.userData = (UserData) JSFUtils.getManagedBean("userData");
		}
		return this.userData;
	}

	/** @return */
	public Integer getLang() {
		return getUserData().getCurrentLang();
	}
	
	/** @return */
	public Date getCurrentDate() {
		return getDateClassif();
	}

	
	public LazyDataModelSQL2Array getDeloList() {
		return (LazyDataModelSQL2Array) getStateHelper().eval(PropertyKeys.DELOLIST, null);
	}


	public void setDeloList(LazyDataModelSQL2Array deloList) {
		getStateHelper().put(PropertyKeys.DELOLIST, deloList);
	}


	public TimeZone getTimeZone() {
		return timeZone;
	}


	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}


	public Integer getPeriodR() {
		return periodR;
	}


	public void setPeriodR(Integer periodR) {
		this.periodR = periodR;
	}


	@SuppressWarnings("unchecked")
	public List<SelectItem> getStatusList() {
		List<SelectItem> eval = (List<SelectItem>) getStateHelper().eval(PropertyKeys.STATUSLIST, null);
		return eval != null ? eval : new ArrayList<>();
	}


	public void setStatusList(List<SelectItem> statusList) {
		getStateHelper().put(PropertyKeys.STATUSLIST, statusList);
	}


	@SuppressWarnings("unchecked")
	public List<SelectItem> getTypeList() {
		List<SelectItem> eval = (List<SelectItem>) getStateHelper().eval(PropertyKeys.TYPELIST, null);
		return eval != null ? eval : new ArrayList<>();		
	}


	public void setTypeList(List<SelectItem> typeList) {
		getStateHelper().put(PropertyKeys.TYPELIST, typeList);
	}
	
	private SystemData getSystemData() {
		if (this.systemData == null) {
			this.systemData =  (SystemData) JSFUtils.getManagedBean("systemData");
		}
		return this.systemData;
	}


	@SuppressWarnings("unchecked")
	public List<Object[]> getDeloSelectedTmp() {
		List<Object[]> eval = (List<Object[]>) getStateHelper().eval(PropertyKeys.DELOSELTMP, null);
		return eval != null ? eval : new ArrayList<>();		
	}


	public void setDeloSelectedTmp(List<Object[]> deloSelectedTmp) {
		getStateHelper().put(PropertyKeys.DELOSELTMP, deloSelectedTmp);
	}


	@SuppressWarnings("unchecked")
	public List<Object[]> getDeloSelectedAllM() {
		List<Object[]> eval = (List<Object[]>) getStateHelper().eval(PropertyKeys.DELOSEL, null);
		return eval != null ? eval : new ArrayList<>();
	}


	public void setDeloSelectedAllM(List<Object[]> deloSelectedAllM) {
		getStateHelper().put(PropertyKeys.DELOSEL, deloSelectedAllM);
	}

	
	public boolean isNastrWithTom() {
		return (Boolean) getStateHelper().eval(PropertyKeys.NASTRWITHTOM, false);
	}
	
	public void setNastrWithTom(boolean nastrWithTom) {
		getStateHelper().put(PropertyKeys.NASTRWITHTOM, nastrWithTom);
	}

	
	
	
}
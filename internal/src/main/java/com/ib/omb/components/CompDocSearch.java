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
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;

/** */
@FacesComponent(value = "compDocSearch", createTag = true)
public class CompDocSearch extends UINamingContainer {
	
	private enum PropertyKeys {
		DOCSEARCH, DOCLIST, SHOWME, DOCSELTMP, DOCSEL, DOCSVIDCLASSIF, DOCSREGISTRICLASSIF, DOCTYPEARR
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(CompDocSearch.class);


	private UserData	userData	= null;
	private SystemData	systemData	= null;
	private Date		dateClassif	= null;
	private Integer		periodR;
	private TimeZone timeZone = TimeZone.getDefault();

	 
	/**
	 * Разширеното търсене - инциализира компонентата   <f:event type="preRenderComponent" listener="#{cc.initDocSearch(true)}" />
	 */	
	public void initDocSearch(boolean bb) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("initDocSearch>>>> ");
		}
	
	
		 
		Integer idReg = (Integer) getAttributes().get("idRegistratura");
		if(idReg == null) {
			idReg = getUserData().getRegistratura();
		}		
	
		DocSearch srchDoc = new DocSearch(idReg);		
		Integer idDoc = (Integer) getAttributes().get("idDoc");
		Integer idDelo = (Integer) getAttributes().get("idDelo");
		Integer idProtocol = (Integer) getAttributes().get("idProtocol");
		Integer markRelDocId = (Integer) getAttributes().get("markRelDocId");		
		
		Integer[] typeArr = (Integer[]) getAttributes().get("docTypeArr");
		if(typeArr != null) {
			// srchDoc.setDocTypeArr(typeArr);
			 createSelectItemListType(typeArr);
		}else {
			createSelectItemListType(null);
		}
		
		srchDoc.setNotInDocId(idDoc);
		srchDoc.setNotInDeloId(idDelo);
		srchDoc.setNotInProtocolId(idProtocol);
		srchDoc.setMarkRelDocId(markRelDocId);
		
		
		if(bb) { // при отваряне на модалния
			initDocSearchOM(srchDoc);		
		}else { // при изчистване...
			ValueExpression expr2 = getValueExpression("searchRnDoc");
			ELContext ctx2 = getFacesContext().getELContext();
			if (expr2 != null) {
				expr2.setValue(ctx2, null);
			}	
			setDocSearch(srchDoc);
			setDocList(null);
		}	
		getDocSearch().setNullWorkOffId((Boolean) getAttributes().get("isWorkOff"));
		
	}
	 
	/**
	 * при отваряне на модалния (инициализиране на комп.)
	 * @param srchDoc
	 */
	private void initDocSearchOM(DocSearch srchDoc ) {
		setShowMe(true);
		String tmp = (String) getAttributes().get("searchRnDoc"); 
		srchDoc.setRnDoc(tmp);			
		
		String tmp2 = (String) getAttributes().get("searchTehNomDoc"); 
		srchDoc.setTehNomer(tmp2);
		if(!isEmpty(tmp2)) {
			srchDoc.setTehNomerEQ(true); // ако е подаден техен номер, търсенето да е по пълно съвпадение по подразбиране
		}
		
		setDocSearch(srchDoc);
		
		if(!isEmpty(tmp) || !isEmpty(tmp2)) {
			actionSearchDoc(); // ако има подаден номер - търсенето да се пусне при инциализиране на комп.
		}else {
			setDocList(null);
		}
		 	
		@SuppressWarnings("unchecked")
		List<Object[]> sdl =  (List<Object[]>) getAttributes().get("selectedDocList");
		if(sdl != null && !sdl.isEmpty()) {
			// ако връзките не са записани със затваряне на модалния - при повторно отваряне, мракираните да останат
			setDocSelectedTmp(sdl);
			setDocSelectedAllM(sdl);
		}
	}
	
	
   /** 
    * разширено търсене на документ - изп. се само, ако е в модален прозорец
    * изивква се при затваряне на модалния прозореца (onhide) 
    * 
    */
   public void actionHideModal() {		
	   setDocSearch(null);
	   setDocList(null);
	   setShowMe(false);
	}

   
	
   
   
   /** 
    * разширено търсене на документ -  - бутон "Търсене"
    */
   public void actionSearchDoc() {		
	   Integer[] typeArr = (Integer[]) getAttributes().get("docTypeArr");
	   if(typeArr != null && getDocSearch().getDocTypeArr()!=null &&  getDocSearch().getDocTypeArr().length == 0) {
		   getDocSearch().setDocTypeArr(typeArr);
	   }
	   setDocSelectedAllM(null);
	   setDocSelectedTmp(null);
	   getDocSearch().buildQueryComp(getUserData());
	   setDocList(new LazyDataModelSQL2Array(getDocSearch(), "a4 desc")); 
	   if (LOGGER.isDebugEnabled()) {
		   LOGGER.debug("actionSearchDoc>>>документ {}",getDocList().getRowCount());
	   }
	}
   
   /** 
    * разширено търсене на  - бутон "Изчисти"
    */
   public void actionClearDoc() {
	   setDocSelectedAllM(null);
	   setDocSelectedTmp(null);
	   initDocSearch(false);
	   periodR = null;
	   String tmp2 = (String) getAttributes().get("searchTehNomDoc"); 
	   if( !Objects.equals(tmp2, null)) {
		   getDocSearch().setTehNomer(tmp2); // ако е подадено търсене по техен номер - да не се позволява това поле да се променя!
	   }
	   if (LOGGER.isDebugEnabled()) {
		   LOGGER.debug("actionClearDoc>>>> ");
	   }
	}
   
  
   
   /**
    * Разширено търсене - единичен избор на документ
    */
   public void actionModalSelectDoc(Object[] row) {	 
	   if (LOGGER.isDebugEnabled()) {
	       LOGGER.debug("actionModalSelectDoc>>>> {}",row[0]);
	   }
	    if( row != null && row[0] != null) {
		   //връща обект SearchDoc
		    ValueExpression expr2 = getValueExpression("searchDocS");
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
				getDocSearch().setDocDateFrom(di[0]);
				getDocSearch().setDocDateTo(di[1]);		
	   	} else {
	   		getDocSearch().setDocDateFrom(null);
	   		getDocSearch().setDocDateTo(null);
		}
    }
	
	public void changeDate() { 
		this.setPeriodR(null);
	}

	
	
	/**
	 * Множествен избор на документи
	 */
	/**
	 * Избира всички редове от текущата страница
	 * @param event
	 */
	public void onRowSelectAll(ToggleSelectEvent event) {    
    	List<Object[]> tmpL = new ArrayList<>();
    	tmpL.addAll(getDocSelectedAllM());
    	if(event.isSelected()) {
    		onRowSelectAllAddItem(tmpL);
    	}else {
    		onRowSelectAllDelItem(tmpL);	
		}
		setDocSelectedAllM(tmpL);	
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("onToggleSelect->>");
		}
	}
		  
  /**
   *   Добавя избраните редове в общия масив
   * @param tmpL
   */
   private void onRowSelectAllAddItem(List<Object[]> tmpL ){
		for (Object[] obj : getDocSelectedTmp()) {
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
		List<Object[]> tmpLPageC =  getDocList().getResult();// rows from current page....    		
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
    		List<Object[]> tmpList =  getDocSelectedAllM();
    		
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
    				setDocSelectedAllM(tmpList);   
    			}
    		}
    	}	    	
    	if (LOGGER.isDebugEnabled()) {
    		LOGGER.debug("1 onRowSelectIil->>{}",getDocSelectedAllM().size());
    	}
    }
		 
		    
    /**
     * unselect one row
     * @param event
     */
    public void onRowUnselect(UnselectEvent<?> event) {
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
    		if (LOGGER.isDebugEnabled()) {
    			LOGGER.debug( "onRowUnselectIil->>{}",getDocSelectedAllM().size());
    		}
    	}
    }

    /**
     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга
     */
    public void   onPageUpdateSelected(){
    	if (getDocSelectedAllM() != null && !getDocSelectedAllM().isEmpty()) {
    		getDocSelectedTmp().clear();
    		getDocSelectedTmp().addAll(getDocSelectedAllM());
    	}	
    	if (LOGGER.isDebugEnabled()) {
    		LOGGER.debug( " onPageUpdateSelected->>{}",getDocSelectedTmp().size());
    	}
    }


   /**
    * Връща списъка с избрани преписки/дела 
    */
   public void actionConfirmSelected() {		
	   if (LOGGER.isDebugEnabled()) {
		   LOGGER.debug("actionSaveSelectedIil {}", getDocSelectedAllM().size());
	   }
		
	   //връща обект списък от SearchDoc
	    ValueExpression expr2 = getValueExpression("selectedDocList");
		ELContext ctx2 = getFacesContext().getELContext();
		if (expr2 != null) {
			expr2.setValue(ctx2, getDocSelectedAllM());
		}	
		actionClearDoc();
	 // извиква remoteCommnad - ако има такава....
		String remoteCommnad = (String) getAttributes().get("onComplete");
		if (remoteCommnad != null && !remoteCommnad.equals("")) {
			PrimeFaces.current().executeScript(remoteCommnad);
		}	
			
	}
   
   public String titleTehenNomer(String tehNom, Date tehDat) {
	   String tehNomFull="";
	   if(tehNom != null) {
		   tehNomFull = "Техен номер: "+tehNom;
	   }
	   if(tehDat != null) {
		   tehNomFull +="/"+DateUtils.printDate(tehDat);
	   }
	   return tehNomFull;
   }
	
	/** @return */
	public boolean isShowMe() {
		return (Boolean) getStateHelper().eval(PropertyKeys.SHOWME, false);
	}
	/** @param showMe */
	public void setShowMe(boolean showMe) {
		getStateHelper().put(PropertyKeys.SHOWME, showMe);
	}


	public DocSearch getDocSearch() {
		DocSearch eval = (DocSearch) getStateHelper().eval(PropertyKeys.DOCSEARCH, null);
		return eval != null ? eval : new DocSearch(getUserData().getRegistratura());
	}


	public void setDocSearch(DocSearch docSearch) {
		getStateHelper().put(PropertyKeys.DOCSEARCH, docSearch);
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

	
		public LazyDataModelSQL2Array getDocList() {
		return (LazyDataModelSQL2Array) getStateHelper().eval(PropertyKeys.DOCLIST, null);
	}


	public void setDocList(LazyDataModelSQL2Array docList) {
		getStateHelper().put(PropertyKeys.DOCLIST, docList);
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

	
	private SystemData getSystemData() {
		if (this.systemData == null) {
			this.systemData =  (SystemData) JSFUtils.getManagedBean("systemData");
		}
		return this.systemData;
	}


	@SuppressWarnings("unchecked")
	public List<Object[]> getDocSelectedTmp() {
		List<Object[]> eval = (List<Object[]>) getStateHelper().eval(PropertyKeys.DOCSELTMP, null);
		return eval != null ? eval : new ArrayList<>();		
	}


	public void setDocSelectedTmp(List<Object[]> docSelectedTmp) {
		getStateHelper().put(PropertyKeys.DOCSELTMP, docSelectedTmp);
	}


	@SuppressWarnings("unchecked")
	public List<Object[]> getDocSelectedAllM() {
		List<Object[]> eval = (List<Object[]>) getStateHelper().eval(PropertyKeys.DOCSEL, null);
		return eval != null ? eval : new ArrayList<>();
	}


	public void setDocSelectedAllM(List<Object[]> docSelectedAllM) {
		getStateHelper().put(PropertyKeys.DOCSEL, docSelectedAllM);
	}


	@SuppressWarnings("unchecked")
	public List<SystemClassif> getDocsVidClassif() {
		
		List<SystemClassif> eval = (List<SystemClassif>) getStateHelper().eval(PropertyKeys.DOCSVIDCLASSIF, null);
		return eval != null ? eval : new ArrayList<>();
	}


	public void setDocsVidClassif(List<SystemClassif> docsVidClassif) {
		getStateHelper().put(PropertyKeys.DOCSVIDCLASSIF, docsVidClassif);
	}


	@SuppressWarnings("unchecked")
	public List<SystemClassif> getDocsRegistriClassif() {
		List<SystemClassif> eval = (List<SystemClassif>) getStateHelper().eval(PropertyKeys.DOCSREGISTRICLASSIF, null);
		return eval != null ? eval : new ArrayList<>();
	}


	public void setDocsRegistriClassif(List<SystemClassif> docsRegistriClassif) {
		getStateHelper().put(PropertyKeys.DOCSREGISTRICLASSIF, docsRegistriClassif);
	}

	
	@SuppressWarnings("unchecked")
	public List<SelectItem> getTypeDocList() {		
		List<SelectItem>eval = (List<SelectItem>) getStateHelper().eval(PropertyKeys.DOCTYPEARR, null);
		return eval != null ? eval : new ArrayList<>();	
	}


	public void setTypeDocList(List<SelectItem> typeDocList) {
		getStateHelper().put(PropertyKeys.DOCTYPEARR, typeDocList);
	}

	public List<SelectItem> createSelectItemListType(Integer[] docTypeArr) {
		List<SelectItem> items = null;
		try {
			if(docTypeArr == null) {
				List<SystemClassif> listT = getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_DOC_TYPE,  new Date(),  getLang());
				items = new ArrayList<>(listT.size());
				for (SystemClassif x : listT) {
					items.add(new SelectItem(x.getCode(), x.getTekst()));
				}			
			} else {			
				items = new ArrayList<>(docTypeArr.length);
				for (Integer i : docTypeArr) {
					items.add(new SelectItem(i, getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DOC_TYPE, i, getLang(), new Date())));
				}
			}
			setTypeDocList(items);
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на логически списък - статуси - роля! ", e);
		}
		return items;
	}
}
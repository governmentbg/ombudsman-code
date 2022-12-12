package com.ib.omb.beans;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.faces.view.facelets.FaceletContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import org.primefaces.PrimeFaces;
import org.primefaces.component.export.PDFOptions;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DeloDAO;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.search.DeloSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
/**
 * Търсене на дело/преписка за актуализация или разглеждане.
 * @author s.marinov
 *
 */
@Named
@ViewScoped
public class DeloList  extends IndexUIbean   {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2989843357341602351L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DeloList.class);

	public static final String  DELOLISTTFORM = "deloListForm";	
	public static final String SELECTED_DELO = "selectedDelo";
	private Date decodeDate=new Date();

	private DeloSearch deloSearch;	              
	private Integer period=null;
	private Integer periodStat=null;
	
	private List<Object[]> deloSelectedAllM=new ArrayList<>();
	private List<Object[]> deloSelectedTmp=new ArrayList<>();
	
	private LazyDataModelSQL2Array deloList;       
	
	private ArrayList<SystemClassif> listVodSlujClassif;
	private boolean showRegistaturiList;
	private Integer fromPage=0;//aktualizaciq
	
	private List<SelectItem> typeList = new ArrayList<>();
	private List<SelectItem> statusList = new ArrayList<>();
	private Integer statusGroup=null;
	private Date statusDate=new Date();
	private boolean endTask; 					//  да се приключат ли задчите с приключване на преписката
	private boolean accessFinishTask; 				// true -  има право да приключва задачи
	private String triCheckStore = "0";
	private List<Integer> deloIdList;
	private List<SelectItem> registraturaList;
	private boolean isOkToRender; // да се показва ли бутон за пълнотекстово търсене
	
	@Inject
	private Flash flash;
	
	private boolean fillTextSearch = false;
	
	/** */
	@PostConstruct
	void initData() {
		
		FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
		String param = (String) faceletContext.getAttribute("isView");
		if (param!=null && param.equals("1")) {
			fromPage=1;//razglejdane
		}else {
			param = (String) faceletContext.getAttribute("isDeloPrereg");
			if (param!=null && param.equals("2")) {
				fromPage=2;//preregistraciq
			}else {
				param = (String) faceletContext.getAttribute("isSahranenie");
				if (param!=null && param.equals("3")) {
					fromPage=3;//TODO - kato kajat kav parametar sa slojili da go dobavq predvideno za sahranenie 
				}else {
					param = (String) faceletContext.getAttribute("isPrehvarlqneRegistratura");
					if (param!=null && param.equals("4")) {
						fromPage=4;////TODO - kato kajat kav parametar sa slojili da go dobavq predvideno za prehvarlqne v druga registratura
					}else {
					param = (String) faceletContext.getAttribute("isView");
						if (param!=null && param.equals("5")) {
							fromPage=5;////опис на том LM
						}
					}
				}
			}
		}
		actionClearDelo();
		try {

			if (this.typeList.isEmpty()) {
				if (fromPage==2) {
					
					this.typeList = null;
							// могат да се пререгистрират само обикновени преписки - затова в случая типа не трябва да с евижда на екрана
//							// включва всички типове, без "номенклатурно дело" 
//							createItemsList(false, OmbConstants.CODE_CLASSIF_DELO_TYPE, deloSearch.getDeloDateTo() == null ? new Date() : deloSearch.getDeloDateTo(), true)
//							.stream()
//							.filter(item -> (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM)
//							.collect(Collectors.toList());
					
				}else {
					this.typeList = createItemsList(false,  OmbConstants.CODE_CLASSIF_DELO_TYPE, deloSearch.getDeloDateTo() == null ? this.decodeDate : deloSearch.getDeloDateTo(), true);
				}
			}
			
			// Статусите - винаги да са пълния списък
			this.statusList = 	createItemsList(false, OmbConstants.CODE_CLASSIF_DELO_STATUS, new Date(), true);
			
			//ако няма дефинитивно право "Право да поддържа учрежденски архив",да не може да търси по тези два статуса в режим редакция и съхранение
			if(!getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_UA) && (fromPage==0 || fromPage==3)) {
				List<SelectItem> forDelete = new ArrayList<>();
				for(SelectItem item: statusList) {
					if(item.getValue().equals(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_UA) || item.getValue().equals(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_DA)) {
						forDelete.add(item);
						if(forDelete.size()==2) {
							break;
						}
					}
				}
				
				for(SelectItem item: forDelete) {
					statusList.remove(item);
				}
			}
			
			setRegistraturaList(createItemsList(true, OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS, this.decodeDate, false));
			
			//String fullTextSetting = getSystemData().getSettingsValue("general.fulltextsearch.enabled");
			//isOkToRender = !("0".equals(fullTextSetting)) ? true : false;
			isOkToRender = false;
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на документ! ", e);
		}
	}

	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClearDelo() {
		deloList=null;
		period=null;
		if(fromPage==1) {
			deloSearch=new DeloSearch(null);
			showRegistaturiList = getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW) ||
				getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT);
		}else {
			deloSearch=new DeloSearch(getUserData(UserData.class).getRegistratura());
		}
		
		if (deloSelectedTmp!=null) {
			//не виждам кое го прави null но по някаква причина в справки е след търсене.
			 deloSelectedTmp.clear();	
		}
		
		deloSelectedAllM.clear();
		triCheckStore = "0";
		listVodSlujClassif = new ArrayList<>();
		periodStat = null;
		fillTextSearch = false;
	}
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
	 */
	public void postProcessXLS(Object delo) {
		
		String title = getMessageResourceString(LABELS, "delo.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(delo, title, dopInfoReport() , null, null);		
     
	}
	

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
	 */
	public void preProcessPDF(Object delo)  {
		try{
			
			String title = getMessageResourceString(LABELS, "delo.reportTitle");		
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
	
	
	
	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	public void changePeriod () {
		
    	if (this.period != null) {
			Date[] din;
			din = DateUtils.calculatePeriod(this.period);
			deloSearch.setDeloDateFrom(din[0]);
			deloSearch.setDeloDateTo(din[1]);		
    	} else {
    		deloSearch.setDeloDateFrom(null);
			deloSearch.setDeloDateTo(null);
		}
    }
	
	public void changeDate() { 
		this.setPeriod(null);
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
	
	public void changeDateStat() { 
		this.setPeriodStat(null);
	}
	
	  
	public void actionSearchDelo(){
		
		deloSelectedAllM.clear();
		if (deloSelectedTmp != null) {
			deloSelectedTmp.clear();
		}
		
		 if(fromPage.equals(3)) {
			deloSearch.setEnableStorageSearch(true);
			deloSearch.setUseDost(false);
			if("0".equals(triCheckStore)) {
				deloSearch.setStorage(null);
			}else if("1".equals(triCheckStore)){
				deloSearch.setStorage(true);
			}else if("2".equals(triCheckStore)){
				deloSearch.setStorage(false);
			}
		 } 
		 deloSearch.buildQueryDeloList(getUserData(), fromPage); // TODO да се определи режима на търсене
		 
		 
		 try {
			 String param1 = getSystemData().getSettingsValue("delo.journalSearchDeloDoc"); // да се журналира ли търсенето
			 if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
				// запис в журнала
				JPA.getUtil().runInTransaction(() -> SearchUtils.auditSearch(deloSearch, SysConstants.CODE_DEIN_SEARCH, getUserData().getUserId()));
			}
		 } catch (BaseException e) {
				LOGGER.error("Грешка при журналиране на търсене на документи! ", e);
		 } 
		 
		 
		 deloList = new LazyDataModelSQL2Array(deloSearch, "a2 desc"); 

	}	
	
	
	public void actionSearchDeloFullText() {
		deloSelectedAllM.clear();
		if (deloSelectedTmp != null) {
			deloSelectedTmp.clear();
		}
		
		 if(fromPage.equals(3)) {
			deloSearch.setEnableStorageSearch(true);
			deloSearch.setUseDost(false);
			if("0".equals(triCheckStore)) {
				deloSearch.setStorage(null);
			}else if("1".equals(triCheckStore)){
				deloSearch.setStorage(true);
			}else if("2".equals(triCheckStore)){
				deloSearch.setStorage(false);
			}
		 }
		 deloSearch.buildQueryDeloListFullText(getUserData(), fromPage);
		 deloList = new LazyDataModelSQL2Array(deloSearch, "a1 desc"); 
	}
	
	
	public String actionGoto(int i, Object[] row) {
		
		Integer idObj = (this.deloSearch.getDvijNotReturned()) ? ((Number) row[11]).intValue() : ((Number) row[0]).intValue();
		
		String result = "";
		if(i == 0) {
			result = "deloEdit.jsf?faces-redirect=true&idObj=" + idObj;
		} else if(i == 1){
			result = "deloView.xhtml?faces-redirect=true&idObj=" + idObj;
		}else if(i == 2){
			fromPage=5;//opis
			result = "opis.jsf?faces-redirect=true&idObj=" + idObj;
		}else if(i == 3){
			result = "deloPrereg.jsf?faces-redirect=true&idObj=" + idObj;
		}
		return result;
	}
	
	public String actionGotoStorage() {
		if(deloSelectedAllM!=null && !deloSelectedAllM.isEmpty()) {
			deloIdList = new ArrayList<Integer>();
			for(Object[] item : deloSelectedAllM) {
				deloIdList.add(SearchUtils.asInteger(item[0]));
			}			
		}
		
		HttpSession session = (HttpSession) JSFUtils.getExternalContext().getSession(false);
		session.setAttribute("deloIdList", deloIdList);
		return "deloStorage.jsf?faces-redirect=true";
	}
	
	/****************************************** 	METODI NA BUTONITE*****************************/
	
	public void actionVlaganeSelected() {
		//Като кажат какво да се сложи
	}
	
	public String actionPredavaneSelected() {
		// DA NE SE PREDAVAT NOM DELA
		
		flash.put(DeloList.SELECTED_DELO, this.getDeloSelectedAllM());
		return "deloGrupovoPredavane.jsf?faces-redirect=true";
	}
	
	public String actionVrashtaneSelected() {
		// DA NE SE VRASHTAT NOM DELA
		
		flash.put(DeloList.SELECTED_DELO, this.getDeloSelectedAllM());
		return "deloGrupovoVrashtane.jsf?faces-redirect=true";
	}
	
	public void actionOpenModalStatusChange() {
		
		String  cmdStr = "PF('modalStatusChange').show();";
		PrimeFaces.current().executeScript(cmdStr);
		
	}
	public void actionChangeStatusSelected() {
		
		try {
			boolean mess=false;
			ArrayList<Object[]> toProcess=new ArrayList<Object[]>();
			for (Object[] row : deloSelectedAllM) {
				if (!Objects.equals(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_DA, SearchUtils.asInteger(row[4])) && !Objects.equals(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_UA, SearchUtils.asInteger(row[4]))) {
					toProcess.add(row);
				}else {
					mess=true;
				}
			}
			
			if (mess) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, "Ако между избраните от Вас преписки има такива в статус \"предадена в УА\" или \"предадена в ДА\", техният статус няма да бъде променен.");
			}

			JPA.getUtil().runInTransaction(() -> new DeloDAO(getUserData()).groupStatusChange(toProcess, this.statusGroup, this.statusDate, this.endTask, getSystemData())); 
		
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );
			
			this.deloSelectedAllM.clear();
			deloSelectedTmp.clear();
			this.deloList.loadCountSql();
			
			
			String  cmdStr = "PF('modalStatusChange').hide();";
			PrimeFaces.current().executeScript(cmdStr);
			endTask = false;
			statusGroup = null;
		} catch (ObjectInUseException e) {
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			LOGGER.error(e.getMessage(), e);
		} catch (BaseException e) {			
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		} 
		
		
	}
	
	public void actionSahranqvaneSelected() {
		//Като кажат какво да се сложи
	}
	
	public void actionDeleteSelected() {
		StringBuilder msg = new StringBuilder();
		DeloDAO deloDao = new DeloDAO(getUserData());
	 
		for (Object[] row : deloSelectedAllM) {
			try {
				Integer deloId = SearchUtils.asInteger(row[0]);
				JPA.getUtil().runInTransaction(() -> {
					Delo d = deloDao.findById(deloId);
					deloDao.delete(d);
					
					if (msg.length() > 0) {
						msg.append(", ");
					}
					msg.append(d.getIdentInfo());
				}); 
				
			} catch (ObjectInUseException e) {
				LOGGER.error("Групово изтриване на преписки -{}", e.getMessage());
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} catch (BaseException e) {
				LOGGER.error("Грешка при групово изтриване на преписки от базата данни!", e);
				JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			} 
		}
		
		if (msg.length() > 0) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"), "Преписки: " + msg + ".");
		}

	 
		this.deloSelectedAllM.clear();
		deloSelectedTmp.clear();
		this.deloList.loadCountSql();
	}
	
	
	/*
	 * Множествен избор на дело/преписка
	 */
	/**
	 * Избира всички редове от текущата страница
	 * @param eventT
	 */
	  public void onRowSelectAll(ToggleSelectEvent eventT) {    
		ArrayList<Object[]> tmpList = new ArrayList<>();
    	tmpList.addAll(getDeloSelectedAllM());
    	if(eventT.isSelected()) {
    		for (Object[] object : getDeloSelectedTmp()) {
    			if(object != null && object.length > 0) {
	    			boolean bo = true;
	    			Integer l2 = Integer.valueOf(object[0].toString());
	    			for (Object[] j : tmpList) { 
	    				Integer l1 = Integer.valueOf(j[0].toString());        			
	    	    		if(l1.equals(l2)) {    	    			
	    	    			bo = false;
	    	    			break;
	    	    		}
	        		}
	    			if(bo) {
	    				tmpList.add(object);
	    			}
    			}
    		}    		
    	}else {
	    	List<Object[]> tmpLPageClass =  getDeloList().getResult();// rows from current page....    		
			for (Object[] object : tmpLPageClass) {
				if(object != null && object.length > 0) {
					Integer l2 = Integer.valueOf(object[0].toString());
					for (Object[] j : tmpList) { 
						Integer l1 = Integer.valueOf(j[0].toString());        			
			    		if(l1.equals(l2)) {    	    			
			    			tmpList.remove(j);
			    			break;
			    		}	
		    		}
				}
			}    		
		}
		setDeloSelectedAllM(tmpList);	    	
		LOGGER.debug("onToggleSelect->>");	    	   
	}
		    
    /** 
     * Select one row
     * @param eventS
     */
    @SuppressWarnings("rawtypes")
	public void onRowSelect(SelectEvent eventS) {    	
    	if(eventS!=null  && eventS.getObject()!=null) {
    		List<Object[]> tmpLst =  getDeloSelectedAllM();
    		
    		Object[] object = (Object[]) eventS.getObject();
    		if(object != null && object.length > 0) {
	    		boolean bo = true;
	    		Integer l3 = Integer.valueOf(object[0].toString());
				for (Object[] j : tmpLst) { 
					Integer l1 = Integer.valueOf(j[0].toString());        			
		    		if(l1.equals(l3)) {
		    			bo = false;
		    			break;
		    		}
		   		}
				if(bo) {
					tmpLst.add(object);
					setDeloSelectedAllM(tmpLst);   
				}
    		}
    	}	    	
    	LOGGER.error(String.format("1 onRowSelectIil->> %s", getDeloSelectedAllM().size()));
    }
		 
		    
    /**
     * unselect one row
     * @param eventU
     */
    @SuppressWarnings("rawtypes")
	public void onRowUnselect(UnselectEvent eventU) {
    	if(eventU!=null  && eventU.getObject()!=null) {
    		Object[] object = (Object[]) eventU.getObject();
    		ArrayList<Object[] > tmpLst = new ArrayList<>();
    		tmpLst.addAll(getDeloSelectedAllM());
    		for (Object[] j : tmpLst) {
    			if(j != null && j.length > 0 
        				&& object != null && object.length > 0) {
	    			Integer l1 = Integer.valueOf(j[0].toString());
	    			Integer l2 = Integer.valueOf(object[0].toString());
		    		if(l1.equals(l2)) {
		    			tmpLst.remove(j);
		    			setDeloSelectedAllM(tmpLst);
		    			break;
		    		}
    			}
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
    }


	
	
	
	
	
	
	
	/**
	 * подзаглавие за екпсорта - .............
	 */
	public Object[] dopInfoReport() {
		Object[] dopInf = null;
		dopInf = new Object[2];
		if(deloSearch.getDeloDateFrom() != null && deloSearch.getDeloDateTo() != null) {
			dopInf[0] = "период: "+DateUtils.printDate(deloSearch.getDeloDateFrom()) + " - "+ DateUtils.printDate(deloSearch.getDeloDateTo());
		} 
	
		return dopInf;
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


	public ArrayList<SystemClassif> getListVodSlujClassif() {
		return listVodSlujClassif;
	}


	public void setListVodSlujClassif(ArrayList<SystemClassif> listVodSlujClassif) {
		this.listVodSlujClassif = listVodSlujClassif;
	}


	public Integer getPeriodStat() {
		return periodStat;
	}


	public void setPeriodStat(Integer periodStat) {
		this.periodStat = periodStat;
	}

	public boolean isShowRegistaturiList() {
		return this.showRegistaturiList;
	}

	public List<SelectItem> getTypeList() {
		return typeList;
	}

	public void setTypeList(List<SelectItem> typeList) {
		this.typeList = typeList;
	}


	public List<SelectItem> getStatusList() {
		return statusList;
	}


	public void setStatusList(List<SelectItem> statusList) {
		this.statusList = statusList;
	}

	public Integer getStatusGroup() {
		return statusGroup;
	}

	public void setStatusGroup(Integer statusGroup) {
		this.statusGroup = statusGroup;
	}

	public Date getStatusDate() {
		return statusDate;
	}

	public void setStatusDate(Date statusDate) {
		this.statusDate = statusDate;
	}

	public String getTriCheckStore() {
		return triCheckStore;
	}

	public void setTriCheckStore(String triCheckStore) {
		this.triCheckStore = triCheckStore;
	}

	public List<Integer> getDeloIdList() {
		return deloIdList;
	}

	public void setDeloIdList(List<Integer> deloIdList) {
		this.deloIdList = deloIdList;
	}
	
	public List<SelectItem> getRegistraturaList() {
		return registraturaList;
	}

	public void setRegistraturaList(List<SelectItem> registraturaList) {
		this.registraturaList = registraturaList;
	}

	public boolean getIsOkToRender() {
		return isOkToRender;
	}

	public void setIsOkToRender(boolean isOkToRender) {
		this.isOkToRender = isOkToRender;
	}
	
	public void actionSearchNew() {
		if(fillTextSearch) {
			actionSearchDeloFullText();
		} else {
			actionSearchDelo();
		}
	}

	public boolean isFillTextSearch() {
		return fillTextSearch;
	}

	public void setFillTextSearch(boolean fillTextSearch) {
		this.fillTextSearch = fillTextSearch;
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
	
	/**
	 * проверка за настройката и дали потребителя има нужните права
	 */
	public void actionEndDelo() {
		accessFinishTask = false;
		if (Objects.equals(statusGroup,OmbConstants.CODE_ZNACHENIE_DELO_STATUS_COMPLETED)) {
			Integer regId = getUserData(UserData.class).getRegistratura();
			try {
				// При приключване на преписка се приключват задачите към нейните документи и вложените преписки 
				Integer s1 = ((SystemData) getSystemData()).getRegistraturaSetting(regId, OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_10);
				if( Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_DA) &&
					getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_COMPLETE_REGISTRATURA_TASKS)) {
					accessFinishTask = true; 
				}
				
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при извичане на настройка CODE_ZNACHENIE_REISTRATURA_SETTINGS_10 на регистратура: {} ", regId, e);
			}
		}
	}
}
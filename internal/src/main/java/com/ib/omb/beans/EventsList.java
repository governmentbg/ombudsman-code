package com.ib.omb.beans;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.EventDAO;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.utils.DateUtils;

@Named
@ViewScoped
public class EventsList  extends IndexUIbean   {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4417787693962181619L;
	private static final Logger LOGGER = LoggerFactory.getLogger(EventsList.class);

	private Date decodeDate=new Date();
	private transient EventDAO eventDao;
	private LazyDataModelSQL2Array eventList; 
	private List<Integer> eventCodeList;
	private List<SystemClassif> eventsVidClassif; 
	private Integer period;
	private Date dateFrom;
	private Date dateTo; 
	private String eventInfo;
	private Integer organizator;
	private List<Integer> codeRefList;
	private List<SystemClassif> codeRefClassif;
	private List<Integer> resourcesList;
	private List<SystemClassif> resoursesClassif;
	private boolean onlyCurrent = true;
	private Integer userId;
	private boolean admin = false; // Администратор на събития
	private boolean fromDb = false; //ако страницата се вика от работния плот
	
	@PostConstruct
	void initData() {
		
	
		eventDao = new EventDAO(getUserData());
		userId = getUserData().getUserId();
		admin  = getUserData().hasAccess(OmbConstants.CODE_CLASSIF_BUSINESS_ROLE, OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_EVENT_ADMIN);
	
		String fromDbStr =  JSFUtils.getRequestParameter("fromdb");
		if(fromDbStr != null && !fromDbStr.contentEquals("null") && fromDbStr.contentEquals("1")) {		
				fromDb = true;
			 	codeRefList = new ArrayList<>();		 	
			 	codeRefList.add(getUserData(UserData.class).getUserAccess());
			 	actionSearch();
		}else {
			 getUserData(UserData.class).checkPageAccess(105);//защото всеки има достъп през работния плот
			
		}
			
	}

	public void actionSearch() {
		try {
			
			
			SelectMetadata smd = this.eventDao.createSelectFilter(getUserData(), eventCodeList, dateFrom, dateTo, eventInfo, organizator, codeRefList, resourcesList,onlyCurrent);
				
			this.eventList = new LazyDataModelSQL2Array(smd, "A0");						
		}catch (Exception e) {
			LOGGER.error("Грешка при зареждане на календар на събития!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void actionClear() {
		
		period = null;
		onlyCurrent = true;
		eventList = null;		
		eventCodeList = null;
		eventsVidClassif = null; 
		dateFrom = null;
		dateTo = null; 
		eventInfo = "";
		organizator = null;
		codeRefList = null;
		codeRefClassif = null;
		resourcesList = null;
		resoursesClassif =null;	
	}
		
	public void changeDate() { 
		this.setPeriod(null);
	}
	
	public void changePeriod () {
		
    	if (this.period != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.period);
			dateFrom = di[0];
			dateTo = di[1];		
    	} else {
    		dateFrom = null;
    		dateTo = null;			
		}
    }
	
	public void changeDateReg() { 
		this.setPeriod(null);
		eventList = null;
	}
	
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "eventsList.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport() , null, null);		
     
	}
	
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "eventsList.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title,  dopInfoReport(), null, null);		
						
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
	public Object[] dopInfoReport() {
		Object[] dopInf = null;
		dopInf = new Object[2];
		if(dateFrom != null && dateTo != null) {
			dopInf[0] = "период: "+DateUtils.printDate(dateFrom) + " - "+ DateUtils.printDate(dateTo);
		} 
	
		return dopInf;
	}

	
	public String actionGoto(int i, Object[] row) {
		Integer idObj = ((Number) row[0]).intValue();
		String result = "";
		
		if(i==0) {
			result = "eventEdit.xhtml?faces-redirect=true&idObj=" + idObj;
		}else if(i == 1) {
			result = "eventView.xhtml?faces-redirect=true&idObj=" + idObj;
		}
		return result;
	}

	public Integer getPeriod() {
		return period;
	}

	public void setPeriod(Integer period) {
		this.period = period;
	}

	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}

	public LazyDataModelSQL2Array getEventList() {
		return eventList;
	}

	public void setEventList(LazyDataModelSQL2Array eventList) {
		this.eventList = eventList;
	}

	public List<Integer> getEventCodeList() {
		return eventCodeList;
	}

	public void setEventCodeList(List<Integer> eventCodeList) {
		this.eventCodeList = eventCodeList;
	}

	public Date getDateFrom() {
		return dateFrom;
	}

	public void setDateFrom(Date dateFrom) {
		this.dateFrom = dateFrom;
	}

	public Date getDateTo() {
		return dateTo;
	}

	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}

	public String getEventInfo() {
		return eventInfo;
	}

	public void setEventInfo(String eventInfo) {
		this.eventInfo = eventInfo;
	}

	public Integer getOrganizator() {
		return organizator;
	}

	public void setOrganizator(Integer organizator) {
		this.organizator = organizator;
	}

	public List<Integer> getCodeRefList() {
		return codeRefList;
	}

	public void setCodeRefList(List<Integer> codeRefList) {
		this.codeRefList = codeRefList;
	}

	public List<Integer> getResourcesList() {
		return resourcesList;
	}

	public void setResourcesList(List<Integer> resourcesList) {
		this.resourcesList = resourcesList;
	}

	public List<SystemClassif> getEventsVidClassif() {
		return eventsVidClassif;
	}

	public void setEventsVidClassif(List<SystemClassif> eventsVidClassif) {
		this.eventsVidClassif = eventsVidClassif;
	}

	public boolean isOnlyCurrent() {
		return onlyCurrent;
	}

	public void setOnlyCurrent(boolean onlyCurrent) {
		this.onlyCurrent = onlyCurrent;
	}

	public List<SystemClassif> getCodeRefClassif() {
		return codeRefClassif;
	}

	public void setCodeRefClassif(List<SystemClassif> codeRefClassif) {
		this.codeRefClassif = codeRefClassif;
	}

	public List<SystemClassif> getResoursesClassif() {
		return resoursesClassif;
	}

	public void setResoursesClassif(List<SystemClassif> resoursesClassif) {
		this.resoursesClassif = resoursesClassif;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public boolean isAdmin() {
		return admin;
	}

	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	public boolean isFromDb() {
		return fromDb;
	}

	public void setFromDb(boolean fromDb) {
		this.fromDb = fromDb;
	}
	
}
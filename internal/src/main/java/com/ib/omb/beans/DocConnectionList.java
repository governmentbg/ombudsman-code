package com.ib.omb.beans;

import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDocDAO;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;

@Named
@ViewScoped
public class DocConnectionList  extends IndexUIbean   {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5221195703372565169L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DocConnectionList.class);

	private Date decodeDate=new Date();
	private transient DocDocDAO docDao;
	private LazyDataModelSQL2Array docList;  
	private List<SelectItem>	  docTypeList;
	
	//Първи документ
	private Integer periodFirst=null;
	private Date dateFromFirst;
	private Date dateToFirst;	
	private Integer[] docTypeArrFirst	= new Integer[3];
	private List<Integer>	docVidListFirst;
	private List<SystemClassif> docsVidClassifFirst;
	private String otnosnoFirst;
	

	//Връзка
	private Integer periodConn=null;
	private Date dateFromConn;
	private Date dateToConn;
	private List<Integer> vid;
	private List<SystemClassif> vidClassif;

	//Втори документ
	private Integer periodSecond=null;
	private Date dateFromSecond;
	private Date dateToSecond;	
	private Integer[] docTypeArrSecond	= new Integer[3];
	private List<Integer>	docVidListSecond;
	private List<SystemClassif> docsVidClassifSecond;
	private String otnosnoSecond;
	
	
	
	/** */
	@PostConstruct
	void initData() {
				
		try {
			setDocDao(new DocDocDAO(getUserData()));
			docTypeList = createItemsList(false, OmbConstants.CODE_CLASSIF_DOC_TYPE, this.decodeDate, true);
		} catch (DbErrorException | UnexpectedResultException e) {
			LOGGER.error("Грешка при инициализиране на филтър за търсене на свързаност на документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} 
	}

	public void actionSearch() {
	
		try {				
			SelectMetadata smd = this.docDao.createSelectReport((UserData) getUserData(), dateFromConn, dateToConn, vid,
					dateFromFirst, dateToFirst, docTypeArrFirst, docVidListFirst, otnosnoFirst ,
					dateFromSecond, dateToSecond, docTypeArrSecond, docVidListSecond, otnosnoSecond);	
			String defaultSortColumn = "A0";	
			this.docList = new LazyDataModelSQL2Array(smd, defaultSortColumn);						
		}catch (Exception e) {
			LOGGER.error("Грешка при търсене на свързани документи!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void actionClear() {
		dateFromConn= null;
		dateToConn = null;
		vid = null;
		dateFromFirst = null;
		dateToFirst = null;
		docTypeArrFirst = null;
		docVidListFirst = null;
		dateFromSecond = null;
		dateToSecond = null;
		docTypeArrSecond = null;
		docVidListSecond = null;
		docsVidClassifFirst = null;
		docsVidClassifSecond = null;
		vidClassif = null;
		periodFirst = null;
		periodConn = null;
		periodSecond = null;
		otnosnoFirst = "";
		otnosnoSecond = "";
		this.docList = null;
	}
	
	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	public void changePeriod () {
		
    	if (this.periodFirst != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodFirst);
			dateFromFirst = di[0];
			dateToFirst = di[1];		
    	} else {
    		dateFromFirst = null;
    		dateToFirst = null;			
		}
    }
	
	public void changeDate() { 
		this.setPeriodFirst(null);
	}
	
public void changePeriodConn () {
		
    	if (this.periodConn != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodConn);
			dateFromConn = di[0];
			dateToConn = di[1];		
    	} else {
    		dateFromConn = null;
    		dateToConn = null;			
		}
    }

	public void changePeriodSecond () {
		
		if (this.periodSecond != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodSecond);
			dateFromSecond = di[0];
			dateToSecond = di[1];		
		} else {
			dateFromSecond = null;
			dateToSecond = null;			
		}
	}
	
	public void changeDateSecond() { 
		this.setPeriodSecond(null);
	}
	
	public void changeDateConn() { 
		this.setPeriodConn(null);
	}
	
	public String actionGoto( Integer idObj) {
		
		return "docView.xhtml?faces-redirect=true&idObj=" + idObj;
	}

	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "docConnection.titleExport");		  
    	new CustomExpPreProcess().postProcessXLS(document, title,null, null, null);		
     
	}
	
	
	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}



	public LazyDataModelSQL2Array getDocList() {
		return docList;
	}

	public void setDocList(LazyDataModelSQL2Array docList) {
		this.docList = docList;
	}

	public DocDocDAO getDocDao() {
		return docDao;
	}

	public void setDocDao(DocDocDAO docDao) {
		this.docDao = docDao;
	}

	public Integer getPeriodSecond() {
		return periodSecond;
	}

	public void setPeriodSecond(Integer periodSecond) {
		this.periodSecond = periodSecond;
	}

	public Integer getPeriodFirst() {
		return periodFirst;
	}

	public void setPeriodFirst(Integer periodFirst) {
		this.periodFirst = periodFirst;
	}

	public Date getDateFromFirst() {
		return dateFromFirst;
	}

	public void setDateFromFirst(Date dateFromFirst) {
		this.dateFromFirst = dateFromFirst;
	}

	public Date getDateToFirst() {
		return dateToFirst;
	}

	public void setDateToFirst(Date dateToFirst) {
		this.dateToFirst = dateToFirst;
	}

	public List<SelectItem> getDocTypeList() {
		return docTypeList;
	}

	public void setDocTypeList(List<SelectItem> docTypeList) {
		this.docTypeList = docTypeList;
	}

	public Integer[] getDocTypeArrFirst() {
		return docTypeArrFirst;
	}

	public void setDocTypeArrFirst(Integer[] docTypeArrFirst) {
		this.docTypeArrFirst = docTypeArrFirst;
	}

	public List<Integer> getDocVidListFirst() {
		return docVidListFirst;
	}

	public void setDocVidListFirst(List<Integer> docVidListFirst) {
		this.docVidListFirst = docVidListFirst;
	}

	public List<SystemClassif> getDocsVidClassifFirst() {
		return docsVidClassifFirst;
	}

	public void setDocsVidClassifFirst(List<SystemClassif> docsVidClassifFirst) {
		this.docsVidClassifFirst = docsVidClassifFirst;
	}

	public String getOtnosnoFirst() {
		return otnosnoFirst;
	}

	public void setOtnosnoFirst(String otnosnoFirst) {
		this.otnosnoFirst = otnosnoFirst;
	}

	public Integer getPeriodConn() {
		return periodConn;
	}

	public void setPeriodConn(Integer periodConn) {
		this.periodConn = periodConn;
	}

	public Date getDateFromConn() {
		return dateFromConn;
	}

	public void setDateFromConn(Date dateFromConn) {
		this.dateFromConn = dateFromConn;
	}

	public Date getDateToConn() {
		return dateToConn;
	}

	public void setDateToConn(Date dateToConn) {
		this.dateToConn = dateToConn;
	}

	public List<Integer> getVid() {
		return vid;
	}

	public void setVid(List<Integer> vid) {
		this.vid = vid;
	}

	public List<SystemClassif> getVidClassif() {
		return vidClassif;
	}

	public void setVidClassif(List<SystemClassif> vidClassif) {
		this.vidClassif = vidClassif;
	}

	public Date getDateFromSecond() {
		return dateFromSecond;
	}

	public void setDateFromSecond(Date dateFromSecond) {
		this.dateFromSecond = dateFromSecond;
	}

	public Date getDateToSecond() {
		return dateToSecond;
	}

	public void setDateToSecond(Date dateToSecond) {
		this.dateToSecond = dateToSecond;
	}

	public Integer[] getDocTypeArrSecond() {
		return docTypeArrSecond;
	}

	public void setDocTypeArrSecond(Integer[] docTypeArrSecond) {
		this.docTypeArrSecond = docTypeArrSecond;
	}

	public List<Integer> getDocVidListSecond() {
		return docVidListSecond;
	}

	public void setDocVidListSecond(List<Integer> docVidListSecond) {
		this.docVidListSecond = docVidListSecond;
	}

	public List<SystemClassif> getDocsVidClassifSecond() {
		return docsVidClassifSecond;
	}

	public void setDocsVidClassifSecond(List<SystemClassif> docsVidClassifSecond) {
		this.docsVidClassifSecond = docsVidClassifSecond;
	}

	public String getOtnosnoSecond() {
		return otnosnoSecond;
	}

	public void setOtnosnoSecond(String otnosnoSecond) {
		this.otnosnoSecond = otnosnoSecond;
	}
}
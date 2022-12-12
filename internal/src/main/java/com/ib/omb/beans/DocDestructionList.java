package com.ib.omb.beans;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

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
import com.ib.omb.db.dao.DocProtocolDAO;
import com.ib.omb.system.UserData;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.BaseException;
import com.ib.system.utils.DateUtils;

@Named
@ViewScoped
public class DocDestructionList extends IndexUIbean   {

	/**
	 * Списък с протоколи за унищожаване на документи
	 * 
	 */
	private static final long serialVersionUID = 6129774408749824460L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocDestructionList.class);
	
	private Integer period;	
	private Date dateFrom;
	private Date dateTo;
	private boolean onlyWorkProt = false;
	private Integer docVid;
	
	private String rnWorkProt;
	private boolean	rnWorkProtEQ;	// ако е true се търси по пълно съвпадение по номер на работен протокол
	
	private Integer periodOficProt;	
	private Date dateFromOficProt;
	private Date dateToOficProt;
	private String rnOficProt;
	private boolean	rnOficProtEQ;
	private String incRnDoc;
	private Date incDocDate;
	
	private LazyDataModelSQL2Array docsDestrucList;
	
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct!!!");
	
		try {
			
			String vidDoc = null;
			
			vidDoc = getSystemData().getSettingsValue("delo.destructionProtocol"); // 7
			
			if (vidDoc == null) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "docDestructionList.noSystemSettings"));
				return;
			} else {
				this.docVid = Integer.valueOf(vidDoc);
			}
					
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на вид на документ 'протокол за унищожаване на документи'! ", e);		
		}
	}
	
	/** 
	 * Списък с протоколи за унищожаване на документи по зададени критерии 
	 * 
	 */
	public void actionSearch(){
		
		SelectMetadata smd = new DocProtocolDAO(1, getUserData()).createSelectDestructProtocol(getUserData(UserData.class).getRegistratura(), this.docVid, this.dateFrom, this.dateTo, this.onlyWorkProt, 
				this.rnWorkProt, this.rnWorkProtEQ, this.dateFromOficProt, this.dateToOficProt, this.rnOficProt, this.rnOficProtEQ, this.incRnDoc, this.incDocDate);
		String defaultSortColumn = "a0";
		this.docsDestrucList = new LazyDataModelSQL2Array(smd, defaultSortColumn);		
	} 
	
	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClear() {		
		
		changePeriod();
		this.period = null;
		this.dateFrom = null;
		this.dateTo = null;
		this.onlyWorkProt = false;
		this.rnWorkProt = null;
		this.rnWorkProtEQ = false;
		
		this.periodOficProt = null;
		this.dateFromOficProt = null;
		this.dateToOficProt = null;
		this.rnOficProt = null;
		this.rnOficProtEQ = false;
		this.incRnDoc = null;
		this.incDocDate = null;
		
		this.docsDestrucList = null;
	}
	
	public String actionGoto(Integer idObj) {
		
		return "docDestruction.jsf?faces-redirect=true&idObj=" + idObj;
	}
	
	public String actionGotoNew() {
		
		return "docDestruction.jsf?faces-redirect=true";	
	}
	
	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	public void changePeriod() {
		
    	if (this.period != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.period);
			setDateFrom(di[0]);
			setDateTo(di[1]);				
    	} else {
    		setDateFrom(null);
    		setDateTo(null);
		}
    }
	
	public void changeDate() { 
		this.setPeriod(null);
	}
	
	public void changePeriodOficProt() {
		
    	if (this.periodOficProt != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodOficProt);
			setDateFromOficProt(di[0]);
			setDateToOficProt(di[1]);				
    	} else {
    		setDateFromOficProt(null);
    		setDateToOficProt(null);
		}
    }
	
	public void changeDateOficProt() { 
		this.setPeriodOficProt(null);
	}
	
/******************************** EXPORTS **********************************/
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за протокола за унищожаване на документи
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "docDestructionList.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfo(), null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за протокола за унищожаване на документи
	 */
	public void preProcessPDF(Object document)  {
		
		try {
			
			String title = getMessageResourceString(LABELS, "docDestructionList.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title, dopInfo(), null, null);		
						
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
	
	
	/**
	 * подзаглавие за експорта 
	 */
	public Object[] dopInfo() {
		
		Object[] dopInf = null;
		dopInf = new Object[2];
		
		if(this.dateFrom != null && this.dateTo != null) {
			dopInf[0] = "период на работния протокол: " + DateUtils.printDate(this.dateFrom) + " - "+ DateUtils.printDate(this.dateTo);
		} 
		
		if(this.dateFromOficProt != null && this.dateToOficProt != null) {
			dopInf[1] = "период на официалния протокол: " + DateUtils.printDate(this.dateFromOficProt) + " - "+ DateUtils.printDate(this.dateToOficProt);
		} 
	
		return dopInf;
	}

	public Integer getPeriod() {
		return period;
	}

	public void setPeriod(Integer period) {
		this.period = period;
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

	public boolean isOnlyWorkProt() {
		return onlyWorkProt;
	}

	public void setOnlyWorkProt(boolean onlyWorkProt) {
		this.onlyWorkProt = onlyWorkProt;
	}

	public Integer getDocVid() {
		return docVid;
	}

	public void setDocVid(Integer docVid) {
		this.docVid = docVid;
	}

	public String getRnWorkProt() {
		return rnWorkProt;
	}

	public void setRnWorkProt(String rnWorkProt) {
		this.rnWorkProt = rnWorkProt;
	}

	public boolean isRnWorkProtEQ() {
		return rnWorkProtEQ;
	}

	public void setRnWorkProtEQ(boolean rnWorkProtEQ) {
		this.rnWorkProtEQ = rnWorkProtEQ;
	}	

	public Integer getPeriodOficProt() {
		return periodOficProt;
	}

	public void setPeriodOficProt(Integer periodOficProt) {
		this.periodOficProt = periodOficProt;
	}

	public Date getDateFromOficProt() {
		return dateFromOficProt;
	}

	public void setDateFromOficProt(Date dateFromOficProt) {
		this.dateFromOficProt = dateFromOficProt;
	}

	public Date getDateToOficProt() {
		return dateToOficProt;
	}

	public void setDateToOficProt(Date dateToOficProt) {
		this.dateToOficProt = dateToOficProt;
	}

	public String getRnOficProt() {
		return rnOficProt;
	}

	public void setRnOficProt(String rnOficProt) {
		this.rnOficProt = rnOficProt;
	}

	public boolean isRnOficProtEQ() {
		return rnOficProtEQ;
	}

	public void setRnOficProtEQ(boolean rnOficProtEQ) {
		this.rnOficProtEQ = rnOficProtEQ;
	}

	public String getIncRnDoc() {
		return incRnDoc;
	}

	public void setIncRnDoc(String incRnDoc) {
		this.incRnDoc = incRnDoc;
	}

	public Date getIncDocDate() {
		return incDocDate;
	}

	public void setIncDocDate(Date incDocDate) {
		this.incDocDate = incDocDate;
	}

	public LazyDataModelSQL2Array getDocsDestrucList() {
		return docsDestrucList;
	}

	public void setDocsDestrucList(LazyDataModelSQL2Array docsDestrucList) {
		this.docsDestrucList = docsDestrucList;
	}
	
}
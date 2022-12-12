package com.ib.omb.beans;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.BaseException;
import com.ib.system.utils.DateUtils;

@Named
@ViewScoped
public class DocArchiveList extends IndexUIbean   {

	/**
	 * Списък с протоколи за предаване в архив
	 * 
	 */
	private static final long serialVersionUID = -4134432849445665442L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocArchiveList.class);
	
	private Integer period;	
	private Date dateFrom;
	private Date dateTo;
	private boolean selProtUA = false;	
	private boolean selProtDA = false;	
	private boolean onlyWorkProt = false;	
	//private Integer[] selectedVidProt; // няма да се използва p:selectManyCheckbox, тъй като трябва да се крие чекбокса, ако няма системна настройка за някой вид протокол - 05.05.2021
	private Integer protForUA;
	private Integer protForDA;
	private Map<Integer, Integer> vidAndStatusArchive;
	
	private String rnWorkProt;
	private boolean	rnWorkProtEQ;	// ако е true се търси по пълно съвпадение по номер на работен протокол
	
	private Integer periodOficProt;	
	private Date dateFromOficProt;
	private Date dateToOficProt;
	private String rnOficProt;
	private boolean	rnOficProtEQ;
	
	private LazyDataModelSQL2Array docsArchiveList;
	
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct!!!");
	
		try {
			String protUA = null;
			String protDA = null;
			
			protUA = getSystemData().getSettingsValue("delo.archiveProtocolUA");	// 8		
			protDA = getSystemData().getSettingsValue("delo.archiveProtocolDA");	// 12
						
			if (protUA == null && protDA == null) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "docArchiveList.noSystemSettings"));
				return;
			
			} else {
				
				this.vidAndStatusArchive = new HashMap<>();
				
				if (protUA != null) {
					this.protForUA = Integer.valueOf(protUA);
					this.vidAndStatusArchive.put(this.protForUA, Integer.valueOf(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_UA));
				}
				
				if (protDA != null) {
					this.protForDA = Integer.valueOf(protDA);
					this.vidAndStatusArchive.put(this.protForDA, Integer.valueOf(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_DA));					
				}	
									
			}	
			
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на вид на документ 'Протокол за предаване в учрежденски архив' или 'Протокол за предаване в Държавен архив'! ", e);		
		}
	}
	
	/** 
	 * Списък с протоколи за унищожаване на документи по зададени критерии 
	 * 
	 */
	public void actionSearch(){
		
		Integer vidProt = null;	
		
//		if (this.selectedVidProt.length == 1) {
//			vidProt = this.selectedVidProt[0];
//		}
		
		if (selProtUA && !selProtDA) {
			vidProt = this.protForUA;
		}
		
		if (selProtDA && !selProtUA) {
			vidProt = this.protForDA;
		}
																						
		SelectMetadata smd = new DocProtocolDAO(2, getUserData()).createSelectArchiveProtocol(getUserData(UserData.class).getRegistratura(), vidProt, this.dateFrom, this.dateTo, this.onlyWorkProt, this.vidAndStatusArchive, 
				this.rnWorkProt, this.rnWorkProtEQ, this.dateFromOficProt, this.dateToOficProt, this.rnOficProt, this.rnOficProtEQ);
		String defaultSortColumn = "a0";
		this.docsArchiveList = new LazyDataModelSQL2Array(smd, defaultSortColumn);		
	} 
	
	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClear() {		
		
		changePeriod();
		this.period = null;
		this.dateFrom = null;
		this.dateTo = null;
		this.selProtUA = false;
		this.selProtDA = false;
		this.onlyWorkProt = false;
//		this.selectedVidProt = null;
		
		this.rnWorkProt = null;
		this.rnWorkProtEQ = false;
		
		this.periodOficProt = null;
		this.dateFromOficProt = null;
		this.dateToOficProt = null;
		this.rnOficProt = null;
		this.rnOficProtEQ = false;
		
		this.docsArchiveList = null;
	}
	
	public String actionGoto(Integer idObj, Integer vidProt) {
		
		return "docArchive.jsf?faces-redirect=true&idObj=" + idObj + "&vidProt=" + vidProt;
	}
	
	public String actionGotoNew(Integer vidProt) {
		
		return "docArchive.jsf?faces-redirect=true&vidProt=" + vidProt;	
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
		
		String title = getMessageResourceString(LABELS, "docArchiveList.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfo(), null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за протокола за унищожаване на документи
	 */
	public void preProcessPDF(Object document)  {
		
		try {
			
			String title = getMessageResourceString(LABELS, "docArchiveList.reportTitle");		
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

	public boolean isSelProtUA() {
		return selProtUA;
	}

	public void setSelProtUA(boolean selProtUA) {
		this.selProtUA = selProtUA;
	}

	public boolean isSelProtDA() {
		return selProtDA;
	}

	public void setSelProtDA(boolean selProtDA) {
		this.selProtDA = selProtDA;
	}

	public boolean isOnlyWorkProt() {
		return onlyWorkProt;
	}

	public void setOnlyWorkProt(boolean onlyWorkProt) {
		this.onlyWorkProt = onlyWorkProt;
	}	

//	public Integer[] getSelectedVidProt() {
//		return selectedVidProt;
//	}
//
//	public void setSelectedVidProt(Integer[] selectedVidProt) {
//		this.selectedVidProt = selectedVidProt;
//	}

	public Integer getProtForUA() {
		return protForUA;
	}

	public void setProtForUA(Integer protForUA) {
		this.protForUA = protForUA;
	}

	public Integer getProtForDA() {
		return protForDA;
	}

	public void setProtForDA(Integer protForDA) {
		this.protForDA = protForDA;
	}

	public Map<Integer, Integer> getVidAndStatusArchive() {
		return vidAndStatusArchive;
	}

	public void setVidAndStatusArchive(Map<Integer, Integer> vidAndStatusArchive) {
		this.vidAndStatusArchive = vidAndStatusArchive;
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

	public LazyDataModelSQL2Array getDocsArchiveList() {
		return docsArchiveList;
	}

	public void setDocsArchiveList(LazyDataModelSQL2Array docsArchiveList) {
		this.docsArchiveList = docsArchiveList;
	}
	
}
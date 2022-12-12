package com.ib.omb.beans;

import java.io.Serializable;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.db.dao.StatTableDAO;
import com.ib.indexui.db.dto.StatTable;
import com.ib.indexui.pagination.UniSelectMetadata;
import com.ib.indexui.report.stat.SavedStatReport;
import com.ib.indexui.report.stat.StatisticalReport;
import com.ib.indexui.report.uni.UniSearchBuilder;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;

/**
 * Статистически справки
 */
@Named
@ViewScoped
public class StatisticReports extends IndexUIbean  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9142426992807085075L;
	private static final Logger LOGGER = LoggerFactory.getLogger(StatisticReports.class);
	
	private Date decodeDate;
	private Integer codeReport; // кода от класифиакцията
	private Date dateFrom;
	private Date dateTo;
	private String labelDate = ""; // името на полето за дата
	private String codeExt; // ИД на съхранена заявка
	private String codeDopInfo; // алгоритъм за използване на датите
	
	private boolean rateRoundBool = false;
	private Integer rateRound = Integer.valueOf(2);
	
	@PostConstruct
	void initData() {
				
		LOGGER.debug("PostConstruct!!!");	
		decodeDate = new Date();					
		
	}
	
	/**
	 * Избор на справка
	 */
	public void actionSelectCode() {
		if(codeReport!=null) {
			try {
				SystemClassif item = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_STAT_REPORTS, codeReport, getCurrentLang(), decodeDate, false);	
				if(item!=null) {
					codeExt = item.getCodeExt();
					
					if(item.getDopInfo()!=null &&!"".equals(item.getDopInfo())) {
						String[] itemDopInfo = item.getDopInfo().split(":");
						if(itemDopInfo!=null) {
							setCodeDopInfo(itemDopInfo[0]); 
							labelDate = itemDopInfo[1];
						}
					}
				}
				
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при избор на справка! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}
	}
	
	/**
	 * Метод за търсене 
	 */
	public String actionSearch() {
		if(checkData()) {
			return "";
		}

		StatisticalReport runReport;
		String title;
		StatTable statTable;
		try {
			// съхранената заявка
			SavedStatReport savedReport = new SavedStatReport(getSystemData(), Integer.valueOf(this.codeExt), true);
			
			// сетване на датите в заявката
			int alg = Integer.parseInt(this.codeDopInfo);
			savedReport.replaceDateArgs(this.dateFrom, this.dateTo, alg);
			
			// статистическата таблица
			statTable = new StatTableDAO(getUserData()).findById(savedReport.getStatTableId());
			
			// справката като изпълнение
			runReport = new StatisticalReport(savedReport.getAisId(), statTable, getCurrentUserId());
			
			// граденето на заявката
			UniSearchBuilder selectBuilder = new UniSearchBuilder(savedReport.getAisId(), getSystemData(), JPA.getUtil().getDbVendorName(), getCurrentLang());
			UniSelectMetadata metadataResult  = selectBuilder.createSQL(savedReport.mergeWithArguments(), false, false);
			
			// ралното изпънение
			runReport.execute(metadataResult, getSystemData(), getCurrentLang(), this.rateRoundBool, this.rateRound);
			
			// сглобявам името и с датите
			String dates;
			if (alg == 2) {
				dates = " " + DateUtils.printDate(this.dateTo);
			} else {
				dates = ": " + DateUtils.printDate(this.dateFrom) + " - " + DateUtils.printDate(this.dateTo);
			}
			title = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_STAT_REPORTS, this.codeReport, getCurrentLang(), null) + dates;

		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"));
			LOGGER.error(e.getMessage(), e);
			return ""; // грешка в изпълнението
		} finally {
			JPA.getUtil().closeConnection();
		}

		statTable.setTitle(title);
		JSFUtils.addFlashScopeValue("statReport", runReport);
		
		LOGGER.debug("report in flashScope >>>>>>");
		
		return "statReportTableResult.xhtml?faces-redirect=true"; 
	}	
	
	private boolean checkData() {
		boolean save = false;
		
		if(codeReport==null) {
			JSFUtils.addMessage("statForm:codeRep:аutoCompl_input", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages ,"statisticReports.selectReport"));
			save = true;
		}else {
		
			if( "2".equals(codeDopInfo)) {
				if(dateTo==null) {
					JSFUtils.addMessage("statForm:date", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages , "statisticReport.selectDate"));
					save = true;
				}
			}else {
				if(dateFrom==null) {
					JSFUtils.addMessage("statForm:dateOt", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages , "statisticReport.selectDateFrom"));
					save = true;
				}
				
				if(dateTo==null) {
					JSFUtils.addMessage("statForm:dateTo", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages , "statisticReport.selectDateTo"));
					save = true;
				}
			}
		}	
		return save;
		
	}
	
	/**
	 * Зачистване на критериите за търсене 
	 */
	public void actionClearFilter() {
		codeReport = null;
		dateFrom = null;
		dateTo = null;
		labelDate = "";
		codeExt = "";
		codeDopInfo= "";
	}

	public Date getDecodeDate() {
//		return new Date(decodeDate.getTime()) ;
		return decodeDate;
	}

//	public void setDecodeDate(Date decodeDate) {
//		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
//	}


	public Integer getCodeReport() {
		return codeReport;
	}


	public void setCodeReport(Integer codeReport) {
		this.codeReport = codeReport;
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

	public String getLabelDate() {
		return labelDate;
	}

	public void setLabelDate(String labelDate) {
		this.labelDate = labelDate;
	}

	public String getCodeDopInfo() {
		return codeDopInfo;
	}

	public void setCodeDopInfo(String codeDopInfo) {
		this.codeDopInfo = codeDopInfo;
	}

	public boolean isRateRoundBool() {
		return rateRoundBool;
	}

	public void setRateRoundBool(boolean rateRoundBool) {
		this.rateRoundBool = rateRoundBool;
	}

	public Integer getRateRound() {
		return rateRound;
	}

	public void setRateRound(Integer rateRound) {
		this.rateRound = rateRound;
	}

	
}
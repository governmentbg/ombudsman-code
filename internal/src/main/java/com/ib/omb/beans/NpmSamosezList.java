package com.ib.omb.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.faces.view.facelets.FaceletContext;
import javax.inject.Named;

import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.search.DocSpecSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

@Named("npmSamosez")
@ViewScoped
public class NpmSamosezList extends IndexUIbean {
	
	private static final long serialVersionUID = 7938578009025160928L;
	private static final Logger LOGGER = LoggerFactory.getLogger(NpmSamosezList.class);
	
	private Date decodeDate;
	private boolean isView;
	private Variant variant;
	
	private DocSpecSearch docSpecSearch;
	private LazyDataModelSQL2Array docsList;
	
	private boolean vidima;
	private boolean preporuka;
	private Integer periodReg;
	private Integer periodStart;
	private Integer periodEnd;
	private String textOrgan;
	private List<SystemClassif> sastoiania;
	private List<SystemClassif> tipOrganNpm;
	private List<SystemClassif> narPrava;
	
	private List<Object[]> docSelectedAllM = new ArrayList<>();
	private List<Object[]> docSelectedTmp = new ArrayList<>();  

	@PostConstruct
	private void initData() {

		this.decodeDate = new Date();
		
		FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
		String isViewStr = (String) faceletContext.getAttribute("isView");
		if(isViewStr != null && !isViewStr.contentEquals("null")) {
			this.isView = isViewStr.contentEquals("1");
		}
		
		String variantStr = (String) faceletContext.getAttribute("variant");
		if(variantStr != null) {
			if(variantStr.contentEquals("npm")) variant = Variant.NPM;
			if(variantStr.contentEquals("samosez")) variant = Variant.SAMOSEZ;
		}
		
		clearFilter();
		
	}
	
	/**
	 * Натиснат е бутонът 'Търсене'
	 */
	public void actionSearch(){
		
		this.docSelectedAllM.clear();
		this.docSelectedTmp.clear();
		
		if(this.variant == Variant.NPM) {
			this.docSpecSearch.buildQueryNpmList(getUserData(), isView);
		}
		else if(this.variant == Variant.SAMOSEZ) {
			this.docSpecSearch.buildQuerySamosList(getUserData(), isView);
		}
		
		this.docsList = new LazyDataModelSQL2Array(this.docSpecSearch, "a2 desc");
	}
	
	/**
	 * Натиснат е бутонът 'Изчистване'
	 */
	public void actionClear() {
		clearFilter();
		this.docsList = null;
	}
	
	/**
	 * Изчиства филтъра
	 */
	public void clearFilter() {
		
		this.docSpecSearch = new DocSpecSearch();
		this.sastoiania = null;
		this.tipOrganNpm = null;
		this.narPrava = null;
		this.textOrgan = null;
		
		this.vidima = false;
		this.preporuka = false;
		
		resetDateReg();
		resetDateStart();
		resetDateEnd();
	}
	
	/**
	 * Избрана е опция 'Изтриване'
	 */
	public void actionDeleteSelected() {
		StringBuilder msg = new StringBuilder();
		DocDAO docDao = new DocDAO(getUserData());

		for (Object[] row : docSelectedAllM) {
			try {
				Integer docId = SearchUtils.asInteger(row[0]);
				JPA.getUtil().runInTransaction(() -> {
					Doc d = docDao.findById(docId);
					docDao.delete(d);
					
					if (msg.length() > 0) {
						msg.append(", ");
					}
					msg.append(d.getIdentInfo());
				}); 
				
			} catch (ObjectInUseException e) {
				LOGGER.error("Групово изтриване на документи -{}", e.getMessage());
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} catch (BaseException e) {
				LOGGER.error("Грешка при групово изтриване на документи от базата данни!", e);
				JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
			} 
		}
		
		if (msg.length() > 0) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"), "Документи: " + msg + ".");
		}

		this.docSelectedAllM.clear();
		this.docSelectedTmp.clear();
		this.docsList.loadCountSql();
	}
	
	/**
	 * При смяна на полето 'период на регистрация'
	 */
	public void changePeriodReg() {
		
    	if (this.periodReg != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodReg);
			this.docSpecSearch.setDocDateFrom(di[0]);
			this.docSpecSearch.setDocDateTo(di[1]);		
    	} else {
    		this.docSpecSearch.setDocDateFrom(null);
    		this.docSpecSearch.setDocDateTo(null);
		}
    }
	public void resetDateReg() { 
		this.setPeriodReg(null);
	}
	
	/**
	 * При смяна на полето 'период на датата на започване'
	 */
	public void changePeriodStart() {
		
    	if (this.periodStart != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodStart);
			this.docSpecSearch.setStartDateFrom(di[0]);
			this.docSpecSearch.setStartDateTo(di[1]);		
    	} else {
    		this.docSpecSearch.setStartDateFrom(null);
    		this.docSpecSearch.setStartDateTo(null);
		}
    }
	public void resetDateStart() { 
		this.setPeriodStart(null);
	}
	
	/**
	 * При смяна на полето 'период на срока за приключване'
	 */
	public void changePeriodEnd() {
		
    	if (this.periodEnd != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodEnd);
			this.docSpecSearch.setSrokFrom(di[0]);
			this.docSpecSearch.setSrokTo(di[1]);		
    	} else {
    		this.docSpecSearch.setSrokFrom(null);
    		this.docSpecSearch.setSrokTo(null);
		}
    }
	public void resetDateEnd() { 
		this.setPeriodEnd(null);
	}
	
	/**
	 * Кликване на чекбокса 'видима в публичния регистър'
	 */
	public void checkVidima() {
		this.docSpecSearch.setPublicVisible(this.vidima ? OmbConstants.CODE_ZNACHENIE_DA : null);
	}
	
	/**
	 * Кликване на чекбокса 'дадена препоръка'
	 */
	public void checkPreporuka() {
		this.docSpecSearch.setPrepor(this.preporuka ? OmbConstants.CODE_ZNACHENIE_DA : null);
	}
	
	/**
	 * При писане в полето 'проверен орган'
	 */
	public void actionChangeTextOrgan() {
		this.docSpecSearch.setCodeOrgan(null);
		this.docSpecSearch.setNameOrgan(this.textOrgan);
	}
	
	/**
	 * При избор на 'проверен орган'
	 */
	public void actionSelectOrgan() {
		if(this.docSpecSearch.getCodeOrgan() != null) {
			try {
				this.textOrgan = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS, this.docSpecSearch.getCodeOrgan(), getCurrentLang(), this.decodeDate);
				this.docSpecSearch.setNameOrgan(null);
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при разкодиране на кореспондент! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}	
	}
	
	/**
	 * Избира всички редове от текущата страница
	 * @param eventT
	 */
	public void onRowSelectAll(ToggleSelectEvent eventT) {    
			ArrayList<Object[]> tmpList = new ArrayList<>();
	    	tmpList.addAll(getDocSelectedAllM());
	    	if(eventT.isSelected()) {
	    		for (Object[] object : getDocSelectedTmp()) {
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
		    	List<Object[]> tmpLPageClass =  getDocsList().getResult(); // rows from current page....    		
				for (Object[] object : tmpLPageClass) {
					if(object != null && object.length > 0) {
						Integer l2 =Integer.valueOf(object[0].toString());
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
			setDocSelectedAllM(tmpList);	    	
			LOGGER.debug("onToggleSelect->>");	    	    	   
	}
		    
    /** 
     * Select one row
     * @param eventS
     */
    @SuppressWarnings("rawtypes")
	public void onRowSelect(SelectEvent eventS) {    	
    	if(eventS!=null  && eventS.getObject()!=null) {
    		List<Object[]> tmpLst =  getDocSelectedAllM();
    		
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
					setDocSelectedAllM(tmpLst);   
				}
    		}
    	}	    	
    	LOGGER.error(String.format("1 onRowSelectIil->> %s", getDocSelectedAllM().size()));
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
    		tmpLst.addAll(getDocSelectedAllM());
    		for (Object[] j : tmpLst) {
    			if(j != null && j.length > 0 
    				&& object != null && object.length > 0) {
    				Integer l1 = Integer.valueOf(j[0].toString());
	    			Integer l2 = Integer.valueOf(object[0].toString());
		    		if(l1.equals(l2)) {
		    			tmpLst.remove(j);
		    			setDocSelectedAllM(tmpLst);
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
    	if (getDocSelectedAllM() != null && !getDocSelectedAllM().isEmpty()) {
    		getDocSelectedTmp().clear();
    		getDocSelectedTmp().addAll(getDocSelectedAllM());
    	}		    	
    }
    
    /**
     * Натиснат е бутонът Редакция/Разглеждане в таблицата
     */
    public String actionGoto(int i, Object[] row) {
		
    	Integer idObj = ((Number) row[0]).intValue();
		
		String result = "";
		if(i == 0) {
			result = "docEdit.jsf?faces-redirect=true&idObj=" + idObj;
		} 
		else if(i == 1){
			result = "docView.xhtml?faces-redirect=true&idObj=" + idObj;
		}
		
		return result;
	}
    
    /**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
	 */
	public void postProcessXLS(Object document) {
		
		String title = "";
		if (variant == Variant.NPM) {
			title = getMessageResourceString(LABELS, "specDoc.npmReportTitle");
		}
		else if (variant == Variant.SAMOSEZ) {
			title = getMessageResourceString(LABELS, "specDoc.samosezReportTitle");
		}
		
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport() , null, null);		
     
	}
	
	/**
	 * подзаглавие за екпсорта - .............
	 */
	public Object[] dopInfoReport() {
		Object[] dopInf = null;
		dopInf = new Object[2];
		if(docSpecSearch.getDocDateFrom() != null && docSpecSearch.getDocDateTo() != null) {
			dopInf[0] = "период: " + DateUtils.printDate(docSpecSearch.getDocDateFrom()) + " - " + DateUtils.printDate(docSpecSearch.getDocDateTo());
		} 
	
		return dopInf;
	}
	
	private enum Variant {
		NPM, SAMOSEZ
	}
    
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
    public DocSpecSearch getDocSpecSearch() {
		return docSpecSearch;
	}

	public void setDocSpecSearch(DocSpecSearch docSpecSearch) {
		this.docSpecSearch = docSpecSearch;
	}
	
	public LazyDataModelSQL2Array getDocsList() {
		return docsList;
	}

	public void setDocsList(LazyDataModelSQL2Array docsList) {
		this.docsList = docsList;
	}

	public Date getDecodeDate() {
		return new Date(decodeDate.getTime()) ;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
	}

	public Integer getPeriodReg() {
		return periodReg;
	}

	public void setPeriodReg(Integer periodReg) {
		this.periodReg = periodReg;
	}

	public Integer getPeriodStart() {
		return periodStart;
	}

	public void setPeriodStart(Integer periodStart) {
		this.periodStart = periodStart;
	}

	public Integer getPeriodEnd() {
		return periodEnd;
	}

	public void setPeriodEnd(Integer periodEnd) {
		this.periodEnd = periodEnd;
	}

	public List<SystemClassif> getSastoiania() {
		return sastoiania;
	}

	public void setSastoiania(List<SystemClassif> sastoiania) {
		this.sastoiania = sastoiania;
	}

	public List<SystemClassif> getTipOrganNpm() {
		return tipOrganNpm;
	}

	public void setTipOrganNpm(List<SystemClassif> tipOrganNpm) {
		this.tipOrganNpm = tipOrganNpm;
	}

	public List<SystemClassif> getNarPrava() {
		return narPrava;
	}

	public void setNarPrava(List<SystemClassif> narPrava) {
		this.narPrava = narPrava;
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

	public boolean isView() {
		return isView;
	}

	public void setView(boolean isView) {
		this.isView = isView;
	}

	public Variant getVariant() {
		return variant;
	}

	public void setVariant(Variant variant) {
		this.variant = variant;
	}

	public boolean isVidima() {
		return vidima;
	}

	public void setVidima(boolean vidima) {
		this.vidima = vidima;
	}

	public boolean isPreporuka() {
		return preporuka;
	}

	public void setPreporuka(boolean preporuka) {
		this.preporuka = preporuka;
	}

	public String getTextOrgan() {
		return textOrgan;
	}

	public void setTextOrgan(String textOrgan) {
		this.textOrgan = textOrgan;
	}
	
}

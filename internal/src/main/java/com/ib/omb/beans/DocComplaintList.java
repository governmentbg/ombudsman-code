package com.ib.omb.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import com.ib.omb.search.DocJalbaSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysClassifAdapter;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

@Named("jalbaList")
@ViewScoped
public class DocComplaintList extends IndexUIbean {
	
	private static final long serialVersionUID = 3616147847217876485L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocComplaintList.class);
	
	private Date decodeDate;
	private boolean isView;
	
	private DocJalbaSearch jalbaSearch;
	private LazyDataModelSQL2Array jalbiList;
	
	private Integer periodReg;
	private Integer periodRazgl;
	private Integer periodNarush;
	private Integer periodPodav;
	private String textNarush;
	private boolean korupcia;
	private boolean vidima;
	private boolean zapazenaSamol;
	private String lichniDanniVid; 
	private Map<Integer, Object> specificsEkatte;
	private List<SystemClassif> jalbaSastoiania;
	private List<SystemClassif> vidOplList;
	private List<SystemClassif> vidNarushList;
	private List<SystemClassif> nachinPredav;
	private List<SystemClassif> nedopustList;
	private List<SystemClassif> vidRezultList;
	
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
		
		clearFilter();
		
	}
	
	/**
	 * Натиснат е бутонът 'Търсене'
	 */
	public void actionSearch(){
		
		this.docSelectedAllM.clear();
		this.docSelectedTmp.clear();
		 
		try {
			this.jalbaSearch.buildQueryJalbaList(getUserData(), isView, getSystemData());
			this.jalbiList = new LazyDataModelSQL2Array(this.jalbaSearch, "a2 desc");
		}
		catch(DbErrorException e) {
			LOGGER.error("Грешка при търсене на жалба!", e);
		}
		
	}
	
	/**
	 * Натиснат е бутонът 'Изчистване'
	 */
	public void actionClear() {
		clearFilter();
		this.jalbiList = null;
	}
	
	/**
	 * Изчиства филтъра
	 */
	public void clearFilter() {
		
		this.jalbaSearch = new DocJalbaSearch();
		this.jalbaSastoiania = null;
		this.textNarush = null;
		
		// При две полета, които са свързани с логически списък, опциите във второто поле зависят от опциите в първото.
		// Когато първото обаче е празно, второто не си седи празно, а показва цялата класификация. Долу се зареждат.
		try {
			this.setVidOplList(getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_VID_OPL, this.decodeDate, getCurrentLang()));
			this.setVidNarushList(getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_VID_NAR, this.decodeDate, getCurrentLang()));
			this.setNedopustList(getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_OSN_NEDOPUST, this.decodeDate, getCurrentLang()));
			this.setVidRezultList(getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_JALBA_RES, this.decodeDate, getCurrentLang()));
		}
		catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на класификация! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			this.setVidOplList(new ArrayList<>());
			this.setVidNarushList(new ArrayList<>());
			this.setNedopustList(new ArrayList<>());
			this.setVidRezultList(new ArrayList<>());
		} 
		
		this.nachinPredav = new ArrayList<>();
		this.korupcia = false;
		this.vidima = false;
		this.zapazenaSamol = false;
		this.lichniDanniVid = "egn";
		
		resetDateReg();
		resetDateRazgl();
		resetDateNarush();
		resetDatePodav();
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
		this.jalbiList.loadCountSql();
	}
	
	/**
	 * При смяна на полето 'период на регистрация'
	 */
	public void changePeriodReg() {
		
    	if (this.periodReg != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodReg);
			this.jalbaSearch.setDocDateFrom(di[0]);
			this.jalbaSearch.setDocDateTo(di[1]);		
    	} else {
    		this.jalbaSearch.setDocDateFrom(null);
    		this.jalbaSearch.setDocDateTo(null);
		}
    }
	public void resetDateReg() { 
		this.setPeriodReg(null);
	}
	
	/**
	 * При смяна на полето 'период на срока за разглеждане'
	 */
	public void changePeriodRazgl() {
		
    	if (this.periodRazgl != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodRazgl);
			this.jalbaSearch.setSrokFrom(di[0]);
			this.jalbaSearch.setSrokTo(di[1]);		
    	} else {
    		this.jalbaSearch.setSrokFrom(null);
    		this.jalbaSearch.setSrokTo(null);
		}
    }
	public void resetDateRazgl() { 
		this.setPeriodRazgl(null);
	}
	
	/**
	 * При смяна на полето 'период на датата на извършване на нарушението'
	 */
	public void changePeriodNarush() {
		
    	if (this.periodNarush != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodNarush);
			this.jalbaSearch.setDateNarFrom(di[0]);
			this.jalbaSearch.setDateNarTo(di[1]);		
    	} else {
    		this.jalbaSearch.setDateNarFrom(null);
    		this.jalbaSearch.setDateNarTo(null);
		}
    }
	public void resetDateNarush() { 
		this.setPeriodNarush(null);
	}
	
	/**
	 * При смяна на полето 'период на датата на подаване'
	 */
	public void changePeriodPodav() {
		
    	if (this.periodPodav != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodPodav);
			this.jalbaSearch.setSubmitDateFrom(di[0]);
			this.jalbaSearch.setSubmitDateTo(di[1]);		
    	} else {
    		this.jalbaSearch.setSubmitDateFrom(null);
    		this.jalbaSearch.setSubmitDateTo(null);
		}
    }
	public void resetDatePodav() { 
		this.setPeriodPodav(null);
	}
	
	public void changeLichniDanniType(String type) {
		this.setLichniDanniVid(type);
		this.jalbaSearch.setJbpEgn(null);
		this.jalbaSearch.setJbpLnc(null);
		this.jalbaSearch.setJbpEik(null);
	}
	
	
	/**
	 * При писане в полето 'Нарушител'
	 */
	public void actionChangeTextNarush() {
		this.jalbaSearch.setCodeRefNar(null) ;
		this.jalbaSearch.setNameNar(this.textNarush);
	}
	
	/**
	 * При избор на 'Нарушител'
	 */
	public void actionSelectNarush() {
		if(this.jalbaSearch.getCodeRefNar() != null) {
			try {
				this.textNarush = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS, this.jalbaSearch.getCodeRefNar(), getCurrentLang(), this.decodeDate);
				this.jalbaSearch.setNameNar(null);
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при разкодиране на кореспондент! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}	
	}
	
	/**
	 * При избор в падащото меню 'Категория на нарушител'
	 */
	public void actionChangeKateg() {
		this.jalbaSearch.setVidNar(null);
		
		try {
			if(this.jalbaSearch.getKatNar() != null) {
				this.setVidNarushList(getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_KAT_NAR_VID_NAR, this.jalbaSearch.getKatNar() , getCurrentLang(), this.decodeDate));
			}
			else {
				this.setVidNarushList(getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_VID_NAR, this.decodeDate, getCurrentLang()));
			}
		}
		catch (DbErrorException e) {
			LOGGER.error("Грешка при избор на категория на нарушител", e);
			this.setVidNarushList(new ArrayList<>());
		}
	}
	
	/**
	 * При избор в падащото меню 'Засегнати права'
	 */
	public void actionChangePrava() {
		this.jalbaSearch.setVidOpl(null);
		try {
			if(this.jalbaSearch.getZasPrava() != null) {
				this.setVidOplList(getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_ZAS_PRAVA_VID_OPL, this.jalbaSearch.getZasPrava() , getCurrentLang(), new Date()));
			}
			else {
				this.setVidOplList(getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_VID_OPL, this.decodeDate, getCurrentLang()));
			}
		}
		catch (DbErrorException e) {
			LOGGER.error("Грешка при избор на засегнати права", e);
			this.setVidOplList(new ArrayList<>());
		}	
	}
	
	/**
	 * При избор в падащото меню 'Допустимост'
	 */
	public void actionChangeDopust() {
		this.jalbaSearch.setOsnNedopust(null);
		try {
			if(this.jalbaSearch.getDopust() != null) {
				this.setNedopustList(getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_DOPUST_OSN_NEDOPUST, this.jalbaSearch.getDopust() , getCurrentLang(), new Date()));
			}
			else {
				this.setNedopustList(getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_OSN_NEDOPUST, this.decodeDate, getCurrentLang()));
			}
		}
		catch (DbErrorException e) {
			LOGGER.error("Грешка при избор на допустимост", e);
			this.setNedopustList(new ArrayList<>());
		}
	}
	
	/**
	 * При избор в падащото меню 'Начин на финализиране'
	 */
	public void actionChangeFinal() {
		this.jalbaSearch.setVidResult(null);
		
		try {
			if(this.jalbaSearch.getFinMethod() != null) {
				this.setVidRezultList(getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_JALBA_FIN_JALBA_RES, this.jalbaSearch.getFinMethod() , getCurrentLang(), new Date()));
			}
			else {
				this.setVidRezultList(getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_JALBA_RES, this.decodeDate, getCurrentLang()));
			}
		}
		catch (DbErrorException e) {
			LOGGER.error("Грешка при избор на начин на финализиране", e);
			this.setVidRezultList(new ArrayList<>());
		}	
	}
	
	/**
	 * Кликване на чекбокса 'Свързана с корупция'
	 */
	public void checkKorupcia() {
		this.jalbaSearch.setCorruption(this.korupcia ? OmbConstants.CODE_ZNACHENIE_DA : null);
	}
	
	/**
	 * Кликване на чекбокса 'Видима в публичния регистър'
	 */
	public void checkVidima() {
		this.jalbaSearch.setPublicVisible(this.vidima ? OmbConstants.CODE_ZNACHENIE_DA : null);
	}
	
	/**
	 * Кликване на чекбокса 'Запазена самоличност'
	 */
	public void checkZapazenaSamol() {
		this.jalbaSearch.setJbpHidden(this.zapazenaSamol ? OmbConstants.CODE_ZNACHENIE_DA : null);
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
		    	List<Object[]> tmpLPageClass =  getJalbiList().getResult(); // rows from current page....    		
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
		
		String title = getMessageResourceString(LABELS, "complaint.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport() , null, null);		
     
	}
	
	/**
	 * подзаглавие за екпсорта - .............
	 */
	public Object[] dopInfoReport() {
		Object[] dopInf = null;
		dopInf = new Object[2];
		if(jalbaSearch.getDocDateFrom() != null && jalbaSearch.getDocDateTo() != null) {
			dopInf[0] = "период: " + DateUtils.printDate(jalbaSearch.getDocDateFrom()) + " - " + DateUtils.printDate(jalbaSearch.getDocDateTo());
		} 
	
		return dopInf;
	}
	
	
	/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
	
	public DocJalbaSearch getJalbaSearch() {
		return jalbaSearch;
	}

	public void setJalbaSearch(DocJalbaSearch jalbaSearch) {
		this.jalbaSearch = jalbaSearch;
	}

	public LazyDataModelSQL2Array getJalbiList() {
		return jalbiList;
	}

	public void setJalbiList(LazyDataModelSQL2Array jalbiList) {
		this.jalbiList = jalbiList;
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

	public Integer getPeriodRazgl() {
		return periodRazgl;
	}

	public void setPeriodRazgl(Integer periodRazgl) {
		this.periodRazgl = periodRazgl;
	}

	public Integer getPeriodNarush() {
		return periodNarush;
	}

	public void setPeriodNarush(Integer periodNarush) {
		this.periodNarush = periodNarush;
	}

	public Integer getPeriodPodav() {
		return periodPodav;
	}

	public void setPeriodPodav(Integer periodPodav) {
		this.periodPodav = periodPodav;
	}

	public List<SystemClassif> getJalbaSastoiania() {
		return jalbaSastoiania;
	}

	public void setJalbaSastoiania(List<SystemClassif> jalbaSastoiania) {
		this.jalbaSastoiania = jalbaSastoiania;
	}
	
	public List<SystemClassif> getVidOplList() {
		return vidOplList;
	}

	public void setVidOplList(List<SystemClassif> vidOplList) {
		this.vidOplList = vidOplList;
	}

	public List<SystemClassif> getVidNarushList() {
		return vidNarushList;
	}

	public void setVidNarushList(List<SystemClassif> vidNarushList) {
		this.vidNarushList = vidNarushList;
	}

	public List<SystemClassif> getNachinPredav() {
		return nachinPredav;
	}

	public void setNachinPredav(List<SystemClassif> nachinPredav) {
		this.nachinPredav = nachinPredav;
	}

	public List<SystemClassif> getNedopustList() {
		return nedopustList;
	}

	public void setNedopustList(List<SystemClassif> nedopustList) {
		this.nedopustList = nedopustList;
	}

	public List<SystemClassif> getVidRezultList() {
		return vidRezultList;
	}

	public void setVidRezultList(List<SystemClassif> vidRezultList) {
		this.vidRezultList = vidRezultList;
	}

	public Map<Integer, Object> getSpecificsEkatte() {
		if(this.specificsEkatte == null) {
			this.specificsEkatte = Collections.singletonMap(SysClassifAdapter.EKATTE_INDEX_TIP, 3);
		}
		return this.specificsEkatte;
	}

	public void setSpecificsEkatte(Map<Integer, Object> specificsEkatte) {
		this.specificsEkatte = specificsEkatte;
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

	public String getTextNarush() {
		return textNarush;
	}

	public void setTextNarush(String textNarush) {
		this.textNarush = textNarush;
	}

	public boolean isKorupcia() {
		return korupcia;
	}

	public void setKorupcia(boolean korupcia) {
		this.korupcia = korupcia;
	}

	public boolean isVidima() {
		return vidima;
	}

	public void setVidima(boolean vidima) {
		this.vidima = vidima;
	}

	public boolean isZapazenaSamol() {
		return zapazenaSamol;
	}

	public void setZapazenaSamol(boolean zapazenaSamol) {
		this.zapazenaSamol = zapazenaSamol;
	}

	public String getLichniDanniVid() {
		return lichniDanniVid;
	}

	public void setLichniDanniVid(String lichniDanniVid) {
		this.lichniDanniVid = lichniDanniVid;
	}
	
}

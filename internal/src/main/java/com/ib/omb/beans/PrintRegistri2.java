package com.ib.omb.beans;

import java.io.IOException;


import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.export.PDFOptions;
import org.primefaces.model.SortMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.RegisterDAO;
import com.ib.omb.db.dao.RegisterDocsPrintDAO;
import com.ib.omb.print.RegistriDocsPrint;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import static com.ib.system.utils.SearchUtils.asInteger;
import static com.ib.system.utils.SearchUtils.asString;

/**
 * Печат/Справка на регистри на документи
 *
 * @author ivanc
 */

@Named
@ViewScoped
public class PrintRegistri2 extends IndexUIbean  implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5579793411253766781L;
	private static final Logger LOGGER = LoggerFactory.getLogger(PrintRegistri2.class);
	private Integer idCurRegistratura;
	private DocSearch searchDoc;	              // парам. за списъка док.
	private Integer periodR;
	private Date decodeDate;
	private String title;
	private List<SelectItem> listRegistri;
	private transient List<Object[]> docList; 
	private transient List<Object[]> registerDocList;
	private transient List<Object[]> registriList;
	private transient Object[] dopInfo;
	private LazyDataModelSQL2Array docsList;  
	private boolean isView = false;
	private boolean useCommonRegisters=false;
	private String sortCol=null;
	private List<String> inclOpt=null;
	private int currentLang=getCurrentLang();
	public static final String  PREGFORM = "formPrintRegisterDoc";
	public static final  String ERRGETDATAORD = "prRegDoc.errGetDataORD";
	public static final  String ERREXPORTORD = "prRegDoc.errorGetData";
	
	/** */
	@PostConstruct
	void initData() {
		LOGGER.debug("!!! PostConstruct PrintRegistri2 !!!");
		this.docList = new ArrayList<>(); 
		this.registerDocList = new ArrayList<>();
		this.registriList=new ArrayList<>();
		this.listRegistri = new ArrayList<>();
		this.isView = false;
		this.useCommonRegisters=false;
		
		this.idCurRegistratura=getUserData(UserData.class).getRegistratura();
				
		setDecodeDate(new Date());
		clearFilter();
		this.periodR = 9; // този месец	
		changePeriodR();
		getReportTitles();// Заглавия на справката
		
		// Изгражда List<SelectItem> listRegistri за текущата регистратура. 						
		if (this.idCurRegistratura != null) {
			try {
				if ("1".equals(getSystemData().getSettingsValue("delo.useCommonRegisters")))
					this.setUseCommonRegisters(true);
				
				JPA.getUtil().runWithClose(() -> this.setRegistriList(new RegisterDAO(getUserData()).findByRegistraturaId(this.idCurRegistratura, this.isUseCommonRegisters())));
				
				SelectItem el=null;
				for (Object[] item: this.registriList) {
					if (null!=item[0] && null!=item[2]) {
						el=new SelectItem(asInteger(item[0]), asString(item[2]));
						this.listRegistri.add(el);
					}
				}
	
			
			} catch (BaseException e) {
				LOGGER.error("Грешка при зареждане данните на регистрите! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}
		}

	}

	/**
	 * @return the idCurRegistratura
	 */
	public Integer getIdCurRegistratura() {
		return idCurRegistratura;
	}

	/**
	 * @param idCurRegistratura the idCurRegistratura to set
	 */
	public void setIdCurRegistratura(Integer idCurRegistratura) {
		this.idCurRegistratura = idCurRegistratura;
	}

	/**
	 * @return the searchDoc
	 */
	public DocSearch getSearchDoc() {
		return searchDoc;
	}

	/**
	 * @param searchDoc the searchDoc to set
	 */
	public void setSearchDoc(DocSearch searchDoc) {
		this.searchDoc = searchDoc;
	}

	/**
	 * @return the periodR
	 */
	public Integer getPeriodR() {
		return periodR;
	}

	/**
	 * @param periodR the periodR to set
	 */
	public void setPeriodR(Integer periodR) {
		this.periodR = periodR;
	}

	/**
	 * @return the decodeDate
	 */
	public Date getDecodeDate() {
		return decodeDate;
	}

	/**
	 * @param decodeDate the decodeDate to set
	 */
	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}
	
	
	public void clearFilter() {
		this.searchDoc = new DocSearch(getUserData(UserData.class).getRegistratura());	
//		this.periodR = 9; // този месец	
		this.periodR = null; 
		this.setDocsList(null);
		this.inclOpt=null;
	}
	

	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	public void changePeriodR () {
		
    	if (this.periodR != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodR);
			searchDoc.setDocDateFrom(di[0]);
			searchDoc.setDocDateTo(di[1]);		
    	} else {
    		searchDoc.setDocDateFrom(null);
			searchDoc.setDocDateTo(null);
		}
    }
	
	public void changeDate() { 
		this.setPeriodR(null);
				
	}

	/**
	 * @return the listRegistri
	 */
	public List<SelectItem> getListRegistri() {
		return listRegistri;
	}

	/**
	 * @param listRegistri the listRegistri to set
	 */
	public void setListRegistri(List<SelectItem> listRegistri) {
		this.listRegistri = listRegistri;
	}
	
		
	public void actionSearch() {
		if(checkDataSearch()) { 
			searchDoc.buildQueryDocList(getUserData(), isView);
			this.docsList = new LazyDataModelSQL2Array(searchDoc, "A4 DESC"); 
			this.docsList.getRowCount();
			
			//PF 10 TODO
			/*DataTable dT = (DataTable) FacesContext.getCurrentInstance().getViewRoot().findComponent("formPrintRegisterDoc:tblDocsPR2");
			dT.setSortOrder("DESCENDING");*/
					
			
		}		
	}
	
	/**
	 * проверка за задължителни полета преди търсене
	 */
	public boolean checkDataSearch() {
		boolean flagS = true;	
	
		if(null==searchDoc.getRegisterId()) {
			JSFUtils.addMessage(PREGFORM+":register",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docu.register")));
			flagS = false;
		}
		if(null==searchDoc.getDocDateFrom()) {
			JSFUtils.addMessage(PREGFORM+":dateOtReg",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(UI_LABELS, "general.dataOt")));
			flagS = false;
		}
		if(null==searchDoc.getDocDateTo()) {
			JSFUtils.addMessage(PREGFORM+":dateDoReg",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(UI_LABELS, "general.dataDo")));
			flagS = false;
		}
		
		
		
		return flagS ;
	}
	
	public String exportOpis() {
		
		String fileName = "RegisterDocumentiOpis "+new SimpleDateFormat("dd.MM.yyyy").format(new Date()) ;
		
		String contentType = null;
		
		int expType;
		if (JSFUtils.getRequestParameter("expT") != null && !"".equals(JSFUtils.getRequestParameter("expT"))) {
			expType = (Integer.valueOf(JSFUtils.getRequestParameter("expT"))).intValue();
		}else {
			expType=OmbConstants.EXPORT_HTML;
		}
		
		
		switch (expType) {
		/*case OmbConstants.EXPORT_HTML:
			contentType = "text/html; charset=UTF-8";
			fileName += ".html";
			break;*/
		case OmbConstants.EXPORT_WORD:
			contentType = "application/vnd.ms-word;  charset=UTF-8";
			fileName += ".doc";
			break;
	    case OmbConstants.EXPORT_EXCEL:
            contentType = "application/vnd.ms-excel; charset=UTF-8";
            fileName += ".xls";
            break;
	    default:    
	    	contentType = "text/html; charset=UTF-8";
			fileName += ".html";
			break;
		}
		
				
		// Извлича всички данни, необходими за справката.
		DataTable dataTable = (DataTable) FacesContext.getCurrentInstance().getViewRoot().findComponent("formPrintRegisterDoc:tblDocsPR2");

		if (null!=dataTable) { //Взема данни за сортирането в DataTable
			Iterator<SortMeta> it = dataTable.getSortByAsMap().values().iterator();
			String ord=null;
			SortMeta sm;
			
			while (it.hasNext()) {
				sm = it.next();
				if (null!= sm && null!=sm.getOrder()) {
					ord = sm.getOrder().toString().trim().toUpperCase();
					if (!ord.equals("UNSORTED")){
						this.sortCol = sm.getField().toUpperCase();
						break;
					}

				}

			}
			
		
			defineSortCol();
			
			ord=defineOrder(ord);
			
			try {
				this.registerDocList = new RegisterDocsPrintDAO(getUserData()).selectRegisterDocs(this.idCurRegistratura, searchDoc.getRegisterId(), searchDoc.getDocDateFrom(), searchDoc.getDocDateTo(), this.getSortCol(), ord);
				
				if (null!=this.registerDocList && ! this.registerDocList.isEmpty())	{
					RegistriDocsPrint regDpr = new RegistriDocsPrint();
					regDpr.registerDocsExport(this.registerDocList, searchDoc.getRegisterId(), searchDoc.getDocDateFrom(), searchDoc.getDocDateTo(), this.inclOpt, contentType, fileName);
					
				}else {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "opis.noResuls"));
				}
				
			} catch (DbErrorException e) {
				LOGGER.error(getMessageResourceString(beanMessages, ERRGETDATAORD), e.toString());
				
			} 

			
		}else {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "printReg.errGetParamDT"));
		}
		
		return null;
		

	}


	/**
	 * @return the docsList
	 */
	public LazyDataModelSQL2Array getDocsList() {
		return docsList;
	}

	/**
	 * @param docsList the docsList to set
	 */
	public void setDocsList(LazyDataModelSQL2Array docsList) {
		this.docsList = docsList;
	}

	/**
	 * @return the docList
	 */
	public List<Object[]> getDocList() {
		return docList;
	}

	/**
	 * @param docList the docList to set
	 */
	public void setDocList(List<Object[]> docList) {
		this.docList = docList;
	}

	/**
	 * @return the registerDocList
	 */
	public List<Object[]> getRegisterDocList() {
		return registerDocList;
	}

	/**
	 * @param registerDocList the registerDocList to set
	 */
	public void setRegisterDocList(List<Object[]> registerDocList) {
		this.registerDocList = registerDocList;
	}

	/**
	 * @return the sortCol
	 */
	public String getSortCol() {
		return sortCol;
	}

	/**
	 * @param sortCol the sortCol to set
	 */
	public void setSortCol(String sortCol) {
		this.sortCol = sortCol;
	}

	/**
	 * @return the inclOpt
	 */
	public List<String> getInclOpt() {
		return inclOpt;
	}

	/**
	 * @param inclOpt the inclOpt to set
	 */
	public void setInclOpt(List<String> inclOpt) {
		this.inclOpt = inclOpt;
	}

	/**
	 * @return the registriList
	 */
	public List<Object[]> getRegistriList() {
		return registriList;
	}

	/**
	 * @param registriList the registriList to set
	 */
	public List<Object[]> setRegistriList(List<Object[]> registriList) {
		this.registriList = registriList;
		return registriList;
	}

	/**
	 * @return the useCommonRegisters
	 */
	public boolean isUseCommonRegisters() {
		return useCommonRegisters;
	}

	/**
	 * @param useCommonRegisters the useCommonRegisters to set
	 */
	public void setUseCommonRegisters(boolean useCommonRegisters) {
		this.useCommonRegisters = useCommonRegisters;
	}


	
	
/******************************** EXPORTS **********************************/
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. 	 */
	
	public void postProcessXLS(Object document) {

		
		new CustomExpPreProcess().postProcessXLS(document, this.title, this.dopInfo, null, null);	
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката 
	 */
	public void preProcessPDF(Object document) {


		try{
			
			new CustomExpPreProcess().preProcessPDF(document, this.title, this.dopInfo, null, null);		
						
		} catch (IOException e) {
			LOGGER.error(e.getMessage(),e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "regNull.errExport"));
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
	
	public void getReportTitles() {
		
		this.dopInfo=new Object[2];

		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
			this.title = getMessageResourceString(LABELS, "printReg.pril2Doc");

			if (null!=searchDoc.getRegisterId())
				this.dopInfo[0]=getMessageResourceString(LABELS, "docu.register")+": "+ getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REGISTRI, searchDoc.getRegisterId(), currentLang, new Date());
			
			String period=getMessageResourceString(LABELS, "doc.lstPeriod")+": ";
			
			if (null!=searchDoc.getDocDateFrom())
				period+=" "+getMessageResourceString(LABELS, "docu.ot")+" " +sdf.format(searchDoc.getDocDateFrom());
				
			if (null!=searchDoc.getDocDateTo())
				period+=" "+getMessageResourceString(LABELS, "refDeleg.do")+" "+ sdf.format(searchDoc.getDocDateTo());
				
			this.dopInfo[1]=period;
			
		
		} catch (Exception e) {	
			LOGGER.error(e.getMessage(),e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "regNull.errExport"));
			return;
		} 	
		
		
	}
	
	
	public void defineSortCol() {
		if (null==getSortCol()) {
			this.setSortCol("A4");
		}else if(getSortCol().equals("A2")){
			this.setSortCol("A5");
		}else if(getSortCol().equals("A3")){
			this.setSortCol("A6");
		}
	}
	
	public String defineOrder(String ord) {
		
		if (null!=ord) { 
			if(ord.equalsIgnoreCase("DESCENDING")) {
				ord="ASC";
			}else if(ord.equalsIgnoreCase("ASCENDING")) {
				ord="DESC";
			}else {
				ord="DESC";
			}
		}else {
			ord="DESC";
		}
		return ord;
	}

	/**
	 * @return the dopInfo
	 */
	public Object[] getDopInfo() {
		return dopInfo;
	}

	/**
	 * @param dopInfo the dopInfo to set
	 */
	public void setDopInfo(Object[] dopInfo) {
		this.dopInfo = dopInfo;
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	
}

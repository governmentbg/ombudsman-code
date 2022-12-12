package com.ib.omb.beans;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.context.Flash;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.faces.view.facelets.FaceletContext;
import javax.inject.Inject;
import javax.inject.Named;

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
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

@Named
@ViewScoped
public class DocList  extends IndexUIbean   {

	/**
	 * Въвеждане и корекция на деловодни документи
	 */
	private static final long serialVersionUID = 507326780295416575L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocList.class);

	public static final String  DOCLISTTFORM = "docListForm";	
	public static final String SELECTED_DOCS = "selectedDocuments";
	private Date decodeDate;

	private DocSearch searchDoc;	              // парам. за списъка док.
	private Integer periodR;
	private Integer periodRes; //период на дата на получаване
	private Integer periodAnswer;//период на дата на очакване на отговор
	private Integer periodAssign;//период на дата на поставяне
	
	private List<SelectItem>	  docTypeList;
	private List<SystemClassif> docsVidClassif;  // заради autocomple (selectManyModalA)
	private List<SystemClassif> docsRegistriClassif;
	//private List<SystemClassif> docsRegistraturiClassif;
	private List<SystemClassif> docsTaskAssignClassif;
	//private List<SystemClassif> docsTopicClassif;
	private LazyDataModelSQL2Array docsList;      // списък документи 
	//private boolean showRegList = false;//дали потребителят има пълен достъп за разглеждане
	//private boolean showRegistaturiList = true; //ако е само една регистратура,да не се показва
	private String txtCorresp;
	private String triCheckWork = "0"; //
	private boolean visibleCheckWork = false;
	private Map<Integer, Object> specificsRegister;  // само за текушата регистратура CODE_CLASSIF_REGISTRI_SORTED=148
	private boolean isView = false;
	//private boolean rnDocEQ = false;
	//private List<SelectItem> registraturaList;
 
	private List<Object[]> docSelectedAllM=new ArrayList<>();
	private List<Object[]> docSelectedTmp = new ArrayList<>();  


	private boolean showWorkRegOfficial = true; // да се показва ли Регистрирани като официални при избор на работен
	private boolean isOkToRender; // да се показва ли бутон за пълнотекстово търсене
	private Integer settingMembers; // Поддържа се списък с участници в документа
	
//	private List<SystemClassif> rolesMembersClassif;
	
	@Inject
	private Flash flash;
	
	private boolean fillTextSearch = false;
	
	//TODO Проверка за заключен документ!!
		
	/** */
	@PostConstruct
	void initData() {
		// TODO вика се при отваряне на документ !?!?!
		try {
						
			decodeDate = new Date();			
			//periodR = 9; // този месец	
			//changePeriodR();
			//String fullTextSetting = getSystemData().getSettingsValue("general.fulltextsearch.enabled");
			//isOkToRender = !("0".equals(fullTextSetting)) ? true : false;
			isOkToRender = false;
			FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
			String isViewStr = (String) faceletContext.getAttribute("isView");
			if(isViewStr != null && !isViewStr.contentEquals("null")) {
				if(isViewStr.contentEquals("1"))
					isView = true;
			}
			if(isView) {
				docTypeList = createItemsList(false, OmbConstants.CODE_CLASSIF_DOC_TYPE, this.decodeDate, true);
			}else {
				docTypeList = createItemsList(true, OmbConstants.CODE_CLASSIF_DOC_TYPE, this.decodeDate, true);
				String setting = getSystemData().getSettingsValue("delo.workUpdateRegOfficial");
				this.showWorkRegOfficial = setting == null || "1".equals(setting); // ако е няма настроката приемем че трябва да се вижда
			}
//			showRegList= isView && (getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_FULL_VIEW) ||
//					getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_DOC_DELO_FULL_EDIT));
			//setRegistraturaList(createItemsList(true, OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS, this.decodeDate, false));
			clearFilter();
			this.setSettingMembers(((SystemData) getSystemData()).getRegistraturaSetting(getUserData(UserData.class).getRegistratura(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_7));
			//docTypeList.add(new SelectItem(null,"всички"));
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при инициализиране на филтър за търсене на документи! ", e);
		} catch (UnexpectedResultException e) {
			LOGGER.error("Грешка при инициализиране на филтър за търсене на документи! ", e);
		}
	}

	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClear() {
		clearFilter();
		changePeriodR();
		docsList = null;
		
	}
	
	public void clearFilter() {
		if(isView) {
			searchDoc = new DocSearch(null);
//			if (!this.showRegList || (this.registraturaList != null && this.registraturaList.size() == 1)) {
//				// ако не е показан избора за регистратура или е показан но е само един 
				specificsRegister = Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA, getUserData(UserData.class).getRegistratura());
//			}
		}else {
			Integer registratura = getUserData(UserData.class).getRegistratura();
			searchDoc = new DocSearch(registratura);
			specificsRegister = Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA, registratura);
		}
		
		// за сега стойностите по подразбиране са - вх. и собствени 
		//  TODO Да се добавят ограничения в зависмисот от правата; в зависимост от това от къде се вика филтъра...
//		searchDoc.getDocTypeArr()[0] = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN; 
//		searchDoc.getDocTypeArr()[1] = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN;	
		periodR = null;
		periodRes = null;
		periodAnswer = null;
		periodAssign = null;
		docsRegistriClassif = null;
//		rolesMembersClassif= null;
		docsVidClassif = null;
		docsTaskAssignClassif = null;
//		docsRegistraturiClassif = null;
//		docsTopicClassif = null;
		txtCorresp = "";
		triCheckWork = "0";
		visibleCheckWork = false;
		fillTextSearch = false;
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
	
	public void changePeriodRes () {
		
    	if (this.periodRes != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodRes);
			searchDoc.setReceiveDateFrom(di[0]);
			searchDoc.setReceiveDateTo(di[1]);		
    	} else {
    		searchDoc.setReceiveDateFrom(null);
			searchDoc.setReceiveDateTo(null);
		}
    }
	
	public void changeDateRes() { 
		this.setPeriodRes(null);
	}
	
public void changePeriodAnswer () {
		
    	if (this.periodAnswer!= null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodAnswer);
			searchDoc.setWaitAnswerDateFrom(di[0]);
			searchDoc.setWaitAnswerDateTo(di[1]);	
    	} else {
    		searchDoc.setWaitAnswerDateFrom(null);
			searchDoc.setWaitAnswerDateTo(null);
		}
    }
	
	public void changeDateAnswer() { 
		this.setPeriodAnswer(null);
	}

public void changePeriodAssign () {
		
    	if (this.periodAssign!= null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodAssign);
			searchDoc.setTaskDateFrom(di[0]);
			searchDoc.setTaskDateTo(di[1]);	
    	} else {
    		searchDoc.setTaskDateFrom(null);
			searchDoc.setTaskDateTo(null);
		}
    }
	
	public void changeDateAssign() { 
		this.setPeriodAssign(null);
	}
	/** 
	 * TODO -още критерии....
	 * Списък с документи по зададени критерии 
	 */
	public void actionSearch(){
		
		if(docSelectedAllM!=null) {
			docSelectedAllM.clear();
		}
		if(docSelectedTmp!=null) {
			docSelectedTmp.clear();
		}
		//searchDoc.setDocTypeArr(selectedDocType);
//		 searchDoc.setDocType(new ArrayList<Integer>());
//		 for (int i = 0; i < selectedDocType.length; i++) {
//			searchDoc.getDocType().add(selectedDocType[i]);
//		 }
		 if("0".equals(triCheckWork)) {
			searchDoc.setNullWorkOffId(null);
		 }else if("1".equals(triCheckWork)) {
			searchDoc.setNullWorkOffId(false);
		 }else if("2".equals(triCheckWork)) {
			searchDoc.setNullWorkOffId(true);
		 }
		 
		 if (!isView // само за актуализация
			 && (searchDoc.getDocTypeArr() == null || searchDoc.getDocTypeArr().length == 0) // ако нищо не е избрано
			 && docTypeList.size() < 3 // и все пак да няма достъп до всички
			 && searchDoc.getDocTypeEditList() == null) { // и вече да не са сетнати позволените му
			
			 if (docTypeList.isEmpty()) {
				 searchDoc.setDocTypeEditList(Arrays.asList(-1));
			 } else {
				 List<Integer> allowedTypes = new ArrayList<>(docTypeList.size());
				 for (SelectItem item : docTypeList) {
					 allowedTypes.add((Integer) item.getValue());
				 }
				 searchDoc.setDocTypeEditList(allowedTypes);
			 }
		 }
		 
		 if (!showWorkRegOfficial) { // ако не се показва значи е забранено да се актуализират
			 searchDoc.setUpdWorkRegOffForbidden(true); 
		 }
		 
		 searchDoc.buildQueryDocList(getUserData(), isView);
		 
		 try {
			 String param1 = getSystemData().getSettingsValue("delo.journalSearchDeloDoc"); // да се журналира ли търсенето
			 if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
				// запис в журнала
				JPA.getUtil().runInTransaction(() -> SearchUtils.auditSearch(searchDoc, SysConstants.CODE_DEIN_SEARCH, getUserData().getUserId()));
			}
		 } catch (BaseException e) {
				LOGGER.error("Грешка при журналиране на търсене на документи! ", e);
		 } 
		 docsList = new LazyDataModelSQL2Array(searchDoc, "a4 desc"); 

	}	
	
	public String actionGoto(int i, Object[] row) {
		
		Integer idObj = (this.searchDoc.getDvijNotReturned()) ? ((Number) row[17]).intValue() : ((Number) row[0]).intValue();
		
		String result = "";
		if(i == 0) {
			result = "docEdit.jsf?faces-redirect=true&idObj=" + idObj;
		} else if(i == 1){
			result = "docView.xhtml?faces-redirect=true&idObj=" + idObj;
		} else if(i == 2) {
			result = "docPrereg.jsf?faces-redirect=true&idObj=" + idObj;
		}
		return result;
	}
	
	public void changeDocType() {
		visibleCheckWork = false;
		for(Integer item:searchDoc.getDocTypeArr()) {
			if(item!=null && item.intValue()==OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK) {
				visibleCheckWork = true;
				break;
			}
		}
		
		if(!visibleCheckWork) {
			triCheckWork="0";
		}
	}
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "doc.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport() , null, null);		
     
	}
	

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "doc.reportTitle");		
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
	
	/**
	 * подзаглавие за екпсорта - .............
	 */
	public Object[] dopInfoReport() {
		Object[] dopInf = null;
		dopInf = new Object[2];
		if(searchDoc.getDocDateFrom() != null && searchDoc.getDocDateTo() != null) {
			dopInf[0] = "период: "+DateUtils.printDate(searchDoc.getDocDateFrom()) + " - "+ DateUtils.printDate(searchDoc.getDocDateTo());
		} 
	
		return dopInf;
	}
	
	public void actionSelectCorresp() {
		
		if(searchDoc.getCodeRefCorresp()!=null) {
			try {
				txtCorresp = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS, searchDoc.getCodeRefCorresp(), getCurrentLang(), decodeDate);
				searchDoc.setNameCorresp(null);
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при разкодиране на кореспондент! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}	
	}
	
	public void actionChangeTxtCorresp() {
		
		searchDoc.setCodeRefCorresp(null) ;
		searchDoc.setNameCorresp(txtCorresp);
	}
	
	public void actionSearchFullText(){

		//searchDoc.setDocTypeArr(selectedDocType);
//		 searchDoc.setDocType(new ArrayList<Integer>());
//		 for (int i = 0; i < selectedDocType.length; i++) {
//			searchDoc.getDocType().add(selectedDocType[i]);
//		 }

		searchDoc.buildQueryDocListWithFulltext(getUserData(), isView);
		docsList = new LazyDataModelSQL2Array(searchDoc, "a1 desc");

	}
	
//	public void actionChangeReg() {
//		try {
//			if(searchDoc.getRegistraturaId()!=null) {		
//				specificsRegister = Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA,searchDoc.getRegistraturaId());
//				
//					this.setSettingMembers(((SystemData) getSystemData()).getRegistraturaSetting(searchDoc.getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_7));
//				
//			}else {
//				specificsRegister = Collections.singletonMap(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA, getUserData(UserData.class).getRegistratura());
//				this.setSettingMembers(((SystemData) getSystemData()).getRegistraturaSetting(getUserData(UserData.class).getRegistratura(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_7));
//			}
//		}
//		 catch (DbErrorException e) {
//			 LOGGER.error("Грешка при инициализиране на настройка на регистратура! ", e);
//		}
//		searchDoc.setRegisterIdList(null);
//		docsRegistriClassif = null;
//	}
	
	public String actionPredavane() {
		flash.put(DocList.SELECTED_DOCS, this.getDocSelectedAllM());
		return "docGrupovoPredavane.jsf?faces-redirect=true";
	}
	
	public String actionVrashtane() {
		flash.put(DocList.SELECTED_DOCS, this.getDocSelectedAllM());
		return "docGrupovoVrashtane.jsf?faces-redirect=true";
	}
	
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
		docSelectedTmp.clear();
		this.docsList.loadCountSql();
	}
	
	/*
	 * Множествен избор на документи
	 */
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
		    	List<Object[]> tmpLPageClass =  getDocsList().getResult();// rows from current page....    		
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

				
	public Date getDecodeDate() {
		return new Date(decodeDate.getTime()) ;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
	}

	public Integer getPeriodR() {
		return periodR;
	}

	public void setPeriodR(Integer periodR) {
		this.periodR = periodR;
	}

	public DocSearch getSearchDoc() {
		return searchDoc;
	}


	public void setSearchDoc(DocSearch searchDoc) {
		this.searchDoc = searchDoc;
	}


	public LazyDataModelSQL2Array getDocsList() {
		return docsList;
	}


	public void setDocsList(LazyDataModelSQL2Array docsList) {
		this.docsList = docsList;
	}


	public List<SelectItem> getDocTypeList() {
		return docTypeList;
	}


	public void setDocTypeList(List<SelectItem> docTypeList) {
		this.docTypeList = docTypeList;
	}


//	public boolean isRnDocEQ() {
//		return rnDocEQ;
//	}
//
//
//	public void setRnDocEQ(boolean rnDocEQ) {
//		this.rnDocEQ = rnDocEQ;
//	}

	public List<SystemClassif> getDocsVidClassif() {
		return docsVidClassif;
	}


	public void setDocsVidClassif(List<SystemClassif> docsVidClassif) {
		this.docsVidClassif = docsVidClassif;
	}

	public List<SystemClassif> getDocsRegistriClassif() {
		return docsRegistriClassif;
	}


	public void setDocsRegistriClassif(List<SystemClassif> docsRegistriClassif) {
		this.docsRegistriClassif = docsRegistriClassif;
	}

	public Integer getPeriodRes() {
		return periodRes;
	}


	public void setPeriodRes(Integer periodRes) {
		this.periodRes = periodRes;
	}


	public Integer getPeriodAnswer() {
		return periodAnswer;
	}


	public void setPeriodAnswer(Integer periodAnswer) {
		this.periodAnswer = periodAnswer;
	}


	public Integer getPeriodAssign() {
		return periodAssign;
	}

	public void setPeriodAssign(Integer periodAssign) {
		this.periodAssign = periodAssign;
	}

	public List<SystemClassif> getDocsTaskAssignClassif() {
		return docsTaskAssignClassif;
	}

	public void setDocsTaskAssignClassif(List<SystemClassif> docsTaskAssignClassif) {
		this.docsTaskAssignClassif = docsTaskAssignClassif;
	}

	public String getTxtCorresp() {
		return txtCorresp;
	}

	public void setTxtCorresp(String txtCorresp) {
		this.txtCorresp = txtCorresp;
	}

	public Map<Integer, Object> getSpecificsRegister() {
		return this.specificsRegister;
	}

	public void setSpecificsRegister(Map<Integer, Object> specificsRegister) {
		this.specificsRegister = specificsRegister;
	}

	public String getTriCheckWork() {
		return triCheckWork;
	}

	public void setTriCheckWork(String triCheckWork) {
		this.triCheckWork = triCheckWork;
	}

	public boolean isVisibleCheckWork() {
		return visibleCheckWork;
	}

	public void setVisibleCheckWork(boolean visibleCheckWork) {
		this.visibleCheckWork = visibleCheckWork;
	}
	
	
	public List<Object[]> getDocSelectedTmp() {
		return docSelectedTmp;
	}

	public void setDocSelectedTmp(List<Object[]> docSelectedTmp) {
		this.docSelectedTmp = docSelectedTmp;
	}
	
	public List<Object[]> getDocSelectedAllM() {
		return docSelectedAllM;
	}

	public void setDocSelectedAllM(List<Object[]> docSelectedAllM) {
		this.docSelectedAllM = docSelectedAllM;
	}
	
//	public List<SelectItem> getRegistraturaList() {
//		return registraturaList;
//	}
//
//	public void setRegistraturaList(List<SelectItem> registraturaList) {
//		this.registraturaList = registraturaList;
//	}

//	public boolean isShowRegList() {
//		return showRegList;
//	}
//
//	public void setShowRegList(boolean showRegList) {
//		this.showRegList = showRegList;
//	}

	public boolean isShowWorkRegOfficial() {
		return showWorkRegOfficial;
	}

//	public List<SystemClassif> getDocsTopicClassif() {
//		return docsTopicClassif;
//	}
//
//	public void setDocsTopicClassif(List<SystemClassif> docsTopicClassif) {
//		this.docsTopicClassif = docsTopicClassif;
//	}

//	public List<SystemClassif> getDocsRegistraturiClassif() {
//		return docsRegistraturiClassif;
//	}
//
//	public void setDocsRegistraturiClassif(List<SystemClassif> docsRegistraturiClassif) {
//		this.docsRegistraturiClassif = docsRegistraturiClassif;
//	}

	public boolean getIsOkToRender() {
		return isOkToRender;
	}

	public void setIsOkToRender(boolean isOkToRender) {
		this.isOkToRender = isOkToRender;
	}
	
	public void actionSearchNew() {
		
		if(fillTextSearch) {
			actionSearchFullText();
 		} else {
 			actionSearch();
 		}
		
	}

	public boolean isFillTextSearch() {
		return fillTextSearch;
	}

	public void setFillTextSearch(boolean fillTextSearch) {
		this.fillTextSearch = fillTextSearch;
	}

//	public List<SystemClassif> getRolesMembersClassif() {
//		return rolesMembersClassif;
//	}
//
//	public void setRolesMembersClassif(List<SystemClassif> rolesMembersClassif) {
//		this.rolesMembersClassif = rolesMembersClassif;
//	}

	public Integer getSettingMembers() {
		return settingMembers;
	}

	public void setSettingMembers(Integer settingMembers) {
		this.settingMembers = settingMembers;
	}
	
}
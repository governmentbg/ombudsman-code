package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.LockObjectDAO;
import com.ib.omb.db.dao.ProcDefDAO;
import com.ib.omb.db.dao.ProcExeDAO;
import com.ib.omb.db.dao.ProcExeEtapDAO;
import com.ib.omb.db.dto.ProcDef;
import com.ib.omb.db.dto.ProcDefEtap;
import com.ib.omb.db.dto.ProcExe;
import com.ib.omb.db.dto.ProcExeEtap;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

@Named
@ViewScoped
public class ProcExeEdit extends IndexUIbean  implements Serializable {		
	
	/**
	 * Въвеждане / актуализация на процедура
	 * 
	 */
	private static final long serialVersionUID = 6625939361418061152L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcExeEdit.class);
	
	private static final String ID_OBJ = "idObj";
	private static final String SCROLL_TO_ERRORS = "scrollToErrors()";
	
	private transient ProcExeDAO procExeDAO;
	
	private ProcExe procExe;
	private ProcDef procDef;
	
	private String docRnFull;
	private boolean viewBtn;
	private Integer codeRefExe;
	private List<Files> filesListForDefProc;
	
	private LazyDataModelSQL2Array etapExeList;
	private List<Object[]> selEtapList;
	private Map<Long, String> colorStatus; // смяна на цветовете на текста спрямо статуса
	
	private List<ProcDefEtap> defEtapsList;
	private boolean viewBtnStopProc;
	private boolean viewBtnRestoreProc;
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct - ProcExeEdit!!!");
		
		try {
		
			boolean fLockOk = true;			
			
			this.procDef = new ProcDef();
			this.procExeDAO = new ProcExeDAO(getUserData());
			this.viewBtn = false;
			this.selEtapList = new ArrayList<>();
			this.defEtapsList = new ArrayList<>();		
			
			if (JSFUtils.getRequestParameter(ID_OBJ) != null && !"".equals(JSFUtils.getRequestParameter(ID_OBJ))) {
				
				Integer idObj = Integer.valueOf(JSFUtils.getRequestParameter(ID_OBJ));
				
				// проверка за заключена процедура
				fLockOk = checkForLock(idObj);
				
				if (fLockOk) {
				// проверка за достъп
					lockProc(idObj);
				// отключване на всички обекти за потребителя(userId) и заключване на процедура, за да не може да се актуализира от друг			
				
					if (idObj != null) {
						
						JPA.getUtil().runWithClose(() -> {
							
							this.procExe = new ProcExe();
							
							this.procExe = this.procExeDAO.findById(idObj);
							
							this.procDef = new ProcDefDAO(getUserData()).findById(this.procExe.getDefId());
							
							this.defEtapsList = new ProcDefDAO(getUserData()).selectDefEtapList(this.procDef.getId(), null);
							
							// извличане на файловете към дефиницията на процедурата от таблица с файловете
							this.filesListForDefProc = new FilesDAO(getUserData()).selectByFileObject(this.procDef.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_DEF);
							
							if (this.procExe.getStatus() == OmbConstants.CODE_ZNACHENIE_PROC_STAT_WAIT || this.procExe.getStatus() == OmbConstants.CODE_ZNACHENIE_PROC_STAT_EXE 								
									&& (this.procExe.getCodeRef() != null && getUserData().getUserId().equals(this.procExe.getCodeRef())
									 || getUserData().hasAccess(OmbConstants.CODE_CLASSIF_BUSINESS_ROLE, OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_PROC_ADMIN)
									 || getUserData(UserData.class).getUserAccess().equals(this.procExe.getCodeRef())
									 || this.procExeDAO.checkEtapControlUser(this.procExe.getId(), this.procExe.getStatus()))) {
								
								this.viewBtnStopProc = true;
							
							} else {							
								this.viewBtnStopProc = false;
							}
							
							if (this.procExe.getStatus() == OmbConstants.CODE_ZNACHENIE_PROC_STAT_STOP && this.procDef.getStatus() == OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE								
									&& (this.procExe.getCodeRef() != null && getUserData().getUserId().equals(this.procExe.getCodeRef())
									 || getUserData().hasAccess(OmbConstants.CODE_CLASSIF_BUSINESS_ROLE, OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_PROC_ADMIN)
									 || getUserData(UserData.class).getUserAccess().equals(this.procExe.getCodeRef())
									 || this.procExeDAO.checkEtapControlUser(this.procExe.getId(), this.procExe.getStatus()))) {
								
								this.viewBtnRestoreProc = true;
							
							} else {
								this.viewBtnRestoreProc = false;
							}
							
						});					
						
						if (this.procExe.getDocId() != null) {
							Object[] docData = new DocDAO(getUserData()).findDocData(this.procExe.getDocId());
							this.docRnFull = DocDAO.formRnDocDate(docData[1], docData[2], docData[5]) + " г.";
						}
						
						if (this.procExe.getCodeRef() != null && getUserData().getUserId().equals(this.procExe.getCodeRef())
							 || getUserData().hasAccess(OmbConstants.CODE_CLASSIF_BUSINESS_ROLE, OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_PROC_ADMIN)
							 || getUserData(UserData.class).getUserAccess().equals(this.procExe.getCodeRef())) {
							
							this.viewBtn = true;
						}
						
						this.codeRefExe = this.procExe.getCodeRef();
						
						actionSearchEtapExeList();
					}				
				}
			}
			
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на процедурата! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	public void actionSearchEtapExeList() {
		
		SelectMetadata smd = this.procExeDAO.createSelectEtapExeList(this.procExe.getId(), this.procExe.getDefId());
		String defaultSortColumn = "a0";
		this.etapExeList = new LazyDataModelSQL2Array(smd, defaultSortColumn);
	}
	
	public void actionChangeCodeRef() {
		
		if (this.codeRefExe == null) {
			JSFUtils.addMessage("formProcedura:selOtgIzpal:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefList.otgIzpal")));
			return;
		
		} else {
			
			try {
				
				JPA.getUtil().runInTransaction(() -> this.procExe = this.procExeDAO.changeCodeRef(this.procExe, this.codeRefExe, getSystemData()));
				
				String dialogWidgetVar = "PF('modalSelOtgIzp').hide();";
				PrimeFaces.current().executeScript(dialogWidgetVar);
				
				actionSearchEtapExeList();
				
			} catch (BaseException e) {			
				LOGGER.error("Грешка при промяна на отговорен за изпълнението! ", e);	
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}			
		}		
	}
	
	public void actionStopProc() {
		
		if (SearchUtils.isEmpty(this.procExe.getStopReason())) { 
			JSFUtils.addMessage("formProcedura:txtStopReason", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procExeEdit.reasonForStop")));
			return;
		
		} else {
			
			try {
				
				JPA.getUtil().runInTransaction(() -> this.procExe = this.procExeDAO.stopProc(this.procExe, this.procExe.getStopReason(), false, null, getSystemData()));
				
				String dialogWidgetVar = "PF('modalStopProc').hide();";
				PrimeFaces.current().executeScript(dialogWidgetVar);
				
				actionSearchEtapExeList();
				
				this.viewBtnStopProc = false;
				this.viewBtnRestoreProc = true;				
				
			} catch (BaseException e) {			
				LOGGER.error("Грешка при прекратяване на изпълнението! ", e);	
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}			
		}		
	}
	
	public void actionRestoreProc() {
		
		try {
			
			JPA.getUtil().runInTransaction(() -> this.procExe = this.procExeDAO.restartProc(this.procExe, getSystemData()));
			
			String dialogWidgetVar = "PF('modalRestoreProc').hide();";
			PrimeFaces.current().executeScript(dialogWidgetVar);
			
			actionSearchEtapExeList();
			
			this.viewBtnRestoreProc = false;
			this.viewBtnStopProc = true;
			
		} catch (BaseException e) {			
			LOGGER.error("Грешка при възобновяването на процедурата! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	
		
	}
	
	//Това се налага защото jsf HashMap не работи с integer
	public Long castLong(Integer i) {
		
		if(i == null) return -1L;		
		return Long.valueOf(i);		
	}
	
	public void actionEtapReturn() {
		
		if (this.selEtapList.isEmpty()) {			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procExeEdit.selEtapForReturn"));
			PrimeFaces.current().executeScript(SCROLL_TO_ERRORS);
			return;
		
		} else {
			
			if (this.selEtapList.size() > 1) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procExeEdit.selOnlyOneEtap"));
				PrimeFaces.current().executeScript(SCROLL_TO_ERRORS);
				return;
			
			} else  if (this.selEtapList.size() == 1) {
				
				for (Object[] obj : selEtapList) {
					
					if (SearchUtils.asInteger(obj[11]).equals(OmbConstants.CODE_ZNACHENIE_ETAP_STAT_IZP) || SearchUtils.asInteger(obj[11]).equals(OmbConstants.CODE_ZNACHENIE_ETAP_STAT_IZP_SROK)) {
						PrimeFaces.current().executeScript("PF('hiddenReturn').jq.click();");
					
					} else {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procExeEdit.selEtapStatIzp"));
						PrimeFaces.current().executeScript(SCROLL_TO_ERRORS);
						return;
					}					
				}				
			}
		}
	}
	
	public void actionReturnExec() {
		
		for (Object[] selEtap : selEtapList) {
			
			try {
				
				ProcExeEtap tmpEtap = new ProcExeEtapDAO(getUserData()).findById(SearchUtils.asInteger(selEtap[0]), false);
				
				JPA.getUtil().runInTransaction(() -> new ProcExeEtapDAO(getUserData()).returnEtap(this.procExe, tmpEtap, getSystemData()));
				
				actionSearchEtapExeList();
			
			} catch (BaseException e) {
				LOGGER.error("Грешка при връщане на изпълнението! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}		
	}
	
	public String actionGotoExeEtap(Integer idObj) {
		
		JSFUtils.addFlashScopeValue("unlock", 1);
		
		return "procExeEtapEdit.jsf?faces-redirect=true&idObj=" + idObj + "&idProc=" + this.procExe.getId();
	}
	
/******************************************************* LOCK & UNLOCK *******************************************************/	
	
	/**
	 * Проверка за заключена процедура 
	 * @param idObj
	 * @return
	 */
	private boolean checkForLock(Integer idObj) {
		boolean res = true;
		LockObjectDAO daoL = new LockObjectDAO();		
		
		try { 
			Object[] obj = daoL.check(getUserData().getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE, idObj);
			
			if (obj != null) {
				 res = false;
				 String msg = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, Integer.valueOf(obj[0].toString()), getUserData().getCurrentLang(), new Date())   
						       + " / " + DateUtils.printDate((Date)obj[1]);
				 
				 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,getMessageResourceString(LABELS, "procExeEdit.procLocked"), msg);
			}
		
		} catch (DbErrorException e) {
			
			LOGGER.error("Грешка при проверка за заключена процедура! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		return res;
	}
	
	/**
	 * Заключване на процедура, като преди това отключва всички обекти, заключени от потребителя
	 * @param idObj
	 */
	public void lockProc(Integer idObj) {	
		LOGGER.info("lockProc! = {}", ((UserData) getUserData()).getPreviousPage());		
		LockObjectDAO daoL = new LockObjectDAO();		
		
		try { 
			
			JPA.getUtil().runInTransaction(() ->  daoL.lock(getUserData().getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE, idObj, null));
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при заключване на процедура! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при заключване на процедура! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}			
	}

	
	/**
	 * при излизане от страницата - отключва обекта и да го освобождава за актуализация от друг потребител
	 */
	@PreDestroy
	public void unlockProc(){
        if (!((UserData) getUserData()).isReloadPage()) {
        	LOGGER.info("unlockData! = {}", ((UserData) getUserData()).getPreviousPage() );
        	unlockAll(true);
        	((UserData) getUserData()).setPreviousPage(null);
        }          
        ((UserData) getUserData()).setReloadPage(false); 
	}
	
	
	/**
	 * отключва всички обекти на потребителя - при излизане от страницата или при натискане на бутон "Нов"
	 */
	private void unlockAll(boolean all) {
		LOGGER.info("unlockProc! = {}", ((UserData) getUserData()).getPreviousPage());
		LockObjectDAO daoL = new LockObjectDAO();		
		
		try { 
			if (all) {
				JPA.getUtil().runInTransaction(() ->  daoL.unlock(getUserData().getUserId()));
			} 
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при отключване на процедура! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при отключване на процедура! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
	}
	
	/**************************************************** END LOCK & UNLOCK ****************************************************/
	

	public ProcExe getProcExe() {
		return procExe;
	}
	
	public void setProcExe(ProcExe procExe) {
		this.procExe = procExe;
	}
	
	public ProcDef getProcDef() {
		return procDef;
	}

	public void setProcDef(ProcDef procDef) {
		this.procDef = procDef;
	}

	public String getDocRnFull() {
		return docRnFull;
	}

	public void setDocRnFull(String docRnFull) {
		this.docRnFull = docRnFull;
	}

	public boolean isViewBtn() {
		return viewBtn;
	}

	public void setViewBtn(boolean viewBtn) {
		this.viewBtn = viewBtn;
	}

	public Integer getCodeRefExe() {
		return codeRefExe;
	}

	public void setCodeRefExe(Integer codeRefExe) {
		this.codeRefExe = codeRefExe;
	}

	public List<Files> getFilesListForDefProc() {
		return filesListForDefProc;
	}

	public void setFilesListForDefProc(List<Files> filesListForDefProc) {
		this.filesListForDefProc = filesListForDefProc;
	}

	public LazyDataModelSQL2Array getEtapExeList() {
		return etapExeList;
	}

	public void setEtapExeList(LazyDataModelSQL2Array etapExeList) {
		this.etapExeList = etapExeList;
	}

	public List<Object[]> getSelEtapList() {
		return selEtapList;
	}

	public void setSelEtapList(List<Object[]> selEtapList) {
		this.selEtapList = selEtapList;
	}

	public Map<Long, String> getColorStatus() {
		
		if (this.colorStatus == null) {
			
			this.colorStatus = new HashMap<>();
			
			this.colorStatus.put(SearchUtils.asLong(OmbConstants.CODE_ZNACHENIE_ETAP_STAT_WAIT), "color:#F28686;");
			this.colorStatus.put(SearchUtils.asLong(OmbConstants.CODE_ZNACHENIE_ETAP_STAT_DECISION), "color:#F28686;");
			this.colorStatus.put(SearchUtils.asLong(OmbConstants.CODE_ZNACHENIE_ETAP_STAT_EXE), "color:#99CE6B;");
			this.colorStatus.put(SearchUtils.asLong(OmbConstants.CODE_ZNACHENIE_ETAP_STAT_IZP), "color:#50A2B9;");
			this.colorStatus.put(SearchUtils.asLong(OmbConstants.CODE_ZNACHENIE_ETAP_STAT_IZP_SROK), "color:#50A2B9;");
		}
		
		return colorStatus;
	}	
	
	public void setColorStatus(Map<Long, String> colorStatus) {
		this.colorStatus = colorStatus;
	}
	
	public List<ProcDefEtap> getDefEtapsList() {
		return defEtapsList;
	}

	public void setDefEtapsList(List<ProcDefEtap> defEtapsList) {
		this.defEtapsList = defEtapsList;
	}

	public boolean isViewBtnStopProc() {
		return viewBtnStopProc;
	}

	public void setViewBtnStopProc(boolean viewBtnStopProc) {
		this.viewBtnStopProc = viewBtnStopProc;
	}

	public boolean isViewBtnRestoreProc() {
		return viewBtnRestoreProc;
	}

	public void setViewBtnRestoreProc(boolean viewBtnRestoreProc) {
		this.viewBtnRestoreProc = viewBtnRestoreProc;
	}

	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за списък с етапи за изпълнение на процедурата
	 */
	public void postProcessXLSProcEtapExeList(Object document) {
		
		String title = getMessageResourceString(LABELS, "procEtapExeList.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, null, null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за списък с етапи за изпълнение на процедурата
	 */
	public void preProcessPDFProcEtapExeList(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "procEtapExeList.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title, null, null, null);		
						
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
	
}
package com.ib.omb.beans;

import static com.ib.system.utils.SearchUtils.isEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import org.omnifaces.cdi.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocProtocolDAO;
import com.ib.omb.db.dao.LockObjectDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocReferent;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

@Named
@ViewScoped
public class DocArchive extends IndexUIbean implements Serializable {			
	
	/**
	 * Въвеждане / актуализация на протокол за предаване в учрежденски и Държавен архив дела
	 * 
	 */
	private static final long serialVersionUID = 3943740740276991343L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocArchive.class);
	
	private static final String ID_OBJ = "idObj";
	private static final String VID_PROT = "vidProt";
	private static final String FORM_DOC_ARCHIVE = "formDocArchive";
	private static final String DELO_ARCHIVE_PROTOCOL_UA = "delo.archiveProtocolUA";
	private static final String DELO_ARCHIVE_PROTOCOL_DA = "delo.archiveProtocolDA";
	
	private Date decodeDate = new Date();
	
	private transient DocProtocolDAO protocolDAO;
	private Doc docProtArchive;
	private Integer docVid;
	private String vidProt;
	
	private boolean avtomNo = true;        			 // true -  автоматично генериране на номер
	private boolean avtomNoDisabled = false;         // true - да се забрани достъпа до бутона за автоматичното генериране на номер
	
	private Map<Integer, Object> specificsRegister;  // за списъка с допустими регистри, в зависимост от типа документ
	private List<SystemClassif> classifRegsList;
	
	private List<Files> filesListForWork;
	private List<Files> filesListForOffic;
	
	private List<Object[]> selectedDela = new ArrayList<>();
	
	private LazyDataModelSQL2Array delaForArchive;
	private Map<Integer, Integer> vidAndStatusArchive;
	
	private boolean existNotArchiveDela;
	private int countDela;
	
	private Object[] selectedDoc;
	private String searchRnDoc;
	
	private Integer idOfficReg;
	private Integer idRegister;
	private boolean existRegister = false;
	
	private Date dateOfficProt = new Date();
	private String otnosnoOfficProt;
	
	private Integer[] docTypeArr; // за да ми зареди само документи от тип "собствен" при свързване с официален протокол
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct - DocArchive!!!");
		
		try {
			
			boolean fLockOk = true;
			
			this.docTypeArr = new Integer[]{ OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN};		
			this.protocolDAO = new DocProtocolDAO(2, getUserData());	
			
			this.specificsRegister = new HashMap<>();
			this.specificsRegister.put(OmbClassifAdapter.REGISTRI_INDEX_DOC_TYPE, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK);
			this.specificsRegister.put(OmbClassifAdapter.REGISTRI_INDEX_VALID, SysConstants.CODE_ZNACHENIE_DA);
			
			this.classifRegsList = getSystemData().queryClassification(getUserData(), OmbConstants.CODE_CLASSIF_REGISTRI, null, decodeDate,getCurrentLang(), this.specificsRegister);
			
			String protUA = null;
			String protDA = null;
			
			protUA = getSystemData().getSettingsValue("delo.archiveProtocolUA");	// 8		
			protDA = getSystemData().getSettingsValue("delo.archiveProtocolDA");	// 12
			
			this.vidAndStatusArchive = new HashMap<>();
			
			if(protUA != null) {
				this.vidAndStatusArchive.put(Integer.valueOf(protUA), Integer.valueOf(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_UA));
			}
			
			if(protDA != null) {
				this.vidAndStatusArchive.put(Integer.valueOf(protDA), Integer.valueOf(OmbConstants.CODE_ZNACHENIE_DELO_STATUS_ARCHIVED_DA));
			}		
			
			if (JSFUtils.getRequestParameter(VID_PROT) != null && !"".equals(JSFUtils.getRequestParameter(VID_PROT))) {
				
				this.docVid = Integer.valueOf(JSFUtils.getRequestParameter(VID_PROT));
				this.vidProt = "";
				
				if (protUA != null && this.docVid.equals(Integer.valueOf(protUA))) {
					this.vidProt = getSystemData().getSettingsDopInfo(DELO_ARCHIVE_PROTOCOL_UA);						
				}
				
				if (protDA != null && this.docVid.equals(Integer.valueOf(protDA))) {
					this.vidProt = getSystemData().getSettingsDopInfo(DELO_ARCHIVE_PROTOCOL_DA);						
				}
			}
			
			if (JSFUtils.getRequestParameter(ID_OBJ) != null && !"".equals(JSFUtils.getRequestParameter(ID_OBJ))) {
				
				Integer idObj = Integer.valueOf(JSFUtils.getRequestParameter(ID_OBJ));
				
				// проверка за заключен документ
				fLockOk = checkForLock(idObj);
				
				if (fLockOk) {
				// проверка за достъп
					lockDoc(idObj);
				// отключване на всички обекти за потребителя(userId) и заключване на док., за да не може да се актуализира от друг	
				
					if (idObj != null) {
						
						JPA.getUtil().runWithClose(() -> {
							
							this.docProtArchive = this.protocolDAO.findById(idObj);
		
							this.filesListForWork = new FilesDAO(getUserData()).selectByFileObject(this.docProtArchive.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
							
							this.filesListForOffic = new FilesDAO(getUserData()).selectByFileObject(this.docProtArchive.getWorkOffId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
							
						});
						
						this.otnosnoOfficProt = this.docProtArchive.getOtnosno();
						
						checkNumberDelaForArchive();
						
						actionSearch();
					}				
				}
			
			} else {
				
				actionNew();
				if (this.classifRegsList.size() == 1) {
					for (SystemClassif sc : classifRegsList) {
						this.docProtArchive.setRegisterId(sc.getCode());
					}
					 
				}
			}
			
			Object[] docSettings = this.protocolDAO.findDocSettings(getUserData(UserData.class).getRegistratura(), this.docVid, getSystemData());
			
			if (docSettings != null) {
				if(docSettings[1] != null) {
					this.idRegister = SearchUtils.asInteger(docSettings[1]);
					this.existRegister = true;
				}
				if(docSettings[3] != null) {
					this.idOfficReg = SearchUtils.asInteger(docSettings[3]);
				}
			}			

		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на протокол за предаване в учрежденски и Държавен архив дела!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	private boolean checkNumberDelaForArchive() {
		
		SelectMetadata smd = this.protocolDAO.createSelectArchiveProtocolDela(this.docProtArchive, Boolean.FALSE, this.vidAndStatusArchive);
		String defaultSortColumn = "A0";
		LazyDataModelSQL2Array tmpDocsList = new LazyDataModelSQL2Array(smd, defaultSortColumn);
		
		this.countDela = tmpDocsList.getRowCount();
		
		if (this.countDela > 0) {
			 this.existNotArchiveDela = true;
		} else {
			 this.existNotArchiveDela = false;
		}
		
		return this.existNotArchiveDela;
	}
	
	private boolean checkData() {
		
		boolean save = false;	
		
		if(!avtomNo && SearchUtils.isEmpty(this.docProtArchive.getRnDoc())) {
			JSFUtils.addMessage(FORM_DOC_ARCHIVE + ":regN", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docDestruction.regNumWorkProt")));
			save = true;
		}
		
		if(this.docProtArchive.getDocDate() == null) {
			JSFUtils.addMessage(FORM_DOC_ARCHIVE + ":dateProt", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docDestruction.dateWorkProt")));
			save = true;
		}
		
		if(this.docProtArchive.getRegisterId() == null) {
			JSFUtils.addMessage(FORM_DOC_ARCHIVE + ":registerId:аutoCompl", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.register")));
			save = true;
		}
		
		if(SearchUtils.isEmpty(this.docProtArchive.getOtnosno())) {
			JSFUtils.addMessage(FORM_DOC_ARCHIVE + ":otnosno", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.otnosno")));
			save = true;
		}
		
		return save;		
	}
	
	public void actionNew() {
		
		this.docProtArchive = new Doc();
		List<DocReferent> referentsAuthor = new ArrayList<>();
		DocReferent author = new DocReferent();

		this.docProtArchive.setRegistraturaId(getUserData(UserData.class).getRegistratura());
		this.docProtArchive.setDocDate(new Date());
		this.docProtArchive.setDocType(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK);
		this.docProtArchive.setFreeAccess(OmbConstants.CODE_ZNACHENIE_NE);
		this.docProtArchive.setDocVid(this.docVid);
		
		author.setRoleRef(OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR);
		author.setCodeRef(getUserData(UserData.class).getUserSave());		
		referentsAuthor.add(author);
		
		this.docProtArchive.setReferentsAuthor(referentsAuthor);
		
		this.avtomNoDisabled = false;
		this.otnosnoOfficProt = "";
		
		unlockAll(true); // да се отключи предишния документ, но да не маха от UserData previousPage	
	}
	
	public void actionSave() {
		
		if(checkData()) {
			return;
		}		
		
		try {			
			
			boolean newDoc = this.docProtArchive.getId() == null;
		
			JPA.getUtil().runInTransaction(() -> this.docProtArchive = this.protocolDAO.save(this.docProtArchive, false, null, null, getSystemData()));
		
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
			
			this.otnosnoOfficProt = this.docProtArchive.getOtnosno();
			
			if(newDoc) {// само за нов документ			
				// заключване на док.
				lockDoc(this.docProtArchive.getId());
			}
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на протокол за предаване в учрежденски и Държавен архив дела! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
		
	}
	
	public void actionDelete() {
		
		try {
			
			JPA.getUtil().runInTransaction(() -> {
				
				this.protocolDAO.delete(this.docProtArchive);
				
				if (this.filesListForWork != null && !this.filesListForWork.isEmpty()) { // трябва да се трият и файловете
					FilesDAO filesDao = new FilesDAO(getUserData());
					for (Files f : this.filesListForWork) {
						filesDao.deleteFileObject(f);
					}
				}
				
				if (this.filesListForOffic != null && !this.filesListForOffic.isEmpty()) { // трябва да се трият и файловете
					FilesDAO filesDao = new FilesDAO(getUserData());
					for (Files f : this.filesListForOffic) {
						filesDao.deleteFileObject(f);
					}
				}
			});
		

			this.protocolDAO.notifDocDelete(docProtArchive, (SystemData) getSystemData());

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			
			actionNew();			
		    
		} catch (ObjectInUseException e) {
			LOGGER.error("Протоколът за предаване в учрежденски и Държавен архив дела се използва и не може да бъде изтрит! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());		
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на протокол за предаване в учрежденски и Държавен архив дела! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
		
	}
	
	/**Изключва автоматичното генериране на номера
	 * 
	 */
	public void actionChangeAvtomNo() {
	   if(this.avtomNo) {
		   this.docProtArchive.setRnDoc(null); 
	   }
	}
	
	   /**При промяна на регистъра 
	    * 
	    */
	   public void actionChangeRegister() {
		    LOGGER.debug(" actionChangeRegister = {}", this.docProtArchive.getRegisterId());
				
			Integer alg = null;
			avtomNoDisabled =  false;
			try {
				 if(this.docProtArchive.getRegisterId() != null) {
					 alg  = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRI, this.docProtArchive.getRegisterId(), getUserData().getCurrentLang(), new Date(), OmbClassifAdapter.REGISTRI_INDEX_ALG);
				 }
				 if(alg != null && alg.equals(OmbConstants.CODE_ZNACHENIE_ALG_FREE)) {
					 this.avtomNo = false; // да се забрани автом. генер. на номера! Да се прави проверка за въведен номер, ако алгоритъмът е "произволен рег.номер"
					 avtomNoDisabled =  true;
				 }else if(SearchUtils.isEmpty(this.docProtArchive.getRnDoc())){
					 this.avtomNo = true; // да се промени според регистъра, само ако вече няма нищо въведено в полето за номер на документ 
				 }
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при промяна на регистъра в протокол за предаване в учрежденски и Държавен архив дела! ", e);				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}						

	   }
	   
	public void actionSearch() {

		SelectMetadata smd = this.protocolDAO.createSelectArchiveProtocolDela(this.docProtArchive, null, this.vidAndStatusArchive);
		String defaultSortColumn = "A0";
		this.delaForArchive = new LazyDataModelSQL2Array(smd, defaultSortColumn);
	}
	
	public void actionDelReg() {
		
		boolean delReg = false;
		
		if(this.dateOfficProt == null) {
			JSFUtils.addMessage(FORM_DOC_ARCHIVE + ":dateDelReg", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docDestruction.dateReg")));
			delReg = true;
		}
		
		if(SearchUtils.isEmpty(this.otnosnoOfficProt)) {
			JSFUtils.addMessage(FORM_DOC_ARCHIVE + ":otnosnoDelReg", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docu.otnosno")));
			delReg = true;
		}
		
		if (delReg) {
			return;
		}
		
		Doc officProt = new Doc();
		List<DocReferent> referentsAuthor = new ArrayList<>();
		DocReferent author = new DocReferent();		
		
		try {
		
			if (this.idOfficReg != null) {
				officProt.setRegistraturaId(this.idOfficReg);
			} else {
				officProt.setRegistraturaId(getUserData(UserData.class).getRegistratura());
			}
			
			officProt.setDocType(OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN);
			officProt.setDocVid(this.docVid);
	
			Integer docTypeReg  = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRI, this.idRegister, getUserData().getCurrentLang(), new Date(), OmbClassifAdapter.REGISTRI_INDEX_DOC_TYPE);
			if(docTypeReg == null || docTypeReg.equals(officProt.getDocType())) { // само, ако регистъра е за всички или за избрания тип документ
				officProt.setRegisterId(this.idRegister);//REGISTER_ID
			}
			
			officProt.setDocDate(this.dateOfficProt);
			officProt.setOtnosno(getOtnosnoOfficProt());
			officProt.setFreeAccess(this.docProtArchive.getFreeAccess());
			officProt.setWorkOffId(this.docProtArchive.getId());
			
			author.setRoleRef(OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AUTHOR);
			author.setCodeRef(getUserData(UserData.class).getUserSave());		
			referentsAuthor.add(author);			
			officProt.setReferentsAuthor(referentsAuthor); 
			
			JPA.getUtil().runInTransaction(() -> { // запис на док и файловете в обща транзакция
				
				this.protocolDAO.save(officProt, false, null, null, getSystemData());

				if (this.filesListForWork != null && !this.filesListForWork.isEmpty()) {
					// при регистриране на офицален от работен!!!
					// да направи връзка към  новия документ
									
					for (Files f : this.filesListForWork) {
						new FilesDAO(getUserData()).saveFileObject(f, officProt.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
					}
				}
				
				this.docProtArchive = this.protocolDAO.findById(this.docProtArchive.getId());
				
				this.filesListForOffic = new FilesDAO(getUserData()).selectByFileObject(this.docProtArchive.getWorkOffId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
				
			}); 
			
			checkNumberDelaForArchive();
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docArchive.succesDelReg", this.vidProt));
		
			this.otnosnoOfficProt = "";
			
			String dialogWidgetVar = "PF('docModalDelReg').hide();";
			PrimeFaces.current().executeScript(dialogWidgetVar);		
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при деловодна регистрация на официален протокол за предаване в учрежденски и Държавен архив дела! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при деловодна регистрация на официален протокол за предаване в учрежденски и Държавен архив дела! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		
		}		
		
	}
	
	public void actionAddDelaInProtocol() {
		
		List<Integer> idsSelDela = new ArrayList<>();
		
		for (Object[] obj : this.selectedDela) {
			
			idsSelDela.add(SearchUtils.asInteger(obj[0]));			
		}
		
		try {
			
			JPA.getUtil().runInTransaction(() -> this.protocolDAO.addArchiveProtocolDela(this.docProtArchive, idsSelDela));
			
			checkNumberDelaForArchive();
			
			actionSearch();
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при добавяне на документи към протокол за предаване в учрежденски и Държавен архив дела! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	
		
	}
	
	public void actionCompleteDelaForArchive() {
		
		try {				
			
			JPA.getUtil().runInTransaction(() -> this.protocolDAO.completeArchiveProtocol(this.docProtArchive, this.vidAndStatusArchive, getSystemData()));
		
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
			
			checkNumberDelaForArchive();
			
			actionSearch();
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при маркиране на дела за предаване в учрежденски и Държавен архив! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	
	}
	
	public void actionDeleteDelo(Integer docId, Integer valid) {
		
		try {

			JPA.getUtil().runInTransaction(() -> this.protocolDAO.removeIncludedDelo(this.docProtArchive, docId, valid, this.vidAndStatusArchive, getSystemData()));
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			
			checkNumberDelaForArchive();
			
			actionSearch();

		} catch (BaseException e) {
			LOGGER.error("Грешка при изтриване на включен дело за за предаване в учрежденски и Държавен архив! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	public Object[] actionSearchOfficProtByRnDoc(String rnDoc) {
		
		Object[] searchedDoc = null;

		if (rnDoc == null) {
			rnDoc = "";
		}
		
		Integer[] docType = new Integer[1];
		List<Integer> docVidList = new ArrayList<>(); 
		
		docType[0] = OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN;
		docVidList.add(this.docVid);

		DocSearch docFromComp = new DocSearch(getUserData(UserData.class).getRegistratura());
		docFromComp.setRnDoc(rnDoc);
		docFromComp.setNotInDocId(this.docProtArchive.getId());
		docFromComp.setDocTypeArr(docType);
		docFromComp.setDocVidList(docVidList);
		docFromComp.setNullWorkOffId(true);
		docFromComp.setRnDocEQ(false);

		docFromComp.buildQueryComp(getUserData());

		LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(docFromComp, "a1 desc");
		
		// Марияна поиска да се избира от компонентата, дори и ако е един документ, за да могат да виждат датата и относното на документа, с който се свързва - 29.05.2020
//		int res = lazy.getRowCount();
//
//		if (res == 0) {
//		
//			LOGGER.info("Не е намерен документ с въведения номер!"); 
//			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(BEAN_MESSAGES, "docDestruction.notFoundDocByregNum"));
//		
//		} else if (res == 1) {
//			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null, null);
//			searchedDoc = new Object[5];
//			searchedDoc = result.get(0);			
//			connectWithOfficProt(SearchUtils.asInteger(searchedDoc[0]), SearchUtils.asString(searchedDoc[1]), SearchUtils.asDate(searchedDoc[4]));	
//			
//			this.selectedDoc = null;
//			this.searchRnDoc = null;
//			
//			String dialogWidgetVar = "PF('dlgLinkWithOfficProt').hide();";
//			PrimeFaces.current().executeScript(dialogWidgetVar);
//		
//		} else {
			lazy.load(0, lazy.getRowCount(), null, null);
			searchedDoc = new Object[5];
			String dialogWidgetVar = "PF('modalDocSearchForOffic').show();";
			PrimeFaces.current().executeScript(dialogWidgetVar);
//		}
		
		return searchedDoc;
	}
	
	public void actionLoadSelectedDoc() {
		
		if (this.selectedDoc != null && this.selectedDoc[0] != null) {
			connectWithOfficProt(SearchUtils.asInteger(selectedDoc[0]), SearchUtils.asString(selectedDoc[1]), SearchUtils.asDate(selectedDoc[4]));
			this.selectedDoc = null;
			this.searchRnDoc = null;
			
			String dialogWidgetVar = "PF('dlgLinkWithOfficProt').hide();";
			PrimeFaces.current().executeScript(dialogWidgetVar);
			
		}
	}

	public void actionSearchDoc() {
		
		if (this.searchRnDoc.isEmpty()) {
			String dialogWidgetVar = "PF('modalDocSearchForOffic').show();";
			PrimeFaces.current().executeScript(dialogWidgetVar);
			actionLoadSelectedDoc();
	
		} else if (!isEmpty(this.searchRnDoc)) {			
			actionSearchOfficProtByRnDoc(this.searchRnDoc);
		}
		
		checkNumberDelaForArchive();
	}	
	
	public void connectWithOfficProt(Integer officialDocId, String rnDoc, Date dateDoc) {
		
		try {

			JPA.getUtil().runInTransaction(() -> {
				
				this.docProtArchive = this.protocolDAO.connectWithOfficial(this.docProtArchive, officialDocId, getSystemData(), "архивиране");
				
				this.filesListForOffic = new FilesDAO(getUserData()).selectByFileObject(this.docProtArchive.getWorkOffId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
			
			});
				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));			

		} catch (BaseException e) {
			LOGGER.error("Грешка при свързване с официален протокол! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void disconnectFromOfficialProt() { 
		
		try {

			JPA.getUtil().runInTransaction(() -> {
				
				this.docProtArchive = this.protocolDAO.disconnectFromOfficial(this.docProtArchive, this.vidAndStatusArchive, getSystemData());
				
				this.filesListForOffic = new FilesDAO(getUserData()).selectByFileObject(this.docProtArchive.getWorkOffId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
				
			});
				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docDestruction.succesDisconnestFromOficProt"));			

		} catch (BaseException e) {
			LOGGER.error("Грешка при свързване с официален протокол! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public String actionGotoDoc(Integer idObj) {
		
		return "docEdit.jsf?faces-redirect=true&idObj=" + idObj; 
	}
	
	public String actionGotoDelo(Integer idObj) {
		
		return "deloView.xhtml?faces-redirect=true&idObj=" + idObj;
		
	}
	
	/******************************************************* LOCK & UNLOCK *******************************************************/	
	
	/**
	 * Проверка за заключен документ 
	 * @param idObj
	 * @return
	 */
	private boolean checkForLock(Integer idObj) {
		boolean res = true;
		LockObjectDAO daoL = new LockObjectDAO();		
		
		try { 
			Object[] obj = daoL.check(getUserData().getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, idObj);
			
			if (obj != null) {
				 res = false;
				 String msg = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, Integer.valueOf(obj[0].toString()), getUserData().getCurrentLang(), new Date())   
						       + " / " + DateUtils.printDate((Date)obj[1]);
				 
				 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,getMessageResourceString(LABELS, "docu.docLocked"), msg);
			}
		
		} catch (DbErrorException e) {
			
			LOGGER.error("Грешка при проверка за заключен документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		return res;
	}
	
	/**
	 * Заключване на документ, като преди това отключва всички обекти, заключени от потребителя
	 * @param idObj
	 */
	public void lockDoc(Integer idObj) {	
		
		LOGGER.info("lockDoc = {}", ((UserData) getUserData()).getPreviousPage());		
		 
		LockObjectDAO daoL = new LockObjectDAO();		
		
		try { 
			
			JPA.getUtil().runInTransaction(() -> daoL.lock(getUserData().getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, idObj, null));
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при заключване на документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при заключване на документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}			
	}

	
	/**
	 * при излизане от страницата - отключва обекта и да го освобождава за актуализация от друг потребител
	 */
	@PreDestroy
	public void unlockDoc(){
        if (!((UserData) getUserData()).isReloadPage()) {
        	LOGGER.info("unlockData 0 = {}", ((UserData) getUserData()).getPreviousPage());
        	unlockAll(true);
        	((UserData) getUserData()).setPreviousPage(null);
        }          
        ((UserData) getUserData()).setReloadPage(false); 
	}
	
	
	/**
	 * отключва всички обекти на потребителя - при излизане от страницата или при натискане на бутон "Нов"
	 */
	private void unlockAll(boolean all) {
		LOGGER.info("unlockDoc 1 = {}", ((UserData) getUserData()).getPreviousPage());
		LockObjectDAO daoL = new LockObjectDAO();		
		
		try { 
			if (all) {
				JPA.getUtil().runInTransaction(() -> {
					daoL.unlock(getUserData().getUserId());
				});
			} 
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при отключване на документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при отключване на документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
	}
	
	/**************************************************** END LOCK & UNLOCK ****************************************************/	
	
	/**
	 * за експорт в pdf 
	 * @return
	 */
	public PDFOptions pdfOptions() {
		PDFOptions pdfOpt = new CustomExpPreProcess().pdfOptions(null, null, null);
        return pdfOpt;
	}
	
	
	/******************************************************* GET & SET *******************************************************/	
	
	public Date getDecodeDate() {
		return new Date(decodeDate.getTime()) ;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
	}	

	public Doc getDocProtArchive() {
		return docProtArchive;
	}

	public void setDocProtArchive(Doc docProtArchive) {
		this.docProtArchive = docProtArchive;
	}

	public Integer getDocVid() {
		return docVid;
	}

	public void setDocVid(Integer docVid) {
		this.docVid = docVid;
	}

	public String getVidProt() {
		return vidProt;
	}

	public void setVidProt(String vidProt) {
		this.vidProt = vidProt;
	}

	public boolean isAvtomNo() {
		return avtomNo;
	}

	public void setAvtomNo(boolean avtomNo) {
		this.avtomNo = avtomNo;
	}

	public boolean isAvtomNoDisabled() {
		return avtomNoDisabled;
	}

	public void setAvtomNoDisabled(boolean avtomNoDisabled) {
		this.avtomNoDisabled = avtomNoDisabled;
	}

	public Map<Integer, Object> getSpecificsRegister() {
		return specificsRegister;
	}

	public void setSpecificsRegister(Map<Integer, Object> specificsRegister) {
		this.specificsRegister = specificsRegister;
	}

	public List<Files> getFilesListForWork() {
		return filesListForWork;
	}

	public void setFilesListForWork(List<Files> filesListForWork) {
		this.filesListForWork = filesListForWork;
	}

	public List<Files> getFilesListForOffic() {
		return filesListForOffic;
	}

	public void setFilesListForOffic(List<Files> filesListForOffic) {
		this.filesListForOffic = filesListForOffic;
	}
	
	public List<Object[]> getSelectedDela() {
		return selectedDela;
	}

	public void setSelectedDela(List<Object[]> selectedDela) {
		this.selectedDela = selectedDela;
	}

	public Map<Integer, Integer> getVidAndStatusArchive() {
		return vidAndStatusArchive;
	}

	public void setVidAndStatusArchive(Map<Integer, Integer> vidAndStatusArchive) {
		this.vidAndStatusArchive = vidAndStatusArchive;
	}

	public List<SystemClassif> getClassifRegsList() {
		return classifRegsList;
	}

	public void setClassifRegsList(List<SystemClassif> classifRegsList) {
		this.classifRegsList = classifRegsList;
	}	

	public LazyDataModelSQL2Array getDelaForArchive() {
		return delaForArchive;
	}

	public void setDelaForArchive(LazyDataModelSQL2Array delaForArchive) {
		this.delaForArchive = delaForArchive;
	}

	public boolean isExistNotArchiveDela() {
		return existNotArchiveDela;
	}

	public void setExistNotArchiveDela(boolean existNotArchiveDela) {
		this.existNotArchiveDela = existNotArchiveDela;
	}

	public int getCountDela() {
		return countDela;
	}

	public void setCountDela(int countDela) {
		this.countDela = countDela;
	}

	public Object[] getSelectedDoc() {
		return selectedDoc;
	}

	public void setSelectedDoc(Object[] selectedDoc) {
		this.selectedDoc = selectedDoc;
	}

	public String getSearchRnDoc() {
		return searchRnDoc;
	}

	public void setSearchRnDoc(String searchRnDoc) {
		this.searchRnDoc = searchRnDoc;
	}

	public Integer getIdOfficReg() {
		return idOfficReg;
	}

	public void setIdOfficReg(Integer idOfficReg) {
		this.idOfficReg = idOfficReg;
	}

	public Integer getIdRegister() {
		return idRegister;
	}

	public void setIdRegister(Integer idRegister) {
		this.idRegister = idRegister;
	}

	public boolean isExistRegister() {
		return existRegister;
	}

	public void setExistRegister(boolean existRegister) {
		this.existRegister = existRegister;
	}

	public Date getDateOfficProt() {
		return new Date(dateOfficProt.getTime());
	}

	public void setDateOfficProt(Date dateOfficProt) {
		this.dateOfficProt = dateOfficProt != null ? new Date(dateOfficProt.getTime()) : null;
	}
	
	public String getOtnosnoOfficProt() {
		return otnosnoOfficProt;
	}

	public void setOtnosnoOfficProt(String otnosnoOfficProt) {
		this.otnosnoOfficProt = otnosnoOfficProt;
	}

	public Integer[] getDocTypeArr() {
		return docTypeArr;
	}

	public void setDocTypeArr(Integer[] docTypeArr) {
		this.docTypeArr = docTypeArr;
	}
	
	/**************************************************** END GET & SET ****************************************************/	
	
}
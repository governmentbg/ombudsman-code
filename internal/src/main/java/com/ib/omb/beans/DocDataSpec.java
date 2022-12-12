package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.DocJalbaDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocSpec;
import com.ib.omb.db.dto.DocSpecOrgan;
import com.ib.omb.search.DocJalbaSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * Актуализация на документ заповед за НПМ или решение за самосезиране 
 *
 * @author rosi
 */
@Named
@ViewScoped
public class DocDataSpec   extends IndexUIbean   implements Serializable {
	/**  */
	private static final long serialVersionUID = 8191901936895268740L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocDataSpec.class);
	
	public  static final String DOCFORMTABS      	 = "docForm:tabsDoc";
	public  static final String	MSGVALIDDATES		 = "docu.validDates";
	public  static final String	MSGVALIDDATESSTART	 = "specDoc.validDates";
	
	private Doc document;
	/**
	 * списък файлове към документа
	 */
	private List<Files> filesList;
	
	private String rnFullDocEdit; 
	/**
	 * рег.ном./дата на протокол за унищожаване на документ
	 */
	private String rnFullProtocol;
	private Date decodeDate = new Date();
	
	private transient DocDAO	dao;
	private SystemData sd;	
	private UserData ud;
	private Integer docVidInclude; // коя страница да вмъкне
	private DocData docDataBean;
	private boolean scanModuleExist;
	private transient Map<Integer, Object> specificsAdm;	
	/**
	 * за какво действие е извикан екрана
	 */
	private int flagFW;
	private int isView;

	/**
	 * Допълнителни експерти
	 */
	private List<SystemClassif>  scDopExpertsList; // заради autocomplete
	/**
	 * код на орган в списъка с проверявани органи - изп. се за корекция на данни за избран орган
	 */
	private Integer rowOrgan;
	
	
	/**
	 * Справка за документи в преписката по НПМ/самосезиране, какви задачи са изпълнявани по тези документи, кои документи са изпратени и до кого.
	 */
	private List<Object[]> spravkaList;	
	
	/**
	 * Справка - история на състоянията
	 */
	private List<Object[]> sastHistoryList;
	
	private String fSize;
	
	private Integer docSettingId;
	
	/** */
	@PostConstruct
	void initData() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("PostConstruct! Complaint" );
		}
		
		ud = getUserData(UserData.class);					
		sd = (SystemData) getSystemData();
		dao = new DocDAO(getUserData());
		
		DocData bean = (DocData) JSFUtils.getManagedBean("docData"); 
		
		if(bean != null){
			setDocVidInclude(bean.getDocVidInclude()); 
			setDocument(bean.getDocument());
			setDocDataBean(bean);
			setScanModuleExist(bean.isScanModuleExist());
			setFilesList(bean.getFilesList());
			setRnFullProtocol(bean.getRnFullProtocol());
			setDecodeDate(document.getDocDate());
			setFlagFW(docDataBean.getFlagFW());
			fSize="350";
			if(document.getId() == null) {
				actionNewDocS(false);
			 // да зареди специфичните неща за новия док.
			}else {
				if(document.getDocSpec() == null) {
					document.setDocSpec(new DocSpec()); // не би трябвало да се случвa
				}else {
					loadSpecDoc();
				}
			}
			docSettingId = bean.getDocSettingId();
		}else {
			document = null;
			LOGGER.error("Грешка при зареждане на данните за документ от docData bean!");
		}
	}
	
	/**
	 * зарежда специфични за нпм и самосезиране атрибути
	 */
	private void loadSpecDoc() {
		
     	 rnFullDocEdit = DocDAO.formRnDocDate(this.document.getRnDoc(), this.document.getDateReg(), this.document.getPoredDelo());
     	 loadScDopExpertsList(); // допълнителни експерти

	}
	
	
	/**
	 * зарежда екип експерти
	 */
	private void loadScDopExpertsList(){
		List<SystemClassif> tmpLst = new ArrayList<>();		
		if(document.getDocSpec().getDopExpertCodes() != null) {
			for( Integer item : document.getDocSpec().getDopExpertCodes()) {
				String tekst = "";
				SystemClassif scItem = new SystemClassif();
				try {
					scItem.setCodeClassif(OmbConstants.CODE_CLASSIF_ADMIN_STR);
					scItem.setCode(item);
					tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, item, getUserData().getCurrentLang(), new Date());		
					scItem.setTekst(tekst);
					tmpLst.add(scItem);
				} catch (DbErrorException e) {
					LOGGER.error("Грешка при зареждане на допълнителни експерти - НПМ/самосезиране! ", e);
				}		
			}				
		}
		setScDopExpertsList(tmpLst); 
	}
	
	/** Нов документ  НПМ или самосезиране
	 * @return
	 */
	public void actionNewDocument() {		
		docDataBean.actionNewDocument(false, true);	 // само, за да е еднакво с другите докуметни
		setDocument(docDataBean.getDocument());
		setFilesList(docDataBean.getFilesList());
		rnFullDocEdit = null;
		docDataBean.setRnFullDocEdit(null);
		actionNewDocS(true);
	}
	
	/**
	 * Нов док. НПМ или самосезиране
	 * стойности по подразбиране
	 * flag2 = true - натиснат е бутон Нов вътре в екрана
	 */
	public void actionNewDocS(boolean flag2) {
		document.setDocSpec(new DocSpec());
		docDataBean.setCreatePrep(true); 
		document.getDocSpec().setPublicVisible(OmbConstants.CODE_ZNACHENIE_NE);
		document.getDocSpec().setStartDate(new Date());
	//	document.getDocSpec().setSrok(DateUtils.addDays(new Date(), 31, true)); //  по подразбиране 31 дни ???? TODO
	//	document.getDocSpec().setSastDate(new Date());
		document.getDocSpec().setSast(OmbConstants.CODE_ZNACHENIE_JALBA_SAST_FILED);
		if(flag2) {
			setScDopExpertsList(null);
			fSize="350";
		}
	}
	
	/**
	 * Шаблони за вида документ 
	 */
	public void actionDocTemplateS() {
		docDataBean.actionDocTemplate();
	}
	
	
	/** 
	 * Нов работен към преписката на текущия док.
	 * Препраща към екрана на раб. док. където видовете док. са само  от класификация "Документи при обработка на НПМ" или "Документи при обработка на самосезиране" 
	 * Раб. документ влиза в раздел "вътрешен" на преписката на жалбата 
	 * @returnna 
	 */
	public String actionGotoWrkDoc() {
		return "docNew.jsf?faces-redirect=true&idJ=" + document.getId()+"&jStr="+rnFullDocEdit+"&v="+docVidInclude;
	}

	/**
	 * Запис на документ
	 */
	public void actionSave() {
			
		if(checkDataDoc()) { 			
			try {
			//	specBParam();
				 
				// файловете за нов документ - да се запишат с него
				// ако е редакция на документ - се записват при upload
				boolean newDoc = getDocument().getId() == null;
			
				saveDoc(newDoc);
				  			 				
				docDataBean.clearDeloDocLink(); //  махам връзката, ако се натисне втори път запис;  само за нов документ deloDocPrep.getId() би могло да е != null
					
				if(newDoc) {// само за нов документ	
					
					rnFullDocEdit = DocDAO.formRnDocDate(this.document.getRnDoc(), this.document.getDateReg(), this.document.getPoredDelo());
					docDataBean.setRnFullDocEdit(rnFullDocEdit);
			   
					// заключване на док.
					docDataBean.lockDoc(getDocument().getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
					docDataBean.setCreatePrep(false);
					
					docDataBean.newDocFromMail();
					
					docDataBean.setFlagFW(DocData.UPDATE_DOC); 
					setFlagFW(DocData.UPDATE_DOC);
				}
			
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );		
			} catch (ObjectInUseException e) {
				LOGGER.error("Грешка при запис на НПМ/самосезиране! ObjectInUseException "); 
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} catch (BaseException e) {			
				LOGGER.error("Грешка при запис на  НПМ/самосезиране! BaseException", e);				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  e.getMessage());
			} catch (Exception e) {
				LOGGER.error("Грешка при запис на  НПМ/самосезиране! ", e);					
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  e.getMessage());
			}
		}
	}

	
	/**
	 * извиква метода за запис на документ и файловете в обща транзакция
	 * @param newDoc
	 * @throws BaseException
	 */
	private void saveDoc(boolean newDoc) throws BaseException {
		JPA.getUtil().runInTransaction(() -> { 
			getDocument().setCountFiles(getFilesList() == null ? 0 :getFilesList().size());
			
			setDocument(getDao().save(getDocument(), docDataBean.isCreatePrep(), null, null, getSystemData()));
			if (newDoc && filesList != null && !filesList.isEmpty()) {
				// при регистриране на офицален от работен!!!
				// да направи връзка към  новия документ
				FilesDAO filesDao = new FilesDAO(getUserData());
				for (Files f : filesList) {
					filesDao.saveFileObject(f, this.document.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
				}
			}	
		});
	}

	
	/**
	 * проверка за задължителни полета при запис на документ
	 */
	public boolean checkDataDoc() {
		boolean flagSave = true;
		if(document.getRegisterId() == null) {
			String  msg="specDoc.regNpm";
			if(docVidInclude == getInclSamosez()) {
				 msg="specDoc.regSamosez";
			}
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(beanMessages, "specDoc.regSpecErr", getMessageResourceString(LABELS, msg)));// "Липсва регистър! За потребителя не е указан регистър ");
			flagSave = false;
		}
		
		if(SearchUtils.isEmpty(document.getOtnosno())) {
			JSFUtils.addMessage(DOCFORMTABS+":otnosno",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docspec.predmet")));
			flagSave = false;
		}
			
		boolean flagSave1 =  checkDates();
		return flagSave && flagSave1;
	}
	
	
	
	/**
	 * Проверка за валидни дати в нпм, самосезиране
	 * @return
	 */
	private boolean checkDates() {
		int flagDatesOk = 0;// всичко е ок
		
		Date docDate = DateUtils.startDate(document.getDocDate());
	
		// дата на започване  - след дата на документа
		Date startDate = DateUtils.startDate(document.getDocSpec().getStartDate());
		if(startDate == null ) {
			document.getDocSpec().setStartDate(docDate);
		} else {
			flagDatesOk += docDataBean.checkDatesB(startDate, docDate, ":startDate", "docspec.startDate", MSGVALIDDATES );
		}
		
		// дата на приключване - след датата на започване
		Date tmpDate = DateUtils.startDate(document.getDocSpec().getSrok());
		if(tmpDate == null ) {
			JSFUtils.addMessage(DOCFORMTABS+":endDate",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "docspec.endDate")));
		} else {
			flagDatesOk += docDataBean.checkDatesB(tmpDate, startDate, ":endDate", "docspec.endDate", MSGVALIDDATESSTART);
		}		
	
		// дата на валидност - след дата на документа
		tmpDate = DateUtils.startDate(document.getValidDate());
		if(tmpDate == null ) {
			document.setValidDate(docDate);
		} else {
			flagDatesOk += docDataBean.checkDatesB(tmpDate, docDate, ":validDat", "docu.validDate", MSGVALIDDATES);
		}

	
		//Проверяван орган - полето за орган е задължително! Датата на резултата не може да е преди началото на проверката
		List<DocSpecOrgan> tmpL = document.getDocSpec().getSpecOrganList();
		if(tmpL != null && !tmpL.isEmpty()) {
			for (DocSpecOrgan row: tmpL) {
				if(row.getCodeOrgan() == null) {
					JSFUtils.addMessage(DOCFORMTABS+":organiList",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages,MSGPLSINS,getMessageResourceString(LABELS, "specDoc.orgName")));
					flagDatesOk++;
					break;
				}
				if(row.getDateResult() != null){
					tmpDate = DateUtils.startDate(row.getDateResult());	
					if(docDataBean.checkDatesB(tmpDate, startDate, ":organiList", "jp.resultDate", MSGVALIDDATESSTART) > 0){
						flagDatesOk++;
						break;
					}
				}
			}
		}
		
		return flagDatesOk == 0;
	}

	
	/**
	 * Изтриване на деловоден документ
	 * права на потребителя за изтриване
	 * 
	 */
	public void actionDelete() {
		try {
	
			getDocDataBean().deleteDoc(document.getId());
		
			getDao().notifDocDelete(getDocument(), getSd());

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,  IndexUIbean.getMessageResourceString(UI_beanMessages, "general.successDeleteMsg") );
			
			actionNewDocument();			
			docDataBean.setCreatePrep(true); 
		} catch (ObjectInUseException e) {
			// ако инициира преписка и има други връзки 
			LOGGER.error("Грешка при изтриване на жалба!", e); 
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, DocData.OBJECTINUSE), e.getMessage());
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на жалба!", e);			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} 
	}



	 
	/**
	 * Нов проверяван орган
	 */
	 public void  actionNewOrgan() {
		 DocSpecOrgan sOrg = new DocSpecOrgan();
		 if (document.getDocSpec().getSpecOrganList() == null ) {
			 document.getDocSpec().setSpecOrganList(new ArrayList<DocSpecOrgan>());
		 }
		 document.getDocSpec().getSpecOrganList().add(sOrg);
	 }
	 
	 /**
	  * Премахване на ред от таблицата с проверявани органи
	  */
	public String actionRemoveOrgan(int key){
		if(document.getDocSpec().getSpecOrganList() != null){
			document.getDocSpec().getSpecOrganList().remove(key);
		}		
		return null;		
	}

	
	
	/**
	 * Справка - история на състоянията на жалбата
	 */
	public void actionSprSast() {
		DocJalbaDAO daoJ = new DocJalbaDAO(getUserData());   // изп. се същото както при жалбата
		try {
			setSastHistoryList(daoJ.selectSastHistory(document.getId()));
		} catch (DbErrorException e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,  e.getMessage());
			LOGGER.info(e.getMessage(), e);
		}	
	}
	
	/**
	 * Справка за документи в преписката по жалбата, какви задачи са изпълнявани по тези документи, кои документи са изпратени и до кого.
	 */
	public void actionSpravka() {
		DocJalbaSearch tmp = new DocJalbaSearch(); // изп. се същото както при жалбата
		try {
			setSpravkaList(tmp.selectProcessObjects(document.getId(), getSd(), getUd())); 
			String  cmdStr = "PF('modalSpravka').show();";
			PrimeFaces.current().executeScript(cmdStr);
		} catch (Exception e) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,  e.getMessage());
			LOGGER.info(e.getMessage(), e);
		} finally {
			JPA.getUtil().closeConnection();
		}
	}
			    
   
   /**
	 * разглеждане на нпм / самосезиране, преписка, задачи
	 * @param i
	 * @return
	 */
	public String actionGotoViewJ(Integer idObj, Integer typeObj, Integer idDoc) {
		String result = null; 
		if(Objects.equals(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, typeObj)){
			result = "docView.xhtml?faces-redirect=true&idObj=" + idObj;
		} else if(Objects.equals(OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO, typeObj)){
			result = "deloView.xhtml?faces-redirect=true&idObj=" + idObj;
		} else if(Objects.equals(OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK, typeObj)){
			result = "taskView.xhtml?faces-redirect=true&idObj=" + idObj+"&idDoc=" + idDoc; // задачи
		}else if(Objects.equals(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_DVIJ, typeObj)){
			result = "docView.xhtml?faces-redirect=true&idObj=" + idObj; // разглеждане на док., към който е движението....
		}
		return result;
	}

	/**
	 * Да обнови файловете
	 */
	 public void actionChangeFilesJ() {
		 docDataBean.actionChangeFiles();
	 }
	 
	 
	 /**
	  * download на файлове от Справка -  нпм, самосезиране
	  * @param files
	  */
	public void download(Integer idFile) { 
		docDataBean.download(idFile);
	}
	 
	 
 
	

	public int getFlagFW() {
		return flagFW;
	}



	public void setFlagFW(int flagFW) {
		this.flagFW = flagFW;
	}



	public int getIsView() {
		return isView;
	}



	public void setIsView(int isView) {
		this.isView = isView;
	}


	public int getUpdateDoc() {
		return DocData.UPDATE_DOC; 
	}



	public String getRnFullDocEdit() {
		return rnFullDocEdit;
	}


	public void setRnFullDocEdit(String rnFullDocEdit) {
		this.rnFullDocEdit = rnFullDocEdit;
	}



	public boolean isScanModuleExist() {
		return scanModuleExist;
	}



	public void setScanModuleExist(boolean scanModuleExist) {
		this.scanModuleExist = scanModuleExist;
	}



	public List<Files> getFilesList() {
		return filesList;
	}



	public void setFilesList(List<Files> filesList) {
		this.filesList = filesList;
	}



	public String getRnFullProtocol() {
		return rnFullProtocol;
	}



	public void setRnFullProtocol(String rnFullProtocol) {
		this.rnFullProtocol = rnFullProtocol;
	}



	public Date getDecodeDate() {
		return decodeDate;
	}



	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}
	
	
	public DocData getDocDataBean() {
		return docDataBean;
	}



	public void setDocDataBean(DocData docDataBean) {
		this.docDataBean = docDataBean;
	}



	public Doc getDocument() {
		return document;
	}



	public void setDocument(Doc document) {
		this.document = document;
	}



	public DocDAO getDao() {
		return dao;
	}



	public void setDao(DocDAO dao) {
		this.dao = dao;
	}



	public SystemData getSd() {
		return sd;
	}



	public void setSd(SystemData sd) {
		this.sd = sd;
	}



	public UserData getUd() {
		return ud;
	}



	public void setUd(UserData ud) {
		this.ud = ud;
	}


	public Integer getDocVidInclude() {
		return docVidInclude;
	}



	public void setDocVidInclude(Integer docVidInclude) {
		this.docVidInclude = docVidInclude;
	}


	public List<SystemClassif> getScDopExpertsList() {
		return scDopExpertsList;
	}


	public void setScDopExpertsList(List<SystemClassif> scDopExpertsList) {
		this.scDopExpertsList = scDopExpertsList;
	}



	public List<Object[]> getSpravkaList() {
		return spravkaList;
	}

	public void setSpravkaList(List<Object[]> spravkaList) {
		this.spravkaList = spravkaList;
	}

	public Integer getInclNpm() {
		return DocData.INCL_NPM;
	}
	
	public Integer getInclSamosez() {
		return DocData.INCL_SAMOSEZ;
	}

	public Integer getRowOrgan() {
		return rowOrgan;
	}

	public void setRowOrgan(Integer rowOrgan) {
		this.rowOrgan = rowOrgan;
	}


	public Map<Integer, Object> getSpecificsAdm() {
		if(specificsAdm == null && docDataBean != null) {
			specificsAdm =  docDataBean.getSpecificsAdm();
		}
		return specificsAdm;
	}

	public void setSpecificsAdm(Map<Integer, Object> specificsAdm) {
		this.specificsAdm = specificsAdm;
	}

	public List<Object[]> getSastHistoryList() {
		return sastHistoryList;
	}

	public void setSastHistoryList(List<Object[]> sastHistoryList) {
		this.sastHistoryList = sastHistoryList;
	}

	public String getfSize() {
		return fSize;
	}

	public void setfSize(String fSize) {
		this.fSize = fSize;
	}

	public Integer getDocSettingId() {
		return docSettingId;
	}

	public void setDocSettingId(Integer docSettingId) {
		this.docSettingId = docSettingId;
	}
	
	private Integer reloadFile;
   
    public Integer getReloadFile() {
		return reloadFile;
	}
   
   
  //метода се извиква от компонентата за сканиране след успешно извършен запис на файл
    public void setReloadFile(Integer reloadFile) {
 		
 	//	System.out.println("setReloadFile ->"+reloadFile);
 		
 		if(reloadFile!=null) {
 			DocData bean = getDocDataBean();
 			bean.reloadDocDataFile(); 
 			bean.actionChangeFiles();
 			setFilesList(bean.getFilesList());
 		}
 		
 		this.reloadFile = reloadFile;
 	}
	
//	NPM
//	Проверявани органи (множествена структура):
//	•	Проверен орган – избор от подмножество на Орган/лице, включващо тези органи, за които е определена стойност в „Тип на орган по НПМ“;
//	•	Нарушени права – класификация „Права по НПМ“;
//	•	Капацитет на проверения орган – свободно поле за отбелязване на числова стойност;
//	•	Реално настанени лица – свободно поле за отбелязване на числова стойност;
//	•	Констатации - поле в свободен текст;
//	•	Дадена препоръка (да/не);
//	•	Резултат от проверка – класификация (Препоръка - изпълнена / Препоръка - частично изпълнена / Препоръка – неизпълнена)
//	•	Дата на резултата.

	
	
//	samosezirane
//		Проверявани органи (множествена структура): 
//		•	Проверен орган – избор от Орган/лице;
//		•	Нарушени права – класификация „Засегнати права“;
//		•	Констатации - поле в свободен текст;
//		•	Дадена препоръка (да/не);
//		•	Резултат от проверка – класификация (Препоръка - изпълнена / Препоръка - частично изпълнена / Препоръка – неизпълнена);
//		•	Дата на резултата.

}
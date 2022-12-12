package com.ib.omb.beans;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.h2.store.FileLister;
import org.primefaces.PrimeFaces;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.EgovMessagesDAO;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SignatureUtils;
import com.ib.system.utils.VerifySignature;
import com.ib.system.utils.X;



@Named
@ViewScoped
public class TestRosi extends IndexUIbean implements Serializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestRosi.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = 4985013225316883670L;
//	
//	private Date decodeDate;	
//	private List<SystemClassif> scList = new ArrayList<SystemClassif>();
//	private List<Integer> codeRefCorresp;
	//private Integer codeRefCorrSrch;
	private Integer codeRefCorr;
	private String corespName;
//	
//	
//	private String textArea;
//	
//	private Date dateClassif;
//	
//	
	private List<SelectItem> msgVidList = new ArrayList<SelectItem>(); // vid на съобщението
	private String msgVid;

	private List<SelectItem> msgStatusList = new ArrayList<SelectItem>(); // status на съобщението
	private String msgStatus;
	
	private List<SelectItem> msgCommStatusList = new ArrayList<SelectItem>(); // статус на изпращане на съобщението
	private Long commStatus;
	
	
	private SelectMetadata sm; 
	private LazyDataModelSQL2Array rezultLst;
	
	private Date date1;
	private List<Date> multi;
	private String seldate;
	  
	
	private List<Long> docList;
	
	@PostConstruct
	void initData() {
		docList= new ArrayList<>();
		docList.add(2787L); //Т123-74
		docList.add(2786L); //Т123-73
		docList.add(2785L);//Т123-72
		
		EgovMessagesDAO daoEgov = new EgovMessagesDAO(getUserData());
		
		try {
			
			ArrayList<Object[]> 	tmpList = daoEgov.createMsgTypesList();
							
			if(tmpList !=null && !tmpList.isEmpty()){
				setMsgVidList(ConvertInSelectItemStr(tmpList));
				setMsgVid("MSG_DocumentRegistrationRequest");
			}
			
			tmpList.clear();
			tmpList = daoEgov.createMsgStatusList();
			if(tmpList !=null && !tmpList.isEmpty()){
				msgStatusList = ConvertInSelectItemStr(tmpList);
			}
			
			tmpList.clear();
			tmpList = daoEgov.createCommStatusList();
			if(tmpList !=null && !tmpList.isEmpty()){
				msgCommStatusList = ConvertInSelectItemStr(tmpList);
			}
			
			
			String guid = daoEgov.findEgovOrgGuidById(809);
		//sm = daoEgov.createFilterMsgSQL(senderGuid,recGuid,msgVid,msgStatus,commStatus,typeMsg,date_from, date_to, elDeliveryParam); 
			sm = daoEgov.createFilterMsgSQL(null,null,null,null,null,null,
					DateUtils.addDays(new Date(), -365, true), new Date(), "S_SEOS",null,null); 
			rezultLst = new LazyDataModelSQL2Array(sm, "A0");	
			LOGGER.error("ssssssssssssssss");
			
			
			
		} catch (BaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		
	}
	
	

	public static List<SelectItem> ConvertInSelectItemStr(
		ArrayList<Object[]> lst) {	
		List<SelectItem> items = new ArrayList<SelectItem>();
		Iterator<Object[]> it = lst.iterator();
		while (it.hasNext()) {
			Object[] item = (Object[]) it.next();
			if(item != null && item[0]!=null && item[1]!=null){
				items.add(new SelectItem( item[0].toString(),item[1].toString()));
			}
		}
		return items;
	}
	
	
		
//	 public List<String> completeArea(String query) {
//	        List<String> results = new ArrayList<String>();
//	       
//	        
//	        if (query != null && !query.trim().isEmpty()) {
//				try {
//					Integer codeClassif = OmbConstants.CODE_CLASSIF_ADMIN_STR;
//					if (codeClassif != null) {
//						
//						List<SystemClassif> lc =loadClassifList(codeClassif, false, query);
//						for(SystemClassif item: lc) {
//							results.add(item.getTekst());	
//						}
//					}
//
//				} catch (Exception e) {
//					LOGGER.error(e.getMessage(), e);
//				}
//				
////				ValueExpression expr2 = getValueExpression("selectedText");
////				ELContext ctx2 = getFacesContext().getELContext();
////				if (expr2 != null) {
////					expr2.setValue(ctx2, query);
////				}	
//
//			
//			}
//			return results != null ? results : new ArrayList<>();	       
//	    }
//	 
//	    public void onSelect(SelectEvent<String> event) {
//	    	textArea  = event.getObject();
//	        LOGGER.info(event.getObject());
//	    }
//	
//
//	    
//	    
//		List<SystemClassif> loadClassifList(Integer codeClassif, boolean fixPrev, String query) throws DbErrorException {
////			@SuppressWarnings("unchecked")
//			Map<Integer, Object> specifics = null;//(Map<Integer, Object>) getAttributes().get("specifics");
////
//		
//			BaseUserData ud = getUserData();
//			int lang = ud.getCurrentLang();
//
//			List<SystemClassif> classifList = new ArrayList<>();
//			if (specifics != null && !specifics.isEmpty()) { // ще се пуска търсене на специфики, bez filtrirane po prawa
//				classifList = getSystemData().queryClassification(codeClassif, query, getDateClassif(), lang, specifics);
//			}  else {
//				classifList = getSystemData().queryClassification(codeClassif, query, getDateClassif(), lang );
//			}
//
//			return classifList;
//		}
//	    
//		/** @return the dateClassif */
//		private Date getDateClassif() {
//			if (this.dateClassif == null) {
//			//	this.dateClassif = (Date) getAttributes().get("dateClassif");
//				if (this.dateClassif == null) {
//					this.dateClassif = new Date();
//				}
//			}
//			return this.dateClassif;
//		}
//		
//		
//		
//		
		
//	private Map<Integer, Object> specificsAdm = Collections.singletonMap(OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE
//			, X.of(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL)); //  така ще дава само служители през аутокомплете, а в дървото ще е цялата
//
//	
//	public String actionGotoDoc() {
//	
//		return "docNew.xhtml@clear@?faces-redirect=true";
//	}
//
//	public String actionGotoDocList() {
//	
//		return "docEditList.xhtml@clear@?faces-redirect=true";
//	}
//	
//	
//	public  void actionAddSelectRef(){
//		LOGGER.info("TODO - да се прехвърли избрания код в selectManyModalA");
//		if(codeRefCorrSrch != null) {
//		
//			String tekst;
//			try {
//				SystemClassif tmpSc = new SystemClassif();
//				tmpSc.setCodeClassif(OmbConstants.CODE_CLASSIF_REFERENTS);
//				tmpSc.setCode(codeRefCorrSrch);
//				tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS, codeRefCorrSrch, getUserData().getCurrentLang(), new Date());
//				tmpSc.setTekst(tekst);
//				scList.add(tmpSc);
//			} catch (DbErrorException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//			codeRefCorrSrch = null;
//		}
//		
//		String  cmdStr = "PF('mCorrS').hide();";
//		PrimeFaces.current().executeScript(cmdStr);	
//	}
//	
//
//	/**
//	 *прехвърля избраното значение в друго поле и зачиства полето за търсене
//	 */
//	public void actionUpdateCorespData() {
//		codeRefCorr = codeRefCorrSrch; 
//	//	codeRefCorrSrch = null;
//	
//		PrimeFaces.current().focus("testForm:refCorrInp1:аutoCompl");
//	}
////	
//	
//	
//	public Date getDecodeDate() {
//		return decodeDate;
//	}
//
//	public void setDecodeDate(Date decodeDate) {
//		this.decodeDate = decodeDate;
//	}
////	
//
//	public List<SystemClassif> getScList() {
//		return scList;
//	}
//
//	public void setScList(List<SystemClassif> scList) {
//		this.scList = scList;
//	}
//
//	public List<Integer> getCodeRefCorresp() {
//		return codeRefCorresp;
//	}
//
//	public void setCodeRefCorresp(List<Integer> codeRefCorresp) {
//		this.codeRefCorresp = codeRefCorresp;
//	}
//
//	public Integer getCodeRefCorrSrch() {
//		return codeRefCorrSrch;
//	}
//
//	public void setCodeRefCorrSrch(Integer codeRefCorrSrch) {
//		this.codeRefCorrSrch = codeRefCorrSrch;
//	}
//
	public String getCorespName() {
		return corespName;
	}

	public void setCorespName(String corespName) {
		this.corespName = corespName;
	}

	public Integer getCodeRefCorr() {
		return codeRefCorr;
	}

	public void setCodeRefCorr(Integer codeRefCorr) {
		this.codeRefCorr = codeRefCorr;
	}
//
//	public String getTextArea() {
//		return textArea;
//	}
//
//	public void setTextArea(String textArea) {
//		this.textArea = textArea;
//	}
//
	public List<SelectItem> getMsgVidList() {
		return msgVidList;
	}

	public void setMsgVidList(List<SelectItem> msgVidList) {
		this.msgVidList = msgVidList;
	}

	public String getMsgVid() {
		return msgVid;
	}

	public void setMsgVid(String msgVid) {
		this.msgVid = msgVid;
	}



	public List<SelectItem> getMsgStatusList() {
		return msgStatusList;
	}



	public void setMsgStatusList(List<SelectItem> msgStatusList) {
		this.msgStatusList = msgStatusList;
	}



	public String getMsgStatus() {
		return msgStatus;
	}



	public void setMsgStatus(String msgStatus) {
		this.msgStatus = msgStatus;
	}



	public List<SelectItem> getMsgCommStatusList() {
		return msgCommStatusList;
	}



	public void setMsgCommStatusList(List<SelectItem> msgCommStatusList) {
		this.msgCommStatusList = msgCommStatusList;
	}



	public Long getCommStatus() {
		return commStatus;
	}



	public void setCommStatus(Long commStatus) {
		this.commStatus = commStatus;
	}

	public LazyDataModelSQL2Array getRezultLst() {
		return rezultLst;
	}



	public void setRezultLst(LazyDataModelSQL2Array rezultLst) {
		this.rezultLst = rezultLst;
	}

//	
//	public void selectDate1(AjaxBehaviorEvent event) { 
//		LOGGER.error(">>> changeDate1");
//	    Object val = ((DatePicker)event.getSource()).getValue();
//	  
//		if(multi!= null) {
//			int s= multi.size();
//			seldate = "selectDate: "+ s +"/ последна дата:" + multi.get(s-1);
//		}
//	}
	
	public void selectDate(SelectEvent<Date> event) { 
		LOGGER.error(">>> selectDate");
		
		if(multi!= null) {
			int s= multi.size();
			seldate = "selectDate: "+ s +"/ последна дата:" + multi.get(s-1);
			setDate1((Date) multi.get(s-1));
			String  cmdStr = "PF('modalDateParam').show();";
			PrimeFaces.current().executeScript(cmdStr);
		}
	}
	



	public List<Date> getMulti() {
		return multi;
	}



	public void setMulti(List<Date> multi) {
		this.multi = multi;
	}



	public String getSeldate() {
		return seldate;
	}



	public void setSeldate(String seldate) {
		this.seldate = seldate;
	}


	public Date getDate1() {
		return date1;
	}


	public void setDate1(Date date1) {
		this.date1 = date1;
	}
	
	
	private List<Files> filesList;

	public List<Files> getFilesList() {
		return filesList;
	}


	public void setFilesList(List<Files> filesList) {
		this.filesList = filesList;
	}

	public void actionNewF() {
		filesList = null;
	}
	
	
	
	

	public void listenerPrime(FileUploadEvent event) throws Exception {
		UploadedFile item = event.getFile();
		String filename = item.getFileName();
		
//		if(isISO88591Encoded(filename)){
//			filename = new String(filename.getBytes("iso-8859-1"), "utf-8");
//		}
		
		X<Files> x = X.empty();
		
		Files files = new Files();
		files.setFilename(filename);
		files.setContentType(item.getContentType());
		files.setContent(item.getContent());
		
//		Boolean viewOfficial = (Boolean) getAttributes().get("viewOfficial");
//		if(viewOfficial) {
//			files.setOfficial(SysConstants.CODE_ZNACHENIE_DA);
//		}
//		
//		//files.setContent(item.getContents()); //03'2020
//		
//		if(filename.endsWith(".docx")) {
//			
//			List<VerifySignature> rez = new SignatureUtils().verifyWordExcelSigNew(files.getContent());
//			if(rez!=null && !rez.isEmpty()) {
//				files.setSigned(Constants.CODE_ZNACHENIE_DA);
//			} else {
//				files.setSigned(Constants.CODE_ZNACHENIE_NE);
//			}
//		} else if(filename.endsWith(".pdf")) {
//			
//			List<VerifySignature> rez = new SignatureUtils().verifyPDFSigNewVersion(item.getInputStream());
//			if(rez!=null && !rez.isEmpty()) {
//				files.setSigned(Constants.CODE_ZNACHENIE_DA);
//			} else {
//				files.setSigned(Constants.CODE_ZNACHENIE_NE);
//			}
//		}
		
		
//		Boolean autoSave = (Boolean) getAttributes().get("autoSave");
//		Boolean externalMode = (Boolean) getAttributes().get("externalMode");
//		
//		if (Boolean.TRUE.equals(autoSave)) {
//			try {
//			//	BaseUserData userData = (BaseUserData) JSFUtils.getManagedBean("userData");
//				
//				FilesDAO dao = new FilesDAO(getUserData());
//
//				Integer idObj = (Integer) getAttributes().get("idObj");
//				Integer codeObj = (Integer) getAttributes().get("codeObj");
//
//				if (Boolean.TRUE.equals(externalMode)) {
//					x.set(dao.saveFileObjectRest(files, idObj, codeObj));
//					
//				} else {
//					JPA.getUtil().runInTransaction(() -> x.set(dao.saveFileObject(files , idObj, codeObj)));
//				}
//				
//				 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString("ui_beanMessages", "general.succesSaveFileMsg") );
//				
//			} catch (Exception e) {
//				LOGGER.error("Exception: " + e.getMessage(), e);
//				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  "Грешка при прикачване на файл!", e.getMessage());
//			}
//			
//		} else {
			x.set(files);
//		}

		if (x.isPresent()) {
			//returnValues (getLstObjTmp(), x.get(), "listObj");
			if (filesList == null) {
				filesList = new ArrayList<>();
			}
			filesList.add(x.get());
		}
	}



	public List<Long> getDocList() {
		return docList;
	}



	public void setDocList(List<Long> docList) {
		this.docList = docList;
	}
	
	private List<SystemClassif> scList = new ArrayList<SystemClassif>();
	/**
	 * autocomplete - търсене на значение
	 *
	 * @param query
	 * @return
	 */
	public List<SystemClassif> actionComplete(String query) {
		List<SystemClassif> result = null;
		int compType = 4;
		if (query != null && !query.trim().isEmpty() || compType == 4) {
			try {
				Integer codeClassif = OmbConstants.CODE_CLASSIF_ADMIN_STR;
				if (codeClassif != null) {
					result = loadClassifList(codeClassif, false, query); // ако има интервали в края или началото - да се махнат
																			// ли?
				}

			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return result != null ? result : new ArrayList<>();
	}
	
	
	List<SystemClassif> loadClassifList(Integer codeClassif, boolean fixPrev, String query) throws DbErrorException {
			

		int lang = getUserData().getCurrentLang();

		List<SystemClassif> classifList;
		
		classifList = getSystemData().getSysClassification(codeClassif, new Date(), lang);
	

		return classifList;
	}



	public List<SystemClassif> getScList() {
		return scList;
	}



	public void setScList(List<SystemClassif> scList) {
		this.scList = scList;
	}
	
}
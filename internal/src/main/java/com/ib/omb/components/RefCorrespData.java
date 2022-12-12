package com.ib.omb.components;

import static com.ib.system.utils.SearchUtils.isEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;
//import org.primefaces.event.SelectEvent;
//import org.primefaces.event.ToggleSelectEvent;
//import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
//import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.ReferentDAO;
//import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.Referent;
import com.ib.omb.db.dto.ReferentAddress;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysClassifAdapter;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
//import com.ib.system.db.SelectMetadata;
//import com.ib.system.db.dao.FilesDAO;
//import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
//import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.ValidationUtils;

/** */
@FacesComponent(value = "refCorrespData", createTag = true)
public class RefCorrespData extends UINamingContainer {
	
	private enum PropertyKeys {
		  REF, SHOWME, EKATTE, EKATTESPEC, DOCSLIST, DOCSEARCH, DOCSELTMP, DOCSEL, SEEPERSONALDATA, VIDNARLST
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(RefCorrespData.class);
	public static final String	UIBEANMESSAGES = "ui_beanMessages";
	public static final String	BEANMESSAGES = "beanMessages";
	public static final String  MSGPLSINS = "general.pleaseInsert";
	public static final String  ERRDATABASEMSG = "general.errDataBaseMsg";
	public static final String  SUCCESSAVEMSG = "general.succesSaveMsg";
	public static final String	OBJECTINUSE		 = "general.objectInUse";
	public static final String	LABELS = "labels";
	private static final String CODEREF = "codeRef";
	private static final String MSGVALIDEIK = "refCorr.msgValidEik";
	private static final String MSGVALIDEGN = "refCorr.msgValidEgn"; 
	private static final String MSGVALIDLNCH = "refCorr.msgValidLnch"; 
	private static final String REFCORRESPMSG1 = "docu.refCorrespMsg1";
	private static final String SUCCESSDELETEMSG = "general.successDeleteMsg";

	private String errMsg = null;
	private SystemData	systemData	= null;
	private UserData	userData	= null;
	private Date			dateClassif	= null;
	private TimeZone timeZone = TimeZone.getDefault();
//	private IndexUIbean 	indexUIbean = null;
//	private String 			modalMsg = "";
	
	private Referent tmpRef;

	private int countryBG; // ще се инициализира в getter-а през системна настройка: delo.countryBG
	
	/// За сега не са включени: 
	//  1. Районите в адреса
	//  2. повече от един адрес
	//  3. имената на латиница
	//  4. гражданството
	//  5. личен номер в ЕС - за физ. лица
	//  6. данъчен номер - за юрид. лица
	
	//  7. Специфичните настройки - сеос,guid и т.н. !!!
	//  8. списък с документи
	 
	
	/**
	 * Данни на кореспондент - актуализация и разглеждане
	 * @return
	 * @throws DbErrorException
	 */
	public void initRefCorresp() {		
		//boolean modal = (Boolean) getAttributes().get("modal"); // обработката е в модален диалог (true) или не (false)
		Integer idR = (Integer) getAttributes().get(CODEREF); 
		if(idR != null) {
			loadRefCorr(idR, null, null, null);
		} else { // нов
			String srchTxt = (String) getAttributes().get("searchTxt"); 
			clearRefCorresp(srchTxt);
			
			ValueExpression expr2 = getValueExpression("searchTxt"); //зачиствам текста - искам да се изпозлва само при първото отваряне
			ELContext ctx2 = getFacesContext().getELContext();
			if (expr2 != null) {
				expr2.setValue(ctx2, null);
			}	
		}
		
		setSeePersonalData(getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_SEE_PERSONAL_DATA));
		
		if (!isSeePersonalData() && getRef().getRefType().equals(OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL)) { 
			getAttributes().put("readonly", true);
		}
		
		setShowMe(true);
		setErrMsg(null);
		LOGGER.debug("initRefCorresp");
	}

	/**
	 *  зарежда данни за корепондент по зададени критерии
	 */
	private boolean loadRefCorr(Integer idR, String eik, String egn, String lnc) {
		boolean bb = true;		
		try {
			setErrMsg(null);
			if(idR != null) {
				JPA.getUtil().runWithClose(() -> tmpRef = new ReferentDAO(getUserData()).findByCode(idR, getDateClassif(), true));
				
			} else {	
				JPA.getUtil().runWithClose(() -> tmpRef = new ReferentDAO(getUserData()).findByIdent(eik, egn, lnc, getRef().getRefType())); 
				if(tmpRef != null && !tmpRef.getId().equals(getRef().getId())) {
				    String str1 = (isEmpty(eik) ? " ЕГН" : " ЕИК" );
					setErrMsg(IndexUIbean.getMessageResourceString(BEANMESSAGES, "refCorr.loadByEikEgn", str1)); 
				}else {
					bb = false;
				}
			}			
			if(bb && tmpRef != null) {
				if(tmpRef.getAddress() == null) {
					tmpRef.setAddress(new ReferentAddress());
				}
				if(tmpRef.getKatNar() != null) {
					setVidNarList(getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_KAT_NAR_VID_NAR, tmpRef.getKatNar(), getLang(), new Date()));
				}
				setRef(tmpRef);
			} 
			tmpRef = null;
			
			LOGGER.debug("load initRefCorresp");
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на данни за кореспондент! ", e);
		}
		return bb;
	}
   
   /**
    * Зачиства данните на кореспондент - бутон "нов"
    * 
    */
   public void clearRefCorresp(String srchTxt) {
	    tmpRef = new Referent();
	    tmpRef.setRefType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_NFL); // юридическо лице
		tmpRef.setDateOt(getDateClassif());
		tmpRef.setAddress(new ReferentAddress());
		tmpRef.setRefName(srchTxt);
		setRef(tmpRef);
		tmpRef.getAddress().setAddrCountry(getCountryBG());
		tmpRef.setRefGrj(getCountryBG());
		setDocSelectedAllM(null);
		setDocSelectedTmp(null);
   }
		
	/**
    * смяна на лице - физическо/юридическо  
    */
   public void actionChTypRef() { 
	   getRef().setNflEik(null);	   
	   getRef().setFzlEgn(null);
	   getRef().setFzlLnc(null);
	   
	   getRef().setTaxOfficeNo(null);
	   getRef().setFzlLnEs(null); 
	   LOGGER.debug("actionChTypRef"); 
	   setDocSelectedAllM(null);
	   setDocSelectedTmp(null); 
	   getRef().setTipOrgan(null);
	   
	   // ако идваме от жалба и за нарушител изберат физическо лице - по подразбиране да зареди"Частноправен субект"-> "Физическо лице" 
	   if( (Boolean)getAttributes().get("jalba") && Objects.equals(getRef().getRefType(), OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL)) {
		   getRef().setKatNar(OmbConstants.CODE_ZNACHENIE_KAT_NAR_CHASTEN_SUBEKT);	   
		   actionChangeKatNar();
		   getRef().setVidNar(OmbConstants.CODE_ZNACHENIE_VID_NAR_FZL);
	   }else {
		   getRef().setKatNar(null);	   
		   getRef().setVidNar(null);
	   }
	   errMsg = null;
   }
  
   /**
    * При смяна на държава - да се нулира полето за ЕКАТЕ
    */
   public void  actionChangeCountry() {
	   getRef().getAddress().setPostBox(null);
	   getRef().getAddress().setPostCode(null);
	   getRef().getAddress().setEkatte(null);
   }
   
   
   /**
    * зарежда кореспондент по зададен еик
    */
	 public void actionLoadByEIK() { 
		//системна настройка - Допуска се дублиране на ЕИК при въвеждане на нефизическо лице  (1- да / 0 - не); по подразбиране - не
		try {
			String	setting = getSystemData().getSettingsValue("delo.allowEikDuplicate"); 	
			if (setting == null || !Objects.equals(Integer.valueOf(setting), SysConstants.CODE_ZNACHENIE_DA)) {
				if (!isEmpty(getRef().getNflEik()) && !ValidationUtils.isValidBULSTAT(getRef().getNflEik())) {

					JSFUtils.addMessage(this.getClientId(FacesContext.getCurrentInstance()) + ":eik",
							FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(BEANMESSAGES, MSGVALIDEIK));
					
					setErrMsg(IndexUIbean.getMessageResourceString(BEANMESSAGES, MSGVALIDEIK)); 
				} else {

					loadRefCorr(null, getRef().getNflEik(), null, null);
				}
			}
		
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addErrorMessage(e.getMessage(), e);
		}

		LOGGER.debug("actionLoadByEIK");
	 }
	 
	 /**
    * зарежда кореспондент по зададен егн
    */
	 public void actionLoadByEGN() {
		// Винаги да се прави проверка за дублирано ЕГН, ако се въвежда физическо лице
		if (!isEmpty(getRef().getFzlEgn()) && !ValidationUtils.isValidEGN(getRef().getFzlEgn())) {
			
			JSFUtils.addMessage(this.getClientId(FacesContext.getCurrentInstance()) + ":egn",
					FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(BEANMESSAGES, MSGVALIDEGN));
			
			setErrMsg(IndexUIbean.getMessageResourceString(BEANMESSAGES, MSGVALIDEGN)); 
		} else {
			
			loadRefCorr(null, null, getRef().getFzlEgn(), null);
		}
		
		LOGGER.debug("actionLoadByEGN");
	 }
   
	/**
	 * зарежда кореспондент по зададен ЛНЧ
	 */
	public void actionLoadByLNCH() {
		// Винаги да се прави проверка за дублирано ЕГН, ако се въвежда физическо лице
		if (!isEmpty(getRef().getFzlLnc()) && !ValidationUtils.isValidLNCH(getRef().getFzlLnc())) {

			JSFUtils.addMessage(this.getClientId(FacesContext.getCurrentInstance()) + ":lnch",
					FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(BEANMESSAGES, MSGVALIDLNCH));

			setErrMsg(IndexUIbean.getMessageResourceString(BEANMESSAGES, MSGVALIDLNCH));
		} else {

			loadRefCorr(null, null, null, getRef().getFzlLnc());
		}

		LOGGER.debug("actionLoadByLNCH");
	}

   /** 
    * Запис на кореспондент 
    */
   public void actionSave(){ 
	    errMsg = " Моля, въведете задължителната информация!";
		if(checkData()) {
			errMsg = null;
			try { 
				LOGGER.debug("actionSave>>>> ");
			   JPA.getUtil().runInTransaction(() -> this.tmpRef = new ReferentDAO(getUserData()).save(getRef()));
			  
			   getSystemData().mergeReferentsClassif(tmpRef, false );	
								
			   if( tmpRef != null && tmpRef.getCode() != null) {
				   //връща id на избрания кореспондент
				    ValueExpression expr2 = getValueExpression(CODEREF);
					ELContext ctx2 = getFacesContext().getELContext();
					if (expr2 != null) {
						expr2.setValue(ctx2, tmpRef.getCode());
					}	
			   }	
			   
			    // извиква remoteCommnad - ако има такава....
				String remoteCommnad = (String) getAttributes().get("onComplete");
				if (remoteCommnad != null && !"".equals(remoteCommnad)) {
					PrimeFaces.current().executeScript(remoteCommnad);
				}
				
//				if(tmpRef != null && tmpRef.getAddress() == null) {
//					tmpRef.setAddress(new ReferentAddress());
//				}
//				setRef(tmpRef); // излишно е, ако веднага при запис се затваря модалния, иначе не е....
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(UIBEANMESSAGES, SUCCESSAVEMSG) );
			} catch (BaseException e) {			
				LOGGER.error("Грешка при запис на кореспондент ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG));
			}
		} 
   }
 

   /**
    * Проверка за валидни данни
    * @return 
    */
	public boolean checkData() {
		boolean flagSave = true;	
		FacesContext context = FacesContext.getCurrentInstance();
	    String clientId = null;	  
	    tmpRef = getRef();
	    if (context != null && tmpRef != null ) {
		   clientId =  this.getClientId(context);		
		   if(isEmpty(tmpRef.getRefName())) {
				JSFUtils.addMessage(clientId+":nameCorr",FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(BEANMESSAGES,"general.msgRefCorrName"));
				flagSave = false;	
		   }
	        
//		   if(getRef().getAddress() != null && getRef().getAddress().getAddrCountry() != null && getRef().getAddress().getEkatte() == null) {
//			   // за да се запише адреса се изисква да се въведе адрес и/или населено място!!!
//			  //String msgAdrtxt =  "general.msgRefCorrAdrTxt";
//			  if(getRef().getAddress().getAddrCountry().equals(getCountryBG())) {
//				  String msgAdrtxt =  "general.msgRefCorrAdr1";
//				  JSFUtils.addMessage(clientId+":mestoC:аutoCompl",FacesMessage.SEVERITY_ERROR,
//							IndexUIbean.getMessageResourceString(BEANMESSAGES,msgAdrtxt));
//			  }
////			  JSFUtils.addMessage(clientId+":adrTxt",FacesMessage.SEVERITY_ERROR,
////						IndexUIbean.getMessageResourceString(BEANMESSAGES,msgAdrtxt));
//			  
//			  flagSave = false;	
//		   }
		   
		   //допустимо е- без държава и адрес или само чужда държава
		   if( getRef().getAddress() != null &&
				   Integer.valueOf(getCountryBG()).equals(getRef().getAddress().getAddrCountry()) &&
				   getRef().getAddress().getEkatte() == null) {
			   // ако e в  България - задължително да се въведе населено място		
				  String msgAdrtxt =  "general.msgRefCorrAdr1";
				  JSFUtils.addMessage(clientId+":mestoC:аutoCompl",FacesMessage.SEVERITY_ERROR,
							IndexUIbean.getMessageResourceString(BEANMESSAGES,msgAdrtxt));
				  flagSave = false;
		   }
		   
		   boolean errEikOrEgn = false;
		   String tmp = tmpRef.getNflEik() == null ? null : tmpRef.getNflEik().trim();  
		   tmpRef.setNflEik(tmp);
		   tmp = tmpRef.getFzlEgn() == null ? null : tmpRef.getFzlEgn().trim();  
		   tmpRef.setFzlEgn(tmp);
		   if (!isEmpty(tmpRef.getNflEik()) &&  !ValidationUtils.isValidBULSTAT(tmpRef.getNflEik()) ){
			  			   JSFUtils.addMessage(clientId+":eik",FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(BEANMESSAGES, MSGVALIDEIK));
			   if (!flagSave) {
				   errMsg = IndexUIbean.getMessageResourceString(BEANMESSAGES, REFCORRESPMSG1);	//" Моля, въведете задължителната информация! ";
				   errMsg += IndexUIbean.getMessageResourceString(BEANMESSAGES, MSGVALIDEIK);	
			   }			  
			   flagSave = false;
			   errEikOrEgn = true;
		   } else if (!isEmpty(tmpRef.getFzlEgn()) &&  !ValidationUtils.isValidEGN(tmpRef.getFzlEgn()) ){			   
			   JSFUtils.addMessage(clientId+":egn",FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(BEANMESSAGES, MSGVALIDEGN));
			   if (!flagSave) {
				   errMsg = IndexUIbean.getMessageResourceString(BEANMESSAGES, REFCORRESPMSG1);	//" Моля, въведете задължителната информация! ";
				   errMsg += IndexUIbean.getMessageResourceString(BEANMESSAGES, MSGVALIDEGN);	
			   }			  
			   
			   flagSave = false;
			   errEikOrEgn = true;
		   }
		   
		   if (!isEmpty(tmpRef.getFzlLnc()) && !ValidationUtils.isValidLNCH(tmpRef.getFzlLnc())) {
			   
			   JSFUtils.addMessage(clientId+":lnch",FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(BEANMESSAGES, MSGVALIDLNCH));
				flagSave = false;	
		   }
		   
		   if (!isEmpty(tmpRef.getContactEmail()) && !ValidationUtils.isEmailValid(tmpRef.getContactEmail())) {
			   
			   JSFUtils.addMessage(clientId+":contactEmail",FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(UIBEANMESSAGES, "general.validE-mail"));
				flagSave = false;	
		   }
		   
	       if((Boolean)getAttributes().get("npm") && tmpRef.getTipOrgan() == null) {
	    	  JSFUtils.addMessage(clientId+":sTipOrgan",FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(BEANMESSAGES, "corespNpm.typeNpm"));
			   flagSave = false;	
	       }
	       
	       // ако е избрано физическо лице -  за категория и вид наришител може да е празно или да е само: "Частноправен субект"-> "Физическо лице" 
	       String errMsg2 = "";
		   if(Objects.equals(getRef().getRefType(), OmbConstants.CODE_ZNACHENIE_REF_TYPE_FZL)) {
			   if( getRef().getKatNar() != null &&  !Objects.equals(getRef().getKatNar(), OmbConstants.CODE_ZNACHENIE_KAT_NAR_CHASTEN_SUBEKT)) {
				   JSFUtils.addMessage(clientId+":sKatNar",FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(BEANMESSAGES, "refCorr.msgFZLCat"));
				   errMsg2 = " "+IndexUIbean.getMessageResourceString(BEANMESSAGES, "refCorr.msgFZLCat");
				   flagSave = false;	
			   }
			   if( getRef().getVidNar() != null &&  !Objects.equals(getRef().getVidNar(), OmbConstants.CODE_ZNACHENIE_VID_NAR_FZL)) {
				   JSFUtils.addMessage(clientId+":sVidNar",FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(BEANMESSAGES, "refCorr.msgFZLVid"));
				   errMsg2 += " " +IndexUIbean.getMessageResourceString(BEANMESSAGES, "refCorr.msgFZLVid");
				   flagSave = false;	
			   }
		   }
		   
		   if (!flagSave && !errEikOrEgn) {			   
			   errMsg = IndexUIbean.getMessageResourceString(BEANMESSAGES, REFCORRESPMSG1) + errMsg2; //" Моля, въведете задължителната информация! ";	
		   }
		   
	    } else {
		   flagSave = false;
	    }		
		return flagSave;
	}

   
   /** 
    * Изтриване на кореспондент 
    */
   public void actionDelete(){ 
		try {
			LOGGER.debug("actionDelete>>>> ");
						
			JPA.getUtil().runInTransaction(() -> new ReferentDAO(getUserData()).delete(getRef()));
		   
			getSystemData().mergeReferentsClassif(getRef(), true );	
			
		
			ValueExpression expr2 = getValueExpression(CODEREF);
			ELContext ctx2 = getFacesContext().getELContext();
			if (expr2 != null) {
				expr2.setValue(ctx2, null);
			}	
			
		    // извиква remoteCommnad - ако има такава....
			String remoteCommnad = (String) getAttributes().get("onComplete");
			if (remoteCommnad != null && !"".equals(remoteCommnad)) {
				PrimeFaces.current().executeScript(remoteCommnad);
			}
						
		
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,  IndexUIbean.getMessageResourceString(UIBEANMESSAGES, SUCCESSDELETEMSG) );
		} catch (ObjectInUseException e) {			
			LOGGER.error("Грешка при изтриване на кореспондент! ObjectInUseException = {}", e.getMessage()); 
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на кореспондент ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG));
		}
		
   }
 
   /** 
    * коригиране данни на кореспондент - изп. се само, ако е в модален прозорец
    * изивква се при затваряне на модалния прозореца (onhide) 
    * 
    */
   public void actionHideModal() {		
	   // за сега няма да се ползва
	   setRef(null);
	   setShowMe(false);
	   LOGGER.debug("actionHideModal>>>> ");
	}
   
//   
//   // Няма да се изпозлва за сега!  
//   //TODO тук все още не са включени - нарушителите в жалбите и проверяваните органи НПМ и самосезиране!!
//   public void actionLoadDocsList() {//	
//	   try {
//		   
//		   setDocSelectedAllM(null);
//		   setDocSelectedTmp(null);
//			
//		   // списък документи, в които участва кореспондента
//	
//			if (getRef().getCode() == null) {
//				setDocsList(null);
//			
//			} else {
//
//				SelectMetadata smd = new DocDAO(getUserData()).createSelectCorrespondentDocs(getRef().getCode());				
//				setDocsList(new LazyDataModelSQL2Array(smd, "a1 desc"));
//			}
//		
//	   } catch (DbErrorException e) {
//		   LOGGER.error(e.getMessage(), e);
//		   JSFUtils.addErrorMessage(e.getMessage(), e);
//		}  
//   }
//   
//	public String actionGotoViewDoc(Integer idObj) {
//		return "docView.xhtml?faces-redirect=true&idObj=" + idObj;
//	}
//	
//	public String actionGotoEditDoc(Integer idObj) {
//		return "docEdit.jsf?faces-redirect=true&idObj=" + idObj;
//	}
//	
//	/**
//	 * Множествен избор на документи
//	 *
//	 * Избира всички редове от текущата страница
//	 * @param event
//	 */
//	  public void onRowSelectAll(ToggleSelectEvent event) {    
//    	
//		  List<Object[]> tmpL = new ArrayList<>();    	
//		  tmpL.addAll(getDocSelectedAllM());
//    	
//			if (event.isSelected()) {
//
//				for (Object[] obj : getDocSelectedTmp()) {
//					if (obj != null && obj.length > 0) {
//						boolean bb = true;
//						Long l2 = Long.valueOf(obj[0].toString());
//						
//						for (Object[] j : tmpL) {
//							Long l1 = Long.valueOf(j[0].toString());
//							if (l1.equals(l2)) {
//								bb = false;
//								break;
//							}
//						}
//						
//						if (bb) {
//							tmpL.add(obj);
//						}
//					}
//				}
//    	
//		  } else {
//	    	
//			List<Object[]> tmpLPageC = getDocsList().getResult();// rows from current page....
//
//			for (Object[] obj : tmpLPageC) {
//				if (obj != null && obj.length > 0) {
//					Long l2 = Long.valueOf(obj[0].toString());
//					for (Object[] j : tmpL) {
//						Long l1 = Long.valueOf(j[0].toString());
//						if (l1.equals(l2)) {
//							tmpL.remove(j);
//							break;
//						}
//					}
//				}
//			}	
//		}
//		
//		  setDocSelectedAllM(tmpL);
//		  if (LOGGER.isDebugEnabled()) {
//			LOGGER.debug("onToggleSelect->>");
//		  }
//	}
//		    
//    /** 
//     * Select one row
//     * @param event
//     */
//    public void onRowSelect(SelectEvent<?> event) {    	
//    	if(event!=null  && event.getObject()!=null) {
//    		List<Object[]> tmpList =  getDocSelectedAllM();
//    		
//    		Object[] obj = (Object[]) event.getObject();
//    		boolean bb = true;
//    		Integer l2 = Integer.valueOf(obj[0].toString());
//			for (Object[] j : tmpList) { 
//				Integer l1 = Integer.valueOf(j[0].toString());        			
//	    		if(l1.equals(l2)) {
//	    			bb = false;
//	    			break;
//	    		}
//	   		}
//			if(bb) {
//				tmpList.add(obj);
//				setDocSelectedAllM(tmpList);   
//			}
//    	}	    	
//    	if (LOGGER.isDebugEnabled()) {
//    		LOGGER.debug("1 onRowSelectIil->>{}",getDocSelectedAllM().size());
//    	}
//    }
//		 
//		    
//    /**
//     * unselect one row
//     * @param event
//     */
//    public void onRowUnselect(UnselectEvent<?> event) {
//    	if(event!=null  && event.getObject()!=null) {
//    		Object[] obj = (Object[]) event.getObject();
//    		List<Object[] > tmpL = new ArrayList<>();
//    		tmpL.addAll(getDocSelectedAllM());
//    		for (Object[] j : tmpL) {
//    			Integer l1 = Integer.valueOf(j[0].toString());
//    			Integer l2 = Integer.valueOf(obj[0].toString());
//	    		if(l1.equals(l2)) {
//	    			tmpL.remove(j);
//	    			setDocSelectedAllM(tmpL);
//	    			break;
//	    		}
//    		}
//    		if (LOGGER.isDebugEnabled()) {
//    			LOGGER.debug( "onRowUnselectIil->>{}",getDocSelectedAllM().size());
//    		}
//    	}
//    }
//
//    /**
//     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга
//     */
//    public void onPageUpdateSelected(){
//    	if (getDocSelectedAllM() != null && !getDocSelectedAllM().isEmpty()) {
//    		getDocSelectedTmp().clear();
//    		getDocSelectedTmp().addAll(getDocSelectedAllM());
//    	}	
//    	if (LOGGER.isDebugEnabled()) {
//    		LOGGER.debug( " onPageUpdateSelected->>{}",getDocSelectedTmp().size());
//    	}
//    }    
//    
//    /**
//     * Метод за изтриване на документи, свързани с кореспондента при заличаване на кореспондент
//     */
//    public void actionDeleteDocs() {
//    	
//    	try {
//    	
//			for (Object[] doc : getDocSelectedAllM()) {
//				
//				Integer docId = SearchUtils.asInteger(doc[0]);
//
//				JPA.getUtil().runInTransaction(() -> {					
//
//					new DocDAO(getUserData()).deleteById(docId);
//					
//					FilesDAO filesDao = new FilesDAO(getUserData());		
//					List<Files> filesList = filesDao.selectByFileObject(docId, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC); 
//					
//					if (filesList != null && !filesList.isEmpty()) {
//						for (Files f : filesList) {
//							filesDao.deleteFileObject(f);	
//						}
//					}					
//				});
//				
//				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,  IndexUIbean.getMessageResourceString(UIBEANMESSAGES, SUCCESSDELETEMSG) );				
//			} 
//			
//			actionLoadDocsList();			
//    	
//    	} catch (ObjectInUseException e) {			
//			LOGGER.error("Грешка при изтриване на документа - обекта се използва!", e); 
//			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(UIBEANMESSAGES, OBJECTINUSE), e.getMessage());
//		
//    	} catch (BaseException e) {			
//			LOGGER.error("Грешка при изтриване на документа - грешка при работа с базата данни!", e);			
//			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
//		
//    	} finally {
//			PrimeFaces.current().executeScript("scrollToErrors()");
//		}
//    	
//    }
//    
//    /**
//     * Метод за изтриване на файлове към документи, за които е отбелязано, че съдържат лична информация при заличаване на кореспондент
//     */
//    public void actionDeleteFiles() {
//    	
//    	try {
//        	
//			for (Object[] doc : getDocSelectedAllM()) {
//				
//				Integer docId = SearchUtils.asInteger(doc[0]);
//
//				JPA.getUtil().runInTransaction(() -> {	
//					
//					FilesDAO filesDao = new FilesDAO(getUserData());		
//					List<Files> filesList = filesDao.selectByFileObjectDop(docId, OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC); 
//					
//					if (filesList != null && !filesList.isEmpty()) {
//						boolean delFile = false;
//						for (Files f : filesList) {
//							if(f.getPersonalData() != null && f.getPersonalData().equals(SearchUtils.asInteger(OmbConstants.CODE_ZNACHENIE_DA))) {
//								delFile = true;
//
//								f.setParrentID(docId);
//								f.setParentObjCode(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
//								
//								filesDao.deleteFileObject(f);
//								
//								// ъпдейтване бройката на файловете
//								Doc tmpDoc = new Doc();
//								tmpDoc.setId(docId); 
//								Integer countFiles = SearchUtils.asInteger(doc[8]) - 1;
//								new DocDAO(getUserData()).updateCountFiles(tmpDoc, countFiles);
//							}
//						}
//						
//						if (delFile) {							
//							JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(UIBEANMESSAGES, SUCCESSDELETEMSG));
//						} else {
//							JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(BEANMESSAGES, "correspForget.msgNotFileForDelete")); 
//						}
//					}					
//				});
//			} 
//			
//			actionLoadDocsList();
//    	
//    	} catch (ObjectInUseException e) {			
//			LOGGER.error("Грешка при изтриване на файлове, съдържащи лична информация - обекта се използва!", e); 
//			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(UIBEANMESSAGES, OBJECTINUSE), e.getMessage());
//		
//    	} catch (BaseException e) {			
//			LOGGER.error("Грешка при изтриване на файлове, съдържащи лична информация - грешка при работа с базата данни!", e);			
//			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
//    	
//    	} finally {
//			PrimeFaces.current().executeScript("scrollToErrors()");
//		} 
//    	
//    }   
//
//    
//    
	/** @return */
	public boolean isShowMe() {
		return (Boolean) getStateHelper().eval(PropertyKeys.SHOWME, false);
	}
	
	/** @param showMe */
	public void setShowMe(boolean showMe) {
		getStateHelper().put(PropertyKeys.SHOWME, showMe);
	}
	
	/** @return */
	public boolean isSeePersonalData() {
		return (Boolean) getStateHelper().eval(PropertyKeys.SEEPERSONALDATA, false);
	}
	/** @param seePersonalData */
	public void setSeePersonalData(boolean seePersonalData) {
		getStateHelper().put(PropertyKeys.SEEPERSONALDATA, seePersonalData);
	}

	/** @return the dateClassif */
	private Date getDateClassif() {
		if (this.dateClassif == null) {
			this.dateClassif = (Date) getAttributes().get("dateClassif");
			if (this.dateClassif == null) {
				this.dateClassif = new Date();
			}
		}
		return this.dateClassif;
	}
	
	private SystemData getSystemData() {
		if (this.systemData == null) {
			this.systemData =  (SystemData) JSFUtils.getManagedBean("systemData");
		}
		return this.systemData;
	}

	/** @return the userData */
	private UserData getUserData() {
		if (this.userData == null) {
			this.userData = (UserData) JSFUtils.getManagedBean("userData");
		}
		return this.userData;
	}
	
	/** 
	 *   1 само области; 2 - само общини; 3 - само населени места; без специфики - всикчи
	 * @return
	 */
	@SuppressWarnings({ "unchecked" })
	public Map<Integer, Object> getSpecificsEKATTE() {
		Map<Integer, Object> eval = (Map<Integer, Object>) getStateHelper().eval(PropertyKeys.EKATTESPEC, null);
		return eval != null ? eval : Collections.singletonMap(SysClassifAdapter.EKATTE_INDEX_TIP, 3);
	}

	/** @return */
	public Integer getLang() {
		return getUserData().getCurrentLang();
	}
	
	/** @return */
	public Date getCurrentDate() {
		return getDateClassif();
	}

//	public String getModalMsg() {
//		return modalMsg;
//	}
//
//
//	public void setModalMsg(String modalMsg) {
//		this.modalMsg = modalMsg;
//	}

	public Referent getRef() {
		return (Referent) getStateHelper().eval(PropertyKeys.REF, null);
	}

	public void setRef(Referent ref) {
		getStateHelper().put(PropertyKeys.REF, ref);
	}

	public String getErrMsg() {
		return errMsg;
	}

	public void setErrMsg(String errMsg) {
		this.errMsg = errMsg;
	}
	
	public DocSearch getDocSearch() {
		return (DocSearch) getStateHelper().eval(PropertyKeys.DOCSEARCH, null);
	}

	public void setDocSearch(DocSearch docSearch) {
		getStateHelper().put(PropertyKeys.DOCSEARCH, docSearch);
	}
	
	public LazyDataModelSQL2Array getDocsList() {
		return (LazyDataModelSQL2Array) getStateHelper().eval(PropertyKeys.DOCSLIST, null);
	}

	public void setDocsList(LazyDataModelSQL2Array docsList) {
		getStateHelper().put(PropertyKeys.DOCSLIST, docsList);
	}
	
	@SuppressWarnings("unchecked")
	public List<Object[]> getDocSelectedTmp() {
		List<Object[]> eval = (List<Object[]>) getStateHelper().eval(PropertyKeys.DOCSELTMP, null);
		return eval != null ? eval : new ArrayList<>();		
	}

	public void setDocSelectedTmp(List<Object[]> docSelectedTmp) {
		getStateHelper().put(PropertyKeys.DOCSELTMP, docSelectedTmp);
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getDocSelectedAllM() {
		List<Object[]> eval = (List<Object[]>) getStateHelper().eval(PropertyKeys.DOCSEL, null);
		return eval != null ? eval : new ArrayList<>();
	}

	public void setDocSelectedAllM(List<Object[]> docSelectedAllM) {
		getStateHelper().put(PropertyKeys.DOCSEL, docSelectedAllM);
	}

	public TimeZone getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	public int getCountryBG() {
		if (this.countryBG == 0) {
			try {
				this.countryBG = Integer.parseInt(getSystemData().getSettingsValue("delo.countryBG"));
			} catch (Exception e) {
				LOGGER.error("Грешка при определяне на код на държава България от настройка: delo.countryBG", e);
			}
		}
		return this.countryBG;
	}
	
	/**
	 * При промяна на категорията, да се зареди съответния списък с видове
	 */
	public void actionChangeKatNar() {
	 if(getRef().getKatNar() == null) {
		 setVidNarList(null);
	  }else {
		 try {
			getRef().setVidNar(null);
			setVidNarList(getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_KAT_NAR_VID_NAR, getRef().getKatNar(), getLang(), new Date()));
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на логически списък Категория на нарушител - Вид на нарушител", e);
		}
	  }
	}
	
	/**
	 * Вид (нарушител) - зависи от "категория"
	 */
	@SuppressWarnings("unchecked")
	public List<SystemClassif> getVidNarList() {
		List<SystemClassif> eval = (List<SystemClassif>) getStateHelper().eval(PropertyKeys.VIDNARLST, null);
		return eval != null ? eval : new ArrayList<>();
	}

	public void setVidNarList(List<SystemClassif> vidNarList) {
		getStateHelper().put(PropertyKeys.VIDNARLST, vidNarList);
	}
	
}
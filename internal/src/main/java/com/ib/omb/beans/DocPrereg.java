package com.ib.omb.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.facelets.FaceletContext;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.LockObjectDAO;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.db.dto.DeloDoc;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.search.DeloSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * Пререгистриране на документи
 * 
 * @author s.arnaudova
 **/

@Named
@ViewScoped
public class DocPrereg extends IndexUIbean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4745597874540624314L;

	public static final String UIBEANMESSAGES = "ui_beanMessages";
	public static final String BEANMSG = "beanMessages";
	public static final String ERRDATABASEMSG = "general.errDataBaseMsg";
	public static final String SUCCESSAVEMSG = "general.succesSaveMsg";
	public static final String MSGPLSINS = "general.pleaseInsert";
	public static final String LABELS = "labels";
	public static final String PREREGFORM = "docPreregForm";
	private static final Logger LOGGER = LoggerFactory.getLogger(DocPrereg.class);

	private Doc document;
	private transient DocDAO docDao;

	private UserData ud;
	private SystemData sd;

	private boolean avtomNo = false;
	private boolean avtomNoDisabled = false;

	private String oldRn;
	private Integer oldPored;
	private Date oldDate;

	private transient Map<Integer, Object> specificsRegister; // списък допустими регистри

	private Date decodeDate = new Date();

	private boolean nastrZadnaData;
	
	/**
	 * преписка, в която се влага документа при ръчно писане на рег. номера - за нов документ
	 */
	private DeloDoc deloDocPrep; 					
	

	/**
	 * избор на преписка при ръчно въвеждане на номер
	 */

	private transient Object[] selectedDelo;		
	
	@PostConstruct
	void initData() {
		LOGGER.info("INIT DOCPREREG!!!!!");

		try {
			ud = getUserData(UserData.class);
			docDao = new DocDAO(getUserData());
			sd = (SystemData) getSystemData();

			String param = JSFUtils.getRequestParameter("idObj");
			FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes()
					.get(FaceletContext.FACELET_CONTEXT_KEY);
			String param2 = (String) faceletContext.getAttribute("isPrereg");

			Integer docId = null;
			int isPrereg = 0;
			if (!SearchUtils.isEmpty(param)) {
				docId = Integer.valueOf(param);

				if (!SearchUtils.isEmpty(param2)) {
					isPrereg = Integer.valueOf(param2);
				}
			}

			boolean checkLockDoc = true;
			if (isPrereg == 0 && docId != null) {
				checkLockDoc = checkForLock(docId); // проверка за заключен документ
				if (checkLockDoc) {
					lockDoc(docId); // отключване на всички обекти за потребителя(userId) и заключване на док.,
				}
			}
			if (docId != null && checkLockDoc) {
				loadDocumentEdit(docId);
				specTypeDocPrereg(this.document.getDocType());
			}

			oldRn = document.getRnDoc();
			oldPored = document.getPoredDelo(); // nomer pored w delo
			oldDate = document.getDocDate();
			
			setNastrZadnaData(false);
			String param1 = sd.getSettingsValue("delo.regOldDateDoc"); // 	Позволява се въвеждане на документи със задна дата 1-да/ 0-не
			if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
				setNastrZadnaData(true);
			}
		} catch (Exception e) {
			LOGGER.error("Грешка при зареждане на данни за документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
		}
	}

	/**
	 * Проверка за заключен документ
	 * 
	 * @param idObj
	 * @return
	 */
	private boolean checkForLock(Integer idObj) {
		boolean res = true;
		LockObjectDAO daoL = new LockObjectDAO();
		try {
			Object[] obj = daoL.check(ud.getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, idObj);
			if (obj != null) {
				res = false;
				String msg = getSystemData().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR,
						Integer.valueOf(obj[0].toString()), getUserData().getCurrentLang(), new Date()) + " / "
						+ DateUtils.printDate((Date) obj[1]);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,
						getMessageResourceString(LABELS, "docu.docLocked"), msg);
			}

		} catch (DbErrorException e) {
			LOGGER.error("Грешка при проверка за заключен документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
		}

		return res;
	}

	/**
	 * Заключване на документ, като преди това отключва всички обекти, заключени от
	 * потребителя
	 * 
	 * @param idObj
	 */
	public void lockDoc(Integer idObj) {
		if (LOGGER.isErrorEnabled()) {
			LOGGER.error(String.format("lockDoc %s", getUd().getPreviousPage()));
		}
		
		LockObjectDAO daoL = new LockObjectDAO();
		try {

			JPA.getUtil().runInTransaction(
					() -> daoL.lock(ud.getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC, idObj, null));

		} catch (BaseException e) {
			LOGGER.error("Грешка при заключване на документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
		}
	}

	/**
	 * Зарежда данните на документа за редактиране
	 * 
	 * @param docId
	 * @return
	 */
	private void loadDocumentEdit(Integer docId) {
		try {

			JPA.getUtil().runWithClose(() -> this.document = this.docDao.findById(docId));

			// всички значения от класификации да се разкодират към датата на документа
			this.setDecodeDate(this.document.getDocDate());

		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на документа! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
		}
	}

	/**
	 * при излизане от страницата - отключва обекта и да го освобождава за
	 * актуализация от друг потребител
	 */
	@PreDestroy
	public void unlockDoc() {
		if (!ud.isReloadPage()) {
			LockObjectDAO daoL = new LockObjectDAO();
			try {
				
				JPA.getUtil().runInTransaction(() -> daoL.unlock(ud.getUserId()));
				
			}  catch (BaseException e) {
				LOGGER.error("Грешка при отключване на документ! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
			}
			ud.setPreviousPage(null);
		}
		ud.setReloadPage(false);
	}

	public void actionChangeAvtomNoPrereg() {
		if (this.avtomNo) {
			this.document.setRnDoc(null);
			this.document.setPoredDelo(null);
		}
	}

	/**
	 * При промяна на вида документ
	 * 
	 * @return
	 */
	public void actionChangeDocVidPrereg() {
		try {

			Object[] docPreregSettings = docDao.findDocSettings(document.getRegistraturaId(), document.getDocVid(),
					getSystemData());

			if (docPreregSettings != null && docPreregSettings[1] != null) {

				Integer register = Integer.valueOf(docPreregSettings[1].toString());
				Integer docTypeReg = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRI,
						register, getUserData().getCurrentLang(), new Date(),
						OmbClassifAdapter.REGISTRI_INDEX_DOC_TYPE);
				if (docTypeReg == null || docTypeReg.equals(document.getDocType())) {
					this.document.setRegisterId(register);
				}
			}

		} catch (Exception e) {
			LOGGER.error("Грешка при промяна на регистър)! ", e);
		}
	}

	/**
	 * При промяна на регистъра
	 */
	public void actionChangeRegister() {
		Integer alg = null;
		avtomNoDisabled = false;

		try {

			if (this.document.getRegisterId() != null) {
				alg = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRI,
						document.getRegisterId(), getUserData().getCurrentLang(), new Date(),
						OmbClassifAdapter.REGISTRI_INDEX_ALG);
			}
			if (alg != null && alg.equals(OmbConstants.CODE_ZNACHENIE_ALG_FREE)) {
				this.avtomNo = false; // да се забрани автом. генер. на номера! Да се прави проверка за въведен номер,
										// ако алгоритъмът е "произволен рег.номер"
				avtomNoDisabled = true;
			} else if (SearchUtils.isEmpty(document.getRnDoc())) {
				this.avtomNo = true; // да се промени според регистъра, само ако вече няма нищо въведено в полето за
										// номер на документ
			}

		} catch (DbErrorException e) {
			LOGGER.error("Грешка при промяна на регистъра на деловоден документ )! ", e);
		}

	}

	private void specTypeDocPrereg(Integer typeDoc) {
		if(specificsRegister == null) {
			specificsRegister = new HashMap<>();
			//specificsRegister.put(OmbClassifAdapter.REGISTRI_INDEX_REGISTRATURA, Optional.of(getUserData(UserData.class).getRegistratura())); //?? трябва ли да го има
			specificsRegister.put(OmbClassifAdapter.REGISTRI_INDEX_VALID, SysConstants.CODE_ZNACHENIE_DA);
    	}
    	if (Objects.equals(typeDoc, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK)) { // изрично само за работни
    		specificsRegister.put(OmbClassifAdapter.REGISTRI_INDEX_DOC_TYPE, typeDoc);
    		
    	} else { // за конкретния тип док + тези регистри , за които не е зададен тип документ
    		specificsRegister.put(OmbClassifAdapter.REGISTRI_INDEX_DOC_TYPE, Optional.of(typeDoc));
    	}
	}

	/**
	 * Запис
	 * 
	 */
	public void actionSavePrereg() {

		if (checkDataDoc()) {

			Integer idDelo = (deloDocPrep == null) ? null : deloDocPrep.getDeloId();
			try {
				if (!avtomNo && (!Objects.equals(oldRn, document.getRnDoc()) ||
						       (SystemData.isDocPoredDeloGen() && !Objects.equals(oldPored, document.getPoredDelo())))) {
					String errorRn = docDao.validateRnDoc(document, idDelo, sd, false);
					if (errorRn != null) {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, errorRn);
						return;
					}
		
				}
				

				JPA.getUtil().runInTransaction(() -> {
					String tmpDocInfo = "Пререгистриран - Стар рег. номер: " + DocDAO.formRnDocDate(oldRn, oldDate, oldPored);

					if (document.getDocInfo() != null) {
						tmpDocInfo += " \n" + document.getDocInfo();
					}

					document.setDocInfo(tmpDocInfo);
					LOGGER.debug(document.getDocInfo());

					this.document = this.docDao.save(document, false, idDelo, null, getSystemData());
				});

				avtomNo = false;
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
						getMessageResourceString(UIBEANMESSAGES, SUCCESSAVEMSG));

				oldRn = document.getRnDoc();
				oldPored = document.getPoredDelo();
				oldDate = document.getDocDate();
			} catch (BaseException e) {
				LOGGER.error("Грешка при запис на документа! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
			}
		}
	}


	public boolean checkDataDoc() {
		boolean flagSave = true;

		if (!avtomNo && SearchUtils.isEmpty(document.getRnDoc())) {
			JSFUtils.addMessage(PREREGFORM + ":regNPrereg", FacesMessage.SEVERITY_ERROR, getMessageResourceString(
					UIBEANMESSAGES, MSGPLSINS, getMessageResourceString(LABELS, "repDoc.regnom")));
			flagSave = false;
		}

		if (document.getDocDate() == null) {
			JSFUtils.addMessage(PREREGFORM + ":regDatPrereg", FacesMessage.SEVERITY_ERROR, getMessageResourceString(
					UIBEANMESSAGES, MSGPLSINS, getMessageResourceString(LABELS, "docu.docDate")));
			flagSave = false;
		}

		if (document.getDocVid() == null) {
			JSFUtils.addMessage(PREREGFORM + ":dVidPrereg:аutoCompl", FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, MSGPLSINS, getMessageResourceString(LABELS, "docu.vid")));
			flagSave = false;
		}

		if (document.getRegisterId() == null) {
			JSFUtils.addMessage(PREREGFORM + ":registerIdPrereg:аutoCompl", FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, MSGPLSINS,
							getMessageResourceString(LABELS, "docu.register")));
			flagSave = false;
		}

		return flagSave;
	}

	/**
	* Ръчно въвеждане на номер на документ
    * Търсене на преписка с въведения номер - при излизане от полето
     * @param rnEQ  - true- пълно съвпадение на номера
	 */
   public void actionSearchRnDelo(boolean rnEQ) {
	   if (!avtomNo && !SearchUtils.isEmpty(document.getRnDoc())) {
			document.setRnPrefix(null);
			document.setRnPored(null);			
		}
	   clearDeloDocLink();	 
	   if(!SearchUtils.isEmpty(document.getRnDoc()) || !rnEQ) {
		   selectedDelo =  searchRnDelo( document.getRnDoc(),  "mDeloS",  rnEQ, new Date());
	   }
	}
   
   /**
    * Премахва отлагането на документа в преписката - при ръчно изписване на номера на документа
    */
    public void clearDeloDocLink() {
 	  selectedDelo = null;
 	  document.setPoredDelo(null);
 	  deloDocPrep = new DeloDoc();
 	  deloDocPrep.setDelo(new Delo());
    }
	   /**
	    * Търси преписка по номер - ръчно въвеждане на номера - бутон "Търси"
	    */
	   public void actionSearchRnDeloBtn() {
		   if(selectedDelo == null) { // ако е намерена вече преписка да не се пуска пак търсенето
			   actionSearchRnDelo(false);
		    }
	   }
	   /**
	    * Търсене на преписка по номер
	    * @param rnDelo
	    * @param varModal
	    * @param rnEQ
	    * @param nastr
	    * @param inpDate
	    * @return
	    */
	   private Object[] searchRnDelo(String rnDelo, String varModal, boolean rnEQ, Date inpDate) {
		   Object[] sDelo = null;
		   rnDelo  =  SearchUtils.trimToNULL_Upper(rnDelo);
	       DeloSearch  tmp = new DeloSearch(document.getRegistraturaId());
	       tmp.setUseDost(false); // да не се ограничава достъпа!! За сега
		   tmp.setRnDelo(rnDelo);
		   tmp.setRnDeloEQ(rnEQ);
		   tmp.buildQueryComp(getUserData());
		
		   LazyDataModelSQL2Array lazy =   new LazyDataModelSQL2Array(tmp, "a1 desc");
		   if(lazy.getRowCount() == 0 && rnEQ) {
			  
			   clearDeloDocLink(); 
			   //	LOGGER.debug("Не е намерена преписка с посочения номер!");
			   
		   } else if(lazy.getRowCount() == 1 && rnEQ) { // само при пълно съвпадение на номера
			   
			   List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);
			   sDelo = new Object[8];
			   if(result != null) {
				    sDelo =  result.get(0);		
				   	loadDataFromDeloS(sDelo, inpDate); // документ - в преписка
				   
			   }	
			   
				LOGGER.debug("Намерена е само една преписка с този рег. номер - данните да се заредят без да излиза модалния");
			   
		   } else {
			   sDelo = new Object[8];		
			   String  cmdStr = "PF('"+varModal+"').show();";
			   PrimeFaces.current().executeScript(cmdStr);
		   }
		  return sDelo;		  
	   }
	   
	   
	   /**
	    * Зарежда данните за избраната преписка 
	    * nastr = true - ръчно въвеждане на номера на документа
	    * 0]-DELO_ID<br>
		* [1]-RN_DELO<br>
		* [2]-DELO_DATE<br>
		* [3]-STATUS<br>
		* [4]-DELO_NAME<br>
		* [5]-INIT_DOC_ID<br>
		* [6]-REGISTER_ID<br>
	    */
	   private void loadDataFromDeloS(Object[] sDelo, Date inpDate) {	  
		    deloDocPrep = new DeloDoc();
		    deloDocPrep.setDelo(new Delo());	    
			if(sDelo[0] != null) {
				deloDocPrep.setDeloId(Integer.valueOf(sDelo[0].toString())); 
			}
			
			Date datd = (Date)sDelo[2];
			if(datd != null ){
				deloDocPrep.getDelo().setDeloDate(datd);
			}
			
			if(inpDate == null) {
				inpDate = new Date();
			}
			deloDocPrep.setInputDate(inpDate);
			deloDocPrep.getDelo().setStatus(Integer.valueOf(sDelo[3].toString()));
			
			String tmpstr = (String)sDelo[1];
			deloDocPrep.getDelo().setRnDelo(tmpstr);	
			

			deloDocPrep.setTomNomer(1);  // по подразбиране
			deloDocPrep.setEkzNomer(1);  //по подразбиране- 1-ви екз. ; раздела се зарежда при записа - за входящи - офицаилен, за собств. и раб. - вътршен
			
		
			// извиква се през полета за ръчно въвеждане на номер на документ
			 document.setRnDoc(tmpstr);
			// иницииращ документ- регистър 	
			
			if(sDelo[6] != null ){
				Integer initDocReg = Integer.valueOf(sDelo[6].toString());
				if( getUserData().hasAccess(OmbConstants.CODE_CLASSIF_REGISTRI, initDocReg)) {
					//само, ако има право да въвежда в този регистър
					document.setRegisterId(initDocReg); // винаги сменям регисъра, ако е върнат....
					actionChangeRegister();// да извлека настройките на регистъра
				}
			}	
		
			try {
				String msg =  deloDocPrep.getDelo().getRnDelo() +" / "+ DateUtils.printDate(deloDocPrep.getDelo().getDeloDate()) +
								"; "+  getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_DELO_STATUS, deloDocPrep.getDelo().getStatus(), getUserData().getCurrentLang(), new Date());
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "docu.addDeloMsg1", msg) );
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при разкодиране на статус на преписка ! ", e);
			}
			
			// ако е от минала година???? Съобщение? -   настройка на регистратура
		}
	   
	   
		/**
		 * Затваряне на модалния за избор на преписка - ръчно въвеждане на номера
		 */
	   public void actionHideModalDelo() {
		   if(selectedDelo != null && selectedDelo[0] != null) {
			   // да заредя полетата
			   loadDataFromDeloS(selectedDelo,  null); // ръчно въвеждане на рег. номер
			
		   } else {
			   selectedDelo = null;
		   }
	   }
	   
	   
	
	
	
	public static Logger getLogger() {
		return LOGGER;
	}

	public Doc getDocument() {
		return document;
	}

	public void setDocument(Doc document) {
		this.document = document;
	}

	public DocDAO getDocDao() {
		return docDao;
	}

	public void setDocDao(DocDAO docDao) {
		this.docDao = docDao;
	}

	public UserData getUd() {
		return ud;
	}

	public void setUd(UserData ud) {
		this.ud = ud;
	}

	public SystemData getSd() {
		return sd;
	}

	public void setSd(SystemData sd) {
		this.sd = sd;
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

	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}

	public String getOldRn() {
		return oldRn;
	}

	public void setOldRn(String oldRn) {
		this.oldRn = oldRn;
	}

	public Date getOldDate() {
		return oldDate;
	}

	public void setOldDate(Date oldDate) {
		this.oldDate = oldDate;
	}
	public DeloDoc getDeloDocPrep() {
		return deloDocPrep;
	}

	public void setDeloDocPrep(DeloDoc deloDocPrep) {
		this.deloDocPrep = deloDocPrep;
	}

	public Object[] getSelectedDelo() {
		return selectedDelo;
	}

	public void setSelectedDelo(Object[] selectedDelo) {
		this.selectedDelo = selectedDelo;
	}

	public Integer getOldPored() {
		return oldPored;
	}

	public void setOldPored(Integer oldPored) {
		this.oldPored = oldPored;
	}

	public boolean isNastrZadnaData() {
		return nastrZadnaData;
	}

	public void setNastrZadnaData(boolean nastrZadnaData) {
		this.nastrZadnaData = nastrZadnaData;
	}
}

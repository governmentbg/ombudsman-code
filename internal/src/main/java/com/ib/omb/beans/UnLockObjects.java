package com.ib.omb.beans;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.db.dto.AdmUser;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.system.OmbConstants;
import com.ib.system.SysConstants;
import com.ib.system.db.DialectConstructor;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;


/**
 * За отключване на обекти. 
 *
 * @author LM
 */

@Named
@ViewScoped
public class UnLockObjects  extends IndexUIbean   {

	/**
	 * Въвеждане и корекция на деловодни документи
	 */
	private static final long serialVersionUID = 507326780295416575L;
	private static final Logger LOGGER = LoggerFactory.getLogger(UnLockObjects.class);

	private Date decodeDate;
	private Integer periodR;
//	private Integer periodRes; //период на дата на получаване
//	private Integer periodAnswer;//период на дата на очакване на отговор
//	private Integer periodAssign;//период на дата на поставяне
	
	private List<SelectItem>	  docTypeList;
	private List<SystemClassif> docsVidClassif;  // заради autocomple (selectManyModalA)
	private List<SystemClassif> docsRegistriClassif;
	
    private List<Integer> usersList = new ArrayList<>();
    private List<SystemClassif> usersListClassifs = new ArrayList<>();
    private List<String> usersImena = new ArrayList<>();
    private transient AdmUserDAO userDao;
	
	private Integer	refType=getCodeDocument();
	private List<SelectItem>	 obectList;	
	private transient List<Object[]> lockList;
	private transient List<Object[]> rowSelectedTmp = new ArrayList<>();
    private List<LockObjects>  lockObjectsList;
    private List<LockObjects>  lockObjectsListAll=new ArrayList<>();
	private transient List<Object[]> rowSelected = new ArrayList<>();
    private List<LockObjects>  rowSelectedN=new ArrayList<>();
    private String msgBox;
    private String msgBox1;
    private String rnDoc;
	private Integer period;	
	private Date dateFrom;
	private Date dateTo;  
 
	public static final String BEANMSG = "beanMessages";
    public static final String NEWLINE = System.lineSeparator();   
    public static final String FROMLOCKOBJLO = " FROM LOCK_OBJECTS lo ";   
    public static final String TIPOBJIDLOCKINF = " lo.OBJECT_TIP objectTip, lo.OBJECT_ID  objectId, lo.LOCK_INFO  lockInfo  ";   
    public static final String OBJTYPE = "objType";   

	
    public Integer getCodeClassifUser() {
        return SysConstants.CODE_CLASSIF_USERS;
    }
    /** Код на класификация "Тип преписка" */
    public Integer getCodePrepiska() {
        return OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO;
    }
    /** Задача " */
    public Integer getCodeZadacha() {
        return OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK;
    }
   
    /** Код на класификация "Тип документ" */
    public Integer getCodeDocument() {
        return OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC;
    }

    /** Код на класификация "Административна структура" */
    public Integer getCodeAdmStructura() {
        return OmbConstants.CODE_ZNACHENIE_MENU_ADM_STRUCT;
    }
    
    /** Пощенска кутия" */
    public Integer getCodeMail() {
        return OmbConstants.CODE_ZNACHENIE_JOURNAL_MAILBOX;
    }
    
    /** Код на класификация "Изпълнение на процедура" */
    public Integer getCodeProcExe() {
        return OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE;
    }

    
    /** Код на класификация "СЕОС/ССЕВ съобщение" */
    public Integer getCodeEgovMsg() {
        return OmbConstants.CODE_ZNACHENIE_JOURNAL_EGOVMESSAGE;
    }

    
	@SuppressWarnings("unchecked")
	/** */
	@PostConstruct
	void initData() {
		
		obectList=new ArrayList<>();
		obectList.add(new SelectItem(getCodeDocument(),getMessageResourceString(LABELS, "docu.document")));
		obectList.add(new SelectItem(getCodePrepiska(),getMessageResourceString(LABELS, "general.delo")));
		obectList.add(new SelectItem(getCodeZadacha(),getMessageResourceString(LABELS, "docu.taskH1")));
		obectList.add(new SelectItem(getCodeAdmStructura(),getMessageResourceString(LABELS, "admStruct.admStruct")));
		obectList.add(new SelectItem(getCodeMail(),getMessageResourceString(LABELS, "unLockObjects.inputMail")));
		obectList.add(new SelectItem(getCodeEgovMsg(),getMessageResourceString(LABELS, "registratura.seosSsev")));
		//obectList.add(new SelectItem(getCodeProcExe(),getMessageResourceString(LABELS, "docu.procedura")));
		
		this.userDao = new AdmUserDAO(getUserData());
		String qq = "select  count( * )  FROM LOCK_OBJECTS "	;
		Query query = createNativeQuery(qq);
		Integer cc = SearchUtils.asInteger(query.getSingleResult());
		if (cc == 0) {
			setMsgBox(getMessageResourceString(BEANMSG, "unLockObject.noLock"));
			PrimeFaces current = PrimeFaces.current();
			current.executeScript("PF('dlg2').show();");
		}
	}

	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionUnlock() {
		if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_MAILBOX) {
			try {
			
				JPA.getUtil().begin();
		
					String qq="DELETE "
							+ FROMLOCKOBJLO
							+ " WHERE lo.OBJECT_TIP=:objType ";
					Query query = JPA.getUtil().getEntityManager().createNativeQuery(qq);
					query.setParameter(OBJTYPE, OmbConstants.CODE_ZNACHENIE_JOURNAL_MAILBOX);
					query.executeUpdate();
					JPA.getUtil().commit();
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.succesSaveMsg") );
					
					
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
				JPA.getUtil().rollback();
			}finally {
				JPA.getUtil().closeConnection();
			}
			PrimeFaces current = PrimeFaces.current();
			current.executeScript("PF('dlg3').hide();$('#form').trigger('reset');");
			return;
			
		}else {
			if (lockObjectsListAll.isEmpty()) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(BEANMSG, "unLockObject.noSelected"));					
				return;
			}
			Long objType = null;
			Long obectId = null;
			try {
				JPA.getUtil().begin();
					for ( LockObjects selectedRow : lockObjectsListAll) {
						obectId= Long.parseLong(selectedRow.getObjectId().toString());
						objType = Long.parseLong(selectedRow.getObjectTip().toString());
						lockList = null;
						String qq="DELETE "
								+ FROMLOCKOBJLO
								+ " WHERE lo.OBJECT_ID=:obectId AND lo.OBJECT_TIP=:objType ";
		
						Query query = JPA.getUtil().getEntityManager().createNativeQuery(qq);
						query.setParameter(OBJTYPE, objType).setParameter("obectId", obectId);
						query.executeUpdate();
					}
					JPA.getUtil().commit();
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(BEANMSG, "unLockObject.unLocked"));					
					
					
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,  getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
				JPA.getUtil().rollback();
			}finally {
				JPA.getUtil().closeConnection();
			}
			actionSearch();
					
		}
	}


	/**
	 * премахва избраните критерии за търсене
	 */
	public void actionClear() {
		
		clearFilter();
		changePeriod();
		
		lockObjectsListAll = new ArrayList<>();
		rowSelectedTmp = new ArrayList<>();
	}
	
	public void clearFilter() {
//		periodR 			= null;
//		periodRes 			= null;
//		periodAnswer 		= null;
//		periodAssign 		= null;
//		docsRegistriClassif = null;
//		docsVidClassif 		= null;
//		usersImena			= null;
		refType				= getCodeDocument();
		lockObjectsList		= null;
		usersList			= new ArrayList<>();
		rnDoc				="";
		period				= null;		
		usersList = new ArrayList<>();
		usersListClassifs = new ArrayList<>();
	}
	
	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	public void changePeriod() {
		
    	if (this.period != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.period);
			setDateFrom(di[0]);
			setDateTo(di[1]);				
    	} else {
    		setDateFrom(null);
    		setDateTo(null);
		}
    }
	
	
	/** Метод за смяна на датите при избор на период за търсене.
	 * 
	 * 
	 */
	
	public void changeDate() { 
		this.setPeriod(null);
	}
	
//	public void changeDateRes() { 
//		this.setPeriodRes(null);
//	}
//	
//	
//	public void changeDateAnswer() { 
//		this.setPeriodAnswer(null);
//	}
//
//	
//	public void changeDateAssign() { 
//		this.setPeriodAssign(null);
//	}
	
	
	/** 
	 * Списък с документи по зададени критерии 
	 */
	@SuppressWarnings("unchecked")
	public void actionSearch(){
		String qq="";
		lockObjectsList = new ArrayList<>();
		lockObjectsListAll = new ArrayList<>();
		rowSelectedTmp = new ArrayList<>();

		SimpleDateFormat sdtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");					
		String strDatBeg = "";
		String strDatEnd = "";
		lockList = null;
		
		String dialect = JPA.getUtil().getDbVendorName();
		
		Query query;
		if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_MAILBOX) {
//			qq="select  lo.LOCK_DATE dataZakl, lo.USER_ID userZakl, "
//					+ TIPOBJIDLOCKINF
//					+ FROMLOCKOBJLO
//					+ " WHERE OBJECT_TIP=:objType ";
//			query = createNativeQuery(qq).setParameter(OBJTYPE, refType);
//			lockList = query.getResultList();
//			if(lockList.isEmpty()){
			
			qq="select  count( * )   "
					+ FROMLOCKOBJLO
					+ " WHERE OBJECT_TIP=:objType ";
			query = createNativeQuery(qq).setParameter(OBJTYPE, refType);
			Integer cc = SearchUtils.asInteger(query.getSingleResult());
			if (cc == 0) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(BEANMSG, "unLockObject.noLocked"));					
				//return;
			}else {			
				setMsgBox("Има заключени "+cc+" съобщение във входящата поща.");//lockList.size()	
				setMsgBox1(" Моля, потвърдете отключването!");
				
				PrimeFaces current = PrimeFaces.current();
				current.executeScript("PF('dlg3').show();");				
			}
			lockObjectsList.clear();
			return;
		} else if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK) {
			qq="select  t.RN_TASK rnDoc,t.ASSIGN_DATE dataVazl,t.TASK_TYPE zadTip , lo.LOCK_DATE dataZakl, lo.USER_ID userZakl, "
					+ TIPOBJIDLOCKINF
					+ " FROM LOCK_OBJECTS lo, task t "
					+ " WHERE lo.object_ID=t.task_ID AND OBJECT_TIP=:objType ";
			if (rnDoc!=null && !"".equals(rnDoc)) {
				qq+=" and UPPER(d.RN_DOC) like '%"+rnDoc.toUpperCase()+"%' ";
			}
			
		} else if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC) {
			qq="select  "+DocDAO.formRnDocSelect("d.", dialect)+" rnDoc,d.DOC_DATE dataDok, "+DialectConstructor.limitBigString(dialect, "d.OTNOSNO", 300)+"    otnosno , lo.LOCK_DATE dataZakl, lo.USER_ID userZakl, "
					+ " lo.OBJECT_TIP objectTip, lo.OBJECT_ID  objectId, d.DOC_TYPE    dokTip  "
					+ " FROM LOCK_OBJECTS lo, doc d "
					+ " WHERE lo.object_ID=d.doc_ID AND OBJECT_TIP=:objType ";
			if (rnDoc!=null && !"".equals(rnDoc)) {
				qq+=" and UPPER(d.RN_DOC) like '%"+rnDoc.toUpperCase()+"%' ";
			}
		} else if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO) {
			qq="select  d.RN_DELO rnDelo,d.DELO_DATE  dataDel, "+DialectConstructor.limitBigString(dialect, "d.DELO_NAME", 300)+" naimen , lo.LOCK_DATE dataZakl, lo.USER_ID userZakl, "
					+ " lo.OBJECT_TIP objectTip, lo.OBJECT_ID  objectId, d.DELO_TYPE         deloTi  "
					+ " FROM LOCK_OBJECTS lo, DELO d "
					+ " WHERE lo.object_ID=d.DELO_ID AND OBJECT_TIP=:objType ";
			if (rnDoc!=null && !"".equals(rnDoc)) {
				qq+=" and UPPER(d.RN_DELO) like '%"+rnDoc.toUpperCase()+"%' ";
			}
			
		} else 	if (refType==OmbConstants.CODE_ZNACHENIE_MENU_ADM_STRUCT) {
			qq="select lo.LOCK_DATE dataZakl, lo.USER_ID userZakl, "
					+ TIPOBJIDLOCKINF
					+ FROMLOCKOBJLO
					+ " WHERE  OBJECT_TIP=:objType ";
		}else	if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_EGOVMESSAGE) {
			qq="select  lo.LOCK_DATE dataZakl, lo.USER_ID userZakl, "
					+ TIPOBJIDLOCKINF
					+ " ,em.MSG_GUID    msgGuid, em.SENDER_NAME sName"
					+ " FROM LOCK_OBJECTS lo ,EGOV_MESSAGES em"
					+ " WHERE em.ID=lo.object_ID AND OBJECT_TIP=:objType ";
		} else	if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE) {
			qq="select  lo.LOCK_DATE dataZakl, lo.USER_ID userZakl, "
					+ "pe.PROC_NAME procName,r.REGISTRATURA  ReId, "
					+ TIPOBJIDLOCKINF
					+ " FROM LOCK_OBJECTS lo ,PROC_EXE pe,REGISTRATURI r"
					+ " WHERE pe.EXE_ID=lo.object_ID  and r.REGISTRATURA_ID=Pe.REGISTRATURA_ID AND OBJECT_TIP=:objType ";
		}

		
		String uId="";
		if (!usersList.isEmpty()) {
			for (Integer tt : usersList) {
				uId+=tt+",";
			}
			uId=uId.substring(0, uId.length()-1);
			qq+=" and lo.USER_ID in ("+uId+")";
		}
		
		if(dateFrom!=null) {
			dateFrom = DateUtils.startDate(dateFrom);
			strDatBeg = " TO_DATE('" + sdtf.format(dateFrom) + "','yyyy-mm-dd HH24:MI:SS')";
			qq+=" and lo.LOCK_DATE >= "+ strDatBeg;
		}
		
		if(dateTo!=null) {
			dateTo = DateUtils.endDateChangeNull(dateTo);
			strDatEnd = " TO_DATE('" + sdtf.format(dateTo) + "','yyyy-mm-dd HH24:MI:SS')";
			qq+=" and lo.LOCK_DATE <=  "+ strDatEnd;
			
		}
		query = createNativeQuery(qq).setParameter(OBJTYPE, refType);
		lockList = query.getResultList();
		
		lockObjectsList = new ArrayList<>();
		if (lockList.isEmpty()) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(BEANMSG, "unLockObject.noLocked"));					
			return;
		}
		for (int i = 0; i < lockList.size(); i++) {
			
			Long objectId = null;
			Long objectTip = null;
			Long userId = null;
			Date lockDate = null;
			String lockInfo = null;
			String rnDoc = null;
			Date dateVazl = null;
			Long zadTip = null;
			String senderName= null;
			LockObjects tmp=new LockObjects(objectId, objectTip, userId, lockDate, lockInfo, rnDoc, dateVazl, zadTip, senderName);
			Object[] op = lockList.get(i);

			if (refType==OmbConstants.CODE_ZNACHENIE_MENU_ADM_STRUCT) {
				tmp.setObjectId(Long.parseLong(op[3].toString()));
				tmp.setObjectTip(Long.parseLong(op[2].toString()));
				tmp.setUserId(Long.parseLong(op[1].toString()));
				if (op[0] instanceof java.util.Date) {
					tmp.setLockDate((java.util.Date)op[0]);
			    }
				tmp.setLockInfo((String) op[4]);
			}
			if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_EGOVMESSAGE) {
				tmp.setObjectId(Long.parseLong(op[3].toString()));
				tmp.setObjectTip(Long.parseLong(op[2].toString()));
				tmp.setUserId(Long.parseLong(op[1].toString()));
				if (op[0] instanceof java.util.Date) {
					tmp.setLockDate((java.util.Date)op[0]);
			    }
				tmp.setLockInfo((String) op[4]);
				tmp.setRnDoc((String) op[5]);
				tmp.setSenderName((String) op[6]);
			}			
			if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK) {
				tmp.setRnDoc((String) op[0]);
				if (op[1] instanceof java.util.Date) {
					tmp.setDateVazl((java.util.Date)op[1]);
			    }
				tmp.setZadTip( Long.parseLong(op[2].toString()));
				if (op[3] instanceof java.util.Date) {
					tmp.setLockDate((java.util.Date)op[3]);
			    }
				tmp.setUserId(Long.parseLong(op[4].toString()));
				tmp.setObjectTip(Long.parseLong(op[5].toString()));
				tmp.setObjectId(Long.parseLong(op[6].toString()));
				tmp.setLockInfo((String) op[7]);
			}
			if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC) {
				tmp.setRnDoc((String) op[0]);
				if (op[1] instanceof java.util.Date) {
					tmp.setDateVazl((java.util.Date)op[1]);
			    }
				tmp.setLockInfo((String) op[2]);
				if (op[3] instanceof java.util.Date) {
					tmp.setLockDate((java.util.Date)op[3]);
			    }
				tmp.setUserId(Long.parseLong(op[4].toString()));
				tmp.setObjectTip(Long.parseLong(op[5].toString()));
				tmp.setObjectId(Long.parseLong(op[6].toString()));
				tmp.setZadTip(Long.parseLong(op[7].toString()));
			}
			if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO) {
				tmp.setRnDoc((String) op[0]);
				if (op[1] instanceof java.util.Date) {
					tmp.setDateVazl((java.util.Date)op[1]);
			    }
				tmp.setLockInfo((String) op[2]);
				if (op[3] instanceof java.util.Date) {
					tmp.setLockDate((java.util.Date)op[3]);
			    }
				tmp.setUserId(Long.parseLong(op[4].toString()));
				tmp.setObjectTip(Long.parseLong(op[5].toString()));
				tmp.setObjectId(Long.parseLong(op[6].toString()));
				tmp.setZadTip(Long.parseLong(op[7].toString()));
			}
			if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_PROC_EXE) {
				tmp.setRnDoc(op[5].toString());
				tmp.setLockInfo((String) op[2]);
				if (op[0] instanceof java.util.Date) {
					tmp.setLockDate((java.util.Date)op[0]);
			    }
				tmp.setUserId(Long.parseLong(op[1].toString()));
				tmp.setObjectTip(Long.parseLong(op[4].toString()));
				tmp.setObjectId(Long.parseLong(op[5].toString()));
//				tmp.setZadTip(Long.parseLong(op[3].toString()));
				tmp.setSenderName((String) op[3]);

			}
			
			
//			if (refType==OmbConstants.CODE_ZNACHENIE_JOURNAL_MAILBOX) {
//				lockObjectsList.clear();
//			}
			
			lockObjectsList.add(tmp);
		}
	}	
	
	
	
	/**
	 * Създава {@link Query} по подадения String
	 *
	 * @param queryString
	 * @return Query
	 */
	public final Query createNativeQuery(String queryString) {
		return getEntityManager().createNativeQuery(queryString);
	}
	/**
	 * @return entityManager instance
	 */
	protected final EntityManager getEntityManager() {
		return JPA.getUtil(this.unitName).getEntityManager();
	}
	/**
	 * Ако по някакъв начин е зададено друго име, то тогава даото ще работи с него в методите за обръщение към БД. Иначе ще си
	 * взима дефолтното, което е описана в JPA-то.
	 */
	private String unitName;

	/**
	 * @return the unitName
	 */
	protected final String getUnitName() {
		return this.unitName;
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

	public List<SelectItem> getDocTypeList() {
		return docTypeList;
	}

	public void setDocTypeList(List<SelectItem> docTypeList) {
		this.docTypeList = docTypeList;
	}

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

//	public Integer getPeriodRes() {
//		return periodRes;
//	}
//
//	public void setPeriodRes(Integer periodRes) {
//		this.periodRes = periodRes;
//	}
//
//	public Integer getPeriodAnswer() {
//		return periodAnswer;
//	}
//
//
//	public void setPeriodAnswer(Integer periodAnswer) {
//		this.periodAnswer = periodAnswer;
//	}
//
//
//	public Integer getPeriodAssign() {
//		return periodAssign;
//	}
//
//	public void setPeriodAssign(Integer periodAssign) {
//		this.periodAssign = periodAssign;
//	}
    public List<Integer> getUsersList() {
        return usersList;
    }

    public void setUsersList(List<Integer> usersList) {

        this.usersImena = new ArrayList<>();

        if (!usersList.isEmpty()) {

            try {

                for (Integer user : usersList) {

                    AdmUser userTmp = this.userDao.findById(user);
                    usersImena.add(userTmp.getNames());
                }

            } catch (DbErrorException e) {
                LOGGER.error("Грешка при търсене на участник в групата! ", e);
                JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"));
            }
        }

        this.usersList = usersList;
    }

    public List<String> getUsersImena() {
        return usersImena;
    }

    public void setUsersImena(List<String> usersImena) {
        this.usersImena = usersImena;
    }

	public Integer getRefType() {
		return refType;
	}

	public void setRefType(Integer refType) {
		this.refType = refType;
	}
	public List<SelectItem> getObectList() {
		return obectList;
	}
	public void setObectList(List<SelectItem> obectList) {
		this.obectList = obectList;
	}
	public void actionDeleteUsersList() {

        this.usersList.clear();
        this.usersImena.clear();
    }
	   public void actionDeleteUser() {
	        String selectedName = JSFUtils.getRequestParameter("selectedName");
	        try {
	            for (Integer user : this.usersList) {
	                AdmUser userTmp = this.userDao.findById(user);
	                if (userTmp.getNames().equals(selectedName)) {
	                    this.usersImena.remove(selectedName);
	                    this.usersList.remove(user);
	                    break;
	                }
	            }
	        } catch (DbErrorException e) {
	            LOGGER.error("Грешка при изтриване на участник в групата! ", e);
	            JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"));
	        }
	    }
	public List<Object[]> getLockList() {
		return lockList;
	}
	public void setLockList(List<Object[]> lockList) {
		this.lockList = lockList;
	}
	
	/*
	 * Множествен избор на задачи
	 */
	/**
	 * Избира всички редове от текущата страница
	 * @param event
	 */
	  public void onRowSelectAll(ToggleSelectEvent event) {  
    	List<LockObjects> tmpL = new ArrayList<>();
    	tmpL.addAll(getLockObjectsListAll());
    	if(event.isSelected()) {
    		for (LockObjects obj : rowSelectedN) {
    			boolean bb = true;
				Integer l1 = Integer.valueOf(obj.getObjectId().toString());        			
				Integer l3 = Integer.valueOf(obj.getObjectTip().toString());        			
				for (LockObjects j : tmpL) { 
		    		Integer l2 = Integer.valueOf(j.getObjectId().toString());
		    		Integer l4 = Integer.valueOf(j.getObjectTip().toString());
		    		if(l1.equals(l2)&&l3.equals(l4)) {
		    			bb = false;
		    			break;
		    		}
		   		}
    			if(bb) {
    				tmpL.add(obj);
    			}
    		}    		
    	}else {
		}
    	setLockObjectsListAll(tmpL);
		LOGGER.debug("onToggleSelect->>");	    	   
	}
		@SuppressWarnings("rawtypes")
		public void rowSelectCheckbox(SelectEvent event) {
	    //	LockObjects u = (LockObjects) event.getObject();
	     	if(event!=null  && event.getObject()!=null) {
	     		List<LockObjects> tmpList = getLockObjectsListAll();
	    		
	    		LockObjects obj = (LockObjects) event.getObject();
	    		boolean bb = true;
	    		Integer l2 = Integer.valueOf(obj.getObjectId().toString());
	    		Integer l4 = Integer.valueOf(obj.getObjectTip().toString());
				for (LockObjects j : tmpList) { 
					Integer l1 = Integer.valueOf(j.getObjectId().toString());        			
					Integer l3 = Integer.valueOf(j.getObjectTip().toString());        			
		    		if(l1.equals(l2)&&l3.equals(l4)) {
		    			bb = false;
		    			break;
		    		}
		   		}
				if(bb) {
					tmpList.add(obj);
					setLockObjectsListAll(tmpList);
				}
	    	}	    	
		}
		@SuppressWarnings("rawtypes")
		public void rowUnselectCheckbox(UnselectEvent event) {
	     		List<LockObjects> tmpList = getLockObjectsListAll();
	     		
	    		LockObjects obj = (LockObjects) event.getObject();
	    		boolean bb = false;
	    		Integer l2 = Integer.valueOf(obj.getObjectId().toString());
	    		Integer l4 = Integer.valueOf(obj.getObjectTip().toString());
				for (LockObjects j : tmpList) { 
					Integer l1 = Integer.valueOf(j.getObjectId().toString());        			
					Integer l3 = Integer.valueOf(j.getObjectTip().toString());        			
		    		if(l1.equals(l2)&&l3.equals(l4)) {
		    			bb = true;
		    			break;
		    		}
		   		}
				if(bb) {
					tmpList.remove(obj);
					setLockObjectsListAll(tmpList);
				}
			
		}
		    
    /** 
     * Select one row
     * @param event
     */
    @SuppressWarnings("rawtypes")
	public void onRowSelect(SelectEvent event) { 
     	if(event!=null  && event.getObject()!=null) {
    		List<Object[]> tmpList =  getRowSelectedTmp();
    		
    		Object[] obj = (Object[]) event.getObject();
    		boolean bb = true;
    		Integer l2 = Integer.valueOf(obj[0].toString());
			for (Object[] j : tmpList) { 
				Integer l1 = Integer.valueOf(j[0].toString());        			
	    		if(l1.equals(l2)) {
	    			bb = false;
	    			break;
	    		}
	   		}
			if(bb) {
				tmpList.add(obj);
				setRowSelectedTmp(tmpList);   
			}
    	}	    	
    }
    
    /**
     * unselect one row
     * @param event
     */
 		    
 
  
//    public void onRowUnselect(SelectEvent<LockObjects> event) {
//    	SelectEvent<LockObjects> a = event;
//    	System.out.println("onRowUnselect");
////        FacesMessage msg = new FacesMessage("Car Unselected", event.getObject().getId());
////        FacesContext.getCurrentInstance().addMessage(null, msg);
//    }
	public void showModalSnemane() {
//		System.out.println("showModalSnemane");
		
	}
	
	public List<LockObjects> getLockObjectsList() {
		return lockObjectsList;
	}
	public void setLockObjectsList(List<LockObjects> lockObjectsList) {
		this.lockObjectsList = lockObjectsList;
	}
	public List<Object[]> getRowSelected() {
		return rowSelected;
	}
	public void setRowSelected(List<Object[]> rowSelected) {
		this.rowSelected = rowSelected;
	}
	public String getMsgBox() {
		return msgBox;
	}
	public void setMsgBox(String msgBox) {
		this.msgBox = msgBox;
	}
	public class LockObjects implements Serializable{
    	/**
		 * 
		 */

		public Long   objectTip;
		public Long   userId;
    	public Date   lockDate;
    	public String lockInfo;
    	public String rnDoc;
    	public Date   dateVazl;
		public Long   zadTip;
	   	public String senderName;
   	
    	

		public LockObjects(Long objectId, Long objectTip, Long userId, Date lockDate,String lockInfo,
				String rnDoc,Date dateVazl, Long zadTip,String senderName) {
			this.objectId	= objectId;
			this.objectTip  = objectTip;
			this.userId		= userId;
			this.lockDate	= lockDate;
			this.rnDoc		= rnDoc;
			this.dateVazl	= dateVazl;
			this.zadTip		= zadTip;
			this.senderName		= senderName;
			
		}


		private static final long serialVersionUID = 1L;
		public Long   objectId;
    	public Long getObjectId() {
			return objectId;
		}

		public void setObjectId(Long objectId) {
			this.objectId = objectId;
		}

		public Long getObjectTip() {
			return objectTip;
		}

		public void setObjectTip(Long objectTip) {
			this.objectTip = objectTip;
		}

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Date getLockDate() {
			return lockDate;
		}

		public void setLockDate(Date lockDate) {
			this.lockDate = lockDate;
		}

		public String getLockInfo() {
			return lockInfo;
		}

		public void setLockInfo(String lockInfo) {
			this.lockInfo = lockInfo;
		}

		public String getRnDoc() {
			return rnDoc;
		}

		public void setRnDoc(String rnDoc) {
			this.rnDoc = rnDoc;
		}

		public Date getDateVazl() {
			return dateVazl;
		}

		public void setDateVazl(Date dateVazl) {
			this.dateVazl = dateVazl;
		}

		public Long getZadTip() {
			return zadTip;
		}

		public void setZadTip(Long zadTip) {
			this.zadTip = zadTip;
		}
		public String getSenderName() {
			return senderName;
		}

		public void setSenderName(String senderName) {
			this.senderName = senderName;
		}


	}
	public String getRnDoc() {
		return rnDoc;
	}
	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}
	public Integer getPeriod() {
		return period;
	}
	public void setPeriod(Integer period) {
		this.period = period;
	}
	public Date getDateFrom() {
		return dateFrom;
	}
	public void setDateFrom(Date dateFrom) {
		this.dateFrom = dateFrom;
	}
	public Date getDateTo() {
		return dateTo;
	}
	public void setDateTo(Date dateTo) {
		this.dateTo = dateTo;
	}
	public List<Object[]> getRowSelectedTmp() {
		return rowSelectedTmp;
	}
	public void setRowSelectedTmp(List<Object[]> rowSelectedTmp) {
		this.rowSelectedTmp = rowSelectedTmp;
	}
	public List<LockObjects> getLockObjectsListAll() {
		return lockObjectsListAll;
	}
	public void setLockObjectsListAll(List<LockObjects> lockObjectsListAll) {
		this.lockObjectsListAll = lockObjectsListAll;
	}
	public List<LockObjects> getRowSelectedN() {
		return rowSelectedN;
	}
	public void setRowSelectedN(List<LockObjects> rowSelectedN) {
		this.rowSelectedN = rowSelectedN;
	}
	public String getMsgBox1() {
		return msgBox1;
	}
	public void setMsgBox1(String msgBox1) {
		this.msgBox1 = msgBox1;
	}
	public List<SystemClassif> getUsersListClassifs() {
		return usersListClassifs;
	}
	public void setUsersListClassifs(List<SystemClassif> usersListClassifs) {
		this.usersListClassifs = usersListClassifs;
	}

    
}
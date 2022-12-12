package com.ib.omb.components;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.beans.DocData;
import com.ib.omb.beans.DocDataVrazki;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.LockObjectDAO;
import com.ib.omb.db.dao.TaskDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.Task;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.exceptions.UnexpectedResultException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.X;

/** */
@FacesComponent(value = "taskData", createTag = true)
public class TaskData extends UINamingContainer  {
	
	private enum PropertyKeys {
		 TASK, DOCREQ, BEZSROK, ONEEXEC, ONEEXECD, SCLIST, NEWSTATUS, STATUSLST, ROLE, SHOWME, 
		 VIEWPANELEXEC, VIEWDATEEXEC,  VIEWOPINION, OPINIONLST,   FILESLIST, EDITM, EDITE, 
		 DATEDOC,  HISTORYLIST, DEFEDITLIST, SEARCHFLAGDOCEND, DOCENDOBJ, 
		 PREPLIST,  DELOSEL, DELOSELTMP, LICALIST,  LICASEL, LISTTYPETASK, DEFUSERTASK, SROKPATTERN, SROKTIME, SROKDAYS, CODEEXTCHECK,
		 DEFSIGEND, EXECSIGNED, DEFSTRING, EXECSTRING, VIEWSIGN
	}
	
	
	


	private static final Logger LOGGER = LoggerFactory.getLogger(TaskData.class);
	public static final String	UI_BEANMESSAGES	= "ui_beanMessages";
	public static final String	BEANMESSAGES	= "beanMessages";
	public static final String  MSGPLSINS		= "general.pleaseInsert";
	public static final String  ERRDATABASEMSG 	= "general.errDataBaseMsg";
	public static final String  SUCCESSAVEMSG 	= "general.succesSaveMsg";
	public static final String  SIGNSAVEMSG 	= "general.signSaveMsg";
	public static final String  SIGNDELMSG 		= "general.signDelMsg";
	public static final String	LABELS			= "labels";
	
	private static final String ID_TASK_ATTR = "idTask";
	private static final String ID_DOC_ATTR = "idDoc";

	private TimeZone timeZone = TimeZone.getDefault();

	private String 		lockMsg		= null;



	private SystemData	systemData	= null;
	private UserData	userData	= null;
	private Integer		defaultStatus	= null;
	private String 		labelDateExec  = IndexUIbean.getMessageResourceString(LABELS,"tasks.dateExec");
	private Task tmpTask;	
	
	private static final Object[][] SPECADMOBJ = {{OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE, X.of(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL)},};  
	
	private Integer[] licaSelected;
	
	private  Doc docEdit = null;
	
	private Integer dopInfHelp;
	
	/**
	 * Въвеждане и актуализация на задачи
	 * @return
	 * @throws DbErrorException
	 */
	public void initTaskComp() {	
	    lockMsg = null;
		setDateDoc(null);
	
		Integer idTask = (Integer) getAttributes().get(ID_TASK_ATTR); 		
		Date dd = (Date) getAttributes().get("dateDoc") ;
		if( dd == null) {
			setDateDoc(new Date());
		}else {
			setDateDoc(dd); // идва от документа
		}
		try {
			 // Срок за изпълнение на задача по подразбиране (в дни)
			String param1 = getSystemData().getSettingsValue("delo.daysToCompleteTask");
			if(!SearchUtils.isEmpty(param1)) {
				setSrokDays(Integer.valueOf(param1));
			}

			if(idTask != null) {	
				loadTask(idTask); // зареждане на задача
				setTaskTypeList(loadISelectItemList()) ; // трябва да е след зареждането на задачата, защото може да не е подаден idDoc!!
			}else { // нова задача
				setTaskTypeList(loadISelectItemList()) ; // ако е нов документ, се налага най-напред да заредя списъка с допустимите видове задачи за потребителя, за да мога да сложа настройките по тип задача
				actionNewTask(false);
			}		
		
			Integer s1 = getSystemData().getRegistraturaSetting(getTask().getRegistraturaId(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_15);
			if(Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_DA)) {
				setSrokPattern("dd.MM.yyyy HH:mm");
				setSrokTime("true");
			}else {
				setSrokPattern("dd.MM.yyyy");
				setSrokTime("false");
			}
			
			//Изпълнител на задача може да бъде от друга регистратура
			s1 = getSystemData().getRegistraturaSetting(getUserData().getRegistratura(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_16);
			if(Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_NE)) {
				Object[] codeExt = new Object[] {OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA, getUserData().getRegistratura().toString(), IndexUIbean.getMessageResourceString(BEANMESSAGES,"task.msgCodeExt")};
				setCodeExtCheck(codeExt);
			}else {
				setCodeExtCheck(null);
			}
			// да се допуска ли подписване на метаданни
			param1 = getSystemData().getSettingsValue("delo.taskDefExeSign");  
			if(Objects.equals(param1,String.valueOf(OmbConstants.CODE_ZNACHENIE_DA))) {
				setViewSignBtn(true);
			}
			
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при извличане на настройки на регистратура! ", e);
		}
		setShowMe(true); 
	}
	
	/**
	 * Връща ид на деловоден документ, ако има такъв
	 * Може да липсва, само, ако извикването е от раб. плот, иначе трябва да е подадено! 
	 * @return
	 */
	private Integer idDocAttr() {
		Integer idDoc = (Integer) getAttributes().get(ID_DOC_ATTR);
	    if(idDoc == null && getTask() != null && getTask().getDocId() != null) {
	    	idDoc = getTask().getDocId(); 
	    }
		return idDoc;
	}
	
	
	/**
	 *  1. вид задача в зависимост от правата на потребителя и 	дали задачата е без документ;
	 *  2. зарежда задача по подразбиране 
	 * @return
	 */
	private  List<SelectItem>  loadISelectItemList() {
		
		List<SelectItem> lst = null; 		
		IndexUIbean ui = new IndexUIbean();	
		try {
			lst = ui.createItemsList(true, OmbConstants.CODE_CLASSIF_TASK_VID, new Date(), null); // филтрира по права на потребителите
			if(lst != null && !lst.isEmpty()) {
				 
				loadISelectItemList1(lst);  
				loadISelectItemList2(lst);  
				
			}
		} catch (DbErrorException | UnexpectedResultException e) {
			LOGGER.error("Грешка при зареждане на вид задача в зависимост от правата на потребителя! ", e);
		} 
		return lst;
	}
	
	/**
	 * За задача без документ -   
	 * да се махнат видовете задачи, които не могат да се избират, ако няма документ
	 * @param lst
	 * @throws DbErrorException
	 */
	private void loadISelectItemList1(List<SelectItem> lst) throws DbErrorException{
		@SuppressWarnings("unchecked")
		List<Long> docList = (List<Long>)  getAttributes().get("docList");
		Integer idDoc = idDocAttr();
		if(idDoc == null && docList==null) {
			int i = 0;
			do {
				SelectItem sc = lst.get(i);
				if(getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_TASK_WITHOUT_DOC, (Integer)sc.getValue(), new Date())) {
					i++;
				}else {
					lst.remove(i);
				}
			}while(i < lst.size());
		}
	}
	
	/**
	 * Да зареди задачата по подразбиране 
	 * @param lst
	 * @return
	 * @throws DbErrorException
	 */
	private void loadISelectItemList2(List<SelectItem> lst) throws DbErrorException{
		boolean bb = true;
		int i = 0;
		if(lst!=null && !lst.isEmpty()) {
			do {
				SelectItem sc = lst.get(i);
				if(Objects.equals(OmbConstants.CODE_ZNACHENIE_TASK_TYPE_DEFAULT, sc.getValue())){
					bb = false;
					break;
				}else {
					i++;
				}
			}while(i < lst.size());
			if(bb) {
				setDefUserTask((Integer)lst.get(0).getValue()); 
		}
		}
	}

	/**
	 *  Зачиства данните за задача - бутон "нов"
	 * @param unlock = true _ да извика отключването
	 */
    public void actionNewTask(boolean unlock) {
    	Integer idDoc = idDocAttr();
	    tmpTask = new Task();
		tmpTask.setAssignDate(new Date());
		tmpTask.setStatusDate(new Date());
		tmpTask.setStatus(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP); // неизпълнена 
		tmpTask.setDocId(idDoc);
		tmpTask.setRegistraturaId(getUserData().getRegistratura());
		tmpTask.setTaskType(getDefUserTask()); // за изпълнение code-1
		tmpTask.setDocRequired(SysConstants.CODE_ZNACHENIE_NE); // изисква ли се документ при приключване на задачата
		  
		tmpTask.setCodeAssign(getUserData().getUserSave());  //  за възложител по подразбиране да е текущия потребител или този делегирани права!!! !ако съм упълномощен от шефа, шефа се явява автор
		
		setTask(tmpTask);
		clearTaskFiled();
		actionChTypeTask(tmpTask.getTaskType());
		
		setDefSigned(false);
		setDefString(null);
		
		setExecSigned(false);
		setExecString(null);
	
		if(unlock) { 
			unlockTask();
		}
    }
    
    
    /**
     * Допълнителни полета, които трябва да се зачистят
     */
    private void clearTaskFiled() {
    	setDocReq(false);
    	setBezSrok(false);
    	setOneExec(false); // изпълнява се индивидуално
       	setOneExecD(false); // да забрани достъпа до чекбокса "изпълнява се индивидуално", само, ако е от типа задачи, за които е указано,че това е задължително
		setScList(new ArrayList<>()); // изпълнители
		setRole(null);
		setViewPanelExec(false);
		setViewOpinion(false);
		setViewDateExec(false);
		setStatusList(null);
		setOpinionLst(null);
		setNewStatus(null);
		setFilesListTsk(new ArrayList<>());
		setEditE(true); // true - разрешава корекция на осн. данни
		setEditM(true); // true - разрешава корекция на данни за приключване
		//setErrMsg(null);
    }
		

    
	/**
	 * Зарежда данни на зaдача по зададени критерии
	 * @param idTask
	 */
	private void loadTask(Integer idTask) {
		try {
			lockMsg = null;
			boolean chTsk = true;
			boolean isView = (Boolean) getAttributes().get("readonly"); // true - в режим на разглеждане
			
//			int tp = (Integer) getAttributes().get("taskProcess");
//			if(!isView && tp!=2 ) {// за сега ще изклюим заключване от работния плот
			if(!isView ) {// за сега ще включим заключване от работния плот
				// режим на актуализация
				// проверка за заключена задача 
				chTsk = checkForLock(idTask);
				if(chTsk) {
					lockTask(idTask);  
				}
			}
		
			if(chTsk) {
				
				loadTaskData(idTask);		// зарежда се tmpTask	
				
				clearTaskFiled();
				
				loadTaskRef();
				
				loadRole(tmpTask);

				if(SysConstants.CODE_ZNACHENIE_DA == tmpTask.getDocRequired().intValue()) {
					setDocReq(true);
				}
							
				FilesDAO daoF = new FilesDAO(getUserData());		
				JPA.getUtil().runWithClose(() -> setFilesListTsk(daoF.selectByFileObjectDop(tmpTask.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK))); // без допълнителни данни за файловете!
				
				
				setTask(tmpTask);	
				
				actionChTypeTask(tmpTask.getTaskType());
				if(tmpTask.getSrokDate() == null) {
					setBezSrok(true);
				}
				
				tmpTask = null;	
				
			}else {
				actionNewTask(false);
				unlockTask(); // ако преди това е имало заключена задача
			}
				
			
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на данни за задача! ", e);
		}
	}
	
	/**
	 * извлича данните за задачата от базата 
	 * @param idTask
	 * @throws BaseException
	 */
	private void loadTaskData(Integer idTask) throws BaseException {
		JPA.getUtil().runWithClose(() -> {
			tmpTask = new TaskDAO(getUserData()).findById(idTask);
			}
		);

		if(tmpTask != null && !SearchUtils.isEmpty(tmpTask.getSignDef())) {
			setDefSigned(true); // Дефиницията на задачата е подписана
			setDefString(tmpTask.getSignDef());
		}else {
			setDefSigned(false);
			setDefString(null);
		}
		if(tmpTask != null && !SearchUtils.isEmpty(tmpTask.getSignExe())) {
			setExecSigned(true); // изпълнението на задачата е подписано
			setExecString(tmpTask.getSignExe());
		}else {
			setExecSigned(false);
			setExecString(null);
		}
		
	}
	
	/**
	 * зарежда референтите в задачатa
	 */
	private void loadTaskRef(){
		List<SystemClassif> tmpLst = 	new ArrayList<>();				
		for( Integer item : tmpTask.getCodeExecs() ) {
			String tekst = "";
			SystemClassif scItem = new SystemClassif();
			try {
				scItem.setCodeClassif(Constants.CODE_CLASSIF_ADMIN_STR);
				scItem.setCode(item);
				tekst = getSystemData().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, item, getUserData().getCurrentLang(), getDateDoc());		
				scItem.setTekst(tekst);
				tmpLst.add(scItem);
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане на референти на задача! ", e);
			}		
		}				
		setScList(tmpLst); // изпълнители
	}
	
	
	
	/**
	 * Проверка за заключена задача  
	 * @param idObj
	 * @return
	 */
	private boolean checkForLock(Integer idObj) {
		boolean res = true;
		LockObjectDAO daoL = new LockObjectDAO();		
		try { 
			Object[] obj = daoL.check(getUserData().getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK, idObj);
			if (obj != null) {
				 res = false;
				 lockMsg = getSystemData().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, Integer.valueOf(obj[0].toString()), getUserData().getCurrentLang(), new Date())   
						       + " / " + DateUtils.printDateFull((Date)obj[1]);
			}
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при проверка за заключена задача ! ", e);
		}
		return res;
	}
	
	/**
	 * Заключване на задача, като преди това отключва само задачите, заключени от потребителя
	 * @param idObj
	 */
	private void lockTask(Integer idObj) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("lockData Task!" );
		}
		LockObjectDAO daoL = new LockObjectDAO();		
		try { 
			JPA.getUtil().runInTransaction(() -> 
				daoL.lock(getUserData().getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK, idObj, OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK)
			);
		} catch (BaseException e) {
			LOGGER.error("Грешка при заключване на задача! ", e);
		}	
	}
	
	/**
	 * при нова на задача - да отключа само задачите, заключени от потребителя
	 */
	public void unlockTask(){
		int tp = (Integer) getAttributes().get("taskProcess");
		if(tp != 2) { 
			// за сега ще изключим заключване от работния плот
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("unlockData Task!" );
			}
			LockObjectDAO daoL = new LockObjectDAO();		
			try { 
				JPA.getUtil().runInTransaction(() -> 
					daoL.unlock(getUserData().getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK)
				);
				getUserData().setReloadPage(false);
	        } catch (BaseException e) {
				LOGGER.error("Грешка при отключване на задача! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UI_BEANMESSAGES, ERRDATABASEMSG), e.getMessage());
			}
			
		}
	}
	
	
	
	/**
	 * Определя ролята на текущия потребител в задачата
	 */
	public void loadRole(Task tmpTask) {
		// права за достъп до полетата!!!
	
		setStatusList(new ArrayList<>());
		Integer tmpRole = null;
		//Пълен достъп за актуализация на задачи - да се приравни на "права на контролиращ задача". Само, ако задачата е от регистрaтурата на потребителя!!
		boolean fullRole = getUserData().hasAccess(OmbConstants.CODE_CLASSIF_DEF_PRAVA, OmbConstants.CODE_ZNACHENIE_DEF_PRAVA_TASK_FULL_EDIT)
						   && Objects.equals(tmpTask.getRegistraturaId(), getUserData().getRegistratura());
		
		Set<Integer> roles = new TaskDAO(getUserData()).findUserRolesInTask(tmpTask);
		if((roles != null && !roles.isEmpty()) || fullRole) {			
			tmpRole = tempRoleAndStatus(roles, fullRole);
		}	
		
		setRole(tmpRole);
		setViewPanelExec(!Objects.equals(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP, tmpTask.getStatus()) ); // ако статуса е различен  от "неизпълнена" - да се вижда панела за обработки по изпълнението
		setViewOpinion(Objects.equals(tmpTask.getStatus(),OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP) 
				    || Objects.equals(tmpTask.getStatus(),OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP_SROK));
		loadViewDateExec(tmpTask.getStatus());			
		loadDisabledEdit(tmpRole,  tmpTask.getStatus());
		
	
	}
	

	/**
	 * Връща ролята на потребителя в задачата и зарежда списъка с допустими статуси, в зависимост от ролята
	 * @param roles
	 * @return
	 */
	private Integer tempRoleAndStatus(Set<Integer> roles, boolean fullRole){
		Object[] rs = new Object[3]; // rs[0] - роля;
			
		roleStatusAC(roles, rs, fullRole); // ако е възложител или контролиращ
		roleStatusExec(roles, rs);			// ако е изпълнител
	   
		@SuppressWarnings("unchecked")
		List<SelectItem> items = createSelectItemListStatus((List<SystemClassif>) rs[1],  (List<SystemClassif>) rs[2]);			
		setStatusList(items);
		return  (Integer)rs[0];
	}
	 
	/**
	 * Определяне на роля и статус на възложител и контролиращ
	 * @param rs
	 * @return
	 */	
	private Object[] roleStatusAC(Set<Integer> roles, Object[] rs, boolean fullRole) {
		Integer tmpRole = (Integer)rs[0];
		List<SystemClassif> tmpStatusListA = new ArrayList<>();			
		if( roles != null &&  roles.contains(OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_ASSIGN)) {
			tmpRole = OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_ASSIGN;
			tmpStatusListA = loadStatus(tmpRole);
		}
		
		if(( roles != null &&  roles.contains(OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_CONTROL) || fullRole) && tmpRole == null) {
			tmpRole = OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_CONTROL;
			tmpStatusListA = loadStatus(tmpRole);
		}
		rs[0] = tmpRole;
		rs[1] = tmpStatusListA;
		return rs;
	}
	
	/**
	 * Определяне на роля и статус на изпълнителите
	 * @param rs
	 * @return
	 */	
	private Object[] roleStatusExec(Set<Integer> roles, Object[] rs) {
		Integer tmpRole = (Integer)rs[0];
		List<SystemClassif> tmpStatusListE = new ArrayList<>();
		if(roles != null &&  roles.contains(OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG) ){
			tmpRole = tmpRole == null ? OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG : tmpRole;
			tmpStatusListE = loadStatus(OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG);  
		}else if(roles != null &&  roles.contains(OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC) ){		
			tmpRole = tmpRole == null ? OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC : tmpRole;
			tmpStatusListE = loadStatus(OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC); 
		}else if(tmpRole == null && roles != null &&  roles.contains(0)) {
			tmpRole = 0;// registrtor!!!! - няма право да сменя статуса!
		}
		rs[0] = tmpRole;
		rs[2] = tmpStatusListE;
		return rs;
	}
	
	
	
	/**
	 * В зависимост от ролята - до кои полета на екрана потребителя да има достъп
	 * @param tmpRole
	 */
	private void loadDisabledEdit(Integer tmpRole, Integer taskStatus) {
		setEditM(false);
		setEditE(false);
		if(tmpRole != null) {
			if(Objects.equals(tmpRole, OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_ASSIGN) || Objects.equals(tmpRole, OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_CONTROL)) {
				setEditM(true);
				setEditE(true);
			} else if(!Objects.equals(taskStatus,OmbConstants.CODE_ZNACHENIE_TASK_STATUS_SNETA) &&
					 (Objects.equals(tmpRole,OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG) || Objects.equals(tmpRole,OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC))) {
				setEditE(true);
			} else if (tmpRole == 0 && Objects.equals(taskStatus, OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP)){
				//Registrator ??
				setEditM(true); // да променя осн. данни, само през актуализация на задача и само, ако задачата е със статус "неизпълнена"
			}
		}
	}
	
	/**
	 * Списък от SelectItem - за избор на статус
	 * @param listA
	 * @param listE
	 * @return
	 */
	public List<SelectItem> createSelectItemListStatus(List<SystemClassif>  listA, List<SystemClassif> listE) {
		if(!listA.isEmpty()) {
			for(SystemClassif itemE: listE) {
				boolean bb = true;
				for(SystemClassif itemA: listA) {
					if(itemA.getCode() == itemE.getCode() ) {
						bb = false; 
						break;
					}
				}
				if(bb) {
					listA.add(itemE); // ако потребителя има повече от една роли  - да се обединят допустимите значения
				}
			}
		}else {
			listA.addAll(listE);
		}
	
		List<SelectItem> items = new ArrayList<>(listA.size());
		for (SystemClassif x : listA) {
			items.add(new SelectItem(x.getCode(), x.getTekst()));				
		}			
		return items;
	}



	/**
	 * Зарежда списъка с допустими статуси в зависмост от ролята на потребителя
	 * 
	 */
	private List<SystemClassif>  loadStatus(Integer role) {
		
		List<SystemClassif> tmpStatusList = new ArrayList<>();
		try {
			tmpStatusList = getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_TASK_REF_ROLE_TASK_STATUS, role, getLang(),  new Date());
		//	SysClassifUtils.doSortClassifTekst(tmpStatusList); //сортиране по име ?
			
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на логически списък - статуси - роля! ", e);
		}
		return tmpStatusList;
	}
	
	
	/**
	 * Промяна на статус на задача
	 */
	public void actionChStatus(){ 
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("actionChStatus{}", getNewStatus());
		}
		Integer tmpStatus = getNewStatus();
		if(tmpStatus == null) {
			tmpStatus = getTask().getStatus();			
			setViewPanelExec(!Objects.equals(tmpStatus, OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP)); // вземам текущия статус
			setViewOpinion(Objects.equals( getTask().getStatus(), OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP) 
						|| Objects.equals( getTask().getStatus(), OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP_SROK));
		} else if(Objects.equals(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP, tmpStatus)	) {
			// da nuliram poletata преди записа, а не тук - да не остане нещо в описанието....!!!
			setViewPanelExec(false);		
			setViewOpinion(false);
		}else if(!Objects.equals(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP, tmpStatus)	) {
			setViewPanelExec(true);
			setViewOpinion(tmpStatus.equals(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP));
		}
		if(!Objects.equals(tmpStatus, getTask().getStatus())){
			//Нулира и полето за коментар и мнение!
			getTask().setStatusComments(null);
			getTask().setEndOpinion(null);
			// Нулира данните за подписването!
			setExecSigned(false);
			setExecString(null);
			getTask().setSignExe(null);
		}
		loadViewDateExec(tmpStatus);
	}
	
	/**
	 * Да се вижда ли полето за дата на изпълнение/снемане
	 * @param cSttaus
	 */
	public void  loadViewDateExec(Integer cStatus) {
		setViewDateExec(false);
		if (isViewPanelExec() && 
				(Objects.equals(cStatus,OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP) || 
				 Objects.equals(cStatus,OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP_SROK) || 
				 Objects.equals(cStatus,OmbConstants.CODE_ZNACHENIE_TASK_STATUS_SNETA ))){
			setViewDateExec(true);	
			
			labelDateExec =  Objects.equals(cStatus, OmbConstants.CODE_ZNACHENIE_TASK_STATUS_SNETA) ?  
					         IndexUIbean.getMessageResourceString(LABELS,"tasks.dateSnemane") : 
					         IndexUIbean.getMessageResourceString(LABELS,"tasks.dateExec");
			if(getTask() != null) {
				getTask().setRealEnd(new Date()); // по подразбиране - днешна дата			
			}
		}
	}  

	/**
	 * Промяна на вида на задачата
	 */
	public void actionChTypeTask(Integer typeTask){
		List<SystemClassif> tmpOpinionLst = new ArrayList<>();
		try {
			tmpOpinionLst = getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_TASK_TYPE_TASK_OPINION, typeTask, getLang(),  new Date());
			//Задачи за един изпълнител, т.е. изпълнява се инидивидуално
			setOneExec(false);
			setOneExecD(false);
			if(getTask().getId() == null) {
				if (getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_TASK_ONE_EMPL, typeTask, new Date())) {
					setOneExec(true);
					setOneExecD(true);
					// при актуализация - няма да се взема под внимание!!! - checkBox - само за нова задача 
				}
				//Задачи по подразбиране без срок - само за нови задачи
				if (getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_TASK_NO_DEADLINE, typeTask, new Date())) {
					setBezSrok(true);
					getTask().setSrokDate(null);
				}else {
					setBezSrok(false);
					if(getSrokDays() != 0) {
						getTask().setSrokDate(DateUtils.addDays(getTask().getAssignDate(), getSrokDays(), true));// по-подразбиране срок за изпълнение
					}
				}
			}			
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при промяна вид на задача! ", e);
		}
		List<SelectItem> items = new ArrayList<>(tmpOpinionLst.size());
		for (SystemClassif x : tmpOpinionLst) {
			items.add(new SelectItem(x.getCode(), x.getTekst()));				
		}		
		setOpinionLst(items);
		// за стари задачи - промяната на типа е забранен!
//		if( Objects.equals(typeTask, OmbConstants.CODE_ZNACHENIE_TASK_TYPE_PODPIS) ||
//			Objects.equals(typeTask, OmbConstants.CODE_ZNACHENIE_TASK_TYPE_SAGL) ||
//			Objects.equals(typeTask, OmbConstants.CODE_ZNACHENIE_TASK_TYPE_REZOL)) {
//			setEditTypeTsk(false);
//		}
//		else {
//			setEditTypeTsk(true);
//		}
		
	}
   
   /**
    * Задачата - без срок за изпълнение
    */
	public void actionClickBezSrok() {
		if(isBezSrok()) {
			getTask().setSrokDate(null);
		}else if(getSrokDays() != 0) {
			getTask().setSrokDate(DateUtils.addDays(getTask().getAssignDate(), getSrokDays(), true));// по-подразбиране срок за изпълнение
		}
	}
	  

   /** 
    * Запис на задача 
    * @param mode = true (задча към един док.); false - една задача към много документи (гр. зад. от раб. плот)
    */
   public boolean actionSave(boolean mode){ 
	   boolean bb = false;
	   if(checkData()) {		
			try {
			   boolean newTask = this.tmpTask.getId() == null; // в checkData се зарежда tmpTask; 
			   
			   taskParams();			  
					
			   saveTask(newTask, mode);
		  
			   optionsAfterSave(newTask);
			   
			   // извиква remoteCommnad - ако има такава....
			   String remoteCommnad = (String) getAttributes().get("onComplete");
			   if (remoteCommnad != null && !remoteCommnad.equals("")) {
					PrimeFaces.current().executeScript(remoteCommnad);
			   }			
			   JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(UI_BEANMESSAGES, SUCCESSAVEMSG));
			   bb = true;
			} catch (BaseException e) {			
				if (e.getCause() == null) { // това означава, че не е системна грешка и трябва да излезе само текста който е формиран в метода за запис
					LOGGER.error("Запис на задача -{}", e.getMessage());
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, e.getMessage());
				} else {
					LOGGER.error("Грешка при запис на задача ", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UI_BEANMESSAGES, ERRDATABASEMSG), e.getMessage());
				}
			}
		} 
	   return bb;
   }
   
   /**
    * настройки на екрана след запис на задачата
    */
   private void optionsAfterSave( boolean newTask ) {
	   if (isOneExec() ) {
		   actionNewTask(true); 	
	   }else {
		   if( tmpTask != null && tmpTask.getId() != null) {
			   //връща id на задача - ако е необходимо 
			    ValueExpression expr2 = getValueExpression(ID_TASK_ATTR);
				ELContext ctx2 = getFacesContext().getELContext();
				if (expr2 != null) {
					expr2.setValue(ctx2, tmpTask.getId());
				}	
		   }	
		
		   loadRole(tmpTask);
		   setTask(tmpTask); // излишно е, ако веднага при запис се затваря модалния, иначе не е....
		   setNewStatus(null);
		   if(newTask) {
			   lockTask(tmpTask.getId()); // 
			   //заключване на новата задачата, която е на екрана в момента
		   }
	   }
   }
 
   /**
    * зарежда праметри на задачата преди същинския запис
    * @throws DbErrorException
    */
   private void taskParams() throws DbErrorException {
	   if(getNewStatus() != null) {
		   this.getTask().setStatus(getNewStatus());
	   }
	   if(isDocReq()) {
		   tmpTask.setDocRequired(SysConstants.CODE_ZNACHENIE_DA);
	   }else {
		   tmpTask.setDocRequired(SysConstants.CODE_ZNACHENIE_NE);
	   }
	   int tp = (Integer) getAttributes().get("taskProcess");
	   if(tp == 0) { // само, ако се извиква през екрана на документи
		   DocData bean = (DocData) JSFUtils.getManagedBean("docData"); 
		   docEdit = null;
		   if(bean != null ) {
			  docEdit = bean.getDocument();
		   }
		}else if(this.tmpTask.getDocId() != null){
		      docEdit = new DocDAO(getUserData()).findById(this.tmpTask.getDocId());  // ако не е зареден вече
		}
	  
		if(!(Objects.equals(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP, this.tmpTask.getStatus()) ||
		     Objects.equals(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP_SROK, this.tmpTask.getStatus()) )) {
			getTask().setEndDocId(null);
			getTask().setRnDocEnd(null);
			getTask().setDateDocEnd(null);
		}
	}
  
   /**
    * извиква метода за запис на задача и файловете в обща транзакция
    * @param newTask
    * @param mode = true (задча към един док.); false - една задача към много документи (гр. зад. от раб. плот)
    * @throws BaseException
    */
 
   private void saveTask(boolean newTask, boolean mode) throws BaseException{   
	  if(mode) { 
		  JPA.getUtil().runInTransaction(() ->  // запис на задачата и файловете в обща транзакция
			  saveTask1(newTask)
		  ); 
	  }else {
		  @SuppressWarnings("unchecked")
		  List<Long> docList = (List<Long>)  getAttributes().get("docList");
		  
		  List<Integer> exec = null;
		  if( isOneExec()) {
			  exec = getTask().getCodeExecs();
		  }
		  docEdit = null; // тук е излишно да се подава. Необх. е за нотификациите и ще се извлече при тяхното формиране
		  for(Long d: docList) { 
			 getTask().setId(null);
			 getTask().setDocId(Integer.valueOf(d.toString()));
			 if(exec != null) {
				 getTask().setCodeExecs(exec);
			 }			 
			 JPA.getUtil().runInTransaction(() ->  // запис на задачата и файловете в обща транзакция
				  saveTask1(newTask)
			  ); 
		  }
	  }
	}
   
   /**
    * запис на задача
    * @param newTask
    * @throws BaseException
    */
   private void saveTask1(boolean newTask) throws BaseException{
	    Task task = getTask();
	    // при промяна на док при приключване за задача по документ трябва да се правят връзки между документи
	    boolean refreshDocVrazki = task.getDocId() != null && !Objects.equals(task.getEndDocId(), task.getDbEndDocId());
	    
		this.tmpTask = new TaskDAO(getUserData()).save(task, isOneExec(),docEdit ,getSystemData());
	    if (newTask && getFilesListTsk() != null && !getFilesListTsk().isEmpty()) {
			// при регистриране на официален от работен!!!
			// да направи връзка към  новия документ
			FilesDAO filesDao = new FilesDAO(getUserData());					
			for (Files f : getFilesListTsk()) {
				filesDao.saveFileObject(f, this.tmpTask.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_TASK);
			}
		}

	    if (refreshDocVrazki) { // за да заредят на ново, защото може да има нова или изтрита връзка
			DocDataVrazki docDataVrBean = (DocDataVrazki) JSFUtils.getManagedBean("docDataVrazki");
	 		if(	docDataVrBean != null) {
	 			docDataVrBean.setRefreshList(true);
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
	    tmpTask = getTask();
	   		   
	    if (context != null && tmpTask != null ) { 
		   clientId =  this.getClientId(context);
		  
		   // проверка за валидни дати	
		   flagSave = checkDates(clientId); 	  
		   
		   // възложител
		   if(tmpTask.getCodeAssign() == null) {
			   JSFUtils.addMessage(clientId+":vazl:аutoCompl",FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(BEANMESSAGES,"task.msgCodeAssign")); 
				flagSave = false;	
		   }
		   
		   // изпълнители
		   if(tmpTask.getCodeExecs() == null || tmpTask.getCodeExecs().isEmpty()) {
			   JSFUtils.addMessage(clientId+":lstIzp:autoComplM",FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(BEANMESSAGES,"task.msgCodeExec")); 
				flagSave = false;	
		   }
		   
		  // проверка за мнение при приключване и коментар
		   flagSave = flagSave && checkOpinionAdnComment(clientId); 
		
		  // проверка док. при приключване 
		   flagSave = flagSave && checkDocEnd(clientId);
				   		
//		   if(isEmpty(tmpTask.getTaskInfo())) { // описание на задачата - да не е задължително
//			JSFUtils.addMessage(clientId+":taskInfo",FacesMessage.SEVERITY_ERROR,
//					IndexUIbean.getMessageResourceString(BEANMESSAGES,"task.msgInfo")); 
//				flagSave = false;	
//		   }
		 
	    } else {
		   flagSave = false;
	    }		
		return flagSave;
	}

	/**
	 * Проверка за валидни дати
	 * @param clientId
	 * @return
	 */
	private boolean checkDates(String clientId) {
		boolean flagD = true;
	   //дата на възлагане 
	   Date adate = DateUtils.startDate(tmpTask.getAssignDate());
	   Date cdate = DateUtils.startDate(getDateDoc());
	   if(adate == null || (tmpTask.getDocId() != null &&  adate.before(cdate))) {//  дали не е преди дата на документа!
			JSFUtils.addMessage(clientId+":vDat",FacesMessage.SEVERITY_ERROR,
					IndexUIbean.getMessageResourceString(BEANMESSAGES,"task.msgDateAssign")); 
			flagD= false;	
	   }
	   
	   if(flagD) { // да се проверяват останaлите дати само, ако има валидна дата на възлагане
		   // срок изпълнение    
		   boolean flagSrokErr =  tmpTask.getSrokDate() == null;
		   Date sdate = DateUtils.startDate(tmpTask.getSrokDate());
		   if(isBezSrok()) {
			   tmpTask.setSrokDate(null);
			   flagSrokErr = false;
		   } else if(!flagSrokErr && sdate!=null && sdate.before(adate)) {
			   flagSrokErr = true;
		   }
		   if(flagSrokErr) {
			   JSFUtils.addMessage(clientId+":srokDat",FacesMessage.SEVERITY_ERROR,
					    IndexUIbean.getMessageResourceString(BEANMESSAGES,"task.msgSrokDate")); 
			   flagD= false;
		   }
		   
			// дата на изпълнение 
		   Date enddate = DateUtils.startDate(tmpTask.getRealEnd());		
		   if (!isViewDateExec()) { 
			   tmpTask.setRealEnd(null); // ако преди това е имало нещо - да го нулирам
		   }else if(enddate == null || enddate.before(adate)) {//  дали не е преди дата на възлагaнe!
				JSFUtils.addMessage(clientId+":exeDat",FacesMessage.SEVERITY_ERROR,
						IndexUIbean.getMessageResourceString(BEANMESSAGES,"task.msgDateRealEnd")); 
				flagD = false;	
		   }			    
	   }
	   return flagD;
	}
	
	
	/**
	 * проверка за мнение при приключване и коментар
	 * @param clientId
	 * @return
	 */
	private boolean checkOpinionAdnComment(String clientId){ 
	   boolean flagO = true;
	   boolean bb =  SearchUtils.isEmpty(tmpTask.getStatusComments()) && !Objects.equals(OmbConstants.CODE_ZNACHENIE_TASK_TYPE_REZOL,tmpTask.getTaskType());
	   boolean b1 =  !(getNewStatus() != null && OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP == getNewStatus().intValue());
	   boolean b2 = !(getNewStatus() == null && Objects.equals(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_NEIZP, tmpTask.getStatus()));
	   bb = bb && b1 && b2;   // изисква ли се коментар за избраното мнение

	   boolean bb1 = isViewOpinion() && !getOpinionList().isEmpty(); //// мнение при приключване - изисква се 
	   
	   if( bb1  && tmpTask.getEndOpinion() == null) {
		   JSFUtils.addMessage(clientId+":opinion",FacesMessage.SEVERITY_ERROR,
					IndexUIbean.getMessageResourceString(BEANMESSAGES,"task.msgOpinion")); 
			flagO= false;
	   } else if(bb1) {
		    try {
				bb  = bb  && getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_TASK_OPINION_WITH_COMMENT, tmpTask.getEndOpinion(), new Date());
			} catch (DbErrorException e) {
				LOGGER.error("Грешка - мнения, коментар! ", e);
			}
	   }
	   
	   if(bb){
	    	// полето коментар да е задължително при смяна на статуса, ако зад. не е "резолюция" и ако мнението задълж. изисква коментар - празно е при въвеждане на нова задача
	    	JSFUtils.addMessage(clientId+":taskStComment",FacesMessage.SEVERITY_ERROR,
					IndexUIbean.getMessageResourceString(BEANMESSAGES,"task.msgStComment")); 
			flagO = false;	
	   }
	   return flagO;
	}
	
	
	/**
	 * проверка док. при приключване 
	 * @param clientId
	 * @return
	 */
	private boolean checkDocEnd(String clientId) {
	  boolean flagDE = true;
	  boolean bb  =  getNewStatus() != null && getNewStatus().intValue() ==  OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP;       			// има смяна на статуса към "изпълнена"
	  boolean  bb1 =  getNewStatus() == null && (Objects.equals(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP, tmpTask.getStatus()) 
			   						 ||  Objects.equals(OmbConstants.CODE_ZNACHENIE_TASK_STATUS_IZP_SROK, tmpTask.getStatus()) );  
			   
	   if((bb || bb1) && Objects.equals(SysConstants.CODE_ZNACHENIE_DA,tmpTask.getDocRequired()) && tmpTask.getEndDocId() == null) {
//			Изисква дел. док. при приключване 
		   JSFUtils.addMessage(clientId+":rnDocEnd",FacesMessage.SEVERITY_ERROR,
				   IndexUIbean.getMessageResourceString(UI_BEANMESSAGES,MSGPLSINS,IndexUIbean.getMessageResourceString(LABELS, "tasks.delDocReq1")));
		   flagDE = false;
	   }
	   return flagDE;
	}
	
   /** 
    * Изтриване на задача 
    */
   public void actionDelete(){ 
		try {
			Task task = getTask();
		    // ако задача по документ има док при приключване трябва да се правят връзки между документи
			boolean refreshDocVrazki = task.getDocId() != null && task.getEndDocId() != null;

			TaskDAO taskDao = new TaskDAO(getUserData());
			JPA.getUtil().runInTransaction(() -> taskDao.deleteById(task.getId()));
		   	
			taskDao.notifTaskDelete(task, null, getSystemData());
			
			actionNewTask(true);		
			
			ValueExpression expr2 = getValueExpression(ID_TASK_ATTR);
			ELContext ctx2 = getFacesContext().getELContext();
			if (expr2 != null) {
				expr2.setValue(ctx2, null);
			}	
			
		    // извиква remoteCommnad - ако има такава....
			String remoteCommnad = (String) getAttributes().get("onComplete");
			if (remoteCommnad != null && !remoteCommnad.equals("")) {
				PrimeFaces.current().executeScript(remoteCommnad);
			}			
			
		    if (refreshDocVrazki) { // за да заредят на ново, защото може да има нова или изтрита връзка
				DocDataVrazki docDataVrBean = (DocDataVrazki) JSFUtils.getManagedBean("docDataVrazki");
		 		if(	docDataVrBean != null) {
		 			docDataVrBean.setRefreshList(true);
		 		}
		    }
		
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,  IndexUIbean.getMessageResourceString(UI_BEANMESSAGES, "general.successDeleteMsg") );
		} catch (ObjectInUseException e) {
			LOGGER.error("Грешка при изтриване на задача-->{}", e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());

		}catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на задача ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UI_BEANMESSAGES, ERRDATABASEMSG));
		}
		
   }
 
//   /** 
//    * коригиране данни на задача - изп. се само, ако е в модален прозорец
//    * изивква се при затваряне на модалния прозореца (onhide) 
//    * 
//    */
//   public void actionHideModal() {		
//	   // за сега няма да се ползва
//	   setTask(null);
//	   setShowMe(false);
//	   LOGGER.debug("actionHideModal>>>> ");
//	}
//   
//   
   
   public void fillHistoryList() {
	   try {
		   setHistoryListH(new TaskDAO(getUserData()).findTaskStatesList(getTask().getId()));
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на списък История на промените за задача! ", e);
		} finally {
			JPA.getUtil().closeConnection();
		}
   }
   
   public void fillDefinitionEditList() {
	   try {
		   setDefEditListD(new TaskDAO(getUserData()).findTaskEditList(getTask().getId(), getSystemData()));
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на списък Промяна на дефинициите за задача! ", e);
		} finally {
			JPA.getUtil().closeConnection();
		}
   }

	
	//**********************************
	
	// Документ при приключване

	/**
	 * търсене по дел. номер на документ - бутон или излизане от полето 
	 * @param bb
	 */
	public void actionSearchDocBtn(boolean bb) {
	
		if (SearchUtils.isEmpty(getTask().getRnDocEnd())) {

			getTask().setEndDocId(null);
			getTask().setDateDocEnd(null); 
			setDocEndObj(null);
			
		}
		if(getSearchFlagDocEnd() == 0){
			searchDocEnd(bb);			
		}else {
			setSearchFlagDocEnd(0);
		}
		
	}
	
	
	/**
	 * Търсене по делов. номер
	 * @param bb = false - ако се извиква директно от полето за въвеждане; true - от лупата
	 * @return
	 */
	private Object[] searchDocEnd(boolean bb) {
	
		Object[] sDoc = null;

		if (SearchUtils.isEmpty(getTask().getRnDocEnd())) { 
			getTask().setRnDocEnd("");
		}else if (!bb){
			bb = true;
		}

		if (bb) {
			DocSearch tmp = new DocSearch(getUserData().getRegistratura());
			tmp.setRnDoc(getTask().getRnDocEnd());
			//тип док - само за собствени и работни
			tmp.setDocTypeArr(getDocTypeArr());
			
			tmp.buildQueryComp(getUserData());
	
			LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(tmp, "a1 desc");
			int res = lazy.getRowCount();
			
			if (res == 0 ) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Не е намерен документ с посочения номер!");
				}
				getTask().setDateDocEnd(null);
				getTask().setEndDocId(null);
				setDocEndObj(null);
				setSearchFlagDocEnd(0);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(BEANMESSAGES, "doc.srchDocDocErrT"));
			} else {
				sDoc = new Object[5];
				String dialogWidgetVar = "PF('docEndVar').show();";
				PrimeFaces.current().executeScript(dialogWidgetVar);
			}
		}
		return sDoc;
	
	}


	/**
	 * търсене по делов. номер - при скриване на модалния
	 */
	public void onHideModalDocEnd() {
	   Object[] docObj= getDocEndObj(); 
	   if(docObj != null && docObj[0] != null) {
		   // да заредя полетата
		    getTask().setEndDocId(Integer.valueOf(docObj[0].toString())); // id_to na DOC object
		    getTask().setRnDocEnd( SearchUtils.asString(docObj[1]));
		    getTask().setDateDocEnd(SearchUtils.asDate(docObj[4]));
		    		    
	   }else {
		   // модалния за избор е отворен, но нищо не е избрано!
		   getTask().setEndDocId(null);
		   getTask().setRnDocEnd(null);
		   getTask().setDateDocEnd(null);
	   }
	   setDocEndObj(null);	 
	   setSearchFlagDocEnd(0);
	}
	
	/**
	 * разглеждане на док. при приключване
	 * @return
	 */
	public String actionGotoViewDoc() {
		return "docView.xhtml?faces-redirect=true&idObj=" + getTask().getEndDocId();
	}
	
	

//	//**** Достъп до преписки през задача. Само, ако потребителия има дефинитивно право: "Право да дава достъп до преписка през задача"
//   /**
//    * @throws DbErrorException 
//	* 
//	*/
//	public void actionAccessDelo() throws DbErrorException {
//		Integer idDoc = getTask().getDocId(); //(Integer) getAttributes().get("idDoc");  
//		if(idDoc != null) {
//			//1. зареждам списъка с преписки към документа
//			DeloDocDAO	deloDocDao = new DeloDocDAO(getUserData());			
//			SelectMetadata sm = deloDocDao.createSelectDeloListByDoc(idDoc); // списък преписки, в които е вложен документа
//			setPrepList( new LazyDataModelSQL2Array(sm, "a0"));	
//			
//  		    // списък с лица: възложител, контролиращ, изпълнители
//			List<Integer> tmpList = new ArrayList<>();
//			tmpList.add(getTask().getCodeAssign());
//			if(getTask().getCodeControl() != null && !tmpList.contains(getTask().getCodeControl())) {
//				tmpList.add(getTask().getCodeControl());
//			}			
//			for (Integer i: getTask().getCodeExecs()) {
//				if(!tmpList.contains(i)) {
//					tmpList.add(i);
//				}
//			}
//			
//			List<SelectItem> items = new ArrayList<>(tmpList.size());
//			for (Integer x : tmpList) {
//				String tekst = getSystemData().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, x, getUserData().getCurrentLang(), getDateDoc());		
//				items.add(new SelectItem(x, tekst));				
//			}	
//			setLicaList(items);
//			
//		
//			String dialogWidgetVar = "PF('accessDeloVar').show();";
//			PrimeFaces.current().executeScript(dialogWidgetVar);
//		}
//		
//	}
//
//	/*
//	 * Множествен избор на дело/преписка
//	 */
//		 
//	/**
//	 * маркиране на всички редове от текуща страница
//	 * @param tmpL
//	 * @param selTmp
//	 */
//	private void  rowSelectAll(List<Object[]> tmpL, List<Object[]> selTmp) {
//		for (Object[] obj : selTmp) {
//			if(obj != null && obj.length > 0) {
//				boolean bb = true;
//				Long l2 = Long.valueOf(obj[0].toString());
//				for (Object[] j : tmpL) { 
//	    			Long l1 = Long.valueOf(j[0].toString());        			
//		    		if(l1.equals(l2)) {    	    			
//		    			bb = false;
//		    			break;
//		    		}
//	    		}
//				if(bb) {
//					tmpL.add(obj);
//				}
//			}
//		} 
//	}
//		
//	/**
//	 * Размракиране на всички редове от текуща страница
//	 * @param tmpL
//	 * @param listRez
//	 */
//	private void  rowUnselectAll(List<Object[]> tmpL, List<Object[]> tmpLPageC) {
//		
//		for (Object[] obj : tmpLPageC) {
//			if(obj != null && obj.length > 0) {
//				Long l2 = Long.valueOf(obj[0].toString());
//				for (Object[] j : tmpL) { 
//	    			Long l1 = Long.valueOf(j[0].toString());        			
//		    		if(l1.equals(l2)) {    	    			
//		    			tmpL.remove(j);
//		    			break;
//		    		}	
//	    		}
//			}
//		}    	
//	}
//		
//    
//
//    /**
//     * избор на ред от списъка
//     * @param tmpList
//     * @param obj
//     * @return
//     */
//    private boolean rowSelect(List<Object[]> tmpList, Object[] obj  ) {
//		boolean bb = true;	
//		Integer l2 = Integer.valueOf(obj[0].toString());
//		for (Object[] j : tmpList) { 
//			Integer l1 = Integer.valueOf(j[0].toString());        			
//    		if(l1.equals(l2)) {
//    			bb = false;
//    			break;
//    		}
//   		}
//		return bb;
//    }
//   
//    /**
//     * размаркиране  на ред от списъка
//     * @param tmpList
//     * @param obj
//     * @return
//     */
//    private boolean rowUnselect(List<Object[]> tmpList, Object[] obj  ) {
//		boolean bb = false;		
//		Integer l2 = Integer.valueOf(obj[0].toString());
//		for (Object[] j : tmpList) {
//			Integer l1 = Integer.valueOf(j[0].toString());
//    		if(l1.equals(l2)) {
//    			tmpList.remove(j);
//    			bb = true;
//    			break;
//    		}
//		}
//		return bb;
//    }
//    
//    /**
//	 * преписки
//	 * Избира всички редове от текущата страница 
//	 * @param event
//	 */
//	 public void onRowSelectAllDelo(ToggleSelectEvent event) {    
//    	List<Object[]> tmpL = new ArrayList<>();    	
//    	tmpL.addAll(getDeloSelectedAllM());    	    	
//    	if(event.isSelected()) {
//    		rowSelectAll(tmpL, getDeloSelectedTmp());   		
//    	}else {
//    	   // rows from current page....   	
//    		rowUnselectAll(tmpL, getPrepList().getResult()); 			
//		}
//		setDeloSelectedAllM(tmpL);	    	
//		if (LOGGER.isDebugEnabled()) {
//			LOGGER.debug("onToggleSelect->>");
//		}
//	}
//	 
//
//	  /** 
//     * избор на ред от списъка - преписки 
//     * @param event
//     */
//   	public void onRowSelectDelo(SelectEvent<?> event) {    	
//    	if(event!=null  && event.getObject()!=null) {
//    		List<Object[]> tmpList =  getDeloSelectedAllM();
//    		Object[] obj = (Object[]) event.getObject();			
//			if(rowSelect(tmpList, obj)) {
//				tmpList.add(obj);
//				setDeloSelectedAllM(tmpList);   
//			}
//    	}	
//    	if (LOGGER.isDebugEnabled()) {
//    		LOGGER.debug("1 onRowSelectDelo->>{}",getDeloSelectedAllM().size());
//    	}
//    }
//	
//
//    /**
//     * unselect one row - преписки
//     * @param event
//     */
//	public void onRowUnselectDelo(UnselectEvent<?>  event) {
//    	if(event!=null  && event.getObject()!=null) {
//    		Object[] obj = (Object[]) event.getObject();
//    		List<Object[] > tmpL = new ArrayList<>();    		
//    		tmpL.addAll(getDeloSelectedAllM());
//    
//    		if(rowUnselect(tmpL, obj ) ) {
//    			setDeloSelectedAllM(tmpL);
//    		}
//    		
//    		if (LOGGER.isDebugEnabled()) {
//    			LOGGER.debug( "onRowUnselectIil->>{}",getDeloSelectedAllM().size());
//    		}
//    	}
//    }
//    
//
//		  
//    /**
//     * Преписки
//     * За да се запази селектирането(визуалано на екрана) при преместване от една страница в друга
//     */
//    public void   onPageUpdateSelectedDelo(){
//    	if (getDeloSelectedAllM() != null && !getDeloSelectedAllM().isEmpty()) {
//    		getDeloSelectedTmp().clear();
//    		getDeloSelectedTmp().addAll(getDeloSelectedAllM());
//    	}	    	
//    	if (LOGGER.isDebugEnabled()) {
//    		LOGGER.debug( " onPageUpdateSelected->>{}", getDeloSelectedTmp().size());
//    	}
//    }
//    
//
//	private Delo deloTmp;
//
//	/**
//	 * Запис на изричен достъп до преписки
//	 * 
//	 */
//	public void actionSaveAccess() {
//		if(checkAccessDelo()) {
//			try {
//				boolean bb = false;
//				DeloDAO	deloDao = new DeloDAO(getUserData());
//				//1. записавам задачата, ако има някакви промени (Не вкл. функц. от раб. плот - една зад. към много документи!)
//				if(actionSave(true)) {
//					//2. записвам достъпа до преписките
//					for(Object[] delo: getDeloSelectedAllM() ) {	
//						JPA.getUtil().runWithClose(() -> 
//							deloTmp = deloDao.taskAccessFindDelo(Integer.valueOf(delo[4].toString()))
//						);
//						bb = licaToDeloAccess(deloTmp);
//						if(bb) {
//							JPA.getUtil().runInTransaction(() -> 
//							 	deloDao.taskAccessSaveDelo(deloTmp, getSystemData())
//							); 
//						}
//					}
//					
//					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(BEANMESSAGES, "task.msgSuccessAccess"));
//				}
//				setDeloSelectedAllM(null);
//				setLicaSelected(null);
//			} catch (BaseException e) {
//				LOGGER.error("Грешка при запис на изричен достъп до преписки! ", e);
//				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UI_BEANMESSAGES, ERRDATABASEMSG), e.getMessage());
//			}
//			String dialogWidgetVar = "PF('accessDeloVar').hide();";
//			PrimeFaces.current().executeScript(dialogWidgetVar);
//		}	
//		if (LOGGER.isDebugEnabled()) {
//			LOGGER.debug( " actionSaveAccess->{}", getDeloSelectedAllM().size());
//		}  
//	}
//	
//	/**
//	 * запис на достъп до преписки
//	 * проверка за валидни данни  
//	 * @return
//	 */
//	private boolean checkAccessDelo() {
//		boolean saveOk = true;
//		FacesContext context = FacesContext.getCurrentInstance();
//	    String clientId = null;	  
//	    if (context != null ) { 
//		    clientId =  this.getClientId(context);		    
//			if(getDeloSelectedAllM() == null || getDeloSelectedAllM().isEmpty()) {
//				saveOk = false;
//				 JSFUtils.addMessage(clientId+":tblDeloList",FacesMessage.SEVERITY_ERROR,
//							IndexUIbean.getMessageResourceString(BEANMESSAGES,"task.msgAccessDelo")); 
//			}			
//			if(getLicaSelected() == null || getLicaSelected().length==0) {
//				saveOk = false;			
//				 JSFUtils.addMessage(clientId+":lica",FacesMessage.SEVERITY_ERROR,
//							IndexUIbean.getMessageResourceString(BEANMESSAGES,"task.msgAccessLica")); 
//			}
//	    }else {
//	    	saveOk = false;
//	    }
//	    return saveOk;
//	}
//	
//	/**
//	 * добавя лицата, които имат достъп към конкретната преписка
//	 * @param delo
//	 * @return
//	 */
//	private boolean licaToDeloAccess(Delo delo) {
//		boolean bb = false;
//		if(delo != null) {
//			DeloAccess da;	
//			List<DeloAccess> licaDA = delo.getDeloAccess();		
//			for (Integer idRef : getLicaSelected() ) {
//				bb = true;
//				for(DeloAccess da1: licaDA) {
//					if(da1.getCodeRef().equals(idRef)) {						
//						bb = false;
//					}
//				}
//				if(bb) {
//					da = new DeloAccess();
//					da.setCodeRef(idRef);
//					da.setDeloId(delo.getId());
//					licaDA.add(da);					
//				}
//			}		
//		}
//		return bb;
//	}
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


	public Date getDateDoc() {
		return (Date) getStateHelper().eval(PropertyKeys.DATEDOC,  null);
	}
	
	public void setDateDoc(Date docDate) {
		getStateHelper().put(PropertyKeys.DATEDOC, docDate);
	}

	
	private SystemData getSystemData() {
		if (this.systemData == null) {
			this.systemData =  (SystemData) JSFUtils.getManagedBean("systemData");
		}
		return this.systemData;
	}

	/** @return the userData */
	public UserData getUserData() {
		if (this.userData == null) {
			this.userData = (UserData) JSFUtils.getManagedBean("userData");
		}
		return this.userData;
	}


	/** @return */
	public Integer getLang() {
		return getUserData().getCurrentLang();
	}
	

	public Task getTask() {
		return (Task) getStateHelper().eval(PropertyKeys.TASK, null);
	}

	public void setTask(Task task) {
		getStateHelper().put(PropertyKeys.TASK, task);
	}


	/** @return */
	public boolean isDocReq() {
		return (Boolean) getStateHelper().eval(PropertyKeys.DOCREQ, false);
	}
	/** @param docReq */
	public void setDocReq(boolean docReq) {
		getStateHelper().put(PropertyKeys.DOCREQ, docReq);
	}


	/** @return */
	public boolean isBezSrok() {
		return (Boolean) getStateHelper().eval(PropertyKeys.BEZSROK, false);
	}
	/** @param bezSrok */
	public void setBezSrok(boolean bezSrok) {
		getStateHelper().put(PropertyKeys.BEZSROK, bezSrok);
	}



	/** @return */
	public boolean isOneExec() {
		return (Boolean) getStateHelper().eval(PropertyKeys.ONEEXEC, false);
	}
	/** @param oneExec */
	public void setOneExec(boolean oneExec) {
		getStateHelper().put(PropertyKeys.ONEEXEC, oneExec);
	}
	
	/** @return */
	public boolean isOneExecD() {
		return (Boolean) getStateHelper().eval(PropertyKeys.ONEEXECD, false);
	}
	/** @param oneExecD */
	public void setOneExecD(boolean oneExecD) {
		getStateHelper().put(PropertyKeys.ONEEXECD, oneExecD);
	}
	
	/** @return */
	public boolean isViewPanelExec() {
		return (Boolean) getStateHelper().eval(PropertyKeys.VIEWPANELEXEC, false);
	}
	/** @param viewPanelExec */
	public void setViewPanelExec(boolean viewPanelExec) {
		getStateHelper().put(PropertyKeys.VIEWPANELEXEC, viewPanelExec);
	}

	/** @return */
	public boolean isViewDateExec() {
		return (Boolean) getStateHelper().eval(PropertyKeys.VIEWDATEEXEC, false);
	}
	/** @param viewDateExec */
	public void setViewDateExec(boolean viewDateExec) {
		getStateHelper().put(PropertyKeys.VIEWDATEEXEC, viewDateExec);
	}
	
	/** @return */
	public boolean isViewOpinion() {
		return (Boolean) getStateHelper().eval(PropertyKeys.VIEWOPINION, false);
	}
	/** @param viewOpinion */
	public void setViewOpinion(boolean viewOpinion) {
		getStateHelper().put(PropertyKeys.VIEWOPINION, viewOpinion);
	}


	/** @return */
	public boolean isEditM() {
		return (Boolean) getStateHelper().eval(PropertyKeys.EDITM, false);
	}
	/** @param editM */
	public void setEditM(boolean editM) {
		getStateHelper().put(PropertyKeys.EDITM, editM);
	}
	

	/** @return */
	public boolean isEditE() {
		return (Boolean) getStateHelper().eval(PropertyKeys.EDITE, false);
	}
	/** @param еditE */
	public void setEditE(boolean editE) {
		getStateHelper().put(PropertyKeys.EDITE, editE);
	}

	/**
	 * списък изпълнители
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<SystemClassif> getScList() {
		List<SystemClassif> eval = (List<SystemClassif>) getStateHelper().eval(PropertyKeys.SCLIST, null);
		return eval != null ? eval : new ArrayList<>();
	}

	public void setScList(List<SystemClassif> scList) {
		getStateHelper().put(PropertyKeys.SCLIST, scList);
	}

	
	/** @return */
	@SuppressWarnings("unchecked")
	public List<SelectItem> getStatusList() {
		List<SelectItem> eval = (List<SelectItem>) getStateHelper().eval(PropertyKeys.STATUSLST, null);
		return eval != null ? eval : new ArrayList<>();
	}

	/** * @param statusList */
	public void setStatusList(List<SelectItem> statusList) {
		getStateHelper().put(PropertyKeys.STATUSLST, statusList);
	}

	
	/** @return */
	@SuppressWarnings("unchecked")
	public List<SelectItem> getOpinionList() {
		List<SelectItem> eval = (List<SelectItem>) getStateHelper().eval(PropertyKeys.OPINIONLST, null);
		return eval != null ? eval : new ArrayList<>();
	}

	/** * @param statusList */
	public void setOpinionLst(List<SelectItem> opinionLst) {
		getStateHelper().put(PropertyKeys.OPINIONLST, opinionLst);
	}

	

	/** @return */
	public Integer getRole() {
		return (Integer) getStateHelper().eval(PropertyKeys.ROLE, null);
	}

	/** * @param Rrle */
	public void setRole(Integer role) {
		getStateHelper().put(PropertyKeys.ROLE, role);
	}

	public Integer getDefaultStatus() {
		return defaultStatus;
	}

	public void setDefaultStatus(Integer defaultStatus) {
		this.defaultStatus = defaultStatus;
	}

	public Integer getNewStatus() {
		return (Integer) getStateHelper().eval(PropertyKeys.NEWSTATUS, null);
	}

	public void setNewStatus(Integer newStatus) {
		getStateHelper().put(PropertyKeys.NEWSTATUS,  newStatus);
	}

	public String getSrokPattern() {
		return (String) getStateHelper().eval(PropertyKeys.SROKPATTERN, "dd.MM.yyyy");
	}

	public void setSrokPattern(String srokPattern) {
		getStateHelper().put(PropertyKeys.SROKPATTERN,  srokPattern);
	}
	
	public String getSrokTime() {
		return (String) getStateHelper().eval(PropertyKeys.SROKTIME, "false");
	}

	public void setSrokTime(String srokTime) {
		getStateHelper().put(PropertyKeys.SROKTIME,  srokTime);
	}
	
	
	public int getSrokDays() {
		return (Integer) getStateHelper().eval(PropertyKeys.SROKDAYS, 0);		
	}

	public void setSrokDays(int srokDays) {
		getStateHelper().put(PropertyKeys.SROKDAYS, srokDays);
	}
	
	public String getLabelDateExec() {
		return labelDateExec;
	}


	public void setLabelDateExec(String labelDateExec) {
		this.labelDateExec = labelDateExec;
	}




	@SuppressWarnings("unchecked")
	public List<Files> getFilesListTsk() {
		List<Files> eval = (List<Files>) getStateHelper().eval(PropertyKeys.FILESLIST, null);
		return eval != null ? eval : new ArrayList<>();
	}


	public void setFilesListTsk(List<Files> filesListTsk) {
		getStateHelper().put(PropertyKeys.FILESLIST, filesListTsk);
	}


	public TimeZone getTimeZone() {
		return timeZone;
	}


	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	public Map<Integer, Object> getSpecificsAdm() {
		return Stream.of(SPECADMOBJ).collect(Collectors.toMap(data -> (Integer) data[0], data ->  data[1]));  // X.of() -> така ще дава само служители през аутокомплете, а в дървото ще е цялата
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getHistoryListH() {
		List<Object[]> eval = (List<Object[]>) getStateHelper().eval(PropertyKeys.HISTORYLIST, null);
		return eval != null ? eval : new ArrayList<>();
	}


	public void setHistoryListH(List<Object[]> historyList) {
		getStateHelper().put(PropertyKeys.HISTORYLIST, historyList);
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getDefEditListD() {
		List<Object[]> eval = (List<Object[]>) getStateHelper().eval(PropertyKeys.DEFEDITLIST, null);
		return eval != null ? eval : new ArrayList<>();
	}


	public void setDefEditListD(List<Object[]> defEditList) {
		getStateHelper().put(PropertyKeys.DEFEDITLIST, defEditList);
	}

	public String getLockMsg() {
		return lockMsg;
	}


	public void setLockMsg(String lockMsg) {
		this.lockMsg = lockMsg;
	}
	
	public Date getCurrentDate() {
		return new Date();
	}

	public int getSearchFlagDocEnd() {
		return (Integer) getStateHelper().eval(PropertyKeys.SEARCHFLAGDOCEND, 0);		
	}


	public void setSearchFlagDocEnd(int searchFlagDocEnd) {
		getStateHelper().put(PropertyKeys.SEARCHFLAGDOCEND, searchFlagDocEnd);
	}

	
	public Object[] getDocEndObj() {
		return (Object[]) getStateHelper().eval(PropertyKeys.DOCENDOBJ, null);	
	}

	public void setDocEndObj(Object[] docEndObj) {
		getStateHelper().put(PropertyKeys.DOCENDOBJ, docEndObj);
	}

	/**
	 * док. при приключване може да е само - собствен или работен
	 * @return
	 */
	public Integer[] getDocTypeArr() {
		return new Integer[]{ OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK };
		
	}


	public LazyDataModelSQL2Array getPrepList() {
		return (LazyDataModelSQL2Array) getStateHelper().eval(PropertyKeys.PREPLIST, null);
	}


	public void setPrepList(LazyDataModelSQL2Array prepList) {
		getStateHelper().put(PropertyKeys.PREPLIST, prepList);
	}

	/** @return */
	@SuppressWarnings("unchecked")
	public List<SelectItem> getLicaList() {
		List<SelectItem> eval = (List<SelectItem>) getStateHelper().eval(PropertyKeys.LICALIST, null);
		return eval != null ? eval : new ArrayList<>();
	}

	/** * @param statusList */
	public void setLicaList(List<SelectItem> licaList) {
		getStateHelper().put(PropertyKeys.LICALIST, licaList);
	}



	@SuppressWarnings("unchecked")
	public List<Object[]> getDeloSelectedAllM() {
		List<Object[]> eval = (List<Object[]>) getStateHelper().eval(PropertyKeys.DELOSEL, null);
		return eval != null ? eval : new ArrayList<>();
	}


	public void setDeloSelectedAllM(List<Object[]> deloSelectedAllM) {
		getStateHelper().put(PropertyKeys.DELOSEL, deloSelectedAllM);
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> getDeloSelectedTmp() {
		List<Object[]> eval = (List<Object[]>) getStateHelper().eval(PropertyKeys.DELOSELTMP, null);
		return eval != null ? eval : new ArrayList<>();		
	}


	public void setDeloSelectedTmp(List<Object[]> deloSelectedTmp) {
		getStateHelper().put(PropertyKeys.DELOSELTMP, deloSelectedTmp);
	}

	
	public Integer[] getLicaSelected() {
		return licaSelected;
	
	}

	public void setLicaSelected(Integer[] licaSelected) {
		this.licaSelected = licaSelected;
	}


	@SuppressWarnings("unchecked")
	public List<SelectItem> getTaskTypeList() {
		List<SelectItem> eval = (List<SelectItem>) getStateHelper().eval(PropertyKeys.LISTTYPETASK, null);
		return eval != null ? eval : new ArrayList<>();
	}

	
	public void setTaskTypeList(List<SelectItem> taskTypeList) {
		getStateHelper().put(PropertyKeys.LISTTYPETASK, taskTypeList);
	}


	public int getDefUserTask() {
		return (int) getStateHelper().eval(PropertyKeys.DEFUSERTASK, OmbConstants.CODE_ZNACHENIE_TASK_TYPE_DEFAULT);
	}


	public void setDefUserTask(int defUserTask) {
		getStateHelper().put(PropertyKeys.DEFUSERTASK, defUserTask);
	}	
	
	
	/* помощни текстове в поле "Допълнителна инфромация"*/
//	 public List<String> completeArea(String query) {
//	        List<String> results = new ArrayList<>();
//	        if (query != null && !query.trim().isEmpty()) {
//				try {
//					Integer codeClassif = OmbConstants.CODE_CLASSIF_TASK_HELP_TEXT;
//					if (codeClassif != null) {
//						List<SystemClassif> classifList =  getSystemData().queryClassification(codeClassif, query, new Date(), getLang() );
//						for(SystemClassif item: classifList) {
//							results.add(item.getTekst());	
//						}
//					}
//
//				} catch (Exception e) {
//					LOGGER.error(e.getMessage(), e);
//				}
//			}
//			return results;	       
//	    }
//	 
//	    public void onSelectArea(SelectEvent<String> event) {
//	    	getTask().setTaskInfo(event.getObject());
//	    
//	        LOGGER.info(event.getObject());
//	    }
	
	/**
	 * Избор на помощен текст за полето "доп. информация" в дефиницията
	 */
	public void selectDopInfHelp() {
		try {
			if(dopInfHelp != null) {
				String dopInfHelpTxt = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_TASK_HELP_TEXT, this.dopInfHelp, getUserData().getCurrentLang(), new Date());
				
				String oldTxt = getTask().getTaskInfo();
				if(SearchUtils.isEmpty(oldTxt)) {
					getTask().setTaskInfo(dopInfHelpTxt);
				}else {
					getTask().setTaskInfo(oldTxt +"; "+dopInfHelpTxt);
				}
			}
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при избор на помощен текст за полето доп. информация! ", e);
		}
	}

	public Object[] getCodeExtCheck() {
		return (Object[]) getStateHelper().eval(PropertyKeys.CODEEXTCHECK, null);
	}


	public void setCodeExtCheck(Object[] codeExtCheck) {
		getStateHelper().put(PropertyKeys.CODEEXTCHECK, codeExtCheck);
	}	
	

	public Integer getDopInfHelp() {
		return dopInfHelp;
	}

	public void setDopInfHelp(Integer dopInfHelp) {
		this.dopInfHelp = dopInfHelp;
	}
	
	/**
	 * дефиницията на задачата е подисана с ел. подпис
	 * @return
	 */
	public boolean isDefSigned() {
		return (Boolean) getStateHelper().eval(PropertyKeys.DEFSIGEND, false);
	}

	public void setDefSigned(boolean defSigned) {
		getStateHelper().put(PropertyKeys.DEFSIGEND, defSigned);
	}

	public String getDefString() {
		return (String) getStateHelper().eval(PropertyKeys.DEFSTRING, null);
	}

	public void setDefString(String defString) {
		getStateHelper().put(PropertyKeys.DEFSTRING, defString);
	}
	
	/**
	 * Изпълнението на задачата е подписано с ел. подпис
	 * @return
	 */
	public boolean isExecSigned() {
		return (Boolean) getStateHelper().eval(PropertyKeys.EXECSIGNED, false);
	}

	public void setExecSigned(boolean execSigned) {
		getStateHelper().put(PropertyKeys.EXECSIGNED, execSigned);
	}
	
	public String getExecString() {
		return (String) getStateHelper().eval(PropertyKeys.EXECSTRING, null);
	}

	public void setExecString(String execString) {
		getStateHelper().put(PropertyKeys.EXECSTRING, execString);
	}
		
	
	/**
	 * Дефиниция на задача
	 * Формира текст, който да се подаде за подписване
	 * След подписване на възлгането, се забранява корекцията на всички данни, които го описват.
	 * За да се направи корекция е необходимо първо да се премахне подписа  
	 */
	public void actionDefSigned() {
		if(checkData()) {	
			if(getDefString() == null || !isDefSigned()){
				StringBuilder sb = new StringBuilder("Зад.:"+getTask().getRnTask());
				sb.append(" Срок:"+ DateUtils.printDate(getTask().getSrokDate()));
				sb.append(" Възл.:"+getTask().getCodeAssign());
				sb.append(" Изп.:"+getTask().getCodeExecs());
				setDefString(sb.toString());
			}
		}else {
			setDefString(null);
		}	
	}

	/**
	 * Дефиниция на задача
	 * ако върне нулл - значи не е подписано
	 * ако не е нулл  - значи е подписано => да се запише задачата
	 * 
	 */
	public void actionDefCompleteSigned() {
		boolean saveTsk = true;
		if(getDefString() == null) {
			setDefSigned(false);  // не е подписано
			if(getTask().getSignDef() == null) {
				saveTsk = false;
			}else {
				getTask().setSignDef(null);
			}
		}else {
			setDefSigned(true);
			getTask().setSignDef(getDefString());
		}
		if(saveTsk) {
			actionSave(true);
		   // Съобщение, че дефиницията е подписана 
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(BEANMESSAGES, SIGNSAVEMSG));
		}
		
	}
	
	/**
	 * Дефиниция на задача
	 * Премахване на подписа и запис на задача
	 */
	public void actionDefRemoveSigned() {
	
		setDefSigned(false);
		getDefString();
		getTask().setSignDef(null);
		actionSave(true);
		// Съобщение, че подписът е премахнат
		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(BEANMESSAGES, SIGNDELMSG));
	}
	

	/**
	 * Изпълнение на задача
	 * Формира текст, който да се подаде за подписване
	 * След подписване на изпълнението, се забранява корекцията на всички данни, които го описват, без статуса!!
	 * При смяна на статуса  и запис - подписа пада! 
	 * За да се направи корекция е необходимо първо да се премахне подписа
	 * Подписването е само за последния статус, свързан с изпълнението!!!  
	 */
	public void actionExecSigned() {
		if(checkData()) {	
			if(getExecString() == null || !isExecSigned()){
				StringBuilder sb = new StringBuilder("Зад.:"+getTask().getRnTask());
				sb.append(" Дата на изп:"+ DateUtils.printDate(getTask().getRealEnd()));
				//sb.append(" Коментар:"+getTask().getStatusComments());
				//sb.append(" Мнение:"+getTask().getEndOpinion());
				setExecString(sb.toString());
			}
		}else {
			setExecString(null);
		}
	}

	/**
	 * Изпълнение на задача
	 * ако върне нулл - значи не е подписано
	 * ако не е нулл  - значи е подписано => да се запише задачата
	 * 
	 */
	public void actionExecCompleteSigned() {
		boolean saveTsk = true;
		if(getExecString() == null) {
			setExecSigned(false);  // не е подписано
			if(getTask().getSignExe() == null) {
				saveTsk = false;
			}else {
				getTask().setSignExe(null);
			}
		}else {
			setExecSigned(true);
			getTask().setSignExe(getExecString());
		}
		if(saveTsk) {
			actionSave(true);
		   // Съобщение, че дефиницията е подписана 
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(BEANMESSAGES, SIGNSAVEMSG));
		}
		
	}
	
	/**
	 * Изпълнение на задача
	 * Премахване на подписа и запис на задача
	 */
	public void actionExecRemoveSigned() {	
		setExecSigned(false);
		getExecString();
		getTask().setSignExe(null);
		actionSave(true);
		// Съобщение, че подписът е премахнат
		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(BEANMESSAGES, SIGNDELMSG));
	}
	
	
	public boolean isViewSignBtn() {	
		return (Boolean) getStateHelper().eval(PropertyKeys.VIEWSIGN, false);
	}

	public void setViewSignBtn(boolean viewSignBtn) {
		getStateHelper().put(PropertyKeys.VIEWSIGN, viewSignBtn);
	}

}
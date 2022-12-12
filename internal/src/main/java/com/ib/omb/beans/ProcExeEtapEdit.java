package com.ib.omb.beans;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.navigation.Navigation;
import com.ib.indexui.navigation.NavigationData;
import com.ib.indexui.navigation.NavigationDataHolder;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.components.TaskData;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.LockObjectDAO;
import com.ib.omb.db.dao.ProcDefEtapDAO;
import com.ib.omb.db.dao.ProcExeDAO;
import com.ib.omb.db.dao.ProcExeEtapDAO;
import com.ib.omb.db.dto.ProcDefEtap;
import com.ib.omb.db.dto.ProcDefTask;
import com.ib.omb.db.dto.ProcExe;
import com.ib.omb.db.dto.ProcExeEtap;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

@Named
@ViewScoped
public class ProcExeEtapEdit extends IndexUIbean  implements Serializable {	
	
	
	/**
	 * Въвеждане / актуализация на изпълнение на етап
	 * 
	 */
	private static final long serialVersionUID = 7703573998051116438L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcExeEtapEdit.class);
	
	private static final String ID_OBJ = "idObj";
	
	private transient ProcExeEtapDAO etapDAO;
	
	private Date decodeDate = new Date();
	
	private ProcExe procExe;
	private ProcExeEtap etapExe;
	private ProcDefEtap etapDef;
	
	private Integer codeRefExeEtap;
	
	private List<ProcDefTask> defTasksList;
	private List<ProcDefTask> selDefTasksList;
	private List<Object[]> regTasksList;
	
	private Map<Integer, List<Integer>> realIzpList; 
	private List<Integer> oprIzpList;
	private List<SystemClassif>	selOprIzpList;
	
	private boolean nextOk;
	private boolean nextNot;
	private boolean nextOpt;
	private boolean activateEtap;
	
	private List<Object[]> selNextOkList;
	private List<Object[]> selNextNotList;
	private List<Object[]> selNextOptList;
	
	private String numEtap;
	private boolean activeStatus;
	private String docRnFull;
	
	private boolean editEtapExe;
	
	private int unlockObj;
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct!!!");
		
		try {
			
			boolean fLockOk = true;
			
			this.etapDAO = new ProcExeEtapDAO(getUserData());
			this.procExe = new ProcExe();
			this.etapExe = new ProcExeEtap();
			this.etapDef = new ProcDefEtap();
			this.defTasksList = new ArrayList<>();
			this.selDefTasksList = new ArrayList<>();
			this.regTasksList = new ArrayList<>();
			this.selOprIzpList = new ArrayList<>();
			this.realIzpList = new LinkedHashMap<>();
			this.nextOk = false;
			this.nextNot = false;
			this.nextOpt = false;
			this.activateEtap = false;
			this.selNextOkList = new ArrayList<>();		
			this.selNextNotList = new ArrayList<>();		
			this.selNextOptList = new ArrayList<>();
			this.unlockObj = 0;

			if (JSFUtils.getRequestParameter(ID_OBJ) != null && !"".equals(JSFUtils.getRequestParameter(ID_OBJ))) {

				Integer idObj = Integer.valueOf(JSFUtils.getRequestParameter(ID_OBJ));
				
				if(JSFUtils.getFlashScopeValue("unlock") != null){
					this.unlockObj = (int) JSFUtils.getFlashScopeValue("unlock");
				}

				if (idObj != null) {

					JPA.getUtil().runWithClose(() -> {

						this.etapExe = this.etapDAO.findById(idObj, true);
						
						this.procExe = new ProcExeDAO(getUserData()).findById(this.etapExe.getExeId());
						
						this.etapDef = new ProcDefEtapDAO(getUserData()).findById(SearchUtils.asInteger(this.etapExe.getDefEtapData()[0]), true); 
						
						this.defTasksList = this.etapDAO.selectDefTaskList(this.procExe, this.etapExe.getNomer(), getSystemData());
						
						this.regTasksList = this.etapDAO.selectRegTaskList(this.etapExe.getId());

					});
					
					// проверка за заключена процедура
					fLockOk = checkForLock(this.procExe.getId());
					
					if (fLockOk) {
						// проверка за достъп
						lockProc(this.procExe.getId());
						// отключване на всички обекти за потребителя(userId) и заключване на процедура, за да не може да се актуализира от друг
					}
					
					this.codeRefExeEtap = this.etapExe.getCodeRef();					
					
					for (ProcDefTask defTask : defTasksList) {	
						this.selDefTasksList.add(defTask);
						
						this.realIzpList.put(defTask.getId(), defTask.getRealIzpCodes());
					}	
					
					actionCheckTaskIsActive(this.regTasksList);	
					
					if (this.etapExe.getDocId() != null) {
						Object[] docData = new DocDAO(getUserData()).findDocData(this.etapExe.getDocId());
						this.docRnFull = DocDAO.formRnDocDate(docData[1], docData[2], docData[5]) + " г.";
					}
					
					if (this.etapExe.getCodeRef() == null || (getUserData(UserData.class).getUserAccess().equals(this.etapExe.getCodeRef())
							|| getUserData().getUserId().equals(this.procExe.getCodeRef())
							|| getUserData().hasAccess(OmbConstants.CODE_CLASSIF_BUSINESS_ROLE, OmbConstants.CODE_ZNACHENIE_BUSINESS_ROLE_PROC_ADMIN))) {						
									
						this.editEtapExe = true;
					
					} else {
						this.editEtapExe = false;
					}
				}
			}

		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на изпълнение на етап! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}		
	}
	
	public void actionCheckTaskIsActive(List<Object[]> tasksList) {
		
		try {			
			
			List<Object[]> taskListNotActiveStatus = new ArrayList<>();
			
			if (tasksList.isEmpty()) {
				this.activeStatus = false;
			
			} else {
				for (Object[] task : tasksList) {
					
					if (!getSystemData().matchClassifItems(OmbConstants.CODE_CLASSIF_TASK_STATUS_ACTIVE, SearchUtils.asInteger(task[7]), SearchUtils.asDate(task[8]))) {
						taskListNotActiveStatus.add(task);							
					}
				}
			}
			
			if (tasksList.size() == taskListNotActiveStatus.size()) {
				this.activeStatus = false;
				
				actionSelectNextOk();
				
			} else {
				this.activeStatus = true;

				actionSelectActivate();
			}
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при проверка на активния статус на задачите към етап! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}	
		
		
	}
	
	public void actionLoadRealIzp(Integer id, Integer idx) {
		
		try {
			
			List<SystemClassif> tmpLst = new ArrayList<>();
			
			if (!this.realIzpList.isEmpty()) {
				for (Entry<Integer, List<Integer>> entry : this.realIzpList.entrySet()) {
					if (entry.getKey().equals(id)) {
						
						for (Integer izp : entry.getValue()) {
							
							SystemClassif scItem = new SystemClassif();
							
							scItem.setCodeClassif(OmbConstants.CODE_CLASSIF_ADMIN_STR);
							scItem.setCode(izp);
							scItem.setTekst(getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, izp, getUserData().getCurrentLang(), new Date()));
							
							tmpLst.add(scItem);
						}								
					}
					
					setOprIzpList(entry.getValue()); 
				}				
			}
			
			setSelOprIzpList(tmpLst); 
			
			PrimeFaces.current().executeScript("$('#formProcExeEtap\\\\:tblDefTasksList\\\\:" + idx + "\\\\:manyIzpalList\\\\:dialogButtonM').trigger('click')");
			
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните за определените изпълнители за задача! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}		
		
	}
	
	public void actionSelectIzpal(Integer id) {
		
		if (this.selOprIzpList != null) {

			this.oprIzpList.clear();

			for (SystemClassif sc : this.selOprIzpList) {

				this.oprIzpList.add(sc.getCode());
			}
		}		
	}
	
	public void actionChangeCodeRef() {
		
		if (this.codeRefExeEtap == null) {
			JSFUtils.addMessage("formProcExeEtap:selContrEtap:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefEdit.controlEtap")));
			return;
		
		} else {
			
			try {
				
				JPA.getUtil().runInTransaction(() -> this.etapExe = this.etapDAO.changeCodeRef(this.procExe, this.etapExe, this.codeRefExeEtap, getSystemData()));
			
				String dialogWidgetVar = "PF('modalSelContrEtap').hide();";
				PrimeFaces.current().executeScript(dialogWidgetVar);
				
				JPA.getUtil().runWithClose(() -> {
					
					this.defTasksList = this.etapDAO.selectDefTaskList(this.procExe, this.etapExe.getNomer(), getSystemData());
					
					this.regTasksList = this.etapDAO.selectRegTaskList(this.etapExe.getId());

				});
				
			} catch (BaseException e) {			
				LOGGER.error("Грешка при промяна на контролиращ на етапа! ", e);	
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}			
		}		
	}
	
	public void actionRegEtapTasks() {
		
		try {
			
			boolean err = false;
			if (!this.defTasksList.isEmpty()) {
				
				for (ProcDefTask defTask : defTasksList) {
					if (defTask.getRealIzpCodes().isEmpty()) {
						err = true;
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procExeEtapEdit.taskBezIzpal"));
						PrimeFaces.current().executeScript("scrollToErrors()");
						return;
					}
				}				
			}
			
			if (!err) {
				JPA.getUtil().runInTransaction(() -> {
					
				this.etapExe = this.etapDAO.restartEtap(this.procExe, this.etapExe, this.defTasksList, getSystemData());
				this.regTasksList = this.etapDAO.selectRegTaskList(this.etapExe.getId());
				
				actionCheckTaskIsActive(this.regTasksList);
				
				});
			}
			
			
		} catch (BaseException e) {			
			LOGGER.error("Грешка при регистриране на задача! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	
	}
	
	public void actionSelectNextOk() {
		
		this.nextOk = true;
		this.selNextOkList = new ArrayList<>();				
		
		for (Object[] ok : this.etapDef.getNextOkList()) {
			this.selNextOkList.add(ok);
		}
		
		this.nextNot = false;
		this.nextOpt = false;
		this.activateEtap = false;			
		this.selNextNotList = new ArrayList<>();		
		this.selNextOptList = new ArrayList<>();
	}
	
	public void actionSelectNextNot() {
		
		this.nextNot = true;
		this.selNextNotList = new ArrayList<>();
		
		for (Object[] not : this.etapDef.getNextNotList()) {
			this.selNextNotList.add(not);
		}		
		
		this.nextOk = false;
		this.nextOpt = false;
		this.activateEtap = false;
		this.selNextOkList = new ArrayList<>();			
		this.selNextOptList = new ArrayList<>();
	}
	
	public void actionSelectNextOpt() {
		
		this.nextOpt = true;
		this.selNextOptList = new ArrayList<>();
		
		for (Object[] opt : this.etapDef.getNextOptionalList()) {
			this.selNextOptList.add(opt);
		}
		
		this.nextOk = false;
		this.nextNot = false;
		this.activateEtap = false;
		this.selNextOkList = new ArrayList<>();		
		this.selNextNotList = new ArrayList<>();
	}
	
	public void actionSelectActivate() {
		
		this.activateEtap = true;
		this.nextOk = false;
		this.nextNot = false;
		this.nextOpt = false;
		
		this.selNextOkList = new ArrayList<>();		
		this.selNextNotList = new ArrayList<>();		
		this.selNextOptList = new ArrayList<>();
	}
	
	public void actionCompleteEtap() {
		
		try {
			
			this.numEtap = null;
			SimpleDateFormat sdfh = new SimpleDateFormat("dd.MM.yyyy HH:mm");
			StringBuilder comments = new StringBuilder();
			SystemClassif empl = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_ADMIN_STR, getUserData().getUserId(), getCurrentLang(), new Date(), false);
			
			if (nextOk) {
				
				if (this.selNextOkList.size() == this.etapDef.getNextOkList().size()) {
					int i = 0;
					String numAndName = "";
					
					for (Object[] next : this.selNextOkList) {
						
						if (this.selNextOkList.size() == 1) {						
							comments.append("За следващ етап е определен № " + SearchUtils.asString(next[1]) + " " + SearchUtils.asString(next[2]));
							
						} else {
							
							if (i == 0) {
								comments.append("За следващи етапи са определени ");
							}							
							numAndName += "№ " + SearchUtils.asString(next[1]) + " " + SearchUtils.asString(next[2]) + ", ";
						}						
						i++;
					}
					
					if (this.selNextOkList.size() > 1) {	
						comments.append(numAndName.substring(0, numAndName.lastIndexOf(",")));
					}
					
					if (empl != null) {
						comments.append(". Решението е взето на " + sdfh.format(this.decodeDate) + " от " + empl.getTekst());
						comments.append(" от " + getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, empl.getCodeParent(), getCurrentLang(), new Date()) + ".");				
					}
					
					JPA.getUtil().runInTransaction(() -> this.etapExe = this.etapDAO.completeEtap(this.etapExe, (SystemData) getSystemData(), true, this.numEtap, comments.toString(), null));
				
				} else {
					
					this.numEtap = null;
					int i = 0;
					String numAndName = "";
					
					for (Object[] next : this.selNextOkList) {
						
						this.numEtap = SearchUtils.asString(next[1]) + ",";
						
						if (this.selNextOkList.size() == 1) {						
							comments.append("За следващ етап е определен № " + SearchUtils.asString(next[1]) + " " + SearchUtils.asString(next[2]));
							
						} else {
							
							if (i == 0) {
								comments.append("За следващи етапи са определени ");
							}							
							numAndName += "№ " + SearchUtils.asString(next[1]) + " " + SearchUtils.asString(next[2]) + ", ";
						}						
						i++;
					}
					
					this.numEtap = this.numEtap.substring(0, this.numEtap.lastIndexOf(","));
					if (this.selNextOkList.size() > 1) {	
						comments.append(numAndName.substring(0, numAndName.lastIndexOf(",")));
					}
					
					if (empl != null) {
						comments.append(". Решението е взето на " + sdfh.format(this.decodeDate) + " от " + empl.getTekst());
						comments.append(" от " + getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, empl.getCodeParent(), getCurrentLang(), new Date()) + ".");				
					}
					
					JPA.getUtil().runInTransaction(() -> this.etapExe = this.etapDAO.completeEtap(this.etapExe, (SystemData) getSystemData(), null, this.numEtap, comments.toString(), null));
				}
			}			
			
			if (nextNot) {
				
				if (this.selNextNotList.size() == this.etapDef.getNextNotList().size()) {
					int i = 0;
					String numAndName = "";
					
					for (Object[] next : this.selNextNotList) {
						
						if (this.selNextNotList.size() == 1) {						
							comments.append("За следващ етап е определен № " + SearchUtils.asString(next[1]) + " " + SearchUtils.asString(next[2]));
							
						} else {
							
							if (i == 0) {
								comments.append("За следващи етапи са определени ");
							}							
							numAndName += "№ " + SearchUtils.asString(next[1]) + " " + SearchUtils.asString(next[2]) + ", ";
						}						
						i++;
					}
					
					if (this.selNextNotList.size() > 1) {
						comments.append(numAndName.substring(0, numAndName.lastIndexOf(",")));
					}
					
					if (empl != null) {
						comments.append(". Решението е взето на " + sdfh.format(this.decodeDate) + " от " + empl.getTekst());
						comments.append(" от " + getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, empl.getCodeParent(), getCurrentLang(), new Date()) + ".");				
					}
					
					JPA.getUtil().runInTransaction(() -> this.etapExe = this.etapDAO.completeEtap(this.etapExe, (SystemData) getSystemData(), false, this.numEtap, comments.toString(), null));
				
				} else {
					
					this.numEtap = null;
					int i = 0;
					String numAndName = "";
					
					for (Object[] next : this.selNextNotList) {
						
						this.numEtap = SearchUtils.asString(next[1]) + ",";
						
						if (this.selNextNotList.size() == 1) {						
							comments.append("За следващ етап е определен № " + SearchUtils.asString(next[1]) + " " + SearchUtils.asString(next[2]));
							
						} else {
							
							if (i == 0) {
								comments.append("За следващи етапи са определени ");
							}							
							numAndName += "№ " + SearchUtils.asString(next[1]) + " " + SearchUtils.asString(next[2]) + ", ";
						}						
						i++;
					}					
					
					this.numEtap = this.numEtap.substring(0, this.numEtap.lastIndexOf(","));
					if (this.selNextNotList.size() > 1) {
						comments.append(numAndName.substring(0, numAndName.lastIndexOf(",")));
					}
					
					if (empl != null) {
						comments.append(". Решението е взето на " + sdfh.format(this.decodeDate) + " от " + empl.getTekst());
						comments.append(" от " + getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, empl.getCodeParent(), getCurrentLang(), new Date()) + ".");				
					}
					
					JPA.getUtil().runInTransaction(() -> this.etapExe = this.etapDAO.completeEtap(this.etapExe, (SystemData) getSystemData(), null, this.numEtap, comments.toString(), null));
				}
			}
			
			if (nextOpt) {
				
				if (this.selNextOptList.size() == this.etapDef.getNextOptionalList().size()) {
					int i = 0;
					String numAndName = ""; 
					
					for (Object[] next : this.selNextOptList) {
						
						if (this.selNextOptList.size() == 1) {						
							comments.append("За следващ етап е определен № " + SearchUtils.asString(next[1]) + " " + SearchUtils.asString(next[2]));
							
						} else {
							
							if (i == 0) {
								comments.append("За следващи етапи са определени ");
							}							
							numAndName += "№ " + SearchUtils.asString(next[1]) + " " + SearchUtils.asString(next[2]) + ", ";
						}						
						i++;
					}
					
					if (this.selNextOptList.size() > 1) {	
						comments.append(numAndName.substring(0, numAndName.lastIndexOf(",")));
					}
					
					if (empl != null) {
						comments.append(". Решението е взето на " + sdfh.format(this.decodeDate) + " от " + empl.getTekst());
						comments.append(" от " + getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, empl.getCodeParent(), getCurrentLang(), new Date()) + ".");				
					}
					
					JPA.getUtil().runInTransaction(() -> this.etapExe = this.etapDAO.completeEtap(this.etapExe, (SystemData) getSystemData(), null, this.etapDef.getNextOptional(), comments.toString(), null));
				
				} else {
					
					this.numEtap = null;
					int i = 0;
					String numAndName = "";
					
					for (Object[] next : this.selNextOptList) {
						
						this.numEtap = SearchUtils.asString(next[1]) + ",";
						
						if (this.selNextOptList.size() == 1) {						
							comments.append("За следващ етап е определен № " + SearchUtils.asString(next[1]) + " " + SearchUtils.asString(next[2]));
							
						} else {
							
							if (i == 0) {
								comments.append("За следващи етапи са определени ");
							}							
							numAndName += "№ " + SearchUtils.asString(next[1]) + " " + SearchUtils.asString(next[2]) + ", ";
						}						
						i++;
					}					
					
					this.numEtap = this.numEtap.substring(0, this.numEtap.lastIndexOf(","));
					if (this.selNextOptList.size() > 1) {
						comments.append(numAndName.substring(0, numAndName.lastIndexOf(",")));
					}
					
					if (empl != null) {
						comments.append(". Решението е взето на " + sdfh.format(this.decodeDate) + " от " + empl.getTekst());
						comments.append(" от " + getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, empl.getCodeParent(), getCurrentLang(), new Date()) + ".");				
					}
					
					JPA.getUtil().runInTransaction(() -> this.etapExe = this.etapDAO.completeEtap(this.etapExe, (SystemData) getSystemData(), null, this.numEtap, comments.toString(), null));
				}
			}
			
			if (activateEtap) {
				
				if (empl != null) {					
					comments.append("Текущият етап е активиран отново. Решението е взето на " + sdfh.format(this.decodeDate) + " от " + empl.getTekst());
					comments.append(" от " + getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, empl.getCodeParent(), getCurrentLang(), new Date()) + ".");
				}
				
				JPA.getUtil().runInTransaction(() -> this.etapExe = this.etapDAO.activateDecisionEtap(this.etapExe, comments.toString()));				
			}
			
			Navigation navHolder = new Navigation();			
		    int i = navHolder.getNavPath().size();	
		   
		    NavigationDataHolder dataHoslder = (NavigationDataHolder) JSFUtils.getManagedBean("navigationSessionDataHolder");
		    Stack<NavigationData> stackPath = dataHoslder.getPageList();
		    NavigationData nd = stackPath.get(i-2);
		    Map<String, Object> mapV = nd.getViewMap();
			
		    ProcExeEdit procExeEdit = (ProcExeEdit) mapV.get("procExeEdit");
		    JPA.getUtil().runWithClose(() ->  this.procExe = new ProcExeDAO(getUserData()).findById(this.etapExe.getExeId()));
		    procExeEdit.setProcExe(this.procExe);
		    procExeEdit.actionSearchEtapExeList();
			
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на активиране на текущия етап! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}			
	}
	
	/******************************************************* GET & SET *******************************************************/	
	
	public Date getDecodeDate() {
		return new Date(decodeDate.getTime()) ;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
	}

	public ProcExe getProcExe() {
		return procExe;
	}

	public void setProcExe(ProcExe procExe) {
		this.procExe = procExe;
	}

	public ProcExeEtap getEtapExe() {
		return etapExe;
	}

	public void setEtapExe(ProcExeEtap etapExe) {
		this.etapExe = etapExe;
	}

	public ProcDefEtap getEtapDef() {
		return etapDef;
	}

	public void setEtapDef(ProcDefEtap etapDef) {
		this.etapDef = etapDef;
	}

	public Integer getCodeRefExeEtap() {
		return codeRefExeEtap;
	}

	public void setCodeRefExeEtap(Integer codeRefExeEtap) {
		this.codeRefExeEtap = codeRefExeEtap;
	}

	public List<ProcDefTask> getDefTasksList() {
		return defTasksList;
	}

	public void setDefTasksList(List<ProcDefTask> defTasksList) {
		this.defTasksList = defTasksList;
	}

	public List<ProcDefTask> getSelDefTasksList() {
		return selDefTasksList;
	}

	public void setSelDefTasksList(List<ProcDefTask> selDefTasksList) {
		this.selDefTasksList = selDefTasksList;
	}

	public List<Object[]> getRegTasksList() {
		return regTasksList;
	}

	public void setRegTasksList(List<Object[]> regTasksList) {
		this.regTasksList = regTasksList;
	}

	public Map<Integer, List<Integer>> getRealIzpList() {
		return realIzpList;
	}

	public void setRealIzpList(Map<Integer, List<Integer>> realIzpList) {
		this.realIzpList = realIzpList;
	}

	public List<Integer> getOprIzpList() {
		return oprIzpList;
	}

	public void setOprIzpList(List<Integer> oprIzpList) {
		this.oprIzpList = oprIzpList;
	}

	public List<SystemClassif> getSelOprIzpList() {
		return selOprIzpList;
	}

	public void setSelOprIzpList(List<SystemClassif> selOprIzpList) {
		this.selOprIzpList = selOprIzpList;
	}

	public boolean isNextOk() {
		return nextOk;
	}

	public void setNextOk(boolean nextOk) {
		this.nextOk = nextOk;
	}

	public boolean isNextNot() {
		return nextNot;
	}

	public void setNextNot(boolean nextNot) {
		this.nextNot = nextNot;
	}

	public boolean isNextOpt() {
		return nextOpt;
	}

	public void setNextOpt(boolean nextOpt) {
		this.nextOpt = nextOpt;
	}

	public boolean isActivateEtap() {
		return activateEtap;
	}

	public void setActivateEtap(boolean activateEtap) {
		this.activateEtap = activateEtap;
	}

	public List<Object[]> getSelNextOkList() {
		return selNextOkList;
	}

	public void setSelNextOkList(List<Object[]> selNextOkList) {
		this.selNextOkList = selNextOkList;
	}

	public List<Object[]> getSelNextNotList() {
		return selNextNotList;
	}

	public void setSelNextNotList(List<Object[]> selNextNotList) {
		this.selNextNotList = selNextNotList;
	}

	public List<Object[]> getSelNextOptList() {
		return selNextOptList;
	}

	public void setSelNextOptList(List<Object[]> selNextOptList) {
		this.selNextOptList = selNextOptList;
	}

	public String getNumEtap() {
		return numEtap;
	}

	public void setNumEtap(String numEtap) {
		this.numEtap = numEtap;
	}
	
	public boolean isActiveStatus() {
		return activeStatus;
	}

	public void setActiveStatus(boolean activeStatus) {
		this.activeStatus = activeStatus;
	}
	
	public String getDocRnFull() {
		return docRnFull;
	}

	public void setDocRnFull(String docRnFull) {
		this.docRnFull = docRnFull;
	}
	
	/**************************************************** END GET & SET ****************************************************/	
	
	/**роси**/
	/**
	 * ид на документа, към който е процедурата
	 */
	
	
	private boolean taskNew;
	private boolean showTaskEdit;
	private Integer idTask;
	private boolean viewTask;
	
	/**
	 * Нова задача към етап
	 */
	public void actionNewTaskEtap() {
		setTaskNew(true); 
		setShowTaskEdit(true);
		idTask = null;
	}

	/**
	 * корекция на задача към етап
	 */
	public void actionEditTaskEtap(Integer idT) {
		setTaskNew(false); 
		setShowTaskEdit(true);
		if (this.etapExe.getStatus() == OmbConstants.CODE_ZNACHENIE_ETAP_STAT_EXE ||this.etapExe.getStatus() == OmbConstants.CODE_ZNACHENIE_ETAP_STAT_DECISION) {
			setViewTask(false);
		} else {
			setViewTask(true);
		}
		idTask = idT;
	}
	
	/**
	 * при затваряне на модалия диалог със здачата
	 */
	public void actionCancelPanelTask() {
		setTaskNew(false);
		setShowTaskEdit(false);
		
//		actionCheckTaskIsActive(this.regTasksList);
	}

	/**
	 * да се запише връзката етап за нова задача
	 * да обнови списъка със задачи към етапа
	 */
	public void actionEtapTaskUpdate() {		
		try { 
			if(taskNew && idTask != null) {
				// 	само за нова задача да се направи връзка
				String info = "Процедура №" + procExe.getId() + " " + procExe.getProcName() + ", Етап №" + etapExe.getNomer() + " " + etapExe.getEtapName();

				JPA.getUtil().runInTransaction(() ->  
					  etapDAO.connectTaskToEtap(idTask,  etapExe.getId(), info)
				  );
				setTaskNew(false);

				// да обнови информацията в компонентата, ако е отворена
				TaskData compTask =	((TaskData)FacesContext.getCurrentInstance().getViewRoot().findComponent("formProcExeEtap").findComponent("formProcExeEtap:compTaskId"));
				if(compTask != null && compTask.getTask() != null) {
					compTask.getTask().setProcInfo(info);
				}

			}
			//Да се обнови списъка
			JPA.getUtil().runWithClose(() -> {
				this.regTasksList = this.etapDAO.selectRegTaskList(this.etapExe.getId());
				
				this.etapExe = this.etapDAO.findById(this.etapExe.getId(), true);
				
				this.procExe = new ProcExeDAO(getUserData()).findById(this.etapExe.getExeId());
			});
						
			actionCheckTaskIsActive(this.regTasksList);
			
			Navigation navHolder = new Navigation();			
		    int i = navHolder.getNavPath().size();	
		   
		    NavigationDataHolder dataHoslder = (NavigationDataHolder) JSFUtils.getManagedBean("navigationSessionDataHolder");
		    Stack<NavigationData> stackPath = dataHoslder.getPageList();
		    NavigationData nd = stackPath.get(i-2);
		    Map<String, Object> mapV = nd.getViewMap();
			
		    ProcExeEdit procExeEdit = (ProcExeEdit) mapV.get("procExeEdit");		    
		    procExeEdit.setProcExe(this.procExe);
		    if (this.procExe.getStatus() == OmbConstants.CODE_ZNACHENIE_PROC_STAT_STOP) {
		    	procExeEdit.setViewBtnRestoreProc(true);
		    	procExeEdit.setViewBtnStopProc(false);
		    }
		    procExeEdit.actionSearchEtapExeList();		    
			
		} catch (BaseException e) {
			LOGGER.error("Грешка при запис на задача към етап! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}		
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
			
			if(this.unlockObj != 1 ){ 
	        	unlockAll(true);
	        	((UserData) getUserData()).setPreviousPage(null);
			}
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
	

	public Integer getIdTask() {
		return idTask;
	}

	public void setIdTask(Integer idTask) {
		this.idTask = idTask;
	}

	public boolean isShowTaskEdit() {
		return showTaskEdit;
	}

	public void setShowTaskEdit(boolean showTaskEdit) {
		this.showTaskEdit = showTaskEdit;
	}

	public boolean isTaskNew() {
		return taskNew;
	}

	public void setTaskNew(boolean taskNew) {
		this.taskNew = taskNew;
	}

	public boolean isViewTask() {
		return viewTask;
	}

	public void setViewTask(boolean viewTask) {
		this.viewTask = viewTask;
	}

	public boolean isEditEtapExe() {
		return editEtapExe;
	}

	public void setEditEtapExe(boolean editEtapExe) {
		this.editEtapExe = editEtapExe;
	}

	public int getUnlockObj() {
		return unlockObj;
	}

	public void setUnlockObj(int unlockObj) {
		this.unlockObj = unlockObj;
	}
	
}
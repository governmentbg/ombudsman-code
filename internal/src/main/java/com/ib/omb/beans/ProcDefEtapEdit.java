package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.inject.Named;

import org.omnifaces.cdi.ViewScoped;
import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.navigation.Navigation;
import com.ib.indexui.navigation.NavigationData;
import com.ib.indexui.navigation.NavigationDataHolder;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.ProcDefDAO;
import com.ib.omb.db.dao.ProcDefEtapDAO;
import com.ib.omb.db.dao.ProcDefTaskDAO;
import com.ib.omb.db.dto.ProcDef;
import com.ib.omb.db.dto.ProcDefEtap;
import com.ib.omb.db.dto.ProcDefTask;
import com.ib.omb.db.dto.ProcDefTaskIzp;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SearchUtils;

@Named
@ViewScoped
public class ProcDefEtapEdit extends IndexUIbean  implements Serializable {	
	
	/**
	 * Въвеждане / актуализация на дефиниция на процедура 
	 * 
	 */
	private static final long serialVersionUID = 1440311381608392101L;
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcDefEtapEdit.class);
	
	private static final String ID_OBJ = "idObj";
	private static final String ID_PROC = "idProc";
	private static final String FORM_PROC_DEF_ETAP = "formProcDefEtap";
	
	private Date decodeDate;
	
	private ProcDef proc;
	private ProcDefEtap defEtap;
	private List<ProcDefEtap> etapsList;
	private List<ProcDefTask> defTasksList;
	private ProcDefTask defTask; 
	
	private transient ProcDefDAO procDAO;
	private transient ProcDefEtapDAO etapDAO;
	private transient ProcDefTaskDAO taskDAO;
	
	private boolean showDataForTask;
	private boolean hideSrokHours;
	private int nextNomerEtap;	
	
	private List<SystemClassif> selNextOk;
	private List<SystemClassif> selNextNot;
	private List<SystemClassif> selNextOpt;
	
	private Integer oldNumberEtap;
	
	private List<SelectItem> emplPositionList;
	private List<SelectItem> businessRoleList;
	
	private List<SystemClassif> opinionList;
	
	private List<SystemClassif> opinionNextOk;
	private List<SystemClassif> opinionNextNot;
	private List<SystemClassif> opinionNextOpt;
	
	private List<Integer> selOpinion;
	private boolean showOpinion;
	private boolean fromEditTask;
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct!!!");
		
		try {
		
			this.decodeDate = new Date();
			this.proc = new ProcDef();
			this.procDAO = new ProcDefDAO(getUserData());
			this.etapDAO = new ProcDefEtapDAO(getUserData());
			this.taskDAO = new ProcDefTaskDAO(getUserData());			
			
			this.defEtap = new ProcDefEtap();
			this.etapsList = new ArrayList<>();
			this.defTasksList = new ArrayList<>();
			this.defTask = new ProcDefTask();
			this.showDataForTask = false;
			this.hideSrokHours = false;
			this.nextNomerEtap = 0;
			
			this.selNextOk = new ArrayList<>();
			this.selNextNot = new ArrayList<>();
			this.selNextOpt = new ArrayList<>();
			
			this.emplPositionList = new ArrayList<>();
			this.businessRoleList = new ArrayList<>();
			
			List<SystemClassif> itemsEmplPos = getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_POSITION, new Date(), getCurrentLang());
			List<SystemClassif> itemsBusRole = getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_PROC_BUSINESS_ROLE, new Date(), getCurrentLang());
						
			for (SystemClassif item : itemsEmplPos) {
				this.emplPositionList.add(new SelectItem(item.getCode(), item.getTekst()));				
			}
			
			Collections.sort(this.emplPositionList, compatator);
			
			for (SystemClassif item : itemsBusRole) {
				this.businessRoleList.add(new SelectItem(item.getCode(), item.getTekst()));				
			}
			
			Collections.sort(this.businessRoleList, compatator);			
						
			if (JSFUtils.getRequestParameter(ID_PROC) != null && !"".equals(JSFUtils.getRequestParameter(ID_PROC))) {					
				
				JPA.getUtil().runWithClose(() -> {
					
					this.proc = this.procDAO.findById(Integer.valueOf(JSFUtils.getRequestParameter(ID_PROC)));
					
					this.etapsList = this.procDAO.selectDefEtapList(this.proc.getId(), null);
				}); 
				
				for (ProcDefEtap etap : this.etapsList) {					
					this.nextNomerEtap = etap.getNomer();
				}
			} 	
			
			if (proc.getWorkDaysOnly().equals(SearchUtils.asInteger(OmbConstants.CODE_ZNACHENIE_DA))){
				this.hideSrokHours = true;
			}			
			
			if (JSFUtils.getRequestParameter(ID_OBJ) != null && !"".equals(JSFUtils.getRequestParameter(ID_OBJ))) {
				
				Integer idObj = Integer.valueOf(JSFUtils.getRequestParameter(ID_OBJ));	
				
				if (idObj != null) {
					
					JPA.getUtil().runWithClose(() -> {
						
						this.defEtap = this.etapDAO.findById(idObj);
						
						this.defTasksList = this.etapDAO.selectDefTaskList(idObj);
						
					});
					
					this.oldNumberEtap = this.defEtap.getNomer();
					
					if (this.defEtap.getNextOk() != null && !this.defEtap.getNextOk().isEmpty()) {
						
						String[] nextOk = this.defEtap.getNextOk().split(",");
						
						for (String str : nextOk) {	
							for (ProcDefEtap etap : this.etapsList) {													
							
								SystemClassif tmp = new SystemClassif();
								
								if (str.equals(SearchUtils.asString(etap.getNomer()))) {
									tmp.setCode(etap.getNomer());
									tmp.setTekst(etap.getEtapName());
									
									this.selNextOk.add(tmp);
								}
							}														
						}						
					}
					
					if (this.defEtap.getNextNot() != null && !this.defEtap.getNextNot().isEmpty()) {
						
						String[] nextNot = this.defEtap.getNextNot().split(",");
								
							for (String str : nextNot) {								
								for (ProcDefEtap etap : this.etapsList) {
								
									SystemClassif tmp = new SystemClassif();
								
								if (str.equals(SearchUtils.asString(etap.getNomer()))) {
									tmp.setCode(etap.getNomer());
									tmp.setTekst(etap.getEtapName());
									
									this.selNextNot.add(tmp);
								}	
							}														
						}						
					}
					
					if (this.defEtap.getNextOptional() != null && !this.defEtap.getNextOptional().isEmpty()) {
						
						String[] nextOpt = this.defEtap.getNextOptional().split(",");
						
						for (String str : nextOpt) {
							for (ProcDefEtap etap : this.etapsList) {								
							
								SystemClassif tmp = new SystemClassif();
								
								if (str.equals(SearchUtils.asString(etap.getNomer()))) {
									tmp.setCode(etap.getNomer());
									tmp.setTekst(etap.getEtapName());
									
									this.selNextOpt.add(tmp);
								}
							}														
						}						
					}
				}				
			
			} else {
				
				actionNewEtap();
			} 
			
			this.showDataForTask = false;
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на дефиниция на процедура ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}	
	
	/******************************************************* ETAP ***********************************************************/	
	
	public void actionNewEtap() {			
		
		this.defEtap = new ProcDefEtap();
		this.defEtap.setDefId(this.proc.getId());		
		this.defEtap.setIsMerge(OmbConstants.CODE_ZNACHENIE_NE);
		this.defEtap.setNomer(this.nextNomerEtap + 1);	
		
		this.defTasksList = new ArrayList<>();
		this.defTask = new ProcDefTask();			
		
		this.selNextOk = new ArrayList<>();
		this.selNextNot = new ArrayList<>();
		this.selNextOpt = new ArrayList<>();
	}
	
	public void actionEditEtap(Integer idObj) {	
		
		try {
			
			if (idObj != null) {		
				
				JPA.getUtil().runWithClose(() -> {
					
					this.defEtap = this.etapDAO.findById(idObj);
					
					this.defTasksList = this.etapDAO.selectDefTaskList(idObj);
					
				});				
			}
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на етап! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	public boolean actionCheckForEtapNomer() {
		
		if (this.defEtap.getNomer() != null) {			
			
			try {
				
				boolean existNomer = false;
				
				if (!this.etapsList.isEmpty()) {
					for (ProcDefEtap etap : etapsList) {
						if (this.defEtap.getId() == null) {
							if (this.defEtap.getNomer().equals(etap.getNomer())) {
								existNomer = this.etapDAO.isEtapNomerExist(this.proc.getId(), this.defEtap.getNomer());
								break;
							}
						} else if (!this.defEtap.getId().equals(etap.getId())) {
							if (this.defEtap.getNomer().equals(etap.getNomer())) {
								existNomer = this.etapDAO.isEtapNomerExist(this.proc.getId(), this.defEtap.getNomer());
								break;
							}
						}
					}
				}
				
				if (existNomer) {
					JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":etapNomer", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "procDefEtapEdit.existNomer"));
					return true;
				}
			
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при проверка номер на етап!!", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}
		}
		
		return false; 		
	}

	private boolean checkDataForEtap() {
		
		boolean save = false;	
		
		if (this.defEtap.getNomer() == null) {
			JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":etapNomer", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefList.nomProc")));
			save = true;
		
		} else {
			
			if(actionCheckForEtapNomer()) {
				save = true;				
			}
		}
		
		if(SearchUtils.isEmpty(this.defEtap.getEtapName())) {
			JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":etapName", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefList.nameProc")));
			save = true;
		}
		
		if (this.defEtap.getIsMerge() == null) {
			JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":merge", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefEdit.isMerge")));
			save = true;
		}
		
		// Марияна поиска премахване задължителността на срока на етап - 22.03.2021 
//		if (this.hideSrokHours && this.defEtap.getSrokDays() == null) {
//			JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":etapSrokDays", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefList.srokDni")));
//			save = true;
//		}
//		
//		if (!this.hideSrokHours && this.defEtap.getSrokHours() == null && this.defEtap.getSrokDays() == null) {
//			JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":etapSrokDays", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "procDefEtapEdit.daysOrHoursIsEmpty"));
//			save = true;
//		}
		
		if(SearchUtils.isEmpty(this.defEtap.getEtapInfo())) {
			JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":etapInfo", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefList.opisProc")));
			save = true;
		}
		
		if (this.selNextOk.isEmpty() && !this.selNextNot.isEmpty()) {
			JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":nextOk", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "procDefEtapEdit.selNextEtapsNot"));
			save = true;			
		}
		
		if (this.selNextOk.isEmpty() && !this.selNextOpt.isEmpty()) {
			JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":nextOk", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "procDefEtapEdit.selNextEtapsOpt"));
		save = true;			
		}
		
		if (this.defEtap.getCodeRef() == null && this.defEtap.getZveno() == null && this.defEtap.getEmplPosition() == null && this.defEtap.getBusinessRole() == null) {		
			JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":controlEtap", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "procDefEtapEdit.contrEtap", getMessageResourceString(LABELS, "procDefEdit.particInProc")));
			save = true; 
		
		} else {
			
			if (this.defEtap.getCodeRef() == null && this.defEtap.getZveno() != null && this.defEtap.getEmplPosition() == null && this.defEtap.getBusinessRole() == null) { 
				JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":controlEtap", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "procDefEdit.choiceDlajOrBussRole"));
				save = true;			
			} 
			
			if (this.defEtap.getCodeRef() == null && this.defEtap.getZveno() == null && this.defEtap.getEmplPosition() != null) {
				JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":selectZvenoEtap:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefList.zveno")));
				save = true;
			}			
		}
		
		return save;		
	}
	
	public void actionSaveEtap() {
		
		if(checkDataForEtap()) {
			return;
		}
		
		this.defEtap.setNextOk(null);
		this.defEtap.setNextNot(null);
		this.defEtap.setNextOptional(null);
		
		try {
			
			for (SystemClassif sel : this.selNextOk) {
				
				if (this.defEtap.getNextOk() == null) {
					this.defEtap.setNextOk(SearchUtils.asString(sel.getCode()) + ",");
				} else {
					this.defEtap.setNextOk(this.defEtap.getNextOk() + SearchUtils.asString(sel.getCode()) + ",");
				}				
			}
			
			for (SystemClassif sel : this.selNextNot) {
				
				if (this.defEtap.getNextNot() == null) {
					this.defEtap.setNextNot(SearchUtils.asString(sel.getCode()) + ",");
				} else {
					this.defEtap.setNextNot(this.defEtap.getNextNot() + SearchUtils.asString(sel.getCode()) + ",");
				}
			}
			
			for (SystemClassif sel : this.selNextOpt) {

				if (this.defEtap.getNextOptional() == null) {
					this.defEtap.setNextOptional(SearchUtils.asString(sel.getCode()) + ",");
				} else {
					this.defEtap.setNextOptional(this.defEtap.getNextOptional() + SearchUtils.asString(sel.getCode()) + ",");
				}
			}
			
			if (this.defEtap.getNextOk() != null) {
				this.defEtap.setNextOk(this.defEtap.getNextOk().substring(0, this.defEtap.getNextOk().lastIndexOf(",")));
			}
			
			if (this.defEtap.getNextNot() != null) {
				this.defEtap.setNextNot(this.defEtap.getNextNot().substring(0, this.defEtap.getNextNot().lastIndexOf(",")));
			}
			
			if (this.defEtap.getNextOptional() != null) {
				this.defEtap.setNextOptional(this.defEtap.getNextOptional().substring(0, this.defEtap.getNextOptional().lastIndexOf(",")));
			}	
					
			JPA.getUtil().runInTransaction(() ->  this.defEtap = this.etapDAO.save(defEtap));
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));			
			
			if (this.oldNumberEtap != null && !this.oldNumberEtap.equals(this.defEtap.getNomer())) { 
				List<ProcDefEtap> tmpEtaps = idsForChange(this.oldNumberEtap);
				if (tmpEtaps != null) {
					for (ProcDefEtap etap : tmpEtaps) {
						JPA.getUtil().runInTransaction(() ->  this.etapDAO.save(etap));					
					}
				}				
			}
			
			JPA.getUtil().runWithClose(() -> this.etapsList = this.procDAO.selectDefEtapList(this.proc.getId(), null)); 
			
			for (ProcDefEtap etap : this.etapsList) {					
				this.nextNomerEtap = etap.getNomer();
			}			
			
			Navigation navHolder = new Navigation();			
		    int i = navHolder.getNavPath().size();	
		   
		    NavigationDataHolder dataHoslder = (NavigationDataHolder) JSFUtils.getManagedBean("navigationSessionDataHolder");
		    Stack<NavigationData> stackPath = dataHoslder.getPageList();
		    NavigationData nd = stackPath.get(i-2);
		    Map<String, Object> mapV = nd.getViewMap();
			
		    ProcDefEdit procEdit = (ProcDefEdit) mapV.get("procDefEdit");	
			procEdit.setDefEtapsList(this.etapsList); 			

			actionNewTask();
			
			checkEtapTaskSrok(this.defEtap);
		    
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на дефиниция на процедура! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	private List<ProcDefEtap> idsForChange(Integer oldNumber) {
		
		List<ProcDefEtap> etaps = new ArrayList<>();
		
		String etapsNumber = checkExistEtapInConn(oldNumber);
		
		if (etapsNumber != null && !etapsNumber.isEmpty()) {
			String[] number = etapsNumber.split(",");
			
			for (String str : number) {
				
				for (ProcDefEtap etap : this.etapsList) {
					
					if (str.equals(SearchUtils.asString(etap.getNomer()))) {
						
						String nextOK = etap.getNextOk();
						if(nextOK != null && !nextOK.isEmpty()) {
							etap.setNextOk(nextOK.replace(SearchUtils.asString(oldNumber), SearchUtils.asString(this.defEtap.getNomer()))); 
						}
						
						String nextNot = etap.getNextNot();	
						if(nextNot != null && !nextNot.isEmpty()) {
							etap.setNextNot(nextNot.replace(SearchUtils.asString(oldNumber), SearchUtils.asString(this.defEtap.getNomer()))); 
						}
						
						String nextOpt = etap.getNextOptional();
						if(nextOpt != null && !nextOpt.isEmpty()) {
							etap.setNextOptional(nextOpt.replace(SearchUtils.asString(oldNumber), SearchUtils.asString(this.defEtap.getNomer())));  
						}
						
						etaps.add(etap);
						
						break;
					}
				}
			}

		}
		 
		return etaps;
	}
	
	
	private String checkExistEtapInConn(Integer nomer) {
		
		String etapNomer = null;
		
		for (ProcDefEtap etap : this.etapsList) {
			
			if (!etap.getId().equals(this.defEtap.getId())) {					
			
				if (etap.getNextOk() != null && !etap.getNextOk().isEmpty()) {
					String[] nextOk = etap.getNextOk().split(",");
					
					for (String str : nextOk) {	
						if (str.equals(SearchUtils.asString(nomer))) {
							if (etapNomer != null && !etapNomer.contains("" + etap.getNomer() + "")) {
								etapNomer += "," +  SearchUtils.asString(etap.getNomer());
							} else if (etapNomer == null){
								etapNomer = SearchUtils.asString(etap.getNomer()); 
							}							
						}
					}
				}
				
				if (etap.getNextNot() != null && !etap.getNextNot().isEmpty()) {
					String[] nextNot = etap.getNextNot().split(",");
					
					for (String str : nextNot) {	
						if (str.equals(SearchUtils.asString(nomer))) {
							if (etapNomer != null && !etapNomer.contains("" + etap.getNomer() + "")) {
								etapNomer += "," +  SearchUtils.asString(etap.getNomer());
							} else if (etapNomer == null){
								etapNomer = SearchUtils.asString(etap.getNomer()); 
							}								
						}
					}				
				}
				
				if (etap.getNextOptional() != null && !etap.getNextOptional().isEmpty()) {
					String[] nextOpt = etap.getNextOptional().split(",");
					
					for (String str : nextOpt) {	
						if (str.equals(SearchUtils.asString(nomer))) {
							if (etapNomer != null && !etapNomer.contains("" + etap.getNomer() + "")) {
								etapNomer += "," + SearchUtils.asString(etap.getNomer());
							} else if (etapNomer == null){
								etapNomer = SearchUtils.asString(etap.getNomer()); 
							}
						}
					}
				}			
			}
		}
		 
		return etapNomer;
	}
	
	public void actionDeleteEtap() {
		
		try {	
			
			String etapsNumber = checkExistEtapInConn(this.defEtap.getNomer());
			
			if (etapsNumber != null && !etapsNumber.isEmpty()) {
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procDefEtapEdit.notDelEtap", etapsNumber));
			
			} else {
				
				JPA.getUtil().runInTransaction(() ->  this.etapDAO.delete(this.defEtap));
				
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));				
				
				JPA.getUtil().runWithClose(() -> this.etapsList = this.procDAO.selectDefEtapList(this.proc.getId(), null)); 
				
				for (ProcDefEtap etap : this.etapsList) {					
					this.nextNomerEtap = etap.getNomer();
				}	
				
				actionNewEtap();
				
				Navigation navHolder = new Navigation();			
			    int i = navHolder.getNavPath().size();	
			   
			    NavigationDataHolder dataHoslder = (NavigationDataHolder) JSFUtils.getManagedBean("navigationSessionDataHolder");
			    Stack<NavigationData> stackPath = dataHoslder.getPageList();
			    NavigationData nd = stackPath.get(i-2);
			    Map<String, Object> mapV = nd.getViewMap();
				
			    ProcDefEdit procEdit = (ProcDefEdit) mapV.get("procDefEdit");	
				procEdit.setDefEtapsList(this.etapsList); 					
			}
			
		} catch (BaseException e) {
			LOGGER.error("Грешка при изтриване на етап! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void actionSelectSlujForEtap() {
		
		try {
			
			if (this.defEtap.getCodeRef() != null) {
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.defEtap.getCodeRef(), getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (!refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL))) {				
					this.defEtap.setCodeRef(null);
					JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":selectSlujEtap:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procDefList.choiceSluj"));
				}			
			} 
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на служител за етап! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	public void actionSelectZvenoForEtap() {
		
		try {
			
			if (this.defEtap.getZveno() != null) {
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.defEtap.getZveno(), getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (!refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO))) {				
					this.defEtap.setZveno(null);
					JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":selectZvenoEtap:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procDefList.choiceZveno"));
				}			
			} 
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на звено за етап! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	/**
	 * autocomplete - търсене на значение
	 *
	 * @param query
	 * @return
	 */	
	public List<SystemClassif> actionComplete(String query) {
		
		List<SystemClassif> result = new ArrayList<>();
			
		for (ProcDefEtap etap : this.etapsList) {
			if (!this.defEtap.getNomer().equals(etap.getNomer())) {
				SystemClassif tmp = new SystemClassif();
				tmp.setCode(etap.getNomer());
				tmp.setTekst(etap.getEtapName());
				result.add(tmp);
			}			
		}
		
		return result;
	}	
	
	public void actionCompleteNext(boolean addItem, SystemClassif item, int next) throws DbErrorException { 
		
		if (next == 1) {
		
			List<SystemClassif> selClassif = this.selNextOk;
	
			if (selClassif != null && !selClassif.isEmpty()) {
				
				for (int i = 0; i < selClassif.size() - 1; i++) {
					Integer codeEtap = selClassif.get(i).getCode();
					
					if (codeEtap.equals(item.getCode()) && addItem) {
						selClassif.remove(selClassif.size() - 1); // дублира се - изтрива се последния
						break;
					}
				}			
			}
		
		} else if (next == 2) {
			
			List<SystemClassif> selClassif = this.selNextNot;

			if (selClassif != null && !selClassif.isEmpty()) {
				
				for (int i = 0; i < selClassif.size() - 1; i++) {
					Integer codeEtap = selClassif.get(i).getCode();
					
					if (codeEtap.equals(item.getCode()) && addItem) {
						selClassif.remove(selClassif.size() - 1); // дублира се - изтрива се последния
						break;
					}
				}			
			}			
		
		} else if (next == 3) {
			
			List<SystemClassif> selClassif = this.selNextOpt;

			if (selClassif != null && !selClassif.isEmpty()) {
				
				for (int i = 0; i < selClassif.size() - 1; i++) {
					Integer codeEtap = selClassif.get(i).getCode();
					
					if (codeEtap.equals(item.getCode()) && addItem) {
						selClassif.remove(selClassif.size() - 1); // дублира се - изтрива се последния
						break;
					}
				}			
			}
		}
	}	
	
	public void onItemSelectNextOk(SelectEvent<?> event) throws DbErrorException {
		
		SystemClassif item = (SystemClassif) event.getObject();	
		actionCompleteNext(true, item, 1);			
	}
	
	@SuppressWarnings("rawtypes")
	public void onItemUnselectNextOk(UnselectEvent event) throws DbErrorException {
		
		SystemClassif item = (SystemClassif) event.getObject();	
		actionCompleteNext(false, item, 1);			
	}
	
	public void onItemSelectNextNot(SelectEvent<?> event) throws DbErrorException {
		
		SystemClassif item = (SystemClassif) event.getObject();	
		actionCompleteNext(true, item, 2);			
	}
	
	@SuppressWarnings("rawtypes")
	public void onItemUnselectNextNot(UnselectEvent event) throws DbErrorException {
		
		SystemClassif item = (SystemClassif) event.getObject();	
		actionCompleteNext(false, item, 2);			
	}
	
	public void onItemSelectNextOpt(SelectEvent<?> event) throws DbErrorException {
		
		SystemClassif item = (SystemClassif) event.getObject();	
		actionCompleteNext(true, item, 3);			
	}
	
	@SuppressWarnings("rawtypes")
	public void onItemUnselectNextOpt(UnselectEvent event) throws DbErrorException {
		
		SystemClassif item = (SystemClassif) event.getObject();	
		actionCompleteNext(false, item, 3);			
	}
	
	/******************************************************* END ETAP *******************************************************/	
	
	/******************************************************* TASK **********************************************************/	
	
	public void actionNewTask() {	
		
		this.showDataForTask = true;
		this.defTask = new ProcDefTask();
		this.defTask.setDefEtapId(this.defEtap.getId());
		this.defTask.setIndividual(OmbConstants.CODE_ZNACHENIE_NE);
		this.defTask.setDocRequired(OmbConstants.CODE_ZNACHENIE_NE);
		
		this.defTask.getTaskIzpList().clear();
		this.defTask.getTaskIzpList().add(new ProcDefTaskIzp());
		
		this.opinionNextOk = new ArrayList<>();
		this.opinionNextNot = new ArrayList<>();
		this.opinionNextOpt = new ArrayList<>();
		this.selOpinion = new ArrayList<>();
		
		this.showOpinion = false;
		this.fromEditTask = false;
	}
	
	public void actionEditTask(Integer idObj) {
		
		this.showDataForTask = true;
		this.fromEditTask = true;		
		
		try {
			
			if (idObj != null) {		
				
				JPA.getUtil().runWithClose(() -> this.defTask = this.taskDAO.findById(idObj));
				
				if (this.defTask.getTaskIzpList().isEmpty()) {
					this.defTask.getTaskIzpList().add(new ProcDefTaskIzp());
				}
				
				actionChangeTaskType();
			}
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на задача! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	

	private boolean checkDataForTask() {
		
		boolean save = false;	
		
		if (this.defTask.getTaskType() == null) {
			JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":taskType", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "taskSchedule.vidZad")));
			save = true;		
		} 
		
		if (this.defTask.getIndividual() == null) {
			JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":individual", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefEdit.taskIzpIndivid")));
			save = true;
		}
		
		if (this.defTask.getDocRequired() == null) {
			JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":docRequired", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefEdit.taskIziskvDoc")));
			save = true;
		}
		
		if (!this.defTask.getTaskIzpList().isEmpty()) {
			
			for (int i = 0; i < this.defTask.getTaskIzpList().size(); i++) {
				
				ProcDefTaskIzp izp = this.defTask.getTaskIzpList().get(i);			
				
				if (izp.getCodeRef() == null && izp.getZveno() == null && izp.getEmplPosition() == null && izp.getBusinessRole() == null) {		
					JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":idRep:" + i + ":addIzpal", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "procDefEtapEdit.izpTask", getMessageResourceString(LABELS, "procDefEdit.particInProc")));
					save = true; 			
				
				}  else {
					
					if (izp.getCodeRef() == null && izp.getZveno() != null && izp.getEmplPosition() == null && izp.getBusinessRole() == null) { 
						JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":idRep:" + i + ":addIzpal", FacesMessage.SEVERITY_ERROR, getMessageResourceString (beanMessages, "procDefEdit.choiceDlajOrBussRole"));
						save = true;			
					} 
					
					if (izp.getCodeRef() == null && izp.getZveno() == null && izp.getEmplPosition() != null) {
						JSFUtils.addMessage(FORM_PROC_DEF_ETAP  + ":idRep:" + i + ":selectZvenoTaskIzp:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString (UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procDefList.zveno")));
						save = true;						
					}			
				}
							
			}
		}

		return save;
	}
	
	
	public void actionSaveTask() {
		
		if(checkDataForTask()) {
			PrimeFaces.current().executeScript("scrollToErrors()");
			return;
		}
		
		try {
			
			if(this.defTask.getTaskIzpList() != null) {
				for (int i = 0; i < this.defTask.getTaskIzpList().size(); i++) {
					ProcDefTaskIzp taskIzp = this.defTask.getTaskIzpList().get(i);
					
					if (i == 0) {
						taskIzp.setIzpRole(OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC_OTG);
					} else {
						taskIzp.setIzpRole(OmbConstants.CODE_ZNACHENIE_TASK_REF_ROLE_EXEC);
					}
				}				
			}
			
			this.defTask.setOpinionOkList(new ArrayList<>());
			if (this.opinionNextOk != null && !this.opinionNextOk.isEmpty()) {
				for (SystemClassif nextOk : this.opinionNextOk) {
					this.defTask.getOpinionOkList().add(nextOk.getCode());
				}
			}
			
			this.defTask.setOpinionNotList(new ArrayList<>());
			if (this.opinionNextNot != null && !this.opinionNextNot.isEmpty()) {
				for (SystemClassif nextOk : this.opinionNextNot) {
					this.defTask.getOpinionNotList().add(nextOk.getCode());
				}
			}
			
			this.defTask.setOpinionOptionalList(new ArrayList<>());
			if (this.opinionNextOpt != null && !this.opinionNextOpt.isEmpty()) {
				for (SystemClassif nextOk : this.opinionNextOpt) {
					this.defTask.getOpinionOptionalList().add(nextOk.getCode());
				}
			}
			
			JPA.getUtil().runInTransaction(() -> { 				
				
				this.defTask = this.taskDAO.save(this.defTask);	
				
				this.defTasksList = this.etapDAO.selectDefTaskList(this.defEtap.getId());			
					
			});
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
			
			checkEtapTaskSrok(this.defEtap);
			
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на дефиниция на процедура! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	public void actionDeleteTask() {
		
		if (this.proc.getStatus().intValue() != OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_DEV && this.defTasksList.size() == 1) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procDefEtapEdit.notDelTask"));
			PrimeFaces.current().executeScript("scrollToErrors()");
			return;
		}
		
		try {			
			
			JPA.getUtil().runInTransaction(() -> { 				
				
				this.taskDAO.deleteById(this.defTask.getId());
				
				this.defTasksList = this.etapDAO.selectDefTaskList(this.defEtap.getId());			
					
			});
		
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			
			checkEtapTaskSrok(this.defEtap);
			
			actionNewTask();
			this.showDataForTask = false;
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при изтриване на задача към етап! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void actionSelectSlujForTaskVazl() {
		
		try {
			
			if (this.defTask.getAssignCodeRef() != null) {
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.defTask.getAssignCodeRef(), getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (!refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL))) {				
					this.defTask.setAssignCodeRef(null);
					JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":selectSlujTaskVazl:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procDefList.choiceSluj"));
				}			
			} 
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на служител за възложител на задача! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	public void actionSelectZvenoForTaskVazl() {
		
		try {
			
			if (this.defTask.getAssignZveno() != null) {
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.defTask.getAssignZveno(), getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (!refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO))) {				
					this.defTask.setAssignZveno(null);
					JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":selectZvenoTaskVazl:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procDefList.choiceZveno"));
				}			
			} 
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на звено за възложител на задача! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void actionSelectSlujForTaskIzp(Integer codeRef) {
		
		try {
			
			if (codeRef != null) {
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, codeRef, getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (!refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL))) {	
					codeRef = null;
					JSFUtils.addMessage(FORM_PROC_DEF_ETAP + ":selectSlujTaskIzp:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procDefList.choiceSluj"));
				}			
			} 
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на служител за изпълнител на задача! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	public void actionSelectZvenoForTaskIzp(Integer zveno) {
		
		try {
			
			if (zveno != null) {
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, zveno, getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (!refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_ZVENO))) {
					zveno = null;
					JSFUtils.addMessage(FORM_PROC_DEF_ETAP  + ":selectZvenoTaskIzp:аutoCompl_input", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "procDefList.choiceZveno"));
				}			
			} 
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на звено за изпълнител на задача ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void actionAddNewIzpal(){
		
		this.defTask.getTaskIzpList().add(new ProcDefTaskIzp());		
    }
	
	public void actionRemoveIzpal(ProcDefTaskIzp izp) {

		this.defTask.getTaskIzpList().remove(izp); 
		
		if (this.defTask.getTaskIzpList().isEmpty()) {
			this.defTask.getTaskIzpList().add(new ProcDefTaskIzp());
		}
	}
	
	public String checkEtapTaskSrok(ProcDefEtap defEtap) {

		int taskSrok = 0;
		
		for (ProcDefTask defTask : defTasksList) { // трябва да се вземе от списъка задачи за етапа
			int tmp = 0;
			
			if (defTask.getSrokDays() != null) {
				tmp = defTask.getSrokDays() * 24;
			}
			
			if (defTask.getSrokHours() != null) {
				tmp = tmp + defTask.getSrokHours();
			}
			
			if (taskSrok < tmp) {
				taskSrok = tmp;
			}
		}

		int etapSrok = 0;
		
		if (defEtap.getSrokDays() != null) {
			etapSrok = defEtap.getSrokDays() * 24;
		}
		
		if (defEtap.getSrokHours() != null) {
			etapSrok = etapSrok + defEtap.getSrokHours();
		}

		if (taskSrok != 0 && etapSrok != 0 && taskSrok > etapSrok) {
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN, getMessageResourceString(beanMessages, "procDefEtapEdit.exceedTermOfEtap"));
			PrimeFaces.current().executeScript("scrollToErrors()");
		}

		return null;
	}
	
	public void actionChangeTaskType() {
		
		this.opinionNextOk = new ArrayList<>();
		this.opinionNextNot = new ArrayList<>();
		this.opinionNextOpt = new ArrayList<>();
		this.selOpinion = new ArrayList<>();
		this.opinionList = new ArrayList<>();
				
		try {
			
			if (!fromEditTask) {
				JPA.getUtil().runWithClose(() -> this.taskDAO.setOpinionLists(defTask));
			}
			
			List<SystemClassif> tmpOpinionList = getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_TASK_TYPE_TASK_OPINION, this.defTask.getTaskType(), getCurrentLang(), new Date());
			
			if (tmpOpinionList != null && !tmpOpinionList.isEmpty()) {
				this.showOpinion = true;	
				
				for (SystemClassif tmpSc : tmpOpinionList) {
					if (tmpSc.getCode() != OmbConstants.CODE_ZNACHENIE_TASK_OPINION_STOP_PROC) {
						this.opinionList.add(tmpSc);
					}					
				}
				
			} else {
				this.showOpinion = false;
			}			
			
			if (this.defTask.getOpinionOkList() != null && !this.defTask.getOpinionOkList().isEmpty()) {					
				for (Integer ok : this.defTask.getOpinionOkList()) {
					SystemClassif tmp = new SystemClassif();						
					tmp = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_TASK_OPINION, ok, getCurrentLang(), new Date(), false);
					this.opinionNextOk.add(tmp);
					this.selOpinion.add(ok);
				}					
			}
			
			if (this.defTask.getOpinionNotList() != null && !this.defTask.getOpinionNotList().isEmpty()) {					
				for (Integer not : this.defTask.getOpinionNotList()) {
					SystemClassif tmp = new SystemClassif();						
					tmp = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_TASK_OPINION, not, getCurrentLang(), new Date(), false);
					this.opinionNextNot.add(tmp);
					this.selOpinion.add(not);
				}					
			}
			
			if (this.defTask.getOpinionOptionalList() != null && !this.defTask.getOpinionOptionalList().isEmpty()) {					
				for (Integer opt : this.defTask.getOpinionOptionalList()) {
					SystemClassif tmp = new SystemClassif();						
					tmp = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_TASK_OPINION, opt, getCurrentLang(), new Date(), false);
					this.opinionNextOpt.add(tmp);
					this.selOpinion.add(opt);					
				}					
			}
			
			this.fromEditTask = false;
						
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на мнения! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане списъците на мнения за приключване на задача в обекта! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	/**
	 * autocomplete - търсене на значение за мнения
	 *
	 * @param query
	 * @return
	 */	
	public List<SystemClassif> actionCompleteOpinion(String query) {

		List<SystemClassif> result = new ArrayList<>();

		for (SystemClassif sc : this.opinionList) {
			SystemClassif tmp = new SystemClassif();	
			
			if (this.selOpinion != null && !this.selOpinion.isEmpty()) {
				if(!checkExistCodeInOpinionList(this.selOpinion, sc.getCode())){					
					tmp.setCode(sc.getCode());
					tmp.setTekst(sc.getTekst());
					result.add(sc);					
				}
			
			} else {
				tmp.setCode(sc.getCode());
				tmp.setTekst(sc.getTekst());
				result.add(sc);
			}
		}
		
		return result.stream().filter(t -> t.getTekst().toLowerCase().contains(query.toLowerCase())).collect(Collectors.toList());		
	}	
	
	public boolean checkExistCodeInOpinionList(List<Integer> listOpinion, int code) {
		
		boolean exist = false;
		
		for (Integer opinion : listOpinion) {
			if (code == opinion) {
				exist = true;
				break;
			}
		}
		
		return exist;		
	}
	
	public void actionCompleteOpinionNext(boolean addItem, SystemClassif item, int next) throws DbErrorException { 
		
		if (next == 1) {
		
			List<SystemClassif> selClassif = this.opinionNextOk;
	
			if (selClassif != null && !selClassif.isEmpty()) {
				
				for (int i = 0; i < selClassif.size() - 1; i++) {
					Integer code = selClassif.get(i).getCode();
					
					if (code.equals(item.getCode()) && addItem) {
						selClassif.remove(selClassif.size() - 1); // дублира се - изтрива се последния
						break;
					}
				}			
			}
		
		} else if (next == 2) {
			
			List<SystemClassif> selClassif = this.opinionNextNot;

			if (selClassif != null && !selClassif.isEmpty()) {
				
				for (int i = 0; i < selClassif.size() - 1; i++) {
					Integer code = selClassif.get(i).getCode();
					
					if (code.equals(item.getCode()) && addItem) {
						selClassif.remove(selClassif.size() - 1); // дублира се - изтрива се последния
						break;
					}
				}	
			}
		
		} else if (next == 3) {
			
			List<SystemClassif> selClassif = this.opinionNextOpt;

			if (selClassif != null && !selClassif.isEmpty()) {
				
				for (int i = 0; i < selClassif.size() - 1; i++) {
					Integer code = selClassif.get(i).getCode();
					
					if (code.equals(item.getCode()) && addItem) {
						selClassif.remove(selClassif.size() - 1); // дублира се - изтрива се последния
						break;
					}
				}			
			}
		}
	}	
	
	public void onItemSelectOpinionNextOk(SelectEvent<?> event) throws DbErrorException {
		
		SystemClassif item = (SystemClassif) event.getObject();	
		actionCompleteOpinionNext(true, item, 1);
		this.selOpinion.add(item.getCode());	
	}
	
	@SuppressWarnings("rawtypes")
	public void onItemUnselectOpinionNextOk(UnselectEvent event) throws DbErrorException {
		
		SystemClassif item = (SystemClassif) event.getObject();	
		actionCompleteOpinionNext(false, item, 1);
		
		List<Integer> tmpList = new ArrayList<>();
		
		for (Integer selOp : this.selOpinion) {			
			if (item.getCode() == selOp) {
				tmpList.add(selOp);
			}					
		}
		
		for (Integer tmp : tmpList) {
			this.selOpinion.remove(tmp);
		}		
	}
	
	public void onItemSelectOpinionNextNot(SelectEvent<?> event) throws DbErrorException {
		
		SystemClassif item = (SystemClassif) event.getObject();	
		actionCompleteOpinionNext(true, item, 2);
		this.selOpinion.add(item.getCode());	
	}
	
	@SuppressWarnings("rawtypes")
	public void onItemUnselectOpinionNextNot(UnselectEvent event) throws DbErrorException {
		
		SystemClassif item = (SystemClassif) event.getObject();	
		actionCompleteOpinionNext(false, item, 2);
		
		List<Integer> tmpList = new ArrayList<>();
		
		for (Integer selOp : this.selOpinion) {			
			if (item.getCode() == selOp) {
				tmpList.add(selOp);
			}					
		}
		
		for (Integer tmp : tmpList) {
			this.selOpinion.remove(tmp);
		}	
	}
	
	public void onItemSelectOpinionNextOpt(SelectEvent<?> event) throws DbErrorException {
		
		SystemClassif item = (SystemClassif) event.getObject();	
		actionCompleteOpinionNext(true, item, 3);
		this.selOpinion.add(item.getCode());	
	}
	
	@SuppressWarnings("rawtypes")
	public void onItemUnselectOpinionNextOpt(UnselectEvent event) throws DbErrorException {
		
		SystemClassif item = (SystemClassif) event.getObject();	
		actionCompleteOpinionNext(false, item, 3);
		
		List<Integer> tmpList = new ArrayList<>();
		
		for (Integer selOp : this.selOpinion) {			
			if (item.getCode() == selOp) {
				tmpList.add(selOp);
			}					
		}
		
		for (Integer tmp : tmpList) {
			this.selOpinion.remove(tmp);
		}	
	}
	
	/******************************************************* END TASK *******************************************************/	
	
	/******************************************************* GET & SET *******************************************************/	
	
	public Date getDecodeDate() {
		return new Date(decodeDate.getTime()) ;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
	}

	public ProcDef getProc() {
		return proc;
	}

	public void setProc(ProcDef proc) {
		this.proc = proc;
	}

	public ProcDefEtap getDefEtap() {
		return defEtap;
	}

	public void setDefEtap(ProcDefEtap defEtap) {
		this.defEtap = defEtap;
	}	

	public List<ProcDefEtap> getEtapsList() {
		return etapsList;
	}

	public void setEtapsList(List<ProcDefEtap> etapsList) {
		this.etapsList = etapsList;
	}

	public List<ProcDefTask> getDefTasksList() {
		return defTasksList;
	}

	public void setDefTasksList(List<ProcDefTask> defTasksList) {
		this.defTasksList = defTasksList;
	}

	public ProcDefTask getDefTask() {
		return defTask;
	}

	public void setDefTask(ProcDefTask defTask) {
		this.defTask = defTask;
	}

	public boolean isShowDataForTask() {
		return showDataForTask;
	}

	public void setShowDataForTask(boolean showDataForTask) {
		this.showDataForTask = showDataForTask;
	}

	public boolean isHideSrokHours() {
		return hideSrokHours;
	}

	public void setHideSrokHours(boolean hideSrokHours) {
		this.hideSrokHours = hideSrokHours;
	}

	public int getNextNomerEtap() {
		return nextNomerEtap;
	}

	public void setNextNomerEtap(int nextNomerEtap) {
		this.nextNomerEtap = nextNomerEtap;
	}

	public List<SystemClassif> getSelNextOk() {
		return selNextOk;
	}

	public void setSelNextOk(List<SystemClassif> selNextOk) {
		this.selNextOk = selNextOk;
	}

	public List<SystemClassif> getSelNextNot() {
		return selNextNot;
	}

	public void setSelNextNot(List<SystemClassif> selNextNot) {
		this.selNextNot = selNextNot;
	}

	public List<SystemClassif> getSelNextOpt() {
		return selNextOpt;
	}

	public void setSelNextOpt(List<SystemClassif> selNextOpt) {
		this.selNextOpt = selNextOpt;
	}

	public Integer getOldNumberEtap() {
		return oldNumberEtap;
	}

	public void setOldNumberEtap(Integer oldNumberEtap) {
		this.oldNumberEtap = oldNumberEtap;
	}

	public List<SelectItem> getEmplPositionList() {
		return emplPositionList;
	}

	public void setEmplPositionList(List<SelectItem> emplPositionList) {
		this.emplPositionList = emplPositionList;
	}

	public List<SelectItem> getBusinessRoleList() {
		return businessRoleList;
	}

	public void setBusinessRoleList(List<SelectItem> businessRoleList) {
		this.businessRoleList = businessRoleList;
	}
	
	public List<SystemClassif> getOpinionList() {
		return opinionList;
	}

	public void setOpinionList(List<SystemClassif> opinionList) {
		this.opinionList = opinionList;
	}

	public List<SystemClassif> getOpinionNextOk() {
		return opinionNextOk;
	}

	public void setOpinionNextOk(List<SystemClassif> opinionNextOk) {
		this.opinionNextOk = opinionNextOk;
	}

	public List<SystemClassif> getOpinionNextNot() {
		return opinionNextNot;
	}

	public void setOpinionNextNot(List<SystemClassif> opinionNextNot) {
		this.opinionNextNot = opinionNextNot;
	}

	public List<SystemClassif> getOpinionNextOpt() {
		return opinionNextOpt;
	}

	public void setOpinionNextOpt(List<SystemClassif> opinionNextOpt) {
		this.opinionNextOpt = opinionNextOpt;
	}	

	public List<Integer> getSelOpinion() {
		return selOpinion;
	}

	public void setSelOpinion(List<Integer> selOpinion) {
		this.selOpinion = selOpinion;
	}

	public boolean isShowOpinion() {
		return showOpinion;
	}

	public void setShowOpinion(boolean showOpinion) {
		this.showOpinion = showOpinion;
	}

	public boolean isFromEditTask() {
		return fromEditTask;
	}

	public void setFromEditTask(boolean fromEditTask) {
		this.fromEditTask = fromEditTask;
	}

	/**************************************************** END GET & SET ****************************************************/	
	
	transient Comparator<SelectItem> compatator = new Comparator<SelectItem>() {
		public int compare(SelectItem s1, SelectItem s2) {
			return (s1.getLabel().toUpperCase().compareTo(s2.getLabel().toUpperCase()));
		}
	};
	
}
package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocVidSettingDAO;
import com.ib.omb.db.dto.DocVidSetting;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.SearchUtils;

/**
 * @author dkarapetrova
 *
 */
@Named
@ViewScoped
public class DocVidSettings extends IndexUIbean implements Serializable {	
	
	/**
	 * Въвеждане / актуализация на характеристики за вид документи
	 * 
	 */
	private static final long serialVersionUID = -2383214964204529272L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocVidSettings.class);
	
	private static final String FORM_REGISTRATURA_TABS_REGISTRATURI = "formRegistratura:tabsRegistraturi";

	private DocVidSetting dvSetting = new DocVidSetting();
	private transient DocVidSettingDAO settDAO;
	private Integer idReg;
	private boolean showDataForDocVidSet = false;

	private LazyDataModelSQL2Array dvSettingsList;

	private List<SelectItem> registriList;
	private boolean createDelo = false;
	
	private List<Files> filesList = new ArrayList<>();
	
	private Integer registerAlg;
	private boolean fromEdit = false;
	
	private List<SystemClassif> proceduriListDefIn;
	private List<SystemClassif> proceduriListDefWork;
	private List<SystemClassif> proceduriListDefOwn;
	
	private boolean viewProcDefIn = false;
	private boolean viewProcDefOwn = false;
	private boolean viewProcDefWork = false;
	
	private Integer settingMembers;
	private boolean memberActive = false;
	private List<SystemClassif> docHarList;

	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {

		LOGGER.debug("PostConstruct - DocVidSettings!!!");

		this.settDAO = new DocVidSettingDAO(getUserData());
		
		this.registriList = new ArrayList<>();
		this.proceduriListDefIn = new ArrayList<>();
		this.proceduriListDefWork = new ArrayList<>();
		this.proceduriListDefOwn = new ArrayList<>();
		this.docHarList = new ArrayList<>();

		this.idReg = (Integer) JSFUtils.getFlashScopeValue("idReg");

		if (idReg != null) {

			SelectMetadata smd = this.settDAO.createSelectMetadataByRegistraturaId(this.idReg);
			String defaultSortColumn = "A0";
			this.dvSettingsList = new LazyDataModelSQL2Array(smd, defaultSortColumn);
		}

		this.showDataForDocVidSet = false;

		setRegistriList(registriListByIdReg(this.idReg));
		
		try {	
			
			Map<Integer, Object> specificsIn = new HashMap<>();
			specificsIn.put(OmbClassifAdapter.PROCEDURI_INDEX_REGISTRATURA, this.idReg); // специфика за процедури по ид на регистратура
			specificsIn.put(OmbClassifAdapter.PROCEDURI_INDEX_STATUS, OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE); // специфика по статус активна за процедурите
			specificsIn.put(OmbClassifAdapter.PROCEDURI_INDEX_DOC_TYPE, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN); // специфика по тип документ - входящ
			
			this.proceduriListDefIn = ((SystemData) getSystemData()).queryClassification(OmbConstants.CODE_CLASSIF_PROCEDURI, null, new Date(), getCurrentLang(), specificsIn);
			
			Map<Integer, Object> specificsWork = new HashMap<>();
			specificsWork.put(OmbClassifAdapter.PROCEDURI_INDEX_REGISTRATURA, this.idReg); // специфика за процедури по ид на регистратура
			specificsWork.put(OmbClassifAdapter.PROCEDURI_INDEX_STATUS, OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE); // специфика по статус активна за процедурите
			specificsWork.put(OmbClassifAdapter.PROCEDURI_INDEX_DOC_TYPE, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK); // специфика по тип документ - работен
			
			this.proceduriListDefWork = ((SystemData) getSystemData()).queryClassification(OmbConstants.CODE_CLASSIF_PROCEDURI, null, new Date(), getCurrentLang(), specificsWork);
			
			Map<Integer, Object> specificsOwn = new HashMap<>();
			specificsOwn.put(OmbClassifAdapter.PROCEDURI_INDEX_REGISTRATURA, this.idReg); // специфика за процедури по ид на регистратура
			specificsOwn.put(OmbClassifAdapter.PROCEDURI_INDEX_STATUS, OmbConstants.CODE_ZNACHENIE_PROC_DEF_STAT_ACTIVE); // специфика по статус активна за процедурите
			specificsOwn.put(OmbClassifAdapter.PROCEDURI_INDEX_DOC_TYPE, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN); // специфика по тип документ - собствен
			
			this.proceduriListDefOwn = ((SystemData) getSystemData()).queryClassification(OmbConstants.CODE_CLASSIF_PROCEDURI, null, new Date(), getCurrentLang(), specificsOwn);
			
			this.settingMembers = ((SystemData) getSystemData()).getRegistraturaSetting(idReg, OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_7);
			
			this.docHarList = ((SystemData) getSystemData()).getSysClassification(OmbConstants.CODE_CLASSIF_CHARACTER_SPEC_DOC, new Date(), getCurrentLang());
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на списъка с процедури! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	
	}
	
	public List<SelectItem> registriListByIdReg(Integer idRegistratura){
		
		List<SelectItem> items = new ArrayList<>();
		
		try {
			
			String specific = (String) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRATURI, idRegistratura, getCurrentLang(), new Date(), OmbClassifAdapter.REGISTRATURI_INDEX_REGISTRI);
			if (SearchUtils.isEmpty(specific)) {
				return items;
			}
	
			String[] registersIds = specific.split(",");
	
			for (String r : registersIds) {
				String name = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_REGISTRI, Integer.valueOf(r), getCurrentLang(), new Date());
				items.add(new SelectItem(r, name));
			}
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на регистри по ид на регистратура! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		
		return items;
	}
	
	/**
	 * Метод за зареждане данните на характеристиките по вид документи
	 * 
	 */
	public void actionEdit(Integer idObj) {

		this.showDataForDocVidSet = true;
		this.fromEdit = true;

		try {

			if (idObj != null) {

				JPA.getUtil().runWithClose(() -> {

					this.dvSetting = this.settDAO.findById(idObj);

					// извличане на файловете от таблица с файловете
					this.filesList = new FilesDAO(getUserData()).selectByFileObject(this.dvSetting.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC_VID_SETT);
				});

				if (Objects.equals(this.dvSetting.getCreateDelo(), OmbConstants.CODE_ZNACHENIE_DA)) {
					this.createDelo = true;
				} else {
					this.createDelo = false;
				}
				
				if (Objects.equals(this.dvSetting.getMembersActive(), OmbConstants.CODE_ZNACHENIE_DA)) {
					this.memberActive = true;
				} else {
					this.memberActive = false;
				}
				
				actionChangeRegister();
				
				if (this.dvSetting.getProcDefIn() != null) {
					this.viewProcDefIn = true;				
				}
				
				if (this.dvSetting.getProcDefOwn() != null) {
					this.viewProcDefOwn = true;						
				}
				
				if (this.dvSetting.getProcDefWork() != null) {
					this.viewProcDefWork = true;						
				}
			}

		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на характеристиките по вид документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}

	}

	public boolean checkData() {

		boolean save = false;

		if (this.dvSetting.getDocVid() == null) {
			JSFUtils.addMessage(FORM_REGISTRATURA_TABS_REGISTRATURI + ":typeDoc:аutoCompl", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docVidSetting.typeDoc")));
			save = true;
		}
		
		if(this.dvSetting.getBegNomer() == null && (this.registerAlg != null && this.registerAlg.equals(OmbConstants.CODE_ZNACHENIE_ALG_VID_DOC))) { 
			JSFUtils.addMessage(FORM_REGISTRATURA_TABS_REGISTRATURI + ":begNumDVS", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "register.begNum")));
			save = true;
		}
		
		if(this.dvSetting.getActNomer() == null && (this.registerAlg != null && this.registerAlg.equals(OmbConstants.CODE_ZNACHENIE_ALG_VID_DOC))) {
			JSFUtils.addMessage(FORM_REGISTRATURA_TABS_REGISTRATURI + ":actNumDVS", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "register.actNum")));
			save = true;
		}
		
		if(this.dvSetting.getStep() == null && (this.registerAlg != null && this.registerAlg.equals(OmbConstants.CODE_ZNACHENIE_ALG_VID_DOC))) {
			JSFUtils.addMessage(FORM_REGISTRATURA_TABS_REGISTRATURI + ":stepDVS", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "register.step")));
			save = true;
		}
		
		if (this.memberActive && (this.dvSetting.getMembersTab() == null ||  this.dvSetting.getMembersTab().isEmpty())) {
			JSFUtils.addMessage(FORM_REGISTRATURA_TABS_REGISTRATURI + ":nameMember", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docVidSettings.nameMemberList")));
			save = true;
		}
		
		return save;
	}

	/**
	 * За нов запис
	 * 
	 */
	public void actionNew() {

		this.showDataForDocVidSet = true;
		this.createDelo = false;
		this.dvSetting = new DocVidSetting();
		this.dvSetting.setRegistraturaId(this.idReg);
		this.filesList = new ArrayList<>();
		
		this.registerAlg = null;
		this.fromEdit = false;
		this.memberActive = false;
	}

	/**
	 * Запис на характеристики по вид документи
	 * 
	 */
	public void actionSave() {

		if (checkData()) {
			return;
		}

		try {
			
			if (this.createDelo) {
				this.dvSetting.setCreateDelo(OmbConstants.CODE_ZNACHENIE_DA);								
			} else {
				this.dvSetting.setCreateDelo(OmbConstants.CODE_ZNACHENIE_NE);	
			}
			
			if (this.memberActive) {
				this.dvSetting.setMembersActive(OmbConstants.CODE_ZNACHENIE_DA);								
			} else {
				this.dvSetting.setMembersActive(OmbConstants.CODE_ZNACHENIE_NE);	
			}

			JPA.getUtil().runInTransaction(() -> this.dvSetting = this.settDAO.save(this.dvSetting, getSystemData()));
			
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS, false, false);

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.succesSaveMsg"));

			SelectMetadata smd = this.settDAO.createSelectMetadataByRegistraturaId(this.idReg);
			String defaultSortColumn = "A0";
			this.dvSettingsList = new LazyDataModelSQL2Array(smd, defaultSortColumn);

		} catch (ObjectInUseException e) {
			LOGGER.error("Грешка при запис на характеристиките по вид документи! - {}", e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при запис на характеристиките по вид документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}

	/**
	 * Изтриване на характеиристики по вид документи
	 * 
	 */
	public void actionDelete() {

		try {

			JPA.getUtil().runInTransaction(() -> this.settDAO.delete(this.dvSetting));

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS, false, false);

			actionNew();

			SelectMetadata smd = this.settDAO.createSelectMetadataByRegistraturaId(this.idReg);
			String defaultSortColumn = "A0";
			this.dvSettingsList = new LazyDataModelSQL2Array(smd, defaultSortColumn);

		} catch (ObjectInUseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());

		} catch (BaseException e) {
			LOGGER.error("Грешка при изтриване на характеристиките по вид документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}
	
	/**
	 * Промяна на регистър
	 * 
	 */
	public void actionChangeRegister() {
		
		try {
		
			if (this.dvSetting.getRegisterId() != null) {
			
				this.registerAlg = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRI, this.dvSetting.getRegisterId(), getCurrentLang(), new Date(), OmbClassifAdapter.REGISTRI_INDEX_ALG);
				
				if (this.registerAlg.equals(OmbConstants.CODE_ZNACHENIE_ALG_VID_DOC)) {
					if(!this.fromEdit) {
						this.dvSetting.setPrefix(null);
						this.dvSetting.setBegNomer(1);
						this.dvSetting.setActNomer(1);
						this.dvSetting.setStep(1);
					}
				} else {
					this.dvSetting.setPrefix(null);
					this.dvSetting.setBegNomer(null);
					this.dvSetting.setActNomer(null);
					this.dvSetting.setStep(null);
				}
								
				Integer registerTipDoc = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_REGISTRI, this.dvSetting.getRegisterId(), getCurrentLang(), new Date(), OmbClassifAdapter.REGISTRI_INDEX_DOC_TYPE);
				
				this.viewProcDefIn = false;
				this.viewProcDefOwn = false;
				this.viewProcDefWork = false;
				
				if (registerTipDoc != null) {						
				
					if (Objects.equals(registerTipDoc, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_IN)) {
						this.viewProcDefIn = true;					
					
					} else if (Objects.equals(registerTipDoc, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_OWN)) {
						this.viewProcDefOwn = true;
						
					} else if (Objects.equals(registerTipDoc, OmbConstants.CODE_ZNACHENIE_DOC_TYPE_WRK)) {
						this.viewProcDefWork = true;					
					}
				
				} else {
					this.viewProcDefIn = true;
					this.viewProcDefOwn = true;
				}
				
				if (!viewProcDefIn) {
					this.dvSetting.setProcDefIn(null);					
				}
				
				if (!viewProcDefOwn) {
					this.dvSetting.setProcDefOwn(null);					
				}
				
				if (!viewProcDefWork) {
					this.dvSetting.setProcDefWork(null);					
				}					
			
			} else {
				this.registerAlg = null;
				
				this.viewProcDefIn = true;					
				this.viewProcDefOwn = true;
				this.viewProcDefWork = true;
			}
							
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}			
		
		
	}
	
	/**
	 * Промяна на вид документ
	 * 
	 */
	public void actionChangeVidDoc() {
		
		if (this.dvSetting.getDocVid() != null) {
			
			try {

				int code = (this.idReg + "_" + this.dvSetting.getDocVid()).hashCode();

				SystemClassif item = getSystemData().decodeItemLite(OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS, code, getCurrentLang(), new Date(), false);
				if (item != null) {
					Integer id = Integer.valueOf(item.getCodeExt());
					// тука трябва да му извикаш findById
					actionEdit(id);
				}
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}
	}
	
	/**
	 * Показване на участници
	 * 
	 */
	public void actionShowHideMemberActive() {
		
		if (this.memberActive) {
			this.dvSetting.setMembersTab("Участници");			
		} else {
			this.dvSetting.setMembersTab(null);			
		}		
	}
	
	/**
	 * Презареждане на характеристики
	 * 
	 */
	public void actionChangeFiles() {
		
		try {
			
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_DOC_VID_SETTINGS, false, false);
		
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}		
	}

	public DocVidSetting getDvSetting() {
		return dvSetting;
	}

	public void setDvSetting(DocVidSetting dvSetting) {
		this.dvSetting = dvSetting;
	}

	public Integer getIdReg() {
		return idReg;
	}

	public void setIdReg(Integer idReg) {
		this.idReg = idReg;
	}

	public boolean isShowDataForDocVidSet() {
		return showDataForDocVidSet;
	}

	public void setShowDataForDocVidSet(boolean showDataForDocVidSet) {
		this.showDataForDocVidSet = showDataForDocVidSet;
	}

	public LazyDataModelSQL2Array getDvSettingsList() {
		return dvSettingsList;
	}

	public void setDvSettingsList(LazyDataModelSQL2Array dvSettingsList) {
		this.dvSettingsList = dvSettingsList;
	}

	public List<SelectItem> getRegistriList() {
		return registriList;
	}

	public void setRegistriList(List<SelectItem> registriList) {
		this.registriList = registriList;
	}

	public boolean isCreateDelo() {
		return createDelo;
	}

	public void setCreateDelo(boolean createDelo) {
		this.createDelo = createDelo;
	}

	public List<Files> getFilesList() {
		return filesList;
	}

	public void setFilesList(List<Files> filesList) {
		this.filesList = filesList;
	}

	public Integer getRegisterAlg() {
		return registerAlg;
	}

	public void setRegisterAlg(Integer registerAlg) {
		this.registerAlg = registerAlg;
	}
	
	public boolean isFromEdit() {
		return fromEdit;
	}

	public void setFromEdit(boolean fromEdit) {
		this.fromEdit = fromEdit;
	}
	
/******************************** EXPORTS **********************************/
	
	public List<SystemClassif> getProceduriListDefIn() {
		return proceduriListDefIn;
	}

	public void setProceduriListDefIn(List<SystemClassif> proceduriListDefIn) {
		this.proceduriListDefIn = proceduriListDefIn;
	}	

	public List<SystemClassif> getProceduriListDefWork() {
		return proceduriListDefWork;
	}

	public void setProceduriListDefWork(List<SystemClassif> proceduriListDefWork) {
		this.proceduriListDefWork = proceduriListDefWork;
	}

	public List<SystemClassif> getProceduriListDefOwn() {
		return proceduriListDefOwn;
	}

	public void setProceduriListDefOwn(List<SystemClassif> proceduriListDefOwn) {
		this.proceduriListDefOwn = proceduriListDefOwn;
	}

	public boolean isViewProcDefIn() {
		return viewProcDefIn;
	}

	public void setViewProcDefIn(boolean viewProcDefIn) {
		this.viewProcDefIn = viewProcDefIn;
	}

	public boolean isViewProcDefOwn() {
		return viewProcDefOwn;
	}

	public void setViewProcDefOwn(boolean viewProcDefOwn) {
		this.viewProcDefOwn = viewProcDefOwn;
	}
	
	public boolean isViewProcDefWork() {
		return viewProcDefWork;
	}

	public void setViewProcDefWork(boolean viewProcDefWork) {
		this.viewProcDefWork = viewProcDefWork;
	}

	public Integer getSettingMembers() {
		return settingMembers;
	}

	public void setSettingMembers(Integer settingMembers) {
		this.settingMembers = settingMembers;
	}

	public boolean isMemberActive() {
		return memberActive;
	}

	public void setMemberActive(boolean memberActive) {
		this.memberActive = memberActive;
	}

	public List<SystemClassif> getDocHarList() {
		return docHarList;
	}

	public void setDocHarList(List<SystemClassif> docHarList) {
		this.docHarList = docHarList;
	}

	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за характеристиките на видове документи
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "docVidSetting.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, null, null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за характеристиките на видове документи
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "docVidSetting.reportTitle");		
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
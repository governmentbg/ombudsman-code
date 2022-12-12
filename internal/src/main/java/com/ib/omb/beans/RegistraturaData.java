package com.ib.omb.beans;

import static com.ib.system.utils.SearchUtils.isEmpty;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;

import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.TreeNode;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.navigation.Navigation;
import com.ib.indexui.navigation.NavigationData;
import com.ib.indexui.navigation.NavigationDataHolder;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.indexui.utils.TreeUtils;
import com.ib.omb.db.dao.RegistraturaDAO;
import com.ib.omb.db.dto.Registratura;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.ValidationUtils;

@Named
@ViewScoped
public class RegistraturaData extends IndexUIbean  implements Serializable {	
	
	/**
	 * Въвеждане / актуализация на регистратура
	 * 
	 */
	private static final long serialVersionUID = -1374075401713395110L;
	private static final Logger LOGGER = LoggerFactory.getLogger(RegistraturaData.class);
	
//	private static final String ID_OBJ = "idObj";
	private static final String FORM_TABS_REGISTRATURI = "formRegistratura:tabsRegistraturi";	
	
	private Registratura registratura;
	private transient RegistraturaDAO regDAO;
	
	private boolean activeReg = true;	
	private boolean useSEOS = false;
	private boolean useSSEV = false;
	
	private List<Files> filesList;
	
	private TreeNode rootNode;
	private boolean showSluj = false;
	private String activeTab; // id на активния в момента таб
	
	private boolean useWEBEAY = false;
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct - RegistraturaData!!!");
		
		try {
		
			this.regDAO = new RegistraturaDAO(getUserData());
			this.registratura = new Registratura();
			
			//if (JSFUtils.getRequestParameter(ID_OBJ) != null && !"".equals(JSFUtils.getRequestParameter(ID_OBJ))) {
				
				//Integer idObj = Integer.valueOf(JSFUtils.getRequestParameter(ID_OBJ));
				
				Integer idObj = getUserData(UserData.class).getRegistratura();
				
				JSFUtils.addFlashScopeValue("idReg", idObj);
				
				if (idObj != null) {
					
					JPA.getUtil().runWithClose(() -> {
						this.registratura = this.regDAO.findById(idObj);
	
						// извличане на файловете от таблица с файловете
						this.filesList = new FilesDAO(getUserData()).selectByFileObject(this.registratura.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA);
					});
					
					if(Objects.equals(this.registratura.getValid(), OmbConstants.CODE_ZNACHENIE_DA)) {
						this.activeReg = true;
					
					} else {
						this.activeReg = false;
					}					
					
					actionShowHideSluj();
				}
			
//			} else {
//				
//				this.registratura = new Registratura();
//			}
			
			String inclSeos = getSystemData().getSettingsValue("system.useSEOS");
			
			if ("1".equals(inclSeos)) {
				this.useSEOS = true;
			} else {
				this.useSEOS = false;
			}
			
			String inclSSEV = getSystemData().getSettingsValue("system.useSSEV");
			
			if ("1".equals(inclSSEV)) {
				this.useSSEV = true;
			} else {
				this.useSSEV = false;
			}
			
			if ("1".equals(getSystemData().getSettingsValue("system.useWEBEAY"))) {
				this.useWEBEAY = true;
			} else {
				this.useWEBEAY = false;
			}
			
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на регистратурата! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	public void actionShowHideSluj() {

		try {

			List<SystemClassif> listItems;
			if (this.showSluj) { 
				listItems = this.regDAO.loadAdmStruct(registratura.getId(), true, getSystemData());
			
			} else {
				listItems = this.regDAO.loadAdmStruct(registratura.getId(), false, getSystemData());
			}

			this.rootNode = new TreeUtils().loadTreeData3(listItems, null, false, !this.showSluj, null, null);

		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, "Грешка при работа с базата данни!");
		}
	}
	
	private boolean checkData() {
		
		boolean save = false;		

		if(this.registratura.getRegistratura() == null || this.registratura.getRegistratura().isEmpty()) {
			JSFUtils.addMessage(FORM_TABS_REGISTRATURI + ":naimReg", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString("labels", "regList.naimReg")));
			save = true;
		}
		
		if (!isEmpty(this.registratura.getOrgEik()) && !ValidationUtils.isValidBULSTAT(this.registratura.getOrgEik())) {
			JSFUtils.addMessage(FORM_TABS_REGISTRATURI + ":eikOrgReg", FacesMessage.SEVERITY_ERROR, String.format(getMessageResourceString(beanMessages, "dvijenie.eikNevaliden"), this.registratura.getOrgEik().trim()));
			save = true;
		}
		
		if (this.registratura.getEkatte() == null) {
			JSFUtils.addMessage(FORM_TABS_REGISTRATURI + ":mesto:аutoCompl", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString("ui_labels", "general.ekatte")));
			save = true;
		}
		
		if (this.registratura.getAddress() == null || this.registratura.getAddress().isEmpty()
				&& (this.registratura.getPostBox() == null || this.registratura.getPostBox().isEmpty())) {
			JSFUtils.addMessage(FORM_TABS_REGISTRATURI + ":addrOrPb", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "registratura.addrOrPostBox"));
			save = true;
		}
		
		return save;		
	}
	
	public void actionNew() {
		
		this.registratura = new Registratura();
		this.filesList = new ArrayList<>();
		
		this.activeReg = true;				
		this.showSluj = false;
	}
	
	public void actionSave() {
		
		if(checkData()) {
			return;
		}
		
		try {
			
			if (activeReg) {
				this.registratura.setValid(Constants.CODE_ZNACHENIE_DA);
			} else {
				this.registratura.setValid(Constants.CODE_ZNACHENIE_NE);
			}
		
			JPA.getUtil().runInTransaction(() -> this.registratura = this.regDAO.save(this.registratura));
		
					
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRATURI, false, false);
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS, false, false);
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRATURI_REQDOC, false, false);
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.succesSaveMsg"));
			
			actionShowHideSluj();
			
			Navigation navHolder = new Navigation();			
		    int i = navHolder.getNavPath().size();	
		   
		    NavigationDataHolder dataHoslder = (NavigationDataHolder) JSFUtils.getManagedBean("navigationSessionDataHolder");
		    Stack<NavigationData> stackPath = dataHoslder.getPageList();
		    NavigationData nd = stackPath.get(i-2);
		    Map<String, Object> mapV = nd.getViewMap();
		    
		    RegistraturiList regsList = (RegistraturiList) mapV.get("registraturiList");		    
		    regsList.initData();
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на регистратура! ", e);			
		}		
		
	}
	
	public void actionDelete() {
		
		try {
			
			JPA.getUtil().runInTransaction(() -> {
				this.regDAO.delete(this.registratura);
				
				if (this.filesList != null && !this.filesList.isEmpty()) { // трябва да се трият и файловете
					FilesDAO filesDao = new FilesDAO(getUserData());
					for (Files f : this.filesList) {
						filesDao.deleteFileObject(f);
					}
				}
			});
		
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRATURI, false, false);
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRATURI_OBJACCESS, false, false);
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_REGISTRATURI_REQDOC, false, false);

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			
			actionNew();			
			
			Navigation navHolder = new Navigation();			
		    int i = navHolder.getNavPath().size();	
		   
		    NavigationDataHolder dataHoslder = (NavigationDataHolder) JSFUtils.getManagedBean("navigationSessionDataHolder");
		    Stack<NavigationData> stackPath = dataHoslder.getPageList();
		    NavigationData nd = stackPath.get(i-2);
		    Map<String, Object> mapV = nd.getViewMap();
		    
		    RegistraturiList regsList = (RegistraturiList) mapV.get("registraturiList");		    
		    regsList.initData();
		    
		    navHolder.goBack();
		    
		} catch (ObjectInUseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());		
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на регистратура! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}	
	
	/**
	 * При смяна на таб
	 * @param event
	 */
	@SuppressWarnings("rawtypes")
    public void onTabChange(TabChangeEvent event) {
	   	if(event != null) {
			LOGGER.debug("onTabChange - Active Tab: {}",  event.getTab().getId());			
			activeTab = event.getTab().getId();
			JSFUtils.addFlashScopeValue("idReg", this.registratura.getId());
	   	}
    }
	
	/**
	 * Проверка за валидност на ЕИК
	 */
	public void actionCheckEik() {
		 
		 if (!isEmpty(this.registratura.getOrgEik()) && !ValidationUtils.isValidBULSTAT(this.registratura.getOrgEik())) {
			JSFUtils.addMessage(FORM_TABS_REGISTRATURI + ":eikOrgReg", FacesMessage.SEVERITY_ERROR, String.format(getMessageResourceString(beanMessages, "dvijenie.eikNevaliden"), this.registratura.getOrgEik().trim()));			
		 }		 
	 }
	
	/** Избор на файлове за лого
	 * 
	 * @param event
	 */
	public void uploadFileListener(FileUploadEvent event){		
		
		try {
			
			UploadedFile upFile = event.getFile();
			
			Files fileObject = new Files();
			fileObject.setFilename(upFile.getFileName());
			fileObject.setContentType(upFile.getContentType());
			fileObject.setContent(upFile.getContent());	
			
			JPA.getUtil().runInTransaction(() -> {
				new FilesDAO(getUserData()).saveFileObject(fileObject, this.registratura.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA); 

				// извличане на файловете от таблица с файловете
				this.filesList = new FilesDAO(getUserData()).selectByFileObject(this.registratura.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_REISTRATURA);
			}); 
		
		} catch (Exception e) {
			LOGGER.error("Exception: " + e.getMessage());	
		} 
	}
	

	
	/**
	 * Download selected file
	 *
	 * @param files
	 */
	public void downloadFile(Files file) {

			boolean ok = true;
			
			if(file.getContent() == null && file.getId() != null) {
				
				try {					
					
					file = new FilesDAO(getUserData()).findById(file.getId());
					
					if(file.getPath() != null && !file.getPath().isEmpty()){
						Path path = Paths.get(file.getPath());
						file.setContent(java.nio.file.Files.readAllBytes(path));
					}
				
				} catch (DbErrorException e) {
					LOGGER.error("DbErrorException: " + e.getMessage());
					ok = false;
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"));
				
				} catch (IOException e) {
					LOGGER.error("IOException: " + e.getMessage());
					ok = false;
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,"general.unexpectedResult"));
					LOGGER.error(e.getMessage(), e);
				
				} catch (Exception e) {
					LOGGER.error("Exception: " + e.getMessage());
					ok = false;
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, "general.exception"));
				
				}  finally {
					JPA.getUtil().closeConnection();
				}
			}
			
			if(ok){
				
				try {
					
					FacesContext facesContext = FacesContext.getCurrentInstance();
				    ExternalContext externalContext = facesContext.getExternalContext();
				    
				    HttpServletRequest request =(HttpServletRequest)externalContext.getRequest();
					String agent = request.getHeader("user-agent");
					
					String codedfilename = ""; 
					
					if (null != agent &&  (-1 != agent.indexOf("MSIE") || (-1 != agent.indexOf("Mozilla") && -1 != agent.indexOf("rv:11")) || (-1 != agent.indexOf("Edge"))  ) ) {
						codedfilename = URLEncoder.encode(file.getFilename(), "UTF8");
					} else if (null != agent && -1 != agent.indexOf("Mozilla")) {
						codedfilename = MimeUtility.encodeText(file.getFilename(), "UTF8", "B");
					} else {
						codedfilename = URLEncoder.encode(file.getFilename(), "UTF8");
					}					
					
				    externalContext.setResponseHeader("Content-Type", "application/x-download");
				    externalContext.setResponseHeader("Content-Length", file.getContent().length + "");
				    externalContext.setResponseHeader("Content-Disposition", "attachment;filename=\"" + codedfilename + "\"");
					externalContext.getResponseOutputStream().write(file.getContent());
					
					facesContext.responseComplete();
				
				} catch (IOException e) {
					LOGGER.error("IOException: " + e.getMessage());
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages,"general.unexpectedResult"));
					LOGGER.error(e.getMessage(), e);
				
				} catch (Exception e) {
					LOGGER.error("Exception: " + e.getMessage());
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, "general.exception"));
				} 
			}
			
		}
	
	/** Премахва избрания файл
	 * 
	 * @param file
	 */
	public void deleteFile(Files file){
		
		try {
			
			JPA.getUtil().runInTransaction(() -> new FilesDAO(getUserData()).deleteFileObject(file));
			
			this.filesList.remove(file);	
		
		} catch (BaseException e) {			
			e.printStackTrace();LOGGER.error("Грешка при изтриване на лого на регистратурата! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}	

	public Registratura getRegistratura() {
		return registratura;
	}

	public void setRegistratura(Registratura registratura) {
		this.registratura = registratura;
	}

	public boolean isActiveReg() {
		return activeReg;
	}

	public void setActiveReg(boolean activeReg) {
		this.activeReg = activeReg;
	}

	public boolean isUseSEOS() {
		return useSEOS;
	}

	public void setUseSEOS(boolean useSEOS) {
		this.useSEOS = useSEOS;
	}

	public boolean isUseSSEV() {
		return useSSEV;
	}

	public void setUseSSEV(boolean useSSEV) {
		this.useSSEV = useSSEV;
	}
	
	public List<Files> getFilesList() {
		return filesList;
	}

	public void setFilesList(List<Files> filesList) {
		this.filesList = filesList;
	}

	public TreeNode getRootNode() {
		return rootNode;
	}

	public void setRootNode(TreeNode rootNode) {
		this.rootNode = rootNode;
	}

	public boolean isShowSluj() {
		return showSluj;
	}

	public void setShowSluj(boolean showSluj) {
		this.showSluj = showSluj;
	}

	public String getActiveTab() {
		return activeTab;
	}

	public void setActiveTab(String activeTab) {
		this.activeTab = activeTab;
	}

	public boolean isUseWEBEAY() {
		return useWEBEAY;
	}

	public void setUseWEBEAY(boolean useWEBEAY) {
		this.useWEBEAY = useWEBEAY;
	}
	
}
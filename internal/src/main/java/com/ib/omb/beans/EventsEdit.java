package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.EventDAO;
import com.ib.omb.db.dto.Event;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dao.FilesDAO;
import com.ib.system.db.dto.Files;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

@Named
@ViewScoped
public class EventsEdit extends IndexUIbean  implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7767572564756526112L;
	private static final Logger LOGGER = LoggerFactory.getLogger(EventsEdit.class);
	
	private Date decodeDate = new Date();
	private transient EventDAO eventDao;
	private Event event;
	private List<SystemClassif> codeRefClassif = new ArrayList<>();
	private List<SystemClassif> resoursesClassif = new ArrayList<>();
	
	private LazyDataModelSQL2Array eventList; 
	
	private List<Files> filesList;
	private boolean showBtnNotif = false;
	
	private transient List<Object[]> selectedDocs = new ArrayList<>();
	private Integer registratura;
	List<Files> filesListDoc = new ArrayList<>();
	FilesDAO filesDao;
	boolean flagSave = Boolean.FALSE; //дали има поне един файл за записване от избраните документи
	
	@PostConstruct
	void initData() {
		
		
		LOGGER.debug("PostConstruct!!!");	
		eventDao = new EventDAO(getUserData());
		filesDao = new FilesDAO(getUserData());
		
		String param = JSFUtils.getRequestParameter("idObj");
		if ( SearchUtils.isEmpty(param)){
			actionNew();
		}else {
			try {
				registratura = getUserData(UserData.class).getRegistratura();
				JPA.getUtil().runWithClose( () -> event = eventDao.findById(Integer.valueOf(param)));
				
				if(event!=null) {
					JPA.getUtil().runWithClose( () -> this.filesList = filesDao.selectByFileObject(this.event.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_EVENT));		
					if(event.getCodeRefList() != null) {
						for( Integer item : event.getCodeRefList()) {
							String tekst = "";
							SystemClassif scItem = new SystemClassif();
							
								scItem.setCodeClassif(OmbConstants.CODE_CLASSIF_ADMIN_STR);
								scItem.setCode(item);
								tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, item, getUserData().getCurrentLang(), new Date());		
								scItem.setTekst(tekst);
								codeRefClassif.add(scItem);							
						}				
					}
					
					if(event.getResourcesList() != null) {
						for( Integer item : event.getResourcesList()) {
							String tekst = "";
							SystemClassif scItem = new SystemClassif();
							
								scItem.setCodeClassif(OmbConstants.CODE_CLASSIF_EVENT_RESOURCES);
								scItem.setCode(item);
								tekst = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_EVENT_RESOURCES, item, getUserData().getCurrentLang(), new Date());		
								scItem.setTekst(tekst);
								resoursesClassif.add(scItem);							
						}				
					}
					
	
					checkDateForNotif();
				
				}						
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при извличане на събитие! ", e);
				 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} catch (BaseException e) {
				LOGGER.error("Грешка при търсене на събитие! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}
		}					
	}
	
	public void actionNew() {
		event = new Event();	
		try {
			event.setAddrCountry(Integer.parseInt(getSystemData().getSettingsValue("delo.countryBG")));
		} catch (NumberFormatException e) {
			LOGGER.error("NumberFormatException ",e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при работа с базата! ", e);
			 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		codeRefClassif = new ArrayList<>();
		resoursesClassif = new ArrayList<>(); 
		filesList = new ArrayList<>();
	}
	

	private boolean checkData() {
		boolean flagSave = false;
		
		if( event.getEventCode()==null ) {
			JSFUtils.addMessage("eventForm:vid:аutoCompl_input", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "events.vid")));
			flagSave = true;
		}
		
		if( event.getDateOt()==null ) {
			JSFUtils.addMessage("eventForm:dateOtValid", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procList.begin")));
			flagSave = true;
		}else if(event.getId()==null && DateUtils.endDate(event.getDateOt()).before(decodeDate)) {
			JSFUtils.addMessage("eventForm:dateOtValid", FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "refDeleg.dateBeforeToday"));						
			flagSave = true;
		}
		
		if( event.getDateDo()==null ) {
			JSFUtils.addMessage("eventForm:dateToValid", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "procList.end")));
			flagSave = true;
		}else if(event.getDateOt()!= null && event.getDateDo().before(event.getDateOt())) {
			JSFUtils.addMessage("eventForm:dateToValid", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages,  "opis.errInpDates"));			
			flagSave = true;
		}
		
		if(event.getOrganizator()==null ) {
			JSFUtils.addMessage("eventForm:organizator:аutoCompl_input", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "events.organizator")));
			flagSave = true;
		}
		
		if(event.getEventInfo()==null || "".equals(event.getEventInfo())) {
			JSFUtils.addMessage("eventForm:info", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "delo.opisanie")));
			flagSave = true;
		}
		
		return flagSave;
	}
				
	public void actionSave() {
		
		 if(checkData()) {
			 return;
		 }
		 
		try {
				
			JPA.getUtil().runInTransaction(() -> event = this.eventDao.save(event, null, getSystemData()));		
		
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.succesSaveMsg"));					
		} catch (ObjectInUseException  e) {					
			LOGGER.error("ObjectInUseException->{}",e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на организирано събитие! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}			

	}
	
	public void actionDelete() {
		
		try {			
			JPA.getUtil().runInTransaction(() -> this.eventDao.delete(event, getSystemData()));		
			if (this.filesList != null && !this.filesList.isEmpty()) {
				FilesDAO filesDao = new FilesDAO(getUserData());
				for (Files f : this.filesList) {
					filesDao.deleteFileObject(f);
				}
			}
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
		} catch (ObjectInUseException  e) {					
			LOGGER.error("ObjectInUseException->{}",e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на организирано събитие! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}	
		actionNew();
	}
	
	public void searchResourses() {
		try {
			if (event.getResourcesList() == null || event.getResourcesList().isEmpty()) {
				this.eventList = null;

			} else {
				SelectMetadata smd = this.eventDao.createSelectResourcesReport(event.getId(), event.getResourcesList());
				
				this.eventList = new LazyDataModelSQL2Array(smd, "A0");						
			}
		}catch (Exception e) {
			LOGGER.error("Грешка при зареждане на ресурси!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void sendNotif() {
		try {
			JPA.getUtil().runInTransaction(() -> event = this.eventDao.sendFilesNotif(event, getSystemData()));
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "events.successSendNotif"));
			showBtnNotif = false;
		} catch (BaseException e) {
			LOGGER.error("Грешка при изпращане на нотификация за материали! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}
	
	public void addFile() {
		checkDateForNotif();
	}
	
	private void checkDateForNotif() {
	
		
		if(filesList!=null && !filesList.isEmpty()) {
			
			if(event.getDateLastNotif()==null) {
				showBtnNotif = true;
			}else {
				Date lastDateFile = filesList.get(0).getUploadDate();
		
				if(filesList.size()>1) {
					for(int i=1;i<filesList.size();i++) {
						if(filesList.get(i).getUploadDate().after(lastDateFile)) {
							lastDateFile = filesList.get(i).getUploadDate();
						}
					}
				
				}
				
				if(lastDateFile.after(event.getDateLastNotif())) {
					showBtnNotif = true;
				}
			}
		}else {
			showBtnNotif = false;
		}
	}
	
	public Object[] searchDoc() {
		Object[] sDoc = new Object[5];
		String dialogWidgetVar = "PF('mDocSPD').show();";
		PrimeFaces.current().executeScript(dialogWidgetVar);
		return sDoc;
	}
	
	public void actionHideModalDoc() {
		
		if(selectedDocs!=null) {
			flagSave = Boolean.FALSE;
			try {
				
				JPA.getUtil().runInTransaction(() -> { 
					for(Object[] item: selectedDocs) {
						
							filesListDoc = filesDao.selectByFileObject(SearchUtils.asInteger(item[0]), OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
							if(filesListDoc!=null && !filesListDoc.isEmpty()) {
								for(Files file:filesListDoc) {
									//filesList.add(file);							
									filesDao.saveFileObject(file, this.event.getId(),  OmbConstants.CODE_ZNACHENIE_JOURNAL_EVENT);
									flagSave = Boolean.TRUE;
								}
							}			
					}
					if(flagSave) {
						this.filesList = filesDao.selectByFileObject(this.event.getId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_EVENT);
						checkDateForNotif();
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "eventsEidt.saveFilesFromDoc"));
					}else {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "eventsEdit.noFilesFromSelDocs"));
						
					}
				});
			} catch (BaseException e) {
				LOGGER.error("Грешка при търсене и запис на файлове! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}
		}
	}
	
	
	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public List<SystemClassif> getCodeRefClassif() {
		return codeRefClassif;
	}

	public void setCodeRefClassif(List<SystemClassif> codeRefClassif) {
		this.codeRefClassif = codeRefClassif;
	}

	public List<SystemClassif> getResoursesClassif() {
		return resoursesClassif;
	}

	public void setResoursesClassif(List<SystemClassif> resoursesClassif) {
		this.resoursesClassif = resoursesClassif;
	}

	public LazyDataModelSQL2Array getEventList() {
		return eventList;
	}

	public void setEventList(LazyDataModelSQL2Array eventList) {
		this.eventList = eventList;
	}

	public List<Files> getFilesList() {
		return filesList;
	}

	public void setFilesList(List<Files> filesList) {
		this.filesList = filesList;
	}

	public boolean isShowBtnNotif() {
		return showBtnNotif;
	}

	public void setShowBtnNotif(boolean showBtnNotif) {
		this.showBtnNotif = showBtnNotif;
	}

	public List<Object[]> getSelectedDocs() {
		return selectedDocs;
	}

	public void setSelectedDocs(List<Object[]> selectedDocs) {
		this.selectedDocs = selectedDocs;
	}

	public Integer getRegistratura() {
		return registratura;
	}

	public void setRegistratura(Integer registratura) {
		this.registratura = registratura;
	}

}
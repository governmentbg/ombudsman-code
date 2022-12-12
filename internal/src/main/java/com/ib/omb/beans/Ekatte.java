package com.ib.omb.beans;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.utils.EkatteLoader;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.InvalidParameterException;

@Named
@ViewScoped
public class Ekatte extends IndexUIbean implements Serializable {

	
	/**
	 * ЕКАТТЕ
	 */
	private static final long serialVersionUID = 8868071011490316109L;

	private static final Logger LOGGER = LoggerFactory.getLogger(Ekatte.class);
	
	private List<Date> datesList;
	private Date dateValid;
	private boolean visibleFileUpload = false;

		
	@PostConstruct
	void initData() {
				
		LOGGER.debug("PostConstruct!!!");	
		try {
			datesList = new EkatteLoader().selectDateImportList();
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}							
	}
	
	/**
	 * Въвеждане на дата
	 */
	public void actionInputDate() {
		if(dateValid!=null && datesList!=null && !datesList.isEmpty()) {
			if(datesList.get(0).before(dateValid)) {
				visibleFileUpload = true;
			}else {
				visibleFileUpload = false;
				JSFUtils.addGlobalMessage( FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "ekatte.dateValidBefore"));
			}
		}else {
			visibleFileUpload = false;
		}
	}
	
	/**
	 * Прикачване на файл
	 * @param event
	 */
	public void actionUploadFile(FileUploadEvent event) {
		UploadedFile file = event.getFile();
		 
	    System.setProperty("file.encoding", "UTF-8");
	    File newFile = new File(file.getFileName());
	    try (OutputStream outputStream = new FileOutputStream(newFile)){
	    	InputStream  inputStream = file.getInputStream();
	    		    	
	        int read = 0;
	        byte[] bytes = new byte[1024];
	        while ((read = inputStream.read(bytes)) != -1) {
	            outputStream.write(bytes, 0, read);
	        }
	        JPA.getUtil().begin();
			new EkatteLoader().load(newFile, dateValid);
			JPA.getUtil().commit();
			getSystemData().reloadClassif(SysConstants.CODE_CLASSIF_EKATTE, false, false);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString("ui_beanMessages", "general.successMsg"));
			
	    }  catch (EncryptedDocumentException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} catch (InvalidFormatException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} catch (InvalidParameterException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		} finally {
			JPA.getUtil().closeConnection();
		} 
	}
	
	public Date getDateValid() {
		return dateValid;
	}

	public void setDateValid(Date dateValid) {
		this.dateValid = dateValid;
	}

	public boolean isVisibleFileUpload() {
		return visibleFileUpload;
	}

	public void setVisibleFileUpload(boolean visibleFileUpload) {
		this.visibleFileUpload = visibleFileUpload;
	}
		
}
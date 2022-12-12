package com.ib.omb.beans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.ib.omb.db.dao.UserNotificationsDAO;
import com.ib.omb.system.UserData;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;


@Named
@ViewScoped
public class NotificationsList  extends IndexUIbean{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 337727922799131893L;
	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationsList.class);
	public static final String  MSGPLSINS		= "general.pleaseInsert";
	public static final String  ERRDATABASEMSG 	= "general.errDataBaseMsg";
	public static final String  SUCCESSAVEMSG 	= "general.succesSaveMsg";
	public static final String  OBJINUSEMSG 	= "general.objectInUse";
	
	private Integer selectedTipNotif;
	private Integer periodDoc = null;
	private Date docDateFrom;
	private Date docDateTo;
	private LazyDataModelSQL2Array notifList;
	private boolean readF = false;
	private String zaglavie;
	private String text;
	private Integer selectedZaglavie;
	private Integer selectedRead = null;
	private List<SelectItem> zaglavieList = new ArrayList<>();
	private Map<Integer, String> zaglavieMap = new HashMap<>();
	
	@PostConstruct
	public void initData() {
		try {
			List<Object[]> tmp = new UserNotificationsDAO().findTitle();
			for (Object[] objects : tmp) {
				zaglavieList.add(new SelectItem(Integer.valueOf(objects[1].toString()), (String) objects[0]));
				zaglavieMap.put(Integer.valueOf(objects[1].toString()), (String) objects[0]);
			}
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при инициализиране на списък със заглавия при търсенене на нотификации! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
		
	}
	
	public void search() {
		notifList = new LazyDataModelSQL2Array( new UserNotificationsDAO().filterNotifications(docDateFrom, docDateTo, selectedTipNotif, selectedRead, selectedZaglavie == null ? null : zaglavieMap.get(selectedZaglavie), text, ((UserData)getUserData()).getUserAccess(), null), "A1 desc");
	}
	
	public void actionClear() {
		notifList = null;
		periodDoc = null;
		docDateFrom = null;
		docDateTo = null;
		readF = false;
		zaglavie = null;
		text = null;
		selectedTipNotif = null;
		selectedZaglavie = null;
		selectedRead = null;
	}

	public void changePeriodDoc () {
		
    	if (this.periodDoc != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodDoc);
			setDocDateFrom(di[0]);
			setDocDateTo(di[1]);		
    	} else {
    		setDocDateFrom(null);
    		setDocDateTo(null);
		}
    }
	
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др.
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "notifications.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport() , null, null);		
     
	}
	

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "notifications.reportTitle");		
			new CustomExpPreProcess().preProcessPDF(document, title,  dopInfoReport(), null, null);		
						
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
	
	
	/**
	 * подзаглавие за екпсорта
	 */
	public Object[] dopInfoReport() {
		return  null;
	}
	
    
	public void changeDateDoc() { 
		this.setPeriodDoc(null);
	}

	public Integer getPeriodDoc() {
		return periodDoc;
	}

	public void setPeriodDoc(Integer periodDoc) {
		this.periodDoc = periodDoc;
	}

	public Date getDocDateFrom() {
		return docDateFrom;
	}

	public void setDocDateFrom(Date docDateFrom) {
		this.docDateFrom = docDateFrom;
	}

	public Date getDocDateTo() {
		return docDateTo;
	}

	public void setDocDateTo(Date docDateTo) {
		this.docDateTo = docDateTo;
	}

	public Integer getSelectedTipNotif() {
		return selectedTipNotif;
	}

	public void setSelectedTipNotif(Integer selectedTipNotif) {
		this.selectedTipNotif = selectedTipNotif;
	}

	public boolean isReadF() {
		return readF;
	}

	public void setReadF(boolean readF) {
		this.readF = readF;
	}

	public LazyDataModelSQL2Array getNotifList() {
		return notifList;
	}

	public void setNotifList(LazyDataModelSQL2Array notifList) {
		this.notifList = notifList;
	}

	public String getZaglavie() {
		return zaglavie;
	}

	public void setZaglavie(String zaglavie) {
		this.zaglavie = zaglavie;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Integer getSelectedZaglavie() {
		return selectedZaglavie;
	}

	public void setSelectedZaglavie(Integer selectedZaglavie) {
		this.selectedZaglavie = selectedZaglavie;
	}

	public List<SelectItem> getZaglavieList() {
		return zaglavieList;
	}

	public void setZaglavieList(List<SelectItem> zaglavieList) {
		this.zaglavieList = zaglavieList;
	}

	public Integer getSelectedRead() {
		return selectedRead;
	}

	public void setSelectedRead(Integer selectedRead) {
		this.selectedRead = selectedRead;
	}
	
	
	
	
	
	
}

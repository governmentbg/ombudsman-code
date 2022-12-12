package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import com.ib.omb.db.dao.EgovMessagesDAO;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

@Named
@ViewScoped
public class DocWebServiceStatus extends IndexUIbean  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9142346787510855379L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocWebServiceStatus.class);
	
	private Integer period;
	private String sender;
	private Integer senderCode;
	private String recepient;
	private Integer recepientCode;
	private String msgType;//вид на съобщението
	private String docStatus;
	private Integer sentStatus;
	private Integer inOut;//тип на съобщението
	private Date dateOt;
	private Date dateDo;
	private String vidSpravka = "S_SEOS";
	private String nameSender = "";
	private String nameRecepient = "";
	
	private Date decodeDate;
	
	private transient EgovMessagesDAO daoEgov;	
	private LazyDataModelSQL2Array msgList;
	
	private List<SelectItem> msgVidList = new ArrayList<>(); 
	private List<SelectItem> msgStatusList = new ArrayList<>(); 
	private List<SelectItem> msgCommStatusList = new ArrayList<>();
	
	private String errorMess;
	
	private String msgRegReq = "MSG_DocumentRegistrationRequest";
	@PostConstruct
	void initData() {
				
		LOGGER.debug("PostConstruct!!!");								
		daoEgov = new EgovMessagesDAO(getUserData());		
		msgType = msgRegReq;
		inOut = 1;
	
		try {
			ArrayList<Object[]> tmpList = daoEgov.createMsgTypesList();
			
			if(tmpList !=null && !tmpList.isEmpty()){
				for(Object[] item:tmpList) {
					if(item != null && item[0]!=null && item[1]!=null){
						msgVidList.add(new SelectItem( item[0].toString(),item[1].toString()));
					}
				}
			}
			
			tmpList.clear();
			
			tmpList = daoEgov.createCommStatusList();
			
			if(tmpList !=null && !tmpList.isEmpty()){
				for(Object[] item:tmpList) {
					if(item != null && item[0]!=null && item[1]!=null){
						msgCommStatusList.add(new SelectItem( item[0].toString(),item[1].toString()));
					}
				}
			}
			
			tmpList.clear();
			
			tmpList = daoEgov.createMsgStatusList();
			
			if(tmpList !=null && !tmpList.isEmpty()){
				for(Object[] item:tmpList) {
					if(item != null && item[0]!=null && item[1]!=null){
						msgStatusList.add(new SelectItem( item[0].toString(),item[1].toString()));
					}
				}
			}
			
			
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на данните! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}

	
	public void actionSearch() {
		
		try {
			SelectMetadata smd = daoEgov.createFilterMsgSQL(sender, recepient, msgType, docStatus, sentStatus, inOut, dateOt, dateDo, vidSpravka,nameSender,nameRecepient);	
			String defaultSortColumn = "A0";	
			this.msgList = new LazyDataModelSQL2Array(smd, defaultSortColumn);						
		}catch (Exception e) {
			LOGGER.error("Грешка при зареждане данните!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}		
	
	
	
	public void actionClearFilter() {
		period = null;
		sender = "";
		senderCode = null;
		recepient = "";
		recepientCode = null;
		if("S_EDELIVERY".equals(vidSpravka)) {
			msgType = "";
			nameSender = "";
			nameRecepient = "";
		}else {
			msgType = msgRegReq;
		}
		docStatus = "";
		sentStatus = null;
		inOut = 1;
		dateOt = null;
		dateDo = null;
		this.msgList = null;
		
	}
	
	public void changePeriod() {
		
    	if (this.period != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.period);
			dateOt =  di[0];
			dateDo =  di[1];		
    	} else {
    		dateOt = null;
    		dateDo = null;
		}
    }

	public void changeDate() { 
		this.setPeriod(null);
	}
	
	public void actionChangeSender() {
		if(senderCode!=null ){
			try {
				sender = daoEgov.findEgovOrgGuidById(senderCode);
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане данните за подател!", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}
		}else {
			sender = "";
		}
	}
	
	public void actionChangeReceiver() {
		if(recepientCode!=null ){
			try {
				recepient = daoEgov.findEgovOrgGuidById(recepientCode);
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане данните за получател!", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}
		}else {
			recepient = "";
		}
	}
	
	public void actionChangeSenderSSEV() {
		if(senderCode!=null ){
			try {
				sender = daoEgov.findEgovDeliveryOrgGuidById(senderCode);
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане данните за подател!", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}
		}else {
			sender = "";
		}
	}
	
	public void actionChangeReceiverSSEV() {
		if(recepientCode!=null ){
			try {
				recepient = daoEgov.findEgovDeliveryOrgGuidById(recepientCode);
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане данните за получател!", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}
		}else {
			recepient = "";
		}
	}
	
	
	public void actionSelectErr(Object err) {
		
		setErrorMess(SearchUtils.asString(err));
		
	}
	
	public void actionChangeVidSpr() {
		if("S_EDELIVERY".equals(vidSpravka)) {
			msgType = "";
			docStatus = "";
			sentStatus = null;		
		}else {
			msgType = msgRegReq;
			nameSender = "";
			nameRecepient = "";
		}
		sender="";
		senderCode = null;
		recepient="";
		recepientCode=null;
				
	}
	
public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "docWSStatus.titleReport");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport() , null, null);		
     
	}
	
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "docWSStatus.titleReport");		
			new CustomExpPreProcess().preProcessPDF(document, title,  dopInfoReport(), null, null);		
						
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
	
	public Object[] dopInfoReport() {
		Object[] dopInf = null;
		dopInf = new Object[2];
		if(dateOt != null && dateDo != null) {
			dopInf[0] = "период: "+DateUtils.printDate(dateOt) + " - "+ DateUtils.printDate(dateDo);
		} 
	
		return dopInf;
	}
	
	public Date getToday() {
    	return new Date();
    }

	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public String getDocStatus() {
		return docStatus;
	}

	public void setDocStatus(String docStatus) {
		this.docStatus = docStatus;
	}

	public Integer getSentStatus() {
		return sentStatus;
	}

	public void setSentStatus(Integer sentStatus) {
		this.sentStatus = sentStatus;
	}

	public Integer getInOut() {
		return inOut;
	}

	public void setInOut(Integer inOut) {
		this.inOut = inOut;
	}

	public Date getDateOt() {
		return dateOt;
	}

	public void setDateOt(Date dateOt) {
		this.dateOt = dateOt;
	}

	public Date getDateDo() {
		return dateDo;
	}

	public void setDateDo(Date dateDo) {
		this.dateDo = dateDo;
	}

	public LazyDataModelSQL2Array getMsgList() {
		return msgList;
	}

	public void setMsgList(LazyDataModelSQL2Array msgList) {
		this.msgList = msgList;
	}

	public Integer getPeriod() {
		return period;
	}

	public void setPeriod(Integer period) {
		this.period = period;
	}

	public List<SelectItem> getMsgVidList() {
		return msgVidList;
	}

	public void setMsgVidList(List<SelectItem> msgVidList) {
		this.msgVidList = msgVidList;
	}

	public List<SelectItem> getMsgStatusList() {
		return msgStatusList;
	}

	public void setMsgStatusList(List<SelectItem> msgStatusList) {
		this.msgStatusList = msgStatusList;
	}

	public List<SelectItem> getMsgCommStatusList() {
		return msgCommStatusList;
	}

	public void setMsgCommStatusList(List<SelectItem> msgCommStatusList) {
		this.msgCommStatusList = msgCommStatusList;
	}

	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}

	public Integer getSenderCode() {
		return senderCode;
	}

	public void setSenderCode(Integer senderCode) {
		this.senderCode = senderCode;
	}

	public Integer getRecepientCode() {
		return recepientCode;
	}

	public void setRecepientCode(Integer recepientCode) {
		this.recepientCode = recepientCode;
	}

	public String getErrorMess() {
		return errorMess;
	}


	public void setErrorMess(String errorMess) {
		this.errorMess = errorMess;
	}


	public String getVidSpravka() {
		return vidSpravka;
	}


	public void setVidSpravka(String vidSpravka) {
		this.vidSpravka = vidSpravka;
	}


	public String getNameSender() {
		return nameSender;
	}


	public void setNameSender(String nameSender) {
		this.nameSender = nameSender;
	}


	public String getNameRecepient() {
		return nameRecepient;
	}


	public void setNameRecepient(String nameRecepient) {
		this.nameRecepient = nameRecepient;
	}	

}
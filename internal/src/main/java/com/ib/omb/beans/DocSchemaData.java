package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.component.export.PDFOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocShemaDAO;
import com.ib.omb.db.dto.DocShema;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.ObjectInUseException;

@Named
@ViewScoped
public class DocSchemaData extends IndexUIbean  implements Serializable {

	/**
	 * Въвеждане / актуализация на схеми за съхранение на документи
	 * 
	 */
	private static final long serialVersionUID = -8696452712632483268L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocSchemaData.class);
	
	private LazyDataModelSQL2Array schemaList;
	private transient DocShemaDAO schemaDao;
	private DocShema docSchema;					
	private Integer year;
	private String index;
	private String name;
	Calendar calendar;
	
	@PostConstruct
	void initData() {
				
		LOGGER.debug("PostConstruct!!!");	
							
		calendar = Calendar.getInstance();
		calendar.setTime(new Date());
		year = calendar.get(Calendar.YEAR);
		schemaDao = new DocShemaDAO(getUserData());
		actionSearch();				
	}
	
	/**
	 * Инициализания на нов запис
	 * 
	 */
	public void actionNew() {
		docSchema = new DocShema();
		year = calendar.get(Calendar.YEAR);
		docSchema.setFromYear(year);
	}
	
	/**
	 * Метод за търсене
	 * 
	 */
	public void actionSearch() {
		if(year==null) {
			JSFUtils.addMessage("schemaForm:yearSearch", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docSchema.year")));			
			return;
		}
		try {
			SelectMetadata smd = this.schemaDao.createSelectMetadataByYear(year, index, name);			
			String defaultSortColumn = "A0";	
			this.schemaList = new LazyDataModelSQL2Array(smd, defaultSortColumn);						
		}catch (Exception e) {
			LOGGER.error("Грешка при зареждане данните за индекси на номенклатурни дела!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
	}		
	
	/**
	 * Зачистване на търсенето
	 * 
	 */
	public void actionClearFilter() {
		year = calendar.get(Calendar.YEAR);		
		index = "";
		name = "";
		actionSearch();
	}
	
	public boolean checkData() {
		
		boolean save = false;

		if(docSchema.getPrefix()==null || "".equals(docSchema.getPrefix()) ) {
			JSFUtils.addMessage("schemaForm:prefix", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "register.prefix")));
			save = true;
		}
		
		if(docSchema.getFromYear()==null  ) {
			JSFUtils.addMessage("schemaForm:fromYear", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docSchema.validFrom")));
			save = true;
		}
		
		if(docSchema.getCompleteMethod()==null  ) {
			JSFUtils.addMessage("schemaForm:complMethod:аutoCompl_input", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docSchema.complMethodLong")));
			save = true;
		}
		
		if(docSchema.getPeriodType()==null  ) {
			JSFUtils.addMessage("schemaForm:termStore:аutoCompl_input", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docSchema.termStore")));
			save = true;
		}else if(docSchema.getPeriodType().intValue() == OmbConstants.CODE_ZNACHENIE_SHEMA_PERIOD_DEFINED && (docSchema.getYears()==null || docSchema.getYears()<1)) {
			JSFUtils.addMessage("schemaForm:years", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docSchema.years")));
			save = true;
		}
	
		if(docSchema.getShemaName()==null || "".equals(docSchema.getShemaName()) ) {
			JSFUtils.addMessage("schemaForm:name", FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "refCorr.nameUL")));
			save = true;
		}
			
		if(docSchema.getFromYear()!=null && docSchema.getToYear()!=null && docSchema.getFromYear()> docSchema.getToYear()) {
			JSFUtils.addMessage("schemaForm:fromYear", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "docSchema.begYearAfterEndYear"));
			save = true;
		}
		
		return save;		
	}
	
	/**
	 * Метод за запис 
	 * 
	 */
	public void actionSave() {
		
		if(checkData()) {
			return;
		}
			
		try {
				
			JPA.getUtil().runInTransaction(() -> this.docSchema = this.schemaDao.save(docSchema, getSystemData()));		
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_DOC_SHEMA, false, false);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString("ui_beanMessages", "general.succesSaveMsg"));					
			year = calendar.get(Calendar.YEAR);
			actionSearch();
		} catch (ObjectInUseException  e) {			
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());				
		} catch (BaseException e) {				
			LOGGER.error("Грешка при запис на схема за съхранение на документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
				
	}
	
	/**
	 * Метод за избор на ред за редакция
	 * @param id
	 */
	public void actionEdit(Integer id) {
		try {
			JPA.getUtil().runWithClose(() -> this.docSchema = this.schemaDao.findById(id));
		} catch (BaseException e) {
			LOGGER.error("Грешка при търсене на схема за съхрнание на документи! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	/**
	 * Метод за изтриване
	 * 
	 */
	public void actionDelete() {
				
		try {	
			
			JPA.getUtil().runInTransaction(() -> this.schemaDao.delete(docSchema,getSystemData()));	
			getSystemData().reloadClassif(OmbConstants.CODE_CLASSIF_DOC_SHEMA, false, false);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString("ui_beanMessages", "general.successDeleteMsg"));					    
			docSchema = null;
			actionSearch();
		} catch (ObjectInUseException e) {
			LOGGER.error(e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());		
			
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на делегирано право! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			
		}
		
	}

	public LazyDataModelSQL2Array getSchemaList() {
		return schemaList;
	}

	public void setSchemaList(LazyDataModelSQL2Array schemaList) {
		this.schemaList = schemaList;
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

	public DocShema getDocSchema() {
		return docSchema;
	}

	public void setDocSchema(DocShema docSchema) {
		this.docSchema = docSchema;
	}

	public Date getToday() {
    	return new Date();
    }

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
/******************************** EXPORTS **********************************/
	
	/**
	 * за експорт в excel - добавя заглавие и дата на изготвяне на справката и др. - за схема за съхранение на документиv
	 * @param document
	 */
	public void postProcessXLS(Object document) {
		
		String title = getMessageResourceString(LABELS, "schemaList.reportTitle");		  
    	new CustomExpPreProcess().postProcessXLS(document, title, null, null, null);		
     
	}

	/**
	 * за експорт в pdf - добавя заглавие и дата на изготвяне на справката - за схема за съхранение на документи
	 * @param document
	 */
	public void preProcessPDF(Object document)  {
		try{
			
			String title = getMessageResourceString(LABELS, "schemaList.reportTitle");		
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
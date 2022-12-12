package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocWSOptionsDAO;
import com.ib.omb.db.dto.DocWSOptions;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.ObjectInUseException;

@Named 
@ViewScoped
public class DocWSOptionsBean extends IndexUIbean  implements Serializable {			
	
	
	/**
	 * Въвеждане / актуализация на настройки за регистрация на  документи през уеб услуга
	 * 
	 */
	private static final long serialVersionUID = 2313924321643873442L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocWSOptionsBean.class);
	
	private static final String FORM_TABS_REG = "formRegistratura:tabsRegistraturi";
	private static final String EXTERNAL_CODE = ":externalCode";	
	
	private Integer idReg;
	private List<DocWSOptions> docWSOptionsList;
	private DocWSOptions entity;	
	private boolean limitedAccessCh = false; // свободен достъп - false; ограничен достъп = true  	
	private transient DocWSOptionsDAO docWSOptionsDAO;
	private boolean showDataForOptions = false;
	private boolean fromEdit = false;
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct - DocWSOptionsBean!!!");	
		
		try {			
			
			this.idReg = (Integer) JSFUtils.getFlashScopeValue("idReg");
			this.docWSOptionsList = new ArrayList<>();
			this.entity = new DocWSOptions();
			this. docWSOptionsDAO = new DocWSOptionsDAO(getUserData());		
			
			if (idReg != null) {
			
				JPA.getUtil().runWithClose(() -> this.docWSOptionsList = this.docWSOptionsDAO.findByRegistraturaId(this.idReg));
			}
			
			this.showDataForOptions = false;			
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане настройки за регистрация на  документи през уеб услуга по регистратура! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	public void actionEdit(Integer idObj) {
		
		this.showDataForOptions = true;
		this.fromEdit = true;
		
		try {
			
			if (idObj != null) {
		
				JPA.getUtil().runWithClose(() -> this.entity = this.docWSOptionsDAO.findById(idObj));
				
				if(Objects.equals(this.entity.getFreeAccess(), OmbConstants.CODE_ZNACHENIE_DA)) {
					this.limitedAccessCh = false;				
				} else {
					this.limitedAccessCh = true;
				}			
				
			}
		
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на настройка за регистрация на документи през уеб услуга! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
	}
	
	public void actionChangeVidDoc() {
		
		if (this.entity.getDocVid() != null) {		
				
			for (DocWSOptions option : this.docWSOptionsList) {
				
				if (option.getDocVid().equals(this.entity.getDocVid())) {					
					actionEdit(option.getId());
					break;
				}
			}
		}
	}
	
	public boolean checkData() {
		
		boolean save = false;
		
		if(this.entity.getExternalCode() == null || this.entity.getExternalCode().trim().length() == 0){
            JSFUtils.addMessage(FORM_TABS_REG + EXTERNAL_CODE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docWsOptions.extCode")));
            save = true;
        }
		
        if(entity.getDocType() == null){
        	JSFUtils.addMessage(FORM_TABS_REG + ":docVid", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "docVidSetting.typeDoc")));
            save = true;
        }

        if(entity.getDocVid() == null){
        	JSFUtils.addMessage(FORM_TABS_REG + ":docType", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "register.tipDoc")));
            save = true;
        }
        
        // check if there is other record with same externalCode and registratura into database
        if (!this.fromEdit) { 
        	 Number duplicateAccessCodeInRegistratura = JPA.getUtil().getEntityManager().createQuery(
             		"select count(doc.id) from DocWSOptions doc where doc.externalCode =:externalCode", Number.class)
             		.setParameter("externalCode", entity.getExternalCode()).getSingleResult();
             if(duplicateAccessCodeInRegistratura.intValue() > 0){
                 JSFUtils.addMessage(FORM_TABS_REG + EXTERNAL_CODE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "docWsOptions.duplicatExtCode"));
                 save = true;
             }
		
        } else {
			
        	if(this.entity.getExternalCode() != null || !this.entity.getExternalCode().isEmpty()){
        		
				 @SuppressWarnings("unchecked")
				List<Object> list = JPA.getUtil().getEntityManager().createQuery(
		             		"select doc.id from DocWSOptions doc where doc.externalCode =:externalCode")
		             		.setParameter("externalCode", entity.getExternalCode()).getResultList();
				 
				 Integer duplicateAccessCodeInRegistratura = null;
				 if (!list.isEmpty()) {
					 duplicateAccessCodeInRegistratura = ((Number)list.get(0)).intValue();
				 }
				 
	             if(duplicateAccessCodeInRegistratura != null && !duplicateAccessCodeInRegistratura.equals(this.entity.getId())){
	                 JSFUtils.addMessage(FORM_TABS_REG + EXTERNAL_CODE, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "docWsOptions.duplicatExtCode"));
	                 save = true;
	             }
        	}
		}
       
        
		return save;		
	}
	
	public void actionNew() {	
		
		this.showDataForOptions = true;
		this.fromEdit = false;
		this.limitedAccessCh = false;
		this.entity = new DocWSOptions();
		this.entity.setDocRegistratura(this.idReg);		
	}
	
	public void actionSave() {
		
		if(checkData()) {
			return;
		}
		
		try {
			
		 if(this.limitedAccessCh) {
             this.entity.setFreeAccess(OmbConstants.CODE_ZNACHENIE_NE); //ограничен достъп
         } else {
             this.entity.setFreeAccess(OmbConstants.CODE_ZNACHENIE_DA); // свободен достъп 
         }
			
		JPA.getUtil().runInTransaction(() -> this.entity = this.docWSOptionsDAO.save(this.entity));
					
		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.succesSaveMsg"));
		
		actionNew();

		JPA.getUtil().runWithClose(() -> this.docWSOptionsList = this.docWSOptionsDAO.findByRegistraturaId(this.idReg));
		   
		
		} catch (ObjectInUseException e) {
			LOGGER.error("Грешка при запис на настройка за регистрация на документи през уеб услуга - обекта се използва!", e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при запис на настройка за регистрация на документи през уеб услуга! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	public void actionDelete() {
		
		try {
			
			JPA.getUtil().runInTransaction(() -> this.docWSOptionsDAO.delete(this.entity));		
			
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			
			actionNew();	
			
			JPA.getUtil().runWithClose(() -> this.docWSOptionsList = this.docWSOptionsDAO.findByRegistraturaId(this.idReg));
		    
		} catch (ObjectInUseException e) {
			LOGGER.error("Грешка при изтриване на настройка за регистрация на документи през уеб услуга!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());		
		
		} catch (BaseException e) {			
			LOGGER.error("Грешка при изтриване на настройка за регистрация на документи през уеб услуга! ", e);	
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
	}
	
	public Integer getIdReg() {
		return idReg;
	}

	public void setIdReg(Integer idReg) {
		this.idReg = idReg;
	}
	
	public List<DocWSOptions> getDocWSOptionsList() {
		return docWSOptionsList;
	}

	public void setDocWSOptionsList(List<DocWSOptions> docWSOptionsList) {
		this.docWSOptionsList = docWSOptionsList;
	}

	public DocWSOptions getEntity() {
		return entity;
	}

	public void setEntity(DocWSOptions entity) {
		this.entity = entity;
	}

	public boolean isLimitedAccessCh() {
		return limitedAccessCh;
	}

	public void setLimitedAccessCh(boolean limitedAccessCh) {
		this.limitedAccessCh = limitedAccessCh;
	}

	public boolean isShowDataForReg() {
		return showDataForOptions;
	}

	public void setShowDataForReg(boolean showDataForReg) {
		this.showDataForOptions = showDataForReg;
	}

	public boolean isFromEdit() {
		return fromEdit;
	}

	public void setFromEdit(boolean fromEdit) {
		this.fromEdit = fromEdit;
	}

}
package com.ib.omb.beans;

import java.io.Serializable;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.utils.DocDostUtils;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.useractivity.UserActivityData;

@Named
@ViewScoped
public class TransferAccess extends IndexUIbean  implements Serializable {
	
	/**
	 * Прехвърляне на достъп на служители
	 * 
	 */
	private static final long serialVersionUID = -8444079362869595226L;
	private static final Logger LOGGER = LoggerFactory.getLogger(TransferAccess.class);
	
	private static final String FORM_TRANSFER_ACCESS = "formTransferAccess";
	private static final String TAKE_ACCESS_АUTO_COMPL_INPUT = ":takeAccess:аutoCompl_input";
	private static final String GIVE_ACCESS_АUTO_COMPL_INPUT = ":giveAccess:аutoCompl_input";
	
	private Integer codeTakeAccess;
	private String userNameForTake;
	private Integer codeGiveAccess;
	private String userNameForGive;
	private boolean saveAccess;
	private Date decodeDate = new Date();	
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct - TransferAccess!!!");	
		this.saveAccess = true;			
	}
	
	public void actionTransferAccess() {
		
		boolean transfer = true;
		
		if (this.codeTakeAccess == null) {
			JSFUtils.addMessage(FORM_TRANSFER_ACCESS + TAKE_ACCESS_АUTO_COMPL_INPUT, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString("labels", "transferAccess.takeAccessFrom")));
			transfer = false;
		}
		
		if (this.codeGiveAccess == null) {
			JSFUtils.addMessage(FORM_TRANSFER_ACCESS + GIVE_ACCESS_АUTO_COMPL_INPUT, FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString("labels", "transferAccess.giveAccessTo")));
			transfer = false;
		}
		
		if (this.codeTakeAccess != null && this.codeGiveAccess != null && this.codeTakeAccess.equals(this.codeGiveAccess)) { 
			JSFUtils.addMessage(FORM_TRANSFER_ACCESS + GIVE_ACCESS_АUTO_COMPL_INPUT, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "transferAccess.giveAccessToSameSluj"));
			transfer = false;
		}
		
		if (transfer) {
			
			try {			
				// служители, които са логнати в системата и не може да се прехвърли достъпа
				Integer slujForTakeLogInSystem = null;
				Integer slujForGiveLogInSystem = null; 
				boolean existLogInSystem = false;
				
				// регистратурите на двамата служители, за да видим дали са от една и съща
				Integer regIdUserForTake = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.codeTakeAccess, getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
				Integer regIdUserForGive = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.codeGiveAccess, getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA);
								
				if (regIdUserForTake != null && regIdUserForGive != null && !regIdUserForTake.equals(regIdUserForGive)) {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "transferAccess.notFromSameRegistratura"));	
					return;					
				}
				
				if (this.userNameForTake != null && !this.userNameForTake.isEmpty()) { // ако е потребител - да проверявам дали е логнат в системата 
					
					UserActivityData uad = getSystemData().getActiveUsers().check("fakeIP", this.codeTakeAccess);
					
					if (uad != null ) { // логнат е в системата и ще се трупа в един Стринг, за да излезе съобщение					
											
						slujForTakeLogInSystem = this.codeTakeAccess;	
						existLogInSystem = true;
					}								
				}
				
				if (this.userNameForGive != null && !this.userNameForGive.isEmpty()) { // ако е потребител - да проверявам дали е логнат в системата 
					
					UserActivityData uad = getSystemData().getActiveUsers().check("fakeIP", this.codeGiveAccess);
					
					if (uad != null ) { // логнат е в системата и ще се трупа в един Стринг, за да излезе съобщение
						
						slujForGiveLogInSystem = this.codeGiveAccess;	
						existLogInSystem = true;
					}								
				}	
				
				if (slujForTakeLogInSystem != null && slujForGiveLogInSystem == null) { // Съобщение за потребителите, които за логнати в системата и няма да им се преопределя достъпа
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "transferAccess.notExecutedUserLogIn", this.userNameForTake));	
				}
				
				if (slujForGiveLogInSystem != null && slujForTakeLogInSystem == null) { // Съобщение за потребителите, които за логнати в системата и няма да им се преопределя достъпа
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "transferAccess.notExecutedUserLogIn", this.userNameForGive));
				}
				
				if (slujForTakeLogInSystem != null && slujForGiveLogInSystem != null ) { 
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "transferAccess.notExecutedTwoUsersLogIn"));					
				}
				
				if (!existLogInSystem) {
					
					JPA.getUtil().runInTransaction(() ->  new DocDostUtils().transferAccess(codeTakeAccess, codeGiveAccess, getSystemData(), saveAccess, getUserData())); 					 
					
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(beanMessages, "transferAccess.successTransferAccess"));
					this.codeTakeAccess = null;
					this.codeGiveAccess = null;	
					this.userNameForTake = "";
					this.userNameForGive = "";
				}
			
			} catch (BaseException e) {			
				LOGGER.error("Грешка при прехвърляне на достъп на служители! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
			}
		}		
	}
	
	public void actionSelectSlujForTake() {
		
		try {
			
			if (this.codeTakeAccess != null) {
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.codeTakeAccess, getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL))) {
				
					boolean existUser = getSystemData().matchClassifItems(SysConstants.CODE_CLASSIF_USERS, this.codeTakeAccess, this.decodeDate);
					
					if (existUser) {
						this.userNameForTake = getSystemData().decodeItem(SysConstants.CODE_CLASSIF_USERS, this.codeTakeAccess, getCurrentLang(), this.decodeDate);						
					}
				} else {
					
					this.codeTakeAccess = null;
					JSFUtils.addMessage(FORM_TRANSFER_ACCESS + TAKE_ACCESS_АUTO_COMPL_INPUT, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "transferAccess.choiceSluj"));
					
				}
			
			} else {
				this.userNameForTake = "";
			}
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на името на потребителя! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}
		
		
	}
	
	public void actionSelectSlujForGive() {
			
		try {
			
			if (this.codeGiveAccess != null) {
				
				// проверява се типа дали е служител, за да не се избере звено, което няма служители
				Integer refType = (Integer) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_ADMIN_STR, this.codeGiveAccess, getCurrentLang(), this.decodeDate, OmbClassifAdapter.ADM_STRUCT_INDEX_REF_TYPE);

				if (refType.equals(Integer.valueOf(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL))) {
			
					boolean existUser = getSystemData().matchClassifItems(SysConstants.CODE_CLASSIF_USERS, this.codeGiveAccess, this.decodeDate);
					
					if (existUser) {
						this.userNameForGive = getSystemData().decodeItem(SysConstants.CODE_CLASSIF_USERS, this.codeGiveAccess, getCurrentLang(), this.decodeDate);						
					}
				} else {
					
					this.codeGiveAccess = null;
					JSFUtils.addMessage(FORM_TRANSFER_ACCESS + GIVE_ACCESS_АUTO_COMPL_INPUT, FacesMessage.SEVERITY_ERROR, getMessageResourceString(beanMessages, "transferAccess.choiceSluj"));
					
				}
				
			} else {
				this.userNameForGive = "";
			}
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на името на потребителя! ", e);				
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());	
		}		
		
	}
	
	public Integer getCodeTakeAccess() {
		return codeTakeAccess;
	}

	public void setCodeTakeAccess(Integer codeTakeAccess) {		
		this.codeTakeAccess = codeTakeAccess;
	}
	
	public String getUserNameForTake() {
		return userNameForTake;
	}

	public void setUserNameForTake(String userNameForTake) {
		this.userNameForTake = userNameForTake;
	}

	public Integer getCodeGiveAccess() {
		return codeGiveAccess;
	}

	public void setCodeGiveAccess(Integer codeGiveAccess) {
		this.codeGiveAccess = codeGiveAccess;
	}

	public String getUserNameForGive() {
		return userNameForGive;
	}

	public void setUserNameForGive(String userNameForGive) {
		this.userNameForGive = userNameForGive;
	}

	public boolean isSaveAccess() {
		return saveAccess;
	}

	public void setSaveAccess(boolean saveAccess) {
		this.saveAccess = saveAccess;
	}

	public Date getDecodeDate() {
		return new Date(decodeDate.getTime()) ;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
	}	
	
}
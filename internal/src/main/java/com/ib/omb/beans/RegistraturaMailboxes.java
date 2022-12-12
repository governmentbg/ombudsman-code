package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.RegistraturaMailBoxDAO;
import com.ib.omb.db.dto.RegistraturaMailBox;
import com.ib.omb.db.dto.RegistraturaMailBoxVar;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;

/**
 * @author dkarapetrova
 *
 */
@Named
@ViewScoped
public class RegistraturaMailboxes extends IndexUIbean implements Serializable {	
	
	/**
	 * Настройки на пощенски кутии към регистратура
	 * 
	 */
	private static final long serialVersionUID = 6860323749806346913L;
	private static final Logger LOGGER = LoggerFactory.getLogger(RegistraturaMailboxes.class);
	
	private static final String FORM_REGISTRATURA_TABS_REGISTRATURI = "formRegistratura:tabsRegistraturi";
	
	private Integer idReg;
	private RegistraturaMailBox mailbox;
	private RegistraturaMailBoxVar mailboxVar;
	private transient RegistraturaMailBoxDAO mailboxDAO;
	private List<RegistraturaMailBox> mailboxList;
	private List<RegistraturaMailBoxVar> varsList;
	private List<RegistraturaMailBoxVar> varsGlobalList;
	private boolean showDataForMailbox = false;
	
	private Integer settOrDefault;
	private Integer mailKey;

	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {

		LOGGER.debug("PostConstruct - RegistraturaMailboxes!!!");
		
		try {
			
			this.mailbox = new RegistraturaMailBox();
			this.mailboxVar = new RegistraturaMailBoxVar();
			this.mailboxList = new ArrayList<>();			
			this.mailboxDAO = new RegistraturaMailBoxDAO(getUserData());

			this.idReg = (Integer) JSFUtils.getFlashScopeValue("idReg");
	
			if (idReg != null) {				
				
				this.mailboxList = this.mailboxDAO.findRegBoxes(this.idReg);
			}
	
			this.showDataForMailbox = false;			
		
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане данните на пощенските кутии по ид на регистратура!!! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		} finally {
			JPA.getUtil().closeConnection();
		}
	}	
	
	/** 
	 * Зареждане на данни за пощенска кутия
	 * @param idObj
	 **/
	public void actionEdit(Integer idObj) {

		this.showDataForMailbox = true;
		this.settOrDefault = 1;
		this.mailbox = new RegistraturaMailBox();
		this.mailboxVar = new RegistraturaMailBoxVar();
		this.varsList = new ArrayList<>();
		this.varsGlobalList = new ArrayList<>();
		this.mailKey = null;

		try {

			if (idObj != null) {

				JPA.getUtil().runInTransaction(() -> {
					
					this.mailbox = this.mailboxDAO.findById(idObj);
					this.varsList = this.mailboxDAO.findBoxVariables(idObj); 
					this.varsGlobalList = this.mailboxDAO.findGlobalVariables();
				
				});
			}

		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане данните на пощенска кутия! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}

	}
	
	/** 
	 * Нулиране на параметрите
	 * 
	 **/
	public void actionNew() {		
		
		this.showDataForMailbox = true;
		this.settOrDefault = 1;
		this.mailbox = new RegistraturaMailBox();
		this.mailboxVar = new RegistraturaMailBoxVar();
		this.varsList = new ArrayList<>();
		this.varsGlobalList = new ArrayList<>();
	}

	public boolean checkData() {

		boolean save = false;

		if(this.mailbox.getMailboxName() == null || this.mailbox.getMailboxName().isEmpty()) {
			JSFUtils.addMessage(FORM_REGISTRATURA_TABS_REGISTRATURI + ":mailboxName", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "regMailboxes.mailbox")));
			save = true;
		}
		
		if(this.mailbox.getMailboxUsername() == null || this.mailbox.getMailboxUsername().isEmpty()) {
			JSFUtils.addMessage(FORM_REGISTRATURA_TABS_REGISTRATURI + ":mailboxUsername", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "refDeleg.user")));
			save = true;
		}
		
		if(this.mailbox.getMailboxPassword() == null || this.mailbox.getMailboxPassword().isEmpty()) {
			JSFUtils.addMessage(FORM_REGISTRATURA_TABS_REGISTRATURI + ":mailboxPassword", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "regMailboxes.mailboxPassword")));
			save = true;
		}
		
		return save;
	}

	/** 
	 * Запис на пощенска кутия
	 * 
	 **/
	public void actionSave() {

		if (checkData()) {
			return;
		}

		try {
			
			Integer idMailbox = this.mailbox.getId();
			
			this.mailbox.setRegistraturaId(this.idReg); 	

			JPA.getUtil().runInTransaction(() -> { 
				
				this.mailbox = this.mailboxDAO.save(this.mailbox);
				this.varsGlobalList = this.mailboxDAO.findGlobalVariables();
			
			});

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.succesSaveMsg"));	
			
			if (idMailbox == null) {
				this.mailboxList.add(this.mailbox);
			}			

		} catch (BaseException e) {
			LOGGER.error("Грешка при запис на пощенска кутия! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}

	/** 
	 * Изтриване на пощенска кутия
	 * 
	 **/
	public void actionDelete() {

		try {

			JPA.getUtil().runInTransaction(() -> this.mailboxDAO.delete(this.mailbox));

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));
			
			this.mailboxList = this.mailboxDAO.findRegBoxes(this.idReg);

			actionNew();			

		} catch (ObjectInUseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());

		} catch (BaseException e) {
			LOGGER.error("Грешка при изтриване на пощенска кутия! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	}
	
	/** 
	 * 
	 * 
	 **/
	public void actionChangeMailKey() {
		
		if (this.mailKey != null) {
		
			try {
				
				String mailKeyTxt = getSystemData().decodeItem(OmbConstants.CODE_CLASSIF_VARIABLES_SETTINGS_MAIL_ACCOUNT, this.mailKey, getCurrentLang(), new Date());
				
				boolean existInList = false;
				for (RegistraturaMailBoxVar var : this.varsList) {
					if (var.getMailKey().equals(mailKeyTxt)) {
						this.mailboxVar.setId(var.getId()); 
						this.mailboxVar.setMailKey(var.getMailKey());
						this.mailboxVar.setMailValue(var.getMailValue());
						existInList = true;
					}				
				}
				
				if (!existInList) {
					this.mailboxVar.setMailKey(mailKeyTxt);
					this.mailboxVar.setMailValue(null);
				}
			
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}
		
	}
	
	public void actionAddMailboxVar() {
		
		boolean add = true;
		
		if(this.mailKey == null) {
			JSFUtils.addMessage(FORM_REGISTRATURA_TABS_REGISTRATURI + ":mailKey", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "regMailboxes.key")));
			add = false;
		}
		
		if(this.mailboxVar.getMailValue() == null || this.mailboxVar.getMailValue().isEmpty()) {
			JSFUtils.addMessage(FORM_REGISTRATURA_TABS_REGISTRATURI + ":mailValue", FacesMessage.SEVERITY_ERROR, getMessageResourceString(UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "regSettings.value")));
			add = false;
		}
		
		if (add) {
			
			this.mailboxVar.setFlag(2);
			boolean existInList = false;
				
			for (RegistraturaMailBoxVar var : this.varsList) {
				if (var.getId() != null) {
					if (var.getId().equals(this.mailboxVar.getId())) { 
						var.setMailValue(this.mailboxVar.getMailValue()); 
						var.setFlag(2); 
						existInList = true;
						break;
					} 
				} else {
					if (var.getMailKey().equals(this.mailboxVar.getMailKey())) { 
						var.setMailValue(this.mailboxVar.getMailValue()); 
						var.setFlag(2); 
						existInList = true;
						break;
					} 
				}				
			}
			
			if (!existInList) {
				this.varsList.add(this.mailboxVar);
			}
		}
		
		this.mailKey = null;
		this.mailboxVar = new RegistraturaMailBoxVar();	
		
		this.mailbox.setVariables(this.varsList); 
		
	}
	
	public Integer returnMailKey(String mailKeyTxt) {
		
		Integer key = null;
		
		try {
			
			List<SystemClassif> tmpSC = getSystemData().queryClassification(OmbConstants.CODE_CLASSIF_VARIABLES_SETTINGS_MAIL_ACCOUNT, mailKeyTxt, new Date(), getCurrentLang());
			
			for (SystemClassif sc : tmpSC) {
				if (tmpSC.size() == 1) {
					key = sc.getCode();
				} else {
					if (sc.getTekst().equals(mailKeyTxt)) {
						key = sc.getCode();
						break;
					}
				}
			}
			
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		
		return key;	
		
	}
	
	public void actionEditMailboxVar(RegistraturaMailBoxVar selectedVar) {

		boolean existVar = false;

		for (RegistraturaMailBoxVar var : this.varsList) {

			if (var.getId() != null) {

				if (var.getId().equals(selectedVar.getId())) {
					this.mailKey = returnMailKey(var.getMailKey());
					this.mailboxVar.setMailKey(var.getMailKey());
					this.mailboxVar.setMailValue(var.getMailValue());
					this.mailboxVar.setId(var.getId());

					existVar = true;

					break;
				}
			}
		}

		if (!existVar) {

			this.mailKey = returnMailKey(selectedVar.getMailKey());
			this.mailboxVar.setMailKey(selectedVar.getMailKey());
			this.mailboxVar.setMailValue(selectedVar.getMailValue());
		}

	}
	
	public void actionDeleteMailboxVar(RegistraturaMailBoxVar var) {
			
		var.setFlag(3);
		this.varsList.remove(var);
	}

	public Integer getIdReg() {
		return idReg;
	}

	public void setIdReg(Integer idReg) {
		this.idReg = idReg;
	}
	
	public RegistraturaMailBox getMailbox() {
		return mailbox;
	}

	public void setMailbox(RegistraturaMailBox mailbox) {
		this.mailbox = mailbox;
	}

	public RegistraturaMailBoxVar getMailboxVar() {
		return mailboxVar;
	}

	public void setMailboxVar(RegistraturaMailBoxVar mailboxVar) {
		this.mailboxVar = mailboxVar;
	}

	public List<RegistraturaMailBox> getMailboxList() {
		return mailboxList;
	}

	public void setMailboxList(List<RegistraturaMailBox> mailboxList) {
		this.mailboxList = mailboxList;
	}

	public List<RegistraturaMailBoxVar> getVarsList() {
		return varsList;
	}

	public void setVarsList(List<RegistraturaMailBoxVar> varsList) {
		this.varsList = varsList;
	}

	public List<RegistraturaMailBoxVar> getVarsGlobalList() {
		return varsGlobalList;
	}

	public void setVarsGlobalList(List<RegistraturaMailBoxVar> varsGlobalList) {
		this.varsGlobalList = varsGlobalList;
	}

	public boolean isShowDataForMailbox() {
		return showDataForMailbox;
	}

	public void setShowDataForMailbox(boolean showDataForMailbox) {
		this.showDataForMailbox = showDataForMailbox;
	}
	
	public Integer getSettOrDefault() {
		return settOrDefault;
	}

	public void setSettOrDefault(Integer settOrDefault) {
		this.settOrDefault = settOrDefault;
	}

	public Integer getMailKey() {
		return mailKey;
	}

	public void setMailKey(Integer mailKey) {
		this.mailKey = mailKey;
	}

}
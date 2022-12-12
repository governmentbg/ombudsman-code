package com.ib.omb.beans;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;

@Named
@ViewScoped
public class SpecReports extends IndexUIbean  implements Serializable {
private static final Logger LOGGER = LoggerFactory.getLogger(SpecReports.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 4949836964802581271L;
	private List<SystemClassif> sprList; 
	
	
	@PostConstruct
	void initData() {
		try {
			sprList = getSystemData().getSysClassification(OmbConstants.CODE_CLASSIF_RPT_SPEC_ACTIVE, new Date (),getUserData().getCurrentLang());
			if(sprList!=null && !sprList.isEmpty()) {
				
				Iterator<SystemClassif> i = sprList.iterator();
				while (i.hasNext()) {
				SystemClassif s = i.next(); 
					if(!getUserData().hasAccess(OmbConstants.CODE_CLASSIF_RPT_SPEC_ACTIVE, s.getCode())) {
						  i.remove();
					}	  
						
				}
				
				}
			}catch (DbErrorException e) {
				LOGGER.error("Грешка при зареждане на справки! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			} 
		}

	
	public String actionGoTo(Integer code) {
		
		
		String link = "";
		try {
			link = getSystemData().decodeItemDopInfo(OmbConstants.CODE_CLASSIF_RPT_SPEC , code, getUserData().getCurrentLang(), new Date());
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при разкодиране! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		if(link!=null && !"".equals(link)) {
			link+=".jsf?faces-redirect=true";
		}else {
			link = "empty.jsf?faces-redirect=true";
		}
		
		return link;
	}

	public List<SystemClassif> getSprList() {
		return sprList;
	}

	public void setSprList(List<SystemClassif> sprList) {
		this.sprList = sprList;
	}
	
}
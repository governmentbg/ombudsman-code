package com.ib.omb.beans;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.beans.UserList;
import com.ib.indexui.search.UserSearch;
import com.ib.indexui.system.Constants;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.search.ExtendedUserSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.system.db.dto.SystemClassif;

/**
 * Предоставя разширение на стандартния екран за търсене на потребител
 *
 * @author belev
 */
@Named("userList")
@ViewScoped
public class ExtendedUserList extends UserList {

	/**  */
	private static final long serialVersionUID = -7652770038607795221L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedUserList.class);

	private List<SelectItem>	registraturaItemList;
	private List<SelectItem>	businessRoleItemList;
	
	private List<SystemClassif> defPravaClassif;

	/** @return the businessRoleItemList */
	public List<SelectItem> getBusinessRoleItemList() {
		return this.businessRoleItemList;
	}

	/** @return the registraturaItemList */
	public List<SelectItem> getRegistraturaItemList() {
		return this.registraturaItemList;
	}

	public List<SystemClassif> getDefPravaClassif() {
		return defPravaClassif;
	}

	public void setDefPravaClassif(List<SystemClassif> defPravaClassif) {
		this.defPravaClassif = defPravaClassif;
	}

	/** */
	@Override
	public boolean isExtendedArgs() {
		return true;
	}

	/** */
	@Override
	public boolean isExtendedCols() {
		return true;
	}

	/** */
	@Override
	public boolean isRenderArgType() {
		return false; // за деловодството няма да има вид потребител в този екран
	}

	/** */
	@Override
	public boolean isRenderColDateReg() {
		return false;
	}

	/** */
	@Override
	public boolean isRenderColType() {
		return false; // за деловодството няма да има вид потребител в този екран
	}

	/** */
	@Override
	protected UserSearch createSearchObject() {
		this.defPravaClassif = new ArrayList<>();
		return new ExtendedUserSearch();
	}

	/** */
	@PostConstruct
	@Override
	protected void initData() {
		super.initData();

		try {
			
			this.registraturaItemList = createItemsList(false, OmbConstants.CODE_CLASSIF_REGISTRATURI, getCurrentDate(), null);
			this.businessRoleItemList = createItemsList(false, Constants.CODE_CLASSIF_BUSINESS_ROLE, getCurrentDate(), null);
			
			this.defPravaClassif = new ArrayList<>();

		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, "general.errDataBaseMsg"), e);

			LOGGER.error(e.getMessage(), e);
		}
	}
}

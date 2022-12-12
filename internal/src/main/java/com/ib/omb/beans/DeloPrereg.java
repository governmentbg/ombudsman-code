package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.faces.view.facelets.FaceletContext;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DeloDAO;
import com.ib.omb.db.dao.LockObjectDAO;
import com.ib.omb.db.dto.Delo;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * Пререгистриране на преписки/дела
 * 
 * @author s.arnaudova
 **/

@Named
@ViewScoped
public class DeloPrereg extends IndexUIbean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2241968491865783780L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DeloPrereg.class);

	public static final String UIBEANMESSAGES = "ui_beanMessages";
	public static final String BEANMSG = "beanMessages";
	public static final String ERRDATABASEMSG = "general.errDataBaseMsg";
	public static final String SUCCESSAVEMSG = "general.succesSaveMsg";
	public static final String MSGPLSINS = "general.pleaseInsert";
	public static final String LABELS = "labels";
	public static final String PREREGFORM = "deloPreregForm";

	private transient Delo delo;
	private transient DeloDAO dao;
	private UserData ud;

	private String deloRn;

	private transient List<SelectItem> typeList = new ArrayList<>();

	@PostConstruct
	void initData() {
		LOGGER.info("INIT DELOPREREG!!");

		ud = getUserData(UserData.class);
		dao = new DeloDAO(getUserData());

		String param = JSFUtils.getRequestParameter("idObj");
		FaceletContext faceletContext = (FaceletContext) FacesContext.getCurrentInstance().getAttributes()
				.get(FaceletContext.FACELET_CONTEXT_KEY);
		String param2 = (String) faceletContext.getAttribute("isDeloPrereg"); // 0 - актуализация

		Integer deloId = null;
		int isDeloPrereg = 0;
		if (!SearchUtils.isEmpty(param)) {
			deloId = Integer.valueOf(param);

			if (!SearchUtils.isEmpty(param2)) {
				isDeloPrereg = Integer.valueOf(param2);
			}
		}

		boolean checkLockDoc = true;
		if (isDeloPrereg == 0 && deloId != null) {
			checkLockDoc = checkForLock(deloId);
			if (checkLockDoc) {
				lockDelo(deloId);
			}
		}
		if (deloId != null && checkLockDoc) {
			loadDelo(deloId);
		}
		deloRn = delo.getRnDelo();

		try {

			if (this.typeList.isEmpty()) {
				
				// включва всички типове, без "номенклатурно дело"
				this.typeList = createItemsList(false,  OmbConstants.CODE_CLASSIF_DELO_TYPE, new Date(), true)
					.stream()
					.filter(item -> (Integer)item.getValue() != OmbConstants.CODE_ZNACHENIE_DELO_TYPE_NOM)
					.collect(Collectors.toList());
			}
			
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на документ! ", e);
		}

	}

	/**
	 * Проверка за заключен документ
	 * 
	 * @param idObj
	 * @return
	 */
	private boolean checkForLock(Integer idObj) {

		boolean res = true;
		LockObjectDAO daoL = new LockObjectDAO();
		
		try {
			
			Object[] obj = daoL.check(getUd().getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO, idObj);
			if (obj != null) {
				res = false;
				String msg = getSystemData().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR,
						Integer.valueOf(obj[0].toString()), getUserData().getCurrentLang(), new Date()) + " / "
						+ DateUtils.printDate((Date) obj[1]);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_WARN,
						getMessageResourceString(BEANMSG, "delo.lockedDelo"), msg);
			}
			
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при проверка за заключена преписка! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}

		return res;
	}

	/**
	 * Заключване на документ, като преди това отключва всички обекти, заключени от
	 * потребителя
	 * 
	 * @param idObj
	 */
	public void lockDelo(Integer idObj) {

		if (LOGGER.isErrorEnabled()) {
			LOGGER.error(String.format("lockDoc %s", getUd().getPreviousPage()));
		}
		
		LockObjectDAO daoL = new LockObjectDAO();
		try {
			
			JPA.getUtil().runInTransaction(
					() -> daoL.lock(getUd().getUserId(), OmbConstants.CODE_ZNACHENIE_JOURNAL_DELO, idObj, null));
		} catch (BaseException e) {
			LOGGER.error("Грешка при заключване на документ! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		}
	}

	/**
	 * при излизане от страницата - отключва обекта и да го освобождава за
	 * актуализация от друг потребител
	 */
	@PreDestroy
	public void unlockDelo() {

		if (!getUd().isReloadPage()) {
			LockObjectDAO daoL = new LockObjectDAO();
			
			try {

				if (LOGGER.isErrorEnabled()) {
					LOGGER.error(String.format("unlockData 0! %s", getUd().getPreviousPage()));
				}
				
				JPA.getUtil().runInTransaction(() -> daoL.unlock(ud.getUserId()));
			} catch (BaseException e) {
				LOGGER.error("Грешка при отключване на документ! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			}

			getUd().setPreviousPage(null);
		}

		getUd().setReloadPage(false);
	}

	private boolean loadDelo(Integer deloId) {

		boolean bb = false;
		if (deloId != null) {

			try {

				JPA.getUtil().runWithClose(() -> delo = this.dao.findById(deloId));

			} catch (BaseException e) {
				JSFUtils.addErrorMessage(getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e);
				LOGGER.error(e.getMessage(), e);
			}
		}

		return bb;
	}

	public boolean checkDataDelo() {
		boolean flagSave = true;

		if (SearchUtils.isEmpty(delo.getRnDelo())) {
			JSFUtils.addMessage(PREREGFORM + ":regNomerPrereg", FacesMessage.SEVERITY_ERROR, getMessageResourceString(
					UIBEANMESSAGES, MSGPLSINS, getMessageResourceString(LABELS, "repDoc.regnom")));
			flagSave = false;
		}

		if (delo.getDeloDate() == null) {
			JSFUtils.addMessage(PREREGFORM + ":regDatedeloPrereg", FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, MSGPLSINS,
							getMessageResourceString(LABELS, "docu.dateDoc")));
			flagSave = false;

		} else {
			// проверка за бъдещи дати
			if (DateUtils.startDate(delo.getDeloDate()).after(DateUtils.startDate(new Date()))) {
				JSFUtils.addMessage(PREREGFORM + ":regDate", FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(beanMessages, "deloPrereg.dateAfter"));
				flagSave = false;
			}

		}

		return flagSave;
	}

	/**
	 * Запис на документ
	 * 
	 */
	public void actionSavePrereg() {

		if (checkDataDelo()) {
			try {

				if (!Objects.equals(deloRn, delo.getRnDelo())) {
					String errorRn = dao.validateRnDelo(this.delo);
					if (errorRn != null) {
						JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, errorRn);
						return;
					}
		
				}
				

				this.delo.setRnPrefix(null);
				this.delo.setRnPored(null); // за да не се получава разминаване при запис

				JPA.getUtil().runInTransaction(() -> {
					this.delo = this.dao.save(delo, false, getSystemData(), null);

					String tmpDocInfo = "Пререгистриран от: " + ud.getLiceNames() + " ("
							+ DateUtils.printDate(new Date()) + ")";

					if (delo.getDeloInfo() != null) {
						tmpDocInfo += "\n" + delo.getDeloInfo();
					}

					delo.setDeloInfo(tmpDocInfo);
					LOGGER.info(delo.getDeloInfo());

				});

				initData(); // да презареди deloRn
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
						getMessageResourceString(UIBEANMESSAGES, SUCCESSAVEMSG));
			} catch (Exception e) {
				LOGGER.error("Грешка при запис на документа! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
			}
		}

	}

	public Delo getDelo() {
		return delo;
	}

	public void setDelo(Delo delo) {
		this.delo = delo;
	}

	public DeloDAO getDao() {
		return dao;
	}

	public void setDao(DeloDAO dao) {
		this.dao = dao;
	}

	public UserData getUd() {
		return ud;
	}

	public void setUd(UserData ud) {
		this.ud = ud;
	}

	public List<SelectItem> getTypeList() {
		return typeList;
	}

	public void setTypeList(List<SelectItem> typeList) {
		this.typeList = typeList;
	}

	public String getDeloRn() {
		return deloRn;
	}

	public void setDeloRn(String deloRn) {
		this.deloRn = deloRn;
	}

}
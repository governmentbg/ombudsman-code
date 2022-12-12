package com.ib.omb.beans;

import java.io.IOException;
import java.io.Serializable;
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

import org.primefaces.PrimeFaces;
import org.primefaces.component.export.PDFOptions;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.ToggleSelectEvent;
import org.primefaces.event.UnselectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.customexporter.CustomExpPreProcess;
import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.UserNotificationsDAO;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.DateUtils;

/**
 * Изтриване на нотификации
 * 
 * @author s.arnaudova
 **/

@Named
@ViewScoped
public class NotificationsDelete extends IndexUIbean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6181395586245524971L;
	private static final Logger LOGGER = LoggerFactory.getLogger(NotificationsDelete.class);

	public static final String FORMNOTIFDEL = "formNotifDelete";
	public static final String ERRDATABASEMSG = "general.errDataBaseMsg";
	public static final String SUCCESSDELETEMSG = "general.successDeleteMsg";
	public static final String UIBEANMESSAGES = "ui_beanMessages";
	public static final String BEANMSG = "beanMessages";

	private transient UserNotificationsDAO notifDao;
	private Date decodeDate;
	private List<SelectItem> zaglaviqList = new ArrayList<>();
	private Map<Integer, String> zaglaviqMap = new HashMap<>();
	private LazyDataModelSQL2Array notificationsList;
	private transient List<Object[]> notifSelectedTmp = new ArrayList<>();
	private transient List<Object[]> notifSelectedAll = new ArrayList<>();

	/* филтър */
	private Integer periodNotif = null;
	private Date notifDateFrom;
	private Date notifDateTo;
	private Integer selectedRead = null;
	private Integer selectedTypeNotif;
	private Integer selectedZaglavie;
	private String notifText;
	private List<Integer> selectedEmpList; // списък избрани служители
	private List<SystemClassif> slujiteliClassif; // заради selectManyModalA

	private int countDelPeriod;

	@PostConstruct
	public void initData() {
		LOGGER.info("INIT NOTIFICATIONSDELETE >>>>>>>>>>>>>>");
		try {

			decodeDate = new Date();
			notifDao = new UserNotificationsDAO();

			List<Object[]> tmp = notifDao.findTitle();
			for (Object[] objects : tmp) {
				zaglaviqList.add(new SelectItem(Integer.valueOf(objects[1].toString()), (String) objects[0]));
				zaglaviqMap.put(Integer.valueOf(objects[1].toString()), (String) objects[0]);
			}

		} catch (DbErrorException e) {
			LOGGER.error("Грешка при инициализиране на списък със заглавия на нотификации! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					IndexUIbean.getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
		}

	}

	public void changePeriodNotif() {

		if (this.periodNotif != null) {
			Date[] di;
			di = DateUtils.calculatePeriod(this.periodNotif);
			setNotifDateFrom(di[0]);
			setNotifDateTo(di[1]);
		} else {
			setNotifDateFrom(null);
			setNotifDateTo(null);
		}
	}

	public void actionSearchNotif() {
		
		if(notifSelectedAll!=null) {
			notifSelectedAll.clear();
		}
		
		if(notifSelectedTmp!=null) {
			notifSelectedTmp.clear();
		}

		SelectMetadata sm = notifDao.filterNotifications(notifDateFrom, notifDateTo, selectedTypeNotif, selectedRead,
				selectedZaglavie == null ? null : zaglaviqMap.get(selectedZaglavie), notifText, null, selectedEmpList);
		notificationsList = new LazyDataModelSQL2Array(sm, "A1 desc");
	}

	public void actionDeleteNotif() {

		try {

			JPA.getUtil().runInTransaction(() -> notifDao.deleteSelected(notifSelectedAll, getCurrentUserId(), getSystemData()));

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
					getMessageResourceString(UIBEANMESSAGES, SUCCESSDELETEMSG));

			this.notifSelectedAll.clear();
			this.notifSelectedTmp.clear();
			this.notificationsList.loadCountSql();

		} catch (Exception e) {
			JSFUtils.addErrorMessage(getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e);
			LOGGER.error(e.getMessage(), e);
		}

	}

	public boolean checkData() {
		boolean flagSave = true;

		if (getNotifDateTo() == null) {
			JSFUtils.addMessage(FORMNOTIFDEL + ":dateDoDialog", FacesMessage.SEVERITY_ERROR, getMessageResourceString(
					UIBEANMESSAGES, MSGPLSINS, getMessageResourceString(LABELS, "docu.dateDoc")));
			flagSave = false;

		} else {
			if (DateUtils.startDate(getNotifDateTo()).after(new Date())) {
				JSFUtils.addMessage(FORMNOTIFDEL + ":dateDoDialog", FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(BEANMSG, "notif.dateAfterToday"));
				flagSave = false;
			}
		}

		return flagSave;
	}

	public void actionDelforPer() {
		if (checkData()) {

			try {

				JPA.getUtil().runInTransaction(
						() -> this.countDelPeriod = notifDao.deleteByPeriod(notifDateFrom, notifDateTo, getCurrentUserId(), getSystemData()));

				String dialogWidgetVar = "PF('perDelConfirmDialog').hide();";
				PrimeFaces.current().executeScript(dialogWidgetVar);

				if (this.countDelPeriod > 0) {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
							"Успешно изтриване на " + this.countDelPeriod + " прегледани нотификации.");
				} else {
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
							"За избрания период няма прегледани нотификации.");
				}
//				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
//						getMessageResourceString(UIBEANMESSAGES, SUCCESSDELETEMSG));

			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addErrorMessage(getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e);
			}
		}
	}

	public void actionClearNotif() {
		
		periodNotif = null;
		notifDateFrom = null;
		notifDateTo = null;
		selectedRead = null;
		selectedTypeNotif = null;
		selectedZaglavie = null;
		notifText = null;
		notificationsList = null;
		setSlujiteliClassif(new ArrayList<>());
		selectedEmpList = null;
		setNotifSelectedAll(new ArrayList<>());
		setNotifSelectedTmp(new ArrayList<>());

	}

	/**
	 * Експорти
	 */
	public void postProcessXLS(Object document) {
		// за експорт в excel - добавя заглавие и дата на изготвяне на справката.

		String title = getMessageResourceString(LABELS, "notifications.reportTitle");
		new CustomExpPreProcess().postProcessXLS(document, title, dopInfoReport(), null, null);

	}

	public void preProcessPDF(Object document) {

		try {
			// за експорт в pdf - добавя заглавие и дата на изготвяне на справката
			String title = getMessageResourceString(LABELS, "notifications.reportTitle");
			new CustomExpPreProcess().preProcessPDF(document, title, dopInfoReport(), null, null);

		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
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
		// подзаглавие за екпсорта
		return null;
	}

	/**
	 * Множествен избор на нотификации
	 */
	// избран ред
	@SuppressWarnings("rawtypes")
	public void onRowSelect(SelectEvent eventSelected) {

		if (eventSelected != null && eventSelected.getObject() != null) {
			List<Object[]> tmpLst = getNotifSelectedAll();

			Object[] object = (Object[]) eventSelected.getObject();
			boolean bo = true;
			Integer l3 = Integer.valueOf(object[0].toString());
			for (Object[] j : tmpLst) {
				Integer l1 = Integer.valueOf(j[0].toString());
				if (l1.equals(l3)) {
					bo = false;
					break;
				}
			}
			if (bo) {
				tmpLst.add(object);
				setNotifSelectedAll(tmpLst);
			}
		}

	}

	// избрани всички редове от текущата странница
	public void onRowSelectAll(ToggleSelectEvent eventT) {

		ArrayList<Object[]> tmpList = new ArrayList<>();
		tmpList.addAll(getNotifSelectedAll());
		if (eventT.isSelected()) {
			for (Object[] object : getNotifSelectedTmp()) {
				boolean bo = true;
				Long l2 = Long.valueOf(object[0].toString());
				for (Object[] j : tmpList) {
					Long l1 = Long.valueOf(j[0].toString());
					if (l1.equals(l2)) {
						bo = false;
						break;
					}
				}
				if (bo) {
					tmpList.add(object);
				}
			}
		} else {
			List<Object[]> tmpLPageClass = getNotificationsList().getResult();
			for (Object[] object : tmpLPageClass) {
				Long l2 = Long.valueOf(object[0].toString());
				for (Object[] j : tmpList) {
					Long l1 = Long.valueOf(j[0].toString());
					if (l1.equals(l2)) {
						tmpList.remove(j);
						break;
					}
				}
			}
		}
		setNotifSelectedAll(tmpList);
		LOGGER.info("onToggleSelect->>");
	}

	// unselect един ред
	@SuppressWarnings("rawtypes")
	public void onRowUnselect(UnselectEvent eventU) {

		if (eventU != null && eventU.getObject() != null) {
			Object[] object = (Object[]) eventU.getObject();
			ArrayList<Object[]> tmpLst = new ArrayList<>();
			tmpLst.addAll(getNotifSelectedAll());
			for (Object[] j : tmpLst) {
				Integer l1 = Integer.valueOf(j[0].toString());
				Integer l2 = Integer.valueOf(object[0].toString());
				if (l1.equals(l2)) {
					tmpLst.remove(j);
					setNotifSelectedAll(tmpLst);
					break;
				}
			}
		}
	}

	// Запазва селектирането(визуалано) при смяна на странниците
	public void onPageUpdateSelected() {

		if (getNotifSelectedAll() != null && !getNotifSelectedAll().isEmpty()) {
			getNotifSelectedTmp().clear();
			getNotifSelectedTmp().addAll(getNotifSelectedAll());
		}
	}

	public void changeDateNotif() {
		this.setPeriodNotif(null);
	}

	public Integer getPeriodNotif() {
		return periodNotif;
	}

	public void setPeriodNotif(Integer periodNotif) {
		this.periodNotif = periodNotif;
	}

	public Date getNotifDateTo() {
		return notifDateTo;
	}

	public void setNotifDateTo(Date notifDateTo) {
		this.notifDateTo = notifDateTo;
	}

	public Date getNotifDateFrom() {
		return notifDateFrom;
	}

	public void setNotifDateFrom(Date notifDateFrom) {
		this.notifDateFrom = notifDateFrom;
	}

	public Integer getSelectedRead() {
		return selectedRead;
	}

	public void setSelectedRead(Integer selectedRead) {
		this.selectedRead = selectedRead;
	}

	public Integer getSelectedTypeNotif() {
		return selectedTypeNotif;
	}

	public void setSelectedTypeNotif(Integer selectedTypeNotif) {
		this.selectedTypeNotif = selectedTypeNotif;
	}

	public Integer getSelectedZaglavie() {
		return selectedZaglavie;
	}

	public void setSelectedZaglavie(Integer selectedZaglavie) {
		this.selectedZaglavie = selectedZaglavie;
	}

	public Map<Integer, String> getZaglaviqMap() {
		return zaglaviqMap;
	}

	public void setZaglaviqMap(Map<Integer, String> zaglaviqMap) {
		this.zaglaviqMap = zaglaviqMap;
	}

	public List<SelectItem> getZaglaviqList() {
		return zaglaviqList;
	}

	public void setZaglaviqList(List<SelectItem> zaglaviqList) {
		this.zaglaviqList = zaglaviqList;
	}

	public String getNotifText() {
		return notifText;
	}

	public void setNotifText(String notifText) {
		this.notifText = notifText;
	}

	public LazyDataModelSQL2Array getNotificationsList() {
		return notificationsList;
	}

	public void setNotificationsList(LazyDataModelSQL2Array notificationsList) {
		this.notificationsList = notificationsList;
	}

	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}

	public List<Integer> getSelectedEmpList() {
		return selectedEmpList;
	}

	public void setSelectedEmpList(List<Integer> selectedEmpList) {
		this.selectedEmpList = selectedEmpList;
	}

	public List<Object[]> getNotifSelectedTmp() {
		return notifSelectedTmp;
	}

	public void setNotifSelectedTmp(List<Object[]> notifSelectedTmp) {
		this.notifSelectedTmp = notifSelectedTmp;
	}

	public List<Object[]> getNotifSelectedAll() {
		return notifSelectedAll;
	}

	public void setNotifSelectedAll(List<Object[]> notifSelectedAll) {
		this.notifSelectedAll = notifSelectedAll;
	}

	public UserNotificationsDAO getNotifDao() {
		return notifDao;
	}

	public void setNotifDao(UserNotificationsDAO notifDao) {
		this.notifDao = notifDao;
	}

	public List<SystemClassif> getSlujiteliClassif() {
		return slujiteliClassif;
	}

	public void setSlujiteliClassif(List<SystemClassif> slujiteliClassif) {
		this.slujiteliClassif = slujiteliClassif;
	}

}

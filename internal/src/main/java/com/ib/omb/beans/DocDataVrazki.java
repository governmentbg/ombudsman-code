package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.primefaces.event.RowEditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDocDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocDoc;
import com.ib.omb.search.DocSearch;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.DateUtils;
import com.ib.system.utils.SearchUtils;

/**
 * Свързан с документи - актуализация на документ
 * 
 * @author s.arnaudova
 **/

@Named
@ViewScoped
public class DocDataVrazki extends IndexUIbean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4139630254086774812L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocDataVrazki.class);

	private static final String ID_DOCDOC = "idDocDoc";
	public static final String UIBEANMESSAGES = "ui_beanMessages";
	public static final String BEANMSG = "beanMessages";
	public static final String ERRDATABASEMSG = "general.errDataBaseMsg";
	public static final String SUCCESSAVEMSG = "general.succesSaveMsg";

	private transient DocDocDAO dao;
	private DocDoc doc;
	private String rnFullDoc;
	private String rnFullDocOther;
	private boolean refreshList;

	private Doc docEdit;

	private transient List<Object[]> resultList;
	private List<SelectItem> relTypeList;
	private List<SelectItem> relTypeListReverse;
	private Integer relType;
	private Integer docId1;
	private Integer docId2;
	private Integer docDocId;
	private String rnDoc;

	private transient List<Object[]> selectedDocs = new ArrayList<>();

	private int searchFlag = 0; // за да предоврати минаването през searchDoc два пъти, ако след въвеждане на
								// номер на док. веднага се натисне лупата

	/**
	 * При отваряне на таба - винаги минава от тук
	 */
	public void initVrazki() {
		DocData bean = (DocData) JSFUtils.getManagedBean("docData");

		if (bean != null && !Objects.equals(bean.getRnFullDoc(), rnFullDoc)) { // значи за 1-ви път се отваря табчето

			refreshList = true;
			dao = new DocDocDAO(getUserData());
			docEdit = bean.getDocument();
			rnFullDoc = bean.getRnFullDoc();
			docId1 = docEdit.getId();
			relType = OmbConstants.CODE_ZNACHENIE_DOC_REL_TYPE_VRAZKA;
			try {
				List<SystemClassif> items = getSystemData().getSysClassification( OmbConstants.CODE_CLASSIF_DOC_REL_TYPE, new Date(), getCurrentLang());
				relTypeList = new ArrayList<>();
				relTypeListReverse = new ArrayList<>();
				for (SystemClassif item : items) {
					relTypeList.add(new SelectItem(item.getCode(), item.getTekst()));
					relTypeListReverse.add(new SelectItem(item.getCode(), item.getDopInfo()));
				}
				Collections.sort(relTypeList, compatator);
				Collections.sort(relTypeListReverse, compatator);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG));
			}
		}
		if (refreshList) {
			initDoc(); // при запис на връзки през "техен номер", refreshList=true, това осигурява
						// презареждане на списъка
			refreshList = false;
		}
	}

	/**
	 * зарежда резултатите
	 */
	public void initDoc() {

		try {
			
			JPA.getUtil().runWithClose(() -> this.resultList = dao.findDocDocList(docId1));
			
		} catch (Exception e) {
			LOGGER.error("Грешка при извличане на резултати!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG));
		}
	}

	/**
	 * нулира стойностите след запис/изтриване
	 */
	public void cancel(boolean flag) {
		relType = OmbConstants.CODE_ZNACHENIE_DOC_REL_TYPE_VRAZKA;
		selectedDocs.clear();
		if (flag) {
			rnDoc = null;
		}
		searchFlag = 0;
	}

	transient Comparator<SelectItem> compatator = new Comparator<SelectItem>() {
		public int compare(SelectItem s1, SelectItem s2) {
			return (s1.getLabel().toUpperCase().compareTo(s2.getLabel().toUpperCase()));
		}
	};

	/**
	 * зарежда данните за избраните от компонентата документи + директен запис
	 */
	private void loadDocs(List<Object[]> selectedDoc) {

		if (!selectedDoc.isEmpty())
			try {

				for (Object[] obj : this.selectedDocs) {
					docId2 = SearchUtils.asInteger(obj[0]);
					JPA.getUtil().runInTransaction(() -> this.dao.save(null, relType, docId1, docId2, true));
				}
				
				initDoc();
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
						getMessageResourceString(UIBEANMESSAGES, SUCCESSAVEMSG));
			} catch (ObjectInUseException e) {
				LOGGER.error("ObjectInUseException->{}", e.getMessage());
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());		
			} catch (Exception e) {
				LOGGER.error("Грешка при зареждане на документ! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG));
			}
	}

	/**
	 * записва връзка
	 */
	public void actionSaveDoc() {

		try {
			JPA.getUtil().runInTransaction(() -> this.dao.save(null, relType, docId1, docId2, true));
			initDoc();
			cancel(true);

			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
					getMessageResourceString(UIBEANMESSAGES, SUCCESSAVEMSG));
		} catch (ObjectInUseException e) {
			LOGGER.error("ObjectInUseException->{}", e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		} catch (Exception e) {
			LOGGER.error("Грешка при запис на връзка!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG));
		}
	}

	/**
	 * изтрива връзка
	 */
	public void actionDelRel() {

		if (JSFUtils.getRequestParameter(ID_DOCDOC) != null && !"".equals(JSFUtils.getRequestParameter(ID_DOCDOC))) {
			docDocId = Integer.valueOf(JSFUtils.getRequestParameter(ID_DOCDOC));

		}

		try {

			JPA.getUtil().runInTransaction(() -> this.dao.deleteById(docDocId));
			initDoc();
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
					getMessageResourceString(BEANMSG, "doc.delDocDoc"));
		} catch (Exception e) {
			LOGGER.error("Грешка при изтриване на връзка!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG));
		}
	}

	/**
	 * редакция (само вид връзка)
	 */
	public void onRowEdit(RowEditEvent<Object> event) {
		Integer row8 = null;
		if (event != null && event.getObject() != null) {
			Object[] tmpDoc = (Object[]) event.getObject();
			docDocId = Integer.valueOf(tmpDoc[0].toString());
			docId2 = Integer.valueOf(tmpDoc[2].toString());
			relType = Integer.valueOf(tmpDoc[1].toString());
			
			row8 = SearchUtils.asInteger(tmpDoc[8]);
		} 

		try {
			if (docId1.equals(row8)) { // права
				JPA.getUtil().runInTransaction(() -> this.dao.save(docDocId, relType, docId1, docId2, true));
			
			} else { // от другата страна се редактира
				JPA.getUtil().runInTransaction(() -> this.dao.save(docDocId, relType, docId2, docId1, true));
			}
			initDoc();
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
					getMessageResourceString(BEANMSG, "doc.editDocDoc"));
		} catch (ObjectInUseException e) {
			LOGGER.error("ObjectInUseException->{}", e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			initDoc();
		} catch (Exception e) {
			LOGGER.error("Грешка при редакция на връзка!", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG));
		}
	}

	/**
	 * ръчно въвеждане на рег. номер
	 */
	private Object[] searchDocRn(String rnDoc, boolean rnEQ) {
		LOGGER.info("searchDocRn>>>>>>>>>>>>>>>>>>>>>!");
		Object[] sDoc = null;

		if (rnDoc == null) {
			rnDoc = "";
		}

		DocSearch tmp = new DocSearch(getUserData(UserData.class).getRegistratura());
		tmp.setRnDoc(rnDoc);
		tmp.setNotInDocId(docId1);
		tmp.setRnDocEQ(rnEQ);

		tmp.buildQueryComp(getUserData());

		LazyDataModelSQL2Array lazy = new LazyDataModelSQL2Array(tmp, "a1 desc");
		int res = lazy.getRowCount();

		if (res == 0 && rnEQ) {
			LOGGER.info("Не е намерен документ с посочения номер!");
			cancel(false); // искам да ми запази текста в номера
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(BEANMSG, "doc.srchDocDocErr"));
		} else if (res == 1 && rnEQ) {
			List<Object[]> result = lazy.load(0, lazy.getRowCount(), null, null);
			//sDoc = new Object[5];
			sDoc = result.get(0);
			docId2 = SearchUtils.asInteger(sDoc[0]);
			rnFullDocOther = SearchUtils.asString(sDoc[1]) + "/" + DateUtils.printDate(SearchUtils.asDate(sDoc[4]));

			String dialogWidgetVar = "PF('confirmWV').show();";
			PrimeFaces.current().executeScript(dialogWidgetVar);
			searchFlag++;
		} else {
			sDoc = new Object[5];
			String dialogWidgetVar = "PF('mDocSPD').show();";
			PrimeFaces.current().executeScript(dialogWidgetVar);

		}
		return sDoc;

	}

	public void actionHideModal() {

		if (!this.selectedDocs.isEmpty()) {
			loadDocs(selectedDocs);
		

		} else {
		
			LOGGER.info("Няма избрани документи.. ");
			String dialogWidgetVar = "PF('mDocSPD').hide();";
			PrimeFaces.current().executeScript(dialogWidgetVar);
		}
		cancel(true);
	}

	public void actionSearchDocBtn(boolean rnEQ) {
		if (searchFlag == 0) {
			searchDocRn(rnDoc, rnEQ);
		} else {
			searchFlag = 0;
		}

	}

	
	public String actionGoto(Integer idObj) {
		return  "docView.xhtml?faces-redirect=true&idObj=" + idObj;
	}
	
	public DocDocDAO getDao() {
		return dao;
	}

	public void setDao(DocDocDAO dao) {
		this.dao = dao;
	}

	public String getRnFullDoc() {
		return rnFullDoc;
	}

	public void setRnFullDoc(String rnFullDoc) {
		this.rnFullDoc = rnFullDoc;
	}

	public Doc getDocEdit() {
		return docEdit;
	}

	public void setDocEdit(Doc docEdit) {
		this.docEdit = docEdit;
	}

	public List<Object[]> getResultList() {
		return resultList;
	}

	public void setResultList(List<Object[]> resultList) {
		this.resultList = resultList;
	}

	public DocDoc getDoc() {
		return doc;
	}

	public void setDoc(DocDoc doc) {
		this.doc = doc;
	}

	public Integer getRelType() {
		return relType;
	}

	public void setRelType(Integer relType) {
		this.relType = relType;
	}

	public Integer getDocId1() {
		return docId1;
	}

	public void setDocId1(Integer docId1) {
		this.docId1 = docId1;
	}

	public Integer getDocId2() {
		return docId2;
	}

	public void setDocId2(Integer docId2) {
		this.docId2 = docId2;
	}

	public List<SelectItem> getRelTypeList() {
		return relTypeList;
	}

	public void setRelTypeList(List<SelectItem> relTypeList) {
		this.relTypeList = relTypeList;
	}
	
	public List<SelectItem> getRelTypeListReverse() {
		return this.relTypeListReverse;
	}

	public void setRelTypeListReverse(List<SelectItem> relTypeListReverse) {
		this.relTypeListReverse = relTypeListReverse;
	}

	public Integer getDocDocId() {
		return docDocId;
	}

	public void setDocDocId(Integer docDocId) {
		this.docDocId = docDocId;
	}

	public String getRnDoc() {
		return rnDoc;
	}

	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}

	public List<Object[]> getSelectedDocs() {
		return selectedDocs;
	}

	public void setSelectedDocs(List<Object[]> selectedDocs) {
		this.selectedDocs = selectedDocs;
	}

	public String getRnFullDocOther() {
		return rnFullDocOther;
	}

	public void setRnFullDocOther(String rnFullDocOther) {
		this.rnFullDocOther = rnFullDocOther;
	}

	public int getSearchFlag() {
		return searchFlag;
	}

	public void setSearchFlag(int searchFlag) {
		this.searchFlag = searchFlag;
	}

	public boolean isRefreshList() {
		return refreshList;
	}

	public void setRefreshList(boolean refreshList) {
		this.refreshList = refreshList;
	}
}

package com.ib.omb.beans;

import java.io.Serializable;
import java.util.Objects;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocPrilDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocPril;
import com.ib.system.db.JPA;
import com.ib.system.db.SelectMetadata;
import com.ib.system.utils.SearchUtils;


/**
 * Приложения - актуализация на документ
 * 
 * @author s.arnaudova
 **/

@Named
@ViewScoped
public class DocDataPrilojenia extends IndexUIbean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3874099685980030748L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocDataPrilojenia.class);

	public static final String DOCFORMTABS = "docForm:tabsDoc";
	public static final String UIBEANMESSAGES = "ui_beanMessages";
	public static final String BEANMSG = "beanMessages";
	public static final String ERRDATABASEMSG = "general.errDataBaseMsg";
	public static final String SUCCESSAVEMSG = "general.succesSaveMsg";

	private transient DocPrilDAO docPrilDao;
	private DocPril docPril;
	private Doc docEdit;
	private Integer docId;
	private String rnFullDoc;
	private LazyDataModelSQL2Array prilojeniaList;

	/**
	 * При отваряне на таба - винаги минава от тук
	 */
	public void initPrilojenia() {
		LOGGER.info("<<<INIT TAB PRILOJENIA>>>");

		DocData bean = (DocData) JSFUtils.getManagedBean("docData");
		if (bean != null && !Objects.equals(bean.getRnFullDoc(), rnFullDoc)) { // значи за 1-ви път се отваря табчето

			rnFullDoc = bean.getRnFullDoc();
			docPrilDao = new DocPrilDAO(getUserData());
			docEdit = bean.getDocument();
			docId = docEdit.getId();
			docPril = new DocPril();
	
			try {
				createPrilojList();
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(UI_beanMessages, ERRDATABASEMSG));
			}
		}
	}

	public void createPrilojList() {

		SelectMetadata sm = docPrilDao.createSelectPrilList(docId);
		prilojeniaList = new LazyDataModelSQL2Array(sm, null);
	}

	/**
	 * Зареждане на избрано приложение
	 */
	public void loadDataFromPrilojenie(Object[] sPriloj) {

		Integer docPrilojId = SearchUtils.asInteger(sPriloj[0]);

		try {
			JPA.getUtil().runWithClose(() -> {
				docPril = docPrilDao.findById(docPrilojId);
				
			});
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
		}
	}

	/**
	 * Запис на приложение
	 */
	public void actionSavePriloj() {

		if (docPril != null && checkDataPriloj()) {

			try {
				docPril.setDocId(docId);
				JPA.getUtil().runInTransaction(() -> docPrilDao.save(docPril));

				createPrilojList();
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
						getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG));
			} catch (Exception e) {
				LOGGER.error(e.getMessage());
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
						getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
			}
		}
	}

	private boolean checkDataPriloj() {

		boolean flagSave = true;
		boolean flagEmpty = SearchUtils.isEmpty(docPril.getNomer()) && SearchUtils.isEmpty(docPril.getPrilTame());

		if (flagEmpty) {
			JSFUtils.addMessage(DOCFORMTABS + ":prilojNum", FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(BEANMSG, "prilojenia.plsInsert"));
			flagSave = false;
		}

		if (docPril.getMediaType() == null) {
			JSFUtils.addMessage(DOCFORMTABS + ":slcNositel", FacesMessage.SEVERITY_ERROR, getMessageResourceString(
					UI_beanMessages, MSGPLSINS, getMessageResourceString(LABELS, "prilojenia.nositel")));
			flagSave = false;
		}

		return flagSave;
	}

	/**
	 * Ново приложение
	 */
	public void actionNew() {
		docPril = new DocPril();
	}

	/**
	 * Изтриване
	 */
	public void actionDelPriloj() {

		try {

			JPA.getUtil().runInTransaction(() -> docPrilDao.delete(docPril));
			
			actionNew();
			createPrilojList();
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,
					getMessageResourceString(BEANMSG, "prilojenia.delPriloj"));
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,
					getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG), e.getMessage());
		}
	}

	public String getRnFullDoc() {
		return rnFullDoc;
	}

	public void setRnFullDoc(String rnFullDoc) {
		this.rnFullDoc = rnFullDoc;
	}

	public LazyDataModelSQL2Array getPrilojeniaList() {
		return prilojeniaList;
	}

	public void setPrilojeniaList(LazyDataModelSQL2Array prilojeniaList) {
		this.prilojeniaList = prilojeniaList;
	}

	public DocPrilDAO getDocPrilDao() {
		return docPrilDao;
	}

	public void setDocPrilDao(DocPrilDAO docPrilDao) {
		this.docPrilDao = docPrilDao;
	}

	public Doc getDocEdit() {
		return docEdit;
	}

	public void setDocEdit(Doc docEdit) {
		this.docEdit = docEdit;
	}

	public Integer getDocId() {
		return docId;
	}

	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	public DocPril getDocPril() {
		return docPril;
	}

	public void setDocPril(DocPril docPril) {
		this.docPril = docPril;
	}

}

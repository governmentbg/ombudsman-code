package com.ib.omb.components;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.primefaces.PrimeFaces;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.ReorderEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.indexui.utils.TreeUtils;
import com.ib.omb.db.dto.DocReferent;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.omb.system.UserData;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.X;

/** */
@FacesComponent(value = "selectReferents", createTag = true)
public class SelectReferents extends UINamingContainer {
	
	private enum PropertyKeys {
		ROOT, SELECTEDNODE, TEMPCODES, TEMPREFERENTS, SEARCHWORD, SHOWME, SELECTEDTYPE, OPINIONLST, CODEEXT
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SelectReferents.class);
	public static final String	BEANMESSAGES	= "beanMessages";


	private SystemData	systemData	= null;
	private UserData	userData	= null;
	private Date		dateClassif	= null;

	private static final String TYPEREF = "typeRef";
	private static final String SELECTEDREFERENTS = "selectedReferents";
	
	
	/**
	 * Инициалиира комп. в зависимост от типа на реферeнта
	 * @return
	 * @throws DbErrorException
	 */
	public void initRefComp() {	
		int role = (Integer) getAttributes().get(TYPEREF);
		if (role == OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_AGREED) {
			loadOpinionList(OmbConstants.CODE_ZNACHENIE_TASK_TYPE_SAGL);
			setCodeExt(null);
		} else if (role == OmbConstants.CODE_ZNACHENIE_DOC_REF_ROLE_SIGNED) {
			loadOpinionList(OmbConstants.CODE_ZNACHENIE_TASK_TYPE_PODPIS);
			
			//Подписал в документ може да бъде само от собствена регистратура
			try {
				Integer s1 = getSystemData().getRegistraturaSetting(getUserData().getRegistratura(), OmbConstants.CODE_ZNACHENIE_REISTRATURA_SETTINGS_3);
				if(Objects.equals(s1,  OmbConstants.CODE_ZNACHENIE_DA)) { 
					setCodeExt(getUserData().getRegistratura().toString());
				}else {
					setCodeExt(null);
				}
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при извличане на настройки на регистрaтура! ", e);
			}
			
		}else {
			setOpinionLst(null);
			setCodeExt(null);
		}
		
	}
	/**
	 * зарежда списък с мнения, в зависимост от това дали е за съглaсуване или подписване
	 * @param typeTask
	 */
	private void loadOpinionList(int typeTask) {
		List<SystemClassif> tmpOpinionLst = null;
		try {
			tmpOpinionLst = getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_TASK_TYPE_TASK_OPINION, typeTask, getLang(),  new Date());
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на логически списък - статуси - роля! ", e);
			
			setOpinionLst(new ArrayList<>());
			return;
		}
		List<SelectItem> items = new ArrayList<>(tmpOpinionLst.size());
		for (SystemClassif x : tmpOpinionLst) {
			items.add(new SelectItem(x.getCode(), x.getTekst()));				
		}		
		setOpinionLst(items);
	}
	
	/**
	 * autocomplete - търсене на значение
	 *
	 * @param query
	 * @return
	 */
	public List<SystemClassif> actionComplete(String query) {
		List<SystemClassif> result = null;		
		if (query != null && !query.trim().isEmpty()) {
			try {
				Integer codeClassif = getCodeClassif();
				if (codeClassif != null) {
					result = loadClassifList(codeClassif, false, query); // ако има интервали в края или началото - да се махнат																			
				}

			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return result != null ? result : new ArrayList<>();
	}

	/**
	 * При натискане на бутон - "Потвърждение" - модалния за избор от дървото
	 */
	public void actionConfirm() {
		ValueExpression expr = null;
		ELContext ctx = getFacesContext().getELContext();

		// връща списък с значения като DocReferent
		expr = getValueExpression(SELECTEDREFERENTS);
		if (expr != null) {
			expr.setValue(ctx, getTempReferents());
		}

		// извиква remoteCommnad - ако има такава....
		String remoteCommnad = (String) getAttributes().get("onComplete");
		if (remoteCommnad != null && !remoteCommnad.equals("")) {
			PrimeFaces.current().executeScript(remoteCommnad);
		}
		setTempReferents(new ArrayList<>());
	}
	

	/**
	 * Извиква се при пренареждане на списъка с избрани занчения
	 */
	  public void onRowReorder( ReorderEvent  event) {
		if (LOGGER.isDebugEnabled()) {
		   LOGGER.debug("onRowReorder>>> toIndex: {}",event.getToIndex());
		   LOGGER.debug("onRowReorder>>> fromIndex: {}",event.getFromIndex());
		}
		// номер поред !!!нова колонка в базата за сортиране
		  List<DocReferent> tmpOrdLst = getTempReferents();
		  setTempReferents(tmpOrdLst);
	  }
  
	
	/**
	 * autocomplete - връщане на резултат
	 *
	 * @param addItem
	 * @param item
	 */
	public void autoCompleteResult(DocReferent item) {
				
		@SuppressWarnings("unchecked")
		List<DocReferent> selectedReferents = (List<DocReferent>) getAttributes().get(SELECTEDREFERENTS);					

			// проверка за дублиране. Не използвам unique="true" на <p:autoComplete/> защото променя подредбата на избраните значeния!!!
		if (selectedReferents != null && !selectedReferents.isEmpty()) {
			int n =  selectedReferents.size();  
			boolean bb = true;
			for (int i = 0; i < n-1; i++) {
				Integer codeR = selectedReferents.get(i).getCodeRef();
				if(codeR.equals(item.getCodeRef())) {
					bb = false;
					break;
				}
			}			
			if(bb) {
				Integer role = (Integer) getAttributes().get(TYPEREF);					
				selectedReferents.get(n-1).setRoleRef(role);
				selectedReferents.get(n-1).setEventDate(new Date());
				selectedReferents.get(n-1).setDateReg(new Date());
				Object dd = getAttributes().get("idDoc");
				if(dd != null) {
					selectedReferents.get(n-1).setDocId( (Integer)dd);
				}
			}else {
				selectedReferents.remove(n-1);	// дублира се - изтривам последния...
			}
		}
		
		// извиква remoteCommnad - ако има такава....
		String remoteCommnad = (String) getAttributes().get("onComplete");
		if (remoteCommnad != null && !remoteCommnad.equals("")) {
			PrimeFaces.current().executeScript(remoteCommnad);
		}
	}

	/**
	 * Зачиства дървото
	 *
	 * @throws DbErrorException
	 */
	public void clear() throws DbErrorException {
		setSearchWord("");
		setRoot(null);
		loadRoot();
	}

	/**
	 * Зачиства таблицата с избрани значения
	 */
	public void clearCodesTable() {
		setTempCodes(new ArrayList<>());
		setTempReferents(new ArrayList<>());
	}

	/**
	 * Зачиства полетата, в които се връща резултата
	 */
	public void clearInput() {
		ValueExpression expr = getValueExpression("selectedCodes");
		ValueExpression expr3 = getValueExpression(SELECTEDREFERENTS);

		ELContext ctx = getFacesContext().getELContext();
		if (expr != null) {
			expr.setValue(ctx, new ArrayList<Integer>());
		}
	
		if (expr3 != null) {
			expr3.setValue(ctx, new ArrayList<>());
		}
		setTempCodes(new ArrayList<>());
		setTempReferents(new ArrayList<>());
	}



	/**
	 * Зарежда дървото по код на класификация
	 *
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public void loadRoot()  {
		List<DocReferent> selectedReferents = (List<DocReferent>) getAttributes().get(SELECTEDREFERENTS);
		if (selectedReferents != null && !selectedReferents.isEmpty()) {
			setTempReferents(selectedReferents);
		}
		Boolean  readOnly = (Boolean) getAttributes().get("readonly");		
		if(Boolean.TRUE.equals(readOnly)) {
			setShowMe(true);
		}
		else{
			setTempCodes(new ArrayList<>());
			if (getTempReferents() != null) {
				List<Integer> tmpArr = new ArrayList<>();
				for (DocReferent item : getTempReferents()) {
					tmpArr.add(item.getCodeRef());
				}
				setTempCodes(tmpArr);
			}

			setSelectedType("0");
			setSearchWord("");

			loadRootClass();
			
		}
		
	}

	/**
	 * Зарежда дървото по код накласификация за модалния
	 * @throws DbErrorException 
	 */
	@SuppressWarnings("unchecked")
	private void loadRootClass()  {
		Integer codeClassif = getCodeClassif();
		Boolean saveStateTree = (Boolean) getAttributes().get("saveStateTree");	
		if (codeClassif != null) {
			setShowMe(true);
			if (getRoot() == null || !saveStateTree) {
				try {
					Boolean sortByName = (Boolean) getAttributes().get("sortByName");

					List<SystemClassif> classifList = loadClassifList(codeClassif, Boolean.FALSE.equals(sortByName), null);

					Boolean expanded = (Boolean) getAttributes().get("expanded");
					List<Integer> readOnlyCodes = (List<Integer>) getAttributes().get("readOnlyCodes");

					TreeNode rootNode = new TreeUtils().loadTreeData3(classifList, "", sortByName, expanded, readOnlyCodes, null);
					setRoot(rootNode);
				} catch (Exception e) {
					JSFUtils.addErrorMessage("Грешка при зареждане на класификация!");
					LOGGER.error(e.getMessage());
					setShowMe(false);
				}
			}		
		} else {
			JSFUtils.addErrorMessage("Грешка при работата! Не е подаден код на класификация.");
		}		
	}
	
	
	
	
	/**
	 * Зарежда списъка за търсене
	 *
	 * @param codeClassif
	 * @param fixPrev     подава се true ако се иска да се оправят код на предходен при филтриране на класификация по правата на
	 *                    потребителя
	 * @param query
	 * @return
	 * @throws DbErrorException
	 */
	List<SystemClassif> loadClassifList(Integer codeClassif, boolean fixPrev, String query) throws DbErrorException {
		int lang = getUserData().getCurrentLang();
		@SuppressWarnings("unchecked")
		Map<Integer, Object> specifics = (Map<Integer, Object>) getAttributes().get("specifics");
		if(getCodeExt() != null ) {
			if (specifics == null) {
				specifics = new HashMap<>();
			}
			specifics.put((Integer)OmbClassifAdapter.ADM_STRUCT_INDEX_REGISTRATURA, X.of(getCodeExt())); 
			// да се включи специфика и проверка по codeExt - значения, за които codeExt е различен от кода на регистратурата - да не могат да се избират. Спецификата е за аутокомплете 
		}

		List<SystemClassif> classifList;
		if (specifics != null && !specifics.isEmpty()) { // ще се пуска търсене на специфики
			classifList = getSystemData().queryClassification(codeClassif, query, getDateClassif(), lang, specifics);

		} else { // иначе както си е
			if (query == null) {
				classifList = getSystemData().getSysClassification(codeClassif, getDateClassif(), lang);
			} else {
				classifList = getSystemData().queryClassification(codeClassif, query, getDateClassif(), lang);
			}
		}

		return classifList;
	}
	
	/**
	 * autocomplete - избор на значение и връщане на резултат
	 *
	 * @param event
	 */
	public void onItemSelectAutoComplete(SelectEvent<?> event) {
		DocReferent item = (DocReferent) event.getObject();
		autoCompleteResult(item); 
	}

	/**
	 * autocomplete - при зачистване нa избрано значение и връщане на резултат
	 *
	 * @param event
	 */
	public void onItemUnselectAutoComplete(UnselectEvent<?> event) throws DbErrorException {
		DocReferent item = (DocReferent) event.getObject();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("onItemUnselectAutoComplete");
		}
		autoCompleteResult( item);
	}

	/**
	 * Зарежда избраната класификация и я предава към value на bean Избор от дървото
	 *
	 * @param event
	 * @throws DbErrorException
	 */
	public void onNodeSelect(NodeSelectEvent event) throws DbErrorException {
		if (event.getTreeNode().isLeaf() || Boolean.TRUE.equals(getIsSelectNode())) {
			boolean msgcodeExt = false; // true - ако се изисква проверка по codeExt и тя не е успешна - да се покаже съобщение!
			
			Integer role = (Integer) getAttributes().get(TYPEREF);
			SystemClassif selectedItem = (SystemClassif) event.getTreeNode().getData();
			if (LOGGER.isDebugEnabled()) {			
				LOGGER.debug("Selected node from tree: {}", selectedItem.getTekst());
			}
			String selText = selectedItem.getTekst();
			selText = selText.replace("<b>", "");
			selText = selText.replace("</b>", "");
			selectedItem.setTekst(selText); // заради autocomplete полето
			
			Object dd = getAttributes().get("idDoc");
			Integer idD = null;
			if(dd != null) {
				idD = (Integer)dd;
			}
			List<DocReferent> tempCl = getTempReferents();
			List<Integer> tempCodes = getTempCodes();
			if (!tempCodes.contains(selectedItem.getCode())) { // само избраното 
				
				msgcodeExt = getCodeExt()!=null && !getCodeExt().equals(selectedItem.getCodeExt()); 
				if(!msgcodeExt) {
					
					tempCodes.add(selectedItem.getCode());   
					setTempCodes(tempCodes);
					
					DocReferent dr = addReferens(idD, role, selectedItem.getCode(), selectedItem.getTekst());
					
					tempCl.add(dr);
					setTempReferents(tempCl);
				}
			} 
			
			if(msgcodeExt) {
				// включена е проверка по codeExt - значения, за които codeExt е различен от подадения (кода на регистртура) 
				// да се изведе съобщение
				String clientId =  this.getClientId(FacesContext.getCurrentInstance());
				JSFUtils.addMessage(clientId+":treeM",FacesMessage.SEVERITY_ERROR, IndexUIbean.getMessageResourceString(BEANMESSAGES,"ref.msgCodeExt"));
			}
		}else {
			// не позволява да се избере node от дървото
			event.getTreeNode().setExpanded(!event.getTreeNode().isExpanded());
			event.getTreeNode().setSelected(false);
		}
	}
	
	
	private DocReferent addReferens(Integer idD, Integer role, Integer code, String text) {	
		DocReferent dr = new DocReferent();
		dr.setRoleRef(role);  
		dr.setCodeRef(code);
		dr.setEventDate(new Date());
		dr.setTekst(text); // име на избраното лице
		
//		if(role.equals(CODE_ZNACHENIE_DOC_REF_ROLE_AGREED)) {		
//			dr.setAgree(CODE_ZNACHENIE_DA);
//		}		
//		
		dr.setDateReg(new Date());
		dr.setDocId(idD);
		return dr;
	}
	

	/**
	 * @param row 
	 */
	public void removeFromCodes(DocReferent row) {		
		List<DocReferent> tmpArrS = getTempReferents();
		for (DocReferent itemS : tmpArrS) {
			if (itemS.equals(row)) { 
				tmpArrS.remove(itemS);
				setTempReferents(tmpArrS);
				break;
			}
		}
		
		List<Integer> tmpArr = getTempCodes();
		if(tmpArr.contains(row.getCodeRef())) {
			tmpArr.remove(row.getCodeRef());
			setTempCodes(tmpArr);
		}
		
	}



	/**
	 * Търсене на класификация по текст
	 *
	 * @throws DbErrorException
	 */
	public void search() throws DbErrorException {
		Integer codeClassif = getCodeClassif();

		if (codeClassif != null) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Searching for classif with: {}", getSearchWord());
			}

			try {
				List<SystemClassif> classifList = loadClassifList(codeClassif, false, null);

				TreeNode rootNode = new TreeUtils().fTree(classifList, getSearchWord(), true, true, null, null);
				setRoot(rootNode);

			} catch (Exception e) {
				throw new DbErrorException(e);
			}
		} else {
			JSFUtils.addErrorMessage("Грешка при работата! Не е подаден код на класификация.");
		}
	}

	
	
	/** @return */
	private Integer getCodeClassif() {
		return  (Integer)getAttributes().get("codeClassif");
	}
	

	/** @return */
	private Boolean getIsSelectNode() {
		return (Boolean) getAttributes().get("isSelectNode"); // true - да позволи избор на node от дървото; false - избор само на листа...
	}


	/** @return */
	public Date getCurrentDate() {
		return getDateClassif();
	}


	/** @return */
	public Integer getLang() {
		return getUserData().getCurrentLang();
	}


	/** @return */
	public TreeNode getRoot() {
		return (TreeNode) getStateHelper().eval(PropertyKeys.ROOT, null);
	}

	/** @return */
	public String getSearchWord() {
		return (String) getStateHelper().eval(PropertyKeys.SEARCHWORD, "");
	}

	/** @return */
	public TreeNode getSelectedNode() {
		return (TreeNode) getStateHelper().eval(PropertyKeys.SELECTEDNODE, null);
	}

	/** @return */
	public String getSelectedType() {
		return (String) getStateHelper().eval(PropertyKeys.SELECTEDTYPE, "0");
	}

	/** @return */
	@SuppressWarnings("unchecked")
	public List<DocReferent> getTempReferents() {
		List<DocReferent> eval = (List<DocReferent>) getStateHelper().eval(PropertyKeys.TEMPREFERENTS, null);
		return eval != null ? eval : new ArrayList<>();
	}

	/** @return */
	@SuppressWarnings("unchecked")
	public List<Integer> getTempCodes() {
		List<Integer> eval = (List<Integer>) getStateHelper().eval(PropertyKeys.TEMPCODES, null);
		return eval != null ? eval : new ArrayList<>();
	}

	/** @return */
	public boolean isShowMe() {
		return (Boolean) getStateHelper().eval(PropertyKeys.SHOWME, false);
	}
	


	/** @param root */
	public void setRoot(TreeNode root) {
		getStateHelper().put(PropertyKeys.ROOT, root);
	}

	/** @param searchWord */
	public void setSearchWord(String searchWord) {
		getStateHelper().put(PropertyKeys.SEARCHWORD, searchWord.trim());
	}

	/** @param selectedNode */
	public void setSelectedNode(TreeNode selectedNode) {
		getStateHelper().put(PropertyKeys.SELECTEDNODE, selectedNode);
	}

	/** @param selectedType */
	public void setSelectedType(String selectedType) {
		getStateHelper().put(PropertyKeys.SELECTEDTYPE, selectedType);
	}

	/** @param showMe */
	public void setShowMe(boolean showMe) {
		getStateHelper().put(PropertyKeys.SHOWME, showMe);
	}

	/** * @param tempReferents */
	public void setTempReferents(List<DocReferent> tempReferents) {
		getStateHelper().put(PropertyKeys.TEMPREFERENTS, tempReferents);
	}

	/** @param tempCodes */
	public void setTempCodes(List<Integer> tempCodes) {
		getStateHelper().put(PropertyKeys.TEMPCODES, tempCodes);
	}


	/** @return */
	@SuppressWarnings("unchecked")
	public List<SelectItem> getOpinionList() {
		List<SelectItem> eval = (List<SelectItem>) getStateHelper().eval(PropertyKeys.OPINIONLST, null);
		return eval != null ? eval : new ArrayList<>();
	}

	/** * @param statusList */
	public void setOpinionLst(List<SelectItem> opinionLst) {
		getStateHelper().put(PropertyKeys.OPINIONLST, opinionLst);
	}

	/** @return the dateClassif */
	private Date getDateClassif() {
		if (this.dateClassif == null) {
			this.dateClassif = (Date) getAttributes().get("dateClassif");
			if (this.dateClassif == null) {
				this.dateClassif = new Date();
			}
		}
		return this.dateClassif;
	}

	/** @return the systemData */
	private SystemData getSystemData() {
		if (this.systemData == null) {
			this.systemData = (SystemData) JSFUtils.getManagedBean("systemData");
		}
		return this.systemData;
	}

	/** @return the userData */
	public UserData getUserData() {
		if (this.userData == null) {
			this.userData = (UserData) JSFUtils.getManagedBean("userData");
		}
		return this.userData;
	}

	/** @return */
	public String getCodeExt() {
		return (String) getStateHelper().eval(PropertyKeys.CODEEXT, null);
	}
	
	/** @param CodeExt */
	public void setCodeExt(String codeExt) {
		getStateHelper().put(PropertyKeys.CODEEXT, codeExt);
	}
	
}
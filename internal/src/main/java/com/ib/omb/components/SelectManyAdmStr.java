package com.ib.omb.components;

import static com.ib.indexui.system.Constants.CODE_CLASSIF_ITEM_CHOICE;
import static com.ib.system.SysConstants.CODE_DEFAULT_LANG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ValueExpression;
import javax.faces.application.FacesMessage;
import javax.faces.component.FacesComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.context.FacesContext;

import org.primefaces.PrimeFaces;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.event.ReorderEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.UnselectEvent;
import org.primefaces.model.TreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.indexui.utils.TreeUtils;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.UserData;
import com.ib.system.BaseSystemData;
import com.ib.system.BaseUserData;
import com.ib.system.SysConstants;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.SearchUtils;
import com.ib.system.utils.SysClassifUtils;
import com.ib.system.utils.X;

/** 
 * Разширена компоннета за избор на занчения от адм. структура и група служители
 * Класификацията на адм. структура  се подава като параметър, за да се осигури по-голям гъвкавост при необходсимост 
 * Включва проверка за заместване на служител 
 * */
@FacesComponent(value = "selectManyAdmStr", createTag = true)
public class SelectManyAdmStr extends UINamingContainer {

	private enum PropertyKeys {
		ROOT, SELECTEDNODE, TEMPCODES, TEMPCLASSIFS, SEARCHWORD,  SHOWME, SELECTEDTYPE, REORDER, DOPINFO, SELECTEDDELEG, DELEG, CODEEXT,
		TREETYPE
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(SelectManyAdmStr.class);

	private static String[] lblRadioBtnDefault = { "избрано", "избрано и подчинени", "подчинени" };
	static { // зареждам ги от класификацията. ако нещо избие ще останат горните имена по дефолт
		try {
			BaseSystemData sd = (BaseSystemData) JSFUtils.getManagedBean("systemData");
			List<SystemClassif> list = sd.getSysClassification(CODE_CLASSIF_ITEM_CHOICE, new Date(), CODE_DEFAULT_LANG);
			SysClassifUtils.doSortClassifPrev(list); // важна е сортировката

			lblRadioBtnDefault = new String[] { list.get(0).getTekst(), list.get(1).getTekst(), list.get(2).getTekst() };
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	private BaseSystemData	systemData	= null;
	private BaseUserData	userData	= null;
	private Date			dateClassif	= null;
	

	public  static final  String SELECTEDTEXT 	 	 = "selectedText";
	public  static final  String SELECTEDCLASSIFS 	 = "selectedClassifs";
	public  static final  String SELECTEDCODES 		 = "selectedCodes";
	public  static final  String ONCOMPLETE			 = "onComplete";
	/**
	 * само избраното
	 */
	public  static final  int SELV_0 	 = 0;
	/**
	 * избрано и подчинени
	 */
	public  static final  int SELV_1 	 = 1;
	/**
	 * подчинени
	 */
	public  static final  int SELV_2	 = 2;
	
	
	/**
	 * preRenderComponent
	 */
	public void initAutoCompl() {
		setTempClassifs(new ArrayList<>());
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("initAutoCompl");
		}
	}
	
	
	/**
	 * autocomplete - търсене на значение
	 *
	 * @param query
	 * @return
	 */
	public List<SystemClassif> actionComplete(String query) {
		List<SystemClassif> result = null;
		int compType = (Integer) getAttributes().get("compType"); // dropdown autocomplete -> compType==4
		if (query != null && !query.trim().isEmpty() || compType == 4) {
			try {
				Integer codeClassif = getCodeClassif();
				if (codeClassif != null) {
					result = loadClassifList(codeClassif, false, query); // ако има интервали в края или началото - да се махнат
																			// ли?
				}

			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return result != null ? result : new ArrayList<>();
	}

	/**
	 * При натискане на бутон - "Потвърждение" - модалния за избор от дървото
	 *
	 * @throws DbErrorException
	 */
	public void actionConfirm() throws DbErrorException {

		List<Integer> tmpArr = getTempCodes();
		String seltxt = sbSelectedText(tmpArr);

		// връща текстовете, разделени със запетя
		ValueExpression expr = getValueExpression(SELECTEDTEXT);
		ELContext ctx = getFacesContext().getELContext();
		if (expr != null) {
			expr.setValue(ctx, seltxt);
		}
		
		// връща списък с избрани кодове
		expr = getValueExpression(SELECTEDCODES);
		ctx = getFacesContext().getELContext();
		if (expr != null) {
			expr.setValue(ctx, tmpArr);
		}
		
		if(isReorder()) {
		  List<SystemClassif> notOrdLst = getTempClassifs();
		  List<SystemClassif> ordLst = new ArrayList<>();
			for (Integer code : tmpArr) {
				for (SystemClassif sc : notOrdLst) {
					if(code.equals(Integer.valueOf(sc.getCode()))) {
						ordLst.add(sc);
						notOrdLst.remove(sc);
						break;
					}
				}
			}
			setTempClassifs(ordLst);
			setReorder(false);
		}
		
		// връща списък с значения като systemCalssif
		expr = getValueExpression(SELECTEDCLASSIFS);
		if (expr != null) {
			expr.setValue(ctx, getTempClassifs());
		}
		
		// setTempCodes(new ArrayList<Integer>()); //коментирано е заради autoComplete....
		setTempClassifs(new ArrayList<>());
		
		// извиква remoteCommnad - ако има такава....
		String remoteCommnad = (String) getAttributes().get(ONCOMPLETE);
		if (remoteCommnad != null && !remoteCommnad.equals("")) {
			PrimeFaces.current().executeScript(remoteCommnad);
		}
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
			  List<Integer> tmpOrdLst = getTempCodes();
			  setTempCodes(tmpOrdLst);
			  setReorder(true); 
		  }
	  

	
	/**
	 * autocomplete - връщане на резултат
	 *
	 * @param addItem
	 * @param item
	 * @throws DbErrorException
	 */
	public void autoCompleteResult(boolean addItem, SystemClassif item) throws DbErrorException {
		boolean addItem1 = true; 
	
		@SuppressWarnings("unchecked")
		List<SystemClassif> selectedC= (List<SystemClassif>) getAttributes().get(SELECTEDCLASSIFS);				
		
		// проверка за дублиране. Не използвам unique="true" на <p:autoComplete/> защото променя подредбата на избраните значeния!!!
		Integer itemCode = item.getCode();
		if (selectedC != null && !selectedC.isEmpty()) {
			if(addItem) {
				addItem1 = addItemDelegate(itemCode, "1"); //  само, ако се добавя
			}
						
			int n =  selectedC.size(); 
			if(addItem1) {	
				checkDouble(n, selectedC, item.getCode(), addItem );		
				
			} else {
				selectedC.remove(n-1);	//  изтривам последния, за да оставя избора от модалния за заместване!!!...
			}			
		}
		
		if(addItem1) {
			returnResultAuto(selectedC);			
		}
	}
	
	/**
	 * Премахва дублираните значения
	 * @param n
	 * @param selectedC
	 * @param itemCode
	 * @param addItem
	 */
	private void checkDouble(int n, List<SystemClassif> selectedC, Integer itemCode, boolean addItem ) {
		for (int i=0; i < n-1; i++) {
			Integer codeR = selectedC.get(i).getCode();
			if(codeR.equals(itemCode) && addItem) {//
				selectedC.remove(n-1);	// дублира се - изтривам последния...
				break;
			}
		}	
	}
	
	
	/**
	 * Връща резултата при избор от autocomplete
	 * @param selectedC
	 * @throws DbErrorException
	 */
	private	void returnResultAuto(List<SystemClassif> selectedC) throws DbErrorException {
		ELContext ctx = getFacesContext().getELContext();
		
		// връща списък с значения като systemCalssif
		ValueExpression expr = getValueExpression(SELECTEDCLASSIFS);
		setTempClassifs(selectedC);
		if (expr != null) {
			expr.setValue(ctx, selectedC);
		}
		
		// връща списък с избрани кодове и/или текст, ако се изискват
		expr = getValueExpression(SELECTEDCODES);
		ValueExpression expr2 = getValueExpression(SELECTEDTEXT);
		List<Integer> tmpArr  = new ArrayList<>();				
		if (expr != null || expr2 != null) {
			for(SystemClassif sc: selectedC) {
				tmpArr.add(sc.getCode());
			}
		}
		if (expr != null) {
			setTempCodes(tmpArr);
			expr.setValue(ctx, tmpArr);
		}			
		if (expr2 != null) {
			String seltxt = sbSelectedText(tmpArr);
			expr2.setValue(ctx, seltxt);
		}
	
		// извиква remoteCommnad - ако има такава....
	    String 	remoteCommnad = (String) getAttributes().get(ONCOMPLETE);
		if (remoteCommnad != null && !remoteCommnad.equals("")) {
			PrimeFaces.current().executeScript(remoteCommnad);
		}
	}
	

	  
	/**
	 * Зачиства дървото
	 *
	 * @throws DbErrorException
	 */
	public void clear() {
		setSearchWord("");
		setRoot(null);
		loadRoot();
	}

	/**
	 * Зачиства таблицата с избрани значения
	 */
	public void clearCodesTable() {
		setTempCodes(new ArrayList<>());
		setTempClassifs(new ArrayList<>());
	}

	/**
	 * Зачиства полетата, в които се връща резултата
	 */
	public void clearInput() {
		ValueExpression expr = getValueExpression(SELECTEDCODES);
		ValueExpression expr2 = getValueExpression(SELECTEDTEXT);
		ValueExpression expr3 = getValueExpression(SELECTEDCLASSIFS);

		ELContext ctx = getFacesContext().getELContext();
		if (expr != null) {
			expr.setValue(ctx, new ArrayList<Integer>());
		}
		if (expr2 != null) {
			expr2.setValue(ctx, null);
		}
		if (expr3 != null) {
			expr3.setValue(ctx, new ArrayList<>());
		}
		setTempCodes(new ArrayList<>());
		setTempClassifs(new ArrayList<>());
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
	public String[] getLblRadioBtnDefault() {
		return lblRadioBtnDefault;
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
	public int getSelectedType() {
		return (int) getStateHelper().eval(PropertyKeys.SELECTEDTYPE, SELV_0);
	}
	
	/** @param selectedType */
	public void setSelectedType(int selectedType) {
		getStateHelper().put(PropertyKeys.SELECTEDTYPE, selectedType);
	}
	
//	/** @param selectedType */
//	public void setSelectedType(String selectedType) {
//		getStateHelper().put(PropertyKeys.SELECTEDTYPE, selectedType);
//	}
//	public String getSelectedType() {
//		return (String) getStateHelper().eval(PropertyKeys.SELECTEDTYPE, "0");
//	}
	/** @return */
	@SuppressWarnings("unchecked")
	public List<SystemClassif> getTempClassifs() {
		List<SystemClassif> eval = (List<SystemClassif>) getStateHelper().eval(PropertyKeys.TEMPCLASSIFS, null);
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

	/**
	 * Зарежда дървото по код на класификация и вече избрани значения, ако има такива
	 *
	 * @throws DbErrorException
	 */
	@SuppressWarnings("unchecked")
	public void loadRoot()  {
		List<SystemClassif> selectedClassifs = (List<SystemClassif>) getAttributes().get(SELECTEDCLASSIFS);
		if (selectedClassifs != null && !selectedClassifs.isEmpty()) {
			setTempClassifs(selectedClassifs);
		}	
	
		setTempCodes(new ArrayList<>());
		List<Integer> selectedCodes = (List<Integer>) getAttributes().get(SELECTEDCODES);
		// loads already selected codes
		if (selectedCodes != null && !selectedCodes.isEmpty()) {
			setTempCodes(selectedCodes);
		} else if (getTempClassifs() != null && !getTempClassifs().isEmpty()) {
			// ако не е подаден списък с кодове, а само списък с systemClassifs
			List<Integer> tmpArr = new ArrayList<>();
			for (SystemClassif item : getTempClassifs()) {
				tmpArr.add(item.getCode());
			}
			setTempCodes(tmpArr);
		}	
		setSearchWord("");
		setRoot(null);
		Integer codeClassif = getCodeClassif();
		boolean bb = false;
		if (getTreeType().equals(ADM_LIST)){
			setSelectedType(SELV_0);			
		} else {
			codeClassif = OmbConstants.CODE_CLASSIF_GROUP_EMPL;
			bb = true;
		}			
		loadRootClass(codeClassif, bb);
	}
	
	
	
	/**
	 * Зарежда дървото по код накласификация за модалния
	 * @throws DbErrorException 
	 */
	@SuppressWarnings("unchecked")
	private void loadRootClass(	Integer codeClassif, boolean filtered)  {		
		if (codeClassif != null) {
			setShowMe(true);			
			if (getRoot() == null) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Loading tree with classifCode:{}", codeClassif);
				}
				try {
					Boolean sortByName = (Boolean) getAttributes().get("sortByName");
					TreeNode rootNode = null;
					Boolean expanded = (Boolean) getAttributes().get("expanded");
					List<Integer> readOnlyCodes = (List<Integer>) getAttributes().get("readOnlyCodes");

					List<SystemClassif> classifList;
					if(filtered) {
						// групите да са филтрирани в рамките на регистртурата
						sortByName = true; // Задължително трябва да е по азбучен ред, иначе не може да се изгради дървото!!!
						readOnlyCodes = null;
						expanded = true;
						Map<Integer, Object> specRegistratura = Collections.singletonMap(OmbClassifAdapter.REG_GROUP_INDEX_REGISTRATURA, ((UserData) getUserData()).getRegistratura());
						classifList = getSystemData().queryClassification(codeClassif, null, getDateClassif(), getLang(), specRegistratura);
					}else {
						classifList = loadClassifList(codeClassif, Boolean.FALSE.equals(sortByName), null);
					}
					rootNode = new TreeUtils().loadTreeData3(classifList, "", sortByName, expanded, readOnlyCodes, null);
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
	 * autocomplete - избор на значение и връщане на резултат
	 *
	 * @param event
	 * @throws DbErrorException
	 */
		public void onItemSelectAutoComplete(SelectEvent<?> event) throws DbErrorException {
		SystemClassif item = (SystemClassif) event.getObject();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("selectManyModalA.... onItemSelectAutoComplete: {}", item);
		}
		autoCompleteResult(true, item);
	}

	/**
	 * autocomplete - при зачистване нa избрано значение и връщане на резултат
	 *
	 * @param event
	 * @throws DbErrorException
	 */
	
	public void onItemUnselectAutoComplete(UnselectEvent<?> event) throws DbErrorException {
		SystemClassif item = (SystemClassif) event.getObject();
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("onItemUnselectAutoComplete");
		}
		autoCompleteResult(false, item);
	}

	

	/**
	 * Зарежда избраната класификация и я предава към value на bean Избор от дървото
	 *
	 * @param event
	 * @throws DbErrorException
	 */
	public void onNodeSelect(NodeSelectEvent event) {
		if(getTreeType().equals(ADM_LIST)) {
			onNodeSelectAdmStr(event);
		} else {
			onNodeSelectAdmGr(event);		
		}
	}
	
	/**
	 * Изборът е от дървото на адм. стуктура
	 * @param event
	 */
	private void onNodeSelectAdmStr(NodeSelectEvent event) {
		Boolean showRadioBtn = (Boolean) getAttributes().get("showRadioBtn");
		int selV = getSelectedType();
		
		boolean bb = true;
		if (!(event.getTreeNode().isLeaf() || Boolean.TRUE.equals(getIsSelectNode()))) { // дали е позволено е да се избира всичко
			if (!(showRadioBtn && selV == SELV_2)) { // ако е забранено избирането на node, но е избран радиобутон "Подчинени"
				bb = false;
			}
			// не позволява да се избере node от дървото
			event.getTreeNode().setExpanded(!event.getTreeNode().isExpanded());
			event.getTreeNode().setSelected(false);
		}
		if (bb) {
			SystemClassif selectedItem = (SystemClassif) event.getTreeNode().getData();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Selected node from tree: {}", selectedItem.getTekst());
			}
			String selText = selectedItem.getTekst();
			selText = selText.replace("<b>", "");
			selText = selText.replace("</b>", "");
			selectedItem.setTekst(selText); // заради autocomplete полето

			try {
				onNodeSelectResult(selectedItem, selV );
			} catch (DbErrorException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addErrorMessage("Грешка при работата  с базата данни!");
			}			
		}
	}
	
	/**
	 * Изборът е от дървото на групи служители
	 * @param event
	 * @throws DbErrorException 
	 */
	private void onNodeSelectAdmGr(NodeSelectEvent event)  {
		SystemClassif selectedItem = (SystemClassif) event.getTreeNode().getData();
		// в групата има специфика на всички служители (кодовете с разделител ',')
		String emplCodes;
		try {
			emplCodes = (String) getSystemData().getItemSpecific(OmbConstants.CODE_CLASSIF_GROUP_EMPL, selectedItem.getCode(), SysConstants.CODE_DEFAULT_LANG, getDateClassif(), OmbClassifAdapter.REG_GROUP_INDEX_MEMBERS);
			if (emplCodes != null) { // може и да е празна
				String[] codes = emplCodes.split(",");
				for (String item : codes) {
					SystemClassif sc= getSystemData().decodeItemLite(Constants.CODE_CLASSIF_ADMIN_STR, Integer.valueOf(item), getLang(), getDateClassif(), false);
					if (sc != null) { // този е напуснал към подадената дата и няма как да участва в групата
						onNodeSelectResult(sc, SELV_0);
					}
				}			
			}else {
				//В групата няма служители!
				String clientId =  this.getClientId(FacesContext.getCurrentInstance());
				JSFUtils.addMessage(clientId+":treeM",FacesMessage.SEVERITY_WARN, IndexUIbean.getMessageResourceString("beanMessages", "docu.admGrSlujIsEmpty"));
			}
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addErrorMessage("Грешка при работата  с базата данни!");
		}
		
	}
	
	/**
	 * 
	 * @param selectedItem - избрания елем. от дървото
	 * @param selV - типа на избора - "избрано", "избрано и подчинени", "подчинени" или ГРУПИ- за груоите се подава SELV_0 - само избраното
	 * @throws DbErrorException
	 */
	private void onNodeSelectResult(SystemClassif selectedItem, int selV ) throws DbErrorException {
		Object[] codeExtCheck =  (Object[] ) getAttributes().get("codeExtCheck"); 
		List<SystemClassif> tempCl = getTempClassifs();
		List<Integer> tempCodes = getTempCodes();
		if(tempCodes==null) {
			tempCodes = new ArrayList<>();
		}
	
		boolean msgcodeExt = false; // true - ако се изисква проверка по codeExt и тя не е успешна - да се покаже съобщение!
		if (selV == SELV_0 && !tempCodes.contains(selectedItem.getCode())) { // само избраното - by default
			msgcodeExt = onNodeSelect1( selectedItem, tempCodes, tempCl);
		} else if (selV != SELV_0){ // с подчинени
			msgcodeExt = onNodeSelect2( selectedItem, tempCodes, tempCl, selV);
		}
		if(msgcodeExt && !SearchUtils.isEmpty((String)codeExtCheck[2])) {
			// включена е проверка по codeExt - значения, за които codeExt е различен от подадения (codeExtCheck[1])- да не могат да се избират. 
			// да се изведе подаденото съобщение
			String clientId =  this.getClientId(FacesContext.getCurrentInstance());
			JSFUtils.addMessage(clientId+":treeM",FacesMessage.SEVERITY_ERROR, (String)codeExtCheck[2]);
		}
		
	}
	
	/**
	 * Избор от дървото - само избраното
	 * @param msgcodeExt
	 * @param selectedItem
	 * @param tempCodes
	 * @param tempCl
	 * @throws DbErrorException 
	 */
	private boolean onNodeSelect1( SystemClassif selectedItem, List<Integer> tempCodes, List<SystemClassif> tempCl) throws DbErrorException {		
		boolean msgcodeExt = false;
	//	// само при избор на конкретно значение!!! и ако има подаден параметър за търсене на заместник и класификацията е  админ. структура
		boolean addItem1;	
		addItem1 = addItemDelegate(selectedItem.getCode(), "2");		
		if(addItem1) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("само избраното ");
			}
			msgcodeExt = getCodeExt()!=null && !getCodeExt().equals(selectedItem.getCodeExt()); 
			if(!msgcodeExt) {
				tempCodes.add(selectedItem.getCode());
				setTempCodes(tempCodes);
				tempCl.add(selectedItem);
				setTempClassifs(tempCl);
			} 
		}
		return msgcodeExt;
	}

	/**
	 * Избор от дървото - с подчинените
	 * @param selectedItem
	 * @param tempCodes
	 * @param tempCl
	 * @param selV
	 * @return
	 * @throws DbErrorException
	 */
	private boolean onNodeSelect2( SystemClassif selectedItem, List<Integer> tempCodes, List<SystemClassif> tempCl, int selV) throws DbErrorException  {
		boolean msgcodeExt = false;
		List<SystemClassif> classifList;
		
		classifList = loadClassifList(getCodeClassif(), false, null);
		 
		List<SystemClassif> sc11 = null;
		sc11 = SysClassifUtils.getChildren(classifList, selectedItem.getCode(), selectedItem);
		List<Integer> tempCodesParent = new ArrayList<>();
		for (SystemClassif item : sc11) {
			if (!tempCodes.contains(item.getCode()) && !(selV == 2 && item.getCode() == selectedItem.getCode())) {
				
//					if (LOGGER.isDebugEnabled()) {
//						LOGGER.debug("item.code>>{}; {}; parent:{}", item.getCode(), item.getTekst(), item.getCodeParent());
//					}
				
				boolean bb1 = selV == SELV_2 && Boolean.FALSE.equals(getIsSelectNode()) && tempCodesParent.contains(item.getCode());
				if (!bb1) {						
					boolean msgcodeExt2 = getCodeExt()!=null && !getCodeExt().equals(selectedItem.getCodeExt());
					if(!msgcodeExt2) {
						tempCodes.add(item.getCode());
						tempCl.add(item);
					}else if (getCodeExt() != null){
						msgcodeExt =  true;
					}
					if (!tempCodesParent.contains(item.getCodeParent())) {
						tempCodesParent.add(item.getCodeParent());
					}
				}
			}
		}

		setTempCodes(tempCodes);
		setTempClassifs(tempCl);

		
		return msgcodeExt;
	}
	
	
	
	/**
	 */
	public void removeFromCodes() {
		Integer row = Integer.valueOf(FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap().get("rItem"));
		List<Integer> tmpArr = getTempCodes();
		if(tmpArr != null) {
			tmpArr.remove(row);
			setTempCodes(tmpArr);
		}

		List<SystemClassif> tmpArrS = getTempClassifs();
		for (SystemClassif itemS : tmpArrS) {
			if (itemS.getCode() == row) {
				tmpArrS.remove(itemS);
				setTempClassifs(tmpArrS);
				break;
			}
		}
	}

	/**
	 * формира текста - избраните значения, разделени със запетаи
	 *
	 * @param tmpArr
	 * @return
	 * @throws DbErrorException
	 */
	public String sbSelectedText(List<Integer> tmpArr) throws DbErrorException {
		Integer codeClassif = getCodeClassif();
		StringBuilder sb = new StringBuilder();
		if(tmpArr != null) {
			try {
				for (Integer code : tmpArr) {
					sb.append(getSystemData().decodeItem(codeClassif, code, getLang(), getDateClassif()).trim()).append(", ");
				}
				int idx = sb.lastIndexOf(",");
				if (idx != -1) {
					sb.replace(idx, idx + 1, " ");
				}
			} catch (Exception e) {
				throw new DbErrorException(e);
			}
		}
		return sb.toString();
	}

	/**
	 * Търсене на класификация по текст
	 *
	 * @throws DbErrorException
	 */
	public void search() throws DbErrorException {
		Integer codeClassif = getCodeClassif();
		try {
			List<SystemClassif> classifList = null;
			if(getTreeType().equals(ADM_GR)) {
				codeClassif = OmbConstants.CODE_CLASSIF_GROUP_EMPL;
				// групите да са филтрирани в рамките на регистртурата
				// Задължително трябва да е по азбучен ред, иначе неможе да се изгради дървото!!!
				Map<Integer, Object> specRegistratura = Collections.singletonMap(OmbClassifAdapter.REG_GROUP_INDEX_REGISTRATURA, ((UserData) getUserData()).getRegistratura());
				classifList = getSystemData().queryClassification(codeClassif, null, getDateClassif(), getLang(), specRegistratura);
			}else {
				classifList = loadClassifList(codeClassif, false, null);
			}

			TreeNode rootNode = new TreeUtils().fTree(classifList, getSearchWord(), true, true, null, null);
			setRoot(rootNode);

		} catch (Exception e) {
			throw new DbErrorException(e);
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
		@SuppressWarnings("unchecked")
		Map<Integer, Object> specifics = (Map<Integer, Object>) getAttributes().get("specifics");
	
		specifExtCode(specifics);
				
		Boolean filtered = (Boolean) getAttributes().get("filtered");
		int lang = getUserData().getCurrentLang();

		List<SystemClassif> classifList;
		if (Boolean.TRUE.equals(filtered)) { // иска се филтрирана - по права на потребителя
			if (query != null || (specifics != null && !specifics.isEmpty())) {
				classifList = getSystemData().queryClassification(getUserData(), codeClassif, query, getDateClassif(), lang, specifics);
			} else {
				classifList = getSystemData().getSysClassification(getUserData(), codeClassif, getDateClassif(), lang, fixPrev);
			}

		} else if (specifics != null && !specifics.isEmpty()) { // ще се пуска търсене на специфики
			classifList = getSystemData().queryClassification(codeClassif, query, getDateClassif(), lang, specifics);

		} else { // иначе както си е
			if (query == null) {
				classifList = getSystemData().getSysClassification(codeClassif, getDateClassif(), lang);
			} else {
				@SuppressWarnings("unchecked")
				List<Integer> notInCodes = (List<Integer>) getAttributes().get("readOnlyCodes");
				classifList = getSystemData().queryClassification(codeClassif, query, getDateClassif(), lang, notInCodes);
			}
		}

		return classifList;
	}
	
	/**
	 * Да се включи специфика и проверка по codeExt.  Спецификата е за аутокомплете
	 * @param specifics
	 */
	private void specifExtCode(Map<Integer, Object> specifics) {
		Object[] codeExtCheck =  (Object[] ) getAttributes().get("codeExtCheck");
		if(codeExtCheck != null && codeExtCheck[0] != null &&  codeExtCheck[1] != null ) {
			if (specifics == null) {
				specifics = new HashMap<>();
			}
			specifics.put((Integer)codeExtCheck[0], X.of(codeExtCheck[1])); 
			setCodeExt((String)codeExtCheck[1]); 
			// да се включи специфика и проверка по codeExt - значения, за които codeExt е различен от подадения (codeExtCheck[1])- да не могат да се избират. 
		}
	}

//	/** @param selectNode */
//	public void setIsSelectNode(Boolean selectNode) {
//		getStateHelper().put(PropertyKeys.ISSELECTNODE, selectNode);
//	}

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

	/** @param showMe */
	public void setShowMe(boolean showMe) {
		getStateHelper().put(PropertyKeys.SHOWME, showMe);
	}

	/** * @param tempClassifs */
	public void setTempClassifs(List<SystemClassif> tempClassifs) {
		getStateHelper().put(PropertyKeys.TEMPCLASSIFS, tempClassifs);
	}

	/** @param tempCodes */
	public void setTempCodes(List<Integer> tempCodes) {
		getStateHelper().put(PropertyKeys.TEMPCODES, tempCodes);
	}

	

	/** @return */
	public boolean isReorder() {
		return (Boolean) getStateHelper().eval(PropertyKeys.REORDER, false);
	}

	/** @param reorder */
	public void setReorder(boolean reorder) {
		getStateHelper().put(PropertyKeys.REORDER, reorder);
	}

	/** @return */
	public String getDopInfo() {
		return (String) getStateHelper().eval(PropertyKeys.DOPINFO, null);
	}
	
	/** @param dopInfo */
	public void setDopInfo(String dopInfo) {
		getStateHelper().put(PropertyKeys.DOPINFO, dopInfo);
	}
	
	/** @return */
	public String getSelectedDeleg() {
		return (String) getStateHelper().eval(PropertyKeys.SELECTEDDELEG, "0");
	}
	
	/** @param selectedDeleg */
	public void setSelectedDeleg(String selectedDeleg) {
		getStateHelper().put(PropertyKeys.SELECTEDDELEG, selectedDeleg);
	}
	
	/** @return */
	public SystemClassif getDeleg() {
		return (SystemClassif) getStateHelper().eval(PropertyKeys.DELEG, null);
	}
	
	/** @param deleg */
	public void setDeleg(SystemClassif deleg) {
		getStateHelper().put(PropertyKeys.DELEG, deleg);
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
	private BaseSystemData getSystemData() {
		if (this.systemData == null) {
			this.systemData = (BaseSystemData) JSFUtils.getManagedBean("systemData");
		}
		return this.systemData;
	}

	/** @return the userData */
	private BaseUserData getUserData() {
		if (this.userData == null) {
			this.userData = (BaseUserData) JSFUtils.getManagedBean("userData");
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
	
	
/***** Методи, свързани със заместването... Само, ако класиф. е админ. структура **/
	
	/**
	 * Проверка за заместване. Само, ако класиф. е админ. структура - да провери за заместване, ако onAddItemDelegate==ture
	 * @param itemCode
	 * @param endDate
	 * @return
	 * @throws DbErrorException
	 */
	  private boolean addItemDelegate(Integer itemCode, String fromAuto) throws DbErrorException {
		boolean rez = true;
		Date endDate = (Date) getAttributes().get("onAddItemDelegate");
		if (endDate != null) {
			setDopInfo(null);
			setDeleg(null);
			SystemClassif zamestnik = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_EMPL_REPLACES, itemCode, getLang(), endDate, false);
			if (zamestnik != null) {
				
				// само при избор на конкретно значение!!! и ако има подаден параметър за търсене на заместник и класификацията е  админ. структура
				String clientId =  "deleg"+this.getClientId(FacesContext.getCurrentInstance());
				String todo = "PF('"+clientId+"').show();";
				setDopInfo(zamestnik.getTekst());
				zamestnik.setDopInfo(fromAuto); // 1 - ot autocomple; 2 - от модалния с дървото
			
				setDeleg(zamestnik);
				
				PrimeFaces.current().executeScript(todo);  				 
				rez = false; // ако има зааместник - да предаде потвърждаването и избора към модалния за избор на заместник					
			}
		} 
		return rez;
	  }
	  
	  
	  
	  /**
	   * Модален за заместване  - потвърждение на избора
	   * @throws DbErrorException
	   */
	  public void actionConfirmDelegate() throws DbErrorException {
		  int sDeleg = Integer.parseInt(getSelectedDeleg());
		  List<Integer> itemCode = new ArrayList<>();
		  @SuppressWarnings("unchecked")
		  List<SystemClassif> selectedC= (List<SystemClassif>) getAttributes().get(SELECTEDCLASSIFS);				
		  if(sDeleg == 0) {
			  // заместник
			  itemCode.add(Integer.valueOf(getDeleg().getCodeExt()));		  			  
		  }else if(sDeleg == 1){			  
			  //титуляр
			  itemCode.add(getDeleg().getCode());
			  
		  }else { // sDeleg == 2
			  itemCode.add(getDeleg().getCode());
			  itemCode.add(Integer.valueOf(getDeleg().getCodeExt()));	
			  //  двамата
		  }
		  
		  int fromA =  (getDeleg().getDopInfo() != null) ? Integer.parseInt(getDeleg().getDopInfo()): 0; //1 - ot autocomple; 2 - от модалния с дървото
		  if(fromA == 1) {	
			  confirmDelegateFromAutoC(itemCode, selectedC);
		  } else if  (fromA == 2) {
			  confirmDelegateFromModal(itemCode);			 
		  } else {
			  if (LOGGER.isDebugEnabled()) {
				  LOGGER.debug("ERROR - потвърждаване на заместник..");
			  }
		  }
	  }
	  
	  /**
	   * заместване - избор от autocomplete
	   * @param itemCode
	   * @param selectedC
	   * @throws DbErrorException
	   */
	  private void confirmDelegateFromAutoC(List<Integer> itemCode, List<SystemClassif> selectedC) throws DbErrorException {
		  for (Integer item: itemCode) {
			  boolean bb = true;
			  int n =  selectedC.size(); 
			  for (int i=0; i < n; i++) {
				Integer codeR = selectedC.get(i).getCode();
				if(codeR.equals(item)) {//
					bb = false; // dublirat se....
					break;
				}
			  }			
			  if (bb) {		
				SystemClassif scItem = scItemZanestnik(item); 		
				selectedC.add(scItem);
			  }			 
		  }
		  returnResultAuto(selectedC);	
	  }
	  
	  /**
	   * заместване - избор от дървото
	   * @param itemCode
	   * @throws DbErrorException
	   */
	  private void confirmDelegateFromModal(List<Integer> itemCode) throws DbErrorException {
		  for (Integer item: itemCode) {
				List<SystemClassif> tempCl = getTempClassifs();
				List<Integer> tempCodes = getTempCodes();
				if(tempCodes==null) {
					tempCodes = new ArrayList<>();
				}

				if (!tempCodes.contains(item)) {
					tempCodes.add(item);
					setTempCodes(tempCodes);
					SystemClassif scItem = scItemZanestnik(item); 					
					tempCl.add(scItem);
					setTempClassifs(tempCl);
				}
		  }
	  }
	  
	  /**
	   * модален за избор на заместник - протвърждение
	   * @param item
	   * @return
	   * @throws DbErrorException
	   */
	  private SystemClassif scItemZanestnik(Integer item) throws DbErrorException {
			SystemClassif scItem = new SystemClassif();
			scItem.setCodeClassif(Constants.CODE_CLASSIF_ADMIN_STR);
			scItem.setCode(item);
			String tekst = getSystemData().decodeItem(Constants.CODE_CLASSIF_ADMIN_STR, item, getUserData().getCurrentLang(), getDateClassif());		
			scItem.setTekst(tekst);
			return scItem;
	  }
	  
	  
	  // Групи служители
	  
	public  static   int ADM_LIST 	 = 1;
	public  static   int ADM_GR 	 = 2;
	
/**
 * Превключване - избор от дървото на адм.структира или от групи служители 
 * @throws DbErrorException
 */
	public void actionChangeTree() throws DbErrorException {
		loadRoot();
	}
	 
	
	
	
	
	/** @return */
	public Integer getTreeType() {
		return (Integer) getStateHelper().eval(PropertyKeys.TREETYPE, ADM_LIST);
	}
	
	/** @param treeType */
	public void setTreeType(Integer treeType) {
		getStateHelper().put(PropertyKeys.TREETYPE, treeType);
	}
	
	
}
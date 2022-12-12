package com.ib.omb.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DeloStorageDAO;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.ObjectInUseException;
import com.ib.system.utils.SearchUtils;

/**
 * Въвеждане/ актуализация на съхранение на преписки/ дела
 */
@Named
@ViewScoped
public class DeloStorageBean  extends IndexUIbean   {

	private static final long serialVersionUID = -1838878271483408727L;

	private static final Logger LOGGER = LoggerFactory.getLogger(DeloStorageBean.class);

	private Date decodeDate=new Date();
	private transient DeloStorageDAO storageDao;
	private String room;
	private String shkaf;
	private String stillage;
	private String box;
	
	private List<Object[]> deloList = new ArrayList<Object[]>();
	private List<Object[]> deloSelectedTmp = new ArrayList<Object[]>();
	
	private Map<Integer, Boolean>	checkBoxAll = new HashMap<>();
	
	private Map<Object[], Boolean>	checkBoxHash = new HashMap<>();
	
	private boolean disableButtons = true;
		
	/** */
	@PostConstruct
	void initData() {
		
	        
		storageDao = new DeloStorageDAO(getUserData());
		HttpSession session = (HttpSession) JSFUtils.getExternalContext().getSession(false);
		
		List<Integer> deloIdList = (List<Integer>) session.getAttribute("deloIdList");
					
		if(deloIdList != null && !deloIdList.isEmpty()) {	
				try {
					deloList = this.storageDao.createSelectLoadStorageData(deloIdList);
				} catch (DbErrorException e) {
					LOGGER.error(e.getMessage(), e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
				}							
		}
	}
	/**
	 * Маркиране/ размаркиране на всички
	 * @param key
	 */		
	public void actionChangeCheckAll(Object key) {
		
		boolean rez = checkBoxAll.get(key);
		for(Object[] item:deloList) {
			if(SearchUtils.asInteger(item[0]).equals(SearchUtils.asInteger(key))) { //само записите от маркираното дело/ преписка
				checkBoxHash.put(item,rez);	
			}
		}	
		checkSelected();
	}
	
	/**
	 * Проверка за размаркиран ред
	 * @param row
	 */
	public void actionChangeCheck(Object[]row) {
	   
		if(Boolean.FALSE.equals(checkBoxHash.get(row))) { //ако размаркират ред от групата,да размаркира горния чекбокс
			checkBoxAll.remove(row[0]);//защото id-то на преписката се повтаря
			checkBoxAll.put(SearchUtils.asInteger(row[0]), false);
		
		}
		
		checkSelected();
	}
	
	private  void checkSelected() {
		if(checkBoxHash.values().contains(true)) {
			disableButtons = false;
		}else {
			disableButtons = true;
		}
	}
	
	private boolean checkInputData() {
		boolean save = false;
		if( room == null || "".equals(room)) {
			JSFUtils.addMessage("storageForm:room", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloStorage.insertRoom"));
			save = true;
		}
		if( (shkaf == null || "".equals(shkaf)) && ((stillage!=null && !"".equals(stillage))|| (box!=null && !"".equals(box)) )) {
			JSFUtils.addMessage("storageForm:shkaf", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloStorage.insertShkaf"));
			save = true;
		}
		if(  ((stillage==null || "".equals(stillage))&& box!=null && !"".equals(box) )) {
			JSFUtils.addMessage("storageForm:stillage", FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloStorage.insertStillage"));
			save = true;
		}
		return save;
	}
	
	/**
	 * Групово въвеждане на стойности за съхранение
	 */
	public void actionInput() {
		if(checkInputData()) {
			return;
		}
		for(Map.Entry<Object[], Boolean> entry : checkBoxHash.entrySet()) {
			Object[] key = entry.getKey();
			if(checkBoxHash.get(key).booleanValue()) {
				for(int i=0;i<deloList.size();i++) {
					Object[] row = deloList.get(i);
					if(key == row) {
						
						row[7] = room;
						if(shkaf!=null && !"".equals(shkaf)) {
							row[8] = shkaf;
						}
						if(stillage!=null && !"".equals(stillage)) {
							row[9] = stillage;
						}
						if(box!=null && !"".equals(box)) {
							row[10] = box;
						}
						break;
					}
				}
			}
			
		}	
		
		checkBoxHash.clear();
		checkBoxAll.clear();
		checkSelected();
	}
	
	/**
	 * Проверка за празни стойности и викане на метода за запис
	 */
	public void actionSave() {
		boolean haveEmpty = false;
		for(Object[] item : deloList) {
			if(item[7]==null) {
				haveEmpty = true;
				break;
			}
		}
		
		if(!haveEmpty) {
			save();
		}else {
			PrimeFaces.current().executeScript("PF('hiddenButton').jq.click();");
		}
	}
	
	/**
	 * извикване на метода за запис
	 */
	public void save() {
		try {
			JPA.getUtil().runInTransaction(() -> storageDao.storeDeloData(deloList));
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, IndexUIbean.getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );
			checkBoxHash.clear();
			checkBoxAll.clear();
			checkSelected();
		} catch (DbErrorException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		} catch (ObjectInUseException e ) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
	
	}
	
	/**
	 * Зачистване на всички стойности
	 */
	public void actionClear() {
		for(Map.Entry<Object[], Boolean> entry : checkBoxHash.entrySet()) {
			Object[] key = entry.getKey();
			if(checkBoxHash.get(key).booleanValue()) {
				for(int i=0;i<deloList.size();i++) {
					Object[] row = deloList.get(i);
					if(key == row) {
						row[6] = null;
						row[7] = null;
						row[8] = null;
						row[9] = null;
						row[10] = null;
						break;
					}
				}
			}
		}	
		
		checkBoxHash.clear();
		checkBoxAll.clear();
		checkSelected();
	}
	
	/**
	 * Проверка за липсващи стойности за помещение 
	 * @param rkv
	 */
	public void changeShkaf(int rkv) {
		if(deloList.get(rkv)[7]==null) {
			deloList.get(rkv)[8]=null;
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloStorage.insertRoom"));
		}
	}
	
	/**
	 * Проверка за липсваща стойност за шкаф
	 * @param rkv
	 */
	public void changeStillage(int rkv) {
		if(deloList.get(rkv)[8]==null) {
			deloList.get(rkv)[9]=null;
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloStorage.insertShkaf"));
		}
	}
	
	/**
	 * Промяна на кутия
	 * @param rkv
	 */
	public void changeBox(int rkv) {
		if(deloList.get(rkv)[9]==null) {
			deloList.get(rkv)[10]=null;
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "deloStorage.insertStillage"));
		}
	}
	
	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}

	public String getRoom() {
		return room;
	}

	public void setRoom(String room) {
		this.room = room;
	}

	public String getShkaf() {
		return shkaf;
	}

	public void setShkaf(String shkaf) {
		this.shkaf = shkaf;
	}

	public String getStillage() {
		return stillage;
	}

	public void setStillage(String stillage) {
		this.stillage = stillage;
	}

	public String getBox() {
		return box;
	}

	public void setBox(String box) {
		this.box = box;
	}

	public List<Object[]> getDeloSelectedTmp() {
		return deloSelectedTmp;
	}

	public void setDeloSelectedTmp(List<Object[]> deloSelectedTmp) {
		this.deloSelectedTmp = deloSelectedTmp;
	}


	public Map<Object[], Boolean> getCheckBoxHash() {
		return checkBoxHash;
	}

	public void setCheckBoxHash(Map<Object[], Boolean> checkBoxHash) {
		this.checkBoxHash = checkBoxHash;
	}


	public Map<Integer, Boolean> getCheckBoxAll() {
		return checkBoxAll;
	}

	public void setCheckBoxAll(Map<Integer, Boolean> checkBoxAll) {
		this.checkBoxAll = checkBoxAll;
	}


	public List<Object[]> getDeloList() {
		return deloList;
	}

	public void setDeloList(List<Object[]> deloList) {
		this.deloList = deloList;
	}

	public boolean isDisableButtons() {
		return disableButtons;
	}

	public void setDisableButtons(boolean disableButtons) {
		this.disableButtons = disableButtons;
	}
}
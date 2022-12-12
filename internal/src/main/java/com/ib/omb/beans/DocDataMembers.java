package com.ib.omb.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.faces.application.FacesMessage;
import javax.faces.model.SelectItem;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.event.RowEditEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocMemberDAO;
import com.ib.omb.db.dto.Doc;
import com.ib.omb.db.dto.DocMember;
import com.ib.omb.system.OmbClassifAdapter;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;
import com.ib.system.SysConstants;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemClassif;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.exceptions.UnexpectedResultException;


@Named
@ViewScoped
public class DocDataMembers   extends IndexUIbean  implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7835901986162256951L;
	private static final Logger LOGGER = LoggerFactory.getLogger(DocDataMembers.class);
	public static final String  DOCFORMTABS = "docForm:tabsDoc";
	public static final String  MSGPLSINSV = "general.pleaseInsertV";
	
		
	private Date decodeDate = new Date();	
	private Doc docEdit;
	private String rnFullDoc; 
	private SystemData sd;	
	private  List<DocMember> membersList;
	private Integer selectedEmp;
	private Integer selectedCoresp;
	private transient DocMemberDAO memberDao;
	private List <SelectItem> roleList;
	private Integer role;

	public void initTab() {
		DocData bean = (DocData) JSFUtils.getManagedBean("docData"); 
		memberDao = new DocMemberDAO(getUserData());
		sd = (SystemData) getSystemData();	
		if(bean != null && !Objects.equals(bean.getRnFullDoc(), rnFullDoc)){ 
					
				docEdit =   bean.getDocument();
				rnFullDoc = bean.getRnFullDoc();
				
				try {
					setRoleList(getRoleList(docEdit.getDocVid()));
					membersList = memberDao.findByDoc(docEdit.getId());
					if(membersList == null) {
						membersList = new ArrayList<>();
					}
				} catch (DbErrorException | UnexpectedResultException e) {
					LOGGER.error("Грешка при зареждане на участници! ", e);
					JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
				} finally {
					JPA.getUtil().closeConnection();
				}
			
		}
	}
	
	private boolean checkForDudlicate(Integer selMember) {
		boolean check = false;
		
		if(membersList!=null && !membersList.isEmpty()) {
			for(DocMember item: membersList) {
				if(item.getCodeRef().equals(selMember)) {
					check = true;
				}
			}
		}
		return check;
	}
	
	public void actionSelectEmpl() {
		if(selectedEmp!=null) {
			if(checkForDudlicate(selectedEmp)) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "docDataMembers.dubEmpl"));
			}else {
				 DocMember member = new DocMember();
				 member.setCodeRef(selectedEmp);
				 member.setDocId(docEdit.getId());
				 member.setRefType(OmbConstants.CODE_ZNACHENIE_REF_TYPE_EMPL);
				 member.setRoleRef(role);
				 try {
					member.setRefText(sd.decodeItem(OmbConstants.CODE_CLASSIF_ADMIN_STR, selectedEmp, getCurrentLang(), decodeDate));
				} catch (DbErrorException e) {
					LOGGER.error("Грешка при избор на служител! ", e);
					 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
				}
				membersList.add(member);
				save(member);
				selectedEmp = null;
			}
					
		 }
	}
		
	public void actionSelectCoresp() {
		if(selectedCoresp!=null) {
			if(checkForDudlicate(selectedCoresp)) {
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(beanMessages, "docDataMembers.dubKoresp"));
			}else {
				 DocMember member = new DocMember();
				 member.setCodeRef(selectedCoresp);
				 member.setDocId(docEdit.getId());
				 member.setRoleRef(role);
				
				 try {
					SystemClassif coresp = getSystemData().decodeItemLite(Constants.CODE_CLASSIF_REFERENTS, selectedCoresp, SysConstants.CODE_DEFAULT_LANG, decodeDate, true);
					member.setRefType((int) coresp.getSpecifics()[OmbClassifAdapter.REFERENTS_INDEX_REF_TYPE]); 
					member.setRefText(sd.decodeItem(OmbConstants.CODE_CLASSIF_REFERENTS, selectedCoresp, getCurrentLang(), decodeDate));
				} catch (DbErrorException e) {
					LOGGER.error("Грешка при избор на служител! ", e);
					 JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
				}
				membersList.add(member);
				save(member);
				selectedCoresp = null;
			}
					
		 }
	}
	
	
	public void actionSave(RowEditEvent<DocMember> event) {
		
		DocMember member = event.getObject();	
	    save(member);				
	}
	
	private void  save(DocMember member) {
		try {
			JPA.getUtil().runInTransaction(() -> memberDao.save(member))	;
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, SUCCESSAVEMSG) );
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при запис на участник! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}		
	}
	
	public void actionDelete(DocMember forDelete) {

		try {
			JPA.getUtil().runInTransaction(() -> memberDao.delete(forDelete));		
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));		
			membersList.remove(forDelete);
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при изтриване на участник! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
		} catch (BaseException e) {
			LOGGER.error(e.getMessage(), e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
		}
		
	}
	
	public void actionDeleteAll() {
		
		for(DocMember member:membersList) {
			try {
				JPA.getUtil().runInTransaction(() -> memberDao.delete(member));		
				
			} catch (DbErrorException e) {
				LOGGER.error("Грешка при изтриване на участник! ", e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,getMessageResourceString(UI_beanMessages, ERRDATABASEMSG), e.getMessage());
			} catch (BaseException e) {
				LOGGER.error(e.getMessage(), e);
				JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR, e.getMessage());
			}
		}
		membersList.clear();
		JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO, getMessageResourceString(UI_beanMessages, "general.successDeleteMsg"));		
		  
	}

	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}


	public Doc getDocEdit() {
		return docEdit;
	}


	public void setDocEdit(Doc docEdit) {
		this.docEdit = docEdit;
	}

	public List<DocMember> getMembersList() {
		return membersList;
	}

	public void setMembersList(List<DocMember> membersList) {
		this.membersList = membersList;
	}

	public String getRnFullDoc() {
		return rnFullDoc;
	}

	public void setRnFullDoc(String rnFullDoc) {
		this.rnFullDoc = rnFullDoc;
	}

	public Integer getSelectedEmp() {
		return selectedEmp;
	}

	public void setSelectedEmp(Integer selectedEmp) {
			
		this.selectedEmp = selectedEmp;
	}

	public Integer getSelectedCoresp() {
		return selectedCoresp;
	}

	public void setSelectedCoresp(Integer selectedCoresp) {
		this.selectedCoresp = selectedCoresp;
	}
	
	private List<SelectItem> getRoleList(Integer docVid) throws DbErrorException, UnexpectedResultException {
		if (docVid == null) {
			return new ArrayList<>();
		}

		List<SystemClassif> classif = getSystemData().getClassifByListVod(OmbConstants.CODE_LIST_DOC_VID_MEMBER_ROLE, docVid, getCurrentLang(), this.decodeDate);

		if (classif == null || classif.isEmpty()) { // ако няма се дава цялата, но с екстри

			return createItemsList(false, OmbConstants.CODE_CLASSIF_DOC_MEMBER_ROLES, this.decodeDate, false, Constants.SELECT_OPTIONS_WITH_PARENTS_DISABLED, 3);
		}

		// ще се формира списък само с допустимите
		List<SelectItem> items = new ArrayList<>();
	
		for (SystemClassif x : classif) {
			items.add(new SelectItem(x.getCode(), x.getTekst()));
		}
		return items;
	}

	public List <SelectItem> getRoleList() {
		return roleList;
	}

	public void setRoleList(List <SelectItem> roleList) {
		this.roleList = roleList;
	}

	public Integer getRole() {
		return role;
	}

	public void setRole(Integer role) {
		this.role = role;
	}
}
package com.ib.omb.beans;

import java.util.Date;

import javax.faces.application.FacesMessage;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.pagination.LazyDataModelSQL2Array;
import com.ib.indexui.system.Constants;
import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.db.dao.DocDAO;
import com.ib.omb.db.dao.ReferentDAO;
import com.ib.omb.db.dto.Referent;
import com.ib.system.db.JPA;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;


@Named
@ViewScoped
public class MergingCoresps  extends IndexUIbean{

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2426173104534513643L;
	private static final Logger LOGGER = LoggerFactory.getLogger(MergingCoresps.class);
	public static final String	UIBEANMESSAGES	= "ui_beanMessages";
	public static final String  MSGPLSINS		= "general.pleaseInsert";
	public static final String  ERRDATABASEMSG 	= "general.errDataBaseMsg";
	public static final String  SUCCESSAVEMSG 	= "general.succesSaveMsg";
	public static final String  OBJINUSEMSG 	= "general.objectInUse";
	
	private Integer codeCorresp;	
	private Date decodeDate = new Date();
	private Integer codeCorresp2;	
	private Date decodeDate2 = new Date();
	private Referent coresp1 = new Referent();
	private Referent coresp2 = new Referent();
	private Referent tmpRef;
	private boolean active1;
	private boolean active2;
    private LazyDataModelSQL2Array docsList;

    private LazyDataModelSQL2Array docsList2;
	
	
	public void save() {
		try {
			if(coresp1.getId() == null) {
				JSFUtils.addMessage("",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UIBEANMESSAGES,MSGPLSINS,getMessageResourceString(LABELS, "docu.corespondent")+ " 1" ) );
				return;
			}
			if(coresp2.getId() == null) {
				JSFUtils.addMessage("",FacesMessage.SEVERITY_ERROR,getMessageResourceString(UIBEANMESSAGES,MSGPLSINS,getMessageResourceString(LABELS, "docu.corespondent") + " 2" ));
				return;
			}
			if(!active1 && !active2) {
				JSFUtils.addMessage("",FacesMessage.SEVERITY_ERROR, "Моля, определете кой от кореспондентите ще остане активен в системата!");
				return;
			}
			if(!coresp1.getRefType().equals(coresp2.getRefType())) {
				JSFUtils.addMessage("",FacesMessage.SEVERITY_ERROR, "Двамата кореспондента трябва да са физически лица или да са юридически лица!");
				return;
			}
			if(active1)
				JPA.getUtil().runInTransaction(() -> new ReferentDAO(getUserData()).mergeCorrespondents(codeCorresp, codeCorresp2, getSystemData()));	
			else
				JPA.getUtil().runInTransaction(() -> new ReferentDAO(getUserData()).mergeCorrespondents(codeCorresp2, codeCorresp, getSystemData()));	
			
			getSystemData().reloadClassif(Constants.CODE_CLASSIF_REFERENTS, false, false);
			actionClear();
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_INFO,IndexUIbean.getMessageResourceString(UIBEANMESSAGES, SUCCESSAVEMSG));
		} catch (BaseException e) {
			LOGGER.error("Грешка при обединяване на кореспонденти! ", e);
			JSFUtils.addGlobalMessage(FacesMessage.SEVERITY_ERROR,IndexUIbean.getMessageResourceString(UIBEANMESSAGES, ERRDATABASEMSG));
		}
	}
	
	public void actionClear() {
		coresp1 = new Referent();
		coresp2 = new Referent();
		codeCorresp = null;
		codeCorresp2 = null;
		active1 = false;
		active2 = false;
		docsList = null;
		docsList2 = null;
	}
	
	public void actionHideModalCoresp1() {
		if(codeCorresp == null)
			coresp1 = new Referent();
		else {
			if(codeCorresp2 != null && codeCorresp.equals(codeCorresp2)) {
				coresp1 = new Referent();
				docsList = null;
				JSFUtils.addMessage("",FacesMessage.SEVERITY_ERROR, "Вече сте избрали този кореспондент!");
				return;
			}
			coresp1 = loadCorresp(codeCorresp, decodeDate);
			actionLoadDocsList1();
		}
		PrimeFaces.current().executeScript("PF('coresp1').hide();");
	}
	
	public void actionHideModalCoresp2() {
		if(codeCorresp2 == null)
			coresp2 = new Referent();
		else {
			if(codeCorresp != null && codeCorresp.equals(codeCorresp2)) {
				coresp2 = new Referent();
				docsList2 = null;
				JSFUtils.addMessage("",FacesMessage.SEVERITY_ERROR, "Вече сте избрали този кореспондент!");
				return;
			}
			coresp2 = loadCorresp(codeCorresp2, decodeDate2);
			actionLoadDocsList2();
		}
		PrimeFaces.current().executeScript("PF('coresp2').hide();");
	}
	
	/**
	 *  зарежда данни за корепондент по зададени критерии
	 */
	private Referent loadCorresp(Integer idCoresp, Date dateCorr) {
		try {
			if(idCoresp != null) {
				JPA.getUtil().runWithClose(() -> tmpRef = new ReferentDAO(getUserData()).findByCode(idCoresp, dateCorr, true));
			}
		} catch (BaseException e) {
			LOGGER.error("Грешка при зареждане на данни за кореспондент! ", e);
		}

		return tmpRef;
	}
	
	public void actionLoadDocsList1() {
		try {
			setDocsList(new LazyDataModelSQL2Array(new DocDAO(getUserData()).createSelectCorrespondentDocs(codeCorresp), null));
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на документи за кореспондент в опция обединяване на кореспонденти! ", e);
		}
	   // списък документи, в които участва кореспондента		
		/*DocSearch tmpSearch = new DocSearch(getUserData(UserData.class).getRegistratura());
		tmpSearch.setCodeRefCorresp(codeCorresp);
		tmpSearch.buildQueryComp(getUserData());
		setDocsList(new LazyDataModelSQL2Array(tmpSearch, "a1 desc"));  */
	}

	public void actionLoadDocsList2() {
		try {
			setDocsList2(new LazyDataModelSQL2Array(new DocDAO(getUserData()).createSelectCorrespondentDocs(codeCorresp2), null));
		} catch (DbErrorException e) {
			LOGGER.error("Грешка при зареждане на документи за кореспондент в опция обединяване на кореспонденти! ", e);
		}
	   // списък документи, в които участва кореспондента		
		/*DocSearch tmpSearch = new DocSearch(getUserData(UserData.class).getRegistratura());
		tmpSearch.setCodeRefCorresp(codeCorresp2);
		tmpSearch.buildQueryComp(getUserData());
		setDocsList2(new LazyDataModelSQL2Array(tmpSearch, "a1 desc"));  */
	}
	
	public void changeActiveCoresp1() {
		active1 = true;
		active2 = false;
	}
	
	public void changeActiveCoresp2() {
		active1 = false;
		active2 = true;
	}

	public Integer getCodeCorresp() {
		return codeCorresp;
	}

	public void setCodeCorresp(Integer codeCorresp) {
		this.codeCorresp = codeCorresp;
	}	

	public Date getDecodeDate() {
		return new Date(decodeDate.getTime()) ;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate != null ? new Date(decodeDate.getTime()) : null;
	}

	public Integer getCodeCorresp2() {
		return codeCorresp2;
	}

	public void setCodeCorresp2(Integer codeCorresp2) {
		this.codeCorresp2 = codeCorresp2;
	}

	public Date getDecodeDate2() {
		return decodeDate2;
	}

	public void setDecodeDate2(Date decodeDate2) {
		this.decodeDate2 = decodeDate2 != null ? new Date(decodeDate2.getTime()) : null;
	}

	public Referent getCoresp1() {
		return coresp1;
	}

	public void setCoresp1(Referent coresp1) {
		this.coresp1 = coresp1;
	}

	public Referent getCoresp2() {
		return coresp2;
	}

	public void setCoresp2(Referent coresp2) {
		this.coresp2 = coresp2;
	}

	public LazyDataModelSQL2Array getDocsList() {
		return docsList;
	}

	public void setDocsList(LazyDataModelSQL2Array docsList) {
		this.docsList = docsList;
	}

	public boolean isActive1() {
		return active1;
	}

	public void setActive1(boolean active1) {
		this.active1 = active1;
	}

	public boolean isActive2() {
		return active2;
	}

	public void setActive2(boolean active2) {
		this.active2 = active2;
	}

	public LazyDataModelSQL2Array getDocsList2() {
		return docsList2;
	}

	public void setDocsList2(LazyDataModelSQL2Array docsList2) {
		this.docsList2 = docsList2;
	}	
	
}

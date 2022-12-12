package com.ib.omb.system;

import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIFF_EVENTS_UNAUTHORIZED_PAGE;
import static com.ib.omb.system.OmbConstants.CODE_ZNACHENIE_NOTIF_ROLIA_CONTR;
import static com.ib.system.SysConstants.CODE_DEIN_UNAUTHORIZED;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.db.dao.AdmUserDAO;
import com.ib.indexui.system.Constants;
import com.ib.indexui.utils.ClientInfo;
import com.ib.indexui.utils.JSFUtils;
import com.ib.omb.experimental.Notification;
import com.ib.system.BaseUserData;
import com.ib.system.db.JPA;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.BaseException;
import com.ib.system.exceptions.DbErrorException;

/**
 * Конкретната за системата. В случая DocuWork!
 */
public class UserData extends BaseUserData {
	/**
	 * Служител, който делегира права
	 *
	 * @author belev
	 */
	public static class DelegatedEmployee implements Serializable {
		/**  */
		private static final long serialVersionUID = -1983470338638266264L;

		private Integer	codeRef;
		private String	nameRef;
		private Integer	codeType;

		/**
		 * @param codeRef
		 * @param nameRef
		 * @param codeType
		 */
		public DelegatedEmployee(Integer codeRef, String nameRef, Integer codeType) {
			this.codeRef = codeRef;
			this.nameRef = nameRef;
			this.codeType = codeType;
		}

		/** @return the codeRef */
		public Integer getCodeRef() {
			return this.codeRef;
		}

		/** @return the codeType */
		public Integer getCodeType() {
			return this.codeType;
		}

		/** @return the nameRef */
		public String getNameRef() {
			return this.nameRef;
		}

		/** */
		@Override
		public String toString() {
			return "DelegatedEmployee [codeRef=" + this.codeRef + ", nameRef=" + this.nameRef + ", codeType=" + this.codeType + "]";
		}
	}

	/** */
	static final Logger LOGGER = LoggerFactory.getLogger(UserData.class);

	/** */
	private static final long serialVersionUID = 2989532705334584877L;

	/** Регистратура в която се работи текущо */
	private Integer	registratura;
	/** звеното в което е назначен по време на логин */
	private Integer	zveno;
	/**
	 * ако логнатия е с ръководна длъжност {@link OmbConstants#CODE_CLASSIF_BOSS_POSITIONS}, то тука ще влезнат списък на звената
	 * до които той има достъп (като се почне от неговото). Ако не е с ръководна длъжност това остава NULL.
	 */
	private String	accessZvenoList;	// пример 5,16,20

	/** <code>true</code> значи е деловодител. определя се от метод {@link #calculateDelovoditelRegistraturi(Map)} */
	private boolean delovoditel;

	/** ID за селекти и за попълване на таблиците за достъп. Виж спецификация. */
	private Integer					userAccess;
	/** ID за запис/актуализация на всички данни, които не са свързани с лог. Виж спецификация. */
	private Integer					userSave;
	/**
	 * В системата се реализира механизъм за делегиране на права от един служител на друг служител. Делегирането е два вида: <br>
	 *  заместване (за период) <br>
	 *  упълномощаване (визира се случая секретарка, която работи от името на ръководителя) <br>
	 */
	private List<DelegatedEmployee>	delegatedEmployees;
	/**
	 * кофти име на поле, но да е ясно за какво става въпрос. когато работи от името на друг човек тука стои то заено с вида на
	 * делегиране
	 */
	private String					otImetoNa; // съответства на userAcces, когато userAccess!=userId

	
	/** пощенските кутии на потребитея спрямо текущата му регистратура. При смяна на реистратура списъка ще се опресни.
	 * Ако няма кутии списъка е NULL  */
	private List<String> mailboxNames; // TODO необходимо ли е да го има полето systemData.mailboxes ???

	/** при <code>true</code> значи че има отнет достъп до документи. определя се на логин */
	private boolean docAccessDenied;
	/** при <code>true</code> значи че има отнет достъп до преписки. определя се на логин */
	private boolean deloAccessDenied;

	/** брой текущи и предстоящи събития. определя се на логин и при заместване, като ако има промяна до като е логнат усера получава нотификация  */
	private Integer eventsCount;

	/**
	 * Използва се при логин в системата. За да се направи първоначална идентификация на потребителя
	 *
	 * @param userId
	 * @param loginName
	 * @param liceNames
	 */
	public UserData(Integer userId, String loginName, String liceNames) {
		super(userId, loginName, liceNames);

		// при логин са еднакви. ако в последствие има делегиране ще се сменят както се иска по спецификация.
		this.userAccess = userId;
		this.userSave = userId;

		this.delegatedEmployees = new ArrayList<>();
		this.delegatedEmployees.add(new DelegatedEmployee(userId, liceNames, null)); // винаги слагам текущия потребител с празен
																						// вид на делегиране
	}

	/**
	 * Проверява дали потребителя има достъп до страницата. Ако няма го праща на index.xhtml
	 *
	 * @param codePage
	 */
	public String checkPageAccess(Integer codePage) {
		boolean ok = hasAccess(Constants.CODE_CLASSIF_MENU, codePage);

		if (!ok) {
			HttpServletRequest request = (HttpServletRequest) JSFUtils.getExternalContext().getRequest();

			LOGGER.info("!!! UNAUTHORIZED PAGE ACCESS !!! username={};codePage={};url={}", getLoginName(), codePage, request.getRequestURL());

			StringBuilder sb = new StringBuilder();

			sb.append("От потребител \"" + getLoginName() + "\" е направен опит за достъп до страница ");
			sb.append(request.getRequestURL());

			SystemData sd = (SystemData) JSFUtils.getManagedBean("systemData");
			if (codePage != null) {
				try {
					sb.append(" (" + sd.decodeItem(Constants.CODE_CLASSIF_MENU, codePage, getCurrentLang(), new Date()) + ")");
				} catch (DbErrorException e) {
					sb.append(" код=" + codePage + "!");
				}
			}

			String userIP = ClientInfo.getClientIpAddr(request);
			String sessionId = ((HttpSession) JSFUtils.getExternalContext().getSession(false)).getId();
			String clientBrowser = ClientInfo.getClientBrowser(request);
			String clientOS = ClientInfo.getClientOS(request);

			sb.append("</br>IP=" + userIP + "; Browser=" + clientBrowser + "; OS=" + clientOS + "; SESSID=" + sessionId);

			try {
				String setting = sd.getSettingsValue("delo.unauthorizedNotifUser");
				if (setting != null) { // нотификция
					sendUnauthorizedPageAccessNotif(request.getRequestURL().toString(), setting.split(","), sd);
				}
			} catch (Exception e) {
				LOGGER.error("Грешка при формиране на нотификация: Опит за неоторизиран достъп до страница.", e);
			}

			SystemJournal journal = new SystemJournal(getUserId(), CODE_DEIN_UNAUTHORIZED, Constants.CODE_ZNACHENIE_JOURNAL_USER, getUserId(), sb.toString(), null);

			try {
				JPA.getUtil().runInTransaction(() -> new AdmUserDAO(this).saveAudit(journal));

			} catch (BaseException e) {
				LOGGER.error("Грешка при журналиране на Опит за неоторизиран достъп", e);
			}

			ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
			try {
				context.redirect(context.getRequestContextPath() + "/pages/empty.xhtml");
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return "@NO@";
	}

	 
	/**
	 * Заради различните менюта за актуализация за жалба, нпм, самосезиране, документ.
	 * страниците за разглеждане и актуализация обаче са общи 
	 * @param codePage1
	 * @param codePage2
	 * @param codePage3
	 * @param codePage4
	 * @return
	 */
	public String checkPageAccessMulti(Integer codePage1, Integer codePage2, Integer codePage3, Integer codePage4) {
		boolean ok1 = hasAccess(Constants.CODE_CLASSIF_MENU, codePage1);
		boolean ok2 = hasAccess(Constants.CODE_CLASSIF_MENU, codePage2);
		boolean ok3 = hasAccess(Constants.CODE_CLASSIF_MENU, codePage3);
		boolean ok4 = hasAccess(Constants.CODE_CLASSIF_MENU, codePage4);
		boolean ok = ok1 || ok2 || ok3 || ok4;
		
		if (!ok) {
			HttpServletRequest request = (HttpServletRequest) JSFUtils.getExternalContext().getRequest();

			LOGGER.info("!!! UNAUTHORIZED PAGE ACCESS !!! username={};codePage={};url={}", getLoginName(), "документи/жалби/НПМ/самосезиране", request.getRequestURL());

			StringBuilder sb = new StringBuilder();

			sb.append("От потребител \"" + getLoginName() + "\" е направен опит за достъп до страница за документи/жалби/НПМ/самосезиране!");
			sb.append(request.getRequestURL());

			SystemData sd = (SystemData) JSFUtils.getManagedBean("systemData");
//			if (codePage != null) {
//				try {
//					sb.append(" (" + sd.decodeItem(Constants.CODE_CLASSIF_MENU, codePage, getCurrentLang(), new Date()) + ")");
//				} catch (DbErrorException e) {
//					sb.append(" код=" + codePage + "!");
//				}
//			}

			String userIP = ClientInfo.getClientIpAddr(request);
			String sessionId = ((HttpSession) JSFUtils.getExternalContext().getSession(false)).getId();
			String clientBrowser = ClientInfo.getClientBrowser(request);
			String clientOS = ClientInfo.getClientOS(request);

			sb.append("</br>IP=" + userIP + "; Browser=" + clientBrowser + "; OS=" + clientOS + "; SESSID=" + sessionId);

			try {
				String setting = sd.getSettingsValue("delo.unauthorizedNotifUser");
				if (setting != null) { // нотификция
					sendUnauthorizedPageAccessNotif(request.getRequestURL().toString(), setting.split(","), sd);
				}
			} catch (Exception e) {
				LOGGER.error("Грешка при формиране на нотификация: Опит за неоторизиран достъп до страница.", e);
			}

			SystemJournal journal = new SystemJournal(getUserId(), CODE_DEIN_UNAUTHORIZED, Constants.CODE_ZNACHENIE_JOURNAL_USER, getUserId(), sb.toString(), null);

			try {
				JPA.getUtil().runInTransaction(() -> new AdmUserDAO(this).saveAudit(journal));

			} catch (BaseException e) {
				LOGGER.error("Грешка при журналиране на Опит за неоторизиран достъп", e);
			}

			ExternalContext context = FacesContext.getCurrentInstance().getExternalContext();
			try {
				context.redirect(context.getRequestContextPath() + "/pages/empty.xhtml");
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return "@NO@";
	}
	
	/** формира и изпраща нотификация: Опит за неоторизиран достъп до страница
	 */
	private void sendUnauthorizedPageAccessNotif(String page, String[] userCodes, SystemData sd) {
		String msg = "Потребител "+getLoginName()+" се опитва да отвори страница "+page+" на приложението, за която няма достъп.";

		List<Integer> adresati = new ArrayList<>();
		for (String userCode : userCodes) {
			adresati.add(Integer.valueOf(userCode));
		}
		
		Notification notif = new Notification(getUserAccess(), null //
			, CODE_ZNACHENIE_NOTIFF_EVENTS_UNAUTHORIZED_PAGE, CODE_ZNACHENIE_NOTIF_ROLIA_CONTR, sd);
		notif.setComment(msg);
		notif.setAdresati(adresati);
		notif.send();
	}

	/**
	 * true - презареждане на страницата	
	 */
	private boolean reloadPage = false;
	private String previousPage = null;

   /**
    * Проверка дали отворената страница се презарежда
    * Извикването е:  
    * <f:metadata><f:viewAction action="#{userData.checkReloadPage}" onPostback="false" /></f:metadata>
    * Използва се за проверка дали заключен за редакция обект (в @PostConstruct)  трябва да бъде отключен (в @PredDestroy)
    * Ако страницата се презарежда - да прескочи отключването.
    * reloadPage и previousPage трябва да се нулират в @PredDestroy, т.е. при излизане от страницата
    */
	public String checkReloadPage() {
		LOGGER.debug("checkReloadPage");
		FacesContext context = FacesContext.getCurrentInstance();
		if(context != null) {
		    UIViewRoot viewRoot = context.getViewRoot();
		    String id = viewRoot.getViewId();
		    if (previousPage != null && (previousPage.equals(id))) {
		    	setReloadPage(true); 
		    }else {
		    	setReloadPage(false);
		    }
		    previousPage = id;
		}
		return "@NO@";
    }

	/** @return the accessZvenoList */
	public String getAccessZvenoList() {
		return this.accessZvenoList;
	}

	/** @return the delegatedEmployees */
	public List<DelegatedEmployee> getDelegatedEmployees() {
		return this.delegatedEmployees;
	}

	/** @return the registratura */
	public Integer getRegistratura() {
		return this.registratura;
	}

	/** @return the userAccess */
	public Integer getUserAccess() {
		return this.userAccess;
	}

	/** @return the userSave */
	public Integer getUserSave() {
		return this.userSave;
	}

	/** @return the zveno */
	public Integer getZveno() {
		return this.zveno;
	}

	/** @return the delovoditel */
	public boolean isDelovoditel() {
		return this.delovoditel;
	}

	/** @param accessZvenoList the accessZvenoList to set */
	public void setAccessZvenoList(String accessZvenoList) {
		this.accessZvenoList = accessZvenoList;
	}

	/** @param registratura the registratura to set */
	public void setRegistratura(Integer registratura) {
		this.registratura = registratura;
	}

	/** @param userAccess the userAccess to set */
	public void setUserAccess(Integer userAccess) {
		this.userAccess = userAccess;
	}

	/** @param userSave the userSave to set */
	public void setUserSave(Integer userSave) {
		this.userSave = userSave;
	}

	/** @param zveno the zveno to set */
	public void setZveno(Integer zveno) {
		this.zveno = zveno;
	}

	/** @return the otImetoNa */
	public String getOtImetoNa() {
		return this.otImetoNa;
	}

	/** @param otImetoNa the otImetoNa to set */
	public void setOtImetoNa(String otImetoNa) {
		this.otImetoNa = otImetoNa;
	}

	public String getPreviousPage() {
		return previousPage;
	}

	public void setPreviousPage(String previousPage) {
		this.previousPage = previousPage;
	}

	public boolean isReloadPage() {
		return reloadPage;
	}

	public void setReloadPage(boolean reloadPage) {
		this.reloadPage = reloadPage;
	}

	/** @return the mailboxNames */
	public List<String> getMailboxNames() {
		return this.mailboxNames;
	}
	/** @param mailboxNames the mailboxNames to set */
	public void setMailboxNames(List<String> mailboxNames) {
		this.mailboxNames = mailboxNames;
	}

	/** @return the docAccessDenied */
	public boolean isDocAccessDenied() {
		return this.docAccessDenied;
	}

	/** @param docAccessDenied the docAccessDenied to set */
	public void setDocAccessDenied(boolean docAccessDenied) {
		this.docAccessDenied = docAccessDenied;
	}

	/** @return the deloAccessDenied */
	public boolean isDeloAccessDenied() {
		return this.deloAccessDenied;
	}

	/** @param deloAccessDenied the deloAccessDenied to set */
	public void setDeloAccessDenied(boolean deloAccessDenied) {
		this.deloAccessDenied = deloAccessDenied;
	}

	/** @param delovoditel the delovoditel to set */
	void setDelovoditel(boolean delovoditel) {
		this.delovoditel = delovoditel;
	}

	/** @return the eventsCount */
	public Integer getEventsCount() {
		return this.eventsCount;
	}
	/** @param eventsCount the eventsCount to set */
	public void setEventsCount(Integer eventsCount) {
		this.eventsCount = eventsCount;
	}
}
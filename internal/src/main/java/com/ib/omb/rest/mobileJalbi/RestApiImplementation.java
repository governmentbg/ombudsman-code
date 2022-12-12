package com.ib.omb.rest.mobileJalbi;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.ServletContext;

import com.google.gson.Gson;
import com.ib.omb.system.OmbConstants;
import com.ib.omb.system.SystemData;

/**
 * Използва се от ...Services класовете в пакетите v1 и т.н.
 * @author n.kanev
 *
 */
public class RestApiImplementation {
	
	protected static final String UI_BEANMESSAGES = "i18n.ui_beanMessages";
	protected static final String BEAN_MESSAGES = "i18n.beanMessages";
	protected static final String LABELS = "i18n.labels";
	
	private Gson gson;
	private SystemData systemData;
	private Locale locale;
	
	public RestApiImplementation(ServletContext context) {
		this.gson = new Gson();
		this.systemData = (SystemData) context.getAttribute("systemData");
	}
	
	public Gson getGson() {
		return gson;
	}
	
	public SystemData getSystemData() {
        return systemData;
    }
	
	protected void setMessagesLocale(Integer lang) {
		String locale = "bg";
		if(lang != null && lang == OmbConstants.CODE_LANG_EN) {
			locale = "en";
		}
		
		this.locale = new Locale(locale);
	}
	
	protected String getMessage(String resourceBundle, String messageName) {
		ResourceBundle bundle = ResourceBundle.getBundle(resourceBundle, this.locale);
		
		try {
			return bundle.getString(messageName);
		}
		catch(NullPointerException | MissingResourceException e) {
			return null;
		}
	}

}

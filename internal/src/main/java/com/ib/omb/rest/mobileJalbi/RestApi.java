package com.ib.omb.rest.mobileJalbi;

import javax.inject.Inject;
import javax.servlet.ServletContext;

/**
 * Използва се от ...Api класовете в пакетите v1 и т.н.
 * @author n.kanev
 *
 */
public class RestApi {
	
	@Inject
    private ServletContext context;

	public ServletContext getContext() {
		return context;
	}
	
}

package com.ib.omb.beans;

import java.io.Serializable;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.indexui.utils.JSFUtils;

@Named
@ViewScoped
public class CorrespEdit extends IndexUIbean  implements Serializable {	
	
	/**
	 * Актуализация на кореспондент
	 * 
	 */
	private static final long serialVersionUID = 1087155210424826840L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CorrespEdit.class);
	
	private static final String CODE_REF = "codeRef";
	
	private Integer codeCorresp;	
	private Date decodeDate = new Date();
	
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct!!!");	
		
		if (JSFUtils.getRequestParameter(CODE_REF) !=  null && !JSFUtils.getRequestParameter(CODE_REF).isEmpty()) {
			this.codeCorresp = Integer.valueOf(JSFUtils.getRequestParameter(CODE_REF));
		}		
	
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
	
}
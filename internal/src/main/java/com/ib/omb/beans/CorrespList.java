package com.ib.omb.beans;

import java.io.Serializable;
import java.util.Date;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;

@Named
@ViewScoped
public class CorrespList extends IndexUIbean  implements Serializable {
	
	/**
	 * Търсене на кореспонденти
	 * 
	 */
	private static final long serialVersionUID = -4003583586430983864L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CorrespList.class);
	
	private Integer codeCorresp;	
	private Date decodeDate = new Date();	
	
	/** 
	 * 
	 * 
	 **/
	@PostConstruct
	public void initData() {
		
		LOGGER.debug("PostConstruct!!!");		
	
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
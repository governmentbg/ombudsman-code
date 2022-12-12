package com.ib.omb.beans;


import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;


@Named
@ViewScoped
public class TestSignText extends IndexUIbean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1827667122996829015L;


	private static final Logger LOGGER = LoggerFactory.getLogger(TestSignText.class);
	
	
	private String xmlData;
	/** */
	@PostConstruct
	void initData() {
		
		xmlData = "najs a";
	}
	
	
	public String getXmlData() {
		return xmlData;
	}

	public void setXmlData(String xmlData) {
		System.out.println("testBean -> "+xmlData);
		this.xmlData = xmlData;
	}
}
package com.ib.omb.beans;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.primefaces.PrimeFaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ib.indexui.system.IndexUIbean;
import com.ib.omb.db.dto.DeloAccess;


@Named
@ViewScoped
public class TestSvilen extends IndexUIbean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1827667122996829015L;


	private static final Logger LOGGER = LoggerFactory.getLogger(TestSvilen.class);
	
	
	private Date decodeDate;	
	private Object[] selectedDoc=null;
	private String rnDoc;
	private List<Object[]> selectedDocM=new ArrayList<Object[]>();

	
	private List<DeloAccess> listDeloAcces=new ArrayList<DeloAccess>();
	
	/** */
	@PostConstruct
	void initData() {
		DeloAccess da=new DeloAccess();
		da.setId(1);
//		da.setCodeClassif(24);
		da.setCodeRef(11);
		listDeloAcces.add(da);
		DeloAccess da2=new DeloAccess();
		da2.setId(2);
//		da2.setCodeClassif(24);
		da2.setCodeRef(12);
		listDeloAcces.add(da2);
		
	}
	
	public  void actionOpenModalDoc(){
			
			String  cmdStr = "PF('modalDocSearch').show();";
			PrimeFaces.current().executeScript(cmdStr);
		
	}			 
	public  void actionOpenModalDocM(){
		
		String  cmdStr = "PF('modalDocSearchM').show();";
		PrimeFaces.current().executeScript(cmdStr);
		
	}

	 
	

	
	public Date getDecodeDate() {
		return decodeDate;
	}

	public void setDecodeDate(Date decodeDate) {
		this.decodeDate = decodeDate;
	}

	public String getRnDoc() {
		return rnDoc;
	}

	public void setRnDoc(String rnDoc) {
		this.rnDoc = rnDoc;
	}

	public Object[] getSelectedDoc() {
		return selectedDoc;
	}

	public void setSelectedDoc(Object[] selectedDoc) {
		this.selectedDoc = selectedDoc;
		if (selectedDoc!=null && selectedDoc[0]!=null) {
			this.rnDoc=(String)selectedDoc[1];
		}
		
	}

	public List<Object[]> getSelectedDocM() {
		return selectedDocM;
	}

	public void setSelectedDocM(List<Object[]> selectedDocM) {
		this.selectedDocM = selectedDocM;
		
		if (selectedDocM!=null && selectedDocM.size()>0 && selectedDocM.get(0)[0]!=null) {
			this.rnDoc=(String)selectedDocM.get(0)[1];
		}
		System.out.println(rnDoc);
	}


	public List<DeloAccess> getListDeloAcces() {
		return listDeloAcces;
	}

	public void setListDeloAcces(List<DeloAccess> listDeloAcces) {
		this.listDeloAcces = listDeloAcces;
		for (int i = 0; i < listDeloAcces.size(); i++) {
			System.out.println(" code: "+listDeloAcces.get(i).getCodeRef()+" id:"+listDeloAcces.get(i).getId());
			System.err.println("ID: "+ listDeloAcces.get(i).getId() + "  flag: "+ listDeloAcces.get(i).getFlag());
		}
		
	}
}
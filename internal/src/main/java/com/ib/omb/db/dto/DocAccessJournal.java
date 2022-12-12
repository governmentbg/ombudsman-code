package com.ib.omb.db.dto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;

import com.ib.omb.system.OmbConstants;
import com.ib.system.db.AuditExt;
import com.ib.system.db.JournalAttr;
import com.ib.system.db.dto.SystemJournal;
import com.ib.system.exceptions.DbErrorException;
import com.ib.system.utils.JAXBHelper;

public class DocAccessJournal implements AuditExt{
	@JournalAttr(label="persons",defaultText = "Изричен достъп")
	private List<DocAccess> persons=new ArrayList<DocAccess>();

	private Integer idDoc;
	
	private String identObject;
	
	public List<DocAccess> getPersons() {
		return persons;
	}

	public void setPersons(List<DocAccess> persons) {
		this.persons = persons;
	}

	@Override
	public SystemJournal toSystemJournal() throws DbErrorException {
		SystemJournal dj = new SystemJournal();
		dj.setCodeAction(OmbConstants.CODE_DEIN_KOREKCIA);
		dj.setCodeObject(OmbConstants.CODE_ZNACHENIE_JOURNAL_IZR_DOST);
		dj.setDateAction(new Date());
		dj.setIdentObject(identObject);
		dj.setIdObject(idDoc);
		dj.setJoinedCodeObject1(OmbConstants.CODE_ZNACHENIE_JOURNAL_DOC);
		dj.setJoinedIdObject1(idDoc);
		
		try {
			String objectXml = JAXBHelper.objectToXml(this, true);
			dj.setObjectXml(objectXml);
		} catch (JAXBException | IOException e) {
			e.printStackTrace();
		}
		
		
		
		return dj;
	}

	public Integer getIdDoc() {
		return idDoc;
	}

	public void setIdDoc(Integer idDoc) {
		this.idDoc = idDoc;
	}

	public String getIdentObject() {
		return identObject;
	}

	public void setIdentObject(String identObject) {
		this.identObject = identObject;
	}
	
	
}

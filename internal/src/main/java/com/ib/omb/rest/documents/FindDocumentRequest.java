package com.ib.omb.rest.documents;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class is used to hold all data returned
 */
@XmlRootElement
public class FindDocumentRequest {
    private Integer docId;
	private String jwtToken;
    private String docNr;
    private Date docDate;

    public FindDocumentRequest() {}

    public FindDocumentRequest(Integer docId, String jwtToken, String docNr, Date docDate) {
		super();
		this.docId = docId;
		this.jwtToken = jwtToken;
		this.docNr = docNr;
		this.docDate = docDate;
	}
    

	public Integer getDocId() {
		return docId;
	}

	public void setDocId(Integer docId) {
		this.docId = docId;
	}

	public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

	public String getDocNr() {
		return docNr;
	}

	public void setDocNr(String docNr) {
		this.docNr = docNr;
	}

	public Date getDocDate() {
		return docDate;
	}

	public void setDocDate(Date docDate) {
		this.docDate = docDate;
	}

}


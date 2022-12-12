package com.ib.omb.rest.documents;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.xml.bind.annotation.XmlRootElement;

import com.ib.omb.rest.DocuWorkRestResponse;

@XmlRootElement
public class FindDocumentResponse extends DocuWorkRestResponse {
	
	private String jwtToken;
	private ArrayList<LinkedHashMap<String,Object>> docList;

	public FindDocumentResponse() {
	}
	
	public FindDocumentResponse(DocuWorkRestResponse.STATUS status, String jwtToken, String message, ArrayList<LinkedHashMap<String,Object>> docList) {
        super(status, message);
        this.jwtToken = jwtToken;
        this.docList = docList;
    }

	public String getJwtToken() {
		return jwtToken;
	}

	public void setJwtToken(String jwtToken) {
		this.jwtToken = jwtToken;
	}

	public ArrayList<LinkedHashMap<String, Object>> getDocList() {
		return docList;
	}

	public void setDocList(ArrayList<LinkedHashMap<String, Object>> docList) {
		this.docList = docList;
	}
}

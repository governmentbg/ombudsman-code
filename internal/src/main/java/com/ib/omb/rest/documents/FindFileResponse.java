package com.ib.omb.rest.documents;

import javax.xml.bind.annotation.XmlRootElement;

import com.ib.omb.rest.DocuWorkRestResponse;

@XmlRootElement
public class FindFileResponse extends DocuWorkRestResponse {
	
	private String jwtToken;
	private byte[] file;

	public FindFileResponse() {
	}
	
	public FindFileResponse(DocuWorkRestResponse.STATUS status, String jwtToken, String message, byte[] file) {
        super(status, message);
        this.jwtToken = jwtToken;
        this.file = file;
    }

	public String getJwtToken() {
		return jwtToken;
	}

	public void setJwtToken(String jwtToken) {
		this.jwtToken = jwtToken;
	}

	public byte[] getFile() {
		return file;
	}

	public void setFile(byte[] file) {
		this.file = file;
	}

}

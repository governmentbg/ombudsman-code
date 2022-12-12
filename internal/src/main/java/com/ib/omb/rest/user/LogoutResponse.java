package com.ib.omb.rest.user;

import javax.xml.bind.annotation.XmlRootElement;

import com.ib.omb.rest.DocuWorkRestResponse;

/**
 * @author ilukov
 */
@XmlRootElement
public class LogoutResponse extends DocuWorkRestResponse {

    public LogoutResponse() {
    }

    public LogoutResponse(DocuWorkRestResponse.STATUS status, String message) {
        super(status, message);
    }
}

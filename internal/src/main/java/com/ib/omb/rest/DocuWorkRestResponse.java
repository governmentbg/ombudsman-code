package com.ib.omb.rest;

/**
 * Super class for all responses into the REST application holding status and message
 * @author ilukov
 */
public class DocuWorkRestResponse {
    public enum STATUS{
        ERROR,WARNING, SUCCESS
    }

    private STATUS status;
    private String message;

    public DocuWorkRestResponse(){}

    public DocuWorkRestResponse(STATUS status, String message) {
        this.status = status;
        this.message = message;
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


}

package com.ib.omb.rest.tasks;

import com.ib.omb.rest.DocuWorkRestResponse;

public class UpdateTaskStatusResponse extends DocuWorkRestResponse {
    private String jwtToken;

    public UpdateTaskStatusResponse() {
    }

    public UpdateTaskStatusResponse(STATUS status, String jwtToken,String message) {
        super(status, message);
        this.jwtToken = jwtToken;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }
}

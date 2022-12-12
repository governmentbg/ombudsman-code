package com.ib.omb.rest.tasks;

import java.util.Map;

import com.ib.omb.rest.DocuWorkRestResponse;

public class FindTaskByIdResponse extends DocuWorkRestResponse {
    private String jwtToken;
    private Map<String, Object> task;

    public FindTaskByIdResponse() {}

    public FindTaskByIdResponse(STATUS status, String jwtToken, String message, Map<String, Object> task) {
        super(status, message);
        this.jwtToken = jwtToken;
        this.task = task;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public Map<String, Object> getTask() {
        return task;
    }

    public void setTask(Map<String, Object> task) {
        this.task = task;
    }
}

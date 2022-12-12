package com.ib.omb.rest.tasks;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class FindTaskByIdRequest {
    private String jwtToken;
    private Integer taskId;

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }
}

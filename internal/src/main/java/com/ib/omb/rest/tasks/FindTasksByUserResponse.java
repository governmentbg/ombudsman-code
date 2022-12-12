package com.ib.omb.rest.tasks;

import javax.xml.bind.annotation.XmlRootElement;

import com.ib.omb.rest.DocuWorkRestResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Class is used to hold returned data from FimdTaskByUser endpoint
 * @author ilukov
 */
@XmlRootElement
public class FindTasksByUserResponse extends DocuWorkRestResponse {
    private String jwtToken;
    private ArrayList<LinkedHashMap<String,Object>> taskList;

    public FindTasksByUserResponse() {}

    public FindTasksByUserResponse(DocuWorkRestResponse.STATUS status,String jwtToken, String message, ArrayList<LinkedHashMap<String,Object>> taskList) {
        super(status, message);
        this.jwtToken = jwtToken;
        this.taskList = taskList;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public ArrayList<LinkedHashMap<String,Object>> getTaskList() {
        return taskList;
    }

    public void setTaskList(ArrayList<LinkedHashMap<String,Object>> taskList) {
        this.taskList = taskList;
    }
}

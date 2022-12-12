package com.ib.omb.rest.tasks;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class is used to hold all data returned
 * @author ilukov
 */
@XmlRootElement
public class FindTasksByUserRequest {
    private String jwtToken;
    private int pageSize;
    private int pageIndex;

    public FindTasksByUserRequest() {}

    public FindTasksByUserRequest(String jwtToken, int pageSize, int pageIndex) {
        this.jwtToken = jwtToken;
        this.pageSize = pageSize;
        this.pageIndex = pageIndex;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }
}

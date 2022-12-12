package com.ib.omb.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Map;
import java.util.Set;

/**
 * Class registers REST endpoints capabilities into the application
 * @author ilukov
 */
//@ApplicationPath("rest")
public class DocuWorkRestApplication extends Application {
    public DocuWorkRestApplication() {
        super();
    }

    @Override
    public Set<Class<?>> getClasses() {
        return super.getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return super.getSingletons();
    }

    @Override
    public Map<String, Object> getProperties() {
        return super.getProperties();
    }
}

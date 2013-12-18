package com.redhat.cloudconsole.api.resources;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.logging.Logger;

@Singleton
@Startup
public class DatabaseProperties {
    private static final Logger log = Logger
            .getLogger(DatabaseProperties.class.getName());
    
    @EJB
    private MongoProvider mongoProvider;

    @PostConstruct
    void initialise() throws Exception {
    	mongoProvider.getLogFilesColl().ensureIndex("{date: 1}" , "{expireAfterSeconds: 300}");
    }
}

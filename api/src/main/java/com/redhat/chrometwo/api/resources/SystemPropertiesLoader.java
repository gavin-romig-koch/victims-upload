package com.redhat.chrometwo.api.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.jboss.logging.Logger;

@Singleton
@Startup
public class SystemPropertiesLoader {
    private static final Logger log = Logger
            .getLogger(SystemPropertiesLoader.class.getName());

    	@PostConstruct
    	void initialise() {
    		Properties apiProps = new Properties();
    		try {
    			InputStream settingsStream = SystemPropertiesLoader.class
    					.getClassLoader()
    					.getResourceAsStream("com/redhat/chrometwo/api/resources/cloudconsole.properties");   
    			
    			apiProps.load(settingsStream);
    			System.getProperties().putAll(apiProps);
    			log.debug(apiProps);
    		} catch (IOException e) {
    			log.error(e);
    		}		
    	}
    }
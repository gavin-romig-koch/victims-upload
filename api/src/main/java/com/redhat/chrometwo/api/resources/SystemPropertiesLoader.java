package com.redhat.chrometwo.api.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
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
            // First attempt to read in local settings, which will only potentially be available in development
            // Otherwise in prod this won't be available so default to the properties within the project itself
            InputStream settingsStream = null;
            File localSettings = new File(System.getProperty("user.home") + "/chrometwo.properties");
            if(localSettings.exists()) {
                settingsStream = new FileInputStream(localSettings);
            } else {
                settingsStream = SystemPropertiesLoader.class
                        .getClassLoader()
                        .getResourceAsStream("com/redhat/chrometwo/api/resources/chrometwo.properties");
            }

            // Now load the settings from the properties stream, whichever was chosen
            apiProps.load(settingsStream);

            // Iterate through the properties and resolve any envars
            // TODO -- need code review on security of this.  I don't *think* there is an issue with the dereferencing
            // But there could be?
            Enumeration e = apiProps.propertyNames();
            while(e.hasMoreElements()) {
                String key = (String)e.nextElement();
                String value = apiProps.getProperty(key);
                if(value.contains("$")) {
                    String dereferencedValue = System.getenv(value.replace("$", ""));
                    // debug as this would show the pass in the logs.  Wouldn't ever hit this in prod though.
                    log.info("De-referencing the property: " + key + " with envar: " + value + " to: " + dereferencedValue);
                    apiProps.setProperty(key.replace("$", ""), dereferencedValue);
               } else {
                    log.debug("key: " + key + " value: " + value);
                }
            }
            System.getProperties().putAll(apiProps);

        } catch (IOException e) {
            log.error(e);
        }
    }
}

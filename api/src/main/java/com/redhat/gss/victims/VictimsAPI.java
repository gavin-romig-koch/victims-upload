package com.redhat.gss.victims;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

/**
 * Described in section 3.1 in the RESTEasy docs.
 */
public class VictimsAPI extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        final HashSet<Class<?>> set = new HashSet<Class<?>>();
        set.add(Check.class);
        set.add(CheckMate.class);

        return set;
    }
}

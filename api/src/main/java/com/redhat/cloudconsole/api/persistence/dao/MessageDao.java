package com.redhat.cloudconsole.api.persistence.dao;

import com.redhat.cloudconsole.api.persistence.entity.*;
import com.redhat.cloudconsole.api.resources.MongoProvider;

import org.bson.types.ObjectId;
import org.jboss.logging.Logger;

import javax.ejb.*;

import java.util.List;
import java.util.UUID;

@Stateless
@LocalBean
public class MessageDao {
    private static final Logger log = Logger.getLogger(AdminDao.class.getName());

    @EJB
    private MongoProvider mongoProvider;

    /**
     * Given that Jongo doesn't allow for programmatic building of queries, and this method requires it, building
     * this with the native mongo api.
     *
     * @param uuid
     * @return
     * @throws Exception
     */
    public DaemonMessage getDaemonMessage(String machineUuid) throws Exception {
        //BasicDBObject query = new BasicDBObject("machineUuid", machineUuid);

        //log.debug("getMessages: " + query.toString());
        // Find and project without the _id since the ObjectId of the metric is really irrelevant in this context
        DaemonMessage result = mongoProvider.getDaemonMessagesColl().find("{machineUuid: #}", machineUuid).sort("{created: 1}").as(DaemonMessage.class).iterator().next();
        
        // Query the latest one limit 1
        
        return result;
    }

    public void addDaemonMessage(DaemonMessage message) throws Exception {
        mongoProvider.getDaemonMessagesColl().save(message);
    }
    
    public void removeDaemonMessage(String key) throws Exception {
    	mongoProvider.getDaemonMessagesColl().remove(new ObjectId(key));
    }
    
    public UIMessage getUIMessage(String key) throws Exception {
        //BasicDBObject query = new BasicDBObject("machineUuid", machineUuid);

        //log.debug("getMessages: " + query.toString());
        // Find and project without the _id since the ObjectId of the metric is really irrelevant in this context
        UIMessage result = mongoProvider.getUIMessagesColl().find("{_id: #}", key).sort("{created: 1}").as(UIMessage.class).iterator().next();
        // Query the latest one limit 1
        
        return result;
    }

    public String addUIMessage(UIMessage message) throws Exception {;
        mongoProvider.getUIMessagesColl().save(message);
        return message.getKey();
    }
    
    public void removeUIMessage(String key) throws Exception {
    	mongoProvider.getUIMessagesColl().remove("{_id: #}", key);
    }
}

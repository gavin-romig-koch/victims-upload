package com.redhat.cloudconsole.api.persistence.dao;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.redhat.cloudconsole.api.persistence.entity.Machine;
import com.redhat.cloudconsole.api.persistence.entity.Metric;
import com.redhat.cloudconsole.api.persistence.entity.Organization;
import com.redhat.cloudconsole.api.resources.CloudCollectionsEnum;
import com.redhat.cloudconsole.api.resources.MetricsEnum;
import com.redhat.cloudconsole.api.resources.MongoProvider;
import org.jboss.logging.Logger;
import org.joda.time.DateTime;
import org.jongo.ResultHandler;

import javax.ejb.*;
import java.util.*;

@Stateless
@LocalBean
public class AdminDao {
    private static final Logger log = Logger.getLogger(AdminDao.class.getName());

    private static final String[] exampleNames = new String[]{"Acme", "Foo Inc.", "Bar'ometry", "AllYourBaseBeOurs", "Boats n' Shoes"};

    @EJB
    private MongoProvider mongoProvider;


    public Iterable<Metric> getMetrics(String uuid, String metricGroup, String metricName) throws Exception {
        return mongoProvider.getMetricsColl().find("{uuid: #, group: #, name: #}", uuid, metricGroup, metricName).as(Metric.class);
    }

    public Iterable<Machine> getMachines(String accountId) throws Exception {
        return mongoProvider.getMachinesColl().find("{accountId: #}", accountId).as(Machine.class);
    }

    /**
     * This creates n number of test machines with accountId set to the param.  This creates them in the native
     * java mongo driver syntax, not jongo syntax
     *
     * @param accountId
     * @throws Exception
     */
    public void createExampleData(String accountId) throws Exception {
        Random random = new Random();

        DBObject[] machines = new DBObject[random.nextInt(20)];

        String[] orgNames = new String[]{exampleNames[random.nextInt(exampleNames.length)],exampleNames[random.nextInt(exampleNames.length)]};
        String[] orgIds = new String[]{UUID.randomUUID().toString(),UUID.randomUUID().toString()};

        log.debug("Creating " + machines.length + " machines each over " + orgIds.length + " orgs.");
        log.debug("orgNames: " + orgNames);
        log.debug("orgIds: " + orgIds[0] + ", " + orgIds[1]);
        for(int i = 0; i < orgIds.length; i++){
            // Create between 2 and 10 machines to persist
            for(int j = 0; j < machines.length; j++) {
                BasicDBObject m = new BasicDBObject("_id", UUID.randomUUID().toString());
                m.append("accountId", accountId);
                StringBuffer sb = new StringBuffer();
                sb.append(random.nextInt(256)).append(".").append(random.nextInt(256)).append(".").append(random.nextInt(256)).append(".").append(random.nextInt(256));
                m.append("hostname", sb.toString());

                // Create the de-normalized org
                BasicDBList orgs = new BasicDBList();
                BasicDBObject org = new BasicDBObject("orgId", orgIds[i]);
                org.append("orgName", orgNames[i]);

                // Add the org to the orgs and the orgs to the machine
                orgs.add(org);
                m.append("orgs", orgs);

                // Add the machine to the list of machines
                machines[j] = m;
            }
        }

        // Insert the de-normalized machine
        mongoProvider.getMachinesColl().insert(machines);

        // Add the org to the orgList then add that orgList to the account

        // Start at a common time, now
        DateTime dt = new DateTime();

        // Iterate over each machine added and add some memory metrics
        for (DBObject m : machines) {
            String uuid = (String)m.get("_id");

            // Create 100 minutes of memory free/total metrics
            for(int i = 100; i > 0; i--) {
                dt = dt.minusMinutes(1);
                // Mem free
                BasicDBObject metric = new BasicDBObject();
                metric.append("uuid", uuid);
                metric.append("name", MetricsEnum.MemFree.getMetricName());
                metric.append("group", MetricsEnum.MemFree.getMetricGroup());
                metric.append("value", random.nextInt(1000000));
                metric.append("max", Integer.MAX_VALUE);
                metric.append("date", dt.toDate());

                // Add the metric
                mongoProvider.getMetricsColl().insert(metric);

                // Mem total
                metric = new BasicDBObject();
                metric.append("uuid", uuid);
                metric.append("name", MetricsEnum.MemTotal.getMetricName());
                metric.append("group", MetricsEnum.MemTotal.getMetricGroup());
                metric.append("value", random.nextInt(1000000));
                metric.append("max", Integer.MAX_VALUE);
                metric.append("date", dt.toDate());

                // Add the metric
                mongoProvider.getMetricsColl().insert(metric);
            }
        }
    }

    /**
     * This is a Cascaded delete.  Here we want to delete all machines and metrics with this specific accountId
     * This is mainly for testing/mocking purposes.
     * @param accountId
     */
    public void deleteAccount(String accountId) throws Exception {
        // First get a list of all machines in a particular account
        BasicDBObject query = new BasicDBObject("accountId", accountId);

        // Get a list of all _ids
//        List<DBObject> results = mongoProvider.getMachinesColl().find(query).toArray();

        List<String> _ids = Lists.newArrayList(mongoProvider.getMachinesColl().find("{accountId: #}", accountId).map(
            new ResultHandler<String>() {
                @Override
                public String map(DBObject result) { return (String)result.get("_id"); }
            }
        ));
        log.debug("Found the following machine uuids to remove: " + _ids);

        // Remove all documents in the machines collection where the accountId matches the passed in one
        log.debug("Removing all machines from account: " + accountId + " with query: " + query.toString());
        mongoProvider.getMachinesColl().getDBCollection().remove(query);

        // Remove all documents in the metrics with the accountId machine uuids
        mongoProvider.getMetricsColl().remove("{uuid: {$in: #}}", _ids);
    }
}

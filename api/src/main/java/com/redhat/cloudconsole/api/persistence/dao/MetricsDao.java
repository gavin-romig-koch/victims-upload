package com.redhat.cloudconsole.api.persistence.dao;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.redhat.cloudconsole.api.persistence.entity.*;
import com.redhat.cloudconsole.api.resources.CloudCollectionsEnum;
import com.redhat.cloudconsole.api.resources.MongoProvider;
import org.jboss.logging.Logger;

import javax.ejb.*;
import java.util.Date;
import java.util.List;

@Stateless
@LocalBean
public class MetricsDao {
    private static final Logger log = Logger.getLogger(AdminDao.class.getName());

    @EJB
    private MongoProvider mongoProvider;

    /**
     * Given that Jongo doesn't allow for programmatic building of queries, and this method requires it, building
     * this with the native mongo api.
     *
     * @param uuid
     * @param metricGroup
     * @param metricName
     * @param start
     * @param end
     * @return
     * @throws Exception
     */
    public Iterable<Metric> getMetrics(String uuid, String metricGroup, String metricName, Date start, Date end, int limit) throws Exception {
        BasicDBObject query = new BasicDBObject("uuid", uuid).append("group", metricGroup).append("name", metricName);

        // If the start date is set, query by that as well
        if(start != null) {
            query.append("date", new BasicDBObject("$gte", start));
        }

        // Same with the end
        if(end != null) {
            query.append("date", new BasicDBObject("$lt", end));
        }


        log.debug("getMetrics: " + query.toString() + ", limit: " + limit);
        // Find and project without the _id since the ObjectId of the metric is really irrelevant in this context
        // Iterable<Metric> results = mongoProvider.getMetricsColl().find(query.toString()).projection("{_id: 0}").as(Metric.class);
        Iterable<Metric> results = null;
        if(limit > 0) {
            results = mongoProvider.getMetricsColl().find(query.toString()).sort("{date: -1}").limit(limit).as(Metric.class);
        } else {
            results = mongoProvider.getMetricsColl().find(query.toString()).sort("{date: -1}").as(Metric.class);
        }

        return results;
        // List<DBObject> results = mongoProvider.getMetricsColl().getDBCollection().find(query).toArray();
        // return results;
    }

    // I'll always send as an array so I don't think we need this
    //public void addMetric(Metric metric) throws Exception {
    //    mongoProvider.getMetricsColl().save(metric);
    //}
    public void addMetric(List<Metric> metrics) throws Exception {
        /*
        There is a bug right now where jongo doesn't persist arrays like you would expect.  It would persist like this:
        {
        "_id" : ObjectId("52a0e804cc061d97d7091333"),
        "0" : {
                "uuid" : "48799a86-f0aa-438b-8bea-ac164d9066be",
                "group" : "mem",
                "name" : "free",
                "value" : NumberLong(1000),
                "date" : ISODate("2013-11-27T15:33:46Z")
        },
        "1" : {
                "uuid" : "48799a86-f0aa-438b-8bea-ac164d9066be",
                "group" : "mem",
                "name" : "free",
                "value" : NumberLong(1000),
                "date" : ISODate("2013-11-27T15:33:46Z")
        }
        }
        Therefore, for now let's just persist each object individually, which will for our purposes will have next to no
        tangible impact on performance.
         */
        for(Metric metric : metrics) {
            mongoProvider.getMetricsColl().save(metric);
        }
    }
}

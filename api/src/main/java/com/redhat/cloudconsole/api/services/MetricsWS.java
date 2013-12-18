
package com.redhat.cloudconsole.api.services;

import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import com.mongodb.DBObject;
import com.redhat.cloudconsole.api.persistence.dao.MetricsDao;
import com.redhat.cloudconsole.api.persistence.entity.Metric;
import com.redhat.cloudconsole.api.security.ConsoleRolesAllowed;
import com.redhat.cloudconsole.api.security.SecurityConstants;
import com.redhat.cloudconsole.api.security.SecurityInterceptor;

@Path("/machines/{uuid}/metrics")
@Stateless
@LocalBean
public class MetricsWS {

    @EJB
    private MetricsDao dao;

    /**
     * Search for metrics given the input parameters.
     *
     * ex. GET /api/machines/7963a435-87a7-4437-9bc1-9a99248ebf17/mem/free  will get all metrics from the machine in
     * this group:name
     *
     * ex. GET /api/machines/7963a435-87a7-4437-9bc1-9a99248ebf17/mem/free?start=2013-10-01T00:00:00&end=2013-12-01T00:00:00
     * will get all metrics between the specified date range
     *
     * @param uuid The uuid of the machine
     * @param group The metric group
     * @param name The metric name
     * @param start Optionally restrict the query where the date is greater than or equal to the start date
     * @param end Optionally restrict the query where the date is less than the end date
     * @return
     * @throws Exception
     */
    @GET
    @Path("/{group}/{name}")
    public Iterable<Metric> getMetrics (
            @PathParam("uuid") String uuid,
            @PathParam("group") String group,
            @PathParam("name") String name,
            @QueryParam("start") Date start,
            @QueryParam("end") Date end,
            @QueryParam("limit") int limit) throws Exception {
        return dao.getMetrics(uuid, group, name, start, end, limit);
    }

    /**
     * Ex. POST
     *
     * {
     *  "uuid": "7963a435-87a7-4437-9bc1-9a99248ebf17",
     *  "group": "mem",
     *  "name": "free",
     *  "value": 1000,
     *  "date": "2013-11-27T15:33:46"
     * }
     *
     * @param metric
     * @throws Exception
     */
//    @POST
//    @Path("/")
//    public void addMetric (Metric metric) throws Exception {
//        dao.addMetric(metric);
//    }

    /**
     * Ex. POST
     *
     * [
     *  {
     *      "uuid": "48799a86-f0aa-438b-8bea-ac164d9066be",
     *      "group": "mem",
     *      "name": "free",
     *      "value": 1000,
     *      "date": "2013-11-27T15:33:46"
     *  },
     *  {
     *      "uuid": "48799a86-f0aa-438b-8bea-ac164d9066be",
     *      "group": "mem",
     *      "name": "free",
     *      "value": 1000,
     *      "date": "2013-11-27T15:33:46"
     *  }
     * ]
     *
     * @param metrics
     * @throws Exception
     */
    @POST
    @Path("/")
    @ConsoleRolesAllowed({SecurityConstants.MACHINE_ROLE,SecurityConstants.REDHAT_SU_ROLE})
    public void addMetric (@Context HttpServletRequest req,List<Metric> metrics) throws Exception {
    	for (Metric m : metrics){
    		SecurityInterceptor.checkValidUuid(req,m.getUuid()); 
    	}
        dao.addMetric(metrics);
    }

}


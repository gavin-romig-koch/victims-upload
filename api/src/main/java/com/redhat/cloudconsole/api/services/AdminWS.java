package com.redhat.cloudconsole.api.services;

import com.mongodb.DBObject;
import com.redhat.cloudconsole.api.persistence.dao.AdminDao;
import com.redhat.cloudconsole.api.persistence.entity.Machine;
import com.redhat.cloudconsole.api.persistence.entity.Metric;

import javax.ejb.*;
import javax.ws.rs.*;
import java.net.UnknownHostException;
import java.util.*;

@Path("/admin")
@Stateless
@LocalBean
public class AdminWS {

    @EJB
    private AdminDao adminDao;

    /**
     * TODO add in start and end date for the query
     *
     * Gets a list of metrics given the account/org/machine/group/name.  The account and org aren't necessary except
     * for security reasons.
     *
     * @param accountId
     * @param orgId
     * @param uuid
     * @param metricGroup
     * @param metricName
     * @return
     * @throws UnknownHostException
     */
    @GET
    @Path("/accounts/{accountId}/orgs/{orgId}/machines/{uuid}/metrics/{group}/{name}")
    public Iterable<Metric> getMetrics(@PathParam("accountId") String accountId,
                                     @PathParam("orgId") String orgId,
                                     @PathParam("uuid") String uuid,
                                     @PathParam("group") String metricGroup,
                                     @PathParam("name") String metricName) throws Exception {
        return adminDao.getMetrics(uuid, metricGroup, metricName);
    }

    @GET
    @Path("/machines/{accountId}")
    public Iterable<Machine> getMachines(@PathParam("accountId") String accountId) throws Exception {
        return adminDao.getMachines(accountId);
    }

    // rh_user=rh_user=rhn-support-smendenh|Samuel|customer|
    /*
     * Creates a test account given an account id
     * Ex. POST /api/admin/accounts/7777
     */
    @POST
    @Path("/accounts/{accountId}")
    public void createExampleData(@PathParam("accountId") String accountId) throws Exception {
        adminDao.createExampleData(accountId);
    }
    /*
     * Deletes all machines with a certain accountId, also deletes all metrics associated with those machines
     * Ex. DLETE /api/admin/accounts/7777
     */
    @DELETE
    @Path("/accounts/{accountId}")
    public void deleteTestAccount(@PathParam("accountId") String accountId) throws Exception {
        adminDao.deleteAccount(accountId);
    }
}

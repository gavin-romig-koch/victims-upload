package com.redhat.cloudconsole.api.services;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;

import com.redhat.cloudconsole.api.persistence.dao.AccountDao;
import com.redhat.cloudconsole.api.persistence.dao.MachineDao;
import com.redhat.cloudconsole.api.persistence.entity.Machine;
import com.redhat.cloudconsole.api.security.ConsoleRolesAllowed;
import com.redhat.cloudconsole.api.security.SecurityConstants;
import com.redhat.cloudconsole.api.security.SecurityInterceptor;

import java.util.List;
import java.util.Set;

@Path("/machines")
@Stateless
@LocalBean
public class MachineWS {
	@EJB
	private MachineDao machineDao;

    /**
     * Get a Machine object from a uuid
     * ex. GET /api/machines/1234-abcd-1234-abcd-1234
     *
     * @param uuid
     * @return
     * @throws Exception
     */
	@GET
	@Path("/{uuid}")
	public Machine getMachine(@PathParam("uuid") String uuid) throws Exception {
		return machineDao.getMachine(uuid);
	}

    /**
     * Deletes a machine and all associated metrics
     *
     * ex. DELETE /api/machines/1234-abcd-1234-abcd-1234
     *
     * @param uuid
     */
    @Path("/{uuid}")
    @DELETE
    @ConsoleRolesAllowed({SecurityConstants.MACHINE_ROLE,SecurityConstants.REDHAT_SU_ROLE})
    public void deleteMachine(@Context HttpServletRequest req,@PathParam("uuid") String uuid) throws Exception {
    	//TODO - is this the deregister method?
    	SecurityInterceptor.checkValidUuid(req,uuid); 
        machineDao.deleteMachine(uuid);
    }
}

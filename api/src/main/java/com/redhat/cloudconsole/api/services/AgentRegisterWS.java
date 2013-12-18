package com.redhat.cloudconsole.api.services;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import com.redhat.cloudconsole.api.persistence.dao.MachineDao;
import com.redhat.cloudconsole.api.persistence.entity.Machine;
import com.redhat.cloudconsole.api.security.ConsoleRolesAllowed;
import com.redhat.cloudconsole.api.security.SecurityConstants;
import com.redhat.cloudconsole.api.security.SecurityInterceptor;

@Path("/register")
@Stateless
@LocalBean
public class AgentRegisterWS {
	@EJB
    private MachineDao machineDao;

    /**
     * Ex. POST { "accountId" : "7777", "hostname" : "127.0.0.1", "orgs" : [ ], "_id" : "7963a435-87a7-4437-9bc1-9a99248ebf17" }
     *
     * @param sec
     * @param machine
     * @throws Exception
     */
	@POST
	@ConsoleRolesAllowed({SecurityConstants.ACCOUNT_OWNER_ROLE,SecurityConstants.REDHAT_SU_ROLE})
    public void addMachine(@Context HttpServletRequest req, Machine machine) throws Exception {
		System.out.println("Registering machine .....");
		SecurityInterceptor.checkValidAccountId(req,machine.getAccountId()); //check that the caller owns the account
    	machineDao.registerMachine(machine);
    }
}

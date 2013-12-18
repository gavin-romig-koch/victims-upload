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
import com.redhat.cloudconsole.api.security.SecurityInterceptor;

@Path("/accounts")
@Stateless
@LocalBean
public class AccountWS {

    @EJB
    private AccountDao accountDao;
    @EJB
    private MachineDao machineDao;

    @GET
    @Path("/{accountId}")
    public Iterable<Machine> getAccount(@PathParam("accountId") String accountId,
    						  @Context HttpServletRequest request) throws Exception {
    	// Lookey test code remove me:
    	System.out.println(request.getAttribute(SecurityInterceptor.ACCOUNT_ID));
    	System.out.println(request.getAttribute(SecurityInterceptor.USER_ID));
    	System.out.println(request.getAttribute(SecurityInterceptor.IS_INTERNAL));

        return accountDao.getAccount(accountId);
    }
}

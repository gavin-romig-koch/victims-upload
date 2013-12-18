package com.redhat.chrometwo.api.services;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import com.redhat.chrometwo.api.persistence.dao.AccountDao;
import com.redhat.chrometwo.api.security.SecurityInterceptor;

@Path("/accounts")
@Stateless
@LocalBean
public class AccountWS {

    @EJB
    private AccountDao accountDao;

    @GET
    @Path("/{accountId}")
    public String getAccount(@PathParam("accountId") String accountId,
    						  @Context HttpServletRequest request) throws Exception {
    	// Lookey test code remove me:
    	System.out.println(accountDao.doSomeBackendFunction());
    	System.out.println(request.getAttribute(SecurityInterceptor.ACCOUNT_ID));
    	System.out.println(request.getAttribute(SecurityInterceptor.USER_ID));
    	System.out.println(request.getAttribute(SecurityInterceptor.IS_INTERNAL));

        return "woot";
    }
}

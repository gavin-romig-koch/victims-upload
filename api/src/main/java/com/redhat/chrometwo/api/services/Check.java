package com.redhat.chrometwo.api.services;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import com.redhat.chrometwo.api.security.SecurityInterceptor;

@Path("/check")
@Stateless
@LocalBean
public class Check {

    @POST
    @Path("/{fileName}")
    public String getAccount(@PathParam("fileName") String fileName,
    						  @Context HttpServletRequest request) throws Exception {
    	System.out.println(request.getAttribute(SecurityInterceptor.ACCOUNT_ID));
    	System.out.println(request.getAttribute(SecurityInterceptor.USER_ID));
    	System.out.println(request.getAttribute(SecurityInterceptor.IS_INTERNAL));

        return "check: fileName: " + fileName 
            + " account: " + request.getAttribute(SecurityInterceptor.ACCOUNT_ID) 
            + " id: " + request.getAttribute(SecurityInterceptor.USER_ID) 
            + " internal: " + request.getAttribute(SecurityInterceptor.IS_INTERNAL);
    }
}

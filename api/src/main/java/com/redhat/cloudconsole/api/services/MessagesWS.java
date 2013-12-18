package com.redhat.cloudconsole.api.services;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import com.redhat.cloudconsole.api.persistence.dao.MessageDao;
import com.redhat.cloudconsole.api.persistence.entity.DaemonMessage;
import com.redhat.cloudconsole.api.persistence.entity.UIMessage;
import com.redhat.cloudconsole.api.security.ConsoleRolesAllowed;
import com.redhat.cloudconsole.api.security.SecurityConstants;
import com.redhat.cloudconsole.api.security.SecurityInterceptor;

@Path("/")
@Stateless
@LocalBean
public class MessagesWS {
	
	@EJB
	private MessageDao messageDao;
	
	@GET
    @Path("machines/{uuid}/messages/")
    public DaemonMessage getDaemonMessages(@PathParam("uuid") String uuid) throws Exception {
		DaemonMessage message = messageDao.getDaemonMessage(uuid);
		messageDao.removeDaemonMessage(message.getKey());
		return message;
    }
	
	@POST
    @Path("machines/{uuid}/messages/")
	@ConsoleRolesAllowed({SecurityConstants.MACHINE_ROLE,SecurityConstants.REDHAT_SU_ROLE})
    public void addDaemonMessage(@Context HttpServletRequest req,@PathParam("uuid") String uuid, DaemonMessage message) throws Exception {
		SecurityInterceptor.checkValidUuid(req,uuid);
		messageDao.addDaemonMessage(message);
    }
	
	@GET
    @Path("uimessages/{uuid}/")
    public UIMessage getUIMessages(@PathParam("uuid") String uuid) throws Exception {
		UIMessage message = messageDao.getUIMessage(uuid);
		messageDao.removeUIMessage(message.getKey());
		return message;
    }
	
	@POST
    @Path("uimessages/")
    public String addUIMessage(UIMessage message) throws Exception {
		return messageDao.addUIMessage(message);
    }
}


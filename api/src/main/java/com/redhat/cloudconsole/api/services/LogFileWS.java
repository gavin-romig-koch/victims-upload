package com.redhat.cloudconsole.api.services;

import java.util.UUID;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import com.redhat.cloudconsole.api.persistence.dao.LogFileDAO;
import com.redhat.cloudconsole.api.persistence.dao.MessageDao;
import com.redhat.cloudconsole.api.persistence.entity.LogFile;
import com.redhat.cloudconsole.api.persistence.entity.UIMessage;
import com.redhat.cloudconsole.api.security.ConsoleRolesAllowed;
import com.redhat.cloudconsole.api.security.SecurityConstants;
import com.redhat.cloudconsole.api.security.SecurityInterceptor;

@Path("/machines/{uuid}/logfiles")
@Stateless
@LocalBean
public class LogFileWS {

	@EJB
	private LogFileDAO logFileDao;

	@EJB
	private MessageDao messageDao;

	@GET
	@Path("/{filename}")
	public Object getLogLines(@PathParam("uuid") String uuid,
			@PathParam("filename") String filename,
			@QueryParam("startIndex") int startIndex,
			@QueryParam("numLines") int numLines,
			@QueryParam("isBackward") Boolean isBackward) throws Exception {
		return logFileDao.getLogLines(uuid, filename, startIndex, numLines,
				isBackward);
	}

	@POST
	@Path("/")
	@ConsoleRolesAllowed({ SecurityConstants.MACHINE_ROLE,
			SecurityConstants.REDHAT_SU_ROLE })
	public void addLogFile(@Context HttpServletRequest req,
			@PathParam("uuid") String uuid, @QueryParam("corrolationId") UUID corrolationId, LogFile logFile) throws Exception {
		SecurityInterceptor.checkValidUuid(req, uuid);
		logFileDao.addLogFile(logFile);
		if (corrolationId != null) {
			messageDao.addUIMessage(new UIMessage(corrolationId.toString(), 1,
					"Messages ready!!", "Horray"));
		}
	}

	@PUT
	@Path("/{filename}")
	@ConsoleRolesAllowed({ SecurityConstants.MACHINE_ROLE,
			SecurityConstants.REDHAT_SU_ROLE })
	public void addLogLines(@Context HttpServletRequest req,
			@PathParam("uuid") String uuid,
			@PathParam("filename") String filename,
			@QueryParam("corrolationId") UUID corrolationId, String logMessages)
			throws Exception {
		SecurityInterceptor.checkValidUuid(req, uuid);
		logFileDao.addLogLines(uuid, filename, logMessages);
		if (corrolationId != null) {
			messageDao.addUIMessage(new UIMessage(corrolationId.toString(), 1,
					"Messages ready!!", "Horray"));
		}
	}

	@POST
	@Path("/{filename}/heartbeat")
	@ConsoleRolesAllowed({ SecurityConstants.MACHINE_ROLE,
			SecurityConstants.REDHAT_SU_ROLE })
	public void heartbeat(@Context HttpServletRequest req,
			@PathParam("uuid") String uuid,
			@PathParam("filename") String fileName) throws Exception {
		SecurityInterceptor.checkValidUuid(req, uuid);
		logFileDao.heartbeat(uuid, fileName);
	}
}

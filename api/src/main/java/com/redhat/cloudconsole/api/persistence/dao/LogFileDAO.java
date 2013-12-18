package com.redhat.cloudconsole.api.persistence.dao;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import com.mongodb.BasicDBObject;
import com.redhat.cloudconsole.api.persistence.entity.*;
import com.redhat.cloudconsole.api.resources.MongoProvider;

import org.jboss.logging.Logger;

import javax.ejb.*;

@Stateless
@LocalBean
public class LogFileDAO {
	private static final Logger log = Logger.getLogger(LogFileDAO.class
			.getName());

	@EJB
	private MongoProvider mongoProvider;

	/**
	 * Given that Jongo doesn't allow for programmatic building of queries, and
	 * this method requires it, building this with the native mongo api.
	 * 
	 * @param startIndex
	 * 
	 * @param uuid
	 * @return
	 * @throws Exception
	 */
	public Object getLogLines(String machineUUID, String fileName, int start,
			int numLines, boolean isBackward) throws Exception {

		List<LogFile> result = null;
		if (isBackward) {
			result = mongoProvider
					.getLogFilesColl()
					.aggregate("{$match:{'_id': #}}",
							machineUUID + ":" + fileName)
					.and("{$unwind: '$logFile'}")
					.and("{$match: {'logFile.lineNo': {$lt: #}}}", start + 1)
					.and("{$match: {'logFile.lineNo': {$gt: #}}}",
							start - numLines)
					.and("{$group: {_id: '$_id', logFile: {$push: '$logFile'}}}")
					.as(LogFile.class);
		} else {
			result = mongoProvider
					.getLogFilesColl()
					.aggregate("{$match:{'_id': #}}",
							machineUUID + ":" + fileName)
					.and("{$unwind: '$logFile'}")
					.and("{$match: {'logFile.lineNo': {$gt: #}}}", start - 1)
					.and("{$match: {'logFile.lineNo': {$lt: #}}}",
							start + numLines)
					.and("{$group: {_id: '$_id', logFile: {$push: '$logFile'}}}")
					.as(LogFile.class);
		}

		Object returnObject;
		if(result.size() == 0){
			UUID uuid = UUID.randomUUID();
			returnObject = new UIMessageNotification(machineUUID, fileName, uuid);
			mongoProvider.getDaemonMessagesColl().save(new DaemonMessage(machineUUID, 1, "Get some files", fileName, uuid));
		} else {
			returnObject = result.get(0);
		}

		return returnObject;
	}

	public void addLogFile(LogFile logFile) throws Exception {
		logFile.setKey(logFile.getUuid() + ":" + logFile.getFileName());
		logFile.setDate(new Date());
		mongoProvider.getLogFilesColl().save(logFile);
	}

	public void heartbeat(String uuid, String fileName) throws Exception {
		// LogFile logFile = getLogFile(uuid, fileName,);
		// logFile.setDate(new Date());
		// mongoProvider.getLogFilesColl().save(logFile);
	}

	public void addLogLines(String uuid, String filename, String logMessages)
			throws Exception {
		mongoProvider.getLogFilesColl()
				.update("{ _id: '" + uuid + ":" + filename + "'}")
				.with("{$pushAll: " + logMessages + "}");
	}
}

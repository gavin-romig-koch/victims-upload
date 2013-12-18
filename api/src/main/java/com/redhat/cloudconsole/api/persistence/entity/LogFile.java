package com.redhat.cloudconsole.api.persistence.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

import org.jongo.marshall.jackson.oid.Id;

public class LogFile {
	
	@Id
	private String key;
    private Date date;
	private String uuid;
	private String fileName;
	private List<Log> logFile = new ArrayList<Log>();
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public List<Log> getLogFile() {
		return logFile;
	}
	public void addLogMessages(List<Log> logFile) {
		this.logFile.addAll(logFile);
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
}

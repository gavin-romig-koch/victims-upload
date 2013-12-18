package com.redhat.cloudconsole.api.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

import java.util.Date;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UIMessageNotification {
	
    private String machineUuid;
    
    private String fileName;
    
    private UUID eventUUID;

    public UIMessageNotification(String machineUuid, String fileName, UUID eventUUID){
    	this.machineUuid = machineUuid;
    	this.fileName = fileName;
    	this.eventUUID = eventUUID;
    }
    
	public String getMachineUuid() {
		return machineUuid;
	}

	public void setMachineUuid(String machineUuid) {
		this.machineUuid = machineUuid;
	}
	
	public UUID getEventUUID() {
		return eventUUID;
	}

	public void setEventUUID(UUID eventUUID) {
		this.eventUUID = eventUUID;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

}

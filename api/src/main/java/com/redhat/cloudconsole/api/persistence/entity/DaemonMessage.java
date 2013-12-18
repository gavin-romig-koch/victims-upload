package com.redhat.cloudconsole.api.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

import java.util.Date;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DaemonMessage {

	@Id @ObjectId // auto
    private String key;
	
    private String machineUuid;
    
    private int messageCode;

    private String message;
    
    private String value;
    
    private UUID corrolationId;
    
    public DaemonMessage(){
    	
    }
    
    public DaemonMessage(String machineUuid, int messageCode, String message, String value, UUID corrolationId){
    	this.machineUuid = machineUuid;
    	this.messageCode = messageCode;
    	this.message = message;
    	this.value = value;
    	this.corrolationId = corrolationId;
    }

	public String getMachineUuid() {
		return machineUuid;
	}

	public void setMachineUuid(String machineUuid) {
		this.machineUuid = machineUuid;
	}

	public int getMessageCode() {
		return messageCode;
	}

	public void setMessageCode(int messageCode) {
		this.messageCode = messageCode;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public UUID getCorrolationId() {
		return corrolationId;
	}

	public void setCorrolationId(UUID corrolationId) {
		this.corrolationId = corrolationId;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}

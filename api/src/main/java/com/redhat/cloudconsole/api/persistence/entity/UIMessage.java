package com.redhat.cloudconsole.api.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UIMessage {

	@Id
	private String key;
    
    private int messageCode;

    private String message;
    
    private String value;
    
    public UIMessage(){
    	
    }
    
    public UIMessage(String key, int messageCode, String message, String value){
    	this.key = key;
    	this.messageCode = messageCode;
    	this.message = message;
    	this.value = value;
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

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}
}

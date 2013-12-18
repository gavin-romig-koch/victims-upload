package com.redhat.cloudconsole.api.persistence.entity;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.jongo.marshall.jackson.oid.ObjectId;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Machine implements Serializable{

    // This represents the machine uuid
//    @ObjectId
    @JsonProperty("_id")
    private String _id;

    private String accountId;

    private String hostname;
    
    private String release;

    private List<Organization> orgs = new ArrayList<Organization>();

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getRelease() {
		return release;
	}

	public void setRelease(String release) {
		this.release = release;
	}

	public List<Organization> getOrgs() {
        return orgs;
    }

    public void setOrgs(List<Organization> orgs) {
        this.orgs = orgs;
    }
}

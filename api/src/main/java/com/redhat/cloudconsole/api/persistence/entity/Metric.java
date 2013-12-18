package com.redhat.cloudconsole.api.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jongo.marshall.jackson.oid.Id;
import org.jongo.marshall.jackson.oid.ObjectId;
//import org.bson.types.ObjectId;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Metric {

    @Id
    @ObjectId
    private String _id;
    public String get_id() {return _id;}
    public void set_id(String _id) {this._id = _id;}

    private String uuid;

    private String group;

    private String name;
    
    private String device;

    private long value;

    private Date date;

    private long max;

    public String getUuid() {
       return this.uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDevice() {
		return device;
	}
	public void setDevice(String device) {
		this.device = device;
	}
	public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }
	@Override
	public String toString() {
		return "Metric [_id=" + _id + ", uuid=" + uuid + ", group=" + group
				+ ", name=" + name + ", device=" + device + ", value=" + value
				+ ", date=" + date + ", max=" + max + "]";
	}
}

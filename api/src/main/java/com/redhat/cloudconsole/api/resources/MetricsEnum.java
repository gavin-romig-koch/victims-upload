package com.redhat.cloudconsole.api.resources;

/**
 * This has nearly no usage period beyond the AdminDao to help create mock data.   And even then this is just an example
 * for consistency
 */
public enum MetricsEnum {
    MemFree("mem", "free"),
    MemTotal("mem", "total");

    private String metricName;
    public String getMetricName(){return this.metricName;}
    private String metricGroup;
    public String getMetricGroup(){return this.metricGroup;}
    private MetricsEnum(String metricGroup, String metricName) {
        this.metricName = metricName;
        this.metricGroup = metricGroup;
    }
}

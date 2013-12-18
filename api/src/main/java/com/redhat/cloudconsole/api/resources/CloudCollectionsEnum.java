package com.redhat.cloudconsole.api.resources;

public enum CloudCollectionsEnum {
    MACHINES ("machines"),
    METRICS ("metrics"),
    DAEMONMESSAGES ("daemonMessages"),
    UIMESSAGES ("uiMessages"),
    LOGFILES ("logfiles");

    private String collName;
    public String getCollName() {return this.collName;}
    private CloudCollectionsEnum(String collName) {
        this.collName = collName;
    }
}

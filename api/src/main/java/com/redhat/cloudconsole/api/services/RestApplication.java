package com.redhat.cloudconsole.api.services;


import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

// TODO -- right now date serialization is in unix ms format, may want http://stackoverflow.com/questions/4428109/jersey-jackson-json-date-serialization-format-problem-how-to-change-the-form
@ApplicationPath("/")
public class RestApplication extends Application {
}

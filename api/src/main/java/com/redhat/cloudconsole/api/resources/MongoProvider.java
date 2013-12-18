package com.redhat.cloudconsole.api.resources;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import javax.ejb.EJB;
import javax.ejb.Singleton;

import java.net.UnknownHostException;
import org.jboss.logging.Logger;
import org.jongo.Jongo;
import org.jongo.MongoCollection;
import org.jongo.marshall.jackson.JacksonMapper;

@Singleton
public class MongoProvider {
    private static final Logger log = Logger.getLogger(MongoProvider.class.getName());

    private static MongoClient mongoClient;

    // This can be removed once I @DependsOn the SystemPropertiesLoader
    @EJB
    SystemPropertiesLoader systemPropertiesLoader;

    public MongoClient getMongoClient() throws Exception {
        try {
            if(mongoClient == null){
                String uri = null;
                String username = System.getProperty("mongoUser");
                String password = System.getProperty("mongoPass");
                String host = System.getProperty("mongoHost");
                String port = System.getProperty("mongoPort");
                String db = System.getProperty("mongoDb");
                if(!"".equals(username) && null != password) {
                    uri = "mongodb://" + username + ":" + password + "@" + host + ":" + port + "/" + db;
                } else {
                    uri = "mongodb://" + host + ":" + port + "/" + db;
                }
                
            	mongoClient = new MongoClient(new MongoClientURI(uri));
            }
            return mongoClient;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new Exception("An Error has occurred on the server, please try back later.");
        }
    }
    
    public DB getDb() throws Exception{
        return getMongoClient().getDB(System.getProperty("mongoDb"));
    }
    
    public MongoCollection getMachinesColl() throws Exception {
        try {
            
            Jongo jongo = new Jongo(getDb(),
                    new JacksonMapper.Builder()
                            .registerModule(new JodaModule())
                            .enable(MapperFeature.AUTO_DETECT_GETTERS)
                            .withView(Views.Public.class)
                            .build()
            );
            MongoCollection collection = jongo.getCollection(CloudCollectionsEnum.MACHINES.getCollName());
            return collection;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new Exception("An Error has occurred on the server, please try back later.");
        }
    }

    public MongoCollection getMetricsColl() throws Exception {
        try {
            // The disable feature seems to have no effect here
            Jongo jongo = new Jongo(getDb(),
                    new JacksonMapper.Builder()
                            .registerModule(new JodaModule())
                            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            .enable(MapperFeature.AUTO_DETECT_GETTERS)
                            .withView(Views.Public.class)
                            .build()
            );
            MongoCollection collection = jongo.getCollection(CloudCollectionsEnum.METRICS.getCollName());
            return collection;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new Exception("An Error has occurred on the server, please try back later.");
        }
    }
    
    public MongoCollection getDaemonMessagesColl() throws Exception {
        try {
            Jongo jongo = new Jongo(getDb(),
                    new JacksonMapper.Builder()
                            .registerModule(new JodaModule())
                            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            .enable(MapperFeature.AUTO_DETECT_GETTERS)
                            .withView(Views.Public.class)
                            .build()
            );
            MongoCollection collection = jongo.getCollection(CloudCollectionsEnum.DAEMONMESSAGES.getCollName());
            return collection;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new Exception("An Error has occurred on the server, please try back later.");
        }
    }
    
    public MongoCollection getUIMessagesColl() throws Exception {
        try {
        	DB db = getDb();
            Jongo jongo = new Jongo(db,
                    new JacksonMapper.Builder()
                            .registerModule(new JodaModule())
                            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            .enable(MapperFeature.AUTO_DETECT_GETTERS)
                            .withView(Views.Public.class)
                            .build()
            );
            MongoCollection collection = jongo.getCollection(CloudCollectionsEnum.UIMESSAGES.getCollName());
            return collection;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new Exception("An Error has occurred on the server, please try back later.");
        }
    }
    
    public MongoCollection getLogFilesColl() throws Exception {
        try {
        	DB db = getDb();
            Jongo jongo = new Jongo(db,
                    new JacksonMapper.Builder()
                            .registerModule(new JodaModule())
                            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
                            .enable(MapperFeature.AUTO_DETECT_GETTERS)
                            .withView(Views.Public.class)
                            .build()
            );
            MongoCollection collection = jongo.getCollection(CloudCollectionsEnum.LOGFILES.getCollName());
            return collection;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new Exception("An Error has occurred on the server, please try back later.");
        }
    }
}

package com.redhat.chrometwo.api.services;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import com.redhat.chrometwo.api.security.SecurityInterceptor;

import com.redhat.victims.VictimsException;
import com.redhat.victims.VictimsRecord;
import com.redhat.victims.VictimsResultCache;
import com.redhat.victims.VictimsScanner;
import com.redhat.victims.database.VictimsDB;
import com.redhat.victims.database.VictimsDBInterface;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.lang.StringBuilder;

@Path("/check")
@Stateless
@LocalBean
public class Check {

    private int count;

    private String checksum(InputStream body) {
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] buffer = new byte[1024];
            int size;
            count = 0;

            size = body.read(buffer);
            count += size;
            while (size > 0) {
                md.update(buffer);
                size = body.read(buffer);
                count += size;
            }

            byte[] digest = md.digest();
            hash = String.format("%0" + (digest.length << 1) + "X", new BigInteger(1, digest));

        } catch (NoSuchAlgorithmException e) {
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        return hash;
    }


    private String checkOne(VictimsDBInterface db, VictimsResultCache cache, String fileName, InputStream body) throws Exception {
        StringBuilder result = new StringBuilder();
       
        String key = checksum(body);
        result.append("key(" + count + "): ");
        result.append(key);
        result.append("\n");

        if (key != null && cache.exists(key)) {
            try {
                HashSet<String> cves = cache.get(key);
                if (cves != null && cves.size() > 0) {
                    result.append(String.format("%s VULNERABLE! ", fileName));
                    for (String cve : cves) {
                        result.append(cve);
                        result.append(" ");
                    }
                    result.append("\n");
                    return result.toString();
                } else {
                    result.append(fileName + " ok\n");
                }
            } catch (VictimsException e) {
                result.append("VictimsException while checking cache:\n");
                e.printStackTrace();
                return result.append(e.toString()).append("\n").toString();
            }
        }
        
        // Scan the item
        ArrayList<VictimsRecord> records = new ArrayList();
        try {
            
            VictimsScanner.scan(fileName, records);
            for (VictimsRecord record : records) {
                
                try {
                    HashSet<String> cves = db.getVulnerabilities(record);
                    if (key != null) {
                        cache.add(key, cves);
                    }
                    if (!cves.isEmpty()) {
                        result.append(String.format("%s VULNERABLE! ", fileName));
                        for (String cve : cves) {
                            result.append(cve);
                            result.append(" ");
                        }
                        result.append("\n");
                        return result.toString();
                    } else {
                        result.append(fileName + " ok\n");
                    }
                    
                } catch (VictimsException e) {
                    result.append("VictimsException while checking database:\n");
                    e.printStackTrace();
                    return result.append(e.toString()).append("\n").toString();
                }
            }
        } catch (IOException e) {
            result.append("VictimsException while scanning file:\n");
            e.printStackTrace();
            return result.append(e.toString()).append("\n").toString();
        }

        return result.toString();
    }

    private String displayHeaders(HttpServletRequest request) throws Exception {
        StringBuilder result = new StringBuilder();
        for (java.util.Enumeration<java.lang.String> headerNames = request.getHeaderNames();
             headerNames.hasMoreElements();) {
            String headerName = headerNames.nextElement();
            for (java.util.Enumeration<java.lang.String> headers = request.getHeaders(headerName);
                 headers.hasMoreElements();) {
                String header = headers.nextElement();
                result.append(headerName + ": " + header + "\n");
            }
        }
        return result.toString();
    }



    @POST
    @Path("/{fileName}")
    public String checkFile(InputStream body,
                            @PathParam("fileName") String fileName,
                            @Context HttpServletRequest request) throws Exception {

        StringBuilder result = new StringBuilder();
        
        result.append("check: ");
        result.append(fileName);
        result.append("\n");
        result.append(displayHeaders(request));

        VictimsDBInterface db;
        VictimsResultCache cache;

        try {
            db = VictimsDB.db();
            cache = new VictimsResultCache();

        } catch (VictimsException e) {
            result.append("VictimsException while opening the database:\n");
            e.printStackTrace();
            return result.append(e.toString()).append("\n").toString();
        }

        result.append(checkOne(db, cache, fileName, body));

        result.append("end of results\n");
        return result.toString();
    }

    @POST
    @Path("/multi")
    public String checkMulti(@Context HttpServletRequest request) throws Exception {

        StringBuilder result = new StringBuilder();
        
        result.append("multi: ");
        result.append(displayHeaders(request));

        VictimsDBInterface db;
        VictimsResultCache cache;

        try {
            db = VictimsDB.db();
            cache = new VictimsResultCache();

        } catch (VictimsException e) {
            result.append("VictimsException while opening the database:\n");
            e.printStackTrace();
            return result.append(e.toString()).append("\n").toString();
        }

        boolean foundAtLeastOne = false;
        for (Part part : request.getParts()) {
            foundAtLeastOne = true;
            result.append("found part named: " + part.getName());
            for (String headerName : part.getHeaderNames()) {
                for (String header : part.getHeaders(headerName)) {
                    result.append("  header " + headerName + ": " + header);
                }
            }
            result.append(checkOne(db, cache, part.getName(), part.getInputStream()));
        }

        if (!foundAtLeastOne) {
            result.append("no parts found\n");
        }
        result.append("end of results\n");
        return result.toString();
    }
}

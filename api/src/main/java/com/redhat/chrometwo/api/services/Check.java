package com.redhat.chrometwo.api.services;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import com.redhat.chrometwo.api.security.SecurityInterceptor;
import javax.mail.internet.ContentDisposition;

import java.io.ByteArrayInputStream;

import com.redhat.victims.VictimsException;
import com.redhat.victims.VictimsRecord;
import com.redhat.victims.VictimsResultCache;
import com.redhat.victims.VictimsScanner;
import com.redhat.victims.database.VictimsDB;
import com.redhat.victims.database.VictimsDBInterface;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.lang.StringBuilder;
import java.util.Map;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

@Path("/check")
@Stateless
@LocalBean
public class Check {

    @POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
    public String checkMulti(MultipartFormDataInput inputForm, @Context HttpServletRequest request) throws Exception {

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

        try {
            result.append("About to synchronize local database with upstream ...\n");
            db.synchronize();
            result.append("   successful synchronize.\n");

        } catch (VictimsException e) {
            result.append("VictimsException while synchronize-ing local database:\n");
            e.printStackTrace();
            return result.append(e.toString()).append("\n").toString();
        }


        boolean foundAtLeastOne = false;
        Map<String, List<InputPart>> multiValuedMap = inputForm.getFormDataMap();
        for (Map.Entry<String, List<InputPart>> entry : multiValuedMap.entrySet()) {
            foundAtLeastOne = true;
            String name = entry.getKey();
            int count = 0;
            result.append("found part named: " + name + "\n");
            for (InputPart inputPart : entry.getValue()) {
                String dispString = "";
                count++;
                result.append("found value " + count + ":\n");
                for (Map.Entry<String, List<String>> headerEntry : inputPart.getHeaders().entrySet()) {
                    for (String headerValue : headerEntry.getValue()) {
                        result.append("  header " + headerEntry.getKey() + ": " + headerValue).append("\n");
                        if (headerEntry.getKey().equals("Content-Disposition")) {
                            dispString += headerValue;
                        }
                    }
                }
                result.append("  mediaType: " + inputPart.getMediaType() + "\n");

                ContentDisposition disp = new ContentDisposition(dispString);
                String fileName = disp.getParameter("filename");
                if (fileName == null) {
                    fileName = name;
                }

                String tmpFileName = null;
                try {
                    tmpFileName = copyToTempFile(fileName, inputPart.getBody(InputStream.class, null));
                    result.append(checkOne(db, cache, tmpFileName));

                } finally {
                    if (tmpFileName != null) {
                        deleteTempFile(tmpFileName);
                    }
                }
            }
        }

        if (!foundAtLeastOne) {
            result.append("no parts found\n");
        }
        result.append("end of results\n");
        return result.toString();
    }

    private String checkOne(VictimsDBInterface db, VictimsResultCache cache, String arg) throws Exception {

            StringBuilder result = new StringBuilder();
       
            result.append("filename: ").append(arg).append("\n");

            String key = null;   //checksum(arg);
            result.append("key: ");
            result.append(key);
            result.append("\n");
            
            // Check cache 
            if (key != null && cache.exists(key)) {
                try {
                    HashSet<String> cves = cache.get(key);
                    if (cves != null && cves.size() > 0) {
                        result.append(String.format("%s VULNERABLE! ", arg));
                        for (String cve : cves) {
                            result.append(cve);
                            result.append(" ");
                        }
                        result.append("\n");
                    } else {
                        result.append(arg + " ok\n");
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

                VictimsScanner.scan(arg, records);
                for (VictimsRecord record : records) {

                    try {
                        HashSet<String> cves = db.getVulnerabilities(record);
                        if (key != null) {
                            cache.add(key, cves);
                        }
                        if (!cves.isEmpty()) {
                            result.append(String.format("%s VULNERABLE! ", arg));
                            for (String cve : cves) {
                                result.append(cve);
                                result.append(" ");
                            }
                            result.append("\n");
                        } else {
                            result.append(arg + " ok\n");
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

    private String checksum(String filename) {
        String hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            InputStream is = new FileInputStream(new File(filename));
            byte[] buffer = new byte[1024];
            while (is.read(buffer) > 0) {
                md.update(buffer);
            }

            byte[] digest = md.digest();
            hash = String.format("%0" + (digest.length << 1) + "X", new BigInteger(1, digest));

        } catch (NoSuchAlgorithmException e) {
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }

        return hash;
    }

    private String createTempDir() throws IOException {
        File t = File.createTempFile("victims", "");
        if (!t.delete()) {
            throw new IOException("could not delete tempfile before creating directory: " + t.getAbsolutePath());
        }
        if (!t.mkdir()) {
            throw new IOException("could not create directory: " + t.getAbsolutePath());
        }
        return t.getAbsolutePath();
    }


    private String copyToTempFile(String fileName, InputStream inputStream) throws IOException {
        File n = new File(createTempDir(), fileName);
        OutputStream outputStream = new FileOutputStream(n);

        int count = 0;
        byte[] buffer = new byte[1024];
        while ((count = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, count);
        }
        outputStream.flush();
        outputStream.close();
        return n.getAbsolutePath();
    }

    private void deleteTempFile(String fileName) {
        File n = new File(fileName);
        n.delete();
        n.getParentFile().delete();
    }
}

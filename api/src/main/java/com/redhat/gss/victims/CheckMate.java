package com.redhat.gss.victims;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
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

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlElement;

@Path("/checkmate")
@Stateless
@LocalBean
public class CheckMate {

    @XmlRootElement(name = "checkresult")
    static public class CheckResult {

        private String data;

        public void setData(String data) {
            this.data = data;
        }

        @XmlElement
        public String getData() {
            return data;
        }

        private String trace;

        public void setTrace(String trace) {
            this.trace = trace;
        }

        @XmlElement
        public String getTrace() {
            return trace;
        }

        private String error;

        public void setError(String error) {
            this.error = error;
        }

        @XmlElement
        public String getError() {
            return error;
        }
    }

    public CheckMate() {

        // This is done at initializer time so that if we have to create a 
        // completely new local copy of the db, it will be done outside of
        // an http request/response cycle.
        try {
            VictimsDBInterface db = VictimsDB.db();
            db.synchronize();

        } catch (VictimsException e) {
            e.printStackTrace();
        }
    }


    @POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces("application/xml")
    public CheckResult checkMultiJAXB(MultipartFormDataInput inputForm, @Context HttpServletRequest request) throws Exception {

        StringBuilder result = new StringBuilder();
        StringBuilder trace = new StringBuilder();
        
        trace.append("multi: ");
        trace.append(displayHeaders(request));

        VictimsDBInterface db;
        VictimsResultCache cache;

        try {
            db = VictimsDB.db();
            cache = new VictimsResultCache();

        } catch (VictimsException e) {
            result.append("VictimsException while opening the database:\n");
            e.printStackTrace();
            result.append(e.toString()).append("\n");

            CheckResult checkResult = new CheckResult();
            checkResult.setData(result.toString());
            checkResult.setTrace(trace.toString());
            checkResult.setError("No Error Found");
            return checkResult;
        }

        try {
            trace.append("About to synchronize local database with upstream ...\n");
            db.synchronize();
            trace.append("   successful synchronize.\n");

        } catch (VictimsException e) {
            result.append("VictimsException while synchronize-ing local database:\n");
            e.printStackTrace();
            result.append(e.toString()).append("\n");

            CheckResult checkResult = new CheckResult();
            checkResult.setData(result.toString());
            checkResult.setTrace(trace.toString());
            checkResult.setError("No Error Found");
            return checkResult;
        }


        boolean foundAtLeastOne = false;
        Map<String, List<InputPart>> multiValuedMap = inputForm.getFormDataMap();
        for (Map.Entry<String, List<InputPart>> entry : multiValuedMap.entrySet()) {
            foundAtLeastOne = true;
            String name = entry.getKey();
            int count = 0;
            trace.append("found part named: " + name + "\n");
            for (InputPart inputPart : entry.getValue()) {
                String dispString = "";
                count++;
                trace.append("found value " + count + ":\n");
                for (Map.Entry<String, List<String>> headerEntry : inputPart.getHeaders().entrySet()) {
                    for (String headerValue : headerEntry.getValue()) {
                        trace.append("  header " + headerEntry.getKey() + ": " + headerValue).append("\n");
                        if (headerEntry.getKey().equals("Content-Disposition")) {
                            dispString += headerValue;
                        }
                    }
                }
                trace.append("  mediaType: " + inputPart.getMediaType() + "\n");

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
            trace.append("no parts found\n");
        }
        trace.append("end of results\n");

        CheckResult checkResult = new CheckResult();
        checkResult.setData(result.toString());
        checkResult.setTrace(trace.toString());
        checkResult.setError("No Error Found");

        return checkResult;
    }

    @POST
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
    public String checkMultiString(MultipartFormDataInput inputForm, @Context HttpServletRequest request) throws Exception {

        StringBuilder result = new StringBuilder();
        StringBuilder trace = new StringBuilder();
        
        trace.append("multi: ");
        trace.append(displayHeaders(request));

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
            trace.append("About to synchronize local database with upstream ...\n");
            db.synchronize();
            trace.append("   successful synchronize.\n");

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
            trace.append("found part named: " + name + "\n");
            for (InputPart inputPart : entry.getValue()) {
                String dispString = "";
                count++;
                trace.append("found value " + count + ":\n");
                for (Map.Entry<String, List<String>> headerEntry : inputPart.getHeaders().entrySet()) {
                    for (String headerValue : headerEntry.getValue()) {
                        trace.append("  header " + headerEntry.getKey() + ": " + headerValue).append("\n");
                        if (headerEntry.getKey().equals("Content-Disposition")) {
                            dispString += headerValue;
                        }
                    }
                }
                trace.append("  mediaType: " + inputPart.getMediaType() + "\n");

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
            trace.append("no parts found\n");
        }
        trace.append("end of results\n");
        return result.toString();
    }

    private String checkOne(VictimsDBInterface db, VictimsResultCache cache, String tmpFileName) throws Exception {
        // tmpFileName is the full (absolute) file name of the temporary copy of the uploaded file
        // it's last path element should be the name of the file that the user attached to it in the uploaded request
 
            String fileName = new File(tmpFileName).getName();

            StringBuilder result = new StringBuilder();
            StringBuilder trace = new StringBuilder();
       
            trace.append("filename: ").append(tmpFileName).append("\n");

            // the cache was giving incorrect results (saying things were OK when they were not, 
            // or vise-versa.  It may be behaving this way because i was screwing with the local 
            // database alot while i was getting this working, or there may be some deeper problem.
            // for now just turn off the cache.
            String key = null;   //checksum(tmpFileName);
            trace.append("key: ");
            trace.append(key);
            trace.append("\n");
            
            // Check cache 
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

                VictimsScanner.scan(tmpFileName, records);
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

    // 
    // Both the cache and the local database want/need to read the whole file seperately, so we must save 
    // the file we are sent locally on the server, so we can read it twice.  We should probably fix this
    // so we dont' need to do this, but we haven't.   
    //
    // For now we create a new temporary directory to store all the files uploaded in one request.  For the
    // processing to work it's best, it is necessary for the name of the file in the temporary directory be
    // the name given to the file as part of the upload request.
    //
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

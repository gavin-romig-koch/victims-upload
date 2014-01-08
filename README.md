This is the deployment directory for an OpenShift app.

This webapp provided a frontend to the http://victi.ms application.  This webapp allows one to upload files for checking against the http://victi.ms database.  It does this simply by putting a webapp wrapper around the client functionality provided by http://victi.ms. 

So far this just consists of a tiny http api which allows one to upload one or more files in a mime multipart form-data post.   The api syncronizes it's own local copy of the victims db with the upstream one at 'victi.ms', and looks at the uploaded files to see if any have any CVE's listed in that db.   The api only handles, and db only contain, info for 'jar' files, 'class' files and 'pom.xml' files.   The api only looks at the name of the file (the part after the last dot) to tell what kind of file it is, so post the files with proper/actual names.   An instance of this api is on the internal openshift at:

   http://victims-labsdev.itos.redhat.com/api/check

There is currently no authentication on this api, nor any HTTP wrapper, and no Portal wrapper.   I'll look into those things next.   But you can try it out using curl:

    curl http://victims-labsdev.itos.redhat.com/api/check  -F ""file=@<name of file you want checked>"  ...

You must include at lest one -F, but you can include as many -F as you like.  The <name of file ...> should be the name of an actual file in your file system that you want checked, not a random name.  Curl only sends the part after the last '/', if any, as the name of the file.  You can override the name of the file (see the curl man page for how).

For example:

$ curl http://victims-labsdev.itos.redhat.com/api/check -F "file=@/home/gavin/projects/andreas/andreas/jacinto/ejb/pom.xml"  -F "file=@/home/gavin/.m2/repository/org/jboss/client/jbossall-client/5.0.0.Beta4/jbossall-client-5.0.0.Beta4.jar" -F "file=@/home/gavin/.m2/repository/org/jboss/kernel/jboss-kernel/2.2.0.GA/jboss-kernel-2.2.0.GA.jar" -F "file=@projects/andreas/andreas/calaveras/target/classes/com/redhat/gss/calaveras/DocKind.class"
pom.xml ok
jbossall-client-5.0.0.Beta4.jar VULNERABLE! CVE-2009-0217
jboss-kernel-2.2.0.GA.jar ok
DocKind.class ok

The jbossall-client-5.0.0.Beta4.jar jar file has a CVE associated with it.  The jboss-kernel jar listed above does not.




This app is based on the the OpenShift `jbosseap` cartridge documentation can be found at:

https://github.com/openshift/origin-server/tree/master/cartridges/openshift-origin-cartridge-jbosseap/README.md

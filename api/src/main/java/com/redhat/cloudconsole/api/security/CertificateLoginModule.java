package com.redhat.cloudconsole.api.security;

import org.jboss.logging.Logger;
import org.jboss.resteasy.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.Signature;
import java.security.SignatureException;
import java.security.acl.Group;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import org.jboss.security.SimpleGroup;
import org.jboss.security.SimplePrincipal;
import org.jboss.security.auth.spi.BaseCertLoginModule;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.login.LoginException;
import javax.security.jacc.PolicyContextException;
import javax.servlet.http.HttpServletRequest;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.util.ASN1Dump;

public class CertificateLoginModule extends BaseCertLoginModule {
	private static final Logger log = Logger
			.getLogger(CertificateLoginModule.class.getName());
	
	
	private X509Certificate validatedCert = null;
	private Principal identity = null;

	protected boolean validateCredential(String alias, X509Certificate cert) {
		boolean valid = super.validateCredential(alias, cert);
		boolean passed = false;
		System.out.println("validating certificate");
		if (valid) {
			validatedCert = cert;
			if (cert != null && CertificateParser.isEntitlementCert(cert)) {
				HttpServletRequest request;
				String entdata = null;
				try {
					if (CertificateParser.isV3Cert(cert)) {
						request = (HttpServletRequest) javax.security.jacc.PolicyContext
								.getContext("javax.servlet.http.HttpServletRequest");
						entdata = request.getHeader(SecurityConstants.HTTP_ENTITLEMENT_DATA_V3_KEY);
						String entsig = request.getHeader(SecurityConstants.HTTP_ENTITLEMENT_DATA_SIG_KEY);
						System.out.println(" Header is " + entdata);
						System.out.println(" Header is " + entsig);
						if (entsig != null && entdata != null) {
							entdata = entdata.replaceAll("\\s+", "");
							entsig = entsig.replaceAll("\\s+", "");
							passed = CertificateVerifier
									.verifyV3EntitlementCertificate(cert,
											Base64.decode(entdata),
											Base64.decode(entsig));
						}
					} else {
						passed = true; // SSL handshake validation is adequate for V1					
					}
				} catch (Exception e) {
					passed = false;
					e.printStackTrace();
				}
				if (passed) {
					try {
						this.identity = new SimplePrincipal(
								CertificateParser.getAccountIdFromCert(cert,
										entdata));
					} catch (Exception e) {
						// For us valid means the certificate is valid AND we
						// have been able to extract the account ID
						e.printStackTrace();
						passed = false;
					}
				}
			}else {
				passed = true;
			}
		}
		return passed;
	}

	protected Principal getIdentity() {
		if (this.identity != null) {
			return this.identity;
		} else {
			return super.getIdentity();
		}
	}

	protected Group[] getRoleSets() throws LoginException

	{
		SimpleGroup rolesGroup = new SimpleGroup("Roles");
		try {
			if (CertificateParser.isEntitlementCert(validatedCert)){
			     rolesGroup.addMember(createIdentity("machine"));
			}else {
				 rolesGroup.addMember(createIdentity("machine"));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ArrayList<Group> groups = new ArrayList<Group>();
		groups.add(rolesGroup);
		Group[] roleSets = new Group[groups.size()];
		groups.toArray(roleSets);
		return roleSets;

	}

}

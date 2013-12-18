package com.redhat.cloudconsole.api.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.jboss.security.auth.certs.X509CertificateVerifier;

public class CertificateVerifier implements X509CertificateVerifier {
	
	// 
	// @see org.candlepin.service.impl.DefaultEntitlementCertServiceAdapter (https://github.com/candlepin/candlepin/tree/master/src)
    // @see org.candlepin.pki.impl.BouncyCastlePKIUtility (https://github.com/candlepin/candlepin/tree/master/src)
	static// TDDO remove example string
	String EXAMPLE_ENT = "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";
	static String EXAMPLE_SIG = "UPDszxWFzo1ATnadCjl2rkynCFM4ThzQQ1NHR7hGD0Z6rO60o9Liw8OdpDGwqqEJLezYP7YP4QKBhX7/rMqi2xBN2ap5QvzVVcYr5M8wCe2Pe13buKARHSUvjgJCPKElZr9RN3USNqhlDuZf3yUJFfhw8bZGcNXPKkmDiSxNISVIPfrYpxaRxgSEk7SHLYKNU7pf1ELwpXHSfy63L0t99gb6W2TqzcGNs5ug7093j3gIcqXTAssQIRxIlDt7sJgEOtwcmu2i82saU9zPE4y3g1trveGVhiQqdVxjD4k0dXmbb99jpbaNjSpBNhfsppYKc9ujrVWkbsoZaAsXCI8mOQ53iOYNCxKhtc0m5qyX6VutiRyp/9uGUMCI/KEVnxJjplNEltpK3VIwkAXoBgnGmcaa0Qwz67MqT4FvSPxqYiBPyxSYLxFsJZrfjlCNMtM50cypNs24zc0EEXyC3FLS+vlS6iatPnr47ioSMEbzBumFUg/fYMiMB8giCrGPrAXaacFaVUAZ7d1nzyKY45GF/18Yhh9851FrOYZiuyaOuAoYJZ4oXVBfdODuNLfvnAoJm/mbG0oTeKTJckDuc5mTqJLpBHas/chNJfaxBlxzQpBF78Gl1imDOZURoBr4xkWBQdcG0PIBTH59V13BK7iOgMA4TEuLLnfmlSrQFfxmtNQ=";

	// @Override
	public boolean verify(X509Certificate arg0, String arg1, KeyStore arg2,
			KeyStore arg3) {
		return true;
	}

	static boolean verifyV3EntitlementCertificate(X509Certificate certificate,
			byte[] entitlementData, byte[] entitlementSignature) {
		return verifySHA256WithRSAHashAgainstCACerts(entitlementData,entitlementSignature);

		//return false;
	}

	public static boolean verifySHA256WithRSAHashAgainstCACerts(byte[] input,
			byte[] signedHash){
		try {

			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init((KeyStore) null);
			for (TrustManager trustManager : trustManagerFactory
					.getTrustManagers()) {
				//System.out.println(trustManager);

				if (trustManager instanceof X509TrustManager) {
					X509TrustManager x509TrustManager = (X509TrustManager) trustManager;
					//System.out.println("\tAccepted issuers count : "
					//		+ x509TrustManager.getAcceptedIssuers());
					for (X509Certificate cert : x509TrustManager
							.getAcceptedIssuers()) {
						//System.out.println("Verify against: " + cert);
						if (verifySHA256WithRSAHash(new ByteArrayInputStream(
								input), signedHash, cert)) {
							//System.out.println("Verified against CA : "
							//		+ cert.getSubjectDN());
							return true;
							
						}
					}
				}
			}
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("Verification failed");
		return false;
	}

	public static boolean verifySHA256WithRSAHash(InputStream input,
			byte[] signedHash, Certificate certificate) {
		try {
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initVerify(certificate);

			updateSignature(input, signature);
			return signature.verify(signedHash);
		} catch (SignatureException se) {
			return false;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static void updateSignature(InputStream input, Signature signature)
			throws IOException, SignatureException {
		byte[] dataBytes = new byte[4096];
		int nread = 0;
		while ((nread = input.read(dataBytes)) != -1) {
			signature.update(dataBytes, 0, nread);
		}
	}

	public static boolean verifyCandlePinGenerated(X509Certificate cert) {
		// TODO Auto-generated method stub
		if (cert == null) return false;
		//System.out.println(cert.getIssuerDN().getName());
		//This is a shortcut verification - we should really be verifying against our trust store.
		return cert.getIssuerDN().getName().equals(SecurityConstants.TRUSTED_ENTITLEMENT_ISSUER);
	}

}

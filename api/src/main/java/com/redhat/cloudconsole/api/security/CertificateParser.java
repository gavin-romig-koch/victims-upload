package com.redhat.cloudconsole.api.security;

//import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.zip.Inflater;

import org.bouncycastle.asn1.ASN1Primitive;
//import org.bouncycastle.asn1.util.ASN1Dump;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.util.encoders.Base64;
import org.codehaus.jackson.map.ObjectMapper;

class CertificateParser {
	static final String REDHAT_OID_NAMESPACE = "1.3.6.1.4.1.2312.9";
	static final int MAX_ENT_DATA_LEN = 100000;

	// V1 OIDS
	// @see https://mojo.redhat.com/docs/DOC-62644
	static final String REDHAT_ORDER_NAMESPACE = REDHAT_OID_NAMESPACE + ".4";
	static final String REDHAT_EXT_ORDER_NAME_OID = REDHAT_OID_NAMESPACE
			+ ".4.1";
	static final String REDHAT_EXT_ACCOUNT_NAME_OID = REDHAT_OID_NAMESPACE
			+ ".4.13";

	// V3 OIDs
	static final String REDHAT_EXT_CERT_VERSION = REDHAT_OID_NAMESPACE + ".6";
	static final String REDHAT_EXT_ENT_PAYLOAD = REDHAT_OID_NAMESPACE + ".7";
	
	static final String PEM_CERT_HEADER = "-----BEGIN CERTIFICATE-----";
	static final String PEM_CERT_TRAILER = "-----END CERTIFICATE-----";

	public static String zlibDecompress(byte[] data) {
		String output = "";
		try {

			Inflater decompresser = new Inflater();
			decompresser.setInput(data, 0, data.length);
			byte[] result = new byte[MAX_ENT_DATA_LEN];
			int resultLength = decompresser.inflate(result);
			decompresser.end();
			// Decode the bytes into a String
			output = new String(result, 0, resultLength, "UTF-8");
		} catch (java.io.UnsupportedEncodingException ex) {
			// handle
			ex.printStackTrace();
		} catch (java.util.zip.DataFormatException ex) {
			// handle
			ex.printStackTrace();
		}
		return output;

	}

	public static boolean isV3Cert(X509Certificate cert) {
		return cert.getExtensionValue(CertificateParser.REDHAT_EXT_ENT_PAYLOAD) != null;
	}

	public static byte[] zlibDecompressRaw(byte[] data) {
		byte[] output = null;
		try {

			Inflater decompresser = new Inflater();
			decompresser.setInput(data, 0, data.length);
			byte[] result = new byte[MAX_ENT_DATA_LEN];
			int resultLength = decompresser.inflate(result);
			decompresser.end();
			output = Arrays.copyOf(result, resultLength);
		} catch (java.util.zip.DataFormatException ex) {
			// handle
			ex.printStackTrace();
		}
		return output;

	}

	public static boolean isEntitlementCert(X509Certificate cert) {

		return (cert
				.getExtensionValue(CertificateParser.REDHAT_EXT_ENT_PAYLOAD) != null)
				|| (cert.getExtensionValue(CertificateParser.REDHAT_EXT_ORDER_NAME_OID) != null);
	}

	public static String getAccountIdFromCert(X509Certificate cert,
			String entData) throws Exception {

		if (isV3Cert(cert)) {
			return getAccountIdFromEntityData(entData);
		} else {
			//System.out
			//		.println("Oids are " + cert.getNonCriticalExtensionOIDs());
			if (cert.getExtensionValue(REDHAT_EXT_ACCOUNT_NAME_OID) != null) {
				ASN1Primitive parsed = JcaX509ExtensionUtils
						.parseExtensionValue(cert
								.getExtensionValue(REDHAT_EXT_ACCOUNT_NAME_OID));
				// log.debug(ASN1Dump.dumpAsString(parsed));
				return parsed.toString();
			} else {
				throw new Exception("Invalid Certificate");
			}
		}
	}
	
	public static X509Certificate createCert(byte[] certData) throws Exception {
  
            CertificateFactory cf = CertificateFactory.getInstance("X509");
            X509Certificate cert = (X509Certificate) cf
            .generateCertificate(new ByteArrayInputStream(certData));
            return cert;
            
    }
	
	public static X509Certificate createCert(String base64CertData) throws Exception {
		
		
		//deliberately not checking for null
		base64CertData = base64CertData.replaceAll(PEM_CERT_HEADER, "");
		base64CertData = base64CertData.replaceAll(PEM_CERT_TRAILER, "");
		base64CertData = base64CertData.trim();
		byte [] certData = Base64.decode(base64CertData);
        return createCert(certData);
        
}
    

	private static String getAccountIdFromEntityData(String entData)
			throws Exception {
		if (entData == null)
			throw new Exception("Invalid Data");
		String jsonData = zlibDecompress(Base64.decode(entData));
		Map<String, Object> data = (Map<String, Object>) fromJson(jsonData,
				Map.class);
		Map<String, Object> order = (Map<String, Object>) data.get("order");
		String accountID = (String) order.get("account");
		if (accountID == null) {
			throw new Exception("Invalid data");
		}
		return accountID;
	}

	@SuppressWarnings("unchecked")
	private static Object fromJson(String json,
			@SuppressWarnings("rawtypes") Class clazz) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		Object output = null;
		output = mapper.readValue(json, clazz);

		return output;
	}

}
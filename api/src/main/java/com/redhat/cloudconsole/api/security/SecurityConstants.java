package com.redhat.cloudconsole.api.security;

public class SecurityConstants {
	
	//Security Roles
	public static final String MACHINE_ROLE = "Machine";
	public static final String ACCOUNT_OWNER_ROLE = "AccountOwner";
	public static final String REDHAT_SU_ROLE = "RhAdmin";
	
	//HTTP Header fields
	public static final String HTTP_CLIENT_CERT_KEY= "X-Forwarded-SSL-Client-Cert";
	public static final String HTTP_ENTITLEMENT_DATA_V3_KEY = "X-RH-Entitlement-Data";
	public static final String HTTP_ENTITLEMENT_DATA_SIG_KEY= "X-RH-Entitlement-Sig";
	
	
	public static final String ACCOUNT_ID = "ACCOUNT_ID";
	public static final String UUID = "UUID";
	public static final String TRUSTED_ENTITLEMENT_ISSUER ="EMAILADDRESS=ca-support@redhat.com, CN=Red Hat Candlepin Authority, OU=Red Hat Network, O=\"Red Hat, Inc.\", ST=North Carolina, C=US";
	
}

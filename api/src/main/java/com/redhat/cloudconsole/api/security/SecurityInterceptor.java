package com.redhat.cloudconsole.api.security;

import java.lang.reflect.Method;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.http.HttpStatus;
import org.bouncycastle.util.encoders.Base64;
import org.jboss.logging.Logger;
//import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.json.JSONObject;

@Provider
@ServerInterceptor
public class SecurityInterceptor implements PreProcessInterceptor, ExceptionMapper<Throwable> {

	public static final String AUTH_URL = System.getProperty("auth_url");
	public static final String ACCOUNT_ID = "ACCOUNT_ID";
	public static final String USER_ID = "USER_ID";
	public static final String IS_INTERNAL = "IS_INTERNAL";
	public static final String RH_SSO = "rh_sso";
	private static final String TRIM_CHARS = "(null\\()(.+?)(\\))";
	
	

	private static final Logger log = Logger
			.getLogger(SecurityInterceptor.class.getName());

	/**
	 * Ensures that the incoming request possesses a valid cookie from
	 * Redhat.com. For posterity, the following 3 Red Hat JSON responses are
	 * recorded. These all are accompanied with HTTP 200's
	 * 
	 * Case 1: User has an invalid cookie (e.g. they clicked logout or it timed
	 * out). { "authorized": false, <--- Notice "internal": false, "lang":
	 * "en_US", "lang_err_msg":
	 * "Some stupid message here.  (Original stricken by KAR)", "hello":
	 * "Hello,", "description_placeholder": "Enter a description" }
	 * 
	 * Case 2: User has a valid cookie but no entitlements. Notice the lack of
	 * an account_number. { "authorized": true, "internal": false, "login":
	 * "kroberts-test", "user_id": "kroberts-test", "account_id": 7119473,
	 * "name": "Keith Robertson", "lang": "en", "lang_err_msg":
	 * "Some stupid message here.  (Original stricken by KAR)", "hello":
	 * "Hello,", "description_placeholder": "Enter a description" }
	 * 
	 * Case 3: User has valid cookie and an entitlement. { "authorized": true,
	 * "internal": true, "login": "rhn-support-kroberts", "user_id":
	 * "rhn-support-kroberts", "account_id": 5838412, "account_number": 540155,
	 * "name": "Keith Robertson", "lang": "en", "lang": "en", "lang_err_msg":
	 * "Some stupid message here.  (Original stricken by KAR)", "hello":
	 * "Hello,", "description_placeholder": "Enter a description" }
	 * 
	 * @return null if the incoming request was successfully authorized by
	 *         AUTH_URL
	 */
	@Override
	public ServerResponse preProcess(HttpRequest request, ResourceMethod method)
			throws Failure, WebApplicationException {
		Response response = null;
	
		if (System.getProperty("devmode").equalsIgnoreCase("true")) {
			log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			log.error("!!! Application security is disabled via cloudconsole.properties !!!");
			log.error("!!!     devmode=true all api calls are pinned to subengdev       !!!");
			log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			request.setAttribute(ACCOUNT_ID, "540155");
			request.setAttribute(USER_ID, "subengdev");
			request.setAttribute(IS_INTERNAL, "true");
			return null;
		}

		Method m = method.getMethod();
		if (log.isDebugEnabled()) {
			HttpHeaders headers = request.getHttpHeaders();
			for (String header : headers.getRequestHeaders().keySet()) {
				System.out.println("Header:" + header + "   Value:"
						+ headers.getRequestHeader(header));
			}
		}

		if (m.isAnnotationPresent(ConsoleRolesAllowed.class)) {
			ConsoleRolesAllowed rolesAnnotation = m
					.getAnnotation(ConsoleRolesAllowed.class);
			Set<String> rolesSet = new HashSet<String>(
					Arrays.asList(rolesAnnotation.value()));
			if (requiresCertAuth(rolesSet)) {
				response = this.validateMachineCall(request, rolesSet);
			} else {
				response = this.validateUserCall(request, rolesSet);
			}
		} else {
			response = this.validateUserCall(request, null); // TODO Code
																// review
		}
		
		
		if (response != null) {
			throw new WebApplicationException(response);
		} else {
			// Returning null translates to a success for RESTeasy, and
			// if this method returns null RESTeasy will invoke the underlying
			// REST endpoint.
			return null;
		}
	}

	private boolean requiresCertAuth(Set<String> rolesSet) {
		assert (rolesSet != null);
		return rolesSet.contains(SecurityConstants.ACCOUNT_OWNER_ROLE)
				|| rolesSet.contains(SecurityConstants.MACHINE_ROLE);
	}
	
	
	//Checks that that user has at least one role in called method
	private boolean isUserInRole(Set<String> userRolesSet, Set<String> calleeRolesSet){
		if (userRolesSet == null || calleeRolesSet == null) return false;
		for (String role: userRolesSet){
			if (calleeRolesSet.contains(role)) return true;
		}
		return false;
	}

	private Response validateMachineCall(HttpRequest request,
			Set<String> rolesSet) {

		Response response = makeAuthFailResponse("Invalid Certicate",
				Status.UNAUTHORIZED, null); // fail safe , since null means
											// success in RestEasy
		String certBase64 = this.getHttpHeader(request,
				SecurityConstants.HTTP_CLIENT_CERT_KEY);
		if (certBase64 != null && certBase64.trim().length() > 0) {
			X509Certificate cert;
			try {
				cert = CertificateParser.createCert(certBase64);
				String entdata = null;
				if (CertificateParser.isEntitlementCert(cert)) {
					boolean isValidEntCert = false;
					if (CertificateParser.isV3Cert(cert)) {
						entdata = this.getHttpHeader(request,
								SecurityConstants.HTTP_ENTITLEMENT_DATA_V3_KEY);
						String entsig = this
								.getHttpHeader(
										request,
										SecurityConstants.HTTP_ENTITLEMENT_DATA_SIG_KEY);
						if (entsig != null && entdata != null) {
							entdata = entdata.replaceAll("\\s+", "");
							entsig = entsig.replaceAll("\\s+", "");
							isValidEntCert = CertificateVerifier
									.verifyV3EntitlementCertificate(cert,
											Base64.decode(entdata),
											Base64.decode(entsig));
						}
					} else {
						System.out.println("Received V1 Certificate");
						isValidEntCert = CertificateVerifier
								.verifyCandlePinGenerated(cert);
						//System.out.println("certificate is valid " + isValidEntCert);
					} 
					if (isValidEntCert){
						request.setAttribute(ACCOUNT_ID,CertificateParser.getAccountIdFromCert(cert,
								entdata));
						//TODO refactor generally bad idea  to set security data on unprivileged thread
						Set<String> userRoles = new HashSet<String>();
						userRoles.add(SecurityConstants.ACCOUNT_OWNER_ROLE);
						if (this.isUserInRole (userRoles,rolesSet)) {
							response = null; //success
						}else {
							throw new Exception("User not in required role set" + rolesSet);
						}
					} 
				} else {
					if (CertificateVerifier.verifyCandlePinGenerated(cert)) {
						//We assume its an identity certificate
						request.setAttribute(SecurityConstants.UUID,cert.getSubjectDN().getName().replace("CN=", ""));//need to remove the "CN=" to get UUID
						System.out.println("Method called with user identity : "+cert.getSubjectDN().getName() );
						//TODO refactor generally bad idea  to set security data on unprivileged thread
						Set<String> userRoles = new HashSet<String>();
						userRoles.add(SecurityConstants.MACHINE_ROLE);
						if (this.isUserInRole (userRoles,rolesSet)) {
							response = null; //success
						}else {
							throw new Exception("User not in required role set" + rolesSet );
						}
					} else {
						throw new Exception("Certificate not signed by candlepin certificate"); //just for debug
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else {
			log.debug("Client not authenticated via TLS mutual authentication");
		}

		return response;
	}

	private Response validateUserCall(HttpRequest request, Set<String> rolesSet) {
		Response response = null;
		Map<String, Cookie> cookies = request.getHttpHeaders().getCookies();
		if (!cookies.isEmpty() && cookies.containsKey(RH_SSO)) {
			log.debug("Cookies: " + cookies);
			ClientRequest ssoClientReq = makeAuthRequest(cookies);
			try {
				ClientResponse<String> ssoResponse = ssoClientReq
						.get(String.class);
				int code = ssoResponse.getStatus();
				log.debug("SSO status code: " + code);
				if (code == HttpStatus.SC_OK) {
					try {
						String strResp = ssoResponse.getEntity().trim();
						log.debug("SSO response(" + strResp + ")");
						Pattern pattern = Pattern.compile(TRIM_CHARS,
								Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
						Matcher matcher = pattern.matcher(strResp);
						String json = matcher.replaceAll("$2");
						log.debug("Trimmed SSO response: " + json);
						JSONObject jsonResponse = new JSONObject(json);
						if (jsonResponse.getBoolean("authorized")) {
							// User has a valid SSO cookie.
							if (jsonResponse.has("account_number")) {
								long accountId = jsonResponse
										.getLong("account_number");
								String userId = jsonResponse
										.getString("user_id");
								log.debug("User(" + userId
										+ ") with account ID (" + accountId
										+ ") has successfully authenticated.");
								request.setAttribute(ACCOUNT_ID,
										Long.toString(accountId));
								request.setAttribute(USER_ID, userId);
								request.setAttribute(IS_INTERNAL, Boolean
										.toString(jsonResponse
												.getBoolean("internal")));
							} else {
								// User has no entitlements
								response = makeAuthFailResponse(
										"No entitlements", Status.UNAUTHORIZED,
										json);
							}
						} else {
							// User has an invalid SSO cookie
							response = makeAuthFailResponse(
									"Invalid SSO cookie", Status.UNAUTHORIZED,
									json);
						}
					} catch (Exception e) {
						log.debug(e);
						response = makeAuthFailResponse(e.toString(),
								Status.INTERNAL_SERVER_ERROR, null);
					}
				} else {
					// HTTP Status code was not 200
					log.warn("Auth fail with code(" + code + ") for cookie: "
							+ cookies);
					response = makeAuthFailResponse(
							"redhat.com auth fail with code: " + code,
							Status.UNAUTHORIZED, ssoResponse.getEntity());
				}
			} catch (Exception e) {
				log.warn("Unable to communicate with RH.com, returning UNAUTHORIZED. Exception: "
						+ e);
				response = makeAuthFailResponse(
						"Unable to authenticate with redhat.com",
						Status.INTERNAL_SERVER_ERROR, null);
			}
		} else {
			log.warn("HTTP request does not have \'rh_sso\' cookie.  Returning UNAUTHORIZED");
			response = makeAuthFailResponse("Agent did not supply rh_sso cookie.",
					Status.UNAUTHORIZED, null);
		}
		return response;
	}

	@Override
	public Response toResponse(Throwable arg0) {
		log.warn("Problem discovered in SecurityInterceptor: " + arg0);

		if (arg0 instanceof WebApplicationException) {
			return ((WebApplicationException) arg0).getResponse();
		} else {
			ResponseBuilder rb = Response.status(Status.INTERNAL_SERVER_ERROR);
			rb.type(MediaType.APPLICATION_JSON);
			rb.entity(new JSONObject().put("ERROR", arg0.getMessage())
					.toString());
			return rb.build();
		}
	}

	/**
	 * @param cookies
	 *            Cookies from the incoming HTTP request
	 * @return A client request with cookies copied from the incoming request
	 *         and the accept type set to JSON
	 */
	private ClientRequest makeAuthRequest(Map<String, Cookie> cookies) {
		log.debug("Authenticating rh_sso(" + 
				  cookies.get(RH_SSO) + ") via(" +
				  AUTH_URL + ")");
		ClientRequest client = new ClientRequest(AUTH_URL);
		client.cookie(cookies.get(RH_SSO));
		client.accept(MediaType.APPLICATION_JSON);
		return client;
	}

	private Response makeAuthFailResponse(String message, Status status,
			String ssoResponse) {
		HashMap<String, String> err = new HashMap<String, String>();
		err.put("message", message);
		err.put("type", status.getReasonPhrase());
		err.put("code", status.getStatusCode() + "");
		err.put("rh_sso_response", (ssoResponse != null) ? ssoResponse : "none");
		JSONObject resp = new JSONObject();
		resp.put("error", err);

		ResponseBuilder rb = Response.status(status);
		rb.type(MediaType.APPLICATION_JSON);
		rb.entity(resp.toString());
		return rb.build();
	}

	// Get a header with supplied key, assuming only one value
	private String getHttpHeader(HttpRequest request, String key) {
		HttpHeaders headers = request.getHttpHeaders();
		List<String> value = headers.getRequestHeader(key);
		if (value != null && value.size() > 0) {
			return value.get(0);
		}
		return null;
	}
	
	
	/*
	 * Validates that the uuid supplied matches the identity of the remote agent making the request
	 */
	public static void checkValidUuid(HttpServletRequest req, String uuid)
			throws Exception {

		System.out.println("Expected   UUID: "
				+ req.getAttribute(SecurityConstants.UUID));
		System.out.println("Supplied UUID: " + uuid);
		//TODO uncomment code when sso issue resolved
		if (System.getProperty("devmode").equalsIgnoreCase("true")) {
			// skip check in dev mode
			log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			log.error("!!!     devmode=true UUID integrity check skipped         !!!");
			log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			return;
		}
		if ((uuid == null)
				|| (req.getAttribute(SecurityConstants.UUID) == null)) {
			throw new SecurityException("Invalid UUID Parameter");
		} else if (!(uuid.trim().equals(req
				.getAttribute(SecurityConstants.UUID).toString().trim()))) {

			throw new SecurityException("Invalid UUID : " + uuid);

		}
	}
    
	/*
	 * Validates that the accountId supplied matches the account of the remote agent making the request
	 */
	public static void checkValidAccountId(HttpServletRequest req,
			String accountId) throws Exception {

		System.out.println("Expected AccountID :"
				+ req.getAttribute(SecurityConstants.ACCOUNT_ID));
		System.out.println("Supplied AccountID : " + accountId);
		//TODO uncomment code when SSO issue resolved
		if (System.getProperty("devmode").equalsIgnoreCase("true")) {
			// skip check in dev mode
			log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			log.error("!!!     devmode=true account ID integrity check skipped         !!!");
			log.error("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			return;
		}
		if ((accountId == null)
				|| (req.getAttribute(SecurityConstants.ACCOUNT_ID) == null)) {
			throw new SecurityException("Invalid account ID Parameter");
		} else if (!(accountId.trim().equals(req
				.getAttribute(SecurityConstants.ACCOUNT_ID).toString().trim()))) {

			throw new SecurityException("Invalid Account ID : " + accountId);

		}
	}
	

	

}

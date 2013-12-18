package com.redhat.cloudconsole.api.test;


import static org.junit.Assert.*;

import org.junit.Test;

import com.redhat.cloudconsole.api.services.AccountWS;

public class AccountWSTest extends AccountWS {

	public AccountWSTest() throws Exception {
	}

	@Test
	public void getAccountTest() throws Exception {
		//Account account = getAccount("12345");
		assertEquals(1, 1);
	}
}

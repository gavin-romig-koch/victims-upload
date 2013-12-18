package com.redhat.chrometwo.api.persistence.dao;

import javax.ejb.LocalBean;
import javax.ejb.Stateless;


@Stateless
@LocalBean
public class AccountDao {

	public String doSomeBackendFunction(){
		return "yaay";
	}

}

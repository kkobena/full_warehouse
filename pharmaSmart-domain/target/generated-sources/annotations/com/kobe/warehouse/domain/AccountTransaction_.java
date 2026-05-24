package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.AccountTransaction}
 **/
@StaticMetamodel(AccountTransaction.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AccountTransaction_ extends PaymentTransaction_ {

	
	/**
	 * @see #account
	 **/
	public static final String ACCOUNT = "account";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.AccountTransaction}
	 **/
	public static volatile EntityType<AccountTransaction> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AccountTransaction#account}
	 **/
	public static volatile SingularAttribute<AccountTransaction, CustomerAccount> account;

}


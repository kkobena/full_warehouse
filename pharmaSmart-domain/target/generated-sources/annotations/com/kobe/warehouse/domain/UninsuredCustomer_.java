package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.UninsuredCustomer}
 **/
@StaticMetamodel(UninsuredCustomer.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class UninsuredCustomer_ extends Customer_ {

	
	/**
	 * @see #account
	 **/
	public static final String ACCOUNT = "account";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.UninsuredCustomer}
	 **/
	public static volatile EntityType<UninsuredCustomer> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UninsuredCustomer#account}
	 **/
	public static volatile SingularAttribute<UninsuredCustomer, CustomerAccount> account;

}


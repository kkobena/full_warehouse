package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.AccountType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.CustomerAccount}
 **/
@StaticMetamodel(CustomerAccount.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class CustomerAccount_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #balance
	 **/
	public static final String BALANCE = "balance";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #customer
	 **/
	public static final String CUSTOMER = "customer";
	
	/**
	 * @see #accountType
	 **/
	public static final String ACCOUNT_TYPE = "accountType";
	
	/**
	 * @see #enabled
	 **/
	public static final String ENABLED = "enabled";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.CustomerAccount}
	 **/
	public static volatile EntityType<CustomerAccount> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CustomerAccount#id}
	 **/
	public static volatile SingularAttribute<CustomerAccount, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CustomerAccount#balance}
	 **/
	public static volatile SingularAttribute<CustomerAccount, Integer> balance;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CustomerAccount#createdAt}
	 **/
	public static volatile SingularAttribute<CustomerAccount, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CustomerAccount#updatedAt}
	 **/
	public static volatile SingularAttribute<CustomerAccount, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CustomerAccount#customer}
	 **/
	public static volatile SingularAttribute<CustomerAccount, UninsuredCustomer> customer;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CustomerAccount#accountType}
	 **/
	public static volatile SingularAttribute<CustomerAccount, AccountType> accountType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CustomerAccount#enabled}
	 **/
	public static volatile SingularAttribute<CustomerAccount, Boolean> enabled;

}


package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.CashSale}
 **/
@StaticMetamodel(CashSale.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class CashSale_ extends Sales_ {

	
	/**
	 * @see #account
	 **/
	public static final String ACCOUNT = "account";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.CashSale}
	 **/
	public static volatile EntityType<CashSale> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashSale#account}
	 **/
	public static volatile SingularAttribute<CashSale, CustomerAccount> account;

}


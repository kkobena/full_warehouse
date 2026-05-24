package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.SalePayment}
 **/
@StaticMetamodel(SalePayment.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class SalePayment_ extends PaymentTransaction_ {

	
	/**
	 * @see #sale
	 **/
	public static final String SALE = "sale";
	
	/**
	 * @see #partAssure
	 **/
	public static final String PART_ASSURE = "partAssure";
	
	/**
	 * @see #partTiersPayant
	 **/
	public static final String PART_TIERS_PAYANT = "partTiersPayant";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.SalePayment}
	 **/
	public static volatile EntityType<SalePayment> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalePayment#sale}
	 **/
	public static volatile SingularAttribute<SalePayment, Sales> sale;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalePayment#partAssure}
	 **/
	public static volatile SingularAttribute<SalePayment, Integer> partAssure;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalePayment#partTiersPayant}
	 **/
	public static volatile SingularAttribute<SalePayment, Integer> partTiersPayant;

}


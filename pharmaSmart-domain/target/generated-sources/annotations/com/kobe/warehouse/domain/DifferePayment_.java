package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.DifferePayment}
 **/
@StaticMetamodel(DifferePayment.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class DifferePayment_ extends PaymentTransaction_ {

	
	/**
	 * @see #differeCustomer
	 **/
	public static final String DIFFERE_CUSTOMER = "differeCustomer";
	
	/**
	 * @see #differePaymentItems
	 **/
	public static final String DIFFERE_PAYMENT_ITEMS = "differePaymentItems";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.DifferePayment}
	 **/
	public static volatile EntityType<DifferePayment> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DifferePayment#differeCustomer}
	 **/
	public static volatile SingularAttribute<DifferePayment, Customer> differeCustomer;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DifferePayment#differePaymentItems}
	 **/
	public static volatile ListAttribute<DifferePayment, DifferePaymentItem> differePaymentItems;

}


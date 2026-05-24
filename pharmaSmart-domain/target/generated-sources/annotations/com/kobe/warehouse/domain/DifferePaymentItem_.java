package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.DifferePaymentItem}
 **/
@StaticMetamodel(DifferePaymentItem.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class DifferePaymentItem_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #expectedAmount
	 **/
	public static final String EXPECTED_AMOUNT = "expectedAmount";
	
	/**
	 * @see #paidAmount
	 **/
	public static final String PAID_AMOUNT = "paidAmount";
	
	/**
	 * @see #sale
	 **/
	public static final String SALE = "sale";
	
	/**
	 * @see #differePayment
	 **/
	public static final String DIFFERE_PAYMENT = "differePayment";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.DifferePaymentItem}
	 **/
	public static volatile EntityType<DifferePaymentItem> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DifferePaymentItem#id}
	 **/
	public static volatile SingularAttribute<DifferePaymentItem, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DifferePaymentItem#expectedAmount}
	 **/
	public static volatile SingularAttribute<DifferePaymentItem, Integer> expectedAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DifferePaymentItem#paidAmount}
	 **/
	public static volatile SingularAttribute<DifferePaymentItem, Integer> paidAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DifferePaymentItem#sale}
	 **/
	public static volatile SingularAttribute<DifferePaymentItem, Sales> sale;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DifferePaymentItem#differePayment}
	 **/
	public static volatile SingularAttribute<DifferePaymentItem, DifferePayment> differePayment;

}


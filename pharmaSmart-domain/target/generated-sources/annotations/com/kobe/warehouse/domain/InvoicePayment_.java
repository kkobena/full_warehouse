package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.InvoicePayment}
 **/
@StaticMetamodel(InvoicePayment.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class InvoicePayment_ extends PaymentTransaction_ {

	
	/**
	 * @see #factureTiersPayant
	 **/
	public static final String FACTURE_TIERS_PAYANT = "factureTiersPayant";
	
	/**
	 * @see #invoicePaymentItems
	 **/
	public static final String INVOICE_PAYMENT_ITEMS = "invoicePaymentItems";
	
	/**
	 * @see #parent
	 **/
	public static final String PARENT = "parent";
	
	/**
	 * @see #grouped
	 **/
	public static final String GROUPED = "grouped";
	
	/**
	 * @see #invoicePayments
	 **/
	public static final String INVOICE_PAYMENTS = "invoicePayments";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.InvoicePayment}
	 **/
	public static volatile EntityType<InvoicePayment> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InvoicePayment#factureTiersPayant}
	 **/
	public static volatile SingularAttribute<InvoicePayment, FactureTiersPayant> factureTiersPayant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InvoicePayment#invoicePaymentItems}
	 **/
	public static volatile ListAttribute<InvoicePayment, InvoicePaymentItem> invoicePaymentItems;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InvoicePayment#parent}
	 **/
	public static volatile SingularAttribute<InvoicePayment, InvoicePayment> parent;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InvoicePayment#grouped}
	 **/
	public static volatile SingularAttribute<InvoicePayment, Boolean> grouped;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InvoicePayment#invoicePayments}
	 **/
	public static volatile ListAttribute<InvoicePayment, InvoicePayment> invoicePayments;

}


package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.InvoicePaymentItem}
 **/
@StaticMetamodel(InvoicePaymentItem.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class InvoicePaymentItem_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #transactionDate
	 **/
	public static final String TRANSACTION_DATE = "transactionDate";
	
	/**
	 * @see #amount
	 **/
	public static final String AMOUNT = "amount";
	
	/**
	 * @see #paidAmount
	 **/
	public static final String PAID_AMOUNT = "paidAmount";
	
	/**
	 * @see #thirdPartySaleLine
	 **/
	public static final String THIRD_PARTY_SALE_LINE = "thirdPartySaleLine";
	
	/**
	 * @see #invoicePayment
	 **/
	public static final String INVOICE_PAYMENT = "invoicePayment";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.InvoicePaymentItem}
	 **/
	public static volatile EntityType<InvoicePaymentItem> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InvoicePaymentItem#id}
	 **/
	public static volatile SingularAttribute<InvoicePaymentItem, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InvoicePaymentItem#transactionDate}
	 **/
	public static volatile SingularAttribute<InvoicePaymentItem, LocalDate> transactionDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InvoicePaymentItem#amount}
	 **/
	public static volatile SingularAttribute<InvoicePaymentItem, Integer> amount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InvoicePaymentItem#paidAmount}
	 **/
	public static volatile SingularAttribute<InvoicePaymentItem, Integer> paidAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InvoicePaymentItem#thirdPartySaleLine}
	 **/
	public static volatile SingularAttribute<InvoicePaymentItem, ThirdPartySaleLine> thirdPartySaleLine;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InvoicePaymentItem#invoicePayment}
	 **/
	public static volatile SingularAttribute<InvoicePaymentItem, InvoicePayment> invoicePayment;

}


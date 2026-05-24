package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.CashRegisterItem}
 **/
@StaticMetamodel(CashRegisterItem.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class CashRegisterItem_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #cashRegister
	 **/
	public static final String CASH_REGISTER = "cashRegister";
	
	/**
	 * @see #amount
	 **/
	public static final String AMOUNT = "amount";
	
	/**
	 * @see #paymentMode
	 **/
	public static final String PAYMENT_MODE = "paymentMode";
	
	/**
	 * @see #typeFinancialTransaction
	 **/
	public static final String TYPE_FINANCIAL_TRANSACTION = "typeFinancialTransaction";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.CashRegisterItem}
	 **/
	public static volatile EntityType<CashRegisterItem> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegisterItem#id}
	 **/
	public static volatile SingularAttribute<CashRegisterItem, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegisterItem#cashRegister}
	 **/
	public static volatile SingularAttribute<CashRegisterItem, CashRegister> cashRegister;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegisterItem#amount}
	 **/
	public static volatile SingularAttribute<CashRegisterItem, Long> amount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegisterItem#paymentMode}
	 **/
	public static volatile SingularAttribute<CashRegisterItem, PaymentMode> paymentMode;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.CashRegisterItem#typeFinancialTransaction}
	 **/
	public static volatile SingularAttribute<CashRegisterItem, TypeFinancialTransaction> typeFinancialTransaction;

}


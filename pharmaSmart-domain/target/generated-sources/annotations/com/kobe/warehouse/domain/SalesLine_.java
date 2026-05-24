package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.SalesLine}
 **/
@StaticMetamodel(SalesLine.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class SalesLine_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #saleDate
	 **/
	public static final String SALE_DATE = "saleDate";
	
	/**
	 * @see #quantitySold
	 **/
	public static final String QUANTITY_SOLD = "quantitySold";
	
	/**
	 * @see #quantityRequested
	 **/
	public static final String QUANTITY_REQUESTED = "quantityRequested";
	
	/**
	 * @see #quantityUg
	 **/
	public static final String QUANTITY_UG = "quantityUg";
	
	/**
	 * @see #quantityAvoir
	 **/
	public static final String QUANTITY_AVOIR = "quantityAvoir";
	
	/**
	 * @see #regularUnitPrice
	 **/
	public static final String REGULAR_UNIT_PRICE = "regularUnitPrice";
	
	/**
	 * @see #discountUnitPrice
	 **/
	public static final String DISCOUNT_UNIT_PRICE = "discountUnitPrice";
	
	/**
	 * @see #netUnitPrice
	 **/
	public static final String NET_UNIT_PRICE = "netUnitPrice";
	
	/**
	 * @see #discountAmount
	 **/
	public static final String DISCOUNT_AMOUNT = "discountAmount";
	
	/**
	 * @see #salesAmount
	 **/
	public static final String SALES_AMOUNT = "salesAmount";
	
	/**
	 * @see #taxValue
	 **/
	public static final String TAX_VALUE = "taxValue";
	
	/**
	 * @see #costAmount
	 **/
	public static final String COST_AMOUNT = "costAmount";
	
	/**
	 * @see #calculationBasePrice
	 **/
	public static final String CALCULATION_BASE_PRICE = "calculationBasePrice";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #sales
	 **/
	public static final String SALES = "sales";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #effectiveUpdateDate
	 **/
	public static final String EFFECTIVE_UPDATE_DATE = "effectiveUpdateDate";
	
	/**
	 * @see #toIgnore
	 **/
	public static final String TO_IGNORE = "toIgnore";
	
	/**
	 * @see #amountToBeTakenIntoAccount
	 **/
	public static final String AMOUNT_TO_BE_TAKEN_INTO_ACCOUNT = "amountToBeTakenIntoAccount";
	
	/**
	 * @see #afterStock
	 **/
	public static final String AFTER_STOCK = "afterStock";
	
	/**
	 * @see #tauxRemise
	 **/
	public static final String TAUX_REMISE = "tauxRemise";
	
	/**
	 * @see #initStock
	 **/
	public static final String INIT_STOCK = "initStock";
	
	/**
	 * @see #lots
	 **/
	public static final String LOTS = "lots";
	
	/**
	 * @see #rates
	 **/
	public static final String RATES = "rates";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.SalesLine}
	 **/
	public static volatile EntityType<SalesLine> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#id}
	 **/
	public static volatile SingularAttribute<SalesLine, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#saleDate}
	 **/
	public static volatile SingularAttribute<SalesLine, LocalDate> saleDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#quantitySold}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> quantitySold;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#quantityRequested}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> quantityRequested;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#quantityUg}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> quantityUg;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#quantityAvoir}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> quantityAvoir;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#regularUnitPrice}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> regularUnitPrice;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#discountUnitPrice}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> discountUnitPrice;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#netUnitPrice}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> netUnitPrice;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#discountAmount}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> discountAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#salesAmount}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> salesAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#taxValue}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> taxValue;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#costAmount}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> costAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#calculationBasePrice}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> calculationBasePrice;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#createdAt}
	 **/
	public static volatile SingularAttribute<SalesLine, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#updatedAt}
	 **/
	public static volatile SingularAttribute<SalesLine, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#sales}
	 **/
	public static volatile SingularAttribute<SalesLine, Sales> sales;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#produit}
	 **/
	public static volatile SingularAttribute<SalesLine, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#effectiveUpdateDate}
	 **/
	public static volatile SingularAttribute<SalesLine, LocalDateTime> effectiveUpdateDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#toIgnore}
	 **/
	public static volatile SingularAttribute<SalesLine, Boolean> toIgnore;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#amountToBeTakenIntoAccount}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> amountToBeTakenIntoAccount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#afterStock}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> afterStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#tauxRemise}
	 **/
	public static volatile SingularAttribute<SalesLine, Float> tauxRemise;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#initStock}
	 **/
	public static volatile SingularAttribute<SalesLine, Integer> initStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#lots}
	 **/
	public static volatile SingularAttribute<SalesLine, List> lots;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.SalesLine#rates}
	 **/
	public static volatile SingularAttribute<SalesLine, List> rates;

}


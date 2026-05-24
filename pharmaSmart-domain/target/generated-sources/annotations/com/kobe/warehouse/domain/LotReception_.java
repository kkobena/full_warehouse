package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.LotReception}
 **/
@StaticMetamodel(LotReception.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class LotReception_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #lot
	 **/
	public static final String LOT = "lot";
	
	/**
	 * @see #orderLine
	 **/
	public static final String ORDER_LINE = "orderLine";
	
	/**
	 * @see #quantityReceived
	 **/
	public static final String QUANTITY_RECEIVED = "quantityReceived";
	
	/**
	 * @see #freeQty
	 **/
	public static final String FREE_QTY = "freeQty";
	
	/**
	 * @see #prixAchat
	 **/
	public static final String PRIX_ACHAT = "prixAchat";
	
	/**
	 * @see #receiptDate
	 **/
	public static final String RECEIPT_DATE = "receiptDate";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.LotReception}
	 **/
	public static volatile EntityType<LotReception> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotReception#id}
	 **/
	public static volatile SingularAttribute<LotReception, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotReception#lot}
	 **/
	public static volatile SingularAttribute<LotReception, Lot> lot;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotReception#orderLine}
	 **/
	public static volatile SingularAttribute<LotReception, OrderLine> orderLine;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotReception#quantityReceived}
	 **/
	public static volatile SingularAttribute<LotReception, Integer> quantityReceived;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotReception#freeQty}
	 **/
	public static volatile SingularAttribute<LotReception, Integer> freeQty;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotReception#prixAchat}
	 **/
	public static volatile SingularAttribute<LotReception, Integer> prixAchat;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotReception#receiptDate}
	 **/
	public static volatile SingularAttribute<LotReception, LocalDate> receiptDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotReception#createdAt}
	 **/
	public static volatile SingularAttribute<LotReception, LocalDateTime> createdAt;

}


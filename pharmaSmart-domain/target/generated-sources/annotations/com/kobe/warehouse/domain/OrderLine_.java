package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.OrderLine}
 **/
@StaticMetamodel(OrderLine.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class OrderLine_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #quantityReceived
	 **/
	public static final String QUANTITY_RECEIVED = "quantityReceived";
	
	/**
	 * @see #orderDate
	 **/
	public static final String ORDER_DATE = "orderDate";
	
	/**
	 * @see #initStock
	 **/
	public static final String INIT_STOCK = "initStock";
	
	/**
	 * @see #finalStock
	 **/
	public static final String FINAL_STOCK = "finalStock";
	
	/**
	 * @see #quantityRequested
	 **/
	public static final String QUANTITY_REQUESTED = "quantityRequested";
	
	/**
	 * @see #quantityReturned
	 **/
	public static final String QUANTITY_RETURNED = "quantityReturned";
	
	/**
	 * @see #discountAmount
	 **/
	public static final String DISCOUNT_AMOUNT = "discountAmount";
	
	/**
	 * @see #orderAmount
	 **/
	public static final String ORDER_AMOUNT = "orderAmount";
	
	/**
	 * @see #grossAmount
	 **/
	public static final String GROSS_AMOUNT = "grossAmount";
	
	/**
	 * @see #netAmount
	 **/
	public static final String NET_AMOUNT = "netAmount";
	
	/**
	 * @see #taxAmount
	 **/
	public static final String TAX_AMOUNT = "taxAmount";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #receiptDate
	 **/
	public static final String RECEIPT_DATE = "receiptDate";
	
	/**
	 * @see #commande
	 **/
	public static final String COMMANDE = "commande";
	
	/**
	 * @see #orderUnitPrice
	 **/
	public static final String ORDER_UNIT_PRICE = "orderUnitPrice";
	
	/**
	 * @see #orderCostAmount
	 **/
	public static final String ORDER_COST_AMOUNT = "orderCostAmount";
	
	/**
	 * @see #freeQty
	 **/
	public static final String FREE_QTY = "freeQty";
	
	/**
	 * @see #fournisseurProduit
	 **/
	public static final String FOURNISSEUR_PRODUIT = "fournisseurProduit";
	
	/**
	 * @see #provisionalCode
	 **/
	public static final String PROVISIONAL_CODE = "provisionalCode";
	
	/**
	 * @see #lots
	 **/
	public static final String LOTS = "lots";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #tva
	 **/
	public static final String TVA = "tva";
	
	/**
	 * @see #datePeremption
	 **/
	public static final String DATE_PEREMPTION = "datePeremption";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.OrderLine}
	 **/
	public static volatile EntityType<OrderLine> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#id}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#quantityReceived}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> quantityReceived;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#orderDate}
	 **/
	public static volatile SingularAttribute<OrderLine, LocalDate> orderDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#initStock}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> initStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#finalStock}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> finalStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#quantityRequested}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> quantityRequested;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#quantityReturned}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> quantityReturned;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#discountAmount}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> discountAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#orderAmount}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> orderAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#grossAmount}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> grossAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#netAmount}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> netAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#taxAmount}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> taxAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#createdAt}
	 **/
	public static volatile SingularAttribute<OrderLine, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#updatedAt}
	 **/
	public static volatile SingularAttribute<OrderLine, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#receiptDate}
	 **/
	public static volatile SingularAttribute<OrderLine, LocalDateTime> receiptDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#commande}
	 **/
	public static volatile SingularAttribute<OrderLine, Commande> commande;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#orderUnitPrice}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> orderUnitPrice;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#orderCostAmount}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> orderCostAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#freeQty}
	 **/
	public static volatile SingularAttribute<OrderLine, Integer> freeQty;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#fournisseurProduit}
	 **/
	public static volatile SingularAttribute<OrderLine, FournisseurProduit> fournisseurProduit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#provisionalCode}
	 **/
	public static volatile SingularAttribute<OrderLine, Boolean> provisionalCode;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#lots}
	 **/
	public static volatile ListAttribute<OrderLine, Lot> lots;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#updated}
	 **/
	public static volatile SingularAttribute<OrderLine, Boolean> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#tva}
	 **/
	public static volatile SingularAttribute<OrderLine, Tva> tva;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OrderLine#datePeremption}
	 **/
	public static volatile SingularAttribute<OrderLine, LocalDate> datePeremption;

}


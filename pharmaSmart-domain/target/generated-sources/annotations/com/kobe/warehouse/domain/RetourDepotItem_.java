package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.RetourDepotItem}
 **/
@StaticMetamodel(RetourDepotItem.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class RetourDepotItem_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #retourDepot
	 **/
	public static final String RETOUR_DEPOT = "retourDepot";
	
	/**
	 * @see #qtyMvt
	 **/
	public static final String QTY_MVT = "qtyMvt";
	
	/**
	 * @see #initStock
	 **/
	public static final String INIT_STOCK = "initStock";
	
	/**
	 * @see #afterStock
	 **/
	public static final String AFTER_STOCK = "afterStock";
	
	/**
	 * @see #regularUnitPrice
	 **/
	public static final String REGULAR_UNIT_PRICE = "regularUnitPrice";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.RetourDepotItem}
	 **/
	public static volatile EntityType<RetourDepotItem> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepotItem#id}
	 **/
	public static volatile SingularAttribute<RetourDepotItem, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepotItem#retourDepot}
	 **/
	public static volatile SingularAttribute<RetourDepotItem, RetourDepot> retourDepot;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepotItem#qtyMvt}
	 **/
	public static volatile SingularAttribute<RetourDepotItem, Integer> qtyMvt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepotItem#initStock}
	 **/
	public static volatile SingularAttribute<RetourDepotItem, Integer> initStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepotItem#afterStock}
	 **/
	public static volatile SingularAttribute<RetourDepotItem, Integer> afterStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepotItem#regularUnitPrice}
	 **/
	public static volatile SingularAttribute<RetourDepotItem, Integer> regularUnitPrice;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourDepotItem#produit}
	 **/
	public static volatile SingularAttribute<RetourDepotItem, Produit> produit;

}


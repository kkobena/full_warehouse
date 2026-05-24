package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.StockProduit}
 **/
@StaticMetamodel(StockProduit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class StockProduit_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #qtyStock
	 **/
	public static final String QTY_STOCK = "qtyStock";
	
	/**
	 * @see #qtyVirtual
	 **/
	public static final String QTY_VIRTUAL = "qtyVirtual";
	
	/**
	 * @see #qtyUG
	 **/
	public static final String QTY_UG = "qtyUG";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #storage
	 **/
	public static final String STORAGE = "storage";
	
	/**
	 * @see #stockReassort
	 **/
	public static final String STOCK_REASSORT = "stockReassort";
	
	/**
	 * @see #seuilMini
	 **/
	public static final String SEUIL_MINI = "seuilMini";
	
	/**
	 * @see #totalStockQuantity
	 **/
	public static final String TOTAL_STOCK_QUANTITY = "totalStockQuantity";
	
	/**
	 * @see #version
	 **/
	public static final String VERSION = "version";
	
	/**
	 * @see #lastModifiedBy
	 **/
	public static final String LAST_MODIFIED_BY = "lastModifiedBy";
	
	/**
	 * @see #stockMaxi
	 **/
	public static final String STOCK_MAXI = "stockMaxi";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.StockProduit}
	 **/
	public static volatile EntityType<StockProduit> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#id}
	 **/
	public static volatile SingularAttribute<StockProduit, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#qtyStock}
	 **/
	public static volatile SingularAttribute<StockProduit, Integer> qtyStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#qtyVirtual}
	 **/
	public static volatile SingularAttribute<StockProduit, Integer> qtyVirtual;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#qtyUG}
	 **/
	public static volatile SingularAttribute<StockProduit, Integer> qtyUG;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#createdAt}
	 **/
	public static volatile SingularAttribute<StockProduit, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#updatedAt}
	 **/
	public static volatile SingularAttribute<StockProduit, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#produit}
	 **/
	public static volatile SingularAttribute<StockProduit, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#storage}
	 **/
	public static volatile SingularAttribute<StockProduit, Storage> storage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#stockReassort}
	 **/
	public static volatile SingularAttribute<StockProduit, Integer> stockReassort;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#seuilMini}
	 **/
	public static volatile SingularAttribute<StockProduit, Integer> seuilMini;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#totalStockQuantity}
	 **/
	public static volatile SingularAttribute<StockProduit, Integer> totalStockQuantity;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#version}
	 **/
	public static volatile SingularAttribute<StockProduit, Integer> version;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#lastModifiedBy}
	 **/
	public static volatile SingularAttribute<StockProduit, String> lastModifiedBy;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockProduit#stockMaxi}
	 **/
	public static volatile SingularAttribute<StockProduit, Integer> stockMaxi;

}


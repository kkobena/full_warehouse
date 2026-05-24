package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.LigneReassort}
 **/
@StaticMetamodel(LigneReassort.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class LigneReassort_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #quantity
	 **/
	public static final String QUANTITY = "quantity";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #reassort
	 **/
	public static final String REASSORT = "reassort";
	
	/**
	 * @see #stockProduit
	 **/
	public static final String STOCK_PRODUIT = "stockProduit";
	
	/**
	 * @see #stockProduitSrc
	 **/
	public static final String STOCK_PRODUIT_SRC = "stockProduitSrc";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.LigneReassort}
	 **/
	public static volatile EntityType<LigneReassort> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LigneReassort#id}
	 **/
	public static volatile SingularAttribute<LigneReassort, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LigneReassort#quantity}
	 **/
	public static volatile SingularAttribute<LigneReassort, Integer> quantity;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LigneReassort#updatedAt}
	 **/
	public static volatile SingularAttribute<LigneReassort, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LigneReassort#reassort}
	 **/
	public static volatile SingularAttribute<LigneReassort, SuggestionReassort> reassort;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LigneReassort#stockProduit}
	 **/
	public static volatile SingularAttribute<LigneReassort, StockProduit> stockProduit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LigneReassort#stockProduitSrc}
	 **/
	public static volatile SingularAttribute<LigneReassort, StockProduit> stockProduitSrc;

}


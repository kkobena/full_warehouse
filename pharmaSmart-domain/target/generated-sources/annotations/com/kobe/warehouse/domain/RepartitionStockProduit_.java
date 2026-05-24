package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TypeRepartition;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.RepartitionStockProduit}
 **/
@StaticMetamodel(RepartitionStockProduit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class RepartitionStockProduit_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #stockProduitSource
	 **/
	public static final String STOCK_PRODUIT_SOURCE = "stockProduitSource";
	
	/**
	 * @see #stockProduitDestination
	 **/
	public static final String STOCK_PRODUIT_DESTINATION = "stockProduitDestination";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #qtyMvt
	 **/
	public static final String QTY_MVT = "qtyMvt";
	
	/**
	 * @see #sourceInitStock
	 **/
	public static final String SOURCE_INIT_STOCK = "sourceInitStock";
	
	/**
	 * @see #sourceFinalStock
	 **/
	public static final String SOURCE_FINAL_STOCK = "sourceFinalStock";
	
	/**
	 * @see #destInitStock
	 **/
	public static final String DEST_INIT_STOCK = "destInitStock";
	
	/**
	 * @see #destFinalStock
	 **/
	public static final String DEST_FINAL_STOCK = "destFinalStock";
	
	/**
	 * @see #typeRepartition
	 **/
	public static final String TYPE_REPARTITION = "typeRepartition";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.RepartitionStockProduit}
	 **/
	public static volatile EntityType<RepartitionStockProduit> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RepartitionStockProduit#id}
	 **/
	public static volatile SingularAttribute<RepartitionStockProduit, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RepartitionStockProduit#stockProduitSource}
	 **/
	public static volatile SingularAttribute<RepartitionStockProduit, StockProduit> stockProduitSource;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RepartitionStockProduit#stockProduitDestination}
	 **/
	public static volatile SingularAttribute<RepartitionStockProduit, StockProduit> stockProduitDestination;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RepartitionStockProduit#user}
	 **/
	public static volatile SingularAttribute<RepartitionStockProduit, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RepartitionStockProduit#created}
	 **/
	public static volatile SingularAttribute<RepartitionStockProduit, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RepartitionStockProduit#qtyMvt}
	 **/
	public static volatile SingularAttribute<RepartitionStockProduit, Integer> qtyMvt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RepartitionStockProduit#sourceInitStock}
	 **/
	public static volatile SingularAttribute<RepartitionStockProduit, Integer> sourceInitStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RepartitionStockProduit#sourceFinalStock}
	 **/
	public static volatile SingularAttribute<RepartitionStockProduit, Integer> sourceFinalStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RepartitionStockProduit#destInitStock}
	 **/
	public static volatile SingularAttribute<RepartitionStockProduit, Integer> destInitStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RepartitionStockProduit#destFinalStock}
	 **/
	public static volatile SingularAttribute<RepartitionStockProduit, Integer> destFinalStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RepartitionStockProduit#typeRepartition}
	 **/
	public static volatile SingularAttribute<RepartitionStockProduit, TypeRepartition> typeRepartition;

}


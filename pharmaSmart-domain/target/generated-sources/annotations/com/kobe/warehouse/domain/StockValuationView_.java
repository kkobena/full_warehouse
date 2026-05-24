package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.math.BigDecimal;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.StockValuationView}
 **/
@StaticMetamodel(StockValuationView.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class StockValuationView_ {

	
	/**
	 * @see #produitId
	 **/
	public static final String PRODUIT_ID = "produitId";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #codeCip
	 **/
	public static final String CODE_CIP = "codeCip";
	
	/**
	 * @see #categorie
	 **/
	public static final String CATEGORIE = "categorie";
	
	/**
	 * @see #categorieId
	 **/
	public static final String CATEGORIE_ID = "categorieId";
	
	/**
	 * @see #magasinId
	 **/
	public static final String MAGASIN_ID = "magasinId";
	
	/**
	 * @see #purchasePrice
	 **/
	public static final String PURCHASE_PRICE = "purchasePrice";
	
	/**
	 * @see #stockQuantity
	 **/
	public static final String STOCK_QUANTITY = "stockQuantity";
	
	/**
	 * @see #salesPrice
	 **/
	public static final String SALES_PRICE = "salesPrice";
	
	/**
	 * @see #totalPurchaseValue
	 **/
	public static final String TOTAL_PURCHASE_VALUE = "totalPurchaseValue";
	
	/**
	 * @see #totalSalesValue
	 **/
	public static final String TOTAL_SALES_VALUE = "totalSalesValue";
	
	/**
	 * @see #potentialMargin
	 **/
	public static final String POTENTIAL_MARGIN = "potentialMargin";
	
	/**
	 * @see #marginPercentage
	 **/
	public static final String MARGIN_PERCENTAGE = "marginPercentage";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.StockValuationView}
	 **/
	public static volatile EntityType<StockValuationView> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#produitId}
	 **/
	public static volatile SingularAttribute<StockValuationView, Integer> produitId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#libelle}
	 **/
	public static volatile SingularAttribute<StockValuationView, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#codeCip}
	 **/
	public static volatile SingularAttribute<StockValuationView, String> codeCip;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#categorie}
	 **/
	public static volatile SingularAttribute<StockValuationView, String> categorie;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#categorieId}
	 **/
	public static volatile SingularAttribute<StockValuationView, Integer> categorieId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#magasinId}
	 **/
	public static volatile SingularAttribute<StockValuationView, Integer> magasinId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#purchasePrice}
	 **/
	public static volatile SingularAttribute<StockValuationView, Integer> purchasePrice;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#stockQuantity}
	 **/
	public static volatile SingularAttribute<StockValuationView, Integer> stockQuantity;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#salesPrice}
	 **/
	public static volatile SingularAttribute<StockValuationView, Integer> salesPrice;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#totalPurchaseValue}
	 **/
	public static volatile SingularAttribute<StockValuationView, Long> totalPurchaseValue;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#totalSalesValue}
	 **/
	public static volatile SingularAttribute<StockValuationView, Long> totalSalesValue;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#potentialMargin}
	 **/
	public static volatile SingularAttribute<StockValuationView, Long> potentialMargin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StockValuationView#marginPercentage}
	 **/
	public static volatile SingularAttribute<StockValuationView, BigDecimal> marginPercentage;

}


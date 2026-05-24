package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.StockAlertType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.MvStockAlert}
 **/
@StaticMetamodel(MvStockAlert.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class MvStockAlert_ {

	
	/**
	 * @see #produitId
	 **/
	public static final String PRODUIT_ID = "produitId";
	
	/**
	 * @see #stockQuantity
	 **/
	public static final String STOCK_QUANTITY = "stockQuantity";
	
	/**
	 * @see #seuilMin
	 **/
	public static final String SEUIL_MIN = "seuilMin";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #codeCip
	 **/
	public static final String CODE_CIP = "codeCip";
	
	/**
	 * @see #expiryDate
	 **/
	public static final String EXPIRY_DATE = "expiryDate";
	
	/**
	 * @see #alertType
	 **/
	public static final String ALERT_TYPE = "alertType";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.MvStockAlert}
	 **/
	public static volatile EntityType<MvStockAlert> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MvStockAlert#produitId}
	 **/
	public static volatile SingularAttribute<MvStockAlert, Integer> produitId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MvStockAlert#stockQuantity}
	 **/
	public static volatile SingularAttribute<MvStockAlert, Integer> stockQuantity;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MvStockAlert#seuilMin}
	 **/
	public static volatile SingularAttribute<MvStockAlert, Integer> seuilMin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MvStockAlert#libelle}
	 **/
	public static volatile SingularAttribute<MvStockAlert, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MvStockAlert#codeCip}
	 **/
	public static volatile SingularAttribute<MvStockAlert, String> codeCip;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MvStockAlert#expiryDate}
	 **/
	public static volatile SingularAttribute<MvStockAlert, LocalDate> expiryDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MvStockAlert#alertType}
	 **/
	public static volatile SingularAttribute<MvStockAlert, StockAlertType> alertType;

}


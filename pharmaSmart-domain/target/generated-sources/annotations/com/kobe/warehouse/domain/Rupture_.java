package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Rupture}
 **/
@StaticMetamodel(Rupture.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Rupture_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #dateMtv
	 **/
	public static final String DATE_MTV = "dateMtv";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #fournisseur
	 **/
	public static final String FOURNISSEUR = "fournisseur";
	
	/**
	 * @see #qty
	 **/
	public static final String QTY = "qty";
	
	/**
	 * @see #productStillOutOfStock
	 **/
	public static final String PRODUCT_STILL_OUT_OF_STOCK = "productStillOutOfStock";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Rupture}
	 **/
	public static volatile EntityType<Rupture> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rupture#id}
	 **/
	public static volatile SingularAttribute<Rupture, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rupture#dateMtv}
	 **/
	public static volatile SingularAttribute<Rupture, LocalDateTime> dateMtv;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rupture#produit}
	 **/
	public static volatile SingularAttribute<Rupture, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rupture#fournisseur}
	 **/
	public static volatile SingularAttribute<Rupture, Fournisseur> fournisseur;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rupture#qty}
	 **/
	public static volatile SingularAttribute<Rupture, Integer> qty;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rupture#productStillOutOfStock}
	 **/
	public static volatile SingularAttribute<Rupture, Boolean> productStillOutOfStock;

}


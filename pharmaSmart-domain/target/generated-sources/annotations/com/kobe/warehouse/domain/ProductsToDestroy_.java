package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.ProductsToDestroy}
 **/
@StaticMetamodel(ProductsToDestroy.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ProductsToDestroy_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #numLot
	 **/
	public static final String NUM_LOT = "numLot";
	
	/**
	 * @see #prixAchat
	 **/
	public static final String PRIX_ACHAT = "prixAchat";
	
	/**
	 * @see #prixUnit
	 **/
	public static final String PRIX_UNIT = "prixUnit";
	
	/**
	 * @see #fournisseurProduit
	 **/
	public static final String FOURNISSEUR_PRODUIT = "fournisseurProduit";
	
	/**
	 * @see #quantity
	 **/
	public static final String QUANTITY = "quantity";
	
	/**
	 * @see #stockInitial
	 **/
	public static final String STOCK_INITIAL = "stockInitial";
	
	/**
	 * @see #datePeremption
	 **/
	public static final String DATE_PEREMPTION = "datePeremption";
	
	/**
	 * @see #dateDestuction
	 **/
	public static final String DATE_DESTUCTION = "dateDestuction";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #destroyed
	 **/
	public static final String DESTROYED = "destroyed";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #magasin
	 **/
	public static final String MAGASIN = "magasin";
	
	/**
	 * @see #editing
	 **/
	public static final String EDITING = "editing";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.ProductsToDestroy}
	 **/
	public static volatile EntityType<ProductsToDestroy> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#id}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#numLot}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, String> numLot;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#prixAchat}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, Integer> prixAchat;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#prixUnit}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, Integer> prixUnit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#fournisseurProduit}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, FournisseurProduit> fournisseurProduit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#quantity}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, Integer> quantity;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#stockInitial}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, Integer> stockInitial;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#datePeremption}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, LocalDate> datePeremption;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#dateDestuction}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, LocalDate> dateDestuction;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#user}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#destroyed}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, Boolean> destroyed;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#created}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#updated}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, LocalDateTime> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#magasin}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, Magasin> magasin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProductsToDestroy#editing}
	 **/
	public static volatile SingularAttribute<ProductsToDestroy, Boolean> editing;

}


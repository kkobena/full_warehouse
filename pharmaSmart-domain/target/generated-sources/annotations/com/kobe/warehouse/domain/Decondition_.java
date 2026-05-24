package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TypeDeconditionnement;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Decondition}
 **/
@StaticMetamodel(Decondition.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Decondition_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #qtyMvt
	 **/
	public static final String QTY_MVT = "qtyMvt";
	
	/**
	 * @see #dateMtv
	 **/
	public static final String DATE_MTV = "dateMtv";
	
	/**
	 * @see #stockBefore
	 **/
	public static final String STOCK_BEFORE = "stockBefore";
	
	/**
	 * @see #stockAfter
	 **/
	public static final String STOCK_AFTER = "stockAfter";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #typeDeconditionnement
	 **/
	public static final String TYPE_DECONDITIONNEMENT = "typeDeconditionnement";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Decondition}
	 **/
	public static volatile EntityType<Decondition> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Decondition#id}
	 **/
	public static volatile SingularAttribute<Decondition, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Decondition#qtyMvt}
	 **/
	public static volatile SingularAttribute<Decondition, Integer> qtyMvt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Decondition#dateMtv}
	 **/
	public static volatile SingularAttribute<Decondition, LocalDateTime> dateMtv;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Decondition#stockBefore}
	 **/
	public static volatile SingularAttribute<Decondition, Integer> stockBefore;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Decondition#stockAfter}
	 **/
	public static volatile SingularAttribute<Decondition, Integer> stockAfter;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Decondition#user}
	 **/
	public static volatile SingularAttribute<Decondition, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Decondition#produit}
	 **/
	public static volatile SingularAttribute<Decondition, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Decondition#typeDeconditionnement}
	 **/
	public static volatile SingularAttribute<Decondition, TypeDeconditionnement> typeDeconditionnement;

}


package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.OptionPrixType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.OptionPrixProduit}
 **/
@StaticMetamodel(OptionPrixProduit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class OptionPrixProduit_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #price
	 **/
	public static final String PRICE = "price";
	
	/**
	 * @see #tiersPayant
	 **/
	public static final String TIERS_PAYANT = "tiersPayant";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #enabled
	 **/
	public static final String ENABLED = "enabled";
	
	/**
	 * @see #rate
	 **/
	public static final String RATE = "rate";
	
	/**
	 * @see #type
	 **/
	public static final String TYPE = "type";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.OptionPrixProduit}
	 **/
	public static volatile EntityType<OptionPrixProduit> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OptionPrixProduit#id}
	 **/
	public static volatile SingularAttribute<OptionPrixProduit, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OptionPrixProduit#price}
	 **/
	public static volatile SingularAttribute<OptionPrixProduit, Integer> price;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OptionPrixProduit#tiersPayant}
	 **/
	public static volatile SingularAttribute<OptionPrixProduit, TiersPayant> tiersPayant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OptionPrixProduit#produit}
	 **/
	public static volatile SingularAttribute<OptionPrixProduit, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OptionPrixProduit#enabled}
	 **/
	public static volatile SingularAttribute<OptionPrixProduit, Boolean> enabled;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OptionPrixProduit#rate}
	 **/
	public static volatile SingularAttribute<OptionPrixProduit, Float> rate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OptionPrixProduit#type}
	 **/
	public static volatile SingularAttribute<OptionPrixProduit, OptionPrixType> type;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OptionPrixProduit#created}
	 **/
	public static volatile SingularAttribute<OptionPrixProduit, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OptionPrixProduit#updated}
	 **/
	public static volatile SingularAttribute<OptionPrixProduit, LocalDateTime> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.OptionPrixProduit#user}
	 **/
	public static volatile SingularAttribute<OptionPrixProduit, AppUser> user;

}


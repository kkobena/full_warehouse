package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.ProduitPerime}
 **/
@StaticMetamodel(ProduitPerime.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ProduitPerime_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #lot
	 **/
	public static final String LOT = "lot";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #quantity
	 **/
	public static final String QUANTITY = "quantity";
	
	/**
	 * @see #peremptionDate
	 **/
	public static final String PEREMPTION_DATE = "peremptionDate";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #initStock
	 **/
	public static final String INIT_STOCK = "initStock";
	
	/**
	 * @see #afterStock
	 **/
	public static final String AFTER_STOCK = "afterStock";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.ProduitPerime}
	 **/
	public static volatile EntityType<ProduitPerime> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProduitPerime#id}
	 **/
	public static volatile SingularAttribute<ProduitPerime, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProduitPerime#produit}
	 **/
	public static volatile SingularAttribute<ProduitPerime, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProduitPerime#lot}
	 **/
	public static volatile SingularAttribute<ProduitPerime, Lot> lot;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProduitPerime#created}
	 **/
	public static volatile SingularAttribute<ProduitPerime, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProduitPerime#quantity}
	 **/
	public static volatile SingularAttribute<ProduitPerime, Integer> quantity;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProduitPerime#peremptionDate}
	 **/
	public static volatile SingularAttribute<ProduitPerime, LocalDate> peremptionDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProduitPerime#user}
	 **/
	public static volatile SingularAttribute<ProduitPerime, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProduitPerime#initStock}
	 **/
	public static volatile SingularAttribute<ProduitPerime, Integer> initStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ProduitPerime#afterStock}
	 **/
	public static volatile SingularAttribute<ProduitPerime, Integer> afterStock;

}


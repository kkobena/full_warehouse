package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.RayonProduit}
 **/
@StaticMetamodel(RayonProduit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class RayonProduit_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #rayon
	 **/
	public static final String RAYON = "rayon";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.RayonProduit}
	 **/
	public static volatile EntityType<RayonProduit> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RayonProduit#id}
	 **/
	public static volatile SingularAttribute<RayonProduit, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RayonProduit#produit}
	 **/
	public static volatile SingularAttribute<RayonProduit, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RayonProduit#rayon}
	 **/
	public static volatile SingularAttribute<RayonProduit, Rayon> rayon;

}


package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Laboratoire}
 **/
@StaticMetamodel(Laboratoire.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Laboratoire_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #produits
	 **/
	public static final String PRODUITS = "produits";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Laboratoire}
	 **/
	public static volatile EntityType<Laboratoire> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Laboratoire#id}
	 **/
	public static volatile SingularAttribute<Laboratoire, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Laboratoire#libelle}
	 **/
	public static volatile SingularAttribute<Laboratoire, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Laboratoire#produits}
	 **/
	public static volatile SetAttribute<Laboratoire, Produit> produits;

}


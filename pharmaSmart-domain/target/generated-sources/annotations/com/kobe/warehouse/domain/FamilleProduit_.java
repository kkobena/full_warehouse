package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.FamilleProduit}
 **/
@StaticMetamodel(FamilleProduit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class FamilleProduit_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #code
	 **/
	public static final String CODE = "code";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #produits
	 **/
	public static final String PRODUITS = "produits";
	
	/**
	 * @see #categorie
	 **/
	public static final String CATEGORIE = "categorie";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.FamilleProduit}
	 **/
	public static volatile EntityType<FamilleProduit> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FamilleProduit#id}
	 **/
	public static volatile SingularAttribute<FamilleProduit, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FamilleProduit#code}
	 **/
	public static volatile SingularAttribute<FamilleProduit, String> code;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FamilleProduit#libelle}
	 **/
	public static volatile SingularAttribute<FamilleProduit, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FamilleProduit#produits}
	 **/
	public static volatile SetAttribute<FamilleProduit, Produit> produits;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FamilleProduit#categorie}
	 **/
	public static volatile SingularAttribute<FamilleProduit, Categorie> categorie;

}


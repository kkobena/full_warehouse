package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.FormProduit}
 **/
@StaticMetamodel(FormProduit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class FormProduit_ {

	
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
	 * Static metamodel type for {@link com.kobe.warehouse.domain.FormProduit}
	 **/
	public static volatile EntityType<FormProduit> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FormProduit#id}
	 **/
	public static volatile SingularAttribute<FormProduit, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FormProduit#libelle}
	 **/
	public static volatile SingularAttribute<FormProduit, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FormProduit#produits}
	 **/
	public static volatile SetAttribute<FormProduit, Produit> produits;

}


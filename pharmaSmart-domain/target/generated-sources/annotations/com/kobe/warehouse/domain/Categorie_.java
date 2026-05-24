package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Categorie}
 **/
@StaticMetamodel(Categorie.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Categorie_ {

	
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
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Categorie}
	 **/
	public static volatile EntityType<Categorie> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Categorie#id}
	 **/
	public static volatile SingularAttribute<Categorie, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Categorie#code}
	 **/
	public static volatile SingularAttribute<Categorie, String> code;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Categorie#libelle}
	 **/
	public static volatile SingularAttribute<Categorie, String> libelle;

}


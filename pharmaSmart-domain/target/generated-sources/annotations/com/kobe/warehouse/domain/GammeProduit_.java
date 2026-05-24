package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.GammeProduit}
 **/
@StaticMetamodel(GammeProduit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class GammeProduit_ {

	
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
	 * Static metamodel type for {@link com.kobe.warehouse.domain.GammeProduit}
	 **/
	public static volatile EntityType<GammeProduit> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GammeProduit#id}
	 **/
	public static volatile SingularAttribute<GammeProduit, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GammeProduit#code}
	 **/
	public static volatile SingularAttribute<GammeProduit, String> code;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GammeProduit#libelle}
	 **/
	public static volatile SingularAttribute<GammeProduit, String> libelle;

}


package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Authority}
 **/
@StaticMetamodel(Authority.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Authority_ {

	
	/**
	 * @see #name
	 **/
	public static final String NAME = "name";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Authority}
	 **/
	public static volatile EntityType<Authority> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Authority#name}
	 **/
	public static volatile SingularAttribute<Authority, String> name;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Authority#libelle}
	 **/
	public static volatile SingularAttribute<Authority, String> libelle;

}


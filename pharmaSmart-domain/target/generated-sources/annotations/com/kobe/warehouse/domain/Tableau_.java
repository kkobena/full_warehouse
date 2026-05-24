package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Tableau}
 **/
@StaticMetamodel(Tableau.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Tableau_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #code
	 **/
	public static final String CODE = "code";
	
	/**
	 * @see #value
	 **/
	public static final String VALUE = "value";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Tableau}
	 **/
	public static volatile EntityType<Tableau> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Tableau#id}
	 **/
	public static volatile SingularAttribute<Tableau, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Tableau#code}
	 **/
	public static volatile SingularAttribute<Tableau, String> code;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Tableau#value}
	 **/
	public static volatile SingularAttribute<Tableau, Integer> value;

}


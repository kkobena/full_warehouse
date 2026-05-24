package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Dci}
 **/
@StaticMetamodel(Dci.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Dci_ {

	
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
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Dci}
	 **/
	public static volatile EntityType<Dci> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Dci#id}
	 **/
	public static volatile SingularAttribute<Dci, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Dci#code}
	 **/
	public static volatile SingularAttribute<Dci, String> code;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Dci#libelle}
	 **/
	public static volatile SingularAttribute<Dci, String> libelle;

}


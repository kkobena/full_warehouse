package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Tva}
 **/
@StaticMetamodel(Tva.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Tva_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #taux
	 **/
	public static final String TAUX = "taux";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Tva}
	 **/
	public static volatile EntityType<Tva> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Tva#id}
	 **/
	public static volatile SingularAttribute<Tva, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Tva#taux}
	 **/
	public static volatile SingularAttribute<Tva, Integer> taux;

}


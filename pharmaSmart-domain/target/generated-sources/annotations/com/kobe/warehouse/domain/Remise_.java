package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Remise}
 **/
@StaticMetamodel(Remise.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Remise_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #valeur
	 **/
	public static final String VALEUR = "valeur";
	
	/**
	 * @see #enable
	 **/
	public static final String ENABLE = "enable";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Remise}
	 **/
	public static volatile EntityType<Remise> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Remise#id}
	 **/
	public static volatile SingularAttribute<Remise, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Remise#valeur}
	 **/
	public static volatile SingularAttribute<Remise, String> valeur;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Remise#enable}
	 **/
	public static volatile SingularAttribute<Remise, Boolean> enable;

}


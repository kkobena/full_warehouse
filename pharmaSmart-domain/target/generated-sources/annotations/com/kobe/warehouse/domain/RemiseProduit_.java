package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.RemiseProduit}
 **/
@StaticMetamodel(RemiseProduit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class RemiseProduit_ extends Remise_ {

	
	/**
	 * @see #grilles
	 **/
	public static final String GRILLES = "grilles";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.RemiseProduit}
	 **/
	public static volatile EntityType<RemiseProduit> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RemiseProduit#grilles}
	 **/
	public static volatile ListAttribute<RemiseProduit, GrilleRemise> grilles;

}


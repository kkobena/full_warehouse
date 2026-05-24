package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.VenteDepot}
 **/
@StaticMetamodel(VenteDepot.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class VenteDepot_ extends Sales_ {

	
	/**
	 * @see #depot
	 **/
	public static final String DEPOT = "depot";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.VenteDepot}
	 **/
	public static volatile EntityType<VenteDepot> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.VenteDepot#depot}
	 **/
	public static volatile SingularAttribute<VenteDepot, Magasin> depot;

}


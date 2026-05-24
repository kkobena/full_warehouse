package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.RemiseClient}
 **/
@StaticMetamodel(RemiseClient.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class RemiseClient_ extends Remise_ {

	
	/**
	 * @see #remiseValue
	 **/
	public static final String REMISE_VALUE = "remiseValue";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.RemiseClient}
	 **/
	public static volatile EntityType<RemiseClient> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RemiseClient#remiseValue}
	 **/
	public static volatile SingularAttribute<RemiseClient, Float> remiseValue;

}


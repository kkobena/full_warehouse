package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.WarehouseSequence}
 **/
@StaticMetamodel(WarehouseSequence.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class WarehouseSequence_ {

	
	/**
	 * @see #name
	 **/
	public static final String NAME = "name";
	
	/**
	 * @see #value
	 **/
	public static final String VALUE = "value";
	
	/**
	 * @see #increment
	 **/
	public static final String INCREMENT = "increment";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.WarehouseSequence}
	 **/
	public static volatile EntityType<WarehouseSequence> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.WarehouseSequence#name}
	 **/
	public static volatile SingularAttribute<WarehouseSequence, String> name;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.WarehouseSequence#value}
	 **/
	public static volatile SingularAttribute<WarehouseSequence, Integer> value;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.WarehouseSequence#increment}
	 **/
	public static volatile SingularAttribute<WarehouseSequence, Short> increment;

}


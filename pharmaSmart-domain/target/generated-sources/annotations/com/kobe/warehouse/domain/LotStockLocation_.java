package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.LotStockLocation}
 **/
@StaticMetamodel(LotStockLocation.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class LotStockLocation_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #lot
	 **/
	public static final String LOT = "lot";
	
	/**
	 * @see #storage
	 **/
	public static final String STORAGE = "storage";
	
	/**
	 * @see #qty
	 **/
	public static final String QTY = "qty";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.LotStockLocation}
	 **/
	public static volatile EntityType<LotStockLocation> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotStockLocation#id}
	 **/
	public static volatile SingularAttribute<LotStockLocation, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotStockLocation#lot}
	 **/
	public static volatile SingularAttribute<LotStockLocation, Lot> lot;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotStockLocation#storage}
	 **/
	public static volatile SingularAttribute<LotStockLocation, Storage> storage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotStockLocation#qty}
	 **/
	public static volatile SingularAttribute<LotStockLocation, Integer> qty;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.LotStockLocation#updatedAt}
	 **/
	public static volatile SingularAttribute<LotStockLocation, LocalDateTime> updatedAt;

}


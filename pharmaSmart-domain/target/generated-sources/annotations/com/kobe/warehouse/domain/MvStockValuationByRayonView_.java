package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.MvStockValuationByRayonView}
 **/
@StaticMetamodel(MvStockValuationByRayonView.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class MvStockValuationByRayonView_ extends StockValuationView_ {

	
	/**
	 * @see #rayon
	 **/
	public static final String RAYON = "rayon";
	
	/**
	 * @see #rayonId
	 **/
	public static final String RAYON_ID = "rayonId";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.MvStockValuationByRayonView}
	 **/
	public static volatile EntityType<MvStockValuationByRayonView> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MvStockValuationByRayonView#rayon}
	 **/
	public static volatile SingularAttribute<MvStockValuationByRayonView, String> rayon;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MvStockValuationByRayonView#rayonId}
	 **/
	public static volatile SingularAttribute<MvStockValuationByRayonView, Integer> rayonId;

}


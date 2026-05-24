package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.ThirdPartySales}
 **/
@StaticMetamodel(ThirdPartySales.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class ThirdPartySales_ extends Sales_ {

	
	/**
	 * @see #numBon
	 **/
	public static final String NUM_BON = "numBon";
	
	/**
	 * @see #ayantDroit
	 **/
	public static final String AYANT_DROIT = "ayantDroit";
	
	/**
	 * @see #partAssure
	 **/
	public static final String PART_ASSURE = "partAssure";
	
	/**
	 * @see #partTiersPayant
	 **/
	public static final String PART_TIERS_PAYANT = "partTiersPayant";
	
	/**
	 * @see #hasPriceOption
	 **/
	public static final String HAS_PRICE_OPTION = "hasPriceOption";
	
	/**
	 * @see #thirdPartySaleLines
	 **/
	public static final String THIRD_PARTY_SALE_LINES = "thirdPartySaleLines";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.ThirdPartySales}
	 **/
	public static volatile EntityType<ThirdPartySales> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySales#numBon}
	 **/
	public static volatile SingularAttribute<ThirdPartySales, String> numBon;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySales#ayantDroit}
	 **/
	public static volatile SingularAttribute<ThirdPartySales, AssuredCustomer> ayantDroit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySales#partAssure}
	 **/
	public static volatile SingularAttribute<ThirdPartySales, Integer> partAssure;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySales#partTiersPayant}
	 **/
	public static volatile SingularAttribute<ThirdPartySales, Integer> partTiersPayant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySales#hasPriceOption}
	 **/
	public static volatile SingularAttribute<ThirdPartySales, Boolean> hasPriceOption;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.ThirdPartySales#thirdPartySaleLines}
	 **/
	public static volatile ListAttribute<ThirdPartySales, ThirdPartySaleLine> thirdPartySaleLines;

}


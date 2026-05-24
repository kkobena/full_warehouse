package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CodeGrilleRemise;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.GrilleRemise}
 **/
@StaticMetamodel(GrilleRemise.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class GrilleRemise_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #enable
	 **/
	public static final String ENABLE = "enable";
	
	/**
	 * @see #remiseValue
	 **/
	public static final String REMISE_VALUE = "remiseValue";
	
	/**
	 * @see #remiseProduit
	 **/
	public static final String REMISE_PRODUIT = "remiseProduit";
	
	/**
	 * @see #code
	 **/
	public static final String CODE = "code";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.GrilleRemise}
	 **/
	public static volatile EntityType<GrilleRemise> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GrilleRemise#id}
	 **/
	public static volatile SingularAttribute<GrilleRemise, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GrilleRemise#enable}
	 **/
	public static volatile SingularAttribute<GrilleRemise, Boolean> enable;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GrilleRemise#remiseValue}
	 **/
	public static volatile SingularAttribute<GrilleRemise, Float> remiseValue;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GrilleRemise#remiseProduit}
	 **/
	public static volatile SingularAttribute<GrilleRemise, RemiseProduit> remiseProduit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.GrilleRemise#code}
	 **/
	public static volatile SingularAttribute<GrilleRemise, CodeGrilleRemise> code;

}


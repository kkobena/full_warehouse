package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TypeSubstitut;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Substitut}
 **/
@StaticMetamodel(Substitut.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Substitut_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #substitut
	 **/
	public static final String SUBSTITUT = "substitut";
	
	/**
	 * @see #type
	 **/
	public static final String TYPE = "type";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Substitut}
	 **/
	public static volatile EntityType<Substitut> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Substitut#id}
	 **/
	public static volatile SingularAttribute<Substitut, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Substitut#produit}
	 **/
	public static volatile SingularAttribute<Substitut, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Substitut#substitut}
	 **/
	public static volatile SingularAttribute<Substitut, Produit> substitut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Substitut#type}
	 **/
	public static volatile SingularAttribute<Substitut, TypeSubstitut> type;

}


package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.MotifRetourProduit}
 **/
@StaticMetamodel(MotifRetourProduit.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class MotifRetourProduit_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.MotifRetourProduit}
	 **/
	public static volatile EntityType<MotifRetourProduit> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MotifRetourProduit#id}
	 **/
	public static volatile SingularAttribute<MotifRetourProduit, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MotifRetourProduit#libelle}
	 **/
	public static volatile SingularAttribute<MotifRetourProduit, String> libelle;

}


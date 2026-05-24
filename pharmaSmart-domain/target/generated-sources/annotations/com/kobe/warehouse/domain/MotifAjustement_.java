package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.MotifAjustement}
 **/
@StaticMetamodel(MotifAjustement.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class MotifAjustement_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.MotifAjustement}
	 **/
	public static volatile EntityType<MotifAjustement> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MotifAjustement#id}
	 **/
	public static volatile SingularAttribute<MotifAjustement, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.MotifAjustement#libelle}
	 **/
	public static volatile SingularAttribute<MotifAjustement, String> libelle;

}


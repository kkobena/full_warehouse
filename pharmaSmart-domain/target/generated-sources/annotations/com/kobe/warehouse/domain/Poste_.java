package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Poste}
 **/
@StaticMetamodel(Poste.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Poste_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #name
	 **/
	public static final String NAME = "name";
	
	/**
	 * @see #posteNumber
	 **/
	public static final String POSTE_NUMBER = "posteNumber";
	
	/**
	 * @see #address
	 **/
	public static final String ADDRESS = "address";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Poste}
	 **/
	public static volatile EntityType<Poste> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Poste#id}
	 **/
	public static volatile SingularAttribute<Poste, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Poste#name}
	 **/
	public static volatile SingularAttribute<Poste, String> name;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Poste#posteNumber}
	 **/
	public static volatile SingularAttribute<Poste, String> posteNumber;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Poste#address}
	 **/
	public static volatile SingularAttribute<Poste, String> address;

}


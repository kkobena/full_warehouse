package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TypeZone;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Rayon}
 **/
@StaticMetamodel(Rayon.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Rayon_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #code
	 **/
	public static final String CODE = "code";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #storage
	 **/
	public static final String STORAGE = "storage";
	
	/**
	 * @see #exclude
	 **/
	public static final String EXCLUDE = "exclude";
	
	/**
	 * @see #typeZone
	 **/
	public static final String TYPE_ZONE = "typeZone";
	
	/**
	 * @see #position
	 **/
	public static final String POSITION = "position";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Rayon}
	 **/
	public static volatile EntityType<Rayon> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rayon#id}
	 **/
	public static volatile SingularAttribute<Rayon, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rayon#code}
	 **/
	public static volatile SingularAttribute<Rayon, String> code;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rayon#libelle}
	 **/
	public static volatile SingularAttribute<Rayon, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rayon#storage}
	 **/
	public static volatile SingularAttribute<Rayon, Storage> storage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rayon#exclude}
	 **/
	public static volatile SingularAttribute<Rayon, Boolean> exclude;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rayon#typeZone}
	 **/
	public static volatile SingularAttribute<Rayon, TypeZone> typeZone;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Rayon#position}
	 **/
	public static volatile SingularAttribute<Rayon, String> position;

}


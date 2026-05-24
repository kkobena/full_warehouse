package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.StorageType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Storage}
 **/
@StaticMetamodel(Storage.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Storage_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #storageType
	 **/
	public static final String STORAGE_TYPE = "storageType";
	
	/**
	 * @see #name
	 **/
	public static final String NAME = "name";
	
	/**
	 * @see #magasin
	 **/
	public static final String MAGASIN = "magasin";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Storage}
	 **/
	public static volatile EntityType<Storage> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Storage#id}
	 **/
	public static volatile SingularAttribute<Storage, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Storage#storageType}
	 **/
	public static volatile SingularAttribute<Storage, StorageType> storageType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Storage#name}
	 **/
	public static volatile SingularAttribute<Storage, String> name;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Storage#magasin}
	 **/
	public static volatile SingularAttribute<Storage, Magasin> magasin;

}


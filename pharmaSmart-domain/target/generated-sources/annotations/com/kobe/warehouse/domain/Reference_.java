package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Reference}
 **/
@StaticMetamodel(Reference.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Reference_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #num
	 **/
	public static final String NUM = "num";
	
	/**
	 * @see #numberTransac
	 **/
	public static final String NUMBER_TRANSAC = "numberTransac";
	
	/**
	 * @see #mvtDate
	 **/
	public static final String MVT_DATE = "mvtDate";
	
	/**
	 * @see #type
	 **/
	public static final String TYPE = "type";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Reference}
	 **/
	public static volatile EntityType<Reference> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Reference#id}
	 **/
	public static volatile SingularAttribute<Reference, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Reference#num}
	 **/
	public static volatile SingularAttribute<Reference, String> num;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Reference#numberTransac}
	 **/
	public static volatile SingularAttribute<Reference, Integer> numberTransac;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Reference#mvtDate}
	 **/
	public static volatile SingularAttribute<Reference, LocalDate> mvtDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Reference#type}
	 **/
	public static volatile SingularAttribute<Reference, Integer> type;

}


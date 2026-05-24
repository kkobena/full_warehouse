package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ParametreValueType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.AppConfiguration}
 **/
@StaticMetamodel(AppConfiguration.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AppConfiguration_ {

	
	/**
	 * @see #name
	 **/
	public static final String NAME = "name";
	
	/**
	 * @see #value
	 **/
	public static final String VALUE = "value";
	
	/**
	 * @see #description
	 **/
	public static final String DESCRIPTION = "description";
	
	/**
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #validatedBy
	 **/
	public static final String VALIDATED_BY = "validatedBy";
	
	/**
	 * @see #otherValue
	 **/
	public static final String OTHER_VALUE = "otherValue";
	
	/**
	 * @see #valueType
	 **/
	public static final String VALUE_TYPE = "valueType";
	
	/**
	 * @see #options
	 **/
	public static final String OPTIONS = "options";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.AppConfiguration}
	 **/
	public static volatile EntityType<AppConfiguration> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppConfiguration#name}
	 **/
	public static volatile SingularAttribute<AppConfiguration, String> name;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppConfiguration#value}
	 **/
	public static volatile SingularAttribute<AppConfiguration, String> value;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppConfiguration#description}
	 **/
	public static volatile SingularAttribute<AppConfiguration, String> description;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppConfiguration#created}
	 **/
	public static volatile SingularAttribute<AppConfiguration, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppConfiguration#updated}
	 **/
	public static volatile SingularAttribute<AppConfiguration, LocalDateTime> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppConfiguration#validatedBy}
	 **/
	public static volatile SingularAttribute<AppConfiguration, AppUser> validatedBy;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppConfiguration#otherValue}
	 **/
	public static volatile SingularAttribute<AppConfiguration, String> otherValue;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppConfiguration#valueType}
	 **/
	public static volatile SingularAttribute<AppConfiguration, ParametreValueType> valueType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppConfiguration#options}
	 **/
	public static volatile SingularAttribute<AppConfiguration, List> options;

}


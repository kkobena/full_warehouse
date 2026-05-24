package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.DashboardComponentKey;
import com.kobe.warehouse.domain.enumeration.DashboardScope;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.DashboardLayout}
 **/
@StaticMetamodel(DashboardLayout.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class DashboardLayout_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #name
	 **/
	public static final String NAME = "name";
	
	/**
	 * @see #description
	 **/
	public static final String DESCRIPTION = "description";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #authorityAssignments
	 **/
	public static final String AUTHORITY_ASSIGNMENTS = "authorityAssignments";
	
	/**
	 * @see #scope
	 **/
	public static final String SCOPE = "scope";
	
	/**
	 * @see #isDefault
	 **/
	public static final String IS_DEFAULT = "isDefault";
	
	/**
	 * @see #isRoute
	 **/
	public static final String IS_ROUTE = "isRoute";
	
	/**
	 * @see #componentKey
	 **/
	public static final String COMPONENT_KEY = "componentKey";
	
	/**
	 * @see #layoutConfig
	 **/
	public static final String LAYOUT_CONFIG = "layoutConfig";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.DashboardLayout}
	 **/
	public static volatile EntityType<DashboardLayout> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayout#id}
	 **/
	public static volatile SingularAttribute<DashboardLayout, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayout#name}
	 **/
	public static volatile SingularAttribute<DashboardLayout, String> name;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayout#description}
	 **/
	public static volatile SingularAttribute<DashboardLayout, String> description;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayout#user}
	 **/
	public static volatile SingularAttribute<DashboardLayout, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayout#authorityAssignments}
	 **/
	public static volatile SetAttribute<DashboardLayout, DashboardLayoutAuthority> authorityAssignments;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayout#scope}
	 **/
	public static volatile SingularAttribute<DashboardLayout, DashboardScope> scope;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayout#isDefault}
	 **/
	public static volatile SingularAttribute<DashboardLayout, Boolean> isDefault;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayout#isRoute}
	 **/
	public static volatile SingularAttribute<DashboardLayout, Boolean> isRoute;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayout#componentKey}
	 **/
	public static volatile SingularAttribute<DashboardLayout, DashboardComponentKey> componentKey;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayout#layoutConfig}
	 **/
	public static volatile SingularAttribute<DashboardLayout, String> layoutConfig;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayout#createdAt}
	 **/
	public static volatile SingularAttribute<DashboardLayout, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayout#updatedAt}
	 **/
	public static volatile SingularAttribute<DashboardLayout, LocalDateTime> updatedAt;

}


package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.DashboardLayoutAuthority}
 **/
@StaticMetamodel(DashboardLayoutAuthority.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class DashboardLayoutAuthority_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #layout
	 **/
	public static final String LAYOUT = "layout";
	
	/**
	 * @see #authority
	 **/
	public static final String AUTHORITY = "authority";
	
	/**
	 * @see #isDefault
	 **/
	public static final String IS_DEFAULT = "isDefault";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.DashboardLayoutAuthority}
	 **/
	public static volatile EntityType<DashboardLayoutAuthority> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayoutAuthority#id}
	 **/
	public static volatile SingularAttribute<DashboardLayoutAuthority, DashboardLayoutAuthorityId> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayoutAuthority#layout}
	 **/
	public static volatile SingularAttribute<DashboardLayoutAuthority, DashboardLayout> layout;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayoutAuthority#authority}
	 **/
	public static volatile SingularAttribute<DashboardLayoutAuthority, Authority> authority;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayoutAuthority#isDefault}
	 **/
	public static volatile SingularAttribute<DashboardLayoutAuthority, Boolean> isDefault;

}


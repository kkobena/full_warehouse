package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.DashboardLayoutAuthorityId}
 **/
@StaticMetamodel(DashboardLayoutAuthorityId.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class DashboardLayoutAuthorityId_ {

	
	/**
	 * @see #layoutId
	 **/
	public static final String LAYOUT_ID = "layoutId";
	
	/**
	 * @see #authorityName
	 **/
	public static final String AUTHORITY_NAME = "authorityName";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.DashboardLayoutAuthorityId}
	 **/
	public static volatile EmbeddableType<DashboardLayoutAuthorityId> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayoutAuthorityId#layoutId}
	 **/
	public static volatile SingularAttribute<DashboardLayoutAuthorityId, Integer> layoutId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.DashboardLayoutAuthorityId#authorityName}
	 **/
	public static volatile SingularAttribute<DashboardLayoutAuthorityId, String> authorityName;

}


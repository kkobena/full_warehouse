package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.MappedSuperclassType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.AbstractAuditingEntity}
 **/
@StaticMetamodel(AbstractAuditingEntity.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AbstractAuditingEntity_ {

	
	/**
	 * @see #createdBy
	 **/
	public static final String CREATED_BY = "createdBy";
	
	/**
	 * @see #createdDate
	 **/
	public static final String CREATED_DATE = "createdDate";
	
	/**
	 * @see #lastModifiedBy
	 **/
	public static final String LAST_MODIFIED_BY = "lastModifiedBy";
	
	/**
	 * @see #lastModifiedDate
	 **/
	public static final String LAST_MODIFIED_DATE = "lastModifiedDate";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.AbstractAuditingEntity}
	 **/
	public static volatile MappedSuperclassType<AbstractAuditingEntity> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AbstractAuditingEntity#createdBy}
	 **/
	public static volatile SingularAttribute<AbstractAuditingEntity, String> createdBy;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AbstractAuditingEntity#createdDate}
	 **/
	public static volatile SingularAttribute<AbstractAuditingEntity, LocalDateTime> createdDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AbstractAuditingEntity#lastModifiedBy}
	 **/
	public static volatile SingularAttribute<AbstractAuditingEntity, String> lastModifiedBy;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AbstractAuditingEntity#lastModifiedDate}
	 **/
	public static volatile SingularAttribute<AbstractAuditingEntity, LocalDateTime> lastModifiedDate;

}


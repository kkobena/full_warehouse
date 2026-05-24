package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TransactionType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Logs}
 **/
@StaticMetamodel(Logs.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Logs_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #transactionType
	 **/
	public static final String TRANSACTION_TYPE = "transactionType";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #comments
	 **/
	public static final String COMMENTS = "comments";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #indentityKey
	 **/
	public static final String INDENTITY_KEY = "indentityKey";
	
	/**
	 * @see #oldObject
	 **/
	public static final String OLD_OBJECT = "oldObject";
	
	/**
	 * @see #newObject
	 **/
	public static final String NEW_OBJECT = "newObject";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Logs}
	 **/
	public static volatile EntityType<Logs> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Logs#id}
	 **/
	public static volatile SingularAttribute<Logs, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Logs#transactionType}
	 **/
	public static volatile SingularAttribute<Logs, TransactionType> transactionType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Logs#createdAt}
	 **/
	public static volatile SingularAttribute<Logs, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Logs#comments}
	 **/
	public static volatile SingularAttribute<Logs, String> comments;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Logs#user}
	 **/
	public static volatile SingularAttribute<Logs, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Logs#indentityKey}
	 **/
	public static volatile SingularAttribute<Logs, String> indentityKey;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Logs#oldObject}
	 **/
	public static volatile SingularAttribute<Logs, String> oldObject;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Logs#newObject}
	 **/
	public static volatile SingularAttribute<Logs, String> newObject;

}


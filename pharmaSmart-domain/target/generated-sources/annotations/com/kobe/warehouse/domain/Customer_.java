package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Customer}
 **/
@StaticMetamodel(Customer.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Customer_ {

	
	/**
	 * @see #remiseClient
	 **/
	public static final String REMISE_CLIENT = "remiseClient";
	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #firstName
	 **/
	public static final String FIRST_NAME = "firstName";
	
	/**
	 * @see #lastName
	 **/
	public static final String LAST_NAME = "lastName";
	
	/**
	 * @see #phone
	 **/
	public static final String PHONE = "phone";
	
	/**
	 * @see #email
	 **/
	public static final String EMAIL = "email";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #status
	 **/
	public static final String STATUS = "status";
	
	/**
	 * @see #code
	 **/
	public static final String CODE = "code";
	
	/**
	 * @see #typeAssure
	 **/
	public static final String TYPE_ASSURE = "typeAssure";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Customer}
	 **/
	public static volatile EntityType<Customer> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Customer#remiseClient}
	 **/
	public static volatile SingularAttribute<Customer, RemiseClient> remiseClient;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Customer#id}
	 **/
	public static volatile SingularAttribute<Customer, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Customer#firstName}
	 **/
	public static volatile SingularAttribute<Customer, String> firstName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Customer#lastName}
	 **/
	public static volatile SingularAttribute<Customer, String> lastName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Customer#phone}
	 **/
	public static volatile SingularAttribute<Customer, String> phone;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Customer#email}
	 **/
	public static volatile SingularAttribute<Customer, String> email;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Customer#createdAt}
	 **/
	public static volatile SingularAttribute<Customer, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Customer#updatedAt}
	 **/
	public static volatile SingularAttribute<Customer, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Customer#status}
	 **/
	public static volatile SingularAttribute<Customer, Status> status;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Customer#code}
	 **/
	public static volatile SingularAttribute<Customer, String> code;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Customer#typeAssure}
	 **/
	public static volatile SingularAttribute<Customer, TypeAssure> typeAssure;

}


package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SetAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.AppUser}
 **/
@StaticMetamodel(AppUser.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class AppUser_ extends AbstractAuditingEntity_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #login
	 **/
	public static final String LOGIN = "login";
	
	/**
	 * @see #password
	 **/
	public static final String PASSWORD = "password";
	
	/**
	 * @see #firstName
	 **/
	public static final String FIRST_NAME = "firstName";
	
	/**
	 * @see #lastName
	 **/
	public static final String LAST_NAME = "lastName";
	
	/**
	 * @see #email
	 **/
	public static final String EMAIL = "email";
	
	/**
	 * @see #activated
	 **/
	public static final String ACTIVATED = "activated";
	
	/**
	 * @see #langKey
	 **/
	public static final String LANG_KEY = "langKey";
	
	/**
	 * @see #imageUrl
	 **/
	public static final String IMAGE_URL = "imageUrl";
	
	/**
	 * @see #activationKey
	 **/
	public static final String ACTIVATION_KEY = "activationKey";
	
	/**
	 * @see #resetKey
	 **/
	public static final String RESET_KEY = "resetKey";
	
	/**
	 * @see #resetDate
	 **/
	public static final String RESET_DATE = "resetDate";
	
	/**
	 * @see #magasin
	 **/
	public static final String MAGASIN = "magasin";
	
	/**
	 * @see #authorities
	 **/
	public static final String AUTHORITIES = "authorities";
	
	/**
	 * @see #persistentTokens
	 **/
	public static final String PERSISTENT_TOKENS = "persistentTokens";
	
	/**
	 * @see #actionAuthorityKey
	 **/
	public static final String ACTION_AUTHORITY_KEY = "actionAuthorityKey";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.AppUser}
	 **/
	public static volatile EntityType<AppUser> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#id}
	 **/
	public static volatile SingularAttribute<AppUser, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#login}
	 **/
	public static volatile SingularAttribute<AppUser, String> login;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#password}
	 **/
	public static volatile SingularAttribute<AppUser, String> password;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#firstName}
	 **/
	public static volatile SingularAttribute<AppUser, String> firstName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#lastName}
	 **/
	public static volatile SingularAttribute<AppUser, String> lastName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#email}
	 **/
	public static volatile SingularAttribute<AppUser, String> email;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#activated}
	 **/
	public static volatile SingularAttribute<AppUser, Boolean> activated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#langKey}
	 **/
	public static volatile SingularAttribute<AppUser, String> langKey;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#imageUrl}
	 **/
	public static volatile SingularAttribute<AppUser, String> imageUrl;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#activationKey}
	 **/
	public static volatile SingularAttribute<AppUser, String> activationKey;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#resetKey}
	 **/
	public static volatile SingularAttribute<AppUser, String> resetKey;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#resetDate}
	 **/
	public static volatile SingularAttribute<AppUser, LocalDateTime> resetDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#magasin}
	 **/
	public static volatile SingularAttribute<AppUser, Magasin> magasin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#authorities}
	 **/
	public static volatile SetAttribute<AppUser, Authority> authorities;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#persistentTokens}
	 **/
	public static volatile SetAttribute<AppUser, PersistentToken> persistentTokens;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.AppUser#actionAuthorityKey}
	 **/
	public static volatile SingularAttribute<AppUser, String> actionAuthorityKey;

}


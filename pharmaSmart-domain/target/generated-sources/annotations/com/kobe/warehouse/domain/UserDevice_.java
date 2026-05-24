package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.Instant;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.UserDevice}
 **/
@StaticMetamodel(UserDevice.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class UserDevice_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #fcmToken
	 **/
	public static final String FCM_TOKEN = "fcmToken";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #deviceName
	 **/
	public static final String DEVICE_NAME = "deviceName";
	
	/**
	 * @see #deviceModel
	 **/
	public static final String DEVICE_MODEL = "deviceModel";
	
	/**
	 * @see #osVersion
	 **/
	public static final String OS_VERSION = "osVersion";
	
	/**
	 * @see #appVersion
	 **/
	public static final String APP_VERSION = "appVersion";
	
	/**
	 * @see #notificationsEnabled
	 **/
	public static final String NOTIFICATIONS_ENABLED = "notificationsEnabled";
	
	/**
	 * @see #registeredAt
	 **/
	public static final String REGISTERED_AT = "registeredAt";
	
	/**
	 * @see #lastActiveAt
	 **/
	public static final String LAST_ACTIVE_AT = "lastActiveAt";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.UserDevice}
	 **/
	public static volatile EntityType<UserDevice> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UserDevice#id}
	 **/
	public static volatile SingularAttribute<UserDevice, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UserDevice#fcmToken}
	 **/
	public static volatile SingularAttribute<UserDevice, String> fcmToken;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UserDevice#user}
	 **/
	public static volatile SingularAttribute<UserDevice, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UserDevice#deviceName}
	 **/
	public static volatile SingularAttribute<UserDevice, String> deviceName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UserDevice#deviceModel}
	 **/
	public static volatile SingularAttribute<UserDevice, String> deviceModel;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UserDevice#osVersion}
	 **/
	public static volatile SingularAttribute<UserDevice, String> osVersion;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UserDevice#appVersion}
	 **/
	public static volatile SingularAttribute<UserDevice, String> appVersion;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UserDevice#notificationsEnabled}
	 **/
	public static volatile SingularAttribute<UserDevice, Boolean> notificationsEnabled;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UserDevice#registeredAt}
	 **/
	public static volatile SingularAttribute<UserDevice, Instant> registeredAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.UserDevice#lastActiveAt}
	 **/
	public static volatile SingularAttribute<UserDevice, Instant> lastActiveAt;

}


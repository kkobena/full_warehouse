package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.DeviceType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.PosteDevice}
 **/
@StaticMetamodel(PosteDevice.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class PosteDevice_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #poste
	 **/
	public static final String POSTE = "poste";
	
	/**
	 * @see #deviceType
	 **/
	public static final String DEVICE_TYPE = "deviceType";
	
	/**
	 * @see #portName
	 **/
	public static final String PORT_NAME = "portName";
	
	/**
	 * @see #label
	 **/
	public static final String LABEL = "label";
	
	/**
	 * @see #baudRate
	 **/
	public static final String BAUD_RATE = "baudRate";
	
	/**
	 * @see #vid
	 **/
	public static final String VID = "vid";
	
	/**
	 * @see #pid
	 **/
	public static final String PID = "pid";
	
	/**
	 * @see #manufacturer
	 **/
	public static final String MANUFACTURER = "manufacturer";
	
	/**
	 * @see #productName
	 **/
	public static final String PRODUCT_NAME = "productName";
	
	/**
	 * @see #serialNumber
	 **/
	public static final String SERIAL_NUMBER = "serialNumber";
	
	/**
	 * @see #active
	 **/
	public static final String ACTIVE = "active";
	
	/**
	 * @see #lastConnectedAt
	 **/
	public static final String LAST_CONNECTED_AT = "lastConnectedAt";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.PosteDevice}
	 **/
	public static volatile EntityType<PosteDevice> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#id}
	 **/
	public static volatile SingularAttribute<PosteDevice, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#poste}
	 **/
	public static volatile SingularAttribute<PosteDevice, Poste> poste;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#deviceType}
	 **/
	public static volatile SingularAttribute<PosteDevice, DeviceType> deviceType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#portName}
	 **/
	public static volatile SingularAttribute<PosteDevice, String> portName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#label}
	 **/
	public static volatile SingularAttribute<PosteDevice, String> label;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#baudRate}
	 **/
	public static volatile SingularAttribute<PosteDevice, Integer> baudRate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#vid}
	 **/
	public static volatile SingularAttribute<PosteDevice, Integer> vid;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#pid}
	 **/
	public static volatile SingularAttribute<PosteDevice, Integer> pid;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#manufacturer}
	 **/
	public static volatile SingularAttribute<PosteDevice, String> manufacturer;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#productName}
	 **/
	public static volatile SingularAttribute<PosteDevice, String> productName;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#serialNumber}
	 **/
	public static volatile SingularAttribute<PosteDevice, String> serialNumber;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#active}
	 **/
	public static volatile SingularAttribute<PosteDevice, Boolean> active;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#lastConnectedAt}
	 **/
	public static volatile SingularAttribute<PosteDevice, LocalDateTime> lastConnectedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PosteDevice#createdAt}
	 **/
	public static volatile SingularAttribute<PosteDevice, LocalDateTime> createdAt;

}


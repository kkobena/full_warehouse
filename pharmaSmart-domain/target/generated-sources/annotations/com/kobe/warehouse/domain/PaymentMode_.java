package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.PaymentGroup;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.PaymentMode}
 **/
@StaticMetamodel(PaymentMode.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class PaymentMode_ {

	
	/**
	 * @see #code
	 **/
	public static final String CODE = "code";
	
	/**
	 * @see #libelle
	 **/
	public static final String LIBELLE = "libelle";
	
	/**
	 * @see #order
	 **/
	public static final String ORDER = "order";
	
	/**
	 * @see #group
	 **/
	public static final String GROUP = "group";
	
	/**
	 * @see #enable
	 **/
	public static final String ENABLE = "enable";
	
	/**
	 * @see #iconUrl
	 **/
	public static final String ICON_URL = "iconUrl";
	
	/**
	 * @see #qrCode
	 **/
	public static final String QR_CODE = "qrCode";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.PaymentMode}
	 **/
	public static volatile EntityType<PaymentMode> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentMode#code}
	 **/
	public static volatile SingularAttribute<PaymentMode, String> code;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentMode#libelle}
	 **/
	public static volatile SingularAttribute<PaymentMode, String> libelle;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentMode#order}
	 **/
	public static volatile SingularAttribute<PaymentMode, Short> order;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentMode#group}
	 **/
	public static volatile SingularAttribute<PaymentMode, PaymentGroup> group;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentMode#enable}
	 **/
	public static volatile SingularAttribute<PaymentMode, Boolean> enable;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentMode#iconUrl}
	 **/
	public static volatile SingularAttribute<PaymentMode, String> iconUrl;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.PaymentMode#qrCode}
	 **/
	public static volatile SingularAttribute<PaymentMode, byte[]> qrCode;

}


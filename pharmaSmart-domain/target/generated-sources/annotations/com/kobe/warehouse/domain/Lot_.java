package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.StatutLot;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Lot}
 **/
@StaticMetamodel(Lot.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Lot_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #numLot
	 **/
	public static final String NUM_LOT = "numLot";
	
	/**
	 * @see #orderLine
	 **/
	public static final String ORDER_LINE = "orderLine";
	
	/**
	 * @see #quantity
	 **/
	public static final String QUANTITY = "quantity";
	
	/**
	 * @see #freeQty
	 **/
	public static final String FREE_QTY = "freeQty";
	
	/**
	 * @see #createdDate
	 **/
	public static final String CREATED_DATE = "createdDate";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #manufacturingDate
	 **/
	public static final String MANUFACTURING_DATE = "manufacturingDate";
	
	/**
	 * @see #expiryDate
	 **/
	public static final String EXPIRY_DATE = "expiryDate";
	
	/**
	 * @see #prixAchat
	 **/
	public static final String PRIX_ACHAT = "prixAchat";
	
	/**
	 * @see #prixUnit
	 **/
	public static final String PRIX_UNIT = "prixUnit";
	
	/**
	 * @see #currentQuantity
	 **/
	public static final String CURRENT_QUANTITY = "currentQuantity";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #serialNumber
	 **/
	public static final String SERIAL_NUMBER = "serialNumber";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Lot}
	 **/
	public static volatile EntityType<Lot> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#id}
	 **/
	public static volatile SingularAttribute<Lot, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#numLot}
	 **/
	public static volatile SingularAttribute<Lot, String> numLot;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#orderLine}
	 **/
	public static volatile SingularAttribute<Lot, OrderLine> orderLine;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#quantity}
	 **/
	public static volatile SingularAttribute<Lot, Integer> quantity;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#freeQty}
	 **/
	public static volatile SingularAttribute<Lot, Integer> freeQty;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#createdDate}
	 **/
	public static volatile SingularAttribute<Lot, LocalDateTime> createdDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#updated}
	 **/
	public static volatile SingularAttribute<Lot, LocalDateTime> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#manufacturingDate}
	 **/
	public static volatile SingularAttribute<Lot, LocalDate> manufacturingDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#expiryDate}
	 **/
	public static volatile SingularAttribute<Lot, LocalDate> expiryDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#prixAchat}
	 **/
	public static volatile SingularAttribute<Lot, Integer> prixAchat;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#prixUnit}
	 **/
	public static volatile SingularAttribute<Lot, Integer> prixUnit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#currentQuantity}
	 **/
	public static volatile SingularAttribute<Lot, Integer> currentQuantity;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#statut}
	 **/
	public static volatile SingularAttribute<Lot, StatutLot> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#serialNumber}
	 **/
	public static volatile SingularAttribute<Lot, String> serialNumber;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Lot#produit}
	 **/
	public static volatile SingularAttribute<Lot, Produit> produit;

}


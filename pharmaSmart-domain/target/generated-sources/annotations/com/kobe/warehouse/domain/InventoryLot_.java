package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.InventoryLot}
 **/
@StaticMetamodel(InventoryLot.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class InventoryLot_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #quantityOnHand
	 **/
	public static final String QUANTITY_ON_HAND = "quantityOnHand";
	
	/**
	 * @see #gap
	 **/
	public static final String GAP = "gap";
	
	/**
	 * @see #quantityInit
	 **/
	public static final String QUANTITY_INIT = "quantityInit";
	
	/**
	 * @see #inventoryValueCost
	 **/
	public static final String INVENTORY_VALUE_COST = "inventoryValueCost";
	
	/**
	 * @see #storeInventoryLine
	 **/
	public static final String STORE_INVENTORY_LINE = "storeInventoryLine";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #lot
	 **/
	public static final String LOT = "lot";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #lastUnitPrice
	 **/
	public static final String LAST_UNIT_PRICE = "lastUnitPrice";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.InventoryLot}
	 **/
	public static volatile EntityType<InventoryLot> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryLot#id}
	 **/
	public static volatile SingularAttribute<InventoryLot, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryLot#quantityOnHand}
	 **/
	public static volatile SingularAttribute<InventoryLot, Integer> quantityOnHand;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryLot#gap}
	 **/
	public static volatile SingularAttribute<InventoryLot, Integer> gap;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryLot#quantityInit}
	 **/
	public static volatile SingularAttribute<InventoryLot, Integer> quantityInit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryLot#inventoryValueCost}
	 **/
	public static volatile SingularAttribute<InventoryLot, Integer> inventoryValueCost;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryLot#storeInventoryLine}
	 **/
	public static volatile SingularAttribute<InventoryLot, StoreInventoryLine> storeInventoryLine;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryLot#updatedAt}
	 **/
	public static volatile SingularAttribute<InventoryLot, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryLot#lot}
	 **/
	public static volatile SingularAttribute<InventoryLot, Lot> lot;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryLot#updated}
	 **/
	public static volatile SingularAttribute<InventoryLot, Boolean> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryLot#lastUnitPrice}
	 **/
	public static volatile SingularAttribute<InventoryLot, Integer> lastUnitPrice;

}


package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.StoreInventoryLine}
 **/
@StaticMetamodel(StoreInventoryLine.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class StoreInventoryLine_ {

	
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
	 * @see #quantitySold
	 **/
	public static final String QUANTITY_SOLD = "quantitySold";
	
	/**
	 * @see #inventoryValueCost
	 **/
	public static final String INVENTORY_VALUE_COST = "inventoryValueCost";
	
	/**
	 * @see #storeInventory
	 **/
	public static final String STORE_INVENTORY = "storeInventory";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #storage
	 **/
	public static final String STORAGE = "storage";
	
	/**
	 * @see #lots
	 **/
	public static final String LOTS = "lots";
	
	/**
	 * @see #updated
	 **/
	public static final String UPDATED = "updated";
	
	/**
	 * @see #lastUnitPrice
	 **/
	public static final String LAST_UNIT_PRICE = "lastUnitPrice";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.StoreInventoryLine}
	 **/
	public static volatile EntityType<StoreInventoryLine> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#id}
	 **/
	public static volatile SingularAttribute<StoreInventoryLine, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#quantityOnHand}
	 **/
	public static volatile SingularAttribute<StoreInventoryLine, Integer> quantityOnHand;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#gap}
	 **/
	public static volatile SingularAttribute<StoreInventoryLine, Integer> gap;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#quantityInit}
	 **/
	public static volatile SingularAttribute<StoreInventoryLine, Integer> quantityInit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#quantitySold}
	 **/
	public static volatile SingularAttribute<StoreInventoryLine, Integer> quantitySold;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#inventoryValueCost}
	 **/
	public static volatile SingularAttribute<StoreInventoryLine, Integer> inventoryValueCost;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#storeInventory}
	 **/
	public static volatile SingularAttribute<StoreInventoryLine, StoreInventory> storeInventory;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#updatedAt}
	 **/
	public static volatile SingularAttribute<StoreInventoryLine, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#produit}
	 **/
	public static volatile SingularAttribute<StoreInventoryLine, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#storage}
	 **/
	public static volatile SingularAttribute<StoreInventoryLine, Storage> storage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#lots}
	 **/
	public static volatile ListAttribute<StoreInventoryLine, InventoryLot> lots;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#updated}
	 **/
	public static volatile SingularAttribute<StoreInventoryLine, Boolean> updated;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventoryLine#lastUnitPrice}
	 **/
	public static volatile SingularAttribute<StoreInventoryLine, Integer> lastUnitPrice;

}


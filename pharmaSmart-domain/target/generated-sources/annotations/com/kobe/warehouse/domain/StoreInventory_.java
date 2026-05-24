package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CategroryInventaire;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.domain.enumeration.InventoryType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.StoreInventory}
 **/
@StaticMetamodel(StoreInventory.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class StoreInventory_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #version
	 **/
	public static final String VERSION = "version";
	
	/**
	 * @see #description
	 **/
	public static final String DESCRIPTION = "description";
	
	/**
	 * @see #inventoryValueCostBegin
	 **/
	public static final String INVENTORY_VALUE_COST_BEGIN = "inventoryValueCostBegin";
	
	/**
	 * @see #inventoryAmountBegin
	 **/
	public static final String INVENTORY_AMOUNT_BEGIN = "inventoryAmountBegin";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #updatedAt
	 **/
	public static final String UPDATED_AT = "updatedAt";
	
	/**
	 * @see #inventoryValueCostAfter
	 **/
	public static final String INVENTORY_VALUE_COST_AFTER = "inventoryValueCostAfter";
	
	/**
	 * @see #inventoryAmountAfter
	 **/
	public static final String INVENTORY_AMOUNT_AFTER = "inventoryAmountAfter";
	
	/**
	 * @see #storeInventoryLines
	 **/
	public static final String STORE_INVENTORY_LINES = "storeInventoryLines";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #statut
	 **/
	public static final String STATUT = "statut";
	
	/**
	 * @see #category
	 **/
	public static final String CATEGORY = "category";
	
	/**
	 * @see #storage
	 **/
	public static final String STORAGE = "storage";
	
	/**
	 * @see #rayon
	 **/
	public static final String RAYON = "rayon";
	
	/**
	 * @see #inventoryType
	 **/
	public static final String INVENTORY_TYPE = "inventoryType";
	
	/**
	 * @see #inventoryCategory
	 **/
	public static final String INVENTORY_CATEGORY = "inventoryCategory";
	
	/**
	 * @see #gapCost
	 **/
	public static final String GAP_COST = "gapCost";
	
	/**
	 * @see #gapAmount
	 **/
	public static final String GAP_AMOUNT = "gapAmount";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.StoreInventory}
	 **/
	public static volatile EntityType<StoreInventory> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#id}
	 **/
	public static volatile SingularAttribute<StoreInventory, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#version}
	 **/
	public static volatile SingularAttribute<StoreInventory, Long> version;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#description}
	 **/
	public static volatile SingularAttribute<StoreInventory, String> description;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#inventoryValueCostBegin}
	 **/
	public static volatile SingularAttribute<StoreInventory, Long> inventoryValueCostBegin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#inventoryAmountBegin}
	 **/
	public static volatile SingularAttribute<StoreInventory, Long> inventoryAmountBegin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#createdAt}
	 **/
	public static volatile SingularAttribute<StoreInventory, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#updatedAt}
	 **/
	public static volatile SingularAttribute<StoreInventory, LocalDateTime> updatedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#inventoryValueCostAfter}
	 **/
	public static volatile SingularAttribute<StoreInventory, Long> inventoryValueCostAfter;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#inventoryAmountAfter}
	 **/
	public static volatile SingularAttribute<StoreInventory, Long> inventoryAmountAfter;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#storeInventoryLines}
	 **/
	public static volatile ListAttribute<StoreInventory, StoreInventoryLine> storeInventoryLines;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#user}
	 **/
	public static volatile SingularAttribute<StoreInventory, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#statut}
	 **/
	public static volatile SingularAttribute<StoreInventory, InventoryStatut> statut;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#category}
	 **/
	public static volatile SingularAttribute<StoreInventory, CategroryInventaire> category;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#storage}
	 **/
	public static volatile SingularAttribute<StoreInventory, Storage> storage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#rayon}
	 **/
	public static volatile SingularAttribute<StoreInventory, Rayon> rayon;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#inventoryType}
	 **/
	public static volatile SingularAttribute<StoreInventory, InventoryType> inventoryType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#inventoryCategory}
	 **/
	public static volatile SingularAttribute<StoreInventory, InventoryCategory> inventoryCategory;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#gapCost}
	 **/
	public static volatile SingularAttribute<StoreInventory, Integer> gapCost;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.StoreInventory#gapAmount}
	 **/
	public static volatile SingularAttribute<StoreInventory, Integer> gapAmount;

}


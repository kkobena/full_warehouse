package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.MouvementProduit;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.InventoryTransaction}
 **/
@StaticMetamodel(InventoryTransaction.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class InventoryTransaction_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #transactionDate
	 **/
	public static final String TRANSACTION_DATE = "transactionDate";
	
	/**
	 * @see #mouvementType
	 **/
	public static final String MOUVEMENT_TYPE = "mouvementType";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";
	
	/**
	 * @see #quantity
	 **/
	public static final String QUANTITY = "quantity";
	
	/**
	 * @see #quantityBefor
	 **/
	public static final String QUANTITY_BEFOR = "quantityBefor";
	
	/**
	 * @see #quantityAfter
	 **/
	public static final String QUANTITY_AFTER = "quantityAfter";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #user
	 **/
	public static final String USER = "user";
	
	/**
	 * @see #costAmount
	 **/
	public static final String COST_AMOUNT = "costAmount";
	
	/**
	 * @see #regularUnitPrice
	 **/
	public static final String REGULAR_UNIT_PRICE = "regularUnitPrice";
	
	/**
	 * @see #magasin
	 **/
	public static final String MAGASIN = "magasin";
	
	/**
	 * @see #storage
	 **/
	public static final String STORAGE = "storage";
	
	/**
	 * @see #entityId
	 **/
	public static final String ENTITY_ID = "entityId";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.InventoryTransaction}
	 **/
	public static volatile EntityType<InventoryTransaction> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#id}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#transactionDate}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, LocalDate> transactionDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#mouvementType}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, MouvementProduit> mouvementType;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#createdAt}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, LocalDateTime> createdAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#quantity}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, Integer> quantity;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#quantityBefor}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, Integer> quantityBefor;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#quantityAfter}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, Integer> quantityAfter;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#produit}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#user}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, AppUser> user;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#costAmount}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, Integer> costAmount;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#regularUnitPrice}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, Integer> regularUnitPrice;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#magasin}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, Magasin> magasin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#storage}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, Storage> storage;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryTransaction#entityId}
	 **/
	public static volatile SingularAttribute<InventoryTransaction, Long> entityId;

}


package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CauseEcart;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.InventoryGapAnalysis}
 **/
@StaticMetamodel(InventoryGapAnalysis.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class InventoryGapAnalysis_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #storeInventoryLine
	 **/
	public static final String STORE_INVENTORY_LINE = "storeInventoryLine";
	
	/**
	 * @see #cause
	 **/
	public static final String CAUSE = "cause";
	
	/**
	 * @see #quantity
	 **/
	public static final String QUANTITY = "quantity";
	
	/**
	 * @see #commentaire
	 **/
	public static final String COMMENTAIRE = "commentaire";
	
	/**
	 * @see #createdAt
	 **/
	public static final String CREATED_AT = "createdAt";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.InventoryGapAnalysis}
	 **/
	public static volatile EntityType<InventoryGapAnalysis> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryGapAnalysis#id}
	 **/
	public static volatile SingularAttribute<InventoryGapAnalysis, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryGapAnalysis#storeInventoryLine}
	 **/
	public static volatile SingularAttribute<InventoryGapAnalysis, StoreInventoryLine> storeInventoryLine;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryGapAnalysis#cause}
	 **/
	public static volatile SingularAttribute<InventoryGapAnalysis, CauseEcart> cause;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryGapAnalysis#quantity}
	 **/
	public static volatile SingularAttribute<InventoryGapAnalysis, Integer> quantity;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryGapAnalysis#commentaire}
	 **/
	public static volatile SingularAttribute<InventoryGapAnalysis, String> commentaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.InventoryGapAnalysis#createdAt}
	 **/
	public static volatile SingularAttribute<InventoryGapAnalysis, LocalDateTime> createdAt;

}


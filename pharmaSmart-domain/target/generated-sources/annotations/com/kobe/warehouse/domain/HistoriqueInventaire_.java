package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.HistoriqueInventaire}
 **/
@StaticMetamodel(HistoriqueInventaire.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class HistoriqueInventaire_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
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
	 * @see #created
	 **/
	public static final String CREATED = "created";
	
	/**
	 * @see #inventoryValueCostAfter
	 **/
	public static final String INVENTORY_VALUE_COST_AFTER = "inventoryValueCostAfter";
	
	/**
	 * @see #inventoryAmountAfter
	 **/
	public static final String INVENTORY_AMOUNT_AFTER = "inventoryAmountAfter";
	
	/**
	 * @see #gapCost
	 **/
	public static final String GAP_COST = "gapCost";
	
	/**
	 * @see #gapAmount
	 **/
	public static final String GAP_AMOUNT = "gapAmount";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.HistoriqueInventaire}
	 **/
	public static volatile EntityType<HistoriqueInventaire> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueInventaire#id}
	 **/
	public static volatile SingularAttribute<HistoriqueInventaire, Long> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueInventaire#description}
	 **/
	public static volatile SingularAttribute<HistoriqueInventaire, String> description;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueInventaire#inventoryValueCostBegin}
	 **/
	public static volatile SingularAttribute<HistoriqueInventaire, Long> inventoryValueCostBegin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueInventaire#inventoryAmountBegin}
	 **/
	public static volatile SingularAttribute<HistoriqueInventaire, Long> inventoryAmountBegin;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueInventaire#created}
	 **/
	public static volatile SingularAttribute<HistoriqueInventaire, LocalDateTime> created;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueInventaire#inventoryValueCostAfter}
	 **/
	public static volatile SingularAttribute<HistoriqueInventaire, Long> inventoryValueCostAfter;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueInventaire#inventoryAmountAfter}
	 **/
	public static volatile SingularAttribute<HistoriqueInventaire, Long> inventoryAmountAfter;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueInventaire#gapCost}
	 **/
	public static volatile SingularAttribute<HistoriqueInventaire, Long> gapCost;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.HistoriqueInventaire#gapAmount}
	 **/
	public static volatile SingularAttribute<HistoriqueInventaire, Long> gapAmount;

}


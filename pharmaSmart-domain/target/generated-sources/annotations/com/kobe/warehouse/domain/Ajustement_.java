package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.AjustType;
import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.Ajustement}
 **/
@StaticMetamodel(Ajustement.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class Ajustement_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #qtyMvt
	 **/
	public static final String QTY_MVT = "qtyMvt";
	
	/**
	 * @see #dateMtv
	 **/
	public static final String DATE_MTV = "dateMtv";
	
	/**
	 * @see #stockBefore
	 **/
	public static final String STOCK_BEFORE = "stockBefore";
	
	/**
	 * @see #type
	 **/
	public static final String TYPE = "type";
	
	/**
	 * @see #stockAfter
	 **/
	public static final String STOCK_AFTER = "stockAfter";
	
	/**
	 * @see #stockProduit
	 **/
	public static final String STOCK_PRODUIT = "stockProduit";
	
	/**
	 * @see #ajust
	 **/
	public static final String AJUST = "ajust";
	
	/**
	 * @see #motifAjustement
	 **/
	public static final String MOTIF_AJUSTEMENT = "motifAjustement";
	
	/**
	 * @see #lot
	 **/
	public static final String LOT = "lot";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.Ajustement}
	 **/
	public static volatile EntityType<Ajustement> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajustement#id}
	 **/
	public static volatile SingularAttribute<Ajustement, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajustement#qtyMvt}
	 **/
	public static volatile SingularAttribute<Ajustement, Integer> qtyMvt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajustement#dateMtv}
	 **/
	public static volatile SingularAttribute<Ajustement, LocalDateTime> dateMtv;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajustement#stockBefore}
	 **/
	public static volatile SingularAttribute<Ajustement, Integer> stockBefore;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajustement#type}
	 **/
	public static volatile SingularAttribute<Ajustement, AjustType> type;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajustement#stockAfter}
	 **/
	public static volatile SingularAttribute<Ajustement, Integer> stockAfter;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajustement#stockProduit}
	 **/
	public static volatile SingularAttribute<Ajustement, StockProduit> stockProduit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajustement#ajust}
	 **/
	public static volatile SingularAttribute<Ajustement, Ajust> ajust;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajustement#motifAjustement}
	 **/
	public static volatile SingularAttribute<Ajustement, MotifAjustement> motifAjustement;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.Ajustement#lot}
	 **/
	public static volatile SingularAttribute<Ajustement, Lot> lot;

}


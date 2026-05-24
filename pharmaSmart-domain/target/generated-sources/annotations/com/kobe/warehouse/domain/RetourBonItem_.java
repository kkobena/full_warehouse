package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.RetourBonItem}
 **/
@StaticMetamodel(RetourBonItem.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class RetourBonItem_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #dateMtv
	 **/
	public static final String DATE_MTV = "dateMtv";
	
	/**
	 * @see #retourBon
	 **/
	public static final String RETOUR_BON = "retourBon";
	
	/**
	 * @see #motifRetour
	 **/
	public static final String MOTIF_RETOUR = "motifRetour";
	
	/**
	 * @see #orderLine
	 **/
	public static final String ORDER_LINE = "orderLine";
	
	/**
	 * @see #lot
	 **/
	public static final String LOT = "lot";
	
	/**
	 * @see #qtyMvt
	 **/
	public static final String QTY_MVT = "qtyMvt";
	
	/**
	 * @see #initStock
	 **/
	public static final String INIT_STOCK = "initStock";
	
	/**
	 * @see #afterStock
	 **/
	public static final String AFTER_STOCK = "afterStock";
	
	/**
	 * @see #acceptedQty
	 **/
	public static final String ACCEPTED_QTY = "acceptedQty";
	
	/**
	 * @see #prixAchat
	 **/
	public static final String PRIX_ACHAT = "prixAchat";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.RetourBonItem}
	 **/
	public static volatile EntityType<RetourBonItem> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBonItem#id}
	 **/
	public static volatile SingularAttribute<RetourBonItem, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBonItem#dateMtv}
	 **/
	public static volatile SingularAttribute<RetourBonItem, LocalDateTime> dateMtv;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBonItem#retourBon}
	 **/
	public static volatile SingularAttribute<RetourBonItem, RetourBon> retourBon;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBonItem#motifRetour}
	 **/
	public static volatile SingularAttribute<RetourBonItem, MotifRetourProduit> motifRetour;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBonItem#orderLine}
	 **/
	public static volatile SingularAttribute<RetourBonItem, OrderLine> orderLine;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBonItem#lot}
	 **/
	public static volatile SingularAttribute<RetourBonItem, Lot> lot;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBonItem#qtyMvt}
	 **/
	public static volatile SingularAttribute<RetourBonItem, Integer> qtyMvt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBonItem#initStock}
	 **/
	public static volatile SingularAttribute<RetourBonItem, Integer> initStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBonItem#afterStock}
	 **/
	public static volatile SingularAttribute<RetourBonItem, Integer> afterStock;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBonItem#acceptedQty}
	 **/
	public static volatile SingularAttribute<RetourBonItem, Integer> acceptedQty;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourBonItem#prixAchat}
	 **/
	public static volatile SingularAttribute<RetourBonItem, Integer> prixAchat;

}


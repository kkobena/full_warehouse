package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDate;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.RetourClientLine}
 **/
@StaticMetamodel(RetourClientLine.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class RetourClientLine_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #retourClient
	 **/
	public static final String RETOUR_CLIENT = "retourClient";
	
	/**
	 * @see #produit
	 **/
	public static final String PRODUIT = "produit";
	
	/**
	 * @see #quantite
	 **/
	public static final String QUANTITE = "quantite";
	
	/**
	 * @see #prixUnitaire
	 **/
	public static final String PRIX_UNITAIRE = "prixUnitaire";
	
	/**
	 * @see #montant
	 **/
	public static final String MONTANT = "montant";
	
	/**
	 * @see #originalSalesLineId
	 **/
	public static final String ORIGINAL_SALES_LINE_ID = "originalSalesLineId";
	
	/**
	 * @see #originalSalesLineDate
	 **/
	public static final String ORIGINAL_SALES_LINE_DATE = "originalSalesLineDate";
	
	/**
	 * @see #montantTp
	 **/
	public static final String MONTANT_TP = "montantTp";
	
	/**
	 * @see #emballageIntact
	 **/
	public static final String EMBALLAGE_INTACT = "emballageIntact";
	
	/**
	 * @see #numLotLisible
	 **/
	public static final String NUM_LOT_LISIBLE = "numLotLisible";
	
	/**
	 * @see #datePeremptionValide
	 **/
	public static final String DATE_PEREMPTION_VALIDE = "datePeremptionValide";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.RetourClientLine}
	 **/
	public static volatile EntityType<RetourClientLine> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClientLine#id}
	 **/
	public static volatile SingularAttribute<RetourClientLine, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClientLine#retourClient}
	 **/
	public static volatile SingularAttribute<RetourClientLine, RetourClient> retourClient;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClientLine#produit}
	 **/
	public static volatile SingularAttribute<RetourClientLine, Produit> produit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClientLine#quantite}
	 **/
	public static volatile SingularAttribute<RetourClientLine, Integer> quantite;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClientLine#prixUnitaire}
	 **/
	public static volatile SingularAttribute<RetourClientLine, Integer> prixUnitaire;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClientLine#montant}
	 **/
	public static volatile SingularAttribute<RetourClientLine, Integer> montant;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClientLine#originalSalesLineId}
	 **/
	public static volatile SingularAttribute<RetourClientLine, Long> originalSalesLineId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClientLine#originalSalesLineDate}
	 **/
	public static volatile SingularAttribute<RetourClientLine, LocalDate> originalSalesLineDate;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClientLine#montantTp}
	 **/
	public static volatile SingularAttribute<RetourClientLine, Integer> montantTp;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClientLine#emballageIntact}
	 **/
	public static volatile SingularAttribute<RetourClientLine, Boolean> emballageIntact;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClientLine#numLotLisible}
	 **/
	public static volatile SingularAttribute<RetourClientLine, Boolean> numLotLisible;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.RetourClientLine#datePeremptionValide}
	 **/
	public static volatile SingularAttribute<RetourClientLine, Boolean> datePeremptionValide;

}


package com.kobe.warehouse.domain;

import jakarta.annotation.Generated;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.time.LocalDateTime;

/**
 * Static metamodel for {@link com.kobe.warehouse.domain.FournisseurProduitPriceHistory}
 **/
@StaticMetamodel(FournisseurProduitPriceHistory.class)
@Generated("org.hibernate.processor.HibernateProcessor")
public abstract class FournisseurProduitPriceHistory_ {

	
	/**
	 * @see #id
	 **/
	public static final String ID = "id";
	
	/**
	 * @see #fournisseurProduit
	 **/
	public static final String FOURNISSEUR_PRODUIT = "fournisseurProduit";
	
	/**
	 * @see #oldPrixAchat
	 **/
	public static final String OLD_PRIX_ACHAT = "oldPrixAchat";
	
	/**
	 * @see #newPrixAchat
	 **/
	public static final String NEW_PRIX_ACHAT = "newPrixAchat";
	
	/**
	 * @see #oldPrixUni
	 **/
	public static final String OLD_PRIX_UNI = "oldPrixUni";
	
	/**
	 * @see #newPrixUni
	 **/
	public static final String NEW_PRIX_UNI = "newPrixUni";
	
	/**
	 * @see #changedAt
	 **/
	public static final String CHANGED_AT = "changedAt";
	
	/**
	 * @see #changedBy
	 **/
	public static final String CHANGED_BY = "changedBy";
	
	/**
	 * @see #commandeId
	 **/
	public static final String COMMANDE_ID = "commandeId";
	
	/**
	 * @see #receiptReference
	 **/
	public static final String RECEIPT_REFERENCE = "receiptReference";

	
	/**
	 * Static metamodel type for {@link com.kobe.warehouse.domain.FournisseurProduitPriceHistory}
	 **/
	public static volatile EntityType<FournisseurProduitPriceHistory> class_;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduitPriceHistory#id}
	 **/
	public static volatile SingularAttribute<FournisseurProduitPriceHistory, Integer> id;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduitPriceHistory#fournisseurProduit}
	 **/
	public static volatile SingularAttribute<FournisseurProduitPriceHistory, FournisseurProduit> fournisseurProduit;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduitPriceHistory#oldPrixAchat}
	 **/
	public static volatile SingularAttribute<FournisseurProduitPriceHistory, Integer> oldPrixAchat;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduitPriceHistory#newPrixAchat}
	 **/
	public static volatile SingularAttribute<FournisseurProduitPriceHistory, Integer> newPrixAchat;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduitPriceHistory#oldPrixUni}
	 **/
	public static volatile SingularAttribute<FournisseurProduitPriceHistory, Integer> oldPrixUni;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduitPriceHistory#newPrixUni}
	 **/
	public static volatile SingularAttribute<FournisseurProduitPriceHistory, Integer> newPrixUni;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduitPriceHistory#changedAt}
	 **/
	public static volatile SingularAttribute<FournisseurProduitPriceHistory, LocalDateTime> changedAt;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduitPriceHistory#changedBy}
	 **/
	public static volatile SingularAttribute<FournisseurProduitPriceHistory, AppUser> changedBy;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduitPriceHistory#commandeId}
	 **/
	public static volatile SingularAttribute<FournisseurProduitPriceHistory, Integer> commandeId;
	
	/**
	 * Static metamodel for attribute {@link com.kobe.warehouse.domain.FournisseurProduitPriceHistory#receiptReference}
	 **/
	public static volatile SingularAttribute<FournisseurProduitPriceHistory, String> receiptReference;

}

